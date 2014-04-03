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
package ai.aitia.meme.intelliResultProcess.rsm;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.processing.model.PolynomialRegressor;
import ai.aitia.meme.processing.model.SampleQueryModel;
import ai.aitia.meme.processing.model.VariableInfo;
import ai.aitia.meme.processing.model.PolynomialRegressor.CannotComputeLeastSquaresException;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

public class PolynomialRsmGui implements ActionListener, ListSelectionListener{
	
	protected PolynomialResponseModel responseModel = null;
	protected PolynomialRsmInfo selectedRegressor = null;

	protected JDialog modelListDialog = null;
	protected JPanel modelListPanel = null;
	protected JList polyRegressorList = null;
	protected JScrollPane polyRegressorScr = null;
	protected JTextArea polyInfoArea = new JTextArea("No model selected");
	protected JScrollPane polyInfoScr = new JScrollPane(polyInfoArea);
	protected JButton addPolyRegressorButton = new JButton("Create regressor...");
	protected JButton removePolyRegressorButton = new JButton("Remove...");
	protected JButton createChartButton2 = new JButton("Create chart...");
	protected JButton closeResponseModelDialogButton = new JButton("Close");
	
	protected JDialog createPolynomialDialog = null;
	protected JPanel createPolynomialPanel = null;
	protected JList inputParametersList = null;
	protected JScrollPane inputParamatersScr = null;
	protected JButton selectInputParametersButton = new JButton("Select input parameters...");
	protected JButton createRegressorButton = new JButton("Create");
	protected JButton closeRegressorButton = new JButton("Close");
	protected JButton increaseOrderButton = new JButton("Increase order");
	protected JButton decreaseOrderButton = new JButton("Decrease order");
	protected JComboBox outputSelectComboBox = null;
	protected JDialog selectInputParametersDialog = null;
	protected JPanel selectInputParametersPanel = null;
	protected JButton closeSelectInputParametersButton = new JButton("Close");
	protected Vector<JCheckBox> selectParametersCheckBoxes = null;
	
	protected JDialog createChartDialog = null;
	protected JPanel createChartPanel = null;
	protected JLabel input1Label = new JLabel("xxxxxx");
	protected JLabel input2Label = new JLabel("yyyyyy");
	protected JComboBox input1ComboBox = null;
	protected JComboBox input2ComboBox = null;
	protected JTextField input1HighField = new JTextField(25);
	protected JTextField input1LowField = new JTextField(25);
	protected JTextField input2HighField = new JTextField(25);
	protected JTextField input2LowField = new JTextField(25);
	protected JTextField otherInputsValueField = new JTextField(40);
	protected JList otherInputsList = null;
	protected JScrollPane otherInputsScr = null;
	protected JButton createChartButton = new JButton("OK");
	protected JButton cancelChartButton = new JButton("Cancel");
	protected JComboBox resolutionComboBox = new JComboBox(new String[]{LOW_RES, MEDIUM_RES, HIGH_RES});
	
	Vector<PolynomialRsmInfo> polyInfos = null;
	Vector<VariableInfo> inputParameterInfos  = null;
	
	public static final String LOW_RES = "Low";
	public static final String MEDIUM_RES = "Medium";
	public static final String HIGH_RES = "High";

