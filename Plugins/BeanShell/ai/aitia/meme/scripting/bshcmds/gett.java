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

import ai.aitia.meme.pluginmanager.IScriptLanguage.IContext;
import ai.aitia.meme.pluginmanager.IScriptLanguage.IRowInfo;
import ai.aitia.meme.pluginmanager.IScriptLanguage.IResultInfo;
import ai.aitia.meme.pluginmanager.IScriptLanguage.IViewInfo;
import ai.aitia.meme.scripting.BeanShellData;

/**
 * Implements the <code>gett(String name)</code> command for BeanShell.
 */
public class gett
{
	//-------------------------------------------------------------------------
	public static Object invoke(Interpreter interp, CallStack callstack, String name) throws Exception
	{
		String lname = (name == null) ? null : name.toLowerCase();
		IContext context = BeanShellData.getContext(interp);
		IRowInfo ri = context.getCurrentRowInfo();
		if ("$Batch$".equals(name) || "batch".equals(lname)) {
			if (ri instanceof IResultInfo)
				return ((IResultInfo)ri).getBatch();
			return null;
		}
		if ("$Run$".equals(name) || "run".equals(lname)) {
			if (ri instanceof IResultInfo)
				return ((IResultInfo)ri).getRun();
			return null;
		}
		if ("$Tick$".equals(name) || "tick".equals(lname)) {
			if (ri instanceof IResultInfo)
				return ((IResultInfo)ri).getTick();
			if (ri instanceof IViewInfo)
				return ((IViewInfo)ri).getRowID();
			return null;
		}
		if ("model".equals(lname)) {
			if (ri instanceof IResultInfo)
				return ((IResultInfo)ri).getModel().getName();
			return null;
		}
		if ("version".equals(lname)) {
			if (ri instanceof IResultInfo)
				return ((IResultInfo)ri).getModel().getVersion();
			return null;
		}
		if ("starttime".equals(lname)) {
			if (ri instanceof IResultInfo)
				return ((IResultInfo)ri).getStartTime();
			return null;
		}
		if ("endtime".equals(lname)) {
			if (ri instanceof IResultInfo)
				return ((IResultInfo)ri).getEndTime();
			return null;
		}
		if ("view".equals(lname)) {
			if (ri instanceof IViewInfo)
				return ((IViewInfo)ri).getView().getName();
			return null;
		}
		if ("isgroupfirst".equals(lname)) {
			return (ri.getFlags() & IRowInfo.GROUP_FIRST) != 0;
		}
		if ("isgrouplast".equals(lname)) {
			return (ri.getFlags() & IRowInfo.GROUP_LAST) != 0;
		}
		if ("isblockfirst".equals(lname)) {
			return (ri.getFlags() & IRowInfo.BLOCK_FIRST) != 0;
		}
		if ("isblocklast".equals(lname)) {
			return (ri.getFlags() & IRowInfo.BLOCK_LAST) != 0;
		}
		throw new Exception(String.format("gett(): unknown technical parameter \"%s\"", name));
	}

}
