/*******************************************************************************
 * Copyright (C) 2006-2014 AITIA International, Inc.
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
package ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.operator;

import java.util.List;

import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.IUniversalRateCalculator;
import org.jgap.InvalidConfigurationException;
import org.jgap.RandomGenerator;
import org.jgap.impl.CrossoverOperator;

import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.gene.IdentifiableBooleanGene;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.gene.IdentifiableDoubleGene;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.gene.IdentifiableListGene;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.gene.IdentifiableLongGene;

/**
 * This genetic operator randomly selects pairs of chromosomes from the
 * population, and creates a single offspring with gene values that are the
 * average of the two corresponding parent genes.
 * 
 * @author Tamás Máhr
 * 
 */
public class GeneAveragingCrossoverOperator extends CrossoverOperator {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -7770745561637082009L;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	/**
	 * @throws InvalidConfigurationException
	 */
	public GeneAveragingCrossoverOperator() throws InvalidConfigurationException {
		super();
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * @param a_configuration
	 * @param a_crossoverRatePercentage
	 * @param a_allowFullCrossOver
	 * @param a_xoverNewAge
	 * @throws InvalidConfigurationException
	 */
	public GeneAveragingCrossoverOperator(final Configuration a_configuration, final double a_crossoverRatePercentage, final boolean a_allowFullCrossOver, 
										  final boolean a_xoverNewAge) throws InvalidConfigurationException {
		super(a_configuration, a_crossoverRatePercentage, a_allowFullCrossOver, a_xoverNewAge);
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * @param a_configuration
	 * @param a_crossoverRatePercentage
	 * @param a_allowFullCrossOver
	 * @throws InvalidConfigurationException
	 */
	public GeneAveragingCrossoverOperator(final Configuration a_configuration, final double a_crossoverRatePercentage, final boolean a_allowFullCrossOver)
																																throws InvalidConfigurationException {
		super(a_configuration, a_crossoverRatePercentage, a_allowFullCrossOver);
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * @param a_configuration
	 * @param a_crossoverRatePercentage
	 * @throws InvalidConfigurationException
	 */
	public GeneAveragingCrossoverOperator(final Configuration a_configuration, final double a_crossoverRatePercentage) throws InvalidConfigurationException {
		super(a_configuration, a_crossoverRatePercentage);
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * @param a_configuration
	 * @param a_desiredCrossoverRate
	 * @param a_allowFullCrossOver
	 * @param a_xoverNewAge
	 * @throws InvalidConfigurationException
	 */
	public GeneAveragingCrossoverOperator(final Configuration a_configuration, final int a_desiredCrossoverRate, final boolean a_allowFullCrossOver, 
								  		  final boolean a_xoverNewAge) throws InvalidConfigurationException {
		super(a_configuration, a_desiredCrossoverRate, a_allowFullCrossOver, a_xoverNewAge);
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * @param a_configuration
	 * @param a_desiredCrossoverRate
	 * @param a_allowFullCrossOver
	 * @throws InvalidConfigurationException
	 */
	public GeneAveragingCrossoverOperator(final Configuration a_configuration, final int a_desiredCrossoverRate, final boolean a_allowFullCrossOver)
																																throws InvalidConfigurationException {
		super(a_configuration, a_desiredCrossoverRate, a_allowFullCrossOver);
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * @param a_configuration
	 * @param a_desiredCrossoverRate
	 * @throws InvalidConfigurationException
	 */
	public GeneAveragingCrossoverOperator(final Configuration a_configuration, final int a_desiredCrossoverRate) throws InvalidConfigurationException {
		super(a_configuration, a_desiredCrossoverRate);
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * @param a_configuration
	 * @param a_crossoverRateCalculator
	 * @param a_allowFullCrossOver
	 * @throws InvalidConfigurationException
	 */
	public GeneAveragingCrossoverOperator(final Configuration a_configuration, final IUniversalRateCalculator a_crossoverRateCalculator,
										  final boolean a_allowFullCrossOver) throws InvalidConfigurationException {
		super(a_configuration, a_crossoverRateCalculator, a_allowFullCrossOver);
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * @param a_configuration
	 * @param a_crossoverRateCalculator
	 * @throws InvalidConfigurationException
	 */
	public GeneAveragingCrossoverOperator(final Configuration a_configuration, final IUniversalRateCalculator a_crossoverRateCalculator) 
																																throws InvalidConfigurationException {
		super(a_configuration, a_crossoverRateCalculator);
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * @param a_configuration
	 * @throws InvalidConfigurationException
	 */
	public GeneAveragingCrossoverOperator(Configuration a_configuration) throws InvalidConfigurationException {
		super(a_configuration);
	}

	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void doCrossover(final IChromosome firstMate, final IChromosome secondMate, final List a_candidateChromosomes, final RandomGenerator generator) {
		// reset the gene values in firstMate to be the average of the
		// corresponding gene values in firstMate and secondMate
		final Gene[] genes1 = firstMate.getGenes();
		final Gene[] genes2 = secondMate.getGenes();
		for (int i = 0; i < genes1.length; i++) {
			final Object allele1 = genes1[i].getAllele();
			final Object allele2 = genes2[i].getAllele();

			if (genes1[i] instanceof IdentifiableDoubleGene) {
				final double newValue = (((Double) allele1).doubleValue() + ((Double) allele2).doubleValue()) / 2.;
				genes1[i].setAllele(newValue);
			}

			if (genes1[i] instanceof IdentifiableLongGene) {
				final long newValue = (long) ((((Long) allele1).longValue() + ((Long) allele2).longValue()) / 2. + 0.5);
				genes1[i].setAllele(newValue);
			}

			if (genes1[i] instanceof IdentifiableListGene) {

				if (allele1 instanceof Number && allele2 instanceof Number) {
					if (allele1 instanceof Double || allele1 instanceof Float
							|| allele2 instanceof Double
							|| allele2 instanceof Float) {
						final double newValue = (((Number) allele1).doubleValue() + ((Number) allele2).doubleValue()) / 2.;
						genes1[i].setAllele(newValue);
					}
				} else {
					final IdentifiableListGene lGene1 = (IdentifiableListGene) genes1[i];
					final IdentifiableListGene lGene2 = (IdentifiableListGene) genes2[i];
					
					final int idx1 = lGene1.getValidValues().indexOf(allele1);
					final int idx2 = lGene2.getValidValues().indexOf(allele2);
					
					final int newIdx = (int) ((idx1 + idx2) / 2. + 0.5);
					lGene1.setAllele(lGene1.getValidValues().get(newIdx));
				}
			}
			
			if (genes1[i] instanceof IdentifiableBooleanGene) {
				final boolean boolAllele1 = (Boolean) allele1;
				final boolean boolAllele2 = (Boolean) allele2;
				
				if (boolAllele1 == boolAllele2) { 
					genes1[i].setAllele(boolAllele1 && boolAllele2);
				} else {
					genes1[i].setToRandomValue(generator);
				}
			}
		}

		// Add the modified chromosome to the candidate pool so that
		// they'll be considered for natural selection during the next
		// phase of evolution.
		a_candidateChromosomes.add(firstMate);
	}
}