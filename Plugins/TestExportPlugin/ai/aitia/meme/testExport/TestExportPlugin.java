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
package ai.aitia.meme.testExport;

import ai.aitia.meme.pluginmanager.IExportPlugin;
import ai.aitia.meme.pluginmanager.IExportPluginContext;

public class TestExportPlugin implements IExportPlugin
{
	//-------------------------------------------------------------------------
	public String getLocalizedName() {
		return "TestExportPlugin";
	}

	//-------------------------------------------------------------------------
	public java.awt.Window showDialog(IExportPluginContext ctx) {
		//app.setActionEnabled(false);
		System.out.println("Selection: " + java.util.Arrays.deepToString( ctx.getSelection().toArray() ));

		ai.aitia.meme.MEMEApp.userAlert("Hello, World!");

		//app.setActionEnabled(false);
		return null;
	}

}
