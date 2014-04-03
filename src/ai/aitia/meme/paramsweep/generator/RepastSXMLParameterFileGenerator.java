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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;

import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.utils.ParameterEnumeration;

public class RepastSXMLParameterFileGenerator {
	//===============================================================================
	// members
	
	public final static String PARAMETER = "parameter";
	public final static String NAME = "name";
	public final static String TYPE = "type";
	public final static String CONSTANT_TYPE = "constant_type";
	public final static String VALUE_TYPE = "value_type";
	public final static String VALUES = "values";
	public final static String VALUE = "value";
	public final static String START = "start";
	public final static String END = "end";
	public final static String STEP = "step";
	public final static String SWEEP = "sweep";
	public final static String RUNS = "runs";
	
	//possible values for attribute 'type'
	public final static String CONSTANT = "constant";
	public final static String LIST = "list";
	public final static String NUMBER = "number";
	
	//possible parameter types
	//public final static String NUMBER = "number";
	public final static String BOOLEAN = "boolean";
	public final static String STRING = "string";
	
	/** The destination file object. */
	private File dest = null;
	
	//===============================================================================
	// methods
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param dest the destination file object
	 */
	public RepastSXMLParameterFileGenerator(File dest) {
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
		String finalRes = generate( root );
		PrintWriter pw = new PrintWriter(new FileWriter(dest));
		pw.print(finalRes);
		pw.flush();
		pw.close();
	}
	
	//--------------------------------------------------------------------------------
	/** Generates the content of the parameter file from the parameter tree.
	 * @param root the root node of the parameter tree
	 */
	public static String generate( DefaultMutableTreeNode root ) {
		StringBuilder output = new StringBuilder();
		
		long run = 1;
		if (root.getChildCount() > 0) {
			ParameterInfo info = (ParameterInfo) ((DefaultMutableTreeNode)root.getFirstChild()).getUserObject();
			run = info.getRuns();
		}
		
		Enumeration<DefaultMutableTreeNode> e = new ParameterEnumeration(root);
		DefaultMutableTreeNode prev = null;
		int tagNo = 0;
		output.append("<sweep runs=\"" + run + "\">");
		while( e.hasMoreElements() ){
			DefaultMutableTreeNode node = e.nextElement();
			if( node.equals( root ) ) continue;
			ParameterInfo info = (ParameterInfo) node.getUserObject();
			if( node.getParent().equals( root ) ){
				for( int i = 0; i < tagNo; ++i ){
					if( i == 0 ) output.append( "/>" );
					else{
						output.append( "\n" );
						for( int j = 0; j < tagNo - i - j + 1; ++j )
							output.append( "\t" );
						output.append( "</parameter>" );
					}
				}
				tagNo = 0;
				output.append( "\n" );
				for( int i = 0; i < tagNo + 1; ++i )
					output.append( "\t" );
				output.append( generateXMLTag( info ) );
				tagNo++;
			}else{	//child of other parameter
				if( prev.equals( node.getParent() ) ){
					output.append( ">" );
				}else{
					// currently we don't use this branch
					System.out.println( "Wrong branch" );
					output.append( "/>" );
					tagNo--;
					DefaultMutableTreeNode dmt = (DefaultMutableTreeNode) prev.getParent();
					while( !dmt.equals( node.getParent() ) ){
						output.append( "\n" );
						for( int i = 0; i < tagNo + 1; ++i )
							output.append( "\t" );
						output.append( "</parameter>" );
						tagNo--;
						dmt = (DefaultMutableTreeNode) dmt.getParent();
					}
				}
				output.append( "\n" );
				for( int i = 0; i < tagNo + 1; ++i )
					output.append( "\t" );
				output.append( generateXMLTag( info ) );
				tagNo++;
			}
			prev = node;
		}
		
		for( int i = 0; i < tagNo; ++i ){
			if( i == 0 ) output.append( "/>" );
			else{
				output.append( "\n" );
				for( int j = 0; j < tagNo + 1; ++j )
					output.append( "\t" );
				output.append( "</parameter>" );
			}
		}

		output.append( "\n</sweep>" );
		return output.toString().substring(0);
	}
	
	//--------------------------------------------------------------------------------
	/** Creates a new (parameter) file object
	 * @param model the original model class
	 * @param modelFileName the path of the class file belongs to the model class
	 */
	public static String generateEmptyXMLFilePath(String modelFileName) {
		//if (model == null)
			//throw new IllegalArgumentException();
		String path = modelFileName.substring(0,modelFileName.lastIndexOf(File.separator));
		String name = /*model.getSimpleName()*/ "batch_parameters";
		File res = new File(path + File.separator + name + ".xml");
		int number = 0;
		while (res.exists()) 
			res = new File(path + File.separator + name + String.valueOf(number++) + ".xml");
		return res.getAbsolutePath();
	}
	
