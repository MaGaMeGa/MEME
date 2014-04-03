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
package ai.aitia.meme.paramsweep.gui.info;

import java.util.List;

/** This class represents the generated methods of the model. The wizard uses this class
 *  only to distinguish generated and original members.
 */
public abstract class GeneratedMemberInfo extends MemberInfo {
	
	//=================================================================================
	// methods
	
	//---------------------------------------------------------------------------------
	/** Constructor. 
	 * @param name the name of the member
	 * @param type the (return) type of the member in string format
	 */
	protected GeneratedMemberInfo(String name, String type, Class<?> javaType) {
		super(name,type,javaType);
	}
	
	//---------------------------------------------------------------------------------
	/** Returns the source code of the generated method. */
	public abstract String getSource();
	/** Tests if the generated method remains valid. 
	 * @param illegalReferences the names of the illegal methods
	 */
	public abstract boolean isValidGeneratedMember(List<String> illegalReferences);
	public abstract List<GeneratedMemberInfo> getReferences();
	public abstract void addReference(GeneratedMemberInfo info);
	public abstract void removeReference(GeneratedMemberInfo info);
	/* null indicates that this object is created before editing functionality
	 * hence it cannot be edited
	 */
	public abstract List<List<? extends Object>> getBuildingBlocks();
	/* null value indicates that this object represents a true script (code).
	 * "unknown" if and only if getBuildingBlocks() returns null 
	 */
	public abstract String getGeneratorName();
	public abstract void setGeneratorName(String generatorName);
}
