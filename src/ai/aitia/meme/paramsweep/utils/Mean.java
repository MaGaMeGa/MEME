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
import java.util.List;

public class Mean<T extends Number> {
	private int n;
	private List<T> data;
	public Mean(int n) {
		if (n <= 0)
			throw new IllegalArgumentException("'n' must be positive.");
		this.n = n;
		this.data = new ArrayList<T>(this.n);
	}
	
	public void add(T number) {
		if (data.size() == n)
			data.remove(0);
		data.add(number);
	}
	
	public double mean() {
		double sum = 0;
		for (Number n : data) sum += n.doubleValue();
		return sum / Math.min(n,data.size());
	}
	
	public int size() { return data.size(); }
	public boolean empty() { return data.isEmpty(); }
	public void clear() { data.clear(); }
}
