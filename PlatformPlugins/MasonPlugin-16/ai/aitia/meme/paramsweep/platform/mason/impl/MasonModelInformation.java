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

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.CtClass;
import sim.engine.SimState;
import sim.util.Interval;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.paramsweep.batch.IHierarchicalModelInformation;
import ai.aitia.meme.paramsweep.batch.output.NonRecordableFunctionInfo;
import ai.aitia.meme.paramsweep.batch.output.NonRecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.ISubmodelParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.SubmodelInfo;
import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.platform.mason.info.MasonChooserParameterInfo;
import ai.aitia.meme.paramsweep.platform.mason.info.MasonIntervalParameterInfo;
import ai.aitia.meme.paramsweep.platform.mason.recording.annotation.Submodel;
import ai.aitia.meme.paramsweep.utils.Util;

public class MasonModelInformation implements IHierarchicalModelInformation {
	//====================================================================================================
	// members
	
//	public static final String GET_PARAMETER = "getParameter";
//	public static final String SET_PARAMETER = "setParameter";

	private static final String GET_UNUSED_PARAMETERS_METHOD_NAME = "getUnusedParameters";
	private IPSWInformationProvider provider = null;
	private CtClass modelClass = null;
	private Class<?> recordingClass = null;
	
	private List<ParameterInfo<?>> parameters = null;
	private List<RecordableInfo> recordables = null;
	private List<NonRecordableInfo> nonRecordables = null;
	private Object model;
	
