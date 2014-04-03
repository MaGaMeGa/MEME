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
package ai.aitia.meme.paramsweep.platform.custom.impl;

import java.util.ArrayList;

import ai.aitia.meme.paramsweep.batch.IBatchListener;
import ai.aitia.meme.paramsweep.platform.custom.ICustomModel;

public interface ICustomGeneratedModel extends ICustomModel {

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public void aitiaGenerated_addBatchListener(IBatchListener listener); 
	public void aitiaGenerated_removeBatchListener(IBatchListener listener);
	
	public void aitiaGenerated_setRun(long run);
	public long aitiaGenerated_getRun();
	
	public void aitiaGenerated_setConstantParameterNames(ArrayList constants); // ArrayList<String>
	public ArrayList aitiaGenerated_getConstantParameterNames(); // ArrayList<String>
	
	public void aitiaGenerated_setMutableParameterNames(ArrayList mutables); // ArrayList<String>
	public ArrayList aitiaGenerated_getMutableParameterNames(); // ArrayList<String>
	
	public void aitiaGenerated_writeEnd();
}
