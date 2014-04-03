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
import java.util.Vector;

import ai.aitia.meme.intelliResultProcess.rsm.IArguments;
import ai.aitia.meme.utils.Utils;

public class SampleQueryModel {
	
	protected Vector<Vector<Double>> samplePoints = null;
	protected Vector<String> sampleNames = null;
	protected Vector<Vector<Double>> responsePoints = null;
	protected Vector<String> responseNames = null;
	protected Vector<Double> mins = null;
	protected Vector<Double> maxs = null;
	
	public SampleQueryModel(Vector<String> sampleNames, Vector<String> responseNames) {
		this.sampleNames = sampleNames;
		this.responseNames = responseNames;
		samplePoints = new Vector<Vector<Double>>();
		responsePoints = new Vector<Vector<Double>>();
		mins = new Vector<Double>();
		maxs = new Vector<Double>();
		for (@SuppressWarnings("unused")
		String sample : sampleNames) {
			mins.add(Double.POSITIVE_INFINITY);
			maxs.add(Double.NEGATIVE_INFINITY);
		}
	}

	public void addSample(Vector<Double> inputs, Vector<Double> outputs){
		samplePoints.add(inputs);
		for (int i = 0; i < inputs.size(); i++) {
			if(inputs.get(i) > maxs.get(i))
				maxs.set(i, inputs.get(i));
			if(inputs.get(i) < mins.get(i))
				mins.set(i, inputs.get(i));
		}
		responsePoints.add(outputs);
	}
	
	public Vector<Double> getSamplePoint(int i){
		return samplePoints.get(i);
	}
	
	public int getSampleNameIndex(String sampleName){
		int ret = -1;
		for (int i = 0; i < sampleNames.size(); i++) {
			if(sampleNames.get(i).equals(sampleName)) ret = i;
		}
		return ret;
	}
	
	public double[][] getResponseColumn(int i){
		double[][] ret = new double[responsePoints.size()][1];
		for (int j = 0; j < responsePoints.size(); j++) {
			ret[j][0] = responsePoints.get(j).get(i);
		}
		return ret;
	}

	public double[][] getResponseColumn(String responseName){
		int i = 0;
		while(!responseNames.get(i).equals(responseName) && i < responseNames.size()) i++;
		if(i >= responseNames.size()) throw new IllegalArgumentException();
		return getResponseColumn(i);
	}

	public List<String> getArgumentNames() {
		return sampleNames;
	}

	public double getResponse(int responseNumber, IArguments arguments) {
		return Double.NaN;
	}

	public double getResponse(String responseName, IArguments arguments) {
		return Double.NaN;
	}

	public List<String> getResponseNames() {
		return responseNames;
	}

	public int getResponseSize() {
		return responseNames.size();
	}

	public int getSamplesSize() {
		return samplePoints.size();
	}
	
	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < sampleNames.size(); i++) {
			sb.append(sampleNames.get(i)+" ");
		}
		sb.append("\n");
		for (int i = 0; i < samplePoints.size(); i++) {
			for (int j = 0; j < samplePoints.get(i).size(); j++) {
				sb.append(samplePoints.get(i).get(j)); sb.append(" ");
			}
			sb.append("\n");
		}
		sb.append("\n");
		
		for (int i = 0; i < responseNames.size(); i++) {
			sb.append(responseNames.get(i)+" ");
		}
		sb.append("\n");
		for (int i = 0; i < responsePoints.size(); i++) {
			for (int j = 0; j < responsePoints.get(i).size(); j++) {
				sb.append(responsePoints.get(i).get(j)); sb.append(" ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public List<String> getSampleNames(){
		return sampleNames;
	}
	
	public double getSampleMin(int i){
		return mins.get(i);
	}
	public double getSampleMax(int i){
		return maxs.get(i);
	}
	public double getSampleCenter(int i){
		return (getSampleMax(i) + getSampleMin(i)) / 2;
	}

	public double getSampleMin(String sampleName){
		return getSampleMin(getSampleNameIndex(sampleName));
	}
	public double getSampleMax(String sampleName){
		return getSampleMax(getSampleNameIndex(sampleName));
	}
	public double getSampleCenter(String sampleName){
		return getSampleCenter(getSampleNameIndex(sampleName));
	}
	
	public SampleQueryModel getSampleQueryModelWithoutConstants(long maxFloatsBetween){
		Vector<String> newSampleNames = new Vector<String>(sampleNames);
		Vector<String> newResponseNames = new Vector<String>(responseNames);
		Vector<Integer> includeThisIndices = new Vector<Integer>();
		Vector<Integer> removeThisIndices = new Vector<Integer>();
		for (int i = 0; i < sampleNames.size(); i++) {
			includeThisIndices.add(i);
		}
		for (Iterator iter = includeThisIndices.iterator(); iter.hasNext();) {
			Integer idx = (Integer) iter.next();
			if (Utils.doubleEqualsWithTolerance(maxs.get(idx), mins.get(idx), maxFloatsBetween)) {
				removeThisIndices.add(idx);
				iter.remove();
			}
		}
		for (int i = removeThisIndices.size() - 1; i >= 0; i--) {
			newSampleNames.remove(removeThisIndices.get(i));
			newResponseNames.remove(removeThisIndices.get(i));
		}
		SampleQueryModel ret = new SampleQueryModel(newSampleNames, newResponseNames);
		for (int j = 0; j < samplePoints.size(); j++) {
			Vector<Double> sampleLine = samplePoints.get(j);
			Vector<Double> responseLine = responsePoints.get(j);
			Vector<Double> newSampleLine = new Vector<Double>(sampleLine);
			for (int i = removeThisIndices.size() - 1; i >= 0; i--) {
				newSampleLine.remove(removeThisIndices.get(i));
			}
			ret.addSample(newSampleLine, responseLine);
		}
		return ret;
	}
}
