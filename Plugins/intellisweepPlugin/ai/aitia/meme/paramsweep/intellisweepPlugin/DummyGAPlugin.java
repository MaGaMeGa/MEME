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
package ai.aitia.meme.paramsweep.intellisweepPlugin;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;

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
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.RecordableElement;
import ai.aitia.meme.paramsweep.gui.info.TimeInfo;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.Chromosome;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.DefaultMutation;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.GAOperatorManager;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.Gene;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.GeneInfo;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.IGAOperator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.OnePointCrossover;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.TournamentSelection;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.jgap.GAOperationException;
import ai.aitia.meme.paramsweep.internal.platform.InfoConverter;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.plugin.IIntelliDynamicMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IOptimizationMethodPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

public class DummyGAPlugin implements IIntelliDynamicMethodPlugin,
									  IOptimizationMethodPlugin,
									  ActionListener, 
									  ListSelectionListener,
									  ItemListener{
	private static final long serialVersionUID = -8300807593814982783L;
	
	//=========================================================================
	//members
	//constants
	protected static final String ADD_GENES = "add_genes";
	protected static final String SET_GENES = "set_genes";
	protected static final String SET_ALL_GENES = "set_all_genes";
	protected static final String REMOVE_ALL_GENES = "remove_all_genes";
	protected static final String MOD_GENE  = "mod_gene";
	protected static final String CANCEL	  = "cancel";
	protected static final String INTERVAL  = "interval";
	protected static final String INT_INTERVAL  = "int_interval";
	protected static final String LIST	  = "list";
	protected static final String SEL_OPS	  = "select_operators";
	protected static final String SEL_ALL	  = "select_all";
	protected static final String SEL_OP	  = "select_operator";
	protected static final String DESEL_OP  = "deselect_operator";
	protected static final String DESEL_ALL = "deselect_all";
	protected static final String CANCEL2	  = "cancel2";
	protected static final String SAVE_OP_SETTINGS = "save_operator_settings";
	protected static final String BROWSE_OP = "browse_operator";
	protected static final String IMPORT_OP = "import_operator";
	
	protected static final String ext = ".txt";
	protected static final String bckExt = ".bak";
	
	//Plugin-specific
	protected DefaultMutableTreeNode paramRoot = null;
	protected int numOfIterations = 2;
	protected int itCounter = 1;
	protected transient IIntelliContext context;
	private transient  DefaultMutableTreeNode recorderTree;
	protected List<ai.aitia.meme.paramsweep.gui.info.ParameterInfo> params = 
					new ArrayList<ai.aitia.meme.paramsweep.gui.info.ParameterInfo>();
	protected transient ArrayList<RecordableInfo> recList = new ArrayList<RecordableInfo>();
//	protected IParameterSweepResultReader resReader = null;
	
	protected String populationFileName = "population";
	protected boolean generatePopFile = true;
	protected transient int initialPopGenSeed = 1;
	
	protected transient String readyStatusDetail = "";
	protected Map<String,String> packageLocations = new HashMap<String,String>();
	
	protected transient int lastPackageIdx = 0;
	protected boolean stopWhenNoNew = false;
	protected boolean idPops = false;
	
//	protected transient boolean debug = true;
	protected transient boolean debug = false;
	
	protected boolean settingsWereLoaded = false;

	//GUI
	protected transient JPanel content = null;
	private transient JButton newParamsButtonCopy 	= null;
	private transient JButton newParamsButtonCopyHere = new JButton("Add new parameters...");
	//basic settings
	protected transient JSpinner popSizeSpinner = new JSpinner();
	protected transient JCheckBox needFileBox = null;
	protected transient JTextField filePathField = new JTextField();
	protected transient JTextField initPopSeedField = new JTextField();
	//search space settings
	protected transient JList genesList = new JList();
	protected transient JPanel geneSetterPanel = null;
	transient JPanel valuePanel = new JPanel( new CardLayout() );
	protected transient JTextField minField = new JTextField();
	protected transient JTextField maxField = new JTextField();
	protected transient JTextArea valuesArea = new JTextArea();
	protected transient JRadioButton intervalRButton = new JRadioButton( "values from an interval" );
	protected transient JRadioButton intIntervalRButton = new JRadioButton( "values from an integer interval" );
	protected transient JRadioButton listRButton = new JRadioButton( "values from a list" );
	protected transient JButton addGeneButton = new JButton( "Add/remove genes..." );
	protected transient JButton addAllGeneButton = new JButton( "Add all" );
	protected transient JButton removeAllGeneButton = new JButton( "Remove all" );
	protected transient JButton modSettingButton = new JButton( "Modify" );
	//settings
	protected transient JTabbedPane settingsPane;
	//add genes dialog
	protected transient JDialog addGeneDialog = null;
	protected transient JButton addGeneOKButton = new JButton( "OK" );
	protected transient JButton addGeneCancelButton = new JButton( "Cancel" );
	protected transient JTable addGenesTable = new JTable();
	protected transient JScrollPane addGenesPane = new JScrollPane( addGenesTable );
	//fitness function settings
	protected transient JPanel fitSettingsPanel = null;
	protected transient JComboBox fitFuncSelBox = new JComboBox();
	private JComboBox maxFitSelBox = new JComboBox();
	//stopping conditions
	protected transient JPanel stopSettingsPanel = new JPanel();
	protected transient JComboBox stopModeBox = new JComboBox();
	protected transient JCheckBox stopWhenNoNewPopsBox = 
		new JCheckBox( "Stop when no new solution was generated" );
	protected transient JSpinner genNumSpinner = new JSpinner();
	protected transient JTextField fitLimitField = new JTextField();
	//import GA operators
	protected transient JPanel importOpPanel = new JPanel();
	protected transient JComboBox selPackageBox = new JComboBox();
	protected transient JTextField importFromField = new JTextField( " " );
	protected transient JButton browseButton = new JButton( "Browse" );
	protected transient JButton importButton = new JButton( "Import" );
	protected transient JList importedList = new JList();
	protected transient JLabel importInfoLabel = new JLabel( " " ); 
	//reproduction
	protected transient JPanel reproSettingsPanel = new JPanel();
	//protected JComboBox pairOpBox = new JComboBox();
	protected transient JList selectedOpsList = new JList();
	protected transient JButton selOpButton = new JButton( "Select operator" );
	protected transient JButton selAllOpButton = new JButton( "Select all" );
	protected transient JButton deselOpButton = new JButton( "Deselect operator" );
	protected transient JButton deselAllOpButton = new JButton( "Deselect all" );
	protected transient JButton saveOpSettingsButton = new JButton( "Save settings" );
	protected transient JScrollPane opSettingsPane = new JScrollPane();
	//select operator dialog
	protected transient JDialog opSelectionDialog = null;
	protected transient JButton opSelOKButton = new JButton( "OK" );
	protected transient JButton opSelCancelButton = new JButton( "Cancel" );
	protected transient JList operatorList = new JList();
	protected transient JTextArea opInfoArea = new JTextArea();
	protected transient JScrollPane opSelPane = new JScrollPane( operatorList );
	
	//GA specific
	protected RecordableInfo fitnessFunc = null;
//	protected int fitnessFuncIdx = -1;
	
	protected List<GeneInfo> genes = new ArrayList<GeneInfo>(); //chromosome
	/** Only the params selected as genes are saved here: other parameters
	 *  are assumed to be constant and accessed trough the 'params' list. */
	protected Vector<Chromosome> population = new Vector<Chromosome>();
	protected Vector<Chromosome> descendants = null;
	//protected Vector<Double> winnerFitness = null;
	//protected Vector<Double> fitness = null;
	protected int populationSize = 12;  //debug ? 12 : 40;
	protected GAOperatorManager opManager = new GAOperatorManager();
	
	//protected boolean withImportantGenes = true;
	
	//messages
	protected static final String NO_GENE_MSG = 
		"The chromosome is empty: \n please select genes from model parameters!";
	protected static final String NO_OPERATOR_MSG = 
		"Please select at least one GA operator!";
	protected static final String NO_REC_MSG = 
		"No recorders added: \n please add a recorder at the Recorders page!";
	protected static final String NO_SELECTED_FITNESS_MSG = 
		"Please select a fitness function!";
	final static String NO_REC_FOUND_MSG = 
		"Please add recorded values to a recorder on the \"Data collection\" page";
	final static String REC_TIMING_MSG = 
		"Please set recording time to \"At the end of runs\" on the \"Data collection\" page";
	final static String BAD_REC_STRUCT_MSG =
		"Corrupted recorder - no timing information found";
	final static String WRONG_SEED_FORMAT_MSG =
		"Wrong seed format!";

	private boolean maximizeFitness = true;

	
	//=========================================================================
	//implemented interfaces
	public boolean alterParameterTree(IIntelliContext ctx) {
		if( context == null ){
			context = ctx;
		}

		populationFileName = 
			((DefaultMutableTreeNode)recorderTree.getChildAt( 0 )).getUserObject().toString() + "_" 
							+ populationFileName;
		
		//create initial population
		DefaultMutableTreeNode root = ctx.getParameterTreeRootNode();
		DefaultMutableTreeNode newRoot = getAlteredParameterTreeRootNode();
		//rngSeedManipulator.randomizeParameterTree( newRoot );
		root.removeAllChildren();
		int count = newRoot.getChildCount();
		
		for( int i = 0; i < count; ++i ){
	        root.add( (DefaultMutableTreeNode) newRoot.getChildAt( 0 ) );
        }
		return true;
	}

	public ParameterTree getNextParameterTree(IParameterSweepResultReader reader) {
		itCounter++;
		
//		resReader = reader;
		
		try{
			updateFitness(reader);
		} catch (ReadingException e) {
			return null;
		}
		
		population = descendants;
		
		if( generatePopFile ) printPopulation( itCounter - 1 );
		
		descendants = new Vector<Chromosome>();
		maximizeFitness = maxFitSelBox.getSelectedIndex() == 0;
		
		try {
			opManager.applySelectedOps( population, descendants, maximizeFitness );
		} catch (GAOperationException e) {
			return null;
		}
		
		//for safety: anything can happen
		if( descendants == null ) descendants = new Vector<Chromosome>();
		
		indentifySurvivors();
		
		DefaultMutableTreeNode node = paramRoot; //!

		int genIdx = 0;
		boolean wasNewSolution = true;
		for (int i = 0;i < node.getChildCount();++i) {
			ai.aitia.meme.paramsweep.gui.info.ParameterInfo paramInfo = 
				(ai.aitia.meme.paramsweep.gui.info.ParameterInfo) 
				((DefaultMutableTreeNode)node.getChildAt(i)).getUserObject();
			
			List<Object> values = new ArrayList<Object>();
			
			// collect new parameter values
			for( int j = 0; j < descendants.size(); ++j ){
				if( descendants.get( j ).getFitness().isNaN() ){
					if( isGene( params.get( i ) ) ){
						values.add( descendants.get( j ).geneAt( genIdx ).getValue() );
					}else{
						values.add( params.get( i ).getValue() );
					}
				}
			}
			
			if( values.size() == 0 ){
				wasNewSolution = false;
				break;
			}
			paramInfo.setDefinitionType(ai.aitia.meme.paramsweep.gui.info.ParameterInfo.LIST_DEF);
			paramInfo.setValues(values);
			
			if( isGene( params.get( i ) ) ) genIdx++;
		}
		
		//if there's no solution, generate the next tree
		if( !wasNewSolution ){
			idPops = true;
			if( noMoreIteration() ){
				return null;
			}
			return getNextParameterTree( reader );
		}
		
		return InfoConverter.node2ParameterTree(paramRoot);
	}

	public int getNumberOfIterations() { return numOfIterations; }

	public JPanel getSettingsPanel(IIntelliContext ctx) { 
		if( ctx != null ){
			context = ctx;
			newParamsButtonCopy = ctx.getNewParametersButton();
		}
		//newParamsButtonCopyHere.addActionListener( this );
		//GUIUtils.addActionListener(this, newParamsButtonCopyHere);
		if( content == null ){
			opManager.loadOperators();
			/*try{
				opManager.loadOperators( "JGAP", "e:\\SVN\\JGap\\jgap.jar" );
			}catch( Exception e ){
				e.printStackTrace();
			}*/
			if( params == null || params.size() == 0 ) loadParams();

			if( !settingsWereLoaded ) resetDefaultSettings();
			makeSettingsPanel();
		}
		return content; 
	}

	public boolean noMoreIteration() { 
		boolean noMore = itCounter > numOfIterations;
		if( stopWhenNoNew && !noMore ){ //when the switch is on
			noMore = idPops; //the last and the next population are not identical
		}
		
		return noMore;
	}

	public int getCurrentIteration() { return itCounter; }

	public void setParameterTreeRoot(DefaultMutableTreeNode root) {
		paramRoot = root;		
	}

	public void setRecordableVariables(DefaultMutableTreeNode root) {
		ArrayList<RecordableInfo> newList = new ArrayList<RecordableInfo>();
		this.recorderTree = root;
		
		//for( int i = 0; i < root.getChildCount(); ++i ){
		//ONLY THE FIRST RECORDER IS USED
		if( root.getChildCount() > 0 ){
			DefaultMutableTreeNode recorder = (DefaultMutableTreeNode) root.getChildAt( 0 );
			filePathField.setText( populationFileName );
			if( content != null ) content.revalidate();
			//first two children contains recorder meta data
			for( int j = 2; j < recorder.getChildCount(); ++j ){
				RecordableElement re = (RecordableElement) 
					((DefaultMutableTreeNode)recorder.getChildAt( j )).getUserObject();
				RecordableInfo recInfo = 
					new RecordableInfo(re.getAlias() != null ? re.getAlias() : re.getInfo().getName(),
									   re.getInfo().getJavaType(), re.getInfo().getName());
				if( !newList.contains( recInfo ) ){
					//recordableVarBox.addItem( re );
					newList.add( recInfo );
				}
			}
		}
		//}
		
		recList = newList;

		fillFitFuncBox();
		if( !settingsWereLoaded ) resetDefaultSettings();
	}

	public String settingsOK(DefaultMutableTreeNode recorders) {
		String settingsState = null;
		
		settingsState = checkFitnessFunction();
		if( settingsState != null ) return settingsState;
		
		settingsState = checkGenes();
		if( settingsState != null ) return settingsState;
		
		settingsState = checkOperators();
		if( settingsState != null ) return settingsState;
		
		settingsState = checkRecorders( recorders );
		if( settingsState != null ) return settingsState;
		
		return null;
	}

	public String getDescription() {
		return "Genetic algorithm plugin.";
	}

	public int getMethodType() { return DYNAMIC_METHOD; }

	public boolean getReadyStatus() {
		readyStatusDetail = checkFitnessFunction(); 
		if( null != readyStatusDetail ) return false;
		
		readyStatusDetail = checkGenes();
		if( null != readyStatusDetail ) return false;
		
		readyStatusDetail = checkOperators();
		if( null != readyStatusDetail ) return false;
		
		readyStatusDetail = checkRecorders( recorderTree );
		if( null != readyStatusDetail ) return false;

		readyStatusDetail = checkInitialPopSeed(); 
		if( null != readyStatusDetail ) return false;
		
		return true; 
	}

	public String getReadyStatusDetail() {
		return readyStatusDetail;
	}

	public void invalidatePlugin() { 
		content = null;
		geneSetterPanel = null;
		valuePanel = new JPanel( new CardLayout() );
		fitSettingsPanel = null;
		stopSettingsPanel = null;
		importOpPanel = null;
		reproSettingsPanel = null;
	}

	public boolean isImplemented() { return true; }

	public void load(IIntelliContext context, Element element)
			throws WizardLoadingException {
		this.context = context;
		loadParams();
		clearSettings();
		
		NodeList nl = element.getElementsByTagName( "generations" );
		if( nl != null && nl.getLength() != 0 )
			numOfIterations = 
				Integer.valueOf( ((Text)((Element)nl.item( 0 )).getChildNodes().item( 0 )).getNodeValue() );
		
		nl = element.getElementsByTagName( "population_size" );
		if( nl != null && nl.getLength() != 0 )
			populationSize = 
				Integer.valueOf( ((Text)((Element)nl.item( 0 )).getChildNodes().item( 0 )).getNodeValue() );
		
		nl = element.getElementsByTagName( "fitness_function" );
		if( nl != null && nl.getLength() != 0 ) {
//			fitnessFuncIdx = 
//				Integer.valueOf( ((Text)((Element)nl.item( 0 )).getChildNodes().item( 0 )).getNodeValue() );
			Element varElement = (Element) nl.item(0);
			String name = varElement.getAttribute("name");
			String type_str = varElement.getAttribute("type");
			String accessibleName = varElement.getAttribute("accessibleName");
			if (name == null)
				fitnessFunc = null;
			else {
				try {
					Class<?> type = toClass(type_str);
					fitnessFunc = new RecordableInfo(accessibleName,type,name);
				} catch (ClassNotFoundException e) {
					throw new WizardLoadingException(true,e);
				}
			}
		}

		nl = element.getElementsByTagName( "stopping" );
		if( nl != null && nl.getLength() != 0 )
			stopWhenNoNew = 
				Boolean.valueOf( ((Element)nl.item( 0 )).getAttribute( "when_no_new" ) );
		stopWhenNoNewPopsBox.setSelected( stopWhenNoNew );
		
		nl = element.getElementsByTagName( "population_file" );
		if( nl != null && nl.getLength() != 0 ){
			populationFileName = ((Element)nl.item( 0 )).getAttribute( "path" );
			generatePopFile = Boolean.valueOf(
				((Text)((Element)nl.item( 0 )).getChildNodes().item( 0 )).getNodeValue() );
		}
	
		//load genes
		nl = element.getElementsByTagName( "chromosome" );
		if( nl != null && nl.getLength() != 0 ){
			nl = ((Element)nl.item( 0 )).getElementsByTagName( "gene" );
			if( nl != null && nl.getLength() != 0 ){
				for( int i = 0; i < nl.getLength(); ++i ){
					String paramName = 
						String.valueOf( ((Text)((Element)nl.item( i )).
									getChildNodes().item( 0 )).getNodeValue() );
					ParameterInfo info = null;
					for( int j = 0; j < params.size(); ++j ){
						if( paramName.equals( params.get( i ).getName() ) ){
							info = params.get( i ).clone();
							break;
						}
					}
					if( info != null ){
						String value = ((Element)nl.item( i )).getAttribute( "min" );
						if( !"null".equals( value ) ){
							Double max = Double.valueOf( ((Element)nl.item( i )).getAttribute( "max" ) );
							GeneInfo gene = new GeneInfo( info.getName(), Double.valueOf( value ), 
												  max, info.getType(), info.getJavaType() );
							boolean onlyInteger = 
								Boolean.valueOf( ((Element)nl.item( i )).getAttribute( "only_int" ) );
							gene.setIntegerVals( onlyInteger );
							genes.add( gene );
							continue;
						}
						value = ((Element)nl.item( i )).getAttribute( "list" );
						if( !"null".equals( value ) ){
							List<Object> values = new ArrayList<Object>();
							StringTokenizer tokenizer = new StringTokenizer( value, " " );
							while( tokenizer.hasMoreTokens() ){
								values.add( tokenizer.nextToken() );
							}
							GeneInfo gene = new GeneInfo( info.getName(), values, info.getType(), 
												  info.getJavaType() );
							genes.add( gene );
							continue;
						}
						//min == null && list == null
						GeneInfo gene = null;
						if( "boolean".equalsIgnoreCase( info.getType() ) ||
								"string".equalsIgnoreCase( info.getType() ) ){
							gene = new GeneInfo( info.getName(), null, info.getType(), 
											 info.getJavaType() );
						}else{
							gene = new GeneInfo( info.getName(), null, null, info.getType(), 
											 info.getJavaType() );
						}
						genes.add( gene );
					}
				}
			}
		}
		
		//load descendants
		nl = element.getElementsByTagName("descendantList");
		if (nl != null && nl.getLength() > 0) {
			Element descList = (Element) nl.item(0);
			nl = descList.getElementsByTagName("descendant");
			
			descendants = new Vector<Chromosome>();
			for (int i = 0;i < nl.getLength();++i) {
				Element dElement = (Element) nl.item(i);
				double fitness = Double.parseDouble(dElement.getAttribute("fitness"));
				NodeList geneList = dElement.getElementsByTagName("gene");
				final Chromosome chromosome = new Chromosome();
				chromosome.setFitness(fitness);
				for (int j = 0;j < geneList.getLength();++j) {
					Element gElement = (Element) geneList.item(j);
					String name = gElement.getAttribute("name");
					String value = gElement.getAttribute("value");
					GeneInfo info = findGeneInfo(name);
					
					if (info == null)
						throw new WizardLoadingException(true,name + " is an unknown gene.");
					
					Object valueObj = getValueFromString(value,info.getType());
					chromosome.addGene(new Gene(info,valueObj));
				}
				descendants.add(chromosome);
			}
		}

		//load operator packages
		nl = element.getElementsByTagName( "operator_pcks" );
		if( nl != null && nl.getLength() != 0 ){
			nl = ((Element)nl.item( 0 )).getElementsByTagName( "package" );
			if( nl != null && nl.getLength() != 0 ){
				for( int i = 0; i < nl.getLength(); ++i ){
					String pckName = ((Element)nl.item( i )).getAttribute( "pck" );
					String path = ((Text)((Element)nl.item( i )).
									getChildNodes().item( 0 )).getNodeValue();
					packageLocations.put( pckName, path );
				}
			}
		}
		
		Iterator it = packageLocations.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        try{
	        	opManager.loadOperators( pairs.getKey().toString(), 
	        							 pairs.getValue().toString() );
	        }catch( Exception e ){
	        	Utilities.userAlert( content, "Couldn't load " + pairs.getKey().toString() +
	        			" operators from \n" + pairs.getValue().toString() );
	        }
	    }

		//load operators
		List<String> operatorNames = new ArrayList<String>();
		nl = element.getElementsByTagName( "operators" );
		if( nl != null && nl.getLength() != 0 ){
			nl = ((Element)nl.item( 0 )).getElementsByTagName( "operator" );
			if( nl != null && nl.getLength() != 0 ){
				for( int i = 0; i < nl.getLength(); ++i ){
					String name = ((Text)((Element)nl.item( i )).
									getChildNodes().item( 0 )).getNodeValue();
					operatorNames.add( name );
				}
			}
		}
		
		settingsWereLoaded = true;
		
		if( content == null ){
			opManager.loadOperators();
			for( int i = 0; i < operatorNames.size(); ++i ){
				IGAOperator op = opManager.getOperatorByName( operatorNames.get( i ) );
				if( op != null ) opManager.selectOp( op );
			}
			makeSettingsPanel();
		}
	}
	
	public void save(Node node) {
		populationSize = (Integer)popSizeSpinner.getValue();
		numOfIterations = (Integer)genNumSpinner.getValue();
		generatePopFile = needFileBox.isSelected();
		
		Document doc = node.getOwnerDocument();
		
		Element pluginElement = (Element) node;
		pluginElement.setAttribute("class",this.getClass().getName());
		
		Element gaElement = doc.createElement( "GA_settings" );
		node.appendChild( gaElement );

		Element generationNumElem = doc.createElement( "generations" );
		generationNumElem.appendChild( doc.createTextNode( String.valueOf( numOfIterations ) ) );
		gaElement.appendChild( generationNumElem );
		Element popSizeElem = doc.createElement( "population_size" );
		popSizeElem.appendChild( doc.createTextNode( String.valueOf( populationSize ) ) );
		gaElement.appendChild( popSizeElem );
		Element fitFuncElem = doc.createElement( "fitness_function" );
		if (fitnessFunc != null) {
			fitFuncElem.setAttribute("name",fitnessFunc.getName());
			fitFuncElem.setAttribute("type",fitnessFunc.getType().getName());
			fitFuncElem.setAttribute("accessibleName",fitnessFunc.getAccessibleName());
		}
//		fitFuncElem.appendChild( doc.createTextNode( String.valueOf( fitnessFuncIdx ) ) );
		gaElement.appendChild( fitFuncElem );
		Element stopElem = doc.createElement( "stopping" );
		stopElem.setAttribute( "when_no_new", String.valueOf( stopWhenNoNew ) );
		stopElem.appendChild( doc.createTextNode( "atIteration" ) );
		gaElement.appendChild( stopElem );
		
		//save genes and their value range
		Element genesElement = doc.createElement( "chromosome" );
		gaElement.appendChild( genesElement );
		for( int i = 0; i < genes.size(); ++i ){
			GeneInfo gene = genes.get( i );
			Element actElement = doc.createElement( "gene" );
			actElement.appendChild( doc.createTextNode( gene.getName() ) );
			actElement.setAttribute( "min", String.valueOf( gene.getMinValue() ) );
			actElement.setAttribute( "max", String.valueOf( gene.getMaxValue() ) );
			actElement.setAttribute( "only_int", String.valueOf( gene.isIntegerVals() ) );
			String vList = "";
			for( int j = 0; gene.getValueRange() != null && j < gene.getValueRange().size(); ++j ){
				if( j > 0 ) vList += " ";
				vList += gene.getValueRange().get( j );
			}
			if( vList.length() == 0 ) vList = "null";
			actElement.setAttribute( "list", vList );
			
			genesElement.appendChild( actElement );
		}
		
		//save descendant list
		Element descendantListElement = doc.createElement("descendantList");
		for (final Chromosome descendant : descendants) {
			Element dElement = doc.createElement("descendant");
			dElement.setAttribute("fitness",String.valueOf(descendant.getFitness()));
			for (int i = 0;i < descendant.getSize();++i) {
				Element gElement = doc.createElement("gene");
				gElement.setAttribute("name",String.valueOf(descendant.geneAt(i).getInfo().getName()));
				gElement.setAttribute("value",String.valueOf(descendant.geneAt(i).getValue()));
				dElement.appendChild(gElement);
			}
			descendantListElement.appendChild(dElement);
		}
		gaElement.appendChild(descendantListElement);
		
		//save genetic operator package locations
		Element opPcksElement = doc.createElement( "operator_pcks" );
		gaElement.appendChild( opPcksElement );
		Iterator it = packageLocations.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pairs = (Map.Entry)it.next();
	        Element elem = doc.createElement( "package" );
			elem.setAttribute( "pck", pairs.getKey().toString() );
			elem.appendChild( doc.createTextNode( pairs.getValue().toString() ) );
			opPcksElement.appendChild( elem );
	    }
		
		//save genetic operator names
		Element opsElement = doc.createElement( "operators" );
		gaElement.appendChild( opsElement );
		for( IGAOperator op : opManager.getSelectedOperators() ){
			Element elem = doc.createElement( "operator" );
			elem.appendChild( doc.createTextNode( op.getName() ) );
			opsElement.appendChild( elem );
		}

		Element genPopFileElem = doc.createElement( "population_file" );
		populationFileName = filePathField.getText();
		genPopFileElem.setAttribute( "path", populationFileName );
		genPopFileElem.appendChild( doc.createTextNode( String.valueOf( generatePopFile ) ) );
		gaElement.appendChild( genPopFileElem );
	}

	public String getLocalizedName() { return "GA Plugin"; }

	//actions
	public void actionPerformed( ActionEvent arg0 ) {
		if( arg0.getActionCommand().equals( ADD_GENES ) ){ //open dialog
			showAddGeneDialog();
		}else if( arg0.getActionCommand().equals( SET_GENES ) ){ //save genes
			Vector<Boolean> sel = ((GenesTableModel)addGenesTable.getModel()).getSelectedList();
			for( int i = 0; i < sel.size(); ++i ){
				ParameterInfo info = params.get( i );
				if( sel.get( i ) && !isGene( info ) ){
					GeneInfo newGene =
						params.get( i ).getType().equalsIgnoreCase( "boolean" ) ||
						params.get( i ).getType().equalsIgnoreCase( "string" ) ?
								new GeneInfo( info.getName(), null, info.getType(), info.getJavaType() ) : 
								new GeneInfo( info.getName(), null, null, info.getType(), info.getJavaType() );
					genes.add( newGene );
				}else if( !sel.get( i ) && isGene( params.get( i ) ) ){
					removeGene( params.get( i ) );
				}
			}
			genesList.setModel( getGenesListModel() );
			addGeneDialog.setVisible( false );
		}else if( arg0.getActionCommand().equals( SET_ALL_GENES ) ){ //save genes
			for( int i = 0; i < params.size(); ++i ){
				ParameterInfo info = params.get( i );
				if( !isGene( info ) ){
					GeneInfo newGene =
						params.get( i ).getType().equalsIgnoreCase( "boolean" ) ||
						params.get( i ).getType().equalsIgnoreCase( "string" ) ?
								new GeneInfo( info.getName(), null, info.getType(), info.getJavaType() ) : 
								new GeneInfo( info.getName(), null, null, info.getType(), info.getJavaType() );
					genes.add( newGene );
					if( addGenesTable != null && addGenesTable.getModel() != null &&
							addGenesTable.getModel() instanceof GenesTableModel ){
						((GenesTableModel)addGenesTable.getModel()).getSelectedList().set( i, true );
					}
				}
			}
			genesList.setModel( getGenesListModel() );
		}else if( arg0.getActionCommand().equals( REMOVE_ALL_GENES ) ){ //save genes
			genes = new ArrayList<GeneInfo>();
			if( addGenesTable != null ){
				addGenesTable.setModel( new GenesTableModel() );
			}
			genesList.setModel( getGenesListModel() );
		}else if( arg0.getActionCommand().equals( CANCEL ) ){ //close dialog
			addGeneDialog.setVisible( false );
		}else if( arg0.getActionCommand().equals( INTERVAL ) ){ //change to interval input type
			((CardLayout)valuePanel.getLayout()).show( valuePanel, "2" );
			if( !modSettingButton.isVisible() ) modSettingButton.setVisible( true );
		}else if( arg0.getActionCommand().equals( LIST ) ){ //change to list input type
			((CardLayout)valuePanel.getLayout()).show( valuePanel, "1" );
		}else if( arg0.getActionCommand().equals( MOD_GENE ) ){
			if( intervalRButton.isSelected() ){
				GeneInfo sel = ((GeneInfo)genesList.getSelectedValue());
				if( !GeneInfo.INTERVAL.equals( sel.getType() ) ){ //change type when needed
					sel.setValueType( GeneInfo.INTERVAL );
					sel.setValueRange( null );
				}
				try{	//parse interval lower bound
					Double min = Double.valueOf( minField.getText() );
					sel.setMinValue( min );
				}catch( NumberFormatException e ){
					Utilities.userAlert( content, "Minimum value is not valid.",
					"Please enter a scalar value!");
				}
				try{	//parse interval upper bound
					Double max = Double.valueOf( maxField.getText() );
					sel.setMaxValue( max );
				}catch( NumberFormatException e ){
					Utilities.userAlert( content, "Maximum value is not valid.",
					"Please enter a scalar value!");
				}
				sel.setIntegerVals( false );
			}else if( intIntervalRButton.isSelected() ){
				GeneInfo sel = ((GeneInfo)genesList.getSelectedValue());
				if( !GeneInfo.INTERVAL.equals( sel.getType() ) ){ //change type when needed
					sel.setValueType( GeneInfo.INTERVAL );
					sel.setValueRange( null );
				}
				try{	//parse interval lower bound
					Double min = Double.valueOf( minField.getText() );
					sel.setMinValue( min );
				}catch( NumberFormatException e ){
					Utilities.userAlert( content, "Minimum value is not valid.",
					"Please enter a scalar value!");
				}
				try{	//parse interval upper bound
					Double max = Double.valueOf( maxField.getText() );
					sel.setMaxValue( max );
				}catch( NumberFormatException e ){
					Utilities.userAlert( content, "Maximum value is not valid.",
					"Please enter a scalar value!");
				}
				sel.setIntegerVals( true );
			}else if( listRButton.isSelected() ){
				GeneInfo sel = ((GeneInfo)genesList.getSelectedValue());
				if( !GeneInfo.LIST.equals( sel.getType() ) ){ //change type when needed
					sel.setValueType( GeneInfo.LIST );
					sel.setMinValue( null );
					sel.setMaxValue( null );
				}
				List<Object> valueList = new ArrayList<Object>();
				String values = valuesArea.getText();
				StringTokenizer tokenizer = new StringTokenizer( values, " " );
				while( tokenizer.hasMoreTokens() ){
					valueList.add( tokenizer.nextToken() );
				}
				sel.setValueRange( valueList );
				sel.setIntegerVals( false );
			}
			//genesList.validate();
			genesList.repaint();
		}else if( arg0.getActionCommand().equals( SEL_OPS ) ){
			showOpSelectionDialog();
		}else if( arg0.getActionCommand().equals( SEL_OP ) ){
			int[] idxs = operatorList.getSelectedIndices();
			for( int i = 0; i < idxs.length; ++i ){
				opManager.selectOp( opManager.operatorAt( idxs[i] ) );
			}
			selectedOpsList.setModel( getSelOperatorsModel() );
			operatorList.clearSelection();
			opSelectionDialog.setVisible( false );
		}else if( arg0.getActionCommand().equals( SEL_ALL ) ){
			for( int i = 0; i < opManager.getOperators().size(); ++i ){
				opManager.selectOp( opManager.operatorAt( i ) );
			}
			selectedOpsList.setModel( getSelOperatorsModel() );
		}else if( arg0.getActionCommand().equals( DESEL_OP ) ){
			int[] idxs = selectedOpsList.getSelectedIndices();
			for( int i = 0; i < idxs.length; ++i ){
				opManager.deselectOp( opManager.operatorAt( idxs[i] ) );
			}
			selectedOpsList.setModel( getSelOperatorsModel() );
		}else if( arg0.getActionCommand().equals( DESEL_ALL ) ){
			for( int i = 0; i < opManager.getOperators().size(); ++i ){
				opManager.deselectOp( opManager.operatorAt( i ) );
			}
			selectedOpsList.setModel( getSelOperatorsModel() );
		}else if( arg0.getActionCommand().equals( CANCEL2 ) ){
			opSelectionDialog.setVisible( false );
		}else if( arg0.getActionCommand().equals( SAVE_OP_SETTINGS ) ){
			int idx = selectedOpsList.getSelectedIndex();
			if( idx > -1 ){
				opManager.selectedOpAt( idx ).saveSettings();
				//inform the user
				Utilities.userAlert( content, "Settings saved." );
			}
		}else if( arg0.getActionCommand().equals( IMPORT_OP ) ){
			if( selPackageBox.getSelectedIndex() == 0 ){
				importInfoLabel.setText( "Please select the source platform!" );
				return;
			}
			String lib = selPackageBox.getSelectedItem().toString();
			String libLoc = importFromField.getText();
			if( libLoc == null || libLoc.length() == 0 || " ".equals( libLoc ) ){
				importInfoLabel.setText( "Please specify the jar file location!" );
				return;
			}
			packageLocations.put( lib, libLoc );
			opManager.removeOperatorsByPackage( lib );
			importedList.setModel( createImportedPluginList( lib ) );
			try{
				opManager.loadOperators( lib, libLoc );
			}catch( Exception e ){
				Logger.logError("(GAOperatorManager) Error while loading %s operators.", lib );
				importInfoLabel.setText( "Error while loading " + lib + " operators." );
			}
			
			importedList.setModel( createImportedPluginList( lib ) );
			importInfoLabel.setText( importedList.getModel().getSize() + " " + lib + 
									 " plugins are loaded successfully" );
			//TODO add selected package to imported list (remove the previous location)
		}else if( arg0.getActionCommand().equals( BROWSE_OP ) ){
			JFileChooser fc = new JFileChooser();
			fc.setFileFilter( new JarFilter() );
			int returnVal = fc.showOpenDialog( content );

	        if( returnVal == JFileChooser.APPROVE_OPTION ){
	            File file = fc.getSelectedFile();
	            importFromField.setText( file.getAbsolutePath() );
	        }

		}else if( arg0.getActionCommand().equals( "NEW_PARAMETERS" ) ){
			newParamsButtonCopy.doClick();
			updateParametersList();
		}
	}
	
	public void valueChanged(ListSelectionEvent arg0) {
		if( !arg0.getValueIsAdjusting() ){
			if( arg0.getSource().equals( genesList ) ){
				intervalRButton.setEnabled( true );
				listRButton.setEnabled( true );
				
				GeneInfo sel = (GeneInfo)genesList.getSelectedValue();
				if( sel == null ) return;

				if( sel.getType().equalsIgnoreCase( "double" ) || 
						sel.getType().equalsIgnoreCase( "float") ){
					intIntervalRButton.setEnabled( true );
				}else{
					intIntervalRButton.setEnabled( false );
				}
				
				if( sel.getValueRange() == null ){
					if( !sel.isIntegerVals() ){
						intervalRButton.setSelected( true );
					}else{
						intIntervalRButton.setSelected( true );
					}
					((CardLayout)valuePanel.getLayout()).show( valuePanel, "2" );
					if( sel.getMinValue() != null )
						minField.setText( String.valueOf( sel.getMinValue() ) );
					else
						minField.setText( "" );
					if( sel.getMaxValue() != null )
						maxField.setText( String.valueOf( sel.getMaxValue() ) );
					else
						maxField.setText( "" );
				}else{
					listRButton.setSelected( true );
					((CardLayout)valuePanel.getLayout()).show( valuePanel, "1" );
					String values = "";
					for( int i = 0; sel.getValueRange() != null &&
									i < sel.getValueRange().size(); ++i ){
						if( i > 0 ) values += " ";
						values += sel.getValueRange().get( i );
					}
					valuesArea.setText( values );
				}
				
				if( !modSettingButton.isVisible() ) modSettingButton.setVisible( true );
			}else if( arg0.getSource().equals( operatorList ) ){
				int idx = operatorList.getSelectedIndex();
				if( idx > -1 )
					opInfoArea.setText( opManager.operatorAt( idx ).getDescription() );
			}else if( arg0.getSource().equals( selectedOpsList ) ){
				int idx = selectedOpsList.getSelectedIndex();
				if( idx > -1 )
					opSettingsPane.setViewportView( opManager.selectedOpAt(idx).getSettingspanel());
			}
		}
	}

	public void itemStateChanged(ItemEvent arg0) {
		if( arg0.getSource().equals( fitFuncSelBox ) ){
			//represents the recorded variable index in the recorder tree and
			//combobox has a default 0th element
			fitnessFunc = (fitFuncSelBox.getSelectedItem() instanceof RecordableInfo) ? (RecordableInfo) fitFuncSelBox.getSelectedItem() : null;  
//			fitnessFuncIdx = fitFuncSelBox.getSelectedIndex() - 1;
		}else if( arg0.getSource().equals( selPackageBox ) ){
			if( lastPackageIdx == selPackageBox.getSelectedIndex() ) return;
			boolean enabled = 0 == selPackageBox.getSelectedIndex() ? false : true;
				
			lastPackageIdx = selPackageBox.getSelectedIndex();
			String pck = selPackageBox.getSelectedItem().toString();
			importedList.setModel( createImportedPluginList( pck ) );
			importFromField.setText( packageLocations.get( pck ) );
			importInfoLabel.setText( " " );
			importFromField.setEnabled( enabled );
			browseButton.setEnabled( enabled );
			importButton.setEnabled( enabled );
		}
	}

	public RecordableInfo getFitnessFunction() {
		return fitnessFunc;
	}

	public void setFitnessFunction( RecordableInfo function ) {
		fitnessFunc = function;
	}

	public Object getOptimalValue() {
		
		throw new UnsupportedOperationException();

		// the implementation commented out below ignores the user setting whether we should maximize the fitness or minimize!
		
//		if( population == null ) return null;
//		if( population.size() == 0 ) return null;
//		
//		Double optimum = Double.NEGATIVE_INFINITY;
//		for( int i = 0; i < population.size(); ++i ){
//			if( population.get( i ).getFitness() > optimum ) 
//				optimum = population.get( i ).getFitness();
//		}
//				
//		return optimum;
	}

	//=========================================================================
	//private & protected functions
	//------------------------------GUI----------------------------------------
	protected void makeSettingsPanel(){
		//TODO make settings panel
		
		JPanel basicSettingsPanel = createBasicSettingsPanel();
		JPanel searchSpaceSettingsPanel = createSearchSpaceSettingsPanel();
		JTabbedPane gaSettingsPanel = createGASettinsPanel();
		
		content = FormsUtils.build( "f:p:g",
				"0 p ||" +
				"1 p ||" +
				"2 f:p:g",
				basicSettingsPanel, searchSpaceSettingsPanel,
				gaSettingsPanel ).getPanel();
	}
	
	protected JPanel createBasicSettingsPanel(){
		JPanel basicSP = null;
		
		JLabel popSizeLabel = new JLabel( "  Population size:" );
		JLabel initPopSeedLabel = new JLabel( "Population generation seed:" );
		initPopSeedField.setColumns( 15 );
		initPopSeedField.setText( String.valueOf( initialPopGenSeed ) );
		needFileBox = new JCheckBox( "Log solutions to file:" );
		needFileBox.setToolTipText( "Each solution, and its fitness will be " +
				"logged in the specified file when selected" );
		needFileBox.setSelected( generatePopFile );
		filePathField.setColumns( 15 );
		Dimension size = new Dimension( 50, popSizeSpinner.getPreferredSize().height );
		popSizeSpinner.setModel( new SpinnerNumberModel( populationSize, 2, Integer.MAX_VALUE, 1 ) );
		
		popSizeSpinner.setPreferredSize( size );
		genNumSpinner.setModel( new SpinnerNumberModel( numOfIterations, 1, Integer.MAX_VALUE, 1 ) );
		genNumSpinner.setPreferredSize( size );
		
		basicSP = FormsUtils.build( "p ~ p f:p:g p ~ p",
				"01_23 f:p ||" +
				"45___ f:p ||" /*+
				"6____ f:p"*/,
				popSizeLabel, popSizeSpinner,
				initPopSeedLabel, initPopSeedField,
				needFileBox, filePathField/*,
				setImpGenesBox*/ ).getPanel();

		basicSP.setBorder( BorderFactory.createTitledBorder( "Basic Settings" ) );
		
		return basicSP;
	}
	
	/**
	 * Creates a GUI for selecting genes from model parameters, and for setting
	 * gene behavior.
	 * 
	 * @return a panel for search space settings.
	 */
	protected JPanel createSearchSpaceSettingsPanel(){
		genesList.setModel( getGenesListModel() );
		genesList.addListSelectionListener( this );
		JScrollPane genePane = new JScrollPane( genesList );
		genePane.setBorder( BorderFactory.createTitledBorder( "Chromosome" ) );
		genePane.setPreferredSize( new Dimension( 320, 120 ) );
		valuesArea.setPreferredSize( new Dimension( 25, 35 ) );
		
		//build input panel for incremental parameter
		JPanel intPanel = FormsUtils.build( "p f:p:g",
				"01 p ||" +
				"23 p",
				new JLabel( "Minimum value: " ), minField,
				new JLabel( "Maximum value: " ), maxField ).getPanel();
		
		//build input panel for constant parameter
		JPanel listPanel = FormsUtils.build( "p f:p:g",
				"0_ p ||" +
				"11 p",
				new JLabel( "Values: " ), valuesArea ).getPanel();
		
		valuePanel.add( new JPanel(), "NULL" );
		valuePanel.add( listPanel, "1" );
		valuePanel.add( intPanel, "2" );
		
		JPanel selectPanel = FormsUtils.build( "f:p:g",
				"0 p ||" +
				"1 p ||" +
				"2 p",
				intervalRButton, listRButton,
				intIntervalRButton ).getPanel();

		//selectPanel.setPreferredSize( new Dimension( 30, 40 ) );
		intervalRButton.setEnabled( false );
		intervalRButton.setActionCommand( INTERVAL );
		intervalRButton.addActionListener( this );
		intIntervalRButton.setEnabled( false );
		intIntervalRButton.setActionCommand( INT_INTERVAL );
		intIntervalRButton.addActionListener( this );
		listRButton.setEnabled( false );
		listRButton.setActionCommand( LIST );
		listRButton.addActionListener( this );
		GUIUtils.createButtonGroup( intervalRButton, listRButton, intIntervalRButton );
		modSettingButton.setVisible( false );
		modSettingButton.setActionCommand( MOD_GENE );
		modSettingButton.addActionListener( this );
		
		geneSetterPanel = FormsUtils.build( "f:p:g p",
				"00 p ||" +
				"11 p ||" +
				"_2 p",
				selectPanel,
				valuePanel, modSettingButton ).getPanel();
		
		geneSetterPanel.setBorder( BorderFactory.createTitledBorder( "Gene Settings" ) );
		geneSetterPanel.setPreferredSize( new Dimension( 220, 180 ) );
		
		addGeneButton.setActionCommand( ADD_GENES );
		addGeneButton.addActionListener( this );
		addAllGeneButton.setActionCommand( SET_ALL_GENES );
		addAllGeneButton.addActionListener( this );
		removeAllGeneButton.setActionCommand( REMOVE_ALL_GENES );
		removeAllGeneButton.addActionListener( this );
		addGeneOKButton.setActionCommand( SET_GENES );
		addGeneOKButton.addActionListener( this );
		addGeneCancelButton.setActionCommand( CANCEL );
		addGeneCancelButton.addActionListener( this );
		newParamsButtonCopyHere.setActionCommand("NEW_PARAMETERS");
		newParamsButtonCopyHere.addActionListener( this );
		
		JPanel searchSpaceSP = FormsUtils.build( "f:p ~ f:p ~ f:p ~ f:p f:p:g f:p",
				"000001 f:p ||" +
				"2345_ f:p",
				genePane, geneSetterPanel, addGeneButton,
				addAllGeneButton, removeAllGeneButton,
				newParamsButtonCopyHere ).getPanel();
		
		searchSpaceSP.setBorder( BorderFactory.createTitledBorder( "Search Space Settings" ) );
		
		return searchSpaceSP;
	}
	
	protected JTabbedPane createGASettinsPanel(){
		if( settingsPane == null ){
			settingsPane = new JTabbedPane();
			Dimension size2 = new Dimension( 50, popSizeSpinner.getPreferredSize().height );
			maxFitSelBox.addItem("Higher");
			maxFitSelBox.addItem("Lower");
			fitSettingsPanel = FormsUtils.build( "p p f:p:g", 
					"01_ p ||" +
					"222 p ||" +
					"344 p ", 
					new JLabel( "Fitness function: " ), fitFuncSelBox,
					new JLabel( "Please add complex statistcs on the Data collection page!"),
					maxFitSelBox , "fitness value is the better." ).getPanel();
			genNumSpinner.setModel( new SpinnerNumberModel( numOfIterations, 1, 1000000, 1 ) );
			genNumSpinner.setPreferredSize( size2 );
			
			stopWhenNoNewPopsBox.setSelected( stopWhenNoNew );
			
			JPanel genPanel = FormsUtils.build( "p p p", 
					"01_ p ||" +
					"222 p", 
					new JLabel( "Number of generations: " ), genNumSpinner,
					stopWhenNoNewPopsBox ).getPanel();
			genPanel.setBorder( new TitledBorder( "Generations" ) );
			
			fitLimitField.setEnabled( false );
			
			JPanel fitRPanel = FormsUtils.build( "p f:p:g", 
					"01 p", 
					new JLabel( "Fitness reached: " ), fitLimitField ).getPanel();
			fitRPanel.setBorder( new TitledBorder( "Fitness goal" ) );
			
			stopSettingsPanel = FormsUtils.build( "p", 
					"0 p ||" +
					"1 p", 
					genPanel, fitRPanel ).getPanel();
			
			String[] items = opManager.getKnownPackageNames(); //add known packages' names
			selPackageBox.addItem( "Please select" );
			for( int i = 0; i < items.length; ++i ){
				selPackageBox.addItem( items[i] );
			}
			if( items == null || items.length == 0 ) selPackageBox.setEnabled( false ); 
			selPackageBox.addItemListener( this );
			importButton.setActionCommand( IMPORT_OP );
			importButton.addActionListener( this );
			browseButton.setActionCommand( BROWSE_OP );
			browseButton.addActionListener( this );
			importInfoLabel.setForeground( Color.red );
			importFromField.setEnabled( false );
			browseButton.setEnabled( false );
			importButton.setEnabled( false );
			//importFromField.setPreferredSize( new Dimension( 250, importFromField.getHeight() ) );
			JScrollPane importedPane = new JScrollPane( importedList );
			importedPane.setBorder( BorderFactory.createTitledBorder( "Imported operators" ) );
			
			importOpPanel = FormsUtils.build( "p ~ f:p ~ p ~ p ~ p ~ f:p:g", 
						"01____ p ||" +
						"23345_ p ||" +
						"666666 f:p:g ||" +
						"777777 p ", 
						new JLabel( "Import from package:" ),
						selPackageBox, new JLabel( "JAR file location:" ),
						importFromField, browseButton, importButton,
						importedPane, importInfoLabel ).getPanel();
	
			selOpButton.setActionCommand( SEL_OPS );
			selOpButton.addActionListener( this );
			opSelOKButton.setActionCommand( SEL_OP );
			opSelOKButton.addActionListener( this );
			selAllOpButton.setActionCommand( SEL_ALL );
			selAllOpButton.addActionListener( this );
			deselOpButton.setActionCommand( DESEL_OP );
			deselOpButton.addActionListener( this );
			deselAllOpButton.setActionCommand( DESEL_ALL );
			deselAllOpButton.addActionListener( this );
			opSelCancelButton.setActionCommand( CANCEL2 );
			opSelCancelButton.addActionListener( this );
			//connect selAll, desel, desel all
			selectedOpsList.setModel( getSelOperatorsModel() );
			selectedOpsList.addListSelectionListener( this );
			JScrollPane selOpPane = new JScrollPane( selectedOpsList );
			selOpPane.setBorder( BorderFactory.createTitledBorder( "Selected operators" ) );
			selOpPane.setPreferredSize( new Dimension( 200, 120 ) );
			opSettingsPane.setBorder( BorderFactory.createTitledBorder( "Operator settings" ) );
			saveOpSettingsButton.setActionCommand( SAVE_OP_SETTINGS );
			saveOpSettingsButton.addActionListener( this );
			reproSettingsPanel = FormsUtils.build( "p p f:p:g p", 
					"0055 f:p:g ||" +
					"12_6 p ||" +
					"34__ p", 
					selOpPane, selOpButton, deselOpButton, 
					selAllOpButton, deselAllOpButton, opSettingsPane,
					saveOpSettingsButton ).getPanel();
			
			
			settingsPane.addTab( "Fitness", fitSettingsPanel );
			settingsPane.addTab( "Stopping", stopSettingsPanel );
			settingsPane.addTab( "Reproduction", reproSettingsPanel );
			settingsPane.addTab( "Import operators", importOpPanel );
		}
		
		return settingsPane;
	}
	
	/**
	 * Creates a gene selection dialog.
	 */
	protected void showAddGeneDialog(){
		if ( addGeneDialog == null ) {
			addGeneDialog = new JDialog();
			loadParams();
			addGenesTable.setModel( new GenesTableModel() );
			addGenesPane.setPreferredSize( new Dimension( addGenesPane.getPreferredSize().width, 200 ) );
			addGeneDialog.setContentPane(
						FormsUtils.build("p ~ p f:p:g", 
						"000 f:p||" + 
						"12_ p", 
						addGenesPane,
						addGeneOKButton, addGeneCancelButton ).getPanel());
			addGeneDialog.setModal(true);
			addGeneDialog.setResizable( false );
			int height = addGenesPane.getPreferredSize().height + 
						 addGeneButton.getPreferredSize().height + 40;
			addGeneDialog.setSize( 500, height );
			addGeneDialog.setTitle( "Add/remove genes" );
		}
		addGeneDialog.setVisible( true );
	}
	
	/**
	 * Creates a gene selection dialog.
	 */
	protected void showOpSelectionDialog(){
		if ( opSelectionDialog == null ) {
			opSelectionDialog = new JDialog();
			//loadParams();
			operatorList.setModel( getOperatorsModel() );
			operatorList.addListSelectionListener( this );
			opSelPane.setPreferredSize( new Dimension( 300, 200 ) );
			opSelPane.setBorder( BorderFactory.createTitledBorder( "Available operators" ) );
			opInfoArea.setEditable(false);
			opInfoArea.setLineWrap(true);
			opInfoArea.setWrapStyleWord(true);
			opInfoArea.setColumns(40);
			opInfoArea.setPreferredSize( new Dimension( 300, 160 ) );
			JScrollPane opInfoPane = new JScrollPane( opInfoArea );
			opInfoPane.setBorder( BorderFactory.createTitledBorder( "Genetic operator information" ) );
			opSelectionDialog.setContentPane(
						FormsUtils.build("p p p f:p:g f:p", 
						"0001 f:p||" + 
						"23__ p|", 
						opSelPane, opInfoPane,
						opSelOKButton, opSelCancelButton ).getPanel());
			opSelectionDialog.setModal(true);
			opSelectionDialog.setResizable( true );
			int height = opSelPane.getPreferredSize().height + 
						 opSelOKButton.getPreferredSize().height + 40;
			opSelectionDialog.setSize( 600, height );
			opSelectionDialog.setTitle( "Select operator" );
		}
		opSelectionDialog.setVisible( true );
	}

	//------------------------------GUI----------------------------------------	
	protected String checkInitialPopSeed(){
		try{
			initialPopGenSeed = Integer.valueOf( initPopSeedField.getText().trim() );
		}catch( NumberFormatException e ){
			return WRONG_SEED_FORMAT_MSG + ": " + initPopSeedField.getText();
		}
		
		return null;
	}
	
	protected String checkFitnessFunction(){
		if( recList == null || recList.size() == 0 )
			return NO_REC_MSG;
//		fitnessFuncIdx = fitFuncSelBox.getSelectedIndex() - 1;
		fitnessFunc = (fitFuncSelBox.getSelectedItem() instanceof RecordableInfo) ? (RecordableInfo) fitFuncSelBox.getSelectedItem() : null;  
//		if( fitnessFuncIdx == -1 )
		if( fitnessFunc == null )
			return NO_SELECTED_FITNESS_MSG;
		
		return null;
	}
	
	protected String checkGenes(){
		if( genes == null || genes.size() == 0 )
			return NO_GENE_MSG;
		
		return null;
	}
	
	protected String checkOperators(){
		if( opManager.getSelectedOperators() == null ||
				opManager.getSelectedOperators().size() == 0)
			return NO_OPERATOR_MSG;
		
		return null;
	}

	 protected String checkRecorders( DefaultMutableTreeNode root ){
		String status = null;
		
		if( root.getChildCount() == 0 ){
			return NO_REC_FOUND_MSG;
		}
		
		for( int i = 0; i < root.getChildCount(); ++i ){
			DefaultMutableTreeNode recorder = (DefaultMutableTreeNode) root.getChildAt( i );
			TimeInfo timeInfo = null;
			
			if( recorder.getChildCount() < 3 )
				return NO_REC_FOUND_MSG;
			
			for( int j = 0; j < recorder.getChildCount(); ++j ){
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) recorder.getChildAt( j );
				
				if( node.getUserObject() instanceof TimeInfo ){
					timeInfo = (TimeInfo) node.getUserObject();
					if( timeInfo.getType() != TimeInfo.Mode.RUN ){
						return REC_TIMING_MSG;
					}
				}
			}
			
			if( timeInfo == null ) return BAD_REC_STRUCT_MSG;
		}
		
		return status;
	}
	
	protected void fillFitFuncBox(){
		fitFuncSelBox.removeAllItems();
		fitFuncSelBox.setPreferredSize( new Dimension( 100, 20 ) );
		fitFuncSelBox.addItem( "Please select..." );
		for (RecordableInfo elem : recList) {
            fitFuncSelBox.addItem( elem );
        }
		
//		fitFuncSelBox.setSelectedIndex( fitnessFuncIdx + 1 );
		if (fitnessFunc == null)
			fitFuncSelBox.setSelectedIndex(0);
		else
			fitFuncSelBox.setSelectedItem(fitnessFunc);
	}
	
	protected void loadParams(){
		if( context != null ){
			params = new ArrayList<ParameterInfo>();
			
			for (ParameterInfo parInf : context.getParameters()) {
				ParameterInfo pInfo = parInf.clone();
				params.add( pInfo );
	        }
		}
	}
	
	protected void removeGene( ParameterInfo info ){
		for( int i = 0; i < genes.size(); ++i ){
			if( genes.get( i ).getName().equals( info.getName() ) ){
				genes.remove( i );
				return;
			}
		}
	}
	
	protected DefaultListModel getGenesListModel(){
		DefaultListModel model = new DefaultListModel();
		for( GeneInfo gene : genes ){
			model.addElement(gene);
        }
		
		return model;
	}
	
	protected DefaultListModel getOperatorsModel(){
		DefaultListModel model = new DefaultListModel();
		for( IGAOperator op : opManager.getOperators() ){
			model.addElement( op.getName() );
        }
		
		return model;
	}
	
	protected DefaultListModel getSelOperatorsModel(){
		DefaultListModel model = new DefaultListModel();
		for( IGAOperator op : opManager.getSelectedOperators() ){
			if( op != null ) model.addElement( op.getName() );
        }
		
		return model;
	}
	
	protected DefaultListModel createImportedPluginList( String pck ){
		DefaultListModel model = new DefaultListModel();
		for( IGAOperator op : opManager.getOperatorsByPackage( pck ) ){
			model.addElement( op.getName() );
        }
		
		return model;
	}
	
	protected DefaultMutableTreeNode getAlteredParameterTreeRootNode(){
		numOfIterations = (Integer)genNumSpinner.getValue();
		populationSize = (Integer)popSizeSpinner.getValue();
		populationFileName = filePathField.getText().trim();
		stopWhenNoNew = stopWhenNoNewPopsBox.isSelected();
		try{
			initialPopGenSeed = Integer.valueOf( initPopSeedField.getText().trim() );
		}catch( NumberFormatException e ){
			//don't change seed then
		}
		//withImportantGenes = setImpGenesBox.isSelected();
		
		generateInitialPopulation();
		
		DefaultMutableTreeNode root = new DefaultMutableTreeNode( "Parameter file" );
		paramRoot = new DefaultMutableTreeNode( "Parameter file" ); 
		
		int genIdx = 0;
		for( int i = 0; i < params.size(); ++i ){
			ai.aitia.meme.paramsweep.gui.info.ParameterInfo paramInfo = 
															params.get( i );
	        paramInfo.setRuns( 1 );	//Run is always 1
	        
	        List<Object> values = new ArrayList<Object>();
	        
	        boolean isGene = isGene( paramInfo );
	        for( int j = 0; j < populationSize; ++j ){
	        	if( isGene )
	        		try{
	        			values.add( descendants.get( j ).geneAt( genIdx ).getValue() );
	        		}catch( NullPointerException e ){
	        			e.printStackTrace();
	        		}
	        	else{
	        		values.add( paramInfo.getValue() );
	        	}
			}
	        
	        if( isGene ) genIdx++;
	        
	        paramInfo.setDefinitionType( ai.aitia.meme.paramsweep.gui.info.ParameterInfo.LIST_DEF );
        	paramInfo.setValues( values );
	        
	        //add and save the node
	        root.add( new DefaultMutableTreeNode( paramInfo ) );
	        paramRoot.add( new DefaultMutableTreeNode( paramInfo ) );
        }
		
		return root;
	}
	
	protected void generateInitialPopulation(){
		descendants = new Vector<Chromosome>();
		params = context.getParameters();
		MersenneTwister emplerReqGenerator = new MersenneTwister( initialPopGenSeed );
		Uniform rnd = new Uniform( emplerReqGenerator );
		
		for( int i = 0; i < populationSize; ++i ){
			Chromosome solution =  new Chromosome();
			
			if( debug ) System.out.print( i );
			int geneIdx = 0;
			for( int j = 0; j < params.size(); ++j ){
				if( isGene( params.get( j ) ) ){
					Gene gene = new Gene( genes.get(geneIdx), null );
					gene.setValue( gene.getUniformRandomValue( rnd ) );
					solution.addGene( gene );
					geneIdx++;
					if( debug ) System.out.print( ";" + gene.getValue() );
				}
			}
			if( debug ) System.out.println();
			
			descendants.add( solution );
		}
		if( generatePopFile ) initPopFile();
	}
	
	protected void updateFitness(IParameterSweepResultReader resReader) throws ReadingException{
//		RecordableInfo recInfo = recList.get( fitnessFuncIdx );
		RecordableInfo recInfo = fitnessFunc;
		//fitness = new Vector<Double>();
		//if( itCounter == 2 ){
			for( int i = 0; i < descendants.size(); ++i ){
				if( !descendants.get( i ).getFitness().isNaN() ) continue;
				List<ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>> pCombo = 
					new ArrayList<ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>>();
				int genIdx = 0;
				for( int j = 0; j < params.size(); ++j ){
					Object value = null;
					if( isGene( params.get( j ) ) ){
						value = descendants.get( i ).geneAt( genIdx ).getValue();
						genIdx++;
					}else{
						value = params.get( j ).getValue();
					}
					ParameterInfo innerInfo = params.get( j ).clone();
					innerInfo.setDefinitionType(ParameterInfo.CONST_DEF);
					innerInfo.setValue(value);
					
					ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?> info = null;
					try{
						info = 
						(ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>)
						 InfoConverter.parameterInfo2ParameterInfo(innerInfo);
					}catch( ClassCastException e ){
						Logger.logError( "Cannot contvert parameter info: %s Problem is: \n%s", 
										 innerInfo.getName(), e.getLocalizedMessage() );
					}
					pCombo.add(info);
				}
				List<ResultValueInfo> results = resReader.getResultValueInfos(recInfo,pCombo);
				try{
					descendants.get( i ).setFitness( ((Number)results.get( 0 ).getValue()).doubleValue() );
				}catch( IndexOutOfBoundsException e ){
					Logger.logError( "%s", e.getLocalizedMessage() );
				}
				//fitness.add( (Double)results.get( 0 ).getValue() );
			}
		//}else{
			/*for( int i = 0; i < descendants.size(); ++i ){
				List<ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>> pCombo = 
					new ArrayList<ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>>();
				int genIdx = 0;
				for( int j = 0; j < params.size(); ++j ){
					//Object value = descendants.get( i ).geneAt( j ).getValue();
					Object value = null;
					if( isGene( params.get( j ) ) ){
						value = descendants.get( i ).geneAt( genIdx ).getValue();
						genIdx++;
					}else{
						value = params.get( j ).getValue();
					}
					ParameterInfo innerInfo = params.get( j ).clone();
					innerInfo.setDefinitionType(ParameterInfo.CONST_DEF);
					innerInfo.setValue(value);
					ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?> info = 
						(ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>)
						 InfoConverter.parameterInfo2ParameterInfo(innerInfo);
					pCombo.add(info);
				}
				List<ResultValueInfo> results = resReader.getResultValueInfos(recInfo,pCombo);
				descendants.get( i ).setFitness( (Double)results.get( 0 ).getValue() );
				fitness.add( (Double)results.get( 0 ).getValue() );
			}*/
		//}
	}
	
	protected Vector<List<Object>> cloneSolutions( Vector<List<Object>> solutions ){
		Vector<List<Object>> clone = new Vector<List<Object>>();
		
		for( int i = 0; i < solutions.size(); ++i ){
			List<Object> newSolution = new ArrayList<Object>();
			for( int j = 0; j < solutions.get( i ).size(); ++j ){
				newSolution.add( cloneObject( solutions.get( i ).get( j ) ) );
			}
			clone.add( newSolution );
		}
		
		return clone;
	}
	
	protected Object cloneObject( Object obj ){
		if( obj instanceof Double ) return new Double( (Double)obj );
		if( obj instanceof String ) return new String( (String)obj );
		if( obj instanceof Integer ) return new Integer( (Integer)obj );
		else return null;
	}
	
	@SuppressWarnings("cast")
	protected void updateParametersList(){
		Vector<ParameterInfo> parameters = new Vector<ParameterInfo>(context.getParameters());
		Vector<ParameterInfo> parametersToRemove = new Vector<ParameterInfo>();

		//Check if the listed parameters are still present:
		for (int i = 0; i < params.size(); i++) {
	        ParameterInfo info = (ParameterInfo) params.get(i);
	        boolean present = false;
	        for (Iterator<ParameterInfo> iter = parameters.iterator(); iter.hasNext();) {
	            ParameterInfo info2 = iter.next();
	            if(info2.getName().equals(info.getName())){
	            	present = true;
	            	//remove, we do not want to check it again:
	            	iter.remove();
	            }
            }
	        if(!present) parametersToRemove.add(info);
        }
		//Remove the non-existent parameters:
		for (ParameterInfo info : parametersToRemove) {
			params.remove(info);
        }
		//Add the new parameters to parameters:
		//The parameters to add remained in the params Vector:
		for (ParameterInfo info : parameters) {
	        params.add(info);
        }
	}

	//------------------------------------GA-----------------------------------
	/**
	 * Decides whether the parameter is a gene in the simulation.
	 * 
	 * @param info is the index of the parameter
	 * @return true when genes.get(i) is a gene in the simulation
	 */
	protected boolean isGene( ParameterInfo info ){
		for( int i = 0; i < genes.size(); ++i )
			if( genes.get( i ).getName().equals( info.getName() ) ) 
				return true;
		
		return false;
	}
	
	/**
	 * Identifies the chromosomes that are unaltered in the next population,
	 * and sets their fintess to the stored fitness value.
	 */
	protected void indentifySurvivors(){
		if( population == null ) return;
		
		for( int i = 0; i < descendants.size(); ++i ){
			Chromosome desc = descendants.get( i );
			boolean identified = false;
			for( Chromosome ch : population ){
				if( desc.getSize() != ch.getSize() ) continue; //not a likely case, however not disclosed
				boolean same = true;
				for( int j = 0; j < ch.getSize(); ++j ){
					if( !desc.geneAt( j ).getValue().equals( ch.geneAt( j ).getValue() ) ){
						same = false;
						break;
					}
				}
				if( same ){
					desc.setFitness( ch.getFitness() );
					identified = true;
					break;
				}
			}

			if( !identified ) 
				desc.setFitness( Double.NaN ); //set new solution's fitness to NaN
		}
	}
	
	/**
	 * Prints the header of the population file. 
	 */
	protected void initPopFile(){
		//create backup file when file already exists
		File file = new File( populationFileName + ext );
		if( file.exists() ){
			for( int i = 1; i <= 1000; ++i ){
				File newFile = new File( populationFileName + bckExt + i + ext );
				if( !newFile.exists() || i == 1000 ){
					file.renameTo( newFile );
					break;
				}
			}
		}
		try{
			PrintWriter popWriter = 
				new PrintWriter( new FileWriter(  populationFileName + ext , true ) );
			
			popWriter.print( "#pop" );
			for( int i = 0; i < params.size(); ++i ){
				popWriter.print( ";" + params.get( i ).getName() );
			}
			popWriter.print( ";fitness" );
			popWriter.println();
			popWriter.close();
		}catch( IOException e ){
			Logger.logError( "Cannot print population to file:\n%s", e );
		}
	}
	
	/**
	 * Prints the population to the file.
	 */
	protected void printPopulation( int iteration ){
		try{
			PrintWriter popWriter = 
				new PrintWriter( new FileWriter(  populationFileName + ext , true ) );
			
			for( int i = 0; i < population.size(); ++i ){
				popWriter.print( iteration );
				for( int j = 0; j < genes.size(); ++j ){
					popWriter.print( ";" + 
							population.get( i ).geneAt( j ).getValue().toString() );
				}
				popWriter.print( ";" + population.get( i ).getFitness() );
				popWriter.println();
			}
			popWriter.close();
		}catch( IOException e ){
			Logger.logError( "Cannot print population to file:\n%s", e );
		}
	}
	
	/**
	 * Sets the default settings.
	 */
	protected void resetDefaultSettings(){
		// clear settings
		clearSettings();
		
		// fitness function
		if( fitFuncSelBox != null && fitFuncSelBox.getModel().getSize() > 1 ){
			fitnessFunc = (fitFuncSelBox.getItemAt( 1 ) instanceof RecordableInfo) ? 
							(RecordableInfo) fitFuncSelBox.getItemAt( 1 ) : 
						    null;
			if( fitnessFunc != null )
				fitFuncSelBox.setSelectedItem( fitnessFunc );
		}
		
		// operators
		opManager.loadOperators();
		opManager.selectOp( opManager.getOperatorByName( new TournamentSelection().getName() ) );
		opManager.selectOp( opManager.getOperatorByName( new OnePointCrossover().getName() ) );
		opManager.selectOp( opManager.getOperatorByName( new DefaultMutation().getName() ) );
		selectedOpsList.setModel( getSelOperatorsModel() );
		
		// genes
		loadParams();
		for( int i = 0; i < params.size(); ++i ){
			ParameterInfo info = params.get( i );
			if( !isGene( info ) ){
				GeneInfo newGene =
					params.get( i ).getType().equalsIgnoreCase( "boolean" ) ||
					params.get( i ).getType().equalsIgnoreCase( "string" ) ?
							new GeneInfo( info.getName(), null, info.getType(), info.getJavaType() ) : 
							new GeneInfo( info.getName(), null, null, info.getType(), info.getJavaType() );
				genes.add( newGene );
				if( addGenesTable != null && addGenesTable.getModel() != null &&
						addGenesTable.getModel() instanceof GenesTableModel ){
					((GenesTableModel)addGenesTable.getModel()).getSelectedList().set( i, true );
				}
			}
		}
		genesList.setModel( getGenesListModel() );
	}
	
	/**
	 * Clears the plugin settings.
	 */
	protected void clearSettings(){
		fitnessFunc = null;
		opManager = new GAOperatorManager();
		opManager.loadOperators();
		selectedOpsList.setModel( getSelOperatorsModel() );
		genes = new ArrayList<GeneInfo>();
		genesList.setModel( new DefaultListModel() );
	}
	
    //----------------------------------------------------------------------------------------------------
    public static Class<?> toClass(String javaTypeStr) throws ClassNotFoundException {
    	if (javaTypeStr == null || "null".equals(javaTypeStr)) return null;
    	if (javaTypeStr.equals("void")) return Void.TYPE;
    	if (javaTypeStr.equals("byte")) return Byte.TYPE;
    	if (javaTypeStr.equals("short")) return Short.TYPE;
    	if (javaTypeStr.equals("int")) return Integer.TYPE;
    	if (javaTypeStr.equals("long")) return Long.TYPE;
    	if (javaTypeStr.equals("float")) return Float.TYPE;
    	if (javaTypeStr.equals("double")) return Double.TYPE;
    	if (javaTypeStr.equals("boolean")) return Boolean.TYPE;
    	if (javaTypeStr.equals("char")) return Character.TYPE;
    	Class<?> result = Class.forName(javaTypeStr); 
    	return result;
    }
    
    //----------------------------------------------------------------------------------------------------
	private GeneInfo findGeneInfo(final String name) {
		for (final GeneInfo gi : genes) {
			if (gi.getName().equals(name))
				return gi;
		}
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private Object getValueFromString(final String value, final String type) {
		if ("double".equalsIgnoreCase(type)) 
			return Double.valueOf(value);
		else if ("float".equalsIgnoreCase(type)) 
			return Float.valueOf(value);
		else if ("int".equalsIgnoreCase(type) || "integer".equalsIgnoreCase(type))
			return Integer.valueOf(value);
		else if ("short".equalsIgnoreCase(type)) 
			return Short.valueOf(value);
		else if ("byte".equalsIgnoreCase(type)) 
			return Byte.valueOf(value);
		else if ("long".equalsIgnoreCase(type)) 
			return Long.valueOf(value);
		else if ("boolean".equalsIgnoreCase(type)) 
			return Boolean.valueOf(value);
		else if ("string".equalsIgnoreCase(type))
			return value;
		return null;
	}

	//------------------------------------GA-----------------------------------
	
	//=========================================================================
	//inner classes
	protected class GenesTableModel extends AbstractTableModel {
		private static final long serialVersionUID = -33063027553895758L;
		Vector<Boolean> isGenes = null;

		public GenesTableModel() {
			isGenes = new Vector<Boolean>();
			for (int i = 0; i < params.size(); i++) {
				isGenes.add( new Boolean( isGene( params.get( i ) ) ) );
			}
		}

		@Override
		public String getColumnName(int columnIndex) {
			switch (columnIndex) {
			case 0:
				return "Selected";
			case 1:
				return "Parameter name";
			case 2:
				return "Type";
			case 3:
				return "Default value";
			default:
				throw new IllegalStateException();
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return (columnIndex == 0 );
		}

		public int getColumnCount() {
			return 4;
		}

		public int getRowCount() {
			return params.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return isGenes.get(rowIndex);
			case 1:
				return params.get(rowIndex).getName();
			case 2:
				return params.get(rowIndex).getType();
			case 3:
				return params.get(rowIndex).getValue() != null ? params.get(
						rowIndex).getValue().toString() : "null";
			default:
				throw new IllegalStateException();
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				isGenes.set(rowIndex, (Boolean) aValue);
			}
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnIndex == 0 ? Boolean.class : String.class;
		}

		public Vector<Boolean> getSelectedList() {
			return isGenes;
		}
	}
	
	public class JarFilter extends FileFilter {
	    @Override
		public boolean accept( File f ) {
	        if (f.isDirectory()) return true;

	        if( f.getName().endsWith( ".jar" ) ) return true;
	        else return false;
	    }

	    @Override
		public String getDescription() {
	        return ".jar";
	    }
	}

}
