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
package ai.aitia.meme.database;

import java.util.ArrayList;

public class Run implements Comparable<Run> {
	
	//====================================================================================================
	// members
			
	/** Id of the run. */
	public int	run;
	
	/** Rows belongs to the run. */ 
	public ArrayList<Result.Row> rows = new ArrayList<Result.Row>();
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public Run(int r) { run = r; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean equals(Object o) {
		if (o instanceof Run) {
			Run that = (Run) o;
			return this.run == that.run;
		}
		return false;
	}
	
	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public int compareTo(Run o) {
		if (this.equals(o))
			return 0;
		if (this.run < o.run)
			return -1;
		return 1;
	}
}
