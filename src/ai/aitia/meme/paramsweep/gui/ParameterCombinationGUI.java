package ai.aitia.meme.paramsweep.gui;

import java.awt.Image;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

//----------------------------------------------------------------------------------------------------
class ParameterCombinationGUI {
	
	//====================================================================================================
	// members
	
	private static final ImageIcon WARNING = new ImageIcon(new ImageIcon(Page_ParametersV2.class.getResource("icons/warning.png")).getImage().
			 										getScaledInstance(Page_ParametersV2.ICON_WIDTH_AND_HEIGHT,Page_ParametersV2.ICON_WIDTH_AND_HEIGHT,Image.SCALE_SMOOTH));


	final JTree tree; 
	final DefaultMutableTreeNode combinationRoot; 
	final JLabel runDisplay;
	final JLabel warningDisplay;
	
	final JButton addButton;
	final JButton removeButton;
	final JButton upButton;
	final JButton downButton;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public ParameterCombinationGUI(final JTree tree, final DefaultMutableTreeNode combinationRoot, final JLabel runDisplay, final JLabel warningDisplay,
								   final JButton addButton, final JButton removeButton, final JButton upButton, final JButton downButton) {
		this.tree = tree;
		this.combinationRoot = combinationRoot;
		this.runDisplay = runDisplay;
		this.warningDisplay = warningDisplay;
		
		this.addButton = addButton;
		this.removeButton = removeButton;
		this.upButton = upButton;
		this.downButton = downButton;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void updateRunDisplay(final int runNumber) {
		String text = "<html><b>Number of runs:</b> ";
		text += String.valueOf(runNumber > 0 ? runNumber : 0);
		text += "</html>";
		
		runDisplay.setText(text);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setWarning(final boolean enabled) {
		Icon icon = null;
		String tooltip = null;
		if (enabled) {
			icon = WARNING;
			tooltip = "<html>There are two or more combination boxes that contains non-constant parameters.<br>"
					+ "Simulation will exit before all parameter values are assigned.</html>";
		} 
		
		warningDisplay.setIcon(icon);
		warningDisplay.setToolTipText(tooltip);
	}
}