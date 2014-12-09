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
package ai.aitia.meme.paramsweep.intellisweepPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

import org.apache.commons.math3.random.MersenneTwister;
import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.FitnessEvaluator;
import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.Population;
import org.jgap.RandomGenerator;
import org.jgap.impl.DefaultConfiguration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ai.aitia.meme.Logger;
import ai.aitia.meme.paramsweep.batch.IParameterSweepResultReader;
import ai.aitia.meme.paramsweep.batch.ReadingException;
import ai.aitia.meme.paramsweep.batch.ResultValueInfo;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;
import ai.aitia.meme.paramsweep.generator.WizardSettingsManager;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.RecordableElement;
import ai.aitia.meme.paramsweep.gui.info.ResultInfo;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.GASearchPanel;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.GASearchPanelModel;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.IntelliBreeder;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.BestChromosomeSelectorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.CrossoverOperatorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.GeneAveragingCrossoverOperatorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.IGAOperatorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.IGASelectorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.MutationOperatorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.TournamentSelectorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.WeightedRouletteSelectorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.gene.IIdentifiableGene;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.gene.IdentifiableDoubleGene;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.gene.IdentifiableListGene;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.gene.IdentifiableLongGene;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.gene.ParameterOrGene;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.operator.StandardPostSelectorFixed;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.GeneInfo;
import ai.aitia.meme.paramsweep.internal.platform.InfoConverter;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.plugin.IIntelliDynamicMethodPlugin;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;

public class JgapGAPlugin implements IIntelliDynamicMethodPlugin, GASearchPanelModel {
	


	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 2516082442494840822L;
	
	private static final String GA_SETTINGS = "ga_settings";
	private static final String POPULATION_SIZE = "population_size";
	private static final String POPULATION_RANDOM_SEED = "population_random_seed";
	private static final String FIX_NUMBER_OF_GENERATIONS = "fix_number_of_generations";
	private static final String NUMBER_OF_GENERATIONS = "number_of_generations";
	private static final String FITNESS_LIMIT_CRITERION = "fitness_limit";
	private static final String OPTIMIZATION_DIRECTION = "optimization_direction";
	private static final String FITNESS_FUNCTION = "fitness_function"; 
	private static final String SELECTORS = "selectors";
	private static final String SELECTOR = "selector";
	private static final String PROPERTY = "property";
	private static final String KEY = "key";
	private static final String GENETIC_OPERATORS = "genetic_operators";
	private static final String GENETIC_OPERATOR = "genetic_operator";
	private static final String CHROMOSOME = "chromosome"; 
	private static final String GENE = "gene";
	private static final String IS_INTEGER = "is_integer"; 
	private static final String MIN_VALUE = "min_value";
	private static final String MAX_VALUE = "max_value";
	private static final String LIST_VALUE = "list_value";
	private static final String PARAMETER = "parameter";

	protected static final String ext = ".txt";
	protected static final String bckExt = ".bak";
	
	private int populationSize = 12;
	private int populationGenerationSeed = 1;
	private int numberOfGenerations = 12;
	private double fitnessLimitCriterion = 0;
	private boolean fixNumberOfGenerations = true;
	private FitnessFunctionDirection optimizationDirection = FitnessFunctionDirection.MINIMIZE;
	private RecordableInfo selectedFunction;
	private final List<IGASelectorConfigurator> selectedSelectionOperators = new ArrayList<IGASelectorConfigurator>();
	private final List<IGAOperatorConfigurator> selectedGeneticOperators = new ArrayList<IGAOperatorConfigurator>();

	private DefaultConfiguration gaConfiguration;

	/**
	 * The available selection operators.
	 */
	private List<IGASelectorConfigurator> selectors;

	/**
	 * The available genetic operators.
	 */
	private List<IGAOperatorConfigurator> geneticOperators;

	/**
	 * The list of fitness functions.
	 */
	private final List<RecordableInfo> fitnessFunctions = new ArrayList<RecordableInfo>();

	private final String populationFileName = "population";
	
	private DefaultTreeModel chromosomeTree;
	private List<GeneInfo> genes = null;
	private int iterationCounter = 1;
	private boolean reachFitnessLimit = false;
	
	private Genotype genotype;
	private List<ParameterInfo> paramList;
	private File workspace;

