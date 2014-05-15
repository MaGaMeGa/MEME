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
package ai.aitia.meme.paramsweep.gui.info;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import _.unknown;
import ai.aitia.meme.paramsweep.utils.Util;

/** This class represents the relevant members (variables and methods too) of the model. */
public class MemberInfo extends NodeInfo {

	//===============================================================================
	// members

	private static final long serialVersionUID = 4380198319616116054L;
	
	/** The numeric types in string format. */
	private static List<String> numericTypes = new ArrayList<String>(7);
	static {
		numericTypes.add("byte");
		numericTypes.add("Byte");
		numericTypes.add("short");
		numericTypes.add("Short");
		numericTypes.add("int");
		numericTypes.add("Integer");
		numericTypes.add("long");
		numericTypes.add("Long");
		numericTypes.add("float");
		numericTypes.add("Float");
		numericTypes.add("double");
		numericTypes.add("Double");
		numericTypes.add("Number"); // for java.lang.Number
	}
	
	/** The type of the member in string format (return type in the case of methods). */
	protected String type = null;
	
	protected Class<?> javaType = null;
	
	/* Void.TYPE means there is no inner type, null means the inner type is unknown. */
	protected Class<?> innerType = Void.TYPE;
	
	//===============================================================================
	// methods
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param name the name of the member 
	 * @param type the type of the member in string format (return type in the case of methods)
	 */
	public MemberInfo(String name, String type, Class<?> javaType) {
		super(name);
		this.type = type;
		this.javaType = javaType;
		this.innerType = Util.innerType(javaType);
	}
	
	//-------------------------------------------------------------------------------
	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	public Class<?> getJavaType() { return javaType; }
	public void setJavaType(Class<?> javaType) { this.javaType = javaType; }
	public Class<?> getInnerType() { return innerType; }
	public void setInnerType(Class<?> innerType) { this.innerType = innerType; }
	/** Tests if the member has a numeric type. */
	public boolean isNumeric() { return numericTypes.contains(this.type); }
	/** Tests if the member has logical type. */
	public boolean isBoolean() { return ("boolean".equals(type) || "Boolean".equals(type)); }
	
	/**
	 * Tests whether the memeber is an enum or not.
	 * 
	 * @return true is this member info describes an enum member
	 */
	public boolean isEnum() { return Enum.class.isAssignableFrom(javaType);} 
	/** Tests if the member has primitive type. */
	public boolean isPrimitive() {
		if ("boolean".equals(type)) return true;
		return numericTypes.contains(this.type) && Character.isLowerCase(type.charAt(0));
	}
	public boolean isFile() {return "File".equals(type) || "file".equals(type);}
	//-------------------------------------------------------------------------------
	@Override
	public String toString() {
		return super.toString() + " : " + (type.equals("List") || javaType.equals(unknown.class) ? javaType.getSimpleName() : type);
	}
	
	//-------------------------------------------------------------------------------
	/** Tests if the <code>value</code> is a interpretable as a <code>type</code> value. */
	public static boolean isValid(String value, String type) {
		if ("byte".equals(type) || "Byte".equals(type))
			return getByteValue(value.trim()) != null;
		if ("short".equals(type) || "Short".equals(type))
			return getShortValue(value.trim()) != null;
		if ("int".equals(type) || "Integer".equals(type))
			return getIntegerValue(value.trim()) != null;
		if ("long".equals(type) || "Long".equals(type))
			return getLongValue(value.trim()) != null;
		if ("float".equals(type) || "Float".equals(type))
			return getFloatValue(value.trim()) != null;
		if ("double".equals(type) || "Double".equals(type))
			return getDoubleValue(value.trim()) != null;
		if ("boolean".equals(type) || "Boolean".equals(type))
			return (value.trim().equals("true") || value.trim().equals("false"));
		if ("String".equals(type))
			return getStringValue(value.trim()) != null;
		if ("File".equals(type))
			return new File(getStringValue(value.trim())).isFile();
		return false;
	}
	
	//-------------------------------------------------------------------------------
	/** Returns <code>value</code> string as a value with type <code>type</code>. If
	 *  <code>value</code> is not interpretable as a <code>type</code> value, it returns
	 *  null. If <code>type</code> is a primitive type the the method uses its wrapper
	 *  type instead.
	 */
	public static Object getValue(String value, String type) {
		if ("byte".equals(type) || "Byte".equals(type))
			return getByteValue(value.trim());
		if ("short".equals(type) || "Short".equals(type))
			return getShortValue(value.trim());
		if ("int".equals(type) || "Integer".equals(type))
			return getIntegerValue(value.trim());
		if ("long".equals(type) || "Long".equals(type))
			return getLongValue(value.trim());
		if ("float".equals(type) || "Float".equals(type))
			return getFloatValue(value.trim());
		if ("double".equals(type) || "Double".equals(type))
			return getDoubleValue(value.trim());
		if ("boolean".equals(type) || "Boolean".equals(type))
			return new Boolean(value.trim());
		if ("String".equals(type))
			return getStringValue(value.trim());
		if ("File".equals(type)) {
			return new File(value.trim());
		}
		return null;
	}
	
	//===============================================================================
	// private methods
	
	//-------------------------------------------------------------------------------
	/** Returns <code>value</code> as a <code>Byte</code> value. */
	private static Byte getByteValue(String value) {
		try {
			return new Byte(value);
		} catch (NumberFormatException e) {
			try {
				if (value.endsWith(".0"))
					return new Byte(value.substring(0,value.length() - 2));
			} catch (NumberFormatException ee) {}
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Returns <code>value</code> as a <code>Short</code> value. */
	private static Short getShortValue(String value) {
		try {
			return new Short(value);
		} catch (NumberFormatException e) {
			try {
				if (value.endsWith(".0"))
					return new Short(value.substring(0,value.length() - 2));
			} catch (NumberFormatException ee) {}
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Returns <code>value</code> as an <code>Integer</code> value. */
	private static Integer getIntegerValue(String value) {
		try {
			return new Integer(value);
		} catch (NumberFormatException e) {
			try {
				if (value.endsWith(".0"))
					return new Integer(value.substring(0,value.length() - 2));
			} catch (NumberFormatException ee) {}
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Returns <code>value</code> as a <code>Long</code> value. */
	private static Long getLongValue(String value) {
		try {
			return new Long(value);
		} catch (NumberFormatException e) {
			try {
				if (value.endsWith(".0"))
					return new Long(value.substring(0,value.length() - 2));
			} catch (NumberFormatException ee) {}
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Returns <code>value</code> as a <code>Float</code> value. */
	private static Float getFloatValue(String value) {
		try {
			return new Float(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Returns <code>value</code> as a <code>Double</code> value. */
	private static Double getDoubleValue(String value) {
		try {
			return new Double(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Returns <code>value</code> as a <code>String</code> value. Eliminates
	 *  the white spaces. */
	private static String getStringValue(String value) {
		String newString = value.replaceFirst("\\s","");
		return  (value.equals(newString) ? value : null);
	}
}
