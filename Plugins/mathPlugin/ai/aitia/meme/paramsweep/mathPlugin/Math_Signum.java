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

/** Statistic method decriptor plugin. This method returns the signum function of the
 *  argument; zero if the argument is zero, 1.0 if the argument is greater than zero,
 *  -1.0 if the argument is less than zero.<p>
 *  Special Cases:<br>
 *  <ul>
 *  <li>If the argument is NaN, then the result is NaN.</li>
 *  <li>If the argument is positive zero or negative zero, then the result is the same as the argument.</li> 
 *  <p>
 *  <b>Parameters:</b>
 *  <dd>value - the floating-point value whose signum is to be returned</dd>
 *  <p>
 *  <b>Returns:</b>
 *  <dd>the signum function of the argument</dd>
 */
public class Math_Signum implements IStatisticsPlugin {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public String getName() { return "signum"; }
	public String getLocalizedName() { return "Signum"; }
	public String getFullyQualifiedName() { return "Math.signum"; }
	public int getNumberOfParameters() { return 1; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "value" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Double.TYPE }); }
	public Class getReturnType() { return Double.TYPE; }
	public String getDescription() { 
		return "Returns the signum function of the argument; zero if the argument is zero, 1.0 if the argument is greater than zero, -1.0 if the argument is less than zero.";
	}
	public List<String> getParameterDescriptions() { 
		return Arrays.asList(new String[] { "Value" });
	}
}
