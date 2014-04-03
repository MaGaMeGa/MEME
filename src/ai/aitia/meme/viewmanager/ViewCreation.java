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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.RandomAccess;

import org.apache.bsf.BSFException;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.DatabaseConnection;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.SQLDialect;
import ai.aitia.meme.database.ViewRec;
import ai.aitia.meme.database.ColumnType.ValueNotSupportedException;
import ai.aitia.meme.database.ViewsDb.AppendView;
import ai.aitia.meme.database.Transaction;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.gui.lop.UserBreakException;
import ai.aitia.meme.pluginmanager.IScriptLanguage;
import ai.aitia.meme.pluginmanager.IVCPlugin;
import ai.aitia.meme.pluginmanager.Parameter;
import ai.aitia.meme.pluginmanager.PluginManager.PluginList;
import ai.aitia.meme.pluginmanager.impl.PluginContextBase;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.viewmanager.ParameterSet.Category;
import ai.aitia.meme.viewmanager.ViewCreationRule.Column;

import static ai.aitia.meme.database.SQLDialect.release;

/**
 * This class is responsible for writing out the rule, which is specified  
 * to the constructor, to the Views database. See the description of trun()
 * for details. 
 */
// Model thread only (the whole class)
public class ViewCreation extends LongRunnable implements InputRowsIterator.ITableChangedListener
{
	static final int		MAX_COLUMNS		= 10000;
	static final int		COMMIT_FREQ		= 5 << 20;		// approx.5 M memory for rows - see at estimateCommitFreq()
	static final String	SCRIPT_LANGUAGE	= "beanshell";	// userprefs?

	final ViewCreationRule		rule;
	final ViewCreationDialog	owner;
	int							nRows;			// number of input rows
	int							nFiltered;		// number of filtered input rows
	int							cntOfRows;		// number of output rows

	boolean						isBasicMode = false;	// fakeBasic eseten is true

	/** 
	 * Contains all parameters (input/output/thisview),
	 * except splitted thisview parameters.
	 * Order is undefined (established in {@link #getNrOfRowsAndCollectColumns(ArrayList)})
	 */ 
	final ArrayList<ValuePar>	allPars		= new ArrayList<ValuePar>();
	AllValues					allValues	= new AllValues(allPars);

	/** Updated automatically by {@link ValuePar#setValue()}, {@link ValuePar#resetChanged()} */ 
	BitSet						changedPars;
	BitSet						thisviewpars, allVisible;
	int[]						defaultOrdering;

	LinkedHashMap<String, ValuePar>		lookupInput 	= new LinkedHashMap<String, ValuePar>();
	LinkedHashMap<String, ValuePar>		lookupOutput	= new LinkedHashMap<String, ValuePar>();
	// Contains all parameters used in the rule, including splitted thisview parameters (with idx==-1)
	LinkedHashMap<String, ThisviewPar>	lookupThisview	= new LinkedHashMap<String, ThisviewPar>();

	SplitManager				blocks;

	/** This variable is maintained by {@link #newResult()}, {@link #newView()}: */
	ArrayList<Integer>			inputIdxMap	= new ArrayList<Integer>();
	/** This variable is maintained by {@link #newResult()}, {@link #newView()}: */
	ArrayList<Integer>			outputIdxMap= new ArrayList<Integer>();
	/** It contains Model/ViewRec objects */
	ArrayList<Object>			tables = new ArrayList<Object>();
	Integer						tableNr;

	String						whereCondition;
	IScriptLanguage				language;
	InterpContext				interp;

	static final String TABLE_TMP2 = "TMP2_GROUPS";
	static final String TABLE_TMP3 = "TMP3";
	static final String INDEX_TMP3 = "TMP3_IDX";
	static final Integer MINUS_ONE = -1;
	static final Long    LONG_LO	= Long.MAX_VALUE >> 31;
	Columns	tmp2cols, tmp4cols;
	int commitFreq = 1000;	// ezt a default erteket elvileg nem hasznaljuk, lenyeg hogy >0

	RowInfo currentRowInfo			= null;		// either a ResultInfo, a ViewInfo or null


	//-------------------------------------------------------------------------
	/** This class represents a parameter with its values. */
	private class ValuePar extends Parameter {
		Object[]		value = { null };
		final int		idx;					//!< -1 for splitted thisview parameters

		/** True when the value of this parameter is actually value[0] and not the
		 * whole array. In other words, the parameter should be scalar in the interpreter,
		 * not array. It is always the case during scalar calculations, but during 
		 * aggregative calculations the parameters change this behavior not uniformly.
		 */
		boolean			isSingleValue = true;

		ValuePar(String name, ColumnType dt, int i)	{ super(name, dt); idx = i; }
		/** Indicates that this parametes is changed. */
		void changed()								{ if (idx >= 0) changedPars.set(idx); }
		void resetChanged()							{ if (idx >= 0) changedPars.clear(idx); }

		void setValue(Object newval) {											// ok
			boolean different = !isSingleValue;
			isSingleValue = true;
			if (value.length == 1) {
				different |= (idx >= 0) && (changedPars.get(idx) || !Utils.equals(newval, value[0]));
				value[0] = newval;
			} else {
				different = true;
				value = new Object[] { newval };
			}
			if (different) changed();
		}
		Object[] setValue(Object[] newval, boolean singleValue) {				// ok
			if (newval == null) throw new NullPointerException("new array is null");
			if (singleValue && newval.length != 1) invalidLength();
			boolean different = (isSingleValue != singleValue) ||
				((idx >= 0) && (changedPars.get(idx) || !Arrays.equals(newval, value)));
			isSingleValue = singleValue;
			Object[] before = value;
			value = newval;
			if (different) changed();
			return before;
		}
		Object getSingleValue() {
			return (value.length == 0) ? null : value[value.length - 1];
		}
		/* Mellesleg atvalt singleValue-ra is */
		/** Reinitializes the parameter. */
		void clearValue() {
			boolean ch = !isSingleValue;
			isSingleValue = true;
			if (value.length == 1) {
				if (value[0] != null) { value[0] = null; ch = true; }
			} else {
				value = new Object[] { null };
				ch = true;
			}
			if (ch) changed();
		}
		void invalidLength() {
			throw new IllegalArgumentException("array length mismatch");
		}
	}

	//-------------------------------------------------------------------------
	/** This class represents a parameter (from the THISVIEW category) with its values. */
	private class ThisviewPar extends ValuePar {
		ViewCreationRule.Column	col;
		BitSet 					visibleIndices = null;

		ValuePar				projectedColumn = null;
		String					script = null;
		IVCPlugin				plugin = null;
		PluginContext			context = null;
		boolean					scalar_script = false;

		ThisviewPar(ViewCreationRule.Column c, int idx) {
			super(c.getName(), c.getInitialDataType(), idx);
			col = c;
		}
	}

	//-------------------------------------------------------------------------
	public ViewCreation(ViewCreationRule rule, ViewCreationDialog owner) {
		this.rule = rule;
		this.owner= owner; 
	}

	//-------------------------------------------------------------------------
	// [EDT]
	public static void test(String[] args) throws Exception {
		MEMEApp.LONG_OPERATION.begin("Creating the view table - TEST", 
				new ViewCreation(new ViewCreationRule(Utils.loadText("rule.xml")), null)
		);
	}

	//-------------------------------------------------------------------------
	@Override
	public void finished() throws Exception {
		if (owner != null)
			owner.viewCreationFinished(getReq().getError());
	}

	//-------------------------------------------------------------------------
	/** 
	 * This method is responsible for writing out the rule, which was specified  
	 * to the constructor, to the Views database. It also creates or re-creates 
	 * the specified view table, reads data from the Results/Views databases as
	 * specified, processes them and writes out to the Views database.  
	 */
	// Model thread only
	@Override
	public void trun() throws Exception
	{
		String displayableErrMsg      = null;
		ArrayList<Object> inputTables = rule.getInputTables();

		Transaction t = new Transaction(getDBConnection());
		try { // catch errors and restore the saved title

			initialization(inputTables);

			t.start();

			deleteTMPTables();
			if (!isBasicMode) {
				filterPass(inputTables, whereCondition, null);
				blocks.noMoreBlocks();	// Dispose some associate tables to free memory
				mappingPass();
			}

			// Clear the initial data types before writing out the rule, because 
			// this type info should not be stored for PROJECTION columns.
			// During the initialization of tmp4cols[] these data types are copied, 
			// therefore this clearing is o.k. because it follows the initialization
			initTmp4cols();
			for (ThisviewPar p : lookupThisview.values()) {
				if (p.projectedColumn != null)
					p.col.setInitialDataType(null);
			}

			MEMEApp.LONG_OPERATION.setTaskName("Creating the view table...");
			MEMEApp.LONG_OPERATION.progress(-1, -1);
			displayableErrMsg = "Cannot create the view table due to a database error.";

			String desc = rule.getDescription();
			if (desc.length() == 0) desc = null;
			long view_id = MEMEApp.getViewsDb().createView(rule.getName(), desc, rule, tmp4cols);

			t.commit();
			t.start();

			displayableErrMsg = "The view table is unfinished due to a database error.";
			if (isBasicMode) {
				cntOfRows = rule.getGrouping() ? 1 : nFiltered;
				filterPass(inputTables, whereCondition, view_id);
			} else {
				aggregatePass(view_id);
			}

			int[] ordering = rule.getOrdering();
			if (!blocks.isSplitting() && ordering.length > 0) {
				MEMEApp.LONG_OPERATION.setTaskName("Sorting rows...");
				MEMEApp.LONG_OPERATION.progress(-1, -1);
				String orderBy = composeOrderBy(tmp4cols, ordering);
				view_id = MEMEApp.getViewsDb().reorderTable(view_id, orderBy);
			}
			t.commit();

		} catch (Exception e) {

			MEMEApp.logExceptionCallStack("ViewCreation.trun()", e);
			t.rollback();
			if (displayableErrMsg == null || e instanceof BSFException)
				displayableErrMsg = Utils.getLocalizedMessage(e);
			throw new Exception(displayableErrMsg + MEMEApp.seeTheErrorLog("\n%s %s"));

		} finally {
			try { 
				if (language != null && interp != null) {
					language.disposeInterp(interp);
					interp = null;
				}
				deleteTMPTables();
				System.gc(); System.gc();
			}
			catch (SQLException e) {
				MEMEApp.logExceptionCallStack("ViewCreation.deleteTMPTables()", e);
				SQLDialect().checkOutOfMemory(e);
			}
			// TODO: kitakaritani minel tobb objektumot hogy ha bennmaradna a memoriaban ez az
			// object akkor minel kevesebb helyet foglaljon!
		}
	}

