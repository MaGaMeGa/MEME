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
package ai.aitia.meme.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ai.aitia.meme.paramsweep.generator.IBlockingHelper;
import ai.aitia.meme.paramsweep.generator.RngSeedManipulatorModel;
import ai.aitia.meme.paramsweep.generator.RngSeedManipulatorModel.NaturalVariationInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterRandomInfo;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

/**
 * A class that handles the control and the GUI of RngSeed manipulation. 
 * <p>
 * How to use this:
 * <li>create it with the list of parameters.</li>
 * <li>put the JPanel returned by getSeedsListPanel() in the GUI</li>
 * <li>apply the randomizeParameterTree() method on the root of the parameter
 * tree before running the simulation</li>
 * <li>multiply the runcount with the value returned by
 * getRunsMultiplierFactor()</li>
 * 
 * @author Ferschl
 */
public class RngSeedManipulator implements ActionListener, MouseListener, ListSelectionListener, ItemListener, Serializable {

	private static final long serialVersionUID = -9658430228030756L;

	private static final String RANDOM_RADIO = "RANDOM_RADIO";
	private static final String DEF_RADIO = "DEF_RADIO";
	private static final String STEP_RADIO = "STEP_RADIO";
	private static final String SWEEP_RADIO = "SWEEP_RADIO";
	private static final String SEQ_RADIO = "SEQ_RADIO";
	private static final String NATURAL_VARIATION_SELECT_ALL = "NATURAL_VARIATION_SELECT_ALL";
	private static final String NATURAL_VARIATION_CLOSE = "NATURAL_VARIATION_CLOSE";
	private static final String NATURAL_VARIATION_BUTTON = "NATURAL_VARIATION_BUTTON";
	private static final String FILL_OK = "FILL_OK";
	private static final String FILL_STEP = "FILL_STEP";
	private static final String FILL_TO = "FILL_TO";
	private static final String FILL_FROM = "FILL_FROM";
	private static final String RADIO = "RADIO";
	private static final String SHOW_MANIP_DIALOG = "SHOW_MANIP_DIALOG";
	private static final String MODIFY_RANDOM_INFO = "MODIFY_RANDOM_INFO";
	private static final String CANCEL_DIALOG = "CANCEL_DIALOG";
	private static final String CLOSE_DIALOG = "CLOSE_DIALOG";
	private static final String BLOCKING_CHECKBOX = "BLOCKING_CHECKBOX";
	private static final String BLOCKING_CLOSE_BUTTON = "BLOCKING_CLOSE_BUTTON";
	private static final String BLOCKING_SET_BUTTON = "BLOCKING_SET_BUTTON";
	private static final String BLOCKING_BUTTON = "BLOCKING_BUTTON";
	private static final String ADD_SEEDS = "ADD_SEEDS";
	private static final String ADD_SEED_OK = "ADD_SEED_OK";
	private static final String ADD_SEED_CANCEL = "ADD_SEED_CANCEL";
	
	/**
	 * The model of the RngSeedManipulator. It is separated from the GUI.
	 */
	private RngSeedManipulatorModel model = null;

	// GUI
	// elements----------------------------------------------------------------
	private transient JPanel content = null;
	private transient JPanel settingsPanel = null;
	private transient JList seedsList = null;
	private transient JScrollPane seedsScr = null;
	private transient JList seedsList2 = null;
	private transient JScrollPane seedsScr2 = null;
	private transient JButton addSeedsButton = null;
	private transient JDialog randomSeedManipulatorDialog = null;
	private transient JButton okButton = null;
	private transient JButton cancelButton = null;
	private transient JRadioButton defaultRandomRadio = null;
	private transient JRadioButton sweepRandomRadio = null;
	private transient JRadioButton randomRandomRadio = null;
	private transient JRadioButton sequenceRandomRadio = null;
	private transient JRadioButton stepRandomRadio = null;
	private transient JTextArea sweepListDefArea = null;
	private transient JScrollPane sweepListDefScr = null;
	private transient JTextArea seqListDefArea = null;
	private transient JScrollPane seqListDefScr = null;
	private transient JTextField stepStartField = null;
	private transient JTextField stepStepField = null;
	private transient JButton modifyRandomInfoButton = null;
	private transient JDialog addSeedDialog = null;
	private transient JTable addSeedTable = null;
	private transient JScrollPane addSeedTableScr = null;
	private transient JButton addSeedOkButton = null;
	private transient JButton addSeedCancelButton = null;
	private transient JButton rngSeedManipulatorButton = null;
	private transient JTextField defValueField = null;
	private transient JPanel seedsListPanel = null;

	private transient JTextField fillFromField = null;
	private transient JTextField fillToField = null;
	private transient JTextField fillStepField = null;
	private transient JButton fillOKButton = null;
	private transient JPanel fillPanel = null;

	private transient JButton naturalVariationButton = null;
	private transient JButton naturalVariationButton2 = null;
	private transient JDialog naturalVariationDialog = null;
	private transient Vector<JCheckBox> naturalVariationCheckBoxes = null;
	private transient JButton naturalVariationCloseButton = null;
	private transient JButton naturalVariationSelectAllButton = null;

	private transient JButton blockingButton = null;
	private transient JButton blockingSmallPanelButton = null;
	private transient JDialog blockingDialog = null;
	private transient JList blockingVariablesList = null;
	private transient JScrollPane blockingVariablesScr = null;
	private transient JSpinner blockingVariableValueCountSpinner = null;
	private transient JTextArea blockingVariableValues = null;
	private transient JButton blockingSetButton = null;
	private transient JButton blockingCloseButton = null;
	private transient JCheckBox blockingIsBlockingCheckBox = null;
	private transient JCheckBox combinedSweepCheckBox = null;
	private transient JLabel runMultiplierNumberLabel;

	// --------------------------------------------------------------------------
	public List<? extends ParameterInfo> selectedParametersList;


	// --------------------------------------------------------------------------
	/**
	 * A constructor that creates an RngSeedManipulator with the model from the seed candidates and 
	 * the blocking helper. 
	 * @param randomSeedCandidateParameters	The parameters of the model that can serve as random seeds.
	 * @param blockingHelper	An object that implements the BlockingHelper interface, this is 
	 * usually the IntelliSweep plugin
	 */
	public RngSeedManipulator(List<ParameterInfo> randomSeedCandidateParameters, IBlockingHelper blockingHelper) {
		this.model = new RngSeedManipulatorModel(randomSeedCandidateParameters, blockingHelper);
		selectedParametersList = new ArrayList<ParameterInfo>();
		layoutGUI();
	}
	
