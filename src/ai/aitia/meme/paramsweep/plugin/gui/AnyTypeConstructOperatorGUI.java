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
package ai.aitia.meme.paramsweep.plugin.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import ai.aitia.meme.paramsweep.gui.ScriptCreationDialog;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin.OperatorGUIType;
import ai.aitia.meme.paramsweep.plugin.gui.NumberParameterGUI.TooltipRenderer;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;
import ai.aitia.meme.paramsweep.utils.MemberInfoList;

public class AnyTypeConstructOperatorGUI extends JPanel implements IOperatorGUI {
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	public static enum UseType { SIZE, PERMUTATION, TIMESERIES }; 

	//=====================================================================================
	// members
	private static final long serialVersionUID = 1L;
	
	/** The list of information objects of appropriate members of the
	 * 	model that can be used in the statistic instance.
	 */
	private MemberInfoList availableMembers = null;
	/** The model of the combobox. */
	private DefaultComboBoxModel model = null;
	/** The dialog that contains this component. */
	private ScriptCreationDialog parent = null;
	private UseType type = null;
	
	//=====================================================================================
	// GUI members
	
	private JPanel content = new JPanel();
	private JComboBox combo = new JComboBox();
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Constructor.
	 * @param parent the dialog that contains this component
	 */
	public AnyTypeConstructOperatorGUI(ScriptCreationDialog parent, MemberInfoList availableMembers, UseType type) {
		super();
		this.parent = parent;
		this.availableMembers = availableMembers;
		this.type = type;
		model = new DefaultComboBoxModel();
		layoutGUI();
		initializeComboBox();
	}
	
	//=====================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public JPanel getGUIComponent() { return this; }
	
	//----------------------------------------------------------------------------------------------------
	public OperatorGUIType getSupportedOperatorType() { 
		switch (type) {
		case SIZE		: return OperatorGUIType.SIZE;
		case TIMESERIES : return OperatorGUIType.TIME_SERIES;
		default			: return OperatorGUIType.ANY_TYPE_CONSTRUCT;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public String checkInput() {
		if (combo.getSelectedIndex() == -1) {
			switch (type) {
			case SIZE 			: return "no selected object";
			case TIMESERIES		: return "no selected scalar object"; 
			case PERMUTATION	: return "no selected list/" + ((PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? "agentset" : "array");
			}
		}
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	public Object[] getInput() {
		MemberInfo info = (MemberInfo) combo.getSelectedItem();
		return new Object[] { info };
	}
	
	//----------------------------------------------------------------------------------------------------
	public void buildContent(List<? extends Object> buildBlock) throws CannotLoadDataSourceForEditingException {
		// there is only one MemberInfo in the list (otherwise this is an invalid state)
		if (buildBlock.size() == 1) {
			MemberInfo memberInfo = (MemberInfo) buildBlock.get(0);
			if (model.getIndexOf(memberInfo) == -1)
				throw new CannotLoadDataSourceForEditingException(memberInfo.getName() + " is missing!");
			combo.setSelectedItem(memberInfo);
			return;
		}
		throw new IllegalStateException("too many MemberInfo");
	}
	
	//=====================================================================================
	// GUI methods
	
	//-------------------------------------------------------------------------------------
	private void layoutGUI() {
		content.setLayout(new BoxLayout(content,BoxLayout.X_AXIS));
		content.add(new JLabel(type == UseType.SIZE || type == UseType.TIMESERIES ? "Select an object: " : "Select a list/" + 
				              ((PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5)? "agentset: " : "array: ")));
		content.add(combo);
		combo.setRenderer(new TooltipRenderer(combo));
		combo.setModel(model);
		int width = Math.min(parent.getAvailableWidth(),combo.getSize().width);
		combo.setPreferredSize(new Dimension(width,26));
		combo.setMaximumSize(new Dimension(1950,30));
		setLayout(new BorderLayout());
		add(content,BorderLayout.NORTH);
		this.setPreferredSize(new Dimension(500,26));
		this.setMaximumSize(new Dimension(2000,30));
		this.setVisible(true);
	}
	
	//=====================================================================================
	// private methods
	
	//-------------------------------------------------------------------------------------
	/** Initializes the combobox from <code>availableMembers</code>. */
	private void initializeComboBox() {
		if (availableMembers != null) {
			List<MemberInfo> list = null;
			if (type == UseType.SIZE)
				list = availableMembers.filterInvalids();
			else if (type == UseType.TIMESERIES)
				list = availableMembers.filterScalars();
			else 
				list = availableMembers.filterOneDimListOnly();
			for (MemberInfo mi : list)
				model.addElement(mi);
		}
	}

}
