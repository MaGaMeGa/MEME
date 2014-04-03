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
package ai.aitia.meme.paramsweep.batch.output;

/** Class representing non-recordable entities of the model. */
public class NonRecordableInfo extends RecordableInfo {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = -5932678563252254347L;
	
	/** The inner type of the non-recordable entity. <code>Void.TYPE</code>
	 *  means there is no inner type, 'null' means the inner type is
	 *  unknown.<br>
	 *	For example,<br>
	 *  - the inner type of an int[] array is int;
	 *  - the inner type of a List<Integer> list is Integer;
	 *  - the inner type of an int is Void.TYPE;
	 *  - the inner type of a List<?> is 'null'
	 */
	protected Class<?> innerType = Void.TYPE;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public NonRecordableInfo(String name, Class type, String accessibleName) { super(name,type,accessibleName); }
	public NonRecordableInfo(String name, Class type, String description, String accessibleName) { super(name,type,description,accessibleName); }
	
	//----------------------------------------------------------------------------------------------------
	public Class<?> getInnerType() { return innerType; }
	public void setInnerType(Class<?> innerType) { this.innerType = innerType; } 
}
