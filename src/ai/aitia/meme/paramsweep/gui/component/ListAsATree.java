package ai.aitia.meme.paramsweep.gui.component;

import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

public class ListAsATree extends JTree {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -6689323518542154720L;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public ListAsATree() {}

	//----------------------------------------------------------------------------------------------------
	public ListAsATree(final Object[] value) {
		super(value);
	}

	//----------------------------------------------------------------------------------------------------
	public ListAsATree(final Vector<?> value) {
		super(value);
	}

	//----------------------------------------------------------------------------------------------------
	public ListAsATree(final Hashtable<?,?> value) {
		super(value);
	}

	//----------------------------------------------------------------------------------------------------
	public ListAsATree(final TreeNode root) {
		super(root);
	}

	//----------------------------------------------------------------------------------------------------
	public ListAsATree(final TreeModel newModel) {
		super(newModel);
	}

	//----------------------------------------------------------------------------------------------------
	public ListAsATree(final TreeNode root, final boolean asksAllowsChildren) {
		super(root, asksAllowsChildren);
	}

}
