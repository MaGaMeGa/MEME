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

import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.jgap.GAOperationException;
import ai.aitia.meme.utils.FormsUtils;

public class AveragingCrossover implements IGAOperator, Serializable {
	//=========================================================================
	//members
	private static final long serialVersionUID = 6170104895245905329L;
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
	public String getDescription() {
		return "Averaging crossover\n\nThe averaging crossover operator " +
		"selects two random solutions and creates a child that inherits the" +
		"averages of the gene values of the two solutions.";
	}

	public String getName() {
		return "Averaging crossover";
	}

	public String getPackageName() {
		return null;
	}

	public JPanel getSettingspanel() {
		if( settingsPanel == null ){
			seedField = new JTextField();
			crossPercentSpinner = new JSpinner();
			crossPercentSpinner.setModel( new SpinnerNumberModel( 50, 0, 10000, 1 ) );
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

	public void operate(List<Chromosome> population,
			List<Chromosome> nextPopulation, boolean maximizeFitness) throws GAOperationException {
		long numOfChildren = 
			Math.round( ( population.size() * ((double)crossPercent/100) ) );
		List<Chromosome> newSolutions = new ArrayList<Chromosome>();
		for( int i = 0; i < numOfChildren; ++i ){
			int p1Idx = genNext.nextInt( nextPopulation.size() );
			int p2Idx = genNext.nextInt( nextPopulation.size() );
			//int crossIdx = 1 + genNext.nextInt( population.get( 0 ).getSize() - 1 );
			Chromosome firstParent = nextPopulation.get( p1Idx );
			Chromosome secondParent = nextPopulation.get( p2Idx );
			Chromosome child = null;
			try {
				child = firstParent.cloneChromosome();
				child.setFitness( Double.NaN );
			} catch (Exception e) {
				//selected parents will be nulls
			}
			for( int j = 0; j < population.get( 0 ).getSize() &&
								firstParent != null && secondParent != null; ++j ){
				child.geneAt( j ).setValue( 
						average( firstParent.geneAt( j ), secondParent.geneAt( j ) ) );
			}
			newSolutions.add( child );
		}
		
		for( int i = 0; i < newSolutions.size(); ++i ){
			nextPopulation.add( newSolutions.get( i ) );
		}
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

	//=========================================================================
	//private & protected finctions
	protected Object average( Gene g1, Gene g2 ){
		if( !g1.getInfo().javaType.equals( g2.getInfo().javaType ) ) 
			return null;
		
		String javaType = g1.getInfo().getType();
		
		if( "double".equalsIgnoreCase( javaType ) ){
			return new Double( ((Double)g1.getValue() + (Double)g2.getValue()) / 2 );
		}else if( "float".equalsIgnoreCase( javaType ) ){
			return new Float( ((Float)g1.getValue() + (Float)g2.getValue()) / 2 );
		}else if( "int".equalsIgnoreCase( javaType ) ){
			return new Integer( ((Integer)g1.getValue() + (Integer)g2.getValue()) / 2 );
		}else if( "integer".equalsIgnoreCase( javaType ) ){
			return new Integer( ((Integer)g1.getValue() + (Integer)g2.getValue()) / 2 );
		}else if( "short".equalsIgnoreCase( javaType ) ){
			return new Short( (short) (((Short)g1.getValue() + (Short)g2.getValue()) / 2 ) );
		}else if( "long".equalsIgnoreCase( javaType ) ){
			return new Long( ((Long)g1.getValue() + (Long)g2.getValue()) / 2L );
		}else if( "boolean".equalsIgnoreCase( javaType ) ){
			return (Boolean)g1.getValue() || (Boolean)g2.getValue();
		}
		return null;
	}
}
