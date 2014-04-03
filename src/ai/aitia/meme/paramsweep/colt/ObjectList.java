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
package ai.aitia.meme.paramsweep.colt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Deprecated
public class ObjectList {

	//====================================================================================================
	// members
	
	protected List<Object> objects = null;
	
	//====================================================================================================
	// metods
	
	//----------------------------------------------------------------------------------------------------
	public ObjectList() { objects = new ArrayList<Object>(); }
	
	private static final long serialVersionUID = 6494214534100604015L;

	//---------------------------------------------------------------------------------------
	public void add(byte[] ba) { for (byte b : ba) objects.add(b); }
	public void add(short[] sa) { for (short s : sa) objects.add(s); }
	public void add(int[] ia) { for (int i : ia) objects.add(i); }
	public void add(long[] la) { for (long l : la) objects.add(l); }
	public void add(float[] fa) { for (float f : fa) objects.add(f); }
	public void add(double[] da) { for (double d : da) objects.add(d); }
	public void add(Object[] oa) { for (Object o : oa) objects.add(o); }
	public void add(cern.colt.list.DoubleArrayList dal) { objects.add(dal.elements()); }
	@SuppressWarnings("unchecked")
	public void add(Object o) {
		if (o instanceof Collection) {
			Collection<? extends Object> coll = (Collection) o;
			for (Object oo : coll) objects.add(oo);
			
			
		} else
			objects.add(o);
	}
	public int size() { return objects.size(); }
}
