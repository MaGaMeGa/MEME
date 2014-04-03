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

import ai.aitia.meme.paramsweep.PsException;
import ai.aitia.meme.paramsweep.batch.output.NonRecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;

/** Provides basic informations about the model for the Parameter Sweep Wizard. 
 *  When an method throws checked exceptions, those have to be converted to 
 *  ModelInformationException first.
 */
public interface IModelInformation {
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	/** Returns the model's name. */
	public String getName();
	
	//----------------------------------------------------------------------------------------------------
	/** Returns the parameters of the model. The type of the parameters must be
	 *  one of the following:<br>
	 *  - Byte<br>
	 *  - Short<br>
	 *  - Integer<br>
	 *  - Long<br>
	 *  - Float<br>
	 *  - Double<br>
	 *  - Boolean<br>
	 *  - String<br>
	 *  See <code>ParameterInfo</code> for further details. 
	 *  @throws ModelInformationException
	 */
	public List<ParameterInfo<?>> getParameters() throws ModelInformationException;
	
	//----------------------------------------------------------------------------------------------------
	/** Returns all possible entities of the model that can be collected as a result value. For
	 *  example, using RepastJ, the returned list contains all variables with appropriate type 
	 *  (except parameters) and all methods with no parameters and appropriate return value
	 *  of the model. The previously mentioned 'appropriate' types are the number types (byte,
	 *  Byte, short, Short, int, Integer, long, Long, float, Float, double, Double), logical
	 *  types (boolean, Boolean) and Strings.<br>
	 *  This type constraint applies all platforms, not just RepastJ. See <code>RecordableInfo</code>
	 *  for further details.       
	 *  @throws ModelInformationException
	 */
	public List<RecordableInfo> getRecordables() throws ModelInformationException;
	
	//----------------------------------------------------------------------------------------------------
	/** Returns all possible entities of the model that can also be a source (besides the recordables)
	 * 	of a statistic/script. For example, at RepastJ, the returned list contains all variables and
	 *  all methods that cannot be recordables. See <code>NonRecordableInfo</code> and <code>
	 *  NonRecordableFunctionInfo</code> for details.
	 *  If the given platform doesn't support scripting the result list can be empty. 
	 *  @throws ModelInformationException
	 */
	public List<NonRecordableInfo> getNonRecordables() throws ModelInformationException;
	
	public List<RecorderInfo> getRecorders() throws ModelInformationException;
	
	public String getRecordersXML() throws ModelInformationException;
	
	//====================================================================================================
	// nested class
	
	//----------------------------------------------------------------------------------------------------
	/** Thrown to indicate problems that may occur during model analysis. */
	@SuppressWarnings("serial")
	public static class ModelInformationException extends PsException {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public ModelInformationException() { super(); }
		public ModelInformationException(String msg) { super(msg); }
		public ModelInformationException(Throwable t) { super(t); }
		public ModelInformationException(String msg, Throwable t) {	super(msg,t); }
	}
}
