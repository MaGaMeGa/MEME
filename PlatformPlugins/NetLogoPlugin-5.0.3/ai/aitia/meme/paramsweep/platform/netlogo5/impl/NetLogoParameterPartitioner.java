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
package ai.aitia.meme.paramsweep.platform.netlogo5.impl;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterNode;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;
import ai.aitia.meme.paramsweep.util.DefaultParameterPartitioner;
import ai.aitia.meme.paramsweep.utils.GlobalRunsIterator;

public class NetLogoParameterPartitioner extends DefaultParameterPartitioner {

	//====================================================================================================
	// members
	
	protected long globalRuns = 1;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	@Override
	public Iterator<List<AbstractParameterInfo>> partition(final ParameterTree params) {
		if (params.isPlain())
			return getIteratorForPlainTree(params);
		final AbstractParameterInfo<?> aParameter = params.breadthFirstEnumeration().nextElement().getParameterInfo();
		if (aParameter.getRunNumber() == 1)
			return super.partition(params);
		globalRuns = aParameter.getRunNumber();
		final ParameterTree cloneTree = params.clone();
		final Enumeration<ParameterNode> nodes = cloneTree.breadthFirstEnumeration();
		while (nodes.hasMoreElements()) {
			final ParameterNode node = nodes.nextElement();
			node.getParameterInfo().setRunNumber(1);
		}
		return new GlobalRunsIterator(super.partition(cloneTree),globalRuns);
	}

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	private Iterator<List<AbstractParameterInfo>> getIteratorForPlainTree(final ParameterTree tree) {
		return new Iterator<List<AbstractParameterInfo>>() { // anonymous class
			private boolean finished = false;
			private int actRun = 0;
			
			public boolean hasNext() { return !finished; }

			public List<AbstractParameterInfo> next() {
				final Enumeration<ParameterNode> nodes = tree.breadthFirstEnumeration();
				final List<AbstractParameterInfo> params = new ArrayList<AbstractParameterInfo>();
				while (nodes.hasMoreElements()) 
					params.add(nodes.nextElement().getParameterInfo());
				actRun++;
				if (params.isEmpty() || params.get(0).getRunNumber() == actRun)
					finished = true;
				return params;
			}

			public void remove() {}
		};
	}
}
