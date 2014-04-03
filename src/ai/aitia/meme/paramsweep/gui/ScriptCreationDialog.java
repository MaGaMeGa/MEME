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
package ai.aitia.meme.paramsweep.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.CtClass;
import javassist.Loader;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import kepletszerkeszto.JDialogKeret;
import _.unknown;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.classloader.RetryLoader;
import ai.aitia.meme.paramsweep.colt.DoubleArrayList;
import ai.aitia.meme.paramsweep.colt.SortedDoubleArrayList;
import ai.aitia.meme.paramsweep.generator.IStatisticInfoGenerator;
import ai.aitia.meme.paramsweep.generator.OperatorsInfoGenerator;
import ai.aitia.meme.paramsweep.generator.StatisticsInfoGenerator;
import ai.aitia.meme.paramsweep.gui.info.GeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.InnerOperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MultiColumnOperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.OperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.RecordableElement;
import ai.aitia.meme.paramsweep.gui.info.ScriptGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.SimpleGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.VariableScriptGeneratedMemberInfo;
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
import ai.aitia.meme.paramsweep.plugin.IMultiColumnOperatorPlugin;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin;
import ai.aitia.meme.paramsweep.plugin.IStatisticsPlugin;
import ai.aitia.meme.paramsweep.plugin.gui.AnyTypeConstructOperatorGUI;
import ai.aitia.meme.paramsweep.plugin.gui.BinaryListConstructOperatorGUI;
import ai.aitia.meme.paramsweep.plugin.gui.CollectionParameterGUI;
import ai.aitia.meme.paramsweep.plugin.gui.ElementSelectionOperatorGUI;
import ai.aitia.meme.paramsweep.plugin.gui.FilterOperatorGUI;
import ai.aitia.meme.paramsweep.plugin.gui.ForEachOperatorGUI;
import ai.aitia.meme.paramsweep.plugin.gui.IMultiColumnOperatorGUI;
import ai.aitia.meme.paramsweep.plugin.gui.IOperatorGUI;
import ai.aitia.meme.paramsweep.plugin.gui.IParameterGUI;
import ai.aitia.meme.paramsweep.plugin.gui.ListOperatorGUI;
import ai.aitia.meme.paramsweep.plugin.gui.ListUnionIntersectionGUI;
import ai.aitia.meme.paramsweep.plugin.gui.MemberSelectionOperatorGUI;
import ai.aitia.meme.paramsweep.plugin.gui.MultiColumnRecordableOperatorGUI;
import ai.aitia.meme.paramsweep.plugin.gui.NumberParameterGUI;
import ai.aitia.meme.paramsweep.plugin.gui.RemoveOperatorGUI;
import ai.aitia.meme.paramsweep.plugin.gui.AnyTypeConstructOperatorGUI.UseType;
import ai.aitia.meme.paramsweep.plugin.gui.ElementSelectionOperatorGUI.SelectionType;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;
import ai.aitia.meme.paramsweep.utils.ReturnTypeElement;
import ai.aitia.meme.paramsweep.utils.SeparatedList;
import ai.aitia.meme.paramsweep.utils.SortedListModel;
import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.pluginmanager.PluginInfo;
import ai.aitia.meme.pluginmanager.PSPluginManager.PluginList;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.Utils.Pair;
import bemenetek.Fugg;
import bemenetek.Valt;

/** This class provides the graphical user interface where the users can define
 *  statistic instances or scripts.
 */
