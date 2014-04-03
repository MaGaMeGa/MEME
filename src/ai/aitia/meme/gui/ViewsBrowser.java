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
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.ConnChangedEvent;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.database.ViewRec;
import ai.aitia.meme.database.ViewsDb.ViewsDbChangeEvent;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.gui.lop.RQ;
import ai.aitia.meme.gui.lop.UserBreakException;
import ai.aitia.meme.pluginmanager.Parameter;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.viewmanager.JTableScrollPane;
import ai.aitia.meme.viewmanager.ViewCreationRule;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;

/** This class represents the main component of the Views panel. It contains 
 *  the views list, the informations display and the data display.
 *  Don't forget to call the onClose() method if you use this class */ 
public class ViewsBrowser extends JPanel implements java.awt.event.ActionListener,  
													  javax.swing.event.ListSelectionListener,
													  ai.aitia.meme.database.IConnChangedListener,
													  ai.aitia.meme.database.ViewsDb.IViewsDbChangeListener,
													  JTableScrollPane.IDataTypeCalculator
{
	private static final long serialVersionUID = 1L;
	private static enum TableMode { SHOW_COLUMNS, SHOW_ROWS, TOGGLE, HIDE_BUTTON };

	/** The storage of all available views. */
	private final Vector<ViewRec>	 allViews;	// EDT and Model thread
	/** The storage of the curretly accepted views by the filter expression. */
	private final Vector<ViewRec>	 views;		// EDT and Model thread
	/** The index of the last selected view. */
	private int current = -1;
	/** The creation rule of the last selected view. */
	 ViewCreationRule currentRule = null;	// used by ViewsPanel when starting the wizard to recreate the view
	/** Information of the columns of the last selected view. */
	private Columns currentCols = null;		//!< in database-order. SQL column names are valid.
	/** The number of the rows in the last selected view. */
	private long currentNRows = 0;
	/** The original description of the last selected view. */
	private String origDesc = null;
	private RQ.Request refreshViewsIsPending = null;
	/** Flag that determines whether the right panel contains the rows of the last selected
	 *  view or the informations about it.
	 */
	private boolean rowTable = false;
	/** Flag that determines whether this browser is used in a dialog or not. */
	private boolean embedded = false;
	private boolean multipleSelection = true;
	
	/** The name of the view that cannot appears in the list even if the filter expression
	 *  is accepted.
	 */
	private String except_view = null;
	private JButton showInDialogOKButton = null;
	
	private JScrollPane jViewsListScrollPane = null;
	private JList jViewsList = null;
	private JSplitPane jSplitPane = null;
	private JPanel jInfoPanel = null;
	private JTableScrollPane jTableScrollPane = null;
	private JScrollPane jDescriptionScrollPane = null;
	private JTextArea jDescriptionTextArea = null;
	private JButton jSaveDescButton = null;
	private JLabel jDescriptionLabel = null;
	private JPanel jBottomInfoPanel = null;
	private JLabel jBottomInfoLabel = null;
	private CardLayout jCard = null;
	private JPanel jTablePanel = null;
	private JScrollPane jDataScrollPane = null;
	private JTable jDataTable = null;
	private JPanel jBottomInfoIntPanel = null;
	private JButton jToggleTableButton = null;
	private JPanel jFilterPanel = null;
	private JLabel jLabel = null;
	private JTextField jFilterTextField = null;
	private JButton jFilterButton = null;

	
	//=========================================================================
	//	Public interface

	//-------------------------------------------------------------------------
	public ViewsBrowser(boolean usedByViewsPanel, boolean multipleSelection) {
		super();
		embedded = !usedByViewsPanel;
		this.multipleSelection = multipleSelection;
		allViews = new Vector<ViewRec>();
		views = new Vector<ViewRec>();

		initialize();

		MEMEApp.getDatabase().connChanged.addWeakListener(this);
		MEMEApp.getViewsDb().viewsDbChanged.addWeakListener(this);
		
		jDataScrollPane.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if (rowTable) 
					GUIUtils.updateColumnWidths(getJDataTable(),getJDataScrollPane());
			}
		});
	}

	//-------------------------------------------------------------------------
	/**
	 * The container of 'this' JPanel is responsible to call this method
	 * when the container is disposed, to remove various listeners from 
	 * external objects, which cannot be removed automatically.
	 * Note: this method clears the selection, thus related methods 
	 * (getSelectedView(), getColumnsOfSelectedView() etc.) should 
	 * not be used after this method.         
	 */
	public void onClose() {
		MEMEApp.getDatabase().connChanged.removeListener(this);
		MEMEApp.getViewsDb().viewsDbChanged.removeListener(this);
		if (jViewsList != null) {
			jViewsList.setListData(new Object[0]);
			jViewsList = null;
			if (jInfoPanel != null) {
				jInfoPanel.removeAll();
				jInfoPanel = null;
			}
		}
	}

	//-------------------------------------------------------------------------
	/** Returns the last selected view object. */
	public ViewRec getSelectedView() {
		return (current < 0) ? null : views.get(current);
	}

	//-------------------------------------------------------------------------
	/** Returns the selected view objects. */
	public ViewRec[] getSelectedViews() {
		if (getJViewsList().getSelectedIndices() == null ||
			getJViewsList().getSelectedIndices().length == 0) return null;
		int[] temp = getJViewsList().getSelectedIndices();
		ViewRec[] ans = new ViewRec[temp.length];
		for (int i=0;i<temp.length;++i) ans[i] = views.get(temp[i]);
		return ans;
	}
	
	//-------------------------------------------------------------------------
	public long getNrOfRowsOfSelectedView() {
		return currentNRows;
	}

	//-------------------------------------------------------------------------
	/**
	 * Looks for the specified view primarily by view_id, if not found then by name.
	 * Returns false if the view is not found.
	 */
	public boolean setSelectedView(Long view_id, String name) {
		int idx = -1;
		if (view_id != null) {
			idx = views.indexOf(new ViewRec(name, view_id));
		}
		if (name != null) {
			for (int i = 0, n = views.size(); i < n && idx < 0; ++i) {
				if (name.equals(views.get(i).getName())) 
					idx = i;
			}
		}
		if (idx >= 0) {
			getJViewsList().setSelectedIndex(idx);
			getJViewsList().ensureIndexIsVisible(idx);
		}
		return idx >= 0;
	}

	//-------------------------------------------------------------------------
	/** 
	 * Returns null if the selection is empty. The returned information is valid
	 * only after the info-loading is finished. In this case the columns are returned
	 * in database-order, with actual SQL column names (not re-generated SQL column names).
	 */
	public Columns getColumnsOfSelectedView() {
		return currentCols;
	}

	//-------------------------------------------------------------------------
	/** 
	 * Creates a modal JDialog and displays 'this' in that. Note that 'this'
	 * remains intact when the dialog is closed. 
	 * @return false if the dialog was cancelled.   
	 *          Use getSelectedView() to retrieve the first item of the user's selection.<br>
	 *          Use getSelectedViews() to retrieve all items of the user's selection.
	 */  
	public boolean showInDialog(java.awt.Window parent, String title, String view) {
		final javax.swing.JDialog dialog[] = { null };
		final boolean ans[] = { false }; 

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(this);
		
		showInDialogOKButton = new JButton("Select");
		showInDialogOKButton.setEnabled(getSelectedView() != null);
		JButton cancel = new JButton("Cancel");
		java.awt.event.ActionListener listener = new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				ans[0] = (e.getSource() == showInDialogOKButton);
				dialog[0].dispose();
			}
		};
		showInDialogOKButton.addActionListener(listener);
		cancel.addActionListener(listener);

		panel.add(ButtonBarFactory.buildOKCancelBar(showInDialogOKButton, cancel), BorderLayout.SOUTH);
		panel.setBorder(Borders.DIALOG_BORDER);

		if (title == null)
			title = "Select a view table";
		dialog[0] = GUIUtils.createDialog(parent, true, title, panel);
		GUIUtils.disposeOnClose(dialog[0]);	// only the dialog will be disposed, not 'this' browser panel!
		if (parent != null)
			dialog[0].setLocationRelativeTo(parent);

		except_view = view;
		filterViews();
		
		dialog[0].setVisible(true);
		showInDialogOKButton = null;

		return ans[0]; 
	}


	//=========================================================================
	//	Controller methods

	//-------------------------------------------------------------------------
	/** This method is public as an implementation side effect. Do not call or override. */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == getJToggleTableButton()) {
			displayData(TableMode.TOGGLE);

		} else if (e.getSource() == getJSaveDescButton()) {
			assert(getSelectedView() != null);
			MEMEApp.LONG_OPERATION.begin("Saving description...", new Runnable() {
				long view_id = getSelectedView().getViewID();
				String desc = getJDescriptionTextArea().getText().trim();
				public void run() { MEMEApp.getViewsDb().setDescription(view_id, desc); }
			});
		} else if (e.getSource() == getJFilterTextField() ||
				   e.getSource() == getJFilterButton()) {
			filterViews();
			getJViewsList().setListData(views);
		}
	}

	//-------------------------------------------------------------------------
	/** This method is public as an implementation side effect. Do not call or override. */
	// EDT or Model thread
	public void onConnChange(ConnChangedEvent event) {
		Utils.invokeLater(this, "valueChanged", (ListSelectionEvent)null );
	}

	//-------------------------------------------------------------------------
	/** This method is public as an implementation side effect. Do not call or override. */
	// EDT or Model thread
	public void onViewsDbChange(ViewsDbChangeEvent e) {
		if (!SwingUtilities.isEventDispatchThread()) {
			Utils.invokeLater(this, "onViewsDbChange", e);
			return;
		}
		switch (e.getAction()) {
			case ADDED : 
			case REMOVED : 
				refreshViewsList();
				break;
			case MODIFIED: 
				if (getSelectedView() != null && getSelectedView().getViewID() == e.getViewID()) {
					refreshInfo();
				}
//				int i = views.indexOf(e.getViewRec());								// i < 0 when this view has just been created
//				if (i >= 0 && !views.get(i).getName().equals(e.getViewName())) {	// it has been renamed
//					views.set(i, e.getViewRec());
//					getJViewsList().setListData(views);								// ???
//				}
				int i = allViews.indexOf(e.getViewRec());								// i < 0 when this view has just been created
				if (i >= 0 && !allViews.get(i).getName().equals(e.getViewName())) {	// it has been renamed
					allViews.set(i, e.getViewRec());
					filterViews();
					getJViewsList().setListData(views);								// ???
					getJViewsList().setSelectedValue(e.getViewRec(),true);
				}
				break;
		}
	}

	//-------------------------------------------------------------------------
	/**
	 * This method is public as an implementation side effect. Do not call or override.
	 * 
	 * It is called by the list when clicking on an item, and also called from
	 * {@link #onConnChange(ConnChangedEvent)} after connecting to a database 
	 * to refresh the contents of the list and the info panel. 
	 * In this latter case 'e' is null.
	 */
	// EDT only
	public void valueChanged(ListSelectionEvent e) {
		if (e == null) {					// connection or views db change
			refreshViewsList();
		}
		else if (e.getValueIsAdjusting())	// don't act until the end of the modifications
			return;

		int before = current;

		if (getJViewsList().getSelectionModel() instanceof NonSortedListSelectionModel) {
			NonSortedListSelectionModel model = (NonSortedListSelectionModel)getJViewsList().getSelectionModel();

			//		current = getJViewsList().getMinSelectionIndex();
			current = model.getLastSelectionIndex();
		} else
			current = getJViewsList().getSelectedIndex();
		
		if (current >= views.size())
			current = -1;

		if (before != current) refreshInfo();

		if (!embedded && (current ^ before) < 0)	// == if their signs are different
			MEMEApp.getMainWindow().whenAViewIsSelected.fireLater();
	}

	//-------------------------------------------------------------------------
	// EDT only
	/** Refreshes the list of the views. It updates both the 'allViews' and
	 *  'view' vectors.  */
	private void refreshViewsList() {
		if (refreshViewsIsPending != null)
			return; 

		refreshViewsIsPending = MEMEApp.LONG_OPERATION.begin("Listing views...", new LongRunnable() {
			java.util.List<ViewRec> newviews; 
			@Override public void run() {
				if (MEMEApp.getDatabase().getConnection() != null) {
					newviews = MEMEApp.getViewsDb().getViews();
					java.util.Collections.sort(newviews);
				}
			}
			@Override public void finished() throws Exception {
				if (newviews != null) {
//					ViewRec selected = getSelectedView();
//					views.clear();
//					views.addAll(newviews);
//					getJViewsList().setListData(views);
//					if (selected != null)
//						setSelectedView(selected.getViewID(), selected.getName());
					ViewRec selected = getSelectedView();
					allViews.clear();
					allViews.addAll(newviews);
					filterViews();
					getJViewsList().setListData(views);
					if (selected != null)
							setSelectedView(selected.getViewID(), selected.getName());
				}
				refreshViewsIsPending = null;
			}
		});
	}
	
	//-------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	/** Creates a filtered view list. The filter expression is a regular expression
	 *  with Java syntax.
	 */
	private void filterViews() {
		getJViewsList().clearSelection();
		current = -1;
		currentRule = null;
		currentCols = null;
		currentNRows = 0;
		Vector<ViewRec> copy = new Vector<ViewRec>(views);
		views.clear();
		String regexp = getJFilterTextField().getText().trim();
		if (regexp.equals("")) views.addAll(allViews); 
		else {
			try {
				Pattern p = Pattern.compile(regexp);
				for (ViewRec vr : allViews) {
					Matcher m = p.matcher(vr.getName());
					if (m.matches()) views.add(vr);
				}
			} catch (PatternSyntaxException e) {
				views.addAll(copy);
				MEMEApp.logError("Invalid regular expression: %s",regexp);
				MEMEApp.userErrors(null,String.format("Invalid regular expression: %s",regexp));
			} finally {
				copy.clear();
				copy = null;
			}
		}
		if (except_view != null) {
			ViewRec vr = null;
			for (ViewRec v : views) {
				if (v.getName().equals(except_view)) {
					vr = v;
					break;
				}
			}
			if (vr != null) views.remove(vr);
		}
		refreshInfo();
	}

	//-------------------------------------------------------------------------
	/** Refreshes the informations about the last selected view. */ 
	private void refreshInfo() {
		LongRunnable task = new LongRunnable() {
			Long view_id = (getSelectedView() != null) ? getSelectedView().getViewID() : null;
			long nRows = 0;
			String desc = null;
			ViewCreationRule rule = null;
			Columns cols = null;
			@Override public void trun() throws UserBreakException {	// precondition: view_id != null
				MEMEApp.LONG_OPERATION.progress(0, 4);
				cols = MEMEApp.getViewsDb().getColumns(view_id);	// must be non-null when rule != null
				MEMEApp.LONG_OPERATION.progress(1);
				rule = (cols == null) ? null : MEMEApp.getViewsDb().getRule(view_id);
				MEMEApp.LONG_OPERATION.progress(2);
				desc = MEMEApp.getViewsDb().getDescription(view_id, false);
				MEMEApp.LONG_OPERATION.progress(3);
				nRows= MEMEApp.getViewsDb().getNrOfRows(view_id);
			}
			@Override public void finished() throws Exception {
				if (getReq() != null && getReq().getError() != null)
					rule = null;
				currentRule = rule;
				currentCols = cols;
				currentNRows= nRows;
				displayDesc(desc);
				String text = "";
				if (rule != null) {
					String msgs[] = { "Contains no rows",
									  "Contains 1 row",
									  "Contains %d rows",
									  " no columns",
									  " 1 column",
									  " %d columns"};
					text = String.format(msgs[(currentNRows < 1) ? 0 : (currentNRows == 1 ? 1 : 2)], currentNRows);
					text += ", " + String.format(msgs[currentCols.size() < 1 ? 3 : (currentCols.size() == 1 ? 4 : 5)],currentCols.size()); 
					displayData(TableMode.SHOW_COLUMNS);
					rule = displayableRule(rule, cols);
				} else {
					if (view_id != null)
						text = MEMEApp.seeTheErrorLog("Error while loading information. %s."); 
					displayData(TableMode.HIDE_BUTTON);
				}
				jBottomInfoLabel.setText(text);

				getJTableScrollPane().fill(rule, ViewsBrowser.this);	// this will call getViewColDataType()
				MEMEApp.getLF().treatAsBigTable(getJTableScrollPane().getJTable(),
						getJTableScrollPane().getData().getRowCount() > 200);
				getDataTableModel().clear(true);

				if (showInDialogOKButton != null) 
					showInDialogOKButton.setEnabled(view_id != null);
			}
		};
		if (getSelectedView() != null) 
			MEMEApp.LONG_OPERATION.begin("Reading information about the selected view...", task); 
		else try {
			task.finished(); 
		} catch (Exception e) {
			MEMEApp.logExceptionCallStack("ViewsBrowser.refreshInfo()", e);
		}
	}
	
	//-------------------------------------------------------------------------
	/** It displays a 'Save' button above the description fieid if the description 
	 *  is changed.
	 *  This method is public as an implementation side effect. Do not call or override. */
	// It is public because it is used with reflection, too
	public void displayOrHideSaveDescButton() {
		String desc = getJDescriptionTextArea().getText().trim();
		boolean dirty = !desc.equals(origDesc == null ? "" : origDesc); 
		getJSaveDescButton().setVisible(dirty);
	}

	//-------------------------------------------------------------------------
	/** Displays in the description field the new description 's'. */
	private void displayDesc(String s) {
		origDesc = s;
		if (s == null) {
			getJDescriptionTextArea().setText("");
			getJDescriptionScrollPane().setVisible(false);
		} else {
			getJDescriptionTextArea().setText(s);
			getJDescriptionScrollPane().setVisible(true);
		}
		displayOrHideSaveDescButton();
		getJInfoPanel().validate();
	}

	//-------------------------------------------------------------------------
	/** Displays the column information or the rows of the selected view according to
	 *  the value of the parameter.
	 */ 
	private void displayData(TableMode mode) {
		String btnlabel[] = { "Show rows", "Show columns" };
		int idx = 0;
		switch (mode) {
			default : assert(false) : mode; 
			case SHOW_COLUMNS : idx = 0; break;
			case SHOW_ROWS    : idx = 1; break;
			case TOGGLE	: 
				idx = java.util.Arrays.asList(btnlabel).indexOf(getJToggleTableButton().getText());
				idx = (idx < 0) ? 0 : 1 - idx;
				break;
			case HIDE_BUTTON :
				getJToggleTableButton().setVisible(false);
				return;
		}
		getJToggleTableButton().setText(btnlabel[idx]);
		getJToggleTableButton().setVisible(true);
		if (idx == 0) {		// show columns info
			rowTable = false;
			jCard.show(getJTableScrollPane().getParent(), getJTableScrollPane().getName());
		} else {			// show data rows
			if (getSelectedView() != null) {
				getDataTableModel().openViewTable(getSelectedView().getViewID(), (int)currentNRows);
			}
			rowTable = true;
			jCard.show(getJDataScrollPane().getParent(), getJDataScrollPane().getName());
		}
	}

	//-------------------------------------------------------------------------
	// A rejtett oszlopokat nem kell mutatni, a bontott oszlopoknak pedig a
	// peldanyait kell mutatni a %-jeles sablon helyett. Ezt ugy erjuk el, 
	// hogy rule-rol masolatot keszitunk, modositjuk, es azt jelenittetjuk 
	// meg a tablaban.
	/** Modifies and returns the 'rule' by the 'cols'. We do this, because the hidden
	 * columns isn't need to display and we need to display the instances of the 
	 * splitted columns instead of the templates.
	 */
	private static ViewCreationRule displayableRule(ViewCreationRule rule, Columns cols) {
		ArrayList<ViewCreationRule.Column> splitted = new ArrayList<ViewCreationRule.Column>();
		HashMap<String, ViewCreationRule.Column> names = new HashMap<String, ViewCreationRule.Column>();
		for (ViewCreationRule.Column col : rule.getColumns()) {
			if (col.isHidden()) continue;
			if (col.isSplitted()) {
				splitted.add(col);
				continue;
			}
			names.put(col.getName(), col);
		}
		ViewCreationRule ans = rule.clone();
		ans.clearColumns();
		int possiblySplitted = 0, ss = splitted.size();
		for (Parameter p : cols) {
			ViewCreationRule.Column col = names.remove(p.getName());
			if (col == null) {
				if (ss > 0) {
					// Pozicio alapjan feleltetunk meg neki egy splitted oszlopot
					col = splitted.get(possiblySplitted++ % ss).clone();
				} else {
					// Elvileg ilyen nem lehet, dehat biztos ami ziher...
					col = new ViewCreationRule.Column(ans);
					col.setInitialDataType(p.getDatatype());
				}
				col.setName(p.getName());
			} else {
				col = col.clone();	// muszaj duplikalni mert masik Node ala kerul majd be
			}						// es egyszerre csak 1 Node alatt lehet
			ans.addColumn(col);
		}
		return ans;
	}


	//-------------------------------------------------------------------------
	/** This method is public as an implementation side effect. Do not call or override.<br>
	 * It is used from {@link #refreshInfo()}.
	 * Precondition: the selection is not empty and the selection info has been loaded
	 * (the long operation which was scheduled by refreshInfo() has (almost) been
	 * finished; precisely: 'currentCols' is up-to-date).
	 * It returns the actual database column type of the specified column, including scripted
	 * and plugin-columns, too. Returns null if the database table does not contain the
	 * requested column.
	 */
	// EDT only
	public ColumnType getViewColDataType(ViewCreationRule.Column col, int idx) {
		ColumnType ans = null;
		if (col != null) {
			Columns c = getColumnsOfSelectedView();
			int i = c.indexOf(col.getName());
			if (i >= 0) ans = c.get(i).getDatatype();
			else ans = col.getInitialDataType();
		}
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Returns the table model of the table that display the rows of a view. */
	private RowsData getDataTableModel() {
		javax.swing.table.TableModel ans = getJDataTable().getModel();
		return (ans instanceof RowsData) ? (RowsData)ans : null;
	}

	//-------------------------------------------------------------------------
	/** The table model of the table that display the rows of the last selected view. */ 
	@SuppressWarnings("serial")
	static class RowsData extends javax.swing.table.AbstractTableModel implements Runnable {
		/** Id of the selected view. */
		volatile long			view_id = 0;							// [EDT & Model thread]
		/** The number of the displayable rows. */
		int						nrows = 0;								// [EDT]
		/** Informations about the columns. */
		volatile Columns		columns = null;							// [EDT & Model thread]

		int						cacheOffset = 0;						// ID of the first row in the cache [EDT & Model thread]
		/** Cache of the rows. */
		Vector<GeneralRow>		cache = new Vector<GeneralRow>();		// [EDT & Model thread]
		volatile boolean		pendingFill = false;					// [EDT & Model thread]
		JTable					jTable = null;							// reference table, used to calculate 
																		// cache size & update column widths
		JScrollPane				jTableScp = null;						// scrollpane of jTable

		//---------------------------------------------------------------------
		// TableModel methods

		public synchronized int getColumnCount() {					// [EDT]
			return (columns == null) ? 1 : columns.size() + 1;
		}

		@Override
		public synchronized String getColumnName(int column) {		// [EDT]
			if (--column < 0)
				return "#";
			return (columns == null) ? "" : columns.get(column).getName();
		}

		@Override
		public synchronized Class<?> getColumnClass(int columnIndex) {// [EDT]
			if (--columnIndex < 0)
				return Integer.class; 

			ColumnType t = (columns == null) ? null : columns.get(columnIndex).getDatatype();
//			return t.getJavaClass();
			// fix of Redmine bug #603
			return t == null ? String.class : t.getJavaClass();
		}

		public int getRowCount() {										// [EDT]
			return nrows;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {		// [EDT]
			if (--columnIndex < 0)
				return rowIndex;
			GeneralRow row = requestRow(rowIndex);
			return (row == null) ? null : row.get(columnIndex);
		}

		//---------------------------------------------------------------------
		// Non-private methods

		/** Sets the table and the scroll pane that are use this model. */
		void setReferenceTable(JTable refTable, JScrollPane refTablescrollPane) {
			jTable   = refTable;
			jTableScp= refTablescrollPane;
		}

		/** Opens a view table identified by 'view_id'  It will 
		 *  display 'nrows' rows.
		 */
		synchronized void openViewTable(long view_id, int nrows) {	// [EDT]
			clear(false);
			this.view_id	= view_id;
			this.nrows		= nrows;
			requestRow(0);
		}

		/** Clears the model. If 'notify' is true the model notifies its observers
		 *  about the change.
		 */
		synchronized void clear(boolean notify) {						// [EDT]
			nrows		= 0;
			columns		= null;
			cacheOffset	= 0;
			cache.clear();
			pendingFill	= false;
			if (notify)
				Utils.invokeLater(this, "fireTableStructureChanged");
		}

		//---------------------------------------------------------------------
		// Internals

		/** Returns the row identified by 'rowidx'.  
		 */
		private synchronized GeneralRow requestRow(int rowidx) {		// [EDT]
			if (rowidx < cacheOffset || cacheOffset + cache.size() <= rowidx) {
				int maxCacheSize = calcMaxCacheSize();
				cacheOffset = Math.max(0, rowidx - maxCacheSize / 2);
				cache.clear();
				cache.addAll(java.util.Collections.nCopies(Math.min(nrows - cacheOffset, maxCacheSize), (GeneralRow)null));
				updateCache();
			}
			int i = rowidx - cacheOffset;
			return (0 <= i && i < cache.size()) ? cache.get(i) : null;
		}
		
		/** Updates the content of the cache. */
		private void updateCache() {									// [EDT or Model thread]
			if (EventQueue.isDispatchThread()) {
				// [EDT only]
				if (!pendingFill) {
					pendingFill = true;
					MEMEApp.LONG_OPERATION.begin("Loading rows from the view table...", this);
				}
				return;
			}
			// [Model thread only]
			// The following cycle is needed because 'cacheOffset' may be modified in the EDT thread
			// during the database operation.
			boolean columnsChanged = false;
			while (true) {
				long vwid;
				int offs, n;
				synchronized (this) {
					vwid = view_id;
					offs = cacheOffset;
					n = cache.size();
				}
				Vector<GeneralRow> tmp = new Vector<GeneralRow>(n);
				Iterator<GeneralRow> it = MEMEApp.getViewsDb().getRows(vwid, offs, n).iterator();
				try {
					while (tmp.size() < n && it.hasNext()) {
						GeneralRow row = it.next();
						if (row == null)				// database error
							break;
						tmp.add(row);
						synchronized (this) {			// detect changes in cacheOffset
							n = cache.size();
							if (cacheOffset != offs) break;
							if (columns == null) {
								columns = row.getColumns();
								columnsChanged = true;
							}
						}
					}
				} finally {
					MEMEApp.getViewsDb().close(it);
				}
				synchronized (this) {					// detect changes in cacheOffset
					if (cacheOffset == offs) {
						n = cache.size();
						cache = tmp;
						// Jelenleg a Model szalban vagyunk, az alabbiaknak pedig 
						// az EDT szalban kell futniuk - ezert kell invokeLater
						if (!cache.isEmpty()) {
							if (columnsChanged)
								Utils.invokeLaterNonPublic(this, "columnsChanged");
							else
								Utils.invokeLater(this, "fireTableRowsUpdated", cacheOffset, cacheOffset + n - 1);
						}
						if (nrows > 1000)
							Utils.invokeLater(System.class, "gc");
						return;
					}
				}
			}
		}

		public void run() {											// [Model thread]
			updateCache();
			pendingFill = false;
		}

		/** Calculates and returns the maximum size of the cache. */
		private int calcMaxCacheSize() {								// [EDT only]
			int rh = 16, ch = 0;	// row height, component height
			if (jTable != null) {
				rh = jTable.getRowHeight(); if (rh == 0) rh = 16;
				java.awt.Container c = jTable.getParent();
				if (c instanceof JViewport) {
					ch = c.getHeight() - GUIUtils.getOverhead(c).height;
				} else {
					ch = jTable.getHeight();
				}
			}
			return Math.max(3 * (ch / rh), 16);
		}

		// Ezt a muveletet csak reflection-el hasznaljuk (azert nem private hogy ne legyen warning) 
		/** Notifies the listeners that the table is changed and resizes the table. */
		void columnsChanged() {											// [EDT only]
			fireTableStructureChanged();
			GUIUtils.updateColumnWidths(jTable, jTableScp);
		}
	}
	
	//-------------------------------------------------------------------------
	/** This selection models always returns the last selected element (in time,
	 *  not in position), when it is asked for only one selected element. 
	 */  
	static class NonSortedListSelectionModel implements ListSelectionModel {
		
		//========================================================================
		// members

		/** List of obseervers. */
		private List<ListSelectionListener> listeners = new ArrayList<ListSelectionListener>();
		/** Indices of the selected elements. */
		private List<Integer> indices = new ArrayList<Integer>();
		
		private int selectionMode = MULTIPLE_INTERVAL_SELECTION;
		private int anchorIndex = -1;
		private int leadIndex = -1;
		private boolean isAdjusting = false;
		
		//========================================================================
		// implemented interface methods

		//------------------------------------------------------------------------
		public void addListSelectionListener(ListSelectionListener x) {
			listeners.add(x);
		}

		//------------------------------------------------------------------------
		public void addSelectionInterval(int index0, int index1) {
			anchorIndex = index0;
			leadIndex = index1;
			for (int i=index0;i<=index1;++i) indices.add(new Integer(i));
			fireValueChanged();
		}

		//-------------------------------------------------------------------------
		public void clearSelection() {
			indices.clear();
			fireValueChanged();
		}

		//-------------------------------------------------------------------------
		public int getAnchorSelectionIndex() {
			return anchorIndex;
		}

		//-------------------------------------------------------------------------
		public int getLeadSelectionIndex() {
			return leadIndex;
		}

		//-------------------------------------------------------------------------
		public int getMaxSelectionIndex() {
			if (indices.isEmpty()) return -1;
			int result = -1;
			for (Integer i : indices) {
				if (i.intValue() > result) result = i.intValue();
			}
			return result;
		}

		//-------------------------------------------------------------------------
		/** Returns the last selected index. */
		public int getLastSelectionIndex() {
			if (indices.isEmpty()) return -1;
			return indices.get(indices.size()-1);
		}
		
		//-------------------------------------------------------------------------
		public int getMinSelectionIndex() {
			if (indices.isEmpty()) return -1;
			int result = Integer.MAX_VALUE;
			for (Integer i : indices) {
				if (i.intValue() < result) result = i.intValue();
			}
			return result;
		}

		//-------------------------------------------------------------------------
		public int getSelectionMode() {
			return selectionMode;
		}

		//-------------------------------------------------------------------------
		public boolean getValueIsAdjusting() {
			return isAdjusting;
		}

		//-------------------------------------------------------------------------
		public void insertIndexInterval(int index, int length, boolean before) {
			// TODO implemented later (what must this method does?) 
		}

		//-------------------------------------------------------------------------
		public boolean isSelectedIndex(int index) {
			Integer i = new Integer(index);
			return indices.contains(i);
		}

		//-------------------------------------------------------------------------
		public boolean isSelectionEmpty() {
			return indices.isEmpty();
		}

		//--------------------------------------------------------------------------
		public void removeIndexInterval(int index0, int index1) {
			// TODO implemented later (what must this method does?)
		}

		//--------------------------------------------------------------------------
		public void removeListSelectionListener(ListSelectionListener x) {
			listeners.remove(x);
		}

		//--------------------------------------------------------------------------
		public void removeSelectionInterval(int index0, int index1) {
			anchorIndex = index0;
			leadIndex = index1;
			for (int i=index0;i<=index1;++i) indices.remove(new Integer(i));
			fireValueChanged();
		}

		//--------------------------------------------------------------------------
		public void setAnchorSelectionIndex(int index) {
			anchorIndex = index;
			Integer i = new Integer(index);
			if (!indices.contains(i)) indices.add(i);
			fireValueChanged();
		}

		//-------------------------------------------------------------------------
		public void setLeadSelectionIndex(int index) {
			leadIndex = index;
			Integer i = new Integer(index);
			if (!indices.contains(i)) indices.add(i);
			fireValueChanged();
		}

		//-------------------------------------------------------------------------
		public void setSelectionInterval(int index0, int index1) {
			anchorIndex = index0;
			leadIndex = index1;
			indices.clear();
			for (int i=index0;i<=index1;++i) indices.add(new Integer(i));
			fireValueChanged();
		}

		//-------------------------------------------------------------------------
		public void setSelectionMode(int selectionMode) {
			switch (selectionMode) {
			case SINGLE_SELECTION :
			case SINGLE_INTERVAL_SELECTION :
			case MULTIPLE_INTERVAL_SELECTION :
			    this.selectionMode = selectionMode;
			    break;
			default:
			    throw new IllegalArgumentException("invalid selectionMode");
			}

		}

		//-------------------------------------------------------------------------
		public void setValueIsAdjusting(boolean isAdjusting) {
			if (isAdjusting != this.isAdjusting) {
			    this.isAdjusting = isAdjusting;
			}
			fireValueChanged();
		}
		
		//=========================================================================
		// assistant methods
		
		//-------------------------------------------------------------------------
		/** Notifies all registered listeners that the last selection is changed. */
		protected void fireValueChanged() {
		    	ListSelectionEvent e = null;
		    	for (ListSelectionListener l : listeners) {
		    		if (e == null) {
		    		    e = new ListSelectionEvent(this,getLastSelectionIndex(),getLastSelectionIndex(), isAdjusting);
		    		}
		    		l.valueChanged(e);
		    	}
		}
	}

	//=========================================================================
	//	GUI (View) methods

	private void initialize() {
		this.setLayout(new BorderLayout());
		this.add(getJSplitPane(), BorderLayout.CENTER);
		this.add(getJFilterPanel(), BorderLayout.NORTH);
	}

	private JScrollPane getJViewsListScrollPane() {
		if (jViewsListScrollPane == null) {
			jViewsListScrollPane = new JScrollPane();
			jViewsListScrollPane.setViewportView(getJViewsList());
			jViewsListScrollPane.setBorder(BorderFactory.createTitledBorder(null, "Available views", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), GUIUtils.getLabelFg()));
			jViewsListScrollPane.setOpaque(true);
		}
		return jViewsListScrollPane;
	}

	JList getJViewsList() {
		if (jViewsList == null) {
			jViewsList = new JList();
			jViewsList.setListData(views);
			jViewsList.setSelectionModel(multipleSelection ? new NonSortedListSelectionModel() : new DefaultListSelectionModel());
			jViewsList.setSelectionMode(multipleSelection ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
			jViewsList.addListSelectionListener(this);
		}
		return jViewsList;
	}

	private JSplitPane getJSplitPane() {
		if (jSplitPane == null) {
			jSplitPane = new JSplitPane();
			jSplitPane.setLeftComponent(getJViewsListScrollPane());
			jSplitPane.setRightComponent(getJInfoPanel());
			jSplitPane.setResizeWeight(0.33);
		}
		return jSplitPane;
	}

	private JPanel getJInfoPanel() {
		if (jInfoPanel == null) {
//			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
//			gridBagConstraints1.gridx = 0;
//			gridBagConstraints1.fill = GridBagConstraints.BOTH;
//			gridBagConstraints1.weightx = 1.0D;
//			gridBagConstraints1.weighty = 1.0D;
//			gridBagConstraints1.weighty = 4.0D;
//			gridBagConstraints1.gridy = 0;
//			GridBagConstraints gridBagConstraints = new GridBagConstraints();
//			gridBagConstraints.gridx = 0;
//			gridBagConstraints.fill = GridBagConstraints.BOTH;
//			gridBagConstraints.weighty = 1.0D;
//			gridBagConstraints.gridy = 1;
			jInfoPanel = new JPanel();
			jInfoPanel.setLayout(new BorderLayout());
			jInfoPanel.add(getJTablePanel(), BorderLayout.CENTER);
			jInfoPanel.add(getJBottomInfoPanel(), BorderLayout.SOUTH);
		}
		return jInfoPanel;
	}

	private JTableScrollPane getJTableScrollPane() {
		if (jTableScrollPane == null) {
			jTableScrollPane = new JTableScrollPane();
			jTableScrollPane.setName("jTableScrollPane");
		}
		return jTableScrollPane;
	}

	private JScrollPane getJDescriptionScrollPane() {
		if (jDescriptionScrollPane == null) {
			jDescriptionScrollPane = new JScrollPane();
			jDescriptionScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			jDescriptionScrollPane.setViewportView(getJDescriptionTextArea());
			jDescriptionScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			jDescriptionScrollPane.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

			JPanel h = new JPanel();
			h.setLayout(new BorderLayout());
			jDescriptionLabel = new JLabel();
			jDescriptionLabel.setText("Description:");
			jDescriptionLabel.setBorder(BorderFactory.createEmptyBorder(GUIUtils.GUI_unit(0.5), 0, 0, 0));
			h.add(jDescriptionLabel, BorderLayout.WEST);
			h.add(getJSaveDescButton(), BorderLayout.EAST);
			jDescriptionScrollPane.setColumnHeaderView(h);

			Dimension d = new Dimension(jDescriptionScrollPane.getMinimumSize().width, GUIUtils.GUI_unit(4, getJDescriptionTextArea())); 
			jDescriptionScrollPane.setMinimumSize(d);
			jDescriptionScrollPane.setPreferredSize(d);
			jDescriptionScrollPane.setVisible(false);
		}
		return jDescriptionScrollPane;
	}

	private JTextArea getJDescriptionTextArea() {
		if (jDescriptionTextArea == null) {
			jDescriptionTextArea = new JTextArea();
			jDescriptionTextArea.setLineWrap(true);
			jDescriptionTextArea.setWrapStyleWord(true);

			jDescriptionTextArea.getDocument().addDocumentListener(
				java.beans.EventHandler.create(javax.swing.event.DocumentListener.class, this, "displayOrHideSaveDescButton")
			);
		}
		return jDescriptionTextArea;
	}

	private JButton getJSaveDescButton() {
		if (jSaveDescButton == null) {
			jSaveDescButton = new JButton();
			jSaveDescButton.setText("Save");
			jSaveDescButton.addActionListener(this);
			jSaveDescButton.setPreferredSize(new Dimension(
					jSaveDescButton.getPreferredSize().width, jDescriptionLabel.getPreferredSize().height));
		}
		return jSaveDescButton;
	}

	private JPanel getJBottomInfoPanel() {
		if (jBottomInfoPanel == null) {
			int b = GUIUtils.GUI_unit(0.25);
			jBottomInfoPanel = new JPanel();
			jBottomInfoPanel.setBorder(BorderFactory.createEmptyBorder(0, b, b, b));
			jBottomInfoPanel.setLayout(new BoxLayout(getJBottomInfoPanel(), BoxLayout.Y_AXIS));
			jBottomInfoPanel.add(getJBottomInfoIntPanel(), null);
			jBottomInfoPanel.add(javax.swing.Box.createVerticalStrut(b));
			jBottomInfoPanel.add(javax.swing.Box.createVerticalStrut(b));
			jBottomInfoPanel.add(getJDescriptionScrollPane(), null);
		}
		return jBottomInfoPanel;
	}

	private JPanel getJTablePanel() {
		if (jTablePanel == null) {
			jTablePanel = new JPanel();
			jCard = new CardLayout();
			jTablePanel.setLayout(jCard);
			jTablePanel.add(getJTableScrollPane(), getJTableScrollPane().getName());
			jTablePanel.add(getJDataScrollPane(), getJDataScrollPane().getName());
		}
		return jTablePanel;
	}

	private JScrollPane getJDataScrollPane() {
		if (jDataScrollPane == null) {
			jDataScrollPane = new JScrollPane();
			jDataScrollPane.setName("jDataScrollPane");
			jDataScrollPane.setViewportView(getJDataTable());
		}
		return jDataScrollPane;
	}

	@SuppressWarnings("serial")
	private JTable getJDataTable() {
		if (jDataTable == null) {
			jDataTable = new JTable() {
				@Override public void addColumn(TableColumn aColumn) {
					if (aColumn.getModelIndex() == 0)
						aColumn.setMaxWidth(GUIUtils.GUI_unit(3.25));
					super.addColumn(aColumn);
				}
			};
			jDataTable.setModel(new RowsData());
			jDataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			getDataTableModel().setReferenceTable(jDataTable, getJDataScrollPane());
			// Az alabbi hivas nelkul a Substance "lefagyasztotta" a programot 
			// amint a tablat billentyuzetrol probalta bongeszni a user.
			MEMEApp.getLF().treatAsBigTable(jDataTable, true);
			jDataTable.setDefaultRenderer(Double.class,new GUIUtils.USDoubleRenderer());
		}
		return jDataTable;
	}

	private JPanel getJBottomInfoIntPanel() {
		if (jBottomInfoIntPanel == null) {
			jBottomInfoLabel = new JLabel();
			jBottomInfoIntPanel = new JPanel();
			jBottomInfoIntPanel.setLayout(new BoxLayout(getJBottomInfoIntPanel(), BoxLayout.X_AXIS));
			jBottomInfoIntPanel.add(jBottomInfoLabel, null);
			jBottomInfoIntPanel.add(javax.swing.Box.createHorizontalStrut(GUIUtils.GUI_unit(2)));
			jBottomInfoIntPanel.add(getJToggleTableButton(), null);
			jBottomInfoIntPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
		}
		return jBottomInfoIntPanel;
	}

	private JButton getJToggleTableButton() {
		if (jToggleTableButton == null) {
			jToggleTableButton = new JButton("Show rows");
			jToggleTableButton.addActionListener(this);
			jToggleTableButton.setVisible(false);
		}
		return jToggleTableButton;
	}

	private JPanel getJFilterPanel() {
		if (jFilterPanel == null) {
			jLabel = new JLabel();
			jLabel.setText("  Filter Expression:  ");
			jFilterPanel = new JPanel();
			jFilterPanel.setLayout(new BoxLayout(getJFilterPanel(), BoxLayout.X_AXIS));
			jFilterPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			jFilterPanel.add(jLabel, null);
			jFilterPanel.add(getJFilterTextField(), null);
			jFilterPanel.add(Box.createRigidArea(new Dimension(10,0)));
			jFilterPanel.add(getJFilterButton(), null);
		}
		return jFilterPanel;
	}

	private JTextField getJFilterTextField() {
		if (jFilterTextField == null) {
			jFilterTextField = new JTextField();
			jFilterTextField.addActionListener(this);
		}
		return jFilterTextField;
	}

	private JButton getJFilterButton() {
		if (jFilterButton == null) {
			jFilterButton = new JButton();
			jFilterButton.setText("Filter");
			jFilterButton.addActionListener(this);
		}
		return jFilterButton;
	}
}  //  @jve:decl-index=0:visual-constraint="10,10"
