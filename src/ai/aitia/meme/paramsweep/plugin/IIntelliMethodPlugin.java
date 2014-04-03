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

import javax.swing.JPanel;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.pluginmanager.IPlugin;

/**
 * A plugin interface for IntelliSweep methods. This interface contains the common elements of all types 
 * of IntelliSweep methods (static and dynamic).
 * @author Ferschl
 *
 */
public interface IIntelliMethodPlugin extends IPlugin{
	
	public static final int STATIC_METHOD = 1;
	public static final int DYNAMIC_METHOD = 2;
	public static final String VARYING_NUMERIC_PARAMETER = "varying_numeric_parameter";

	/**
	 * Checks and returns the ready status of the method setup process.
	 * @return the status of the IntelliSweep method setup. If everything is ready
	 * to go, then it returns <code>true</code> otherwise
	 * <code>false</code>.
	 */
	public boolean getReadyStatus();
	/**
	 * Checks and explains the ready status of the method setup process. It is 
	 * used in alert messages when a plugin is not ready, to inform the user.
	 * @return	An explanation of the ready status. It says something useful
	 * about why the plugin is not ready, and what should the user do about it.
	 */
	public String getReadyStatusDetail();

	/**
	 * Creates and returns a JPanel that contains everything to setup the 
	 * IntelliSweep method. It receives a context object that has
	 * the necessary information for this.
	 * 
	 * @param ctx	the context object.
	 * @return	a working JPanel.
	 */
	public JPanel getSettingsPanel(IIntelliContext ctx);

	/**
	 * Saves the settings of the plugin into the given XML node.
	 * @param node the Node that will contain the settings of the plugin.
	 */
	public void save(Node node);

	/**
	 * Loads the plugin settings from the given XML element.
	 * @param context The IntelliContext of the wizard.
	 * @param element	The element containing the settings of the plugin.
	 * @throws WizardLoadingException 
	 */
	public void load(IIntelliContext context, Element element) throws WizardLoadingException;

	/**
	 * Notifies the plugin that its settings are obsolete. This is called when a
	 * new model is loaded.
	 */
	public void invalidatePlugin();
	/**
	 * Returns a long description of the plugin.
	 * @return A plain text description of the method implemented in the plugin.
	 */
	public String getDescription();
	
	/**
	 * Returns the ID of the method type of the plugin.
	 * @return One of the method type IDs defined in this interface.
	 */
	public int getMethodType();
	
	/**
	 * Returns whether the plugin is implemented. If it returns false, the Paramsweep
	 * does not allow the user to continue from the Method Selection Page.
	 * @return
	 */
	public boolean isImplemented();

	/**
	 * Alters the parameter tree to carry out the experiment designed with
	 * the plugin. 
	 * @param ctx 	the context object.
	 * @return true if the method was successful
	 */
	public boolean alterParameterTree(IIntelliContext ctx);
	
}
