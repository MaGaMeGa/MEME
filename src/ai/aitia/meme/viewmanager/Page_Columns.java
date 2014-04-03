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

import static ai.aitia.meme.gui.ExpandedEditor.makeEEButton;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.UserPrefs;
import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.gui.DraggableViewport;
import ai.aitia.meme.gui.MainWindow;
import ai.aitia.meme.gui.Wizard;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.pluginmanager.IVCPlugin;
import ai.aitia.meme.pluginmanager.Parameter;
import ai.aitia.meme.pluginmanager.PluginInfo;
import ai.aitia.meme.pluginmanager.PluginManager.PluginList;
import ai.aitia.meme.pluginmanager.impl.PluginContextBase;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.FormsUtils.CellInsets;
import ai.aitia.meme.utils.FormsUtils.Separator;
import ai.aitia.meme.viewmanager.JTableScrollPane.ColInfo;
import ai.aitia.meme.viewmanager.ParameterSet.Category;
import ai.aitia.meme.viewmanager.ParameterSet.Par;
import ai.aitia.meme.viewmanager.ParameterSet.ParListCellRenderer;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;

/** Wizard page "Computation" in the View Creation Wizard. */ 
public class Page_Columns extends JPanel implements Runnable, Wizard.IArrowsInHeader,
														Wizard.IWizardPage, Wizard.ICloseListener,  
														JTableScrollPane.IDataTypeCalculator,
														Utils.NameGenerator.IFinder,
														java.awt.event.ActionListener, 
														javax.swing.event.ListSelectionListener,
														javax.swing.event.ListDataListener,
														javax.swing.ListCellRenderer
{
	private static final long serialVersionUID	= 1L;
	private java.io.File helpFile = new java.io.File(MEMEApp.g_AppDir, "Documents/MEME_Beanshell.pdf"); 
	private static final String DEFAULT_SPLITTER_DELIMITER = "_";
	private static javax.swing.ImageIcon pluginIcon = null;
	private static final AggrComboItem VECTOR_MODE_SCRIPT = new AggrComboItem(null);
	private static final int PROBLEM_TIMEOUT		= 5 * 1000;
	// warning levels
	private static final int WL_ALL				= 0;
	private static final int WL_NONE				= 0;
	private static final int WL_COLUMN_PROP		= 0;
	private static final int WL_NO_COLUMN_PROP	= 1;
	private static final int WL_TABLE				= 1;
	// clearForm() constants
	private static final int CF_ADD				= -1;
	private static final int CF_CANCEL			= -2;

	private static final int idx_AllPars			= Category.values().length;

	private static enum Mode { IMMEDIATE, LATER, SCHEDULE_END };  

	private ViewCreationDialog		owner		= null;
	private boolean	in_enableDisableButtons = false;
	private boolean	clearName				= false;
	private boolean 	updateScheduled			= false;
	private boolean	updateRule				= true;
	private boolean editedColumn			= false;
	private String		generatedColName		= null;
	private ArrayList<? extends Parameter>	allPars		= null;
	private BitSet		visiblePars				= null;
	private Utils.Pair<String, Integer>	 warning= new Utils.Pair<String, Integer>(null, WL_NONE);	// text, level
	private int		warningLevel			= WL_ALL;
	private Utils.TimerHandle warningTimer		= null;
	private int		editedIdx				= -1;	// indicates whether a new (<0) or an existing (>=0)  
														// column's properties are being edited
	private Boolean		wasSplitter				= null;	// az aktualisan szerkesztett oszlop eredetileg splitter volt-e
	private boolean	jScriptTextAreaWasEmpty	= true; 
	private DefaultListCellRenderer comboRenderer = null;

	private JTextArea	jFilterTextArea			= new JTextArea();
	private JTextArea	jScriptTextArea			= new JTextArea();
	private JTextField	jColNameTextField		= new JTextField();
	private JTextField	jSplittedDelimTextField	= new JTextField();
	private JComboBox	jAggregateCombo			= new JComboBox();

	private JTableScrollPane jTableScrollPane	= null;
	private JList[]		jParList				= null;
	private JTabbedPane jParListTabbedPane		= null;
	private Color		pltpNormalColor, pltpOtherColor;	// "pltp" = "ParListTabbedPane"

	private String		filterLabel				= "&Filter expression:";
	private JLabel		jResultTypeLabel		= new JLabel("Result type:");
	private JLabel		jNamingDelimiter		= new JLabel("Naming delimiter:");

	private JButton		jFilterEEButton			= makeEEButton(jFilterTextArea,filterLabel);
	private JButton		jScriptEEButton			= null; // initialized later, need owner to do it
	private JButton		jHelpButton				= new JButton("Help");
	private JButton		jMoveUpButton			= new JButton("Move up");
	private JButton		jMoveDownButton			= new JButton("Move down");
	private JButton		jAddButton				= new JButton();
	private JButton		jCancelButton			= new JButton();
	private JButton		jRemoveButton			= new JButton("Remove");
	private JButton		jEditButton				= new JButton("Edit");
	private JToggleButton	jBooleanDataTypeButton	= null;
	private JToggleButton	jIntDataTypeButton		= null;
	private JToggleButton	jRealDataTypeButton		= null;
	private JToggleButton	jStringDataTypeButton	= null;
	private JCheckBox	jSplittedCheckBox		= new JCheckBox("One column per splitter value");
	private JCheckBox	jHiddenCheckBox			= new JCheckBox("Hidden");
	private JCheckBox	jForceAggregateCheckBox	= new JCheckBox("Exclude from grouping / used in aggregate calculations only");
	private JCheckBox	jScriptCheckBox			= new JCheckBox("Custom expression or script");
	private JPopupMenu 	tableContextMenu 		= new JPopupMenu();
	
	private AbstractAction moveUpAction = null;
	private AbstractAction moveDownAction = null;
	private AbstractAction removeAction = null;
	private AbstractAction editAction = null;
	
	//=========================================================================
	//	Public methods

	//-------------------------------------------------------------------------
	public Page_Columns(ViewCreationDialog owner) {
		super();
		this.owner = owner;
		jScriptEEButton	= makeEEButton(jScriptTextArea,"Custom expression or script:",true,owner.params);
		initialize();
	}

	//-------------------------------------------------------------------------
	public String					getCondition()		{ return jFilterTextArea.getText(); }
	public int 					getColumnCount()	{ return getData().getRowCount(); }
	public ViewCreationRule.Column 	getColumn(int col)	{ return getJTableScrollPane().getViewCol(col); }

	//-------------------------------------------------------------------------
	/** Returns true if grouping is used: at least one column requires aggregative calculation */
	public boolean getGrouping() {
		//return jUseGroupingCheckBox.isSelected();
    	for (int i = 0, n = getColumnCount(); i < n; ++i) {
    		ViewCreationRule.Column c = getColumn(i);
    		if (!c.isScalar()) return true;
    	}
    	return false;
	}

	//-------------------------------------------------------------------------
	/** 
	 * @return 0: -splitter-splitted, 1: +splitter-splitted, 
	 *          2: -splitter+splitted, 3: +splitter+splitted
	 */
	public int isSplittingUsed() {
		boolean splitter = false, splitted = false;
    	for (int i = 0, n = getColumnCount(); i < n; ++i) {
    		ViewCreationRule.Column c = getColumn(i);
    		splitter = splitter || c.isSplitter();
    		splitted = splitted || c.isSplitted();
    	}
    	return (splitter ? 1 : 0) + (splitted ? 2 : 0);
	}
	public void update()								{ updateDataTypes(Mode.LATER); }

	//=========================================================================
	//	Interface methods

	//-------------------------------------------------------------------------
	public String		getTitle()				{ return "Computation"; }
	public Container	getPanel()	 			{ return this; }
	public String		getInfoText(Wizard w)	{
		String s;
		if (warning.getFirst() != null && warning.getSecond() >= warningLevel)
			s = "<img src=\"gui/icons/warning.png\">&nbsp;&nbsp;" + Utils.htmlQuote(warning.getFirst());
		else
			s = "Define columns of the view table and computation rules "; 
					//+ "<img src=\"gui/icons/placeholder16.png\">";

		return w.getArrowsHeader(s);
	}

	//-------------------------------------------------------------------------
	public boolean isEnabled(Wizard.Button b) {
		switch (b) {
			case FINISH :
			case NEXT : { 
				int n = getColumnCount();
				boolean ok = (n > 0) && (isSplittingUsed() % 3 == 0);
				for (int i = n - 1; i >= 0 && ok; --i) {
					ok = (getColumn(i).getInitialDataType() != null);
				}
				return ok;
			}
			default : return true;
		}
	}

	//-------------------------------------------------------------------------
	public boolean	 onButtonPress(Wizard.Button b) {
		boolean ans = isEnabled(b);
		// Finish magaba foglalja Modfy-t is, ha lehet
		if (ans && b == Wizard.Button.FINISH && editedIdx >= 0 && jAddButton.isEnabled()) {
			ans = addOrModify();
		}
		if (warning(b == Wizard.Button.FINISH && ans && isAllHidden(),WL_TABLE,"There is no non-hidden column.")) 
			return false;
		return ans;
	}

	//-------------------------------------------------------------------------
	public void				onPageChange(boolean show)	{
		if (owner == null)		// GUI-builder case
			return;
		// Update the data types to reveal missing columns
		updateDataTypes(Mode.LATER);

		if (show) {
			enableDisableButtons(Mode.LATER);
			setWarningLevel(WL_NO_COLUMN_PROP);
			if (owner != null) {
				// A wizard mas lapjai elallithattak a limit-et, ezert biztos ami biztos beallitjuk.
				owner.getSelectedPars().getListModel(Category.THISVIEW, editedIdx);
			}
		}
	}

	//-------------------------------------------------------------------------
	public void onClose(Wizard w) {
		if (warningTimer != null)
			warningTimer.stop();
		warningTimer = null;
	}

	//-------------------------------------------------------------------------
	/** 
	 * Verifies that the source column(s) referenced by 'colInfo' is/are included
	 * in the current set of input tables. Returns null if the check fails.
	 */
	// [EDT]
	public ColumnType getViewColDataType(ViewCreationRule.Column col, int idx) {
		if (col == null) return null;
		ArrayList<Utils.Pair<SrcType, String>> sources = col.getSource();
		ArrayList<Parameter> inputPars = new ArrayList<Parameter>(sources.size());
		for (Utils.Pair<SrcType, String> src : sources) {
			Category c = srcType2Category(src.getFirst());
			Parameter p;
			if (c == null) {								
				if (src.getFirst() == SrcType.GROUP_SCRIPT)		// group script
					return col.getInitialDataType();
				if (sources.size() == 1) {						// scalar script
					// the script is the only source
					p = new Parameter(col.getName(), col.getScriptDataType());
				} else {
					p = null;
				}
			} else {
				p = owner.getSelectedPars().findParam(src.getSecond(), c, idx); 
			}
			if (p == null || p.getDatatype() == null) 
				return null;								// parameter not found or has unknown type
			inputPars.add(p);
		}
		// inputPars may be empty: some plugins don't need input pars 
		String fn = col.getAggrFn();
		if (fn == null || fn.length() == 0)
			return (inputPars.size() != 1) ? null : inputPars.get(0).getDatatype();
		IVCPlugin pl = getAvailablePlugins().findByName(fn);

		// 'allPars', 'visiblePars' are used in VCPluginContext
		visiblePars = null;		// this ensures that 'idx' will be used
		return (pl == null) ? null : pl.getReturnType(new VCPluginContext(inputPars, idx));
	}


	//=========================================================================
	//	Controller methods

	//-------------------------------------------------------------------------
	private DefaultTableModel	getData()				{ return getJTableScrollPane().getData(); }
	private JTable				getJTable() 			{ return getJTableScrollPane().getJTable(); }

	//-------------------------------------------------------------------------
	// [EDT]
	/** Enables/disables the buttons of the page. 'm' defines the executing time. */
	public void enableDisableButtons(Mode m) {
		if (in_enableDisableButtons && m != Mode.SCHEDULE_END) return;
		if (m == Mode.LATER) {
			in_enableDisableButtons = true;
			Utils.invokeLater(this, "enableDisableButtons", Mode.SCHEDULE_END);
			return;
		}
		boolean save = (m == Mode.SCHEDULE_END) ? false : in_enableDisableButtons;
		in_enableDisableButtons = true;
		try { enableDisableButtonsImpl(); }
		finally { in_enableDisableButtons = save; }
	}


	//-------------------------------------------------------------------------
	/** Enables/disables the buttons of the page. */
	private void enableDisableButtonsImpl()
	{
		// Kenyszerito ertekek: ha van ilyen, akkor ez kell legyen az erteke, es le kell tiltani
		Boolean forceAggrChkBox = null;	
		Boolean splitted = null;
		Boolean hidden = null;
		Boolean script = null;
		boolean nameEnabled = true;
		boolean ok = true;
		boolean aggrComboEnabled = 	true; 	// jUseGroupingCheckBox.isSelected();
		boolean vectorModeScript = (aggrComboEnabled && jAggregateCombo.getSelectedItem() == VECTOR_MODE_SCRIPT);

		ArrayList<Par> pars = new ArrayList<Par>(getSelectedPars());
		if (generatedColName == null)
			generatedColName = generateName(null);
		String suggestedName = generatedColName;
		
		// Bug fix #1446
		final Par pRun = new Par("Run ",ColumnType.INT,Category.INPUT);
		final Par pTick = new Par("Tick ",ColumnType.INT,Category.INPUT);
		aggrComboEnabled = !pars.contains(pRun) && !pars.contains(pTick);
		// End of bug fix #1446	
		
		switch (pars.size()) {
			case 0 :
				suggestedName = generateName(null);
				ok = ok && !warning(!jScriptCheckBox.isSelected() && !vectorModeScript, WL_COLUMN_PROP, 
									"No parameter is selected");
				break;
			case 1 :
				suggestedName = generateName(pars.get(0).getName());
				script = false;
				break;
			default :
				if (!aggrComboEnabled || jAggregateCombo.getSelectedItem() == null) {	// Multi-column projection
					nameEnabled = false;
					forceAggrChkBox = false;
					hidden = false;
					splitted = false;
					// If the user is editing a single, existing column, multi-column projection is disabled:
					ok = ok && !warning(editedIdx >= 0, WL_COLUMN_PROP,
										"Too many parameters are selected"); 
				} else {								// Aggregative operation with multiple parameters
					nameEnabled = true;
					suggestedName = generateName(null);
				}
				script = false;
				break;
		}

		if (vectorModeScript) {
			script = true;
			ok = ok && !warning(jScriptTextArea.getText().length() == 0, WL_COLUMN_PROP, 
								"The script is empty");
		} else if (jScriptCheckBox.isSelected()) {
			pars.clear();
			pars.add(new Par(jColNameTextField.getText(), getScriptResultType(), Category.THISVIEW));
			ok = ok && !warning(jScriptTextArea.getText().length() == 0, WL_COLUMN_PROP, 
								"The script is empty");
		}
		jAggregateCombo.setEnabled(aggrComboEnabled);
		updateAggrComboList(pars);

		jColNameTextField.setEnabled( nameEnabled );
		if (clearName) {
			jColNameTextField.setText("");
			clearName = false;
		}
		else {
			String s = jColNameTextField.getText();
			if (nameEnabled && (s.length() == 0 || s.equals(generatedColName))) {
				jColNameTextField.setText(suggestedName);
				generatedColName = suggestedName;
			} else {
				setWarningLevel(WL_ALL);
			}
		}

		if (aggrComboEnabled) {
			if (jAggregateCombo.getSelectedItem() != null) {
				setWarningLevel(WL_ALL);
				forceAggrChkBox = true;
			} else if (pars.size() == 1 && (pars.get(0) instanceof ParameterSet.ThisViewPar)
							&& !((ParameterSet.ThisViewPar)pars.get(0)).col.isScalar()) {
				// vektoros szamolasu oszlopnak a projekcioja is vektoros
				forceAggrChkBox = true;
			}
		}
		if (jHiddenCheckBox.isSelected()) {
			splitted = false;
		}
		switch (isSplittingUsed()) {
			case 0 :
				splitted = false;
				break;
			case 1 : 
				warning(true, WL_TABLE, "Inconsistent specification of splitting: no splitted column");
				break;
			case 2 : 
				warning(true, WL_TABLE, "Inconsistent specification of splitting: no splitter column");
				break;
			default :
			case 3 : 
				break;
		}
		// TODO: splitted-et le kell tiltani ha erre az oszlopra hivatkoznak mashol.
		// warning-ban ki kellene irni, hogy ezert lett letiltva.

		force(jSplittedCheckBox, splitted);
		boolean b = jSplittedCheckBox.isSelected();
		jSplittedDelimTextField.setEnabled(b);
		jNamingDelimiter.setEnabled(b);

		if (jSplittedCheckBox.isSelected()) {
			hidden = false;
			forceAggrChkBox = true;
			String name = jColNameTextField.getText();
			if (name.indexOf('%') < 0) { jColNameTextField.setText(name + '%'); }
		} 
		else if (forceAggrChkBox == null && jForceAggregateCheckBox.isSelected()) {
			// Skalar szamolasu oszlop vektorossa minositese egyben segedoszloppa is minositi azt 
			hidden = true;
		}
		
		if (!jSplittedCheckBox.isSelected()) {
			String name = jColNameTextField.getText();
			if (name.endsWith("%")) jColNameTextField.setText(name.substring(0,name.length()-1));
		}

		force(jScriptCheckBox, script);
		b = jScriptCheckBox.isSelected();
		jResultTypeLabel.setEnabled(b);
		jBooleanDataTypeButton.setEnabled(b);
		jIntDataTypeButton.setEnabled(b);
		jRealDataTypeButton.setEnabled(b);
		jStringDataTypeButton.setEnabled(b);
		jScriptTextArea.setEnabled(b);
		jScriptEEButton.setEnabled(b);
		jHelpButton.setEnabled(b);

		force(jHiddenCheckBox, hidden);
		force(jForceAggregateCheckBox, forceAggrChkBox);

		// The event is coming from the table
		ListSelectionModel lsm = getJTable().getSelectionModel();
    	b = !lsm.isSelectionEmpty();
    	jEditButton.setEnabled(b);
    	jRemoveButton.setEnabled(b);
    	editAction.setEnabled(b);
    	removeAction.setEnabled(b);
    	b = b && (getJTable().getRowCount() > 1);
		jMoveUpButton.setEnabled(b);
		jMoveDownButton.setEnabled(b);
		moveUpAction.setEnabled(b);
		moveDownAction.setEnabled(b);
		if (editedColumn) {
			jMoveUpButton.setEnabled(false);
			jMoveDownButton.setEnabled(false);
			jRemoveButton.setEnabled(false);
			moveUpAction.setEnabled(false);
			moveDownAction.setEnabled(false);
	    	removeAction.setEnabled(false);
		}
		jAddButton.setEnabled(ok);
		if (ok) clearProblemText(Mode.IMMEDIATE, WL_COLUMN_PROP);
	}

	//-------------------------------------------------------------------------
	public void actionPerformed(java.awt.event.ActionEvent e) {
		String ac = e.getActionCommand();
		if (e.getSource() == jAddButton || e.getSource() == jColNameTextField) {
			if (jAddButton.isEnabled())
				addOrModify();
		}
		else if (e.getSource() == jRemoveButton) {
			int[] selected = getJTable().getSelectedRows();
			if (selected.length == 0) return;
			String msg[] = new String[2];
			if (selected.length == 1) 
				msg[0] = String.format("The following column will be deleted: %s", getColumn(selected[0]).getName());
			else 
				msg[0] = String.format("The selected columns (%d) will be deleted.",selected.length);
			msg[1] = "Are you sure?";
			boolean ok = JOptionPane.showConfirmDialog(this, msg, "Warning", 
													   JOptionPane.YES_NO_OPTION,
													   JOptionPane.WARNING_MESSAGE
													   ) == JOptionPane.YES_OPTION;
			if (ok) {
				while (selected.length != 0) {
					getData().removeRow(selected[0]);
					selected = getJTable().getSelectedRows();
				}
				enableDisableButtons(Mode.IMMEDIATE);
			}
		}
		else if (e.getSource() == jEditButton) {
			int i = getJTable().getSelectedRow();
			editedColumn = true;
			if (i >= 0) startEdit(i);
		}
		else if (e.getSource() == jCancelButton) {
			editedColumn = false;
			clearForm(CF_CANCEL);
		}
		else if (e.getSource() == jHelpButton) {
			MEMEApp.LONG_OPERATION.begin(String.format("Opening %s...", helpFile.toString()), new LongRunnable() {
				@Override public void trun() throws Exception {
					MEMEApp.getOSUtils().openDocument(helpFile.toURI(), helpFile.getParentFile());
				}
			});
		}
		else if (ac != null && ac.startsWith("MOVE")) {
			// A mozgatast megengedjuk akkor is, ha elrontja a hivatkozasokat.
			// Ezert a mozgatas utan ellenorizni kell, hogy a hivatkozasok sorrendje
			// felborult-e. Ha igen, akkor azt jelezni kell a tablaban hibakeppen.
			// Errol gondoskodik 'updateDataTypes()'
			//
			int offset = Integer.parseInt(ac.substring(4));
			int[] selected = getJTable().getSelectedRows();
			
			List<int[]> intervals = new ArrayList<int[]>();
			int start = selected[0], end = -1, previous = selected[0] - 1;

			for (int i=0;i<selected.length;++i) {
					if (selected[i] == previous + 1) previous = selected[i];
					else {
						end = previous;
						int[] intv = { start, end };
						intervals.add(intv);
						end = -1;
						start = previous = selected[i];
					}
			}
			intervals.add(new int[] { start, selected[selected.length-1] });
			
			getJTable().getSelectionModel().clearSelection();
			for (int[] intv : intervals) {
				int to = intv[0] + offset;
				if (0 <= intv[0] && 0 <= to && intv[1] + offset < getJTable().getRowCount()) {
					getData().moveRow(intv[0],intv[1],to);
					getJTable().getSelectionModel().addSelectionInterval(intv[0] + offset,intv[1] + offset);
				} else 
					getJTable().getSelectionModel().addSelectionInterval(intv[0],intv[1]);
			}
			updateDataTypes(Mode.LATER);
		}
		else if (e.getSource() == jAggregateCombo) {
			formIsModified();
			enableDisableButtons(Mode.IMMEDIATE);
			if (jScriptTextArea.isEnabled() && jAggregateCombo.getSelectedItem() == VECTOR_MODE_SCRIPT) {
				jScriptTextArea.requestFocus();
			}
		}
		else /*if (e.getSource() == jScriptCheckBox)*/ {
			formIsModified();
			enableDisableButtons(Mode.IMMEDIATE);
		}
	}

	//-------------------------------------------------------------------------
	/** Adds new column to the view or modifies the edited one. The return value is
	 *  true if the operation is successful. */
	private boolean addOrModify() {
		boolean add = (editedIdx < 0);

		// Leolvassuk a GUI controlokrol az adatokat
		// Ellenorizzuk, hogy nem hibasak-e
		// es ha rendben van, akkor osszepakoljuk egy ColInfo objektumba.
		ArrayList<ColInfo> newcols = new ArrayList<ColInfo>(1);
		ArrayList<Utils.Pair<SrcType, String>> sources = new ArrayList<Utils.Pair<SrcType, String>>(1);
		Utils.Pair<SrcType, String> src = new Utils.Pair<SrcType, String>();

		String delim = null;

		Collection<Par> pars = getSelectedPars();

		if (pars.size() > 1 && jAggregateCombo.getSelectedItem() == null) {
			// Tobb oszlopot is kivalasztottak egyszeru projekciora
			sources.add(src);
			ColInfo info = new ColInfo(owner.getRule());
			for (Par p : pars) {
				info.col.setInitialDataType(p.getDatatype());
				info.col.setName(generateName(p.getName(), newcols));
				
				// Bug fix #1446
				if ("Run ".equals(p.getName()) || "Tick ".equals(p.getName())) 
					src.set(SrcType.SCALAR_SCRIPT,"$" + p.getName().trim() + "$");
				else 
				// End of bug fix #1446
					src.set(category2SrcType(p.getCategory()), p.getName());
				info.col.setSource(sources);
				if (p instanceof ParameterSet.ThisViewPar) {
					// Vektoros szamolasu oszlopnak a projekcioja is vektoros
					info.col.setGrouping(((ParameterSet.ThisViewPar)p).col.isGrouping());
				} else {
					info.col.setGrouping(true);		// == it is scalar
				}
				newcols.add(info.clone());
			}
		} else {
			ColInfo info = new ColInfo(owner.getRule());
			newcols.add(info);
			ColumnType dt = null, st = null;
			boolean isVector = false;

			// Ellenorizzuk a nevet, hogy ne lehessen duplazodas
			String name = jColNameTextField.getText();
			if (name.length() == 0) {
				name = generateName(null);
				jColNameTextField.setText(name);
			}
			if (name.equals("#")) {
				MEMEApp.userAlert(String.format("The column name cannot be %s.", name),
								"Please enter a different name or press Cancel!");
				jColNameTextField.requestFocus();
				return false;
			}
			int found = findColumn(name);
			if (add && found >= 0 || !add && found >= 0 && found != editedIdx) {
				MEMEApp.userAlert("A column with the same name already exists.",
									"Please enter a different name or press Cancel!");
				jColNameTextField.requestFocus();
				return false;
			}
			if (!add && found < 0) {
				// TODO: modositottak az oszlop nevet, az uj nevet nem hasznalja mas.
				// Ilyenkor vegig kell nyalni a mar meglevo oszlopokat, es megnezni, hogy van-e olyan
				// script amiben a regi nev elofordul. Ha igen, akkor szolni kell hogy a modositasok
				// vegigvitelehez a scriptekbe is bele kellene nyulni. Harom valasz lehet: rendben;
				// modositson de scriptekbe ne nyuljon; megse modositsunk (ilyenkor meg itt kilepunk,
				// mielott torolnenk a formot).
				// Ezutan pedig MAJD vegig kell vinni a modositasokat. Ennek erre az oszlopra meg
				// nincs hatasa, mivel erre az oszlopra csak kesobbi oszlopok hivatkozhattak.
				// A dolog menete egyebkent ez: projekcioknal at kell irni a forrast, ha a regi 
				// nevre hivatkozik, fuggvenyeknel es plugineknel ujra kell szamoltatni az eredmeny 
				// tipusat, scripteknel pedig find&replace-t kell vegrehajtani.
				// Ha egy fgv/plugin eredmenyenek tipusa modosul, akkor azoknak a modosulasoknak
				// a hatasait is vegig kell kovetni (legegyszerubb ha az elso modosulastol kezdve
				// a kovetkezo oszlopokban mindenutt ujraszamoltatjuk a tipusokat). Ennek eredmenyekepp
				// lehet h. a tipus null-ra jon ki egyes oszlopoknal. Ezeknek a tablaban hibakent kell
				// megjelenniuk. -> ezt a tipus-ujraszamolast updateDataTypes() mar most is megcsinalja
			}

			info.col.setName(name);

			// Aggregate function or plugin
			Object o = jAggregateCombo.getSelectedItem();
			if (o == VECTOR_MODE_SCRIPT) {
				src.set(SrcType.GROUP_SCRIPT, jScriptTextArea.getText());
				dt = getScriptResultType();
				isVector = true;
				sources.add(src);
			}
			else if (o instanceof AggrComboItem) {
				AggrComboItem item = (AggrComboItem)o;
				info.col.setAggrFn(item.pinfo.getInternalName());
				dt = item.datatype;
				assert(dt != null);
				isVector = true;	// bedrotoztuk h. plugint csak vektoros modban hasznalunk
			}

			// Hidden
			boolean hidden = jHiddenCheckBox.isSelected();
			info.col.setHidden(hidden);

			// Splitted
			delim = (!hidden && jSplittedCheckBox.isSelected()) ? jSplittedDelimTextField.getText() : null;
			info.col.setSplitted(delim);
			isVector = isVector || (delim != null);

			if (src.getFirst() == null) {
				if (jScriptCheckBox.isSelected()) {
					src.set(SrcType.SCALAR_SCRIPT, jScriptTextArea.getText());
					st = getScriptResultType();
					if (dt == null) { dt = st; st = null; }
					sources.add(src);
				} else {
					assert(!pars.isEmpty());
					for (Par p : pars) {
						if (dt == null) {
							// single-column projection
							assert(pars.size() == 1);
							dt = p.getDatatype();
							// Vektoros szamolasu oszlopnak a projekcioja is vektoros:
							if (p instanceof ParameterSet.ThisViewPar)
								isVector = !((ParameterSet.ThisViewPar)p).col.isScalar();
						}
						// Bug fix #1446
						if ("Run ".equals(p.getName()) || "Tick ".equals(p.getName())) 
							src.set(SrcType.SCALAR_SCRIPT, "$" + p.getName().trim() + "$");
						else
						// End of bug fix #1446
							src.set(category2SrcType(p.getCategory()), p.getName());
						sources.add(src);
						src = new Utils.Pair<SrcType, String>();
					}
				}
			}
			assert(dt != null);
			info.col.setInitialDataType(dt);
			info.col.setSource(sources);
			if (st != null)
				info.col.setScriptDataType(st);

			// Grouping
			boolean grouping = !isVector && !jForceAggregateCheckBox.isSelected();
			info.col.setGrouping(grouping);
		}

		if (add) {
			for (ColInfo info : newcols)
				getJTableScrollPane().addRow(info);
		}
		else {
			// Ha modosult az adattipus, es ezt az oszlopot hasznaljak valahol, akkor az
			// elso hasznalattol kezdve ujra kell MAJD szamoltatni a kesobbi oszlopokban 
			// a fgv/plugin tipusokat.
			// Az alabbi clearForm() hatasara -> owner.beginUpdateParameters() -> ... 
			// -> updateDataTypes() -> getJTableScrollPane().updateDataTypes() meghivodik
			// es ez mindig ujraszamol minden ilyesmit; mas egyebet tehat itt nem kell 
			// tenni ehhez.

			assert(newcols.size() == 1);	// Editalas eseten szerintem nem kene tobb sor keletkezeset megengedni...
			getData().removeRow(editedIdx);
			ColInfo info = newcols.get(0);
			// A Splitter flag-et lehet, hogy torolni kell
			if (wasSplitter != null) {
				if (warning(wasSplitter && !canBeSplitter(info.col), WL_TABLE,
							"The column had to be removed from the splitter set!")) {
					wasSplitter = false;
				}
				info.col.setSplitter(wasSplitter);
			}
			getJTableScrollPane().insertRow(editedIdx, info);
			getJTable().getSelectionModel().setSelectionInterval(editedIdx, editedIdx);
		}
		clearForm(CF_ADD);	// this calls ParameterSet.collectParams(), too 

		// Splitter delimiter erteket, ha mas mint az alapertelmezett, kiirjuk userPrefs-be
		if (delim != null && delim.trim().length() > 0) {
			if (delim.equals(DEFAULT_SPLITTER_DELIMITER))
				MEMEApp.userPrefs.remove(UserPrefs.VWCR_DELIMITER);
			else
				MEMEApp.userPrefs.put(UserPrefs.VWCR_DELIMITER, delim);
		}
		editedColumn = false;
		return true;
	}

	//-------------------------------------------------------------------------
	// This method is called by both the parameter lists and the table.
	// [EDT]
    public void valueChanged(javax.swing.event.ListSelectionEvent e) {
		if (in_valueChanged || e.getValueIsAdjusting()) return;
		assert(EventQueue.isDispatchThread());
		in_valueChanged = true;
		try {
			if (e.getSource() instanceof JList) {
	    		// The event is coming from a parameter list
				// Synchronize the selection amongst lists (Output/Thisview/Input <-> All)
				JList list = (JList)e.getSource(), otherList;
				for (int idx = Math.max(0, e.getFirstIndex()), last = Math.min(list.getModel().getSize()-1, e.getLastIndex()); 
						idx <= last; ++idx) {
	    			// The selection of one parameter was toggled
					Par p = (Par)list.getModel().getElementAt(idx);
					boolean isSelected = list.isSelectedIndex(idx);
					if (list == jParList[idx_AllPars]) {
						otherList = jParList[p.getCategory().ordinal()];
					} else {
						otherList = jParList[idx_AllPars];
					}
					if (!isSelected && otherList.isSelectionEmpty())
						continue;
					javax.swing.ListModel lm = otherList.getModel();
					for (int j = lm.getSize() - 1; j >= 0; --j) {
						Par p2 = (Par)lm.getElementAt(j);
						if (p2.getCategory() == p.getCategory() && p2.getName().equals(p.getName())) {
							if (isSelected)
								otherList.getSelectionModel().addSelectionInterval(j, j);
							else
								otherList.getSelectionModel().removeSelectionInterval(j, j);
							break;
						}
					}
	    		}
			}
		} finally {
			in_valueChanged = false;
		}
		enableDisableButtons(Mode.LATER);	// helps to coalesce calls caused by rapid modifications 
    }										// (e.g. setSelectedPars())
    private boolean in_valueChanged = false;


	//-------------------------------------------------------------------------
    /* Ezt a muveletet meg kell hivni olyankor amikor az also reszen modosult vmi */   
	private void formIsModified() {
		setWarningLevel(WL_ALL);
	}
	
	//-------------------------------------------------------------------------
	// Azert public mert reflection-el hasznaljuk 
	public void jScriptTextAreaChanged() {
		String text = jScriptTextArea.getText().trim();
		boolean isEmpty = (text.length() == 0);
		if (jScriptTextAreaWasEmpty ^ isEmpty) {
			// ures <-> nem ures valtozas
			jScriptTextAreaWasEmpty = isEmpty;
			formIsModified();
			enableDisableButtons(Mode.LATER);
		}
	}


	//-------------------------------------------------------------------------
	/** This class doesn't allow to deletes % from a columname if the column is a splitted
	 *  column.
	 */
	private class JColNameTextFieldFilter extends javax.swing.text.DocumentFilter {
		//javax.swing.text.JTextComponent tc;
		<T extends javax.swing.text.JTextComponent> T install(T comp) { 
			//tc = comp;
			((javax.swing.text.AbstractDocument)comp.getDocument()).setDocumentFilter(this);
			return comp;
		}
		@Override 
		public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
			if (jSplittedCheckBox.isSelected()) {
				int len = fb.getDocument().getLength();
				StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, len));
				sb.delete(offset, offset+length);
				int i = sb.indexOf("%");
				if (i < 0) {
					fb.insertString(len, "%", null);
				}
			}
			super.remove(fb, offset, length);
		}
		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
			if (jSplittedCheckBox.isSelected()) {
				int len = fb.getDocument().getLength();
				StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, len));
				sb.replace(offset, offset+length, text);
				int i = sb.indexOf("%");
				if (i < 0) {
					fb.insertString(len, "%", null);
				}
			}
			super.replace(fb, offset, length, text, attrs);
		}
	}


	//-------------------------------------------------------------------------
	/** This method handles the double click events. */
    private void dblClickInList(JList list, int row) {
    	boolean saved = in_valueChanged;
    	in_valueChanged = true;
    	try {
    		setSelectedPars(java.util.Collections.<Par>emptyList());
    	} finally {
    		in_valueChanged = saved;
    	}
		list.setSelectedIndex(row);
		addOrModify();
    }

	//-------------------------------------------------------------------------
    public void intervalRemoved(javax.swing.event.ListDataEvent e)	{ guardListWidth(e.getSource()); }
    public void contentsChanged(javax.swing.event.ListDataEvent e)	{}
    public void intervalAdded(javax.swing.event.ListDataEvent e)	{
    	guardListWidth(e.getSource());
    	updateDataTypes(Mode.LATER);
    }

	//-------------------------------------------------------------------------
    /** This method should be called when the contents of the parameter lists is
     *  modified or this-view column data types need to be re-calculated.
     */
    // Some ways of calling this method:
    // a) Page_InputTables -> owner.beginUpdateParameters() -> ParameterSet.collectParams() 
    //    -> .updateCategory() -> .ParListModel.updateFrom() -> this.intervalAdded()
    // b) clearForm() -> owner.beginUpdateParameters() -> ...
    // c) clearForm() -> ParameterSet.getListModel() -> .ParListModel.updateFrom() -> ...
    // d) initialize() -> ParameterSet.getListModel() -> ...
    // e) initialize() -> ParameterSet.getListModelForAll() -> .ParListModel.updateFrom() -> ...
    // f) onPageChange()
    // g) jMove*Button -> actionPerformed()
    // h) update();
	// [EDT]
	private void updateDataTypes(Mode m) {
		if (m == Mode.LATER) {
			if (!updateScheduled) {
				updateScheduled = true;
				Utils.invokeLaterNonPublic(this, "updateDataTypes", Mode.SCHEDULE_END);
			}
			return;
		}

		// Arrange for updating 'allPars[]' and 'visiblePars[]' which   
		// cache the list of parameters for aggr.fn. result type calculation
		// (see VCPluginContext)
		allPars = null;
		visiblePars = null;

		if (updateRule) {
			// Load the rule from the owner
			getJTableScrollPane().fill(owner.getRule(), this);	// triggers getViewColDataType()
			updateRule = false;
			enableDisableButtons(Mode.LATER);
			owner.beginUpdateParameters(true);					// frissitse a thisview listajat
		} else {
			// Update the data types of the view table's parameters
			getJTableScrollPane().updateDataTypes(this);		// triggers getViewColDataType()
		}

		// fill()/updateDataTypes() modosithatta nehany Thisview-parameter tipusat.
		// Ahhoz hogy ez a valtozas a GUI-n is megjelenjen, ujra kell rajzoltatni
		// a listat es a tablat is
		jParList[Category.THISVIEW.ordinal()].repaint();
		getJTableScrollPane().getJTable().repaint();

		owner.getWizard().enableDisableButtons();

		updateScheduled = false;
	}

	//-------------------------------------------------------------------------
	/** This method is called whenever the user want to edit an existing column. */
    private void startEdit(int idx) {
    	clearForm(idx);

    	ViewCreationRule.Column col = getColumn(idx);
		boolean isVector = false;
    	jColNameTextField.setText(col.getName());
    	jColNameTextField.selectAll();
    	jColNameTextField.requestFocus();
    	wasSplitter = col.isSplitter();

		ArrayList<Utils.Pair<SrcType, String>> sources = col.getSource();
		ArrayList<Par> tmp = new ArrayList<Par>();
		for (Utils.Pair<SrcType, String> src : sources) {
			switch (src.getFirst()) {
				case SCALAR_SCRIPT :
					jScriptCheckBox.setSelected(true);
					jScriptTextArea.setText(src.getSecond());
					setScriptResultType(col.getScriptDataType());
					break;

				case GROUP_SCRIPT :
					jAggregateCombo.setSelectedItem(VECTOR_MODE_SCRIPT);
					jScriptCheckBox.setSelected(true);
					jScriptTextArea.setText(src.getSecond());
					setScriptResultType(col.getInitialDataType());
					isVector = true;
					break;
					
				default :
					tmp.add(new Par(src.getSecond(), null, srcType2Category(src.getFirst())));
					break;
			}
		}
		setSelectedPars(tmp);
		jHiddenCheckBox.setSelected(col.isHidden());

		String delim = col.getSplitted();
		jSplittedCheckBox.setSelected(delim != null);
		jSplittedDelimTextField.setText(delim != null ? delim : DEFAULT_SPLITTER_DELIMITER);

		String aggrfn = col.getAggrFn();
		if (aggrfn != null && aggrfn.length() > 0) {
			isVector = true;
			updateAggrComboList(getSelectedPars());
			javax.swing.ComboBoxModel cm = jAggregateCombo.getModel();
			for (int i = 0, n = cm.getSize(); i < n; ++i) {
				Object o = cm.getElementAt(i);
				if (o == null || o == VECTOR_MODE_SCRIPT) continue;
				if (aggrfn.equals(((AggrComboItem)o).pinfo.getInternalName())) {
					cm.setSelectedItem(o); break;
				}
			}
		}

		jForceAggregateCheckBox.setSelected(isVector || !col.isGrouping());
		enableDisableButtons(Mode.LATER);
    }

	//-------------------------------------------------------------------------
    /** Clears the column editor area. */
    private void clearForm(int editedIdx) {
    	this.editedIdx = editedIdx;
    	if (editedIdx >= 0) {
    		jAddButton.setText("Modify");
    		jCancelButton.setText("Cancel");
    	} else {
    		jAddButton.setText("Add");
    		jCancelButton.setText("Reset form");
    	}

    	generatedColName = null;
    	clearName = (editedIdx < 0);	// after Cancel & Add
		wasSplitter = null;
		setSelectedPars(java.util.Collections.<Par>emptyList());	// triggers enableDisableButtons() (later)
		jSplittedCheckBox.setSelected(false);
		jHiddenCheckBox.setSelected(false);
		jForceAggregateCheckBox.setSelected(false);
		jScriptCheckBox.setSelected(false);
		jScriptTextArea.setText("");
    	jScriptTextAreaWasEmpty = true;
		jColNameTextField.setText("");
		jAggregateCombo.setSelectedItem(null); 		// this may trigger enableDisableButtons() (immediate)

		setWarningLevel((editedIdx >= 0) ? WL_COLUMN_PROP : WL_NO_COLUMN_PROP);
		if (warning.getSecond() < warningLevel && warning.getFirst() != null)
			clearProblemText(Mode.LATER, warning.getSecond());

		if (owner != null) {
			if (editedIdx == CF_ADD) {
				owner.beginUpdateParameters(true);	// triggers wizard.enableDisableButtons()
			} else {
				owner.getWizard().enableDisableButtons();
			}
			// Atallitja limit-et es ennek megfeleloen frissiti a Thisview es 
			// az All parameterlistakat: 
			owner.getSelectedPars().getListModel(Category.THISVIEW, editedIdx);
		}
    }

	//-------------------------------------------------------------------------
	private static void force(javax.swing.AbstractButton button, Boolean forcedValue) {
		if (forcedValue != null) {
			button.setSelected(forcedValue);
			button.setEnabled(false);
		} else {
			button.setEnabled(true);
		}
	}

	//-------------------------------------------------------------------------
    private void updateAggrComboList(Collection<? extends Parameter> selectedPars) {
    	Object curr = jAggregateCombo.getSelectedItem();
		VCPluginContext ctx = null;

    	// Filter out those plugins that do not accept the selected parameters
    	//
    	// Itt meg nem getReturnType()-ot hivunk, mert a pontos tipus kiszamitasa
    	// esetleg sokkal koltsegesebb lehet mint egy igen/nem valasze, es itt
    	// egyelore eleg ennyi is.
    	ArrayList<AggrComboItem> tmp = new ArrayList<AggrComboItem>();
    	tmp.add(null);
    	if (jAggregateCombo.isEnabled()) {
	    	for (PluginInfo<IVCPlugin> pinfo : getAvailablePlugins()) {
	    		if (ctx == null) ctx = new VCPluginContext(selectedPars, editedIdx);
	    		if (pinfo.getInstance().isSelectionOK(ctx)) 
	    			tmp.add(new AggrComboItem(pinfo));
	    	}
	    	if (selectedPars.isEmpty())
	    		tmp.add(VECTOR_MODE_SCRIPT);
    	}

    	javax.swing.ComboBoxModel cm = new javax.swing.DefaultComboBoxModel(tmp.toArray());
    	int i = tmp.indexOf(curr);
    	if (i >= 0) {
    		curr = tmp.get(i);
    		cm.setSelectedItem(curr);
    		if (curr instanceof AggrComboItem) {
    			AggrComboItem a = (AggrComboItem)curr;
    			if (a.pinfo != null) {
        			// Az eredmeny pontos tipusat csak azzal az egy pluginnal
    				// szamoltatjuk ki, amit a user kivalasztott. 
        			//
    	    		if (ctx == null) ctx = new VCPluginContext(selectedPars, editedIdx);
    				a.datatype = a.pinfo.getInstance().getReturnType(ctx);
    			}
    		}
    	}
    	jAggregateCombo.setModel(cm);
    }

	//-------------------------------------------------------------------------
	/** This triggers {@link #findName()} */
	private String generateName(String tryThis, ArrayList<ColInfo> more) {
		return new Utils.NameGenerator().defName("Column").finder(this).generate(tryThis, more);
	}
	/** This triggers {@link #findName()} */
	private String generateName(String tryThis)	 { return generateName(tryThis, null); }

	//-------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public boolean findName(String name, Object userData) {
		if (findColumn(name) >= 0) 
			return true;
		if (userData != null) {
			for (ColInfo info : (ArrayList<ColInfo>)userData)
				if (name.equals(info.col.getName())) { return true; }
		}
		return false;
	}

	//-------------------------------------------------------------------------
	/** Returns the index of the column named 'name' or -1. */
    private int findColumn(String name) {
    	if (name != null) {
	    	for (int i = 0, n = getColumnCount(); i < n; ++i)
	    		if (name.equals(getColumn(i).getName()))
	    			return i;
    	}
    	return -1;
    }

	//-------------------------------------------------------------------------
    private Collection<Par> getSelectedPars() {
    	java.util.TreeMap<Par, Par> ans = new java.util.TreeMap<Par, Par>();
    	for (int i = 0; i < jParList.length; ++i) {
    		boolean isEmpty = true;
    		for (Object o : jParList[i].getSelectedValues()) {
    			isEmpty = false;
    			Par p = (Par)o, key = new Par(p.getName(), null, p.getCategory());
    			if (!ans.containsKey(key)) {
    				ans.put(key, p);
    			}
    		}
    		jParListTabbedPane.setForegroundAt(i, isEmpty ? pltpNormalColor : pltpOtherColor);
    	}
    	return ans.values();
    }

	//-------------------------------------------------------------------------
    /** Side effect: schedules delayed call of enableDisableButtons() */
    private void setSelectedPars(java.util.List<Par> pars) {
		// Clear the selection
    	boolean save = in_valueChanged;	
    	in_valueChanged = true;				// make implied valueChanged() calls faster
    	try { 
			for (int i = 0; i < jParList.length; ++i)
				jParList[i].clearSelection();
    	} finally {
    		in_valueChanged = save;
    	}
		// Select specified parameters
		for (Par p : pars) {
			JList list = jParList[p.getCategory().ordinal()];
			javax.swing.ListModel lm = list.getModel();
			for (int i = 0, n = lm.getSize(); i < n; ++i) {
				if (lm.getElementAt(i).equals(p.getName())) {
					list.addSelectionInterval(i, i);
					break;
				}
			}
		}
		enableDisableButtons(Mode.LATER);	// just to be sure
    }

	//-------------------------------------------------------------------------
    /** Returns the result type of a the current script. */
    private ColumnType getScriptResultType() {
		if (jBooleanDataTypeButton.isSelected())	return ColumnType.BOOLEAN;
		if (jIntDataTypeButton.isSelected())		return ColumnType.INT;	
		if (jRealDataTypeButton.isSelected())		return ColumnType.DOUBLE;
		return ColumnType.STRING;
    }
    
	//-------------------------------------------------------------------------
    private void setScriptResultType(ColumnType dt) {
    	if (dt.equals(ColumnType.BOOLEAN))
    		jBooleanDataTypeButton.setSelected(true);
    	else if (dt.equals(ColumnType.DOUBLE))
    		jRealDataTypeButton.setSelected(true);
    	else if (dt.equals(ColumnType.INT) || dt.equals(ColumnType.LONG))
    		jIntDataTypeButton.setSelected(true);
    	else 
    		jStringDataTypeButton.setSelected(true);
    }

	//-------------------------------------------------------------------------
    /** Converts a category to a source type. */
	private static SrcType category2SrcType(Category c) {
		switch (c) {
			case INPUT		: return SrcType.PROJECTION_INPUT;
			case OUTPUT		: return SrcType.PROJECTION_OUTPUT;
			case THISVIEW	: return SrcType.PROJECTION_VIEW;
		}
		throw new IllegalArgumentException(c.toString());
	}
	
	//-------------------------------------------------------------------------
    /** Converts a source type to a category. */
	private static Category srcType2Category(SrcType t) {
		switch (t) {
			case PROJECTION_INPUT	: return Category.INPUT;
			case PROJECTION_OUTPUT	: return Category.OUTPUT;
			case PROJECTION_VIEW	: return Category.THISVIEW;
		}
		return null;
	}

	//-------------------------------------------------------------------------
	// This method is used via reflection
	void setWarningLevel(int level) {
		warningLevel = level;
	}

	//-------------------------------------------------------------------------
	/** Displays the message if 'condition' is true. Returns 'condition' */
	private boolean warning(boolean condition, int level, String message) {
		if (level >= warningLevel) {
			String before = warning.getFirst();
			warning.set(condition ? message : null, level);
			if (!Utils.equals(warning.getFirst(), before) && owner != null) {
				owner.getWizard().updateInfo();
				if (warningTimer != null)
					warningTimer.stop();
				if (warning.getFirst() != null)
					warningTimer = Utils.invokeAfter(PROBLEM_TIMEOUT, this);
			}
		}
		return condition;
	}

	//-------------------------------------------------------------------------
	/** used by {@link #warningTimer} */
	public void run() {
		clearProblemText(Mode.IMMEDIATE, warning.getSecond());
	}

	//-------------------------------------------------------------------------
	/** Clears the warning message from the information panel. */
	private void clearProblemText(Mode m, int level) {
		if (m == Mode.IMMEDIATE) {
			String before = warning.getFirst();
			if (warning.getSecond() <= level) {
				warning.set(null, WL_NONE);
				if (warningTimer != null) {
					warningTimer.stop();
					warningTimer = null;
				}
			}
			if (!Utils.equals(warning.getFirst(), before) && owner != null)
				owner.getWizard().updateInfo();
		} else {
			javax.swing.SwingUtilities.invokeLater(this);
		}
	}

	//-------------------------------------------------------------------------
	static PluginList<IVCPlugin> getAvailablePlugins() {
		return MEMEApp.getPluginManager().getVCPluginInfos();
	}

	//-------------------------------------------------------------------------
	/** Record class to encapsulate a view creation plugin and a columntype. */
	static class AggrComboItem {
		PluginInfo<IVCPlugin>	pinfo;
		ColumnType				datatype;
		AggrComboItem(PluginInfo<IVCPlugin> p) { pinfo = p; }
		@Override public String toString() { 
			return pinfo == null ? null : pinfo.getInstance().getLocalizedName();
		}
		// Used in updateAggrComboList() via 'tmp.indexOf(...)'
		@Override public boolean equals(Object obj) {
			if (obj instanceof AggrComboItem)
				return ((AggrComboItem)obj).pinfo == pinfo;
			return super.equals(obj);
		}
	}

	//-------------------------------------------------------------------------
	/** 
	 * Type for arguments of IVCPlugin methods.
	 */
	@SuppressWarnings("serial")
	class VCPluginContext extends PluginContextBase implements IVCPlugin.IContext
	{
		java.util.List<? extends Parameter>		selectedPars;
		Integer									limit;

		//-------------------------------------------------------------------------
		VCPluginContext(Collection<? extends Parameter> pars, Integer limit) {
			setSelectedPars(pars);
			this.limit	= limit;
		}

		//-------------------------------------------------------------------------
		private void setSelectedPars(Collection<? extends Parameter> pars) {
			if (pars == null)
				selectedPars = null;
			else if (pars instanceof java.util.List)
				selectedPars= (java.util.List<? extends Parameter>)pars;
			else
				selectedPars= new ArrayList<Parameter>(pars);
		}

		//-------------------------------------------------------------------------
		//	Interface methods

		//-------------------------------------------------------------------------
		public List<? extends Parameter> getAllPars() {
			if (allPars == null) {
				allPars = owner.getSelectedPars().getAllPars();
			}
			return java.util.Collections.unmodifiableList(allPars);
		}

		//-------------------------------------------------------------------------
		public List<Object[]> getAllValues() {
			return new NCopiesModifiable<Object[]>(getAllPars().size(), null);
		}

		//-------------------------------------------------------------------------
		public BitSet getVisibleParIndices() {
			if (visiblePars == null) {
				visiblePars = new BitSet();
				for (Integer i : owner.getSelectedPars().getVisibleParIndices(getAllPars(), limit))
					visiblePars.set(i);
			}
			return visiblePars;
		}

		//-------------------------------------------------------------------------
		public int indexOf(String parName) {
			List<? extends Parameter> all = getAllPars();
			BitSet visible = getVisibleParIndices();
			for (int i = visible.nextSetBit(0); i >= 0; i = visible.nextSetBit(i+1)) {
				if (all.get(i).equals(parName)) return i;
			}
			return -1;
		}

		//-------------------------------------------------------------------------
		public List<? extends Parameter> getArgumentPars() {
			if (selectedPars == null)
				setSelectedPars(getSelectedPars());
			return selectedPars;
		}

		//-------------------------------------------------------------------------
		public List<Object[]> getArguments() {
			return new NCopiesModifiable<Object[]>(getArgumentPars().size(), null);
		}

		//-------------------------------------------------------------------------
		public boolean isScalarMode() {
			return false;		// ezzel tk. bedrotoztuk h. plugint csak vektoros modban hasznalunk
		}
		
	}

	//-------------------------------------------------------------------------
	/** This class represents a list which contains only one value but it contains that n times. */
	public static class NCopiesModifiable<T> extends java.util.AbstractList<T> implements java.util.RandomAccess {
		int	size;
		T	value;
		public					NCopiesModifiable(int n, T val)	{ size = n; value = val; } 
		@Override public T		get(int index)					{ return value; }
		@Override public int	size()							{ return size; }
		@Override public T		set(int index, T element)		{ return value; }	// do nothing
	}


	//=========================================================================
	//	GUI (View) methods

	//-------------------------------------------------------------------------
	public static void test(String[] args) {
		new Wizard(new Page_Columns(null)).showInDialog(null).setVisible(true);
		System.exit(0);
	}

    /**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		jBooleanDataTypeButton	= new JToggleButton(JTableScrollPane.getColumnTypeIcon(ColumnType.BOOLEAN));
		jIntDataTypeButton		= new JToggleButton(JTableScrollPane.getColumnTypeIcon(ColumnType.INT));
		jRealDataTypeButton		= new JToggleButton(JTableScrollPane.getColumnTypeIcon(ColumnType.DOUBLE));
		jStringDataTypeButton	= new JToggleButton(JTableScrollPane.getColumnTypeIcon(ColumnType.STRING));
		
		//tooltips
		jBooleanDataTypeButton.setToolTipText("Logical");
		jIntDataTypeButton.setToolTipText("Integer");
		jRealDataTypeButton.setToolTipText("Real number");
		jStringDataTypeButton.setToolTipText("String");
		
		GUIUtils.createButtonGroup(jBooleanDataTypeButton, jIntDataTypeButton,
				jRealDataTypeButton, jStringDataTypeButton);
		jRealDataTypeButton.setSelected(true);

		initializeContextMenu();
		
		jMoveUpButton.setActionCommand("MOVE-1");
		jMoveDownButton.setActionCommand("MOVE1");

		GUIUtils.addActionListener(this,
				jColNameTextField,
				jFilterEEButton, jScriptEEButton, jHelpButton,
				jMoveUpButton, jMoveDownButton, jRemoveButton, jEditButton,  
				jAddButton, jCancelButton,
				jAggregateCombo, jSplittedCheckBox, jHiddenCheckBox, jForceAggregateCheckBox,
				jScriptCheckBox,
				jBooleanDataTypeButton, jIntDataTypeButton, jRealDataTypeButton, jStringDataTypeButton
		);
		
		GUIUtils.bind(this,JComponent.WHEN_IN_FOCUSED_WINDOW,"DELETE",jRemoveButton);

		new JColNameTextFieldFilter().install(jColNameTextField);

		jScriptTextArea.getDocument().addDocumentListener(
			java.beans.EventHandler.create(DocumentListener.class, this, "jScriptTextAreaChanged")
		);
		jScriptTextArea.addFocusListener(
			java.beans.EventHandler.create(FocusListener.class, this, "jScriptTextAreaChanged", null, "focusLost")
		);
		jScriptTextArea.setColumns(30);
		jScriptTextArea.setRows(3);

		jAggregateCombo.setRenderer(this);

		getJTable().getSelectionModel().addListSelectionListener(this);
		getJTable().addMouseListener(new java.awt.event.MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				// Double-click in the table should trigger "Edit"
	    		if (!javax.swing.SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 2
	    				|| getJTable().rowAtPoint(e.getPoint()) < 0)
	    			return;
	    		jEditButton.doClick();
			}
			@Override public void mouseReleased(MouseEvent e) {
				// Context menu
				if (!javax.swing.SwingUtilities.isRightMouseButton(e))
					return;
				if (e.getComponent().isEnabled()) {
					int idx = getJTable().rowAtPoint(e.getPoint());
					if (idx == -1)
						idx = getJTable().getRowCount() - 1;
					
					if (!ai.aitia.chart.util.Utilities.contains(getJTable().getSelectedRows(),idx))
						getJTable().getSelectionModel().setSelectionInterval(idx,idx);
					
					tableContextMenu.show(e.getComponent(),e.getX(),e.getY());
				}
			}
		});

		// Plug parameter list data models to JList components
		javax.swing.ListCellRenderer r = new ParListCellRenderer();
		GUIUtils.ToggleClickMultiSelection s = new GUIUtils.ToggleClickMultiSelection() {
			@Override protected void doubleClick(java.awt.event.MouseEvent e, int rowIdx) {
				dblClickInList((JList)e.getSource(), rowIdx);
			}
		};

		jParListTabbedPane = new JTabbedPane();
		jParList = new JList[Category.values().length + 1];
		for (int i = 0; i < jParList.length; ++i) {
			jParList[i] = new JList();
			jParList[i].setCellRenderer(r);
			jParList[i].addListSelectionListener(this);
			jParList[i].setToolTipText("Double-click to add single item");
			s.installOn(jParList[i]);
			if (owner != null) {
				javax.swing.ListModel lm = (i == idx_AllPars) ? owner.getSelectedPars().getListModelForALL()
						: owner.getSelectedPars().getListModel(Category.values()[i], -1);
				lm.addListDataListener(this);	// this will trigger update()
				jParList[i].setModel(lm);
				guardListWidth(jParList[i]);
			}
			JScrollPane sp = new JScrollPane(jParList[i]);
			sp.setBorder(null);
			jParListTabbedPane.add(i == idx_AllPars ? "All" : Category.values()[i].tabTitle, sp);
		}
		pltpNormalColor = jParListTabbedPane.getForeground();
		pltpOtherColor	= getOtherColor(pltpNormalColor, jParListTabbedPane.getBackground());

		if (owner != null) {
			ViewCreationRule rule = owner.getRule();
			String where = rule.getCondition();
			if (where != null)
				jFilterTextArea.setText(where);
			//jUseGroupingCheckBox.setSelected(rule.getGrouping());
		}

		jSplittedDelimTextField.setText(MEMEApp.userPrefs.get(UserPrefs.VWCR_DELIMITER, DEFAULT_SPLITTER_DELIMITER));
		if (jSplittedDelimTextField.getText().trim().length() == 0)
			jSplittedDelimTextField.setText(DEFAULT_SPLITTER_DELIMITER);
		
		clearForm(CF_CANCEL);	// sets the text on some buttons
								// also triggers enableDisableButtons() (later)
		layoutGUI();
	}
	
	//-------------------------------------------------------------------------
	@SuppressWarnings("serial")
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
		editAction = new AbstractAction() {
			{ putValue(NAME,"Edit"); }
			public void actionPerformed(ActionEvent e) {
				 jEditButton.doClick();
			}
		};
		tableContextMenu.add(moveUpAction);
		tableContextMenu.add(moveDownAction);
		tableContextMenu.add(removeAction);
		tableContextMenu.add(editAction);
	}

	//-------------------------------------------------------------------------
	/** Returns the 'avarage' color of 'fg' and 'bg'. */
	public static Color getOtherColor(Color fg, Color bg) {
		int f[]   = { fg.getRed(), fg.getGreen(), fg.getBlue() };
		int b[]   = { bg.getRed(), bg.getGreen(), bg.getBlue() };
		int rgb[] = { (f[0] + b[0]) / 2, (f[1] + b[1]) / 2, (f[2] + b[2]) / 2 };
		rgb[0] = f[0];
		return new Color(rgb[0], rgb[1], rgb[2], fg.getAlpha());
	}

	//-------------------------------------------------------------------------
	// A JList-ek preferredWidth szelessege alapertelmezesben nagyon ugrandozik
	// ures <-> nem ures allapot kozott, es ez nagyon zavaro. Ez a muvelet ezt
	// az ugrandozast probalja kikuszobolni azaltal, hogy ures allapotban kicsire
	// veszi a lista szelesseget, nem-ures allapotban pedig az elemek szelessegetol
	// fuggove teszi.
	// Az 'o' parameter JList is lehet es ListModel is. Utobbi esetben megkeresi 
	// a hozza tartozo JList-et. 
    private void guardListWidth(Object o) {
    	JList list = (o instanceof JList) ? (JList)o : null;
    	if (o instanceof javax.swing.ListModel) {
    		javax.swing.ListModel lm = (javax.swing.ListModel)o;
    		for (int i = jParList.length - 1; i >= 0 && list == null; --i)
    			if (jParList[i].getModel() == lm) list = jParList[i];
    	}
    	if (list != null) {
    		if (list.getModel().getSize() == 0)
    			list.setFixedCellWidth(GUIUtils.dluX(40, list));
    		else
    			list.setFixedCellWidth(-1);
    	}
    }

	//-------------------------------------------------------------------------
	// TextArea wrapper
	@SuppressWarnings("serial")
	static class TAWrapper extends JViewport {
		public TAWrapper(javax.swing.text.JTextComponent tc) {
			this.setView(tc);
			this.setMinimumSize(tc.getMinimumSize());
		}
	}

	//-------------------------------------------------------------------------
	private void layoutGUI() {
		final JPanel panel = new JPanel(new BorderLayout());

		this.setLayout(new BorderLayout());
		this.add(new DraggableViewport(panel));

		panel.setBorder(Borders.DIALOG_BORDER);
		panel.add(FormsUtils.build(
				"pref - min:grow - 30px", "012 min|~|3|=",
				filterLabel, new TAWrapper(jFilterTextArea), jFilterEEButton,	// 0-2
				new Separator("Columns of the view table")					// 3
		).getPanel(), BorderLayout.NORTH);

		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(FormsUtils.build(
				"f:80dlu:g - pref", "01 f:min:g|-",
				getJTableScrollPane(), 
				FormsUtils.buttonStack(
						jMoveUpButton, jMoveDownButton, FormsUtils.BS_UNRELATED, 
						jRemoveButton, jEditButton ).getPanel()
		).getPanel());

		// Use different preferred size for docked and floating conditions 
		JScrollPane jScriptScrollPane = new JScrollPane(jScriptTextArea) /*{
			private static final long serialVersionUID = 1L;
			public Dimension getPreferredSize() {
				if (SwingUtilities.getRoot(this) == SwingUtilities.getRoot(panel)) {
					return new Dimension(100, 24);									// docked
				}
				return new Dimension(100, Sizes.dialogUnitYAsPixel(120, this));	// floating
			}
		}*/;
		MEMEApp.getLF().makeTransparent(jScriptScrollPane);

		// Ezt nem szabad bekapcsolni mert akkor sajnos rosszul szamolja a meretet  
		// es a JScrollPane-ben a fugg.scrollbar letakarhatja a listaelemek veget.
		// Ezen esetleg a LookAndFeel.makeTransparent() tud segiteni. 
		//tp.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		java.awt.Component columnProps = FormsUtils.titledPanel(" Column properties ", FormsUtils.build(
				"- d:g ~~ f:max(120dlu;pref)", "01|%|02|~|03 f:m:g",
				// Left side  #0
				FormsUtils.build("pref ' 25dlu:g ' 30px",
						"011 p|~|" +
						"233||" +
						"444||" +
						"555||" +
						"666||" +
						"777||" +
						"889 f:d:g",
						"&Column name:", jColNameTextField,							// 0-1
						"Aggregative operation:", jAggregateCombo,					// 2-3  
						FormsUtils.build("l:d:g'r:m'20dlu", "012",
								jSplittedCheckBox, jNamingDelimiter,
								jSplittedDelimTextField 				
						).getPanel(),												// 4
						jHiddenCheckBox, CellConstraints.LEFT,						// 5
						jForceAggregateCheckBox,									// 6
						FormsUtils.build("l:m:g = r:m'22px'22px'22px'22px'p", "0123456 p",
								jScriptCheckBox, jResultTypeLabel,
								jBooleanDataTypeButton,
								jIntDataTypeButton,
								jRealDataTypeButton,
								jStringDataTypeButton ,
								jHelpButton
						).getPanel(),												// 7
						jScriptScrollPane, new CellInsets(0,20,0,0),				// 8
						jScriptEEButton, CellConstraints.TOP						// 9
				).getPanel(),
				// Right side  #1,2,3
				FormsUtils.build("0:g, fill:max(40dlu;pref) % fill:max(40dlu;pref), 0:g", "[ColGroups=24]" + 
							"_01_ p",
							jAddButton, jCancelButton 
				).getPanel(),
				"Available parameters:", jParListTabbedPane
		)).getPanel();

		/*boolean useToolbar = false;
		if (useToolbar) {
			JToolBar tb = new JToolBar("Column properties", JToolBar.HORIZONTAL);
			tb.add(columnProps);
			columnProps = tb;
		}*/
		box.add(columnProps);

		panel.add(box, BorderLayout.CENTER);
	}

	//-------------------------------------------------------------------------
	@SuppressWarnings("serial")
	private JTableScrollPane getJTableScrollPane() {
		if (jTableScrollPane == null) {
			jTableScrollPane = new JTableScrollPane() {
				@Override protected Mode getMode() {
					return Mode.PAGE_COLUMNS;
				}
				@Override protected boolean isCellEditable(int row, int column) {
					if (Mode.PAGE_COLUMNS.isSplitter(column) && canBeSplitter(getColumn(row)))
						return true;
					if (Mode.PAGE_COLUMNS.isHidden(column) && canBeHidden(getColumn(row)))
						return true;
					return false;
				}
				@Override protected void splitterChange(int row, boolean value) {
					getColumn(row).setSplitter(value);
					if (value && !getColumn(row).isHidden()) {
						// a splitter bekapcsolja hidden-t is, amit aztan kikapcsolhat a user
						getColumn(row).setHidden(value);
						updateRow(row);
					}
					enableDisableButtons(Page_Columns.Mode.IMMEDIATE);
					owner.getWizard().enableDisableButtons();
				}
				@Override protected void hiddenChange(int row, boolean value) {
					getColumn(row).setHidden(value);
					enableDisableButtons(Page_Columns.Mode.IMMEDIATE);
				}
			};
			jTableScrollPane.getJTable().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			jTableScrollPane.getJTable().getSelectionModel().addListSelectionListener(this);
		}
		return jTableScrollPane;
	}

	//------------------------------------------------------------------------
	/** Tests if 'col' can be a splitter column. */
	private static boolean canBeSplitter(ViewCreationRule.Column col) {
		return col.isScalar() && !col.isSplitted();
	}

	//------------------------------------------------------------------------
	/** Tests if 'col' can be a hidden column. */
	private static boolean canBeHidden(ViewCreationRule.Column col) {
		return !col.isSplitted();
	}

	//------------------------------------------------------------------------
	public java.awt.Component getListCellRendererComponent(JList list, Object value, 
			int index, boolean isSelected, boolean cellHasFocus)
	{
		if (comboRenderer == null) {
			comboRenderer = new DefaultListCellRenderer();
			comboRenderer.setFont(jAggregateCombo.getFont());
		}
		if (value == null) {
			comboRenderer.setText("<none>");
			comboRenderer.setIcon(null);
		} else if (value == VECTOR_MODE_SCRIPT) {
			comboRenderer.setText("<Aggregative script>");
			comboRenderer.setIcon(null);
		} else {
			AggrComboItem item = (AggrComboItem)value;
			if (item.pinfo.getInstance() instanceof IVCPlugin.IBuiltinAggregateFn) {
				comboRenderer.setIcon(null);
			}
			else {
				if (pluginIcon == null)
					pluginIcon = MainWindow.getIcon("type_javaplugin.png");
				comboRenderer.setIcon(pluginIcon);
			}
			comboRenderer.setText(item.toString());
		}
		return comboRenderer;
	}
	
	//-------------------------------------------------------------------------
	private boolean isAllHidden() {
		for (int i = 0; i < getData().getRowCount(); ++i) {
			if (!(Boolean)getData().getValueAt(i,3)) return false; 
		}
		return true;
	}
}
