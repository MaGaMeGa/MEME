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

/**
 * This class contains some Fractional Factorial Design configurations. It has
 * a static method that generates the specified design if it is in the source.
 * In the future it should use a file to store the configurations, that can be
 * altered by the user.
 * @author Ferschl
 */
public class FactorialFractionalPresets {
	
	/**
	 * Returns a 2<sup>k-p</sup> <code>FactorialDesign</code> object or null if 
	 * it does not know about such a setting. 
	 * @param k	The number of factors.
	 * @param p	The design size will be divided by 2<sup>p</sup>.
	 * @return	A 2<sup>k-p</sup> <code>FactorialDesign</code> object or null if 
	 * it does not know about such a setting.
	 */
	public static FactorialDesign getFractionalFactorialDesign(int k, int p){
		FactorialDesign ret = null;
		switch (k) {
        case 3: 
	        if(p == 1) ret = new FactorialDesign(2,"AB");
	        break;
        case 4:
	        if(p == 1) ret = new FactorialDesign(3,"ABC");
	        break;
        case 5:
	        if(p == 1) ret = new FactorialDesign(4,"ABCD");
	        if(p == 2) ret = new FactorialDesign(3,"AB", "AC");
	        break;
        case 6:
	        if(p == 1) ret = new FactorialDesign(5,"ABCDE");
	        if(p == 2) ret = new FactorialDesign(4,"ABC", "BCD");
	        if(p == 3) ret = new FactorialDesign(3,"AB", "AC", "BC");
	        break;
        case 7:
	        if(p == 1) ret = new FactorialDesign(6,"ABCDEF");
	        if(p == 2) ret = new FactorialDesign(5,"ABCD", "ABDE");
	        if(p == 3) ret = new FactorialDesign(4,"ABC", "BCD", "ACD");
	        if(p == 4) ret = new FactorialDesign(3,"AB", "AC", "BC", "ABC");
	        break;
        case 8:
	        if(p == 1) ret = new FactorialDesign(7,"ABCDEFG");
	        if(p == 2) ret = new FactorialDesign(6,"ABCD", "ABEF");
	        if(p == 3) ret = new FactorialDesign(5,"ABC", "ABD", "BCDE");
	        if(p == 4) ret = new FactorialDesign(4,"BCD", "ACD", "ABC", "ABD");
	        break;
        case 9:
	        if(p == 1) ret = new FactorialDesign(8,"ABCDEFGH");
	        if(p == 2) ret = new FactorialDesign(7,"ACDFG","BCEFG");
	        if(p == 3) ret = new FactorialDesign(6,"ABCD", "ACEF", "CDEF");
	        if(p == 4) ret = new FactorialDesign(5,"BCDE", "ACDE", "ABDE", "ABCE");
	        if(p == 5) ret = new FactorialDesign(4,"ABC", "BCD", "ACD", "ABD", "ABCD");
	        break;
        case 10:
	        if(p == 1) ret = new FactorialDesign(9,"ABCDEFGHI");
	        if(p == 2) ret = new FactorialDesign(8,"CDEFGH","ABEFGH");
	        if(p == 3) ret = new FactorialDesign(7,"ABCG","BCDE", "ACDF");
	        if(p == 4) ret = new FactorialDesign(6,"BCDF", "ACDF", "ABDE", "ABCE");
	        if(p == 5) ret = new FactorialDesign(5,"ABCD", "ABCE", "ABDE", "ACDE", "BCDE");
	        if(p == 6) ret = new FactorialDesign(4,"ABC", "BCD", "ACD", "ABD", "ABCD", "AB");
	        break;
        case 11:
	        if(p == 1) ret = new FactorialDesign(10,"ABCDEFGHIJ");
	        if(p == 2) ret = new FactorialDesign(9,"CDEFGHI","ABFGHI");
	        if(p == 3) ret = new FactorialDesign(8,"BCDEFGH","AEFGH", "ACDGH");
	        if(p == 4) ret = new FactorialDesign(7,"ABCG","BCDE", "ACDF", "ABCDEFG");
	        if(p == 5) ret = new FactorialDesign(6,"CDE", "ABCD", "ABF", "BDEF", "ADEF");
	        if(p == 6) ret = new FactorialDesign(5,"ABC", "BCD", "CDE", "ACD", "ADE", "BDE");
	        if(p == 7) ret = new FactorialDesign(4,"ABC", "BCD", "ACD", "ABD", "ABCD", "AB", "AC");
	        break;
//        case 12:
//	        if(p == 1) ret = new FactorialDesign(11,"ABCDEFGHIJK");
//	        if(p == 2) ret = new FactorialDesign(10,"","");
//	        if(p == 3) ret = new FactorialDesign(9,"","","");
//	        if(p == 4) ret = new FactorialDesign(8,"","","","");
//	        if(p == 5) ret = new FactorialDesign(7,"","","","","");
//	        if(p == 6) ret = new FactorialDesign(6,"","","","","","");
//	        break;
        case 12:
	        if(p == 1) ret = new FactorialDesign(11,"ABCDEFGHIJK");
	        if(p == 2) ret = new FactorialDesign(10,"DEFGHIJ","ABCGHIJ");
	        if(p == 3) ret = new FactorialDesign(9,"ABCDEFGHI","EFGHI","CDGHI");
	        if(p == 4) ret = new FactorialDesign(8,"BCDEFGH","AEFGH","ACDGH","ABDFH");
	        if(p == 5) ret = new FactorialDesign(7,"ABCDEFG","CDEFG","BDEFG","ADEFG","BCEFG");
	        if(p == 6) ret = new FactorialDesign(6,"ABCDEF","CDEF","BDEF","ADEF","BCEF","ACEF");
	        break;
        case 13:
	        if(p == 1) ret = new FactorialDesign(12,"ABCDEFGHIJKL");
	        if(p == 2) ret = new FactorialDesign(11,"CDEFGHIJK","ABGHIJK");
	        if(p == 3) ret = new FactorialDesign(10,"ABCDEFGHIJ","EFGHIJ","CDGHIJ");
	        if(p == 4) ret = new FactorialDesign(9,"ABCDEFGHI","EFGHI","CDGHI","BDFHI");
	        if(p == 5) ret = new FactorialDesign(8,"ABCDEFGH","DEFGH","BCFGH","ACEGH","BDGH");
	        if(p == 6) ret = new FactorialDesign(7,"ABCDEFG","CDEFG","BDEFG","ADEFG","BCEFG","ACEFG");
	        break;
        case 14:
	        if(p == 1) ret = new FactorialDesign(13,"ABCDEFGHIJKLM");
	        if(p == 2) ret = new FactorialDesign(12,"DEFGHIJKL","ABCHIJKL");
	        if(p == 3) ret = new FactorialDesign(11,"ABCDEFGHIJK","FGHIJK","CDEIJK");
	        if(p == 4) ret = new FactorialDesign(10,"ABCDEFGHIJ","EFGHIJ","CDGHIJ","BDFHIJ");
	        if(p == 5) ret = new FactorialDesign(9,"ABCDEFGHI","EFGHI","CDGHI","BDFHI","ACEHI");
	        if(p == 6) ret = new FactorialDesign(8,"ABCDEFGH","DEFGH","BCFGH","ACEGH","BDGH","CEFH");
	        if(p == 7) ret = new FactorialDesign(7,"ABCDEFG","CDEFG","BDEFG","ADEFG","BCEFG","ACEFG","ABEFG");
	        break;
        case 15:
	        if(p == 1) ret = new FactorialDesign(14,"ABCDEFGHIJKLMN");
	        if(p == 2) ret = new FactorialDesign(13,"CDEFGHIJKLM","ABHIJKLM");
	        if(p == 3) ret = new FactorialDesign(12,"BCDEFGHIJKL","AGHIJKL","ADEFJKL");
	        if(p == 4) ret = new FactorialDesign(11,"ABCDEFGHIJK","FGHIJK","CDEIJK","ABEHJK");
	        if(p == 5) ret = new FactorialDesign(10,"ABCDEFGHIJ","EFGHIJ","CDGHIJ","BDFHIJ","ACEHIJ");
	        if(p == 6) ret = new FactorialDesign(9,"ABCDEFGHI","EFGHI","CDGHI","BDFHI","ACEHI","ADFGI");
	        if(p == 7) ret = new FactorialDesign(8,"ABCDEFGH","DEFGH","BCFGH","ACEGH","BDGH","CEFH","ADFH");
	        if(p == 11) ret = new FactorialDesign(4,"AB", "AC", "AD", "BC", "BD", "CD", "ABC", "ABD", "ACD", "BCD", "ABCD");
	        break;
        case 16:
	        if(p == 1) ret = new FactorialDesign(15,"ABCDEFGHIJKLMNO");
	        if(p == 2) ret = new FactorialDesign(14,"DEFGHIJKLMN","ABCIJKLMN");
	        if(p == 3) ret = new FactorialDesign(13,"ABCDEFGHIJKLM","GHIJKLM","DEFJKLM");
	        if(p == 4) ret = new FactorialDesign(12,"BCDEFGHIJKL","AGHIJKL","ADEFJKL","ABCFIKL");
	        if(p == 5) ret = new FactorialDesign(11,"ABCDEFGHIJK","FGHIJK","CDEIJK","ABEHJK","ABDGIK");
	        if(p == 6) ret = new FactorialDesign(10,"ABCDEFGHIJ","EFGHIJ","CDGHIJ","BDFHIJ","ACEHIJ","ADFGIJ");
	        if(p == 7) ret = new FactorialDesign(9,"ABCDEFGHI","EFGHI","CDGHI","BDFHI","ACEHI","ADFGI","ABEGI");
	        if(p == 8) ret = new FactorialDesign(8,"ABCDEFGH","DEFGH","BCFGH","ACEGH","BDGH","CEFH","ADFH","ABEH");
	        break;
        case 31:
	        if(p == 26) ret = new FactorialDesign(4,"AB", "AC", "AD", "AE", "BC", "BD", "BE", "CD", "CE", "DE", "ABC", "ABD", "ABE", "ACD", "ACE", "ADE", "BCD", "BCE", "BDE", "CDE", "ABCD", "ABCE", "ABDE", "ACDE", "BCDE", "ABCDE");
	        break;

        default:
	        break;
        }
		if (ret != null)
			ret.setOriginal(true);
		return ret;
	}

	
	public static boolean isAPreset(FactorialDesign fd) {
		boolean ret = false;
		FactorialDesign preset = getFractionalFactorialDesign(fd.getK(), fd.getP());
		if (preset != null && preset.compareTo(fd) == 0) ret = true;
		return ret;
	}
}
