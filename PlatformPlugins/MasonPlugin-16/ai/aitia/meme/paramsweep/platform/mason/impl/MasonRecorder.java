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
package ai.aitia.meme.paramsweep.platform.mason.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import ai.aitia.meme.paramsweep.utils.SeparatedList;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.utils.Utils.Pair;

public class MasonRecorder {

	//====================================================================================================
	// members

	private static final String TICK_HEADER_LABEL = "tick";
	protected String fileName = null;
	protected String fileNamePrefix = null;
	protected String fileNameSuffix = null;
	protected String delimiter = null;
	protected IMasonGeneratedModel model = null;
	protected boolean writeHeader = true;
	
	private String parameterValues = null;
	private Map<String, Object> parameterList = new HashMap<String, Object>();
	private Map<String, Object> constantParameters = new HashMap<String, Object>();
	protected Vector<Pair<String,Vector<String>>> data = new Vector<Pair<String,Vector<String>>>();
	protected Vector<Pair<String,Method>> sources = new Vector<Pair<String,Method>>();
	protected Map<String, Integer> collectionLength = new HashMap<String, Integer>();
	protected Map<String, Method> collectionLengthMember = new HashMap<String, Method>();
	protected Method getParameterMethod = null;
	
	protected long runNumber;
	protected String header;
	protected BufferedWriter out;
	protected int part = 1;
	
	protected List<MasonRecorderListener> listeners = new ArrayList<MasonRecorderListener>();
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public MasonRecorder(String fileName, IMasonGeneratedModel model) { this(fileName,model,"|"); }
	
