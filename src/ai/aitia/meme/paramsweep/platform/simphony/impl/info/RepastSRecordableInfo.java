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
package ai.aitia.meme.paramsweep.platform.simphony.impl.info;

import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.platform.simphony.impl.info.AggrType;

public class RepastSRecordableInfo extends RecordableInfo {
	
	//====================================================================================================
	// members

	private static final long serialVersionUID = -5398918072362142949L;

	protected String agentClass;
	protected String accessMethod;
	protected String fieldName = null;
	protected AggrType aggrType = AggrType.NONE;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public RepastSRecordableInfo(String name, Class<?> type, String accessibleName, String agentClass, String accessMethod) {
		super(name,type,accessibleName);
		this.agentClass = agentClass;
		this.accessMethod = accessMethod;
		if (accessMethod.contains("."))
			this.accessMethod = this.accessMethod.substring(this.accessMethod.lastIndexOf('.') + 1);
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getAccessMethod() { return accessMethod; }
	public String getAgentClass() {	return agentClass; }
	public AggrType getAggrType() { return aggrType; }
	public void setAggrType(AggrType aggrType) { this.aggrType = aggrType; }
	public boolean isSimple() { return aggrType == AggrType.NONE; }
	public String getFieldName() { return fieldName; }
	public void setFieldName(String fieldName) { this.fieldName = fieldName; }

	//----------------------------------------------------------------------------------------------------
	public String getAgentPackage() {
		int idx = agentClass.lastIndexOf('.');
		return agentClass.substring(0,idx);
	}

	//----------------------------------------------------------------------------------------------------
	public static AggrType getAggregateType(String typeString)  {
		if (typeString == null) return null;
		
		if (typeString.equals("NONE"))
			return AggrType.NONE;
		else if (typeString.equals("VARIANCE"))
			return AggrType.VARIANCE;
		else if (typeString.equals("STANDARD_DEVIATION"))
			return AggrType.STANDARD_DEVIATION;
		else if (typeString.equals("MAX"))
			return AggrType.MAX;
		else if (typeString.equals("SKEWNESS"))
			return AggrType.SKEWNESS;
		else if (typeString.equals("MEAN"))
			return AggrType.MEAN;
		else if (typeString.equals("KURTOSIS"))
			return AggrType.KURTOSIS;
		else if (typeString.equals("SUMSQ"))
			return AggrType.SUMSQ;
		else if (typeString.equals("MIN"))
			return AggrType.MIN;
		else if (typeString.equals("COUNT"))
			return AggrType.COUNT;
		else if (typeString.equals("SUM"))
			return AggrType.SUM;
		else if (typeString.equals("GEOMETRIC_MEAN"))
			return AggrType.GEOMETRIC_MEAN;
		
		return null;
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
