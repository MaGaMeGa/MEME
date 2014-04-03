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
package ai.aitia.meme.paramsweep.intellisweepPlugin;

import javax.swing.JDialog;

import ai.aitia.meme.paramsweep.batch.IParameterSweepResultReader;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;

/**
 * Defines an interface for the dynamic plugin to get the next parameter tree.
 * Classes implementing this interface should be able to create a new parameter
 * tree for the next iteration based on the previously gathered data. 
 * 
 * @author Attila Szabo, AITIA International Zrt.
 *
 */
public interface IDynamicParamTreeGenerator {
	
	/**
	 * Creates the parameter tree for the next iteration.
	 * 
	 * @param reader guarantees access to the gathered data.
	 * @return the next parameter tree.
	 */
	ParameterTree getNextParameterTree( IParameterSweepResultReader reader );
	
	/**
	 * Set the statistical operator to be applied in case of randomized 
	 * experiments.
	 * 
	 * @param code is the code of the operator.
	 */
	void setStatisticalOperator( int code );
	
	/**
	 * Provides a GUI to adjust generator specific settings.
	 * 
	 * @return the settings dialog object.
	 */
	JDialog getSettingsDialog();
}
