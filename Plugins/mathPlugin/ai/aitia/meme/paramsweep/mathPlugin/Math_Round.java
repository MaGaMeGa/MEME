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
package ai.aitia.meme.paramsweep.mathPlugin;

import java.util.Arrays;
import java.util.List;

import ai.aitia.meme.paramsweep.plugin.IStatisticsPlugin;

/** Statistic method decriptor plugin. This method returns the closest long to the
 *  argument. The result is rounded to an integer by adding 1/2, taking the floor of the
 *  result, and casting the result to type long. In other words, the result is equal to
 *  the value of the expression:<p>
 *  <em>(long)Math.floor(a + 0.5d)</em><p>
 *  Special cases:<br>
 *  <ul>
 *  <li>If the argument is NaN, the result is 0.</li>
 *  <li>If the argument is negative infinity or any value less than or equal to the value
 *  of Long.MIN_VALUE, the result is equal to the value of Long.MIN_VALUE.</li>
 *  <li>If the argument is positive infinity or any value greater than or equal to the
 *  value of Long.MAX_VALUE, the result is equal to the value of Long.MAX_VALUE.</li>
 *  </ul>
 *  <p>
 *  <b>Parameters:</b>
 *  <dd>value - a floating-point value to be rounded to a long.</dd>
 *  <p>
 *  <b>Returns:</b>
 *  <dd>the value of the argument rounded to the nearest long value.</dd>
 */
public class Math_Round implements IStatisticsPlugin {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public String getName() { return "round"; }
	public String getLocalizedName() { return "Round"; }
	public String getFullyQualifiedName() { return "Math.round"; }
	public int getNumberOfParameters() { return 1; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "value" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Double.TYPE }); }
	public Class getReturnType() { return Long.TYPE; }
	public String getDescription() { return "Returns the closest long to the argument."; }
	public List<String> getParameterDescriptions() { 
		return Arrays.asList(new String[] { "Value" });
	}
}
