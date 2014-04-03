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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.batch.IModelInformation.ModelInformationException;
import ai.aitia.meme.paramsweep.batch.output.NonRecordableFunctionInfo;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.generator.IStatisticInfoGenerator;
import ai.aitia.meme.paramsweep.generator.StatisticsInfoGenerator;
import ai.aitia.meme.paramsweep.generator.WizardSettingsManager;
import ai.aitia.meme.paramsweep.gui.Page_Recorders;
import ai.aitia.meme.paramsweep.gui.ScriptCreationDialog;
import ai.aitia.meme.paramsweep.gui.info.ConstantInfo;
import ai.aitia.meme.paramsweep.gui.info.ConstantKeyInfo;
import ai.aitia.meme.paramsweep.gui.info.ExtendedOperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.GeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.InnerOperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MultiColumnOperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.OneArgFunctionMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.OperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.RecordableElement;
import ai.aitia.meme.paramsweep.gui.info.ScriptGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.SimpleGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.VariableScriptGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.plugin.IStatisticsPlugin;
import ai.aitia.meme.paramsweep.plugin.gui.CollectionParameterGUI;
import ai.aitia.meme.paramsweep.utils.AssistantMethod;
import ai.aitia.meme.paramsweep.utils.AssistantMethod.ScheduleTime;
import ai.aitia.meme.paramsweep.utils.CannotLoadDataSourceForEditingException;
import ai.aitia.meme.paramsweep.utils.MemberInfoList;
import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.utils.Utils;
import bemenetek.Fugg;
import bemenetek.Valt;

public class RepastScriptSupport implements IScriptSupport {
	
	//====================================================================================================
	// members
	
	/** Script type constant: statistic instance. */
	private static final String STATISTIC_TYPE = "stat";
	/** Script type constant: script. */
	private static final String SCRIPT_TYPE	   = "script";
	private static final String OPERATOR_TYPE  = "operator";
	
	private ParameterSweepWizard wizard = null;
	private Page_Recorders recordersPage = null;

	/** The list of information objects of all members of the model that can be
	 *  used in a statistic instance or a script. 
	 */
	private MemberInfoList allMembers = new MemberInfoList();
	private List<OneArgFunctionMemberInfo> foreachMembers = new ArrayList<OneArgFunctionMemberInfo>();
	private List<UserDefinedVariable> userVariables = new ArrayList<UserDefinedVariable>();

	/** The list of information objects of all members of the model that can be
	 *  used in a statistic instance or a script. 
	 */
	private List<GeneratedMemberInfo> innerScripts = new ArrayList<GeneratedMemberInfo>();
	
	/** List of information objects of generated members that became invalid because
	 *  they referenced a method that never will be generated.
	 */
	private List<GeneratedMemberInfo> illegalGenerateds = null; 

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public RepastScriptSupport(ParameterSweepWizard wizard, Page_Recorders recordersPage) {
		this.wizard = wizard;
		this.recordersPage = recordersPage;
	}

	//----------------------------------------------------------------------------------------------------
	public MemberInfoList getAllMembers() { return allMembers; }
	public List<OneArgFunctionMemberInfo> getForeachMembers() { return foreachMembers; }
	public List<UserDefinedVariable> getUserVariables() { return userVariables; }
	public List<GeneratedMemberInfo> getInnerScripts() { return innerScripts; }
	public List<GeneratedMemberInfo> getIllegalGenerateds() { return illegalGenerateds; }
	public void illegalGeneratedsToNull() { illegalGenerateds = null; }
	public boolean isValidScriptName(String name) {	return name.matches("[a-zA-Z_]\\w*\\(?\\)?"); }
	public boolean isValidVariableName(final String name) { return name.matches("[a-zA-Z_]\\w*"); }
	public IStatisticInfoGenerator getStatisticInfoGenerator(IStatisticsPlugin statistic) { return new StatisticsInfoGenerator(statistic); }
	public ParameterSweepWizard getWizard(){ return wizard; }
	
	//----------------------------------------------------------------------------------------------------
	public String getDefaultInitialization(final String name, final String defaultValue) {
		return name + " = " + defaultValue + ";\n";
	}
	
