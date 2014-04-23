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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ViewRec;
import ai.aitia.meme.gui.DialogPluginContext;
import ai.aitia.meme.gui.MainWindow;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.pluginmanager.IExportPlugin;
import ai.aitia.meme.usertools.UTParameterComb.CleanerComputedUTParameter;
import ai.aitia.meme.usertools.UTParameterComb.ConstantUTParameter;
import ai.aitia.meme.usertools.UTParameterComb.UTParameter;
import ai.aitia.meme.usertools.UserToolGroup.UserToolItem;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.OSUtils.OSType;
import ai.aitia.meme.utils.Utils.IUnaryCallable;

public class UserToolParser {

	//=======================================================================================
	// nested classes
	
	public static class Argument implements Comparable<Argument> {
		private String id;
		private String text;
		private String tooltip;
		private boolean hasParameter = false;
		public Argument(String id, String text, String tooltip) {
			this.id = id;
			this.text = text;
			this.tooltip = tooltip;
		}
		public Argument(String id, String text, String tooltip, boolean hasParameter) {
			this(id,text,tooltip);
			this.hasParameter = hasParameter;
		}
		public String getId() { return id; }
		public String getText() { return text; }
		public String getTooltip() { return tooltip; }
		public String getArgument() {
			String ans = id;
			if (hasParameter) ans += "( )";
			return ans;
		}
		public int compareTo(Argument o) { return text.compareTo(o.text); }
		@Override public String toString() {return text; }
		@Override public boolean equals(Object o) {
			if (o instanceof Argument) {
				Argument that = (Argument)o;
				return this.id.equals(that.id);
			} 
			return false;
		}
	}
	
	public static class UserToolParserException extends Exception {
		private static final long serialVersionUID = 1L;
		public UserToolParserException(String message) { super(message); }
		public UserToolParserException(Throwable cause) { super(cause); }
		public UserToolParserException(String message, Throwable cause) { super(message,cause); }
	}
	
	//=======================================================================================
	// members
	
	private static String PERCENT = "____PERCENT_yeah_come_on____";
	
	private static List<Argument> possibleArguments = new ArrayList<Argument>();
	static {
		possibleArguments.add(new Argument("$View","Selected view","Each selected view table"));
		possibleArguments.add(new Argument("$ViewName","Name of the selected view","Name of each selected view table"));
		possibleArguments.add(new Argument("$ViewCol","Column's name of the selected view","N-th column's name of each selected view table",true));
		
		possibleArguments.add(null); // separator
		possibleArguments.add(new Argument("$Result","Selected result","Each (model,version) pairs from the selected result tables"));
		possibleArguments.add(new Argument("$ResultName","Name of the selected result","Name of each selected result table"));
		possibleArguments.add(new Argument("$ResultCol","Column's name of the selected result","N-th column's name of each selected result table",true));
		
		possibleArguments.add(null); // separator
		possibleArguments.add(new Argument("$ResultOrView","Selected result or view","Same as 'Selected result' if the result page/window is active and same as 'Selected view' otherwise"));
		
		possibleArguments.add(null); // separator
		possibleArguments.add(new Argument("$Input","User input","<html>User can define parameter value before each run.<br>Example: $Input(Output file) defines an input parameter named 'Output file'.</html>",true));
		
		possibleArguments.add(null); // separator
		possibleArguments.add(new Argument("$ChartXml","XML description of the created charts","XML description of the created charts"));
		possibleArguments.add(new Argument("$ChartPng","PNG version of the created charts","PNG version of the created charts"));
	}
	
	private UserToolItem userTool = null;
	private UserToolGroup group = null;
	private MainWindow mainWindow = null;
	private boolean verboseMode = false;
	
	private List<String> deleteList = new ArrayList<String>();
	
	//=======================================================================================
	// methods

	//---------------------------------------------------------------------------------------
	public UserToolParser(MainWindow mainWindow, UserToolItem userTool, UserToolGroup group, boolean verboseMode) {
		this.mainWindow = mainWindow;
		this.userTool = userTool;
		this.group = group;
		this.verboseMode = verboseMode;
	}
	
	//--------------------------------------------------------------------------------------
	public MainWindow getMainWindow() { return mainWindow; }
	
