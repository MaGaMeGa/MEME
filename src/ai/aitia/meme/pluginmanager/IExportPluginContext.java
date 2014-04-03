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
package ai.aitia.meme.pluginmanager;

import java.util.ArrayList;

import ai.aitia.meme.database.AbstractResultsDb;
import ai.aitia.meme.database.ViewsDb;

//-----------------------------------------------------------------------------
/** 
 * Type for arguments of IExportPlugin methods.
 */
public interface IExportPluginContext extends IDialogPluginContext
{
	/** Returns the results database object. */
	public AbstractResultsDb	getResultsDb();
	/** Returns the views database object. */
	public ViewsDb				getViewsDb();

	/**
	 * Returns a list containing all currently selected results 
	 * (Long[] arrays: {model_id, batch#, run#})
	 * and the selected view(s) (ViewRec objects: {view name, view_id}).
	 */
	public ArrayList<Object>	getSelection();
	
	/** 
	 * Returns the id of the active panel (Results panel or View panel).
	 * @return id of the active panel 
	 * @see RESULTS
	 * @see VIEWS
	 */
	public int getActive();
	
	public static int RESULTS = 0;
	public static int VIEWS = 1;
}
