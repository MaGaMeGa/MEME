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
package ai.aitia.meme.paramsweep.batch.output;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/** Class representing outputters/recorders. */
public class RecorderInfo implements Serializable {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -4376623033885135019L;
	private String name;
	private File outputFile;
	static public String DEFAULT_DELIMITER = "|";
	private String delimiter = DEFAULT_DELIMITER;
	
	/** Conditional code describing when to record. It's interpretation is platform-specific. */
	private String recordType;
	
	/** Conditional code describing when to persist recordings. It's interpretation is 
	 *  platform-specific. */
	private String writeType;
	
	private List<RecordableInfo> recordables;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public String getName() { return name; }
	
	//----------------------------------------------------------------------------------------------------
	public void setName(String name) {
		if (name == null || name.trim().equals("")) 
			throw new IllegalArgumentException("Name is empty or null.");
		this.name = name;
	}
	
	//----------------------------------------------------------------------------------------------------
	public File getOutputFile() { return outputFile; }
	
	//----------------------------------------------------------------------------------------------------
	public void setOutputFile(File outputFile) {
		if (outputFile == null) 
			throw new IllegalArgumentException("Output file is null.");
		this.outputFile = outputFile;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getDelimiter() { return delimiter; }
	
	//----------------------------------------------------------------------------------------------------
	public void setDelimiter(String delimiter) {
		if (delimiter == null || delimiter.trim().equals("")) 
			throw new IllegalArgumentException("Delimiter is empty or null.");
		this.delimiter = delimiter;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getRecordType() { return recordType; }
	
	//----------------------------------------------------------------------------------------------------
	public void setRecordType(String recordType) {
		if (recordType == null || recordType.trim().equals("")) 
			throw new IllegalArgumentException("Recorder type is empty or null.");
		this.recordType = recordType;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getWriteType() { return writeType; }
	
	//----------------------------------------------------------------------------------------------------
	public void setWriteType(String writeType) {
		if (writeType == null || writeType.trim().equals("")) 
			throw new IllegalArgumentException("Write type is empty or null.");
		this.writeType = writeType;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<RecordableInfo> getRecordables() { return recordables; }
	public void setRecordables(List<RecordableInfo> recordables) { this.recordables = recordables; }
}
