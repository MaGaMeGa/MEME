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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * A generic Vector combined with a list of checkboxes in a scrollpane.
 */
public class GenericCheckBoxListGui<E> implements ActionListener {

	private static final String SELECT_NONE_BUTTON = "select none button";
	private static final String SELECT_ALL_BUTTON = "select all button";
	protected JPanel cbPanel = null;
	protected JScrollPane jListScr = null;
	protected Vector<E> list = null;
	protected Vector<JCheckBox> cbList = null;
	protected JPanel guiPanel = null;
	protected JButton allButton = null;
	protected JButton noneButton = null;

	public GenericCheckBoxListGui() {
		cbPanel = new JPanel(new GridLayout(0, 1));
		jListScr = new JScrollPane(cbPanel);
		list = new Vector<E>();
		cbList = new Vector<JCheckBox>();
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
	}

	public GenericCheckBoxListGui(int width, int height) {
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
		int ret = -1;
		int i = 0;
		while (i < cbList.size() && ret == -1) {
			if (cbList.get(i).isSelected())
				ret = i;
			i++;
		}
		return ret;
	}

	public int[] getSelectedIndices() {
		int size = 0;
		for (int i = 0; i < cbList.size(); i++) {
			size += cbList.get(i).isSelected() ? 1 : 0;
		}
		int[] ret = new int[size];
		if (size > 0) {
			int idx = 0;
			for (int i = 0; i < cbList.size(); i++) {
				if (cbList.get(i).isSelected()) {
					ret[idx] = i;
					idx++;
				}
			}
		}
		return ret;
	}

	public E getSelectedValue() {
		return list.get(getSelectedIndex());
	}

	public List<E> getSelectedValues() {
		Vector<E> ret = new Vector<E>();
		int[] idx = getSelectedIndices();
		for (int i = 0; i < idx.length; i++) {
			ret.add(get(idx[i]));
		}
		return ret;
	}

	public void setSelectedIndex(int index) {
		cbList.get(index).setSelected(true);
	}

	public void setSelectedIndices(int[] indices) {
		for (int i = 0; i < indices.length; i++) {
			setSelectedIndex(indices[i]);
		}
	}

	public void setAllNoneButtonsVisible(boolean flag) {
		allButton.setVisible(flag);
		noneButton.setVisible(flag);
	}

	public boolean add(E e) {
		boolean ret = list.add(e);
		cbList.add(new JCheckBox(e.toString()));
		refreshDisplayKeepSelection();
		return ret;
	}

	public boolean addAll(Collection<? extends E> c) {
		boolean ret = list.addAll(c);
		for (E element : c) {
			cbList.add(new JCheckBox(element.toString()));
		}
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
		cbList.remove(index);
		refreshDisplayKeepSelection();
		return ret;
	}

	public void removeIndices(int[] indices) {
		Arrays.sort(indices);
		for (int i = indices.length - 1; i >= 0; i--) {
			list.remove(indices[i]);
			cbList.remove(indices[i]);
		}
		refreshDisplayKeepSelection();
	}

	public void refreshDisplay() {
		cbPanel = new JPanel(new GridLayout(0, 1));
		for (int i = 0; i < cbList.size(); i++) {
			cbPanel.add(cbList.get(i));
		}
		jListScr.setViewportView(cbPanel);
	}

	public void refreshDisplayKeepSelection() {
		refreshDisplay();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().equals(SELECT_ALL_BUTTON)) {
			for (int i = 0; i < cbList.size(); i++) {
				cbList.get(i).setSelected(true);
			}
		} else if (e.getActionCommand().equals(SELECT_NONE_BUTTON)) {
			for (int i = 0; i < cbList.size(); i++) {
				cbList.get(i).setSelected(false);
			}
		}
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
