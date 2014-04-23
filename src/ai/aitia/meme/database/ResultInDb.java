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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.AbstractResultsDb.InvalidRowException;

/** This class represents the data of one run in the database. */
public class ResultInDb extends Result
{
	/** The id of the batch. */
	protected int				batchNumber = -1;
	/** The id of the run. */
	protected int				runNumber = -1;
	/** The start time of the run. */
	protected long				startTime = -1;
	/** The end time of the run. */
	protected long				endTime = -1;
	/** The model belongs to the run. */
	protected Model			model = null;
	/** The input parameter combination of the run. */
	protected ParameterComb	inputs = new ParameterComb();
	/** The output parameter names */
	protected Columns			outputCols = new Columns();
	/** The number of recordings about this run. */
	protected int				nrows = -1;	// Number of rows (cached)
	protected int				startRowID = Integer.MIN_VALUE, limit = 0;	// for rewind()

	/**
	 * Allows {@link ResultInMem} being a descendant of this class.
	 * All data that isn't useful in {@link ResultInMem} is encapsulated into this object.
	 */
	private DbThings db = null;

	protected ResultInDb() 
	{
	}

	public ResultInDb(int				batchNumber,
					   int				runNumber,
					   long				startTime,
					   long				endTime,
					   Model			model,
					   ParameterComb	inputs,
					   Columns			outputCols)
	{
		this.batchNumber	= batchNumber;
		this.runNumber		= runNumber;
		this.startTime		= startTime;
		this.endTime		= endTime;
		this.model			= model;
		this.inputs			= inputs;
		this.outputCols		= outputCols;
	}

	@Override public int	getBatch()			{ return batchNumber; }
	@Override public int	getRun()			{ return runNumber; }
	@Override public long	getStartTime()		{ return startTime; }
	@Override public long	getEndTime()		{ return endTime; }
	@Override public Model	getModel()			{ return model; }

	@Override public ParameterComb	getParameterComb()	{ return inputs; }
	@Override public Columns		getOutputColumns()	{ return outputCols; }

	//-------------------------------------------------------------------------
	@SuppressWarnings("cast")
	@Override
	public int	getNumberOfRows() {
		if (nrows < 0) {
			Long tmp[] = { (long)getBatch(), (long)getRun() };
			nrows = MEMEApp.getResultsDb().getNumberOfRows(getModel().getModel_id(), tmp);
		}
		return nrows; 
	}

	//-------------------------------------------------------------------------
	@Override
	public int getFirstRowID() {
		int ans = Row.UNKNOWN_ROW;
		java.sql.Statement st = null;
		try {
			ResultSet rs = MEMEApp.getResultsDb().readResultByRowID(this, Integer.MIN_VALUE, 1);
			st = rs.getStatement();
			if (rs.next())
				ans = rs.getInt("ID");
			rs.close();
			st.close(); st = null;
		} catch (SQLException e) {
			Logger.logException("ResultInDb.getFirstRowID()", e);
			MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
		} finally {
			SQLDialect.release(st);
		}
		return ans;
	}

	//-------------------------------------------------------------------------
	@Override
	public void close() {
		try { 
			if (db != null) db.close();
		} catch (SQLException e) {
			Logger.logException("ResultInDb.close()", e);
			MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
		} finally {
			db = null;
		}
	}

	//-------------------------------------------------------------------------
	@Override
	protected void finalize() throws Exception {
		close();
	}

	//-------------------------------------------------------------------------
	@Override
	protected void rewind(int rowID, int limit) {
		close();
		startRowID = rowID;
		this.limit = limit;
	}

	//-------------------------------------------------------------------------
	@Override
	protected Row getNextRow() {
		try {
			return ensureData().read(this);
		} catch (SQLException e) {
			Logger.logException("ResultInDb.getNextRow()", e);
			MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
		} catch (InvalidRowException e) {
			Logger.logException("ResultInDb.getNextRow()", e);
		}
		return null;
	}

