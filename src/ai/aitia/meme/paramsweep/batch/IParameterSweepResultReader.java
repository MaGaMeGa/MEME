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

import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;

/** Interface to read simulation results for in-run processing
 *   plugins (e.g. IIntelliDynamicMethodPlugin).
 */
public interface IParameterSweepResultReader {
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/** Returns the simulation's results related to the specified - possibly partial - parameter
	 *  combination. <code>info</code> defines from which result column needs to collect.   
	 *  @throws ReadingException if any problem occurs during the output file processing
	 */
	public List<ResultValueInfo> getResultValueInfos(RecordableInfo info, List<ParameterInfo<?>> parameterCombination) throws ReadingException;
}
