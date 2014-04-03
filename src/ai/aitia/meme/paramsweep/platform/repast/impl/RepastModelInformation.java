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
package ai.aitia.meme.paramsweep.platform.repast.impl;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javassist.CannotCompileException;
import javassist.CtClass;
import uchicago.src.sim.engine.BaseController;
import uchicago.src.sim.engine.SimEvent;
import ai.aitia.meme.paramsweep.batch.IModelInformation;
import ai.aitia.meme.paramsweep.batch.output.NonRecordableFunctionInfo;
import ai.aitia.meme.paramsweep.batch.output.NonRecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;
import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.utils.Util;
import fables.paramsweep.runtime.annotations.InnerClass;

public class RepastModelInformation implements IModelInformation {

	//====================================================================================================
	// members
	
	private static List<String> bannedMethodNames = new ArrayList<String>(6);
	static {
		bannedMethodNames.add("hashCode");
		bannedMethodNames.add("toString");
		bannedMethodNames.add("clone");
		bannedMethodNames.add("getClass");
		bannedMethodNames.add("finalize");
		bannedMethodNames.add("main");
	}
	
	private IPSWInformationProvider provider = null;
	private CtClass modelClass = null;
	
	private List<ParameterInfo<?>> parameters = null;
	private List<RecordableInfo> recordables = null;
	private List<NonRecordableInfo> nonRecordables = null;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public RepastModelInformation(IPSWInformationProvider provider, CtClass modelClass) {
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
				error = "RepastJ compatibility error while processing the model : " + modelClass.getSimpleName();
				error += "\nThe model may use old RepastJ version.";
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
			return error;
		}
		String[] originalInitParamResult = null;
		try {
			Class controllerIntfs = Class.forName("uchicago.src.sim.engine.IController",true,provider.getCustomClassLoader());
			Class controllerClass = Class.forName("ai.aitia.meme.paramsweep.platform.repast.impl.RepastModelInformation$DummyController",true,
												  provider.getCustomClassLoader());
			javaClass.getMethod("setController",new Class[] { controllerIntfs }).invoke(model,new Object[] { controllerClass.newInstance() });
			javaClass.getMethod("setup",(Class[])null).invoke(model,(Object[])null);
			originalInitParamResult = (String[])javaClass.getMethod("getInitParam",(Class[])null).invoke(model,(Object[])null);
		} catch (Exception e) {
			// we checked the existense of these methods in the RepastPlatform.checkModel() method
			throw new IllegalStateException();
		}
		String[] paramsMod = capitalizeParams(originalInitParamResult);
		parameters = new ArrayList<ParameterInfo<?>>();
		for (String parameterName : paramsMod) {
			if (parameterName.equals("")) continue;
			try {
				Method m = null;
				try {
					m = javaClass.getMethod("get" + parameterName,(Class[])null);
				} catch (NoSuchMethodException e) {
					m = javaClass.getMethod("is" + parameterName,(Class[])null);
				}
				javaClass.getMethod("set" + parameterName,new Class[] { m.getReturnType() });
				parameters.add(createParameterInfo(model,parameterName,m));
			} catch (NoSuchMethodException e) {
				return "Cannot find this method: " + Util.getLocalizedMessage(e);
			} catch (IllegalAccessException e) {
				return "The method " + Util.getLocalizedMessage(e) + " is not visible.";
			} catch (Exception e) {
				return e.getClass().getSimpleName() + " occurs during the initialization: " + Util.getLocalizedMessage(e);
			}
		}
		//Collections.sort((List<ParameterInfo<?>>)parameters);
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Capitalizes the parameter names and returns in a new array.
	 * @param params the parameter names
	 */
	private String[] capitalizeParams(String[] params) {
		boolean isRngSeedParameter = provider.rngSeedAsParameter(); 
		if (params == null || params.length == 0) 
			return  isRngSeedParameter ? new String[] { "RngSeed" } : new String[0];
		boolean hasRngSeedRef = false;
		for (String s : params) {
			if (s.equals("rngSeed") || s.equals("RngSeed")) {
				hasRngSeedRef = true;
				break;
			}
		}
		String[] result = hasRngSeedRef || !isRngSeedParameter ? new String[params.length] : new String[params.length + 1];
		for (int i = 0;i < params.length;++i) {
			if (params[i] == null || params[i].equals("")) {
				result[i] = "";
				continue;
			}
			char start = params[i].charAt(0);
			if (Character.isLowerCase(start)) 
				result[i] = String.valueOf(Character.toUpperCase(start)) + params[i].substring(1);
			else
				result[i] = params[i];
		}
		if (isRngSeedParameter && !hasRngSeedRef)
			result[params.length] = "RngSeed";
		return result;
	}

