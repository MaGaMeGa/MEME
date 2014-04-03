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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import ai.aitia.meme.gui.Preferences;
import ai.aitia.meme.gui.PreferencesPage;
import ai.aitia.meme.gui.Preferences.Button;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.gui.WizardPreferences.IReinitalizeable;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.FormsUtils.Separator;
import ai.aitia.testing.fest.CompNames.Btn;

/** This class provides the General page of the Prefereces dialog of the wizard. */
public class Page_General extends PreferencesPage implements ActionListener,
															 CaretListener,
															 IReinitalizeable {

	//=====================================================================================
	//members

	private static final long serialVersionUID = 1L;
	
	/** The owner component of the page. */
	private WizardPreferences owner = null;
	
	//=====================================================================================
	// GUI members
	
	private JPanel content = null;
	private JTextArea settingsPathField = new JTextArea();
	private JButton browseButton = new JButton("Browse...");
	private JCheckBox saveParamTreeBox = new JCheckBox("Save parameter tree to file");
	private JCheckBox skipPlatformBox = new JCheckBox("Skip platform selection page and use previous selection");
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the owner component of the page
	 */
	public Page_General(WizardPreferences owner) {
		super("General");
		this.owner = owner;
		layoutGUI();
		initialize();
	}

	//=====================================================================================
	// implemented interfaces
	
	//-------------------------------------------------------------------------------------
	public void reinitialize() {
		settingsPathField.setText(owner.getSettingsPath());
		saveParamTreeBox.setSelected(owner.saveParameterTree());
		skipPlatformBox.setSelected(owner.skipPlatformPage());
	}
	
	//-------------------------------------------------------------------------------------
	@Override public String getInfoText(Preferences p) { return "General properties and flags."; }
	
	//-------------------------------------------------------------------------------------
	@Override
	public boolean isEnabled(Button b) {
		switch (b) {
		case CANCEL : return true;
		case OK		: return !settingsPathField.getText().trim().equals(""); 
		}
		return false;
	}
	
	//-------------------------------------------------------------------------------------
	@Override
	public boolean onButtonPress(Button b) {
		switch (b) {
		case CANCEL : return true;
		case OK		: if (settingsPathField.getText().trim().equals("")) return false;
					  File f = new File(settingsPathField.getText().trim());
					  if (f.exists()) {
						  boolean ok = !owner.warning(!f.isDirectory(),settingsPathField.getText().trim() + " is not a directory.",
								  	   Preferences.WARNING,true);
						  if (ok) {
							  Properties p = owner.getProperties();
							  p.setProperty(WizardPreferences.SETTINGS_PATH,settingsPathField.getText().trim());
							  p.setProperty(WizardPreferences.SAVE_PARAMETER_TREE,String.valueOf(saveParamTreeBox.isSelected()));
							  p.setProperty(WizardPreferences.SKIP_PLATFORM_PAGE,String.valueOf(skipPlatformBox.isSelected()));
						  }
						  return ok;
					  } else
						  return !owner.warning(true,settingsPathField.getText().trim() + " does not exist.",Preferences.WARNING,true);
		}
		return true;
	}
	
	//-------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if ("BROWSE".equals(command)) {
			JFileChooser chooser = new JFileChooser(ParameterSweepWizard.getLastDir());
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int result = chooser.showOpenDialog(owner);
			if (result == JFileChooser.APPROVE_OPTION) {
				settingsPathField.setText(chooser.getSelectedFile().getPath());
				ParameterSweepWizard.setLastDir(chooser.getSelectedFile());
				owner.enableDisableButtons();
			}
		}
	}
	
	//------------------------------------------------------------------------------------
	public void caretUpdate(CaretEvent e) {
		String s = settingsPathField.getText().trim();
		owner.warning(s.length() == 0, "Wizard settings folder: empty path",Preferences.WARNING,true);
		if (s.length() > 0)
			owner.clearProblemText();
		if (s.length() < 2)
			owner.enableDisableButtons();
	}
	
	//=====================================================================================
	// GUI methods
	
	//-------------------------------------------------------------------------------------
	private void layoutGUI() {
		content = FormsUtils.build("p ~ p:g ~ p",
								   "[DialogBorder]012||" +
								   				 "333||" +
								   				 "444|" +
								   				 "555|",
								   "Wizard settings folder: ",settingsPathField,browseButton,
								   new Separator("General flags"),
								   saveParamTreeBox,
								   skipPlatformBox).getPanel();
		browseButton.setName("btn_preferences_browse");
		this.setLayout(new BorderLayout());
		this.add(content,BorderLayout.CENTER);
	}
	
	//-------------------------------------------------------------------------------------
	private void initialize() {
		settingsPathField.setPreferredSize(new Dimension(400,22));
		settingsPathField.addCaretListener(this);
		browseButton.setActionCommand("BROWSE");
		GUIUtils.addActionListener(this,browseButton);
		reinitialize();
	}
}
