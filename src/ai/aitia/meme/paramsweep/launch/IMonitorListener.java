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
package ai.aitia.meme.paramsweep.launch;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;

public interface IMonitorListener {
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public void stopSimulationSignal(MonitorEvent event);
	public void killSimulationSignal(MonitorEvent event);
	public void stopCurrentRunSignal(MonitorEvent event);
	public void updateSimulationListSignal(MonitorEvent event);
	public void removeSimulationSignal(MonitorEvent event);
	public void downloadSimulationResultSignal(MonitorEvent event);
	public void importSimulationResultSignal(MonitorEvent event);
	
	public void reconnectSignal(MonitorEvent event);
	
	//================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	public static class MonitorEvent extends EventObject {
		
		//====================================================================================================
		// members
		
		List<ModelElement> elements = null;
		List<String> outputs = null;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public MonitorEvent(Object source) { super(source); }
		
		//----------------------------------------------------------------------------------------------------
		public MonitorEvent(Object source, ModelElement element, List<String> outputs) {
			super(source);
			elements = new ArrayList<ModelElement>(1);
			elements.add(element);
			this.outputs = outputs;
		}
		
		//----------------------------------------------------------------------------------------------------
		public MonitorEvent(Object source, List<ModelElement> elements) {
			super(source);
			this.elements = elements;
		}
		
		//----------------------------------------------------------------------------------------------------
		public List<ModelElement> getElements() { return elements; }
		public List<String> getOutputs() { return outputs; }
		
		//----------------------------------------------------------------------------------------------------
		public ModelElement getElement() { 
			if (elements == null || elements.size() == 0) return null;
			return elements.get(0);
		}
	}
	
	//--------------------------------------------------------------------------------
	/** This class represents a model in the Finished simulations list. */
	public static class ModelElement implements Comparable<ModelElement> {
		
		//====================================================================================================
		// members
		
		/** The name of the model. */
		private String modelName = null;
		/** The description of the model. */
		private String description = null;
		/** The directory of the model on the server computer. */
		private String directory = null;
		/** Flag that determines whether the simulation is finished with or without error. */
		private boolean errorFlag = false;
		/** The names of the output files of the model. */
		private List<String> outputs = null;
		
		private PlatformType platform = null;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public ModelElement(String modelName, String description, String directory,	boolean errorFlag, List<String> outputs, PlatformType platform) {
			this.modelName = modelName;
			this.description = description;
			this.directory = directory;
			this.errorFlag = errorFlag;
			this.outputs = outputs;
			this.platform = platform;
		}
		
		//----------------------------------------------------------------------------------------------------
		public String getDescription() { return description; }
		public String getDirectory() { return directory; }
		public String getModelName() { return modelName; }
		public List<String> getOutputs() { return outputs; }
		public boolean isWrong() { return errorFlag; }
		public PlatformType getPlatform() { return platform; }
		
		//----------------------------------------------------------------------------------------------------
		@Override public String toString() { return Utilities.getModelNameAndTimeStamp(modelName)[0]; }
		
		//----------------------------------------------------------------------------------------------------
		public String oldToString() {
			String s = errorFlag ? "ERROR: " : "";
			return s + Util.restoreNameAndTimeStamp(modelName);
		}
		
		//====================================================================================================
		// implemented interfaces
		
		//----------------------------------------------------------------------------------------------------
		public int compareTo(ModelElement o) { return this.toString().compareTo(o.toString()); }
	}
}
