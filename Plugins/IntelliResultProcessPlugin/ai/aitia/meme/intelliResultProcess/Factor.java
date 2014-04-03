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

import org.jscience.mathematics.function.Polynomial;
import org.jscience.mathematics.function.Variable;
import org.jscience.mathematics.number.Float64;

/**
 * A class to store the plus and minus (high and low) values of a factor in a
 * Design of Experiments setting.
 * @author Ferschl
 *
 */
public class Factor {
	
	private String name = null;
	private Object plusValue = null;
	private double plusDouble = 1.0;
	private Object minusValue = null;
	private double minusDouble = -1.0;
	private Variable<Float64> varF = null;
	private Polynomial<Float64> rangeCorrector = null;
	
	/**
     * @param plusValue
     * @param minusValue
     */
    public Factor(String name, Object plusValue, Object minusValue) {
    	this.name = name;
	    this.plusValue = plusValue;
	    this.minusValue = minusValue;
	    this.plusDouble = FactorCombination.getDoubleFromObject(plusValue);
	    this.minusDouble = FactorCombination.getDoubleFromObject(minusValue);
	    varF = new Variable.Local<Float64>(name);
	    rangeCorrector = Polynomial.valueOf(Float64.valueOf(2.0/(plusDouble-minusDouble)), varF);
	    rangeCorrector = rangeCorrector.plus(Float64.valueOf(-((plusDouble+minusDouble)/(plusDouble-minusDouble))));
    }

	public Object getMinusValue() {
    	return minusValue;
    }

	public Object getPlusValue() {
    	return plusValue;
    }

	public String getName() {
    	return name;
    }
	
	public Variable<Float64> getVariable(){
		return varF;
	}
	
	public Polynomial<Float64> getRangeCorrector(){
		return rangeCorrector;
	}

	public double getPlusDouble() {
		return plusDouble;
	}

	public double getMinusDouble() {
		return minusDouble;
	}
	

}
