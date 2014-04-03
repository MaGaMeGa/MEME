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
package ai.aitia.meme.paramsweep.plugin.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javassist.CtClass;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import ai.aitia.meme.gui.ExpandedEditor;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin.OperatorGUIType;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;
import ai.aitia.meme.paramsweep.utils.MemberInfoList;
import ai.aitia.meme.paramsweep.utils.Utilities;

public class MemberSelectionOperatorGUI extends JPanel implements IOperatorGUI,
																  TreeWillExpandListener {
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 1L;
	
	private static List<String> bannedMethodNames = new ArrayList<String>(4);
	static {
		bannedMethodNames.add("hashCode");
		bannedMethodNames.add("toString");
		bannedMethodNames.add("clone");
		bannedMethodNames.add("getClass");
		bannedMethodNames.add("getMediaProducers");
	}
	
	private DefaultMutableTreeNode root = null;
	private CtClass modelClass = null;
	
	//====================================================================================================
	// GUI members
	
	private JTree tree = new JTree();
	private JScrollPane treeScr = new JScrollPane(tree);
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public MemberSelectionOperatorGUI(CtClass modelClass, MemberInfoList allMembers) {
		super();
		this.modelClass = modelClass;
		root = new DefaultMutableTreeNode(modelClass.getName());
		layoutGUI();
		initialize();
		initializeTree(allMembers);
	}
	
	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public JPanel getGUIComponent() { return this; }
	public OperatorGUIType getSupportedOperatorType() { return OperatorGUIType.MEMBER_SELECTION; }

	//----------------------------------------------------------------------------------------------------
	public String checkInput() {
		TreePath path = tree.getSelectionPath();
		if (path == null)
			return "no selection";
		if (path.getPathCount() < 3)
			return "no member selection";
		return null;
	}

	//----------------------------------------------------------------------------------------------------
	public Object[] getInput() {
		Object[] pathParts = tree.getSelectionPath().getPath();
		MemberInfo[] result = new MemberInfo[2];
		for (int i = 0;i < result.length;++i)
			result[i] = ((MemberNode)pathParts[i+1]).getMemberInfo();
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void buildContent(List<? extends Object> buildBlock) throws CannotLoadDataSourceForEditingException {
		if (buildBlock.size() == 2) {
			MemberInfo object = (MemberInfo) buildBlock.get(0);
			MemberInfo member = (MemberInfo) buildBlock.get(1);
			MemberNode objectNode = findNode(object);
			if (objectNode == null)
				throw new CannotLoadDataSourceForEditingException(object.getName() + " is missing.");
			MemberNode memberNode = findNode(objectNode,member);
			if (memberNode == null)
				throw new CannotLoadDataSourceForEditingException(member.getName() + " is missing.");
			MemberInfo realObject = (MemberInfo) objectNode.getUserObject();
			realObject.setInnerType(object.getInnerType());
			MemberInfo realMember = (MemberInfo) memberNode.getUserObject();
			realMember.setInnerType(member.getInnerType());
			TreePath path = new TreePath(memberNode.getPath());
			tree.expandPath(path);
			tree.setSelectionPath(path);
			return;
		}
		throw new IllegalStateException("too many MemberInfo");
	}

	//----------------------------------------------------------------------------------------------------
	public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {}

	//----------------------------------------------------------------------------------------------------
	public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
		TreePath path = event.getPath();
		List<MemberInfo> members = new ArrayList<MemberInfo>();
		MemberNode node = (MemberNode) path.getLastPathComponent();
		if (node.getChildCount() == 1 && ((DefaultMutableTreeNode)node.getFirstChild()).getUserObject() instanceof String) {
			node.removeAllChildren();
			MemberInfo info = node.getMemberInfo();
			Field[] fields = Utilities.getAccessibleFields(modelClass,info.getJavaType());
			for (Field field : fields) {
				if (field.getName().equals("serialVersionUID")) continue;
				MemberInfo mi = new MemberInfo(field.getName(),Utilities.toTypeString1(field.getType()),field.getType());
				if (!members.contains(mi)) {
					members.add(mi);
					node.add(new MemberNode(mi,false));
				}
			}
			Method[] methods = Utilities.getAccessibleMethods(modelClass,info.getJavaType());
			for (Method method : methods) {
				if (method.isSynthetic() || bannedMethodNames.contains(method.getName())) continue;
				if (method.getParameterTypes().length == 0 && !method.getReturnType().equals(Void.TYPE)) {
					MemberInfo mi = new MemberInfo(method.getName() + "()",Utilities.toTypeString1(method.getReturnType()),method.getReturnType());
					if (!members.contains(mi)) {
						members.add(mi);
						node.add(new MemberNode(mi,false));
					}
				}
			}
			tree.revalidate();
		}
	}
	
	//====================================================================================================
	// GUI methods
	
	//----------------------------------------------------------------------------------------------------
	private void layoutGUI() {
		this.setLayout(new BorderLayout());
		this.add(treeScr,BorderLayout.CENTER);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initialize() {
		treeScr.setBorder(BorderFactory.createTitledBorder("Select a member from one of these objects."));
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setCellRenderer(new MemberTreeRenderer());
		tree.addTreeWillExpandListener(this);
		
		setPreferredSize(new Dimension(550,200));
		setVisible(true);
	}
	
	//====================================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private void initializeTree(MemberInfoList allMembers) {
		List<MemberInfo> objectInfoList = allMembers.filterObject();
		for (MemberInfo mi : objectInfoList) {
			if (mi.getName().indexOf("()") > 0 && bannedMethodNames.contains(mi.getName().substring(0,mi.getName().length() - 2)))
				continue;
			root.add(new MemberNode(mi,true));
		}
		tree.setModel(new DefaultTreeModel(root));
	}
	
	//----------------------------------------------------------------------------------------------------
	private MemberNode findNode(MemberInfo info) {
		for (int i = 0;i < root.getChildCount();++i) {
			MemberNode candidate = (MemberNode) root.getChildAt(i);
			MemberInfo _info = (MemberInfo) candidate.getUserObject();
			if (info.equals(_info))
				return candidate;
		}
		return null;	
	}
	
	//----------------------------------------------------------------------------------------------------
	private MemberNode findNode(MemberNode parent, MemberInfo info) {
		if (parent.getChildCount() == 0)
			parent.add(new DefaultMutableTreeNode("<<loading...>>"));
		TreePath path = new TreePath(parent.getPath());
		tree.expandPath(path); // do this to create children 
		tree.collapsePath(path);
		for (int i = 0;i < parent.getChildCount();++i) {
			MemberNode candidate = (MemberNode) parent.getChildAt(i);
			MemberInfo _info = (MemberInfo) candidate.getUserObject();
			if (info.equals(_info))
				return candidate;
		}
		return null;	
	}
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	public static class MemberNode extends DefaultMutableTreeNode {

		//====================================================================================================
		// members
		
		private static final long serialVersionUID = 1L;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public MemberNode(MemberInfo info, boolean firstLevel) {
			super(info,firstLevel);
			if (firstLevel)
				this.add(new DefaultMutableTreeNode("<<loading...>>"));
		}
		
		//----------------------------------------------------------------------------------------------------
		public MemberInfo getMemberInfo() {
			Object userObject = getUserObject();
			if (userObject == null || !(userObject instanceof MemberInfo))
				return null;
			return (MemberInfo) userObject;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	private class MemberTreeRenderer extends DefaultTreeCellRenderer {
		
		private ImageIcon variableIcon	= ExpandedEditor.getIcon("model_version.png");
		private ImageIcon methodIcon	= ExpandedEditor.getIcon("model_name.png");
		
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			Component com = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
															   row, hasFocus);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			Object userObject = node.getUserObject();
			Icon icon = null;
			if (userObject instanceof String) 
				icon = null;
			else if (userObject instanceof MemberInfo) {
				MemberInfo mi = (MemberInfo) userObject;
				if (mi.getName().endsWith(")"))
					icon = methodIcon;
				else
					icon = variableIcon;
			}
			if (icon != null) 
				((JLabel)com).setIcon(icon);
			tree.setToolTipText(value.toString());
			((JLabel)com).setToolTipText(value.toString());
			return com;
		}
	}
}
