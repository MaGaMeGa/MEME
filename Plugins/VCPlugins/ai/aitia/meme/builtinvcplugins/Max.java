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
public class Max extends AbstractBuiltinFn
{
	int maximize = Integer.MAX_VALUE;

	//-------------------------------------------------------------------------
	public String getLocalizedName() {
		return "MAX()";
	}

	//-------------------------------------------------------------------------
	public ColumnType getReturnType(IContext context)
	{
		ColumnType ans = null;
		for (Parameter p : context.getArgumentPars()) {
			if (ColumnType.BOOLEAN.equals(p.getDatatype())) return null;
			if (ans == null) ans = p.getDatatype();
			else ans = ans.getUnion(p.getDatatype());
		}
		return ans;
	}

	//-------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public Object compute(IContext context) {
		Object ans		= null;
		String ansStr	= null;
		ColumnType ansNrT = null;

		for (Object[] v : context.getArguments()) {
			for (Object o : v) {
				if (o == null) continue;		// ignore null values
				if (ans != null) {
					int res;
					if (ansNrT != null) {
						if (o instanceof Number) {
							// Sajnos compareTo() minden tipusra exception-t dob kiveve
							// a sajat tipusara, ezert kell igy kavarni es konvertalgatni
							res = ((Comparable)ans).compareTo(ansNrT.convert(o));
						} else {
							if (ansStr == null) ansStr = ans.toString();
							res = ansStr.compareTo(o.toString());
						}
					} else {
						res = ansStr.compareTo(o.toString());
					}
					if ((res ^ maximize) >= 0)
						continue;
				}
				ans = o;
				if (ans instanceof Number) {
					ansNrT = ColumnType.convertJavaClass(ans.getClass());
					ans	   = ansNrT.convert(ans);
					ansStr = null;
				} else {
					ansNrT = null;
					ansStr = ans.toString();
				}
			}
		}
		return ans;
	}
}
