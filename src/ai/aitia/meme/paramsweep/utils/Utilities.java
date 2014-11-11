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
package ai.aitia.meme.paramsweep.utils;

import java.awt.Component;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.CtClass;
import javassist.Modifier;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import ai.aitia.meme.gui.SimpleFileFilter;
import ai.aitia.meme.paramsweep.classloader.RetryLoader;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.OperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.TestableDialog;
import ai.aitia.meme.utils.Utils;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import fables.paramsweep.runtime.annotations.Synthetic;

/** Collection of utility functions. */
public class Utilities {
	
	//----------------------------------------------------------------------------------------------------
	public interface IEBinary<T,P> { public void run(T arg1, P arg2) throws Exception; }

	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	public static class CancelImportException extends Exception {}
	
	//-------------------------------------------------------------------------
	/** It shows a confirmation dialog. 
	 *  Return values: 1=yes, 0=no, -1=cancel or the user closed the window with the right-upper 'x'.
	 *  <br>imported from MEME (modified)
	 *  @param allowCancel is 'Cancel' button on the dialog or not.
	 *  @param title the title of the dialog
	 *  @param messages messages on the dialog
	 *  @return the response of the user
	 */ 
	public static int askUser(Component parent, boolean allowCancel, String title, Object ... messages) {
		if (messages.length == 1 && messages[0] instanceof Object[])
			messages = (Object[])messages[0];
		if (title == null) {
			title = "Confirmation";
		}
		/*int ans = JOptionPane.showConfirmDialog(parent, messages, title,
					allowCancel ? JOptionPane.YES_NO_CANCEL_OPTION : JOptionPane.YES_NO_OPTION);*/
		int ans = TestableDialog.showConfirmDialog(parent, messages, title,
				allowCancel ? JOptionPane.YES_NO_CANCEL_OPTION : JOptionPane.YES_NO_OPTION, "");
		if (ans == JOptionPane.YES_OPTION)
			return 1;
		if (ans == JOptionPane.NO_OPTION)
			return 0;
		return -1;
	}
	
	//-------------------------------------------------------------------------
	/** It shows a confirmation dialog. 
	 *  Return values: 1=ok, 0=cancel
	 *  @param title the title of the dialog
	 *  @param messages messages on the dialog
	 *  @return the response of the user
	 */ 
	public static int askUserOK(Component parent, String title, Object ... messages) {
		if (messages.length == 1 && messages[0] instanceof Object[])
			messages = (Object[])messages[0];
		if (title == null) {
			title = "Warning";
		}
		//int ans = JOptionPane.showConfirmDialog(parent, messages, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
		int ans = TestableDialog.showConfirmDialog(parent, messages, title, JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE, "");
		if (ans == JOptionPane.OK_OPTION)
			return 1;
		if (ans == JOptionPane.CANCEL_OPTION)
			return 0;
		return -1;
	}
	
	//-----------------------------------------------------------------------------
	/** It shows the <code>messages</code> in a modal dialog.  
	 *  <br>imported from MEME (modified)
	 */
	public static void userAlert(Component parent, Object ... messages) {
		if (messages.length == 1 && messages[0] instanceof Object[])
			messages = (Object[])messages[0];
		
		/*JOptionPane pane = new JOptionPane(messages);

		pane.setWantsInput(false);
		int style = JRootPane.WARNING_DIALOG;		//lehet hogy meg kell majd változtatni
		JDialog dialog = pane.createDialog(parent,"Message");
		pane.selectInitialValue();

		Component[] gombok = ((JPanel)pane.getComponents()[1]).getComponents();
		gombok[0].setName("btn_ok");
		dialog.setName("dial_useralert");
		dialog.setVisible(true);
					
		dialog.dispose();*/
		
		TestableDialog.showMessageDialog(parent, messages, "dial_useralert");
		//javax.swing.JOptionPane.showMessageDialog(parent, messages);
	}

	//------------------------------------------------------------------------------
	/** Returns all fields of <code>clazz</code> that can be access from an derived class. */ 
	public static Field[] getAccessibleFields(CtClass modelClass, Class clazz) {
		List<Field> result = new ArrayList<Field>();
		String packageName = getPackageName(modelClass.getName());
		getAccessibleFieldsImpl(clazz,result,packageName);
		return result.toArray(new Field[0]);
	}
	
