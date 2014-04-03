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
package ai.aitia.meme.processing.variation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.database.IResultsDbMinimal;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ParameterComb;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.ResultInMem;
import ai.aitia.meme.database.Result.Row;
import ai.aitia.meme.paramsweep.generator.RngSeedManipulatorModel;
import ai.aitia.meme.paramsweep.gui.info.ParameterRandomInfo;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.processing.IIntelliGenericProcesser;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

public class NaturalVariationV2 implements ActionListener, IIntelliGenericProcesser {

	//====================================================================================================
	// members
		
	private static final String SELECT_NONE = "SELECT_NONE";
	private static final String SELECT_ALL = "SELECT_ALL";
	
	protected String[] observed = null;
	protected Vector<Object[]> uniqueSamples = null;
	protected int[] observedIndices = null;
	protected String[] rngSeedNames = null;
	protected Vector<Double[]> uniqueSampleMeans = null;
	protected Vector<Double[]> uniqueSampleStdDevs = null;
	protected Vector<Double[]> uniqueSampleCoefficientOfVariations = null;
	protected Vector<Double[]> seedStdDevs = null;
	protected Vector<Double[]> seedCoefficientOfVariations = null;
	protected int[] seedValuesSize = null;
	private Result first = null; 
	private boolean needRandomSeedStatistics = false;
	private boolean needNaturalVariation = true;
	
