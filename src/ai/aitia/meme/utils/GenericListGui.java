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
package ai.aitia.meme.utils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class GenericListGui<E> implements ActionListener, ListSelectionListener{

	private static final String SELECT_NONE_BUTTON = "select none button";
	private static final String SELECT_ALL_BUTTON = "select all button";
	protected JList jList = null;
	protected JScrollPane jListScr = null;
	protected Vector<E> list = null;
	protected JPanel guiPanel = null;
	protected JButton allButton = null;
	protected JButton noneButton = null;
	
	public GenericListGui() {
		jList = new JList();
		jListScr = new JScrollPane(jList);
		list = new Vector<E>();
		guiPanel = new JPanel(new BorderLayout());
		JPanel buttonsPanel = new JPanel(new FlowLayout());
		allButton = new JButton("All");
		noneButton = new JButton("None");
		buttonsPanel.add(allButton);
		buttonsPanel.add(noneButton);
		allButton.setActionCommand(SELECT_ALL_BUTTON);
		noneButton.setActionCommand(SELECT_NONE_BUTTON);
		GUIUtils.addActionListener(this, allButton, noneButton);
		guiPanel.add(jListScr, BorderLayout.CENTER);
		guiPanel.add(buttonsPanel, BorderLayout.SOUTH);
		jList.addListSelectionListener(this);
	}

	public GenericListGui(int width, int height) {
		this();
		this.jListScr.setPreferredSize(new Dimension(width, height));
	}
	
	public JPanel getPanel() {
		return guiPanel;
	}
	
	public void setVisible(boolean flag) {
		guiPanel.setVisible(flag);
	}
	public int getSelectedIndex() {
		return jList.getSelectedIndex();
	}
	public int[] getSelectedIndices() {
		return jList.getSelectedIndices();
	}
	public E getSelectedValue() {
		return list.get(jList.getSelectedIndex());
	}
	public List<E> getSelectedValues() {
		Vector<E> ret = new Vector<E>();
		int[] idx = getSelectedIndices();
		for (int i = 0; i < idx.length; i++) {
			ret.add(get(idx[i]));
		}
		return ret;
	}
	public int getSelectionMode() {
		return jList.getSelectionMode();
	}
	public void setSelectedIndex(int index) {
		jList.setSelectedIndex(index);
	}
	public void setSelectedIndices(int[] indices) {
		jList.setSelectedIndices(indices);
	}
	public void setSelectionMode(int listSelectionMode) {
		jList.setSelectionMode(listSelectionMode);
		boolean allNoneVisible = !(listSelectionMode == ListSelectionModel.SINGLE_SELECTION);
		setAllNoneButtonsVisible(allNoneVisible);
	}
	public void setAllNoneButtonsVisible(boolean flag){
		allButton.setVisible(flag);
		noneButton.setVisible(flag);
	}
	public boolean add(E e) {
		boolean ret = list.add(e);
		refreshDisplayKeepSelection();
		return ret;
	}
	public boolean addAll(Collection<? extends E> c) {
		boolean ret = list.addAll(c);
		if (ret) {
			refreshDisplayKeepSelection();
		}
		return ret;
	}
	public E get(int index) {
		return list.get(index);
	}
	public E remove(int index) {
		E ret = list.remove(index);
		refreshDisplayKeepSelection();
		return ret;
	}
	public void removeIndices(int[] indices) {
		Arrays.sort(indices);
		for (int i = indices.length - 1; i >= 0; i--) {
			list.remove(indices[i]);
		}
		refreshDisplayKeepSelection();
	}
	
	public void refreshDisplay() {
		jList = new JList(list);
		jList.addListSelectionListener(this);
		jListScr.setViewportView(jList);
//		jListScr.setVisible(false);
//		jListScr.setVisible(true);
	}

	public void refreshDisplayKeepSelection() {
		refreshDisplay();
//		if(jList.getSelectedIndex() > -1){
//			Object[] selectedObjects = jList.getSelectedValues();
//			refreshDisplay();
//			for (int i = 0; i < selectedObjects.length; i++) {
//				int idx = list.indexOf(selectedObjects[i]);
//				if (idx > -1) {
//					jList.getSelectionModel().addSelectionInterval(idx, idx);
//				}
//			}
//		} else refreshDisplay();
	}
	
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(SELECT_ALL_BUTTON)) {
			jList.getSelectionModel().addSelectionInterval(0, jList.getModel().getSize() - 1);
		} else if (e.getActionCommand().equals(SELECT_NONE_BUTTON)) {
			jList.getSelectionModel().clearSelection();
		}
	}

	public void valueChanged(ListSelectionEvent e) {
		listSelectionChanged(e);
	}

	public void listSelectionChanged(ListSelectionEvent e) {
	}
	
	public List<E> getList() {
		return list;
	}

	public void clear() {
		list.clear();
		refreshDisplay();
	}

	public int size() {
		return list.size();
	}
	
}
