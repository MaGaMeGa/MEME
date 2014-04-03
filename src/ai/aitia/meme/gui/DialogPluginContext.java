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
package ai.aitia.meme.gui;

import java.util.ArrayList;

import javax.swing.JPanel;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.AbstractResultsDb;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ViewRec;
import ai.aitia.meme.database.ViewsDb;
import ai.aitia.meme.pluginmanager.IExportPluginContext;
import ai.aitia.meme.pluginmanager.IImportPluginContext;
import ai.aitia.meme.pluginmanager.impl.PluginContextBase;

/** We use this class to give parameters to dialog plugins. It implements both
 *  the interfaces for import and export plugins. */
 /* Dialog pluginek hivasakor atadott objektum. Azert implementalja egyszerre
 * az import es export pluginek altal megkivant interfeszt is, mert igy nem 
 * kellett kulonbontani a ket esetet {@link MainWindow#hybridAction}-ben. 
 */
@SuppressWarnings("serial")
public class DialogPluginContext extends PluginContextBase implements IImportPluginContext,
																		 IExportPluginContext 
{
	//=========================================================================
	// Member variables

	/** Refers to the action that launched this plugin. */
	// ld. a TODOt alabb setActionEnabled()-nel 
	javax.swing.Action action = null;

	//=========================================================================
	// Public methods

	public DialogPluginContext(javax.swing.Action action)	{ this.action = action; }


	//=========================================================================
	// IDialogPluginContext methods

	/**
	 * Returns the main window of the application. Use this as parent for
	 * modal dialogs.
	 */
	public java.awt.Frame getAppWindow() {
		return MEMEApp.getMainWindow().getJFrame();
	}

	public boolean isActionEnabled() {
		return (action != null) && action.isEnabled();
	}

	/**
	 * Disables or enables all GUI items that can launch this plugin.
	 */
	/* TODO: lehetne csinalni egy programstate parametert amit megcsongetunk dialogpluginek
	 * inditasakor es leallasakor. Erre minden GUI reszleg raakaszthatna controllereket azon
	 * GUI-elemek letiltasara/engedelyezesere amelyek dialogplugint inditanak. Ekkor a 
	 * GUI-elemek engedelyezesenek vezerlese automatikus lehetne a programbol es a pluginekben
	 * nem kellene tenni semmit sem a letiltashoz.
	 */
	public void setActionEnabled(boolean enable) {
		assert(action != null);
		action.setEnabled(enable);
	}


	//=========================================================================
	// IImportPluginContext methods

	public Model[] getSelectedModels() {
		ai.aitia.meme.gui.ResultsBrowser br = MEMEApp.getMainWindow().getResultsBrowser();
		return (br == null) ? null : br.getSelectedModels();
	}


	//=========================================================================
	// IExportPluginContext methods

	public AbstractResultsDb	getResultsDb()		{ return MEMEApp.getResultsDb(); }	
	public ViewsDb				getViewsDb()		{ return MEMEApp.getViewsDb(); }

	/**
	 * Returns a list containing all currently selected results 
	 * (Long[] arrays: {model_id, batch#, run#})
	 * and the selected view(s) (ViewRec objects: {view name, view_id}).
	 */
	public ArrayList<Object> getSelection() {
		ArrayList<Object> ans = new ArrayList<Object>();
		ans.addAll(java.util.Arrays.asList(MEMEApp.getMainWindow().getResultsBrowser().getSelection()));
		ViewRec[] rec = MEMEApp.getMainWindow().getViewsPanel().getSelectedViews();
		if (rec != null)
			ans.addAll(java.util.Arrays.asList(rec));
		return ans;
	}
	
	/** 
	 * Returns the id of the active panel (Results panel or View panel).
	 * @return id of the active panel 
	 * @see RESULTS
	 * @see VIEWS
	 */
	public int getActive() {
		JPanel activePanel = MEMEApp.getMainWindow().activePanel.getValue();
		if (activePanel == MEMEApp.getMainWindow().getViewsPanel())	return IExportPluginContext.VIEWS;
		return IExportPluginContext.RESULTS;
	}
}
