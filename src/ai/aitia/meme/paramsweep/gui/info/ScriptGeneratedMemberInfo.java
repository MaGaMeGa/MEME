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

import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;

/** This class represents the generated scripts. */
public class ScriptGeneratedMemberInfo extends GeneratedMemberInfo {
	
	//=====================================================================================
	// members
	
	private static final long serialVersionUID = 4593742561959533942L;
	/** Source code of the script. */
	protected String source = null;
	/** The generated members referenced by this script. */
	protected List<GeneratedMemberInfo> references = null;
	/** Import declarations that belong to this script. */ 
	protected List<String> imports = null;
	protected List<UserDefinedVariable> userVariables;
	
	//=====================================================================================
	// methods

	//-------------------------------------------------------------------------------------
	/** Constructor.
	 * @param name name of the script
	 * @param type return type of the script in string format
	 */
	public ScriptGeneratedMemberInfo(String name, String type, Class<?> javaType) {
		super(name,type,javaType);
		references = new ArrayList<GeneratedMemberInfo>();
		imports = new ArrayList<String>();
		userVariables = new ArrayList<UserDefinedVariable>();
	}

	//-------------------------------------------------------------------------------------
	@Override public String getSource() { return source; }
	@Override public List<GeneratedMemberInfo> getReferences() { return Collections.unmodifiableList(references); }
	public List<String> getImports() { return Collections.unmodifiableList(imports); }
	public List<UserDefinedVariable> getUserVariables() { return Collections.unmodifiableList(userVariables); }
	@Override public List<List<? extends Object>> getBuildingBlocks() { return new ArrayList<List<?>>(); } // editable, but don't need any information for that
	@Override public String getGeneratorName() { return null; } // means this is a real script
	@Override public void setGeneratorName(String generatorName) {} // do nothing because there is no generator

	//---------------------------------------------------------------------------------
	public void setSource(String source) { this.source = source; }
	
	//-------------------------------------------------------------------------------------
	@Override
	public boolean isValidGeneratedMember(List<String> illegalReferences) {
		for (String s : illegalReferences) {
			if (source.contains(s)) return false;
		}
		for (GeneratedMemberInfo gmi : references) {
			if (!gmi.isValidGeneratedMember(illegalReferences))
				return false;
		}
		return true;
	}

	//-------------------------------------------------------------------------------------
	/** Adds <code>gmi</code> to the <code>references</code> list. */ 
	@Override
	public void addReference(GeneratedMemberInfo gmi) {	
		if (!references.contains(gmi))
			references.add(gmi);
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public void removeReference(final GeneratedMemberInfo gmi) { references.remove(gmi); }

	//-------------------------------------------------------------------------------------
	/** Adds <code>importDecl</code> to the <code>imports</code> list. */ 
	public void addImport(String importDecl) {
		if (!imports.contains(importDecl))
			imports.add(importDecl);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void addUserVariable(final UserDefinedVariable variable) {
		if (!userVariables.contains(variable))
			userVariables.add(variable);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void removeUserVariable(final UserDefinedVariable variable) { userVariables.remove(variable); }
}
