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

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import ai.aitia.meme.utils.FormsUtils;

public class BestSelection implements IGAOperator, Serializable {


	//=========================================================================
	//members
	//protected int genSeed = 1;
	//protected Random genNext = new Random( genSeed );
	private static final long serialVersionUID = -3209594951328746510L;
	protected int selectedPercent = 50;
	
	//settings panel
	protected transient JPanel settingsPanel;
	//protected JTextField seedField;
	protected transient JSpinner selectedPercentSpinner;

	//=========================================================================
	//implemented interfaces
	public void operate(List<Chromosome> population,
			List<Chromosome> nextPopulation, boolean maximizeFitness) {
		long numOfSelected = 
			Math.round( population.size() * ((double)selectedPercent/100) );
		ArrayList<Chromosome> ordered = new ArrayList<Chromosome>();
		         
		for( int i = 0; i < population.size(); ++i ){
			if( i == 0 ) ordered.add( population.get( 0 ) );
			else{
				Chromosome act = population.get( i );
				for( int j = 0; j < ordered.size(); ++j ){
					if( act.getFitness() > ordered.get( j ).getFitness() ){
						ordered.add( j, act );
						break;
					}
					if( j == ordered.size() - 1 ){
						ordered.add( act );
						break;
					}
				}
			}
		}
		
		if( !maximizeFitness ) Collections.reverse( ordered );
		
		for( int i = 0; i < numOfSelected; ++i ){
			nextPopulation.add( ordered.get( i ) );
		}
	}

	public String getDescription() {
		return "Best Selection\n\nThis operator selects the best" +
			   " solutions.";
	}

	public String getName() {
		return "Best Selection";
	}

	public JPanel getSettingspanel() {
		if( settingsPanel == null ){
			//seedField = new JTextField();
			//seedField.setColumns( 15 );
			selectedPercentSpinner = new JSpinner();
			selectedPercentSpinner.setModel( new SpinnerNumberModel( 50, 0, 100, 1 ) );
			settingsPanel = FormsUtils.build( "p ~ p ~ p", 
					"012 p", 
					new JLabel( "Percent of selected solutions: " ), 
					selectedPercentSpinner, new JLabel( "%" ) ).getPanel();

			//seedField.setText( String.valueOf( genSeed ) );
		}
		return settingsPanel;
	}

	public String saveSettings() {
		//try{
		//	genSeed = Integer.valueOf( seedField.getText() );
		//	genNext = new Random( genSeed );
		//}catch( NumberFormatException e ){
		//	return "'" + seedField.getText() + "' is not a valid seed value!";
		//}
		selectedPercent = (Integer)selectedPercentSpinner.getValue();
		return null;
	}

	public String getPackageName() {
		return null;
	}
}
