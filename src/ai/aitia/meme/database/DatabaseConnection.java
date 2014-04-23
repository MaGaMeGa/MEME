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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.events.Event;
import ai.aitia.meme.utils.Utils;

/** This class represents a database connection. 
 * @author Robert
 *
 */
public class DatabaseConnection 
{
    /** The JDBC connection object. */
	private Connection	conn				= null;
	/** Flag that indicates that the connection should be shutdown. */
    private boolean	shouldBeShutdown	= false;
    /** The dialect unique  of the used database engine. We use this to handle nonstandard
     *  SQL commands.*/
   	private SQLDialect	dialect				= null;

	/** Storage for listeners which observe database connection events. */ 
   	public static class Listeners extends Event<IConnChangedListener, ConnChangedEvent> {
   		Listeners() { super(IConnChangedListener.class, ConnChangedEvent.class); }
		@Override protected void fire(ConnChangedEvent msg) { super.fire(msg); }
   	}

    /**
     * This event is fired after connecting to a new database (which may be empty),
     * even if the connection attempt has failed.
     * Listeners are the LocalAPI (to create tables), GUI components etc.  
     */	
    public final Listeners connChanged = new Listeners();
    
	// 'maxVarcharLengthCache' caches the result of SQLDialect.getMaxVarcharLength()
	// because it is needed very frequently. The cached value must be updated  
	// whenever the database connection changes.
    static Integer maxVarcharLengthCache = 64;

	//=========================================================================
	// Constructor

    public DatabaseConnection() {
        connChanged.addListener(new IConnChangedListener() {
			public void onConnChange(ConnChangedEvent event) {
				if (dialect != null) maxVarcharLengthCache = dialect.getMaxVarcharLength();
			}
        });
    }	

	//=========================================================================
	// Public methods

    //-------------------------------------------------------------------------
    /**
     * Creates (or replaces) the JDBC connection to the database engine, and
     * notifies the listeners that are registered in connChanged.
     * After this method, use getConnection() to retrieve the created connection.
     * Note that getConnection() will return null if all connection attempts failed
     * (in which case this method throws an exception).
     */
    public void connect(DatabaseSettings settings) throws Exception
    {	
//    	if (getConnection() != null)
    	if (conn != null)
    		shutdown();
    	try {
			if (settings.isExternal()) {
				//String err = connectToServer("", settings);
				String err = connectToServer(settings.getExtConnStr(), settings);
				if (0 != err.length()) {
					Logger.logError("Connection to external server failed: %s", err);
					Logger.logError("Fall back to Stand Alone Mode");
					launchInternal(settings);	
				}
			}
			else {
				launchInternal(settings);
			}
			checkCapabilities();
    	} finally {
			connChanged.fire(new ConnChangedEvent(this));
    	}
    }

    //-------------------------------------------------------------------------
    /**
     * Returns the JDBC connection to the database engine.
     *
     */
    public Connection getConnection() {
    	if (conn == null) return null;
    	try {
	    	if (!dialect.isValidConnection(conn))
	    		connect(MEMEApp.getDbSettings());
    	} catch (final Exception e) {
    		return null;
    	}
    	return conn; 
    }

    //-------------------------------------------------------------------------
    public SQLDialect getSQLDialect() {
    	return dialect; 
    }

    //-------------------------------------------------------------------------
    /**
     * Closes the database connection.
     *
     */
    public void disconnect() {
    	try {
    		if (conn != null) {
    			conn.close();    // if there are no other open connection
    			conn = null;
		        Logger.logError("Connection closed");
    		}
    	} catch (SQLException e) {
	        conn = null;
	        Logger.logError("Error while disconnecting database: %s", e.getMessage());
    	}
    	if (dialect != null) {
    		dialect.clearCache();
    		dialect = null;
    	}
    }
 
    //-------------------------------------------------------------------------
    /**
     * Closes the database connection and, if necessary, shuts down the DBMS too.
     */
    public void shutdown() {
    	if (shouldBeShutdown && conn != null) {
    		// This is executed for HSQLDB only
    		// (if you change it in the future, then move shutdown behind SQLDialect)
    		Logger.logError("Shutting down database engine..");
    		try {
	    		Statement st = conn.createStatement();

	        	// db writes out to files and performs clean shutdown
	        	// otherwise there will be an unclean shutdown
	        	// when program ends
	        	st.execute("SHUTDOWN");
	        	shouldBeShutdown = false;
	        	Logger.logError("done");  
    		}
    		catch (SQLException e) {
    			Logger.logError("error: %s", e.getMessage());
    		}
    	}
    	disconnect();
    }
    

