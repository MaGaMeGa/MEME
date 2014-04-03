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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jscience.mathematics.function.Polynomial;
import org.jscience.mathematics.function.Term;
import org.jscience.mathematics.number.Float64;

import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.ParameterComb;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.Result.Row;

/**
 * A class to represent a subset of all factors in a Design of Experiments
 * design. It has a method that decides if a given combination of factor values
 * of this combination is plus.
 * @author Ferschl
 */
public class FactorCombination {
	private Vector<Factor> combination = null;
	private String name = null;
	private double paraCombPlus = 0;
	private double paraCombMinus = 0;
	private Vector<Double> rowPlus = null;
	private Vector<Double> rowMinus = null;
//	private Vector<Double> avgHigh = null;
//	private Vector<Double> avgLow = null;
	private int plusCount = 0;
	private int minusCount = 0;
	private Polynomial<Float64> combinationRangeCorrector = null;
	private Term combinationTerm = null;
	protected static final double EPS = 0.005;
	protected static final long EPS_LONG = 16;

	public FactorCombination(List<Factor> comb) {
		combination = new Vector<Factor>(comb);
		paraCombPlus = 0;
		paraCombMinus = 0;
		rowPlus = new Vector<Double>();
		rowMinus = new Vector<Double>();
		//multiplying the factors' rangeCorrector polynoms:
		combinationRangeCorrector = comb.get(0).getRangeCorrector();
		combinationTerm = Term.valueOf(comb.get(0).getVariable(), 1);
		for (int i = 1; i < comb.size(); i++) {
			combinationRangeCorrector = combinationRangeCorrector.times(comb.get(i)
					.getRangeCorrector());
			combinationTerm = combinationTerm.times(Term.valueOf(comb.get(i)
					.getVariable(), 1));
		}
	}

	public boolean isPlus(ParameterComb values) {
		boolean ret = true; //important start value
		for (int i = 0; i < combination.size(); i++) {
			for (int j = 0; j < values.size(); j++) {
				if (combination.get(i).getName().equals(values.getNames().get(j).getName())) {
					ColumnType ct = values.getNames().get(j).getDatatype();
					Object value = values.getValues().get(j);
					if (ct.equals(ColumnType.INT) || ct.equals(ColumnType.LONG) || ct.equals(ColumnType.DOUBLE)){
						Double paramValue = new Double(value.toString());
						Double combMinusValue = new Double(combination.get(i).getMinusValue().toString());
						if(Double.doubleToLongBits(Math.abs(paramValue-combMinusValue)) < 16) {
							ret = !ret;
						}
					} else if (combination.get(i).getMinusValue().toString().equals(value.toString())) {
						ret = !ret;//invert ret if a minus value is in the combination
					}
				}
			}
		}
		return ret;
	}
	
	public boolean isCenterPoint(ParameterComb pc){
		boolean ret = false;
		Factor first = combination.get(0);
		String name = first.getName();
		int idx = -1;
		for (int i = 0; i < pc.getNames().size(); i++) {
			if(pc.getNames().get(i).getName().equals(name)) idx = i;
		}
		if (idx != -1) {
			//		Object value = pc.getValues().get(idx);
			//		ColumnType ct = pc.getNames().get(idx).getDatatype();
			//		if (ct.equals(ColumnType.DOUBLE)) value = new Double(value.toString());
			double minus;
			try {
				minus = new Double(first.getMinusValue().toString());
			} catch (NumberFormatException e) {
				if (first.getMinusValue().toString().equalsIgnoreCase("true")) {
					minus = 1;
				} else {
					minus = 0;
				}
			}
			double plus; 
			try {
				plus = new Double(first.getPlusValue().toString());
			} catch (NumberFormatException e) {
				if (first.getPlusValue().toString().equalsIgnoreCase("true")) {
					plus = 1;
				} else {
					plus = 0;
				}
			}
			double value;
			try {
				value = new Double(pc.getValues().get(idx).toString());
			} catch (NumberFormatException e) {
				if (pc.getValues().get(idx).toString().equalsIgnoreCase("true")) {
					value = 1;
				} else {
					value = 0;
				}
			}
			if(!(equalWithTolerance(minus, value, EPS_LONG) || equalWithTolerance(plus, value, EPS_LONG))){
				ret = true;
			}
		}
		return ret;
	}
	
