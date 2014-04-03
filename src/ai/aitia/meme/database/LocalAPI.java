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
package ai.aitia.meme.database;

import static ai.aitia.meme.database.Columns.quote;
import static ai.aitia.meme.database.SQLDialect.release;
import static ai.aitia.meme.utils.Utils.append;
import static ai.aitia.meme.utils.Utils.join;
import static ai.aitia.meme.utils.Utils.repeat;

import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeMap;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.ColumnType.ValueNotSupportedException;
import ai.aitia.meme.database.SQLDialect.SQLState;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.viewmanager.ViewCreationRule;
import ai.aitia.visu.globalhandlers.UserBreakException;

public class LocalAPI extends AbstractResultsDb implements IConnChangedListener 
{
	//=========================================================================
	// Member variables
	private static final int FIRST_BATCH = 1;
	private static final int FIRST_RUN   = 1;

	//=========================================================================
	// Getter/setter methods

	//=========================================================================
	// Interface methods

	//---------------------------------------------------------------------
	public Model findModel(String name, String version) {
		Model ans = null;
		PreparedStatement ps = null;
		try {
	    	ps = getConnection().prepareStatement(
	    			"SELECT Model_id FROM MODELS WHERE Name=? AND Version=?");
	    	ps.setString(1, name);			// Columns are numbered from 1
	    	ps.setString(2, version);
	    	ResultSet rs = ps.executeQuery();
	    	if (rs.next())
	    		ans = new Model(rs.getLong(1), name, version);
	    	rs.close();
	    	ps.close(); ps = null;
		} catch (SQLException e) {
			MEMEApp.logException("LocalAPI.findModel()", e);
			dialect().checkOutOfMemory(e);
		} finally {
			release(ps);
		}
		return ans;
	}
	
	//---------------------------------------------------------------------
	@Override
	public Model findModel(long model_id) {
		Model ans = null;
		Statement st = null;
		try {
			st = getConnection().createStatement();
			ResultSet rs = findModel(model_id,st);
			if (rs != null) ans = new Model(rs.getLong(1),rs.getString(2),rs.getString(3));
			rs.close();
			st.close(); st = null;
		} catch (SQLException e) {
			MEMEApp.logException("LocalAPI.findModel()", e);
			dialect().checkOutOfMemory(e);
		} finally {
			release(st);
		}
		return ans;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean renameModel(final long model_id, final String old_name, final String old_version, final String new_name, final String new_version) {
		if (model_id < 0 || old_name == null || old_name.trim().length() == 0 || old_version == null || old_version.trim().length() == 0 ||
			new_name == null || new_name.trim().length() == 0 || new_version == null || new_version.trim().length() == 0)
			throw new IllegalArgumentException();
		
		boolean result = true;
		Statement st = null;
		PreparedStatement ps = null;
		Transaction t = null;
		try {
			t = new Transaction(getConnection());
			t.start();
			ps  = getConnection().prepareStatement("UPDATE Models SET Name=?, Version=?" +
												   " WHERE Model_id=?");
			ps.setString(1,new_name);
			ps.setString(2,new_version);
			ps.setLong(3,model_id);
			ps.executeUpdate();
			ps.close(); ps = null;

	    	st = getConnection().createStatement();
	    	ResultSet rs = st.executeQuery("SELECT View_id, Creation_rule FROM VIEWS ORDER BY View_id");

	    	final int c_id  = rs.findColumn("View_id");
	    	final int c_rule = rs.findColumn("Creation_rule");

	    	while (rs.next()) {
	    		final ViewCreationRule _rule = new ViewCreationRule(rs.getString(c_rule));
	    		_rule.changeReference(old_name,old_version,new_name,new_version);
	    		
	    		ps = getConnection().prepareStatement("UPDATE VIEWS SET Creation_rule=? WHERE" +
	    											  " View_Id=?");
	    		ps.setString(1,_rule.toString());
	    		ps.setLong(2,rs.getLong(c_id));
	    		ps.executeUpdate();
	    		ps.close(); ps = null;
	    	}
	    	rs.close();
	    	st.close(); st = null;
				
	   		t.commit();
	   		final Model m = findModel(model_id);
	   		resultsDbChanged.fire(new ResultsDbChangeEvent(this,m,true));
		} catch (final Exception e) {
			result = false;
			MEMEApp.logException("LocalAPI.renameModel()",e);
			if (e instanceof SQLException)
				MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory((SQLException)e);
			if (t != null) {
				try {
					t.rollback();
				} catch (SQLException e1) {
					MEMEApp.logException("LocalAPI.renameModel()", e1);
					MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e1);
				}
			}
		} finally {
			release(st);
			release(ps);
		}
		return result;
	}

	//---------------------------------------------------------------------
	public int getNewBatch(Model m) {
		int ans = FIRST_BATCH; 
		if (m != null) {
			int batch[] = { ans }, run[] = { 0 };
			getNewBatchAndRun(m.getModel_id(), batch, run);
			ans = batch[0];
		}
		return ans;
	}

	//---------------------------------------------------------------------
	public void addResult(Result result) throws Exception {
		if (result == null) throw new NullPointerException();
		if (result.getModel() == null
				|| result.getParameterComb() == null
				|| result.getParameterComb().size() < 0
				|| result.getOutputColumns() == null
				|| result.getOutputColumns().size() <= 0)
			throw new IllegalArgumentException();

		Transaction t = new Transaction(getConnection());
		try {	// catch Exceptions, finally commit or rollback
			t.start();
			Model m = findModel(result.getModel().getName(), result.getModel().getVersion());
	    	if (m == null) {
	    		// New model: create it
	    		createModel(result);
	    	}
	    	else {
	    		// The model already exists: update user fields
	    		appendModel(result, m.getModel_id());
	    	}
	    	t.commit();
		} catch (final ValueNotSupportedException e) {
			t.commit();
			throw e;
		} catch (Exception e) {
			MEMEApp.logException("LocalAPI.addResult()", e);
			throw e;
		} finally {
			t.rollback();
			resultsDbChanged.fire(new ResultsDbChangeEvent(this, ChangeAction.ADDED, result));
		}
	}

