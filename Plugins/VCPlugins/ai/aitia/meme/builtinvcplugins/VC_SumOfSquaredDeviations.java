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

import ai.aitia.meme.paramsweep.colt.Descriptive;
import ai.aitia.meme.paramsweep.colt.DoubleArrayList;

public class VC_SumOfSquaredDeviations extends ColtBuiltinFn {

	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getLocalizedName() { return "SUM_OF_SQUARED_DEVIATIONS()"; }
	
	//----------------------------------------------------------------------------------------------------
	public Object compute(final IContext context) {
		final DoubleArrayList dal = convert2DAL(context);
		return Descriptive.sumOfSquaredDeviations(dal);
	}
}
