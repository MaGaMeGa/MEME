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

/** This class represents the recording and writing time of the recorders in the recorder tree. */
public class TimeInfo implements Serializable {

	//===============================================================================
	// members
	
	private static final long serialVersionUID = 6202180758294441931L;

	/** Enumeration type for representing recording time options. */
	public enum Mode { ITERATION ("ITERATION"),
					   ITERATION_INTERVAL ("ITERATION_INTERVAL"),
					   RUN ("RUN"),
					   CONDITION ("CONDITION");
					   
					   private String stringRep;
					   Mode(String stringRep) { this.stringRep = stringRep; }
					   @Override public String toString() { return stringRep; }
					 };
					 
	/** Enumeration type for representing writing time option. */
	public enum WriteMode { RUN ("RUN"),
							ITERATION_INTERVAL ("ITERATION_INTERVAL"),
							RECORD ("RECORD");
	
							private String stringRep;
							WriteMode(String stringRep) { this.stringRep = stringRep; }
							@Override public String toString() { return stringRep; }
	}
	
	/** Recording time. */
	private Mode type = Mode.ITERATION;
	/** The argument of the recording time in string format (if any). */
	private String arg = null;
	
	/** Writing time. */
	private WriteMode writeType = WriteMode.RUN;
	/** The argument of the writing time (if any). */
	private long writeArg = -1;
	
	//===============================================================================
	// methods
	
	//-------------------------------------------------------------------------------
	/** Constructor. 
	 * @param type the recording time
	 */
	public TimeInfo(Mode type) {
		if (type == Mode.ITERATION_INTERVAL || type == Mode.CONDITION)
			throw new IllegalArgumentException("Missing parameter");
		this.type = type;
	}
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param type the recording time
	 * @param arg the argument of the recording time in string format
	 */
	public TimeInfo(Mode type, String arg) {
		this.type = type;
		this.arg = arg;
	}
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param type the recording time in string format
	 */
	public TimeInfo(String type) {
		this(type,null);
	}
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param type the recording time in string format
	 * @param arg the argument of the recording time in string format
	 */
	public TimeInfo(String type, String arg) {
		if ("ITERATION".equals(type)) this.type = Mode.ITERATION;
		else if ("ITERATION_INTERVAL".equals(type)) this.type = Mode.ITERATION_INTERVAL;
		else if ("RUN".equals(type)) this.type = Mode.RUN;
		else if ("CONDITION".equals(type)) this.type = Mode.CONDITION;
		else 
			throw new IllegalArgumentException("Invalid 'type' parameter.");
		this.arg = arg;
	}
	
	//-------------------------------------------------------------------------------
	public Mode getType() { return type; }
	public String getArg() { return arg; }
	public WriteMode getWriteType() { return writeType; }
	public long getWriteArg() { return writeArg; }
	
	//-------------------------------------------------------------------------------
	/** Sets the recording time and its argument. */
	public void set(Mode type, String arg) { this.type = type; this.arg = arg; }
	/** Sets the writing time and its argument. */
	public void setWriteMode(WriteMode writeType, long writeArg) { this.writeType = writeType; this.writeArg = writeArg; }
	
	//-------------------------------------------------------------------------------
	/** Sets the writing time and its argument.
	 * @param writeType the writing time in string format
	 */
	public void setWriteMode(String writeType, long writeArg) {
		WriteMode mode = null;
		if ("RUN".equals(writeType))
			mode = WriteMode.RUN;
		else if ("ITERATION_INTERVAL".equals(writeType))
			mode = WriteMode.ITERATION_INTERVAL;
		else if ("RECORD".equals(writeType))
			mode = WriteMode.RECORD;
		if (mode == null)
			throw new IllegalArgumentException("Invalid 'writeType' parameter.");
		setWriteMode(mode,writeArg);
	}
	
	//-------------------------------------------------------------------------------
	@Override
	public String toString() {
		String res = "Time: ";
		switch(type) {
		case ITERATION 			: res += "at the end of every iterations"; break;
		case ITERATION_INTERVAL	: res += "after every " + arg + " iterations"; break;
		case RUN				: res += "at the end of runs"; break;
		case CONDITION			: res = "Condition: " + arg; break; 
		}
		res += " (Write time: ";
		switch(writeType) {
		case RUN				: res += "at the end of runs"; break;
		case ITERATION_INTERVAL : res += "after every " + String.valueOf(writeArg) + " iterations"; break;
		case RECORD				: res += "after every recordings"; break;
		}
		res += ")";
		return res;
	}
}