	//-------------------------------------------------------------------------
	private void initialization(ArrayList<Object> inputTables) throws Exception
	{
		//nextGroupID		= 0;
		currentRowInfo	= null;

		// Collect column data types and count rows
		nRows = getNrOfRowsAndCollectColumns(inputTables);

		// Initialize computation (create PluginContext objects etc.) 
		//
		whereCondition = rule.getCondition();
		if (whereCondition != null && whereCondition.length() == 0) {
			whereCondition = null;
		}
		boolean needInterpreter = (whereCondition != null);

		blocks = new SplitManager();
		thisviewpars = new BitSet(allPars.size());
		ArrayList<Integer> groupingPars = new ArrayList<Integer>(lookupThisview.size());
		int idx = -1;

		NEXTPAR:
		for (ThisviewPar tp : lookupThisview.values()) {
			idx += 1;
			if (tp.col.isSplitter())		blocks.splitter.add(tp);
			if (tp.col.isSplitted())		blocks.splitted.add(tp);
			else if (!tp.col.isHidden())	blocks.notHiddenCnt += 1;
			if (tp.col.isGrouping())		groupingPars.add(idx);

			if (tp.idx >= 0) thisviewpars.set(tp.idx);

			ArrayList<Utils.Pair<SrcType, String>> sources = tp.col.getSource();
			ArrayList<Integer> inputPars = new ArrayList<Integer>(sources.size());
			// TODO: a script es plugin szamitasu oszlopoknal a String tipust helyesbiteni
			// kellene: String eseten VARCHAR(64) helyett LONGVARCHAR(?) legyen. A vegso 
			// tablaba valo atiras elott kellene kiszamolni a tenylegesen szukseges
			// szelesseget, es a vegleges tablat mar azzal hozni letre. 
			for (Utils.Pair<SrcType, String> src : sources) {
				ValuePar p = null;
				switch (src.getFirst()) {
					case PROJECTION_INPUT	: p = lookupInput.get(src.getSecond());		break;
					case PROJECTION_OUTPUT	: p = lookupOutput.get(src.getSecond());	break;
					case PROJECTION_VIEW	: p = lookupThisview.get(src.getSecond());
											  if (p != null && p.idx > tp.idx && tp.idx >= 0)
												  p = null;	// invalid reference to later column
											  break;
					case SCALAR_SCRIPT :
					case GROUP_SCRIPT :
						tp.script  = redirectOutput(src.getSecond()); // modified by Bocsi Rajmund (see bug #1436)
						needInterpreter = true;
						if (sources.size() != 1) {
							throw new IllegalArgumentException(String.format(
									"Error at column %s: invalid source columns", tp.getName() ));
						}
						if (src.getFirst() == SrcType.GROUP_SCRIPT)
							continue NEXTPAR;
						tp.scalar_script = true;
						p = tp;
						break;
				}
				if (p == null) {
					throw new IllegalArgumentException(String.format(
							"Error at column %s: cannot find source column %s", 
							tp.getName(), src.getSecond() ));
				}
				inputPars.add(p.idx);
			}
			String func = tp.col.getAggrFn();
			if (func == null || func.length() == 0) {
				if (inputPars.size() != 1) {
					throw new IllegalArgumentException(String.format(
							"Error at column %s: missing or too many source columns", tp.getName() ));
				}
				if (tp.script == null)
					tp.projectedColumn = allPars.get(inputPars.get(0));
			}
			else {
				tp.plugin = getAvailablePlugins().findByName(func);
				if (tp.plugin == null) {
					throw new IllegalArgumentException(String.format(
							"Error at column %s: unknown plugin %s", tp.getName(), func ));
				}
				// Ha bontott oszlopnal script+plugin van, akkor inputPars[] = { -1 }
				// (mivel bontott oszlopok nem szerepelnek allPars[]-ban, igy nincs
				//  indexuk). Ilyenkor halasztjuk a PluginContext letrehozasat
				// (tp.context==null marad)
				if (tp.script == null || inputPars.size() != 1 || inputPars.get(0) >= 0)
					tp.context = new PluginContext(tp.idx, Utils.asIntArray(inputPars));
			}
		}

		defaultOrdering = Utils.asIntArray(groupingPars);
		blocks.init(getDBConnection());

		isBasicMode = !blocks.isSplitting() && (!rule.getGrouping() 
							|| groupingPars.isEmpty() || groupingPars.size() == lookupThisview.size());

		if (needInterpreter) {
			String lang = SCRIPT_LANGUAGE;	// userPrefs?
			language = MEMEApp.getPluginManager().getScriptLangPluginInfos().findByLocalizedName(lang);
			if (language == null) {
				throw new UnsupportedOperationException(String.format(
						"Cannot find %s scripting plugin", lang));					
			}
			interp = new InterpContext();
			language.createInterp(interp);
		}
	}

	//-------------------------------------------------------------------------
	/**
	 * Pass#1: enumerate and load every rows and evaluate filter & scalar expressions
	 * + Prepare for grouping and splitting.
	 * @throws SQLException, UserBreakException, TooManyColumnsException,
	 * script evaluation: Exception
	 */
	private void filterPass(ArrayList<Object> inputTables, String whereCondition, Long view_id) throws Exception
	{
		ArrayList<ThisviewPar> projectedCols	= new ArrayList<ThisviewPar>();
		ArrayList<ThisviewPar> scalarComputed	= new ArrayList<ThisviewPar>();
		for (ThisviewPar tvp : lookupThisview.values()) {
			if (tvp.idx <= 0) continue;
			if (tvp.projectedColumn != null)
				projectedCols.add(tvp);
			else if (tvp.col.isScalar())
				scalarComputed.add(tvp);
		}
		int firstThisviewIdx = getFirstThisviewIdx();

		AppendView viewTableWriter = null;
		AggregateManager agm = null;
		boolean fakeBasic = false;
		if (view_id != null) {
			agm = new AggregateManager().init();
			// fakeBasic: az az eset, amikor az aggregacios oszlopokon kivul masmilyen nincsen,
			// es igy az osszes input sorbol 1 soros kimenetet akarnak kapni. Ilyenkor be kell
			// gyujtenunk a memoriaba az osszes input sort amit a filter atenged.
			fakeBasic = !agm.nonSplitted.isEmpty();
			if (fakeBasic) agm.newBlock(0);
			viewTableWriter = MEMEApp.getViewsDb().new AppendView();
			viewTableWriter.init(view_id, estimateCommitFreq(tmp4cols));
		}
		clearValues(true);

		MEMEApp.LONG_OPERATION.setTaskName("Filtering input rows and performing non-aggregate computation...");
		MEMEApp.LONG_OPERATION.progress(0, nRows);
		int progress = 0, rownr = 0;
		PreparedStatement ps = null;
		try {
		final InputRowsIterator inputRowsIterator = new InputRowsIterator(inputTables, this);
		for (GeneralRow row : inputRowsIterator)
		{
			MEMEApp.LONG_OPERATION.progress(progress++);

			// Load values from input tables
			loadInputValues(row);

			// Evaluate filter condition
			if (whereCondition != null) {
				assert(language != null && interp != null);
				interp.limitIdx = firstThisviewIdx;
				Object val = language.eval(interp, whereCondition);
				if (!Utils.getBooleanValue(val, false))
					continue;
			}

			// 'row' has passed through the filter condition  

			// Perform projections
			//
			for (ThisviewPar p : projectedCols) {
				p.setValue(p.projectedColumn.value, true);
			}

			// Perform computations
			// Note: a plugin may be applied to the result of the script
			//
			for (ThisviewPar p : scalarComputed) {
				if (p.script != null) {							// Script evaluation
					assert(language != null && interp != null);
					interp.limitIdx = p.idx + 1;	// a sajat oszlopot latja, pl. lekerheti a legutobbi erteket
					p.setValue(language.eval(interp, p.script));
				}
				if (p.plugin != null) {							// Plugin call
					assert(p.context != null);
					p.setValue(p.plugin.compute(p.context));
				}
			}

			if (view_id == null) {
				ps = populateTMP2(ps);
			} else if (fakeBasic) {
				agm.collectGroupingValues();
			} else {
				agm.newBlock(0);
				agm.collectGroupingValues();
				row = flushViewRow(agm, rownr++);
				if (row != null) {
					try {
						viewTableWriter.addRow(row);
					} catch (final ValueNotSupportedException e) {
						owner.warning();
					}
				}
				agm.clear();
			}
		}
		if (fakeBasic) {
			GeneralRow row = flushViewRow(agm, rownr++);
			if (row != null) {
				try {
					viewTableWriter.addRow(row);
				} catch (final ValueNotSupportedException e) {
					owner.warning();
				}
			}
			agm.clear();
		}
		if (viewTableWriter != null) { viewTableWriter.finish(); viewTableWriter = null; }
		} finally {
			if (viewTableWriter != null) viewTableWriter.dispose();
			release(ps);
			ps = null;
		}
		
		System.gc(); System.gc();
	}

