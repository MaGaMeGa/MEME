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
package ai.aitia.meme.paramsweep.generator;

import java.io.Serializable;
import java.util.List;

/**
 * An interface for blocking methods. Blocking serves as an additional parameter of the model that is 
 * changing uncontrollably. This is to simulate outside effects, like date of experiment, 
 * daily mean temperature, etc. This interface can be used (but is not required) by the IntelliSweep 
 * methods to help with providing blocking options to the user.  
 * @author Ferschl
 *
 */
public interface IBlockingHelper extends Serializable{
	/**
	 * Limits the number of selectable blocking variables. 
	 * @return	The maximum possible number of variables permitted. 0 if unlimited.
	 */
	public int getMaxBlockingVariables();
	/**
	 * Returns the maximum number of blocking values for any blocking variable, if the number of blocking variables 
	 * is the argument.
	 * @param blockingVariableCount	Number of blocking variables.
	 * @return	The maximum number of blocking values for any blocking variable, 0 if unlimited.
	 */
	public int getBlockingVariableValueCountsFor(int blockingVariableCount);
	/**
	 * @return true, if blocking helper is used in the method, false, if not 
	 * (but blocking is possible in this case as well)
	 */
	public boolean isBlockingHelpSupported();
	/**
	 * Returns the possible values of the blocking variable in the argument. 
	 * @param info	The blocking info for which the possible values are returned.
	 * @return	A list of objects of the possible values of the given blocking info.
	 */
	public List<Object> getBlockingVariableValues(RngSeedManipulatorModel.BlockingParameterInfo info);
	/**
	 * Calculates the design size (without replication).
	 * @return	The design size (without replication).
	 */
	public int getDesignSize();
}
