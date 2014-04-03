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

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.aitia.meme.MEMEApp;

/** Collection of utility functions. */
public class Utils
{
	//-------------------------------------------------------------------------
	/** General equals function. */
	public static boolean equals(Object a, Object b) {
		if (a == b) return true;
		if (a != null) return a.equals(b);
		return b.equals(a);
	}

	//=========================================================================
	//	String utilities

	//-------------------------------------------------------------------------
	/**
	 * Usage examples (suppose that s.length == n):<pre>
	 *  sub(s, 5, 2)     // 2 chars starting at 5: [5..6]
	 *  sub(s, 5, -2)    // all but the last 2 chars starting at 5: [5..n-3]
	 *  sub(s, -7, 4)    // 4 chars starting at n-7: [n-7..n-4]
	 *  sub(s, -7, -2)   // all but the last 2 chars starting at n-7: [n-7..n-3]
	 * </pre>
	 */
	public static String sub(String s, int begin, int len) {
		int n = (s == null) ? 0 : s.length();
		if (n == 0) return "";
		if (begin < 0)
			begin = Math.max(n + begin, 0);
		else if (begin >= n)
			return "";
		n -= begin;
		if (len < 0)
			len = Math.max(n - begin + len, 0);
		else if (len > n - begin)
			len = n - begin;
		return s.substring(begin, begin + len);
	}

	//-------------------------------------------------------------------------
	/** Returns the s.substring(first, last+1). */
	public static String mid(String s, int first, int last) {
		if (first < 0) first = 0;
		if (s.length() <= last) last = s.length() - 1;
		return s.substring(first, last+1);
	}

	//-------------------------------------------------------------------------
	/** Returns the s.substring(first). */
	public static String mid(String s, int first) {
		return s.substring(first < 0 ? 0 : first);
	}

	//-------------------------------------------------------------------------
	/** Returns the s.substring(0,len). */
	public static String left(String s, int len) {
		return (len <= 0 || s == null) ? new String() : 
			    ( s.length() < len ? s : s.substring(0, len) ); 
	}

	//-------------------------------------------------------------------------
	/** Returns the s.substring(s.length()-len). */
	public static String right(String s, int len) {
		return (len <= 0 || s == null) ? new String() : 
		    ( s.length() < len ? s : s.substring(s.length()-len) ); 
	}

	//-------------------------------------------------------------------------
	/** Returns s.substring(s.lastIndexOf(ch) + 1). */
	public static String tail(String s, char ch) {
		return s.substring(s.lastIndexOf(ch) + 1); 
	}

	//---------------------------------------------------------------------
	public static boolean isEmpty(String s) {
		return (s == null) || (s.length() == 0); 
	}

	//---------------------------------------------------------------------
	/** Removes all content from 'sb'. */ 
	public static void clear(StringBuilder sb) {
		if (sb != null && sb.length() > 0)
			sb.delete(0, sb.length());
	}
	
	//---------------------------------------------------------------------
	/** Appends 'args' to 'sb'. 
	 * @return sb
	 */
	public static StringBuilder append(StringBuilder sb, Object...args) {
		if (sb == null)
			sb = new StringBuilder();
		for (int i = 0; i < args.length; ++i)
			sb.append(args[i]);
		return sb;
	}

	//---------------------------------------------------------------------
	/** Returns a string representation of 'strings' which contains all elements
	 *  of 'strings' separated by 'delimiter'.
	 */
	public static String join(java.util.Collection<?> strings, String delimiter) {
		StringBuilder ans = new StringBuilder();
		boolean first = true;
		for (Object s : strings) {
			if (first) first = false; else ans.append(delimiter);			
			ans.append(s);
		}
		return ans.toString();
	}

	//---------------------------------------------------------------------
	/** Returns a string representation of 'args' which contains all elements
	 *  of 'args' separated by 'delimiter'.
	 */
	public static String join(String delimiter, Object ... args) {
		if (args.length == 1) {
			if (args[0] instanceof Object[])
				args = (Object[])args[0];
			else if (args[0] instanceof java.util.Collection)
				return join((java.util.Collection<?>)args[0], delimiter);
		}
		return join(java.util.Arrays.asList(args), delimiter);
	}
	
	//---------------------------------------------------------------------
	/** Creates a string that contains 's' 'count' times separated by 'delimiter'. */
	public static String repeat(String s, int count, String delimiter) {
		int len = s.length();
		if (len == 0 || count == 1) return s;
		if (count == 0) return new String();
		if (delimiter == null || delimiter.length() == 0) {
			StringBuilder ans = new StringBuilder(len * count);
			while (--count >= 0)
				ans.append(s);
			return ans.toString();
		}
		StringBuilder ans = new StringBuilder(len * count + delimiter.length() * (count - 1)); 
		ans.append(s);
		s = delimiter + s;
		while (--count > 0)
			ans.append(s);
		return ans.toString();
	}
	
	//---------------------------------------------------------------------
	/** Replaces the first occurance of 'pattern' with the given replacement in 'source'. It
	 *  returns a new string. */
	public static String replaceFirst(String source, String pattern, String replacement) {
		int i = source.indexOf(pattern);
		if (i < 0) return source;
		if (i > 0)
			replacement = source.substring(0, i) + replacement;
		return replacement + source.substring(i + pattern.length());
	}

