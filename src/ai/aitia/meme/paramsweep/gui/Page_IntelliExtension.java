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
import java.awt.Container;
import java.awt.Image;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ai.aitia.meme.gui.Wizard;
import ai.aitia.meme.gui.Wizard.Button;
import ai.aitia.meme.gui.Wizard.IArrowsInHeader;
import ai.aitia.meme.gui.Wizard.IWizardPage;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.generator.WizardSettingsManager;
import ai.aitia.meme.paramsweep.plugin.IIntelliMethodPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;

/**
 * @author Ferschl
 *
 */
public class Page_IntelliExtension extends JPanel implements IWizardPage,
																IArrowsInHeader {

    private static final long serialVersionUID = -1139304437944444616L;
    
	private static final int ICON_WIDTH_AND_HEIGHT = 25;
	public static final ImageIcon DESCRIPTION_ICON = new ImageIcon(new ImageIcon(Page_IntelliExtension.class.getResource("icons/info.png")).
																	getImage().getScaledInstance(ICON_WIDTH_AND_HEIGHT, ICON_WIDTH_AND_HEIGHT, Image.SCALE_SMOOTH));

    
    private ParameterSweepWizard owner = null;
    private boolean beenHere = false;
    
    private JComponent content = null;

	private JScrollPane contentScr;

	public Page_IntelliExtension(ParameterSweepWizard owner){
		this.owner = owner;
		layoutGUI();
	}
    
    private void layoutGUI() {
    	String title = "";
    	if(owner.getSweepingMethodID() == 1){
    		content = owner.getIntelliStaticMethodPlugin().getSettingsPanel(owner.getIntelliContext());
    		title = owner.getIntelliStaticMethodPlugin().getLocalizedName()+" [static] method";
    	}
    	if(owner.getSweepingMethodID() == 2){
    		content = owner.getIntelliDynamicMethodPlugin().getSettingsPanel(owner.getIntelliContext());//maybe another context is needed
    		title = owner.getIntelliDynamicMethodPlugin().getLocalizedName()+" [dynamic] method";
    	}
    	if(content != null){
    		contentScr = new JScrollPane(content);
	    	this.removeAll();
			this.setLayout(new BorderLayout());
			contentScr.setBorder(BorderFactory.createTitledBorder(title));
	    	this.add(contentScr,BorderLayout.CENTER);
	    	this.validate();
    	}
    }

	/* (non-Javadoc)
	 * @see ai.aitia.meme.gui.Wizard.IWizardPage#getInfoText(ai.aitia.meme.gui.Wizard)
	 */
	public String getInfoText(Wizard w) {
		return w.getArrowsHeader("You can set up the chosen IntelliSweep method on this page.");
	}

	/* (non-Javadoc)
	 * @see ai.aitia.meme.gui.Wizard.IWizardPage#getPanel()
	 */
	public Container getPanel() {
		return this;
	}

	/* (non-Javadoc)
	 * @see ai.aitia.meme.gui.Wizard.IWizardPage#isEnabled(ai.aitia.meme.gui.Wizard.Button)
	 */
	public boolean isEnabled(Button b) {
		if(owner.getSweepingMethodID() == 0){
			switch (b) {
			case BACK :
			case CANCEL :
			case CUSTOM : 
			case FINISH : return true;
			default : return false;
			}
		} else{
			switch (b) {
			case BACK :
			case CANCEL : return true;
			case CUSTOM : 
			case FINISH : return beenHere;
			default : return false;
			}
		}
	}

	/* (non-Javadoc)
	 * @see ai.aitia.meme.gui.Wizard.IWizardPage#onButtonPress(ai.aitia.meme.gui.Wizard.Button)
	 */
	public boolean onButtonPress(Button b) {
		if(owner.getSweepingMethodID() == 0){
			return isEnabled(b);
		} else if(owner.getSweepingMethodID() == IIntelliMethodPlugin.STATIC_METHOD){
			if(b == Button.FINISH || b == Button.CUSTOM){
				if(owner.getIntelliStaticMethodPlugin().getReadyStatus()){
					boolean success = owner.getIntelliStaticMethodPlugin().alterParameterTree(owner.getIntelliContext());
					return success;
				} else{
					Utilities.userAlert(owner, 	"Warning",
												"The IntelliSweep " + owner.getIntelliStaticMethodPlugin().getLocalizedName() + " plugin reported:",
												owner.getIntelliStaticMethodPlugin().getReadyStatusDetail());
					return false;
				}
			}
			else return isEnabled(b);
		} else if(owner.getSweepingMethodID() == IIntelliMethodPlugin.DYNAMIC_METHOD){
			if(b == Button.FINISH || b == Button.CUSTOM){
				if(owner.getIntelliDynamicMethodPlugin().getReadyStatus()){
					owner.getIntelliDynamicMethodPlugin().alterParameterTree(owner.getIntelliContext());
					return true;
				} else{
					String errorMsg = 
						owner.getIntelliDynamicMethodPlugin().settingsOK( owner.getRecorderTreeRoot() );
					if( errorMsg == null ){
						Utilities.userAlert(owner, 	"Warning",
												"The IntelliSweep " + owner.getIntelliDynamicMethodPlugin().getLocalizedName() + " plugin is not fully configured.",
												"Please check any missing settings and try again!");
					}else{
						Utilities.userAlert(owner, 	"Warning", errorMsg );
					}
					return false;
				}
			}
			else return isEnabled(b);
		}
		return isEnabled(b);
	}

	/* (non-Javadoc)
	 * @see ai.aitia.meme.gui.Wizard.IWizardPage#onPageChange(boolean)
	 */
	public void onPageChange(boolean toThis) {
		if(toThis){
			beenHere = true;
			if(content == null){
				layoutGUI();
			}
			else{
				//TODO: check if other plugin was selected and call layoutGUI() in that case
				layoutGUI(); //no checking at this moment
			}
			
			if( owner.getSweepingMethodID() == 2 ){				
				owner.getIntelliDynamicMethodPlugin().setRecordableVariables( owner.getRecorderTreeRoot() );	
			}
			

			beenHere = true;
		}

	}

	public String getTitle() {
	    return "IntelliSweep settings";
    }
	
	//-------------------------------------------------------------------------------
	/** Saves the page-related model settings to the XML node <code>node</code>. This
	 *  method is part of the model settings storing performed by {@link ai.aitia.meme.paramsweep.generator.WizardSettingsManager 
	 *  WizardSettingsManager}.
	 */
	public void save(Node node) {
		Document document = node.getOwnerDocument();
		String typeAttributeValue = null;
		switch (owner.getSweepingMethodID()) {
        case 0:
	        typeAttributeValue = "none";
	        break;
        case 1:
	        typeAttributeValue = "static";
	        Element pluginElement = document.createElement("plugin");
	        pluginElement.setAttribute(WizardSettingsManager.NAME, owner.getIntelliStaticMethodPlugin().getLocalizedName());
	        owner.getIntelliStaticMethodPlugin().save(pluginElement);
	        node.appendChild(pluginElement);
	        break;
        case 2:
        	typeAttributeValue = "dynamic";
	        Element pluginElement2 = document.createElement("plugin");
	        pluginElement2.setAttribute(WizardSettingsManager.NAME, owner.getIntelliDynamicMethodPlugin().getLocalizedName());
	        owner.getIntelliDynamicMethodPlugin().save(pluginElement2);
	        node.appendChild(pluginElement2);
	        break;
        default:
	        break;
        }
		((Element)node).setAttribute(WizardSettingsManager.TYPE, typeAttributeValue);
	}
	
	//-------------------------------------------------------------------------------
	/** Loads the page-related model settings from the XML element <code>element</code>. This
	 *  method is part of the model settings retrieving performed by {@link ai.aitia.meme.paramsweep.generator.WizardSettingsManager 
	 *  WizardSettingsManager}.
	 * @throws WizardLoadingException if the XML document is invalid
	 */
	public void load(Element element) throws WizardLoadingException {
		NodeList nl = null;
		if(element.getAttribute(WizardSettingsManager.TYPE).equals("none")){

		} else if(element.getAttribute(WizardSettingsManager.TYPE).equals("static")){
			nl = element.getChildNodes();
			if (nl != null && nl.getLength() > 0){
				Element pluginElement = (Element) nl.item(0);
				String pluginName = pluginElement.getAttribute(WizardSettingsManager.NAME);
				owner.getIntelliStaticPluginInfos().findByLocalizedName(pluginName).load(owner.getIntelliContext(), pluginElement);
				owner.setSelectedSweepingMethodByLocalizedName(pluginName);
			}
		} else if(element.getAttribute(WizardSettingsManager.TYPE).equals("dynamic")){
			nl = element.getChildNodes();
			if (nl != null && nl.getLength() > 0){
				Element pluginElement = (Element) nl.item(0);
				String pluginName = pluginElement.getAttribute(WizardSettingsManager.NAME);
				owner.getIntelliDynamicPluginInfos().findByLocalizedName(pluginName).load(owner.getIntelliContext(), pluginElement);
				owner.setSelectedSweepingMethodByLocalizedName(pluginName);
				owner.getIntelliDynamicMethodPlugin().setRecordableVariables(owner.getRecorderTreeRoot());
			}
		}
	}


}
