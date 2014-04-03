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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import ai.aitia.meme.utils.Utils;


public class MysqlSQLDialect extends DefaultSQLDialect {
	
	private static final String SEQ_NAME = "aitia_generated_sequence_function";
	private static final String SEQ_TABLE = "sequences";

	//=========================================================================
	// Required methods
	
	//---------------------------------------------------------------------
	// TODO: 1. called nowhere 
	// TODO: 2. what is a proper id?
	@Override
	public String	getDialectID()	{ return "MySQL"; }

	//---------------------------------------------------------------------
	@Override
	public boolean recognize(String address) {
		return (address.startsWith("jdbc:mysql:"));
	}

	//---------------------------------------------------------------------
	@Override
	public void loadDriver(String address) throws Exception {
		assert (recognize(address));
		//From the connector doc: The newInstance() call is a work around for some
        // broken Java implementations
		Class.forName("com.mysql.jdbc.Driver").newInstance();
	}

	//---------------------------------------------------------------------
	// TODO: SQLDialect's description is hard to interpret:
	// - why to call loadDriver if it might have already been done
	// - how to ensure that loadDriver is already called?
	// - what info to collect?: apparently this is done in DatabaseConnection
	@Override
	public List<String> isAcceptable(java.sql.DatabaseMetaData md) throws SQLException {
		return null;
	}

	//---------------------------------------------------------------------
	// TODO: super swallows errors:
	// - why to proceed when tables are not/partially created?
	// - the log entry is going to be misleading; copying the whole method body
	// only for circumventing this does not worth the trouble
	//public String[] createResultsTables() {}

	//---------------------------------------------------------------------
	// see above comment
	//public String[] createViewsTables() {
    /*
	public String[] createResultsTables() {
		if ( !hasTables() ) {
			return super.createResultsTables();
		}
		return new String[]{};
	}
	
	public String[] createViewsTables() {
		if ( !hasTables() ) {
			return super.createViewsTables();
		}
		return new String[]{};
	}
	*/

	//-------------------------------------------------------------------------
	// TODO: is there any specifics; hasTables()... ?
    @Override
	public String createBigTable(String definition) {
  		return "CREATE TABLE " + definition;
    }

	//---------------------------------------------------------------------
    @Override
	public String createTmpTable(String definition) {
		return "CREATE TEMPORARY TABLE " + definition;
	}

	//---------------------------------------------------------------------
	@Override
	public String alterTableSetColumnType(String table, String colSpec) throws SQLException {
		return "ALTER TABLE " + table + " MODIFY COLUMN " + colSpec;
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
    	Statement st = null;
    	
       	try {
    		st = getConnection().createStatement();
        	final String table = "CREATE TABLE " + SEQ_TABLE + " (id VARCHAR(200) NOT NULL, seq INT NOT NULL, PRIMARY KEY(id))";
        	st.execute(table);
    	} catch (final SQLException e) {
    		if (!isError(e,SQLState.TableAlreadyExists_Create))
    			throw e;
    	} finally {
    		release(st);
    	}
    	
    	try {
    		st = getConnection().createStatement();
    		st.executeQuery("SELECT " + SEQ_NAME + "()");
    	} catch (final SQLException e) {
    		if (isError(e,SQLState.FunctionNotFound_Select)) {
    			final String function = "CREATE FUNCTION " + SEQ_NAME + "(seqname varchar(200))" +
										"RETURNS INT DETERMINISTIC " + 
    									"BEGIN " +
    									"UPDATE " + SEQ_TABLE + " SET seq = seq + 1 WHERE id=seqname;" +
    									"SELECT seq INTO @result FROM " + SEQ_TABLE + " WHERE id=seqname;" +
    									"RETURN @result;" + 
    									"END ";
    			st.execute(function);

    		} else if (!isError(e,SQLState.FunctionMissingParameters_Select)) 
    			throw e;
    	} finally {
    		release(st);
    	}
    	
    	int initialValue = 0;
    	if ("IDGEN".equals(sequenceTable)) {
    		// for backward compatibility
    		try {
    			st = getConnection().createStatement();
    			final String query = "SELECT max(seq) FROM IDGEN";
    			final ResultSet rs = st.executeQuery(query);
    			rs.next();
    			initialValue = rs.getInt(1);
    		} catch (final SQLException e) {
    			if (!isError(e,SQLState.BaseTableNotFound_Select))
    				throw e;
    		}
    	}
    	
//    	try {
//    		st = getConnection().createStatement();
//    		final String cmd = "DELETE FROM " + SEQ_TABLE + " WHERE id='" + sequenceTable + "'";
//    		st.executeUpdate(cmd);
//    	} finally {
//    		release(st);
//    	}
    	return "INSERT INTO " + SEQ_TABLE + " (id,seq) VALUES ('" + sequenceTable + "'," + initialValue + ")";
    }

