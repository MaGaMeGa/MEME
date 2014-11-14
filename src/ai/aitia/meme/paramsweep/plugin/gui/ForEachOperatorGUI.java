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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import _.agentset;
import _.link;
import _.list;
import _.patch;
import _.turtle;
import _.unknown;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.OneArgFunctionMemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin.OperatorGUIType;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;
import ai.aitia.meme.paramsweep.utils.MemberInfoList;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.utils.FormsUtils;

public class ForEachOperatorGUI extends JPanel implements IOperatorGUI,
														  ActionListener,
														  FocusListener {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 1L;
	
	private static final String PLEASE_USE_TEXT = "Please use the fully qualified name.";
	
	private ParameterSweepWizard wizard = null;
	
	private static List<String> bannedMethodNames = new ArrayList<String>(1);
	static {
		bannedMethodNames.add("equals");
	}
	
	//====================================================================================================
	// GUI members
	
	private JPanel content = null;
	private JComboBox collectionBox = new JComboBox();
	private JTextField innerTypeField = new JTextField();
	private JComboBox functionBox = new JComboBox();
	private JComboBox netLogoTypeBox = new JComboBox();
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public ForEachOperatorGUI(ParameterSweepWizard wizard, MemberInfoList allMembers, List<OneArgFunctionMemberInfo> foreachMethods) {
		super();
		this.wizard = wizard;
		layoutGUI();
		initialize();
		initializeCollectionBox(allMembers);
		initializeNetLogoTypeBox(allMembers);
		initializeFunctionBox(foreachMethods);
	}
	
	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public JPanel getGUIComponent() { return this; }
	public OperatorGUIType getSupportedOperatorType() { return OperatorGUIType.FOREACH; }
	
	//----------------------------------------------------------------------------------------------------
	public String checkInput() {
		if (collectionBox.getSelectedIndex() == -1)
			return "no available collection";
		Class<?> clazz = null;
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) {
			if (innerTypeField.getText().trim().equals("") ||
				innerTypeField.getText().trim().equals(PLEASE_USE_TEXT))
				return "empty inner type";
			try {
				clazz = forName(innerTypeField.getText().trim());
			} catch (ClassNotFoundException e) {
				return "invalid inner type: " + innerTypeField.getText().trim();
			}
		} else {
			NetLogoType type = (NetLogoType) netLogoTypeBox.getSelectedItem();
			clazz = type.getInnerType();
		}
		if (functionBox.getSelectedIndex() == -1)
			return "no available function";
		else {
			OneArgFunctionMemberInfo function = (OneArgFunctionMemberInfo) functionBox.getSelectedItem();
			if (!unknown.class.equals(function.getParameterType())) {
				if (!boxingType(function.getParameterType()).isAssignableFrom(boxingType(clazz)))
					return "the component type of the selected collection cannot be cast to the parameter type of the selected function.";
			}
		}
		return null;
	}

	//----------------------------------------------------------------------------------------------------
	public Object[] getInput() {
		List<Object> result = new ArrayList<Object>();
		MemberInfo selectedItem = (MemberInfo) collectionBox.getSelectedItem();
		result.add(selectedItem);
		if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) {
			NetLogoType selectedType = (NetLogoType) netLogoTypeBox.getSelectedItem();
			setType(selectedItem,selectedType);
		}
		Class<?> clazz = null;
		try {
			if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5)
				clazz = selectedItem.getInnerType();
			else 
				clazz = forName(innerTypeField.getText().trim());
			result.add(clazz);
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
		result.add(functionBox.getSelectedItem());
		return result.toArray();
	}

	//----------------------------------------------------------------------------------------------------
	public void buildContent(List<? extends Object> buildBlock) throws CannotLoadDataSourceForEditingException {
		// there is only three objects in the list (otherwise this is an invalid state)
		if (buildBlock.size() == 3) {
			DefaultComboBoxModel collectionModel = (DefaultComboBoxModel) collectionBox.getModel();
			DefaultComboBoxModel functionModel = (DefaultComboBoxModel) functionBox.getModel();
			MemberInfo memberInfo = (MemberInfo) buildBlock.get(0);
			if (collectionModel.getIndexOf(memberInfo) == -1)
				throw new CannotLoadDataSourceForEditingException(memberInfo.getName() + " is missing!");
			collectionBox.setSelectedItem(memberInfo);
			Class<?> clazz = (Class<?>) buildBlock.get(1);
			if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) 
				selectFromNetLogoTypeBox(memberInfo.getJavaType(),clazz);
			else {
				innerTypeField.setText(clazz.getCanonicalName());
				innerTypeField.setEditable(true);
			}
			OneArgFunctionMemberInfo oafmi = (OneArgFunctionMemberInfo) buildBlock.get(2);
			if (functionModel.getIndexOf(oafmi) == -1)
				throw new CannotLoadDataSourceForEditingException(oafmi.getName() + " is missing!");
			functionBox.setSelectedItem(oafmi);
			return;
		}
		throw new IllegalStateException("too many MemberInfo");
	}
	
	//----------------------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if ("INNER_TYPE".equals(command) || "NETLOGO_TYPE".equals(command)) {
			functionBox.grabFocus();
		} else if ("COLLECTION".equals(command)) {
			if (collectionBox.getSelectedIndex() != -1) {
				MemberInfo actual = (MemberInfo) collectionBox.getSelectedItem();
				innerTypeField.setText(actual.getInnerType() !=  null ? actual.getInnerType().getCanonicalName() 
																	  : PLEASE_USE_TEXT);
				innerTypeField.setEditable(actual.getInnerType() == null);
				selectFromNetLogoTypeBox(actual.getJavaType(),actual.getInnerType());
			}
		} 
	}
	
	//----------------------------------------------------------------------------------------------------
	public void focusGained(FocusEvent e) {
		innerTypeField.selectAll();
	}

	//----------------------------------------------------------------------------------------------------
	public void focusLost(FocusEvent e) {}
	
	//====================================================================================================
	// GUI methods
	
	//----------------------------------------------------------------------------------------------------
	private void layoutGUI() {
		boolean nl = PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5;
		content = FormsUtils.build("p ~ p:g",
								   "01||" +
								   "23||" +
								   "45",
								   "Collection: ",collectionBox,
								   nl ? "Type: " : "Inner type: ",
								   nl ? netLogoTypeBox : innerTypeField,
								   "Function: ", functionBox).getPanel();
		setLayout(new BorderLayout());
		add(content,BorderLayout.CENTER);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initialize() {
		collectionBox.setActionCommand("COLLECTION");
		collectionBox.addActionListener(this);
		innerTypeField.setActionCommand("INNER_TYPE");
		innerTypeField.addActionListener(this);
		innerTypeField.addFocusListener(this);
		netLogoTypeBox.setActionCommand("NETLOGO_TYPE");
		netLogoTypeBox.addActionListener(this);
		setPreferredSize(new Dimension(550,200));
		setVisible(true);
	}
	
	//====================================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private void initializeCollectionBox(MemberInfoList allMembers) {
		
		List<MemberInfo> list = new ArrayList<MemberInfo>();
		for (final MemberInfo mi : allMembers.filterOneDimListOnly()) {
			if (!agentset.class.equals(mi.getJavaType()))
				list.add(mi);
		}
		DefaultComboBoxModel model = new DefaultComboBoxModel(list.toArray(new MemberInfo[0]));
		collectionBox.setModel(model);
		if (collectionBox.getSelectedIndex() != -1) {
			MemberInfo actual = (MemberInfo) collectionBox.getSelectedItem();
			innerTypeField.setText(actual.getInnerType() !=  null ? actual.getInnerType().getName() 
																  : PLEASE_USE_TEXT);
			innerTypeField.setEditable(actual.getInnerType() == null);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializeFunctionBox(List<OneArgFunctionMemberInfo> functions) {
		List<OneArgFunctionMemberInfo> list = new ArrayList<OneArgFunctionMemberInfo>();
		for (OneArgFunctionMemberInfo f : functions) {
			if (!bannedMethodNames.contains(f.getName().substring(0,f.getName().length() - 2)))
				list.add(f);
		}
		DefaultComboBoxModel model = new DefaultComboBoxModel(list.toArray(new OneArgFunctionMemberInfo[0]));
		functionBox.setModel(model);
	}
	
	//----------------------------------------------------------------------------------------------------
	private Class<?> forName(String name) throws ClassNotFoundException {
    	if (name.equals("byte")) 
    		return Byte.TYPE;
    	if (name.equals("short"))
    		return Short.TYPE;
    	if (name.equals("int"))
    		return Integer.TYPE;
    	if (name.equals("long"))
    		return Long.TYPE;
    	if (name.equals("float"))
    		return Float.TYPE;
    	if (name.equals("double"))
    		return Double.TYPE;
    	if (name.equals("boolean"))
    		return Boolean.TYPE;
    	if (name.equals("char"))
    		return Character.TYPE;
    	Class<?> result = Util.convertToClass(name,wizard.getClassLoader());
    	if (result == null) 
    		throw new ClassNotFoundException(name);
    	return result;
 	}
	
	//----------------------------------------------------------------------------------------------------
	private Class<?> boxingType(Class<?> memberType) {
		boolean nl = PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5;
		if (Byte.TYPE.equals(memberType))
			return nl ? Double.class : Byte.class;
		if (Short.TYPE.equals(memberType))
			return nl ? Double.class : Short.class;
		if (Integer.TYPE.equals(memberType))
			return nl ? Double.class : Integer.class;
		if (Long.TYPE.equals(memberType))
			return nl ? Double.class : Long.class;
		if (Float.TYPE.equals(memberType))
			return nl ? Double.class : Float.class;
		if (Double.TYPE.equals(memberType))
			return Double.class;
		if (Boolean.TYPE.equals(memberType))
			return Boolean.class;
		if (Character.TYPE.equals(memberType))
			return Character.class;
		return memberType;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializeNetLogoTypeBox(final MemberInfoList allMembers) {
		final NetLogoType[] listTypes = new NetLogoType[] {
			new NetLogoType("List of numbers",list.class,Double.TYPE),
			new NetLogoType("List of booleans",list.class,Boolean.TYPE),
			new NetLogoType("List of strings",list.class,String.class),
			new NetLogoType("List of turtles",list.class,turtle.class),
			new NetLogoType("List of patches",list.class,patch.class),
			new NetLogoType("List of links",list.class,link.class)
		};
		
		final List<MemberInfo> breeds = allMembers.filterBreeds();
		final Vector<NetLogoType> types = new Vector<NetLogoType>(Arrays.asList(listTypes));
		for (final MemberInfo mi : breeds) 
			types.add(new NetLogoType("List of " + mi.getName(),list.class,mi.getInnerType()));

		DefaultComboBoxModel model = new DefaultComboBoxModel(types);
		netLogoTypeBox.setModel(model);
		if (collectionBox.getItemCount() > 0) {
			MemberInfo selected = (MemberInfo) collectionBox.getSelectedItem();
			selectFromNetLogoTypeBox(selected.getJavaType(),selected.getInnerType());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void setType(MemberInfo info, NetLogoType type) {
		info.setType(type.getType().getSimpleName());
		info.setJavaType(type.getType());
		info.setInnerType(type.getInnerType());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void selectFromNetLogoTypeBox(Class<?> type, Class<?> innerType) {
		NetLogoType dummy = new NetLogoType("",type,innerType);
		DefaultComboBoxModel model = (DefaultComboBoxModel) netLogoTypeBox.getModel();
		int idx = model.getIndexOf(dummy);
		if (idx == -1)
			idx = 0;
		netLogoTypeBox.setSelectedIndex(idx);
	}
}
