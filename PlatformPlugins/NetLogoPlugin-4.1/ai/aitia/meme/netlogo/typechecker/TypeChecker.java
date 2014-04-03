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
package ai.aitia.meme.netlogo.typechecker;

import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultReporter;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.LogoException;
import org.nlogo.api.Syntax;

public class TypeChecker extends DefaultReporter {
	
	//====================================================================================================
	// members
	
	public static final String NUMBER 	= "<number>";
	public static final String BOOLEAN	= "<boolean>";
	public static final String STRING	= "<string>";
	public static final String OTHER	= "<other>";

	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public strictfp Syntax getSyntax() { return Syntax.reporterSyntax(new int[] { Syntax.TYPE_WILDCARD },Syntax.TYPE_STRING); }

	//----------------------------------------------------------------------------------------------------
	public Object report(Argument[] argument, Context context) {
		if (isNumber(argument[0]))
			return NUMBER;
		if (isBoolean(argument[0]))
			return BOOLEAN;
		if (isString(argument[0]))
			return STRING;
		return OTHER;
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private boolean isNumber(Argument arg) {
		try {
			arg.getDoubleValue();
			return true; 
		} catch (LogoException e) {} 
		  catch (ExtensionException e) {}
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isBoolean(Argument arg) {
		try {
			arg.getBooleanValue();
			return true; 
		} catch (LogoException e) {} 
		  catch (ExtensionException e) {}
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isString(Argument arg) {
		try {
			arg.getString();
			return true; 
		} catch (LogoException e) {} 
		  catch (ExtensionException e) {}
		return false;
	}
}
