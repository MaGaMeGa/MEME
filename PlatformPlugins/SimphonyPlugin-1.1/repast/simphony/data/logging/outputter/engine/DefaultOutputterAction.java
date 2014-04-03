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
package repast.simphony.data.logging.outputter.engine;

import repast.simphony.data.DataConstants;
import repast.simphony.data.logging.LoggingFramework;
import repast.simphony.data.logging.LoggingInitializable;
import repast.simphony.data.logging.gather.LoggingRegistry;
import repast.simphony.data.logging.outputter.L4JFileOutputter;
import repast.simphony.data.logging.outputter.Outputter;
import repast.simphony.data.logging.outputter.OutputterRegisterer;
import repast.simphony.engine.controller.ControllerActionVisitor;
import repast.simphony.engine.environment.ControllerAction;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.parameter.Parameters;

/**
 * A ControllerAction that will handle setting up an Outputter. This provides a
 * set of methods that aid in the hooking of the Outputter into the logging
 * framework and is provides some Template methods (getOuputter, 
 * getScheduleParameters) that when overridden will allow for dynamic outputter
 * generation and so forth.
 *
 * @see #getOutputter()
 * @see #getScheduleParameters()
 * 
 * @author Jerry Vos
 * @version $Revision: 1.1 $ $Date: 2010-02-03 12:37:43 $
 */

/* 09/07/23: extends batchCleanup() with closing the outputter
 * modified by Rajmund Bocsi
 */