    //---------------------------------------------------------------------
	@Override
	public List<Model> getModelsAndVersions() {
		ArrayList<Model> ans = new ArrayList<Model>();
		Statement st = null;
		try {
	    	st = getConnection().createStatement();
	    	ResultSet rs = st.executeQuery("SELECT * FROM MODELS ORDER BY Name, Version");

	    	int c_model_id = rs.findColumn("Model_id");
	    	int c_name     = rs.findColumn("Name");
	    	int c_version  = rs.findColumn("Version");

	    	while (rs.next()) {
	    		ans.add(new Model(rs.getLong(c_model_id),
	    				          rs.getString(c_name),
	    				          rs.getString(c_version) ));
	    	}
	    	rs.close();
		} catch (SQLException e) {
			MEMEApp.logException("LocalAPI.getModelsAndVersions()", e);
			dialect().checkOutOfMemory(e);
		} finally {
			release(st);
		}
		return ans;
	}

	//---------------------------------------------------------------------
	@Override String getDescription(long model_id) {
		String ans = null;
		Statement st = null;
		try {
			st = getConnection().createStatement(); 
			ResultSet rs = findModel(model_id, st);
			ans = (rs == null) ? null : rs.getString("Description");
			st.close(); st = null;
		} catch (SQLException e) {
			MEMEApp.logException("LocalAPI.getDescription()", e);
			dialect().checkOutOfMemory(e);
		} finally {
			release(st);
		}
		return ans;
	}

    //---------------------------------------------------------------------
	@Override void writeDescription(Model m) {
		try {
			writeDescription(m, m.getModel_id());
			resultsDbChanged.fire(new ResultsDbChangeEvent(this, m, m.getDescription()));
		} catch (SQLException e) {
			MEMEApp.logException("LocalAPI.writeDescription()", e);
			dialect().checkOutOfMemory(e);
		}
	}

    //---------------------------------------------------------------------
	@Override
	public String getDescription(long model_id, int batch) {
		String ans = null;
		Statement st = null;
		try {
			st = getConnection().createStatement();
	    	ResultSet rs = st.executeQuery("SELECT Description FROM BATCH_DESCRIPTION" + 
	    										" WHERE Model_id=" + model_id + " AND Batch=" + batch);
	    	if (rs.next())
	    		ans = rs.getString(1);
	    	st.close(); st = null;
		} catch (SQLException e) {
			MEMEApp.logException("LocalAPI.getDescription(model,batch)", e);
			dialect().checkOutOfMemory(e);
		} finally {
			release(st);
		}
		return ans;
	}

	//---------------------------------------------------------------------
	public void storeBatchDescription(long model_id, int batch, String s) {
		Statement st = null;
		PreparedStatement ps = null;
		try {
			st = getConnection().createStatement();

			// Verify model_id
			ResultSet rs = findModel(model_id, st);
			if (rs == null)
				throw new IllegalArgumentException("setBatchDescription(): non-existing model_id " + model_id);
			Model m = new Model(model_id, rs.getString("Name"), rs.getString("Version")); 			
			rs.close();

			// Verify batch
	    	rs = st.executeQuery("SELECT Batch FROM RESULTS_INPUT_" + model_id +
									" WHERE Batch=" + batch +
    								" LIMIT 1");
	    	if (!rs.next())
				throw new IllegalArgumentException("setBatchDescription(): non-existing batch# " + batch);
	    	rs.close();

	    	// Write string
	    	if (s == null || s.length() == 0) {
				ps = getConnection().prepareStatement("DELETE FROM BATCH_DESCRIPTION" +
										" WHERE Model_id=" + model_id + " AND Batch=" + batch);
				ps.executeUpdate();
	    	} else {							// First try to modify - if exists
				ps = getConnection().prepareStatement("UPDATE BATCH_DESCRIPTION SET Description = ?" +
										" WHERE Model_id=" + model_id + " AND Batch=" + batch);
				ps.setString(1, s);
				if (0 == ps.executeUpdate()) {	// no such row - insert it
					ps = getConnection().prepareStatement("INSERT INTO BATCH_DESCRIPTION (Model_id, Batch, Description)" +
										" VALUES (?,?,?)" );
					ps.setLong  (1, model_id);				// Columns are numbered from 1
					ps.setInt   (2, batch);
					ps.setString(3, s);
					ps.executeUpdate();
				}
	    	}
			ps.close();
			resultsDbChanged.fire(new ResultsDbChangeEvent(this, m, batch, s));

		} catch (IllegalArgumentException e) {
			throw e;
		} catch (SQLException e) {
			MEMEApp.logException("LocalAPI.setBatchDescription()", e);
			dialect().checkOutOfMemory(e);
		} finally {
			release(st);
			release(ps);
		}
	}

