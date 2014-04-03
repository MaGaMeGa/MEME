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
package ai.aitia.meme.testImport;

import ai.aitia.meme.pluginmanager.IImportPlugin;
import ai.aitia.meme.pluginmanager.IImportPluginContext;
import ai.aitia.meme.pluginmanager.IPlugin;

public class TestImportPlugin implements IImportPlugin 
{
	//-------------------------------------------------------------------------
	public static TestImportPlugin onPluginLoad(IPlugin.IOnPluginLoad loadCtx) {
		//System.out.println(String.format("Loading TestImportPlugin... (context=%s)", loadCtx));
		return new TestImportPlugin();
	}

	//-------------------------------------------------------------------------
	public static void onPluginUnload(Object arg) {
		//System.out.println(String.format("Unloading TestImportPlugin... (context=%s)", arg));
	}

	//-------------------------------------------------------------------------
	public String getLocalizedName() {
		return "TestImportPlugin";
	}

	//-------------------------------------------------------------------------
	public java.awt.Window showDialog(IImportPluginContext app) {
		//app.setActionEnabled(false);
		System.out.print("TestImportPlugin.showDialog(): Running gc..");
		System.gc();
		System.out.println("..done");
		ai.aitia.meme.MEMEApp.userAlert("Hello, World!");
		//app.setActionEnabled(true);
		return null;
	}

}
