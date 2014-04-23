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
package ai.aitia.meme.csvImport;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.database.IResultsDbMinimal;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ParameterComb;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.ResultInMem;
import ai.aitia.meme.database.ColumnType.ValueNotSupportedException;
import ai.aitia.meme.pluginmanager.Parameter;
import ai.aitia.meme.utils.Utils;

/** The class represents a parser that parse a CSV file and loads its content to the
 *  data base.
 */
public class CSVResultParser {
	
	//================================================================================
	// nested classes
	
	//--------------------------------------------------------------------------------
	/** Record class for representing runs. */
	static class Run {
		/** Id of the run. */
		int	run;
		/** Rows belongs to the run. */ 
		ArrayList<Result.Row> rows	= new ArrayList<Result.Row>();
		Run(int r) { run = r; }
	};
	
	//---------------------------------------------------------------------------------
	/** Exception class for representing warnings and errors occured by the advanced
	 *  CSV parser.
	 */
	public static class AdvancedCSVParserException extends Exception {
		private static final long serialVersionUID = 1L;
		/** Flag that determines whether the exception is a warning or an error. */
		private boolean warning;
		public AdvancedCSVParserException() { super(); this.warning = false; }
		public AdvancedCSVParserException(String message, boolean warning) { 
			super(message);
			this.warning = warning;
		}
		public AdvancedCSVParserException(String message, Throwable cause, boolean warning) { 
			super(message,cause);
			this.warning = warning;
		}
		public AdvancedCSVParserException(Throwable cause, boolean warning) {
			super(cause);
			this.warning = warning;
		}
		public boolean isWarning() { return warning; }
	}
	
	//----------------------------------------------------------------------------------
	/** Class for parsing lines that uses advanced CSV format (or aCSV lines). */
	public class AdvancedCSVParser {
		
		//==============================================================================
		// members
		
		/** Pattern of the prefix of aCSV lines; */
		Pattern prefix = null;
		/** Pattern of the suffix of aCSV lines; */
		Pattern suffix = null;
		/** List of delimiter patterns (in string format) that separates values in aCSV lines. */
		List<String> delimiters = null;
		/** Scanf-like pattern of values in aCSV lines. */
		String valuePattern = null;
		
		/** Pattern of the prefix of aCSV header; */
		Pattern headerPrefix = null;
		/** Pattern of the suffix of aCSV header; */
		Pattern headerSuffix = null;
		/** List of delimiter patterns (in string format) that separates values in aCSV header. */
		List<String> headerDelimiters = null;
		/** Scanf-like pattern of values in aCSV header. */
		String headerValuePattern = null;
		
		/** List of supported scanf-like type representing characters. */
		private List<Character> acceptableTypeCharacters = new ArrayList<Character>(6);
		{
			acceptableTypeCharacters.add('c');
			acceptableTypeCharacters.add('d');
			acceptableTypeCharacters.add('o');
			acceptableTypeCharacters.add('x');
			acceptableTypeCharacters.add('s');
			acceptableTypeCharacters.add('f');
		}
		
		//=============================================================================
		// methods
		
		//-----------------------------------------------------------------------------
		/** Constructor.
		 * @param pattern the full pattern of an aCSV line
		 * @throws AdvancedCSVParserException if the pattern is invalid
		 */
		public AdvancedCSVParser(String pattern) throws AdvancedCSVParserException {
			if (pattern == null)
				throw new IllegalArgumentException("'pattern' cannot be 'null'.");
			String _pattern = findPrefix(pattern,false);
			_pattern = findSuffix(_pattern,false);
			init(_pattern,false);
		}

		//-----------------------------------------------------------------------------
		/** Constructor.
		 * @param pattern the full pattern of an aCSV line
		 * @param headerPattern the full pattern of the aCSV header
		 * @throws AdvancedCSVParserException if any of the patterns is invalid
		 */
		public AdvancedCSVParser(String pattern, String headerPattern) throws AdvancedCSVParserException {
			this(pattern);
			if (headerPattern != null) {
				String _pattern = findPrefix(headerPattern,true);
				_pattern = findSuffix(_pattern,true);
				init(_pattern,true);
			}
		}
		
