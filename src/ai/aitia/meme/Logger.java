/**
 * 
 */
package ai.aitia.meme;

import java.io.File;
import java.io.PrintStream;
import java.sql.SQLException;

import ai.aitia.meme.utils.Utils;

/**
 * This class handles logging in MEME.
 * 
 * @author Tamás Máhr
 *
 */
public class Logger {

	/** The log file. */
	static java.io.File					g_logFileLoc	= null;
	/** The log stream. */
	static java.io.PrintStream			g_logStream		= null;

	//-------------------------------------------------------------------------
	/** Writes the error specified by <code>format</code> and <code>args</code>
	 *  to the log file.
	 * @param format the format string of the error message
	 * @param args the parameters of the format string
	 */  
	public static void logError(String format, Object ... args) {
		String pfx = String.format("%1$tT.%1$tL\t", System.currentTimeMillis()); 
		if (g_logStream == null) {
			g_logStream = System.err;
			try {
	    		if (g_logFileLoc != null) {
	    			g_logStream = new java.io.PrintStream(g_logFileLoc);
	    			System.setErr(g_logStream);
	    		}
	    		g_logStream.println(String.format(
	    				"%tF %sLog file created", System.currentTimeMillis(), pfx
	    		));
			} catch (Exception e) {
				logError("Cannot create log file " + e.getLocalizedMessage());
			}
		}
		if (!"".equals(format.trim()))
			g_logStream.println(pfx + Utils.formatv(format, args));
	}

	//-------------------------------------------------------------------------
	/** Writes the localized message of the exception <code>t</code> to the log file.
	 * @param t the exception
	 */
	public static void logException(Throwable t)							{ Logger.logException(null, t, false); }

	/** Writes the place and the localized message of the exception <code>t</code> to the log file.
	 * @param where the place where the exception is occured
	 * @param t the exception
	 */
	public static void logException(String where, Throwable t)			{ Logger.logException(where,t, false); }

	/** Writes the localized message and the stack trace of the exception <code>t</code> to the log file.
	 * @param t the exception
	 */
	public static void logExceptionCallStack(Throwable t)					{ Logger.logException(null, t, true); }

	/** Writes the place, the localized message and the stack trace of the exception <code>t</code> to the log file.
	 * @param where the place where the exception is occured
	 * @param t the exception
	 */
	public static void logExceptionCallStack(String where, Throwable t) 	{ Logger.logException(where,t, true); }

	//-------------------------------------------------------------------------
	/** Writes the place and the localized message of the exception <code>t</code> to the log file.
	 *  If <code>callStack</code> is true, the stack trace of the exception is also written. 
	 * @param where the place where the exception is occured
	 * @param t the exception
	 * @param callStack does it write the stack trace too or not?
	 */
	public static void logException(String where, Throwable t, boolean callStack) {
		if (t == null) return;
		String type = t.getClass().getName();
		type = type.substring(type.lastIndexOf('.') + 1);
		if (where == null)
			logError("*** %s: %s", type, t.getLocalizedMessage());
		else if (t instanceof java.sql.SQLException) {
			java.sql.SQLException e = (java.sql.SQLException)t;
			logError("*** %s caught at %s: (state %s, code %d) %s",
					type, where, e.getSQLState(), e.getErrorCode(), t.getLocalizedMessage());
		}
		else
			logError("*** %s caught at %s: %s", type, where, t.getLocalizedMessage());
		if (callStack)
			t.printStackTrace(g_logStream == null ? System.err : g_logStream);
	}

}
