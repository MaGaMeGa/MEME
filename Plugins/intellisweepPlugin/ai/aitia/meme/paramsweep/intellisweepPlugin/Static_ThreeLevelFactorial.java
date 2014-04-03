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

import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.AbstractThreeLevelMethod;


@SuppressWarnings("serial")
public class Static_ThreeLevelFactorial extends AbstractThreeLevelMethod{

	public Static_ThreeLevelFactorial() {
		super();
		this.PARAMETERS_ELEM = "TLF_PARAMETERS";
		this.INFO_ELEM = "ThreeLevelFactorialInfo";
	}

	@Override
	protected void createThisDesignInStandardMethodSpecificPart() {
		int designSize = getDesignSize();
		for (int i = 0; i < designSize; i++) {
			String numStr = Integer.toString(i, 3);
			int originalLength = numStr.length();
			for (int j = originalLength; j < designInfos.size(); j++) {
				numStr = "0" + numStr;
			}
			for (int j = 0; j < numStr.length(); j++) {
				char c = numStr.charAt(j);
				Object toSet = null;
				switch (c) {
				case '0':
					toSet = designInfos.get(j).getLow();
					break;

				case '1':
					toSet = designInfos.get(j).getCenter();
					break;

				case '2':
					toSet = designInfos.get(j).getHigh();
					break;

				default:
					break;
				}
				standardPlugin.designTableModel.setValueInDesignAt(toSet, i, j);
			}
		}
	}

	public int getDesignSize() {
		int designSize = 1;
		if (designInfos.size() > 0) {
			for (int i = 0; i < designInfos.size(); i++) {
				designSize *= 3;
			}
		}
		return designSize;
	}

	@Override
	public long getNumberOfRuns() {
		return getDesignSize() * rngSeedManipulator.getRunsMultiplierFactor();
	}

	@Override
	protected String getLocalizedNameOfThis() {
		return "Three level factorial";
	}

	@Override
	public String getMethodDisplayName() {
		return "Three level factorial";
	}

	public String getDescription() {
		return "The Three Level Factorial Design method. It uses three levels of " +
				"each parameter and runs a simulation for every possible combination " +
				"of them.";
	}

	public boolean isImplemented() {
		return true;
	}

}
