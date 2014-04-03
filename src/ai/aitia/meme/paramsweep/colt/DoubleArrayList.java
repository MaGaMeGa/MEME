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

import java.util.List;

/** This class is extends the cern.colt.list.DoubleArrayList. It  
 *  allows to add numbers and number collections to the storage and 
 *  performs automatic conversions.
 */
public class DoubleArrayList extends cern.colt.list.DoubleArrayList {

	private static final long serialVersionUID = 6494214534100604015L;

	//---------------------------------------------------------------------------------------
	/** Adds a <code>byte</code> to the list. */
	public void add(byte b) { add((double)b); }
	/** Adds a <code>short</code> to the list. */
	public void add(short s) { add((double)s); }
	/** Adds an <code>int</code> to the list. */
	public void add(int i) { add((double)i); }
	/** Adds a <code>long</code> to the list. */
	public void add(long l) { add((double)l); }
	/** Adds a <code>double</code> to the list. */
	public void add(float f) { add((double)f); }
	/** Adds a <code>Number</code> to the list. */
	public void add(Number n) { add(n.doubleValue()); }
	
	/** Adds the elements of a <code>byte</code> array to the list. */
	public void add(byte[] ba) { for (byte b : ba) add(b); }
	/** Adds the elements of a <code>short</code> array to the list. */
	public void add(short[] sa) { for (short s : sa) add(s); }
	/** Adds the elements of an <code>int</code> array to the list. */
	public void add(int[] ia) { for (int i : ia) add(i); }
	/** Adds the elements of a <code>long</code> array to the list. */
	public void add(long[] la) { for (long l : la) add(l); }
	/** Adds the elements of a <code>float</code> array to the list. */
	public void add(float[] fa) { for (float f : fa) add(f); }
	/** Adds the elements of a <code>double</code> array to the list. */
	public void add(double[] da) { for (double d : da) add(d); }
	/** Adds the elements of a <code>Number</code> array to the list. */
	public void add(Number[] na) { for (Number n : na) add(n); }
	
	/** Adds the elements of an other cern.colt.list.DoubleArrayList to the list. */
	public void add(cern.colt.list.DoubleArrayList dal) { add(dal.elements()); }
	/** Adds the elements of a List (that contains numbers) to the list. */
	@SuppressWarnings("cast")
	public void add(List<? extends Number> nl) {
		for (int i = 0;i < nl.size();++i) {
			if (nl.get(i) instanceof Number) {
				Number n = (Number) nl.get(i);
				add(n);
			}
		}
	}
	
	/** This method does nothing. It is created to ensure that the method call
	 *  add() has never thrown exception.
	 */
	public void add(Object o) {
		System.err.println("Invalid type: " + o.getClass().getName());
	};
	
	//-------------------------------------------------------------------------------------
	/** Returns the elements of the list as a <code>byte</code> array (It performs automatic conversion). */
	public byte[] to_byteArray() {
		byte[] result = new byte[size()];
		for (int i = 0;i < size();++i)
			result[i] = (byte)getQuick(i);
		return result;
	}
	
	//-------------------------------------------------------------------------------------
	/** Returns the elements of the list as a <code>short</code> array (It performs automatic conversion). */
	public short[] to_shortArray() {
		short[] result = new short[size()];
		for (int i = 0;i < size();++i)
			result[i] = (short)getQuick(i);
		return result;
	}
	
	//-------------------------------------------------------------------------------------
	/** Returns the elements of the list as an <code>int</code> array (It performs automatic conversion). */
	public int[] to_intArray() {
		int[] result = new int[size()];
		for (int i = 0;i < size();++i)
			result[i] = (int)getQuick(i);
		return result;
	}
	
	//-------------------------------------------------------------------------------------
	/** Returns the elements of the list as a <code>long</code> array (It performs automatic conversion). */
	public long[] to_longArray() {
		long[] result = new long[size()];
		for (int i = 0;i < size();++i)
			result[i] = (long)getQuick(i);
		return result;
	}
	
	//-------------------------------------------------------------------------------------
	/** Returns the elements of the list as a <code>float</code> array (It performs automatic conversion). */
	public float[] to_floatArray() {
		float[] result = new float[size()];
		for (int i = 0;i < size();++i)
			result[i] = (float)getQuick(i);
		return result;
	}
	
	//-------------------------------------------------------------------------------------
	/** Returns the elements of the list as a <code>double</code> array. */
	public double[] to_doubleArray() {
		double[] result = new double[size()];
		for (int i = 0;i < size();++i)
			result[i] = getQuick(i);
		return result;
	}
	
	//-------------------------------------------------------------------------------------
	/** Returns the elements of the list as a <code>Byte</code> array (It performs automatic conversion). */
	public Byte[] to_ByteArray() {
		Byte[] result = new Byte[size()];
		for (int i = 0;i < size();++i)
			result[i] = (byte)getQuick(i);
		return result;
	}
	
	//-------------------------------------------------------------------------------------
	/** Returns the elements of the list as a <code>Short</code> array (It performs automatic conversion). */
	public Short[] to_ShortArray() {
		Short[] result = new Short[size()];
		for (int i = 0;i < size();++i)
			result[i] = (short)getQuick(i);
		return result;
	}
	
	//-------------------------------------------------------------------------------------
	/** Returns the elements of the list as an <code>Integer</code> array (It performs automatic conversion). */
	public Integer[] to_IntegerArray() {
		Integer[] result = new Integer[size()];
		for (int i = 0;i < size();++i)
			result[i] = (int)getQuick(i);
		return result;
	}
	
	//-------------------------------------------------------------------------------------
	/** Returns the elements of the list as a <code>Long</code> array (It performs automatic conversion). */
	public Long[] to_LongArray() {
		Long[] result = new Long[size()];
		for (int i = 0;i < size();++i)
			result[i] = (long)getQuick(i);
		return result;
	}
	
	//-------------------------------------------------------------------------------------
	/** Returns the elements of the list as a <code>Float</code> array (It performs automatic conversion). */
	public Float[] to_FloatArray() {
		Float[] result = new Float[size()];
		for (int i = 0;i < size();++i)
			result[i] = (float)getQuick(i);
		return result;
	}
	
	//-------------------------------------------------------------------------------------
	/** Returns the elements of the list as a <code>Double</code> array. */
	public Double[] to_DoubleArray() {
		Double[] result = new Double[size()];
		for (int i = 0;i < size();++i)
			result[i] = getQuick(i);
		return result;
	}
}
