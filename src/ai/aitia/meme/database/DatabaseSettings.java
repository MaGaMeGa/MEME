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

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.Session.SF;

/** This class represents the settings of a database connection. 
 * @author Jean-Francois
 *
 */
public class DatabaseSettings {
	public static final String DEFAULT_EXTERNAL_DATABASE_NAME = "meme";

//	static final String IS_EXTERNAL = "isExternal"; 
//	static final String LOGIN_NAME = "login"; 
//	static final String PASSWORD = "pwd";
//	static final String EXTERNAL_CONN_STR = "external";
//	static final String FILE_FOR_INTERNAL = "internal";

	//===============================================================
	//	Creation / initialization

	//---------------------------------------------------------------
	/**
	 * This method is responsible to ensure that MEMEApp.SESSION
	 * contains all necessary fields, i.e. the getter methods of
	 * 'this' will return reasonable values. 
	 */
	public void init() throws Exception {
	}

	//===============================================================
	//	Getters setters

	//---------------------------------------------------------------
	/** Returns whether we use an external database engine or not. */
	public boolean isExternal()	{ return Utils.getBooleanValue(MEMEApp.SESSION.get(SF.IS_EXTERNAL, "false")); }
	/** Returns the login name. */
	public String getLogin()		{ return MEMEApp.SESSION.get(SF.LOGIN_NAME, "sa"); }
	/** Returns the password. */
	public String getPwd()			{ return MEMEApp.SESSION.get(SF.PASSWORD, ""); }
	/** Returns the connection string of the external database engine. */
	public String getExtConnStr()	{ return MEMEApp.SESSION.get(SF.EXTERNAL_CONN_STR, "jdbc:hsqldb:hsql://localhost/"); }
	/** Returns the path to the file that contains the internal database. */
	public String getIntDbPath()	{
		return MEMEApp.SESSION.get2(SF.FILE_FOR_INTERNAL, new DefaultIntDbPathFinder());
	}

	/** The default path to the file that contains the internal database. */
	static String defIntDbPath = null;
	/** Assistant class that search the default path to the file that contains the internal database. */
	static class DefaultIntDbPathFinder {
		/** This method is only called when MEMEApp.SESSION does not contain a valid path. */  
		@Override public String toString() {
			if (defIntDbPath == null) {
				defIntDbPath = "db/meme";
				//  A kesobbiekben ezt a default-ot esetleg modositjuk majd, pl. 
				//  a user home konyvtaraban hozzon letre egy MEME/db konyvtarat 
				//  es az legyen a default v ilyesmi.
				//  Ha ennek a fgv.nek sikerul talalnia egy hasznalhato erteket, akkor azt 
				//  mentsuk le DataBaseSettings-be is (tehat Session-ba). Ha nem mukodik az,
				//  amit talalt, akkor azt nem muszaj elmenteni (bar lehet).
			}
			return defIntDbPath;
		}
	}

	//---------------------------------------------------------------
	/** Sets the connection string of the external database engine.  
	 * @param extConnStr <code>null</code> restores the factory default connection string */
	public void setToExternal(String extConnStr) {
		MEMEApp.SESSION.set(SF.IS_EXTERNAL, "true"); 
		MEMEApp.SESSION.set(SF.EXTERNAL_CONN_STR, extConnStr); 
	}

	//---------------------------------------------------------------
	/** Sets the path to the file that contains the internal database. 
	 * @param dbpath <code>null</code> restores the factory default path */
	public void setToInternal(String dbpath) {
		MEMEApp.SESSION.set(SF.IS_EXTERNAL, "false"); 
		MEMEApp.SESSION.set(SF.FILE_FOR_INTERNAL, dbpath); 
	}

	//---------------------------------------------------------------
	/** Sets the login name.  
	 * @param name <code>null</code> restores the factory default name ("sa") */
	public void setLogin(String name) {
		MEMEApp.SESSION.set(SF.LOGIN_NAME, name); 
	}

	//---------------------------------------------------------------
	/** Sets the password.
	 * @param pwd <code>null</code> restores the factory default password (the empty string) */
	public void setPwd(String pwd) {
		MEMEApp.SESSION.set(SF.PASSWORD, pwd); 
	}

	
	//===============================================================
	//	Public methods

	//-------------------------------------------------------------------------
	/** Converts path 's' from UNIX-style to the style used by the OS. */
	public static String fromUnixStylePath(String s) {
		if (java.io.File.separatorChar != '/')
			s = s.replace('/', java.io.File.separatorChar);
		return s;
	}

    //-------------------------------------------------------------------------
	/** Converts path 'dbpath' from the style used by the OS to UNIX-style. */
    public static String toUnixStylePath(String dbpath) throws Exception {
    	java.io.File f = new java.io.File(dbpath);
    	dbpath = Utils.getRootName(f.toString());
    	f = f.getParentFile();
		if (f == null || !f.exists())
			throw new Exception(String.format("cannot find \"%s\"", f.getAbsolutePath()));
		if (java.io.File.separatorChar != '/')
			dbpath = dbpath.replace(java.io.File.separatorChar, '/');
		return dbpath;
    }
    
	//TODO: Below is a hack. Database setting is mutable, should not contain
    // specific states: at least not this way.
	public static final String INTERNAL_USER = "sa";
	public static final String INTERNAL_PWD = "";

    /**
     * Restores internal database settings. 
     */
    public void restoreInternal() {
    	setLogin(INTERNAL_USER);
    	setPwd(INTERNAL_PWD);
    }
    
    public void restoreExternal() {
    	setLogin("");
    	setPwd("");
    }
}
