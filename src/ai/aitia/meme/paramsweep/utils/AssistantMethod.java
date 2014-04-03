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
package ai.aitia.meme.paramsweep.utils;

import java.io.Serializable;

public class AssistantMethod implements Serializable {

	//====================================================================================================
	// nested type
	
	//----------------------------------------------------------------------------------------------------
	public static enum ScheduleTime { NEVER, TICK, RUN }
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 6408463142302346156L;
	public final String body;
	public final Class<?> returnValue;
	public final ScheduleTime scheduleTime;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public AssistantMethod(final String body, final Class<?> returnValue, final ScheduleTime scheduleTime) {
		this.body = body;
		this.returnValue = returnValue;
		this.scheduleTime = scheduleTime;
	}
}
