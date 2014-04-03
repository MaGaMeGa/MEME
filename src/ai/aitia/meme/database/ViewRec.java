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

//-----------------------------------------------------------------------------
/** Record class of views. */
public class ViewRec implements Comparable<ViewRec> {
	/** Name of the view. */ 
	protected String	name;
	/** ID of the view. */
	protected long		view_id;
	public						ViewRec(String n, long id)		{ name = n; view_id = id; }
	public long				getViewID()						{ return view_id; }
	public String				getName()						{ return name; }

	public int 				compareTo(ViewRec o)			{ return name.compareTo(o.name); }
	@Override public String		toString()						{ return name; }
	@Override public int		hashCode()						{ return Long.valueOf(view_id).hashCode(); }
	@Override public boolean	equals(Object obj) {
		if (obj == null) return false;
		if (obj instanceof ViewRec) return ((ViewRec)obj).view_id == view_id;
		return toString().equals(obj.toString());
	}
}