	private Map<Class<?>,List<ISubmodelParameterInfo>> submodelCache = new HashMap<Class<?>,List<ISubmodelParameterInfo>>();
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public MasonModelInformation(IPSWInformationProvider provider, CtClass modelClass) {
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
	
	//----------------------------------------------------------------------------------------------------
	public List<ISubmodelParameterInfo> getSubmodelParameters(final SubmodelInfo<?> submodel) throws ModelInformationException {
		if (submodel.getActualType() == null)
			throw new ModelInformationException("No actual type is selected for parameter " + submodel.getName());
		
		List<ISubmodelParameterInfo> resultList = submodelCache.get(submodel.getActualType());
		if (resultList == null) {
			resultList = initializeSubmodelParameters(submodel.getActualType());
			submodelCache.put(submodel.getActualType(),resultList);
		}
		
		return resultList;
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
			throw new ModelInformationException(e);
		} finally {
			modelClass.defrost();
		} 
		
		List<String> reserved = collectReserved();
		
		Field[] fields = Util.getFields(javaClass);
		for (Field field : fields) {
			if (reserved.contains(Util.capitalize(field.getName())) || reserved.contains(Util.uncapitalize(field.getName())) ||
				Modifier.isFinal(field.getModifiers()))	continue;
			if (Util.isAcceptableType(field.getType())) {
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
		model = null;
		Class javaClass = null;
		List<String> unusedParameters = null;
		try {
			javaClass = provider.getClassCache().get(modelClass.getName());
			if (javaClass == null) {
				modelClass.stopPruning(true);
				javaClass = modelClass.toClass(provider.getCustomClassLoader(),null);
				provider.getClassCache().put(modelClass.getName(),javaClass);
			}
			try {
				Constructor ctru = javaClass.getConstructor(long.class);
				model = ctru.newInstance(1L); //calling the constructor with the long 'seed' parameter

				// try find the unused parameters
				try {
					Method getUnusedParametersMethod = javaClass.getMethod(GET_UNUSED_PARAMETERS_METHOD_NAME);
					String[] strings = (String[]) getUnusedParametersMethod.invoke(model);
					if (strings != null){
						unusedParameters = Arrays.asList(strings);
					} else {
						unusedParameters = new ArrayList<String>();
					}
				} catch (NoSuchMethodException e){
					// it is fine not to find this exception
					unusedParameters = new ArrayList<String>();
				}
				//FIXME: why these many exceptions?
			} catch (NoClassDefFoundError e) {
				throw new CannotCompileException(e);
			} catch (NoSuchMethodError e) {
				throw new CannotCompileException(e);
			} catch (SecurityException e) {
				throw new CannotCompileException(e);
			} catch (NoSuchMethodException e) {
				throw new CannotCompileException(e);
			} catch (IllegalArgumentException e) {
				throw new CannotCompileException(e);
			} catch (InvocationTargetException e) {
				throw new CannotCompileException(e.getTargetException());
			}
		} catch (CannotCompileException e) {
			error = "Error while processing the model : " + modelClass.getSimpleName();
			MEMEApp.logException(e);
		} catch (InstantiationException e) {
			error = "Can't instantiate : " + modelClass.getSimpleName();
			MEMEApp.logException(e);
		} catch (IllegalAccessException e) {
			error = "Can't instantiate : " + modelClass.getSimpleName();
			MEMEApp.logException(e);
		} finally {
			modelClass.defrost();
		}
		if (error != null) {
			parameters = null;
			return error;
		}
		
		
		parameters = new ArrayList<ParameterInfo<?>>();
		Method[] allMethods = javaClass.getMethods();
		for (int i = 0; i < allMethods.length; i++) {
			String parameterName = allMethods[i].getName().substring(3);
//			parameterName = String.format("%c%s", Character.toLowerCase(parameterName.charAt(0)), parameterName.substring(1));
			if (	allMethods[i].getName().startsWith("set") && 
					allMethods[i].getParameterTypes().length == 1 && 
					(Util.isAcceptableSimpleType(allMethods[i].getParameterTypes()[0]) || File.class.isAssignableFrom(allMethods[i].getParameterTypes()[0])) &&
					allMethods[i].getReturnType().equals(Void.TYPE) &&
					!unusedParameters.contains(String.format("%c%s", Character.toLowerCase(parameterName.charAt(0)), parameterName.substring(1)))) {
				//this is a setter method, look for a getter:
				Method getter = searchGetter(allMethods, allMethods[i].getName().substring(3), allMethods[i].getParameterTypes()[0]);
				if (getter != null) {
					//we have getter as well, this is a parameter, add it to the list:
					try {
						String description = null;
						// if there is a description method for the parameter, call it to retrieve the description of the parameter
						Method descriptionMethod = searchDescription(allMethods, allMethods[i].getName().substring(3));
						if (descriptionMethod != null){
							description = (String) descriptionMethod.invoke(model);
						}
						
						
						Method domain = searchDomain(allMethods, allMethods[i].getName().substring(3));
						boolean inserted = false;
						if (domain != null) {
							//we have domain restrictions on this parameter
							Object domResult = domain.invoke(model);
//							if (domResult.getClass().getCanonicalName().equals("sim.util.Interval") && !getter.getReturnType().equals(String.class) && !getter.getReturnType().equals(Boolean.class) && !getter.getReturnType().equals(Boolean.TYPE)) {
							if (domResult instanceof sim.util.Interval && !getter.getReturnType().equals(String.class) && !getter.getReturnType().equals(Boolean.class) && !getter.getReturnType().equals(Boolean.TYPE)) {
								parameters.add(createIntervalParameterInfo(model, allMethods[i].getName().substring(3), getter, (Interval) domResult, description));
								inserted = true;
							} else if (domResult instanceof String[] && (getter.getReturnType().equals(Integer.class) || getter.getReturnType().equals(Integer.TYPE))) {
								parameters.add(createChooserParameterInfo(model, allMethods[i].getName().substring(3), getter, (String[]) domResult, description));
								inserted = true;
							}
						}
						if (!inserted) {
							parameters.add(createParameterInfo(model, allMethods[i].getName().substring(3), getter, description));
						}
					} catch (IllegalAccessException e) {
						return "The method " + Util.getLocalizedMessage(e) + " is not visible.";
					} catch (InvocationTargetException e) {
						return e.getClass().getSimpleName() + " occurs during the initialization: " + Util.getLocalizedMessage(e);
					}
				}
			}
		}
		
		// find submodels
		final Field[] fields = getFields(javaClass);
		for (final Field field : fields) {
			final Map<String,Object> submodelInformations = getSubmodelInformations(field);
			if (submodelInformations != null) {
				final String capitalizedFieldName = Util.capitalize(field.getName());
				
				final Method getter = searchGetter(allMethods,capitalizedFieldName,field.getType());
				if (getter != null) {
					final Method setter = searchSetter(allMethods,capitalizedFieldName,field.getType());
					if (setter != null) {
						
						String description = null;
						// if there is a description method for the parameter, call it to retrieve the description of the parameter
						Method descriptionMethod = searchDescription(allMethods,capitalizedFieldName);
						if (descriptionMethod != null){
							try {
								description = (String) descriptionMethod.invoke(model);
								
							} catch (IllegalAccessException e) {
								return "The method " + Util.getLocalizedMessage(e) + " is not visible.";
							} catch (InvocationTargetException e) {
								return e.getClass().getSimpleName() + " occurs during the initialization: " + Util.getLocalizedMessage(e);
							}
						}
						
						parameters.add(createSubmodelParameterInfo(capitalizedFieldName,field.getType(),description,submodelInformations));
					}
				}
			}
		}
		
		//getting the 'seed' as parameter:
		try {
			Method seedGetter = javaClass.getMethod("seed", null);
			parameters.add(createParameterInfo(model, "Seed", seedGetter));
		} catch (SecurityException e) {
			MEMEApp.logException(e);
			return "The method " + Util.getLocalizedMessage(e) + " is not visible.";
		} catch (NoSuchMethodException e) {
			MEMEApp.logException(e);
			return "The method " + Util.getLocalizedMessage(e) + " does not exist.";
		} catch (IllegalAccessException e) {
			MEMEApp.logException(e);
			return "The method " + Util.getLocalizedMessage(e) + " is not visible.";
		} catch (InvocationTargetException e) {
			MEMEApp.logException(e);
			return e.getClass().getSimpleName() + " occurs during the initialization: " + Util.getLocalizedMessage(e);
		}
		Collections.sort(parameters);
		
		return null;
	}
	
	private Map<String,Object> getSubmodelInformations(final Field field) {
		final Submodel annotation = field.getAnnotation(Submodel.class);
		if (annotation == null) return null;
		
		final Map<String,Object> result = new HashMap<String,Object>();
		
		final Class<?>[] types = annotation.value();
		final List<Class<?>> validTypes = new ArrayList<Class<?>>(types.length);
		for (final Class<?> type : types) {
			if (field.getType().isAssignableFrom(type))
				validTypes.add(type);
		}
		result.put("types",validTypes);
		
		//TODO: mining other informations
		
		return result;
	}

	//----------------------------------------------------------------------------------------------------
	private List<ISubmodelParameterInfo> initializeSubmodelParameters(final Class<?> actualType) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private MasonChooserParameterInfo<?> createChooserParameterInfo(Object model, String name, Method getter, String[] domResult, String description) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Object o = null;
		o = getter.invoke(model,(Object[])null);
		Class<?> c = o.getClass();
		ArrayList<Integer> possibleValues = new ArrayList<Integer>(domResult.length);
		for (int i = 0; i < domResult.length; i++) {
			possibleValues.add(new Integer(i));
		}
		if (Integer.TYPE.equals(c) || Integer.class.equals(c))
			return new MasonChooserParameterInfo<Integer>(name,description,(Integer)o, possibleValues, Arrays.asList(domResult));
		throw new IllegalArgumentException("invalid parameter type");
	}

	private MasonIntervalParameterInfo<?> createIntervalParameterInfo(Object model, String name, Method getter, Interval domResult, String description) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		Object o = null;
		o = getter.invoke(model,(Object[])null);
		Class<?> c = o.getClass();
		if (Byte.TYPE.equals(c) || Byte.class.equals(c))
			return new MasonIntervalParameterInfo<Byte>(name,description,(Byte)o, domResult.getMin(), domResult.getMax(), domResult.isDouble());
		else if (Short.TYPE.equals(c) || Short.class.equals(c))
			return new MasonIntervalParameterInfo<Short>(name,description,(Short)o, domResult.getMin(), domResult.getMax(), domResult.isDouble());
		else if (Integer.TYPE.equals(c) || Integer.class.equals(c))
			return new MasonIntervalParameterInfo<Integer>(name,description,(Integer)o, domResult.getMin(), domResult.getMax(), domResult.isDouble());
		else if (Long.TYPE.equals(c) || Long.class.equals(c))
			return new MasonIntervalParameterInfo<Long>(name,description,(Long)o, domResult.getMin(), domResult.getMax(), domResult.isDouble());
		else if (Float.TYPE.equals(c) || Float.class.equals(c))
			return new MasonIntervalParameterInfo<Float>(name,description,(Float)o, domResult.getMin(), domResult.getMax(), domResult.isDouble());
		else if (Double.TYPE.equals(c) || Double.class.equals(c))
			return new MasonIntervalParameterInfo<Double>(name,description,(Double)o, domResult.getMin(), domResult.getMax(), domResult.isDouble());
		throw new IllegalArgumentException("invalid parameter type");
	}

