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

import java.util.ArrayList;

import bsh.CallStack;
import bsh.Interpreter;

import ai.aitia.meme.pluginmanager.IScriptLanguage.IContext;
import ai.aitia.meme.scripting.BeanShellData;

/**
 * Implements the <code>call(String func, Object... args)</code> command for BeanShell.
 */
public class call
{
	//-------------------------------------------------------------------------
	public static Object invoke(Interpreter interp, CallStack callstack, String func, Object[] args) 
			throws bsh.EvalError, NoSuchMethodException
	{
		IContext context = BeanShellData.getContext(interp);
		ArrayList<Object[]> tmp = new ArrayList<Object[]>(args.length);
		for (Object o : args) {
			if (!(o instanceof Object[]))
				o = new Object[] { o };
			tmp.add((Object[])o);
		}
		return context.callAggrFn(func, tmp);
	}

	//-------------------------------------------------------------------------
	public static Object invoke(Interpreter interp, CallStack callstack, String func, 
									Object a0
								) throws bsh.EvalError, NoSuchMethodException {
		return invoke(interp, callstack, func, new Object[] { a0 });
	}
	public static Object invoke(Interpreter interp, CallStack callstack, String func, 
									Object a0, Object a1
								) throws bsh.EvalError, NoSuchMethodException {
		return invoke(interp, callstack, func, new Object[] { a0, a1 });
	}
	public static Object invoke(Interpreter interp, CallStack callstack, String func, 
									Object a0, Object a1, Object a2
								) throws bsh.EvalError, NoSuchMethodException {
		return invoke(interp, callstack, func, new Object[] { a0, a1, a2 });
	}
	public static Object invoke(Interpreter interp, CallStack callstack, String func, 
									Object a0, Object a1, Object a2, Object a3
								) throws bsh.EvalError, NoSuchMethodException {
		return invoke(interp, callstack, func, new Object[] { a0, a1, a2, a3 });
	}
	public static Object invoke(Interpreter interp, CallStack callstack, String func, 
									Object a0, Object a1, Object a2, Object a3,
									Object a4
								) throws bsh.EvalError, NoSuchMethodException {
		return invoke(interp, callstack, func, new Object[] { a0, a1, a2, a3, a4 });
	}
	public static Object invoke(Interpreter interp, CallStack callstack, String func, 
									Object a0, Object a1, Object a2, Object a3,
									Object a4, Object a5, Object a6
								) throws bsh.EvalError, NoSuchMethodException {
		return invoke(interp, callstack, func, new Object[] { a0, a1, a2, a3, a4, a5, a6 });
	}
	public static Object invoke(Interpreter interp, CallStack callstack, String func, 
									Object a0, Object a1, Object a2, Object a3,
									Object a4, Object a5, Object a6, Object a7
								) throws bsh.EvalError, NoSuchMethodException {
		return invoke(interp, callstack, func, new Object[] { a0, a1, a2, a3, a4, a5, a6, a7 });
	}
	public static Object invoke(Interpreter interp, CallStack callstack, String func, 
									Object a0, Object a1, Object a2, Object a3,
									Object a4, Object a5, Object a6, Object a7,
									Object a8
								) throws bsh.EvalError, NoSuchMethodException {
		return invoke(interp, callstack, func, new Object[] { a0, a1, a2, a3, a4, a5, a6, a7, a8 });
	}
	public static Object invoke(Interpreter interp, CallStack callstack, String func, 
									Object a0, Object a1, Object a2, Object a3,
									Object a4, Object a5, Object a6, Object a7,
									Object a8, Object a9
								) throws bsh.EvalError, NoSuchMethodException {
		return invoke(interp, callstack, func, new Object[] { a0, a1, a2, a3, a4, a5, a6, a7, a8, a9 });
	}


}
