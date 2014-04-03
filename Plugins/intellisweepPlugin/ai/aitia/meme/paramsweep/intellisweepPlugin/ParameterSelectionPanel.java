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
package ai.aitia.meme.paramsweep.intellisweepPlugin;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

public abstract class ParameterSelectionPanel implements ActionListener, ListSelectionListener {

	private static final String TO_TOP = "to_top";
	private static final String ONE_UP = "one_up";
	private static final String ONE_DOWN = "one_down";
	private static final String TO_BOTTOM = "to_bottom";
	private static final String CONST_SET = "const_value_set";
	private static final String MOVE_TO_DESIGN = "move_to_design";
	private static final String MOVE_FROM_DESIGN = "move_from_design";
	private static final String CONST_FIELD_ENTER = "constant_value_field_enter_pressed";
	private static final String NEW_PARAMETERS = "new parameters";
	protected ListDataListener listDataListener = null;

	// private ArrayList<? extends ParameterInfo> constantParameters = null;
	// private ArrayList<? extends ParameterInfo> designTableParameters = null;

	private JPanel content = null;
	private JList constantList = null;
	private JList designList = null;
	private JButton moveToDesignButton = new JButton("->");
	private JButton moveFromDesignButton = new JButton("<-");
	private JLabel constantLabel = new JLabel("Constant parameters:");
	private JLabel designLabel = new JLabel("Selected parameters:");
	private JLabel constantValueLabel = new JLabel("Constant value:");
	private JTextField constantValueField = new JTextField(10);
	private JScrollPane constantScr = new JScrollPane();
	protected JButton newParametersButton = new JButton("Add new parameters...");
	private JScrollPane designScr = new JScrollPane();
	private JButton constantValueSetButton = new JButton("Set");
	private JButton toTopButton = new JButton("To top");
	private JButton oneUpButton = new JButton("One up");
	private JButton oneDownButton = new JButton("One down");
	private JButton toBottomButton = new JButton("To bottom");
	
	public ParameterSelectionPanel() {
		init();
	}

