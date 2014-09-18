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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ai.aitia.meme.paramsweep.gui.ScriptCreationDialog;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin.OperatorGUIType;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;
import ai.aitia.meme.paramsweep.utils.MemberInfoList;
import ai.aitia.meme.utils.FormsUtils;

public class TimeSeriesOperatorGUI extends JPanel implements IOperatorGUI {
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	private static final long serialVersionUID = 1L;
	
	//====================================================================================================
	// GUI members
	
	protected JPanel content = null;
	protected JComboBox<MemberInfo> memberBox = new JComboBox<MemberInfo>();
	protected JCheckBox limitBox = new JCheckBox("Limit");
	protected JLabel maxLengthLabel = new JLabel("Maximum length: "); 
//	protected JTextField maxLengthField = new JTextField();
	protected JSpinner maxLengthField = new JSpinner(new SpinnerNumberModel(100, 1, null, 1));
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public TimeSeriesOperatorGUI(ScriptCreationDialog parent, MemberInfoList availableMembers) {
		super();
		limitBox.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				if (limitBox.isSelected()){
					maxLengthField.setEnabled(true);
				} else {
					maxLengthField.setEnabled(false);
				}
			}
		});
		layoutGUI();
		initialize();
		initializeCollectionBox(availableMembers);
	}
	
	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public JPanel getGUIComponent() { return this; }

	//----------------------------------------------------------------------------------------------------
	public OperatorGUIType getSupportedOperatorType() {
		return OperatorGUIType.TIME_SERIES;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String checkInput() {
		if (memberBox.getSelectedIndex() == -1)
			return "no selected member";
		
		return null;
	}

	//----------------------------------------------------------------------------------------------------
	public Object[] getInput() {
		List<Object> result = new ArrayList<Object>();
		MemberInfo selectedItem = (MemberInfo) memberBox.getSelectedItem();
		result.add(selectedItem);

		if (limitBox.isSelected()){
			try {
				maxLengthField.commitEdit();
			} catch (ParseException e) {
				// the entered value is invalide, set it to the last correct value that is still present in the model of JSpinner
				((DefaultEditor)maxLengthField.getEditor()).getTextField().setValue(maxLengthField.getValue());
			}
			result.add(maxLengthField.getValue());
		} else {
			result.add(-1);
		}
		
		return result.toArray();
	}

	//----------------------------------------------------------------------------------------------------
	public void buildContent(List<? extends Object> buildBlock) throws CannotLoadDataSourceForEditingException {
		DefaultComboBoxModel<MemberInfo> collectionModel = (DefaultComboBoxModel<MemberInfo>) memberBox.getModel();
		MemberInfo memberInfo = (MemberInfo) buildBlock.get(0);
		if (collectionModel.getIndexOf(memberInfo) == -1)
			throw new CannotLoadDataSourceForEditingException(memberInfo.getName() + " is missing");
		memberBox.setSelectedItem(memberInfo);
		
		if (buildBlock.size() > 1){
			Integer limit = (Integer)buildBlock.get(1);
			limitBox.setSelected(true);
			maxLengthField.setValue(limit);
		}
	}

	//====================================================================================================
	// GUI methods
	
	//----------------------------------------------------------------------------------------------------
	private void layoutGUI() {
		content = FormsUtils.build("p ~ p:g",
								   "01||" +
								   "22||" +
								   "34",
								   "Select an object: ",memberBox,
								   limitBox,
								   maxLengthLabel, maxLengthField
									   ).getPanel();
		setLayout(new BorderLayout());
		add(content,BorderLayout.CENTER);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initialize() {
//		content.setBorder(BorderFactory.createTitledBorder(opName));
		//setPreferredSize(new Dimension(550,200));
		setVisible(true);
		limitBox.setSelected(false);
		maxLengthField.setEnabled(false);
	}
	
	//====================================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private void initializeCollectionBox(MemberInfoList allMembers) {
		List<MemberInfo> list = allMembers.filterScalars();
		DefaultComboBoxModel<MemberInfo> model = new DefaultComboBoxModel<MemberInfo>(list.toArray(new MemberInfo[0]));
		memberBox.setModel(model);
	}
	
}
