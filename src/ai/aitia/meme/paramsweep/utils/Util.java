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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;

import javax.swing.text.Utilities;

import org.reflections.ReflectionUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.jcraft.jsch.SftpException;

import ai.aitia.meme.paramsweep.batch.param.ParameterNode;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;
import ai.aitia.meme.paramsweep.cloud.util.IFileTranserService.OperationFailedException;
import ai.aitia.meme.utils.Utils;
import fables.paramsweep.runtime.annotations.Synthetic;

/**
 * Methods can be traced using logger configuration entry named after the
 * package and FINER level.
 */
public class Util {
	private static final Logger logger = Logger.getLogger(Util.class
			.getPackage().getName());

	// -------------------------------------------------------------------------------
	/** List of acceptable parameter types. */
	private static List<Class> acceptableTypes = new ArrayList<Class>(16);

	static {
		Util.acceptableTypes.add(Byte.TYPE);
		Util.acceptableTypes.add(Byte.class);
		Util.acceptableTypes.add(Short.TYPE);
		Util.acceptableTypes.add(Short.class);
		Util.acceptableTypes.add(Integer.TYPE);
		Util.acceptableTypes.add(Integer.class);
		Util.acceptableTypes.add(Long.TYPE);
		Util.acceptableTypes.add(Long.class);
		Util.acceptableTypes.add(Float.TYPE);
		Util.acceptableTypes.add(Float.class);
		Util.acceptableTypes.add(Double.TYPE);
		Util.acceptableTypes.add(Double.class);
		Util.acceptableTypes.add(Boolean.TYPE);
		Util.acceptableTypes.add(Boolean.class);
		Util.acceptableTypes.add(String.class);
		Util.acceptableTypes.add(Number.class);
		
		Util.acceptableTypes.add(byte[].class);
		Util.acceptableTypes.add(Byte[].class);
		Util.acceptableTypes.add(short[].class);
		Util.acceptableTypes.add(Short[].class);
		Util.acceptableTypes.add(int[].class);
		Util.acceptableTypes.add(Integer[].class);
		Util.acceptableTypes.add(long[].class);
		Util.acceptableTypes.add(Long[].class);
		Util.acceptableTypes.add(float[].class);
		Util.acceptableTypes.add(Float[].class);
		Util.acceptableTypes.add(double[].class);
		Util.acceptableTypes.add(Double[].class);
		Util.acceptableTypes.add(boolean[].class);
		Util.acceptableTypes.add(Boolean[].class);
		Util.acceptableTypes.add(String[].class);
		Util.acceptableTypes.add(Number[].class);
		
		Util.acceptableTypes.add(Collection.class); // why do we need this here? it won't match any actual collection! (it shouldn't even)
	}
	
	public static final String GENERATED_MODEL_MODEL_FIELD_NAME = "aitiaGeneratedOriginalModel";

	public static final String GENERATED_MODEL_RECORDER_VARIABLE_NAME = "aitiaGeneratedRecorder";

	public static final String GENERATED_MODEL_MULTICOLUMN_POSTFIX = "Multi()";
	
	public static boolean isAcceptableType(Class<?> type){
		return acceptableTypes.contains(type) || Collection.class.isAssignableFrom(type) || Enum.class.isAssignableFrom(type); 
	}

	public static boolean isAcceptableSimpleType(Class<?> type){
		return (!type.isArray() && acceptableTypes.contains(type)) || Enum.class.isAssignableFrom(type);
	}
	
	private Util() {
	}

	/**
	 * Unzips an archive including dir hierarchy under the current directory.
	 * 
	 * @param zipped
	 * @throws IOException
	 */
	public static void unzip(File zipped) throws IOException {
		unzip(zipped,"." + File.separator);
	}

	public static void unzip(File zipped, File prependix) throws IOException {
		unzip(zipped,prependix.getAbsolutePath() + File.separator);
	}

