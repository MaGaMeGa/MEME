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
import ai.aitia.meme.utils.FormsUtils.Separator;

public class Page_SimphonyPrefs extends PreferencesPage implements ActionListener,
																   IReinitalizeable {

	//=====================================================================================
	//members

	private static final long serialVersionUID = 1L;
	private static final PlatformType type = PlatformType.SIMPHONY; 
	
	/** The owner component of the page. */
	private WizardPreferences owner = null;
	
	//=====================================================================================
	// GUI members
	
	private JPanel content = null;
	private JTextField platformDirField = null;
	private JButton browseButton = null;
	private JButton registerButton = null;
	private JButton deRegisterButton = null;
	private JCheckBox sourceCheckBox = new JCheckBox("Generate source files");
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the owner component of the page
	 */
	public Page_SimphonyPrefs( WizardPreferences owner ){
		super("Repast Simphony 1.1");
		this.owner = owner;
		layoutGUI();
		initialize();
	}
	
	//=====================================================================================
	// implemented interfaces
	
	//-------------------------------------------------------------------------------------
	@Override public String getInfoText(Preferences p) { return "Repast Simphony properties."; }
	@Override public boolean onButtonPress(Button b) { return true; }
	
	//-------------------------------------------------------------------------------------
	public void reinitialize() {
		Properties p = owner.getProperties();
		sourceCheckBox.setSelected(
				Boolean.parseBoolean(
						p.getProperty(WizardPreferences.REPASTS_SOURCE_GENERATION)));
		String installationDirectory = PlatformManager.getInstallationDirectory(type);
		platformDirField.setText(installationDirectory == null ? "" : installationDirectory);
		deRegisterButton.setEnabled(installationDirectory != null);
	}
	
	//-------------------------------------------------------------------------------------
	public void actionPerformed( ActionEvent e ){
		String command = e.getActionCommand();
		if (command.equals("BROWSE")) {
			File ans = null;
			JFileChooser chooser = new JFileChooser(ParameterSweepWizard.getLastDir());
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
		} else if (command.equals("REGISTER")) {
			File dir = new File(platformDirField.getText().trim());
			if (PlatformManager.registerPlatform(type,dir.getAbsolutePath())) {
				owner.warning(true,"Registration successful!",Preferences.MESSAGE,true);
				deRegisterButton.setEnabled(true);
			} else
				owner.warning(true,"Invalid platform directory.",Preferences.WARNING,false);
		} else if (command.equals("DEREGISTER")) {
			int result = Utilities.askUser(owner,false,"Confirmation","Are you sure?");
			if (result == 1) {
				PlatformManager.removePlatform(PlatformType.SIMPHONY);
				owner.warning(true,"Deregistration successful!",Preferences.MESSAGE,true);
				deRegisterButton.setEnabled(false);
				platformDirField.setText("");
			}
		}else if (command.equals("NEEDSOURCE")){
			Properties p = owner.getProperties();
			p.setProperty( WizardPreferences.REPASTS_SOURCE_GENERATION,
						   String.valueOf(sourceCheckBox.isSelected() ) );
		}
	}
	
	//=====================================================================================
	// GUI methods
	
	//-------------------------------------------------------------------------------------
	private void layoutGUI() {
		platformDirField = new JTextField();
		browseButton = new JButton("Browse");
		registerButton = new JButton("Register");
		deRegisterButton = new JButton("Deregister");
		deRegisterButton.setEnabled(false);
		
		content = FormsUtils.build("p ~ p ~ p:g ~ p",
				   "[DialogBorder]0000||" +
				   				 "1112||" +
				   				 "34__||" +
				   				 "5555||"  +
				   				 "66__|",
				   "Register platform by selecting its installation directory:",
				   platformDirField,
				   browseButton,
				   registerButton, deRegisterButton,
				   new Separator("Settings"),
				   sourceCheckBox).getPanel();

		this.setLayout(new BorderLayout());
		this.add(content,BorderLayout.CENTER);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initialize() {
		browseButton.setActionCommand("BROWSE");
		browseButton.addActionListener(this);
		registerButton.setActionCommand("REGISTER");
		registerButton.addActionListener(this);
		deRegisterButton.setActionCommand("DEREGISTER");
		deRegisterButton.addActionListener(this);
		sourceCheckBox.setActionCommand( "NEEDSOURCE" );
		sourceCheckBox.addActionListener(this);
		reinitialize();
	}
}