		//-----------------------------------------------------------------------------
		/** Parses a line by the stored patterns.
		 * @param line a line
		 * @return the values contained by the line in string format
		 * @throws AdvancedCSVParserException if the line is ill-formed
		 */
		public String[] parseLine(String line) throws AdvancedCSVParserException {
			String _line = line;
			if (prefix != null) {
				Matcher m = prefix.matcher(_line);
				if (!m.find() || m.start() != 0)
					throw new AdvancedCSVParserException("Prefix doesn't match",true);
				_line = _line.substring(m.end());
			}
			
			if (suffix != null) {
				Matcher m = suffix.matcher(_line);
				if (!m.find())
					throw new AdvancedCSVParserException("Suffix doesn't match",true);
				while (m.end() != _line.length()) {
					if (!m.find())
						throw new AdvancedCSVParserException("Suffix doesn't match",true);
				}
				_line = _line.substring(0,m.start());
			}
			
			List<String> parts = new ArrayList<String>();
			String tmp = _line;
			for (String delimiter : delimiters) {
				String[] tmpArray = tmp.split(delimiter,2);
				if (tmpArray.length == 1) break;
				parts.add(tmpArray[0]);
				tmp = tmpArray[1];
			}
			parts.add(tmp);
			
			if (parts.size() << 1 < valuePattern.length())
				throw new AdvancedCSVParserException("Missing data",true);
			
			List<String> ans = new ArrayList<String>(parts.size());
			for (int i = 0;i < parts.size();++i) {
				if (parts.get(i).equals(settings.nullToken))
						ans.add(parts.get(i));
				else {
					char type = valuePattern.charAt(2*i+1);
					checkType(parts.get(i),type);
					ans.add(parts.get(i));
				}
			}
			return ans.toArray(new String[0]);
		}
		
		//-----------------------------------------------------------------------------
		/** Parses the header line by the stored patterns.
		 * @param line the header line
		 * @return the values contained by the header in string format
		 * @throws AdvancedCSVParserException if the header is ill-formed
		 */
		public String[] parseHeaderLine(String line) throws AdvancedCSVParserException {
			String _line = line;
			if (headerPrefix != null) {
				Matcher m = headerPrefix.matcher(_line);
				if (!m.find() || m.start() != 0)
					throw new AdvancedCSVParserException("Header prefix doesn't match",false);
				_line = _line.substring(m.end());
			}
			
			if (headerSuffix != null) {
				Matcher m = headerSuffix.matcher(_line);
				if (!m.find())
					throw new AdvancedCSVParserException("Header suffix doesn't match",false);
				while (m.end() != _line.length()) {
					if (!m.find())
						throw new AdvancedCSVParserException("Header suffix doesn't match",false);
				}
				_line = _line.substring(0,m.start());
			}
			
			List<String> parts = new ArrayList<String>();
			String tmp = _line;
			for (String delimiter : headerDelimiters) {
				String[] tmpArray = tmp.split(delimiter,2);
				if (tmpArray.length == 1) break;
				parts.add(tmpArray[0]);
				tmp = tmpArray[1];
			}
			parts.add(tmp);
			
			if (parts.size() << 1 < headerValuePattern.length())
				throw new AdvancedCSVParserException("Missing data",false);
			
			List<String> ans = new ArrayList<String>(parts.size());
			for (int i = 0;i < parts.size();++i) {
				if (parts.get(i).equals(settings.nullToken))
						ans.add(parts.get(i));
				else {
					char type = headerValuePattern.charAt(2*i+1);
					checkType(parts.get(i),type);
					ans.add(parts.get(i));
				}
			}
			return ans.toArray(new String[0]);
		}
		
		//-----------------------------------------------------------------------------
		/** Checks whether the <code>value</value> can be cast to the type specified by
		 *  <code>type</code>.  
		 * @param value a value in string format
		 * @param type a scanf-like type representing character
		 * @throws AdvancedCSVParserException if the casting operation is failed
		 */
		private void checkType(String value, char type) throws AdvancedCSVParserException {
			char _type = Character.toLowerCase(type);
			switch(_type) {
 			case 'c' : if (value.length() > 1)
 							throw new AdvancedCSVParserException(String.format("%s is not a character",value),true);
 					   break;
 			case 'd' : try { Long.parseLong(value); } 
 					   catch (NumberFormatException e) { 
 						   throw new AdvancedCSVParserException(String.format("%s is not a valid integer",value),true);
 					   }
 					   break;
 			case 'o' : try { Long.parseLong(value,8); }
 					   catch (NumberFormatException e) {
 						   throw new AdvancedCSVParserException(String.format("%s is not a valid octal integer",value),true);
 					   }
 					   break;
 			case 'x' : try { Long.parseLong(value,16); }
 					   catch (NumberFormatException e) {
 						   throw new AdvancedCSVParserException(String.format("%s is not a valid hexadecimal integer",value),true);
 					   }
 					   break;
 			case 'f' : try { Double.parseDouble(value); }
 					   catch (NumberFormatException e) {
 						   throw new AdvancedCSVParserException(String.format("%s is not a valid real value",value),true);
 					   }
			}
		}
		
