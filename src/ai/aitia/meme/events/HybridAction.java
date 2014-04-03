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
package ai.aitia.meme.events;

import java.awt.event.ActionEvent;

import javax.swing.JToolBar;

import ai.aitia.meme.gui.MainWindow;

//-----------------------------------------------------------------------------
/**
 * It is a special Action that forwards the actionPerformed() message to an 
 * IHybridActionListener host object - this is why this class is called as "hybrid".
 * The host object serves as a common place for handling somewhat-related 
 * actionPerformed() messages.
 * Furthermore, the constructor of this class accepts variable number of arguments 
 * to allow easy configuration of this Action.  
 */
public class HybridAction extends javax.swing.AbstractAction
{
	private static final long serialVersionUID = 1L;

	/** Host object - a common place for handling somewhat-related actionPerformed() messages. */
	protected final IHybridActionListener host;

	/** Constructor.
	 * 
	 * @param host host object
	 * @param name name of the action
	 * @param icon small icon of the action
	 * @param args key-value parameter pairs for further parameters
	 */
	public HybridAction(IHybridActionListener host, String name, String icon, Object... args) {
		this.host = host;
		putValue(NAME, name);
		if (icon != null)
			putValue(SMALL_ICON, MainWindow.getIcon(icon));

		for (int i = 1; i < args.length; i += 2)
			putValue(args[i-1].toString(), args[i]);

		if (null == getValue(SHORT_DESCRIPTION))
			putValue(SHORT_DESCRIPTION, name);
	}

	/** Calls the host.hybridAction() method with arguments 'e' and 'this'. */
	public void actionPerformed(ActionEvent e) {
		host.hybridAction(e, this);
	}
	
	public String getName() {
		return getValue(NAME).toString();
	}
}
