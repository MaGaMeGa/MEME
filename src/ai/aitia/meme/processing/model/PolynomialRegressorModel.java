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
package ai.aitia.meme.processing.model;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;

import org.jscience.mathematics.function.Polynomial;
import org.jscience.mathematics.function.Term;
import org.jscience.mathematics.function.Variable;
import org.jscience.mathematics.number.Float64;

import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.database.IResultsDbMinimal;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ParameterComb;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.ResultInMem;
import ai.aitia.meme.database.ColumnType.ValueNotSupportedException;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.processing.model.PolynomialRegressor.CannotComputeLeastSquaresException;
import ai.aitia.meme.utils.AbstractPlusMinusButtons;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.GenericListGui;

public class PolynomialRegressorModel extends AbstractRegressorModelDetails implements ActionListener{
	private static final String NEW_TERM_CANCEL = "new term cancel";
	private static final String NEW_TERM_OK = "new term ok";
	private static final String CREATE_NEW_TERM = "create new term";
	private static final String REMOVE_TERM = "remove term";
	private static final String ADD_TERM = "add term";
	public static final int INTERVALS = 20;

	public static class InputInfo {
		protected String name = null;
		protected int order = 0;
		
		public InputInfo(String name, int order) {
			this.name = name;
			this.order = order;
			if (name == null || name.length() == 0) {
				throw new IllegalArgumentException("The name of InputInfo must not be empty or null.");
			}
			if (order < 1) {
				throw new IllegalArgumentException("The order of InputInfo must be a positive integer.");
			}
		}

		public int getOrder() {
			return order;
		}

		public void setOrder(int order) {
			this.order = order;
		}

		public String getName() {
			return name;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("[");
			sb.append(name);
			sb.append(" - order: ");
			sb.append(order);
			sb.append("]");
			return sb.toString();
		}
	}
	
	public static class PolynomialModel {
		protected Vector<String> inputs = null;
		protected String output = null;
		protected PolynomialRegressor regressor = null;
		protected boolean createChart = false;
		protected double polyResponseMin = Double.POSITIVE_INFINITY;
		protected double polyResponseMax = Double.NEGATIVE_INFINITY;
		protected int chartId = -1;
		
		protected Vector<TermInfo> availableTerms = null;
		protected Vector<TermInfo> selectedTerms = null;

		public PolynomialModel(List<String> inputs, String output) {
			this.inputs = new Vector<String>();
			this.inputs.addAll(inputs);
			this.output = output;
			this.availableTerms = new Vector<TermInfo>();
			this.selectedTerms = new Vector<TermInfo>();
			if(inputs.size() < 3) {
				createChart = true; //default 
			}
		}

		@Override
		public String toString() {
			StringBuilder ret = new StringBuilder();
			ret.append("{");
			for (int i = 0; i < inputs.size() - 1; i++) {
				ret.append(inputs.get(i) + ", ");
			}
			ret.append(inputs.get(inputs.size() - 1) + "} -> ");
			ret.append(output);
			return ret.toString();
		}

		public PolynomialRegressor getRegressor() {
			return regressor;
		}

		public void setRegressor(PolynomialRegressor regressor) {
			this.regressor = regressor;
			selectedTerms.clear();
			for (int i = 0; i < regressor.getTermsWithoutOne().size(); i++) {
				TermInfo termInfo = new TermInfo(regressor.getTermsWithoutOne().get(i), regressor.getRegressor());
				selectedTerms.add(termInfo);
			}
			selectedTerms.add(new TermInfo(Term.ONE, regressor.getRegressor()));
//			for (int i = 0; i < regressor.getTermsLeftOutWithoutOne().size(); i++) {
//				TermInfo termInfo = new TermInfo(regressor.getTermsLeftOutWithoutOne().get(i), regressor.getRegressor());
//				availableTerms.add(termInfo);
//			}
		}

		public String getOutput() {
			return output;
		}

		public List<String> getInputs() {
			return inputs;
		}

		public boolean isCreateCharts() {
			return createChart;
		}

		public void setCreateChart(boolean selected) {
			createChart = selected;
		}
		
	}
	
	public static class TermInfo {
		protected Term term = null;
		protected Polynomial<Float64> polynomial = null;
		
		public TermInfo(Term term, Polynomial<Float64> poly) {
			this.term = term;
			this.polynomial = poly;
		}
		