	private JPanel genericProcesserPanel = null;
	private Element pluginElement = null;
	private Vector<Element> natVarElements = null;
	private Vector<JCheckBox> natVarCheckboxes = null;
	private JButton selectAllButton = null;
	private JButton selectNoneButton = null;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public boolean loadAndProcessData(List<Result> results, Element pluginElement) {
		needRandomSeedStatistics = false;
		needNaturalVariation = true;
		first = results.get(0);
		final Vector<String> observedV = new Vector<String>();
		uniqueSamples = new Vector<Object[]>();
		NodeList nl = null;
		Element rsmElem = null;
		nl = pluginElement.getElementsByTagName(RngSeedManipulatorModel.RSM_ELEMENT_NAME);
		if(nl != null && nl.getLength() > 0){
			rsmElem = (Element) nl.item(0);
		}
		if(rsmElem != null){
			nl = null;
			nl = rsmElem.getElementsByTagName(RngSeedManipulatorModel.NATURAL_VARIATION_ELEMENT);
			if(nl!=null && nl.getLength() > 0){
				for (int i = 0; i < nl.getLength(); i++) {
					final Element natVarElem = (Element) nl.item(i);
					if (natVarElem.hasAttribute("selected")) {
						if (natVarElem.getAttribute("selected").equalsIgnoreCase("true")) {
							observedV.add(natVarElem.getAttribute("name"));
						}
					} else {
						observedV.add(natVarElem.getAttribute("name"));
					}
				}
			} else needNaturalVariation  = false;
			if(observedV.size() == 0) needNaturalVariation  = false;
			rngSeedNames = new String[0];
			nl = null;
			nl = rsmElem.getElementsByTagName(ParameterRandomInfo.PRI_ELEM_NAME);
			if(nl!=null && nl.getLength() > 0){
				final Vector<String> rngSeedNamesV = new Vector<String>();
				for (int i = 0; i < nl.getLength(); i++) {
					final Element priVarElem = (Element) nl.item(i);
					if (priVarElem.getAttribute(ParameterRandomInfo.RND_TYPE_ATTR).equals(String.valueOf(ParameterRandomInfo.SWEEP_RND))) {
						rngSeedNamesV.add(priVarElem.getAttribute(ParameterRandomInfo.PARAMETER_NAME));
					}
				}
				rngSeedNames = new String[rngSeedNamesV.size()];
				rngSeedNames = rngSeedNamesV.toArray(rngSeedNames);
			}
			if (rngSeedNames.length > 0) {
				needRandomSeedStatistics = true;
			} else {
				needNaturalVariation  = false;
				needRandomSeedStatistics = false;
			}
			if (needNaturalVariation) {
				MEMEApp.LONG_OPERATION.setTaskName("Variation analysis");
				observed = new String[observedV.size()];
				for (int i = 0; i < observed.length; i++) {
					observed[i] = observedV.get(i);
				}
				observedIndices = new int[observed.length];
				for (int i = 0; i < observedIndices.length; i++) {
					int filterColumnIdx = -1;
					for (int j = 0; j < results.get(0).getParameterComb().getNames().size(); j++) {
						if(results.get(0).getParameterComb().getNames().get(j).getName().equals(observed[i])){
							filterColumnIdx = j;
						}
					}
					observedIndices[i] = filterColumnIdx;
				}
				for (int i = 0; i < results.size(); i++) {
					Result run = results.get(i);
					Object[] sample = getIndexSubset(run.getParameterComb().getValues(), observedIndices);
					if(isUniqueSample(uniqueSamples, sample)){
						uniqueSamples.add(sample);
					}
				}
				//for every unique sample: filter the results and
				//identify every unique input parameter combination within, compute
				//the variation and sample size for them, then compute a weighted
				//average of the variations
				Columns cols = results.get(0).getParameterComb().getNames();
				String[] parameters = new String[cols.size()-rngSeedNames.length];
				int paramIdx = 0;
				for (int j = 0; j < cols.size(); j++) {
					if(!Arrays.asList(rngSeedNames).contains(cols.get(j).getName())){
						parameters[paramIdx] = cols.get(j).getName();
						paramIdx++;
					}
				}
				uniqueSampleStdDevs = new Vector<Double[]>();
				uniqueSampleCoefficientOfVariations = new Vector<Double[]>();
				MEMEApp.LONG_OPERATION.progress(0, uniqueSamples.size());
				for (int i = 0; i < uniqueSamples.size(); i++) {
					MEMEApp.LONG_OPERATION.progress(i, -1);
					List<Result> filtered = filterResults(results, observed, uniqueSamples.get(i));
					Vector<Object[]> fullUniqueSamples = new Vector<Object[]>();
					int[] sampleIndices = new int[parameters.length];
					for (int j = 0; j < sampleIndices.length; j++) {
						int sampleIndex = -1;
						for (int k = 0; k < results.get(0).getParameterComb().getNames().size(); k++) {
							if(results.get(0).getParameterComb().getNames().get(k).getName().equals(parameters[j])){
								sampleIndex = k;
							}
						}
						sampleIndices[j] = sampleIndex;
					}
					for (int j = 0; j < filtered.size(); j++) {
						Result run = filtered.get(j);
						Object[] sample = getIndexSubset(run.getParameterComb().getValues(), sampleIndices);
						if(isUniqueSample(fullUniqueSamples, sample)){
							fullUniqueSamples.add(sample);
						}
					}
					final Vector<Double[]> means = new Vector<Double[]>();
					final Vector<Double[]> stdDevs = new Vector<Double[]>();
					final Vector<Double> populations = new Vector<Double>(fullUniqueSamples.size());
					for (int j = 0; j < fullUniqueSamples.size(); j++) {
						final List<Result> filteredFull = filterResults(filtered, parameters, fullUniqueSamples.get(j));
						populations.add((double) filteredFull.size());
						Double[] mean = null;
						for (int l = 0; l < filteredFull.size(); l++) {
							Row lastRow = getLastRow(filteredFull.get(l));
							if (mean == null){
								mean = new Double[lastRow.size()];
							}
							for (int m = 0; m < mean.length; m++) {
								if(mean[m] == null) mean[m] = new Double(0);
								mean[m] += getDoubleFromObject(lastRow.get(m));
							}
						}
						for (int m = 0; m < mean.length; m++) {
							mean[m] /= filteredFull.size();
						}
						Double[] sum2 = null;
						Double[] sumC = null;
						for (int l = 0; l < filteredFull.size(); l++) {
							Row lastRow = getLastRow(filteredFull.get(l));
							if (sum2 == null){
								sum2 = new Double[lastRow.size()];
								sumC = new Double[lastRow.size()];
								for (int m = 0; m < sumC.length; m++) {
									sum2[m] = new Double(0);
									sumC[m] = new Double(0);
								}
							}
							for (int m = 0; m < sumC.length; m++) {
								double toAdd = getDoubleFromObject(lastRow.get(m)) - mean[m];
								sum2[m] += toAdd * toAdd;
								sumC[m] += toAdd;
							}
						}
						final Double[] stdDev = new Double[sumC.length];
						for (int l = 0; l < sumC.length; l++) {
							stdDev[l] = (sum2[l] - (sumC[l]*sumC[l]) / filteredFull.size())/(filteredFull.size() - 1);
							//we take the square root of it, to get the std.dev.:
							stdDev[l] = Math.sqrt(stdDev[l]);
						}
						means.add(mean);
						stdDevs.add(stdDev);
					}
					Double[] weighedStdDevs = NaturalVariation.weighedArithmeticMean(stdDevs, populations);
					Vector<Double[]> coeffOfVariations = new Vector<Double[]>();
					for (int j = 0; j < means.size(); j++) {
						Double[] cv = new Double[means.get(j).length];
						for (int l = 0; l < cv.length; l++) {
							cv[l] = stdDevs.get(j)[l] / Math.abs(means.get(j)[l]);
						}
						coeffOfVariations.add(cv);
					}
					Double[] weighedCoeffOfVariations = NaturalVariation.weighedHarmonicMean(coeffOfVariations, populations);
					uniqueSampleStdDevs.add(weighedStdDevs);
					uniqueSampleCoefficientOfVariations.add(weighedCoeffOfVariations);
				}
			}
			if (needRandomSeedStatistics) {
				seedStdDevs = new Vector<Double[]>();
				seedCoefficientOfVariations = new Vector<Double[]>();
				MEMEApp.LONG_OPERATION.setTaskName("Calculating seed statistics");
				for (int rngSeedIndex = 0; rngSeedIndex < rngSeedNames.length; rngSeedIndex++) {
					String[] rngSeedNamesForSeedStats = new String[1];
					rngSeedNamesForSeedStats[0] = rngSeedNames[rngSeedIndex];
					String[] observedSeed = new String[1];
					observedSeed[0] = null;
					first = results.get(0);
					for (int i = 0; i < first.getParameterComb().getNames().size() && observedSeed[0] == null; i++) {
						if(!first.getParameterComb().getNames().get(i).getName().equals(rngSeedNamesForSeedStats[0])){
							observedSeed[0] = first.getParameterComb().getNames().get(i).getName();
						}
					}
					int[] observedIndicesSeed = new int[observedSeed.length];
					for (int i = 0; i < observedIndicesSeed.length; i++) {
						int filterColumnIdx = -1;
						for (int j = 0; j < first.getParameterComb().getNames().size(); j++) {
							if(first.getParameterComb().getNames().get(j).getName().equals(observedSeed[i])){
								filterColumnIdx = j;
							}
						}
						observedIndicesSeed[i] = filterColumnIdx;
					}
					Vector<Object[]> uniqueSamplesSeed = new Vector<Object[]>();
					for (int i = 0; i < results.size(); i++) {
						Result run = results.get(i);
						Object[] sample = getIndexSubset(run.getParameterComb().getValues(), observedIndicesSeed);
						if(isUniqueSample(uniqueSamplesSeed, sample)){
							uniqueSamplesSeed.add(sample);
						}
					}
					//for every unique sample: filter the results and
					//identify every unique input parameter combination within, compute
					//the variation and sample size for them, then compute a weighted
					//average of the variations
					Columns cols = first.getParameterComb().getNames();
					String[] parametersForSeeds = new String[cols.size()-rngSeedNamesForSeedStats.length];
					int paramIdx = 0;
					for (int j = 0; j < cols.size(); j++) {
						if(!Arrays.asList(rngSeedNamesForSeedStats).contains(cols.get(j).getName())){
							parametersForSeeds[paramIdx] = cols.get(j).getName();
							paramIdx++;
						}
					}
					Vector<Double[]> uniqueSampleSeedStdDevs = new Vector<Double[]>();
					Vector<Double[]> uniqueSampleSeedCoeffOfVars = new Vector<Double[]>();
					Vector<Double> uniqueSamplePopulations = new Vector<Double>();
					for (int i = 0; i < uniqueSamplesSeed.size(); i++) {
						MEMEApp.LONG_OPERATION.progress(rngSeedIndex*uniqueSamplesSeed.size()+i, rngSeedNames.length*uniqueSamplesSeed.size());
						List<Result> filtered = filterResults(results, observedSeed, uniqueSamplesSeed.get(i));
						Vector<Object[]> fullUniqueSamples = new Vector<Object[]>();
						int[] sampleIndices = new int[parametersForSeeds.length];
						for (int j = 0; j < sampleIndices.length; j++) {
							int sampleIndex = -1;
							for (int k = 0; k < first.getParameterComb().getNames().size(); k++) {
								if(first.getParameterComb().getNames().get(k).getName().equals(parametersForSeeds[j])){
									sampleIndex = k;
								}
							}
							sampleIndices[j] = sampleIndex;
						}
						for (int j = 0; j < filtered.size(); j++) {
							Result run = filtered.get(j);
							Object[] sample = getIndexSubset(run.getParameterComb().getValues(), sampleIndices);
							if(isUniqueSample(fullUniqueSamples, sample)){
								fullUniqueSamples.add(sample);
							}
						}
						final Vector<Double> populations = new Vector<Double>(fullUniqueSamples.size());
						final Vector<Double[]> means = new Vector<Double[]>();
						final Vector<Double[]> stdDevs = new Vector<Double[]>();
						for (int j = 0; j < fullUniqueSamples.size(); j++) {
							final List<Result> filteredFull = filterResults(filtered, parametersForSeeds, fullUniqueSamples.get(j));
							if (j==0) { //only do it once
								if (seedValuesSize == null) {
									seedValuesSize = new int[rngSeedNames.length];
								}
								seedValuesSize[rngSeedIndex] = filteredFull.size();
							}
							populations.add((double) filteredFull.size());
							Double[] mean = null;
							for (int l = 0; l < filteredFull.size(); l++) {
								Row lastRow = getLastRow(filteredFull.get(l));
								if (mean == null){
									mean = new Double[lastRow.size()];
								}
								for (int m = 0; m < mean.length; m++) {
									if(mean[m] == null) mean[m] = new Double(0);
									mean[m] += getDoubleFromObject(lastRow.get(m));
								}
							}
							for (int m = 0; m < mean.length; m++) {
								mean[m] /= filteredFull.size();
							}
							Double[] sum2 = null;
							Double[] sumC = null;
							for (int l = 0; l < filteredFull.size(); l++) {
								Row lastRow = getLastRow(filteredFull.get(l));
								if (sum2 == null){
									sum2 = new Double[lastRow.size()];
									sumC = new Double[lastRow.size()];
									for (int m = 0; m < sumC.length; m++) {
										sum2[m] = new Double(0);
										sumC[m] = new Double(0);
									}
								}
								for (int m = 0; m < sumC.length; m++) {
									double toAdd = getDoubleFromObject(lastRow.get(m)) - mean[m];
									sum2[m] += toAdd * toAdd;
									sumC[m] += toAdd;
								}
							}
							final Double[] stdDev = new Double[sumC.length];
							for (int l = 0; l < sumC.length; l++) {
								stdDev[l] = (sum2[l] - (sumC[l]*sumC[l]) / filteredFull.size())/(filteredFull.size() - 1);
								//we take the square root of the variance, to get the std.dev.:
								stdDev[l] = Math.sqrt(stdDev[l]);
							}
							means.add(mean);
							stdDevs.add(stdDev);
						}
						Vector<Double[]> coeffOfVariations = new Vector<Double[]>();
						for (int j = 0; j < means.size(); j++) {
							Double[] cv = new Double[means.get(j).length];
							for (int l = 0; l < cv.length; l++) {
								cv[l] = stdDevs.get(j)[l] / Math.abs(means.get(j)[l]);
							}
							coeffOfVariations.add(cv);
						}
						double[] weights = new double[fullUniqueSamples.size()];
						double sumW = 0;
						double population = 0;
						for (int j = 0; j < populations.size(); j++) {
							weights[j] = populations.get(j);
							sumW += populations.get(j);
							population += populations.get(j);
						}
						for (int j = 0; j < weights.length; j++) {
							weights[j] /= sumW;
						}
						Double[] weighedStdDev = new Double[stdDevs.get(0).length];
						for (int j = 0; j < weighedStdDev.length; j++) {
							weighedStdDev[j] = new Double(0.0);
							for (int p = 0; p < weights.length; p++) {
								weighedStdDev[j] += weights[p]*stdDevs.get(p)[j];
							}
						}
						uniqueSampleSeedStdDevs.add(weighedStdDev);
						uniqueSampleSeedCoeffOfVars.add(NaturalVariation.weighedHarmonicMean(coeffOfVariations, populations));
						uniqueSamplePopulations.add(population);
					}
					seedStdDevs.add(NaturalVariation.weighedArithmeticMean(uniqueSampleSeedStdDevs, uniqueSamplePopulations));
					seedCoefficientOfVariations.add(NaturalVariation.weighedHarmonicMean(uniqueSampleSeedCoeffOfVars, uniqueSamplePopulations));
				}
			}
		}
		return needNaturalVariation || needRandomSeedStatistics;
	}
	