	//================================================================================
	// private methods
	
	//--------------------------------------------------------------------------------
	protected static String generateXMLTag( ParameterInfo info ){
		StringBuilder sb = new StringBuilder();
		sb.append( "<parameter name=\"" );
		sb.append( info.getName() );
		sb.append( "\" type=\"" );
		switch( info.getDefinitionType() ) {
		case ParameterInfo.CONST_DEF	: sb.append( "constant\" constant_type=\"" );
						  if (info.isNumeric())
							  sb.append("number\"");
						  else if ( info.isBoolean() ) sb.append( "boolean\"" );
						  else sb.append( "string\"" );
						  sb.append( " value=\"" );
						  sb.append( toStringWithoutScientificNotation( info.getValues().get(0), 
								  			 						    info.getType() ) );
						  sb.append(typeSuffix(info.getJavaType()));
						  sb.append( "\"" );
						  break;
		case ParameterInfo.LIST_DEF	: sb.append( "list\" value_type=\"" );
						  sb.append( getSimphonyTypeString( info.getType() ) );
						  sb.append( "\" values=\"" );
						  for( int i = 0; i < info.getValues().size(); ++i ){
							  if( i > 0 ) sb.append( " " );
							  sb.append( toStringWithoutScientificNotation( info.getValues().get(i), 
										   									info.getType() ) );
							  sb.append( "" );
						  }
						  sb.append( "\"" );
						  break;
		case ParameterInfo.INCR_DEF	: info.transformIfNeed();
						  sb.append( "number\" start=\"" );
						  sb.append( toStringWithoutScientificNotation( info.getStartValue(),
								  										info.getType() ) );
						  sb.append(typeSuffix(info.getJavaType()));
						  sb.append( "\" end=\"" );
						  sb.append( toStringWithoutScientificNotation( info.getEndValue(), 
								  										info.getType() ) );
						  sb.append(typeSuffix(info.getJavaType()));
						  sb.append( "\" step=\"" );
						  sb.append( toStringWithoutScientificNotation( info.getStep(), 
								  										info.getType() ) );
						  sb.append(typeSuffix(info.getJavaType()));
						  sb.append( "\"" );
		}
		return sb.toString();
	}
	
	/** Formats the parameter file content <code>orig</code> and returns the formatted string. */
	@SuppressWarnings("unused")
	private String prettyPrint(String orig) {
		StringBuilder sb = new StringBuilder();
		int tabNo = 0;
		String[] lines = orig.split("\n");
		for (String line : lines) {
			line = line.trim();
			if (line.endsWith("}")) tabNo--;
			for (int i=0;i<tabNo;++i) sb.append("\t");
			if (line.endsWith("{")) tabNo++;
			sb.append(line + "\n");
		}
		return sb.toString();
	}
	
	public static String getSimphonyTypeString( String type ){		
		if( "Integer".equalsIgnoreCase( type ) || "int".equalsIgnoreCase( type ) )
			return "int";
		if( "Double".equalsIgnoreCase( type ) ) return "double";
		if( "Boolean".equalsIgnoreCase( type ) ) return "boolean";
		if( "Long".equalsIgnoreCase( type ) ) return "long";
		if( "Float".equalsIgnoreCase( type ) ) return "float";
		if( "String".equalsIgnoreCase( type ) ) return "string";
		
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static String toStringWithoutScientificNotation(Object num, String type) {
		if (null == num)
			return "null";
		StringBuilder result = new StringBuilder();
		String string = num.toString();
		if ("float".equals(type) || "Float".equals(type) ||
			"double".equals(type) || "Double".equals(type)) {
			String[] split = string.trim().split("E");
			if (split.length == 1) 
				return string;
			else {
				int exp = Integer.parseInt(split[1]);
				if (exp < 0) {
					int _exp = -1 * exp;
					for (int i = 0;i < _exp;++i) {
						result.append("0");
						if (i == 0)
							result.append(".");
					}
					result.append(split[0].replaceAll("\\.",""));
				} else {
					int dotIndex = split[0].indexOf('.');
					int fragment = split[0].substring(dotIndex,split[0].length()).length();
					result.append(split[0].replaceAll("\\.",""));
					if (fragment - exp > 0){
						result.insert(exp + 1, '.');
					} else {
						for (int i = 0; i <= exp - fragment;++i) { 
							result.append("0");
						}
					}
				}
				return result.toString();
			}
		} else
			return string;

	}
	
	//----------------------------------------------------------------------------------------------------
	private static String typeSuffix(Class<?> type) {
		if (Float.class.equals(type) || Float.TYPE.equals(type)) 
			return "f";
		else if (Long.class.equals(type) || Long.TYPE.equals(type))
			return "L";
		return "";
	}
 }
