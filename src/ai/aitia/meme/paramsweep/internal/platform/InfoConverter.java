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
package ai.aitia.meme.paramsweep.internal.platform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ai.aitia.meme.paramsweep.batch.output.BreedNonRecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.NonRecordableFunctionInfo;
import ai.aitia.meme.paramsweep.batch.output.NonRecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo.ValueType;
import ai.aitia.meme.paramsweep.batch.param.IncrementalParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterNode;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;
import ai.aitia.meme.paramsweep.generator.WizardSettingsManager;
import ai.aitia.meme.paramsweep.gui.info.ArgsFunctionMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.ChooserParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.ExtendedOperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.GeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MultiColumnOperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.NLBreedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.NLSimpleGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.OneArgFunctionMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.OperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.RecordableElement;
import ai.aitia.meme.paramsweep.gui.info.RepastSMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.RepastSRecordableElement;
import ai.aitia.meme.paramsweep.gui.info.ResultInfo;
import ai.aitia.meme.paramsweep.gui.info.ScriptGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.SimpleGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.TimeInfo;
import ai.aitia.meme.paramsweep.gui.info.TimeInfo.WriteMode;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings.UnsupportedPlatformException;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.platform.mason.info.MasonChooserParameterInfo;
import ai.aitia.meme.paramsweep.platform.mason.info.MasonIntervalParameterInfo;
import ai.aitia.meme.paramsweep.platform.netlogo.impl.NetLogoChooserParameterInfo;
import ai.aitia.meme.paramsweep.platform.netlogo.info.NLStatisticGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.impl.ModelGenerator;
import ai.aitia.meme.paramsweep.platform.repast.info.ExtendedOperatorGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.GeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.MultiColumnOperatorGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.OperatorGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.ScriptGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.StatisticGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.simphony.impl.info.RepastSRecordableInfo;
import ai.aitia.meme.paramsweep.utils.AssistantMethod;
import ai.aitia.meme.paramsweep.utils.PlatformConstants;
import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;

public class InfoConverter {
	
	protected static final int UNKNOWN_TYPE 	= -1;
	protected static final int BOOLEAN_TYPE 	= 0;
	protected static final int INTEGER_TYPE 	= 1;
	protected static final int DOUBLE_TYPE 		= 2;
	protected static final int FLOAT_TYPE 		= 3;
	protected static final int LONG_TYPE 		= 4;
	protected static final int BYTE_TYPE 		= 5;
	protected static final int SHORT_TYPE 		= 6;
	protected static final int STRING_TYPE 		= 7;
	protected static final int FILE_TYPE		= 8;
	
