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
package ai.aitia.meme.paramsweep.utils;

import java.util.ArrayList;
import java.util.Collection;

public class SeparatedList extends ArrayList {

	public static final String SEPARATOR = "|";
	private static final long serialVersionUID = -1779925573204763642L;

	public SeparatedList() {
		super();
	}

	public SeparatedList(Collection c) {
		super(c);
	}

	public SeparatedList(int initialCapacity) {
		super(initialCapacity);
	}

	@Override
	public String toString() {
		java.lang.StringBuilder ret = new java.lang.StringBuilder(size() * 15);
		for (int i = 0; i < size() - 1; i++) {
			ret.append(get(i));
			ret.append(SEPARATOR);
		}
		if (size() > 0){
			ret.append(get(size() - 1));
		}
		return ret.toString();
	}

}
