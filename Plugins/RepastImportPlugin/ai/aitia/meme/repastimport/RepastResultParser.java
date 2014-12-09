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
package ai.aitia.meme.repastimport;

import static ai.aitia.meme.utils.Utils.join;
import static ai.aitia.meme.utils.Utils.parseCSVLine;
import static ai.aitia.meme.utils.Utils.parseDateTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import sun.launcher.resources.launcher;
import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.database.IResultsDbMinimal;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ParameterComb;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.ResultInMem;
import ai.aitia.meme.database.Run;
import ai.aitia.meme.database.ColumnType.ValueNotSupportedException;
import ai.aitia.meme.gui.lop.UserBreakException;
import ai.aitia.meme.processing.ResultProcessingFrame;
import ai.aitia.meme.processing.ResultProcessingFrame.NoSuchPluginException;
import ai.aitia.meme.utils.Utils;

/** The class represents a parser that parse a Repast result file and loads its content
 *  to the data base.
 */
public class RepastResultParser {
	
	private static final int MAX_NUMBER_OF_DISPLAYABLE_WARNINGS = 100;
	
	/** The Repast result file object. */
	File[]					files;
	
	int fileIndex=0;
	/** The model object. */
	Model					model		= null;
	/** The start time of the model. */
	long					startTime	= 0;
	/** The end time of the model. */
	long					endTime		= 0;

	/** Type and value of the constant parameters. */ 
	GeneralRow				fixedPar	= null;
	/** Column informations about non constant columns. */ 
	Columns					otherPar	= null;
	ArrayList<Object>		canBeInput	= null; // potential input parameters in otherPar[0..canBeInput.size()-1]
	int						inputIndex  = -1;	// selected number of input parameters in otherPar[]
												// RepastImportDialog manages this value.
												// It must be within {-1..canBeInput.size()-1}
	/** List of runs. */
	ArrayList<Run>			runs		= null;
	/** The maximum number of rows of a run. */
	int						maxRowsPerRun = 0;
	
	/** Map containing maximum column numbers for each recordable (1 for non multicolumn recordables)*/
	LinkedHashMap<String, Integer> maxMultiColumnNumbers;
	ArrayList<LinkedHashMap<String, Integer>> actualMultiColumnNumbers;
	ArrayList<String> distinctLabels;
	ArrayList<ArrayList<Integer>> placeholderIndices = new ArrayList<ArrayList<Integer>>();
	ArrayList<String> multiColNames;
	
	/** Warning messages. */
	String					warnings	= "";
	
	/** true if input is a multicolumn fileset */
	private boolean isMultiCol = false;
//	boolean					dirty		= false;
	private boolean simphony = false;

	//=========================================================================
	// Constructor
	
	public RepastResultParser(File f, boolean simphony) {
		
		if (!f.exists() || f.isDirectory())
	    	throw new RuntimeException("Cannot find file"); 
		
		ArrayList<File> fileList = new ArrayList<File>();
		fileList.add(f);
		
		String fileName = f.getPath();
		int index = fileName.lastIndexOf('.');
		final String fileNamePrefix = fileName.substring(0, index);
		final String fileNameSuffix = fileName.substring(index);
		int partIndex = 1;
		
		File partFile = new File(fileNamePrefix+"-part"+partIndex+fileNameSuffix);
		
		while(partFile.exists())
		{
			fileList.add(partFile);
			partIndex++;
			partFile = new File(fileNamePrefix+"-part"+partIndex+fileNameSuffix);
		}
					
		
		this.files = fileList.toArray(new File[fileList.size()]);
		this.simphony = simphony;
	}

	
	//=========================================================================
	// Public methods