	//----------------------------------------------------------------------------------------------------
	public VariableScriptGeneratedMemberInfo createScriptForVariable(final UserDefinedVariable variable) {
		final String source = "return " + variable.getName() + ";\n";
		String name = "get" + Util.capitalize(variable.getName());
		int idx = 0;
		while (getDefinedScript(name + "()") != null)
			name += idx++;
 		final VariableScriptGeneratedMemberInfo result = new VariableScriptGeneratedMemberInfo(name + "()",variable);
 		result.setSource(source);
 		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public VariableScriptGeneratedMemberInfo getScriptForVariable(final UserDefinedVariable variable) {
		for (final MemberInfo info : allMembers) {
			if (info instanceof VariableScriptGeneratedMemberInfo) {
				VariableScriptGeneratedMemberInfo _info = (VariableScriptGeneratedMemberInfo) info;
				if (variable.equals(_info.getParentVariable())) 
					return _info;
			}
		}
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	public DefaultListModel[] initializeScriptSupport(List<ParameterInfo> illegalParameters) {
		List<GeneratedMemberInfo> legalGenerateds = collectLegalsAndIllegals(illegalParameters);
		allMembers.clear();
		foreachMembers.clear();
		collectMathFunctions();
		allMembers.addAll(legalGenerateds);
		if (wizard.hasNewParameters()) {
			List<MemberInfo> generatedGetters = createGeneratedGettersMemberInfo(wizard.getNewParameters_internal());
			allMembers.addAll(generatedGetters);
		}
		
		List<RecordableInfo> members = new ArrayList<RecordableInfo>();
		try {
			members.addAll(wizard.getModelInformation().getRecordables());
			members.addAll(wizard.getModelInformation().getNonRecordables());
		} catch (ModelInformationException e1) {
			// we check this before
			throw new IllegalStateException(e1);
		}
		
		Valt.v.clear();
		Fugg.f.clear();
		
		for (RecordableInfo info : members) {
			if (info instanceof NonRecordableFunctionInfo) {
				NonRecordableFunctionInfo _info = (NonRecordableFunctionInfo) info;
				String name = info.getName() + "(";
				for (Class param : _info.getParameterTypes())
					name += Utilities.toTypeString2(param) + ", ";
				if (_info.getParameterTypes().size() != 0)
					name = name.substring(0,name.length() - 2);
				name += ") : " + Utilities.toTypeString2(_info.getType());
				Fugg.f.setFuggveny(name,"");
				RecordableElement re = InfoConverter.recordableInfo2RecordableElement(_info);
				if (!_info.getType().equals(Void.TYPE) && _info.getParameterTypes().size() == 1)
					foreachMembers.add((OneArgFunctionMemberInfo)re.getInfo());
				else
					allMembers.add(re.getInfo());
			} else {
				RecordableElement re = InfoConverter.recordableInfo2RecordableElement(info);
				allMembers.add(re.getInfo());
				if (info.getName().equals(info.getAccessibleName()))
					Valt.v.setValtozo(info.getName(),Utilities.toTypeString2(info.getType()));
				else
					Fugg.f.setFuggveny(info.getName() + "()","");
			}
		}
		
		List<ParameterInfo> newParam = wizard.getNewParameters_internal();
		if (newParam != null) {
			for (ParameterInfo pi : newParam) {
				Fugg.f.setFuggveny("get" + Util.capitalize(pi.getName()) + "() : " + pi.getType(),"");
				Fugg.f.setFuggveny("set" + Util.capitalize(pi.getName()) + "(" + pi.getType() + ") : void","");
			}
		}
		
		DefaultListModel rSModel = new DefaultListModel();
		DefaultListModel nRSModel = new DefaultListModel();
		for (GeneratedMemberInfo gmi : legalGenerateds) {
			Fugg.f.setFuggveny(gmi.getName() + " : " + gmi.getType(),"");
			String nameWB = gmi.getName().substring(0,gmi.getName().length() - 2);
			if (Util.isAcceptableType(gmi.getJavaType()))
				rSModel.addElement(new RecordableElement(gmi,nameWB));
			else
				nRSModel.addElement(new RecordableElement(gmi,nameWB));
		}
		return new DefaultListModel[] { rSModel, nRSModel };
	}
	
	//----------------------------------------------------------------------------------------------------
	public void saveScripts(Node node) {
		Document document = node.getOwnerDocument();
		
		final Element userVariablesElement = document.createElement(WizardSettingsManager.USER_VARIABLES);
		
		for (final UserDefinedVariable variable : userVariables) {
			final Element variableElement = document.createElement(WizardSettingsManager.VARIABLE);
			variableElement.setAttribute(WizardSettingsManager.NAME,variable.getName());
			variableElement.setAttribute(WizardSettingsManager.TYPE,variable.getType().getName());
			variableElement.setAttribute(WizardSettingsManager.DEFAULT_INITIALIZED,String.valueOf(variable.isDefaultInitialized()));
			variableElement.appendChild(document.createTextNode(variable.getInitializationCode()));
			userVariablesElement.appendChild(variableElement);
		}
		
		node.appendChild(userVariablesElement);
		
		Element scripts = document.createElement(WizardSettingsManager.SCRIPTS);
		List<MemberInfo> allMembersReally = new ArrayList<MemberInfo>(innerScripts);
		allMembersReally.addAll(allMembers);
		Collections.sort(allMembersReally,new Comparator<MemberInfo>() {
			public int compare(MemberInfo o1, MemberInfo o2) {
				if (o1 instanceof VariableScriptGeneratedMemberInfo && !(o2 instanceof VariableScriptGeneratedMemberInfo))
					return -1;
				if (o2 instanceof VariableScriptGeneratedMemberInfo && !(o1 instanceof VariableScriptGeneratedMemberInfo))
					return 1;
				return 0;
			}
		});
		
		Element element = null;
		for (int i = 0;i < allMembersReally.size();++i) {
			if (allMembersReally.get(i) instanceof GeneratedMemberInfo) {
				GeneratedMemberInfo gmi = (GeneratedMemberInfo) allMembersReally.get(i);
				Element script = document.createElement(WizardSettingsManager.SCRIPT);
				script.setAttribute(WizardSettingsManager.INNER,String.valueOf(innerScripts.contains(gmi)));
				element = document.createElement(WizardSettingsManager.NAME);
				element.appendChild(document.createTextNode(gmi.getName()));
				script.appendChild(element);
				element = document.createElement(WizardSettingsManager.TYPE);
				element.appendChild(document.createTextNode(gmi.getType()));
				script.appendChild(element);
				element = document.createElement(WizardSettingsManager.JAVA_TYPE);
				element.appendChild(document.createTextNode(gmi.getJavaType().getName()));
				script.appendChild(element);
				element = document.createElement(WizardSettingsManager.SOURCE);
				element.appendChild(document.createTextNode(gmi.getSource()));
				script.appendChild(element);
				if (!(gmi instanceof ScriptGeneratedMemberInfo)) {
					if (gmi.getGeneratorName() != null && !"unknown".equals(gmi.getGeneratorName())) {
						Element generatorName = document.createElement(WizardSettingsManager.GENERATOR_NAME);
						generatorName.appendChild(document.createTextNode(gmi.getGeneratorName()));
						script.appendChild(generatorName);
					}
					List<List<? extends Object>> buildBlocksList = gmi.getBuildingBlocks();
					if (buildBlocksList != null) {
						Element buildBlocks = document.createElement(WizardSettingsManager.BUILD_BLOCKS);
						for (List<? extends Object> buildBlock : buildBlocksList) {
							Element block = document.createElement(WizardSettingsManager.BLOCK);
							for (Object blockObject : buildBlock) {
								Element blockElement = document.createElement(WizardSettingsManager.BLOCK_ELEMENT);
								if (blockObject instanceof Class) {
									Class<?> classObject = (Class<?>) blockObject;
									blockElement.setAttribute(WizardSettingsManager.TYPE,WizardSettingsManager.CLASS);
									blockElement.appendChild(document.createTextNode(classObject.getName()));
								} else if (blockObject instanceof Integer) {
									Integer intObject = (Integer) blockObject;
									blockElement.setAttribute(WizardSettingsManager.TYPE,WizardSettingsManager.INT);
									blockElement.appendChild(document.createTextNode(intObject.toString()));
								} else if (blockObject instanceof ConstantInfo) {
									ConstantInfo constantInfoObject = (ConstantInfo) blockObject;
									blockElement.setAttribute(WizardSettingsManager.TYPE,WizardSettingsManager.CONSTANT);
									blockElement.appendChild(document.createTextNode(constantInfoObject.getName()));
								} else if (blockObject instanceof ConstantKeyInfo) {
									ConstantKeyInfo keyObject = (ConstantKeyInfo) blockObject;
									blockElement.setAttribute(WizardSettingsManager.TYPE,WizardSettingsManager.CONSTANT_KEY);
									blockElement.appendChild(document.createTextNode(keyObject.getValue().toString()));
								} else if (blockObject instanceof String) {
									final String stringObject = (String) blockObject;
									blockElement.setAttribute(WizardSettingsManager.TYPE,WizardSettingsManager.STRING);
									blockElement.appendChild(document.createTextNode(stringObject));
								} else {
									MemberInfo infoObject = (MemberInfo) blockObject;
									blockElement.setAttribute(WizardSettingsManager.TYPE,WizardSettingsManager.MEMBER);
									element = document.createElement(WizardSettingsManager.NAME);
									element.appendChild(document.createTextNode(infoObject.getName()));
									blockElement.appendChild(element);
									element = document.createElement(WizardSettingsManager.TYPE);
									element.appendChild(document.createTextNode(infoObject.getType()));
									blockElement.appendChild(element);
									element = document.createElement(WizardSettingsManager.JAVA_TYPE);
									element.appendChild(document.createTextNode(infoObject.getJavaType().getName()));
									blockElement.appendChild(element);
									element = document.createElement(WizardSettingsManager.INNER_TYPE);
									String innerType = infoObject.getInnerType() == null ? "null" : infoObject.getInnerType().getName();
									element.appendChild(document.createTextNode(innerType));
									blockElement.appendChild(element);
									if (infoObject instanceof OneArgFunctionMemberInfo) {
										OneArgFunctionMemberInfo methodInfoObject = (OneArgFunctionMemberInfo) blockObject;
										blockElement.setAttribute(WizardSettingsManager.TYPE,WizardSettingsManager.METHOD);
										element = document.createElement(WizardSettingsManager.PARAMETER_TYPE);
										element.appendChild(document.createTextNode(methodInfoObject.getParameterType().getName()));
										blockElement.appendChild(element);
									}
								}
								block.appendChild(blockElement);
							}
							buildBlocks.appendChild(block);
						}
						script.appendChild(buildBlocks);
					}
				}
				if (gmi instanceof SimpleGeneratedMemberInfo) {
					SimpleGeneratedMemberInfo sgmi = (SimpleGeneratedMemberInfo) gmi;
					script.setAttribute(WizardSettingsManager.TYPE,STATISTIC_TYPE);
					element = document.createElement(WizardSettingsManager.CALL);
					element.appendChild(document.createTextNode(sgmi.getCall()));
					script.appendChild(element);
					List<String> referencedNames = new ArrayList<String>(sgmi.getReferences().size());
					for (GeneratedMemberInfo info : sgmi.getReferences()) 
						referencedNames.add(info.getName());
					String refs = Utils.join(referencedNames,";");
					element = document.createElement(WizardSettingsManager.REFERENCES);
					if (refs != null && !"".equals(refs))
						element.appendChild(document.createTextNode(refs));
					script.appendChild(element);
				} else if (gmi instanceof OperatorGeneratedMemberInfo) {
					OperatorGeneratedMemberInfo ogmi = (OperatorGeneratedMemberInfo) gmi;
					script.setAttribute(WizardSettingsManager.TYPE,OPERATOR_TYPE);
					element = document.createElement(WizardSettingsManager.DISPLAY_NAME);
					element.appendChild(document.createTextNode(ogmi.getDisplayName()));
					script.appendChild(element);
					List<String> referencedNames = new ArrayList<String>(ogmi.getReferences().size());
					for (GeneratedMemberInfo info : ogmi.getReferences()) 
						referencedNames.add(info.getName());
					String refs = Utils.join(referencedNames,";");
					element = document.createElement(WizardSettingsManager.REFERENCES);
					if (refs != null && !"".equals(refs))
						element.appendChild(document.createTextNode(refs));
					script.appendChild(element);
					if (innerScripts.contains(ogmi)) {
						InnerOperatorGeneratedMemberInfo _info = (InnerOperatorGeneratedMemberInfo) ogmi;
						MemberInfo parentInfo = _info.getParentInfo();
						if (parentInfo != null) {
							Element parentElement = document.createElement(WizardSettingsManager.PARENT);
							element = document.createElement(WizardSettingsManager.NAME);
							element.appendChild(document.createTextNode(parentInfo.getName()));
							parentElement.appendChild(element);
							element = document.createElement(WizardSettingsManager.TYPE);
							element.appendChild(document.createTextNode(parentInfo.getType()));
							parentElement.appendChild(element);
							element = document.createElement(WizardSettingsManager.JAVA_TYPE);
							element.appendChild(document.createTextNode(parentInfo.getJavaType().getName()));
							parentElement.appendChild(element);
							element = document.createElement(WizardSettingsManager.INNER_TYPE);
							Class<?> innerType = parentInfo.getInnerType();
							String innerTypeStr = innerType == null ? "null" : innerType.getName();
							element.appendChild(document.createTextNode(innerTypeStr));
							parentElement.appendChild(element);
							script.appendChild(parentElement);
						}
					}
					if (ogmi instanceof ExtendedOperatorGeneratedMemberInfo) {
						ExtendedOperatorGeneratedMemberInfo eogmi = (ExtendedOperatorGeneratedMemberInfo) ogmi;
						Element assistantMethodsElement = document.createElement(WizardSettingsManager.ASSISTANT_METHODS);
						for (final AssistantMethod method : eogmi.getAssistantMethods()) {
							element = document.createElement(WizardSettingsManager.METHOD);
							element.setAttribute(WizardSettingsManager.RETURN_TYPE,method.returnValue.getName());
							element.setAttribute(WizardSettingsManager.SCHEDULE_TIME,method.scheduleTime.toString());
							element.appendChild(document.createTextNode(method.body));
							assistantMethodsElement.appendChild(element);
						}
						script.appendChild(assistantMethodsElement);
					}
				} else {
					ScriptGeneratedMemberInfo sgmi = (ScriptGeneratedMemberInfo) gmi;
					script.setAttribute(WizardSettingsManager.TYPE,SCRIPT_TYPE);
					List<String> referencedNames = new ArrayList<String>(sgmi.getReferences().size());
					for (GeneratedMemberInfo info : sgmi.getReferences())
						referencedNames.add(info.getName());
					String refs = Utils.join(referencedNames,";");
					element = document.createElement(WizardSettingsManager.REFERENCES);
					if (refs != null && !"".equals(refs))
						element.appendChild(document.createTextNode(refs));
					script.appendChild(element);
					String imports = Utils.join(sgmi.getImports(),";");
					element = document.createElement(WizardSettingsManager.IMPORTS);
					if (imports != null && !"".equals(imports))
						element.appendChild(document.createTextNode(imports));
					script.appendChild(element);
					List<String> usedVariables = new ArrayList<String>(sgmi.getUserVariables().size());
					for (final UserDefinedVariable variable : sgmi.getUserVariables())
						usedVariables.add(variable.getName());
					String variables = Utils.join(usedVariables,";");
					element = document.createElement(WizardSettingsManager.USER_VARIABLES);
					if (variables != null && !"".equals(variables))
						element.appendChild(document.createTextNode(variables));
					script.appendChild(element);
					if (sgmi instanceof VariableScriptGeneratedMemberInfo) {
						VariableScriptGeneratedMemberInfo vsgmi = (VariableScriptGeneratedMemberInfo) sgmi;
						element = document.createElement(WizardSettingsManager.PARENT_VARIABLE);
						element.appendChild(document.createTextNode(vsgmi.getParentVariable().getName()));
						script.appendChild(element);
					}
				}
				scripts.appendChild(script);
			}
		}
		node.appendChild(scripts);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void createScriptList(Element node, DefaultListModel rModel, DefaultListModel nrModel) throws WizardLoadingException {
		
		// clear list
		for (int i = 0;i < rModel.size();++i) {
			RecordableElement re = (RecordableElement) rModel.get(i);
			GeneratedMemberInfo gmi = (GeneratedMemberInfo) re.getInfo();
			allMembers.remove(gmi);
		}
		for (int i = 0;i < nrModel.size();++i) {
			RecordableElement re = (RecordableElement) nrModel.get(i);
			GeneratedMemberInfo gmi = (GeneratedMemberInfo) re.getInfo();
			allMembers.remove(gmi);
		}
		innerScripts.clear();
		rModel.clear();
		nrModel.clear();
		userVariables.clear();
		
		NodeList nodes = node.getElementsByTagName(WizardSettingsManager.USER_VARIABLES);
		if (nodes == null || nodes.getLength() == 0)
			throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.USER_VARIABLES);
		
		final Element userVariablesElement = (Element) nodes.item(0);
		
		nodes = userVariablesElement.getElementsByTagName(WizardSettingsManager.VARIABLE);
		for (int i = 0;i < nodes.getLength();++i) {
			final Element variable = (Element) nodes.item(i);
			final String name = variable.getAttribute(WizardSettingsManager.NAME);
			if (name == null || "".equals(name.trim()))
				throw new WizardLoadingException(true,"missing attribute " + WizardSettingsManager.NAME);
			final String typeStr = variable.getAttribute(WizardSettingsManager.TYPE);
			if (typeStr == null || "".equals(typeStr.trim()))
				throw new WizardLoadingException(true,"missing attribute " + WizardSettingsManager.TYPE + " at variable " + name);
			Class<?> javaType = null;
			try {
				javaType = Utilities.toClass(wizard.getClassLoader(),typeStr);
			} catch (ClassNotFoundException e) {
				throw new WizardLoadingException(true,"invalid type at variable: " + name);
			}
			final String defaultInitStr = variable.getAttribute(WizardSettingsManager.DEFAULT_INITIALIZED);
			if (defaultInitStr == null || "".equals(defaultInitStr.trim()))
				throw new WizardLoadingException(true,"missing attribute: " + WizardSettingsManager.DEFAULT_INITIALIZED + " at variable " + name);
			boolean defaultInitialized = Boolean.parseBoolean(defaultInitStr);
			final NodeList content = variable.getChildNodes();
			if (content == null || content.getLength() == 0)
				throw new WizardLoadingException(true,"missing initialization code at variable: " + name);
			final String code = ((Text)content.item(0)).getNodeValue();
			final UserDefinedVariable udv = new UserDefinedVariable(name,javaType,code);
			udv.setDefaultInitialized(defaultInitialized);
			userVariables.add(udv);
		}
		
		nodes = node.getElementsByTagName(WizardSettingsManager.SCRIPTS);
		if (nodes == null || nodes.getLength() == 0) return;
		
		final Element scriptsElement = (Element) nodes.item(0);
		nodes = scriptsElement.getElementsByTagName(WizardSettingsManager.SCRIPT);
		if (nodes == null) return;
		for (int i = 0;i < nodes.getLength();++i) {
			final Element script = (Element) nodes.item(i);
			
			final String innerStr = script.getAttribute(WizardSettingsManager.INNER);
			boolean inner = false;
			if (innerStr != null && !"".equals(innerStr))
				inner = Boolean.parseBoolean(innerStr);
			
			NodeList nl = script.getElementsByTagName(WizardSettingsManager.NAME);
			if (nl == null || nl.getLength() == 0)
				throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.NAME);
			Element element = (Element) nl.item(0);
			NodeList content = element.getChildNodes();
			if (content == null || content.getLength() == 0)
				throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.NAME);
			final String name = ((Text)content.item(0)).getNodeValue();
			
			nl = script.getElementsByTagName(WizardSettingsManager.TYPE);
			if (nl == null || nl.getLength() == 0)
				throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.TYPE);
			element = (Element) nl.item(0);

			content = element.getChildNodes();
			if (content == null || content.getLength() == 0)
				throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.TYPE);
			final String type = ((Text)content.item(0)).getNodeValue();
			
