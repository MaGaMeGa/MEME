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
package ai.aitia.meme.paramsweep.platform.simphony.impl;

import repast.simphony.data.logging.outputter.DelimitedFormatter;
import repast.simphony.data.logging.outputter.FileOutputter;
import repast.simphony.data.logging.outputter.Formatter;
import repast.simphony.data.logging.outputter.L4JFileOutputter;
import repast.simphony.data.logging.outputter.LMDelimitedFormatter;
import repast.simphony.data.logging.outputter.OutputterUtil;
import repast.simphony.data.logging.outputter.StaticOutputterFactory;
import repast.simphony.data.logging.outputter.engine.DefaultOutputterDescriptor;
import repast.simphony.data.logging.outputter.engine.FileOutputterDescriptor;
import repast.simphony.util.SimpleFactory;

public class AitiaFileOutputterDescriptor extends DefaultOutputterDescriptor<FileOutputter> implements FileOutputterDescriptor {
	
	//====================================================================================================
	// members
	
	private String fileName;
	private boolean insertTimeToFileName;
	private boolean appendToFile;
	private boolean writeHeader;
	private Formatter formatType;
	private String delimiter = ",";
	private String formattingString;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public AitiaFileOutputterDescriptor() {
		this(null,DelimitedFormatter.DEFAULT_DELIMITER);
	}
	
	//----------------------------------------------------------------------------------------------------
	public AitiaFileOutputterDescriptor(String delimiter) {
		this(null,delimiter);
	}

	//----------------------------------------------------------------------------------------------------
	public AitiaFileOutputterDescriptor(String name,String delimiter) {
		super(name);
		this.insertTimeToFileName = true;
		this.appendToFile = false;
		this.writeHeader = true;
		this.delimiter = delimiter;
		this.formatType = new LMDelimitedFormatter(delimiter);

		super.setOutputterFactory(new SimpleFactory<FileOutputter>() {
			public FileOutputter create() {
				return createFileOutputter();
			}
		});
	}
	
	//====================================================================================================
	// assistant methods

	//----------------------------------------------------------------------------------------------------
	private FileOutputter createFileOutputter() {
		String newFileName = fileName;
		
		if (insertTimeToFileName) 
			newFileName = OutputterUtil.insertTimeVarToString(fileName);

		FileOutputter outputter = new L4JFileOutputter(newFileName,delimiter);
		outputter.setAppend(appendToFile);
		((L4JFileOutputter)outputter).setPerformCloseOnAppender(true); // inspired on Effective Java :)
		StaticOutputterFactory.initPrintOutputter(outputter,getColumns(),formatType);
		return outputter;
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean getAppendToFile() { return appendToFile; }
	public String getDelimiter() { return delimiter; }
	public String getFileName() { return fileName; }
	public String getFormattingString() { return formattingString; }
	public Formatter getFormatter() { return formatType; }
	public boolean getInsertTimeToFileName() { return insertTimeToFileName; }
	public boolean getWriteHeader() { return writeHeader; }
	
	//----------------------------------------------------------------------------------------------------
	public void setAppendToFile(boolean appendToFile) {	this.appendToFile = appendToFile; }
	public void setDelimiter(String delimiter) { this.delimiter = delimiter; }
	public void setFileName(String fileName) { this.fileName = fileName; }
	public void setFormattingString(String formattingString) { this.formattingString = formattingString; }
	public void setFormatter(Formatter formatType) { this.formatType = formatType; }
	public void setInsertTimeToFileName(boolean insertTimeToFileName) { this.insertTimeToFileName = insertTimeToFileName; }
	public void setWriteHeader(boolean writeHeader) { this.writeHeader = writeHeader; }
}