    //---------------------------------------------------------------------
	@Override
	List<BatchInfo> getBatchInfoImpl(long model_id, Integer batch) {
		ArrayList<BatchInfo> ans = new ArrayList<BatchInfo>();
		Statement st = null;
		try {
			TreeMap<Integer, Utils.Pair<Integer, String>> tmp = new TreeMap<Integer, Utils.Pair<Integer, String>>();
			String qry = "";
			if (batch != null)
				qry = " WHERE Batch=" + batch;

			qry = "SELECT Batch, COUNT(Run) from RESULTS_INPUT_" + model_id + qry + " GROUP BY Batch";

			st = getConnection().createStatement();
	    	ResultSet rs = st.executeQuery(qry);				
	    	while (rs.next()) {
	    		tmp.put(rs.getInt(1), new Utils.Pair<Integer, String>(rs.getInt(2), ""));
	    	}
	    	rs.close();

	    	qry = "SELECT Batch, Description FROM BATCH_DESCRIPTION WHERE Model_id=" + model_id;
	    	if (batch != null)
	    		qry = qry + " AND Batch=" + batch;

			rs = st.executeQuery(qry);
	    	while (rs.next()) {
	    		Integer b = rs.getInt(1);
	    		Utils.Pair<Integer, String> v = tmp.get(b);
	    		if (v != null)
	    			v.setValue(rs.getString(2));
	    	}
	    	rs.close();
	    	st.close(); st = null;

	    	ans.ensureCapacity(tmp.size());
	    	for (java.util.Map.Entry<Integer, Utils.Pair<Integer, String>> entry : tmp.entrySet()) {
	    		ans.add(new BatchInfo(entry.getKey(), entry.getValue().getKey(), entry.getValue().getValue()));
	    	}

		} catch (SQLException e) {
			MEMEApp.logException("LocalAPI.getDescription(model,batch)", e);
			dialect().checkOutOfMemory(e);
		} finally {
			release(st);
		}
		return ans;
	}

	
    //---------------------------------------------------------------------
	/**
	 * Note: The Columns list of input parameter names is SHARED 
	 *       amongst the returned Result objects!
	 *       Therefore be careful if you add, remove or rename
	 *       any input parameter! (Clone the Columns object before!) 
	 */
	@Override
	public List<Result> getResults(long model_id, Long[] batchAndRun) {
		ArrayList<Result> ans = new ArrayList<Result>();
		Statement st = null;
		try {
	    	st = getConnection().createStatement();

	    	ResultSet rs = st.executeQuery("SELECT * FROM MODELS WHERE Model_id=" + model_id);
	    	if (!rs.next()) {
	    		rs = null;
	    		release(st);
	    		return ans;			// no such model
	    	}
	    	Model model = new Model(model_id, rs.getString("Name"), rs.getString("Version"));
	    	rs.close();

	    	ColumnOrder icols = new ColumnOrder();
	    	ColumnOrder ocols = new ColumnOrder();

	    	icols.read(model_id, "RESULTS_I_COLUMNS", st);
	    	ocols.read(model_id, "RESULTS_O_COLUMNS", st);   	

	    	rs = st.executeQuery("SELECT * FROM RESULTS_OUTPUT_" + model_id + " WHERE 1=0");
	    	ocols.reorder(rs);
	    	rs.close();

	    	String q = "SELECT * FROM RESULTS_INPUT_" + model_id;
	    	if (batchAndRun == JUST_ANY_ONE) {
	    		q = q + " WHERE 1=1 LIMIT 1";
	    	}
	    	else if (batchAndRun != null && batchAndRun.length > 0) {
	    		q = q + " WHERE Batch=" + batchAndRun[0];
	    		if (batchAndRun.length > 1)
	    			q = q + " AND Run=" + batchAndRun[1];
	    		else
	    			q = q + " ORDER BY Run";
	    	}
	    	else {
	    		q = q + " ORDER BY Batch, Run";
	    	}
	    	st.setFetchSize(10000);
	    	rs = st.executeQuery(q);
	    	int c_batch = rs.findColumn("Batch");
	    	int c_run   = rs.findColumn("Run");
	    	int c_time  = rs.findColumn("Timestamp");
	    	icols.reorder(rs);
	    	while (rs.next()) {
	    		int  batch = rs.getInt(c_batch);
	    		int  run   = rs.getInt(c_run);
    			long time  = rs.getTimestamp(c_time).getTime();

	    		ans.add(new ResultInDb(batch, run, time, 0, model, new ParameterComb(icols.readRow(rs)), ocols));
	    	}
	    	rs.close();
	    	st.close(); st = null;
		} catch (SQLException e) {
			MEMEApp.logExceptionCallStack("LocalAPI.getResults()", e);
			dialect().checkOutOfMemory(e);
		} finally {
			release(st);
		}
		return ans;
	}

	//---------------------------------------------------------------------
	@Override
	public int getNumberOfRows(long model_id, Long[] batchAndRun) {
		int ans = 0, len = (batchAndRun == null) ? 0 : batchAndRun.length;
		StringBuilder cmd = Utils.append(null, "SELECT COUNT(*) FROM RESULTS_OUTPUT_", model_id);
		if (len > 0) cmd = Utils.append(cmd, " WHERE Batch=", batchAndRun[0]);
		if (len > 1) cmd = Utils.append(cmd, " AND Run=", batchAndRun[1]);
		Statement st = null;
		try {
	    	st = getConnection().createStatement();
	    	ResultSet rs = st.executeQuery(cmd.toString());
	    	if (rs.next())
	    		ans = rs.getInt(1);		// Columns are numbered from 1
	    	rs.close();
	    	st.close(); st = null;
		} catch (SQLException e) {
			MEMEApp.logException("LocalAPI.getNumberOfRows()", e);
			dialect().checkOutOfMemory(e);
		} finally {
			release(st);
		}
		return ans;
	}

