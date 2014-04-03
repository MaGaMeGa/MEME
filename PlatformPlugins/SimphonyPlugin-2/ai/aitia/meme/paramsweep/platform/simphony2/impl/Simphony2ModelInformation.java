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
package ai.aitia.meme.paramsweep.platform.simphony2.impl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javassist.CannotCompileException;
import javassist.CtClass;
import ai.aitia.meme.paramsweep.batch.IModelInformation;
import ai.aitia.meme.paramsweep.batch.output.NonRecordableFunctionInfo;
import ai.aitia.meme.paramsweep.batch.output.NonRecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;
import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.utils.Util;

public class Simphony2ModelInformation implements IModelInformation {

	//====================================================================================================
	// members
	
	public static final String GET_PARAMETER = "getParameter";
	public static final String SET_PARAMETER = "setParameter";

	private IPSWInformationProvider provider = null;
	private CtClass modelClass = null;
	
	private List<ParameterInfo<?>> parameters = null;
	private List<RecordableInfo> recordables = null;
	private List<NonRecordableInfo> nonRecordables = null;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public Simphony2ModelInformation(IPSWInformationProvider provider, CtClass modelClass) {
		this.provider = provider;
		this.modelClass = modelClass;
	}
	
	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return modelClass.getName(); } 

	//----------------------------------------------------------------------------------------------------
	public List<ParameterInfo<?>> getParameters() throws ModelInformationException { 
		if (parameters == null) {
			String error = collectParameters();
			if (error != null)
				throw new ModelInformationException(error);
		}
		return parameters;
	}

	//----------------------------------------------------------------------------------------------------
	public List<RecordableInfo> getRecordables() throws ModelInformationException {
		if (recordables == null)
			initializeRecordableList();
		return recordables;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<NonRecordableInfo> getNonRecordables() throws ModelInformationException {
		if (nonRecordables == null) 
			initializeNonRecordableList();
		return nonRecordables;
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void initializeRecordableList() throws ModelInformationException {
		recordables = new ArrayList<RecordableInfo>();
		Class javaClass = provider.getClassCache().get(modelClass.getName());
		try {
			if (javaClass == null) {
				modelClass.stopPruning(true);
				javaClass = modelClass.toClass(provider.getCustomClassLoader(),null);
				provider.getClassCache().put(modelClass.getName(),javaClass);
			}
		} catch (CannotCompileException e) {
		} finally {
			modelClass.defrost();
		} 
		
		List<String> reserved = collectReserved();
		
		Field[] fields = Util.getFields(javaClass);
		for (Field field : fields) {
			if (reserved.contains(Util.capitalize(field.getName())) || reserved.contains(Util.uncapitalize(field.getName())) ||
				Modifier.isFinal(field.getModifiers()))	continue;
			if (	Util.isAcceptableType(field.getType())) {
				recordables.add(new RecordableInfo(field.getName(),field.getType(),null,field.getName()));
			}
		}

		Method[] methods = Util.getMethods(javaClass);
		for (Method method : methods) {
			if (method.getParameterTypes().length == 0 && !method.getReturnType().equals(Void.TYPE)) {
				String name = method.getName() + "()";
				if ((method.getName().length() > 3 && reserved.contains(method.getName().substring(3))) ||
					(method.getName().length() > 3 && reserved.contains(Util.uncapitalize(method.getName().substring(3)))) ||
					(method.getName().length() > 2 && reserved.contains(method.getName().substring(2))) ||
					(method.getName().length() > 2 && reserved.contains(Util.uncapitalize(method.getName().substring(2))))) {
					if (method.getName().startsWith("get") || method.getName().startsWith("is")) continue;
				}
				if (Util.isAcceptableType(method.getReturnType()) && !"getPropertiesValues".equals(method.getName()) &&
					!"hashCode".equals(method.getName()) && !"toString".equals(method.getName())) 
					recordables.add(new RecordableInfo(method.getName(),method.getReturnType(),null,name));
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private List<String> collectReserved() throws ModelInformationException {
		List<String> result = new ArrayList<String>(getParameters().size());
		for (ParameterInfo<?> info : getParameters())
			result.add(info.getName());
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializeNonRecordableList() {
		nonRecordables = new ArrayList<NonRecordableInfo>();
		Class javaClass = provider.getClassCache().get(modelClass.getName());
		try {
			if (javaClass == null) {
				modelClass.stopPruning(true);
				javaClass = modelClass.toClass(provider.getCustomClassLoader(),null);
				provider.getClassCache().put(modelClass.getName(),javaClass);
			}
		} catch (CannotCompileException e) {
		} finally {
			modelClass.defrost();
		} 
		
		Field[] fields = Util.getFields(javaClass);
		for (Field field : fields) {
			if (!Util.isAcceptableType(field.getType())) {
				NonRecordableInfo info = new NonRecordableInfo(field.getName(),field.getType(),null,field.getName());
				info.setInnerType(Util.innerType(field.getType()));
				nonRecordables.add(info);
			}
		}
		
		Method[] methods = Util.getMethods(javaClass);
		for (Method method : methods) {
			if (method.getParameterTypes().length == 0 && !method.getReturnType().equals(Void.TYPE) &&
				Util.isAcceptableType(method.getReturnType())) continue;
			NonRecordableFunctionInfo info = new NonRecordableFunctionInfo(method.getName(),method.getReturnType(),null,method.getName() + "()",
																		   Arrays.asList(method.getParameterTypes()));
			info.setInnerType(Util.innerType(method.getReturnType()));
			nonRecordables.add(info);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private String collectParameters() {
		String error = null;
		Object model = null;
		Class javaClass = null;
		try {
			javaClass = provider.getClassCache().get(modelClass.getName());
			if (javaClass == null) {
				modelClass.stopPruning(true);
				javaClass = modelClass.toClass(provider.getCustomClassLoader(),null);
				provider.getClassCache().put(modelClass.getName(),javaClass);
			}
			try {
				model = javaClass.newInstance();
			} catch (NoClassDefFoundError e) {
				throw new CannotCompileException(e);
			} catch (NoSuchMethodError e) {
				throw new CannotCompileException(e);
			}
		} catch (CannotCompileException e) {
			error = "Error while processing the model : " + modelClass.getSimpleName();
		} catch (InstantiationException e) {
			error = "Can't instantiate : " + modelClass.getSimpleName();
		} catch (IllegalAccessException e) {
			error = "Can't instantiate : " + modelClass.getSimpleName();
		} finally {
			modelClass.defrost();
		}
		if (error != null) {
			parameters = null;
			return error;
		}
		String[] getParamsResult = null;
		try {
			getParamsResult = (String[])javaClass.getMethod("getParams",(Class[])null).invoke(model,(Object[])null);
		} catch (Exception e) {
			// we checked the existense of these methods in the Simphony2JavaPlatform.checkModel() method
			throw new IllegalStateException();
		}
		String[] paramsMod = capitalizeParams(getParamsResult);
		parameters = new ArrayList<ParameterInfo<?>>();
		for (String parameterName : paramsMod) {
			if (parameterName.equals("")) continue;
			try {
				Method m = null;
				try {
					m = javaClass.getMethod("get" + parameterName,(Class[])null);
				} catch (NoSuchMethodException e) {
					try {
						m = javaClass.getMethod("is" + parameterName,(Class[])null);
					} catch (NoSuchMethodException ee) {
						m = javaClass.getMethod(GET_PARAMETER,String.class);
					}
				}
				try {
					javaClass.getMethod("set" + parameterName,new Class[] { m.getReturnType() });
				} catch (NoSuchMethodException e) {
					javaClass.getMethod(SET_PARAMETER,String.class,Object.class);
				}
				parameters.add(createParameterInfo(model,parameterName,m));
			} catch (NoSuchMethodException e) {
				return "Cannot find this method: " + Util.getLocalizedMessage(e);
			} catch (IllegalAccessException e) {
				return "The method " + Util.getLocalizedMessage(e) + " is not visible.";
			} catch (Exception e) {
				return e.getClass().getSimpleName() + " occurs during the initialization: " + Util.getLocalizedMessage(e);
			}
		}
		Collections.sort(parameters);
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Capitalizes the parameter names and returns in a new array.
	 * @param params the parameter names
	 */
	private String[] capitalizeParams(String[] params) {
		if (params == null || params.length == 0) 
			return new String[0];
		List<String> result = new ArrayList<String>();
		for (int i = 0;i < params.length;++i) {
			if (params[i] != null && !params[i].equals("")) {
				char start = params[i].charAt(0);
				String p = null;
				if (Character.isLowerCase(start)) 
					p = String.valueOf(Character.toUpperCase(start)) + params[i].substring(1);
				else
					p = params[i];
				if (!result.contains(p))
					result.add(p);
			}
		}
		return result.toArray(new String[result.size()]);
	}
	
	//----------------------------------------------------------------------------------------------------
	private ParameterInfo<?> createParameterInfo(Object model, String name, Method method) throws IllegalAccessException, InvocationTargetException {
		Object o = null;
		if (method.getParameterTypes().length > 0)  { // getParameter
			try {
				o = method.invoke(model,Util.capitalize(name));
			} catch (InvocationTargetException e){
				// we should probably try uncapitalized
			}
			if (o == null)
				o = method.invoke(model,Util.uncapitalize(name));
		} else // get<Name>
			o = method.invoke(model,(Object[])null);
		Class<?> c = o.getClass();
		if (Byte.TYPE.equals(c) || Byte.class.equals(c))
			return new ParameterInfo<Byte>(name,null,(Byte)o);
		else if (Short.TYPE.equals(c) || Short.class.equals(c))
			return new ParameterInfo<Short>(name,null,(Short)o);
		else if (Integer.TYPE.equals(c) || Integer.class.equals(c))
			return new ParameterInfo<Integer>(name,null,(Integer)o);
		else if (Long.TYPE.equals(c) || Long.class.equals(c))
			return new ParameterInfo<Long>(name,null,(Long)o);
		else if (Float.TYPE.equals(c) || Float.class.equals(c))
			return new ParameterInfo<Float>(name,null,(Float)o);
		else if (Double.TYPE.equals(c) || Double.class.equals(c))
			return new ParameterInfo<Double>(name,null,(Double)o);
		else if (Boolean.TYPE.equals(c) || Boolean.class.equals(c))
			return new ParameterInfo<Boolean>(name,null,(Boolean)o);
		else if (String.class.equals(c))
			return new ParameterInfo<String>(name,null,(String)o);
		throw new IllegalArgumentException("invalid parameter type");
	}

	@Override
	public List<RecorderInfo> getRecorders() throws ModelInformationException {
		return new ArrayList<RecorderInfo>();
	}

	@Override
	public String getRecordersXML() throws ModelInformationException {
		// TODO Auto-generated method stub
		return null;
	}
}
