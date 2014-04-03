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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.CtClass;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;

import _.unknown;
import ai.aitia.meme.paramsweep.classloader.RetryLoader;
import ai.aitia.meme.paramsweep.generator.InnerOperatorsInfoGenerator;
import ai.aitia.meme.paramsweep.generator.OperatorsInfoGenerator;
import ai.aitia.meme.paramsweep.gui.ScriptCreationDialog;
import ai.aitia.meme.paramsweep.gui.info.ConstantInfo;
import ai.aitia.meme.paramsweep.gui.info.GeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.InnerOperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.gui.ListOperatorGUI.CastInputPanel;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

/** This class represents graphical components for formal parameters if the 
 *  formal parameter is a numeric collection.
 */
public class CollectionParameterGUI extends JPanel implements IParameterGUI,
															  ActionListener,
															  MouseListener,
															  TreeWillExpandListener {

	//=====================================================================================
	// members

	private static final long serialVersionUID = 1L;
	
	private static List<String> bannedMethodNames = new ArrayList<String>(5);
	static {
		bannedMethodNames.add("hashCode");
		bannedMethodNames.add("toString");
		bannedMethodNames.add("clone");
		bannedMethodNames.add("getClass");
		bannedMethodNames.add("getMediaProducers");
	}
	
	/** List model of the 'Selected elements'. */
	private DefaultListModel sModel = null;
	
	/** Name of the formal parameter that belongs to this graphical component. */
	private String name = null;
	/** The list of information objects of appropriate members of the
	 * 	model that can be used in the statistic instance.
	 */
	private List<MemberInfo> availableMembers = null;
	private List<GeneratedMemberInfo> innerScripts = null;
	private List<GeneratedMemberInfo> temporaryScripts = null;
	/** The dialog that contains this component. */
	private ScriptCreationDialog parent = null;
	/** Regular expression for double numbers. */
	private	Pattern pattern = null; 
	
	private CtClass modelClass = null;
	private DefaultMutableTreeNode root = null;
	private static int generatedIdx = 0;
	
	//=====================================================================================
	// GUI members
	
	private JTextField constantField = new JTextField();
	private JButton constantButton = new JButton("Add constant");
	private JLabel nameLabel = new JLabel();
	private JTree tree = new JTree();
	private JScrollPane treeScr = new JScrollPane(tree);
	private JButton addButton = new JButton("Add");
	private JButton removeButton = new JButton("Remove");
	private JList selectedList = new JList();
	private JScrollPane selectedListScr = new JScrollPane(selectedList);
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Constructor.
	 * @param parent the dialog that contains this component
	 */
	public CollectionParameterGUI(ScriptCreationDialog parent, CtClass modelClass, List<GeneratedMemberInfo> innerScripts) {
		super();
		this.parent = parent;
		this.modelClass = modelClass;
		this.innerScripts = innerScripts;
		temporaryScripts = new ArrayList<GeneratedMemberInfo>();
		root = new DefaultMutableTreeNode(modelClass != null ? modelClass.getName() : "model");
		sModel = new DefaultListModel();
		layoutGUI();
		initialize();
	}
	
	//----------------------------------------------------------------------------------------------------
	public static int getGeneratedIdx() { return generatedIdx; }
	public static void setGeneratedIdx(int generatedIdx) { CollectionParameterGUI.generatedIdx = generatedIdx; }
	
	//=====================================================================================
	// implemented interfaces
	
	//------------------------------------------------------------------------------------
	public String getParameterName() { return name; }
	
	//----------------------------------------------------------------------------------------------------
	public void postCreation() {
		for (GeneratedMemberInfo gmi : temporaryScripts) {
			if (innerScripts.contains(gmi))
				innerScripts.remove(gmi);
			innerScripts.add(gmi);
		}
		temporaryScripts.clear();
	}
	
	//-------------------------------------------------------------------------------------
	public String checkInput() {
		if (sModel.isEmpty())
			return "missing selected element(s).";
		return null;
	}

	//-------------------------------------------------------------------------------------
	public JPanel getGUIComponent(String name, String tooltip,List<MemberInfo> availables) {
		this.name = name;
		nameLabel.setText("  " + this.name + ": ");
		if (tooltip != null && !"".equals(tooltip.trim()))
			nameLabel.setToolTipText(tooltip);
		this.availableMembers = availables;
		initializeTree();
		return this;

	}

	//-------------------------------------------------------------------------------------
	public Object getInput() {
		List<MemberInfo> result = new ArrayList<MemberInfo>(sModel.getSize());
		for (int i = 0;i < sModel.getSize();++i) {
			MemberInfo mi = (MemberInfo) sModel.get(i);
			result.add(mi);
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void buildContent(List<MemberInfo> buildBlock) throws CannotLoadDataSourceForEditingException {
		if (buildBlock.size() > 0) {
			for (MemberInfo mi : buildBlock) {
				if (mi instanceof InnerOperatorGeneratedMemberInfo) {
					InnerOperatorGeneratedMemberInfo _mi = (InnerOperatorGeneratedMemberInfo) mi;
					MemberInfo parentInfo = _mi.getParentInfo();
					ParameterNode node = findNode(parentInfo);
					if (node == null)
						throw new CannotLoadDataSourceForEditingException("invalid building block: " + mi.getName());
					TreePath path = new TreePath(node.getPath());
					if (node.getNodeType() == NodeType.COLLECTION) { 
						MemberInfo realParentInfo = (MemberInfo) node.getUserObject();
						realParentInfo.setInnerType(parentInfo.getInnerType());
					}
					if (tree.isExpanded(path)) 
						tree.collapsePath(path);
					node.removeAllChildren();
					node.add(new DefaultMutableTreeNode("<<loading...>>"));
					fireTreeStructureChanged(path);
					tree.expandPath(path);
					sModel.addElement(_mi);
					if (!temporaryScripts.contains(_mi))
						temporaryScripts.add(_mi);
				} else if (mi instanceof ConstantInfo) {
					ConstantInfo _mi = (ConstantInfo) mi;
					ParameterNode node = findNode(_mi);
					if (node == null) {
						node = new ParameterNode(_mi,NodeType.PRIMITIVE,true);
						root.add(node);
						fireTreeStructureChanged(new TreePath(root));
					}
					sModel.addElement(_mi);
				} else {
					ParameterNode node = findNode(mi);
					if (node == null)
						throw new CannotLoadDataSourceForEditingException(mi.getName() + " is missing");
					MemberInfo realInfo = (MemberInfo) node.getUserObject();
					realInfo.setInnerType(mi.getInnerType());
					sModel.addElement(mi);
				}
			}
			return;
		}
		throw new IllegalStateException("missing MemberInfo objects");
	}
	
	//-------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if ("ADD".equals(command)) {
			TreePath path = tree.getSelectionPath();
			if (path != null) {
				ParameterNode node = (ParameterNode) path.getLastPathComponent();
				if (!parent.warning(!node.isAddable(),"The selected component cannot be a part of a number collection",ScriptCreationDialog.WARNING,true)) {
					MemberInfo info = node.getMemberInfo();
					if (!sModel.contains(info))
						sModel.addElement(info);
					tree.clearSelection();
				}
			}
		} else if ("REMOVE".equals(command)) {
			Object[] selected = selectedList.getSelectedValues();
			for (Object obj : selected) {
				sModel.removeElement(obj);
				if (obj instanceof InnerOperatorGeneratedMemberInfo) {
					temporaryScripts.remove(obj);
					innerScripts.remove(obj);
				}
			}
		} else if ("CONSTANT".equals(command)) {
			String text = constantField.getText().trim();
			if (text == null || "".equals(text))
				return;
			boolean valid = isValidNumber(text);
			if (!parent.warning(!valid,"Invalid constant: " + text,ScriptCreationDialog.WARNING,true)) {
				double d = Double.parseDouble(text.trim());
				ConstantInfo info = new ConstantInfo(d);
				ParameterNode newNode = new ParameterNode(info,NodeType.PRIMITIVE,true);
				if (tree.getModel().getIndexOfChild(root,newNode) < 0) {
					root.add(newNode);
					fireTreeStructureChanged(new TreePath(root));
				}
			}
			constantField.setText("");
		}
	}

	//-------------------------------------------------------------------------------------
	public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
		if (modelClass != null) {
			TreePath path = event.getPath();
			List<MemberInfo> members = new ArrayList<MemberInfo>();
			ParameterNode node = (ParameterNode) path.getLastPathComponent();
			if (node.getChildCount() == 1 && node.getFirstChild() instanceof DefaultMutableTreeNode) {
				node.removeAllChildren();
				MemberInfo info = node.getMemberInfo();
				Class<?> accessType = node.getNodeType() == NodeType.OBJECT ? info.getJavaType() : info.getInnerType();
				Field[] fields = Utilities.getAccessibleFields(modelClass,accessType);
				for (Field field : fields) {
					if (field.getName().equals("serialVersionUID")) continue;
					MemberInfo mi = new MemberInfo(field.getName(),Utilities.toTypeString1(field.getType()),field.getType());
					NodeType nodeType = calculateNodeType(mi);
					if (!members.contains(mi) && (nodeType == NodeType.PRIMITIVE || nodeType == NodeType.P_ARRAY)) {
						members.add(mi);
						node.add(new ParameterNode(mi,nodeType,false));
					}
				}
				Method[] methods = Utilities.getAccessibleMethods(modelClass,accessType);
				for (Method method : methods) {
					if (method.isSynthetic() || bannedMethodNames.contains(method.getName())) continue;
					if (method.getParameterTypes().length == 0 && !method.getReturnType().equals(Void.TYPE)) {
						MemberInfo mi = new MemberInfo(method.getName() + "()",Utilities.toTypeString1(method.getReturnType()),method.getReturnType());
						NodeType nodeType = calculateNodeType(mi);
						if (!members.contains(mi) && (nodeType == NodeType.PRIMITIVE || nodeType == NodeType.P_ARRAY)) {
							members.add(mi);
							node.add(new ParameterNode(mi,nodeType,false));
						}
					}
				}
				tree.revalidate();
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
			if (e.getComponent().equals(tree)) {
				TreePath path = tree.getSelectionPath();
				if (path != null) {
					Object lastPathComponent = path.getLastPathComponent();
					if (root.equals(lastPathComponent)) return;
					addButton.doClick();
				}
			} else if (e.getComponent().equals(selectedList))
				removeButton.doClick();
		}	
	}
	
	//----------------------------------------------------------------------------------------------------
	public void mouseReleased(MouseEvent e) {
		if (!SwingUtilities.isRightMouseButton(e) || !e.getComponent().equals(tree))
			return;
		if (e.getComponent().isEnabled()) {
			TreePath path = tree.getPathForLocation(40,e.getY());
			if (path == null)
				path = tree.getPathForRow(tree.getRowCount() - 1);
			tree.setSelectionPath(path);
			
			if (path != null && path.getPathCount() == 2) {
				ParameterNode node = (ParameterNode) path.getLastPathComponent();
				MemberInfo info = node.getMemberInfo();
				if (node.getNodeType() == NodeType.COLLECTION) { // need to cast
					JDialog dialog = new JDialog(parent,true);
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
					boolean hasPreviousInnerType = info.getInnerType() != null;
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
						parent.warning(true,"Unknown class: " + classStr + ". Please use fully qualified names.",ScriptCreationDialog.WARNING,true);
						return;
					}
					if (hasPreviousInnerType) {
						List<MemberInfo> needToDelete = new ArrayList<MemberInfo>();
						for (int i = 0; i < sModel.size();++i) {
							if (sModel.get(i) instanceof InnerOperatorGeneratedMemberInfo) {
								InnerOperatorGeneratedMemberInfo _info = (InnerOperatorGeneratedMemberInfo) sModel.get(i);
								if (_info.getParentInfo().equals(info))
									needToDelete.add(_info);
							}
						}
						for (MemberInfo _info : needToDelete) {
							sModel.removeElement(_info);
							temporaryScripts.remove(_info);
							innerScripts.remove(_info);
						}
					}
				}
			}
		}
	} 
	
	//-------------------------------------------------------------------------------------
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {}
	
	//=====================================================================================
	// GUI methods
	
	//------------------------------------------------------------------------------------
	private void layoutGUI() {
		setLayout(new BorderLayout());
		JPanel tmp = FormsUtils.build("p ~ f:p:g(0.6) ~ p ~ f:p:g",
									  "01_2|" +
									  "_1_2 f:p:g|" +
									  "_132 p||" +
									  "_142|" +
									  "_1_2 f:p:g||" +
									  "_56_ p|",
									  nameLabel,treeScr,selectedListScr,
									  addButton,
									  removeButton,
									  constantField,constantButton).getPanel();
		add(tmp,BorderLayout.CENTER);
	}
	
	//------------------------------------------------------------------------------------
	private void initialize() {
		treeScr.setBorder(BorderFactory.createTitledBorder("Available elements"));
		selectedListScr.setBorder(BorderFactory.createTitledBorder("Selected elements"));
		
		tree.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		selectedList.setModel(sModel);
		selectedList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		tree.addTreeWillExpandListener(this);
		tree.addMouseListener(this);
		selectedList.addMouseListener(this);
		
		tree.setCellRenderer(new ListOperatorGUI.ListMemberTreeRenderer());
		
		constantButton.setActionCommand("CONSTANT");
		constantField.setActionCommand("CONSTANT");
		addButton.setActionCommand("ADD");
		removeButton.setActionCommand("REMOVE");
		
		GUIUtils.addActionListener(this,constantButton,constantField,addButton,removeButton);
		
		setPreferredSize(new Dimension(550,200));
		treeScr.setPreferredSize(new Dimension(270,160));
		selectedListScr.setPreferredSize(new Dimension(180,160));
		setVisible(true);
	}
	//----------------------------------------------------------------------------------------------------
		public void nameComponents(String i)
		{
	 
			constantField.setName("fld_datasource_const".concat(i));
			constantButton.setName("btn_datasource_const".concat(i));
			tree.setName("tree_datasource_paramtree".concat(i));
			addButton.setName("btn_datasource_add".concat(i));
			removeButton.setName("btn_datasource_remove".concat(i));
			selectedList.setName("lst_datasource_selected".concat(i));
		}
	 
	//=====================================================================================
	// private methods
	
	//------------------------------------------------------------------------------------
	/** Initializes the 'Available elements' list from <code>availableMembers</code>. */
	private void initializeTree() {
		if (availableMembers != null) {
			for (MemberInfo mi : availableMembers) {
				if (mi.getName().indexOf("()") > 0 && bannedMethodNames.contains(mi.getName().substring(0,mi.getName().length() - 2)))
					continue;
				NodeType nodeType = calculateNodeType(mi);
				if (nodeType == NodeType.NONE) continue;
				ParameterNode node = new ParameterNode(mi,nodeType,true);
				root.add(node);
				if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) {
					if (nodeType == NodeType.NP_ARRAY || nodeType == NodeType.OBJECT ||
					   (nodeType == NodeType.COLLECTION && mi.getInnerType() != null))
						node.add(new DefaultMutableTreeNode("<<loading...>>"));
				}
			}
			tree.setModel(new DefaultTreeModel(root));
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private NodeType calculateNodeType(MemberInfo info) {
		if (Utilities.isPrimitive(info.getJavaType())) 
			return NodeType.PRIMITIVE;
		if (unknown.class.isAssignableFrom(info.getJavaType()))
			return NodeType.UNKNOWN;
		if (Boolean.TYPE.equals(info.getJavaType()) || Boolean.class.equals(info.getJavaType()) ||
			Character.TYPE.equals(info.getJavaType()) || Character.class.equals(info.getJavaType()))
			return NodeType.NONE;
		if (info.getJavaType().isArray() && Utilities.isPrimitive(info.getJavaType().getComponentType()))
			return NodeType.P_ARRAY;
		if (info.getJavaType().isArray() && !info.getJavaType().getComponentType().isArray())
			return NodeType.NP_ARRAY;
		if (Collection.class.isAssignableFrom(info.getJavaType()))
			return NodeType.COLLECTION;
		if (Map.class.isAssignableFrom(info.getJavaType()))
			return NodeType.NONE;
		if (!info.getJavaType().isArray())
			return NodeType.OBJECT;
		return NodeType.NONE;
	}
	
	//------------------------------------------------------------------------------------
	/** Tests if <code>text</code> contains a valid number. */
	private boolean isValidNumber(String text) {
		if (text == null) return false;
		String str = text.trim();
		if ("".equals(str)) return false;
		if (pattern == null)
		  pattern = Pattern.compile("^[-]?[0-9]+[.]?[0-9]*$"); // pattern for double numbers
		Matcher m = pattern.matcher(str);
		return m.matches();
	}
	
	//----------------------------------------------------------------------------------------------------
	private Class<?> forName(String name) throws ClassNotFoundException {
    	if (parent.getClassLoader() instanceof RetryLoader) {
			RetryLoader rl = (RetryLoader) parent.getClassLoader();
			rl.stopRetry();
		}
    	try {
    		Class<?> result = Class.forName(name,true,parent.getClassLoader()); 
    		return result;
    	} finally {
    		if (parent.getClassLoader() instanceof RetryLoader) {
    			RetryLoader rl = (RetryLoader) parent.getClassLoader();
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
	private ParameterNode findNode(MemberInfo userObject) {
		for (int i = 0;i < root.getChildCount();++i) {
			ParameterNode node = (ParameterNode) root.getChildAt(i);
			MemberInfo _info = (MemberInfo) node.getUserObject();
			if (userObject.equals(_info))
				return node;
		}
		return null;
	}
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	public enum NodeType { NONE, PRIMITIVE, P_ARRAY, NP_ARRAY, OBJECT, COLLECTION, UNKNOWN }; 
	
	//----------------------------------------------------------------------------------------------------
	private class ParameterNode extends DefaultMutableTreeNode {
		
		//====================================================================================================
		// members
		
		private static final long serialVersionUID = 1L;
		
		private final NodeType nodeType;
		private final boolean firstLevel;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public ParameterNode(MemberInfo info, NodeType nodeType, boolean firstLevel) {
			super(info);
			this.nodeType = nodeType;
			this.firstLevel = firstLevel;
		}
		
		//----------------------------------------------------------------------------------------------------
		public NodeType getNodeType() { return nodeType; }
		
		//----------------------------------------------------------------------------------------------------
		public boolean isAddable() {
			switch (nodeType) {
			case PRIMITIVE : case UNKNOWN : case P_ARRAY : case COLLECTION : return true;
			default : return false;
			}
		}
		
		//----------------------------------------------------------------------------------------------------
		public MemberInfo getMemberInfo() {
			Object object = getUserObject();
			if (object == null || !(object instanceof MemberInfo))
				return null;
			if (firstLevel)
				return (MemberInfo) object;
			return createInnerMember();
		}
		//====================================================================================================
		// private methods
		
		//----------------------------------------------------------------------------------------------------
		private MemberInfo createInnerMember() {
			ParameterNode parentNode = (ParameterNode) getParent();
			MemberInfo object = parentNode.getMemberInfo();
			MemberInfo member = (MemberInfo) getUserObject();
			InnerOperatorsInfoGenerator innerOperatorsInfoGenerator = new InnerOperatorsInfoGenerator();
			switch (parentNode.getNodeType()) {
			case OBJECT : {
				InnerOperatorGeneratedMemberInfo newInfo = innerOperatorsInfoGenerator.generateSimpleInfoObject(object, member);
				temporaryScripts.add(newInfo);
				return newInfo;
				}
			case NP_ARRAY : case COLLECTION : {
				InnerOperatorGeneratedMemberInfo newInfo = innerOperatorsInfoGenerator.generateListInfoObject(object, member);
				temporaryScripts.add(newInfo);
				return newInfo;
				}
			default : return member;
			}
		}
//		private InnerOperatorGeneratedMemberInfo generateInfoObject(MemberInfo object, MemberInfo member) {
//			InnerOperatorGeneratedMemberInfo newInfo = new InnerOperatorGeneratedMemberInfo("innerList" + generatedIdx++ + "()",ArrayList.class.getSimpleName(),ArrayList.class,object);
//			if (object instanceof GeneratedMemberInfo)
//				newInfo.addReference((GeneratedMemberInfo)object);
//			newInfo.setDisplayName(Utilities.name(object) + "." + Utilities.name(member) + "[]");
//			StringBuilder code = new StringBuilder();
//			Class<?> innerType = object.getInnerType();
//			if (Utilities.isPrimitive(member.getJavaType())) {
//				code.append("java.util.ArrayList result = new java.util.ArrayList(" + object.getName());
//				if (object.getType().endsWith("[]")) {
//					code.append(".length);\n");
//					code.append("for (int i = 0;i < " + object.getName() + ".length;++i) {\n");
//					code.append(boxingIfNeed(object.getName() + "[i]." + member.getName(),member.getJavaType()));
//					code.append("}\n");
//				} else {
//					code.append(".size());\n");
//					code.append("Object[] source = " + object.getName() + ".toArray();\n");
//					code.append("for (int i = 0;i < source.length;++i) {\n");
//					code.append(boxingIfNeed("((" + innerType.getName() + ")source[i])." + member.getName(),member.getJavaType()));
//					code.append("}\n");
//				}
//				code.append("return result;\n");
//				newInfo.setInnerType(boxingType(member.getJavaType()));
//			} else {
//				code.append("java.util.ArrayList result = new java.util.ArrayList();\n");
//				if (object.getType().endsWith("[]")) {
//					code.append("for (int i = 0;i < " + object.getName() + ".length;++i) {\n");
//					code.append(member.getJavaType().getCanonicalName() + " temp = " + object.getName() + "[i]." + member.getName() + ";\n");
//					code.append("for (int j = 0;j < temp.length;++j) {\n");
//					code.append(boxingIfNeed("temp[j]",member.getInnerType()));
//					code.append("}\n}\n");
//				} else {
//					code.append("Object[] source = " + object.getName() + ".toArray();\n");
//					code.append("for (int i = 0;i < source.length;++i) {\n");
//					code.append(member.getJavaType().getCanonicalName() + " temp = ((" + innerType.getName() + ")source[i])." + member.getName() + ";\n");
//					code.append("for (int j = 0;j < temp.length;++j) {\n");
//					code.append(boxingIfNeed("temp[j]",member.getInnerType()));
//					code.append("}\n}\n");
//				}
//				code.append("return result;\n");
//				newInfo.setInnerType(boxingType(member.getInnerType()));
//			}
//			newInfo.setSource(code.toString());
//			return newInfo;
//		}


		
		// moved to ai.aitia.meme.paramsweep.utils.Util
//		//----------------------------------------------------------------------------------------------------
//		private String boxingIfNeed(String core, Class<?> memberType) {
//			StringBuilder code = new StringBuilder();
//			code.append("result.add(");
//			if (memberType.isPrimitive()) 
//				code.append("new " + boxingType(memberType).getName() + "(");
//			code.append(core);
//			if (memberType.isPrimitive())
//				code.append(")");
//			code.append(");\n");
//			return code.toString();
//		}
//		
//		//----------------------------------------------------------------------------------------------------
//		private Class<?> boxingType(Class<?> memberType) {
//			if (Byte.TYPE.equals(memberType)) return Byte.class;
//			if (Short.TYPE.equals(memberType)) return Short.class;
//			if (Integer.TYPE.equals(memberType)) return Integer.class;
//			if (Long.TYPE.equals(memberType)) return Long.class;
//			if (Float.TYPE.equals(memberType)) return Float.class;
//			if (Double.TYPE.equals(memberType)) return Double.class;
//			if (Boolean.TYPE.equals(memberType)) return Boolean.class;
//			if (Character.TYPE.equals(memberType)) return Character.class;
//			return memberType;
//		}
	}
}
