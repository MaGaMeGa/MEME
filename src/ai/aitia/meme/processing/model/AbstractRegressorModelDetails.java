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

import java.util.List;
import java.util.Vector;

public abstract class AbstractRegressorModelDetails implements IRegressorModelDetails {

	protected Vector<String> inputNames = null;
	protected Vector<String> outputNames = null;

	public AbstractRegressorModelDetails(List<String> inputs, List<String> outputs) {
		this.inputNames = new Vector<String>();
		this.inputNames.addAll(inputs);
		this.outputNames = new Vector<String>();
		this.outputNames.addAll(outputs);
	}

	public String getDetailsAsString() {
		StringBuilder ret = new StringBuilder();
		ret.append("{");
		for (int i = 0; i < inputNames.size() - 1; i++) {
			ret.append(inputNames.get(i) + ", ");
		}
		ret.append(inputNames.get(inputNames.size() - 1) + "} -> {");
		for (int i = 0; i < outputNames.size() - 1; i++) {
			ret.append(outputNames.get(i) + ", ");
		}
		ret.append(outputNames.get(outputNames.size() - 1) + "}");
		return ret.toString();
	}
	
	@Override
	public String toString() {
		return getDetailsAsString();
	}
	
	
}