	//-------------------------------------------------------------------------
	@Override
	void updateModelAndRun(long model_id, int run) {
		Model m = new Model(model_id, getModel().getName(), getModel().getVersion());
		if (getModel().isDescriptionChanged()) m.setDescription(getModel().getDescription(), false);
		model = m;
		runNumber = run;
	}

    //=========================================================================
	// Private methods

	//-------------------------------------------------------------------------
	private DbThings ensureData() throws SQLException {
		if (db == null) { 
			DbThings tmp = new DbThings();
			tmp.init(MEMEApp.getResultsDb().readResultByRowID(this, startRowID, limit));
			db = tmp;
			assert(db != null && db.colIdx.length == getOutputColumns().size());
		}
		return db;
	}

	//-------------------------------------------------------------------------
	/** This class encapsulates the database related data. */ 
	static class DbThings {
		/** The result set of the run. */
		ResultSet	rs;
		/** The index of the 'ID' column. */
		int			c_rowid;
		/** The index of the 'Batch' column. */
		int			c_batch;
		/** The index of the 'Run' column. */
		int		    c_run;
		/** The index of the 'Tick' column. */
		int			c_tick;
		/** The indices of the others column. */
		int			colIdx[];

		//---------------------------------------------------------------------
		/** Initializes 'this'. */
		void init(ResultSet rs) throws SQLException {
			this.rs = rs;
			try { // in case of error, close tmp.rs
				c_rowid = rs.findColumn("ID");
				c_batch = rs.findColumn("Batch");
				c_run   = rs.findColumn("Run");
				c_tick  = rs.findColumn("Tick");
				int n = rs.getMetaData().getColumnCount();
				colIdx  = new int[n - 4];
				for (int i = 1, j = 0; i <= n; ++i) {	// 'i' starts from 1 because columns are numbered from 1
					if (i == c_rowid || 
						i == c_batch || 
						i == c_run || 
						i == c_tick) continue;
					colIdx[j++] = i;
				}
				if (rs.next())
					rs = null;
			} finally {
				if (rs != null) close();
			}
		}

		//---------------------------------------------------------------------
		/** Releases the resources. */
		void close() throws SQLException {
			if (rs != null) {
				java.sql.Statement st = rs.getStatement();
				rs.close();
				st.close();
				rs = null; 
			}
			colIdx = null;
		}

		//---------------------------------------------------------------------
		/** Read the next row from 'rs'.
		 * @param r the result object belongs to the result set 'rs' */
		Row read(Result r) throws SQLException, InvalidRowException {
			if (rs == null)
				return null;

			assert(colIdx.length == r.getOutputColumns().size());

			Row ans = new Row(r.getOutputColumns());
			ans.rowID = rs.getInt(c_rowid);

			// Verify Batch# and Run#
			if (rs.getInt(c_batch) != r.getBatch()
				|| rs.getInt(c_run) != r.getRun() )
				throw new InvalidRowException();

			ans.setTick(rs.getInt(c_tick));

			for (int i = 0, k = ans.size(); i < k; ++i) {
				ans.readValue(i, rs, colIdx[i]);
			}
			if (!rs.next()) close();
			return ans;
		}

		//---------------------------------------------------------------------
		/** Reads the row specified by 'model_id' and 'rs'. The model_id specifies the
		 *  table which from the method reads the row. The 'rs' contains the batch and
		 *  run ids.  
		 * 	Designed for ViewCreation upspeed.
		 * 	Returns an array of length 2: <code>{ Result, Result.Row }</code>
		 */
		static Object[] readOneRow(long model_id, ResultSet rs) throws SQLException, InvalidRowException {
			DbThings tmp = new DbThings();
			tmp.init(rs);
			Long batchAndRun[] = { rs.getLong(tmp.c_batch), rs.getLong(tmp.c_run) };
			List<Result> list = MEMEApp.getResultsDb().getResults(model_id, batchAndRun);
			try {
				Object[] ans = { list.get(0), tmp.read(list.get(0)) };
				return ans;
			} finally {
				tmp.close();
			}
		}
	}

}
