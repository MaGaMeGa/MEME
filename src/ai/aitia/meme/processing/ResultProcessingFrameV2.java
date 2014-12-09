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
import ai.aitia.meme.database.AbstractResultsDb;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.database.IResultsDbMinimal;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.ColumnType.ValueNotSupportedException;
import ai.aitia.meme.paramsweep.generator.WizardSettingsManager;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.pluginmanager.IIntelliResultProcesserPluginV2;
import ai.aitia.meme.processing.blocking.BlockingProcesserV2;
import ai.aitia.meme.processing.model.GenericModelSelectorGui;
import ai.aitia.meme.processing.quadratic.QuadraticModelProcesser;
import ai.aitia.meme.processing.variation.NaturalVariationV2;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.XMLUtils;
import ai.aitia.meme.viewmanager.SrcType;
import ai.aitia.meme.viewmanager.ViewCreation;
import ai.aitia.meme.viewmanager.ViewCreationRule;

public class ResultProcessingFrameV2 implements ActionListener {

	//====================================================================================================
	// members
	
	private static final String PROCESSING_CANCEL = "PROCESSING_CANCEL";
	private static final String PROCESSING_OK = "PROCESSING_OK";
	
	private IIntelliResultProcesserPluginV2 resultProcPlugin = null;
	private NaturalVariationV2 naturalVariation = null;
	
	private JDialog processingDialog = null;
	private JButton processingOkButton = null;
	private JButton processingCancelButton = null;
	private int processingDialogResult = 0;
	@SuppressWarnings("unused") private GenericModelSelectorGui genericModelSelector = null;
	private Vector<Document> chartConfigXmls = null;
	private Vector<String> chartConfigViews = null;
	private int chartAdditiveId = 0;

	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	public static class NoSuchPluginException extends Exception {
		private static final long serialVersionUID = 0L;

		public NoSuchPluginException(String message) {
			super(message);// ???
		}
	}
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public ResultProcessingFrameV2(){
		chartConfigXmls = new Vector<Document>();
		chartConfigViews = new Vector<String>();
	}
	
