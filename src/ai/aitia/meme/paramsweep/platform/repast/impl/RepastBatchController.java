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
package ai.aitia.meme.paramsweep.platform.repast.impl;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import uchicago.src.reflector.Invoker;
import uchicago.src.reflector.InvokerFactory;
import uchicago.src.sim.engine.BatchController;
import uchicago.src.sim.engine.SimModel;
import uchicago.src.sim.parameter.ParameterSetter;
import uchicago.src.sim.util.Random;
import ai.aitia.meme.paramsweep.batch.BatchEvent;
import ai.aitia.meme.paramsweep.batch.IBatchController;
import ai.aitia.meme.paramsweep.batch.IBatchListener;
import ai.aitia.meme.paramsweep.batch.InvalidEntryPointException;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;
import ai.aitia.meme.paramsweep.util.DefaultParameterPartitioner;
import ai.aitia.meme.paramsweep.utils.Util;


/**
 * This implementation uses Repast classes internally delegating
 * actions as appropriate. A logger with the class fully qualified 
 * name is defined.  
 */
public class RepastBatchController implements IBatchController {
	private static final Logger logger =  Logger.getLogger(RepastBatchController.class.getName());
	private BatchController bc;
	private MyRepastParameterSetter parameterSetter;
	private Map<IBatchListener, InternalBatchListener> listeners;
	
	private String generatedModelName = null;
	private String modelPath = null;
	private boolean local = false;
	
	private ParameterTree paramTree = null;
	private int noRuns = -1;
	
	public RepastBatchController(String generatedModelName, boolean local) {
		logger.entering(this.getClass().getName(), "constructor");
		this.generatedModelName = generatedModelName;
		this.local = local;
		parameterSetter = new MyRepastParameterSetter();
		bc = new BatchController(parameterSetter);
		bc.setExitOnExit(false);
		listeners = new HashMap<IBatchListener, InternalBatchListener>();
	}
	
	public void addBatchListener(IBatchListener listener) {
		logger.entering(this.getClass().getName(), "addBatchListener");
		InternalBatchListener l = new InternalBatchListener(listener,bc);
		listeners.put(listener, l);
    	bc.addBatchListener(l);
	}

	public int getNumberOfRuns() {
//		return parameterSetter.getNumberOfRuns(); // it's too slow
		if (noRuns < 0) 
			noRuns = (int) Util.calculateNumberOfRuns(paramTree,false);
		return noRuns;
	}

	public void removeBatchListener(IBatchListener listener) {
		logger.entering(this.getClass().getName(), "removeBatchListener");
		bc.removeBatchListener(listeners.remove(listener));
	}

	public void setModelPath(String path) {
		modelPath = path;
	}

	public void setParameters(ParameterTree paramTree) {
		logger.entering(this.getClass().getName(), "setParameters");
		this.paramTree = paramTree;
		parameterSetter.setParameterTree(paramTree);
	}

	/**
	 * Not used. Model contains recorders.
	 */
	public void setRecorders(List<RecorderInfo> recorders) {
		// empty
	}

	/**
	 * Not used. Model contains conditions.
	 */
	public void setStopCondition(String conditionCode) {
		// empty
	}

	public void startBatch() {
		logger.entering(this.getClass().getName(), "startBatch");
		Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
		bc.begin();
	}

	public void stopBatch() {
		logger.entering(this.getClass().getName(), "stopBatch");
		bc.endSim();
	}

	public void stopCurrentRun() {
		logger.entering(this.getClass().getName(), "stopCurrentRun");
		bc.stopRun();
	}

	/**
	 * This is where real platform plugging into BAL happens.
	 * It eventually does what SimInit, except of starting the model.
	 */
	public void setEntryPoint(String entryPoint) throws InvalidEntryPointException {
		logger.entering(this.getClass().getName(), "setEntryPoint");
		Class<?> modelClass;
		try {
			String tmp = entryPoint.substring(modelPath.length() + 1).replace('/','.').replace('\\','.').replaceAll("\\.class","");
			if (!local)
				generatedModelName = tmp;
			if (!generatedModelName.startsWith(tmp))
				throw new InvalidEntryPointException("Entry path doesn't match to the model class");
			modelClass = Class.forName(generatedModelName);
			SimModel model = (SimModel) modelClass.newInstance();

			model.setController(bc);
			bc.setModel(model);
		} catch (Exception e) {
			throw new InvalidEntryPointException(e);
		}
	}
	
	//////////////////////////////////////////////
	//////////// INNER ///////////////////////////
	//////////////////////////////////////////////
	
	/**
	 * The heart of batch controller implementation. Knows how to translate
	 * BAL defined ParameterTree to the language of Repast.  
	 */
	class MyRepastParameterSetter implements ParameterSetter {
		List<AbstractParameterInfo> actualParams;
		Iterator<List<AbstractParameterInfo>> iterator;
		int runNumber = 0;
		boolean finished = false;
		
		public MyRepastParameterSetter() {
			logger.entering(this.getClass().getName(), "constructor");
		}
		
		public MyRepastParameterSetter(ParameterTree tree) {
			this();
			setParameterTree(tree);
		}
		
		public Hashtable<String, String> getDefaultModelParameters(SimModel model) {
			logger.entering(this.getClass().getName(), "getDefaultModelParameters");
			Hashtable<String, String> t = new Hashtable<String, String>();
			for ( AbstractParameterInfo p : actualParams ) {
				t.put(p.getName(), "" + p.iterator().next());
			}
			return t;
		}

