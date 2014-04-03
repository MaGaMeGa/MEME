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
package ai.aitia.meme.paramsweep.gui.info;

import java.io.Serializable;

/** Abstract ancestor for classes that represents different aspects of the members of
 *  the model.
 */
public abstract class NodeInfo implements Serializable,
										  Comparable<NodeInfo> {

	//==============================================================================
	// members
	
	/** Name. */
	protected String name = null;
	
	//==============================================================================
	// methods
	
	//------------------------------------------------------------------------------
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	@Override public String toString() { return name; }
	/** Costructor.
	 * @param name name
	 */
	protected NodeInfo(String name) { this.name = name; }
	public int compareTo(NodeInfo o) { return this.name.compareTo(o.name); }
	@Override public boolean equals(Object o) {
		if (o instanceof NodeInfo) {
			NodeInfo that = (NodeInfo)o;
			return compareTo(that) == 0;
		}
		return false;
	}
}
