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
package ai.aitia.meme.paramsweep.coltPlugin;

import java.util.Arrays;
import java.util.List;

import ai.aitia.meme.paramsweep.plugin.IStatisticsPlugin;

public class Colt_RMSSSOQ implements IStatisticsPlugin {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public String getName() { return "rms"; }
	public String getLocalizedName() { return "RMS from size and sum of squares"; }
	public String getFullyQualifiedName() { return "cern.jet.stat.Descriptive.rms"; }
	public int getNumberOfParameters() { return 2; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "size", "sumOfSquares" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Integer.TYPE, Double.TYPE }); }
	public Class getReturnType() { return Double.TYPE; }
	public String getDescription() {
		return "Returns the RMS (Root-Mean-Square) of a data sequence. The RMS of data sequence" +
			   " is the square-root of the mean of the squares of the elements in the data" +
			   " sequence. It is a measure of the average \"size\" of the elements of a data" +
			   " sequence. The data sequence is defined by its size and the sum of squares of" +
			   " its elements.";
	}
	public List<String> getParameterDescriptions() {
		return Arrays.asList(new String[] { "Size of the data sequence", "Sum( data[i]*data[i] )" });
	}
}
