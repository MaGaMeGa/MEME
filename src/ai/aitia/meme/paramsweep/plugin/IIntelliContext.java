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
package ai.aitia.meme.paramsweep.plugin;

import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.tree.DefaultMutableTreeNode;

import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.pluginmanager.IPlugin.IContext;

/**
 * The context interface for the IntelliSweep static method plugins. It provides
 * access to the parameters list and tree.
 * @author Ferschl
 */
public interface IIntelliContext extends IContext {
	
	/**
	 * Returns a list with the parameters of the model. It contains the user-added
	 * parameters as well.
	 * @return a list of the available parameters that the model has as a 
	 * <code>List</code> of <code>ParameterInfo</code>s 
	 */
	public List<ParameterInfo> getParameters();
	
	/**
	 * Returns a reference to the parameter tree that represents the Repast
	 * parameter file. The plugin can manipulate it to create the experiment
	 * design. 
	 * @return the <code>DefaultTreeModel</code> reference that contains the
	 * Repast parameter file configuration.
	 */
	public DefaultMutableTreeNode getParameterTreeRootNode();
	
	/**
	 * Returns a JButton that is responsible for bringing up the "Add new parameters" dialog.
	 * This button is originally on the Parameters page, but the IntelliSweep methods do not
	 * need that page.
	 * @return	A JButton object.
	 */
	public JButton getNewParametersButton();
	
	/**
	 * A method that returns the File object representing the directory 
	 * containing the resources files of the plugins.
	 * @return
	 */
	public File getPluginResourcesDirectory();

}
