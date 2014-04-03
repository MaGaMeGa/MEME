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

import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.generator.IStatisticInfoGenerator;
import ai.aitia.meme.paramsweep.gui.info.GeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.OneArgFunctionMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.RecordableElement;
import ai.aitia.meme.paramsweep.gui.info.VariableScriptGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.plugin.IStatisticsPlugin;
import ai.aitia.meme.paramsweep.utils.MemberInfoList;
import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;

public interface IScriptSupport {

	//====================================================================================================
	// methods 
	
	public MemberInfoList getAllMembers();
	public List<OneArgFunctionMemberInfo> getForeachMembers();
	public List<GeneratedMemberInfo> getInnerScripts();
	public List<UserDefinedVariable> getUserVariables();
	public List<GeneratedMemberInfo> getIllegalGenerateds();
	public DefaultListModel[] initializeScriptSupport(List<ParameterInfo> illegalParameters);
	public void illegalGeneratedsToNull();
	public void saveScripts(Node node);
	public void createScriptList(Element node,DefaultListModel rModel, DefaultListModel nrModel) throws WizardLoadingException;
	public GeneratedMemberInfo getDefinedScript(String name);
	public void createScript(DefaultListModel rModel, DefaultListModel nrModel);
	public void createMultiColumnScript(MemberInfo selectedCollection, DefaultListModel rModel, DefaultListModel nrModel, String alias);
	public void editScript(RecordableElement selected, DefaultListModel rModel, DefaultListModel nrModel);
	public void removeScript(JList recordableScripts, JList nonRecordableScripts);
	public boolean isValidScriptName(String name);
	public boolean isValidVariableName(String name);
	public IStatisticInfoGenerator getStatisticInfoGenerator(IStatisticsPlugin statistic);
	public String getDefaultInitialization(String name, String defaultValue);
	public VariableScriptGeneratedMemberInfo createScriptForVariable(UserDefinedVariable variable);
	public VariableScriptGeneratedMemberInfo getScriptForVariable(UserDefinedVariable variable);
	public ParameterSweepWizard getWizard();
}
