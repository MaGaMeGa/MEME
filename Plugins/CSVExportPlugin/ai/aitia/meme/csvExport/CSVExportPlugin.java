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
package ai.aitia.meme.csvExport;

import static ai.aitia.meme.utils.Utils.composeCSVLine;
import static ai.aitia.meme.utils.Utils.getBooleanValue;

import java.awt.Window;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

import ai.aitia.meme.UserPrefs;
import ai.aitia.meme.csvExport.CSVExportSettingsDialog.Item;
import ai.aitia.meme.database.AbstractResultsDb;
import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.ViewRec;
import ai.aitia.meme.database.ViewsDb;
import ai.aitia.meme.database.Result.Row;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.pluginmanager.IExportPlugin;
import ai.aitia.meme.pluginmanager.IExportPluginContext;
import ai.aitia.meme.pluginmanager.Parameter;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.visu.globalhandlers.UserBreakException;

/** Plugin for exporting results and views to CSV file(s). */
public class CSVExportPlugin implements IExportPlugin {

	//================================================================================
	// implementation of the IExportPlugin interface
	
	//--------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public Window showDialog(final IExportPluginContext ctx) {
		Object justSettings = ctx.get("JUST_SETTINGS");
		if (justSettings != null) {
			CSVExportSettingsDialog dialog = new CSVExportSettingsDialog(ctx,null);
			dialog.start();
			return null;
		}
		
		Object oneObject = ctx.get("ONE_OBJECT");
		
		final boolean macroMode = oneObject != null;
		final boolean isView = (macroMode ? oneObject instanceof ViewRec : ctx.getActive() == IExportPluginContext.VIEWS);
		ArrayList<Object> selection = ctx.getSelection();
		
