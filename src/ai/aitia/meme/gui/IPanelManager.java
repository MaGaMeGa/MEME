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

import javax.swing.JPanel;

/** This interface describes the panel manager mechanism of MEME for external modules, 
 *  especially for the Parameter Sweep Module
 */
public interface IPanelManager {
	
	//---------------------------------------------------------------------------------------
	/** Adds a panel to the manager.
	 * @param panel the panel
	 * @param title the title of the panel
	 * @param closeable can the user close this panel or not
	 */
	public void add(JPanel panel, String title, boolean closeable);

	//---------------------------------------------------------------------------------------
	/** Removes a panel from the manager.
	 * @param panel the panel
	 */
	public void remove(JPanel panel);
	//---------------------------------------------------------------------------------------
	/** Rebuilds the GUI of the MEME from the informations of the panel manager. */
	public void rebuild();
	//---------------------------------------------------------------------------------------
	/** Sets the specified panel to active.
	 * @param panel the panel
	 */
	public void setActive(JPanel panel);
	//---------------------------------------------------------------------------------------
	/** Returns whether has a remote monitor (<code>RemoteMonitor</code> panel in the storage
	 *  of the panel manager.
	 */
	public boolean hasAliveMonitor();
	//---------------------------------------------------------------------------------------
	/** Sets the remote monitor panel to active. */
	public void setMonitorActive();
	
	public boolean hasAliveDownloader();
}
