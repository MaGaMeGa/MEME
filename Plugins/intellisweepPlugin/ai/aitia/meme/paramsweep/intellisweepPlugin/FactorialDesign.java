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

import java.util.List;
import java.util.Vector;

/**
 * This class represents a 2-level factorial design.
 * @author Ferschl
 */
public class FactorialDesign implements Comparable<FactorialDesign>{

	/**	Contains the design in form of <code>Integer</code>s of the value -1 or +1. */
	protected Vector<Vector<Integer>> design = null;
	/** Contains the confounding definition as <code>String</code>s, where
	 * letter A refers to the first column (factor), and AB means the 'product'
	 * of column A and B. */
	protected Vector<String> columnGenerators = null;
	/** True if this design is a fractional preset, false otherwise (new 
	 * design by the user, or a modified preset). */
	protected boolean original = false;
	
	/**
	 * Creates a Full Factorial design with <code>factor</code> factors.
	 * @param factors	The number of factors in the Full Factorial design.
	 */
	public FactorialDesign(int factors){
		design = new Vector<Vector<Integer>>(factors);
		columnGenerators = new Vector<String>(factors);
		int rows = 1;
		for (int i = 0; i < factors; i++) {
			columnGenerators.add(new Character((char)('A' + i)).toString());
	        rows *= 2;
        }
		int changePeriod = 1;
		for (int i = 0; i < factors; i++) {
			Vector<Integer> column = new Vector<Integer>(rows);
			int periodLength = 0;
			boolean lowPeriod = true;
			for (int j = 0; j < rows; j++) {
	            if(lowPeriod){
	            	column.add(-1);
	            }else{
	            	column.add(+1);
	            }
	            periodLength++;
	            if(periodLength == changePeriod){
	            	periodLength = 0;
	            	lowPeriod = !lowPeriod;
	            }
			}
			design.add(column);
			changePeriod *= 2;
		}
	}
	
	/**
	 * This method first creates a Full Factorial design, then adds columns 
	 * defined by the parameters, to create a Fractional Factorial design. 
	 * @param factors	The number of factors in the initial Full Factorial design.
	 * This determines the row count (number of runs).
	 * @param strings	The generators for the new columns.
	 */
	public FactorialDesign(int factors, String ... strings){
		this(factors);
		for (int i = 0; i < strings.length; i++) {
	        this.addColumnByMultiplying(strings[i].toUpperCase());
        }
	}
	
	/**
	 * This method first creates a Full Factorial design, then adds columns 
	 * defined by the <code>generators</code> parameter, to create a Fractional 
	 * Factorial design.
	 * @param factors	The number of factors in the initial Full Factorial design.
	 * This determines the row count (number of runs).
	 * @param generators	The generators for the new columns.
	 */
	public FactorialDesign(int factors, final List<String> generators){
		this(factors);
		for (int i = 0; i < generators.size(); i++) {
	        this.addColumnByMultiplying(generators.get(i).toUpperCase());
        }
	}

	/**
	 * This method first creates a Full Factorial design, then adds columns 
	 * defined by the <code>generators</code> parameter, to create a Fractional 
	 * Factorial design.
	 * @param factors	The number of factors in the initial Full Factorial design.
	 * This determines the row count (number of runs).
	 * @param generators	The generators for the new columns.
	 */
	public FactorialDesign(int k, int p){
		this(k-p);
		for (int i = 0; i < p; i++) {
	        this.addEmptyColumn();
        }
	}

	protected void addEmptyColumn() {
		Vector<Integer> temp = new Vector<Integer>();
		for (int i = 0; i < design.get(0).size(); i++) {
	        temp.add(0);
        }
		design.add(temp);
		columnGenerators.add("?");
    }
	
	protected void setColumnAt(int i, final Vector<Integer> column){
		design.set(i, column);
	}
	
