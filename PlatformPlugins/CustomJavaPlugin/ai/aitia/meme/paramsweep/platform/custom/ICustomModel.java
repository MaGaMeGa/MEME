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
package ai.aitia.meme.paramsweep.platform.custom;

import ai.aitia.meme.paramsweep.utils.SimulationException;

/** Interface for custom Java models. It enables for any model written in Java to use the capabilities of
 *  the MEME Parameter Sweep Wizard.   
 */
public interface ICustomModel {

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/** Returns the number value of the current step of the simulation. */
	public double getCurrentStep();
	
	//----------------------------------------------------------------------------------------------------
	/** Implement this method with empty body. IMPORTANT: You must call this method at the end of every 
	 *  step of your simulation.
	 */
	public void stepEnded();
	
	//----------------------------------------------------------------------------------------------------
	/** This method must stop the entire simulation. IMPORTANT: You must use this method if your simulation
	 *  wants to stop itself. */
	public void simulationStop();
	
	//----------------------------------------------------------------------------------------------------
	/** Initializes and starts the simulation.
	 *  @throws SimulationException if any problem occurs during the initialization or simulation starting
	 */
	public void simulationStart() throws SimulationException; 
	
	//----------------------------------------------------------------------------------------------------
	public String[] getParams();
	
	//----------------------------------------------------------------------------------------------------
	/** ADDITIONAL REQUIREMENTS:
	 *  - Nullary constructor in the main class of the model. The constructor must set the default value of 
	 *    all parameters.<br>
	 *  - All variables in the main class of the model must be public, package private or at least protected.<br>
	 *  - For each name X contained by getParams() array you have to specify a public getX()/isX() (latter
	 *    only if X is a boolean parameter) and a public setX() method  in the model OR alternatively a
	 *    public Object getParameter(String) and a public setParameter(String,Object) method.
	 *    getParameter("X") must return the current value of parameter X while setParameter("X",value)
	 *    must set the value of parameter X to value.<br>
	 *  - About the names of parameters: In MEME,  'param' and 'Param' denotes the same
	 *    parameter because of the name conventions of getters/setters (e.g. getParam()). 
	 */
	
}
