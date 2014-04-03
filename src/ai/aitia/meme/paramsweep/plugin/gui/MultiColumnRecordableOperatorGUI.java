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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;

import _.unknown;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.gui.ScriptCreationDialog;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin.OperatorGUIType;
import ai.aitia.meme.paramsweep.plugin.gui.CollectionParameterGUI.NodeType;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;
import ai.aitia.meme.paramsweep.utils.MemberInfoList;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FilteredListPanel;
import ai.aitia.meme.utils.FormsUtils;

public class MultiColumnRecordableOperatorGUI extends JPanel implements
		IMultiColumnOperatorGUI, ActionListener, FocusListener {

	private static final long serialVersionUID = 1499143282154270669L;

	private static final String PLEASE_USE_TEXT = "Class name (if not in model's package or java.lang, use the fully qualified name)";
	private static List<String> bannedMethodNames = new ArrayList<String>(4);
	static {
		bannedMethodNames.add("hashCode");
	//	bannedMethodNames.add("toString");
		bannedMethodNames.add("clone");
		bannedMethodNames.add("getClass");
		bannedMethodNames.add("getMediaProducers");
	}
	
	private DefaultMutableTreeNode root = null;
	private CtClass modelClass = null;
	private Class<?> innerClass = null;
	private ScriptCreationDialog owner = null;
	private ParameterSweepWizard wizard = null;
	private ArrayList<String> acceptableLengthList = new ArrayList<String>();

	private JComboBox collectionBox = new JComboBox();
	private JComboBox memberBox = new JComboBox();
	private JTextField innerTypeField = new JTextField();
	private JTextField recordingLengthField = new JTextField(); 
	private JTextField noDataFillerField = new JTextField();
	private JButton selectTypeButton = new JButton("...");
	private JButton selectSizeButton = new JButton("...");
	
	public MultiColumnRecordableOperatorGUI(ParameterSweepWizard wizard, ScriptCreationDialog owner, CtClass modelClass, MemberInfoList allMembers) {
		super();
		this.wizard = wizard;
		this.owner = owner;
		this.modelClass = modelClass;
		root = new DefaultMutableTreeNode(modelClass.getName());
		layoutGUI();
		initialize();
		initializeCollectionBox(allMembers);
		updateMemberBox();
	}

	private void initializeCollectionBox(MemberInfoList allMembers) {
		List<MemberInfo> list = null;
		list = allMembers.filterCollection();
		DefaultComboBoxModel model = new DefaultComboBoxModel(list.toArray(new MemberInfo[0]));
		collectionBox.setModel(model);
		if (collectionBox.getSelectedIndex() != -1) {
			MemberInfo actual = (MemberInfo) collectionBox.getSelectedItem();
			innerTypeField.setText(actual.getInnerType() !=  null ? actual.getInnerType().getName() 
																  : PLEASE_USE_TEXT);
			innerTypeField.setEditable(actual.getInnerType() == null);
		}
	}

	private void updateMemberBox() {
		if (collectionBox.getSelectedIndex() != -1) {
			String typeStr = innerTypeField.getText().trim();
			try {
				MemberInfo member = (MemberInfo) collectionBox.getSelectedItem();
				ArrayList<MemberInfo> membersList = new ArrayList<MemberInfo>();
				innerClass = member.getInnerType();
				if (innerClass == null) {
					//unknown inner type, should read it from the textfield
					try {
					innerClass = forName(typeStr);
					} catch (ClassNotFoundException e) {
						try {
							innerClass = forName(modelClass.getPackageName()+"."+typeStr);
						} catch (ClassNotFoundException e2) {
							innerClass = forName("java.lang."+typeStr);
						}
					}
				}
				innerTypeField.setText(innerClass.getCanonicalName());
				if (!(innerClass.isPrimitive() || Number.class.isAssignableFrom(innerClass) || String.class.equals(innerClass))) {
					Field[] fields = innerClass.getFields();
					Method[] methods = innerClass.getMethods();
					for (int i = 0; i < fields.length; i++) {
						Field field = fields[i];
						if (field.getName().equals("serialVersionUID")) continue;
						MemberInfo mi = new MemberInfo(field.getName(),Utilities.toTypeString1(field.getType()),field.getType());
						NodeType nodeType = calculateNodeType(mi);
						if (!membersList.contains(mi) && (nodeType == NodeType.PRIMITIVE || nodeType == NodeType.OBJECT)) {
							membersList.add(mi);
						}
					}
					for (Method method : methods) {
						if (method.isSynthetic() || bannedMethodNames.contains(method.getName())) continue;
						if (method.getParameterTypes().length == 0 && !method.getReturnType().equals(Void.TYPE)) {
							MemberInfo mi = new MemberInfo(method.getName() + "()",Utilities.toTypeString1(method.getReturnType()),method.getReturnType());
							NodeType nodeType = calculateNodeType(mi);
							if (!membersList.contains(mi) && (nodeType == NodeType.PRIMITIVE || nodeType == NodeType.OBJECT)) {
								membersList.add(mi);
							}
						}
					}
					DefaultComboBoxModel model = new DefaultComboBoxModel(membersList.toArray(new MemberInfo[0]));
					memberBox.setModel(model);
					memberBox.setSelectedIndex(0);
					memberBox.setEnabled(true);
				} else {
					DefaultComboBoxModel model = new DefaultComboBoxModel(new String[] {"Cannot select members of " + typeStr});
					memberBox.setModel(model);
					memberBox.setSelectedIndex(0);
					memberBox.setEnabled(false);
				}
			} catch (ClassNotFoundException e) {
				//MEMEApp.logException("Multiple Column Recordable Operator GUI", e, false);
				memberBox.setSelectedIndex(-1);
				memberBox.setEnabled(false);
			}
		} 
	}
	
	private static NodeType calculateNodeType(MemberInfo info) {
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
	
	private Class<?> forName(String name) throws ClassNotFoundException {
    	if (name.equals("byte")) return Byte.TYPE;
    	if (name.equals("short")) return Short.TYPE;
    	if (name.equals("int")) return Integer.TYPE;
    	if (name.equals("long")) return Long.TYPE;
    	if (name.equals("float")) return Float.TYPE;
    	if (name.equals("double")) return Double.TYPE;
    	if (name.equals("boolean")) return Boolean.TYPE;
    	if (name.equals("char")) return Character.TYPE;
    	Class<?> result = Util.convertToClass(name,wizard.getClassLoader());
    	if (result == null) 
    		throw new ClassNotFoundException(name);
    	return result;
 	}
	
	@Override 
	public JPanel getGUIComponent() { return this; }
	@Override
	public OperatorGUIType getSupportedOperatorType() {	return OperatorGUIType.MULTIPLE_COLUMN; }

	@Override
	public String checkInput() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[] getInput() {
		Object[] ret = new Object[6];
		ret[0] = collectionBox.getSelectedItem();
		ret[1] = innerClass;
		ret[2] = memberBox.getSelectedItem();
		ret[3] = checkLengthField();
		ret[4] = noDataFillerField.getText();
		ret[5] = owner.getRecName()+"Multi()";
		return ret;
	}

	@Override
	public void buildContent(List<? extends Object> buildBlock)
			throws CannotLoadDataSourceForEditingException {
		MemberInfo collectionMemberInfo = (MemberInfo) buildBlock.get(0);
		collectionBox.setSelectedItem(collectionMemberInfo);
		innerClass = (Class<?>) buildBlock.get(1);
		innerTypeField.setText(innerClass.getCanonicalName());
		MemberInfo memberMemberInfo = null;
		if (buildBlock.get(2) instanceof MemberInfo) {
			memberMemberInfo = (MemberInfo) buildBlock.get(2);
		}
		
		String recordinglength = ((String)buildBlock.get(3));
		recordingLengthField.setText(recordinglength);
		noDataFillerField.setText((String) buildBlock.get(4));
	}
	private void layoutGUI() {
		this.setLayout(new BorderLayout());
		JPanel form = FormsUtils.build(
				"p ~ p:g ~ 20dlu", 
				"01_ p|" +
				"234 p|" +
				"56_ p|" +
				"78B p|" +
				"9A_ p", 
				"Collection:", collectionBox,
				"Inner type:", innerTypeField,selectTypeButton,
				"Members:", memberBox,
				"Number of columns:", recordingLengthField,
				"N/A filler:", noDataFillerField,selectSizeButton).getPanel();
		this.add(form,BorderLayout.CENTER);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initialize() {
		this.setBorder(BorderFactory.createTitledBorder("Multiple column recordable"));
		collectionBox.setActionCommand("COLLECTION");
		collectionBox.addActionListener(this);
		innerTypeField.setActionCommand("INNER_TYPE");
		innerTypeField.addActionListener(this);
		innerTypeField.addFocusListener(this);
		setAcceptableLengthList();
		selectTypeButton.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(selectInnerType())updateMemberBox();//innerTypeField.postActionEvent();
			}
		});
		selectSizeButton.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(selectLengthMember())updateMemberBox();//innerTypeField.postActionEvent();
			}
		});
		setPreferredSize(new Dimension(550,200));
		setVisible(true);
		
		setPreferredSize(new Dimension(550,200));
		setVisible(true);
	}
	
	@Override
	public void focusGained(FocusEvent e) {
		innerTypeField.selectAll();
	}
	@Override
	public void focusLost(FocusEvent e) {
		if (e.getComponent().equals(innerTypeField)) {
			updateMemberBox();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if ("INNER_TYPE".equals(command)) {
//			MemberInfoList mil = new MemberInfoList();
			updateMemberBox();
		} else if ("COLLECTION".equals(command)) {
			if (collectionBox.getSelectedIndex() != -1) {
				MemberInfo actual = (MemberInfo) collectionBox.getSelectedItem();
				innerTypeField.setText(actual.getInnerType() !=  null ? actual.getInnerType().getCanonicalName() 
																	  : PLEASE_USE_TEXT);
				innerTypeField.setEditable(actual.getInnerType() == null);
				updateMemberBox();
			}
		}
	}

	@Override
	public void setSelectedCollection(MemberInfo collection) {
		if (collectionBox != null && collection != null) {
			collectionBox.setSelectedItem(collection);
		}
	}
	
	public boolean selectInnerType()
	{
		FilteredListPanel flp = new FilteredListPanel(modelClass, owner.getClassLoader()) {
			@Override
			protected ArrayList<String> formatElements(Object... params) {
				ArrayList<String> elements = new ArrayList<String>();
				elements.addAll(((CtClass)params[0]).getRefClasses());
				Vector<Class> classes = new Vector<Class>();
				
				try 
				{
					ClassLoader cl = (ClassLoader)params[1];
					Field f = ClassLoader.class.getDeclaredField("classes");
					f.setAccessible(true);
					while(cl!=null)
					{
						classes.addAll((Vector<Class>) f.get(cl));
						cl = cl.getParent();
					}
				} catch (NoSuchFieldException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SecurityException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IllegalArgumentException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IllegalAccessException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				for(Class c : classes)if(!elements.contains(c.getName()))elements.add(c.getName());
				return elements;
			}
		};
		
        int iResult = JOptionPane.showConfirmDialog(null, flp, "Type selection", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if(iResult == JOptionPane.OK_OPTION)
    	{
    		innerTypeField.setText((String)flp.getSelectedType());
    		return true;
    	}
        else return false;
	}
	
	public String checkLengthField()
	{
		String length = recordingLengthField.getText();
		if (Pattern.matches("\\d+",length) || acceptableLengthList.contains(length)) return length;
		return null;
	}
	
	public boolean selectLengthMember()
	{
		FilteredListPanel flp = new FilteredListPanel() {
			
			@Override
			protected ArrayList<String> formatElements(Object... params) {
				// TODO Auto-generated method stub
				return acceptableLengthList;
			}
		};
		int iResult = JOptionPane.showConfirmDialog(null, flp, "Length selection", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if(iResult == JOptionPane.OK_OPTION)
    	{
    		recordingLengthField.setText((String)flp.getSelectedType());
    		return true;
    	}
        else return false;		
		
	}
	

	private void setAcceptableLengthList()
	{
		for(CtMethod m : modelClass.getMethods())
		{
			try {
				if(m.getName().startsWith("get")
					&& 
		 					(m.getReturnType().getSimpleName().equals("int") 
		 					||	m.getReturnType().getSimpleName().equals("Integer")
		 					||	m.getReturnType().getSimpleName().equals("Number")
		 					)
		 			&& !Modifier.isPrivate(m.getModifiers())	
				)
				acceptableLengthList.add(m.getName()+"()");
			
			} catch (NotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for(CtField f : modelClass.getDeclaredFields())
		{
			try {
				if(!Modifier.isPrivate(f.getModifiers()) 
					&& 
							(f.getType().getSimpleName().equals("int") 
						|| 	f.getType().getSimpleName().equals("Integer") 
						|| 	f.getType().getSimpleName().equals("Number"))
					)
					acceptableLengthList.add(f.getName());
			} catch (NotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
	}
}