	//-------------------------------------------------------------------------
	/** Reads the Repast result file to the memory. */
	public void readFile() throws Exception {

		model		= null;
		startTime	= 0;
		endTime		= 0;

		fixedPar	= null;
		otherPar	= new Columns();
		canBeInput	= new ArrayList<Object>();
		inputIndex	= -1;
		runs		= new ArrayList<Run>();
		maxRowsPerRun= 0;
		warnings 	= "";


	    java.io.FileInputStream is	= new java.io.FileInputStream(files[fileIndex]);
	    // BufferedReader miatt a buffermeret kozvetlen hatassal van a progressbar elorehaladasara
	    // Ezert ha tul nagy a buffer a fajlmerethez kepest, az ellehetetleniti a hatralevo ido szamitasat
	    // Ezert korlatozom a buffer nagysagat. 
	    // TODO: ezt meg nezd meg meg egyszer mert vmi nem ok! Lehet h. FileInputStream is bufferel?
	    long fSize = is.available();
	    int buffSize = (int)Math.min(Math.max(fSize/100, 80), 8192);	// min 80, max 8192
	    // TODO: consider using java.io.LineNumberReader.
	    java.io.BufferedReader  in	= new BufferedReader(new InputStreamReader(is), buffSize);
		final int	TIMESTAMP		= 0,
					HEADER			= 1,
					EMPTY_LINES1	= 2,
					COLNAMES		= 3,
					ROWS			= 4,
					EMPTY_LINES2	= 5,
					END_TIME		= 6;
		String				line	= null;
		String				delim	= null;
		int					lineno	= 0;
		int					state	= TIMESTAMP;
		String				err		= null;		
		ArrayList<String>	warn	= new ArrayList<String>();

		Columns				fname	= new Columns();
		ArrayList<Object>	fval	= new ArrayList<Object>();
		ColumnType.TypeAndValue tv	= null; 

		MEMEApp.LONG_OPERATION.progress(0, is.available());
		do {
			MEMEApp.LONG_OPERATION.progress(is.getChannel().position());

			line = in.readLine();	//if (line == null) break;
			if(line!=null)line = line.trim();
			lineno += 1;

//			System.out.println(lineno);

			if (state < EMPTY_LINES1) {
				if (state == HEADER && line.length() == 0) {
					state = EMPTY_LINES1;
					continue;
				}
				int colon = line.indexOf(':');
				if (colon < 0 || line.length() <= colon) {
					if (state == TIMESTAMP) {
						warn.add(String.format("in line %d: start time expected - this line is ignored", lineno));
						state = HEADER;
						continue;
					}
					err = "syntax error"; 
					break;
				}

				String a = line.substring(0, colon).trim();
				String b = line.substring(colon+1).trim();

				if (state == TIMESTAMP) {
					state = HEADER;
					if (!a.equalsIgnoreCase("Timestamp")) {
						warn.add(String.format("in line %d: expected 'Timestamp' but got '%s'", lineno, a));
						continue;
					}
					startTime = parseDateTime(b);
					if (startTime < 0) {
						warn.add(String.format("in line %d: cannot interpret start time - unknown time format '%s'", lineno, b));
						startTime = 0;
					}
					continue;
				}
				if (state == HEADER) {
					if (a.length() == 0) {
						err = "missing parameter name";
						break;
					}
					if (fname.contains(a)) {
						warn.add(String.format("in line %d: ignoring second definition of parameter %s", lineno, a));
						continue;
					}
					tv = ColumnType.parseValue(b); 
					fname.append(a, tv.getType());
					fval.add(tv.getValue());
					continue;
				}
				assert false;
			}
			
			if (state == EMPTY_LINES1) {
				if (line.length() == 0) continue;
				state = COLNAMES;
			}
			
			if (state == COLNAMES) {
				// Autodetect delimiter string
				line = line.replaceFirst("^\\s", "");
				boolean ok = (line.length() >= 4);
				if (ok) {
					String low = line.toLowerCase();
					if (line.charAt(0) == '"') {
						ok = simphony ? low.startsWith("\"run number\"") && line.length() >= 13
									  : low.startsWith("\"run\"") && line.length() >= 6;
						if (ok) {
							int i = line.indexOf('"',simphony ? 12 : 5);
							ok = i > (simphony ? 12 : 5);
							if (ok) delim = line.substring(simphony ? 12 : 5, i);
						}
					}
					else {
						ok = low.startsWith(simphony ? "run number" : "run");
						if (ok) {
							int i = low.indexOf("tick",simphony ? 10 : 3);
							ok = i > (simphony ? 10 : 3);
							if (ok) delim = line.substring(simphony ? 10 : 3, i);
						}
					}
				}
				
				String[] names = ok ? getColumnLabels(line,lineno,delim) : null;
				//String[] names = ok ? parseCSVLine(line, delim, '"', Utils.FORCE_EMPTY) : null;
				ok = (names != null && names.length >= 3);
				if (!ok) {
					err = "missing output columns";
					break;
					
				}
				
				ok = ((simphony ?  names[0].equalsIgnoreCase("run number") : names[0].equalsIgnoreCase("run"))
					  && names[1].equalsIgnoreCase("tick") );
				if (!ok) {
					err = "invalid column labels";
					break;
				}
				for (int i = 2; i < names.length && err == null; ++i) {
					if (names[i].length() == 0) {
						err = "column label must be non-empty string";
						break;
					}
					if (otherPar.contains(names[i])) {
						err = String.format("identical name for different columns (%s)", names[i]);
						break;
					}
					otherPar.append(names[i], null);
				}
				state = ROWS;
				continue;
			}

			if (state == ROWS) {
				
				if(line == null && isMultiCol)
				{
					is.close();in.close();
					is = new FileInputStream(files[++fileIndex]);
					in = new BufferedReader(new InputStreamReader(is));
					lineno=0;
					line=in.readLine();
					continue;
				}
				
				if (line.length() == 0) {
					state = EMPTY_LINES2;
					continue;
				}

				String[] values = parseCSVLine(line, delim, '"', 0);
				
				if (values.length < 3) {
					warn.add(String.format("in line %d: too few values - this line is ignored", lineno));
					continue;
				}
				tv = parseValueOrNumberWithDotOrComma(values[0], delim);
				if (!(tv.getValue() instanceof Number)) {
					warn.add(String.format("in line %d: syntax error in value of 'run' - this line is ignored", lineno));
					continue;
				}
				Run lastRun = (runs.isEmpty() ? null : runs.get(runs.size()-1));
				int run = ((Number)tv.getValue()).intValue();
//				if (run <= 0 || (lastRun != null && run < lastRun.run)) {
//					warn.add(String.format("in line %d: invalid 'run' value %d - this line is ignored", lineno, run));
//					continue;
//				}
//				if (lastRun != null && lastRun.run < run)
				if (lastRun != null && lastRun.run != run)
					lastRun = null;
				tv = parseValueOrNumberWithDotOrComma(values[1], delim);
				if (!(tv.getValue() instanceof Number)) {
					warn.add(String.format("in line %d: syntax error in value of 'tick' - this line is ignored", lineno));
					continue;
				}
				int tick = ((Number)tv.getValue()).intValue();
				Result.Row lastRow = null;
				if (lastRun != null && !lastRun.rows.isEmpty())
					lastRow = lastRun.rows.get(lastRun.rows.size()-1);
				if (tick <= 0 || (lastRow != null && tick < lastRow.getTick())) {
					warn.add(String.format("in line %d: invalid 'tick' value %d - this line is ignored", lineno, tick));
					continue;
				}
				if (lastRun == null) {
					lastRun = new Run(run);
					runs.add(lastRun);
				}
				lastRow = new Result.Row(otherPar, tick);
				if(!isMultiCol)
				{
					for (int i = 2; i < values.length; ++i) {
						
						if (i > otherPar.size()+1) {
							throw new IllegalStateException(String.format("in line %d: inconsistent input - too many values",lineno));
						}
						tv = parseValueOrNumberWithDotOrComma(values[i], delim);
						lastRow.set(i-2, tv.getValue());
						otherPar.get(i-2).extendType(tv.getType());
					}
				}
				else
				{
					int i = 2;
					int z = 0;
					ArrayList<Integer> placeholders = new ArrayList<Integer>();
					for(String label : distinctLabels)
					{
						if(label.equals("run") || label.equals("tick"))continue;
						if(!maxMultiColumnNumbers.keySet().contains(label))
						{
							if (i > otherPar.size()+1) {
								throw new IllegalStateException(String.format("in line %d: inconsistent input - too many values",lineno));
							}
							tv = parseValueOrNumberWithDotOrComma(values[i-z], delim);
							lastRow.set(i-2, tv.getValue());
							otherPar.get(i-2).extendType(tv.getType());
							i++;
						}
						else
						{
							int j = 0;
							for(;j<=actualMultiColumnNumbers.get(fileIndex).get(label);j++)
							{
								if (i > otherPar.size()+1) {
									throw new IllegalStateException(String.format("in line %d: inconsistent input - too many values",lineno));
								}
								tv = parseValueOrNumberWithDotOrComma(values[i-z], delim);
								lastRow.set(i-2, tv.getValue());
								otherPar.get(i-2).extendType(tv.getType());
								i++;
								
							}
							for(;j<=maxMultiColumnNumbers.get(label);j++)
							{
								if(placeholders.isEmpty())
								{
									placeholders.add(runs.size()-1);
									placeholders.add(lastRun.rows.size());
								}
								placeholders.add(i-2);
								lastRow.set(i-2, "aitia_generated_placeholder_"+label);
								otherPar.get(i-2).extendType(ColumnType.STRING);
								z++;
								i++;
							}
							
						}
					}
					if(placeholders.size()>0)placeholderIndices.add(placeholders);
				}
				lastRun.rows.add(lastRow);
				
				if (lastRun.rows.size() > maxRowsPerRun)
					maxRowsPerRun = lastRun.rows.size();
				continue;
			}
			
			if (state == EMPTY_LINES2) {
				if (line.length() == 0) continue;
				state = END_TIME;
			}

			if (state == END_TIME) {
				int colon = line.indexOf(':');
				if (colon < 0 || line.length() <= colon) {
					warn.add(String.format("in line %d: end time expected - this line is ignored", lineno)); 
					break;
				}
				String a = line.substring(0, colon).trim();
				String b = line.substring(colon+1).trim();
				if (!a.equalsIgnoreCase("End Time")) {
					warn.add(String.format("in line %d: expected 'End Time' but got '%s'", lineno, a));
					break;
				}
				endTime = parseDateTime(b);
				if (endTime < 0) {
					warn.add(String.format("in line %d: cannot interpret end time - unknown time format '%s'", lineno, b));
					endTime = 0;
				}
				break;
			}


		} while (line !=null && err == null);

		if (err != null) {
			in.close(); is.close();
			throw new Exception(String.format("in line %d: %s", lineno, err)); 
		}

		if (warn.size() > MAX_NUMBER_OF_DISPLAYABLE_WARNINGS) {
			warnings = String.format("%d warnings. %s",warn.size(),MEMEApp.seeTheErrorLog("%s"));
			String twarn = String.format("%d warnings. Writing to the log...",warn.size());
			MEMEApp.LONG_OPERATION.setTaskName(twarn);
			MEMEApp.LONG_OPERATION.progress(0,warn.size());
			for (int i=0;i<warn.size();++i) {
				try {
					MEMEApp.LONG_OPERATION.progress(i);
					Logger.logError(warn.get(i));
				} catch (UserBreakException e) {
					Logger.logError("%d warning(s) more.",warn.size()-i-1);
					break;
				}
			}
		} else warnings = join(warn, "\n");

		fixedPar = new GeneralRow(fname);
		for (int i = 0; i < fname.size(); ++i)
			fixedPar.set(i, fval.get(i));

		//test();
		
		// sorting run
		java.util.Collections.sort(runs);

		// Determine the range of columns otherPar[0..i] which may be input parameters,
		// because their values are always constant during a run.
		// The constant values are extracted to canBeInput[].
		//
		ALL:
		for (int col = 0; col < otherPar.size() - 1; ++col) {
			for (int r = runs.size() - 1; r >= 0; --r) {
				Run run = runs.get(r);
				assert !run.rows.isEmpty();
				Object val = run.rows.get(0).get(col);

				if (col >= canBeInput.size())
					canBeInput.add(val);
				else if (!val.equals(canBeInput.get(col)))
					canBeInput.set(col, RepastImportDialog.ONE_RUN_CONSTANT);

				for (int j = run.rows.size() - 1; j >= 0; --j) {
					Result.Row row = run.rows.get(j);
					assert row.getColumns() == otherPar;
					Object v = row.get(col);
					if ((v == null) ? (val != null) : !v.equals(val)) {
						canBeInput.remove(col);
						break ALL;
					}
				}
			}
		}
		in.close(); is.close();
	}
	
