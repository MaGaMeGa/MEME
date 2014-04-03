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

import javax.swing.filechooser.FileFilter;

/** This file filter accepts all directories and class files. It also
 *  accepts jar files if the approriate flag is true. */ 
public class ModelFileFilter extends FileFilter {
	
	//===============================================================================
	// members
	
	/** Flag that determines whether the filter object accepts jar files too. */
	private boolean jar = false;
	
	//===============================================================================
	// methods
	
	//-------------------------------------------------------------------------------
	/** Constructor. */
	public ModelFileFilter() {}
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param jar if true, the filter accepts jar files too
	 */
	public ModelFileFilter(boolean jar) {
		this.jar = jar;
	}

	//-------------------------------------------------------------------------------
	@Override
	public boolean accept(File f) {
		if (f.isDirectory()) return true;
		if (f.getName().toLowerCase().endsWith(".class")) return true;
		if (jar && f.getName().toLowerCase().endsWith(".jar")) return true;
		return false;
	}

	//-------------------------------------------------------------------------------
	@Override
	public String getDescription() {
		return jar ? "Java class files and JARs (*.class,*.jar)" : "Repast model files (*.class)";
	}
}