	//-------------------------------------------------------------------------
	/** Assistant method for filterPass(). */
	@SuppressWarnings("cast")
	private PreparedStatement populateTMP2(PreparedStatement ps) throws SQLException, SplitManager.TooManyColumnsException {
		if (tmp2cols == null)	{ createTMP2(); commitFreq = estimateCommitFreq(tmp2cols); }
		if (tmp2cols.isEmpty())	return ps;

		int  blockNr = blocks.getBlockNr();
		long inputRef = (long)currentRowInfo.tableNr << 32 | ((long)currentRowInfo.rowID & LONG_LO);

		if (ps == null) {
			String cmd = "INSERT INTO " + TABLE_TMP2 + " VALUES (?,?" +		// InputRef,BlockNr
						 Utils.repeat(",?", tmp2cols.size(), "") + ')';
			ps = getDBConnection().prepareStatement(cmd);
		}
		int colIdx = 0;
		ps.setLong(++colIdx, inputRef);
		ps.setInt(++colIdx, blockNr);
		for (Parameter p : tmp2cols) {
			try {
				p.getDatatype().writeValue(((ValuePar)p).getSingleValue(), ps, ++colIdx);
			} catch (final ValueNotSupportedException e) {
				MEMEApp.logError(e.getMessage());
				owner.warning();
			}
		}
		ps.executeUpdate();
		nFiltered += 1;
		if ((nFiltered % commitFreq) == 0) getDBConnection().commit();
		return ps;
	}


	//-------------------------------------------------------------------------
	/** Pass#2: Mapping input rows to output cells. */ 
	private void mappingPass() throws SQLException, SplitManager.TooManyColumnsException, UserBreakException 
	{
		if (tmp2cols == null) {
			// populateGroups() egyszer sem hivodott meg mert a filter kifejezes
			// semmit sem engedett at
			return;
		}

		boolean isSplitting = blocks.isSplitting();
		String taskname = isSplitting? "Sorting rows..." : "Grouping rows...";

		MEMEApp.LONG_OPERATION.setTaskName(taskname);
		MEMEApp.LONG_OPERATION.progress(-1, -1);

		int ordering[] = isSplitting ? rule.getOrdering() : defaultOrdering.clone();
		ArrayList<ThisviewPar> orderPars = new ArrayList<ThisviewPar>(ordering.length);
		String sql = "";
		if (ordering.length > 0) {
			sql = composeOrderBy(tmp2cols, ordering) + ',';
			for (int i : ordering) orderPars.add((ThisviewPar)tmp2cols.get(i));
		}
		sql = sql + "InputRef";
		sql = "SELECT " + sql + (isSplitting ? ",BlockNr FROM " : " FROM ")
						+ TABLE_TMP2 + " ORDER BY " + sql;

		createTMP3();

		PreparedStatement ps = null;
		Statement st = null;
		try {
			st = getDBConnection().createStatement();
			st.setFetchSize(10000);
			ResultSet rs = st.executeQuery(sql);			// takes long time

			taskname = isSplitting ? "Mapping input rows to view table cells..." : taskname;

			MEMEApp.LONG_OPERATION.setTaskName(taskname);
			MEMEApp.LONG_OPERATION.progress(0, nFiltered);
			int progress = 0;

			ps = getDBConnection().prepareStatement("INSERT INTO " + TABLE_TMP3 + " VALUES (?,?)");
																				// InputRef, RowNr
			boolean first = true;
			int rowNr = -1;
			while (rs.next()) {
				MEMEApp.LONG_OPERATION.progress(progress++);

				boolean changed = first;
				first = false;
				int ci = 0;
				for (ThisviewPar p : orderPars) {
					Object val = p.getDatatype().readValue(rs, ++ci);
					changed = changed || !Utils.equals(p.getSingleValue(), val);
					if (changed) p.setValue(val);
				}
				if (changed) rowNr += 1;

				long inputRef = rs.getLong(++ci);
				if (isSplitting) {
					int blockNr = rs.getInt(++ci);
					blocks.block2values.get(blockNr).seenAt(rowNr);
				}

				ps.setLong(1, inputRef);
				ps.setInt(2, rowNr);
				ps.executeUpdate();
			}
			cntOfRows = rowNr;
			rs.close(); rs = null;
			ps.close(); ps = null;
			createTMP3Index();
			
			System.gc(); System.gc();
		} finally {
			release(st);
			release(ps);
		}
	}

	//-------------------------------------------------------------------------
	/**
	 * Pass#3: aggregate computations
	 * @throws SQLException, UserBreakException, script evaluation: Exception
	 */
	private void aggregatePass(long view_id) throws Exception {
		int					progress;
		AggregateManager	agm;
		Statement			st, st2;
		ResultSet			rs;
		int					lastRow = -1, lastBlock = -1;

		if (tmp2cols == null) {
			// populateGroups() egyszer sem hivodott meg mert a filter kifejezes
			// semmit sem engedett at
			return;
		}

		MEMEApp.LONG_OPERATION.setTaskName("Preparing for aggregate computation...");
		MEMEApp.LONG_OPERATION.progress(0, 0);

		AppendView viewTableWriter = MEMEApp.getViewsDb().new AppendView();

		agm = new AggregateManager().init();
		st = null; st2 = null;
		try {
			viewTableWriter.init(view_id, estimateCommitFreq(tmp4cols));

			String sql = "SELECT " + 
					TABLE_TMP3 + ".RowNr," +
					TABLE_TMP2 + ".BlockNr," +
					TABLE_TMP2 + ".InputRef" +
					Utils.join(','+TABLE_TMP2+'.', Utils.insert(tmp2cols.getSQLNames().toArray(), 0, "")) +
					" FROM " + TABLE_TMP2 + " JOIN " + TABLE_TMP3 + " ON (" +
					TABLE_TMP2 + ".InputRef=" + TABLE_TMP3 + ".InputRef)" +
					" ORDER BY " +	TABLE_TMP3 + ".RowNr, "+
									TABLE_TMP2 + ".BlockNr, " +
									TABLE_TMP2 + ".InputRef";
			st = getDBConnection().createStatement();
			st.setFetchSize(10000);
			rs = st.executeQuery(sql);

			MEMEApp.LONG_OPERATION.setTaskName("Performing aggregate computation and writing output...");
			MEMEApp.LONG_OPERATION.progress(0, nFiltered);
			progress = 0;

			while (rs.next()) {
				int ci = 0;
				int rownr = rs.getInt(++ci);
				int block = rs.getInt(++ci);
				long inputRef = rs.getLong(++ci);

				if (rownr != lastRow && lastRow != -1) {
					GeneralRow r = flushViewRow(agm, lastRow);	// implies clearValues(true);
					if (r != null) {
						try {
							viewTableWriter.addRow(r);
						} catch (final ValueNotSupportedException e) {
							owner.warning();
						}
					}
					agm.clear();
					lastBlock = -1;
				}
				lastRow = rownr;
				currentRowInfo = null;
				if (block != lastBlock) {
					lastBlock = block;
					agm.newBlock(block);
				}

				// Load values of non-aggregative thisview-columns
				for (Parameter p : tmp2cols) {
					allPars.get( ((ValuePar)p).idx )
								.setValue( p.getDatatype().readValue(rs, ++ci) );
				}

				// TODO: az egyenkenti beolvasast erdemes lenne kesleltetni flushView-ig. Addig csak
				// gyujtogetni kellene, hogy melyik sorokat kell beolvasni es melyiket hova
				// (agm.vectors hanyadik soraba). flushView elott aztan optimalizaltan kellene
				// beolvasni leheto legkevesebb adatbazismuvelettel, mert piszok lassu.
				//
				MEMEApp.LONG_OPERATION.progress(progress++);

				boolean collect = false;

				// tableNr egy jelzes is newView()/newResult() szamara, hogy ne bovitsek tables[]-t
				tableNr   = (int)(inputRef >> 32);	
				int rowid = (int)(inputRef & LONG_LO);
				Object o  = tables.get(tableNr);
				if (o instanceof ViewRec) {
					// view
					ViewRec rec = (ViewRec)o;
					for (GeneralRow r : MEMEApp.getViewsDb().getRows(rec.getViewID(), rowid, 1)) {
						newView(rec, r.getColumns());					// implies clearValues(false)
						loadInputValues(r);
						collect = true;
					}
				} else {
					// result
					Model m = (Model)o;
					Object[] rr = MEMEApp.getResultsDb().readOneRow(m.getModel_id(), rowid);
					if (rr != null && rr.length > 1 && rr[0] != null && rr[1] != null) {
						newResult((Result)rr[0]);						// implies clearValues(false)
						loadInputValues((GeneralRow)rr[1]);
						collect = true;
					}
				}
				if (collect) {
					agm.collectGroupingValues();
				}
			}
			System.gc(); System.gc();
			GeneralRow r = flushViewRow(agm, lastRow);
			if (r != null) {
				try {
					viewTableWriter.addRow(r);
				} catch (final ValueNotSupportedException e) {
					owner.warning();
				}
			}
			viewTableWriter.finish();

		} finally {
			rs = null;
			release(st);	st = null;
			release(st2);	st2 = null;
			viewTableWriter.dispose();
			System.gc(); System.gc();
		}
	}

