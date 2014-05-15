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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import ai.aitia.meme.paramsweep.internal.platform.IGUIController.RunOption;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;

/** This class represents the parameters of the model. */
public class ParameterInfo extends MemberInfo implements Cloneable {
	
	//===============================================================================
	// members

	private static final long serialVersionUID = 1756550112305785450L;
	
	/** Parameter category constant: constant parameter. */
	public static final int CONST_DEF = 0;
	/** Parameter category constant: list parameter. */
	public static final int LIST_DEF = 1;
	/** Parameter category constant: iterator parameter. */
	public static final int INCR_DEF = 2;

	/** Category of the parameter. */
	protected int defType = CONST_DEF;
	/** Number of runs that uses the same value of the parameter. */
	protected long runs = -1;
	/** Value(s) of the parameter (used only in the case of constant and list parameter. */
	protected List<Object> values = null;
	/** The start value of the parameter (used only in the case of  iterator parameter. */
	protected Number startValue = null;
	/** The end value of the parameter (used only in the case of  iterator parameter. */
	protected Number endValue = null;
	/** The step value of the parameter (used only in the case of  iterator parameter. */
	protected Number step = null;
	/** The textual description of the parameter as returned by the model. */
	protected String description = null;
	
	//===============================================================================
	// methods
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param name name of the parameter
	 * @param type type of the parameter in string format
	 */
	public ParameterInfo(String name, String description, String type, Class<?> javaType) {
		super(name,type,javaType);
		this.description = description;
		values = new ArrayList<Object>();
	}
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param name name of the parameter
	 * @param type type of the parameter in string format
	 */
	public ParameterInfo(String name, String type, Class<?> javaType) {
		super(name,type,javaType);
		values = new ArrayList<Object>();
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public ParameterInfo clone() { return new ParameterInfo(this); }

	
	//-------------------------------------------------------------------------------
	public int getDefinitionType() { return defType; }
	public long getRuns() { return runs; }
	public Object getValue() { return (values.size() == 0 ? null : values.get(0)); }
	public List<Object> getValues() { return values; }
	public Number getStartValue() { return startValue; }
	public Number getEndValue() { return endValue; }
	public Number getStep() { return step; }
	public boolean isConstant() { return defType == CONST_DEF; }
		
	//-------------------------------------------------------------------------------
	public void setRuns(long runs) { this.runs = runs; }
	public void setValues(List<Object> values) { this.values = values; }
	public void setStartValue(Number startValue) { this.startValue = startValue; }
	public void setEndValue(Number endValue) { this.endValue = endValue; }
	public void setStep(Number step) { this.step = step; }
	
	//-------------------------------------------------------------------------------
	public void setDefinitionType(int defType) {
		if (defType < CONST_DEF || defType > INCR_DEF)
			throw new IllegalArgumentException();
		this.defType = defType;
	}

	//-------------------------------------------------------------------------------
	public void setValue(Object value) {
		values.clear();
		values.add(value);
	}
	
	//-------------------------------------------------------------------------------
	/** Sets the parameter to its default value. */
	public void setInitValue() {
		values.clear();
		if ("byte".equals(type) || "Byte".equals(type))
			values.add(new Byte("0"));
		else if ("short".equals(type) || "Short".equals(type)) 
			values.add(new Short("0"));
		else if ("int".equals(type) || "Integer".equals(type))
			values.add(new Integer(0));
		else if ("long".equals(type) || "Long".equals(type))
			values.add(new Long(0));
		else if ("float".equals(type) || "Float".equals(type))
			values.add(new Float(0));
		else if ("double".equals(type) || "Double".equals(type))
			values.add(new Double(0));
		else if ("boolean".equals(type) || "Boolean".equals(type))
			values.add(new Boolean(false));
		else if ("String".equals(type))
			values.add("null");
	}
	
	//-------------------------------------------------------------------------------
	/** Resets the parameter. */
	public void clear() {
		defType = CONST_DEF;
		runs = -1;
		values.clear();
		startValue = endValue = step = null;
	}
	
	//-------------------------------------------------------------------------------
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append(" - [");
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL && runs > 0) {
			sb.append("runs=");
			sb.append(runs);
			sb.append(",");
		}
		switch (defType) {
		case CONST_DEF : sb.append("value=");
						 if (isFile()) {
							 final File file = (File) values.get(0);
							 if (file != null)
								 sb.append(file.getAbsolutePath());
						 } else
							 sb.append(toStringWithoutFuckingScientificNotation(values.get(0),type));
						 break;
		case LIST_DEF  : sb.append("list=");
						 for (int i = 0;i < values.size();++i) {
							 if  (i != 0) sb.append(" ");
							 sb.append(toStringWithoutFuckingScientificNotation(values.get(i),type));
						 }
						 break;
		case INCR_DEF  : sb.append("start=");
						 sb.append(toStringWithoutFuckingScientificNotation(startValue,type));
						 sb.append(",end=");
						 sb.append(toStringWithoutFuckingScientificNotation(endValue,type));
						 sb.append(",increment=");
						 sb.append(toStringWithoutFuckingScientificNotation(step,type));
		}
		sb.append("]");
		return sb.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	public String valuesToString() {
		StringBuilder result = new StringBuilder();
		for (Object o : values)
			   result.append(" ").append(toStringWithoutFuckingScientificNotation(o,type));
		return result.toString().substring(1);
	}
	