	//----------------------------------------------------------------------------------------------------
	public static Row getLastRow(Result result) {
		final Iterable<Row> rows = result.getAllRows();
		Row lastRow = null;
		for (final Iterator<Row> iter = rows.iterator(); iter.hasNext();) {
			Row row = iter.next();
			lastRow = row;
		}
		return lastRow;
	}
	
	//----------------------------------------------------------------------------------------------------
	public static Double getDoubleFromObject(final Object from) {
		Double ret = null;
		if (from != null){
			try {
				ret = new Double(from.toString());
			} catch (NumberFormatException e) {
				if (from.toString().equalsIgnoreCase("false"))
					ret = new Double(0);
				else
					ret = new Double(1);
			}
		} else {
			ret = new Double(0.0);
		}
		return ret;
	}

	//----------------------------------------------------------------------------------------------------
	public static Object[] getIndexSubset(final GeneralRow values, final int[] indices){
		final Object[] ret = new Object[indices.length];
		for (int i = 0; i < ret.length; i++) 
			ret[i] = values.get(indices[i]);
		return ret;
	}
	
	//----------------------------------------------------------------------------------------------------
	protected static boolean isUniqueSample(final List<Object[]> uniqueSamples, final Object[] sample) {
		boolean retUnique = true;
		for (int i = 0; i < uniqueSamples.size() && retUnique; i++) 
			retUnique = !Arrays.equals(uniqueSamples.get(i),sample);
		return retUnique;
	}
	
