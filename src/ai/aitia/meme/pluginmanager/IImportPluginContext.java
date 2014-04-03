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

import ai.aitia.meme.database.AbstractResultsDb;
import ai.aitia.meme.database.Model;

//-----------------------------------------------------------------------------
/** 
 * Type for arguments of IImportPlugin methods.
 */
public interface IImportPluginContext extends IDialogPluginContext
{
	/** Returns the results database object. */
	public AbstractResultsDb	getResultsDb(); // bug fix #1478
	
	/** Returns the selected models in the results tree. */
	public Model[]				getSelectedModels();
}