	//---------------------------------------------------------------------------------------
	public void execute() throws UserToolParserException {
		if (userTool.isDocument()) 
			openDocument();
		else {
			final List<UTParameterComb> combs = parse();
			if (combs == null) return;
			MEMEApp.WAITING_USER_TOOL = true;
			final String userCommand = replaceEnvironmentVariables(userTool.getCommand());
			Thread executor = new Thread(new Runnable() {
				public void run() {
					for (int i = 0;i < combs.size();++i) {
						try {
							if (i == combs.size() - 1)
								MEMEApp.WAITING_USER_TOOL = false;
							String command = userCommand;
							if (MEMEApp.getOSUtils().getOSType() == OSType.WINDOWS && command.indexOf(" ") > 0) {
								command = command.replaceAll("\"","");
								command = "\"" + command + "\"";
							}
							String argument = combs.get(i).getArgumentString();
							String parts[] = argument.split(" ");
							parts = fillWithUserInputs(parts);
							argument = Utils.join(" ",(Object[])parts);
							if (verboseMode) 
								Logger.logError("[Verbose] " + command + " " + argument);
							Process p = Runtime.getRuntime().exec(command + " " + argument);
							BufferedReader br = null;
							String line = null;
							br = new BufferedReader(new InputStreamReader(p.getInputStream()));
							while ((line = br.readLine()) != null) { 
								if (!"".equals(line))
									Logger.logError("[Output] " + line);
							}
							br.close();
							br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
							line = null;
							boolean first = true;
							while ((line = br.readLine()) != null) {
								if (first) {
									Logger.logError("User tool error: " + userTool.getMenuText());
									Logger.logError("Command: " + userTool.getCommand() + "(" + command + ")");
									Logger.logError("Argument: " + userTool.getArguments() + " (" + argument + ")");
									first = false;
								}
								Logger.logError("[Error] " + line);
							}
							br.close();
							int exitCode = p.waitFor();
							if (verboseMode || !first)
								Logger.logError("Exit code: %d",exitCode);
						} catch (InterruptedException e) { 
							continue;
						} catch (IOException e) {
							MEMEApp.WAITING_USER_TOOL = false;
							Logger.logException(e);
							clean();
							return;
						} catch (UserToolParserException e) {
							MEMEApp.WAITING_USER_TOOL = false;
							Logger.logException(e);
							clean();
							return;
						} 
					}
					clean();
				}
			});
			executor.setName("MEME-UserToolExecutor-Thread");
			executor.start();
		}
	}
	
	//--------------------------------------------------------------------------------------
	public void addDeletableItem(String item) {
		deleteList.add(item);
	}
	
	//---------------------------------------------------------------------------------------
	public static List<Argument> getPossibleArguments() { return Collections.unmodifiableList(possibleArguments); }
	
	//---------------------------------------------------------------------------------------
	private void openDocument() throws UserToolParserException {
		final File doc = new File(replaceEnvironmentVariables(userTool.getCommand()));
		if (!doc.exists())
			throw new UserToolParserException("File not found: " + doc.toString());
		MEMEApp.LONG_OPERATION.begin(String.format("Opening %s...", doc.toString()), new LongRunnable() {
			@Override public void trun() throws Exception {
				MEMEApp.getOSUtils().openDocument(doc.toURI(), doc.getParentFile());
			}
		});
	}
	
