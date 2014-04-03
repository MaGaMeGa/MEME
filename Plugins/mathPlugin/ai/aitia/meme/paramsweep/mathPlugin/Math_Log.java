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

/** Statistic method decriptor plugin. This method returns the natural logarithm (base e)
 *  of a double value. Special cases:<br>
 *	<ul>
 *  <li>If the argument is NaN or less than zero, then the result is NaN.</li>
 *  <li>If the argument is positive infinity, then the result is positive infinity.</li>
 *  <li>If the argument is positive zero or negative zero, then the result is negative infinity.</li>
 *  </ul>
 *  <p>
 *  <b>Parameters:</b>
 *  <dd>value - a value</dd>
 *  <p>
 *  <b>Returns:</b>
 *  <dd>the value ln value, the natural logarithm of value.</dd>
 */
public class Math_Log implements IStatisticsPlugin {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public String getName() { return "log"; }
	public String getLocalizedName() { return "Logarithm (natural)"; }
	public String getFullyQualifiedName() { return "Math.log"; }
	public int getNumberOfParameters() { return 1; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "value" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Double.TYPE }); }
	public Class getReturnType() { return Double.TYPE; }
	public String getDescription() { return "Returns the natural logarithm (base e) of a value."; }
	public List<String> getParameterDescriptions() { 
		return Arrays.asList(new String[] { "Value" });
	}
}
