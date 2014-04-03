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
package ai.aitia.meme.intelliResultProcess.rsm;

import ai.aitia.meme.processing.model.PolynomialRegressor;


public class PolynomialRsmInfo implements IRsmInfo {
	
	PolynomialRegressor poly = null;
	
	public PolynomialRsmInfo(PolynomialRegressor poly){
		this.poly = poly;
	}

	public String getPolynomialAsString(){
		return poly.getRegressor().toString();
	}
	
	@Override
	public String toString(){
		StringBuilder ret = new StringBuilder();
		ret.append("[");
		for (int i = 0; i < poly.getVariables().size(); i++) {
			int power = poly.getRegressor().getOrder(poly.getVariables().get(i));
			if(power > 0){
				ret.append(poly.getVariables().get(i).getSymbol());
				if(power > 1) ret.append("^" + power);
				if(i < poly.getVariables().size() - 1) ret.append(", ");
			}
		}
		ret.append(" -> ");
		ret.append(poly.getResponseName());
		ret.append("]");
		return ret.toString();
	}

	public PolynomialRegressor getPoly() {
		return poly;
	}
}