		@Override
		public String toString() {
			Float64 coef = polynomial.getCoefficient(term);
			if(term.equals(Term.ONE)) return "" + (coef == null ? "0" : coef.doubleValue());
			StringBuilder ret = new StringBuilder("" + (coef == null ? "0" : coef.doubleValue()) + "*");
			for (int i = 0; i < term.size() - 1; i++) {
				ret.append(term.getVariable(i).getSymbol());
				if (term.getPower(i)>1) {
					ret.append("^");
					ret.append(term.getPower(i));
				}
				ret.append("*");
			}
			if(term.size() > 0){
				ret.append(term.getVariable(term.size() - 1).getSymbol());
				if (term.getPower(term.size() - 1)>1) {
					ret.append("^");
					ret.append(term.getPower(term.size() - 1));
				}
			}
			return ret.toString();
		}

		public Term getTerm() {
			return term;
		}
	}
	
	protected GenericListGui<InputInfo> inputs = null;
	protected GenericListGui<PolynomialModel> polynomials = null;
	protected AbstractPlusMinusButtons inputOrderButtons = null;
	protected JPanel mainPolynomialModelPanel = null;
	protected JPanel polynomialPanel = null;
	protected PolynomialModel oldPolynomial = null;

	protected GenericListGui<TermInfo> availableTermList = null;
	protected GenericListGui<TermInfo> selectedTermList = null;
	protected JButton addButton = null;
	protected JButton removeButton = null;
	protected JButton createNewTermButton = null;
	protected JCheckBox createChartCheckBox = null;
	//newTermDialog components:
	protected JDialog newTermDialog = null;
	protected GenericListGui<InputInfo> termElements = null;
	protected JTextField termDisplay = null;
	protected JButton newTermOkButton = null;
	protected JButton newTermCancelButton = null;
	protected AbstractPlusMinusButtons termOrderButtons = null;
	protected Term term = null;
	
	protected SampleQueryModel sampleModel;
	protected String name = null;
	protected JTextField nameField= null;
	protected GenericModelSelectorGui genericModelSelector = null;
	
