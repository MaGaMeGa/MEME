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
package ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.jgap;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import org.jgap.InvalidConfigurationException;

import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.Chromosome;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.IGAOperator;

public class JGAPOperatorWrapper implements IGAOperator, Serializable {
	//=========================================================================
	//members
	private static final long serialVersionUID = -9189804993271871946L;
	protected org.jgap.GeneticOperator operator;
	
	//=========================================================================
	//constructors
	@SuppressWarnings("unchecked")
	public JGAPOperatorWrapper( Class opClass ) throws IllegalArgumentException{
		if( org.jgap.GeneticOperator.class.isAssignableFrom( opClass ) ){
			if( org.jgap.Genotype.getStaticConfiguration() == null ){
				org.jgap.Configuration config = new org.jgap.Configuration();
				//TODO: init configuration
				Map<String,Double> fitnessValues = null;
				//TODO: set fitness values
				try{
					config.setFitnessFunction( new MEMEFitnessFunction( fitnessValues ) );
					config.setChromosomePool( new org.jgap.impl.ChromosomePool() );
					config.setRandomGenerator( new org.jgap.impl.StockRandomGenerator() );
				}catch( InvalidConfigurationException ex ){
					//TODO 
					throw new IllegalArgumentException( "Cannot set JGAP Configuration " +
							"in perator wrapper!" );
				}
				config.setJGAPFactory( new org.jgap.impl.JGAPFactory( true ) );
				
				//TODO: set sample chromosome
				config.setFitnessEvaluator( new org.jgap.DefaultFitnessEvaluator() );
				org.jgap.Genotype.setStaticConfiguration( config );
			}
			try {
				/*operator = (org.jgap.GeneticOperator)
					opClass.getConstructor( org.jgap.Configuration.class ).newInstance( config );*/
				operator = (org.jgap.GeneticOperator)
					opClass.getConstructor().newInstance();
			}catch( Exception e ){
				throw new IllegalArgumentException( "Couldn't instantiate operator " +
							"using a default configuration.\n" + e.getLocalizedMessage() );
			}
		}else{
			throw new IllegalArgumentException( "Operator doesn't implement the" +
					"org.jgap.GeneticOperator interface!" );
		}
	}
	
	//=========================================================================
	//implemented interfaces
	public String getDescription() {
		String name = operator.getClass().getName();
		String info = name;
		if( name.lastIndexOf( "." ) > -1 ){
			name = name.substring( name.lastIndexOf( "." ) + 1 );
		}
		info += "\n\n" + name + " is a JGAP genetic operator. " +
				"See http://jgap.sourceforge.net/# documentation" +
				" for documentation.";
		return info;
	}

	public String getName() {
		String name = operator.getClass().getName();
		if( name.lastIndexOf( "." ) > -1 ){
			name = name.substring( name.lastIndexOf( "." ) + 1 );
		}
		name += " (JGAP)";
		return name;
	}

	public JPanel getSettingspanel() {
		// TODO Auto-generated method stub
		return null;
	}

	public void operate(List<Chromosome> population,
			List<Chromosome> nextPopulation, boolean maximizeFitness) throws GAOperationException{
		org.jgap.Population pop;
		List<org.jgap.IChromosome> nextPop;
		try {
			pop = JGAPPopulationConverter.convertPopulation( population );
			nextPop = JGAPPopulationConverter.getChromosomeList( nextPopulation );
			org.jgap.Genotype.getStaticConfiguration().setPopulationSize( population.size() );
			operator.operate( pop, nextPop );
		} catch (InvalidConfigurationException e) {
			throw new GAOperationException( "Problem during execution of " +
											 getName(), e );
		}catch( ClassCastException e ){
			if( "Comparison not possible: different types!".equals( e.getMessage() ) ){
				throw new IllegalArgumentException( "Selected operator in JGAP " +
						"requires genes of the same type (" + operator.getClass() + ")!" );
			}
			else throw e;
		}
		
		//operator.operate( pop, nextPop );
		nextPopulation = JGAPPopulationConverter.convertJGAPPopulation( nextPop, population.get( 0 ) );
	}

	public String saveSettings() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getPackageName() {
		return "JGAP";
	}

}
