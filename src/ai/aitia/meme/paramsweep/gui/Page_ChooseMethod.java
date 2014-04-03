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
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.BorderFactory;
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
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.plugin.IIntelliDynamicMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IIntelliMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IIntelliStaticMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IntelliSweepPluginDescriptor;
import ai.aitia.meme.pluginmanager.PluginInfo;
import ai.aitia.meme.pluginmanager.PSPluginManager.PluginList;
import ai.aitia.meme.utils.FormsUtils;

/**
 * @author Ferschl
 *
 */
public class Page_ChooseMethod extends JPanel implements IWizardPage,
        												 IArrowsInHeader,
        												 ListSelectionListener {
	
	//====================================================================================================
	// members
	
    private static final long serialVersionUID = 863894276442799487L;

    /** The owner of the page. */
	private ParameterSweepWizard owner = null;
	private Vector<IntelliSweepPluginDescriptor> methods = null;

	//====================================================================================================
	// GUI members:
	
	private JPanel content = null;
	private JList methodList = null;
	private JTextArea descriptionArea = null;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public Page_ChooseMethod(ParameterSweepWizard owner){
		this.owner = owner;
		methods = new Vector<IntelliSweepPluginDescriptor>();
		methods.add(new IntelliSweepPluginDescriptor(null, true));//this is the manual method
		PluginList<IIntelliStaticMethodPlugin> statics = ParameterSweepWizard.getPluginManager().getIntelliStaticPluginInfos();
		PluginList<IIntelliDynamicMethodPlugin> dynamics = ParameterSweepWizard.getPluginManager().getIntelliDynamicPluginInfos();
		for (PluginInfo<IIntelliStaticMethodPlugin> info : statics) 
	        methods.add(new IntelliSweepPluginDescriptor(info.getInstance(),true));
		for (PluginInfo<IIntelliDynamicMethodPlugin> info : dynamics) 
	        methods.add(new IntelliSweepPluginDescriptor(info.getInstance(),false));
		layoutGUI();
	}
	
	//----------------------------------------------------------------------------------------------------
	public int getSweepingMethodID() {
		int idx = methodList.getSelectedIndex();
		if (idx > 0)//if it is an IntelliSweep method
			return methods.get(idx).getMethodType();
		else
			return 0;
	}
	
	//----------------------------------------------------------------------------------------------------
	public IIntelliStaticMethodPlugin getSelectedIntelliStaticMethodPlugin() {
		int idx = methodList.getSelectedIndex();
	    return (IIntelliStaticMethodPlugin) methods.get(idx).getPlugin();
    }

	//----------------------------------------------------------------------------------------------------
	public IIntelliDynamicMethodPlugin getSelectedIntelliDynamicMethodPlugin() {
		int idx = methodList.getSelectedIndex();
	    return (IIntelliDynamicMethodPlugin) methods.get(idx).getPlugin();
    }

	//----------------------------------------------------------------------------------------------------
	public void setSelectedMethodByLocalizedName(String localizedName){
		int i = 0;
		int idx = -1;
		while (idx == -1 && i < methods.size()){
			if (methods.get(i).getLocalizedName().equals(localizedName))
				idx = i;
			i++;
		}
		if (idx > -1)
			methodList.setSelectedIndex(idx);
		else
			methodList.setSelectedIndex(0);
	}

	//----------------------------------------------------------------------------------------------------
	public String getSweepingMethodTitleName() {
	    return methods.get(methodList.getSelectedIndex()).toString();
    }
	
	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getInfoText(Wizard w) { return w.getArrowsHeader("Select a method to investigate the model and its parameters"); }
	public Container getPanel() { return this; }
	public void onPageChange(boolean show) {}
	public String getTitle() { return "Method selection"; }

	//----------------------------------------------------------------------------------------------------
	public boolean isEnabled(Button b) {
		if (b == Button.NEXT || b == Button.FINISH || b == Button.CUSTOM){
			if (getSweepingMethodID() > 0){
				IIntelliMethodPlugin plugin = getSweepingMethodID() == IIntelliMethodPlugin.STATIC_METHOD ? getSelectedIntelliStaticMethodPlugin()
																										  : getSelectedIntelliDynamicMethodPlugin();
				return plugin.isImplemented();
			}
		}
		return true;
	}

	//----------------------------------------------------------------------------------------------------
	public boolean onButtonPress(Button b) {
		if(getSweepingMethodID() > 0 && b == Button.NEXT)
			owner.gotoPage(3);
		return true;
	}

	//----------------------------------------------------------------------------------------------------
	public void valueChanged(ListSelectionEvent e) {
		int idx = methodList.getSelectedIndex();
		if (idx > -1)
			descriptionArea.setText(methods.get(idx).getDescription());
		else
			descriptionArea.setText(methods.get(0).getDescription());
		owner.enableDisableButtons();
    }
	
	//====================================================================================================
	// GUI methods
	
	//----------------------------------------------------------------------------------------------------
	private void layoutGUI() {
		methodList = new JList(methods);
		methodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		methodList.setName("lst_wizard_methodselection_methodlst");
		descriptionArea = new JTextArea();
		descriptionArea.setEditable(false);
		descriptionArea.setLineWrap(true);
		descriptionArea.setWrapStyleWord(true);
		descriptionArea.setColumns(40);
		JScrollPane scrollPane = new JScrollPane(descriptionArea);
		scrollPane.setBorder(BorderFactory.createEtchedBorder());
		methodList.addListSelectionListener(this);
		methodList.setSelectedIndex(0);
		methodList.setPreferredSize(new Dimension(270, 300));
		methodList.setBorder(BorderFactory.createEtchedBorder());
		content = FormsUtils.build("p ~ f:p:g", 
								   "[DialogBorder]01 p|" +
								   				 "23 f:p:g", 
								   "IntelliSweep methods:", "Description:",
								   methodList, descriptionArea).getPanel();
		this.setLayout(new BorderLayout());
		
		final JScrollPane sp = new JScrollPane(content);
    	this.add(sp,BorderLayout.CENTER);
    	this.validate();
    }
}
