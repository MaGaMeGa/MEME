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
package ai.aitia.meme.csvExport;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import ai.aitia.meme.UserPrefs;
import ai.aitia.meme.gui.SimpleFileFilter;
import ai.aitia.meme.pluginmanager.IExportPluginContext;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.FormsUtils.Separator;

public class CSVExportSettingsDialog extends JDialog implements ActionListener 
{
	private static final long serialVersionUID = -5547492085909056825L;

	//===============================================================================
	// nested classes
	static class Item<P> {
		private P item;
		private String title;
		public Item(P item, String title) { this.item = item; this.title = title; }
		public P getItem() { return item; }
		@Override public String toString() { return title; }
		@SuppressWarnings("unchecked")
		@Override public boolean equals(Object obj) {
			if (obj instanceof Item) {
				Item<P> other = (Item<P>)obj;
				return item.equals(other.item);
			}
			return false;
		}
	}
	
	//===============================================================================
	// constants
	
	public static final int OK_OPTION = 0;
	public static final int CANCEL_OPTION = 1;
	
	private final static Vector<Item<String>> decimalSigns = new Vector<Item<String>>();
	
	static {
		decimalSigns.add(new Item<String>(".","Dot (.)"));
		decimalSigns.add(new Item<String>(",","Comma (,)"));
	}
	
	//===============================================================================
	// variables
	
	//private IExportPluginContext ctx = null;	
	private int result;
	static File lastFile = null;
	
	//===============================================================================
	// GUI-components
	private JRadioButton dTabButton = new JRadioButton("Tab");
	private JRadioButton dSemiColonButton = new JRadioButton("Semi-colon");
	private JRadioButton dCommaButton = new JRadioButton("Comma");
	private JRadioButton dSpaceButton = new JRadioButton("Space");
	private JRadioButton dOtherButton = new JRadioButton("Other:");
	@SuppressWarnings("unused")
	private ButtonGroup group2 = GUIUtils.createButtonGroup(dTabButton,dSemiColonButton,dCommaButton,dSpaceButton,dOtherButton);
	private JTextField dOtherField = new JTextField(1);
	private JPanel other = FormsUtils.build("p d",
											"01",
											dOtherButton,dOtherField).getPanel();
	private JComboBox decimalSignBox = new JComboBox(decimalSigns);
	private JRadioButton nullButton = new JRadioButton("\"null\" string");
	private JRadioButton nothingButton = new JRadioButton("<nothing>");
	private JRadioButton spaceButton = new JRadioButton("Space");
	private JRadioButton zeroButton = new JRadioButton("0 value");
	private JRadioButton otherButton = new JRadioButton("Other:");
	@SuppressWarnings("unused")
	private ButtonGroup group1 = GUIUtils.createButtonGroup(nullButton,nothingButton,
															spaceButton,zeroButton,
															otherButton);
	private JTextField otherField = new JTextField(5);
	private JRadioButton include = new JRadioButton("Include header line");
	private JRadioButton exclude = new JRadioButton("Exclude header line");
	@SuppressWarnings("unused")
	private ButtonGroup group = GUIUtils.createButtonGroup(include,exclude);
	private JButton okButton = new JButton("OK");
	private JButton cancelButton = new JButton("Cancel");
	private JPanel content = FormsUtils.build("p:g p ~ p p:g",
			  								  "[DialogBorder]0000|~|" +
			  								  				"1111 pref||" +
			  								  				"2233 pref||" +
			  								  				"4444||" +
			  								  				"5555 pref||" +
			  								  				"6666 pref||" +
			  								  				"7777 pref||" +
			  								  				"8888 pref||" +
			  								  				"99AA pref||" +
			  								  				"BBBB||" +
			  								  				"CCCC pref||" +
			  								  				"DDDD pref||" +
			  								  				"EEEE||" +
			  								  				"_FG_ default",
			  								  				new Separator("Output settings"),
			  								  				FormsUtils.build("p ~ p ~ p ~ p",
			  								  								 "0123|" +
			  								  								 "_455",
			  								  								 "Delimiter:",dTabButton,dSemiColonButton,dCommaButton,
			  								  								 dSpaceButton,other).getPanel(),
			  								  				"De&cimal sign:",decimalSignBox,
			  								  				new Separator("Output of 'null' values"),
			  								  				nullButton,
			  								  				nothingButton,
			  								  				spaceButton,
			  								  				zeroButton,
			  								  				otherButton,otherField,
			  								  				new Separator("Header"),
			  								  				include,
			  								  				exclude,
			  								  				new Separator(""),
			  								  				okButton,cancelButton).getPanel();
	
