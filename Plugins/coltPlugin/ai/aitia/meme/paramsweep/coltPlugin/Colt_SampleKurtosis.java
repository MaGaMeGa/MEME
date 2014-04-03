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

/** Statistic method decriptor plugin. This method returns the sample kurtosis (aka
 *  excess) of a data sequence.
 */
@Deprecated
public class Colt_SampleKurtosis extends Colt_Kurtosis {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	@Override public String getName() { return "sampleKurtosis"; }
	@Override public String getLocalizedName() { return "Sample kurtosis"; }
	@Override public String getFullyQualifiedName() { return "cern.jet.stat.Descriptive.sampleKurtosis"; }
	@Override public List<String> getParameterNames() { return Arrays.asList(new String[] { "data", "mean", "sampleVariance" }); }
	@Override public String getDescription() { return "Computes the sample kurtosis (aka excess} of a data sequence."; }
	@Override
	public List<String> getParameterDescriptions() {
		return Arrays.asList(new String[] { "Data sequence", "Mean", "Sample variance" });
	}
}
