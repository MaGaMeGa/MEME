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

import java.awt.Window;
import java.io.File;
import java.util.Properties;

import ai.aitia.meme.pluginmanager.IImportPlugin;
import ai.aitia.meme.pluginmanager.IImportPluginContext;

/** Plugin for imports CSV files. */
public class CSVImportPlugin implements IImportPlugin {

	//================================================================================
	// Implementation of the IImportPlugin interface
	
	public Window showDialog(final IImportPluginContext ctx) {
		File[] f = CSVImportSettingsDialog.openFileDialog(ctx.getAppWindow(),true);
		if (f != null && f.length != 0) {
			if (f.length == 1) {
				CSVImportSettingsDialog dialog = new CSVImportSettingsDialog(ctx,settings);
				dialog.start(CSVImportSettingsDialog.FIRST_PART);
			} else {
				CSVMultiImportSettingsDialog dialog = new CSVMultiImportSettingsDialog(ctx,settings);
				dialog.start(f);
			}
		}
		return null;
	}

	//--------------------------------------------------------------------------------
	public String getLocalizedName() {
		return "CSV file";
	}
	
	//=================================================================================

	/** stored settings. */
	Properties settings = new Properties();
	
	// property keys
	final static String DELIMITER 				= "delimiter"; // String
	final static String MERGE					= "merge"; // boolean
	final static String QUOTE	  				= "quote"; // String
	final static String COMMENT	  				= "commentCharacter"; // String
	final static String EMPTY_TOKEN				= "emptyToken"; // String
	final static String HAS_COLUMN_NAMES		= "hasColumnNames"; // boolean
	final static String NR_IGNORED_LINES		= "nrOfIgnoredLines"; // int
	final static String RUNS					= "runsInTheFile"; // int
	final static String NR_RECORDS				= "nrOfRecordsPerRun"; // int
	final static String TICK_COLUMN				= "tickColumn"; // String
	final static String IS_ADVANCED_CSV_TYPE	= "CSVType"; // boolean
	final static String LINE_PATTERN			= "linePattern"; // String
	final static String HEADER_PATTERN			= "headerPattern"; // String
}
