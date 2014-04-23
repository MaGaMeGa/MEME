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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.pluginmanager.Parameter;
import ai.aitia.meme.utils.Utils;

/**
 * List of human names of columns + additional info (SQL-names and types)
 * 
 * @author robin
 *
 */
@SuppressWarnings("serial")
public class Columns extends ArrayList<Parameter> 
{
	private static final String allowed = "0123456789_ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	/** Storage of the SQL-names. */
	protected ArrayList<String> sqlNames= null;
	
    //-------------------------------------------------------------------------
	/** Returns whether 'this' object is consistent with <code>sqlNames</code> or not. */
	private boolean isConsistent() {
		return (isEmpty() && (sqlNames == null || sqlNames.isEmpty()))
				|| (size() > 0 && (sqlNames == null || sqlNames.size() <= size()));
	}

    //-------------------------------------------------------------------------
	// Intentionally not public
	/** Adds a new column to 'this' object.
	 * @param name human name of the column
	 * @param sqlcolumn  the SQL-name of the column
	 * @param type the type of the column
	 */
	void append(String name, String sqlcolumn, ColumnType type) {
		assert isConsistent();
		if (sqlNames == null || sqlNames.size() < size())
			getSQLNames();
		super.add(new Parameter(name, type));
		sqlNames.add(sqlcolumn);
	}

    //-------------------------------------------------------------------------
	/** Adds a new column to 'this' object. 
	 * 	Precondition: 'name' is new. <br> Note: 'type' may be null! 
	 * @param name human name of the column
	 * @param type the type of the column
	 */
	public void append(String name, ColumnType type) {
		assert isConsistent(); 
		super.add(new Parameter(name, type));
	}

    //-------------------------------------------------------------------------
	/** Adds columns from other (from index begin to index end) to 'this' object. 
	 * Precondition: the new elements do not interfere with existing elements of 'this'
	 * (neither name, nor sqlName!)
	 */ 
	public void append(Columns other, int begin, int end) {
		other.getSQLNames();
		for (int i = begin; i < end; ++i) {
			Parameter p = other.get(i);
			append(p.getName(), other.sqlNames.get(i), p.getDatatype());
		}
	}

    //-------------------------------------------------------------------------
	@Override
	public Parameter remove(int index) {
		Parameter ans = super.remove(index);
		if (sqlNames != null && index < sqlNames.size())
			sqlNames.remove(index);

		return ans;
	}

    //-------------------------------------------------------------------------
	/** Uses <code>element.equals()</code> instead of <code>obj.equals()</code> */
	@Override
	public int indexOf(Object obj) {
		return Utils.indexOf(this, obj);
	}

    //-------------------------------------------------------------------------
	/** Returns a string array containing SQL-counterparts of the elements of 'this' array. */
	public ArrayList<String> getSQLNames() {
		if (sqlNames == null)
			sqlNames = new ArrayList<String>(this.size());
		if (sqlNames.size() < this.size()) {
			int maxlen = MEMEApp.getDatabase().getSQLDialect().getMaxColumnNameLengthInColMap();
			for (int i = sqlNames.size(); i < this.size(); ++i) {
				sqlNames.add(correctedNameForSQL(get(i).getName(), sqlNames, maxlen));
			}
		}
		return sqlNames;
	}

    //-------------------------------------------------------------------------
	/**
	 * Loads column info (human names, sql column names and type info) 
	 * from the database 'table'.
	 * Note: the columns will be in undefined order!
	 */
	// Intentionally not public
	void read(long id, String table, Statement st) throws SQLException {
    	ResultSet rs = st.executeQuery("SELECT * FROM " + table + " WHERE ID=" + id);
    	int c_Name   = rs.findColumn("Name");
    	int c_Column = rs.findColumn("Column");
    	int c_Type   = rs.findColumn("Type");
    	int c_A      = rs.findColumn("A");
    	int c_B      = rs.findColumn("B");
    	clear();
		while (rs.next()) {
			String name   = rs.getString(c_Name);
			String column = rs.getString(c_Column);
			ColumnType t;
			try {
				t = new ColumnType(rs.getInt(c_Type), rs.getInt(c_A), rs.getInt(c_B));
			} catch (IllegalArgumentException e) {
				t = ColumnType.STRING;
				Logger.logError("Invalid ColumnType in table %s at row {ID=%d,Name=%s,Column=%s}: %d" +
						" It will be treated as %s",
						table, id, name, column, rs.getInt(c_Type), t.getSQLTypeStr());
			}
			append(name, column, t);
		}
		rs.close();
	}

