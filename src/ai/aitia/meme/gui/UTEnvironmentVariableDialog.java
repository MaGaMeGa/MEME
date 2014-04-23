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

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.skin.ModerateSkin;

import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.usertools.UserToolGroup;
import ai.aitia.meme.usertools.UserToolManager;
import ai.aitia.meme.usertools.UserToolGroup.EnvironmentVariable;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.FormsUtils.Separator;

public class UTEnvironmentVariableDialog extends JDialog implements ActionListener,
 																	ListSelectionListener {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 1L;
	
	private UserToolManager manager = null;
	private int index = -1;
	
	//====================================================================================================
	// GUI members
	
	private JPanel content = null;
	private JPanel globalPanel = null;
	private JList globalList = new JList();
	private JScrollPane globalListScr = new JScrollPane(globalList);
	private JButton gl_NewButton = new JButton("New...");
	private JButton gl_EditButton = new JButton("Edit...");
	private JButton gl_RemoveButton = new JButton("Remove");
	
	private JPanel groupPanel = null;
	private JComboBox groupBox = new JComboBox();
	private JList groupList = new JList();
	private JScrollPane groupListScr = new JScrollPane(groupList);
	private JButton gr_NewButton = new JButton("New...");
	private JButton gr_EditButton = new JButton("Edit...");
	private JButton gr_RemoveButton = new JButton("Remove");
	
	private JPanel buttonPanel = new JPanel();
	private JButton closeButton = new JButton("Close");
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public UTEnvironmentVariableDialog(Frame owner, UserToolManager manager, int index) {
		super(owner,"Environmental Variables",true);
		this.manager = manager;
		this.index = index;
		layoutGUI();
		initialize();
		setLocationRelativeTo(owner);
	}

	//----------------------------------------------------------------------------------------------------
	public UTEnvironmentVariableDialog(Dialog owner, UserToolManager manager, int index) {
		super(owner,"Enviromental Variables",true);
		this.manager = manager;
		this.index = index;
		layoutGUI();
		initialize();
		setLocationRelativeTo(owner);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void showDialog() { setVisible(true); }
	
	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if ("GLOBAL_NEW".equals(command)) {
			newVariable(globalList);
		} else if ("GLOBAL_EDIT".equals(command)) {
			editVariable(globalList);
		} else if ("GLOBAL_REMOVE".equals(command)) {
			removeVariable(globalList);
		} else if ("GROUP_SELECTION".equals(command)) {
			int idx = groupBox.getSelectedIndex();
			groupList.clearSelection();
			initializeGroupList(idx);
			index = idx;
		} else if ("GROUP_NEW".equals(command)) {
			newVariable(groupList);
		} else if ("GROUP_EDIT".equals(command)) {
			editVariable(groupList);
		} else if ("GROUP_REMOVE".equals(command)) {
			removeVariable(groupList);
		} else if ("CLOSE".equals(command)) {
			try {
				manager.save();
			} catch (Exception e1) {
				Logger.logException("Saving user tools",e1,true);
				MEMEApp.userErrors("Warning","Saving environment variables failed.");
			}
			setVisible(false);
		}
	}

	//----------------------------------------------------------------------------------------------------
	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource().equals(globalList)) {
			gl_EditButton.setEnabled(globalList.getSelectedIndex() != -1);
			gl_RemoveButton.setEnabled(globalList.getSelectedIndex() != -1);
		} else if (e.getSource().equals(groupList)) {
			gr_EditButton.setEnabled(groupList.getSelectedIndex() != -1);
			gr_RemoveButton.setEnabled(groupList.getSelectedIndex() != -1);
		}
	}
	
	//====================================================================================================
	// GUI methods
	
	//----------------------------------------------------------------------------------------------------
	private void layoutGUI() {
		globalPanel = FormsUtils.build("p:g ~ p",
									   "01||" +
									   "02||" +
									   "03||" +
									   "0_ f:p:g",
									   globalListScr,gl_NewButton,
									   gl_EditButton,
									   gl_RemoveButton).getPanel();
		
		groupPanel = FormsUtils.build("p ~ p:g ~ p",
									  "01_|||" +
									  "223||" +
									  "224||" +
									  "225||" +
									  "22_ f:p:g",
									  "Group:",groupBox,
									  groupListScr,gr_NewButton,
									  gr_EditButton,
									  gr_RemoveButton).getPanel();
		buttonPanel.add(closeButton);
		
		content = FormsUtils.build("~ p:g ~",
								   "|0||" +
								   "1||" +
								   "2||" +
								   "3|",
								   globalPanel,
								   groupPanel,
								   new Separator(""),
								   buttonPanel).getPanel();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initialize() {
		globalPanel.setBorder(BorderFactory.createTitledBorder("Global variables"));
		groupPanel.setBorder(BorderFactory.createTitledBorder("Group variables"));
		
		initializeGlobalList();
		globalList.addListSelectionListener(this);
		gl_NewButton.setActionCommand("GLOBAL_NEW");
		gl_EditButton.setActionCommand("GLOBAL_EDIT");
		gl_EditButton.setEnabled(false);
		gl_RemoveButton.setActionCommand("GLOBAL_REMOVE");
		gl_RemoveButton.setEnabled(false);
		
		groupBox.setPreferredSize(new Dimension(300,20));
		groupBox.setModel(new DefaultComboBoxModel(manager.getGroups().toArray()));
		groupBox.setActionCommand("GROUP_SELECTION");
		
		groupList.addListSelectionListener(this); 
		gr_NewButton.setActionCommand("GROUP_NEW");
		gr_EditButton.setActionCommand("GROUP_EDIT");
		gr_EditButton.setEnabled(false);
		gr_RemoveButton.setActionCommand("GROUP_REMOVE");
		gr_RemoveButton.setEnabled(false);
		
		closeButton.setActionCommand("CLOSE");
		
		GUIUtils.addActionListener(this,gl_NewButton,gl_EditButton,gl_RemoveButton,groupBox,gr_NewButton,gr_EditButton,gr_RemoveButton,closeButton);
		groupBox.setSelectedIndex(index);
		
		final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(null);
		this.setContentPane(sp);
		this.pack();
		Dimension oldD = this.getPreferredSize();
		this.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
										    oldD.height + sp.getHorizontalScrollBar().getHeight()));
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		oldD = this.getPreferredSize();
		final Dimension newD = GUIUtils.getPreferredSize(this);
		if (!oldD.equals(newD)) 
			this.setPreferredSize(newD);
		this.pack();
	}

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void initializeGlobalList() {
		SortedListModel model = new SortedListModel();
		List<EnvironmentVariable> list = UserToolGroup.getGlobalVariables();
		for (EnvironmentVariable env : list)
			model.addElement(env);
		model.sort();
		globalList.setModel(model);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializeGroupList(int index) {
		SortedListModel  model = new SortedListModel();
		List<EnvironmentVariable> list = manager.getGroup(index).getEnvironmentVariables();
		for (EnvironmentVariable env : list)
			model.addElement(env);
		model.sort();
		groupList.setModel(model);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void removeVariable(JList source) {
		int result = MEMEApp.askUser(false,null,"Are you sure?");
		if (result == 1) {
			EnvironmentVariable var = (EnvironmentVariable) source.getSelectedValue();
			((SortedListModel)source.getModel()).removeElement(var);
			if (source.equals(globalList))
				UserToolGroup.getGlobalVariables().remove(var);
			else
				manager.getGroup(index).getEnvironmentVariables().remove(var);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void newVariable(JList source) {
		UTEditVariableDialog dlg = new UTEditVariableDialog(this,source.equals(globalList) ? null : manager.getGroup(index));
		int result = dlg.showDialog();
		if (result == UTEditVariableDialog.OK_OPTION) {
			EnvironmentVariable variable = dlg.getVariable();
			List<EnvironmentVariable> varList = source.equals(globalList) ? UserToolGroup.getGlobalVariables() 
																		  : manager.getGroup(index).getEnvironmentVariables();
			int idx = varList.indexOf(variable);
			if (idx < 0)
				varList.add(variable);
			else
				varList.set(idx,variable);
		}
		if (source.equals(globalList))
			initializeGlobalList();
		else 
			initializeGroupList(index);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void editVariable(JList source) {
		EnvironmentVariable orig = (EnvironmentVariable) source.getSelectedValue();
		UTEditVariableDialog dlg = new UTEditVariableDialog(this,source.equals(globalList) ? null : manager.getGroup(index));
		int result = dlg.showDialog(orig.getKey(),orig.getValue(),null);
		if (result == UTEditVariableDialog.OK_OPTION) {
			EnvironmentVariable variable = dlg.getVariable();
			List<EnvironmentVariable> varList = source.equals(globalList) ? UserToolGroup.getGlobalVariables() 
																		  : manager.getGroup(index).getEnvironmentVariables();
			int idx = varList.indexOf(variable);
			varList.set(idx,variable);
		}
		if (source.equals(globalList))
			initializeGlobalList();
		else 
			initializeGroupList(index);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void main(String[] args) {
		try {
			System.setProperty("substancelaf.noExtraElements", "");
			UIManager.setLookAndFeel(new SubstanceLookAndFeel());
			SubstanceLookAndFeel.setSkin(new ModerateSkin());
			System.setProperty("sun.awt.noerasebackground","true");
		} catch (UnsupportedLookAndFeelException e1) {}
		
		UserToolManager m = UserToolManager.newInstance();
		UTEnvironmentVariableDialog dlg = new UTEnvironmentVariableDialog((Frame)null,m,0);
		dlg.showDialog();
		System.exit(0);
	}
	
	//====================================================================================================
	// nested classes
	
	//------------------------------------------------------------------------------------
	/** List model that allows to the list to sort its elements easy.
	 *  This is a copy of DefaultListModel with two new sorting methods
	 */
	private static class SortedListModel extends AbstractListModel {

		private static final long serialVersionUID = -1770368970363993910L;
		/** The element storage. */
		protected Vector delegate = new Vector();
	    public int getSize() { return delegate.size(); }
	    public Object getElementAt(int index) {	return delegate.elementAt(index); }
	    /**
	     * Copies the components of this list into the specified array.
	     * The array must be big enough to hold all the objects in this list, 
	     * else an <code>IndexOutOfBoundsException</code> is thrown.
	     *
	     * @param   anArray   the array into which the components get copied
	     * @see Vector#copyInto(Object[])
	     */
	    public void copyInto(Object anArray[]) { delegate.copyInto(anArray); }
	    /**
	     * Trims the capacity of this list to be the list's current size.
	     * 
	     * @see Vector#trimToSize()
	     */
	 	public void trimToSize() { delegate.trimToSize(); }
	    /**
	     * Increases the capacity of this list, if necessary, to ensure 
	     * that it can hold at least the number of components specified by 
	     * the minimum capacity argument. 
	     *
	     * @param   minCapacity   the desired minimum capacity
	     * @see Vector#ensureCapacity(int)
	     */
	 	public void ensureCapacity(int minCapacity) { delegate.ensureCapacity(minCapacity); }
	    /**
	     * Sets the size of this list. 
	     *
	     * @param   newSize   the new size of this list
	     * @see Vector#setSize(int)
	     */
	    public void setSize(int newSize) {
	    	int oldSize = delegate.size();
	    	delegate.setSize(newSize);
	    	if (oldSize > newSize) {
	    		fireIntervalRemoved(this, newSize, oldSize-1);
	    	} else if (oldSize < newSize) {
	    		fireIntervalAdded(this, oldSize, newSize-1);
	    	}
	    }
	    /**
	     * Returns the current capacity of this list.
	     *
	     * @return  the current capacity
	     * @see Vector#capacity()
	     */
	    public int capacity() {	return delegate.capacity(); }
	    /**
	     * Returns the number of components in this list.
	     *
	     * @return  the number of components in this list
	     * @see Vector#size()
	     */
	    public int size() { return delegate.size(); }
	    /**
	     * Tests whether this list has any components.
	     *
	     * @return  <code>true</code> if and only if this list has 
	     *          no components, that is, its size is zero;
	     *          <code>false</code> otherwise
	     * @see Vector#isEmpty()
	     */
	    public boolean isEmpty() { return delegate.isEmpty(); }
	    /**
	     * Returns an enumeration of the components of this list.
	     *
	     * @return  an enumeration of the components of this list
	     * @see Vector#elements()
	     */
	    public Enumeration<?> elements() { return delegate.elements(); }
	    /**
	     * Tests whether the specified object is a component in this list.
	     *
	     * @param   elem   an object
	     * @return  <code>true</code> if the specified object 
	     *          is the same as a component in this list
	     * @see Vector#contains(Object)
	     */
	    public boolean contains(Object elem) { return delegate.contains(elem); }
	    /**
	     * Searches for the first occurrence of <code>elem</code>.
	     *
	     * @param   elem   an object
	     * @return  the index of the first occurrence of the argument in this
	     *          list; returns <code>-1</code> if the object is not found
	     * @see Vector#indexOf(Object)
	     */
	    public int indexOf(Object elem) { return delegate.indexOf(elem); }
	    /**
	     * Searches for the first occurrence of <code>elem</code>, beginning 
	     * the search at <code>index</code>. 
	     *
	     * @param   elem    an desired component
	     * @param   index   the index from which to begin searching
	     * @return  the index where the first occurrence of <code>elem</code>
	     *          is found after <code>index</code>; returns <code>-1</code>
	     *          if the <code>elem</code> is not found in the list
	     * @see Vector#indexOf(Object,int)
	     */
	    public int indexOf(Object elem, int index) { return delegate.indexOf(elem, index); }
	    /**
	     * Returns the index of the last occurrence of <code>elem</code>.
	     *
	     * @param   elem   the desired component
	     * @return  the index of the last occurrence of <code>elem</code>
	     *          in the list; returns <code>-1</code> if the object is not found
	     * @see Vector#lastIndexOf(Object)
	     */
	    public int lastIndexOf(Object elem) { return delegate.lastIndexOf(elem); }
	    /**
	     * Searches backwards for <code>elem</code>, starting from the 
	     * specified index, and returns an index to it. 
	     *
	     * @param  elem    the desired component
	     * @param  index   the index to start searching from
	     * @return the index of the last occurrence of the <code>elem</code> 
	     *          in this list at position less than <code>index</code>;
	     *          returns <code>-1</code> if the object is not found
	     * @see Vector#lastIndexOf(Object,int)
	     */
	    public int lastIndexOf(Object elem, int index) { return delegate.lastIndexOf(elem, index); }
	    /**
	     * Returns the component at the specified index.
	     * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index
	     * is negative or not less than the size of the list.
	     * <blockquote>
	     * <b>Note:</b> Although this method is not deprecated, the preferred
	     *    method to use is <code>get(int)</code>, which implements the 
	     *    <code>List</code> interface defined in the 1.2 Collections framework.
	     * </blockquote>
	     *
	     * @param      index   an index into this list
	     * @return     the component at the specified index
	     * @see #get(int)
	     * @see Vector#elementAt(int)
	     */
	    public Object elementAt(int index) { return delegate.elementAt(index); }
	    /**
	     * Returns the first component of this list.
	     * Throws a <code>NoSuchElementException</code> if this
	     * vector has no components.
	     * @return     the first component of this list
	     * @see Vector#firstElement()
	     */
	    public Object firstElement() { return delegate.firstElement(); }
	    /**
	     * Returns the last component of the list.
	     * Throws a <code>NoSuchElementException</code> if this vector
	     * has no components.
	     *
	     * @return  the last component of the list
	     * @see Vector#lastElement()
	     */
	    public Object lastElement() { return delegate.lastElement(); }
	    /**
	     * Sets the component at the specified <code>index</code> of this 
	     * list to be the specified object. The previous component at that 
	     * position is discarded.
	     * <p>
	     * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index 
	     * is invalid.
	     * <blockquote>
	     * <b>Note:</b> Although this method is not deprecated, the preferred
	     *    method to use is <code>set(int,Object)</code>, which implements the 
	     *    <code>List</code> interface defined in the 1.2 Collections framework.
	     * </blockquote>
	     *
	     * @param      obj     what the component is to be set to
	     * @param      index   the specified index
	     * @see #set(int,Object)
	     * @see Vector#setElementAt(Object,int)
	     */
	    @SuppressWarnings("unchecked")
		public void setElementAt(Object obj, int index) {
	    	delegate.setElementAt(obj, index);
	    	fireContentsChanged(this, index, index);
	    }
	    /**
	     * Deletes the component at the specified index.
	     * <p>
	     * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index 
	     * is invalid.
	     * <blockquote>
	     * <b>Note:</b> Although this method is not deprecated, the preferred
	     *    method to use is <code>remove(int)</code>, which implements the 
	     *    <code>List</code> interface defined in the 1.2 Collections framework.
	     * </blockquote>
	     *
	     * @param      index   the index of the object to remove
	     * @see #remove(int)
	     * @see Vector#removeElementAt(int)
	     */
	    public void removeElementAt(int index) {
	    	delegate.removeElementAt(index);
	    	fireIntervalRemoved(this, index, index);
	    }
	    /**
	     * Inserts the specified object as a component in this list at the 
	     * specified <code>index</code>.
	     * <p>
	     * Throws an <code>ArrayIndexOutOfBoundsException</code> if the index 
	     * is invalid.
	     * <blockquote>
	     * <b>Note:</b> Although this method is not deprecated, the preferred
	     *    method to use is <code>add(int,Object)</code>, which implements the 
	     *    <code>List</code> interface defined in the 1.2 Collections framework.
	     * </blockquote>
	     *
	     * @param      obj     the component to insert
	     * @param      index   where to insert the new component
	     * @exception  ArrayIndexOutOfBoundsException  if the index was invalid
	     * @see #add(int,Object)
	     * @see Vector#insertElementAt(Object,int)
	     */
	    @SuppressWarnings("unchecked")
		public void insertElementAt(Object obj, int index) {
	    	delegate.insertElementAt(obj, index);
	    	fireIntervalAdded(this, index, index);
	    }
	    /**
	     * Adds the specified component to the end of this list. 
	     *
	     * @param   obj   the component to be added
	     * @see Vector#addElement(Object)
	     */
	    @SuppressWarnings("unchecked")
		public void addElement(Object obj) {
	    	int index = delegate.size();
	    	delegate.addElement(obj);
	    	fireIntervalAdded(this, index, index);
	    }
	    /**
	     * Removes the first (lowest-indexed) occurrence of the argument 
	     * from this list.
	     *
	     * @param   obj   the component to be removed
	     * @return  <code>true</code> if the argument was a component of this
	     *          list; <code>false</code> otherwise
	     * @see Vector#removeElement(Object)
	     */
	    public boolean removeElement(Object obj) {
	    	int index = indexOf(obj);
	    	boolean rv = delegate.removeElement(obj);
	    	if (index >= 0) {
	    		fireIntervalRemoved(this, index, index);
	    	}
	    	return rv;
	    }
	    /**
	     * Removes all components from this list and sets its size to zero.
	     * <blockquote>
	     * <b>Note:</b> Although this method is not deprecated, the preferred
	     *    method to use is <code>clear</code>, which implements the 
	     *    <code>List</code> interface defined in the 1.2 Collections framework.
	     * </blockquote>
	     *
	     * @see #clear()
	     * @see Vector#removeAllElements()
	     */
	    public void removeAllElements() {
	    	int index1 = delegate.size()-1;
	    	delegate.removeAllElements();
	    	if (index1 >= 0) {
	    		fireIntervalRemoved(this, 0, index1);
	    	}
	    }
	    @Override public String toString() { return delegate.toString();  }
	    /**
	     * Returns an array containing all of the elements in this list in the
	     * correct order.
	     *
	     * @return an array containing the elements of the list
	     * @see Vector#toArray()
	     */
	    public Object[] toArray() {
	    	Object[] rv = new Object[delegate.size()];
	    	delegate.copyInto(rv);
	    	return rv;
	    }
	    /**
	     * Returns the element at the specified position in this list.
	     * <p>
	     * Throws an <code>ArrayIndexOutOfBoundsException</code>
	     * if the index is out of range
	     * (<code>index &lt; 0 || index &gt;= size()</code>).
	     *
	     * @param index index of element to return
	     */
	    public Object get(int index) { return delegate.elementAt(index); }
	    /**
	     * Replaces the element at the specified position in this list with the
	     * specified element.
	     * <p>
	     * Throws an <code>ArrayIndexOutOfBoundsException</code>
	     * if the index is out of range
	     * (<code>index &lt; 0 || index &gt;= size()</code>).
	     *
	     * @param index index of element to replace
	     * @param element element to be stored at the specified position
	     * @return the element previously at the specified position
	     */
	    @SuppressWarnings("unchecked")
		public Object set(int index, Object element) {
	    	Object rv = delegate.elementAt(index);
	    	delegate.setElementAt(element, index);
	    	fireContentsChanged(this, index, index);
	    	return rv;
	    }
	    /**
	     * Inserts the specified element at the specified position in this list.
	     * <p>
	     * Throws an <code>ArrayIndexOutOfBoundsException</code> if the
	     * index is out of range
	     * (<code>index &lt; 0 || index &gt; size()</code>).
	     *
	     * @param index index at which the specified element is to be inserted
	     * @param element element to be inserted
	     */
	    @SuppressWarnings("unchecked")
		public void add(int index, Object element) {
	    	delegate.insertElementAt(element, index);
	    	fireIntervalAdded(this, index, index);
	    }
	    /**
	     * Removes the element at the specified position in this list.
	     * Returns the element that was removed from the list.
	     * <p>
	     * Throws an <code>ArrayIndexOutOfBoundsException</code>
	     * if the index is out of range
	     * (<code>index &lt; 0 || index &gt;= size()</code>).
	     *
	     * @param index the index of the element to removed
	     */
	    public Object remove(int index) {
	    	Object rv = delegate.elementAt(index);
	    	delegate.removeElementAt(index);
	    	fireIntervalRemoved(this, index, index);
	    	return rv;
	    }
	    /**
	     * Removes all of the elements from this list.  The list will
	     * be empty after this call returns (unless it throws an exception).
	     */
	    public void clear() {
	    	int index1 = delegate.size()-1;
	    	delegate.removeAllElements();
	    	if (index1 >= 0) {
	    		fireIntervalRemoved(this, 0, index1);
	    	}
	    }
	    /**
	     * Deletes the components at the specified range of indexes.
	     * The removal is inclusive, so specifying a range of (1,5)
	     * removes the component at index 1 and the component at index 5,
	     * as well as all components in between.
	     * <p>
	     * Throws an <code>ArrayIndexOutOfBoundsException</code>
	     * if the index was invalid.
	     * Throws an <code>IllegalArgumentException</code> if
	     * <code>fromIndex &gt; toIndex</code>.
	     *
	     * @param      fromIndex the index of the lower end of the range
	     * @param      toIndex   the index of the upper end of the range
	     * @see	   #remove(int)
	     */
	    public void removeRange(int fromIndex, int toIndex) {
	    	if (fromIndex > toIndex) {
	    		throw new IllegalArgumentException("fromIndex must be <= toIndex");
	    	}
	    	for(int i = toIndex; i >= fromIndex; i--) {
	    		delegate.removeElementAt(i);
	    	}
	    	fireIntervalRemoved(this, fromIndex, toIndex);
	    }
	    /** Sorts the elements of the list model into ascending order, according to the
	     * <i>natural ordering</i>.
	     */
	    @SuppressWarnings("unchecked") public void sort() { Collections.sort(delegate); }
	    /** Sorts the elements of the list model according to the order induced by the
	     *  specified comparator.
	     */
	    @SuppressWarnings({ "unchecked", "rawtypes" }) public void sort(Comparator comp) { Collections.sort(delegate,comp); } 
	}
}
