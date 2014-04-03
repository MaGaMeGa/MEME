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

/**
 * General declarations for all types of plugins
 */
public interface IPlugin 
{
	/** Argument type for the static onPluginLoad() method */ 
	public interface IOnPluginLoad{
	}

	/** 
	 * Base type for arguments of plugin methods.
	 * The Map facilities of this object are intended to be used (for example) <ul> 
	 * <li> to store plugin-specific data between subsequent invocations
	 *      of the plugin's methods
	 * <li> to return multiple values from a plugin method
	 * </ul>
	 * Note that the lifetime of this object varies amongst plugin types
	 * and uses. See the documentation of the particlar plugin type for 
	 * detailed specification.
	 */
	@SuppressWarnings("serial")
	public interface IContext extends java.util.Map<Object, Object> {
	}

	/** Returns the localized (display-)name of the plugin */
	public String getLocalizedName();
}
