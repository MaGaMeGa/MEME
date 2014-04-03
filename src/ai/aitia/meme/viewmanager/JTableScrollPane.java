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
package ai.aitia.meme.viewmanager;

import java.awt.Font;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.gui.MainWindow;
import ai.aitia.meme.pluginmanager.IVCPlugin;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.viewmanager.JTableScrollPane.ColInfo.Name;

//-----------------------------------------------------------------------------
public class JTableScrollPane extends JScrollPane implements javax.swing.table.TableCellRenderer,
																javax.swing.event.TableModelListener
{
	//=========================================================================
	//	Nested types

	//-------------------------------------------------------------------------
	/** This interface defines only one method that returns the type of a column
	 *  from the columns information of an XML documents.
	 */
	public interface IDataTypeCalculator {
		public ColumnType getViewColDataType(ViewCreationRule.Column col, int idx);
	}

	//-------------------------------------------------------------------------
	/** Enum type for representing the place where the scrollpane is used. */
	protected static enum Mode { 
		PAGE_COLUMNS {
			// Column titles
			Object HEADERS[] = { "Column name", "Source", "Splitter", "Hidden" };
			Class ColClass[] = { Object.class, Object.class, Boolean.class, Boolean.class };
			@Override Object[] getHeaders()			{ return HEADERS; } 
			@Override Class[]  getColClass()			{ return ColClass; }
			@Override boolean isSplitter(int idx)	{ return idx == 2; }
			@Override boolean isHidden(int idx)		{ return idx == 3; }
		},
		VIEWS_BROWSER {
			// Column titles
			Object HEADERS[] = { "Column name", "Source", "Generated" };
			Class ColClass[] = { Object.class, Object.class, Boolean.class };
			@Override Object[] getHeaders()			{ return HEADERS; }
			@Override Class[]  getColClass()			{ return ColClass; }
			@Override boolean isSplitter(int idx)	{ return false; }
			@Override boolean isHidden(int idx)		{ return false; }
		};

		/** Returns the header names. */
		abstract Object[] getHeaders();
		/** Returns the types of the table columns. */
		abstract Class[]  getColClass();
		/** Tests if the i-th column is a splitter column. */
		abstract boolean isSplitter(int idx);
		/** Tests if the i-th column is a hidden column. */
		abstract boolean isHidden(int idx);
	};

	//-------------------------------------------------------------------------
	/**	Rows in DefaultTableModel must contain 1 Object for every column 
	 *  of the table. Even those columns which display compound information 
	 *  must appear as single Objects, to obey the number of columns. 
	 *  Namely, "Name" column shows data type + column name; 
	 *  the "Source" column shows source type + details.
	 */
	public static class ColInfo implements Cloneable
	{
		
		class Name				{ ViewCreationRule.Column getCol()		{ return col; }
								  ColInfo				  getColInfo()	{ return ColInfo.this; }
								  @Override public String toString() { return col.getName(); }
								}
		class Source			{ ViewCreationRule.Column getCol()		{ return col; } }

		public ViewCreationRule.Column col;

		public	ColInfo(ViewCreationRule rule)		{ this(new ViewCreationRule.Column(rule)); }
		public	ColInfo(ViewCreationRule.Column c)	{ col = c; }
		//private ColInfo(Object[] r)				{ this(getViewCol(r)); }

		@Override public ColInfo clone() {
			try { 
				ColInfo ans = (ColInfo)super.clone();
				ans.col = col.clone();
				return ans;
			} catch (CloneNotSupportedException e) { return null; }
		}
		Object[] getRow() {
			Object ans[] = { new Name(), new Source(), col.isSplitter(), col.isHidden() };
			return ans;
		}
		private static ViewCreationRule.Column getViewCol(Object[] row) {
			return ((Name)row[0]).getCol();
		}
		private static ColInfo getColInfo(Object[] row) {
			return ((Name)row[0]).getColInfo();
		}
	}

	//-------------------------------------------------------------------------
	private static class ColInfoForViewsBrowser extends ColInfo {
		private ColInfoForViewsBrowser(ViewCreationRule.Column c) { super(c); }
		@Override Object[] getRow() {
			return new Object[] { new Name(), new Source(), col.isSplitted() };
		}
	}

	//=========================================================================
	//	Member variables

	private static final long serialVersionUID = 1L;
	private JTable jTable = null;
	private DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
	private Font normalFont = renderer.getFont();
	private Font otherFont = normalFont.isBold() ? normalFont.deriveFont(Font.ITALIC)
												  : normalFont.deriveFont(Font.BOLD);

	//=========================================================================
	//	Public interface

	//-------------------------------------------------------------------------
	public JTableScrollPane() {
		setViewportView(getJTable());
	}

	//-------------------------------------------------------------------------
	/** Clears the table and then fills it from 'rule' */
	public void fill(ViewCreationRule rule, IDataTypeCalculator dtc) {
		// Fill in the table from owner.getRule()
		clear();
		if (rule != null) {
			int idx = getData().getRowCount();
			boolean vbr = (getMode() == Mode.VIEWS_BROWSER);
			for (ViewCreationRule.Column c : rule.getColumns()) {
				ColInfo ci = vbr ? new ColInfoForViewsBrowser(c) : new ColInfo(c); 
				addRow(ci);
				if (c.getInitialDataType() == null)
					c.setInitialDataType(dtc.getViewColDataType(c, idx));
				idx += 1;
			}
		}
	}

	//-------------------------------------------------------------------------
	/** Updates the data type of every existing column */
	public boolean updateDataTypes(IDataTypeCalculator dtc) {
		boolean ans = true;
		for (int i = getData().getRowCount() - 1; i >= 0; --i) {
			ViewCreationRule.Column c = getViewCol(i);
			if (c.setInitialDataType(dtc.getViewColDataType(c, i)) == null)
				ans = false;
		}
		return ans;
	}

	//-------------------------------------------------------------------------
	/** It updates the content of cells in the DefaultTableModel whose
	 *  values are copies of the ViewCreationRule.Column (such as hidden).
	 *  This may generate tableChanged() event many times.  
	 */ 
	public void updateRow(int idx) {
		DefaultTableModel tm = getData();
		Object[] olddata = ((java.util.Vector)tm.getDataVector().get(idx)).toArray();
		Object[] newdata = ColInfo.getColInfo(olddata).getRow();
		for (int i = 0; i < olddata.length; ++i) {
			if (!Utils.equals(olddata[i], newdata[i]))
				tm.setValueAt(newdata[i], idx, i);
		}
	}

	//-------------------------------------------------------------------------
	/** Clears the table model. */
	public void clear() {
		DefaultTableModel tm = new ViewColumnsTableModel();
		tm.addTableModelListener(this);
		getJTable().setModel(tm);
		getData().setColumnIdentifiers(getMode().getHeaders());
	}

	//-------------------------------------------------------------------------
	public DefaultTableModel getData() {
		return ((DefaultTableModel)getJTable().getModel());
	}

	//-------------------------------------------------------------------------
	public ViewCreationRule.Column getViewCol(int idx) {
		Object[] data = ((java.util.Vector)getData().getDataVector().get(idx)).toArray();
		return ColInfo.getViewCol(data);
	}
//	public ColInfo getTableRow(int row) {
//	return new ColInfo( ((java.util.Vector)getData().getDataVector().get(row)).toArray() );
//}

	//-------------------------------------------------------------------------
	/** Inserts a row into the table model. */
	public void insertRow(int idx, ColInfo row) {
		assert(getMode() != Mode.VIEWS_BROWSER || (row instanceof ColInfoForViewsBrowser));
		Object[] data = row.getRow();
		DefaultTableModel tm = getData();
		if (idx >= getData().getRowCount()) tm.addRow(data);
		else tm.insertRow(idx, data);
	}
	/** Appends a row to the table model. */
	public void addRow(ColInfo row) { insertRow(Integer.MAX_VALUE, row); }
	
	//-------------------------------------------------------------------------
	@SuppressWarnings("serial")
	public JTable getJTable() {
		if (jTable == null) {
			jTable = new JTable();
			jTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			jTable.setDefaultRenderer(Object.class, this);
			jTable.setColumnSelectionAllowed(false);
			jTable.setRowHeight(Math.max(jTable.getRowHeight(), 18));	// because icons' height is 16 pixels
			clear();	// <- setModel()
		}
		return jTable;
	}

	//-------------------------------------------------------------------------
	/** Returns the icon belongs to the column type 't'. */
	public static javax.swing.ImageIcon getColumnTypeIcon(ColumnType t) {
		if (t != null) {
			if (t.javaSqlType == ColumnType.BOOLEAN.javaSqlType) {
				if (boolean_icon == null) boolean_icon = MainWindow.getIcon("datatype_boolean.png");
				return boolean_icon;
			}
			if (t.javaSqlType == ColumnType.INT.javaSqlType ||
			    t.javaSqlType == ColumnType.LONG.javaSqlType) {
				if (int_icon == null) int_icon = MainWindow.getIcon("datatype_int.png");
				return int_icon;
			}
			if (t.javaSqlType == ColumnType.DOUBLE.javaSqlType) {
				if (real_icon == null) real_icon = MainWindow.getIcon("datatype_real.png");
				return real_icon;
			}
			if (t.javaSqlType == ColumnType.STRING.javaSqlType) {
				if (string_icon == null) string_icon = MainWindow.getIcon("datatype_string.png");
				return string_icon;
			}
		}
		if (missing_icon == null) missing_icon = MainWindow.getIcon("datatype_missing.png");
		return missing_icon;
	}
	private static javax.swing.ImageIcon	boolean_icon = null,
											int_icon = null,
											real_icon = null,
											string_icon = null,
											missing_icon = null,
											plugin_icon = null;

	//=========================================================================
	//	Protected interface
	
	protected Mode getMode()									{ return Mode.VIEWS_BROWSER; }	
//	protected boolean isCellEditable(int row, int column)		{ return false; }	
	/** Tests if the specified cell is editable. */
	protected boolean isCellEditable(int row, int column)		{ if (getMode().equals(Mode.VIEWS_BROWSER) &&
																      column == 0) return true;
																  return false; }
	/** It is called whenever the splitter status of a column is changed. It does
	 *  nothing by default.
	 */
	protected void splitterChange(int row, boolean value)		{}
	/** It is called whenever the hidden status of a column is changed. It does
	 *  nothing by default.
	 */
	protected void hiddenChange(int row, boolean value)		{}

	//=========================================================================
	//	Internals

	//-------------------------------------------------------------------------
	/** This class represents the table model of the tables belongs to views. */
	@SuppressWarnings("serial")
	private class ViewColumnsTableModel extends DefaultTableModel {
		@Override public Class<?> getColumnClass(int columnIndex)		{ return getMode().getColClass()[columnIndex]; }
		@Override public boolean isCellEditable(int row, int column)	{ return JTableScrollPane.this.isCellEditable(row, column); }
		@Override
		public void setValueAt(final Object aValue, int row, int column) {
			if (getMode().equals(Mode.VIEWS_BROWSER)) {
				int res = MEMEApp.askUser(false,"Alert","The rename of a column may cause problems when you edit",
								          "this view (or an other view that created from this one).",
								          "Do you want to continue?");
				if (res == 1) {
					Name info = (Name)getValueAt(row,column);
					final String old_name = info.toString();
					final long view_id = MEMEApp.getMainWindow().getViewsPanel().getSelectedView().getViewID();
					MEMEApp.LONG_OPERATION.begin("Rename column...",new Runnable() {
						public void run() {
							try {
								boolean success = MEMEApp.getViewsDb().renameColumn(view_id,old_name,aValue.toString());
								if (!success) {
									MEMEApp.userAlert("Invalid or reserved name.");
								}
							} catch (IllegalArgumentException e) {
								MEMEApp.userAlert("Empty name is invalid.");
							}
						}
					});
				}
			} else super.setValueAt(aValue,row,column);
		}
	}

	//-------------------------------------------------------------------------
	public 	java.awt.Component getTableCellRendererComponent(JTable 	table, 
															 Object		value,
															 boolean	isSelected, 
															 boolean	hasFocus, 
															 int		row, 
															 int		column)  
	{
		renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		Font font = normalFont;
		if (value instanceof ColInfo.Name) {
			ViewCreationRule.Column col = ((ColInfo.Name)value).getCol();
			renderer.setText(col.getName());
			renderer.setIcon(getColumnTypeIcon(col.getInitialDataType()));
			font = col.isScalar() ? otherFont : normalFont;
			// TODO: splitted oszlopoknal ez hogy lehet scalar?
		}
		else if (value instanceof ColInfo.Source) {
			ViewCreationRule.Column col = ((ColInfo.Source)value).getCol();
			java.util.ArrayList<Utils.Pair<SrcType, String>> src = col.getSource();
			java.util.ArrayList<String> names = new java.util.ArrayList<String>(src.size());
			javax.swing.Icon icon = null;
			SrcType t = null;
			for (Utils.Pair<SrcType, String> p : src) {
				t = p.getFirst();
				names.add(p.getSecond());
			}
			if (t == null) 
				icon = getColumnTypeIcon(null);	// TODO: ide valami kerdojelecske jobb lenne.
			String text = Utils.join(names, ", ");
			int i = text.indexOf('\n');
			if (i >= 0) text = text.substring(0, i) + "...";

			String fn = col.getAggrFn();
			if (fn != null && fn.length() > 0) {
				IVCPlugin p = Page_Columns.getAvailablePlugins().findByName(fn);
				fn = (p == null) ? null : p.getLocalizedName();
				if (fn != null) {
					text = String.format("%s(%s)", fn.replaceAll("\\([^)]*\\)", ""), text);
					if (plugin_icon == null)
						plugin_icon = MainWindow.getIcon("type_javaplugin.png");
					icon = plugin_icon;
				}
			}
			renderer.setText(text);
			if (icon == null) {
				//icon = t.isProjection() ? null : t.getIcon();
				icon = t.getIcon();
			}
			renderer.setIcon(icon);
		} else {
			renderer.setIcon(null);
		}
		renderer.setFont(font);
		return renderer;
	}

	//-------------------------------------------------------------------------
	public void tableChanged(javax.swing.event.TableModelEvent e) {
		if (e.getType() == javax.swing.event.TableModelEvent.UPDATE
			&& e.getFirstRow() == e.getLastRow()) {
			int row = e.getFirstRow();
			Mode m = getMode();
			if (m.isSplitter(e.getColumn())) {
				splitterChange(row, (Boolean)getJTable().getValueAt(row, e.getColumn()));
			} 
			else if (m.isHidden(e.getColumn())) {
				hiddenChange(row, (Boolean)getJTable().getValueAt(row, e.getColumn()));
			}
		}
	}
}
