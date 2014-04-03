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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ai.aitia.meme.paramsweep.batch.IParameterSweepResultReader;
import ai.aitia.meme.paramsweep.batch.ReadingException;
import ai.aitia.meme.paramsweep.batch.ResultValueInfo;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;
import ai.aitia.meme.utils.Utils;

/** The class represents a parser that parse a Repast result file and loads its content
 *  to the data base.
 */
public class IntelliSweepRepastResultParser implements IParameterSweepResultReader {
	
	/** The Repast result file object. */
	private File file = null;
	private String delimiter = null;

	//=========================================================================
	// Constructor
	
	//----------------------------------------------------------------------------------------------------
	public IntelliSweepRepastResultParser(List<RecorderInfo> recorders) {
		file = recorders.get(0).getOutputFile();
		delimiter = recorders.get(0).getDelimiter();
	}
	
	//=========================================================================
	// Public methods
	
	//----------------------------------------------------------------------------------------------------
	public List<ResultValueInfo> getResultValueInfos(RecordableInfo info, List<ParameterInfo<?>> parameterCombination) throws ReadingException {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			return read(reader,info,parameterCombination);
		} catch (FileNotFoundException e) {
			throw new ReadingException(file.getName() + " is not exist");
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
	private List<ResultValueInfo> read(BufferedReader reader, RecordableInfo info, List<ParameterInfo<?>> parameterCombination) throws ReadingException {
		List<ResultValueInfo> result = new ArrayList<ResultValueInfo>();
		String[] names = null;

		final int	HEADER			= 1,
					EMPTY_LINES1	= 2,
					COLNAMES		= 3,
					ROWS			= 4,
					EMPTY_LINES2	= 5;
		String line	= null;
		int	state = HEADER;
		String err = null;		

		String value = null;
		Class<?> type = null;

		do {
			try {
				line = reader.readLine();
			} catch (IOException e) {
				throw new ReadingException(e);
			}
			if (line == null) break;
			line = line.trim();

			if (state < EMPTY_LINES1) {
				if (line.length() == 0) 
					state = EMPTY_LINES1;
				continue;
			}
			
			if (state == EMPTY_LINES1) {
				if (line.length() == 0) continue;
				state = COLNAMES;
			}
			
			if (state == COLNAMES) {
				line = line.replaceFirst("^\\s","");
				boolean ok = ( line.length() >= 4 );
				if( ok ){
					String low = line.toLowerCase();
					if (line.charAt(0) == '"') {
						ok = low.startsWith("\"run\"") && line.length() >= 6;
						if (ok) {
							int i = line.indexOf('"',5);
							ok = (i > 5);
						}
					} else {
						ok = low.startsWith("run");
						if (ok) {
							int i = low.indexOf( "tick", 3 );
							ok = ( i > 3 );
						}
					}
				}

				names = ok ? Utils.parseCSVLine(line,delimiter,'"',0) : null;
				ok = (names != null && names.length >= 3);
				if (!ok) {
					err = "missing output columns";
					break;
				}
				ok = (names[0].equalsIgnoreCase("run") && names[1].equalsIgnoreCase("tick") );
				if (!ok) {
					err = "invalid column labels";
					break;
				}
				List<String> pars = new ArrayList<String>();
				for (int i = 2;i < names.length && err == null;++i) {
					if (names[i].length() == 0) {
						err = "column label must be non-empty string";
						break;
					}
					if (pars.contains(names[i])) {
						err = String.format("identical name for different columns (%s)", names[i]);
						break;
					}
					pars.add(names[i]);
				}
				if (find(names,info.getName()) < 0)
					throw new ReadingException("There is no " + info.getName() + " column in file: " + file.getName());
				state = ROWS;
				continue;
			}

			if (state == ROWS) {
				if (line.length() == 0) {
					state = EMPTY_LINES2;
					continue;
				}
				if (!",".equals(delimiter))
					line = line.replace(',','.');
				String[] values = Utils.parseCSVLine(line,delimiter,'"',0);
				if (values.length < 3) continue;
				if (values.length > names.length) 
					throw new ReadingException("Inconsistent input in data lines - too many values");
				value = parseValue(values[0]);
				type = parseType(values[0]);
				if (!(Number.class.isAssignableFrom(type))) continue;
				
				value = parseValue(values[1]);
				type = parseType(values[1]);
				if (!(Number.class.isAssignableFrom(type))) continue;

				double tick = Double.valueOf(value);
				ResultValueInfo rvi = getResultValueInfo(info,parameterCombination,names,values,tick);
				if (rvi != null)
					result.add(rvi);
			}

			if (state == EMPTY_LINES2 && line.length() == 0) break;
		} while (line != null && err == null);

		if (err != null) {
			try { reader.close(); } catch (IOException e) {}
			throw new ReadingException(err); 
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private int find(String[] array, String element) {
		for (int i = 0;i < array.length;++i) {
			if (array[i].equals(element))
				return i;
		}
		return -1;
	}
	
	//----------------------------------------------------------------------------------------------------
	private ResultValueInfo getResultValueInfo(RecordableInfo info, List<ParameterInfo<?>> parameterCombination, String[] names, String[] values,
											   double tick) {
		for (ParameterInfo p : parameterCombination) {
			int idx = find(names,p.getName());
			if (idx == -1) continue; // constant parameter
			//Class<?> type = parseType(values[idx]);
			String valueStr = parseValue(values[idx]);
			Class<?> type = p.getDefaultValue().getClass();
			if (!getValue(p.getValues().get(0).toString(),type).equals(getValue(valueStr,type))) return null;
		}
		int idx = find(names,info.getName()); // idx != -1 we checked this before
		Object value = getValue(parseValue(values[idx]),info.getType());
		return new ResultValueInfo(info,value,tick);
	}
	
	
	//----------------------------------------------------------------------------------------------------
	private Class<?> parseType(String valueStr) {
		Class<?> type = null;
		if (valueStr != null) {
			String s = valueStr.trim();
			try {
				Number d = Double.valueOf(s);
				double x = d.doubleValue();
				if (d.intValue() == x) 
					type = Integer.class;
				else if (Long.MIN_VALUE <= x && x <= Long.MAX_VALUE && s.indexOf('.') < 0 ) 
					type = Long.class;
				else
					type = Double.class;
			} catch (NumberFormatException e) {
				s = s.toLowerCase();
				if (s.equals(Boolean.TRUE.toString())) 
					type = Boolean.class;
				else if (s.equals(Boolean.FALSE.toString())) 
					type = Boolean.class;
				else 
					type = String.class;
			}
		}
		return type;
	}

	//----------------------------------------------------------------------------------------------------
	private String parseValue(String valueStr) {
		String value = null;
		if (valueStr != null) {
			String s = valueStr.trim();
			try {
				Number d = Double.valueOf(s);
				double x = d.doubleValue();
				if (d.intValue() == x) 
					value = Integer.valueOf(d.intValue()).toString();
				else if (Long.MIN_VALUE <= x && x <= Long.MAX_VALUE && s.indexOf('.') < 0) 
					value = Long.valueOf(s).toString();
				else
					value = d.toString();
			} catch (NumberFormatException e) {
				s = s.toLowerCase();
				if (s.equals(Boolean.TRUE.toString()) || s.equals(Boolean.FALSE.toString())) 
					value = s;
				else 
					value = valueStr;
			}
		}
		return value;
	}
	
	//-------------------------------------------------------------------------------
	private Object getValue(String value, Class<?> type) {
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
			return new Boolean(value.trim());
		if (String.class.equals(type))
			return value;
		return null;
	}
	
	//-------------------------------------------------------------------------------
	private Byte getByteValue(String value) {
		try {
			return new Byte(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	private Short getShortValue(String value) {
		try {
			return new Short(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	private Integer getIntegerValue(String value) {
		try {
			return new Integer(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	private Long getLongValue(String value) {
		try {
			return new Long(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	private Float getFloatValue(String value) {
		try {
			return new Float(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
	
	//-------------------------------------------------------------------------------
	private Double getDoubleValue(String value) {
		try {
			return new Double(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