	//---------------------------------------------------------------------
	@Override
	public void deleteAll(Long[][] selection) throws SQLException {
		if (selection == null || selection.length == 0)
			return;

		SQLState e = SQLState.BaseTableNotFound_Drop;
		Statement st = null;
		Transaction t = new Transaction(getConnection());
		t.start();
		try {
			st = getConnection().createStatement();
			LinkedHashSet<Model> wholeModels  = new LinkedHashSet<Model>();
			LinkedHashSet<Model> checkIfEmpty = new LinkedHashSet<Model>();
			for (Long[] l : selection) {
				if (l.length < 1)
					continue;

				// Create a Model object (for listeners)
				ResultSet rs = findModel(l[0], st);
				if (rs == null)
					continue;
				Model m = new Model(l[0], rs.getString("Name"), rs.getString("Version")); 			
				rs.close();

				if (l.length == 1) { 
					wholeModels.add(m);
					continue;
				}
				String run = (l.length <= 2) ? "" : " AND Run=" + l[2];
				suppress(st, "DELETE FROM RESULTS_OUTPUT_" + l[0] + " WHERE Batch=" + l[1] + run, e);
				suppress(st, "DELETE FROM RESULTS_INPUT_"  + l[0] + " WHERE Batch=" + l[1] + run, e);
				if (run.length() == 0) {
					st.executeUpdate("DELETE FROM BATCH_DESCRIPTION WHERE Model_id=" + l[0] + 
										" AND Batch=" + l[1]);
				} else {
					// TODO: ellenorizni kell a vegen hogy ez a batch ures lett-e es ha igen akkor
					// torolni a batch descriptionbol is. Eleg csak akkor ellenorizni ha van hozza
					// batch description (azaz volna mit torolni).
				}
				checkIfEmpty.add(m);
				resultsDbChanged.fire(new ResultsDbChangeEvent(this, m, l[1].intValue()));
			}
			e = SQLState.BaseTableNotFound_Select;
			for (Model m : checkIfEmpty) {
				ResultSet rs = suppress(st, "SELECT COUNT(*) FROM RESULTS_INPUT_"  + m.getModel_id(), e);
				if (rs != null && rs.next() && rs.getInt(1) == 0)
					wholeModels.add(m);
				if (rs != null)
					rs.close();
			}
			for (Model m : wholeModels) {
				st.executeUpdate("DELETE FROM RESULTS_O_COLUMNS WHERE ID=" + m.getModel_id());
				st.executeUpdate("DELETE FROM RESULTS_I_COLUMNS WHERE ID=" + m.getModel_id());
				st.executeUpdate("DELETE FROM MODELS WHERE Model_id=" + m.getModel_id());
				st.executeUpdate("DELETE FROM BATCH_DESCRIPTION WHERE Model_id=" + m.getModel_id());
			}
			e = SQLState.BaseTableNotFound_Drop;
			for (Model m : wholeModels) {
				suppress(st, "DROP TABLE RESULTS_OUTPUT_" + m.getModel_id(), e);
				suppress(st, "DROP TABLE RESULTS_INPUT_"  + m.getModel_id(), e);
				resultsDbChanged.fire(new ResultsDbChangeEvent(this, m, (Integer)null));
			}
			st.close(); st = null;
			t.commit();
		} finally {
			release(st);
			t.rollback();
		}
	}

	//---------------------------------------------------------------------
	ResultSet suppress(Statement st, String cmd, SQLState suppressThisError) throws SQLException {
		try {
			if (cmd.startsWith("SELECT"))
				return st.executeQuery(cmd);
			st.executeUpdate(cmd);
			return null;
		} catch (SQLException e) {
			if (!dialect().isError(e, suppressThisError))
				throw e;
		}
		return null;
	}

	//---------------------------------------------------------------------
	@Override
	ResultSet readResultByRowID(Result r, int startRowID, int limit) throws SQLException
	{
		Statement st = getConnection().createStatement();
		// TODO: ezt ird at preparedstatement-re majd amikor lesz preparedstatement cache!
		StringBuilder cmd = append(null,
				"SELECT * FROM RESULTS_OUTPUT_", r.getModel().getModel_id(), " WHERE");
		if (startRowID > 0) Utils.append(cmd, 
				" ID>=", startRowID, " AND");
		Utils.append(cmd,
				" Batch=", r.getBatch(),
				" AND Run=", r.getRun(),
				" ORDER BY ID"
		);
		if (limit > 0) Utils.append(cmd, " LIMIT ", limit);
		if (limit <= 0 || limit > 10000)
			st.setFetchSize(10000);
    	return st.executeQuery(cmd.toString());
	}

	//---------------------------------------------------------------------
	@Override
	public Object[] readOneRow(long model_id, int rowID) throws SQLException, InvalidRowException
	{
		Object[] ans = { null, null };
		Statement st = getConnection().createStatement();
		try {
			// TODO: ezt ird at preparedstatement-re majd amikor lesz preparedstatement cache!
			String cmd = "SELECT * FROM RESULTS_OUTPUT_" + model_id + " WHERE ID=" + rowID;
			ResultSet rs = st.executeQuery(cmd.toString());
			ans = ResultInDb.DbThings.readOneRow(model_id, rs);
			st = null;
		} finally {
			release(st);
		}
		return ans;
	}

	//---------------------------------------------------------------------
	@Override
	public IResultsRowsIterator getResultsRows(long model_id, Long[] batchAndRun) {
    	StringBuilder q = append(null, "SELECT * FROM RESULTS_OUTPUT_", model_id);
    	if (batchAndRun == JUST_ANY_ONE) {
    		append(q, " WHERE 1=1 LIMIT 1");
    	}
    	else if (batchAndRun != null && batchAndRun.length > 0) {
    		append(q, " WHERE Batch=", batchAndRun[0]);
    		if (batchAndRun.length > 1)
    			append(q, " AND Run=", batchAndRun[1], " ORDER BY ID");
    		else
    			append(q, " ORDER BY Run, ID");
    	}
    	else {
    		append(q, " ORDER BY Batch, Run, ID");
    	}
		return new ResultsRowsIterator(model_id, q.toString());
	}
	
	//---------------------------------------------------------------------
	@Override
	public IResultsRowsIterator getResultsRows(long model_id, Long[] batches, long min_id, long limit) {
		StringBuilder q = append(null, "SELECT * FROM RESULTS_OUTPUT_", model_id);
		append(q, " WHERE ");
		if (batches != null && batches.length != 0) {
			append(q, "(");
			for (int i=0;i<batches.length;++i) {
				if (i != 0) append(q, " OR ");
				append(q, "Batch=", batches[i]);
			}
			append(q, ") AND ");
		}
		append(q, "ID>=", min_id, " ORDER BY Run, ID", " LIMIT ", limit);
		return new ResultsRowsIterator(model_id, q.toString());
	}

