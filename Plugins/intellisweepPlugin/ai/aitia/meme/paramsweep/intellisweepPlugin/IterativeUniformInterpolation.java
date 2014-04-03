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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.gui.IRngSeedManipulatorChangeListener;
import ai.aitia.meme.gui.RngSeedManipulator;
import ai.aitia.meme.paramsweep.batch.IParameterSweepResultReader;
import ai.aitia.meme.paramsweep.batch.ReadingException;
import ai.aitia.meme.paramsweep.batch.ResultValueInfo;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;
import ai.aitia.meme.paramsweep.generator.RngSeedManipulatorModel;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.RecordableElement;
import ai.aitia.meme.paramsweep.gui.info.TimeInfo;
import ai.aitia.meme.paramsweep.internal.platform.InfoConverter;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.plugin.IIntelliDynamicMethodPlugin;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

/**
 * An iterative interpolator plugin that interpolates a one variate one valued function. 
 * Generates an initial parameter tree from the user input and then generates a new 
 * tree at each iteration from the simulation data produced in the previous iteration. 
 * 
 * @author Attila Szabo
 */
public class IterativeUniformInterpolation implements IIntelliDynamicMethodPlugin, ListSelectionListener,
													  ActionListener, IRngSeedManipulatorChangeListener,
													  Serializable {
	
	private static final long serialVersionUID = 2262974348852305274L;
	
	protected static final String AGGREGATE_MAX = "Maximum";
	protected static final String AGGREGATE_MIN = "Minimum";
	protected static final String AGGREGATE_AVG = "Average";
	protected static final String AGGREGATE_NONE = "None"; //this one is not listed in the combobox
	
	protected transient JPanel content = null;
	protected transient JPanel parametersPanel = null;
	protected transient JScrollPane iterationsScrollPane = null;
	protected transient JScrollPane randomSeedsScrollPane = null;
	protected transient JLabel dimensionLabel = null;
	
	private String readyStatusDetail;
	protected int iterationCnt = 0;
	protected int numberOfIterations = 4;
	protected int numberOfIntervals = 4;
	protected double gradient = 0.0;
	protected double deviation = 0.01;
	protected int dimension = 1;
	protected int numOfIncrementParams = 0;
	protected Number actStep = 0;
	protected ArrayList<ParameterInfo> modParamsList = new ArrayList<ParameterInfo>();
	protected ArrayList<ParameterInfo> seedList = new ArrayList<ParameterInfo>();
	protected ArrayList<RecordableElement> selectedVars = new ArrayList<RecordableElement>();
	protected boolean hasSeed = false;
	
	protected ArrayList<Double> domain = new ArrayList<Double>();	//contains the result
	protected ArrayList<Double> range = new ArrayList<Double>();	//contains the result
	
	/** Lower bounds of further examined intervals. (Upper bounds are calculated using the 
	 *  storedIntervalSize member.) */
	protected ArrayList<Double> storedIntervals = new ArrayList<Double>();
	/** Previous interval size. */
	protected double storedIntervalSize;
	/** Curve gradients on the further examined intervals. */
	protected ArrayList<Double> storedGradients = new ArrayList<Double>();
	/** Indicates whether there's no more interesting intervals; */
	protected boolean noNewIntervals = false;
	
	protected RngSeedManipulator rngSeedManipulator = null;
	protected String selectedStatFunc = AGGREGATE_AVG;
	
	protected transient  JRadioButton constantRadioButton1 = null;
	protected transient  JRadioButton incrementRadioButton1 = null;
	protected transient  JTextField constantValue1TextField = null;
	protected transient  JTextField startValue1TextField = null;
	protected transient  JTextField toValue1TextField = null;
	protected transient  JTextField stepValue1TextField = null;
	protected transient  JTextField numOfItTextField = new JTextField( String.valueOf( numberOfIterations ) );
	protected transient  JTextField numOfIntTextField = new JTextField( String.valueOf( numberOfIntervals ) );
	protected transient  JTextField gradientTextField = new JTextField( String.valueOf( gradient ) );
	protected transient  JTextField deviationTextField = new JTextField( String.valueOf( deviation) );
	protected transient  JComboBox statisticalFuncComboBox = new JComboBox();
	protected transient  JComboBox recordableVarComboBox = new JComboBox();
	protected transient  JComboBox dimensionComboBox = null;
	protected transient  JPanel inputPanel1 = null;
	protected transient  JPanel inputPanel2 = null;
	protected transient  JList paramList = new JList();
	protected transient  JList paramList2 = new JList();
	protected transient  JButton saveParamButton = null;
	
	private transient  JButton newParametersButtonCopy = null;
	private transient  JButton newParametersButtonCopyHere = new JButton("Add new parameters...");
	
	protected transient  IIntelliContext context;
	protected transient  ArrayList<String> recordableVarList = new ArrayList<String>();
	private transient  DefaultMutableTreeNode recorderTree = null;
	

	DefaultMutableTreeNode paramRoot = null;
	
	final static String CONST_INPUT = "constant";
	final static String INCR_INPUT = "increment";
	public static final String PARAMETER = "parameter";
	public static final String PARAMETERS = "parameters";
	public static final String VARIABLE = "variable";
	public static final String VARIABLES = "variables";
	public static final String STAT = "stat";
	public static final String FUNCTION = "function";
	public static final String SETTINGS = "settings";
	public static final String HAS_SEED = "has-seed";
	
	final static String NO_REC_FOUND_MSG = 
		"please add recorded values to a recorder on the \"Data collection\" page";
	final static String REC_TIMING_MSG = 
		"please set recording time to \"At the end of runs\" on the \"Data collection\" page";
	final static String BAD_REC_STRUCT_MSG =
		"corrupted recorder - no timing information found";
	final static String BAD_NUM_MSG = 
		"not valid numeric value: ";
	final static String WRONG_DIM_MSG =
		"please set at least one and at the most 'dimension' parameter(s) to increment";
	
	/** Returns the localized (display-)name of the plugin */
	public String getLocalizedName(){
		return "Iterative Uniform Interpolation";
	}
	
	/**
	 * Checks and returns the ready status of the method setup process.
	 * @return the status of the IntelliSweep method setup. If everything is ready
	 * to go, then it returns <code>true</code> otherwise
	 * <code>false</code>.
	 */
	public boolean getReadyStatus(){
		selectedVars.clear();
		if( recordableVarComboBox.getSelectedItem() != null ){
			//remove type info
			RecordableElement var = 
				(RecordableElement) ((DefaultMutableTreeNode)recordableVarComboBox.getSelectedItem()).getUserObject();
			selectedVars.add(var);
		}
		if( statisticalFuncComboBox.getSelectedItem() != null ){
			selectedStatFunc = statisticalFuncComboBox.getSelectedItem().toString();
		}
		
		readyStatusDetail = "";
		if (!rngSeedManipulator.isNaturalVariationConsistent()) {
			readyStatusDetail = "You selected Variation analysis, but do not have at least one random seed\nwith Sweep setting, with more than one value.";
			return false;
		}
		if (!rngSeedManipulator.isBlockingConsistent()) {
			readyStatusDetail = "You selected Blocking design, but do not have at least one random seed\nwith Sweep setting, with more than one value.";
			return false;
		}
		
		return checkNumericParams() == null && checkParameters() == null && settingsOK(recorderTree) == null;
	}
	
	/**
	 * Checks and explains the ready status of the method setup process. It is 
	 * used in alert messages when a plugin is not ready, to inform the user.
	 * @return	An explanation of the ready status. It says something useful
	 * about why the plugin is not ready, and what should the user do about it.
	 */
	public String getReadyStatusDetail(){
		getReadyStatus();
		return readyStatusDetail + "\n" + settingsOK(recorderTree);
	}

	/**
	 * Saves the settings of the plugin as children of the given XML node. 1
	 * 
	 * @param node represents the parent XML node
	 */
	public void save(Node node) {
		Document doc = node.getOwnerDocument();
		
		Element pluginElement = (Element) node;
		pluginElement.setAttribute("class",this.getClass().getName());
		
		Element variablesElement = doc.createElement(VARIABLES);
		node.appendChild(variablesElement);
		
		Element variableElement = doc.createElement(VARIABLE);
		variablesElement.appendChild(variableElement);
		RecordableElement selectedVar = (RecordableElement) ((DefaultMutableTreeNode) recordableVarComboBox.getSelectedItem()).getUserObject();
		
		variableElement.setAttribute("name",selectedVar == null ? "null" : selectedVar.getInfo().getName());
		variableElement.setAttribute("type",selectedVar == null ? "null" : selectedVar.getInfo().getJavaType().getName());
		variableElement.setAttribute("alias",selectedVar == null ? "null" : selectedVar.getAlias());
		
		Element statElement = doc.createElement(STAT);
		node.appendChild(statElement);
		statElement.setAttribute(FUNCTION,selectedStatFunc);
		
		Element parametersElement = doc.createElement(PARAMETERS);
		node.appendChild(parametersElement);
		
		for (int i = 0;i < modParamsList.size();++i) {
			ParameterInfo param = modParamsList.get(i);
			Element parameterElement = doc.createElement(PARAMETER);
			parameterElement.setAttribute("name",param.getName());
			parameterElement.setAttribute("type", param.getType());
			parameterElement.setAttribute("start",param.getStartValue().toString());
			parameterElement.setAttribute("end",param.getEndValue().toString());
			parametersElement.appendChild( parameterElement );
		}
		
		Element settingsElement = doc.createElement(SETTINGS);
		node.appendChild(settingsElement);
		
		settingsElement.setAttribute("iteration_num",numOfItTextField.getText());
		settingsElement.setAttribute("interval_num",numOfIntTextField.getText());
		settingsElement.setAttribute("gradient", gradientTextField.getText());
		settingsElement.setAttribute("deviation", deviationTextField.getText());
		
		Element hasSeedElement = doc.createElement(HAS_SEED);
		node.appendChild(hasSeedElement);
		hasSeedElement.setAttribute("value",String.valueOf(hasSeed));
		
		Element rsmElement = doc.createElement(RngSeedManipulatorModel.RSM_ELEMENT_NAME);
		rngSeedManipulator.save(rsmElement);
		node.appendChild(rsmElement);
	}

	/**
	 * Loads the plugin settings from the given XML element.
	 * 
	 * @param context 
	 * @param element
	 * @throws WizardLoadingException 
	 */
	public void load(IIntelliContext context, Element element) throws WizardLoadingException{
		this.context = context;
		modParamsList = new ArrayList<ParameterInfo>();
		makeSettingsPanel();
		NodeList nl = element.getElementsByTagName(RngSeedManipulatorModel.RSM_ELEMENT_NAME);
		if (nl != null && nl.getLength() > 0) {
			Element rsmElement = (Element) nl.item(0);
			rngSeedManipulator.load(rsmElement);
		}
		
		nl = element.getElementsByTagName(STAT);
		if (nl != null && nl.getLength() > 0) {
			Element statElement = (Element) nl.item(0);
			String attrVal = statElement.getAttribute(FUNCTION);
			selectedStatFunc = attrVal;
			//for compatibility reasons, old aggregation operator names get converted to new ones
			if (selectedStatFunc.equalsIgnoreCase("AVG")) selectedStatFunc = AGGREGATE_AVG; 
			else if (selectedStatFunc.equalsIgnoreCase("MIN")) selectedStatFunc = AGGREGATE_MIN;
			else if (selectedStatFunc.equalsIgnoreCase("MAX")) selectedStatFunc = AGGREGATE_MAX;
			//conversion done
			statisticalFuncComboBox.setSelectedItem(selectedStatFunc);
		}
		
		nl = element.getElementsByTagName(SETTINGS);
		if (nl != null && nl.getLength() > 0) {
			Element settingsElement = (Element) nl.item(0);
			String attrVal = settingsElement.getAttribute("interval_num");
			numOfIntTextField.setText(attrVal);
			numberOfIntervals = Integer.parseInt(attrVal);
			attrVal = settingsElement.getAttribute("iteration_num");
			numOfItTextField.setText(attrVal);
			numberOfIterations = Integer.parseInt(attrVal);
			attrVal = settingsElement.getAttribute("gradient");
			gradientTextField.setText(attrVal);
			gradient = Double.parseDouble(attrVal);
			attrVal = settingsElement.getAttribute("deviation");
			deviationTextField.setText(attrVal);
			deviation = Double.parseDouble(attrVal);
		}
		
		nl = element.getElementsByTagName(PARAMETERS);
		if (nl != null && nl.getLength() > 0) {
			Element parametersElement = (Element) nl.item(0);
			NodeList paramNodes = parametersElement.getChildNodes();
			for (int i = 0;i < paramNodes.getLength();i++) {
				Element paramNode = (Element) paramNodes.item(i);
				String paramName = paramNode.getAttribute("name");
				String startVal = paramNode.getAttribute("start");
				String endVal = paramNode.getAttribute("end");
				int j = 0;
				while (j < paramList.getModel().getSize() && !((ParameterInfo)(paramList.getModel().getElementAt(j))).getName().equals(paramName))
					j++;
				if (j == paramList.getModel().getSize())
					throw new WizardLoadingException(true,"invalid reference: " + paramName);
				ParameterInfo foundParamInfo = (ParameterInfo) (paramList.getModel().getElementAt(j));
				foundParamInfo.setStartValue((Number)ParameterInfo.getValue(startVal,foundParamInfo.getType()));
				foundParamInfo.setEndValue((Number)ParameterInfo.getValue(endVal,foundParamInfo.getType()));
				foundParamInfo.setStep(0);
				foundParamInfo.setDefinitionType(ParameterInfo.INCR_DEF);
				if( !modParamsList.contains( foundParamInfo ) )
					modParamsList.add(foundParamInfo);
			}
		}
		
		nl = element.getElementsByTagName(VARIABLES);
		if (nl != null && nl.getLength() > 0) {
			Element variablesElement = (Element) nl.item(0);
			NodeList varList = variablesElement.getElementsByTagName(VARIABLE);
			if (varList != null && varList.getLength() > 0) {
				for (int i = 0;i < varList.getLength();++i) {
					Element varElement = (Element) varList.item(i);
					String name = varElement.getAttribute("name");
					String type_str = varElement.getAttribute("type");
					String alias = varElement.getAttribute("alias");
					try {
						Class<?> type = toClass(type_str);
						MemberInfo info = new MemberInfo(name,type.getSimpleName(),type);
						RecordableElement re = new RecordableElement(info);
						if (alias != null && alias.length() > 0)
							re.setAlias(alias);
						selectedVars.add(re);
						recordableVarComboBox.setSelectedItem(re);
					} catch (ClassNotFoundException e) {
						throw new WizardLoadingException(true,e);
					}
					
				}
			}
		}
		
		nl = element.getElementsByTagName(HAS_SEED);
		if (nl != null && nl.getLength() > 0) {
			Element hasSeedElement = (Element) nl.item(0);
			String str = hasSeedElement.getAttribute("value");
			if (str != null)
				hasSeed = Boolean.parseBoolean(str);
		}

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


	/**
	 * Notifies the plugin that its settings are obsolete. This is called when a
	 * new model is loaded.
	 */
	public void invalidatePlugin(){
		content = null;
	}
	
	private DefaultListModel getParameterList( List<ParameterInfo> parameters ){
		Vector<ParameterInfo> parameterInfos = new Vector<ParameterInfo>();
		for (ParameterInfo parInf : parameters) {
			ParameterInfo pInfo = parInf.clone();
			if( pInfo.getDefinitionType() != ParameterInfo.CONST_DEF &&
					!modParamsList.contains( pInfo ) ){
				modParamsList.add( pInfo );
			}
            parameterInfos.add( pInfo );
        }
		DefaultListModel vv = new DefaultListModel();
		for (ParameterInfo info : parameterInfos) {
            vv.addElement(info);
        }
		
		return vv;
	}
	
	/**
	 *  Creates the list of model parameters and a setter panel for them.
	 * 
	 * @param paramPane
	 * @param paramList
	 * @param constRB
	 * @param iRB
	 * @param inputPanel
	 * @param cTF
	 * @param startTF
	 * @param toTF
	 * @param stepTF
	 * @param saveButton
	 * @return
	 */
	private JPanel createSetterPanel( JScrollPane paramPane, JList paramList, JRadioButton constRB,
			JRadioButton iRB, JPanel inputPanel, JTextField cTF, JTextField startTF, JTextField toTF,
			JTextField stepTF, JButton saveButton ){
		
		inputPanel.setPreferredSize( new Dimension( 50, 80 ) );
		
		//build input panel for incremental parameter
		JPanel incrPanel = FormsUtils.build( "p f:p:g p",
				"01_ p ||" +
				"23_ p",
				new JLabel( "Start value: " ), startTF,
				new JLabel( "End value: " ), toTF ).getPanel();
		
		//build input panel for constant parameter
		JPanel constPanel = FormsUtils.build( "p f:p:g p",
				"01_ p",
				new JLabel( "Constant value: " ), cTF ).getPanel();
		
		inputPanel.add( new JPanel(), "NULL" );
		inputPanel.add( constPanel, CONST_INPUT );
		inputPanel.add( incrPanel, INCR_INPUT );
		
		//input type selection panel
		JPanel selectPanel = FormsUtils.build( "f:p:g",
				"0 p ||" +
				"1 p",
				constRB, iRB ).getPanel();
		selectPanel.setPreferredSize( new Dimension( 30, 50 ) );
		
		JPanel buttonPanel = FormsUtils.build( "p",
				"0 p",
				saveButton ).getPanel();
		
		//aggregates the input selection panel, the input panel, and the save button
		JPanel valuePanel = FormsUtils.build( "f:p:g f:p:g f:p:g",
				"0__ p ||" +
				"111 p ||" +
				"2__ p",
				selectPanel,
				inputPanel, buttonPanel ).getPanel();
		
		valuePanel.setBorder( BorderFactory.createTitledBorder( "Values" ) );
		valuePanel.setPreferredSize( new Dimension( 250, 80 ) );
		
		//group the radio buttons
		GUIUtils.createButtonGroup( constRB, iRB );
		paramPane.setPreferredSize( 
				new Dimension( 400, paramPane.getPreferredSize().height ) );
		
		//create the parameter setter stuff together
		JPanel contentPanel = FormsUtils.build( "f:p:g p",
				"01 f:p:g", 
				paramPane, valuePanel ).getPanel();
		//JScrollPane content = new JScrollPane( contentPanel );
		contentPanel.setPreferredSize( new Dimension( 250, 200 ) );
		
		return contentPanel;
	}
	
	/**
	 * Creates the parameter and output selection panel.
	 * 
	 * @param parameters .
	 * @param parameterList
	 * @return
	 */
	protected JPanel createParametersPanel( List<ParameterInfo> parameters, JList parameterList ){
		
		parameterList.setModel( getParameterList( parameters ) );
		parameterList.addListSelectionListener( this );
		JScrollPane parameterPane = new JScrollPane( parameterList );
		parameterPane.setBorder( BorderFactory.createTitledBorder( "Parameters" ) );
		
		//initialize components
		constantRadioButton1 = new JRadioButton( "Constant" );
		constantRadioButton1.setActionCommand( "CRB_1" );
		GUIUtils.addActionListener( this, constantRadioButton1 );
		constantRadioButton1.setEnabled( false );
		incrementRadioButton1 = new JRadioButton( "Increment" );
		incrementRadioButton1.setActionCommand( "IRB_1" );
		GUIUtils.addActionListener( this, incrementRadioButton1 );
		incrementRadioButton1.setEnabled( false );
		inputPanel1 = new JPanel( new CardLayout() );
		constantValue1TextField = new JTextField();
		startValue1TextField = new JTextField();
		toValue1TextField = new JTextField();
		stepValue1TextField = new JTextField();
		saveParamButton = new JButton( "Modify" );
		saveParamButton.setActionCommand( "SAVE_PARAM" );
		saveParamButton.setEnabled( false );
		GUIUtils.addActionListener( this, saveParamButton );
		newParametersButtonCopyHere.setActionCommand("NEW_PARAMETERS");
		
		//create the parameter setter panel
		JPanel panel = createSetterPanel( parameterPane, null, constantRadioButton1, 
				incrementRadioButton1, inputPanel1, constantValue1TextField, startValue1TextField,
				toValue1TextField, stepValue1TextField, saveParamButton );
		
		recordableVarComboBox.setActionCommand( "VAR_SELECTED" );
		
		JPanel contentPanel = FormsUtils.build( "p f:p:g f:p:g f:p:g",
				"0000 f:p:g |" +
				"1___ p |" +
				"23__ p",
				panel, 
				newParametersButtonCopyHere,
				new JLabel( "Output: " ), recordableVarComboBox ).getPanel();
		//JScrollPane content = new JScrollPane( contentPanel );
		contentPanel.setPreferredSize( new Dimension( 250, 250 ) );

		contentPanel.setBorder(BorderFactory.createTitledBorder("Parameter and output selection"));
		
		return contentPanel;
	}
	
	/**
	 * Create a setter panel for the model-independent plugin parameters (like number of iterations).
	 * 
	 * @return the created setter panel.
	 */
	private JScrollPane createIterationsScrollPane(){
		
		numOfItTextField.setPreferredSize( new Dimension( 50, numOfItTextField.getPreferredSize().height ) );
		numOfIntTextField.setPreferredSize( new Dimension( 50, numOfIntTextField.getPreferredSize().height ) );
		gradientTextField.setPreferredSize( new Dimension( 50, gradientTextField.getPreferredSize().height ) );
		deviationTextField.setPreferredSize( new Dimension( 50, deviationTextField.getPreferredSize().height ) );
		
		JPanel contentPanel = FormsUtils.build( "p p f:p:g p p f:p:g",
				"04_26_ p ||" +
				"15_37_ p ||",
				new JLabel( "Number of iterations: " ), new JLabel( "Number of intervals: " ), 
				new JLabel( "Gradient: " ), new JLabel( "Deviation: " ),
				numOfItTextField, numOfIntTextField, gradientTextField, deviationTextField ).getPanel();
		
		JScrollPane content = new JScrollPane( contentPanel );
		content.setPreferredSize( new Dimension( 250, 85 ) );

		content.setBorder(BorderFactory.createTitledBorder("Iterations"));
		
		return content;
	}
	
	/**
	 * Create the random seed management panel using the ai.aitia.meme.gui.RngSeedManipulator class.
	 * 
	 * @param parameters
	 * @param parameterList
	 * 
	 * @return the created panel.
	 */
	private JScrollPane createRandomSeedsScrollPane( List<ParameterInfo> parameters, JList parameterList ){		
		
		statisticalFuncComboBox.addItem( AGGREGATE_AVG );
		statisticalFuncComboBox.addItem( AGGREGATE_MAX );
		statisticalFuncComboBox.addItem( AGGREGATE_MIN );
		
		
		JPanel contentPanel = FormsUtils.build( "p f:p:g f:p:g f:p:g",
				"0000 f:p:g |" +
				"12__ p",
				rngSeedManipulator.getSeedsListPanel(),
				new JLabel( "Aggregate result: " ),
				statisticalFuncComboBox ).getPanel();
		JScrollPane content = new JScrollPane( contentPanel );

		content.setBorder(BorderFactory.createTitledBorder("Seed management"));
		content.setPreferredSize( new Dimension( 250, 200 ) );
		
		return content;
	}
	
	/**
	 * Creates the plugin's settings panel.
	 */
	protected void makeSettingsPanel(){
		if( content == null ){
			rngSeedManipulator = new RngSeedManipulator( context.getParameters(),null);
			rngSeedManipulator.addChangeListener( this );
			saveRngSeeds( rngSeedManipulator.getRandomSeedNames() );
			parametersPanel = createParametersPanel( context.getParameters(), paramList );
			iterationsScrollPane = createIterationsScrollPane();
			randomSeedsScrollPane = createRandomSeedsScrollPane( context.getParameters(), paramList2 );
			
			dimensionLabel = new JLabel("Dimension: ");
			
			dimensionComboBox = new JComboBox();
			dimensionComboBox.addItem( "1" );
			dimensionComboBox.setEnabled( false );
			dimensionComboBox.setPreferredSize( 
					new Dimension( 50, dimensionComboBox.getPreferredSize().height ) );
			
			stepValue1TextField.setEnabled( false );
			
			saveRngSeeds( rngSeedManipulator.getRandomSeedNames() );
			
			content = FormsUtils.build(	"p p f:p:g",
					"01_ p |" +
					"222 f:p:g |" +
					"333 p |" +
					"444 p",
					dimensionLabel, dimensionComboBox, parametersPanel,
					iterationsScrollPane, randomSeedsScrollPane ).getPanel();
		}
	}
	
	public JPanel getSettingsPanel(IIntelliContext ctx){
		if( context == null ){
			context = ctx;
			newParametersButtonCopy = ctx.getNewParametersButton();
			GUIUtils.addActionListener(this, newParametersButtonCopyHere);
		}
		makeSettingsPanel();
		return content;
	}	

	public void valueChanged(ListSelectionEvent arg0) {
		if( !arg0.getValueIsAdjusting() ){
			if( arg0.getSource().equals( paramList ) && paramList != null ){
				constantRadioButton1.setEnabled( true );
				incrementRadioButton1.setEnabled( true );
				saveParamButton.setEnabled( true );
				
				ParameterInfo selectedInfo = (ParameterInfo) paramList.getSelectedValue();
				
				if( selectedInfo == null ) return;
				
				if (selectedInfo.getDefinitionType() == ParameterInfo.CONST_DEF) {
					constantRadioButton1.setSelected( true );
					CardLayout cl = (CardLayout)( inputPanel1.getLayout() );
				    cl.show( inputPanel1, CONST_INPUT );
				    
				    constantValue1TextField.setText( String.valueOf( selectedInfo.getValue() ) );
				    startValue1TextField.setText( "" );
				    toValue1TextField.setText( "" );
				    stepValue1TextField.setText( "0" );
				} else if (selectedInfo.getDefinitionType() == ParameterInfo.INCR_DEF) {
					incrementRadioButton1.setSelected( true );
					CardLayout cl = (CardLayout)( inputPanel1.getLayout() );
				    cl.show( inputPanel1, INCR_INPUT );
				    
				    startValue1TextField.setText( String.valueOf( selectedInfo.getStartValue() ) );
				    toValue1TextField.setText( String.valueOf( selectedInfo.getEndValue() ) );
				    stepValue1TextField.setText( String.valueOf( selectedInfo.getStep() ) );
				    constantValue1TextField.setText( "" );
				}
				
			}else if( true ){	// ?
			}
		}
		
	}

	public void actionPerformed( ActionEvent ae ) {
		if(ae.getActionCommand().compareTo("NEW_PARAMETERS") == 0){
			newParametersButtonCopy.doClick();
			updateParametersList();
		}
		/*if( ae.getActionCommand().compareTo( "VAR_SELECTED" ) == 0 ){
			selectedVarIdx = recordableVarComboBox.getSelectedIndex();
		}*/else if( ae.getActionCommand().compareTo( "CRB_1" ) == 0 ){	//Constant Radio Button1
			
			CardLayout cl = (CardLayout)( inputPanel1.getLayout() );	
		    cl.show( inputPanel1, CONST_INPUT );
		    
		}else if( ae.getActionCommand().compareTo( "IRB_1" ) == 0 ){	//Increment Radio Button1
			
			CardLayout cl = (CardLayout)( inputPanel1.getLayout() );
		    cl.show( inputPanel1, INCR_INPUT );
		    
		}else if( ae.getActionCommand().compareTo( "CRB_2" ) == 0 ){	//Constant Radio Button2
			
			CardLayout cl = (CardLayout)( inputPanel2.getLayout() );
		    cl.show( inputPanel2, CONST_INPUT );
		    
		}else if( ae.getActionCommand().compareTo( "IRB_2" ) == 0 ){	//Increment Radio Button2
			
			CardLayout cl = (CardLayout)( inputPanel2.getLayout() );
		    cl.show( inputPanel2, INCR_INPUT );
		    
		}else if( ae.getActionCommand().compareTo( "SAVE_PARAM" ) == 0 ){	//save parameter settings
			
			ParameterInfo paramInfo = (ParameterInfo) paramList.getSelectedValue();
			int idx = paramList.getSelectedIndex();
			
			if( constantRadioButton1.isSelected() ){
				List<String> err = PlatformSettings.additionalParameterCheck(paramInfo,new String[] { constantValue1TextField.getText() },
																			 ParameterInfo.CONST_DEF);
				if (ParameterInfo.isValid(constantValue1TextField.getText(),paramInfo.getType()) && err.size() == 0) {
					modParamsList.remove( paramInfo );
					paramInfo.setValue( ParameterInfo.getValue( constantValue1TextField.getText(), paramInfo.getType() ) );
					paramInfo.setDefinitionType( ParameterInfo.CONST_DEF );
					( (DefaultListModel) paramList.getModel() ).remove( idx );
					( (DefaultListModel) paramList.getModel() ).add( idx, paramInfo );
					paramList.setSelectedIndex( idx );
					paramList.repaint();
				}
			}else if( incrementRadioButton1.isSelected() ){
				if( ParameterInfo.isValid( startValue1TextField.getText(), paramInfo.getType() ) &&
						ParameterInfo.isValid( toValue1TextField.getText(), paramInfo.getType() ) /*&&
						ParameterInfo.isValid( stepValue1TextField.getText(), paramInfo.getType() ) */ ){
					//paramInfo.clear();
					if( ! modParamsList.contains( paramInfo ) ){
						modParamsList.add( paramInfo );
						paramInfo.setStartValue( (Number)ParameterInfo.getValue( startValue1TextField.getText(), paramInfo.getType() ) );
						paramInfo.setStep( (Number)ParameterInfo.getValue( stepValue1TextField.getText(), paramInfo.getType() ) );
						paramInfo.setEndValue( (Number)ParameterInfo.getValue( toValue1TextField.getText(), paramInfo.getType() ) );
						paramInfo.setDefinitionType( ParameterInfo.INCR_DEF );
						( (DefaultListModel) paramList.getModel() ).remove( idx );
						( (DefaultListModel) paramList.getModel() ).add( idx, paramInfo );
						paramList.setSelectedIndex( idx );
					}
					paramList.repaint();
				}
			}
			
		}
		
	}
	
	/**
	 * Creates the parameter tree.
	 * 
	 * @return the root of the the created parameter tree.
	 */
	private DefaultMutableTreeNode getAlteredParameterTreeRootNode(){
		
		DefaultMutableTreeNode root = new DefaultMutableTreeNode( "Parameter file" );
		paramRoot = new DefaultMutableTreeNode( "Parameter file" ); 
		
		/* User set random seeds are removed from paramList1 during the plugin settings procedure
		 * to avoid duplicate parameter settings. Simply put them back to paramList1 here.
		 */
		putBackRngSeeds();
		
		hasSeed = rngSeedManipulator.getRandomSeedNames().length > 0; 
		
		for( int i = 0; i < paramList.getModel().getSize(); ++i ){
			
	        ParameterInfo paramInfo = (ParameterInfo) paramList.getModel().getElementAt( i );
	        paramInfo.setRuns( 1 );	//Run is always 1
	        
	        if( modParamsList.contains( paramInfo ) ){
	        	Number step = paramInfo.getEndValue().doubleValue() - paramInfo.getStartValue().doubleValue(); //!!!!!
	        	
	        	try{		//try to parse the number of intervals from text field
	        		if( ParameterInfo.isValid( numOfIntTextField.getText(), "int" ) )
	        			numberOfIntervals = Integer.valueOf( numOfIntTextField.getText() );
	        		else{
	        			numberOfIntervals = 1;
	        		}
	        	}catch( Exception e ){
	        		e.printStackTrace();
	        		numberOfIntervals = 1;
	        	}
	        	
	        	try{		//try to parse the number of iterations from text field
	        		if( ParameterInfo.isValid( numOfItTextField.getText(), "int" ) )
	        			numberOfIterations = Integer.valueOf( numOfItTextField.getText() );
	        		else{
	        			numberOfIterations = 1;
	        		}
	        	}catch( Exception e ){
	        		e.printStackTrace();
	        		numberOfIterations = 1;
	        	}
	        	
	        	try{		//try to parse the gradient from text field
	        		if( ParameterInfo.isValid( gradientTextField.getText(), "double" ) )
	        			gradient = Double.valueOf( gradientTextField.getText() );
	        		else{
	        			gradient = 1;
	        		}
	        	}catch( Exception e ){
	        		e.printStackTrace();
	        		gradient = 1;
	        	}
	        	
	        	try{		//try to parse the deviation from text field
	        		if( ParameterInfo.isValid( deviationTextField.getText(), "double" ) )
	        			deviation = Double.valueOf( deviationTextField.getText() );
	        		else{
	        			deviation = 1;
	        		}
	        	}catch( Exception e ){
	        		e.printStackTrace();
	        		deviation = 1;
	        	}
	        	
	        	//calculate the parameter's step value
	        	step = step.doubleValue() / numberOfIntervals;
	        	storedIntervalSize = step.doubleValue();
	        	ArrayList<Object> values = new ArrayList<Object>();
	        	
	        	for( int j = 0; j < numberOfIntervals + 1; ++j ){	//store parameter values to a list
	        		
	        		Number next = paramInfo.getStartValue().doubleValue() + ( j * step.doubleValue() );
	        		next = (Number) ParameterInfo.getValue( next.toString(), paramInfo.getType() );
	        		
	        		values.add( next );
	        	}
	        	
	        	paramInfo.setDefinitionType( ParameterInfo.LIST_DEF );
	        	paramInfo.setValues( values );
	        	
	        }
	        
	        //add and save the node
	        root.add( new DefaultMutableTreeNode( paramInfo ) );
	        paramRoot.add( new DefaultMutableTreeNode( paramInfo ) );
        }
		
		return root;
	}
	
	/**
	 * Alters the parameter tree (actually builds a new one).
	 * 
	 *  @param ctx contains the original parameter tree.
	 */
	public boolean alterParameterTree( IIntelliContext ctx ) {
		
		DefaultMutableTreeNode root = ctx.getParameterTreeRootNode();
		DefaultMutableTreeNode newRoot = getAlteredParameterTreeRootNode();
		rngSeedManipulator.getModel().randomizeParameterTree( newRoot );
		root.removeAllChildren();
		int count = newRoot.getChildCount();
		
		for( int i = 0; i < count; ++i ){
	        root.add( (DefaultMutableTreeNode) newRoot.getChildAt( 0 ) );
        }
		return true;
	}
	
	protected Number getAbsDifference( String value1, String value2 ){
		if( value1 == null || value2 == null )
			return -1;
		
		Number difference = 0;
		try{
			Number first = Double.valueOf( value1 );
			Number second = Double.valueOf( value2 );
			difference = java.lang.Math.abs( first.doubleValue() - second.doubleValue() );
		}catch( Exception e ){
			return -1;
		}
		
		return difference;
	}

	protected void aggregationOfResults(IParameterSweepResultReader reader, String aggregationMethod) throws ReadingException {
		MEMEApp.logError("***DEBUG: aggregation method=%s", aggregationMethod);
		RecordableElement re = selectedVars.get(0);
		RecordableInfo recInfo = new RecordableInfo(re.getAlias() != null ? re.getAlias() : re.getInfo().getName(),re.getInfo().getJavaType(),
				re.getInfo().getName()); 
		for (int i = 0;i < modParamsList.get(0).getValues().size();++i) {
			Object value = modParamsList.get(0).getValues().get(i);
			ParameterInfo innerInfo = modParamsList.get(0).clone();
			innerInfo.setDefinitionType(ParameterInfo.CONST_DEF);
			innerInfo.setValue(value);
			ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?> info = (ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>)
			InfoConverter.parameterInfo2ParameterInfo(innerInfo);
			List<ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>> pCombo = new ArrayList<ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>>();
			pCombo.add(info);
			List<ResultValueInfo> results = reader.getResultValueInfos(recInfo,pCombo);
			double xVal = Double.valueOf(value.toString());
			double aggregatedResult = 0.0;
			int numOfSeeds = 0;
			
			if (aggregationMethod.equals(AGGREGATE_AVG)) {
				//calculate the average that will be inserted to the result list
				for (int j = i;j < results.size();++j) {
					aggregatedResult += Double.valueOf(results.get(j).getValue().toString());
					++numOfSeeds;
				}
				aggregatedResult = aggregatedResult / numOfSeeds;
			} else if (aggregationMethod.equals(AGGREGATE_MIN)) {
				//calculate the minimum that will be inserted to the result list
				for (int j = i;j < results.size();++j) {
					double nextVal = Double.valueOf(results.get(j).getValue().toString());
					if (aggregatedResult > nextVal)
						aggregatedResult = nextVal;
				}
			} else if (aggregationMethod.equals(AGGREGATE_MAX)) {
				//calculate the maximum that will be inserted to the result list
				for (int j = i;j < results.size();++j) {
					double nextVal = Double.valueOf(results.get(j).getValue().toString());
					if (aggregatedResult < nextVal)
						aggregatedResult = nextVal;
				}
			} else if (aggregationMethod.equals(AGGREGATE_NONE)) {
				aggregatedResult = Double.valueOf(results.get(0).getValue().toString());
			}

			boolean inserted = domain.contains(xVal); 
			for (int j = 0;!inserted && j < domain.size() + 1;++j) {
				if (j == domain.size()) {	// add 'x' to the end of the domain list
					domain.add(xVal);
					range.add(aggregatedResult);
					inserted = true;
				} else if (xVal < domain.get(j)) { // insert 'x' into the ordered domain list
					domain.add(j,xVal);
					range.add(j,aggregatedResult);
					break;
				}
			}
		}
	}
	
	public ParameterTree getNextParameterTree(IParameterSweepResultReader reader) {
		if (iterationCnt > numberOfIterations) return null;
		
		try {
			if (reader == null)
				return null;
			
			++iterationCnt;
	
			//save data
			if (hasSeed) {
				aggregationOfResults(reader, selectedStatFunc);
			} else {
				aggregationOfResults(reader, AGGREGATE_NONE);
			}
		
			// find the parameter values for the next iteration by analyzing the gradients
			ArrayList<Integer> diffIdxs = new ArrayList<Integer>();
			ArrayList<Double> newGradients = new ArrayList<Double>();
			double actualGradient;
			for (int i = 0;i < range.size() - 1;++i) {
				Number y1 = range.get(i);
				Number y2 = range.get(i + 1);
				Number x1 = domain.get(i);
				Number x2 = domain.get(i + 1);
				Number diff = y2.doubleValue() - y1.doubleValue();
				actualGradient = gradient;
				
				if( iterationCnt > 2 ){
					for( int j = 0; j < storedIntervals.size(); ++j ){
						double parentIntervalSize = storedIntervalSize * numberOfIntervals;
						if( storedIntervals.get( j ) <= x1.doubleValue() && 
								x1.doubleValue() < storedIntervals.get( j ) + parentIntervalSize ){
							actualGradient = storedGradients.get( j );
							break;
						}
					}
				}
				
				//as 'domain' is strictly monotone ascending, x2 > x1
				diff = diff.doubleValue() / (x2.doubleValue() - x1.doubleValue());
				
				if( diff.doubleValue() > actualGradient + deviation || 
						diff.doubleValue() < actualGradient - deviation ){ 
					diffIdxs.add(i);
					newGradients.add( new Double( diff.doubleValue() ) );
				}
			}
			
			//store further examined intervals and the corresponding gradients
			storedIntervals = new ArrayList<Double>();
			storedGradients = newGradients;
			
			for( int i = 0; i < diffIdxs.size(); ++i ){
				storedIntervals.add( Double.valueOf( domain.get( diffIdxs.get( i ) ) ) );
			}
			
			//generate tree root then convert to ParameterTree
		
			DefaultMutableTreeNode node = paramRoot; //!
			
			if (diffIdxs.size() == 0){
				noNewIntervals = true;
				return null; // iterations interrupted in the lack of new inter-points
			}
				
			for (int i = 0;i < node.getChildCount();++i) {
				ParameterInfo paramInfo = (ParameterInfo) ((DefaultMutableTreeNode)node.getChildAt(i)).getUserObject();
				int idx = -1;
				if ((idx = modParamsList.indexOf(paramInfo)) >= 0) {
					modParamsList.set(idx,paramInfo);
					ArrayList<Object> values = new ArrayList<Object>();
	
					for (int j = 0;j < diffIdxs.size();++j) {
						Number lowerBound = domain.get(diffIdxs.get(j));
						Number upperBound = domain.get(diffIdxs.get(j) + 1);
						actStep = Math.abs(upperBound.doubleValue() - 
										lowerBound.doubleValue()) / numberOfIntervals;
						storedIntervalSize = actStep.doubleValue();
						
						for (int k = 0;k < numberOfIntervals - 1;++k) {
							Number interElement = lowerBound.doubleValue() + ((k + 1) * actStep.doubleValue());
						
							if (isDiscreteNumeric(paramInfo.getType()))
								interElement = Math.round(interElement.doubleValue());
							interElement = (Number) ParameterInfo.getValue(interElement.toString(),paramInfo.getType());
							
							if (values.size() == 0 || !interElement.equals(values.get(values.size() - 1)))
								values.add(interElement);
						}
					}
					
					paramInfo.setDefinitionType(ParameterInfo.LIST_DEF);
					paramInfo.setValues(values);
				}
			}
			
			rngSeedManipulator.getModel().randomizeParameterTree(paramRoot);
			return InfoConverter.node2ParameterTree(paramRoot);
		} catch (ReadingException e) {
			return null;
		}
	}
	
	protected void updateParametersList(){
		Vector<ParameterInfo> params = new Vector<ParameterInfo>(context.getParameters());
		Vector<ParameterInfo> parametersToRemove = new Vector<ParameterInfo>();

		//Check if the listed parameters are still present:
		for (int i = 0; i < paramList.getModel().getSize(); i++) {
	        ParameterInfo info = (ParameterInfo) paramList.getModel().getElementAt(i);
	        boolean present = false;
	        for (Iterator iter = params.iterator(); iter.hasNext();) {
	            ParameterInfo info2 = (ParameterInfo) iter.next();
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
			((DefaultListModel)(paramList.getModel())).removeElement(info);
        }
		//Add the new parameters to parameters:
		//The paramters to add remained in the params Vector:
		for (ParameterInfo info : params) {
	        ((DefaultListModel)(paramList.getModel())).addElement(info);
        }
	}
	
	 /**
	  * 
	  * @param type
	  * @return
	  */
	 public boolean isDiscreteNumeric(String type) {
		 if (type.equalsIgnoreCase("int") || type.equalsIgnoreCase("Integer") || type.equalsIgnoreCase("byte") || type.equalsIgnoreCase("long") || 
			 type.equalsIgnoreCase("short")) return true;
		 return false;
	 }
 
	
	public boolean noMoreIteration() { return iterationCnt == numberOfIterations || noNewIntervals; }
	
	public void setRecordableVariables( DefaultMutableTreeNode root ){
		//recordableVarComboBox.removeAllItems();
		this.recorderTree = root;
		
		for( int i = 0; i < root.getChildCount(); ++i ){
			DefaultMutableTreeNode recorder = (DefaultMutableTreeNode) root.getChildAt( i );
			//first two children contains recorder meta data
			for( int j = 2; j < recorder.getChildCount(); ++j ){
				DefaultMutableTreeNode variable = (DefaultMutableTreeNode) recorder.getChildAt( j );
				if( !recordableVarList.contains( variable.toString() ) ){
					recordableVarComboBox.addItem( variable );
					recordableVarList.add( variable.toString() );
				}
			}
		}
		
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
	
	protected String checkParameters(){
		String status = null;
		
		if( modParamsList.size() == 0 || modParamsList.size() > dimension ){
			status = WRONG_DIM_MSG + ": current number of non-constant parameters is " +
					 modParamsList.size();
		}
		
		return status;
	}
	
	protected String checkNumericParams(){
		String status = null;
		
		if( !ParameterInfo.isValid( numOfIntTextField.getText(), "int" ) ){
			numOfIntTextField.requestFocusInWindow();
			return BAD_NUM_MSG + "(Number of intervals) \"" + numOfIntTextField.getText() + "\"";
		}else if( !ParameterInfo.isValid( numOfItTextField.getText(), "int" ) ){
			numOfItTextField.requestFocusInWindow();
			return BAD_NUM_MSG + "(Number of iterations) \"" + numOfItTextField.getText() + "\"";
		}else if( !ParameterInfo.isValid( gradientTextField.getText(), "double" ) ){
			gradientTextField.requestFocusInWindow();
			return BAD_NUM_MSG + "(Gradient) \"" + gradientTextField.getText() + "\"";
		}else if( !ParameterInfo.isValid( deviationTextField.getText(), "double" ) ){
			deviationTextField.requestFocusInWindow();
			return BAD_NUM_MSG + "(Deviation) \"" + deviationTextField.getText() + "\"";
		}else if( Integer.valueOf( numOfIntTextField.getText() ) < 1 ){
			return BAD_NUM_MSG + "'Number of intervals' must be greater than 0 (currently " + 
			   numOfIntTextField.getText() + ").";
		}
		//number of intervals should be greater than 0
		//number of iterations should be greater or equal to 0
		
		return status;
	}
	
	public String settingsOK( DefaultMutableTreeNode recorders ){
		String settingsState = null;
		
		if( recorders != null ){
			settingsState = checkRecorders( recorders );
			if( ! ( settingsState == null ) ){
				return settingsState;
			}
		}
		
		settingsState = checkParameters();
		if( ! ( settingsState == null ) ){
			return settingsState;
		}
		
		settingsState = checkNumericParams();
		if( ! ( settingsState == null ) ){
			return settingsState;
		}
		
		//additional check
		
		return settingsState;
	}
	

	protected void saveRngSeeds( String[] seedNames ){
		seedList = new ArrayList<ParameterInfo>();
		
		for( int i = 0; i < seedNames.length; ++i ){
			for( int j = 0; j < paramList.getModel().getSize(); ++j ){
				ParameterInfo parameter = (ParameterInfo)paramList.getModel().getElementAt( j );
				if( seedNames[i].equalsIgnoreCase( parameter.getName() ) ){
					seedList.add( parameter );
					( (DefaultListModel) paramList.getModel() ).remove( j );
					break;
				}
			}
		}
	}
	
	protected void putBackRngSeeds(){
		for( int i = 0; i < seedList.size(); ++i ){
			( (DefaultListModel) paramList.getModel() ).addElement( seedList.get( i ) );
		}
	}
	
	public void rngSeedsChanged(){
		String[] actSeeds = rngSeedManipulator.getRandomSeedNames();
		
		putBackRngSeeds();
		saveRngSeeds( actSeeds );
		
		/*for( int i = 0; i < actSeeds.length; ++i ){
			numOfIntTextField.setText( actSeeds[0] );
		}*/
	}

	public String getDescription() {
	    return "The Iterative Uniform Interpolation method. Tihs method interpolates the selected output value of the model as a function of the " +
	    	   "chosen parameter. The method refines the parameter domain between iterations to achieve better interpolation of the output value." +
	    	   "\nA response analyzer method.";
    }

	public int getMethodType() { return DYNAMIC_METHOD; }
	public boolean isImplemented() { return true; }
	public void setParameterTreeRoot(DefaultMutableTreeNode root) { this.paramRoot = root; }
	public int getNumberOfIterations() { return numberOfIterations; }
	public int getCurrentIteration() { return iterationCnt; }
}