	//--------------------------------------------------------------------------------------
	private List<UTParameterComb> parse() throws UserToolParserException {
		String argumentLine = userTool.getArguments() == null ? "" : userTool.getArguments().trim().replaceAll("\\s"," ");
		argumentLine = replaceEnvironmentVariables(argumentLine);
		List<UTParameterComb> ans = new ArrayList<UTParameterComb>();
		if (argumentLine == null || "".equals(argumentLine)) {
			ans.add(new UTParameterComb());
			return ans;
		}
		String[] parts = argumentLine.split(" ");
		Object masterParam = null;
//		try {
//			parts = fillWithUserInputs(parts);
//		} catch (InterruptedException e1) { return null; }
		replaceSelectedResultOrView(parts);
		ans.add(new UTParameterComb());
		for (String s : parts) {
			if ("$View".equals(s) || "$$View".equals(s)) {
				ViewRec[] views = mainWindow.getViewsPanel().getSelectedViews();
				if (views == null || views.length == 0)
					throw new UserToolParserException("No selected views");
				if (masterParam != null && "$View".equals(s))
					addCSVViewParam(masterParam,ans,views);
				 else {
					if (masterParam == null)
						masterParam = views;
					List<CleanerComputedUTParameter> params = new ArrayList<CleanerComputedUTParameter>(views.length);
					for (int i = 0;i < views.length; ++i) {
						IUnaryCallable<String,UserToolParser> callable = createCSVCallable(views[i]);
						params.add(new CleanerComputedUTParameter(views[i].getName(),this,callable));
					}
					cloneParameterCombs(ans,params);
				}
			} else if ("$ViewName".equals(s) || "$$ViewName".equals(s)) {
				ViewRec[] views = mainWindow.getViewsPanel().getSelectedViews();
				if (views == null || views.length == 0)
					throw new UserToolParserException("No selected views");
				if (masterParam != null && "$ViewName".equals(s)) 
					addViewNameParam(masterParam,ans,views);
				else {
					if (masterParam == null)
						masterParam = views;
					List<ConstantUTParameter> params = new ArrayList<ConstantUTParameter>(views.length);
					for (int i = 0;i < views.length;++i)
						params.add(new ConstantUTParameter(views[i].getName(),"\"" + views[i].getName() + "\""));
					cloneParameterCombs(ans,params);
				}
			} else if (s.startsWith("$ViewCol(") || s.startsWith("$$ViewCol(")) {
				ViewRec[] views = mainWindow.getViewsPanel().getSelectedViews();
				if (views == null || views.length == 0)
					throw new UserToolParserException("No selected views");
				String colParam = s.replaceAll(" ","").substring(9);
				colParam = colParam.substring(0,colParam.length() - 1);
				if (s.startsWith("$$")) colParam = colParam.substring(1);
				int colParamNo;
				try {
					colParamNo = Integer.parseInt(colParam);
				} catch (NumberFormatException e) {
					throw new UserToolParserException("Invalid parameter in $ViewCol()");
				} 
				if (masterParam != null && s.startsWith("$ViewCol(")) 
					addViewColParam(colParamNo,masterParam,ans,views);
				else {
					if (masterParam == null)
						masterParam = views;
					List<ConstantUTParameter> params = new ArrayList<ConstantUTParameter>(views.length);
					for (int i = 0;i < views.length;++i) { 
						String viewColumnName = getViewColumnName(views[i].getViewID(),colParamNo-1);
						params.add(new ConstantUTParameter(views[i].getName(), "\"" + viewColumnName + "\""));
					}
					cloneParameterCombs(ans,params);
				}
			} else if ("$Result".equals(s) || "$$Result".equals(s)) {
				Long[][] models = mainWindow.getResultsBrowser().getSelection();
				if (models == null || models.length == 0)
					throw new UserToolParserException("No selected results");
				if (masterParam != null && "$Result".equals(s))
					addCSVResultParam(masterParam,ans,models);
				 else {
					if (masterParam == null)
						masterParam = models;
					List<CleanerComputedUTParameter> params = new ArrayList<CleanerComputedUTParameter>(models.length);
					for (int i = 0;i < models.length; ++i) {
						IUnaryCallable<String,UserToolParser> callable = createCSVCallable(models[i]);
						params.add(new CleanerComputedUTParameter(String.valueOf(models[i][0]),this,callable));
					}
					cloneParameterCombs(ans,params);
				}
				
			} else if ("$ResultName".equals(s) || "$$ResultName".equals(s)) {
				Long[][] models = mainWindow.getResultsBrowser().getSelection();
				if (models == null || models.length == 0)
					throw new UserToolParserException("No selected results");
				if (masterParam != null && "$ResultName".equals(s)) 
					addResultNameParam(masterParam,ans,models);
				else {
					if (masterParam == null)
						masterParam = models;
					List<ConstantUTParameter> params = new ArrayList<ConstantUTParameter>(models.length);
					for (int i = 0;i < models.length;++i) {
						String name = getModelName(models[i][0]);
						params.add(new ConstantUTParameter(String.valueOf(models[i][0]),"\"" + name + "\""));
					}
					cloneParameterCombs(ans,params);
				}
			} else if (s.startsWith("$ResultCol(") || s.startsWith("$$ResultCol(")) {
				Long[][] models = mainWindow.getResultsBrowser().getSelection();
				if (models == null || models.length == 0)
					throw new UserToolParserException("No selected results");
				String colParam = s.replaceAll(" ","").substring(11);
				if (s.startsWith("$$")) colParam = colParam.substring(1);
				colParam = colParam.substring(0,colParam.length() - 1);
				int colParamNo;
				try {
					colParamNo = Integer.parseInt(colParam);
				} catch (NumberFormatException e) {
					throw new UserToolParserException("Invalid parameter in $ResultCol()");
				} 
				if (masterParam != null && s.startsWith("$ResultCol(")) 
					addResultColParam(colParamNo,masterParam,ans,models);
				else {
					if (masterParam == null)
						masterParam = models;
					List<ConstantUTParameter> params = new ArrayList<ConstantUTParameter>(models.length);
					for (int i = 0;i < models.length;++i) { 
						String resultColumnName = getResultColumnName(models[i][0],colParamNo-1);
						params.add(new ConstantUTParameter(String.valueOf(models[i][0]),"\"" + resultColumnName + "\""));
					}
					cloneParameterCombs(ans,params);
				}
			} else if ("$ChartXml".equals(s) || "$$ChartXml".equals(s)) {
				File dir = new File("Temp");
				if (!dir.exists()) dir.mkdir();
				int index = 0;
				File file = null;
				do 
					file = new File(dir,"tempChartDescriptor_" + index++ + ".xml");
				while (file.exists());
				try {
					mainWindow.getChartsPanel().saveChartsAsXML(file);
					deleteList.add(file.getAbsolutePath());
				} catch (Exception e) {
					throw new UserToolParserException(e);
				}
				String path = file.getAbsolutePath();
				if (MEMEApp.getOSUtils().getOSType() == OSType.WINDOWS)
					path = "\"" + path + "\"";
				addParameter(ans,new ConstantUTParameter(null,path));
			} else if ("$ChartPng".equals(s) || "$$ChartPng".equals(s)) {
				File dir = new File("Temp");
				if (!dir.exists()) dir.mkdir();
				int noImages;
				try {
					noImages = mainWindow.getChartsPanel().saveChartsAsPNG(dir,"tempChartImage");
				} catch (Exception e) {
					throw new UserToolParserException(e);
				}
				if (noImages < 1)
					throw new UserToolParserException("No charts");
				String[] files = new String[noImages];
				for (int i = 0;i < noImages;++i) {
					files[i] = (new File(dir,"tempChartImage_" + i + ".png")).getAbsolutePath();
					deleteList.add(files[i]);
					if (MEMEApp.getOSUtils().getOSType() == OSType.WINDOWS)
						files[i] = "\"" + files[i] + "\"";
				}
				if (masterParam != null && "$ChartPng".equals(s)) { 
					ConstantUTParameter param = new ConstantUTParameter(files[0],files[0]);
					addParameter(ans,param);
				} else {
					if (masterParam == null)
						masterParam = files;
					List<ConstantUTParameter> params = new ArrayList<ConstantUTParameter>(files.length);
					for (int i = 0;i < files.length;++i) 
						params.add(new ConstantUTParameter(files[i],files[i]));
					cloneParameterCombs(ans,params);
				}
			} else addParameter(ans,new ConstantUTParameter(null,s));
		}
		return ans;
	}
	
