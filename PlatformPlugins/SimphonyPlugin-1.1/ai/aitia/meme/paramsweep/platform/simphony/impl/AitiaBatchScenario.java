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
package ai.aitia.meme.paramsweep.platform.simphony.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import repast.score.SAgent;
import repast.score.SContext;
import repast.score.ScoreFactory;
import repast.simphony.batch.BatchScenario;
import repast.simphony.data.engine.DataGathererDescriptor;
import repast.simphony.data.engine.DataSetsActionCreator;
import repast.simphony.data.engine.DefaultDataGathererDescriptor;
import repast.simphony.data.engine.DefaultDataGathererDescriptorAction;
import repast.simphony.data.logging.outputter.FileOutputter;
import repast.simphony.data.logging.outputter.engine.DefaultOutputterDescriptorAction;
import repast.simphony.data.logging.outputter.engine.FileOutputterDescriptor;
import repast.simphony.data.logging.outputter.engine.OutputtersActionCreator;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.dataLoader.engine.DataLoaderCompositeActionCreator;
import repast.simphony.dataLoader.engine.DataLoaderControllerAction;
import repast.simphony.engine.controller.DefaultControllerRegistry;
import repast.simphony.engine.controller.ScheduledMethodControllerAction;
import repast.simphony.engine.controller.WatcherControllerAction;
import repast.simphony.engine.environment.ControllerAction;
import repast.simphony.engine.environment.ControllerRegistry;
import repast.simphony.engine.environment.RunEnvironmentBuilder;
import repast.simphony.engine.watcher.WatchAnnotationReader;
import repast.simphony.engine.watcher.WatcheeInstrumentor;
import repast.simphony.engine.watcher.WatcherTrigger;
import repast.simphony.parameter.ParameterSetter;
import repast.simphony.parameter.ParameterSweeper;
import repast.simphony.plugin.CompositeControllerActionCreator;
import repast.simphony.plugin.MiscCompositeActionCreator;
import repast.simphony.plugin.RandomCompositeActionCreator;
import repast.simphony.plugin.ScheduledActionsCreator;
import repast.simphony.scenario.ModelInitializer;

public class AitiaBatchScenario extends BatchScenario {
	
	//====================================================================================================
	// nested classes

	//----------------------------------------------------------------------------------------------------
	// stores the data about any added actions
	private static class ActionData {
		
		//====================================================================================================
		// members

		Object contextType;
		String parentAction;
		ControllerAction action;
		
		//====================================================================================================
		// methods

		//----------------------------------------------------------------------------------------------------
		public ActionData(ControllerAction action, Object contextType, String parentAction) {
			this.action = action;
			this.contextType = contextType;
			this.parentAction = parentAction;
		}
	}
	
	//====================================================================================================
	// members

	private Object masterID;
	private WatchAnnotationReader watchAnnotationReader = null;
	private ScheduledMethodControllerAction scheduleAction = null;
	private List<ActionData> data;
	private WatcheeInstrumentor instrumentor;
	//protected ParameterSweeper parameterSweeper;

	//protected ModelInitializer userInit = null;
	private boolean usingScoreFile = false;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public AitiaBatchScenario(Object masterContextID) {
		super(masterContextID);
		context = ScoreFactory.eINSTANCE.createSContext();
		context.setLabel(masterContextID.toString());
		init(masterContextID); 
	}