	//-------------------------------------------------------------------------
	// Idonkent commit-olunk mert a HSQLDB a memoriaban tartja az uncommitted sorokat 
	// es ettol bizony betelhet a memoria. A commit-olas lassit, ezert minel ritkabban
	// csinaljuk (valahany soronkent). A gyakorisagot a sorok helyfoglalasatol tesszuk
	// fuggove, ehhez kell tudni hogy hany es milyen oszlopok vannak (cols)
	/** This methods estimates the frequency (in number of rows) of commiting. */
	private int estimateCommitFreq(Columns cols) {
		// Durvan becsuljuk hogy egy-egy sor mennyi memoriat foglal. Atlag 8 byte-ot
		// szamolunk oszloponkent. Ugy vettem eszre hogy ennek a becslesnek eleg keves
		// koze van a valosaghoz (amit itt 10M-nak becsulnek az a HSQLDB-ben 200M is
		// tud lenni) de talan megis jobb mint a semmi...
		// Ha COMMIT_FREQ-t kisebbre vesszuk akkor gyakrabban fog commit-elni. Erre
		// akkor lehet szukseg hogyha meg mindig tapasztalunk OutOfMemory problemakat.
		// 
		return Math.max(COMMIT_FREQ / ((cols.size()+1) * 8), 1);
	}

	//-------------------------------------------------------------------------
	// Elofeltetel: agm.vectors[]-ban benn van az osszes skalar adat ami ehhez a viewtabla-sorhoz kell
	// Feladat: elvegezni a vektoros szamolasokat. Elobb a nem-bontott oszlopokat, utana a bontottakat.
	private GeneralRow flushViewRow(AggregateManager agm, int rowNr) throws Exception 
	{
		// Lezarjuk a legutobbi blokkot. Ha ures volt, akkor nem kell csinalni semmit
		if (!agm.newBlock(null))
			return null;

		// Az eloirt sorrendben oszloprol oszlopra haladva elvegezzuk a szamitasokat.
		// Eloszor csak a nem-bontott oszlopokat. A bontottakat azert hagyjuk kesobbre,
		// mert azokra masfajta ciklus kell, es kulonben sem hivatkozhat rajuk mas.
		//
		for (ThisviewPar tp : agm.nonSplitted) {
			agm.aggregateCalculations(tp, null, rowNr);
		}

		for (SplitManager.SplittedPar spp : blocks.genSplCols) {
			spp.value = Utils.EMPTY_ARRAY;		// biztos ami ziher, kitakaritjuk
			if (!agm.isEmpty(spp.blockNr)) {
				agm.aggregateCalculations(spp.tvp, spp, rowNr);
			}
			// Ha nem volt olyan aggregacios muvelet ami skalar erteket eredmenyezett
			// volna, akkor csak a legutolso adatot tartjuk meg. Ezzel memoriat is
			// elengedunk.
			switch (spp.value.length) {
				case 0	: spp.value = ONE_NULL; break;
				case 1	: break;
				default	: spp.value = new Object[] { spp.value[spp.value.length - 1] }; break;
			}
		}

		// Az alabbi hivas az osszes parameternel lecsereli vektorokra a skalar ertekeket.
		// BASIC modban, amikor filterPass()-bol jon ide a vezerles, ez kerulendo, mert
		// ilyenkor a parameterek egy resze nem fog uj erteket kapni a kovetkezo input sor 
		// behuzasakor (ezert kell kihagyni alabb clearValues()-t is). A bennmaradt vektor  
		// ertekek ui. hibat okoznak, mert skalarokra szamitunk (pl. filter kifejezesben)
		if (!isBasicMode)
			agm.activateBlockRows(null);

		GeneralRow ans = new GeneralRow(tmp4cols);
		int i = 0;
		for (Parameter p : tmp4cols) {
			Object val;
			if (p instanceof SplitManager.SplittedPar)
				val = ((SplitManager.SplittedPar)p).value[0];
			else
				val = ((ThisviewPar)p).getSingleValue();
			ans.set(i++, val);
		}

		if (!isBasicMode)
			clearValues(true);
		return ans;
	}
	private static final Object[] ONE_NULL = { null };	// don't change its element


	//-------------------------------------------------------------------------
	/** Assistant method for filterPass(). */
	private void createTMP2() throws SQLException {
		tmp2cols = new Columns();
		for (ThisviewPar p : lookupThisview.values()) {
			if (p.col.isGrouping()) {
				tmp2cols.add(p);
				assert(!p.col.isSplitted() && p.idx >= 0);
			}
		}
		Statement st = null;
		try {
			String bigint = SQLDialect().getSQLType(java.sql.Types.BIGINT);
			st = getDBConnection().createStatement();
			st.execute(SQLDialect().createBigTable(TABLE_TMP2 + " (" +  
					"InputRef "+bigint+" NOT NULL," +
					"BlockNr SMALLINT NOT NULL"+
					tmp2cols.compose() + ')'
			));
			st.close(); st = null;
		} finally {
			release(st);
		}
	}

	//-------------------------------------------------------------------------
	/** Assistant method for mappingPass(). */
	private void createTMP3() throws SQLException {
		Statement st = null;
		try {
			String bigint = SQLDialect().getSQLType(java.sql.Types.BIGINT);
			st = getDBConnection().createStatement();
			st.execute(SQLDialect().createTmpTable(TABLE_TMP3 + " (" +
					"InputRef "+bigint+" NOT NULL," +
					"RowNr   INT NOT NULL)"
			));
			st.close(); st = null;
		} finally {
			release(st);
		}
	}

	//-------------------------------------------------------------------------
	/** Assistant method for mappingPass(). */
	private void createTMP3Index() throws SQLException {
		Statement st = null;
		try {
			st = getDBConnection().createStatement();
			st.execute("CREATE INDEX " + INDEX_TMP3 + " ON " + TABLE_TMP3 + " (InputRef)");
			st.close(); st = null;
		} finally {
			release(st);
		}
	}

	//-------------------------------------------------------------------------
	/** Assistant method for trun(). */
	private void initTmp4cols() {
		if (tmp4cols == null) {
			tmp4cols = new Columns();
			for (ThisviewPar p : lookupThisview.values()) {
				if (!p.col.isHidden() && !p.col.isSplitted())
					tmp4cols.add(p);
			}
			blocks.generateSplittedColNames(tmp4cols);
		}
	}

	//-------------------------------------------------------------------------
	/** Drops the temporary tables from the database.*/
	private void deleteTMPTables() throws SQLException, UserBreakException {
		MEMEApp.LONG_OPERATION.setTaskName("Cleaning temporary space...");
		MEMEApp.LONG_OPERATION.progress(0, 2);
		int pos = 0;

		SQLDialect sql = SQLDialect();
		sql.deleteTableIfExists(TABLE_TMP2);	MEMEApp.LONG_OPERATION.progress(++pos);
		sql.deleteTableIfExists(TABLE_TMP3);	MEMEApp.LONG_OPERATION.progress(++pos);
	}

	//-------------------------------------------------------------------------
	/*
	 * cols[]-bol kivalasztja az ordering[]-ben felsorolt indexeknek "megfelelo"
	 * parametereket (a megfeleltetes kozvetetten, a rule-beli oszlopok szerint
	 * tortenik), es visszaad egyreszt egy formazott stringet ORDER BY szamara, 
	 * masreszt a cols[]-beli indexeket ordering[]-ben.    
	 */
	/** Creates an ORDER BY command fragment. */
	private String composeOrderBy(Columns cols, int[] ordering) {
		ArrayList<ThisviewPar> tmp = new ArrayList<ThisviewPar>(lookupThisview.values());
		ArrayList<String> sqlnames = new ArrayList<String>(ordering.length);
		for (int k = 0; k < ordering.length; ++k) {
			int i = ordering[k];
			boolean desc = (i < 0);
			int j = cols.indexOf(tmp.get((desc) ? ~i : i));
			if (j < 0) {
				throw new IllegalArgumentException(String.format("Error in ordering: bad column reference %d", i));
			}
			ordering[k] = j;
			String s = cols.getSQLNames().get(j);
			if (desc) s = s + " DESC";
			sqlnames.add(s);
		}
		tmp = null;
		return Utils.join(sqlnames, ",");
	}

	//-------------------------------------------------------------------------
	public void newView(ViewRec view, Columns cols) {
		inputIdxMap.clear();
		outputIdxMap.clear();
		outputIdxMap.ensureCapacity(cols.size());
		for (Parameter p : cols) {
			outputIdxMap.add(lookupOutput.get(p.getName()).idx);
		}
		clearValues(false);
		ViewInfo info = (currentRowInfo instanceof ViewInfo) ? (ViewInfo)currentRowInfo : new ViewInfo();
		info.view 		= view;
		info.rowID		= -1;
		currentRowInfo	= info;
		if (tableNr == null) {
			tables.add(view);
			info.tableNr= tables.size() - 1;
		} else {
			info.tableNr= tableNr;
		}
	};

