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
package ai.aitia.meme.paramsweep.platform.repast.info;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;

public class ScriptGeneratedRecordableInfo extends GeneratedRecordableInfo {

	//=====================================================================================
	// members
	
	private static final long serialVersionUID = -453765734310269952L;
	
	/** Source code of the script. */
	protected String source = null;
	
	/** The generated members referenced by this script. */
	protected List<GeneratedRecordableInfo> references = null;
	protected final List<UserDefinedVariable> userVariables;
	
	/** Import declarations that belong to this script. */ 
	protected List<String> imports = null;
	
	//=====================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public ScriptGeneratedRecordableInfo(String name, Class type, String accessibleName, String source, List<String> imports) {
		super(name, type, accessibleName);
		this.source = source;
		references = new ArrayList<GeneratedRecordableInfo>();
		userVariables = new ArrayList<UserDefinedVariable>();
		this.imports = imports;
	}

	//----------------------------------------------------------------------------------------------------
	@Override public List<GeneratedRecordableInfo> getReferences() { return references; }
	public List<UserDefinedVariable> getUserVariables() { return Collections.unmodifiableList(userVariables); }
	@Override public String getSource() { return source; }
	public List<String> getImports() { return imports; }
	public void setSource(String source) { this.source = source; }

	//-------------------------------------------------------------------------------------
	/** Adds <code>gmi</code> to the <code>references</code> list. */ 
	public void addReference(GeneratedRecordableInfo gri) {	
		if (!references.contains(gri))
			references.add(gri);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void addUserVariable(final UserDefinedVariable variable) {
		if (!userVariables.contains(variable))
			userVariables.add(variable);
	}
}
