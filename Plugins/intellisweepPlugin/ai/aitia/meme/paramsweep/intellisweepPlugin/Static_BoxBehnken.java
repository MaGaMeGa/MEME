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
package ai.aitia.meme.paramsweep.intellisweepPlugin;

import java.util.Vector;

import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.AbstractThreeLevelMethod;

@SuppressWarnings("serial")
public class Static_BoxBehnken extends AbstractThreeLevelMethod {

	public Static_BoxBehnken() {
		super();
		this.PARAMETERS_ELEM = "BB_PARAMETERS";
		this.INFO_ELEM = "BoxBehnkenInfo";
	}

	public int getDesignSize() {
		int designSize = 1;
		if (designInfos.size() > 0) {
			designSize = designInfos.size();
			for (int i = 0; i < designInfos.size() - 1; i++) {
				designSize *= 2;
			}
		}
		designSize++; // centerpoint
		return designSize;
	}
	
	@Override
	public long getNumberOfRuns() {
		return getDesignSize() * rngSeedManipulator.getRunsMultiplierFactor();
	}

	public String getDescription() {
		return "The Box-Behnken design is an independent quadratic design in "
				+ "that it does not contain an embedded factorial or fractional "
				+ "factorial design. In this design the treatment combinations "
				+ "are at the midpoints of edges of the process space and at "
				+ "the center.\n"
				+ "These designs are rotatable (or near rotatable) and require "
				+ "3 levels of each factor. The designs have limited capability "
				+ "for orthogonal blocking compared to the central composite designs.\n"
				+ "This design is useful for response surface methodology (RSM).\n\n"
				+ "NIST/SEMATECH e-Handbook of Statistical Methods, http://www.itl.nist.gov/div898/handbook/, 2006\n\n";
	}

	public boolean isImplemented() {
		return true;
	}

	@Override
	protected String getLocalizedNameOfThis() {
		return "Box-Behnken";
	}

	@Override
	protected void createThisDesignInStandardMethodSpecificPart() {
		int size = designInfos.size();
		int factorialSize = size - 1;
		int actualRunIndex = 0;
		FactorialDesign fd = new FactorialDesign(factorialSize);
		for (int actualCenterIndex = 0; actualCenterIndex < size; actualCenterIndex++) {
			for (int i = 0; i < size; i++) {
				if (i < actualCenterIndex) {
					fillStandardColumn(actualRunIndex, i, fd.getColumn(i));
				} else if (i == actualCenterIndex) {
					Vector<Integer> centers = new Vector<Integer>();
					for (int j = 0; j < fd.getColumn(0).size(); j++) {
						centers.add(0);
					}
					fillStandardColumn(actualRunIndex, i, centers);
				} else if (i > actualCenterIndex) {
					fillStandardColumn(actualRunIndex, i, fd.getColumn(i - 1));
				}
			}
			actualRunIndex += fd.getColumn(0).size();
		}
		int centerPointIdx = getDesignSize() - 1;
		Vector<Integer> centerPointRun = new Vector<Integer>();
		centerPointRun.add(0);
		for (int i = 0; i < size; i++) {
			fillStandardColumn(centerPointIdx, i, centerPointRun);
		}
	}

	protected void fillStandardColumn(int actualRunIndex, int i,
			Vector<Integer> column) {
		for (int j = 0; j < column.size(); j++) {
			if (column.get(j) == -1) {
				standardPlugin.designTableModel.setValueInDesignAt(designInfos.get(i)
						.getLow(), actualRunIndex + j, i);
			} else if (column.get(j) == 1) {
				standardPlugin.designTableModel.setValueInDesignAt(designInfos.get(i)
						.getHigh(), actualRunIndex + j, i);
			} else {
				standardPlugin.designTableModel.setValueInDesignAt(designInfos.get(i)
						.getCenter(), actualRunIndex + j, i);
			}
		}
	}

	@Override
	public String getMethodDisplayName() {
		return "Box-Behnken";
	}
}