	//---------------------------------------------------------------------
	/** Implementation of interface IResultsRowsIterator. */
	private class ResultsRowsIterator implements IResultsRowsIterator {
		/** The query string of the iterator. */
		String		query;
		/** Model id. */ 
		long		model_id;
		Statement	st;
		/** The next element. */
		Object[]	next;
		/** The reader object. */
		ResultInDb.DbThings reader;
		
		/** Contructor. 
		 * @param model_id model id
		 * @param query query string 
		 */
		ResultsRowsIterator(long model_id, String query) {
			this.model_id = model_id;
			this.query = query;
		}
		public java.util.Iterator<Object[]> iterator() {
			dispose();
			try {
				st = getConnection().createStatement();
				st.setFetchSize(10000);
				ResultSet rs = st.executeQuery(query);
				reader = new ResultInDb.DbThings();
				reader.init(rs);
				if (updateResult()) {
					next();
					if (next != null) next[2] = Boolean.TRUE;
				}
			} catch (SQLException e) {
				handleSQLException(e);
			}
			return this;
		}
		public void dispose() {
			if (st != null) { release(st); st = null; }
			reader = null;
			next = null;
		}
		@Override protected void finalize() throws Throwable {
			dispose();
		}
		/** This method handles the exception 'e' (log entry, dispose, etc.) */ 
		private void handleSQLException(SQLException e) {
			MEMEApp.logExceptionCallStack("LocalAPI.RowIterator", e);
			dispose();
			dialect().checkOutOfMemory(e);
		}
		/** This method switches from one Results to an other (if any).
		 * @return has new row or not 
		 */
		private boolean updateResult() throws SQLException {
			ResultSet rs = (reader == null) ? null : reader.rs;
			if (rs != null) {
				Long batchAndRun[] = { rs.getLong(reader.c_batch), rs.getLong(reader.c_run) };
				List<Result> list = MEMEApp.getResultsDb().getResults(model_id, batchAndRun);
				next = new Object[] { list.get(0), null, Boolean.TRUE };
			} else {
				dispose();
			}
			return next != null;
		}

		public void remove()				{ throw new UnsupportedOperationException(); }
		public boolean hasNext()			{ return next != null; }
		public Object[] next() {
			Object[] ans = next;
			try {
				next = new Object[] { next[0], null, Boolean.FALSE };
				while (true) try {
					next[1] = reader.read((Result)next[0]);
					break;
				} catch (InvalidRowException e) {
					if (updateResult())
						continue;
				}
			} catch (SQLException e) {
				handleSQLException(e);
			} finally {
				if (next != null && next[1] == null) dispose();
			}
			return ans;
		}
	}

	//---------------------------------------------------------------------
	/**
	 * The new database may be empty, therefore this method tries to create
	 * the fixed tables without overwriting.
	 */
	public void onConnChange(ConnChangedEvent event) {
		if (event.getConnection() == null)
			return;

		createTables();
		migrateTables();
    }

	//=========================================================================
	// Public methods

	//=========================================================================
	// Internal methods

	/**
	 * Gets the connection from the DatabaseConnection class, from the databasemanager package.
	 * This connection is established at the beginning of the program execution.
	 * @return Connection
	 */
	private Connection getConnection() {
		return MEMEApp.getDatabase().getConnection();
	}

	/** Returns the dialect belongs to the used database engine. */
	private static SQLDialect dialect() {
		return MEMEApp.getDatabase().getSQLDialect();
	}

	//---------------------------------------------------------------------
	/** It creates all the administration tables belongs to the results tables. */
	private void createTables() {
		String[] cmds = MEMEApp.getDatabase().getSQLDialect().createResultsTables();
		try {
			runCreateCommands(getConnection(), cmds);
		} catch (SQLException sqle) {
			MEMEApp.logException("LocalAPI.createTables()", sqle);    			
			dialect().checkOutOfMemory(sqle);
		}
	}

	//---------------------------------------------------------------------
	/** It runs the create commands contained by 'cmds'. */
	// Note: this method is used by ViewsDb, too
    static void runCreateCommands(java.sql.Connection conn, String cmds[]) throws SQLException {
    	Statement st = null;
    	for (int i = 0; i <= cmds.length; ++i) {
    		try {
	    		if (i == 0) {
	    			st = conn.createStatement();
	    		}
	    		else if (cmds.length <= i) {
	    			st.close();
	    			st = null;
	    		}
	    		if (st == null) break;
	   	    	st.execute(cmds[i]);
    		} catch (SQLException sqle) {
    			if (!dialect().isError(sqle, SQLState.TableAlreadyExists_Create) &&
    				!dialect().isError(sqle, SQLState.SequenceAlreadyExists_Create))
    				throw sqle;
    		}
    	}
    }

