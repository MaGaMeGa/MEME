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

import java.io.Serializable;

import javax.swing.tree.DefaultMutableTreeNode;

import ai.aitia.meme.paramsweep.batch.IParameterSweepResultReader;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;

/**
 * Interface for the IntelliSweep dynamic method plugins.
 * @author Ferschl
 */
public interface IIntelliDynamicMethodPlugin extends IIntelliMethodPlugin, Serializable {
	
	/**
	 * Returns the parameter tree to be run in the next iteration. Should avoid re-running already completed 
	 * parameter combinations. Uses the provided IParameterSweepResultReader instance to read the previous 
	 * results.
	 * Returns null if the method does not need more runs, or if the maximum number of iterations is exceeded.
	 * @param reader	The reader to read the previous results.
	 * @return	The parameter tree in the next iteration, or null if maximum number of iterations is exceeded, 
	 * or there is no need to run more iterations.
	 */
	public ParameterTree getNextParameterTree(IParameterSweepResultReader reader);
	
	/**
	 * Returns if the method has any more iterations.
	 * @return true if there are no more iterations in the experiment, false otherwise.
	 */
	public boolean noMoreIteration();
	
	/**
	 * The tree of recordable variables, that is used in the method to calculate the parameter tree in the next 
	 * iteration. As this remains the same between runs, it needs to be set once per experiment.  
	 * @param root	The recorder tree root that can be obtained from the ParameterSweepWizard instance.
	 */
	public void setRecordableVariables( DefaultMutableTreeNode root );
	
	/**
	 * Checks the settings of the method for correct settings, including e.g.: is there a recorder that records 
	 * something that the method can evaluate and create the next iteration? Is there a parameter that varies 
	 * between runs, or there are only constant (or no) parameters? Are the settings of the method valid?
	 * @param recorders	The root of the recorder tree.
	 * @return	null if settings are OK, an error message if not.
	 */
	public String settingsOK( DefaultMutableTreeNode recorders );
	
	/**
	 * A method to transfer the parameter tree of the model to the method.
	 * @param root	The parameter tree root of the model.
	 */
	public void setParameterTreeRoot( DefaultMutableTreeNode root);
	
	/**
	 * Returns the maximum number of iterations.
	 * @return	the maximum number of iterations.
	 */
	public int getNumberOfIterations();
	
	/**
	 * Returns the actual number of iterations.
	 * @return	the actual number of iterations.
	 */
	public int getCurrentIteration();
}
