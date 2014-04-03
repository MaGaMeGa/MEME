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
package ai.aitia.meme.paramsweep.gui.info;

/** This class represents a contant value. The wizard uses this class to allows to 
 *  the user to use constant values as actual parameters of statistics.
 */
public class ConstantInfo extends MemberInfo {

	//===============================================================================
	// members

	private static final long serialVersionUID = 1273737900396203308L;

	/** The value of the constant. */
	protected double value;
	
	//===============================================================================
	// methods
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param value the value of the constant
	 */
	public ConstantInfo(double value) {
		super(String.valueOf(value),"double",Double.TYPE);
		this.value = value;
	}
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param value the value of the constant
	 */
	public ConstantInfo(Number value) {
		this(value.doubleValue());
	}
	
	//-------------------------------------------------------------------------------
	public double getValue() { return value; }
	
	//-------------------------------------------------------------------------------
	@Override
	public String toString() {
		return String.valueOf(value);
	}
	
	//-------------------------------------------------------------------------------
	@Override
	public boolean equals(Object o) {
		if (o instanceof ConstantInfo) {
			ConstantInfo that = (ConstantInfo)o;
			return this.value == that.value;
		}
		return false;
	}
}