    //=========================================================================
	//	DBMS characteristics

    
    //=========================================================================
	// Private methods

    //-------------------------------------------------------------------------
	/**
	 * Checks the basic capabilties of the new database
	 */
	protected void checkCapabilities() 
	{
		String msg = null; 
//		if (getConnection() == null) {
		if (conn == null) {
			msg = Utils.htmlPage("Cannot connect to the database!<br>Please modify database settings " +
								"in <em>File/Database settings</em>, or restart the program!"); 
		}
		else {		
			LinkedList<String> problems = new LinkedList<String>();
			LinkedList<String> warnings = new LinkedList<String>();
			try {
//				java.sql.DatabaseMetaData md = getConnection().getMetaData();
				java.sql.DatabaseMetaData md = conn.getMetaData();
				assert(dialect != null);
				java.util.List<String> tmp = dialect.isAcceptable(md);
				if (tmp != null) problems.addAll(tmp);

				if (!md.supportsAlterTableWithAddColumn())		problems.add("does not support ALTER TABLE with ADD COLUMN");
				if (!md.supportsGroupBy())						problems.add("does not support GROUP BY");
				if (!md.supportsNonNullableColumns())			problems.add("does not support NOT NULL columns");
				//if (!md.supportsUnion())						problems.add("does not support UNION");
				if (dialect.getMaxColumnNameLength() < 13)		problems.add("supports too short column names only");
				if (dialect.getSQLType(java.sql.Types.VARCHAR).length() == 0)		problems.add("does not support VARCHAR");
				if (dialect.getSQLType(java.sql.Types.LONGVARCHAR).length() == 0)	problems.add("does not support LONGVARCHAR");
				if (dialect.getSQLType(java.sql.Types.BIGINT).length() == 0)		problems.add("does not support BIGINT");
				if (dialect.getSQLType(java.sql.Types.DOUBLE).length() == 0)		problems.add("does not support DOUBLE");
				if (!md.supportsTransactions())					warnings.add("does not support transactions");
				if (!md.supportsDataDefinitionAndDataManipulationTransactions()
					|| md.supportsDataManipulationTransactionsOnly())
																warnings.add("provides only limited support for transactions");
				if (!atLeast(md.getMaxColumnsInOrderBy(), 2))	problems.add("does not allow more than 1 column in ORDER BY");
				if (!atLeast(md.getMaxColumnsInSelect(), 2))	problems.add("does not allow more than 1 column in SELECT");
				if (!atLeast(md.getMaxColumnsInTable(), 4))		problems.add("does not allow more than 3 columns per table");
				if (!atLeast(md.getMaxTableNameLength(), 27))	problems.add("supports too short table names only");
				if (!atLeast(md.getMaxStatements(), 3))			problems.add("does not allow 3 or more open Statements simulaneously");
				// TODO: kellenek meg SEUQENCE-k is, es 3 helyett lehet hogy 5 Statement is is kell?
			} catch (Exception e) {
				problems.add("throws exception while checking capabilities: " + Utils.getLocalizedMessage(e));
				Logger.logExceptionCallStack("DatabaseConnection.checkCapabilities()", e);
			}
			if (!warnings.isEmpty()) {
				if (warnings.size() > 1) warnings.add(0, "");
				Logger.logError("Warning: the database server %s", Utils.join(warnings, "\n- "));
			}
			if (!problems.isEmpty()) {
				if (problems.size() > 1) problems.add(0, "");
				Logger.logError("ERROR: this database server %s", Utils.join(problems, "\n- "));
				msg = "Incompatible database! The database server does not support " +
							"all necessary SQL+JDBC functionality.";
				shutdown();	// - do not use this database: it is not compatible
			}
		}
		if (msg != null) {
			Utils.invokeLater(MEMEApp.class, "userAlert", msg);
		}
	}

	//-------------------------------------------------------------------------
	/**
	 * Attempts to connect to an external database server. 
	 * @param connStr JDBC connection string, e.g. "jdbc:hsqldb:hsql://127.0.0.1/" .
	 *                In case of empty string the current database setting is used ("address="). 
	 */
    private String connectToServer(String connStr, DatabaseSettings settings) {
    	if (connStr == null || connStr.length() == 0)
    		connStr = settings.getExtConnStr();

    	try{
        	SQLDialect newdialect = SQLDialect.find(connStr);
        	if (newdialect == null) {
        		return String.format("Cannot interpret connection string \"%s\"", connStr);
        	}
        	newdialect.loadDriver(connStr);

    		conn = DriverManager.getConnection(connStr, settings.getLogin(), settings.getPwd());
    		dialect = newdialect;
			shouldBeShutdown = false;

			if (dialect.isHSQLDB())  
				setBigDBIfNeed();
			
    		Logger.logError("Connection succeeded: %s", connStr);
			return "";
		} catch(Exception e) {
			return Utils.getLocalizedMessage(e);
		}
    }

