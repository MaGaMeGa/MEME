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
package javassist;

import java.util.jar.JarFile;

public class DamnVisibilityUtils {

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public static boolean isDirClassPath(final ClassPath cp) { return (cp instanceof DirClassPath); }
	public static boolean isJarClassPath(final ClassPath cp) { return (cp instanceof JarClassPath); }

	//----------------------------------------------------------------------------------------------------
	public static String getDirFromDirClassPath(final ClassPath cp) {
		if (cp instanceof DirClassPath) {
			final DirClassPath dcp = (DirClassPath) cp;
			return dcp.directory;
		}
		throw new ClassCastException("Parameter is not a DirClassPath object.");
	}
	
	//----------------------------------------------------------------------------------------------------
	public static JarFile getJarFileFromJarClassPath(final ClassPath cp) {
		if (cp instanceof JarClassPath) {
			final JarClassPath jcp = (JarClassPath) cp;
			return jcp.jarfile;
		}
		throw new ClassCastException("Parameter is not a JarClassPath object.");
	}

	//----------------------------------------------------------------------------------------------------
	public static String getJarURLFromJarClassPath(final ClassPath cp) {
		if (cp instanceof JarClassPath) {
			final JarClassPath jcp = (JarClassPath) cp;
			return jcp.jarfileURL;
		}
		throw new ClassCastException("Parameter is not a JarClassPath object.");
	}

	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private DamnVisibilityUtils() {}
}
