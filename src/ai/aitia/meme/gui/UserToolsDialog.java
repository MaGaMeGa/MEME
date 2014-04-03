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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.usertools.UserToolGroup;
import ai.aitia.meme.usertools.UserToolManager;
import ai.aitia.meme.usertools.UserToolParser;
import ai.aitia.meme.usertools.UserToolGroup.EnvironmentVariable;
import ai.aitia.meme.usertools.UserToolGroup.GroupIsFullException;
import ai.aitia.meme.usertools.UserToolGroup.ToolType;
import ai.aitia.meme.usertools.UserToolGroup.UserToolItem;
import ai.aitia.meme.usertools.UserToolParser.Argument;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.FormsUtils.Separator;
import ai.aitia.meme.utils.OSUtils.OSType;

public class UserToolsDialog extends JDialog implements ActionListener,
														ListSelectionListener,
														FocusListener {
	
	//=====================================================================================
	// nested classes
	
	public static class UTListSelectionModel extends DefaultListSelectionModel {
		private static final long serialVersionUID = 1L;
		private int oldSelected = -1;
		public int getOldSelected() { return oldSelected; }
		@Override
		public void setSelectionInterval(int index0, int index1) {
			oldSelected = getMinSelectionIndex();
			super.setSelectionInterval(index0, index1);
		}
	}

	//=====================================================================================
	// members
	
	private static final long serialVersionUID = 1L;
	
	private UserToolManager manager = null;
	private boolean canceled = false;
	
	private ComboBoxModel groupModel = null;
	private DefaultListModel toolsModel = null;
	private JTextField activeField = null;
	
	//=====================================================================================
	// GUI members
	
	private JComboBox groupBox = new JComboBox();
	private JButton changeButton = new JButton("Change name...");
	private JList toolsList = new JList();
	private JScrollPane toolsListScr = new JScrollPane(toolsList);
	private JButton addButton = new JButton("Add tool",MainWindow.getIcon("rightarrow.png"));
	private JButton removeButton = new JButton("Remove tool");
	private JButton moveUpButton = new JButton("Move up");
	private JButton moveDownButton = new JButton("Move down");
	private JButton variableDialogButton = new JButton("Define variables...");
	private JTextField menuTextField = new JTextField();
	private JTextField commandField = new JTextField();
	private JButton browseButton = new JButton("Browse...");
	private JLabel argumentsLabel = new JLabel("Arguments:");
	private JTextField argumentsField = new JTextField();
	private JButton insertButton = new JButton(MainWindow.getIcon("downarrow.png"));
	private JPanel bottomButtonLine = null;
	private JButton insertVariableButton = new JButton("Insert variable",MainWindow.getIcon("rightarrow.png"));
	private JPanel selectedToolPanel = null;
	private JButton okButton = new JButton("Ok");
	private JButton cancelButton = new JButton("Cancel");
	private JPanel buttonsPanel = null;
	private JPanel content = null;
	
	private JPopupMenu addMenu = null;
	private JMenuItem addProgram = new JMenuItem("Program");
	private JMenuItem addDocument = new JMenuItem("Document");
	
	private JPopupMenu argumentsMenu = null;
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	public UserToolsDialog(JFrame owner, UserToolManager manager, int groupIndex) {
		super(owner,true);
		this.manager = manager;
		layoutGUI();
		initialize(groupIndex);
		setLocationRelativeTo(owner);
	}
	
	//-------------------------------------------------------------------------------------
	public boolean start() {
		setVisible(true);
		dispose();
		return !canceled;
	}
	
	//=====================================================================================
	// implemented interfaces

	//-------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if ("GROUP".equals(command)) {
			UserToolGroup grp = (UserToolGroup) groupModel.getSelectedItem();
			toolsModel = new DefaultListModel();
			List<UserToolItem> tools = grp.getTools();
			for (UserToolItem item : tools)
				toolsModel.addElement(item);
			toolsList.setModel(toolsModel);
			if (tools.size() > 0)
				toolsList.setSelectedIndex(0);
			else
				clearSelection();
		} else if ("CHANGE".equals(command)) {
			UserToolGroup grp = (UserToolGroup)groupModel.getSelectedItem();
			String result = (String) JOptionPane.showInputDialog(UserToolsDialog.this,"Enter the new name.","Rename User Tool Group",
																JOptionPane.PLAIN_MESSAGE,null,null,grp.toString());
			if (result != null) {
				if (grp.toString().equals(result)) return;
				if (manager.isUsedGroupName(result))
					MEMEApp.userErrors("Warning","This group name is already used.");
				else {
					grp.setName(result);
					groupBox.setSelectedItem(grp);
				}
			}
		} else if ("ADD".equals(command)) 
			getAddMenu().show(addButton,75,5);
		else if ("ADD_PROGRAM".equals(command)) {
			UserToolGroup grp = (UserToolGroup) groupModel.getSelectedItem();
			String defaultName =  grp.createDefaultName(ToolType.PROGRAM,"New Program");
			UserToolItem item = new UserToolItem(ToolType.PROGRAM,defaultName);
			addTool(grp,item);
		} else if ("ADD_DOCUMENT".equals(command)) {
			UserToolGroup grp = (UserToolGroup) groupModel.getSelectedItem();
			String defaultName =  grp.createDefaultName(ToolType.DOCUMENT,"New Document");
			UserToolItem item = new UserToolItem(ToolType.DOCUMENT,defaultName);
			addTool(grp,item);
		} else if ("REMOVE".equals(command)) {
			int index = toolsList.getSelectedIndex();
			if (index != -1) {
				UserToolGroup grp = (UserToolGroup) groupModel.getSelectedItem();
				toolsModel.remove(index);
				grp.remove(index);
				if (index != 0) 
					toolsList.setSelectedIndex(index - 1);
				else if (toolsModel.size() > 0)
					toolsList.setSelectedIndex(0);
			}
		} else if ("UP".equals(command)) {
			int index = toolsList.getSelectedIndex();
			if (index != -1) {
				UserToolGroup grp = (UserToolGroup) groupModel.getSelectedItem();
				grp.moveUp(index);
				if (index == 0) return;
				UserToolItem tool = (UserToolItem) toolsModel.remove(index);
				toolsModel.add(index - 1,tool);
				toolsList.setSelectedValue(tool,true);
			}
		} else if ("DOWN".equals(command)) {
			int index = toolsList.getSelectedIndex();
			if (index != -1) {
				UserToolGroup grp = (UserToolGroup) groupModel.getSelectedItem();
				grp.moveDown(index);
				if (index == toolsModel.size() - 1) return;
				UserToolItem origTool = (UserToolItem) toolsModel.get(index);
				UserToolItem nextTool = (UserToolItem) toolsModel.remove(index+1);
				toolsModel.add(index,nextTool);
				toolsList.setSelectedValue(origTool,true);
			}
		} else if ("MENU_TEXT".equals(command)) 
			commandField.grabFocus();
		else if ("COMMAND".equals(command)) {
			UserToolItem tool = (UserToolItem) toolsList.getSelectedValue();
			(tool.isProgram() ? argumentsField : okButton).grabFocus();
		} else if ("BROWSE".equals(command)) {
			String cmd = commandField.getText().trim();
			File file = null;
			if (cmd != null) {
				try {
					file = new File(cmd);
					if (!file.exists())
						file = null;
				} catch (Exception e1) { file = null; }
			}
			if (file == null) 
				file = MEMEApp.getLastDir();
			UserToolItem tool = (UserToolItem) toolsList.getSelectedValue();
			SimpleFileFilter filter = tool.isProgram() ? 
									  MEMEApp.getOSUtils().getOSType().equals(OSType.WINDOWS) ? new SimpleFileFilter("Executable Files (*.exe;*.bat;*.cmd)") : null :
									  new SimpleFileFilter("Document Files (*.pdf;*.txt;*.htm;*.html)");	  
			
			JFileChooser chooser = new JFileChooser(file);
			if (file != null && file.isFile())
				chooser.setSelectedFile(file);
			chooser.setAcceptAllFileFilterUsed(true);
			if (filter != null)
				chooser.setFileFilter(filter);
			int result = chooser.showDialog(UserToolsDialog.this,"Select");
			if (result == JFileChooser.APPROVE_OPTION) {
				File selected = chooser.getSelectedFile();
				MEMEApp.setLastDir(selected);
				commandField.setText(selected.getAbsolutePath());
				commandField.grabFocus();
			}
		} else if ("ARGS".equals(command)) 
			okButton.grabFocus();
		else if ("INSERT".equals(command)) 
			getArgumentsMenu().show(insertButton,20,5);
		else if ("OK".equals(command)) {
			try {
				manager.save();
			} catch (Exception e1) {
				MEMEApp.logExceptionCallStack("UserToolsDialog [OK]",e1);
			}
			canceled = false;
			setVisible(false);
		} else if ("CANCEL".equals(command)) {
			canceled = true;
			manager.reload();
			setVisible(false);
		} else if ("VARIABLE_DIALOG".equals(command)) {
			int idx = groupBox.getSelectedIndex();
			UTEnvironmentVariableDialog dlg = new UTEnvironmentVariableDialog(this,manager,idx);
			dlg.showDialog();
		} else if ("VARIABLE".equals(command)) {
			getVariablesMenu().show(insertVariableButton,90,5);
		} else if (command.startsWith("%") && command.endsWith("%")) {
			int pos = activeField.getCaretPosition();
			String newText = pos >= activeField.getText().length() ? activeField.getText().substring(0,pos) + command : 
																	 activeField.getText().substring(0,pos) + command + activeField.getText().substring(pos);
			activeField.setText(newText);
			activeField.setCaretPosition(pos + command.length());
			activeField.grabFocus();
		} else {
			int pos = argumentsField.getCaretPosition();
			String newText = pos >= argumentsField.getText().length() ? argumentsField.getText().substring(0,pos) + command + " " : 
																		argumentsField.getText().substring(0,pos) + command + argumentsField.getText().substring(pos) + " ";
			argumentsField.setText(newText);
			argumentsField.setCaretPosition(pos + command.length() + 1);
			argumentsField.grabFocus();
		}
	}

	//-------------------------------------------------------------------------------------
	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			int index = toolsList.getSelectedIndex();
			if (index != -1) {
				removeButton.setEnabled(true);
				moveUpButton.setEnabled(toolsModel.size() > 1);
				moveDownButton.setEnabled(toolsModel.size() > 1);
				UserToolItem item = (UserToolItem)toolsModel.get(index);
				menuTextField.setText(item.getMenuText()); menuTextField.setEnabled(true);
				commandField.setText(item.getCommand()); commandField.setEnabled(true);
				browseButton.setEnabled(true);
				argumentsLabel.setEnabled(item.isProgram());
				argumentsField.setText(item.isDocument() ? "" : item.getArguments());
				argumentsField.setEnabled(item.isProgram());
				insertButton.setEnabled(item.isProgram());
			} else {
				clearSelection();
			}
		}
	}

	//-------------------------------------------------------------------------------------
	public void focusGained(FocusEvent e) {
		Object source = e.getSource();
		insertVariableButton.setEnabled(source.equals(commandField) || source.equals(argumentsField));
		if (source.equals(commandField))
			activeField = commandField;
		else if (source.equals(argumentsField))
			activeField = argumentsField;
		else 
			activeField = null;
	}

	//-------------------------------------------------------------------------------------
	public void focusLost(FocusEvent e) {
		Object source = e.getSource();
		boolean isList = toolsList.equals(e.getOppositeComponent());
		if (source.equals(menuTextField))
			storeMenuText(isList);
		else if (source.equals(commandField))
			storeCommand(isList);
		else if (source.equals(argumentsField))
			storeArgs(isList);
	}
	
	//=====================================================================================
	// GUI methods
	
	//-------------------------------------------------------------------------------------
	private void layoutGUI() {
		bottomButtonLine = FormsUtils.build("p:g ~ p",
											"00||" +
											"_1",
											new Separator(""),
											insertVariableButton).getPanel();
		
		selectedToolPanel = FormsUtils.build("p ~ f:p:g ~ 25dlu ~ p",
											 "0111||" +
											 "2344||" +
											 "5667||" +
											 "8888|",
											 "Menu text:",menuTextField,
											 "Command:",commandField,browseButton,
											 argumentsLabel,argumentsField,insertButton,
											 bottomButtonLine).getPanel();
		
		buttonsPanel = FormsUtils.build("p:g p ~ p p:g",
										"_01_",
										okButton,cancelButton).getPanel();
		
		content = FormsUtils.build("p ~ p:g ~ p",
								   "[DialogBorder]012||" +
								   				 "345||" +
								   				 "_46||" +
								   				 "_47||" +
								   				 "_48||" +
								   				 "_49" +
								   				 "_4_ p:g||" +
								   				 "AAA p||" +
								   				 "BBB||" +
								   				 "CCC",
								   				 "Group:",groupBox,changeButton,
								   				 "Tool items:",toolsListScr,addButton,
								   				 removeButton,
								   				 moveUpButton,
								   				 moveDownButton,
								   				 variableDialogButton,
								   				 selectedToolPanel,
								   				 new JSeparator(),
								   				 buttonsPanel).getPanel();
	}
	
	//-------------------------------------------------------------------------------------
	private void initialize(int groupIndex) {
		addButton.setHorizontalTextPosition(JButton.LEFT);
		insertVariableButton.setHorizontalTextPosition(JButton.LEFT);
		selectedToolPanel.setBorder(BorderFactory.createTitledBorder("Selected tool"));
		
		groupBox.setActionCommand("GROUP");
		changeButton.setActionCommand("CHANGE");
		addButton.setActionCommand("ADD");
		removeButton.setActionCommand("REMOVE");
		moveUpButton.setActionCommand("UP");
		moveDownButton.setActionCommand("DOWN");
		variableDialogButton.setActionCommand("VARIABLE_DIALOG");
		menuTextField.setActionCommand("MENU_TEXT");
		commandField.setActionCommand("COMMAND");
		browseButton.setActionCommand("BROWSE");
		argumentsField.setActionCommand("ARGS");
		insertButton.setActionCommand("INSERT");
		okButton.setActionCommand("OK");
		cancelButton.setActionCommand("CANCEL");
		insertVariableButton.setActionCommand("VARIABLE");
		insertVariableButton.setEnabled(false);
		
		menuTextField.addFocusListener(this);
		commandField.addFocusListener(this);
		argumentsField.addFocusListener(this);

		groupBox.addFocusListener(this);
		changeButton.addFocusListener(this);
		toolsList.addFocusListener(this);
		addButton.addFocusListener(this);
		removeButton.addFocusListener(this);
		moveUpButton.addFocusListener(this);
		moveDownButton.addFocusListener(this);
		variableDialogButton.addFocusListener(this);
		browseButton.addFocusListener(this);
		insertButton.addFocusListener(this);
		
		groupModel = new DefaultComboBoxModel(manager.getGroups().toArray());
		groupBox.setModel(groupModel);
	
		GUIUtils.addActionListener(this,groupBox,changeButton,addButton,removeButton,moveUpButton,
								   moveDownButton,browseButton,insertButton,okButton,cancelButton,
								   menuTextField,commandField,argumentsField,variableDialogButton,
								   insertVariableButton);
		toolsList.addListSelectionListener(this);
		toolsList.setSelectionModel(new UTListSelectionModel());
		
		int index = (groupIndex < 1 || groupIndex > manager.getGroups().size()) ? 0 : groupIndex - 1;
		
		groupBox.setSelectedItem(manager.getGroup(index));
		List<UserToolItem> tools = manager.getGroup(index).getTools();
		if (tools.size() > 0)
			toolsList.setSelectedValue(tools.get(0),true);
		else
			clearSelection();
		this.setTitle("Configure User Tools");
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				canceled = true;
				manager.reload();
				setVisible(false);
			}
		});
		
		commandField.setPreferredSize(new Dimension(300,24));
		this.setPreferredSize(new Dimension(500,380));
		final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		this.setContentPane(sp);
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
	
	//-------------------------------------------------------------------------------------
	private JPopupMenu getAddMenu() {
		if (addMenu == null) {
			addMenu = new JPopupMenu();
			addProgram.setActionCommand("ADD_PROGRAM");
			addMenu.add(addProgram);
			addDocument.setActionCommand("ADD_DOCUMENT");
			addMenu.add(addDocument);
			GUIUtils.addActionListener(this,addProgram,addDocument);
		}
		return addMenu;
	}
	
	//-------------------------------------------------------------------------------------
	private JPopupMenu getArgumentsMenu() {
		if (argumentsMenu == null) {
			argumentsMenu = new JPopupMenu();
			List<Argument> argList = UserToolParser.getPossibleArguments();
			for (Argument arg : argList) {
				if (arg == null)
					argumentsMenu.addSeparator();
				else {
					JMenuItem item = new JMenuItem(arg.toString());
					item.setToolTipText(arg.getTooltip());
					item.setActionCommand(arg.getArgument());
					item.addActionListener(this);
					argumentsMenu.add(item);
				}
			}
		}
		return argumentsMenu;
	}
	
	//----------------------------------------------------------------------------------------------------
	private JPopupMenu getVariablesMenu() {
		JPopupMenu back = new JPopupMenu();
		UserToolGroup group = (UserToolGroup) groupBox.getSelectedItem();
		List<EnvironmentVariable> environmentVariables = group.getEnvironmentVariables();
		for (EnvironmentVariable env : environmentVariables) {
			JMenuItem item = new JMenuItem(env.getKey());
			item.setToolTipText(env.getValue());
			item.setActionCommand("%" + env.getKey() + "%");
			item.addActionListener(this);
			back.add(item);
		}
		List<EnvironmentVariable> globalVariables = UserToolGroup.getGlobalVariables();
		if (environmentVariables.size() > 0 && globalVariables.size() > 0)
			back.addSeparator();
		for (EnvironmentVariable env : globalVariables) {
			JMenuItem item = new JMenuItem(env.getKey());
			item.setToolTipText(env.getValue());
			item.setActionCommand("%" + env.getKey() + "%");
			item.addActionListener(this);
			back.add(item);
		}
		if (environmentVariables.size() == 0 && globalVariables.size() == 0) {
			JMenuItem item =  new JMenuItem("(Empty)");
			back.add(item);
			item.setEnabled(false);
		}
		return back;
	}
	
	//-------------------------------------------------------------------------------------
	private void clearSelection() {
		menuTextField.setText(""); menuTextField.setEnabled(false);
		commandField.setText(""); commandField.setEnabled(false);
		browseButton.setEnabled(false);
		argumentsLabel.setEnabled(false);
		argumentsField.setText(""); argumentsField.setEnabled(false);
		insertButton.setEnabled(false);
		removeButton.setEnabled(false);
		moveUpButton.setEnabled(false);
		moveDownButton.setEnabled(false);
		toolsList.clearSelection();		
	}
	
	//-------------------------------------------------------------------------------------
	private void addTool(UserToolGroup grp, UserToolItem item) {
		try {
			grp.addTool(item);
			groupBox.setSelectedItem(grp);
			toolsList.setSelectedValue(item,true);
			menuTextField.grabFocus();
			menuTextField.selectAll();
		} catch (GroupIsFullException e1) {
			MEMEApp.userErrors("Warning","This group is full, try an other one.");
		}		
	}
	
	//-------------------------------------------------------------------------------------
	private void storeMenuText(boolean isList) {
		String newName = menuTextField.getText().trim();
		UserToolItem tool = !isList ? (UserToolItem) toolsList.getSelectedValue() :
									  (UserToolItem) toolsModel.get(((UTListSelectionModel)toolsList.getSelectionModel()).getOldSelected());
		if (newName.equals(tool.getMenuText())) 
			return;
		UserToolGroup grp = (UserToolGroup) groupModel.getSelectedItem();
		
		if ("".equals(newName)) {
			menuTextField.setText(tool.getMenuText());
			menuTextField.grabFocus();
			menuTextField.selectAll();
		} else if (grp.isUsedToolName(tool.getType(),newName)) {
			MEMEApp.userErrors("Warning","There is a tool in this group with the same menu text.");
			if (!isList) {
				menuTextField.setText(tool.getMenuText());
				menuTextField.grabFocus();
				menuTextField.selectAll();
			} else {
				UserToolItem item = (UserToolItem) toolsList.getSelectedValue();
				menuTextField.setText(item.getMenuText()); 
				commandField.setText(item.getCommand()); 
				argumentsLabel.setEnabled(item.isProgram());
				argumentsField.setText(item.isDocument() ? "" : item.getArguments());
				argumentsField.setEnabled(item.isProgram());
				insertButton.setEnabled(item.isProgram());
			}
		} else if (newName.length() > 64) {
			MEMEApp.userErrors("Warning","The length of the menu text must be lesser than 64 characters.");
			if (!isList) {
				menuTextField.setText(tool.getMenuText());
				menuTextField.grabFocus();
				menuTextField.selectAll();
			} else {
				UserToolItem item = (UserToolItem) toolsList.getSelectedValue();
				menuTextField.setText(item.getMenuText()); 
				commandField.setText(item.getCommand()); 
				argumentsLabel.setEnabled(item.isProgram());
				argumentsField.setText(item.isDocument() ? "" : item.getArguments());
				argumentsField.setEnabled(item.isProgram());
				insertButton.setEnabled(item.isProgram());
			}
		} else {
			tool.setMenuText(newName);
			Object temp = toolsList.getSelectedValue();
			toolsList.setSelectedValue(tool,true);
			if (isList)
				toolsList.setSelectedValue(temp,true);
		}
	}
	
	//-------------------------------------------------------------------------------------
	private void storeCommand(boolean isList) {
		String cmd = commandField.getText().trim();
		UserToolItem tool = !isList ? (UserToolItem) toolsList.getSelectedValue() :
			  						  (UserToolItem) toolsModel.get(((UTListSelectionModel)toolsList.getSelectionModel()).getOldSelected());
		if (cmd.equals(tool.getCommand())) 
			return;
		tool.setCommand(cmd);
		Object temp = toolsList.getSelectedValue();
		toolsList.setSelectedValue(tool,true);
		if (isList)
			toolsList.setSelectedValue(temp,true);
	}
	
	//-------------------------------------------------------------------------------------
	private void storeArgs(boolean isList) {
		String args = argumentsField.getText().trim();
		UserToolItem tool = !isList ? (UserToolItem) toolsList.getSelectedValue() :
			  						  (UserToolItem) toolsModel.get(((UTListSelectionModel)toolsList.getSelectionModel()).getOldSelected());
		if (args.equals(tool.getArguments())) 
			return;
		tool.setArguments(args);
		Object temp = toolsList.getSelectedValue();
		toolsList.setSelectedValue(tool,true);
		if (isList)
			toolsList.setSelectedValue(temp,true);
	}
}
