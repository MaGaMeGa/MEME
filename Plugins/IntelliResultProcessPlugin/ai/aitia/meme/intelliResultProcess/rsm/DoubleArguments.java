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

import java.util.List;
import java.util.Vector;

public class DoubleArguments implements IArguments {
	
	protected Vector<Double> values = null;
	protected Vector<String> argumentNames = null;
	
	public DoubleArguments(){
		values = new Vector<Double>();
		argumentNames = new Vector<String>();
	}

	public List<String> getArgumentNames() {
		return argumentNames;
	}

	public Vector<Double> getValues() {
		return values;
	}
	
	public double getValueForName(String name){
		int i = 0;
		while(!argumentNames.get(i).equals(name) && i < argumentNames.size()) i++;
		if(i >= values.size()) throw new IllegalArgumentException();
		return values.get(i);
	}

}
