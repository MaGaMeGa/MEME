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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.table.AbstractTableModel;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.internal.platform.InfoConverter;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.internal.platform.IGUIController.RunOption;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;

/** The graphical component where the user can define new parameters for his/her
 *  model.
 */
public class NewParametersDialog extends JDialog implements ActionListener {


	//===============================================================================
	// members

	private static final long serialVersionUID = 1L;
	
	/** Return value constant: indicates that the user closes the dialog by pressing Cancel button (or 'x' in the right top corner). */
	public static final int CANCEL_OPTION = -1;
	/** Return value constant: indicates that the user closes the dialog by pressing OK button. */
	public static final int OK_OPTION = 0;
	
	/** The return value of the dialog. */
	private int returnValue = CANCEL_OPTION; 
	
	//===============================================================================
	// GUI members
	
	private JPanel content = new JPanel(new BorderLayout());
	private JTextPane infoPane = new JTextPane();
	private JScrollPane infoScr = new JScrollPane(infoPane);
	@SuppressWarnings("serial")
	private JTable table = new JTable() {{ tableHeader.setReorderingAllowed(false); }};
	private JScrollPane tableScr = new JScrollPane(table);
	private JPanel centerPanel = new JPanel(new BorderLayout());
	private JButton okButton = new JButton("OK");
	private JButton cancelButton = new JButton("Cancel");
	private JPanel buttonsPanel = new JPanel();
	
