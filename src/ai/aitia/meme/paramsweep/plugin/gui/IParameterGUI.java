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

import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;

/** This interface is representing graphical components for formal parameters of statistics.
 *  There are many categories of formal parameters and each category needs different 
 *  graphical components. This interface defines the common services of these graphical
 *  components. 
 */
public interface IParameterGUI {
	/** Returns the graphical component that belongs to a formal parameter.
	 * @param name the name of the parameter
	 * @param tooltip the short description of the parameter
	 * @param availables the list of information objects of appropriate members of the
	 * 					 model that can be used in the statistic instance. 
	 * @return the graphical component
	 */
	public JPanel getGUIComponent(String name, String tooltip, List<MemberInfo> availables);
	/** Checks the inputs of the graphical component.
	 * @return the error message (null if there is no error)
	 */
	public String checkInput();
	public void buildContent(List<MemberInfo> buildBlock) throws CannotLoadDataSourceForEditingException;
	
	/** Returns the name of the parameter. <br>
	 *  Pre-condition: calls only after {@link #getGUIComponent(String, String, List)}.
	 */
	public String getParameterName();
	
	// pre-condition : checkInput() == null
	// return type : MemberInfo or List<MemberInfo>
	/** Returns the input of the graphical component.<br>
	 *  Pre-condition: {@link #checkInput()} is null.
	 * @return a MemberInfo or List&lt;MemberInfo&gt; object
	 */
	public Object getInput();
	public void postCreation();
}
