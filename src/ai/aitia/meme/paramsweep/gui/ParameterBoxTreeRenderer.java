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
package ai.aitia.meme.paramsweep.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import ai.aitia.meme.paramsweep.gui.info.ParameterInATree;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.internal.platform.IGUIController.RunOption;

//----------------------------------------------------------------------------------------------------
@SuppressWarnings("serial")
public class ParameterBoxTreeRenderer extends DefaultTreeCellRenderer {
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public Component getTreeCellRendererComponent(final JTree tree, final Object value,	final boolean sel, final boolean expanded, final boolean leaf, final int row,
												  final boolean focus) {
		String tooltipText = null;
		
		final DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		
		if (node.isRoot()) {
			return new JLabel("<html><b>All combinations of:</b></html>");
		}
		
		final ParameterInATree userObj = (ParameterInATree) node.getUserObject();
		
		long multiplicity = userObj.info.getMultiplicity();
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL)
			multiplicity *= userObj.info.getRuns();

		if (userObj.info.isFile()) {
			final File file = (File) userObj.info.getValue();
			if (file != null)
				tooltipText = file.getAbsolutePath();
		}
		
		setToolTipText(tooltipText);
		
		if (multiplicity > 0) {
			final JPanel panel = new JPanel(new BorderLayout());
			
			panel.add(new JLabel("<html><b>" + multiplicity + "x</b>&nbsp;</html>"), BorderLayout.WEST);
			panel.add(new JLabel(value.toString()),BorderLayout.CENTER);
			panel.setSize(new Dimension(tree.getSize().width, tree.getRowHeight()));
			panel.setOpaque(true);
			
			return panel;
		}
		
		final JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focus);
		label.setIcon(null);
		
		return label;
	}
}