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
package ai.aitia.meme.netlogoimport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.AbstractResultsDb;
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
import ai.aitia.meme.processing.ResultProcessingFrameV2;
import ai.aitia.meme.processing.ResultProcessingFrame.NoSuchPluginException;
import ai.aitia.meme.utils.Utils;
import ai.aitia.visu.globalhandlers.UserBreakException;

public class BufferedNetLogoResultParser {

	//====================================================================================================
	// members
	
	private static final int MAX_NUMBER_OF_DISPLAYABLE_WARNINGS = 100;
//	private static final int MAX_NUMBER_OF_BUFFERED_LINE = 3; // just for testing 
	private static final int MAX_NUMBER_OF_BUFFERED_LINE = 100000;  
	
	private final static int TIMESTAMP		= 0,
							 EMPTY_LINES1	= 1,
							 COLNAMES		= 2,
							 ROWS			= 3,
							 EMPTY_LINES2	= 4,
							 END_TIME		= 5;
	
	final File file;
	Model model	= null;
	long startTime = 0;
	long endTime = 0;
	Columns par = null;
	ArrayList<Run> runs	= null;
	int numberOfRuns = 0;
	int	maxRowsPerRun = 0;
	String warnings	= "";
	int stepIndex = -1;
	int runBuffer = MAX_NUMBER_OF_BUFFERED_LINE;
	String delim = null;

	//=========================================================================
	// Methods
	
	//----------------------------------------------------------------------------------------------------
	public BufferedNetLogoResultParser(final File f) { file = f; }
	
