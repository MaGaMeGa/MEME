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

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import ai.aitia.meme.gui.Wizard;
import ai.aitia.meme.gui.Wizard.Button;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;

/** Wizard page "Name and description" in the View Creation Wizard. */
@SuppressWarnings("serial")
public class Page_Sorting extends JPanel implements Wizard.IWizardPage, 
														//Wizard.ICloseListener, 
														Wizard.IArrowsInHeader, 
														java.awt.event.ActionListener,
														java.awt.event.MouseListener,
														javax.swing.event.ListSelectionListener
{
	/*
	 * Mindegyik esetre igaz az, hogy a torolt oszlopokat kiveszi a rendezesbol,
	 * tagolas eseten pedig automatikusan hozzaveszi a nem-rejtett nem-aggregacios
	 * oszlopokat (ezek sorrendje: input, output, thisview; az elso 2 kategorian
	 * belul abc sorrend).
	 */
	/** Enum type for representing automatic sorting modes. */
	public static enum UpdateMode {
		/*
		 * Akkor kerulunk ebbe az uzemmodba, ha a user belemodosit a rendezesbe. 
		 * Ebben az uzemmodban ha a torolt oszlopok kivetele miatt valik uresse 
		 * a tabla, akkor az uzemmod automatikusan atvalt {@link #FILL_FROM_COLUMNS}-ra.   
		 */
		FILL_FROM_USER_SELECTION,
		FILL_FROM_RULE,

		/* Uresen tartja a tablat, csak tagolas eseten veszi be amit muszaj (ld. fent). */
		FILL_FROM_COLUMNS 
	};

	private ViewCreationDialog	owner 			= null;
	private UpdateMode			updateMode		= UpdateMode.FILL_FROM_COLUMNS;
	private ArrayList<String>	userSelection	= new ArrayList<String>();
	private ArrayList<String>	mustBeUsed		= new ArrayList<String>();

	private JTable		jTable 				= new JTable();
	private JButton		jMoveUpButton		= new JButton("Move up");
	private JButton		jMoveDownButton		= new JButton("Move down");
	private JButton		jRemoveButton		= new JButton("Remove");
	private JComboBox	jCombobox			= new JComboBox();
	private JPopupMenu	jTableContextMenu 	= new JPopupMenu();
	
	private AbstractAction moveUpAction		= null;
	private AbstractAction moveDownAction 	= null;
	private AbstractAction removeAction		= null;

	//=========================================================================
	//	Interface methods

	//-------------------------------------------------------------------------
	public String getTitle() {
		return "Sorting";
	}
	
	//-------------------------------------------------------------------------
	public String getInfoText(Wizard w) {
		return w.getArrowsHeader("Resulting rows will be sorted by the column(s) specified here");
	}

	//-------------------------------------------------------------------------
	public Container getPanel() {
		return this;
	}

	//-------------------------------------------------------------------------
	public boolean isEnabled(Button b) {
		switch (b) {
			case FINISH	:
			case NEXT	: 
			case BACK	: 
			case CANCEL	:
			default    	: return true;
		}
	}

	//-------------------------------------------------------------------------
	public boolean onButtonPress(Button b) {
		if (b == Button.FINISH) {
			updateOrdering();
		}
		return isEnabled(b);
	}

	//-------------------------------------------------------------------------
	public void onPageChange(boolean show) {
		if (show) {
			updateOrdering();
		}
	}

//	//-------------------------------------------------------------------------
//	public void onClose(java.util.EventObject event) {
//	}



	//=========================================================================
	//	Public methods

	//-------------------------------------------------------------------------
	public Page_Sorting(ViewCreationDialog owner) {
		this.owner = owner;  
		initialize();
	}

	//-------------------------------------------------------------------------
	/** Returns the ordering array. The array contains the indices of the columns
	 *  in 'cols'. 
	 */
	public int[] getOrdering() {
    	java.util.List<ViewCreationRule.Column> cols = owner.getColumns();
		int n = getTableData().getRowCount(), k = cols.size();
		ArrayList<Integer> ans = new ArrayList<Integer>(n); 
		for (int i = 0; i < n; ++i) {
			Object name = getTableData().getValueAt(i, 0);
			for (int j = 0; j < k; ++j) {
				if (cols.get(j).getName().equals(name)) {
					Object o = getTableData().getValueAt(i, 1);
					boolean ascending = Boolean.TRUE.equals(o);
					ans.add(ascending ? j : ~j);
					break;
				}
			}
		}
		return Utils.asIntArray(ans);
	}
	
	//-------------------------------------------------------------------------
	/* 
	 * Amikor ez a flag true, akkor updateColumns() automatikusan ujratolti a
	 * tablat (onPageChange/show es onButtonPress(FINISH) alkalmaval).
	 * Maskulonben csak a nemletezo oszlopokat szedi ki a tablabol. Ha ilyen 
	 * modon kiurul a tabla, akkor ez a flag beallitodik automatikusra. Ha a
	 * user kezzel belemodosit a tablaba, akkor ez a flag torlodik. 
	 */
	public UpdateMode	getUpdateMode()				{ return updateMode; }

	/** Precondition: owner.getColumns() is up-to-date.  */
	public void		setUpdateMode(UpdateMode m)	{ updateMode = m; updateOrdering(); }

	//-------------------------------------------------------------------------
	/*
	 * Frissiti a combobox listajat, es getUpdateMode()-nak megfeleloen
	 * frissiti a tabla tartalmat is. 
	 */
	/** Updates the table and the comboboxes of the table. */
	public void updateOrdering() {
		if (owner == null)
			return;
		boolean splitting = owner.isSplittingUsed();

		// Combobox frissitese es rendezheto oszlopok kigyujtese

		ArrayList<ViewCreationRule.Column> cols = owner.getColumns();
		HashSet<String> allowed = new HashSet<String>(cols.size());	// ezekre _lehet_ rendezni
		DefaultComboBoxModel cm = new DefaultComboBoxModel();
		for (ViewCreationRule.Column c : cols) {
			// Bontott oszlopokra nem engedunk rendezni. Tagolas eseten a vektoros oszlopokra sem.
//			if (c.isSplitted() || (splitting && !c.isScalar())) continue;
			if ((c.isSplitter() && c.isHidden()) || c.isSplitted() || (splitting && !c.isScalar())) continue;

			String name = c.getName();
			cm.addElement(name);
			allowed.add(name);
		}
		jCombobox.setModel(cm);

		// Torolt oszlopok eltavolitasa

		int n = getTableData().getRowCount();
		HashSet<String> used = new HashSet<String>(n);
		for (int i = n - 1; i >= 0; --i) {
			Object name = getTableData().getValueAt(i, 0);
			if (name == null || !allowed.contains(name))
				getTableData().removeRow(i);
			else
				used.add((String)name);
		}
		if (n > 0 && getTableData().getRowCount() == 0 
					&& getUpdateMode() == UpdateMode.FILL_FROM_USER_SELECTION) {
			// most urult ki, mert megszuntek az oszlopok
			setUpdateMode(UpdateMode.FILL_FROM_COLUMNS);
		}

		// Tagolas miatt hozzaveendok osszegyujtese

		mustBeUsed.clear();
		if (splitting) {
			TreeSet<String> in = new TreeSet<String>();
			TreeSet<String> out= new TreeSet<String>();
			ArrayList<String> thisview = new ArrayList<String>();	// itt abc helyett definicio-sorrend

			for (ViewCreationRule.Column c : cols) {
				if (!c.isGrouping() || c.isHidden()) continue;
				ArrayList<Utils.Pair<SrcType, String>> sources = c.getSource();
				if (sources.size() == 1) {
					SrcType t = sources.get(0).getFirst();
					if (t == SrcType.PROJECTION_INPUT)	{ in.add(c.getName()); continue; }
					if (t == SrcType.PROJECTION_OUTPUT)	{ out.add(c.getName());continue; }
				}
				thisview.add(c.getName());
			}

			for (String name : Utils.forAll(in, out, thisview))
				mustBeUsed.add(name);
		}

		// Frissites getUpdateMode() szerint

		n = getTableData().getRowCount();
		if (getUpdateMode() == UpdateMode.FILL_FROM_USER_SELECTION) {
			while (--n >= 0) getTableData().removeRow(n);				// kiuriti a tablat
			used.clear();
			for (String name : userSelection) {
				if (!used.contains(name) && allowed.contains(name)) {
					getTableData().addRow(new Object[] { name, true });
					used.add(name);
				}
			}
		} else if (getUpdateMode() == UpdateMode.FILL_FROM_COLUMNS) {
			while (--n >= 0) getTableData().removeRow(n);				// kiuriti a tablat
			used.clear();
		}
		else if (getUpdateMode() == UpdateMode.FILL_FROM_RULE) {
			while (--n >= 0) getTableData().removeRow(n);
			used.clear();
			ArrayList<ViewCreationRule.Column> rulecols = owner.getRule().getColumns();
			for (Integer i : owner.getRule().getOrdering()) {
				boolean asc = (i >= 0);
				ViewCreationRule.Column c = rulecols.get((asc) ? i : ~i);
				String name = c.getName();
				if (allowed.contains(name)) {
					getTableData().addRow(new Object[] { name, asc });
					used.add(name);
				}
			}
		}

		// Hianyzok hozzavetele

		for (String name : mustBeUsed) {
			if (!used.contains(name)) {
				getTableData().addRow(new Object[] { name, true });
				used.add(name);
			}
		}

		// Utolso utani sor uj elem felvetelehez
		n = getTableData().getRowCount();
		if (n < cm.getSize()) {
			getTableData().addRow(new Object[] { null, null });
			jTable.removeEditor();
		}
	}

	//=========================================================================
	//	Controller methods

	//-------------------------------------------------------------------------
	/** Enables/disables the buttons.  
	 *  Precondition: mustBeUsed[] is up-to-date */
	private void enableDisableButtons() {
		int idx = jTable.getSelectedRow();
		int n = getTableData().getRowCount();
		n -= (n <= 0) ? n : isEmptyRow(n-1);
		String curr = null;
		if (idx >= 0) {
			curr = (String)getTableData().getValueAt(idx, 0);
		}
		jMoveUpButton.setEnabled(curr!=null && idx > 0);
		jMoveDownButton.setEnabled(curr!=null && idx + 1 < n);
		jRemoveButton.setEnabled(curr!=null && idx >= 0 && mustBeUsed.indexOf(curr) < 0);
		
		moveUpAction.setEnabled(curr != null && idx > 0);
		moveDownAction.setEnabled(curr != null && idx + 1 < n);
		removeAction.setEnabled(curr != null && idx >= 0 && mustBeUsed.indexOf(curr) < 0);
	}

	//-------------------------------------------------------------------------
	/** Tests if the i-th row of the table is empty. */
	private int isEmptyRow(int i) {
		return getTableData().getValueAt(i, 0) == null ? 1 : 0;
	}

	//-------------------------------------------------------------------------
	public void actionPerformed(java.awt.event.ActionEvent e) {
		if (e.getSource() == jRemoveButton) {
			getTableData().removeRow(jTable.getSelectedRow());
			saveUserSelection();
			setUpdateMode(UpdateMode.FILL_FROM_USER_SELECTION);
			updateOrdering();
		}
		else if (e.getSource() == jMoveUpButton || e.getSource() == jMoveDownButton) {
			int idx = jTable.getSelectedRow();
			int newIdx = idx + (e.getSource() == jMoveUpButton ? -1 : 1);
			getTableData().swapRows(idx, newIdx);
			jTable.getSelectionModel().setSelectionInterval(newIdx, newIdx);
			saveUserSelection();
			setUpdateMode(UpdateMode.FILL_FROM_USER_SELECTION);
		}
		// Megj: ha a comboboxban kivalasztanak egy elemet akkor nem ez hivodik meg
		// hanem MyTableModel.setValueAt()
	}

	//-------------------------------------------------------------------------
	// Kivalasztottak egy sort a tablaban
    public void valueChanged(javax.swing.event.ListSelectionEvent e) {
    	enableDisableButtons();
    }
    
    //-------------------------------------------------------------------------
	public void mouseReleased(MouseEvent e) {
		if (!javax.swing.SwingUtilities.isRightMouseButton(e))
			return;
		if (e.getComponent().isEnabled()) {
			int idx = jTable.rowAtPoint(e.getPoint());
			if (idx == -1)
				idx = jTable.getRowCount() - 1;
			
			if (!ai.aitia.chart.util.Utilities.contains(jTable.getSelectedRows(),idx))
				jTable.getSelectionModel().setSelectionInterval(idx,idx);
			
			jTableContextMenu.show(e.getComponent(),e.getX(),e.getY());
		}
	}

	//--------------------------------------------------------------------------
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}

	//-------------------------------------------------------------------------
    @SuppressWarnings("unused")
    /** Table model of the sorting table. */
	private class MyTableModel extends DefaultTableModel {
    	boolean in_setValue = false;
    	// A comboboxban kivalasztottak egy elemet
    	@Override public void setValueAt(Object newValue, int row, int column) {
    		Object oldValue = getValueAt(row, column);
    		boolean save = in_setValue, en = false;
			try {
	    		if (!in_setValue) {
					in_setValue = true;
					if (column == 0 && newValue != null) {
						// Megnezzuk hogy az uj ertek elofordul-e masutt a tablaban
						// Ha igen, akkor megcsereljuk a kettot
						for (int other = 0, n = getRowCount(); other < n; ++other) {
							if (Utils.equals(newValue, getValueAt(other, column)) && other != row) {
								swapRows(row, other);
								if (oldValue == null) {
									removeRow(other);
									if (other < row) row -= 1;
								}
								break;
							}
						}
						// Ha uj elemet vettek fel, akkor az 'ascending' oszlopba is 
						// teszunk egy ertelmes erteket, az ures sort pedig potoljuk.
						if (oldValue == null && column == 0) {
							if (getValueAt(row, 1) == null)
								setValueAt(true, row, 1);			// rekurziv hivas
							addRow(new Object[] { null, null });
							en = true;
						}
						// Megjegyezzuk h. a user mar belenyult
						saveUserSelection();
						setUpdateMode(UpdateMode.FILL_FROM_USER_SELECTION);
					} else {
						// Megjegyezzuk h. a user mar belenyult
						saveUserSelection();
						setUpdateMode(UpdateMode.FILL_FROM_USER_SELECTION);
					}
	    		}
	    		super.setValueAt(newValue, row, column);
	    		if (en) enableDisableButtons();
			} 
			finally { in_setValue = save; }
    	}
    	@Override
    	public Class<?> getColumnClass(int columnIndex) {
    		return (columnIndex == 1) ? Boolean.class : super.getColumnClass(columnIndex);
    	}
    	@Override
    	public boolean isCellEditable(int row, int column) {
    		return (column == 0) || getValueAt(row, 0) != null;
    	}
		@SuppressWarnings("unchecked")
		void swapRows(int a, int b) {
			if (a == b) return;
			Vector v = getDataVector();
			Object tmp = v.get(a);
			v.set(a, v.get(b));
			v.set(b, tmp);
			fireTableRowsUpdated(Math.min(a, b), Math.max(a, b));
		}
    }

	//-------------------------------------------------------------------------
    private MyTableModel getTableData() {
    	return (MyTableModel)jTable.getModel();
    }

	//-------------------------------------------------------------------------
    private void saveUserSelection() {
    	userSelection.clear();
    	int n = getTableData().getRowCount();
		for (int i = 0; i < n; ++i) {
			Object name = getTableData().getValueAt(i, 0);
			if (name != null) userSelection.add((String)name);
		}
    }

	//=========================================================================
	//	GUI (View) methods

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() 
	{
		jTable.setModel(new MyTableModel());
		jTable.getSelectionModel().addListSelectionListener(this);
		jTable.addMouseListener(this);
		
		initializeContextMenu();

		Object[] headers = { "Column name", "Ascending" };
		getTableData().setColumnIdentifiers(headers);
		jTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(jCombobox));
		
		GUIUtils.addActionListener(this, jMoveUpButton, jMoveDownButton, jRemoveButton);
		GUIUtils.bind(this,JComponent.WHEN_IN_FOCUSED_WINDOW,"DELETE",jRemoveButton);
		
		enableDisableButtons();

		layoutGUI();
	}
	
	//-------------------------------------------------------------------------
	private void initializeContextMenu() {
		moveUpAction = new AbstractAction() {
			{ putValue(NAME,"Move up"); }
			public void actionPerformed(ActionEvent e) {
				jMoveUpButton.doClick();
			}
		};
		moveDownAction = new AbstractAction() {
			{ putValue(NAME,"Move down"); }
			public void actionPerformed(ActionEvent e) {
				jMoveDownButton.doClick();
			}
		};
		removeAction = new AbstractAction() {
			{ putValue(NAME,"Remove"); }
			public void actionPerformed(ActionEvent e) {
				jRemoveButton.doClick();
			}
		};
		jTableContextMenu.add(moveUpAction);
		jTableContextMenu.add(moveDownAction);
		jTableContextMenu.add(removeAction);
	}

	//-------------------------------------------------------------------------
	private void layoutGUI() {
		this.setLayout(new java.awt.BorderLayout());
		this.add(FormsUtils.build("f:p:g ~ d", "[DialogBorder]01 t:p:g", 
				new JScrollPane(jTable),
				FormsUtils.buttonStack(
						jMoveUpButton, jMoveDownButton, FormsUtils.BS_UNRELATED,
						jRemoveButton
				).getPanel()
		).getPanel(), java.awt.BorderLayout.CENTER);
	}

	//-------------------------------------------------------------------------
	public static void test(String[] args) {
		new Wizard(new Page_Sorting(null)).showInDialog(null).setVisible(true);
		System.exit(0);
	}
}
