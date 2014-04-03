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
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import ai.aitia.meme.utils.FormsUtils;

public class FitnessProportionateSelection implements IGAOperator, Serializable {

	//=========================================================================
	//members
	private static final long serialVersionUID = 3840152575091173849L;
	protected int genSeed = 1;
	protected Random genNext = new Random( genSeed );
	protected int selectedPercent = 50;
	
	//settings panel
	protected transient JPanel settingsPanel;
	protected transient JTextField seedField;
	protected transient JSpinner selectedPercentSpinner;

	//=========================================================================
	//implemented interfaces
	public void operate(List<Chromosome> population,
			List<Chromosome> nextPopulation, boolean maximizeFitness) {
		long numOfSelected = 
			Math.round( population.size() * ((double)selectedPercent/100) );
		double allFitness = 0.0;
		double minFitness = Double.POSITIVE_INFINITY;
		double maxFitness = Double.NEGATIVE_INFINITY;
		ArrayList<Double> normalizedFitness = new ArrayList<Double>(population.size());
		ArrayList<Chromosome> ordered = new ArrayList<Chromosome>(population);
		if (maximizeFitness)
			Collections.sort(ordered, Collections.reverseOrder());
		else
			Collections.sort(ordered);

		for( int i = 0; i < population.size(); ++i ){
			if (population.get(i).getFitness() < minFitness) 
				minFitness = population.get(i).getFitness();
			if (population.get(i).getFitness() > maxFitness) 
				maxFitness = population.get(i).getFitness();
		}
		
		for (int i = 0; i < ordered.size(); i++) {
			normalizedFitness.add(ordered.get(i).getFitness() - minFitness);
			allFitness += ordered.get(i).getFitness() - minFitness;
		}
		for (int i = 0; i < normalizedFitness.size(); i++) {
			normalizedFitness.set(i, normalizedFitness.get(i) / allFitness);
		}
		
		for( int i = 0; i < numOfSelected; ++i ){
			double rnd = genNext.nextDouble();
			double actFitSum = 0.0;
			for( int j = 0; j < normalizedFitness.size(); ++j ){
				actFitSum += normalizedFitness.get(j);
				if( rnd <= actFitSum || j == ordered.size() - 1 ){
					nextPopulation.add( ordered.get( j ) );
					break;
				}
			}
			
		}
	}

	public String getDescription() {
		return "Fitness Proportionate Selection\n\nThis operator selects the better" +
			   " solutions at a higher probability, which is proportionate to the" +
			   " solutions' fitness value.";
	}

	public String getName() {
		return "Fitness Proportionate Selection";
	}

	public JPanel getSettingspanel() {
		if( settingsPanel == null ){
			seedField = new JTextField();
			seedField.setColumns( 15 );
			selectedPercentSpinner = new JSpinner();
			selectedPercentSpinner.setModel( new SpinnerNumberModel( 50, 0, 100, 1 ) );
			settingsPanel = FormsUtils.build( "p ~ p ~ p ~ p", 
					"0111 p ||" +
					"234_ p", 
					new JLabel( "Selection seed: " ), seedField,
					new JLabel( "Percent of selected solutions: " ), 
					selectedPercentSpinner, new JLabel( "%" ) ).getPanel();

			seedField.setText( String.valueOf( genSeed ) );
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
		selectedPercent = (Integer)selectedPercentSpinner.getValue();
		return null;
	}

	public String getPackageName() {
		return null;
	}
}

