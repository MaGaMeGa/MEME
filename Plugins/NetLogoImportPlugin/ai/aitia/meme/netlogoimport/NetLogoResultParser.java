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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

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
import ai.aitia.meme.utils.Utils;

@Deprecated
public class NetLogoResultParser {
	
	//====================================================================================================
	// members
	
	private static final int MAX_NUMBER_OF_DISPLAYABLE_WARNINGS = 100;
	
	/** The Repast result file object. */
	File file;

	/** The model object. */
	Model model	= null;
	/** The start time of the model. */
	
	long startTime = 0;
	
	/** The end time of the model. */
	long endTime = 0;

	
	/** Column informations about columns. */ 
	Columns par = null;
	
	/** List of runs. */
	ArrayList<Run> runs	= null;
	
	/** The maximum number of rows of a run. */
	int	maxRowsPerRun = 0;

	/** Warning messages. */
	String warnings	= "";
	
	int stepIndex = -1;

	//=========================================================================
	// Methods
	
	//----------------------------------------------------------------------------------------------------
	public NetLogoResultParser(File f) { file = f; }
	
	//-------------------------------------------------------------------------
	/** Reads the NetLogo result file to the memory. */
	public void readFile() throws Exception {
		model = null;
		startTime = 0;
		endTime = 0;

		par = new Columns();
		runs = new ArrayList<Run>();
		maxRowsPerRun = 0;
		warnings = "";
		stepIndex = -1;

	    if (!file.exists() || file.isDirectory())
	    	throw new Exception("Cannot find file"); 

	    FileInputStream is	= new FileInputStream(file);
	    
	    long fSize = is.available();
	    int buffSize = (int) Math.min(Math.max(fSize / 100,80),8192);	// min 80, max 8192
	    BufferedReader in = new BufferedReader(new InputStreamReader(is),buffSize);
		final int	TIMESTAMP		= 0,
					EMPTY_LINES1	= 1,
					COLNAMES		= 2,
					ROWS			= 3,
					EMPTY_LINES2	= 4,
					END_TIME		= 5;
		String line	= null;
		String delim = null;
		int	lineno	= 0;
		int	state = TIMESTAMP;
		String err = null;		
		ArrayList<String> warn = new ArrayList<String>();

		ColumnType.TypeAndValue tv = null; 

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
					continue;
				}

				String a = line.substring(0,colon).trim();
				String b = line.substring(colon + 1).trim();

				state = EMPTY_LINES1;
				if (!a.equalsIgnoreCase("Timestamp")) {
					warn.add(String.format("in line %d: expected 'Timestamp' but got '%s'",lineno,a));
					continue;
				}
				startTime = Utils.parseDateTime(b);
				if (startTime < 0) {
					warn.add(String.format("in line %d: cannot interpret start time - unknown time format '%s'",lineno,b));
					startTime = 0;
				}
				continue;
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
				
				Run lastRun = (runs.isEmpty() ? null : runs.get(runs.size() - 1));
				int run = ((Number)tv.getValue()).intValue();
				if (lastRun != null && lastRun.run != run)
					lastRun = null;
				
				tv = ColumnType.parseValue(values[stepIndex]);
				if (!(tv.getValue() instanceof Number)) {
					warn.add(String.format("in line %d: syntax error in value of '[step]' - this line is ignored",lineno));
					continue;
				}
				
				int tick = ((Number)tv.getValue()).intValue();
				Result.Row lastRow = null;
				if (lastRun != null && !lastRun.rows.isEmpty())
					lastRow = lastRun.rows.get(lastRun.rows.size() - 1);
				if (tick < 0 || (lastRow != null && tick < lastRow.getTick())) {
					warn.add(String.format("in line %d: invalid '[step]' value %d - this line is ignored",lineno,tick));
					continue;
				}
				
				if (lastRun == null) {
					lastRun = new Run(run);
					runs.add(lastRun);
				}
				
				lastRow = new Result.Row(par,tick);
				
				for (int i = 1;i < values.length;++i) {
					if (i == stepIndex) continue;
					if (i > par.size() + 1) 
						throw new IllegalStateException(String.format("in line %d: inconsistent input - too many values",lineno));
					tv = ColumnType.parseValue(values[i]);
					int offset = i > stepIndex ? 2 : 1;
					lastRow.set(i - offset,tv.getValue());
					par.get(i - offset).extendType(tv.getType());
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
				line = line.replaceFirst("^\"+","");
				line = line.replaceFirst("\"+$","");
				int colon = line.indexOf(':');
				if (colon < 0 || line.length() <= colon) {
					warn.add(String.format("in line %d: end time expected - this line is ignored",lineno)); 
					break;
				}
				String a = line.substring(0,colon).trim();
				String b = line.substring(colon + 1).trim();
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
					MEMEApp.logError(warn.get(i));
				} catch (UserBreakException e) {
					MEMEApp.logError("%d warning(s) more.",warn.size() - i - 1);
					break;
				}
			}
		} else warnings = Utils.join(warn,"\n");
		
		// sorting run
		Collections.sort(runs);

		// Determine the range of columns otherPar[0..i] which may be input parameters,
		// because their values are always constant during a run.
		// The constant values are extracted to canBeInput[].
		//
		in.close();
		is.close();
	}

	//-------------------------------------------------------------------------
	// Model thread only
	/** Writes the content of the NetLogo result file to the data base.
	 * @param db the database
	 */
	public void write(IResultsDbMinimal db, String modelName, String version, String batchDesc, final boolean include0th) throws Exception { 

		// Ensure that the GUI thread is waiting and making no changes in the Model   
		MEMEApp.LONG_OPERATION.showProgressNow();
		MEMEApp.LONG_OPERATION.progress(0,runs.size());

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

		for (int i = 0;i < runs.size();++i) {
			MEMEApp.LONG_OPERATION.progress(i); // display progress
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
				for (int l = 0;l < n;++l)
					newrow.set(l,row.get(stepIndex - 1 + l));
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

		if (batchDesc != null && batchDesc.length() > 0) {
			MEMEApp.LONG_OPERATION.setTaskName("Writing batch description...");
			db.storeBatchDescription(result.getModel().getModel_id(),result.getBatch(),batchDesc);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void writeIntelliSweepResults(IResultsDbMinimal db, String isPluginXMLFileName, String modelName, String version, final boolean include0th)
																																throws Exception {
		ResultProcessingFrame resultProcesser = new ResultProcessingFrame();
		Columns emptyColumns = new Columns();
		GeneralRow fixedPar = new GeneralRow(emptyColumns);
		try {
			resultProcesser.writeIntelliSweepResults(db,isPluginXMLFileName,modelName,version,runs,fixedPar,par,stepIndex - 2,startTime,endTime,
													 include0th);
		} catch (final ValueNotSupportedException e) {
			warnings += "\nDatabase warnings: see the error log for details.\n";
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
