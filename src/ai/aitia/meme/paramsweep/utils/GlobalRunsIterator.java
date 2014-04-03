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

import java.util.Iterator;
import java.util.List;

import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;

public class GlobalRunsIterator implements Iterator<List<AbstractParameterInfo>> {
	
	//====================================================================================================
	// members
	
	private long globalRuns;
	private long actRun;
	private Iterator<List<AbstractParameterInfo>> innerIterator;
	private List<AbstractParameterInfo> current = null;
	
	//====================================================================================================
	// methods 
	
	//----------------------------------------------------------------------------------------------------
	public GlobalRunsIterator(Iterator<List<AbstractParameterInfo>> innerIterator, long globalRuns) {
		this.innerIterator = innerIterator;
		this.globalRuns = globalRuns;
		this.actRun = globalRuns;
	}
	
	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public boolean hasNext() { return innerIterator.hasNext() || actRun < globalRuns; }

	//----------------------------------------------------------------------------------------------------
	public List<AbstractParameterInfo> next() {
		if (actRun == globalRuns) {
			actRun = 0;
			current = innerIterator.next();
		}
		actRun++;
		return current;
	}

	//----------------------------------------------------------------------------------------------------
	public void remove() { throw new UnsupportedOperationException(); }
}