	/**
	 * A constructor that creates an RngSeedManipulator from the model in the argument.
	 * @param model
	 */
	public RngSeedManipulator(RngSeedManipulatorModel model) {
		this.model = model;
		selectedParametersList = new ArrayList<ParameterInfo>();
		layoutGUI();
	}

	/**
	 * Adds the parameter in the argument to the random seeds. It will be a combined sweep seed with 
	 * the parameters default value as the only element, or 1 if it is null.
	 * @param parInf	The parameter info that the random seed info is based on.
	 */
	public void addSeed(ParameterInfo parInf) {
		//checking if the possible seeds contain the argument:
		if (model.possibleRandomSeedParameters.contains(parInf)) {
			ParameterRandomInfo toAdd = new ParameterRandomInfo(parInf);
			model.randomSeedParameters.add(toAdd);
			toAdd.setRndType(ParameterRandomInfo.SWEEP_RND);
			toAdd.setCombined(true);
			toAdd.getSeedList().add((parInf.getValue() != null ? parInf.getValue() : 1));
			updateSeedsLists();
		}
	}

	/**
	 * Updates the possible random seeds list. This is used when this list could have been updated, like when 
	 * adding new parameters to the model by the user in the Parameter Sweep Wizard.
	 * @param newList	The list of the possible random seeds.
	 * @param selectedInfos	The infos that are selected already in the design, so they will not appear as 
	 * selectable random seeds. 
	 */
	public void updatePossibleRandomSeedParameters(List<? extends ParameterInfo> newList,
			List<? extends ParameterInfo> selectedInfos) {
		selectedParametersList = selectedInfos;
		updateNaturalVariationCheckBoxes();
		model.possibleRandomSeedParameters = new Vector<ParameterInfo>();
		for (ParameterInfo info : newList) {
			if (!info.getType().equalsIgnoreCase("boolean") && !info.getType().equalsIgnoreCase("string")) 
				model.possibleRandomSeedParameters.add(info);
		}
		// Removing the elements from randomSeedParameters that are no longer
		// present in model.possibleRandomSeedParameters.
		boolean removeOccured = false;
		for (Iterator iter = model.randomSeedParameters.iterator(); iter.hasNext();) {
			ParameterRandomInfo info = (ParameterRandomInfo) iter.next();
			boolean present = false;
			for (int i = 0; i < model.possibleRandomSeedParameters.size(); i++) {
				if (info.getName().compareTo(model.possibleRandomSeedParameters.get(i).getName()) == 0) {
					present = true;
				}
			}
			if (!present) {
				iter.remove();
				removeOccured = true;
			}
		}
		if (removeOccured) {
			updateSeedsLists();
			model.newSeedsTableModel();
			addSeedTable.setModel(model.seedsTableModel);
			addSeedTable.setVisible(false);
			addSeedTable.setVisible(true);
		}
		model.updateBlockingInfos(newList);
	}

	/**
	 * Fills the list of natural variation checkboxes with the selected random seeds. This is done by 
	 * adding the non-random seed parameters of the model that are selected in the design. 
	 */
	private void updateNaturalVariationCheckBoxes() {
		// remove items not on the list anymore:
		for (Iterator<JCheckBox> iterator = naturalVariationCheckBoxes.iterator(); iterator.hasNext();) {
			JCheckBox checkBox = iterator.next();
			boolean present = false;
			for (int i = 0; i < selectedParametersList.size(); i++) {
				if (selectedParametersList.get(i).getName().equals(checkBox.getText())) {
					present = true;
				}
			}
			if (!present) iterator.remove();
		}
		// add new items to the list:
		for (int i = 0; i < selectedParametersList.size(); i++) {
			boolean present = false;
			for (int j = 0; j < naturalVariationCheckBoxes.size(); j++) {
				if (naturalVariationCheckBoxes.get(j).getText().equals(selectedParametersList.get(i).getName())) {
					present = true;
				}
			}
			if (!present) {
				JCheckBox newCB = new JCheckBox(selectedParametersList.get(i).getName());
				newCB.addItemListener(this);
				naturalVariationCheckBoxes.add(newCB);
			}
		}
		// updating the infos based on the checkboxes
		model.naturalVariationInfos = new ArrayList<NaturalVariationInfo>();
		for (int i = 0; i < naturalVariationCheckBoxes.size(); i++) {
			model.naturalVariationInfos.add(new NaturalVariationInfo(naturalVariationCheckBoxes.get(i).getText(), naturalVariationCheckBoxes.get(i).isSelected()));
		}
	}

