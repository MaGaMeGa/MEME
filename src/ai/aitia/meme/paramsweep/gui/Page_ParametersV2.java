package ai.aitia.meme.paramsweep.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ai.aitia.meme.Logger;
import ai.aitia.meme.events.HybridAction;
import ai.aitia.meme.events.IHybridActionListener;
import ai.aitia.meme.gui.Wizard;
import ai.aitia.meme.gui.Wizard.Button;
import ai.aitia.meme.gui.Wizard.IArrowsInHeader;
import ai.aitia.meme.gui.Wizard.IWizardPage;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.batch.IModelInformation.ModelInformationException;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.generator.WizardSettingsManager;
import ai.aitia.meme.paramsweep.gui.info.AvailableParameter;
import ai.aitia.meme.paramsweep.gui.info.MasonChooserParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInATree;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.internal.platform.IGUIController;
import ai.aitia.meme.paramsweep.internal.platform.IGUIController.RunOption;
import ai.aitia.meme.paramsweep.internal.platform.InfoConverter;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.utils.ParameterEnumeration;
import ai.aitia.meme.paramsweep.utils.ParameterParserException;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.FormsUtils.Separator;
import ai.aitia.meme.utils.GUIUtils;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.looks.common.RGBGrayFilter;
import com.wordpress.tips4java.ListAction;

/** This class provides a new version of Parameters page of the Parameter Sweep Wizard. */
public class Page_ParametersV2 implements IWizardPage, IArrowsInHeader, ActionListener, FocusListener {
	
	//====================================================================================================
	// members
	
	static final int ICON_WIDTH_AND_HEIGHT = 25;
	
	private static final ImageIcon PARAMETER_ADD_ICON_DIS = new ImageIcon(new ImageIcon(Page_ParametersV2.class.getResource("icons/arrow_button_metal_silver_right.png")).
															getImage().getScaledInstance(ICON_WIDTH_AND_HEIGHT, ICON_WIDTH_AND_HEIGHT, Image.SCALE_SMOOTH));
	private static final ImageIcon PARAMETER_ADD_ICON = new ImageIcon(new ImageIcon(Page_ParametersV2.class.getResource("icons/arrow_button_metal_green_right.png")).
														getImage().getScaledInstance(ICON_WIDTH_AND_HEIGHT, ICON_WIDTH_AND_HEIGHT, Image.SCALE_SMOOTH));
	private static final ImageIcon PARAMETER_ADD_ICON_RO = new ImageIcon(new ImageIcon(Page_ParametersV2.class.getResource("icons/arrow_button_metal_red_right.png")).
														   getImage().getScaledInstance(ICON_WIDTH_AND_HEIGHT, ICON_WIDTH_AND_HEIGHT, Image.SCALE_SMOOTH));
	
	private static final ImageIcon PARAMETER_REMOVE_ICON_DIS = new ImageIcon(new ImageIcon(Page_ParametersV2.class.getResource("icons/arrow_button_metal_silver_left.png")).
														   	   getImage().getScaledInstance(ICON_WIDTH_AND_HEIGHT, ICON_WIDTH_AND_HEIGHT, Image.SCALE_SMOOTH));
	private static final ImageIcon PARAMETER_REMOVE_ICON = new ImageIcon(new ImageIcon(Page_ParametersV2.class.getResource("icons/arrow_button_metal_green_left.png")).
														   getImage().getScaledInstance(ICON_WIDTH_AND_HEIGHT, ICON_WIDTH_AND_HEIGHT, Image.SCALE_SMOOTH));
	private static final ImageIcon PARAMETER_REMOVE_ICON_RO = new ImageIcon(new ImageIcon(Page_ParametersV2.class.getResource("icons/arrow_button_metal_red_left.png")).
															  getImage().getScaledInstance(ICON_WIDTH_AND_HEIGHT, ICON_WIDTH_AND_HEIGHT, Image.SCALE_SMOOTH));
		
	private static final ImageIcon PARAMETER_UP_ICON_DIS = new ImageIcon(new ImageIcon(Page_ParametersV2.class.getResource("icons/arrow_button_metal_silver_up.png")).
														   getImage().getScaledInstance(ICON_WIDTH_AND_HEIGHT, ICON_WIDTH_AND_HEIGHT, Image.SCALE_SMOOTH));
	private static final ImageIcon PARAMETER_UP_ICON = new ImageIcon(new ImageIcon(Page_ParametersV2.class.getResource("icons/arrow_button_metal_green_up.png")).
													   getImage().getScaledInstance(ICON_WIDTH_AND_HEIGHT, ICON_WIDTH_AND_HEIGHT, Image.SCALE_SMOOTH));
	private static final ImageIcon PARAMETER_UP_ICON_RO = new ImageIcon(new ImageIcon(Page_ParametersV2.class.getResource("icons/arrow_button_metal_red_up.png")).
														  getImage().getScaledInstance(ICON_WIDTH_AND_HEIGHT, ICON_WIDTH_AND_HEIGHT, Image.SCALE_SMOOTH));
																																				
	private static final ImageIcon PARAMETER_DOWN_ICON_DIS = new ImageIcon(new ImageIcon(Page_ParametersV2.class.getResource("icons/arrow_button_metal_silver_down.png")).
															 getImage().getScaledInstance(ICON_WIDTH_AND_HEIGHT, ICON_WIDTH_AND_HEIGHT, Image.SCALE_SMOOTH));
	private static final ImageIcon PARAMETER_DOWN_ICON = new ImageIcon(new ImageIcon(Page_ParametersV2.class.getResource("icons/arrow_button_metal_green_down.png")).
														 getImage().getScaledInstance(ICON_WIDTH_AND_HEIGHT, ICON_WIDTH_AND_HEIGHT, Image.SCALE_SMOOTH));
	private static final ImageIcon PARAMETER_DOWN_ICON_RO = new ImageIcon(new ImageIcon(Page_ParametersV2.class.getResource("icons/arrow_button_metal_red_down.png")).
															getImage().getScaledInstance(ICON_WIDTH_AND_HEIGHT, ICON_WIDTH_AND_HEIGHT, Image.SCALE_SMOOTH));

	private static final ImageIcon PARAMETER_BOX_REMOVE = new ImageIcon(new ImageIcon(Page_ParametersV2.class.getResource("icons/remove_box.png")).getImage().
														  getScaledInstance(ICON_WIDTH_AND_HEIGHT, ICON_WIDTH_AND_HEIGHT, Image.SCALE_SMOOTH));
	
	private static final String ACTIONCOMMAND_ADD_PARAM = "ADD_PARAM",
								ACTIONCOMMAND_REMOVE_PARAM = "REMOVE_PARAM",
								ACTIONCOMMAND_ADD_BOX = "ADD_BOX",
								ACTIONCOMMAND_REMOVE_BOX = "REMOVE_BOX",
								ACTIONCOMMAND_MOVE_UP = "MOVE_UP",
								ACTIONCOMMAND_MOVE_DOWN = "MOVE_DOWN",
								ACTIONCOMMAND_EDIT = "EDIT",
								ACTIONCOMMAND_BROWSE = "BROWSE",
								ACTIONCOMMAND_ADD_ENUM = "ADD_ENUM",
								ACTIONCOMMAND_REMOVE_ENUM = "REMOVE_ENUM",
								ACTIONCOMMAND_NEW_PARAMETERS = "NEW_PARAMETER";

	private static final String TITLE = "Parameters";
	private static final String INFO_TEXT = "Specify input parameter values: Set the number of runs for each parameter value and define the parameter space";
	private static final String ORIGINAL_TEXT = "Select a parameter to modify its settings.";
	
	/** The owner of the page. */
	private ParameterSweepWizard owner = null;
	/** The list of information objects of the parameters. */ 
	private List<ParameterInfo> parameters = null;
	/** The names of the parameters. */
	private String[] initParamResult = null;
	/** The names of the parameters of the original model. */
	private String[] originalInitParamResult = null;
	/** The list of information objects of the new (defined by this wizard) parameters. */
	private List<ParameterInfo> newParameters = null;
	
	private IntelliContext intelliContext = null;
	private boolean modelInformationException = false;

	private final Container container;
	
	private JLabel runsLabel = new JLabel("Runs: ");
	private JTextField runsField = new JTextField("");
	private JRadioButton constDef;
	private JRadioButton listDef;
	private JRadioButton incrDef;
	private JTextField constDefField;
	private JTextArea listDefArea;
	private JTextField incrStartValueField;
	private JTextField incrEndValueField;
	private JTextField incrStepField;
	private JComboBox<Object> enumDefBox;
	private JList<Object> leftEnumValueList;
	private JList<Object> rightEnumValueList;
	private JButton addEnumButton;
	private JButton removeEnumButton;
	private JButton cancelButton;
	private JButton modifyButton;
	private JTextField fileTextField;
	private JButton browseFileButton = new JButton(FileSystemView.getFileSystemView().getSystemIcon(new File(".")));

	private JLabel editedParameterText;
	private JPanel rightMiddle;

	private JList<AvailableParameter> parameterList;
	private JButton newParameterButton = new JButton("Add new parameters...");
	private JButton newParameterButtonCopy = new JButton("Add new parameters...");
	
	private DefaultMutableTreeNode editedNode;
	private JTree editedTree; 
	private List<ParameterCombinationGUI> parameterTreeBranches = new ArrayList<ParameterCombinationGUI>(5);
	private List<ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>> batchParameters;

	private JPanel combinationsPanel;
	private JScrollPane combinationsScrPane;

	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public Page_ParametersV2(final ParameterSweepWizard owner) {
		this.owner = owner;
		container = initContainer();
		//Style.apply(container, dashboard.getCssStyle()); //TODO: 
	}

	//----------------------------------------------------------------------------------------------------
	/** {@inheritDoc} 
	 */
	@Override
	public String getInfoText(final Wizard w) {
		return w.getArrowsHeader(INFO_TEXT);
	}
	