	//-------------------------------------------------------------------------
	public void readFile() throws Exception {
		model = null;
		startTime = 0;
		endTime = 0;

		par = new Columns();
		numberOfRuns = 0;
		maxRowsPerRun = 0;
		warnings = "";
		stepIndex = -1;
		runBuffer = MAX_NUMBER_OF_BUFFERED_LINE;
		delim = null;

	    if (!file.exists() || file.isDirectory())
	    	throw new Exception("Cannot find file"); 

	    final FileInputStream is	= new FileInputStream(file);
	    
	    long fSize = is.available();
	    int buffSize = (int) Math.min(Math.max(fSize / 100,80),8192);	// min 80, max 8192
	    final BufferedReader in = new BufferedReader(new InputStreamReader(is),buffSize);
		String line	= null;
		int	lineno	= 0;
		int	state = TIMESTAMP;
		String err = null;		
		final ArrayList<String> warn = new ArrayList<String>();

		ColumnType.TypeAndValue tv = null;
		int lastRun = -1, lastTick = -1, numberOfRows = 0;

		MEMEApp.LONG_OPERATION.progress(0,is.available());
		do {
			MEMEApp.LONG_OPERATION.progress(is.getChannel().position());

			line = in.readLine();
			if (line == null) break;
			line = line.trim();
			lineno++;

			if (state == TIMESTAMP) {
				line = line.replaceFirst("^\"+","");
				line = line.replaceFirst("\"+$","");
				int colon = line.indexOf(':');
				if (colon < 0 || line.length() <= colon) {
					warn.add(String.format("in line %d: start time expected - this line is ignored",lineno));
					state = EMPTY_LINES1;
//					continue;
				} else {

					final String a = line.substring(0,colon).trim();
					final String b = line.substring(colon + 1).trim();

					state = EMPTY_LINES1;
					if (!a.equalsIgnoreCase("Timestamp")) {
						warn.add(String.format("in line %d: expected 'Timestamp' but got '%s'",lineno,a));
//						continue;
					} else {
						startTime = Utils.parseDateTime(b);
						if (startTime < 0) {
							warn.add(String.format("in line %d: cannot interpret start time - unknown time format '%s'",lineno,b));
							startTime = 0;
						} else {
							continue;
						}
					}
				}
			}
			
			if (state == EMPTY_LINES1) {
				if (line.length() == 0) continue;
				state = COLNAMES;
			}
			
			if (state == COLNAMES) {
				// Autodetect delimiter string
				boolean ok = line.length() > 13;
				if (ok) {
					String low = line.toLowerCase();
					ok = low.startsWith("\"[run number]\"");
					if (ok) {
						int i = line.indexOf('"',14);
						ok = (i > 14);
						if (ok)
							delim = line.substring(14,i);
					}
				}

				if (!ok){
					err = "invalid header format";
					break;
				}
				
				String[] names = ok ? Utils.parseCSVLine(line,delim,'"',Utils.EXC_QUOTE) : null;
				trimAll(names);
				ok = (names != null && names.length >= 3);
				if (!ok) {
					err = "missing output columns";
					break;
				}
				
				ok = (names[0].equalsIgnoreCase("[run number]") && (stepIndex = findIndex(names,"[step]",false)) >= 1);
				if (!ok) {
					err = "invalid column labels";
					break;
				}
				
				for (int i = 1;i < names.length && err == null; ++i) {
					if (i == stepIndex) continue;
					if (names[i].length() == 0) {
						err = "column label must be non-empty string";
						break;
					}
					if (par.contains(names[i])) {
						err = String.format("identical name for different columns (%s)",names[i]);
						break;
					}
					par.append(names[i],null);
				}
				state = ROWS;
				continue;
			}

			if (state == ROWS) {
				if (line.length() == 0) {
					state = EMPTY_LINES2;
					continue;
				}
				
				if (!",".equals(delim))
					line = line.replace(',','.');
				String[] values = Utils.parseCSVLine(line,delim,'"',Utils.EXC_QUOTE);
				removeQuotesAll(values);
				if (values.length < 3) {
					warn.add(String.format("in line %d: too few values - this line is ignored",lineno));
					continue;
				}
				tv = ColumnType.parseValue(values[0]);
				if (!(tv.getValue() instanceof Number)) {
					warn.add(String.format("in line %d: syntax error in value of '[run number]' - this line is ignored",lineno));
					continue;
				}
				
				int run = ((Number)tv.getValue()).intValue();
				if (lastRun != run) {
					lastRun = run;
					numberOfRuns++;
					numberOfRows = 0;
					lastTick = -1;
				}
				
				tv = ColumnType.parseValue(values[stepIndex]);
				if (!(tv.getValue() instanceof Number)) {
					warn.add(String.format("in line %d: syntax error in value of '[step]' - this line is ignored",lineno));
					continue;
				}
				
				int tick = ((Number)tv.getValue()).intValue();
				if (tick < 0 || tick < lastTick) {
					warn.add(String.format("in line %d: invalid '[step]' value %d - this line is ignored",lineno,tick));
					continue;
				}
				
				lastTick = tick;
				
				for (int i = 1;i < values.length;++i) {
					if (i == stepIndex) continue;
					if (i > par.size() + 1) 
						throw new IllegalStateException(String.format("in line %d: inconsistent input - too many values",lineno));
					tv = ColumnType.parseValue(values[i]);
					int offset = i > stepIndex ? 2 : 1;
					par.get(i - offset).extendType(tv.getType());
				}
				numberOfRows++;
				
				if (numberOfRows > maxRowsPerRun)
					maxRowsPerRun = numberOfRows;
				continue;
			}

			if (state == EMPTY_LINES2) {
				if (line.length() == 0) continue;
				state = END_TIME;
			}

			if (state == END_TIME) {
				line = line.replaceFirst("^\"+","");
				line = line.replaceFirst("\"+$","");
				int colon = line.indexOf(':');
				if (colon < 0 || line.length() <= colon) {
					warn.add(String.format("in line %d: end time expected - this line is ignored",lineno)); 
					break;
				}
				final String a = line.substring(0,colon).trim();
				final String b = line.substring(colon + 1).trim();
				if (!a.equalsIgnoreCase("End time")) {
					warn.add(String.format("in line %d: expected 'End time' but got '%s'",lineno,a));
					break;
				}
				endTime = Utils.parseDateTime(b);
				if (endTime < 0) {
					warn.add(String.format("in line %d: cannot interpret end time - unknown time format '%s'",lineno,b));
					endTime = 0;
				}
				break;
			}
		} while (line != null && err == null);

		if (err != null) {
			in.close();
			is.close();
			throw new Exception(String.format("in line %d: %s",lineno,err)); 
		}

		if (warn.size() > MAX_NUMBER_OF_DISPLAYABLE_WARNINGS) {
			warnings = String.format("%d warnings. %s",warn.size(),MEMEApp.seeTheErrorLog("%s"));
			String twarn = String.format("%d warnings. Writing to the log...",warn.size());
			MEMEApp.LONG_OPERATION.setTaskName(twarn);
			MEMEApp.LONG_OPERATION.progress(0,warn.size());
			for (int i = 0;i < warn.size();++i) {
				try {
					MEMEApp.LONG_OPERATION.progress(i);
					Logger.logError(warn.get(i));
				} catch (UserBreakException e) {
					Logger.logError("%d warning(s) more.",warn.size() - i - 1);
					break;
				}
			}
		} else warnings = Utils.join(warn,"\n");
		
		runBuffer = Math.max(1,MAX_NUMBER_OF_BUFFERED_LINE / maxRowsPerRun);
		
		in.close();
		is.close();
	}
	
