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
package ai.aitia.meme;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import ai.aitia.meme.events.ProgramState;
import ai.aitia.meme.utils.Utils;

//-----------------------------------------------------------------------------
/**
 * Session class for storing user defined settings of the application.
 * Notes:
 * - load() and save() use gzip to discourage users from modifying the properties
 *   file manually
 * - the test() method can be used as a command-line tool to print or update
 *   the coded data in the registry. It only works with the registry because 
 *   for in-file sessions (.mes files) it is unnecessary, for example:
 *     gzip -d -c file.mes >output.txt
 */
public class Session 
{
	public static final String FMT_VERSION			= "1.0"; // increase this when you change the format or structure

	/** Session field names. */
	public static class SF {
		public static final String FMT_VERSION_KEY	= "$SESSION_FMT_VERSION$";

		// Fields for database settings
		public static final String IS_EXTERNAL		= "isExternal"; 
		public static final String LOGIN_NAME			= "login"; 
		public static final String PASSWORD			= "pwd";
		public static final String EXTERNAL_CONN_STR	= "external";
		public static final String FILE_FOR_INTERNAL	= "internal";
	};

	/** Special flag that determines whether the settings change during the current session of the application or not. */
	public final ProgramState.CompCtrl<Boolean> dirty;

	// Invariant: (saved==null || saved.equals(data)) means that there was no real modification in 'data'
	//            since the last save/load. Otherwise 'saved' stores the last saved/loaded state.
	/** It stores the current settings. */ 
	java.util.Properties	data;
	/** It stores the last saved settings. */
	java.util.Properties   saved;
	/** The session file. */
	File					file = null;

	//-------------------------------------------------------------------------
	public Session() {
		dirty = MEMEApp.PROGRAM_STATE.registerParameter(new ProgramState.CompCtrl<Boolean>() {
			public Boolean getValue()		{ return isDirty(); }
			@Override public Object getID()	{ return "Session.change"; }
		});

		data  = init(new java.util.Properties());
		saved = null;
	}

	//-------------------------------------------------------------------------
	/** Initializes and returns the Properties object <code>p</code>. */
	private java.util.Properties init(java.util.Properties p) {
		p.clear();
		p.setProperty(SF.FMT_VERSION_KEY, FMT_VERSION);
		return p;
	}

	//-------------------------------------------------------------------------
	public synchronized File setFile(File f) {
		File ans = file; file = f; return ans; 
	}

	//-------------------------------------------------------------------------
	public synchronized File getFile() {
		return file;
	}
	
	//-------------------------------------------------------------------------
	public synchronized boolean isDirty() {
		boolean ans = (saved == null) ? false : !saved.equals(data);
		if (!ans && saved != null)
			saved = null;
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Loads the settings from file <code>file</code>. If <code>file</code> is null or
	 *  doesn't exists, it loads the settings from the registry. */
	public synchronized void load() throws IOException {
		java.io.InputStream is;
		boolean fileIsOK = true;
		if (file != null && (fileIsOK = file.exists()) ) {
			is = new FileInputStream(file);
		} else {
			byte[] data = MEMEApp.userPrefs.getByteArray(UserPrefs.SESSION, Utils.emptyGzip()); 
			is = new ByteArrayInputStream(data);
		}
		is = new GZIPInputStream(is);
		try {
			java.util.Properties tmp = new java.util.Properties();
			init(tmp).load(is);
			data = tmp;
			saved = (fileIsOK) ? null : /*intentionally empty:*/ new java.util.Properties();
			dirty.fireLater();
		} finally { 
			is.close();
		}
	}

	//-------------------------------------------------------------------------
	/** Saves the settings to the file <code>file</code>. If <code>file</code> is null or
	 *  doesn't exists, it saves the settings to the registry. */
	public synchronized void save() throws IOException {
		java.io.OutputStream os0 = (file != null) ? new FileOutputStream(file) : new ByteArrayOutputStream();  
		java.io.OutputStream os = new GZIPOutputStream(os0);
		try {
			data.store(os, "");
		} finally {
			os.close();
		}
		saved = null;
		dirty.fireLater();
		if (file == null) {
			ByteArrayOutputStream b = (ByteArrayOutputStream)os0;
			MEMEApp.userPrefs.putByteArray(UserPrefs.SESSION, b.toByteArray());
		}
	}

	//-------------------------------------------------------------------------
	/** It tries to save the settings to the file <code>f</code>. If the saving
	 *  operation is successful then file object of the session will be <code>f</code>.
	 * @param f the new file
	 */
	public void tryToSave(java.io.File f) throws IOException {
		if (f == null)
			throw new NullPointerException();
		java.io.File tmp = file;
		try {
			file = f;
			save();
			tmp = f;	// ha sikerult a mentes akkor 'file' ezen a fajlon marad
		} finally {
			file = tmp;
		}
	}

	//-------------------------------------------------------------------------
	public synchronized String get(String key, String defValue) {
		return data.getProperty(key, defValue);
	}

	//-------------------------------------------------------------------------
	/**
	 * This version of get() allows you to save calling defValue.toString()
	 * when the default value is not used. This is useful in cases when the 
	 * computation of the default is expensive (or has side effects) and 
	 * therefore should be avoided.
	 */
	public String get2(String key, Object defValue) {
		String s = Utils.getMyMagicString() + System.currentTimeMillis();
		String ans = get(key, s);
		return (ans != s) ? ans : (defValue == null ? null : defValue.toString());
	}

	//-------------------------------------------------------------------------
	public synchronized String set(String key, String value) {
		Object ans;
		if (value == null) {
			ans = data.get(key);
			if (ans != null) {
				boolean change = (saved == null);
				if (change)
					saved = (java.util.Properties)data.clone();
				data.remove(key);
				if (change)
					dirty.fireLater();
			}
		} else {
			ans = data.setProperty(key, value);
			if (saved == null && (ans == null || !ans.equals(value))) {
				saved = (java.util.Properties)data.clone();
				if (ans == null)
					saved.remove(key);
				else
					saved.setProperty(key, ans.toString());
				dirty.fireLater();
			}
		}
		return (ans == null) ? null : ans.toString();
	}

	//-------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public static void test(String[] args) {
		String cmdlinespec[] = {
				"", "Arguments: -readreg >text_output (reads the registry)",
				"", "       or: -toreg   <text_input  (updates the registry)",
				"", "--testhelp",
				"positional", "0 0",		// min max
				"-readreg", null,
				"-toreg", null
		};
		java.util.Map<String, Object> switches = Utils.cmdLineParse(args, cmdlinespec);
		Utils.cmdLineArgsExit(switches);

		boolean readReg  = (switches.get("-readreg") == Boolean.TRUE);
		boolean writeReg = (switches.get("-toreg") == Boolean.TRUE);

		if (!readReg && !writeReg)
			Utils.cmdLineUsageExit(switches);

		try {
			if (readReg) {
				byte[] data = MEMEApp.userPrefs.getByteArray(UserPrefs.SESSION, Utils.emptyGzip());
				java.io.InputStream is = new GZIPInputStream(new ByteArrayInputStream(data));
				Utils.copyStream(is, System.out);
				return;
			}
			if (writeReg) {
				Session s = new Session();
				s.data.load(System.in);
				s.save();
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
