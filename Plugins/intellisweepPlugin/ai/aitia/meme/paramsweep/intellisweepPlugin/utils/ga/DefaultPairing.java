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

public class DefaultPairing implements IGAOperator, Serializable {


	//=========================================================================
	//members
	private static final long serialVersionUID = 549255398842428649L;
	protected static Random genShuffle = new Random( 1 );
	
	protected transient JPanel settingsPanel = null;
	
	//=========================================================================
	//public static functions
	/**
	 * Sets the random seed for the pseudo-random generator, that is used to
	 * shuffle solutions.
	 */
	public static void initSelectionGenerator( int seed ){
		genShuffle = new Random( seed );
	}
	
	/**
	 * Shuffles the solutions, saves their fitness.
	 * 
	 * @param solutions the original solutions
	 * @param fitness contains the fitness of solutions
	 * @param modificationProb is the modification probability 
	 * @return the first (populationSize * modProb) solutions of the shuffled
	 * list of the original solutions. If (populationSize * modProb) is odd,
	 * (populationSize * modProb) + 1 is used instead.
	 */
	/*protected Vector<List<IMutableGene>> pair( Vector<List<IMutableGene>>  solutions,
											  Vector<Double> fitness,
											  Double modificationProb ){
		Vector<Integer> permutation = new Vector<Integer>();
		for( int i = 0; i < solutions.size(); ++i ){
			permutation.add( i );
		}
		//randomize solution
		Collections.shuffle( permutation, genShuffle );
		
		Vector<List<IMutableGene>> combinations = new Vector<List<IMutableGene>>();
		//save the fitness
		Vector<Double> newWinnerFitness = new Vector<Double>();
		long numOfSel = Math.round( solutions.size() * modificationProb );
		if( numOfSel % 2 != 0 ) numOfSel++;
	       
		//select a portion (defined by modProb) of the solutions for 
		//modification
        for( int i = 0; i < numOfSel; i += 2 ){
	        List<IMutableGene> solution1 = solutions.get( permutation.get( i ) );
	        List<IMutableGene> solution2 = solutions.get( permutation.get( i + 1 ) );

            combinations.add( solution1 );
            combinations.add( solution2 );
            newWinnerFitness.add( fitness.get( permutation.get( i ) ) ); //save fitness
            newWinnerFitness.add( fitness.get( permutation.get( i + 1 ) ) ); //save fitness
        }
        
        fitness = newWinnerFitness;
        
        return combinations;
	}*/
	

	//=========================================================================
	//implemented interfaces
	public void operate(List<Chromosome> population,
			List<Chromosome> nextPopulation, boolean maximizeFitness) {
		// TODO Auto-generated method stub
		
	}

	public String getDescription() {
		return "Default pairing";
	}

	public String getName() {
		return "Default pairing";
	}

	public JPanel getSettingspanel() {
		// TODO Auto-generated method stub
		if( settingsPanel == null ){
			settingsPanel = new JPanel();
			settingsPanel.add( new JLabel( "Default pairing settings" ) );
		}
		return settingsPanel;
	}

	public String saveSettings() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPackageName() {
		// TODO Auto-generated method stub
		return null;
	}
}
