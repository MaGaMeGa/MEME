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

public class Test {

	public static String proba(Double d) {
		String s = String.format("%.3e", d);
//		int dot = s.indexOf('.');
//		int e = s.indexOf('e');
//		int exp = Integer.valueOf(s.substring(e+1));
//		exp += 
//		
//		if (dot >= 0) {
//			int i = dot;
//			while (++i < s.length() && s.charAt(i)=='0') {};
//			if (i >= s.length())
//				s = s.substring(0, dot);
//			else if (!Character.isDigit(s.charAt(i)))
//				s = s.substring(0, dot) + s.substring(i);
//		}
		return s;
	}

	//-------------------------------------------------------------------------
	public static void main(String[] args) {

		try {

//			java.io.File fn = new java.io.File("C:/lala bela/x.txt"); 
////			java.net.URL url = fn.toURL();
////			java.net.URL url = new java.net.URL("http://foo.com/hello world");
////
////			System.out.println(url);
////			System.out.println(url.toURI().toURL());
//			
//			//java.net.URI uri = new java.net.URI(fn.toString());
//			java.net.URI uri = fn.toURI();
//			System.out.println(uri);
//			System.out.println(uri.toURL());
			
//			java.io.File f = new java.io.File("D:/users/robin/temp");
//			for (java.io.File f2 : f.listFiles()) {
//				java.net.URI uri = f2.toURI();
//				System.out.println(uri.getScheme());
//				System.out.println(uri.toASCIIString());
//			}
//			System.out.println(new java.net.URL("http://user.aitia.ai/~meszaros_robert/MEME_for_release.rar").toURI().getScheme());


//			java.util.Random rnd = new java.util.Random();
//			for (int i = 10; i >= 0; --i) {
//				Double d = Math.pow(((int)1+rnd.nextDouble()*1000)/10.0, Math.round(1/rnd.nextDouble()));
//				//d = 0.0;
//				String ds = //String.format("%.3e", d);
//					d.toString();
//				String s = proba(d);
//				boolean same = s.equals(ds);
//				if (!same) {
//					System.out.println(String.format("eredeti: %s \t javitott: %s", ds, s));
//				}
//			}

			int cnt = 100000;
			long tic, toc;

			java.text.DateFormat fmt = new java.text.SimpleDateFormat("HH:mm:ss.SSS");
			fmt.setTimeZone(new java.util.SimpleTimeZone(0, "noDST"));
			tic = System.currentTimeMillis();
			for (int i = cnt; i > 0; --i) {
				fmt.format(new java.util.Date(System.currentTimeMillis()));
			}
			toc = System.currentTimeMillis();
			System.out.println(String.format("Speed: %g/msec", cnt/(double)(toc - tic)));

			tic = System.currentTimeMillis();
			for (int i = cnt; i > 0; --i) {
				String.format("%1$tT.%1$tL", System.currentTimeMillis());
			}
			toc = System.currentTimeMillis();
			System.out.println(String.format("Speed: %g/msec", cnt/(double)(toc - tic)));

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
}
