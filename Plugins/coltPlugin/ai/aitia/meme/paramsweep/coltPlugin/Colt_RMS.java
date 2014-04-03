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

import ai.aitia.meme.paramsweep.colt.DoubleArrayList;
import ai.aitia.meme.paramsweep.plugin.IStatisticsPlugin;

/** Statistic method decriptor plugin. This method returns the RMS (Root-Mean-Square) of
 *  a data sequence. That is Math.sqrt(Sum( data[i]*data[i] ) / data.size()). The RMS of
 *  data sequence is the square-root of the mean of the squares of the elements in the
 *  data sequence. It is a measure of the average "size" of the elements of a data
 *  sequence.
 */
public class Colt_RMS implements IStatisticsPlugin {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public String getName() { return "rms"; }
	public String getLocalizedName() { return "RMS"; }
	public String getFullyQualifiedName() { return "ai.aitia.meme.paramsweep.colt.Descriptive.rms"; }
	public int getNumberOfParameters() { return 1; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "data" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { DoubleArrayList.class }); }
	public Class getReturnType() { return Double.TYPE; }
	public List<String> getParameterDescriptions() { return Arrays.asList(new String[] { "The data sequence" }); }
	public String getDescription() {
		return "Returns the RMS (Root-Mean-Square) of a data sequence. The RMS of data sequence" +
			   " is the square-root of the mean of the squares of the elements in the data" +
			   " sequence. It is a measure of the average \"size\" of the elements of a data" +
			   " sequence.";
	}
}
