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
package ai.aitia.meme.paramsweep.platform;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListModel;

import javassist.ClassPool;

import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;

/** Interface that provides information for the platform by
 *  the Parameter Sweep Wizard (i. e. from the user).<p> 
 * 
 * 	Note: There are some platform-specific methods in this 
 *        interface, because some platforms need more informations
 *        than others.<br>
 *        This interface is open. This means new methods can be
 *        added to if a new platform needs it (and the Parameter
 *        Sweep Wizard can provide the desired information.
 *
 */
public interface IPSWInformationProvider {
	
	//====================================================================================================
	// methods

	//====================================================================================================
	// for all platforms
	
	//----------------------------------------------------------------------------------------------------
	/** The path to the model (directory). This information can be used to
	 *  locate the model.
	 */
	public String getModelRoot();
	
	//----------------------------------------------------------------------------------------------------
	/** The entry point of the model. */ 
	public String getEntryPoint();
	
	//----------------------------------------------------------------------------------------------------
	/** Returns whether the simulation will run on the local machine or not. */  
	public boolean isLocalRun();
	
	//----------------------------------------------------------------------------------------------------
	/** Returns the description of the model. It could be 'null'. */
	public String getDescription();

	
	//====================================================================================================
	// for RepastJ
	
	//----------------------------------------------------------------------------------------------------
	/** The class file of the model. */
	public File getModelFile();
	
	//----------------------------------------------------------------------------------------------------
	/** The class loader that was used to load the model class. */
	public ClassLoader getCustomClassLoader();
	
	//----------------------------------------------------------------------------------------------------
	/** Returns the class cache that belongs to the model. */
	public Map<String,Class<?>> getClassCache();
	
	//----------------------------------------------------------------------------------------------------
	/** Returns the Javassist class pool that belongs to the model. */ 
	public ClassPool getClassPool();
	
	//----------------------------------------------------------------------------------------------------
	/** Returns the class path that belongs to the model. */
	public DefaultListModel getClassPathListModel();
	
	//----------------------------------------------------------------------------------------------------
	/** Returns whether 'rngSeed' is a parameter. */
	public boolean rngSeedAsParameter();

	//----------------------------------------------------------------------------------------------------
	/** Returns whether model generation also produces source code. */
	public boolean writeSource();

	//----------------------------------------------------------------------------------------------------
	/** Returns the recorders. Note that information is only available in
	 *  the {@link Platform#prepareModel(IPSWInformationProvider)} method.
	 */
	public List<RecorderInfo> getRecorders();
	
	//----------------------------------------------------------------------------------------------------
	/** Returns the stopping condition. Note that information is only available in
	 *  the {@link Platform#prepareModel(IPSWInformationProvider)} method.
	 */
	public String getStoppingCondition();
	
	//----------------------------------------------------------------------------------------------------
	/** Returns whether the defined stopping condition is a time step or a
	 *  logical condition. Note that information is only available in
	 *  the {@link Platform#prepareModel(IPSWInformationProvider)} method.
	 */
	public boolean isNumberStoppingCondition();
	
	//----------------------------------------------------------------------------------------------------
	/** Returns new parameters defined by the user. Note that information is
	 *  only available in the {@link Platform#prepareModel(IPSWInformationProvider)}
	 *  method.
	 */
	public List<AbstractParameterInfo> getNewParameters();
	
	//----------------------------------------------------------------------------------------------------
	/** The (RepastJ) platform can set the name of the generated class in the
	 *  {@link Platform#prepareModel(IPSWInformationProvider)} method.
	 */ 
	public void setGeneratedModelName(String generatedModelName);
	
	//====================================================================================================
	// for NetLogo 4.0.4
	
	//----------------------------------------------------------------------------------------------------
	/** The (NetLogo 4.0.4) platform can set some resource file with this method.
	 *  These resource file are similar than the resources set by the user however
	 *  they are not appeared on resources dialogue hence they are not removable by
	 *  the user.
	 */
	public void setAutomaticResources(List<String> resources);
	public List<String> getAutomaticResources();
}
