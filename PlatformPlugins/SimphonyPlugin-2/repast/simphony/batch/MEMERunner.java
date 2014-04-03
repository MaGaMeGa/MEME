package repast.simphony.batch;
/**
 * 
 */


import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import repast.simphony.context.Context;
import repast.simphony.engine.controller.Controller;
import repast.simphony.engine.controller.DefaultController;
import repast.simphony.engine.environment.AbstractRunner;
import repast.simphony.engine.environment.ControllerRegistry;
import repast.simphony.engine.environment.DefaultRunEnvironmentBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunEnvironmentBuilder;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.Schedule;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.watcher.WatcheeInstrumentor;
import repast.simphony.parameter.DefaultParameters;
import repast.simphony.scenario.ScenarioLoadException;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.paramsweep.platform.simphony2.ISimphony2Model;
import ai.aitia.meme.paramsweep.utils.SimulationException;

/**
 * @author Tamas
 * 
 */
public abstract class MEMERunner extends AbstractRunner implements ISimphony2Model {

	private RunEnvironmentBuilder runEnvironmentBuilder;
	private Controller controller;
	private boolean pause = false;
	private Object monitor = new Object();
	private ISchedule schedule;
	protected Context<?> context;
	
	protected DefaultParameters parameters = new DefaultParameters();
	private boolean stop;
	private static ControllerRegistry registry;


	public MEMERunner() {
		runEnvironmentBuilder = new DefaultRunEnvironmentBuilder(this, true);
		controller = new DefaultController(runEnvironmentBuilder);
		controller.setScheduleRunner(this);
	}

	public abstract String getScenarioDirName();
	
	public boolean load(File scenarioDir) throws Exception {
		if (registry == null){
			if (scenarioDir.exists()) {
				BatchScenarioLoader loader = new BatchScenarioLoader(scenarioDir);
				registry = loader.load(runEnvironmentBuilder);
			} else {
				MEMEApp.logError("Scenario not found", new IllegalArgumentException(
						"Invalid scenario " + scenarioDir.getAbsolutePath()));
				return false;
			}
		}

		controller.setControllerRegistry(registry);
		controller.batchInitialize();
		controller.runParameterSetters(parameters);
		
		return true;
	}

	public void runInitialize() {
		controller.runInitialize(parameters);
		RunState rs = RunState.getInstance();
		schedule = rs.getScheduleRegistry().getModelSchedule(); // get the
																// schedule
		context = (Context<?>) rs.getMasterContext(); // get the main context of
													// the model
	}

	public void cleanUpRun() {
		controller.runCleanup();
	}

	public void cleanUpBatch() {
		controller.batchCleanup();
	}

	// returns the tick count of the next scheduled item
	public double getNextScheduledTime() {
		return ((Schedule) RunEnvironment.getInstance().getCurrentSchedule())
				.peekNextAction().getNextTime();
	}

	// returns the number of model actions on the schedule
	public int getModelActionCount() {
		return schedule.getModelActionCount();
	}

	// returns the number of non-model actions on the schedule
	public int getActionCount() {
		return schedule.getActionCount();
	}

	// Step the schedule
	public void step() {
		schedule.execute();
	}

	// stop the schedule
	public void stop() {
		if (schedule != null)
			schedule.executeEndActions();
	}

	public void setFinishing(boolean fin) {
		schedule.setFinishing(fin);
	}

	public void execute(RunState toExecuteOn) {
		// required AbstractRunner stub. We will control the
		// schedule directly.
	}

//	public static void main(String[] args) {
//		System.err.println(new MEMERunner().getModelActionCount());
//	}
	
	//-------------------------- ISimphony2Model methods ---------------------------------------------
	/* (non-Javadoc)
	 * @see ai.aitia.meme.paramsweep.platform.custom.ICustomModel#getCurrentStep()
	 */
	@Override
	public double getCurrentStep() {
		return RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
	}

	/* (non-Javadoc)
	 * @see ai.aitia.meme.paramsweep.platform.custom.ICustomModel#getParams()
	 */
	@Override
	public String[] getParams() {
		Iterable<String> parameterNames = parameters.getSchema().parameterNames();
		ArrayList<String> parameterNameList = new ArrayList<String>();
		for (String name : parameterNames) {
			parameterNameList.add(name);
		} 
		
		return parameterNameList.toArray(new String[parameterNameList.size()]);
	}

	/* (non-Javadoc)
	 * @see ai.aitia.meme.paramsweep.platform.custom.ICustomModel#simulationStart()
	 */
	@Override
	public void simulationStart() throws SimulationException {
		try {

			WatcheeInstrumentor.getInstrumented().add(getClass().getName());
			WatcheeInstrumentor.getInstrumented().add(getClass().getSuperclass().getName());
			WatcheeInstrumentor.getInstrumented().add("repast.simphony.batch.MEMERunner");
			// TODO we must search the classpath for *BatchGenerated* classes
			// and put them on the instrumented list!
//			URI classUri = getClass().getResource("/").toURI();
//			File scenarioDir = new File(new File(classUri).getParentFile().getAbsolutePath(), "ElFarol.rs");
			if (!load(new File(getScenarioDirName()))){
				throw new SimulationException("Could not load the scenario!");
			}

			
			runInitialize();

			while (getActionCount() > 0){
				if (getModelActionCount() == 0){
					setFinishing(true);
				}
						
				step();
					
				stepEnded();
					
				if (stop){
					setFinishing(true);
					break;
				}
			}
				
			stop();
			cleanUpRun();
			cleanUpBatch();
			
		} catch (URISyntaxException e){
			throw new SimulationException(e);
		} catch (ScenarioLoadException e) {
			throw new SimulationException(e);
		} catch (Exception e) {
			throw new SimulationException(e);
		}
	}

	/* (non-Javadoc)
	 * @see ai.aitia.meme.paramsweep.platform.custom.ICustomModel#simulationStop()
	 */
	@Override
	public void simulationStop() {
		stop = true;
	}

	/* (non-Javadoc)
	 * @see ai.aitia.meme.paramsweep.platform.custom.ICustomModel#stepEnded()
	 */
	@Override
	@ScheduledMethod(start=1, interval=1, priority=1)
	public void stepEnded() {
	}
	
	public void setParameter(final String name, final Object value){
		parameters.setValue(name, value);
	}
	
	public Object getParameter(final String name){
		return parameters.getValue(name);
	}	

}
