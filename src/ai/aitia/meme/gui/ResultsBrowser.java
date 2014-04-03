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

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.ConnChangedEvent;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ParameterComb;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.AbstractResultsDb.BatchInfo;
import ai.aitia.meme.database.AbstractResultsDb.IResultsRowsIterator;
import ai.aitia.meme.database.AbstractResultsDb.ResultsDbChangeEvent;
import ai.aitia.meme.database.Result.Row;
import ai.aitia.meme.gui.ResultsTree.Node;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.GUIUtils;
import static ai.aitia.meme.utils.GUIUtils.getOverhead;
import static ai.aitia.meme.utils.GUIUtils.GUI_unit;


//-----------------------------------------------------------------------------
/** This class represents the main component of the Results panel. It contains 
 *  the results tree, the informations display and the data display.
 *  Don't forget to call the onClose() method if you use this class */ 
@SuppressWarnings("serial")
public class ResultsBrowser extends JPanel implements java.awt.event.ActionListener,
														 java.beans.PropertyChangeListener,
														 javax.swing.event.TreeSelectionListener,
														 ResultsTree.BatchExpansionListener,
														 ai.aitia.meme.database.IConnChangedListener,
														 ai.aitia.meme.database.AbstractResultsDb.IResultsDbChangeListener
{
	private static int RIGHT_PANEL_MIN_WIDTH()	{ return GUI_unit(16); }
	private static int RIGHT_PANEL_MIN_HEIGHT()	{ return GUI_unit(7); }
	private static int LEFT_PANEL_MIN_WIDTH()		{ return GUI_unit(10); }

	/** The original description of the current node. */
	private String origDesc = null;
	/** The current node (the last clicked) of the results tree. */ 
	private volatile TreePath current = null;	//!< the current node (last clicked) [EDT: rw, Model thread: read-only]
	
	/** Listener object that listens the changes of the tree. */
	private TreeModelListener tml = null;		//!< removed in onClose()
	
	/** Flag that determines whether the right panel contains the rows of the selected
	 *  results or the informations about them.
	 */
	private boolean rowTable = false;

	// The followings are maintained for descendants, like Page_InputTables.???. SQL column names are useless.
	protected Columns selectedInpColumns = new Columns(); 
	protected Columns selectedOutColumns = new Columns();
	private JButton showInDialogOKButton = null;

	private JTextArea DescriptionTextArea = null;
	private JLabel jLabel = null;
	private JPanel jInfoPanel = null;
	private JSplitPane jSplitPane = null;
	private JPanel jRightPanel = null;
	private JScrollPane jDescriptionScrollPane = null;
	private JScrollPane jInfoScrollPane = null;
	private JTextPane jInfoTextPane = null;
	private JButton jSaveDescButton = null;
	private JScrollPane jTreeScrollPane = null;
	private JTree jTree = null;
	private JPanel jSouthPanel = null;
	private JButton jShowRowsButton = null;
	private JPanel jRowsPanel = null;
	private JScrollPane jTableScrollPane = null;
	private JTable jTable = null;
	private JLabel rowLabel = new JLabel();
	private JLabel rowLabel2 = new JLabel();
	private JButton jShowInfoButton = null;
	
	private int nRows = -1;

	//=========================================================================
	//	Public interface
	
	/**
	 * This is the default constructor
	 */
	public ResultsBrowser() {
		super();
		initialize();
		MEMEApp.getDatabase().connChanged.addWeakListener(this);
		MEMEApp.getResultsDb().resultsDbChanged.addWeakListener(this);
		
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				if (rowTable) 
					GUIUtils.updateColumnWidths(getJTable(),getJTableScrollPane());
			}
			
		});
	}
	
	//-------------------------------------------------------------------------
	/**
	 * The container of 'this' JPanel is responsible to call this method
	 * when the container is disposed, to remove various listeners from 
	 * external objects, which cannot be removed automatically.
	 * Note: this method clears the selection, thus related methods 
	 * (getSelectedModels(), getSelection() etc.) should not be used
	 * after this method.         
	 */
	public void onClose() {
		MEMEApp.getDatabase().connChanged.removeListener(this);
		MEMEApp.getResultsDb().resultsDbChanged.removeListener(this);
		if (jTree != null) {
			ResultsTree rt = getResultsTreeModel();
			if (rt != null)
				rt.delayedBatchExpansion.removeListener(this);

			// Remove our TreeModelHandler
			if (tml != null) {
				jTree.getModel().removeTreeModelListener(tml);
				tml = null;
			}
			jTree.removeTreeSelectionListener(this);

			// Remove JTree's internal TreeModelHandler
			jTree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
			jTree = null;
			
			if (jInfoPanel != null) {
				jInfoPanel.removeAll();
				jInfoPanel = null;
			}
			if (jInfoScrollPane != null) {
				jInfoScrollPane.removeAll();
				jInfoScrollPane = null;
			}
			if (jSplitPane != null) {
				jSplitPane.removePropertyChangeListener(this);
				jSplitPane = null;
			}
		}
	}

	//-------------------------------------------------------------------------
	/** Calls ResultsTree.getSelectedModels() */
	public Model[] getSelectedModels() {
		return ResultsTree.getSelectedModels(getJTree().getSelectionModel());
	}

	//-------------------------------------------------------------------------
	/** Calls ResultsTree.getModel(long) */
	public Model getModel(long model_id) {
		return getResultsTreeModel().getModel(model_id);
	}

	//-------------------------------------------------------------------------
	/** Calls ResultsTree.getSelection() */
	public Long[][] getSelection() {
		return ResultsTree.getSelection(getJTree().getSelectionModel());
	}
	
	//-------------------------------------------------------------------------
	public boolean isSelectionEmpty() {
		return (getSelectedModels().length == 0);
	}

	//-------------------------------------------------------------------------
	/** Calls ResultsTree.setSelection(), and then expands nodes. */
	// EDT only
	public void setSelection(Long[][] selection) {
		ArrayList<TreePath> toExpand = new ArrayList<TreePath>();

		getResultsTreeModel().setSelection(getJTree().getSelectionModel(), selection, toExpand);

		for (TreePath p : toExpand)
			jTree.expandPath(p);
	}

	//-------------------------------------------------------------------------
	/** 
	 * Returns the elements of the current selection as String arrays, for example:
	 *   [modelname]
	 *   [modelname, version]
	 *   [modelname, version, batchNr]
	 * If all children are selected of a parent, then children are not returned,
	 * only the parent. E.g. [modelname] means that all versions of that model
	 * are selected. The batchNr never includes the description.  
	 */
	public ArrayList<String[]> getSelectionAsStrings()
	{
		ArrayList<String[]> ans = new ArrayList<String[]>();
		for (TreePath p : getJTree().getSelectionPaths()) {
			String[] tmp = new String[p.getPathCount() - 1];
			int i = -2;
			for (Object node : p.getPath()) {
				if (++i >= 0) {
					BatchInfo bi = ResultsTree.getBatch(node);
					tmp[i] = (bi == null) ? node.toString() : String.valueOf(bi.batch);
				}
			}
			ans.add(tmp);
		}
		java.util.Collections.sort(ans, new Utils.LexComp<String>());
		for (int i = ans.size() - 1; i > 0; --i) {
			if (Utils.lex(ans.get(i-1), ans.get(i), false) == -2) {
				ans.remove(i);
				if (i < ans.size()) i += 1;
			}
		}
		return ans;
	}

	
	//-------------------------------------------------------------------------
	/** Sets the behaviour of this ResultsBrowser: whether or not batches are shown in the tree. */ 
	public void setDisplayBatches(boolean b) {
		if (MEMEApp.getMainWindow() == null)		// GUI-builder case
			return;
		javax.swing.tree.TreeModel m = MEMEApp.getMainWindow().resultsTreeModel; 
		if (!b) {
			// When batches are hidden, m.resultsTreeModel is wrapped into 
			// a HideBatch_TreeModelWrapper object because isLeaf(node)  
			// must return true at version nodes.
			// Note: it wouldn't be sufficient to use TreeWillExpandListener 
			// instead of wrapping for disabling the expansion of version nodes, 
			// because it wouldn't make them leaf nodes.
			m = new HideBatch_TreeModelWrapper((ResultsTree)m);
		}
		getJTree().setModel(m);
	}

	//-------------------------------------------------------------------------
	/** Returns whether or not batches are shown in the tree. */
	public boolean isDisplayBatches() {
		return !(getJTree().getModel() instanceof HideBatch_TreeModelWrapper);
	}

	//-------------------------------------------------------------------------
	/** 
	 * Creates a modal JDialog and displays 'this' in that. Note that 'this'
	 * remains intact when the dialog is closed. 
	 * @return false if the dialog was cancelled. Use getSelection() to retrieve 
	 *          the user's selection.
	 */  
	public boolean showInDialog(java.awt.Window parent, String title) {
		final javax.swing.JDialog dialog[] = { null };
		final boolean ans[] = { false }; 

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(this);
		
			showInDialogOKButton = new JButton("Select");
			showInDialogOKButton.setEnabled(!isSelectionEmpty());
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

		if (title == null) title = "Select Models and Results";
		dialog[0] = GUIUtils.createDialog(parent, true, title, panel);
		GUIUtils.disposeOnClose(dialog[0]);	// only the dialog will be disposed, not this ResultsBrowser!
		if (parent != null)
			dialog[0].setLocationRelativeTo(parent);
		dialog[0].setVisible(true);
		showInDialogOKButton = null;

		return ans[0]; 
	}

	//=========================================================================
	//	Controller methods

	//-------------------------------------------------------------------------
	/** Sets the dimensions of this component. This method is public as an implementation side effect.
	 *  Do not call or override. */
	// It is public because it is used with reflection, too
	public void controlSizes() {
		int width = Math.max(getJInfoPanel().getWidth(), RIGHT_PANEL_MIN_WIDTH());
		int oh = GUIUtils.add(getOverhead(getJInfoPanel()), getOverhead(getJInfoScrollPane())).width;

		GUIUtils.setWrapLength(getJInfoTextPane(), width - oh);
		Dimension d = getJInfoTextPane().getPreferredSize();
		d.width = Math.max(d.width, RIGHT_PANEL_MIN_WIDTH()) + oh;
		getJInfoScrollPane().setMaximumSize(d);
		getJInfoPanel().setPreferredSize(null);
		getJInfoPanel().validate();
		if (getJInfoPanel().getWidth() < RIGHT_PANEL_MIN_WIDTH()) {
			d = getJInfoPanel().getMinimumSize();
			GUIUtils.enlarge(d, getJInfoPanel().getPreferredSize());
			getJInfoPanel().setPreferredSize(d);
			GUIUtils.repack(getJInfoPanel());
		}
	}

	//-------------------------------------------------------------------------
	/** Refreshes the informations on the information panel at right side.
	 *  Precondition: 'current' is up-to-date */ 
	private void refreshInfo() {
		CardLayout l = (CardLayout)getJRightPanel().getLayout();
		l.show(getJRightPanel(),"INFO");
		MEMEApp.LONG_OPERATION.setAbortable(false);
		MEMEApp.LONG_OPERATION.begin("Collecting information about selection...", new LongRunnable() {
			ArrayList<Columns> icols = new ArrayList<Columns>();
			ArrayList<Columns> ocols = new ArrayList<Columns>();
			java.util.TreeSet<Long> models = new java.util.TreeSet<Long>();
			String desc = null;
			String preamble = "";
			TreePath[] selection = getJTree().getSelectionPaths();	// must be in GUI thread
			// Potentially long part:
			@Override public void run() {
				final int n = (selection == null) ? 0 : selection.length;
				for (int i = n - 1; i >= 0; --i) {
					Model m = ResultsTree.getModel(selection[i].getLastPathComponent());
					if (m != null && models.add(m.getModel_id())) {
						Columns[] io = MEMEApp.getResultsDb().getModelColumns(m.getModel_id());
						if (io != null && io.length >= 2) {
							icols.add(io[0]);
							ocols.add(io[1]);
						}
					}
					if (m != null && selection[i].equals(current)) {
						BatchInfo b = ResultsTree.getBatch(current.getLastPathComponent());
						if (b == null) {
							desc = m.getDescription();
							preamble = "";
						} else {
							desc = b.description;
							preamble = String.format("<tr>" +
													"<td nowrap><b>Batch#:   </b></td><td width=\"40%%\">%d</td>" +
													"<td nowrap><b># of runs:</b></td><td width=\"40%%\">%d</td>",
													b.batch, b.nrOfRuns
												);
						}
						if (desc == null) desc = "";
						preamble = String.format("<table width=\"100%%\">%s<tr>" +
													"<td nowrap><b>Model:  </b></td><td width=\"40%%\">%s</td>" +
													"<td nowrap><b>Version:</b></td><td width=\"40%%\">%s</td>" +
													"</table>", preamble,
													Utils.htmlQuote(m.getName()),
													Utils.htmlQuote(m.getVersion())
												);
					}
				}
				if (models.size() > 1) {
					desc = null;
					preamble = ""; 
				}
				
				Long[][] selection = getSelection();
				if (selection.length > 0) {
					final long model_id = selection[0][0];
					final List<Long> batch_ids = new ArrayList<Long>(selection.length);
					for (int i = 0;i < selection.length;++i) {
						if (selection[i].length == 0 || selection[i][0] != model_id) 
							throw new IllegalStateException();
						if (selection[i].length == 1) {
							batch_ids.clear();
							break;
						} else
							batch_ids.add(selection[i][1]);
					}
					
					int nrRows = 0;
					if (batch_ids.size() == 0)
						nrRows = MEMEApp.getResultsDb().getNumberOfRows(model_id,null);
					else {
						for (int i = 0;i < batch_ids.size();++i) 
							nrRows += MEMEApp.getResultsDb().getNumberOfRows(model_id,new Long[] { batch_ids.get(i) });
					}
					nRows = nrRows;
				} else 
					nRows = -1;
			}
			// GUI-part:
			@Override public void finished() {
				try {
					String text = preamble;
					if (!icols.isEmpty()) {
						// Merge column types (losing SQL column names)
						selectedInpColumns = new Columns();
						selectedOutColumns = new Columns();
						for (Columns c : icols) selectedInpColumns.merge(c, null);	
						for (Columns c : ocols) selectedOutColumns.merge(c, null);	
						text = text + "<dl compact><dt><b>Input parameters:</b></dt><dd>" 
									+ Utils.join(selectedInpColumns.getSorted(), ", ")
									+ "</dd><dt><b>Output factors:</b></dt><dd>" 
									+ Utils.join(selectedOutColumns.getSorted(), ", ")
									+ "</dd></dl>";
					} else if (!getResultsTreeModel().isEmptyDb()) {	// nothing is selected, although could be
						text = "Click in the tree on the left to expand " +
							   " and select results stored in the database." +
							   " Use <kbd>CTRL+Click</kbd> to select more.";
					} else {
						text = Utils.htmlPage("The Results database is empty.<br>" +
			       		  			   "Use <i>File &gt; Import</i> to add data.");
					}
					if (text != null) {
						GUIUtils.setTextPane(getJInfoTextPane(), Utils.htmlPage(text));
					}
					displayDesc(desc);
					controlSizes();
					Utils.invokeLater(getJInfoTextPane(), "scrollRectToVisible", new java.awt.Rectangle(0,0,1,1));
					//getJInfoTextPane().scrollRectToVisible(new java.awt.Rectangle(0,0,1,1));
					refreshFinished();
					
					String msgs[] = { "Contains no rows", "Contains 1 row", "Contains %d rows", " no columns  ", " 1 column  ", " %d columns  " };
					String rowText = String.format(msgs[(nRows < 1) ? 0 : (nRows == 1 ? 1 : 2)], nRows);
					final int nCols = selectedInpColumns.size() + selectedOutColumns.size();
					rowText += ", " + String.format(msgs[nCols < 1 ? 3 : (nCols == 1 ? 4 : 5)],nCols); 

					setNRows(rowText);
				} finally {
					MEMEApp.LONG_OPERATION.setAbortable(true);
				}
			}
		});
	}

	//-------------------------------------------------------------------------
	/** This method is called whenever the information panel is updated. */ 
	protected void refreshFinished() {
		if (this == MEMEApp.getMainWindow().getResultsBrowser())
			MEMEApp.getMainWindow().whenNrOfSelectedModels.fireLater();
		if (showInDialogOKButton != null)
			showInDialogOKButton.setEnabled(!isSelectionEmpty());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void setNRows(String text) {
		String tmp = nRows >= 0 ? text : "";
		rowLabel.setText(tmp);
		rowLabel2.setText(tmp);
	}

	//-------------------------------------------------------------------------
	/** Displays in the description field the new description 's'. */
	private void displayDesc(String s) {
		origDesc = s;
		if (s == null) {
			getDescriptionTextArea().setText("");
//			getJDescriptionScrollPane().setVisible(false);
			getJSouthPanel().setVisible(false);
		} else {
			getDescriptionTextArea().setText(s);
//			getJDescriptionScrollPane().setVisible(true);
			getJSouthPanel().setVisible(true);
		}
		displayOrHideSaveDescButton();
		getJInfoPanel().validate();
	}

	//-------------------------------------------------------------------------
	/** It displays a 'Save' button above the description fieid if the description 
	 *  is changed.
	 *  This method is public as an implementation side effect. Do not call or override. */
	// It is public because it is used with reflection, too
	public void displayOrHideSaveDescButton() {
		String desc = getDescriptionTextArea().getText().trim();
		boolean dirty = !desc.equals(origDesc == null ? "" : origDesc); 
		getSaveDescButton().setVisible(dirty);
	}

	//-------------------------------------------------------------------------
	/** Saves the new description to the database. */ 
	private void saveDesc() {
		final String s = getDescriptionTextArea().getText().trim();

		assert current != null;
		if (current == null) return;
		switch (ResultsTree.getPathType(current.getLastPathComponent())) {
			default :
			case ROOT : 
			case MODEL :
				assert false;
				break;
			case VERSION :
			case BATCH :
				final BatchInfo b = ResultsTree.getBatch(current.getLastPathComponent());
				final Model m = ResultsTree.getModel(current.getLastPathComponent());
				if (b != null && m != null) {
					MEMEApp.LONG_OPERATION.begin("Saving batch description...", new LongRunnable() {
						@Override
						public void run() {
							MEMEApp.getResultsDbMinimal().storeBatchDescription(m.getModel_id(), b.batch, s);
						}
						@Override
						public void finished() {
							if (getReq().getError() == null)
								displayDesc(s);		// hides the Save button
						}
					});
				} else if (m != null) {
					MEMEApp.LONG_OPERATION.begin("Saving model description...", new LongRunnable() {
						@Override
						public void run() {
							m.setDescription(s, true);
						}
						@Override
						public void finished() {
							if (getReq().getError() == null)
								displayDesc(s);		// hides the Save button
						}
					});
				}
		}
	}
	
	//-------------------------------------------------------------------------
	/** Returns the model of the Results tree. */
	private ResultsTree getResultsTreeModel() {
		javax.swing.tree.TreeModel m = getJTree().getModel();
		if (m instanceof HideBatch_TreeModelWrapper)
			m = ((HideBatch_TreeModelWrapper)m).rt;
		return (m instanceof ResultsTree) ? (ResultsTree)m : null;
	}

	//-------------------------------------------------------------------------
	/** Wrapper class for the tree model ResultsTree. It hides the batches in the 
	 *  tree. 
	 */ 
	static class HideBatch_TreeModelWrapper implements javax.swing.tree.TreeModel {
		final ResultsTree rt;

		public 			HideBatch_TreeModelWrapper(ResultsTree rt)			{ this.rt = rt; }

		public void	addTreeModelListener(TreeModelListener l)			{ rt.addTreeModelListener(l); }
		public void	removeTreeModelListener(TreeModelListener l)		{ rt.removeTreeModelListener(l); }
		public void	valueForPathChanged(TreePath path, Object newValue)	{ rt.valueForPathChanged(path, newValue); }
		public Object	getRoot()											{ return rt.getRoot(); }
		public Object	getChild(Object parent, int index)					{ return rt.getChild(parent, index); }
		public int		getIndexOfChild(Object parent, Object child)		{ return rt.getIndexOfChild(parent, child); }

		public boolean isLeaf(Object node) {
			return (ResultsTree.getPathType(node) == ResultsTree.PathType.VERSION);
		}

		public int getChildCount(Object parent) {
			return (isLeaf(parent)) ? 0 : rt.getChildCount(parent);
		}
	}

	//-------------------------------------------------------------------------
	// TODO: vond ossze ezt a fgv.t ResultsTree.findModelNode()-al: ezt kellene attenni oda
	/** Returns the node from the Results tree identified by 'model_id' and 'batch'.
	 *  It returns null if doesn't find the node.
	 */ 
	private DefaultMutableTreeNode findNode(long model_id, Integer batch) {
		DefaultMutableTreeNode ans = null;
		javax.swing.tree.TreeModel t = getJTree().getModel();
		Object root = t.getRoot();
		MODEL:
		for (int i = t.getChildCount(root) - 1; i >= 0; --i) {
			Object modelname = t.getChild(root, i);
			for (int j = t.getChildCount(modelname) - 1; j >= 0; --j) {
				Model m = ResultsTree.getModel(t.getChild(modelname, j));
				if (m != null && m.getModel_id() == model_id) {
					ans = (DefaultMutableTreeNode)t.getChild(modelname, j);
					break MODEL;
				}
			}
		}
		if (batch == null || ans == null)
			return ans;
		for (int i = ans.getChildCount() - 1; i >= 0; --i) {
			Object x = ans.getChildAt(i);
			BatchInfo b = ResultsTree.getBatch(x);
			if (b != null && b.batch == batch.intValue()) {
				ans = (DefaultMutableTreeNode)x;
				return ans;
			}
		}
		return null;
	}
	
	//=========================================================================
	//	Listener methods

	//-------------------------------------------------------------------------
	// EDT or Model thread
	public void onConnChange(ConnChangedEvent event) {
		String text = "";
		if (event.getConnection() == null) {
			// no database connection
			text = Utils.htmlPage("There is no database connection.<br>" +
					"Please modify the connection settings in the" +
					" <i>File &gt; Database settings</i> window.");
		}
		GUIUtils.setTextPane(getJInfoTextPane(), Utils.htmlPage(text));

		Utils.invokeLater(this, "valueChanged", (TreeSelectionEvent)null );
	}

	//-------------------------------------------------------------------------
	// Called when a (new) model/batch/result is created/removed or a description
	// is modified.
	// TODO: ennek ResultsTree-ben van a helye. Ide is kell egy kicsi: refreshInfo()-t kell hivni (mindig!)
	// [EDT or Model thread]
	public void onResultsDbChange(ResultsDbChangeEvent e) {
		if (!SwingUtilities.isEventDispatchThread()) {
			Utils.invokeLater(this, "onResultsDbChange", e);
			return;
		}
		// EDT:
		if (e.getDescription() == null) {		// an IResult was added/modified, or whole batches/models were removed, or model renamed
			Model old = null;
			final Model[] sm = getSelectedModels();
			if (sm.length > 0)
				old = sm[0];
			getResultsTreeModel().refresh();
			if (e.isRenamed()) 
				setSelection(new Long[][] {{ old.getModel_id() }});
			return;
		}
		if (e.getModel() != null) {				// description modification
			DefaultMutableTreeNode node = findNode(e.getModel().getModel_id(), e.getBatchNr());
			if (node != null) {
				if (e.getBatchNr() != null)
					((ResultsTree.Node.Batch)node.getUserObject()).updateDesc(e.getDescription());
				else {
					Model m = (Model)node.getUserObject();
					if (m != e.getModel()) {
						m.setDescription(e.getDescription(), false);
						if (current != null && current.getLastPathComponent() == node)
							refreshInfo();
					}
				}
			}
			return;
		}
	}

	//-------------------------------------------------------------------------
	// [EDT only]
	/**
	 * Called when placeholder Batch node under 'modelNode' has been filled with actual data.
	 * Adds the new Batch nodes to the current selection if 'modelNode' is selected. 
	 */
	public void onDelayedExpansion(Node modelNode) {
		javax.swing.tree.TreeSelectionModel ts = getJTree().getSelectionModel(); 
		TreePath path = new TreePath(modelNode.getPath());
		if (ts.isPathSelected(path)) {
			// The focus automatically moves to the last added node.
			// To prevent the focus from moving, I add the current node at the end.
			int addCurr = (current != null && ts.isPathSelected(current)) ? 1 : 0;
			TreePath childPaths[] = new TreePath[modelNode.getChildCount() + addCurr];
			int i = 0;
			for (java.util.Enumeration e = modelNode.children(); e.hasMoreElements(); )
				childPaths[i++] = path.pathByAddingChild(e.nextElement());
			if (addCurr != 0)
				childPaths[i] = current;
			ts.addSelectionPaths(childPaths);
		}
	}

	//-------------------------------------------------------------------------
	// [EDT only]
	/**
	 * Called by the tree when clicking on a node, and also called from
	 * {@link #onConnChange(ConnChangedEvent)} after connecting to a database 
	 * to refresh the contents of the info panel.
	 * In this latter case e == null and we expect that ResultsTree.refresh() 
	 * was triggered <i>before</i> this method.
	 */
	public void valueChanged(TreeSelectionEvent e) {
		if (!in_valueChangedMethod) try {
			in_valueChangedMethod = true;

			current = (e == null) ? null : e.getNewLeadSelectionPath();
			if (current == null || !isAddedPath(current, e)) {
				Object x = getJTree().getLastSelectedPathComponent();
				if (x != null && (x instanceof DefaultMutableTreeNode))
					current = new TreePath(((DefaultMutableTreeNode)x).getPath());
			}
			// The following call may cause recursive call to this method
			// (this is the reason for in_valueChangedMethod)
			//
			ResultsTree.adjustSelection(getJTree().getSelectionModel(), current);
			
			refreshInfo();
		} finally {
			in_valueChangedMethod = false;
		}
	}
	boolean in_valueChangedMethod = false;

	//-------------------------------------------------------------------------
    /**
     * Returns true if the path identified by 'p' was added to the
     * selection. A return value of false means the path was in the
     * selection but is no longer in the selection. This will raise if
     * path is not one of the paths identified by event 'e'.
     */
	private static boolean isAddedPath(TreePath p, TreeSelectionEvent e) {
		try { return e.isAddedPath(p); } 
		catch (Exception ex) { return false; }
	}
	
	//-------------------------------------------------------------------------
	// It is public because it is used with reflection
	/**
	 * This method is public as an implementation side effect. Do not call or override.
	 * It is called by ResultsTree when nodes are inserted/removed/modified.
	 */
	public void treeModelEvent() {
		if (getJTree().getSelectionCount() == 0 && MEMEApp.getDatabase().getConnection() != null) {
			if (getResultsTreeModel().isEmptyDb()) {		// the database is empty
				String text = Utils.htmlPage("The Results database is empty.<br>" +
				       		  "Use <i>File &gt; Import</i> to add data.");
				GUIUtils.setTextPane(getJInfoTextPane(), Utils.htmlPage(text));
			} else {
				refreshInfo();
			}
		}
	}

	//-------------------------------------------------------------------------
	public void actionPerformed(java.awt.event.ActionEvent e) {
		if (e.getSource() == getSaveDescButton()) {
			saveDesc();
		} else {
			final CardLayout l = (CardLayout)getJRightPanel().getLayout();
			if (e.getSource() == getJShowRowsButton()) {
				Long[][] selection = getSelection();
				final long model_id = selection[0][0];
				final List<Long> batch_ids = new ArrayList<Long>(selection.length);
				for (int i=0;i<selection.length;++i) {
					if (selection[i].length == 0 || selection[i][0] != model_id) 
						throw new IllegalStateException();
					if (selection[i].length == 1) {
						batch_ids.clear();
						break;
					} else batch_ids.add(selection[i][1]);
				}
//				MEMEApp.LONG_OPERATION.begin("Calculating number of rows...",new LongRunnable(){
//					int nrRows = 0;
//					public void trun() throws Exception {
//						MEMEApp.LONG_OPERATION.progress(0,batch_ids.size());
//						nrRows = 0;
//						if (batch_ids.size() == 0) nrRows = MEMEApp.getResultsDb().getNumberOfRows(model_id,null);
//						else {
//							for (int i=0;i<batch_ids.size();++i) {
//								MEMEApp.LONG_OPERATION.progress(i);
//								nrRows += MEMEApp.getResultsDb().getNumberOfRows(model_id,new Long[] { batch_ids.get(i) });
//							}
//						}
//					}
//					
//					public void finished() {
					getJTable().setModel(new TableData());
					getTableModel().setReferenceTable(getJTable(),getJTableScrollPane());
					getTableModel().openResultTable(model_id,batch_ids,nRows);
					l.show(getJRightPanel(),"ROWS");
					rowTable = true;
//					}
//				});
			} else if (e.getSource() == getJShowInfoButton()) {
				l.show(getJRightPanel(),"INFO");
				rowTable = false;
			}
		}
	}

	//-------------------------------------------------------------------------
	/**
	 * This method is public as an implementation side effect. Do not call or override.
	 * It is called when the slider of the split pane is repositioned.
	 */
	public void propertyChange(java.beans.PropertyChangeEvent evt) {
		if (JSplitPane.LAST_DIVIDER_LOCATION_PROPERTY.equals(evt.getPropertyName())) {
			int loc = getJSplitPane().getDividerLocation() - getJSplitPane().getDividerSize()/2;
			if (loc < getJTreeScrollPane().getMinimumSize().width) {
				loc = getJTreeScrollPane().getMinimumSize().width;
			}
			getJTreeScrollPane().setPreferredSize(null);
			Dimension d = getJTreeScrollPane().getPreferredSize();
			d.width = loc;
			getJTreeScrollPane().setPreferredSize(d);
		}
	}

	//=========================================================================
	//	GUI (View) methods
	
	/**
	 * This method initializes this
	 */
	private void initialize() {
		// Buttons
		BorderLayout borderLayout = new BorderLayout();
		this.setLayout(borderLayout);
		this.add(getJSplitPane(), BorderLayout.CENTER);
		
		// Trigger 'controlSizes()' when the size of jPanel2 changes - to resize the line-wrapping text components 
		getJInfoPanel().addComponentListener(
				java.beans.EventHandler.create(java.awt.event.ComponentListener.class, this, "controlSizes", null, "componentResized")
		);
	}

	private JSplitPane getJSplitPane() {
		if (jSplitPane == null) {
			jSplitPane = new JSplitPane();
			jSplitPane.setLeftComponent(getJTreeScrollPane());
			jSplitPane.setRightComponent(getJRightPanel());
			jSplitPane.addPropertyChangeListener(JSplitPane.LAST_DIVIDER_LOCATION_PROPERTY, this);
		}
		return jSplitPane;
	}

	private JScrollPane getJTreeScrollPane() {
		if (jTreeScrollPane == null) {
			jTreeScrollPane = new JScrollPane();
			jTreeScrollPane.setBorder(BorderFactory.createTitledBorder(null, "Available results", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), GUIUtils.getLabelFg()));
			jTreeScrollPane.setViewportView(getJTree());
			Dimension d = new Dimension();
			d.width = LEFT_PANEL_MIN_WIDTH();
			jTreeScrollPane.setMinimumSize(d);
			jTreeScrollPane.setOpaque(true);
		}
		return jTreeScrollPane;
	}

	JTree getJTree() {
		if (jTree == null) {
			jTree = new JTree();
			jTree.setName("tree_mainwindow_resulttree");
			// TreeModel is set here:
			setDisplayBatches(true);	// this default can be changed later.
			jTree.setExpandsSelectedPaths(false);
			jTree.getSelectionModel().setSelectionMode(
					javax.swing.tree.TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
			);
			jTree.setCellRenderer(new MyRenderer());
			jTree.addTreeSelectionListener(this);										// to trigger valueChanged()
			if (getResultsTreeModel() != null)
				getResultsTreeModel().delayedBatchExpansion.addWeakListener(this);		// to trigger onDelayedExpansion()

			// The following listener cannot be removed automatically.
			// It will be removed in onClose().
			tml = java.beans.EventHandler.create(TreeModelListener.class, this, "treeModelEvent");
			jTree.getModel().addTreeModelListener(tml);									// to trigger treeModelEvent()
		}
		return jTree;
	}

	//-------------------------------------------------------------------------
	/** This class allows using different icons in the tree
	 *  for model names, versions and batches.
	 */ 
	@SuppressWarnings("serial")
	public static class MyRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
		public static final javax.swing.ImageIcon 
			MODELNAME_ICON	= MainWindow.getIcon("model_name.png"),
			MODELVER_ICON	= MainWindow.getIcon("model_version.png"),
			BATCH_ICON		= MainWindow.getIcon("model_batch.png");

		@Override 
		public java.awt.Component getTreeCellRendererComponent(JTree tree,
																Object value,
																boolean sel,
																boolean expanded,
																boolean leaf,
																int row,
																boolean hasFocus)
		{
	        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			switch (ResultsTree.getPathType(value)) {
			 	case MODEL :	setIcon(MODELNAME_ICON);break;
			 	case VERSION :	setIcon(MODELVER_ICON);	break;
			 	case BATCH :	setIcon(BATCH_ICON);	break;
				case ROOT : 
			 	default :		setIcon(null);	 		break;
			}
			return this;
		}
	}

	private JPanel getJRightPanel() {
		if (jRightPanel == null) {
			jRightPanel = new JPanel();
			jRightPanel.setMinimumSize(new Dimension(RIGHT_PANEL_MIN_WIDTH(), RIGHT_PANEL_MIN_HEIGHT()));
			jRightPanel.setLayout(new CardLayout());
			//jRightPanel.add(getJToolBar(), null);
			jRightPanel.add(getJInfoPanel(), "INFO");
			jRightPanel.add(getJRowsPanel(), "ROWS");
		}
		return jRightPanel;
	}

	private JPanel getJInfoPanel() {
		if (jInfoPanel == null) {
			jInfoPanel = new JPanel();
			jInfoPanel.setLayout(new BorderLayout());
			jInfoPanel.setBorder(BorderFactory.createTitledBorder(null, "Information about selected items", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), GUIUtils.getLabelFg()));
			jInfoPanel.add(getJInfoScrollPane(), BorderLayout.CENTER);
			jInfoPanel.add(getJSouthPanel(), BorderLayout.SOUTH);
			jInfoPanel.setAlignmentX(0.0f);
		}
		return jInfoPanel;
	}
	
	private JPanel getJSouthPanel() {
		if (jSouthPanel == null) {
			jSouthPanel = new JPanel();
			jSouthPanel.setLayout(new BorderLayout());
			jSouthPanel.add(getJDescriptionScrollPane(), BorderLayout.CENTER);
			JPanel p = new JPanel(new BorderLayout());
			JPanel p2 = new JPanel();
			p2.add(rowLabel2);
			p2.add(getJShowRowsButton());
			p.add(p2, BorderLayout.WEST);
			jSouthPanel.add(p, BorderLayout.SOUTH);
		}
		return jSouthPanel;
	}
	
	private JButton getJShowRowsButton() {
		if (jShowRowsButton == null) {
			jShowRowsButton = new JButton("Show rows");
			jShowRowsButton.addActionListener(this);
		}
		return jShowRowsButton;
	}

	private JScrollPane getJDescriptionScrollPane() {
		if (jDescriptionScrollPane == null) {
			jDescriptionScrollPane = new JScrollPane();
			jDescriptionScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			jDescriptionScrollPane.setViewportView(getDescriptionTextArea());
			jDescriptionScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

			JPanel h = new JPanel();
			h.setLayout(new BorderLayout());
			jLabel = new JLabel();
			jLabel.setText(" Description:");
			jLabel.setBorder(BorderFactory.createEmptyBorder(GUI_unit(0.5), 0, 0, 0));
			h.add(jLabel, BorderLayout.WEST);
			h.add(getSaveDescButton(), BorderLayout.EAST);
			jDescriptionScrollPane.setColumnHeaderView(h);

			Dimension d = new Dimension(jDescriptionScrollPane.getMinimumSize().width, GUI_unit(4, getDescriptionTextArea())); 
			jDescriptionScrollPane.setMinimumSize(d);
			jDescriptionScrollPane.setPreferredSize(d);
		}
		return jDescriptionScrollPane;
	}

	private JTextArea getDescriptionTextArea() {
		if (DescriptionTextArea == null) {
			DescriptionTextArea = new JTextArea();
			DescriptionTextArea.setLineWrap(true);
			DescriptionTextArea.setWrapStyleWord(true);

			DescriptionTextArea.getDocument().addDocumentListener(
				java.beans.EventHandler.create(javax.swing.event.DocumentListener.class, this, "displayOrHideSaveDescButton")
			);
		}
		return DescriptionTextArea;
	}

	private JScrollPane getJInfoScrollPane() {
		if (jInfoScrollPane == null) {
			jInfoScrollPane = new JScrollPane();
			jInfoScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			jInfoScrollPane.setViewportView(getJInfoTextPane());
			jInfoScrollPane.setAlignmentX(0.0f);
			jInfoScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			jInfoScrollPane.setMinimumSize(new Dimension(RIGHT_PANEL_MIN_WIDTH(),
					GUIUtils.GUI_unit(1.5, getJInfoTextPane())));
		}
		return jInfoScrollPane;
	}

	private JTextPane getJInfoTextPane() {
		if (jInfoTextPane == null) {
			jInfoTextPane = new JTextPane();
			jInfoTextPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			jInfoTextPane.setEditable(false);
			jInfoTextPane.setContentType("text/html");
			jInfoTextPane.setBackground(getJInfoPanel().getBackground());
		}
		return jInfoTextPane;
	}


	private JButton getSaveDescButton() {
		if (jSaveDescButton == null) {
			jSaveDescButton = new JButton();
			jSaveDescButton.setText("Save");
			jSaveDescButton.addActionListener(this);
			jSaveDescButton.setPreferredSize(new Dimension(
					jSaveDescButton.getPreferredSize().width, jLabel.getPreferredSize().height));
		}
		return jSaveDescButton;
	}

	private JPanel getJRowsPanel() {
		if (jRowsPanel == null) {
			jRowsPanel = new JPanel(new BorderLayout());
			jRowsPanel.add(getJTableScrollPane(), BorderLayout.CENTER);
			JPanel p = new JPanel(new BorderLayout());
			p.setBorder(BorderFactory.createEmptyBorder(5,5,5,0));
			JPanel p2 = new JPanel();
			p2.add(rowLabel);
			p2.add(getJShowInfoButton());
			p.add(p2, BorderLayout.WEST);
			jRowsPanel.add(p, BorderLayout.SOUTH);
		}
		return jRowsPanel;
	}
	
	private JScrollPane getJTableScrollPane() {
		if (jTableScrollPane == null) {
			jTableScrollPane = new JScrollPane();
			jTableScrollPane.setViewportView(getJTable());
			jTableScrollPane.setName("jTableScrollPane");
		}
		return jTableScrollPane;
	}
	
	private JTable getJTable() {
		if (jTable == null) {
			jTable = new JTable() {
				@Override public void addColumn(TableColumn aColumn) {
					if (aColumn.getModelIndex() == 0)
						aColumn.setMaxWidth(GUIUtils.GUI_unit(3.25));
					super.addColumn(aColumn);
				}
			};
			jTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			jTable.setDefaultRenderer(Double.class,new GUIUtils.USDoubleRenderer());
			MEMEApp.getLF().treatAsBigTable(jTable,true);
		}
		return jTable;
	}
	
	//----------------------------------------------------------------------------
	/** Returns the table model of the table that display the rows of the selected results. */ 
	private TableData getTableModel() {
		javax.swing.table.TableModel ans = getJTable().getModel();
		return (ans instanceof TableData) ? (TableData)ans : null;
	}
	
	/** The table model of the table that display the rows of the selected results. */ 
	static class TableData extends javax.swing.table.AbstractTableModel implements Runnable {
		/** Id of the selected model. */
		volatile long 				model_id = 0;
		/** Ids of the selected batches. */
		volatile List<Long> 		batch_ids = null;
		/** The number of the displayable rows. */
		int 						nrows = 0;
		/** Informations about the input columns. */
		volatile Columns 			inputColumns = null; 
		/** Informations about the output columns. */
		volatile Columns			outputColumns = null;
		int 						cacheOffset = 0;
		/** Cache of the input parts of the rows. */
		Vector<GeneralRow> 			inputCache = new Vector<GeneralRow>();
		/** Cache of the output parts of the rows. */
		Vector<GeneralRow>			outputCache = new Vector<GeneralRow>();
		volatile boolean 			pendingFill = false;
		JTable						jTable = null;
		JScrollPane					jTableScp = null;
		
		//---------------------------------------------------------------------
		// TableModel methods
		public synchronized int getColumnCount() {					// [EDT]
			return (inputColumns == null) ? 1 + (outputColumns == null ? 0 
																	   : outputColumns.size())
										  : 1 + inputColumns.size() +  (outputColumns == null ? 0
												  										      : outputColumns.size());
		}

		@Override
		public synchronized String getColumnName(int column) {		// [EDT]
			if (--column < 0)
				return "#";
			if (inputColumns == null)
				return (outputColumns == null) ? "" : outputColumns.get(column).getName();
			else {
				if (column >= inputColumns.size())
					return (outputColumns == null) ? "" : outputColumns.get(column - inputColumns.size()).getName();
				return inputColumns.get(column).getName();
			}
		}

		
		@Override
		public synchronized Class<?> getColumnClass(int columnIndex) {// [EDT]
			if (--columnIndex < 0)
				return Integer.class; 
			ColumnType t = null;
			if (inputColumns == null) 
				t = (outputColumns == null) ? null : outputColumns.get(columnIndex).getDatatype();
			else {
				if (columnIndex >= inputColumns.size()) 
					t = (outputColumns == null ? null : outputColumns.get(columnIndex - inputColumns.size()).getDatatype());
				else
					t = inputColumns.get(columnIndex).getDatatype();
			}
			return (t == null) ? String.class : t.getJavaClass();
		}

		public int getRowCount() {										// [EDT]
			return nrows;
		}

		public Object getValueAt(int rowIndex, int columnIndex) {		// [EDT]
			if (--columnIndex < 0)
				return rowIndex;
			GeneralRow row = requestRow(rowIndex, columnIndex);
			if (row == null) return null;
			if (row instanceof Row) return row.get(columnIndex - inputColumns.size());
			return row.get(columnIndex);
		}

		//---------------------------------------------------------------------
		// Non-private methods
		
		/** Sets the table and the scroll pane that are use this model. */
		void setReferenceTable(JTable refTable, JScrollPane refTablescrollPane) {
			jTable   = refTable;
			jTableScp= refTablescrollPane;
		}

		/** Opens a result table identified by 'model_id' and 'batch_ids'. It will 
		 *  display 'nrows' rows.
		 */
		synchronized void openResultTable(long model_id, List<Long> batch_ids, int nrows) {	// [EDT]
			clear(false);
			this.model_id	= model_id;
			this.batch_ids  = batch_ids;
			this.nrows		= nrows;
			requestRow(0,0);
		}

		/** Clears the model. If 'notify' is true the model notifies its observers
		 *  about the change.
		 */
		synchronized void clear(boolean notify) {						// [EDT]
			nrows			= 0;
			inputColumns	= null;
			outputColumns	= null;
			cacheOffset		= 0;
			inputCache.clear();
			outputCache.clear();
			pendingFill	= false;
			if (notify)
				Utils.invokeLater(this, "fireTableStructureChanged");
		}

		//---------------------------------------------------------------------
		// Internals

		/** Returns the row identified by 'rowidx'. 'colidx' indicates what 
		 *  part (input or output) of the whole row must returns. 
		 */
		private synchronized GeneralRow requestRow(int rowidx, int colidx) {		// [EDT]
			if (rowidx < cacheOffset || cacheOffset + outputCache.size() <= rowidx) {
				int maxCacheSize = calcMaxCacheSize();
				cacheOffset = Math.max(0, rowidx - maxCacheSize / 2);
				inputCache.clear();
				outputCache.clear();
				inputCache.addAll(java.util.Collections.nCopies(Math.min(nrows - cacheOffset, maxCacheSize), (GeneralRow)null));
				outputCache.addAll(java.util.Collections.nCopies(Math.min(nrows - cacheOffset, maxCacheSize), (GeneralRow)null));
				updateCache();
			}
			int i = rowidx - cacheOffset;
			Vector<GeneralRow> ans = inputColumns == null ? null : ((colidx < inputColumns.size()) ? inputCache : outputCache);
			return (ans != null && 0 <= i && i < ans.size()) ? ans.get(i) : null;
		}
		
		/** Updates the content of the caches. */
		private void updateCache() {									// [EDT or Model thread]
			if (EventQueue.isDispatchThread()) {
				// [EDT only]
				if (!pendingFill) {
					pendingFill = true;
					MEMEApp.LONG_OPERATION.begin("Loading rows from the result tables...", this);
				}
				return;
			}
			// [Model thread only]
			// The following cycle is needed because 'cacheOffset' may be modified in the EDT thread
			// during the database operation.
			boolean columnsChanged = false;
			while (true) {
				long mid;
				Long[] batches = batch_ids.toArray(new Long[0]);
				int offs, n;
				synchronized (this) {
					mid = model_id;
					offs = cacheOffset;
					n = outputCache.size();
				}
				Vector<GeneralRow> iTmp = new Vector<GeneralRow>(n);
				Vector<GeneralRow> oTmp = new Vector<GeneralRow>(n);
				IResultsRowsIterator it = MEMEApp.getResultsDb().getResultsRows(mid,batches,offs,n);
				it.iterator();
				
				try {
					while (oTmp.size() < n && it.hasNext()) {
						Object[] element = it.next();
						if (element == null || element.length < 3)				// database error
							break;
						Result result = (Result)element[0];
						Row row = (Row)element[1];
						ParameterComb input = result.getParameterComb();
						iTmp.add(input.getValues());
						oTmp.add(row);
						synchronized (this) {			// detect changes in cacheOffset
							n = outputCache.size();
							if (cacheOffset != offs) break;
							if (inputColumns == null) {
								inputColumns = input.getNames();
								columnsChanged = true;
							}
							if (outputColumns == null) {
								outputColumns = row.getColumns();
								columnsChanged = true;
							}
						}
					}
				} finally {
					it.dispose();
				}
				synchronized (this) {					// detect changes in cacheOffset
					if (cacheOffset == offs) {
						n = outputCache.size();
						inputCache = iTmp;
						outputCache = oTmp;
						// Jelenleg a Model szalban vagyunk, az alabbiaknak pedig 
						// az EDT szalban kell futniuk - ezert kell invokeLater
						if (!inputCache.isEmpty() && !outputCache.isEmpty()) {
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

		/** Calculates and returns the maximum size of the caches. */
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
	
	private JButton getJShowInfoButton() {
		if (jShowInfoButton == null) {
			jShowInfoButton = new JButton("Show info");
			jShowInfoButton.addActionListener(this);
		}
		return jShowInfoButton;
	}
}
