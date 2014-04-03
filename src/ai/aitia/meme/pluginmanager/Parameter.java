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
package ai.aitia.meme.pluginmanager;

import ai.aitia.meme.database.ColumnType;

//-----------------------------------------------------------------------------
/** This class represents columns for the plugins. */
public class Parameter implements Comparable<Object>
{
	/** Name of the parameter. */
	protected String		name;
	/** Type of the parameter. */
	protected ColumnType	datatype;

	public 					Parameter()								{ this(null, null); }
	public 					Parameter(String name, ColumnType dt)	{ this.name = name; datatype = dt; }
																	// ^itt nem jo set()-et hivni, ld. pl. 
																	// ParameterSet.ThisViewPar: col==null meg
	public String			getName()								{ return name; }
	public ColumnType		getDatatype()							{ return datatype; }

	public void			setName(String name)					{ this.name = name; }
	public void			setDatatype(ColumnType dt)				{ datatype = dt; }

	public void			set(String name, ColumnType dt)			{ setName(name); setDatatype(dt); }
	
	/**
	 * Modifies type to bs able to represent values of both 'dataType' type 
	 * and 'dt'.    
	 */
	public void extendType(ColumnType dt) {
		ColumnType t = getDatatype();
		setDatatype(t == null ? dt : t.getUnion(dt));
	}

	/**
	 * Sorts parameters primarily by name, but also pays attention to
	 * datatype when 'o' is a Parameter and <code>o.datatype != null</code>.
	 */
	public int compareTo(Object o) {
		int ans = 2;
		if (o == this)
			ans = 0;
		else if (o instanceof Parameter) {
			Parameter other = (Parameter)o;
			ans = getName().compareTo(other.getName());

			// ParameterSet.updateCategory()-ben levo Collection<Parameter>.equals(...) 
			// hivasokban szukseg van a datatype szerinti megkulonboztetesre is.
			// Masutt viszont (pl. ParameterSet.findParam()), az kell hogy datatype-ra
			// ne figyeljunk.
			ColumnType dt;
			if (ans == 0 && (dt = getDatatype()) != null)
				ans = dt.compareTo(other.getDatatype());
		}
		else if (o != null) {
			ans = getName().compareTo(o.toString());
		}
		return ans;
	}
	@Override public boolean equals(Object obj)		{ return compareTo(obj) == 0; }
	@Override public String toString()					{ return getName(); }	// used in ResultsBrowser.refreshInfo()
}
