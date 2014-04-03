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

import ai.aitia.meme.paramsweep.platform.simphony.impl.info.AggrType;

public class RepastSRecordableElement extends RecordableElement {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -1808914335086071624L;
	
	public static final String AGGREGATE_TYPE = "aggregate_type";
	public static final String AGENT_CLASS = "agent_class";
	public static final String ACCESS_METHOD = "access_method";

	protected AggrType aggrType = AggrType.NONE;
	
	//====================================================================================================
	// members
	
	//----------------------------------------------------------------------------------------------------
	public RepastSRecordableElement(RepastSMemberInfo info) {
		super(info);
	}
	
	//----------------------------------------------------------------------------------------------------
	public RepastSRecordableElement(RepastSMemberInfo info, AggrType aggrType) {
		super(info);
		this.aggrType = aggrType;
	}
	
	//----------------------------------------------------------------------------------------------------
	public RepastSRecordableElement(RepastSMemberInfo info, String alias, AggrType aggrType) {
		super(info);
		this.aggrType = aggrType;
		setAlias(alias);
	}
	
	//----------------------------------------------------------------------------------------------------
	public AggrType getAggrType() { return aggrType; }
	public void setAggrType(AggrType aggrType) { this.aggrType = aggrType; }
	public boolean isSimple() { return aggrType == AggrType.NONE; }
	@Override public RepastSMemberInfo getInfo() { return (RepastSMemberInfo) info; }

	//----------------------------------------------------------------------------------------------------
	@Override
	public String toString() {
		if (aggrType == AggrType.NONE)
			return super.toString();
		else
			return super.toString() + "(Aggregator: " + toString(aggrType) + ")"; 
	}
	
	//----------------------------------------------------------------------------------------------------
	public static String toString(AggrType type) {
		switch (type) {
		case VARIANCE			: return "Variance";
		case STANDARD_DEVIATION : return "Standard deviation";
		case MAX				: return "Maximum";
		case SKEWNESS			: return "Skewness";
		case MEAN				: return "Mean";
		case KURTOSIS			: return "Kurtosis";
		case SUMSQ				: return "Sumsq";
		case MIN				: return "Minimum";
		case COUNT				: return "Count";
		case SUM				: return "Sum";
		case GEOMETRIC_MEAN		: return "Geometric mean";
		default					: return "Simple";
		}
	}
}
