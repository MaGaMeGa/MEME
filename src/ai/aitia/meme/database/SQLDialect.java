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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;

/**
 * Interface for nonstandard SQL commands.
 * 
 * This class is an interface and a manager: it provides no implementation 
 * except for the static methods which manage the collection of available
 * SQL-dialect implementations.
 */
public abstract class SQLDialect {

	//=========================================================================
	// Constants
	
	public static final int	MAX_MODEL_NAME_LEN			= 64;
	public static final int	MAX_MODEL_VER_LEN			= 64;
	public static final int	MAX_BATCH_DESC_LEN			= 64;
	public static final int	MAX_COLNAME_LEN_IN_COLMAP	= 64;
	public static final int	MAX_VIEW_NAME_LEN			= 64;
	
	public static enum SQLState {
		BaseTableNotFound_Select,
		BaseTableNotFound_Drop,
		SequenceNotFound_Drop,
		ColumnNotFound_Select,
		TableAlreadyExists_Create,
		SequenceAlreadyExists_Create,
		OutOfMemory,
		FunctionNotFound_Select,
		FunctionMissingParameters_Select
	};


	//=========================================================================
	// Required methods
	
	/** The string id of the dialect. */
	public abstract String		getDialectID();

	/**
	 * Returns true if the specified JDBC connection string is known by 'this' dialect. 
	 * @param address A JDBC connection string like "jdbc:hsqldb:hsql://localhost/"
	 */
	public abstract boolean	recognize(String address);
	
	/**
	 * Loads the JDBC driver for the specified connection string.
	 * Precondition: recognize() returned true for the specified string. 
	 */
	public abstract void		loadDriver(String address) throws Exception;

	/** 
	 * The last step in determining whether <code>this</code> SQLDialect can be used 
	 * for a connection.
	 * <br>
	 * Precondition: {@link #recognize(String)} returned true for the connection string, 
	 * then {@link #loadDriver(String)} was called, and the connection has been created
	 * successfully.   
	 * @return List of localized strings describing the problem(s), if any. 
	 * If the connection is acceptable, null or empty list should be returned. 
	 */
	public abstract List<String>	isAcceptable(java.sql.DatabaseMetaData md) throws SQLException;

	/**
	 * Returns individual SQL commands for creating the followings (in this order):
	 * Sequences: IDGEN (integer, starting with 0, incrementing by 1)
	 * Tables:    MODELS, RESULTS_I_COLUMNS, RESULTS_O_COLUMNS, BATCH_DESCRIPTION. 
	 */
	public abstract String[]	createResultsTables();

	/**
	 * Returns individual SQL commands for creating the following tables (in this order): 
	 *   VIEWS, VIEW_COLUMNS
	 */
	public abstract String[]	createViewsTables();
    /**
     * Returns a modified SQL command string for creating a database table 
     * that is capable of storing dozens of megabytes of data.
     * @param definition The core of the SQL command: "TABLENAME (columnspec...)"
     */
	public abstract String		createBigTable(String definition);
    /**
     * Returns a modified SQL command string for creating a temporary table that preserves
     * its contents while the connection is open.
     * @param definition The core of the SQL command: "TABLENAME (columnspec...)"
     */
    public abstract String		createTmpTable(String definition);
    /**
     * Returns a modified SQL command string for altering a table by adding new columns
     * @param table the name of the table
     * @param colSpec the specification of the new columns"
     */
	public abstract String		alterTableAddColumn(String table, String colSpec) throws SQLException;
    /**
     * Returns a modified SQL command string for altering a table by modifying existed columns
     * @param table the name of the table
     * @param colSpec the new specification of the columns"
     */
	public abstract String		alterTableSetColumnType(String table, String colSpec) throws SQLException;
	/** Deletes a table from the database if exists.
	 * @param tableName the name of the table
	 */
	public abstract void		deleteTableIfExists(String tableName) throws SQLException;
    /**
     * Returns an SQL command string for creating a sequence that starts from 0
     * and increments by 1, having integer values. 
     */
    public abstract String		createSequence(String sequenceTable) throws SQLException;
    /** Deletes a sequence from the database, if exists.
     * @param sequenceTable the name of the sequence
     */
    public abstract void		deleteSeqIfExists(String sequenceTable) throws SQLException;
	/** Returns the next value of a sequence. 
	 * @param sequenceTable the name of the sequence
	 * @param st SQL statement
	 */
    public abstract long		getSeqNext(String sequenceTable, java.sql.Statement st) throws SQLException;
    /** Returns a modified SQL command string for using the next value of a sequence.
     * @param sequenceTable the name of the sequence
     */
	public abstract String		selectSeqNext(String sequenceTable) throws SQLException;

