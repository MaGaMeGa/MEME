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

/** Statistic method decriptor plugin. This method returns the kurtosis (aka excess) of a
 *  data sequence.
 *  <p>
 *  <b>Parameters:</b>
 *  <dd>moment4 - the fourth central moment, which is moment(data,4,mean).</dd>
 *  <dd>standardDeviation - the standardDeviation.</dd>
 */
public class Colt_KurtosisFCM extends Colt_Kurtosis {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	@Override public String getLocalizedName() { return super.getLocalizedName() + " from fourth central moment"; }
	@Override public int getNumberOfParameters() { return 2; }
	@Override public List<String> getParameterNames() { return Arrays.asList(new String[] { "moment4", "standardDeviation" }); }
	@Override public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Double.TYPE, Double.TYPE }); }
	@Override public String getFullyQualifiedName() { return "cern.jet.stat.Descriptive.kurtosis"; }
	@Override
	public String getDescription() {
		return "Returns the kurtosis (aka excess) of a data sequence. The data sequence is defined by its fourth central moment.";
	}
	@Override
	public List<String> getParameterDescriptions() {
		return Arrays.asList(new String[] { "The fourth central moment", "Standard deviation" });
	}
}
