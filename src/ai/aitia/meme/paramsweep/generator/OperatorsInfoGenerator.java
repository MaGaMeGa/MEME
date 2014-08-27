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
package ai.aitia.meme.paramsweep.generator;

import java.util.ArrayList;
import java.util.List;

import ai.aitia.meme.paramsweep.gui.info.ExtendedOperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.GeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MultiColumnOperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.OperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IExtendedOperatorPlugin;
import ai.aitia.meme.paramsweep.plugin.IListOperatorPlugin;
import ai.aitia.meme.paramsweep.plugin.IMultiColumnOperatorPlugin;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin;
import ai.aitia.meme.paramsweep.utils.AssistantMethod;
import ai.aitia.meme.paramsweep.utils.AssistantMethod.ScheduleTime;
import ai.aitia.meme.paramsweep.utils.PlatformConstants;

public class OperatorsInfoGenerator {
	
	//=====================================================================================
	// members
	
	public static final String EXCEPTION_START = "[OPERATOR EXCEPTION]";
	public static final String EXCEPTION_END	= "[END OF OPERATOR EXCEPTION]";
	
	private IOperatorPlugin operator = null;
	private List<String> error = null;
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	public OperatorsInfoGenerator(IOperatorPlugin operator) {
		this.operator = operator;
		this.error = new ArrayList<String>();
	}
	
	//-------------------------------------------------------------------------------------
	public List<String> getError() { return error; }
 	
	//-------------------------------------------------------------------------------------
	public OperatorGeneratedMemberInfo generateInfoObject(String origName, Object... actParameters) {
		// -1 indicates that the number is parameters is not a constant value.
		if (operator.getNumberOfParameters() != -1 && actParameters.length != operator.getNumberOfParameters()) {
			error.add("Invalid number of parameters!");
			return null;
		}
		
		List<String> typeErrors = checkParameterTypes(actParameters);
		if (typeErrors != null) {
			error.addAll(typeErrors);
			return null;
		}
		
		String name = origName;
		if (!name.endsWith("()") && PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) 
			name += "()";
		
		String simpleName = getSimpleName(operator.getReturnType(actParameters));
		OperatorGeneratedMemberInfo result = null;
		if (operator instanceof IExtendedOperatorPlugin){
			result = new ExtendedOperatorGeneratedMemberInfo(name,simpleName,operator.getReturnType(actParameters));
		} else if (operator instanceof IMultiColumnOperatorPlugin){
			result = new MultiColumnOperatorGeneratedMemberInfo(name, simpleName, operator.getReturnType(actParameters), ((IMultiColumnOperatorPlugin)operator).getNumberOfColumns(actParameters));
		} else {
			result = new OperatorGeneratedMemberInfo(name,simpleName,operator.getReturnType(actParameters));
		}
		
		for (Object obj : actParameters) {
			if (obj instanceof GeneratedMemberInfo) {
				GeneratedMemberInfo gmi = (GeneratedMemberInfo) obj;
				result.addReference(gmi);
			} else if (obj instanceof List) {
				List<?> list = (List<?>) obj;
				for (Object _obj : list) {
					if (_obj instanceof GeneratedMemberInfo) {
						GeneratedMemberInfo gmi = (GeneratedMemberInfo) _obj;
						result.addReference(gmi);
					}
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5)
			sb.append("try {\n");
		sb.append(operator.getCode(actParameters)); // TODO we have to provide the name of the class declaring the member to allow the generated code to cast the member access 
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) {
			sb.append("}\n");
			sb.append("catch (Throwable t) {\n");
			sb.append("System.err.println(\"" + EXCEPTION_START + "\");\n");
	        sb.append("System.err.println(\"Name: " + name + "\");\n");
	        sb.append("System.err.println(t.getLocalizedMessage());\n");
			sb.append("t.printStackTrace();\n");
			sb.append("System.err.println(\"" + EXCEPTION_END + "\");\n");
			sb.append("throw new RuntimeException(t);\n");
			sb.append("}\n");
		}
		result.setSource(sb.toString());
		result.setDisplayName(operator.getInstanceDisplayName(name,actParameters));
		if (operator instanceof IListOperatorPlugin) {
			IListOperatorPlugin _operator = (IListOperatorPlugin) operator;
			result.setInnerType(_operator.getInnerClass(actParameters));
		}
		if (operator instanceof IExtendedOperatorPlugin)
			addAssistantMethods((ExtendedOperatorGeneratedMemberInfo)result,actParameters);
		
		return result;
	}
	
	//=====================================================================================
	// private methods
	
	//-------------------------------------------------------------------------------------
	private List<String> checkParameterTypes(Object... actParameters) {
		List<String> errors = operator.checkParameters(actParameters); 
		return errors.size() == 0 ? null : errors;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getSimpleName(Class<?> type) {
		if (List.class.isAssignableFrom(type))
			return "List";
		return type.getSimpleName();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void addAssistantMethods(final ExtendedOperatorGeneratedMemberInfo result, final Object... actParameters) {
		final IExtendedOperatorPlugin op = (IExtendedOperatorPlugin) operator;
		for (int i = 0;i < op.getAssistantMethodCount();++i) {
			final StringBuilder sb = new StringBuilder();
			if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5)
				sb.append("try {\n");
			sb.append(op.getCode(i,actParameters));
			if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) {
				String _methodName = result.getName();
				if (_methodName.endsWith("()"))
					_methodName = _methodName.substring(0,_methodName.length() - 2);
				_methodName += PlatformConstants.AITIA_GENERATED_INFIX + i + "()";

				sb.append("}\n");
				sb.append("catch (Throwable t) {\n");
				sb.append("System.err.println(\"" + EXCEPTION_START + "\");\n");
		        sb.append("System.err.println(\"Name: " + _methodName + "\");\n");
		        sb.append("System.err.println(t.getLocalizedMessage());\n");
				sb.append("t.printStackTrace();\n");
				sb.append("System.err.println(\"" + EXCEPTION_END + "\");\n");
				sb.append("throw new RuntimeException(t);\n");
				sb.append("}\n");
			}
			final Class<?> returnType = op.getReturnType(i,actParameters);
			final ScheduleTime scheduleTime = op.getScheduleTime(i);
			result.addAssistantMethod(new AssistantMethod(sb.toString(),returnType,scheduleTime));
		}
	}
}
