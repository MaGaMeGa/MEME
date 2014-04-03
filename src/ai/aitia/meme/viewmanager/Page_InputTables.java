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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ViewRec;
import ai.aitia.meme.gui.MainWindow;
import ai.aitia.meme.gui.ResultsBrowser;
import ai.aitia.meme.gui.ResultsTree;
import ai.aitia.meme.gui.ViewsBrowser;
import ai.aitia.meme.gui.Wizard;
import ai.aitia.meme.gui.Wizard.Button;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

/** Wizard page "Base data" in the View Creation Wizard. */
public class Page_InputTables extends JPanel implements Wizard.IWizardPage, Wizard.ICloseListener, 
															Wizard.IArrowsInHeader,
															java.awt.event.ActionListener,
															java.awt.event.MouseListener,
															javax.swing.event.ListSelectionListener,
															javax.swing.ListCellRenderer
{
	private static final long serialVersionUID = 1L;
	private ViewCreationDialog owner = null;

	/** The selected input tables. */
	private ArrayList<Object>		selection	= new ArrayList<Object>();
	/** The index of the selected element of the "Selected input tables" list. */
	private int					selectedIdx = -1;
	private DefaultListCellRenderer listRenderer = new DefaultListCellRenderer();

	private JButton		jAddResultButton		= new JButton("Add result table");
	private JButton		jAddViewButton 			= new JButton("Add view table");
	private JButton		jRemoveButton 			= new JButton("Remove");
	private JButton		jMoveUpButton 			= new JButton("Move up");
	private JButton		jMoveDownButton 		= new JButton("Move down");
	private JList		jTablesList				= new JList();
	private JList		paramsList[]			= new JList[2];
	private JPopupMenu	jTablesListContextMenu 	= new JPopupMenu();
	
	private AbstractAction addResultAction 	= null;
	private AbstractAction addViewAction	= null;
	private AbstractAction removeAction		= null;
	private AbstractAction moveUpAction		= null;
	private AbstractAction moveDownAction	= null;

	private ResultsBrowser	rbr = null; 
	private ViewsBrowser	vbr = getVbr(); 

	//=========================================================================
	//	Public methods
	
	//-------------------------------------------------------------------------
	public Page_InputTables(ViewCreationDialog owner) {
		super();
		this.owner = owner;

		initialize();
		// The user can bypass this panel by pressing ENTER
		//GUIUtils.bind(this, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, "ENTER", owner.getWizard(), "next");
	}
	
	//-------------------------------------------------------------------------
	public ArrayList<Object> getInputTables() {
		return selection;
	}

	//-------------------------------------------------------------------------
	public void setSelection(ArrayList<Object> sources, boolean recollect) {
		selection = sources;
		updateSelection(recollect);
	}

	//=========================================================================
	//	Interface methods

	public String getTitle() {
		return "Base Data";
	}
	
	public String getInfoText(Wizard w) {
		return w.getArrowsHeader("Select source data for the computation");
	}

	public java.awt.Container getPanel() {
		return this;
	}

	public boolean isEnabled(Button b) {
		switch (b) {
			case NEXT : 
			case FINISH : return getInputTables().size() > 0;
			default : return true;
		}
	}

	public boolean onButtonPress(Button b) {
		return isEnabled(b);
	}

	public void onPageChange(boolean show) {
		//getJTree().requestFocusInWindow();
	}

	//-------------------------------------------------------------------------
	public void onClose(Wizard w) {
		if (rbr != null) { rbr.onClose(); rbr = null; }
		if (vbr != null) { vbr.onClose(); vbr = null; }
	}

	
	
	//=========================================================================
	//	Controller methods

	//-------------------------------------------------------------------------
	public void actionPerformed(java.awt.event.ActionEvent e) {
		if (e.getSource() == jAddResultButton) {
			if (getRbr().showInDialog(javax.swing.SwingUtilities.getWindowAncestor(this), null)) {
				addToSelection(getRbr().getSelection());
			}
		}
		else if (e.getSource() == jAddViewButton) {
			if (getVbr().showInDialog(javax.swing.SwingUtilities.getWindowAncestor(this), null, owner.getRule() == null ? null : owner.getRule().getName())) {
				addToSelection(getVbr().getSelectedViews());
			}
		}
		else if (e.getSource() == jRemoveButton && selectedIdx >= 0) {
			selection.remove(selectedIdx);
			updateSelection(true);
		}
		else if (e.getSource() == jMoveUpButton || e.getSource() == jMoveDownButton) {
			int newpos = selectedIdx + ((e.getSource() == jMoveUpButton) ? -1 : 1);
			Object o = selection.remove(selectedIdx);
			selection.add(newpos, o);
			updateSelection(false);		// clears selectedIdx
			jTablesList.setSelectedIndex(newpos);
		}
	}

	//-------------------------------------------------------------------------
	/** Adds 'o' to the selected input tables. 
	 *  'o' is expected to be a ViewRec[] or a Long[][] (results) */
	private void addToSelection(Object o) {
		ArrayList<Object> tmp = new ArrayList<Object>();
		if (o instanceof Long[][])
			tmp.addAll(java.util.Arrays.asList((Long[][])o));
		else
			tmp.addAll(java.util.Arrays.asList((ViewRec[])o));

		boolean changed = false;
		J:
		for (int j = selection.size() - 1; j >= 0; --j) {
			Object existing = selection.get(j);
			for (int i = tmp.size() - 1; i >= 0; --i) {
				switch (includes(existing, tmp.get(i))) {
					case -1: tmp.remove(i); continue J;						// 'existing' includes tmp[i]
					case  1: selection.remove(i); changed = true; continue J;	// vice versa 
				}
			}
		}
		if (!tmp.isEmpty()) {
			selection.addAll(tmp);
			changed = true; 
		}
		if (changed)
			updateSelection(true);
	}

	//-------------------------------------------------------------------------
	/** Checks the relationship between two selected elements. 
	 * @return -1:a includes b;  0:no relationship,  1:b includes a
	 */  
	private static int includes(Object a, Object b) {
		if (!a.getClass().equals(b.getClass())) return 0;
		if (a instanceof Long[]) {
			Long[] la = (Long[])a, lb = (Long[])b;
			if (0 != ResultsTree.LONG_ARRAY_COMP.compare(la, lb)) return 0;
			return (la.length <= lb.length) ? -1 : 1;
		}
		return a.equals(b) ? -1 : 0;
	}

	//-------------------------------------------------------------------------
	/** Updates the selection display. If 'recollect' is true then the parameters
	 *  list are also updated.*/
	private void updateSelection(boolean recollect) {
		jTablesList.setListData(selection.toArray());
		if (recollect)
			owner.beginUpdateParameters(false);
	}

	//-------------------------------------------------------------------------
	public void valueChanged(javax.swing.event.ListSelectionEvent e) {
		if (e.getValueIsAdjusting()) return;
		selectedIdx = jTablesList.getMinSelectionIndex();
		if (selectedIdx < 0 || selection.size() < selectedIdx)
			selectedIdx = -1;
		jRemoveButton.setEnabled(selectedIdx >= 0);
		jMoveUpButton.setEnabled(selectedIdx > 0);
		jMoveDownButton.setEnabled(0 <= selectedIdx && selectedIdx < selection.size() - 1);
		
		removeAction.setEnabled(selectedIdx >=0);
		moveUpAction.setEnabled(selectedIdx > 0);
		moveDownAction.setEnabled(0 <= selectedIdx && selectedIdx < selection.size() - 1);
	}

	//--------------------------------------------------------------------------
	public void mouseReleased(MouseEvent e) {
		if (!javax.swing.SwingUtilities.isRightMouseButton(e))
			return;
		if (e.getComponent().isEnabled()) {
			int idx = jTablesList.locationToIndex(e.getPoint());
			if (idx == -1)
				idx = jTablesList.getModel().getSize() - 1;
			
			if (!ai.aitia.chart.util.Utilities.contains(jTablesList.getSelectedIndices(),idx))
				jTablesList.setSelectedIndex(idx);
			
			jTablesListContextMenu.show(e.getComponent(),e.getX(),e.getY());
		}
	}

	//--------------------------------------------------------------------------
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}

	//=========================================================================
	//	GUI

	//-------------------------------------------------------------------------
	private void initialize() {
		GUIUtils.addActionListener(this, jAddResultButton, jAddViewButton, jRemoveButton, 
				jMoveUpButton, jMoveDownButton);

		jMoveUpButton.setEnabled(false);
		jMoveDownButton.setEnabled(false);
		jRemoveButton.setEnabled(false);
		GUIUtils.bind(this,JComponent.WHEN_IN_FOCUSED_WINDOW, "DELETE",jRemoveButton);
		
		initializeContextMenu();
		moveUpAction.setEnabled(false);
		moveDownAction.setEnabled(false);
		removeAction.setEnabled(false);

		listRenderer.setFont(jTablesList.getFont());
		jTablesList.setCellRenderer(this);
		jTablesList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		jTablesList.addListSelectionListener(this);
		jTablesList.addMouseListener(this);

		javax.swing.ListCellRenderer r = new ParameterSet.ParListCellRenderer();
		paramsList[0] = new JList();
		paramsList[0].setCellRenderer(r);
		paramsList[0].setEnabled(false);
		if (owner != null)
			paramsList[0].setModel(owner.getSelectedPars().getListModel(ParameterSet.Category.INPUT, -1));

		paramsList[1] = new JList();
		paramsList[1].setCellRenderer(r);
		paramsList[1].setEnabled(false);
		if (owner != null)
			paramsList[1].setModel(owner.getSelectedPars().getListModel(ParameterSet.Category.OUTPUT, -1));

		layoutGUI();
	}

	//------------------------------------------------------------------------
	public java.awt.Component getListCellRendererComponent(JList list, Object value, 
			int index, boolean isSelected, boolean cellHasFocus)
	{
		if (value instanceof Long[]) {
			Long[] l = (Long[])value;
			Model m = (owner == null) ? null : owner.getSelectedPars().getModelsCache().get(l[0]);
			Long batch = (l.length > 1) ? l[1] : null;
			String fmt = (batch	== null)? "%s/%s" : "Batch#%3$d of %s/%s";
			String name = null, version = null;
			if (m != null) { name = m.getName(); version = m.getVersion(); }
			listRenderer.setText(String.format(fmt, name, version, batch));
			listRenderer.setIcon((batch == null)	? ResultsBrowser.MyRenderer.MODELVER_ICON 
													: ResultsBrowser.MyRenderer.BATCH_ICON );
		} else {
			listRenderer.setText(value.toString());
			listRenderer.setIcon(VIEW_TABLE_ICON);
		} 
		return listRenderer;
	}
	public static final javax.swing.ImageIcon VIEW_TABLE_ICON = MainWindow.getIcon("view_table.png");


	//------------------------------------------------------------------------
	private ResultsBrowser getRbr() {
		if (rbr == null) {
			rbr = new ResultsBrowser();
			rbr.valueChanged(null);
		}
		return rbr;
	}

	//------------------------------------------------------------------------
	private ViewsBrowser getVbr() {
		if (vbr == null) {
			vbr = new ViewsBrowser(false,true);
			vbr.valueChanged(null);
		}
		return vbr;
	}

	//-------------------------------------------------------------------------
	private Page_InputTables layoutGUI() {
		this.setLayout(new BorderLayout());
		this.add(FormsUtils.build("p:g - m", "[DialogBorder]" + 
				"00|~|" +
				"12 f:p|~~|" +
				"33 f:m:g",
				"Selected result/view tables, which will provide the data for the computation, in this order:",
				new JScrollPane(jTablesList),
				FormsUtils.buttonStack( 
					jAddResultButton,
					jAddViewButton,
					FormsUtils.BS_UNRELATED,
					jRemoveButton,
					FormsUtils.BS_UNRELATED,
					jMoveUpButton,
					jMoveDownButton
				).getPanel(),
				FormsUtils.titledBorder(" Parameters in the selected tables ", 
						FormsUtils.build("- f:m:g - f:m:g - ", "02|-|13 f:m:g|-",
							"Input", new JScrollPane(paramsList[0]),
							"Output", new JScrollPane(paramsList[1])
				).getPanel())
		).getPanel());
		return this;
	}
	
	//------------------------------------------------------------------------
	@SuppressWarnings("serial")
	private void initializeContextMenu() {
		addResultAction = new AbstractAction() {
			{ putValue(NAME,"Add result table"); }
			public void actionPerformed(ActionEvent e) {
				jAddResultButton.doClick();
			}
		};
		addViewAction = new AbstractAction() {
			{ putValue(NAME,"Add view table"); }
			public void actionPerformed(ActionEvent e) {
				jAddViewButton.doClick();
			}
		};
		removeAction = new AbstractAction() {
			{ putValue(NAME,"Remove"); }
			public void actionPerformed(ActionEvent e) {
				jRemoveButton.doClick();
			}
		};
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
		jTablesListContextMenu.add(addResultAction);
		jTablesListContextMenu.add(addViewAction);
		jTablesListContextMenu.addSeparator();
		jTablesListContextMenu.add(removeAction);
		jTablesListContextMenu.add(moveUpAction);
		jTablesListContextMenu.add(moveDownAction);
	}

	//------------------------------------------------------------------------
	public static void test(String[] args) {
		new ai.aitia.meme.gui.Wizard(new Page_InputTables(null)).showInDialog(null).setVisible(true);
		System.exit(0);
	}
}
