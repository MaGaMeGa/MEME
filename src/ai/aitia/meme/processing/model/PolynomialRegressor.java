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
package ai.aitia.meme.processing.model;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.jscience.mathematics.function.Polynomial;
import org.jscience.mathematics.function.Term;
import org.jscience.mathematics.function.Variable;
import org.jscience.mathematics.number.Float64;


import Jama.Matrix;

public class PolynomialRegressor {
	
	@SuppressWarnings("serial")
	public static class CannotComputeLeastSquaresException extends Exception{
		
	}
	
	protected Polynomial<Float64> regressor = Polynomial.valueOf(Float64.ONE, Term.ONE);
	protected String responseName = null;
	protected SampleQueryModel samples = null;
	protected Vector<Polynomial<Float64>> termsAsPolys = null;
	protected Vector<Variable.Local<Float64>> variables = null;
	protected Vector<VariableInfo> variableInfos = null;
	protected Vector<Term> termsWithoutOne = null;
	protected Vector<Polynomial<Float64>> termsLeftOutAsPolys = null;
	protected Vector<Term> termsLeftOutWithoutOne = null;
	
	public PolynomialRegressor(SampleQueryModel samples, String responseName){
		this.samples = samples;
		this.responseName = responseName;
	}
	
	public void createDefaultPolynomial() throws CannotComputeLeastSquaresException{
		createPolynomial(1);
	}
	
	/**
	 * Creates a regressor polynom of the specified degree for every variable.
	 * @param degree	The degree for every variable.
	 * @throws CannotComputeLeastSquaresException 
	 */
	public void createPolynomial(int degree) throws CannotComputeLeastSquaresException{
		createTermsAsPolys(degree);
		regressor = computeRegressor();
	}
	
	protected void createTermsAsPolys(int degree){
		if (degree < 1) throw new IllegalArgumentException("The polynomial must have a positive degree");
		List<String> inputNames = samples.getArgumentNames();
		variables = new Vector<Variable.Local<Float64>>();
		Polynomial<Float64> tempRegr = Polynomial.valueOf(Float64.ONE, Term.ONE);
		for (int i = 0; i < inputNames.size(); i++) {
			Variable.Local<Float64> newVar = new Variable.Local<Float64>(inputNames.get(i));
			variables.add(newVar);
			tempRegr = tempRegr.plus(Polynomial.valueOf(Float64.ONE, newVar));
		}
		termsAsPolys = PolynomialRegressor.getTermsAsPolys(tempRegr.pow(degree));
		termsLeftOutAsPolys = new Vector<Polynomial<Float64>>();
	}
	
	public void createPolynomial(int[] degrees) throws CannotComputeLeastSquaresException{
		int maxDegree = 0;
		for (int i = 0; i < degrees.length; i++)
			if(maxDegree < degrees[i]) maxDegree = degrees[i];
		if(maxDegree == 0) throw new IllegalArgumentException("At least one variable must have a non-negative degree");
		createTermsAsPolys(maxDegree);
		for (int i = 0; i < variables.size(); i++) {
			Variable<Float64> var = variables.get(i);
			for (Iterator polyIter = termsAsPolys.iterator(); polyIter.hasNext();) {
				@SuppressWarnings("unchecked")
				Polynomial<Float64> poly = (Polynomial<Float64>) polyIter.next();
				Set<Term> termSet = poly.getTerms();
				Term term = null;
				boolean removed = false;
				for (Iterator termIter = termSet.iterator(); termIter.hasNext() && !removed;) {
					term = (Term) termIter.next();
					if (term.getPower(var) > degrees[i]){
						polyIter.remove();
						removed = true;
						if(degrees[i] > 0){
							termsLeftOutAsPolys.add(poly);
						}
					}
				}
			}
		}
		regressor = computeRegressor();
	}
	
	public void createPolynomial(List<VariableInfo> variables) throws CannotComputeLeastSquaresException{
		variableInfos = new Vector<VariableInfo>(variables);
		int degrees[] = new int[samples.getArgumentNames().size()];
		for (int i = 0; i < degrees.length; i++) {
			degrees[i] = 0; //just to be sure
		}
		for (VariableInfo info : variables) {
			int idx = 0;
			while(!samples.getArgumentNames().get(idx).equals(info.getName()) && idx < degrees.length)
				idx++;
			if (idx >= degrees.length) throw new IllegalStateException("Variable "+info.getName()+" not found in the samples");
			degrees[idx] = info.getOrder();
		}
		createPolynomial(degrees);
	}
	
	public void createLinearInteractionPolynomial(int interactionSize) throws CannotComputeLeastSquaresException{
		if (interactionSize < 1 || interactionSize > 3) {
			throw new IllegalArgumentException("interactionSize must be between 1 and 3");
		}
		createPolynomial(interactionSize);
		if(interactionSize > 1){
			for (int i = 0; i < variables.size(); i++) {
				Variable<Float64> var = variables.get(i);
				for (Iterator polyIter = termsAsPolys.iterator(); polyIter.hasNext();) {
					@SuppressWarnings("unchecked")
					Polynomial<Float64> poly = (Polynomial<Float64>) polyIter.next();
					Set<Term> termSet = poly.getTerms();
					Term term = null;
					boolean removed = false;
					for (Iterator termIter = termSet.iterator(); termIter.hasNext() && !removed;) {
						term = (Term) termIter.next();
						if (term.getPower(var) > 1){
							polyIter.remove();
							removed = true;
						}
					}
				}
			}
			regressor = computeRegressor();
		}
	}
	
