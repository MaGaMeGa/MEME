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

/** Statistic method decriptor plugin. This method returns the value of the first
 *  argument raised to the power of the second argument. Special cases:<br>
 *  <ul>
 *  <li>If the second argument is positive or negative zero, then the result is 1.0.</li>
 *  <li>If the second argument is 1.0, then the result is the same as the first argument.</li>
 *  <li>If the second argument is NaN, then the result is NaN.</li>
 *  <li>If the first argument is NaN and the second argument is nonzero, then the result is NaN.</li>
 *  <li>If<ul>
 *  <li>the absolute value of the first argument is greater than 1 and the second argument is positive infinity, or</li>
 *  <li>the absolute value of the first argument is less than 1 and the second argument is negative infinity,</li>
 *  </ul> 
 *  then the result is positive infinity.</li>
 *  <li>If<ul>
 *  <li>the absolute value of the first argument is greater than 1 and the second argument is negative infinity, or</li>
 *  <li>the absolute value of the first argument is less than 1 and the second argument is positive infinity,</li>
 *  </ul> 
 *  then the result is positive zero.</li>
 *  <li>If the absolute value of the first argument equals 1 and the second argument is infinite, then the result is NaN.</li>
 *  <li>If<ul>
 *  <li>the first argument is positive zero and the second argument is greater than zero, or</li>
 *  <li>the first argument is positive infinity and the second argument is less than zero,</li>
 *  </ul> 
 *  then the result is positive zero.</li>
 *  <li>If<ul>
 *  <li>the first argument is positive zero and the second argument is less than zero, or</li>
 *  <li>the first argument is positive infinity and the second argument is greater than zero,</li>
 *  </ul> 
 *  then the result is positive infinity.</li>
 *  <li>If<ul>
 *  <li>the first argument is negative zero and the second argument is greater than zero but not a finite odd integer, or</li>
 *  <li>the first argument is negative infinity and the second argument is less than zero but not a finite odd integer,</li>
 *  </ul> 
 *  then the result is positive zero.</li>
 *  <li>If<ul>
 *  <li>the first argument is negative zero and the second argument is a positive finite odd integer, or</li>
 *  <li>the first argument is negative infinity and the second argument is a negative finite odd integer,</li>
 *  </ul> 
 *  then the result is negative zero.</li>
 *  <li>If<ul>
 *  <li>the first argument is negative zero and the second argument is less than zero but not a finite odd integer, or</li>
 *  <li>the first argument is negative infinity and the second argument is greater than zero but not a finite odd integer,</li>
 *  </ul> 
 *  then the result is positive infinity.</li>
 *  <li>If<ul>
 *  <li>the first argument is negative zero and the second argument is a negative finite odd integer, or</li>
 *  <li>the first argument is negative infinity and the second argument is a positive finite odd integer,</li>
 *  </ul> 
 *  then the result is negative infinity.</li>
 *  <li>If the first argument is finite and less than zero<ul>
 *  <li>if the second argument is a finite even integer, the result is equal to the result of raising the absolute value of the first argument to the power of the second argument</li>
 *  <li>if the second argument is a finite odd integer, the result is equal to the negative of the result of raising the absolute value of the first argument to the power of the second argument</li>
 *  <li>if the second argument is finite and not an integer, then the result is NaN.</li></ul> 
 *  <li>If both arguments are integers, then the result is exactly equal to the mathematical result of raising the first argument to the power of the second argument if that result can in fact be represented exactly as a double value.</li>
 *  </ul>
 *  <p>
 *  (In the foregoing descriptions, a floating-point value is considered to be an integer if
 *  and only if it is finite and a fixed point of the method ceil or, equivalently, a
 *  fixed point of the method floor. A value is a fixed point of a one-argument method
 *  if and only if the result of applying the method to the value is equal to the value.)
 *  <p>
 *  <b>Parameters:</b>
 *  <dd>base - the base.</dd>
 *  <dd>exponent - the exponent.</dd>
 *  <p>
 *  <b>Returns:</b>
 *  <dd>the value base<sup>exponent</sup></dd>
 */
public class Math_Pow implements IStatisticsPlugin {

	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public String getName() { return "pow"; }
	public String getLocalizedName() { return "Power"; }
	public String getFullyQualifiedName() { return "Math.pow"; }
	public int getNumberOfParameters() { return 2; }
	public List<String> getParameterNames() { return Arrays.asList(new String[] { "base", "exponent" }); }
	public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Double.TYPE, Double.TYPE }); }
	public Class getReturnType() { return Double.TYPE; }
	public String getDescription() { return "Returns the value of the base raised to the power of the exponent."; }
	public List<String> getParameterDescriptions() { 
		return Arrays.asList(new String[] { "Base", "Exponent" });
	}
}
