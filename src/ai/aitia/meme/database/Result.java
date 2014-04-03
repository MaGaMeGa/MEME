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

import java.util.Iterator;


/**
 * Read-only interface to the recordings of one run of a simulation 
 * 
 * @author robin
 *
 */
public abstract class Result 
{
	//---------------------------------------------------------------------
	/** A special row type. Every row has an id and a tick value. */
	public static class Row extends ai.aitia.meme.database.GeneralRow {
		public static final int UNKNOWN_ROW = -1;		// indicates that this row is not in the database
		protected int	tick = -1, rowID = UNKNOWN_ROW;

		public	Row(Columns c)				{ super(c); }
		public	Row(Columns c, int tick)	{ super(c); this.tick = tick; }

		public int		getTick() 			{ return tick; }
		public void	setTick(int tick)	{ this.tick = tick; }

		public int		getRowID() 			{ return rowID; }
	}

	//---------------------------------------------------------------------
	/** Iterator for the rows of a Result object. 
	 * Calls {@link Result#close()} when the last row is reached. If the iteration is
	 * stopped before reaching the last row, the caller is responsible for 
	 * calling {@link #close()}.
	 */
	static class RowIterator implements Iterable<Row>, java.util.Iterator<Row>
	{
		protected final Result result;
		protected final int startID, limit;
		protected Row next;

		private RowIterator(Result result, int startID, int limit) {
			this.result		= result;
			this.startID	= startID;
			this.limit		= limit;
		}

		/** Resets 'this' iterator to the beginning of rows. */
		public Iterator<Row> iterator()	 {
			result.rewind(startID, limit);
			next();
			return this;
		}

		public void remove()				{ throw new UnsupportedOperationException(); }
		public boolean hasNext()			{ return (next != null); }
		public Row next() {
			Row ans = next;
			next = result.getNextRow();
			if (next == null) result.close();
			return ans;
		}
	}

	/** Returns the model belongs to the run. */
	public abstract Model 			getModel();			// which simulation
	/** Returns the id of the batch. */
	public abstract int			getBatch();			// ID of batch
	/** Returns the id of the run. */
	public abstract int			getRun();			// ID of run

	/** Returns the start time of the run. */
	public abstract long			getStartTime();		// when was it started
	/** Returns the end time of the run. */
	public abstract long			getEndTime();		// when was it finished
	/** Returns the input parameter combination of the run. */
	public abstract ParameterComb	getParameterComb();	// what was the input for the simulation
	/** Returns the output parameter names */
	public abstract Columns		getOutputColumns();	// list of output parameter names

	/** Returns the number of recordings about this run. */
	public abstract int			getNumberOfRows();	// number of recordings about this run
	/** Returns the id of the first row of the run. */
	public abstract int			getFirstRowID();	// ID of the first row. May be {@link Row#UNKNOWN_ROW}

	/** Returns the row iterator of the run.
	 * @param rowId the starting row id
	 * @param limit the maximum number of iterable rows */
	public Iterable<Row> getRowsByID(int rowID, int limit) {
		return new RowIterator(this, rowID, limit);
	}
	/** Returns the row iterator of the run. */
	public Iterable<Row> getAllRows() {
		return getRowsByID(Integer.MIN_VALUE, 0);
	}

	/** Releases the resources used during reading */
	public abstract void			close();

	/** 
	 * Rewind the current position so that getNextRow() will return the first row
	 * in which getRowID() is not smaller than 'rowID'. Also releases the resources 
	 * used during previous reading.
	 * @param limit Limits the number of rows that can be read. Zero or negative 
	 *               means infinite. getNextRow() will return false after reading
	 *               this many rows.
	 */
	protected abstract void		rewind(int rowID, int limit);

	/** Returns initially the first, then the next set of output values */
	protected abstract Row		getNextRow();

	/**
	 * This method is intended to be called by IResultsDbMinimal.addResult()
	 * to update the model_id and run# information to the database-generated
	 * values. 
	 */
	abstract void updateModelAndRun(long model_id, int run);
}