	//---------------------------------------------------------------------
	// Megnez minden RESULTS_OUTPUT_ tablat hogy van-e benne ID oszlop
	// es ha nincs akkor beleteszi
    /** It iterates through the RESULTS_OUTPUT_* tables. If such a table 
     *  doesn't contains a column named 'ID', extends with it.
     */ 
	private void migrateTables() {
		SQLDialect dialect = MEMEApp.getDatabase().getSQLDialect();
		String tmpTable = null, seq = null, savedTaskName = null;
		int progress = 0;
		Statement st = null;
		try {
			List<Model> models = getModelsAndVersions();
			for (Model m : models) {
				try {
					if (st == null)
						st = getConnection().createStatement();
					String tableName = "RESULTS_OUTPUT_" + m.getModel_id();
					try {
						st.executeQuery("SELECT ID FROM " + tableName + " WHERE 1=0");
						continue;
					} catch (SQLException sqle) {
						if (!dialect().isError(sqle, SQLState.ColumnNotFound_Select))
							throw sqle;
					}
					Columns io[] = getModelColumns(m.getModel_id());

					if (savedTaskName == null) {
						MEMEApp.LONG_OPERATION.progress(-1, -1);
						MEMEApp.LONG_OPERATION.progress(0, models.size());
						MEMEApp.LONG_OPERATION.showProgressNow();
						savedTaskName = MEMEApp.LONG_OPERATION.getTaskName();
					}
					MEMEApp.LONG_OPERATION.setTaskName(String.format(
							"Migrating results in %s/%s due to version change...",
							m.getName(), m.getVersion()
					));
					MEMEApp.LONG_OPERATION.progress(progress++);
					tmpTable = "RESULTS_TMP";
					seq = dialect.makeTmpSeq(st);
					dialect.deleteTableIfExists(tmpTable);
					String cols = createOutputTable(st, tmpTable, io[1], "RESULTS_INPUT_" + m.getModel_id());
					st.executeUpdate("INSERT INTO " + tmpTable 
									+ " SELECT " + dialect.selectSeqNext(seq) + ',' + cols
									+ " FROM " + tableName + " ORDER BY Batch,Run,Tick");
					dialect.deleteTableIfExists(tableName);
					st.executeUpdate("ALTER TABLE " + tmpTable + " RENAME TO " + tableName);

				} catch (SQLException e) {
					MEMEApp.logException("LocalAPI.migrateTables()", e);
					dialect().checkOutOfMemory(e);
				} catch (UserBreakException e) {
					break;
				}
			}
		} finally {
			release(st);
			if (tmpTable != null) try {
				dialect.deleteTableIfExists(tmpTable);
			} catch (SQLException e) {
				MEMEApp.logException("LocalAPI.migrateTables()", e);
				dialect().checkOutOfMemory(e);
			}
			if (seq != null) try {
				dialect.deleteSeqIfExists(seq);
			} catch (SQLException e) {
				MEMEApp.logException("LocalAPI.migrateTables()", e);
				dialect().checkOutOfMemory(e);
			}
			if (savedTaskName != null)
				MEMEApp.LONG_OPERATION.setTaskName(savedTaskName);
		}
	}

	//---------------------------------------------------------------------
	/** Returns the model specified by 'model_id'. */
	private ResultSet findModel(long model_id, Statement st) throws SQLException {
		ResultSet rs = st.executeQuery("SELECT * FROM MODELS WHERE Model_id=" + model_id);
		return rs.next() ? rs : null;
	}

	//---------------------------------------------------------------------
	/** Returns  a non-used batch and run number in the model specified by 'model_id'.
	 *  The method uses batch[0] and run[0] as output parameters.
	 */
	private void getNewBatchAndRun(long model_id, int batch[], int run[]) {
		assert (batch.length > 0 && run.length > 0);
		batch[0] = FIRST_BATCH;
		run[0]   = FIRST_RUN;
		if (model_id < 0)
			return;
		Statement st = null;
		try {
	    	st = getConnection().createStatement();
	    	// Get the largest run# within the largest batch# 
	    	ResultSet rs = st.executeQuery("SELECT BATCH, RUN FROM RESULTS_INPUT_" + model_id +
	    			                       " ORDER BY BATCH DESC, RUN DESC LIMIT 1");
	    	rs.next();
	    	batch[0] = rs.getInt(1) + 1;				// Columns are numbered from 1
	    	run[0]   = rs.getInt(2) + 1;
	    	rs.close();
	    	st.close(); st = null;
		} catch (SQLException e) {
			// Base table not found = no such model_id
			if (!dialect().isError(e, SQLState.BaseTableNotFound_Select)) {
				MEMEApp.logException("LocalAPI.getNewBatch()", e);
				dialect().checkOutOfMemory(e);
			}
		} finally {
			release(st);
		}
	}


