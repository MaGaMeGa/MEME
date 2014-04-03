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


/**
 * An essentially write-only interface for the Results database.
 * It is designed for the remove batch-runner engines: it reduces
 * the functionality to make it easier to develop a client-server
 * implementation.
 */
public interface IResultsDbMinimal {

	/**
	 * Looks for the model specified by the arguments in the database.
	 * @return The specified Model or null.
	 */
	public Model findModel(String name, String version);

	/**
	 * Returns a list containing 2 Columns objects: the input and output
	 * parameters of the Model specified by 'model_id'. If there's no such 
	 * Model, <code>null</code> is returned.
	 */
	public Columns[] getModelColumns(long model_id);

	/**
	 * Returns a batch# identifier for a new batch within the specified Model
	 * (which may be null).
	 * @param m A model in which a new batch is about to be created. May be null. 
	 */
	public int getNewBatch(Model m);
	
	/**
	 * Adds data (about a new run) into the results database.
	 * @param result Specifies a Result object to be added to the database.
	 * It will be added to that Model (name, version) and batch# which is contained 
	 * in <code>result</code>. Note that model_id and run# are NOT read but are UPDATED
	 * through the IResult.updateModelAndRun() method.
	 * If the model is new, the database will assign a value to it automatically.
	 * Otherwise it is looked up by (name, version) and corrected.
	 * The database also assigns a new value to run#.
	 * @throws Exception If the batch# is invalid (not an existing batch#, and 
	 * not a valid number for a new batch - see getNewBatch()). 
	 * SQL errors are also returned as exception.   
	 */
	public void addResult(Result result) throws Exception;

	/** Writes 's' to the database as the description of a batch identified by 'model_id' and
	 *  'batch'.
	 * Throws IllegalArgumentException if the specified 'model_id' and 'batch' do not exists.   
	 */
	public void storeBatchDescription(long model_id, int batch, String s);
}