	public boolean equalWithTolerance(double a, double b, long maxFloatsBetween){
		if(a == b) return true;
		if(Math.abs(Double.doubleToLongBits(a)-Double.doubleToLongBits(b)) <= maxFloatsBetween) return true;
		if(Double.doubleToLongBits(Math.abs(a-b)) <= maxFloatsBetween) return true;
		return false;
	}

//	public void addNewResult(ResultInMem result) {
	public void addNewResult(Result result) {
		ParameterComb pc = result.getParameterComb();
		if(!isCenterPoint(pc)){
			Iterable<Row> rows = result.getAllRows();
			Row lastRow = null;
			for (Iterator iter = rows.iterator(); iter.hasNext();) {
				Row row = (Row) iter.next();
				lastRow = row;
			}
			//		ArrayList<Object> pcValues = new ArrayList<Object>();
			//		for (int i = 0; i < pc.getValues().size(); i++) {
			//	        pcValues.add(pc.getValues().get(i));
			//        }
			boolean isPlus = isPlus(pc);
			int resultWidth = lastRow.size();
			if (rowPlus.size() != resultWidth) {
				rowPlus.clear();
				rowMinus.clear();
				for (int i = 0; i < resultWidth; i++) {
					rowPlus.add(new Double(0));
					rowMinus.add(new Double(0));
				}
			}
			if (isPlus) {
				double tempProduct = 1;
				for (int i = 0; i < combination.size(); i++) {
					for (int j = 0; j < pc.size(); j++) {
						if (combination.get(i).getName().equals(
								pc.getNames().get(j).getName())) {
							tempProduct *= getDoubleFromObject(pc.getValues().get(j));
						}
					}
				}
				paraCombPlus += tempProduct;
				for (int i = 0; i < resultWidth; i++) {
					rowPlus.set(i, rowPlus.get(i) + getDoubleFromObject(lastRow.get(i)));
				}
				plusCount++;
			} else {
				double tempProduct = 1;
				for (int i = 0; i < combination.size(); i++) {
					for (int j = 0; j < pc.size(); j++) {
						if (combination.get(i).getName().equals(
								pc.getNames().get(j).getName())) {
							tempProduct *= getDoubleFromObject(pc.getValues().get(j));
						}
					}
				}
				paraCombMinus += tempProduct;
				for (int i = 0; i < resultWidth; i++) {
					rowMinus.set(i, rowMinus.get(i) + getDoubleFromObject(lastRow.get(i)));
				}
				minusCount++;
			}
		}
	}

	public Vector<Double> getEffects() {
//		avgLow = new Vector<Double>();
//		avgHigh = new Vector<Double>(); 
		Vector<Double> ret = new Vector<Double>(rowPlus.size());
		for (int i = 0; i < rowPlus.size(); i++) {
			ret.add(new Double((rowPlus.get(i) / plusCount)
					- (rowMinus.get(i) / minusCount)));
//			avgLow.add(rowMinus.get(i) / minusCount);
//			avgHigh.add(rowPlus.get(i) / plusCount);
		}
		return ret;
	}

	public String getName() {
		if (name != null) {
			return name;
		} else {
			StringBuffer sb = new StringBuffer();
			sb.append(combination.get(0).getName());
			for (int i = 1; i < combination.size(); i++) {
				sb.append("_" + combination.get(i).getName());
			}
			name = sb.toString();
			return name;
		}
	}

	public static Double getDoubleFromObject(Object from) {
		Double ret = null;
		try {
			ret = new Double(from.toString());
		} catch (NumberFormatException e) {
			if (from.toString().equalsIgnoreCase("false"))
				ret = new Double(0);
			else
				ret = new Double(1);
		}
		return ret;
	}

	public Polynomial<Float64> getCombinationRangeCorrector() {
		return combinationRangeCorrector;
	}

	public Term getCombinationTerm() {
		return combinationTerm;
	}
	
	public double getIntervalLength(){
		double ret = 1.0;
		if (combination.size() == 1) {
			ret = combination.get(0).getPlusDouble()-combination.get(0).getMinusDouble();
		} else if (combination.size() == 2) {
			double plus = 	combination.get(0).getPlusDouble() * combination.get(1).getPlusDouble() +
			combination.get(0).getMinusDouble() * combination.get(1).getMinusDouble();
			double minus = 	combination.get(0).getPlusDouble() * combination.get(1).getMinusDouble() +
			combination.get(0).getMinusDouble() * combination.get(1).getPlusDouble();
			ret = (plus - minus) / 2.0;
		}
		return ret;
	}

//	public Vector<Double> getAvgHigh() {return avgHigh;}
//	public Vector<Double> getAvgLow() {return avgLow;}
}