	//----------------------------------------------------------------------------------------------------
	public MasonRecorder(String fileName, IMasonGeneratedModel model, String delimiter) {
		this.fileName = fileName;
		int index = fileName.lastIndexOf('.');
		this.fileNamePrefix = fileName.substring(0, index);
		this.fileNameSuffix = fileName.substring(index);
		this.model = model;
		this.delimiter = delimiter;
		try {
			getParameterMethod = model.getClass().getMethod("getParameter",String.class);
		} catch (Exception e) {
			getParameterMethod = null;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void addRecorderListener(final MasonRecorderListener listener) {
		listeners.add(listener);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void removeRecorderListener(final MasonRecorderListener listener) {
		listeners.remove(listener);
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
	public boolean addMultiColumnSource(final String name, final String method, final String collectionLengthMember){
		try {
			Method collectionLengthMethod = model.getClass().getMethod(collectionLengthMember);
			this.collectionLengthMember.put(name, collectionLengthMethod);
		} catch (NoSuchMethodException e1) {
			return false;
		}
		return addSource(name, method);
	}
	
	public int getCollectionLength(final String collectionName){
		// check if we have any collection length information
		Integer length = collectionLength.get(collectionName);
		if (length == null){
			try {
				Method collectionLengthMethod = collectionLengthMember.get(collectionName);
				collectionLengthMethod.setAccessible(true);
				length = (Integer)collectionLengthMethod.invoke(model);
				collectionLength.put(collectionName, length);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException(e);
			} catch (InvocationTargetException e) {
				throw new IllegalStateException(e);
			}
		}

		return length;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void record() {
		long currentRun = model.aitiaGenerated_getRun();
		if (currentRun > runNumber){
			
			HashMap<String, Integer> oldCollectionLength = new HashMap<String, Integer>(collectionLength);
			collectionLength.clear();
			
			boolean collectionsChanged = false;
			for (String collectionName : oldCollectionLength.keySet()) {
				int oldLength = oldCollectionLength.get(collectionName);
				int newLength = getCollectionLength(collectionName);
				
				if (newLength != oldLength){
					collectionsChanged = true;
				}
			}
			if (collectionsChanged){
				header = getHeader();
			}
			
//			parameterValues = null;
			initializeParameterValues();
			runNumber = currentRun;
		}
		
		Vector<String> v = new Vector<String>();
		Map<String, Object> reportedValues = new HashMap<String, Object>(sources.size());
		reportedValues.put(TICK_HEADER_LABEL, model.getCurrentStep());
	    for (Pair<String,Method> source : sources) {
	    	Method m = source.getSecond();
			Object o = null;
			try {
				o = m.invoke(model);
			} catch (Exception e) {}

			v.add(o.toString());
			
			if (listeners.size() > 0){
				if (o instanceof SeparatedList){
					SeparatedList list = (SeparatedList)o;
					String name = source.getFirst().replace(Util.GENERATED_MODEL_MULTICOLUMN_POSTFIX, "") + "Multi_";
					for (int i = 0 ; i < list.size() ; i++){
						reportedValues.put(name + i, list.get(i));
					}
				} else {
					reportedValues.put(source.getFirst(), o);
				}
			}
	    }
	    data.add(new Pair<String,Vector<String>>(String.valueOf(model.getCurrentStep()),v));
	    
	    for (MasonRecorderListener listener : listeners) {
			listener.recordingPerformed(parameterList, reportedValues);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void writeToFile() {
		if (parameterValues == null)
			initializeParameterValues();
	    try {
	      if (model.aitiaGenerated_getRun() == 1 && writeHeader) { // if this is the first line to write
	        renameFile();
	        out = new BufferedWriter(new FileWriter(fileName,true));
	        out.write(getModelHeader());
	        out.newLine();
	        out.newLine();
	        out.write(getHeader());
	        out.newLine();
	        writeHeader = false;
	      }

	      if (out == null){ // somehow this get's nulled...
		        out = new BufferedWriter(new FileWriter(fileName,true));
	      }
	      
	      if (header != null){ // if some of the multicolumn lengths have changed between runs
	    	  try {
	    		  out.close();
	    	  } catch (IOException e){
	    		  e.printStackTrace();
	    	  }
	    	  fileName = fileNamePrefix + "-part" + part++ + fileNameSuffix;
	    	  renameFile();
	    	  out = new BufferedWriter(new FileWriter(fileName, true));
//	    	  out.write(getModelHeader());
//	    	  out.newLine();
//	    	  out.newLine();
	    	  out.write(header);
	    	  out.newLine();
	    	  header = null;
	      }
	      
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
	    	ex.printStackTrace();
	    } finally {
	    	try {
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
	    	data.clear();
	    }
	}
	
	//----------------------------------------------------------------------------------------------------
	public void writeEnd() {
		try {
			Date date = new Date();
			String dateTime = DateFormat.getDateTimeInstance().format(date);
			out.newLine();
			out.write("End Time: " + dateTime);
	    } catch (IOException e) {
	    	e.printStackTrace();
	    } finally {
	      try {
	        out.flush();
	        out.close();
	        out = null;
	      } catch (Exception e1) {
	    	  e1.printStackTrace();
	      }
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
//					for (int i = 0; i <= exp - fragment;++i) 
//						result.append("0");
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
	    
	    constantParameters.clear();
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
						o = getParameterMethod.invoke(model,Util.capitalize(p));
						if (o == null)
							o = getParameterMethod.invoke(model,Util.uncapitalize(p));
					}
				}
			} catch (IllegalStateException e) {
				throw e;
			} catch (Exception e) {}
//	    	b.append(p).append(": ").append(toStringWithoutScientificNotation(o,o.getClass())).append("\n");
	    	b.append(p).append(": ").append(o.toString()).append("\n");
	    	
	    	constantParameters.put(p, o);
	    }
	    return b.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private String getHeader() {
		StringBuffer b = new StringBuffer();
		b.append("\"run\"").append(delimiter).append("\"" + TICK_HEADER_LABEL + "\"").append(delimiter);
		List<String> parameters = model.aitiaGenerated_getMutableParameterNames();
		for (String p : parameters)
			b.append("\"").append(p).append("\"").append(delimiter);
		
		for (Pair<String,Method> p : sources){
			String name = p.getFirst();

			Integer length = collectionLength.get(name);
			// if we have collection length information, this is a multicolumn recordable
			if (length != null){
				name = name.replace(Util.GENERATED_MODEL_MULTICOLUMN_POSTFIX, "");
				StringBuffer alias = new StringBuffer();
				for (int i = 0 ; i < length ; i++){
					alias.append("\"" + name + "Multi_" + i + "\"|");
				}
				if (alias.length() > 1){
					alias.delete(alias.length() - 2, alias.length());
					alias.deleteCharAt(0);
				}
				name = alias.toString();
//				p.set(name, p.getValue());
//				collectionLength.put(name, length);
			}

			b.append("\"").append(name).append("\"").append(delimiter);
		}
		String result = b.toString();
		return result.substring(0,result.length() - delimiter.length());
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private void initializeParameterValues() {
		StringBuffer b = new StringBuffer();
		
		// to be able to report values to MasonRecorderListener instances, we have to populate the constantParameters map!
		if (constantParameters.isEmpty()){
			getModelHeader();
		}
		
		parameterList = new HashMap<String, Object>(constantParameters);
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
						o = getParameterMethod.invoke(model,Util.capitalize(p));
						if (o == null)
							o = getParameterMethod.invoke(model,Util.uncapitalize(p));
					}
				}
//			} catch (IllegalStateException e) {
//				throw e;
			} catch (Exception e) {
				if (e instanceof IllegalStateException){
					throw (IllegalStateException)e;
				}
				throw new IllegalStateException(e);
			}
//	    	b.append(toStringWithoutScientificNotation(o,o.getClass())).append(delimiter);
	    	b.append(o.toString()).append(delimiter);
	    	parameterList.put(p, o);
	    }
	    parameterValues = b.toString();
	    parameterList = Collections.unmodifiableMap(parameterList);
	}
}
