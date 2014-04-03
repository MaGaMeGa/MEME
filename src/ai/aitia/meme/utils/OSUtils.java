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
package ai.aitia.meme.utils;

import java.awt.EventQueue;
import java.util.LinkedList;

/**
 * Central place for operation system specific operations that are used 
 * by the program. All such operations should be accessed through this 
 * interface, and all exceptions should be documented here:<br>
 * - The ai.aitia.Activation module uses platform-specific operations 
 *   through the JUG package (see org.safehaus.uuid, com.ccg.net.ethernet).<br>
 * - In debug mode (during development, when not running from a .jar file), 
 *   ai.aitia.meme.database.DatabaseConnection.launchInternal() uses
 *   exec() with "cmd /c start" to launch the built-in database server 
 *   in a separate process. 
 */
public abstract class OSUtils
{
	//=========================================================================
	//	Static members
	
	/** Registered implementation of this abstract class. */
	protected static final LinkedList<OSUtils> implementations = new LinkedList<OSUtils>();
	/** The most relevant implementation. */
	protected static volatile OSUtils actual = null;

	//-------------------------------------------------------------------------
	// [Model thread]
	/** Registers 'impl' to the implementation storage. */
	public static void registerImplementation(OSUtils impl) {
		synchronized (implementations) {
			if (!implementations.contains(impl)) implementations.add(impl);
		}
	}

	//-------------------------------------------------------------------------
	static {
		// Register the "factory built-in" implementations
		registerImplementation(new WinUtils());
		registerImplementation(new UnixUtils());
		registerImplementation(new MacUtils());

		// Additional implementations may come from plugins.
	}

	//-------------------------------------------------------------------------
	/**
	 * Selects the most relevant implementation and disposes the others.
	 * Must be called from the Model thread. 
	 */
	// [Model thread]
	public static void init() {
		synchronized (implementations) {
			int max = Integer.MIN_VALUE;
			for (OSUtils s : implementations) {
				int compatibility = s.getRelevance();
				if (compatibility > max) {
					actual = s;
					max = compatibility;
				}
			}
			// Dispose the irrelevant implementations
			implementations.clear();
			implementations.add(actual);
		}
	}

	//-------------------------------------------------------------------------
	/** 
	 * Returns the implementation that was selected by init().
	 * <br>Precondition: init() was called. 
	 */
	// [EDT or Model thread]
	public static OSUtils getActual() {
		if (actual == null) {
			if (EventQueue.isDispatchThread())
				throw new IllegalStateException("OSUtils.init() was not called");
			init();
		}
		return actual;
	}


	//=========================================================================
	//	Nonstatic members

	//-------------------------------------------------------------------------
	/**
	 * Returns an integer indicating how much is this implementation compatible with 
	 * the current operating system. The implementation that returns the highest value 
	 * will be selected. Recommended values: 0 = no relevance, 1 = OSType-level compatibility,
	 * 10 or more: os.name is recognized. 
	 */
	// [Model thread]
	abstract protected int getRelevance();


	//-------------------------------------------------------------------------
	/**
	 * Opens the document located by 'uri' asynchronously in a separate process 
	 * by launching the system-specific associated application. If there's no   
	 * such application, an exception may be thrown (depending on the implementation).
	 * The implementation is expected to support at least the <code>file</code>
	 * and <code>http</code> schemes.   
	 * @param dir If not null, specifies starting directory for the application.  
	 */
	// [Model thread]
	abstract public void	openDocument(java.net.URI uri, java.io.File dir) throws java.io.IOException;


	//-------------------------------------------------------------------------
	/**
	 *	Returns the type of the current operating system, or null if it is other 
	 *  than those listed in OSType.
	 *  The detection may be incorrect, depending on the underlying JVM. This is why
	 *  this method can be reimplemented in derived classes.  
	 */
	// [EDT or Model thread]
	public OSType	getOSType() {
		String name = System.getProperty("os.name").toLowerCase();
		if (name.indexOf("windows") >= 0)	// Windows 95,Windows 98,Windows NT,Windows 2000,Windows XP
			return OSType.WINDOWS;
		if (name.indexOf("mac") >= 0)		// Mac OS
			return OSType.MAC;
		if (name.indexOf("os/2") >= 0)		// OS/2
			return OSType.OS2;
		if (name.indexOf('x') >= 0 || name.indexOf("solaris") >= 0 || name.indexOf("bsd") >= 0)
			return OSType.UNIX;				// AIX,Digital Unix,HP UX,Irix,Linux,MPE/iX,Solaris,FreeBSD

		return null;
	}
	
	/** Enum type for representing supported OSs. */
	public static enum OSType { WINDOWS, UNIX, MAC, OS2 };
}
