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

public class RepastSMemberInfo extends MemberInfo {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 8606506668169851991L;
	
	protected String agentClass;
	protected String accessMethod;
	protected String fieldName = null;
	
	//====================================================================================================
	// method
	
	//----------------------------------------------------------------------------------------------------
	public RepastSMemberInfo(String name, String type, Class<?> javaType, String agentClass, String accessMethod) {
		super(name,type,javaType);
		this.agentClass = agentClass;
		this.accessMethod = accessMethod;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getAccessMethod() { return accessMethod; }
	public String getAgentClass() {	return agentClass; }
	public String getFieldName() { return fieldName; }
	
	//----------------------------------------------------------------------------------------------------
	public void setFieldName(String fieldName) { this.fieldName = fieldName; } 

	//----------------------------------------------------------------------------------------------------
	public String getAgentPackage() {
		int idx = agentClass.lastIndexOf('.');
		return agentClass.substring(0,idx);
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getAgentClassSimpleName() {
		int idx = agentClass.lastIndexOf('.');
		return agentClass.substring(idx + 1);
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		String result = getAgentClassSimpleName();
		if (name != null)
			return result + "." + name + " : " + type;
		else
			return result + "." + accessMethod + " : " + type;
	}
}
