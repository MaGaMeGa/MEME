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
package ai.aitia.meme.paramsweep.batch;

import java.util.List;

import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;

/** The model executor component of the batch abstraction layer. When an execution
 *  controller method throws checked exceptions, those have to be converted to BatchException 
 *  first.
 */
public interface IBatchController {
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/** Sets the path to the model directory. This information is used
	 *  to locate the model to be run.
	 */
	public void setModelPath(String path);
	
	//----------------------------------------------------------------------------------------------------
	/** This information is used to determine what
	 *  to run. For example using RepastJ <code>entryPoint</code> is the 
	 *  path of the class file of the model.<br>
	 *  This method may throw InvalidEntryPointException if the parameter
	 *  value (besides the model path) is not enough to identify the model or
	 *  the format of the parameter is invalid (e.g. using RepastJ the 
	 *  <code>entryPoint</code>'s tail must be '.class'). 
	 *  
	 * @throws InvalidEntryPointException 
	 */
	public void setEntryPoint(String entryPoint) throws InvalidEntryPointException;
	
	//----------------------------------------------------------------------------------------------------
	/** Sets the parameter tree of a batch run. The tree has to be decomposed
	 *  to a set of parameter combinations. Each member of this set defines
	 *  a single simulation run.<br>
	 *  The <code>DefaultParameterPartitioner</code> class is an implementation of 
	 *  the tree decomposer. It can be found in the 
	 *  <code>ai.aitia.meme.paramsweep.util</code> package.
	 */
	public void setParameters(ParameterTree paramTree);
	
	//----------------------------------------------------------------------------------------------------
	/** Sets outputters (or recorders) for the batch run. Each object in the list
	 *  describes an outputter that produces one result file in a platform-specific
	 *  format. See <code>RecorderInfo</code> class for details.
	 *  
	 */   
	public void setRecorders(List<RecorderInfo> recorders);
	
	//----------------------------------------------------------------------------------------------------
	/** Sets the stopping condition for the model in textual format. The interpretation of this 
	 *  condition is platform-specific.<br>
	 *  For example using RepastJ <code>conditionCode</code> could be a number or a logical expression.
	 *  A number specifies the time step at runs will be stopped � it can assign a constant value, 
	 *  variable with appropriate type or a method with appropriate return type. A logical expression
	 *  is also built from the model�s variables and methods. The simulation will be stopped when the 
	 *  condition becomes true.
	 */ 
	public void setStopCondition(String conditionCode);
	
	//----------------------------------------------------------------------------------------------------
	/** Starts the batch run of the model.
	 *  @throws BatchException
	 */
	public void startBatch() throws BatchException;
	
	//----------------------------------------------------------------------------------------------------
	/** Stops the batch run of the model.
	 *  @throws BatchException
	 */
	public void stopBatch() throws BatchException;
	
	//----------------------------------------------------------------------------------------------------
	/** Stops the current run of the model and starts the next one. If there's no more
	 *  runs left the batch ends.
	 * @throws BatchException
	 */
	public void stopCurrentRun() throws BatchException;
	
	//----------------------------------------------------------------------------------------------------
	/** Returns the total number of runs. This can be calculated from the 
	 *  parameter tree.<br>
	 *  Precondition: <code>setParameters</code> called before.
	 */ 
	public int getNumberOfRuns();
	
	//----------------------------------------------------------------------------------------------------
	/** Registers a listener to the controller. See <code>IBatchListener</code> and 
	 *  <code>BatchEvent</code> for details. 
	 */
	public void addBatchListener(IBatchListener listener);
	
	//----------------------------------------------------------------------------------------------------
	/** Removes a listener. See <code>IBatchListener</code> and <code>BatchEvent</code>
	 *  for details. 
	 */
	public void removeBatchListener(IBatchListener listener);
}
