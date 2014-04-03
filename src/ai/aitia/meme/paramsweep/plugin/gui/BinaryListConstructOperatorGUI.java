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

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import ai.aitia.meme.paramsweep.gui.ScriptCreationDialog;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin.OperatorGUIType;
import ai.aitia.meme.paramsweep.plugin.gui.NumberParameterGUI.TooltipRenderer;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;
import ai.aitia.meme.paramsweep.utils.MemberInfoList;
import ai.aitia.meme.utils.FormsUtils;

public class BinaryListConstructOperatorGUI extends JPanel implements IOperatorGUI {
	
	//=====================================================================================
	// members
	private static final long serialVersionUID = 1L;
	
	/** The list of information objects of appropriate members of the
	 * 	model that can be used in the statistic instance.
	 */
	private MemberInfoList availableMembers = null;
	/** The model of the combobox. */
	private DefaultComboBoxModel model = null;
	private DefaultComboBoxModel model2 = null;
	/** The dialog that contains this component. */
	private ScriptCreationDialog parent = null;
	
	//=====================================================================================
	// GUI members
	
	private JPanel content = null;
	private JComboBox combo = new JComboBox();
	private JComboBox combo2 = new JComboBox();
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Constructor.
	 * @param parent the dialog that contains this component
	 */
	public BinaryListConstructOperatorGUI(ScriptCreationDialog parent, MemberInfoList availableMembers) {
		super();
		this.parent = parent;
		this.availableMembers = availableMembers;
		model = new DefaultComboBoxModel();
		model2 = new DefaultComboBoxModel();
		layoutGUI();
		initializeComboBoxes();
	}
	
	//=====================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public JPanel getGUIComponent() { return this; }
	public OperatorGUIType getSupportedOperatorType() { return OperatorGUIType.BINARY_LIST_CONSTRUCT; }
	
	//----------------------------------------------------------------------------------------------------
	public String checkInput() {
		if (combo.getSelectedIndex() == -1 || combo2.getSelectedIndex() == -1)
			return "no selected list/array";
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	public Object[] getInput() {
		MemberInfo info = (MemberInfo) combo.getSelectedItem();
		MemberInfo info2 = (MemberInfo) combo2.getSelectedItem();
		return new Object[] { info, info2 };
	}

	public void buildContent(List<? extends Object> buildBlock) throws CannotLoadDataSourceForEditingException {
		// there is only two MemberInfos in the list (otherwise this is an invalid state)
		if (buildBlock.size() == 2) {
			MemberInfo memberInfo = (MemberInfo) buildBlock.get(0);
			if (model.getIndexOf(memberInfo) == -1)
				throw new CannotLoadDataSourceForEditingException(memberInfo.getName() + " is missing!");
			combo.setSelectedItem(memberInfo);
			memberInfo = (MemberInfo) buildBlock.get(1);
			if (model.getIndexOf(memberInfo) == -1)
				throw new CannotLoadDataSourceForEditingException(memberInfo.getName() + " is missing!");
			combo2.setSelectedItem(memberInfo);
			return;
		}
		throw new IllegalStateException("too many MemberInfo");
	}
	
	//=====================================================================================
	// GUI methods
	
	//-------------------------------------------------------------------------------------
	private void layoutGUI() {
		content = FormsUtils.build("p ~ p:g",
								   "01||" +
								   "23",
								   "First list/array: ",combo,
								   "Second list/array: ",combo2).getPanel();
		combo.setRenderer(new TooltipRenderer(combo));
		combo2.setRenderer(new TooltipRenderer(combo2));
		combo.setModel(model);
		combo2.setModel(model2);
		int width = Math.min(parent.getAvailableWidth(),combo.getSize().width);
		width = Math.min(width,combo2.getSize().width);
		combo.setPreferredSize(new Dimension(width,26));
		combo.setMaximumSize(new Dimension(1950,30));
		combo2.setPreferredSize(new Dimension(width,26));
		combo2.setMaximumSize(new Dimension(1950,30));
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
	private void initializeComboBoxes() {
		if (availableMembers != null) {
			List<MemberInfo> members = availableMembers.filterOneDimListOnly();
			for (MemberInfo mi : members) {
				model.addElement(mi);
				model2.addElement(mi);
			}
		}
	}
}
