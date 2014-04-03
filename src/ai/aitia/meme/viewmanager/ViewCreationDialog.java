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

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.EventObject;

import javax.swing.JDialog;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.ViewRec;
import ai.aitia.meme.gui.Wizard;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;

//-----------------------------------------------------------------------------
/**
 * This class implements the "Views/Create view" menu function.
 * The start() method is invoked by the menu's Action. 
 */
public class ViewCreationDialog implements Utils.IUnary<ArrayList<Object>>
{
	static String WINDOW_TITLE = "Create View Wizard";

	/** The rule of the current view. */
	private ViewCreationRule	rule		= null;
	/** The wizard object. */
	private Wizard				wizard		= null;
	ParameterSet				params		= new ParameterSet();
	JDialog						window		= null;
	/** Error object: not null value means the view is wrong. */
	Throwable					viewCreationFinished = null;
	boolean 					warning		= false;
	/** Flag that determines whether the view is existed before or not. */
	boolean						isRecreating= true;
	boolean						updateOrdering = true;

	Page_InputTables			page_input	= null;
	Page_Columns				page_columns= null;
	Page_Sorting				page_sorting= null;
	Page_NameAndDescription		page_name	= null;

	//=========================================================================
	//	Public interface

	//-------------------------------------------------------------------------
	/** 
	 * This method creates and displays the View Creation Wizard, and does not
	 * return until the wizard is closed (either finished or cancelled).
	 * The closing event is handled in {@link #onClose(EventObject)}.
	 */
	public void start(ViewCreationRule	data) {
		if (data == null)
			throw new NullPointerException();
		
		System.gc();	// try to be sure that weak references are cleared
		rule = data;
		ArrayList<Object> selection = null;
		selection = rule.getInputTables();

		wizard = new VCWizard();

		page_input	= wizard.addPage(new Page_InputTables(this));
		page_columns= wizard.addPage(new Page_Columns(this));
		page_sorting= wizard.addPage(new Page_Sorting(this));
		page_name	= wizard.addPage(new Page_NameAndDescription(this));

		updateOrdering = true;		// delayed update, occurs when columns are loaded into page_columns
		page_input.setSelection(selection, true);	// this involves beginUpdateParameters()
		if (!selection.isEmpty())
			wizard.gotoPage(1);

		window = Wizard.showInDialog(wizard, MEMEApp.getAppWnd());
		updateViewNameOnWindowTitle(page_name.getViewTableName());
		window.setVisible(true);	// onClose() will be triggered before this returns
		wizard = null;
		window = null;
		page_name	= null;
		page_input 	= null;
		page_columns= null;
		page_sorting= null;
	}

	//-------------------------------------------------------------------------
	/** 
	 * This method creates and displays the View Creation Wizard, and does not
	 * return until the wizard is closed (either finished or cancelled).
	 * The closing event is handled in {@link #onClose(EventObject)}.
	 */
	public void start(Long[][] selected_results) {
		ViewCreationRule r = new ViewCreationRule();
		r.setInputTables( java.util.Arrays.asList(selected_results) );
		isRecreating = false;
		start(r);
	}

	//-------------------------------------------------------------------------
	/** 
	 * This method creates and displays the View Creation Wizard, and does not
	 * return until the wizard is closed (either finished or cancelled).
	 * The closing event is handled in {@link #onClose(EventObject)}.
	 */
	public void start(ViewRec[] selected_rec) {
		ViewCreationRule r = new ViewCreationRule();
		r.setInputTables( java.util.Arrays.asList(selected_rec) );
		isRecreating = false;
		start(r);
	}

	//-------------------------------------------------------------------------
	// [EDT]
	public static void test(String[] args) throws Exception {
		new ViewCreationDialog().start(new ViewCreationRule(Utils.loadText("rule.xml")));
	}

	//-------------------------------------------------------------------------
	/** This class represents the View Creation Wizard. */
	@SuppressWarnings("serial")
	class VCWizard extends Wizard {
		public VCWizard() {
			super();
			getJInfoScrollPane().setPreferredSize(new Dimension(GUIUtils.GUI_unit(20),70));
		}
		
