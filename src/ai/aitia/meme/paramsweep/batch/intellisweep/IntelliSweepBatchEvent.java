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
package ai.aitia.meme.paramsweep.batch.intellisweep;

import ai.aitia.meme.paramsweep.batch.BatchEvent;

/** A class of dynamic DoE method related batch events. */
public class IntelliSweepBatchEvent extends BatchEvent {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 7341421679970872227L;
	private long iteration = -1;
	private String text = "";
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public IntelliSweepBatchEvent(Object source, EventType member) { super(source,member); }
	
	//----------------------------------------------------------------------------------------------------
	/** Constructor.
	 * @param source source of the event
	 * @param member event type
	 * @param number current run number (in case of RUN_ENDED event) or the actual time value (in case of
	 * 				 STEP_ENDED event) 
	 * @param iteration the current iteration of the dynamic DOE method
	 * @param text other informations in textual format
	 */
	public IntelliSweepBatchEvent(Object source, EventType member, double number, long iteration, String text) {
		super(source,member,number);
		this.iteration = iteration;
		this.text = text;
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Constructor for ITERATION_ENDED events. */
	public IntelliSweepBatchEvent(Object source, long iteration) {
		super(source,EventType.ITERATION_ENDED,-1);
		this.iteration = iteration;
	}

	//----------------------------------------------------------------------------------------------------
	public long getIteration() { return iteration; }
	public String getText() { return text.trim(); }
}
