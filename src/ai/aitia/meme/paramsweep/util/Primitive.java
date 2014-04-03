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
package ai.aitia.meme.paramsweep.util;


/** A little utility class that knows of the dynamic type of a generic
 *  (of number) and can return the corresponding object wrapper for
 *  a primitive number value. 
 *  
 *  This class is for internal use only. 
 */
public class Primitive<T extends Number> {
	
	//====================================================================================================
	// members
	
	private Class<T> clz;
	
	// TODO: cache cast type and reuse
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public Primitive(T num) {
		clz = (Class<T>) num.getClass();
	}
	
	//----------------------------------------------------------------------------------------------------
	public T getObjectWrapper(long l) {
		try {
			return clz.cast(new Byte("" + l));
		} catch (ClassCastException e) {}
		  catch (NumberFormatException e) {};
		
		try {
			return clz.cast(new Short("" + l));
		} catch (ClassCastException e) {}
		  catch (NumberFormatException e) {};
		  
		try {
			return clz.cast(new Integer("" + l));
		} catch (ClassCastException e) {}
		  catch (NumberFormatException e) {};
		  
		try {
			return clz.cast(new Long("" + l));
		} catch (ClassCastException e) {}
		  catch (NumberFormatException e) {};
		  
		throw new IllegalArgumentException("Conversion of value " + l + " failed to type " + clz);
	}
	
	//----------------------------------------------------------------------------------------------------
	public T getObjectWrapper(double d) {
		try {
			return clz.cast(new Float("" + d));
		} catch (ClassCastException e) {}
		  catch (NumberFormatException e) {};
		  
		try {
			return clz.cast(new Double("" + d));
		} catch (ClassCastException e) {}
		  catch(NumberFormatException e) {};
		  
		throw new IllegalArgumentException("Conversion of value " + d + " failed to type " + clz);
	}
}