	//----------------------------------------------------------------------------------------------------
	/** {@inheritDoc} 
	 */
	@Override
	public Container getPanel() {
		
		return container;
	}

	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	private Container initContainer() {
		
		// left
		parameterList = new JList<AvailableParameter>();
		parameterList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		new ListAction<AvailableParameter>(parameterList,new AbstractAction() {
         public void actionPerformed(final ActionEvent event) {
				final AvailableParameter selectedParameter = parameterList.getSelectedValue();
				addParameterToTree(Collections.singletonList(selectedParameter), parameterTreeBranches.get(0));
				enableDisableParameterCombinationButtons();
			}
		});
		parameterList.addListSelectionListener(new ListSelectionListener() {
         public void valueChanged(ListSelectionEvent e) {
				if (!parameterList.isSelectionEmpty()) {
					boolean success = true;
					if (editedNode != null) 
						success = modify();
					
					if (success) {
						cancelAllSelectionBut(parameterList);
						resetSettings();
						enableDisableParameterCombinationButtons();
					} else
						parameterList.clearSelection();
				}
			}
		});
		
		final JScrollPane parameterListPane = new JScrollPane(parameterList);
		parameterListPane.setBorder(BorderFactory.createTitledBorder(null, "Model parameters", TitledBorder.LEADING, TitledBorder.BELOW_TOP)); 
		parameterListPane.setPreferredSize(new Dimension(230, 300)); 
		
		final JPanel parametersPanel = FormsUtils.build("p:g p",
										  "[DialogBorder]00 f:p:g||" +
														"_1 p",
														parameterListPane,
														newParameterButton).getPanel();
		newParameterButton.setActionCommand("NEW_PARAMETER");
		newParameterButtonCopy.setActionCommand("NEW_PARAMETER");
		
		combinationsPanel = new JPanel(new GridLayout(0, 1, 5, 5)); 
		combinationsScrPane = new JScrollPane(combinationsPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		combinationsScrPane.setBorder(null);
		combinationsScrPane.setPreferredSize(new Dimension(400, 500)); 
		
		final JButton addNewBoxButton = new JButton("Add new combination"); 
		addNewBoxButton.setActionCommand(ACTIONCOMMAND_ADD_BOX);
		
		final JPanel left = FormsUtils.build("p ~ f:p:g ~ p",
				 "011 f:p:g ||" +
				 "0_2 p",
				 parametersPanel,combinationsScrPane,
				 addNewBoxButton).getPanel();
		left.setBorder(BorderFactory.createTitledBorder(null, "Specify parameter combinations", TitledBorder.LEADING, TitledBorder.BELOW_TOP));
//		Style.registerCssClasses(left, Dashboard.CSS_CLASS_COMMON_PANEL); //TODO: check
		
		// right
		editedParameterText = new JLabel(ORIGINAL_TEXT);
		editedParameterText.setPreferredSize(new Dimension(250, 40)); 
		
		runsField = new JTextField();
		runsField.setActionCommand("RUNS_FIELD");
		runsField.addFocusListener(this);
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.GLOBAL)
			runsField.setEnabled(true);
		
		constDef = new JRadioButton("Constant");
		listDef = new JRadioButton("List");
		incrDef = new JRadioButton("Increment");
		
		final JPanel rightTop = FormsUtils.build("p ' p:g",
				   "[DialogBorder]00||" +
						   		 "12||" + 
					             "33||" +
					             "44||" + 
					             "55",
	                editedParameterText,
	                runsLabel, runsField,
					constDef,
					listDef,
					incrDef).getPanel();
		
//		Style.registerCssClasses(rightTop, Dashboard.CSS_CLASS_COMMON_PANEL); //TODO
		
		constDefField = new JTextField();
		final JPanel constDefPanel = FormsUtils.build("p ~ p:g",
								 "[DialogBorder]01 p",
								 "Constant value: ", CellConstraints.TOP,constDefField).getPanel();
		
		listDefArea = new JTextArea();
		final JScrollPane listDefScr = new JScrollPane(listDefArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		final JPanel listDefPanel = FormsUtils.build("p ~ p:g",
								"[DialogBorder]01|" +
								              "_1 f:p:g||" +
								              "_2 p",
								"Value list: ", listDefScr,
								"(Separate values with spaces!)").getPanel();
		
		incrStartValueField = new JTextField();
		incrEndValueField = new JTextField();
		incrStepField = new JTextField();
		
		final JPanel incrDefPanel = FormsUtils.build("p ~ p:g",
								"[DialogBorder]01||" +
											  "23||" +
											  "45",
											  "Start value: ", incrStartValueField,
											  "End value: ", incrEndValueField,
											  "Step: ", incrStepField).getPanel();
		
		enumDefBox = new JComboBox<Object>(new DefaultComboBoxModel<Object>());
		final JPanel enumDefPanel = FormsUtils.build("p ~ p:g", 
								"[DialogBorder]01 p", 
								"Constant value:", CellConstraints.TOP, enumDefBox).getPanel();
		
		leftEnumValueList = new JList<Object>(new DefaultListModel<Object>());
		rightEnumValueList = new JList<Object>(new DefaultListModel<Object>());
		final JScrollPane leftEnumValuePane = new JScrollPane(leftEnumValueList,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		final JScrollPane rightEnumValuePane = new JScrollPane(rightEnumValueList,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		leftEnumValuePane.setPreferredSize(new Dimension(125,100));
		rightEnumValuePane.setPreferredSize(new Dimension(125,100));
		
		addEnumButton = new JButton();
		addEnumButton.setOpaque(false);
		addEnumButton.setRolloverEnabled(true);
		addEnumButton.setIcon(PARAMETER_ADD_ICON);
		addEnumButton.setRolloverIcon(PARAMETER_ADD_ICON_RO);
		addEnumButton.setDisabledIcon(PARAMETER_ADD_ICON_DIS);
		addEnumButton.setBorder(null);
		addEnumButton.setBorderPainted(false);
		addEnumButton.setContentAreaFilled(false);
		addEnumButton.setFocusPainted(false);
		addEnumButton.setToolTipText("Add selected value");
		addEnumButton.setActionCommand(ACTIONCOMMAND_ADD_ENUM);
		
		removeEnumButton = new JButton();
		removeEnumButton.setOpaque(false);
		removeEnumButton.setRolloverEnabled(true);
		removeEnumButton.setIcon(PARAMETER_REMOVE_ICON);
		removeEnumButton.setRolloverIcon(PARAMETER_REMOVE_ICON_RO);
		removeEnumButton.setDisabledIcon(PARAMETER_REMOVE_ICON_DIS);
		removeEnumButton.setBorder(null);
		removeEnumButton.setBorderPainted(false);
		removeEnumButton.setContentAreaFilled(false);
		removeEnumButton.setFocusPainted(false);
		removeEnumButton.setToolTipText("Remove selected value");
		removeEnumButton.setActionCommand(ACTIONCOMMAND_REMOVE_ENUM);
				
		final JPanel enumListPanel = FormsUtils.build("p:g ~ p ' p ~ p:g",
				 				"[DialogBorder]0011 f:p:g||" +
				 							  "_23_ p||",
							   leftEnumValuePane,rightEnumValuePane,
							   removeEnumButton,addEnumButton).getPanel();
		
		leftEnumValueList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(final MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && e.getComponent().isEnabled() && e.getClickCount() == 2) {
					final DefaultListModel<Object> model = (DefaultListModel<Object>) rightEnumValueList.getModel();
					
					for (final Object element : leftEnumValueList.getSelectedValuesList())
						model.addElement(element);
				}
			}
		});
		rightEnumValueList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && e.getComponent().isEnabled() && e.getClickCount() == 2) {
					final DefaultListModel<Object> model = (DefaultListModel<Object>) rightEnumValueList.getModel();
					
					for (final Object element : rightEnumValueList.getSelectedValuesList())
						model.removeElement(element);
				}
			}
		});
		
		fileTextField = new JTextField();
		fileTextField.addKeyListener(new KeyAdapter() {
			public void keyTyped(final KeyEvent e) {
				final char character = e.getKeyChar();
				final File file = new File(Character.isISOControl(character) ? fileTextField.getText() : fileTextField.getText() + character);
				fileTextField.setToolTipText(file.getAbsolutePath());
			}
		});
		browseFileButton.setActionCommand(ACTIONCOMMAND_BROWSE);
		final JPanel fileDefPanel = FormsUtils.build("p ~ p:g ~p",
									   "[DialogBorder]012",
									   "File:", fileTextField, browseFileButton).getPanel();
		
		constDefPanel.setName("CONST");
		listDefPanel.setName("LIST");
		incrDefPanel.setName("INCREMENT");
		enumDefPanel.setName("ENUM");
		enumListPanel.setName("ENUMLIST");
		fileDefPanel.setName("FILE");

		//TODO
//		Style.registerCssClasses(constDefPanel,Dashboard.CSS_CLASS_COMMON_PANEL);
//		Style.registerCssClasses(listDefPanel,Dashboard.CSS_CLASS_COMMON_PANEL);
//		Style.registerCssClasses(incrDefPanel,Dashboard.CSS_CLASS_COMMON_PANEL);
//		Style.registerCssClasses(enumDefPanel,Dashboard.CSS_CLASS_COMMON_PANEL);
//		Style.registerCssClasses(fileDefPanel,Dashboard.CSS_CLASS_COMMON_PANEL);
		
		rightMiddle = new JPanel(new CardLayout());
//		Style.registerCssClasses(rightMiddle, Dashboard.CSS_CLASS_COMMON_PANEL); //TODO
		rightMiddle.add(constDefPanel, constDefPanel.getName());
		rightMiddle.add(listDefPanel, listDefPanel.getName());
		rightMiddle.add(incrDefPanel, incrDefPanel.getName());
		rightMiddle.add(enumDefPanel, enumDefPanel.getName());
		rightMiddle.add(enumListPanel, enumListPanel.getName());
		rightMiddle.add(fileDefPanel, fileDefPanel.getName());
		
		modifyButton = new JButton("Modify");
		cancelButton = new JButton("Cancel");
		
		final JPanel rightBottom = FormsUtils.build("p:g p ~ p ~ p:g",
							  "[DialogBorder]_01_ p",
							  modifyButton, cancelButton).getPanel();
		
//		Style.registerCssClasses(rightBottom, Dashboard.CSS_CLASS_COMMON_PANEL); //TODO
		
		final JPanel right = new JPanel(new BorderLayout());
		right.add(rightTop, BorderLayout.NORTH);
		right.add(rightMiddle, BorderLayout.CENTER);
		right.add(rightBottom, BorderLayout.SOUTH);
		right.setBorder(BorderFactory.createTitledBorder(null, "Parameter settings", TitledBorder.LEADING, TitledBorder.BELOW_TOP));

//		Style.registerCssClasses(right, Dashboard.CSS_CLASS_COMMON_PANEL); //TODO

		// the whole paramsweep panel
		
		final JPanel content = FormsUtils.build("p:g p",
								   "01 f:p:g",
								   left, right).getPanel();
//		Style.registerCssClasses(content, Dashboard.CSS_CLASS_COMMON_PANEL); //TODO
		
		JPanel sweepPanel = new JPanel();
