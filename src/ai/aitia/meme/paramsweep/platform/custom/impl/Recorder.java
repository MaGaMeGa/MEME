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
package ai.aitia.meme.paramsweep.platform.custom.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import ai.aitia.meme.Logger;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.utils.Utils.Pair;

public class Recorder {

	//====================================================================================================
	// members

	protected String fileName = null;
	protected String delimiter = null;
	protected ICustomGeneratedModel model = null;
	protected boolean writeHeader = true;
	
	private String parameterValues = null;
	protected Vector<Pair<String,Vector<String>>> data = new Vector<Pair<String,Vector<String>>>();
	protected Vector<Pair<String,Method>> sources = new Vector<Pair<String,Method>>();
	protected Method getParameterMethod = null;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public Recorder(String fileName, ICustomGeneratedModel model) { this(fileName,model,"|"); }
	
	//----------------------------------------------------------------------------------------------------
	public Recorder(String fileName, ICustomGeneratedModel model, String delimiter) {
		this.fileName = fileName;
		this.model = model;
		this.delimiter = delimiter;
		try {
			getParameterMethod = model.getClass().getMethod("getParameter",String.class);
		} catch (Exception e) {
			getParameterMethod = null;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean addSource(String name, String method) {
		try {
			Method m = model.getClass().getMethod(method);
			sources.add(new Pair<String,Method>(name,m));
			return true;
		} catch (NoSuchMethodException e) {
			return false;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void record() {
		Vector<String> v = new Vector<String>();
	    for (Pair<String,Method> source : sources) {
	    	Method m = source.getSecond();
			Object o = null;
			try {
				o = m.invoke(model);
			} catch (Exception e) {}
//	    	v.add(toStringWithoutScientificNotation(o,m.getReturnType()));
	    	v.add(o.toString());
	    }
	    data.add(new Pair<String,Vector<String>>(String.valueOf(model.getCurrentStep()),v));
	}
	
	//----------------------------------------------------------------------------------------------------
	public void writeToFile() {
		if (parameterValues == null)
			initializeParameterValues();
		BufferedWriter out = null;
	    try {
	      if (model.aitiaGenerated_getRun() == 1 && writeHeader) {
	        renameFile();
	        out = new BufferedWriter(new FileWriter(fileName,true));
	        out.write(getModelHeader());
	        out.newLine();
	        out.newLine();
	        out.write(getHeader());
	        out.newLine();
	        writeHeader = false;
	      }

	      if (out == null)
	        out = new BufferedWriter(new FileWriter(fileName, true));
	      
	      for (Pair<String,Vector<String>> row : data) {
	    	  StringBuffer b = new StringBuffer();
	    	  b.append(String.valueOf(model.aitiaGenerated_getRun())).append(delimiter).append(row.getFirst()).append(delimiter);
	    	  b.append(parameterValues);
	    	  for (String result : row.getSecond()) 
	    		  b.append(result).append(delimiter);
	    	  String rowStr = b.toString();
	    	  rowStr = rowStr.substring(0,rowStr.length() - delimiter.length());
	    	  out.write(rowStr);
	    	  out.newLine();
	      }
	    } catch (IOException ex) {
	    } finally {
	    	try {
				out.flush();
				out.close();
			} catch (IOException e) {}
	    	data.clear();
	    }
	}
	
	//----------------------------------------------------------------------------------------------------
	public void writeEnd() {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(fileName,true));
			Date date = new Date();
			String dateTime = DateFormat.getDateTimeInstance().format(date);
			out.newLine();
			out.write("End Time: " + dateTime);
	    } catch (IOException e) {
	    } finally {
	      try {
	        out.flush();
	        out.close();
	      } catch (Exception e1) {}
	    }
	}

	//====================================================================================================
	// assistant methods

	//----------------------------------------------------------------------------------------------------
//	private String toStringWithoutScientificNotation(Object num, Class<?> type) {
//		if (null == num)
//			return "null";
//		StringBuilder result = new StringBuilder();
//		String string = String.format(Locale.US,num.toString());
//		if (Float.TYPE.equals(type) || Float.class.equals(type) || Double.TYPE.equals(type) || Double.class.equals(type)) {
//			String[] split = string.trim().split("E");
//			if (split.length == 1) 
//				return string;
//			else {
//				int exp = Integer.parseInt(split[1]);
//				if (exp < 0) {
//					int _exp = -1 * exp;
//					for (int i = 0;i < _exp;++i) {
//						result.append("0");
//						if (i == 0)
//							result.append(".");
//					}
//					result.append(split[0].replaceAll("\\.",""));
//				} else {
//					int dotIndex = split[0].indexOf('.');
//					int fragment = split[0].substring(dotIndex,split[0].length()).length();
//					result.append(split[0].replaceAll("\\.",""));
//					if (fragment - exp > 0){
//						result.insert(exp + 1, '.');
//					} else {
//						for (int i = 0; i <= exp - fragment;++i){ 
//							result.append("0");
//						}
//					}
//				}
//				return result.toString();
//			}
//		} else
//			return string;
//	}
	
	//----------------------------------------------------------------------------------------------------
	private void renameFile() throws IOException {
	    File oldFile = new File(fileName);
	    fileName = oldFile.getCanonicalPath();

	    if (oldFile.exists()) {
	    	int x = 1;
	    	File newFile;
	    	String newName = fileName;
	    	String lastPart = "";

	    	if (fileName.indexOf(".") != -1) {
	    		int index = fileName.lastIndexOf(".");
	    		newName = fileName.substring(0,index);
	    		lastPart = fileName.substring(index,fileName.length());
	    	}

	        newName += ".bak";

		    do 
		    	newFile = new File(newName + x++ + lastPart);
		    while (newFile.exists());
		    oldFile.renameTo(newFile);
		    oldFile.delete();
	    }
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private String getModelHeader() {
		StringBuffer b = new StringBuffer();
	    b.append("Timestamp: ").append(DateFormat.getDateTimeInstance().format(new Date())).append("\n");
	    List<String> constants = model.aitiaGenerated_getConstantParameterNames();
	    for (String p : constants) {
	    	Method get = null;
	    	Object o = null;
			try {
				try {
					get = model.getClass().getMethod("get" + p);
					o = get.invoke(model);
				} catch (final NoSuchMethodException e) {
					try {
						get = model.getClass().getMethod("is" + p);
						o = get.invoke(model);
					} catch (final NoSuchMethodException e1) {
						if (getParameterMethod == null)
							throw new IllegalStateException();
						//o = getParameterMethod.invoke(model,Util.capitalize(p));
						if (o == null)
							o = getParameterMethod.invoke(model,Util.uncapitalize(p));
					}
				}
			} catch (IllegalStateException e) {
				throw e;
			} catch (Exception e) {}
//	    	b.append(p).append(": ").append(toStringWithoutScientificNotation(o,o.getClass())).append("\n");
	    	b.append(p).append(": ").append(o.toString()).append("\n");
	    }
	    return b.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private String getHeader() {
		StringBuffer b = new StringBuffer();
		b.append("\"run\"").append(delimiter).append("\"tick\"").append(delimiter);
		List<String> parameters = model.aitiaGenerated_getMutableParameterNames();
		for (String p : parameters)
			b.append("\"").append(p).append("\"").append(delimiter);
		for (Pair<String,Method> p : sources) 
			b.append("\"").append(p.getFirst()).append("\"").append(delimiter);
		String result = b.toString();
		return result.substring(0,result.length() - delimiter.length());
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private void initializeParameterValues() {
		StringBuffer b = new StringBuffer();
	    List<String> parameters = model.aitiaGenerated_getMutableParameterNames();
	    for (String p : parameters) {
	    	Method get = null;
	    	Object o = null;
			try {
				try {
					get = model.getClass().getMethod("get" + p);
					o = get.invoke(model);
				} catch (NoSuchMethodException e) {
					try {
						get = model.getClass().getMethod("is" + p);
						o = get.invoke(model);
					} catch (final NoSuchMethodException e1) {
						if (getParameterMethod == null)
							throw new IllegalStateException(e);
						try {
							o = getParameterMethod.invoke(model,Util.capitalize(p));
						} catch (InvocationTargetException e2){
							// this is ignored, it might happen, that we shouldn't have capitalize the parameter name
						}
						if (o == null)
							o = getParameterMethod.invoke(model,Util.uncapitalize(p));
					}
				}
			} catch (IllegalStateException e) {
				throw e;
			} catch (Exception e) {
				Logger.logException(e);
			}
//	    	b.append(toStringWithoutScientificNotation(o,o.getClass())).append(delimiter);
	    	b.append(o.toString()).append(delimiter);
	    }
	    parameterValues = b.toString();
	}
}
