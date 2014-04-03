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
package ai.aitia.meme.paramsweep.platform.repast.impl;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

/** This class checks the syntax of a logical expression built from the members of a
 *  model. It is uses Javassist to perform the checking operation.
 */
public class ConditionParser {

	//===============================================================================
	// members
	
	/** Flag that determines whether the condition is seemed to be valid or not. */
	private boolean valid = true;
	/** Error message (null if there is no error). */
	private String errorMessage = null;
	
	//===============================================================================
	// methods
	
	/** Constructor.
	 * @param pool this object controls bytecode modification with Javassist.
	 * @param ancestor the Javassist representation of the model class
	 * @param conditionStr the condition in string format
	 */
	public ConditionParser(ClassPool pool, CtClass ancestor, String conditionStr) {
		if ("".equals(conditionStr.trim())) {
			valid = false;
			errorMessage = "The condition is empty.";
			return;
		}
		String src = new String(conditionStr);
		if (!src.startsWith("return")) //FIXME: "return "
			src = "return ($r) (" + src + ")";
		else 
			src = "return ($r) (" + src.substring(6) + ")";
		CtClass dummy = pool.makeClass("Dummy_Generated_Class" + String.valueOf((int)(100 * Math.random())),ancestor);
		try {
			CtMethod m = CtNewMethod.make("public boolean tryIt() { " + src + "; }",dummy);
			dummy.addMethod(m);
		} catch (CannotCompileException e) {
			valid = false;
			if (e.getReason().equals("[source error] bad filed access"))
				errorMessage = "[source error] not a conditional expression";
			else
				errorMessage = e.getReason();
		}
	}
	
	//--------------------------------------------------------------------------------
	public boolean isValid() { return valid; }
	public String getMessage() { return (errorMessage == null ? "" : errorMessage); }
}