	//-------------------------------------------------------------------------
	public void newResult(Result res) {								// ok
		Model m = res.getModel();
		ResultInfo ri = (currentRowInfo instanceof ResultInfo) ? (ResultInfo)currentRowInfo : new ResultInfo();
		if (ri.result == null || !ri.getModel().equals(m)) {
			// New <model,version> table
			inputIdxMap.clear();
			inputIdxMap.ensureCapacity(res.getParameterComb().getNames().size());
			for (Parameter p : res.getParameterComb().getNames()) {
				inputIdxMap.add(lookupInput.get(p.getName()).idx);
			}
			outputIdxMap.clear();
			outputIdxMap.ensureCapacity(res.getOutputColumns().size());
			for (Parameter p : res.getOutputColumns()) {
				outputIdxMap.add(lookupOutput.get(p.getName()).idx);
			}
			clearValues(false);
			if (tableNr == null) {
				tables.add(m);
				ri.tableNr = tables.size() - 1;
			} else {
				ri.tableNr = tableNr;
			}
		}
		ri.result		= res;
		ri.tick			= -1;
		currentRowInfo	= ri;

		GeneralRow values = res.getParameterComb().getValues();
		assert(inputIdxMap.size() == values.size());
		for (int i = values.size() - 1; i >= 0; --i) {
			allPars.get(inputIdxMap.get(i)).setValue(values.get(i));
		}
	}

	//-------------------------------------------------------------------------
	/* 
	 * Az inputIdxMap es outputIdxMap-beli elemeket azert nem torli, mert azok
	 * ugyis kapnak uj erteket (ezaltal a changed flag nem all be, ha az uj
	 * ertek uaz mint a regi volt; ellenben ha elobb null-ra kitorolnenk es 
	 * ugy kapna ugyanazt az erteket, akkor beallna).
	 * A thisview parametereket amikor nem toroljuk, azt azert tesszuk, hogy
	 * megmaradjon bennuk a legutobbi sorbeli ertek (ezt 'clearThisviewPars'-ban
	 * kerni kell).
	 * A tobbit kinullazza. A thisview parameterek kozul csak azokat tudja
	 * kinullazni amik allPars-ban szerepelnek.
	 */
	/** Clears the values of the parameters. */
	private void clearValues(boolean clearThisviewPars) {					// ok
		BitSet clear = new BitSet();
		clear.set(0, allPars.size());
		for (Integer i : inputIdxMap)  clear.clear(i);
		for (Integer i : outputIdxMap) clear.clear(i);
		if (!clearThisviewPars) clear.andNot(thisviewpars);
		for (int i = clear.nextSetBit(0); i >= 0; i = clear.nextSetBit(i+1))
			allPars.get(i).clearValue();
	}


	//-------------------------------------------------------------------------
	/** Loads values from the row. */
	private void loadInputValues(GeneralRow row) {							// ok
		// Update current row location
		if (row instanceof Result.Row) {
			Result.Row	r = (Result.Row)row;
			ResultInfo	ri = (ResultInfo)currentRowInfo;
			ri.rowID= r.getRowID();
			ri.tick = r.getTick();
		}
		else {
			currentRowInfo.rowID += 1;
		}

		assert(outputIdxMap.size() == row.size());
		for (int i = row.size() - 1; i >= 0; --i) {
			allPars.get(outputIdxMap.get(i)).setValue(row.get(i));
		}
	}

	//-------------------------------------------------------------------------
	/** This class is used during nonscalar (aggregative) calculation only. */ 
	private class AggregateManager {
		// vectors[]: a view tabla 1 sorahoz tartozo osszes input sort reprezentalja
		// (nRows db sor) + a nem-bontott thisview parameterekhez tartozo sorokat.
		// Elemei vektorok, egy-egy parameternek (oszlopnak) felelnek meg. Az 
		// input&output oszlopok azonosan nRows magasak, a thisview oszlopok magassaga
		// pedig valtozoan vagy nRows vagy 1 vagy null: a grouping skalar oszlopok 
		// magassaga nRows; a non-grouping skalar oszlopoke kezdetben null, aztan
		// nRows; az aggregacios/vektorscriptes oszlopoke 1.
		// Az i. sor (i in [0..nRows-1]) mindegyik vektorban az i. elem.
		// A sorok a grouping-nak megfeleloen kovetik egymast: az egy csoportba
		// tartozo sorok 'i' indexei egy intervallumot alkotnak. Mivel 1 group = 1 block,
		// ezert ezzel a blokkokat is meg tudjuk fogni (-> 'blocks' map).
		ArrayList<Object>		vectors;		// az elemei lehetnek: null, Object[], ArrayList
		ArrayList<RowInfo>		savedRowInfo;	// minden sorhoz elmentjuk 'currentRowInfo' erteket is
		int						nRows = 0, nextRow;
		HashMap<Integer, int[]>	blocks = new HashMap<Integer, int[]>();
		Integer					currBlock;
		BitSet					groupingPars;				// these parameters define the groups
		BitSet					oneValue = new BitSet();	// have one common value computed from all input rows
		ArrayList<ThisviewPar>	nonSplitted;				// non-splitted aggregative calculation

		AggregateManager init() {
			int n		= allPars.size();
			groupingPars= new BitSet(n);
			groupingPars.set(0, n);
			nonSplitted = new ArrayList<ThisviewPar>();
			for (ThisviewPar p : lookupThisview.values()) {
				if (p.idx >= 0 && !p.col.isGrouping()) {
					groupingPars.clear(p.idx);
					nonSplitted.add(p);
				}
			}
			vectors = new ArrayList<Object>(Collections.nCopies(n, null));
			savedRowInfo = new ArrayList<RowInfo>();
			return this;
		}

		/** Prepares for a new block set (new row in the view table) */
		void clear() {
			Collections.fill(vectors, null);
			nRows = nextRow = 0;
			savedRowInfo.clear();
			blocks.clear();
			currBlock = null;
		}

		void collectGroupingValues() {
//			for (int i = groupingPars.nextSetBit(0); i >= 0; i = groupingPars.nextSetBit(i+1)) {
//				addParValue(i);
//			}
			
			// fixing Redmine bug #528
			// yeah, I know, it's disgusting
			for (int i = 0;i < allPars.size();++i) {
				boolean collect = false;
				if (groupingPars.get(i)) // grouping
					collect = true;
				else { // special case: scalar script and aggregate function
					final Column column = rule.findColumn(allPars.get(i).getName());
					if (column != null && column.hasScalarScriptTag() && column.getAggrFn().length() > 0) {
						collect = true;
						oneValue.set(i,false); // I don't know how, but this fix the assertion error and the result is fine
					}
				}
				
				if (collect)
					addParValue(i);
			}
			// end of fixing Redmine bug #528
			
			savedRowInfo.add(currentRowInfo.clone());
			nRows += 1;
		}

		@SuppressWarnings("unchecked")
		void addParValue(int idx) {
			ValuePar p = allPars.get(idx);
			assert(!oneValue.get(idx) && p.isSingleValue);

			Object o = p.getSingleValue();
			Object host = vectors.get(idx);
			if (host == null) {
				vectors.set(idx, new ArrayList(Collections.singletonList(o)));
			}
			else if (host instanceof Object[]) {
				List<Object> v, tmp = Arrays.asList( (Object[])host );
				v = new ArrayList(tmp.size() + 1);
				v.addAll(tmp);
				v.add(o);
				vectors.set(idx, v);
			}
			else {	
				// Csokkenthetjuk a memoriafoglalast ha az 'o' ugyanaz a String/szam 
				// mint a legutobbi (viszonylag gyakori eset): ekkor az uj helyett  
				// a regit hivatkozzuk be meg egyszer, igy az uj felszabadulhat.
				ArrayList<Object> v = (ArrayList<Object>)host;
				Object prev;
				if (!v.isEmpty() && (o instanceof String || o instanceof Number)
						&& o.equals(prev = v.get(v.size()-1))) {
					o = prev;
				}
				v.add(o);
			}
		}

		void setValue(int idx, Object value) {
			oneValue.set(idx);
			vectors.set(idx, new Object[] { value });
		}
		void setValueArray(int idx, Object[] value) {
			vectors.set(idx, value);
			assert(value == null || value.length == 1 || value.length == nRows);
		}

		Object[] getAsArray(int idx) {
			Object ans[], v = vectors.get(idx);
			if (v == null) {
				ans = new Object[] { null };
			}
			else if (v instanceof ArrayList) {
				ans = ((ArrayList)v).toArray();
				vectors.set(idx, ans);
			}
			else {
				ans = (Object[])v;
			}
			return ans;
		}
		
		Object[] getAsArray(int idx, Integer block) {
			Object[] tmp = getAsArray(idx);
			if (block == null) return tmp;
			int beginEnd[] = blocks.get(block);
			if (beginEnd == null || beginEnd[0] >= beginEnd[1]) 
				return Utils.EMPTY_ARRAY;
			Object ans[] = new Object[beginEnd[1] - beginEnd[0]];
			System.arraycopy(tmp, beginEnd[0], ans, 0, ans.length);
			return ans;
		}

		/** blockNr==null means: init the iteration for all rows of all blocks */
		void rewind(Integer block) {
			assert(!isEmpty(block));
			currBlock = block;
			nextRow = (currBlock == null) ? 0 : blocks.get(currBlock)[0];
		}

