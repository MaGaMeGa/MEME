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

public abstract class AbstractResultFileTool implements IResultFileTool {

	//----------------------------------------------------------------------------------------------------
	public void splitFileOnlyData(File resultFile, File dataFile,
			long runNumberOffset) throws FileNotFoundException, IOException {
		if (dataFile == null) throw new IllegalArgumentException("You must provide a File for output.");
		splitFile(resultFile, false, null, false, null, true, dataFile, false, null, runNumberOffset);
	}


}
