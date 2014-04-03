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
 *  excess) of a data sequence. Ref: R.R. Sokal, F.J. Rohlf, Biometry: the principles
 *  and practice of statistics in biological research (W.H. Freeman and Company, New
 *  York, 1998, 3rd edition) p. 114-115.
 *  <p>
 *  <b>Parameters:</b>
 *  <dd>size - the number of elements of the data sequence.</dd>
 *  <dd>moment4 - the fourth central moment, which is moment(data,4,mean).</dd>
 *  <dd>sampleVariance - the sample variance.</dd>
 */
@Deprecated
public class Colt_SampleKurtosisFCM extends Colt_SampleKurtosis {
	
	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	@Override public String getLocalizedName() { return super.getLocalizedName() + " from fourth central moment"; }
	@Override public List<String> getParameterNames() { return Arrays.asList(new String[] { "size", "moment4", "sampleVariance" }); }
	@Override public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Integer.TYPE, Double.TYPE, Double.TYPE }); }
	@Override
	public String getDescription() {
		return "Returns the sample kurtosis (aka excess) of a data sequence. The data sequence is defined by its size and its fourth central moment.";
	}
	@Override
	public List<String> getParameterDescriptions() {
		return Arrays.asList(new String[] { "Size of the data sequence", "The fourth central moment", "Sample variance" });
	}
}