		public ArrayList<String> getDynamicParameterNames() {
			logger.entering(this.getClass().getName(), "getDynamicParameterNames");
			ArrayList<String> l = new ArrayList<String>();
			for ( AbstractParameterInfo p : actualParams) {
				if ( !p.isOriginalConstant() ) {
					l.add(p.getName());
				}
			}
			return l;
		}

		public Object getParameterValue(String name, SimModel model) {
			logger.entering(this.getClass().getName(), "getParameterValue");
			for ( AbstractParameterInfo p : actualParams) {
				if ( p.getName().equals(name)) {
					return p.iterator().next();
				}
			}
			return null;
		}

		public boolean hasNext() {
			logger.entering(this.getClass().getName(), "hasNext");
			return !finished;
		}

		/**
		 * Empty.
		 */
		public void init(String filePath) throws IOException {}
		
		public void setParameterTree(ParameterTree tree) {
			logger.entering(this.getClass().getName(), "setParameterTree");
			finished = false;
//			iterator = new DefaultParameterPartitioner().partition(tree);
//			while ( iterator.hasNext() ) {
//				iterator.next();
//				runNumber++;
//			}
			runNumber = (int) Util.calculateNumberOfRuns(paramTree,false);

			iterator = new DefaultParameterPartitioner().partition(tree);
			// TODO: tree validation?
			actualParams = iterator.next();
		}

		public int getNumberOfRuns() {
			return runNumber;
		}
		
		public boolean isConstant(String name) {
			logger.entering(this.getClass().getName(), "isConstant");
			for ( AbstractParameterInfo p : actualParams) {
				if ( p.getName().equals(name) ) {
					return p.isOriginalConstant();
				}
			}
			return false;
		}

		public boolean isParameter(String name) {
			logger.entering(this.getClass().getName(), "isParameter");
			// TODO: RngSeed handling: is it necessary here, too?
			for ( AbstractParameterInfo p : actualParams) {
				if ( p.getName().equals(name) ) {
					return true;
				}
			}
			return false;
		}

		public Iterator<String> parameterNames() {
			logger.entering(this.getClass().getName(), "getParameterNames");
			return getParameterNames().iterator();
		}

		public void setModelParameters(SimModel model) {
			logger.entering(this.getClass().getName(), "setModelParameters");
			model.generateNewSeed();
		    Random.createUniform();
		    setParameters(model);
		}

		public void setNextModelParameters(SimModel model) {
			logger.entering(this.getClass().getName(), "setNextModelParameters");
			if (iterator.hasNext()) {
				actualParams = iterator.next(); 
				setModelParameters(model);
			} else
				finished = true;
		}

		private void setParameters(SimModel model) {
			logger.entering(this.getClass().getName(), "setParameters");
			fillMethodTable(model);
			Method m;
			for ( AbstractParameterInfo p : actualParams ) {
				m = methods.get(p.getName());
				Invoker invoker = InvokerFactory.createInvoker(
						m.getParameterTypes()[0], 
						model, m, 
						"" + p.iterator().next());
				try {
					invoker.execute();
				} 
				catch (Exception ex) {
					// TODO: what else; ParmeterSetter interface does not allow checked
					throw new RuntimeException("Unable to set model parameter " + p.getName(), ex);
				}
			}
		}
		
		private Map<String, Method> methods = new HashMap<String, Method>();
		
		private void fillMethodTable(Object obj) {
			methods.clear();
			Method[] ms = obj.getClass().getMethods();
			List<String> names = getParameterNames();
			Method m;
			for (int i = 0, n = ms.length; i < n; i++) {
				m = ms[i];
				String name = m.getName();
				if (name.startsWith("set") && m.getParameterTypes().length == 1) {
					String propName = name.substring(3);
					if ( names.contains(propName)) {
						methods.put(propName, m);
					}
				}
			}
		}
		
		private List<String> getParameterNames() {
			List<String> names = new ArrayList<String>();
			for  ( AbstractParameterInfo p : actualParams ) {
				names.add(p.getName());
			}
			return names;
		}
	}

	////////////////////////////////////////////
	
	/**
	 * A class that aggregates a BAL listener and forwards those events to it that
	 * the Repast controller wrapped by BAL produces. 
	 */
	class InternalBatchListener implements uchicago.src.sim.engine.BatchListener {
		private IBatchListener listener; 
		private BatchController bc;
		
		public InternalBatchListener(IBatchListener listener, BatchController bc) {
			this.listener = listener;
			this.bc = bc;
		}
		
		public void batchEventPerformed(uchicago.src.sim.engine.BatchEvent e) {
			if (e.getType() == uchicago.src.sim.engine.BatchEvent.BATCH_FINISHED) 
				listener.batchEnded(new BatchEvent(e.getSource(),BatchEvent.EventType.BATCH_ENDED));
			else if (e.getType() == uchicago.src.sim.engine.BatchEvent.RUN_ENDED) 
				listener.runChanged(new BatchEvent(e.getSource(),BatchEvent.EventType.RUN_ENDED,bc.getRunCount()));
			else if (e.getType() == uchicago.src.sim.engine.BatchEvent.TICK_CHANGED) {
				listener.timeProgressed(new BatchEvent(e.getSource(),BatchEvent.EventType.STEP_ENDED,e.getTick()));
			}
		}
	}
}
