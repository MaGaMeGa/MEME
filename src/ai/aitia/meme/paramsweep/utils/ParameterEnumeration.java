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
package ai.aitia.meme.paramsweep.utils;

import java.util.Enumeration;
import java.util.Stack;

import javax.swing.tree.DefaultMutableTreeNode;

/** Class for iterating the nodes of the paramter tree.
 *  It implements the following strategy:<br>
 *  <ul>
 *  <li>It uses a stack as an internal storage. </li>
 *  <li>First it puts to the stack the root node of the tree. </li>
 *  <li>When the user calls next(), it takes the top node from the 
 *      stack then puts all child (in reverse order) into the stack. </li>
 *  </ul>
 */
public class ParameterEnumeration implements Enumeration<DefaultMutableTreeNode> {
	
	//==============================================================================
	// members
	
	private Stack<DefaultMutableTreeNode> stack = null;
	
	//==============================================================================
	// methods
	
	//------------------------------------------------------------------------------
	/** Constructor. 
	 * @param root the root node of the tree
	 */
	public ParameterEnumeration(DefaultMutableTreeNode root) {
		stack = new Stack<DefaultMutableTreeNode>();
		stack.push(root);
	}
	
	//==============================================================================
	// implemented interfaces

	//------------------------------------------------------------------------------
	public boolean hasMoreElements() {
		return !stack.empty();
	}

	//------------------------------------------------------------------------------
	public DefaultMutableTreeNode nextElement() {
		DefaultMutableTreeNode res = stack.pop();
		for (int i=res.getChildCount()-1;i>=0;--i)
			stack.push((DefaultMutableTreeNode)res.getChildAt(i));
		return res;
	}
}
