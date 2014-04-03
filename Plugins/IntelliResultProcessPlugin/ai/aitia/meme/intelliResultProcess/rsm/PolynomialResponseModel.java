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
package ai.aitia.meme.intelliResultProcess.rsm;

import java.util.List;
import java.util.Vector;

import org.jscience.mathematics.function.Polynomial;
import org.jscience.mathematics.function.Variable;
import org.jscience.mathematics.number.Float64;

import ai.aitia.meme.processing.model.PolynomialRegressor;
import ai.aitia.meme.processing.model.SampleQueryModel;

public class PolynomialResponseModel implements IResponseSurfaceModel {
	
//	protected DoubleArguments arguments = null;
	protected Vector<PolynomialRegressor> responses = null;
//	protected Vector<String> responseNames = null;
	protected SampleQueryModel sampleModel = null;

	public PolynomialResponseModel(SampleQueryModel sampleModel){
		this.sampleModel = sampleModel;
//		arguments = new DoubleArguments();
		responses = new Vector<PolynomialRegressor>();
//		responseNames = new Vector<String>();
	}
	
	public List<String> getArgumentNames() {
		return sampleModel.getArgumentNames();
	}

	public double getResponse(int responseNumber, IArguments arguments) {
		Polynomial<Float64> poly = responses.get(responseNumber).getRegressor();
		List<Variable<Float64>> vars = poly.getVariables();
		DoubleArguments args = (DoubleArguments) arguments;
		for (int i = 0; i < vars.size(); i++) {
			vars.get(i).set(Float64.valueOf(args.getValueForName(vars.get(i).getSymbol())));
		}
		return poly.evaluate().doubleValue();
	}

	public double getResponse(String responseName, IArguments arguments) {
		int i = 0;
		while(!getResponseNames().get(i).equals(responseName) && i < getResponseNames().size()) i++;
		if(i >= getResponseNames().size()) throw new IllegalArgumentException();
		return getResponse(i, arguments);
	}

	public List<String> getResponseNames() {
		return sampleModel.getResponseNames();
	}

	public int getResponseSize() {
		return responses.size();
	}
	
	public Vector<PolynomialRegressor> getResponses(){
		return responses;
	}
	
	public static void setVariableValues(
			List<Variable<Float64>> variables, 
			SampleQueryModel sampleModel, 
			int sampleIndex){
		for (int i = 0; i < variables.size(); i++) {
			String name = variables.get(i).getSymbol();
			int sampleIndexInModel = sampleModel.getSampleNameIndex(name);
			if(sampleIndexInModel == -1){
				throw new IllegalArgumentException("There are variables that cannot be given a value.");
			}
			variables.get(i).set(Float64.valueOf(sampleModel.getSamplePoint(sampleIndex).get(sampleIndexInModel)));
		}
	}

	public SampleQueryModel getSampleModel() {
		return sampleModel;
	}

}
