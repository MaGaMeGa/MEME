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
import java.util.Random;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ai.aitia.meme.utils.FormsUtils;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

public class DefaultMutation implements IGAOperator, Serializable {

	//=========================================================================
	//members
	private static final long serialVersionUID = 7385576920337495076L;
	protected static Random genSelection = new Random( 1 );
	protected static Uniform genValue;
	
	protected double geneMutProb = 0.01;
	
	//settings panel
	protected transient JPanel settingsPanel = null;
	protected transient JTextField geneMutProbField = new JTextField();
	
	static{
		MersenneTwister gEngine = new MersenneTwister( 1 );
		genValue = new Uniform( gEngine );
	}
	
	//=========================================================================
	//public static functions
	/**
	 * Sets the random seed for the pseudo-random generator, that is used to
	 * select genes randomly for mutation.
	 */
	public static void initSelectionGenerator( int seed ){
		genSelection = new Random( seed );
	}

	/**
	 * Sets the random seed for the pseudo-random generator, that is used to
	 * generate new random values for the genes.
	 */
	public static void initValueGenerator( int seed ){
		MersenneTwister gEngine = new MersenneTwister( seed );
		genValue = new Uniform( gEngine );
	}
	
	/**
	 * Mutates the solutions: each gene is mutated at equal probability.
	 * 
	 * @param <T> is the type of the gene
	 * @param solution is the list of a solution's genes.
	 * @param mutationProb is the probability of the mutation of each gene 
	 * (an element of the [0,1] interval).
	 */
	/*public static void mutate( List<IMutableGene> solution, double mutationProb ){
		for( int j = 0; j < solution.size(); ++j ){
			double nextRnd = genSelection.nextDouble();
            if( nextRnd < mutationProb ){
            	solution.get( j ).mutate();
            }
        }
	}*/
	
	//=========================================================================
	//implemented interfaces
	public void operate(List<Chromosome> population,
			List<Chromosome> nextPopulation, boolean maximizeFitness) {
		for( int i = 0; i < nextPopulation.size(); ++i ){
			Chromosome ch = nextPopulation.get( i );
			for( int j = 0; j < ch.getSize(); ++j ){
				double nextRnd = genSelection.nextDouble();
	            if( nextRnd < geneMutProb ){
	            	ch.geneAt( j ).setValue( ch.geneAt( j ).getUniformRandomValue( genValue ) );
	            }
			}
        }
		
	}

	public String getDescription() {
		return "Default mutation";
	}

	public String getName() {
		return "Default mutation";
	}

	public JPanel getSettingspanel() {
		// TODO Auto-generated method stub
		if( settingsPanel == null ){
			geneMutProbField.setColumns( 15 );
			geneMutProbField.setText( String.valueOf( geneMutProb ) );
			settingsPanel = FormsUtils.build( "p p", 
					"01 p", 
					new JLabel( "Gene mutation probability: " ),
					geneMutProbField ).getPanel();
		}
		return settingsPanel;
	}

	public String saveSettings() {
		try{
			geneMutProb = Integer.valueOf( geneMutProbField.getText() );
		}catch( NumberFormatException e ){
			return "'" + geneMutProbField.getText() + "' is not a valid value!";
		}
		return null;
	}

	public String getPackageName() {
		return null;
	}
}
