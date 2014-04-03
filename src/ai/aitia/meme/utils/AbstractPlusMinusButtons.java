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
package ai.aitia.meme.utils;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public abstract class AbstractPlusMinusButtons implements ActionListener{

	private static final String PLUS_PRESSED = "plus button pressed";
	private static final String MINUS_PRESSED = "minus button pressed";
	protected JButton plusButton = null;
	protected JButton minusButton = null;
	protected JPanel guiPanel = null;
	protected JLabel captionLabel = null;
	
	public AbstractPlusMinusButtons(String caption) {
		this();
		captionLabel.setText(caption);
	}
	
	public AbstractPlusMinusButtons() {
		captionLabel = new JLabel();
		plusButton = new JButton("+");
		minusButton = new JButton("-");
		guiPanel = new JPanel(new GridLayout(0,1));
		guiPanel.add(captionLabel);
		guiPanel.add(plusButton);
		guiPanel.add(minusButton);
		plusButton.setActionCommand(PLUS_PRESSED);
		minusButton.setActionCommand(MINUS_PRESSED);
		GUIUtils.addActionListener(this, plusButton, minusButton);
	}
	
	public void setPlusEnabled(boolean flag) {
		plusButton.setEnabled(flag);
	}
	public void setMinusEnabled(boolean flag) {
		minusButton.setEnabled(flag);
	}
	
	public abstract void plusButtonPressed();
	public abstract void minusButtonPressed();

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(PLUS_PRESSED)) {
			plusButtonPressed();
		} else if (e.getActionCommand().equals(MINUS_PRESSED)) {
			minusButtonPressed();
		}
	}
	
	public JPanel getPanel() {
		return guiPanel;
	}
}
