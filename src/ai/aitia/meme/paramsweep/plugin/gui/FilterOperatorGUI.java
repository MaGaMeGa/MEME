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

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import _.agentset;
import _.link;
import _.list;
import _.patch;
import _.turtle;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin.OperatorGUIType;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;
import ai.aitia.meme.paramsweep.utils.MemberInfoList;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;

@SuppressWarnings("serial")
public class FilterOperatorGUI extends JPanel implements IOperatorGUI,
														 ActionListener,
														 FocusListener {
	
	private static final String PLEASE_USE_TEXT = "Please use the fully qualified name.";
	private static final String TOOLTIP = "Please use %value% to reference the current element in the filter condition.";
	
	private String opName = null;
	private ParameterSweepWizard wizard = null;
	
	//====================================================================================================
	// GUI members
	
	private JPanel content = null;
	private JComboBox collectionBox = new JComboBox();
	private JLabel typeLabel = new JLabel("Inner type: ");
	private JTextField innerTypeField = new JTextField();
	private JTextField filterField = new JTextField();
	private JComboBox netLogoTypeBox = new JComboBox();
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public FilterOperatorGUI(final ParameterSweepWizard wizard, final String opName, final MemberInfoList allMembers) {
		super();
		this.wizard = wizard;
		this.opName = opName;
		layoutGUI();
		initialize();
		initializeCollectionBox(allMembers);
		initializeNetLogoTypeBox(allMembers);
	}
	
	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public JPanel getGUIComponent() { return this; }
	public OperatorGUIType getSupportedOperatorType() { return OperatorGUIType.FILTER; }
	
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
		if (filterField.getText().trim().equals(""))
			return "empty filter";
		final String candidate = createTestFilterExpression();
		final String error = PlatformManager.getPlatform(PlatformSettings.getPlatformType()).checkCondition(candidate,wizard);
		if (error != null)
			return error;
		return null;
	}

	//----------------------------------------------------------------------------------------------------
	public Object[] getInput() {
		final List<Object> result = new ArrayList<Object>();
		final MemberInfo selectedItem = (MemberInfo) collectionBox.getSelectedItem();
		if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) {
			final NetLogoType selectedType = (NetLogoType) netLogoTypeBox.getSelectedItem();
			setType(selectedItem,selectedType);
		}
		result.add(selectedItem);
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
		result.add(filterField.getText().trim());
		return result.toArray();
	}

	//----------------------------------------------------------------------------------------------------
	public void buildContent(final List<? extends Object> buildBlock) throws CannotLoadDataSourceForEditingException {
		DefaultComboBoxModel collectionModel = (DefaultComboBoxModel) collectionBox.getModel();
		if (buildBlock.size() < 3)
			throw new IllegalStateException("missing build blocks");
		final MemberInfo memberInfo = (MemberInfo) buildBlock.get(0);
		if (collectionModel.getIndexOf(memberInfo) == -1)
			throw new CannotLoadDataSourceForEditingException(memberInfo.getName() + " is missing");
		collectionBox.setSelectedItem(memberInfo);
		final Class<?> clazz = (Class<?>) buildBlock.get(1);
		if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) 
			selectFromNetLogoTypeBox(memberInfo.getJavaType(),clazz);
		else { 
			innerTypeField.setEditable(true);
			innerTypeField.setText(clazz.getCanonicalName());
		}
		final String filter = (String) buildBlock.get(2);
		filterField.setText(filter);
	}

	//----------------------------------------------------------------------------------------------------
	public void actionPerformed(final ActionEvent e) {
		final String command = e.getActionCommand();
		if ("INNER_TYPE".equals(command)) {
			filterField.grabFocus();
		} else if ("COLLECTION".equals(command)) {
			if (collectionBox.getSelectedIndex() != -1) {
				final MemberInfo actual = (MemberInfo) collectionBox.getSelectedItem();
				innerTypeField.setText(actual.getInnerType() !=  null ? actual.getInnerType().getCanonicalName() : PLEASE_USE_TEXT);
				innerTypeField.setEditable(actual.getInnerType() == null);
				selectFromNetLogoTypeBox(actual.getJavaType(),actual.getInnerType());
			}
		} else if ("NETLOGO_TYPE".equals(command))
			filterField.grabFocus();
	}
	
	//----------------------------------------------------------------------------------------------------
	public void focusGained(final FocusEvent e) { innerTypeField.selectAll(); }
	public void focusLost(final FocusEvent e) {}
	
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
								   typeLabel,netlogo ? netLogoTypeBox : innerTypeField,
								   "Filter: ",filterField).getPanel();
		setLayout(new BorderLayout());
		add(content,BorderLayout.CENTER);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initialize() {
		content.setBorder(BorderFactory.createTitledBorder(opName));
		collectionBox.setActionCommand("COLLECTION");
		collectionBox.addActionListener(this);
		if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) {
			typeLabel.setText("Type: ");
			netLogoTypeBox.setActionCommand("NETLOGO_TYPE");
			netLogoTypeBox.addActionListener(this);
		}
		innerTypeField.setActionCommand("INNER_TYPE");
		innerTypeField.addActionListener(this);
		innerTypeField.addFocusListener(this);
		filterField.setToolTipText(TOOLTIP);
		setPreferredSize(new Dimension(550,200));
		setVisible(true);
	}
	
	//====================================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private void initializeCollectionBox(final MemberInfoList allMembers) {
		final List<MemberInfo> list = allMembers.filterCollection();
		DefaultComboBoxModel model = new DefaultComboBoxModel(list.toArray(new MemberInfo[0]));
		collectionBox.setModel(model);
		if (collectionBox.getSelectedIndex() != -1) {
			final MemberInfo actual = (MemberInfo) collectionBox.getSelectedItem();
			innerTypeField.setText(actual.getInnerType() !=  null ? actual.getInnerType().getName() : PLEASE_USE_TEXT);
			innerTypeField.setEditable(actual.getInnerType() == null);
		}
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
	private Class<?> forName(final String name) throws ClassNotFoundException {
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
    	final Class<?> result = Util.convertToClass(name,wizard.getClassLoader());
    	if (result == null) 
    		throw new ClassNotFoundException(name);
    	return result;
 	}
	
	//----------------------------------------------------------------------------------------------------
	private void setType(final MemberInfo info, final NetLogoType type) {
		info.setType(type.getType().getSimpleName());
		info.setJavaType(type.getType());
		info.setInnerType(type.getInnerType());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void selectFromNetLogoTypeBox(final Class<?> type, final Class<?> innerType) {
		final NetLogoType dummy = new NetLogoType("",type,innerType);
		final DefaultComboBoxModel model = (DefaultComboBoxModel) netLogoTypeBox.getModel();
		int idx = model.getIndexOf(dummy);
		if (idx == -1)
			idx = 0;
		netLogoTypeBox.setSelectedIndex(idx);
	}
	
	//----------------------------------------------------------------------------------------------------
	private String createTestFilterExpression() {
		return (PlatformSettings.getPlatformType() == PlatformType.NETLOGO  || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) ? createNetLogoTestFilterExpression() : createJavaTestFilterExpression();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String createJavaTestFilterExpression() {
		final Object[] input = getInput();
		final MemberInfo info = (MemberInfo) input[0];
		final Class<?> innerType = (Class<?>) input[1];
		final String filter = (String) input[2];
		String replacement = "((" + innerType.getCanonicalName() + ")" + info.getName();
		if (info.getType().endsWith("[]"))
			replacement += "[0])";
		else
			replacement += ".get(0))";
		return filter.replaceAll(Utilities.FILTER_EXP,replacement);
	}
	
	//----------------------------------------------------------------------------------------------------
	private String createNetLogoTestFilterExpression() {
		final Object[] input = getInput();
		final MemberInfo info = (MemberInfo) input[0];
		final String filter = (String) input[2];
		if (agentset.class.equals(info.getJavaType()))
			return filter.replaceAll(Utilities.FILTER_EXP + "\\.","");
		else
			return filter.replaceAll(Utilities.FILTER_EXP,"(one-of " + info.getName() + ")");
	}
}
