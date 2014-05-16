/*******************************************************************************
 * Copyright (C) 2006-2013 AITIA International, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package ai.aitia.meme.paramsweep.platform.mason.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ai.aitia.meme.paramsweep.batch.BatchEvent;
import ai.aitia.meme.paramsweep.batch.BatchEvent.EventType;
import ai.aitia.meme.paramsweep.batch.BatchException;
import ai.aitia.meme.paramsweep.batch.IBatchController;
import ai.aitia.meme.paramsweep.batch.IBatchListener;
import ai.aitia.meme.paramsweep.batch.InvalidEntryPointException;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.ISubmodelParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;
import ai.aitia.meme.paramsweep.batch.param.SubmodelInfo;
import ai.aitia.meme.paramsweep.util.DefaultParameterPartitioner;
import ai.aitia.meme.paramsweep.utils.SimulationException;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.utils.Utils.Pair;

public class MasonBatchController implements IBatchController, IRecorderListenerAware {

	//====================================================================================================
	// members
	
	private List<IBatchListener> listeners = new ArrayList<IBatchListener>();;
	private String modelPath = null;
	private String generatedModelName = null;
	private boolean local = false;
	private ParameterTree paramTree = null;
	private int noRuns = -1;
	private IMasonGeneratedModel model = null; 
	private long batchCount = 0;
	private long skipCount = -1;
	private Class<?> modelClass = null;
	
	private Map<String,Pair<Object,Method>> methods = new HashMap<String,Pair<Object,Method>>();
	private Map<String,Object> parentParameters = new HashMap<String,Object>();
	private Deque<SubmodelInfo<?>> submodelInfos = new LinkedList<SubmodelInfo<?>>();
	private HashMap<String,Object> constantParameterNames = new HashMap<String,Object>();
	private HashMap<String,Object> mutableParameterNames = new HashMap<String,Object>();
	private boolean stopped = false;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public MasonBatchController(String generatedModelName, boolean local) {
		this.generatedModelName = generatedModelName;
		this.local = local;
	}

	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public void addBatchListener(IBatchListener listener) { listeners.add(listener); }
	public void setModelPath(String path) { modelPath = path; }

	//----------------------------------------------------------------------------------------------------
	public void setRecorders(List<RecorderInfo> recorders) {} 
	public void setStopCondition(String conditionCode) {}
	
	//----------------------------------------------------------------------------------------------------
	public void removeBatchListener(IBatchListener listener) {
		if (model != null)
			model.aitiaGenerated_removeBatchListener(listener);
		listeners.remove(listener);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void addRecorderListener(final MasonRecorderListener listener){
		model.aitiaGenerated_addRecorderListener(listener);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void removeRecorderListener(final MasonRecorderListener listener){
		model.aitiaGenerated_removeRecorderListener(listener);
	}
	
	//----------------------------------------------------------------------------------------------------
	public Map<String,Object> getConstantParameterNames() { return constantParameterNames; }
	
	//----------------------------------------------------------------------------------------------------
	public Map<String,Object> getMutableParameterNames() { return mutableParameterNames; }
	
	//----------------------------------------------------------------------------------------------------
	public int getNumberOfRuns() {
		if (noRuns < 0) {
			if (paramTree == null) 
				return 0;
//			int temp = 0;
//			for (Iterator<List<AbstractParameterInfo>> it = new DefaultParameterPartitioner().partition(paramTree);it.hasNext();it.next(),temp++);
//			noRuns = temp;
			noRuns = (int) Util.calculateNumberOfRuns(paramTree,false);
		}
		return noRuns;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setEntryPoint(String entryPoint) throws InvalidEntryPointException {
		try {
			//FIXME: possibly ruining paths containing the name 'class', like: /home/user/models/newProject/src/main/java/com/package/class/student/StudentModel.class
			//maybe other platforms have the same problem
			String tmp = entryPoint.substring(modelPath.length() + 1).replace('/','.').replace('\\','.').replaceAll("\\.class","");
			if (!local)
				generatedModelName = tmp;
			if (!generatedModelName.startsWith(tmp))
				throw new InvalidEntryPointException("Entry path doesn't match to the model class ( " + generatedModelName + " ~ " + tmp + " )");
			Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
			modelClass = Class.forName(generatedModelName);
			model = (IMasonGeneratedModel) modelClass.newInstance();
		} catch (Exception e) {
			throw new InvalidEntryPointException(e);
		}
	}

	//----------------------------------------------------------------------------------------------------
	public void setParameters(ParameterTree paramTree) {
		if (paramTree == null) 
			throw new IllegalArgumentException("'paramTree' cannot be null.");
		this.paramTree = paramTree;
	}

	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	public void startBatch() throws BatchException {
		for (IBatchListener l : listeners)
			model.aitiaGenerated_addBatchListener(l);

		Iterator<List<AbstractParameterInfo>> it = new DefaultParameterPartitioner().partition(paramTree);
		while (it.hasNext() && !stopped) {
			batchCount++;
			List<AbstractParameterInfo> combination = it.next();
			
			if (batchCount > skipCount) {
				fillParentParametersTable(combination);
				fillMethodTable(combination);
				setParameters(combination);
				setSubmodelParameters();
				model.modelInitialization();
				try {
					startSim();
				} catch (final SimulationException e) {
					throw new BatchException(e);
				} catch (final Throwable e) {
					e.printStackTrace();
				}
			}
	
		}
		model.aitiaGenerated_writeEnd();
		for (IBatchListener l : listeners)
			model.aitiaGenerated_removeBatchListener(l);
		fireBatchEnded();
	}

	//----------------------------------------------------------------------------------------------------
	public void stopBatch() throws BatchException { 
		stopped = true;
		model.simulationStop();
	}
	
	//----------------------------------------------------------------------------------------------------
	public void stopCurrentRun() throws BatchException { model.simulationStop(); }
	
	//----------------------------------------------------------------------------------------------------
	public void setSkipCount(final long skipCount) { this.skipCount = skipCount; } 

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	private void fillParentParametersTable(List<AbstractParameterInfo> combination) throws BatchException {
		submodelInfos.clear();
		for (final AbstractParameterInfo p : combination) {
			if (p instanceof ISubmodelParameterInfo) {
				final SubmodelInfo<?> parentInfo = ((ISubmodelParameterInfo)p).getParentInfo();
				fillParentParametersTable(parentInfo);
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void fillParentParametersTable(final SubmodelInfo<?> info) throws BatchException {
		if (info != null) {
			fillParentParametersTable(info.getParentInfo());
			
			final String name = getSubmodelInfoFullName(info);
			if (parentParameters.get(name) == null) {
				try {
					final Object instance = info.getActualType().newInstance();
					parentParameters.put(name,instance);
//					if (info.getParentInfo() == null) {
//						final Method setter = modelClass.getMethod("set" + Util.capitalize(info.getName()),info.getReferenceType());
//						setter.invoke(model,instance);
//					} else {
//						final Method setter = info.getParentInfo().getActualType().getMethod("set" + Util.capitalize(info.getName()),info.getReferenceType());
//						final int idx = name.lastIndexOf('#'); 
//						final String parentName = name.substring(0,idx);
//						setter.invoke(parentParameters.get(parentName),instance);
//					}
					if (info.getParentInfo() == null || !submodelInfos.contains(info.getParentInfo()))
						submodelInfos.addLast(info);
					else
						submodelInfos.addFirst(info);
				} catch (final Exception e) {
					throw new BatchException(e);
				}
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getSubmodelInfoFullName(final SubmodelInfo<?> info) {
		if (info == null) 
			return "";
		
		return getSubmodelInfoFullName(info.getParentInfo()) + "#" + info.getName();
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	private void fillMethodTable(List<AbstractParameterInfo> combination) throws BatchException { 
		final Method[] methods = modelClass.getMethods();
		for (AbstractParameterInfo p : combination) {
			final String name = Util.capitalize(p.getName());
			if (p instanceof ISubmodelParameterInfo) {
				final ISubmodelParameterInfo spi = (ISubmodelParameterInfo) p;
				final String parentInstanceId = getSubmodelInfoFullName(spi.getParentInfo());
				final Object instance = parentParameters.get(parentInstanceId);
				if (instance == null)
					throw new BatchException("Instance not found for " + p.getName());
				final String instanceId = parentInstanceId + "#" + p.getName();
				final Pair<Object,Method> pair = this.methods.get(instanceId);
				
				if (pair == null) {
					for (final Method m : spi.getParentInfo().getActualType().getMethods()) {
						if (m.getName().equals("set" + name) && m.getParameterTypes().length == 1) {
							this.methods.put(instanceId,new Pair<Object,Method>(instance,m));
							(p.isOriginalConstant() ? constantParameterNames : mutableParameterNames).put(instanceId,instance);
							break;
						}
					}
				} else 
					pair.set(instance,pair.getValue());
			} else {
				final Pair<Object,Method> pair = this.methods.get(name);
				if (pair == null) {
					for (final Method m : methods) {
						if (m.getName().equals("set" + name) && m.getParameterTypes().length == 1) {
							this.methods.put(p.getName(),new Pair<Object,Method>(model,m));
							(p.isOriginalConstant() ? constantParameterNames : mutableParameterNames).put(name,model);
							break;
						}
					}
				}
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	private void setParameters(final List<AbstractParameterInfo> combination) throws BatchException {
		model.aitiaGenerated_setRun(batchCount);
		model.aitiaGenerated_setConstantParameterNames(constantParameterNames);
		model.aitiaGenerated_setMutableParameterNames(mutableParameterNames);
		for (AbstractParameterInfo p : combination) {
			String name = p.getName();
			if (p instanceof ISubmodelParameterInfo) 
				name = getSubmodelInfoFullName(((ISubmodelParameterInfo)p).getParentInfo()) + "#" + name;
			
			final Pair<Object,Method> setterPair  = methods.get(name);
			final Object value = p.iterator().next();
			if (setterPair != null) {
				try {
					setterPair.getSecond().invoke(setterPair.getFirst(),value);
				} catch (IllegalAccessException e) {
					throw new BatchException(e);
				} catch (InvocationTargetException e) {
					throw new BatchException(e);
				}
			} else 
				// never happens
				throw new IllegalStateException();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void setSubmodelParameters() throws BatchException {
		for (final SubmodelInfo<?> info : submodelInfos) {
			final String name = getSubmodelInfoFullName(info);
			final Object instance = parentParameters.get(name);

			try {
				if (info.getParentInfo() == null) {
					final Method setter = modelClass.getMethod("set" + Util.capitalize(info.getName()),info.getReferenceType());
					setter.invoke(model,instance);
				} else {
					final Method setter = info.getParentInfo().getActualType().getMethod("set" + Util.capitalize(info.getName()),info.getReferenceType());
					final int idx = name.lastIndexOf('#'); 
					final String parentName = name.substring(0,idx);
					setter.invoke(parentParameters.get(parentName),instance);
				}
			} catch (final Exception e) {
				throw new BatchException(e);
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void startSim() throws SimulationException {
		model.simulationStart();
		fireRunChanged();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void fireRunChanged() {
		BatchEvent event = new BatchEvent(this,EventType.RUN_ENDED,batchCount);
		for (IBatchListener l : listeners) 
			l.runChanged(event);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void fireBatchEnded() {
		BatchEvent event = new BatchEvent(this,EventType.BATCH_ENDED);
		List<IBatchListener> _tempList = new ArrayList<IBatchListener>(listeners);
		for (IBatchListener l : _tempList)
			l.batchEnded(event);
	}
}
