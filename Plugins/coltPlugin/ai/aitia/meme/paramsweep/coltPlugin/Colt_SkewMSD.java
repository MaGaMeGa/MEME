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

public class Colt_SkewMSD implements IStatisticsPlugin {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public String getName() { return "skew"; }
	public String getLocalizedName() { return "Skew from mean and standard deviation"; }
	public String getFullyQualifiedName() { return "cern.jet.stat.Descriptive.skew"; }
	public int getNumberOfParameters() { return 3; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "data", "mean", "standardDeviation" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { DoubleArrayList.class, Double.TYPE, Double.TYPE }); }
	public Class getReturnType() { return Double.TYPE; }
	public String getDescription() { return "Computes the skew of a data sequence, which is moment(data,3,mean) / standardDeviation<sup>3</sup>."; }
	public List<String> getParameterDescriptions() { return Arrays.asList(new String[] { "Data sequence", "Mean", "Standard deviation" }); }
}
