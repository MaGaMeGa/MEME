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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.Population;
import org.jgap.impl.DefaultConfiguration;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ai.aitia.meme.paramsweep.batch.IParameterSweepResultReader;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.GASearchPanelModel;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.BestChromosomeSelectorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.CrossoverOperatorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.GeneAveragingCrossoverOperatorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.IGAOperatorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.IGASelectorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.MutationOperatorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.TournamentSelectorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.WeightedRouletteSelectorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.gene.ParameterOrGene;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.GeneInfo;
import ai.aitia.meme.paramsweep.internal.platform.InfoConverter;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.plugin.IIntelliDynamicMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IOptimizationMethodPlugin;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;

public class JgapGAPlugin implements IIntelliDynamicMethodPlugin, IOptimizationMethodPlugin, GASearchPanelModel {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 2516082442494840822L;
	
	private static final String POPULATION_SIZE = "Population size";
	private static final String POPULATION_RANDOM_SEED = "Population random seed";
	private static final String FIX_NUMBER_OF_GENERATIONS = "Fix number of generations";
	private static final String NUMBER_OF_GENERATIONS = "Number of generations";
	private static final String FITNESS_LIMIT_CRITERION = "Fitness limit";
	private static final String OPTIMIZATION_DIRECTION = "Optimization direction";
	private static final String FITNESS_FUNCTION = "Fitness function"; 
	private static final String FITNESS_STATISTICS = "Fitness statistics"; 
	private static final String FITNESS_TIMESERIES_LENGTH = "Fitness time-series length"; 

	protected static final String ext = ".txt";
	protected static final String bckExt = ".bak";
	
	private static final List<Class<?>> acceptableReturnTypesForFitnessFunction;
	
	static {
		final Class<?>[] array = { Byte.TYPE, Byte.class, Short.TYPE, Short.class, Integer.TYPE, Integer.class, Long.TYPE, Long.class, Float.TYPE, Float.class,
							 	   Double.TYPE, Double.class };
		acceptableReturnTypesForFitnessFunction = Arrays.asList(array);

	}

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