		//--------------------------------------------------------------------------------
		/** Finds and sets the prefix of the full pattern <code>pattern</code>. 
		 * @param pattern the full pattern
		 * @param header flag that determines whether <code>pattern</code> is a pattern of
		 *        a header line or a ordinary line
		 * @return the pattern without the prefix
		 * @throws AdvancedCSVParserException if the pattern is invalid
		 */
		private String findPrefix(String pattern, boolean header) throws AdvancedCSVParserException {
			String _pattern = pattern.replaceAll("\\\\%","__<percent>__");
			int index = _pattern.indexOf('%');
			if (index == -1) 
				throw new AdvancedCSVParserException("Invalid pattern: missing % sign",false);
			if (_pattern.length() == 1)
				throw new AdvancedCSVParserException("Invalid pattern: missing type character",false);
			if (!acceptableTypeCharacters.contains(Character.toLowerCase(_pattern.charAt(index+1))))
				throw new AdvancedCSVParserException("Invalid pattern: not supported type character: " + _pattern.charAt(index+1),false);
			if (index == 0) {
				if (header)
					headerPrefix = null;
				else
					prefix = null;
				return pattern;
			}
			if (header)
				headerPrefix = Pattern.compile(_pattern.substring(0,index).replaceAll("__<percent>__","\\\\%"));
			else
				prefix = Pattern.compile(_pattern.substring(0,index).replaceAll("__<percent>__","\\\\%")); 
			return _pattern.substring(index).replaceAll("__<percent>__","\\\\%");
		}
		
		//--------------------------------------------------------------------------------
		/** Finds and sets the suffix of the full pattern <code>pattern</code>. 
		 * @param pattern the full pattern
		 * @param header flag that determines whether <code>pattern</code> is a pattern of
		 *        a header line or a ordinary line
		 * @return the pattern without the suffix
		 * @throws AdvancedCSVParserException if the pattern is invalid
		 */
		private String findSuffix(String pattern, boolean header) throws AdvancedCSVParserException {
			String _pattern = pattern.replaceAll("\\\\%","__<percent>__");
			int index = _pattern.lastIndexOf('%');
			if (index == -1) 
				throw new AdvancedCSVParserException("Invalid pattern: missing % sign",false);
			if (_pattern.length() == 1 || index == _pattern.length() - 1)
				throw new AdvancedCSVParserException("Invalid pattern: missing type character",false);
			if (!acceptableTypeCharacters.contains(Character.toLowerCase(_pattern.charAt(index+1))))
				throw new AdvancedCSVParserException("Invalid pattern: not supported type character: " + _pattern.charAt(index+1),false);
			if (index >= _pattern.length()-2) {
				if (header)
					headerSuffix = null;
				else
					suffix = null;
				return pattern;
			}
			if (header)
				headerSuffix = Pattern.compile(_pattern.substring(index+2).replaceAll("__<percent>__","\\\\%"));
			else
				suffix = Pattern.compile(_pattern.substring(index+2).replaceAll("__<percent>__","\\\\%")); 
			return _pattern.substring(0,index+2).replaceAll("__<percent>__","\\\\%");
		}
		
