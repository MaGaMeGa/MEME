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

/**
 * This is an associate class, collection of subroutines commonly used in LocalAPI
 * and ViewsDb. It is actually part of their implementation, this is why it is not public. 
 * 
 * @author robin
 */
@SuppressWarnings("serial")
class ColumnOrder extends Columns 
{
	/**
	 * maps the indices of 'this' to the columns of a ResultSet:
	 *   colIdx[i] == rs.findColumn(col.sqlNames[i])
	 */
	protected int	colIdx[]= null;		// initialized by reorder()

    //-------------------------------------------------------------------------
	/**
	 * Reorders 'this' to match the actual order of columns in 'rs'
	 * (in order to match the order of values in the table).
	 * Creates and fills colIdx[] so that colIdx[i] == rs.findColumn(col.sqlNames[i]).  
	 * @param rs A ResultSet from a table containing values, e.g. RESULTS_INPUT_*, 
	 *            RESULTS_OUPUT_* or VIEW_*.
	 */
	public void reorder(ResultSet rs) throws SQLException {
		int n = size();
    	getSQLNames();		// ensure that sqlNames is up-to-date

    	int tmp[] = new int[rs.getMetaData().getColumnCount() + 1];	// +1 because columns are numbered from 1 
    	java.util.Arrays.fill(tmp, -1);
    	for (int i = n - 1; i >= 0; --i) {
    		int idx = rs.findColumn(sqlNames.get(i));
    		if (idx < 0)
    			throw new IllegalStateException(String.format("Column %s (%s) is not found in table %s",
    									get(i), sqlNames.get(i), rs.getMetaData().getTableName(0)));
    		tmp[idx] = i;
    	}

    	colIdx		= new int[n];
    	Columns	tmp2= new Columns();
    	for (int i = 0; i < tmp.length; ++i) {
    		int j = tmp[i];
    		if (0 <= j) {
	    		colIdx[tmp2.size()] = i;
	    		tmp2.append(get(j).getName(), sqlNames.get(j), get(j).getDatatype());
    		}
    	}

    	assert tmp2.size() == n;
    	java.util.Collections.copy(this, tmp2);
    	sqlNames = tmp2.sqlNames;	tmp2.sqlNames = null;
	}

    //-------------------------------------------------------------------------
	/** Reads one row from the ResultSet rs. */
	public GeneralRow readRow(ResultSet rs) throws SQLException {
		GeneralRow ans = new GeneralRow(this);
		for (int i = 0, k = size(); i < k; ++i)
			ans.readValue(i, rs, colIdx[i]);
		return ans;
	}

}
