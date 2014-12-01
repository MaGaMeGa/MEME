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
package ai.aitia.meme.paramsweep.intellisweepPlugin.jgap;

import java.util.Iterator;
import java.util.List;

import org.jgap.BulkFitnessFunction;
import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.GeneticOperator;
import org.jgap.IChromosome;
import org.jgap.IInitializer;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;
import org.jgap.event.GeneticEvent;
import org.jgap.impl.GABreeder;

/**
 * @author Tamás Máhr
 * 
 */
public class IntelliBreeder extends GABreeder {

	//====================================================================================================
	// members

	private static final long serialVersionUID = 7946876800174982830L;

	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	@Override
	public Population evolve(final Population a_pop, final Configuration a_conf) {
		Population pop = a_pop;
		IChromosome fittest = null;

		// If first generation: Set age to one to allow genetic operations,
		// see CrossoverOperator for an illustration.
		if (a_conf.getGenerationNr() == 0) {
			final int size = pop.size();
			for (int i = 0; i < size; i++) {
				final IChromosome chrom = pop.getChromosome(i);
				chrom.increaseAge();
			}
		} else {
			// Select fittest chromosome in case it should be preserved and we
			// are not in the very first generation.
			if (a_conf.isPreserveFittestIndividual()) {
				fittest = pop.determineFittestChromosome(0, pop.size() - 1);
			}
		}
		
		if (a_conf.getGenerationNr() > 0) {
			// Adjust population size to configured size (if wanted).
			// Theoretically, this should be done at the end of this method.
			// But for optimization issues it is not. If it is the last call to
			// evolve() then the resulting population possibly contains more
			// chromosomes than the wanted number. But this is no bad thing as
			// more alternatives mean better chances having a fit candidate.
			// If it is not the last call to evolve() then the next call will
			// ensure the correct population size by calling
			// keepPopSizeConstant.
			keepPopSizeConstant(pop, a_conf);
		}
		
		// Ensure fitness value of all chromosomes is updated.
		updateChromosomes(pop, a_conf);
		
		// Apply certain NaturalSelectors before GeneticOperators will be
		// executed.
		pop = applyNaturalSelectors(a_conf, pop, true);
		
		// Execute all of the Genetic Operators.
		final int survivorsSize = pop.size();
		final Population children = useGeneticOperators(a_conf, pop);
		// combine the survivors of the previous generation and their children
		pop.getChromosomes().addAll(children.getChromosomes());

		// Reset fitness value of genetically operated chromosomes.
		// Normally, this should not be necessary as the Chromosome class
		// initializes each newly created chromosome with
		// FitnessFunction.NO_FITNESS_VALUE. But who knows which Chromosome
		// implementation is used...
		final int currentPopSize = pop.size();
		for (int i = survivorsSize; i < currentPopSize; i++) {
			final IChromosome chrom = pop.getChromosome(i);
			chrom.setFitnessValueDirectly(FitnessFunction.NO_FITNESS_VALUE);
			// Mark chromosome as new-born.
			chrom.resetAge();
			// Mark chromosome as being operated on.
			chrom.increaseOperatedOn();
		}
		
		// Increase age of all chromosomes which are not modified by genetic
		// operations.
		for (int i = 0; i < currentPopSize; i++) {
			final IChromosome chrom = pop.getChromosome(i);
			chrom.increaseAge();
			// Mark chromosome as not being operated on.
			chrom.resetOperatedOn();
		}
		
		// If a bulk fitness function has been provided, call it.
		final BulkFitnessFunction bulkFunction = a_conf.getBulkFitnessFunction();
		if (bulkFunction != null) {
			bulkFunction.evaluate(pop);
		}
		
		// Apply certain NaturalSelectors after GeneticOperators have been
		// applied.
		pop = applyNaturalSelectors(a_conf, pop, false);
		
		// Fill up population randomly if size dropped below specified
		// percentage of original size.
		// ----------------------------------------------------------------------
		if (a_conf.getMinimumPopSizePercent() > 0) {
			final int sizeWanted = a_conf.getPopulationSize();
			int popSize;
			final int minSize = (int) Math.round(sizeWanted	* (double) a_conf.getMinimumPopSizePercent() / 100);
			popSize = pop.size();
			if (popSize < minSize) {
				IChromosome newChrom;
				final IChromosome sampleChrom = a_conf.getSampleChromosome();
				final Class<? extends IChromosome> sampleChromClass = sampleChrom.getClass();
				final IInitializer chromIniter = a_conf.getJGAPFactory().getInitializerFor(sampleChrom, sampleChromClass);
				while (pop.size() < minSize) {
					try {
						newChrom = (IChromosome) chromIniter.perform(sampleChrom, sampleChromClass, null);
						newChrom.increaseAge();
						pop.addChromosome(newChrom);
					} catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}
			}
		}
		
		reAddFittest(pop, fittest);
		
		// Increase number of generations.
		a_conf.incrementGenerationNr();
		
		// Fire an event to indicate we've performed an evolution.
		a_conf.getEventManager().fireGeneticEvent(new GeneticEvent(GeneticEvent.GENOTYPE_EVOLVED_EVENT, this));

		return pop;
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * Applies all GeneticOperators registered with the Configuration. The
	 * chromosomes created by the operators are collected in a new Population
	 * instance, which is returned.
	 * 
	 * @param a_config
	 *            the configuration to use
	 * @param a_pop
	 *            the population to use as input
	 * @return a new Population instance containing the new Chromosomes
	 * 
	 * @author Klaus Meffert
	 * @author Tamas Mahr
	 * @since 3.2
	 */
	@SuppressWarnings("rawtypes")
	protected Population useGeneticOperators(final Configuration a_config, final Population a_pop) {
		try {
			final Population newGeneration = new Population(a_config, a_pop.size());
			final List geneticOperators = a_config.getGeneticOperators();
			final Iterator operatorIterator = geneticOperators.iterator();
			
			while (operatorIterator.hasNext()) {
				final GeneticOperator operator = (GeneticOperator) operatorIterator.next();
				operator.operate(a_pop, newGeneration.getChromosomes());
			}

			return newGeneration;
		} catch (InvalidConfigurationException e) {
			// should never happen
			throw new IllegalStateException(e);
		}
	}
}