			nl = script.getElementsByTagName(WizardSettingsManager.JAVA_TYPE);
			if (nl == null || nl.getLength() == 0)
				throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.JAVA_TYPE);
			element = (Element) nl.item(0);
			content = element.getChildNodes();
			if (content == null || content.getLength() == 0)
				throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.JAVA_TYPE);
			final String javaTypeStr = ((Text)content.item(0)).getNodeValue();
			Class<?> javaType = null;
			try {
				javaType = Utilities.toClass(wizard.getClassLoader(),javaTypeStr);
			} catch (ClassNotFoundException e) {
				throw new WizardLoadingException(true,"invalid return type at data source: " + name);
			}
			
			nl = script.getElementsByTagName(WizardSettingsManager.SOURCE);
			if (nl == null || nl.getLength() == 0)
				throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.SOURCE);
			element = (Element) nl.item(0);
			content = element.getChildNodes();
			if (content == null || content.getLength() == 0)
				throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.SOURCE);
			final String source = ((Text)content.item(0)).getNodeValue();
			
			String[] refs = new String[0];
			nl = script.getElementsByTagName(WizardSettingsManager.REFERENCES);
			if (nl == null || nl.getLength() == 0)
				throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.REFERENCES);
			element = (Element) nl.item(0);
			content = element.getChildNodes();
			if (content != null && content.getLength() > 0) {
				final String refString = ((Text)content.item(0)).getNodeValue();
				refs = refString.trim().split(";");
			}
			
			final String scriptType = script.getAttribute(WizardSettingsManager.TYPE);
			if (scriptType == null || scriptType.trim().equals(""))
				throw new WizardLoadingException(true,"missing 'type' attribute at node: " + WizardSettingsManager.SCRIPT);
			
			if (STATISTIC_TYPE.equals(scriptType.trim())) {
				final SimpleGeneratedMemberInfo info = new SimpleGeneratedMemberInfo(name,type,javaType);
				info.setSource(source);
				
				nl = script.getElementsByTagName(WizardSettingsManager.CALL);
				if (nl == null || nl.getLength() == 0)
					throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.CALL);
				element = (Element) nl.item(0);
				content = element.getChildNodes();
				if (content == null || content.getLength() == 0)
					throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.CALL);
				final String call = ((Text)content.item(0)).getNodeValue();
				info.setCall(call);
				
				for (int j = 0;j < refs.length;++j) {
					final GeneratedMemberInfo gmi = getDefinedScript(refs[j].trim());
					if (gmi == null)
						throw new WizardLoadingException(true,"missing data source definition: " + refs[j].trim());
					info.addReference(gmi);
				}
				
				setBuildingBlocksAndGeneratorName(info,script);
				final String _name = info.getName().substring(0,info.getName().length() - 2);
				rModel.addElement(new RecordableElement(info,_name));
				allMembers.add(info);
			} else if (OPERATOR_TYPE.equals(scriptType.trim())) {
				MemberInfo parentInfo = null;
				if (inner) {
					NodeList pnl = script.getElementsByTagName(WizardSettingsManager.PARENT);
					if (pnl != null && pnl.getLength() != 0) {
						final Element parent = (Element) pnl.item(0);

						nl = parent.getElementsByTagName(WizardSettingsManager.NAME);
						if (nl == null || nl.getLength() == 0)
							throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.NAME);
						element = (Element) nl.item(0);
						content = element.getChildNodes();
						if (content == null || content.getLength() == 0)
							throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.NAME);
						final String parent_name = ((Text)content.item(0)).getNodeValue();
						
						nl = parent.getElementsByTagName(WizardSettingsManager.TYPE);
						if (nl == null || nl.getLength() == 0)
							throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.TYPE);
						element = (Element) nl.item(0);
						content = element.getChildNodes();
						if (content == null || content.getLength() == 0)
							throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.TYPE);
						final String parent_type = ((Text)content.item(0)).getNodeValue();

						nl = parent.getElementsByTagName(WizardSettingsManager.JAVA_TYPE);
						if (nl == null || nl.getLength() == 0)
							throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.JAVA_TYPE);
						element = (Element) nl.item(0);
						content = element.getChildNodes();
						if (content == null || content.getLength() == 0)
							throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.JAVA_TYPE);
						final String parent_javaTypeStr = ((Text)content.item(0)).getNodeValue();
						Class<?> parent_javaType = null;
						try {
							parent_javaType = Utilities.toClass(wizard.getClassLoader(),parent_javaTypeStr);
						} catch (ClassNotFoundException e) {
							throw new WizardLoadingException(true,"invalid return type at member: " + parent_name);
						}
						
						nl = parent.getElementsByTagName(WizardSettingsManager.INNER_TYPE);
						if (nl == null || nl.getLength() == 0)
							throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.INNER_TYPE);
						element = (Element) nl.item(0);
						content = element.getChildNodes();
						if (content == null || content.getLength() == 0)
							throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.INNER_TYPE);
						final String parent_innerTypeStr = ((Text)content.item(0)).getNodeValue();
						Class<?> parent_innerType = null;
						try {
							parent_innerType = Utilities.toClass(wizard.getClassLoader(),parent_innerTypeStr);
						} catch (ClassNotFoundException e) {
							throw new WizardLoadingException(true,"invalid inner type at member: " + parent_name);
						}
						parentInfo = new MemberInfo(parent_name,parent_type,parent_javaType);
						parentInfo.setInnerType(parent_innerType);
					}
				}

				String length = null;
				nl = script.getElementsByTagName(WizardSettingsManager.MULTICOLUMN_LENGTH);
				if (nl == null || nl.getLength() == 0){
					length = "1";
				} else {
					element = (Element) nl.item(0);
					content = element.getChildNodes();
					if (content == null || content.getLength() == 0){
						length = "1";
					} else {
						length = ((Text)content.item(0)).getNodeValue();
						if (length == null){
							length = "1";
						}
					}
				}
				
				nl = script.getElementsByTagName(WizardSettingsManager.ASSISTANT_METHODS);
				final boolean hasAssistantMethods = nl != null && nl.getLength() > 0;
				final OperatorGeneratedMemberInfo info = inner ? new InnerOperatorGeneratedMemberInfo(name,type,javaType,parentInfo) :
																(hasAssistantMethods ? new ExtendedOperatorGeneratedMemberInfo(name,type,javaType)
																					 : Collection.class.isAssignableFrom(javaType) ? new MultiColumnOperatorGeneratedMemberInfo(name, type, javaType, length) 
																																	: new OperatorGeneratedMemberInfo(name,type,javaType));
				info.setSource(source);

				nl = script.getElementsByTagName(WizardSettingsManager.DISPLAY_NAME);
				if (nl == null || nl.getLength() == 0)
					throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.DISPLAY_NAME);
				element = (Element) nl.item(0);
				content = element.getChildNodes();
				if (content == null || content.getLength() == 0)
					throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.DISPLAY_NAME);
				final String displayName = ((Text)content.item(0)).getNodeValue();
				info.setDisplayName(displayName);

				for (int j = 0;j < refs.length;++j) {
					GeneratedMemberInfo gmi = getDefinedScript(refs[j].trim());
					if (gmi == null)
						throw new WizardLoadingException(true,"missing data source definition: " + refs[j].trim());
					info.addReference(gmi);
				}
				
				if (hasAssistantMethods) {
					nl = script.getElementsByTagName(WizardSettingsManager.ASSISTANT_METHODS);
					final Element assistantMethodsElement = (Element) nl.item(0);

					nl = assistantMethodsElement.getElementsByTagName(WizardSettingsManager.METHOD);
					if (nl == null || nl.getLength() == 0)
						throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.METHOD);
					for (int j = 0;j < nl.getLength();++j) {
						final Element methodElement = (Element) nl.item(j);
						
						final String returnTypeStr = methodElement.getAttribute(WizardSettingsManager.RETURN_TYPE);
						if (returnTypeStr == null || "".equals(returnTypeStr.trim()))
							throw new WizardLoadingException(true,"missing attribute: " + WizardSettingsManager.RETURN_TYPE);
						Class<?> returnType = null;
						try {
							returnType = Utilities.toClass(wizard.getClassLoader(),returnTypeStr);
						} catch (ClassNotFoundException e) {
							throw new WizardLoadingException(true,"invalid return type at the assistant method " + i + " of  data source: " + name);
						}
						
						final String scheduleTimeStr = methodElement.getAttribute(WizardSettingsManager.SCHEDULE_TIME);
						if (scheduleTimeStr == null || "".equals(scheduleTimeStr.trim()))
							throw new WizardLoadingException(true,"missing attribute: " + WizardSettingsManager.SCHEDULE_TIME);
						final ScheduleTime scheduleTime = ScheduleTime.valueOf(scheduleTimeStr);
						
						content = methodElement.getChildNodes();
						if (content == null || content.getLength() == 0)
							throw new WizardLoadingException(true,"missing body definition at assistant method " + i + " of data source: " + name);
						final String bodyStr = ((Text)content.item(0)).getNodeValue();
						
						final ExtendedOperatorGeneratedMemberInfo eogmi = (ExtendedOperatorGeneratedMemberInfo) info;
						eogmi.addAssistantMethod(new AssistantMethod(bodyStr,returnType,scheduleTime));
					}
				}
				
				setBuildingBlocksAndGeneratorName(info,script);
				
				if (inner) {
					CollectionParameterGUI.setGeneratedIdx(CollectionParameterGUI.getGeneratedIdx() + 1);
					innerScripts.add(info);
				} else  {
					final String _name = info.getName().substring(0,info.getName().length() - 2);
					final RecordableElement recordableElement = new RecordableElement(info,_name);
					if (Util.isAcceptableType(javaType)) {
						rModel.addElement(recordableElement);
					} else {
						if (info instanceof MultiColumnOperatorGeneratedMemberInfo) {
							rModel.addElement(recordableElement);
						} else {
							nrModel.addElement(recordableElement);
						}
					}
					allMembers.add(info);
				}
			} else {
				UserDefinedVariable parentVariable = null;
				nl = script.getElementsByTagName(WizardSettingsManager.PARENT_VARIABLE);
				if (nl != null && nl.getLength() > 0) {
					element = (Element) nl.item(0);
					content = element.getChildNodes();
					if (content == null || content.getLength() == 0)
						throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.PARENT_VARIABLE);
					final String variableStr = ((Text)content.item(0)).getNodeValue();
					parentVariable = getDefinedVariable(variableStr);
				}
				
				final ScriptGeneratedMemberInfo info = parentVariable == null ? new ScriptGeneratedMemberInfo(name,type,javaType) 
																			  : new VariableScriptGeneratedMemberInfo(name,parentVariable);
				info.setSource(source);
				
				for (int j = 0;j < refs.length;++j) {
					final GeneratedMemberInfo gmi = getDefinedScript(refs[j].trim());
					if (gmi == null)
						throw new WizardLoadingException(true,"missing script definition: " + refs[j].trim());
					info.addReference(gmi);
				}
				
				nl = script.getElementsByTagName(WizardSettingsManager.USER_VARIABLES);
				if (nl == null || nl.getLength() == 0)
					throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.USER_VARIABLES);
				element = (Element) nl.item(0);
				content = element.getChildNodes();
				if (content != null && content.getLength() > 0) {
					final String uvString = ((Text)content.item(0)).getNodeValue();
					final String[] uvs = uvString.trim().split(";");
					for (final String variableName : uvs) {
						final UserDefinedVariable variable = getDefinedVariable(variableName);
						if (variable == null)
							throw new WizardLoadingException(true,"missing variable definition: " + variableName.trim());
						info.addUserVariable(variable);
					}
				}
			
				nl = script.getElementsByTagName(WizardSettingsManager.IMPORTS);
				if (nl == null || nl.getLength() == 0)
					throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.IMPORTS);
				element = (Element) nl.item(0);
				content = null;
				content = element.getChildNodes();
				if (content != null && content.getLength() > 0) {
					final String impString = ((Text)content.item(0)).getNodeValue();
					final String[] imports = impString.trim().split(";");
					for (int j = 0;j < imports.length;++j)
						info.addImport(imports[j].trim());
				}
				final String _name = info.getName().substring(0,info.getName().length() - 2);
				final RecordableElement recordableElement = new RecordableElement(info,_name);
				if (Util.isAcceptableType(javaType))
					rModel.addElement(recordableElement);
				else
					nrModel.addElement(recordableElement);
				allMembers.add(info);
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public GeneratedMemberInfo getDefinedScript(String name) { 
		for (int i = 0;i < allMembers.size();++i) {
			if (allMembers.get(i) instanceof GeneratedMemberInfo) {
				GeneratedMemberInfo gmi = (GeneratedMemberInfo) allMembers.get(i);
				if (name.equals(gmi.getName()))
					return gmi;
			}
		}
		for (int i = 0;i < innerScripts.size();++i) {
			if (name.equals(innerScripts.get(i).getName()))
				return innerScripts.get(i);
		}
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	public UserDefinedVariable getDefinedVariable(final String name) {
		if (name == null || "".equals(name.trim()))
			throw new IllegalArgumentException("'name' is null or empty");
		for (final UserDefinedVariable variable : userVariables)
			if (name.equals(variable.getName()))
				return variable;
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void setBuildingBlocksAndGeneratorName(GeneratedMemberInfo info, Element scriptNode) throws WizardLoadingException { 
		NodeList nl = scriptNode.getElementsByTagName(WizardSettingsManager.GENERATOR_NAME);
		if (nl != null && nl.getLength() > 0) {
			Element element = (Element) nl.item(0);
			NodeList content = element.getChildNodes();
			if (content != null && content.getLength() > 0) {
				String generatorName = ((Text)content.item(0)).getNodeValue();
				info.setGeneratorName(generatorName);
			}
		}
		nl = null;
		nl = scriptNode.getElementsByTagName(WizardSettingsManager.BUILD_BLOCKS);
		if (nl != null && nl.getLength() > 0) {
			if (info instanceof SimpleGeneratedMemberInfo) 
				((SimpleGeneratedMemberInfo)info).clearBuildingBlocks();
			Element buildBlocks = (Element) nl.item(0);
			nl = null;
			nl = buildBlocks.getElementsByTagName(WizardSettingsManager.BLOCK);
			if (nl == null || nl.getLength() == 0)
				throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.BLOCK);
			for (int i = 0;i < nl.getLength();++i) {
				Element block = (Element) nl.item(i);
				NodeList blockElementList = block.getElementsByTagName(WizardSettingsManager.BLOCK_ELEMENT);
				if (blockElementList == null || blockElementList.getLength() == 0)
					throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.BLOCK_ELEMENT);
				List<Object> buildBlock = new ArrayList<Object>();
				for (int j = 0;j < blockElementList.getLength();++j) {
					Element blockElement = (Element) blockElementList.item(j);
					String blockType = blockElement.getAttribute(WizardSettingsManager.TYPE);
					if ("".equals(blockType))
						throw new WizardLoadingException(true,"missing 'type' attribute at node: " + WizardSettingsManager.BLOCK_ELEMENT);
					if (WizardSettingsManager.INT.equals(blockType)) {
						NodeList content = blockElement.getChildNodes();
						if (content == null || content.getLength() == 0) 
							throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.BLOCK_ELEMENT);
							String intValueStr = ((Text)content.item(0)).getNodeValue();
							try {
								buildBlock.add(new Integer(intValueStr));
							} catch (NumberFormatException e) { throw new WizardLoadingException(true,e); }
					} else if (WizardSettingsManager.CLASS.equals(blockType)) {
						NodeList content = blockElement.getChildNodes();
						if (content == null || content.getLength() == 0) 
							throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.BLOCK_ELEMENT);
						String classStr = ((Text)content.item(0)).getNodeValue();
						try {
							Class<?> clazz = Utilities.toClass(wizard.getClassLoader(),classStr);
							buildBlock.add(clazz);
						} catch (ClassNotFoundException e) { throw new WizardLoadingException(true,e); }
					} else if (WizardSettingsManager.CONSTANT.equals(blockType)) {
						NodeList content = blockElement.getChildNodes();
						if (content == null || content.getLength() == 0) 
							throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.BLOCK_ELEMENT);
						String doubleValueStr = ((Text)content.item(0)).getNodeValue();
						try {
							double value = Double.parseDouble(doubleValueStr);
							buildBlock.add(new ConstantInfo(value));
						} catch (NumberFormatException e) { throw new WizardLoadingException(true,e); }
					} else if (WizardSettingsManager.CONSTANT_KEY.equals(blockType)) {
						NodeList content = blockElement.getChildNodes();
						if (content == null) 
							throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.BLOCK_ELEMENT);

						String str = content.getLength() == 0 ? "" : ((Text)content.item(0)).getNodeValue();
						try {
							double value = Double.parseDouble(str);
							buildBlock.add(new ConstantKeyInfo(value));
						} catch (NumberFormatException e) {
							buildBlock.add(new ConstantKeyInfo(str));
						}
					} else if (WizardSettingsManager.STRING.equals(blockType)) {
						final NodeList content = blockElement.getChildNodes();
						if (content == null)
							throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.BLOCK_ELEMENT);
						final String str;
						if (content.getLength() == 0) {
							str = ""; 
						}
						else {
							str = ((Text)content.item(0)).getNodeValue();
						}
						buildBlock.add(str);
					} else if (WizardSettingsManager.MEMBER.equals(blockType) || WizardSettingsManager.METHOD.equals(blockType)) {
						NodeList memberList = blockElement.getElementsByTagName(WizardSettingsManager.NAME);
						if (memberList == null || memberList.getLength() == 0)
							throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.NAME);
						Element element = (Element) memberList.item(0);
						NodeList content = element.getChildNodes();
						if (content == null || content.getLength() == 0)
							throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.NAME);
						String member_name = ((Text)content.item(0)).getNodeValue();
						memberList = null;
						memberList = blockElement.getElementsByTagName(WizardSettingsManager.TYPE);
						if (memberList == null || memberList.getLength() == 0)
							throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.TYPE);
						element = (Element) memberList.item(0);
						content = null;
						content = element.getChildNodes();
						if (content == null || content.getLength() == 0)
							throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.TYPE);
						String member_type = ((Text)content.item(0)).getNodeValue();
						memberList = null;
						memberList = blockElement.getElementsByTagName(WizardSettingsManager.JAVA_TYPE);
						if (memberList == null || memberList.getLength() == 0)
							throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.JAVA_TYPE);
						element = (Element) memberList.item(0);
						content = null;
						content = element.getChildNodes();
						if (content == null || content.getLength() == 0)
							throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.JAVA_TYPE);
						String member_javaTypeStr = ((Text)content.item(0)).getNodeValue();
						Class<?> member_javaType = null;
						try {
							member_javaType = Utilities.toClass(wizard.getClassLoader(),member_javaTypeStr);
						} catch (ClassNotFoundException e) {
							throw new WizardLoadingException(true,"invalid return type at member: " + member_name);
						}
						memberList = null;
						memberList = blockElement.getElementsByTagName(WizardSettingsManager.INNER_TYPE);
						if (memberList == null || memberList.getLength() == 0)
							throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.INNER_TYPE);
						element = (Element) memberList.item(0);
						content = null;
						content = element.getChildNodes();
						if (content == null || content.getLength() == 0)
							throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.INNER_TYPE);
						String member_innerTypeStr = ((Text)content.item(0)).getNodeValue();
						Class<?> member_innerType = null;
						try {
							member_innerType = Utilities.toClass(wizard.getClassLoader(),member_innerTypeStr);
						} catch (ClassNotFoundException e) {
							throw new WizardLoadingException(true,"invalid inner type at member: " + member_name);
						}
						if (WizardSettingsManager.METHOD.equals(blockType)) {
							memberList = null;
							memberList = blockElement.getElementsByTagName(WizardSettingsManager.PARAMETER_TYPE);
							if (memberList == null || memberList.getLength() == 0)
								throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.PARAMETER_TYPE);
							element = (Element) memberList.item(0);
							content = null;
							content = element.getChildNodes();
							if (content == null || content.getLength() == 0)
								throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.PARAMETER_TYPE);
							String member_parameterTypeStr = ((Text)content.item(0)).getNodeValue();
							Class<?> member_parameterType = null;
							try {
								member_parameterType = Utilities.toClass(wizard.getClassLoader(),member_parameterTypeStr);
							} catch (ClassNotFoundException e) {
								throw new WizardLoadingException(true,"invalid inner type at member: " + member_name);
							}
							OneArgFunctionMemberInfo method = new OneArgFunctionMemberInfo(member_name,member_type,member_javaType,
																						   member_parameterType);
							method.setInnerType(member_innerType);
							buildBlock.add(method);
						} else {
							MemberInfo member = getDefinedScript(member_name);
							if (member == null) 
								member = new MemberInfo(member_name,member_type,member_javaType);
							member.setInnerType(member_innerType);
							buildBlock.add(member);
						}
					} else {
						throw new WizardLoadingException(true,"invalid 'type' attribute at node: " + WizardSettingsManager.BLOCK_ELEMENT);
					}
				}
				if (info instanceof SimpleGeneratedMemberInfo) {
					List<MemberInfo> temp = new ArrayList<MemberInfo>(buildBlock.size());
					for (Object o : buildBlock)
						temp.add((MemberInfo)o);
					((SimpleGeneratedMemberInfo)info).addBuildingBlock(temp);
				} else 
					((OperatorGeneratedMemberInfo)info).setBuildingBlock(buildBlock);
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void createScript(DefaultListModel rModel, DefaultListModel nrModel) {
		ScriptCreationDialog dialog = new ScriptCreationDialog(ParameterSweepWizard.getFrame(),wizard,rModel,nrModel,this);
		dialog.showDialog();
		wizard.repaint();
	}
	
	public void createMultiColumnScript(MemberInfo selectedCollection, DefaultListModel rModel, DefaultListModel nrModel, String alias) {
		ScriptCreationDialog dialog = new ScriptCreationDialog(ParameterSweepWizard.getFrame(),wizard,rModel,nrModel,this);
		dialog.showMultiColumnRecordable(selectedCollection, alias);
		wizard.repaint();
	}
	
	//----------------------------------------------------------------------------------------------------
	public void editScript(RecordableElement selected, DefaultListModel rModel, DefaultListModel nrModel) {
		if (selected != null) {
			ScriptCreationDialog dialog = new ScriptCreationDialog(ParameterSweepWizard.getFrame(),wizard,rModel,nrModel,this);
			try {
				dialog.showEditDialog((GeneratedMemberInfo)selected.getInfo());
			} catch (CannotLoadDataSourceForEditingException e1) {
				e1.printStackTrace(ParameterSweepWizard.getLogStream());
				Utilities.userAlert(wizard,"Data source " + selected.getInfo().toString() + " is not editable.");
			}
			wizard.repaint();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void removeScript(JList recordableScripts, JList nonRecordableScripts) {
		int result = Utilities.askUser(wizard,false,"Remove data source","Do you really want to remove the selected data source(s)?");
		if (result == 1) {
			DefaultListModel rSModel = (DefaultListModel) recordableScripts.getModel();
			Object[] selected = recordableScripts.getSelectedValues();
			for (int i = 0;i < selected.length;++i) {
				GeneratedMemberInfo info = (GeneratedMemberInfo) ((RecordableElement)selected[i]).getInfo();
				List<GeneratedMemberInfo> referers = collectReferers(info);
				if (referers.size() > 0) {
					Utilities.userAlert(wizard,"Remove failed because of other data source(s) use this data source: " + info.toString(),
										"Referer data source(s): ", Utils.join(referers,",\n"));
					return;
				}
				deleteInnerScript(info);
				allMembers.remove(info);
				Fugg.f.remove(info.getName() + " : " + info.getType());
				rSModel.removeElement(selected[i]);
				recordersPage.removeFromRecorders(info);
				recordersPage.removeFromStoppingCondition(info);
			}
			DefaultListModel nRSModel = (DefaultListModel) nonRecordableScripts.getModel();
			selected = nonRecordableScripts.getSelectedValues();
			for (int i = 0;i < selected.length;++i) {
				GeneratedMemberInfo info = (GeneratedMemberInfo) ((RecordableElement)selected[i]).getInfo();
				List<GeneratedMemberInfo> referers = collectReferers(info);
				if (referers.size() > 0) {
					Utilities.userAlert(wizard,"Remove failed because of other data source(s) use this data source: " + info.toString(),
										"Referer data source(s): ", Utils.join(referers,",\n"));
					return;
				}
				deleteInnerScript(info);
				allMembers.remove(info);
				Fugg.f.remove(info.getName() + " : " + info.getType());
				nRSModel.removeElement(selected[i]);
			}
			wizard.enableDisableButtons();
		}
	}
	
	//====================================================================================================
	// private methods
	
	//-------------------------------------------------------------------------------
	/** Collects the legal and illegal generated members.
	 * @param illegalParameters the list of information objects of ceased (new) parameters
	 * @return the list of information objects of legal generated members
	 */
	private List<GeneratedMemberInfo> collectLegalsAndIllegals(List<ParameterInfo> illegalParameters) {  
		List<String> illegalReferences = new ArrayList<String>(illegalParameters.size());
		for (ParameterInfo pi : illegalParameters) {
			String name = pi.getName();
			name = "get" + Util.capitalize(name) + "()";
			illegalReferences.add(name);
		}
		illegalGenerateds = new ArrayList<GeneratedMemberInfo>();
		List<GeneratedMemberInfo> legals = new ArrayList<GeneratedMemberInfo>();
		for (MemberInfo mi : allMembers) {
			if (mi instanceof GeneratedMemberInfo) {
				GeneratedMemberInfo gmi = (GeneratedMemberInfo) mi;
				if (gmi.isValidGeneratedMember(illegalReferences))
					legals.add(gmi);
				else
					illegalGenerateds.add(gmi);
			}
		}
		for (GeneratedMemberInfo gmi : innerScripts) {
			if (!gmi.isValidGeneratedMember(illegalReferences))
				illegalGenerateds.add(gmi);
		}
		return legals;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void collectMathFunctions() { 
		Method[] methods = Math.class.getMethods();
		for (Method m : methods) {
			if (Modifier.isStatic(m.getModifiers()) && m.getParameterTypes().length == 1 && m.getReturnType() != Void.TYPE) {
				OneArgFunctionMemberInfo info = new OneArgFunctionMemberInfo("java.lang.Math." + m.getName() + "()",
																			 Utilities.toTypeString1(m.getReturnType()),m.getReturnType(),
																			 m.getParameterTypes()[0]);
				foreachMembers.add(info);
			}
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Creates the list of information objects of generated getter methods.
	 * @param newParameters the list of information objects of new parameters
	 */
	private List<MemberInfo> createGeneratedGettersMemberInfo(List<ParameterInfo> newParameters) {
		List<MemberInfo> result = new ArrayList<MemberInfo>(newParameters.size());
		for (ParameterInfo pi : newParameters) {
			String name = pi.getName();
			name = "get" + Util.capitalize(name) + "()";
			MemberInfo info = new MemberInfo(name,pi.getType(),pi.getJavaType());
			result.add(info);
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private List<GeneratedMemberInfo> collectReferers(GeneratedMemberInfo info) { 
		List<GeneratedMemberInfo> result = new ArrayList<GeneratedMemberInfo>();
		for (MemberInfo mi : allMembers) {
			if (mi instanceof GeneratedMemberInfo) {
				GeneratedMemberInfo gmi = (GeneratedMemberInfo) mi;
				if (gmi.getReferences().contains(info))
					result.add(gmi);
			}
		}
		for (GeneratedMemberInfo gmi : innerScripts) {
			if (gmi.getReferences().contains(info))
				result.add(gmi);
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void deleteInnerScript(GeneratedMemberInfo info) { 
		List<GeneratedMemberInfo> references = info.getReferences();
		for (GeneratedMemberInfo ref : references) {
			if (ref instanceof InnerOperatorGeneratedMemberInfo)
				innerScripts.remove(ref);
		}
	}
}