		//------------------------------------------------------------------------------
		/** Collects the delimiters and sets the value pattern.
		 * @param pattern the full pattern
		 * @param header flag that determines whether <code>pattern</code> is a pattern of
		 *        a header line or a ordinary line
		 * @throws AdvancedCSVParserException if the pattern is invalid
		 */
		private void init(String pattern, boolean header) throws AdvancedCSVParserException {
			String _pattern = pattern.replaceAll("\\\\%","__<percent>__");
			String[] parts = _pattern.split("%.");
			if (header)
				headerDelimiters = new ArrayList<String>(parts.length-1);
			else
				delimiters = new ArrayList<String>(parts.length-1);
			List<String> tmpList = header ? headerDelimiters : delimiters;
			for (int i = 1;i < parts.length;++i) {
				String tmp = parts[i].replaceAll("__<percent>__","%");
				tmpList.add(tmp);
			}
			
			String value = "";
			int index = -1;
			do {
				index = _pattern.indexOf('%',index+1);
				if (index != -1) {
					if (index == _pattern.length()-1)
						throw new AdvancedCSVParserException("Invalid pattern: missing type character",false);
					if (!acceptableTypeCharacters.contains(Character.toLowerCase(_pattern.charAt(index+1))))
						throw new AdvancedCSVParserException("Invalid pattern: not supported type character: " + _pattern.charAt(index+1),false);
					value += _pattern.substring(index,index+2);
				}
			} while (index != -1);
			if ("".equals(value))
				throw new AdvancedCSVParserException("Invalid pattern: missing % sign.",false);
			if (header)
				headerValuePattern = value;
			else
				valuePattern = value;
		}
	}
	
	//================================================================================
	// contants
	public final int ONE_RUN_CONSTANT = 0;
	public final int VARIABLE = 1;

	//================================================================================
	// variables
	
	/** The CSV file object. */
	File file = null;
	/** The CSV settings. */
	CSVImportSettingsDialog settings;
	/** The model object. */
	Model model = null;
	/** The start time of the model. */
	long startTime = 0;
	/** The end time of the model. */
	long endTime = 0;
	/** Columns informations. */
	Columns parameters = null;
	/** Flag that determines whether generates the column names or it is not necessary. */
	boolean generatedParameterNames = false;
	int[] inputStatus;
	/** List of runs. */
	ArrayList<Run> runs = null;
	/** The maximum number of rows of a run. */
	int maxRowsPerRun = 0;
	int warnings = 0;
	
	//================================================================================
	// methods
	
	public CSVResultParser(CSVImportSettingsDialog settings, File file) {
		this.settings = settings;
		this.file = file;
	}
	
	//--------------------------------------------------------------------------------
	public CSVResultParser(CSVImportSettingsDialog settings) {
		this.settings = settings;
	}
	
