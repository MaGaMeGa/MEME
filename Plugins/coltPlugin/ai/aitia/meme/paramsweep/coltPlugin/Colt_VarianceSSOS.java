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

/** Statistic method decriptor plugin. This method returns the variance of a data
 *  sequence. That is (sumOfSquares - mean*sum) / size with mean = sum/size.
 *  <p>
 *  <b>Parameters:</b>
 *  <dd>size - the number of elements of the data sequence.</dd>
 *  <dd>sum - == Sum( data[i] ).</dd>
 *  <dd>sumOfSquares - == Sum( data[i]*data[i] ).</dd>
 */
public class Colt_VarianceSSOS extends Colt_Variance {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	@Override public String getLocalizedName() { return super.getLocalizedName() + " from sum and sum of squares";	}
	@Override public int getNumberOfParameters() { return 3; }
	@Override public List<String> getParameterNames() { return Arrays.asList(new String[] { "size", "sum", "sumOfSquares" }); }
	@Override public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Integer.TYPE, Double.TYPE, Double.TYPE }); }
	@Override public String getFullyQualifiedName() { return "cern.jet.stat.Descriptive.variance"; }
	@Override
	public String getDescription() {
		return "Computes the variance of a data sequence. That is (sumOfSquares - mean*sum) / size with mean = sum/size." +
			   "The data sequence is defined by its size, the sum of its elements and the sum of squares of its elements.";
	}
	@Override
	public List<String> getParameterDescriptions() {
		return Arrays.asList(new String[] { "Size of the data sequence", "Sum( data[i] )", "Sum( data[i]*data[i] )" });
	}
}