	//----------------------------------------------------------------------------------------------------
	public AitiaBatchScenario(File scenarioDir, SContext context, Object masterContextID) {
		super(scenarioDir, context, masterContextID);
		usingScoreFile = true;
		init(masterContextID);
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * Prepare the specified field in the specified class for watching. If this is not done, any
	 * watcher annotations will not work.
	 *
	 * @param className the name of the class to watch
	 * @param fieldName the name of the field to watch
	 */
	public void prepareWatchee(String className, String fieldName) {
		instrumentor.addFieldToWatch(className,fieldName);
		instrumentor.instrument();
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * Prepare the specified field in the specified class for watching. If this is not done, any
	 * watcher annotations will not work.
	 *
	 * @param className the name of the class to watch
	 * @param fieldName the name of the field to watch
	 */
	public void prepareWatchee(String className, String fieldName, String classpath) {
		instrumentor.addFieldToWatch(className,fieldName);
		instrumentor.instrument(classpath);
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * Adds a data loader to this scenario. The data loader will be associated with the specific
	 * context. If the contextTypeID refers to the master context, then this data loader is
	 * responsible for creating the master context and its children. If the contextTypeID refers to
	 * a subcontext type, then the data loader is responsible for creating the children of any
	 * contexts of that type.
	 *
	 * @param contextTypeID the context type to associate the data loader with
	 * @param loader        the loader to add to the scenario
	 */
	public <T extends ContextBuilder> void addDataLoader(Object contextTypeID, T loader) {
		// if we create the BatchScenario from a score file then
		// we have a valid SContext, otherwise we want to pass null
		SContext sContext = usingScoreFile ? this.getContext() : null;
		DataLoaderControllerAction<T> action = new DataLoaderControllerAction<T>("Data loader for " + contextTypeID,loader,sContext);
		ActionData actionData = new ActionData(action,contextTypeID,new DataLoaderCompositeActionCreator().getID());
		data.add(actionData);
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * Adds a data gatherer to the scenario. A data gatherer is responsible for retrieving data that
	 * can later be recorded to a file. The added DataGathererDescriptor is returned and the user
	 * can further customize it by adding data mappings that specify the exact data to record.
	 *
	 * @param contextTypeID the context to associate the DataGatherer with
	 * @param dataID        an unique id for this data gatherer
	 * @param agentClass    the class of agent this data gatherer will work with
	 * @return the added DataGathererDescriptor. The user can further customize it by adding data
	 *         mappings that specify the exact data to record.
	 */
	public <T> DataGathererDescriptor<T> addDataGatherer(Object contextTypeID, Object dataID, Class<T> agentClass) {
		DataGathererDescriptor<T> descriptor = new DefaultDataGathererDescriptor<T>();
		descriptor.setDataSetId(dataID);
		descriptor.setAgentClass(agentClass);
		ControllerAction action = new DefaultDataGathererDescriptorAction<T>(descriptor);
		ActionData actionData = new ActionData(action,contextTypeID,new DataSetsActionCreator().getID());
		data.add(actionData);
		return descriptor;
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * Adds a data logger to this scenario. A data logger takes data from a data gatherer add saves
	 * it to a file. The data id parameter specified which data gatherer the logger should get its
	 * data from. A FileOutputterDescriptor is returned and it can be used to further customize the
	 * logger, setting the file name and so forth.
	 *
	 * @param contextTypeID the context to associate this data logger with
	 * @param dataID        the id of the data gatherer that is the source of the data for this data logger.
	 * @return A FileOutputterDescriptor that can be used to further customize the logger, setting
	 *         the file name and so forth.
	 */
	public FileOutputterDescriptor addDataLogger(Object contextTypeID, Object dataID) {
		FileOutputterDescriptor fileDescriptor = new AitiaFileOutputterDescriptor();
		fileDescriptor.addDataSet(dataID);
		ControllerAction action = new DefaultOutputterDescriptorAction<FileOutputter, FileOutputterDescriptor>(fileDescriptor);
		ActionData actionData = new ActionData(action,contextTypeID,new OutputtersActionCreator().getID());
		data.add(actionData);
		return fileDescriptor;
	}


	//----------------------------------------------------------------------------------------------------
	/**
	 * Creates a ControllerRegistry from this BatchScenario. This registry is then used to do the
	 * actual batch runs.
     *
     * @param builder used to create the run enviroment and passed to a user's model initializer, if any
	 *
	 * @return a ControllerRegistry created from this BatchScenario.
	 * @throws ClassNotFoundException if this BatchScenario is unable process (read annotations etc.)
	 *                                the agent classes.
	 */
	public ControllerRegistry createRegistry(RunEnvironmentBuilder builder) throws ClassNotFoundException {
		processAgentClasses();
		ControllerRegistry registry = new DefaultControllerRegistry();
		registry.setMasterContextId(masterID);
		registerContextID(registry,context);

		addParentActions(registry);
		if (userInit != null) 
			userInit.initialize(this,builder);

		for (ActionData actionData : data) {
			ControllerAction parent = registry.findAction(actionData.contextType,actionData.parentAction);
			registry.addAction(actionData.contextType,parent,actionData.action);
		}

		registry.addAction(registry.getMasterContextId(),null,DEFAULT_MASTER_PARENT);
		for (ControllerAction action : masterActions) 
			registry.addAction(registry.getMasterContextId(),DEFAULT_MASTER_PARENT,action);

		if (parameterSweeper != null) {
			registry.addParameterSetter(parameterSweeper);
			for (ParameterSetter sweeper : paramSweepers) 
				parameterSweeper.add(null,sweeper);
		}

		ControllerAction scheduleRoot = registry.findAction(masterID,new ScheduledActionsCreator().getID());
		ControllerAction watcherAction = new WatcherControllerAction(watchAnnotationReader);
		registry.addAction(masterID,scheduleRoot,watcherAction);
		registry.addAction(masterID,scheduleRoot,scheduleAction);

		super.registry = registry;
		return registry;
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * Gets a custom ModelInitializer, if any, supplied by the user.
	 *
	 * @return a custom RunInitializer, if any, supplied by the user.
	 */
	@Override
	public ModelInitializer getModelInitializer() {
		if (userInit != null) 
			return userInit;
		else {
			try {
				return super.getModelInitializer();
			} catch (Exception ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
				return null;
			}
		}
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * Sets a ModelInitializer that will be run when the scenario is loaded. This is optional and
	 * need not be set unless a custom initializer is desired.
	 *
	 * @param userInit a customized ModelInitializer
	 */
	public void setUserInitializer(ModelInitializer userInit) { this.userInit = userInit; }
	public void setParameterSweeper(ParameterSweeper sweeper) {	this.parameterSweeper = sweeper; }
	public ParameterSweeper getParameterSweeper() {	return parameterSweeper; }
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void init(Object masterContextID) {
		this.masterID = masterContextID;
		this.data = new ArrayList<ActionData>();
		instrumentor = new WatcheeInstrumentor();
		WatcherTrigger.initInstance(instrumentor);
	}

	//----------------------------------------------------------------------------------------------------
	private void processAgentClasses() throws ClassNotFoundException {
		for (SAgent agent : context.getAllSubAgents()) {
			if (watchAnnotationReader == null) {
				watchAnnotationReader = new WatchAnnotationReader();
				scheduleAction = new ScheduledMethodControllerAction();
			}
			Class clazz = Class.forName(agent.getImplementation().getQualifiedName(),true,getClass().getClassLoader());
			watchAnnotationReader.processClass(clazz);
			scheduleAction.processAnnotations(clazz);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void registerContextID(ControllerRegistry registry, SContext parent) {
		for (SContext child : parent.getSubContexts()) {
			registry.addContextId(parent.getLabel(),child.getLabel());
			registerContextID(registry,child);
		}
	}

	//----------------------------------------------------------------------------------------------------
	// adds the parent actions through the various composite action creators
	private void addParentActions(ControllerRegistry registry) {
		List<CompositeControllerActionCreator> parentActionCreators = new ArrayList<CompositeControllerActionCreator>();
        parentActionCreators.add(new RandomCompositeActionCreator());
        parentActionCreators.add(new DataLoaderCompositeActionCreator());
        parentActionCreators.add(new ScheduledActionsCreator());
        parentActionCreators.add(new DataSetsActionCreator());
        parentActionCreators.add(new OutputtersActionCreator());
        parentActionCreators.add(new MiscCompositeActionCreator());

		for (CompositeControllerActionCreator creator : parentActionCreators) {
			ControllerAction action = creator.createControllerAction();
			registry.addAction(masterID,null,action);
			registry.registerAction(masterID,creator.getID(),action);
		}

		for (ActionData actionData : data) {
			Object contextID = actionData.contextType;
			for (CompositeControllerActionCreator creator : parentActionCreators) {
				ControllerAction action = creator.createControllerAction();
				registry.addAction(contextID,null,action);
				registry.registerAction(contextID,creator.getID(),action);
			}
		}
	}
}