	private ColumnType.TypeAndValue parseValueOrNumberWithDotOrComma(String str, String delimiter) {
		ColumnType.TypeAndValue tv = ColumnType.parseValue(str);
		if (!",".equals(delimiter) && str.contains(",")) {
			ColumnType.TypeAndValue tvWithDot = ColumnType.parseValue(str.replace(',', '.'));
			if (
					tv.getValue() instanceof String && 
						(tvWithDot.getValue() instanceof Double || 
						tvWithDot.getValue() instanceof Integer ||
						tvWithDot.getValue() instanceof Long
					)) {
				tv = tvWithDot;
			}
		}
		return tv;
	}
	
	private String[] getColumnLabels(String line,int lineno, String delim)
	{
		multiColNames = new ArrayList<String>();
		ArrayList<String> colHeaders = new ArrayList<String>();
		ArrayList<String> resultHeader = new ArrayList<String>();
		distinctLabels = new ArrayList<String>();
		actualMultiColumnNumbers = new ArrayList<LinkedHashMap<String,Integer>>();
		
		
		String[] names = parseCSVLine(line, delim, '"', Utils.FORCE_EMPTY);
		if(files.length==1)return names;
		
		
		java.io.FileInputStream is;
		java.io.BufferedReader  in;
		
		colHeaders.add(line);
		try
		{
			
			for(int fileNo=1; fileNo<files.length;fileNo++)
			{
				is = new java.io.FileInputStream(files[fileNo]);
				in = new BufferedReader(new InputStreamReader(is));
				colHeaders.add(in.readLine());
				is.close();
				in.close();
			}
			
			Pattern p = Pattern.compile("(.+Multi)_\\d+$");
			for(String header : colHeaders)//searching for multicolumn labels in every file, since there is a possibility for zero length multicolumn elements
			{
				names = parseCSVLine(header, delim, '"', Utils.FORCE_EMPTY);
				for(String name: names)
				{
					Matcher m = p.matcher(name);
					Boolean isMatch = m.matches();
					
					if(isMatch && !multiColNames.contains(m.group(1)))
					{
						isMultiCol = true;
						multiColNames.add(m.group(1));
						distinctLabels.add(m.group(1));
					}
					else if(!isMatch && !distinctLabels.contains(name))distinctLabels.add(name);
				}
			}
			
			for(String header : colHeaders)
			{
				LinkedHashMap<String, Integer> actualLengths =  new LinkedHashMap<String, Integer>();
				for(String multiColName : multiColNames)
				{
					int colNameIndex = header.lastIndexOf(multiColName);
					if(colNameIndex<0)actualLengths.put(multiColName, 0);
					else
					{
						String sub = header.substring(colNameIndex);
						p = Pattern.compile(multiColName+"_(\\d+).*");
						Matcher m = p.matcher(sub);
						m.matches();
						actualLengths.put(multiColName, Integer.parseInt(m.group(1)));
					}
				}
				actualMultiColumnNumbers.add(actualLengths);
				
			}
			
			maxMultiColumnNumbers = new LinkedHashMap<String, Integer>();
			for(String name : distinctLabels)
			{
				if(multiColNames.contains(name))maxMultiColumnNumbers.put(name, 0); 
			}
			for(String header : colHeaders)
			{
					for(String name : multiColNames)
					{
						int colNameIndex = header.lastIndexOf(name);
						String sub;
						if(colNameIndex>=0)sub = header.substring(colNameIndex);
						else continue;
						p = Pattern.compile(name+"_(\\d+).*");
						Matcher m = p.matcher(sub);
						m.matches();
						int maxLength = Integer.parseInt(m.group(1));
						if(maxLength>maxMultiColumnNumbers.get(name))maxMultiColumnNumbers.put(name, maxLength);
					}
			}	
		} 
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		    
		for(String colName : distinctLabels)
		{
			if(!multiColNames.contains(colName)) resultHeader.add(colName);
			else
			{
				for(int i=0; i<=maxMultiColumnNumbers.get(colName);i++)
				{
					resultHeader.add(colName+"_"+i);
				}
			}
		}
		
		return resultHeader.toArray(new String[resultHeader.size()]);
	}
	