	//--------------------------------------------------------------------------------
	// [Model] thread
	/** Reads the CSV file to the memory. */
	public void readFile() throws Exception {
		model = null;
		startTime = 0;
		
		parameters = new Columns();
		runs = new ArrayList<Run>();
		maxRowsPerRun = 0;

		String err = null;
		int tickIndex = -1;
		ColumnType.TypeAndValue tv = null;

		if (file == null)  file = CSVImportSettingsDialog.lastFile;
		if (file == null || !file.exists() || file.isDirectory()) 
			throw new Exception("Cannot find file");
		
		startTime = file.lastModified(); // instead timestamp
		endTime = startTime; //TODO: need better solution  
		java.io.FileInputStream is = new java.io.FileInputStream(file);
		long fSize = is.available();
		int buffSize = (int)Math.min(Math.max(fSize/100,80),8192);
		java.io.BufferedReader in = new java.io.BufferedReader(new InputStreamReader(is),buffSize);
		
		String line = null;
		int lineno = 0;
		MEMEApp.LONG_OPERATION.progress(0,is.available());
		boolean needPreProcess = !settings.isAdvancedCSVType && (settings.delimiters.size() > 1 || settings.mergeConsecutive);
		
		AdvancedCSVParser advParser = null;
		if (settings.isAdvancedCSVType)
			advParser = new AdvancedCSVParser(settings.linePattern,
											  settings.containsHeader ? settings.headerPattern : null);
		
		// I. ignore rows (if need)
		if (settings.isIgnore) {
			for (int i=0;i<settings.ignoreNumber;++i) {
				MEMEApp.LONG_OPERATION.progress(is.getChannel().position());
				in.readLine();
				lineno++;
			}
		}
		// II. reading headers (if need) 
		if (settings.containsHeader) {
			do {
				line = in.readLine();
				if (line == null) break;
				line = line.trim();
				lineno++;
			} while (line.startsWith(settings.comment));
			if (line == null || line.equals("")) {
				err = "missing column names and data";
			} else {
				String[] names = null;
				if (settings.isAdvancedCSVType)
					names = advParser.parseHeaderLine(line);
				else {
					if (needPreProcess)
						line = preprocessCSVLine(line,settings.delimiters,settings.mergeConsecutive);
					names = Utils.parseCSVLine(line,settings.delimiters.get(0),settings.quote.charAt(0),Utils.EXC_QUOTE);
				}
				if (names == null) err = "invalid column labels";
				else {
					for (int i=0;i<names.length;++i) {
						if (names[i].length() == 0) {
							err = "column labels must be non-empty string";
							break;
						}
						if (settings.runStrategy == CSVImportSettingsDialog.USE_TICK &&
						   (tickColumn() == i || settings.tickColumn.equals(names[i]))) {
							tickIndex = i;
							continue; // skip the 'tick' column
						}
						if (parameters.contains(names[i])) {
							err = String.format("identical names for different columns: %s",names[i]);
							break;
						}
						parameters.append(names[i],null);
					}
					if (settings.runStrategy == CSVImportSettingsDialog.USE_TICK && tickIndex == -1) {
						err = "invalid 'tick' column";
					}
				}
			}
		}
		
		if (err != null)
			throw new Exception(String.format("in line %d: %s",lineno,err));

		boolean needGeneratedNames = generatedParameterNames = !settings.containsHeader;
		int tick = 1;
		int run = 1;
		double lastTickValue = -1;
		// III. reading lines
		do {
			MEMEApp.LONG_OPERATION.progress(is.getChannel().position());
			
			line = in.readLine(); if (line == null) break;
			line = line.trim();
			lineno++;
			
			if (line.equals("") || line.startsWith(settings.comment)) continue;
			String values[] = null;
			if (settings.isAdvancedCSVType) {
				try {
					values = advParser.parseLine(line);
				} catch (AdvancedCSVParserException e) {
					if (e.isWarning()) {
						warnings++;
						ai.aitia.meme.Logger.logError("Warning: %s: ignoring line %d",e.getLocalizedMessage(),lineno);
						continue;
					}
					throw e;
				}
			} else {
				if (needPreProcess)
					line = preprocessCSVLine(line,settings.delimiters,settings.mergeConsecutive);
				values = Utils.parseCSVLine(line,settings.delimiters.get(0),settings.quote.charAt(0),Utils.EXC_QUOTE | Utils.FORCE_EMPTY);
			}
			if (needGeneratedNames) { // we read the first data line
				int nrOfColumns = values.length;
				if (settings.runStrategy == CSVImportSettingsDialog.USE_TICK) {
					tickIndex = tickColumn();
					if (tickIndex == -1 || tickIndex >= nrOfColumns) {
						err = String.format("wrong tick index: %d",tickIndex);
						break;
					}
					nrOfColumns--; //skip the 'tick' column
				}
				for (int i=0;i<nrOfColumns;++i) parameters.append("Column"+i,null);
				needGeneratedNames = false;
			}
			
			if (values.length < parameters.size()) {
				err = "not enough data";
				break;
			}

			switch(settings.runStrategy) {
			case CSVImportSettingsDialog.ONE_RUN:
				{
					if (runs.isEmpty()) runs.add(new Run(1));
					Run lastRun = runs.get(0);
					Result.Row row = new Result.Row(parameters,tick++);
					for (int i=0;i<values.length;++i) {
						if (values[i].equals(settings.nullToken)) values[i] = null;
						tv = ColumnType.parseValue(values[i]);
						if (i >= parameters.size()) {
							err = "more data in the record than column";
							break;
						}
						assert i < parameters.size();
						row.set(i,tv.getValue());
						parameters.get(i).extendType(tv.getType());
					}
					lastRun.rows.add(row);
					if (lastRun.rows.size() > maxRowsPerRun) maxRowsPerRun = lastRun.rows.size();
					break;
				}
			case CSVImportSettingsDialog.RECORD_PER_RUN:
				{
					if (runs.isEmpty()) runs.add(new Run(1));
					Run lastRun = runs.get(run-1);
					Result.Row row = new Result.Row(parameters,tick++);
					if (tick == settings.recordsPerRun+1) {
						tick = 1;
						runs.add(new Run(++run));
					}
					for (int i=0;i<values.length;++i) {
						if (values[i].equals(settings.nullToken)) values[i] = null;
						tv = ColumnType.parseValue(values[i]);
						if (i >= parameters.size()) {
							err = "more data in the record than column";
							break;
						}
						assert i < parameters.size();
						row.set(i,tv.getValue());
						parameters.get(i).extendType(tv.getType());
					}
					lastRun.rows.add(row);
					if (lastRun.rows.size() > maxRowsPerRun) maxRowsPerRun = lastRun.rows.size();
					break;
				}
			case CSVImportSettingsDialog.USE_TICK:
				{
					if (runs.isEmpty()) runs.add(new Run(1));
					try {
						double currentTickValue = Double.parseDouble(values[tickIndex]);
						if (currentTickValue <= lastTickValue) {
							tick = 1;
							runs.add(new Run(++run));
						}
						lastTickValue = currentTickValue;
						Run lastRun = runs.get(run-1);
						Result.Row row = new Result.Row(parameters,tick++);
						for (int i=0;i<values.length;++i) {
							if (i == tickIndex) continue;
							if (values[i].equals(settings.nullToken)) values[i] = null;
							tv = ColumnType.parseValue(values[i]);
							if (i<tickIndex) {
								if (i >= parameters.size()) {
									err = "more data in the record than column";
									break;
								}
								assert i < parameters.size();
								row.set(i,tv.getValue());
								parameters.get(i).extendType(tv.getType());
							} else if (i>tickIndex) {
								if (i-1 >= parameters.size()) {
									err = "more data in the record than column";
									break;
								}
								assert i-1 < parameters.size();
								row.set(i-1,tv.getValue());
								parameters.get(i-1).extendType(tv.getType());
							}
						}
						lastRun.rows.add(row);
						if (lastRun.rows.size() > maxRowsPerRun) maxRowsPerRun = lastRun.rows.size();
					} catch (IndexOutOfBoundsException e) {
						err = "missing 'tick' value";
						break;
					} catch (NumberFormatException e) {
						err = "'tick' value is not a number";
						break;
					}
				}
			}
			
		} while (line != null && err == null);

		if (err != null) {
			in.close();
			is.close();
			throw new Exception(String.format("in line %d: %s", lineno, err)); 
		}

		if (runs.size() == 0) throw new Exception("Empty file.");
		
		if (runs.get(runs.size()-1).rows.isEmpty()) runs.remove(runs.size()-1);
		
		in.close();
		is.close();
		
		inputStatus = new int[parameters.size()];
		
		for (int col = 0; col <= parameters.size() - 1; ++col) {
			RUN:
			for (int r = runs.size() - 1; r >= 0; --r) {
				Run actRun = runs.get(r);
				assert !actRun.rows.isEmpty();
				Object val = actRun.rows.get(0).get(col);

				for (int j = actRun.rows.size() - 1; j >= 0; --j) {
					Result.Row row = actRun.rows.get(j);
					assert row.getColumns() == parameters;
					Object v = row.get(col);
					if ((v == null) ? (val != null) : !v.equals(val)) {
						inputStatus[col] = VARIABLE;
						break RUN;
					}
				}
			}
		}
		boolean variableColumn = false;
		for (int i=0;i<inputStatus.length;++i) {
			if (inputStatus[i] == VARIABLE) {
				variableColumn = true;
				break;
			}
		}
		if (!variableColumn) inputStatus[inputStatus.length-1] = VARIABLE;
		
		// set 'null' data type to int
		for (int i = 0;i < parameters.size();++i) {
			Parameter p = parameters.get(i);
			if (p.getDatatype() == null)
				p.setDatatype(ColumnType.INT);
		}
	}
	