	//----------------------------------------------------------------------------------------------------
	public static List<Result> filterResults(List<Result> runs, String filterColumnName, Object filterValue) {
		ArrayList<Result> ret = new ArrayList<Result>();
		int filterColumnIdx = -1;
		if (runs.size() > 0) {
			for (int i = 0;i < runs.get(0).getParameterComb().getNames().size();i++) {
				if (runs.get(0).getParameterComb().getNames().get(i).getName().equals(filterColumnName)) 
					filterColumnIdx = i;
            }
		}
		if (filterColumnIdx != -1) {
			for (final Result run : runs) {
				if (run.getParameterComb().getValues().get(filterColumnIdx).toString().equals(filterValue.toString())) 
					ret.add(run);
			}
		}
		return ret;
	}
	
	//----------------------------------------------------------------------------------------------------
	public static List<Result> filterResults(List<Result> runs,String[] filterColumnName,Object[] filterValue) {
		List<Result> ret2 = null;
		if (filterColumnName.length == filterValue.length && filterColumnName.length > 0) {
			int i = 1;
			ret2 = filterResults(runs,filterColumnName[0],filterValue[0]); 
			while (ret2.size() > 0 && i < filterValue.length) {
				ret2 = filterResults(ret2,filterColumnName[i],filterValue[i]);
				i++;
			}
		}
		ArrayList<Result> ret = null;
		if (ret2 != null)
			ret = new ArrayList<Result>(ret2);
		else
			ret = new ArrayList<Result>();
		return ret;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void writeVariationResults(final IResultsDbMinimal db, final String isPluginXMLFileName) throws Exception {
		String newModelName = null;
		String timestamp = Util.getTimeStamp();
		Model model = null;
		newModelName = first.getModel().getName();
		if (needNaturalVariation) {
			//generating columns:
			final Columns natVarColumns = new Columns();
			for (int i = 0; i < observedIndices.length; i++) {
				natVarColumns.append(
						first.getParameterComb().getNames().get(observedIndices[i]).getName(), 
						first.getParameterComb().getNames().get(observedIndices[i]).getDatatype()
				);
			}
			for (int i = 0; i < first.getOutputColumns().size(); i++) {
				natVarColumns.append("Standard deviation of " + first.getOutputColumns().get(i).getName(), ColumnType.DOUBLE);
				natVarColumns.append("Coefficient of variation for " + first.getOutputColumns().get(i).getName(), ColumnType.DOUBLE);
			}
			//generating new model:
			final String versionNatVar = first.getModel().getVersion()+"_Natural_Variation"+timestamp;
			model = db.findModel(newModelName, versionNatVar);
			if (model == null)
				model = new Model(Model.NONEXISTENT_MODELID, newModelName, versionNatVar);
			// Generate batch number
			final int b = db.getNewBatch(model);
			final Columns cols = new Columns();
			for (int i = 0; i < uniqueSamples.size(); i++) {
				ResultInMem toAdd = new ResultInMem();
				toAdd.setModel(model);
				toAdd.setBatch(b);
				toAdd.setStartTime(first.getStartTime());
				toAdd.setEndTime(first.getEndTime());
				toAdd.setRun(i);

				ParameterComb pc = new ParameterComb(new GeneralRow(cols));
				Result.Row row = new Result.Row(natVarColumns, 0);
				for (int j = 0; j < uniqueSamples.get(i).length; j++) {
					row.set(j, uniqueSamples.get(i)[j]);
				}
				for (int j = 0; j < uniqueSampleStdDevs.get(i).length; j++) {
					row.set(uniqueSamples.get(i).length + 2*j, uniqueSampleStdDevs.get(i)[j]);
				}
				for (int j = 0; j < uniqueSampleCoefficientOfVariations.get(i).length; j++) {
					row.set(uniqueSamples.get(i).length + 2*j+1, uniqueSampleCoefficientOfVariations.get(i)[j]);
				}
				toAdd.setParameterComb(pc);
				toAdd.add(row);
				db.addResult(toAdd);
			}
		}
		if (needRandomSeedStatistics) {
			final Columns natVarColumnsSeedStat = new Columns();
			natVarColumnsSeedStat.append("Random Seed", ColumnType.STRING);
			natVarColumnsSeedStat.append("Seed values size", ColumnType.INT);
			for (int i = 0; i < first.getOutputColumns().size(); i++) {
				natVarColumnsSeedStat.append("Combined standard deviation of " + first.getOutputColumns().get(i).getName(), ColumnType.DOUBLE);
				natVarColumnsSeedStat.append("Combined coefficient of variation for " + first.getOutputColumns().get(i).getName(), ColumnType.DOUBLE);
			}
			final String versionSeedStat = first.getModel().getVersion()+"_Seed_Statistics"+timestamp;
			Model modelSeedStat = db.findModel(newModelName, versionSeedStat);
			if (modelSeedStat == null)
				modelSeedStat = new Model(Model.NONEXISTENT_MODELID, newModelName, versionSeedStat);
			// Generate batch number
			final int bSeedStat = db.getNewBatch(model);
			final Columns colsSeedStat = new Columns();
			for (int i = 0; i < rngSeedNames.length; i++) {
				ResultInMem toAdd = new ResultInMem();
				toAdd.setModel(modelSeedStat);
				toAdd.setBatch(bSeedStat);
				toAdd.setStartTime(first.getStartTime());
				toAdd.setEndTime(first.getEndTime());
				toAdd.setRun(i);
				
				ParameterComb pc = new ParameterComb(new GeneralRow(colsSeedStat));
				Result.Row row = new Result.Row(natVarColumnsSeedStat, 0);
				row.set(0, rngSeedNames[i]);
				row.set(1, seedValuesSize[i]);
				for (int j = 0; j < seedStdDevs.get(i).length; j++) {
					row.set(2 + 2*j, seedStdDevs.get(i)[j]);
				}
				for (int j = 0; j < seedCoefficientOfVariations.get(i).length; j++) {
					row.set(2 + 2*j+1, seedCoefficientOfVariations.get(i)[j]);
				}
				toAdd.setParameterComb(pc);
				toAdd.add(row);
				db.addResult(toAdd);
			}
			
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public JPanel getGenericProcessPanel(final Element pluginElement){
		if (this.pluginElement == null || this.genericProcesserPanel == null) {
			this.pluginElement = pluginElement;
			NodeList nl = this.pluginElement.getElementsByTagName(RngSeedManipulatorModel.NATURAL_VARIATION_ELEMENT);
			if (nl != null && nl.getLength() > 0) {
				natVarElements = new Vector<Element>();
				natVarCheckboxes = new Vector<JCheckBox>();
				for (int i = 0;i < nl.getLength();i++) {
					final Element natVarElem = (Element) nl.item(i);
					natVarElements.add(natVarElem);
					final JCheckBox natVarCB = new JCheckBox(natVarElem.getAttribute("name"));
					boolean selected = false;
					if (natVarElem.hasAttribute("selected")) 
						selected = natVarElem.getAttribute("selected").equalsIgnoreCase("true");
					else
						selected = true;
					natVarCB.setSelected(selected);
					natVarCheckboxes.add(natVarCB);
				}
			} else return null;
			this.genericProcesserPanel = new JPanel(new BorderLayout());
			final JPanel checkboxesPanel = new JPanel(new GridLayout(0,1));
			checkboxesPanel.setPreferredSize(new Dimension(500, 300));
			final JPanel checkboxesPanelPanel = FormsUtils.build("~ p ~", 
																 "_ p||" +
																 "0 p||", 
																 checkboxesPanel).getPanel();
			for (int i = 0;i < natVarCheckboxes.size();i++) 
				checkboxesPanel.add(natVarCheckboxes.get(i));
			this.genericProcesserPanel.add(new JLabel("Select the variables to study how their values affect natural variation"),BorderLayout.NORTH);
			this.genericProcesserPanel.add(checkboxesPanelPanel,BorderLayout.CENTER);
			final JPanel buttonsPanel = new JPanel(new FlowLayout());
			selectAllButton = new JButton("Select all");
			selectAllButton.setActionCommand(SELECT_ALL);
			buttonsPanel.add(selectAllButton);
			selectNoneButton = new JButton("Select none");
			selectNoneButton.setActionCommand(SELECT_NONE);
			buttonsPanel.add(selectNoneButton);
			this.genericProcesserPanel.add(buttonsPanel, BorderLayout.SOUTH);
			GUIUtils.addActionListener(this, selectAllButton, selectNoneButton);
			this.genericProcesserPanel.setBorder(BorderFactory.createTitledBorder("Natural variation"));
		}
		return this.genericProcesserPanel;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void doGenericProcessing(){
		for (int i = 0;i < natVarCheckboxes.size();i++) 
			natVarElements.get(i).setAttribute("selected","" + natVarCheckboxes.get(i).isSelected());
	}

	//----------------------------------------------------------------------------------------------------
	public Vector<Object[]> getUniqueSamples() { return uniqueSamples; }
	public Vector<Double[]> getUniqueSampleVariances() { return uniqueSampleStdDevs; }
	public String[] getObserved() { return observed; }
	public boolean isGenericProcessingSupported() {	return true; }

	//----------------------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(SELECT_ALL)) {
			for (int i = 0;i < natVarCheckboxes.size();i++) 
				natVarCheckboxes.get(i).setSelected(true);
		} else if (e.getActionCommand().equals(SELECT_NONE)) {
			for (int i = 0;i < natVarCheckboxes.size();i++) 
				natVarCheckboxes.get(i).setSelected(false);
		}
	}
}
