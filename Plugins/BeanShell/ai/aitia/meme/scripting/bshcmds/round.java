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
package ai.aitia.meme.scripting.bshcmds;

import bsh.CallStack;
import bsh.Interpreter;

public class round {
	
	//----------------------------------------------------------------------------------------------------
	public static Object invoke(Interpreter interp, CallStack callstack, double num) throws Exception {
		return invoke(interp,callstack,num,0);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static Object invoke(Interpreter interp, CallStack callstack, double num, int precision) throws Exception {
		if (precision < 0)
			throw new Exception("round: precision is negative");
		if (precision == 0)
			return (double) Math.round(num);
					
		String toRound = String.valueOf( num + " " );
		final char sep = '.';
		String mantissa = toRound.substring(toRound.indexOf(sep) + 1);
		mantissa = mantissa.trim();
		for (int i = mantissa.length();i < precision + 1;++i )
			mantissa += "0";
		int afterLastVal = Integer.valueOf(mantissa.substring(precision,precision + 1));
		int lastVal = Integer.valueOf(mantissa.substring(precision - 1,precision));
		if (afterLastVal > 4)
			++lastVal;
		mantissa = mantissa.substring(0,precision - 1) + lastVal;
		toRound = toRound.substring(0,toRound.indexOf(sep) + 1) + mantissa;
		return Double.valueOf(toRound);
	}
}
