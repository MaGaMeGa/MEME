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
package ai.aitia.meme.paramsweep.platform.mason.impl;

import java.util.ArrayList;
import java.util.HashMap;

import ai.aitia.meme.paramsweep.batch.IBatchListener;

public interface IMasonGeneratedModel {
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public void aitiaGenerated_addBatchListener(IBatchListener listener); 
	public void aitiaGenerated_removeBatchListener(IBatchListener listener);

	public void aitiaGenerated_addRecorderListener(MasonRecorderListener listener);
	public void aitiaGenerated_removeRecorderListener(MasonRecorderListener listener);
	
	public void aitiaGenerated_setRun(long run);
	public long aitiaGenerated_getRun();
	
	public void aitiaGenerated_setConstantParameterNames(HashMap constants); // HashMap<String,Object>
	public HashMap aitiaGenerated_getConstantParameterNames(); // HashMap<String,Object>
	
	public void aitiaGenerated_setMutableParameterNames(HashMap mutables); // HashMap<String,Object>
	public HashMap aitiaGenerated_getMutableParameterNames(); // HashMap<String,Object>
	
	public void aitiaGenerated_writeEnd();
	
	public double getCurrentStep();
	
	public void simulationStart();
	public void simulationStop();
	public void modelInitialization();
	
	public void stepEnded();

	// this is called by the MasonRecorder to directly access collectionLengthMembers
	public Object getModel();
}
