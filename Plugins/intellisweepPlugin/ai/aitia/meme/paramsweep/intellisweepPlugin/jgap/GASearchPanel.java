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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jdesktop.swingx.JXLabel;

import ai.aitia.meme.Logger;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.gui.component.CheckList;
import ai.aitia.meme.paramsweep.gui.component.CheckListModel;
import ai.aitia.meme.paramsweep.gui.component.ListAsATree;
import ai.aitia.meme.paramsweep.gui.info.MasonChooserParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.GASearchPanelModel.FitnessFunctionDirection;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.GASearchPanelModel.ModelListener;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.IGAConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.IGAOperatorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator.IGASelectorConfigurator;
import ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.gene.ParameterOrGene;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.GeneInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.FormsUtils.Separator;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;

import com.jgoodies.forms.layout.CellConstraints;

/**
 * @author Tamás Máhr
 * @author Rajmund Bocsi
 *
 */
public class GASearchPanel extends JPanel {
	
	//====================================================================================================
	// members
   
	private static final long serialVersionUID = 7957280032992002080L;
	
	private static final String GENE_SETTINGS_INT_RANGE_VALUE_PANEL = "int_range";
	private static final String GENE_SETTINGS_DOUBLE_RANGE_VALUE_PANEL = "double_range";
	private static final String GENE_SETTINGS_LIST_VALUE_PANEL = "list";
	private static final String PARAM_SETTINGS_CONSTANT_VALUE_PANEL = "constant";
	private static final String PARAM_SETTINGS_ENUM_VALUE_PANEL = "enum";
	private static final String PARAM_SETTINGS_FILE_VALUE_PANEL = "file";
	
	private static final String OPERATOR_SETTINGS_PANEL_DEFAULT_PANEL = "DEFAULT";
	private static final String OPERATOR_SETTINGS_PANEL_TITLE_POSTFIX = "Operator settings";
	
	private static final NumberFormat numberFormatter = NumberFormat.getInstance(Locale.US);
	
	static { // print double numbers rounded to 2 decimal digits using US format
		numberFormatter.setMaximumFractionDigits(2);
		numberFormatter.setMinimumFractionDigits(0);
		numberFormatter.setGroupingUsed(false);
	}
	
	private JSpinner populationSizeField;
	private JTextField populationRandomSeedField;
	private JRadioButton numberOfGenerationsRadioButton;
	private JSpinner numberOfGenerationsField;
	private JRadioButton fitnessLimitRadioButton;
	private JTextField fitnessLimitField;
	private JRadioButton fitnessMinimizeRadioButton;
	private JRadioButton fitnessMaximizeRadioButton;
	private JComboBox<RecordableInfo> fitnessFunctionList;
	private JTree geneTree;
	private JLabel numberLabel;
	protected Popup errorPopup;
	private JTextField constantField;
	private JTextField intMinField;
	private JTextField intMaxField;
	private JTextField doubleMinField;
	private JTextField doubleMaxField;
	private JTextArea valuesArea;
	private JComboBox<Object> enumDefBox;
	private JTextField fileTextField;
	private JButton fileBrowseButton;

	private JButton localNewParametersButton;
	private JButton remoteNewParametersButton;

	private JRadioButton constantRButton;
	private JRadioButton geneRButton;
	private JRadioButton doubleGeneValueRangeRButton;
	private JRadioButton intGeneValueRangeRButton;
	private JRadioButton listGeneValueRButton;
	private JPanel geneSettingsValuePanel;

	private JButton modifyButton;
	private JButton cancelButton;

	private JScrollPane mainScrPane;
	private JPanel operatorSettingsPanel;
	private CheckList selectorList;
	private CheckList operatorList;