	//--------------------------------------------------------------------------------------
	private void addParameter(List<UTParameterComb> list, UTParameter param) {
		for (UTParameterComb pc : list)
			pc.addParameter(param);
	}
	
	//--------------------------------------------------------------------------------------
	private void cloneParameterCombs(List<UTParameterComb> list, List<? extends UTParameter> params) {
		List<UTParameterComb> tempList = new ArrayList<UTParameterComb>();
		for (UTParameterComb pc : list) {
			tempList.add(pc);
			for (int i = 1;i < params.size();++i) {
				UTParameterComb newPC = (UTParameterComb)pc.clone();
				newPC.addParameter(params.get(i));
				tempList.add(newPC);
			}
			pc.addParameter(params.get(0));
		}
		list.clear();
		list.addAll(tempList);
	}
	
	//--------------------------------------------------------------------------------------
	private void addCSVViewParam(Object masterParam, List<UTParameterComb> list, ViewRec[] views) throws UserToolParserException {
		if (!(masterParam instanceof ViewRec[])) {
			IUnaryCallable<String,UserToolParser> callable = createCSVCallable(views[0]);
			CleanerComputedUTParameter param = new CleanerComputedUTParameter(null,this,callable);
			addParameter(list,param);
		} else {
			for (UTParameterComb pc : list) {
				UTParameter master = null;
				int i = 0;
				do 
					master = pc.getParameter(i++);
				while (master.getActualID() == null);
				int index = findViewRec(views,master.getActualID());
				if (index == -1)
					throw new IllegalStateException();
				IUnaryCallable<String,UserToolParser> callable = createCSVCallable(views[index]);
				CleanerComputedUTParameter param = new CleanerComputedUTParameter(null,this,callable);
				pc.addParameter(param);
			}
		}
	}
	
