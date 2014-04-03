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

import java.util.List;

import javax.swing.JPanel;

import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin.OperatorGUIType;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;

public interface IOperatorGUI {
	public JPanel getGUIComponent();
	public OperatorGUIType getSupportedOperatorType(); 
	public String checkInput();
	//Post-condition: the length of the return value == IOperatorPlugin.getNumberOfParameters
	public Object[] getInput(); 
	public void buildContent(List<? extends Object> buildBlock) throws CannotLoadDataSourceForEditingException;
}