	//---------------------------------------------------------------------
	/** Returns the first matching region of 's' or null if there's no match */
	public static String findFirst(String regexp, String s) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile(regexp).matcher(s);
		return m.find() ? m.group() : null;
	}

	//---------------------------------------------------------------------
	public static String stringMap(String str, String[] map) {							// ???
		int len = str.length(), shortest = 0, m = (map == null) ? 0 : map.length - 1;	// ???
		for (int i = 0; i < m; i += 2) {
			if (map[i].length() > shortest)
				shortest = map[i].length();
		}
		if (shortest == 0)
			return str;
		StringBuilder b = new StringBuilder(len);
		A:
		for (int pos = 0; pos <= len - shortest; ) {
			for (int i = 0; i < m; i += 2) {
				int k = map[i].length();
				if (str.substring(pos, pos+k).equals(map[i])) {
					b.append(map[i + 1]);
					pos += k;
					continue A;
				}
			}
			b.append(str.charAt(pos));
			pos += 1;
		}
		return b.toString();
	}

	//-------------------------------------------------------------------------
	public static String charMap(String str, java.util.Map<Integer, String> map) {
		StringBuilder b = null;
		int len = str.length(), i = 0;
		while (i < len) {
			int ch = str.codePointAt(i);
			String repl = map.get(ch);
			if (repl != null) {
				if (b == null)
					b = new StringBuilder(str.substring(0, i));
				b.append(repl);
			}
			else if (b != null) {
				b.append(str.charAt(i++));
				if (ch > Character.MIN_SUPPLEMENTARY_CODE_POINT)
					b.append(str.charAt(i++));
				continue;
			}
			i += 1;
			if (ch > Character.MIN_SUPPLEMENTARY_CODE_POINT)
				i += 1;
		}
		return (b == null) ? str : b.toString();
	}

	//-------------------------------------------------------------------------
	public static java.util.Map<Integer, String> makeCharMap(String[] map) {		// ???
		java.util.HashMap<Integer, String> ans = new java.util.HashMap<Integer, String>(map.length * 2/3 + 2); 
		for (int i = 1; i < map.length; i += 2) {
			String ch = map[i-1];
			if (ch == null || ch.length() == 0) continue;
			ans.put(ch.codePointAt(0), map[i]);
		}
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Returns the localized message of exception 't'. In special case (e.g.
	 *  OutOfMemeryError it returns the more useful message than the localized
	 *  message. 
	 */
	public static String getLocalizedMessage(Throwable t) {
		if (t == null) return "";
		String s = null;
		if (t instanceof OutOfMemoryError) {
			// The localized message of this exception is "Java heap space", which is too bad.
			// We have to provide something more useful:
			s = "Out of memory.\nPlease exit from the program.";
		} else if (t instanceof ArrayIndexOutOfBoundsException) {
			// The localized message of this exception is the bad index
			s = "Wrong index: " + t.getLocalizedMessage();
		} else if (t instanceof NullPointerException) {
			// The localized message of this exception is 'null'
			s = "Null pointer.";
		} else {
			s = t.getLocalizedMessage();
		}
		if (s == null || s.length() == 0)
			s = String.format("exception %s", t.getClass().getName());
		return s;
	}
	
	//-------------------------------------------------------------------------
	/** Returns the lines of the stack trace belongs to exception 't' in a string. */
	public static String getStackTraceLines(Throwable t) {
		java.io.StringWriter buff = new java.io.StringWriter();
		java.io.PrintWriter w = new java.io.PrintWriter(buff);
		if (t instanceof java.lang.reflect.InvocationTargetException)
			t = t.getCause();
		t.printStackTrace(w);
		w.close();
		return buff.toString();
	}

	//-------------------------------------------------------------------------
	/** Returns the lines of the stack trace belongs to exception 't' in a string array. */
	public static String[] getStackTraceLines(Throwable t, String before, String after) {
		if (before == null) before = ""; else before = before + '\n';
		if (after == null) after = "";  else after = '\n' + after;
		return (before + getStackTraceLines(t) + after).split("\n");
	}
	
	//-------------------------------------------------------------------------
	/** Rethrows exception 't'. */
	public static void rethrow(Throwable t) throws Exception {
		if (t instanceof RuntimeException)	throw (RuntimeException)t;
		if (t instanceof Error)			throw (Error)t;
		throw (Exception)t;
	}

	//-------------------------------------------------------------------------
	/** Replaces the characters (e.g. &lt;, &gt;, etc.) in a string that have
	 *  special meaning in HTML.
	 */
    public static String htmlQuote(String text) {
    	if (g_Str2HTMLMap == null) {
    		java.util.ResourceBundle b = 
    			java.util.PropertyResourceBundle.getBundle("com.sun.org.apache.xml.internal.serializer.HTMLEntities");
    		if (b != null) {
    			g_Str2HTMLMap = new java.util.HashMap<Integer, String>();
        		for (String name : iterate(b.getKeys())) {
        			g_Str2HTMLMap.put(Integer.parseInt(b.getString(name)), '&' + name + ';');
        		}
        		g_Str2HTMLMap.put(10, "<br>");	// 10 = '\n'
    		} else {
    			// A last resort - not as complete but more than nothing
    			g_Str2HTMLMap = makeCharMap(new String[] { "&", "&amp;",		"<", "&lt;",	">", "&gt;",	
    													 "\"", "&quot;",	"\n", "<br>" });
    		}
    	}
    	return charMap(text, g_Str2HTMLMap);
    }
    public static java.util.Map<Integer, String> g_Str2HTMLMap = null;

	//-------------------------------------------------------------------------
    /** Tests if 'text' is a HTML-text. */
    public static boolean isHTML(String text) {
    	return (0 < text.length() && 0 <= left(text, 200).toLowerCase().indexOf("<html>"));
    }

	//-------------------------------------------------------------------------
    /** Returns the text between &lt;body&gt; &lt;/body&gt; tags of a HTML source. */ 
    public static String extractBody(String text) {
    	text = replaceFirst(text, "</html>", "");
    	text = replaceFirst(text, "</body>", "");
    	int i = text.indexOf("<body");
    	String ans = text;
    	if (i >= 0) {									// strip all <head>
    		if (i + 5 >= text.length())
    			ans = "";
    		else if (text.charAt(i+5) == '>')
    			ans = text.substring(i + 6);
    		else
    			ans = text.replaceFirst("^.*<body[^>]+>", "");
    	}
    	if (ans.length() == text.length())
    		ans = replaceFirst(ans, "<html>", "");
    	return ans;
    }

	//-------------------------------------------------------------------------
    /** Returns the starter body-tag. */ 
    public static String htmlBody() {
    	java.awt.Color c = GUIUtils.getLabelFg();
    	return "<body style=\"font-family:sans-serif;font-size:100%;color:" + GUIUtils.color2html(c) +"\">";
    }

	//-------------------------------------------------------------------------
    /** Returns 'htmlText' as a HTML-source (with &lt;html&gt;,&lt;body&gt;,&lt;/body&gt;,&lt;/html&gt; tags. */
    public static String htmlPage(String htmltext) {
    	return "<html>" + htmlBody() + ' ' + htmltext + " </body></html>";
    }

	//---------------------------------------------------------------------
    /** Concatenates two strings, each can be plain or HTML-text. If one of them
     *  is a HTML-text, then the result will be that, too. Otherwise the result 
     *  will be plain text. 'insertNewLine' indicates whether be a new line between
     *  the two strings or not.
     */
    public static String htmlOrPlainJoin(String s1, String s2, boolean insertNewline) {
		boolean ishtml1 = isHTML(s1); 
		boolean ishtml2 = isHTML(s2);
		if (ishtml1 || ishtml2) {
			s1 = ishtml1 ? extractBody(s1) : Utils.htmlQuote(s1);
			s2 = ishtml2 ? extractBody(s2) : Utils.htmlQuote(s2);
			if (insertNewline && (s1.length() == 0 || s2.length() == 0))
				insertNewline = false;
			return insertNewline ? htmlPage(s1 + "<br>" + s2) : htmlPage(s1 + s2);
		}
		if (insertNewline && (s1.length() == 0 || s2.length() == 0))
			insertNewline = false;
		return insertNewline ? s1 + "\\n" + s2 : s1 + s2;   
    }

	//---------------------------------------------------------------------
    /** Returns a string from a format string and its argument. */
	public static String formatv(String format, Object[] args) {
		if (args == null || args.length == 0)
			return format;
		synchronized (g_formatter) {
			g_formatter.format(format, args);
			g_formatter.flush();
			StringBuilder s = (StringBuilder)(g_formatter.out());
			String ans = s.toString();
			s.delete(0, s.length());			
			return ans; 
		}
	}
	private static final java.util.Formatter g_formatter = new java.util.Formatter();

	//-------------------------------------------------------------------------
	/** Parses a date from a string. */
	public static long parseDateTime(String s) {
		DateFormat df = null;
		for (int i = 0; true; ++i) {
			switch (i) {
				case 0 :
					df = new java.text.SimpleDateFormat("MMM d, yyyy K:m:s a", java.util.Locale.ENGLISH);
					break;
				case 1 : 
					df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, java.util.Locale.ENGLISH);
					break;
				case 2 : 
					df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
					break;
				case 3 : 
					df = DateFormat.getDateTimeInstance();
					break;
				case 4 :
					df = new java.text.SimpleDateFormat("MM/dd/yyyy kk:mm:ss",java.util.Locale.ENGLISH);
					break;
				default : 
					return new java.util.Date().getTime();
			}
			try {
				java.util.Date date = df.parse(s);
				return date.getTime();
			} catch (java.text.ParseException e) {}			
		}
	}
	
	//-------------------------------------------------------------------------
//	/** Like <code>String.format("%tF", time)</code>, but prints UTC time */
//	public static String logDate(long time) {
//		if (g_logDateFmt == null) {
//			g_logDateFmt = new java.text.SimpleDateFormat("yyyy-MM-dd");
//			g_logDateFmt.setTimeZone(new java.util.SimpleTimeZone(0, "noDST"));
//		}
//		return g_logDateFmt.format(new java.util.Date(time));
//	}
//	/** Like <code>String.format("%1$tT.%1$tL", time)</code>, but faster and UTC time instead of local */
//	public static String logTime(long time) {
//		if (g_logTimeFmt == null) {
//			g_logTimeFmt = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
//			g_logTimeFmt.setTimeZone(new java.util.SimpleTimeZone(0, "noDST"));
//		}
//		return g_logTimeFmt.format(new java.util.Date(time));
//	}

//	public static String nowStr() {
//		return logTime(System.currentTimeMillis());
//	}

//	private static DateFormat g_logDateFmt = null; 
//	private static DateFormat g_logTimeFmt = null; 


	//-------------------------------------------------------------------------
	/** Returns the boolean meanings of an object 'o'. Never been null. */
	public static boolean getBooleanValue(Object o) {
		return getBooleanValue(o, false);
	}
	/** Returns the boolean meanings of an object 'o'.
	 *  If 'o' is a number, then zero value means false, other values mean true.
	 *  If 'o' is a not a number, then the methods uses its string representation.
	 *  If its value is "false", "f", "0", "no" or "off", the result is false.
	 *  If ist value is "true", "t", "yes", "on" or a non-zero number string, the result is true.
	 *  The comparsion above is not case sensitive.
	 *  If null result is not enabled, the method returns false instead null.
	 */
	public static Boolean getBooleanValue(Object o, boolean mayBeNull) { 
		if (o instanceof Boolean)
			return (Boolean)o;
		if (o instanceof Number)
			return ((Number)o).intValue() != 0;

		Boolean ans = null;
		String s = (o == null) ? null : o.toString();
		if (s != null && s.length() > 0) {
			s = s.trim().toLowerCase();
			if (s.equals("false") || s.equals("f") || s.equals("0") || s.equals("no") || s.equals("off"))
				ans = false;
			else if (s.equals("true") || s.equals("t") || s.equals("yes") || s.equals("on"))
				ans = true;
			else try {								// ha szam es <>0, akkor is true
				ans = (Double.valueOf(s) != 0);		// ha szam es 0, akkor false
			}
			catch (NumberFormatException e) {}
		}
		return (ans == null && !mayBeNull) ? false : ans; 
	}


	//-------------------------------------------------------------------------
	/** Simplifies the generation of auto-numbered names */
	public static class NameGenerator
	{
		/** Interface for representing finders. */
		public interface IFinder {
			/** Returns true if 'name' is found */
			boolean		findName(String name, Object userData);
		}

		protected String tryThis, defName="", delimiter="", numFmt = "%d";
		protected int startIdx = 1, detectNumbersAtEndMaxWidth = 2, maxlen = 0;
		protected IFinder finder;

		public String generate(String suggested) { return generate(suggested, null); }
		public String generate(String suggested, Object userData) {
			String ans = tryThis = suggested;
			// Detect if the suggested name ends with numbers
			boolean detectNumbersAtEnd = true;
			if (tryThis == null || tryThis.length() == 0) {
				tryThis = defName;
				detectNumbersAtEnd = false;
			}
			int i = startIdx - 1;
			for (boolean found = true; found; ) { 
				if (++i > startIdx || ans == null || ans.length() == 0) {
					// Szamozzuk az eredetileg megadott nevet. Ha mar most is szamozva van 
					// (pl. tryThis=="Column2"), akkor nem irunk hozza ujabb szamjegyet 
					// ("Column22") hanem felismerjuk a szamot es noveljuk.
					if (detectNumbersAtEnd) {
						i = detectNumbersAtEnd(i);
						detectNumbersAtEnd = false;
					}
					String s = delimiter + String.format(numFmt, i);
					if (maxlen > 0 && tryThis.length() + s.length() > maxlen) {
						tryThis = tryThis.substring(0, Math.max(0, maxlen - s.length()));
					}
					ans = tryThis + s; 
				}
				found = find(ans, userData);
			}
			return ans;
		}
		protected int detectNumbersAtEnd(int i) {
			String num = findFirst("[0-9]+$", tryThis);
			if (num != null && num.length() <= detectNumbersAtEndMaxWidth) {
				i = Integer.valueOf(num) + 1;
				tryThis = tryThis.substring(0, tryThis.length() - num.length());
				int d = delimiter.length(), j = tryThis.length() - d;
				if (d > 0 && j >= 0 && tryThis.substring(j).equals(delimiter))
					tryThis = tryThis.substring(0, j);
			}
			return i;
		}
		protected boolean find(String name, Object userData) {
			return (finder != null && finder.findName(name, userData));
		}; 

		public NameGenerator defName(String name)	{ defName	= name;	return this; }
		public NameGenerator delimiter(String d)	{ delimiter	= d;	return this; }
		public NameGenerator NumFmt(String fmt)		{ numFmt	= fmt;	return this; }
		public NameGenerator finder(IFinder f)		{ finder	= f;	return this; }
		public NameGenerator startIdx(int idx)		{ startIdx	= idx;	return this; }
		public NameGenerator maxlen(int len)		{ maxlen	= len;	return this; }
		public NameGenerator dnaemw(int width)		{ detectNumbersAtEndMaxWidth = width; return this; }
	}

	/** 
	 * Specifies that double quotation marks should be treated specially
	 * except when non-delimiter text follows. E.g. <tt>a:"b":"c:"x:d</tt>
	 * should parse to <tt>a, b, "c:"x, d</tt> instead of the default 
	 * <tt>a, b, c:, d</tt> (ignoring x).
	 */
	public static final int KEEP_AFTERQ	= 1;

	/** Treat double quotation mark (") as ordinary character (involves KEEP_AFTERQ) */ 
	public static final int NO_QUOTE		= 3; 

	/** 
	 * MS.Excel-style quoting (quotes are doubled instead of using backslash)
	 * This flag cannot be used together with KEEP_AFTERQ and NO_QUOTE. 
	 */
	public static final int EXC_QUOTE		= 4; 
	public static final int FORCE_EMPTY		= 8;


	//-------------------------------------------------------------------------
	/** Parses a CSV (Comma separated value) line. 
	 * @param flags Combination of the following bits: NO_QUOTE, KEEP_AFTERQ, EXC_QUOTE.
	 */
	public static String[] parseCSVLine(String line, String delimiter,char quote, int flags) {
		if (delimiter == null || line == null)
			throw new NullPointerException();

		boolean keepAftQ = (flags & KEEP_AFTERQ) == KEEP_AFTERQ;
		boolean useQuote = (flags & NO_QUOTE) != NO_QUOTE;
		boolean excel    = (flags & EXC_QUOTE) == EXC_QUOTE;
		boolean forceEmpty = (flags & FORCE_EMPTY) == FORCE_EMPTY;

		if (delimiter.length() == 0 || (excel && (flags & NO_QUOTE) != 0)
				|| (excel && delimiter.indexOf(quote) == 0))
			throw new IllegalArgumentException();

		ArrayList<String> ans = new ArrayList<String>();
		int dl = delimiter.length(), state = 0;		// state: 0 = before string
		boolean addIfEmpty = false;	// indicates that the last word may be an empty string and isn't added yet
		StringBuilder word = new StringBuilder();
		for (int i = 0; i < line.length(); ++i) {
			addIfEmpty = false;
			char ch = line.charAt(i);
			switch (state) {
				case 0 :		// 0: before quoted string or before/within/after unquoted string
					if (line.startsWith(delimiter, i)) {
						ans.add(word.toString());
						word.delete(0, word.length());
						i += dl - 1;
					}
					else if (useQuote && ch == quote && word.length() == 0) {     
						state = 1;
					}
					else
						word.append(ch);
					break;
				case 1 :								// 1: within quoted string, no backslash
					if (!excel && ch == '\\') {
						state = 2;
					} else if (ch == quote) {
						state = 3; addIfEmpty = true;
					} else
						word.append(ch);						
					break;
				case 2 :								// 2: within quoted string, just after backslash
					word.append(ch);
					state = 1;
					break;
				case 3 :								// 3: after quoted string
					if (excel && ch == quote) {
						word.append(ch);
						state = 1;
						break;
					}
					if (line.startsWith(delimiter, i)) {
						ans.add(word.toString());
						word.delete(0, word.length());
						i += dl - 1;
					} else if (keepAftQ) {
						word.insert(0, quote);
						word.append(quote);
						word.append(ch);
					} else {
						i = line.indexOf(delimiter, i+1);
						if (i >= 0) {
							ans.add(word.toString());
							word.delete(0, word.length());
							i += dl - 1;
						} else {
							addIfEmpty = true; break;
						}
					}
					state = 0;
					break;
				default :
					assert false;
			}
		}
		if (word.length() > 0 || addIfEmpty || forceEmpty)
			ans.add(word.toString());
		return ans.toArray(new String[ans.size()]);
	}

	//-------------------------------------------------------------------------
	/** Creates a CSV line from the 'args'. 
	 * @param useQuote if true, produces a MS.Excel-like quoted string
	 *                   (quotes are doubled instead of using backslash).
	 * @see http://en.wikipedia.org/wiki/Comma-separated_values
	 */
	public static String composeCSVLine(String delim, boolean useQuote, Object ... args) {
		if (args.length == 1 && args[0] instanceof Object[])
			args = (Object[])args[0];

		if (delim == null)
			delim = ",";

		if (!useQuote)
			return join(delim, args);

		StringBuilder ans = new StringBuilder();
		for (int i = 0; i < args.length; ++i) {
			if (i > 0) ans.append(delim);
			String s = (args[i] == null) ? null : args[i].toString();
			if (s == null) continue;
			if (s.length() == 0) {
				ans.append("\"\"");
				continue;
			}
			int a = s.indexOf('"'), b = s.indexOf(delim);
			if ((a & b) >= 0) {		// == if at least one is non-negative
				for (b = 0; a >= 0; a = s.indexOf('"', a)) {
					ans.append('"');
					ans.append(s.substring(b, ++a));
					b = a;
				}
				ans.append('"');
				ans.append(s.substring(b));
				ans.append('"');
			} else {
				ans.append(s);
			}
		}
		return ans.toString();
	}


	//-------------------------------------------------------------------------
	/** 
	 * Returns a string whose contents is unique: we can be quite sure that the program
	 * will never generate the same by itself. 
	 */
	public static String getMyMagicString() {
		return "6ccb9af0-782d-43cd-85be-ccf4fdb986c7";	// generated by guidgen, 2006.11.25
	}


	//=========================================================================
	//	Command line arguments

	
	//-------------------------------------------------------------------------
	/** 
	 * Usage example:
	 * <pre>
	 *		String cmdlinespec[] = {
	 *				"", "Usage: MEMEApp [options] [session.mes]",
	 *				"", "Available options:",
	 *				"", "  -boolOpt             ...",
	 *				"", "  -intOpt n            default is 3",
	 *				"", "  --help -h -? /? /h   These options show this help.",
	 *				"", "--help -h -? /? /h",
	 *				"positional", "0 1"    // if we accept 0<=nargs<=1 positional args
	 *				"-opt", "3"            // an option requiring argument ("3" if omitted)
	 *				"-boolOpt", null,      // no argument->boolean: will be TRUE if used
	 *		}; 
	 *		java.util.Map&lt;String,Object&gt; switches = Utils.cmdLineParse(args, cmdlinespec);
	 *		Utils.cmdLineArgsExit(switches);       // prints error/help and calls System.exit()
	 *		String[] positional = (String[])switches.get("positional");
	 *		boolean  boolOpt    = (switches.get("-boolOpt") == Boolean.TRUE);
	 *		int      opt        = Integer.parseInt(switches.get("-opt"));
	 *		// ...
	 * </pre>
	 * Note: unknown options are considered as positional arguments. 
	 */
	@SuppressWarnings("unchecked")
	public static java.util.LinkedHashMap<String, Object> cmdLineParse(String[] args, String[] switchSpecs) {
		java.util.LinkedHashMap<String, Object> spec = new java.util.LinkedHashMap<String, Object>(), ans;

		java.util.HashSet<String> usageOpts = new java.util.HashSet<String>();
		ArrayList<String> usageLines = new ArrayList<String>();
		for (int i = switchSpecs.length - 2; i >= 0; i -= 2) {
			if (switchSpecs[i].toString().length() == 0) {
				if (usageOpts.isEmpty()) {
					usageOpts.addAll(java.util.Arrays.asList(switchSpecs[i+1].toString().split("\\s")));
				} else {
					usageLines.add(0, switchSpecs[i+1].toString());
				}
			} else {
				spec.put(switchSpecs[i].toString(), switchSpecs[i+1]);
			}
		}
		String tmp = spec.get("positional").toString(), error = null;
		String[] posrange = (tmp == null) ? null : tmp.split("[ \t,]");

		ans = (java.util.LinkedHashMap<String, Object>)spec.clone();
		ArrayList<String> positional = new ArrayList<String>();
		for (int i = 0; i < args.length && error == null; ++i) {
			String a = args[i].toString();
			if ("--".equals(a)) {
				if (i+1 < args.length)
					positional.addAll(java.util.Arrays.asList(args).subList(i+1, args.length-1));
				break;
			}
			if (usageOpts.contains(a)) {
				ans.put("usage", join(usageLines, "\n"));
				break;
			}
			if (spec.containsKey(a)) {
				Object defVal = spec.get(a);
				if (defVal == null)
					ans.put(a, Boolean.TRUE);
				else if (i+1 < args.length)
					ans.put(a, args[++i]);
				else
					error = String.format("missing argument for option %s", a);
			} else {
				positional.add(args[i]);
			}
		}
		if (error == null) {
			if (posrange != null && posrange.length > 0) {
				if (posrange.length > 0 && positional.size() < Integer.parseInt(posrange[0]))
					error = "not enough arguments";
				if (posrange.length > 1 && positional.size() > Integer.parseInt(posrange[1]))
					error = "too many arguments";
				if (!usageOpts.isEmpty() && error != null)
					error = String.format("%s. For usage information, use %s.", error, join(usageOpts, " or "));
			}
		}
		if (error != null)
			ans.put("error", error);

		ans.put("positional", positional.toArray(new String[positional.size()]));
		ans.put("usageLines", usageLines);
		return ans;
	}
	
	//-------------------------------------------------------------------------
	public static void cmdLineArgsExit(java.util.Map<String, Object> switches) {
		Object msg = switches.get("error");
		if (msg == null) msg = switches.get("usage");
		if (msg != null) {
			System.err.println(msg);
			System.exit(CMDLINEARGS_EXIT_CODE);
		}
	}
	public static int CMDLINEARGS_EXIT_CODE = 1;

	//-------------------------------------------------------------------------
	public static void cmdLineUsageExit(java.util.Map<String, Object> switches) {
		switches.put("usage", join("\n", switches.get("usageLines")));
		cmdLineArgsExit(switches);
	}

	
	//=========================================================================
	//	File and stream utilities

	
	//---------------------------------------------------------------------
	/**
	 * Returns the extension of f excluding the dot (e.g. "ext")
	 * Note: the dot is excluded because "foo" and "foo." should produce the same result.
	 */
	public static String getExt(java.io.File f) {
		String name = f.getName();
		int i = name.lastIndexOf('.');
		return (0 <= i) ? name.substring(i+1) : "";  
	}

	//---------------------------------------------------------------------
	/**
	 * Returns the extension of the file specifid by 'pathname' excluding the dot (e.g. "ext")
	 * Note: the dot is excluded because "foo" and "foo." should produce the same result.
	 */
	public static String getExt(String pathname) {
		return getExt(new java.io.File(pathname));
	}
	
	//-------------------------------------------------------------------------
	/** Returns the whole 'pathname' without the extension ('.' is also removed) */
	public static String getRootName(String pathname) {
		String name = new java.io.File(pathname).getName();
		int i = name.lastIndexOf('.');
		return (i < 0) ? pathname : pathname.substring(0, pathname.length() + i - name.length()); 
	}
	
	//-------------------------------------------------------------------------
	/** Replaces the extension in 'pathname'. */
	public static String withExt(String pathname, String newext) {
		pathname = getRootName(pathname);
		if (newext == null || newext.length() == 0) return pathname;
		return (newext.charAt(0) != '.') ? pathname + '.' + newext : pathname.concat(newext);
	}

	//-------------------------------------------------------------------------
	/** Copies all data from 'in' stream to 'out' stream.
	 * @return out
	 */
	public static java.io.OutputStream copyStream(java.io.InputStream in, java.io.OutputStream out) throws java.io.IOException {
		// Nem kell Buffered...Stream mert itt van ez a buffer:
		byte buffer[] = new byte[8192];
		int length;
		try {
			while ((length = in.read(buffer)) != -1)
				out.write(buffer, 0, length);
		} finally {
			in.close();
		}
		return out;
	}

	//-------------------------------------------------------------------------
	/** Copies all data from 'in' reader to 'out' writer.
	 * @return out
	 */
	public static java.io.Writer copyRW(java.io.Reader in, java.io.Writer out) throws java.io.IOException {
		// Nem kell Buffered...Reader mert itt van ez a buffer:
		char buffer[] = new char[8192];  
		int length;
		try {
			while ((length = in.read(buffer)) != -1)
				out.write(buffer, 0, length);
		} finally {
			in.close();
		}
		return out;
	}

	//-------------------------------------------------------------------------
	/** Loads a binary file 'f' and returns its content as a byte array. */
	public static byte[] loadBinFile(java.io.File f) {
		byte[] ans = null;
		try {
			java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
			copyStream(new java.io.FileInputStream(f), bo).close();
			ans = bo.toByteArray();
		} 
		catch (java.io.IOException e) {}
		return ans;
	}
	
	//-------------------------------------------------------------------------
	/** Loads a text file 'f' and returns its content as a String. */
	public static String loadText(java.io.File f) {
		java.io.StringWriter ans = new java.io.StringWriter();
		try {
			copyRW(new java.io.FileReader(f), ans).close();
		} 
		catch (java.io.IOException e) {}
		return ans.toString();
	}
	public static String loadText(String fname) {
		return loadText(new java.io.File(fname));
	}

	//-------------------------------------------------------------------------
	/** Saves 'text' to file 'f'. */
	public static void saveText(java.io.File f, String text) {
		java.io.StringReader in = new java.io.StringReader(text);
		try {
			copyRW(in, new java.io.FileWriter(f)).close();
		} 
		catch (java.io.IOException e) {}
	}
	/** Saves 'text' to file named 'fname'. */
	public static void saveText(String fname, String text) {
		saveText(new java.io.File(fname), text);
	}

	//-------------------------------------------------------------------------
	/** Creates an empty gzip stream and returns its content as a byte array. */
	public static byte[] emptyGzip() { 
		java.io.ByteArrayOutputStream ans = new java.io.ByteArrayOutputStream();
		try { new java.util.zip.GZIPOutputStream(ans).close(); } catch (java.io.IOException e) {}; 
		return ans.toByteArray();
	}

	
	
	//=========================================================================
	//	Array utilities

	public static final Object[]	EMPTY_ARRAY = {};

	//-------------------------------------------------------------------------
	/** Returns 'c' as an int array. */
	public static int[] asIntArray(java.util.Collection<? extends Number> c) {
		int i = 0, n = (c == null) ? 0 : c.size();
		int[] ans = new int[n];
		if (n > 0) for (Number nr : c) ans[i++] = nr.intValue();
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Returns 'c' as a long array. */
	public static long[] asLongArray(java.util.Collection<? extends Number> c) {
		int i = 0, n = (c == null) ? 0 : c.size();
		long[] ans = new long[n];
		if (n > 0) for (Number nr : c) ans[i++] = nr.longValue();
		return ans;
	}
	
	//-------------------------------------------------------------------------
	/** Returns the hash code of 'args'. */
	public static int hashCode(Object ... args) {
		return java.util.Arrays.hashCode(args);
	}

	//-------------------------------------------------------------------------
	/** Inserts elements at the specifed position to an array. */
	@SuppressWarnings("unchecked")
	public static <T> T[] insert(T[] array, int pos, T... args) {
		if (array == null)
			return args;

		T[] ans = (T[])java.lang.reflect.Array.newInstance(
						array.getClass().getComponentType(), array.length + args.length);
		
		if (pos > array.length)	pos = array.length; 
		else if (pos < 0)		pos = 0;

		System.arraycopy(array, 0, ans, 0, pos);
		System.arraycopy(args, 0, ans, pos, args.length);
		System.arraycopy(array, pos, ans, pos + args.length, array.length - pos);
		return ans;
	}

	//-------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	/** Removes elements at a specifed range from an array. */
	public static <T> T[] remove(T[] array, int first, int last) {
		if (array == null || last < 0 || array.length <= first || last < first)
			return array;
		if (first < 0)				first = 0;
		if (array.length <= last)	last = array.length-1;

		T[] ans = (T[])java.lang.reflect.Array.newInstance(
				array.getClass().getComponentType(), array.length - (last-first+1));

		if (first > 0)			System.arraycopy(array, 0, ans, 0, first-1);
		if (first < ans.length)	System.arraycopy(array, last+1, ans, first, array.length - last);
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Adds null elements to the end of the collection if necessary */
	public static <T, C extends java.util.Collection<T>> C ensureSize(C coll, int newsize) {
		int n = coll.size();
		if (newsize > n) {
			coll.addAll(java.util.Collections.<T>nCopies(newsize - n, null));
		}
		return coll;
	}

	//-------------------------------------------------------------------------
	/** Truncates 'list' or adds null elements to its end */
	public static <T, L extends java.util.List<T>> L setSize(L list, int newsize) {
		int n = list.size();
		if (newsize > n) {
			list.addAll(java.util.Collections.<T>nCopies(newsize - n, null));
		} else if (newsize < n) {
			list.subList(newsize, n).clear();
		}
		return list;
	}

	//-------------------------------------------------------------------------
	/**
	 * Alternative implementation for <code>array.indexOf(obj)</code>: uses the
	 * <code>equals()</code> method of the array elements instead of <code>obj.equals()</code>.
	 */
	public static int indexOf(ArrayList<?> array, Object obj) {
		int n = (array == null) ? 0 : array.size();
		for (int i = 0; i < n; ++i) {
			Object elem = array.get(i);
			if (elem == null) { if (obj == elem) return i; }
			else if (elem.equals(obj)) return i;
		}
		return -1;
	}


	//=========================================================================
	//	Invoke method utilities
	
	//-------------------------------------------------------------------------
	/**
	 * Example: <pre> invokeLater(obj, "foo") </pre>
	 * is roughly equivalent to <pre>
	 *   SwingUtilities.invokeLater(new Runnable() {
	 *      public void run() { obj.foo(); }
	 *   });
	 * </pre>
	 * with the following differences:
	 * <ul>
	 * <li> An argument list may also be specified, and the arguments need not be final. 
	 *      Note that the arguments are not cloned, thus modifications after returning  
	 *      from this function but before the method invocation will affect the method.
	 * <li> The named method must be public, and the actual argument list must match the method's  
	 *      formal arguments, otherwise IllegalArgumentException is thrown. 
	 *      Arguments types are checked upon calling only.
	 * <li> Any exception thrown by the method is caught and logged using MEMEApp.logExceptionCallStack()
	 * <li> Static methods can be specified by passing the Class object in 'obj'. 
	 * </ul>
	 */
	public static void invokeLater(Object o, String methodName, Object ... args) {
		javax.swing.SwingUtilities.invokeLater(new Callback(o, methodName, args));
	}

	//-------------------------------------------------------------------------
	/** Same as the invokeLater(), but the method named 'methodName' can be nonpublic too. */
	public static void invokeLaterNonPublic(Object o, String methodName, Object ... args) {
		javax.swing.SwingUtilities.invokeLater(new Callback(o, methodName, args, false));
	}

	//-------------------------------------------------------------------------
	/** Same as the other invokeLater(), but the method must be specified its object rather than its name. */
	public static void invokeLater(Object o, java.lang.reflect.Method m, Object ... args) {
		javax.swing.SwingUtilities.invokeLater(new Callback(o, m, args));
	}

	//-------------------------------------------------------------------------
	// TODO: probald ki hogy a publikus metodusokkal foglalkozo reszt lehetne-e
	// implementalni java.beans.Statement-el inkabb? Annyibol jobb lenne, hogy az
	// talan a nyelv specifikaciojahoz hubben kezelne az muvelet-tulterhelesbol 
	// fakado helyzeteket. Ki kell probalni hogy varargs-hoz mit szol.
	/** This class represents a task (a Runnable instance). It encapsulates a 
	 *  method and its target and its arguments. In the run() method it calls 
	 *  method.invoke(target, args). 
	 */
	public static class Callback implements Runnable {
		/** The target of the method. */
		public final Object					target;
		/** The method object. */
		public final java.lang.reflect.Method	method;
		/** The arguments of the method. */
		public final Object[]					args;

		// See description above at Utils.invokeLater() 
		public Callback(Object o, String methodName, Object[] args) {
			this(o, methodName, args, true);
		}

		public Callback(Object o, String methodName, Object[] args, boolean publicOnly) {
			this(o, findMethodV(o, methodName, args, publicOnly), args);
		}

		public Callback(Object o, java.lang.reflect.Method	m, Object[] args) {
			target = o;
			method = m;
			if (args == null) {
				args = EMPTY_ARRAY;
			}
			Class<?>[] formal = m.getParameterTypes();
			int n = formal.length;
			int actual = args.length;
			// Check for varargs
			if (n > 0 && formal[n-1].isArray() && 
					(actual != n || args[actual-1] == null || args[actual-1].getClass() != formal[n-1]))
			{
				assert (n - 1 <= actual) : makePair(this, args);	// errol findMethod() mar gondoskodott elvileg 

				// Atmasoljuk a varargs elotti parametereket 
				this.args = new Object[n];
				System.arraycopy(args, 0, this.args, 0, n - 1);

				// tmp[] lesz a varargs tomb 
				Object tmp = java.lang.reflect.Array.newInstance(formal[n-1].getComponentType(), actual - n + 1);
				this.args[n - 1] = tmp;
				// tmp[]-be beramoljuk a maradek parametereket. Tipushiba eseten itt fog exception-t dobni.
				for (int i = actual - n + 1 - 1; i >= 0; --i)
					java.lang.reflect.Array.set(tmp, i, args[n - 1 + i]);
			} else {
				this.args = args;
			}
		}

		public void run() {
			try {
				method.invoke(target, args);
			} catch (Throwable t) {
				log("Utils.Callback", target, method.getName(), t);
			}
		}
		
		/** Writes a log entry from the parameters to the log file. */ 
		public static void log(String where, Object target, String methodName, Throwable t) {
			String where2;
			if (target == null)
				where2 = "<null>";
			else if (target instanceof Class)
				where2 = ((Class)target).getCanonicalName();
			else
				where2 = target.getClass().getCanonicalName();
			where = where + ": " + where2 + "." + methodName + "()";
			t = (t instanceof java.lang.reflect.InvocationTargetException) ? t.getCause() : t;
			if (MEMEApp.isDebugVersion())
				MEMEApp.uncaughtException(where, Thread.currentThread(), t);
			else
				MEMEApp.logExceptionCallStack(where, t);
		}
	}

	//-------------------------------------------------------------------------
	/** Searches a method object specified by the parameters.
	 * @param o Class object or instance that has the method
	 * @param methodName name of the method
	 * @param args  arguments of the method
	 * @param publicOnly if true it searches amongst public methods only
	 */
	public static java.lang.reflect.Method findMethodV(Object		o,
														String		methodName, 
														Object[]	args,
														boolean		publicOnly)
	{
		return findMethod(o, methodName, (args == null) ? 0 : args.length, publicOnly);
	}

	//-------------------------------------------------------------------------
	/** Searches a method object specified by the parameters.
	 * @param o Class object or instance that has the method
	 * @param methodName name of the method
	 * @param reqnargs  the number of the required parameters
	 * @param publicOnly if true it searches amongst public methods only
	 */
	public static java.lang.reflect.Method findMethod(	Object		o,
														String		methodName, 
														int			reqnargs, 
														boolean		publicOnly)
	{
		// TODO: a keresett �s megtal�lt m�veleteket �rdemes lenne cache-elni.
		// ennek megvalositasara jo pelda van java.beans.ReflectionUtils.getMethod()-ban
		// (sajnos nem publikus).
		//
		Class c = (o instanceof Class) ? (Class)o : o.getClass();
		Class origc = c;
		do {
			java.lang.reflect.Method[] methods = publicOnly ? c.getMethods() : c.getDeclaredMethods();
			for (java.lang.reflect.Method m : methods) {
				if (m.getName().equals(methodName)) {
					Class<?>[] formal = m.getParameterTypes();
					int n = formal.length;
					if (n == reqnargs || 
						// Check for varargs:
						(0 < n && n - 1 <= reqnargs && formal[n - 1].isArray())
					) {
						if (!publicOnly) m.setAccessible(true);
						return m;
					}
				}
			}
			if (publicOnly)
				c = c.getSuperclass();
		} while (publicOnly && c != null);

		throw new IllegalArgumentException(String.format(
				"Class %s does not contain a method %s() with %d arguments" +
				(publicOnly ? " or it is not public" : ""), 
				origc.getName(), methodName, reqnargs)); 
	}

//	//-------------------------------------------------------------------------
//	/**
//	 * Arranges for executing o.methodName(args) in the GUI thread
//	 * exclusively against the Model thread (e.g.\ after all currently
//	 * running long operations).
//	 * 
//	 * @b Note: 
//	 * - 'methodName' must be a public method.
//	 * - Avoid using this function, because if the specified method lasts long, it will block
//	 *   the GUI thread, worsening the responsivity of the GUI. Only use this function 
//	 *   if you're sure that the called method will never lasts long. 
//	 */
//	public static void invokeModelExclusive(String taskName, Object o, String methodName, Object ... args) {
//		invokeModelExclusive(taskName, new Utils.Callback(o, methodName, args));
//	}
//
//	//-------------------------------------------------------------------------
//	/**
//	 * Arranges for executing task.run() in the GUI thread exclusively against the Model thread 
//	 * (e.g.\ after all currently running long operations).
//	 * @note Avoid using this function, because if the specified method lasts long, it will block
//	 *       the GUI thread, worsening the responsivity of the GUI. Only use this function 
//	 *       if you're sure that the called method will never lasts long. 
//	 */
//	public static void invokeModelExclusive(final String taskName, final Runnable task) {
//		synchronized (MEMEApp.MODEL_THREAD) {
//			if (!MEMEApp.MODEL_THREAD.isWorking(false)) {
//				task.run();
//				return;
//			}
//		}
//		MEMEApp.LONG_OPERATION.begin(taskName, new LongRunnable() {
//			public void finished() throws Exception {
//				getReq().setErrorDisplay(false);
//				synchronized (MEMEApp.MODEL_THREAD) {
//					if (MEMEApp.MODEL_THREAD.isWorking(false))
//						MEMEApp.LONG_OPERATION.begin(taskName, this);
//					else
//						task.run();
//				}
//			}	
//		});
//	}
	
	//-------------------------------------------------------------------------
	/** Returns a Swing timer which is already started, and by default will run the method only once */
	public static TimerHandle invokeAfter(int msec, Object o, String methodName, Object ... args) {
		return invokeAfter(msec, new Utils.Callback(o, methodName, args));
	}

	//-------------------------------------------------------------------------
	/** Returns a Swing timer which is already started, and by default will run the task only once */
	public static TimerHandle invokeAfter(int msec, Runnable task) {
		// The standard Swing 'Timer' cannot be used with Runnable objects,
		// but with ActionListeners only. The 'TimerHanlde' class solves this.
		//
		return new TimerHandle(msec, task);
	}

	//-------------------------------------------------------------------------
	/**
	 * It is an extended Swing timer which can execute a Runnable, besides 
	 * the ActionListeners that are handled by the superclass.
	 * The timer is automatically started at the end of the constructor 
	 * and by default will fire only once (not repeat).<br>
	 * Note: the start() method is provided by the superclass. The JDK doc
	 * does not mention about it that it does nothing when issued before the 
	 * specified delay. After that start() restarts the timer. Therefore, 
	 * it's recommended to use stop() and then start() to restart the timer.
	 */
	public static class TimerHandle extends javax.swing.Timer implements java.awt.event.ActionListener, Runnable {
		private static final long serialVersionUID = -4558451624547667643L;
		protected Runnable task;
		public TimerHandle(int delay, Runnable task) {
			super(Math.max(delay, 0), null);
			this.task = task;
			if (task != null) {
				addActionListener(this);
				setRepeats(false);
				start();
			}
		}
		public TimerHandle(int delay, java.awt.event.ActionListener task)	{
			super(Math.max(delay, 0), task);
			this.task = null;	// causes this.actionPerformed() do nothing.
								// 'task' will be executed by the superclass' code
			if (task != null) {
				setRepeats(false);
				start();
			}
		}
		/**
		 * This method is called from the timer event. Executes at most that task 
		 * which was specified to the constructor (if it was a Runnable). Remaining
		 * listeners - that may have been added by TimerHandle.addActionListener() - 
		 * will be notified by base class's code.
		 */
		// EDT only
		public void actionPerformed(java.awt.event.ActionEvent e) 	{
			if (task != null) task.run();
		}
		/** 
		 * Calls this.actionPerformed() to notify all listeners immediately
		 * (generates an immediate timer event). This method is not used by default.
		 * It allows the caller to use this TimerHandle as a Runnable object.  
		 */
		// EDT only
		public void run()	{
            fireActionPerformed(new java.awt.event.ActionEvent(this, 0, null, System.currentTimeMillis(), 0));
		}
	}

	
	//-------------------------------------------------------------------------
	/** Generic interface that defines a run() method with one parameter. */
	public interface IUnary<T>		{ public void run(T arg); 	 }
	/** Generic interface that defines a run() method with one parameter. run()
	 *  can throw exceptions. */
	public interface IEUnary<T>	{ public void run(T arg) throws Exception;}
	/** Generic interface that defines a call() method with one parameter. */
	public interface IUnaryCallable<R,T> { public R call(T arg); }

	//-------------------------------------------------------------------------
	/** 
	 * Returns a proxy of 'target' which refers to 'target' weakly. This proxy
	 * redirects all calls to 'target' as long as 'target' exists. When the proxy
	 * finds that 'target' is finalized, calls <code>remover.run(target)</code> once.   
	 */
	@SuppressWarnings("unchecked")
	public static <T> T weakProxy(T target, IEUnary<T> remover) {
		return (T)java.lang.reflect.Proxy.newProxyInstance(target.getClass().getClassLoader(),
				target.getClass().getInterfaces(), new WeakInvocationHandler(target, remover)  
		);
	}

	//-------------------------------------------------------------------------
	public static class WeakInvocationHandler<T> implements java.lang.reflect.InvocationHandler {
		java.lang.ref.WeakReference<T> t;
		IEUnary<T> remover;
		public WeakInvocationHandler(T target, IEUnary<T> remover) {
			t = new java.lang.ref.WeakReference<T>(target);
			this.remover = remover;
		}
		public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
			T target = null;
			if (t != null) {
				target = t.get();
				if (target == null) {
					t = null;
					remover.run(target);
					remover = null;
				}
			}
			return (target != null) ? method.invoke(target, args) : null;
		}
	}


	//=========================================================================
	//	Iterators


	//-------------------------------------------------------------------------
	/** Iterator for iterating Enumeration-s. 
	 *  Note: this class supports at most one iteration at a time! */
	public static class Enumeration2Iterator<T> implements java.util.Iterator<T>, Iterable<T> {
		private final java.util.Enumeration<T> e;

		public 				Enumeration2Iterator(java.util.Enumeration<T> e)	{ this.e = e; }
		public boolean		hasNext()											{ return e.hasMoreElements(); }
		public T			next()												{ return e.nextElement(); }
		public void		remove() { throw new UnsupportedOperationException(); }

		public java.util.Iterator<T>	iterator() { return this; } 
	}

	//-------------------------------------------------------------------------
	/** Returns an iterator of 'e'. */
	public static <T> Iterable<T> iterate(final java.util.Enumeration<T> e) {
		return new Enumeration2Iterator<T>(e); 
	}

	//-------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	/** Returns a raw iterator of 'e'. */
	public static Iterable<Object> iterateRaw(final java.util.Enumeration e) {
		return new Enumeration2Iterator<Object>(e); 
	}

	//-------------------------------------------------------------------------
	public static class Concat<T> implements java.util.Iterator<T>, Iterable<T> {
		Iterable<?> 			collections;
		java.util.Iterator<?>	it0;
		java.util.Iterator<T>	it1;

		/** This works for Iterable<T[]>, too. Furthermore, it can be mixed. */
		public Concat(Iterable<?> args)				{ collections = args; }
		public java.util.Iterator<T>	iterator()	{ it0 = collections.iterator(); it1 = null; return this; }
		public T						next()		{ return it1.next(); }
		public void					remove()	{ it1.remove(); }
		@SuppressWarnings("unchecked")
		public boolean hasNext() {
			while (it0 != null) {
				if (it1 != null) {
					if (it1.hasNext()) return true;
					it1 = null;
				}
				if (!it0.hasNext()) {
					it0 = null;
					break;
				}
				Object o = it0.next();
				if (o instanceof Object[])
					it1 = java.util.Arrays.asList((T[])o).iterator();
				else
					it1 = ((Iterable<T>)o).iterator();
			}
			return false;
		}
	}

	//-------------------------------------------------------------------------
	// Az elso argumentum azert van kulon, hogy a hivas helyen se warningot ne adjon,
	// se a tipust ne kelljen explicite odairni.
	// Tip: amikor bajos az elso argumentumot kulonvenni, probald ezt: 'new Concat<T>(c)'
	@SuppressWarnings({ "unchecked", "cast" })
	public static <T> Iterable<T> forAll(Iterable<T> arg, Object...args) {
		return new Concat<T>((Iterable<?>) java.util.Arrays.asList( insert(args, 0, arg) ));
	}
	@SuppressWarnings("cast")
	public static <T> Iterable<T> forAll(T[] arg, Object...args) {
		return new Concat<T>((Iterable<?>) java.util.Arrays.asList( insert(args, 0, arg) ));
	}
	
//	//-------------------------------------------------------------------------
//	public static <T> ArrayList<T> cleanWeakList(java.util.List<java.lang.ref.WeakReference<T>> list) {
//		ArrayList<T> ans = new ArrayList<T>(list == null ? 0 : list.size());
//		if (list != null) {
//			for (java.util.ListIterator<java.lang.ref.WeakReference<T>> it = list.listIterator(); it.hasNext(); ) {
//				java.lang.ref.WeakReference<T> w = it.next();
//				if (w.get() == null)
//					it.remove();
//				else
//					ans.add(w.get());
//			}
//		}
//		return ans;
//	}
	
	//-------------------------------------------------------------------------
	/**
	 * Automatically removes null weak references during iteration.
	 * Does not support remove().
	 * Note: this class supports at most one iteration at a time!
	 */ 
	public static class WeakIter<T> implements Iterable<T>, java.util.Iterator<T> {
		protected Iterable<java.lang.ref.WeakReference<T>>				iterable;
		protected java.util.Iterator<java.lang.ref.WeakReference<T>>	it = null;
		protected T next = null;

						WeakIter(Iterable<java.lang.ref.WeakReference<T>> i)	{ iterable = i; } 
		public boolean	hasNext()												{ return (next != null); }
		public void	remove()												{ throw new UnsupportedOperationException(); }

		public java.util.Iterator<T> iterator() {
			it = iterable.iterator();
			next = null;
			next();
			return this;
		}
		public T next() {
			T ans = next;
			while (it.hasNext()) {
				next = it.next().get();
				if (next != null) return ans;
				it.remove();
			}
			next = null; it = null; 
			return ans;
		}
	}
	/**
	 * Automatically removes null weak references during iteration.
	 */ 
	public static <T> WeakIter<T> iterateAutoRemove(Iterable<java.lang.ref.WeakReference<T>> it) {
		return new WeakIter<T>(it);
	}

	//-------------------------------------------------------------------------
	/**
	 * Looks for the first element in 'collection' that equals to 'obj', using linear search.  
	 * @param flags Combination of the following bit values:<br>
	 *                1: call Iterator.remove() when null weak references are found<br>
	 *                2: call Iterator.remove() when the first 'obj' is found<br>
	 *                4: return a strong reference instead of the Integer index when 'obj' is found<br>
	 *                8: use element.equals(obj) instead of obj.equals(element) during the search.  
	 * @return null if 'obj' is not found, otherwise the index of the equal element (flags & 4 == 0)
	 *               or a strong reference to the equal element (flags & 4 == 4).
	 */
	public static Object indexOf(Iterable<java.lang.ref.WeakReference<?>> collection, Object obj, int flags) {
		if (obj != null) {
			java.util.Iterator<java.lang.ref.WeakReference<?>> it = collection.iterator();
			if (it != null) {
				if ((flags & 8) == 8) {
					for (int i = 0; it.hasNext(); ++i) {
						Object tmp = it.next().get();
						if (tmp == null) {
							if ((flags & 1) == 1) {
								it.remove(); --i;
							}
						} else if (tmp.equals(obj)) {
							if ((flags & 2) == 2) {
								it.remove();
							}
							return ((flags & 4) == 4) ? tmp : i;
						}
					}
				} else { 
					for (int i = 0; it.hasNext(); ++i) {
						Object tmp = it.next().get();
						if (tmp == null) {
							if ((flags & 1) == 1) {
								it.remove(); --i;
							}
						} else if (obj.equals(tmp)) {
							if ((flags & 2) == 2) {
								it.remove();
							}
							return ((flags & 4) == 4) ? tmp : i;
						}
					}
				}
			}
		}
		return null;
	}


	//=========================================================================
	//	Tiny structure classes


	//-------------------------------------------------------------------------
	/** Generic representation of a pair. */
	public static class Pair<K, V> implements java.util.Map.Entry<K, V> {
		protected K	first;
		protected V	second;

		public Pair()						{ first = null; second = null; }
		public Pair(K first, V second)		{ this.first = first; this.second = second; }

		public K getKey()					{ return first; } 
		public K getFirst()					{ return first; }
		
		public V getValue()					{ return second; }
		public V getSecond()				{ return second; }
		
		public V setValue(V value)			{ V ans = second; second = value; return ans; }
		public V setSecond(V value)			{ V ans = second; second = value; return ans; }
		public void set(K key, V value)	{ first = key; second = value; }
		@Override public int hashCode()	{ return Utils.hashCode(getKey(), getValue()); }
		@Override
		public boolean equals(Object o) {
			if (o == this) return true;
			if (o instanceof java.util.Map.Entry) {
				java.util.Map.Entry other = (java.util.Map.Entry)o;
				return (getKey() == null ? other.getKey() == null : getKey().equals(other.getKey()))
						&& (getValue() == null ? other.getValue() == null : getValue().equals(other.getValue()));
			}
			return false;
		}
		@Override
		public String toString() {
			Object tmp[] = { first, second };
			return java.util.Arrays.toString(tmp);
		}
	}
	/** Creates a pair from the parameters. */
	public static <K, V> Pair<K, V> makePair(K first, V second) { return new Pair<K, V>(first, second); }
	
	//-------------------------------------------------------------------------
	/** Generic representation of a pair. */
	public static class TPair<K extends Serializable,V extends Serializable> implements java.util.Map.Entry<K,V>, Serializable {
		private static final long serialVersionUID = 638059430689029740L;
		protected K	first;
		protected V	second;

		public TPair()						{ first = null; second = null; }
		public TPair(K first, V second)		{ this.first = first; this.second = second; }

		public K getKey()					{ return first; } 
		public K getFirst()					{ return first; }
		
		public V getValue()					{ return second; }
		public V getSecond()				{ return second; }
		
		public V setValue(V value)			{ V ans = second; second = value; return ans; }
		public V setSecond(V value)			{ V ans = second; second = value; return ans; }
		public void set(K key, V value)	{ first = key; second = value; }
		@Override public int hashCode()	{ return Utils.hashCode(getKey(), getValue()); }
		@Override
		public boolean equals(Object o) {
			if (o == this) return true;
			if (o instanceof java.util.Map.Entry) {
				java.util.Map.Entry other = (java.util.Map.Entry)o;
				return (getKey() == null ? other.getKey() == null : getKey().equals(other.getKey()))
						&& (getValue() == null ? other.getValue() == null : getValue().equals(other.getValue()));
			}
			return false;
		}
		@Override
		public String toString() {
			Object tmp[] = { first, second };
			return java.util.Arrays.toString(tmp);
		}
	}
	
	//-------------------------------------------------------------------------
	/** This class represents flags. */
	public static class Flag {
		protected boolean					value;
		
		public Flag()						{ this(false); } 
		public Flag(boolean b)				{ value = b; } 
		public void		set(boolean b)	{ value = b; } 
		public boolean		get()			{ return value; }
		@Override public String toString()	{ return Boolean.valueOf(value).toString(); }
		@Override public boolean equals(Object obj) {
			if (obj == this)				return true;
			if (obj instanceof Flag)		return ((Flag)obj).value == value;
			if (obj instanceof Boolean)	return ((Boolean)obj).booleanValue() == value;
			return false;
		}
	}

	//-------------------------------------------------------------------------
	/** This class represents counters. */
	public static class Counter {
		protected int						value;
		
		public Counter()					{ this(0); } 
		public Counter(int i)				{ value = i; } 
		public void		set(int i)		{ value = i; } 
		public int			get()			{ return value; }
		public void		incr()			{ ++value; }
		public void		decr()			{ --value; }
		public void		add(int i)		{ value += i; }
		
		@Override public String toString()	{ return Integer.valueOf(value).toString(); }
		@SuppressWarnings("cast")
		@Override
		public boolean equals(Object obj) {
			if (obj == this)				return true;
			if (obj instanceof Counter)	return ((Counter)obj).value == value;
			if (obj instanceof Integer)	return ((Integer)obj).intValue() == value;
			if (obj instanceof Number)		return ((Number)obj).doubleValue() == (double)value;
			return false;
		}
	}
	
	//-------------------------------------------------------------------------
	/** This class represents a reference. */
	public static class Ref<T> {
		protected T						value;
		public Ref()						{ value = null; }
		public Ref(T x)						{ value = x; }
		public T get()						{ return value; }
		public T set(T x)					{ T ans = value; value = x; return ans; }
		
		@Override public String toString() { return (value == null) ? "null" : value.toString(); }
		@Override public boolean equals(Object obj) {
			if ((obj == this) || (obj == value)) return true;
			if (obj instanceof Ref) {
				Ref ref = (Ref)obj;
				return (value == null) ? ref.value == null : value.equals(ref.value);   
			}
			return (value != null) && value.equals(obj); 
		}
		@Override public int hashCode()	{ return (value == null) ? 0 : value.hashCode(); }
	}

	
	//=========================================================================
	//	Sorting


	//-------------------------------------------------------------------------
	/** Returns -2 if o1 is prefix of o2; +2 if o2 is prefix of o1. */ 
	public static <K extends Comparable<K>> int lex(K[] o1, K[] o2, boolean nullsAtFirst) {
		final int len = Math.min(o1.length, o2.length);
		for (int i = 0; i < len; ++i) {
			K a = o1[i], b = o2[i];
			if (a == null) {
				if (b == null) continue;
				return nullsAtFirst ? -1 : 1;	// null is smaller: null < b
			} else if (b == null) {
				return nullsAtFirst ? 1 : -1;	// null is smaller: a > null
			}
			int ans = a.compareTo(b);
			if (ans != 0)
				return ans < 0 ? -1 : 1;
		}
		if (o1.length < o2.length)
			return -2;
		else if (o1.length == o2.length)
			return 0;
		else
			return 2;
	}

	//-------------------------------------------------------------------------
	/** A comparator inducing lexicographical ordering */
	public static class LexComp<T extends Comparable<T>> implements java.util.Comparator<T[]> {
		public final boolean nullsAtFirst;
		public LexComp()						{ this(false); }
		public LexComp(boolean nullsAtFirst)	{ this.nullsAtFirst = nullsAtFirst; }
		public 	int compare(T[] o1, T[] o2)		{ return lex(o1, o2, nullsAtFirst); }
	}
	
	//-------------------------------------------------------------------------
	/** 
	 * Compares two version values. E.g. "1.0.3" < "1.0.12". Numeric objects are allowed (e.g. 2.1 > 1).
	 * @throws NumberFormatException if any of the strings contain letters. 
	 */
	public static int versionCmp(Object v1, Object v2) {
		java.util.StringTokenizer st = new java.util.StringTokenizer(v1 == null ? "" : v1.toString(), ".");
		int n = st.countTokens();
		Long l1[] = new Long[n];
		while (--n >= 0) l1[l1.length - n - 1] = new Long(st.nextToken());

		st = new java.util.StringTokenizer(v2 == null ? "" : v2.toString(), ".");
		n = st.countTokens();
		Long l2[] = new Long[n];
		while (--n >= 0) l2[l2.length - n - 1] = new Long(st.nextToken());

		return lex(l1, l2, true);
	}
	
	/**
	 * Returns the catalog name part from a connection URL. For instance: 
	 * jdbc:mysql//localhost/catalogX -> catalogX
	 * jdbc:mysql//localhost/ -> ""
	 * jdbc:mysql//localhost -> ""
	 * @param connStr empty string if no catalog found
	 * @return catalog name
	 */
	public static String getCatalogName(String connStr) {
		Pattern p = Pattern.compile("//.+/(.*)$");
		Matcher m = p.matcher(connStr);
		String g = "";
		if ( m.find() ) {
			g = m.group(1);
		}
		return g;
	}
	
	public static boolean doubleEqualsWithTolerance(double a, double b, long maxFloatsBetween){
		if (a == b) return true;
		if (Math.abs(Double.doubleToLongBits(a)-Double.doubleToLongBits(b)) <= maxFloatsBetween) return true;
		if (Double.doubleToLongBits(Math.abs(a-b)) <= maxFloatsBetween) return true;
		return false;
	}
	
	
}
