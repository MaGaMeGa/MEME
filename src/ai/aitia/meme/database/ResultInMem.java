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

import java.util.ArrayList;

//-----------------------------------------------------------------------------
/** This class represents the data of one run in the memory. */
public class ResultInMem extends ResultInDb
{
	/** Storage of the rows belongs to the run. */
	protected ArrayList<Row>	outputRows = new ArrayList<Row>(); 

	public void	setBatch(int n)							{ batchNumber = n; }
	public void	setRun(int n)							{ runNumber = n; }
	public void	setStartTime(long t)					{ startTime = t; }
	public void	setEndTime(long t)						{ endTime = t; }
	public void	setModel(Model m)						{ model = m; }
	public void	setParameterComb(ParameterComb inputs)	{ this.inputs = inputs; }
	
	//-------------------------------------------------------------------------
	@Override public int getNumberOfRows() {
		return outputRows.size();
	}

	//-------------------------------------------------------------------------
	@Override public int getFirstRowID() {
		return Row.UNKNOWN_ROW;
	}

	//-------------------------------------------------------------------------
	@Override protected void rewind(int rowID, int limit) {
		this.limit = (limit <= 0) ? Integer.MAX_VALUE : limit;
		nrows = -1;
		for (Row r : outputRows) {
			if (rowID <= r.getRowID()) break; 
			nrows += 1;
		}
	}

	//-------------------------------------------------------------------------
	@Override protected Row getNextRow() {
		if (nrows < -1) nrows = -1;
		if (limit > 0 && nrows + 1 < getNumberOfRows()) {
			if (limit != Integer.MAX_VALUE) limit -= 1;
			return outputRows.get(++nrows);
		}
		return null;
	}

	//-------------------------------------------------------------------------
	/** Adds row 'r' to 'this' object. 
	 * If this is the first row and output columns is unset, then set it from 'r'
	 */
	public void add(Row r) {
		if (r != null) {
			if (outputRows.isEmpty() && outputCols.isEmpty())
				outputCols = r.getColumns();

			assert(r.size() == getOutputColumns().size());
			outputRows.add(r);
		}
	}	

	//-------------------------------------------------------------------------
	/**
	 * If output columns is unset, then set it from the first row of 'rows'.
	 * The caller is responsible for specifying regular Rows (i.e. referring
	 * to identical Columns objects), because it is not verified (for perfomance).
	 */
	public void setRows(ArrayList<Row> rows) {
		if (rows != null) {
			if (!rows.isEmpty()) {
				if (outputCols.isEmpty())
					outputCols = rows.get(0).getColumns();
				else
					assert(rows.get(0).size() == getOutputColumns().size());
			}
			outputRows = rows;
			rewind(Integer.MIN_VALUE, 0);
		}
	}

	//-------------------------------------------------------------------------
	/** Clears the row storage and sets output paramters to 'outputCols'. */
	public void resetRows(Columns outputCols) {
		outputRows.clear();
		if (outputCols != null)
			this.outputCols = outputCols;
		else
			this.outputCols.clear();
	}

}
