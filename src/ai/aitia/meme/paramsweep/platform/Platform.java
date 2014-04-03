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
package ai.aitia.meme.paramsweep.platform;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import ai.aitia.meme.paramsweep.batch.IBatchController;
import ai.aitia.meme.paramsweep.batch.IModelInformation;
import ai.aitia.meme.paramsweep.batch.IParameterSweepResultReader;
import ai.aitia.meme.paramsweep.batch.IParameterPartitioner;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;

/** Interface that represents a simulation platform for the MEME. */  
public interface Platform extends Serializable {
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/** The displayable name of the platform. */
	public String getDisplayableName();
	
	//----------------------------------------------------------------------------------------------------
	/** The version of the platform (not the version of implementation class but the platform itself, e. g.
	 *  The version of RepastJ 3.1 platform is 3.1).
	 */
	public String getVersion();
	
	//----------------------------------------------------------------------------------------------------
	/** A short description of the platform. */
	public String getDescription();
	
	
	//----------------------------------------------------------------------------------------------------
	/** This method provides information about the model by analyzing
	 *  what is known to the platform using the supplied parameter 
	 *  (e.g. inputs from user). See <code>IPSWInformationProvider</code>
	 *  for details.
	 */
	public IModelInformation getModelInformation(IPSWInformationProvider provider);
	
	//----------------------------------------------------------------------------------------------------
	/** This method checks whether the model specified by the 
	 *  parameter is a valid model for the platform or not. The
	 *  return value is an error message or 'null' when the model
	 *  is valid. See <code>IPSWInformationProvider</code>
	 *  for details.
	 */
	public String checkModel(IPSWInformationProvider provider);
	
	//----------------------------------------------------------------------------------------------------
	/** Checks whether the <code>condition</code> string contains a valid condition.
	 *  Returns 'null' when the condtion is valid; returns
	 *  an error message otherwise. See <code>IPSWInformationProvider</code>
	 *  for details.
	 */
	public String checkCondition(String condition, IPSWInformationProvider provider);

	//----------------------------------------------------------------------------------------------------
	/** This method is called just before running the simulation. The platform can perform here some 
	 *  initializing operations (e.g. RepastJ generates a derived model that 
	 *  contains all recorders). See <code>IPSWInformationProvider</code> for details.
	 */
	public String prepareModel(IPSWInformationProvider provider); 
	
	//----------------------------------------------------------------------------------------------------
	/** Returns a batch controller object that executes the batch run of the model. */
	public IBatchController getBatchController();
	
	//----------------------------------------------------------------------------------------------------
	/** Defines the strategy of how parameters are to be combined. */
	public IParameterPartitioner getParameterPartitioner();
	
	//----------------------------------------------------------------------------------------------------
	/** Returns a reader that can read simulation results from platform-specific output files
	 *  for iterative plugins (e.g. IIntelliDynamicMethodPlugin).
	 * @param recorders 
	 */
	public IParameterSweepResultReader getReader(List<RecorderInfo> recorders);
	
	//----------------------------------------------------------------------------------------------------
	/** For each output file in recorders searches the <code>workingDir</code> for 
	  * result file parts (ends with 'partX', where 'X' is a number), and merges them
	  * into a new file (in the working directory) that has the base file name.
	  */
	public List<File> prepareResult(List<RecorderInfo> recorders, File workingDir);
	
	//----------------------------------------------------------------------------------------------------
	/** For each output file in recorders searches the <code>workingDir</code> for
	 *  result file parts ends with the specified suffixes, and merges them
	 *  into a new file (in the working directory) that has the base file name.
	 */
	public List<File> prepareResult(List<RecorderInfo> recorders, List<String> suffixes, File workingDir);
	
	//----------------------------------------------------------------------------------------------------
	/** Returns a new instance of the result line tool for the platform, which can be used to manipulate 
	 * lines of the result file, mostly to add an offset to the run# when working with distributed experiments.
	 * Creates a new instance on every call, as the result line tool is a state machine, a clean starting state 
	 * is necessary for every new result file.   
	 */
	public IResultFileTool getResultFileTool();
}
