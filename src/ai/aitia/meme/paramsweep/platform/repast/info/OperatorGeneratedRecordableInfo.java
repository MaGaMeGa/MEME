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
import java.util.List;

public class OperatorGeneratedRecordableInfo extends GeneratedRecordableInfo {

	//=====================================================================================
	// members
	
	private static final long serialVersionUID = 847787320600391077L;

	/** Source code of the script. */
	protected String source = null;
	
	/** The generated members referenced by this script. */
	protected List<GeneratedRecordableInfo> references = null;
	
	protected String generatorName = "unknown";
	
	//=====================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public OperatorGeneratedRecordableInfo(String name, Class type, String accessibleName, String source) {
		super(name, type, accessibleName);
		this.source = source;
		references = new ArrayList<GeneratedRecordableInfo>();
	}

	//----------------------------------------------------------------------------------------------------
	@Override public List<GeneratedRecordableInfo> getReferences() { return references; }
	@Override public String getSource() { return source; }
	public String getGeneratorName() { return generatorName; }
	
	//----------------------------------------------------------------------------------------------------
	public void setGeneratorName(String generatorName) { this.generatorName = generatorName; }

	//-------------------------------------------------------------------------------------
	/** Adds <code>gmi</code> to the <code>references</code> list. */ 
	public void addReference(GeneratedRecordableInfo gri) {	
		if (!references.contains(gri))
			references.add(gri);
	}
}
