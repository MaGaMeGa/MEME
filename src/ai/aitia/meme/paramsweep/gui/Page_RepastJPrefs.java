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
import java.util.Properties;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import ai.aitia.meme.gui.Preferences;
import ai.aitia.meme.gui.PreferencesPage;
import ai.aitia.meme.gui.Preferences.Button;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.FormsUtils.Separator;

import ai.aitia.meme.paramsweep.gui.WizardPreferences.IReinitalizeable;

public class Page_RepastJPrefs extends PreferencesPage implements IReinitalizeable {
	
	//=====================================================================================
	//members

	private static final long serialVersionUID = 1L;
	
	/** The owner component of the page. */
	private WizardPreferences owner = null;
	
	//=====================================================================================
	// GUI members
	
	private JPanel content = null;
	private JCheckBox sourceCheckBox = new JCheckBox("Generate source file");
	private JCheckBox rngSeedCheckBox = new JCheckBox("Always use 'rngSeed' as parameter (may require restart)");
	private JCheckBox closeAfterOneScriptCheckBox = new JCheckBox("Close Script dialog after creating a script");
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the owner component of the page
	 */
	public Page_RepastJPrefs(WizardPreferences owner) {
		super("RepastJ");
		this.owner = owner;
		layoutGUI();
		reinitialize();
	}
	
	//=====================================================================================
	// implemented interfaces
	
	//-------------------------------------------------------------------------------------
	@Override public String	getInfoText(Preferences p) { return "RepastJ properties."; }
	
	//-------------------------------------------------------------------------------------
	public void reinitialize() {
		Properties p = owner.getProperties();
		sourceCheckBox.setSelected(Boolean.parseBoolean(p.getProperty(WizardPreferences.SOURCE_GENERATION)));
		rngSeedCheckBox.setSelected(Boolean.parseBoolean(p.getProperty(WizardPreferences.RNGSEED_AS_PARAMETER)));
		closeAfterOneScriptCheckBox.setSelected(Boolean.parseBoolean(p.getProperty(WizardPreferences.CLOSE_AFTER_ONE_SCRIPT)));
	}
	
	//-------------------------------------------------------------------------------------
	@Override
	public boolean onButtonPress(Button b) {
		switch (b) {
		case CANCEL : return true;
		case OK		: Properties p = owner.getProperties();
					  p.setProperty(WizardPreferences.SOURCE_GENERATION,String.valueOf(sourceCheckBox.isSelected()));
					  p.setProperty(WizardPreferences.RNGSEED_AS_PARAMETER,String.valueOf(rngSeedCheckBox.isSelected()));
					  p.setProperty(WizardPreferences.CLOSE_AFTER_ONE_SCRIPT,String.valueOf(closeAfterOneScriptCheckBox.isSelected()));
		}
		return true;
	}
	
	//=====================================================================================
	// GUI methods
	
	//-------------------------------------------------------------------------------------
	private void layoutGUI() {
		content = FormsUtils.build("p ~ p:g ~ p",
				   "[DialogBorder]000||" +
				   				 "111||" +
				   				 "222|" +
				   				 "333|" +
				   				 "444",
				   "RepastJ is a built-in platform.",
				   new Separator("Settings"),
				   sourceCheckBox,
				   rngSeedCheckBox,
				   closeAfterOneScriptCheckBox).getPanel();

		this.setLayout(new BorderLayout());
		this.add(content,BorderLayout.CENTER);
	}
}