public class DefaultOutputterAction<T extends Outputter> implements
				ControllerAction, OutputterRegisterer {

	protected T outputter;

	protected ScheduleParameters scheduleParameters;

	public DefaultOutputterAction() {
		super();
	}

	/**
	 * If isBatchAction of the Outputter returns <em>true</em> then this will
	 * call register on the Outputter (the one returned by getOutputter()) with
	 * the ScheduleParameters returned by getScheduleParameters().
	 * 
	 * @see #register(RunState, Outputter, ScheduleParameters)
	 * 
	 * @param runState
	 *            the RunState to pass to the register function
	 * @param contextId
	 *            ignored
	 */
	public void batchInitialize(RunState runState, Object contextId) {
		if (getOutputter().isBatchAction()) {
			register(runState, getOutputter(), getScheduleParameters());
			if (getOutputter() instanceof LoggingInitializable) {
				((LoggingInitializable) getOutputter()).initialize(runState.getRunInfo());
			}
		}
	}

	/**
	 * If isBatchAction of the Outputter returns <em>false</em> then this will
	 * call register on the Outputter (the one returned by getOutputter()) with
	 * the ScheduleParameters returned by getScheduleParameters().
	 * 
	 * @see #register(RunState, Outputter, ScheduleParameters)
	 * 
	 * @param runState
	 *            the RunState to pass to the register function
	 * @param contextId
	 *            ignored
	 */
	public void runInitialize(RunState runState, Object contextId, Parameters params) {
		if (!getOutputter().isBatchAction()) {
			register(runState, getOutputter(), getScheduleParameters());
			if (getOutputter() instanceof LoggingInitializable) {
				((LoggingInitializable) getOutputter()).initialize(runState.getRunInfo());
			}
		}
	}

	/**
	 * If isBatchAction of the Outputter returns <em>false</em> then this will
	 * call unregister on the Outputter (the one returned by getOutputter()).
	 * 
	 * @see #unregister(RunState, Outputter)
	 * 
	 * @param runState
	 *            the RunState to pass to the unregister function
	 * @param contextId
	 *            ignored
	 */
	public void runCleanup(RunState runState, Object contextId) {
		if (outputter != null && !outputter.isBatchAction()) {
			unregister(runState, getOutputter());
		}
	}

	/**
	 * If isBatchAction of the Outputter returns <em>true</em> then this will
	 * call unregister on the Outputter (the one returned by getOutputter()).
	 * 
	 * @see #unregister(RunState, Outputter)
	 * 
	 * @param runState
	 *            the RunState to pass to the unregister function
	 * @param contextId
	 *            ignored
	 */
	public void batchCleanup(RunState runState, Object contextId) {
		if (outputter instanceof L4JFileOutputter) 
			((L4JFileOutputter)outputter).close();
		if (outputter != null && outputter.isBatchAction()) {
			unregister(runState, getOutputter());
		}
	}

	/**
	 * This performs 3 functions.
	 * 
	 * <ol>
	 * <li>
	 * Adds the given outputter to the RunState's logging registry with the
	 * specified scheduleParameters
	 * </li>
	 * <li>
	 * If the ScheduleParameters != null it schedules the Outputter to be 
	 * ran by the schedule with the given ScheduleParameters.
	 * </li>
	 * <li>
	 * Registers the outputter with the logging framework through  
	 * {@link LoggingFramework#register(Outputter, repast.simphony.engine.RunInfo)}.
	 * </li>
	 * </ol>
	 * 
	 * @param runState
	 *            the runState used to grab the needed registries and so forth
	 *            from
	 * @param outputter
	 *            the outputter to register
	 * @param scheduleParameters
	 *            the parameters used to schedule the Outputter's execution (may
	 *            be null).
	 */
	public void register(RunState runState, Outputter outputter,
	                     ScheduleParameters scheduleParameters) {
		LoggingRegistry registry = (LoggingRegistry) runState.getFromRegistry(DataConstants.REGISTRY_KEY);
		registry.add(outputter, scheduleParameters);
		if (scheduleParameters != null) {
			runState.getScheduleRegistry().getModelSchedule().schedule(
					scheduleParameters, outputter);
		}

		LoggingFramework.register(outputter, runState
				.getRunInfo());
	}

	/**
	 * This performs 2 functions.
	 * 
	 * <ol>
	 * <li>
	 * Removes the given outputter from the RunState's logging registry
	 * </li>
	 * <li>
	 * Unregisters the outputter with the logging framework through  
	 * {@link LoggingFramework#unregister(Outputter, repast.simphony.engine.RunInfo)}.
	 * </li>
	 * </ol>
	 * 
	 * @param runState
	 *            the runState used to grab the needed registries and so forth
	 *            from
	 * @param outputter
	 *            the outputter to register
	 */
	public void unregister(RunState runState, Outputter outputter) {
		LoggingRegistry registry = (LoggingRegistry) runState.getFromRegistry(DataConstants.REGISTRY_KEY);
		registry.remove(outputter);
		LoggingFramework.unregister(outputter, runState
				.getRunInfo());
	}

	/**
	 * Returns the ScheduleParameters that control when the outputter will have
	 * its execute method be called. This will generally be null since
	 * Outputters should output data when they have a full set of data, but this
	 * can represent things like telling a chart when to update itself.<p/>
	 * 
	 * This should be overriden by subclasses that want to generate the
	 * Outputter's settings.
	 * 
	 * @return when to execute the outputter
	 */
	public ScheduleParameters getScheduleParameters() {
		return scheduleParameters;
	}

	/**
	 * Sets the ScheduleParameters that control when the outputter will have its
	 * execute method be called.
	 * 
	 * @see #getScheduleParameters()
	 * 
	 * @param scheduleParameters
	 *            when to execute the outputter
	 */
	public void setScheduleParameters(ScheduleParameters scheduleParameters) {
		this.scheduleParameters = scheduleParameters;
	}

	/**
	 * Retrieves the Outputter this action will be working with.<p/>
	 * 
	 * This should be overridden by subclasses that want to generate the
	 * Outputter.
	 * 
	 * @return the Outputter this class is working with
	 */
	public T getOutputter() {
		return outputter;
	}

	/**
	 * Sets the Outputter this action will be working with.
	 * 
	 * @see #getOutputter()
	 * 
	 * @param outputter
	 *            the Outputter this class is working with
	 */
	public void setOutputter(T outputter) {
		this.outputter = outputter;
	}

	/**
	 * Accepts the specified visitor. This calls the default
	 * visit method.
	 *
	 * @param visitor the visitor to accept
	 */
	public void accept(ControllerActionVisitor visitor) {
		visitor.visit(this);
	}
}
