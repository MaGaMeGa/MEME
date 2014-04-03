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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import _.link;
import _.patch;
import _.turtle;
import _.agentset;
import _.list;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.gui.info.ConstantKeyInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin.OperatorGUIType;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;
import ai.aitia.meme.paramsweep.utils.MemberInfoList;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.utils.FormsUtils;

public class ElementSelectionOperatorGUI extends JPanel implements IOperatorGUI,
																   ActionListener,
																   FocusListener {
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	public static enum SelectionType { NORMAL, LIST, MAP, COLLECTION };
	
	private static final long serialVersionUID = 1L;
	
	private static final String PLEASE_USE_TEXT = "Please use the fully qualified name.";
	
	private boolean needIndex = false;
	private SelectionType type;
	private Pattern numberPattern = null;
	private String opName = null;
	private ParameterSweepWizard wizard = null;
	
	private static List<String> bannedMethodNames = new ArrayList<String>(4);
	static {
		bannedMethodNames.add("hashCode");
		bannedMethodNames.add("toString");
		bannedMethodNames.add("clone");
		bannedMethodNames.add("getClass");
	}
	
	//====================================================================================================
	// GUI members
	
	private JPanel content = null;
	private JComboBox collectionBox = new JComboBox();
	private JLabel typeLabel = new JLabel("Inner type: ");
	private JTextField innerTypeField = new JTextField();
	private JLabel indexLabel = new JLabel("Index: "); 
	private JTextField indexField = new JTextField();
	private JComboBox objectBox = new JComboBox();
	private JComboBox netLogoTypeBox = new JComboBox();
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public ElementSelectionOperatorGUI(ParameterSweepWizard wizard, String opName, MemberInfoList allMembers, boolean needIndex, SelectionType type) {
		super();
		this.wizard = wizard;
		this.opName = opName;
		this.needIndex = needIndex;
		this.type = type;
		layoutGUI();
		initialize();
		initializeCollectionBox(allMembers);
		initializeNetLogoTypeBox(allMembers);
		if (type == SelectionType.MAP)
			initializeObjectBox(allMembers);
	}
	
	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public JPanel getGUIComponent() { return this; }

	//----------------------------------------------------------------------------------------------------
	public OperatorGUIType getSupportedOperatorType() {
		if (type == SelectionType.LIST)
			return OperatorGUIType.ONEONE_CONSTRUCT;
		if (type == SelectionType.COLLECTION)
			return OperatorGUIType.LIST_SELECTION;
		if (type == SelectionType.MAP)
			return OperatorGUIType.MAP_SELECTION;
		if (needIndex)
			return OperatorGUIType.ELEMENT_SELECTION;
		return OperatorGUIType.SPEC_ELEMENT_SELECTION;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String checkInput() {
		if (collectionBox.getSelectedIndex() == -1)
			return "no available collection";
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO && PlatformSettings.getPlatformType() != PlatformType.NETLOGO5) {
			if (innerTypeField.getText().trim().equals("") ||
					innerTypeField.getText().trim().equals(PLEASE_USE_TEXT))
				return "empty inner type";
			try {
				forName(innerTypeField.getText().trim());
			} catch (ClassNotFoundException e) {
				return "invalid inner type: " + innerTypeField.getText().trim();
			}
		} 
		switch (type) {
		case NORMAL : {
				if (needIndex && "".equals(indexField.getText().trim()))
					return "empty index field"; 
				if (needIndex && !isValidIndex(indexField.getText())) 
					return "invalid index: " + indexField.getText(); 
			}
			break;
		case COLLECTION : 
		case LIST : 
			// do nothing
			break;
		case MAP : {
				if (objectBox.getSelectedIndex() == -1)
					return "no available key";
			}
		}
		return null;
	}

	//----------------------------------------------------------------------------------------------------
	public Object[] getInput() {
		List<Object> result = new ArrayList<Object>();
		MemberInfo selectedItem = (MemberInfo) collectionBox.getSelectedItem();
		if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) {
			NetLogoType selectedType = (NetLogoType) netLogoTypeBox.getSelectedItem();
			setType(selectedItem,selectedType);
		}
		result.add(selectedItem);
		switch (type) {
		case NORMAL : {
				if (needIndex)
					result.add(new Integer(Integer.parseInt(indexField.getText().trim())));
				try {
					Class clazz = null;
					if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) 
						clazz = selectedItem.getInnerType();
					else
						clazz = forName(innerTypeField.getText().trim());
					result.add(clazz);
				} catch (ClassNotFoundException e) {
					throw new IllegalStateException(e);
				}
			}
			break;
		case COLLECTION : 
		case LIST : {
				try {
					Class clazz = null;
					if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) 
						clazz = selectedItem.getInnerType();
					else
						clazz = forName(innerTypeField.getText().trim());
					result.add(clazz);
				} catch (ClassNotFoundException e) {
					throw new IllegalStateException(e);
				}
			}
			break;
		case MAP : {
				try {
					Class clazz = forName(innerTypeField.getText().trim());
					result.add(clazz);
				} catch (ClassNotFoundException e) {
					throw new IllegalStateException(e);
				}
				result.add(objectBox.getSelectedItem());
			}
		}
		return result.toArray();
	}

	//----------------------------------------------------------------------------------------------------
	public void buildContent(List<? extends Object> buildBlock) throws CannotLoadDataSourceForEditingException {
		DefaultComboBoxModel collectionModel = (DefaultComboBoxModel) collectionBox.getModel();
		DefaultComboBoxModel objectModel = (DefaultComboBoxModel) objectBox.getModel();
		if (buildBlock.size() < 1)
			throw new IllegalStateException("missing build blocks");
		MemberInfo memberInfo = (MemberInfo) buildBlock.get(0);
		if (collectionModel.getIndexOf(memberInfo) == -1)
			throw new CannotLoadDataSourceForEditingException(memberInfo.getName() + " is missing");
		collectionBox.setSelectedItem(memberInfo);
		switch (type) {
		case NORMAL : { 
			if (needIndex) {
				if (buildBlock.size() != 3)
					throw new IllegalStateException("missing Integer object");
				Integer index = (Integer) buildBlock.get(1);
				indexField.setText(String.valueOf(index));
			}
			Class<?> clazz = (Class<?>) buildBlock.get(needIndex ? 2 : 1);
			if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) {
				selectFromNetLogoTypeBox(memberInfo.getJavaType(),clazz);
				return;
			} else if (buildBlock.size() >= 2) { 
				innerTypeField.setEditable(true);
				innerTypeField.setText(clazz.getCanonicalName());
				return;
			}
			throw new IllegalStateException("missing objects");
		}
		case MAP :
			if (buildBlock.size() == 3) {
				innerTypeField.setEditable(true);
				Class<?> clazz = (Class<?>) buildBlock.get(1);
				innerTypeField.setText(clazz.getCanonicalName());
				memberInfo = (MemberInfo) buildBlock.get(2);
				if (memberInfo instanceof ConstantKeyInfo) {
					ConstantKeyInfo ki = (ConstantKeyInfo) memberInfo;
					if (objectModel.getIndexOf(ki) == -1)
						objectModel.addElement(ki);
				}
				if (objectModel.getIndexOf(memberInfo) == -1)
					throw new CannotLoadDataSourceForEditingException(memberInfo.getName() + " is missing");
				objectBox.setSelectedItem(memberInfo);
				return;
			}
			throw new IllegalStateException("too many or missing build blocks");
		case LIST :
		case COLLECTION : {
			Class<?> clazz = (Class<?>) buildBlock.get(1);
			if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO) {
				selectFromNetLogoTypeBox(memberInfo.getJavaType(),clazz);
				return;
			} else if (buildBlock.size() == 2) {
				innerTypeField.setEditable(true);
				innerTypeField.setText(clazz.getCanonicalName());
				return;
			}
			throw new IllegalStateException("too many or missing build blocks");
		}}
	}

	//----------------------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if ("INNER_TYPE".equals(command)) {
			if (type == SelectionType.MAP)
				objectBox.grabFocus();
			else if (type == SelectionType.NORMAL && needIndex)
				indexField.grabFocus();
		} else if ("COLLECTION".equals(command)) {
			if (collectionBox.getSelectedIndex() != -1) {
				MemberInfo actual = (MemberInfo) collectionBox.getSelectedItem();
				innerTypeField.setText(actual.getInnerType() !=  null ? actual.getInnerType().getCanonicalName() 
																	  : PLEASE_USE_TEXT);
				innerTypeField.setEditable(actual.getInnerType() == null);
				selectFromNetLogoTypeBox(actual.getJavaType(),actual.getInnerType());
			}
		} else if ("OBJECT".equals(command)) {
			Object obj = objectBox.getSelectedItem();
			if (obj instanceof String) {
				String text = (String) obj;
				DefaultComboBoxModel model = (DefaultComboBoxModel) objectBox.getModel();
				ConstantKeyInfo info = null;
				try {
					double number = Double.parseDouble(text.trim());
					info = new ConstantKeyInfo(number);
				} catch (NumberFormatException e1) {
					info = new ConstantKeyInfo(text.trim());
				}
				if (model.getIndexOf(info) == -1)
					model.addElement(info);
				objectBox.setSelectedItem(info);
			}
		} else if ("NETLOGO_TYPE".equals(command) && needIndex)
			indexField.grabFocus();
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
		boolean netlogo = PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5;
		content = FormsUtils.build("p ~ p:g",
								   "01||" +
								   "23||" +
								   "45",
								   "Collection: ",collectionBox,
								   typeLabel, netlogo ? netLogoTypeBox : innerTypeField,
								   needIndex ? indexLabel : " ",
								   needIndex ? (type == SelectionType.MAP ? objectBox : indexField) : " ").getPanel();
		setLayout(new BorderLayout());
		add(content,BorderLayout.CENTER);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initialize() {
		content.setBorder(BorderFactory.createTitledBorder(opName));
		collectionBox.setActionCommand("COLLECTION");
		collectionBox.addActionListener(this);
		if (type == SelectionType.MAP) { 
			typeLabel.setText("Value type: ");
			indexLabel.setText("Key: ");
		}
		if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO) {
			typeLabel.setText("Type: ");
			netLogoTypeBox.setActionCommand("NETLOGO_TYPE");
			netLogoTypeBox.addActionListener(this);
		}
		innerTypeField.setActionCommand("INNER_TYPE");
		innerTypeField.addActionListener(this);
		innerTypeField.addFocusListener(this);
		objectBox.setEditable(true);
		objectBox.setActionCommand("OBJECT");
		objectBox.addActionListener(this);
		setPreferredSize(new Dimension(550,200));
		setVisible(true);
	}
	
	//====================================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private void initializeCollectionBox(MemberInfoList allMembers) {
		List<MemberInfo> list = null;
		switch (type) {
		case MAP : 	list = allMembers.filterMap();
				   	break;
		case LIST : list = allMembers.filterOneDimListOnly();
					break;
		default :	list = allMembers.filterCollection();
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
	private void initializeObjectBox(MemberInfoList allMembers) {
		List<MemberInfo> list = allMembers.filterKeyObject();
		List<MemberInfo> list2 = new ArrayList<MemberInfo>();
		for (MemberInfo mi : list) {
			if (!mi.getName().endsWith("()") || !bannedMethodNames.contains(mi.getName().substring(0,mi.getName().length() - 2)))
				list2.add(mi);
		}
		DefaultComboBoxModel model = new DefaultComboBoxModel(list2.toArray(new MemberInfo[0]));
		objectBox.setModel(model);
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
		
		final NetLogoType[] agentsetTypes = new NetLogoType[] {
			new NetLogoType("Agentset of turtles",agentset.class,turtle.class),
			new NetLogoType("Agentset of patches",agentset.class,patch.class),
			new NetLogoType("Agentset of links",agentset.class,link.class)
		};
		
		final List<MemberInfo> breeds = allMembers.filterBreeds();
		
		final Vector<NetLogoType> types = new Vector<NetLogoType>(Arrays.asList(listTypes));
		for (final MemberInfo mi : breeds) 
			types.add(new NetLogoType("List of " + mi.getName(),list.class,mi.getInnerType()));
		types.addAll(Arrays.asList(agentsetTypes));
		for (final MemberInfo mi : breeds) 
			types.add(new NetLogoType("Agentset of " + mi.getName(),agentset.class,mi.getInnerType()));
		
		DefaultComboBoxModel model = new DefaultComboBoxModel(types);
		netLogoTypeBox.setModel(model);
		if (collectionBox.getItemCount() > 0) {
			MemberInfo selected = (MemberInfo) collectionBox.getSelectedItem();
			selectFromNetLogoTypeBox(selected.getJavaType(),selected.getInnerType());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isValidIndex(String text) {
		if (numberPattern == null) 
			numberPattern = Pattern.compile("^[0-9]|[1-9][0-9]+$"); // pattern for non-negative intergers
		Matcher m = numberPattern.matcher(text.trim());
		return m.matches();
	}
	
	//----------------------------------------------------------------------------------------------------
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
