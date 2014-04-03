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
package ai.aitia.meme.paramsweep.coltPlugin;

import java.util.Arrays;
import java.util.List;

/** Statistic method decriptor plugin. This method returns log<sub>2</sub>value. */
public class Colt_Log2 extends Colt_Log {
	
	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	@Override public String getName() { return "log2_"; }
	@Override public String getLocalizedName() { return super.getLocalizedName() + " (2-based)"; }
	@Override public String getFullyQualifiedName() { return super.getFullyQualifiedName() + "2"; }
	@Override public int getNumberOfParameters() { return 1; }
	@Override public List<String> getParameterNames() { return Arrays.asList(new String[] { "value" }); }
	@Override public List<Class> getParameterTypes() { return Arrays.asList(new Class[] { Double.TYPE }); }
	@Override public String getDescription() { return "Returns the 2-based logarithm of the value."; }
	@Override
	public List<String> getParameterDescriptions() { 
		return Arrays.asList(new String[] { "Value" });
	}
}
