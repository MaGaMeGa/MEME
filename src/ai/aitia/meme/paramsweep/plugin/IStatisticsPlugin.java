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

import java.util.List;

/** Interface for statistic method descriptor plugins (or statistic plugins). */
public interface IStatisticsPlugin extends IPSScriptPlugin {
	/** Returns the names of the formal parameters of the statistic method. */
	public List<String> 				getParameterNames();
	/** Returns the short descriptions of the formal parameters of the statistic method. */
	public List<String>					getParameterDescriptions();
	/** Returns the types of the formal parameter of the statistic method. */
	public List<Class> 					getParameterTypes();
	/** Returns the return type of the statistic method. */
	public Class						getReturnType();
	/** Returns the fully qualified name of the statistic method. The form of a method's
	 *  fully qualified name is the following:<br>
	 *  &lt;the fully qualified name of the class that contains the method&gt;.&lt;name
	 *  of the method without braces&gt; 
	 */
	public String						getFullyQualifiedName();
}
