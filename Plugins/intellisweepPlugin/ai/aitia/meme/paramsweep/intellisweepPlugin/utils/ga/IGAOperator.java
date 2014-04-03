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
package ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga;

import java.io.Serializable;
import java.util.List;

import javax.swing.JPanel;

import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.jgap.GAOperationException;

/**
 * Interface for the genetic operators.
 * 
 * @author Attila Szabo
 *
 */
public interface IGAOperator extends Serializable {
	/**
	 * 
	 * @param population is the previous population.
	 * @param nextPopulation is the next population. Operators modifies this.
	 * @param maximizeFitness should be true if higher fitness is desirable, false otherwise
	 */
	public void operate( List<Chromosome> population, List<Chromosome> nextPopulation, boolean maximizeFitness )
													throws GAOperationException;
	
	/**
	 * 
	 * @return the operator's name.
	 */
	public String getName();
	
	/**
	 * 
	 * @return the  operator package's name (e.g. JGAP).
	 */
	public String getPackageName();
	
	/**
	 * 
	 * @return a short description of the operator.
	 */
	public String getDescription();

	/**
	 * 
	 * @return the operator's settings panel.
	 */
	public JPanel getSettingspanel();
	
	/**
	 * 
	 * @return error string: null when everything is OK, informative message
	 * else
	 */
	public String saveSettings();
}