	/**
	 * Creates both GUIs of the RngSeedManipulator, the one used in the dialog, and the one that can be 
	 * inserted into the plugin's settings GUI. 
	 */
	private void layoutGUI() {
		addSeedsButton = new JButton("Add/remove seeds...");
		okButton = new JButton("OK");
		cancelButton = new JButton("Cancel");
		defaultRandomRadio = new JRadioButton(
				"(Default) Default behaviour, a constant seed for every run, value = ");
		sweepRandomRadio = new JRadioButton(
				"(Sweep) Repeat the whole design several times with these seeds:");
		randomRandomRadio = new JRadioButton("(Random) Generates a random randomseed for every run");
		sequenceRandomRadio = new JRadioButton(
				"(Sequence) Use this sequence of seeds, changing between every run, loop if necessary");
		stepRandomRadio = new JRadioButton(
				"(Step) Use a step sequence that generates a new seed at each run with these parameters:");
		sweepListDefArea = new JTextArea();
		sweepListDefScr = new JScrollPane(sweepListDefArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		seqListDefArea = new JTextArea();
		seqListDefScr = new JScrollPane(seqListDefArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		stepStartField = new JTextField();
		stepStepField = new JTextField();
		modifyRandomInfoButton = new JButton("Apply");
		addSeedTable = new JTable();
		addSeedTableScr = new JScrollPane(addSeedTable);
		addSeedOkButton = new JButton("OK");
		addSeedCancelButton = new JButton("Cancel");
		rngSeedManipulatorButton = new JButton("Random seed management...");
		defValueField = new JTextField(20);

		fillFromField = new JTextField("1", 10);
		fillToField = new JTextField("10", 10);
		fillStepField = new JTextField("1", 10);
		fillOKButton = new JButton("OK");
		fillPanel = new JPanel(new FlowLayout());

		naturalVariationButton = new JButton("Variation analysis...");
		naturalVariationButton2 = new JButton("Variation analysis...");
//		naturalVariationDialog = new JDialog();
		naturalVariationCheckBoxes = new Vector<JCheckBox>();
		naturalVariationCloseButton = new JButton("Close");
		naturalVariationSelectAllButton = new JButton("Select all");

		blockingButton = new JButton("Blocking...");
		blockingSmallPanelButton = new JButton("Blocking...");
		blockingVariableValues = new JTextArea(4, 30);
		blockingSetButton = new JButton("Set");
		blockingCloseButton = new JButton("Close");
		blockingIsBlockingCheckBox = new JCheckBox("Blocking variable");
		// TODO: needs an understandable and real English caption:
		combinedSweepCheckBox = new JCheckBox("Combine this seed with the other combined sweep seeds");
		
		
		seedsList = new JList(model.randomSeedParameters);
		seedsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		seedsList.addListSelectionListener(getSeedsListSelectionListener());
		if (model.randomSeedParameters.size() > 0) seedsList.setSelectedIndex(0);
		seedsScr = new JScrollPane(seedsList);
		seedsScr.setPreferredSize(new Dimension(300, 120));
		seedsList2 = new JList(model.randomSeedParameters);
		seedsList2.addMouseListener(this);
		seedsScr2 = new JScrollPane(seedsList2);
		seedsScr2.setPreferredSize(new Dimension(300, 50));
		sweepListDefArea.setLineWrap(true);
		sweepListDefArea.setWrapStyleWord(true);
		sweepListDefScr.setPreferredSize(new Dimension(200, 60));
		seqListDefArea.setLineWrap(true);
		seqListDefArea.setWrapStyleWord(true);
		seqListDefScr.setPreferredSize(new Dimension(200, 60));
		fillPanel.add(new JLabel("From:"));
		fillPanel.add(fillFromField);
		fillPanel.add(new JLabel("To:"));
		fillPanel.add(fillToField);
		fillPanel.add(new JLabel("Step:"));
		fillPanel.add(fillStepField);
		fillPanel.add(fillOKButton);
//		combinedSweepCheckBox.setVisible(false);
		JPanel settingButtonPanel = new JPanel(new FlowLayout());
		settingButtonPanel.add(modifyRandomInfoButton);
		// settingsPanel is used to set the selected random info 
		settingsPanel = FormsUtils.build(
				"r:50dlu f:p:g p",
				"000 p|" + 
				"_11 p|" + 
				"_22 p||" + 
				"333 p|" + 
				"_44 p||" + 
				"555 p||" + 
				"666 p|" + 
				"777 p|||" + 
				"888 p",
				// defaultRandomRadio, defValueField,
				sweepRandomRadio, combinedSweepCheckBox, sweepListDefScr, sequenceRandomRadio, seqListDefScr,
				"Fill the selected box above with numbers:", fillPanel,
				// stepRandomRadio,
				// "start :", stepStartField,
				// "step :", stepStepField,
				randomRandomRadio, settingButtonPanel).getPanel();
		settingsPanel.setBorder(BorderFactory.createTitledBorder("Behaviour of the chosen random seed"));
		JPanel runMultiplierPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		runMultiplierNumberLabel = new JLabel("1");
		JLabel runMultiplierTextLabel = new JLabel("Number of replications of the design =");
		runMultiplierPanel.add(runMultiplierTextLabel);
		runMultiplierPanel.add(runMultiplierNumberLabel);
		runMultiplierPanel.setBorder(BorderFactory.createTitledBorder("Replication multiplier"));
		JPanel okCancelPanel = new JPanel(new FlowLayout());
		okCancelPanel.add(okButton);
		okCancelPanel.add(cancelButton);
		JPanel underListButtonPanel = new JPanel(new FlowLayout());
		underListButtonPanel.add(addSeedsButton);
		underListButtonPanel.add(naturalVariationButton);
		underListButtonPanel.add(blockingButton);
		// content is the whole GUI of the RngSeedManipulator
		content = FormsUtils.build(
				"f:p:g", 
				"0 p|" + 
				"1 f:p:g|" + 
				"2 p|" + 
				"3 p|" + 
				"4 p|" +
				"5 p", 
				"Random seeds:", seedsScr,
				underListButtonPanel, settingsPanel, runMultiplierPanel, okCancelPanel).getPanel();
		content.setBorder(BorderFactory.createTitledBorder("Random seed manipulation"));
		model.newSeedsTableModel();
		addSeedTable.setModel(model.seedsTableModel);
		addSeedTableScr.setPreferredSize(new Dimension(450, 250));
		// addSeedDialog.setContentPane(FormsUtils.build(
		// "p p f:p:g",
		// "000 f:p||" +
		// "12_ p|",
		// addSeedTableScr,
		// addSeedOkButton, addSeedCancelButton
		// ).getPanel());
		// addSeedDialog.setModal(true);
		// addSeedDialog.setSize(500, 350);
		// addSeedDialog.setTitle("Add/remove seeds");
		addSeedCancelButton.setActionCommand(ADD_SEED_CANCEL);
		addSeedOkButton.setActionCommand(ADD_SEED_OK);
		addSeedsButton.setActionCommand(ADD_SEEDS);
		okButton.setActionCommand(CLOSE_DIALOG);
		cancelButton.setActionCommand(CANCEL_DIALOG);
		modifyRandomInfoButton.setActionCommand(MODIFY_RANDOM_INFO);
		rngSeedManipulatorButton.setActionCommand(SHOW_MANIP_DIALOG);
		sequenceRandomRadio.setActionCommand(SEQ_RADIO);
		sweepRandomRadio.setActionCommand(SWEEP_RADIO);
		stepRandomRadio.setActionCommand(STEP_RADIO);
		defaultRandomRadio.setActionCommand(DEF_RADIO);
		randomRandomRadio.setActionCommand(RANDOM_RADIO);
		fillOKButton.setActionCommand(FILL_OK);
		fillFromField.setActionCommand(FILL_FROM);
		fillToField.setActionCommand(FILL_TO);
		fillStepField.setActionCommand(FILL_STEP);
		naturalVariationButton.setActionCommand(NATURAL_VARIATION_BUTTON);
		naturalVariationButton2.setActionCommand(NATURAL_VARIATION_BUTTON);
		naturalVariationCloseButton.setActionCommand(NATURAL_VARIATION_CLOSE);
		naturalVariationSelectAllButton.setActionCommand(NATURAL_VARIATION_SELECT_ALL);
		blockingButton.setActionCommand(BLOCKING_BUTTON);
		blockingSmallPanelButton.setActionCommand(BLOCKING_BUTTON);
		blockingSetButton.setActionCommand(BLOCKING_SET_BUTTON);
		blockingCloseButton.setActionCommand(BLOCKING_CLOSE_BUTTON);
		blockingIsBlockingCheckBox.setActionCommand(BLOCKING_CHECKBOX);

		ButtonGroup bg = new ButtonGroup();
		bg.add(defaultRandomRadio);
		bg.add(randomRandomRadio);
		bg.add(sequenceRandomRadio);
		bg.add(stepRandomRadio);
		bg.add(sweepRandomRadio);
		defaultRandomRadio.setSelected(true);
		GUIUtils.addActionListener(this, addSeedCancelButton, addSeedOkButton, addSeedsButton, okButton, cancelButton,
				modifyRandomInfoButton, rngSeedManipulatorButton, sequenceRandomRadio, sweepRandomRadio, stepRandomRadio,
				defaultRandomRadio, randomRandomRadio, fillOKButton, fillFromField, fillToField, fillStepField,
				naturalVariationButton, naturalVariationButton2, naturalVariationCloseButton,
				naturalVariationSelectAllButton, blockingButton, blockingSmallPanelButton, blockingSetButton,
				blockingCloseButton, blockingIsBlockingCheckBox);
	}

	/**
	 * Creates (if necessary) and returns the add/remove seed dialog.
	 * @return A JDialog instance with the selectable seeds in table format.
	 */
	public JDialog getAddSeedDialog() {
		if (addSeedDialog == null) {
			addSeedDialog = new JDialog();
			final JPanel panel = FormsUtils.build("p p f:p:g", "000 f:p||" + "12_ p|", addSeedTableScr,addSeedOkButton,addSeedCancelButton).getPanel();
			final JScrollPane sp = new JScrollPane(panel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			sp.setBorder(null);
			addSeedDialog.setModal(true);
			addSeedDialog.setTitle("Add/remove seeds");
			addSeedDialog.setContentPane(sp);
			addSeedDialog.pack();
			Dimension oldD = addSeedDialog.getPreferredSize();
			addSeedDialog.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
														 oldD.height + sp.getHorizontalScrollBar().getHeight()));
			sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			oldD = addSeedDialog.getPreferredSize();
			final Dimension newD = GUIUtils.getPreferredSize(addSeedDialog);
			if (!oldD.equals(newD)) 
				addSeedDialog.setPreferredSize(newD);
			addSeedDialog.pack();
		}
		return addSeedDialog;
	}

	/**
	 * Updates the seed lists in both GUIs, according to the model that has probably changed before the 
	 * call of this method.
	 */
	public void updateSeedsLists() {
		Object selectedValue = null;
		if (model.randomSeedParameters.size() > 0) {
			selectedValue = seedsList.getSelectedValue();
		}
		DefaultListModel lm = new DefaultListModel();
		DefaultListModel lm2 = new DefaultListModel();
		// Collections.sort(randomSeedParameters, new
		// ParameterRandomInfo("","").getComparator());
		for (int i = 0; i < model.randomSeedParameters.size(); i++) {
			lm.addElement(model.randomSeedParameters.get(i));
			lm2.addElement(model.randomSeedParameters.get(i));
		}
		seedsList.setModel(lm);
		seedsList2.setModel(lm2);
		seedsList.setVisible(false);
		seedsList2.setVisible(false);
		seedsList.setVisible(true);
		seedsList2.setVisible(true);
		if (selectedValue != null) {
			// seedsList.setSelectedValue(selectedValue, true);
		}
		for (IRngSeedManipulatorChangeListener rngSMCListener : model.rngSeedManipulatorChangeListeners) {
			rngSMCListener.rngSeedsChanged();
		}
		
		updateRunMultiplierDisplay(getRunsMultiplierFactor());
	}

	/**
	 * Updates the GUI label which shows the multiplier factor of the current setting of the RngSeedManipulator.
	 * @param runMultiplierFactor	The number to show as multiplier.
	 */
	private void updateRunMultiplierDisplay(int runMultiplierFactor) {
		runMultiplierNumberLabel.setText(""+runMultiplierFactor);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().compareTo(ADD_SEEDS) == 0) {
			model.newSeedsTableModel();
			addSeedTable.setModel(model.seedsTableModel);
			// addSeedDialog.setVisible(true);
			getAddSeedDialog().setVisible(true);
		} else if (e.getActionCommand().compareTo(ADD_SEED_OK) == 0) {
			Vector<Boolean> selectedSeeds = model.seedsTableModel.getSelectedList();
			for (int i = 0; i < selectedSeeds.size(); i++) {
				if (selectedSeeds.get(i)) {
					if (!model.isSeed(model.possibleRandomSeedParameters.get(i))) {
						addSeed(model.possibleRandomSeedParameters.get(i));
					}
				} else {
					if (model.isSeed(model.possibleRandomSeedParameters.get(i))) {
						removeFromRandomSeeds(model.possibleRandomSeedParameters.get(i).getName());
					}
				}
			}
			updateSeedsLists();
			// addSeedDialog.setVisible(false);
			getAddSeedDialog().setVisible(false);
		} else if (e.getActionCommand().compareTo(ADD_SEED_CANCEL) == 0) {
			// addSeedDialog.setVisible(false);
			getAddSeedDialog().setVisible(false);
		} else if (e.getActionCommand().compareTo(CLOSE_DIALOG) == 0) {
			modifyInfo();
			randomSeedManipulatorDialog.setVisible(false);
		} else if (e.getActionCommand().compareTo(CANCEL_DIALOG) == 0) {
			randomSeedManipulatorDialog.setVisible(false);
		} else if (e.getActionCommand().compareTo(MODIFY_RANDOM_INFO) == 0) {
			modifyInfo();
		} else if (e.getActionCommand().compareTo(SHOW_MANIP_DIALOG) == 0) {
			showDialog();
		} else if (e.getActionCommand().endsWith(RADIO)) {
			setEnabledDisabled();
		} else if (e.getActionCommand().compareTo(FILL_FROM) == 0) {
			requestFocusAndSelectAll(fillToField);
		} else if (e.getActionCommand().compareTo(FILL_TO) == 0) {
			requestFocusAndSelectAll(fillStepField);
		} else if (e.getActionCommand().compareTo(FILL_STEP) == 0) {
			fillOKButton.doClick();
		} else if (e.getActionCommand().compareTo(FILL_OK) == 0) {
			fillChosenTextArea();
		} else if (e.getActionCommand().compareTo(NATURAL_VARIATION_BUTTON) == 0) {
			showNaturalVariationDialog();
		} else if (e.getActionCommand().compareTo(NATURAL_VARIATION_CLOSE) == 0) {
			naturalVariationDialog.setVisible(false);
		} else if (e.getActionCommand().compareTo(NATURAL_VARIATION_SELECT_ALL) == 0) {
			for (int i = 0; i < naturalVariationCheckBoxes.size(); i++) {
				naturalVariationCheckBoxes.get(i).setSelected(true);
			}
		} else if (e.getActionCommand().compareTo(BLOCKING_BUTTON) == 0) {
			showBlockingDialog();
		} else if (e.getActionCommand().compareTo(BLOCKING_CLOSE_BUTTON) == 0) {
			if (checkBlockingSettings()) {
				blockingDialog.setVisible(false);
			}
		} else if (e.getActionCommand().compareTo(BLOCKING_CHECKBOX) == 0) {
			enableDisableBlockingControls(blockingIsBlockingCheckBox.isSelected());
			blockingVariablesList.setVisible(false);
			blockingVariablesList.setVisible(true);
		} else if (e.getActionCommand().compareTo(BLOCKING_SET_BUTTON) == 0) {
			RngSeedManipulatorModel.BlockingParameterInfo info = getSelectedBlockingInfo();
			if (info != null) {
				setBlockingInfo(info);
				blockingVariablesList.setVisible(false);
				blockingVariablesList.setVisible(true);
			}
		}

	}

