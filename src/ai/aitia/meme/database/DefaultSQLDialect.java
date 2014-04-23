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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import ai.aitia.meme.Logger;

/**
 * Default implementation for nonstandard SQL commands.
 * 
 * This class provides a default implementation for the HSQLDB database engine.
 * Other implementations should derive from this class.
 */
public class DefaultSQLDialect extends SQLDialect 
{
	//=========================================================================
	// Required methods
	
	//---------------------------------------------------------------------
	@Override
	public String	getDialectID()	{ return "HSQLDB 1.8.0"; }

	//---------------------------------------------------------------------
	@Override
	public boolean recognize(String address) {
		return (address.startsWith("jdbc:hsqldb:"));
	}

	//---------------------------------------------------------------------
	@Override
	public void loadDriver(String address) throws Exception {
		assert (recognize(address));

		Class.forName("org.hsqldb.jdbcDriver");
	}

	//---------------------------------------------------------------------
	@Override
	public List<String> isAcceptable(java.sql.DatabaseMetaData md) throws SQLException {
		// Since this is the default SQLDialect, it accepts everything.
		return null;
//		if (md.getDriverName().contains("HSQL Database Engine")
//				&& 0 <= md.getDriverVersion().compareTo("1.8.0")) 
//			return true;
//
//		return false;
	}

	//---------------------------------------------------------------------
	@Override
	public String[] createResultsTables() {
    	String[] ans = null;
    	try {
	    	String longvarchar = getSQLType(java.sql.Types.LONGVARCHAR);
	    	String bigint = getSQLType(java.sql.Types.BIGINT);
	    	assert(0 < longvarchar.length() && 0 < bigint.length());
	    	String Column = quote("Column") + ' ' + varchar(getMaxColumnNameLengthInColMap()); 
	    	String tmp[]  = {
	    			createSequence("IDGEN"),

					"CREATE TABLE MODELS (Model_id " + bigint + " PRIMARY KEY," +
	                					 "Name     " + varchar(MAX_MODEL_NAME_LEN) + " NOT NULL," +
						                 "Version  " + varchar(MAX_MODEL_VER_LEN)  + " NOT NULL," +
						                 "Description " + longvarchar + ',' +
						                 "UNIQUE (Name, Version) )",

					"CREATE TABLE RESULTS_I_COLUMNS (ID     " + bigint + " NOT NULL," +
						                 			"Name   " + varchar(MAX_COLNAME_LEN_IN_COLMAP) + " NOT NULL," +
						                 			 Column + " NOT NULL," +
						                 			"Type SMALLINT NOT NULL," +
						                 			"A SMALLINT NOT NULL," + 
						                 			"B SMALLINT NOT NULL," +
												    "FOREIGN KEY (ID) REFERENCES MODELS (Model_id) )", 

					"CREATE TABLE RESULTS_O_COLUMNS (ID     " + bigint + " NOT NULL," +
	                           						"Name   " + varchar(MAX_COLNAME_LEN_IN_COLMAP) + " NOT NULL," +
						                 			 Column + " NOT NULL," +
						                 			"Type SMALLINT NOT NULL," +
						                 			"A SMALLINT NOT NULL," + 
						                 			"B SMALLINT NOT NULL," +
												    "FOREIGN KEY (ID) REFERENCES MODELS (Model_id) )", 

   					"CREATE TABLE BATCH_DESCRIPTION (Model_id " + bigint + ',' +
   													"Batch INT," +
						                 			"Description " + varchar(MAX_BATCH_DESC_LEN) + ',' +
						                 			"PRIMARY KEY (Model_id, Batch) )"
	    	};
	    	ans = tmp;
    	} catch (Exception e) {
    		Logger.logException("DefaultSQLDialect.createResultsTables()", e);
    	}
    	return ans;
	}