//		Style.registerCssClasses(sweepPanel, Dashboard.CSS_CLASS_COMMON_PANEL); //TODO
		sweepPanel.setLayout(new BorderLayout());
		final JScrollPane sp = new JScrollPane(content);
		sp.setBorder(null);
		sp.setViewportBorder(null);
		sweepPanel.add(sp,  BorderLayout.CENTER);
		
		GUIUtils.createButtonGroup(constDef, listDef, incrDef);
		constDef.setSelected(true);
		constDef.setActionCommand("CONST");
		listDef.setActionCommand("LIST");
		incrDef.setActionCommand("INCREMENT");
		
		constDefField.setActionCommand("CONST_FIELD");
		incrStartValueField.setActionCommand("START_FIELD");
		incrEndValueField.setActionCommand("END_FIELD");
		incrStepField.setActionCommand("STEP_FIELD");
		
		modifyButton.setActionCommand("EDIT");
		cancelButton.setActionCommand("CANCEL");
		
		listDefArea.setLineWrap(true);
		listDefArea.setWrapStyleWord(true);
		listDefScr.setPreferredSize(new Dimension(100, 200)); 
		
		GUIUtils.addActionListener(this, modifyButton, cancelButton, constDef, listDef, incrDef, constDefField, incrStartValueField, incrEndValueField, incrStepField,
								   addNewBoxButton, browseFileButton, runsField, newParameterButton, newParameterButtonCopy, addEnumButton, removeEnumButton);
		
		runsField.setName("fld_wizard_params_runs");
		constDef.setName("rbtn_wizard_params_const");
		listDef.setName("rbtn_wizard_params_list");
		incrDef.setName("rbtn_wizard_params_incr");
		modifyButton.setName("btn_wizard_params_modify");
		cancelButton.setName("btn_wizard_params_cancel");
		incrStartValueField.setName("fld_wizard_params_incrstart");
		incrEndValueField.setName("fld_wizard_params_incrend");
		incrStepField.setName("fld_wizard_params_incrstep");
		constDefField.setName("fld_wizard_params_constval");
		listDefArea.setName("fld_wizard_params_paramlist");
		newParameterButton.setName("btn_wizard_params_newparameter");
		enumDefBox.setName("cb_wizard_params_enum_const");
		addEnumButton.setName("btn_wizard_params_add_enum_value");
		removeEnumButton.setName("btn_wizard_params_remove_enum_value");
		browseFileButton.setName("btn_wizard_params_browse");
		fileTextField.setName("fld_wizard_params_file_const");
		addNewBoxButton.setName("btn_wizard_params_newbox");
		
		return sweepPanel;
	}
	
	//----------------------------------------------------------------------------------------------------
	private JPanel createAParameterBox(final boolean first) {
		
		final JLabel runLabel = new JLabel("<html><b>Number of runs:</b> 0</html>");
		final JLabel warningLabel = new JLabel();
		
		final JButton closeButton = new JButton();
		closeButton.setOpaque(false);
	    closeButton.setFocusable(false);
	    closeButton.setBorderPainted(false);
	    closeButton.setContentAreaFilled(false);
	    closeButton.setFocusPainted(false);
	    closeButton.setBorder(null);
	    
	    if (!first) {
		    closeButton.setRolloverIcon(PARAMETER_BOX_REMOVE);
		    closeButton.setRolloverEnabled(true);
		    closeButton.setIcon(RGBGrayFilter.getDisabledIcon(closeButton, PARAMETER_BOX_REMOVE));
		    closeButton.setActionCommand(ACTIONCOMMAND_REMOVE_BOX);
	    }
	    
	    final JScrollPane treeScrPane = new JScrollPane();
	    final DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode();
	    final JTree tree = new JTree(treeRoot);
	    ToolTipManager.sharedInstance().registerComponent(tree);
	    
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setCellRenderer(new ParameterBoxTreeRenderer());
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			public void valueChanged(final TreeSelectionEvent e) {
				final TreePath selectionPath = tree.getSelectionPath();
				boolean success = true;
				if (editedNode != null && (selectionPath == null || !editedNode.equals(selectionPath.getLastPathComponent()))) 
					success = modify();
				
				if (success) {
					if (selectionPath != null) {
						cancelAllSelectionBut(tree);
						final DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
						if (!node.equals(editedNode)) {
							ParameterInATree userObj = null;
							final DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
							if (!node.isRoot() && selectionPath.getPathCount() == model.getPathToRoot(node).length) {
								userObj = (ParameterInATree) node.getUserObject();
								final ParameterInfo info = userObj.info;
								editedNode = node;
								editedTree = tree;
								edit(info);
							} else {
								tree.setSelectionPath(null);
								if (cancelButton.isEnabled())
									cancelButton.doClick();
								resetSettings();
								enableDisableSettings(false);
								editedNode = null;
								editedTree = null;
							}
						} 
					} 
					
					enableDisableParameterCombinationButtons();
				} else {
					final DefaultTreeModel model = (DefaultTreeModel) editedTree.getModel();
					final DefaultMutableTreeNode storedEditedNode = editedNode;
					editedNode = null;
					tree.setSelectionPath(null);
					editedNode = storedEditedNode;
					editedTree.setSelectionPath(new TreePath(model.getPathToRoot(editedNode)));
				}
			}
		});

	    treeScrPane.setViewportView(tree);
	    treeScrPane.setBorder(null);
	    treeScrPane.setViewportBorder(null);
		treeScrPane.setPreferredSize(new Dimension(250, 250)); 
		
		final JButton upButton = new JButton();
		final JButton downButton = new JButton();
		
		final JPanel mainPanel = FormsUtils.build("~ f:p:g ~ p ~ r:p",
												  "012||" +
												  "333||" +
												  "44_||" + 
												  "445||" +
												  "446||" + 
												  "44_ f:p:g",
												  runLabel,first ? "" : warningLabel, first ? warningLabel : closeButton,
												  new Separator(""),
												  treeScrPane,
												  upButton,
												  downButton).getPanel(); 
				
				
		mainPanel.setBorder(BorderFactory.createTitledBorder(""));
		
		final JButton addButton = new JButton();
		addButton.setOpaque(false);
		addButton.setRolloverEnabled(true);
		addButton.setIcon(PARAMETER_ADD_ICON);
		addButton.setRolloverIcon(PARAMETER_ADD_ICON_RO);
		addButton.setDisabledIcon(PARAMETER_ADD_ICON_DIS);
		addButton.setBorder(null);
		addButton.setBorderPainted(false);
		addButton.setContentAreaFilled(false);
		addButton.setFocusPainted(false);
		addButton.setToolTipText("Add selected parameter");
		addButton.setActionCommand(ACTIONCOMMAND_ADD_PARAM);
		
		final JButton removeButton = new JButton();
		
		final JPanel result = FormsUtils.build("p ~ f:p:g",
											   "_0 f:p:g||" +
											   "10 p ||" +
											   "20 p||" +
											   "_0 f:p:g",
											   mainPanel,
											   addButton,
											   removeButton).getPanel();
		
