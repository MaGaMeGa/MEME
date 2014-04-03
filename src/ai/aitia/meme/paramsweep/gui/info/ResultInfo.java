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
package ai.aitia.meme.paramsweep.gui.info;

import java.io.Serializable;

/** This class represents the output file of the recorder in the recorder tree. */
public class ResultInfo implements Serializable {

	//===============================================================================
	// members
	
	private static final long serialVersionUID = -7617050208119231392L;
	/** The name of the output file */
	private String file = null;
	
	//===============================================================================
	// methods
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param file the name of the output file
	 */
	public ResultInfo(String file) {
		this.file = file;
	}
	
	//-------------------------------------------------------------------------------
	public String getFile() { return file; }
	public void setFile(String file) { this.file = file; }
	
	//-------------------------------------------------------------------------------
	@Override
	public String toString() {
		return "Result file: " + file.trim();
	}
}