	//---------------------------------------------------------------------
	@Override
	public String[] createViewsTables() {
    	String[] ans = null;
    	try {
	    	final String longvarchar = getSQLType(java.sql.Types.LONGVARCHAR);
	    	final String bigint = getSQLType(java.sql.Types.BIGINT);
	    	assert(0 < longvarchar.length() && 0 < bigint.length());
	    	final String Column = quote("Column") + ' ' + varchar(getMaxColumnNameLengthInColMap()); 
	    	assert(0 < longvarchar.length());
	    	final String tmp[]  = {
	    			"CREATE TABLE  VIEWS        (View_id " + bigint + " PRIMARY KEY," +
	    										"Name    " + varchar(MAX_VIEW_NAME_LEN) + " NOT NULL," +
	    										"Description " + longvarchar + ',' +
	    										"Creation_rule " + longvarchar + ',' +
	    										"UNIQUE (Name) )",
	
					"CREATE TABLE  VIEW_COLUMNS (ID     " + bigint + " NOT NULL," +
												"Name   " + varchar(MAX_COLNAME_LEN_IN_COLMAP) + " NOT NULL," +
						                 		 Column+" NOT NULL," +
						                 		"Type SMALLINT NOT NULL," +
						                 		"A SMALLINT NOT NULL," + 
						                 		"B SMALLINT NOT NULL," +
											    "FOREIGN KEY (ID) REFERENCES VIEWS (View_id) )" 
	    	};
	    	ans = tmp;
    	} catch (Exception e) {
    		Logger.logException("DefaultSQLDialect.createViewsTables()", e);
    	}
    	return ans;
	}

	//-------------------------------------------------------------------------
	@Override
    public String createBigTable(String definition) {
    	return (isHSQLDB() ? "CREATE CACHED TABLE " : "CREATE TABLE ") + definition;
    }

	//---------------------------------------------------------------------
	@Override
    public String createTmpTable(String definition) {
		return "CREATE TEMPORARY TABLE " + definition + " ON COMMIT PRESERVE ROWS";
		//return "CREATE CACHED TABLE" + definition;	// for debugging
	}

	//---------------------------------------------------------------------
	@Override
	public String alterTableAddColumn(String table, String colSpec) throws SQLException {
		return "ALTER TABLE " + table + " ADD COLUMN " + colSpec;
		//return "ALTER TABLE " + table + " ADD COLUMN " + c.compose(idx);
	}

	//---------------------------------------------------------------------
	@Override
	public String alterTableSetColumnType(String table, String colSpec) throws SQLException {
		return "ALTER TABLE " + table + " ALTER COLUMN " + colSpec;
		//return "ALTER TABLE " + table + " ALTER COLUMN " + c.compose(idx);
	}

	//-------------------------------------------------------------------------
	@Override
	public void deleteTableIfExists(String tableName) throws SQLException {
		Statement st = null;
		try {
			st = getConnection().createStatement();
			st.executeUpdate("DROP TABLE " + tableName);
			st.close(); st = null;
		} catch (SQLException e) {
			if (!isError(e, SQLState.BaseTableNotFound_Drop))
				throw e;
		} finally {
			release(st);
		}
	}

	//---------------------------------------------------------------------
	@Override
    public String createSequence(String sequenceTable) throws SQLException {
		return "CREATE SEQUENCE " + sequenceTable;
    }

	//---------------------------------------------------------------------
	@Override
    public void deleteSeqIfExists(String sequenceTable) throws SQLException {
		Statement st = null;
		try {
			st = getConnection().createStatement();
			st.executeUpdate("DROP SEQUENCE " + sequenceTable);
			st.close(); st = null;
		} catch (SQLException e) {
			if (!isError(e, SQLState.SequenceNotFound_Drop))
				throw e;
		} finally {
			release(st);
		}
    }

	//---------------------------------------------------------------------
	@Override
	public long getSeqNext(String sequenceTable, java.sql.Statement st) throws SQLException {
		ResultSet rs = st.executeQuery("CALL NEXT VALUE FOR " + sequenceTable);
		rs.next();
		long ans = rs.getLong(1);
		if (rs != null)
			rs.close();
		return ans;
	}

	//---------------------------------------------------------------------
	@Override
	public String selectSeqNext(String sequenceTable) {
		return "NEXT VALUE FOR " + sequenceTable;
	}


	//-------------------------------------------------------------------------
	@Override
	public String autoIncrementColumn(String colname) {
		return colname + " INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 0) PRIMARY KEY";
//		return colname + " INTEGER GENERATED BY DEFAULT AS IDENTITY(START WITH 1) PRIMARY KEY";
	}

    //-------------------------------------------------------------------------
	@Override
    public boolean isHSQLDB() {
    	try {
    		return getConnection().getMetaData().getDriverName().contains("HSQL Database Engine");
    	} catch (Throwable t) {
    		return false;
    	}
    }

