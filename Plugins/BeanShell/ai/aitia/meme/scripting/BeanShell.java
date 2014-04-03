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
package ai.aitia.meme.scripting;

import org.apache.bsf.BSFException;

import ai.aitia.meme.pluginmanager.IScriptLanguage;

//-------------------------------------------------------------------------
/** This class represents the BeanShell scripting language. */
public class BeanShell implements IScriptLanguage
{
	private final String DATA = "BeanShellData";

	//=========================================================================
	//	Interface methods

	//-------------------------------------------------------------------------
	public String getLocalizedName() {
		return "beanshell";			// BSF-standard name of the language
	}

	//-------------------------------------------------------------------------
	public void createInterp(IContext context) throws Exception {
		context.put(DATA, new BeanShellData().init(context));
	}

	//-------------------------------------------------------------------------
	public void disposeInterp(IContext context) {
		BeanShellData data = (BeanShellData)context.remove(DATA);
		if (data != null)
			data.dispose();
	}

	//-------------------------------------------------------------------------
	public Object eval(IContext context, String script) throws BSFException {
		return ((BeanShellData)context.get(DATA)).eval(context, script);
	}

	//=========================================================================
	//	Internals


	//-------------------------------------------------------------------------
//	public static BeanShell onPluginLoad(IOnPluginLoad loadCtx) throws BSFException {
//		//System.out.println(String.format("Loading TestImportPlugin... (context=%s)", loadCtx));
//		return new BeanShell();
//	}

	//-------------------------------------------------------------------------
//	public static void onPluginUnload(Object arg) {
//		//System.out.println(String.format("Unloading TestImportPlugin... (context=%s)", arg));
//	}

}
