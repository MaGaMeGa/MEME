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
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import ai.aitia.meme.gui.Preferences;
import ai.aitia.meme.gui.PreferencesPage;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.FormsUtils.Separator;

public class Page_Platforms extends PreferencesPage implements ActionListener {
	
	//=====================================================================================
	//members

	private static final long serialVersionUID = 1L;
	
	/** The owner component of the page. */
	private WizardPreferences owner = null;
	
	//=====================================================================================
	// GUI members
	
	private JPanel content = null;
	private JList knownPlatforms = null;
	private JList registeredPlatforms = null;
	private JButton editButton = null;
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the owner component of the page
	 */
	public Page_Platforms( WizardPreferences owner ){
		super("Platforms");
		this.owner = owner;
		layoutGUI();
	}
	
	//=====================================================================================
	// implemented interfaces
	
	//-------------------------------------------------------------------------------------
	@Override public String getInfoText(Preferences p) { return "Platform properties."; }
	
	//----------------------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		
		if (command.equals("EDIT")) {
			if (knownPlatforms.getSelectedIndex() > -1) {
				PlatformType type = (PlatformType) knownPlatforms.getSelectedValue();
				PreferencesPage page = owner.getPlatformPages().get(type);
				if (page != null)
					owner.gotoPage(page);
				else
					owner.warning(true,"Cannot find page",WizardPreferences.WARNING,true);
			}
		}
	}
	
	//=====================================================================================
	// GUI methods
	
	//-------------------------------------------------------------------------------------
	private void layoutGUI() {
		Set<PlatformManager.PlatformType> platformSet = PlatformManager.getRegisteredPlatforms();
		Vector<String> platforms = new Vector<String>();
		Iterator<PlatformManager.PlatformType> iterator = platformSet.iterator();
		while (iterator.hasNext()) 
			platforms.add(PlatformManager.getPlatform(iterator.next()).getDisplayableName());
		
		PlatformManager.PlatformType[] known = PlatformManager.getSupportedPlatforms();
		
		knownPlatforms = new JList(known);
		knownPlatforms.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scrollPane = new JScrollPane(knownPlatforms);
		scrollPane.setBorder(BorderFactory.createEtchedBorder());
		registeredPlatforms = new JList(platforms);
		JScrollPane scrollPane2 = new JScrollPane(registeredPlatforms);
		scrollPane2.setBorder(BorderFactory.createEtchedBorder());
		
		editButton = new JButton("Edit settings");
		editButton.setActionCommand("EDIT");
		editButton.addActionListener(this);
		
		content = FormsUtils.build("p ~ p:g ~ p",
				   				   "[DialogBorder]000||" +
				   				   "111||" +
				   				   "222 f:p||" +
				   				   "__3 p||" +
				   				   "444||" +
				   				   "555 f:p",
				   				   "To register a known platform select it from the list and " +
				   				   "press the 'Edit settings' button.",
				   				   new Separator("Known platforms"),
				   				   scrollPane,
				   				   editButton,
				   				   new Separator("Registered platforms"),
				   				   scrollPane2).getPanel();
		this.setLayout(new BorderLayout());
		this.add(content,BorderLayout.CENTER);
	}
}