	//================================================================================
	// methods
	
	public CSVExportSettingsDialog(IExportPluginContext ctx, String title) {
		super ((ctx != null ? ctx.getAppWindow() : null), "CSV Export Settings" + (title != null ? " - " + title : ""), true);
		//this.ctx = ctx;
		initialize();
	}
	
	//--------------------------------------------------------------------------------
	private void initialize() {
		// set combobox from userpref
		decimalSignBox.setPreferredSize(new Dimension(150,20));
		String oldDecimalSign = ai.aitia.meme.MEMEApp.userPrefs.get(UserPrefs.CSVE_DECIMALSIGN,".");
		int index = 0;
		for (int i=0;i<decimalSignBox.getItemCount();++i) {
			if (new Item<String>(oldDecimalSign,"").equals(decimalSignBox.getItemAt(i))) {
				index = i;
				break;
			}
		}
		decimalSignBox.setSelectedIndex(index);
		
		// set radiobuttons selection
		String oldDelimiter = ai.aitia.meme.MEMEApp.userPrefs.get(UserPrefs.CSVE_DELIMITER,",");
		dOtherField.setEnabled(false);
		if (oldDelimiter.equals("\t")) dTabButton.setSelected(true);
		else if (oldDelimiter.equals(";")) dSemiColonButton.setSelected(true);
		else if (oldDelimiter.equals(",")) dCommaButton.setSelected(true);
		else if (oldDelimiter.equals(" ")) dSpaceButton.setSelected(true);
		else {
			dOtherButton.setSelected(true);
			dOtherField.setEnabled(true);
			dOtherField.setText(oldDelimiter);
		}
		
		String nullOutput = ai.aitia.meme.MEMEApp.userPrefs.get(UserPrefs.CSVE_NULLSTRING,"null");
		otherField.setEnabled(false);
		if (nullOutput.equals("null")) nullButton.setSelected(true);
		else if (nullOutput.equals("<nothing>")) nothingButton.setSelected(true);
		else if (nullOutput.equals(" ")) spaceButton.setSelected(true);
		else if (nullOutput.equals("0")) zeroButton.setSelected(true);
		else {
			otherButton.setSelected(true);
			otherField.setEnabled(true);
			otherField.setText(nullOutput);
		}
	
		String actHeader = ai.aitia.meme.MEMEApp.userPrefs.get(UserPrefs.CSVE_INCLUDEHEADER,"yes");
		if (actHeader.equals("yes")) include.setSelected(true);
		else exclude.setSelected(true);
		
		// initialize buttons
		dTabButton.setActionCommand("DNO");
		dSemiColonButton.setActionCommand("DNO");
		dCommaButton.setActionCommand("DNO");
		dSpaceButton.setActionCommand("DNO");
		dOtherButton.setActionCommand("DO");
		nullButton.setActionCommand("NOTOTHER");
		nothingButton.setActionCommand("NOTOTHER");
		spaceButton.setActionCommand("NOTOTHER");
		zeroButton.setActionCommand("NOTOTHER");
		otherButton.setActionCommand("OTHER");
		okButton.setActionCommand("OK");
		cancelButton.setActionCommand("CANCEL");
		GUIUtils.addActionListener(this,dTabButton,dSemiColonButton,dCommaButton,dSpaceButton,dOtherButton,nullButton,nothingButton,spaceButton,
								   zeroButton,otherButton,okButton,cancelButton);
		
		
		final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(null);
		this.setContentPane(sp);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				result = CANCEL_OPTION;
				setVisible(false);
			}
		});
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
	
	//--------------------------------------------------------------------------------
	public int start() {
		setVisible(true);
		int res = result;
		dispose();
		return res;
	}

	//--------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("OK")) {
			String delimiter = "";
			if (dTabButton.isSelected()) delimiter = "\t";
			else if (dSemiColonButton.isSelected()) delimiter = ";";
			else if (dCommaButton.isSelected()) delimiter = ",";
			else if (dSpaceButton.isSelected()) delimiter = " ";
			else delimiter = dOtherField.getText();
			
			if (delimiter.equals(((Item<String>)decimalSignBox.getSelectedItem()).getItem())) {
				ai.aitia.meme.MEMEApp.userErrors("Invalid combination","The delimiter and the decimal sign cannot be the same.");
				return;
			} else if (delimiter.contains(((Item<String>)decimalSignBox.getSelectedItem()).getItem())) {
				ai.aitia.meme.MEMEApp.userErrors("Invalid combination","The delimiter cannot contain the decimal sign.");
				return;
			}

			// save the settings
			ai.aitia.meme.MEMEApp.userPrefs.put(UserPrefs.CSVE_DELIMITER,delimiter);
			ai.aitia.meme.MEMEApp.userPrefs.put(UserPrefs.CSVE_DECIMALSIGN,((Item<String>)decimalSignBox.getSelectedItem()).getItem());
			ai.aitia.meme.MEMEApp.userPrefs.put(UserPrefs.CSVE_INCLUDEHEADER,(include.isSelected() ? "yes" : "no"));
			
			String nullOutput = "";
			if (nullButton.isSelected()) nullOutput = "null";
			else if (nothingButton.isSelected()) nullOutput = "<nothing>";
			else if (spaceButton.isSelected()) nullOutput = " ";
			else if (zeroButton.isSelected()) nullOutput = "0";
			else nullOutput = otherField.getText();
			ai.aitia.meme.MEMEApp.userPrefs.put(UserPrefs.CSVE_NULLSTRING,nullOutput);
			
			result = OK_OPTION;
			setVisible(false);
		} else if (command.equals("CANCEL")) {
			result = CANCEL_OPTION;
			setVisible(false);
		} else if (command.equals("NOTOTHER")) {
			otherField.setText("");
			otherField.setEnabled(false);
		} else if (command.equals("OTHER")) {
			otherField.setEnabled(true);
			otherField.grabFocus();
		} else if (command.equals("DNO")) {
			dOtherField.setText("");
			dOtherField.setEnabled(false);
		} else if (command.equals("DO")) {
			dOtherField.setEnabled(true);
			dOtherField.grabFocus();
		}
	}
	
	//-------------------------------------------------------------------------------
	static File saveFileDialog(java.awt.Frame parent, String defaultName, boolean isFileSelection) {
		JFileChooser chooser = new JFileChooser();
		File dir = ai.aitia.meme.MEMEApp.getLastDir();
		if (isFileSelection) {
			File defaultFile = null;
			if (dir == null) defaultFile = new File(defaultName + ".csv");
			else defaultFile = new File(dir.getPath() + File.separator + defaultName + ".csv");
			chooser.setSelectedFile(defaultFile);
			chooser.addChoosableFileFilter(new SimpleFileFilter("CSV files (*.csv;*.txt;*.dat)"));
		} else {
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setSelectedFile(dir);
		}
		int returnVal = chooser.showSaveDialog(parent);
		if (returnVal != JFileChooser.APPROVE_OPTION) return null;
		lastFile = chooser.getSelectedFile();
		ai.aitia.meme.MEMEApp.setLastDir(lastFile);
		return lastFile;
	}
}