		/** Returns false if iteration is over, nothing happened */
		boolean activateNextRow() {
			assert(!isEmpty(currBlock));
			int lastRow = (currBlock == null) ? nRows : blocks.get(currBlock)[1];
			if (nextRow >= lastRow) {
				currBlock = null;
				return false;
			}
			for (int i = allPars.size() - 1; i >= 0; --i) {
				Object[] v = getAsArray(i);
				allPars.get(i).setValue( oneValue.get(i) ? v[0] : v[nextRow] );				
			}
			currentRowInfo = savedRowInfo.get(nextRow);
			nextRow += 1;
			currentRowInfo.setGroupLast(nextRow >= lastRow);
			return true;
		}

		/** blockNr==null means: activate all rows of all blocks */
		void activateBlockRows(Integer blockNr) {
			assert(!isEmpty(blockNr));
			if (nextRow == -1 && currBlock == blockNr)	// mar aktivalva van
				return;
			if (blockNr == null) {
				for (int i = vectors.size() - 1; i >= 0; --i) {
					Object[] v = getAsArray(i);
					if (oneValue.get(i)) allPars.get(i).setValue(v[0]);		// make it single value
					else allPars.get(i).setValue(v, false);				// make it array
				}
				currentRowInfo = savedRowInfo.isEmpty() ? null : savedRowInfo.get(savedRowInfo.size() - 1);
			} else {
				int beginEnd[] = blocks.get(blockNr);
				for (int i = vectors.size() - 1; i >= 0; --i) {
					Object[] v = getAsArray(i);
					if (oneValue.get(i)) {
						allPars.get(i).setValue(v[0]);
					} else {
						Object tmp[] = new Object[beginEnd[1] - beginEnd[0]];
						System.arraycopy(v, beginEnd[0], tmp, 0, tmp.length);
						allPars.get(i).setValue(tmp, false);
					}
				}
				currentRowInfo = (beginEnd[1] > savedRowInfo.size()) ? null : savedRowInfo.get(beginEnd[1]-1);
			}
			currentRowInfo = currentRowInfo.clone();
			currentRowInfo.clearFlags();
			currentRowInfo.setGroupLast(true);
			nextRow = -1;
			currBlock = blockNr;
		}

		/** 
		 * If blockNr==null closes the current group.
		 * It returns true if the closed group is not empty.
		 *  Note: 1 block = 1 group 
		 */ 
		boolean newBlock(Integer blockNr) {
			boolean ans = false;
			if (currBlock != null) {
				int[] be = blocks.get(currBlock);
				be[1] = nRows;
				ans = (be[0] < be[1]);
			}
			currBlock = blockNr;
			if (currBlock != null) {
				blocks.put(currBlock, new int[] { nRows, nRows });
			}
			return ans;
		}
		
		boolean isEmpty(Integer blockNr) {
			if (blockNr == null)
				return (nRows <= 0);
			int beginEnd[] = blocks.get(blockNr);
			return (beginEnd == null) || (beginEnd[0] >= beginEnd[1]);
		}

		/*
		 * Elofeltetel: vectors[]-ban benn van az osszes skalar adat, ami az 
		 * aktualis viewtabla-sorhoz kell, lezartak az utolso blokkot is, es
		 * az aktualis blokk nem ures. (Az aktualis blokk spp==null eseten
		 * az egesz view-tabla sor, spp!=null eseten pedig spp.blockNr).
		 *   
		 * Feladat: elvegezni a vektoros szamolasokat a 'tp' oszlopra. 
		 * Ha ez nem bontott oszlop, akkor spp==null. Ha bontott oszlop, 
		 * akkor spp!=null && tp==spp.tvp && tp.idx < 0.
		 */
		/** This method performs the vector computations with the column 'tp'. */
		void aggregateCalculations(ThisviewPar tp, SplitManager.SplittedPar spp, int rownr) throws Exception
		{
			Integer block = (spp != null) ? spp.blockNr : null;
			assert(!isEmpty(block));
			boolean blocklast, blockfirst;
			if (block == null) {
				blockfirst= (rownr == 0);
				blocklast = (rownr == cntOfRows-1);
			} else {
				SplitManager.BlockRec br = ViewCreation.this.blocks.block2values.get(block);
				blockfirst= (rownr == br.minRowNr);
				blocklast = (rownr == br.maxRowNr);
			}

			if (tp.script != null) {
				assert(language != null && interp != null);

				// a sajat oszlopot latja, pl. lekerheti a legutobbi erteket. -1: mindent lat
				interp.limitIdx = (spp != null) ? -1 : tp.idx + 1;

				if (tp.scalar_script) {
					ArrayList<Object> tmp = new ArrayList<Object>();
					rewind(block);
					boolean grpfirst = true;
					while (activateNextRow()) {
						currentRowInfo.setGroupFirst(grpfirst); grpfirst = false;
						currentRowInfo.setBlockFirst(blockfirst); blockfirst = false;
						currentRowInfo.setBlockLast(blocklast && (currentRowInfo.getFlags() & RowInfo.GROUP_LAST) != 0);

						Object res = language.eval(interp, tp.script);

						currentRowInfo.clearFlags();
						tmp.add(res);
						tp.setValue(res);
					}
					if (spp != null)
						spp.value = tmp.toArray();
					else
						setValueArray(tp.idx, tmp.toArray());
				} 
				else {												// vektoros script
					activateBlockRows(block);
					currentRowInfo.setBlockFirst(blockfirst);
					currentRowInfo.setBlockLast(blocklast);
					Object res = language.eval(interp, tp.script);	// az eredmenye skalar
					currentRowInfo.clearFlags();
					tp.setValue(res);
					if (spp != null)
						spp.value = new Object[] { res };
					else
						setValue(tp.idx, res);					// egyertekuve minositjuk ezt az oszlopot
				}
			}

			if (tp.projectedColumn != null) {
				if (oneValue.get(tp.projectedColumn.idx) || isBasicMode) {	// egyerteku oszlop projekcioja
					Object o = getAsArray(tp.projectedColumn.idx)[0];
					tp.setValue(o);
					if (spp != null)
						spp.value = new Object[] { o };
					else
						setValue(tp.idx, o);					// egyertekuve minositi ezt is
				} else {										// tobberteku oszlop projekcioja
					Object[] a = getAsArray(tp.projectedColumn.idx, block);
					tp.setValue(a, false);
					if (spp != null)
						spp.value = a; 
					else
						setValueArray(tp.idx, a);
				}
			}

			if (tp.plugin != null) {
				assert(tp.context != null || (spp != null && tp.script != null));
				activateBlockRows(block);
				currentRowInfo.setBlockFirst(blockfirst);
				currentRowInfo.setBlockLast(blocklast);
				PluginContext context = tp.context;
				if (tp.script != null) {
					if (spp != null)     tp.setValue(spp.value, false);
					if (context == null) context = new PluginContext2(tp, tp.value);
				}
				ColumnType restoreType = null;
				Object res;
				try {
					if (tp.script != null) {
						// Ha script is van, akkor a plugin hivas idejere atmenetileg  
						// at kell atallitani a datatype-ot a script eredmeny tipusara;
						// kulonben a plugin a sajat eredmeny tipusat latja input 
						// tipuskent, ami felrevezeto es hibat okozhat.
						restoreType = tp.getDatatype();
						tp.setDatatype(tp.col.getScriptDataType());
					}
					res = tp.plugin.compute(context);
				} finally {
					if (restoreType != null)
						tp.setDatatype(restoreType);
				}
				currentRowInfo.clearFlags();
				tp.setValue(res);
				if (spp != null)
					spp.value = new Object[] { res };
				else
					setValue(tp.idx, res);				// egyertekuve minositjuk ezt az oszlopot
			}
		}

	} // AggregateManager

	//-------------------------------------------------------------------------
	/** This class manages the splitting operations. */
	private static class SplitManager implements Utils.NameGenerator.IFinder
	{
		/** This class represents the splitted columns. */
		private static class SplittedPar extends Parameter {
			ThisviewPar			tvp;
			Object[]			value = Utils.EMPTY_ARRAY;
			Integer				blockNr;
			SplittedPar(String name, ThisviewPar p, Integer b)	{ super(name, null); tvp = p; blockNr = b; }
			@Override public ColumnType getDatatype()			{ return tvp.getDatatype(); }
		}
		/** This class represents the blocks. */
		private static class BlockRec {
			ArrayList<Object>	values;
			int					minRowNr = Integer.MAX_VALUE, maxRowNr = Integer.MIN_VALUE;
			BlockRec(ArrayList<Object> v) { values = v; }
			void seenAt(int r) { 
				if (r < minRowNr) minRowNr = r;
				if (maxRowNr < r) maxRowNr = r;
			}
		}

		ArrayList<ThisviewPar>	splitter = new ArrayList<ThisviewPar>();	// splitter columns
		ArrayList<ThisviewPar>	splitted = new ArrayList<ThisviewPar>();	// splitted columns
		ArrayList<SplittedPar>	genSplCols = new ArrayList<SplittedPar>();
		int						notHiddenCnt = 0;	// number of not-splitted and not-hidden columns
		int						MAX_BLOCKNR, maxCols;
		int						nextBlockNr = 0;

		LinkedHashMap<ArrayList<Object>, ArrayList<Integer>>
								values2blocks = new LinkedHashMap<ArrayList<Object>, ArrayList<Integer>>();
		ArrayList<BlockRec>		block2values = new ArrayList<BlockRec>();

