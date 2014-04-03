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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ai.aitia.meme.utils.Utils.Pair;

public class UserToolGroup {

	//=======================================================================================
	// nested types
	
	//---------------------------------------------------------------------------------------
	public static enum ToolType { PROGRAM { @Override public String toString() { return UserToolGroup.PROGRAM; } }, 
								  DOCUMENT { @Override public String toString() { return UserToolGroup.DOCUMENT; } }
	};
	
	//---------------------------------------------------------------------------------------
	public static class UserToolItem {
		
		//====================================================================================================
		// members
		
		private ToolType type;
		private String menuText = null;
		private String command = null;
		private String arguments = null;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public UserToolItem(ToolType type, String menuText) {
			super();
			this.type = type;
			this.menuText = menuText;
		}
		
		//----------------------------------------------------------------------------------------------------
		public UserToolItem(ToolType type, String menuText, String command, String arguments) {
			this(type,menuText);
			this.command = command;
			this.arguments = arguments;
		}
		
		//----------------------------------------------------------------------------------------------------
		public ToolType getType() { return type; }
		public String getMenuText() { return menuText; }
		public String getCommand() { return command; }
		public String getArguments() { return arguments; }
		public boolean isProgram() { return ToolType.PROGRAM.equals(type); }
		public boolean isDocument() { return ToolType.DOCUMENT.equals(type); }
		
		//----------------------------------------------------------------------------------------------------
		public void setMenuText(String menuText) { this.menuText = menuText; }
		public void setCommand(String command) { this.command = command; }
		public void setArguments(String arguments) { this.arguments = arguments; }
		
		//----------------------------------------------------------------------------------------------------
		public void save(Node node) {
			Document document = node.getOwnerDocument();
			Element toolElement = document.createElement(UserToolManager.TOOL);
			toolElement.setAttribute(UserToolManager.TYPE,type.toString());
			
			Element menuTextElement = document.createElement(UserToolManager.MENU_TEXT);
			menuTextElement.appendChild(document.createTextNode(menuText));
			toolElement.appendChild(menuTextElement);
			
			Element commandElement = document.createElement(UserToolManager.COMMAND);
			commandElement.appendChild(document.createTextNode(command));
			toolElement.appendChild(commandElement);
			
			if (arguments != null) {
				Element argsElement = document.createElement(UserToolManager.ARGS);
				argsElement.appendChild(document.createTextNode(arguments));
				toolElement.appendChild(argsElement);
			}
			node.appendChild(toolElement);
		}
		
		//----------------------------------------------------------------------------------------------------
		@Override public String toString() { return menuText; }
		
		//----------------------------------------------------------------------------------------------------
		@Override
		public boolean equals(Object o) {
			if (o instanceof UserToolItem) {
				UserToolItem that = (UserToolItem)o;
				return this.menuText.equals(that.menuText);
			}
			return false;
		}
	}
	
	//---------------------------------------------------------------------------------------
	public static class GroupIsFullException extends Exception {
		
		//====================================================================================================
		// members
		
		private static final long serialVersionUID = 1L;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public GroupIsFullException() { super(); }
	}
	
	//----------------------------------------------------------------------------------------------------
	public static class EnvironmentVariable extends Pair<String,String> implements Comparable<EnvironmentVariable> {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public EnvironmentVariable(String name, String value) { super(name,value); }
		
		//----------------------------------------------------------------------------------------------------
		@Override
		public boolean equals(Object o) {
			if (o instanceof EnvironmentVariable) {
				EnvironmentVariable that = (EnvironmentVariable) o;
				return this.first.equals(that.first);
			}
			return false;
		}
		
		//----------------------------------------------------------------------------------------------------
		@Override public String toString() { return first + " = " + second; }
		public int compareTo(EnvironmentVariable env) { return this.first.compareTo(env.first); }
		
