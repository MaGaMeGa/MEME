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

import ai.aitia.meme.events.Event;


/** Abstract implementation of the interface IResultsDbMinimal. It defines
 *  new abstract methods that it uses to implements some of the methods of the 
 *  interface.
 *  
 * @author robin
 */
public abstract class AbstractResultsDb implements IResultsDbMinimal 
{
	public static final Long[]		JUST_ANY_ONE = { 0L, 0L };

	//---------------------------------------------------------------------
	/** Every iterator class that implements this interface must returns 
	 *  three-length { Result, Result.Row, Boolean } arrays. The last element indicates
	 *  whether the first element is new (true) or the same than it was in the previous
	 *  row (false).
	 *  The dispose() method isn't called if the caller hasn't finished the loop (except 
	 *  in the case of an Exception). This time the caller may call it manually. If the 
	 *  caller didn't do that, the finalize() will do.
	 */  
	public interface IResultsRowsIterator extends Iterable<Object[]>, java.util.Iterator<Object[]> {
		public void dispose();
	}

	/** The returned list contains available Models ordered by name and then version. */
	public abstract List<Model>			getModelsAndVersions();
	/** Returns the model from the database specified by model_id. */
	public abstract Model				findModel(long model_id);
	
	public abstract boolean renameModel(final long model_id, final String old_name, final String old_version, final String new_name,
										final String new_version);
	
	/** Returns the description of the batch specified by model_id and batch. */
	public abstract String				getDescription(long model_id, int batch);
	/** Returns the information about the batch(es) that belongs to the model identified
	 *  by model_id. If batch is null, returns all available batches of the model, otherwise
	 *  the specified one only.
	 */ 
		   abstract List<BatchInfo>		getBatchInfoImpl(long model_id, Integer batch);
	/** Returns the information about the batches that belongs to the model identified
	 *  by model_id. 
	 */ 
	public List<BatchInfo>				getBatchInfo(long model_id) { return getBatchInfoImpl(model_id, null); }
	/** Returns the information about the batch that belongs to the model identified
	 *  by model_id and batch. 
	 */ 
	public BatchInfo					getBatchInfo(long model_id, int batch) {
		List<BatchInfo> l = getBatchInfoImpl(model_id, batch);
		return (l.isEmpty()) ? null : l.get(0);
	}
	/** Returns all Results where the model id is model_id. */  
	public List<Result>					getResults(long model_id)	{ return getResults(model_id, null); }
	/** The array batchAndRun has two elements:<br>
	 *    - batch id<br>
	 *    - run id<br>
	 *  Returns all Results where the model id == model_id and the batch id == batchAndRun[0]
	 *  and the run id == batchAndRun[1]. batchAndRun==null indicates that the batch and
	 *  run id are arbitrary. */  
	public abstract List<Result>		getResults(long model_id, Long[] batchAndRun);
	
	/** The array batchAndRun has two elements:<br>
	 *    - batch id<br>
	 *    - run id<br>
	 *  Returns an iterator of the Results where the model id == model_id and the batch id == batchAndRun[0]
	 *  and the run id == batchAndRun[1]. batchAndRun==null indicates that the batch and
	 *  run id are arbitrary. If there are more than one Results then the iterator works in the 
	 *  following way: when it iterates all rows of a Result object, the takes the next Result
	 *  objects and iterates further on its rows (and so on). */  
	public abstract IResultsRowsIterator getResultsRows(long model_id, Long[] batchAndRun);
	/**  Returns an iterator of the Results where the model id == model_id and the
	 *   batch id == batches[i]. batches==0 indicates the the batch id is arbitrary.
	 *   If there are more than one Results then the iterator works in the 
	 *   following way: when it iterates all rows of a Result object, the takes the next Result
	 *   objects and iterates further on its rows (and so on).
	 *   The iterator iterates only those rows whose row id are greater than or equals
	 *   to min_id. The number of rows that that are iterable with this iterator are <code>limit</code>. */  
	public abstract IResultsRowsIterator getResultsRows(long model_id, Long[] batches, long min_id, long limit);
	/** The array batchAndRun has two elements:<br>
	 *    - batch id<br>
	 *    - run id<br>
	 *  Returns the sum of number of rows of the Results where the model id == model_id and the batch id == batchAndRun[0]
	 *  and the run id == batchAndRun[1]. batchAndRun==null indicates that the batch and
	 *  run id are arbitrary. */  
	public abstract int				getNumberOfRows(long model_id, Long[] batchAndRun);

    //-------------------------------------------------------------------------
	/** Returns the columns of the Results specified by model_id. The return value  
	 *  is a two-length array, because the input and output columns are divided.
	 */
	public Columns[] getModelColumns(long model_id) {
		List<Result> l = getResults(model_id, JUST_ANY_ONE);
		if (l == null || l.isEmpty()) return null;
		Columns ans[] = { l.get(0).getParameterComb().getNames(), l.get(0).getOutputColumns() };
		return ans;
	}
	
	/** Deletes rows from the database. The parameter is an array of number-triples
	 * (model id, batch id, run id). */
	public abstract void				deleteAll(Long[][] selection) throws SQLException;