	//===============================================================================
	// methods
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the parent component of the dialog
	 * @param modelClass the class object of the model
	 * @param initParams the names of the original parameters of the model
	 */
	public NewParametersDialog(Frame owner, List<RecordableInfo> candidates, String[] initParams) {
		super(owner,"Add new parameters",true);
		layoutGUI();
		initialize();
		table.setModel(new ParametersTableModel(candidates,initParams));
		GUIUtils.updateColumnWidths(table,tableScr);
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				GUIUtils.updateColumnWidths(table,tableScr);
			}
		});
		this.setLocationRelativeTo(owner);
	}
	
	//-------------------------------------------------------------------------------
	/** Shows the dialog.
	 * @return an int that indicates the closing mode of the dialog
	 */
	public int showDialog() {
		setVisible(true);
		int result = returnValue;
		dispose();
		return result;
	}
	
	//-------------------------------------------------------------------------------
	/** Returns the list of the information objects of the new parameters. */
	public List<ParameterInfo> getNewParameterList() {
		return ((ParametersTableModel)table.getModel()).getNewParameters();
	}
	
	//===============================================================================
	// implemented interfaces
	
	//-------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("OK")) {
			returnValue = OK_OPTION;
			setVisible(false);
		} else if (command.equals("CANCEL")) {
			returnValue = CANCEL_OPTION;
			setVisible(false);
		}
	}
	
	//===============================================================================
	// GUI methods
	
	//-------------------------------------------------------------------------------
	private void layoutGUI() {
		centerPanel.add(tableScr,BorderLayout.CENTER);
		
		buttonsPanel.add(okButton);
		buttonsPanel.add(cancelButton);
		
		Box tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(infoScr);
		tmp.add(new JSeparator());
		content.add(tmp,BorderLayout.NORTH);
		content.add(centerPanel,BorderLayout.CENTER);
		tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(new JSeparator());
		tmp.add(buttonsPanel);
		content.add(tmp,BorderLayout.SOUTH);
		
		this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				returnValue = CANCEL_OPTION;
			}
		});
	}
	
	//-------------------------------------------------------------------------------
	private void initialize() {
		infoScr.setBorder(null);
		
		infoPane.setEditable(false);
		int b = GUIUtils.GUI_unit(0.5);
		infoPane.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
		Utilities.setTextPane(infoPane,Utils.htmlPage("Please mark the new parameters in the first column of the table and set its " +
							  						  "initial values in the fourth column."));
		
		centerPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
		okButton.setActionCommand("OK");
		cancelButton.setActionCommand("CANCEL");

		GUIUtils.addActionListener(this,okButton,cancelButton);
		
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
	
	//===============================================================================
	// nested classes
	
	/** The table model of the table that contains the possible parameters. */
	@SuppressWarnings("serial")
	private static class ParametersTableModel extends AbstractTableModel {
		
		//===========================================================================
		// members
		
		/** Names of the columns of the table. */
		private static String[] columnNames = { "Selected", "Name", "Type", "Initial value" };
		/** Types of the columns of the table. */
		private static Class<?>[] columnTypes = { Boolean.class, String.class, String.class, String.class };
		
		/** The list of information objects of the possible parameters. */
		private List<ParameterInfo> infoList = null;
		/** Flag list. A true flag indicates that the possible parameter in the <code>
		 *  infoList</code> at the same position is marked as a new parameter.
		 */
		private List<Boolean> markList = null;
		
		//===========================================================================
		// methods
		
		//---------------------------------------------------------------------------
		/** Constructor.
		 * @param modelClass the class object of the model
		 * @param initParams the names of the original parameters of the model
		 */
		public ParametersTableModel(List<RecordableInfo> candidates, String[] initParams) {
			infoList = new ArrayList<ParameterInfo>();
			markList = new ArrayList<Boolean>();
			
			List<String> reserved = new ArrayList<String>(Arrays.asList(initParams));
			if (ParameterSweepWizard.getPreferences().rngSeedAsParameter() && PlatformSettings.getPlatformType() == PlatformType.REPAST)
				reserved.add("RngSeed");
			
			for (RecordableInfo candidate : candidates) {
				if (InfoConverter.isVariable(candidate)) { // means this is a variable
					if (reserved.contains(Util.capitalize(candidate.getName())) || reserved.contains(Util.uncapitalize(candidate.getName())))
						continue;
					if (Util.isAcceptableSimpleType(candidate.getType())) {
						Class<?> convertedType = convert(candidate.getType());
						ParameterInfo pi = new ParameterInfo(candidate.getName(),Utilities.toTypeString(convertedType),convertedType);
						pi.setInitValue();
						if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL)
							pi.setRuns(1);
						pi.setDefinitionType(ParameterInfo.CONST_DEF);
						infoList.add(pi);
					}
				}
			}
			for (int i = 0;i < infoList.size();++i)
				markList.add(new Boolean(false));
		}

		//---------------------------------------------------------------------------
		public int getColumnCount() { return columnNames.length; }
		public int getRowCount() { return infoList.size(); }
		@Override public Class<?> getColumnClass(int columnIndex) { return columnTypes[columnIndex]; }
		@Override public String getColumnName(int column) { return columnNames[column]; }
		@Override public boolean isCellEditable(int rowIndex, int columnIndex) { return columnIndex == 0 || columnIndex == 3; }

		//---------------------------------------------------------------------------
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case 0  : return markList.get(rowIndex);
			case 1  : return infoList.get(rowIndex).getName();
			case 2  : return infoList.get(rowIndex).getType();
			case 3  : return infoList.get(rowIndex).getValue() == null ? "" : infoList.get(rowIndex).getValue().toString();
			default : throw new IllegalStateException();
			}
		}

		//---------------------------------------------------------------------------
		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 0) 
				markList.set(rowIndex,(Boolean)aValue);
			else {
				ParameterInfo info = infoList.get(rowIndex);
				boolean valid = ParameterInfo.isValid(aValue.toString(),info.getType());
				if (valid)
					info.setValue(ParameterInfo.getValue(aValue.toString(),info.getType()));
			}
			
		}
		
		//---------------------------------------------------------------------------
		/** Returns a list that contains the marked elements of the <code>infoList</code>. */
		public List<ParameterInfo> getNewParameters() {
			List<ParameterInfo> res = new ArrayList<ParameterInfo>();
			for (int i = 0;i < infoList.size();++i) {
				if (markList.get(i).booleanValue())
					res.add(infoList.get(i));
			}
			return res;
		}
		
		//------------------------------------------------------------------------------
		private Class<?> convert(Class<?> type) {  
			if (type.equals(Boolean.TYPE))
					return Boolean.class;
			if (type.equals(Byte.TYPE))
				return Byte.class;
			if (type.equals(Short.TYPE))
				return Short.class;
			if (type.equals(Integer.TYPE))
				return Integer.class;
			if (type.equals(Long.TYPE))
				return Long.class;
			if (type.equals(Float.TYPE))
				return Float.class;
			if (type.equals(Double.TYPE))
				return Double.class;
			return type;
		}
	}
}
