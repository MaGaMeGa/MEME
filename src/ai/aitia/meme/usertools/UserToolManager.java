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
package ai.aitia.meme.usertools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.gui.UTEditVariableDialog;
import ai.aitia.meme.usertools.UserToolGroup.EnvironmentVariable;
import ai.aitia.meme.usertools.UserToolGroup.GroupIsFullException;
import ai.aitia.meme.usertools.UserToolGroup.RequiredEnvironmentVariable;
import ai.aitia.meme.usertools.UserToolGroup.ToolType;
import ai.aitia.meme.usertools.UserToolGroup.UserToolItem;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.GUIUtils.MessageBoard;

public class UserToolManager {

	//=======================================================================================
	// members

	private static final String ROOT 	= "user_tools";
	static final String GROUP			= "group";
	static final String NAME			= "name";
	static final String TOOL			= "tool";
	static final String TYPE			= "type";
	static final String MENU_TEXT		= "menu_text";
	static final String COMMAND			= "command";
	static final String ARGS			= "args";
	static final String GLOBAL_ENV_VAR	= "global_environment_variables";
	static final String VARIABLE		= "variable";
	static final String DESCRIPTION		= "description";

	private static final File userToolsFile = new File("usertools.xml");
	private static final File customGroupDirectory = new File("UserToolPackages");
	private static final File installedFile = new File(customGroupDirectory,"installedPackages.list");
	
	static {
		if (!customGroupDirectory.exists())
			customGroupDirectory.mkdir();
	}
	
	private List<UserToolGroup> groups = null;
	
	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public static UserToolManager newInstance() {
		UserToolManager ans = new UserToolManager();
		ans.reload();
		return ans;
	}
	
	//--------------------------------------------------------------------------------------
	public void save() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = factory.newDocumentBuilder();
		Document document = parser.newDocument();
		
