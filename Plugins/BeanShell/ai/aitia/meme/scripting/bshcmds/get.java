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

import ai.aitia.meme.scripting.BeanShellData;
import ai.aitia.meme.viewmanager.ParameterSet.Category;
import ai.aitia.meme.pluginmanager.Parameter;
import ai.aitia.meme.pluginmanager.IScriptLanguage.IContext;

/**
 * Implements the <code>get(String name)</code> command for BeanShell.
 */
public class get
{
	//-------------------------------------------------------------------------
	public static Object invoke(Interpreter interp, CallStack callstack, String name) throws Exception
	{
		return doIt("get", null, interp, name);
	}

	//-------------------------------------------------------------------------
	static Object doIt(String cmd, Category c, Interpreter interp, String name) throws Exception {
		if (name == null)
			throw new Exception(cmd + ": parameter name is null");

		IContext context = BeanShellData.getContext(interp);

		java.util.List<? extends Parameter>	 allPars = context.getAllPars();
		boolean found = false;
		int i = allPars.size();

		if (c == null) {
			java.util.BitSet visible = context.getVisibleParIndices();
			while (!found && --i >= 0) {
				if (!visible.get(i)) continue;
				Parameter p = allPars.get(i);
				// Kihasznaljuk, hogy a visible-k kozott nincs ket egyforma nevu
				found = name.equals(p.getName());	
			}
		} else {
			while (!found && --i >= 0) {
				Parameter p = allPars.get(i);
				if (context.getCategory(p) != c) continue;
				found = name.equals(p.getName());
			}
		}
		if (!found) {
			throw new Exception(String.format(cmd + ": unknown parameter \"%s\"", name));
		}
		Object[] val = context.getAllValues().get(i);
		if (context.isSingleValue(i))
			return val[0];
		return val;
	}

}
