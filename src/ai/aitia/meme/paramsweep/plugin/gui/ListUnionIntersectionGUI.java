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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import _.agentset;
import _.link;
import _.list;
import _.patch;
import _.turtle;
import _.unknown;
import ai.aitia.meme.paramsweep.classloader.RetryLoader;
import ai.aitia.meme.paramsweep.gui.ScriptCreationDialog;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin.OperatorGUIType;
import ai.aitia.meme.paramsweep.plugin.gui.ListOperatorGUI.CastInputPanel;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;
import ai.aitia.meme.paramsweep.utils.MemberInfoList;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

public class ListUnionIntersectionGUI extends JPanel implements IOperatorGUI,
																ActionListener,
																MouseListener {

	//=====================================================================================
	// members

	private static final long serialVersionUID = 1L;
	
	/** List model of the 'Selected elements'. */
	private _DefaultListModel sModel = null;
	private _DefaultListModel aModel = null;
	
	private MemberInfoList allMembers = null;
	/** The dialog that contains this component. */
	private ScriptCreationDialog parent = null;
	/** Regular expression for double numbers. */

	//=====================================================================================
	// GUI members
	
	private JList availablesList = new JList();
	private JScrollPane availablesListScr = new JScrollPane(availablesList);
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
	public ListUnionIntersectionGUI(ScriptCreationDialog parent, MemberInfoList allMembers) {
		super();
		this.parent = parent;
		this.allMembers = allMembers;
		aModel = new _DefaultListModel();
		sModel = new _DefaultListModel();
		layoutGUI();
		initialize();
		initializeAvailablesList();
	}
	
	//=====================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public JPanel getGUIComponent() { return this; }
	public OperatorGUIType getSupportedOperatorType() { return OperatorGUIType.LIST_UNION_INTERSECTION; }
	
	//-------------------------------------------------------------------------------------
	public String checkInput() {
		if (sModel.isEmpty())
			return "missing selected element(s).";
		if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) {
			MemberInfo first = (MemberInfo) sModel.get(0);
			for (int i = 1;i < sModel.getSize();++i) {
				MemberInfo mi = (MemberInfo) sModel.get(i);
				if (!first.getJavaType().equals(mi.getJavaType()) ||
					!first.getInnerType().equals(mi.getInnerType()))
					return "different types in the selected elements list.";
			}
		}
		return null;
	}

	//-------------------------------------------------------------------------------------
	public Object[] getInput() {
		List<MemberInfo> result = new ArrayList<MemberInfo>(sModel.getSize());
		for (int i = 0;i < sModel.getSize();++i) {
			MemberInfo mi = (MemberInfo) sModel.get(i);
			result.add(mi);
		}
		return result.toArray();
	}
	
	//----------------------------------------------------------------------------------------------------
	public void buildContent(List<? extends Object> buildBlock) throws CannotLoadDataSourceForEditingException {
		if (buildBlock.size() > 0) {
			for (Object o : buildBlock) {
				MemberInfo mi = (MemberInfo) o;
				MemberInfo pair = findPair(mi);
				if (pair == null)
					throw new CannotLoadDataSourceForEditingException(mi.getName() + " is missing");
				pair.setInnerType(mi.getInnerType());
				sModel.addElement(mi);
			}
			return;
		}
		throw new IllegalStateException("missing MemberInfo objects");
	}

	//-------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if ("ADD".equals(command)) {
			boolean firstCondition = true;
			Object[] selected = availablesList.getSelectedValues();
			int notDefined = 0;
			for (Object obj : selected) {
				MemberInfo info = (MemberInfo) obj;
				if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5)
					firstCondition = !info.getInnerType().equals(Void.TYPE) && !info.getInnerType().equals(null);
				if (!firstCondition)
					notDefined++;
				if (firstCondition && !sModel.contains(info))
					sModel.addElement(info);
			}
			parent.warning(notDefined > 0,"Some of the selected items are ignored because of missing type informations",ScriptCreationDialog.WARNING,
						   true);
		} else if ("REMOVE".equals(command)) {
			Object[] selected = selectedList.getSelectedValues();
			for (Object obj : selected)
				sModel.removeElement(obj);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void mousePressed(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
			if (e.getComponent().equals(availablesList)) 
				addButton.doClick();
			else if (e.getComponent().equals(selectedList))
				removeButton.doClick();
		}	
	}
	
	//----------------------------------------------------------------------------------------------------
	public void mouseReleased(MouseEvent e) {
		if (!SwingUtilities.isRightMouseButton(e) || !(e.getComponent().equals(availablesList) || e.getComponent().equals(selectedList)))
			return;
		JList source = (JList) e.getComponent();
		_DefaultListModel model = (_DefaultListModel) source.getModel();
		_DefaultListModel otherModel = source.equals(availablesList) ? sModel : aModel;
		if (e.getComponent().isEnabled()) {
			int idx = source.locationToIndex(e.getPoint());
			if (idx == -1)
				idx = model.getSize() - 1;
			source.setSelectedIndex(idx);
			
			if (idx != -1) {
				MemberInfo info = (MemberInfo) model.get(idx);
				boolean needUpdate = false;
				if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) {
					if (!info.getJavaType().isArray()) {
						JDialog dialog = new JDialog(parent,true);
						CastInputPanel panel = new CastInputPanel(dialog,info.getInnerType() == null ? null : info.getInnerType().getCanonicalName());
						dialog.setUndecorated(true);
						dialog.setContentPane(panel);
						dialog.pack();
						dialog.setLocation(source.getLocationOnScreen().x + e.getX(),source.getLocationOnScreen().y + e.getY()); 
						dialog.setVisible(true);
						String classStr = panel.getInput(); 
						dialog.dispose();
						if (classStr == null)
							return;
						try {
							Class innerClass = forName(classStr);
							info.setInnerType(innerClass);
							needUpdate = true;
						} catch (ClassNotFoundException ee) {
							parent.warning(true,"Unknown class: " + classStr + ". Please use fully qualified names.",ScriptCreationDialog.WARNING,true);
							return;
						}
					}
				} else {
					JDialog dialog = new JDialog(parent,true);
					NetLogoType init = null;
					if (!info.getJavaType().equals(unknown.class) && (!info.getInnerType().equals(null) || !info.getInnerType().equals(Void.TYPE)))
						init = new NetLogoType("",info.getJavaType(),info.getInnerType());
					NetLogoCastPanel panel = new NetLogoCastPanel(dialog,init,allMembers);
					dialog.setUndecorated(true);
					dialog.setContentPane(panel);
					dialog.pack();
					dialog.setLocation(source.getLocationOnScreen().x + e.getX(),source.getLocationOnScreen().y + e.getY()); 
					dialog.setVisible(true);
					NetLogoType selected = panel.getInput(); 
					dialog.dispose();
					if (selected == null)
						return;
					info.setType(selected.getType().getSimpleName());
					info.setJavaType(selected.getType());
					info.setInnerType(selected.getInnerType());
					needUpdate = true;
				}
				if (needUpdate) {
					int otherIdx = -1;
					for (int i = 0; i < otherModel.size();++i) {
						MemberInfo mi = (MemberInfo) otherModel.get(i);
						if (mi.equals(info)) {
							otherIdx = i;
							break;
						}
					}
					model.fireContentsChanged(model,idx,idx);
					if (otherIdx != -1)
						otherModel.fireContentsChanged(otherModel,otherIdx,otherIdx);
				}
			}
		}
	} 
	
	//-------------------------------------------------------------------------------------
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	
	//=====================================================================================
	// GUI methods
	
	//------------------------------------------------------------------------------------
	private void layoutGUI() {
		setLayout(new BorderLayout());
		JPanel tmp = FormsUtils.build("f:p:g(0.6) ~ p ~ f:p:g",
									  "0_1|" +
									  "0_1 f:p:g|" +
									  "021 p||" +
									  "031|" +
									  "0_1 f:p:g||",
									  availablesListScr,selectedListScr,
									  addButton,
									  removeButton).getPanel();
		add(tmp,BorderLayout.CENTER);
	}
	
	//------------------------------------------------------------------------------------
	private void initialize() {
		String title = "Use right click to define the ";
		if (PlatformSettings.getPlatformType() != PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5)
			title += "inner ";
		title += "type of the collections.";
		setBorder(BorderFactory.createTitledBorder(title));

		availablesListScr.setBorder(BorderFactory.createTitledBorder("Available elements"));
		selectedListScr.setBorder(BorderFactory.createTitledBorder("Selected elements"));
		
		availablesList.setModel(aModel);
		availablesList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		selectedList.setModel(sModel);
		selectedList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		availablesList.setCellRenderer(new ListListRenderer());
		selectedList.setCellRenderer(new ListListRenderer());
		
		availablesList.addMouseListener(this);
		selectedList.addMouseListener(this);
		
		addButton.setActionCommand("ADD");
		removeButton.setActionCommand("REMOVE");
		
		GUIUtils.addActionListener(this,addButton,removeButton);
		
		setPreferredSize(new Dimension(550,200));
		availablesListScr.setPreferredSize(new Dimension(240,200));
		selectedListScr.setPreferredSize(new Dimension(240,200));
		setVisible(true);
	}
	 
	//=====================================================================================
	// private methods
	
	//------------------------------------------------------------------------------------
	/** Initializes the 'Available elements' list from <code>availableMembers</code>. */
	private void initializeAvailablesList() {
		aModel.clear();
		if (allMembers != null) {
			List<MemberInfo> availables = allMembers.filterOneDimListOnly();
			for (MemberInfo mi : availables) 
				aModel.addElement(mi);
		}
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
	private MemberInfo findPair(MemberInfo info) {
		for (int i = 0;i < aModel.getSize();++i) {
			MemberInfo candidate = (MemberInfo) aModel.get(i);
			if (info.equals(candidate))
				return candidate;
		}
		return null;
	}
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	public static class ListListRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 1L;

		//----------------------------------------------------------------------------------------------------
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			Component com = super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
			MemberInfo info = (MemberInfo) value;
			String text = value.toString();
			if (!info.getJavaType().isArray()) {
				text = value.toString() + "<";
				text += (info.getInnerType() == null ? "?" : info.getInnerType().getCanonicalName()) + ">";
				if (info.getInnerType() == Void.TYPE)
					text = value.toString();
			}
			((JLabel)com).setText(text);
			((JLabel)com).setToolTipText(text);
			return com;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private static class _DefaultListModel extends DefaultListModel {

		private static final long serialVersionUID = 1L;

		//----------------------------------------------------------------------------------------------------
		@Override
		public void fireContentsChanged(Object source, int index0, int index1) {
			super.fireContentsChanged(source, index0, index1);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private static class NetLogoCastPanel extends JPanel implements ActionListener, KeyListener {

		//====================================================================================================
		// members
		
		private static final long serialVersionUID = 1L;

		private JDialog owner = null;
		private NetLogoType initialValue = null;
		private boolean cancel = true;
		private final MemberInfoList allMembers;
		
		//====================================================================================================
		// GUI members
		
		private JPanel panel = null;
		private JComboBox box = new JComboBox();
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public NetLogoCastPanel(final JDialog owner, final NetLogoType initialValue, final MemberInfoList allMembers) {
			super();
			this.owner = owner;
			this.initialValue = initialValue;
			this.allMembers = allMembers;
			layoutGUI();
		}
		
		//----------------------------------------------------------------------------------------------------
		public NetLogoType getInput() { 
			return cancel ? null : (NetLogoType) box.getSelectedItem();
		}
		
		//====================================================================================================
		// implemented interfaces

		//----------------------------------------------------------------------------------------------------
		public void actionPerformed(ActionEvent e) {
			cancel = false;
			owner.setVisible(false);
		}
		
		//----------------------------------------------------------------------------------------------------
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				if (initialValue != null)
					selectFromNetLogoTypeBox(initialValue);
				cancel = true;
				owner.setVisible(false);
			} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
				cancel = false;
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
									 "Type: ",box).getPanel();
			panel.addKeyListener(this);
			initializeNetLogoTypeBox();
			if (initialValue != null)
				selectFromNetLogoTypeBox(initialValue);
			box.addActionListener(this);
			box.addKeyListener(this);
			this.setLayout(new BorderLayout());
			this.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			this.add(panel,BorderLayout.CENTER);
			this.setPreferredSize(new Dimension(300,50));
			box.grabFocus();
		}
		
		//====================================================================================================
		// assistant methods
		
		//----------------------------------------------------------------------------------------------------
		private void selectFromNetLogoTypeBox(NetLogoType type) {
			DefaultComboBoxModel model = (DefaultComboBoxModel) box.getModel();
			int idx = model.getIndexOf(type);
			if (idx == -1)
				idx = 0;
			box.setSelectedIndex(idx);
		}
		
		//----------------------------------------------------------------------------------------------------
		private void initializeNetLogoTypeBox() {
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
			box.setModel(model);
		}
	}
}