	/**
	 * Checks blocking settings and returns <code>false</code> when something is wrong, and pops up alert windows 
	 * to notify the user.
	 * @return
	 */
	private boolean checkBlockingSettings() {
		boolean ret = true;
		Vector<String> notEnoughValues = new Vector<String>();
		Vector<String> tooManyValues = new Vector<String>();
		Vector<String> longerThanDesignSize = new Vector<String>();
		int designSize = model.blockingHelper.getDesignSize();
		for (RngSeedManipulatorModel.BlockingParameterInfo info : model.blockingInfos) {
			if(info.isBlocking()){
				if (info.getSize() > info.getBlockingValues().size()) {
					notEnoughValues.add(info.getName());
					ret = false; //this is an error
				}
				if (info.getSize() < info.getBlockingValues().size()) {
					tooManyValues.add(info.getName());
					//this is only a warning, no need to return false
				}
				if (info.getSize() > designSize) {
					longerThanDesignSize.add(info.getName());
					//this is only a warning, no need to return false
				}
			}
		}
		Vector<Object> alertMessage = new Vector<Object>();
		if (notEnoughValues.size() > 0) {
			alertMessage.add("Error: The following blocking variables do not have enough values: ");
			alertMessage.addAll(notEnoughValues);
		}
		if (tooManyValues.size() > 0) {
			alertMessage.add("Warning: The following blocking variables have more values than their size: ");
			alertMessage.addAll(tooManyValues);
		}
		if (longerThanDesignSize.size() > 0) {
			alertMessage.add("Warning: The following blocking variables have more values than the size of the design: ");
			alertMessage.addAll(longerThanDesignSize);
		}
		if(alertMessage.size() > 0){
			Utilities.userAlert(content, "Warning", alertMessage.toArray());
		}
		return ret;
	}

