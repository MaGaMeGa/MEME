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

import java.util.List;
import java.util.Vector;

/**
 * This class represents the knowledge gathered from the results of the Latin
 * Hypercube design. This class calculates the average response for every
 * input value of a nuisance factor.
 * @author Ferschl
 */
public class LHCFunctionModel {
	private Vector<Double> inputLevels = null;
	private Vector<Double> sumOfResponses = null;
	private Vector<Integer> numberOfOccurences = null;
	private String nuisanceName = null;
	private String responseName = null;
	
	public LHCFunctionModel(String nuisanceName, String responseName, int numberOfLevels){
		inputLevels = new Vector<Double>(numberOfLevels);
		sumOfResponses = new Vector<Double>(numberOfLevels);
		numberOfOccurences = new Vector<Integer>(numberOfLevels);
		for (int i = 0; i < numberOfLevels; i++) {
			sumOfResponses.add(0.0);
	        numberOfOccurences.add(0);
        }
		this.nuisanceName = nuisanceName;
		this.responseName = responseName;
	}
	
	public LHCFunctionModel(String inputName, String outputName){
		inputLevels = new Vector<Double>();
		sumOfResponses = new Vector<Double>();
		numberOfOccurences = new Vector<Integer>();
		this.nuisanceName = inputName;
		this.responseName = outputName;
	}
	
	public void setLevels(Vector<Object> levels){
		for (int i = 0; i < levels.size(); i++) {
	        inputLevels.add(new Double(levels.get(i).toString()));
        }
	}
	
	public List<Double> getLevels(){ return inputLevels; }
	
	public void addResponseValue(Number inputLevel, double response){
		if(inputLevels.size() == 0){
			sumOfResponses.add(response);
	        numberOfOccurences.add(1);
	        inputLevels.add(inputLevel.doubleValue());
		}else{
			int i = 0;
			while(i < inputLevels.size() && inputLevel.doubleValue() != inputLevels.get(i)){
				i++;
			}
			if(i < inputLevels.size()){
				int oldNumberOfOccurences = numberOfOccurences.get(i);
				numberOfOccurences.set(i, oldNumberOfOccurences + 1);
				double olSumOfResponse = sumOfResponses.get(i);
				sumOfResponses.set(i, olSumOfResponse + response);
			} else{
				//it's not in the list, insert it! in the right place (insertion sort)
				int j = 0;
				while(j < inputLevels.size() && inputLevels.get(j).doubleValue() < inputLevel.doubleValue()) j++;
				if(j == inputLevels.size()){//it's larger than every element
					sumOfResponses.add(response);
			        numberOfOccurences.add(1);
			        inputLevels.add(inputLevel.doubleValue());
				} else{//insert it
					sumOfResponses.insertElementAt(response, j);
			        numberOfOccurences.insertElementAt(1, j);
			        inputLevels.insertElementAt(inputLevel.doubleValue(), j);
				}
			}
		}
	}
	
	public List<Double> getFunctionModel(){
		Vector<Double> ret = new Vector<Double>();
		for (int i = 0; i < sumOfResponses.size(); i++) {
	        ret.add(sumOfResponses.get(i)/numberOfOccurences.get(i));
        }
		return ret;
	}

    public String getNuisanceName() { return nuisanceName; }
    public String getResponseName() { return responseName; }
}