		/** Precondition: the caller initialized splitter[],splitted[],notHiddenCnt */
		void init(java.sql.Connection conn) throws SQLException, TooManyColumnsException {
			maxCols = conn.getMetaData().getMaxColumnsInTable();
			if (DatabaseConnection.atLeast(maxCols, MAX_COLUMNS)) { // if maxCols >= MAX_COLUMNS
				// Bedrotozott korlat hogy values2blocks[] ne nohessen tul nagyra:
				maxCols = MAX_COLUMNS;
				// Megj: values2blocks[] vegett eleg volna csak MAX_BLOCKNR-t korlatozni
				// (eredetileg igy is volt) ez azonban kulonfele oszlopszamokat 
				// eredmenyezhetett a kimenetben 'splitted' elemszamatol fuggoen. 
				// Ezt a usernek nehez "megertenie" es megjegyeznie, ezert inkabb  
				// attertem az oszlopszam maximalasara.
			}
			MAX_BLOCKNR = maxCols - notHiddenCnt;
			if (!splitted.isEmpty()) {
				MAX_BLOCKNR /= splitted.size();	// max. number of blocks
				MAX_BLOCKNR -= 1;				// max. block index (starting from 0)
			}
			if (MAX_BLOCKNR < 0)
				throw new TooManyColumnsException(maxCols);
		}

		boolean isSplitting() {
			return (!splitter.isEmpty() && !splitted.isEmpty());
		}

		// Visszaadja a letezot vagy felvesz egy ujat. Konstans 0 ha tagolas ki van kapcsolva.
		/** Returns the existing block number or creates a new one. */
		Integer getBlockNr() throws TooManyColumnsException {
			if (!isSplitting())
				return 0;

			BlockRec br = new BlockRec(new ArrayList<Object>(splitter.size()));
			for (ThisviewPar p : splitter) br.values.add(p.getSingleValue());
			ArrayList<Integer> blocks = values2blocks.get(br.values);
			assert(blocks == null || !blocks.isEmpty());
			if (blocks == null) {
				blocks = new ArrayList<Integer>();
				Integer bnr = incrementBlockNr();
				blocks.add(bnr);
				values2blocks.put(br.values, blocks);
				Utils.ensureSize(block2values, bnr+1).set(bnr, br);
			}
			return blocks.get(blocks.size() - 1);
		}

		void noMoreBlocks() {
			values2blocks = null;
		}

		/* 
		 * Nemcsak a neveket, magukat az oszlopokat is generalja: feltolti genSplCols[]-t.
		 * Fontos, hogy a splitted oszlopok tmp4cols VEGERE kerulnek, espedig blokkonkent
		 * imsetlodo thisview-beli sorrendben: ez az alapja a Views lapon valo
		 * Columns.Parameter <-> ViewCreationRule.Column megfeleltetesnek
		 * (ld. {@link ViewsBrowser#displayableRule()}
		 */
		/** Generates the names of the splitted columns. It is also
		 *  generates the splitted columns.
		 */
		void generateSplittedColNames(Columns tmp4cols) {
			if (!isSplitting()) 
				return;

			Utils.NameGenerator ng = new Utils.NameGenerator();
			ng.maxlen(SQLDialect.MAX_COLNAME_LEN_IN_COLMAP);
			ng.defName("Column");
			ng.finder(this);

			ArrayList<String> strings = new ArrayList<String>(splitter.size());
			int blockNr = -1;
			for (BlockRec br : block2values) {
				blockNr += 1;
				strings.clear();
				// br.values[] a tagolo oszlopok aktualis ertekkombinaciojat tartalmazza.
				// Elemei Number, Boolean. String objektumok lehetnek, vagy null.
				for (Object o : br.values) strings.add(String.valueOf(o));
				for (ThisviewPar tvp : splitted) {
					String delim = tvp.col.getSplitted();
					assert(delim != null);
					ng.delimiter(delim);
					
					String name = tvp.getName();
					final int noPercentSigns = numberOfPercentSigns(name);
					if (noPercentSigns == 1) {
						name = Utils.join(strings, delim);
						name = ng.generate(tvp.getName().replace("%", name), tmp4cols);	// triggers this.findName()
					} else if (noPercentSigns >= strings.size()) {
						for (final String s : strings)
							name = name.replaceFirst("%",s);
						name = ng.generate(name,tmp4cols);
					} else
						throw new IllegalStateException("missing % sign");
					genSplCols.add(new SplittedPar(name, tvp, blockNr));
				}
			}
			tmp4cols.addAll(genSplCols);
		}
		
		//----------------------------------------------------------------------------------------------------
		private int numberOfPercentSigns(final String s) {
			int back = 0;
			int idx = s.indexOf('%');
			while (idx >= 0) {
				back++;
				if (idx == s.length() - 1) break;
				idx = s.indexOf('%',idx + 1);
			}
			return back;
		}

		/** Tests if 'name' is already in in use. userData is a Columns object. */ 
		public boolean findName(String name, Object userData) {
			Columns tmp4cols = (Columns)userData;
			return tmp4cols.indexOf(name) >= 0;
		}

		/** Increments the block number. */
		Integer incrementBlockNr() throws TooManyColumnsException {
			if (nextBlockNr >= MAX_BLOCKNR) throw new TooManyColumnsException(maxCols);
			return nextBlockNr++;
		}

		//-------------------------------------------------------------------------
		@SuppressWarnings("serial")
		static class TooManyColumnsException extends Exception {
			TooManyColumnsException(int maxCols) {
				super(String.format("Too many columns in the table (max. %d)", maxCols));
			}
		}
	}

