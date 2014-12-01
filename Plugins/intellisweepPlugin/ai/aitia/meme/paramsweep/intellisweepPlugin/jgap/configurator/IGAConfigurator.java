/*******************************************************************************
 * Copyright (C) 2006-2014 AITIA International, Inc.
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
package ai.aitia.meme.paramsweep.intellisweepPlugin.jgap.configurator;

import java.io.Serializable;
import java.util.Map;

import javax.swing.JPanel;

import ai.aitia.meme.paramsweep.batch.IModelInformation.ModelInformationException;

public interface IGAConfigurator extends Serializable {
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * 
	 * @return the operator's name.
	 */
	public String getName();

	//----------------------------------------------------------------------------------------------------
	/**
	 * 
	 * @return a short description of the operator.
	 */
	public String getDescription();

	//----------------------------------------------------------------------------------------------------
	/**
	 * 
	 * @return the operator's settings panel.
	 */
	public JPanel getSettingsPanel();
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * @return the operator's configuration map.
	 */
	public Map<String,String> getConfiguration();
	
	//----------------------------------------------------------------------------------------------------
	public void setConfiguration(final Map<String,String> configuration) throws ModelInformationException;
}