	//-------------------------------------------------------------------------
    /** Used only with HSQLDB server. It tries to set the data file size limit to
     *  the maximum value (8 Gb).
     */
    private void setBigDBIfNeed() {
    	assert(dialect.isHSQLDB());
    	try {
			Statement st = conn.createStatement();
			st.executeUpdate("SET PROPERTY \"hsqldb.cache_file_scale\" 8");
			st.executeUpdate("CHECKPOINT"); // use the new settings
			st.close();
		} catch (SQLException e) {
			// if the database already has cached table(s) => do nothing
		}
    }

	//-------------------------------------------------------------------------
    /** Connects to the built-in database engine. */
    private void launchInternal(DatabaseSettings settings) throws Exception {
		String err = "";
		Exception cause = null;

		// Set it to true to avoid separate-process engine in debug mode
		boolean always_inProcessEngine = true;
//		always_inProcessEngine = false;

    	if (!MEMEApp.isDebugVersion() || always_inProcessEngine) {
    		// "Release mode"
    		// Start the internal database engine in "in-process" mode. 
    		// This will load the db files and start the database if it is not running already.
    		String path = null;
    		try {
    			path = DatabaseSettings.toUnixStylePath(settings.getIntDbPath());
    			err = connectToServer("jdbc:hsqldb:file:" + path, settings);
    		} catch (Exception e) {
    			cause = e;
    			if (err.length() == 0)
    				err = Utils.getLocalizedMessage(cause);
    		}
			if (err.length() != 0 || cause != null) {
				err = "Connection to stand-alone (in-process) database engine failed: " + err;
				throw (cause == null) ? new Exception(err) : new Exception(err, cause);
			}
			shouldBeShutdown = true;
    		return;
    	}

    	// Debug mode: launch the server in a separate process 
    	// 	           or use it at localhost if it is already launched.

    	final String internalDbEngineInSeparateProcess = "jdbc:hsqldb:hsql://localhost/"; 

    	// If the server has already been started (e.g. by the previous run of this program),
    	// use the running instance, do not launch a new one!
    	if (connectToServer(internalDbEngineInSeparateProcess, settings).length() == 0) {
    		return;
    	}

    	// The server is not running - launch it in a separate process and then connect to it
		try {
			final int CONN_TIMEOUT = 20;	// seconds 
			MEMEApp.LONG_OPERATION.progress(-1, CONN_TIMEOUT);

			Logger.logError("Launching a server in a separate process..");
			// Note: the following command is specific to the Windows platform, 
			// but this should be no problem because this is executed only during development,
			// which is normally done under Windows.
			// 'cmd /c start' is necessary to avoid buffering the standard output of the server.
			// Without this, exec() redirects the standard output of the new process into a stream
			// and when the stream's buffer gets full, the server process becomes blocked - and 
			// thus cannot accept connections.
			// The following workaround allocates a new DOS window for the server process
			// and thus exec() only catches the output of the 'cmd /c start' command, which 
			// completes at the moment when the server process is started.
			String cmd = "cmd /c start \"\" /MIN java -Xmx256m -cp 3rdParty/hsqldb/lib/hsqldb.jar org.hsqldb.Server " +
							"-dbname.0 \"\" -database.0 \"" + DatabaseSettings.toUnixStylePath(settings.getIntDbPath()) + "\"";
			Logger.logError("%s", cmd);
			Runtime.getRuntime().exec(cmd);

			Logger.logError("Server launching succeeded");
			for (int i = 0; i < CONN_TIMEOUT; ++i) {
				java.lang.Thread.sleep(1000);
				MEMEApp.LONG_OPERATION.progress(i+1);
				err = connectToServer(internalDbEngineInSeparateProcess, settings);
				if (err.length() == 0) break;
			}
			shouldBeShutdown = true;
		}
		catch (InterruptedException e) {}
		catch (Exception e) {
			cause = e;
			if (err.length() == 0)
				err = Utils.getLocalizedMessage(cause);
		}
		if (err.length() != 0 || cause != null) {
			err = "Cannot connect to the locally launched server: " + err;
			throw (cause == null) ? new Exception(err) : new Exception(err, cause);
		}
    }

    //-------------------------------------------------------------------------
    /** Returns true if <code>actual == 0 || actual >= required</code> */
	public static boolean atLeast(int actual, int required) {
		return (actual == 0) || (actual >= required);
	}

}