	/**
	 * Unzips an archive including dir hierarchy under the prepended directory.
	 * 
	 * @param zipped
	 * @throws IOException
	 */
	public static void unzip(File zipped, String prependix) throws IOException {
		ZipFile zipFile = new ZipFile(zipped);
		try {
			Enumeration entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();

				int idx = entry.getName().lastIndexOf('/');
				if (idx >= 0) {
					String dirStr = entry.getName().substring(0,idx);
					logger.finer("Creating dirs: " + dirStr);
					new File(prependix + dirStr).mkdirs();
				}

				logger.finer("Extracting file: " + entry.getName());
				if (!entry.getName().endsWith("/"))
					copyInputStream(zipFile.getInputStream(entry),
							new BufferedOutputStream(new FileOutputStream(
									prependix + entry.getName())));
			}
		} finally {
			zipFile.close();
		}
	}

	public static void unzip(File zipped, String prependix, String extension)
			throws IOException {
		ZipFile zipFile = new ZipFile(zipped);

		try {
			Enumeration entries = zipFile.entries();

			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();

				if (entry.getName().endsWith(extension))
					copyInputStream(zipFile.getInputStream(entry),
							new BufferedOutputStream(new FileOutputStream(
									prependix + entry.getName())));
			}
		} finally {
			zipFile.close();
		}
	}

	public static void unzipHere(File zipped, String extension)
			throws IOException {
		ZipFile zipFile = new ZipFile(zipped);

		try {
			Enumeration entries = zipFile.entries();

			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();

				if (entry.getName().endsWith(extension)) {
					String fileName = entry.getName();
					int idx = entry.getName().lastIndexOf('/');
					if (idx >= 0)
						fileName = fileName.substring(idx + 1);
					copyInputStream(zipFile.getInputStream(entry),
							new BufferedOutputStream(new FileOutputStream(
									fileName)));
				}
			}
		} finally {
			zipFile.close();
		}

	}
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * Zips files from a directory, or just one file. Does not go into subdirectories recursively, only zips files.
	 * @param sourceDir	The directory that is to be compressed, or it can point to a single file.
	 * @param zipFile	The resultant zip file.
	 * @throws IOException
	 */
	public static void zip(final File sourceDir, final File zipFile) throws IOException {
		if (!sourceDir.exists()) return;
		
		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream(new FileOutputStream(zipFile));
			zos.setLevel(Deflater.BEST_COMPRESSION);
			
			File[] files = null;
			if (sourceDir.isDirectory())
				files = sourceDir.listFiles();
			else if (sourceDir.isFile())
				files = new File[] { sourceDir };

			ZipEntry ze = null;
			InputStream is = null;
			for (final File f : files) {
				if (!f.isFile()) continue; // just files
				if (zipFile.getPath().equals(f.getPath())) continue;
				try {
					ze = new ZipEntry(f.getName());
					ze.setTime(f.lastModified());
					is = new FileInputStream(f);
					zos.putNextEntry(ze);
					copyInputStreamOutRemainsOpen(is,zos);
					zos.closeEntry();
				} catch (ZipException e) {
					if (!e.getLocalizedMessage().startsWith("duplicate entry")) 
						throw e;
				}				
			}
		} finally {
			if (zos != null)
				zos.close();
		}
	}

	public static void copyInputStream(InputStream in, OutputStream out)
			throws IOException {
		try {
			byte[] buffer = new byte[1024];
			int len;

			while ((len = in.read(buffer)) >= 0)
				out.write(buffer,0,len);
		} finally {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void copyFolder(final File srcFolder, final File destFolder) throws IOException {
		if (srcFolder.isDirectory()) {
			if (!destFolder.exists())
				destFolder.mkdir();
			
			final File[] files = srcFolder.listFiles();
			for (final File f : files)
				copyFolder(f,new File(destFolder,f.getName()));
		} else {
			if (destFolder.isDirectory())
				copyFile(srcFolder,new File(destFolder,srcFolder.getName()));
			else
				copyFile(srcFolder,destFolder);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void copyFile(final File src, final File dest) throws IOException {
		final InputStream in = new FileInputStream(src);
		final OutputStream out = new FileOutputStream(dest);
		copyInputStream(in,out);
	}
	
	public static void copyInputStreamOutRemainsOpen(final InputStream in, final OutputStream out) throws IOException {
		try {
			byte[] buffer = new byte[1024];
			int len;

			while ((len = in.read(buffer)) >= 0)
				out.write(buffer,0,len);
		} finally {
			if (in != null)
				in.close();
		}
	}

	/**
	 * Deletes a directory's content, recursively. If rootToo is true, deletes
	 * the directory itself, too.
	 * 
	 * @param dir
	 * @return
	 */
	public static boolean delete(File dir, boolean rootToo) {
		if (!dir.exists()) {
			return true;
		}
		boolean res = true;
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (int i = 0; i < files.length; i++) {
				res &= delete(files[i],rootToo);
			}
			if (rootToo) {
				res = dir.delete();// Delete dir itself
			}
		} else {
			res = dir.delete();
		}
		return res;
	}

	// ----------------------------------------------------------------------------------
	/** Returns the current date and time in string format. */
	public static String getTimeStamp() {
		Date current = new Date();
		DateFormat df1 = DateFormat.getDateInstance(DateFormat.SHORT,Locale.US);
		DateFormat df2 = DateFormat
				.getTimeInstance(DateFormat.MEDIUM,Locale.US);
		String time = df2.format(current);
		String code = time.substring(time.length() - 3).trim();
		time = time.substring(0,time.length() - 3);
		if ("PM".equals(code) || ("pm".equals(code))) {
			String[] parts = time.split(":");
			int hour = Integer.parseInt(parts[0].trim());
			hour += hour < 12 ? 12 : 0;
			time = String.valueOf(hour) + ":" + parts[1] + ":" + parts[2];
		}
		String stamp = df1.format(current) + "_" + time;
		stamp = stamp.replace('/','_');
		stamp = stamp.replace(':','_');
		stamp = stamp.replace(' ','_');
		return stamp;
	}

	// -------------------------------------------------------------------------
	/**
	 * Returns the localized message of exception 't'. In special case (e.g.
	 * OutOfMemeryError it returns the more useful message than the localized
	 * message.
	 */
	public static String getLocalizedMessage(Throwable t) {
		if (t == null)
			return "";
		String s = null;
		if (t instanceof NullPointerException) {
			// The localized message of this exception is 'null'
			s = "Null pointer.";
		} else if ((t instanceof OperationFailedException) || (t instanceof SftpException) && "4".equals(t.getLocalizedMessage()))
			s = "Connection lost.";
		else
			s = Utils.getLocalizedMessage(t);
		return s;
	}

	// ------------------------------------------------------------------------------
	/**
	 * Capitalizes <code>orig</code> (if necessary) and returns as a new
	 * string.
	 */
	public static String capitalize(String orig) {
		if (orig.equals(""))
			return orig;
		char start = orig.charAt(0);
		return Character.isLowerCase(start) ? Character.toUpperCase(start)
				+ orig.substring(1) : orig;
	}

	// ------------------------------------------------------------------------------
	/**
	 * Uncapitalizes <code>orig</code> (if necessary) and returns as a new
	 * string.
	 */
	public static String uncapitalize(String orig) {
		if (orig.equals(""))
			return orig;
		char start = orig.charAt(0);
		return Character.isLowerCase(start) ? orig : Character
				.toLowerCase(start)
				+ orig.substring(1);
	}

	// ------------------------------------------------------------------------------
	/**
	 * Changes the all members of <code>clazz</code> with protected or
	 * package-level visibility to public.
	 */
	public static void all2Public(CtClass clazz) {
		CtField[] fields = clazz.getDeclaredFields();
		for (CtField field : fields) {
			int modifier = field.getModifiers();
			if (/* Modifier.isPrivate(modifier) || */Modifier
					.isPackage(modifier)
					|| Modifier.isProtected(modifier))
				field.setModifiers(Modifier.setPublic(modifier));
		}

		CtMethod[] methods = clazz.getDeclaredMethods();
		for (CtMethod method : methods) {
			int modifier = method.getModifiers();
			if (/* Modifier.isPrivate(modifier) || */Modifier
					.isPackage(modifier)
					|| Modifier.isProtected(modifier))
				method.setModifiers(Modifier.setPublic(modifier));
		}
	}

	// ------------------------------------------------------------------------------
	/**
	 * Returns all fields of <code>clazz</code> that can be access from an
	 * derived class.
	 */
	public static Field[] getFields(Class clazz) {
		List<Field> result = new ArrayList<Field>();
		String packageName = getPackageName(clazz.getName());
		getFieldsImpl(clazz,result,packageName);
		return result.toArray(new Field[0]);
	}

	/**
	 * Implements the functions of
	 * {@link Utilities#getFields(Class) getFields()}.
	 */
	public static void getFieldsImpl(Class clazz, List<Field> result,
			String packageName) {
		Field[] fields = clazz.getDeclaredFields();
		boolean samePackage = getPackageName(clazz.getName()).equals(
				packageName);
		for (Field f : fields) {
			Synthetic syn = f.getAnnotation(Synthetic.class);
			if (syn == null && !f.isSynthetic()) {
				int modifier = f.getModifiers();
				if (Modifier.isPublic(modifier)
						|| Modifier.isProtected(modifier))
					result.add(f);
				else if (Modifier.isPackage(modifier) && samePackage)
					result.add(f);
			}
		}
		Class parent = clazz.getSuperclass();
		if (parent != null)
			getFieldsImpl(parent,result,packageName);
	}

	// ------------------------------------------------------------------------------
	/**
	 * Returns all methods of <code>clazz</code> that can be accessed from a
	 * derived class.
	 */
	public static Method[] getMethods(Class clazz) {
		Map<String, Method> result = new HashMap<String, Method>();
		List<String> synthetics = new ArrayList<String>();
		String packageName = getPackageName(clazz.getName());
		getMethodsImpl(clazz,result,packageName,synthetics);
		return result.values().toArray(new Method[0]);
	}

	/**
	 * Implements the functions of
	 * {@link Utilities#getMethods(Class) getMethods()}.
	 */
	public static void getMethodsImpl(Class clazz, Map<String, Method> result,
			String packageName, List<String> synthetics) {
		Method[] methods = clazz.getDeclaredMethods();
		boolean samePackage = getPackageName(clazz.getName()).equals(
				packageName);
		for (Method m : methods) {
			String key = m.getName() + "(" + Arrays.toString(m.getGenericParameterTypes()) + ")";
			if (result.keySet().contains(key)){
				continue;
			}
			Synthetic syn = m.getAnnotation(Synthetic.class);
			if (syn == null && !m.isSynthetic()
					&& !synthetics.contains(m.getName())) {
				int modifier = m.getModifiers();
				if (Modifier.isAbstract(modifier))
					continue;
				if (Modifier.isPublic(modifier)
						|| Modifier.isProtected(modifier))
					result.put(key, m);
				else if (Modifier.isPackage(modifier) && samePackage)
					result.put(key, m);
			} else if (syn != null)
				synthetics.add(m.getName());
		}
		Class parent = clazz.getSuperclass();
		if (parent != null)
			getMethodsImpl(parent,result,packageName,synthetics);
	}

	// ----------------------------------------------------------------------------------------------------
	/** Returns the package name of the class specified by <code>className</code>. */
	public static String getPackageName(String className) {
		int index = className.lastIndexOf('.');
		if (index == -1)
			return ""; // default package
		return className.substring(0,index);
	}

	// ----------------------------------------------------------------------------------------------------
	public static interface IStoppableRetryLoader {
		public void startRetry();

		public void stopRetry();
	}

	// ----------------------------------------------------------------------------------------------------
	public static Class<?> convertToClass(String name, ClassLoader loader) {
		String tmp = "";
		int index = name.indexOf("[");
		int firstIndex = index;
		while (index != -1) {
			tmp += "[";
			index = name.indexOf("[",index + 1);
		}
		if (firstIndex != -1) {
			String core = name.substring(0,firstIndex);
			if ("boolean".equals(core))
				tmp += "Z";
			else if ("byte".equals(core))
				tmp += "B";
			else if ("char".equals(core))
				tmp += "C";
			else if ("double".equals(core))
				tmp += "D";
			else if ("float".equals(core))
				tmp += "F";
			else if ("int".equals(core))
				tmp += "I";
			else if ("long".equals(core))
				tmp += "J";
			else if ("short".equals(core))
				tmp += "S";
			else
				tmp += "L" + core + ";";
		} else
			tmp = name;
		if (loader instanceof IStoppableRetryLoader) {
			IStoppableRetryLoader rl = (IStoppableRetryLoader) loader;
			rl.stopRetry();
		}
		try {
			return Class.forName(tmp,true,loader);
		} catch (ClassNotFoundException e) {
		} finally {
			if (loader instanceof IStoppableRetryLoader) {
				IStoppableRetryLoader rl = (IStoppableRetryLoader) loader;
				rl.startRetry();
			}
		}
		return null;
	}

	// ----------------------------------------------------------------------------------------------------
	public static Class<?> innerType(Class<?> type) {
		if (type == null)
			return Void.TYPE;
		if (type.isArray())
			return type.getComponentType();
		if (!Collection.class.isAssignableFrom(type)
				&& !Map.class.isAssignableFrom(type))
			return Void.TYPE;
		return null;
	}

	// -----------------------------------------------------------------------------------
	/**
	 * Returns the displayable model name (with date and time) from
	 * <code>modelName</code> which contains timestamp.
	 */
	public static String restoreNameAndTimeStamp(String modelName) {
		int index = modelName.lastIndexOf("__");
		if (index == -1)
			return modelName;
		String name = modelName.substring(0,index);
		String _timestamp = modelName.substring(index + 2);
		String[] parts = _timestamp.split("_");
		String timestamp = parts[0] + "/" + parts[1] + "/" + parts[2] + " "
				+ parts[3] + ":" + parts[4] + ":" + parts[5];
		return name + " (" + timestamp + ")";
	}
	
	//----------------------------------------------------------------------------------------------------
	public static String getTimeStampFromXMLFileName(final String xmlFileName) {
		int idx = xmlFileName.lastIndexOf(".settings.xml");
		String result = xmlFileName.substring(0,idx);
		idx = result.lastIndexOf("__");
		result = result.substring(idx + 2);
		return result;
	}

	// ----------------------------------------------------------------------------------------------------
	public static String stackTrace(Throwable t) {
		StringWriter sw = new StringWriter();
		t.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

	// ----------------------------------------------------------------------------------------------------
	public static long calculateNumberOfRuns(final ParameterTree tree,
			final boolean isGlobalRun) {
		final Enumeration<ParameterNode> children = tree.children();
		long result = Long.MAX_VALUE;
		long globalRun = 1;
		ParameterNode node = null;
		boolean allConstants = true;
		while (children.hasMoreElements()) {
			node = children.nextElement();
			if (node.getParameterInfo().isOriginalConstant()) continue; // these are constants
			else {
				allConstants = false;
				final long actBranch = calculateNumberOfRuns(node,isGlobalRun);
				if (actBranch < result)
					result = actBranch;
			}
		}
		if (isGlobalRun)
			globalRun = node.getParameterInfo().getRunNumber();
		if (allConstants)
			result = 1;
		return globalRun * result;
	}

	// ----------------------------------------------------------------------------------------------------
	private static long calculateNumberOfRuns(final ParameterNode branch,
			final boolean isGlobalRun) {
		long run = 1;
		if (!isGlobalRun)
			run = branch.getParameterInfo().getRunNumber();
		final long result = run * branch.getParameterInfo().getMultiplicity();
		return branch.isLeaf() ? result : result
				* calculateNumberOfRuns((ParameterNode) branch.getFirstChild(),
						isGlobalRun);
	}

	// ----------------------------------------------------------------------------------------------------
	public static <E> List<E> addAllDistinct(final List<E> result,
			final Collection<? extends E> source) {
		for (final E element : source) {
			if (!result.contains(element))
				result.add(element);
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public static String boxingIfNeed(String core, Class<?> memberType) {
		StringBuilder code = new StringBuilder();
		code.append("result.add(");
		if (memberType.isPrimitive()) 
			code.append("new " + boxingType(memberType).getName() + "(");
		code.append(core);
		if (memberType.isPrimitive())
			code.append(")");
		code.append(");\n");
		return code.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	public static Class<?> boxingType(Class<?> memberType) {
		if (Byte.TYPE.equals(memberType)) return Byte.class;
		if (Short.TYPE.equals(memberType)) return Short.class;
		if (Integer.TYPE.equals(memberType)) return Integer.class;
		if (Long.TYPE.equals(memberType)) return Long.class;
		if (Float.TYPE.equals(memberType)) return Float.class;
		if (Double.TYPE.equals(memberType)) return Double.class;
		if (Boolean.TYPE.equals(memberType)) return Boolean.class;
		if (Character.TYPE.equals(memberType)) return Character.class;
		return memberType;
	}

}
