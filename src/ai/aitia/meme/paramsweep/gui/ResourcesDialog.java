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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;

/** This class provides the graphical user interface where the users can define the
 *  resources of the model. */
public class ResourcesDialog extends JDialog implements ActionListener,
														ListSelectionListener {

	//=================================================================================
	// members

	private static final long serialVersionUID = 1L;
	
	/** The parent of the dialog. */
	private Frame owner = null;
	/** The base directory of the model (the starting point of the package structure of the model). */
	private String modelDirectory = null;
	/** The list model that contains the resources. */
	private DefaultListModel listModel = new DefaultListModel();
	
	//=================================================================================
	// GUI-members
	
	private JPanel content = new JPanel(new BorderLayout());
	private JTextPane infoPane = new JTextPane();
	private JScrollPane infoScr = new JScrollPane(infoPane);
	private JList resourceList = new JList(listModel);
	private JScrollPane resourceScr = new JScrollPane(resourceList);
	private JPanel centerPanel = null;
	private JButton addButton = new JButton("Add resources...");
	private JButton removeButton = new JButton("Remove resources");
	private JButton closeButton = new JButton("Close");
	private JPanel buttonPanel = new JPanel();

	
	//=================================================================================
	// methods
	
	//--------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the parent of the dialog
	 * @param modelDirectory the base directory of the model (the starting point of the package structure of the model)
	 */
	public ResourcesDialog(Frame owner, String modelDirectory) {
		super(owner,"Resources",true);
		if (modelDirectory == null || "".equals(modelDirectory.trim()))
			throw new IllegalArgumentException("'modelDirectory' is invalid");
		File dir = new File(modelDirectory);
		if (!dir.exists() || !dir.isDirectory())
			throw new IllegalArgumentException("'modelDirectory' is invalid");
		this.owner = owner;
		this.modelDirectory = modelDirectory;
		layoutGUI();
		initialize();
	}
	
	//--------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the parent of the dialog
	 * @param modelDirectory the base directory of the model (the starting point of the package structure of the model)
	 * @param initResources the list of the paths of the initial resource files
	 */
	public ResourcesDialog(Frame owner, String modelDirectory, String[] initResources) {
		this(owner,modelDirectory);
		initializeList(initResources);
	}
	
	//--------------------------------------------------------------------------------
	/** Shows the dialog. */
	public void showDialog() { setVisible(true); }
	public Object[] getResources() { return listModel.toArray(); }
	public String getModelDirectory() { return modelDirectory; }
	
	//=================================================================================
	// implemented interfaces
	
	//---------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("ADD")) {
			File baseDir = new File(modelDirectory);
			JFileChooser chooser = new JFileChooser(baseDir);
			chooser.setMultiSelectionEnabled(true);
			int result = chooser.showOpenDialog(owner);
			if (result == JFileChooser.APPROVE_OPTION) {
				File[] selected = chooser.getSelectedFiles();
				for (File f : selected) {
					String path = f.getPath();
					if (listModel.contains(path))
						continue;
					if (checkPath(path))
						listModel.addElement(path);
					else {
						Utilities.userAlert(owner,"The path of the resource file(s) must contain the directory of the model: ",
											baseDir.getPath() + "."," ","In the model you must use relative path to access the resource file(s).");
						break;
					}
				}
			}
		} else if (command.equals("REMOVE")) {
			Object[] resources = resourceList.getSelectedValues();
			for (Object o : resources)
					listModel.removeElement(o);
		} else if (command.equals("CLOSE")) {
			resourceList.clearSelection();
			setVisible(false);
		}
	}
	
	//--------------------------------------------------------------------------------
	public void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) 
			removeButton.setEnabled(resourceList.getSelectedIndex() != -1);
	}
	
	//=================================================================================
	// GUI-methods
	
	//---------------------------------------------------------------------------------
	private void layoutGUI() {
		
		centerPanel = FormsUtils.build("d:g ~ p",
									   "[DialogBorder]01||" +
									                 "02|" +
									                 "0_ f:p:g",
									                 resourceScr,addButton,
									                 removeButton).getPanel();
		
		buttonPanel.add(closeButton);
		
		Box tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(infoScr);
		tmp.add(new JSeparator());
		content.add(tmp,BorderLayout.NORTH);
		content.add(centerPanel,BorderLayout.CENTER);
		tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(new JSeparator());
		tmp.add(buttonPanel);
		content.add(tmp,BorderLayout.SOUTH);
		
		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) { resourceList.clearSelection(); }
		});
		this.setPreferredSize(new Dimension(500,400));
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
		this.setLocationRelativeTo(owner);
	}
	
	//---------------------------------------------------------------------------------
	private void initialize() {
		infoScr.setBorder(null);
		
		infoPane.setEditable(false);
		int b = GUIUtils.GUI_unit(0.5);
		infoPane.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
		Utilities.setTextPane(infoPane,Utils.htmlPage("Please define the list of the resources used by the model."));
		
		resourceList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);		
		resourceList.addListSelectionListener(this);
		
		addButton.setActionCommand("ADD");
		removeButton.setActionCommand("REMOVE");
		closeButton.setActionCommand("CLOSE");

		removeButton.setEnabled(false);
		
		GUIUtils.addActionListener(this,addButton,removeButton,closeButton);
	}
	
	//=================================================================================
	// private methods
	
	//--------------------------------------------------------------------------------
	/** Initializes the resource list. */
	private void initializeList(String[] initList) {
		for (String element : initList) 
			listModel.addElement(element);
	}
	
	//--------------------------------------------------------------------------------
	/** Checks a resource file path whether the resource file is under the base directory
	 *  of the model or not.
	 * @param path the resource file path
	 */
	private boolean checkPath(String path) {
		File f = new File(path);
		if (f.getParent() == null) 
			return false;
		if (f.getParent().equals(modelDirectory))
			return true;
		return checkPath(f.getParent());
	}
}
