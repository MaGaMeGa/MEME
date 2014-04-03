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
package ai.aitia.meme.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.tree.TreePath;

import org.w3c.dom.Element;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.events.HybridAction;
import ai.aitia.meme.events.IHybridActionListener;
import ai.aitia.meme.gui.MainWindow.DynamicMenu;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.pluginmanager.IDialogPlugin;
import ai.aitia.meme.pluginmanager.IExportPlugin;
import ai.aitia.meme.pluginmanager.IImportPlugin;
import ai.aitia.meme.pluginmanager.PluginInfo;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.TestableDialog;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.XMLUtils;
import ai.aitia.meme.viewmanager.ViewCreation;
import ai.aitia.meme.viewmanager.ViewCreationDialog;
import ai.aitia.meme.viewmanager.ViewCreationRule;

/**
 * The Results Panel in the main window: consists of a Results browser + toolbar buttons. 
 */
@SuppressWarnings("serial")
public class ResultsPanel extends javax.swing.JPanel implements IHybridActionListener,
																ai.aitia.meme.events.IProgramStateChangeListener,
																MouseListener
{
	private ResultsBrowser resultsBrowser = null;
	
	private JToolBar jToolBar = null;
	private JButton jViewsExportButton = null;
	private JPopupMenu resultsContextMenu = null;
	private JMenu importContextMenu = null;
	private JMenu exportContextMenu = null;

	
	//=========================================================================
	//	Public interface

	public HybridAction deleteModel	= new HybridAction(this, "Delete", "model_delete.png", HybridAction.SHORT_DESCRIPTION, "Delete selected results from the database permanently");
	public HybridAction renameModel	= new HybridAction(this,"Rename","model_rename.png",HybridAction.SHORT_DESCRIPTION,"Rename the selected result");
	public HybridAction createView 	= new HybridAction(this, "Create a view...", "view_create.png", HybridAction.SHORT_DESCRIPTION, "Start the view table creation wizard");
	public HybridAction loadView   	= new HybridAction(this, "Load view...", "view_load.png", HybridAction.SHORT_DESCRIPTION, "Load one or more views from script");
	
	//-------------------------------------------------------------------------
	public ResultsPanel() {
		super();
		initialize();

		MainWindow mw = MEMEApp.getMainWindow();
		mw.whenNrOfSelectedModels.addWeakListener(this);
		mw.activePanel.addWeakListener(this);
		mw.whenAViewIsSelected.addWeakListener(this);	// ez azert kell h. amikor a Views lapon
			// tortentek miatt MainWindow.exportBtn-t engedelyezik, akkor nalunk lefusson a tiltas.
	}


	//-------------------------------------------------------------------------
	ResultsBrowser getResultsBrowser() {
		if (resultsBrowser == null) {
			resultsBrowser = new ResultsBrowser();
			resultsBrowser.getJTree().addMouseListener(this);
		}
		return resultsBrowser;
	}
	
	//-------------------------------------------------------------------------
	/** Releases the resources (result browser, etc.). */ 
	public void onClose() {
		if (resultsBrowser != null)
			resultsBrowser.removeMouseListener(this);
			resultsBrowser.onClose();
	}

	
	//=========================================================================
	//	Controller methods

	//-------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public void hybridAction(ActionEvent e, HybridAction a) {
		Object o = null;
		if (a == createView) {
			if (MEMEApp.getMainWindow().activePanel.getValue() == this)
				new ViewCreationDialog().start(resultsBrowser.getSelection());
			else
				new ViewCreationDialog().start(MEMEApp.getMainWindow().getViewsPanel().getSelectedViews());
		} else if (a == deleteModel) 
			deleteResults();
		else if (a == renameModel)
			renameResult();
		else if (a == loadView) 
			loadViews();
		else if (null != (o = a.getValue(DynamicMenu.DIALOG_PLUGIN.name()))) {
			PluginInfo<?> info = (PluginInfo)o;
			((IDialogPlugin)(info.getInstance())).showDialog(new DialogPluginContext(a));
		}

	}

	//-------------------------------------------------------------------------
	public void onProgramStateChange(ai.aitia.meme.events.ProgramStateChangeEvent parameters) {
		MainWindow mw = MEMEApp.getMainWindow();
		boolean thisPanelIsActive = (mw.activePanel.getValue() == this);
		boolean isViewsPanelActive = (mw.activePanel.getValue() == mw.getViewsPanel());

		createView.setEnabled((thisPanelIsActive && mw.whenNrOfSelectedModels.getValue() >= 1) ||
							  (isViewsPanelActive && mw.whenAViewIsSelected.getValue()));
		deleteModel.setEnabled(thisPanelIsActive && mw.whenNrOfSelectedModels.getValue() == 1);
		renameModel.setEnabled(thisPanelIsActive && mw.whenNrOfSelectedModels.getValue() == 1);
		if (jViewsExportButton != null)
			jViewsExportButton.setEnabled(thisPanelIsActive && mw.exportBtn.isEnabled());
		
		int nrOfSelectedModels = MEMEApp.getMainWindow().whenNrOfSelectedModels.getValue();
		boolean enabled = !MEMEApp.getPluginManager().getImportPluginInfos().isEmpty()
							&& nrOfSelectedModels <= 1;
		getImportContextMenu().setEnabled(enabled);

		enabled = !MEMEApp.getPluginManager().getExportPluginInfos().isEmpty() 
					&& nrOfSelectedModels >= 1;
		getExportContextMenu().setEnabled(enabled);
	}

	//-------------------------------------------------------------------------
	/** Deletes the selected results from the database. */
	private void deleteResults() {
		ArrayList<String[]> strings = getResultsBrowser().getSelectionAsStrings();
		assert(!strings.isEmpty());
		String what = "?";
		boolean multiple = (strings.size() > 1);
		if (strings.size() > 0) {
			String tmp[] = strings.get(0);
			switch (tmp.length) {
				case 0:
					assert false;
				case 1:
					what = String.format("model \"%s\"", tmp[0]);
					break;
				case 2:		// ide sose jut mert ilyenkor nem engedjuk kivalasztani a Delete gombot.
					what = multiple ?
							  String.format("selected versions of model \"%s\"", tmp[0])
							: String.format("version \"%2$s\" of model \"%1$s\"", tmp[0], tmp[1]);
					break;
				case 3:
				default :
					what = multiple ? 
							  String.format("selected batches of version \"%2$s\" of model \"%1$s\"", tmp[0], tmp[1])
							: String.format("batch #%3$s of version \"%2$s\" of model \"%1$s\"", tmp[0], tmp[1], tmp[2]);
					break;
			}
		}
		String msg[] = { 
				String.format("All results contained in %s", what),
		 		"will be deleted. Are you sure?"
		};
		/*int res = JOptionPane.showConfirmDialog(this, msg, "Delete results", JOptionPane.YES_NO_OPTION);*/
		int res = TestableDialog.showConfirmDialog(this, msg, "Delete results", JOptionPane.YES_NO_OPTION,"dial_mainwindow_deleteresults");
		if (res == JOptionPane.YES_OPTION) {
			MEMEApp.LONG_OPERATION.begin("Deleting results...", new LongRunnable() {
				Long[][] selection = getResultsBrowser().getSelection();
				@Override public void trun() throws Exception {
					MEMEApp.getResultsDb().deleteAll(selection);
				}
			});
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void renameResult() {
		// enabling rules of the button/menu item guarantees that the array contains exactly one Model 
		final Model selected = getResultsBrowser().getSelectedModels()[0];
		final RenameResultDialog dlg = new RenameResultDialog(MEMEApp.getMainWindow().getJFrame());
		final String[] newValues = dlg.showDialog(selected.getName(),selected.getVersion());
		if (newValues != null && (!newValues[0].equals(selected.getName()) || !newValues[1].equals(selected.getVersion()))) {
			GUIUtils.setBusy(MEMEApp.getAppWnd(),true);
			try {
				final boolean existed = (Boolean) MEMEApp.LONG_OPERATION.execute("Checking duplicate names...",new Callable<Object>() {
					public Object call() {
						MEMEApp.LONG_OPERATION.progress(0,-1);
						final Model model = MEMEApp.getResultsDb().findModel(newValues[0],newValues[1]);
						return model != null;
					}
				});
				if (existed) {
					MEMEApp.userAlert(String.format("The %s/%s model-version pair is already used.",newValues[0],newValues[1]));
					return;
				}
				final boolean success = (Boolean) MEMEApp.LONG_OPERATION.execute("Renaming result...",new Callable<Object>() {
					public Object call() {
						MEMEApp.LONG_OPERATION.progress(0,-1);
						return MEMEApp.getResultsDb().renameModel(selected.getModel_id(),selected.getName(),selected.getVersion(),newValues[0],
																  newValues[1]);
					}
				});
				if (!success)
					MEMEApp.userErrors(null,"The renaming operation is failed.",MEMEApp.seeTheErrorLog("%s"));
			} catch (Exception e) {
				MEMEApp.logException("ResultsPanel.renameResult()",e,true);
				MEMEApp.userErrors(null,String.format("Exception occured during the operation: %s",Utils.getLocalizedMessage(e)),
								   MEMEApp.seeTheErrorLog("%s"));
			} finally {
				GUIUtils.setBusy(MEMEApp.getAppWnd(),false);
			}
		}
	}
	
	//-------------------------------------------------------------------------
	/** Loads view scripts and create views from them. */
	@SuppressWarnings("unchecked")
	private void loadViews() {
		final JFileChooser chooser = new JFileChooser(MEMEApp.getLastDir());
		chooser.setMultiSelectionEnabled(true);
		chooser.addChoosableFileFilter(new SimpleFileFilter("MEME View Script files (*.xml)"));
		int returnVal = chooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			MEMEApp.setLastDir(chooser.getSelectedFiles()[0]);
			Integer errno = 0;
			try {
				GUIUtils.setBusy(MEMEApp.getAppWnd(),true);
				errno = (Integer)MEMEApp.LONG_OPERATION.execute("Processing scripts...",
																		new Callable<Object>() {
					public Object call() {
						int errorNumber = 0;
						ArrayList<File> delayed = new ArrayList<File>();
						List<File> files = java.util.Arrays.asList(chooser.getSelectedFiles());
						final int finalSize = files.size();
						int processed = 0;
						int size;
						
						do {
							size = files.size(); // size ujraszamitasa
							FILES:
							for (File f : files) {
								if (MEMEApp.LONG_OPERATION.isUserBreak()) return new Integer(-1);
//								MEMEApp.LONG_OPERATION.setTaskName(String.format("Processing scripts... (Completed %d out of %d)",processed,finalSize));
								MEMEApp.LONG_OPERATION.setTitle(String.format("Please wait... (%d/%d)",processed + 1,finalSize),null);
								MEMEApp.LONG_OPERATION.progress(processed,finalSize);
								Element root = null;
								try {
									root = XMLUtils.load(f.toURI());
								} catch (Exception e) {
									MEMEApp.logException(e);
									errorNumber += 1;
									continue;
								}
								List<Element> tables = XMLUtils.findAll(root,ViewCreationRule.INPUT_TABLE);
								for (Element table : tables) {
									String model_version = table.getAttribute(ViewCreationRule.INPUT_MODELVERSION_ATTR);
									if (model_version != null && model_version.length() != 0) { // reference to result table
										String model_name = table.getAttribute(ViewCreationRule.INPUT_MODELNAME_ATTR);
										if (model_name == null || model_name.length() == 0) {
											MEMEApp.logError("Missing model name attribute in file %s",f.getName());
											errorNumber += 1;
											processed += 1;
											if (delayed.contains(f)) delayed.remove(f);
											continue FILES;
										}
										Model model = MEMEApp.getResultsDb().findModel(model_name,model_version);
										if (model == null) {
											MEMEApp.logError("Invalid reference to model %s in file %s",model_name + "/" + model_version,f.getName());
											errorNumber += 1;
											processed += 1;
											if (delayed.contains(f)) delayed.remove(f);
											continue FILES;
										}
										table.setAttribute(ViewCreationRule.INPUT_MODELID_ATTR,String.valueOf(model.getModel_id()));
										table.removeAttribute(ViewCreationRule.INPUT_MODELNAME_ATTR);
										table.removeAttribute(ViewCreationRule.INPUT_MODELVERSION_ATTR);
									} else { // reference to view table
										String viewName = table.getAttribute(ViewCreationRule.INPUT_VIEWNAME_ATTR);
										if (viewName != null && viewName.length() != 0) {
											Long view_id = MEMEApp.getViewsDb().findView(viewName); 
											if (view_id == null) {
												delayed.add(f);
												continue FILES;
											}
											else {
												table.setAttribute(ViewCreationRule.INPUT_VIEWID_ATTR,view_id.toString());
											}
										}
									}
								}
								ViewCreationRule rule = new ViewCreationRule(root);
								ViewCreation vc = new ViewCreation(rule,null);
								try {
									System.gc();
									vc.trun();
								} catch (Exception e) {
									MEMEApp.logException("ResultsPanel.loadViews() - " + rule.getName(),e);
									errorNumber += 1;
								}
								processed += 1;
							} // for loop - FILES
							files = null;
							files = delayed;
							delayed = new ArrayList<File>();
						} while (files.size() != 0 && files.size() != size);
						if (files.size() != 0) {
							errorNumber += files.size();
							processed += files.size();
							for (File f : files)
								MEMEApp.logError("Invalid view reference in file %s",f.getName());
						}
						MEMEApp.LONG_OPERATION.setTaskName(String.format("Processing scripts... (Completed %d out of %d)",processed,finalSize));
						MEMEApp.LONG_OPERATION.progress(processed,finalSize);
						return new Integer(errorNumber);
					}
				});
			} catch (Exception e) {}
			GUIUtils.setBusy(MEMEApp.getAppWnd(),false);
			if (errno.intValue() > 0) {
				MEMEApp.userErrors("Error during the script processing",
								   "Number of wrong/unparseable scripts: " + errno.toString(),
								   MEMEApp.seeTheErrorLog("%s %s"));
			} else {
				MEMEApp.userAlert("Script processing successful!");
			}
		}
	}
	
	//----------------------------------------------------------------------------
	/** This method shows the context menu of the results tree. It is public because
	 *  of implementation side effect. Do not call or override.
	 */
	public void mouseReleased(MouseEvent e) {
		if (!javax.swing.SwingUtilities.isRightMouseButton(e))
			return;
		if (e.getComponent().isEnabled() && e.getSource() == resultsBrowser.getJTree()) {
			TreePath path = null;
			for (int i = 0;path == null && i <= e.getX();path = resultsBrowser.getJTree().getPathForLocation(i,e.getY()),i += 20);
			if (path == null)
				path = resultsBrowser.getJTree().getPathForRow(resultsBrowser.getJTree().getRowCount() - 1);
			resultsBrowser.getJTree().setSelectionPath(path);
			
			getResultsContextMenu().show(e.getComponent(),e.getX(),e.getY());
		}
	}

	//----------------------------------------------------------------------------
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	
	//----------------------------------------------------------------------------
	/** Make and returns a HybridAction object from a dialog plugin. */
	private HybridAction makeAction(PluginInfo<? extends IDialogPlugin<?>> info) {
		HybridAction ans = new HybridAction(this, info.getInstance().getLocalizedName(), null);
		ans.putValue(DynamicMenu.DIALOG_PLUGIN.name(), info);
		return ans;
	}
	
	//=========================================================================
	//	GUI (View) methods

	private void initialize() {
		this.setLayout(new java.awt.BorderLayout());
		this.add(getResultsBrowser());
		this.add(getJToolBar(), BorderLayout.NORTH);
	}

	private JToolBar getJToolBar() {
		if (jToolBar == null) {
			jToolBar = new JToolBar();
			jToolBar.add(MEMEApp.getMainWindow().importBtn);
			jToolBar.getComponent(jToolBar.getComponentCount()-1).setName("btn_mainwindow_import");
			jViewsExportButton = jToolBar.add(MEMEApp.getMainWindow().exportBtn);
			jToolBar.getComponent(jToolBar.getComponentCount()-1).setName("btn_mainwindow_export");
			jToolBar.add(renameModel);
			jToolBar.getComponent(jToolBar.getComponentCount()-1).setName("btn_mainwindow_renamemodel");
			jToolBar.add(deleteModel);
			jToolBar.getComponent(jToolBar.getComponentCount()-1).setName("btn_mainwindow_deletemodel");
			jToolBar.add(createView);
			jToolBar.getComponent(jToolBar.getComponentCount()-1).setName("btn_mainwindow_createview");
			jToolBar.add(loadView);
			jToolBar.getComponent(jToolBar.getComponentCount()-1).setName("btn_mainwindow_loadview");
			jToolBar.add(MEMEApp.getMainWindow().getChartsPanel().createChart);
			jToolBar.getComponent(jToolBar.getComponentCount()-1).setName("btn_mainwindow_createchart");
			jToolBar.add(MEMEApp.getMainWindow().runSimulation);
			jToolBar.getComponent(jToolBar.getComponentCount()-1).setName("btn_mainwindow_runsimulation");
			jToolBar.add(MEMEApp.getMainWindow().monitorSimulation);
			jToolBar.getComponent(jToolBar.getComponentCount()-1).setName("btn_mainwindow_monitorsimulation");
			jToolBar.add(MEMEApp.getMainWindow().cloudDownload);
			jToolBar.getComponent(jToolBar.getComponentCount()-1).setName("btn_mainwindow_clouddownload");
			

			//jToolBar.add(MEMEApp.getMainWindow().analysis);
			jToolBar.setAlignmentX(0.0f);
			GUIUtils.showTextOnToolbar(jToolBar, true);
		}
		return jToolBar;
	}
	
	private JPopupMenu getResultsContextMenu() {
		if (resultsContextMenu == null) {
			resultsContextMenu = new JPopupMenu();
			resultsContextMenu.setName("cmenu_mainwindow_treecmenu");
			resultsContextMenu.add(getImportContextMenu());
			resultsContextMenu.add(getExportContextMenu());
			resultsContextMenu.add(renameModel);
			resultsContextMenu.add(deleteModel);
			resultsContextMenu.addSeparator();
			resultsContextMenu.add(createView);
			resultsContextMenu.add(loadView);
			resultsContextMenu.addSeparator();
			resultsContextMenu.add(MEMEApp.getMainWindow().getChartsPanel().createChart);
			
			Component[] comps = resultsContextMenu.getComponents();
			comps[0].setName("btn_mainwindow_treecmenuimport");
			comps[1].setName("btn_mainwindow_treecmenuexport");
			comps[2].setName("btn_mainwindow_treecmenurename");
			comps[3].setName("btn_mainwindow_treecmenudelete");
			comps[5].setName("btn_mainwindow_treecmenucreateview");
			comps[6].setName("btn_mainwindow_treecmenuimportloadview");
			comps[8].setName("btn_mainwindow_treecmenuimportcreatechart");

		}
		return resultsContextMenu;
	}
	
	private JMenu getImportContextMenu() {
		if (importContextMenu == null) {
			importContextMenu = new JMenu();
			importContextMenu.setText(MEMEApp.getMainWindow().importBtn.getValue(Action.NAME).toString());
			importContextMenu.setIcon((Icon)MEMEApp.getMainWindow().importBtn.getValue(Action.SMALL_ICON));

			for (PluginInfo<IImportPlugin> info : MEMEApp.getPluginManager().getImportPluginInfos())
				importContextMenu.add(new JMenuItem(makeAction(info)));
		}
		return importContextMenu;
	}
	
	private JMenu getExportContextMenu() {
		if (exportContextMenu == null) {
			exportContextMenu = new JMenu();
			exportContextMenu.setText(MEMEApp.getMainWindow().exportBtn.getValue(Action.NAME).toString());
			exportContextMenu.setIcon((Icon)MEMEApp.getMainWindow().exportBtn.getValue(Action.SMALL_ICON));

			for (PluginInfo<IExportPlugin> it : MEMEApp.getPluginManager().getExportPluginInfos())
				exportContextMenu.add(new JMenuItem(makeAction(it)));
		}
		return exportContextMenu;
	}
}
