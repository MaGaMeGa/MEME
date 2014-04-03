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
package ai.aitia.meme.paramsweep.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javassist.ClassPath;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ai.aitia.meme.gui.Wizard;
import ai.aitia.meme.gui.Wizard.Button;
import ai.aitia.meme.gui.Wizard.IArrowsInHeader;
import ai.aitia.meme.gui.Wizard.IWizardPage;
import ai.aitia.meme.paramsweep.PS_ModelWizard;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.Platform;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.utils.ClassPathPair;
import ai.aitia.meme.paramsweep.utils.SortedListModel;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;

public class Page_ChoosePlatform extends JPanel implements IWizardPage,
														   IArrowsInHeader,
														   ActionListener,
														   ListSelectionListener {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 1L;
	
	/** The owner of the page. */
	private ParameterSweepWizard owner = null;
	private PS_ModelWizard mEntry = null;
	private PlatformType type = null;
	
	//====================================================================================================
	// GUI members:
	
	private JPanel content = null;
	private JList platformList = null;
	private JTextArea descriptionArea = null;
	private JButton registerButton = null;
	private JPanel buttonPanel = null;

	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public Page_ChoosePlatform(ParameterSweepWizard owner, PS_ModelWizard mEntry) {
		this.owner = owner;
		this.mEntry = mEntry;
		layoutGUI();
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setSelectedPlatform(PlatformType type) {
		this.type = type;
		platformList.setSelectedValue(type,true);
	}

	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("REGISTER")) {
			ParameterSweepWizard.setPreferences(new WizardPreferences());
			ParameterSweepWizard.getPreferences().gotoPage(1);
			JDialog dialog = ParameterSweepWizard.getPreferences().showInDialog(ParameterSweepWizard.getFrame());
			dialog.setVisible(true);
			Set<PlatformType> platformSet = PlatformManager.getRegisteredPlatforms();
			SortedListModel listModel = new SortedListModel();
			for (PlatformType type : platformSet) 
				listModel.addElement(type);
			listModel.sort();
			platformList.setModel(listModel);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getInfoText(Wizard w) { return w.getArrowsHeader("Select a simulation platform"); }
	public String getTitle() { return "Platform selection"; }
	public JPanel getPanel() { return this;	}
	public boolean onButtonPress(Button b) { return true; }
	
	//----------------------------------------------------------------------------------------------------
	public void onPageChange(boolean show) {
		if (!show) {
			if (type != PlatformSettings.getPlatformType()) {
				int result = Utilities.askUser(owner,false,"Warning","You changed the selected platform.","All of your previous settings will be lost.",
											   "Are you sure?");
				if (result < 1) {
					platformList.setSelectedValue(PlatformSettings.getPlatformType(),true);
					descriptionArea.setText(PlatformManager.getPlatform(PlatformSettings.getPlatformType()).getDescription());
					type = PlatformSettings.getPlatformType();
				} else {
					if (mEntry != null) {
						try {
							PlatformSettings.setSelectedPlatform(type);
							owner.writeUsedPlatformToRegistry();
							mEntry.reinitialize(mEntry.getInitClassPath(),false,true);
							mEntry.start();
						} catch (Exception e2) {
							if( e2 instanceof PlatformSettings.UnsupportedPlatformException )
								Utilities.userAlert(owner, "Wizard reinitialization failed.\n" +
														   "Selected platform is not supported" +
														   " by the current version of MEME.");
							else
								Utilities.userAlert(owner,"Wizard reinitialization failed.");
							
							if (ParameterSweepWizard.isFromMEME()) {
								JDialog appWindow = (JDialog) mEntry.getAppWindow();
								appWindow.setVisible(false);
								owner.dispose();
								appWindow.dispose();
								appWindow = null;
								System.gc();
							} else
								System.exit(1);
						} 
					} else
						System.exit(1);
				}
			} 
		} else {
			Set<PlatformType> platformSet = PlatformManager.getRegisteredPlatforms();
			SortedListModel listModel = new SortedListModel();
			for (PlatformType type : platformSet) 
				listModel.addElement(type);
			listModel.sort();
			platformList.setModel(listModel);
			if (PlatformSettings.getPlatformType() != null)
				platformList.setSelectedValue(PlatformSettings.getPlatformType(),true);
			else
				platformList.setSelectedValue(PlatformType.REPAST,true);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean isEnabled(Button b) {
			switch (b) {
			case CANCEL : return true;
			case CUSTOM :
			case FINISH : 
			case NEXT 	: return !platformList.isSelectionEmpty();
			case BACK : 
			default : return false;
			}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void valueChanged(ListSelectionEvent e) {
		PlatformType selectedPlatform = (PlatformType) platformList.getSelectedValue();
		type = selectedPlatform;
		descriptionArea.setText(selectedPlatform != null ? PlatformManager.getPlatform(selectedPlatform).getDescription() : "");
		owner.enableDisableButtons();
    }
	
	//====================================================================================================
	// GUI methods
	
	//----------------------------------------------------------------------------------------------------
	private void layoutGUI() {
		Set<PlatformType> platformSet = PlatformManager.getRegisteredPlatforms();
		Vector<PlatformType> platforms = new Vector<PlatformType>(platformSet);
		SortedListModel model = new SortedListModel(platforms);
		model.sort();
		platformList = new JList(model);
		platformList.setName("lst_wizard_platformlst");
		platformList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		platformList.addListSelectionListener(this);
		platformList.setPreferredSize(new Dimension(270,300));
		platformList.setBorder(BorderFactory.createEtchedBorder());
		
		descriptionArea = new JTextArea();
		descriptionArea.setEditable(false);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		descriptionArea.setColumns(40);
		JScrollPane scrollPane = new JScrollPane(descriptionArea);
		scrollPane.setBorder(BorderFactory.createEtchedBorder());
		
		platformList.setSelectedValue(PlatformType.REPAST,true);
		
		registerButton = new JButton("Register platform...");
		registerButton.setName("btn_platforms_register");
		registerButton.setActionCommand("REGISTER");
		registerButton.addActionListener(this);
		buttonPanel = FormsUtils.build("p ~ p:g",
									   "0_",
									   registerButton).getPanel();
		
		content = FormsUtils.build("p ~ f:p:g", 
								   "[DialogBorder]01 ||" +
								   				 "23 f:p:g||" +
								   				 "44 p", 
								   "Available platforms: ","Description: ",
								   platformList,descriptionArea,
								   buttonPanel).getPanel();
	
		this.setLayout(new BorderLayout());
		final JScrollPane sp = new JScrollPane(content);
    	this.add(sp,BorderLayout.CENTER);
    	this.validate();
    }
}
