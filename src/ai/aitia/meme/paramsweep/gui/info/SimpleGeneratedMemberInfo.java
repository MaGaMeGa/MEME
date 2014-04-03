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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** This class represents the generated statistic instances. */
public class SimpleGeneratedMemberInfo extends GeneratedMemberInfo {
	
	//=================================================================================
	// members
	
	private static final long serialVersionUID = -6808440049142310023L;
	
	/** Source code of the statistic instance. */ 
	protected String source = null;
	/** Displayable name (with actual parameters of the statistic) of the statistic instance. */
	protected String call = null;
	/** The generated members referenced by this statistic instance. */
	protected List<GeneratedMemberInfo> references = null;
	protected String generatorName = "unknown";
	protected List<List<? extends Object>> buildingBlocks = null;
	
	//=================================================================================
	// methods
	
	//---------------------------------------------------------------------------------
	/** Constructor.
	 * @param name name of the statistic instance
	 * @param type return type of the statistic instance in string format
	 */
	public SimpleGeneratedMemberInfo(String name, String type, Class<?> javaType) {
		super(name,type,javaType);
		references = new ArrayList<GeneratedMemberInfo>();
	}
	
	//---------------------------------------------------------------------------------
	@Override public String getSource() { return source; }
	public String getCall() { return call; }
	@Override public List<GeneratedMemberInfo> getReferences() { return Collections.unmodifiableList(references);	}
	@Override public List<List<? extends Object>> getBuildingBlocks() { return buildingBlocks; }
	@Override public String getGeneratorName() { return generatorName; }
	
	//---------------------------------------------------------------------------------
	public void setSource(String source) { this.source = source; }
	public void setCall(String call) { this.call = call; }
	@Override public void setGeneratorName(String generatorName) { this.generatorName = generatorName; }
	
	//---------------------------------------------------------------------------------
	/** Adds <code>gmi</code> to the <code>references</code> list. */ 
	@Override
	public void addReference(GeneratedMemberInfo gmi) {	
		if (!references.contains(gmi))
			references.add(gmi);
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public void removeReference(final GeneratedMemberInfo gmi) { references.remove(gmi); }
	
	//---------------------------------------------------------------------------------
	@Override public String toString() { return call + " : " + type; }

	//---------------------------------------------------------------------------------
	@Override
	public boolean isValidGeneratedMember(List<String> illegalReferences) {
		for (String s : illegalReferences) {
			if (call.contains(s)) return false;
		}
		for (GeneratedMemberInfo gmi : references) {
			if (!gmi.isValidGeneratedMember(illegalReferences))
				return false;
		}
		return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void addBuildingBlock(List<MemberInfo> buildingBlock) {
		if (buildingBlocks == null) 
			buildingBlocks = new ArrayList<List<? extends Object>>();
		buildingBlocks.add(buildingBlock);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void clearBuildingBlocks() {
		if (buildingBlocks == null)
			buildingBlocks = new ArrayList<List<? extends Object>>();
		else
			buildingBlocks.clear();
	}
}