	//-------------------------------------------------------------------------------
	// [Model] only
	/** Writes the content of the CVS file to the data base.
	 * @param db the database
	 */
	public void write(IResultsDbMinimal db, String modelName, String version, String batchDesc, String[] types) throws Exception {
		
		// Ensure that the GUI thread is waiting and making no changes in the Model   
		MEMEApp.LONG_OPERATION.showProgressNow();
		MEMEApp.LONG_OPERATION.progress(0, runs.size());

		Columns c = new Columns();
		Columns out = new Columns();
		for (int i=0;i<types.length;++i) {
			if (types[i].equals("Input")) c.append(parameters.get(i).getName(),parameters.get(i).getDatatype());
			else out.append(parameters.get(i).getName(),parameters.get(i).getDatatype());
		}
		GeneralRow inp = new GeneralRow(c);
		
		model = db.findModel(modelName, version);
		if (model == null)
			model = new Model(Model.NONEXISTENT_MODELID, modelName, version);

		// Generate batch number
		int b = db.getNewBatch(model);

		ResultInMem result = new ResultInMem();
		result.setModel(model);
		result.setBatch(b);
		result.setParameterComb(new ParameterComb(inp));

		// TODO: Using this.startTime and this.endTime here are inproper: 
		// these are the times of the whole batch, not the individual runs.
		// I use them because I cannot tell better. 
		result.setStartTime(startTime); 
		result.setEndTime(endTime);

		for (int i = 0; i < runs.size(); ++i) {
			MEMEApp.LONG_OPERATION.progress(i);				// display progress
			result.setRun(runs.get(i).run); // 
			ArrayList<Result.Row> rows = runs.get(i).rows;
			if (rows.isEmpty()) continue;

			result.resetRows(out);
			for (int j = 0; j < rows.size(); ++j) {
				Result.Row row = rows.get(j);
				Result.Row newrow = new Result.Row(out, row.getTick());
				int iindex = 0, oindex = 0;
				for (int l = 0; l < row.size(); ++l) {
					if (j==0 && types[l].equals("Input")) inp.set(iindex++,row.get(l));
					else if (types[l].equals("Output")) {
						newrow.set(oindex++,row.get(l));
					}
				}
				result.add(newrow);
			}
			try {
				db.addResult(result);
			} catch (final ValueNotSupportedException e) {
				warnings++;
				ai.aitia.meme.Logger.logError("Database warnings: see the error log for details.");
			}

			// free memory:
			runs.set(i, null);
		}
		runs.clear();

		if (batchDesc != null && batchDesc.length() > 0) {
			MEMEApp.LONG_OPERATION.setTaskName("Writing batch description...");
			db.storeBatchDescription(result.getModel().getModel_id(), result.getBatch(), batchDesc);
		}

	}
	
