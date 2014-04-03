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
import java.util.List;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.pluginmanager.IIntelliResultProcesserPlugin;
import ai.aitia.meme.pluginmanager.IIntelliResultProcesserPluginV2;
import ai.aitia.meme.pluginmanager.Parameter;

/**
 * This is an example implementation of the IIntelliResultProcesserPlugin
 * interface. 
 * 
 * @author Ferschl
 *
 */
public class Static_LatinHypercube_Processer implements IIntelliResultProcesserPlugin, IIntelliResultProcesserPluginV2 {
	
	@SuppressWarnings("unused")
	private JDialog optionsDialog = null;
	@SuppressWarnings("unused")
	private JCheckBox relativeDifferenceRankingCheckBox = new JCheckBox("Ranking by the deviation");
	@SuppressWarnings("unused")
	private JCheckBox linearityRankingCheckBox = new JCheckBox("Ranking by the linearity");
	@SuppressWarnings("unused")
	private static final String GENERATED_FUNCTION = "Fn";
	@SuppressWarnings("unused")
	private static final String NUISANCE_MAP_NAME = "_nuisance";
	@SuppressWarnings("unused")
	private static final String RESPONSE_MAP_NAME = "_response";
	private Columns intelliSweepResultColumns;
	private Vector<Vector<LHCFunctionModel>> responseFnModels;
	
	

	public String getLocalizedName() {
		return "Latin Hypercube Design_Processer";
	}

