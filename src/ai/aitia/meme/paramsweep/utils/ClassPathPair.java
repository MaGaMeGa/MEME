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

import java.io.File;
import java.io.IOException;

import javassist.ClassPath;

/** This class encapsualtes a ClassPath object (used by the Javassist) with its
 *  string representation of the classpath element.
 */
public class ClassPathPair {
	
	//===============================================================================
	// members

	/** The classpath element in canonical path string format */
	private String name;
	/** The classpath element in Javassist representation. */
	private ClassPath path;
	
	//===============================================================================
	// methods
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param name the classpath element in string format
	 * @param path the classpath element in Javassist representation
	 */
	public ClassPathPair(String name, ClassPath path) {
		try {
			this.name = new File(name).getCanonicalPath();
		}
		catch(IOException e) {
			this.name = name; 	// what was here originally
		}
		this.path = path;
	}
	
	//-------------------------------------------------------------------------------
	public ClassPath getClassPath() { return path; }
	public void setClassPath(ClassPath path) { this.path = path; }
	@Override public String toString() { return name; }
	
	//-------------------------------------------------------------------------------
	/**
	 * Two classpath elements are equal if their File objects are equal. This
	 * eliminates ambiguity in path separator, upper case issues.
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof ClassPathPair) {
			ClassPathPair that = (ClassPathPair)other;
			//return that.name.equals(this.name);
			return new File(name).equals(new File(that.name)); 
		}
		return false;
	}

}
