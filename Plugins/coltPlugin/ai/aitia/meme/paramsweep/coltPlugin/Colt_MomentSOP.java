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

/** Statistic method decriptor plugin. This method returns the moment of k-th order with
 *  constant c of a data sequence, which is Sum( (data[i]-c)<sup>k</sup> ) / size.
 *  <p>
 *  <b>Parameters:</b>
 *  <dd>sumOfPowers - sumOfPowers[m] == Sum( data[i]<sup>m</sup>) ) for m = 0,1,..,k ).
 *  In particular there must hold sumOfPowers.length == k+1.</dd>
 *  <dd>size - the number of elements of the data sequence.</dd>
 */
public class Colt_MomentSOP extends Colt_Moment {

	//=====================================================================================
	// methods

	//-------------------------------------------------------------------------------------
	@Override public String getLocalizedName() { return super.getLocalizedName() + " from sum of powers"; }
	@Override public int getNumberOfParameters() { return 4; }
	@Override
	public List<String> getParameterNames() {
		return Arrays.asList(new String[] { "k", "c", "size", "sumOfPowers" }); 
	}
	@Override
	public List<Class> getParameterTypes() {
		return Arrays.asList(new Class[] { Integer.TYPE, Double.TYPE, Integer.TYPE, double[].class });
	}
	@Override
	public String getDescription() {
		return "Computes the moment of k-th order with constant c of a data sequence, where sumOfPowers[m] == Sum( data[i]<sup>m</sup> ) for m = 0,1,..,k.";
	}
	@Override
	public List<String> getParameterDescriptions() {
		return Arrays.asList(new String[] { "Order", "Constant", "Size of the data sequence", "<html>sumOfPowers[m] == Sum( data[i]<sup>m</sup> ) for m = 0,1,..,k</html>" });
	}
}
