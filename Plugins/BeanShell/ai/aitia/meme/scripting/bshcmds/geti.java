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

import ai.aitia.meme.viewmanager.ParameterSet.Category;

/**
 * Implements the <code>geti(String name)</code> command for BeanShell.
 */
public class geti
{
	//-------------------------------------------------------------------------
	public static Object invoke(Interpreter interp, CallStack callstack, String name) throws Exception
	{
		return get.doIt("geti", Category.INPUT, interp, name);
	}

}
