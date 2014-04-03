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
package ai.aitia.meme.paramsweep.intellisweepPlugin;

import java.util.Vector;

/**
 * This class helps to represent a fractional factorial design by encapsulating
 * the information about the factors, about the design, caches some past designs
 * (later: presets can be pre-loaded), and provides methods for easy display of
 * data. 
 * @author Ferschl
 */
public class FactorialFractionalHelper {
	private Vector<FactorialFactorInfo> factors = null;
	private FactorialDesign actualDesign = null;
	private FactorialDesign[][] pastDesigns = null;
	private int k = 3;
	private int p = 1;
	
	/** Creates an instance of this class. */
	public FactorialFractionalHelper(){
		factors = new Vector<FactorialFactorInfo>();
		pastDesigns = new FactorialDesign[32][32];
		//I do not understand JAVA's two-dimensional array indexing. Am I stupid?
//		for (int i = 1; i < pastDesigns.length; i++) {
//	        pastDesigns[i] = new FactorialDesign[i];
//        }
	}

    
    public int factorMoveUp(int i){
    	int pos = 0;
    	if(i>0){
    		FactorialFactorInfo temp1 = factors.get(i);
    		FactorialFactorInfo temp2 = factors.get(i-1);
    		factors.set(i, temp2);
    		factors.set(i-1, temp1);
    		pos = i - 1;
    	}
    	return pos;
    }
	
    public int factorMoveDown(int i){
    	int pos = factors.size() - 1;
    	if(i<factors.size()-1){
    		FactorialFactorInfo temp1 = factors.get(i);
    		FactorialFactorInfo temp2 = factors.get(i+1);
    		factors.set(i, temp2);
    		factors.set(i+1, temp1);
    		pos = i + 1; 
    	}
    	return pos;
    }
    
    public void addFactor(FactorialFactorInfo factor){
    	if(actualDesign != null){
    		pastDesigns[k][p] = actualDesign;
    	}
    	factors.add(factor);
    	k = factors.size();
    	if(k > 2 && pastDesigns[k][p] != null){
    		actualDesign = pastDesigns[k][p];
    	} else{
    		actualDesign = FactorialFractionalPresets.getFractionalFactorialDesign(k, p);
    	}
    }
    
    public void removeFactor(FactorialFactorInfo factor){
    	if(actualDesign != null){
    		pastDesigns[k][p] = actualDesign;
    	}
    	factors.remove(factor);
    	k = factors.size();
    	if(k > 2 && pastDesigns[k][p] != null){
    		actualDesign = pastDesigns[k][p];
    	} else{
    		actualDesign = FactorialFractionalPresets.getFractionalFactorialDesign(k, p);
    	}
    }

    /** @param p the p to set */
    public void setP(int p) {
    	if(actualDesign != null){
    		pastDesigns[k][this.p] = actualDesign;
    	}
    	this.p = p;
    	if(k > 2 && pastDesigns[k][p] != null){
    		actualDesign = pastDesigns[k][p];
    	} else{
    		actualDesign = FactorialFractionalPresets.getFractionalFactorialDesign(k, p);
    	}
    }
    
	/** @return the p */
    public int getP() { return p; }
    /** @return the actualDesign */
    public FactorialDesign getActualDesign() { return actualDesign; }
    /** @param actualDesign the actualDesign to set */
    public void setActualDesign(FactorialDesign actualDesign) {
    	this.actualDesign = actualDesign;
    	this.k = actualDesign.getK();
    	this.p = actualDesign.getP();
    }
    
    public String getFactorNameConfounding(int i){
    	Character nameChar = new Character((char) ('A' + i));
    	StringBuilder ret = new StringBuilder();
    	ret.append(nameChar);
    	ret.append(": ");
    	ret.append(factors.get(i).getName() + " = " + actualDesign.getColumnGeneratorAt(i));
    	return ret.toString();
    }
    
    public String[] getFactorsNamesConfoundings(){
    	String[] ret = new String[factors.size()];
    	for (int i = 0; i < factors.size(); i++) {
	        ret[i] = getFactorNameConfounding(i);
        }
    	return ret;
    }
    
    public FactorialFactorInfo getFactorAt(int i){
    	return factors.get(i);
    }

}
