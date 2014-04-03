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
package ai.aitia.meme.intelliResultProcess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JPanel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jscience.mathematics.function.Polynomial;
import org.jscience.mathematics.function.Variable;
import org.jscience.mathematics.number.Float64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.database.IResultsDbMinimal;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ParameterComb;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.ResultInMem;
import ai.aitia.meme.intelliResultProcess.rsm.PolynomialResponseModel;
import ai.aitia.meme.intelliResultProcess.rsm.PolynomialRsmGui;
import ai.aitia.meme.intelliResultProcess.rsm.PolynomialRsmInfo;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.pluginmanager.IIntelliResultProcesserPlugin;
import ai.aitia.meme.pluginmanager.IIntelliResultProcesserPluginV2;
import ai.aitia.meme.processing.model.SampleQueryModel;
import ai.aitia.meme.processing.model.VariableInfo;

public abstract class Static_GeneralThreeLevel_Processer implements IIntelliResultProcesserPlugin, IIntelliResultProcesserPluginV2 {

//	private Vector<Vector<LHCFunctionModel>> responseFnModels;
	protected String parametersXMLName = null;
	protected String parameterXMLName = null;
	protected String resultAdditiveName = null;
	protected SampleQueryModel sampleModel = null;
	protected PolynomialRsmGui responseSurfaceMethodGui = null;
	protected double responseMin = Double.POSITIVE_INFINITY;
	protected double responseMax = Double.NEGATIVE_INFINITY;
	protected VariableInfo input1;
	protected VariableInfo input2;
	protected int intervals;
	protected int inputsCount = 0;

