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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javassist.CtClass;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import ai.aitia.meme.gui.ExpandedEditor;
import ai.aitia.meme.paramsweep.classloader.RetryLoader;
import ai.aitia.meme.paramsweep.gui.ScriptCreationDialog;
import ai.aitia.meme.paramsweep.gui.info.ConstantInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin.OperatorGUIType;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;
import ai.aitia.meme.paramsweep.utils.MemberInfoList;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;
import fables.paramsweep.runtime.annotations.InnerClass;

public class ListOperatorGUI extends JPanel implements IOperatorGUI,
													   MouseListener,
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
	private ScriptCreationDialog owner = null;

	//====================================================================================================
	// GUI members
	
	private JTree tree = new JTree();
	private JScrollPane treeScr = new JScrollPane(tree);
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public ListOperatorGUI(ScriptCreationDialog owner, CtClass modelClass, MemberInfoList allMembers) {
		super();
		this.owner = owner;
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
	public OperatorGUIType getSupportedOperatorType() { return OperatorGUIType.LIST; }
	
	//----------------------------------------------------------------------------------------------------
	public String checkInput() {
		TreePath path = tree.getSelectionPath();
		if (path == null)
			return "no selection";
		if (path.getPathCount() < 3)
			return "no source selection";
		return null;
	}

	//----------------------------------------------------------------------------------------------------
	public Object[] getInput() {
		Object[] pathParts = tree.getSelectionPath().getPath();
		MemberInfo[] result = new MemberInfo[2];
		for (int i = 0;i < result.length;++i)
			result[i] = (MemberInfo) ((DefaultMutableTreeNode)pathParts[i+1]).getUserObject();
		return result;
	}

	//----------------------------------------------------------------------------------------------------
	public void buildContent(List<? extends Object> buildBlock) throws CannotLoadDataSourceForEditingException {
		MemberInfo collection = (MemberInfo) buildBlock.get(0);
		MemberInfo innerTypeMember = (MemberInfo) buildBlock.get(1);
		DefaultMutableTreeNode collectionNode = findNode(collection);
		if (collectionNode == null)
			throw new CannotLoadDataSourceForEditingException("invalid building block: " + collection.getName());
		MemberInfo realCollection = (MemberInfo) collectionNode.getUserObject();
		realCollection.setInnerType(collection.getInnerType());
		DefaultMutableTreeNode innerTypeMemberNode = findNode(collectionNode,innerTypeMember);
		if (innerTypeMemberNode == null)
			throw new CannotLoadDataSourceForEditingException("invalid building block: " + innerTypeMember.getName());
		TreePath path = new TreePath(innerTypeMemberNode.getPath());
		MemberInfo realInnerTypeMember = (MemberInfo) innerTypeMemberNode.getUserObject();
		realInnerTypeMember.setInnerType(innerTypeMember.getInnerType());
		if (tree.isExpanded(path)) 
			tree.collapsePath(path);
		tree.expandPath(path);
		tree.setSelectionPath(path);
	}	
	
	//----------------------------------------------------------------------------------------------------
	public void mouseReleased(MouseEvent e) {
		if (!SwingUtilities.isRightMouseButton(e))
			return;
		if (e.getComponent().isEnabled()) {
			TreePath path = tree.getPathForLocation(40,e.getY());
			if (path == null)
				path = tree.getPathForRow(tree.getRowCount() - 1);
			tree.setSelectionPath(path);
			
			if (path != null && path.getPathCount() == 2) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				MemberInfo info = (MemberInfo) node.getUserObject();
				if (!info.getJavaType().isArray()) { // need to cast
					JDialog dialog = new JDialog(owner,true);
					CastInputPanel panel = new CastInputPanel(dialog,info.getInnerType() == null ? null : info.getInnerType().getCanonicalName());
					dialog.setUndecorated(true);
					dialog.setContentPane(panel);
					dialog.pack();
					dialog.setLocation(tree.getLocationOnScreen().x + e.getX(),tree.getLocationOnScreen().y + e.getY()); 
					dialog.setVisible(true);
					String classStr = panel.getInput(); 
					dialog.dispose();
					if (classStr == null)
						return;
					try {
						Class innerClass = forName(classStr);
						info.setInnerType(innerClass);
						boolean isExpanded = false;
						if (tree.isExpanded(path)) {
							isExpanded = true;
							tree.collapsePath(path);
						}
						node.removeAllChildren();
						node.add(new DefaultMutableTreeNode("<<loading...>>"));
						fireTreeStructureChanged(path);
						if (isExpanded)
							tree.expandPath(path);
					} catch (ClassNotFoundException ee) {
						owner.warning(true,"Unknown class: " + classStr + ". Please use fully qualified names.",ScriptCreationDialog.WARNING,true);
						return;
					}
				}
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
		TreePath path = event.getPath();
		List<MemberInfo> members = new ArrayList<MemberInfo>();
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
		if (node.getChildCount() == 1 && ((DefaultMutableTreeNode)node.getFirstChild()).getUserObject() instanceof String) {
			node.removeAllChildren();
			MemberInfo info = (MemberInfo) node.getUserObject();
			Field[] fields = Utilities.getAccessibleFields(modelClass,info.getInnerType());
			for (Field field : fields) {
				if (field.getName().equals("serialVersionUID")) continue;
				MemberInfo mi = new MemberInfo(field.getName(),Utilities.toTypeString1(field.getType()),field.getType());
				if (!members.contains(mi)) {
					if (mi.getInnerType() == null) { // means inner type is unknown currently
						InnerClass ic = field.getAnnotation(InnerClass.class);
						if (ic != null) {
							String ic_string = ic.value();
							mi.setInnerType(Util.convertToClass(ic_string,owner.getClassLoader()));
						}
					}
					members.add(mi);
					node.add(new DefaultMutableTreeNode(mi));
				}
			}
			Method[] methods = Utilities.getAccessibleMethods(modelClass,info.getInnerType());
			for (Method method : methods) {
				if (method.isSynthetic() || bannedMethodNames.contains(method.getName())) continue;
				if (method.getParameterTypes().length == 0 && !method.getReturnType().equals(Void.TYPE)) {
					MemberInfo mi = new MemberInfo(method.getName() + "()",Utilities.toTypeString1(method.getReturnType()),method.getReturnType());
					if (!members.contains(mi)) {
						if (mi.getInnerType() == null) { // means inner type is unknown currently
							InnerClass ic = method.getAnnotation(InnerClass.class);
							if (ic != null) {
								String ic_string = ic.value();
								mi.setInnerType(Util.convertToClass(ic_string,owner.getClassLoader()));
							}
						}
						members.add(mi);
						node.add(new DefaultMutableTreeNode(mi));
					}
				}
			}
			tree.revalidate();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {}

	//====================================================================================================
	// GUI methods
	
	//----------------------------------------------------------------------------------------------------
	private void layoutGUI() {
		this.setLayout(new BorderLayout());
		this.add(treeScr,BorderLayout.CENTER);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initialize() {
		treeScr.setBorder(BorderFactory.createTitledBorder("Use right click to define the inner type of the collections."));
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setCellRenderer(new ListMemberTreeRenderer());
		tree.addTreeWillExpandListener(this);
		tree.addMouseListener(this);
		
		setPreferredSize(new Dimension(550,200));
		setVisible(true);
	}
	
	//====================================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private void initializeTree(MemberInfoList allMembers) {
		List<MemberInfo> infoList = allMembers.filterNonPrimitveCollections();
		for (MemberInfo mi : infoList) {
			if (mi.getName().indexOf("()") > 0 && bannedMethodNames.contains(mi.getName().substring(0,mi.getName().length() - 2)))
				continue;
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(mi);
			root.add(node);
			if (mi.getInnerType() != null && mi.getInnerType() != Void.TYPE)
				node.add(new DefaultMutableTreeNode("<<loading...>>"));
		}
		tree.setModel(new DefaultTreeModel(root));
	}
	
	//----------------------------------------------------------------------------------------------------
	private Class<?> forName(String name) throws ClassNotFoundException {
    	if (owner.getClassLoader() instanceof RetryLoader) {
			RetryLoader rl = (RetryLoader) owner.getClassLoader();
			rl.stopRetry();
		}
    	try {
    		Class<?> result = Class.forName(name,true,owner.getClassLoader()); 
    		return result;
    	} finally {
    		if (owner.getClassLoader() instanceof RetryLoader) {
    			RetryLoader rl = (RetryLoader) owner.getClassLoader();
    			rl.startRetry();
    		}
    	}
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Notifies all listeners of the parameters tree that the structure of the tree is
	 *  changed.
	 */
	private void fireTreeStructureChanged(TreePath path) {
        // Guaranteed to return a non-null array
        Object[] listeners = ((DefaultTreeModel)tree.getModel()).getListeners(TreeModelListener.class);
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 1;i >= 0;i--) {
                // Lazily create the event:
                if (e == null)
                    e = new TreeModelEvent(this,path);
                ((TreeModelListener)listeners[i]).treeStructureChanged(e);
        }
	}
	
	//----------------------------------------------------------------------------------------------------
	private DefaultMutableTreeNode findNode(MemberInfo info) {
		for (int i = 0;i < root.getChildCount();++i) {
			DefaultMutableTreeNode candidate = (DefaultMutableTreeNode) root.getChildAt(i);
			MemberInfo _info = (MemberInfo) candidate.getUserObject();
			if (info.equals(_info))
				return candidate;
		}
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private DefaultMutableTreeNode findNode(DefaultMutableTreeNode parent, MemberInfo info) {
		if (parent.getChildCount() == 0) 
			parent.add(new DefaultMutableTreeNode("<<loading...>>"));
		TreePath path = new TreePath(parent.getPath());
		tree.expandPath(path); // do this to create the children
		tree.collapsePath(path);
		for (int i = 0;i < parent.getChildCount();++i) {
			DefaultMutableTreeNode candidate = (DefaultMutableTreeNode) parent.getChildAt(i);
			MemberInfo _info = (MemberInfo) candidate.getUserObject();
			if (info.equals(_info))
				return candidate;
		}
		return null;
	}
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	static class ListMemberTreeRenderer extends DefaultTreeCellRenderer {
		private ImageIcon variableIcon	= ExpandedEditor.getIcon("model_version.png");
		private ImageIcon methodIcon	= ExpandedEditor.getIcon("model_name.png");
		
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
			Component com = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
															   row, hasFocus);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			Object userObject = node.getUserObject();
			Icon icon = null;
			String text = value.toString();
			if (userObject instanceof String || userObject instanceof ConstantInfo) 
				icon = null;
			else if (userObject instanceof MemberInfo) {
				MemberInfo mi = (MemberInfo) userObject;
				if (mi.getName().endsWith(")"))
					icon = methodIcon;
				else
					icon = variableIcon;
				if (!mi.getJavaType().isArray()) {
					text = value.toString() + "<";
					text += (mi.getInnerType() == null ? "?" : mi.getInnerType().getCanonicalName()) + ">";
					
					if (mi.getInnerType() == Void.TYPE)
						text = value.toString();
				}
			}
			if (icon != null) 
				((JLabel)com).setIcon(icon);
			((JLabel)com).setText(text);
			tree.setToolTipText(text);
			((JLabel)com).setToolTipText(text);
			return com;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	static class CastInputPanel extends JPanel implements ActionListener,
														  KeyListener {

		//====================================================================================================
		// members
		
		private static final long serialVersionUID = 1L;
		private static final String PLEASE_USE_TEXT = "Please use the fully qualified name.";

		private JDialog owner = null;
		private String initialValue = null;
		
		//====================================================================================================
		// GUI members
		
		private JPanel panel = null;
		private JTextField field = new JTextField();
		
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public CastInputPanel(JDialog owner, String initialValue) {
			super();
			this.owner = owner;
			this.initialValue = initialValue;
			layoutGUI();
		}
		
		//----------------------------------------------------------------------------------------------------
		public String getInput() { 
			return field.getText().trim().equals(PLEASE_USE_TEXT) ? null : field.getText().trim();
		}
		
		//====================================================================================================
		// implemented interfaces

		//----------------------------------------------------------------------------------------------------
		public void actionPerformed(ActionEvent e) {
			owner.setVisible(false);
		}
		
		//----------------------------------------------------------------------------------------------------
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				field.setText(PLEASE_USE_TEXT);
				owner.setVisible(false);
			}
		}
		
		//----------------------------------------------------------------------------------------------------
		public void keyReleased(KeyEvent e) {}
		public void keyTyped(KeyEvent e) {}
		
		//====================================================================================================
		// GUI methods
		
		//----------------------------------------------------------------------------------------------------
		private void layoutGUI() {
			panel = FormsUtils.build("p ~ p:g",
									 "[DialogBorder]|01",
									 "Element type: ",field).getPanel();
			field.setText(initialValue != null ? initialValue : PLEASE_USE_TEXT);
			field.selectAll();
			field.addActionListener(this);
			field.addKeyListener(this);
			this.setLayout(new BorderLayout());
			this.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			this.add(panel,BorderLayout.CENTER);
			this.setPreferredSize(new Dimension(300,50));
			field.grabFocus();
		}
	}

}