	//----------------------------------------------------------------------------------------------------
	public String valueToString(Object value) {	return toStringWithoutFuckingScientificNotation(value,type); }
	public String startToString() {	return toStringWithoutFuckingScientificNotation(startValue,type); }
	public String endToString() {	return toStringWithoutFuckingScientificNotation(endValue,type); }
	public String stepToString() {	return toStringWithoutFuckingScientificNotation(step,type); }
	
	//-------------------------------------------------------------------------------
	/** Transforms an iterator parameter with negative step value to an equivalent 
	 *  parameter that uses positive step value. 
	 */
	public void transformIfNeed() {
		boolean need = false;
		if ("double".equals(type) || "Double".equals(type) ||
			"float".equals(type) || "Float".equals(type)) {
			double start = startValue.doubleValue(); 
			double end = endValue.doubleValue();
			double incr = step.doubleValue();
			need = incr < 0 && end <= start;
		} else if ("byte".equals(type) || "Byte".equals(type) ||
				   "short".equals(type) || "Short".equals(type) ||
				   "int".equals(type) || "Integer".equals(type) ||
				   "long".equals(type) || "Long".equals(type)) {
			long start = startValue.longValue();
			long end = endValue.longValue();
			long incr = step.longValue();
			need = incr < 0 && end <= start;
		}
		if (need) {
			Number temp = endValue;
			endValue = startValue;
			startValue = temp;
			abs();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public long getMultiplicity() {
		switch (defType) {
		case CONST_DEF	:
		case LIST_DEF	: return values.size();
		case INCR_DEF	: return calcStepCount(startValue,endValue,step);
		}
		return 0;
	}
	
	//----------------------------------------------------------------------------------------------------
	public static String defTypeToString(int defType) {
		switch (defType) {
		case CONST_DEF : return "constant";
		case LIST_DEF  : return "list";
		case INCR_DEF  : return "iteration";
		}
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	public static int defTypeFromString(String defType) {
		if ("constant".equals(defType))
			return CONST_DEF;
		if ("list".equals(defType))
			return LIST_DEF;
		if ("iteration".equals(defType))
			return INCR_DEF;
		return -1;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getDescription(){
		return description;
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean isSubmodelParameter() { return false; }
	
	//===============================================================================
	// private methods
	
	//-------------------------------------------------------------------------------
	protected ParameterInfo(ParameterInfo p) {
		super(p.name,p.type,p.javaType);
		values = new ArrayList<Object>(p.values);
		defType = p.defType;
		runs = p.runs;
		startValue = p.startValue;
		endValue = p.endValue;
		step = p.step;
		description = p.description;
	}
	
	//-------------------------------------------------------------------------------
	/** Sets <code>step</code> to its absolute value. */
	private void abs() {
		if ("byte".equals(type) || "Byte".equals(type)) {
			byte b = step.byteValue();
			if (b < 0) b *= Byte.parseByte("-1");
			step = new Byte(b);
		} else if ("short".equals(type) || "Short".equals(type)) {
			short s = step.shortValue();
			if (s < 0) s *= Short.parseShort("-1");
			step = new Short(s);
		} else if ("int".equals(type) || "Integer".equals(type)) {
			int i = step.intValue();
			i = Math.abs(i);
			step = new Integer(i);
		} else if ("long".equals(type) || "Long".equals(type)) {
			long l = step.longValue();
			l = Math.abs(l);
			step = new Long(l);
		} else if ("float".equals(type) || "Float".equals(type)) {
			float f = step.floatValue();
			f = Math.abs(f);
			step = new Float(f);
		} else if ("double".equals(type) || "Double".equals(type)) {
			double d = step.doubleValue();
			d = Math.abs(d);
			step = new Double(d);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	protected String toStringWithoutFuckingScientificNotation(Object num, String type) {
		try {
			if (null == num)
				return "null";
			StringBuilder result = new StringBuilder();
			String string = num.toString();
			if ("float".equals(type) || "Float".equals(type) ||
				"double".equals(type) || "Double".equals(type)) {
				String[] split = string.trim().split("E");
				if (split.length == 1) 
					return string;
				else {
					int exp = Integer.parseInt(split[1]);
					if (exp < 0) {
						int _exp = -1 * exp;
						for (int i = 0;i < _exp;++i) {
							result.append("0");
							if (i == 0)
								result.append(".");
						}
						result.append(split[0].replaceAll("\\.",""));
					} else {
						int dotIndex = split[0].indexOf('.');
						int fragment = split[0].substring(dotIndex,split[0].length()).length();
						result.append(split[0].replaceAll("\\.",""));
						if (fragment - exp > 0){
							result.insert(exp + 1, '.');
						} else {
							for (int i = 0; i <= exp - fragment;++i){ 
								result.append("0");
							}
						}
					}
					return result.toString();
				}
			} else
				return string;
		} catch (Throwable t) {
			RuntimeException rt = new RuntimeException(t); 
			StackTraceElement[] stackTrace = t.getStackTrace();
			StackTraceElement[] rTStackTrace = rt.getStackTrace();
			boolean needChange = false;
			for (int i = 0; i < stackTrace.length; ++i) {
				String methodName = stackTrace[i].getMethodName();
				if (methodName.indexOf("Fucking") >= 0) {
					String newMethod = methodName.replaceAll("Fucking","");
					stackTrace[i] = new StackTraceElement(stackTrace[i].getClassName(),
														  newMethod,
														  stackTrace[i].getFileName(),
														  stackTrace[i].getLineNumber());
					needChange = true;
				}
			}
			for (int i = 0; i < rTStackTrace.length;++i) {
				String methodName = rTStackTrace[i].getMethodName();
				if (methodName.indexOf("Fucking") >= 0) {
					String newMethod = methodName.replaceAll("Fucking","");
					rTStackTrace[i] = new StackTraceElement(rTStackTrace[i].getClassName(),
														  	newMethod,
														  	rTStackTrace[i].getFileName(),
														  	rTStackTrace[i].getLineNumber());
					needChange = true;
				}

			}
			try {
				if (needChange) {
					Field field = Throwable.class.getDeclaredField("stackTrace");
					field.setAccessible(true);
					field.set(t,stackTrace);
					field.set(rt,rTStackTrace);
					field.setAccessible(false);
				}
			} catch (Exception e) {}
			throw rt;
		}
	}
	
    //----------------------------------------------------------------------------------------------------
	private long calcStepCount(final Number s, final Number e, final Number st) {
		long cnt;
		if (Math.abs(e.longValue()) < Math.abs(s.longValue())) 
			cnt = _calcStepCount(e,s,st);
		else 
			cnt = _calcStepCount(s,e,st);
		return cnt;
	}
    
	//----------------------------------------------------------------------------------------------------
	private long _calcStepCount(final Number start, final Number end, final Number step) {
		final double s = start.doubleValue();
		final double e = end.doubleValue();
		final double st = step.doubleValue();
		return (long) Math.floor((Math.abs(e - s)) / Math.abs(st)) + 1;
	}
}
