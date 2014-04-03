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
package ai.aitia.meme.paramsweep.platform;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;

/**
 * An interface for tools that manipulate simulation experiment result files. It provides methods to split 
 * a result file into separate header, data and footer files, and adding an offset to all run numbers in the file. 
 * @author AITIA International Inc.
 */
public interface IResultFileTool {
	
	/**
	 * Splits the result file into three (optional) parts, fixing run numbers as it goes. You have to 
	 * provide which parts you want in which file.
	 * @param resultFile	The result file, which must exist.
	 * @param needHeader	Pass true if you want to create the header file.
	 * @param headerFile	The header file, will be overwritten if exists. May be null.
	 * @param needColumns	Pass true if you want to create the columns file.
	 * @param columnsFile	The columns file, will be overwritten if exists. May be null.
	 * @param needData	Pass true if you want to create the data file.
	 * @param dataFile	The data file, will be overwritten if exists. May be null.
	 * @param needFooter	Pass true if you want to create the footer file.
	 * @param footerFile	The footer file, will be overwritten if exists. May be null.
	 * @param runNumberOffset	The run number offset that will be added to every run number in the data section.
	 * @throws FileNotFoundException when resultFile does not exist.
	 * @throws IOException when an I/O exception occurs.
	 */
	public void splitFile(File resultFile, 
			boolean needHeader, File headerFile, 
			boolean needColumns, File columnsFile, 
			boolean needData, File dataFile, 
			boolean needFooter, File footerFile, 
			long runNumberOffset) throws FileNotFoundException, IOException;
	
	/**
	 * A convenience method to extract only the data part with the run numbers fixed.
	 * @param resultFile	The result file, which must exist.
	 * @param dataFile	The data file, will be overwritten if exists. Must not be null.
	 * @param runNumberOffset	The run number offset that will be added to every run number.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public void splitFileOnlyData(File resultFile, File dataFile, long runNumberOffset) throws FileNotFoundException, IOException;
	
	/**
	 * Creates a fake header string as if the platform created it.  
	 * @param parameters	The list of parameter infos.
	 * @param timestampMillis	The timestamp that may appear in the header as the starting time.
	 * @return	A string that contains a fake header according to the rules of the platform.
	 */
	public String getHeader(List<? extends AbstractParameterInfo> parameters, long timestampMillis);
	
	/**
	 * Creates a fake footer string as if the platform created it.  
	 * @param parameters	The list of parameter infos.
	 * @param timestampMillis	The timestamp that may appear in the footer as the ending time.
	 * @return	A string that contains a fake footer according to the rules of the platform.
	 */
	public String getFooter(List<? extends AbstractParameterInfo> parameters, long timestampMillis);
	
}
