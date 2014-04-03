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
package ai.aitia.meme.gui;

import static java.util.Collections.binarySearch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.ConnChangedEvent;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.AbstractResultsDb.BatchInfo;
import ai.aitia.meme.events.*;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.gui.lop.RQ;
import ai.aitia.meme.utils.Utils;

/** This class represents the model of the Results tree.*/
@SuppressWarnings("serial")
public class ResultsTree extends javax.swing.tree.DefaultTreeModel
						  implements ai.aitia.meme.database.IConnChangedListener 
{
	private static final String NO_DATABASE = "<<no database!>>";
	/** Refresh request object. */
	private volatile RQ.Request refreshIsPending = null;
	private WeakHashMap<TreeSelectionModel, ArrayList<Long[]>> setSelectionPending = null;	// EDT only 


	//=========================================================================
	//	Event for batch node expansion
	
	/**
     * This event is fired when a batch node was expanded for the first time.
     */
 	public final BatchExpansionListeners delayedBatchExpansion = new BatchExpansionListeners();

	//-------------------------------------------------------------------------
	/** Interface for listening batch node expanding events. */ 
	public interface BatchExpansionListener extends java.util.EventListener {
		/**
		 * This method is called when a batch node was expanded for the first time,
		 * thus runs were counted and batch descriptions were loaded for every batch
		 * of a Model.
		 * @param modelNode The Node object containing the Model in question. 
		 */
		// EDT only
		public void onDelayedExpansion(Node modelNode);
	}

	//-------------------------------------------------------------------------
	/** Storage for listeners which observe batch node expanding events. */ 
	public static class BatchExpansionListeners extends Event<BatchExpansionListener, Node> {
		BatchExpansionListeners() { super(BatchExpansionListener.class, Node.class); }
		@Override protected void fire(Node msg) { super.fire(msg); }
	}

	
	//=========================================================================
	//	Constructor
	
	//-------------------------------------------------------------------------
	/**
	 *  Caller is responsible for registering 'this' in MEMEApp.getDatabase().connChanged  
	 */ 
	public ResultsTree() {
		super(new DefaultMutableTreeNode(NO_DATABASE));
	}

	//-------------------------------------------------------------------------
	/** Returns whether the connected database is empty or not. */
	public boolean isEmptyDb() {
		return (getChildCount(getRoot()) == 0);
	}

	//=========================================================================
	//	Tree refresh
	
	//-------------------------------------------------------------------------
	// May be called from the Model thread!
	public void onConnChange(ConnChangedEvent event) {
		refresh();
	}

	//-------------------------------------------------------------------------
	// May be called from the Model thread!
	/** Refreshes the tree. */
	public void refresh() {
		DefaultMutableTreeNode root = (getRoot() != null) ? (DefaultMutableTreeNode)getRoot() : null;
		final String previousDbName = (root != null) ? root.getUserObject().toString() : null;

		synchronized (this) {
		if (refreshIsPending != null) return;
		refreshIsPending = MEMEApp.LONG_OPERATION.begin("Reading list of Results", new LongRunnable() {
			java.sql.Connection	conn	= null;
			String				dbname	= NO_DATABASE;
			List<Model>			models	= null;

			// in Model thread (first)
			@Override
			public void run() {
				conn = MEMEApp.getDatabase().getConnection();
				if (conn != null) { 
					models = MEMEApp.getResultsDb().getModelsAndVersions();

					try { 
						dbname = MEMEApp.getDatabase().getSQLDialect().getDbName();
						return;
					} catch (java.sql.SQLException e) {
						MEMEApp.logException("ResultsTree.refresh().run()", e);
					}
					try { dbname = conn.getMetaData().getURL(); } 
					catch (java.sql.SQLException e) {}
				}
			}

			// in GUI thread (afterwards)
			@Override
			public void finished() {
				if (conn == null)
					dbname = NO_DATABASE;
				try {	// finally clear 'refreshIsPending'
				DefaultMutableTreeNode top = (dbname.equals(previousDbName)) ? (DefaultMutableTreeNode)getRoot() : new Node(dbname);
				top.removeAllChildren();
				if (models != null) {
					DefaultMutableTreeNode lastParent = top;
					String lastModelName = "";
					for (Model m : models) {
						String name = m.getName(); 
						if (!name.equals(lastModelName)) {
							lastParent = new Node(name);
							top.add(lastParent);
							lastModelName = name;
						}
						lastParent.add(new Node(m));	// This also creates a Batch-fetcher subnode
					}
				}
				ResultsTree.this.setRoot(top);
				}
				finally { refreshIsPending = null; }
			}
		});		
		}	// synchronized (this)
	}


	//=========================================================================
	//	Node class
	
	//-------------------------------------------------------------------------
	/** This class represents the elements of the tree. */
	@SuppressWarnings("serial")
	public class Node extends DefaultMutableTreeNode {
		private RQ.Request batchListIspending = null;		// EDT only

		public Node(String s) { super(s); }
		public Node(Model m)  {
			super(m);
			// Create a fetcher for the list of batches.
			// It will fetch the list from the database on the first access.  
			this.add(new DefaultMutableTreeNode(new Batch()));
		}
		@Override
		public String toString() { 
			if (getUserObject() instanceof Model)
				return ((Model)getUserObject()).getVersion();
			return super.toString();
		}

		/** Returns the model of the tree that contains this node. */
		public ResultsTree getTreeModel() 	{ return ResultsTree.this; }
		/** Returns whether the node is a place holder or a valid node with correct informations. */
		boolean isPlaceHolder()				{ return (getChildCount() == 0) || (getBatch(getFirstChild()) == null); }

		//---------------------------------------------------------------------
		/** This class stores information about a batch. */ 
		public class Batch {
			/** Informations about the batch. null until the data is fethed. */
			private BatchInfo	batchInfo;		// null until the data is fetched.

			public Batch()						{ batchInfo = null; }
			public Batch(BatchInfo b)			{ batchInfo = b; }

			public BatchInfo getBatchInfo()		{ return batchInfo; }
			/** Returns the node of the batch. */
			public Node getModelNode()			{ return Node.this; }
			/** Returns the model belongs to 'this' batch. */
			private Model getModel()			{ return (Model)getModelNode().getUserObject(); }

			@Override public String toString() {
				String ans = "<<loading...>>";
				if (batchInfo != null) {
					if (batchInfo.description != null && batchInfo.description.length() > 0)
						ans = String.format("% 2d: %s", batchInfo.batch, batchInfo.description);
					else
						ans = String.format("% 2d", batchInfo.batch);
				}
				else if (batchListIspending == null) {
					batchListIspending = 
					MEMEApp.LONG_OPERATION.begin("Reading information about batches...", new LongRunnable() {
						List<BatchInfo> info = null;
						
						// in Model thread (first)
						@Override
						public void run() {
							if (getModel() != null)
								info = MEMEApp.getResultsDb().getBatchInfo(getModel().getModel_id());
						}

						// in GUI thread (afterwards)
						@Override
						public void finished() {
							final Node modelNode = getModelNode();
							if (info == null) {
								// Give up waiting for batch nodes of this Model 
								concludeSetSelection(modelNode);
								return;
							}
							boolean first = true;
							ArrayList<Integer> inserted = new ArrayList<Integer>(info.size());
							for (BatchInfo b : info) {
								assert b != null;
								if (first) {
									first = false;
									batchInfo = b;
								} else {
									inserted.add(modelNode.getChildCount());
									modelNode.add(new DefaultMutableTreeNode(new Batch(b)));
								}
							}
							modelNode.getTreeModel().nodesChanged(modelNode, new int[] { 0 });
							if (!inserted.isEmpty())
								modelNode.getTreeModel().nodesWereInserted(modelNode, Utils.asIntArray(inserted));

							// Notify selection(s) about the new item(s)
							modelNode.getTreeModel().delayedBatchExpansion.fire(modelNode);

							// Extend the selection with the batch nodes we're waiting for   
							concludeSetSelection(modelNode);

							batchListIspending = null;
						}
					});
				}
				return ans;
			}
			
			/** This method updates the description of the batch in the tree (not in
			 *  the database.
			 */
			public void updateDesc(String s)	{
				if (batchInfo != null) {
					batchInfo = new BatchInfo(batchInfo.batch, batchInfo.nrOfRuns, s);
					DefaultMutableTreeNode node = getNode();
					if (node != null) getTreeModel().nodeChanged(node);
				}
			}
			/** Returns the node object belongs to 'this' batch object. */ 
			public DefaultMutableTreeNode getNode() {
				for (Object node : Utils.iterateRaw(getModelNode().children())) {
					if (node instanceof DefaultMutableTreeNode 
							&& ((DefaultMutableTreeNode)node).getUserObject() == this) {
						return (DefaultMutableTreeNode)node;
					}
				}
				return null;
			}
		} // Batch class
	} // Node class

	//=========================================================================
	//	Information about nodes

	//-------------------------------------------------------------------------
	// May be called from the Model thread!
	/** Returns the model object belongs to 'node'. */
	public static Model getModel(Object node) {
		if (node instanceof DefaultMutableTreeNode) {
			DefaultMutableTreeNode tmp = (DefaultMutableTreeNode)node; 
			node = tmp.getUserObject();
			if (node instanceof Node.Batch)
				node = ((Node.Batch)node).getModelNode().getUserObject();
		}
		return (node instanceof Model) ? (Model)node : null;
	}

	//-------------------------------------------------------------------------
	// May be called from the Model thread!
	/** Returns the batch information object belongs to 'node'. */
	public static BatchInfo getBatch(Object node) {
		if (node instanceof DefaultMutableTreeNode) {
			DefaultMutableTreeNode tmp = (DefaultMutableTreeNode)node;
			node = tmp.getUserObject();
			if (node instanceof Node.Batch)
				return ((Node.Batch)node).getBatchInfo();
		}
		return null;
	}

	//-------------------------------------------------------------------------
	/** Returns the model object identified by 'model_id'. Returns null if the
	 *  specified 'model_id' is unknown */
	public Model getModel(long model_id) {
		Node n = findModelNode(model_id);
		return (n == null) ? null : getModel(n);
	}

	//-------------------------------------------------------------------------
	/** Enum type for separating path by its last components. */
	public static enum PathType { ROOT, MODEL, VERSION, BATCH };

	//-------------------------------------------------------------------------
	/** Returns the type of the path that leads to 'node'. */ 
	public static PathType getPathType(Object node) {
		if (node != null && (node instanceof DefaultMutableTreeNode)) {
			DefaultMutableTreeNode tmp = (DefaultMutableTreeNode)node; 
			node = tmp.getUserObject();
			if (node instanceof Node.Batch) return PathType.BATCH;
			if (node instanceof Model) return PathType.VERSION;
			if (tmp.getLevel() == 0) return PathType.ROOT;
			return PathType.MODEL;
		}
		return null;
	}
	
	//=========================================================================
	//	Tree selection management

	//-------------------------------------------------------------------------
	/** To be called from TreeSelectionListener.valueChanged() (i.e.\ when the selection changes) */
	// EDT only
	public static void adjustSelection(TreeSelectionModel ts, TreePath current) {
		TreePath[] paths = ts.getSelectionPaths();
		ArrayList<TreePath> more = new ArrayList<TreePath>(); 
		for (int i = (paths == null) ? -1 : paths.length - 1; i >= 0; --i) {
			TreePath p = paths[i];
			switch (getPathType(p.getLastPathComponent())) {
			 	case MODEL :
			 		for (Object child : Utils.iterateRaw(((TreeNode)p.getLastPathComponent()).children())) {
			 			TreePath versionPath = p.pathByAddingChild(child);
			 			if (!ts.isPathSelected(versionPath)) {
			 				more.add(versionPath);
			 				doForVersionNode(versionPath, more);
			 			}
			 		}
			 		break;
			 	case VERSION :
			 		doForVersionNode(p, more);
			 		// no break
			 	case BATCH : 
			 		// testverek ellenorzese: ha mind ki van szelektalva, akkor more += szulo
			 		checkAllSiblings(ts, p.getParentPath(), more);
			 		break;
			 	case ROOT : 
			 	default :
			 		break;
			}
		}
		if (!more.isEmpty()) {
			// Set the 'lead' node to the current node
			if (ts.isPathSelected(current) || more.contains(current))
				more.add(current);		// the last node becomes 'lead' (at least for DISCONTIGUOUS_TREE_SELECTION mode)
			ts.addSelectionPaths(more.toArray(new TreePath[more.size()]));
		}
	}
	
	//-------------------------------------------------------------------------
	// Hozzaadja 'collection'-hoz a 'pathToVersion' alatti batch node-okat, amennyiben
	// mar be vannak toltve. Ha meg nincsenek betoltve, akkor nem csinal semmit.
	private static void doForVersionNode(TreePath pathToVersion, ArrayList<TreePath> collection) {
 		boolean first = true;
 		for (Object child : Utils.iterateRaw(((TreeNode)pathToVersion.getLastPathComponent()).children())) {
 			if (first && isUnexpandedBatchNode(child)) break;
 			first = false;
 			collection.add(pathToVersion.pathByAddingChild(child));
 		}
	}

	//-------------------------------------------------------------------------
	/** Returns the selected model objects. Never returns null.
	 */
	public static Model[] getSelectedModels(TreeSelectionModel ts) {
		TreePath[] selection = ts.getSelectionPaths();
		int n = (selection == null) ? 0 : selection.length;
		java.util.LinkedHashSet<Model> ans = new java.util.LinkedHashSet<Model>(n);
		if (n > 0) {
			for (int i = 0; i < n; ++i) {
				Model m = getModel(selection[i].getLastPathComponent());
				if (m != null) 
					ans.add(m);
			}
		}
		return ans.toArray(new Model[ans.size()]);
	}

	//-------------------------------------------------------------------------
	/**
	 * Returns an array of long[] arrays. Every long[] array contains 1 to 3 elements.
	 * When an array is containing 1 element, it is a model_id, representing all 
	 * results in all batches of that Model.
	 * When an array is containing 2 elements, these are a model_id and a batch number,
	 * representing all results in that batch of that Model.
	 * When an array is containing 3 elements, these are model_id, batch number and run
	 * number, representing one particular result.
	 */
	public static Long[][] getSelection(TreeSelectionModel ts) {
		TreePath[] selection = ts.getSelectionPaths();
		final int n = (selection == null) ? 0 : selection.length;
		ArrayList<Long[]> tmp = new ArrayList<Long[]>(n);
		for (int i = 0; i < n; ++i) {
			Model m = ResultsTree.getModel(selection[i].getLastPathComponent());
			BatchInfo bi = ResultsTree.getBatch(selection[i].getLastPathComponent());
			if (m != null) {
				long model_id = m.getModel_id();
				@SuppressWarnings("cast") Long[] r = (bi == null) ? new Long[] { model_id } : new Long[] { model_id, (long)bi.batch };
				int idx = binarySearch(tmp, r, LONG_ARRAY_COMP);
				if (idx < 0)
					tmp.add(-idx-1, r);
				else if (r.length < tmp.get(idx).length)	// replace to a shorter, if possible 
					tmp.set(idx, r);
			}
		}
		return tmp.toArray(new Long[tmp.size()][]);
	}

	/** 
	 * This comparator returns 0 when one array is prefix of the other.
	 * Kihasznaljuk, hogy ha egy modellen belul nem az osszes batch van kivalasztva,
	 * akkor maga a modell nincs kivalasztva (mert amikor a modell ki van valasztva,
	 * akkor minden batch ki van valasztva - errol {@link #adjustSelection(TreeSelectionModel, TreePath)}
	 * gondoskodik).
	 */
	public static final Comparator<Long[]> LONG_ARRAY_COMP = new Comparator<Long[]>() {
		public int compare(Long[] o1, Long[] o2) { 
			int ans = Utils.lex(o1, o2, false);
			return (Math.abs(ans) == 2) ? 0 : ans;
		}
	};

	//-------------------------------------------------------------------------
	/**
	 * Selects the specified nodes asynchronously.
	 * If some of the batch nodes need loading, the method returns before the 
	 * selection contains all of the nodes. The caller is responsible for 
	 * expanding the paths returned in 'toExpand' - this will initiate the
	 * loading of missing batches.
	 */
	// EDT only
	public void setSelection(TreeSelectionModel ts, Long[][] selection, ArrayList<TreePath> toExpand) {
		LinkedHashSet<TreePath> select = new LinkedHashSet<TreePath>(selection.length);
		HashSet<Long> selectedModels = new HashSet<Long>(selection.length);
		HashSet<TreeNode> nameNodes = new HashSet<TreeNode>();
		ArrayList<Long[]> pending = new ArrayList<Long[]>(); 
		for (int i = 0; i < selection.length; ++i) {
			Long[] l = selection[i];
			if (l == null || l.length == 0) continue;
			Node n = findModelNode(l[0]);		// 'n' is a version node
			if (n == null) continue;			// there's no such Model: ignore it 

			nameNodes.add(n.getParent());

			if (l.length == 1) {
				selectedModels.add(l[0]);
				select.add(new TreePath(n.getPath()));
				continue;
			}
			// Expand this version node because not all of its batches are selected.
			// If this node has never been expanded yet, its batch children are not
			// loaded, thus have no node, and therefore cannot be selected right now.
			// In this case loading will start when the caller expands this 'n' version 
			// node. Don't forget that it is important to delay this loading until the
			// selected batches are registered in 'setSelectionPending' (see below,
			// setSelectionPending.put()), otherwise the loader will not add them 
			// into the selection.
			toExpand.add(new TreePath(n.getPath()));
			if (n.isPlaceHolder()) {
				// This batch is not loaded yet: register it in 'setSelectionPending' 
				pending.add(l);
			} else {
				long batch = l[1];
		 		for (Object child : Utils.iterateRaw(n.children())) {	// look for this batch's node
		 			BatchInfo bi = getBatch(child);
		 			if (bi != null && bi.batch == batch) {
		 				select.add(makePath(n, child));
		 				break;
		 			}
		 		}
		 		// if there's no node for the requested batch - despite the fact
		 		// that the Model's batches are loaded - ignore the request
			}
		}
		// The selection will be further extended when missing batch nodes are loaded,
		// see in concludeSetSelection().
		//
		if (!pending.isEmpty()) {
			if (setSelectionPending == null)
				setSelectionPending = new WeakHashMap<TreeSelectionModel, ArrayList<Long[]>>(); 
			setSelectionPending.put(ts, pending);
		} else if (setSelectionPending != null) {
			setSelectionPending.remove(ts);
		}
		// Add name nodes to the selection where all the versions are selected.
		// Expand name nodes where not all versions are selected.
		A:
		for (TreeNode nameNode : nameNodes) {
			TreePath namePath = new TreePath( ((DefaultMutableTreeNode)nameNode).getPath() );
			for (Object child : Utils.iterateRaw(nameNode.children())) {
				Model m = getModel(child);
				if (m != null && !selectedModels.contains(m.getModel_id())) {
					toExpand.add(namePath);
					continue A;
				}
			}
			select.add(namePath);
		}
		ts.setSelectionPaths(select.toArray(new TreePath[select.size()]));
	}

	//-------------------------------------------------------------------------
	/** Creates a path from the path of 'parent', then extends this with 'child'. */
	public static TreePath makePath(DefaultMutableTreeNode parent, Object child) {
		return new TreePath(parent.getPath()).pathByAddingChild(child);
	}

	//-------------------------------------------------------------------------
	/** Extends the selection(s) with the batch nodes we are waiting for. */
	// EDT only
	private void concludeSetSelection(Node modelNode) {
		Model m = getModel(modelNode);
		if (setSelectionPending == null || m == null) return;

		HashMap<Long, Object> currentBatches = new HashMap<Long, Object>();
 		for (Object child : Utils.iterateRaw(modelNode.children())) {
 			BatchInfo bi = getBatch(child);
 			if (bi != null)
 				currentBatches.put(Long.valueOf(bi.batch), child);
 		}
 		boolean loadFailed = currentBatches.isEmpty(); 

		long model_id = m.getModel_id();
		Iterator<Entry<TreeSelectionModel, ArrayList<Long[]>>> spit = 
			setSelectionPending.entrySet().iterator();
		while (spit.hasNext()) {
			Entry<TreeSelectionModel, ArrayList<Long[]>> e = spit.next();
			TreeSelectionModel ts = e.getKey();	// make sure gc will not clear e.getKey() while we're using it here 
			if (ts == null) { spit.remove(); continue; }
			ArrayList<Long[]> pending = e.getValue();
			ArrayList<TreePath> select = new ArrayList<TreePath>(pending.size());
			for (ListIterator<Long[]> it = pending.listIterator(); it.hasNext(); ) {
				Long[] l = it.next();
				if (l[0] == model_id) {
					if (loadFailed)
						it.remove();	// give up waiting for batch nodes of 'modelNode'
					else {
						Object found = currentBatches.get(l[1]);
						if (found != null) {
			 				select.add(makePath(modelNode, found));
			 				it.remove();
						}
					}
				}
			}
			if (!select.isEmpty())
				ts.addSelectionPaths(select.toArray(new TreePath[select.size()]));

			if (pending.isEmpty())
				spit.remove();
		}
		if (setSelectionPending.isEmpty())
			setSelectionPending = null;
	}

	//-------------------------------------------------------------------------
	/** Returns the node belongs to the specified model (with 'model_id'). */
	private Node findModelNode(long model_id) {
		Object root = getRoot();
		for (int i = 0, n = getChildCount(root); i < n; ++i) {
			Object name = getChild(root, i);
			for (int j = 0, k = getChildCount(name); j < k; ++j) {
				Object node = getChild(name, j);
				Model m = getModel(node);
				if (m != null && m.getModel_id() == model_id)
					return (Node)node;
			}
		}
		return null;
	}

	//-------------------------------------------------------------------------
	// Testverek ellenorzese: ha mind ki van szelektalva, akkor more += szulo
	private static void checkAllSiblings(TreeSelectionModel ts, TreePath parentPath, ArrayList<TreePath> more) {
		// Ha a szulo mar ki van szelektalva, akkor biztosan ki van szelektalva az osszes teso
		// (mert errol adjustSelection() gondoskodik), tehat ilyenkor folosleges ellenorizni. 
		if (ts.isPathSelected(parentPath) || more.contains(parentPath)) return;

 		for (Object child : Utils.iterateRaw(((TreeNode)parentPath.getLastPathComponent()).children())) {
 			TreePath childPath = parentPath.pathByAddingChild(child);
 			if (!ts.isPathSelected(childPath) && !more.contains(childPath)) return;	// legalabb 1 teso nincs szelektalva
 		}
 		// Minden teso szelektalva van: 
 		more.add(parentPath);
 		// Felterjesztjuk a valtozast:  
 		parentPath = parentPath.getParentPath();
 		if (parentPath.getPathCount() > 1)
 			checkAllSiblings(ts, parentPath, more);
	}

	//-------------------------------------------------------------------------
	/** Returns true if the batch node 'o' hasn't expanded yet, false otherwise. */
	private static boolean isUnexpandedBatchNode(Object o) {
		if (o instanceof DefaultMutableTreeNode) {
			Object x = ((DefaultMutableTreeNode)o).getUserObject();
			if (x instanceof Node.Batch)
				return ((Node.Batch)x).getBatchInfo() == null;
		}
		return false;
	}
}
