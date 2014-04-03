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

import java.sql.SQLException;

//---------------------------------------------------------------------
/** This class represents a transaction. */
public class Transaction
{
	/** The JDBC connection to the database. */
	java.sql.Connection 		conn;
	/** The last conn.getAutoCommit() value. */
	boolean						restoreAutocommit = false;

	public						Transaction(java.sql.Connection conn)	{ this.conn = conn; }
	public java.sql.Connection	getConnection()							{ return conn; 	}
	/** Does nothing. */
	public void				setSavepoint() throws SQLException		{ }
	
	/** Starts a transaction. */
	public void start() throws SQLException {
		restoreAutocommit = conn.getAutoCommit();
		conn.setAutoCommit(false);
	}
	/** Commits the changes. */
	public void commit() throws SQLException {
		if (restoreAutocommit) { 
			conn.commit();
			restoreAutocommit = false;
			conn.setAutoCommit(true);
		}
	}
	/** Rolls back the changes. */
	public void rollback() throws SQLException {	// does nothing if commit() has been called
		if (restoreAutocommit) { 
			conn.rollback();
			restoreAutocommit = false;
			conn.setAutoCommit(true);
		}
	}
}