	private static final String ADD_REGRESSOR = "add regressor";
	private static final String REMOVE_REGRESSOR = "remove regressor";
	private static final String CREATE_CHART_DIALOG = "create chart button pressed on modellist dialog";
	private static final String CLOSE_RESPONSE_MODEL_DIALOG = "close response model dialog";
	private static final String SELECT_INPUT_PARAMETERS = "select input parameters";
	private static final String CLOSE_INPUT_PARAMETERS = "close input parameters dialog";
	private static final String CREATE_REGRESSOR = "create regressor";
	private static final String CLOSE_CREATE_REGRESSOR = "close create regressor dialog";
	private static final String INCREASE_VARIABLE_ORDER = "increase variable order";
	private static final String DECREASE_VARIABLE_ORDER = "decrease variable order";
	private static final String SELECT_INPUT1 = "select input1";
	private static final String SELECT_INPUT2 = "select input2";
	private static final String INPUT1_LOW_ENTER = "input1 low enter pressed";
	private static final String INPUT1_HIGH_ENTER = "input1 high enter pressed";
	private static final String INPUT2_LOW_ENTER = "input2 low enter pressed";
	private static final String INPUT2_HIGH_ENTER = "input2 high enter pressed";
	private static final String OTHER_INPUT_VALUE_ENTER = "other input value enter pressed";
	private static final String CREATE_CHART = "create chart";
	private static final String CANCEL_CHART = "cancel chart";
	private static final String SELECT_RESOLUTION = "select resolution/detail";
	private static final String SELECT_OUTPUT = "select output";

	
	public PolynomialRsmGui(PolynomialResponseModel responseModel){
		this.responseModel = responseModel;
		generatePolyInfos();
		//create objects:
		polyRegressorList = new JList(polyInfos);
		polyRegressorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		polyRegressorScr = new JScrollPane(polyRegressorList);
		modelListDialog = new JDialog();
		modelListDialog.setTitle("Response Surface Models");
			
		inputParametersList = new JList();
		inputParametersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		inputParamatersScr = new JScrollPane(inputParametersList);
		outputSelectComboBox = new JComboBox();
		createPolynomialDialog = new JDialog(modelListDialog, "Create a new polynomial");
		
		input1ComboBox = new JComboBox();
		input2ComboBox = new JComboBox();
		otherInputsList = new JList();
		otherInputsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		otherInputsScr = new JScrollPane(otherInputsList);
		createChartDialog = new JDialog(modelListDialog, "Create a chart");
		
		polyInfoArea.setEditable(false);
		polyInfoArea.setLineWrap(true);

		//make the components work:
		initialize();
		addListeners();
	}
	
	private void initialize() {
		modelListPanel = FormsUtils.build(
				"~ p ~ f:p:g ~", 
				"01 f:p:g||" +
				"22 p||", 
				polyRegressorScr, polyInfoScr,
				FormsUtils.buttonStack(addPolyRegressorButton, removePolyRegressorButton, createChartButton2, closeResponseModelDialogButton).getPanel()
		).getPanel();
		modelListDialog.setContentPane(modelListPanel);
		modelListDialog.pack();

		createPolynomialPanel = FormsUtils.build(
				"~ f:p:g ~ p ~", 
				"01 p||" +
				"23 p||" +
				"24 p||" +
				"2_ f:p:g||" +
				"5_ p||" +
				"6_ p||" +
				"7_ p||" +
				"8_ p||", 
				"Input parameters:", "Order",
				inputParamatersScr, increaseOrderButton,
				decreaseOrderButton,
				selectInputParametersButton,
				"Output:",
				outputSelectComboBox,
				FormsUtils.buttonStack(createRegressorButton, closeRegressorButton).getPanel()
		).getPanel();
		createPolynomialDialog.setContentPane(createPolynomialPanel);
		createPolynomialDialog.pack();
		createChartPanel = FormsUtils.build(
				"~ p ~ p ~ p ~ p ~",
				"__01 p||" +
				"2345 p||" +
				"6789 p||" +
				"AABB p||" +
				"CCDD p||" +
				"CC__ f:p:g||" +
				"EF__ p||" +
				"GH__ p||",
				"Low", "High",
				"Input1:", input1ComboBox, input1LowField, input1HighField,
				"Input2:", input2ComboBox, input2LowField, input2HighField,
				"Other inputs:", "Value of selected input:",
				otherInputsScr, otherInputsValueField,
				"Resolution:", resolutionComboBox,
				createChartButton, cancelChartButton
		).getPanel();
		createChartDialog.setContentPane(createChartPanel);
		createChartDialog.pack();
	}

