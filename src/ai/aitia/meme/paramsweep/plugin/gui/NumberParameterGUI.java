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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import ai.aitia.meme.paramsweep.gui.ScriptCreationDialog;
import ai.aitia.meme.paramsweep.gui.info.ConstantInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;

/** This class represents graphical components for formal parameters if the 
 *  formal parameter is numeric.
 */
public class NumberParameterGUI extends JPanel implements IParameterGUI,
														  ActionListener {

	//=====================================================================================
	// members
	private static final long serialVersionUID = 1L;
	
	/** The list of information objects of appropriate members of the
	 * 	model that can be used in the statistic instance.
	 */
	private List<MemberInfo> availableMembers = null;
	/** Name of the formal parameter that belongs to this graphical component. */
	private String name = null;
	/** The model of the combobox. */
	private DefaultComboBoxModel model = null;
	/** Regular expression for double numbers. */
	private	Pattern pattern = null; 
	/** The dialog that contains this component. */
	private ScriptCreationDialog parent = null;
	
	//=====================================================================================
	// GUI members
	
	private JLabel nameLabel = new JLabel();
	private JComboBox combo = new JComboBox();
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Constructor.
	 * @param parent the dialog that contains this component
	 */
	public NumberParameterGUI(ScriptCreationDialog parent) {
		super();
		this.parent = parent;
		model = new DefaultComboBoxModel();
		layoutGUI();
	}
	
	//=====================================================================================
	// implemented interfaces
	
	//------------------------------------------------------------------------------------
	public String getParameterName() { return name; }

	//------------------------------------------------------------------------------------
	public String checkInput() {
		if (combo.getItemCount() == 0 || combo.getSelectedIndex() == -1) 
			return "missing parameter.";
		return null;
	}

	//------------------------------------------------------------------------------------
	public JPanel getGUIComponent(String name, String tooltip,
			List<MemberInfo> availables) {
		this.name = name;
		nameLabel.setText("  " + this.name + ": ");
		combo.setName("cbox_datasource_".concat(this.name.toLowerCase()));
		if (tooltip != null && !"".equals(tooltip.trim()))
			nameLabel.setToolTipText(tooltip);
		this.availableMembers = availables;
		initializeComboBox();
		return this;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void buildContent(List<MemberInfo> buildBlock) throws CannotLoadDataSourceForEditingException {
		// there is only one MemberInfo in the list (otherwise this is an invalid state)
		if (buildBlock.size() == 1) {
			MemberInfo memberInfo = buildBlock.get(0);
			if (memberInfo instanceof ConstantInfo) {
				ConstantInfo cInfo = (ConstantInfo) memberInfo;
				if (model.getIndexOf(cInfo) == -1)
					model.addElement(cInfo);
			}
			if (model.getIndexOf(memberInfo) == -1)
				throw new CannotLoadDataSourceForEditingException(memberInfo.getName() + " is missing!");
			combo.setSelectedItem(memberInfo);
			return;
		}
		throw new IllegalStateException("too many MemberInfo");
	}

	//------------------------------------------------------------------------------------
	public Object getInput() { return combo.getSelectedItem(); }

	//------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		Object obj = combo.getSelectedItem();
		if (combo.getSelectedIndex() != -1)
			combo.setToolTipText(obj.toString());
		if (obj instanceof String) {
			String text = (String)obj;
			boolean valid = isValidNumber(text);
			if (!parent.warning(!valid,"Invalid constant: " + text,ScriptCreationDialog.WARNING,true)) {
				double d = Double.parseDouble(text.trim());
				ConstantInfo info = new ConstantInfo(d);
				if (model.getIndexOf(info) == -1) 
					model.addElement(info);
				combo.setSelectedItem(info);
			} else if (combo.getItemCount() > 0)
				combo.setSelectedIndex(0);
			else
				combo.setSelectedIndex(-1);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void postCreation() {}
	
	//=====================================================================================
	// GUI methods
	
	//-------------------------------------------------------------------------------------
	private void layoutGUI() {
		this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
		add(nameLabel);
		add(combo);
		combo.setEditable(true);
		combo.setRenderer(new TooltipRenderer(combo));
		combo.setModel(model);
		combo.addActionListener(this);
		int width = Math.min(parent.getAvailableWidth(),combo.getSize().width);
		combo.setPreferredSize(new Dimension(width,26));
		this.setPreferredSize(new Dimension(500,26));
		this.setMaximumSize(new Dimension(2000,30));
		this.setVisible(true);
	}
	
	//=====================================================================================
	// private methods
	
	//-------------------------------------------------------------------------------------
	/** Initializes the combobox from <code>availableMembers</code>. */
	private void initializeComboBox() {
		if (availableMembers != null) {
			for (MemberInfo mi : availableMembers)
				model.addElement(mi);
		}
	}
	
	//------------------------------------------------------------------------------------
	/** Tests if <code>text</code> contains a valid number. */
	private boolean isValidNumber(String text) {
		if (text == null) return false;
		String str = text.trim();
		if ("".equals(str)) return false;
		if (pattern == null)
		  pattern = Pattern.compile("^[-]?[0-9]+[.]?[0-9]*$"); // pattern for double numbers
		Matcher m = pattern.matcher(str);
		return m.matches();
	}
	
	//====================================================================================
	// nested classes
	
	//------------------------------------------------------------------------------------
	/** This class renders the elements of the combobox. */
	public static class TooltipRenderer extends JLabel implements ListCellRenderer {

		private static final long serialVersionUID = 1L;
		
		/** Owner of the renderer. */
		private JComboBox owner = null;
		
		//================================================================================
		// methods
		
		//--------------------------------------------------------------------------------
		/** Constructor.
		 * @param owner owner of the renderer
		 */
		public TooltipRenderer(JComboBox owner) {
			super();
			if (owner == null) {
				throw new IllegalArgumentException("Argument 'owner' must be not null.");
			}
			this.owner = owner;
		}

		//-------------------------------------------------------------------------------
		/** Returns a component that contains an icon and a name.
		 * @param list the list that use this renderer (internal list of the combobox)
		 * @param value the combobox element
		 * @param index the row index of the combobox element
		 * @param isSelected whether the row is selected or not
		 * @param cellHasFocus whether the combobox has focus or not
		 * @return the rendered component 
		 */
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			if (isSelected) {
				setBackground(list.getSelectionBackground());
		        setForeground(list.getSelectionForeground());
		    } else {
		        setBackground(list.getBackground());
		        setForeground(list.getForeground());
		    }
			if (value == null) return this;
		    String name = value.toString();
		    setText(name);
		    setToolTipText(name);
		    setFont(list.getFont());
		    setEnabled(owner.isEnabled());
		    return this;
		}
	}
}
