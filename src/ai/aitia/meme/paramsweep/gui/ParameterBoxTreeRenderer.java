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
		final JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focus);
		label.setIcon(null);
		
		if (multiplicity > 0) {
			final JPanel panel = new JPanel(new BorderLayout());
			panel.setOpaque(false);
			
			panel.add(new JLabel("<html><b>" + multiplicity + "x</b>&nbsp;</html>"), BorderLayout.WEST);
			panel.add(label,BorderLayout.CENTER);
			panel.setSize(new Dimension(tree.getSize().width, tree.getRowHeight()));
			
			return panel;
		}
		
		return label;
	}
}