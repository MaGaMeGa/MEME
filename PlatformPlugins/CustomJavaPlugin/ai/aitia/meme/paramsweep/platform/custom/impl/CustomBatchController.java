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
package ai.aitia.meme.paramsweep.platform.custom.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ai.aitia.meme.paramsweep.batch.BatchEvent;
import ai.aitia.meme.paramsweep.batch.BatchException;
import ai.aitia.meme.paramsweep.batch.IBatchController;
import ai.aitia.meme.paramsweep.batch.IBatchListener;
import ai.aitia.meme.paramsweep.batch.InvalidEntryPointException;
import ai.aitia.meme.paramsweep.batch.BatchEvent.EventType;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;
import ai.aitia.meme.paramsweep.util.DefaultParameterPartitioner;
import ai.aitia.meme.paramsweep.utils.SimulationException;
import ai.aitia.meme.paramsweep.utils.Util;

public class CustomBatchController implements IBatchController {

	//====================================================================================================
	// members
	
	private List<IBatchListener> listeners = new ArrayList<IBatchListener>();;
	private String modelPath = null;
	private String generatedModelName = null;
	private boolean local = false;
	private ParameterTree paramTree = null;
	private int noRuns = -1;
	private ICustomGeneratedModel model = null; 
	private long batchCount = 0;
	private boolean first = true;
	private Class<?> modelClass = null;
	
	private HashMap<String,Method> methods = new HashMap<String,Method>();
	private Method setParameterMethod = null;
	private ArrayList<String> constantParameterNames = new ArrayList<String>();
	private ArrayList<String> mutableParameterNames = new ArrayList<String>();
	private boolean stopped = false;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public CustomBatchController(String generatedModelName, boolean local) {
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
			String tmp = entryPoint.substring(modelPath.length() + 1).replace('/','.').replace('\\','.').replaceAll("\\.class","");
			if (!local)
				generatedModelName = tmp;
			if (!generatedModelName.startsWith(tmp))
				throw new InvalidEntryPointException("Entry path doesn't match to the model class");
			Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
			modelClass = Class.forName(generatedModelName);
			model = (ICustomGeneratedModel) modelClass.newInstance();
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
	public void startBatch() throws BatchException {
		Iterator<List<AbstractParameterInfo>> it = new DefaultParameterPartitioner().partition(paramTree);
		while (it.hasNext() && !stopped) {
			batchCount++;
			List<AbstractParameterInfo> combination = it.next();
			if (first) {
				fillMethodTable(combination);
				first = false;
			} else
				try {
					model = (ICustomGeneratedModel) modelClass.newInstance();
				} catch (InstantiationException e) {
					throw new BatchException(e);
				} catch (IllegalAccessException e) {
					throw new BatchException(e);
				}
			for (IBatchListener l : listeners)
				model.aitiaGenerated_addBatchListener(l);
			setParameters(combination);
			try {
				startSim();
			} catch (SimulationException e) {
				e.printStackTrace();
				throw new BatchException(e);
			}
			for (IBatchListener l : listeners)
				model.aitiaGenerated_removeBatchListener(l);
		}
		model.aitiaGenerated_writeEnd();
		fireBatchEnded();
	}

	//----------------------------------------------------------------------------------------------------
	public void stopBatch() throws BatchException { 
		stopped = true;
		model.simulationStop();
	}
	public void stopCurrentRun() throws BatchException { model.simulationStop(); }

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void fillMethodTable(List<AbstractParameterInfo> combination) { 
		try {
			setParameterMethod = modelClass.getMethod(CustomModelInformation.SET_PARAMETER,String.class,Object.class);
		} catch (Exception e) {
			setParameterMethod = null;
		}
		Method[] methods = modelClass.getMethods();
		for (AbstractParameterInfo p : combination) {
			String name = Util.capitalize(p.getName());
			(p.isOriginalConstant() ? constantParameterNames : mutableParameterNames).add(name);
			for (Method m : methods) {
				if (m.getName().equals("set" + name) && m.getParameterTypes().length == 1) {
					this.methods.put(p.getName(),m);
					break;
				}
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void setParameters(List<AbstractParameterInfo> combination) throws BatchException {
		model.aitiaGenerated_setRun(batchCount);
		model.aitiaGenerated_setConstantParameterNames(constantParameterNames);
		model.aitiaGenerated_setMutableParameterNames(mutableParameterNames);
		for (AbstractParameterInfo p : combination) {
			Method m = methods.get(p.getName());
			Object value = p.iterator().next();
			if (m != null) {
				try {
					m.invoke(model,value);
				} catch (IllegalAccessException e) {
					throw new BatchException(e);
				} catch (InvocationTargetException e) {
					throw new BatchException(e);
				}
			} else if (setParameterMethod != null) {
				try {
					//setParameterMethod.invoke(model,Util.capitalize(p.getName()),value);
					setParameterMethod.invoke(model,Util.uncapitalize(p.getName()),value);
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