	//----------------------------------------------------------------------------------------------------
	private ParameterInfo<?> createParameterInfo(Object model, String name, Method method) throws IllegalAccessException, InvocationTargetException {
		Object o = method.invoke(model,(Object[])null);
		Class<?> c = method.getReturnType();
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
		if (provider.rngSeedAsParameter())
			reserved.add("rngSeed");
		
		Field[] fields = Util.getFields(javaClass);
		for (Field field : fields) {
			RecordableInfo info = new RecordableInfo(field.getName(),field.getType(),null,field.getName());
			if (reserved.contains(Util.capitalize(field.getName())) ||
				reserved.contains(Util.uncapitalize(field.getName())) ||
				Modifier.isFinal(field.getModifiers()))
				continue;
			if (	Util.isAcceptableType(field.getType())) {
				recordables.add(info);
			}
		}

		Method[] methods = Util.getMethods(javaClass);
		for (Method method : methods) {
			if (method.getParameterTypes().length == 0 && !method.getReturnType().equals(Void.TYPE) && !bannedMethodNames.contains(method.getName())) {
				String name = method.getName() + "()";
				RecordableInfo info = new RecordableInfo(method.getName(),method.getReturnType(),null,name);
				if ((method.getName().length() > 3 && reserved.contains(method.getName().substring(3))) ||
					(method.getName().length() > 3 && reserved.contains(Util.uncapitalize(method.getName().substring(3)))) ||
					(method.getName().length() > 2 && reserved.contains(method.getName().substring(2))) ||
					(method.getName().length() > 2 && reserved.contains(Util.uncapitalize(method.getName().substring(2))))) {
						if (method.getName().startsWith("get") || method.getName().startsWith("is")) continue;
				}
				if (Util.isAcceptableType(method.getReturnType()) && !"getPropertiesValues".equals(method.getName()) &&
					!"hashCode".equals(method.getName()) && !"toString".equals(method.getName())) 
					recordables.add(info);
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
				InnerClass ic = field.getAnnotation(InnerClass.class);
				if (ic != null) {
					String ic_string = ic.value();
					info.setInnerType(Util.convertToClass(ic_string,provider.getCustomClassLoader()));
				} else 
					info.setInnerType(Util.innerType(field.getType()));
				nonRecordables.add(info);
			}
		}
		
		Method[] methods = Util.getMethods(javaClass);
		for (Method method : methods) {
			if ((method.getParameterTypes().length == 0 && !method.getReturnType().equals(Void.TYPE) &&
				Util.isAcceptableType(method.getReturnType())) || bannedMethodNames.contains(method.getName())) continue;
			NonRecordableFunctionInfo info = new NonRecordableFunctionInfo(method.getName(),method.getReturnType(),null,method.getName() + "()",
																		   Arrays.asList(method.getParameterTypes()));
			InnerClass ic = method.getAnnotation(InnerClass.class);
			if (ic != null) {
				String ic_string = ic.value();
				info.setInnerType(Util.convertToClass(ic_string,provider.getCustomClassLoader()));
			} else 
				info.setInnerType(Util.innerType(method.getReturnType()));
			nonRecordables.add(info);
		}
	}
	
	//====================================================================================================
	// nested classes
	
	//------------------------------------------------------------------------------
	/** This is a dummy implementation of uchicago.src.sim.engine.IController interface.
	 *  We need this because we must call the setup() method of the model to initialize
	 *  the parameters and in certain cases setup() throws a NullPointerException if
	 *  the controller of the model is null. So we use this class as a temporary controller
	 *  that does nothing.
	 */
	public static class DummyController extends BaseController {
		@Override protected void onTickCountUpdate() {}
		public void simEventPerformed(SimEvent evt) {}
		public void exitSim() {}
		public long getRunCount() {	return 0; }
		public boolean isGUI() { return false; }
		@Override public boolean isBatch() { return true; }
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
