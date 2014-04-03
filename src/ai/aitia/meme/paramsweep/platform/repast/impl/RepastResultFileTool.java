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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;
import ai.aitia.meme.paramsweep.platform.AbstractResultFileTool;

public class RepastResultFileTool extends AbstractResultFileTool {

	private static String newline = System.getProperty("line.separator");
	
	//----------------------------------------------------------------------------------------------------
	public void splitFile(File resultFile, 
			boolean needHeader, File headerFile,
			boolean needColumns, File columnsFile,
			boolean needData, File dataFile, 
			boolean needFooter, File footerFile, 
			long runNumberOffset) throws IOException {
		if (!needHeader && !needData && !needFooter && !needColumns) throw new IllegalArgumentException("At least one section must be selected to extraction.");
		BufferedReader resultReader = null;
		BufferedWriter headerWriter = null;
		BufferedWriter columnsWriter = null;
		BufferedWriter dataWriter = null;
		BufferedWriter footerWriter = null;
		try{
			resultReader = new BufferedReader(new FileReader(resultFile));
			if (needHeader) headerWriter = new BufferedWriter(new FileWriter(headerFile));
			if (needColumns) columnsWriter = new BufferedWriter(new FileWriter(columnsFile));
			if (needData) dataWriter = new BufferedWriter(new FileWriter(dataFile));
			if (needFooter) footerWriter = new BufferedWriter(new FileWriter(footerFile));
			RepastResultLineTool lineTool = new RepastResultLineTool();
			String line = null;
			StringBuilder sb = new StringBuilder(500);
			while ((line = resultReader.readLine()) != null) {
				lineTool.feedLine(line);
				if (needHeader && lineTool.isHeaderLine()) {
					headerWriter.write(line); headerWriter.newLine();
				} else if (lineTool.isColumnNamesLine()) {
					if (needHeader) {
						headerWriter.newLine();
						headerWriter.newLine();
					}
					if (needColumns) {
						columnsWriter.write(line);
						columnsWriter.newLine();
					}
				} else if (needData && lineTool.isResultLine()) {
					sb.delete(0, sb.length());
					sb.append(line);
					lineTool.addToRunNumber(sb, runNumberOffset);
					dataWriter.write(sb.toString()); dataWriter.newLine();
				} else if (needFooter && lineTool.isFooterLine()) {
					footerWriter.newLine(); footerWriter.write(line);
				}
			}
		} finally {
			if (resultReader != null) resultReader.close();
			if (headerWriter != null && needHeader) headerWriter.close();
			if (columnsWriter != null && needColumns) columnsWriter.close();
			if (dataWriter != null && needData) dataWriter.close();
			if (footerWriter != null && needFooter) footerWriter.close();
		}
	}

	//----------------------------------------------------------------------------------------------------
	public String getHeader(List<? extends AbstractParameterInfo> parameters,
			long timestampMillis) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd. HH:mm:ss");
		StringBuilder result = new StringBuilder(1000);
		result.append("Timestamp: ");
		result.append(sdf.format(new Date(timestampMillis)));
		result.append(newline);
		for (int i = 0; i < parameters.size(); i++) {
			if (parameters.get(i).isOriginalConstant()) {
				result.append(parameters.get(i).getName());
				result.append(": ");
				result.append(parameters.get(i).iterator().next());
				result.append(newline);
			}
		}
		result.append(newline);
		result.append(newline);
		return result.toString();
	}

	//----------------------------------------------------------------------------------------------------
	public String getFooter(List<? extends AbstractParameterInfo> parameters,
			long timestampMillis) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd. HH:mm:ss");
		StringBuilder result = new StringBuilder(1000);
		result.append(newline);
		result.append("Timestamp: ");
		result.append(sdf.format(new Date(timestampMillis)));
		return result.toString();
	}

}