	//-------------------------------------------------------------------------
	private BufferedReader readFilePart(final BufferedReader reader, final String[] firstLine) throws Exception {
		runs = new ArrayList<Run>();

	    final FileInputStream is	= new FileInputStream(file);
	    long fSize = is.available();
	    int buffSize = (int) Math.min(Math.max(fSize / 100,80),8192);	// min 80, max 8192
	    
	    boolean first = reader == null;
	    BufferedReader in = first ? new BufferedReader(new FileReader(file),buffSize) : reader;
		String line	= firstLine[0];
		
		if (line == null)
			line = in.readLine();
		
		ColumnType.TypeAndValue tv = null; 
		int state = first ? TIMESTAMP : ROWS;
		int noOfRuns = 0;
		
		while (line != null && noOfRuns <= runBuffer) {

			if (line == null) break;
			line = line.trim();

			if (state == TIMESTAMP) { // first part, skipping timestamp
				state = EMPTY_LINES1;
				line = in.readLine();
				continue;
			}
			
			if (state == EMPTY_LINES1) { // first part, skipping timestamp
				if (line.length() == 0) {
					line = in.readLine();
					continue;
				}
				state = COLNAMES;
			}
			
			if (state == COLNAMES) { // first part, skipping column names
				state = ROWS;
				line = in.readLine();
				continue;
			}

			if (state == ROWS) {
				if (line.length() == 0) {
					state = EMPTY_LINES2;
					break;
				}
				
				if (!",".equals(delim))
					line = line.replace(',','.');
				String[] values = Utils.parseCSVLine(line,delim,'"',Utils.EXC_QUOTE);
				removeQuotesAll(values);

				tv = ColumnType.parseValue(values[0]);
				Run lastRun = (runs.isEmpty() ? null : runs.get(runs.size() - 1));
				int run = ((Number)tv.getValue()).intValue();
				if (lastRun != null && lastRun.run != run)
					lastRun = null;
				
				if (lastRun == null) {
					lastRun = new Run(run);
					if (++noOfRuns > runBuffer) break;
					runs.add(lastRun);
					
				}
				
				tv = ColumnType.parseValue(values[stepIndex]);
				int tick = ((Number)tv.getValue()).intValue();
				
				Result.Row row = new Result.Row(par,tick);
				
				for (int i = 1;i < values.length;++i) {
					if (i == stepIndex) continue;
					tv = ColumnType.parseValue(values[i]);
					int offset = i > stepIndex ? 2 : 1;
					row.set(i - offset,tv.getValue());
//					par.get(i - offset).extendType(tv.getType());
				}
				lastRun.rows.add(row);
			}
			line = in.readLine();
		} 
		
		// sorting run
		Collections.sort(runs);
		
		is.close();
		if (state == EMPTY_LINES2) {
			in.close();
			in = null;
		}
		
		firstLine[0] = line;
		return in;
	}

