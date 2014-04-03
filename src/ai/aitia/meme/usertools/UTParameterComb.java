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
package ai.aitia.meme.usertools;

import java.util.ArrayList;
import java.util.List;

import ai.aitia.meme.utils.Utils.IUnaryCallable;

public class UTParameterComb implements Cloneable {

	//=======================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	public static interface UTParameter extends Cloneable {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public Object clone();
		public String getValue();
		public String getActualID();
	}
	
	//----------------------------------------------------------------------------------------------------
	public static class ConstantUTParameter implements UTParameter,
													   Cloneable {
		
		//====================================================================================================
		// members
		
		private String actualId = null;;
		private String value;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public ConstantUTParameter(String actualId, String value) {
			this.actualId = actualId;
			this.value = value;
		}
		
		//----------------------------------------------------------------------------------------------------
		public String getActualID() { return actualId; }
		public String getValue() { return value; }
		@Override public Object clone()  { return new ConstantUTParameter(actualId,value); }
		@Override public String toString() { return value; }
	}
	
	//----------------------------------------------------------------------------------------------------
	public static class ComputedUTParameter implements UTParameter,
													   Cloneable {
		
		//====================================================================================================
		// members
		
		protected UserToolParser parser;
		protected IUnaryCallable<String,UserToolParser> computation;
		protected String actualId = null;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public ComputedUTParameter(String actualId, UserToolParser parser, IUnaryCallable<String,UserToolParser> computation) {
			this.actualId = actualId;
			this.parser = parser;
			this.computation = computation;
		}
		
		//----------------------------------------------------------------------------------------------------
		public String getValue() { return computation.call(parser); }
		public String getActualID() { return actualId; }
		@Override public Object clone() { return new ComputedUTParameter(actualId,parser,computation); }
	}
	
	//----------------------------------------------------------------------------------------------------
	public static class CleanerComputedUTParameter extends ComputedUTParameter implements Cloneable {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public CleanerComputedUTParameter(String actualId, UserToolParser parser, IUnaryCallable<String, UserToolParser> computation) {
			super(actualId,parser,computation);
		}
		
		//----------------------------------------------------------------------------------------------------
		@Override
		public String getValue() { 
			String ans  = super.getValue();
			parser.addDeletableItem(ans);
			return ans;
		}
	}
	
	//=======================================================================================
	// members
	
	private List<UTParameter> parameters = null;
	
	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public UTParameterComb() { parameters = new ArrayList<UTParameter>(); }
	
	//---------------------------------------------------------------------------------------
	public UTParameter getParameter(int index) { return parameters.get(index); }
	public void addParameter(UTParameter param) { parameters.add(param); }
	
	//---------------------------------------------------------------------------------------
	public String getArgumentString() {
		String ans = "";
		for (UTParameter p : parameters) 
			ans += p.getValue() + " ";
		if (!"".equals(ans))
			ans = ans.substring(0,ans.length()-1);
		return ans;
	}
	
	//---------------------------------------------------------------------------------------
	@Override public Object clone() {
		UTParameterComb clone = new UTParameterComb();
		for (UTParameter p : parameters)
			clone.addParameter((UTParameter)p.clone());
		return clone;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override public String toString() { return parameters.toString(); }
}