	public Document createCharts(String viewName, String model, String version)
			throws ParserConfigurationException {
		if (version.contains("_Polynomials_")) return null;
		Document chartXML = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder chartBuilder = factory.newDocumentBuilder();
		chartXML = chartBuilder.newDocument();
		Element chartRoot = chartXML.createElement("chart");
		chartRoot.setAttribute("singleFlag", "false");
		chartXML.appendChild(chartRoot);
		Element datasourcesElem = chartXML.createElement("datasources");
		chartRoot.appendChild(datasourcesElem);

		for (int i = 0; i < inputsCount; i++) {
			//Setting up a textual datasource for the chart:
			Element factorsDSElem = chartXML.createElement("datasource");
			factorsDSElem.setAttribute("id", "1");
			factorsDSElem.setAttribute("type", "ai.aitia.visu.ds.ISeriesProducer");
			datasourcesElem.appendChild(factorsDSElem);
			Element versionElem1 = chartXML.createElement("property");
			versionElem1.setAttribute("key", "version");
			versionElem1.setTextContent("1.0.80901");
			factorsDSElem.appendChild(versionElem1);
			Element localeElem1 = chartXML.createElement("property");
			localeElem1.setAttribute("key", "locale_specific");
			localeElem1.setTextContent("false");
			factorsDSElem.appendChild(localeElem1);
			Element ascendingElem1 = chartXML.createElement("property");
			ascendingElem1.setAttribute("key", "ascending");
			ascendingElem1.setTextContent("true");
			factorsDSElem.appendChild(ascendingElem1);
			Element caseElem1 = chartXML.createElement("property");
			caseElem1.setAttribute("key", "case_sensitive");
			caseElem1.setTextContent("true");
			factorsDSElem.appendChild(caseElem1);
			Element idElem1 = chartXML.createElement("property");
			idElem1.setAttribute("key", "id");
			//this is the key: "-1." means MEME will look for the view by its name
			//then comes the viewName
			//and finally the column name after a ";"
			idElem1.setTextContent("-1."+viewName+";Value");
			factorsDSElem.appendChild(idElem1);
		}

		//chartconfig of an 2DGridChart
		Element chartconfigElem = chartXML.createElement("chartconfig");
		chartconfigElem.setAttribute("fireInitialEvent", "true");
		//you define the type of the chart here:
		chartconfigElem.setAttribute("type", "Grid2D");
		chartRoot.appendChild(chartconfigElem);

		Element dsElem = chartXML.createElement("property");
		dsElem.setAttribute("key", "datasource");
		//define the datasources used for this chart in a proper order
		dsElem.setTextContent("1");
		chartconfigElem.appendChild(dsElem);

		Element colormapElem = chartXML.createElement("property");
		colormapElem.setAttribute("key", "colormap");
		colormapElem.setTextContent("RAINBOW,"+responseMin+","+responseMax);
		chartconfigElem.appendChild(colormapElem);

		Element envElem = chartXML.createElement("property");
		envElem.setAttribute("key", "environment appearance");
		envElem.setTextContent("normal");
		chartconfigElem.appendChild(envElem);
		
		Element columnLabelElem = chartXML.createElement("property");
		columnLabelElem.setAttribute("key", "column label");
		columnLabelElem.setTextContent(input2.getName()+" ["+input2.getIntervalStart()+".."+input2.getIntervalEnd()+"]");
		chartconfigElem.appendChild(columnLabelElem);

		Element rowLabelElem = chartXML.createElement("property");
		rowLabelElem.setAttribute("key", "row label");
		rowLabelElem.setTextContent(input1.getName()+" ["+input1.getIntervalStart()+".."+input1.getIntervalEnd()+"]");
		chartconfigElem.appendChild(rowLabelElem);

		Element userDefHeightElem = chartXML.createElement("property");
		userDefHeightElem.setAttribute("key", "user_defined height");
		userDefHeightElem.setTextContent("true");
		chartconfigElem.appendChild(userDefHeightElem);

		Element colorElem = chartXML.createElement("property");
		colorElem.setAttribute("key", "color appearance");
		colorElem.setTextContent("colored");
		chartconfigElem.appendChild(colorElem);
		
		Element customElem = chartXML.createElement("property");
		customElem.setAttribute("key", "custom appearance");
		chartconfigElem.appendChild(customElem);
		
		Element colorBarElem = chartXML.createElement("property");
		colorBarElem.setAttribute("key", "colorbar");
		colorBarElem.setTextContent("true");
		chartconfigElem.appendChild(colorBarElem);

		Element titleElem = chartXML.createElement("property");
		titleElem.setAttribute("key", "title");
		//this is the title of the chart
		titleElem.setTextContent(responseSurfaceMethodGui.getSelectedRegressor().getPoly().getResponseName());
		chartconfigElem.appendChild(titleElem);
		
		Element subElem = chartXML.createElement("property");
		subElem.setAttribute("key", "subtitle");
		//you can specify a subtitle as well:
		subElem.setTextContent("");
		chartconfigElem.appendChild(subElem);
		
		Element rowOrderElem = chartXML.createElement("property");
		rowOrderElem.setAttribute("key", "row order");
		rowOrderElem.setTextContent("true");
		chartconfigElem.appendChild(rowOrderElem);

		Element heightElem = chartXML.createElement("property");
		heightElem.setAttribute("key", "height");
		heightElem.setTextContent(""+intervals);
		chartconfigElem.appendChild(heightElem);

		Element widthElem = chartXML.createElement("property");
		widthElem.setAttribute("key", "width");
		widthElem.setTextContent(""+intervals);
		chartconfigElem.appendChild(widthElem);

		Element userDefWidthElem = chartXML.createElement("property");
		userDefWidthElem.setAttribute("key", "user defined width");
		userDefWidthElem.setTextContent("true");
		chartconfigElem.appendChild(userDefWidthElem);

		Element tooltipElem = chartXML.createElement("property");
		tooltipElem.setAttribute("key", "tooltip");
		tooltipElem.setTextContent("false");
		chartconfigElem.appendChild(tooltipElem);
		
		return chartXML;
	}

