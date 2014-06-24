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
package ai.aitia.meme.paramsweep.operatorPlugin;

import java.util.ArrayList;
import java.util.List;

import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings.UnsupportedPlatformException;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IMultiColumnOperatorPlugin;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;

public class Operator_MultiColumnRecordable implements IMultiColumnOperatorPlugin {

	@Override public String getName() { return "multiColumnRecordable"; }
	@Override public String getLocalizedName() { return "Multi Column Recordable"; }
	@Override public int getNumberOfParameters() { return 6; }//collection, inner type, member, recording length, n/a filler, datasource name
	@Override public String getDescription() { return "Enables MEME to record a selected field of elements " +
														"of a collection or an array in separate columns.";}
	@Override public boolean isRecordable(Object... actParams) { return true; }
	@Override public OperatorGUIType getGUIType() {
		return OperatorGUIType.MULTIPLE_COLUMN;
	}
	
	@Override
	public String getCode(Object... actParams) {
		MemberInfo info = (MemberInfo) actParams[0];
		Class<?> innerType = (Class) actParams[1];
		MemberInfo member = actParams[2] instanceof MemberInfo ? (MemberInfo) actParams[2] : null;
//		String recordingLength = (String) actParams[3];
		String noDataFiller = (String) actParams[4];
		String dataSourceName = (String) actParams[5];
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		:
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM 	: return getJavaCode(info,innerType,member,noDataFiller, dataSourceName);
		case NETLOGO5	:
		case NETLOGO	: 
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());
		}
	}
	
	@Override
	public String getNumberOfColumns(Object... actParams) {
		return (String) actParams[3];
	}

	@Override
	public List<String> checkParameters(Object... actParams) {
		List<String> result = new ArrayList<String>();
		if (!(actParams[0] instanceof MemberInfo))
			result.add("Parameter #0 is not a MemberInfo instance.");
		if (!(actParams[1] instanceof Class))
			result.add("Parameter #1 is not a Class instance.");
		if (!(actParams[2] instanceof MemberInfo) && !(actParams[2] instanceof String))
			result.add("Parameter #2 is not a MemberInfo instance.");
		if (!(actParams[3] instanceof String))
			result.add("Parameter #3 is not an String.");
		if (!(actParams[4] instanceof String))
			result.add("Parameter #4 is not a String.");
		if (!(actParams[5] instanceof String))
			result.add("Parameter #5 is not a String.");
		return result;
	}

	@Override
	public Class<?> getReturnType(Object... actParams) {
		return ArrayList.class;
	}

	@Override
	public String getInstanceDisplayName(String name, Object... actParams) {
		String _name = name;
		if (_name.endsWith("()"))
			_name = _name.substring(0,_name.length() - 2);
		MemberInfo info = (MemberInfo) actParams[0];
		String recordingLength = (String) actParams[3];
		if (actParams[2] instanceof MemberInfo) {
			MemberInfo element = (MemberInfo) actParams[2];
			return _name + "(" + Utilities.name(info) + "." + Utilities.name(element) + "["+recordingLength+"])";
		} else {
			return _name + "(" + Utilities.name(info) + "["+recordingLength+"])";
		}
	}

	@Override
	public boolean isSupportedByPlatform(PlatformType platform) {
		switch (platform) {
		case REPAST:
		case MASON:
		case CUSTOM:
			return true;
		case NETLOGO5:
		case NETLOGO:
			return false;
			//TODO: add NetLogo support
		}
		return false;
	}

	@Override
	public Class<?> getInnerClass(Object... actParams) {
		Class<?> innerType = (Class) actParams[1];
		return innerType;
	}

	private String getJavaCode(MemberInfo collection, Class<?> innerType, MemberInfo member, String noDataFiller, String dataSourceName) {
		String collectionName = collection.getName();
		String accessor = Util.getMethodAccessorInGeneratedModel(collectionName);
		StringBuilder code = new StringBuilder();
		code.append("int recordingLength = " + Util.GENERATED_MODEL_RECORDER_VARIABLE_NAME + ".getCollectionLength(\"" + dataSourceName + "\");\n");
		code.append("java.util.ArrayList result = new ai.aitia.meme.paramsweep.utils.SeparatedList(recordingLength);\n");
		if (collection.getType().endsWith("[]")) {
			code.append("int collectionLength = 0;\n");
			code.append("if (" + accessor + " != null) collectionLength = " + accessor + ".length;\n");
			code.append("for (int i = 0 ; i < recordingLength ; ++i) {\n");
			code.append("\n");
			code.append("\n");
			code.append("\n");
			code.append("if (i < collectionLength) {\n");
			if (member != null) {
				code.append(boxingIfNeed(accessor + "[i]." + member.getName(),member.getJavaType()));
			} else {
				code.append(boxingIfNeed(accessor + "[i]",innerType));
			}
			code.append("} else {\n");
			code.append("result.add(\""+noDataFiller+"\");\n");
			code.append("}\n");
			code.append("}\n");
		} else {
			code.append("int collectionLength = 0;\n");
			code.append("if ("+accessor + " != null) collectionLength = "+ accessor + ".size();\n");
			code.append("Object[] source = null;\n");
			code.append("if (" + accessor + " != null) source = " + accessor + ".toArray();\n");
			code.append("else source = new Object[0];\n");
			code.append("for (int i = 0 ; i < recordingLength ; ++i) {\n");
			code.append("if (i < collectionLength) {\n");
			if (member != null) {
				code.append(boxingIfNeed("((" + innerType.getName() + ")source[i])." + member.getName(),member.getJavaType()));
			} else {
				code.append(boxingIfNeed("((" + innerType.getName() + ")source[i])",innerType));
			}
			code.append("} else {\n");
			code.append("result.add(\""+noDataFiller+"\");\n");
			code.append("}\n");
			code.append("}\n");
		}
		code.append("return result;\n");
		return code.toString();
	}

	private String boxingIfNeed(String core, Class<?> memberType) {
		StringBuilder code = new StringBuilder();
		code.append("result.add(");
		if (memberType.isPrimitive()) 
			code.append("new " + boxingType(memberType).getName() + "(");
		code.append(core.replace("$", "."));
		if (memberType.isPrimitive())
			code.append(")");
		code.append(");\n");
		return code.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private Class<?> boxingType(Class<?> memberType) {
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