	//---------------------------------------------------------------------
	/**
	 * This method is part of addResult(result).
	 * @pre <code>result</code> specifies a completely new model,
	 *      i.e. {name,version} pair does not exist in the MODELS table
	 */
	private void createModel(Result result) throws Exception {
		
		boolean exception = false;
		
		/*
		 * New model: create it
		 */

		if (result.getBatch() != FIRST_BATCH)
			throw new Exception("Invalid batch#");

//		int run = FIRST_RUN;
		int run = result.getRun() == -1 ? FIRST_RUN : result.getRun();
		long model_id;

		Statement  st = getConnection().createStatement();
		try {
			// I don't verify the value returned by IDGEN - I assume it is really unused. 
			model_id = MEMEApp.getDatabase().getSQLDialect().getSeqNext("IDGEN", st);
		} finally {
			release(st);
			st = null;
		}

		String iTable = "RESULTS_INPUT_"  + model_id;
		String oTable = "RESULTS_OUTPUT_" + model_id;

		/*
		 * SQL mapping for input/output parameter names
		 */

		Columns icols = result.getParameterComb().getNames();
		Columns ocols = result.getOutputColumns();
		String ocolsStr;

		/*
		 * Prepare transaction frame
		 * 
		 * Due to possibly limited support for transactions,
		 * data definition (DDL) commands are done first, and
		 * data manipulation (DML) commands afterwards
		 */
		Transaction tr = new Transaction(getConnection());
		tr.start();

		try {	// finally commit or rollback

			/*
			 * Create table RESULTS_INPUT_*
			 */

			String cmd = MEMEApp.getDatabase().getSQLDialect().createBigTable(iTable +
							" (Batch INT," +
							" Run INT," +
							  quote("Timestamp") + " TIMESTAMP" +
							  icols.compose() +
							",PRIMARY KEY (Batch, Run)" + 
						 	")");
			st = getConnection().createStatement();
			st.execute(cmd);

			/*
			 * Create table RESULTS_OUTPUT_*
			 */

			ocolsStr = createOutputTable(st, oTable, ocols, iTable);
			st.close(); st = null;
			tr.commit();
		} finally {
			release(st);
			tr.rollback();
		}

		/*
		 * Data manipulation section: add new rows.
		 */

		tr.start();

		PreparedStatement ps = null;
		try {	// finally commit or rollback
			/*
			 * First, create the model
			 */
		
			ps = getConnection().prepareStatement(
					"INSERT INTO MODELS (Model_id, Name, Version, Description) VALUES (?,?,?,?)" );
	
			Model m = result.getModel();
			ps.setLong  (1, model_id);				// Columns are numbered from 1
			ps.setString(2, m.getName());
			ps.setString(3, m.getVersion());
			ps.setString(4, m.getDescription());
			ps.executeUpdate();
			ps.close();
			
			/*
			 * Add rows to column map tables
			 */
	
			try {
				icols.updateColMapping("RESULTS_I_COLUMNS", model_id, getConnection());
				ocols.updateColMapping("RESULTS_O_COLUMNS", model_id, getConnection());
			} catch (final DataTruncation e) {
				throw new Exception("One (or more) parameter name is more than " + dialect().getMaxColumnNameLengthInColMap() + " characters.");
			}
			
			/*
			 * Add 1 row to RESULTS_INPUT_*
			 */
	
			ps = getConnection().prepareStatement(
				"INSERT INTO " + iTable + " VALUES (" + repeat("?", 3 + icols.size(), ",") + ")" );
			ps.setInt(1, result.getBatch());
			ps.setInt(2, run);
			ps.setTimestamp(3, new java.sql.Timestamp(result.getStartTime()));
			try {
				result.getParameterComb().getValues().writeValues(ps, 4);
			} catch (final ValueNotSupportedException e) {
				exception = true;
			}
			ps.executeUpdate();
			ps.close();
	
			/*
			 * Add rows to  RESULTS_OUTPUT_*
			 */
	
			ps = getConnection().prepareStatement(
					"INSERT INTO " + oTable + " (" + ocolsStr + 
										") VALUES (" + repeat("?", 3 + ocols.size(), ",")  + ")" );
			ps.setInt(1, result.getBatch());
			ps.setInt(2, run);
			for (Result.Row row : result.getAllRows()) {
				ps.setInt(3, row.getTick());
				try {
					row.writeValues(ps, 4);
				} catch (final ValueNotSupportedException e) {
					exception = true;
				}
				ps.executeUpdate();
			}
			ps.close(); ps = null;

			tr.commit();

			/*
			 * Adjust Result
			 */
			result.updateModelAndRun(model_id, run);
			if (exception)
				throw new ValueNotSupportedException();
		} finally {
			release(ps);
			tr.rollback();
			result.close();
		}
	}
	
	//---------------------------------------------------------------------
	/** Creates a RESULTS_OUTPUT_* table */
	private String createOutputTable(Statement st, String oTable, Columns ocols, String iTable) throws SQLException {
		SQLDialect dialect = MEMEApp.getDatabase().getSQLDialect();
		st.execute(dialect.createBigTable(oTable +
		    " (" + dialect.autoIncrementColumn("ID") +
		    ",Batch INT NOT NULL," +
		    " Run INT NOT NULL, " +
		    " Tick INT" +
		      ocols.compose() +
		    ",FOREIGN KEY (Batch, Run) REFERENCES " + iTable + " (Batch, Run)" + 
		 	")"
		));
		// szandekosan ID nelkul:
		return "Batch,Run,Tick," + join(ocols.getSQLNames(), ",");
	}

