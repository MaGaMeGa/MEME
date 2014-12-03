/*******************************************************************************
 * Copyright (C) 2006-2014 AITIA International, Inc.
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
package ai.aitia.meme.paramsweep.intellisweepPlugin.jgap;

import java.util.List;

import javax.swing.tree.DefaultTreeModel;

import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.IGAOperatorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.IGASelectorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.gene.ParameterOrGene;

/**
 * @author Tamás Máhr
 *
 */
public interface GASearchPanelModel {
	
	
	public enum FitnessFunctionDirection {
		MINIMIZE,
		MAXIMIZE
	}
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * The name of model methods that can be used as fitness functions.
	 * @return a list of method names
	 */
	public List<RecordableInfo> getFitnessFunctions();

	//----------------------------------------------------------------------------------------------------
	/**
	 * Adds a new fitness function to the model. The object can be of any class. The {@link #getFitnessFunctions()} method returns the list of objects
	 * added by this method. The implementation of this method should notify {@link ModelListener} instances by invoking the
	 * {@link ModelListener#fitnessFunctionAdded()} method.
	 * 
	 * @param fitnessFunction
	 *            a fitness function of any class.
	 */
	public void addFitnessFunction(final RecordableInfo fitnessFunction);
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * Returns the fitness function name that should be selected in the list of fitness function names initially.
	 * 
	 * @return the default fitness function name
	 */
	public RecordableInfo getSelectedFitnessFunction();
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * When the user selects one of the fitness functions in the list, it is set in the model by calling this function.
	 * 
	 * @param fitnessFunctionName the selected fitness function name
	 */
	public void setSelectedFitnessFunction(final RecordableInfo fitnessFunction);
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * Removes all fitness functions from the model. The implementation of this method should notify {@link ModelListener} instances by invoking the
	 * {@link ModelListener#fitnessFunctionsRemoved()} method.
	 */
	public void removeAllFitnessFunctions();
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * Returns a list of selection operators. These operators will be displayed as selection operators, and their preferences panel will be displayed.
	 * 
	 * @return a list of selection operators
	 */
	public List<IGASelectorConfigurator> getSelectionOperators();

	//----------------------------------------------------------------------------------------------------
	/**
	 * Returns a list of genetic operators. These operators will be displayed as genetic operators, and their preferences panel will be displayed.
	 * 
	 * @return a list of genetic operators
	 */
	public List<IGAOperatorConfigurator> getGeneticOperators();

	//----------------------------------------------------------------------------------------------------
	/**
	 * Returns the list of genetic operators that are selected by the user.
	 * 
	 * @return the list of selected genetic operators
	 */
	public List<IGAOperatorConfigurator> getSelectedGeneticOperators();

	//----------------------------------------------------------------------------------------------------
	/**
	 * Returns the list of selection operators that are selected by the user.
	 * 
	 * @return the list of selected selection operators
	 */
	public List<IGASelectorConfigurator> getSelectedSelectionOperators();

	//----------------------------------------------------------------------------------------------------
	/**
	 * When the user selects a selection operator on the GUI, it is communicated to the model by calling this method.
	 * 
	 * @param operator the selection operator that has been selected by the user
	 */
	public void setSelectedSelectionOperator(final IGASelectorConfigurator operator);
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * When the user selects a genetic operator on the GUI, it is communicated to the model by calling this method.
	 * 
	 * @param operator the genetic operator that has been selected by the user
	 */
	public void setSelectedGeneticOperator(final IGAOperatorConfigurator operator);
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * When the user deselects a selection operator on the GUI, it is communicated to the model by calling this method.
	 * 
	 * @param operator the selection operator that has been deselected by the user
	 * @return true if the operator was present in the selectedSelectionOperator list before, false otherwise
	 */
	public boolean unsetSelectedSelectionOperator(final IGASelectorConfigurator operator);
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * When the user deselects a genetic operator on the GUI, it is communicated to the model by calling this method.
	 * 
	 * @param operator the genetic operator that has been deselected by the user
	 * @return true if the operator was present in the selectedGeneticOperator list before, false otherwise
	 */
	public boolean unsetSelectedGeneticOperator(final IGAOperatorConfigurator operator);
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * Returns the value that should be set in the 'Population size' input initially.
	 * 
	 * @return the default population size
	 */
	public int getPopulationSize();
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * When the user changes the 'Population size' input, the selected value is set in the model by calling this method.
	 * 
	 * @param populationSize the population size as input by the user
	 */
	public void setPopulationSize(final int populationSize);
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * Returns the value that has been set in the 'Random seed' input.
	 * 
	 * @return the default population random seed
	 */
	public int getPopulationRandomSeed();
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * When the user changes the 'Random seed' input, the selected value is set in the model by calling this method.
	 * 
	 * @param seed the population random seed as input by the user
	 */
	public void setPopulationRandomSeed(final int seed);
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * Returns the value that should be set in the 'Number of generations' input initially. If positive, then this input will be active and the
	 * fitness limit disabled. If negative, this input will be disabled and the fitness limit enabled.
	 * 
	 * @return the default number of generations
	 */
	public int getNumberOfGenerations();
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * When the user changes the 'Number of generations' input, the selected value is set in the model by calling this method.
	 * 
	 * @param numberOfGenerations the number of generations as input by the user
	 */
	public void setNumberOfGenerations(final int numberOfGenerations);
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * Returns the value that should be set in the 'Fitness limit' input initially.
	 * 
	 * @return the default fitness limit
	 */
	public double getFitnessLimitCriterion();
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * When the user changes the 'Fitness limit' input, the selected value is set in the model by calling this method.
	 * 
	 * @param fitnessLimit the fitness limit criterion as input by the user
	 */
	public void setFitnessLimitCriterion(final double fitnessLimit);
	
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * Returns the value that governs whether 'Minimize' or 'Maximize' is selected in the Fitness panel initially.
	 * 
	 * @return the default fitness direction
	 */
	public FitnessFunctionDirection getFitnessFunctionDirection();
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * When the user selects either 'Minimize' or 'Maximize', the selected value is set in the model by calling this method.
	 * 
	 * @param direction the selected optimization direction
	 */
	public void setFitnessFunctionDirection(final FitnessFunctionDirection direction);
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * Returns the treemodel that is displayed in the Chromosome section of the GUI.
	 * 
	 */
	public DefaultTreeModel getChromosomeTree();
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * Adds the parameter (a gene or a constant parameter) to the model. This method can be used to initialize the chromosome tree. The implementation of
	 * this method should notifiy {@link ModelListener} instances by invoking the {@link ModelListener#parameterAdded()} method.
	 */
	public void addParameter(final ParameterOrGene parameterOrGene);

	//----------------------------------------------------------------------------------------------------
	/**
	 * Removes all genes and constant parameters from the model. The implementation of this method should notify {@link ModelListener} instances by invoking
	 * the {@link ModelListener#parametersRemoved()} method. 
	 */
	public void removeAllParameters();
	
	//----------------------------------------------------------------------------------------------------
	public boolean addModelListener(final ModelListener listener);
	
	//----------------------------------------------------------------------------------------------------
	public boolean removeModelListener(final ModelListener listener);
	
	//----------------------------------------------------------------------------------------------------
	public boolean canBeFitnessFunction(final RecordableInfo candidate); //TODO: kell ez?
	
	//----------------------------------------------------------------------------------------------------
	public boolean isFixNumberOfGenerations();
	
	//----------------------------------------------------------------------------------------------------
	void setFixNumberOfGenerations(final boolean fixNumberOfGenerations);
	
	//====================================================================================================
	// nested classes, interfaces
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * The {@link ModelListener} interface is used to notify interested parties about changes in the model.
	 * 
	 * @author Tamás Máhr
	 *
	 */
	public interface ModelListener {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		/**
		 * Fired when a new fitness function has been added to the model.
		 */
		public void fitnessFunctionAdded();
		
		//----------------------------------------------------------------------------------------------------
		/**
		 * Fired when all fitness functions are removed from the model.
		 */
		public void fitnessFunctionsRemoved();
		
		//----------------------------------------------------------------------------------------------------
		/**
		 * Fired when a new parameter or gene has been added to the model.
		 */
		public void parameterAdded();
		
		//----------------------------------------------------------------------------------------------------
		/**
		 * Fired when all parameters and genes are removed from the model.
		 */
		public void parametersRemoved();
	}

}