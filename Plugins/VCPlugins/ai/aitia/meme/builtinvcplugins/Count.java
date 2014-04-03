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
package ai.aitia.meme.builtinvcplugins;

import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.pluginmanager.Parameter;

//-----------------------------------------------------------------------------
public class Count extends AbstractBuiltinFn {
	
	//-------------------------------------------------------------------------
	public String getLocalizedName() {
		return "COUNT()";
	}

	//-------------------------------------------------------------------------
	public ColumnType getReturnType(IContext context) {
		java.util.Collection<? extends Parameter> pars = context.getArgumentPars();
		if (pars.isEmpty())
			return null;
//		for (Parameter p : pars) {
//			Class c = p.getDatatype().getJavaClass();
//			if (c == Boolean.class || c == String.class) return null;
//		}
		return ColumnType.INT;
	}

	//-------------------------------------------------------------------------
	/** 'null' values are not counted */
	public Object compute(IContext context) {
		int cnt = 0;
		for (Object[] v : context.getArguments()) {
			for (Object o : v) {
				if (o == null) continue;	// ignore null values
				cnt += 1;
			}
		}
		return cnt;
	}
}
