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
import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.SubmodelInfo;
import ai.aitia.meme.paramsweep.batch.param.SubmodelParameterInfo;
import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.platform.mason.info.MasonChooserParameterInfo;
import ai.aitia.meme.paramsweep.platform.mason.info.MasonChooserSubmodelParameterInfo;
import ai.aitia.meme.paramsweep.platform.mason.info.MasonIntervalParameterInfo;
import ai.aitia.meme.paramsweep.platform.mason.info.MasonIntervalSubmodelParameterInfo;
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
	
	private final Map<Class<?>,List<ParameterInfo<?>>> submodelCache = new HashMap<Class<?>,List<ParameterInfo<?>>>();
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public MasonModelInformation(final IPSWInformationProvider provider, final CtClass modelClass) {
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
			final String error = collectParameters();
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
	public List<ParameterInfo<?>> getSubmodelParameters(final SubmodelInfo<?> submodel) throws ModelInformationException {
		if (submodel.getActualType() == null)
			throw new ModelInformationException("No actual type is selected for parameter " + submodel.getName());
		
		List<ParameterInfo<?>> resultList = submodelCache.get(submodel.getActualType());
		if (resultList == null) {
			resultList = initializeSubmodelParameters(submodel);
			submodelCache.put(submodel.getActualType(),resultList);
		}
		
		return resultList;
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void initializeRecordableList() throws ModelInformationException {
		recordables = new ArrayList<RecordableInfo>();
		Class<?> javaClass = provider.getClassCache().get(modelClass.getName());
		try {
			if (javaClass == null) {
				modelClass.stopPruning(true);
				javaClass = modelClass.toClass(provider.getCustomClassLoader(),null);
				provider.getClassCache().put(modelClass.getName(),javaClass);
			}
		} catch (final CannotCompileException e) {
			throw new ModelInformationException(e);
		} finally {
			modelClass.defrost();
		} 
		
		final List<String> reserved = collectReserved();
		
		final Field[] fields = Util.getFields(javaClass);
		for (final Field field : fields) {
			if (reserved.contains(Util.capitalize(field.getName())) || reserved.contains(Util.uncapitalize(field.getName())) ||
				Modifier.isFinal(field.getModifiers()))	continue;
			if (Util.isAcceptableType(field.getType())) {
				recordables.add(new RecordableInfo(field.getName(),field.getType(),null,field.getName()));
			}
		}

		final Method[] methods = Util.getMethods(javaClass);
		for (final Method method : methods) {
			if (method.getParameterTypes().length == 0 && !method.getReturnType().equals(Void.TYPE)) {
				final String name = method.getName() + "()";
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
		final List<String> result = new ArrayList<String>(getParameters().size());
		for (final ParameterInfo<?> info : getParameters())
			result.add(info.getName());
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializeNonRecordableList() {
		nonRecordables = new ArrayList<NonRecordableInfo>();
		Class<?> javaClass = provider.getClassCache().get(modelClass.getName());
		try {
			if (javaClass == null) {
				modelClass.stopPruning(true);
				javaClass = modelClass.toClass(provider.getCustomClassLoader(),null);
				provider.getClassCache().put(modelClass.getName(),javaClass);
			}
		} catch (final CannotCompileException e) {
		} finally {
			modelClass.defrost();
		} 
		
		final Field[] fields = Util.getFields(javaClass);
		for (final Field field : fields) {
			if (!Util.isAcceptableType(field.getType())) {
				final NonRecordableInfo info = new NonRecordableInfo(field.getName(),field.getType(),null,field.getName());
				info.setInnerType(Util.innerType(field.getType()));
				nonRecordables.add(info);
			}
		}
		
		final Method[] methods = Util.getMethods(javaClass);
		for (final Method method : methods) {
			if (method.getParameterTypes().length == 0 && !method.getReturnType().equals(Void.TYPE) &&
					Util.isAcceptableType(method.getReturnType())) continue;
			final NonRecordableFunctionInfo info = new NonRecordableFunctionInfo(method.getName(),method.getReturnType(),null,method.getName() + "()",
																		   Arrays.asList(method.getParameterTypes()));
			info.setInnerType(Util.innerType(method.getReturnType()));
			nonRecordables.add(info);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private String collectParameters() {
		String error = null;
		model = null;
		Class<?> javaClass = null;
		List<String> unusedParameters = null;
		try {
			javaClass = provider.getClassCache().get(modelClass.getName());
			if (javaClass == null) {
				modelClass.stopPruning(true);
				javaClass = modelClass.toClass(provider.getCustomClassLoader(),null);
				provider.getClassCache().put(modelClass.getName(),javaClass);
			}
			try {
				@SuppressWarnings("rawtypes")
				final Constructor ctru = javaClass.getConstructor(Long.TYPE);
				model = ctru.newInstance(1L); //calling the constructor with the long 'seed' parameter

				// try find the unused parameters
				try {
					final Method getUnusedParametersMethod = javaClass.getMethod(GET_UNUSED_PARAMETERS_METHOD_NAME);
					final String[] strings = (String[]) getUnusedParametersMethod.invoke(model);
					if (strings != null){
						unusedParameters = Arrays.asList(strings);
					} else {
						unusedParameters = new ArrayList<String>();
					}
				} catch (final NoSuchMethodException e){
					// it is fine not to find this exception
					unusedParameters = new ArrayList<String>();
				}
				//FIXME: why these many exceptions?
			} catch (final NoClassDefFoundError e) {
				throw new CannotCompileException(e);
			} catch (final NoSuchMethodError e) {
				throw new CannotCompileException(e);
			} catch (final SecurityException e) {
				throw new CannotCompileException(e);
			} catch (final NoSuchMethodException e) {
				throw new CannotCompileException(e);
			} catch (final IllegalArgumentException e) {
				throw new CannotCompileException(e);
			} catch (final InvocationTargetException e) {
				throw new CannotCompileException(e.getTargetException());
			}
		} catch (final CannotCompileException e) {
			error = "Error while processing the model : " + modelClass.getSimpleName();
			MEMEApp.logException(e);
		} catch (final InstantiationException e) {
			error = "Can't instantiate : " + modelClass.getSimpleName();
			MEMEApp.logException(e);
		} catch (final IllegalAccessException e) {
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
		error = collectParameters(javaClass,parameters,unusedParameters,null);
	
		if (error != null) {
			parameters = null;
			return error;
		}
		
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String collectParameters (final Class<?> javaClass, final List<ParameterInfo<?>> parameters, final List<String> unusedParameters, 
									  final SubmodelInfo<?> parent) {
		final Method[] allMethods = javaClass.getMethods();
		for (int i = 0; i < allMethods.length; i++) {
			final String parameterName = allMethods[i].getName().substring(3);
//			parameterName = String.format("%c%s", Character.toLowerCase(parameterName.charAt(0)), parameterName.substring(1));
			if (	allMethods[i].getName().startsWith("set") && 
					allMethods[i].getParameterTypes().length == 1 && 
					(Util.isAcceptableSimpleType(allMethods[i].getParameterTypes()[0]) || File.class.isAssignableFrom(allMethods[i].getParameterTypes()[0])) &&
					allMethods[i].getReturnType().equals(Void.TYPE) &&
					!unusedParameters.contains(String.format("%c%s", Character.toLowerCase(parameterName.charAt(0)), parameterName.substring(1)))) {
				//this is a setter method, look for a getter:
				final Method getter = searchGetter(allMethods, allMethods[i].getName().substring(3), allMethods[i].getParameterTypes()[0]);
				if (getter != null) {
					//we have getter as well, this is a parameter, add it to the list:
					try {
						String description = null;
						// if there is a description method for the parameter, call it to retrieve the description of the parameter
						final Method descriptionMethod = searchDescription(allMethods, allMethods[i].getName().substring(3));
						if (descriptionMethod != null){
							description = (String) descriptionMethod.invoke(model);
						}
						
						
						final Method domain = searchDomain(allMethods, allMethods[i].getName().substring(3));
						boolean inserted = false;
						if (domain != null) {
							//we have domain restrictions on this parameter
							final Object domResult = domain.invoke(model);
//							if (domResult.getClass().getCanonicalName().equals("sim.util.Interval") && !getter.getReturnType().equals(String.class) && !getter.getReturnType().equals(Boolean.class) && !getter.getReturnType().equals(Boolean.TYPE)) {
							if (domResult instanceof sim.util.Interval && !getter.getReturnType().equals(String.class) && !getter.getReturnType().equals(Boolean.class) && !getter.getReturnType().equals(Boolean.TYPE)) {
								parameters.add(createIntervalParameterInfo(model, allMethods[i].getName().substring(3), getter, (Interval) domResult, description, parent));
								inserted = true;
							} else if (domResult instanceof String[] && (getter.getReturnType().equals(Integer.class) || getter.getReturnType().equals(Integer.TYPE))) {
								parameters.add(createChooserParameterInfo(model, allMethods[i].getName().substring(3), getter, (String[]) domResult, description, parent));
								inserted = true;
							}
						}
						if (!inserted) {
							parameters.add(createParameterInfo(model, allMethods[i].getName().substring(3), getter, description, parent));
						}
					} catch (final IllegalAccessException e) {
						return "The method " + Util.getLocalizedMessage(e) + " is not visible.";
					} catch (final InvocationTargetException e) {
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
						final Method descriptionMethod = searchDescription(allMethods,capitalizedFieldName);
						if (descriptionMethod != null){
							try {
								description = (String) descriptionMethod.invoke(model);
								
							} catch (final IllegalAccessException e) {
								return "The method " + Util.getLocalizedMessage(e) + " is not visible.";
							} catch (final InvocationTargetException e) {
								return e.getClass().getSimpleName() + " occurs during the initialization: " + Util.getLocalizedMessage(e);
							}
						}
						
						parameters.add(createSubmodelParameterInfo(capitalizedFieldName,field.getType(),description,submodelInformations,parent));
					}
				}
			}
		}
		
		if (parent == null) {
			//getting the 'seed' as parameter:
			try {
				final Method seedGetter = javaClass.getMethod("seed");
				if (seedGetter != null)
					parameters.add(createParameterInfo(model, "Seed", seedGetter, parent));
			} catch (final SecurityException e) {
				MEMEApp.logException(e);
				return "The method " + Util.getLocalizedMessage(e) + " is not visible.";
			} catch (final NoSuchMethodException e) {
				MEMEApp.logException(e);
				return "The method " + Util.getLocalizedMessage(e) + " does not exist.";
			} catch (final IllegalAccessException e) {
				MEMEApp.logException(e);
				return "The method " + Util.getLocalizedMessage(e) + " is not visible.";
			} catch (final InvocationTargetException e) {
				MEMEApp.logException(e);
				return e.getClass().getSimpleName() + " occurs during the initialization: " + Util.getLocalizedMessage(e);
			}
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
	private List<ParameterInfo<?>> initializeSubmodelParameters(final SubmodelInfo<?> submodel) throws ModelInformationException {
		String error = null;
		List<String> unusedParameters = null;
		try {
			final Object submodelObj = submodel.getActualType().newInstance(); 

			// try find the unused parameters
			try {
				final Method getUnusedParametersMethod = submodel.getActualType().getMethod(GET_UNUSED_PARAMETERS_METHOD_NAME);
				final String[] strings = (String[]) getUnusedParametersMethod.invoke(submodelObj);
				if (strings != null){
					unusedParameters = Arrays.asList(strings);
				} else {
					unusedParameters = new ArrayList<String>();
				}
			} catch (final NoSuchMethodException e){
				// it is fine not to find this exception
				unusedParameters = new ArrayList<String>();
			}
		} catch (final InstantiationException e1) {
			error = "Can't instantiate : " + submodel.getActualType().getSimpleName();
			MEMEApp.logException(e1);
		} catch (final IllegalAccessException e1) {
			error = "Can't instantiate : " + submodel.getActualType().getSimpleName();
			MEMEApp.logException(e1);
		} catch (final InvocationTargetException e1) {
			error = "Error while processing the model : " + submodel.getActualType().getSimpleName();
			MEMEApp.logException(e1.getTargetException());
		}
		
		if (error != null)
			throw new ModelInformationException(error);
		
		final List<ParameterInfo<?>> result = new ArrayList<ParameterInfo<?>>();
		error = collectParameters(submodel.getActualType(),result,unusedParameters,submodel);
		
		if (error != null)
			throw new ModelInformationException(error);
		
		return result;
	}
	
	private MasonChooserParameterInfo<?> createChooserParameterInfo(final Object model, final String name, final Method getter, final String[] domResult,
																	final String description, final SubmodelInfo<?> parent) throws IllegalArgumentException,
																															   IllegalAccessException,
																															   InvocationTargetException {
		Object o = null;
		o = getter.invoke(model,(Object[])null);
		final Class<?> c = o.getClass();
		final ArrayList<Integer> possibleValues = new ArrayList<Integer>(domResult.length);
		for (int i = 0; i < domResult.length; i++) {
			possibleValues.add(new Integer(i));
		}
		if (Integer.TYPE.equals(c) || Integer.class.equals(c))
			return parent == null ? new MasonChooserParameterInfo<Integer>(name,description,(Integer)o,possibleValues,Arrays.asList(domResult))
								  : new MasonChooserSubmodelParameterInfo<Integer>(name,description,(Integer)o,possibleValues,Arrays.asList(domResult),parent);
		throw new IllegalArgumentException("invalid parameter type");
	}

	private MasonIntervalParameterInfo<?> createIntervalParameterInfo(final Object model, final String name, final Method getter, final Interval domResult,
																	  final String description, final SubmodelInfo<?> parent) throws IllegalArgumentException,
																	  																 IllegalAccessException,
																	  																 InvocationTargetException {
		Object o = null;
		o = getter.invoke(model,(Object[])null);
		final Class<?> c = o.getClass();
		if (Byte.TYPE.equals(c) || Byte.class.equals(c))
			return parent == null ? new MasonIntervalParameterInfo<Byte>(name,description,(Byte)o, domResult.getMin(), domResult.getMax(), domResult.isDouble())
								  : new MasonIntervalSubmodelParameterInfo<Byte>(name,description,(Byte)o, domResult.getMin(), domResult.getMax(),
										  										 domResult.isDouble(),parent);
		else if (Short.TYPE.equals(c) || Short.class.equals(c))
			return parent == null ? new MasonIntervalParameterInfo<Short>(name,description,(Short)o, domResult.getMin(), domResult.getMax(), domResult.isDouble())
								  : new MasonIntervalSubmodelParameterInfo<Short>(name,description,(Short)o, domResult.getMin(), domResult.getMax(),
										  										  domResult.isDouble(),parent);
		else if (Integer.TYPE.equals(c) || Integer.class.equals(c))
			return parent == null ? new MasonIntervalParameterInfo<Integer>(name,description,(Integer)o, domResult.getMin(), domResult.getMax(), domResult.isDouble())
								  : new MasonIntervalSubmodelParameterInfo<Integer>(name,description,(Integer)o, domResult.getMin(), domResult.getMax(),
										  											domResult.isDouble(),parent);
		else if (Long.TYPE.equals(c) || Long.class.equals(c))
			return parent == null ? new MasonIntervalParameterInfo<Long>(name,description,(Long)o, domResult.getMin(), domResult.getMax(), domResult.isDouble())
								  : new MasonIntervalSubmodelParameterInfo<Long>(name,description,(Long)o, domResult.getMin(), domResult.getMax(),
										  										 domResult.isDouble(),parent);
		else if (Float.TYPE.equals(c) || Float.class.equals(c))
			return parent == null ? new MasonIntervalParameterInfo<Float>(name,description,(Float)o, domResult.getMin(), domResult.getMax(), domResult.isDouble())
								  : new MasonIntervalSubmodelParameterInfo<Float>(name,description,(Float)o, domResult.getMin(), domResult.getMax(),
										  										  domResult.isDouble(),parent);
		else if (Double.TYPE.equals(c) || Double.class.equals(c))
			return parent == null ? new MasonIntervalParameterInfo<Double>(name,description,(Double)o, domResult.getMin(), domResult.getMax(), domResult.isDouble())
								  : new MasonIntervalSubmodelParameterInfo<Double>(name,description,(Double)o, domResult.getMin(), domResult.getMax(),
										  										   domResult.isDouble(),parent);
		throw new IllegalArgumentException("invalid parameter type");
	}

	private Method searchGetter(final Method[] methodList, final String getterNameEnding, final Class<?> getterReturnType) {
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
	
	private Method searchSetter(final Method[] methodList, final String setterNameEnding, final Class<?> parameterType) {
		for (final Method method : methodList) {
			if (Void.TYPE.equals(method.getReturnType()) && ("set" + setterNameEnding).equals(method.getName()) &&
				method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(parameterType))
				return method;
		}
		
		return null;
	}

	private Method searchDescription(final Method[] methodList, final String getterNameEnding) {
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

	private Method searchDomain(final Method[] methodList, final String domainNameEnding) {
		for (int i = 0; i < methodList.length; i++) {
			if (methodList[i].getName().equals("dom"+domainNameEnding) && 
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
//	private String[] capitalizeParams(final String[] params) {
//		if (params == null || params.length == 0) 
//			return new String[0];
//		final List<String> result = new ArrayList<String>();
//		for (int i = 0;i < params.length;++i) {
//			if (params[i] != null && !params[i].equals("")) {
//				final char start = params[i].charAt(0);
//				String p = null;
//				if (Character.isLowerCase(start)) 
//					p = String.valueOf(Character.toUpperCase(start)) + params[i].substring(1);
//				else
//					p = params[i];
//				if (!result.contains(p))
//					result.add(p);
//			}
//		}
//		return result.toArray(new String[result.size()]);
//	}
	
	//----------------------------------------------------------------------------------------------------
	private ParameterInfo<?> createParameterInfo(final Object model, final String name, final Method method, final SubmodelInfo<?> parent) 
																											throws IllegalAccessException, InvocationTargetException {
		return createParameterInfo(model, name, method, null, parent);
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	private ParameterInfo<?> createParameterInfo(final Object model, final String name, final Method method, final String description,
												 final SubmodelInfo<?> parent) throws IllegalAccessException, InvocationTargetException {
		Object o = null;
		o = method.invoke(model,(Object[])null);
		Class<?> c;
		
		if (o == null){
			c = method.getReturnType();
		} else {
			c = o.getClass();
		}
		if (Byte.TYPE.equals(c) || Byte.class.equals(c))
			return parent == null ? new ParameterInfo<Byte>(name,description,(Byte)o) : new SubmodelParameterInfo<Byte>(name,description,(Byte)o,parent);
		else if (Short.TYPE.equals(c) || Short.class.equals(c))
			return parent == null ? new ParameterInfo<Short>(name,description,(Short)o) : new SubmodelParameterInfo<Short>(name,description,(Short)o,parent);
		else if (Integer.TYPE.equals(c) || Integer.class.equals(c))
			return parent == null ? new ParameterInfo<Integer>(name,description,(Integer)o) : new SubmodelParameterInfo<Integer>(name,description,(Integer)o,parent);
		else if (Long.TYPE.equals(c) || Long.class.equals(c))
			return parent == null ? new ParameterInfo<Long>(name,description,(Long)o) : new SubmodelParameterInfo<Long>(name,description,(Long)o,parent);
		else if (Float.TYPE.equals(c) || Float.class.equals(c))
			return parent == null ? new ParameterInfo<Float>(name,description,(Float)o) : new SubmodelParameterInfo<Float>(name,description,(Float)o,parent);
		else if (Double.TYPE.equals(c) || Double.class.equals(c))
			return parent == null ? new ParameterInfo<Double>(name,description,(Double)o) : new SubmodelParameterInfo<Double>(name,description,(Double)o,parent);
		else if (Boolean.TYPE.equals(c) || Boolean.class.equals(c))
			return parent == null ? new ParameterInfo<Boolean>(name,description,(Boolean)o) : new SubmodelParameterInfo<Boolean>(name,description,(Boolean)o,parent);
		else if (String.class.equals(c))
			return parent == null ? new ParameterInfo<String>(name,description,(String)o) : new SubmodelParameterInfo<String>(name,description,(String)o,parent);
		else if (File.class.isAssignableFrom(c)){
			return parent == null ? new ParameterInfo<File>(name,description,(File)o) : new SubmodelParameterInfo<File>(name,description,(File)o,parent);
		} else if (Enum.class.isAssignableFrom(c)){
			return parent == null ? new ParameterInfo<Enum>(name,description,(Enum)o) : new SubmodelParameterInfo<Enum>(name,description,(Enum)o,parent);
		}
		throw new IllegalArgumentException("invalid parameter (" + name + ") type (" + c + ")");
	}
	
	//----------------------------------------------------------------------------------------------------
	private SubmodelInfo<?> createSubmodelParameterInfo(final String name, final Class<?> type, final String description, 
														final Map<String,Object> submodelInformations, final SubmodelInfo<?> parent) {
		
		@SuppressWarnings("unchecked") final List<Class<?>> types = (List<Class<?>>) submodelInformations.get("types"); 
		SubmodelInfo<Object> result = new SubmodelInfo<Object>(name,description,null,types,type);
		result.setParent(parent);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<RecorderInfo> getRecorders() throws ModelInformationException {
		try {
			if (recordingClass == null){
				initializeRecordingClass();
			}
			final Method method = recordingClass.getMethod("getRecorders", SimState.class);
			return (List<RecorderInfo>)method.invoke(null, model);
		} catch (final NoSuchMethodException e) {
			throw new ModelInformationException(e);
		} catch (final SecurityException e) {
			throw new ModelInformationException(e);
		} catch (final IllegalAccessException e) {
			throw new ModelInformationException(e);
		} catch (final IllegalArgumentException e) {
			throw new ModelInformationException(e);
		} catch (final InvocationTargetException e) {
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
		} catch (final ClassNotFoundException e) {
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
			final Method method = recordingClass.getMethod("getRecorderPageXML", SimState.class);
			return (String)method.invoke(null, model);
		} catch (final NoSuchMethodException e) {
			throw new ModelInformationException(e);
		} catch (final SecurityException e) {
			throw new ModelInformationException(e);
		} catch (final IllegalAccessException e) {
			throw new ModelInformationException(e);
		} catch (final IllegalArgumentException e) {
			throw new ModelInformationException(e);
		} catch (final InvocationTargetException e) {
			throw new ModelInformationException(e);
		}
	}
	
	//------------------------------------------------------------------------------
	/**
	 * Returns all fields of <code>clazz</code> that can be access from an
	 * derived class.
	 */
	private Field[] getFields(final Class<?> clazz) {
		final List<Field> result = new ArrayList<Field>();
		getFieldsImpl(clazz,result);
		return result.toArray(new Field[0]);
	}

	//----------------------------------------------------------------------------------------------------
	private void getFieldsImpl(final Class<?> clazz, final List<Field> result) {
		final Field[] fields = clazz.getDeclaredFields();
		for (final Field f : fields) {
			if (!f.isSynthetic()) {
				result.add(f);
			}
		}
		final Class<?> parent = clazz.getSuperclass();
		if (parent != null)
			getFieldsImpl(parent,result);
	}
}