	private void addListeners() {
		addPolyRegressorButton.setActionCommand(ADD_REGRESSOR);
		removePolyRegressorButton.setActionCommand(REMOVE_REGRESSOR);
		createChartButton2.setActionCommand(CREATE_CHART_DIALOG);
		closeResponseModelDialogButton.setActionCommand(CLOSE_RESPONSE_MODEL_DIALOG);
		selectInputParametersButton.setActionCommand(SELECT_INPUT_PARAMETERS);
		closeSelectInputParametersButton.setActionCommand(CLOSE_INPUT_PARAMETERS);
		createRegressorButton.setActionCommand(CREATE_REGRESSOR);
		closeRegressorButton.setActionCommand(CLOSE_CREATE_REGRESSOR);
		increaseOrderButton.setActionCommand(INCREASE_VARIABLE_ORDER);
		decreaseOrderButton.setActionCommand(DECREASE_VARIABLE_ORDER);
		input1ComboBox.setActionCommand(SELECT_INPUT1);
		input2ComboBox.setActionCommand(SELECT_INPUT2);
		input1LowField.setActionCommand(INPUT1_LOW_ENTER);
		input1HighField.setActionCommand(INPUT1_HIGH_ENTER);
		input2LowField.setActionCommand(INPUT2_LOW_ENTER);
		input2HighField.setActionCommand(INPUT2_HIGH_ENTER);
		otherInputsValueField.setActionCommand(OTHER_INPUT_VALUE_ENTER);
		createChartButton.setActionCommand(CREATE_CHART);
		cancelChartButton.setActionCommand(CANCEL_CHART);
		resolutionComboBox.setActionCommand(SELECT_RESOLUTION);
		outputSelectComboBox.setActionCommand(SELECT_OUTPUT);
		GUIUtils.addActionListener(this, addPolyRegressorButton, removePolyRegressorButton,
				closeResponseModelDialogButton, selectInputParametersButton, createRegressorButton, 
				increaseOrderButton, decreaseOrderButton, input1HighField, input1LowField,
				input2HighField, input2LowField, otherInputsValueField, createChartButton, 
				cancelChartButton, input1ComboBox, input2ComboBox, resolutionComboBox, 
				outputSelectComboBox, createChartButton2, closeRegressorButton, 
				closeSelectInputParametersButton);
		polyRegressorList.addListSelectionListener(this);
		inputParametersList.addListSelectionListener(this);
		otherInputsList.addListSelectionListener(this);
	}

	public void generatePolyInfos(){
		polyInfos = new Vector<PolynomialRsmInfo>();
		for (int i = 0; i < responseModel.responses.size(); i++) {
			polyInfos.add(new PolynomialRsmInfo(responseModel.responses.get(i)));
		}
	}
	
	public void refreshPolyInfos(){
		generatePolyInfos();
		DefaultListModel lm = new DefaultListModel();
		for (PolynomialRsmInfo polyInfo : polyInfos) {
			lm.addElement(polyInfo);
			//System.out.println(polyInfo.getPolynomialAsString());
		}
		polyRegressorList.setModel(lm);
		polyRegressorList.addListSelectionListener(this);
//		polyRegressorScr.setVisible(false);
//		polyRegressorScr.setVisible(true);
	}

