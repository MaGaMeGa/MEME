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

import java.util.EventListener;

/** A listener interface for handling batch related events. */
public interface IBatchListener extends EventListener {
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/** Invoked when a run has finished. */
	public void runChanged(BatchEvent event);
	
	//----------------------------------------------------------------------------------------------------
	/** Invoked when the batch has finished. */
	public void batchEnded(BatchEvent event);
	
	//----------------------------------------------------------------------------------------------------
	/** Invoked when a time step (e.g. tick in RepastJ) has finished. */ 
	public void timeProgressed(BatchEvent event);
	
	//----------------------------------------------------------------------------------------------------
	/** Invoked when the batch is interrupted. This is special event handler,
	 *  used only in distributed mode by a distributor batch controller.
	 *  Do not call in any platform specific <code>IBatchController</code> implementation. */
	public void softCancelled(BatchEvent event);
	
	//----------------------------------------------------------------------------------------------------
	/** Invoked when the batch is killed. This is special event handler,
	 *  used only in distributed mode by a distributor batch controller.
	 *  Do not call in any platform specific <code>IBatchController</code> implementation. */
	public void hardCancelled(BatchEvent event);
}
