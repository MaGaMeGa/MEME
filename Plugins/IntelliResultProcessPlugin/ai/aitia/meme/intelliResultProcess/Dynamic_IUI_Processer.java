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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

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
import ai.aitia.meme.database.Result.Row;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.pluginmanager.IIntelliResultProcesserPlugin;
import ai.aitia.meme.pluginmanager.IIntelliResultProcesserPluginV2;

public class Dynamic_IUI_Processer implements IIntelliResultProcesserPlugin, IIntelliResultProcesserPluginV2 {

	private Columns intelliSweepResultColumns;
	//private Vector<Vector<LHCFunctionModel>> responseFnModels;
	protected String parametersXMLName = "parameters";
	protected String parameterXMLName = "parameter";
	protected String variablesXMLName = "variables";
	protected String variableXMLName = "variable";
	protected String resultAdditiveName = "_IUI_Results";
	protected String variableName = null;
	protected String parameterName = null;

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
		
		Element factorsDSElem = chartXML.createElement("datasource");
		//int id = new Integer(i*(responseFnModels.get(0).size()+1));
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
		idElem1.setTextContent("-1."+viewName+";"+parameterName);
		factorsDSElem.appendChild(idElem1);
		
		Element effectsDSElem = chartXML.createElement("datasource");
		//int id = new Integer(i*(responseFnModels.get(i).size()+1)+j+1);
		effectsDSElem.setAttribute("id", "2");
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
		idElem2.setTextContent("-1."+viewName+";"+variableName);
		effectsDSElem.appendChild(idElem2);
		
		Element chartconfigElem = chartXML.createElement("chartconfig");
		chartconfigElem.setAttribute("fireInitialEvent", "true");
		//you define the type of the chart here:
		chartconfigElem.setAttribute("type", "ScatterPlot");
		chartRoot.appendChild(chartconfigElem);

		Element dsElem = chartXML.createElement("property");
		dsElem.setAttribute("key", "datasource");
		//define the datasources used for this chart in a proper order
		dsElem.setTextContent("1 2");
		chartconfigElem.appendChild(dsElem);

		Element xAxisElem = chartXML.createElement("property");
		xAxisElem.setAttribute("key", "x-axis");
		xAxisElem.setTextContent(parameterName);
		chartconfigElem.appendChild(xAxisElem);

		Element yAxisElem = chartXML.createElement("property");
		yAxisElem.setAttribute("key", "y-axis");
		yAxisElem.setTextContent(variableName);
		chartconfigElem.appendChild(yAxisElem);

		Element xScaleElem = chartXML.createElement("property");
		xScaleElem.setAttribute("key", "x-axis scale");
		xScaleElem.setTextContent("NORMAL");
		chartconfigElem.appendChild(xScaleElem);

		Element yScaleElem = chartXML.createElement("property");
		yScaleElem.setAttribute("key", "y-axis scale");
		yScaleElem.setTextContent("NORMAL");
		chartconfigElem.appendChild(yScaleElem);

		Element envElem = chartXML.createElement("property");
		envElem.setAttribute("key", "environment appearance");
		envElem.setTextContent("normal");
		chartconfigElem.appendChild(envElem);

		Element colorElem = chartXML.createElement("property");
		colorElem.setAttribute("key", "color appearance");
		colorElem.setTextContent("colored");
		chartconfigElem.appendChild(colorElem);

		//???????
//		Element angleElem = chartXML.createElement("property");
//		angleElem.setAttribute("key", "label angle");
		//this property controls the angle of the labels on the barchart
//		angleElem.setTextContent("90");
//		chartconfigElem.appendChild(angleElem);

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
		titleElem.setTextContent(parameterName + " - " + variableName + " function in " + model + " model");
		chartconfigElem.appendChild(titleElem);

		Element subElem = chartXML.createElement("property");
		subElem.setAttribute("key", "subtitle");
		//you can specify a subtitle as well:
//		subElem.setTextContent("of "+model+" model, version: "+version);
		subElem.setTextContent("");
		chartconfigElem.appendChild(subElem);

		File file = new File( "IUI_TMP.xml" );
        
