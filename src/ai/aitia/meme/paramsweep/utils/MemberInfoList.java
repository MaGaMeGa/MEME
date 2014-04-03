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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import _.unknown;
import ai.aitia.meme.paramsweep.gui.info.ArgsFunctionMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.NLBreedMemberInfo;
import cern.colt.list.DoubleArrayList;

/** This class represents the list of the members of a Repast model. It provides 
 *  category-based (numbers, number collections) filter methods. */
public class MemberInfoList extends ArrayList<MemberInfo> {

	//=====================================================================================
	// members
	
	private static final long serialVersionUID = -4508472708173103998L;

	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Returns a list that contains those elements whose type is in the list <code>types</code>. */
	public List<MemberInfo> filter(List<String> types) {
		List<MemberInfo> result = new ArrayList<MemberInfo>();
		for (MemberInfo mi : this) {
			if (isInvalidParam(mi)) continue;
			if (types.contains(mi.getType())) 
				result.add(mi);
		}
		Collections.sort(result);
		return result;
	}
	
	//-------------------------------------------------------------------------------------
	/** Returns a list that contains those elements whose type is a numeric type. */
	public List<MemberInfo> filterNumbers() {
		String[] types = { "byte", "Byte", "short", "Short", "int", "Integer", "long", "Long",
						   "float", "Float", "double", "Double" };
		return filter(Arrays.asList(types));
	}
	
	//-------------------------------------------------------------------------------------
	/** Returns a list that contains those elements whose type may be numeric collection type. */
	public List<MemberInfo> filterNumberCollections() {
		String[] types = { "byte[]", "Byte[]", "short[]", "Short[]", "int[]", "Integer[]",
						   "long[]", "Long[]", "float[]", "Float[]", "double[]", "Double[]",
						   "DoubleArrayList", "List", "java.util.List", "unknown", "list" }; // we don't know the component type of List in runtime,
																		  			         // so we accept all lists. The user of this method must
																		  			         // expect that not all MemberInfo describe a 
																		  			         // List<? extends Number> type.
		//MYTODO: supports further types
		return filter(Arrays.asList(types));
	}
	
	//-------------------------------------------------------------------------------------
	/** Returns a list that contains those elements whose type is a numeric type or may
	 *  be a numeric collection type. */
	public List<MemberInfo> filterNumbersAndNumberCollections() {
		List<MemberInfo> result = new ArrayList<MemberInfo>(filterNumbers());
		result.addAll(filterNumberCollections());
		Collections.sort(result);
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<MemberInfo> filterObject() {
		List<MemberInfo> result = new ArrayList<MemberInfo>();
		for (MemberInfo mi : this) {
			if (isInvalidParam(mi)) continue;
			if ((!Util.isAcceptableType(mi.getJavaType()) &&
				!Character.TYPE.equals(mi.getJavaType()) &&
				!Character.class.equals(mi.getJavaType()) &&
				!mi.getJavaType().isArray() || mi.getJavaType().equals(String.class))
				&& !result.contains(mi))
				result.add(mi);
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<MemberInfo> filterScalars() {
		final List<MemberInfo> result = new ArrayList<MemberInfo>();
		for (MemberInfo mi : this) {
			if (isInvalidParam(mi)) continue;
			if (!result.contains(mi) && !mi.getJavaType().isArray() && !Collection.class.isAssignableFrom(mi.getJavaType()) &&
				!DoubleArrayList.class.isAssignableFrom(mi.getJavaType()) && !Map.class.isAssignableFrom(mi.getJavaType()))
				result.add(mi);
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<MemberInfo> filterKeyObject() {
		List<MemberInfo> result = new ArrayList<MemberInfo>();
		for (MemberInfo mi : this) {
			if (isInvalidParam(mi)) continue;
			if (!result.contains(mi) && !mi.getJavaType().isPrimitive())
				result.add(mi);
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<MemberInfo> filterCollection() {
		List<MemberInfo> result = new ArrayList<MemberInfo>();
		for (MemberInfo mi : this) {
			if (isInvalidParam(mi)) continue;
			if (!result.contains(mi) && (mi.getJavaType().isArray() ||
				Collection.class.isAssignableFrom(mi.getJavaType())) ||
				unknown.class.isAssignableFrom(mi.getJavaType()))
				result.add(mi);
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<MemberInfo> filterOneDimListOnly() {
		List<MemberInfo> result = new ArrayList<MemberInfo>();
		for (MemberInfo mi : this) {
			if (isInvalidParam(mi)) continue;
			if (!result.contains(mi) && ((mi.getJavaType().isArray() &&
				!mi.getInnerType().isArray()) ||
				List.class.isAssignableFrom(mi.getJavaType()) ||
				unknown.class.isAssignableFrom(mi.getJavaType())))
				result.add(mi);
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<MemberInfo> filterMap() {
		List<MemberInfo> result = new ArrayList<MemberInfo>();
		for (MemberInfo mi : this) {
			if (isInvalidParam(mi)) continue;
			if (!result.contains(mi) && Map.class.isAssignableFrom(mi.getJavaType()))
				result.add(mi);
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<MemberInfo> filterNonPrimitveCollections() {
		List<MemberInfo> result = new ArrayList<MemberInfo>();
		for (MemberInfo mi : this) {
			if (isInvalidParam(mi)) continue;
			if (!result.contains(mi) && (mi.getJavaType().isArray() && 
				!isPrimitiveArray(mi.getJavaType())
				|| Collection.class.isAssignableFrom(mi.getJavaType())))
				result.add(mi);
		}
		return result;
	}

	//-------------------------------------------------------------------------------------
	public List<MemberInfo> filterInvalids() {
		List<MemberInfo> result = new ArrayList<MemberInfo>();
		for (MemberInfo mi : this) {
			if (!isInvalidParam(mi))
				result.add(mi);
		}
		Collections.sort(result);
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<MemberInfo> filterBreeds() {
		final List<MemberInfo> result = new ArrayList<MemberInfo>();
		for (final MemberInfo mi : this) {
			if (mi instanceof NLBreedMemberInfo)
				result.add(mi);
		}
		return result;
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private boolean isInvalidParam(MemberInfo info) { return Void.TYPE.equals(info.getJavaType()) || (info instanceof ArgsFunctionMemberInfo); }
	
	//----------------------------------------------------------------------------------------------------
	private boolean isPrimitiveArray(Class<?> type) {
		if (type.isArray()) {
			return isPrimitiveArray(type.getComponentType());
		}
		return type.isPrimitive();
	}
}
