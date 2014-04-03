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

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import ai.aitia.meme.utils.FormsUtils;

public class TournamentSelection implements IGAOperator, Serializable {

	//=========================================================================
	//members
	private static final long serialVersionUID = 7954002229739926309L;
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
		         
		for( int i = 0; i < numOfSelected; ++i ){
			int one = genNext.nextInt( population.size() );
			int other = genNext.nextInt( population.size() );
			
			if( maximizeFitness ){
				if( population.get( one ).fitness >= population.get( other ).fitness ){
					nextPopulation.add( population.get( one ) );
				}else{
					nextPopulation.add( population.get( other ) );
				}
			}else{
				if( population.get( one ).fitness <= population.get( other ).fitness ){
					nextPopulation.add( population.get( one ) );
				}else{
					nextPopulation.add( population.get( other ) );
				}
			}
		}		
	}

	public String getDescription() {
		return "Tournament selection\n\nThis operator selects two agents" +
			   " randomly and adds the one with higher fitness to the next " +
			   "population. This selection is repeated until the next population" +
			   "has enough solutions.";
	}

	public String getName() {
		return "Tournament selection";
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
