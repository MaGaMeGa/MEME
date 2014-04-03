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
package ai.aitia.meme.paramsweep.platform.netlogo5.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import _.unknown;
import ai.aitia.meme.paramsweep.batch.IParameterSweepResultReader;
import ai.aitia.meme.paramsweep.batch.ReadingException;
import ai.aitia.meme.paramsweep.batch.ResultValueInfo;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;
import ai.aitia.meme.utils.Utils;

public class IntelliSweepNetLogoResultParser implements IParameterSweepResultReader {

	//====================================================================================================
	// members
	
	private final File file;
	private final String delimiter;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public IntelliSweepNetLogoResultParser(final List<RecorderInfo> recorders) {
		file = recorders.get(0).getOutputFile();
		delimiter = String.valueOf(recorders.get(0).getDelimiter().charAt(0));
	}

	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public List<ResultValueInfo> getResultValueInfos(final RecordableInfo info, final List<ParameterInfo<?>> parameterCombination) throws ReadingException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			return read(reader,info,parameterCombination);
		} catch (final FileNotFoundException e) {
			throw new ReadingException(file.getName() + " is not exist.",e);
		} finally {
			try {
				if (reader != null) 
					reader.close();
			} catch (IOException e) {}
		}
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private List<ResultValueInfo> read(final BufferedReader reader, final RecordableInfo info, final List<ParameterInfo<?>> parameterCombination)
									   throws ReadingException {
		final List<ResultValueInfo> result = new ArrayList<ResultValueInfo>();
		
		String[] names = null;
		
		final int HEADER			= 1,
				  EMPTY_LINES1		= 2,
				  COLNAMES			= 3,
				  ROWS				= 4,
				  EMPTY_LINES2		= 5;
		
		String line = null;
		int state = HEADER;
		String err = null;
		
		String value = null;
		Class<?> type = null;
		int stepIndex = -1;

		do {
			try {
				line = reader.readLine();
			} catch (IOException e) {
				throw new ReadingException(e);
			}
			if (line == null) break;
			line = line.trim();
			
			if (state < EMPTY_LINES1 && line.length() == 0) {
				state = EMPTY_LINES1;
				continue;
			}
			
			if (state == EMPTY_LINES1) {
				if (line.length() == 0) continue;
				state = COLNAMES;
			}
			
			if (state == COLNAMES) {
				boolean ok = line.length() > 13;
				if (ok) {
					final String low = line.toLowerCase(Locale.US);
					ok = low.startsWith("\"[run number]\"");
					if (ok) 
						ok = low.indexOf("[step]") >= 13;
				}
				
				names = ok ? Utils.parseCSVLine(line,delimiter,'"',Utils.EXC_QUOTE) : null;
				trimAll(names);
				ok = (names != null && names.length >= 3);
				if (!ok) {
					err = "missing columns";
					break;
				}
				
				ok = (names[0].equalsIgnoreCase("[run number]") && (stepIndex = findIndex(names,"[step]",false)) >= 1);
				if (!ok) {
					err = "invalid column labels";
					break;
				}
				
				final List<String> pars = new ArrayList<String>();
				for (int i = 1;i < names.length && err == null;++i) {
					if (i == stepIndex) continue;
					if (names[i].length() == 0) {
						err = "column label must be non-empty string";
						break;
					}
					if (pars.contains(names[i])) {
						err = String.format("identical string for different columns (%s)",names[i]);
						break;
					}
					pars.add(names[i]);
				}
				
				if (findIndex(names,info.getName(),true) < 0)
					throw new ReadingException("There is no " + info.getName() + " column in file: " + file.getName());
				
				state = ROWS;
				continue;
			}
			
			if (state == ROWS) {
				if (line.length() == 0) {
					state = EMPTY_LINES2;
					continue;
				}
				
				final String[] values = Utils.parseCSVLine(line,delimiter,'"',Utils.EXC_QUOTE);
				if (values.length < 3) continue;
				if (values.length > names.length)
					throw new ReadingException("Inconsistent input data lines - too many values");
				value = parseValue(values[0]);
				type = parseType(values[0]);
				if (!Number.class.isAssignableFrom(type)) continue;
				
				value = parseValue(values[stepIndex]);
				type = parseType(values[stepIndex]);
				if (!Number.class.isAssignableFrom(type)) continue;
				
				final double tick = Double.valueOf(value);
				final ResultValueInfo rvi = getResultValueInfo(info,parameterCombination,names,values,tick);
				if (rvi != null)
					result.add(rvi);
			}
			
			if (state == EMPTY_LINES2 && line.length() == 0) break;
		} while (line != null && err == null);
		
		if (err != null) {
			try { reader.close(); } catch (final IOException _) {}
			throw new ReadingException(err); 
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private int findIndex(final String[] array, final String element, final boolean caseSensitive) {
		for (int i = 0;i < array.length;++i) {
			if (caseSensitive) {
				if (array[i].equals(element)) 
					return i;
			} else {
				if (array[i].equalsIgnoreCase(element)) 
					return i;
			}
		}
		return -1;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void trimAll(String[] array) {
		if (array != null) {
			for (int i = 0;i < array.length;++i) {
				final String tmp = array[i].trim();
				array[i] = tmp;
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private String parseValue(final String valueStr) {
		String value = null;
		if (valueStr != null) {
			String str = valueStr.trim();
			str = str.replaceFirst("^\"+","");
			str = str.replaceFirst("\"+$","");
			try {
				final Number dValue = Double.valueOf(str);
				final double xValue = dValue.doubleValue();
				if (dValue.intValue() == xValue) 
					value = Integer.valueOf(dValue.intValue()).toString();
				else if (Long.MIN_VALUE <= xValue && xValue <= Long.MAX_VALUE && str.indexOf('.') < 0) 
					value = Long.valueOf(str).toString();
				else
					value = dValue.toString();
			} catch (NumberFormatException e) {
				final String lows = str.toLowerCase(Locale.US);
				if (lows.equals(Boolean.TRUE.toString()) || lows.equals(Boolean.FALSE.toString())) 
					value = lows;
				else 
					value = str;
			}
		}
		return value;
	}
	
	//----------------------------------------------------------------------------------------------------
	private Class<?> parseType(final String valueStr) {
		Class<?> type = null;
		if (valueStr != null) {
			String str = valueStr.trim();
			str = str.replaceFirst("^\"+","");
			str = str.replaceFirst("\"+$","");
			try {
				final Number dValue = Double.valueOf(str);
				final double xValue = dValue.doubleValue();
				if (dValue.intValue() == xValue) 
					type = Integer.class;
				else if (Long.MIN_VALUE <= xValue && xValue <= Long.MAX_VALUE && str.indexOf('.') < 0 ) 
					type = Long.class;
				else
					type = Double.class;
			} catch (NumberFormatException e) {
				str = str.toLowerCase(Locale.US);
				if (str.equals(Boolean.TRUE.toString()) || str.equals(Boolean.FALSE.toString())) 
					type = Boolean.class;
				else 
					type = String.class;
			}
		}
		return type;
	}
	
	//----------------------------------------------------------------------------------------------------
	private ResultValueInfo getResultValueInfo(final RecordableInfo info, final List<ParameterInfo<?>> parameterCombination, final String[] names, final String[] values,
											   final double tick) throws ReadingException {
		for (final ParameterInfo<?> p : parameterCombination) {
			final int idx = findIndex(names,p.getName(),true);
			if (idx == -1) 
				throw new ReadingException("cannot find parameter " + p.getName());
			final String valueStr = parseValue(values[idx]);
			final Class<?> type = p.getDefaultValue().getClass();
			if (!getValue(p.getValues().get(0).toString(),type).equals(getValue(valueStr,type))) return null;
		}
		final int idx = findIndex(names,info.getName(),true); // idx != -1 we checked this before
		final Object value = getValue(parseValue(values[idx]),info.getType());
		return new ResultValueInfo(info,value,tick);
	}
	
	//-------------------------------------------------------------------------------
	private Object getValue(final String value, final Class<?> type) {
		if (Byte.TYPE.equals(type) || Byte.class.equals(type))
			return getByteValue(value.trim());
		if (Short.TYPE.equals(type) || Short.class.equals(type))
			return getShortValue(value.trim());
		if (Integer.TYPE.equals(type) || Integer.class.equals(type))
			return getIntegerValue(value.trim());
		if (Long.TYPE.equals(type) || Long.class.equals(type))
			return getLongValue(value.trim());
		if (Float.TYPE.equals(type) || Float.class.equals(type))
			return getFloatValue(value.trim());
		if (Double.TYPE.equals(type) || Double.class.equals(type))
			return getDoubleValue(value.trim());
		if (Boolean.TYPE.equals(type) || Boolean.class.equals(type))
			return Boolean.valueOf(value.trim()); 
		if (String.class.equals(type))
			return value;
		if (unknown.class.equals(type)) {
			final Class<?> clazz = parseType(value);
			if (clazz != null)
				return getValue(value,clazz);
		}
		return null;
	}
	
	//-------------------------------------------------------------------------------
	private Byte getByteValue(final String value) {
		try {
			return Byte.valueOf(value);
		} catch (final NumberFormatException _) {
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	private Short getShortValue(final String value) {
		try {
			return Short.valueOf(value);
		} catch (final NumberFormatException _) {
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	private Integer getIntegerValue(final String value) {
		try {
			return Integer.valueOf(value);
		} catch (final NumberFormatException _) {
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	private Long getLongValue(final String value) {
		try {
			return Long.valueOf(value); 
		} catch (final NumberFormatException _) {
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	private Float getFloatValue(final String value) {
		try {
			return new Float(value);
		} catch (final NumberFormatException _) {
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	private Double getDoubleValue(final String value) {
		try {
			return new Double(value);
		} catch (final NumberFormatException _) {
			return null;
		}
	}
}