public class ScriptCreationDialog extends JDialog implements ActionListener,
															 ListSelectionListener,
															 ChangeListener {
	
	//=====================================================================================
	// members
	
	private static final long serialVersionUID = 1L;

	/** The duration of displaying warning messages. */
	private static final int PROBLEM_TIMEOUT = 5 * 1000;
	
	/** The commentary message for defining statistic instances. */
	private static final String STATISTICS_TEXT = "Please select a statistics from the list on the left side and define its actual parameters."; 
	/** The commentary message for defining scripts. */
	private static final String SCRIPTS_TEXT = "Please define a name, a return type and a body to creating a script. In the body please" +
											   " use fully qualified names or define an import declaration by pressing 'Add import...' button.";
	private static final String SCRIPTS_TEXT_NETLOGO = "Please define a name, a return type and a body to creating a script.";
	
	/** The prefix of the default names of scripts. */
	private static final String DEFAULT_NAME_PREFIX = "script_";
	
	/** Message type constant: represents normal messages. */
	public static final int MESSAGE	= 0;
	/** Message type constant: represents warning messages. */
	public static final int WARNING	= 1;
	
	/** Class objects of numeric types. */
	private static List<Class> numericTypes = new ArrayList<Class>(12);
	/** Class objects of numeric collection types. */
	private static List<Class> numericCollectionTypes = new ArrayList<Class>(16);
	/** List of the default import declarations. */
	private static List<String> defaultImports = new ArrayList<String>(3);
	
	static {
		Class[] classes = { Byte.TYPE, Byte.class, Short.TYPE, Short.class, Integer.TYPE, Integer.class, Long.TYPE, Long.class, Float.TYPE,
							Float.class, Double.TYPE, Double.class
						  };
		numericTypes.addAll(Arrays.asList(classes));
		
		classes = new Class[] { byte[].class, Byte[].class, short[].class, Short[].class, int[].class, Integer[].class, long[].class, Long[].class,
								float[].class, Float[].class, double[].class, Double[].class, DoubleArrayList.class, SortedDoubleArrayList.class,
								List.class, cern.colt.list.DoubleArrayList.class
							  };
		numericCollectionTypes.addAll(Arrays.asList(classes));
		
		String[] imports = { "java.util.*", "uchicago.src.sim.engine.*", "uchicago.src.sim.analysis.*" };
		defaultImports.addAll(Arrays.asList(imports));
	}
	
	/** The current commentary message. */
	private String infoText = null;
	/** A pair that contains a message for the user. The Integer part identifes the type
	 *  of the message.
	 */ 
	private Pair<String,Integer> warning = null;
	/** Timer. */
	private Utils.TimerHandle warningTimer = null;
	/** The list of information objects of all members of the model that can be
	 *  used in a statistic instance or a script. 
	 */
	
	private IScriptSupport scriptSupport = null;
	/** The list model of information objects of the generated statistic instances and scripts. */ 
	private DefaultListModel scriptListModel = null;
	private DefaultListModel nonRecordableScriptListModel = null;
	/** The list of GUI components of the current statistic instance. */
	private List<IParameterGUI> parameterGUIList = null;
	private IOperatorGUI operatorGUI = null;
	/** The regular expression of import declarations. */
	private Pattern importPattern = null;
	/** The wizard. */
	private ParameterSweepWizard wizard = null;
	private boolean editMode = false;
	private boolean forceCloseAfterCreate = false;
	
	//=====================================================================================
	// GUI members
	
	private JPanel content = new JPanel(new BorderLayout());
	private JTextPane infoTextPane = new JTextPane();
	private JScrollPane infoTextPaneScr = new JScrollPane(infoTextPane);
	private JTabbedPane tabbed = new JTabbedPane();
	
	// I. First tab : statistics
	private JSplitPane I_split = new JSplitPane();
	private JPanel I_left = new JPanel(new BorderLayout());
	private JList I_availableStats = new JList();
	private JScrollPane I_availableStatsScr = new JScrollPane(I_availableStats);
	private JList I_availableOperators = new JList();
	private JScrollPane I_availableOperatorsScr = new JScrollPane(I_availableOperators);
	private JPanel I_right = new JPanel(new BorderLayout());
	private JTextPane I_descriptionPane = new JTextPane();
	private JScrollPane I_descriptionPaneScr = new JScrollPane(I_descriptionPane);
	private JScrollPane I_generatedGUIScr = new JScrollPane();
	private JTextField I_nameField = new JTextField(); 
	private JPanel I_buttons = new JPanel();
	private JButton I_createButton = new JButton("Create");
	private JButton I_closeButton = new JButton("Close");
	
	// II. Second tab: : scripts
	private JPanel II_panel = null;
	private JComboBox II_returnType = new JComboBox();
	private JTextField II_nameField = new JTextField();
	private JPanel II_importPanel = null;
	private JList II_importList = new JList();
	private JScrollPane II_importListScr = new JScrollPane(II_importList);
	private JButton II_addButton = new JButton("Add import...");
	private JButton II_removeButton = new JButton("Remove import");
	private JTextArea II_bodyArea = new JTextArea();
	private JScrollPane II_bodyAreaScr = new JScrollPane(II_bodyArea);
	private JButton II_createButton = new JButton("Create");
	private JButton II_closeButton = new JButton("Close");
	private JButton II_startAssistantButton = new JButton("Start script assistant");
	
	private JPanel II_variablesPanel = null;
	private JList II_variablesList = new JList();
	private JScrollPane II_variablesListScr = new JScrollPane(II_variablesList);
	private JButton II_insertVariableButton = new JButton("Insert");
	private JButton II_addVariableButton = new JButton("Add");
	private JButton II_editVariableButton = new JButton("Edit");
	private JButton II_removeVariableButton = new JButton("Remove");

	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	public ScriptCreationDialog(Frame owner, ParameterSweepWizard wizard, DefaultListModel scriptListModel,
								DefaultListModel nonRecordableScriptListModel, IScriptSupport scriptSupport) {
		super(owner,"Create data source",true);
		this.setName("dial_datasource");
		this.wizard = wizard;
		this.scriptListModel = scriptListModel;
		this.nonRecordableScriptListModel = nonRecordableScriptListModel;
		this.scriptSupport = scriptSupport;
		layoutGUI();
		initialize();
		initializeAvailableStatisticsList();
		initializeAvailableOperatorsList();
		initializeImportList();
		this.setLocationRelativeTo(owner);
	}
	
	//------------------------------------------------------------------------------------
	/** Shows the dialog. */
	public void showDialog() {
		setVisible(true);
		onClose();
		dispose();
	}
	
	//------------------------------------------------------------------------------------
	/** Shows the dialog with the Multi Column Recordable operator selected, and disables other settings.  */
	public void showMultiColumnRecordable(MemberInfo selectedCollection, String aliasName) {
		ListModel operators = I_availableOperators.getModel();
		for (int i = 0; i < operators.getSize(); i++) {
			PluginInfo<IOperatorPlugin> opPlugin = (PluginInfo<IOperatorPlugin>) operators.getElementAt(i);
			if (opPlugin.getInstance().getName().equals("multiColumnRecordable")) {
				I_availableOperators.setSelectedIndex(i);
				break;
			}
		}
		if (aliasName != null && aliasName.length() > 0) {
			I_nameField.setText(aliasName);
		} else { 
			I_nameField.setText(selectedCollection.getName().replace("()", "")+"Multi");
		}
		tabbed.setSelectedIndex(0);
		tabbed.setEnabled(false);
		I_availableOperators.setEnabled(false);
		I_availableStats.setEnabled(false);
		if (operatorGUI instanceof IMultiColumnOperatorGUI) {
			((IMultiColumnOperatorGUI)operatorGUI).setSelectedCollection(selectedCollection);
		}
		forceCloseAfterCreate = true;
		I_createButton.setText("OK");
		I_closeButton.setText("Cancel");
		setVisible(true);
		tabbed.setEnabled(true);
		forceCloseAfterCreate = false;
		I_createButton.setText("Create");
		if (ParameterSweepWizard.getPreferences().closeAfterOneScript()) {
			I_closeButton.setText("Cancel");
		} else {
			I_closeButton.setText("Close");
		}
		onClose();
		dispose();
	}
	
	//----------------------------------------------------------------------------------------------------
	public void showEditDialog(GeneratedMemberInfo edited) throws CannotLoadDataSourceForEditingException {
		editScript(edited);
		showDialog();
	}
	
	//------------------------------------------------------------------------------------
	/** Returns the actual content of the information panel. This can be the commentary message
	 *  of the current page or a warning/message to the user.
	 */
	public String getInfoText()	{
		String s;
		if (warning != null) {
			s = warning.getSecond().intValue() == WARNING ? "<img src=\"gui/icons/warning.png\">&nbsp;&nbsp;" : ""; 
			s += Utils.htmlQuote(warning.getFirst());
		} else
			s = infoText;
		return s == null ? null : Utils.htmlPage(s);
	}
	
	//------------------------------------------------------------------------------------
	/** Returns the available width for the GUI components of a statistic. */
	public int getAvailableWidth() { return I_generatedGUIScr.getWidth() - 10; }
	public void setInfoText(String infoText) { this.infoText = infoText; }
	public ClassLoader getClassLoader() { return wizard.getClassLoader(); }
	
	//------------------------------------------------------------------------------------
	/** Displays the <code>message</code> if <code>condition</code> is true.
	 * @param level the type of the message
	 * @param clear flag that determines whether clears the message from the information
	 *  panel after a duration of time or not
	 * @return condition
	 */
	public boolean warning(boolean condition, String message, int level, boolean clear) {
		String before = warning == null ? null : warning.getFirst();
		warning = condition ?  new Pair<String,Integer>(message,level) : null;
		if (warning != null && !Utils.equals(warning.getFirst(),before)) {
			updateInfo();
			if (warningTimer != null)
					warningTimer.stop();
			if (warning != null && clear)
					warningTimer = Utils.invokeAfter(PROBLEM_TIMEOUT,new Runnable() {
						public void run() { clearProblemText(); }
 					});
		}
		return condition;
	}
	
	//=====================================================================================
	// implemented interfaces

	//------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("CREATE")) {
			String name = I_nameField.getText().trim();
			// I. name is empty
			if (warning(name.equals(""),"Name field is empty.",WARNING,true)) return;
			// II. spaces
			if (warning(name.contains(" "),"Invalid identifier: the name cannot contain spaces",WARNING,true)) return;
			// III. valid name
			if (warning(!scriptSupport.isValidScriptName(name),"Invalid identifier: the name in not a valid identifier on the platform",WARNING,true))
				return;
			// IV. name is used
			if (!editMode && warning(!isUniqueName(name),
				String.format("Name '%s' is already used in the model",name),WARNING,true))
				return;
			if (I_availableStats.getSelectedIndex() >= 0) { // statistic
				PluginInfo<IStatisticsPlugin> statInfo = (PluginInfo<IStatisticsPlugin>) I_availableStats.getSelectedValue();
				IStatisticsPlugin current = statInfo.getInstance();
				List<Object> actParams = new ArrayList<Object>(current.getNumberOfParameters());
				for (IParameterGUI pGUI : parameterGUIList) {
					String error = pGUI.checkInput();
					if (warning(error != null,"Error at parameter " + pGUI.getParameterName() + ": " + error,WARNING,true))
						return;
					actParams.add(pGUI.getInput());
				}
				IStatisticInfoGenerator infoGenerator = scriptSupport.getStatisticInfoGenerator(current);
				SimpleGeneratedMemberInfo newInfo = infoGenerator.generateInfoObject(name,actParams.toArray());
				List<String> errors = infoGenerator.getError();
				if (errors != null && errors.size() > 0) {
					String str = Utils.join(errors,"\n");
					str = "Errors at creation time:\n " + str;
					warning(true,str,WARNING,true);
					return;
				}
				newInfo.clearBuildingBlocks();
				newInfo.setGeneratorName(statInfo.getInternalName());
				for (IParameterGUI pGUI : parameterGUIList) {
					Object input = pGUI.getInput();
					if (input instanceof MemberInfo) {
						List<MemberInfo> l = new ArrayList<MemberInfo>(1);
						l.add((MemberInfo)input);
						newInfo.addBuildingBlock(l);
					} else 
						newInfo.addBuildingBlock((List)input);
					pGUI.postCreation();
				}
				deletePreviousInstance(newInfo);
				scriptSupport.getAllMembers().add(newInfo);
				Fugg.f.setFuggveny(newInfo.getName() + " : " + newInfo.getType(),"");
				String alias = newInfo.getName().endsWith("()") ? newInfo.getName().substring(0,newInfo.getName().length() - 2) : newInfo.getName();
				RecordableElement newElement = new RecordableElement(newInfo,alias); 
				scriptListModel.addElement(newElement);
				if (ParameterSweepWizard.getPreferences().closeAfterOneScript() || forceCloseAfterCreate)
					setVisible(false);
				else {
					editMode = false;
					warning(true,newInfo.getName() + " created.",MESSAGE,true);
					I_availableStats.clearSelection();
					I_nameField.setText("");
				}
			} else if (I_availableOperators.getSelectedIndex() >= 0) { // operators 
				PluginInfo<IOperatorPlugin> operatorInfo = (PluginInfo<IOperatorPlugin>) I_availableOperators.getSelectedValue();
				IOperatorPlugin current = operatorInfo.getInstance();
				String error = operatorGUI.checkInput();
				if (warning(error != null,"Error at operator " + current.getLocalizedName() + ": " + error,WARNING,true))
					return;
				OperatorsInfoGenerator infoGenerator = new OperatorsInfoGenerator(current);
				OperatorGeneratedMemberInfo newInfo = infoGenerator.generateInfoObject(name,operatorGUI.getInput());
				List<String> errors = infoGenerator.getError();
				if (errors != null && errors.size() > 0) {
					String str = Utils.join(errors,"\n");
					str = "Errors at creation time:\n " + str;
					warning(true,str,WARNING,true);
					return;
				}
				newInfo.setGeneratorName(operatorInfo.getInternalName());
				newInfo.setBuildingBlock(Arrays.asList(operatorGUI.getInput()));
				deletePreviousInstance(newInfo);
				scriptSupport.getAllMembers().add(newInfo);
				Fugg.f.setFuggveny(newInfo.getName() + " : " + newInfo.getType(),"");
				String alias = (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? null : 
							   newInfo.getName().endsWith("()") ? newInfo.getName().substring(0,newInfo.getName().length() - 2) : newInfo.getName();
				if (current instanceof IMultiColumnOperatorPlugin) {
					// this is coming from a spinner, it cannot throw a NumberFormatException
					/*int numColumns = Integer.valueOf(((IMultiColumnOperatorPlugin)current).getNumberOfColumns(operatorGUI.getInput())).intValue();
					StringBuilder sbNewAlias = new StringBuilder(300);
					for (int i = 0; i < numColumns-1; i++) {
						sbNewAlias.append(alias);
						sbNewAlias.append("_");
						sbNewAlias.append(i);
						sbNewAlias.append("\"");
						sbNewAlias.append(SeparatedList.SEPARATOR);
						sbNewAlias.append("\"");
					}
					sbNewAlias.append(alias);
					sbNewAlias.append("_");
					sbNewAlias.append(numColumns-1);
					alias = sbNewAlias.toString();
				}
				RecordableElement newElement = new RecordableElement(newInfo,alias); */
				Object[] params = new Object[6];
				params = ((IOperatorGUI)operatorGUI).getInput();
				
				MultiColumnOperatorGeneratedMemberInfo multiColOperatorInfo = (MultiColumnOperatorGeneratedMemberInfo)new OperatorsInfoGenerator(current).generateInfoObject((String)params[5], params); 
				multiColOperatorInfo.setGeneratorName("ai.aitia.meme.paramsweep.operatorPlugin.Operator_MultiColumnRecordable");
				RecordableElement newElement = new RecordableElement(multiColOperatorInfo, (String)params[5]);
				
				if (current.isRecordable(operatorGUI.getInput()))
					scriptListModel.addElement(newElement);
				else
					nonRecordableScriptListModel.addElement(newElement);
				if (ParameterSweepWizard.getPreferences().closeAfterOneScript() || forceCloseAfterCreate)
					setVisible(false);
				else {
					editMode = false;
					warning(true,newInfo.getDisplayName() + " created.",MESSAGE,true);
					I_availableOperators.clearSelection();
					I_nameField.setText("");
				}
			}
		}
		}
		else if (command.equals("CLOSE")) 
			setVisible(false);
		else if (command.equals("ADD_IMPORT")) {
			String importDecl = (String)JOptionPane.showInputDialog(ParameterSweepWizard.getFrame(),"Please define an import declaration (e.g. " +
																	"java.util.List\n or java.util.*).\n\n","Import declaration",
																	JOptionPane.PLAIN_MESSAGE,null,null,null);
			if (importDecl != null && !importDecl.trim().equals("") && isValidImportDeclaration(importDecl.trim())) {
				importDecl = importDecl.trim();
				SortedListModel model = (SortedListModel) II_importList.getModel();
				if (!model.contains(importDecl)) {
					model.addElement(importDecl);
					model.sort();
				}
			} else 
				warning(true,"Invalid import declaration" +	(importDecl == null || importDecl.trim().equals("") ? "" : ": " + importDecl),WARNING,
						true);
		} else if (command.equals("REMOVE_IMPORT")) {
			Object[] selected = II_importList.getSelectedValues();
			SortedListModel model = (SortedListModel) II_importList.getModel();
			for (Object o : selected) {
				if (!defaultImports.contains(o))
					model.removeElement(o);
			}
			II_importList.clearSelection();
		} else if (command.equals("RETURN_TYPE") || e.getSource().equals(II_returnType)) { 
			Object obj = II_returnType.getSelectedItem();
			if (obj instanceof String) {
				String text = (String) obj;
				if (!warning(text.trim().equals("") || text.trim().equals("void"),"Invalid return type",ScriptCreationDialog.WARNING,true)) {
					DefaultComboBoxModel model = (DefaultComboBoxModel) II_returnType.getModel();
					Class<?> javaType = getJavaType(text);
					if (warning(javaType == null,"Unknown type. Please use the fully qualified name or import declaration.",WARNING,true))
						return;
					ReturnTypeElement re = new ReturnTypeElement(text,javaType);
					if (model.getIndexOf(re) == -1) 
						model.addElement(re);
					II_returnType.setSelectedItem(re);
				} else if (II_returnType.getItemCount() > 0)
					II_returnType.setSelectedIndex(0);
				else
					II_returnType.setSelectedIndex(-1);
			} else {
				ReturnTypeElement re = (ReturnTypeElement) obj;
				replaceReturnValue(re.getDefaultValue());
			}
		} else if (command.equals("II_CREATE")) { 
			String name = II_nameField.getText().trim();
			if (name.endsWith(")")) {
				int index = name.lastIndexOf('(');
				name = name.substring(0,index);
			}
			// I. name is empty
			if (warning(II_nameField.getText() == null || name.equals(""),"Script name is empty.",WARNING,true))
				return;
			// II. spaces
			if (warning(name.contains(" "),"Invalid identifier: the script name cannot contain spaces",WARNING,true))
				return;
			// III. valid name
			if (warning(!scriptSupport.isValidScriptName(name),"Invalid identifier: the name in not a valid identifier on the platform",WARNING,true))
				return;
			// IV. body is empty
			if (warning(II_bodyArea.getText() == null || II_bodyArea.getText().trim().equals(""),"Script body is empty.",WARNING,true))
				return;
			// V. name is used
			if (!editMode && warning(!isUniqueName(name),String.format("Script name '%s' is already used in the model",II_nameField.getText().trim()),
				WARNING,true))
				return;
			// VI. body doesn't contain return statement 
			String RETURN = PlatformSettings.getPlatformType() == PlatformType.NETLOGO ? "report" : "return"; 
			if (warning(!II_bodyArea.getText().toLowerCase().contains(RETURN + " "),"Script body does not contain '" + RETURN + "' statement",WARNING,true))
				return;
			String _name = name + (PlatformSettings.getPlatformType() == PlatformType.NETLOGO ? "" : "()");
			Class<?> returnType = ((ReturnTypeElement)II_returnType.getSelectedItem()).getJavaType();
			ScriptGeneratedMemberInfo sgmi = new ScriptGeneratedMemberInfo(_name,returnType.getSimpleName(),returnType);
			sgmi.setSource(II_bodyArea.getText());
			if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO) {
				SortedListModel model = (SortedListModel) II_importList.getModel();
				for (int i = 0;i < model.size();++i)
					sgmi.addImport(model.get(i).toString());
			}
			List<String> errors = new ArrayList<String>(1); 
			sgmi = checkScript(sgmi,errors); 
			if (sgmi == null && errors.size() > 0) {
				String str = Utils.join(errors,"\n");
				str = "Syntax error: " + str;
				warning(true,str,WARNING,false);
				return;
			}
			deletePreviousInstance(sgmi);
			scriptSupport.getAllMembers().add(sgmi);
			Fugg.f.setFuggveny(sgmi.getName() + " : " + sgmi.getType(),"");
			RecordableElement element = new RecordableElement(sgmi,name);
			if (isRecordableScript((ReturnTypeElement)II_returnType.getSelectedItem()))
				scriptListModel.addElement(element);
			else
				nonRecordableScriptListModel.addElement(element);
			if (ParameterSweepWizard.getPreferences().closeAfterOneScript() || forceCloseAfterCreate)
				setVisible(false);
			else {
				editMode = false;
				warning(true,sgmi.getName() + " created.",MESSAGE,true);
				resetScriptTab();
			}
		
		} else if (command.equals("START")) {
			Fugg.f.javaFugg = PlatformSettings.getPlatformType() != PlatformType.NETLOGO;
			JDialogKeret jdk = new JDialogKeret(this);
			String text = jdk.getSource();
			if (!"".equals(text))
				II_bodyArea.setText(text);
			jdk.dispose();
		} else if ("INSERT_VARIABLE".equals(command)) {
			final int idx = II_bodyArea.getCaretPosition();
			final String varName = ((UserDefinedVariable)II_variablesList.getSelectedValue()).getName() + " ";
			II_bodyArea.insert(varName,idx);
		} else if ("ADD_VARIABLE".equals(command)) {
			final List<String> imports = new ArrayList<String>();
			if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO) {
				SortedListModel model = (SortedListModel) II_importList.getModel();
				for (int i = 0;i < model.size();++i)
					imports.add(model.get(i).toString());
			}
			final UserVariableDialog dlg = new UserVariableDialog(this,scriptSupport,wizard,imports);
			final UserDefinedVariable var = dlg.showDialog();
			if (var != null)
				addVariable(var);
		} else if ("EDIT_VARIABLE".equals(command)) {
			final UserDefinedVariable edited = (UserDefinedVariable) II_variablesList.getSelectedValue();
			final boolean referenced = getReferencedForVariable(edited).size() > 0;
			removeVariable(edited);
			final List<String> imports = new ArrayList<String>();
			if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO) {
				SortedListModel model = (SortedListModel) II_importList.getModel();
				for (int i = 0;i < model.size();++i)
					imports.add(model.get(i).toString());
			}
			UserVariableDialog dlg = new UserVariableDialog(this,scriptSupport,edited,wizard,imports,referenced);
			final UserDefinedVariable newVariable = dlg.showDialog();
			addVariable(newVariable == null ? edited : newVariable);
			if (newVariable != null) 
				updateReferences(newVariable);
		} else if ("REMOVE_VARIABLE".equals(command)) {
			final UserDefinedVariable selected = (UserDefinedVariable) II_variablesList.getSelectedValue();
			final int result = Utilities.askUser(this,false,"Remove variable","Do you really want to remove this variable: " +  selected.getName() +
			   									 "?");
			if (result == 1) {
				final List<String> referers = getReferencedForVariable(selected);
				if (referers.size() > 0) {
					Utilities.userAlert(this,"Remove failed because the selected variable is used by the following data source(s): ",
										Utils.join(referers,",\n"));
					return;
				}
				removeVariable(selected);
				warning(true,"Variable removed: " + selected.getName(),MESSAGE,true);
			}
		}
	}

	//------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource() == I_availableStats && !e.getValueIsAdjusting()) {
			editMode = false;
			int index = I_availableStats.getSelectedIndex();
			String text = null;
			if (index == -1) {
				text = "";
				I_generatedGUIScr.setViewportView(null);
			} else {
				setBorderText(true);
				I_availableOperators.clearSelection();
				SortedListModel model = (SortedListModel) I_availableStats.getModel();
				PluginInfo<IStatisticsPlugin> info = (PluginInfo<IStatisticsPlugin>)model.get(index);
				IStatisticsPlugin stat = info.getInstance();
				if (warning(!StatisticsInfoGenerator.checkPlugin(stat),"Wrong statistic. Deleted from the list.",WARNING,true)) {
					I_availableStats.clearSelection();
					model.remove(index);
					I_generatedGUIScr.setViewportView(null);
					Utilities.setTextPane(I_descriptionPane,"");
					return;
				}
				text = stat.getDescription();
				JPanel panel = new JPanel();
				panel.setName("pane_datasource_generatedgui");
				panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
				List<Class> params = stat.getParameterTypes();
				List<String> names = stat.getParameterNames();
				List<String> tips = stat.getParameterDescriptions();
				operatorGUI = null;
				parameterGUIList = new ArrayList<IParameterGUI>();
				for (int i = 0;i < stat.getNumberOfParameters();++i) {
					IParameterGUI parGUI = getParameterGUI(params.get(i));
					if (warning(parGUI == null,"Unsupported parameter type. Statistic is deleted from the list.",WARNING,true)) {
						I_availableStats.clearSelection();
						model.remove(index);
						Utilities.setTextPane(I_descriptionPane,"");
						I_generatedGUIScr.setViewportView(null);
						return;
					}
					parameterGUIList.add(parGUI);
					JPanel parGUIComponent = parGUI.getGUIComponent(names.get(i),tips.get(i),getAvailableMembers(params.get(i)));
					panel.add(parGUIComponent);
					if (i != stat.getNumberOfParameters() - 1) {
						JSeparator separator = new JSeparator();
						separator.setMaximumSize(new Dimension(2000,3));
						panel.add(separator);
					}
				}
				
				int c=0;
				for(IParameterGUI ipg : parameterGUIList)
				{
					if(ipg instanceof CollectionParameterGUI){
						((CollectionParameterGUI)ipg).nameComponents(Integer.toString(c++));
						}
					
				}
				
				I_nameField.setText(generateUniqueName(stat.getName()));
				I_generatedGUIScr.setViewportView(panel);
			}
			I_createButton.setEnabled(index != -1);
			Utilities.setTextPane(I_descriptionPane,Utils.htmlPage(text));
			I_descriptionPane.setCaretPosition(0);
		} else if (e.getSource() == II_importList && !e.getValueIsAdjusting()) 
			II_removeButton.setEnabled(PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5 && II_importList.getSelectedIndices().length != 0);
		else if (e.getSource() == I_availableOperators && !e.getValueIsAdjusting()) {
			editMode = false;
			int index = I_availableOperators.getSelectedIndex();
			String text = null;
			if (index == -1) {
				text = "";
				I_generatedGUIScr.setViewportView(null);
			} else {
				setBorderText(false);
				I_availableStats.clearSelection();
				SortedListModel model = (SortedListModel) I_availableOperators.getModel();
				PluginInfo<IOperatorPlugin> info = (PluginInfo<IOperatorPlugin>) model.get(index);
				IOperatorPlugin operator = info.getInstance();
				text = operator.getDescription();
				parameterGUIList = null;
				operatorGUI = getParameterGUI(operator);
				if (warning(operatorGUI == null,"Unsupported operator type. Operator is deleted from the list.",WARNING,true)) {
					I_availableOperators.clearSelection();
					model.remove(index);
					Utilities.setTextPane(I_descriptionPane,"");
					I_generatedGUIScr.setViewportView(null);
					return;
				}
				I_generatedGUIScr.setViewportView(operatorGUI.getGUIComponent());
				operatorGUI.getGUIComponent().setName("pane_datasource_generatedgui");
				I_nameField.setText(generateUniqueName(operator.getName()));
			}
			I_createButton.setEnabled(index != -1);
			Utilities.setTextPane(I_descriptionPane,Utils.htmlPage(text));
			I_descriptionPane.setCaretPosition(0);
		} else if (e.getSource() == II_variablesList && !e.getValueIsAdjusting()) {
			final int idx = II_variablesList.getSelectedIndex();
			II_editVariableButton.setEnabled(idx != -1);
			II_insertVariableButton.setEnabled(idx != -1);
			II_removeVariableButton.setEnabled(idx != -1);
		}
	}
	
	//------------------------------------------------------------------------------------
	public void stateChanged(ChangeEvent e) {
		editMode = false;
		String s = (tabbed.getSelectedIndex() == 0) ? STATISTICS_TEXT
				: ((PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? SCRIPTS_TEXT_NETLOGO
						: SCRIPTS_TEXT);
		infoText = s;
		Utilities.setTextPane(infoTextPane,Utils.htmlPage(s));
		if (tabbed.getSelectedIndex() == 0)
			resetScriptTab();
		else {
			I_availableStats.clearSelection();
			I_availableOperators.clearSelection();
			I_nameField.setText("");
		}
	}
	
	public String getRecName()
	{
		return I_nameField.getText();
	}

	//=====================================================================================
	// GUI methods
	
	//-------------------------------------------------------------------------------------
	private void layoutGUI() {
		Box tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(infoTextPaneScr);
		tmp.add(new JSeparator());
		content.add(tmp,BorderLayout.NORTH);
		
		I_buttons.add(I_createButton);
		I_buttons.add(I_closeButton);

		I_left.add(I_availableStatsScr,BorderLayout.CENTER);
		I_left.add(I_availableOperatorsScr,BorderLayout.SOUTH);
		
		JPanel namePanel = FormsUtils.build("p ~ p:g",
											"|01||",
											"Name: ",I_nameField).getPanel();
		
		Box tmp2 = new Box(BoxLayout.Y_AXIS);
		tmp2.add(I_descriptionPaneScr);
		tmp2.add(namePanel);
		
		I_right.add(tmp2,BorderLayout.NORTH);
		I_right.add(I_generatedGUIScr,BorderLayout.CENTER);
		I_right.add(I_buttons,BorderLayout.SOUTH);
		
		
		I_split.setLeftComponent(I_left);
		I_split.setRightComponent(I_right);
		
		II_importPanel = FormsUtils.build("p:g ~ p",
										  "01||" +
										  "02|" +
										  "0_ f:p:g",
										  II_importListScr,II_addButton,
										  II_removeButton).getPanel();
		
		II_variablesPanel = FormsUtils.build("p:g ~ p ~ p",
											 "000 f:p:g||" +
											 "_12 p||" +
											 "_34",
											 II_variablesListScr,
											 II_insertVariableButton,II_addVariableButton,
											 II_editVariableButton,II_removeVariableButton).getPanel();
		
		II_panel = FormsUtils.build("p ~ p:g ~ p ~ p ~ p:g ~ p",
									"[DialogBorder]011222||" +
												  "344222||" +
												  "555222|" +
												  "___222 f:p:g(0.3)||" +
												  "666777 f:p:g(0.7)||" +
												  "__89__ p",
												  "Return type: ",II_returnType,II_importPanel,
												  "Script name: ",II_nameField,
												  II_startAssistantButton,
												  II_bodyAreaScr,II_variablesPanel,
												  II_createButton,II_closeButton).getPanel();
		
		
		tabbed.addTab("Statistics",I_split);
		tabbed.addTab("Scripts",II_panel);
		
		I_availableStats.setName("lst_datasource_availstats");
		I_availableOperators.setName("lst_datasource_availops");
		I_nameField.setName("fld_datasource_name");
				
		content.add(tabbed,BorderLayout.CENTER);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	}
	
	//-------------------------------------------------------------------------------------
	private void initialize() {
		infoTextPaneScr.setBorder(null);
		
		infoTextPane.setEditable(false);
		int b = GUIUtils.GUI_unit(0.5);
		infoTextPane.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
		infoText = Utils.htmlPage(STATISTICS_TEXT);
		Utilities.setTextPane(infoTextPane,infoText);
		infoTextPane.setPreferredSize(new Dimension(700,75));
		
		I_availableStatsScr.setBorder(BorderFactory.createTitledBorder("Available statistics"));
		
		I_availableStats.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		I_availableStats.addListSelectionListener(this);
		
		I_availableOperatorsScr.setBorder(BorderFactory.createTitledBorder("Available operators"));
		
		I_availableOperators.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		I_availableOperators.addListSelectionListener(this);
		
		I_descriptionPaneScr.setBorder(BorderFactory.createTitledBorder("Description"));
		I_descriptionPaneScr.setPreferredSize(new Dimension(550,95));
		I_descriptionPane.setEditable(false);
		I_descriptionPane.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
		
		I_generatedGUIScr.setBorder(BorderFactory.createTitledBorder("Actual parameters"));
		I_generatedGUIScr.setPreferredSize(new Dimension(550,400));
		
		tabbed.addChangeListener(this);
		
		I_createButton.setActionCommand("CREATE");
		I_createButton.setName("btn_ok");
		I_closeButton.setActionCommand("CLOSE");
		I_closeButton.setName("btn_cancel");
		
		I_createButton.setEnabled(false);
		
		II_importPanel.setBorder(BorderFactory.createTitledBorder("Import declarations"));
		II_importListScr.setPreferredSize(new Dimension(250,100));
		II_importList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		II_importList.addListSelectionListener(this);
		II_addButton.setEnabled(PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5);
		II_removeButton.setEnabled(false);
		
		II_bodyArea.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
		String RETURN = (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5 )? "report " : "return ";
		String END = (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5 ) ? "" : ";";
		II_bodyArea.setText(RETURN + "false" + END);
		II_bodyAreaScr.setBorder(BorderFactory.createTitledBorder("Body"));
		II_bodyAreaScr.setPreferredSize(new Dimension(450,250));
		
		II_returnType.setEditable(true);
		II_returnType.setPreferredSize(new Dimension(100,20));
		
		II_variablesPanel.setBorder(BorderFactory.createTitledBorder("User variables"));
		II_variablesList.setBorder(null);
		II_variablesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		II_variablesList.addListSelectionListener(this);
		II_variablesList.addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				final int idx = II_variablesList.getSelectedIndex();
				if (idx != -1 && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
					II_insertVariableButton.doClick(0);
			}
		});
		II_editVariableButton.setEnabled(false);
		II_insertVariableButton.setEnabled(false);
		II_removeVariableButton.setEnabled(false);
		
		String defName = searchUniqueName();
		II_nameField.setText(defName == null ? "" : defName);
		
		II_addButton.setActionCommand("ADD_IMPORT");
		II_removeButton.setActionCommand("REMOVE_IMPORT");
		II_returnType.setActionCommand("RETURN_TYPE");
		II_createButton.setActionCommand("II_CREATE");
		II_closeButton.setActionCommand("CLOSE");
		II_startAssistantButton.setActionCommand("START");
		II_insertVariableButton.setActionCommand("INSERT_VARIABLE");
		II_addVariableButton.setActionCommand("ADD_VARIABLE");
		II_editVariableButton.setActionCommand("EDIT_VARIABLE");
		II_removeVariableButton.setActionCommand("REMOVE_VARIABLE");
		
		if (ParameterSweepWizard.getPreferences().closeAfterOneScript()) {
			I_closeButton.setText("Cancel");
			II_closeButton.setText("Cancel");
		}
		
		GUIUtils.addActionListener(this,I_createButton,I_closeButton,II_addButton,II_removeButton,II_createButton,II_closeButton,II_returnType,
								   II_startAssistantButton,II_insertVariableButton,II_addVariableButton,II_editVariableButton,
								   II_removeVariableButton);
		
		initializeReturnType();
		initializeVariablesList();
		final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(null);
		this.setContentPane(sp);
		this.setPreferredSize(new Dimension(915,714));
		this.pack();
		Dimension oldD = this.getPreferredSize();
		this.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
										    oldD.height + sp.getHorizontalScrollBar().getHeight()));
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		oldD = this.getPreferredSize();
		final Dimension newD = GUIUtils.getPreferredSize(this);
		if (!oldD.equals(newD)) 
			this.setPreferredSize(newD);
		this.pack();
		I_split.setDividerLocation((int)(this.getWidth() * 0.25));
	}
	
	//=====================================================================================
	// private methods
	
	//------------------------------------------------------------------------------------
	/** Releases the <code>warningTimer</code> and the scrollpanes. */
	private void onClose() {
		if (warningTimer != null)
			warningTimer.stop();
		warningTimer = null;
		if (infoTextPaneScr != null) {
			infoTextPaneScr.removeAll();
			infoTextPaneScr = null;
		}
		if (I_descriptionPaneScr != null) {
			I_descriptionPaneScr.removeAll();
			I_descriptionPaneScr = null;
		}
	}
	
	//------------------------------------------------------------------------------------
	/** Initializes the list of the available statistic from the informations of the 
	 *  plugin manager.
	 */
	private void initializeAvailableStatisticsList() {
		SortedListModel model = new SortedListModel();
		PluginList<IStatisticsPlugin> pl = ParameterSweepWizard.getPluginManager().getStatisticsPluginInfos();
		for (PluginInfo<IStatisticsPlugin> info : pl) 
			model.addElement(info);
		model.sort(new StatisticsComparator());
		I_availableStats.setModel(model);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializeAvailableOperatorsList() {
		SortedListModel model = new SortedListModel();
		PluginList<IOperatorPlugin> pl = ParameterSweepWizard.getPluginManager().getOperatorPluginInfos();
		for (PluginInfo<IOperatorPlugin> info : pl) {
			if (info.getInstance().isSupportedByPlatform(PlatformSettings.getPlatformType()))
				model.addElement(info);
		}
		model.sort(new OperatorComparator());
		I_availableOperators.setModel(model);
	}
	
	//------------------------------------------------------------------------------------
	/** Initializes the import declarations list. */
	private void initializeImportList() {
		SortedListModel model = new SortedListModel();
		if (PlatformSettings.getPlatformType() == PlatformType.CUSTOM ||
				PlatformSettings.getPlatformType() == PlatformType.SIMPHONY2)
			model.addElement("java.util.*");
		else if (PlatformSettings.getPlatformType() == PlatformType.REPAST) {
			for (String s : defaultImports)
				model.addElement(s);
		}
		II_importList.setModel(model);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void setBorderText(boolean statistics) {
		String text = statistics ? "Actual parameters" : "";
		I_generatedGUIScr.setBorder(BorderFactory.createTitledBorder(text));
	}
	
	//------------------------------------------------------------------------------------
	/** Returns a GUI component that belongs to the type <code>type</code>. */
	private IParameterGUI getParameterGUI(Class type) {
		if (numericTypes.contains(type))
			return new NumberParameterGUI(this);
		else if (numericCollectionTypes.contains(type)) {
			CtClass	clazz = null;
			if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) {
				File f = new File(wizard.getModelFileName());
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
			}
			return new CollectionParameterGUI(this,clazz,scriptSupport.getInnerScripts());
		}
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private IOperatorGUI getParameterGUI(IOperatorPlugin operator) {
		CtClass	clazz = null;
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) { 
			File f = new File(wizard.getModelFileName());
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
		}
		switch (operator.getGUIType()) {
		case MEMBER_SELECTION 			: return new MemberSelectionOperatorGUI(clazz,scriptSupport.getAllMembers());
		case ELEMENT_SELECTION			: return new ElementSelectionOperatorGUI(wizard,operator.getLocalizedName(),scriptSupport.getAllMembers(),
																				 true,SelectionType.NORMAL);
		case SPEC_ELEMENT_SELECTION		: return new ElementSelectionOperatorGUI(wizard,operator.getLocalizedName(),scriptSupport.getAllMembers(),
																				 false,SelectionType.NORMAL);
		case LIST_SELECTION				: return new ElementSelectionOperatorGUI(wizard,operator.getLocalizedName(),scriptSupport.getAllMembers(),
																				 false,SelectionType.COLLECTION);
		case ONEONE_CONSTRUCT			: return new ElementSelectionOperatorGUI(wizard,operator.getLocalizedName(),scriptSupport.getAllMembers(),
																				 false,SelectionType.LIST);
		case MAP_SELECTION				: return new ElementSelectionOperatorGUI(wizard,operator.getLocalizedName(),scriptSupport.getAllMembers(),
																				 true,SelectionType.MAP);
		case LIST						: return new ListOperatorGUI(this,clazz,scriptSupport.getAllMembers());
		case LIST_UNION_INTERSECTION	: return new ListUnionIntersectionGUI(this,scriptSupport.getAllMembers());
		case ANY_TYPE_CONSTRUCT			: return new AnyTypeConstructOperatorGUI(this,scriptSupport.getAllMembers(),UseType.PERMUTATION);
		case SIZE						: return new AnyTypeConstructOperatorGUI(this,scriptSupport.getAllMembers(),UseType.SIZE);
		case TIME_SERIES				: return new AnyTypeConstructOperatorGUI(this,scriptSupport.getAllMembers(),UseType.TIMESERIES); 
		case BINARY_LIST_CONSTRUCT		: return new BinaryListConstructOperatorGUI(this,scriptSupport.getAllMembers());
		case REMOVE						: return new RemoveOperatorGUI(wizard,scriptSupport.getAllMembers());
		case FOREACH					: return new ForEachOperatorGUI(wizard,scriptSupport.getAllMembers(),scriptSupport.getForeachMembers());
		case FILTER						: return new FilterOperatorGUI(wizard,operator.getLocalizedName(),scriptSupport.getAllMembers());
		case MULTIPLE_COLUMN			: return new MultiColumnRecordableOperatorGUI(wizard, this, clazz, scriptSupport.getAllMembers());
		}
		return null;
	}
	
	//------------------------------------------------------------------------------------
	/** Returns a list of information objects. This list contains those objects that
	 *  can be served as an actual parameter (or part of an actual parameter) in the place 
	 *  of a formal parameter with type <code>type</code>.
	 */
	private List<MemberInfo> getAvailableMembers(Class type) {
		if (numericTypes.contains(type))
			return scriptSupport.getAllMembers().filterNumbers();
		else if (numericCollectionTypes.contains(type))
			return scriptSupport.getAllMembers().filterInvalids();
		return null;
	}
	
	//------------------------------------------------------------------------------------
	/** Updates the content of the information panel. */
	private void updateInfo() {
		String s = getInfoText();
		Utilities.setTextPane(infoTextPane, s == null ? "" : s);
	}
	
	//------------------------------------------------------------------------------------
	/** Clears the warning/message from the information panel and updates its content. */
	private void clearProblemText() {
		warning = null;
		if (warningTimer != null) {
			warningTimer.stop();
			warningTimer = null;
		}
		updateInfo();
	}
	
	//------------------------------------------------------------------------------------
	/** Initializes the combobox that contains the possible return types. */
	private void initializeReturnType() {
		DefaultComboBoxModel model = new DefaultComboBoxModel();
		model.addElement(new ReturnTypeElement("boolean","false",Boolean.TYPE));
		model.addElement(new ReturnTypeElement("String","\"\"",String.class));
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) {
			model.addElement(new ReturnTypeElement("byte","0",Byte.TYPE));
			model.addElement(new ReturnTypeElement("double","0.0",Double.TYPE));
			model.addElement(new ReturnTypeElement("float","0.0",Float.TYPE));
			model.addElement(new ReturnTypeElement("int","0",Integer.TYPE));
			model.addElement(new ReturnTypeElement("long","0",Long.TYPE));
			model.addElement(new ReturnTypeElement("short","0",Short.TYPE));
		} else {
			II_returnType.setEditable(false);
			model.addElement(new ReturnTypeElement("number","0",Double.TYPE));
			model.addElement(new ReturnTypeElement("other","[]",unknown.class));
		}
		II_returnType.setModel(model);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializeVariablesList() {
		SortedListModel model = new SortedListModel();
		for (final UserDefinedVariable variable : scriptSupport.getUserVariables())
			model.addElement(variable);
		model.sort(new UserDefinedVariableComparator());
		II_variablesList.setModel(model);
		II_variablesList.setCellRenderer(new UserVariableListCellRenderer());
	}
 	
	//------------------------------------------------------------------------------------
	/** Replaces the returning values of a script with <code>defaultValue</code>. */
	private void replaceReturnValue(String defaultValue) { 
		String RETURN = (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5 ) ? "report " : "return ";
		String END = (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5 ) ? "" : ";";
		StringBuilder text = new StringBuilder(II_bodyArea.getText());
		int index = text.indexOf(RETURN);
		if (index == -1)  
			text.append(RETURN + defaultValue + END);
		while (index != -1) {
			int valueIndexFrom = text.indexOf(" ",index);
			int valueIndexTo = text.indexOf(";",valueIndexFrom + 1);
			if (valueIndexTo == -1) 
				valueIndexTo = text.indexOf(" ",valueIndexFrom + 1);
			if (valueIndexTo == -1) 
				valueIndexTo = text.indexOf("\n",valueIndexFrom + 1);
			if (valueIndexTo == -1) 
				valueIndexTo = text.length();
			text.replace(valueIndexFrom + 1,valueIndexTo,defaultValue);
			index = text.indexOf(RETURN,index + 5);
		}
		II_bodyArea.setText(text.toString());
	}
	
	//------------------------------------------------------------------------------------
	/** Checks whether <code>text</code> is a well-formed import declaration or not. */
	private boolean isValidImportDeclaration(String text) {
		if (text == null || text.equals(""))
			return false;
		if (importPattern == null)
			importPattern = Pattern.compile("^(\\w+\\.)+(\\w+|\\*)$");
		Matcher m = importPattern.matcher(text);
		return m.matches();
	}
	
	//------------------------------------------------------------------------------------
	/** Creates a unique script name. */
	private String searchUniqueName() {
		int nextID = 0;
		boolean isUnique = true;
		String candidate = null;
		final String suffix = (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5 ) ? "" : "()";
		do {
			candidate = DEFAULT_NAME_PREFIX + String.valueOf(nextID++);
			isUnique = !scriptSupport.getAllMembers().contains(new MemberInfo(candidate + suffix,null,null)) &&
					   !scriptSupport.getInnerScripts().contains(new InnerOperatorGeneratedMemberInfo(candidate + suffix,null,null,null));
		} while (!isUnique);
		return candidate;
	}
	
	//------------------------------------------------------------------------------------
	/** Returns whether the name <code>name</code> is a unique script name or not.
	 * 	Pre-condition: <code>name</code> is not null and not an empty string
	 */
	private boolean isUniqueName(final String name) {
		String _name = name;
		if (name.endsWith(")")) {
			int index = name.lastIndexOf('(');
			_name = name.substring(0,index);
		}
		final List<MemberInfo> all = new ArrayList<MemberInfo>(scriptSupport.getAllMembers());
		all.addAll(scriptSupport.getInnerScripts());
		for (final MemberInfo mi : all) {
			String miName = mi.getName();
			if (mi.getName().endsWith(")")) {
				int index = mi.getName().lastIndexOf('(');
				miName = mi.getName().substring(0,index);
			}
			if (_name.equals(miName)) return false;
		}
		return true;
	}
	
	//------------------------------------------------------------------------------------
	/** Resets the Scripts page. */
	private void resetScriptTab() {
		II_bodyArea.setText("");
		II_nameField.setText(searchUniqueName());
		II_returnType.setSelectedIndex(0);
		II_variablesList.clearSelection();
		II_editVariableButton.setEnabled(false);
		II_insertVariableButton.setEnabled(false);
		II_removeVariableButton.setEnabled(false);
	}
	
	//----------------------------------------------------------------------------------------------------
	private Class<?> getJavaType(String type) {
		if (type == null || type.trim().equals("")) return null;
		String candidate = type.trim();
		if (candidate.equals("char")) return Character.TYPE;
		Class<?> result = null;
		RetryLoader rl = (RetryLoader) wizard.getClassLoader();
		rl.stopRetry();
		try {
			result = Class.forName(candidate,true,wizard.getClassLoader());
		} catch (ClassNotFoundException e) {
			try {
				result = Class.forName("java.lang." + candidate,true,wizard.getClassLoader());
			} catch (ClassNotFoundException ee) {
				SortedListModel model = (SortedListModel) II_importList.getModel();
				for (int i = 0;i < model.size();++i) {
					String importDecl = model.get(i).toString().trim();
					int lastDot = importDecl.lastIndexOf(".");
					String suffix =  importDecl.substring(lastDot + 1);
					String newCandidate = null;
					if (suffix.endsWith("*")) 
						newCandidate = importDecl.substring(0,lastDot) + "." + candidate;
					else if (suffix.equals(candidate))
						newCandidate = importDecl;
					if (newCandidate != null) {
						try {
							result = Class.forName(newCandidate,true,wizard.getClassLoader());
							break;
						} catch (ClassNotFoundException eee) {} 
					}
				}
			}
		} finally {
			rl.startRetry();
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isRecordableScript(final ReturnTypeElement returnType) {
		final Class<?> type = returnType.getJavaType();
		return isRecordableScript(type);
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isRecordableScript(final Class<?> type) {
		return numericTypes.contains(type) || type.equals(Boolean.TYPE) || type.equals(Boolean.class) || type.equals(String.class);
	}
	
	//----------------------------------------------------------------------------------------------------
	private String generateUniqueName(final String name) {
		String _name = name;
		if (name.endsWith(")")) {
			final int index = name.lastIndexOf('(');
			_name = name.substring(0,index);
		}
		int count = 0;
		String candidate = _name + String.valueOf(count);
		while (!isUniqueName(candidate)) 
			candidate = _name + String.valueOf(++count);
		return _name + String.valueOf(count) + ((PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? "" : "()");
	}
	
	//----------------------------------------------------------------------------------------------------
	private void editScript(GeneratedMemberInfo edited) throws CannotLoadDataSourceForEditingException { 
		if (edited instanceof ScriptGeneratedMemberInfo) {
			ScriptGeneratedMemberInfo info = (ScriptGeneratedMemberInfo) edited;
			if (info instanceof VariableScriptGeneratedMemberInfo)
				throw new CannotLoadDataSourceForEditingException(info.getName() + " is an automatically generated script therefore it is not " +
																				   "editable.");
			tabbed.setSelectedIndex(1);
			editMode = true; // because the previous line is set editMode to false
			II_nameField.setText(info.getName());
			ReturnTypeElement returnType = getReturnTypeElement(info);
			DefaultComboBoxModel rTModel = (DefaultComboBoxModel) II_returnType.getModel();
			if (rTModel.getIndexOf(returnType) == -1)
				rTModel.addElement(returnType);
			rTModel.setSelectedItem(returnType);
			if (info.getImports() != null) {
				SortedListModel importModel = (SortedListModel) II_importList.getModel();
				for (String importDecl : info.getImports()) {
					if (!importModel.contains(importDecl))
						importModel.addElement(importDecl);
				}
				importModel.sort();
			}
			String source = info.getSource();
			if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) {
				source = source.substring(6);
				int index = source.lastIndexOf("catch");
				source = source.substring(0,index - 2);
			}
			II_bodyArea.setText(source);
			Thread hackThread = new Thread(new Runnable() {
				Object o = new Object();
				public void run() {
					try {
						synchronized (o) { o.wait(2); }
						II_bodyArea.grabFocus();
					} catch (InterruptedException e) {}
				}
			});
			hackThread.setName("Script-Support-Thread-0");
			hackThread.start();
			return;
		} else if (edited instanceof SimpleGeneratedMemberInfo) {
			SimpleGeneratedMemberInfo info = (SimpleGeneratedMemberInfo) edited;
			PluginInfo<IStatisticsPlugin> stat = ParameterSweepWizard.getPluginManager().getStatisticsPluginInfos().
												 findPInfoByName(info.getGeneratorName());
			if (stat == null)
				throw new CannotLoadDataSourceForEditingException(edited.getName() + " is not editable.");
			I_availableStats.setSelectedValue(stat,true);
			editMode = true;
			int index = I_availableStats.getSelectedIndex();
			Rectangle cellBounds = I_availableStats.getCellBounds(index + 1,index + 1);
			if (cellBounds != null)
				I_availableStats.scrollRectToVisible(cellBounds);
			I_nameField.setText(info.getName());
			List<List<? extends Object>> buildBlocks = info.getBuildingBlocks();
			for (int i = 0;i < parameterGUIList.size();++i) {
				List<? extends Object> _buildBlock = buildBlocks.get(i);
				List<MemberInfo> block = new ArrayList<MemberInfo>(_buildBlock.size());
				for (Object o : _buildBlock)
					block.add((MemberInfo)o);
				parameterGUIList.get(i).buildContent(block);
			}
			return;
		} else if (edited instanceof OperatorGeneratedMemberInfo) {
			OperatorGeneratedMemberInfo info = (OperatorGeneratedMemberInfo) edited;
			PluginInfo<IOperatorPlugin> operator = ParameterSweepWizard.getPluginManager().getOperatorPluginInfos().
												   findPInfoByName(info.getGeneratorName());
			if (operator == null)
				throw new CannotLoadDataSourceForEditingException(edited.getName() + " is not editable.");
			I_availableOperators.setSelectedValue(operator,true);
			editMode = true;
			int index = I_availableOperators.getSelectedIndex();
			Rectangle cellBounds = I_availableOperators.getCellBounds(index + 1,index + 1);
			if (cellBounds != null)
				I_availableOperators.scrollRectToVisible(cellBounds);
			I_nameField.setText(info.getName());
			List<List<? extends Object>> buildBlocks = info.getBuildingBlocks();
			operatorGUI.buildContent(buildBlocks.get(0));
			return;
		}
		throw new CannotLoadDataSourceForEditingException(edited.getName() + " is not editable.");
	}
	
	//----------------------------------------------------------------------------------------------------
	private ReturnTypeElement getReturnTypeElement(ScriptGeneratedMemberInfo mi) {
		if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) {
			if (mi.getJavaType().equals(Double.TYPE))
				return new ReturnTypeElement("number",Double.TYPE);
			else if (mi.getJavaType().equals(unknown.class))
				return new ReturnTypeElement("other",unknown.class);
		}
		return new ReturnTypeElement(mi.getType(),mi.getJavaType());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void deletePreviousInstance(GeneratedMemberInfo info) {
		scriptSupport.getAllMembers().remove(info);
		Fugg.f.remove(info.getName() + " : " + info.getType());
		RecordableElement element = new RecordableElement(info);
		scriptListModel.removeElement(element);
		nonRecordableScriptListModel.removeElement(element);
	}
	
	//----------------------------------------------------------------------------------------------------
	private ScriptGeneratedMemberInfo checkScript(ScriptGeneratedMemberInfo info, List<String> errors) { 
		for (final UserDefinedVariable variable : scriptSupport.getUserVariables()) {
			if (info.getSource().contains(variable.getName()))
				info.addUserVariable(variable);
		}
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
	
	//----------------------------------------------------------------------------------------------------
	private void addVariable(final UserDefinedVariable variable) {
		if (!scriptSupport.getUserVariables().contains(variable)) {
			scriptSupport.getUserVariables().add(variable);
			SortedListModel model = (SortedListModel) II_variablesList.getModel();
			model.addElement(variable);
			model.sort(new UserDefinedVariableComparator());
			Valt.v.setValtozo(variable.getName(),variable.getType().getCanonicalName());
			final VariableScriptGeneratedMemberInfo getterMethod = scriptSupport.createScriptForVariable(variable);
			scriptSupport.getAllMembers().add(getterMethod);
			Fugg.f.setFuggveny(getterMethod.getName() + " : " + getterMethod.getType(),"");
			RecordableElement element = new RecordableElement(getterMethod);
			if (isRecordableScript(variable.getType()))
				scriptListModel.addElement(element);
			else
				nonRecordableScriptListModel.addElement(element);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void removeVariable(final UserDefinedVariable variable) {
		scriptSupport.getUserVariables().remove(variable);
		((SortedListModel)II_variablesList.getModel()).removeElement(variable);
		Valt.v.removeValtozo(variable.getName());
		VariableScriptGeneratedMemberInfo scriptForVariable = scriptSupport.getScriptForVariable(variable);
		if (scriptForVariable != null)
			deletePreviousInstance(scriptForVariable);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void updateReferences(final UserDefinedVariable newVariable) {
		final VariableScriptGeneratedMemberInfo getter = scriptSupport.getScriptForVariable(newVariable);
		for (final MemberInfo info : scriptSupport.getAllMembers()) {
			if (info instanceof GeneratedMemberInfo) {
				final GeneratedMemberInfo gmi = (GeneratedMemberInfo) info;
				if (gmi.getReferences().contains(getter)) {
					gmi.removeReference(getter);
					gmi.addReference(getter);
				} else if (gmi instanceof ScriptGeneratedMemberInfo) {
					final ScriptGeneratedMemberInfo sgmi = (ScriptGeneratedMemberInfo) gmi;
					if (sgmi.getUserVariables().contains(newVariable)) {
						sgmi.removeUserVariable(newVariable);
						sgmi.addUserVariable(newVariable);
					}
				}
			} 
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private List<String> getReferencedForVariable(final UserDefinedVariable variable) {
		final List<String> result = new ArrayList<String>();
		final VariableScriptGeneratedMemberInfo getter = scriptSupport.getScriptForVariable(variable); 
		for (final MemberInfo info : scriptSupport.getAllMembers()) {
			if (info instanceof GeneratedMemberInfo) {
				final GeneratedMemberInfo gmi = (GeneratedMemberInfo) info;
				if (gmi.getReferences().contains(getter))
					result.add(gmi.getName());
				else if (gmi instanceof ScriptGeneratedMemberInfo && !(gmi instanceof VariableScriptGeneratedMemberInfo)) {
					final ScriptGeneratedMemberInfo sgmi = (ScriptGeneratedMemberInfo) gmi;
					if (sgmi.getUserVariables().contains(variable))
						result.add(sgmi.getName());
				}
			}
		}
		return result;
	}
	
	//====================================================================================
	// nested classes
	
	//------------------------------------------------------------------------------------
	/** Comparator to compare statistic plugins. The base of the comparing is the 
	 *  localized name of the plugins.
	 */
	private static class StatisticsComparator implements Comparator<PluginInfo<IStatisticsPlugin>> {
		public int compare(PluginInfo<IStatisticsPlugin> o1, PluginInfo<IStatisticsPlugin> o2) { return o1.toString().compareToIgnoreCase(o2.toString());	}
	}
	
	//----------------------------------------------------------------------------------------------------
	private static class OperatorComparator implements Comparator<PluginInfo<IOperatorPlugin>> {
		public int compare(PluginInfo<IOperatorPlugin> o1, PluginInfo<IOperatorPlugin> o2) { return o1.toString().compareToIgnoreCase(o2.toString()); }
	}
	
	//----------------------------------------------------------------------------------------------------
	private static class UserDefinedVariableComparator implements Comparator<UserDefinedVariable> {
		public int compare(UserDefinedVariable o1, UserDefinedVariable o2) { return o1.getName().compareToIgnoreCase(o2.getName()); }
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	private static class UserVariableListCellRenderer extends DefaultListCellRenderer {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		@Override public Component getListCellRendererComponent(final JList list, final Object value, final int index, final boolean isSelected,
																final boolean cellHasFocus) {
			final JLabel label = (JLabel) super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
			if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO) {
				final UserDefinedVariable variable = (UserDefinedVariable) value;
				label.setText(variable.getName() + " : " + getNetLogoTypeName(variable.getType()));
			}
			return label;
		}
		
		//----------------------------------------------------------------------------------------------------
		private String getNetLogoTypeName(final Class<?> type) {
			if (type == null)
				throw new IllegalArgumentException("'type' is null");
			if (type.equals(Boolean.TYPE)) 
				return "boolean";
			if (type.equals(String.class))
				return "String";
			if (type.equals(Double.TYPE))
				return "number";
			else
				return "other";
		}
	}
}
