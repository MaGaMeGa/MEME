/*******************************************************************************
 * Copyright (C) 2006-2014 AITIA International, Inc.
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
package ai.aitia.meme.paramsweep.gui.component;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

public class DefaultJButton extends JButton {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -3708017061765474773L;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public DefaultJButton() {}

	//----------------------------------------------------------------------------------------------------
	public DefaultJButton(final Icon icon) {
		super(icon);
	}

	//----------------------------------------------------------------------------------------------------
	public DefaultJButton(final String text) {
		super(text);
	}

	//----------------------------------------------------------------------------------------------------
	public DefaultJButton(final Action a) {
		super(a);
	}

	//----------------------------------------------------------------------------------------------------
	public DefaultJButton(final String text, final Icon icon) {
		super(text, icon);
	}
}