		//----------------------------------------------------------------------------------------------------
		public void save(Node node) {
			Document document = node.getOwnerDocument();
			Element varElement = document.createElement(UserToolManager.VARIABLE);
			varElement.setAttribute(UserToolManager.NAME,first);
			varElement.appendChild(document.createTextNode(second));
			node.appendChild(varElement);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static class RequiredEnvironmentVariable extends EnvironmentVariable {

		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public RequiredEnvironmentVariable(String name, String description) { super(name,description); }
		
		//----------------------------------------------------------------------------------------------------
		public String getDescription() { return second; }
		
		//----------------------------------------------------------------------------------------------------
		@Override public String toString() { return first + " = ? (" + second + ")"; }
	}
	
	//=======================================================================================
	// members
	
	private static final int MAX_NUMBER_OF_TOOLS_PER_GROUP = 10;
	
	public static final String PROGRAM 	= "PROGRAM";
	public static final String DOCUMENT	= "DOCUMENT";
	
	private static List<EnvironmentVariable> globalVariables = new ArrayList<EnvironmentVariable>();
	
	private String name = null;
	private List<UserToolItem> tools = null;
	private List<EnvironmentVariable> environmentalVariables = null;
	
	//=======================================================================================
	// methods
	
	//---------------------------------------------------------------------------------------
	public UserToolGroup(String name) {
		if (name == null || "".equals(name.trim()))
			throw new IllegalArgumentException("'name' cannot be empty.");
		this.name = name;
		this.tools = new ArrayList<UserToolItem>();
		this.environmentalVariables = new ArrayList<EnvironmentVariable>();
	}
	
	//---------------------------------------------------------------------------------------
	public static List<EnvironmentVariable> getGlobalVariables() { return globalVariables; }
	public List<EnvironmentVariable> getEnvironmentVariables() { return environmentalVariables; }
	public List<UserToolItem> getTools() { return tools; }
	public void setName(String name) { this.name = name; }
	
	//----------------------------------------------------------------------------------------------------
	public String lookup(String key) {
		if (key == null) return null;
		EnvironmentVariable env = new EnvironmentVariable(key,"");
		int resultIdx = environmentalVariables.indexOf(env);
		if (resultIdx == -1) {
			resultIdx = globalVariables.indexOf(env);
			if (resultIdx == -1) return null;
			else
				return globalVariables.get(resultIdx).getValue();
		}
		return environmentalVariables.get(resultIdx).getValue();
	}
	
	//---------------------------------------------------------------------------------------
	public boolean addTool(UserToolItem tool) throws GroupIsFullException {
		if (tools.size() >= MAX_NUMBER_OF_TOOLS_PER_GROUP)
			throw new GroupIsFullException();
		if (tools.contains(tool))
			return false;
		tools.add(tool);
		return true;
	}
	
	//--------------------------------------------------------------------------------------
	public void remove(UserToolItem tool) { tools.remove(tool); }
	public void remove(int index) {
		if (index < tools.size())
			tools.remove(index);
	}
	
	//--------------------------------------------------------------------------------------
	public void moveUp(int index) {
		if (index == 0) return;
		UserToolItem tool = tools.remove(index);
		tools.add(index - 1,tool);
	}
	
	//--------------------------------------------------------------------------------------
	public void moveDown(int index) {
		if (index == tools.size() - 1) return;
		moveUp(index + 1);
	}
	
	//--------------------------------------------------------------------------------------
	public void save(Node node) {
		Document document = node.getOwnerDocument();
		Element groupElement = document.createElement(UserToolManager.GROUP);
		groupElement.setAttribute(UserToolManager.NAME,name);
		for (EnvironmentVariable env : environmentalVariables)
			env.save(groupElement);
		for (UserToolItem tool : tools)
			tool.save(groupElement);
		node.appendChild(groupElement);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void saveGlobalVariables(Node node) {
		Document document = node.getOwnerDocument();
		Element globalElement = document.createElement(UserToolManager.GLOBAL_ENV_VAR);
		for (EnvironmentVariable env : globalVariables) 
			env.save(globalElement);
		node.appendChild(globalElement);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static int indexOfGlobalVariable(EnvironmentVariable env) { return globalVariables.indexOf(env); }
	
	//--------------------------------------------------------------------------------------
	public boolean isUsedToolName(ToolType type, String name) {
		UserToolItem dummy = new UserToolItem(type,name);
		return tools.contains(dummy);
	}
	
	//--------------------------------------------------------------------------------------
	public String createDefaultName(ToolType type, String prefix) {
		int i = 1;
		for (;;) {
			String candidate = prefix + " " + i++;
			if (!isUsedToolName(type,candidate))
				return candidate;
		}
	}
	
	//---------------------------------------------------------------------------------------
	@Override public String toString() { return name; }
	@Override public boolean equals(Object o) {
		if (o instanceof UserToolGroup) {
			UserToolGroup that = (UserToolGroup) o;
			return this.name.equals(that.name);
		}
		return false;
	}
}