	private static final String STOP_CODE 		= "STOP=";
	private static final String REC_CODE		= "REC=";
	private static final String END_CONSTANT	= "END";
	private static final String WRITE_CODE		= "WRITE="; 
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public static RecordableElement recordableInfo2RecordableElement(RecordableInfo info) {
		switch (PlatformSettings.getPlatformType()) { // TODO: finish
		case REPAST 	: return _RepastJRecordableInfo2RecordableElement(info);
		case SIMPHONY 	: return _RepastSRecordableInfo2RecordableElement(info);
		case TRASS		:
		case EMIL		: return _EMILRecordableInfo2RecordableElement(info);
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM		: return _CustomJavaRecordableInfo2RecordableElement(info);
		case NETLOGO5	:
		case NETLOGO	: return _NetLogoRecordableInfo2RecordableElement(info);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());

		}
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public static AbstractParameterInfo<?> parameterInfo2ParameterInfo(ParameterInfo info) {
		AbstractParameterInfo<?> convertedInfo = null;
		switch (getTypeNo(info.getType())) {
		case BOOLEAN_TYPE : // Boolean
							convertedInfo =	new ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Boolean>(info.getName(),"",false);
							if (info.getDefinitionType() == ParameterInfo.CONST_DEF) 
								((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Boolean>)convertedInfo).setValue((Boolean)info.getValue());
							else if (info.getDefinitionType() == ParameterInfo.LIST_DEF) {
								ArrayList<Boolean> values = new ArrayList<Boolean>();
								List<Object> toConvert = info.getValues();
								for (int i = 0;i < toConvert.size();++i ) 
									values.add((Boolean)toConvert.get(i));
								((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Boolean>)convertedInfo).setValues(values);
							}
							break;
		case INTEGER_TYPE : // Integer
							convertedInfo =	info.getDefinitionType() == ParameterInfo.INCR_DEF ? 
											new IncrementalParameterInfo<Integer>(info.getName(),"",0) :
											new ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Integer>(info.getName(),"",0);
							if (info.getDefinitionType() == ParameterInfo.CONST_DEF) 
								((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Integer>)convertedInfo).setValue((Integer)info.getValue());
							else if (info.getDefinitionType() == ParameterInfo.LIST_DEF) {
								ArrayList<Integer> values = new ArrayList<Integer>();
								List<Object> toConvert = info.getValues();
								for (int i = 0;i < toConvert.size();++i )
									values.add((Integer)toConvert.get(i));
								((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Integer>)convertedInfo).setValues(values);
							} else if (info.getDefinitionType()	== ParameterInfo.INCR_DEF) 
								((IncrementalParameterInfo<Integer>)convertedInfo).setValues((Integer)info.getStartValue(),
																							 (Integer)info.getEndValue(),(Integer)info.getStep());
							break;
		case DOUBLE_TYPE : // Double
						   convertedInfo = info.getDefinitionType() == ParameterInfo.INCR_DEF ? 
								   		   new IncrementalParameterInfo<Double>(info.getName(),"",0.0) :
								   		   new ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Double>(info.getName(),"",0.0);
						   if (info.getDefinitionType() == ParameterInfo.CONST_DEF) 
							   ((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Double>)convertedInfo).setValue((Double)info.getValue());
						   else if (info.getDefinitionType() == ParameterInfo.LIST_DEF) {
							   ArrayList<Double> values = new ArrayList<Double>();
							   List<Object> toConvert = info.getValues();
							   for (int i = 0;i < toConvert.size();++i) 
								   values.add((Double)toConvert.get(i));
							   ((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Double>)convertedInfo).setValues(values);
						   } else if (info.getDefinitionType() == ParameterInfo.INCR_DEF) 
							   ((IncrementalParameterInfo<Double>)convertedInfo).setValues((Double)info.getStartValue(),(Double)info.getEndValue(),
													  									   (Double)info.getStep());
						   break;
		case FLOAT_TYPE : // Float
						  convertedInfo = info.getDefinitionType() == ParameterInfo.INCR_DEF ?
								  		  new IncrementalParameterInfo<Float>(info.getName(),"",0f) :
								  		  new ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Float>(info.getName(),"",0f);
						  if (info.getDefinitionType() == ParameterInfo.CONST_DEF) 
							  ((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Float>)convertedInfo).setValue((Float)info.getValue());
						  else if (info.getDefinitionType() == ParameterInfo.LIST_DEF) {
							  ArrayList<Float> values = new ArrayList<Float>();
							  List<Object> toConvert = info.getValues();
							  for (int i = 0;i < toConvert.size();++i) 
								  values.add((Float)toConvert.get(i));
							  ((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Float>)convertedInfo).setValues(values);
						  } else if (info.getDefinitionType() == ParameterInfo.INCR_DEF) 
							  ((IncrementalParameterInfo<Float>)convertedInfo).setValues((Float)info.getStartValue(),(Float)info.getEndValue(),
													  									 (Float)info.getStep());
						  break;
		case LONG_TYPE : // Long
						 convertedInfo = info.getDefinitionType() == ParameterInfo.INCR_DEF ?
								 		 new IncrementalParameterInfo<Long>(info.getName(),"",0l) :
								 		 new ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Long>(info.getName(),"",0l);
						 if (info.getDefinitionType() == ParameterInfo.CONST_DEF)
							 ((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Long>)convertedInfo).setValue((Long)info.getValue());
						 else if (info.getDefinitionType() == ParameterInfo.LIST_DEF ){
							 ArrayList<Long> values = new ArrayList<Long>();
							 List<Object> toConvert = info.getValues();
							 for (int i = 0;i < toConvert.size();++i)
								 values.add((Long)toConvert.get(i));
							 ((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Long>)convertedInfo).setValues(values);
						 } else if (info.getDefinitionType() == ParameterInfo.INCR_DEF) 
							 ((IncrementalParameterInfo<Long>)convertedInfo).setValues((Long)info.getStartValue(),(Long)info.getEndValue(),
													  								   (Long)info.getStep());
						 break;
		case BYTE_TYPE : // Byte
						 convertedInfo = info.getDefinitionType() == ParameterInfo.INCR_DEF ?
								 		 new IncrementalParameterInfo<Byte>(info.getName(),"",new Byte("0")) :
								 		 new ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Byte>(info.getName(),"",new Byte("0"));
						 if (info.getDefinitionType() == ParameterInfo.CONST_DEF) 
							 ((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Byte>)convertedInfo).setValue((Byte)info.getValue());
						 else if (info.getDefinitionType() == ParameterInfo.LIST_DEF) {
							 ArrayList<Byte> values = new ArrayList<Byte>();
							 List<Object> toConvert = info.getValues();
							 for (int i = 0;i < toConvert.size();++i) 
								 values.add((Byte)toConvert.get(i));
							 ((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Byte>)convertedInfo).setValues(values);
						 } else if (info.getDefinitionType() == ParameterInfo.INCR_DEF) 
							 ((IncrementalParameterInfo<Byte>)convertedInfo).setValues((Byte)info.getStartValue(),(Byte)info.getEndValue(),
													  								   (Byte)info.getStep());
						 break;
		case SHORT_TYPE : // Short
						 convertedInfo = info.getDefinitionType() == ParameterInfo.INCR_DEF ?
								 	   new IncrementalParameterInfo<Short>(info.getName(),"",(short)0) :
								 	   new ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Short>(info.getName(),"",(short)0);
						 if (info.getDefinitionType() == ParameterInfo.CONST_DEF) 
							 ((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Short>)convertedInfo).setValue((Short)info.getValue());
						 else if (info.getDefinitionType() == ParameterInfo.LIST_DEF) {
							 ArrayList<Short> values = new ArrayList<Short>();
							 List<Object> toConvert = info.getValues();
							 for (int i = 0;i < toConvert.size();++i)
								 values.add((Short)toConvert.get(i));
							 ((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Short>)convertedInfo).setValues(values);
						 } else if (info.getDefinitionType() == ParameterInfo.INCR_DEF) 
							 ((IncrementalParameterInfo<Short>)convertedInfo).setValues((Short)info.getStartValue(),(Short)info.getEndValue(),
													  									(Short)info.getStep());
						 break;
		case STRING_TYPE : // String
						   convertedInfo =	new ai.aitia.meme.paramsweep.batch.param.ParameterInfo<String>(info.getName(),"","");
						   if (info.getDefinitionType() == ParameterInfo.CONST_DEF)
							   ((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<String>)convertedInfo).setValue((String)info.getValue());
						   else if (info.getDefinitionType() == ParameterInfo.LIST_DEF) {
							   ArrayList<String> values = new ArrayList<String>();
							   List<Object> toConvert = info.getValues();
							   for (int i = 0;i < toConvert.size();++i)
								   values.add((String)toConvert.get(i));
							   ((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<String>)convertedInfo).setValues(values);
						   }
						   break;
		case FILE_TYPE: // File
			   convertedInfo =	new ai.aitia.meme.paramsweep.batch.param.ParameterInfo<File>(info.getName(),"", new File(""));
			   if (info.getDefinitionType() == ParameterInfo.CONST_DEF)
				   ((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<File>)convertedInfo).setValue((File)info.getValue());
			   else if (info.getDefinitionType() == ParameterInfo.LIST_DEF) {
				   ArrayList<File> values = new ArrayList<File>();
				   List<Object> toConvert = info.getValues();
				   for (int i = 0;i < toConvert.size();++i)
					   values.add((File)toConvert.get(i));
				   ((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<File>)convertedInfo).setValues(values);
			   }
			   break;
		default: 
			if (Enum.class.isAssignableFrom(info.getJavaType())){ // Enum type
				 convertedInfo = new ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Enum<?>>(info.getName(),"",(Enum<?>)info.getValue());
				 if (info.getDefinitionType() == ParameterInfo.LIST_DEF) {
					 ArrayList<Enum<?>> values = new ArrayList<Enum<?>>();
					 List<Object> toConvert = info.getValues();
					 for (int i = 0;i < toConvert.size();++i)
						 values.add((Enum<?>)toConvert.get(i));
					 ((ai.aitia.meme.paramsweep.batch.param.ParameterInfo<Enum<?>>)convertedInfo).setValues(values);
				 }
			} else {
				convertedInfo = info.getDefinitionType() == ParameterInfo.INCR_DEF ? new IncrementalParameterInfo(info.getName(), "", 0)
				: new ai.aitia.meme.paramsweep.batch.param.ParameterInfo(info.getName(), "", 0);
			}
		}
		convertedInfo.setRunNumber(info.getRuns());
		return convertedInfo;
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public static ParameterInfo parameterInfo2ParameterInfo(final ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo info, 
															final PlatformType platformType) {
		switch (platformType) {
		case REPAST		:
		case SIMPHONY	:
		case TRASS		:
		case EMIL		:
		case SIMPHONY2	:
		case CUSTOM		: return defaultParameterInfo2ParameterInfo(info);
		case MASON		: return _MasonParameterInfo2ParameterInfo(info);
		case NETLOGO5	:
		case NETLOGO	: return _NetLogoParameterInfo2ParameterInfo(info);
		default			: throw new UnsupportedPlatformException(platformType.toString());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public static ParameterInfo parameterInfo2ParameterInfo(final ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo info) {
		return parameterInfo2ParameterInfo(info,PlatformSettings.getPlatformType());
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void saveRecorderElementsToXml(Element recorderElement, DefaultMutableTreeNode recorderTreeNode) { //TODO: finish
		switch (PlatformSettings.getPlatformType()) { // TODO: finish
		case REPAST		: _RepastJSaveRecorderElementsToXml(recorderElement,recorderTreeNode);
					  	  break;
		case SIMPHONY 	: _RepastSSaveRecorderElementsToXml(recorderElement,recorderTreeNode);
		  				  break;
		case TRASS		:
		case EMIL		: _EMILSaveRecorderElementsToXml(recorderElement,recorderTreeNode);
					  	  break;
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM 	: _CustomJavaSaveRecorderElementsToXml(recorderElement,recorderTreeNode);
					  	  break;
		case NETLOGO5	:
		case NETLOGO	: _NetLogoSaveRecorderElementsToXml(recorderElement,recorderTreeNode);
						  break;
		default 		: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void loadRecorderElementsFromXml(ClassLoader loader, DefaultTreeModel treeModel, DefaultMutableTreeNode recorderTreeNode,
												   NodeList elements, IScriptSupport scriptSupport) throws WizardLoadingException { //TODO: finish
		switch (PlatformSettings.getPlatformType()) { // TODO: finish
		case REPAST 	: _RepastJLoadRecorderElementsFromXml(loader,treeModel,recorderTreeNode,elements,scriptSupport);
					  	  break;
		case SIMPHONY 	: _RepastSLoadRecorderElementsFromXml(loader,treeModel,recorderTreeNode,elements,scriptSupport);
		  				  break;
		case TRASS		:
		case EMIL		: _EMILLoadRecorderElementsFromXml(loader,treeModel,recorderTreeNode,elements);
					  	  break;
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM		: _CustomJavaLoadRecorderElementsFromXml(loader,treeModel,recorderTreeNode,elements,scriptSupport);
					  	  break;
		case NETLOGO5	:
		case NETLOGO	: _NetLogoLoadRecorderElementsFromXml(loader,treeModel,recorderTreeNode,elements,scriptSupport); 
						  break;
		default 		: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static RecordableInfo convertRecordableElement2RecordableInfo(RecordableElement element) { 
		switch (PlatformSettings.getPlatformType()) { // TODO: finish
		case REPAST 	: return _RepastJConvertRecordableElement2RecordableInfo(element);
		case SIMPHONY 	: return _RepastSRecordableElement2RecordableInfo((RepastSRecordableElement)element);
		case TRASS		:
		case EMIL		: return _EMILConvertRecordableElement2RecordableInfo(element);
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM		: return _CustomJavaConvertRecordableElement2RecordableInfo(element);
		case NETLOGO5	:
		case NETLOGO	: return _NetLogoConvertRecordableElement2RecordableInfo(element);
		default     	: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static boolean isVariable(RecordableInfo info) {
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		:
		case TRASS		:
		case EMIL		:
		case SIMPHONY2	:
		case CUSTOM		: 
		case MASON		:
		case NETLOGO5	:
		case NETLOGO	: return info.getName().equals(info.getAccessibleName());
		case SIMPHONY	: return _RepastSIsVariable(info);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static List<RecorderInfo> convertRecorderTree2List(DefaultMutableTreeNode root) {
		if (root.getChildCount() == 0) 
			return new ArrayList<RecorderInfo>(0);
		
		ArrayList<RecorderInfo> recorderList = new ArrayList<RecorderInfo>();
		for (int i = 0;i < root.getChildCount();++i) {
			DefaultMutableTreeNode recorder = (DefaultMutableTreeNode) root.getChildAt(i);
			RecorderInfo recorderInfo = new RecorderInfo();
			recorderInfo.setDelimiter("|");
			recorderInfo.setName(recorder.getUserObject().toString());
			
			ResultInfo recFileInfo = (ResultInfo) recorder.getFirstLeaf().getUserObject();
			recorderInfo.setOutputFile(new File(recFileInfo.getFile()));
			
			TimeInfo timeInfo = (TimeInfo) ((DefaultMutableTreeNode)recorder.getChildAt(1)).getUserObject();
			setTimeInfo(recorderInfo,timeInfo);
			
			ArrayList<RecordableInfo> recordables = new ArrayList<RecordableInfo>();
			for (int j = 2;j < recorder.getChildCount();++j) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) recorder.getChildAt(j);
				RecordableElement re = (RecordableElement) node.getUserObject(); 
				recordables.add(convertRecordableElement2RecordableInfo(re));
			}
			recorderInfo.setRecordables(recordables);
			recorderList.add(recorderInfo);
		}
		return recorderList;
	}
	
	//----------------------------------------------------------------------------------------------------
	public static ParameterTree node2ParameterTree(DefaultMutableTreeNode root) {
		ParameterTree tree = new ParameterTree();
		ParameterNode treeRoot = new ParameterNode(null);
		
		for (int i = 0;i < root.getChildCount();++i) 
			treeCopy(root.getChildAt(i),treeRoot);

		int childCount = treeRoot.getChildCount();
		for (int i = 0;i < childCount;++i) 
			tree.addNode((ParameterNode)treeRoot.getChildAt(0));
		return tree;
	}
	
	//----------------------------------------------------------------------------------------------------
	public static DefaultMutableTreeNode parameterTree2Node(final ParameterTree tree, final PlatformType platformType) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		for (int i = 0;i < tree.getRoot().getChildCount();inverseTreeCopy(tree.getRoot().getChildAt(i),root,platformType),i++);
		return root;
	}
	
	//----------------------------------------------------------------------------------------------------
	public static DefaultMutableTreeNode parameterTree2Node(final ParameterTree tree) {
		return parameterTree2Node(tree,PlatformSettings.getPlatformType());
	}
	
	//----------------------------------------------------------------------------------------------------
	public static String getPlatformSpecificStoppingCondition(String condition, boolean logical) { //TODO: finish
		switch (PlatformSettings.getPlatformType()) {
		case REPAST 	:
		case SIMPHONY	:
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM		: return condition;
		case TRASS		:
		case EMIL		: return _EMILSGetPlatformSpecificStoppingCondition(condition,logical);
		case NETLOGO5	:
		case NETLOGO	: return _NetLogoGetPlatformSpecificStoppingCondition(condition,logical);
		default			: throw new UnsupportedPlatformException(PlatformSettings.getPlatformType().toString());
		}
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	protected static int getTypeNo(String type) {
		if (type.equals("boolean") || type.equals("Boolean"))
			return BOOLEAN_TYPE;
		if (type.equals("byte") || type.equals("Byte"))
			return BYTE_TYPE;
		if (type.equals("short") || type.equals("Short"))
			return SHORT_TYPE;
		if (type.equals("int") || type.equals("Integer"))
			return INTEGER_TYPE;
		if (type.equals("long") || type.equals("Long"))
			return LONG_TYPE;
		if (type.equals("float") || type.equals("Float"))
			return FLOAT_TYPE;
		if (type.equals("double") || type.equals("Double"))
			return DOUBLE_TYPE;
		if (type.equals("String"))
			return STRING_TYPE;
		if ("File".equals(type)){
			return FILE_TYPE;
		}
		return UNKNOWN_TYPE;
	}
	
	//----------------------------------------------------------------------------------------------------
	protected static void treeCopy(TreeNode node, ParameterNode parent) {
		//copy root
		AbstractParameterInfo<?> info = parameterInfo2ParameterInfo((ParameterInfo)((DefaultMutableTreeNode)node).getUserObject());
		ParameterNode newNode = new ParameterNode(info);
		parent.add(newNode);
		for (int i = 0;i < node.getChildCount();++i)
			treeCopy(node.getChildAt(i),newNode);
	}
	
	//----------------------------------------------------------------------------------------------------
	protected static void inverseTreeCopy(final TreeNode node, final DefaultMutableTreeNode parent, final PlatformType platformType) {
		ParameterInfo info = parameterInfo2ParameterInfo(((ParameterNode)node).getParameterInfo(),platformType);
		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(info);
		parent.add(newNode);
		for (int i = 0;i < node.getChildCount();inverseTreeCopy(node.getChildAt(i),newNode,platformType),i++);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void setTimeInfo(RecorderInfo recorder, TimeInfo time) {
		switch (PlatformSettings.getPlatformType()) { // TODO: finish
		case REPAST 	: _RepastJSetTimeInfo(recorder,time);
					 	  break;
		case SIMPHONY 	:  _RepastSSetTimeInfo(recorder,time);
							break;
		case TRASS		:
		case EMIL		: _EMILSetTimeInfo(recorder,time);
					  	  break;
		case MASON		:
		case SIMPHONY2	:
		case CUSTOM		: _CustomJavaSetTimeInfo(recorder,time);
					  	  break;
		case NETLOGO5	:
		case NETLOGO	: _NetLogoSetTimeInfo(recorder,time);
						  break;
		default : throw new UnsupportedPlatformException();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public static ParameterInfo defaultParameterInfo2ParameterInfo(ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo info) {
		ParameterInfo convertedInfo = new ParameterInfo(info.getName(), info.getDescription(),Utilities.toTypeString1(info.getDefaultValue().getClass()),
														info.getDefaultValue().getClass());
		copySettingsFromAbstractParameterInfo2ParameterInfo(convertedInfo, info);
		return convertedInfo;
	}

	//----------------------------------------------------------------------------------------------------
	public static void copySettingsFromAbstractParameterInfo2ParameterInfo(ParameterInfo to, ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo from) {
		to.setRuns(from.getRunNumber());
		if (from.getValueType() == ValueType.INCREMENT) {
			to.setDefinitionType(ParameterInfo.INCR_DEF);
			to.setStartValue(((IncrementalParameterInfo)from).getStart());
			to.setEndValue(((IncrementalParameterInfo)from).getEnd());
			to.setStep(((IncrementalParameterInfo)from).getIncrement());
		} else if (from.getValueType() == ValueType.CONSTANT) {
			to.setDefinitionType(from.isOriginalConstant() ? ParameterInfo.CONST_DEF : ParameterInfo.LIST_DEF);
			to.setValue(((ai.aitia.meme.paramsweep.batch.param.ParameterInfo)from).getValues().get(0));
		} else {
			to.setDefinitionType(ai.aitia.meme.paramsweep.gui.info.ParameterInfo.LIST_DEF);
			to.setValues(((ai.aitia.meme.paramsweep.batch.param.ParameterInfo)from).getValues());
		}
	}
	
	//====================================================================================================
	// RepastJ section
	
	//----------------------------------------------------------------------------------------------------
	private static RecordableInfo _RepastJConvertRecordableElement2RecordableInfo(RecordableElement element) {
		String name = element.getAlias();
		if (name == null)
			name = element.getInfo().getName();
		MemberInfo info = element.getInfo();
		RecordableInfo result = _RepastJConvertMemberInfo2RecordableInfo(info);
		result.setName(name);
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static RecordableInfo _RepastJConvertMemberInfo2RecordableInfo(MemberInfo info) {
		if (info instanceof SimpleGeneratedMemberInfo) {
			SimpleGeneratedMemberInfo _info = (SimpleGeneratedMemberInfo) info;
			StatisticGeneratedRecordableInfo result = new StatisticGeneratedRecordableInfo(_info.getName(),_info.getJavaType(),_info.getName(),
																						   _info.getSource());
			List<GeneratedMemberInfo> references = _info.getReferences();
			for (GeneratedMemberInfo gmi : references) 
				result.addReference((GeneratedRecordableInfo)_RepastJConvertMemberInfo2RecordableInfo(gmi));
			return result;
		} else if (info instanceof OperatorGeneratedMemberInfo) {
			OperatorGeneratedMemberInfo _info = (OperatorGeneratedMemberInfo) info;
			final boolean hasAssistantMethods = _info instanceof ExtendedOperatorGeneratedMemberInfo;
			OperatorGeneratedRecordableInfo result = null;
			if (hasAssistantMethods){
				result = new ExtendedOperatorGeneratedRecordableInfo(_info.getName(), _info.getJavaType(), _info.getName(), _info.getSource());
			} else if (info instanceof MultiColumnOperatorGeneratedMemberInfo){
				MultiColumnOperatorGeneratedMemberInfo multiInfo = (MultiColumnOperatorGeneratedMemberInfo) info;
				
				result = new MultiColumnOperatorGeneratedRecordableInfo(multiInfo.getName(), multiInfo.getJavaType(), multiInfo.getName(), multiInfo.getSource(), multiInfo.getNumberOfColumns());
			} else {
				result = new OperatorGeneratedRecordableInfo(_info.getName(),_info.getJavaType(), _info.getName(),_info.getSource());
			}
			result.setGeneratorName(_info.getGeneratorName());
			List<GeneratedMemberInfo> references = _info.getReferences();
			for (GeneratedMemberInfo gmi : references)
				result.addReference((GeneratedRecordableInfo)_RepastJConvertMemberInfo2RecordableInfo(gmi));
			if (hasAssistantMethods) {
				final ExtendedOperatorGeneratedMemberInfo eogmi = (ExtendedOperatorGeneratedMemberInfo) _info;
				final ExtendedOperatorGeneratedRecordableInfo _result = (ExtendedOperatorGeneratedRecordableInfo) result;
				for (final AssistantMethod method : eogmi.getAssistantMethods())
					_result.addAssistantMethod(method);
			}
			return result;
		} else if (info instanceof ScriptGeneratedMemberInfo) {
			ScriptGeneratedMemberInfo _info = (ScriptGeneratedMemberInfo) info;
			ScriptGeneratedRecordableInfo result = new ScriptGeneratedRecordableInfo(_info.getName(),_info.getJavaType(),_info.getName(),
																					 _info.getSource(),_info.getImports());
			List<GeneratedMemberInfo> references = _info.getReferences();
			for (GeneratedMemberInfo gmi : references)
				result.addReference((GeneratedRecordableInfo)_RepastJConvertMemberInfo2RecordableInfo(gmi));
			final List<UserDefinedVariable> userVariables = _info.getUserVariables();
			for (final UserDefinedVariable variable : userVariables)
				result.addUserVariable(variable);
			return result;
		} else 
			return new RecordableInfo(info.getName(),info.getJavaType(),info.getName());
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void _RepastJSetTimeInfo(RecorderInfo recorder, TimeInfo time) {
		String recorderType = null;
		switch (time.getType()) {
		case RUN 				: recorderType = ModelGenerator.RUN;
				   				  break;
		case ITERATION 			: recorderType = ModelGenerator.ITERATION;
						 		  break;
		case ITERATION_INTERVAL : recorderType = ModelGenerator.ITERATION_INTERVAL + ":" + time.getArg();
								  break;
		case CONDITION		    : recorderType = ModelGenerator.CONDITION + ":" + time.getArg();
 		}
		recorder.setRecordType(recorderType);
		String writeType = null;
		switch (time.getWriteType()) {
		case RUN 				: writeType = ModelGenerator.RUN;
				   				  break;
		case RECORD				: writeType = ModelGenerator.RECORD;
								  break;
		case ITERATION_INTERVAL : writeType = ModelGenerator.ITERATION_INTERVAL + ":" + time.getWriteArg();
 		}
		recorder.setWriteType(writeType);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static RecordableElement _RepastJRecordableInfo2RecordableElement(RecordableInfo info) {
		MemberInfo elementInfo = null;
		if (info instanceof NonRecordableFunctionInfo) {
			NonRecordableFunctionInfo _info = (NonRecordableFunctionInfo) info;
			if (!_info.getType().equals(Void.TYPE) && _info.getParameterTypes().size() == 1)
				elementInfo = new OneArgFunctionMemberInfo(info.getAccessibleName(),Utilities.toTypeString1(info.getType()),info.getType(),
													   	   _info.getParameterTypes().get(0));
			else if (_info.getType().equals(Void.TYPE) || _info.getParameterTypes().size() > 0)
				elementInfo = new ArgsFunctionMemberInfo(info.getAccessibleName(),Utilities.toTypeString1(info.getType()),info.getType());
			else 
				elementInfo = new MemberInfo(info.getAccessibleName(),Utilities.toTypeString1(info.getType()),info.getType());
			elementInfo.setInnerType(_info.getInnerType());
		} else if (info instanceof StatisticGeneratedRecordableInfo){ 
			elementInfo = new SimpleGeneratedMemberInfo(info.getAccessibleName(),Utilities.toTypeString1(info.getType()),info.getType());
			SimpleGeneratedMemberInfo newInfo = (SimpleGeneratedMemberInfo) elementInfo;
			newInfo.setSource(((StatisticGeneratedRecordableInfo) info).getSource());
			StringBuilder call = new StringBuilder(info.getName() + "(");
			call.append(info.getAccessibleName() + ")");
			newInfo.setCall(call.toString());
		} else {
			elementInfo = new MemberInfo(info.getAccessibleName(),Utilities.toTypeString1(info.getType()),info.getType());
			if (info instanceof NonRecordableInfo) 
				elementInfo.setInnerType(((NonRecordableInfo)info).getInnerType());
		}
		RecordableElement element = new RecordableElement(elementInfo);
		if (!info.getAccessibleName().equals(info.getName()))
			element.setAlias(info.getName());
		return element;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void _RepastJSaveRecorderElementsToXml(Element recorderElement, DefaultMutableTreeNode recorderTreeNode) {
		Document document = recorderElement.getOwnerDocument();
		for (int j = 2;j < recorderTreeNode.getChildCount();++j) {
			DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) recorderTreeNode.getChildAt(j);
			RecordableElement re = (RecordableElement) childNode.getUserObject();
			MemberInfo mi = re.getInfo();
			Element element = document.createElement(WizardSettingsManager.MEMBER);
			element.setAttribute(WizardSettingsManager.TYPE,mi.getType());
			element.setAttribute(WizardSettingsManager.JAVA_TYPE,mi.getJavaType().getName());
			if (re.getAlias() != null)
				element.setAttribute(WizardSettingsManager.ALIAS,re.getAlias());
			element.appendChild(document.createTextNode(mi.getName()));
			recorderElement.appendChild(element);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void _RepastJLoadRecorderElementsFromXml(ClassLoader loader, DefaultTreeModel treeModel, DefaultMutableTreeNode recorderTreeNode,
															NodeList elements, IScriptSupport scriptSupport) throws WizardLoadingException {
		for (int j = 0;j < elements.getLength();++j) {
			Element memberElement = (Element) elements.item(j);
			String memberType = memberElement.getAttribute(WizardSettingsManager.TYPE);
			if (memberType == null || memberType.equals(""))
				throw new WizardLoadingException(true,"missing 'type' attribute at node: " + WizardSettingsManager.MEMBER);
			String javaTypeStr = memberElement.getAttribute(WizardSettingsManager.JAVA_TYPE);
			if (javaTypeStr == null || "".equals(javaTypeStr))
				throw new WizardLoadingException(true,"missing 'java_type' attribute at node: " + WizardSettingsManager.MEMBER);
			NodeList content = memberElement.getChildNodes();
			if (content == null || content.getLength() == 0)
				throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.MEMBER);
			String memberName = ((Text)content.item(0)).getNodeValue();
			Class<?> javaType = null;
			try {
				javaType = Utilities.toClass(loader,javaTypeStr);
			} catch (ClassNotFoundException e) {
				throw new WizardLoadingException(true,"invalid type at recorder member: " + memberName);
			}
			MemberInfo mi = null;
			if (scriptSupport != null)
				mi = scriptSupport.getDefinedScript(memberName);
			if (mi == null)
				mi = new MemberInfo(memberName,memberType,javaType);
			if (!mi.isNumeric() && !mi.isBoolean() && !mi.getType().equals("String") && !(mi instanceof MultiColumnOperatorGeneratedMemberInfo))
				throw new WizardLoadingException(true,"invalid 'type' attribute at node: " + WizardSettingsManager.MEMBER);
			String aliasName = memberElement.getAttribute(WizardSettingsManager.ALIAS);
			if (aliasName == null || "".equals(aliasName))
				aliasName = null;
			DefaultMutableTreeNode member = new DefaultMutableTreeNode(new RecordableElement(mi,aliasName));
			treeModel.insertNodeInto(member,recorderTreeNode,2 + j);
		}
	}
	
	//====================================================================================================
	// End of RepastJ section
	
	//====================================================================================================
	// Custom Java section
	
	//----------------------------------------------------------------------------------------------------
	private static RecordableInfo _CustomJavaConvertRecordableElement2RecordableInfo(RecordableElement element) {
		return _RepastJConvertRecordableElement2RecordableInfo(element);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void _CustomJavaSetTimeInfo(RecorderInfo recorder, TimeInfo time) { _RepastJSetTimeInfo(recorder,time); }
	
	//----------------------------------------------------------------------------------------------------
	private static RecordableElement _CustomJavaRecordableInfo2RecordableElement(RecordableInfo info) {
		return _RepastJRecordableInfo2RecordableElement(info);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void _CustomJavaSaveRecorderElementsToXml(Element recorderElement, DefaultMutableTreeNode recorderTreeNode) {
		_RepastJSaveRecorderElementsToXml(recorderElement,recorderTreeNode);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void _CustomJavaLoadRecorderElementsFromXml(ClassLoader loader, DefaultTreeModel treeModel,
															   DefaultMutableTreeNode recorderTreeNode, NodeList elements, 
															   IScriptSupport scriptSupport) throws WizardLoadingException {
		_RepastJLoadRecorderElementsFromXml(loader,treeModel,recorderTreeNode,elements,scriptSupport);
	}
	
	//====================================================================================================
	// End of Custom Java section
	
	//=====================================================================================
	// Simphony section
	
	//----------------------------------------------------------------------------------------------------
	private static void _RepastSSetTimeInfo(RecorderInfo recorder, TimeInfo time) {
		String recorderType = null;
		switch (time.getType()) {
		case RUN 				: recorderType = PlatformConstants.RUN;
				   				  break;
		case ITERATION 			: recorderType = PlatformConstants.ITERATION;
						 		  break;
		case ITERATION_INTERVAL : recorderType = PlatformConstants.ITERATION_INTERVAL + ":" + time.getArg();
								  break;
 		}
		recorder.setRecordType(recorderType);
		recorder.setWriteType(PlatformConstants.RUN);
	}
	
	//----------------------------------------------------------------------------------------------------
	// RepastSRecordableInfo -> RepastSRecordableElement
	private static RecordableElement _RepastSRecordableInfo2RecordableElement(RecordableInfo info) {
		String accesibleName = info.getAccessibleName();
		int lastPointIdx = accesibleName.lastIndexOf('.');
		String agentClass = accesibleName.substring(0,lastPointIdx);
		agentClass = agentClass.substring(agentClass.lastIndexOf('.') + 1);
		String name = accesibleName.substring(lastPointIdx + 1);
		RepastSMemberInfo elementInfo = new RepastSMemberInfo(name,Utilities.toTypeString1(info.getType()),info.getType(),agentClass,
															  info.getAccessibleName());
		RepastSRecordableElement element = new RepastSRecordableElement(elementInfo);
		if (info instanceof RepastSRecordableInfo)
			element.setAggrType(((RepastSRecordableInfo)info).getAggrType());
		if (!info.getAccessibleName().equals(info.getName())) {
			elementInfo.setFieldName(info.getName());
			element.setAlias(info.getName());
		}
		return element;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static RecordableInfo _RepastSRecordableElement2RecordableInfo(RepastSRecordableElement element) {
		String name = element.getAlias();
		if (name == null)
			name = element.getInfo().getName();
		RepastSMemberInfo info = element.getInfo();
		RepastSRecordableInfo result = _convertRepastSMemberInfo2RecordableInfo(info);
		result.setAggrType(element.getAggrType());
		result.setName(name);
		result.setFieldName(info.getFieldName());
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static RepastSRecordableInfo _convertRepastSMemberInfo2RecordableInfo(RepastSMemberInfo info) {
		String agentClass = info.getAccessMethod().substring(0,info.getAccessMethod().lastIndexOf('.'));
		RepastSRecordableInfo result = new RepastSRecordableInfo(info.getName(),info.getJavaType(),info.getAccessMethod(),agentClass,
																 info.getAccessMethod());
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("cast")
	private static void _RepastSSaveRecorderElementsToXml(Element recorderElement, DefaultMutableTreeNode recorderTreeNode) {
		Document document = recorderElement.getOwnerDocument();
		for (int j = 2;j < recorderTreeNode.getChildCount();++j) {
			DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) recorderTreeNode.getChildAt(j);
			RepastSRecordableElement re = (RepastSRecordableElement) childNode.getUserObject();
			RepastSMemberInfo mi = (RepastSMemberInfo) re.getInfo();
			Element element = document.createElement(WizardSettingsManager.MEMBER);
			element.setAttribute(WizardSettingsManager.TYPE,mi.getType());
			element.setAttribute(WizardSettingsManager.JAVA_TYPE,mi.getJavaType().getName());
			element.setAttribute( RepastSRecordableElement.AGGREGATE_TYPE,re.getAggrType().toString());
			element.setAttribute( RepastSRecordableElement.AGENT_CLASS,mi.getAgentClass());
			element.setAttribute( RepastSRecordableElement.ACCESS_METHOD,mi.getAccessMethod());
			if (re.getAlias() != null)
				element.setAttribute(WizardSettingsManager.ALIAS,re.getAlias());
			element.appendChild(document.createTextNode(mi.getName()));
			recorderElement.appendChild(element);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void _RepastSLoadRecorderElementsFromXml(ClassLoader loader, DefaultTreeModel treeModel, DefaultMutableTreeNode recorderTreeNode,
														   NodeList elements, IScriptSupport scriptSupport) throws WizardLoadingException {
		for (int j = 0;j < elements.getLength();++j) {
			Element memberElement = (Element) elements.item(j);
			
			String memberType = memberElement.getAttribute(WizardSettingsManager.TYPE);
			if (memberType == null || memberType.equals(""))
				throw new WizardLoadingException(true,"missing 'type' attribute at node: " + WizardSettingsManager.MEMBER);
			
			String javaTypeStr = memberElement.getAttribute(WizardSettingsManager.JAVA_TYPE);
			if (javaTypeStr == null || "".equals(javaTypeStr))
				throw new WizardLoadingException(true,"missing 'java_type' attribute at node: " + WizardSettingsManager.MEMBER);
			
			String aggregateTypeStr = memberElement.getAttribute(RepastSRecordableElement.AGGREGATE_TYPE);
			if (aggregateTypeStr == null || "".equals(aggregateTypeStr))
				throw new WizardLoadingException(true,"missing 'aggregate_type' attribute at node: " + WizardSettingsManager.MEMBER);
			
			String agentClassStr = memberElement.getAttribute(RepastSRecordableElement.AGENT_CLASS);
			if (agentClassStr == null || "".equals(agentClassStr))
				throw new WizardLoadingException(true,"missing 'agent_class' attribute at node: " + WizardSettingsManager.MEMBER);
			
			String accessMethodStr = memberElement.getAttribute(RepastSRecordableElement.ACCESS_METHOD);
			if (accessMethodStr == null || "".equals(accessMethodStr))
				throw new WizardLoadingException(true,"missing 'access_method' attribute at node: " + WizardSettingsManager.MEMBER);
						
			NodeList content = memberElement.getChildNodes();
			if (content == null || content.getLength() == 0)
				throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.MEMBER);
			String memberName = ((Text)content.item(0)).getNodeValue();
			Class<?> javaType = null;
			try {
				javaType = Utilities.toClass(loader,javaTypeStr);
			} catch (ClassNotFoundException e) {
				throw new WizardLoadingException(true,"invalid type at recorder member: " + memberName);
			}
			RepastSMemberInfo mi = new RepastSMemberInfo(memberName,memberType,javaType,agentClassStr,accessMethodStr);
			
			if (!mi.isNumeric() && !mi.isBoolean() && !mi.getType().equals("String"))
				throw new WizardLoadingException(true,"invalid 'type' attribute at node: " + WizardSettingsManager.MEMBER);
			
			String aliasName = memberElement.getAttribute(WizardSettingsManager.ALIAS);
			if ("".equals(aliasName))
				aliasName = null;
			
			DefaultMutableTreeNode member =	new DefaultMutableTreeNode(new RepastSRecordableElement(mi,aliasName,RepastSRecordableInfo.getAggregateType(aggregateTypeStr)));
			treeModel.insertNodeInto(member,recorderTreeNode,2 + j);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean _RepastSIsVariable(RecordableInfo info) {
		if (!info.getName().endsWith("()")) {
			int lastPointIdx = info.getAccessibleName().lastIndexOf('.');
			String name = lastPointIdx == -1 ? info.getAccessibleName() : info.getAccessibleName().substring(lastPointIdx + 1);
			String field = Util.capitalize(info.getName());
			return ("get" + field + "()").equals(name) || ("is" + field + "()").equals(name);
		}
		return false;
	}
	
	//====================================================================================================
	// End of Simphony section
	
	//====================================================================================================
	// EMIL-S section
	
	//----------------------------------------------------------------------------------------------------
	private static RecordableElement _EMILRecordableInfo2RecordableElement(RecordableInfo info) {
		return _RepastJRecordableInfo2RecordableElement(info);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void _EMILSaveRecorderElementsToXml(Element recorderElement, DefaultMutableTreeNode recorderTreeNode) {
		_RepastJSaveRecorderElementsToXml(recorderElement,recorderTreeNode);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void _EMILLoadRecorderElementsFromXml(ClassLoader loader, DefaultTreeModel treeModel,
														DefaultMutableTreeNode recorderTreeNode, NodeList elements) throws WizardLoadingException {
		for (int j = 0;j < elements.getLength();++j) {
			Element memberElement = (Element) elements.item(j);
			String memberType = memberElement.getAttribute(WizardSettingsManager.TYPE);
			if (memberType == null || memberType.equals(""))
				throw new WizardLoadingException(true,"missing 'type' attribute at node: " + WizardSettingsManager.MEMBER);
			String javaTypeStr = memberElement.getAttribute(WizardSettingsManager.JAVA_TYPE);
			if (javaTypeStr == null || "".equals(javaTypeStr))
				throw new WizardLoadingException(true,"missing 'java_type' attribute at node: " + WizardSettingsManager.MEMBER);
			NodeList content = memberElement.getChildNodes();
			if (content == null || content.getLength() == 0)
				throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.MEMBER);
			String memberName = ((Text)content.item(0)).getNodeValue();
			Class<?> javaType = null;
			try {
				javaType = Utilities.toClass(MemberInfo.class.getClassLoader(),javaTypeStr);
			} catch (ClassNotFoundException e) {
				throw new WizardLoadingException(true,"invalid type at recorder member: " + memberName);
			}
			MemberInfo mi = new MemberInfo(memberName,memberType,javaType);
			if (!mi.isNumeric() && !mi.isBoolean() && !mi.getType().equals("String"))
				throw new WizardLoadingException(true,"invaild 'type' attribute at node: " + WizardSettingsManager.MEMBER);
			String aliasName = memberElement.getAttribute(WizardSettingsManager.ALIAS);
			if (aliasName == null || "".equals(aliasName))
				aliasName = null;
			DefaultMutableTreeNode member = new DefaultMutableTreeNode(new RecordableElement(mi,aliasName));
			treeModel.insertNodeInto(member,recorderTreeNode,2 + j);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private static RecordableInfo _EMILConvertRecordableElement2RecordableInfo(RecordableElement element) {
		String name = element.getAlias();
		if (name == null)
			name = element.getInfo().getName();
		MemberInfo info = element.getInfo();
		RecordableInfo result = _EMILConvertMemberInfo2RecordableInfo(info);
		result.setName(name);
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static RecordableInfo _EMILConvertMemberInfo2RecordableInfo(MemberInfo info) {
		return new RecordableInfo(info.getName(),info.getJavaType(),info.getName());
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void _EMILSetTimeInfo(RecorderInfo recorder, TimeInfo time) {
		String recorderType = REC_CODE;
		switch (time.getType()) {
		case RUN 				: recorderType += END_CONSTANT;
				   				  break;
		case ITERATION 			: recorderType += 1;
						 		  break;
		case ITERATION_INTERVAL : recorderType += time.getArg();
								  break;
		case CONDITION		    : recorderType += "{" + time.getArg() + "}";
 		}
		recorder.setRecordType(recorderType);
		String writeType = WRITE_CODE;
		switch (time.getWriteType()) {
		case RUN 				: writeType += END_CONSTANT;
				   				  break;
		case RECORD				: writeType += 1;
								  break;
		case ITERATION_INTERVAL : writeType += time.getWriteArg();
 		}
		recorder.setWriteType(writeType);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static String _EMILSGetPlatformSpecificStoppingCondition(String condition, boolean logical) {
		String result = STOP_CODE;
		result += (logical ? "{" : "") + condition + (logical ? "}" : "");
		return result;
	}
	
	//====================================================================================================
	// End of EMIL-S section
	
	//====================================================================================================
	// NetLogo section
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private static ParameterInfo _NetLogoParameterInfo2ParameterInfo(ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo info) {
		if (info instanceof NetLogoChooserParameterInfo) {
			NetLogoChooserParameterInfo cpi = (NetLogoChooserParameterInfo) info;
			ChooserParameterInfo convertedInfo = new ChooserParameterInfo(cpi.getName(),Utilities.toTypeString1(cpi.getDefaultValue().getClass()),
																		  cpi.getDefaultValue().getClass(),cpi.getPossibleValues());
			convertedInfo.setRuns(cpi.getRunNumber());
			// only constant valueType possible with this type
			//FIXME: really? Isn't this a bug? (Gabor)
			convertedInfo.setDefinitionType(ParameterInfo.CONST_DEF);
			convertedInfo.setValue(info.getDefaultValue());
			return convertedInfo;
		}
		return defaultParameterInfo2ParameterInfo(info);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static RecordableInfo _NetLogoConvertRecordableElement2RecordableInfo(RecordableElement element) {
		String name = element.getAlias();
		if (name == null)
			name = element.getInfo().getName();
		MemberInfo info = element.getInfo();
		RecordableInfo result = _NetLogoConvertMemberInfo2RecordableInfo(info);
		result.setName(name);
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static RecordableInfo _NetLogoConvertMemberInfo2RecordableInfo(MemberInfo info) { 
		if (info instanceof NLSimpleGeneratedMemberInfo) {
			NLSimpleGeneratedMemberInfo _info = (NLSimpleGeneratedMemberInfo) info;
			NLStatisticGeneratedRecordableInfo result = new NLStatisticGeneratedRecordableInfo(_info.getName(),_info.getJavaType(),_info.getName(),
																						   	   _info.getSource(),_info.getSyntaxBody(),
																						   	   _info.getReportBody());
			List<GeneratedMemberInfo> references = _info.getReferences();
			for (GeneratedMemberInfo gmi : references) 
				result.addReference((GeneratedRecordableInfo)_NetLogoConvertMemberInfo2RecordableInfo(gmi));
			return result;
		} else if (info instanceof OperatorGeneratedMemberInfo) {
			OperatorGeneratedMemberInfo _info = (OperatorGeneratedMemberInfo) info;
			final boolean hasAssistantMethods = _info instanceof ExtendedOperatorGeneratedMemberInfo;
			OperatorGeneratedRecordableInfo result = hasAssistantMethods ? new ExtendedOperatorGeneratedRecordableInfo(_info.getName(),
																													   _info.getJavaType(),
																													   _info.getName(),
																													   _info.getSource())
																		 : new OperatorGeneratedRecordableInfo(_info.getName(),_info.getJavaType(),
																				 							   _info.getName(),_info.getSource());
			result.setGeneratorName(_info.getGeneratorName());
			List<GeneratedMemberInfo> references = _info.getReferences();
			for (GeneratedMemberInfo gmi : references)
				result.addReference((GeneratedRecordableInfo)_NetLogoConvertMemberInfo2RecordableInfo(gmi));
			if (hasAssistantMethods) {
				final ExtendedOperatorGeneratedMemberInfo eogmi = (ExtendedOperatorGeneratedMemberInfo) _info;
				final ExtendedOperatorGeneratedRecordableInfo _result = (ExtendedOperatorGeneratedRecordableInfo) result;
				for (final AssistantMethod method : eogmi.getAssistantMethods())
					_result.addAssistantMethod(method);
			}
			return result;
		} else if (info instanceof ScriptGeneratedMemberInfo) {
			ScriptGeneratedMemberInfo _info = (ScriptGeneratedMemberInfo) info;
			ScriptGeneratedRecordableInfo result = new ScriptGeneratedRecordableInfo(_info.getName(),_info.getJavaType(),_info.getName(),
																					 _info.getSource(),_info.getImports());
			List<GeneratedMemberInfo> references = _info.getReferences();
			for (GeneratedMemberInfo gmi : references)
				result.addReference((GeneratedRecordableInfo)_NetLogoConvertMemberInfo2RecordableInfo(gmi));
			final List<UserDefinedVariable> userVariables = _info.getUserVariables();
			for (final UserDefinedVariable variable : userVariables)
				result.addUserVariable(variable);
			return result;
		} else 
			return new RecordableInfo(info.getName(),info.getJavaType(),info.getName());
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void _NetLogoSetTimeInfo(RecorderInfo recorder, TimeInfo time) {
		String recorderType = null;
		switch (time.getType()) {
		case RUN 		: recorderType = ModelGenerator.RUN;
				   		  break;
		case ITERATION 	: recorderType = ModelGenerator.ITERATION;
						  break;
		default			: throw new IllegalStateException(); 
 		}
		recorder.setRecordType(recorderType);
		if (time.getWriteType() != WriteMode.RUN)
			throw new IllegalStateException();
		recorder.setWriteType(ModelGenerator.RUN);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static RecordableElement _NetLogoRecordableInfo2RecordableElement(final RecordableInfo info) {
		MemberInfo elementInfo = null;
		if (info instanceof NonRecordableFunctionInfo) {
			final NonRecordableFunctionInfo _info = (NonRecordableFunctionInfo) info;
			if (!_info.getType().equals(Void.TYPE) && _info.getParameterTypes().size() == 1)
				elementInfo = new OneArgFunctionMemberInfo(info.getAccessibleName(),Utilities.toTypeString1(info.getType()),info.getType(),
													   	   _info.getParameterTypes().get(0));
			else if (_info.getType().equals(Void.TYPE) || _info.getParameterTypes().size() > 0)
				elementInfo = new ArgsFunctionMemberInfo(info.getAccessibleName(),Utilities.toTypeString1(info.getType()),info.getType());
			else 
				elementInfo = new MemberInfo(info.getAccessibleName(),Utilities.toTypeString1(info.getType()),info.getType());
			elementInfo.setInnerType(_info.getInnerType());
		} else {
			elementInfo = (info instanceof BreedNonRecordableInfo) 
											? new NLBreedMemberInfo(info.getAccessibleName(),Utilities.toTypeString1(info.getType()),info.getType()) 
											: new MemberInfo(info.getAccessibleName(),Utilities.toTypeString1(info.getType()),info.getType());
			if (info instanceof NonRecordableInfo) 
				elementInfo.setInnerType(((NonRecordableInfo)info).getInnerType());
		}
		final RecordableElement element = new RecordableElement(elementInfo);
		if (!info.getAccessibleName().equals(info.getName()))
			element.setAlias(info.getName());
		return element;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void _NetLogoSaveRecorderElementsToXml(Element recorderElement, DefaultMutableTreeNode recorderTreeNode) {
		_RepastJSaveRecorderElementsToXml(recorderElement,recorderTreeNode);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void _NetLogoLoadRecorderElementsFromXml(ClassLoader loader, DefaultTreeModel treeModel, DefaultMutableTreeNode recorderTreeNode,
															NodeList elements, IScriptSupport scriptSupport) throws WizardLoadingException {
		for (int j = 0;j < elements.getLength();++j) {
			Element memberElement = (Element) elements.item(j);
			String memberType = memberElement.getAttribute(WizardSettingsManager.TYPE);
			if (memberType == null || memberType.equals(""))
				throw new WizardLoadingException(true,"missing 'type' attribute at node: " + WizardSettingsManager.MEMBER);
			String javaTypeStr = memberElement.getAttribute(WizardSettingsManager.JAVA_TYPE);
			if (javaTypeStr == null || "".equals(javaTypeStr))
				throw new WizardLoadingException(true,"missing 'java_type' attribute at node: " + WizardSettingsManager.MEMBER);
			NodeList content = memberElement.getChildNodes();
			if (content == null || content.getLength() == 0)
				throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.MEMBER);
			String memberName = ((Text)content.item(0)).getNodeValue();
			Class<?> javaType = null;
			try {
				javaType = Utilities.toClass(InfoConverter.class.getClassLoader(),javaTypeStr);
			} catch (ClassNotFoundException e) {
				throw new WizardLoadingException(true,"invalid type at recorder member: " + memberName);
			}
			MemberInfo mi = null;
			if (scriptSupport != null)
				mi = scriptSupport.getDefinedScript(memberName);
			if (mi == null)
				mi = new MemberInfo(memberName,memberType,javaType);
			String aliasName = memberElement.getAttribute(WizardSettingsManager.ALIAS);
			if (aliasName == null || "".equals(aliasName))
				aliasName = null;
			DefaultMutableTreeNode member = new DefaultMutableTreeNode(new RecordableElement(mi,aliasName));
			treeModel.insertNodeInto(member,recorderTreeNode,2 + j);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private static String _NetLogoGetPlatformSpecificStoppingCondition(String condition, boolean logical) {
		return (logical ? "{" : "") + condition + (logical ? "}" : "");
	}
	
	//====================================================================================================
	// End of NetLogo section

	//====================================================================================================
	// MASON section
	
	//----------------------------------------------------------------------------------------------------
	private static ParameterInfo _MasonParameterInfo2ParameterInfo(AbstractParameterInfo info) {
		if (info instanceof MasonChooserParameterInfo) {
			MasonChooserParameterInfo castedParameter = (MasonChooserParameterInfo) info;
			ai.aitia.meme.paramsweep.gui.info.MasonChooserParameterInfo convertedInfo = 
				new	ai.aitia.meme.paramsweep.gui.info.MasonChooserParameterInfo(
						info.getName(), 
						info.getDescription(),
						Utilities.toTypeString1(info.getDefaultValue().getClass()), 
						info.getDefaultValue().getClass(), 
						castedParameter.getPossibleValues(), 
						castedParameter.getPossibleNamedValues());
			copySettingsFromAbstractParameterInfo2ParameterInfo(convertedInfo, info);
			return convertedInfo;
		} else if (info instanceof MasonIntervalParameterInfo) {
			MasonIntervalParameterInfo castedParameter = (MasonIntervalParameterInfo) info;
			ai.aitia.meme.paramsweep.gui.info.MasonIntervalParameterInfo convertedInfo = 
				new ai.aitia.meme.paramsweep.gui.info.MasonIntervalParameterInfo(
						info.getName(), 
						info.getDescription(),
						Utilities.toTypeString1(info.getDefaultValue().getClass()),
						info.getDefaultValue().getClass(), 
						castedParameter.getIntervalMin(),
						castedParameter.getIntervalMax(), 
						castedParameter.isIntervalDouble());
			copySettingsFromAbstractParameterInfo2ParameterInfo(convertedInfo, info);
			return convertedInfo;
		} else {
			return defaultParameterInfo2ParameterInfo(info);
		}
	}

	//====================================================================================================
	// End of MASON section
	
	
}
