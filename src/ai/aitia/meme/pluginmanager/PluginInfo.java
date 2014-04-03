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
package ai.aitia.meme.pluginmanager;

import java.net.URLClassLoader;

//-----------------------------------------------------------------------------
/** Generic class to encapsulates informations about plugins. */
public class PluginInfo<P extends IPlugin>
{
	/** The class loader of the plugin. */
	URLClassLoader	pluginLoader;
	/** An instance of the plugin. */
	P				instance	= null;
	/** The internal name of the plugin. */
	String			internalName= null;

	public P				getInstance()					 { return instance; }
	public String			getInternalName()				 { return internalName; }
	public URLClassLoader	getLoader()						 { return pluginLoader; }
	@Override public String toString()						 { return instance.getLocalizedName(); }
	public void setInstance(P instance) 					 { this.instance = instance; }
	public void setInternalName(String internalName) 		 { this.internalName = internalName; }
	public void setPluginLoader(URLClassLoader pluginLoader) { this.pluginLoader = pluginLoader; }
}