	public List<ResultInMem> processResultFiles(IResultsDbMinimal db,
	        List<ResultInMem> runs, Element pluginElement) {
		
		int nuisancesCount = 0;
		int levelsCount = 0;
		NodeList nl = null;
		Vector<LatinFactorInfo> nuisances = null;
		nl = pluginElement.getElementsByTagName("design");
		if(nl != null && nl.getLength() > 0){
			Element designElem = (Element) nl.item(0);
			levelsCount = Integer.parseInt(designElem.getAttribute("levels"));
			nuisancesCount = Integer.parseInt(designElem.getAttribute("nuisances"));
			nuisances = new Vector<LatinFactorInfo>(nuisancesCount);
			nl = null;
			nl = designElem.getElementsByTagName("LatinFactorInfo");
			if(nl != null && nl.getLength() > 0){
				for (int i = 0; i < nl.getLength(); i++) {
	                Element lfiElem = (Element) nl.item(i);
	                if(lfiElem.getAttribute("nuisance").compareToIgnoreCase("true") == 0){
	                	LatinFactorInfo lfi = new LatinFactorInfo(lfiElem.getAttribute("name"), lfiElem.getAttribute("type"), null);
	                	lfi.load(lfiElem);
	                	nuisances.add(lfi);
	                }
                }
				//we have all the nuisance factors in 'nuisances'
			} else{
				throw new IllegalArgumentException("The plugin XML element is corrupt.");
			}
		} else{
			throw new IllegalArgumentException("The plugin XML element is corrupt.");
		}
		Columns resultColumns = runs.get(0).getOutputColumns();
		int resultSize = resultColumns.size();
		responseFnModels = new Vector<Vector<LHCFunctionModel>>(nuisancesCount);
		for (int i = 0; i < nuisancesCount; i++) {
	        responseFnModels.add(new Vector<LHCFunctionModel>(resultSize));
	        for (int j = 0; j < resultSize; j++) {
	            responseFnModels.get(i).add(new LHCFunctionModel(nuisances.get(i).getName(),resultColumns.get(j).getName(),levelsCount));
	            responseFnModels.get(i).get(j).setLevels(nuisances.get(i).getLevels());
            }
        }
		for (int i = 0; i < runs.size(); i++) {
			//the result has the output columns, let's get them:
			resultColumns = runs.get(i).getOutputColumns();
			for (int j = 0; j < resultColumns.size(); j++) {
				@SuppressWarnings("unused")
				Parameter resultColumn = resultColumns.get(j);
				//all results have parameter combinations (input parameters):
				ParameterComb paramComb = runs.get(i).getParameterComb();
				for (int k = 0; k < nuisances.size(); k++) {
					String name = nuisances.get(k).getName();
					int l = 0;
					while(l < paramComb.getNames().size() && paramComb.getNames().get(l).getName().compareTo(name) != 0) l++;
					if(l < paramComb.getNames().size()){
						Number input = LatinFactorInfo.getNumberValue(paramComb.getValues().get(l).toString(), nuisances.get(k).getType());
						double response = (LatinFactorInfo.getNumberValue(runs.get(i).getAllRows().iterator().next().get(j).toString(), "double")).doubleValue();
						responseFnModels.get(k).get(j).addResponseValue(input, response);
					}
				}
            }
        }
		//
		//the List of ResultInMems that the method returns:
		ArrayList<ResultInMem> ret = new ArrayList<ResultInMem>();
		//let's create a new model:
		String newModelName = runs.get(0).getModel().getName();
		String version = runs.get(0).getModel().getVersion()+"_Latin_Hypercube_Results"+Util.getTimeStamp();
		Model model = db.findModel(newModelName, version);
		if (model == null)
			model = new Model(Model.NONEXISTENT_MODELID, newModelName, version);
		//generate batch number
		int b = db.getNewBatch(model);

		Columns cols = new Columns();
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
        }
	    return ret;
	}

	public Document createCharts(String viewName, String model, String version)
	throws ParserConfigurationException {
		Document chartXML = null;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder chartBuilder = factory.newDocumentBuilder();
		chartXML = chartBuilder.newDocument();
		Element chartRoot = chartXML.createElement("chart");
		chartRoot.setAttribute("singleFlag", "false");
		chartXML.appendChild(chartRoot);
		Element datasourcesElem = chartXML.createElement("datasources");
		chartRoot.appendChild(datasourcesElem);

		//These two for-iterations may be replaced by one...
		for (int i = 0; i < responseFnModels.size(); i++) {
			//Setting up a textual datasource for the chart:
			Element factorsDSElem = chartXML.createElement("datasource");
			int id = new Integer(i*(responseFnModels.get(0).size()+1));
			factorsDSElem.setAttribute("id", ""+id);
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
			idElem1.setTextContent("-1."+viewName+";"+intelliSweepResultColumns.get(id).getName());
			factorsDSElem.appendChild(idElem1);
		}
		for (int i = 0; i < responseFnModels.size(); i++) {
			for (int j = 0; j < responseFnModels.get(i).size(); j++) {
				//Setting up a double datasource:
				Element effectsDSElem = chartXML.createElement("datasource");
				int id = new Integer(i*(responseFnModels.get(i).size()+1)+j+1);
				effectsDSElem.setAttribute("id", ""+id);
				effectsDSElem.setAttribute("type", "ai.aitia.visu.ds.ISeriesProducer");
				datasourcesElem.appendChild(effectsDSElem);
				Element versionElem2 = chartXML.createElement("property");
				versionElem2.setAttribute("key", "version");
				versionElem2.setTextContent("1.0.80901");
				effectsDSElem.appendChild(versionElem2);
				Element localeElem2 = chartXML.createElement("property");
				localeElem2.setAttribute("key", "locale_specific");
				localeElem2.setTextContent("false");
				effectsDSElem.appendChild(localeElem2);
				Element ascendingElem2 = chartXML.createElement("property");
				ascendingElem2.setAttribute("key", "ascending");
				ascendingElem2.setTextContent("true");
				effectsDSElem.appendChild(ascendingElem2);
				Element caseElem2 = chartXML.createElement("property");
				caseElem2.setAttribute("key", "case_sensitive");
				caseElem2.setTextContent("true");
				effectsDSElem.appendChild(caseElem2);
				Element idElem2 = chartXML.createElement("property");
				idElem2.setAttribute("key", "id");
				idElem2.setTextContent("-1."+viewName+";"+intelliSweepResultColumns.get(id).getName());
				effectsDSElem.appendChild(idElem2);

			}
		}


		for (int i = 0; i < responseFnModels.size(); i++) {
			int nuisID = i * (responseFnModels.get(i).size() + 1);
			for (int j = 0; j < responseFnModels.get(i).size(); j++) {
				int respID = i * (responseFnModels.get(i).size() + 1) + j + 1;
				//chartconfig of an XYLineChart
				Element chartconfigElem = chartXML.createElement("chartconfig");
				chartconfigElem.setAttribute("fireInitialEvent", "true");
				//you define the type of the chart here:
				chartconfigElem.setAttribute("type", "XYLineChart");
				chartRoot.appendChild(chartconfigElem);

				Element dsElem = chartXML.createElement("property");
				dsElem.setAttribute("key", "datasource");
				//define the datasources used for this chart in a proper order
				dsElem.setTextContent(""+nuisID+" "+respID);
				chartconfigElem.appendChild(dsElem);

				Element xAxisElem = chartXML.createElement("property");
				xAxisElem.setAttribute("key", "x-axis");
				xAxisElem.setTextContent(responseFnModels.get(i).get(j).getNuisanceName());
				chartconfigElem.appendChild(xAxisElem);

				Element yAxisElem = chartXML.createElement("property");
				yAxisElem.setAttribute("key", "y-axis");
				yAxisElem.setTextContent(responseFnModels.get(i).get(j).getResponseName());
				chartconfigElem.appendChild(yAxisElem);

				Element xScaleElem = chartXML.createElement("property");
				xScaleElem.setAttribute("key", "x-axis scale");
				xScaleElem.setTextContent("NORMAL");
				chartconfigElem.appendChild(xScaleElem);

				Element yScaleElem = chartXML.createElement("property");
				yScaleElem.setAttribute("key", "y-axis scale");
				yScaleElem.setTextContent("NORMAL");
				chartconfigElem.appendChild(yScaleElem);

				Element xyLineRendererElem = chartXML.createElement("property");
				xyLineRendererElem.setAttribute("key", "xy line renderer");
				xyLineRendererElem.setTextContent("0");
				chartconfigElem.appendChild(xyLineRendererElem);

				Element envElem = chartXML.createElement("property");
				envElem.setAttribute("key", "environment appearance");
				envElem.setTextContent("normal");
				chartconfigElem.appendChild(envElem);

				Element colorElem = chartXML.createElement("property");
				colorElem.setAttribute("key", "color appearance");
				colorElem.setTextContent("colored");
				chartconfigElem.appendChild(colorElem);

				//???????
//				Element angleElem = chartXML.createElement("property");
//				angleElem.setAttribute("key", "label angle");
				//this property controls the angle of the labels on the barchart
//				angleElem.setTextContent("90");
//				chartconfigElem.appendChild(angleElem);

				Element customElem = chartXML.createElement("property");
				customElem.setAttribute("key", "custom appearance");
				chartconfigElem.appendChild(customElem);

				Element legendElem = chartXML.createElement("property");
				legendElem.setAttribute("key", "show legend");
				legendElem.setTextContent("true");
				chartconfigElem.appendChild(legendElem);

				Element shortLegendElem = chartXML.createElement("property");
				shortLegendElem.setAttribute("key", "short legend names");
				shortLegendElem.setTextContent("true");
				chartconfigElem.appendChild(shortLegendElem);

				Element titleElem = chartXML.createElement("property");
				titleElem.setAttribute("key", "title");
				//this is the title of the chart
				titleElem.setTextContent(responseFnModels.get(i).get(j).getNuisanceName() + " - " + responseFnModels.get(i).get(j).getResponseName() + " function in " + model + " model");
				chartconfigElem.appendChild(titleElem);

				Element subElem = chartXML.createElement("property");
				subElem.setAttribute("key", "subtitle");
				//you can specify a subtitle as well:
//				subElem.setTextContent("of "+model+" model, version: "+version);
				subElem.setTextContent("");
				chartconfigElem.appendChild(subElem);
			}
		}

		return chartXML;
	}

	public void doGenericProcessing() {
	}

	public JPanel getGenericProcessPanel(Element pluginElement) {
		return null;
	}

	public boolean isGenericProcessingSupported() {
		return false;
	}

	//====================================================================================================
	// implementing IIntelliResultProcesserPluginV2
	
	//----------------------------------------------------------------------------------------------------
	public List<String> processResultFiles(final IResultsDbMinimal db, final List<Result> runs, final Element pluginElement, 
										   final String isPluginXMLFileName) throws Exception {
		int nuisancesCount = 0;
		int levelsCount = 0;
		Vector<LatinFactorInfo> nuisances = null;
		NodeList nl = pluginElement.getElementsByTagName("design");
		if (nl != null && nl.getLength() > 0) {
			final Element designElem = (Element) nl.item(0);
			levelsCount = Integer.parseInt(designElem.getAttribute("levels"));
			nuisancesCount = Integer.parseInt(designElem.getAttribute("nuisances"));
			nuisances = new Vector<LatinFactorInfo>(nuisancesCount);
			
			nl = null;
			nl = designElem.getElementsByTagName("LatinFactorInfo");
			if (nl != null && nl.getLength() > 0) {
				for (int i = 0;i < nl.getLength();i++) {
	                final Element lfiElem = (Element) nl.item(i);
	                if (lfiElem.getAttribute("nuisance").compareToIgnoreCase("true") == 0) {
	                	final LatinFactorInfo lfi = new LatinFactorInfo(lfiElem.getAttribute("name"),lfiElem.getAttribute("type"),null);
	                	lfi.load(lfiElem);
	                	nuisances.add(lfi);
	                }
                }
				//we have all the nuisance factors in 'nuisances'
			} else 
				throw new IllegalArgumentException("The plugin XML element is corrupt.");
		} else
			throw new IllegalArgumentException("The plugin XML element is corrupt.");
		
		Columns resultColumns = runs.get(0).getOutputColumns();
		final int resultSize = resultColumns.size();
		responseFnModels = new Vector<Vector<LHCFunctionModel>>(nuisancesCount);
		for (int i = 0;i < nuisancesCount;i++) {
	        responseFnModels.add(new Vector<LHCFunctionModel>(resultSize));
	        for (int j = 0;j < resultSize;j++) {
	            responseFnModels.get(i).add(new LHCFunctionModel(nuisances.get(i).getName(),resultColumns.get(j).getName(),levelsCount));
	            responseFnModels.get(i).get(j).setLevels(nuisances.get(i).getLevels());
            }
        }
		
		for (int i = 0;i < runs.size();i++) {
			//the result has the output columns, let's get them:
			resultColumns = runs.get(i).getOutputColumns();
			for (int j = 0; j < resultColumns.size(); j++) {
//				Parameter resultColumn = resultColumns.get(j);
				//all results have parameter combinations (input parameters):
				final ParameterComb paramComb = runs.get(i).getParameterComb();
				for (int k = 0;k < nuisances.size();k++) {
					final String name = nuisances.get(k).getName();
					int kk = 0;
					while(kk < paramComb.getNames().size() && paramComb.getNames().get(kk).getName().compareTo(name) != 0)
						kk++;
					if (kk < paramComb.getNames().size()) {
						final Number input = LatinFactorInfo.getNumberValue(paramComb.getValues().get(kk).toString(),nuisances.get(k).getType());
						double response = (LatinFactorInfo.getNumberValue(runs.get(i).getAllRows().iterator().next().get(j).toString(),"double")).doubleValue();
						responseFnModels.get(k).get(j).addResponseValue(input,response);
					}
				}
            }
        }
		
		List<String> ret = new ArrayList<String>();

		//let's create a new model:
		final String newModelName = runs.get(0).getModel().getName();
		final String version = runs.get(0).getModel().getVersion() + "_Latin_Hypercube_Results" + Util.getTimeStampFromXMLFileName(isPluginXMLFileName);
		Model model = db.findModel(newModelName,version);
		if (model == null)
			model = new Model(Model.NONEXISTENT_MODELID,newModelName,version);
		
		ret.add(version);
		
		//generate batch number
		final int b = db.getNewBatch(model);

		final Columns cols = new Columns();
		intelliSweepResultColumns = new Columns();
		for (int i = 0;i < responseFnModels.size();i++) {
			intelliSweepResultColumns.append(responseFnModels.get(i).get(0).getNuisanceName(),ColumnType.DOUBLE);
	        for (int j = 0;j < responseFnModels.get(i).size();j++) 
	        	intelliSweepResultColumns.append(responseFnModels.get(i).get(j).getResponseName() + " for " + 
	        									 responseFnModels.get(i).get(0).getNuisanceName(),ColumnType.DOUBLE);
        }
		
		for (int i = 0;i < responseFnModels.get(0).get(0).getLevels().size();i++) {
			final ResultInMem toAdd = new ResultInMem();
			final Result.Row row = new Result.Row(intelliSweepResultColumns, 0); // 0 is the tick#
			final ParameterComb pc = new ParameterComb(new GeneralRow(cols));
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
        }
	    return ret;
	}
}
