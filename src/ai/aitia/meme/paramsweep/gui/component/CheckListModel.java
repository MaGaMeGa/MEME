/*******************************************************************************
 * Copyright (C) 2006-2014 AITIA International, Inc.
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
package ai.aitia.meme.paramsweep.gui.component;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.ListModel;

/**
 * This class represents a list model that maintans the extra information about each element whether it is checked or not. 
 * 
 * @author Tamás Máhr
 *
 */
@SuppressWarnings({ "serial", "rawtypes" })
public class CheckListModel extends AbstractListModel implements ListModel {
	
	//====================================================================================================
	// members

	private List<?> elements = new ArrayList<Object>();
	private List<Boolean> checkedState = new ArrayList<Boolean>();
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * Creates a new list model containing the specified elements. The checked state of all elements are initialized to false.
	 * 
	 * @param elements the elements this list model should contain
	 */
	public CheckListModel(final List<?> elements) {
		this.elements = new ArrayList<Object>(elements);

		for (int i = 0; i < elements.size(); i++) {
			checkedState.add(false);
		}
	}

	//----------------------------------------------------------------------------------------------------
	/** {@inheritDoc} 
	 */
	@Override
	public int getSize() {
		return elements.size();
	}

	//----------------------------------------------------------------------------------------------------
	/** {@inheritDoc} 
	 */
	@Override
	public Object getElementAt(int index) {
		return elements.get(index);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setCheckedState(final int index, final boolean state) {
		checkedState.set(index, state);

		fireContentsChanged(this, index, index);
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean getCheckedState(final int index) {
		if (index < 0) return false;
		
		return checkedState.get(index);
	}
}