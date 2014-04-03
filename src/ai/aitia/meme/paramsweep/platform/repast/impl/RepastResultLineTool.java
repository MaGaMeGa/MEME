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

import ai.aitia.meme.paramsweep.platform.IResultFileTool;

/**
 * A tool to parse result files line by line and change the content. The intended usage is to 
 * feed the lines of the file to the tool, and modify result lines on the fly. A method to add an offset to run# 
 * is provided.
 * @author AITIA International Inc.
 */
public class RepastResultLineTool {
	
	private boolean headerLine;
	private boolean columnNamesLine;
	private boolean resultLine;
	private boolean footerLine;
	
	private long lineFeedCount;
//	private boolean headerPassed;
//	private boolean columnNamesPassed;
	private boolean resultLinesPassed;
	private String delimiter;
	private char delimiterChar;
	
	public RepastResultLineTool() {
		lineFeedCount = 0L;
		
		headerLine = false;
		columnNamesLine = false;
		resultLine = false;
		footerLine = false;
		
//		headerPassed = false;
//		columnNamesPassed = false;
		resultLinesPassed = false;
	}

	/** Feeds a new line of the result file to the tool, which changes state based on the content. 
	 * @param line	One line of the result file.
	 */
	public void feedLine(CharSequence line) {
		// TODO parse line, look at previous states, and set flags
		lineFeedCount++;
		if (lineFeedCount == 1) {
			headerLine = true;
			columnNamesLine = false;
			resultLine = false;
			footerLine = false;
			return;
		} else if (headerLine && "".contentEquals(line)) {
//			headerPassed = true;
			headerLine = false;
			columnNamesLine = false;
			resultLine = false;
			footerLine = false;
			return;
		} else if (line.length() >=5 && "\"run\"".contentEquals(line.subSequence(0, 5))) {
			delimiterChar = line.charAt(5);
			delimiter = String.valueOf(delimiterChar);
			headerLine = false;
			columnNamesLine = true;
			resultLine = false;
			footerLine = false;
			return;
		} else if (columnNamesLine || (resultLine && !"".contentEquals(line))) {
//			columnNamesPassed = true;
			headerLine = false;
			columnNamesLine = false;
			resultLine = true;
			footerLine = false;
			return;
		} else if (resultLine && "".contentEquals(line)) {
			resultLinesPassed = true;
			headerLine = false;
			columnNamesLine = false;
			resultLine = false;
			footerLine = false;
			return;
		} else if (resultLinesPassed && !"".contentEquals(line)) {
			headerLine = false;
			columnNamesLine = false;
			resultLine = false;
			footerLine = true;
			return;
		}
	}

	/** 
	 * @return whether the last line fed was a header line.
	 */
	public boolean isHeaderLine() { return headerLine; }
	/**
	 * @return whether the last line fed was the column names line.
	 */
	public boolean isColumnNamesLine() { return columnNamesLine; }
	/**
	 * @return whether the last line fed was a result line.
	 */
	public boolean isResultLine() { return resultLine; }
	/**
	 * @return whether the last line fed was a footer line.
	 */
	public boolean isFooterLine() { return footerLine; }
	/**
	 * @return the number of lines fed to the tool.
	 */
	public long getLineFeedCount() { return lineFeedCount; }

	/** Adds an offset to the run# in the result line. The line must be a result line in the result file of 
	 * the platform.
	 * @param line	The result line from the result file of the platform.
	 * @param runOffset	The number to add to the run# in the line.
	 * @return	The altered line, must be the same reference as the parameter.
	 */
	public StringBuilder addToRunNumber(StringBuilder line, long runOffset) {
		//Parse first item to delimiter as number, add runOffset, replace subsequence
		try {
			long runNumber = Long.parseLong(line.substring(0, line.indexOf(delimiter)));
			runNumber += runOffset;
			line.replace(0, line.indexOf(delimiter), ""+runNumber);
		} catch (NumberFormatException e) {
			System.err.println("WARNING: Could not parse run#, leaving line as it is:");
			System.err.println(line);
		}
		return line;
	}

	/** Replaces a value in the result line. 
	 * @param line	The result line from the result file of the platform.
	 * @param columnIndex	The index of the column to replace (starts from 0).
	 * @param newColumnContent	The new content of the column.
	 * @return	The altered line, must be the same reference as the parameter.
	 */
	public StringBuilder replaceColumnValue(StringBuilder line,
			int columnIndex, String newColumnContent) {
		// replaces the columnIndex-th column, uses delimiter to locate, replaces column to newColumnContent
		int actualColumn = 0;
		int actualColumnBeginIndex = 0;
		for (int i = 0; i < line.length() && actualColumn < columnIndex; i++) {
			if (line.charAt(i) == delimiterChar) {
				actualColumn++;
				actualColumnBeginIndex = i+1;
			}
		}
		if (actualColumn == columnIndex) {
			int actualColumnEndIndex = line.indexOf(delimiter, actualColumnBeginIndex);
			if (actualColumnEndIndex == -1) actualColumnEndIndex = line.length();
			line.replace(actualColumnBeginIndex, actualColumnEndIndex, newColumnContent);
		} else if (actualColumn < columnIndex) {
			System.err.println("WARNING: There were less columns than"+columnIndex+", leaving line as it is:");
			System.err.println(line);
		}
		return line;
	}

}