	/** Implements the functions of {@link Util#getFields(Class) getFields()}. */
	private static void getAccessibleFieldsImpl(Class clazz, List<Field> result, String packageName) {
		Field[] fields = clazz.getDeclaredFields();
		boolean samePackage = getPackageName(clazz.getName()).equals(packageName); 
		for (Field f : fields) {
			Synthetic syn = f.getAnnotation(Synthetic.class);
			if (syn == null && !f.isSynthetic()) {
				int modifier = f.getModifiers();
				if (Modifier.isPublic(modifier))
					result.add(f);
				else if (Modifier.isPackage(modifier) && samePackage)
					result.add(f);
			}
		}
		Class parent = clazz.getSuperclass();
		if (parent != null)
			getAccessibleFieldsImpl(parent,result,packageName);
	}

	//----------------------------------------------------------------------------------------------------
	/** Returns the package name of the class specified by <code>className</code>. */
	private static String getPackageName(String className) {
		int index = className.lastIndexOf('.');
		if (index == -1) return ""; // default package
		return className.substring(0,index);
	}
	
	//------------------------------------------------------------------------------
	/** Returns all methods of <code>clazz</code> that can be access from an derived class. */ 
	public static Method[] getAccessibleMethods(CtClass modelClass, Class clazz) {
		List<Method> result = new ArrayList<Method>();
		List<String> synthetics = new ArrayList<String>();
		String packageName = getPackageName(modelClass.getName());
		getAccessibleMethodsImpl(clazz,result,packageName,synthetics);
		return result.toArray(new Method[0]);
	}
	
	/** Implements the functions of {@link Util#getMethods(Class) getMethods()}. */
	private static void getAccessibleMethodsImpl(Class clazz, List<Method> result, String packageName, List<String> synthetics) {
		Method[] methods = clazz.getDeclaredMethods();
		boolean samePackage = getPackageName(clazz.getName()).equals(packageName); 
		for (Method m : methods) {
			Synthetic syn = m.getAnnotation(Synthetic.class);
			if (syn == null && !m.isSynthetic() && !synthetics.contains(m.getName())) {
				int modifier = m.getModifiers();
				if (Modifier.isAbstract(modifier)) continue;
				if (Modifier.isPublic(modifier))
					result.add(m);
				else if (Modifier.isPackage(modifier) && samePackage)
					result.add(m);
			} else if (syn != null)
				synthetics.add(m.getName());
		}
		Class parent = clazz.getSuperclass();
		if (parent != null)
			getAccessibleMethodsImpl(parent,result,packageName,synthetics);
	}

	//------------------------------------------------------------------------------
	/** Supported classes for parameter types. */
	private static List<Class> supportedClasses = new ArrayList<Class>(8);
	
	static {
		supportedClasses.add(String.class);
		supportedClasses.add(Boolean.class);
		supportedClasses.add(Byte.class);
		supportedClasses.add(Short.class);
		supportedClasses.add(Integer.class);
		supportedClasses.add(Long.class);
		supportedClasses.add(Float.class);
		supportedClasses.add(Double.class);
		supportedClasses.add(File.class);
		supportedClasses.add(Number.class);
	}
	
	/** Returns the string representation of <code>type</code>. This method
	 *  can be used with primitive types (except char) or supported classes
	 *  contained by <code>supportedClasses</code>.*/
	public static String toTypeString(Class type) {
		if (type.equals(Byte.TYPE)) return "byte";
		if (type.equals(Short.TYPE)) return "short";
		if (type.equals(Integer.TYPE)) return "int";
		if (type.equals(Long.TYPE)) return "long";
		if (type.equals(Float.TYPE)) return "float";
		if (type.equals(Double.TYPE)) return "double";
		if (type.equals(Boolean.TYPE)) return "boolean";
		if (supportedClasses.contains(type)) return type.getSimpleName();
		return null;
	}
	
