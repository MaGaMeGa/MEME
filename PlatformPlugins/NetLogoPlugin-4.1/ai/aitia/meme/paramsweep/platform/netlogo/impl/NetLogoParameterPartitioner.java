package ai.aitia.meme.paramsweep.platform.netlogo.impl;

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
}