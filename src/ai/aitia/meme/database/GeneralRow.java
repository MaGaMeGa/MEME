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

import java.sql.PreparedStatement;
import java.sql.SQLException;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.ColumnType.ValueNotSupportedException;
import ai.aitia.meme.utils.Utils;

/** A general class to represents rows.
 * I plan to extend this class with support for numeric values, too.
 * Now it handles strings only. 
 * 
 * @author robin
 */
public class GeneralRow implements Cloneable
{
	/** Values of the row. */
	Object		values[];
	/** Column informations. */
	Columns		columns = null;

	public GeneralRow()						{ values = new Object[0]; }
	public GeneralRow(int size)			{ values = new Object[size]; }
	public GeneralRow(Columns c)			{ values = new Object[c.size()]; columns = c; }

	public Columns	getColumns()			{ assert isConsistent(); return columns; }
	public int		size()					{ return values.length; }					// Including null values
	public void	set(int i, Object s)	{ values[i] = s; assert isConsistent(); }	// Note: s may be null
	public Object	get(int i)				{ assert isConsistent(); return values[i]; }
	/** Returns the string representation of the i-th value. */ 
	public String	getLocalizedString(int i)	{
		if (values[i] == null)
			return null;
		if (columns == null || !ColumnType.BOOLEAN.equals(columns.get(i).getDatatype()))
			return values[i].toString();
		return Utils.getBooleanValue(values[i]) ? "true" : "false";	// localizable
	}

	public void	reset()					{ reset(size()); }
	public void	reset(Columns c)		{ columns = c; reset(c.size()); }
	public	void	reset(int i) {
		if (i != values.length)
			values = new Object[i];
		while (--i >= 0)
			values[i] = null;

		assert (isConsistent());
	}

	//-------------------------------------------------------------------------
	/** Writes the i-th value to the prepared statement 'ps' into position 'coldIdx'. 
	 * @throws ValueNotSupportedException */
	public void writeValue(int i, java.sql.PreparedStatement ps, int colIdx) throws SQLException, ValueNotSupportedException {
		assert(columns != null && isConsistent());
		columns.get(i).getDatatype().writeValue(values[i], ps, colIdx);
	}

	//-------------------------------------------------------------------------
	/** Reads value specified by 'colIdx' from 'rs' to the i-th position. */
	public void readValue(int i, java.sql.ResultSet rs, int colIdx) throws SQLException {
		assert(columns != null && isConsistent());
		values[i] = columns.get(i).getDatatype().readValue(rs, colIdx);
	}

	//---------------------------------------------------------------------
	/**
	 * Helps to write one GeneralRow to a table row (via a PreparedStatement). 
	 * Note: 'ps' must be prepared for an INSERT/UPDATE statement 
	 *       containing one '?' for every column of the table.
	 * @throws ValueNotSupportedException 
	 */
	public void writeValues(PreparedStatement ps, int offset) throws SQLException, ValueNotSupportedException {
		boolean exception = false;
		for (int i = 0; i < values.length; ++i) {
			try {
				writeValue(i, ps, offset + i);
			} catch (final ValueNotSupportedException e) {
				MEMEApp.logError(e.getMessage());
				exception = true;
			}
		}
		if (exception)
			throw new ValueNotSupportedException();
	}

	//---------------------------------------------------------------------
	/** Returns whether the values of the row are consistents with the columns information. */
	boolean isConsistent() {
		return (columns == null) || (columns.size() == values.length);		
	}

	//---------------------------------------------------------------------
	/** Shares the columns and duplicates the values[] array (but not the Objects inside) */
	@Override
	public GeneralRow clone() {
		GeneralRow ans = null;
		try { 
			ans = (GeneralRow)super.clone(); 
			values = ans.values.clone();
		} 
		catch (CloneNotSupportedException e) {}
		return ans;
	}

//	public void setValues(String[] s) {
//		values.clear();
//		if (s != null) {
//			values.ensureCapacity(s.length);
//			for (int i = 0; i < s.length; ++i)
//				values.add(s[i]);
//		}
//	}

}
