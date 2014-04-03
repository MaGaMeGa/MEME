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

public class Permutation {
	private static void swap(int[] p, int i, int j) {
 		int t= p[i];
		p[i]= p[j];
		p[j]= t;
	}
 
	public static boolean nextPerm(int[] p) {
 		int i;
 		for (i= p.length-1; i-- > 0 && p[i] > p[i+1];)
			;
 		if (i < 0)
			return false;
 		int j;
 		for (j= p.length; --j > i && p[j] < p[i];)
			;
 		swap(p, i, j);
 		for (j= p.length; --j > ++i; swap(p, i, j))
			;
 		return true;
	}
 
	public static void print(int[] p) {
 		for (int i= 0; i < p.length; ++i)
			System.out.print(p[i]+" ");
		System.out.println();
	}
 
	public static void main(String args[]) {
 		int[] p= { 0, 1, 2, 3, 4};
// 		print(p);
		do 
			print(p);
//			;		
		while (nextPerm(p));
//		print(p);
	}

}
