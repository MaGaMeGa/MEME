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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

import ai.aitia.meme.gui.Preferences;
import ai.aitia.meme.gui.PreferencesPage;
import ai.aitia.meme.gui.Preferences.Button;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.gui.WizardPreferences.IReinitalizeable;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.FormsUtils.Separator;

public class Page_NetLogo5Prefs extends PreferencesPage implements IReinitalizeable, ActionListener {
	//=====================================================================================
	//members

	private static final long serialVersionUID = 1L;
	
	/** The owner component of the page. */
	private WizardPreferences owner = null;
	
	//=====================================================================================
	// GUI members
	
	private JPanel content = null;
	private JTextField platformDirField = new JTextField();
	private JButton browseButton = new JButton("Browse");
	private JButton registerButton = new JButton("Register");
	private JButton deRegisterButton = new JButton("Deregister");
//	private JCheckBox sourceCheckBox = new JCheckBox("Generate source file");
	private JCheckBox randomSeedCheckBox = new JCheckBox("Always use 'random-seed' as parameter (may require restart)");
	private JCheckBox closeAfterOneScriptCheckBox = new JCheckBox("Close Script dialog after creating a script");
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the owner component of the page
	 */
	public Page_NetLogo5Prefs(WizardPreferences owner) {
		super("NetLogo-5");
		this.owner = owner;
		layoutGUI();
		reinitialize();
	}
	
	//=====================================================================================
	// implemented interfaces
	
	//-------------------------------------------------------------------------------------
	@Override public String getInfoText(Preferences p) { return "NetLogo 5 properties."; }
	
	//-------------------------------------------------------------------------------------
	public void reinitialize() {
		Properties p = owner.getProperties();
		String installationDirectory = PlatformManager.getInstallationDirectory(PlatformType.NETLOGO5);
		platformDirField.setText(installationDirectory == null ? "" : installationDirectory);
		deRegisterButton.setEnabled(installationDirectory != null);
		randomSeedCheckBox.setSelected(Boolean.parseBoolean(p.getProperty(WizardPreferences.NETLOGO5_RANDOM_SEED_AS_PARAMETER)));
		closeAfterOneScriptCheckBox.setSelected(Boolean.parseBoolean(p.getProperty(WizardPreferences.NETLOGO5_CLOSE_AFTER_ONE_SCRIPT)));
	}
	
	//----------------------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if ("BROWSE".equals(cmd)) {
			File ans = null;
			final JFileChooser chooser = new JFileChooser(ParameterSweepWizard.getLastDir());
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = -1;
			result = chooser.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				ans = chooser.getSelectedFile();
				if (ans != null)
					ParameterSweepWizard.setLastDir(ans);
				
			}
			if (ans != null)
				platformDirField.setText(ans.getAbsolutePath());
		} else if ("REGISTER".equals(cmd)) {
			final File dir = new File(platformDirField.getText().trim());
			if (PlatformManager.registerPlatform(PlatformType.NETLOGO5,dir.getAbsolutePath())) {
				owner.warning(true,"Registration successful!",Preferences.MESSAGE,true);
				deRegisterButton.setEnabled(true);
			} else 
				owner.warning(true,"Invalid platform directory.",Preferences.WARNING,false);
		} else if ("DEREGISTER".equals(cmd)) {
			int result = Utilities.askUser(owner,false,"Confirmation","Are you sure?");
			if (result == 1) {
				PlatformManager.removePlatform(PlatformType.NETLOGO5);
				owner.warning(true,"Deregistration successful!",Preferences.MESSAGE,true);
				deRegisterButton.setEnabled(false);
				platformDirField.setText("");
			}
		}
	}
	
	//-------------------------------------------------------------------------------------
	@Override
	public boolean onButtonPress(Button b) {
		switch (b) {
		case CANCEL : return true;
		case OK		: Properties p = owner.getProperties();
					  p.setProperty(WizardPreferences.NETLOGO5_RANDOM_SEED_AS_PARAMETER,String.valueOf(randomSeedCheckBox.isSelected()));
					  p.setProperty(WizardPreferences.NETLOGO5_CLOSE_AFTER_ONE_SCRIPT,String.valueOf(closeAfterOneScriptCheckBox.isSelected()));
		}
		return true;
	}
	
	//=====================================================================================
	// GUI methods
	
	//-------------------------------------------------------------------------------------
	private void layoutGUI() {
		
		JPanel tmp = FormsUtils.build("p ~ p ~ p:g ~ p",
									  "0000||" +
									  "1112||" +
									  "34__|",
									  "Register platform by selecting its installation directory:",
									  platformDirField,browseButton,
									  registerButton,deRegisterButton).getPanel();	
		
		content = FormsUtils.build("p ~ p:g ~ p ",
				   "[DialogBorder]000||" +
				   				 "111||" +
				   				 "222|" +
				   				 "333|" +
				   				 "444",
				   "A platform for NetLogo 5 models",
				   tmp,
				   new Separator("Settings"),
				   randomSeedCheckBox,
				   closeAfterOneScriptCheckBox).getPanel();

		this.setLayout(new BorderLayout());
		this.add(content,BorderLayout.CENTER);
		
		registerButton.setActionCommand("REGISTER");
		deRegisterButton.setActionCommand("DEREGISTER");
		browseButton.setActionCommand("BROWSE");
		registerButton.setName("btn_preferences_register");
		deRegisterButton.setName("btn_preferences_deregister");
		browseButton.setName("btn_preferences_browse");
		platformDirField.setName("fld_preferences_platformdir");
		
		GUIUtils.addActionListener(this,registerButton,deRegisterButton,browseButton);
	}
}