	private Method searchGetter(Method[] methodList, String getterNameEnding, Class getterReturnType) {
		for (int i = 0; i < methodList.length; i++) {
			if (	(
					methodList[i].getName().equals("get"+getterNameEnding) ||
					methodList[i].getName().equals("is"+getterNameEnding)
					) && 
					methodList[i].getParameterTypes().length == 0 && 
					methodList[i].getReturnType().equals(getterReturnType)) {
				return methodList[i];
			}
		}
		return null;
	}
	
	private Method searchSetter(final Method[] methodList, final String setterNameEnding, final Class parameterType) {
		for (final Method method : methodList) {
			if (Void.TYPE.equals(method.getReturnType()) && ("set" + setterNameEnding).equals(method.getName()) &&
				method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(parameterType))
				return method;
		}
		
		return null;
	}

	private Method searchDescription(Method[] methodList, String getterNameEnding) {
		for (int i = 0; i < methodList.length; i++) {
			if (	(
					methodList[i].getName().equals("des"+getterNameEnding)
					) && 
					methodList[i].getParameterTypes().length == 0 && 
					methodList[i].getReturnType().equals(String.class)) {
				return methodList[i];
			}
		}
		return null;
	}

	private Method searchDomain(Method[] methodList, String domainNameEnding) {
		for (int i = 0; i < methodList.length; i++) {
			if (	methodList[i].getName().equals("dom"+domainNameEnding) && 
					methodList[i].getParameterTypes().length == 0 && 
					methodList[i].getReturnType().equals(Object.class)) {
				return methodList[i];
			}
		}
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
		return createParameterInfo(model, name, method, null);
	}
	private ParameterInfo<?> createParameterInfo(Object model, String name, Method method, String description) throws IllegalAccessException, InvocationTargetException {
		Object o = null;
		o = method.invoke(model,(Object[])null);
		Class<?> c;
		
		if (o == null){
			c = method.getReturnType();
		} else {
			c = o.getClass();
		}
		if (Byte.TYPE.equals(c) || Byte.class.equals(c))
			return new ParameterInfo<Byte>(name,description,(Byte)o);
		else if (Short.TYPE.equals(c) || Short.class.equals(c))
			return new ParameterInfo<Short>(name,description,(Short)o);
		else if (Integer.TYPE.equals(c) || Integer.class.equals(c))
			return new ParameterInfo<Integer>(name,description,(Integer)o);
		else if (Long.TYPE.equals(c) || Long.class.equals(c))
			return new ParameterInfo<Long>(name,description,(Long)o);
		else if (Float.TYPE.equals(c) || Float.class.equals(c))
			return new ParameterInfo<Float>(name,description,(Float)o);
		else if (Double.TYPE.equals(c) || Double.class.equals(c))
			return new ParameterInfo<Double>(name,description,(Double)o);
		else if (Boolean.TYPE.equals(c) || Boolean.class.equals(c))
			return new ParameterInfo<Boolean>(name,description,(Boolean)o);
		else if (String.class.equals(c))
			return new ParameterInfo<String>(name,description,(String)o);
		else if (File.class.isAssignableFrom(c)){
			return new ParameterInfo<File>(name, description, (File)o);
		} else if (Enum.class.isAssignableFrom(c)){
			return new ParameterInfo<Enum>(name, description, (Enum)o);
		}
		throw new IllegalArgumentException("invalid parameter (" + name + ") type (" + c + ")");
	}
	
