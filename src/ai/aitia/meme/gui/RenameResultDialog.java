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
package ai.aitia.meme.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

public class RenameResultDialog extends JDialog implements ActionListener, CaretListener, FocusListener {

	//====================================================================================================
	// members

	private static final long serialVersionUID = 1L;

	private final Frame owner;

	private JPanel content = new JPanel(new BorderLayout());
	private JPanel center = null;
	private JTextField modelField = new JTextField();
	private JTextField versionField = new JTextField();
	private JPanel bottom = new JPanel();
	private JButton okButton = new JButton("OK");
	private JButton cancelButton = new JButton("Cancel");
	
	private boolean cancel = false;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public RenameResultDialog(final Frame owner) {
		super(owner,"Rename Result",true);
		this.owner = owner;
		layoutGUI();
		initialize();
	}
	
	//----------------------------------------------------------------------------------------------------
	public String[] showDialog(final String oldModel, final String oldVersion) {
		setTitle("Rename Result - " + oldModel + "/" + oldVersion);
		modelField.setText(oldModel);
		versionField.setText(oldVersion);
		modelField.selectAll();
		setVisible(true);
		return cancel ? null : new String[] { modelField.getText().trim(), versionField.getText().trim() };
	}

	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		final String command = e.getActionCommand();
		if ("MODEL".equals(command)) 
			versionField.grabFocus();
		else if ("VERSION".equals(command)) {
			if (okButton.isEnabled())
				okButton.doClick(0);
		} else if ("CANCEL".equals(command)) {
			cancel = true;
			setVisible(false);
		} else if ("OK".equals(command)) {
			cancel = false;
			setVisible(false);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void caretUpdate(CaretEvent e) {
		okButton.setEnabled(modelField.getText().trim().length() > 0 && versionField.getText().trim().length() > 0);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void focusGained(FocusEvent e) {
		((JTextField)e.getSource()).selectAll();
	}

	//----------------------------------------------------------------------------------------------------
	public void focusLost(FocusEvent e) {}

	
	//====================================================================================================
	// GUI methods
	
	//----------------------------------------------------------------------------------------------------
	private void layoutGUI() {
		center = FormsUtils.build("p ~ p:g",
				   				  "[DialogBorder]0_||" +
				   				  				"11||" +
				                                "2_||" +
				                                "33|",
				                  "Please define a new model name: ",
				                  modelField,
				                  "And a new version: ",
				                  versionField).getPanel();

		bottom.add(okButton);
		bottom.add(cancelButton);

		content.add(center,BorderLayout.CENTER);
		content.add(bottom,BorderLayout.SOUTH);
		
		this.setContentPane(content);
		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				cancel = true;
			}
		});
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initialize() {
		modelField.setActionCommand("MODEL");
		versionField.setActionCommand("VERSION");
		okButton.setActionCommand("OK");
		cancelButton.setActionCommand("CANCEL");
		
		GUIUtils.addActionListener(this,modelField,versionField,okButton,cancelButton);
		modelField.addCaretListener(this);
		modelField.addFocusListener(this);
		versionField.addCaretListener(this);
		versionField.addFocusListener(this);
		
		this.getRootPane().setDefaultButton(okButton);
		this.pack();
		this.setLocationRelativeTo(owner);
	}
}
