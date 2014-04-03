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

import ai.aitia.meme.paramsweep.batch.IBatchListener;

/** A listener interface to handle dynamic DoE related batch events. */
public interface IIntelliSweepBatchListener extends IBatchListener {
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	/** Invoked when an iteration of a dynamic DoE plugin has finished. This is a
	 *  special event handler, it is used only by a special class of batch controller.
	 *  Do not call in any platform specific <code>IBatchController</code> implementation. */
	public void iterationChanged(IntelliSweepBatchEvent event);
}