	protected void init() {
		constantList = new JList(getConstantVector());
		designList = new JList(getDesignVector());
		constantScr = new JScrollPane(constantList);
		designScr = new JScrollPane(designList);
		constantScr.setPreferredSize(new Dimension(160, 140));
		designScr.setPreferredSize(new Dimension(110, 140));
		content = FormsUtils.build(
		        "~ f:p:g ~ p ~ f:p:g ~ p ~",
		        "012_ p|" + 
		        "3456 p|" + 
		        "3758 p|" + 
		        "3_59 p|" + 
		        "3A5B p|" + 
		        "3C5_ p||" + 
		        "3_5_ f:p:g||" + 
		        "D_5_ p||", 
		        constantLabel, constantValueLabel, designLabel, 
		        constantScr,constantValueField, designScr, toTopButton,
		        constantValueSetButton, 
		        oneUpButton, oneDownButton,
		        moveToDesignButton, toBottomButton, moveFromDesignButton, newParametersButton)
		        .getPanel();
		toTopButton.setActionCommand(TO_TOP);
		toBottomButton.setActionCommand(TO_BOTTOM);
		oneUpButton.setActionCommand(ONE_UP);
		oneDownButton.setActionCommand(ONE_DOWN);
		constantValueSetButton.setActionCommand(CONST_SET);
		moveToDesignButton.setActionCommand(MOVE_TO_DESIGN);
		moveFromDesignButton.setActionCommand(MOVE_FROM_DESIGN);
		constantValueField.setActionCommand(CONST_FIELD_ENTER);
		newParametersButton.setActionCommand(NEW_PARAMETERS);
		GUIUtils.addActionListener(this, toTopButton, toBottomButton,
		        oneUpButton, oneDownButton, constantValueSetButton,
		        moveToDesignButton, moveFromDesignButton, constantValueField, newParametersButton);
		constantList.addListSelectionListener(this);
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals(MOVE_TO_DESIGN)) {
			moveParametersToDesign(constantList.getSelectedIndices());
			updateLists();
		} else if (command.equals(MOVE_FROM_DESIGN)) {
			moveParametersFromDesign(designList.getSelectedIndices());
			updateLists();
		} else if (command.equals(ONE_UP)) {
			int[] dest = moveParametersOneUp(designList.getSelectedIndices());
			updateLists();
			designList.setSelectedIndices(dest);
		} else if (command.equals(ONE_DOWN)) {
			int[] dest = moveParametersOneDown(designList.getSelectedIndices());
			updateLists();
			designList.setSelectedIndices(dest);
		} else if (command.equals(TO_TOP)) {
			int[] dest = moveParametersToTop(designList.getSelectedIndices());
			updateLists();
			designList.setSelectedIndices(dest);
		} else if (command.equals(TO_BOTTOM)) {
			int[] dest = moveParametersToBottom(designList.getSelectedIndices());
			updateLists();
			designList.setSelectedIndices(dest);
		} else if(command.equals(CONST_SET)) {
			setConstantValueFromField();
		} else if(command.equals(CONST_FIELD_ENTER)) {
			constantValueSetButton.doClick();
		} else if(command.equals(NEW_PARAMETERS)) {
			newParametersPressed();
			updateLists();
		}
	}

	private void setConstantValueFromField() {
		String value = constantValueField.getText().trim();
		if(constantList.getSelectedIndex() != -1){
			ParameterInfo info = getConstantVector().get(constantList.getSelectedIndex());
			Object newValue = ParameterInfo.getValue(value, info.getType());
			if(newValue != null){
				info.setValue(newValue);
				constantScr.setVisible(false);
				constantScr.setVisible(true);
				if(constantList.getSelectedIndices().length == 1){
					if(constantList.getSelectedIndex() < getConstantVector().size() - 1){
						constantList.setSelectedIndex(constantList.getSelectedIndex() + 1);
					} else {
						constantList.setSelectedIndex(0);					
					}
					selectAndFocusTextField(constantValueField);
				}
			} else {
				//TODO: warn user, that the value entered is incompatible with the type
			}
		}
    }

    private void selectAndFocusTextField(JTextField which) {
        which.requestFocusInWindow();
        which.setSelectionStart(0);
        which.setSelectionEnd(which.getText().length());
    }

    private int[] moveParametersToTop(int[] selectedIndices) {
		for (int i = 0; i < selectedIndices.length; i++) {
			ParameterInfo temp = getDesignVector().get(selectedIndices[i]);
			getDesignVector().remove(selectedIndices[i]);
			getDesignVector().add(i, temp);
		}
		int[] destinationIdxs = new int[selectedIndices.length];
		for (int i = 0; i < selectedIndices.length; i++) {
			destinationIdxs[i] = i;
		}
		return destinationIdxs;
	}

	private int[] moveParametersToBottom(int[] selectedIndices) {
		for (int i = 0; i < selectedIndices.length; i++) {
			ParameterInfo temp = getDesignVector().get(selectedIndices[i]);
			getDesignVector().add(temp);
		}
		for (int i = selectedIndices.length - 1; i >= 0; i--) {
			getDesignVector().remove(selectedIndices[i]);
		}
		int[] destinationIdxs = new int[selectedIndices.length];
		for (int i = 0; i < selectedIndices.length; i++) {
			destinationIdxs[i] = getDesignVector().size() - selectedIndices.length + i;
		}
		return destinationIdxs;
	}

	private int[] moveParametersOneUp(int[] selectedIndices) {
		int[] destinationIdxs = new int[selectedIndices.length];
		int destinationOfPreviousMove = -1;
		for (int i = 0; i < selectedIndices.length; i++) {
			if (selectedIndices[i] > 0) {
				if (selectedIndices[i] - 1 > destinationOfPreviousMove) {
					ParameterInfo temp = getDesignVector().get(
					        selectedIndices[i]);
					getDesignVector().remove(selectedIndices[i]);
					getDesignVector().add(selectedIndices[i] - 1, temp);
					destinationOfPreviousMove = selectedIndices[i] - 1;
				} else {
					destinationOfPreviousMove = selectedIndices[i];
				}
			} else {
				destinationOfPreviousMove = 0;
			}
			destinationIdxs[i] = destinationOfPreviousMove;
		}
		return destinationIdxs;
	}

	private int[] moveParametersOneDown(int[] selectedIndices) {
		int[] destinationIdxs = new int[selectedIndices.length];
		int destinationOfPreviousMove = getDesignVector().size();
		for (int i = selectedIndices.length - 1; i >= 0; i--) {
			if (selectedIndices[i] < getDesignVector().size() - 1) {
				if (selectedIndices[i] + 1 < destinationOfPreviousMove) {
					ParameterInfo temp = getDesignVector().get(
					        selectedIndices[i]);
					getDesignVector().remove(selectedIndices[i]);
					getDesignVector().add(selectedIndices[i] + 1, temp);
					destinationOfPreviousMove = selectedIndices[i] + 1;
				} else {
					destinationOfPreviousMove = selectedIndices[i];
				}
			} else {
				destinationOfPreviousMove = getDesignVector().size() - 1;
			}
			destinationIdxs[i] = destinationOfPreviousMove;
		}
		return destinationIdxs;
	}

	/**
	 * Updates the lists from the actual Vectors returned by getConstantVector()
	 * and getDesignVector().
	 */
	private void updateLists() {
		constantList = new JList(getConstantVector());
		constantList.addListSelectionListener(this);
		designList = new JList(getDesignVector());
		constantScr.setViewportView(constantList);
		designScr.setViewportView(designList);
		if(listDataListener != null){
			designList.getModel().addListDataListener(listDataListener);
			listDataListener.contentsChanged(new ListDataEvent(designList.getModel(), ListDataEvent.CONTENTS_CHANGED, 0, 1));
		}
	}

	//ListSelectionListener implementation:
	public void valueChanged(ListSelectionEvent e) {
		if(constantList.getSelectedIndex() > -1){
			constantValueField.setText(getConstantVector().get(constantList.getSelectedIndex()).getValue().toString());
			selectAndFocusTextField(constantValueField);
		}
    }

	public JPanel getPanel() {
		return content;
	}
	
	public void addDesignListDataListener(ListDataListener ldl){
		listDataListener = ldl;
		designList.getModel().addListDataListener(ldl);
	}
	public void removeDesignListDataListener(ListDataListener ldl){
		designList.getModel().removeListDataListener(ldl);
	}

	/**
	 * Moves the parameters with the specified indices from the constant
	 * parameter list to the 'selected for inspection' design list
	 * 
	 * @param idx
	 */
	protected abstract void moveParametersToDesign(int[] idxs);

	/**
	 * @param idx
	 */
	protected abstract void moveParametersFromDesign(int[] idxs);

	public abstract <T extends ParameterInfo> Vector<T> getConstantVector();

	public abstract <T extends ParameterInfo> Vector<T> getDesignVector();
	
	protected abstract void newParametersPressed();
	

}