	private transient List<ModelListener> listeners = new ArrayList<GASearchPanelModel.ModelListener>();
	
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
	public void addFitnessFunction(final RecordableInfo fitnessFunction) {
		fitnessFunctions.add(fitnessFunction);
		
		if (listeners != null) {
			for (final ModelListener listener : listeners) 
				listener.fitnessFunctionAdded();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void removeAllFitnessFunctions() {
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
	public void setSelectedFitnessFunction(final RecordableInfo fitnessFunction) { this.selectedFunction = (RecordableInfo) fitnessFunction; }
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
	public void addParameter(final ParameterOrGene parameterOrGene) {
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
	public void removeAllParameters() {
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
	public String[] checkGAModel() {
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
	public boolean canBeFitnessFunction(final RecordableInfo candidate) {
		return acceptableReturnTypesForFitnessFunction.contains(candidate.getType());
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
		final List chromosomes = genotype.getPopulation().getChromosomes();
		@SuppressWarnings("unchecked")
		final Set<IChromosome> descendants = new HashSet<IChromosome>(chromosomes);
		
		for (int i = 0; i < paramList.size(); ++i) {
			final ParameterInfo paramInfo = paramList.get(i);
			paramInfo.setRuns(1); // Run is always 1

			final List<Object> values = new ArrayList<Object>();

			final int genIdx = whichGene(calculateName(paramInfo));
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

			final int genIdx = whichGene(calculateName(paramInfo));
			for (int j = 0; j < populationSize; ++j) {
				if (genIdx >= 0) {
					final String strValue = String.valueOf(descendants.getChromosome(j).getGene(genIdx).getAllele());
					values.add(ParameterInfo.getValue(strValue,paramInfo.getType()));
					paramInfo.setDefinitionType(ParameterInfo.LIST_DEF);
				} else {
					values.add(paramInfo.getValue());
					paramInfo.setDefinitionType(ParameterInfo.CONST_DEF);
				}
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
		final String newName = calculateName(info);  
		param.getGeneInfo().setName(newName);
	}
	
	//----------------------------------------------------------------------------------------------------
	private String calculateName(final ParameterInfo info) {
		ParameterInfo _info = info;
		
		if (_info != null) {
			String resultName = _info.getName();
			while ((_info instanceof ISubmodelGUIInfo) && ((ISubmodelGUIInfo)_info).getParent() != null) {
				_info = ((ISubmodelGUIInfo)_info).getParent();
				resultName = _info.getName() + "#" + resultName;
			}
			
			return resultName;
		}
		
		return null;
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
				public boolean isFitter(IChromosome a_chrom1, IChromosome a_chrom2) {
					return isFitter(a_chrom1.getFitnessValue(),a_chrom2.getFitnessValue());				}
				
				public boolean isFitter(double a_fitness_value1, double a_fitness_value2) {
					return optimizationDirection == FitnessFunctionDirection.MINIMIZE ? a_fitness_value1 < a_fitness_value2 : a_fitness_value1 > a_fitness_value2;
				}
			});
			gaConfiguration.setFitnessFunction(new ResultFileFitnessFunction());
			gaConfiguration.setRandomGenerator(new RandomGenerator() {
				private static final long serialVersionUID = 1L;

				final MersenneTwister randomGenerator = new MersenneTwister(populationGenerationSeed);

				public long nextLong() {
					return randomGenerator.nextLong();
				}

				public int nextInt(final int a_ceiling) {
					return randomGenerator.nextInt(a_ceiling);
				}

				public int nextInt() {
					return randomGenerator.nextInt();
				}

				public float nextFloat() {
					return randomGenerator.nextFloat();
				}

				public double nextDouble() {
					return randomGenerator.nextDouble();
				}

				public boolean nextBoolean() {
					return randomGenerator.nextBoolean();
				}
			});
			
			final Gene[] initialGenes = new Gene[getSelectedGenes().size()];
			int i = 0;
			for (final GeneInfo geneInfo : getSelectedGenes()) {
				if (GeneInfo.INTERVAL.equals(geneInfo.getValueType())) {
					if (geneInfo.isIntegerVals()) {
						initialGenes[i] = new IdentifiableLongGene(geneInfo.getName(),gaConfiguration,geneInfo.getMinValue().intValue(),
																	  geneInfo.getMaxValue().intValue());
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
			gaConfiguration.setBreeder(new DashboardBreeder());
			
			final Chromosome sampleChromosome = new Chromosome(gaConfiguration,initialGenes);
			gaConfiguration.setSampleChromosome(sampleChromosome);
			gaConfiguration.setPopulationSize(populationSize);

			for (final IGAOperatorConfigurator operator : selectedGeneticOperators) 
				gaConfiguration.addGeneticOperator(operator.getConfiguredOperator(gaConfiguration));
			
			for (IGASelectorConfigurator selectorConfig : selectedSelectionOperators)
				gaConfiguration.addNaturalSelector(selectorConfig.getSelector(gaConfiguration),true);

			gaConfiguration.addNaturalSelector(new StandardPostSelectorFixed(gaConfiguration),false);

			final Genotype initialGenotype = Genotype.randomInitialGenotype(gaConfiguration);
			
			return initialGenotype;
		} catch (final InvalidConfigurationException e) {
			e.printStackTrace();
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
			log.error("Cannot print population to file:\n",e);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	protected void printPopulation(final int iteration) {
		try {
			final PrintWriter popWriter = new PrintWriter(new FileWriter(new File(workspace,populationFileName + ext),true));
			
			Population population = genotype.getPopulation();
			for (int i = 0;i < population.size();++i) {
				popWriter.print(iteration);
				for (int j = 0;j < population.getChromosome(i).size(); ++j) 
					popWriter.print(";" + population.getChromosome(i).getGene(j).getAllele().toString());
				
				popWriter.print(";" + population.getChromosome(i).getFitnessValue());
				popWriter.println();
			}
			popWriter.close();
		} catch (final IOException e) {
			log.error( "Cannot print population to file.",e);
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
	
	//====================================================================================================
	// not implemented methods
	
	//----------------------------------------------------------------------------------------------------
	public void setRecordableVariables(final DefaultMutableTreeNode root) {
		throw new UnsupportedOperationException(); // intentionally not implemented
	}

	//----------------------------------------------------------------------------------------------------
	public String settingsOK(final DefaultMutableTreeNode recorders) {
		throw new UnsupportedOperationException(); // intentionally not implemented
	}

	//----------------------------------------------------------------------------------------------------
	public void setParameterTreeRoot(final DefaultMutableTreeNode root) {
		throw new UnsupportedOperationException(); // intentionally not implemented
	}


	//----------------------------------------------------------------------------------------------------
	public boolean getReadyStatus() {
		throw new UnsupportedOperationException(); // intentionally not implemented
	}

	//----------------------------------------------------------------------------------------------------
	public String getReadyStatusDetail() { 
		throw new UnsupportedOperationException(); // intentionally not implemented
	}

	//----------------------------------------------------------------------------------------------------
	public JPanel getSettingsPanel(final IIntelliContext ctx) {
		throw new UnsupportedOperationException(); // intentionally not implemented
	}

	//----------------------------------------------------------------------------------------------------
	public void save(final Node node) {
		throw new UnsupportedOperationException(); // intentionally not implemented
	}

	//----------------------------------------------------------------------------------------------------
	public void load(final IIntelliContext context, final Element element) throws WizardLoadingException {
		throw new UnsupportedOperationException(); // intentionally not implemented
	} 

	//----------------------------------------------------------------------------------------------------
	public void invalidatePlugin() {
		throw new UnsupportedOperationException(); // intentionally not implemented
	}


	@Override
	public RecordableInfo getSelectedFitnessStatisticsInfo() {
		final RecordableInfo selectedFitnessFunction = getSelectedFitnessFunction();
		IStatisticsPlugin selectedFitnessStatistics = getSelectedFitnessStatistics();
		int fitnessTimeSeriesLength = getFitnessTimeSeriesLength();
		// this seems to be useless, as the GASearchPanelModel can directly return the selected fitness function
//			final List<RecordableInfo> recordables = recorder.getRecordables();
//			for (final RecordableInfo recordableInfo : recordables) {
//				if (recordableInfo.equals(selectedFitnessFunction))
//					return recordableInfo;
//			}
		
		if (selectedFitnessStatistics.equals(GASearchHandler.noStatistics)){
			return selectedFitnessFunction;
		}
		
		// create a time series of the specified length using the selected fitness function
		Operator_TimeSeries timeSeriesOperator = new Operator_TimeSeries();
		OperatorsInfoGenerator timeSeriesInfoGenerator = new OperatorsInfoGenerator(timeSeriesOperator);
		
		List<Object> parameters = new ArrayList<Object>();
		
		MemberInfo fitnessMemberInfo = new MemberInfo(selectedFitnessFunction.getAccessibleName(), selectedFitnessFunction.getType().getSimpleName(), selectedFitnessFunction.getType());

		parameters.add(fitnessMemberInfo);
		parameters.add(fitnessTimeSeriesLength);
		
		OperatorGeneratedMemberInfo timeSeriesMemberInfo = timeSeriesInfoGenerator.generateInfoObject("timeseries", parameters.toArray());
		List<String> errors = timeSeriesInfoGenerator.getError();
		if (errors != null && errors.size() > 0) {
			throw new IllegalStateException("Could not create the timeseries of the last '" + selectedFitnessFunction.getName() + "' values!\n" + Utils.join(errors,"\n"));
		}
		timeSeriesMemberInfo.setGeneratorName(timeSeriesOperator.getClass().getName());
		timeSeriesMemberInfo.setBuildingBlock(parameters);
		
		// apply the selected statistics on the timeseries
		IStatisticInfoGenerator statisticsInfoGenerator = new StatisticsInfoGenerator(selectedFitnessStatistics);
		parameters = new ArrayList<Object>(selectedFitnessStatistics.getNumberOfParameters());
		List<Object> collectionParameter = new ArrayList<Object>();
		collectionParameter.add(timeSeriesMemberInfo);
		parameters.add(collectionParameter);
		SimpleGeneratedMemberInfo statMemberInfo = statisticsInfoGenerator.generateInfoObject(selectedFitnessStatistics.getName() + "_last" + fitnessTimeSeriesLength + "_" + selectedFitnessFunction.getName(), parameters.toArray());
		errors = statisticsInfoGenerator.getError();
		if (errors != null && errors.size() > 0) {
			throw new IllegalStateException("Could not create the specified statistics (" + selectedFitnessStatistics.getName() + ") of '" + selectedFitnessFunction.getName() + "'!\n" + Utils.join(errors,"\n"));
		}
		statMemberInfo.clearBuildingBlocks();
		statMemberInfo.setGeneratorName(selectedFitnessStatistics.getClass().getName());
		
		List<MemberInfo> l = new ArrayList<MemberInfo>(1);
		l.add(timeSeriesMemberInfo);
		statMemberInfo.addBuildingBlock(l);
		
		String alias = statMemberInfo.getName().endsWith("()") ? statMemberInfo.getName().substring(0, statMemberInfo.getName().length() - 2) : statMemberInfo.getName();
		RecordableElement stdRecordableElement = new RecordableElement(statMemberInfo, alias); 
		
		
		return InfoConverter.convertRecordableElement2RecordableInfo(stdRecordableElement);
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
				final List<ResultValueInfo> infos = currentReader.getResultValueInfos(getSelectedFitnessStatisticsInfo(),combination);
				if (infos == null || infos.size() == 0) {
					return 1e19; // dirty hack to make Ross going; 
				}
				
				if(Double.isInfinite(getFitnessValue(infos).doubleValue())){
					return Double.MAX_VALUE;
				}
				if(Double.isNaN(getFitnessValue(infos).doubleValue())){
					return 1e19;
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
			
			for (int i = 0;i < a_subject.getGenes().length;++i) {
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
//					return new Double(optimizationDirection == FitnessFunctionDirection.MAXIMIZE ? 0. :  Double.MAX_VALUE);
			
			Collections.sort(candidates,new Comparator<ResultValueInfo>() {
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