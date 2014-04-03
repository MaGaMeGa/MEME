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
package ai.aitia.meme.paramsweep.plugin;

import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;

/**
 * A common interface for optimization plugins. Optimization plugins
 * may be encapsulated by other methods (e.g. by Active Nonlinear Tests).  
 * These methods may have to access the encapsulated method's state in manners 
 * the {@link ai.aitia.meme.paramsweep.plugin.IIntelliMethodPlugin) interface 
 * cannot provide. 
 * */
public interface IOptimizationMethodPlugin extends IIntelliDynamicMethodPlugin {
	/**
	 * Returns the current optimal value.
	 * 
	 * @return the optimal fitness value
	 */
	public Object getOptimalValue();
	
	/**
	 * Sets the fitness function of the method.
	 * 
	 * @param function is the new fitness function
	 */
	public void setFitnessFunction( RecordableInfo function );

	/**
	 * Returns the fitness function of the method.
	 * 
	 * @return the current fitness function of the method
	 */
	public RecordableInfo getFitnessFunction();

}
