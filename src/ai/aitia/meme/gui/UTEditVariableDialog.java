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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.usertools.UserToolGroup;
import ai.aitia.meme.usertools.UserToolGroup.EnvironmentVariable;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.FormsUtils.Separator;
import ai.aitia.meme.utils.OSUtils.OSType;

public class UTEditVariableDialog extends JDialog implements ActionListener,
															 CaretListener {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 1L;
	public static final int OK_OPTION = 0;
	public static final int CANCEL_OPTION = -1;
	
	private int returnValue = CANCEL_OPTION;
	private UserToolGroup group = null; // null means global variable
	private boolean editMode = false;
	
	//====================================================================================================
	// GUI members
	
	private JPanel content = null;
	private JButton okButton = new JButton("Ok");
	private JButton cancelButton = new JButton("Cancel");
	private JPanel buttonPanel = new JPanel();
	private JTextField nameField = new JTextField();
	private JTextField valueField = new JTextField();
	private JButton fileButton = new JButton("File...");
	private JButton folderButton = new JButton("Folder...");
	private JLabel referenceLabel = new JLabel("");
	private JTextArea descriptionArea = new JTextArea();
	private JScrollPane descriptionScr = new JScrollPane(descriptionArea);
	private JPanel dockPanel = new JPanel(new BorderLayout());
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public UTEditVariableDialog(Frame owner, UserToolGroup group) {
		super(owner,"Environment Variable Entry " + (group == null ? " (Global)" : "(Group: " + group + ")"),true);
		this.group = group;
		layoutGUI();
		initialize();
		setLocationRelativeTo(owner);
	}
	
	//----------------------------------------------------------------------------------------------------
	public UTEditVariableDialog(Dialog owner, UserToolGroup group) {
		super(owner,"Environment Variable Entry " + (group == null ? " (Global)" : "(Group: " + group + ")"),true);
		this.group = group;
		layoutGUI();
		initialize();
		setLocationRelativeTo(owner);
	}
	
	//----------------------------------------------------------------------------------------------------
	public int showDialog() {
		setVisible(true);
		int result = returnValue;
		dispose();
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public int showDialog(String name, String value, String description) {
		editMode = true;
		nameField.setEditable(false);
		nameField.setText(name);
		referenceLabel.setText("<html><b>%" + name + "%</b></html>");
		valueField.setText(value);
		if (description != null && description.length() > 0) {
			descriptionArea.setBorder(null);
			descriptionArea.setRows(5);
			descriptionArea.setEditable(false);
			descriptionArea.setLineWrap(true);
			descriptionArea.setWrapStyleWord(true);
			descriptionArea.setText(description);
			descriptionArea.setCaretPosition(0);
			descriptionScr.setBorder(BorderFactory.createTitledBorder("Description"));
			dockPanel.add(descriptionScr,BorderLayout.CENTER);
			pack();
		}
		return showDialog();
	}
	
	//----------------------------------------------------------------------------------------------------
	public EnvironmentVariable getVariable() {
		String name = nameField.getText().trim();
		String value = valueField.getText().trim();
		if (MEMEApp.getOSUtils().getOSType() == OSType.WINDOWS && value.contains(" "))
			value = "\"" + value + "\"";
		return new EnvironmentVariable(name,value);
	}
	
	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if ("OK".equals(command)) {
			String name = nameField.getText().trim();
			if (name.contains(" ")) {
				MEMEApp.userErrors("Warning","An environment variable name cannot contain spaces.");
				nameField.selectAll();
				nameField.grabFocus();
				return;
			} else if (!editMode && findName(group,name)) {
				String msg = group == null ? " global namespace." : group + " group.";
				int result = MEMEApp.askUser(false,"Override confirmation","Environment variable " + name + " is already defined in " + msg,
											 "Do you want to override it?");
				if (result == 0) return;
			}
			returnValue = OK_OPTION;
			setVisible(false);
		} else if ("CANCEL".equals(command)) {
			returnValue = CANCEL_OPTION;
			setVisible(false);
		} else if ("FILE".equals(command)) {
			String value = valueField.getText().trim();
			File current = value == null ? null : new File(value);
			File f = openDialog(current,false);
			if (f != null) {
				try {
					valueField.setText(f.getCanonicalPath());
					valueField.setToolTipText(f.getCanonicalPath());
				} catch (IOException e1) {
					MEMEApp.logException("UTEditVariableDialog",e1,true);
					MEMEApp.userErrors("Error","Error accessing seleted file.");
					valueField.setText("");
					valueField.setToolTipText("");
				}
			}
		} else if ("FOLDER".equals(command)) {
			String value = valueField.getText().trim();
			File current = value == null ? null : new File(value);
			File dir = openDialog(current,true);
			if (dir != null) {
				try {
					valueField.setText(dir.getCanonicalPath());
					valueField.setToolTipText(dir.getCanonicalPath());
				} catch (IOException e1) {
					MEMEApp.logException("UTEditVariableDialog",e1,true);
					MEMEApp.userErrors("Error","Error accessing seleted folder.");
					valueField.setText("");
					valueField.setToolTipText("");
				}
			}
		}
	}

	//----------------------------------------------------------------------------------------------------
	public void caretUpdate(CaretEvent e) {
		if (e.getSource().equals(nameField))
			referenceLabel.setText("<html><b>%" + nameField.getText().trim() + "%</b></html>");
		else if (e.getSource().equals(valueField))
			valueField.setToolTipText(valueField.getText());
	}

	//====================================================================================================
	// GUI methods
	
	//----------------------------------------------------------------------------------------------------
	private void layoutGUI() {
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		
		content = FormsUtils.build("~ p ~ p:g ~ p ~",
								   "|01_||" +
								   "234||" +
								   "567||" +
								   "888||" +
								   "999||" +
								   "AAA|",
								   "Name:",nameField,
								   "Value:",valueField,fileButton,
								   "Reference:",referenceLabel,folderButton,
								   dockPanel,
								   new Separator(""),
								   buttonPanel).getPanel();
		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) {
				returnValue = CANCEL_OPTION;
			}
		});
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initialize() {
		okButton.setActionCommand("OK");
		cancelButton.setActionCommand("CANCEL");
		nameField.addCaretListener(this);
		nameField.setPreferredSize(new Dimension(300,20));
		valueField.setPreferredSize(new Dimension(300,20));
		valueField.addCaretListener(this);
		fileButton.setActionCommand("FILE");
		folderButton.setActionCommand("FOLDER");
		GUIUtils.addActionListener(this,okButton,cancelButton,fileButton,folderButton);
		
		final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(null);
		this.setContentPane(sp);
		this.pack();
		Dimension oldD = this.getPreferredSize();
		this.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
										    oldD.height + sp.getHorizontalScrollBar().getHeight()));
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		oldD = this.getPreferredSize();
		final Dimension newD = GUIUtils.getPreferredSize(this);
		if (!oldD.equals(newD)) 
			this.setPreferredSize(newD);
		this.pack();
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private File openDialog(File current, boolean directoryOnly) {
		JFileChooser chooser = new JFileChooser(current == null ? MEMEApp.getLastDir() : current);
		if (directoryOnly)
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setAcceptAllFileFilterUsed(true);
		if (current != null)
			chooser.setSelectedFile(current);
		int result = chooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			MEMEApp.setLastDir(chooser.getSelectedFile());
			return chooser.getSelectedFile();
		}
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean findName(UserToolGroup group, String name) {
		EnvironmentVariable dummy = new EnvironmentVariable(name,"");
		if (group == null)
			return UserToolGroup.getGlobalVariables().contains(dummy);
		return group.getEnvironmentVariables().contains(dummy);
	}
}
