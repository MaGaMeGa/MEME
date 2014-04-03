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

import java.util.ArrayList;
import java.util.Iterator;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.ViewRec;

// The whole class is [Model thread only]
/** This iterator iterates through the rows of the input tables of the view. There
 *  are two types of input tables: results tables and view tables.  */
public class InputRowsIterator implements Iterator<GeneralRow>, Iterable<GeneralRow>
{
	/** The list of the input tables. */
	final ArrayList<Object>	inputTables;
	/** Iterator of input tables. */ 
	Iterator<Object>		tablesIt;
	/** Iterator of the rows of the current result table. */ 
	Iterator<Object[]>		resultsRowsIt;
	/** Iterator of the rows of the current view table. */ 
	Iterator<GeneralRow>	viewRowsIt;
	ITableChangedListener	listener;
	GeneralRow				next = null;
	Result					fireNewResult = null;
	Object					fireNewView[] = { null, null }; 

	//-------------------------------------------------------------------------
	/** Interface for listening table changing events. */
	public interface ITableChangedListener {
		/** Handler method. It is called when the iterator starts a new result table. */
		void	newResult(Result result);
		/** Handler method. It is called when the iterator starts a new view table. */
		void	newView(ViewRec view, Columns cols);
	}

	//-------------------------------------------------------------------------
	public InputRowsIterator(ArrayList<Object> inputTables, ITableChangedListener l) {
		this.inputTables = inputTables;
		listener = l;
	}

	//-------------------------------------------------------------------------
	/** Resets 'this' iterator and returns it */
	public Iterator<GeneralRow> iterator() {
		tablesIt = inputTables.iterator();
		resultsRowsIt= null;
		viewRowsIt	= null;
		next();
		return this;
	}

	//-------------------------------------------------------------------------
	public boolean hasNext()				{ return (next != null); }
	public void remove()					{ throw new UnsupportedOperationException(); } 
	public GeneralRow next() {
		GeneralRow ans = next;
		if (listener != null) {
			if (fireNewResult != null)	{
				listener.newResult(fireNewResult);
				fireNewResult = null;
			}
			if (fireNewView[0] != null) {
				listener.newView((ViewRec)fireNewView[0], (Columns)fireNewView[1]);
				java.util.Arrays.fill(fireNewView, null);
			}
		}

		// Step next
		while (true) { 
			if (viewRowsIt != null) {
				if (viewRowsIt.hasNext()) {
					next = viewRowsIt.next();
					if (fireNewView[0] != null)
						fireNewView[1] = next.getColumns();
					break;
				}
				viewRowsIt = null;
			}
			else if (resultsRowsIt != null) {
				if (resultsRowsIt.hasNext()) {
					Object[] row = resultsRowsIt.next();
					if ((Boolean)row[2]) fireNewResult = (Result)row[0];
					next = (GeneralRow)row[1];
					break;
				}
				resultsRowsIt = null;
			}
			// now  'viewRowsIt == null && resultRowsIt == null'
			if (!tablesIt.hasNext()) {
				// Game over
				next = null;
				break;
			}
			Object o = tablesIt.next();
			if (o instanceof Long[]) {
				Long[] spec = (Long[])o;
				Long[] tmp = new Long[spec.length - 1];
				System.arraycopy(spec, 1, tmp, 0, tmp.length);
				resultsRowsIt = MEMEApp.getResultsDb().getResultsRows(spec[0], tmp).iterator();
			}
			else if (o instanceof ViewRec) {										// ok
				fireNewView[0] = o;
				viewRowsIt = MEMEApp.getViewsDb().getRows(((ViewRec)o).getViewID(), 0, -1).iterator();
			}
			else {
				assert(false) : o;
			}
		} // while
		return ans;
	}
}