	private transient List<ModelListener> listeners = new ArrayList<GASearchPanelModel.ModelListener>();
	private transient String readyStatusDetail;
	private transient GASearchPanel content = null;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public JgapGAPlugin() {
		selectors = Arrays.asList(new TournamentSelectorConfigurator(),new WeightedRouletteSelectorConfigurator(),new BestChromosomeSelectorConfigurator());
		geneticOperators = Arrays.asList(new GeneAveragingCrossoverOperatorConfigurator(), new CrossoverOperatorConfigurator(),new MutationOperatorConfigurator());
		
		init();
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getLocalizedName() { return "Genetic Algorithm"; }
	public int getMethodType() { return DYNAMIC_METHOD; }
	public boolean isImplemented() { return true; }
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription() {
		return "The method uses genetic algorithms to find the optimal parameter settings.";
	}
	
	//----------------------------------------------------------------------------------------------------
	private void init() {
		populationSize = 12;
		populationGenerationSeed = 1;
		numberOfGenerations = 12;
		fitnessLimitCriterion = 0;
		fixNumberOfGenerations = true;
		optimizationDirection = FitnessFunctionDirection.MINIMIZE;
		selectedFunction = new RecordableInfo("Please select a function!",Object.class,"Please select a function!");

		selectedSelectionOperators.clear();
		selectedGeneticOperators.clear();

		gaConfiguration = null;
		fitnessFunctions.clear();

		iterationCounter = 1;
		reachFitnessLimit = false;
		
		genotype = null;
		paramList = null;
		
		fitnessFunctions.add(selectedFunction);
		
		selectedSelectionOperators.add(selectors.get(2));

		selectedGeneticOperators.add(geneticOperators.get(0));
		selectedGeneticOperators.add(geneticOperators.get(2));
	}
	
	//----------------------------------------------------------------------------------------------------
	private void removeAllFitnessFunctions() {
		fitnessFunctions.clear();
		fitnessFunctions.add(new RecordableInfo("Please select a function!", Object.class, "Please select a function!"));
		
		if (listeners != null) {
			for (final ModelListener listener : listeners) 
				listener.fitnessFunctionsRemoved();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<IGASelectorConfigurator> getSelectionOperators() { return selectors; }
	public List<RecordableInfo> getFitnessFunctions() { return fitnessFunctions; }
	public List<IGAOperatorConfigurator> getGeneticOperators() { return geneticOperators; }
	public int getPopulationSize() { return populationSize; }
	public int getPopulationRandomSeed() { return populationGenerationSeed;	}
	public int getNumberOfGenerations() { return numberOfGenerations; }
	public double getFitnessLimitCriterion() { return fitnessLimitCriterion; }
	public FitnessFunctionDirection getFitnessFunctionDirection() { return optimizationDirection; }
	public RecordableInfo getSelectedFitnessFunction() { return selectedFunction; }
	public List<IGAOperatorConfigurator> getSelectedGeneticOperators() { return selectedGeneticOperators; }
	public List<IGASelectorConfigurator> getSelectedSelectionOperators() { return selectedSelectionOperators; }
	public boolean isFixNumberOfGenerations() { return fixNumberOfGenerations; }

	//----------------------------------------------------------------------------------------------------
	public void setPopulationSize(final int populationSize) { this.populationSize = populationSize; }
	public void setPopulationRandomSeed(final int seed) { this.populationGenerationSeed = seed; }
	public void setNumberOfGenerations(final int numberOfGenerations) { this.numberOfGenerations = numberOfGenerations; }
	public void setFitnessLimitCriterion(final double fitnessLimit) { this.fitnessLimitCriterion = fitnessLimit; }
	public void setFitnessFunctionDirection(final FitnessFunctionDirection direction) { this.optimizationDirection = direction; }
	public void setSelectedFitnessFunction(final RecordableInfo fitnessFunction) { this.selectedFunction = fitnessFunction; }
	public void setFixNumberOfGenerations(final boolean fixNumberOfGenerations) { this.fixNumberOfGenerations = fixNumberOfGenerations; }

	//----------------------------------------------------------------------------------------------------
	public DefaultTreeModel getChromosomeTree() {
		if (chromosomeTree == null)
			chromosomeTree = new DefaultTreeModel(new DefaultMutableTreeNode());
		return chromosomeTree;
	}
	//----------------------------------------------------------------------------------------------------
	public void setSelectedSelectionOperator(final IGASelectorConfigurator operator) {
		if (operator != null && !selectedSelectionOperators.contains(operator)) 
			selectedSelectionOperators.add(operator);
	}

	//----------------------------------------------------------------------------------------------------
	public boolean unsetSelectedSelectionOperator(final IGASelectorConfigurator operator) {
		return selectedSelectionOperators.remove(operator);
	}

	//----------------------------------------------------------------------------------------------------
	public void setSelectedGeneticOperator(final IGAOperatorConfigurator operator) {
		if (operator != null && !selectedGeneticOperators.contains(operator)) 
			selectedGeneticOperators.add(operator);
	}

	//----------------------------------------------------------------------------------------------------
	public boolean unsetSelectedGeneticOperator(final IGAOperatorConfigurator operator) {
		return selectedGeneticOperators.remove(operator);
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean addModelListener(final ModelListener listener) {
		if (listeners == null) {
			listeners = new ArrayList<>();
		}
		
		return listeners.add(listener);
	}

	//----------------------------------------------------------------------------------------------------
	public boolean removeModelListener(final ModelListener listener) {
		if (listeners == null) {
			listeners = new ArrayList<>();
		}
		
		return listeners.remove(listener);
	}

	//----------------------------------------------------------------------------------------------------
	private void addParameter(final ParameterOrGene parameterOrGene) {
		if (chromosomeTree == null) { 
			chromosomeTree = new DefaultTreeModel(new DefaultMutableTreeNode());
			genes = null;
		}
		
		final DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(parameterOrGene);
		final DefaultMutableTreeNode root = (DefaultMutableTreeNode) chromosomeTree.getRoot();
		chromosomeTree.insertNodeInto(newNode,root,root.getChildCount());
		
		if (listeners != null) {
			for (final ModelListener listener : listeners) 
				listener.parameterAdded();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void removeAllParameters() {
		if (chromosomeTree == null)  
			chromosomeTree = new DefaultTreeModel(new DefaultMutableTreeNode());
		else 
			chromosomeTree.setRoot(new DefaultMutableTreeNode());
		
		genes = null;
		
		if (listeners != null) {
			for (final ModelListener listener : listeners) 
				listener.parametersRemoved();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private String[] checkGAModel() {
		final List<String> errors = new ArrayList<String>();
		
		final RecordableInfo invalid = new RecordableInfo("Please select a function!",Object.class,"Please select a function!");
		if (selectedFunction.equals(invalid))
			errors.add("Please select a valid fitness function!");
		
		if (selectedSelectionOperators.isEmpty())
			errors.add("Please select at least one selector!");
		
		if (selectedGeneticOperators.isEmpty())
			errors.add("Please select at least one genetic operator!");

		if (calculateNumberOfGenes() == 0)
			errors.add("Please specify at least one gene!");
		
		return errors.size() == 0 ? null : errors.toArray(new String[0]);
	}
	
	//----------------------------------------------------------------------------------------------------
	public int getNumberOfIterations() {
		if (fixNumberOfGenerations)
			return numberOfGenerations - 1;
		else
			return Integer.MAX_VALUE;
	}
	
	//----------------------------------------------------------------------------------------------------
	public int getCurrentIteration() {
		return iterationCounter;
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean noMoreIteration() {
		if (fixNumberOfGenerations) 
			return iterationCounter > getNumberOfGenerations();
		else 
			return reachFitnessLimit;
	}
	
	//----------------------------------------------------------------------------------------------------
	public ParameterTree getNextParameterTree(final IParameterSweepResultReader reader) {
		if (iterationCounter == 1) // first time
			initPopFile();
		
		iterationCounter++;
		
		((ResultFileFitnessFunction)gaConfiguration.getFitnessFunction()).setCurrentReader(reader);
		
		printPopulation(iterationCounter - 1);
		
		if (!fixNumberOfGenerations) {
			final IChromosome fittestChromosome = genotype.getFittestChromosome();
			final double fitnessValue = gaConfiguration.getFitnessFunction().getFitnessValue(fittestChromosome);
			reachFitnessLimit = optimizationDirection == FitnessFunctionDirection.MINIMIZE ? fitnessValue <= fitnessLimitCriterion 
																						   : fitnessValue >= fitnessLimitCriterion;
																						   
			if (reachFitnessLimit) 
				return null;
		} 
		
		if (iterationCounter > getNumberOfGenerations()) return null;
		
		genotype.evolve();
		
		final ParameterTree nextTree = new ParameterTree();
		
		// this filters out duplicate chromosomes
		@SuppressWarnings("rawtypes")
		final List chromosomes = genotype.getPopulation().getChromosomes();
		@SuppressWarnings("unchecked")
		final Set<IChromosome> descendants = new HashSet<IChromosome>(chromosomes);
		
		for (int i = 0; i < paramList.size(); ++i) {
			final ParameterInfo paramInfo = paramList.get(i);
			paramInfo.setRuns(1); // Run is always 1

			final List<Object> values = new ArrayList<Object>();

			final int genIdx = whichGene(paramInfo.getName());
			// here we assume that the set iterates through the entries in deterministic order
			for (final IChromosome chromosome : descendants) {
				if (chromosome.getFitnessValueDirectly() == -1.0D) { // the default fitness value 
					if (genIdx >= 0) {
						final String strValue = String.valueOf(chromosome.getGene(genIdx).getAllele());
						values.add(ParameterInfo.getValue(strValue,paramInfo.getType()));
						paramInfo.setDefinitionType(ParameterInfo.LIST_DEF);
					} else {
						values.add(paramInfo.getValue());
						paramInfo.setDefinitionType(ParameterInfo.CONST_DEF);
					}
				}
			}

			paramInfo.setValues(values);

			nextTree.addNode(InfoConverter.parameterInfo2ParameterInfo(paramInfo));
		}

		return nextTree;
	}

	//----------------------------------------------------------------------------------------------------
	public boolean alterParameterTree(final IIntelliContext ctx) {
		// create initial population
		final DefaultMutableTreeNode root = ctx.getParameterTreeRootNode();
		final DefaultMutableTreeNode newRoot = getAlteredParameterTreeRootNode(ctx);
		root.removeAllChildren();
		final int count = newRoot.getChildCount();

		for (int i = 0; i < count; ++i) 
			root.add((DefaultMutableTreeNode) newRoot.getChildAt(0));
		
		return true;
	}

	//----------------------------------------------------------------------------------------------------
	protected DefaultMutableTreeNode getAlteredParameterTreeRootNode(final IIntelliContext ctx) {
		genotype = generateInitialPopulation(ctx);

		final Population descendants = genotype.getPopulation();
		final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Parameter file");

		paramList = ctx.getParameters();
		for (int i = 0; i < paramList.size(); ++i) {
			final ParameterInfo paramInfo = paramList.get(i);
			paramInfo.setRuns(1); // Run is always 1

			final List<Object> values = new ArrayList<Object>();

			final int genIdx = whichGene(paramInfo.getName());
			if (genIdx >= 0) {
				for (int j = 0; j < populationSize; ++j) {
					final String strValue = String.valueOf(descendants.getChromosome(j).getGene(genIdx).getAllele());
					values.add(ParameterInfo.getValue(strValue,paramInfo.getType()));
					paramInfo.setDefinitionType(ParameterInfo.LIST_DEF);
				}
			} else {
				values.add(paramInfo.getValue());
				paramInfo.setDefinitionType(ParameterInfo.CONST_DEF);
			}

			paramInfo.setValues(values);

			// add and save the node
			root.add(new DefaultMutableTreeNode(paramInfo));
		}

		return root;
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * Searches the selected genes list for the specified parameter, and returns
	 * its index.
	 * 
	 * @param name
	 *            the parameter's name that is searched in the selected genes list
	 * @return the index of the gene that represents the given parameter, or -1
	 *         if the parameter is not selected as a gene
	 */
	private int whichGene(final String name) {
		for (int i = 0; i < genes.size(); i++) {
			final GeneInfo geneInfo = genes.get(i);
			if (geneInfo.getName().equals(name)) 
				return i;
		}

		return -1;
	}
	
	//----------------------------------------------------------------------------------------------------
	private List<GeneInfo> getSelectedGenes() {
		if (genes == null) {
			genes = new ArrayList<GeneInfo>();
			
			final DefaultMutableTreeNode root = (DefaultMutableTreeNode) chromosomeTree.getRoot();
			@SuppressWarnings("rawtypes")
			final Enumeration nodes = root.preorderEnumeration();
			nodes.nextElement();
			while (nodes.hasMoreElements()) {
				final DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
				final ParameterOrGene userObj = (ParameterOrGene) node.getUserObject();
				if (userObj.isGene()) {
					convertGeneName(userObj);
					genes.add(userObj.getGeneInfo());
				}
			}
		}
		
		return genes;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void convertGeneName(final ParameterOrGene param) {
		ParameterInfo info = param.getInfo();
		param.getGeneInfo().setName(info.getName());
	}

	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	protected Genotype generateInitialPopulation(final IIntelliContext context) {
		try {
			iterationCounter = 1;
			genes = null;
			
			Configuration.reset();
			gaConfiguration = new DefaultConfiguration();
			Configuration.reset();
			gaConfiguration.removeNaturalSelectors(true);
			gaConfiguration.removeNaturalSelectors(false);
			gaConfiguration.getGeneticOperators().clear();
			gaConfiguration.setAlwaysCaculateFitness(false);
			gaConfiguration.setPreservFittestIndividual(true);
			gaConfiguration.setKeepPopulationSizeConstant(false);
			gaConfiguration.setFitnessEvaluator(new FitnessEvaluator() {
				
				//====================================================================================================
				// methods
				
				//----------------------------------------------------------------------------------------------------
				@Override
				public boolean isFitter(final IChromosome a_chrom1, final IChromosome a_chrom2) {
					return isFitter(a_chrom1.getFitnessValue(),a_chrom2.getFitnessValue());				}
				
				//----------------------------------------------------------------------------------------------------
				@Override
				public boolean isFitter(final double a_fitness_value1, final double a_fitness_value2) {
					return optimizationDirection == FitnessFunctionDirection.MINIMIZE ? a_fitness_value1 < a_fitness_value2 : a_fitness_value1 > a_fitness_value2;
				}
			});
			gaConfiguration.setFitnessFunction(new ResultFileFitnessFunction());
			gaConfiguration.setRandomGenerator(new RandomGenerator() {
				
				//====================================================================================================
				// members
				
				private static final long serialVersionUID = 1L;

				final MersenneTwister randomGenerator = new MersenneTwister(populationGenerationSeed);
				
				//====================================================================================================
				// methods
				
				//----------------------------------------------------------------------------------------------------
				public long nextLong() {
					return randomGenerator.nextLong();
				}

				//----------------------------------------------------------------------------------------------------
				public int nextInt(final int a_ceiling) {
					return randomGenerator.nextInt(a_ceiling);
				}

				//----------------------------------------------------------------------------------------------------
				public int nextInt() {
					return randomGenerator.nextInt();
				}

				//----------------------------------------------------------------------------------------------------
				public float nextFloat() {
					return randomGenerator.nextFloat();
				}

				//----------------------------------------------------------------------------------------------------
				public double nextDouble() {
					return randomGenerator.nextDouble();
				}

				//----------------------------------------------------------------------------------------------------
				public boolean nextBoolean() {
					return randomGenerator.nextBoolean();
				}
			});
			
			final Gene[] initialGenes = new Gene[getSelectedGenes().size()];
			int i = 0;
			for (final GeneInfo geneInfo : getSelectedGenes()) {
				if (GeneInfo.INTERVAL.equals(geneInfo.getValueType())) {
					if (geneInfo.isIntegerVals()) {
						initialGenes[i] = new IdentifiableLongGene(geneInfo.getName(),gaConfiguration,geneInfo.getMinValue().longValue(),
																	  geneInfo.getMaxValue().longValue());
					} else {
						initialGenes[i] = new IdentifiableDoubleGene(geneInfo.getName(),gaConfiguration,geneInfo.getMinValue().doubleValue(),
																	 geneInfo.getMaxValue().doubleValue());
					}
				} else {
					final IdentifiableListGene listGene = new IdentifiableListGene(geneInfo.getName(),gaConfiguration);
					final List<Object> values = geneInfo.getValueRange();
					for (final Object value : values) {
						listGene.addAllele(value);
					}
					listGene.setAllele(values.get(0));
					initialGenes[i] = listGene;
				}

				i++;
			}
			gaConfiguration.setMinimumPopSizePercent(100);
			gaConfiguration.setBreeder(new IntelliBreeder());
			
			final Chromosome sampleChromosome = new Chromosome(gaConfiguration,initialGenes);
			gaConfiguration.setSampleChromosome(sampleChromosome);
			gaConfiguration.setPopulationSize(populationSize);

			for (final IGAOperatorConfigurator operator : selectedGeneticOperators) 
				gaConfiguration.addGeneticOperator(operator.getConfiguredOperator(gaConfiguration));
			
			for (final IGASelectorConfigurator selectorConfig : selectedSelectionOperators)
				gaConfiguration.addNaturalSelector(selectorConfig.getSelector(gaConfiguration),true);

			gaConfiguration.addNaturalSelector(new StandardPostSelectorFixed(gaConfiguration),false);

			final Genotype initialGenotype = Genotype.randomInitialGenotype(gaConfiguration);
			
			return initialGenotype;
		} catch (final InvalidConfigurationException e) {
			Logger.logException(e);
			throw new RuntimeException(e);
		}
	}

	//----------------------------------------------------------------------------------------------------
	/**
	 * Prints the header of the population file.
	 */
	protected void initPopFile() {
		// create backup file when file already exists
		final File file = new File(workspace,populationFileName + ext);
		if (file.exists()) {
			for (int i = 1; i <= 1000; ++i) {
				final File newFile = new File(workspace,populationFileName + bckExt + i + ext);
				if (!newFile.exists() || i == 1000) {
					file.renameTo(newFile);
					break;
				}
			}
		}
		
		try {
			final PrintWriter popWriter = new PrintWriter(new FileWriter(file,true));

			popWriter.print("#pop");
			for (int i = 0; i < genes.size(); ++i) 
				popWriter.print(";" + genes.get(i).getName());
			
			popWriter.print(";fitness");
			popWriter.println();
			popWriter.close();
		} catch (final IOException e) {
			Logger.logExceptionCallStack("Cannot print population to file:\n",e);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	protected void printPopulation(final int iteration) {
		try {
			final PrintWriter popWriter = new PrintWriter(new FileWriter(new File(workspace,populationFileName + ext),true));
			
			final Population population = genotype.getPopulation();
			for (int i = 0;i < population.size();++i) {
				popWriter.print(iteration);
				for (int j = 0;j < population.getChromosome(i).size(); ++j) 
					popWriter.print(";" + population.getChromosome(i).getGene(j).getAllele().toString());
				
				popWriter.print(";" + population.getChromosome(i).getFitnessValue());
				popWriter.println();
			}
			popWriter.close();
		} catch (final IOException e) {
			Logger.logExceptionCallStack( "Cannot print population to file.",e);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private int calculateNumberOfGenes() {
		int count = 0;
		
		final DefaultMutableTreeNode root = (DefaultMutableTreeNode) chromosomeTree.getRoot();
		@SuppressWarnings("rawtypes")
		final Enumeration nodes = root.breadthFirstEnumeration();
		nodes.nextElement();
		
		while (nodes.hasMoreElements()) {
			final DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
			final ParameterOrGene userObj = (ParameterOrGene) node.getUserObject();
			if (userObj.isGene())
				count++;
		}
		
		return count;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setRecordableVariables(final DefaultMutableTreeNode root) {
		final List<RecordableInfo> newList = new ArrayList<RecordableInfo>();
		
		if (root.getChildCount() > 0) {
			final DefaultMutableTreeNode recorder = (DefaultMutableTreeNode) root.getChildAt( 0 );
			final ResultInfo resultInfo = (ResultInfo) recorder.getFirstLeaf().getUserObject();
			workspace = new File(resultInfo.getFile()).getParentFile();
			//first two children contains recorder meta data
			for (int j = 2; j < recorder.getChildCount(); ++j) {
				final RecordableElement re = (RecordableElement) ((DefaultMutableTreeNode)recorder.getChildAt(j)).getUserObject();
				final RecordableInfo recInfo = new RecordableInfo(re.getAlias() != null ? re.getAlias() : re.getInfo().getName(),
									   							  re.getInfo().getJavaType(), re.getInfo().getName());
				if (!newList.contains(recInfo)) {
					newList.add(recInfo);
				}
			}
		}
		
		final RecordableInfo oldSelectedFunction = selectedFunction;
		removeAllFitnessFunctions();
		fitnessFunctions.addAll(newList);
		
		if (listeners != null) {
			for (final ModelListener listener : listeners) {
				listener.fitnessFunctionAdded(); 
			}
		}
		
		if (oldSelectedFunction != null) {
			RecordableInfo selectedFunctionInTheList = null;
			for (final RecordableInfo ri : fitnessFunctions)
			{
				if (ri.equals(oldSelectedFunction)) {
					// accessible name equality
					selectedFunctionInTheList = ri;
				}
			}
			
			if (selectedFunctionInTheList != null) {
				selectedFunction = selectedFunctionInTheList;
				if (listeners != null) {
					for (final ModelListener listener : listeners) {
						listener.fitnessFunctionSelected(selectedFunction);
					}
				}
			}
					
		}
		
	}

	//----------------------------------------------------------------------------------------------------
	public String settingsOK(final DefaultMutableTreeNode recorders) {
		return getReadyStatusDetail();
	}

	//----------------------------------------------------------------------------------------------------
	public void setParameterTreeRoot(final DefaultMutableTreeNode root) {
		// we don't use this in this implementation
	}


	//----------------------------------------------------------------------------------------------------
	public boolean getReadyStatus() {
		String[] errors = checkGAModel();
		
		if (errors == null) {
			return true;
		} else {
			readyStatusDetail = errors[0];
			return  false;
		}
	}

	//----------------------------------------------------------------------------------------------------
	public String getReadyStatusDetail() { 
		return readyStatusDetail;
	}

	//----------------------------------------------------------------------------------------------------
	public JPanel getSettingsPanel(final IIntelliContext ctx) {
		if (content == null) {
			content = new GASearchPanel(this);
			
			removeAllParameters();
			for (final ParameterInfo parameterInfo : ctx.getParameters()) {
				addParameter(new ParameterOrGene(parameterInfo));
			}
			
			if (listeners != null) {
				for (final ModelListener listener : listeners) {
					listener.fitnessFunctionAdded(); // added before, now just inform the gui
				}
			}
		}
		
		
		return content;
	}

	//----------------------------------------------------------------------------------------------------
	public void save(final Node node) {
		final Document document = node.getOwnerDocument();
		final Element pluginElement = (Element) node;
		pluginElement.setAttribute(WizardSettingsManager.CLASS, this.getClass().getName());
		
		final Element gaSettingsElement = document.createElement(GA_SETTINGS);
		node.appendChild(gaSettingsElement);
		
		saveGeneralParameters(gaSettingsElement);
		saveSelectors(gaSettingsElement);
		saveOperators(gaSettingsElement);
		saveChromosome(gaSettingsElement);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void saveGeneralParameters(final Element gaSettingsElement) {
		final Document document = gaSettingsElement.getOwnerDocument();
		
		Element element = document.createElement(POPULATION_SIZE);
		element.appendChild(document.createTextNode(String.valueOf(populationSize)));
		gaSettingsElement.appendChild(element);
		
		element = document.createElement(POPULATION_RANDOM_SEED);
		element.appendChild(document.createTextNode(String.valueOf(populationGenerationSeed)));
		gaSettingsElement.appendChild(element);
		
		element = document.createElement(FIX_NUMBER_OF_GENERATIONS);
		element.appendChild(document.createTextNode(String.valueOf(fixNumberOfGenerations)));
		gaSettingsElement.appendChild(element);
		
		if (fixNumberOfGenerations) {
			element = document.createElement(NUMBER_OF_GENERATIONS);
			element.appendChild(document.createTextNode(String.valueOf(numberOfGenerations)));
			gaSettingsElement.appendChild(element);
		} else {
			element = document.createElement(FITNESS_LIMIT_CRITERION);
			element.appendChild(document.createTextNode(String.valueOf(fitnessLimitCriterion)));
			gaSettingsElement.appendChild(element);
		}
		
		element = document.createElement(OPTIMIZATION_DIRECTION);
		element.appendChild(document.createTextNode(optimizationDirection.name()));
		gaSettingsElement.appendChild(element);
		
		element = document.createElement(FITNESS_FUNCTION);
		element.appendChild(document.createTextNode(selectedFunction.getAccessibleName()));
		gaSettingsElement.appendChild(element);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void saveSelectors(final Element gaSettingsElement) {
		final Document document = gaSettingsElement.getOwnerDocument();
		
		final Element selectorsElement = document.createElement(SELECTORS);
		gaSettingsElement.appendChild(selectorsElement);
		
		for (final IGASelectorConfigurator selectorOperator : selectedSelectionOperators) {
			final Element selectorElement = document.createElement(SELECTOR);
			selectorsElement.appendChild(selectorElement);
			
			selectorElement.setAttribute(WizardSettingsManager.TYPE, selectorOperator.getName());

			for (final Entry<String,String> entry : selectorOperator.getConfiguration().entrySet()) {
				final Element propertyElement = document.createElement(PROPERTY);
				propertyElement.setAttribute(KEY, entry.getKey());
				propertyElement.appendChild(document.createTextNode(entry.getValue()));
				selectorElement.appendChild(propertyElement);
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void saveOperators(final Element gaSettingsElement) {
		final Document document = gaSettingsElement.getOwnerDocument();
		
		final Element geneticOperatorsElement = document.createElement(GENETIC_OPERATORS);
		gaSettingsElement.appendChild(geneticOperatorsElement);
		
		for (final IGAOperatorConfigurator geneticOperator : selectedGeneticOperators) {
			final Element operatorElement = document.createElement(GENETIC_OPERATOR);
			geneticOperatorsElement.appendChild(operatorElement);
			
			operatorElement.setAttribute(WizardSettingsManager.TYPE, geneticOperator.getName());

			for (final Entry<String,String> entry : geneticOperator.getConfiguration().entrySet()) {
				final Element propertyElement = document.createElement(PROPERTY);
				propertyElement.setAttribute(KEY, entry.getKey());
				propertyElement.appendChild(document.createTextNode(entry.getValue()));
				operatorElement.appendChild(propertyElement);
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void saveChromosome(final Element gaSettingsElement) {
		final Document document = gaSettingsElement.getOwnerDocument();
		
		final Element chromosomeElement = document.createElement(CHROMOSOME);
		gaSettingsElement.appendChild(chromosomeElement);
		
		@SuppressWarnings("rawtypes")
		final Enumeration nodes = ((DefaultMutableTreeNode)chromosomeTree.getRoot()).children();
		while (nodes.hasMoreElements()) {
			final DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
			saveParameterOrGene(node,chromosomeElement);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void saveParameterOrGene(final DefaultMutableTreeNode node, final Element chromosomeElement) {
		final Document document = chromosomeElement.getOwnerDocument();
		
		final ParameterOrGene userObj = (ParameterOrGene) node.getUserObject();
		final ParameterInfo info = userObj.getInfo();
		
		if (userObj.isGene()) {
			final GeneInfo geneInfo = userObj.getGeneInfo();
			
			final Element geneElement = document.createElement(GENE);
			chromosomeElement.appendChild(geneElement);
			
			geneElement.setAttribute(WizardSettingsManager.NAME, info.getName());
			geneElement.setAttribute(WizardSettingsManager.TYPE, geneInfo.getValueType());
			
			if (GeneInfo.INTERVAL.equals(geneInfo.getValueType())) {
				geneElement.setAttribute(IS_INTEGER, String.valueOf(geneInfo.isIntegerVals()));
				geneElement.setAttribute(MIN_VALUE, String.valueOf(geneInfo.getMinValue()));
				geneElement.setAttribute(MAX_VALUE, String.valueOf(geneInfo.getMaxValue()));
			} else {
				for (final Object value : geneInfo.getValueRange()) {
					final Element element = document.createElement(LIST_VALUE);
					geneElement.appendChild(element);
					element.appendChild(document.createTextNode(String.valueOf(value)));
				}
			}
		} else {
			final Element paramElement = document.createElement(PARAMETER);
			chromosomeElement.appendChild(paramElement);
			
			paramElement.setAttribute(WizardSettingsManager.NAME, info.getName());
			paramElement.appendChild(document.createTextNode(String.valueOf(info.getValue())));
		}
	}

	//----------------------------------------------------------------------------------------------------
	public void load(final IIntelliContext context, final Element element) throws WizardLoadingException {
		selectors = Arrays.asList(new TournamentSelectorConfigurator(),new WeightedRouletteSelectorConfigurator(),new BestChromosomeSelectorConfigurator());
		geneticOperators = Arrays.asList(new GeneAveragingCrossoverOperatorConfigurator(), new CrossoverOperatorConfigurator(),new MutationOperatorConfigurator());
		
		init();
		selectedSelectionOperators.clear();
		selectedGeneticOperators.clear();
		readyStatusDetail = null;
		content = (GASearchPanel) getSettingsPanel(context);
		
		final NodeList nl = element.getElementsByTagName(GA_SETTINGS);
		if (nl != null && nl.getLength() > 0) {
			final Element gaSettingsElement = (Element) nl.item(0);
			loadGeneralParameters(gaSettingsElement);
			loadSelectors(gaSettingsElement);
			loadOperators(gaSettingsElement);
			loadChromosome(gaSettingsElement, context.getParameters());
			content.reset(this);
		} else {
			throw new WizardLoadingException(true, "missing node: " + GA_SETTINGS);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void loadGeneralParameters(final Element gaSettingsElement) throws WizardLoadingException {
		NodeList nl = gaSettingsElement.getElementsByTagName(POPULATION_SIZE);
		if (nl != null && nl.getLength() > 0) {
			final Element element = (Element) nl.item(0);
			final NodeList content = element.getChildNodes();
			if (content == null || content.getLength() == 0) {
				throw new WizardLoadingException(true, "missing content at node: " + POPULATION_SIZE);
			}
			
			final String populationSizeStr = ((Text)content.item(0)).getNodeValue().trim();
			try {
				populationSize = Integer.parseInt(populationSizeStr);
			} catch (final NumberFormatException e) {
				throw new WizardLoadingException(true,  "invalid content (" + populationSizeStr + ") at node: " + POPULATION_SIZE + " (expected: integer number)");
			}
		} else {
			throw new WizardLoadingException(true, "missing node: " + POPULATION_SIZE);
		}

		nl = gaSettingsElement.getElementsByTagName(POPULATION_RANDOM_SEED);
		if (nl != null && nl.getLength() > 0) {
			final Element element = (Element) nl.item(0);
			final NodeList content = element.getChildNodes();
			if (content == null || content.getLength() == 0) {
				throw new WizardLoadingException(true, "missing content at node: " + POPULATION_RANDOM_SEED);
			}
			
			final String populationGenerationSeedStr = ((Text)content.item(0)).getNodeValue().trim();
			try {
				populationGenerationSeed = Integer.parseInt(populationGenerationSeedStr);
			} catch (final NumberFormatException e) {
				throw new WizardLoadingException(true,  "invalid content (" + populationGenerationSeedStr + ") at node: " + POPULATION_RANDOM_SEED +
												 " (expected: integer number)");
			}
		} else {
			throw new WizardLoadingException(true, "missing node: " + POPULATION_RANDOM_SEED);
		}
		
		nl = gaSettingsElement.getElementsByTagName(FIX_NUMBER_OF_GENERATIONS);
		if (nl != null && nl.getLength() > 0) {
			final Element element = (Element) nl.item(0);
			final NodeList content = element.getChildNodes();
			if (content == null || content.getLength() == 0) {
				throw new WizardLoadingException(true, "missing content at node: " + FIX_NUMBER_OF_GENERATIONS);
			}
			
			final String fixNumberOfGenerationsStr = ((Text)content.item(0)).getNodeValue().trim();
			fixNumberOfGenerations = Boolean.parseBoolean(fixNumberOfGenerationsStr);
		} else {
			throw new WizardLoadingException(true, "missing node: " + FIX_NUMBER_OF_GENERATIONS);
		}
		
		if (fixNumberOfGenerations) {
			nl = gaSettingsElement.getElementsByTagName(NUMBER_OF_GENERATIONS);
			if (nl != null && nl.getLength() > 0) {
				final Element element = (Element) nl.item(0);
				final NodeList content = element.getChildNodes();
				if (content == null || content.getLength() == 0) {
					throw new WizardLoadingException(true, "missing content at node: " + NUMBER_OF_GENERATIONS);
				}
				
				final String numberOfGenerationsStr = ((Text)content.item(0)).getNodeValue().trim();
				try {
					numberOfGenerations = Integer.parseInt(numberOfGenerationsStr);
				} catch (final NumberFormatException e) {
					throw new WizardLoadingException(true,  "invalid content (" + numberOfGenerationsStr + ") at node: " + NUMBER_OF_GENERATIONS +
													 " (expected: integer number)");
				}
			} else {
				throw new WizardLoadingException(true, "missing node: " + NUMBER_OF_GENERATIONS);
			}
		} else {
			nl = gaSettingsElement.getElementsByTagName(FITNESS_LIMIT_CRITERION);
			if (nl != null && nl.getLength() > 0) {
				final Element element = (Element) nl.item(0);
				final NodeList content = element.getChildNodes();
				if (content == null || content.getLength() == 0) {
					throw new WizardLoadingException(true, "missing content at node: " + FITNESS_LIMIT_CRITERION);
				}
				
				final String fitnessLimitCriterionStr = ((Text)content.item(0)).getNodeValue().trim();
				try {
					fitnessLimitCriterion = Double.parseDouble(fitnessLimitCriterionStr);
				} catch (final NumberFormatException e) {
					throw new WizardLoadingException(true,  "invalid content (" + fitnessLimitCriterionStr + ") at node: " + FITNESS_LIMIT_CRITERION +
													 " (expected: real number)");
				}
			} else {
				throw new WizardLoadingException(true, "missing node: " + FITNESS_LIMIT_CRITERION);
			}
		}
		
		nl = gaSettingsElement.getElementsByTagName(OPTIMIZATION_DIRECTION);
		if (nl != null && nl.getLength() > 0) {
			final Element element = (Element) nl.item(0);
			final NodeList content = element.getChildNodes();
			if (content == null || content.getLength() == 0) {
				throw new WizardLoadingException(true, "missing content at node: " + OPTIMIZATION_DIRECTION);
			}
			
			final String optimizationDirectionStr = ((Text)content.item(0)).getNodeValue().trim();
			try {
				optimizationDirection = FitnessFunctionDirection.valueOf(optimizationDirectionStr);
			} catch (final IllegalArgumentException e) {
				throw new WizardLoadingException(true,  "invalid content (" + optimizationDirectionStr + ") at node: " + OPTIMIZATION_DIRECTION +
												 " (expected: MINIMIZE or MAXIMIZE)");
			}
		} else {
			throw new WizardLoadingException(true, "missing node: " + OPTIMIZATION_DIRECTION);
		}
		
		nl = gaSettingsElement.getElementsByTagName(FITNESS_FUNCTION);
		if (nl != null && nl.getLength() > 0) {
			final Element element = (Element) nl.item(0);
			final NodeList content = element.getChildNodes();
			if (content == null || content.getLength() == 0) {
				throw new WizardLoadingException(true, "missing content at node: " + FITNESS_FUNCTION);
			}
			
			final String fitnessFunctionStr = ((Text)content.item(0)).getNodeValue().trim();
			selectedFunction = new RecordableInfo(fitnessFunctionStr, Void.TYPE, fitnessFunctionStr); // dummy object to store accessible name
		} else {
			throw new WizardLoadingException(true, "missing node: " + FITNESS_FUNCTION);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void loadSelectors(final Element gaSettingsElement) throws WizardLoadingException {
		NodeList nl = gaSettingsElement.getElementsByTagName(SELECTORS);
		if (nl != null && nl.getLength() > 0) {
			final Element selectorsElement = (Element) nl.item(0);
			
			nl = selectorsElement.getElementsByTagName(SELECTOR);
			if (nl != null && nl.getLength() > 0) {
				for (int i = 0; i < nl.getLength(); ++i) {
					final Element selectorElement = (Element) nl.item(i);
					final String type = selectorElement.getAttribute(WizardSettingsManager.TYPE);
					if (type == null || type.trim().isEmpty()) {
						throw new WizardLoadingException(true, "missing '" + WizardSettingsManager.TYPE + "' attribute at node: " + SELECTOR);
					}
					final IGASelectorConfigurator configurator = findSelectorConfigurator(type.trim());
					
					final Map<String,String> config = readProperties(selectorElement);
					configurator.setConfiguration(config);
					selectedSelectionOperators.add(configurator);
				}
			}
		} else {
			throw new WizardLoadingException(true, "missing node: " + SELECTORS);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void loadOperators(final Element gaSettingsElement) throws WizardLoadingException {
		NodeList nl = gaSettingsElement.getElementsByTagName(GENETIC_OPERATORS);
		if (nl != null && nl.getLength() > 0) {
			 final Element geneticOperatorsElement = (Element) nl.item(0);
			 
			 nl = geneticOperatorsElement.getElementsByTagName(GENETIC_OPERATOR);
			 if (nl != null && nl.getLength() >0) {
				 for (int i = 0; i < nl.getLength(); ++i) {
					 final Element goElement = (Element) nl.item(i);
					 final String type = goElement.getAttribute(WizardSettingsManager.TYPE);
					 if (type == null || type.trim().isEmpty()) {
						 throw new WizardLoadingException(true, "missing '" + WizardSettingsManager.TYPE + "' attribute at node: " + GENETIC_OPERATOR);
					 }
					 final IGAOperatorConfigurator configurator = findOperatorConfigurator(type.trim());
					 
					 final Map<String,String> config = readProperties(goElement);
					 configurator.setConfiguration(config);
					 selectedGeneticOperators.add(configurator);
				 }
			 }
		} else {
			throw new WizardLoadingException(true, "missing node: " + GENETIC_OPERATORS);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void loadChromosome(final Element gaSettingsElement, final List<ParameterInfo> params) throws WizardLoadingException {
		final int childCount = getChromosomeTree().getChildCount(getChromosomeTree().getRoot());
		if (childCount > 0) {
			final DefaultMutableTreeNode root = (DefaultMutableTreeNode) getChromosomeTree().getRoot();
			for (int i = childCount - 1; i >= 0; --i) {
				final MutableTreeNode node = (MutableTreeNode) root.getChildAt(i);
				getChromosomeTree().removeNodeFromParent(node); 
			}
			genes = null;
		}
		
		final DefaultMutableTreeNode root = (DefaultMutableTreeNode) getChromosomeTree().getRoot();
		
		NodeList nl = gaSettingsElement.getElementsByTagName(CHROMOSOME);
		if (nl != null && nl.getLength() > 0) {
			final Element chromosomeElement = (Element) nl.item(0);
			
			nl = chromosomeElement.getElementsByTagName(GENE);
			loadGene(nl, params, root);
			
			nl = chromosomeElement.getElementsByTagName(PARAMETER);
			loadParameter(nl, params, root);
		} else {
			throw new WizardLoadingException(true, "missing node: " + CHROMOSOME);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void loadGene(final NodeList geneList, final List<ParameterInfo> params, final DefaultMutableTreeNode root) throws WizardLoadingException {
		if (geneList != null) {
			for (int i = 0; i < geneList.getLength(); ++i) {
				final Element geneElement = (Element) geneList.item(i);
				final String geneName = geneElement.getAttribute(WizardSettingsManager.NAME);
				
				if (geneName == null || geneName.trim().isEmpty()) {
					throw new WizardLoadingException(true, "missing attribute '" + WizardSettingsManager.NAME + "' at node: " + GENE);
				}
				
				final ParameterInfo info = findParameterInfo(params,geneName.trim());
				if (info != null) {
					final ParameterOrGene userObj = initializeUserObjectFromGene(geneElement,info);
					final DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(userObj);
					getChromosomeTree().insertNodeInto(newNode,root,root.getChildCount());
				} else {
					throw new WizardLoadingException(false, "Unrecognized gene: " + geneName + " (ignored)");
				}
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void loadParameter(final NodeList parameterList, final List<ParameterInfo> params, final DefaultMutableTreeNode root) throws WizardLoadingException {
		if (parameterList != null) {
			for (int i = 0; i < parameterList.getLength(); ++i) {
				final Element parameterElement = (Element) parameterList.item(i);
				final String paramName = parameterElement.getAttribute(WizardSettingsManager.NAME);
				
				if (paramName == null || paramName.trim().isEmpty()) {
					throw new WizardLoadingException(true, "missing attribute '" + WizardSettingsManager.NAME + "' at node: " + PARAMETER);
				}
				
				final ParameterInfo info = findParameterInfo(params,paramName.trim());
				if (info != null) {
					final NodeList content = parameterElement.getChildNodes();
					if (content == null || content.getLength() == 0) {
						throw new WizardLoadingException(true, "missing content at node: " + PARAMETER);
					}
					final String parameterValue = ((Text)content.item(0)).getNodeValue().trim();
					Object value;
					if (info.getType().equals("File") && parameterValue.isEmpty()) {
						value = new File("");
					} else {
						value = ParameterInfo.getValue(parameterValue,info.getJavaType());
					}
					info.setValue(value);
					
					final ParameterOrGene userObj = new ParameterOrGene(info);
					final DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(userObj);
					getChromosomeTree().insertNodeInto(newNode,root,root.getChildCount());
				} else {
					throw new WizardLoadingException(false, "Unrecognized gene: " + paramName + " (ignored)");
				}
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private ParameterOrGene initializeUserObjectFromGene(final Element geneElement, final ParameterInfo info) throws WizardLoadingException {
		final String geneType = geneElement.getAttribute(WizardSettingsManager.TYPE);
		if (geneType == null || geneType.trim().isEmpty()) {
			throw new WizardLoadingException(true, "missing attribute '" + WizardSettingsManager.TYPE + "' at node: " + GENE);
		}
		
		if (GeneInfo.INTERVAL.equals(geneType.trim())) {
			final String isIntegerStr = geneElement.getAttribute(IS_INTEGER);
			if (isIntegerStr == null || isIntegerStr.trim().isEmpty()) {
				throw new WizardLoadingException(true, "missing attribute '" + IS_INTEGER + "' at node: " + GENE);
			}
			final boolean isInteger = Boolean.parseBoolean(isIntegerStr.trim());
			
			final String minStr = geneElement.getAttribute(MIN_VALUE);
			if (minStr == null || minStr.trim().isEmpty()) {
				throw new WizardLoadingException(true, "missing attribute '" + MIN_VALUE + "' at node: " + GENE);
			}
			
			final String maxStr = geneElement.getAttribute(MAX_VALUE);
			if (maxStr == null || maxStr.trim().isEmpty()) {
				throw new WizardLoadingException(true, "missing attribute '" + MAX_VALUE + "' at node: " + GENE);
			}
			
			if (isInteger) {
				long min = Long.MIN_VALUE;
				try {
					min = Long.parseLong(minStr);
				} catch (final NumberFormatException e) {
					throw new WizardLoadingException(true, "invalid attribute value (" + MIN_VALUE + "=" + minStr + ") at node: " + GENE + 
							" (expected: integer number)");
				}
				
				long max = Long.MAX_VALUE;
				try {
					max = Long.parseLong(maxStr);
				} catch (final NumberFormatException e) {
					throw new WizardLoadingException(true, "invalid attribute value (" + MAX_VALUE + "=" + maxStr + ") at node: " + GENE +
							" (expected: integer number)");
				}
				
				return new ParameterOrGene(info,min,max);
			} else {
				double min = - Double.MAX_VALUE;
				try {
					min = Double.parseDouble(minStr);
				} catch (final NumberFormatException e) {
					throw new WizardLoadingException(true, "invalid attribute value (" + MIN_VALUE + "=" + minStr + ") at node: " + GENE + 
							" (expected: real number)");
				}
				
				double max = Double.MAX_VALUE;
				try {
					max = Double.parseDouble(maxStr);
				} catch (final NumberFormatException e) {
					throw new WizardLoadingException(true, "invalid attribute value (" + MAX_VALUE + "=" + maxStr + ") at node: " + GENE + 
							" (expected: real number)");
				}
				
				return new ParameterOrGene(info,min,max);
			}
		} else if (GeneInfo.LIST.equals(geneType.trim())) {
			final NodeList nl = geneElement.getElementsByTagName(LIST_VALUE);
			if (nl != null && nl.getLength() > 0) {
				final List<Object> valueList = new ArrayList<>(nl.getLength());
				
				for (int i = 0; i < nl.getLength(); ++i) {
					final Element element = (Element) nl.item(i);
					final NodeList content = element.getChildNodes();
					if (content == null || content.getLength() == 0) {
						throw new WizardLoadingException(true, "missing content at node: " + LIST_VALUE);
					}
					final String strValue = ((Text)content.item(0)).getNodeValue().trim();
					valueList.add(parseListElement(info.getJavaType(),strValue));
				}
				
				return new ParameterOrGene(info, valueList);
			} else {
				throw new WizardLoadingException(true, "missing node: " + LIST_VALUE);
			}
		} else {
			throw new WizardLoadingException(true, "invalid attribute value (" + WizardSettingsManager.TYPE + "=" + geneType.trim() + ") at node: " + GENE);
		}
	}

	//----------------------------------------------------------------------------------------------------
	private Object parseListElement(final Class<?> type, final String strValue) throws WizardLoadingException {
		if (type.equals(Double.class) || type.equals(Float.class) || type.equals(Double.TYPE) || type.equals(Float.TYPE)) {
			try {
				return Double.parseDouble(strValue);
			} catch (final NumberFormatException e) {
				throw new WizardLoadingException(true, "invalid content (" + strValue + ") at node: " + LIST_VALUE + 
						" (expected: real number)");
			}
		}
		
		try {
			return Long.parseLong(strValue);
		} catch (final NumberFormatException e) {
			throw new WizardLoadingException(true, "invalid content (" + strValue + ") at node: " + LIST_VALUE + 
					" (expected: integer number)");
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private ParameterInfo findParameterInfo(final List<ParameterInfo> parameters, final String name) {
		for (final ParameterInfo info : parameters) {
			if (name.equals(info.getName()))
				return info;
		}
		
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private IGASelectorConfigurator findSelectorConfigurator(final String selectorType) throws WizardLoadingException {
		for (final IGASelectorConfigurator configurator : selectors) {
			if (configurator.getName().equals(selectorType))
				return configurator;
		}
		
		throw new WizardLoadingException(true, "Unrecognized selector: " + selectorType + ".");
	}
	
	//----------------------------------------------------------------------------------------------------
	private IGAOperatorConfigurator findOperatorConfigurator(final String operatorType) throws WizardLoadingException {
		for (final IGAOperatorConfigurator configurator : geneticOperators) {
			if (configurator.getName().equals(operatorType))
				return configurator;
		}
		
		throw new WizardLoadingException(true, "Unrecognized genetic operator: " + operatorType + ".");
	}
	
	//----------------------------------------------------------------------------------------------------
	private Map<String,String> readProperties(final Node node) {
		final Map<String,String> prop = new HashMap<String,String>();
		final NodeList nodes = node.getChildNodes();
		if (nodes != null) {
			for (int i = 0; i < nodes.getLength(); ++i) {
				if (!(nodes.item(i) instanceof Element)) continue;
				final Element element = (Element) nodes.item(i);
				if (!element.getTagName().equals(PROPERTY)) continue;
				final String key = element.getAttribute(KEY);
				final Text text = (Text) element.getChildNodes().item(0);
				if (text == null) { 
					prop.put(key.trim(),"");
				} else {
					final String value = text.getNodeValue();
					prop.put(key.trim(),value.trim());
				}
			}
		}
		
		return prop;
	}

	//----------------------------------------------------------------------------------------------------
	public void invalidatePlugin() {
		content = null;
		selectors = Arrays.asList(new TournamentSelectorConfigurator(),new WeightedRouletteSelectorConfigurator(),new BestChromosomeSelectorConfigurator());
		geneticOperators = Arrays.asList(new GeneAveragingCrossoverOperatorConfigurator(), new CrossoverOperatorConfigurator(),new MutationOperatorConfigurator());
		
		init();
	}
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	private class ResultFileFitnessFunction extends FitnessFunction {
		
		//====================================================================================================
		// members
		
		private IParameterSweepResultReader currentReader;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public void setCurrentReader(final IParameterSweepResultReader currentReader) { this.currentReader = currentReader; } 

		//----------------------------------------------------------------------------------------------------
		protected double evaluate(final IChromosome a_subject) {
			if (currentReader == null)
				throw new IllegalStateException("Fitness function evaluation cannot access the results.");
			
			final List<ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>> combination = createCombinationFromChromosome(a_subject);
			try {
				final List<ResultValueInfo> infos = currentReader.getResultValueInfos(getSelectedFitnessFunction(),combination);
				if (infos == null || infos.size() == 0) {
					return getFitnessFunctionDirection() == FitnessFunctionDirection.MINIMIZE ? 1e19 : 0;  
				}
				
				if (Double.isInfinite(getFitnessValue(infos).doubleValue())) {
					return Double.MAX_VALUE;
				}
				
				if (Double.isNaN(getFitnessValue(infos).doubleValue())) {
					return getFitnessFunctionDirection() == FitnessFunctionDirection.MINIMIZE ? 1e19 : 0;
				}
				
				return getFitnessValue(infos).doubleValue();
			} catch (final ReadingException e) {
				throw new RuntimeException(e);
			}
		}

		//----------------------------------------------------------------------------------------------------
		private List<ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>> createCombinationFromChromosome(final IChromosome a_subject) {
			final List<ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>> combination = 
																	new ArrayList<ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>>(a_subject.getGenes().length);
			
			for (int i = 0; i < a_subject.getGenes().length; ++i) {
				final IIdentifiableGene geneWithId = (IIdentifiableGene) a_subject.getGene(i);
				final GeneInfo geneInfo = genes.get(whichGene(geneWithId.getId()));
				final ParameterInfo info = new ParameterInfo(geneInfo.getName(),geneInfo.getType(),geneInfo.getJavaType());
				
				final String strValue = String.valueOf(geneWithId.getAllele());
				info.setValue(ParameterInfo.getValue(strValue,info.getType()));
				
				combination.add((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>)InfoConverter.parameterInfo2ParameterInfo(info));
			}
			
			return combination;
		}
		
		//----------------------------------------------------------------------------------------------------
		private Number getFitnessValue(final List<ResultValueInfo> candidates) {
			if (candidates == null || candidates.isEmpty())
				return new Double(FitnessFunction.NO_FITNESS_VALUE);
			
			Collections.sort(candidates,new Comparator<ResultValueInfo>() {
				@Override
				public int compare(final ResultValueInfo info1, final ResultValueInfo info2) {
					final double tick1 = (Double) info1.getLabel();
					final double tick2 = (Double) info2.getLabel();
					
					return Double.compare(tick1,tick2);
				}
			});
			
			final Object value = candidates.get(candidates.size() - 1).getValue();
			return (Number) value;
		}
	}
}