    //-------------------------------------------------------------------------
	// To be used through the Model class only
	/** Returns the description of the model specified by model_id. */
	abstract String					getDescription(long model_id);
	/** Writes the description of model m to the database. */
	abstract void						writeDescription(Model m);

	/** Reads <code>limit<code> rows from the result. The id of first row will be
	 *  startsRowID.
	 * 	To be used through the ResultInDb class only.
	 * 	Do not forget to close both the Result and the Statement!
	 */
	abstract ResultSet					readResultByRowID(Result result, int startRowID, int limit) throws SQLException;

	/** Reads the row specified by rowID. The model_id specifies the table which from the 
	 *  method reads the row.  
	 * Designed for ViewCreation upspeed.
	 * Returns an array of length 2: <code>{ Result, Result.Row }</code>
	 * which is <code>{null,null}</code> if the specified 'rowID' does not exist.
	 */
	public abstract Object[]			readOneRow(long model_id, int rowID) throws SQLException, InvalidRowException;

    //-------------------------------------------------------------------------
    /**
     * This event is fired after adding/removing results into the database,
     * or modifyig model/batch descriptions.
     */
    public final Listeners resultsDbChanged = new Listeners();

    //-------------------------------------------------------------------------
    /** Interface for listening database modification events. */
	public interface IResultsDbChangeListener extends java.util.EventListener {
		/** 
		 * This method is called when a (new) model/batch/result is created/removed
		 * or a model/batch description is modified.
		 * Note: May be called in any thread (usually in the %Model thread)!
		 */
		public void onResultsDbChange(ResultsDbChangeEvent e);
	}

    //-------------------------------------------------------------------------
	/** Storage for listeners which observe database modification events. */ 
	public static class Listeners extends Event<IResultsDbChangeListener, ResultsDbChangeEvent> {
		public Listeners() { super(IResultsDbChangeListener.class, ResultsDbChangeEvent.class); }
		@Override protected void fire(ResultsDbChangeEvent msg) { super.fire(msg); }
	}

    //-------------------------------------------------------------------------
	/** Database modfication types. */
	public static enum ChangeAction { ADDED, REMOVED, MODIFIED };

    //-------------------------------------------------------------------------
	/** 
	 * This class represents a modification in the Results database.
	 * The following modifications can be represented:<br>
	 * - if getResult() != null: an IResult has been added/modified (according to getAction()).
	 *   E.g. one or more columns have been added.<br>
	 * - if getResult() == null && getDescription() != null: getModel() is non-null, representing 
	 *   a description modification of a Model or a batch. If getBatchNr() is null, the description 
	 *   of a Model has been changed. Otherwise the batch's description has been changed.<br> 
	 * - if getResult() == null && getDescription() == null: getModel() is non-null, representing
	 *   removal of the whole model (when getBatchNr() is null) or a whole batch (otherwise). 
	 */
    @SuppressWarnings("serial")
	public static class ResultsDbChangeEvent extends java.util.EventObject
	{
    	private final ChangeAction		action;
    	private Result					result = null;
    	private String					description = null;
    	private Model					model = null;
    	private Integer					batch = null;
    	private boolean					renamed = false;

		public ResultsDbChangeEvent(Object source, ChangeAction a, Result result) {
			super(source);
			this.action = a;
			this.result = result;
		}
		public ResultsDbChangeEvent(Object source, Model m, String description) {
			super(source);
			assert(description != null);
			this.action = ChangeAction.MODIFIED;
			this.model  = m;
			this.description = description;
		}
		public ResultsDbChangeEvent(Object source, Model m, int batch, String description) {
			super(source);
			this.action = ChangeAction.MODIFIED;
			this.model  = m;
			this.batch  = batch;
			this.description = description;
		}
		public ResultsDbChangeEvent(Object source, Model m, Integer batch) {
			super(source);
			this.action = ChangeAction.REMOVED;
			this.model  = m;
			this.batch  = batch;
			this.description = null;
		}
		public ResultsDbChangeEvent(Object source, Model m, boolean renamed) {
			super(source);
			this.action = ChangeAction.MODIFIED;
			this.model = m;
			this.batch = null;
			this.description = null;
			this.renamed = renamed;
		}

		@Override public AbstractResultsDb	getSource()	{ return (AbstractResultsDb)super.getSource(); }
		public ChangeAction			getAction()			{ return action; }
		public Result				getResult()			{ return result; }
		public String				getDescription()	{ return description; }
		public Model				getModel()			{ return model; }
		public Integer				getBatchNr()		{ return batch; }
		public boolean 				isRenamed()			{ return renamed; }
    }

    //-------------------------------------------------------------------------
    /** Information record for batches. */
	public static class BatchInfo {
		public final int		batch;
		public final int		nrOfRuns;
		public final String	description;
		public	BatchInfo(int b, int nr, String s) { batch = b; nrOfRuns = nr; description = s; }
	}

	//-------------------------------------------------------------------------
	public static class InvalidRowException extends Exception {
		private static final long serialVersionUID = -851124207246667602L;
		public InvalidRowException() { super("invalid batch# or run# in the database"); }
	}
}
