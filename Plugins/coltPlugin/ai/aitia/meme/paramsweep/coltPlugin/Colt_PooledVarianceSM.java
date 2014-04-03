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

public class Colt_PooledVarianceSM implements IStatisticsPlugin {

	//=====================================================================================
	// methods

	//-------------------------------------------------------------------------------------
	public String getName() { return "pooledVariance"; }
	public String getLocalizedName() { return "Pooled variance from sizes and means"; }
	public String getFullyQualifiedName() { return "cern.jet.stat.Descriptive.pooledVariance"; }
	public int getNumberOfParameters() { return 4; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "size1", "variance1", "size2", "variance2" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Integer.TYPE, Double.TYPE, Integer.TYPE, Double.TYPE }); }
	public Class getReturnType() { return Double.TYPE; }
	public String getDescription() { return "Computes the pooled variance of two data sequences. That is (size1 * variance1 + size2 * variance2) / (size1 + size2)."; }
	public List<String> getParameterDescriptions() {
		return Arrays.asList(new String[] { "Size of the first data sequence", "Variance of the first data sequence",
											"Size of the second data sequence", "Variance of the second data sequence" });
	}
}
