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

import ai.aitia.meme.database.Columns;

/** This class represents a combination of the input parameters.
 * @author robin
 *
 */
public class ParameterComb 
{
	/** The row that contains the combinations of the input parameters. */ 
	protected GeneralRow	values;

	//---------------------------------------------------------------------
	public ParameterComb() {
		values= new GeneralRow(new Columns());
	}

	//---------------------------------------------------------------------
	public ParameterComb(GeneralRow values) {
		assert values.isConsistent();		
		this.values= values;
	}

	//---------------------------------------------------------------------
	public int size() {
		assert values.isConsistent();
		return values.size();
	}

	//---------------------------------------------------------------------
	/** Returns the columns information. */
	public Columns	getNames() {
		assert (values.getColumns() != null);
		return values.getColumns();
	}

	//---------------------------------------------------------------------
	/** Returns the values. */
	public GeneralRow getValues() {
		assert values.isConsistent();
		return values;
	}

	//---------------------------------------------------------------------
//	public void assign(ParameterComb other) {
//		names  = other.getNames();
//		values = other.getValues();
//		assert this.isConsistent();
//	}

}

