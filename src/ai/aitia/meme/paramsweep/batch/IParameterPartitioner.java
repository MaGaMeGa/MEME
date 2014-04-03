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
package ai.aitia.meme.paramsweep.batch;

import java.util.Iterator;
import java.util.List;

import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;

/** This interface defines how a given parameter tree is partitioned
 *  so that it can be used for batch running.
 */
public interface IParameterPartitioner {
	
	//----------------------------------------------------------------------------------------------------
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/** Partitions a parameter tree suitable for individual running. 
	 *  An element of the iterator is a list of constant parameters 
	 *  (describes a single run). The extent of possible combinations
	 *  that iterator provides may depend on - therefore it is part of - 
	 *  the platform.
	 *  @param params the parameter tree
	 */
	public Iterator<List<AbstractParameterInfo>> partition(ParameterTree params);
	
	
}