	private void enableDisableBlockingControls(boolean selected) {
		blockingSetButton.setEnabled(selected);
		blockingVariableValueCountSpinner.setEnabled(selected);
		blockingVariableValues.setEnabled(selected);
		if (getSelectedBlockingInfo() != null) {
			getSelectedBlockingInfo().setBlocking(selected);
		}
	}

	private RngSeedManipulatorModel.BlockingParameterInfo getSelectedBlockingInfo() {
		RngSeedManipulatorModel.BlockingParameterInfo ret = null;
		if (blockingVariablesList.getSelectedIndex() > -1) {
			ret = model.blockingInfos.get(blockingVariablesList.getSelectedIndex());
		}
		return ret;
	}

	private void showNaturalVariationDialog() {
		naturalVariationDialog = getNaturalVariationDialog();
		naturalVariationDialog.setModal(true);
		naturalVariationDialog.setVisible(true);
	}

	private void showBlockingDialog() {
		if (blockingDialog == null) {
			blockingDialog = getBlockingDialog();
		}
		updateBlockingList();
		blockingDialog.setModal(true);
		blockingDialog.setVisible(true);
	}

	private void updateBlockingList() {
		DefaultListModel lm = new DefaultListModel();
		for (RngSeedManipulatorModel.BlockingParameterInfo info : model.blockingInfos) {
			lm.addElement(info);
		}
		blockingVariablesList.setModel(lm);
	}

