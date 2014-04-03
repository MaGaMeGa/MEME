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
package ai.aitia.meme.database;

import ai.aitia.meme.MEMEApp;

/**
 * Represents a Model &mdash; (name,version) pair &mdash; in the Results database.
 */
public class Model
{
	public static final long NONEXISTENT_MODELID = Long.MIN_VALUE;

	/** The id of the model used by the database. */
	protected long 	model_id;			//<! The Model_id key used in the database
	/** Model name. */ 
	protected String	name;
	/** Model version. */
	protected String	version;
	/** Model description. */
	protected String	description = null;

	//-------------------------------------------------------------------------
	/** Constructor
	 * @param model_id the id of the model used by the database
	 * @param name model name
	 * @param version model version
	 */
	public Model(long model_id, String name, String version) {
		this.model_id	= model_id;
		this.name		= name;
		this.version	= version;
	}

	//-------------------------------------------------------------------------
	public long	getModel_id()	 		{ return model_id; }
	public String	getName()				{ return name; }
	public String	getVersion()			{ return version; }
	public boolean	isDescriptionChanged()	{ return (description != null); }
	
	//-------------------------------------------------------------------------
	public void	setDescription(String s, boolean writeNow) { 
		description = s;
		if (writeNow)
			MEMEApp.getResultsDb().writeDescription(this);
	} 
	//-------------------------------------------------------------------------
	public String	getDescription()	{
		return isDescriptionChanged() ? description : MEMEApp.getResultsDb().getDescription(model_id); 
	}

	//-------------------------------------------------------------------------
	// Used in: LocalAPI.deleteAll(), ResultsTree.getSelectedModels(), ViewCreation.newResult() etc. 
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Model)) return false;
		Model other = (Model)obj;
		return other.model_id == model_id;
	}

	//-------------------------------------------------------------------------
	@Override
	public int hashCode() {
		return Long.valueOf(model_id).hashCode(); 
	}
}
