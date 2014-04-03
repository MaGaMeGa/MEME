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

import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import ai.aitia.meme.gui.Preferences.Button;
import ai.aitia.meme.gui.Preferences.IPreferencesPage;

/** Convenience class that implements the IPreferencesPage interface. Users that want
 *  to use the Preferences component can derived their page classes from this class
 *  instead of implementing directly the interface because this class has already
 *  dealed with the storage of the subpages. 
 */
public class PreferencesPage extends JPanel implements IPreferencesPage {
	
	//=====================================================================================
	// members
	
	private static final long serialVersionUID = 1L;
	/** Title of the page. */
	protected String title = null;
	/** The subpages of the page. */
	protected List<IPreferencesPage> subPages = null;
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Constructor. */
	public PreferencesPage() { this(""); }
	
	//-------------------------------------------------------------------------------------
	/** Constructor.
	 * @param title title of the page 
	 */
	public PreferencesPage(String title) {
		super();
		if (title == null)
			throw new IllegalArgumentException("'title' is null");
		this.title = title;
		this.subPages = new ArrayList<IPreferencesPage>();
	}
	
	//-------------------------------------------------------------------------------------
	@Override public String toString() { return title; }
	
	//=====================================================================================
	// implemented interfaces

	//-------------------------------------------------------------------------------------
	public String getTitle(Preferences p) { return title; }
	public String getInfoText(Preferences p) { return ""; }
	public Container getPanel() { return this; }
	public List<IPreferencesPage> getSubpages(Preferences p) { return subPages; }
	public boolean isEnabled(Button b) { return true; }
	public boolean onButtonPress(Button b) { return true; }
	public void onPageChange(boolean show) {}
}