	/** Returns the string representation of <code>type</code>. */
	public static String toTypeString1(Class type) {
		String result = toTypeString(type);
		if (result == null) {
			if (type.equals(Character.TYPE)) return "char";
			if (List.class.isAssignableFrom(type)) 
					return "List"; // we don't know the component type
			return type.getSimpleName();
		}
		return result;
	}
	
	//------------------------------------------------------------------------------------
	/** Returns the name of the type specified by the parameter. */
	public static String toTypeString2(Class type) {
		String result = toTypeString(type);
		if (result == null) {
			if (type.equals(Character.TYPE)) return "char";
			if (type.isArray()) {
				int dim = 0;
				Class compType = type;
				while (compType.isArray()) {
					compType = compType.getComponentType();
					dim++;
				}
				return compType.getName() + Utils.repeat("[]",dim,""); 
			}
			return type.getName();
		}
		return result;
	}
	
	//-------------------------------------------------------------------------
	/** Returns the starter body-tag.<br>  
	 * imported from MEME (modified)
	 */
    public static String htmlBody() {
    	return "<body style=\"font-family:arial;font-size:100%\">";
    }

	//-------------------------------------------------------------------------
    /** Returns 'htmlText' as a HTML-source (with &lt;html&gt;,&lt;body&gt;,&lt;/body&gt;,&lt;/html&gt; tags.<br>
     *  imported from MEME
     */
    public static String htmlPage(String htmltext) {
    	return "<html>" + htmlBody() + ' ' + htmltext + " </body></html>";
    }
    
    //-------------------------------------------------------------------------
	/** Returns the lines of the stack trace belongs to exception 't' in a string.<br>
     *  imported from MEME
	 */ 
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
	/** Returns the lines of the stack trace belongs to exception 't' in a string array.<br>
	 *  imported from MEME
	 */
	public static String[] getStackTraceLines(Throwable t, String before, String after) {
		if (before == null) before = ""; else before = before + '\n';
		if (after == null) after = "";  else after = '\n' + after;
		return (before + getStackTraceLines(t) + after).split("\n");
	}
	
	//-------------------------------------------------------------------------
    /** Sets 'text' as the content of the 'textpane'
	 *	imported from MEME (modified)
     */
    public static void setTextPane(javax.swing.JEditorPane textpane, String text) {
		// Swing throws IllegalArgumentException if I try to change the content type
		// from "text/html" to "text/plain". Therefore I make the change in one 
    	// direction only (plain -> html) but not back. 
    	if (GUIUtils.g_baseURL == null) {
    		GUIUtils.g_baseURL = SimpleFileFilter.class.getResource("/ai/aitia/meme/dummy.txt");
    	}
    	GUIUtils.setTextPane(textpane,text);
    }
    
    //----------------------------------------------------------------------------------------------------
	public static String[] getModelNameAndTimeStamp(String modelNameWithTimeStamp) {
		int index = modelNameWithTimeStamp.lastIndexOf("__");
		if (index == -1)
			return new String[] { modelNameWithTimeStamp, "" };
		String name = modelNameWithTimeStamp.substring(0,index);
    	String _timestamp = modelNameWithTimeStamp.substring(index + 2);
    	String[] parts = _timestamp.split("_");
    	String timestamp = parts[0] + "/" + parts[1] + "/" + parts[2] + " " +
    					   parts[3] + ":" + parts[4] + ":" + parts[5];
    	return new String[] { name, timestamp };
	}
	