	//----------------------------------------------------------------------------------------------------
	private SubmodelInfo<?> createSubmodelParameterInfo(final String name, final Class<?> type, final String description, 
														final Map<String,Object> submodelInformations) {
		
		@SuppressWarnings("unchecked") final List<Class<?>> types = (List<Class<?>>) submodelInformations.get("types"); 
		return new SubmodelInfo<Object>(name,description,null,types);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<RecorderInfo> getRecorders() throws ModelInformationException {
		try {
			if (recordingClass == null){
				initializeRecordingClass();
			}
			Method method = recordingClass.getMethod("getRecorders", SimState.class);
			return (List<RecorderInfo>)method.invoke(null, model);
		} catch (NoSuchMethodException e) {
			throw new ModelInformationException(e);
		} catch (SecurityException e) {
			throw new ModelInformationException(e);
		} catch (IllegalAccessException e) {
			throw new ModelInformationException(e);
		} catch (IllegalArgumentException e) {
			throw new ModelInformationException(e);
		} catch (InvocationTargetException e) {
			throw new ModelInformationException(e.getTargetException());
		}
	}
	
	private void initializeRecordingClass() throws ModelInformationException {
		recordingClass = provider.getClassCache().get("ai.aitia.meme.paramsweep.platform.mason.recording.RecordingHelper");
//		CtClass recordingCtClass = null;
		try {
			if (recordingClass == null) {
//				recordingCtClass = modelClass.getClassPool().get("eu.crisis_economics.abm.aspects.Recording");
//				recordingCtClass.stopPruning(true);
//				Class<?> recordingClass = recordingCtClass. toClass(provider.getCustomClassLoader(), null);

				recordingClass = provider.getCustomClassLoader().loadClass("ai.aitia.meme.paramsweep.platform.mason.recording.RecordingHelper");
				provider.getClassCache().put("ai.aitia.meme.paramsweep.platform.mason.recording.RecordingHelper", recordingClass);
			}
//		} catch (CannotCompileException e) {
//			throw new ModelInformationException(e);
//		} catch (NotFoundException e) {
//			throw new ModelInformationException(e);
		} catch (ClassNotFoundException e) {
			throw new ModelInformationException(e);
//		} finally {
//			if (recordingCtClass != null){
//				recordingCtClass.defrost();
//			}
		} 

	}

	@Override
	public String getRecordersXML() throws ModelInformationException {
		try {
			if (recordingClass == null){
				initializeRecordingClass();
			}
			Method method = recordingClass.getMethod("getRecorderPageXML", SimState.class);
			return (String)method.invoke(null, model);
		} catch (NoSuchMethodException e) {
			throw new ModelInformationException(e);
		} catch (SecurityException e) {
			throw new ModelInformationException(e);
		} catch (IllegalAccessException e) {
			throw new ModelInformationException(e);
		} catch (IllegalArgumentException e) {
			throw new ModelInformationException(e);
		} catch (InvocationTargetException e) {
			throw new ModelInformationException(e);
		}
	}
	
	//------------------------------------------------------------------------------
	/**
	 * Returns all fields of <code>clazz</code> that can be access from an
	 * derived class.
	 */
	private Field[] getFields(Class<?> clazz) {
		List<Field> result = new ArrayList<Field>();
		getFieldsImpl(clazz,result);
		return result.toArray(new Field[0]);
	}

	//----------------------------------------------------------------------------------------------------
	private void getFieldsImpl(Class<?> clazz, List<Field> result) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field f : fields) {
			if (!f.isSynthetic()) {
				result.add(f);
			}
		}
		Class<?> parent = clazz.getSuperclass();
		if (parent != null)
			getFieldsImpl(parent,result);
	}
}