	public SampleQueryModel getSampleModel(){
		return responseModel.sampleModel;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(ADD_REGRESSOR)){
			selectInputParametersDialog = null;
			inputParametersList.setModel(new DefaultListModel());
			showCreatePolynomialDialog();
		} else if (e.getActionCommand().equals(CREATE_CHART_DIALOG)){
			showCreateChartDialog();
		} else if (e.getActionCommand().equals(CLOSE_RESPONSE_MODEL_DIALOG)){
			modelListDialog.setVisible(false);
		} else if (e.getActionCommand().equals(CANCEL_CHART)){
			createChartDialog.setVisible(false);
		} else if (e.getActionCommand().equals(CLOSE_CREATE_REGRESSOR)){
			createPolynomialDialog.setVisible(false);
		} else if (e.getActionCommand().equals(CLOSE_INPUT_PARAMETERS)){
			selectInputParametersDialog.setVisible(false);
			inputParameterInfos = new Vector<VariableInfo>();
			for (int i = 0; i < selectParametersCheckBoxes.size(); i++) {
				if (selectParametersCheckBoxes.get(i).isSelected()){
					VariableInfo vi = new VariableInfo(selectParametersCheckBoxes.get(i).getText(), 2);
					vi.setMin(responseModel.getSampleModel().getSampleMin(vi.getName()));
					vi.setMax(responseModel.getSampleModel().getSampleMax(vi.getName()));
					vi.setCenter(responseModel.getSampleModel().getSampleCenter(vi.getName()));
					vi.setIntervalStart(vi.getMin());
					vi.setIntervalEnd(vi.getMax());
					vi.setIntervalCenter(vi.getCenter());
					inputParameterInfos.add(vi);
				}
			}
			inputParametersList = new JList(inputParameterInfos);
			inputParametersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			inputParametersList.addListSelectionListener(this);
			inputParamatersScr.setViewportView(inputParametersList);
		} else if (e.getActionCommand().equals(SELECT_INPUT_PARAMETERS)){
			showSelectInputParametersDialog();
		} else if (e.getActionCommand().equals(INCREASE_VARIABLE_ORDER)){
			if(inputParametersList.getSelectedIndex() > -1){
				inputParameterInfos.get(inputParametersList.getSelectedIndex()).increaseOrder();
				inputParametersList.setVisible(false);
				inputParametersList.setVisible(true);
			}
		} else if (e.getActionCommand().equals(DECREASE_VARIABLE_ORDER)){
			if(inputParametersList.getSelectedIndex() > -1){
				inputParameterInfos.get(inputParametersList.getSelectedIndex()).decreaseOrder();
				inputParametersList.setVisible(false);
				inputParametersList.setVisible(true);
			}
		} else if (e.getActionCommand().equals(CREATE_REGRESSOR)){
			if(inputParameterInfos.size() > 0){
				PolynomialRegressor pr = new PolynomialRegressor(responseModel.getSampleModel(), outputSelectComboBox.getSelectedItem().toString());
				//TODO: MatrixRank iz�biz�t lekezelni
				try {
					pr.createPolynomial(inputParameterInfos);
					responseModel.getResponses().add(pr);
					refreshPolyInfos();
				} catch (CannotComputeLeastSquaresException e1) {
					Utilities.userAlert(createChartDialog, "Cannot create model, because matrix rank is deficient");
				}
				createPolynomialDialog.setVisible(false);
			} else {
				//warning
			}
		} else if (e.getActionCommand().equals(SELECT_INPUT1)){
			int idx = input1ComboBox.getSelectedIndex();
			VariableInfo vi = selectedRegressor.getPoly().getVariableInfos().get(idx);
			double startValue = vi.getIntervalStart();
			double endValue = vi.getIntervalEnd();
			input1LowField.setText(""+startValue);
			input1HighField.setText(""+endValue);
		} else if (e.getActionCommand().equals(SELECT_INPUT2)){
			int idx = input2ComboBox.getSelectedIndex();
			VariableInfo vi = selectedRegressor.getPoly().getVariableInfos().get(idx);
			double startValue = vi.getIntervalStart();
			double endValue = vi.getIntervalEnd();
			input2LowField.setText(""+startValue);
			input2HighField.setText(""+endValue);
		} else if (e.getActionCommand().equals(INPUT1_LOW_ENTER)){
			try {
				int idx = input1ComboBox.getSelectedIndex();
				VariableInfo vi = selectedRegressor.getPoly().getVariableInfos().get(idx);
				double value = Double.parseDouble(input1LowField.getText());
				vi.setIntervalStart(value);
			} catch (NumberFormatException e1) {
				input1LowField.setSelectionStart(0);
				input1LowField.setSelectionEnd(input1LowField.getText().length());
			}
		} else if (e.getActionCommand().equals(INPUT1_HIGH_ENTER)){
			try {
				int idx = input1ComboBox.getSelectedIndex();
				VariableInfo vi = selectedRegressor.getPoly().getVariableInfos().get(idx);
				double value = Double.parseDouble(input1HighField.getText());
				vi.setIntervalEnd(value);
			} catch (NumberFormatException e1) {
				input1HighField.setSelectionStart(0);
				input1HighField.setSelectionEnd(input1HighField.getText().length());
			}
		} else if (e.getActionCommand().equals(INPUT2_LOW_ENTER)){
			try {
				int idx = input2ComboBox.getSelectedIndex();
				VariableInfo vi = selectedRegressor.getPoly().getVariableInfos().get(idx);
				double value = Double.parseDouble(input2LowField.getText());
				vi.setIntervalStart(value);
			} catch (NumberFormatException e1) {
				input2LowField.setSelectionStart(0);
				input2LowField.setSelectionEnd(input2LowField.getText().length());
			}
		} else if (e.getActionCommand().equals(INPUT2_HIGH_ENTER)){
			try {
				int idx = input2ComboBox.getSelectedIndex();
				VariableInfo vi = selectedRegressor.getPoly().getVariableInfos().get(idx);
				double value = Double.parseDouble(input2HighField.getText());
				vi.setIntervalEnd(value);
			} catch (NumberFormatException e1) {
				input2HighField.setSelectionStart(0);
				input2HighField.setSelectionEnd(input2HighField.getText().length());
			}
		} else if (e.getActionCommand().equals(OTHER_INPUT_VALUE_ENTER)){
			try {
				int idx = otherInputsList.getSelectedIndex();
				if (idx > -1){
					VariableInfo vi = selectedRegressor.getPoly().getVariableInfos().get(idx);
					double value = Double.parseDouble(otherInputsValueField.getText());
					vi.setIntervalCenter(value);
				}
			} catch (NumberFormatException e1) {
				otherInputsValueField.setSelectionStart(0);
				otherInputsValueField.setSelectionEnd(otherInputsValueField.getText().length());
			}
		} else if (e.getActionCommand().equals(CREATE_CHART)){
			//egyel�re itt lesz a v�ge, indul a feldolgoz�s
			if(input1ComboBox.getSelectedIndex() != input2ComboBox.getSelectedIndex()){
				
				createChartDialog.setVisible(false);
				modelListDialog.setVisible(false);
			} else throw new RuntimeException("Two identical inputs selected");
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource().equals(polyRegressorList)){
			if(polyRegressorList.getSelectedIndex() > -1){
				selectedRegressor = polyInfos.get(polyRegressorList.getSelectedIndex());
				polyInfoArea.setText(selectedRegressor.getPolynomialAsString());
			} else {
				polyInfoArea.setText("No model selected");
			}
		} else if (e.getSource().equals(inputParametersList)){
			if(inputParametersList.getSelectedIndex() > -1){
				//nothing
			}
		} else if (e.getSource().equals(otherInputsList)){
			if (otherInputsList.getSelectedIndex() > -1){
				int idx = otherInputsList.getSelectedIndex();
				VariableInfo vi = selectedRegressor.getPoly().getVariableInfos().get(idx);
				double centerValue = vi.getIntervalCenter();
				otherInputsValueField.setText(""+centerValue);
			}
		} 
	}
	
	public void showModelListDialog(){
		modelListDialog.setModal(true);
		modelListDialog.setVisible(true);
	}

	protected void showCreatePolynomialDialog(){
		outputSelectComboBox.setModel(new DefaultComboBoxModel(responseModel.getResponseNames().toArray()));
		createPolynomialDialog.setModal(true);
		createPolynomialDialog.setVisible(true);
	}

	protected void showCreateChartDialog(){
		if(polyRegressorList.getSelectedIndex() > -1){
			int i = polyRegressorList.getSelectedIndex();
			input1ComboBox.setModel(new DefaultComboBoxModel(responseModel.getResponses().get(i).getVariableInfos()));
			input2ComboBox.setModel(new DefaultComboBoxModel(responseModel.getResponses().get(i).getVariableInfos()));
			DefaultListModel lm = new DefaultListModel();
			for (int j = 0; j < responseModel.getResponses().get(i).getVariableInfos().size(); j++) {
				lm.addElement(responseModel.getResponses().get(i).getVariableInfos().get(i));
			}
			otherInputsList.setModel(lm);
			createChartDialog.setModal(true);
			createChartDialog.setVisible(true);
		}
	}
	
	protected void showSelectInputParametersDialog(){
		if(selectInputParametersDialog == null){
			selectInputParametersDialog = new JDialog(createPolynomialDialog, "Select input parameters");
			int rows = responseModel.getArgumentNames().size();
			selectInputParametersPanel = new JPanel(new GridLayout(rows+1, 1));
			selectParametersCheckBoxes = new Vector<JCheckBox>(rows);
			for (int i = 0; i < rows; i++) {
				JCheckBox cb = new JCheckBox(responseModel.getArgumentNames().get(i));
				selectInputParametersPanel.add(cb);
				selectParametersCheckBoxes.add(cb);
			}
			selectInputParametersPanel.add(closeSelectInputParametersButton);
			selectInputParametersDialog.setContentPane(selectInputParametersPanel);
			selectInputParametersDialog.pack();
		}
		selectInputParametersDialog.setModal(true);
		selectInputParametersDialog.setVisible(true);
	}

	public PolynomialRsmInfo getSelectedRegressor() {
		return selectedRegressor;
	}
	public int getInput1Index(){
		return input1ComboBox.getSelectedIndex();
	}
	public int getInput2Index(){
		return input2ComboBox.getSelectedIndex();
	}
	public PolynomialResponseModel getResponseModel(){
		return responseModel;
	}
	public Vector<PolynomialRsmInfo> getPolyInfos(){
		return polyInfos;
	}
	public String getResolution(){
		return resolutionComboBox.getSelectedItem().toString();
	}
}
