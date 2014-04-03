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
package ai.aitia.meme.paramsweep.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import ai.aitia.meme.paramsweep.utils.Util;

import uchicago.src.sim.parameter.Parameter;
import uchicago.src.sim.parameter.ParameterReader;

/** This class parses an existing parameter file. It uses the Repast services to 
 *  achieve this.
 */
public class RepastJParameterFileParser {
	
	//===============================================================================
	// members
	
	/** This object reads a model's parameters from a parameter file. */
	ParameterReader reader = null;

	//===============================================================================
	// methods
	
	/** Creates a parser for the parameter file <code>file</code>.
	 * @param file the parameter file
	 * @return the parser object
	 * @throws IOException if any problem occurs
	 */
	public static RepastJParameterFileParser createParser(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		
		while ((line = reader.readLine()) != null) {
			if (line.trim().length() != 0)
				break;
		}
		reader.close();
		
		if (line == null)
			throw new IOException("Empty file: " + file.getPath());
		line = line.trim();
		if (line.startsWith("#"))
			throw new IOException("Unsupported file: starts with #");
		
		ParameterReader pReader = new ParameterReader(file.getPath());
		return new RepastJParameterFileParser(pReader);
	}
	
	//-------------------------------------------------------------------------------
	/** Gets the parameter structure read by <code>reader</code>.
	 * @return a vector that contains the top level parameters
	 */ 
	@SuppressWarnings("unchecked")
	public Vector<Parameter> getParametersStructure() {
		return reader.getParameters();
	}
	
	//-------------------------------------------------------------------------------
	/** Returns all parameters read by <code>reader</code>. */
	public Vector<Parameter> getParameters() {
		Vector<Parameter> v = getParametersStructure();
		Vector<Parameter> allParam = new Vector<Parameter>();
		for (Parameter p : v) {
			collect(p,allParam);
		}
		return allParam;
	}
	
	//-------------------------------------------------------------------------------
	/** Returns the names of all parameters read by <code>reader</code> in an
	 *  array. All returning parameter name starts with capital letter.
	 */
	public String[] getCapitalizedParameterNames() {
		Vector<Parameter> v = getParametersStructure();
		Vector<Parameter> allParam = new Vector<Parameter>();
		for (Parameter p : v) {
			collect(p,allParam);
		}
		List<String> result = new ArrayList<String>(allParam.size());
		for (Parameter p : allParam) 
			result.add(Util.capitalize(p.getName()));
		return result.toArray(new String[0]);
	}
	
	//===============================================================================
	// private methods
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param reader object that reads the parameters from a parameter file
	 */
	protected RepastJParameterFileParser(ParameterReader reader) {
		this.reader = reader;
	}
	
	//-------------------------------------------------------------------------------
	/** Puts all embedded parameters of <code>p</code> into the vector <code>v</code>. */
	private void collect(Parameter p, Vector<Parameter> v) {
		v.add(p);
		if (!p.hasChildren()) return;
		for (Object o : p.getChildren()) {
			Parameter pp = (Parameter)o;
			collect(pp,v);
		}
	}
}