	public void replaceNaPlaceholders(String settingsXML) throws Exception
	{
		HashMap<String, String> naFillers = new LinkedHashMap<String, String>();
		if(!new File(settingsXML).exists())throw new Exception("settings file doesn't exist!");
		else if(!isMultiCol)return;
		else
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder xmlParser = factory.newDocumentBuilder();
			Document document = xmlParser.parse(new File(settingsXML));
			Element root = document.getDocumentElement();
			document.getXmlVersion();
			NodeList nl = root.getElementsByTagName("source");
			Pattern namePattern = Pattern.compile(".*aitiaGeneratedRecorder\\.getCollectionLength\\(\"(.+?)\"\\);.*", Pattern.DOTALL);
			Pattern fillerPattern = Pattern.compile(".*else \\{\nresult.add\\(\"(.+?)\"\\);\n\\}\n\\}\nreturn result;.*", Pattern.DOTALL);
			for(int i=0; i<nl.getLength();i++)
			{
				String source = nl.item(i).getTextContent();
				Matcher nameMatcher = namePattern.matcher(source);
				Matcher fillerMatcher = fillerPattern.matcher(source);
				
				if(nameMatcher.matches() && fillerMatcher.matches())
				{
					String collectionLengthName =  nameMatcher.group(1);
					if(collectionLengthName.endsWith("()"))collectionLengthName = collectionLengthName.substring(0, collectionLengthName.length()-2);
					naFillers.put(collectionLengthName, fillerMatcher.group(1));
				}
				//else throw new Exception("can't extract n/a fillers from settings file");	
			}
			for(String name : multiColNames)
			{
				if(!naFillers.keySet().contains(name)) throw new Exception("can't extract n/a fillers from settings file");
			}
		}
		replaceNaPlaceholders(naFillers);
		
	}
	
	public void replaceNaPlaceholders(HashMap<String, String> naFillers)
	{
		int placeholderLength = "aitia_generated_placeholder_".length();
		for(ArrayList<Integer> indices : placeholderIndices)
		{
			int run = indices.get(0);
			int row = indices.get(1);
			for(int i = 2;i<indices.size();i++)
			{
				String element = (String)runs.get(run).rows.get(row).get(indices.get(i));
				runs.get(run).rows.get(row).set(indices.get(i),naFillers.get(element.substring(placeholderLength)));
			}
		}
	}

	//-------------------------------------------------------------------------
	// Model thread only
	/** Writes the content of the Repast result file to the data base.
	 * @param db the database
	 */
	public void write(IResultsDbMinimal db, String modelName, String version, String batchDesc, String settingsXML) throws Exception {

		// Ensure that the GUI thread is waiting and making no changes in the Model   
		MEMEApp.LONG_OPERATION.showProgressNow();
		MEMEApp.LONG_OPERATION.progress(0, runs.size());
		LinkedHashMap<String, String> naFillers = new LinkedHashMap<String, String>();
		
		if(!new File(settingsXML).exists() && isMultiCol)throw new Exception("settings.xml needed for writing variable column size results into database!");
		if(isMultiCol)replaceNaPlaceholders(settingsXML);
		int k = fixedPar.size();
		GeneralRow inp = fixedPar;
		Columns out = otherPar;
		if (inputIndex >= 0) {
			Columns c = new Columns();
			c.append(fixedPar.getColumns(), 0, fixedPar.size());
			c.append(otherPar, 0, inputIndex + 1);
			inp = new GeneralRow(c);
			for (int i = 0; i < k; ++i)
				inp.set(i, fixedPar.get(i));
			out = new Columns();
			out.append(otherPar, inputIndex + 1, otherPar.size());
		}
		final int n = out.size(); 

		model = db.findModel(modelName, version);
		if (model == null)
			model = new Model(Model.NONEXISTENT_MODELID, modelName, version);

		// Generate batch number
		//
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
		
		int placeholderLength = "aitia_generated_placeholder_".length();
		
		for (int i = 0; i < runs.size(); ++i) {
			MEMEApp.LONG_OPERATION.progress(i);				// display progress
			result.setRun(runs.get(i).run); // !!!!M�DOS�T�S!!!!
			ArrayList<Result.Row> rows = runs.get(i).rows;
			if (rows.isEmpty()) continue;

			for (int j = 0; j <= inputIndex; ++j)
				inp.set(k + j, rows.get(0).get(j));

			result.resetRows(out);
			for (int j = 0; j < rows.size(); ++j) {
				Result.Row row = rows.get(j);
				Result.Row newrow = new Result.Row(out, row.getTick());
				for (int l = 0; l < n; ++l)
				{
					Object o = row.get(inputIndex + 1 + l);
//					if(o instanceof String)
//					{
//						String element = (String)o; 
//						if(element.contains("aitia_generated_placeholder_"))o=naFillers.get(element.substring(placeholderLength));
//					}
					newrow.set(l, o);
				}
				result.add(newrow);
			}
			try {
				db.addResult(result);
			} catch (final ValueNotSupportedException e) {
				warnings += "\nDatabase warnings: see the error log for details.\n";
			}
//			dirty = true;

			// free memory:
			runs.set(i, null);
		}
		runs.clear();

		if (batchDesc != null && batchDesc.length() > 0) {
			MEMEApp.LONG_OPERATION.setTaskName("Writing batch description...");
			db.storeBatchDescription(result.getModel().getModel_id(), result.getBatch(), batchDesc);
		}
	}
	
	public void writeIntelliSweepResults(
			IResultsDbMinimal db, 
			String isPluginXMLFileName, 
			String modelName, 
			String version) throws 	Exception{
		ResultProcessingFrame resultProcesser = new ResultProcessingFrame();
		try {
		resultProcesser.writeIntelliSweepResults(
				db, 
				isPluginXMLFileName, 
				modelName, 
				version, 
				runs, 
				fixedPar, 
				otherPar, 
				inputIndex, 
				startTime, 
				endTime);
		} catch (final ValueNotSupportedException e) {
			warnings += "\nDatabase warnings: see the error log for details.\n";
		} catch (final NoSuchPluginException e) {
			Logger.logWarning(e.getMessage());
			write(db, modelName, version, null, isPluginXMLFileName);
		}

	}

	//-------------------------------------------------------------------------
	/** Does nothing. */
	void close() {
//		if (input != null) {
//			input.close();
//			input = null;
//		}
//		if (is != null) {
//			is.close();
//			is = null;
//		}
	}

	//-------------------------------------------------------------------------
	@Override public void finalize() {
		try { close(); } catch (Throwable t) {}
	}


	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}


	/**
	 * @return the endTime
	 */
	public long getEndTime() {
		return endTime;
	}


	/**
	 * @return the runs
	 */
	public ArrayList<Run> getRuns() {
		return runs;
	}


	/**
	 * @return the warnings
	 */
	public String getWarnings() {
		return warnings;
	}
	

	//-------------------------------------------------------------------------