    /** Generated values must start at 0, increment by 1, and the column must be primary key */
	public abstract String		autoIncrementColumn(String colname);
	/** Returns whether the current dialect is for an HSQLDB engine or not. */
    public abstract boolean	isHSQLDB();
    /** Returns the name of the database. */
    public abstract String		getDbName() throws SQLException;

    /** Returns the maximal length of the VARCHAR type. */
	public abstract int		getMaxVarcharLength();
    /** Returns the maximal length of a column identifier. */
	public abstract int		getMaxColumnNameLength();
    /** Returns the maximal length of a column identifier in the human name - SQL name mapping tables. */
	public abstract int		getMaxColumnNameLengthInColMap();
	/** Returns 'columName' between quote strings. */
	public abstract String		quote(String columnName) throws SQLException;
	/** Returns the string representation of the 'javaSQLType'. */
	public abstract String		getSQLType(int javaSQLType);

	/** Returns whether an error is specified by the parameters or not. */  
	public abstract boolean	isError(SQLException e, SQLState st);

	public abstract boolean isValidConnection(Connection conn) throws SQLException;
	
	//---------------------------------------------------------------------
	// Optional methods
	
	protected SQLDialect()		{ clearCache(); }

	/**
	 * This method is called by DatabaseConnection.disconnect(), 
	 * when the connection is closed and 'this' was the current SQLDialect. 
	 * It should also be called from the constructor.
	 */
	protected void clearCache() {}
	
	//=========================================================================
	// Static members

	//---------------------------------------------------------------------
	/** Creates and returns a temporary sequence. */
	public String makeTmpSeq(Statement recreate) throws SQLException {
		String ans = "TMPSEQ";
		if (recreate != null) {
			deleteSeqIfExists(ans);
			recreate.execute(createSequence(ans));
		}
		return ans;
	}
	
	//---------------------------------------------------------------------
	/** Checks whether the exception e is occured because out of memory event.
	 *  If the answer is true, it throws an OutOfMemoryException.
	 */
	public void checkOutOfMemory(SQLException e) {
		if (isError(e, SQLState.OutOfMemory)) {
			// At this point, we've nothing to lose by doing this
			System.gc();
			throw new OutOfMemoryError("Out of memory in the database engine");
		}
	}

	//---------------------------------------------------------------------
	/** Releases the statement st. */
	public static void release(Statement st) {
		if (st != null) 
			try { st.close(); } 
			catch (SQLException e) {
				Logger.logException("SQLDialect.release()", e);
				MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
			}
	}

	//---------------------------------------------------------------------
	/** Global storage of the available dialects. */
	protected static final LinkedList<SQLDialect>	sqlDialects = new LinkedList<SQLDialect>();

	//---------------------------------------------------------------------
	/**
	 * The default SQL dialect, which accepts all databases, should go to the   
	 * end of the list, therefore it must be registered _after_ all others. 
	 */
	public static void register(SQLDialect d) {
		if (!sqlDialects.contains(d))
			sqlDialects.add(d);
	}

	//---------------------------------------------------------------------
	/** Finds and returns the the dialect specified by 'address'. If it didn't
	 *  find anything, returns null.
	 */ 
	protected static SQLDialect find(String address) {
		for (SQLDialect d : sqlDialects) {
			if (d.recognize(address)) return d;
		}
		return null;
	}

	//---------------------------------------------------------------------
	/** Returns the JDBC connection to the database. */
	protected Connection getConnection() {
    	return MEMEApp.getDatabase().getConnection(); 
    }

	//---------------------------------------------------------------------
	/** Returns the string VARCHAR(len). */
	protected static String varchar(int len) {
		return String.format("VARCHAR(%d)", len);
	}
	
}