	public void setColumnAtByMultiplying(int i, String generator){
		setColumnAt(i, getMultipliedColumn(generator));
		if (original) {
			String old = columnGenerators.get(i);
			if (old.compareTo(generator.toUpperCase()) != 0)
				original = false;
		}
		columnGenerators.set(i, generator.toUpperCase());
	}

	/**
	 * Returns the <code>i</code>th column (one factor) of the design.
	 * @param i	Which column to get.
	 * @return	A <code>Vector&lt;Integer&gt;</code> containing the values for
	 * 			the given factor.
	 */
	public Vector<Integer> getColumn(int i){
		return design.get(i);
	}
	
	/**
	 * Returns the column named by the character <code>c</code>. The first
	 * column is <code>'A'</code>, the second is <code>'B'</code>, etc. 
	 * @param c	Which column to get.
	 * @return	A <code>Vector&lt;Integer&gt;</code> containing the values for
	 * 			the given factor.
	 */
	public Vector<Integer> getColumn(char c){
		char cc = Character.toUpperCase(c);
		return getColumn(cc-'A');
	}
	
	/**
	 * Returns the column named by the first character in <code>s</code>. The first
	 * column is <code>'A'</code>, the second is <code>'B'</code>, etc. 
	 * @param s	Its first character tells which column to get.
	 * @return	A <code>Vector&lt;Integer&gt;</code> containing the values for
	 * 			the given factor.
	 */
	public Vector<Integer> getColumn(String s){
		char cc = Character.toUpperCase(s.charAt(0));
		return getColumn(cc-'A');
	}
	
	/**
	 * Adds a column (a new factor) to the design. Does not alter the row count
	 * (number of runs).
	 * @param newColumn	The new column to add.
	 */
	public void addColumn(final Vector<Integer> newColumn){
		design.add(newColumn);
	}
	
	protected Vector<Integer> getMultipliedColumn(String newColumnGenerator){
		char[] columns = null;
		int invert = 1;
		if(newColumnGenerator.charAt(0) == '-'){
			invert = -1;
			columns = newColumnGenerator.substring(1).toCharArray();
		} else{
			columns = newColumnGenerator.toCharArray();
		}
		Vector<Integer> newColumn = new Vector<Integer>(design.get(0).size());
		for (int i = 0; i < design.get(0).size(); i++) {
			int newValue = 1;
			for (int j = 0; j < columns.length; j++) {
				newValue *= getColumn(columns[j]).get(i);
			}
			newColumn.add(invert * newValue);
        }
		return newColumn;
	}
	
	/**
	 * Adds a column (a new factor) to the design by multiplying some of the present
	 * columns. Does not alter the row count (number of runs). 
	 * @param newColumnGenerator	A String containing the one-letter names of the
	 * columns that are to be multiplied. An optional <code>'-'</code> (minus sign)
	 * can lead the letters, that inverts the new column.
	 */
	public void addColumnByMultiplying(String newColumnGenerator){
		columnGenerators.add(newColumnGenerator);
		addColumn(getMultipliedColumn(newColumnGenerator));
	}
	
	public String getColumnGeneratorAt(int i){
		if(i<columnGenerators.size()){
			return columnGenerators.get(i);
		} else{
			return "?";
		}
	}
	