    //-------------------------------------------------------------------------
	@Override
	public void clear() {
		super.clear();
		if (sqlNames != null)
			sqlNames.clear();
	}

    //-------------------------------------------------------------------------
	/**
	 * Extends 'this' with parameters from 'newPars' that are new. For each new
	 * parameter, an SQL-counterpart is generated and added to 'sqlNames'. 
	 * For existing parameters, the column types are extended as needed. 
	 * This method is intended to be used after read().
	 * @param newPars List of (potentially new) parameters to consider
	 * @param order If non-null, specifies an array that receives the list of 
	 *         SQL counterparts of 'newPars'. This means that 'order' will 
	 *         contain the same number of elements as 'newPars', one SQL-column 
	 *         for each name in 'newPars', in the same order.
	 * @return Array of nonzero integers, indicating new/modified elements of 'this',
	 * that were created now or the type was extended.
	 * A positive integer i indicates that that get(i-1) is a new column.
	 * A negative integer i means that the type of get(-i-1) has been extended.
	 */
	public int[] merge(Columns newPars, ArrayList<String> order) {
		int n = newPars.size();
		ArrayList<Integer> ans = new ArrayList<Integer>(n);
		if (order != null) {
			order.clear();
			order.ensureCapacity(n);
		}
    	for (Parameter newp : newPars) {
    		int found = this.indexOf(newp.getName()); 
    		if (found < 0) {
	    		found = size();
	    		add(newp);
	    		ans.add(found + 1);					// indicate that it is new (-> positive integer)
    		}
    		else {
    			ColumnType currType = get(found).getDatatype(), newtype = newp.getDatatype();
    			if (currType != null)
    				newtype = currType.getUnion(newtype);
    			if (!newtype.equals(currType)) {
    				get(found).setDatatype(newtype);
    				ans.add(-(found + 1));			// indicate type change (-> negative integer)
    			}
    		}
    		if (order != null)
    			order.add(getSQLNames().get(found));
    	}
    	return Utils.asIntArray(ans);
	}

    //-------------------------------------------------------------------------
	/**
	 * Returns the elements of this list in ascending order (without sql names). 
	 */
	public ArrayList<Parameter> getSorted() {
		ArrayList<Parameter> tmp = new ArrayList<Parameter>(this);
		java.util.Collections.sort(tmp);
		return tmp;
	}

	//---------------------------------------------------------------------
	/**
	 * Helps to compose list of columns for CREATE TABLE, ALTER TABLE commands.
	 * @param idx If positive or zero, the method generates string for one
	 *             column only (specified by the idx). Otherwise the method  
	 *             generates a string for all columns.
	 * @return A string like <code>"_COLNAME VARCHAR(99)"</code> (when idx >= 0)
	 *          or <code>", _COL1 VARCHAR(99), _COL2 CHAR(1)"</code>... (when idx < 0)
	 */
	public String compose(int idx) throws SQLException {
    	getSQLNames();	// ensure that 'sqlNames' is up-to-date
		StringBuilder ans = new StringBuilder();
		final String before = (idx >= 0) ? "" : ", ";
		final String after  = " ";
		int i = 0, n = sqlNames.size() - 1;		if (idx >= 0) { i = n = idx; }
		for (; i <= n; ++i) {
			ans.append(before);
			ans.append(sqlNames.get(i));
			ans.append(after);
			ans.append(get(i).getDatatype().getSQLTypeStr());
		}
		return ans.toString();
	}