	public PolynomialRegressorModel(SampleQueryModel sampleModel, List<String> inputs, List<String> outputs, GenericModelSelectorGui genericModelSelector0) {
		super(inputs, outputs);
		this.genericModelSelector = genericModelSelector0;
		this.sampleModel = sampleModel;
		this.inputs = new GenericListGui<InputInfo>(150,250);
		for (int i = 0; i < inputs.size(); i++) {
			InputInfo toAdd = new InputInfo(inputs.get(i), 1);
			this.inputs.add(toAdd);
		}
		this.inputs.getPanel().setBorder(BorderFactory.createTitledBorder("Inputs"));
		polynomials = new GenericListGui<PolynomialModel>(150,250){
			@Override
			public void listSelectionChanged(ListSelectionEvent e) {
				if(polynomials.getSelectedIndex() > -1){
					PolynomialModel newPolynomial = polynomials.getSelectedValue();
					displayTermPanel(newPolynomial);
				} else {
					if(polynomials.size() > 0) polynomials.setSelectedIndex(0);
				}
			}
		};
		polynomials.getPanel().setBorder(BorderFactory.createTitledBorder("Polynomials"));
		try {
			createPolynomialsFromInputs();
		} catch (CannotComputeLeastSquaresException e1) {
			e1.printStackTrace();
		}
		addButton = new JButton("->");
		removeButton = new JButton("<-");
		createNewTermButton = new JButton("Create new term...");
		inputOrderButtons = new AbstractPlusMinusButtons("Modify order:"){
			@Override
			public void minusButtonPressed() {
				decreaseInputOrder();
			}
			@Override
			public void plusButtonPressed() {
				increaseInputOrder();
			}
		};
		createChartCheckBox = new JCheckBox("Create chart from polynomial", false);
		createChartCheckBox.setEnabled(false);
		createChartCheckBox.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				if(polynomials.getSelectedIndex() > -1)
					polynomials.getSelectedValue().setCreateChart(createChartCheckBox.isSelected());
			}
		});
		availableTermList = new GenericListGui<TermInfo>(150,250);
		selectedTermList = new GenericListGui<TermInfo>(150,250);
		availableTermList.getPanel().setBorder(BorderFactory.createTitledBorder("Available terms"));
		selectedTermList.getPanel().setBorder(BorderFactory.createTitledBorder("Selected terms"));
		addButton.setActionCommand(ADD_TERM);
		removeButton.setActionCommand(REMOVE_TERM);
		createNewTermButton.setActionCommand(CREATE_NEW_TERM);
		createNewTermButton.setEnabled(false);
		GUIUtils.addActionListener(this, addButton, removeButton, createNewTermButton);
		nameField = new JTextField(30);
		nameField.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e) {
				super.keyPressed(e);
				name = nameField.getText();
			}
		});
		polynomialPanel = FormsUtils.build(
				"~ f:p:g ~ p ~ f:p:g ~", 
				"012 p||" +
				"032 p||" +
				"0_2 f:p:g", 
				availableTermList.getPanel(), addButton, selectedTermList.getPanel(),
				removeButton
				).getPanel();
		JPanel namePanel = new JPanel(new FlowLayout());
		namePanel.add(new JLabel("Name:"));
		namePanel.add(nameField);
		mainPolynomialModelPanel = FormsUtils.build(
				"~ f:p:g ~ p ~ f:p:g ~", 
				"000 p||" +
				"123 p|" +
				"1_3 f:p:g||" +
				"444 f:p:g||" +
				"555 p||", 
//				"6__ p",
				namePanel,
				this.inputs.getPanel(), inputOrderButtons.getPanel(), polynomials.getPanel(),
				polynomialPanel,
				createChartCheckBox
				//createNewTermButton
				).getPanel();
		polynomials.setSelectedIndex(0);
	}
	
	protected void decreaseInputOrder() {
		modifySelectedOrders(-1, 1);
	}

	protected void increaseInputOrder() {
		modifySelectedOrders(1, 1);
	}
	
	public void modifySelectedOrders(int difference, int min){
		List<InputInfo> selected = inputs.getSelectedValues();
		for (InputInfo info : selected) {
			int order = info.getOrder();
			int oldOrder = order;
			order += difference;
			if(order < min) order = min;
			info.setOrder(order);
			try {
				createPolynomialsFromInputs();
				inputs.refreshDisplayKeepSelection();
				if (polynomials.getSelectedIndex() < 0 && polynomials.size() > 0) {
					polynomials.setSelectedIndex(0);
				}
				if(polynomials.getSelectedIndex() > -1) {
					displayTermPanel(polynomials.getSelectedValue());
				}
			} catch (CannotComputeLeastSquaresException e) {
				info.setOrder(oldOrder);
			}
		}
	}

	protected void createPolynomialsFromInputs() throws CannotComputeLeastSquaresException {
		polynomials.clear();
		for (String outputName : outputNames) {
			PolynomialRegressor pr = new PolynomialRegressor(sampleModel, outputName);
			int[] degrees = new int[sampleModel.getSampleNames().size()];
			for (int i = 0; i < degrees.length; i++) {
				degrees[i] = 0;
				String sampleColumnName = sampleModel.getSampleNames().get(i);
				for (int j = 0; j < inputs.getList().size(); j++) {
					if(inputs.get(j).getName().equals(sampleColumnName)) {
						degrees[i] = inputs.get(j).getOrder();
					}
				}
			}
			pr.createPolynomial(degrees);
			PolynomialModel polyModel = new PolynomialModel(inputNames, outputName);
			polyModel.setRegressor(pr);
			polynomials.add(polyModel);
		}
	}

	protected void displayTermPanel(PolynomialModel newPolynomial) {
		oldPolynomial = newPolynomial; 
		availableTermList.clear();
		selectedTermList.clear();
		availableTermList.addAll(newPolynomial.availableTerms);
		selectedTermList.addAll(newPolynomial.selectedTerms);
		if (newPolynomial.getInputs().size() < 3) {
			createChartCheckBox.setEnabled(true);
			createChartCheckBox.setSelected(newPolynomial.isCreateCharts());
		} else {
			createChartCheckBox.setEnabled(false);
			createChartCheckBox.setSelected(false);
		}
	}

	public JPanel getPanel(){
		
		return mainPolynomialModelPanel;
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(ADD_TERM)) {
			int[] selectedAvailableTermIndices = availableTermList.getSelectedIndices();
			List<TermInfo> selectedAvailableTerms = availableTermList.getSelectedValues();
			selectedTermList.addAll(selectedAvailableTerms);
			availableTermList.removeIndices(selectedAvailableTermIndices);
			polynomials.getSelectedValue().availableTerms = new Vector<TermInfo>(availableTermList.getList());
			try {
				createPolynomialFromTermList();
			} catch (CannotComputeLeastSquaresException e1) {
				//move back the terms to their original places
				availableTermList.addAll(selectedAvailableTerms);
				selectedTermList.getList().removeAll(selectedAvailableTerms);
				selectedTermList.refreshDisplay();
			}
		} else if (e.getActionCommand().equals(REMOVE_TERM)) {
			int[] selectedSelectedTermIndices = selectedTermList.getSelectedIndices();
			List<TermInfo> selectedSelectedTerms = selectedTermList.getSelectedValues();
			availableTermList.addAll(selectedSelectedTerms);
			selectedTermList.removeIndices(selectedSelectedTermIndices);
			polynomials.getSelectedValue().availableTerms = new Vector<TermInfo>(availableTermList.getList());
			try {
				createPolynomialFromTermList();
			} catch (CannotComputeLeastSquaresException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} else if (e.getActionCommand().equals(CREATE_NEW_TERM)) {
			showCreateNewTermDialog();
		} else if (e.getActionCommand().equals(NEW_TERM_OK)) {
			newTermDialog.setVisible(false);
		} else if (e.getActionCommand().equals(NEW_TERM_CANCEL)) {
			newTermDialog.setVisible(false);
		}
	}

	protected void createPolynomialFromTermList() throws CannotComputeLeastSquaresException {
		PolynomialModel polyModel = polynomials.getSelectedValue();
		Vector<TermInfo> newSelectedTerms = new Vector<TermInfo>(selectedTermList.getList());
		@SuppressWarnings("unused")
		Vector<TermInfo> newAvailableTerms = new Vector<TermInfo>(availableTermList.getList());
		Vector<Term> newTerms = new Vector<Term>();
		for (TermInfo termInfo : newSelectedTerms) {
			newTerms.add(termInfo.getTerm());
		}
		PolynomialRegressor pr = new PolynomialRegressor(sampleModel, polyModel.getOutput());
		pr.computeRegressor(newTerms);
		polyModel.setRegressor(pr);
		displayTermPanel(polyModel);
	}

	protected void showCreateNewTermDialog() {
		if (newTermDialog == null) {
			newTermDialog = new JDialog();
			newTermDialog.setTitle("Create new term");
			newTermOkButton = new JButton("OK");
			newTermCancelButton = new JButton("Cancel");
			newTermOkButton.setActionCommand(NEW_TERM_OK);
			newTermCancelButton.setActionCommand(NEW_TERM_CANCEL);
			GUIUtils.addActionListener(this, newTermOkButton, newTermCancelButton);
			termDisplay = new JTextField(30);
			termElements = new GenericListGui<InputInfo>();
			termOrderButtons = new AbstractPlusMinusButtons(){
				@Override
				public void minusButtonPressed() {
					decreaseTermOrder();
				}
				@Override
				public void plusButtonPressed() {
					increaseTermOrder();
				}
			};
			JPanel buttonsPanel = new JPanel(new FlowLayout());
			buttonsPanel.add(newTermOkButton);
			buttonsPanel.add(newTermCancelButton);
			JPanel content = FormsUtils.build(
					"~ f:p:g ~ p ~ f:p:g ~", 
					"01_ p||" +
					"0_ f:p:g||" +
					"222 p||" +
					"333 p", 
					termElements, termOrderButtons.getPanel(),
					termDisplay,
					buttonsPanel
					).getPanel();
			final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			sp.setBorder(null);
			newTermDialog.setContentPane(sp);
			newTermDialog.pack();
			Dimension oldD = newTermDialog.getPreferredSize();
			newTermDialog.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
											     		 oldD.height + sp.getHorizontalScrollBar().getHeight()));
			sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			oldD = newTermDialog.getPreferredSize();
			final Dimension newD = GUIUtils.getPreferredSize(newTermDialog);
			if (!oldD.equals(newD)) 
				newTermDialog.setPreferredSize(newD);
			newTermDialog.pack();
		}
		termElements.clear();
		for (int i = 0; i < inputs.getList().size(); i++) {
			termElements.add(new InputInfo(inputs.get(i).getName(), 1));
		}
		newTermDialog.setVisible(true);
	}

	protected void increaseTermOrder() {
		modifySelectedOrders(termElements, 1, 0);
	}


	protected void decreaseTermOrder() {
		modifySelectedOrders(termElements, -1, 0);
	}
	
	private static void modifySelectedOrders(GenericListGui<InputInfo> list, int difference, int min) {
		List<InputInfo> selected = list.getSelectedValues();
		for (InputInfo info : selected) {
			int order = info.getOrder();
			order += difference;
			if(order < min) order = min;
			info.setOrder(order);
		}
		list.refreshDisplayKeepSelection();
	}

	@Override
	public String getDetailsAsString() {
		return name + " (Polynomial) " + super.getDetailsAsString();
	}

	public void writeRegressorToDb(IResultsDbMinimal db, String modelName, String versionName) throws Exception {
//		int actualChartId = 0;
//		Vector<ResultInMem> calculatedResults = new Vector<ResultInMem>();
		boolean exception = false;
		String versionPoly = versionName + "_Polynomial_" + Util.getTimeStamp();
		Model model = db.findModel(modelName, versionPoly);
		if (model == null)
			model = new Model(Model.NONEXISTENT_MODELID, modelName, versionPoly);
		//generate batch number
		int batch = db.getNewBatch(model);
		//putting the polynomials into the table
		Columns colsIn = new Columns();
		Columns colsOut = new Columns();
		colsOut.append("Polynomial name", ColumnType.STRING);
		colsOut.append("Response name", ColumnType.STRING);
		colsOut.append("Polynomials", ColumnType.STRING);
		List<PolynomialModel> polyInfos = polynomials.getList();
		for (int i = 0; i < polyInfos.size(); i++) {
			ResultInMem toAdd = new ResultInMem();
			Result.Row row = new Result.Row(colsOut, 0); //0 is the tick#
			ParameterComb pc = new ParameterComb(new GeneralRow(colsIn));
			toAdd.setModel(model);
			toAdd.setBatch(batch);
			toAdd.setStartTime(0L);
			toAdd.setEndTime(0L);
			toAdd.setRun(i);//the run#
        	row.set(0, name);
           	row.set(1, polyInfos.get(i).getOutput());
           	row.set(2, polyInfos.get(i).getRegressor().getRegressor().toString());
	        toAdd.setParameterComb(pc);
	        toAdd.add(row);
	        try {
	        	db.addResult(toAdd);
			} catch (final ValueNotSupportedException e) {
				exception = true;
			}
			//create chart data:
	        //chart if selected:
	        if (polyInfos.get(i).isCreateCharts()){
	        	if (polyInfos.get(i).getInputs().size() == 2){
//	        		if (polyInfos.get(i).chartId == -1) {
//	        			polyInfos.get(i).chartId = actualChartId;
//	        			actualChartId++;
//	        		}
//	        		String versionChart = "_ChartDataXYZ_Polynomials";
	        		String var1 = polyInfos.get(i).getInputs().get(0);
	        		String var2 = polyInfos.get(i).getInputs().get(1);
	        		double inp1min = sampleModel.getSampleMin(var1);
	        		double inp1max = sampleModel.getSampleMax(var1);
	        		double inp2min = sampleModel.getSampleMin(var2);
	        		double inp2max = sampleModel.getSampleMax(var2);
	        		Vector<Double> input1Vals = new Vector<Double>(INTERVALS);
	        		Vector<Double> input2Vals = new Vector<Double>(INTERVALS);
	        		double inp1div = (inp1max - inp1min) / (INTERVALS - 1.0);
	        		double inp2div = (inp2max - inp2min) / (INTERVALS - 1.0);
	        		for (int j = 0; j < INTERVALS - 1; j++) {
	        			input1Vals.add(inp1min + inp1div * j);
	        			input2Vals.add(inp2min + inp2div * j);
					}
        			input1Vals.add(inp1max);
        			input2Vals.add(inp2max);
        			Variable<Float64> input1Var = null;
        			Variable<Float64> input2Var = null;
        			Polynomial<Float64> poly = polyInfos.get(i).getRegressor().getRegressor();
        			for (int j = 0; j < poly.getVariables().size(); j++) {
        				if(poly.getVariables().get(j).getSymbol().equals(var1)){
        					input1Var = poly.getVariables().get(j);
        				} else if(poly.getVariables().get(j).getSymbol().equals(var2)){
        					input2Var = poly.getVariables().get(j);
        				}
        			}
        			Vector<Double> chartColumn = new Vector<Double>();
        			for (int j = 0; j < input1Vals.size(); j++) {
						for (int k = 0; k < input2Vals.size(); k++) {
							input1Var.set(Float64.valueOf(input1Vals.get(j)));
							input2Var.set(Float64.valueOf(input2Vals.get(k)));
							double response = poly.evaluate().doubleValue();
//							if(response > polyInfos.get(i).polyResponseMax) polyInfos.get(i).polyResponseMax = response;
//							if(response < polyInfos.get(i).polyResponseMin) polyInfos.get(i).polyResponseMin = response;
							chartColumn.add(response);
						}
					}
        			genericModelSelector.addChartColumn(var1+"-"+var2, chartColumn, var1, var2, inp1min, inp1max, inp2min, inp2max);
	        	}
	        }
		}
		if (exception)
			throw new ValueNotSupportedException();
	}

	public String getName() {
		return name;
	}
	

}