		Element root = document.createElement(ROOT);
		UserToolGroup.saveGlobalVariables(root);
		for (UserToolGroup utg : groups)
			utg.save(root);
		document.appendChild(root);
		
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		Source source = new DOMSource(document);
		FileOutputStream os = new FileOutputStream(userToolsFile);
		Result result = new StreamResult(os);
		transformer.transform(source,result);
	}
	
	//--------------------------------------------------------------------------------------
	public void reload() {
		try {
			groups.clear();
			UserToolGroup.getGlobalVariables().clear();
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder parser = factory.newDocumentBuilder();
			Document document = parser.parse(userToolsFile);
			Element root = document.getDocumentElement();
			NodeList globalNode = root.getElementsByTagName(GLOBAL_ENV_VAR);
			if (globalNode != null && globalNode.getLength() > 0) {
				Element globalElement = (Element) globalNode.item(0);
				NodeList varNodes = globalElement.getElementsByTagName(VARIABLE);
				if (varNodes != null && varNodes.getLength() > 0) {
					for (int i = 0;i < varNodes.getLength();++i) {
						Element varElement = (Element) varNodes.item(i);
						String name = varElement.getAttribute(NAME);
						if (name == null || "".equals(name.trim()) || isUsedVariableName(null,name.trim()))
							throw new Exception();
						Text text = (Text) varElement.getChildNodes().item(0);
						String value = text == null ? "" : text.getNodeValue();
						EnvironmentVariable env = new EnvironmentVariable(name,value);
						UserToolGroup.getGlobalVariables().add(env);
					}
				}
			}
			NodeList groupNodes = root.getElementsByTagName(GROUP);
			if (groupNodes == null || groupNodes.getLength() == 0)
				throw new Exception();
			for (int i = 0;i < groupNodes.getLength();++i) {
				Element groupNode = (Element) groupNodes.item(i);
				String name = groupNode.getAttribute(NAME);
				if (name == null || "".equals(name.trim()) || isUsedGroupName(name))
					throw new Exception();
				UserToolGroup utGroup = new UserToolGroup(name);
				NodeList varNodes = groupNode.getElementsByTagName(VARIABLE);
				if (varNodes != null && varNodes.getLength() > 0) {
					for (int j = 0;j < varNodes.getLength();++j) {
						Element varElement = (Element) varNodes.item(j);
						String varName = varElement.getAttribute(NAME);
						if (varName == null || "".equals(varName.trim()) || isUsedVariableName(utGroup,varName.trim()))
							throw new Exception();
						Text text = (Text) varElement.getChildNodes().item(0);
						String value = text == null ? "" : text.getNodeValue();
						EnvironmentVariable env = new EnvironmentVariable(varName,value);
						utGroup.getEnvironmentVariables().add(env);
					}
				}
				NodeList toolNodes = groupNode.getElementsByTagName(TOOL);
				if (toolNodes == null || toolNodes.getLength() == 0) {
					groups.add(utGroup);
					continue;
				}
				for (int j = 0; j < toolNodes.getLength();++j) {
					Element toolNode = (Element) toolNodes.item(j);
					String typeStr = toolNode.getAttribute(TYPE);
					ToolType type = null;
					try {
						type = toToolType(typeStr);
					} catch (IllegalArgumentException e) { continue; }
					String menuText = null, command = null, args = null;
					NodeList nodes = toolNode.getElementsByTagName(MENU_TEXT);
					if (nodes == null || nodes.getLength() == 0) continue;
					Element element = (Element) nodes.item(0);
					Text text = (Text) element.getChildNodes().item(0);
					if (text == null) continue;
					menuText = text.getNodeValue();
					nodes = toolNode.getElementsByTagName(COMMAND);
					if (nodes == null || nodes.getLength() == 0) continue;
					element = (Element)nodes.item(0);
					text = (Text) element.getChildNodes().item(0);
					if (text == null) continue;
					command = text.getNodeValue();
					if (ToolType.PROGRAM.equals(type)) {
						nodes = toolNode.getElementsByTagName(ARGS);
						if (nodes != null && nodes.getLength() > 0) {
							element = (Element)nodes.item(0);
							text = (Text) element.getChildNodes().item(0);
							args = text == null ? "" : text.getNodeValue();
						}
					}
					UserToolItem item = new UserToolItem(type,menuText,command,args);
					try {
						utGroup.addTool(item);
					} catch (GroupIsFullException e) { break; }
				}
				groups.add(utGroup);
			}
		} catch (Exception e) {
			MEMEApp.logError("Invalid or missing user tool configuration file.");
			createDefaultGroups(this);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void scanCustomGroups() {
		List<String> installed = getInstalledPackages();
		List<String> invalidFiles = new ArrayList<String>();
		File[] candidates = customGroupDirectory.listFiles();
		for (File candidate : candidates) {
			String tmp = candidate.getName().toLowerCase();
			if (!tmp.endsWith("xml") || installed.contains(candidate.getName())) continue;
			UserToolGroup group = loadCustomGroup(candidate);
			if (group == null) {
				invalidFiles.add(candidate.getName());
				continue;
			}
			MessageBoard board = null;
			try {
				board = new MessageBoard(MEMEApp.getMainWindow().getJFrame(),"Installing user tool package: " + candidate.getName());
				board.showScreen();
				UserToolGroup replaced = null;
				if (groups.contains(group)) {
					int result = MEMEApp.askUser(false,"Override confirmation","Group " + group + " already exists. " +
												 "Do you want to override?", "Answer 'No' means MEME won't use the " +
												 "user tools contained by " + candidate.getName());
					if (result == 0) continue;
					int idx = groups.indexOf(group);
					replaced = groups.get(idx);
				} else 
					replaced = (UserToolGroup) JOptionPane.showInputDialog(MEMEApp.getMainWindow().getJFrame(),"Select a group which you want to replace with the" +
																		   " new one (" + group + ").","Place selection",JOptionPane.QUESTION_MESSAGE,null,
																		   groups.toArray(),groups.get(0));
				if (replaced == null) continue;
				if (!setEnvironmentVariables(group)) continue;
				int replacedIndex = groups.indexOf(replaced);
				groups.set(replacedIndex,group);
				installed.add(candidate.getName());
			} finally {
				if (board != null)
					board.hideScreen();
			}
		}
		saveInstalledPackages(installed);
		try {
			save();
		} catch (Exception e) {
			MEMEApp.logExceptionCallStack("UserToolsManager.scanCustomGroups",e);
		}
		if (invalidFiles.size() > 0) {
			String msg = Utils.join(invalidFiles,"\n");
			MEMEApp.userAlert("The following user tool packages are invalid:",msg);
		}
	}
	
	//--------------------------------------------------------------------------------------
	public List<UserToolGroup> getGroups() { return Collections.unmodifiableList(groups); }
	public UserToolGroup getGroup(int index) { return index >= groups.size() ? null : groups.get(index); }
	public UserToolGroup getGroup(String name) { return groups.get(groups.indexOf(new UserToolGroup(name))); }
	
	//---------------------------------------------------------------------------------------
	public boolean isUsedGroupName(String name) {
		UserToolGroup dummy = new UserToolGroup(name);
		return groups.contains(dummy);
	}
	
	//---------------------------------------------------------------------------------------
	protected UserToolManager() {
		groups = new ArrayList<UserToolGroup>();
	}
	
	//---------------------------------------------------------------------------------------
	private static ToolType toToolType(String s) throws IllegalArgumentException {
		if (s == null) 
			throw new IllegalArgumentException();
		if (UserToolGroup.PROGRAM.equals(s))
			return ToolType.PROGRAM;
		else if (UserToolGroup.DOCUMENT.equals(s))
			return ToolType.DOCUMENT;
		throw new IllegalArgumentException();
	}
	
	//--------------------------------------------------------------------------------------
	private static void createDefaultGroups(UserToolManager utm) {
		utm.groups.clear();
		for (int i = 1;i <= 10;++i)
			utm.groups.add(new UserToolGroup("Group " + i));
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isUsedVariableName(UserToolGroup group, String name) {
		List<EnvironmentVariable> list = group == null ? UserToolGroup.getGlobalVariables() : group.getEnvironmentVariables();
		EnvironmentVariable dummy = new EnvironmentVariable(name,"");
		return list.contains(dummy);
	}
	
	//----------------------------------------------------------------------------------------------------
	private List<String> getInstalledPackages() {
		List<String> installed = new ArrayList<String>();
		if (installedFile.exists()) {
			BufferedReader reader = null; 
			try {
				reader = new BufferedReader(new FileReader(installedFile));
				String line = null;
				while ((line = reader.readLine()) != null) 
					installed.add(line.trim());
			} catch (FileNotFoundException e) {
				// never happens
				throw new IllegalStateException(e);
			} catch (IOException e) {
				MEMEApp.logExceptionCallStack(e);
			} finally {
				try { reader.close(); } catch (IOException e1) {}
			}
		}
		return installed;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void saveInstalledPackages(List<String> installed) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(installedFile));
			for (String s : installed)
				writer.println(s);
			writer.flush();
		} catch (IOException e) {
			// never happens
			throw new IllegalStateException();
		} finally {
			writer.close();
		}
		
	}
	
	//----------------------------------------------------------------------------------------------------
	private UserToolGroup loadCustomGroup(File file) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder parser = factory.newDocumentBuilder();
			Document document = parser.parse(file);
			Element root = document.getDocumentElement();
			String name = root.getAttribute(NAME);
			if (name == null || "".equals(name.trim()))
				throw new Exception();
			UserToolGroup utGroup = new UserToolGroup(name);
			NodeList varNodes = root.getElementsByTagName(VARIABLE);
			if (varNodes != null && varNodes.getLength() > 0) {
				for (int i = 0;i < varNodes.getLength();++i) {
						Element varElement = (Element) varNodes.item(i);
						String varName = varElement.getAttribute(NAME);
						if (varName == null || "".equals(varName.trim()))
							throw new Exception();
						String description = varElement.getAttribute(DESCRIPTION);
						if (description == null)
							description = "";
						RequiredEnvironmentVariable env = new RequiredEnvironmentVariable(varName,description);
						utGroup.getEnvironmentVariables().add(env);
				}
			}
			NodeList toolNodes = root.getElementsByTagName(TOOL);
			for (int i = 0; i < toolNodes.getLength();++i) {
				Element toolNode = (Element) toolNodes.item(i);
				String typeStr = toolNode.getAttribute(TYPE);
				ToolType type = null;
				try {
					type = toToolType(typeStr);
				} catch (IllegalArgumentException e) { continue; }
				String menuText = null, command = null, args = null;
				NodeList nodes = toolNode.getElementsByTagName(MENU_TEXT);
				if (nodes == null || nodes.getLength() == 0) continue;
				Element element = (Element) nodes.item(0);
				Text text = (Text) element.getChildNodes().item(0);
				if (text == null) continue;
				menuText = text.getNodeValue();
				nodes = toolNode.getElementsByTagName(COMMAND);
				if (nodes == null || nodes.getLength() == 0) continue;
				element = (Element) nodes.item(0);
				text = (Text) element.getChildNodes().item(0);
				if (text == null) continue;
				command = text.getNodeValue();
				if (ToolType.PROGRAM.equals(type)) {
					nodes = toolNode.getElementsByTagName(ARGS);
					if (nodes != null && nodes.getLength() > 0) {
						element = (Element)nodes.item(0);
						text = (Text)element.getChildNodes().item(0);
						args = text == null ? "" : text.getNodeValue();
					}
				}
				UserToolItem item = new UserToolItem(type,menuText,command,args);
				try {
					utGroup.addTool(item);
				} catch (GroupIsFullException e) { break; }
			}
			return utGroup;
		} catch (Exception e) {
			MEMEApp.logError("Invalid user tool definition file: " + file.getName());
			return null;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean setEnvironmentVariables(UserToolGroup group) {
		List<EnvironmentVariable> overridedGlobalVariables = new ArrayList<EnvironmentVariable>();
		final List<EnvironmentVariable> requiredVariables = new ArrayList<EnvironmentVariable>(group.getEnvironmentVariables());
		group.getEnvironmentVariables().clear();
		for (EnvironmentVariable v : requiredVariables) {
			if (v instanceof RequiredEnvironmentVariable) {
				RequiredEnvironmentVariable env = (RequiredEnvironmentVariable) v;
				int idx = UserToolGroup.indexOfGlobalVariable(env);
				if (idx == -1) {
					UTEditVariableDialog dlg = new UTEditVariableDialog(MEMEApp.getMainWindow().getJFrame(),group);
					int result = dlg.showDialog(env.getKey(),"",env.getDescription());
					if (result == UTEditVariableDialog.OK_OPTION) {
						EnvironmentVariable defined = dlg.getVariable();
						group.getEnvironmentVariables().add(defined);
					} else return false;
				} else {
					EnvironmentVariable existed = UserToolGroup.getGlobalVariables().get(idx);
					UTEditVariableDialog dlg = new UTEditVariableDialog(MEMEApp.getMainWindow().getJFrame(),group);
					int result = dlg.showDialog(env.getKey(),existed.getValue(),env.getDescription());
					EnvironmentVariable defined = existed;
					if (result == UTEditVariableDialog.OK_OPTION)
						defined = dlg.getVariable();
					overridedGlobalVariables.add(defined);
				}
			}
			else 
				throw new IllegalStateException();
		}
		for (EnvironmentVariable v : overridedGlobalVariables) {
			int idx = UserToolGroup.getGlobalVariables().indexOf(v);
			if (idx == -1)
				throw new IllegalStateException();
			UserToolGroup.getGlobalVariables().set(idx,v);
		}
		return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void main(String[] args) {
		UserToolManager.newInstance();
	}
}
