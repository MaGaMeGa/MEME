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

import java.io.Serializable;
import java.util.Enumeration;

/** A parameter tree structure. It has a root node with an 
 *  empty parameter info set.
 */
public class ParameterTree implements Serializable {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 844305689644424081L;
	private ParameterNode root;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public ParameterTree() {
		ParameterInfo<Integer> p = new ParameterInfo<Integer>("root","root",0);
		root = new ParameterNode(p);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void addNode(AbstractParameterInfo<?> p) {
		if (p == null) 
			throw new IllegalArgumentException("Parameter is null.");
		root.add(new ParameterNode(p));
	}
	
	//----------------------------------------------------------------------------------------------------
	public void addNode(ParameterNode node) {
		if (node == null) 
			throw new IllegalArgumentException("Node is null.");
		root.add(node);
	}
	
	//----------------------------------------------------------------------------------------------------
	/** A tree is plain if all of its leaves contain constant parameters. */
	@SuppressWarnings("unchecked")
	public boolean isPlain() {
		Enumeration<ParameterNode> e = root.breadthFirstEnumeration();
		e.nextElement(); // root
		AbstractParameterInfo p;
		while (e.hasMoreElements()) {
			p = e.nextElement().getParameterInfo();
			if (p.getValueType() != AbstractParameterInfo.ValueType.CONSTANT) return false;
		}
		return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Where the structure begins. If you want all the nodes without
	 *  root, use <code>breadthFirstEnumeration()</code>. Also, for getting
	 *  children a separate method is provided.
	 */
	public ParameterNode getRoot() { return root; }
	
	//----------------------------------------------------------------------------------------------------
	/** A convenience method that returns enumeration without root. */
	@SuppressWarnings("unchecked")
	public Enumeration<ParameterNode> breadthFirstEnumeration() {
		Enumeration<ParameterNode> e = root.breadthFirstEnumeration();
		e.nextElement(); // root;
		return e;
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public Enumeration<ParameterNode> children() { return root.children(); }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public ParameterTree clone() {
		ParameterTree t = new ParameterTree();
		t.root = root.deepClone();
		t.root.setParameterInfo(root.getParameterInfo());
		return t;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		Enumeration e = root.breadthFirstEnumeration();
		String s = "";
		while (e.hasMoreElements()) 
			s += e.nextElement().toString();
		return s;
	}
}