	//----------------------------------------------------------------------------------------------------
	public static Date getTimeStamp(String modelNameWithTimeStamp) throws ParseException {
		int index = modelNameWithTimeStamp.lastIndexOf("__");
		if (index == -1)
			throw new IllegalArgumentException();
    	String _timestamp = modelNameWithTimeStamp.substring(index + 2);
    	String[] parts = _timestamp.split("_");
    	int hour = Integer.parseInt(parts[3]);
    	String mode = " AM";
    	if (hour >= 12) { 
    		mode = " PM";
    		if (hour > 12)
    			hour -= 12;
    	}
    	
    	String timestamp = parts[0] + "/" + parts[1] + "/" + parts[2] + " " +
    					   String.valueOf(hour) + ":" + parts[4] + ":" + parts[5] + mode;
		DateFormat f = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.MEDIUM,Locale.US);
		Date date = f.parse(timestamp);
		return date;
	}
    
    //-----------------------------------------------------------------------------------
    /** Returns a\b using contains() method. */
    public static <T> List<T> listSubtract(List<T> aList, List<T> bList) {
    	if (aList == null || bList == null)
    		throw new IllegalArgumentException();
    	List<T> result = new ArrayList<T>();
    	for (T t : aList) {
    		if (!bList.contains(t))
    			result.add(t);
    	}
    	return result;
    }
    
    //----------------------------------------------------------------------------------------------------
    public static Class<?> toClass(ClassLoader loader, String javaTypeStr) throws ClassNotFoundException {
    	if (javaTypeStr == null || "null".equals(javaTypeStr)) return null;
    	if (javaTypeStr.equals("void")) return Void.TYPE;
    	if (javaTypeStr.equals("byte")) return Byte.TYPE;
    	if (javaTypeStr.equals("short")) return Short.TYPE;
    	if (javaTypeStr.equals("int")) return Integer.TYPE;
    	if (javaTypeStr.equals("long")) return Long.TYPE;
    	if (javaTypeStr.equals("float")) return Float.TYPE;
    	if (javaTypeStr.equals("double")) return Double.TYPE;
    	if (javaTypeStr.equals("boolean")) return Boolean.TYPE;
    	if (javaTypeStr.equals("char")) return Character.TYPE;
    	if (loader instanceof RetryLoader) {
			RetryLoader rl = (RetryLoader) loader;
			rl.stopRetry();
		}
    	try {
    		Class<?> result = Class.forName(javaTypeStr,true,loader); 
    		return result;
    	} finally {
    		if (loader instanceof RetryLoader) {
    			RetryLoader rl = (RetryLoader) loader;
    			rl.startRetry();
    		}
    	}
    }
    
    //----------------------------------------------------------------------------------------------------
	public static String name(MemberInfo member) {
		return member instanceof OperatorGeneratedMemberInfo ? ((OperatorGeneratedMemberInfo)member).getDisplayName() : member.getName();
	}
	
	//----------------------------------------------------------------------------------------------------
	public static boolean isPrimitive(Class<?> type) {
		if (type == null) return false;
		if (type.isPrimitive() && !type.equals(Character.TYPE) && !type.equals(Boolean.TYPE))
			return true;
		if (supportedClasses.contains(type) && !type.equals(String.class) && !type.equals(Boolean.class) && !type.equals(Number.class))
			return true;
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	public static boolean isCompatibleWithMaskedShellFolderManager() {
//		String version = System.getProperty("java.runtime.version");
//		try {
//			int minorNr = 0;
//			String[] parts = version.split("_");
//			if (parts.length >= 2) {
//				int idx = parts[1].indexOf('-');
//				String minor = idx == -1 ? parts[1] : parts[1].substring(0,idx);
//				minorNr = Integer.parseInt(minor);
//			}
//			parts = parts[0].split("\\.");
//			int majorNr = Integer.parseInt(parts[1]);
//			return majorNr < 6 || minorNr < 14;
//		} catch (Throwable t) {
//			return false;
//		}
		
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	// parameter globalRun is negative if the platform not support global runs
	public static long calculateNumberOfRuns(final DefaultMutableTreeNode root, final long globalRun) {
		@SuppressWarnings("unchecked") 
		Enumeration<TreeNode> children = root.children();
		
		long result = Long.MAX_VALUE;
		boolean allConstants = true;
		DefaultMutableTreeNode node = null;
		while (children.hasMoreElements()) {
			node = (DefaultMutableTreeNode) children.nextElement();
			ParameterInfo info = null;
			try {
				info = (ParameterInfo) node.getUserObject();
			} catch (ClassCastException e) { continue; }
			
			if (info.isConstant()) continue ; // these are constants
			else {
				allConstants = false;
				final long actBranch = _calculateNumberOfRuns(node,globalRun);
				if (actBranch < result)
					result = actBranch;
			}
		}
		long _globalRun = globalRun;
		if (globalRun <= 0)
			_globalRun = 1;
		if (allConstants)
			result = 1;
		return _globalRun * result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static long _calculateNumberOfRuns(final DefaultMutableTreeNode branch, final long globalRun) {
		final ParameterInfo info = (ParameterInfo) branch.getUserObject();
		long run = 1;
		if (globalRun <= 0)
			run = info.getRuns();
		final long result = run * info.getMultiplicity();
		return branch.isLeaf() ? result : result * _calculateNumberOfRuns((DefaultMutableTreeNode)branch.getFirstChild(),globalRun);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static final String FILTER_EXP = "%value%"; 
	
	//----------------------------------------------------------------------------------------------------
	private static final String salt = "FvHBedW4st1dsaa18Y1B1dJ4";
	private static final IvParameterSpec IvParameters = new IvParameterSpec( new byte[] { 12, 34, 56, 78, 90, 87, 65, 43 });

	/** Key to encode/decode informations. */
	private static Key key = null;

	//------------------------------------------------------------------------------------
	/** Encodes <code>password</code> and returns the encoded version.
	 * @throws Exception if any problem occurs
	 */
	public static String encode(String password) throws Exception {
		if (key == null) {
			try {
				key = SecretKeyFactory.getInstance("DESede").generateSecret(new DESedeKeySpec(salt.getBytes()));
			} catch (final Exception e) {
				// never happens
				throw new IllegalStateException();
			}
		}
		try {
			final Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE,key,IvParameters);
			final byte[] encoded = cipher.doFinal(password.getBytes("UTF-8"));
			return Base64.encode(encoded);
		} catch (final InvalidKeyException _) {
			throw new IllegalStateException("invalid key");
		} catch (final NoSuchAlgorithmException _) {
			// never happens
			throw new IllegalStateException();
		}
	}
	
	//------------------------------------------------------------------------------------
	/** Decodes <code>encoded_password</code> and returns the decoded version.
	 * @throws Exception if any problem occurs
	 */
	public static String decode(String encoded_password) throws Exception {
		if (key == null) {
			try {
				key = SecretKeyFactory.getInstance("DESede").generateSecret(new DESedeKeySpec(salt.getBytes()));
			} catch (final Exception e) {
				// never happens
				throw new IllegalStateException();
			}
		}
		try {
			final Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE,key,IvParameters);
			final byte[] decoded = cipher.doFinal(Base64.decode(encoded_password));
			return new String(decoded,"UTF-8");
		} catch (final InvalidKeyException _) {
			throw new IllegalStateException("invalid key");
		} catch (final NoSuchAlgorithmException _) {
			// never happens
			throw new IllegalStateException();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private static Pattern emailPattern = Pattern.compile(".+@.+\\.[a-z]+");
	
	public static boolean validateEmail(final String email) {
	   // Input the string for validation
	   // String email = "xyz@.com";
	   // Set the email pattern string
	   // Match the given string with the pattern
	   final Matcher m = emailPattern.matcher(email);

	   // check whether match is found
	   boolean matchFound = m.matches();

	   final StringTokenizer st = new StringTokenizer(email,".");
	   String lastToken = null;
	   while (st.hasMoreTokens()) 
		   lastToken = st.nextToken();
	   return matchFound && lastToken.length() >= 2 && email.length() - 1 != lastToken.length(); 
	}
	
	//----------------------------------------------------------------------------------------------------
	public static String md5(final String original) {
		try {
			final byte[] originalBytes = original.getBytes();
			final MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(originalBytes);
			final byte messageDigest[] = algorithm.digest();
			final BigInteger number = new BigInteger(1,messageDigest);
			final String tmp = number.toString(16);
			String result = "";
			if (tmp.length() < 32){
				int sum = 32 - tmp.length();
				for (int i = 0;i < sum;i++) 
					result = result.concat("0");
			} 
			result = result.concat(tmp);
			return result;
		} catch (final NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
	
 	//----------------------------------------------------------------------------------------------------
	public static boolean contains(final int[] array, final int element) {
		for (final int i : array) {
			if (i == element) return true;
		}
		return false;
	}

}
