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
package ai.aitia.meme.paramsweep.platform.simphony.impl;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterNode;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;
import ai.aitia.meme.paramsweep.util.DefaultParameterPartitioner;
import ai.aitia.meme.paramsweep.utils.GlobalRunsIterator;

public class SimphonyParameterPartitioner extends DefaultParameterPartitioner {

	//====================================================================================================
	// members
	
	protected long globalRuns = 1;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public Iterator<List<AbstractParameterInfo>> partition(ParameterTree params) {
		if (params.isPlain())
			return getIteratorForPlainTree(params);
		AbstractParameterInfo<?> aParameter = params.breadthFirstEnumeration().nextElement().getParameterInfo();
		if (aParameter.getRunNumber() == 1)
			return super.partition(params);
		globalRuns = aParameter.getRunNumber();
		ParameterTree cloneTree = params.clone();
		Enumeration<ParameterNode> nodes = cloneTree.breadthFirstEnumeration();
		while (nodes.hasMoreElements()) {
			ParameterNode node = nodes.nextElement();
			node.getParameterInfo().setRunNumber(1);
		}
		return new GlobalRunsIterator(super.partition(cloneTree),globalRuns);
	}

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private Iterator<List<AbstractParameterInfo>> getIteratorForPlainTree(final ParameterTree tree) {
		return new Iterator<List<AbstractParameterInfo>>() { // anonymous class
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
}
