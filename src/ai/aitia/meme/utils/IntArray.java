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
package ai.aitia.meme.utils;

/** This class is an growable array type with integer elements. */
public class IntArray implements java.util.RandomAccess
{
	/** The values. */
	protected int[] data = null;
	/** Size of the array. */
	protected int size = 0;

	public IntArray()									{}
	public IntArray(int capacity)						{ ensureCapacity(capacity); }
	public IntArray(int[] x)							{ data = x; size = (x == null ? 0 : x.length); }
	public IntArray(java.util.Collection<? extends Number> x)	 { ensureCapacity(x.size());
														  for (Number i : x) add(i.intValue()); }
	/** Appends x to the end of the array. */
	public boolean add(int x)							{ add(size, x); return true; }
	/** Appends the elements of 'other' to the end of 'this'. Returns false if 'other' has no elements. */
	public boolean addAll(IntArray other)				{ return addAll(size, other.data); }
	/** Inserts the elements of 'other' into 'this' at the specified position. Returns false if 'other' has no elements. */
	public boolean addAll(int index, IntArray other)	{ return addAll(index, other.data); }
	/** Appends the elements of 'x' to the end of 'this'. Returns false if 'other' has no elements. */
	public boolean addAll(int[] x)					{ return addAll(size, x); }
	/** Tests if the specified object is a component in this array. */
	public boolean contains(Object x)					{ return indexOf(x) >= 0; }
	/** Tests if this array has no components. */
	public boolean isEmpty()							{ return (size == 0); }
	/** Removes all of the elements from this array. */
	public void    clear()								{ size = 0; }
	/** Returns the element at the specified position in this array. */
	public int     get(int index)						{ /*RangeCheck(index);*/ return data[index]; }
	/** Returns the number of components in this array. */
	public int     size()								{ return size; }
	/** Returns an int array containing all of the elements in this array object in the correct order. */
	public int[]   toArray()							{ trimToSize(); return data; }

	@Override public String toString()					{ trimToSize(); return java.util.Arrays.toString(data); }
	@Override public int hashCode() 					{ trimToSize(); return java.util.Arrays.hashCode(data); }
	@Override public boolean equals(Object obj) {
		if (obj instanceof IntArray) {
			return (obj == this) || java.util.Arrays.equals(toArray(), ((IntArray)obj).toArray());
		}
		if (obj instanceof int[])
			return java.util.Arrays.equals(toArray(), (int[])obj);
		return super.equals(obj);
	}

	/**  Sets the size of this array. */
	public void setSize(int newSize) {
		if (newSize <= 0) clear();
		else {
			ensureCapacity(newSize);
			size = newSize;
		}
	}
	/** Inserts 'x' into 'this' at the specified position.  */
	public void add(int index, int x) {
		if (size >= (data == null ? 0 : data.length)) {
			int growBy = size >> 3;
			growBy = (growBy < 4) ? 4 : (growBy > 1024 ? 1024 : growBy);
			ensureCapacity(size + growBy);
		}
		if (index < size++)
			System.arraycopy(data, index, data, index + 1, size - index - 1);
		//RangeCheck(index);
		data[index] = x;
	}
	/** Inserts the elements of 'x' into 'this' at the specified position. Returns false if 'x' has no elements. */
	public boolean addAll(int index, int[] x) {
		if (x == null || x.length == 0) return false;
		//if (0 > index || index > size) RangeCheck(index);
		ensureCapacity(size + x.length);
		if (index < size)
			System.arraycopy(data, index, data, index + x.length, size - index);
		size += x.length;
		System.arraycopy(x, 0, data, index, x.length);
		return true;
	}
	/** Increases the capacity of this array, if necessary, to ensure that it can hold at
	 *  least the number of components specified by the minimum capacity argument.
	 */ 
	public void ensureCapacity(int minCap) {
		if (minCap > 0) {
			if (data == null)
				data = new int[minCap];
			else if (data.length < minCap) {
				int tmp[] = new int[minCap];
				System.arraycopy(data, 0, tmp, 0, size);
				data = tmp;
			}
		}
	}
	/** Searches for the first occurence of the given argument, testing for equality
	 *  using the == operator.
	 */ 
	public int indexOf(Object x) {
		if (!isEmpty() && (x instanceof Number)) {
			int val = ((Number)x).intValue();
			for (int i = 0; i < size; ++i)
				if (data[i] == val) return i;
		}
		return -1;
	}
	/** Removes and returns the element at the specified position. If necessary, it
	 *  moves the last element to the empty position.<br>
	 *  <b>This method may change the order of its elements.</b>
	 */
	public int fastRemoveAt(int index) {
		//RangeCheck(index);
		int ans = data[index];
		if (index < --size)
			data[index] = data[size];
		return ans;
	}
	/** Removes and returns the element at the specified position in this array, then
	 *  shifts the following elements. This method does not change the order of its
	 *  elements. 
	 */
	public int removeAt(int index) {
		//RangeCheck(index);
		int ans = data[index];
		if (index < --size)
		    System.arraycopy(data, index + 1, data, index, size - index);
		return ans;
	}
	/** Removes and returns the specified element in this array, then
	 *  shifts the following elements. This method does not change the order of its
	 *  elements. 
	 */
	public boolean remove(Object x) {
		int idx = indexOf(x);
		if (idx < 0) return false;
		removeAt(idx); return true;
	}
	/** Replaces the element at the specified position in this array with
	 *  the specified element.
	 */ 
	public int set(int index, int x) {
		//RangeCheck(index);
		int ans = data[index];
		data[index] = x;
		return ans;
	}
	/** Trims the capacity of this array to be the array's current size. */
	public void trimToSize() {
		if (data != null && data.length > size) {
			int tmp[] = new int[size];
			System.arraycopy(data, 0, tmp, 0, size);
			data = tmp;
		}
	}
}
