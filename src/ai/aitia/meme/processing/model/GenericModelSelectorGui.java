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

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ai.aitia.meme.MEMEApp;
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
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.processing.IIntelliGenericProcesser;
import ai.aitia.meme.processing.ResultProcessingFrame;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.GenericListGui;

public class GenericModelSelectorGui implements ActionListener, IIntelliGenericProcesser{
	private static final String REMOVE_MODEL = "remove model";
	private static final String CREATE_MODEL = "create model";
	protected GenericListGui<String> inputs = null;
	protected GenericListGui<String> outputs = null;
	protected JButton createButton = null;
	protected JButton removeButton = null;
	protected GenericListGui<IRegressorModelDetails> modelList = null;
	protected JPanel guiPanel = null;
	protected JPanel modelPanel = null;
	protected SampleQueryModel sampleModel = null;
	protected Vector<String> chartHeader = null;
	protected Vector<Vector<Double>> chartData = null;
	protected Vector<String> inp1Names = null; 
	protected Vector<String> inp2Names = null; 
	protected Vector<Double> inp1Starts = null; 
	protected Vector<Double> inp1Ends = null; 
	protected Vector<Double> inp2Starts = null; 
	protected Vector<Double> inp2Ends = null;
	protected Vector<Double> outMins = null; 
	protected Vector<Double> outMaxs = null;
	protected IResultsDbMinimal db = null;
	protected Model resultModel = null;
	protected ResultProcessingFrame resultProcessingFrame = null;
	
	
	public GenericModelSelectorGui(SampleQueryModel sampleModel, IResultsDbMinimal db0, Model resultModel0, ResultProcessingFrame resultProcessingFrame) {
		this.sampleModel = sampleModel;
		this.db = db0;
		this.resultModel = resultModel0;
		this.resultProcessingFrame = resultProcessingFrame;
		inputs = new GenericListGui<String>();
		outputs = new GenericListGui<String>();
		inputs.addAll(sampleModel.getSampleNames());
		outputs.addAll(sampleModel.getResponseNames());
		inputs.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		outputs.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		inputs.getPanel().setBorder(BorderFactory.createTitledBorder("Inputs"));
		outputs.getPanel().setBorder(BorderFactory.createTitledBorder("Outputs"));
		inputs.setAllNoneButtonsVisible(true);
		outputs.setAllNoneButtonsVisible(true);
		createButton = new JButton("Create");
		removeButton = new JButton("Remove");
		createButton.setActionCommand(CREATE_MODEL);
		removeButton.setActionCommand(REMOVE_MODEL);
		JPanel buttonsPanel = new JPanel(new FlowLayout());
		buttonsPanel.add(createButton);
		buttonsPanel.add(removeButton);
		GUIUtils.addActionListener(this, createButton, removeButton);
		modelList = new GenericListGui<IRegressorModelDetails>() {
			@Override
			public void listSelectionChanged(ListSelectionEvent e) {
				if(modelPanel.getComponentCount() > 0){
					if(!modelPanel.getComponent(0).equals(modelList.getSelectedValue().getPanel())) {
						modelPanel.removeAll();
						modelPanel.add(modelList.getSelectedValue().getPanel());
						modelPanel.setVisible(false);
						modelPanel.setVisible(true);
					}
				} else {
					modelPanel.add(modelList.getSelectedValue().getPanel());
				}
			}
		};
		modelPanel = new JPanel();
		PolynomialRegressorModel nullModel = new PolynomialRegressorModel(sampleModel, new Vector<String>(), new Vector<String>(), this);
		nullModel.getPanel().setEnabled(false);
		modelPanel.add(nullModel.getPanel());
		modelPanel.setBorder(BorderFactory.createTitledBorder("Model options"));
		modelList.getPanel().setBorder(BorderFactory.createTitledBorder("List of models"));
		guiPanel = FormsUtils.build(
				"~ p ~ p ~ f:p:g ~", 
				"00_ p||" +
				"123 f:p:g||" +
				"443 p||" +
				"553 f:p:g||",
				"Select model inputs and outputs!",
				inputs.getPanel(), outputs.getPanel(), modelPanel,
				buttonsPanel,
				modelList.getPanel()
				).getPanel();
		chartHeader = new Vector<String>();
		chartData = new Vector<Vector<Double>>();
		inp1Names = new Vector<String>(); 
		inp2Names = new Vector<String>(); 
		inp1Starts = new Vector<Double>(); 
		inp1Ends = new Vector<Double>(); 
		inp2Starts = new Vector<Double>(); 
		inp2Ends = new Vector<Double>();
		outMins = new Vector<Double>(); 
		outMaxs = new Vector<Double>();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(CREATE_MODEL)) {
			List<String> selectedInputs = inputs.getSelectedValues();
			List<String> selectedOutputs = outputs.getSelectedValues();
			if (selectedInputs.size() == 0) {
				Utilities.userAlert(MEMEApp.getAppWnd(), "You have to select at least one input.");
				return;
			}
			if (selectedOutputs.size() == 0) {
				Utilities.userAlert(MEMEApp.getAppWnd(), "You have to select at least one output.");
				return;
			}
			IRegressorModelDetails model = new PolynomialRegressorModel(sampleModel, selectedInputs, selectedOutputs, this);
			modelList.add(model);
			modelList.setSelectedIndex(modelList.size() - 1);
		} else if (e.getActionCommand().equals(REMOVE_MODEL)) {
			modelList.removeIndices(modelList.getSelectedIndices());
		}
	}

	public void doGenericProcessing() throws ValueNotSupportedException {
		boolean exception = false;
		Vector<String> errors = new Vector<String>();
		for (int i = 0; i < modelList.size(); i++) {
			IRegressorModelDetails actualModel = modelList.get(i);
			if (actualModel instanceof PolynomialRegressorModel) {
				PolynomialRegressorModel polyModel = (PolynomialRegressorModel) actualModel;
				try {
					polyModel.writeRegressorToDb(db, resultModel.getName(), resultModel.getVersion());
				} catch (final ValueNotSupportedException e) {
					exception = true;
				} catch (Exception e) {
					errors.add(polyModel.getName());
				}
			}
		}
		if(chartData.size() > 0){
			try {
				List<ResultInMem> generatedResults = putChartToDatabase(resultModel.getName(), resultModel.getVersion() + "_2D_charts");
				String viewName = resultModel.getName()+"_"+resultModel.getVersion()+"_2D_ChartsView"+Util.getTimeStamp();
				ResultProcessingFrame.viewCreation(generatedResults, generatedResults.get(0).getModel(), viewName);
				Document doc = resultProcessingFrame.getDocumentForViewName(viewName);
				Element chartElem = create2dChartElement(doc, viewName);
				resultProcessingFrame.displayChartConfig(chartElem, viewName);
				//display errors
				if (errors.size() > 0)
					Utilities.userAlert(MEMEApp.getAppWnd(), "There were problems with these polynomials:", errors.toArray());
			} catch (Exception e) {
				//display error
				e.printStackTrace();
				Utilities.userAlert(MEMEApp.getAppWnd(), "There was an error during creating charts");
			}
		}
		if (exception)
			throw new ValueNotSupportedException();
	}

	private Element create2dChartElement(Document doc, String viewName) {
		Element chartRoot = doc.createElement("chart");
		chartRoot.setAttribute("singleFlag", "false");
		doc.appendChild(chartRoot);
		Element datasourcesElem = doc.createElement("datasources");
		chartRoot.appendChild(datasourcesElem);

		for (int i = 0; i < chartHeader.size(); i++) {
			//Setting up a double datasource for the chart:
			Element datasourceElem = doc.createElement("datasource");
			datasourceElem.setAttribute("id", "" + (1 + i));
			datasourceElem.setAttribute("type", "ai.aitia.visu.ds.ISeriesProducer");
			datasourcesElem.appendChild(datasourceElem);
			Element versionElem1 = doc.createElement("property");
			versionElem1.setAttribute("key", "version");
			versionElem1.setTextContent("1.0.80901");
			datasourceElem.appendChild(versionElem1);
			Element localeElem1 = doc.createElement("property");
			localeElem1.setAttribute("key", "locale_specific");
			localeElem1.setTextContent("false");
			datasourceElem.appendChild(localeElem1);
			Element ascendingElem1 = doc.createElement("property");
			ascendingElem1.setAttribute("key", "ascending");
			ascendingElem1.setTextContent("true");
			datasourceElem.appendChild(ascendingElem1);
			Element caseElem1 = doc.createElement("property");
			caseElem1.setAttribute("key", "case_sensitive");
			caseElem1.setTextContent("true");
			datasourceElem.appendChild(caseElem1);
			Element idElem1 = doc.createElement("property");
			idElem1.setAttribute("key", "id");
			//this is the key: "-1." means MEME will look for the view by its name
			//then comes the viewName
			//and finally the column name after a ";"
			idElem1.setTextContent("-1."+viewName+";"+chartHeader.get(i));
			datasourceElem.appendChild(idElem1);

			//chartconfig of an 2DGridChart
			Element chartconfigElem = doc.createElement("chartconfig");
			chartconfigElem.setAttribute("fireInitialEvent", "true");
			//you define the type of the chart here:
			chartconfigElem.setAttribute("type", "Grid2D");
			chartRoot.appendChild(chartconfigElem);

			Element dsElem = doc.createElement("property");
			dsElem.setAttribute("key", "datasource");
			//define the datasources used for this chart in a proper order
			dsElem.setTextContent("" + (1 + i));
			chartconfigElem.appendChild(dsElem);

			Element colormapElem = doc.createElement("property");
			colormapElem.setAttribute("key", "colormap");
			colormapElem.setTextContent("RAINBOW,"+outMins.get(i)+","+outMaxs.get(i));
			chartconfigElem.appendChild(colormapElem);

			Element envElem = doc.createElement("property");
			envElem.setAttribute("key", "environment appearance");
			envElem.setTextContent("normal");
			chartconfigElem.appendChild(envElem);

			Element columnLabelElem = doc.createElement("property");
			columnLabelElem.setAttribute("key", "column label");
			columnLabelElem.setTextContent(inp2Names.get(i)+" ["+inp2Starts.get(i)+".."+inp2Ends.get(i)+"]");
			chartconfigElem.appendChild(columnLabelElem);

			Element rowLabelElem = doc.createElement("property");
			rowLabelElem.setAttribute("key", "row label");
			rowLabelElem.setTextContent(inp1Names.get(i)+" ["+inp1Starts.get(i)+".."+inp1Ends.get(i)+"]");
			chartconfigElem.appendChild(rowLabelElem);

			Element userDefHeightElem = doc.createElement("property");
			userDefHeightElem.setAttribute("key", "user_defined height");
			userDefHeightElem.setTextContent("true");
			chartconfigElem.appendChild(userDefHeightElem);

			Element colorElem = doc.createElement("property");
			colorElem.setAttribute("key", "color appearance");
			colorElem.setTextContent("colored");
			chartconfigElem.appendChild(colorElem);

			Element customElem = doc.createElement("property");
			customElem.setAttribute("key", "custom appearance");
			chartconfigElem.appendChild(customElem);

			Element colorBarElem = doc.createElement("property");
			colorBarElem.setAttribute("key", "colorbar");
			colorBarElem.setTextContent("true");
			chartconfigElem.appendChild(colorBarElem);

			Element titleElem = doc.createElement("property");
			titleElem.setAttribute("key", "title");
			//this is the title of the chart
			titleElem.setTextContent(chartHeader.get(i));
			chartconfigElem.appendChild(titleElem);

			Element subElem = doc.createElement("property");
			subElem.setAttribute("key", "subtitle");
			//you can specify a subtitle as well:
			subElem.setTextContent("");
			chartconfigElem.appendChild(subElem);

			Element rowOrderElem = doc.createElement("property");
			rowOrderElem.setAttribute("key", "row order");
			rowOrderElem.setTextContent("true");
			chartconfigElem.appendChild(rowOrderElem);

			Element heightElem = doc.createElement("property");
			heightElem.setAttribute("key", "height");
			heightElem.setTextContent(""+PolynomialRegressorModel.INTERVALS);
			chartconfigElem.appendChild(heightElem);

			Element widthElem = doc.createElement("property");
			widthElem.setAttribute("key", "width");
			widthElem.setTextContent(""+PolynomialRegressorModel.INTERVALS);
			chartconfigElem.appendChild(widthElem);

			Element userDefWidthElem = doc.createElement("property");
			userDefWidthElem.setAttribute("key", "user defined width");
			userDefWidthElem.setTextContent("true");
			chartconfigElem.appendChild(userDefWidthElem);

			Element tooltipElem = doc.createElement("property");
			tooltipElem.setAttribute("key", "tooltip");
			tooltipElem.setTextContent("false");
			chartconfigElem.appendChild(tooltipElem);
		}
		
		return chartRoot;
	}

	public JPanel getGenericProcessPanel(Element pluginElement) {
		return guiPanel;
	}

	public boolean isGenericProcessingSupported() {
		return true;
	}
	
	public void addChartColumn(String name, Vector<Double> data, String inp1Name, String inp2Name, double inp1start, double inp1end, double inp2start, double inp2end) {
		String newName = name;
		int headerNamePlus = 2;
		while (chartHeader.contains(newName)) {
			newName = name + headerNamePlus;
			headerNamePlus++;
		}
		name = newName;
		chartHeader.add(name);
		chartData.add(data);
		inp1Names.add(inp1Name);
		inp2Names.add(inp2Name);
		inp1Starts.add(inp1start);
		inp1Ends.add(inp1end);
		inp2Starts.add(inp2start);
		inp2Ends.add(inp2end);
		//search the min and max in data:
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		for (Double d : data) {
			if (d < min) min = d;
			if (d > max) max = d;
		}
		outMaxs.add(max);
		outMins.add(min);
	}
	
	public List<ResultInMem> putChartToDatabase(String modelName, String versionName) throws Exception{
		Columns colsInChart = new Columns();
		Columns colsOutChart = new Columns();
		Model modelChart = db.findModel(modelName, versionName);
		if (modelChart == null) {
			modelChart = new Model(Model.NONEXISTENT_MODELID, modelName, versionName);
		}
		//generate batch number
		int batchChart = db.getNewBatch(modelChart);
		Vector<ResultInMem> ret = new Vector<ResultInMem>();
		for (int i = 0; i < chartHeader.size(); i++) {
			colsOutChart.append(chartHeader.get(i), ColumnType.DOUBLE);
		}
		for (int i = 0; i < chartData.get(0).size(); i++) {
			ResultInMem toAdd = new ResultInMem();
			Result.Row row = new Result.Row(colsOutChart, 0); //0 is the tick#
			ParameterComb pc = new ParameterComb(new GeneralRow(colsInChart));
			toAdd.setModel(modelChart);
			toAdd.setBatch(batchChart);
			toAdd.setStartTime(0L);
			toAdd.setEndTime(0L);
			toAdd.setRun(i);//the run#
			for (int j = 0; j < chartData.size(); j++) {
				row.set(j, chartData.get(j).get(i));
			}
			toAdd.setParameterComb(pc);
			toAdd.add(row);
			try {
				db.addResult(toAdd);
			} catch (final ValueNotSupportedException e) {}
			ret.add(toAdd);
		}
		return ret;
	}
}