	//---------------------------------------------------------------------
	/**
	 * This method is part of addResult(result).
	 * @pre <code>result</code> specifies an existing model, which is 
	 *      found by {name,version} at the specified model_id.
	 */
	private void appendModel(Result result, long model_id) throws Exception 
	{
		boolean exception = false;
		/*
		 * Find the batch and generate new run#.
		 */

		int batch[] = { result.getBatch() }, run[] = { 0 };
		Statement st = getConnection().createStatement();
		// !!!MODOSITAS!!!
		if (result.getRun() == -1)
			throw new Exception("Missing run#");
		run[0] = result.getRun(); 
		ResultSet rs = null;
		try {
//			rs = st.executeQuery("SELECT Run FROM RESULTS_INPUT_" + model_id +
//										  " WHERE Batch=" + batch[0] + 
//										  " ORDER BY Run DESC LIMIT 1");
			rs = st.executeQuery("SELECT Run FROM RESULTS_INPUT_" + model_id +
										  " WHERE Batch=" + batch[0] +
										  " AND Run=" + run[0]);
			if (rs.next()) {
//				run[0] = rs.getInt(1) + 1;				// Columns are numbered from 1
				throw new Exception("Invalid run#");
			} /* else {
				// This batch does not exist - it must be the new batch

				getNewBatchAndRun(model_id, batch, run);

				if (result.getBatch() != batch[0])
					throw new Exception("Invalid batch#");
			} */
			rs.close();
			st.close(); st = null;
		} finally {
			release(st);
		}
		/*
		 * Prepare transaction frame
		 * 
		 * Due to possibly limited support for transactions,
		 * data definition (DDL) commands are done first, and
		 * data manipulation (DML) commands afterwards
		 */
		Transaction t = new Transaction(getConnection());
		t.start();

    	// Meg kell nezni hogy van-e olyan oszlop result-ban ami icols-ban nincs
    	// illetoleg ocols-ban nincs. Ha van, akkor ezekkel boviteni kell az icols/ocols-ban
    	// levo Columns-t, tovabba ALTER TABLE-t kell vegrehajtani. 
    	// Ha sikerult, akkor utana ezeket az oszlopokat hozza kell adni 
    	// a columnmap tablakhoz is.
    	//
		AlterColumns in = null, out = null;

		try {	// finally commit or rollback

			st = getConnection().createStatement();

			in = new AlterColumns("RESULTS_I_COLUMNS", "RESULTS_INPUT_" + model_id, model_id,
									result.getParameterComb().getNames(), st);

			out= new AlterColumns("RESULTS_O_COLUMNS", "RESULTS_OUTPUT_" + model_id, model_id,
									result.getOutputColumns(), st);

			st.close(); st = null;
			t.commit();
		} finally {
			release(st);
			t.rollback();
		}


		/*
		 * Data manipulation section: add new rows.
		 */

		t.start();

		PreparedStatement ps = null;
		try {	// finally commit or rollback
			
			/*
			 * Update columnmap tables (RESULTS_I_COLUMNS, RESULTS_O_COLUMNS)
			 */

			in.updateColMapping(getConnection()); 
			out.updateColMapping(getConnection());

			/*
			 * Add 1 row to the RESULTS_INPUT_* table: the input values.
			 * 
			 * Note that by including the column names in the INSERT statement,
			 * we devolve on the DBMS both the column order translation
			 * and the null-filling of any missing columns for which
			 * there're no values in 'result'
			 */

	    	ps = getConnection().prepareStatement(
	    			"INSERT INTO RESULTS_INPUT_" + model_id + 
	    				" (Batch, Run, " + quote("Timestamp") + in.order + ")" + 
	    				" VALUES (?,?,?" + in.orderQ + ")");
	    	ps.setInt(1, batch[0]);
	    	ps.setInt(2, run[0]);
	    	ps.setTimestamp(3, new java.sql.Timestamp(result.getStartTime()));
	    	try {
				result.getParameterComb().getValues().writeValues(ps, 4);
			} catch (final ValueNotSupportedException e) {
				exception = true;
			}
	    	ps.executeUpdate();
	    	ps.close(); ps = null;

	    	/*
	    	 * Do similarly for the RESULTS_OUTPUT_* table.
	    	 */

	    	ps = getConnection().prepareStatement(
	    			"INSERT INTO RESULTS_OUTPUT_" + model_id + 
	    				" (Batch, Run, Tick" + out.order + ")" + 	// ID erteke automatikusan generalodik
	    				" VALUES (?,?,?" + out.orderQ + ")");
        	ps.setInt(1, batch[0]);
        	ps.setInt(2, run[0]);
        	for (Result.Row row : result.getAllRows()) {
	        	ps.setInt(3, row.getTick());
	        	try {
	        		row.writeValues(ps, 4);
				} catch (final ValueNotSupportedException e) {
					exception = true;
				}
	        	ps.executeUpdate();
	    	}
        	ps.close(); ps = null;

			/*
			 * Update user fields (currently description only)
			 */
			writeDescription(result.getModel(), model_id);

			/*
			 * Adjust Result
			 */
			result.updateModelAndRun(model_id, run[0]);

			t.commit();
			if (exception)
				throw new ValueNotSupportedException();
		} finally {
			if (ps != null) ps.close();
			t.rollback();
			result.close();
		}
	}

    //---------------------------------------------------------------------
	/* Megnezi, hogy van-e olyan oszlop 'names'-ben, ami 'st' tablajaban nincs
	 * (vagy mas tipusu). Ha van, akkor ezekkel boviteni kell az 'st'-bol
	 * beolvasott Columns vektort, tovabba ALTER TABLE-t kell vegrehajtani.
	 * Ha sikerult, akkor utana majd frissiteni kell a column mapping-et is.
	 */
	// Note: this class is used by ViewsDb, too
	static class AlterColumns {
		String	mapTable, order, orderQ;
		Columns	cols		= new Columns();
		boolean changed		= false;
		long id;

		AlterColumns(String mapTable, String valuesTable, long id, Columns names, Statement st) throws SQLException
		{
			this.mapTable	= mapTable;
			this.id			= id;

			cols.read(id, mapTable, st);
	    	ArrayList<String> orderTmp = new ArrayList<String>(names.size()); 
	    	int missing[] = cols.merge(names, orderTmp);
	    	order = (orderTmp.isEmpty() ? "" : ",") + join(orderTmp, ",");
	    	orderQ= repeat(",?", orderTmp.size(), null);
	    	changed = (0 < missing.length);
	    	if (changed) {
	    		SQLDialect dialect = MEMEApp.getDatabase().getSQLDialect();
	    		for (int i = 0; i < missing.length; ++i) {
	    			int idx = missing[i];
	    			if (idx > 0)
		    			st.execute(dialect.alterTableAddColumn    (valuesTable, cols.compose(idx-1)));	// ???
	    			else
		    			st.execute(dialect.alterTableSetColumnType(valuesTable, cols.compose(-idx-1)));	// ???
	    		}
	    	}
		}
		void updateColMapping(java.sql.Connection conn) throws SQLException {
			if (changed)
				cols.updateColMapping(mapTable, id, conn);
		}
	}

    //---------------------------------------------------------------------
	/** Updates the model description (identified by 'model_id') in the database. 
	 *  The new description is in the model 'm'.
	 */ 
	private void writeDescription(Model m, long model_id) throws SQLException {
		if (m.isDescriptionChanged()) {
			PreparedStatement ps = null;
			try { 
				ps = getConnection().prepareStatement(
						"UPDATE MODELS SET Description = ? WHERE Model_id=" + model_id);
				ps.setString(1, m.getDescription());
				ps.executeUpdate();
				ps.close();	ps = null;
			} finally {
				release(ps);
			}
		}
	}

//	//---------------------------------------------------------------------
//	private ResultSet readResult(long model_id, int batch, int run, String what) throws Exception {
//		
//    	Statement st = getConnection().createStatement();
//    	return st.executeQuery("SELECT " + what + " FROM RESULTS_OUTPUT_" + model_id +
//		    			" WHERE Batch=" + batch +  
//		    			" AND Run=" + run
//		    			);
//	}
	
}
