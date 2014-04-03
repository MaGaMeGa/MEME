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
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import ai.aitia.meme.gui.Preferences;
import ai.aitia.meme.gui.PreferencesPage;
import ai.aitia.meme.gui.Preferences.Button;
import ai.aitia.meme.paramsweep.gui.WizardPreferences.IReinitalizeable;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.FormsUtils.Separator;

public class Page_CustomJavaPrefs extends PreferencesPage implements IReinitalizeable, ActionListener {
	//=====================================================================================
	//members

	private static final long serialVersionUID = 1L;
	
	/** The owner component of the page. */
	private WizardPreferences owner = null;
	
	//=====================================================================================
	// GUI members
	
	private JPanel content = null;
	private JButton registerButton = new JButton("Register");
	private JButton deRegisterButton = new JButton("Deregister");
	private JCheckBox sourceCheckBox = new JCheckBox("Generate source file");
	private JCheckBox closeAfterOneScriptCheckBox = new JCheckBox("Close Script dialog after creating a script");
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the owner component of the page
	 */
	public Page_CustomJavaPrefs(WizardPreferences owner) {
		super("Custom Java");
		this.owner = owner;
		layoutGUI();
		reinitialize();
	}
	
	//=====================================================================================
	// implemented interfaces
	
	//-------------------------------------------------------------------------------------
	@Override public String getInfoText(Preferences p) { return "Custom Java platform properties."; }
	
	//-------------------------------------------------------------------------------------
	public void reinitialize() {
		Properties p = owner.getProperties();
		sourceCheckBox.setSelected(Boolean.parseBoolean(p.getProperty(WizardPreferences.CUSTOM_JAVA_SOURCE_GENERATION)));
		closeAfterOneScriptCheckBox.setSelected(Boolean.parseBoolean(p.getProperty(WizardPreferences.CUSTOM_JAVA_CLOSE_AFTER_ONE_SCRIPT)));
	}
	
	//----------------------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if ("REGISTER".equals(cmd)) {
			PlatformManager.registerPlatform(PlatformType.CUSTOM,".");
			owner.warning(true,"Registration successful!",Preferences.MESSAGE,true);
		} else if ("DEREGISTER".equals(cmd)) {
			int result = Utilities.askUser(owner,false,"Confirmation","Are you sure?");
			if (result == 1) {
				PlatformManager.removePlatform(PlatformType.CUSTOM);
				owner.warning(true,"Deregistration successful!",Preferences.MESSAGE,true);
			}
		}
	}
	
	//-------------------------------------------------------------------------------------
	@Override
	public boolean onButtonPress(Button b) {
		switch (b) {
		case CANCEL : return true;
		case OK		: Properties p = owner.getProperties();
					  p.setProperty(WizardPreferences.CUSTOM_JAVA_SOURCE_GENERATION,String.valueOf(sourceCheckBox.isSelected()));
					  p.setProperty(WizardPreferences.CUSTOM_JAVA_CLOSE_AFTER_ONE_SCRIPT,String.valueOf(closeAfterOneScriptCheckBox.isSelected()));
		}
		return true;
	}
	
	//=====================================================================================
	// GUI methods
	
	//-------------------------------------------------------------------------------------
	private void layoutGUI() {
		
		JPanel tmp = FormsUtils.build("p ~ p ~ p:g",
									  "01_",
									  registerButton,deRegisterButton).getPanel();	
		
		content = FormsUtils.build("p ~ p:g ~ p ",
				   "[DialogBorder]000||" +
				   				 "111||" +
				   				 "222||" +
				   				 "333|" +
				   				 "444",
				   "A platform for custom Java models.",
				   tmp,
				   new Separator("Settings"),
				   sourceCheckBox,
				   closeAfterOneScriptCheckBox).getPanel();

		this.setLayout(new BorderLayout());
		this.add(content,BorderLayout.CENTER);
		
		registerButton.setActionCommand("REGISTER");
		deRegisterButton.setActionCommand("DEREGISTER");
		
		GUIUtils.addActionListener(this,registerButton,deRegisterButton);
	}
}
