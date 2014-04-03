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
package ai.aitia.meme.viewmanager;

import ai.aitia.meme.gui.MainWindow;

/** Enum type for representing the source of the different parameters. */
public enum SrcType 
{
	PROJECTION_INPUT, PROJECTION_OUTPUT, PROJECTION_VIEW, SCALAR_SCRIPT, GROUP_SCRIPT;

	// TODO: ird le ide hogy ezek mire kellenek?
	public boolean isInput()		{ return this == PROJECTION_INPUT; }
	
	// Pl. ViewCreationRule.Column.getInitialDataType() hasznalja az alapertelmezett
	// tipus kiszamitasakor. Tovabba JTableScrollPane table cell renderer is hasznalja.
	public boolean isProjection()	{ return this.ordinal() <= PROJECTION_VIEW.ordinal(); }
	
	public javax.swing.ImageIcon getIcon() {
		if (icons == null) {
			icons = new javax.swing.ImageIcon[values().length];
			icons[PROJECTION_INPUT.ordinal()]	= MainWindow.getIcon("type_parameter.png");
			icons[SCALAR_SCRIPT.ordinal()]		= MainWindow.getIcon("type_beanshell.png");
			icons[PROJECTION_OUTPUT.ordinal()]	= icons[PROJECTION_INPUT.ordinal()];	
			icons[PROJECTION_VIEW.ordinal()]	= icons[PROJECTION_INPUT.ordinal()];	
			icons[GROUP_SCRIPT.ordinal()]		= icons[SCALAR_SCRIPT.ordinal()];	
		}
		return icons[ordinal()];
	}
	protected static javax.swing.ImageIcon icons[] = null;
};
