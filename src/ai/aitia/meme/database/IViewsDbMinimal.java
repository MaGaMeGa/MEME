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
 * Essentially read-only interface to the views database.
 * This interface is designed for chart creation and export plugins.  
 *
 */
public interface IViewsDbMinimal
{
	/** Returns the list of available views. */
	public java.util.List<ViewRec> getViews();

	/** Returns null if the named view does not exist */
	public Long findView(String name);

	/** Returns the name of the view or null if error occurs or there's no such view  */
	public String getName(long view_id);

	/** The returned object is actually a ColumnOrder, or null if error occurs */
	public Columns getColumns(long view_id);

	/** 
	 * Returns rows from the view_id view table, only rows for which ID >= minid
	 * (minid==0 returns all rows), at most 'limit' rows (zero or negative means no limit). 
	 * Never returns null. If the specified view does not exist, an empty iterator is returned.
	 */
	public Iterable<GeneralRow> getRows(long view_id, long minid, long limit);
	
	/** 
	 * Disposes the database resources associated with 'rowIt', 
	 * provided that it was created by getRows(). May be called twice.  
	 */
	public void close(Iterable<GeneralRow> rowIt);
	/** 
	 * Disposes the database resources associated with 'rowIt', 
	 * provided that it was created by getRows(). May be called twice.  
	 */
	public void close(java.util.Iterator<GeneralRow> rowIt);
}