	//-------------------------------------------------------------------------
	// Model thread only
	/** Writes the content of the NetLogo result file to the database.
	 * @param db the database
	 */
	public int write(IResultsDbMinimal db, String modelName, String version, String batchDesc, final boolean include0th) throws Exception { 

		// Ensure that the GUI thread is waiting and making no changes in the Model   
		MEMEApp.LONG_OPERATION.showProgressNow();
		MEMEApp.LONG_OPERATION.progress(0,numberOfRuns);

		// separate input and output parameters
		Columns out = new Columns();
		Columns c = new Columns();
		c.append(par,0,stepIndex - 1);
		GeneralRow inp = new GeneralRow(c);
		out.append(par,stepIndex - 1,par.size());
		final int n = out.size(); 
		model = db.findModel(modelName,version);
		if (model == null)
			model = new Model(Model.NONEXISTENT_MODELID,modelName,version);
		
		// Generate batch number
		int b = db.getNewBatch(model);
		ResultInMem result = new ResultInMem();
		result.setModel(model);
		result.setBatch(b);
		result.setParameterComb(new ParameterComb(inp));
		
		result.setStartTime(startTime); 
		result.setEndTime(endTime);
		
		int actualNumberOfRuns = 0;
		BufferedReader reader = null;
		final String[] firstLine = { null };
		do {
			reader = readFilePart(reader,firstLine);
			for (int i = 0;i < runs.size();++i,++actualNumberOfRuns) {
				MEMEApp.LONG_OPERATION.progress(actualNumberOfRuns); // display progress
				result.setRun(runs.get(i).run); 
				ArrayList<Result.Row> rows = runs.get(i).rows;
				if (rows.isEmpty()) continue;

				for (int j = 0;j < stepIndex - 1;++j)
					inp.set(j,rows.get(0).get(j));

				result.resetRows(out);
				for (int j = 0;j < rows.size();++j) {
					Result.Row row = rows.get(j);
					if (!include0th && row.getTick() == 0) continue;
					Result.Row newrow = new Result.Row(out,row.getTick());
					for (int k = 0;k < n;++k)
						newrow.set(k,row.get(stepIndex - 1 + k));
					result.add(newrow);
				}
				try {
					db.addResult(result);
				} catch (final ValueNotSupportedException e) {
					warnings += "\nDatabase warnings: see the error log for details.\n";
				}

				// free memory:
				runs.set(i,null);
			}
			runs.clear();
			System.gc();
		} while (reader != null);

		if (batchDesc != null && batchDesc.length() > 0) {
			MEMEApp.LONG_OPERATION.setTaskName("Writing batch description...");
			db.storeBatchDescription(result.getModel().getModel_id(),result.getBatch(),batchDesc);
		}
		
		return b;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void writeIntelliSweepResults(final AbstractResultsDb db, final String isPluginXMLFileName, final String modelName, final String version,
										 final String batchDesc, final boolean include0th) throws Exception {
		
		int batch = write(db,modelName,version,batchDesc,include0th);
		
		ResultProcessingFrameV2 resultProcesser = new ResultProcessingFrameV2();
		Columns emptyColumns = new Columns();
		GeneralRow fixedPar = new GeneralRow(emptyColumns);
		try {
			resultProcesser.writeIntelliSweepResults(db,isPluginXMLFileName,modelName,version,batch,fixedPar,par,stepIndex - 2,startTime,endTime,include0th);
		} catch (final ValueNotSupportedException e) {
			warnings += "\nDatabase warnings: see the error log for details.\n";
		} catch (final NoSuchPluginException e) {
			Logger.logWarning(e.getMessage());
			write(db, modelName, version, null, include0th);
		}
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private int findIndex(String[] array, String element, boolean caseSensitive) {
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
		for (int i = 0;i < array.length;++i) {
			String tmp = array[i].trim();
			array[i] = tmp;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void removeQuotesAll(String[] array) {
		for (int i = 0;i < array.length;++i) {
			String tmp = array[i].trim();
			tmp = tmp.replaceFirst("^\"+","");
			tmp = tmp.replaceFirst("\"+$","");
			array[i] = tmp;
		}
	}
}