    //---------------------------------------------------------------------
	@Override
    public String getDbName() throws SQLException {
		java.sql.Statement st = getConnection().createStatement();
		ResultSet rs = null;
		try {
			rs = st.executeQuery("CALL DATABASE()");
			rs.next();
			return rs.getString(1);
		} finally {
			if (rs != null) rs.close();
			if (st != null) st.close();
		}
	}

    //---------------------------------------------------------------------
	@Override
	public int getMaxVarcharLength() {
		if (maxVarcharLength == null) {
			try {
				ResultSet r = getConnection().getMetaData().getTypeInfo();
				int data_type = r.findColumn("DATA_TYPE");
				while (r.next()) {
					if (r.getInt(data_type) == java.sql.Types.VARCHAR) {
						int ans = r.getInt("PRECISION");
						if (ans == 0) ans = Integer.MAX_VALUE;
						maxVarcharLength = new Integer(ans);
						break;
					}
				}
			} catch (SQLException e) {
				return 0;
			}
		}
		return maxVarcharLength.intValue();		
	}

	//---------------------------------------------------------------------
	@Override
	public int getMaxColumnNameLength() {
		if (maxColumnNameLength < 0) {
			try {
				maxColumnNameLength = getConnection().getMetaData().getMaxColumnNameLength();
				if (maxColumnNameLength == 0) maxColumnNameLength = Integer.MAX_VALUE;
			} catch (Throwable t) {
				Logger.logException("DatabaseConnection.getMaxColumnNameLength()", t);
				return 0;
			}
		}
		return maxColumnNameLength;
	}

	//---------------------------------------------------------------------
	@Override
	public int getMaxColumnNameLengthInColMap() {
		if (maxColumnNameLengthInColMap <= 0) {
			Integer tmp[] = {64, getMaxVarcharLength(), getMaxColumnNameLength()}; 
			maxColumnNameLengthInColMap = java.util.Collections.min(java.util.Arrays.asList(tmp)); 
		}
		return maxColumnNameLengthInColMap;
	}

	//---------------------------------------------------------------------
	@Override
	public String quote(String columnName) throws SQLException {
		if (identifierQuoteString == null) {
			identifierQuoteString = getConnection().getMetaData().getIdentifierQuoteString();
			if (identifierQuoteString == null)
				return columnName;
		}
		return identifierQuoteString + columnName + identifierQuoteString;  
	}

	//---------------------------------------------------------------------
	@Override
	public String getSQLType(int javaSQLType) {
		try {
			ResultSet r = getConnection().getMetaData().getTypeInfo();
			int data_type = r.findColumn("DATA_TYPE");
			while (r.next()) {
				if (r.getInt(data_type) == javaSQLType) 
					return r.getString("TYPE_NAME");
			}
		} catch (SQLException e) {
		}
		return "";
	}

	//---------------------------------------------------------------------
	@Override
	public boolean isError(SQLException e, SQLState st) {
		switch (st) {
			case BaseTableNotFound_Select		: return "S0002".equals(e.getSQLState());
			case BaseTableNotFound_Drop			: return "S0002".equals(e.getSQLState()); 
			case SequenceNotFound_Drop			: return "S0002".equals(e.getSQLState());
			case ColumnNotFound_Select			: return "S0022".equals(e.getSQLState());
			case TableAlreadyExists_Create		: return "S0001".equals(e.getSQLState());
			case SequenceAlreadyExists_Create	: 
				// 192 == org.hsqldb.Trace.SEQUENCE_ALREADY_EXISTS
				return "S1000".equals(e.getSQLState()) && Math.abs(e.getErrorCode()) == 192;
			case OutOfMemory					:
				// 72 == org.hsqldb.Trace.OUT_OF_MEMORY
				return "S1000".equals(e.getSQLState()) && Math.abs(e.getErrorCode()) == 72;
		}
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean isValidConnection(final Connection conn) throws SQLException {
		// because HSQLDB does not support this operation
		return true;
	}


	//=========================================================================
	//	Internals
	
	//	Variables
	//	Cached information about the connection. See clearCache().
	private Integer		maxVarcharLength;
	private int		maxColumnNameLength;
	private int		maxColumnNameLengthInColMap;
	private String		identifierQuoteString;

    //-------------------------------------------------------------------------
	@Override
	protected void clearCache() {
		maxVarcharLength	= null;
		maxColumnNameLength	= -1;
		maxColumnNameLengthInColMap = -1;
		identifierQuoteString= null;
    }

}