	//--------------------------------------------------------------------------------------
	private void addCSVResultParam(Object masterParam, List<UTParameterComb> list, Long[][] models) throws UserToolParserException {
		if (!(masterParam instanceof Long[][])) {
			IUnaryCallable<String,UserToolParser> callable = createCSVCallable(models[0]);
			CleanerComputedUTParameter param = new CleanerComputedUTParameter(null,this,callable);
			addParameter(list,param);
		} else {
			for (UTParameterComb pc : list) {
				UTParameter master = null;
				int i = 0;
				do
					master = pc.getParameter(i++);
				while (master.getActualID() == null);
				int index = findModel(models,master.getActualID());
				if (index == -1)
					throw new IllegalStateException();
				IUnaryCallable<String,UserToolParser> callable = createCSVCallable(models[index]);
				CleanerComputedUTParameter param = new CleanerComputedUTParameter(null,this,callable);
				pc.addParameter(param);
			}
		}
	}
 	
	//--------------------------------------------------------------------------------------
	private void addViewNameParam(Object masterParam, List<UTParameterComb> list, ViewRec[] views) throws UserToolParserException {
		if (!(masterParam instanceof ViewRec[])) {
			ConstantUTParameter param = new ConstantUTParameter(null,"\"" + views[0].getName() + "\"");
			addParameter(list,param);
		} else {
			for (UTParameterComb pc : list) {
				UTParameter master = null;
				int i = 0;
				do 
					master = pc.getParameter(i++);
				while (master.getActualID() == null);
				int index = findViewRec(views,master.getActualID());
				if (index == -1)
					throw new IllegalStateException();
				ConstantUTParameter param = new ConstantUTParameter(null,"\"" + views[index].getName() + "\"");
				pc.addParameter(param);
			}
		}
	}
	
	//--------------------------------------------------------------------------------------
	private void addResultNameParam(Object masterParam, List<UTParameterComb> list, Long[][] models) throws UserToolParserException  {
		if (!(masterParam instanceof Long[][])) {
			String name = getModelName(models[0][0]);
			ConstantUTParameter param = new ConstantUTParameter(null,"\"" + name + "\"");
			addParameter(list,param);
		} else {
			for (UTParameterComb pc : list) {
				UTParameter master = null;
				int i = 0;
				do
					master = pc.getParameter(i++);
				while (master.getActualID() == null);
				int index = findModel(models,master.getActualID());
				if (index == -1)
					throw new IllegalStateException();
				String name = getModelName(models[index][0]);
				ConstantUTParameter param = new ConstantUTParameter(null,"\"" + name + "\"");
				pc.addParameter(param);
			}
		}
	}
	
	//--------------------------------------------------------------------------------------
	private void addViewColParam(int col, Object masterParam, List<UTParameterComb> list, ViewRec[] views) throws UserToolParserException {
		if (!(masterParam instanceof ViewRec[])) {
			String viewColumnName = getViewColumnName(views[0].getViewID(),col - 1);
			ConstantUTParameter param = new ConstantUTParameter(null,"\"" + viewColumnName + "\"");
			addParameter(list,param);
		} else {
			for (UTParameterComb pc : list) {
				UTParameter master = null;
				int i = 0;
				do
					master = pc.getParameter(i++);
				while (master.getActualID() == null);
				int index = findViewRec(views,master.getActualID());
				if (index == -1)
					throw new IllegalStateException();
				String viewColumnName = getViewColumnName(views[index].getViewID(),col - 1);
				ConstantUTParameter param = new ConstantUTParameter(null,"\"" + viewColumnName + "\"");
				pc.addParameter(param);
			}
		}
	}
	
