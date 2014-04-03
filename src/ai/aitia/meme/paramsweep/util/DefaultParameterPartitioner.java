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
package ai.aitia.meme.paramsweep.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import ai.aitia.meme.paramsweep.batch.IParameterPartitioner;
import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterNode;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;

/** The default implementation how a given parameter tree is partitioned
 *  so that it can be used for batch running.
 */
public class DefaultParameterPartitioner implements IParameterPartitioner {

	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	/** The Repast way of partitioning. Combination of subtrees goes parallel,
	 *  that is one combination of a subtree A added to one combination of B.
	 *  This is done until one of them exhausted. A single node having only a 
	 *  constant parameter is repeated until other combinations exhaust.
	 */
	public Iterator<List<AbstractParameterInfo>> partition(ParameterTree params) {
		if (params.isPlain()) 
			return getIteratorForPlainTree(params);
		
		List<SubTreeIterator> subTreeIterators = new ArrayList<SubTreeIterator>();
		List<AbstractParameterInfo> singleValued =  new ArrayList<AbstractParameterInfo>();
		ParameterNode pn = null;
		Enumeration e = params.children();
		while (e.hasMoreElements()) {
			pn = (ParameterNode) e.nextElement();
			if (pn.isLeaf() && pn.getParameterInfo().isSingleValued()) {
				// although SV parameters cause no problem, collect them separately for efficiency
				singleValued.add(pn.getParameterInfo());	
			} else 
				subTreeIterators.add(new SubTreeIterator(buildParamList(pn)));	// MVd
		}
		return new TreeIterator(subTreeIterators, singleValued);
	}
	
	//====================================================================================================
	// assistant methods

	//----------------------------------------------------------------------------------------------------
	private Iterator<List<AbstractParameterInfo>> getIteratorForPlainTree(final ParameterTree tree) {
		return new Iterator<List<AbstractParameterInfo>>() { // anonymus class
			private boolean finished = false;
			
			public boolean hasNext() { return !finished; }

			public List<AbstractParameterInfo> next() {
				Enumeration<ParameterNode> nodes = tree.breadthFirstEnumeration();
				List<AbstractParameterInfo> ps = new ArrayList<AbstractParameterInfo>();
				while (nodes.hasMoreElements()) 
					ps.add(nodes.nextElement().getParameterInfo());
				finished = true;
				return ps;
			}

			public void remove() {}
		};
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Extracts parameters from the sub-tree (node) as list using breadth-first
	 *  enumeration.
	 */
	@SuppressWarnings("unchecked")
	private List<AbstractParameterInfo> buildParamList(ParameterNode node) {
		List<AbstractParameterInfo> subTreeParams = new ArrayList<AbstractParameterInfo>();
		Enumeration<ParameterNode> e = node.breadthFirstEnumeration();
		while (e.hasMoreElements()) 
			subTreeParams.add(e.nextElement().getParameterInfo());
		return subTreeParams;
	}
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	final private class TreeIterator implements Iterator<List<AbstractParameterInfo>> {
		
		//====================================================================================================
		// members
		
		List<SubTreeIterator> subTreeIterators = new ArrayList<SubTreeIterator>();
		List<AbstractParameterInfo> singleValued =  new ArrayList<AbstractParameterInfo>();
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		TreeIterator(List<SubTreeIterator> subTreeIterators, List<AbstractParameterInfo> singleValued) {
			this.subTreeIterators = subTreeIterators;
			this.singleValued = singleValued;
		}
		
		//====================================================================================================
		// implemented interfaces
		
		//----------------------------------------------------------------------------------------------------
		/** Returns <code>true</code> if all the sub tree iterators has next. */
		public boolean hasNext() {
			for (SubTreeIterator it : subTreeIterators) {
				if (!it.hasNext()) return false;
			}
			return true;
		}
		
		//----------------------------------------------------------------------------------------------------
		/** Walks through all sub tree iterators next and combines them. */
		public List<AbstractParameterInfo> next() {
			List<AbstractParameterInfo> l = new ArrayList<AbstractParameterInfo>();
			for (SubTreeIterator it : subTreeIterators) 
				l.addAll(it.next());
			l.addAll(singleValued);
			return l;
		}

		//----------------------------------------------------------------------------------------------------
		public void remove() { throw new UnsupportedOperationException(); }

	}

	//----------------------------------------------------------------------------------------------------
	final private class SubTreeIterator implements Iterator<List<AbstractParameterInfo>> {
		
		//====================================================================================================
		// members
		
		private boolean finished = false;
		private int index = 0;
		
		/** List of parameters. */
		List<AbstractParameterInfo> params;
		
		/** List of iterators that provide SVd ParameterInfo instances. */
		List<Iterator<AbstractParameterInfo>> valueIterators;
		
		/** Result list. */
		List<AbstractParameterInfo> res;
		
		//====================================================================================================
		// methods

		//----------------------------------------------------------------------------------------------------
		@SuppressWarnings("unchecked")
		public SubTreeIterator(List<AbstractParameterInfo> params) {
			this.params = new ArrayList<AbstractParameterInfo>(params);
			res = new ArrayList<AbstractParameterInfo>(params);	// init with some values
			valueIterators = new ArrayList<Iterator<AbstractParameterInfo>>();
			// get value iterators for each parameter
			for (AbstractParameterInfo p : params) 
				valueIterators.add(p.parameterIterator());
		}

		//====================================================================================================
		// impemented interfaces
		
		//----------------------------------------------------------------------------------------------------
		public boolean hasNext() { return !finished; }

		//----------------------------------------------------------------------------------------------------
		public List<AbstractParameterInfo> next() {
			if (!hasNext()) 
				throw new NoSuchElementException();

			createNext();
			return res;
		}

		//----------------------------------------------------------------------------------------------------
		public void remove() { throw new UnsupportedOperationException(); }

		
		//====================================================================================================
		// assistant methods
		
		//----------------------------------------------------------------------------------------------------
		/** Produces a new list of single valued parameters in <code>res</code> */
		@SuppressWarnings("unchecked")
		private void createNext() {
			if (!hasNext()) return;

			Iterator<AbstractParameterInfo> it = valueIterators.get(index);
			if (it.hasNext()) {
				res.set(index,it.next());
				if (!isLast(index)) 
					index++;
				else {
					if (hasAllFinished()) 
						finished = true;	// top, no more combination
					return;
				}
			} else {
				// consumed
				// new value iterator
				valueIterators.set(index, params.get(index).parameterIterator());
				index--;
			}
			createNext();
		}

		//----------------------------------------------------------------------------------------------------
		private boolean isLast(int index) { return index == valueIterators.size() - 1; }
		
		//----------------------------------------------------------------------------------------------------
		private boolean hasAllFinished() {
			for (Iterator<AbstractParameterInfo> i : valueIterators) {
				if (i.hasNext()) return false;	// there are more
			}
			return true;
		}
	}
}