		@Override protected boolean canClose() {
			if (isCanceling())
				return true;

			// The Finish button has been pressed - Do the work!
			// If some error occurs (e.g. syntax error in a script),
			// the wizard remains open, so the user can correct the
			// mistakes and try again - not to lose everything
			// without saving. 

			rule.setName(page_name.getViewTableName());
			rule.setDescription(page_name.getDescription());

			rule.setInputTables(page_input.getInputTables());
			rule.setCondition(page_columns.getCondition());
			rule.setGrouping(page_columns.getGrouping());
			rule.setOrdering(page_sorting.getOrdering());

			rule.clearColumns();
			for (int i = 0, n = page_columns.getColumnCount(); i < n; ++i) {
				rule.addColumn(page_columns.getColumn(i));
			}

			// Enter data to the database - long operation
			MEMEApp.logError("ViewCreationRule:\n%s", rule);
			viewCreationFinished = null;
			warning = false;
			String savedTitle[] = { null };
			MEMEApp.LONG_OPERATION.setTitle("Creating the view table", savedTitle);
			MEMEApp.LONG_OPERATION.begin("Creating the view table", 
					new ViewCreation(rule, ViewCreationDialog.this));
			MEMEApp.LONG_OPERATION.showProgressNow();
			MEMEApp.LONG_OPERATION.setTitle(savedTitle[0], null);
			boolean ans = (viewCreationFinished == null);
			if (!ans) page_columns.update();
			else {
				// A view letrehozasa sikeres volt: valtsunk at a Views lapra es mutassuk.
				// Azert van hosszu muveletben, hogy megvarja, amig frissul a lista  
				// a views lapon (az uj view megjelenik benne)
				MEMEApp.LONG_OPERATION.begin("", new LongRunnable() {
					@Override public void finished() {
						MEMEApp.getMainWindow().showViewTable(null, rule.getName());
						if (warning)
							MEMEApp.userAlert("Some warning occured during the view creation.","Please see the log for details.");
					}
				});
			}
			Utils.invokeAfter(5*1000, System.class, "gc");
			Utils.invokeAfter(8*1000, System.class, "gc");
			Utils.invokeAfter(9*1000, System.class, "gc");
			Utils.invokeAfter(10*1000, System.class, "gc");
			return ans;
		}
	}


	//=========================================================================
	//
	//	SERVICES FOR WIZARD PAGES
	//

	//-------------------------------------------------------------------------
	ViewCreationRule	getRule()					{ return rule; }
	Wizard				getWizard()					{ return wizard; }

	//-------------------------------------------------------------------------
	ParameterSet getSelectedPars() {
		return params;
	}

	//-------------------------------------------------------------------------
	/** Returns the columns of the view. */
	ArrayList<ViewCreationRule.Column> getColumns() {
		int n = page_columns.getColumnCount();
		ArrayList<ViewCreationRule.Column> tmp = new ArrayList<ViewCreationRule.Column>(n);
		for (int i = 0; i < n; ++i) tmp.add(page_columns.getColumn(i));
		return tmp;
	}

	//-------------------------------------------------------------------------
	boolean isSplittingUsed() {
		return page_columns.isSplittingUsed() != 0;
	}

	//-------------------------------------------------------------------------
	/** Begins the operation that updates the parameters from the data base. It returns
	 *  immediately. */ 
	void beginUpdateParameters(boolean viewColsOnly) {
		params.collectParams(getColumns(), 
							 (page_input == null || viewColsOnly)? null : page_input.getInputTables(),
							 this);
		wizard.enableDisableButtons();
	}

	//-------------------------------------------------------------------------
	/** This is "endUpdateParameters()": called by params.collectParams() when finished. */ 
	public void run(ArrayList<Object> arg) {
		if (arg != null) {
			page_input.setSelection(arg, false);
		}
		if (updateOrdering) {
			updateOrdering = false;
			if (isRecreating)
				page_sorting.setUpdateMode(Page_Sorting.UpdateMode.FILL_FROM_RULE);
			else
				page_sorting.setUpdateMode(Page_Sorting.UpdateMode.FILL_FROM_COLUMNS);
		}
	}

	//-------------------------------------------------------------------------
	void updateViewNameOnWindowTitle(String name) {
		if (window != null) {
			if (name == null || name.length() == 0)
				window.setTitle(WINDOW_TITLE);
			else
				window.setTitle(String.format("%s - %s", WINDOW_TITLE, name));
		}
	}

	//-------------------------------------------------------------------------
	/** Sets the error object to 'error'. This means the view creation is finished
	 *  because of this error. 
	 */
	void viewCreationFinished(Throwable error) {
		this.viewCreationFinished = error;
	}
	
	//----------------------------------------------------------------------------------------------------
	void warning() {
		warning = true;
	}

	//-------------------------------------------------------------------------
	/** Updates and returns the rule of the current view. */
	ViewCreationRule updateRule() {
		rule.setName(page_name.getUncheckedViewTableName());
		rule.setDescription(page_name.getDescription());

		rule.setInputTables(page_input.getInputTables());
		rule.setCondition(page_columns.getCondition());
		rule.setGrouping(page_columns.getGrouping());
		rule.setOrdering(page_sorting.getOrdering());

		rule.clearColumns();
		for (int i = 0, n = page_columns.getColumnCount(); i < n; ++i) {
			rule.addColumn(page_columns.getColumn(i));
		}
		return rule;
	}
}