	//-------------------------------------------------------------------------------
//	public void test() {
//		System.out.println("startTime: " + DateFormat.getDateTimeInstance().format(new java.util.Date(startTime)));
//		
//		System.out.println("Columns:");
//		for (int i = 0; i < parameters.size(); ++i) {
//			System.out.println(String.format(
//					"[%d] %s {%s}", i, parameters.get(i),
//							parameters.get(i).getDatatype().getSQLTypeStr()
//					));
//		}
//
//		System.out.println("Rows:");
//		for (int i = 0; i < runs.size(); ++i) {
//			for (int j = 0; j < runs.get(i).rows.size(); ++j) {
//				StringBuilder row = new StringBuilder(String.format("run#%d,tick#%d", runs.get(i).run, runs.get(i).rows.get(j).getTick()));
//				for (int k = 0; k < runs.get(i).rows.get(j).size(); ++k) {
//					row.append(',');
//					row.append( runs.get(i).rows.get(j).getLocalizedString(k) );
//				}
//				System.out.println(row.toString());
//			}
//		}
//		
//		System.out.println("Inputstat:");
//		for (int i=0;i<inputStatus.length;++i) {
//			System.out.println(parameters.get(i).getName() + ": " + (inputStatus[i] == ONE_RUN_CONSTANT ? "may be input" : "output"));
//		}
//	}
	
	//-------------------------------------------------------------------------------
	/** Returns the index of the column that the parser uses as 'tick'. */
	private int tickColumn() {
		if (settings.runStrategy != CSVImportSettingsDialog.USE_TICK) return -2;
		int result = -1;
		try {
			result = Integer.parseInt(settings.tickColumn);
		} catch (NumberFormatException e) {}
		return result;
	}
	
	//-------------------------------------------------------------------------------
	public static String preprocessCSVLine(String line, List<String> delimiters, boolean mergeConsecutive) {
		if (delimiters.size() == 0)
			throw new IllegalArgumentException("'delimiters' must contain at least one delimiter.");
		String ans = line;
		final String usedDelimiter = delimiters.get(0);
		// step I: replace all delimiters to the used one.
		for (int i = 1;i < delimiters.size();++i)
			ans = ans.replaceAll(delimiters.get(i),usedDelimiter);
		// step II: contract successive delimiter (if necessary)
		if (mergeConsecutive) 
			ans = ans.replaceAll("(" + usedDelimiter + ")+",usedDelimiter);
		return ans;
	}
	
	//-------------------------------------------------------------------------------
	/** Releases the resources. */
	public void dispose() {
		settings = null;
		model = null;
		if (parameters != null) parameters.clear();
		parameters = null;
		if (runs != null) runs.clear();
		runs = null;
		inputStatus = null;
	}
}