	private DefaultMutableTreeNode editedNode;

	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public GASearchPanel(final GASearchPanelModel model, final JButton remoteNewParametersButton) {
		this.remoteNewParametersButton = remoteNewParametersButton;
		initSearchPanel(model);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void reset(final GASearchPanelModel model) {
		populationSizeField.setValue(model.getPopulationSize());
		populationRandomSeedField.setText(String.valueOf(model.getPopulationRandomSeed()));
		
		// create the stopping conditions panel
		numberOfGenerationsRadioButton.setSelected(true);
		numberOfGenerationsField.setValue(model.getNumberOfGenerations());
		fitnessLimitField.setText(String.valueOf(model.getFitnessLimitCriterion()));
		
		if (model.getNumberOfGenerations() < 0) {
			fitnessLimitRadioButton.setSelected(true);
			numberOfGenerationsField.setEnabled(false);
		} else {
			fitnessLimitField.setEnabled(false);
			numberOfGenerationsRadioButton.setSelected(true);
		}
		
		if (model.getFitnessFunctionDirection() == FitnessFunctionDirection.MINIMIZE) 
			fitnessMinimizeRadioButton.setSelected(true);
		else 
			fitnessMaximizeRadioButton.setSelected(true);
//		fitnessFunctionList.setSelectedItem(model.getSelectedFitnessFunction()); // set fitness function later
		
		final CardLayout settingslayout = (CardLayout) operatorSettingsPanel.getLayout();
		settingslayout.show(operatorSettingsPanel,OPERATOR_SETTINGS_PANEL_DEFAULT_PANEL);
		((TitledBorder)operatorSettingsPanel.getBorder()).setTitle(OPERATOR_SETTINGS_PANEL_TITLE_POSTFIX);
		
		final List<IGASelectorConfigurator> selectionOperators = model.getSelectionOperators();
		final List<IGASelectorConfigurator> selectedSelectionOperators = model.getSelectedSelectionOperators();
		
		int i = 0;
		for (final IGASelectorConfigurator selectionOperator : selectionOperators) {
			selectorList.setChecked(i,selectedSelectionOperators.contains(selectionOperator));
			++i;
		}
		selectorList.clearSelection();

		final List<IGAOperatorConfigurator> geneticOperators = model.getGeneticOperators();
		final List<IGAOperatorConfigurator> selectedGeneticOperators = model.getSelectedGeneticOperators();

		i = 0;
		for (final IGAOperatorConfigurator operator : geneticOperators){
			operatorList.setChecked(i,selectedGeneticOperators.contains(operator));
			++i;
		}
		operatorList.clearSelection();

		numberLabel.setText("<html><b>Number of genes:</b> 0</html>");
		
		for (int j = 0;j < geneTree.getRowCount();++j) 
			geneTree.expandRow(j);
		
		resetSettings();
		enableDisableSettings(false);
		localNewParametersButton.setVisible(PlatformSettings.getGUIControllerForPlatform().isNewParametersEnabled());
		updateNumberOfGenes();
	}
	
	//====================================================================================================
	// assistant methods
		
	//----------------------------------------------------------------------------------------------------
	private void initSearchPanel(final GASearchPanelModel model) {
		model.addModelListener(new ModelListener() {
			
			//====================================================================================================
			// methods
			
			//----------------------------------------------------------------------------------------------------
			public void fitnessFunctionAdded() {
				fitnessFunctionList.removeAllItems();
				final List<RecordableInfo> list = model.getFitnessFunctions();
				for (final RecordableInfo fitnessFunction : list) {
					fitnessFunctionList.addItem(fitnessFunction);
				}				
			}
			
			//----------------------------------------------------------------------------------------------------
			public void fitnessFunctionsRemoved() {
				fitnessFunctionList.removeAllItems();
			}

			//----------------------------------------------------------------------------------------------------
			public void fitnessFunctionSelected(final RecordableInfo selectedFunction) {
				fitnessFunctionList.setSelectedItem(selectedFunction);
			}
			
			//----------------------------------------------------------------------------------------------------
			public void parameterAdded() {
				final DefaultTreeModel treeModel = model.getChromosomeTree();
				final DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
 				geneTree.expandPath(new TreePath(treeModel.getPathToRoot(root)));
			}

			//----------------------------------------------------------------------------------------------------
			public void parametersRemoved() {
				parameterAdded(); // :)
			}
		});
		
		// create the population parameters panel
		populationSizeField = new JSpinner(new SpinnerNumberModel(model.getPopulationSize(),2,Integer.MAX_VALUE,1));
		populationSizeField.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				model.setPopulationSize((Integer)((JSpinner)e.getSource()).getValue());
			}
		});
		
		populationRandomSeedField = new JTextField(String.valueOf(model.getPopulationRandomSeed()));
		populationRandomSeedField.setInputVerifier(new InputVerifier() {
			@Override
			public boolean verify(final JComponent input) {
				if (errorPopup != null) {
					errorPopup.hide();
					errorPopup = null;
				}
				
				try {
					final int value = Integer.parseInt(populationRandomSeedField.getText());
					model.setPopulationRandomSeed(value);
					return true;
				} catch (final NumberFormatException e) {
					final PopupFactory popupFactory = PopupFactory.getSharedInstance();
					final Point locationOnScreen = populationRandomSeedField.getLocationOnScreen();
					final JLabel message = new JLabel("Please specify an integer number!");
					message.setBorder(new LineBorder(Color.RED, 2, true));
					errorPopup = popupFactory.getPopup(populationRandomSeedField, message, locationOnScreen.x - 10, locationOnScreen.y - 30);
					errorPopup.show();
					
					return false;					
				}
			}
		});
		
		final JPanel populationPanel = FormsUtils.build("p ~ f:p:g", 
				"01 ||"+
				"23", 
				"Size",populationSizeField,
				"Random seed", populationRandomSeedField).getPanel();
		populationPanel.setBorder(BorderFactory.createTitledBorder(null,"Population",TitledBorder.LEADING,TitledBorder.BELOW_TOP));
		
		// create the stopping conditions panel
		numberOfGenerationsRadioButton = new JRadioButton("Number of generations");
		numberOfGenerationsField = new JSpinner(new SpinnerNumberModel(model.getNumberOfGenerations(),1,Integer.MAX_VALUE,1));
		numberOfGenerationsField.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				model.setNumberOfGenerations((Integer)((JSpinner)e.getSource()).getValue());				
			}
		});
		numberOfGenerationsRadioButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				if (numberOfGenerationsRadioButton.isSelected()) {
					numberOfGenerationsField.setEnabled(true);
					model.setFixNumberOfGenerations(true);
				} else 
					numberOfGenerationsField.setEnabled(false);
			}
		});
		
		fitnessLimitRadioButton = new JRadioButton("Fitness limit");
		fitnessLimitField = new JTextField(String.valueOf(model.getFitnessLimitCriterion()));
		fitnessLimitField.setInputVerifier(new InputVerifier() {
			@Override
			public boolean verify(final JComponent input) {
				if (errorPopup != null){
					errorPopup.hide();
					errorPopup = null;
				}

				try {
					final double value = Double.parseDouble(fitnessLimitField.getText());
					if (value < 0)
						throw new NumberFormatException();
					model.setFitnessLimitCriterion(value);
					return true;
				} catch (final NumberFormatException e) {
					final PopupFactory popupFactory = PopupFactory.getSharedInstance();
					final Point locationOnScreen = fitnessLimitField.getLocationOnScreen();
					final JLabel message = new JLabel("Please specify a non-negative (possibly floating point) number!");
					message.setBorder(new LineBorder(Color.RED, 2, true));
					errorPopup = popupFactory.getPopup(fitnessLimitField, message, locationOnScreen.x - 10, locationOnScreen.y - 30);
					errorPopup.show();
					
					return false;
				}
			}
		});
		fitnessLimitRadioButton.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(final ChangeEvent e) {
				if (fitnessLimitRadioButton.isSelected()) {
					fitnessLimitField.setEnabled(true);
					model.setFixNumberOfGenerations(false);
				} else 
					fitnessLimitField.setEnabled(false);
			}
		});
		
		GUIUtils.createButtonGroup(numberOfGenerationsRadioButton,fitnessLimitRadioButton);
		
		if (model.getNumberOfGenerations() < 0) {
			fitnessLimitRadioButton.setSelected(true);
			numberOfGenerationsField.setEnabled(false);
			model.setFixNumberOfGenerations(false);
		} else {
			fitnessLimitField.setEnabled(false);
			numberOfGenerationsRadioButton.setSelected(true);
			model.setFixNumberOfGenerations(true);
		}
		
		final JPanel stoppingPanel = FormsUtils.build("p ~ f:p:g", 
				"01 |"+
				"23", 
				numberOfGenerationsRadioButton,numberOfGenerationsField,
				fitnessLimitRadioButton,fitnessLimitField).getPanel();
		stoppingPanel.setBorder(BorderFactory.createTitledBorder(null,"Stopping",TitledBorder.LEADING,TitledBorder.BELOW_TOP));
		
		// create the fitness function panel
		fitnessMinimizeRadioButton = new JRadioButton("Minimize");
		fitnessMaximizeRadioButton = new JRadioButton("Maximize");
		
		GUIUtils.createButtonGroup(fitnessMinimizeRadioButton,fitnessMaximizeRadioButton);
		GUIUtils.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (fitnessMinimizeRadioButton.isSelected())
					model.setFitnessFunctionDirection(FitnessFunctionDirection.MINIMIZE);
				else if (fitnessMaximizeRadioButton.isSelected())
					model.setFitnessFunctionDirection(FitnessFunctionDirection.MAXIMIZE);
			}
		},fitnessMinimizeRadioButton,fitnessMaximizeRadioButton);
		
		if (model.getFitnessFunctionDirection() == FitnessFunctionDirection.MINIMIZE) 
			fitnessMinimizeRadioButton.setSelected(true);
		else 
			fitnessMaximizeRadioButton.setSelected(true);
		
		final JPanel fitnessButtonsPanel = new JPanel();
		fitnessButtonsPanel.add(fitnessMinimizeRadioButton);
		fitnessButtonsPanel.add(fitnessMaximizeRadioButton);
		
		fitnessFunctionList = new JComboBox<RecordableInfo>(model.getFitnessFunctions().toArray(new RecordableInfo[0]));
		fitnessFunctionList.setSelectedItem(model.getSelectedFitnessFunction());
		fitnessFunctionList.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(final ItemEvent e) {
				if (ItemEvent.SELECTED == e.getStateChange())
					model.setSelectedFitnessFunction((RecordableInfo) e.getItem());
			}
		});
		
		final JXLabel fitnessMessage = new JXLabel("Please specify a function that always returns with non-negative value!");
		fitnessMessage.setLineWrap(true);
		final JPanel fitnessPanel = FormsUtils.build("p ~ f:p:g", 
				"00 ||" +
				"12 ||" +
				"33",
				fitnessButtonsPanel,
				"Fitness function", fitnessFunctionList,
				fitnessMessage).getPanel();
		fitnessPanel.setBorder(BorderFactory.createTitledBorder(null,"Fitness",TitledBorder.LEADING,TitledBorder.BELOW_TOP));
		
		operatorSettingsPanel = new JPanel(new CardLayout());
		operatorSettingsPanel.setBorder(BorderFactory.createTitledBorder(null,OPERATOR_SETTINGS_PANEL_TITLE_POSTFIX,TitledBorder.LEADING,TitledBorder.BELOW_TOP));
		operatorSettingsPanel.add(new JLabel(""), OPERATOR_SETTINGS_PANEL_DEFAULT_PANEL);
		
		final List<IGASelectorConfigurator> selectionOperators = model.getSelectionOperators();
		final List<IGASelectorConfigurator> selectedSelectionOperators = model.getSelectedSelectionOperators();
		final CheckListModel selectorListModel = new CheckListModel(selectionOperators);
		selectorList = new CheckList(selectorListModel);
		selectorList.setBorder(BorderFactory.createTitledBorder(null,"Selection",TitledBorder.LEADING,TitledBorder.BELOW_TOP));
		selectorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		int i = 0;
		for (final IGASelectorConfigurator selectionOperator : selectionOperators) {
			final JPanel settingspanel = selectionOperator.getSettingsPanel();
			operatorSettingsPanel.add(settingspanel,selectionOperator.getName());
			
			if (selectedSelectionOperators.contains(selectionOperator))
				selectorList.setChecked(i, true);
			
			++i;
		}
		
		selectorList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				final IGASelectorConfigurator operator = (IGASelectorConfigurator) selectorList.getSelectedValue();
				showOperatorDetails(operatorSettingsPanel, operator);
			}
		});
		
		selectorListModel.addListDataListener(new ListDataListener() {
			
			//====================================================================================================
			// methods
			
			//----------------------------------------------------------------------------------------------------
			@Override public void intervalRemoved(final ListDataEvent e) {}
			@Override public void intervalAdded(final ListDataEvent e) {}
			
			//----------------------------------------------------------------------------------------------------
			@Override
			public void contentsChanged(final ListDataEvent e) {
				final IGASelectorConfigurator operator = (IGASelectorConfigurator) selectorList.getSelectedValue();
				
				if (selectorList.getModel().getCheckedState(e.getIndex0()))
					model.setSelectedSelectionOperator(operator);
				else 
					model.unsetSelectedSelectionOperator(operator);
			}
		});

		final List<IGAOperatorConfigurator> geneticOperators = model.getGeneticOperators();
		final List<IGAOperatorConfigurator> selectedGeneticOperators = model.getSelectedGeneticOperators();
		final CheckListModel operatorListModel = new CheckListModel(geneticOperators);
		operatorList = new CheckList(operatorListModel);
		operatorList.setBorder(BorderFactory.createTitledBorder(null,"Genetic operators",TitledBorder.LEADING,TitledBorder.BELOW_TOP));
		operatorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		i = 0;
		for (final IGAOperatorConfigurator operator : geneticOperators){
			final JPanel settingsPanel = operator.getSettingsPanel();
			operatorSettingsPanel.add(settingsPanel,operator.getName());
			
			if (selectedGeneticOperators.contains(operator))
				operatorList.setChecked(i, true);
			
			++i;
		}

		operatorList.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent e) {
				final IGAOperatorConfigurator operator = (IGAOperatorConfigurator) operatorList.getSelectedValue();
				showOperatorDetails(operatorSettingsPanel, operator);
			}
		});
		
		operatorListModel.addListDataListener(new ListDataListener() {
			
			//====================================================================================================
			// methods
			
			//----------------------------------------------------------------------------------------------------
			@Override public void intervalRemoved(final ListDataEvent e) {}
			@Override public void intervalAdded(final ListDataEvent e) {}
			
			//----------------------------------------------------------------------------------------------------
			@Override
			public void contentsChanged(final ListDataEvent e) {
				final IGAOperatorConfigurator operator = (IGAOperatorConfigurator) operatorList.getSelectedValue();
				
				if (operatorList.getModel().getCheckedState(e.getIndex0()))
					model.setSelectedGeneticOperator(operator);
				else
					model.unsetSelectedGeneticOperator(operator);
			}
		});		
		
		final JPanel gaSettingsPanel = FormsUtils.build("f:p:g", 
				"0||" +
				"1||" +
				"2||" +
				"3||" +
				"4 f:p:g", 
				populationPanel,
				stoppingPanel,
				fitnessPanel,
				selectorList,
				operatorList).getPanel();
		
		// the chromosome panel
	    final JScrollPane treeScrPane = new JScrollPane();
	    geneTree = new ListAsATree(model.getChromosomeTree()); 
		geneTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		geneTree.setCellRenderer(new ChromosomeTreeRenderer());
		
		geneTree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(final TreeSelectionEvent e) {
				final TreePath selectionPath = geneTree.getSelectionPath();
				
				boolean success = true;
				if (editedNode != null && (selectionPath == null || !editedNode.equals(selectionPath.getLastPathComponent()))) 
					success = modify();
				
				final DefaultTreeModel treeModel = model.getChromosomeTree();
				if (success) {
					if (selectionPath != null) {
						final DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
						final ParameterOrGene userObj = (ParameterOrGene) node.getUserObject();
						
						if (!node.equals(editedNode)) {
							if (!node.isRoot() && selectionPath.getPathCount() == treeModel.getPathToRoot(node).length) {
								editedNode = node;
								edit(userObj);
							} else {
								editedNode = null;
								geneTree.setSelectionPath(null);
								resetSettings();
								enableDisableSettings(false);
							}
						}
					} else {
						editedNode = null;
						resetSettings();
						enableDisableSettings(false);
					}
				} else 
					geneTree.setSelectionPath(new TreePath(treeModel.getPathToRoot(editedNode)));
				
			}
		});

	    treeScrPane.setViewportView(geneTree);
	    treeScrPane.setBorder(null);
	    treeScrPane.setViewportBorder(null);
		
		numberLabel = new JLabel("<html><b>Number of genes:</b> 0</html>");
		final JPanel treePanel = FormsUtils.build("~ f:p:g",
				  "0||" +
				  "1||" +
				  "2 f:p:g", 
				  numberLabel,
				  new Separator(""),
				  treeScrPane).getPanel(); 

		treePanel.setBorder(BorderFactory.createTitledBorder(""));
		
		geneSettingsValuePanel = new JPanel(new CardLayout());

		constantRButton = new JRadioButton("Constant");
		geneRButton = new JRadioButton("Gene");
		doubleGeneValueRangeRButton = new JRadioButton("Double value range");
		intGeneValueRangeRButton = new JRadioButton("Integer value range");
		listGeneValueRButton = new JRadioButton("Value list");
		GUIUtils.createButtonGroup(constantRButton,geneRButton,doubleGeneValueRangeRButton,intGeneValueRangeRButton,listGeneValueRButton);

		constantField = new JTextField();
		constantField.setActionCommand("CONST_FIELD");
		
		final JPanel constantPanel = FormsUtils.build("p ~ p:g",
								 		"[DialogBorder]01 p",
								 		"Constant value",constantField).getPanel();

		
		enumDefBox = new JComboBox<Object>(new DefaultComboBoxModel<Object>());
		
		final JPanel enumDefPanel = FormsUtils.build("p ~ p:g", 
								"[DialogBorder]01 p", 
								"Constant value",enumDefBox).getPanel();
		
		fileTextField = new JTextField();
		fileTextField.setActionCommand("FILE_FIELD");
		fileTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(final KeyEvent e) { 
				final char character = e.getKeyChar();
				final File file = new File(Character.isISOControl(character) ? fileTextField.getText() : fileTextField.getText() + character);
				fileTextField.setToolTipText(file.getAbsolutePath());
			}
		});
		fileBrowseButton = new JButton(FileSystemView.getFileSystemView().getSystemIcon(new File(".")));
		fileBrowseButton.setActionCommand("FILE_BROWSE");
		
		final JPanel fileDefPanel = FormsUtils.build("p ~ p:g ' p",
									   "[DialogBorder]012",
									   "File",fileTextField,fileBrowseButton).getPanel();

		intMinField = new JTextField();
		intMinField.setActionCommand("INT_MIN");
		intMaxField = new JTextField();
		intMaxField.setActionCommand("INT_MAX");
		doubleMinField = new JTextField();
		doubleMinField.setActionCommand("D_MIN");
		doubleMaxField = new JTextField();
		doubleMaxField.setActionCommand("D_MAX");
		valuesArea = new JTextArea();
		valuesArea.setToolTipText("Please specify ' ' (space) separated values!");
		valuesArea.setLineWrap(true);
		valuesArea.setWrapStyleWord(true);
		final JScrollPane valuesDefScr = new JScrollPane(valuesArea,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		final JPanel doublePanel = FormsUtils.build("p ' f:p:g",
									  "[DialogBorder]01 p ||" +
											  		"23 p",
											  		"Minimum value",doubleMinField,
											  		"Maximum value",doubleMaxField).getPanel();

		final JPanel intPanel = FormsUtils.build("p ' f:p:g",
								   "[DialogBorder]01 p ||" +
										   		 "23 p",
										   		 "Minimum value",intMinField,
										   		 "Maximum value",intMaxField).getPanel();
		
		final JPanel valuesPanel = FormsUtils.build("p ' p:g",
									  "[DialogBorder]01 f:p:g",
								      "Value list",CellConstraints.TOP,valuesDefScr).getPanel();
		
		final JPanel emptyPanel = new JPanel();
		
		geneSettingsValuePanel.add(emptyPanel,"NULL");
		geneSettingsValuePanel.add(constantPanel,PARAM_SETTINGS_CONSTANT_VALUE_PANEL);
		geneSettingsValuePanel.add(enumDefPanel,PARAM_SETTINGS_ENUM_VALUE_PANEL);
		geneSettingsValuePanel.add(fileDefPanel,PARAM_SETTINGS_FILE_VALUE_PANEL);
		geneSettingsValuePanel.add(doublePanel,GENE_SETTINGS_DOUBLE_RANGE_VALUE_PANEL);
		geneSettingsValuePanel.add(intPanel,GENE_SETTINGS_INT_RANGE_VALUE_PANEL);
		geneSettingsValuePanel.add(valuesPanel,GENE_SETTINGS_LIST_VALUE_PANEL);
		
		final ItemListener itemListener = new ItemListener() {
			@Override
			public void itemStateChanged(final ItemEvent _) {
				if (editedNode != null) {
					final CardLayout layout = (CardLayout) geneSettingsValuePanel.getLayout();
					if (doubleGeneValueRangeRButton.isSelected())
						layout.show(geneSettingsValuePanel,GENE_SETTINGS_DOUBLE_RANGE_VALUE_PANEL);
					else if (intGeneValueRangeRButton.isSelected())
						layout.show(geneSettingsValuePanel,GENE_SETTINGS_INT_RANGE_VALUE_PANEL);
					else if (listGeneValueRButton.isSelected())
						layout.show(geneSettingsValuePanel,GENE_SETTINGS_LIST_VALUE_PANEL);
					else if (constantRButton.isSelected()) {
						final ParameterOrGene param = (ParameterOrGene) editedNode.getUserObject();
						if (param.getInfo().isEnum() || param.getInfo() instanceof MasonChooserParameterInfo)
							layout.show(geneSettingsValuePanel,PARAM_SETTINGS_ENUM_VALUE_PANEL);
						else if (param.getInfo().isFile()) 
							layout.show(geneSettingsValuePanel,PARAM_SETTINGS_FILE_VALUE_PANEL);
						else
							layout.show(geneSettingsValuePanel,PARAM_SETTINGS_CONSTANT_VALUE_PANEL);
					} else if (geneRButton.isSelected()) {
						layout.show(geneSettingsValuePanel, "NULL");
					}
				} else {
					resetSettings();
					enableDisableSettings(false);
				}
			}
		};
		constantRButton.addItemListener(itemListener);
		geneRButton.addItemListener(itemListener);
		doubleGeneValueRangeRButton.addItemListener(itemListener);
		intGeneValueRangeRButton.addItemListener(itemListener);
		listGeneValueRButton.addItemListener(itemListener);
		
		final JPanel selectPanel = FormsUtils.build("f:p:g",
													"0||" +
													"1||" +
													"2||" +
													"3||" +
													"4",
													constantRButton,
													geneRButton,
													doubleGeneValueRangeRButton,
													intGeneValueRangeRButton,
													listGeneValueRButton ).getPanel();
		
		final JPanel buttonsPanel = new JPanel();
		modifyButton = new JButton("Modify");
		modifyButton.setActionCommand("EDIT");
		cancelButton = new JButton("Cancel");
		cancelButton.setActionCommand("CANCEL");
		GUIUtils.addActionListener(new ActionListener() {
			
			//====================================================================================================
			// methods
			
			//----------------------------------------------------------------------------------------------------
			@Override
			public void actionPerformed(ActionEvent e) {
				final String cmd = e.getActionCommand();
				if ("EDIT".equals(cmd)) 
					edit();
				else if ("CANCEL".equals(cmd)) 
					cancel();
				else if ("CONST_FIELD".equals(cmd) || "INT_MAX".equals(cmd) || "D_MAX".equals(cmd))
					modifyButton.doClick();
				else if ("INT_MIN".equals(cmd))
					intMaxField.grabFocus();
				else if ("D_MIN".equals(cmd))
					doubleMaxField.grabFocus();
				else if ("FILE_BROWSE".equals(cmd)) 
					chooseFile();
			}
			
			//----------------------------------------------------------------------------------------------------
			private void edit() {
				if (editedNode != null)
					geneTree.setSelectionPath(null);
			}
			
			//----------------------------------------------------------------------------------------------------
			private void cancel() {
				resetSettings();
				enableDisableSettings(false);
				editedNode = null;
				geneTree.setSelectionPath(null);
			}
			
			//----------------------------------------------------------------------------------------------------
			private void chooseFile() {
				final JFileChooser fileDialog = new JFileChooser(!"".equals(fileTextField.getToolTipText()) ? fileTextField.getToolTipText() :
					ParameterSweepWizard.getLastDir().getPath());
				if (!"".equals(fileTextField.getToolTipText()))
					fileDialog.setSelectedFile(new File(fileTextField.getToolTipText()));
				int dialogResult = fileDialog.showOpenDialog(GASearchPanel.this);
				if (dialogResult == JFileChooser.APPROVE_OPTION) {
					final File selectedFile = fileDialog.getSelectedFile();
					if (selectedFile != null) {
						ParameterSweepWizard.setLastDir(selectedFile.getAbsoluteFile().getParentFile());
						fileTextField.setText(selectedFile.getName());
						fileTextField.setToolTipText(selectedFile.getAbsolutePath());
					}
				}
			}
		},modifyButton,cancelButton,constantField,fileTextField,intMinField,intMaxField,doubleMinField,doubleMaxField,fileBrowseButton);
		buttonsPanel.add(modifyButton);
		buttonsPanel.add(cancelButton);
		
		final JPanel geneSetterPanel = FormsUtils.build("f:p:g",
														"0||" +
														"1 f:p:g||" +
														"2 p",
														selectPanel,
														geneSettingsValuePanel,
														buttonsPanel).getPanel();
		
		geneSetterPanel.setBorder(BorderFactory.createTitledBorder(null,"Gene/parameter settings",TitledBorder.LEADING,TitledBorder.BELOW_TOP));
		
		localNewParametersButton = new JButton("Add new parameters...");
		localNewParametersButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				if (remoteNewParametersButton != null) {
					remoteNewParametersButton.doClick();
					model.addParametersToChromosomeTree();
					updateNumberOfGenes();
				} else {
					Utilities.userAlert(mainScrPane,"Sorry, this feature is not available.");
				}
			}
		});
		localNewParametersButton.setVisible(PlatformSettings.getGUIControllerForPlatform().isNewParametersEnabled());
		
		final JPanel rightTop = FormsUtils.build("f:p:g p ' p",
												 "001 f:p:g||" + 
												 "_21 p",
												 treePanel,geneSetterPanel,localNewParametersButton).getPanel();
		rightTop.setBorder(BorderFactory.createTitledBorder(null,"Chromosome",TitledBorder.LEADING,TitledBorder.BELOW_TOP));
		
		final JPanel mainPanel = FormsUtils.build("p f:p:g", 
				"01 f:p:g||" +
				"02 p",
				gaSettingsPanel,rightTop,
				operatorSettingsPanel).getPanel();
		
		setLayout(new BorderLayout());
		mainScrPane = new JScrollPane(mainPanel);
		mainScrPane.setBorder(null);
		mainScrPane.setViewportBorder(null);
		add(mainScrPane, BorderLayout.CENTER);
		
		resetSettings();
		enableDisableSettings(false);
	}
	
	//----------------------------------------------------------------------------------------------------
	protected void showOperatorDetails(final JPanel operatorSettingsPanel, final IGAConfigurator operator) {
		if (operator != null) {
			CardLayout settingslayout = (CardLayout) operatorSettingsPanel.getLayout();
			settingslayout.show(operatorSettingsPanel, operator.getName());
			((TitledBorder)operatorSettingsPanel.getBorder()).setTitle(operator.getName());
			operatorSettingsPanel.repaint();
		} 
	}
	
	//------------------------------------------------------------------------------
	/** Enables/disables the parameter editing components of the page according to
	 *  the value of <code>enabled</code>.
	 */
	private void enableDisableSettings(final boolean enabled) {
		constantRButton.setEnabled(enabled);
		geneRButton.setEnabled(enabled);
		intGeneValueRangeRButton.setEnabled(enabled);
		doubleGeneValueRangeRButton.setEnabled(enabled);
		listGeneValueRButton.setEnabled(enabled);
		constantField.setEnabled(enabled);
		enumDefBox.setEnabled(enabled);
		fileTextField.setEnabled(enabled);
		fileBrowseButton.setEnabled(enabled);
		intMinField.setEnabled(enabled);
		intMaxField.setEnabled(enabled);
		doubleMinField.setEnabled(enabled);
		doubleMaxField.setEnabled(enabled);
		valuesArea.setEnabled(enabled);
		modifyButton.setEnabled(enabled);
		cancelButton.setEnabled(enabled);
		localNewParametersButton.setEnabled(PlatformSettings.getGUIControllerForPlatform().isNewParametersEnabled() && !enabled);
	}

	//------------------------------------------------------------------------------
	/** Resets the parameter editing related components. */
	private void resetSettings() { 
		constantRButton.setSelected(false);
		geneRButton.setSelected(false);
		intGeneValueRangeRButton.setSelected(false);
		doubleGeneValueRangeRButton.setSelected(false);
		listGeneValueRButton.setSelected(false);
		constantField.setText("");
		fileTextField.setText("");
		intMinField.setText("");
		intMaxField.setText("");
		doubleMinField.setText("");
		doubleMaxField.setText("");
		valuesArea.setText("");
		modifyButton.setEnabled(false);
		cancelButton.setEnabled(false);
		
		geneRButton.setVisible(false);
		intGeneValueRangeRButton.setVisible(true);
		doubleGeneValueRangeRButton.setVisible(true);
		listGeneValueRButton.setVisible(true);
		
		final CardLayout cl = (CardLayout) geneSettingsValuePanel.getLayout();
		cl.show(geneSettingsValuePanel,"NULL");
	}
	
	//----------------------------------------------------------------------------------------------------
	private void edit(final ParameterOrGene param) {
		enableDisableSettings(true);
		
		if (param.getInfo().isBoolean()) {
			geneRButton.setVisible(true);
			intGeneValueRangeRButton.setVisible(false);
			doubleGeneValueRangeRButton.setVisible(false);
			listGeneValueRButton.setVisible(false);
		}
		
		String view = PARAM_SETTINGS_CONSTANT_VALUE_PANEL;
		if (param.isGene()) {
			editGene(param.getGeneInfo());
			return;
		} else if (param.getInfo().isEnum() || param.getInfo() instanceof MasonChooserParameterInfo) {
			view = PARAM_SETTINGS_ENUM_VALUE_PANEL;
			intGeneValueRangeRButton.setEnabled(false);
			doubleGeneValueRangeRButton.setEnabled(false);
			listGeneValueRButton.setEnabled(false);
			constantRButton.setSelected(true);
			
			final ParameterInfo info = param.getInfo();
			final Object selected = info.getValue();
			Object[] elements = null;
			if (info.isEnum()) {
				@SuppressWarnings("unchecked")
				final Class<Enum<?>> type = (Class<Enum<?>>) info.getJavaType();
				elements = type.getEnumConstants();
			} else {
				final MasonChooserParameterInfo chooserInfo = (MasonChooserParameterInfo) info;
				elements = chooserInfo.getValidStrings();
			}
			
			final DefaultComboBoxModel<Object> model = (DefaultComboBoxModel<Object>) enumDefBox.getModel();
			model.removeAllElements();
			for (final Object object : elements) 
				model.addElement(object);
				
			if (info.isEnum())
				enumDefBox.setSelectedItem(selected);
			else 
				enumDefBox.setSelectedIndex((Integer)selected);
		} else if (param.getInfo().isFile()) {
			view = PARAM_SETTINGS_FILE_VALUE_PANEL;
			constantRButton.setSelected(true);
			final File file = (File) param.getInfo().getValue();
			if (file != null) {
				fileTextField.setText(file.getName());
				fileTextField.setToolTipText(file.getAbsolutePath());
			}
		} else {
			constantRButton.setSelected(true);
			constantField.setText(param.getInfo().valuesToString());
		}
		
		final CardLayout cl = (CardLayout) geneSettingsValuePanel.getLayout();
		cl.show(geneSettingsValuePanel,view);
		if (!param.getInfo().isNumeric()) {
			intGeneValueRangeRButton.setEnabled(false);
			doubleGeneValueRangeRButton.setEnabled(false);
			listGeneValueRButton.setEnabled("String".equals(param.getInfo().getType()));
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void editGene(final GeneInfo geneInfo) {
		String view = null;
		if (GeneInfo.LIST.equals(geneInfo.getValueType())) {
			view = GENE_SETTINGS_LIST_VALUE_PANEL;
			listGeneValueRButton.setSelected(true);
			valuesArea.setText(Utils.join(geneInfo.getValueRange()," "));
		} else if (GeneInfo.INTERVAL.equals(geneInfo.getValueType())) {
			if (geneInfo.isIntegerVals()) {
				view = GENE_SETTINGS_INT_RANGE_VALUE_PANEL;
				intGeneValueRangeRButton.setSelected(true);
				intMinField.setText(String.valueOf(geneInfo.getMinValue()));
				intMaxField.setText(String.valueOf(geneInfo.getMaxValue()));
			} else {
				view = GENE_SETTINGS_DOUBLE_RANGE_VALUE_PANEL;
				doubleGeneValueRangeRButton.setSelected(true);
				doubleMinField.setText(String.valueOf(geneInfo.getMinValue()));
				doubleMaxField.setText(String.valueOf(geneInfo.getMaxValue()));
			}
		} else {
			view = "NULL";
			geneRButton.setSelected(true);
		}
		
		final CardLayout cl = (CardLayout) geneSettingsValuePanel.getLayout();
		cl.show(geneSettingsValuePanel,view);
	}
	
	//----------------------------------------------------------------------------------------------------
	private String[] checkInput(final ParameterOrGene param) {
		final List<String> errors = new ArrayList<String>();
		
		if (constantRButton.isSelected()) {
			final ParameterInfo info = param.getInfo();
			
			if (info.isEnum() || info instanceof MasonChooserParameterInfo) { 
				// well, the combo box must contain a valid value
			} else if (info.isFile()) {
				if (fileTextField.getText().trim().isEmpty())
					Logger.logWarning("Empty string was specified as file parameter " + info.getName().replaceAll("([A-Z])"," $1"));

				final File file = new File(fileTextField.getToolTipText());
				if (!file.exists()) 
					errors.add("File " + fileTextField.getToolTipText() + " does not exist.");
			} else if (constantField.getText().trim().equals(""))
				errors.add("'Constant value' cannot be empty.");
			else {
				final boolean valid = ParameterInfo.isValid(constantField.getText().trim(),info.getType());
				if (!valid) 
					errors.add("'Constant value' must be a valid " + getTypeText(info.getType()) + ".");
				
				errors.addAll(PlatformSettings.additionalParameterCheck(info,new String[] { constantField.getText().trim() },ParameterInfo.CONST_DEF));
			}
		} else if (intGeneValueRangeRButton.isSelected()) {
			final String minFieldText = intMinField.getText();
			final String maxFieldText = intMaxField.getText();
			Long minValue = null;
			Long maxValue = null;
			
			if (minFieldText == null || minFieldText.trim().isEmpty()) 
				errors.add("'Minimum value' cannot be empty.");
			else {
				try {
					minValue = new Long(minFieldText.trim());
					
					final boolean valid = ParameterInfo.isValid(minFieldText,param.getInfo().getType());
					if (!valid) 
						errors.add("'Minimum value' must be a valid " + getTypeText(param.getInfo().getType()) + ".");
					
					errors.addAll(PlatformSettings.additionalParameterCheck(param.getInfo(),new String[] { minFieldText.trim() },ParameterInfo.CONST_DEF));
				} catch (final NumberFormatException _) {
					errors.add("'Minimum value' must be a valid integer number.");
				}
			}
			
			if (maxFieldText == null || maxFieldText.trim().isEmpty()) 
				errors.add("'Maximum value' cannot be empty.");
			else {
				try {
					maxValue = new Long(maxFieldText.trim());
					
					final boolean valid = ParameterInfo.isValid(maxFieldText,param.getInfo().getType());
					if (!valid) 
						errors.add("'Maximum value' must be a valid " + getTypeText(param.getInfo().getType()) + ".");
					
					errors.addAll(PlatformSettings.additionalParameterCheck(param.getInfo(),new String[] { maxFieldText.trim() },ParameterInfo.CONST_DEF));
				} catch (final NumberFormatException _) {
					errors.add("'Maximum value' must be a valid integer number.");
				}
			}
			
			if (minValue != null && maxValue != null) {
				if (minValue.longValue() > maxValue.longValue())
					errors.add("Please specify a non-empty interval!");
			}
		} else if (doubleGeneValueRangeRButton.isSelected()) {
			final String minFieldText = doubleMinField.getText();
			final String maxFieldText = doubleMaxField.getText();
			Double minValue = null;
			Double maxValue = null;
			
			if (minFieldText == null || minFieldText.trim().isEmpty()) 
				errors.add("'Minimum value' cannot be empty.");
			else {
				try {
					minValue = new Double(minFieldText.trim());
					
					final boolean valid = ParameterInfo.isValid(minFieldText,param.getInfo().getType());
					if (!valid) 
						errors.add("'Minimum value' must be a valid " + getTypeText(param.getInfo().getType()) + ".");
					
					errors.addAll(PlatformSettings.additionalParameterCheck(param.getInfo(),new String[] { minFieldText.trim() },ParameterInfo.CONST_DEF));
				} catch (final NumberFormatException _) {
					errors.add("'Minimum value' must be a valid number.");
				}
			}
			
			if (maxFieldText == null || maxFieldText.trim().isEmpty()) 
				errors.add("'Maximum value' cannot be empty.");
			else {
				try {
					maxValue = new Double(maxFieldText.trim());
					
					final boolean valid = ParameterInfo.isValid(maxFieldText,param.getInfo().getType());
					if (!valid) 
						errors.add("'Maximum value' must be a valid " + getTypeText(param.getInfo().getType()) + ".");
					
					errors.addAll(PlatformSettings.additionalParameterCheck(param.getInfo(),new String[] { maxFieldText.trim() },ParameterInfo.CONST_DEF));
				} catch (final NumberFormatException _) {
					errors.add("'Maximum value' must be a valid number.");
				}
			}
			
			if (minValue != null && maxValue != null) {
				if (minValue.doubleValue() > maxValue.doubleValue())
					errors.add("Please specify a non-empty interval!");
			}

		} else if (listGeneValueRButton.isSelected()) {
			if (valuesArea.getText() == null || valuesArea.getText().trim().isEmpty())
				errors.add("'Value list' cannot be empty.");
			else {
				try {
					final List<Object> valueList = new ArrayList<Object>();
					String values = valuesArea.getText().trim();
					values = values.replaceAll("\\s+"," ");
					final StringTokenizer tokenizer = new StringTokenizer(values, " ");
					while (tokenizer.hasMoreTokens()) {
						final String strValue = tokenizer.nextToken();
						
						final boolean valid = ParameterInfo.isValid(strValue,param.getInfo().getType());
						if (!valid) 
							errors.add("'" + strValue + "' is not a valid " + getTypeText(param.getInfo().getType()) + ".");
						
						valueList.add(parseListElement(param.getInfo().getJavaType(),strValue));
					}
					
					errors.addAll(PlatformSettings.additionalParameterCheck(param.getInfo(),values.split(" "),ParameterInfo.LIST_DEF));
				} catch (final NumberFormatException _) {
					errors.add("All element of the value list must be a valid number.");
				}
			}
		}
		
		return errors.isEmpty() ? null : errors.toArray(new String[0]);
	}
	
	//------------------------------------------------------------------------------
	/** Returns the appropriate error message part according to the value of <code>type</code>. */
	private String getTypeText(final String type) {
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
	private Object parseListElement(final Class<?> type, final String strValue) {
		if (String.class.equals(type)) 
			return strValue;
		
		if (type.equals(Double.class) || type.equals(Float.class) || type.equals(Double.TYPE) || type.equals(Float.TYPE))
			return Double.parseDouble(strValue);
		
		return Long.parseLong(strValue);
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean modify() {
		final ParameterOrGene userObj = (ParameterOrGene) editedNode.getUserObject();
		
		final String[] errors = checkInput(userObj);
		if (errors != null && errors.length != 0) {
			Utilities.userAlert(mainScrPane,(Object)errors);
			return false;
		} else {
			modifyParameterOrGene(userObj);
			resetSettings();
			
			final DefaultTreeModel model = (DefaultTreeModel) geneTree.getModel();
			model.nodeStructureChanged(editedNode);
			
			editedNode = null;
			
			updateNumberOfGenes();
			enableDisableSettings(false);
			return true;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean closeActiveModification() {
		return editedNode != null ? modify() : true;
	}
	
	//------------------------------------------------------------------------------
	private void modifyParameterOrGene(final ParameterOrGene param) {
		if (constantRButton.isSelected()) {
			final ParameterInfo info = param.getInfo();
			if (info.isEnum()) 
				param.setConstant(enumDefBox.getSelectedItem());
			else if (info instanceof MasonChooserParameterInfo) 
				param.setConstant(enumDefBox.getSelectedIndex());				
			else if (info.isFile()) 
				param.setConstant(ParameterInfo.getValue(fileTextField.getToolTipText(),info.getType()));
			else
				param.setConstant(ParameterInfo.getValue(constantField.getText().trim(),info.getType()));
		} else if (intGeneValueRangeRButton.isSelected()) {
			final Long minValue = new Long(intMinField.getText().trim());
			final Long maxValue = new Long(intMaxField.getText().trim());
			param.setGene(minValue,maxValue);
		} else if (doubleGeneValueRangeRButton.isSelected()) {
			final Double minValue = new Double(doubleMinField.getText().trim());
			final Double maxValue = new Double(doubleMaxField.getText().trim());
			param.setGene(minValue,maxValue);
		} else if (listGeneValueRButton.isSelected()) {
			final List<Object> valueList = new ArrayList<Object>();
			String values = valuesArea.getText().trim();
			values = values.replaceAll("\\s+"," ");
			final StringTokenizer tokenizer = new StringTokenizer(values, " ");
			while (tokenizer.hasMoreTokens()) 
				valueList.add(parseListElement(param.getInfo().getJavaType(),tokenizer.nextToken()));
			param.setGene(valueList);
		} else if (geneRButton.isSelected()) {
			param.setBooleanGene();
		}
	}

	//----------------------------------------------------------------------------------------------------
	private void updateNumberOfGenes() {
		int count = 0;
		
		final DefaultTreeModel treeModel = (DefaultTreeModel) geneTree.getModel();
		final DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
		@SuppressWarnings("rawtypes")
		final Enumeration nodes = root.breadthFirstEnumeration();
		nodes.nextElement();
		
		while (nodes.hasMoreElements()) {
			final DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
			final ParameterOrGene userObj = (ParameterOrGene) node.getUserObject();
			if (userObj.isGene())
				count++;
		}
		
		numberLabel.setText("<html><b>Number of genes:</b> " + count + "</html>");
	}
}