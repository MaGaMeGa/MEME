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
package ai.aitia.meme.paramsweep.intellisweepPlugin.utils;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

public abstract class CustomThreeLevelDesignSettingPanel extends JPanel implements ActionListener, ListSelectionListener{
	
	private static final String MODIFY_INFO = "MODIFY_INFO";
	protected JPanel content = null;
	protected JLabel designListLabel = new JLabel("Selected parameters:");
	protected JLabel lowValueLabel = new JLabel("Low:");
	protected JLabel highValueLabel = new JLabel("High:");
	protected JLabel optionalValueLabel = new JLabel("Center:");
	protected JCheckBox optionalValueCheckBox = new JCheckBox("Specify center value");
	protected JList designList = null;
	protected JScrollPane designScr = null;
	protected JButton okButton = new JButton("OK");
	protected JTextField lowValueField = new JTextField(10);
	protected JTextField highValueField = new JTextField(10);
	protected JTextField optionalValueField = new JTextField(10);
	protected ListCellRenderer cellRenderer = null;

	public CustomThreeLevelDesignSettingPanel(Vector<? extends ParameterInfo> infos, ListCellRenderer cellRenderer, JPanel extraPanel) {
		super(new BorderLayout());
		this.cellRenderer = cellRenderer;
		designScr = new JScrollPane();
		updateList(infos);
		okButton.setActionCommand(MODIFY_INFO);
		GUIUtils.addActionListener(this, okButton);
		content = FormsUtils.build(
				"f:p:g ~ p ~ p",
				"000 p|" +
				"1__ p|" +
				"2__ f:p:g|" +
				"234 p||" +
				"256 p||" +
				"277 p||" +
				"289 p||" +
				"2AA p||" +
				"2__ f:p:g||",
				extraPanel,
				designListLabel,
				designScr, lowValueLabel, lowValueField,
				highValueLabel, highValueField,
				optionalValueCheckBox,
				optionalValueLabel, optionalValueField,
				okButton).getPanel();
		this.add(content, BorderLayout.CENTER);
	}
	
	public CustomThreeLevelDesignSettingPanel(Vector<? extends ParameterInfo> infos,
			ListCellRenderer cellRenderer,
			JPanel extraPanel,
			String designListLabelText,
			String lowValueLabelText,
			String highValueLabelText,
			boolean optionalPartVisible,
			boolean optionalCheckBoxVisible,
			String optionalCheckBoxText,
			String optionalValueLabelText
			) {
		this(infos, cellRenderer, extraPanel);
		optionalValueCheckBox.setVisible(optionalCheckBoxVisible && optionalPartVisible);
		optionalValueField.setVisible(optionalPartVisible);
		optionalValueLabel.setVisible(optionalPartVisible);
		designListLabel.setText(designListLabelText);
		lowValueLabel.setText(lowValueLabelText);
		highValueLabel.setText(highValueLabelText);
		if(optionalPartVisible){
			optionalValueLabel.setText(optionalValueLabelText);
			if(optionalCheckBoxVisible){
				optionalValueCheckBox.setText(optionalCheckBoxText);
			} 
		}
	}
	
	public void updateList(Vector<? extends ParameterInfo> infos){
		designList = new JList(infos);
		designList.setCellRenderer(cellRenderer);
		designScr.setViewportView(designList);
		designList.addListSelectionListener(this);
	}
	
	public abstract void controlFiller();
	public abstract void infoFiller();

	public JTextField getHighValueField() {
    	return highValueField;
    }

	public JLabel getHighValueLabel() {
    	return highValueLabel;
    }

	public JTextField getLowValueField() {
    	return lowValueField;
    }

	public JLabel getLowValueLabel() {
    	return lowValueLabel;
    }

	public JCheckBox getOptionalValueCheckBox() {
    	return optionalValueCheckBox;
    }

	public JTextField getOptionalValueField() {
    	return optionalValueField;
    }

	public JLabel getOptionalValueLabel() {
    	return optionalValueLabel;
    }

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(MODIFY_INFO)){
			infoFiller();
		}
	}
	
	public void valueChanged(ListSelectionEvent e) {
		if(e.getSource().equals(designList)){
			controlFiller();
		}
	}
	
	
}
