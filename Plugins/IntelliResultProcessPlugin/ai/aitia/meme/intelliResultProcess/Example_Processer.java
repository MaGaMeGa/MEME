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

import javax.swing.JPanel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.database.IResultsDbMinimal;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ParameterComb;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.ResultInMem;
import ai.aitia.meme.pluginmanager.IIntelliResultProcesserPlugin;

/**
 * This is an example implementation of the IIntelliResultProcesserPlugin
 * interface. 
 * 
 * @author Ferschl
 *
 */
public class Example_Processer implements IIntelliResultProcesserPlugin {

	public String getLocalizedName() {
		//it is important for the plugin to have the name like this:
		//"<Exact name of the Parametersweep plugin>_Processer"
		//e.g. the "Factorial" plugin's processer plugin is called
		//"Factorial_Processer"
		return "The localized name of the ParamSweep plugin_Processer";
	}

	public List<ResultInMem> processResultFiles(IResultsDbMinimal db,
	        List<ResultInMem> runs, Element pluginElement) {
		//the results ar in the runs variable: let's see the first one:
		ResultInMem first = runs.get(0);
		//the result the output columns, let's get them:
		@SuppressWarnings("unused")
		Columns resultColumns = first.getOutputColumns();
		//all results have parameter combinations (input parameters):
		@SuppressWarnings("unused")
		ParameterComb paramComb = first.getParameterComb();
		//let's create a new model by naming it and it's version
		String newModelName = runs.get(0).getModel().getName()+"_IntelliSweep_SomeGoodName";
		String version = runs.get(0).getModel().getVersion()+"This is not necessary, but a good way to make multiple results b";
		//the List of ResultInMems that the method returns:
		ArrayList<ResultInMem> ret = new ArrayList<ResultInMem>();
		//generate the new model in the database:
        Model model = db.findModel(newModelName, version);
        if (model == null)
        	model = new Model(Model.NONEXISTENT_MODELID, newModelName, version);
        //generate batch number
        int b = db.getNewBatch(model);
        //generate a result and put it into the returned list:
		ResultInMem toAdd = new ResultInMem();
		toAdd.setModel(model);
		toAdd.setBatch(b);
		toAdd.setStartTime(first.getStartTime());
		toAdd.setEndTime(first.getEndTime());
		toAdd.setRun(0);//the run#
		Columns cols = new Columns();
		cols.append("Input1", ColumnType.STRING); //you should append the intended input columns like this
		Columns out = new Columns();
		out.append("Output1", ColumnType.DOUBLE);//you should append the intended output columns like this

		ParameterComb pc = new ParameterComb(new GeneralRow(cols));
		//the value of the input column:
		pc.getValues().set(0, "Input1's value");
		toAdd.setParameterComb(pc);
		//the value of the output column in a result row:
		Result.Row row = new Result.Row(out, 0); //0 is the tick#
		row.set(0, 111.123);
		toAdd.add(row);
		ret.add(toAdd);

	    return ret;
	}

	public Document createCharts(String viewName, String model, String version)
	throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder chartBuilder = factory.newDocumentBuilder();
		Document chartXML = chartBuilder.newDocument();
		Element chartRoot = chartXML.createElement("chart");
		chartRoot.setAttribute("singleFlag", "false");
		chartXML.appendChild(chartRoot);
		Element datasourcesElem = chartXML.createElement("datasources");
		chartRoot.appendChild(datasourcesElem);
		
		//Setting up a textual datasource for the chart:
		Element factorsDSElem = chartXML.createElement("datasource");
		factorsDSElem.setAttribute("id", "1");
		factorsDSElem.setAttribute("type", "ai.aitia.chart.ds.IStringSeriesProducer");
		datasourcesElem.appendChild(factorsDSElem);
		Element versionElem1 = chartXML.createElement("property");
		versionElem1.setAttribute("key", "version");
		versionElem1.setTextContent("1.0.80116");
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
		idElem1.setTextContent("-1."+viewName+";Input1");
		factorsDSElem.appendChild(idElem1);
		
		//Setting up a double datasource:
		Element effectsDSElem = chartXML.createElement("datasource");
		effectsDSElem.setAttribute("id", "2");
		effectsDSElem.setAttribute("type", "ai.aitia.visu.ds.ISeriesProducer");
		datasourcesElem.appendChild(effectsDSElem);
		Element versionElem2 = chartXML.createElement("property");
		versionElem2.setAttribute("key", "version");
		versionElem2.setTextContent("1.0.80116");
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
		idElem2.setTextContent("-1."+viewName+";Output1");
		effectsDSElem.appendChild(idElem2);
		
		//chartconfig of a barchart (you can use more of this, even different charts):
		Element chartconfigElem = chartXML.createElement("chartconfig");
		chartconfigElem.setAttribute("fireInitialEvent", "true");
		//you define the type of the chart here:
		chartconfigElem.setAttribute("type", "BarChart");
		chartRoot.appendChild(chartconfigElem);
		
		Element dsElem = chartXML.createElement("property");
		dsElem.setAttribute("key", "datasource");
		//define the datasources used for this chart in a proper order
		dsElem.setTextContent("1,2");
		chartconfigElem.appendChild(dsElem);

		Element envElem = chartXML.createElement("property");
		envElem.setAttribute("key", "environment appearance");
		envElem.setTextContent("normal");
		chartconfigElem.appendChild(envElem);

		Element colorElem = chartXML.createElement("property");
		colorElem.setAttribute("key", "color appearance");
		colorElem.setTextContent("colored");
		chartconfigElem.appendChild(colorElem);

		Element angleElem = chartXML.createElement("property");
		angleElem.setAttribute("key", "label angle");
		//this property controls the angle of the labels on the barchart
		angleElem.setTextContent("90");
		chartconfigElem.appendChild(angleElem);

		Element customElem = chartXML.createElement("property");
		customElem.setAttribute("key", "custom appearance");
		chartconfigElem.appendChild(customElem);

		Element legendElem = chartXML.createElement("property");
		legendElem.setAttribute("key", "show legend");
		legendElem.setTextContent("true");
		chartconfigElem.appendChild(legendElem);

		Element titleElem = chartXML.createElement("property");
		titleElem.setAttribute("key", "title");
		//this is the title of the chart
		titleElem.setTextContent("The title of " + model + " model's something");
		chartconfigElem.appendChild(titleElem);

		Element subElem = chartXML.createElement("property");
		subElem.setAttribute("key", "subtitle");
		//you can specify a subtitle as well:
		subElem.setTextContent("This is the subtitle");
		chartconfigElem.appendChild(subElem);

		Element barElem = chartXML.createElement("property");
		barElem.setAttribute("key", "bar renderer");
		barElem.setTextContent("one bar per datarow");
		chartconfigElem.appendChild(barElem);

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


}
