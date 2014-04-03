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
package ai.aitia.meme.paramsweep.intellisweepPlugin.utils;


/**
 * Interface for a category node of a categorization tree.
 *
 */
public class MethodCategory {
	//=========================================================================
	//members
	protected String name;
	protected String description;
	
	protected MethodCategory parent = null;
	
	//=========================================================================
	//constructors
	public MethodCategory( String name, String desc, MethodCategory parent ){
		this.name = name;
		description = desc;
		this.parent = parent;
	}
	
	//=========================================================================
	//public functions
	/**
	 * Returns the parent category.
	 * 
	 * @return the parent category, or <code>null</code> when this object is 
	 * a top level category.
	 */
	public MethodCategory getParent(){ return parent; }
	
	public void setMethodCategory( MethodCategory parent ){
		this.parent = parent; 
	}
	
	@Override
	public String toString(){
		return name;
	}
	
	public void setName( String name ){ this.name = name; }
	
	/**
	 * Returns the description of the category.
	 * 
	 * @return a description of the category
	 */
	public String getDescription(){ return description;	}
	
	public void setDescription( String desc ){ description = desc; }
}