	//---------------------------------------------------------------------
    @Override
	public void deleteSeqIfExists(String sequenceTable) throws SQLException {
		Statement st = null;
		try {
    		st = getConnection().createStatement();
    		final String cmd = "DELETE FROM " + SEQ_TABLE + " WHERE id='" + sequenceTable + "'";
    		st.executeUpdate(cmd);
		} finally {
			release(st);
		}
    }

	//---------------------------------------------------------------------
	@Override
	public long getSeqNext(String sequenceTable, java.sql.Statement st) throws SQLException {
		ResultSet rs = st.executeQuery("SELECT " + SEQ_NAME + "('" + sequenceTable + "')");
		rs.next();
		long ans = rs.getLong(1);
		rs.close();
		return ans;
	}

	//---------------------------------------------------------------------
	@Override
	public String selectSeqNext(String sequenceTable) {
//		Statement st = null;
//		String s = "";
//		try {
//			st = getConnection().createStatement();
////			s = "" + getSeqNext(sequenceTable, st);
//			
//			st.close(); 
//			st = null;
//		} catch (SQLException e) {
//			MEMEApp.logException(e);
//		} finally {
//			release(st);
//		}
//		return s;
		return SEQ_NAME + "('" + sequenceTable + "')";
	}


	//-------------------------------------------------------------------------
	@Override
	public String autoIncrementColumn(String colname) {
		return colname + " INT NOT NULL AUTO_INCREMENT PRIMARY KEY";
	}


    //---------------------------------------------------------------------
    @Override
	public String getDbName() throws SQLException {
		// this is the standard way
		return getConnection().getMetaData().getURL();
	}


	//---------------------------------------------------------------------
	// TODO: OutOfMemory
	@Override
	public boolean isError(SQLException e, SQLState st) {
		switch (st) {
			case BaseTableNotFound_Select			: return "42S02".equals(e.getSQLState());
			case BaseTableNotFound_Drop				: return "42S02".equals(e.getSQLState()); 
			case SequenceNotFound_Drop				: return "42S02".equals(e.getSQLState());
			case ColumnNotFound_Select				: return "42S22".equals(e.getSQLState());
			case TableAlreadyExists_Create			: return "42S01".equals(e.getSQLState());
			case SequenceAlreadyExists_Create		: 
				return "23000".equals(e.getSQLState()) && Math.abs(e.getErrorCode()) == 1062;
			case OutOfMemory						:
				// 72 == org.hsqldb.Trace.OUT_OF_MEMORY
				return "S1000".equals(e.getSQLState()) && Math.abs(e.getErrorCode()) == 1037;
			case FunctionNotFound_Select			:
				return "42000".equals(e.getSQLState()) && Math.abs(e.getErrorCode()) == 1305;
			case FunctionMissingParameters_Select	:
				return "42000".equals(e.getSQLState()) && Math.abs(e.getErrorCode()) == 1318;
		}
		return false;
	}

	@Override
	/**
	 *  The mysql-connector-java-5.1.6-bin.jar returns erronous value(<13) that
	 *  prevents this dialect from working.
	 *  <P>
	 *  Documentation claims value of 64 
	 *  http://dev.mysql.com/doc/refman/5.0/en/identifiers.html
	 */
	public int getMaxColumnNameLength() {
		return 64;
	}
	
	@Override
	public boolean isValidConnection(final Connection conn) throws SQLException {
		return conn != null && conn.isValid(0);
	}
	
	@SuppressWarnings("unused")
	private void createCatalog() throws SQLException {
		Statement st = null;
		try {
			DatabaseMetaData md = getConnection().getMetaData();
			st = getConnection().createStatement();
			st.executeUpdate("CREATE DATABASE " + Utils.getCatalogName(md.getURL()));
			st.close(); st = null;
		} finally {
			release(st);
		}
	}
	
	/**
	 * Checks if tables are already created. Implemented by examining "MODELS"
	 * table.
	 * @return true if tables already created
	 */
	@SuppressWarnings("unused")
	private boolean hasTables() {
		try {
			DatabaseMetaData md = getConnection().getMetaData();
			ResultSet rs = md.getTables(Utils.getCatalogName(md.getURL()), null, "MODELS", null );
			return rs.next();
		}
		catch(SQLException e) {
			return false;
		}
	}
}
