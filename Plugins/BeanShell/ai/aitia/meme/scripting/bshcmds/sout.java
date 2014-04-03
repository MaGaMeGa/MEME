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
package ai.aitia.meme.scripting.bshcmds;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.prefs.Preferences;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.UserPrefs;
import bsh.CallStack;
import bsh.Interpreter;

public class sout {
	
	//====================================================================================================
	// members
	
	private static final File logFile = new File(Preferences.userNodeForPackage(MEMEApp.class).get(UserPrefs.LOGFILELOC,"MEME.log"));

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public static void invoke(Interpreter interp, CallStack callstack, char c) throws IOException { _invoke(interp,callstack,String.valueOf(c)); }
	public static void invoke(Interpreter interp, CallStack callstack, boolean b) throws IOException { _invoke(interp,callstack,String.valueOf(b)); }
	public static void invoke(Interpreter interp, CallStack callstack, byte b) throws IOException { _invoke(interp,callstack,String.valueOf(b)); }
	public static void invoke(Interpreter interp, CallStack callstack, short s) throws IOException { _invoke(interp,callstack,String.valueOf(s)); }
	public static void invoke(Interpreter interp, CallStack callstack, int i) throws IOException { _invoke(interp,callstack,String.valueOf(i)); }
	public static void invoke(Interpreter interp, CallStack callstack, long l) throws IOException { _invoke(interp,callstack,String.valueOf(l)); }
	public static void invoke(Interpreter interp, CallStack callstack, float f) throws IOException { _invoke(interp,callstack,String.valueOf(f)); }
	public static void invoke(Interpreter interp, CallStack callstack, double d) throws IOException { _invoke(interp,callstack,String.valueOf(d)); }
	
	//----------------------------------------------------------------------------------------------------
	public static void invoke(Interpreter interp, CallStack callstack, char[] c) throws IOException { _invoke(interp,callstack,Arrays.toString(c)); }
	public static void invoke(Interpreter interp, CallStack callstack, boolean[] b) throws IOException { _invoke(interp,callstack,Arrays.toString(b)); }
	public static void invoke(Interpreter interp, CallStack callstack, byte[] b) throws IOException { _invoke(interp,callstack,Arrays.toString(b)); }
	public static void invoke(Interpreter interp, CallStack callstack, short[] s) throws IOException { _invoke(interp,callstack,Arrays.toString(s)); }
	public static void invoke(Interpreter interp, CallStack callstack, int[] i) throws IOException { _invoke(interp,callstack,Arrays.toString(i)); }
	public static void invoke(Interpreter interp, CallStack callstack, long[] l) throws IOException { _invoke(interp,callstack,Arrays.toString(l)); }
	public static void invoke(Interpreter interp, CallStack callstack, float[] f) throws IOException { _invoke(interp,callstack,Arrays.toString(f)); }
	public static void invoke(Interpreter interp, CallStack callstack, double[] d) throws IOException { _invoke(interp,callstack,Arrays.toString(d)); }
	
	//----------------------------------------------------------------------------------------------------
	public static void invoke(Interpreter interp, CallStack callstack, Object o) throws IOException {
		if (o.getClass().isArray())
			_invoke(interp,callstack,Arrays.toString((Object[])o));
		else
			_invoke(interp,callstack,"" + o);
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private static void _invoke(Interpreter interp, CallStack callstack, String msg) throws IOException {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(logFile,true));
			writer.println(msg);
			writer.flush();
		} finally {
			if (writer != null)
				writer.close();
		}
	}
}
