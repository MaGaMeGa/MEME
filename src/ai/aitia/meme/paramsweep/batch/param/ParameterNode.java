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
package ai.aitia.meme.paramsweep.batch.param;

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;

/** Node definition for the parameter tree. */
public class ParameterNode extends DefaultMutableTreeNode {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -7320747411911275598L;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	ParameterNode() { super(); }
	
	//----------------------------------------------------------------------------------------------------
	public <R> ParameterNode(AbstractParameterInfo<R> p) { super(p); }

	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public <R> AbstractParameterInfo<R> getParameterInfo() { return (AbstractParameterInfo<R>) getUserObject(); }
	
	//----------------------------------------------------------------------------------------------------
	public <R> void setParameterInfo(AbstractParameterInfo<R> pi) {
		if (pi == null) 
			throw new IllegalArgumentException("Parameter is null.");
		setUserObject(pi);
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Clones the tree starting at this node. */
	public ParameterNode deepClone() {
		ParameterNode n = new ParameterNode();
		clone(this,n);
		return n;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		if (getParameterInfo() != null) {
			AbstractParameterInfo pi = getParameterInfo();
			return pi.toString();			
		}
		return "null;";
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
	private void clone(ParameterNode src, ParameterNode dst) {
        Enumeration<ParameterNode> e = src.children();
        ParameterNode n = null;
        while (e.hasMoreElements()) {
            n = e.nextElement();
            ParameterNode n2 = (ParameterNode) n.clone();
            n2.setUserObject(n.getParameterInfo().clone());
            dst.add(n2);
            if (!n.isLeaf()) 
                clone(n,n2);
        }
    }
}
