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
package ai.aitia.meme.processing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.database.IResultsDbMinimal;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ParameterComb;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.ResultInMem;
import ai.aitia.meme.database.Run;
import ai.aitia.meme.database.ColumnType.ValueNotSupportedException;
import ai.aitia.meme.paramsweep.generator.WizardSettingsManager;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.pluginmanager.IIntelliResultProcesserPlugin;
import ai.aitia.meme.processing.blocking.BlockingProcesser;
import ai.aitia.meme.processing.model.GenericModelSelectorGui;
import ai.aitia.meme.processing.model.SampleQueryModel;
import ai.aitia.meme.processing.quadratic.QuadraticModelProcesser;
import ai.aitia.meme.processing.variation.NaturalVariation;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.XMLUtils;
import ai.aitia.meme.viewmanager.SrcType;
import ai.aitia.meme.viewmanager.ViewCreation;
import ai.aitia.meme.viewmanager.ViewCreationRule;

public class ResultProcessingFrame implements ActionListener {

	private static final String PROCESSING_CANCEL = "PROCESSING_CANCEL";
	private static final String PROCESSING_OK = "PROCESSING_OK";
	private IIntelliResultProcesserPlugin resultProcPlugin = null;
	private NaturalVariation naturalVariation = null;
	private JDialog processingDialog = null;
	private JButton processingOkButton = null;
	private JButton processingCancelButton = null;
	private int processingDialogResult = 0;
	@SuppressWarnings("unused") private GenericModelSelectorGui genericModelSelector = null;
	private Vector<Document> chartConfigXmls = null;
	private Vector<String> chartConfigViews = null;
	private int chartAdditiveId = 0;

	class NoSuchPluginException extends Exception {
		private static final long serialVersionUID = 0L;

		public NoSuchPluginException(String message) {
			super(message);// ???
		}
	}
	
	public ResultProcessingFrame(){
		chartConfigXmls = new Vector<Document>();
		chartConfigViews = new Vector<String>();
	}
	
	public void writeIntelliSweepResults(IResultsDbMinimal db, String isPluginXMLFileName, String modelName, String version, List<Run> runs,
										 GeneralRow fixedPar, Columns otherPar,	int inputIndex, long startTime, long endTime) throws Exception {
		writeIntelliSweepResults(db,isPluginXMLFileName,modelName,version,runs,fixedPar,otherPar,inputIndex,startTime,endTime,true);
	}
	
