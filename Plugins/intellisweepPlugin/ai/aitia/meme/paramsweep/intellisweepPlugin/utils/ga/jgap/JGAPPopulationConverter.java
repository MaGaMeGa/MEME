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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jgap.InvalidConfigurationException;

import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.Chromosome;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.Gene;

/**
 * GA population converter for JGap v.3.4.4.
 * 
 * @author Attila Szabo
 *
 */
public class JGAPPopulationConverter {
	/**
	 * Converts a JGAP population to a MEME Chromosome list.
	 * 
	 * @param pop is the JGAP type population to be converted.
	 * @param prototype has the GeneInfo objects for each gene.
	 * @return the MEME type population
	 */
	public static List<Chromosome> convertJGAPPopulation( List<org.jgap.IChromosome> pop, 
														  Chromosome prototype ){
		List<Chromosome> converted = new ArrayList<Chromosome>();
				
		//TODO: implement
		for( int i = 0; i < pop.size(); ++i ){
			org.jgap.IChromosome ch = pop.get( i );
			org.jgap.Gene[] genes = ch.getGenes();
			Chromosome convCh = new Chromosome();
			for( int j = 0; j < genes.length; ++j ){
				Gene conv = convertJGAPGene( genes[j], prototype.geneAt( j ) );
				convCh.addGene( conv );
			}
			converted.add( convCh );
		}
		
		return converted;
	}
	
	/**
	 * Converts a MEME Chromosome list to a JGap population.
	 * 
	 * @param pop is the MEME type population to be converted.
	 * @return the JGAP type population
	 * @throws InvalidConfigurationException propagates the JGAP exception
	 */
	public static org.jgap.Population convertPopulation( List<Chromosome> pop ) throws
													InvalidConfigurationException{
		if( pop == null ) return null;
		
		org.jgap.Configuration config = new org.jgap.Configuration();
		//TODO: init configuration
		Map<String,Double> fitnessValues = null;
		//TODO: set fitness values
		org.jgap.Configuration.reset();
		config.setFitnessFunction( new MEMEFitnessFunction( fitnessValues ) );
		config.setChromosomePool( new org.jgap.impl.ChromosomePool() );
		config.setJGAPFactory( new org.jgap.impl.JGAPFactory( true ) );
		config.setPopulationSize( pop.size() );
		config.setRandomGenerator( new org.jgap.impl.StockRandomGenerator() );
		
		//TODO: set sample chromosome
		config.setFitnessEvaluator( new org.jgap.DefaultFitnessEvaluator() );
		
		org.jgap.Population converted = new org.jgap.Population( config );
		
		if( pop.size() == 0 ) return converted;
		
		//TODO: implement
		for( int i = 0; i < pop.size(); ++i ){
			org.jgap.Gene[] genes = new org.jgap.Gene[pop.get( 0 ).getSize()];
			for( int j = 0; j < pop.get( i ).getSize(); ++j ){
				genes[j] = convertGene( pop.get( i ).geneAt( j ), config );
				
			}
			
			org.jgap.Chromosome ch = new org.jgap.Chromosome( config, genes );
			converted.addChromosome( ch );
		}
		
		return converted;
	}
	
	/**
	 * Converts a MEME Chromosome list to a list of JGAP chromosomes.
	 * 
	 * @param pop is the MEME type population to be converted.
	 * @return the JGap type population
	 * @throws InvalidConfigurationException propagates the JGAP exception
	 */
	public static List<org.jgap.IChromosome> getChromosomeList( List<Chromosome> pop ) throws
													InvalidConfigurationException{
		List<org.jgap.IChromosome> converted = new ArrayList<org.jgap.IChromosome>();
		
		//TODO: implement
		
		return converted;
	}
	
	//=========================================================================
	//private & protected functions
	protected static org.jgap.Gene convertGene( Gene gene, org.jgap.Configuration conf ) 
											throws InvalidConfigurationException{
		String type = gene.getInfo().getType();
		if( "int".equals( type ) || "Integer".equals( type ) ){
			org.jgap.impl.IntegerGene conv = new org.jgap.impl.IntegerGene( conf );
			//TODO: set gene
			conv.setAllele( gene.getValue() );
			return conv;
		}if( "float".equalsIgnoreCase( type ) ){
			org.jgap.impl.DoubleGene conv = new org.jgap.impl.DoubleGene( conf );
			//TODO: set gene
			conv.setAllele( gene.getValue() );
			return conv;
		}if( "String".equals( type ) ){
			org.jgap.impl.StringGene conv = new org.jgap.impl.StringGene( conf );
			//TODO: set gene
			conv.setAllele( gene.getValue() );
			return conv;
		}if( "short".equalsIgnoreCase( type ) ){
			org.jgap.impl.IntegerGene conv = new org.jgap.impl.IntegerGene( conf );
			//TODO: set gene
			conv.setAllele( gene.getValue() );
			return conv;
		}if( "byte".equalsIgnoreCase( type ) ){
			org.jgap.impl.IntegerGene conv = new org.jgap.impl.IntegerGene( conf );
			//TODO: set gene
			conv.setAllele( gene.getValue() );
			return conv;
		}if( "long".equalsIgnoreCase( type ) ){
			//org.jgap.impl.IntegerGene conv = new org.jgap.impl.IntegerGene( conf );
			//there's only IntegerGene and DoubleGene in JGAP. Both can cause data
			//loss. Here, long values are converted into integers.
			//conv.setAllele( ((Long)gene.getValue()).intValue() );
			//return conv;
			throw new IllegalArgumentException( "Supported JGAP version (v.3.4.4.)" +
					" doesn't support long type genes (" + 
					gene.getInfo().getName() + ")!");
		}if( "double".equalsIgnoreCase( type ) ){
			org.jgap.impl.DoubleGene conv = new org.jgap.impl.DoubleGene( conf );
			//TODO: set gene
			conv.setAllele( gene.getValue() );
			return conv;
		}if( "boolean".equalsIgnoreCase( type ) ){
			org.jgap.impl.BooleanGene conv = new org.jgap.impl.BooleanGene( conf );
			//TODO: set gene
			conv.setAllele( gene.getValue() );
			return conv;
		}
		return null;
	}
	
	@SuppressWarnings("cast")
	protected static Gene convertJGAPGene( org.jgap.Gene gene,  Gene prototype ){
		Object value = null;
		String type = prototype.getInfo().getType();
		
		if( "int".equals( type ) || "Integer".equals( type ) ){
			value = gene.getAllele();
		}if( "float".equalsIgnoreCase( type ) ){
			value = (Float)gene.getAllele();
		}if( "String".equals( type ) ){
			value = gene.getAllele();
		}if( "short".equalsIgnoreCase( type ) ){
			value = (Short)gene.getAllele();
		}if( "byte".equalsIgnoreCase( type ) ){
			value = (Byte)gene.getAllele();
		}if( "long".equalsIgnoreCase( type ) ){
			value = gene.getAllele();
		}if( "double".equalsIgnoreCase( type ) ){
			value = gene.getAllele();
		}if( "boolean".equalsIgnoreCase( type ) ){
			value = gene.getAllele();
		}
		
		return new Gene( prototype.getInfo(), value );
	}
}
