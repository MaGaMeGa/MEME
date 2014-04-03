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
package ai.aitia.meme.events;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** This global storage class enables to fire a directory changing event with reflection.
 *  All registrated class must have a static setLastDir_(File) method because the 
 *  mechanizm call this method whenever an event occured.
 */ 
public class LastDirectoryChanger {
	
	/** Storage that contains the class object of classes whose wanted to notify
	 *  the directory changing event.
	 */ 
	private static List<Class<?>> candidates = new ArrayList<Class<?>>();
	
	//====================================================================================
	// methods
	
	//------------------------------------------------------------------------------------
	/** Adds a class object to the storage. */
	public static void add(Class<?> candidate) {
		if (!candidates.contains(candidate)) candidates.add(candidate);
	}
	
	//------------------------------------------------------------------------------------
	/** Removes a class object from the storage. */
	public static void remove(Class<?> candidate) { candidates.remove(candidate); }
	
	
	//------------------------------------------------------------------------------------
	/** Notifies all registered class that the directory is changed.
	 * @param dir the new directory (or file)
	 * @param who the class object of the sender class
	 */
	public static void fireDirectoryChanged(File dir, Class<?> who) {
		for (Class<?> clazz : candidates) {
			if (clazz.equals(who)) continue;
			callSetDirectory(clazz,dir);
		}
	}
	
	//------------------------------------------------------------------------------------
	/** Calls the setLastDir_() method of class <code>clazz</code> with parameter
	 *  <code>dir</dir>.
	 */
	private static void callSetDirectory(Class<?> clazz, File dir) {
		try {
			Method m =  clazz.getDeclaredMethod("setLastDir_",File.class);
			m.invoke(null,dir);
		} catch (Exception e) {
			return;
		}
	}
}