	//----------------------------------------------------------------------------------------------------
	public void writeIntelliSweepResults(final AbstractResultsDb db, final String isPluginXMLFileName, final String modelName, final String version,
										 final int batchNumber, final GeneralRow fixedPar, final Columns otherPar,	final int inputIndex,
										 final long startTime, final long endTime) throws Exception {
		writeIntelliSweepResults(db,isPluginXMLFileName,modelName,version,batchNumber,fixedPar,otherPar,inputIndex,startTime,endTime,true);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void writeIntelliSweepResults(final AbstractResultsDb db, final String isPluginXMLFileName, final String modelName, final String version,
										 final int batchNumber, final GeneralRow fixedPar, final Columns otherPar, final int inputIndex,
										 final long startTime, final long endTime, final boolean include0th) throws Exception {
		boolean exception = false;
		final File xmlFile = new File(isPluginXMLFileName);
		if (!xmlFile.exists())
			throw new FileNotFoundException("IntelliSweep plugin descriptor XML");
		
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder xmlParser = factory.newDocumentBuilder();
		final Document document = xmlParser.parse(xmlFile);
		final Element root = document.getDocumentElement();
		NodeList nl = root.getElementsByTagName(WizardSettingsManager.INTELLIEXTENSION_PAGE);
		String pluginName = "notFound";
		Element pluginElement = null;
		if (nl != null && nl.getLength() > 0) {
			Element intelliElement = (Element) nl.item(0);
			nl = null;
			nl = intelliElement.getElementsByTagName("plugin");
			if (nl != null && nl.getLength() > 0) {
				pluginElement = (Element) nl.item(0);
				pluginName = pluginElement.getAttribute(WizardSettingsManager.NAME);
			}
		}
		if (pluginName.equals("notFound")) 
			throw new WizardLoadingException(false,"Missing intelliSweep tag in: " + isPluginXMLFileName);

		// obtain the plugin:
		pluginName = pluginName + "_Processer";
		resultProcPlugin = MEMEApp.getPluginManager().getIntelliResultProcesserPluginV2Infos().findByLocalizedName(pluginName);
		if (resultProcPlugin == null) 
			throw new NoSuchPluginException("No such plugin: " + pluginName);
		
		
		Model model = db.findModel(modelName,version);
		if (model == null) 
			throw new IllegalStateException("Result not found: " + modelName + "/" + version);
		@SuppressWarnings("cast") final List<Result> results = db.getResults(model.getModel_id(),new Long[] { (long) batchNumber });
		// results are ready, let's process them!
		
		naturalVariation = new NaturalVariationV2();
		processingDialog = new JDialog(MEMEApp.getAppWnd());
		processingDialog.setTitle("Intellisweep result processing options");
		JTabbedPane tabPane = new JTabbedPane();
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
			/*processingOkButton.setName("btn_ok");
			processingCancelButton.setName("btn_cancel");*/
			processingOkButton = new JButton("Process");
			processingOkButton.setActionCommand(PROCESSING_OK);
			buttonsPanel.add(processingOkButton);
			processingCancelButton = new JButton("Skip processing");
			processingCancelButton.setActionCommand(PROCESSING_CANCEL);
			buttonsPanel.add(processingCancelButton);
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
				qmp.processResultFiles(db, results, pluginElement, "XXXdon't careYYY");
			}

			if (resultProcPlugin.isGenericProcessingSupported()) {
				try {
					resultProcPlugin.doGenericProcessing();
				} catch (final ValueNotSupportedException e) {
					exception = true;
				}
			}
			final List<String> processedVersionNames = resultProcPlugin.processResultFiles(db,results,pluginElement,isPluginXMLFileName);
			if (processedVersionNames != null && processedVersionNames.size() > 0) {
				// automatic view creation (from every model-version pair created so
				// far):
				viewCreation(db,modelName,processedVersionNames);
			}
			
			// natural variation handling:
			try {
				if (natVarPanel != null) {
					naturalVariationHandling(db,results,pluginElement,isPluginXMLFileName);
				}
			} catch (final ValueNotSupportedException e) {
				exception = true;
			}
			
			// blocking handling:
			try {
				blockingHandling(db,results,pluginElement,isPluginXMLFileName);
			} catch (final ValueNotSupportedException e) {
				exception = true;
			}

			if (exception)
				throw new ValueNotSupportedException();
		}
	}

	//----------------------------------------------------------------------------------------------------
	protected void naturalVariationHandling(final IResultsDbMinimal db, final List<Result> results, final Element pluginElement, final String isPluginXMLFileName) throws Exception {
		if (naturalVariation.loadAndProcessData(results,pluginElement)) 
			naturalVariation.writeVariationResults(db,isPluginXMLFileName);
	}

	//----------------------------------------------------------------------------------------------------
	protected void blockingHandling(final IResultsDbMinimal db, final List<Result> results, final Element pluginElement,
									final String isPluginXMLFileName) throws Exception {
		final BlockingProcesserV2 blockingProc = new BlockingProcesserV2();
		blockingProc.processBlockingData(results,db,pluginElement,isPluginXMLFileName);
	}

	//----------------------------------------------------------------------------------------------------
	protected void viewCreation(final IResultsDbMinimal db, final String modelName, final List<String> versions) throws ParserConfigurationException,
																														TransformerException,
																														IOException {
		final Vector<Model> modelsDone = new Vector<Model>();
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder viewBuilder = factory.newDocumentBuilder();
		for (final String v : versions) {
			Model actModel = db.findModel(modelName,v);
			if (actModel == null)
				throw new IllegalStateException("Result not found: " + modelName + "/" + v);
				
			boolean newModel = true;
			for (int i = 0;newModel && i < modelsDone.size();i++) {
				if (modelsDone.get(i).equals(actModel))
					newModel = false;
			}
			
			if (newModel) {
				final Document viewXML = viewBuilder.newDocument();
				final Element viewRoot = viewXML.createElement(ViewCreationRule.NODENAME);
				viewXML.appendChild(viewRoot);
				
				final Element dataElem = viewXML.createElement(ViewCreationRule.INPUT_DATA);
				final Element tableElem = viewXML.createElement(ViewCreationRule.INPUT_TABLE);
				tableElem.setAttribute(ViewCreationRule.INPUT_MODELNAME_ATTR,actModel.getName());
				tableElem.setAttribute(ViewCreationRule.INPUT_MODELVERSION_ATTR,actModel.getVersion());
				dataElem.appendChild(tableElem);
				viewRoot.appendChild(dataElem);
				
				final Element viewNameElem = viewXML.createElement(ViewCreationRule.NAME);
				viewNameElem.setTextContent(actModel.getName() + "_" + actModel.getVersion() + "_View");
				viewRoot.appendChild(viewNameElem);
				
				final Element descElem = viewXML.createElement(ViewCreationRule.DESCRIPTION);
				viewRoot.appendChild(descElem);
				
				final Element condElem = viewXML.createElement(ViewCreationRule.CONDITION);
				final Element scalarScriptElem = viewXML.createElement(SrcType.SCALAR_SCRIPT.toString());
				condElem.appendChild(scalarScriptElem);
				viewRoot.appendChild(condElem);
				
				final Element columnsElem = viewXML.createElement(ViewCreationRule.COLUMNS);
				columnsElem.setAttribute(ViewCreationRule.GROUPINGATTR,"false");
				
				// make column elements for all columns:
				final Columns[] columns = db.getModelColumns(actModel.getModel_id());
				final Columns inputCols = columns[0];
				final Columns outputCols = columns[1];
				
				for (int i = 0;i < inputCols.size();i++) {
					final Element columnElem = viewXML.createElement(ViewCreationRule.COLUMN);
					columnElem.setAttribute(ViewCreationRule.COL_INITIAL_TYPE,inputCols.get(i).getDatatype().toString());
					columnElem.setAttribute(ViewCreationRule.COL_GRP_ATTR,"true");
					
					final Element colNameElem = viewXML.createElement(ViewCreationRule.COL_NAME);
					colNameElem.setTextContent(inputCols.get(i).getName());
					
					final Element projElem = viewXML.createElement(ViewCreationRule.COL_PROJECTION);
					projElem.setAttribute(ViewCreationRule.COL_PROJ_SRC_ATTR,ViewCreationRule.COL_PROJ_INPUT);
					projElem.setTextContent(inputCols.get(i).getName());
					columnElem.appendChild(colNameElem);
					columnElem.appendChild(projElem);
					columnsElem.appendChild(columnElem);
				}
				
				for (int i = 0;i < outputCols.size();i++) {
					final Element columnElem = viewXML.createElement(ViewCreationRule.COLUMN);
					columnElem.setAttribute(ViewCreationRule.COL_INITIAL_TYPE,outputCols.get(i).getDatatype().toString());
					columnElem.setAttribute(ViewCreationRule.COL_GRP_ATTR,"true");
					
					final Element colNameElem = viewXML.createElement(ViewCreationRule.COL_NAME);
					colNameElem.setTextContent(outputCols.get(i).getName());
					
					final Element projElem = viewXML.createElement(ViewCreationRule.COL_PROJECTION);
					projElem.setAttribute(ViewCreationRule.COL_PROJ_SRC_ATTR,ViewCreationRule.COL_PROJ_OUTPUT);
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
				final Document chartXML = resultProcPlugin.createCharts(viewNameElem.getTextContent(),actModel.getName(),actModel.getVersion());
				if (chartXML != null) {
					// write it to a temporary file:
					File tempXML = new File("temporaryChartConfig3431hdrhd5352"	+ Util.getTimeStamp() + ".xml");
					XMLUtils.write(tempXML,chartXML);
					MEMEApp.getMainWindow().getChartsPanel().loadChartFromXMLAndDelete(tempXML);
				}
			}
		}
	}

	//----------------------------------------------------------------------------------------------------
	public static void viewCreation(final String modelName, final List<String> versions, final Model model,	final String viewName)
																												throws ParserConfigurationException,
																													   TransformerException,
																													   IOException {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder viewBuilder = factory.newDocumentBuilder();
		boolean modelDone = false;
		for (final String v : versions) {
			if (!modelDone) {
				Model actModel = MEMEApp.getResultsDbMinimal().findModel(modelName,v);
				if (actModel == null)
					throw new IllegalStateException("Result not found: " + modelName + "/" + v);

				if (actModel.equals(model)) {
					final Document viewXML = viewBuilder.newDocument();
					final Element viewRoot = viewXML.createElement(ViewCreationRule.NODENAME);
					viewXML.appendChild(viewRoot);
					
					final Element dataElem = viewXML.createElement(ViewCreationRule.INPUT_DATA);
					final Element tableElem = viewXML.createElement(ViewCreationRule.INPUT_TABLE);
					tableElem.setAttribute(ViewCreationRule.INPUT_MODELNAME_ATTR,actModel.getName());
					tableElem.setAttribute(ViewCreationRule.INPUT_MODELVERSION_ATTR,actModel.getVersion());
					dataElem.appendChild(tableElem);
					viewRoot.appendChild(dataElem);
					
					final Element viewNameElem = viewXML.createElement(ViewCreationRule.NAME);
					viewNameElem.setTextContent(viewName);
					viewRoot.appendChild(viewNameElem);
					
					final Element descElem = viewXML.createElement(ViewCreationRule.DESCRIPTION);
					viewRoot.appendChild(descElem);
					
					final Element condElem = viewXML.createElement(ViewCreationRule.CONDITION);
					final Element scalarScriptElem = viewXML.createElement(SrcType.SCALAR_SCRIPT.toString());
					condElem.appendChild(scalarScriptElem);
					viewRoot.appendChild(condElem);
					
					final Element columnsElem = viewXML.createElement(ViewCreationRule.COLUMNS);
					columnsElem.setAttribute(ViewCreationRule.GROUPINGATTR,"false");

					// make column elements for all columns:
					
					final Columns[] columns = MEMEApp.getResultsDbMinimal().getModelColumns(actModel.getModel_id());
					final Columns inputCols = columns[0];
					final Columns outputCols = columns[1];
					for (int i = 0;i < inputCols.size();i++) {
						final Element columnElem = viewXML.createElement(ViewCreationRule.COLUMN);
						columnElem.setAttribute(ViewCreationRule.COL_INITIAL_TYPE,inputCols.get(i).getDatatype().toString());
						columnElem.setAttribute(ViewCreationRule.COL_GRP_ATTR,"true");
						
						final Element colNameElem = viewXML.createElement(ViewCreationRule.COL_NAME);
						colNameElem.setTextContent(inputCols.get(i).getName());
						
						final Element projElem = viewXML.createElement(ViewCreationRule.COL_PROJECTION);
						projElem.setAttribute(ViewCreationRule.COL_PROJ_SRC_ATTR,ViewCreationRule.COL_PROJ_INPUT);
						projElem.setTextContent(inputCols.get(i).getName());
						columnElem.appendChild(colNameElem);
						columnElem.appendChild(projElem);
						columnsElem.appendChild(columnElem);
					}
					
					for (int i = 0;i < outputCols.size();i++) {
						final Element columnElem = viewXML.createElement(ViewCreationRule.COLUMN);
						columnElem.setAttribute(ViewCreationRule.COL_INITIAL_TYPE,outputCols.get(i).getDatatype().toString());
						columnElem.setAttribute(ViewCreationRule.COL_GRP_ATTR,"true");
						
						final Element colNameElem = viewXML.createElement(ViewCreationRule.COL_NAME);
						colNameElem.setTextContent(outputCols.get(i).getName());
						
						final Element projElem = viewXML.createElement(ViewCreationRule.COL_PROJECTION);
						projElem.setAttribute(ViewCreationRule.COL_PROJ_SRC_ATTR,ViewCreationRule.COL_PROJ_OUTPUT);
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

	//----------------------------------------------------------------------------------------------------
	public static void saveViewRule(final Element root) {
		final List<Element> tables = XMLUtils.findAll(root,ViewCreationRule.INPUT_TABLE);
		for (final Element table : tables) {
			String model_version = table.getAttribute(ViewCreationRule.INPUT_MODELVERSION_ATTR);
			if (model_version != null && model_version.length() != 0) { // reference to result table
				final String model_name = table.getAttribute(ViewCreationRule.INPUT_MODELNAME_ATTR);
				if (model_name == null || model_name.length() == 0) {
					Logger.logError("Missing model name attribute");
					return;
				}
				final Model model = MEMEApp.getResultsDb().findModel(model_name,model_version);
				if (model == null) {
					Logger.logError("Invalid reference to model %s",model_name + "/" + model_version);
					return;
				}
				table.setAttribute(ViewCreationRule.INPUT_MODELID_ATTR,String.valueOf(model.getModel_id()));
				table.removeAttribute(ViewCreationRule.INPUT_MODELNAME_ATTR);
				table.removeAttribute(ViewCreationRule.INPUT_MODELVERSION_ATTR);
			} else { // reference to view table
				String viewName = table.getAttribute(ViewCreationRule.INPUT_VIEWNAME_ATTR);
				if (viewName != null && viewName.length() != 0) {
					final Long view_id = MEMEApp.getViewsDb().findView(viewName);
					if (view_id == null) return;
					else
						table.setAttribute(ViewCreationRule.INPUT_VIEWID_ATTR,view_id.toString());
				}
			}
		}
		ViewCreationRule rule = new ViewCreationRule(root);
		ViewCreation vc = new ViewCreation(rule,null);
		try {
			System.gc();
			vc.trun();
		} catch (final Exception e) {
			Logger.logException("ResultsPanel.loadViews() - " + rule.getName(), e);
		}
	}

	//----------------------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(PROCESSING_OK)) {
			processingDialogResult = 0;
			processingDialog.setVisible(false);
		} else if (e.getActionCommand().equals(PROCESSING_CANCEL)) {
			processingDialogResult = -1;
			processingDialog.setVisible(false);
		} 
	}
	
	//----------------------------------------------------------------------------------------------------
//	protected SampleQueryModel createSamplemodel(List<ResultInMem> runs, Element pluginElement){
//		SampleQueryModel sampleModel = null;
//		Vector<MemberInfo> inputs = new Vector<MemberInfo>();
//		ResultInMem first = runs.get(0);
//		Vector<String> inputStrings = new Vector<String>();
//		Vector<Integer> inputIndices = new Vector<Integer>();
//		ParameterComb pc = first.getParameterComb();
//		for (int i = 0; i < pc.getNames().size(); i++) {
//			if(		!pc.getNames().get(i).getDatatype().getJavaClass().equals(Boolean.class) &&
//					!pc.getNames().get(i).getDatatype().getJavaClass().equals(String.class)) {
//				MemberInfo info = new MemberInfo(pc.getNames().get(i).getName(),"Double", null);
//				inputs.add(info);
//				inputStrings.add(info.getName());
//				inputIndices.add(i);
//			}
//		}
//		Columns resultColumns = first.getOutputColumns();
//		Vector<String> resultStrings = new Vector<String>();
//		Vector<Integer> resultIndices = new Vector<Integer>();
//		for (int i = 0; i < resultColumns.size(); i++) {
//			if(Number.class.isAssignableFrom(resultColumns.get(i).getDatatype().getJavaClass())){
//				resultStrings.add(resultColumns.get(i).getName());
//				resultIndices.add(i);
//			}
//		}
//		sampleModel = new SampleQueryModel(inputStrings, resultStrings);
//		for (int i = 0; i < runs.size(); i++) {
//			//the result has the output columns, let's get them:
//			resultColumns = runs.get(i).getOutputColumns();
//			Vector<Double> resultDoubles = new Vector<Double>();
//			Vector<Double> inputDoubles = null;
//			for (int j = 0; j < resultColumns.size(); j++) {
//				double response = (getNumberValue(runs.get(i).getAllRows().iterator().next().get(j).toString())).doubleValue();
//				if(resultIndices.contains(new Integer(j))){
//					resultDoubles.add(response);
//				}
//				//all results have parameter combinations (input parameters):
//				ParameterComb paramComb = runs.get(i).getParameterComb();
//				Vector<Double> inputDoublesTmp = null;
//				if(inputDoubles == null){
//					inputDoublesTmp = new Vector<Double>();
//				}
//				for (int k = 0; k < inputs.size(); k++) {
//					String name = inputs.get(k).getName();
//					int l = 0;
//					while(paramComb.getNames().get(l).getName().compareTo(name) != 0 && l < paramComb.getNames().size()) l++;
//					if(l < paramComb.getNames().size()){
//						Number input = new Integer(-1);
//						Number inputCandidate = getNumberValue(paramComb.getValues().get(l).toString());
//						if(inputCandidate != null) input = inputCandidate; 
//						if(inputDoubles == null && inputIndices.contains(new Integer(k))){
//							inputDoublesTmp.add(input.doubleValue());
//						}
//					}
//				}
//				if(inputDoubles == null){
//					inputDoubles = inputDoublesTmp;
//				}
//            }
//			//adding the sample to the SampleQueryModel:
//			sampleModel.addSample(inputDoubles, resultDoubles);
//        }
//		return sampleModel.getSampleQueryModelWithoutConstants(16);
//	}

	//----------------------------------------------------------------------------------------------------
	public static Number getNumberValue(final String value) {
		Number ret = null;
		try {
			ret = new Double(value);
		} catch (final NumberFormatException e) {
			ret = null;
		}
		if (ret == null) {
			if (value.equalsIgnoreCase("true"))
				return new Double(1.0);
			else
				return new Double(0.0);
		} else 
			return ret;
	}
	
	//----------------------------------------------------------------------------------------------------
	public Document getDocumentForViewName(final String viewName) throws ParserConfigurationException {
		int index = chartConfigViews.indexOf(viewName);
		boolean present = index > -1;
		Document chartXml = null;
		if (!present) {
			index = chartConfigViews.size();
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder chartBuilder = factory.newDocumentBuilder();
			chartXml = chartBuilder.newDocument();
			chartConfigXmls.add(chartXml);
			chartConfigViews.add(viewName);
		} else 
			chartXml = chartConfigXmls.get(index);
		return chartXml;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void displayChartConfig(final Element chartElement, final String viewName) throws ParserConfigurationException, TransformerException, IOException {
		int index = chartConfigViews.indexOf(viewName);
		if (index == -1) {
			final Document newDoc = getDocumentForViewName(viewName);
			index = chartConfigXmls.size();
			chartConfigViews.add(viewName);
			chartConfigXmls.add(newDoc);
		}
		final Document doc = chartConfigXmls.get(index);
		// write it to a temporary file:
		File tempXML = new File("temporaryChartConfig3431hdrhd5352" + chartAdditiveId + Util.getTimeStamp() + ".xml");
		chartAdditiveId++;
		XMLUtils.write(tempXML,doc);
		MEMEApp.getMainWindow().getChartsPanel().loadChartFromXMLAndDelete(tempXML);
	}
}