	private JDialog getBlockingDialog() {
		JDialog ret = new JDialog();
		blockingVariablesList = new JList();
		blockingVariablesList.addListSelectionListener(this);
		blockingVariablesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		blockingVariablesScr = new JScrollPane(blockingVariablesList);
		ret.setTitle("Blocking");
		JPanel blockingBottomPanel = new JPanel(new FlowLayout());
		Object[] list = new Object[15];
		for (int i = 0; i < list.length; i++) {
			list[i] = new Integer(i + 2);
		}
		blockingVariableValueCountSpinner = new JSpinner(new SpinnerListModel(list));
		blockingBottomPanel.add(blockingCloseButton);
		blockingVariablesScr.setPreferredSize(new Dimension(400,200));
		JPanel panel = FormsUtils.build(
				"~ f:p:g ~ p ~",
				"01 p|" + 
				"02 p|" + 
				"03 p|" + 
				"04 p|" + 
				"05 p|" + 
				"06 p|" + 
				"0_ f:p:g||" + 
				"77 p|", 
				blockingVariablesScr, 
				blockingIsBlockingCheckBox, "Blocking variable values size:", blockingVariableValueCountSpinner,
				"Blocking variable values:", blockingVariableValues, blockingSetButton, blockingBottomPanel).getPanel();
		final JScrollPane sp = new JScrollPane(panel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(null);
		ret.setContentPane(sp);
		ret.pack();
		Dimension oldD = ret.getPreferredSize();
		ret.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
										   oldD.height + sp.getHorizontalScrollBar().getHeight()));
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		oldD = ret.getPreferredSize();
		final Dimension newD = GUIUtils.getPreferredSize(ret);
		if (!oldD.equals(newD)) 
			ret.setPreferredSize(newD);
		ret.pack();
		return ret;
	}

	/**
	 * A method that fills the active (selected seed type) text area with step values. 
	 */
	private void fillChosenTextArea() {
		long from = 0, to = 0, step = 0;
		JTextField whichWentWrong = null;
		try {
			from = Long.parseLong(fillFromField.getText().trim());
		} catch (NumberFormatException e) {
			whichWentWrong = fillFromField;
		}
		if (whichWentWrong == null) {
			try {
				to = Long.parseLong(fillToField.getText().trim());
			} catch (NumberFormatException e) {
				whichWentWrong = fillToField;
			}
		}
		if (whichWentWrong == null) {
			try {
				step = Long.parseLong(fillStepField.getText().trim());
			} catch (NumberFormatException e) {
				whichWentWrong = fillStepField;
			}
		}
		if (whichWentWrong == null && step == 0) {
			whichWentWrong = fillStepField;
		}
		if (whichWentWrong == null) {
			String toFill = "";
			if (from < to && step > 0) {
				for (long i = from; i <= to; i += step) {
					toFill = toFill + i + " ";
				}
			} else if (from > to && step > 0) {
				for (long i = from; i >= to; i -= step) {
					toFill = toFill + i + " ";
				}
			} else if (from < to && step < 0) {
				for (long i = to; i >= from; i += step) {
					toFill = toFill + i + " ";
				}
			} else if (from > to && step < 0) {
				for (long i = from; i >= to; i += step) {
					toFill = toFill + i + " ";
				}
			} else if (from == to) {
				toFill = toFill + from;
			}
			if (sweepRandomRadio.isSelected()) {
				sweepListDefArea.setText(toFill);
			} else if (sequenceRandomRadio.isSelected()) {
				seqListDefArea.setText(toFill);
			}
		} else {
			requestFocusAndSelectAll(whichWentWrong);
		}
	}

	private void removeFromRandomSeeds(String seedName) {
		model.removeFromRandomSeeds(seedName);
		updateSeedsLists();
	}

	/**
	 * Checks the inputs for the random seed, alerts user if something is wrong, and updates the random seed info.
	 */
	private void modifyInfo() {
		if (seedsList.getSelectedIndex() != -1) {
			ParameterRandomInfo info = model.randomSeedParameters.get(seedsList.getSelectedIndex());
			int rndType = -1;
			Object defValue = null;
			long stepStart = -1;
			long stepStep = -1;
			Vector<Object> seedList = null;
			boolean combined = true;

			if (sequenceRandomRadio.isSelected()) {
				rndType = ParameterRandomInfo.SEQ_RND;
				seedList = RngSeedManipulatorModel.parseTypeVector(seqListDefArea.getText(), info.getType());
				if (seedList.size() > 0) {
					info.setRndType(rndType);
					info.setSeedList(seedList);
				} else {
					Utilities.userAlert(randomSeedManipulatorDialog, "The list is empty.",
							"Please enter some seed values separated with spaces!");
				}
			} else if (stepRandomRadio.isSelected()) {
				rndType = ParameterRandomInfo.STEP_RND;
				stepStart = new Long(stepStartField.getText().trim());
				stepStep = new Long(stepStepField.getText().trim());
				info.setRndType(rndType);
				info.setStepStart(stepStart);
				info.setStepStep(stepStep);
			} else if (sweepRandomRadio.isSelected()) {
				rndType = ParameterRandomInfo.SWEEP_RND;
				seedList = RngSeedManipulatorModel.parseTypeVector(sweepListDefArea.getText(), info.getType());
				combined = combinedSweepCheckBox.isSelected();
				//combined = true;
				if (seedList.size() > 0) {
					info.setRndType(rndType);
					info.setSeedList(seedList);
					info.setCombined(combined);
				} else {
					Utilities.userAlert(randomSeedManipulatorDialog, "The list is empty.",
							"Please enter some seed values separated with spaces!");
				}
			} else if (randomRandomRadio.isSelected()) {
				rndType = ParameterRandomInfo.RANDOM_RND;
				info.setRndType(rndType);
			} else if (defaultRandomRadio.isSelected()) {
				rndType = ParameterRandomInfo.DEF_RND;
				if (ParameterRandomInfo.isValid(defValueField.getText(), info.getType())) {
					defValue = ParameterRandomInfo.getValue(defValueField.getText(), info.getType());
					info.setRndType(rndType);
					info.setValue(defValue);
					for (int i = 0; i < model.possibleRandomSeedParameters.size(); i++) {
						if (model.possibleRandomSeedParameters.get(i).getName().equals(info.getName())) {
							model.possibleRandomSeedParameters.get(i).setValue(info.getValue());
						}
					}
				} else {
					Utilities.userAlert(randomSeedManipulatorDialog, "The value entered (" + defValueField.getText()
							+ ") is not compatible with the type (" + info.getType() + ")");
				}
			}
			updateSeedsLists();
		}
	}

	/**
	 * Parses a whitespace-separated sequence of long numbers and returns them in a Vector.
	 * If a NumberFormatException occurs during parsing, the element is simply omitted. 
	 * @param text	The text to be parsed.
	 * @return	A Vector of Objects containing Long numbers.
	 */
	public static Vector<Object> parseLongVector(String text) {
		Vector<Object> ret = new Vector<Object>();
		text = text.replaceAll("[\\s]+", " ").trim();
		String[] elements = text.split(" ");
		for (String element : elements) {
			try {
				ret.add(new Long(element.trim()));
			} catch (NumberFormatException e) {
			}
		}
		return ret;
	}

	/**
	 * Parses a whitespace-separated sequence of int numbers and returns them in a Vector. 
	 * If a NumberFormatException occurs during parsing, the element is simply omitted. 
	 * @param text	The text to be parsed.
	 * @return	A Vector of Objects containing Integer numbers.
	 */
	public static Vector<Object> parseIntegerVector(String text) {
		Vector<Object> ret = new Vector<Object>();
		text = text.replaceAll("[\\s]+", " ").trim();
		String[] elements = text.split(" ");
		for (String element : elements) {
			try {
				ret.add(new Integer(element.trim()));
			} catch (NumberFormatException e) {
			}
		}
		return ret;
	}

	/**
	 * Creates if necessary and returns the seeds list panel that can be used inside an IntelliSweep method's GUI.
	 * @return	A JPanel instance with the list of seeds in it and buttons to show the random seed settings dialog, 
	 * the natural variation or the blocking settings dialog.
	 */
	public JPanel getSeedsListPanel() {
		if (seedsListPanel == null) {
			seedsListPanel = new JPanel(new BorderLayout());
			seedsListPanel.setBorder(BorderFactory.createTitledBorder("Random seeds (Replications):"));
			seedsListPanel.add(seedsScr2, BorderLayout.CENTER);
			JPanel seedPanelButtonPanel = new JPanel(new FlowLayout());
			seedPanelButtonPanel.add(rngSeedManipulatorButton);
			seedPanelButtonPanel.add(naturalVariationButton2);
			seedPanelButtonPanel.add(blockingSmallPanelButton);
			seedsListPanel.add(seedPanelButtonPanel, BorderLayout.SOUTH);
		}
		return seedsListPanel;
	}

	/**
	 * Creates (if necessary) the random seed manipulator dialog and shows it.
	 */
	public void showDialog() {
		if (randomSeedManipulatorDialog == null) {
			randomSeedManipulatorDialog = new JDialog();
			randomSeedManipulatorDialog.setTitle("Random Seed Manipulation (Replications)");
			randomSeedManipulatorDialog.setModal(true);
			randomSeedManipulatorDialog.setPreferredSize(new Dimension(550,660));
			final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			sp.setBorder(null);
			randomSeedManipulatorDialog.setContentPane(sp);
			randomSeedManipulatorDialog.pack();
			Dimension oldD = randomSeedManipulatorDialog.getPreferredSize();
			randomSeedManipulatorDialog.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
											   						   oldD.height + sp.getHorizontalScrollBar().getHeight()));
			sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			oldD = randomSeedManipulatorDialog.getPreferredSize();
			final Dimension newD = GUIUtils.getPreferredSize(randomSeedManipulatorDialog);
			if (!oldD.equals(newD)) 
				randomSeedManipulatorDialog.setPreferredSize(newD);
			randomSeedManipulatorDialog.pack();
		}
		//selecting the last selected one, or the first one in the list:
		if (seedsList.getModel().getSize() > 0 && seedsList.getSelectedIndex() == -1) seedsList.setSelectedIndex(0);
		randomSeedManipulatorDialog.setVisible(true);
	}

	/**
	 * Delegated from {@link RngSeedManipulatorModel}
	 */
	public boolean isBlockingConsistent() {
		return model.isBlockingConsistent();
	}
	/**
	 * Delegated from {@link RngSeedManipulatorModel}
	 */
	public boolean isNaturalVariationConsistent() {
		return model.isNaturalVariationConsistent();
	}
	
	/**
	 * @return A ListSelectionListener for the seeds list. Sets other parts of the GUI as seeds get selected.
	 */
	private ListSelectionListener getSeedsListSelectionListener() {
		ListSelectionListener ret = new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				combinedSweepCheckBox.setSelected(false);
				int selected = seedsList.getSelectedIndex();
				if (selected >= 0 && selected < model.randomSeedParameters.size()) {
					model.lastSelectedIdx = selected;
					ParameterRandomInfo info = model.randomSeedParameters.get(selected);
					seqListDefArea.setText("");
					sweepListDefArea.setText("");
					stepStartField.setText("");
					stepStepField.setText("");
					StringBuilder sb = null;
					switch (info.getRndType()) {
					case ParameterRandomInfo.DEF_RND:
						defaultRandomRadio.setSelected(true);
						if (info.getValue() != null) defValueField.setText(info.getValue().toString());
						grabFocusAfterTimeout(defValueField, 125);
						break;
					case ParameterRandomInfo.SEQ_RND:
						sequenceRandomRadio.setSelected(true);
						sb = new StringBuilder();
						for (Object value : info.getSeedList()) {
							sb.append(value);
							sb.append(" ");
						}
						seqListDefArea.setText(sb.toString());
						grabFocusAfterTimeout(seqListDefArea, 125);
						break;
					case ParameterRandomInfo.STEP_RND:
						stepRandomRadio.setSelected(true);
						stepStartField.setText("" + info.getStepStart());
						stepStepField.setText("" + info.getStepStep());
						grabFocusAfterTimeout(stepStartField, 125);
						break;
					case ParameterRandomInfo.SWEEP_RND:
						sweepRandomRadio.setSelected(true);
						if (info.isCombined()) {
							combinedSweepCheckBox.setSelected(true);
						} else {
							combinedSweepCheckBox.setSelected(false);
						}
						sb = new StringBuilder();
						for (Object value : info.getSeedList()) {
							sb.append(value);
							sb.append(" ");
						}
						sweepListDefArea.setText(sb.toString());
						grabFocusAfterTimeout(sweepListDefArea, 125);
						break;
					case ParameterRandomInfo.RANDOM_RND:
						randomRandomRadio.setSelected(true);
						break;
					default:
						break;
					}
				} else {
					if (model.lastSelectedIdx >= 0 && model.lastSelectedIdx < model.randomSeedParameters.size()) {
						seedsList.setSelectedIndex(model.lastSelectedIdx);
					}
				}
				setEnabledDisabled();
			}
		};
		return ret;
	}

	/**
	 * Jumps to a text field and selects all its contents. Used when something needs to be changed in that 
	 * text field.
	 * @param tf
	 */
	private void requestFocusAndSelectAll(JTextField tf) {
		tf.requestFocusInWindow();
		tf.setSelectionStart(0);
		tf.setSelectionEnd(tf.getText().length());
	}

	private void setEnabledDisabled() {
		boolean def = false;
		boolean seq = false;
		boolean step = false;
		boolean sweep = false;
		boolean fill = false;
		if (sequenceRandomRadio.isSelected()) {
			seq = true;
			fill = true;
		} else if (stepRandomRadio.isSelected()) {
			step = true;
		} else if (sweepRandomRadio.isSelected()) {
			sweep = true;
			fill = true;
		} else if (defaultRandomRadio.isSelected()) {
			def = true;
		}
		defValueField.setEnabled(def);
		combinedSweepCheckBox.setEnabled(sweep);
		sweepListDefArea.setEnabled(sweep);
		sweepListDefScr.setEnabled(sweep);
		stepStartField.setEnabled(step);
		stepStepField.setEnabled(step);
		seqListDefArea.setEnabled(seq);
		seqListDefScr.setEnabled(seq);
		fillPanel.setEnabled(fill);
		fillFromField.setEnabled(fill);
		fillOKButton.setEnabled(fill);
		fillStepField.setEnabled(fill);
		fillToField.setEnabled(fill);
	}

	/**
	 * Delegates to the {@link RngSeedManipulatorModel}
	 */
	public int getRunsMultiplierFactor() {
		return model.getRunsMultiplierFactor();
	}

	/**
	 * Adds a change listener for random seed changes. Listeners are notified on seed changes.
	 * @param o The listener to add.
	 */
	public void addChangeListener(IRngSeedManipulatorChangeListener o) {
		model.rngSeedManipulatorChangeListeners.add(o);
	}

	/**
	 * Removes the change listener for random seed changes. 
	 * @param o The listener to remove.
	 */
	public void removeChangeListener(Object o) {
		model.rngSeedManipulatorChangeListeners.remove(o);
	}

	/**
	 * Loads the random seed management settings from the XML element in the argument, then update the GUI.
	 * @param rsmElem The element that contains the settings of the random seeds.
	 */
	public void load(Element rsmElem) {
		model.load(rsmElem);
		naturalVariationCheckBoxes = new Vector<JCheckBox>();
		for (int i = 0; i < model.naturalVariationInfos.size(); i++) {
			naturalVariationCheckBoxes.add(new JCheckBox(model.naturalVariationInfos.get(i).getName(), model.naturalVariationInfos.get(i).isSelected()));
		}
		updateNaturalVariationCheckBoxes();
		updateSeedsLists();
	}

	/**
	 * Saves the random seed settings to the XML node in the argument. Delegates to {@link RngSeedManipulatorModel}.
	 * @param rsmNode	The node where the settings are saved.
	 */
	public void save(Node rsmNode) {
		model.save(rsmNode);
	}

	/* Handles double-click on the seeds list panel inserted into the IntelliSweep method's GUI: opens the 
	 * dialog with the double-clicked seed selected. 
	 * (non-Javadoc)
	 * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e) {
		if (e.getComponent().equals(seedsList2)) {
			if (e.getClickCount() == 2) {
				int index = seedsList2.locationToIndex(e.getPoint());
				ListModel dlm = seedsList2.getModel();
				@SuppressWarnings("unused")
				Object item = dlm.getElementAt(index);
				;
				seedsList2.ensureIndexIsVisible(index);
				seedsList.setSelectedIndex(index);
				rngSeedManipulatorButton.doClick();
			}
		}
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
	}

	public void mouseReleased(MouseEvent e) {
	}

	/**
	 * Creates and returns the natural variation settings dialog. 
	 * @return A JDialog instance.
	 */
	private JDialog getNaturalVariationDialog() {
		JDialog ret = new JDialog();
		ret.setTitle("Select parameters to study their natural variation");
		int rows = naturalVariationCheckBoxes.size() + 1;
		JPanel natVarPanel = new JPanel(new GridLayout(rows, 1));
		for (int i = 0; i < naturalVariationCheckBoxes.size(); i++) {
			natVarPanel.add(naturalVariationCheckBoxes.get(i));
		}
		JPanel natVarDialButtonPanel = new JPanel(new FlowLayout());
		natVarDialButtonPanel.add(naturalVariationSelectAllButton);
		natVarDialButtonPanel.add(naturalVariationCloseButton);
		natVarPanel.add(natVarDialButtonPanel);
		final JScrollPane sp = new JScrollPane(natVarPanel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(null);
		ret.setContentPane(sp);
		ret.pack();
		Dimension oldD = ret.getPreferredSize();
		ret.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
										   oldD.height + sp.getHorizontalScrollBar().getHeight()));
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		oldD = ret.getPreferredSize();
		final Dimension newD = GUIUtils.getPreferredSize(ret);
		if (!oldD.equals(newD)) 
			ret.setPreferredSize(newD);
		ret.pack();
		return ret;
	}

	/**
	 * A hacking method that starts a waiting thread, which waits for the given amount of time, then transfers the
	 * focus to the given component. Useful in GUI building.
	 * @param component	The component to give the focus to.
	 * @param timeMillis	Waiting time in milliseconds.
	 */
	public static void grabFocusAfterTimeout(final JComponent component, final long timeMillis) {
		Thread hackThread = new Thread(new Runnable() {
			// do this because <component>.grabFocus() does nothing if the
			// <component> is not visible
			Object o = new Object();

			public void run() {
				try {
					synchronized (o) {
						o.wait(timeMillis);
					}
				} catch (InterruptedException e) {
				}
				component.grabFocus();
			}
		});
		hackThread.setName("Support-Thread-0");
		hackThread.start();
	}

	// ListSelectionListener.valueChanged:
	// used for the blocking settings
	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource().equals(blockingVariablesList) || e.getSource().equals(blockingVariablesList.getModel())) {
			displayBlockingInfo(getSelectedBlockingInfo());
		}

	}

	/**
	 * Displays the given blocking info in the blocking settings dialog.
	 * @param info
	 */
	private void displayBlockingInfo(RngSeedManipulatorModel.BlockingParameterInfo info) {
		if (info != null) {
			blockingIsBlockingCheckBox.setSelected(info.isBlocking());
			enableDisableBlockingControls(info.isBlocking());
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < info.getBlockingValues().size() - 1; i++) {
				sb.append(info.getBlockingValues().get(i).toString());
				sb.append(" ");
			}
			if (info.getBlockingValues().size() > 0) {
				sb.append(info.getBlockingValues().get(info.getBlockingValues().size() - 1).toString());
			}
			blockingVariableValues.setText(sb.toString());
			blockingVariableValueCountSpinner.setValue(info.getSize());
		}
	}

	/**
	 * Sets the blocking info from the GUI elements.
	 * @param info
	 */
	private void setBlockingInfo(RngSeedManipulatorModel.BlockingParameterInfo info) {
		info.setBlocking(blockingIsBlockingCheckBox.isSelected());
		info.setBlockingValues(RngSeedManipulatorModel.parseTypeVector(blockingVariableValues.getText(), info.getType()));
		info.setSize(Integer.parseInt(blockingVariableValueCountSpinner.getValue().toString()));
	}

	public RngSeedManipulatorModel getModel() {
		return model;
	}

	public String[] getRandomSeedNames() {
		return model.getRandomSeedNames();
	}

	/* Called when a checkbox in the natural variation dialog is changed. Updates the list of natural variation 
	 * infos in the model.
	 * (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent ie) {
		model.naturalVariationInfos = new ArrayList<NaturalVariationInfo>();
		for (int i = 0; i < naturalVariationCheckBoxes.size(); i++) {
			model.naturalVariationInfos.add(new NaturalVariationInfo(naturalVariationCheckBoxes.get(i).getText(), naturalVariationCheckBoxes.get(i).isSelected()));
		}
	}

}
