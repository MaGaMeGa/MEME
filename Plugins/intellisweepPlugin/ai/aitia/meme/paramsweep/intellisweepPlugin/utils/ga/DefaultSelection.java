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

public class DefaultSelection implements IGAOperator, Serializable {


	//=========================================================================
	//members
	private static final long serialVersionUID = -8195260589803174275L;
	protected static Random genNext = new Random( 1 );
	
	protected transient JPanel settingsPanel = null;
	
	//=========================================================================
	//public static functions
	/**
	 * Sets the random seed for the pseudo-random generator, that is used to
	 * select solutions randomly for tournament selection.
	 */
	public static void initSelectionGenerator( int seed ){
		genNext = new Random( seed );
	}
	
	/**
	 * Runs a tournament selection with replacement for populationSize times.
	 * 
	 * @param <T> is the type of the gene
	 * @param population is the population of solutions from which the best
	 * is selected using tournament selection.
	 * @param fitness contains the solutions' fitness
	 * @param winnerFitness is the vector where the winners' fitness will be stored.
	 * @return
	 */
	/*public static Vector<List<IMutableGene>> tournamentSelection( Vector<List<IMutableGene>> population, 
																 Vector<Double> fitness,
																 Vector<Double> winnerFitness ){
		if( fitness == null ) return null ;
		winnerFitness = new Vector<Double>();
		
		Vector<List<IMutableGene>> newPopulation = new Vector<List<IMutableGene>>();
		for( int i = 0; i < population.size(); ++i ){
			int one = genNext.nextInt( fitness.size() );
			int other = genNext.nextInt( fitness.size() );
			
			if( fitness.get( one ) >= fitness.get( other ) ){
				newPopulation.add( population.get( one ) );
				winnerFitness.add( fitness.get( one ) );
			}else{
				newPopulation.add( population.get( other ) );
				winnerFitness.add( fitness.get( other ) );
			}
		}
		
		return newPopulation;
	}*/

	//=========================================================================
	//implemented interfaces
	public void operate(List<Chromosome> population,
			List<Chromosome> nextPopulation, boolean maximizeFitness) {
		// TODO Auto-generated method stub
		
	}

	public String getDescription() {
		return "Default selection";
	}

	public String getName() {
		return "Default selection";
	}

	public JPanel getSettingspanel() {
		// TODO Auto-generated method stub
		if( settingsPanel == null ){
			settingsPanel = new JPanel();
			settingsPanel.add( new JLabel( "Default selection settings" ) );
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
