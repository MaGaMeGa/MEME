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
package ai.aitia.meme.processing.quadratic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;
import javax.xml.parsers.ParserConfigurationException;

import org.jscience.mathematics.function.Constant;
import org.jscience.mathematics.function.Polynomial;
import org.jscience.mathematics.function.Term;
import org.jscience.mathematics.function.Variable;
import org.jscience.mathematics.number.Real;
import org.jscience.mathematics.vector.DenseMatrix;
import org.jscience.mathematics.vector.DenseVector;
import org.jscience.mathematics.vector.Matrix;
import org.w3c.dom.Document;
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
import ai.aitia.meme.database.ColumnType.ValueNotSupportedException;
import ai.aitia.meme.database.Result.Row;
import ai.aitia.meme.paramsweep.plugin.IIntelliMethodPlugin;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.pluginmanager.IIntelliResultProcesserPlugin;
import ai.aitia.meme.pluginmanager.IIntelliResultProcesserPluginV2;
import ai.aitia.meme.processing.IIntelliGenericProcesser;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GenericCheckBoxListGui;

public class QuadraticModelProcesser implements IIntelliResultProcesserPlugin, IIntelliResultProcesserPluginV2, IIntelliGenericProcesser {
	
	private GenericCheckBoxListGui<String> termSelector = null;
	private boolean needsQuadraticProcessing = false;

	
	public void doGenericProcessing() throws ValueNotSupportedException {
		if (termSelector != null) {
			if (termSelector.getSelectedIndices().length == 0) {
				needsQuadraticProcessing = false;
			}
		}
	}

	public JPanel getGenericProcessPanel(Element pluginElement) {
		JPanel panel = null;
		NodeList nl = null;
		nl = pluginElement.getElementsByTagName(IIntelliMethodPlugin.VARYING_NUMERIC_PARAMETER);
		if (nl != null && nl.getLength() != 0) {
			needsQuadraticProcessing = true;
			termSelector = new GenericCheckBoxListGui<String>(360, 500);
			ArrayList<String> varyingParameters = new ArrayList<String>();
			for (int i = 0; i < nl.getLength(); i++) {
				String parameterName = ((Element) nl.item(i)).getAttribute("name");
				varyingParameters.add(parameterName);
			}
			termSelector.addAll(varyingParameters);
			//select all:
			int[] indices = new int[varyingParameters.size()];
			for (int i = 0; i < indices.length; i++)
				indices[i] = i;
			termSelector.setSelectedIndices(indices);
			panel = FormsUtils.build("f:p:g", 
					"0||" +
					"1||", 
					"Select parameters for quadratic model:",
					termSelector.getPanel()).getPanel();
		} else {
			needsQuadraticProcessing = false;
			panel = null;
		}
		return panel;
	}

	public boolean isGenericProcessingSupported() {
		return true;
	}

	public String getLocalizedName() {
		return "Quadratic Model Processer";
	}

	public List<String> processResultFiles(IResultsDbMinimal db, List<Result> runs, Element pluginElement, String isPluginXMLFileName) throws Exception {
		List<ResultInMem> quadraticModel = process(db, runs);
		for (int i = 0; i < quadraticModel.size(); i++) {
			db.addResult(quadraticModel.get(i));
		}
		return new ArrayList<String>();
	}

	public List<ResultInMem> processResultFiles(IResultsDbMinimal db, List<ResultInMem> runs, Element pluginElement) {
		return process(db, runs);
	}

	public Document createCharts(String viewName, String model, String version) throws ParserConfigurationException {
		//do nothing
		return null;
	}