		final ArrayList<ViewRec> views = new ArrayList<ViewRec>();
		final ArrayList<Long> results = new ArrayList<Long>();
		if (macroMode) {
			if (oneObject instanceof ViewRec) {
				ViewRec v = (ViewRec)oneObject;
				views.add(v);
			} else if (oneObject instanceof Long[]) {
				Long l = ((Long[])oneObject)[0];
				results.add(l);
			} else return null;
		} else {
			for (Object o : selection) {
				if (o instanceof ViewRec) {
					ViewRec v = (ViewRec)o;
					views.add(v);
				} else if (o instanceof Long[]) {
					Long[] l = (Long[])o;
					results.add(l[0]);
				}
			}
		}
		String title = null;
		HashMap<Long,String> resultNames = new HashMap<Long, String>();
		if (!macroMode) {
			try {
				if (!isView) {
					if (results.size() == 1) {
						GUIUtils.setBusy(ctx.getAppWindow(),true);
						String modelAndVersion = (String)ai.aitia.meme.MEMEApp.LONG_OPERATION.execute("Collect information...",
								                    new Callable<Object>(){
														public Object call() throws Exception {
															ArrayList<Long> one = new ArrayList<Long>(1);
															one.add(results.get(0));
															Object[] temp = getResultNames(ctx,one);
															String ans = ((Item<Long>)temp[0]).toString();
															return ans;
														}
													});
						GUIUtils.setBusy(ctx.getAppWindow(),false);
						title = modelAndVersion;
					} else {
						GUIUtils.setBusy(ctx.getAppWindow(),true);
						Object[] rNames = (Object[])ai.aitia.meme.MEMEApp.LONG_OPERATION.execute("Collect information...",
												new Callable<Object>(){
													public Object call() throws Exception {
														return getResultNames(ctx,results);
													}
												});
						GUIUtils.setBusy(ctx.getAppWindow(),false);
						for (Object o : rNames) {
							Item<Long> item = (Item<Long>)o;
							resultNames.put(item.getItem(),item.toString());
						}
						title = "All selected results";
					}
				} else {
					if (views.size() == 1) title = views.get(0).getName();
					else title = "All selected views";
				}
			} catch (Exception e) {
				if (!(e instanceof UserBreakException)) {
					ai.aitia.meme.MEMEApp.logExceptionCallStack(e);
					ai.aitia.meme.MEMEApp.userErrors("Error while collecting informations",
													 Utils.getLocalizedMessage(e)+".");
				}
				return null;
			}
		
			CSVExportSettingsDialog dialog = new CSVExportSettingsDialog(ctx,title);
			if (dialog.start() == CSVExportSettingsDialog.OK_OPTION) {
				java.io.File f = null;
				if ((isView && views.size() == 1) ||
					(!isView && results.size() == 1)) {
					f = CSVExportSettingsDialog.saveFileDialog(ctx.getAppWindow(),title,true);
					while (f != null && f.exists()) {
						int result = ai.aitia.meme.MEMEApp.askUser(false,"Override confirmation",f.getName() + " already exists.", "Do you want to replace it?");
						if (result == 1) break;
						f = CSVExportSettingsDialog.saveFileDialog(ctx.getAppWindow(),title,true);
					}
					if (f == null) return null;
				} else {
					f = CSVExportSettingsDialog.saveFileDialog(ctx.getAppWindow(),"",false);
					if (f == null) return null;
				}
				java.io.File dir = f.isDirectory() ? f : f.getParentFile();
				String dir_str = dir.getPath() + java.io.File.separator; 
				if (isView) {
					for (ViewRec cV : views) {
						GUIUtils.setBusy(ctx.getAppWindow(),true);
						final ViewRec fcV = cV;
						if (views.size() > 1) f = new java.io.File(dir_str + cV.getName() + ".csv");
						final java.io.File file = f;
						ai.aitia.meme.MEMEApp.LONG_OPERATION.begin("Saving " + file.getName() + "...",new LongRunnable() {
							@Override
							public void trun() throws Exception {
								saveViewToCSV(file,ctx,fcV);
							}
	
							@Override
							public void finished() {
								GUIUtils.setBusy(ctx.getAppWindow(),false);
								if (getReq().getError() != null) {
									if (!(getReq().getError() instanceof UserBreakException)) {
										ai.aitia.meme.MEMEApp.logExceptionCallStack(getReq().getError());
									}
									if (file.exists()) file.delete();
			 					}
							}
						});
					}
				} else {
					for (Long rV : results) {
						GUIUtils.setBusy(ctx.getAppWindow(),true);
						final Long frV = rV;
						if (results.size() > 1) f = new java.io.File(dir_str + resultNames.get(rV) + ".csv");
						final java.io.File file = f;
						ai.aitia.meme.MEMEApp.LONG_OPERATION.begin("Saving " + file.getName() + "...",new LongRunnable() {
							@Override
							public void trun() throws Exception {
								saveResultToCSV(file,ctx,frV);
							}
	
							@Override
							public void finished() {
								GUIUtils.setBusy(ctx.getAppWindow(),false);
								if (getReq().getError() != null) {
									if (!(getReq().getError() instanceof UserBreakException)) {
										ai.aitia.meme.MEMEApp.logExceptionCallStack(getReq().getError());
									}
									if (file.exists()) file.delete();
			 					}
							}
						});
					}
				}
			}
		} else {
			java.io.File tempDir = new File("Temp");
			if (!tempDir.exists()) tempDir.mkdir();
			String dir_str = tempDir.getPath() + java.io.File.separator; 
			if (isView) {
				GUIUtils.setBusy(ctx.getAppWindow(),true);
				final ViewRec fcV = views.get(0);
				final java.io.File f = new java.io.File(dir_str + fcV.getName() + ".csv");
				ai.aitia.meme.MEMEApp.LONG_OPERATION.begin("Creating " + f.getName() + "...",new LongRunnable() {
					@Override
					public void trun() throws Exception {
						saveViewToCSV(f,ctx,fcV);
					}
	
					@Override
					public void finished() {
						GUIUtils.setBusy(ctx.getAppWindow(),false);
						if (getReq().getError() != null) {
							if (!(getReq().getError() instanceof UserBreakException)) 
								ai.aitia.meme.MEMEApp.logExceptionCallStack(getReq().getError());
							if (f.exists()) f.delete();
			 			}
					}
				});
			} else {
				GUIUtils.setBusy(ctx.getAppWindow(),true);
				final Long frV = results.get(0);
				final java.io.File f = new java.io.File(dir_str + "result_model_id" + frV + ".csv");
				ai.aitia.meme.MEMEApp.LONG_OPERATION.begin("Creating " + f.getName() + "...",new LongRunnable() {
					@Override
					public void trun() throws Exception {
						saveResultToCSV(f,ctx,frV);
					}
	
					@Override
					public void finished() {
						GUIUtils.setBusy(ctx.getAppWindow(),false);
						if (getReq().getError() != null) {
							if (!(getReq().getError() instanceof UserBreakException)) 
								ai.aitia.meme.MEMEApp.logExceptionCallStack(getReq().getError());
							if (f.exists()) f.delete();
			 			}
					}
				});
			}
		}
		return null;
	}

	//--------------------------------------------------------------------------------
	public String getLocalizedName() {
		return "CSV file";
	}

	//================================================================================
	// private methods

	//--------------------------------------------------------------------------------
	// [Model thread]
	/** Writes a view table to a CSV file. */
	private void saveViewToCSV(java.io.File f, IExportPluginContext ctx, ViewRec view) throws Exception {
		ai.aitia.meme.MEMEApp.LONG_OPERATION.progress(-1,-1);
		String delimiter = ai.aitia.meme.MEMEApp.userPrefs.get(UserPrefs.CSVE_DELIMITER,",");
		String decimalSign = ai.aitia.meme.MEMEApp.userPrefs.get(UserPrefs.CSVE_DECIMALSIGN,".");
		String nullString = ai.aitia.meme.MEMEApp.userPrefs.get(UserPrefs.CSVE_NULLSTRING,"null");
		boolean header = getBooleanValue(ai.aitia.meme.MEMEApp.userPrefs.get(UserPrefs.CSVE_INCLUDEHEADER,"yes"));

		ViewsDb db = ctx.getViewsDb();
		PrintWriter pw = new PrintWriter(f);
		ai.aitia.meme.MEMEApp.LONG_OPERATION.progress(0,db.getNrOfRows(view.getViewID()) + 1);
		Columns cols = db.getColumns(view.getViewID());
		boolean[] isDouble = new boolean[cols.size()];
		Object[] values = new String[cols.size()];
		int index = 0;
		for (Parameter par : cols) {
			isDouble[index] = par.getDatatype().equals(ColumnType.DOUBLE);
			values[index] = par.getName();
			index += 1;
		}
		if (header) pw.println(composeCSVLine(delimiter, true, values));
		int lineNr = 0;
		final Iterable<GeneralRow> rows = db.getRows(view.getViewID(),0,-1); 
		for (GeneralRow row : rows) {
			ai.aitia.meme.MEMEApp.LONG_OPERATION.progress(lineNr++);
			for (int i=0;i<row.size();++i) {
				String v = row.getLocalizedString(i);
				if (v == null && !nullString.equals("<nothing>")) v = nullString;
				values[i] = isDouble[i] ? replaceSign(v, decimalSign) : v;
			}
			pw.println(composeCSVLine(delimiter, true, values));
		}
		pw.flush();
		pw.close();
	}
	
	//-------------------------------------------------------------------------------
	// [Model thread]
	/** Writes a result table to a CSV file. */
	private void saveResultToCSV(java.io.File f, IExportPluginContext ctx, Long id) throws Exception {
		ai.aitia.meme.MEMEApp.LONG_OPERATION.progress(-1,-1);
		String delimiter = ai.aitia.meme.MEMEApp.userPrefs.get(UserPrefs.CSVE_DELIMITER,",");
		String decimalSign = ai.aitia.meme.MEMEApp.userPrefs.get(UserPrefs.CSVE_DECIMALSIGN,".");
		String nullString = ai.aitia.meme.MEMEApp.userPrefs.get(UserPrefs.CSVE_NULLSTRING,"null");
		boolean header = getBooleanValue(ai.aitia.meme.MEMEApp.userPrefs.get(UserPrefs.CSVE_INCLUDEHEADER,"yes"));

		AbstractResultsDb db = ctx.getResultsDb();
		PrintWriter pw = new PrintWriter(f);
		ai.aitia.meme.MEMEApp.LONG_OPERATION.progress(0,db.getNumberOfRows(id.longValue(),null) + 1);
		Columns[] cols = db.getModelColumns(id.longValue());
		boolean[] isDouble = new boolean[cols[0].size() + cols[1].size()];
		Object[] values = new String[cols[0].size() + cols[1].size()];
		int index = 0;
		for (Parameter par : cols[0]) { // input params
			isDouble[index] = par.getDatatype().equals(ColumnType.DOUBLE);
			values[index] = par.getName();
			index += 1;
		}
		for (Parameter par : cols[1]) { // output params
			isDouble[index] = par.getDatatype().equals(ColumnType.DOUBLE);
			values[index] = par.getName();
			index += 1;
		}
		if (header) pw.println(composeCSVLine(delimiter, true, values));
		int lineNr = 0;
	
		List<Result> runs = db.getResults(id.longValue());
		for (Result result : runs) {
			GeneralRow input = result.getParameterComb().getValues();
			for (int i=0;i<input.size();++i) {
				String v = input.getLocalizedString(i);
				if (v == null && !nullString.equals("<nothing>")) v = nullString;
				values[i] = isDouble[i] ? replaceSign(v, decimalSign) : v;
			}
			for (Row row : result.getAllRows()) {
				ai.aitia.meme.MEMEApp.LONG_OPERATION.progress(lineNr++);
				for (int i=0;i<row.size();++i) {
					String v = row.getLocalizedString(i);
					if (v == null && !nullString.equals("<nothing>")) v = nullString;
					values[i+input.size()] = isDouble[i+input.size()] ? replaceSign(v, decimalSign) : v;
				}
				pw.println(composeCSVLine(delimiter, true, values));
			}
		}
		pw.flush();
		pw.close();
	}

	//-------------------------------------------------------------------------------
	/** Replaces all occurances of '.' with 'decimalSign' in 'text'. */
	private String replaceSign(String text, String decimalSign) {
		if (text == null || decimalSign.equals(".")) return text;
		return text.replace(".", decimalSign);
	}
	
	//-------------------------------------------------------------------------------
	// [Model thread]
	/** Returns the (model_id,name_version) pairs of the models belongs to 'results'. */
	private Object[] getResultNames(IExportPluginContext ctx, ArrayList<Long> results) throws Exception {
		if (ctx == null || results == null)
			throw new IllegalArgumentException();
		Object[] ans = new Object[results.size()];
		AbstractResultsDb db = ctx.getResultsDb();
		List<Model> models = db.getModelsAndVersions();
		ai.aitia.meme.MEMEApp.LONG_OPERATION.progress(0,models.size());
		int index = 0;
		for (int i=0;i<models.size();++i) {
			ai.aitia.meme.MEMEApp.LONG_OPERATION.progress(i);
			long model_id = models.get(i).getModel_id();
			if (results.contains(new Long(model_id))) {
				String name = models.get(i).getName();
				String version = models.get(i).getVersion();
				Item<Long> item = new Item<Long>(new Long(model_id),name + "_" + version);
				ans[index++] = item;
			}
		}
		return ans;
	}
}
