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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javassist.CtClass;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.batch.IParameterSweepResultReader;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;
import ai.aitia.meme.paramsweep.gui.info.GeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.RecordableElement;
import ai.aitia.meme.paramsweep.gui.info.ScriptGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.IScriptSupport;
import ai.aitia.meme.paramsweep.internal.platform.InfoConverter;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.parser.ScriptParser;
import ai.aitia.meme.paramsweep.platform.IScriptChecker;
import ai.aitia.meme.paramsweep.platform.Platform;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.platform.repast.info.GeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.ScriptGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.plugin.IIntelliDynamicMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IOptimizationMethodPlugin;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

/**
 * This plugin implements the Active Nonlinear Tests method.
 * 
 * @author Attila Szabo
 */
public class ActiveNonlinearTests implements IIntelliDynamicMethodPlugin, Serializable,
		ActionListener {
	//=========================================================================
	// Members
	private static final long serialVersionUID = -22351676589309951L;
	
	protected transient IIntelliContext context;
	/** Root of the parameter tree. */
	protected DefaultMutableTreeNode paramRoot = null;
	/** Root of the recorder tree. */
	private transient  DefaultMutableTreeNode recorderTree;
	/** List of the optimization methods. */
	private List<IOptimizationMethodPlugin> optimizationPlugins;
	/** The encapsulated optimization method. */
	protected IOptimizationMethodPlugin optMethod;
	/** Identifier of the object function. */
	protected String objectFunctionName = "";
	/** Object function. */
	protected ScriptGeneratedMemberInfo objectFunction;
	/** The object value. */
	protected double objectValue = 0.0;
	/** The allowed perturbation. */
	protected double perturbation = 0.0;
	/** List of the recorded variables (potential fitness functions). */
	protected transient ArrayList<RecordableInfo> recList = 
		new ArrayList<RecordableInfo>();
	
	private IScriptSupport scriptSupport;
	
	// GUI elements
	/** The plugin's settings panel. */
	protected transient JPanel content = null;
	private transient JButton newParametersButtonCopy = null;
	private transient JButton newParametersButtonCopyHere = 
									new JButton( "Add new parameters..." );
	/** Enables to select object function from the list of possible functions. */
	private transient JComboBox objectFSelBox;
	/** Links to the object function definition GUI. */
	private transient JButton defObjectFButton;
	/** Displays information about the object function. */
	private transient JLabel objectFInfoLabel;
	/** Contains the object value. */
	private transient JTextField objValueField = new JTextField();
	/** Contains the acceptable perturbation. */
	private transient JTextField perturbationField = new JTextField();
	/** Combo box of the available optimization methods. */
	private transient JComboBox optMethodSelBox;
	/** Links to the optimization method's settings page. */
	private transient JButton setOptMethodButton;
	/** Displays information about the selected optimization method. */
	private transient JLabel optMethodInfoLabel = new JLabel();
	
	// Error messages
	private String NO_OPT_MSG = 
		"Please select an optimization method!";
	private String NO_FITNESS_FUNC_MSG = 
		"Please select a fitness function";
	private String WRONG_OBJ_VAL_MSG = 
		"Object value should be a valid double value! Found: ";
	private String WRONG_PERT_VAL_MSG = 
		"Perturbation value should be a valid double value! Found: ";
	
	//=========================================================================
	// Constructors
	public ActiveNonlinearTests(){
		// TODO add optimization methods dynamically
		optimizationPlugins = new ArrayList<IOptimizationMethodPlugin>();
		optimizationPlugins.add( new DummyGAPlugin() );
	}
	
	//=========================================================================
	// Implemented interfaces

	public void actionPerformed( ActionEvent ae ) {
		if( ae.getActionCommand().equals( "define object function" ) ){
			JOptionPane.showMessageDialog( content, "Define object function on " +
					"the Data Collection Page!", "Define object function", 
					JOptionPane.INFORMATION_MESSAGE );
		}else if( ae.getActionCommand().equals( "optimalization method settings" ) ){
			if( optMethod != null ){
				//optMethod.invalidatePlugin();
				JDialog settingsDialog = new JDialog();
				settingsDialog.setTitle( optMethod.getLocalizedName() + " settings" );
				settingsDialog.getContentPane().add( optMethod.getSettingsPanel( context ) );
				settingsDialog.setModal( true );
				settingsDialog.pack();
				settingsDialog.setVisible( true );
			}else{
				JOptionPane.showMessageDialog( content, "Cannot open the settings page!", 
					"Error", JOptionPane.ERROR_MESSAGE );
			}			
		}else if( ae.getSource() instanceof JComboBox ){
			if( optMethodSelBox.equals( (JComboBox)ae.getSource() ) ){
				String selectedMethod = optMethodSelBox.getSelectedItem().toString();
				if( ( optMethod == null || !selectedMethod.equals( optMethod.getLocalizedName() ) )
						&& optMethodSelBox.getSelectedIndex() != 0 ){
					for( int i = 0; i < optimizationPlugins.size(); ++i ){
						if( optimizationPlugins.get( i ).getLocalizedName().equals( selectedMethod ) ){
							optMethod = optimizationPlugins.get( i );
							break;
						}
					}
					optMethod.invalidatePlugin();
					optMethod.setParameterTreeRoot( paramRoot );
					optMethod.setRecordableVariables( recorderTree );
					optMethodInfoLabel.setText( optMethod.getDescription() );
				}
			}
		}
	}

	public boolean alterParameterTree(IIntelliContext ctx) {
		optMethod.alterParameterTree( ctx );
		return true;
	}

	public int getCurrentIteration() {
		return optMethod.getCurrentIteration();
	}

	public ParameterTree getNextParameterTree(IParameterSweepResultReader reader) {
		return optMethod.getNextParameterTree( reader );
	}

	public int getNumberOfIterations() {
		return optMethod.getNumberOfIterations();
	}

	public JPanel getSettingsPanel(IIntelliContext ctx) {
		if( ctx != null ){
			context = ctx;
			scriptSupport = (IScriptSupport)ctx.get( "scriptSupport" );
			newParametersButtonCopy = ctx.getNewParametersButton();
			GUIUtils.addActionListener(this, newParametersButtonCopyHere);
		}
		makeSettingsPanel();
		return content;
	}

	public boolean noMoreIteration() {
		Number optimalValue = (Number)optMethod.getOptimalValue();
		boolean hypothesisFalsified = 
			optimalValue.doubleValue() > objectValue - perturbation &&
			optimalValue.doubleValue() < objectValue + perturbation;
		
		return hypothesisFalsified || optMethod.noMoreIteration();
	}

	public void setParameterTreeRoot(DefaultMutableTreeNode root) {
		paramRoot = root;
		if( optMethod != null ){
			optMethod.setParameterTreeRoot( root );
		}
	}

	public void setRecordableVariables(DefaultMutableTreeNode root) {
		recorderTree = root;
		if( optMethod != null ){
			optMethod.setRecordableVariables( root );
			optMethod.getSettingsPanel( context ).revalidate();
		}
		
		ArrayList<RecordableInfo> newList = new ArrayList<RecordableInfo>();
		//ONLY THE FIRST RECORDER IS USED
		if( root.getChildCount() > 0 ){
			DefaultMutableTreeNode recorder = (DefaultMutableTreeNode) root.getChildAt( 0 );
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
		
		recList = newList;
	}

	public String settingsOK(DefaultMutableTreeNode recorders) {
		String error = null;
		
		checkANTsParameters();
		
		if( optMethod == null ) return NO_OPT_MSG;
		error = optMethod.settingsOK( recorders );
		if( error != null ){
			error = optMethod.getLocalizedName() + ": " + error;
		}
		return error;
	}

	public String getDescription() {
		return "The Active Nonlinear Tests allow testing of nonlinear computational models. " +
				"The method evaluates the model applying an optimization method in " +
				"order to falsify a particular conclusion about the model that " +
				"is formulated by an object function and the allowed perturbation.\n\n" +
				"The method was published by John H. Miller (in Management Science, " +
				"vol. 44. No. 6 pp. 820-830., June 1998).";
	}

	public int getMethodType() {
		return DYNAMIC_METHOD;
	}

	public boolean getReadyStatus() {
		if( null != checkANTsParameters() ) return false;
		
		if( null != optMethod.settingsOK( recorderTree ) ) return false;
		
		setOptMethodObjectFunction();
		
		return true;
	}

	public String getReadyStatusDetail() {
		return settingsOK( recorderTree );
	}

	public void invalidatePlugin() {
		content = null;
	}

	public boolean isImplemented() {
		return true;
	}

	public void load(IIntelliContext context, Element element)
			throws WizardLoadingException {
		this.context = context;
		
		// set ANTs parameters
		NodeList nl = element.getElementsByTagName( "object_value" );
		if( nl != null && nl.getLength() != 0 ){
			objectValue = 
				Double.valueOf( ((Text)((Element)nl.item( 0 )).getChildNodes().item( 0 )).getNodeValue() );
			objValueField.setText( objectValue + "" );
		}
		
		nl = element.getElementsByTagName( "perturbation" );
		if( nl != null && nl.getLength() != 0 ){
			perturbation = 
				Double.valueOf( ((Text)((Element)nl.item( 0 )).getChildNodes().item( 0 )).getNodeValue() );
			perturbationField.setText( perturbation + "" );
		}
		
		// set optimization method and delegate load
		optMethodSelBox = new JComboBox();
		optMethodSelBox.addItem( "Please select..." );
		for( int i = 0; optimizationPlugins != null && i < optimizationPlugins.size(); ++i ){
			optMethodSelBox.addItem( optimizationPlugins.get( i ).getLocalizedName() );
		}
		
		nl = element.getElementsByTagName( "opt_alg" );
		if( nl != null && nl.getLength() != 0 ){
			Element methodElem = (Element)nl.item( 0 );
			String methodName = methodElem.getAttribute( "name" );
			for( int i = 0; methodName != null && i < optimizationPlugins.size(); ++i ){
				if( methodName.equals( optimizationPlugins.get( i ).getLocalizedName() ) ){
					optMethod = optimizationPlugins.get( i );
					optMethodSelBox.setSelectedIndex( i + 1 );
					break;
				}
			}
			if( optMethod != null ){
				optMethod.load( context, methodElem );
				if( recorderTree != null ){
					optMethod.setRecordableVariables( recorderTree );
				}
				
				objectFunctionName = optMethod.getFitnessFunction().getName();
			}
		}
	}

	public void save(Node node) {
		Document doc = node.getOwnerDocument();
		
		Element pluginElement = (Element) node;
		pluginElement.setAttribute("class",this.getClass().getName());
		
		Element antsElement = doc.createElement( "ANTs_settings" );
		node.appendChild( antsElement );
		
		objectFunctionName = optMethod.getFitnessFunction().getName();
		Element objFuncElem = doc.createElement( "object_function" );
		objFuncElem.appendChild( doc.createTextNode( objectFunctionName ) );
		antsElement.appendChild( objFuncElem );
		Element objValueElem = doc.createElement( "object_value" );
		objValueElem.appendChild( doc.createTextNode( String.valueOf( objectValue ) ) );
		antsElement.appendChild( objValueElem );
		Element perturbationElem = doc.createElement( "perturbation" );
		perturbationElem.appendChild( doc.createTextNode( String.valueOf( perturbation ) ) );
		antsElement.appendChild( perturbationElem );
		
		Element optAlgElement = doc.createElement( "opt_alg" );
		antsElement.appendChild( optAlgElement );
		optAlgElement.setAttribute("name",optMethod.getLocalizedName());
		optMethod.save( optAlgElement );
	}

	public String getLocalizedName() {
		return "Active Nonlinear Tests";
	}
	
	//=========================================================================
	// Private & protected functions
	/**
	 * Creates the plugin's settings panel.
	 */
	protected void makeSettingsPanel(){
		if( content == null ){
			initObjectFSelControls();
			initOptMethodControls();
			
			JPanel optMethodInfoPanel = new JPanel();
			optMethodInfoPanel.setBorder( BorderFactory.createTitledBorder( "Otimization method" ) );
			optMethodInfoPanel.setLayout( new GridLayout( 1, 1 ) );
			optMethodInfoPanel.add( optMethodInfoLabel );
			
			content = FormsUtils.build(	"p ~ p ~ p ~ f:p:g",
					"0123 p ||" +
					"45__ p ||" +
					"67__ p ||" +
					"89A_ p ||" +
					"BBBB p",
					new JLabel( "Object function:" ), objectFSelBox, 
					defObjectFButton, objectFInfoLabel,
					new JLabel( "Object value:" ), objValueField,
					new JLabel( "Perturbation:" ), perturbationField,
					new JLabel( "Optimization method:" ), optMethodSelBox,
					setOptMethodButton, optMethodInfoPanel ).getPanel();
		}
	}

	protected void initObjectFSelControls(){
		objectFSelBox = new JComboBox();
		objectFSelBox.addItem( "Please select..." );
		objectFSelBox.removeAllItems();
		for (RecordableInfo elem : recList) {
			objectFSelBox.addItem( elem );
        }
		
		if( objectFunctionName == null )
			objectFSelBox.setSelectedIndex(0);
		else
			objectFSelBox.setSelectedItem( objectFunctionName );
		objectFSelBox.setEnabled( false );
		
		defObjectFButton = new JButton( "Define function" );
		defObjectFButton.setActionCommand( "define object function" );
		defObjectFButton.addActionListener( this );
		
		objectFInfoLabel = new JLabel( "Select function on " +
				"the optimization method's settings page." );
		
		objValueField.setColumns( 20 );
		objValueField.setText( "0.0" );
		objValueField.setHorizontalAlignment( JTextField.RIGHT );
		
		perturbationField.setColumns( 20 );
		perturbationField.setText( "0.0" );
		perturbationField.setHorizontalAlignment( JTextField.RIGHT );
	}
	
	protected void initOptMethodControls(){
		optMethodSelBox = new JComboBox();
		optMethodSelBox.addItem( "Please select..." );
		String methodName = optMethod == null ? null : optMethod.getLocalizedName();
		for( int i = 0; optimizationPlugins != null && i < optimizationPlugins.size(); ++i ){
			String actMethodName = optimizationPlugins.get( i ).getLocalizedName();
			optMethodSelBox.addItem( actMethodName );
			if( methodName != null && methodName.equals( actMethodName ) )
				optMethodSelBox.setSelectedIndex( i + 1 );
		}
		optMethodSelBox.addActionListener( this );
		
		setOptMethodButton = new JButton( "Settings" );
		setOptMethodButton.setActionCommand( "optimalization method settings" );
		setOptMethodButton.addActionListener( this );
	}
	
	protected String checkANTsParameters(){
		try{
			objectValue = Double.valueOf( objValueField.getText() );
		}catch( NumberFormatException e ){
			return WRONG_OBJ_VAL_MSG;
		}
		
		try{
			perturbation = Double.valueOf( perturbationField.getText() );
		}catch( NumberFormatException e ){
			return WRONG_PERT_VAL_MSG;
		}
		
		return null;
	}
	
	protected void setOptMethodObjectFunction(){
		objectFunction = generateObjectFunction();
		
		//get first recorder
		DefaultMutableTreeNode recorder = (DefaultMutableTreeNode) recorderTree.getChildAt( 0 );
		recorder.add( new DefaultMutableTreeNode(new RecordableElement( objectFunction )) );
		
		RecordableInfo recInfo = 
			new RecordableInfo(objectFunction.getName(), objectFunction.getJavaType(), objectFunction.getName());
		optMethod.setFitnessFunction( recInfo );
	}
	
	protected ScriptGeneratedMemberInfo generateObjectFunction(){
		RecordableInfo info = optMethod.getFitnessFunction();
		if( info != null ){
			ScriptGeneratedMemberInfo objFunc = 
				new ScriptGeneratedMemberInfo( "antsObjectFunction()", "double", Double.TYPE );
			String script = "return java.lang.Math.abs( ((double)" + info.getAccessibleName() +
							") - " + objectValue + " );";
			objFunc.setSource( script );
			
			List<String> errors = new ArrayList<String>(1); 
			objFunc = checkScript(objFunc,errors); 
			return objFunc;
		}
		
		return null;
	}
	
	protected ScriptGeneratedMemberInfo checkScript(ScriptGeneratedMemberInfo info, List<String> errors) { 
		ParameterSweepWizard wizard = scriptSupport.getWizard();
		
		if (PlatformSettings.getPlatformType() == PlatformType.REPAST || PlatformSettings.getPlatformType() == PlatformType.CUSTOM
				|| PlatformSettings.getPlatformType() == PlatformType.SIMPHONY2) {
			File f = new File(wizard.getModelFileName());
			CtClass	clazz = null;
			try {
				InputStream ins = new FileInputStream(f);
				clazz = wizard.getClassPool().makeClass(ins);
				clazz.stopPruning(true);
				ins.close();
			} catch (IOException e) {
				e.printStackTrace(ParameterSweepWizard.getLogStream());
			} finally {
				if (clazz != null)
					clazz.defrost();
			}
			List<GeneratedMemberInfo> generateds = new ArrayList<GeneratedMemberInfo>();
			for (MemberInfo mi : scriptSupport.getAllMembers()) {
				if (mi instanceof GeneratedMemberInfo)
					generateds.add((GeneratedMemberInfo)mi);
			}
			ScriptParser parser = new ScriptParser(wizard.getClassPool(),clazz,wizard.getClassLoader(),generateds,wizard.getNewParameters_internal());
			return parser.checkScript(info,errors);
		} else {
			Platform platform = PlatformManager.getPlatform(PlatformSettings.getPlatformType());
			List<GeneratedRecordableInfo> others = new ArrayList<GeneratedRecordableInfo>();
			for (MemberInfo mi : scriptSupport.getAllMembers()) {
				if (mi instanceof GeneratedMemberInfo) {
					GeneratedMemberInfo _mi = (GeneratedMemberInfo) mi;
					if (info.getSource().contains(_mi.getName())) 
						info.addReference(_mi);
					if (platform instanceof IScriptChecker)
						others.add((GeneratedRecordableInfo)InfoConverter.convertRecordableElement2RecordableInfo(new RecordableElement(_mi,null)));
				}
			}
			if (platform instanceof IScriptChecker) {
				IScriptChecker _platform = (IScriptChecker) platform;
				final ScriptGeneratedRecordableInfo _info = (ScriptGeneratedRecordableInfo) 
															InfoConverter.convertRecordableElement2RecordableInfo(new RecordableElement(info,null));
				errors.addAll(_platform.checkScript(_info,others,wizard));
			}
			if (errors.size() > 0) return null;
			return info;
		}
	}
}