	//--------------------------------------------------------------------------------------
	private void addResultColParam(int col, Object masterParam, List<UTParameterComb> list, Long[][] models) throws UserToolParserException {
		if (!(masterParam instanceof Long[][])) {
			String resultColumnName = getResultColumnName(models[0][0],col - 1);
			ConstantUTParameter param = new ConstantUTParameter(null,"\"" + resultColumnName + "\"");
			addParameter(list,param);
		} else {
			for (UTParameterComb pc : list) {
				UTParameter master = null;
				int i = 0;
				do
					master = pc.getParameter(i++);
				while (master.getActualID() == null);
				int index = findModel(models,master.getActualID());
				if (index == -1)
					throw new IllegalStateException();
				String resultColumnName = getResultColumnName(models[index][0],col - 1);
				ConstantUTParameter param = new ConstantUTParameter(null, "\"" + resultColumnName + "\"");
				pc.addParameter(param);
			}
		}
	}
	
	//--------------------------------------------------------------------------------------
	private int findViewRec(ViewRec[] views, String name) {
		for (int i = 0;i < views.length;++i) {
			if (views[i].getName().equals(name)) return i;
		}
		return -1;
	}
	
	//--------------------------------------------------------------------------------------
	private int findModel(Long[][] models, String name) {
		for (int i = 0;i < models.length;++i) {
			if (String.valueOf(models[i][0]).equals(name)) return i;
		}
		return -1;
	}
	
	//--------------------------------------------------------------------------------------
	private IUnaryCallable<String,UserToolParser> createCSVCallable(final Object o) throws UserToolParserException {
		final IExportPlugin plugin = mainWindow.findCSVExportPlugin();
		if (plugin == null) 
			throw new UserToolParserException("CSV export plugin is not available.");
		return new IUnaryCallable<String,UserToolParser>() {
			public String call(UserToolParser arg) {
				File f = null;
				if (o instanceof ViewRec) {
					ViewRec view = (ViewRec) o;
					f = new File("Temp" + File.separator + view.getName() + ".csv");
					if (f.exists()) {
						if (MEMEApp.getOSUtils().getOSType() == OSType.WINDOWS)
							return "\"" + f.getAbsolutePath() + "\"";
						else
							return f.getAbsolutePath();
					}
				} else if (o instanceof Long[]) {
					Long[] res = (Long[]) o;
					f = new File("Temp" + File.separator + "result_model_id" + res[0] + ".csv");
					if (f.exists()) {
						if (MEMEApp.getOSUtils().getOSType() == OSType.WINDOWS)
							return "\"" + f.getAbsolutePath() + "\"";
						else
							return f.getAbsolutePath();
					}
				}	
				DialogPluginContext ctx = new DialogPluginContext(null);
				ctx.put("ONE_OBJECT",o);
				plugin.showDialog(ctx);
				if (MEMEApp.getOSUtils().getOSType() == OSType.WINDOWS)
					return "\"" + f.getAbsolutePath() + "\"";
				else
					return f.getAbsolutePath();
			}
		};
	}
	
	//--------------------------------------------------------------------------------------
	private String getViewColumnName(final long view_id, final int col_index) throws UserToolParserException {
		try {
			GUIUtils.setBusy(mainWindow.getJFrame(),true);
			String ans = (String)MEMEApp.LONG_OPERATION.execute("Reading view...",new Callable<Object>() {
				public Object call() throws Exception {
					MEMEApp.LONG_OPERATION.progress(-1);
					Columns cols = MEMEApp.getViewsDb().getColumns(view_id);
					return cols.get(col_index).getName();
				}
			});
			return ans;
		} catch (Exception e) {
			throw new UserToolParserException(e);
		} finally {
			GUIUtils.setBusy(mainWindow.getJFrame(),false);
		}
	}
	
