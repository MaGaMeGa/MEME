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

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jscience.mathematics.function.Constant;
import org.jscience.mathematics.function.Polynomial;
import org.jscience.mathematics.number.Float64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
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

/**
 * @author Ferschl
 * 
 */
public class Static_Factorial_Processer implements IIntelliResultProcesserPlugin, IIntelliResultProcesserPluginV2 {

	private Columns intelliSweepResultColumns;
	private boolean normalizedParameterSpace;
	private Vector<Double> centerpointModelValues = new Vector<Double>();
	private Vector<Double> centerpointSampleValues = new Vector<Double>();
	private int centerpointSampleCount = 0;
	private int modelSampleCount = 0;
	private Columns intelliSweepCenterpointColumns;
	// private SampleQueryModel sampleModel = null;
	// private String parametersXMLName = "factors";
	// private String parameterXMLName = "factor";
	private boolean processCenterpoints = true;
	private JPanel genericProcesserPanel = null;
	private JRadioButton effectsRadio = null;
	private JRadioButton coefficientsRadio = null;
	private JCheckBox centerPointCheckbox = null;
	private Element pluginElement = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ai.aitia.meme.pluginmanager.IIntelliResultProcesserPlugin#processResultFiles
	 * (ai.aitia.meme.database.IResultsDbMinimal, java.util.List,
	 * org.w3c.dom.Element)
	 */
	public List<ResultInMem> processResultFiles(IResultsDbMinimal db, List<ResultInMem> runs, Element pluginElement) {
		ResultInMem first = runs.get(0);
		Columns resultColumns = first.getOutputColumns();
		Vector<Factor> factors = new Vector<Factor>();
		Vector<FactorCombination> combinations = new Vector<FactorCombination>();
		NodeList nl = pluginElement.getElementsByTagName("factors");
		if (nl != null && nl.getLength() > 0) {
			Element factorsElement = (Element) nl.item(0);
			nl = null;
			nl = factorsElement.getElementsByTagName("factor");
			if (nl != null && nl.getLength() > 0) {
				for (int i = 0; i < nl.getLength(); i++) {
					Element factorElement = (Element) nl.item(i);
					factors.add(new Factor(factorElement.getAttribute("name"), factorElement.getAttribute("high"),
							factorElement.getAttribute("low")));
				}
			} else {
				throw new IllegalArgumentException("The plugin XML element is corrupt.");
			}
		} else {
			throw new IllegalArgumentException("The plugin XML element is corrupt.");
		}
		nl = null;
		nl = pluginElement.getElementsByTagName("normalize");
		normalizedParameterSpace = false;
		if (nl != null && nl.getLength() > 0) {
			normalizedParameterSpace = true;
		}
		// Generate the combinations to analyze:
		// first, the one-factor 'combinations':
		for (Factor fact : factors) {
			Vector<Factor> toAdd = new Vector<Factor>();
			toAdd.add(fact);
			combinations.add(new FactorCombination(toAdd));
		}
		// then the two-factor combinations:
		if (factors.size() > 1) {
			for (int i = 0; i < factors.size() - 1; i++) {
				for (int j = i + 1; j < factors.size(); j++) {
					Vector<Factor> toAdd = new Vector<Factor>();
					toAdd.add(factors.get(i));
					toAdd.add(factors.get(j));
					combinations.add(new FactorCombination(toAdd));
				}
			}
		}
		// the three-or-greater factor combinations are usually not interesting,
		// so we skip them
		// �tlagoljuk az �sszes run-t (ticket is), az �sszes parakombi szerint,
		// minden result oszlopra, k�l�n a plus-minus csoportokat, majd abb�l
		// m�r lehet effectet sz�molni
		int resCnt = 0;
		int allCnt = 0;
		centerpointSampleValues = null;
		centerpointModelValues = null;
		for (ResultInMem result : runs) {
			resCnt++;
			if (combinations.get(0).isCenterPoint(result.getParameterComb())) {
				Vector<Double> cpTempSample = getResultColumns(result);
				centerpointSampleCount++;
				if (centerpointSampleValues == null) centerpointSampleValues = cpTempSample;
				else {
					for (int i = 0; i < cpTempSample.size(); i++) {
						double temp = centerpointSampleValues.get(i);
						temp += cpTempSample.get(i);
						centerpointSampleValues.set(i, temp);
					}
				}
			} else {
				modelSampleCount++;
				Vector<Double> modelTempSample = getResultColumns(result);
				if (centerpointModelValues == null) centerpointModelValues = modelTempSample;
				else {
					for (int i = 0; i < modelTempSample.size(); i++) {
						double temp = centerpointModelValues.get(i);
						temp += modelTempSample.get(i);
						centerpointModelValues.set(i, temp);
					}
				}
				for (FactorCombination combination : combinations) {
					combination.addNewResult(result);
					allCnt++;
				}
			}
		}
		if (centerpointSampleCount > 0 && processCenterpoints) {
			for (int i = 0; i < centerpointSampleValues.size(); i++) {
				double temp = centerpointSampleValues.get(i);
				centerpointSampleValues.set(i, temp / centerpointSampleCount);
			}
			for (int i = 0; i < centerpointModelValues.size(); i++) {
				double temp = centerpointModelValues.get(i);
				centerpointModelValues.set(i, temp / modelSampleCount);
			}
		}
		Vector<Polynomial<Float64>> effectPolys = new Vector<Polynomial<Float64>>(resultColumns.size());

		Vector<Vector<FactorEffect>> effectData = new Vector<Vector<FactorEffect>>(resultColumns.size());
		for (int i = 0; i < resultColumns.size(); i++) {
			effectData.add(new Vector<FactorEffect>(combinations.size()));
			effectPolys.add(Constant.valueOf(Float64.ZERO));
			for (int j = 0; j < combinations.size(); j++) {
				effectData.get(i).add(new FactorEffect("a", new AbsoluteDouble(0)));
			}
		}
		for (int i = 0; i < combinations.size(); i++) {
			Vector<Double> effectLine = combinations.get(i).getEffects();
			for (int j = 0; j < effectLine.size(); j++) {
				effectData.get(j).set(i,
						new FactorEffect(combinations.get(i).getName(), new AbsoluteDouble(effectLine.get(j))));
				effectPolys.set(j, effectPolys.get(j).plus(
						combinations.get(i).getCombinationRangeCorrector().times(Float64.valueOf(effectLine.get(j)))));
			}
		}

		for (int i = 0; i < combinations.size(); i++) {
			for (int j = 0; j < resultColumns.size(); j++) {
				Float64 coef = effectPolys.get(j).getCoefficient(combinations.get(i).getCombinationTerm());
				double coefDouble = 0;
				if (coef != null) coefDouble = coef.doubleValue() / 2;
				if (!normalizedParameterSpace) {
					effectData.get(j)
							.set(i, new FactorEffect(combinations.get(i).getName(), new AbsoluteDouble(coefDouble)));
				} else {
					// coefDouble *= combinations.get(i).getIntervalLength();
				}
			}
		}

		// ---------------------------------------------------
		// extra code for linear interaction model fitting
		// ---------------------------------------------------
		// int inputsCount = 0;
		// nl = null;
		// Vector<String> inputStrings = null;
		// Vector<Integer> inputIndices = null;
		// nl = pluginElement.getElementsByTagName(parametersXMLName);
		// if(nl != null && nl.getLength() > 0){
		// Element designElem = (Element) nl.item(0);
		// inputStrings = new Vector<String>();
		// inputIndices = new Vector<Integer>();
		// nl = null;
		// nl = designElem.getElementsByTagName(parameterXMLName);
		// if(nl != null && nl.getLength() > 0){
		// for (int i = 0; i < nl.getLength(); i++) {
		// Element lfiElem = (Element) nl.item(i);
		// inputsCount++;
		// inputStrings.add(lfiElem.getAttribute("name"));
		// inputIndices.add(inputsCount-1); //the index of the last added
		// element
		// }
		// } else{
		// throw new
		// IllegalArgumentException("The plugin XML element is corrupt.");
		// }
		// } else{
		// throw new
		// IllegalArgumentException("The plugin XML element is corrupt.");
		// }
		// Columns resultColumnsPoly = runs.get(0).getOutputColumns();
		// Vector<String> resultStrings = new Vector<String>();
		// Vector<Integer> resultIndices = new Vector<Integer>();
		// for (int i = 0; i < resultColumnsPoly.size(); i++) {
		// if(Number.class.isAssignableFrom(resultColumnsPoly.get(i).getDatatype().getJavaClass())){
		// resultStrings.add(resultColumnsPoly.get(i).getName());
		// resultIndices.add(i);
		// }
		// }
		// sampleModel = new SampleQueryModel(inputStrings, resultStrings);
		// int resultSize = resultColumnsPoly.size();
		// for (int i = 0; i < runs.size(); i++) {
		// //the result has the output columns, let's get them:
		// resultColumnsPoly = runs.get(i).getOutputColumns();
		// Vector<Double> resultDoubles = new Vector<Double>();
		// Vector<Double> inputDoubles = null;
		// for (int j = 0; j < resultColumnsPoly.size(); j++) {
		// double response =
		// (LatinFactorInfo.getNumberValue(runs.get(i).getAllRows().iterator().next().get(j).toString(),
		// null)).doubleValue();
		// if(resultIndices.contains(new Integer(j))){
		// resultDoubles.add(response);
		// }
		// //all results have parameter combinations (input parameters):
		// ParameterComb paramComb = runs.get(i).getParameterComb();
		// Vector<Double> inputDoublesTmp = null;
		// if(inputDoubles == null){
		// inputDoublesTmp = new Vector<Double>();
		// }
		// for (int k = 0; k < inputsCount; k++) {
		// String name = inputStrings.get(k);
		// int l = 0;
		// while(paramComb.getNames().get(l).getName().compareTo(name) != 0 && l
		// < paramComb.getNames().size()) l++;
		// if(l < paramComb.getNames().size()){
		// Number input =
		// LatinFactorInfo.getNumberValue(paramComb.getValues().get(l).toString(),
		// null);
		// if (input == null) input = new Double(0.0);
		// if(inputDoubles == null && inputIndices.contains(new Integer(k))){
		// inputDoublesTmp.add(input.doubleValue());
		// }
		// }
		// }
		// if(inputDoubles == null){
		// inputDoubles = inputDoublesTmp;
		// }
		// }
		// //adding the sample to the SampleQueryModel:
		// sampleModel.addSample(inputDoubles, resultDoubles);
		// }
		// //
		// Vector<PolynomialRegressor> regressors = new
		// Vector<PolynomialRegressor>();
		// for (int i = 0; i < resultStrings.size(); i++) {
		// PolynomialRegressor regressor = new PolynomialRegressor(sampleModel,
		// resultStrings.get(i));
		// regressor.createLinearInteractionPolynomial(2);
		// regressors.add(regressor);
		// }
		//
		// effectData = new Vector<Vector<FactorEffect>>(regressors.size());
		// for (int i = 0; i < regressors.size(); i++) {
		// effectData.add(new
		// Vector<FactorEffect>(regressors.get(i).getTermsWithoutOne().size()));
		// for (int j = 0; j < regressors.get(i).getTermsWithoutOne().size();
		// j++) {
		// //effectData.get(i).add(new FactorEffect("",new AbsoluteDouble(0)));
		// }
		// }
		// for (int j = 0; j < regressors.size(); j++) {
		// for (int i = 0; i < regressors.get(j).getTermsWithoutOne().size();
		// i++) {
		// Float64 coef =
		// regressors.get(j).getRegressor().getCoefficient(regressors.get(j).getTermsWithoutOne().get(i));
		// double coefDouble = 0;
		// if(coef != null) coefDouble = coef.doubleValue();
		// // if (normalizedParameterSpace){
		// // coefDouble *= combinations.get(i).getIntervalLength();
		// // }
		// effectData.get(j).add(new
		// FactorEffect(regressors.get(j).getTermsWithoutOne().get(i).toString(),new
		// AbsoluteDouble(coefDouble)));
		// }
		// }

		// ---------------------------------------------------
		// extra code for linear interaction model fitting end
		// ---------------------------------------------------

		String newModelName = runs.get(0).getModel().getName();
		String versionBase = runs.get(0).getModel().getVersion() + "_Factorial_Results" + Util.getTimeStamp();
		ArrayList<ResultInMem> ret = new ArrayList<ResultInMem>();
		String version = versionBase;
		Model model = db.findModel(newModelName, version);
		if (model == null) model = new Model(Model.NONEXISTENT_MODELID, newModelName, version);
		// Generate batch number
		int b = db.getNewBatch(model);
		for (int i = 0; i < effectData.size(); i++) {
			// sort the effects in descending order
			Collections.sort(effectData.get(i), Collections.reverseOrder());
		}
		intelliSweepResultColumns = new Columns();
		Columns cols = new Columns();
		for (int k = 0; k < effectData.size(); k++) {
			intelliSweepResultColumns.append("Factor for " + resultColumns.get(k).getName(), ColumnType.STRING);
			intelliSweepResultColumns.append("Effect on " + resultColumns.get(k).getName(), ColumnType.DOUBLE);
		}
		for (int j = 0; j < effectData.get(0).size(); j++) {
			ResultInMem toAdd = new ResultInMem();
			toAdd.setModel(model);
			toAdd.setBatch(b);
			toAdd.setStartTime(first.getStartTime());
			toAdd.setEndTime(first.getEndTime());
			toAdd.setRun(j);

			ParameterComb pc = new ParameterComb(new GeneralRow(cols));
			Result.Row row = new Result.Row(intelliSweepResultColumns, 0);
			for (int i = 0; i < effectData.size(); i++) {
				// pc.getValues().set(i, effectData.get(i).get(j).factorName);
				// row.set(i, effectData.get(i).get(j).effect.signedValue);
				row.set(i * 2 + 0, effectData.get(i).get(j).factorName);
				row.set(i * 2 + 1, effectData.get(i).get(j).effect.signedValue);
			}
			toAdd.setParameterComb(pc);
			toAdd.add(row);
			ret.add(toAdd);
		}

		// centerpoint/curvature results:
		if (centerpointSampleCount > 0 && processCenterpoints) {
			newModelName = runs.get(0).getModel().getName();
			versionBase = runs.get(0).getModel().getVersion() + "_Factorial_Curvature_Results" + Util.getTimeStamp();
			version = versionBase;
			model = db.findModel(newModelName, version);
			if (model == null) model = new Model(Model.NONEXISTENT_MODELID, newModelName, version);
			// Generate batch number
			b = db.getNewBatch(model);
			intelliSweepCenterpointColumns = new Columns();
			cols = new Columns();
			intelliSweepCenterpointColumns.append("Output name", ColumnType.STRING);
			intelliSweepCenterpointColumns.append("Centerpoint value from samples", ColumnType.DOUBLE);
			intelliSweepCenterpointColumns.append("Centerpoint value from model", ColumnType.DOUBLE);
			intelliSweepCenterpointColumns.append("Difference [%]", ColumnType.DOUBLE);
			for (int j = 0; j < resultColumns.size(); j++) {
				ResultInMem toAdd = new ResultInMem();
				toAdd.setModel(model);
				toAdd.setBatch(b);
				toAdd.setStartTime(first.getStartTime());
				toAdd.setEndTime(first.getEndTime());
				toAdd.setRun(j);

				ParameterComb pc = new ParameterComb(new GeneralRow(cols));
				Result.Row row = new Result.Row(intelliSweepCenterpointColumns, 0);
				row.set(0, resultColumns.get(j).getName());
				row.set(1, centerpointSampleValues.get(j));
				row.set(2, centerpointModelValues.get(j));
				row.set(3, 100.0 * (centerpointModelValues.get(j) - centerpointSampleValues.get(j))
						/ centerpointSampleValues.get(j));
				toAdd.setParameterComb(pc);
				toAdd.add(row);
				ret.add(toAdd);
			}
		}

		return ret;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ai.aitia.meme.pluginmanager.IPlugin#getLocalizedName()
	 */
	public String getLocalizedName() {
		return "Factorial_Processer";
	}

	public Document createCharts(String viewName, String model, String version) throws ParserConfigurationException {
		Document chartXML = null;
		if (!version.contains("Curvature")) {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder chartBuilder = factory.newDocumentBuilder();
			chartXML = chartBuilder.newDocument();
			Element chartRoot = chartXML.createElement("chart");
			chartRoot.setAttribute("singleFlag", "false");
			chartXML.appendChild(chartRoot);
			Element datasourcesElem = chartXML.createElement("datasources");
			chartRoot.appendChild(datasourcesElem);

			for (int i = 0; i < intelliSweepResultColumns.size(); i++) {
				if (i % 2 == 0) {
					// Factors:
					Element factorsDSElem = chartXML.createElement("datasource");
					factorsDSElem.setAttribute("id", new Integer(i + 1).toString());
					factorsDSElem.setAttribute("type", "ai.aitia.chart.ds.IStringSeriesProducer");
					datasourcesElem.appendChild(factorsDSElem);
					Element versionElem1 = chartXML.createElement("property");
					versionElem1.setAttribute("key", "version");
					versionElem1.setTextContent("1.0.80826");
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
					idElem1.setTextContent("-1." + viewName + ";" + intelliSweepResultColumns.get(i).getName());
					factorsDSElem.appendChild(idElem1);
				} else {
					// Effect:
					Element effectsDSElem = chartXML.createElement("datasource");
					effectsDSElem.setAttribute("id", new Integer(i + 1).toString());
					effectsDSElem.setAttribute("type", "ai.aitia.visu.ds.ISeriesProducer");
					datasourcesElem.appendChild(effectsDSElem);
					Element versionElem2 = chartXML.createElement("property");
					versionElem2.setAttribute("key", "version");
					versionElem2.setTextContent("1.0.80826");
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
					idElem2.setTextContent("-1." + viewName + ";" + intelliSweepResultColumns.get(i).getName());
					effectsDSElem.appendChild(idElem2);
				}
			}
			for (int i = 0; i < intelliSweepResultColumns.size(); i += 2) {
				// chartconfig:
				Element chartconfigElem = chartXML.createElement("chartconfig");
				chartconfigElem.setAttribute("fireInitialEvent", "true");
				chartconfigElem.setAttribute("type", "BarChart");
				chartRoot.appendChild(chartconfigElem);

				Element dsElem = chartXML.createElement("property");
				dsElem.setAttribute("key", "datasource");
				dsElem.setTextContent(new Integer(i + 1).toString() + "," + new Integer(i + 2).toString());
				chartconfigElem.appendChild(dsElem);

				Element envElem = chartXML.createElement("property");
				envElem.setAttribute("key", "environment appearance");
				envElem.setTextContent("basic");
				chartconfigElem.appendChild(envElem);

				Element colorElem = chartXML.createElement("property");
				colorElem.setAttribute("key", "color appearance");
				colorElem.setTextContent("colored");
				chartconfigElem.appendChild(colorElem);

				Element angleElem = chartXML.createElement("property");
				angleElem.setAttribute("key", "label angle");
				angleElem.setTextContent("0");
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
				titleElem.setTextContent(intelliSweepResultColumns.get(i + 1).getName() + " in the " + model + " model");
				chartconfigElem.appendChild(titleElem);

				Element subElem = chartXML.createElement("property");
				subElem.setAttribute("key", "subtitle");
				if (normalizedParameterSpace) subElem.setTextContent("Effects are normalized to the parameter intervals");
				chartconfigElem.appendChild(subElem);

				Element barElem = chartXML.createElement("property");
				barElem.setAttribute("key", "bar renderer");
				barElem.setTextContent("one bar per datarow");
				chartconfigElem.appendChild(barElem);
			}
		}
		return chartXML;
	}

//	public static Vector<Double> getResultColumns(ResultInMem result) {
	public static Vector<Double> getResultColumns(Result result) {
		Vector<Double> ret = new Vector<Double>();
		Iterable<Row> rows = result.getAllRows();
		Row lastRow = null;
		for (Iterator<Row> iter = rows.iterator(); iter.hasNext();) {
			Row row = iter.next();
			lastRow = row;
		}
		for (int i = 0; i < lastRow.size(); i++) {
			ret.add(FactorCombination.getDoubleFromObject(lastRow.get(i)));
		}
		return ret;
	}

	public static class AbsoluteDouble extends Number implements Comparable<AbsoluteDouble> {
		private static final long serialVersionUID = 7230403240811856328L;

		Double signedValue = null;

		public AbsoluteDouble(double d) {
			signedValue = new Double(d);
		}

		@Override
		public double doubleValue() {
			return signedValue.doubleValue();
		}

		@Override
		public float floatValue() {
			return signedValue.floatValue();
		}

		@Override
		public int intValue() {
			return signedValue.intValue();
		}

		@Override
		public long longValue() {
			return signedValue.longValue();
		}

		public int compareTo(AbsoluteDouble o) {
			Double absolute = new Double(Math.abs(signedValue));
			Double absoluteO = new Double(Math.abs(o.signedValue));
			return absolute.compareTo(absoluteO);
		}
	}

	public void doGenericProcessing() {
		processCenterpoints = (centerPointCheckbox == null ? false : centerPointCheckbox.isSelected());
		NodeList nl = null;
		nl = pluginElement.getElementsByTagName("normalize");
		if (nl != null && nl.getLength() > 0 && coefficientsRadio.isSelected()) {
			Node normNode = nl.item(0);
			normNode.getParentNode().removeChild(normNode);
		} else if ((nl == null || nl.getLength() == 0) && effectsRadio.isSelected()) {
			Element normElem = pluginElement.getOwnerDocument().createElement("normalize");
			pluginElement.appendChild(normElem);
		}
	}

	public JPanel getGenericProcessPanel(Element pluginElement) {
		this.pluginElement = pluginElement;
		genericProcesserPanel = new JPanel(new GridLayout(0, 1));
		genericProcesserPanel.setBorder(BorderFactory.createTitledBorder("Factorial method options"));
		JPanel radioButtonsPanel = new JPanel(new GridLayout(0, 1));
		radioButtonsPanel.setBorder(BorderFactory.createTitledBorder("Analysis options"));
		ButtonGroup radioGroup = new ButtonGroup();
		NodeList nl = null;
		nl = pluginElement.getElementsByTagName("normalize");
		boolean effectsSelected = (nl != null && nl.getLength() > 0);
		effectsRadio = new JRadioButton("Show effects as difference in output between low and high settings",
				effectsSelected);
		coefficientsRadio = new JRadioButton("Show the coefficients of the linear model built on data", !effectsSelected);
		radioGroup.add(effectsRadio);
		radioGroup.add(coefficientsRadio);
		radioButtonsPanel.add(effectsRadio);
		radioButtonsPanel.add(coefficientsRadio);
		nl = pluginElement.getElementsByTagName("centerpoint");
		if (nl != null && nl.getLength() > 0) {
			centerPointCheckbox = new JCheckBox("Curvature analysis", true);
			genericProcesserPanel.add(centerPointCheckbox);
		}
		genericProcesserPanel.add(radioButtonsPanel);
		return genericProcesserPanel;
	}

	public boolean isGenericProcessingSupported() { return true; }

	//====================================================================================================
	// implementing IIntelliResultProcesserPluginV2
	
	//----------------------------------------------------------------------------------------------------
	public List<String> processResultFiles(final IResultsDbMinimal db, final List<Result> runs, final Element pluginElement, 
										   final String isPluginXMLFileName) throws Exception {
		final Result first = runs.get(0);
		final Columns resultColumns = first.getOutputColumns();
		final Vector<Factor> factors = new Vector<Factor>();
		final Vector<FactorCombination> combinations = new Vector<FactorCombination>();
		
		NodeList nl = pluginElement.getElementsByTagName("factors");
		if (nl != null && nl.getLength() > 0) {
			final Element factorsElement = (Element) nl.item(0);
			nl = null;
			nl = factorsElement.getElementsByTagName("factor");
			if (nl != null && nl.getLength() > 0) {
				for (int i = 0;i < nl.getLength();i++) {
					final Element factorElement = (Element) nl.item(i);
					factors.add(new Factor(factorElement.getAttribute("name"),factorElement.getAttribute("high"),factorElement.getAttribute("low")));
				}
			} else 
				throw new IllegalArgumentException("The plugin XML element is corrupt.");
		} else 
			throw new IllegalArgumentException("The plugin XML element is corrupt.");

		nl = null;
		nl = pluginElement.getElementsByTagName("normalize");
		normalizedParameterSpace = false;
		if (nl != null && nl.getLength() > 0) 
			normalizedParameterSpace = true;
		
		// Generate the combinations to analyze:
		// first, the one-factor 'combinations':
		for (final Factor fact : factors) {
			Vector<Factor> toAdd = new Vector<Factor>();
			toAdd.add(fact);
			combinations.add(new FactorCombination(toAdd));
		}
		
		// then the two-factor combinations:
		if (factors.size() > 1) {
			for (int i = 0;i < factors.size() - 1;i++) {
				for (int j = i + 1;j < factors.size();j++) {
					Vector<Factor> toAdd = new Vector<Factor>();
					toAdd.add(factors.get(i));
					toAdd.add(factors.get(j));
					combinations.add(new FactorCombination(toAdd));
				}
			}
		}

		// the three-or-greater factor combinations are usually not interesting,
		// so we skip them
		// �tlagoljuk az �sszes run-t (ticket is), az �sszes parakombi szerint,
		// minden result oszlopra, k�l�n a plus-minus csoportokat, majd abb�l
		// m�r lehet effectet sz�molni
		int resCnt = 0;
		int allCnt = 0;
		centerpointSampleValues = null;
		centerpointModelValues = null;
		for (final Result result : runs) {
			resCnt++;
			if (combinations.get(0).isCenterPoint(result.getParameterComb())) {
				final Vector<Double> cpTempSample = getResultColumns(result);
				centerpointSampleCount++;
				if (centerpointSampleValues == null)
					centerpointSampleValues = cpTempSample;
				else {
					for (int i = 0;i < cpTempSample.size();i++) {
						double temp = centerpointSampleValues.get(i);
						temp += cpTempSample.get(i);
						centerpointSampleValues.set(i,temp);
					}
				}
			} else {
				modelSampleCount++;
				final Vector<Double> modelTempSample = getResultColumns(result);
				if (centerpointModelValues == null)
					centerpointModelValues = modelTempSample;
				else {
					for (int i = 0;i < modelTempSample.size();i++) {
						double temp = centerpointModelValues.get(i);
						temp += modelTempSample.get(i);
						centerpointModelValues.set(i, temp);
					}
				}
				for (final FactorCombination combination : combinations) {
					combination.addNewResult(result);
					allCnt++;
				}
			}
		}
		if (centerpointSampleCount > 0 && processCenterpoints) {
			for (int i = 0;i < centerpointSampleValues.size();i++) {
				final double temp = centerpointSampleValues.get(i);
				centerpointSampleValues.set(i,temp / centerpointSampleCount);
			}
			for (int i = 0;i < centerpointModelValues.size();i++) {
				final double temp = centerpointModelValues.get(i);
				centerpointModelValues.set(i, temp / modelSampleCount);
			}
		}
		
		final Vector<Polynomial<Float64>> effectPolys = new Vector<Polynomial<Float64>>(resultColumns.size());
		final Vector<Vector<FactorEffect>> effectData = new Vector<Vector<FactorEffect>>(resultColumns.size());
		for (int i = 0;i < resultColumns.size();i++) {
			effectData.add(new Vector<FactorEffect>(combinations.size()));
			effectPolys.add(Constant.valueOf(Float64.ZERO));
			for (int j = 0;j < combinations.size();j++) 
				effectData.get(i).add(new FactorEffect("a",new AbsoluteDouble(0)));
		}
		
		for (int i = 0;i < combinations.size();i++) {
			final Vector<Double> effectLine = combinations.get(i).getEffects();
			for (int j = 0;j < effectLine.size();j++) {
				effectData.get(j).set(i,new FactorEffect(combinations.get(i).getName(),new AbsoluteDouble(effectLine.get(j))));
				effectPolys.set(j,effectPolys.get(j).plus(combinations.get(i).getCombinationRangeCorrector().times(Float64.valueOf(effectLine.get(j)))));
			}
		}

		for (int i = 0;i < combinations.size();i++) {
			for (int j = 0;j < resultColumns.size();j++) {
				final Float64 coef = effectPolys.get(j).getCoefficient(combinations.get(i).getCombinationTerm());
				double coefDouble = 0;
				if (coef != null)
					coefDouble = coef.doubleValue() / 2;
				if (!normalizedParameterSpace) 
					effectData.get(j).set(i,new FactorEffect(combinations.get(i).getName(),new AbsoluteDouble(coefDouble)));
				else {
					// coefDouble *= combinations.get(i).getIntervalLength();
				}
			}
		}

		String newModelName = runs.get(0).getModel().getName();
		String versionBase = runs.get(0).getModel().getVersion() + "_Factorial_Results" + Util.getTimeStampFromXMLFileName(isPluginXMLFileName);
		final List<String> ret = new ArrayList<String>();
		String version = versionBase;
		
		Model model = db.findModel(newModelName,version);
		if (model == null) 
			model = new Model(Model.NONEXISTENT_MODELID,newModelName,version);
		
		ret.add(version);
		
		// Generate batch number
		int b = db.getNewBatch(model);
		for (int i = 0;i < effectData.size();i++) {
			// sort the effects in descending order
			Collections.sort(effectData.get(i),Collections.reverseOrder());
		}
		
		intelliSweepResultColumns = new Columns();
		Columns cols = new Columns();
		for (int k = 0;k < effectData.size();k++) {
			intelliSweepResultColumns.append("Factor for " + resultColumns.get(k).getName(),ColumnType.STRING);
			intelliSweepResultColumns.append("Effect on " + resultColumns.get(k).getName(),ColumnType.DOUBLE);
		}
		for (int j = 0;j < effectData.get(0).size();j++) {
			final ResultInMem toAdd = new ResultInMem();
			toAdd.setModel(model);
			toAdd.setBatch(b);
			toAdd.setStartTime(first.getStartTime());
			toAdd.setEndTime(first.getEndTime());
			toAdd.setRun(j);

			final ParameterComb pc = new ParameterComb(new GeneralRow(cols));
			final Result.Row row = new Result.Row(intelliSweepResultColumns,0);
			for (int i = 0;i < effectData.size();i++) {
				row.set(i * 2 + 0,effectData.get(i).get(j).factorName);
				row.set(i * 2 + 1,effectData.get(i).get(j).effect.signedValue);
			}
			toAdd.setParameterComb(pc);
			toAdd.add(row);
			db.addResult(toAdd);
		}

		// centerpoint/curvature results:
		if (centerpointSampleCount > 0 && processCenterpoints) {
			newModelName = runs.get(0).getModel().getName();
			versionBase = runs.get(0).getModel().getVersion() + "_Factorial_Curvature_Results" + Util.getTimeStampFromXMLFileName(isPluginXMLFileName);
			version = versionBase;
			model = db.findModel(newModelName,version);
			if (model == null) 
				model = new Model(Model.NONEXISTENT_MODELID,newModelName,version);
			
			ret.add(version);
			
			// Generate batch number
			b = db.getNewBatch(model);
			intelliSweepCenterpointColumns = new Columns();
			cols = new Columns();
			intelliSweepCenterpointColumns.append("Output name",ColumnType.STRING);
			intelliSweepCenterpointColumns.append("Centerpoint value from samples",ColumnType.DOUBLE);
			intelliSweepCenterpointColumns.append("Centerpoint value from model",ColumnType.DOUBLE);
			intelliSweepCenterpointColumns.append("Difference [%]",ColumnType.DOUBLE);
			for (int j = 0;j < resultColumns.size();j++) {
				final ResultInMem toAdd = new ResultInMem();
				toAdd.setModel(model);
				toAdd.setBatch(b);
				toAdd.setStartTime(first.getStartTime());
				toAdd.setEndTime(first.getEndTime());
				toAdd.setRun(j);

				final ParameterComb pc = new ParameterComb(new GeneralRow(cols));
				final Result.Row row = new Result.Row(intelliSweepCenterpointColumns,0);
				row.set(0,resultColumns.get(j).getName());
				row.set(1,centerpointSampleValues.get(j));
				row.set(2,centerpointModelValues.get(j));
				row.set(3,100.0 * (centerpointModelValues.get(j) - centerpointSampleValues.get(j)) / centerpointSampleValues.get(j));
				toAdd.setParameterComb(pc);
				toAdd.add(row);
				db.addResult(toAdd);
			}
		}
		return ret;
	}
}