	public List<ResultInMem> processResultFiles(IResultsDbMinimal db,
			List<ResultInMem> runs, Element pluginElement) {
		//TODO: set this 'doNotUseThisProcesser' variable to false to regain functionality of this processer:
		boolean doNotUseThisProcesser = true;
		if (doNotUseThisProcesser) {
			return new Vector<ResultInMem>();
		}
		
		inputsCount = 0;
		NodeList nl = null;
		Vector<LatinFactorInfo> inputs = null;
		Vector<String> inputStrings = null;
		Vector<Integer> inputIndices = null;
		nl = pluginElement.getElementsByTagName(parametersXMLName);
		if(nl != null && nl.getLength() > 0){
			Element designElem = (Element) nl.item(0);
			inputs = new Vector<LatinFactorInfo>();
			inputStrings = new Vector<String>();
			inputIndices = new Vector<Integer>();
			nl = null;
			nl = designElem.getElementsByTagName(parameterXMLName);
			if(nl != null && nl.getLength() > 0){
				for (int i = 0; i < nl.getLength(); i++) {
	                Element lfiElem = (Element) nl.item(i);
	                if(lfiElem.getAttribute("inDesign").compareToIgnoreCase("true") == 0){
	                	inputsCount++;
	                	LatinFactorInfo lfi = new LatinFactorInfo(lfiElem.getAttribute("name"), lfiElem.getAttribute("type"), null);
//	                	lfi.load(lfiElem);
	                	inputs.add(lfi);
	                	if(!(lfi.getType().equalsIgnoreCase("boolean") || lfi.getType().equalsIgnoreCase("string"))){
	                		inputStrings.add(lfi.getName());
	                		inputIndices.add(inputs.size()-1); //the index of the last added element
	                	}
	                }
                }
				//we have all the nuisance factors in 'inputs'
			} else{
				throw new IllegalArgumentException("The plugin XML element is corrupt.");
			}
		} else{
			throw new IllegalArgumentException("The plugin XML element is corrupt.");
		}
		Columns resultColumns = runs.get(0).getOutputColumns();
		Vector<String> resultStrings = new Vector<String>();
		Vector<Integer> resultIndices = new Vector<Integer>();
		for (int i = 0; i < resultColumns.size(); i++) {
			if(Number.class.isAssignableFrom(resultColumns.get(i).getDatatype().getJavaClass())){
				resultStrings.add(resultColumns.get(i).getName());
				resultIndices.add(i);
			}
		}
		sampleModel = new SampleQueryModel(inputStrings, resultStrings);
		@SuppressWarnings("unused")
		int resultSize = resultColumns.size();
//		responseFnModels = new Vector<Vector<LHCFunctionModel>>(inputsCount);
//		for (int i = 0; i < inputsCount; i++) {
//	        responseFnModels.add(new Vector<LHCFunctionModel>(resultSize));
//	        for (int j = 0; j < resultSize; j++) {
//	            responseFnModels.get(i).add(new LHCFunctionModel(inputs.get(i).getName(),resultColumns.get(j).getName()));
//            }
//        }
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
//						responseFnModels.get(k).get(j).addResponseValue(input, response);
						if(inputDoubles == null && inputIndices.contains(new Integer(k))){
							//System.out.println("run no.: "+i);
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
		//
		//the List of ResultInMems that the method returns:
		ArrayList<ResultInMem> ret = new ArrayList<ResultInMem>();
		//let's create a new model:
		String newModelName = runs.get(0).getModel().getName();
		String timeStamp = Util.getTimeStamp();
		String version = runs.get(0).getModel().getVersion()+resultAdditiveName+timeStamp;
		Model model = db.findModel(newModelName, version);
		if (model == null)
			model = new Model(Model.NONEXISTENT_MODELID, newModelName, version);
		//generate batch number
		int b = db.getNewBatch(model);

/*		Columns cols = new Columns();
		intelliSweepResultColumns = new Columns();
		for (int i = 0; i < responseFnModels.size(); i++) {
			intelliSweepResultColumns.append(responseFnModels.get(i).get(0).getNuisanceName(), ColumnType.DOUBLE);
	        for (int j = 0; j < responseFnModels.get(i).size(); j++) {
	        	intelliSweepResultColumns.append(responseFnModels.get(i).get(j).getResponseName() + " for " + responseFnModels.get(i).get(0).getNuisanceName(), ColumnType.DOUBLE);
            }
        }
		
		for (int i = 0; i < responseFnModels.get(0).get(0).getLevels().size(); i++) {
			ResultInMem toAdd = new ResultInMem();
			Result.Row row = new Result.Row(intelliSweepResultColumns, 0); //0 is the tick#
			ParameterComb pc = new ParameterComb(new GeneralRow(cols));
			toAdd.setModel(model);
			toAdd.setBatch(b);
			toAdd.setStartTime(runs.get(0).getStartTime());
			toAdd.setEndTime(runs.get(0).getEndTime());
			toAdd.setRun(i);//the run#
	        for (int j = 0; j < responseFnModels.size(); j++) {
	        	row.set(j*(responseFnModels.get(j).size()+1), responseFnModels.get(j).get(0).getLevels().get(i));
	            for (int k = 0; k < responseFnModels.get(j).size(); k++) {
	            	row.set(j*(responseFnModels.get(j).size()+1)+k+1, responseFnModels.get(j).get(k).getFunctionModel().get(i));
	            }
            }
	        toAdd.setParameterComb(pc);
	        toAdd.add(row);
	        ret.add(toAdd);
        } */
		
		//Innen van a polinomos r�sz:
		responseSurfaceMethodGui = new PolynomialRsmGui(new PolynomialResponseModel(sampleModel));
		responseSurfaceMethodGui.showModelListDialog();
		String versionPoly = runs.get(0).getModel().getVersion()+"_Polynomials_"+timeStamp;
		model = db.findModel(newModelName, versionPoly);
		if (model == null)
			model = new Model(Model.NONEXISTENT_MODELID, newModelName, versionPoly);
		//generate batch number
		b = db.getNewBatch(model);
		//putting the polynomials into the table
		Columns colsIn = new Columns();
		Columns colsOut = new Columns();
		colsOut.append("Polynomials", ColumnType.STRING);
		colsOut.append("Response name", ColumnType.STRING);
		Vector<PolynomialRsmInfo> polyInfos = responseSurfaceMethodGui.getPolyInfos();
		for (int i = 0; i < polyInfos.size(); i++) {
			ResultInMem toAdd = new ResultInMem();
			Result.Row row = new Result.Row(colsOut, 0); //0 is the tick#
			ParameterComb pc = new ParameterComb(new GeneralRow(colsIn));
			toAdd.setModel(model);
			toAdd.setBatch(b);
			toAdd.setStartTime(runs.get(0).getStartTime());
			toAdd.setEndTime(runs.get(0).getEndTime());
			toAdd.setRun(i);//the run#
        	row.set(0, polyInfos.get(i).getPolynomialAsString());
           	row.set(1, polyInfos.get(i).getPoly().getResponseName());
	        toAdd.setParameterComb(pc);
	        toAdd.add(row);
	        ret.add(toAdd);
		}
		
		//create chart data:
		Polynomial<Float64> poly = responseSurfaceMethodGui.getSelectedRegressor().getPoly().getRegressor();
		input1 = responseSurfaceMethodGui.getSelectedRegressor().getPoly().getVariableInfos().get(responseSurfaceMethodGui.getInput1Index());
		input2 = responseSurfaceMethodGui.getSelectedRegressor().getPoly().getVariableInfos().get(responseSurfaceMethodGui.getInput2Index());
		Vector<VariableInfo> otherInputs = new Vector<VariableInfo>(responseSurfaceMethodGui.getSelectedRegressor().getPoly().getVariableInfos());
		for (Iterator iter = otherInputs.iterator(); iter.hasNext();) {
			VariableInfo info = (VariableInfo) iter.next();
			if(info.getName().equals(input1.getName())){
				iter.remove();
			} else if(info.getName().equals(input2.getName())){
				iter.remove();
			}
		}
		String resolution = responseSurfaceMethodGui.getResolution();
		intervals = 40;
		if(resolution.equals(PolynomialRsmGui.LOW_RES)){
			intervals = 40;
		} else if(resolution.equals(PolynomialRsmGui.MEDIUM_RES)){
			intervals = 60;
		} else if(resolution.equals(PolynomialRsmGui.HIGH_RES)){
			intervals = 80;
		}
		Vector<Double> input1Vals = new Vector<Double>(intervals);
		Vector<Double> input2Vals = new Vector<Double>(intervals);
		double input1Div = (input1.getIntervalEnd() - input1.getIntervalStart()) / (intervals - 1.0);
		double input2Div = (input2.getIntervalEnd() - input2.getIntervalStart()) / (intervals - 1.0);
		for (int i = 0; i < intervals-1; i++) {
			input1Vals.add(input1.getIntervalStart() + (input1Div * i));
			input2Vals.add(input2.getIntervalStart() + (input2Div * i));
		}
		input1Vals.add(input1.getIntervalEnd());
		input2Vals.add(input2.getIntervalEnd());

		String versionChart = runs.get(0).getModel().getVersion()+"_ChartData_"+timeStamp;
		model = db.findModel(newModelName, versionChart);
		if (model == null)
			model = new Model(Model.NONEXISTENT_MODELID, newModelName, versionChart);
		//generate batch number
		b = db.getNewBatch(model);
		colsIn = new Columns();
		colsOut = new Columns();
		colsOut.append("Value", ColumnType.DOUBLE);
		Variable<Float64> input1Var = null;
		Variable<Float64> input2Var = null;
		for (int i = 0; i < poly.getVariables().size(); i++) {
			if(poly.getVariables().get(i).getSymbol().equals(input1.getName())){
				input1Var = poly.getVariables().get(i);
			} else if(poly.getVariables().get(i).getSymbol().equals(input2.getName())){
				input2Var = poly.getVariables().get(i);
			} else{
				for (int j = 0; j < otherInputs.size(); j++) {
					if(poly.getVariables().get(i).getSymbol().equals(otherInputs.get(j).getName())){
						poly.getVariables().get(i).set(Float64.valueOf(otherInputs.get(j).getIntervalCenter()));
					}
				}
			}
		}
		for (int i = 0; i < input1Vals.size(); i++) {
			for (int j = 0; j < input2Vals.size(); j++) {
				int runNo = i * intervals + j;
				ResultInMem toAdd = new ResultInMem();
				Result.Row row = new Result.Row(colsOut, 0); //0 is the tick#
				ParameterComb pc = new ParameterComb(new GeneralRow(colsIn));
				toAdd.setModel(model);
				toAdd.setBatch(b);
				toAdd.setStartTime(runs.get(0).getStartTime());
				toAdd.setEndTime(runs.get(0).getEndTime());
				toAdd.setRun(runNo);//the run#
				input1Var.set(Float64.valueOf(input1Vals.get(i)));
				input2Var.set(Float64.valueOf(input2Vals.get(j)));
				double response = poly.evaluate().doubleValue();
				if(response > responseMax) responseMax = response;
				if(response < responseMin) responseMin = response;
	        	row.set(0, response);
		        toAdd.setParameterComb(pc);
		        toAdd.add(row);
		        ret.add(toAdd);
			}
		}
	    return ret;
	}

	public void doGenericProcessing() {
	}

	public JPanel getGenericProcessPanel(Element pluginElement) {
		return null;
	}

	public boolean isGenericProcessingSupported() {
		return false;
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
	
	//====================================================================================================
	// implementing IIntelliResultProcesserPluginV2
	
	//----------------------------------------------------------------------------------------------------
	public List<String> processResultFiles(final IResultsDbMinimal db, final List<Result> runs, final Element pluginElement, 
										   final String isPluginXMLFileName) throws Exception {
		//TODO: set this 'doNotUseThisProcesser' variable to false to regain functionality of this processer:
		boolean doNotUseThisProcesser = true;
		if (doNotUseThisProcesser) 
			return new ArrayList<String>();
		
		inputsCount = 0;
		NodeList nl = null;
		Vector<LatinFactorInfo> inputs = null;
		Vector<String> inputStrings = null;
		Vector<Integer> inputIndices = null;
		nl = pluginElement.getElementsByTagName(parametersXMLName);
		if (nl != null && nl.getLength() > 0) {
			final Element designElem = (Element) nl.item(0);
			inputs = new Vector<LatinFactorInfo>();
			inputStrings = new Vector<String>();
			inputIndices = new Vector<Integer>();
			nl = null;
			nl = designElem.getElementsByTagName(parameterXMLName);
			if (nl != null && nl.getLength() > 0) {
				for (int i = 0; i < nl.getLength(); i++) {
	                final Element lfiElem = (Element) nl.item(i);
	                if (lfiElem.getAttribute("inDesign").compareToIgnoreCase("true") == 0) {
	                	inputsCount++;
	                	final LatinFactorInfo lfi = new LatinFactorInfo(lfiElem.getAttribute("name"),lfiElem.getAttribute("type"),null);
//		               	lfi.load(lfiElem);
	                	inputs.add(lfi);
	                	if (!(lfi.getType().equalsIgnoreCase("boolean") || lfi.getType().equalsIgnoreCase("string"))) {
	                		inputStrings.add(lfi.getName());
	                		inputIndices.add(inputs.size()-1); //the index of the last added element
	                	}
	                }
                }
				//we have all the nuisance factors in 'inputs'
			} else 
				throw new IllegalArgumentException("The plugin XML element is corrupt.");
		} else
			throw new IllegalArgumentException("The plugin XML element is corrupt.");
		
		Columns resultColumns = runs.get(0).getOutputColumns();
		final Vector<String> resultStrings = new Vector<String>();
		final Vector<Integer> resultIndices = new Vector<Integer>();
		for (int i = 0;i < resultColumns.size();i++) {
			if (Number.class.isAssignableFrom(resultColumns.get(i).getDatatype().getJavaClass())) {
				resultStrings.add(resultColumns.get(i).getName());
				resultIndices.add(i);
			}
		}
		sampleModel = new SampleQueryModel(inputStrings,resultStrings);
		final int resultSize = resultColumns.size();

		for (int i = 0; i < runs.size(); i++) {
			//the result has the output columns, let's get them:
			resultColumns = runs.get(i).getOutputColumns();
			final Vector<Double> resultDoubles = new Vector<Double>();
			Vector<Double> inputDoubles = null;
			for (int j = 0; j < resultSize; j++) {
				final double response = (getNumberValue(runs.get(i).getAllRows().iterator().next().get(j).toString())).doubleValue();
				if (resultIndices.contains(new Integer(j)))
					resultDoubles.add(response);
				
				//all results have parameter combinations (input parameters):
				final ParameterComb paramComb = runs.get(i).getParameterComb();
				Vector<Double> inputDoublesTmp = null;
				if (inputDoubles == null)
					inputDoublesTmp = new Vector<Double>();
				for (int k = 0;k < inputs.size();k++) {
					final String name = inputs.get(k).getName();
					int kk = 0;
					while(paramComb.getNames().get(kk).getName().compareTo(name) != 0 && kk < paramComb.getNames().size())
						kk++;
					if (kk < paramComb.getNames().size()) {
						Number input = new Integer(-1);
						final Number inputCandidate = getNumberValue(paramComb.getValues().get(kk).toString());
						if (inputCandidate != null)
							input = inputCandidate; 
//						responseFnModels.get(k).get(j).addResponseValue(input, response);
						if (inputDoubles == null && inputIndices.contains(new Integer(k))) {
							//System.out.println("run no.: "+i);
							inputDoublesTmp.add(input.doubleValue());
						}
					}
				}
				if (inputDoubles == null) 
					inputDoubles = inputDoublesTmp;
            }
			//adding the sample to the SampleQueryModel:
			sampleModel.addSample(inputDoubles,resultDoubles);
        }
		
		final List<String> ret = new ArrayList<String>();
		
		//let's create a new model:
		final String newModelName = runs.get(0).getModel().getName();
		final String timestamp = Util.getTimeStampFromXMLFileName(isPluginXMLFileName);
		final String version = runs.get(0).getModel().getVersion()+ resultAdditiveName + timestamp;
		Model model = db.findModel(newModelName,version);
		if (model == null)
			model = new Model(Model.NONEXISTENT_MODELID, newModelName, version);
		
		//generate batch number
		int b = db.getNewBatch(model);

/*      ret.add(version);		
  		Columns cols = new Columns();
		intelliSweepResultColumns = new Columns();
		for (int i = 0;i < responseFnModels.size();i++) {
			intelliSweepResultColumns.append(responseFnModels.get(i).get(0).getNuisanceName(),ColumnType.DOUBLE);
	        for (int j = 0;j < responseFnModels.get(i).size();j++) 
	        	intelliSweepResultColumns.append(responseFnModels.get(i).get(j).getResponseName() + " for " + responseFnModels.get(i).get(0).getNuisanceName(),ColumnType.DOUBLE);
        }
		
		for (int i = 0;i < responseFnModels.get(0).get(0).getLevels().size();i++) {
			ResultInMem toAdd = new ResultInMem();
			Result.Row row = new Result.Row(intelliSweepResultColumns,0); // 0 is the tick#
			ParameterComb pc = new ParameterComb(new GeneralRow(cols));
			toAdd.setModel(model);
			toAdd.setBatch(b);
			toAdd.setStartTime(runs.get(0).getStartTime());
			toAdd.setEndTime(runs.get(0).getEndTime());
			toAdd.setRun(i); // the run#
	        for (int j = 0;j < responseFnModels.size();j++) {
	        	row.set(j * (responseFnModels.get(j).size() + 1),responseFnModels.get(j).get(0).getLevels().get(i));
	            for (int k = 0;k < responseFnModels.get(j).size();k++) 
	            	row.set(j * (responseFnModels.get(j).size() + 1) + k + 1,responseFnModels.get(j).get(k).getFunctionModel().get(i));
            }
	        toAdd.setParameterComb(pc);
	        toAdd.add(row);
			db.addResult(toAdd);
        } */
		
		//Innen van a polinomos r�sz:
		responseSurfaceMethodGui = new PolynomialRsmGui(new PolynomialResponseModel(sampleModel));
		responseSurfaceMethodGui.showModelListDialog();
		final String versionPoly = runs.get(0).getModel().getVersion() + "_Polynomials_" + timestamp;
		model = db.findModel(newModelName,versionPoly);
		if (model == null)
			model = new Model(Model.NONEXISTENT_MODELID,newModelName,versionPoly);
		
		//generate batch number
		b = db.getNewBatch(model);
		
		//putting the polynomials into the table
		ret.add(versionPoly);
		Columns colsIn = new Columns();
		Columns colsOut = new Columns();
		colsOut.append("Polynomials",ColumnType.STRING);
		colsOut.append("Response name",ColumnType.STRING);
		final Vector<PolynomialRsmInfo> polyInfos = responseSurfaceMethodGui.getPolyInfos();
		for (int i = 0;i < polyInfos.size();i++) {
			final ResultInMem toAdd = new ResultInMem();
			final Result.Row row = new Result.Row(colsOut,0); // 0 is the tick#
			final ParameterComb pc = new ParameterComb(new GeneralRow(colsIn));
			toAdd.setModel(model);
			toAdd.setBatch(b);
			toAdd.setStartTime(runs.get(0).getStartTime());
			toAdd.setEndTime(runs.get(0).getEndTime());
			toAdd.setRun(i); // the run#
        	row.set(0,polyInfos.get(i).getPolynomialAsString());
           	row.set(1,polyInfos.get(i).getPoly().getResponseName());
	        toAdd.setParameterComb(pc);
	        toAdd.add(row);
	        db.addResult(toAdd);
		}
		
		//create chart data:
		final Polynomial<Float64> poly = responseSurfaceMethodGui.getSelectedRegressor().getPoly().getRegressor();
		input1 = responseSurfaceMethodGui.getSelectedRegressor().getPoly().getVariableInfos().get(responseSurfaceMethodGui.getInput1Index());
		input2 = responseSurfaceMethodGui.getSelectedRegressor().getPoly().getVariableInfos().get(responseSurfaceMethodGui.getInput2Index());
		final Vector<VariableInfo> otherInputs = new Vector<VariableInfo>(responseSurfaceMethodGui.getSelectedRegressor().getPoly().getVariableInfos());
		for (final Iterator iter = otherInputs.iterator();iter.hasNext();) {
			final VariableInfo info = (VariableInfo) iter.next();
			if (info.getName().equals(input1.getName())) 
				iter.remove();
			else if (info.getName().equals(input2.getName()))
				iter.remove();
		}
		final String resolution = responseSurfaceMethodGui.getResolution();
		intervals = 40;
		if (resolution.equals(PolynomialRsmGui.LOW_RES))
			intervals = 40;
		else if (resolution.equals(PolynomialRsmGui.MEDIUM_RES))
			intervals = 60;
		else if (resolution.equals(PolynomialRsmGui.HIGH_RES))
			intervals = 80;
		
		final Vector<Double> input1Vals = new Vector<Double>(intervals);
		final Vector<Double> input2Vals = new Vector<Double>(intervals);
		final double input1Div = (input1.getIntervalEnd() - input1.getIntervalStart()) / (intervals - 1.0);
		final double input2Div = (input2.getIntervalEnd() - input2.getIntervalStart()) / (intervals - 1.0);
		for (int i = 0;i < intervals - 1;i++) {
			input1Vals.add(input1.getIntervalStart() + (input1Div * i));
			input2Vals.add(input2.getIntervalStart() + (input2Div * i));
		}
		input1Vals.add(input1.getIntervalEnd());
		input2Vals.add(input2.getIntervalEnd());

		final String versionChart = runs.get(0).getModel().getVersion() + "_ChartData_" + timestamp;
		model = db.findModel(newModelName,versionChart);
		if (model == null)
			model = new Model(Model.NONEXISTENT_MODELID,newModelName,versionChart);
		
		//generate batch number
		b = db.getNewBatch(model);
		
		ret.add(versionChart);
		colsIn = new Columns();
		colsOut = new Columns();
		colsOut.append("Value",ColumnType.DOUBLE);
		Variable<Float64> input1Var = null;
		Variable<Float64> input2Var = null;
		for (int i = 0;i < poly.getVariables().size();i++) {
			if (poly.getVariables().get(i).getSymbol().equals(input1.getName()))
				input1Var = poly.getVariables().get(i);
			else if (poly.getVariables().get(i).getSymbol().equals(input2.getName()))
				input2Var = poly.getVariables().get(i);
			else {
				for (int j = 0;j < otherInputs.size();j++) {
					if (poly.getVariables().get(i).getSymbol().equals(otherInputs.get(j).getName())) 
						poly.getVariables().get(i).set(Float64.valueOf(otherInputs.get(j).getIntervalCenter()));
				}
			}
		}
		for (int i = 0;i < input1Vals.size();i++) {
			for (int j = 0;j < input2Vals.size();j++) {
				final int runNo = i * intervals + j;
				final ResultInMem toAdd = new ResultInMem();
				final Result.Row row = new Result.Row(colsOut, 0); // 0 is the tick#
				final ParameterComb pc = new ParameterComb(new GeneralRow(colsIn));
				toAdd.setModel(model);
				toAdd.setBatch(b);
				toAdd.setStartTime(runs.get(0).getStartTime());
				toAdd.setEndTime(runs.get(0).getEndTime());
				toAdd.setRun(runNo);//the run#
				input1Var.set(Float64.valueOf(input1Vals.get(i)));
				input2Var.set(Float64.valueOf(input2Vals.get(j)));
				final double response = poly.evaluate().doubleValue();
				if (response > responseMax)
					responseMax = response;
				if (response < responseMin)
					responseMin = response;
	        	row.set(0, response);
		        toAdd.setParameterComb(pc);
		        toAdd.add(row);
		        db.addResult(toAdd);
			}
		}
		return ret;
	}

}