	public void writeIntelliSweepResults(IResultsDbMinimal db,
			String isPluginXMLFileName, String modelName, String version,
			List<Run> runs, GeneralRow fixedPar, Columns otherPar,
			int inputIndex, long startTime, long endTime, final boolean include0th) throws Exception {
		boolean exception = false;
		File xmlFile = new File(isPluginXMLFileName);
		if (!xmlFile.exists())
			throw new FileNotFoundException(
					"IntelliSweep plugin descriptor XML");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder xmlParser = factory.newDocumentBuilder();
		Document document = xmlParser.parse(xmlFile);
		Element root = document.getDocumentElement();
		NodeList nl = root
				.getElementsByTagName(WizardSettingsManager.INTELLIEXTENSION_PAGE);
		String pluginName = "notFound";
		Element pluginElement = null;
		if (nl != null && nl.getLength() > 0) {
			Element intelliElement = (Element) nl.item(0);
			nl = null;
			nl = intelliElement.getElementsByTagName("plugin");
			if (nl != null && nl.getLength() > 0) {
				pluginElement = (Element) nl.item(0);
				pluginName = pluginElement
						.getAttribute(WizardSettingsManager.NAME);
			}
		}
		if (pluginName.equals("notFound")) {
			throw new WizardLoadingException(false,
					"Missing intelliSweep tag in: " + isPluginXMLFileName);
		}
		// obtain the plugin:
		pluginName = pluginName + "_Processer";
		resultProcPlugin = MEMEApp.getPluginManager()
				.getIntelliResultProcesserPluginInfos().findByLocalizedName(
						pluginName);
		if (resultProcPlugin == null) {
			throw new NoSuchPluginException("No such plugin: " + pluginName);
		}
		// put the results in results first!
		ArrayList<ResultInMem> results = new ArrayList<ResultInMem>();
		// Ensure that the GUI thread is waiting and making no changes in the
		// Model
		MEMEApp.LONG_OPERATION.showProgressNow();
		MEMEApp.LONG_OPERATION.progress(0, runs.size());

		int k = fixedPar.size();
		GeneralRow inp = fixedPar;
		Columns out = otherPar;
		if (inputIndex >= 0) {
			Columns c = new Columns();
			c.append(fixedPar.getColumns(), 0, fixedPar.size());
			c.append(otherPar, 0, inputIndex + 1);
			inp = new GeneralRow(c);
			for (int i = 0; i < k; ++i)
				inp.set(i, fixedPar.get(i));
			out = new Columns();
			out.append(otherPar, inputIndex + 1, otherPar.size());
		}
		final int n = out.size();

		Model model = db.findModel(modelName, version);
		if (model == null)
			model = new Model(Model.NONEXISTENT_MODELID, modelName, version);

		// Generate batch number
		//
		int b = db.getNewBatch(model);

		for (int i = 0; i < runs.size(); ++i) {
			ResultInMem result = new ResultInMem();
			result.setModel(model);
			result.setBatch(b);

			// TODO: Using this.startTime and this.endTime here are improper:
			// these are the times of the whole batch, not the individual runs.
			// I use them because I cannot tell better.
			result.setStartTime(startTime);
			result.setEndTime(endTime);

			MEMEApp.LONG_OPERATION.progress(i); // display progress
			result.setRun(runs.get(i).run); // !!!!M�DOS�T�S!!!!
			ArrayList<Result.Row> rows = runs.get(i).rows;
			if (rows.isEmpty())
				continue;

			GeneralRow inpNewObj = new GeneralRow(inp.getColumns());
			for (int j = 0; j <= inputIndex; ++j)
				inpNewObj.set(k + j, rows.get(0).get(j));
			for (int j = 0; j < k; j++)
				inpNewObj.set(j, fixedPar.get(j));
			result.setParameterComb(new ParameterComb(inpNewObj));
			// result.setParameterComb(new ParameterComb(inp));
			// for (int j = 0; j <= inputIndex; ++j)
			// inp.set(k + j, rows.get(0).get(j));

			result.resetRows(out);
			for (int j = 0; j < rows.size(); ++j) {
				Result.Row row = rows.get(j);
				if (!include0th && row.getTick() == 0) continue;
				Result.Row newrow = new Result.Row(out, row.getTick());
				for (int l = 0; l < n; ++l)
					newrow.set(l, row.get(inputIndex + 1 + l));
				result.add(newrow);
			}
			results.add(result);
		}
		// results are ready, let's process them!
		naturalVariation = new NaturalVariation();
		processingDialog = new JDialog(MEMEApp.getAppWnd());
		processingDialog.setTitle("Intellisweep result processing options");
		JTabbedPane tabPane = new JTabbedPane();
		processingDialog.setName("dial_intelliprocess");
		tabPane.setName("pane_intelliprocess_tabpane");
		if (resultProcPlugin.isGenericProcessingSupported()) {
			tabPane.add(resultProcPlugin.getLocalizedName(), resultProcPlugin.getGenericProcessPanel(pluginElement));
		}
//		SampleQueryModel sampleModel = createSamplemodel(results, pluginElement);
//		genericModelSelector = new GenericModelSelectorGui(sampleModel, db, model, this);
//		tabPane.add("General processing", genericModelSelector.getGenericProcessPanel(pluginElement));
		JPanel natVarPanel = naturalVariation.getGenericProcessPanel(pluginElement);
		if (natVarPanel != null) {
			tabPane.add("Natural variation", natVarPanel);
		}
		QuadraticModelProcesser qmp = new QuadraticModelProcesser();
		JPanel quadPanel = qmp.getGenericProcessPanel(pluginElement);
		if (quadPanel != null) {
			tabPane.add("Quadratic modeler", quadPanel);
		}
		if (tabPane.getTabCount() > 0){
			JPanel dialogContent = new JPanel(new BorderLayout());
			dialogContent.add(tabPane, BorderLayout.CENTER);
			JPanel buttonsPanel = new JPanel(new FlowLayout());
			processingOkButton = new JButton("Process");
			processingOkButton.setActionCommand(PROCESSING_OK);
			buttonsPanel.add(processingOkButton);
			processingCancelButton = new JButton("Skip processing");
			processingCancelButton.setActionCommand(PROCESSING_CANCEL);
			buttonsPanel.add(processingCancelButton);
			
			processingOkButton.setName("btn_ok");
			processingCancelButton.setName("btn_cancel");
			
			GUIUtils.addActionListener(this, processingOkButton, processingCancelButton);
			dialogContent.add(buttonsPanel, BorderLayout.SOUTH);
			processingDialog.setModal(true);
			final JScrollPane sp = new JScrollPane(dialogContent,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			sp.setBorder(null);
			processingDialog.setContentPane(sp);
			processingDialog.pack();
			Dimension oldD = processingDialog.getPreferredSize();
			processingDialog.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
											     		    oldD.height + sp.getHorizontalScrollBar().getHeight()));
			sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			oldD = processingDialog.getPreferredSize();
			final Dimension newD = GUIUtils.getPreferredSize(processingDialog);
			if (!oldD.equals(newD)) 
				processingDialog.setPreferredSize(newD);
			processingDialog.pack();
			processingDialog.setVisible(true);
			//execution halts here, until the dialog is shown
		}
		if (processingDialogResult == 0) {
			if (natVarPanel != null) naturalVariation.doGenericProcessing();
			if (quadPanel != null) {
				qmp.doGenericProcessing();
				List<ResultInMem> qmpRes = qmp.processResultFiles(db, results, pluginElement);
				for (ResultInMem resultInMem : qmpRes) {
					db.addResult(resultInMem);
				}
			}

			if (resultProcPlugin.isGenericProcessingSupported()) {
				try {
					resultProcPlugin.doGenericProcessing();
				} catch (final ValueNotSupportedException e) {
					exception = true;
				}
			}
			List<ResultInMem> generatedResults = resultProcPlugin
			.processResultFiles(db, results, pluginElement);
			if(generatedResults.size() > 0){
				for (ResultInMem actResult : generatedResults) {
					try {
						db.addResult(actResult);
					} catch (final ValueNotSupportedException e) {
						exception = true;
					}
				}
				// automatic view creation (from every model-version pair created so
				// far):
				viewCreation(generatedResults);
			}
			// natural variation handling:
			try {
				if (natVarPanel != null) {
					naturalVariationHandling(db, results, pluginElement);
				}
			} catch (final ValueNotSupportedException e) {
				exception = true;
			}
			//blocking handling:
			try {
				blockingHandling(db, results, pluginElement);
			} catch (final ValueNotSupportedException e) {
				exception = true;
			}
//			try {
//				genericModelSelector.doGenericProcessing();
//			} catch (final ValueNotSupportedException e) {
//				exception = true;
//			}
			if (exception)
				throw new ValueNotSupportedException();
		}
		setNames();
	}

	protected void naturalVariationHandling(IResultsDbMinimal db,
			ArrayList<ResultInMem> results, Element pluginElement)
			throws Exception {
		boolean exception = false;
		if (naturalVariation.loadAndProcessData(results, pluginElement)) {
			List<ResultInMem> variationResults = naturalVariation.getVariationResults(db);
			for (int i = 0; i < variationResults.size(); i++) {
				try {
					db.addResult(variationResults.get(i));
				} catch (final ValueNotSupportedException e) {
					exception = true;
				}
			}
		}
		if (exception)
			throw new ValueNotSupportedException();
	}

	protected void blockingHandling(IResultsDbMinimal db,
			ArrayList<ResultInMem> results, Element pluginElement)
			throws Exception {
		boolean exception = false;
		BlockingProcesser blockingProc = new BlockingProcesser();
		List<ResultInMem> blockingResults = blockingProc.processBlockingData(results, db, pluginElement);
		for (int i = 0; i < blockingResults.size(); i++) {
			try {
				db.addResult(blockingResults.get(i));
			} catch (final ValueNotSupportedException e) {
				exception = true;
			}
		}
		if (exception)
			throw new ValueNotSupportedException();
	}

	protected void viewCreation(List<ResultInMem> generatedResults)
			throws ParserConfigurationException, TransformerException,
			IOException {
		Vector<Model> modelsDone = new Vector<Model>();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder viewBuilder = factory.newDocumentBuilder();
		for (ResultInMem actResult : generatedResults) {
			Model actModel = actResult.getModel();
			boolean newModel = true;
			for (int i = 0; i < modelsDone.size() && newModel; i++) {
				if (modelsDone.get(i).equals(actModel))
					newModel = false;
			}
			if (newModel) {
				Document viewXML = viewBuilder.newDocument();
				Element viewRoot = viewXML
						.createElement(ViewCreationRule.NODENAME);
				viewXML.appendChild(viewRoot);
				Element dataElem = viewXML
						.createElement(ViewCreationRule.INPUT_DATA);
				Element tableElem = viewXML
						.createElement(ViewCreationRule.INPUT_TABLE);
				tableElem.setAttribute(ViewCreationRule.INPUT_MODELNAME_ATTR,
						actModel.getName());
				tableElem.setAttribute(
						ViewCreationRule.INPUT_MODELVERSION_ATTR, actModel
								.getVersion());
				dataElem.appendChild(tableElem);
				viewRoot.appendChild(dataElem);
				Element viewNameElem = viewXML
						.createElement(ViewCreationRule.NAME);
				viewNameElem.setTextContent(actModel.getName() + "_"
						+ actModel.getVersion() + "_View");
				viewRoot.appendChild(viewNameElem);
				Element descElem = viewXML
						.createElement(ViewCreationRule.DESCRIPTION);
				viewRoot.appendChild(descElem);
				Element condElem = viewXML
						.createElement(ViewCreationRule.CONDITION);
				Element scalarScriptElem = viewXML
						.createElement(SrcType.SCALAR_SCRIPT.toString());
				condElem.appendChild(scalarScriptElem);
				viewRoot.appendChild(condElem);
				Element columnsElem = viewXML
						.createElement(ViewCreationRule.COLUMNS);
				columnsElem
						.setAttribute(ViewCreationRule.GROUPINGATTR, "false");
				// make column elements for all columns:
				Columns inputCols = actResult.getParameterComb().getNames();
				Columns outputCols = actResult.getOutputColumns();
				for (int i = 0; i < inputCols.size(); i++) {
					Element columnElem = viewXML
							.createElement(ViewCreationRule.COLUMN);
					columnElem.setAttribute(ViewCreationRule.COL_INITIAL_TYPE,
							inputCols.get(i).getDatatype().toString());
					columnElem.setAttribute(ViewCreationRule.COL_GRP_ATTR,
							"true");
					Element colNameElem = viewXML
							.createElement(ViewCreationRule.COL_NAME);
					colNameElem.setTextContent(inputCols.get(i).getName());
					Element projElem = viewXML
							.createElement(ViewCreationRule.COL_PROJECTION);
					projElem.setAttribute(ViewCreationRule.COL_PROJ_SRC_ATTR,
							ViewCreationRule.COL_PROJ_INPUT);
					projElem.setTextContent(inputCols.get(i).getName());
					columnElem.appendChild(colNameElem);
					columnElem.appendChild(projElem);
					columnsElem.appendChild(columnElem);
				}
				for (int i = 0; i < outputCols.size(); i++) {
					Element columnElem = viewXML
							.createElement(ViewCreationRule.COLUMN);
					columnElem.setAttribute(ViewCreationRule.COL_INITIAL_TYPE,
							outputCols.get(i).getDatatype().toString());
					columnElem.setAttribute(ViewCreationRule.COL_GRP_ATTR,
							"true");
					Element colNameElem = viewXML
							.createElement(ViewCreationRule.COL_NAME);
					colNameElem.setTextContent(outputCols.get(i).getName());
					Element projElem = viewXML
							.createElement(ViewCreationRule.COL_PROJECTION);
					projElem.setAttribute(ViewCreationRule.COL_PROJ_SRC_ATTR,
							ViewCreationRule.COL_PROJ_OUTPUT);
					projElem.setTextContent(outputCols.get(i).getName());
					columnElem.appendChild(colNameElem);
					columnElem.appendChild(projElem);
					columnsElem.appendChild(columnElem);
				}
				viewRoot.appendChild(columnsElem);

				// The view creation rule document is ready!
				saveViewRule(viewRoot);
				modelsDone.add(actModel);
				// automatic chart generation for the actual view:
				Document chartXML = resultProcPlugin.createCharts(viewNameElem
						.getTextContent(), actModel.getName(), actModel
						.getVersion());
				if (chartXML != null) {
					// write it to a temporary file:
					File tempXML = new File("temporaryChartConfig3431hdrhd5352"
							+ Util.getTimeStamp() + ".xml");
					XMLUtils.write(tempXML, chartXML);
					MEMEApp.getMainWindow().getChartsPanel().loadChartFromXMLAndDelete(
							tempXML);
				}
			}
		}
	}

	public static void viewCreation(List<ResultInMem> generatedResults, Model model, String viewName)
	throws ParserConfigurationException, TransformerException,
	IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder viewBuilder = factory.newDocumentBuilder();
		boolean modelDone = false;
		for (ResultInMem actResult : generatedResults) {
			if (!modelDone) {
				Model actModel = actResult.getModel();
				if (actModel.equals(model)) {
					Document viewXML = viewBuilder.newDocument();
					Element viewRoot = viewXML
					.createElement(ViewCreationRule.NODENAME);
					viewXML.appendChild(viewRoot);
					Element dataElem = viewXML
					.createElement(ViewCreationRule.INPUT_DATA);
					Element tableElem = viewXML
					.createElement(ViewCreationRule.INPUT_TABLE);
					tableElem.setAttribute(ViewCreationRule.INPUT_MODELNAME_ATTR,
							actModel.getName());
					tableElem.setAttribute(
							ViewCreationRule.INPUT_MODELVERSION_ATTR, actModel
							.getVersion());
					dataElem.appendChild(tableElem);
					viewRoot.appendChild(dataElem);
					Element viewNameElem = viewXML
					.createElement(ViewCreationRule.NAME);
					viewNameElem.setTextContent(viewName);
					viewRoot.appendChild(viewNameElem);
					Element descElem = viewXML
					.createElement(ViewCreationRule.DESCRIPTION);
					viewRoot.appendChild(descElem);
					Element condElem = viewXML
					.createElement(ViewCreationRule.CONDITION);
					Element scalarScriptElem = viewXML
					.createElement(SrcType.SCALAR_SCRIPT.toString());
					condElem.appendChild(scalarScriptElem);
					viewRoot.appendChild(condElem);
					Element columnsElem = viewXML
					.createElement(ViewCreationRule.COLUMNS);
					columnsElem
					.setAttribute(ViewCreationRule.GROUPINGATTR, "false");
					// make column elements for all columns:
					Columns inputCols = actResult.getParameterComb().getNames();
					Columns outputCols = actResult.getOutputColumns();
					for (int i = 0; i < inputCols.size(); i++) {
						Element columnElem = viewXML
						.createElement(ViewCreationRule.COLUMN);
						columnElem.setAttribute(ViewCreationRule.COL_INITIAL_TYPE,
								inputCols.get(i).getDatatype().toString());
						columnElem.setAttribute(ViewCreationRule.COL_GRP_ATTR,
						"true");
						Element colNameElem = viewXML
						.createElement(ViewCreationRule.COL_NAME);
						colNameElem.setTextContent(inputCols.get(i).getName());
						Element projElem = viewXML
						.createElement(ViewCreationRule.COL_PROJECTION);
						projElem.setAttribute(ViewCreationRule.COL_PROJ_SRC_ATTR,
								ViewCreationRule.COL_PROJ_INPUT);
						projElem.setTextContent(inputCols.get(i).getName());
						columnElem.appendChild(colNameElem);
						columnElem.appendChild(projElem);
						columnsElem.appendChild(columnElem);
					}
					for (int i = 0; i < outputCols.size(); i++) {
						Element columnElem = viewXML
						.createElement(ViewCreationRule.COLUMN);
						columnElem.setAttribute(ViewCreationRule.COL_INITIAL_TYPE,
								outputCols.get(i).getDatatype().toString());
						columnElem.setAttribute(ViewCreationRule.COL_GRP_ATTR,
						"true");
						Element colNameElem = viewXML
						.createElement(ViewCreationRule.COL_NAME);
						colNameElem.setTextContent(outputCols.get(i).getName());
						Element projElem = viewXML
						.createElement(ViewCreationRule.COL_PROJECTION);
						projElem.setAttribute(ViewCreationRule.COL_PROJ_SRC_ATTR,
								ViewCreationRule.COL_PROJ_OUTPUT);
						projElem.setTextContent(outputCols.get(i).getName());
						columnElem.appendChild(colNameElem);
						columnElem.appendChild(projElem);
						columnsElem.appendChild(columnElem);
					}
					viewRoot.appendChild(columnsElem);

					// The view creation rule document is ready!
					saveViewRule(viewRoot);
					modelDone = true;
				}
			}
		}
	}

	public static void saveViewRule(Element root) {
		List<Element> tables = XMLUtils.findAll(root,
				ViewCreationRule.INPUT_TABLE);
		for (Element table : tables) {
			String model_version = table
					.getAttribute(ViewCreationRule.INPUT_MODELVERSION_ATTR);
			if (model_version != null && model_version.length() != 0) { // reference
				// to
				// result
				// table
				String model_name = table
						.getAttribute(ViewCreationRule.INPUT_MODELNAME_ATTR);
				if (model_name == null || model_name.length() == 0) {
					Logger.logError("Missing model name attribute");
					return;
				}
				Model model = MEMEApp.getResultsDb().findModel(model_name,
						model_version);
				if (model == null) {
					Logger.logError("Invalid reference to model %s",
							model_name + "/" + model_version);
					return;
				}
				table.setAttribute(ViewCreationRule.INPUT_MODELID_ATTR, String
						.valueOf(model.getModel_id()));
				table.removeAttribute(ViewCreationRule.INPUT_MODELNAME_ATTR);
				table.removeAttribute(ViewCreationRule.INPUT_MODELVERSION_ATTR);
			} else { // reference to view table
				String viewName = table
						.getAttribute(ViewCreationRule.INPUT_VIEWNAME_ATTR);
				if (viewName != null && viewName.length() != 0) {
					Long view_id = MEMEApp.getViewsDb().findView(viewName);
					if (view_id == null) {
						return;
					} else {
						table.setAttribute(ViewCreationRule.INPUT_VIEWID_ATTR,
								view_id.toString());
					}
				}
			}
		}
		ViewCreationRule rule = new ViewCreationRule(root);
		ViewCreation vc = new ViewCreation(rule, null);
		try {
			System.gc();
			vc.trun();
		} catch (Exception e) {
			Logger.logException(
					"ResultsPanel.loadViews() - " + rule.getName(), e);
		}

	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(PROCESSING_OK)) {
			processingDialogResult = 0;
			processingDialog.setVisible(false);
		} else if (e.getActionCommand().equals(PROCESSING_CANCEL)) {
			processingDialogResult = -1;
			processingDialog.setVisible(false);
		} 
	}
	
	protected SampleQueryModel createSamplemodel(List<ResultInMem> runs, Element pluginElement){
		SampleQueryModel sampleModel = null;
		Vector<MemberInfo> inputs = new Vector<MemberInfo>();
		ResultInMem first = runs.get(0);
		Vector<String> inputStrings = new Vector<String>();
		Vector<Integer> inputIndices = new Vector<Integer>();
		ParameterComb pc = first.getParameterComb();
		for (int i = 0; i < pc.getNames().size(); i++) {
			if(		!pc.getNames().get(i).getDatatype().getJavaClass().equals(Boolean.class) &&
					!pc.getNames().get(i).getDatatype().getJavaClass().equals(String.class)) {
				MemberInfo info = new MemberInfo(pc.getNames().get(i).getName(),"Double", null);
				inputs.add(info);
				inputStrings.add(info.getName());
				inputIndices.add(i);
			}
		}
		Columns resultColumns = first.getOutputColumns();
		Vector<String> resultStrings = new Vector<String>();
		Vector<Integer> resultIndices = new Vector<Integer>();
		for (int i = 0; i < resultColumns.size(); i++) {
			if(Number.class.isAssignableFrom(resultColumns.get(i).getDatatype().getJavaClass())){
				resultStrings.add(resultColumns.get(i).getName());
				resultIndices.add(i);
			}
		}
		sampleModel = new SampleQueryModel(inputStrings, resultStrings);
		for (int i = 0; i < runs.size(); i++) {
			//the result has the output columns, let's get them:
			resultColumns = runs.get(i).getOutputColumns();
			Vector<Double> resultDoubles = new Vector<Double>();
			Vector<Double> inputDoubles = null;
			for (int j = 0; j < resultColumns.size(); j++) {
				double response = (getNumberValue(runs.get(i).getAllRows().iterator().next().get(j).toString())).doubleValue();
				if(resultIndices.contains(new Integer(j))){
					resultDoubles.add(response);
				}
				//all results have parameter combinations (input parameters):
				ParameterComb paramComb = runs.get(i).getParameterComb();
				Vector<Double> inputDoublesTmp = null;
				if(inputDoubles == null){
					inputDoublesTmp = new Vector<Double>();
				}
				for (int k = 0; k < inputs.size(); k++) {
					String name = inputs.get(k).getName();
					int l = 0;
					while(paramComb.getNames().get(l).getName().compareTo(name) != 0 && l < paramComb.getNames().size()) l++;
					if(l < paramComb.getNames().size()){
						Number input = new Integer(-1);
						Number inputCandidate = getNumberValue(paramComb.getValues().get(l).toString());
						if(inputCandidate != null) input = inputCandidate; 
						if(inputDoubles == null && inputIndices.contains(new Integer(k))){
							inputDoublesTmp.add(input.doubleValue());
						}
					}
				}
				if(inputDoubles == null){
					inputDoubles = inputDoublesTmp;
				}
            }
			//adding the sample to the SampleQueryModel:
			sampleModel.addSample(inputDoubles, resultDoubles);
        }
		return sampleModel.getSampleQueryModelWithoutConstants(16);
	}

	public static Number getNumberValue(String value) {
		Number ret = null;
		try {
			ret = new Double(value);
		} catch (NumberFormatException e) {
			ret = null;
		}
		if (ret == null) {
			if (value.equalsIgnoreCase("true")) return new Double(1.0);
			else return new Double(0.0);
		} else {
			return ret;
		}
	}
	
	public Document getDocumentForViewName(String viewName) throws ParserConfigurationException {
		int index = chartConfigViews.indexOf(viewName);
		boolean present = index > -1;
		Document chartXml = null;
		if (!present) {
			index = chartConfigViews.size();
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder chartBuilder = factory.newDocumentBuilder();
			chartXml = chartBuilder.newDocument();
			chartConfigXmls.add(chartXml);
			chartConfigViews.add(viewName);
		} else {
			chartXml = chartConfigXmls.get(index);
		}
		return chartXml;
	}
	
	public void displayChartConfig(Element chartElement, String viewName) throws ParserConfigurationException, TransformerException, IOException {
		int index = chartConfigViews.indexOf(viewName);
		if (index == -1) {
			Document newDoc = getDocumentForViewName(viewName);
			index = chartConfigXmls.size();
			chartConfigViews.add(viewName);
			chartConfigXmls.add(newDoc);
		}
		Document doc = chartConfigXmls.get(index);
		// write it to a temporary file:
		File tempXML = new File("temporaryChartConfig3431hdrhd5352" + chartAdditiveId
				+ Util.getTimeStamp() + ".xml");
		chartAdditiveId++;
		XMLUtils.write(tempXML, doc);
		MEMEApp.getMainWindow().getChartsPanel().loadChartFromXMLAndDelete(
				tempXML);
	}
	public void setNames()
	{
		
	}

}