		try{
			Transformer transformer = 
	                              TransformerFactory.newInstance().newTransformer();
			Source source = new DOMSource(chartXML);
			FileOutputStream os = new FileOutputStream(file);
			javax.xml.transform.Result result = new StreamResult(os);
			transformer.transform(source,result); 
		}catch( TransformerException e ){
			
		}catch( FileNotFoundException e2 ){
			
		}
		/*//These two for-iterations may be replaced by one...
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
*/
		return chartXML;
	}
	
	public static List<ResultInMem> filterResults(List<ResultInMem> runs, String filterColumnName, Object filterValue){
		ArrayList<ResultInMem> ret = new ArrayList<ResultInMem>();
		int filterColumnIdx = -1;
		if(runs.size()>0){
			for (int i = 0; i < runs.get(0).getParameterComb().getNames().size(); i++) {
				if(runs.get(0).getParameterComb().getNames().get(i).getName().equals(filterColumnName)){
					filterColumnIdx = i;
				}
            }
		}
		if(filterColumnIdx != -1){
			for (ResultInMem run : runs) {
				if(run.getParameterComb().getValues().get(filterColumnIdx).toString().equals(filterValue.toString())){
					ret.add(run);
				}
			}
		}
		return ret;
	}
	
	public static List<ResultInMem> filterResults(List<ResultInMem> runs, String[] filterColumnName, Object[] filterValue){
		List<ResultInMem> ret2 = null;
		if(filterColumnName.length == filterValue.length && filterColumnName.length > 0){
			int i = 1;
			ret2 = filterResults(runs, filterColumnName[0], filterValue[0]); 
			while(ret2.size() > 0 && i < filterValue.length){
				ret2 = filterResults(ret2, filterColumnName[i], filterValue[i]);
				i++;
			}
		}
		ArrayList<ResultInMem> ret = null;
		if(ret2 != null)
			ret = new ArrayList<ResultInMem>(ret2);
		else
			ret = new ArrayList<ResultInMem>();
		return ret;
	}

	public List<ResultInMem> processResultFiles(IResultsDbMinimal db, List<ResultInMem> runs, 
																		Element pluginElement) {
		@SuppressWarnings("unused")
		int inputsCount = 0;
		NodeList nl = null;
		//Vector<LatinFactorInfo> inputs = null;
		parameterName = null;
		nl = pluginElement.getElementsByTagName(parametersXMLName);
		if(nl != null && nl.getLength() > 0){
			Element designElem = (Element) nl.item(0);
			//inputs = new Vector<LatinFactorInfo>();
			nl = null;
			nl = designElem.getElementsByTagName(parameterXMLName);
			if(nl != null && nl.getLength() > 0){
				parameterName = ((Element) nl.item( 0 )).getAttribute( "name" );
			} else{
				throw new IllegalArgumentException("The plugin XML element is corrupt.");
			}
		} else{
			throw new IllegalArgumentException("The plugin XML element is corrupt.");
		}
		
		variableName = null;
		nl = pluginElement.getElementsByTagName(variablesXMLName);
		if(nl != null && nl.getLength() > 0){
			Element designElem = (Element) nl.item(0);
			nl = null;
			nl = designElem.getElementsByTagName(variableXMLName);
			if(nl != null && nl.getLength() > 0){
				variableName = ((Element) nl.item( 0 )).getAttribute( "name" );
			} else{
				throw new IllegalArgumentException("The plugin XML element is corrupt.");
			}
		} else{
			throw new IllegalArgumentException("The plugin XML element is corrupt.");
		}
		

		//double response = (LatinFactorInfo.getNumberValue(runs.get(i).getAllRows().iterator().next().get(j).toString(), "double")).doubleValue();

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

		Columns cols = new Columns();
		intelliSweepResultColumns = new Columns();
		intelliSweepResultColumns.append(parameterName, ColumnType.DOUBLE);
		intelliSweepResultColumns.append(variableName, ColumnType.DOUBLE);
		
		for (int i = 0; i < runs.size(); i++) {
			ResultInMem toAdd = new ResultInMem();
			Result.Row row = new Result.Row(intelliSweepResultColumns, 0); //0 is the tick#
			ParameterComb pc = new ParameterComb(new GeneralRow(cols));
			toAdd.setModel(model);
			toAdd.setBatch(b); 
			toAdd.setStartTime(runs.get(0).getStartTime());
			toAdd.setEndTime(runs.get(0).getEndTime());
			toAdd.setRun(i);//the run#
			Row actRow = runs.get(i).getAllRows().iterator().next();
			
			//look up where is the variable
			int variableIndex = getColumnIndex( actRow.getColumns(), variableName );
			Double variableVal = null;
			if( variableIndex > -1 ){
				variableVal =
					Double.valueOf( actRow.get( variableIndex ).toString() );
			}
			ParameterComb actParamComb = runs.get( i ).getParameterComb();
			
			//let's find out, where is the parameter value (user might added it to output values
			//on the import dialog)
			int parameterIndex = getColumnIndex(actParamComb.getNames(), parameterName );
			Double parameterVal = null;
			if( parameterIndex > -1  ){
				parameterVal = 
					Double.valueOf( actParamComb.getValues().get( parameterIndex ).toString() );
			}else{
				parameterIndex = getColumnIndex( row.getColumns(), parameterName );
				parameterVal = 
					Double.valueOf( actRow.get( parameterIndex ).toString() );
			}
			
			row.set(0, parameterVal);
			row.set(1, variableVal);
			toAdd.setParameterComb(pc);
	        toAdd.add(row);
	        ret.add(toAdd);
        }
	    return ret;
	}
	
	public int getColumnIndex( Columns cols, String colName ){
		if( cols == null ) return -1;
		
		for( int i = 0; i < cols.size(); ++i ){
			if( cols.get( i ).getName().equals( colName ) )
				return i;
		}
		
		return -1;
	}

	public String getLocalizedName() {
		//it is important for the plugin to have the name like this:
		//"<Exact name of the Parametersweep plugin>_Processer"
		//e.g. the "Factorial" plugin's processer plugin is called
		//"Factorial_Processer"
		return "Iterative Uniform Interpolation_Processer";
	}

	public void doGenericProcessing() {
	}

	public JPanel getGenericProcessPanel(Element pluginElement) {
		return null;
	}

	public boolean isGenericProcessingSupported() {
		return false;
	}

	//----------------------------------------------------------------------------------------------------
	public List<String> processResultFiles(final IResultsDbMinimal db, final List<Result> runs, final Element pluginElement,
										   final String isPluginXMLFileName) throws Exception {
		NodeList nl = null;
		parameterName = null;
		nl = pluginElement.getElementsByTagName(parametersXMLName);
		if (nl != null && nl.getLength() > 0) {
			final Element designElem = (Element) nl.item(0);
			nl = null;
			nl = designElem.getElementsByTagName(parameterXMLName);
			if (nl != null && nl.getLength() > 0) 
				parameterName = ((Element)nl.item(0)).getAttribute("name");
			else
				throw new IllegalArgumentException("The plugin XML element is corrupt.");
		} else
			throw new IllegalArgumentException("The plugin XML element is corrupt.");
		
		variableName = null;
		nl = pluginElement.getElementsByTagName(variablesXMLName);
		if (nl != null && nl.getLength() > 0) {
			final Element designElem = (Element) nl.item(0);
			nl = null;
			nl = designElem.getElementsByTagName(variableXMLName);
			if (nl != null && nl.getLength() > 0)
				variableName = ((Element)nl.item(0)).getAttribute("name");
			else 
				throw new IllegalArgumentException("The plugin XML element is corrupt.");
		} else
			throw new IllegalArgumentException("The plugin XML element is corrupt.");

		final List<String> ret = new ArrayList<String>();
		
		//let's create a new model:
		final String newModelName = runs.get(0).getModel().getName();
		final String timeStamp = Util.getTimeStampFromXMLFileName(isPluginXMLFileName);
		final String version = runs.get(0).getModel().getVersion() + resultAdditiveName + timeStamp;
		Model model = db.findModel(newModelName,version);
		if (model == null)
			model = new Model(Model.NONEXISTENT_MODELID,newModelName,version);

		//generate batch number
		final int b = db.getNewBatch(model);

		ret.add(version);
		final Columns cols = new Columns();
		intelliSweepResultColumns = new Columns();
		intelliSweepResultColumns.append(parameterName,ColumnType.DOUBLE);
		intelliSweepResultColumns.append(variableName,ColumnType.DOUBLE);
		
		for (int i = 0;i < runs.size();i++) {
			final ResultInMem toAdd = new ResultInMem();
			final Result.Row row = new Result.Row(intelliSweepResultColumns,0); // 0 is the tick#
			final ParameterComb pc = new ParameterComb(new GeneralRow(cols));
			toAdd.setModel(model);
			toAdd.setBatch(b); 
			toAdd.setStartTime(runs.get(0).getStartTime());
			toAdd.setEndTime(runs.get(0).getEndTime());
			toAdd.setRun(i); // the run#
			final Row actRow = runs.get(i).getAllRows().iterator().next();
			
			//look up where is the variable
			final int variableIndex = getColumnIndex(actRow.getColumns(),variableName);
			Double variableVal = null;
			if (variableIndex > -1) 
				variableVal = Double.valueOf(actRow.get(variableIndex).toString());
			final ParameterComb actParamComb = runs.get(i).getParameterComb();
			
			//let's find out, where is the parameter value (user might added it to output values on the import dialog)
			int parameterIndex = getColumnIndex(actParamComb.getNames(),parameterName);
			Double parameterVal = null;
			if (parameterIndex > -1)
				parameterVal = Double.valueOf(actParamComb.getValues().get(parameterIndex).toString());
			else {
				parameterIndex = getColumnIndex(row.getColumns(),parameterName);
				parameterVal = Double.valueOf(actRow.get(parameterIndex).toString());
			}
			
			row.set(0,parameterVal);
			row.set(1,variableVal);
			toAdd.setParameterComb(pc);
	        toAdd.add(row);
	        db.addResult(toAdd);
        }
	    return ret;
	}
}