	/**
	 * This method solves the problem with the Least Squares method.
	 * @return
	 * @throws CannotComputeLeastSquaresException 
	 */
	public Polynomial<Float64> computeRegressor() throws CannotComputeLeastSquaresException{
		Polynomial<Float64> ret = null;
		//Ax=b
		//fill A matrix:
		double[][] A = new double[samples.getSamplesSize()][termsAsPolys.size()];
		for (int i = 0; i < A.length; i++) {
			Vector<Double> sample = samples.getSamplePoint(i);
			//setting the variables:
			for (int j = 0; j < variables.size(); j++) {
				String varName = variables.get(j).getSymbol();
				variables.get(j).set(Float64.valueOf(sample.get(samples.getSampleNameIndex(varName))));
			}
			//calculating the values in the A matrix:
			for (int j = 0; j < A[i].length; j++) {
				A[i][j] = termsAsPolys.get(j).evaluate().doubleValue();
			}
		} //A matrix filled
		for (int i = 0; i < getTermsWithoutOne().size(); i++) {
//			System.out.println(getTermsWithoutOne().get(i).toString());
		}
//		System.out.println();
		double[][] b = samples.getResponseColumn(responseName);
		Matrix amx = new Matrix(A);
		//checking for A's full rank:
		if (!amx.qr().isFullRank()) {
			throw new CannotComputeLeastSquaresException();
		}
//		amx.print(7, 4);
		Matrix bmx = new Matrix(b);
//		bmx.print(7, 4);
		Matrix xmx = amx.solve(bmx);
//		xmx.print(7, 4);
		double[][] x = xmx.getArray();
		ret = Polynomial.valueOf(Float64.ZERO, Term.ONE);
		for (int i = 0; i < x.length; i++) {
			ret = ret.plus(termsAsPolys.get(i).times(Float64.valueOf(x[i][0])));
		}
		regressor = ret;
		//System.out.println("regressor: "+ret.toString());
		return ret;
	}
	
	public Polynomial<Float64> computeRegressor(Vector<Term> terms) throws CannotComputeLeastSquaresException{
		int order = 1;
		for (Term term : terms) {
			for (int i = 0; i < term.size(); i++) {
				if (term.getPower(i) > order) order = term.getPower(i);
			}
		}
		createTermsAsPolys(order);
		getTermsWithoutOne();
		for (int i = 0; i < termsWithoutOne.size(); i++) {
			if (!terms.contains(termsWithoutOne.get(i)))
			termsLeftOutWithoutOne.add(termsWithoutOne.get(i));
		}
		if(termsAsPolys != null) termsAsPolys.clear();
		else termsAsPolys = new Vector<Polynomial<Float64>>();
		for (Term term : terms) {
			termsAsPolys.add(Polynomial.valueOf(Float64.ONE, term));
		}
		return computeRegressor();
	}
	
	public Polynomial<Float64> getRegressor() {
		return regressor;
	}

	protected static Vector<Term> getTermsWithoutOne(Polynomial<Float64> poly) {
		Set<Term> terms = poly.getTerms();
		Vector<Term> ret = new Vector<Term>();
		for (Term term : terms) {
			if(!term.equals(Term.ONE)) {
				ret.add(term);
			}
		}
		return ret;
	}
	
	public static Vector<Polynomial<Float64>> getTermsAsPolys(Polynomial<Float64> poly) {
		Set<Term> terms = poly.getTerms();
		Vector<Polynomial<Float64>> ret = new Vector<Polynomial<Float64>>();
		for (Term term : terms) {
			ret.add(Polynomial.valueOf(Float64.ONE, term));
		}
		return ret;
	}

	public static void main(String[] args) {
		Vector<String> input = new Vector<String>();
		Vector<String> resp = new Vector<String>();
		input.add("input1");
		input.add("input2");
		input.add("input3");
		resp.add("outputA");
		SampleQueryModel sqm = new SampleQueryModel(input, resp);
		double i = 1;
		for (int j = 0; j < 15; j++) {
			Vector<Double> smp = new Vector<Double>(3);
			Vector<Double> rsp = new Vector<Double>(1);
			smp.add((i+=0.04)); smp.add((i+=0.02)); smp.add((i+=0.02));
			rsp.add((i+=0.03));
			sqm.addSample(smp, rsp);
		}
		PolynomialRegressor preg = new PolynomialRegressor(sqm, "outputA");
		int[] degrees = {2, 0, 1};
		try {
			preg.createPolynomial(degrees);
		} catch (CannotComputeLeastSquaresException e) {
			e.printStackTrace();
		}
	}

	public Vector<VariableInfo> getVariableInfos() {
		return variableInfos;
	}

	public String getResponseName(){
		return responseName;
	}

	public Vector<Variable.Local<Float64>> getVariables() {
		return variables;
	}

	public Vector<Polynomial<Float64>> getTermsAsPolys() {
		return termsAsPolys;
	}
	
	public Vector<Term> getTermsWithoutOne(){
		termsWithoutOne = new Vector<Term>();
		for (Polynomial<Float64> poly : termsAsPolys) {
			Set<Term> terms = poly.getTerms();
			for (Term term : terms) {
				if(!term.equals(Term.ONE)) {
					termsWithoutOne.add(term);
				}
			}
		}
		return termsWithoutOne;
	}

	public Vector<Term> getTermsLeftOutWithoutOne(){
		termsLeftOutWithoutOne = new Vector<Term>();
		for (Polynomial<Float64> poly : termsLeftOutAsPolys) {
			Set<Term> terms = poly.getTerms();
			for (Term term : terms) {
				if(!term.equals(Term.ONE)) {
					termsLeftOutWithoutOne.add(term);
				}
			}
		}
		return termsLeftOutWithoutOne;
	}
	
	
}