	//--------------------------------------------------------------------------------------
	private String getResultColumnName(final long model_id, final int col_index) throws UserToolParserException {
		try {
			GUIUtils.setBusy(mainWindow.getJFrame(),true);
			String ans = (String)MEMEApp.LONG_OPERATION.execute("Reading results...",new Callable<Object>() {
				public Object call() throws Exception {
					MEMEApp.LONG_OPERATION.progress(-1);
					Columns[] cols_array = MEMEApp.getResultsDb().getModelColumns(model_id);
					if (col_index < cols_array[0].size())
						return cols_array[0].get(col_index).getName();
					else
						return cols_array[1].get(col_index - cols_array[0].size()).getName();
				}
			});
			return ans;
		} catch (Exception e) {
			throw new UserToolParserException(e);
		} finally {
			GUIUtils.setBusy(mainWindow.getJFrame(),false);
		}
	}
	
	//---------------------------------------------------------------------------------------
	private String getModelName(final long model_id) throws UserToolParserException {
		try {
			GUIUtils.setBusy(mainWindow.getJFrame(),true);
			String ans = (String)MEMEApp.LONG_OPERATION.execute("Searching result...",new Callable<Object>() {
				public Object call() throws Exception {
					MEMEApp.LONG_OPERATION.progress(-1);
					Model model = MEMEApp.getResultsDb().findModel(model_id);
					return model.getName() + ":" + model.getVersion();
				}
			});
			return ans;
		} catch (Exception e) {
			throw new UserToolParserException(e);
		} finally {
			GUIUtils.setBusy(mainWindow.getJFrame(),false);
		}
	}
	
	//--------------------------------------------------------------------------------------
	private void clean() {
		for (String str : deleteList) {
			File f = new File(str);
			if (f.exists())
				f.delete();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void replaceSelectedResultOrView(String[] arguments) {
		JPanel activePanel = MEMEApp.getMainWindow().activePanel.getValue();
		String newArgumentSuffix = "$View";
		if (activePanel == MEMEApp.getMainWindow().getResultsPanel())
			newArgumentSuffix = "$Result";
		for (int i = 0;i < arguments.length;++i) {
			String newArgument = "";
			if (arguments[i].trim().endsWith("$ResultOrView")) {
				if (arguments[i].trim().startsWith("$$"))
					newArgument += "$";
				newArgument += newArgumentSuffix;
				arguments[i] = newArgument;
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private String[] fillWithUserInputs(String[] arguments) throws UserToolParserException, InterruptedException {
		List<String> newArguments = new ArrayList<String>(arguments.length);
		for (int i = 0;i < arguments.length;++i) {
			if (arguments[i].trim().startsWith("$Input(") ||
				arguments[i].trim().startsWith("$$Input(")) {
				String arg = "";
				while (!arguments[i].endsWith(")")) {
					arg += " " + arguments[i++];
					if (i == arguments.length)
						throw new UserToolParserException("Invalid '$Input' argument: missing ')' character");
				}
				arg += " " + arguments[i];
				arg = arg.trim();
				int firstIdx = arg.indexOf('(');
				int lastIdx = arg.lastIndexOf(')');
				String parameterName = arg.substring(firstIdx + 1,lastIdx);
				String value = (String) JOptionPane.showInputDialog(MEMEApp.getAppWnd(),parameterName + ": ","User input",JOptionPane.PLAIN_MESSAGE,null,null,"");
				if (value == null)
					throw new InterruptedException();
				else if (value.indexOf(" ") > 0)
					value = "\"" + value + "\"";
				newArguments.add(value);
			} else
				newArguments.add(arguments[i]);
		}
		return newArguments.toArray(new String[0]);
	}
	
	//----------------------------------------------------------------------------------------------------
	private String replaceEnvironmentVariables(String orig) throws UserToolParserException {
		String temp = orig.trim().replaceAll("\\s"," ").replaceAll("\\\\%",PERCENT);
		String[] words = temp.split("%");
		for (int i = 1;i < words.length;i += 2) {
			String replacement = group.lookup(words[i]);
			if (replacement == null)
				throw new UserToolParserException("Missing environment variable: " + words[i]);
			words[i] = replacement;
		}
		String result = Utils.join("",(Object[])words);
		result = result.replaceAll(PERCENT,"%");
		return result;
	}
}