	private List<ResultInMem> process(IResultsDbMinimal db, List<? extends Result> runs) {
		ArrayList<ResultInMem> ret = new ArrayList<ResultInMem>();
		if (needsQuadraticProcessing) {
			//determining positions of model variables within result columns:
			Result first = runs.get(0);
			ArrayList<Integer> inputIndices = new ArrayList<Integer>();
			ParameterComb pc = first.getParameterComb();
			for (int i = 0; i < termSelector.getSelectedValues().size(); i++) {
				for (int j = 0; j < pc.getNames().size(); j++) {
					if (pc.getNames().get(j).getName().equals(termSelector.getSelectedValues().get(i))) {
						inputIndices.add(j);
					}
				}
			}
			Columns resultColumns = first.getOutputColumns();
			ArrayList<String> resultStrings = new ArrayList<String>();
			ArrayList<Integer> resultIndices = new ArrayList<Integer>();
			for (int i = 0; i < resultColumns.size(); i++) {
				if(Number.class.isAssignableFrom(resultColumns.get(i).getDatatype().getJavaClass())){
					resultStrings.add(resultColumns.get(i).getName());
					resultIndices.add(i);
				}
			}
			//input variables:
			ArrayList<Variable<Real>> variables = new ArrayList<Variable<Real>>(termSelector.getSelectedIndices().length);
			for (int i = 0; i < termSelector.getSelectedIndices().length; i++) {
				variables.add(new Variable.Local<Real>(termSelector.getSelectedValues().get(i)));
			}
			//terms created from variables:
			ArrayList<Term> terms = new ArrayList<Term>();
			for (int i = 0; i < variables.size(); i++) {
				terms.add(Term.valueOf(variables.get(i), 2));
			}
			for (int i = 0; i < variables.size() - 1; i++) {
				for (int j = i+1; j < variables.size(); j++) {
					Term term1 = Term.valueOf(variables.get(i), 1);
					Term term2 = Term.valueOf(variables.get(j), 1);
					terms.add(term1.times(term2));
				}
			}
			for (int i = 0; i < variables.size(); i++) {
				terms.add(Term.valueOf(variables.get(i), 1));
			}
			terms.add(Term.ONE);
			ArrayList<Matrix<Real>> models = new ArrayList<Matrix<Real>>();
			ArrayList<ArrayList<Term>> termsInModels = new ArrayList<ArrayList<Term>>();
			ArrayList<Boolean> usableModels = new ArrayList<Boolean>();
			Matrix<Real> solved = null; 
			ArrayList<Term> termsInModel = null;
			for (int resultIndex = 0; resultIndex < resultIndices.size(); resultIndex++) {
				boolean noInteractionTerms = false;
				boolean noQuadraticTerms = false;
				boolean notANumberComputedResults = false;
				boolean notANumberImportedResults = false;
				do {
					if (notANumberComputedResults) {
						if (noInteractionTerms) {
							noQuadraticTerms = true;
							noInteractionTerms = false;
						} else if (noQuadraticTerms) {
							noQuadraticTerms = true;
							noInteractionTerms = true;
						} else {
							noQuadraticTerms = false;
							noInteractionTerms = true;
						}
					}
					notANumberComputedResults = false;
					//copying and maybe pruning terms:
					termsInModel = new ArrayList<Term>(terms.size());
					for (int i = 0; i < terms.size(); i++) {
						if (terms.get(i).size() > 1) {
							if (!noInteractionTerms)
								termsInModel.add(terms.get(i));
						} else {
							if (terms.get(i).size() == 1 && terms.get(i).getPower(0) > 1) {
								if (!noQuadraticTerms)
									termsInModel.add(terms.get(i));
							} else {
								termsInModel.add(terms.get(i));
							}
						}
					}
					//Coefficients of terms as variables:
					ArrayList<Variable<Real>> modelCoeffs = new ArrayList<Variable<Real>>(termsInModel.size());
					for (int i = 0; i < termsInModel.size(); i++) {
						modelCoeffs.add(new Variable.Local<Real>("coeff("+termsInModel.get(i).toString()+")"));
					}
					Polynomial<Real> errorPoly = Polynomial.valueOf(Real.valueOf(0), Term.ONE);
					MEMEApp.LONG_OPERATION.setTaskName("Building quadratic model "+(resultIndex+1)+"/"+resultIndices.size());
					MEMEApp.LONG_OPERATION.progress(0, runs.size());
					for (int i = 0; i < runs.size(); i++) {
						MEMEApp.LONG_OPERATION.progress(i, -1);
						//setting values of variables:
						for (int j = 0; j < inputIndices.size(); j++) {
							double value = ((Number) runs.get(i).getParameterComb().getValues().get(inputIndices.get(j))).doubleValue();
							if (value < 0)
								variables.get(j).set(Real.valueOf(Math.abs(value)).opposite());
							else
								variables.get(j).set(Real.valueOf(value));
						}
						//calculating squared error:
						Polynomial<Real> partialError = Constant.valueOf(Real.ZERO); //0 polynomial
						for (int j = 0; j < termsInModel.size(); j++) {
							Polynomial<Real> termPoly = Polynomial.valueOf(Real.ONE, termsInModel.get(j));
							partialError = partialError.plus(Polynomial.valueOf(termPoly.evaluate(), modelCoeffs.get(j)));
						}
						//obtaining the result value (Y):
						Iterable<Row> rows = runs.get(i).getAllRows();
						Row lastRow = null;
						for (Iterator<Row> iter = rows.iterator(); iter.hasNext();) {
							Row row = iter.next();
							lastRow = row;
						}
						Object yObject = lastRow.get(resultIndices.get(resultIndex));
						double yValue = 0.0;
						if (yObject != null)
							yValue = ((Number) yObject).doubleValue();
						else
							notANumberImportedResults = true;
						Real y = Real.valueOf(Math.abs(yValue));
						if (yValue < 0) y = y.opposite();
						//subtracting result value:
						partialError = partialError.minus(Constant.valueOf(y));
						//squaring the error term:
						partialError = partialError.pow(2);
						//adding to the LS-polynomial:
						errorPoly = errorPoly.plus(partialError);
					}
					//calculating partial differentiated errors for every model coefficient:
					ArrayList<Polynomial<Real>> diffedErrors = new ArrayList<Polynomial<Real>>(modelCoeffs.size());
					for (int i = 0; i < modelCoeffs.size(); i++) {
						Polynomial<Real> diffed = errorPoly.differentiate(modelCoeffs.get(i));
						diffedErrors.add(diffed);
					}
					//building matrix:
					ArrayList<DenseVector<Real>> rows = new ArrayList<DenseVector<Real>>();
					for (int i = 0; i < diffedErrors.size(); i++) {
						//building a row
						ArrayList<Real> rowElements = new ArrayList<Real>();
						for (int j = 0; j < modelCoeffs.size(); j++) {
							Real toAdd = diffedErrors.get(i).getCoefficient(Term.valueOf(modelCoeffs.get(j),1));
							if (toAdd != null)
								rowElements.add(toAdd);
							else 
								rowElements.add(Real.ZERO);
						}
						DenseVector<Real> row = DenseVector.valueOf(rowElements);
						rows.add(row);
					}
					//solving the equation:
					DenseMatrix<Real> matrix = DenseMatrix.valueOf(rows);
					ArrayList<Real> bs = new ArrayList<Real>();
					for (int i = 0; i < diffedErrors.size(); i++) {
						Real toAdd = diffedErrors.get(i).getCoefficient(Term.ONE);
						if (toAdd != null)
							bs.add(toAdd.opposite());
						else 
							bs.add(Real.ZERO);
					}
					DenseVector<Real> bVector = DenseVector.valueOf(bs);
					@SuppressWarnings("unchecked")
					Matrix<Real> bMatrix = DenseMatrix.valueOf(bVector);
					solved = matrix.solve(bMatrix.transpose());
					for (int i = 0; i < solved.getNumberOfRows(); i++) {
						if(solved.get(i, 0).equals(Real.NaN)) {
							notANumberComputedResults = true;
						}
					}
				} while (!notANumberImportedResults && notANumberComputedResults && !(noInteractionTerms && noQuadraticTerms));
				models.add(solved);
				termsInModels.add(termsInModel);
				usableModels.add(!(notANumberImportedResults || notANumberComputedResults));
			}
			String newModelName = runs.get(0).getModel().getName();
			String newVersion = runs.get(0).getModel().getVersion() + "_Quadratic_Model_" + Util.getTimeStamp();
			String version = newVersion;
			Model model = db.findModel(newModelName, version);
			if (model == null) model = new Model(Model.NONEXISTENT_MODELID, newModelName, version);
			// Generate batch number
			int b = db.getNewBatch(model);
			Columns quadraticModelColumns = new Columns();
			Columns cols = new Columns();
			quadraticModelColumns.append("Term", ColumnType.STRING);
			for (int i = 0; i < resultIndices.size(); i++) {
				if (usableModels.get(i))
					quadraticModelColumns.append("Coefficients in model for "+resultStrings.get(i), ColumnType.DOUBLE);
				else
					quadraticModelColumns.append("Could not build model for "+resultStrings.get(i), ColumnType.DOUBLE);
			}
			for (int i = 0; i < terms.size(); i++) {
				ResultInMem toAdd = new ResultInMem();
				toAdd.setModel(model);
				toAdd.setBatch(b);
				toAdd.setStartTime(first.getStartTime());
				toAdd.setEndTime(first.getEndTime());
				toAdd.setRun(i);

				ParameterComb pcRes = new ParameterComb(new GeneralRow(cols));
				Result.Row row = new Result.Row(quadraticModelColumns, 0);
				row.set(0, termToString(terms.get(i)));
				for (int j = 0; j < models.size(); j++) {
					if (usableModels.get(j)) {
						int termIndexInModel = 0;
						while (termIndexInModel < termsInModels.get(j).size() && !termsInModels.get(j).get(termIndexInModel).equals(terms.get(i)))
							termIndexInModel++;
						if (termIndexInModel < termsInModels.get(j).size())
							row.set(1 + j, models.get(j).get(termIndexInModel, 0));
						else 
							row.set(1 + j, Double.NaN);
					} else {
						row.set(1 + j, Double.NaN);
					}
				}
				toAdd.setParameterComb(pcRes);
				toAdd.add(row);
				ret.add(toAdd);
			}
		}
		return ret;
	}
	
	public static String termToString(Term term) {
		if(term.equals(Term.ONE)) return "1";
		StringBuilder ret = new StringBuilder();
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
}