//	private void test() {
//		System.out.println("startTime: " + DateFormat.getDateTimeInstance().format(new java.util.Date(startTime)));
//		System.out.println("endTime: " + DateFormat.getDateTimeInstance().format(new java.util.Date(endTime)));
//		
//		System.out.println("fixed pars:"); 
//		for (int i = 0; i < fixedPar.size(); ++i) {
//			System.out.println(format(
//					"[%d] %s {%s} \"%s\"", i, fixedPar.getColumns().get(i),
//							fixedPar.getColumns().getType(i).getSQLTypeStr(),
//							fixedPar.getAsString(i)
//					));
//		}
//		
//		System.out.println("Other columns:");
//		for (int i = 0; i < otherPar.size(); ++i) {
//			System.out.println(format(
//					"[%d] %s {%s}", i, otherPar.get(i),
//							otherPar.getType(i).getSQLTypeStr()
//					));
//		}
//
//		System.out.println("Rows:");
//		for (int i = 0; i < runs.size(); ++i) {
//			for (int j = 0; j < runs.get(i).rows.size(); ++j) {
//				StringBuilder row = new StringBuilder(format("run#%d,tick#%d", runs.get(i).run, runs.get(i).rows.get(j).getTick()));
//				for (int k = 0; k < runs.get(i).rows.get(j).size(); ++k) {
//					row.append(',');
//					row.append( runs.get(i).rows.get(j).getAsString(k) );
//				}
//				System.out.println(row.toString());
//			}
//		}
//	}

}
