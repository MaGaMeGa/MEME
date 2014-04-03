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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import ai.aitia.meme.utils.FormsUtils;

public class OnePointCrossover implements IGAOperator, Serializable {
	//=========================================================================
	//members
	private static final long serialVersionUID = -4179712654539696597L;
	protected int genSeed = 1;
	protected Random genNext = new Random( genSeed );
	protected int crossPercent = 50;
	
	//settings panel
	protected transient JPanel settingsPanel;
	protected transient JTextField seedField;
	protected transient JSpinner crossPercentSpinner;
	//protected JLabel comboProbLabel = 
	//					new JLabel( "New solutions: " );

	//=========================================================================
	//implemented interfaces
	public void operate(List<Chromosome> population,
			List<Chromosome> nextPopulation, boolean maximizeFitness) {
		long numOfChildren = 
			Math.round( ( population.size() * ((double)crossPercent/100) ) / 2 ); //two children per crossover
		List<Chromosome> newSolutions = new ArrayList<Chromosome>();
		for( int i = 0; i < numOfChildren; ++i ){
			int p1Idx = genNext.nextInt( nextPopulation.size() );
			int p2Idx = genNext.nextInt( nextPopulation.size() );
			int crossIdx = 1;
			if( population.get( 0 ).getSize() > 1 )
				crossIdx += genNext.nextInt( population.get( 0 ).getSize() - 1 );
			Chromosome first = null;
			Chromosome second = null;
			try {
				first = nextPopulation.get( p1Idx ).cloneChromosome();
				first.setFitness( Double.NaN );
				second = nextPopulation.get( p2Idx ).cloneChromosome();
				second.setFitness( Double.NaN );
			} catch (Exception e) {
				//selected parents will be nulls
			}
			for( int j = 0; j < population.get( 0 ).getSize() &&
								first != null && second != null; ++j ){
				if( j >= crossIdx ){ //swap gene values
					Gene swap = first.geneAt( j );
					first.removeGene( first.geneAt( j ) );
					if( j < population.get( 0 ).getSize() - 1 ){
						first.addGene( j, second.geneAt( j ) );
					}else{
						first.addGene( second.geneAt( j ) );
					}
					second.removeGene( second.geneAt( j ) );
					if( j < population.get( 0 ).getSize() - 1 ){
						second.addGene( j, swap );
					}else{
						second.addGene( swap );
					}
				}
			}
			newSolutions.add( first );
			newSolutions.add( second );
		}
		
		for( int i = 0; i < newSolutions.size(); ++i ){
			nextPopulation.add( newSolutions.get( i ) );
		}
	}

	public String getDescription() {
		return "One-point crossover\n\nThe one-point crossover operator " +
				"selects two random solutions and a single (random) crossover " +
				"point. It copies the solutions and swaps the genes beyond that point the " +
				"two copies - each resulted chromosome is a child.";
	}

	public String getName() {
		return "One-point crossover";
	}

	public JPanel getSettingspanel() {
		if( settingsPanel == null ){
			seedField = new JTextField();
			crossPercentSpinner = new JSpinner();
			crossPercentSpinner.setModel( new SpinnerNumberModel( 50, 0, 100, 1 ) );
			seedField.setColumns( 15 );
			seedField.setText( String.valueOf( genSeed ) );
			settingsPanel = FormsUtils.build( "p p p p p", 
					"0111_ p ||" +
					"234__ p", 
					new JLabel( "Parent selection seed: " ), seedField,
					new JLabel( "Percent of crossovers: " ), crossPercentSpinner,
					new JLabel( "%" ) ).getPanel();
		}
		return settingsPanel;
	}

	public String saveSettings() {
		try{
			genSeed = Integer.valueOf( seedField.getText() );
			genNext = new Random( genSeed );
		}catch( NumberFormatException e ){
			return "'" + seedField.getText() + "' is not a valid seed value!";
		}
		crossPercent = (Integer)crossPercentSpinner.getValue();
		return null;
	}

	public String getPackageName() {
		return null;
	}

}