//		Style.registerCssClasses(result,Dashboard.CSS_CLASS_COMMON_PANEL); //TODO:
		
		final ParameterCombinationGUI pcGUI = new ParameterCombinationGUI(tree, treeRoot, runLabel, warningLabel, addButton, removeButton, upButton, downButton);
		parameterTreeBranches.add(pcGUI);
		
		int idx = parameterTreeBranches.size() - 1;
		if (!first)
			closeButton.setName("btn_wizard_params_close_" + idx);
		upButton.setName("btn_wizard_params_moveup_" + idx);
		downButton.setName("btn_wizard_params_movedown_" + idx);
		addButton.setName("btn_wizard_params_addparam_" + idx);
		removeButton.setName("btn_wizard_params_removeparam_" + idx);
		
		class HybridActionListener implements ActionListener, IHybridActionListener {
			
			//====================================================================================================
			// methods
			
			//----------------------------------------------------------------------------------------------------
			public void actionPerformed(final ActionEvent e) {
				hybridAction(e, null);
			}
			
			//----------------------------------------------------------------------------------------------------
			public void hybridAction(ActionEvent e, HybridAction a) {
				String cmd = null;
				if ( a != null ) 
					cmd = a.getValue(Action.ACTION_COMMAND_KEY).toString();
				else 
					cmd = e.getActionCommand();
					
				if (ACTIONCOMMAND_ADD_PARAM.equals(cmd)) 
					handleAddParameter(pcGUI);
				else if (ACTIONCOMMAND_REMOVE_PARAM.equals(cmd)) 
					handleRemoveParameter(tree);
				else if (ACTIONCOMMAND_REMOVE_BOX.equals(cmd)) 
					handleRemoveBox(tree);
				else if (ACTIONCOMMAND_MOVE_UP.equals(cmd)) 
					handleMoveUp();
				else if (ACTIONCOMMAND_MOVE_DOWN.equals(cmd))
					handleMoveDown();
			}

			//----------------------------------------------------------------------------------------------------
			private void handleAddParameter(final ParameterCombinationGUI pcGUI) {
				final List<AvailableParameter> selectedValues = parameterList.getSelectedValuesList();
				if (selectedValues != null && selectedValues.size() > 0) {
					addParameterToTree(selectedValues, pcGUI);
					enableDisableParameterCombinationButtons();
				}
			}
			
			//----------------------------------------------------------------------------------------------------
			private void handleRemoveParameter(final JTree tree) {
				final TreePath selectionPath = tree.getSelectionPath();
				if (selectionPath != null) {
					cancelButton.doClick();
					
					final DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
					if (!node.isRoot()) {
						final DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
						if (parentNode.isRoot()) { 
							removeParameter(tree, node, parentNode);
							enableDisableParameterCombinationButtons();
						}
					}
				}
			}
			
			//----------------------------------------------------------------------------------------------------
			private void handleRemoveBox(final JTree tree) {
				final int answer = Utilities.askUser(owner,false,"Comfirmation","This operation deletes the combination.", 
																			  	"All related parameter returns back to the list on the left side.",
																			  	"Are you sure?");
				if (answer == 1) {
					final DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();

					if (tree.getSelectionCount() > 0) {
						editedNode = null;
						tree.setSelectionPath(null);
						if (cancelButton.isEnabled())
							cancelButton.doClick();
					}
					
					final DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
					for (int i = 0; i < root.getChildCount(); ++i) {
						final DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
						removeParameter(tree, node, root);
					}
					
					enableDisableParameterCombinationButtons();

					parameterTreeBranches.remove(pcGUI);
					combinationsPanel.remove(result);
					combinationsPanel.revalidate();
					
					updateNumberOfRuns();
				}
			}
			
			//----------------------------------------------------------------------------------------------------
			private void removeParameter(final JTree tree, final DefaultMutableTreeNode node, final DefaultMutableTreeNode parentNode) {
				final ParameterInATree userObj = (ParameterInATree) node.getUserObject();
				final ParameterInfo originalInfo = findMatchedInfo(userObj.info);
				if (originalInfo != null) {
					final DefaultListModel<AvailableParameter> model = (DefaultListModel<AvailableParameter>) parameterList.getModel();
					model.addElement(new AvailableParameter(originalInfo));
					final DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
					treeModel.removeNodeFromParent(node);
					updateNumberOfRuns();
					tree.expandPath(new TreePath(treeModel.getPathToRoot(parentNode)));
				} else 
					throw new IllegalStateException("Parameter " + userObj.info.getName() + " is not found in the model.");
			}
			
			//----------------------------------------------------------------------------------------------------
			private void handleMoveUp() {
				final TreePath selectionPath = tree.getSelectionPath();
				if (selectionPath != null) {
					boolean success = true;
					if (editedNode != null) 
						success = modify();
					
					if (success) {
						final DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
						final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
						
						if (parent == null || parent.getFirstChild().equals(node)) {
							tree.setSelectionPath(null); // we need this to preserve the state of the parameter settings panel
							tree.setSelectionPath(new TreePath(node.getPath()));
							return;
						}
						
						final int index = parent.getIndex(node);
						final DefaultTreeModel treemodel = (DefaultTreeModel) tree.getModel();
						treemodel.removeNodeFromParent(node);
						treemodel.insertNodeInto(node, parent, index - 1);
						tree.setSelectionPath(new TreePath(node.getPath()));
					}

				}
			}
				
			//----------------------------------------------------------------------------------------------------
			private void handleMoveDown() {
				final TreePath selectionPath = tree.getSelectionPath();
				if (selectionPath != null) {
					boolean success = true;
					if (editedNode != null) 
						success = modify();
					
					if (success) {
						final DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
						final DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
						
						if (parent == null || parent.getLastChild().equals(node)) {
							tree.setSelectionPath(null); // we need this to preserve the state of the parameter settings panel
							tree.setSelectionPath(new TreePath(node.getPath()));
							return;
						}
						
						final int index = parent.getIndex(node);
						final DefaultTreeModel treemodel = (DefaultTreeModel) tree.getModel();
						treemodel.removeNodeFromParent(node);
						treemodel.insertNodeInto(node, parent, index + 1);
						tree.setSelectionPath(new TreePath(node.getPath()));
					}
				}
			}
		};
		
		final HybridActionListener hybridActionListener = new HybridActionListener(); 
		GUIUtils.addActionListener(hybridActionListener, closeButton, addButton );
		
		final HybridAction upAction = new HybridAction(hybridActionListener,"Move up",null,Action.ACTION_COMMAND_KEY,ACTIONCOMMAND_MOVE_UP);
		final HybridAction downAction = new HybridAction(hybridActionListener,"Move down",null,Action.ACTION_COMMAND_KEY,ACTIONCOMMAND_MOVE_DOWN);
		final HybridAction removeAction = new HybridAction(hybridActionListener,"Remove",null,Action.ACTION_COMMAND_KEY,ACTIONCOMMAND_REMOVE_PARAM);
		
		final JPopupMenu contextMenu = new JPopupMenu();
		contextMenu.setName("cmenu_wizard_parameters_treecmenu_" + idx);
		contextMenu.add(upAction);
		contextMenu.add(downAction);
		contextMenu.add(removeAction);
			
		contextMenu.getComponent(0).setName("btn_wizard_params_cmenumoveup_" + idx);
		contextMenu.getComponent(1).setName("btn_wizard_params_cmenumovedown_" + idx);
		contextMenu.getComponent(2).setName("btn_wizard_params_cmenuremove_" + idx);
		
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e) && e.getComponent().isEnabled()) {
					DefaultMutableTreeNode node = mouseOnNode(tree, e);
					if (node == null) 
						node = (DefaultMutableTreeNode) tree.getPathForRow(tree.getRowCount() - 1).getLastPathComponent();
					tree.setSelectionPath(new TreePath(node.getPath()));
					contextMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
					
		upButton.setAction(upAction);
		upButton.setOpaque(false);
		upButton.setRolloverEnabled(true);
		upButton.setText(""); // need to delete the text explicitly because the button inherits text from the action
		upButton.setIcon(PARAMETER_UP_ICON);
		upButton.setRolloverIcon(PARAMETER_UP_ICON_RO);
		upButton.setDisabledIcon(PARAMETER_UP_ICON_DIS);
		upButton.setBorder(null);
		upButton.setBorderPainted(false);
		upButton.setContentAreaFilled(false);
		upButton.setFocusPainted(false);
		upButton.setToolTipText("Move up the selected parameter");

		downButton.setAction(downAction);
		downButton.setOpaque(false);
		downButton.setRolloverEnabled(true);
		downButton.setText(""); // need to delete the text explicitly because the button inherits text from the action
		downButton.setIcon(PARAMETER_DOWN_ICON);
		downButton.setRolloverIcon(PARAMETER_DOWN_ICON_RO);
		downButton.setDisabledIcon(PARAMETER_DOWN_ICON_DIS);
		downButton.setBorder(null);
		downButton.setBorderPainted(false);
		downButton.setContentAreaFilled(false);
		downButton.setFocusPainted(false);
		downButton.setToolTipText("Move down the selected parameter");

		
		removeButton.setAction(removeAction);
		removeButton.setOpaque(false);
		removeButton.setRolloverEnabled(true);
		removeButton.setText(""); // need to delete the text explicitly because the button inherits text from the action
		removeButton.setIcon(PARAMETER_REMOVE_ICON);
		removeButton.setRolloverIcon(PARAMETER_REMOVE_ICON_RO);
		removeButton.setDisabledIcon(PARAMETER_REMOVE_ICON_DIS);
		removeButton.setBorder(null);
		removeButton.setBorderPainted(false);
		removeButton.setContentAreaFilled(false);
		removeButton.setFocusPainted(false);
		removeButton.setToolTipText("Remove selected parameter");
		
		result.setPreferredSize(new Dimension(300,250)); 
		enableDisableParameterCombinationButtons();
		
//		Style.apply(result, dashboard.getCssStyle()); //TODO

		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Returns the node that belongs to the location of the event. */
	private DefaultMutableTreeNode mouseOnNode(final JTree tree, final MouseEvent event) {
		Point loc = event.getPoint();
		TreePath path = null; 
		for (int i = 20;path == null && i <= loc.x;path = tree.getPathForLocation(i,loc.y),i += 20);
		if (path != null)
			return (DefaultMutableTreeNode) path.getLastPathComponent();
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void addParameterToTree(final List<AvailableParameter> parameters, final ParameterCombinationGUI pcGUI) {
		final DefaultListModel<AvailableParameter> listModel = (DefaultListModel<AvailableParameter>) parameterList.getModel();
		final DefaultTreeModel treeModel = (DefaultTreeModel) pcGUI.tree.getModel();
		final DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
		
		for (final AvailableParameter parameter : parameters) {
			listModel.removeElement(parameter);
			final ParameterInfo selectedInfo = parameter.info;
			final DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new ParameterInATree(selectedInfo));
			treeModel.insertNodeInto(newNode, root, root.getChildCount());
		}

		updateNumberOfRuns();
		pcGUI.tree.expandPath(new TreePath(treeModel.getPathToRoot(root)));
	}

 
	//----------------------------------------------------------------------------------------------------
	public void resetParamsweepGUI(){
		changeText(null, null);
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL)
			runsField.setText(""); 
		constDef.setSelected(true);
		constDefField.setText(null);
		incrStartValueField.setText(null);
		incrEndValueField.setText(null);
		incrStepField.setText(null);
		listDefArea.setText(null);
		fileTextField.setText(null);
		fileTextField.setToolTipText(null);
		leftEnumValueList.setModel(new DefaultListModel<Object>());
		rightEnumValueList.setModel(new DefaultListModel<Object>());
		combinationsPanel.removeAll();
		parameterTreeBranches.clear();
		combinationsPanel.add(createAParameterBox(true));
		combinationsPanel.revalidate();
	}
	
	//----------------------------------------------------------------------------------------------------
	/** {@inheritDoc} 
	 */
	@Override
	public boolean isEnabled(final Button b) {
		if (modelInformationException && (b == Button.NEXT || b == Button.FINISH)) return false;
		return PlatformSettings.isEnabledForPageParameters(owner,b);
	}
	
	//----------------------------------------------------------------------------------------------------
	/** {@inheritDoc} 
	 */
	public boolean onButtonPress(final Button b) { 
		boolean enabled = isEnabled(b);
		if (enabled) {
			if (b == Button.CANCEL) return true ;
			if (editedNode == null || modify()) {
				String invalidInfoName = checkInfos();
				if (invalidInfoName != null) {
					Utilities.userAlert(owner,"Please specify a file for parameter " + invalidInfoName);
					return false;
				}
				
				return true;
			}
		}
		return false;
	}

	//----------------------------------------------------------------------------------------------------
	public void focusGained(FocusEvent e) {}
	
	//----------------------------------------------------------------------------------------------------
	public void focusLost(FocusEvent e) {
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.GLOBAL)
			updateNumberOfRuns();
	}

	//----------------------------------------------------------------------------------------------------
	/** {@inheritDoc} 
	 */
	public void onPageChange(boolean show) {
		if (show) {
			if (owner.getModelState() == ParameterSweepWizard.ERROR) return;
			initializePageForPlatform();
			if (owner.getModelState() == ParameterSweepWizard.NEW_MODEL) {
				modelInformationException = false;
				resetParamsweepGUI();
				parameters = createParameters();
				if (parameters == null) return;
				if (owner.getParameterFile() != null && owner.getParameterFile().exists()) {
					try {
						final DefaultMutableTreeNode root = PlatformSettings.parseParameterFile(parameters,owner.getParameterFile());
						setGlobalRunsField(root);
						initializePageFromTree(root);
					} catch (ParameterParserException | WizardLoadingException e) {
						Utilities.userAlert(owner,"Cannot initialize from the defined parameter file.","Reason: " + Util.getLocalizedMessage(e));
						e.printStackTrace(ParameterSweepWizard.getLogStream());
						createDefaultParameterList(parameters);;
					}
				} else
					createDefaultParameterList(parameters);
				enableDisableSettings(false);
				modifyButton.setEnabled(false);
				owner.setModelState(ParameterSweepWizard.NEW_RECORDERS);
			}
			if (owner.getParameterFile() != null && owner.isParameterFileChanged()) { 
				if (owner.getModelState() != ParameterSweepWizard.NEW_MODEL && owner.getParameterFile().exists()) {
					try {
						final DefaultMutableTreeNode root = PlatformSettings.parseParameterFile(parameters,owner.getParameterFile());
						setGlobalRunsField(root); 
						initializePageFromTree(root);
					} catch (ParameterParserException | WizardLoadingException e) {
						Utilities.userAlert(owner,"Cannot initialize from the defined parameter file.","Reason: " + Util.getLocalizedMessage(e));
						e.printStackTrace(ParameterSweepWizard.getLogStream());
					}
				}
			}
			owner.setParameterFileChanged(false);
			updateNumberOfRuns();
		} else {
			if (cancelButton.isEnabled())
				cancelButton.doClick();
		}
	}

	//----------------------------------------------------------------------------------------------------
	@Override
	public String getTitle() {
		return TITLE;
	}
	
	//-------------------------------------------------------------------------------
	/** Loads the page-related model settings from the XML element <code>element</code>. This
	 *  method is part of the model settings retrieving performed by {@link ai.aitia.meme.paramsweep.generator.WizardSettingsManager 
	 *  WizardSettingsManager}.
	 * @throws WizardLoadingException if the XML document is invalid
	 */
	public void load(Element element) throws WizardLoadingException { 
		NodeList nl = element.getElementsByTagName(WizardSettingsManager.NEW_PARAMETERS);
		if (nl == null || nl.getLength() == 0)
			newParameters = null;
		else {
			NodeList nodes = ((Element)nl.item(0)).getElementsByTagName(WizardSettingsManager.PARAMETER);
			if (nodes == null || nodes.getLength() == 0)
				throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.PARAMETER);
			newParameters = new ArrayList<ParameterInfo>(nodes.getLength());
			for (int i = 0;i < nodes.getLength();++i) {
				Element param = (Element) nodes.item(i);
				String type = param.getAttribute(WizardSettingsManager.TYPE);
				if (type == null || type.equals("")) 
					throw new WizardLoadingException(true,"missing 'type' attribute at node: " + WizardSettingsManager.PARAMETER);
				String javaTypeStr = param.getAttribute(WizardSettingsManager.JAVA_TYPE);
				if (javaTypeStr == null || "".equals(javaTypeStr))
					throw new WizardLoadingException(true,"missing 'java_type' attribute at node: " + WizardSettingsManager.PARAMETER);
				NodeList content = param.getChildNodes();
				if (content == null || content.getLength() == 0)
					throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.PARAMETER);
				String name = ((Text)content.item(0)).getNodeValue();
				Class<?> javaType = null;
				try {
					javaType = Utilities.toClass(owner.getClassLoader(),javaTypeStr);
				} catch (ClassNotFoundException e) {
					throw new WizardLoadingException(true,"invalid type at parameter: " + name);
				}
				ParameterInfo pi = new ParameterInfo(name,type,javaType);
				if (!pi.isNumeric() && !pi.isBoolean() && !pi.getType().equals("String"))
					throw new WizardLoadingException(true,"invalid 'type' attribute at node: " + WizardSettingsManager.PARAMETER);
				pi.setInitValue();
				pi.setRuns(1);
				newParameters.add(pi);
			}
		}
		
		resetParamsweepGUI();
		parameters = createParameters();
		
		nl = null;
		nl = element.getElementsByTagName(WizardSettingsManager.PARAMETER_FILE);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.PARAMETER_FILE);
		NodeList content = nl.item(0).getChildNodes();
		if (content == null || content.getLength() == 0)
			throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.PARAMETER_FILE);
		String paramFileContent = ((Text)content.item(0)).getNodeValue();
		DefaultMutableTreeNode root = null;
		try {
			root = PlatformSettings.parseParameterFile(parameters,paramFileContent);
			initializePageFromTree(root);
		} catch (ParameterParserException e) {
			throw new WizardLoadingException(true,e);
		}
		if (cancelButton.isEnabled()) 
			cancelButton.doClick();
		setGlobalRunsField(root); 
		updateNumberOfRuns();
	}
	
	//-------------------------------------------------------------------------------
	/** Saves the page-related model settings to the XML node <code>node</code>. This
	 *  method is part of the model settings storing performed by {@link ai.aitia.meme.paramsweep.generator.WizardSettingsManager 
	 *  WizardSettingsManager}.
	 * @throws TransformerException 
	 * @throws ParserConfigurationException 
	 */
	public void save(Node node) throws ParserConfigurationException, TransformerException {
		final Document document = node.getOwnerDocument();
		
		Element element = document.createElement(WizardSettingsManager.PARAMETER_FILE);
		element.appendChild(document.createTextNode(PlatformSettings.generateParameterTreeOutput(createTreeFromParameterPage())));
		node.appendChild(element);
		
		if (newParameters != null) {
			Element np = document.createElement(WizardSettingsManager.NEW_PARAMETERS);
			for (ParameterInfo pi : newParameters) {
				element = document.createElement(WizardSettingsManager.PARAMETER);
				element.setAttribute(WizardSettingsManager.TYPE,pi.getType());
				element.setAttribute(WizardSettingsManager.JAVA_TYPE,pi.getJavaType().getName());
				String name = pi.getName();
				final RecordableInfo dummy = new RecordableInfo(name, Integer.TYPE, null, name);
				try {
					if (owner.getModelInformation().getRecordables().indexOf(dummy) == -1)
						name = Util.uncapitalize(name);
				} catch (ModelInformationException e) { 
					throw new IllegalStateException(e);
				}
				element.appendChild(document.createTextNode(name));
				np.appendChild(element);
			}
			node.appendChild(np);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public String[] getInitParamResult() { return initParamResult; }
	public List<ParameterInfo> getNewParametersList() { return newParameters; }
	public List<ParameterInfo> getParameters() { return parameters; }

	//------------------------------------------------------------------------------
    public IIntelliContext getIntelliContext(){
		if (intelliContext == null)
			intelliContext = new IntelliContext();
		return intelliContext;
	}
	
	//------------------------------------------------------------------------------
	/** Creates the list of information objects of parameters. */
	private List<ParameterInfo> createParameters() { 
		try {
			batchParameters = owner.getModelInformation().getParameters();
		} catch (ModelInformationException e) {
			Utilities.userAlert(owner,"Identification of parameters is failed:", Util.getLocalizedMessage(e));
			modelInformationException = true;
			owner.enableDisableButtons();
			owner.setModelState(ParameterSweepWizard.ERROR);
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			return null;
		}
		originalInitParamResult = new String[batchParameters.size()];
		for (int i = 0;i < batchParameters.size();++i)
			originalInitParamResult[i] = batchParameters.get(i).getName();
		List<ParameterInfo> info = new ArrayList<ParameterInfo>();
		for (ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?> parameter : batchParameters) {
			ParameterInfo pi = InfoConverter.parameterInfo2ParameterInfo(parameter);
			if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL)
				pi.setRuns(1);
			pi.setDefinitionType(ParameterInfo.CONST_DEF);
			pi.setValue(parameter.getDefaultValue());
			info.add(pi);
			
		}
		if (newParameters != null) { 
			initParamResult = new String[originalInitParamResult.length + newParameters.size()];
			for (int i = 0;i < originalInitParamResult.length;++i)
				initParamResult[i] = originalInitParamResult[i];
			for (int i = 0;i < newParameters.size();++i) {
				String name = newParameters.get(i).getName();
				initParamResult[originalInitParamResult.length + i] = name;
				newParameters.get(i).setName(Util.capitalize(name));
				info.add(newParameters.get(i).clone());
			}
//			info.addAll(newParameters);
		} else 
			initParamResult = originalInitParamResult;
		Collections.sort(info);
		return info;
	}

	
	//------------------------------------------------------------------------------
	/** Builds the parameter list on the gui from the <code>parameters</code> list. */
	private void createDefaultParameterList(final List<ParameterInfo> parameters) {
		final DefaultListModel<AvailableParameter> model = new DefaultListModel<>();
		for (final ParameterInfo parameterInfo : parameters) 
			model.addElement(new AvailableParameter(parameterInfo));
		parameterList.setModel(model); 
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.GLOBAL)
			runsField.setText("1");
	}
	
	//------------------------------------------------------------------------------
	/** Edits the information object <code>info</code> so this method fills the appropriate
	 *  components with the informations of <code>info</code>.
	 */
	private void edit(final ParameterInfo info) { 
		changeText(info.getName(), info.getType());
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL)
			runsField.setText(String.valueOf(info.getRuns()));
		String view = null;
		switch (info.getDefinitionType()) {
		case ParameterInfo.CONST_DEF : {
			if (info.isEnum() || info instanceof MasonChooserParameterInfo) {
				view = "ENUM";
				constDef.setSelected(true);
				incrDef.setEnabled(false);
				listDef.setEnabled(true);
				Object[] elements = null;
				if (info.isEnum()){
					@SuppressWarnings("unchecked")
					final Class<Enum<?>> type = (Class<Enum<?>>) info.getJavaType();
					elements = type.getEnumConstants();
				} else {
					final MasonChooserParameterInfo chooserInfo = (MasonChooserParameterInfo) info;
					elements = chooserInfo.getValidStrings();
				}
				final DefaultComboBoxModel<Object> model = (DefaultComboBoxModel<Object>) enumDefBox.getModel();
				model.removeAllElements();
				for (final Object object : elements) {
					model.addElement(object);
				}
				if (info.isEnum())
					enumDefBox.setSelectedItem(info.getValue());
				else 
					enumDefBox.setSelectedIndex((Integer)info.getValue());
			} else if (info.isFile()) {
				view = "FILE";
				constDef.setSelected(true);
				final File file = (File) info.getValue();
				if (file != null) {
					fileTextField.setText(file.getName());
					fileTextField.setToolTipText(file.getAbsolutePath());
				}
			} else {
				view = "CONST";
				constDef.setSelected(true);
				constDefField.setText(info.valuesToString());
			}
			break;
		}
		case ParameterInfo.LIST_DEF  :
			if (info.isEnum() || info instanceof MasonChooserParameterInfo) {
				view = "ENUMLIST";
				listDef.setSelected(true);
				incrDef.setEnabled(false);
				constDef.setEnabled(true);
				Object[] elements = null;
				if (info.isEnum()){
					@SuppressWarnings("unchecked")
					final Class<Enum<?>> type = (Class<Enum<?>>) info.getJavaType();
					elements = type.getEnumConstants();
				} else {
					final MasonChooserParameterInfo chooserInfo = (MasonChooserParameterInfo) info;
					elements = chooserInfo.getValidStrings();
				}
				
				final DefaultListModel<Object> leftEnumModel = (DefaultListModel<Object>) leftEnumValueList.getModel();
				leftEnumModel.removeAllElements();
				for (final Object object : elements)
					leftEnumModel.addElement(object);
				
				final DefaultListModel<Object> rightEnumModel = (DefaultListModel<Object>) rightEnumValueList.getModel();
				rightEnumModel.removeAllElements();
				for (final Object object : info.getValues()) {
					if (info.isEnum())
						rightEnumModel.addElement(object);
					else {
						final MasonChooserParameterInfo chooserInfo = (MasonChooserParameterInfo) info;
						rightEnumModel.addElement(chooserInfo.getValidStrings()[(Integer) object]);
					}
				}
			} else {
				view = "LIST";
				listDef.setSelected(true);
				listDefArea.setText(info.valuesToString());
			}
			
			break;
		case ParameterInfo.INCR_DEF  : view = "INCREMENT";
			incrDef.setSelected(true);
			incrStartValueField.setText(info.startToString());
			incrEndValueField.setText(info.endToString());
			incrStepField.setText(info.stepToString());
		}
		
		final CardLayout cl = (CardLayout) rightMiddle.getLayout();
		cl.show(rightMiddle,view);
		enableDisableSettings(true);
		if (!info.isNumeric()) 
			incrDef.setEnabled(false);
		if (info.isFile()) { 
			incrDef.setEnabled(false);
			listDef.setEnabled(false);
		}
		modifyButton.setEnabled(true);
	}

	//------------------------------------------------------------------------------
	/** Changes the text on the right top corner of the page. The text can be the initial
	 *  commentary text ("Select a parameter to modify its settings") or
	 *  the name and type of the edited parameter.
	 * @param name name of the edited parameter (null if there is no editing parameter)
	 * @param type name of type of the edited parameter
	 */
	private void changeText(final String name, final String type) {
		if (name == null) {
			editedParameterText.setText(ORIGINAL_TEXT);
		} else {
			final String humanName = name.replaceAll("([A-Z])", " $1");
			editedParameterText.setText("<html><b>" + humanName + "</b>: " + type + "</html>");
		}
	}

	//----------------------------------------------------------------------------------------------------
	private boolean modify() {
		final ParameterInATree userObj = (ParameterInATree) editedNode.getUserObject();
		
		final String[] errors = checkInput(userObj.info);
		if (errors != null && errors.length != 0) {
			Utilities.userAlert(container,(Object)errors);
			return false;
		} else {
			modifyParameterInfo(userObj.info);
			resetSettings();
			modifyButton.setEnabled(false);
			final DefaultTreeModel model = (DefaultTreeModel) editedTree.getModel();
			model.nodeStructureChanged(editedNode);
			
			editedNode = null;
			editedTree = null;
			
			updateNumberOfRuns();
			enableDisableSettings(false);
			return true;
		}
	}

	//------------------------------------------------------------------------------
	/** Checks the content of the editing components. 
	 * @param info the info object that belongs to the edited parameter
	 * @return the error messages in an array (or null if there is no error)
	 */
	private String[] checkInput(final ParameterInfo info) {
		final ArrayList<String> errors = new ArrayList<String>();
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL) {
			final boolean allowEmptyRun = constDef.isSelected();
			if (runsField.getText().trim().equals("") && !allowEmptyRun) 
				errors.add("'Runs' cannot be empty.");
			else if (!runsField.getText().trim().equals("")) {
				try {
					long i = new Long(runsField.getText().trim()).longValue();
					if (i <= 0)
						throw new NumberFormatException();
				} catch (NumberFormatException e) {
					errors.add("'Runs' must be a positive integer.");
				}
			}
		}
		if (constDef.isSelected()) {
			if (info.isEnum() || info instanceof MasonChooserParameterInfo) { 
				// well, the combo box must contain a valid value
			} else if (info.isFile()) {
				if (fileTextField.getText().trim().isEmpty())
					Logger.logWarning("Empty string was specified as file parameter " + info.getName().replaceAll("([A-Z])"," $1"));

				final File file = new File(fileTextField.getToolTipText());
				if (!file.exists()) 
					errors.add("File " + fileTextField.getToolTipText() + " does not exist.");
			} else if (constDefField.getText().trim().equals(""))
				errors.add("'Constant value' cannot be empty.");
			else {
				final boolean valid = ParameterInfo.isValid(constDefField.getText().trim(), info.getType());
				if (!valid)
					errors.add("'Constant value' must be a valid " + getTypeText(info.getType()));
				errors.addAll(PlatformSettings.additionalParameterCheck(info, new String[] { constDefField.getText().trim() }, ParameterInfo.CONST_DEF));
			}
		} else if (listDef.isSelected()) {
			if (info.isEnum() || info instanceof MasonChooserParameterInfo) {
				final ListModel<Object> model = rightEnumValueList.getModel();
				if (model.getSize() == 0)
					errors.add("The right list cannot be empty."); //TODO: better error message
			} else {
				String text = listDefArea.getText().trim();
				if (text.equals("")) 
					errors.add("'List of values' cannot be empty.");
				else {
					text = text.replaceAll("[\\s]+"," ");
					final String[] elements = text.split(" ");
					boolean goodList = true;
					for (final String element : elements)
						goodList = goodList && ParameterInfo.isValid(element.trim(), info.getType());
					if (!goodList)
						errors.add("All elements of the list must be a valid " + getTypeText(info.getType()));
					errors.addAll(PlatformSettings.additionalParameterCheck(info, elements, ParameterInfo.LIST_DEF));
				}
			}
		} else {
			boolean s = false, e = false, st = false; 
			if (incrStartValueField.getText().trim().equals("")) {
				errors.add("'Start value' cannot be empty.");
				s = true;
			} else {
				final boolean valid = ParameterInfo.isValid(incrStartValueField.getText().trim(), info.getType());
				if (!valid)
					errors.add("'Start value' must be a valid " + getTypeText(info.getType()));
			}
			if (incrEndValueField.getText().trim().equals("")) {
				errors.add("'End value' cannot be empty.");
				e = true;
			} else {
				final boolean valid = ParameterInfo.isValid(incrEndValueField.getText().trim(), info.getType());
				if (!valid)
					errors.add("'End value' must be a valid " + getTypeText(info.getType()));
			}
			if (incrStepField.getText().trim().equals("")) {
				errors.add("'Step' cannot be empty.");
				st = true;
			} else {
				final double d = Double.valueOf(incrStepField.getText().trim());
				if (d == 0)
					errors.add("'Step' cannot be zero.");
				else {
					final boolean valid = ParameterInfo.isValid(incrStepField.getText().trim(), info.getType());
					if (!valid)
						errors.add("'Step' must be a valid " + getTypeText(info.getType()));
				}
			}
			if (!(s || e || st))
				errors.addAll(PlatformSettings.additionalParameterCheck(info,new String[] { incrStartValueField.getText().trim(), 
																							incrEndValueField.getText().trim(), 
																							incrStepField.getText().trim() }, ParameterInfo.INCR_DEF));
		}
		return errors.size() == 0 ? null : errors.toArray(new String[0]);
	}

	//------------------------------------------------------------------------------
	/** Returns the appropriate error message part according to the value of <code>type</code>. */
	String getTypeText(final String type) {
		if ("int".equals(type) || "Integer".equals(type))
			return "integer value.";
		if ("float".equals(type) || "Float".equals(type)
			|| "double".equals(type) || "Double".equals(type))
			return "real value.";
		if ("boolean".equals(type) || "Boolean".equals(type))
			return "boolean value (\"true\" or \"false\").";
		if ("String".equals(type))
			return "string that not contains any white space.";
		return type + ".";
	}

	//----------------------------------------------------------------------------------------------------
	private void updateNumberOfRuns() {
		final List<Integer> runNumbers = new ArrayList<Integer>(parameterTreeBranches.size());
		for (final ParameterCombinationGUI pGUI : parameterTreeBranches) {
			final int run = calculateNumberOfRuns(pGUI.combinationRoot);
			pGUI.updateRunDisplay(run);
			runNumbers.add(run);
		}
		
		int validMin = Integer.MAX_VALUE;
		for (final int runNumber : runNumbers) {
			if (runNumber > 1 && runNumber < validMin)
				validMin = runNumber;
		}
		
		for (int i = 0; i < parameterTreeBranches.size(); ++i) {
			final int runNumber = runNumbers.get(i);
			parameterTreeBranches.get(i).setWarning(runNumber > 1 && runNumber != validMin);
		}
	}


	//----------------------------------------------------------------------------------------------------
	public boolean needWarning() {
		if (parameterTreeBranches.isEmpty()) return false;
		
		for (final ParameterCombinationGUI pGUI : parameterTreeBranches) {
			if (pGUI.warningDisplay.getIcon() != null) return true;
		}
		
		return false;
	}
	
	//------------------------------------------------------------------------------
	/** Enables/disables the parameter editing components of the page according to
	 *  the value of <code>enabled</code>.
	 */
	private void enableDisableSettings(final boolean enabled) {
		runsField.setEnabled(enabled || PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.GLOBAL);
		constDef.setEnabled(enabled);
		listDef.setEnabled(enabled);
		incrDef.setEnabled(enabled);
		constDefField.setEnabled(enabled);
		listDefArea.setEnabled(enabled);
		incrStartValueField.setEnabled(enabled);
		incrEndValueField.setEnabled(enabled);
		incrStepField.setEnabled(enabled);
		enumDefBox.setEnabled(enabled);
		leftEnumValueList.setEnabled(enabled);
		rightEnumValueList.setEnabled(enabled);
		addEnumButton.setEnabled(enabled);
		removeEnumButton.setEnabled(enabled);
		fileTextField.setEnabled(enabled);
		browseFileButton.setEnabled(enabled);
		cancelButton.setEnabled(enabled);
		newParameterButton.setEnabled(PlatformSettings.getGUIControllerForPlatform().isNewParametersEnabled() && !enabled);
	}

	//------------------------------------------------------------------------------
	/** Resets the parameter editing related components. */
	private void resetSettings() { 
		changeText(null,null);
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL)
			runsField.setText(""); 
		constDefField.setText("");
		listDefArea.setText("");
		incrStartValueField.setText("");
		incrEndValueField.setText("");
		incrStepField.setText("");
		fileTextField.setText("");
		fileTextField.setToolTipText("");
		leftEnumValueList.setModel(new DefaultListModel<Object>());
		rightEnumValueList.setModel(new DefaultListModel<Object>());
		constDef.setSelected(true);
		final CardLayout cl = (CardLayout) rightMiddle.getLayout();
		cl.show(rightMiddle,"CONST");
	}

	//------------------------------------------------------------------------------
	/** Modifies the information object from the contents of the editing components of the page.<br>
	 *  Pre-condition: all input values are valid.
	 */
	private void modifyParameterInfo(final ParameterInfo info) { 
		info.clear();
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL) {
			if (runsField.getText().trim().equals("")) {
				if (constDef.isSelected())
					info.setRuns(1);
				else 
					throw new IllegalStateException("Runs field is empty.");
			} else {
				long i = Long.parseLong(runsField.getText().trim());
				info.setRuns(i);
			}
		}

		int defType = ParameterInfo.CONST_DEF;
		if (listDef.isSelected())
			defType = ParameterInfo.LIST_DEF;
		else if (incrDef.isSelected())
			defType = ParameterInfo.INCR_DEF;
		info.setDefinitionType(defType);
		switch (defType) {
		case ParameterInfo.CONST_DEF : {
			if (info.isEnum()) {
				info.setValue(enumDefBox.getSelectedItem());
			} else if (info instanceof MasonChooserParameterInfo) {
				info.setValue(enumDefBox.getSelectedIndex()); 
			} else if (info.isFile()) {
				info.setValue(ParameterInfo.getValue(fileTextField.getToolTipText(), info.getType()));
			} else {
				info.setValue(ParameterInfo.getValue(constDefField.getText().trim(), info.getType()));
			}
			
			break;
		}
		case ParameterInfo.LIST_DEF  : {
			if (info.isEnum()) { 
				final DefaultListModel<Object> model = (DefaultListModel<Object>) rightEnumValueList.getModel();
				List<Object> result = new ArrayList<Object>();
				for (int i = 0; i < model.size(); result.add(model.get(i++)));
				info.setValues(result);
			}
			else if (info instanceof MasonChooserParameterInfo) {
				final MasonChooserParameterInfo chooserInfo = (MasonChooserParameterInfo) info;
				final String[] validStrings = chooserInfo.getValidStrings();
				List<Object> result = new ArrayList<Object>();
				final DefaultListModel<Object> model = (DefaultListModel<Object>) rightEnumValueList.getModel();
				for (int i = 0; i < model.size(); ++i) {
					int idx = -1;
					for (int j = 0; j < validStrings.length; ++j) {
						if (validStrings[j].equals(model.get(i))) {
							idx = j;
							break;
						}
					}
					
					if (idx >= 0)
						result.add(idx);
				}
				info.setValues(result);
			} else {
				String text = listDefArea.getText().trim();
				text = text.replaceAll("[\\s]+"," ");
				final String[] elements = text.split(" ");
				final List<Object> list = new ArrayList<Object>(elements.length);
				for (final String element : elements) 
					list.add(ParameterInfo.getValue(element, info.getType()));
				info.setValues(list);
				break;
			}
		}
		case ParameterInfo.INCR_DEF  : info.setStartValue((Number)ParameterInfo.getValue(incrStartValueField.getText().trim(), info.getType()));
									   info.setEndValue((Number)ParameterInfo.getValue(incrEndValueField.getText().trim(), info.getType()));
									   info.setStep((Number)ParameterInfo.getValue(incrStepField.getText().trim(), info.getType()));
									   break;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public void actionPerformed(final ActionEvent e) {
		final String command = e.getActionCommand();

		if (ACTIONCOMMAND_EDIT.equals(command)){
			if (editedNode != null)
				cancelAllSelectionBut(null);
		} else if (command.equals("CONST")) {
			final DefaultMutableTreeNode node = editedNode;
			final ParameterInATree userObj = (ParameterInATree) node.getUserObject();
			final CardLayout cl = (CardLayout) rightMiddle.getLayout();
			if (userObj.info instanceof MasonChooserParameterInfo || userObj.info.isEnum()) {
				Object[] elements = null;
				if (userObj.info.isEnum()){
					@SuppressWarnings("unchecked")
					final Class<Enum<?>> type = (Class<Enum<?>>) userObj.info.getJavaType();
					elements = type.getEnumConstants();
				} else {
					final MasonChooserParameterInfo chooserInfo = (MasonChooserParameterInfo) userObj.info;
					elements = chooserInfo.getValidStrings();
				}
				final DefaultComboBoxModel<Object> model = (DefaultComboBoxModel<Object>) enumDefBox.getModel();
				model.removeAllElements();
				for (final Object object : elements) {
					model.addElement(object);
				}
				if (userObj.info.isEnum())
					enumDefBox.setSelectedItem(userObj.info.getValue());
				else 
					enumDefBox.setSelectedIndex((Integer)userObj.info.getValue());
				cl.show(rightMiddle,"ENUM");
			} else 
				cl.show(rightMiddle,"CONST");
		} else if (command.equals("LIST")) {
			final DefaultMutableTreeNode node = editedNode;
			final ParameterInATree userObj = (ParameterInATree) node.getUserObject();
			final CardLayout cl = (CardLayout) rightMiddle.getLayout();
			if (userObj.info instanceof MasonChooserParameterInfo || userObj.info.isEnum()) {
				Object[] elements = null;
				if (userObj.info.isEnum()){
					@SuppressWarnings("unchecked")
					final Class<Enum<?>> type = (Class<Enum<?>>) userObj.info.getJavaType();
					elements = type.getEnumConstants();
				} else {
					final MasonChooserParameterInfo chooserInfo = (MasonChooserParameterInfo) userObj.info;
					elements = chooserInfo.getValidStrings();
				}
				
				final DefaultListModel<Object> leftEnumModel = (DefaultListModel<Object>) leftEnumValueList.getModel();
				leftEnumModel.removeAllElements();
				for (final Object object : elements)
					leftEnumModel.addElement(object);
				
				final DefaultListModel<Object> rightEnumModel = (DefaultListModel<Object>) rightEnumValueList.getModel();
				rightEnumModel.removeAllElements();
				for (final Object object : userObj.info.getValues()) {
					if (userObj.info.isEnum())
						rightEnumModel.addElement(object);
					else {
						final MasonChooserParameterInfo chooserInfo = (MasonChooserParameterInfo) userObj.info;
						rightEnumModel.addElement(chooserInfo.getValidStrings()[(Integer) object]);
					}
				}
				cl.show(rightMiddle,"ENUMLIST");
			} else 
				cl.show(rightMiddle,"LIST");
		} else if (command.equals("INCREMENT")) {
			final CardLayout cl = (CardLayout) rightMiddle.getLayout();
			cl.show(rightMiddle,command);
		} else if (command.equals("CANCEL")) {
			resetSettings();
			enableDisableSettings(false);
			modifyButton.setEnabled(false);
			editedNode = null;
			editedTree = null;
			cancelAllSelectionBut(null);
			enableDisableParameterCombinationButtons();
		} else if (command.equals("RUNS_FIELD") && PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL)  
			constDef.grabFocus();
		else if (command.equals("CONST_FIELD") || command.equals("STEP_FIELD"))
			modifyButton.doClick();
		else if (command.equals("START_FIELD"))
			incrEndValueField.grabFocus();
		else if (command.equals("END_FIELD"))
			incrStepField.grabFocus();
		else if (ACTIONCOMMAND_ADD_BOX.equals(command)) 
			addBox();
		else if (ACTIONCOMMAND_ADD_ENUM.equals(command)) {
			final DefaultListModel<Object> model = (DefaultListModel<Object>) rightEnumValueList.getModel();
			
			for (final Object element : leftEnumValueList.getSelectedValuesList())
				model.addElement(element);
		} else if (ACTIONCOMMAND_REMOVE_ENUM.equals(command)) {
			final DefaultListModel<Object> model = (DefaultListModel<Object>) rightEnumValueList.getModel();
			
			for (final Object element : rightEnumValueList.getSelectedValuesList())
				model.removeElement(element);
		}
		else if (ACTIONCOMMAND_BROWSE.equals(command))
			chooseFile();
		else if (ACTIONCOMMAND_NEW_PARAMETERS.equals(command)) { 
			int result = Utilities.askUser(owner,false,"Warning","All user defined parameter settings will be lost.",
										   "Do you want to continue?");
			if (result == 1) { // yes
				List<RecordableInfo> candidates = null;
				try {
					candidates = owner.getModelInformation().getRecordables();
				} catch (ModelInformationException e1) {
					Utilities.userAlert(owner,"Error while collecting new parameter candidates.","Reason: " +
										Util.getLocalizedMessage(e1));
					e1.printStackTrace(ParameterSweepWizard.getLogStream());
					return;
				}
				NewParametersDialog dialog = new NewParametersDialog(ParameterSweepWizard.getFrame(),candidates,originalInitParamResult);
				result = dialog.showDialog();
				if (result == NewParametersDialog.OK_OPTION) {
					List<ParameterInfo> oldNewParameters = newParameters == null ? new ArrayList<ParameterInfo>() : newParameters; 
					newParameters = dialog.getNewParameterList();
					if (newParameters.size() == 0)
						newParameters = null;
					resetParamsweepGUI();
					parameters = createParameters();
					if (owner.getParameterFile() != null && owner.getParameterFile().exists()) {
						try {
							final DefaultMutableTreeNode root = PlatformSettings.parseParameterFile(parameters,owner.getParameterFile());
							setGlobalRunsField(root);
							initializePageFromTree(root);
						} catch (ParameterParserException | WizardLoadingException e1) {
							createDefaultParameterList(parameters);
							Utilities.userAlert(owner,"Cannot initialize from the defined parameter file.","Reason: " +
												Util.getLocalizedMessage(e1));
							e1.printStackTrace(ParameterSweepWizard.getLogStream());
						}
					} else
						createDefaultParameterList(parameters);
					enableDisableSettings(false);
					modifyButton.setEnabled(false);
					owner.reinitializeRecordableLists(newParameters == null ? oldNewParameters : 
																			  Utilities.listSubtract(oldNewParameters,newParameters));
					owner.cleanRecorders();
					updateNumberOfRuns();
				}
			}
		}

	}

	//----------------------------------------------------------------------------------------------------
	private void addBox() {
		final JPanel panel = createAParameterBox(false);
		combinationsPanel.add(panel);
		combinationsPanel.revalidate();

		combinationsScrPane.invalidate();
		combinationsScrPane.validate();
		final JScrollBar verticalScrollBar = combinationsScrPane.getVerticalScrollBar();
		verticalScrollBar.setValue(verticalScrollBar.getMaximum());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void chooseFile() {
		final JFileChooser fileDialog = new JFileChooser(!"".equals(fileTextField.getToolTipText()) ? fileTextField.getToolTipText() : 
																									  ParameterSweepWizard.getLastDir().getPath());
		if (!"".equals(fileTextField.getToolTipText()))
			fileDialog.setSelectedFile(new File(fileTextField.getToolTipText()));
		int dialogResult = fileDialog.showOpenDialog(container);
		if (dialogResult == JFileChooser.APPROVE_OPTION) {
			final File selectedFile = fileDialog.getSelectedFile();
			if (selectedFile != null) {
				ParameterSweepWizard.setLastDir(selectedFile.getAbsoluteFile().getParentFile());
				fileTextField.setText(selectedFile.getName());
				fileTextField.setToolTipText(selectedFile.getAbsolutePath());
			}
		}
	}
		
	//----------------------------------------------------------------------------------------------------
	private void cancelAllSelectionBut(JComponent exception) {
		if (exception != parameterList)
			parameterList.clearSelection();

		for (final ParameterCombinationGUI pGUI : parameterTreeBranches) {
			if (exception != pGUI.tree) 
				pGUI.tree.setSelectionPath(null);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private ParameterInfo findOriginalInfo(final ParameterInfo info) {
		for (final ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?> pInfo : batchParameters) {
			final ParameterInfo converted = InfoConverter.parameterInfo2ParameterInfo(pInfo);
			if (info.equals(converted)) {
				converted.setRuns(1); 
				return converted;
			}
		}
		
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private ParameterInfo findMatchedInfo(final ParameterInfo info) {
		ParameterInfo result = findOriginalInfo(info);
		
		if (result == null && newParameters != null) {
			for (final ParameterInfo candidate : newParameters) {
				if (info.equals(candidate))
					return candidate;
			}
		}
		
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void enableDisableParameterCombinationButtons() {
		final boolean selectionInTheList = parameterList.getSelectedIndex() >= 0; 
		
		for (ParameterCombinationGUI pGUI : parameterTreeBranches) {
			final boolean selectionInTheTree = pGUI.tree.getSelectionPath() != null;
			final boolean topLevelSelectionInTheTree = selectionInTheTree &&  
														pGUI.combinationRoot.isNodeChild((DefaultMutableTreeNode)pGUI.tree.getSelectionPath().getLastPathComponent()); 
					
			
			pGUI.addButton.setEnabled(selectionInTheList);
			pGUI.removeButton.getAction().setEnabled(topLevelSelectionInTheTree);
			pGUI.upButton.getAction().setEnabled(selectionInTheTree);
			pGUI.downButton.getAction().setEnabled(selectionInTheTree);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private String checkInfos() { 
		final DefaultListModel<AvailableParameter> listModel = (DefaultListModel<AvailableParameter>) parameterList.getModel();
		for (int i = 0;i < listModel.getSize();++i) {
			final AvailableParameter param = listModel.get(i);
			final String invalid = checkFileInfo(param);
			if (invalid != null)
				return invalid;
		}
		
		for (final ParameterCombinationGUI pGUI : parameterTreeBranches) {
			final DefaultMutableTreeNode root = pGUI.combinationRoot;
			@SuppressWarnings("rawtypes")
			final Enumeration nodes = root.breadthFirstEnumeration();
			nodes.nextElement(); // root
			while (nodes.hasMoreElements()) {
				final DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
				final ParameterInATree userObj = (ParameterInATree) node.getUserObject();
				final String invalid = checkFileInfo(userObj);
				if (invalid != null)
					return invalid;
			}
		}
		
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String checkFileInfo(final AvailableParameter parameter) {
		if (parameter.info.isFile()) {
			if (parameter.info.getValue() == null)
				return parameter.info.getName().replaceAll("([A-Z])"," $1");
		}
		
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	public DefaultMutableTreeNode createTreeFromParameterPage() {
		final DefaultMutableTreeNode result = new DefaultMutableTreeNode();
		
		final DefaultListModel<AvailableParameter> listModel = (DefaultListModel<AvailableParameter>) parameterList.getModel();
		for (int i = 0; i < listModel.getSize(); ++i) {
			final AvailableParameter param = listModel.get(i);
			result.add(new DefaultMutableTreeNode(param.info));
		}
		
		for (final ParameterCombinationGUI pGUI : parameterTreeBranches) {
			final DefaultMutableTreeNode root = pGUI.combinationRoot;
			@SuppressWarnings("rawtypes")
			final Enumeration nodes = root.breadthFirstEnumeration();
			nodes.nextElement(); // root;
			while (nodes.hasMoreElements()) {
				final DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
				final ParameterInATree userObj = (ParameterInATree) node.getUserObject();
				filterConstants(userObj, result);
			}
		}
		
		for (final ParameterCombinationGUI pGUI : parameterTreeBranches) {
			final DefaultMutableTreeNode root = pGUI.combinationRoot;
			@SuppressWarnings("rawtypes")
			final Enumeration nodes = root.preorderEnumeration();
			nodes.nextElement(); // root
			DefaultMutableTreeNode parent = result;
			while (nodes.hasMoreElements()) {
				final DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
				final ParameterInATree userObj = (ParameterInATree) node.getUserObject();
				if (userObj.info.getMultiplicity() > 1) {
					final DefaultMutableTreeNode parameterNode = new DefaultMutableTreeNode(userObj.info);
					parent.add(parameterNode);
					parent = parameterNode;
				}
			}
		}
		
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializePageFromTree(DefaultMutableTreeNode paramTree) throws WizardLoadingException {
		final DefaultListModel<AvailableParameter> model = new DefaultListModel<>();
		parameterList.setModel(model); 
		boolean useFirstBox = true;
		for (int i = 0; i < paramTree.getChildCount(); ++i) {
			final DefaultMutableTreeNode node = (DefaultMutableTreeNode) paramTree.getChildAt(i);
			final ParameterInfo embeddedInfo = (ParameterInfo) node.getUserObject();
			
			switch (checkIfConstant(embeddedInfo)) {
			case CONSTANT:
				final ParameterCombinationGUI pcGUI = parameterTreeBranches.get(0);
				final DefaultTreeModel treeModel = (DefaultTreeModel) pcGUI.tree.getModel();
				final DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
				final DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new ParameterInATree(embeddedInfo));
				treeModel.insertNodeInto(newNode, root, root.getChildCount());
				pcGUI.tree.expandPath(new TreePath(treeModel.getPathToRoot(root)));
				break;
			case DEFAULT_CONSTANT:
				model.addElement(new AvailableParameter(embeddedInfo));
				break;
			case NOT_CONSTANT:
				initializeAParameterBox(node,useFirstBox);
				useFirstBox = false;
				break;
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializeAParameterBox(final DefaultMutableTreeNode branchRootNode, final boolean useFirstBox) {
		if (!useFirstBox)
			addBox();
		final ParameterCombinationGUI pcGUI = parameterTreeBranches.get(parameterTreeBranches.size() - 1);
		final DefaultTreeModel treeModel = (DefaultTreeModel) pcGUI.tree.getModel();
		DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
		
		@SuppressWarnings("rawtypes")
		final Enumeration nodes = branchRootNode.breadthFirstEnumeration();
		while (nodes.hasMoreElements()) {
			final DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
			final ParameterInfo info = (ParameterInfo) node.getUserObject();
			
			final DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(new ParameterInATree(info));
			treeModel.insertNodeInto(newNode, root, root.getChildCount());
			pcGUI.tree.expandPath(new TreePath(treeModel.getPathToRoot(root)));
		}
	}

	//----------------------------------------------------------------------------------------------------
	private ConstantCheckResult checkIfConstant(ParameterInfo info) throws WizardLoadingException {
		if (info.isConstant()) {
			final ParameterInfo originalInfo = findMatchedInfo(info);
			if (originalInfo == null)
				throw new WizardLoadingException(true,"Unknown referenced parameter: " + info.getName());
			return isDefaultConstant(info,originalInfo) ? ConstantCheckResult.DEFAULT_CONSTANT : ConstantCheckResult.CONSTANT;
		}
		
		return ConstantCheckResult.NOT_CONSTANT;
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isDefaultConstant(final ParameterInfo info, final ParameterInfo defaultValueInfo)
	{
		if (!info.getJavaType().equals(defaultValueInfo.getJavaType())) {
			throw new IllegalStateException("isDefaultConstant(): Type does not match.");
		}
		
		if (info.isEnum())
			return info.getValue() == defaultValueInfo.getValue();
		if (info.isBoolean())
			return ((Boolean)info.getValue()).booleanValue() == ((Boolean)defaultValueInfo.getValue()).booleanValue();
		if (info.isNumeric()) {
			final Number num1 = (Number) info.getValue();
			final Number num2 = (Number) defaultValueInfo.getValue();
			return Utilities.numberEquals(num1,num2);
		}
		
		return info.getValue().equals(defaultValueInfo.getValue());
	}

	//----------------------------------------------------------------------------------------------------
	private void filterConstants(final ParameterInATree paramContainer, final DefaultMutableTreeNode root) {
		if (paramContainer.info.getMultiplicity() == 1) {
			transformToConstant(paramContainer.info);
			root.add(new DefaultMutableTreeNode(paramContainer.info));
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void transformToConstant(final ParameterInfo info) {
		if (info.isConstant())
			return;
		else if (info.getDefinitionType() == ParameterInfo.INCR_DEF) {
			info.setValue(info.getStartValue());
			info.setStartValue(null);
			info.setEndValue(null);
			info.setStep(null);
		}
		
		info.setDefinitionType(ParameterInfo.CONST_DEF);
	}

	//----------------------------------------------------------------------------------------------------
	private int calculateNumberOfRuns(final DefaultMutableTreeNode combinationRoot) {
		if (combinationRoot.getChildCount() == 0) 
			return 0;
		
		int result = 1;
		@SuppressWarnings("rawtypes")
		final Enumeration combinationElements = combinationRoot.children();
		
		while (combinationElements.hasMoreElements()) {
			final DefaultMutableTreeNode node = (DefaultMutableTreeNode) combinationElements.nextElement();
			final ParameterInfo info = ((ParameterInATree)node.getUserObject()).info;
			
			long run = 1;
			if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL && info.getMultiplicity() > 1 )
				run = info.getRuns();
			result *= run * info.getMultiplicity();
		}
		
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.GLOBAL) 
			result *= parseGlobalRuns(); 
		
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private long parseGlobalRuns() {
		long runs = 1;
		final String runsStr = runsField.getText().trim();
		if (runsStr != null && runsStr.length() > 0) {
			try {
				runs = Long.parseLong(runsStr);
			} catch (NumberFormatException e) {}
		}
		return runs;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setRunsIfNeccessary(final DefaultMutableTreeNode root) {
		long runs = 1;
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.GLOBAL)
			runs = parseGlobalRuns();
		ParameterEnumeration pe = new ParameterEnumeration(root);
		while (pe.hasMoreElements()) {
			DefaultMutableTreeNode node = pe.nextElement();
			if (root.equals(node)) continue;
			ParameterInfo info = (ParameterInfo) node.getUserObject();
			info.setRuns(runs);
		}
	}
	
	
	//----------------------------------------------------------------------------------------------------
	public void transformIncrementsIfNeed(final DefaultMutableTreeNode root) {
		ParameterEnumeration pe = new ParameterEnumeration(root);
		for (DefaultMutableTreeNode node = pe.nextElement();pe.hasMoreElements();node = pe.nextElement()) {
			if (root.equals(node)) continue;
			ParameterInfo info = (ParameterInfo) node.getUserObject();
			if (info.getDefinitionType() == ParameterInfo.INCR_DEF)
				info.transformIfNeed();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializePageForPlatform() {
		IGUIController controller = PlatformSettings.getGUIControllerForPlatform();
		newParameterButton.setEnabled(controller.isNewParametersEnabled()); 
		newParameterButtonCopy.setEnabled(controller.isNewParametersEnabled());
		newParameterButton.setVisible(controller.isNewParametersEnabled()); 
		newParameterButtonCopy.setVisible(controller.isNewParametersEnabled());

		switch (controller.getRunOption()) {
		case NONE 	: runsLabel.setVisible(false);
					  runsField.setVisible(false);
					  break;
		case GLOBAL	: runsLabel.setText("Runs (global): ");
		default: // do nothing
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void setGlobalRunsField(DefaultMutableTreeNode root) {
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.GLOBAL) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(0);
			if (node != null) {
				ParameterInfo pi = (ParameterInfo) node.getUserObject();
				if (pi != null)
					runsField.setText(String.valueOf(pi.getRuns()));
			}
		}
	}

	//----------------------------------------------------------------------------------------------------
	public JButton getNewParametersButton() { return newParameterButtonCopy; }
	
	//====================================================================================================
	// nested classes

	//----------------------------------------------------------------------------------------------------
	private class IntelliContext extends HashMap<Object, Object> implements IIntelliContext {
		{
			put( "scriptSupport", owner.getRecordersPage().getScriptSupport() );
		}
        private static final long serialVersionUID = -7383965550748377433L;
		public List<ParameterInfo> getParameters() { return parameters; }
		public DefaultMutableTreeNode getParameterTreeRootNode() { return createTreeFromParameterPage(); } 
		public JButton getNewParametersButton() { return Page_ParametersV2.this.getNewParametersButton(); } 
		public File getPluginResourcesDirectory() { return new File(System.getProperty("user.dir") + "/resources/Plugins"); }
	}

	//----------------------------------------------------------------------------------------------------
	private enum ConstantCheckResult {
		CONSTANT, DEFAULT_CONSTANT, NOT_CONSTANT;
	}
}