    //-------------------------------------------------------------------------
	/**
	 * Helps to compose list of columns for CREATE TABLE, ALTER TABLE commands.
	 * @return A string like <code>", _COL1 VARCHAR(99), _COL2 CHAR(1)"</code>... 
	 */
	public String compose() throws SQLException {
		return compose(-1);
	}

	//---------------------------------------------------------------------
	/** Updates the human (human name,SQL-name) map in the database.
	 * @param table the name of table whose column mapping in not up-to-date
	 * @param id id of updateable row
	 * @param conn database connection 
	 */
	void updateColMapping(String table, long id, java.sql.Connection conn) throws SQLException {
    	getSQLNames();	// ensure that sqlNames is up-to-date

    	deleteColMapping(table, id, conn);

		PreparedStatement ps = conn.prepareStatement(
			"INSERT INTO " + table + " (ID, Name, " + quote("Column") + ", Type, A, B)" +
					" VALUES (?,?,?,?,?,?)");

		for (int i = 0; i < size(); ++i) {
			Parameter p = get(i);
			ps.setLong(1, id);
			ps.setString(2, p.getName());
			ps.setString(3, sqlNames.get(i));
			ColumnType t = p.getDatatype();
			ps.setShort(4, (short)t.javaSqlType);
			ps.setShort(5, (short)t.a);
			ps.setShort(6, (short)t.b);
			ps.executeUpdate();
		}
		ps.close();
	}
	
	//-------------------------------------------------------------------------
	/** Removes the human name-SQL-name map in the database.
	 * @param table the name of table whose column mapping are deletable
	 * @param id id of updateable row
	 * @param conn database connection 
	 */
	static void deleteColMapping(String table, long id, java.sql.Connection conn) throws SQLException {
		Statement st = conn.createStatement();
		try {
			st.executeUpdate("DELETE FROM " + table + " WHERE ID=" + id);
		} finally {
			st.close();
		}
	}

	//-------------------------------------------------------------------------
	/**
	 * Generates an SQL-counterpart for the string <code>name</code>,  
	 * with respect to the already <code>existing</code> names and <code>maxlen</code>.
	 * Note: the returned name always begins with '_'.
	 * Example: createSQLName("pH", null, 15) = "_PH"
	 * @param maxlen Must be greater or equal to 8.     
	 */
	public static String correctedNameForSQL(String name, List<String> existing, int maxlen) {

		assert(maxlen >= 8);
		
		// Lower case -> upper case

		String ans = name.toUpperCase();

		// Remove all chars that are not in [0-9A-Z_]
		
		StringBuilder q = new StringBuilder(ans.length());
		int n = Math.min(ans.length(), maxlen);
		for (int i = 0; i < n; ++i) {
			char ch = ans.charAt(i);
			if (0 <= allowed.indexOf(ch))
				q.append(ch);
		}

		// If the result does not begin with an underscore, make it begin with that,
		// to ensure that it will not coincide with reserved SQL words.   
		
		// If empty, set it to a single underscore.  
		// If begins with number, insert an underscore before.
		
		if (q.length() == 0 || q.charAt(0) != '_') 
			q.insert(0, '_');
		
		// Cut if too long.
		if (q.length() > maxlen)
			q.setLength(maxlen);

		// Look for the resulting string: if it already exsists, then number it
		
		ans = q.toString();
		if (existing != null) {
			int i = 1, j = q.length();
			while (existing.contains(ans)) {
				i += 1;
				while (j >= 0) {
					q.setLength(j);
					q.append(i);
					if (q.length() <= maxlen) break;
					j -= 1;
				}
				ans = q.toString();
			}
		}
		return ans;
	}


    //-------------------------------------------------------------------------
	/** Returns the 'columnName' between quotes (the quote string is database engine-specific). */ 
	public static String quote(String columnName) throws SQLException {
		return MEMEApp.getDatabase().getSQLDialect().quote(columnName);
	}

}
