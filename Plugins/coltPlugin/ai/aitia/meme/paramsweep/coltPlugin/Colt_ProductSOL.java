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

/** Statistic method decriptor plugin. This method returns the product, which is
 *  Prod( data[i] ). In other words: data[0]*data[1]*...*data[data.size()-1]. This
 *  method uses the equivalent definition: prod = pow( exp( Sum( Log(x[i]) ) / size,
 *  size()).
 */
public class Colt_ProductSOL extends Colt_Product {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	@Override public String getLocalizedName() { return super.getLocalizedName() + " from sum of logarithms"; }
	@Override public int getNumberOfParameters() { return 2; }
	@Override public List<String> getParameterNames() { return Arrays.asList(new String[] { "size", "sumOfLogarithms" }); }
	@Override public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Integer.TYPE, Double.TYPE }); }
	@Override
	public String getDescription() {
		return "Computes the product of a data sequence. The data sequence is defined by its size and the sum of its logarithmic transformed elements.";
	}
	@Override
	public List<String> getParameterDescriptions() {
		return Arrays.asList(new String[] { "Size of the data sequence", "Sum of the logarithmic transformed elements of the data sequence" }); 
	}
}