	//-------------------------------------------------------------------------
	/** Counts the rows in the selected elements and collects column informations. */
	private int getNrOfRowsAndCollectColumns(ArrayList<Object> selection) throws UserBreakException
	{
		MEMEApp.LONG_OPERATION.setTaskName("Counting rows...");
		MEMEApp.LONG_OPERATION.progress(0, selection.size());
		int progress = 0, ans = 0;

		Columns input = new Columns();
		Columns output= new Columns();

		for (Object o : selection) {
			MEMEApp.LONG_OPERATION.progress(progress++);
			if (o instanceof Long[]) {
				Long[] spec = (Long[])o;
				Long[] tmp = new Long[spec.length - 1];
				System.arraycopy(spec, 1, tmp, 0, tmp.length);
				Columns io[] = MEMEApp.getResultsDb().getModelColumns(spec[0]);
				if (io != null) {
					input.merge(io[0], null);
					output.merge(io[1], null);
					ans += MEMEApp.getResultsDb().getNumberOfRows(spec[0], tmp);
				}
			}
			else if (o instanceof ViewRec) {
				ViewRec rec = (ViewRec)o;
				output.merge(MEMEApp.getViewsDb().getColumns(rec.getViewID()), null);
				ans += MEMEApp.getViewsDb().getNrOfRows(rec.getViewID());
			}
			else {
				assert(false) : o;
			}
		}

		// Set up allPars, lookup*, splitter, changedPars
		allPars.clear();

		lookupInput.clear();
		for (Parameter p : input) {
			ValuePar vp = new ValuePar(p.getName(), p.getDatatype(), allPars.size());
			allPars.add(vp);
			lookupInput.put(p.getName(), vp);
		}
		lookupOutput.clear();
		for (Parameter p : output) {
			ValuePar vp = new ValuePar(p.getName(), p.getDatatype(), allPars.size());
			allPars.add(vp);
			lookupOutput.put(p.getName(), vp);
		}
		lookupThisview.clear();
		for (ViewCreationRule.Column c : rule.getColumns()) {
			boolean splitted = c.isSplitted();
			ThisviewPar p = new ThisviewPar(c, splitted ? -1 : allPars.size());
			if (!splitted) {
				allPars.add(p);
			}
			lookupThisview.put(p.getName(), p);
		}
		changedPars = new BitSet(allPars.size());
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Returns the available view creation plugins. */
	private static PluginList<IVCPlugin> getAvailablePlugins() {
		return MEMEApp.getPluginManager().getVCPluginInfos();
	}

	//-------------------------------------------------------------------------
	private SQLDialect SQLDialect() {
		return MEMEApp.getDatabase().getSQLDialect();
	}

	//-------------------------------------------------------------------------
	private java.sql.Connection getDBConnection() {
		return MEMEApp.getDatabase().getConnection();
	}

	//-------------------------------------------------------------------------
	private int getFirstThisviewIdx() {
		return thisviewpars.nextSetBit(0);
	}

	//-------------------------------------------------------------------------
	// TODO: illene valahogy megoldani hogy nem-modosithato BitSet-et adjon
	// vissza, mert jelenleg az interpreter/plugin elmatathatjak amit itt elore
	// kiszamolunk es letarolunk.
	/** Returns the indexlist of the visible parameters. */ 
	private BitSet getVisibleParIndices(int limitIdx) {
		BitSet ans = null;
		boolean isNew = false;
		if (0 <= limitIdx && limitIdx < allPars.size()) {
			Object o = allPars.get(limitIdx);
			if (o instanceof ThisviewPar) {
				ThisviewPar tvp = (ThisviewPar)o;
				ans = tvp.visibleIndices;
				if (ans == null) {
					ans = tvp.visibleIndices = new BitSet(limitIdx);
					isNew = true;
				}
			}
		}
		if (ans == null) {
			ans = allVisible;
			if (ans == null) {
				ans = allVisible = new BitSet(allPars.size());
				isNew = true;
			}
		}
		if (isNew) {
			for (ValuePar vp : lookupOutput.values())
				ans.set(vp.idx);

			HashSet<String> tmp = new HashSet<String>(lookupThisview.size());
			for (ThisviewPar vp : lookupThisview.values()) {
				if (vp.idx >= limitIdx) break;
				if (vp.idx >= 0 && !lookupOutput.containsKey(vp.getName())) {
					ans.set(vp.idx);
					tmp.add(vp.getName());
				}
			}
			for (ValuePar vp : lookupInput.values()) {
				String name = vp.getName();
				if (!lookupOutput.containsKey(name) && !tmp.contains(name))
					ans.set(vp.idx);
			}
		}
		return ans;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String redirectOutput(String original) {
		if (original == null) return null;
		String tmp = original.replaceAll("System\\.out\\.println","sout");
		tmp = tmp.replaceAll("System\\.err\\.println","sout");
		return tmp;
	}

	//-------------------------------------------------------------------------
	/** 
	 * Type for arguments of IVCPlugin methods.
	 */
	@SuppressWarnings("serial")
	private class PluginContext extends PluginContextBase implements IVCPlugin.IContext
	{
		final int		limitIdx;
		final int[]	selectedIndices;

		PluginContext(int limitIdx, int[] sel) {
			this.limitIdx = limitIdx;
			selectedIndices = sel;
		}

		public BitSet getVisibleParIndices() {
			return ViewCreation.this.getVisibleParIndices(limitIdx);
		}

		public int indexOf(String parName) {
			ValuePar vp = lookupOutput.get(parName);
			if (vp != null) return vp.idx;
			vp = lookupThisview.get(parName);
			if (vp != null && vp.idx < limitIdx && vp.idx >= 0) return vp.idx;
			vp = lookupOutput.get(parName);
			return (vp == null) ? -1 : vp.idx;
		}

		public List<? extends Parameter>	getAllPars()		{ return Collections.unmodifiableList(allPars); }
		public List<Object[]>				getAllValues()		{ return allValues; }
		public List<? extends Parameter>	getArgumentPars()	{ return new IndexedPars(selectedIndices); }
		public List<Object[]>				getArguments()		{ return new IndexedValues(selectedIndices); }
	}

	//-------------------------------------------------------------------------
	// Designed for splitted columns
	/** 
	 * Type for arguments of IVCPlugin methods. Designed for splitted columns.
	 */
	@SuppressWarnings("serial")
	private class PluginContext2 extends PluginContext {
		List<? extends Parameter>	pars;
		List<Object[]>				values;
		PluginContext2(Parameter par, Object[] values) {
			this(Collections.singletonList(par), Arrays.asList(new Object[][] { values }));
		}
		PluginContext2(List<? extends Parameter> pars, List<Object[]> values) {
			super(-1, null);
			this.pars   = pars;
			this.values = values;
		}
		@Override public List<? extends Parameter>	getArgumentPars()	{ return pars; }
		@Override public List<Object[]>				getArguments()		{ return values; }
	}


	//-------------------------------------------------------------------------
	/** 
	 * Type for arguments of IScriptingLanguage methods.
	 */
	@SuppressWarnings("serial")
	private class InterpContext extends PluginContextBase implements IScriptLanguage.IContext
	{
		int					limitIdx = getFirstThisviewIdx();	// by default no thisview parameter is visible
		boolean				scalarMode = true;

		public IScriptLanguage.IRowInfo			getCurrentRowInfo()	{ return currentRowInfo; }
		public ArrayList<? extends Parameter>	getAllPars()		{ return allPars; }
		public List<Object[]>					getAllValues()		{ return allValues; }
		public boolean	 isSingleValue(int idx) { return allPars.get(idx).isSingleValue; }

		public BitSet getVisibleParIndices() {
			return ViewCreation.this.getVisibleParIndices(limitIdx);
		}

		public BitSet getChangedParIndices() {
			BitSet ans = (BitSet)changedPars.clone();
			changedPars.clear();
			return ans;
		}

		public Category getCategory(Parameter p) {
			if (p instanceof ThisviewPar)			return Category.THISVIEW;
			if (lookupOutput.get(p.getName()) == p)	return Category.OUTPUT;
			if (lookupInput.get(p.getName()) == p)	return Category.INPUT;
			return null;
		}

		public Object callAggrFn(String pluginType, final List<Object[]> args) throws NoSuchMethodException {
			IVCPlugin plugin = getAvailablePlugins().findByName(pluginType);
			if (plugin == null) {
				plugin = getAvailablePlugins().findByLocalizedName(pluginType);
			}
			if (plugin == null && !pluginType.matches(".*\\(\\)$")) {
				plugin = getAvailablePlugins().findByLocalizedName(pluginType + "()");
			}
			if (plugin == null) {
				throw new NoSuchMethodException(String.format("Cannot find plugin %s", pluginType));
			}

			IVCPlugin.IContext ctx = new PluginContext(limitIdx, null) {
				ArrayList<Parameter> pars;
				@Override public List<Object[]>	 getArguments() { return args; }
				
				@Override
				public List<? extends Parameter> getArgumentPars() {
					// Kideritjuk az argumentumok tipusat: ures vektor tipusa String,
					// null-al kezdodoe szinten, a nem-ureseknel az elso elemet vizsgaljuk 
					// meg. Ha String tipusu, akkor toString()-et is meghivjuk a string
					// hosszanak beallitasahoz, de csak novelni engedjuk a default hosszot.
					if (pars == null) {
						List<Object[]> ar = getArguments();
						pars = new ArrayList<Parameter>(ar.size());
						int i = 0;
						for (Object[] v : ar) {
							String name = String.format("arg%02d", i++);
							ColumnType t = ColumnType.STRING;
							if (v.length > 0 && v[0] != null) {
								t = ColumnType.convertJavaClass(v[0].getClass());
								if (t == ColumnType.STRING) {
									ColumnType.TypeAndValue tv = ColumnType.parseObject(v[0]);
									if (tv.getType().getJavaClass() == String.class
											&& tv.getType().a > t.a) t = tv.getType();
								}
							}
							pars.add(new Parameter(name, t));
						}
					}
					return pars;
				}
			};
			return plugin.compute(ctx);
		}

	}

	//-------------------------------------------------------------------------
	private class IndexedValues extends AbstractList<Object[]> implements RandomAccess {
		final int indices[];
		IndexedValues(int indices[])				{ this.indices = indices; }
		@Override public int size()				{ return indices.length; }
		@Override public Object[] get(int index)	{ return allPars.get(indices[index]).value; }
		@Override public Object[] set(int index, Object[] element) {
			return allValues.set(indices[index], element);
		}
	}

	//-------------------------------------------------------------------------
	private class IndexedPars extends AbstractList<ValuePar> implements RandomAccess {
		final int indices[];
		IndexedPars(int indices[])					{ this.indices = indices; }
		@Override public int size()				{ return indices.length; }
		@Override public ValuePar get(int index)	{ return allPars.get(indices[index]); }
	}

	//-------------------------------------------------------------------------
	private static class AllValues extends AbstractList<Object[]> implements RandomAccess {
		ArrayList<ValuePar> allPars;
		AllValues(ArrayList<ValuePar> all)			{ allPars = all; }

		@Override public int size()				{ return allPars.size(); }
		@Override public Object[] get(int index)	{ return allPars.get(index).value; }
		@Override public Object[] set(int index, Object[] element) {
			ValuePar p = allPars.get(index);
			if (element != null && element.length != p.value.length) p.invalidLength();
			return p.setValue(element, p.isSingleValue);
		}
	}

	//-------------------------------------------------------------------------
	/** Class for representing rows for the scripting language plugins. */
	private static class RowInfo implements IScriptLanguage.IRowInfo, Cloneable {
		int flags = 0;
		int	tableNr, rowID;

		void	setGroupFirst(boolean b)	{ flags = (flags & ~GROUP_FIRST) | (b ? GROUP_FIRST : 0); } 
		void	setGroupLast(boolean b)		{ flags = (flags & ~GROUP_LAST)  | (b ? GROUP_LAST  : 0); } 
		void	setBlockFirst(boolean b)	{ flags = (flags & ~BLOCK_FIRST) | (b ? BLOCK_FIRST : 0); } 
		void	setBlockLast(boolean b)		{ flags = (flags & ~BLOCK_LAST)  | (b ? BLOCK_LAST  : 0); }
		void	clearFlags()				{ flags = 0; }

		public int		getFlags()			{ return flags; }

		@Override public RowInfo clone() {
			try { return (RowInfo)super.clone(); }
			catch (CloneNotSupportedException e) { return null; }
		}
	}
	/** Class for representing information about results rows for the scripting language plugins. */
	private static class ResultInfo extends RowInfo implements IScriptLanguage.IResultInfo
	{
		Result	result;
		int		tick;
		public Model	getModel()		{ return result.getModel(); }
		public int		getBatch()		{ return result.getBatch(); }
		public int		getRun()		{ return result.getRun(); }
		public int		getTick()		{ return tick; }
		public long	getStartTime()	{ return result.getStartTime(); }
		public long	getEndTime()	{ return result.getEndTime(); }
	}
	/** Class for representing information about views rows for the scripting language plugins. */
	private static class ViewInfo extends RowInfo implements IScriptLanguage.IViewInfo {
		ViewRec	view;
		public ViewRec	getView()		{ return view; }
		public int getRowID()			{ return rowID; }
	}
}