	/**
	 * Returns a viewable String representation of the design (column names, row
	 * numbers, +,- signs). 
	 */
	@Override
	public String toString(){
		StringBuilder ret = new StringBuilder();
		if(design == null || design.size() == 0 || design.get(0) == null || design.get(0).size() == 0){
			ret.append("Empty design.");
		}else{
			//If there is confounding, we print the generators:
			if(design.get(0).size() < Math.pow(2, design.size())){
				//the design is confounded
				ret.append("Fractional factorial design, generators:\n");
				char columnName = 'A';
				for (int i = 0; i < design.size(); i++) {
	                ret.append(columnName);
	                ret.append(" = " + getColumnGeneratorAt(i) + "\n");
	                columnName++;
				}
				ret.append("\n");
			} else{
				ret.append("Full factorial design:\n");
			}
			int rowNumberWidth = 5;
			for (int i = 0; i < rowNumberWidth+1; i++) {
	            ret.append(' ');
            }
			for (char i = 'A'; i < design.size() + 'A'; i++) {
	            ret.append(i);
	            ret.append(' ');
            }
			ret.append("\n");
			for (int i = 0; i < design.get(0).size(); i++) {
				String rowNo = new Integer(i+1).toString();
				if(rowNo.length() < rowNumberWidth){
					for (int j = 0; j < rowNumberWidth - rowNo.length(); j++) {
	                    ret.append(' ');
                    }
				}
				ret.append(rowNo+" ");
				for (int j = 0; j < design.size(); j++) {
					if(design.get(j).get(i) < 0){
						ret.append("- ");
					} else if(design.get(j).get(i) > 0){
						ret.append("+ ");
					} else{
						ret.append("X ");
					}
				}
				ret.append("\n");
			}
		}
		return ret.toString();
	}
	
	
	/**
	 * @return	the number of factors.
	 */
	public int getK(){
		return design.size();
	}
	
	/**
	 * @return	the p from 2**(k-p)
	 */
	public int getP(){
		int p = 0;
		for (int i = 0; i < columnGenerators.size(); i++) {
	        if(columnGenerators.get(i).length()>1 || columnGenerators.get(i).compareTo("?") == 0) p++;
        }
		return p;
	}
	
	/**
	 * @return	true, if the generators are filled in, no '?'-s are present
	 */
	public boolean getReadyStatus(){
		for (int i = 0; i < columnGenerators.size(); i++) {
	        if(columnGenerators.get(i).startsWith("?")) return false;
        }
		return true;
	}
	
	public static void main(String args[]){
		FactorialDesign test = new FactorialDesign(3);
		System.out.println(test);
		test = new FactorialDesign(4);
		test.addColumnByMultiplying("-ABCD");
		System.out.println(test);
		test = new FactorialDesign(3,"AB", "AC");
		System.out.println(test);
		test = new FactorialDesign(5,2);
		System.out.println(test);
		System.out.println(Integer.MAX_VALUE);
		System.out.println(Integer.MAX_VALUE + 1);
		System.out.println(Integer.MIN_VALUE);
		Vector<Integer> testvect = new Vector<Integer>();
		testvect.add(1);
		testvect.add(2);
		testvect.add(3);
		System.out.println(testvect);
		testvect.addAll(testvect);
		System.out.println(testvect);
	}

	/**
	 * @return factorial design resolution. 0 if full factorial, the length of the shortest
	 *  generator+1 in case of fractional factorial design.
	 *  */
	public int getDesignResolution() {
		int designResolution = 0;
		int min = Integer.MAX_VALUE;
		for (int i = 0; i < columnGenerators.size(); i++) {
			int length = columnGenerators.get(i).length();
			if (columnGenerators.get(i).charAt(0) == '-')
				length--;
			if (length > 1 && length < min)
				min = length;
		}
		if (min < Integer.MAX_VALUE) designResolution = min+1;
		return designResolution; 
	}

	public boolean isOriginal() { return original; }
	public void setOriginal(boolean original) { this.original = original; }

	/**
	 * @return  
	 */
	public int compareTo(FactorialDesign o) {
		int ret = this.getK()-o.getK();
		if (ret == 0) {
			ret = o.getP() - this.getP();
			if (ret == 0) {
				for (int i = 0; i < this.columnGenerators.size() && ret == 0; i++) {
					if (this.columnGenerators.get(i).compareTo(o.columnGenerators.get(i)) != 0) {
						ret = 1;
					}
				}
			} else { 
				ret = (ret < 0 ? -2 : 2); //2 is for designs with same K but different P
			}
		} else {
			//adding 1 in absolute value to bias it from the designs with same K
			ret += (ret < 0 ? -2 : 2);
		}
		
		return ret;
	}
}
