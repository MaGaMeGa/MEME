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
package ai.aitia.meme.paramsweep.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Enumeration;

import javassist.ClassPool;
import javassist.CtClass;

import javax.swing.tree.DefaultMutableTreeNode;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.utils.ParameterEnumeration;

/** This class generates the Repast parameter file to the model. */ 
public class RepastJPlainTextParameterFileGenerator {

	//===============================================================================
	// members
	
	public final static String LEFT_BRACE = "{\n";
	public final static String RIGHT_BRACE = "}\n";
	
	// keywords of the parameter file
	/** Parameter file keyword. */
	public static final String RUNS 			= "runs: ";
	/** Parameter file keyword. */
	public static final String START 			= "start: ";
	/** Parameter file keyword. */
	public static final String END 				= "end: ";
	/** Parameter file keyword. */
	public static final String INCR 			= "incr: ";
	/** Parameter file keyword. */
	public static final String SET 				= "set: ";
	/** Parameter file keyword. */
	public static final String SET_LIST			= "set_list: ";
	/** Parameter file keyword. */
	public static final String SET_BOOLEAN		= "set_boolean: ";
	/** Parameter file keyword. */
	public static final String SET_BOOLEAN_LIST = "set_boolean_list: ";
	/** Parameter file keyword. */
	public static final String SET_STRING		= "set_string: ";
	/** Parameter file keyword. */
	public static final String SET_STRING_LIST	= "set_string_list: ";
	/** Parameter file keyword. */
	public static final String SET_FILE			= "set_file: ";
	/** Parameter file keyword. */
	public static final String SET_ENUM			= "set_enum: ";
	/** Parameter file keyword. */
	public static final String SET_ENUM_LIST	= "set_enum_list: ";
	
	/** The destination file object. */
	private File dest = null;
	
	//===============================================================================
	// methods
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param dest the destination file object
	 */
	public RepastJPlainTextParameterFileGenerator(File dest) {
		if (dest == null)
			throw new IllegalArgumentException();
		this.dest = dest;
	}
	
	//-------------------------------------------------------------------------------
	/** Generates the parameter file from the parameter tree.
	 * @param root the root node of the parameter tree
	 * @throws IOException if any problem occures during the file creation
	 */
	public void generateFile(DefaultMutableTreeNode root) throws IOException {
		String finalRes = prettyPrint(generate(root));
		PrintWriter pw = new PrintWriter(new FileWriter(dest));
		pw.print(finalRes);
		pw.flush();
		pw.close();
	}
	
	//--------------------------------------------------------------------------------
	/** Generates the content of the parameter file from the parameter tree.
	 * @param root the root node of the parameter tree
	 */
	public static String generate(DefaultMutableTreeNode root) {
		StringBuilder output = new StringBuilder();
		Enumeration<DefaultMutableTreeNode> e = new ParameterEnumeration(root);
		DefaultMutableTreeNode prev = null;
		int braceNo = 0;
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode node = e.nextElement();
			if (node.equals(root)) continue;
			ParameterInfo info = (ParameterInfo) node.getUserObject();
			if (node.getParent().equals(root)) {
				for (int i = 0;i < braceNo;++i) 
					output.append(RIGHT_BRACE);
				braceNo = 0;
				output.append("\n" + generateOutput(info));
				braceNo++;
			} else {
				if (prev.equals(node.getParent())) {
					output.append(LEFT_BRACE);
					braceNo++;
				} else {
					// currently we don't use this branch
					System.out.println("Wrong branch");
					output.append(RIGHT_BRACE);
					braceNo--;
					DefaultMutableTreeNode dmt = (DefaultMutableTreeNode) prev.getParent();
					while (!dmt.equals(node.getParent())) {
						output.append(RIGHT_BRACE);
						output.append(RIGHT_BRACE);
						braceNo -= 2;
						dmt = (DefaultMutableTreeNode) dmt.getParent();
					}
				}
				output.append(generateOutput(info));
				braceNo++;
			}
			prev = node;
		}
		for (int i = 0;i < braceNo;++i)  
			output.append(RIGHT_BRACE);
		return output.toString().substring(1);
	}
	
	//--------------------------------------------------------------------------------
	/** Creates a new (parameter) file object
	 * @param model the original model class
	 * @param modelFileName the path of the class file belongs to the model class
	 */
	public static String generateEmptyFilePath(ClassPool classPool, String modelFileName) {
		File f = new File(modelFileName);
		CtClass	clazz = null;
		try {
			InputStream ins = new FileInputStream(f);
			clazz = classPool.makeClass(ins);
			clazz.stopPruning(true);
			ins.close();
		} catch (IOException e) {
			e.printStackTrace(ParameterSweepWizard.getLogStream());
		} finally {
			if (clazz != null)
				clazz.defrost();
		}
		String path = modelFileName.substring(0,modelFileName.lastIndexOf(File.separator));
		String name = clazz.getSimpleName() + "_parameters";
		File res = new File(path + File.separator + name + ".txt");
		int number = 0;
		while (res.exists()) 
			res = new File(path + File.separator + name + String.valueOf(number++) + ".txt");
		return res.getAbsolutePath();
	}
	
	//-------------------------------------------------------------------------------
	/** Generates and returns the parameter file part according to this parameter. */
	public static String generateOutput(ParameterInfo info) {
		StringBuilder sb = new StringBuilder();
		sb.append(RUNS);
		sb.append(info.getRuns());
		sb.append("\n");
		sb.append(info.getName());
		sb.append(" {\n");
		switch (info.getDefinitionType()) {
		case ParameterInfo.CONST_DEF	: if (info.isNumeric())
						  	  				  sb.append(SET);
						  				  else if (info.isBoolean())
						  					  sb.append(SET_BOOLEAN);
						  				  else if (info.isFile())
						  					  sb.append(SET_FILE);
						  				  else if (info.isEnum())
						  					  sb.append(SET_ENUM);
						  				  else
						  					  sb.append(SET_STRING);
						  				  sb.append(info.valuesToString());
						  				  break;
		case ParameterInfo.LIST_DEF  	: if (info.isNumeric())
						  	  				  sb.append(SET_LIST);
						  				  else if (info.isBoolean())
						  					  sb.append(SET_BOOLEAN_LIST);
						  				  else if (info.isEnum())
						  					  sb.append(SET_ENUM_LIST);
						  				  else
						  					  sb.append(SET_STRING_LIST);
						  				  sb.append(info.valuesToString());
						  				  break;
		case ParameterInfo.INCR_DEF		: info.transformIfNeed();
						  				  sb.append(START);
						  				  sb.append(info.startToString());
						  				  sb.append("\n");
						  				  sb.append(END);
						  				  sb.append(info.endToString());
						  				  sb.append("\n");
						  				  sb.append(INCR);
						  				  sb.append(info.stepToString());
		}
		sb.append("\n");
		return sb.toString();
	}
	
	//================================================================================
	// private methods
	
	//--------------------------------------------------------------------------------
	/** Formats the parameter file content <code>orig</code> and returns the formatted string. */
	private String prettyPrint(String orig) {
		StringBuilder sb = new StringBuilder();
		int tabNo = 0;
		String[] lines = orig.split("\n");
		for (String line : lines) {
			line = line.trim();
			if (line.endsWith("}"))
				tabNo--;
			for (int i = 0;i < tabNo;++i)
				sb.append("\t");
			if (line.endsWith("{"))
				tabNo++;
			sb.append(line + "\n");
		}
		return sb.toString();
	}
}
