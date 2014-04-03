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
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.concurrent.Callable;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;

import org.w3c.dom.Element;

import ai.aitia.chart.util.Utilities;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.ViewRec;
import ai.aitia.meme.events.HybridAction;
import ai.aitia.meme.gui.MainWindow.DynamicMenu;
import ai.aitia.meme.gui.lop.ITRunnable;
import ai.aitia.meme.gui.lop.UserBreakException;
import ai.aitia.meme.pluginmanager.IDialogPlugin;
import ai.aitia.meme.pluginmanager.IExportPlugin;
import ai.aitia.meme.pluginmanager.PluginInfo;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.XMLUtils;
import ai.aitia.meme.viewmanager.ViewCreationDialog;
import ai.aitia.meme.viewmanager.ViewCreationRule;

/**
 * The Views Panel in the main window: displays available views + toolbar buttons. 
 */
public class ViewsPanel extends JPanel implements java.awt.event.ActionListener,
												  java.awt.event.MouseListener,
												  ai.aitia.meme.events.IProgramStateChangeListener,
												  ai.aitia.meme.events.IHybridActionListener
{
	private static final long serialVersionUID = 1L;

	private ViewsBrowser browser = null;
	private JButton jViewsExportButton = null;
	private JToolBar jToolBar = null;
	private JPopupMenu viewsContextMenu = null;
	private JMenu exportContextMenu = null;

	
	//=========================================================================
	//	Public interface

	public HybridAction createView		= new HybridAction(this, "Create a view...", "view_create.png", HybridAction.SHORT_DESCRIPTION, "Create a new view from the selected view(s)."); 
	public HybridAction recreateView	= new HybridAction(this, "Recreate view", "view_recreate.png", HybridAction.SHORT_DESCRIPTION, "Edit creation rules of the first selected view table");
	public HybridAction renameView		= new HybridAction(this, "Rename view", "view_rename.png", HybridAction.SHORT_DESCRIPTION, "Rename the selected view table");
	public HybridAction deleteView		= new HybridAction(this, "Delete view", "view_delete.png", HybridAction.SHORT_DESCRIPTION, "Delete the selected view table(s) from the database permanently");
	public HybridAction saveView		= new HybridAction(this, "Save view...", "view_save.png", HybridAction.SHORT_DESCRIPTION, "Save the selected view table(s) to script(s)");
	
	//-------------------------------------------------------------------------
	public ViewsPanel() {
		super();
		browser = new ViewsBrowser(true,true);
		browser.getJViewsList().addMouseListener(this);
		initialize();

		MainWindow mw = MEMEApp.getMainWindow();
		mw.activePanel.addWeakListener(this);
		mw.whenAViewIsSelected.addWeakListener(this);
		mw.whenNrOfSelectedModels.addWeakListener(this);	// ez azert kell h. amikor a Results lapon
					// tortentek miatt MainWindow.exportBtn-t engedelyezik, akkor nalunk lefusson a tiltas.
	}

	//-------------------------------------------------------------------------
	/** Returns the last selected view. */
	public ViewRec getSelectedView() {
		return browser.getSelectedView(); 
	}
	
	//-------------------------------------------------------------------------
	/** Returns the selected views. */
	public ViewRec[] getSelectedViews() {
		return browser.getSelectedViews();
	}
	
	//-------------------------------------------------------------------------
	/** Returns the number of rows of the last selected view. */
	public long getNrOfRowsOfSelectedView() {
		return browser.getNrOfRowsOfSelectedView();
	}

	//-------------------------------------------------------------------------
	/**
	 * Looks for the specified view primarily by view_id, if not found then by name.
	 * Returns false if the view is not found.
	 */
	public boolean setSelectedView(Long view_id, String name) {
		return browser.setSelectedView(view_id, name);
	}

	//-------------------------------------------------------------------------
	/** See ViewsBrowser.getColumnsOfSelectedView() */
	public Columns getColumnsOfSelectedView() {
		return browser.getColumnsOfSelectedView();
	}

	
	
	//=========================================================================
	//	Controller methods

	//-------------------------------------------------------------------------
	/** This method is public as an implementation side effect. Do not call or override. */
	public void actionPerformed(ActionEvent e) {
		hybridAction(e, null);
	}

	//-------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public void hybridAction(ActionEvent e, HybridAction a)
	{	
		Object o = null;
		if (a == deleteView) {
			ViewRec[] selectedViews = getSelectedViews();
			if (selectedViews == null) return;
			String msg[] = new String[2];
			if (selectedViews.length == 1)
				msg[0] = "The following view will be deleted: " + selectedViews[0].getName();
			else
				msg[0] = "All selected ("+ selectedViews.length + ") views will be deleted.";
			msg[1] =  "Are you sure?";
			int res = JOptionPane.showConfirmDialog(this, msg, "Delete view", JOptionPane.YES_NO_OPTION);
			if (res == JOptionPane.YES_OPTION) {
				String taskName = selectedViews.length == 1 ? selectedViews[0].getName() : "";
				MEMEApp.LONG_OPERATION.begin("Deleting view " + taskName, new Runnable() {
					ViewRec[] views = browser.getSelectedViews();
					public void run() {
						if (views != null) {
							for (ViewRec view : views) MEMEApp.getViewsDb().deleteView(view);
						}
					}
				});
			}
		} else if (a == createView) {
			new ViewCreationDialog().start(getSelectedViews());
		} else if (a == recreateView) {
			new ViewCreationDialog().start(browser.currentRule);
		} else if (a == renameView) {
			ViewRec selectedView = getSelectedView();
			if (selectedView == null) return;
			String newName = (String) JOptionPane.showInputDialog(this,"Please define the new name: ","Rename View - " + selectedView.getName(),JOptionPane.PLAIN_MESSAGE,null,
									  null,selectedView.getName());
			if (newName != null && !"".equals(newName.trim()) && !selectedView.getName().equals(newName.trim()))
				renameView(selectedView,newName.trim());	
		} else if (a == saveView) {
			final ViewRec[] selectedViews = getSelectedViews();
			if (selectedViews == null) return;
			File file = null;
			if (selectedViews.length == 1) file = openSaveDialog(selectedViews[0]);
			else file = openSaveDialog(null);
			if (file == null) return;
			final File f = file;
			try {
				GUIUtils.setBusy(MEMEApp.getAppWnd(),true);
				MEMEApp.LONG_OPERATION.execute2("Save views...", new ITRunnable() {
					public void trun() throws Exception {
						try {
							MEMEApp.LONG_OPERATION.progress(0,selectedViews.length);
							for (int i=0;i<selectedViews.length;++i) {
								MEMEApp.LONG_OPERATION.setTaskName("Save " + selectedViews[i]);
								MEMEApp.LONG_OPERATION.progress(i);
								ViewCreationRule rule = MEMEApp.getViewsDb().getRule(selectedViews[i].getViewID());
								Element node = rule.transformRuleToSave();
								rule.restoreRule(node,selectedViews[i].getViewID());
								File ff = null;
								if (f.isDirectory())
									ff = new File(f.getPath() + File.separator + selectedViews[i].getName() + ".xml");
								else
									ff = f;
								XMLUtils.write(ff,node);
							}
						} catch (UserBreakException e) {
							GUIUtils.setBusy(MEMEApp.getAppWnd(),false);
							return;
						}
					}
				});
			} catch (Exception e1) {
				MEMEApp.logExceptionCallStack("Saving view script", e1);
				throw new IllegalStateException(e1);
			} finally {
				GUIUtils.setBusy(MEMEApp.getAppWnd(),false);
			}
		} else if (null != (o = a.getValue(DynamicMenu.DIALOG_PLUGIN.name()))) {
			PluginInfo<?> info = (PluginInfo)o;
			((IDialogPlugin)(info.getInstance())).showDialog(new DialogPluginContext(a));
		}
	}

	//-------------------------------------------------------------------------
	public void onProgramStateChange(ai.aitia.meme.events.ProgramStateChangeEvent parameters) {
		MainWindow mw = MEMEApp.getMainWindow();
		boolean isViewsPanelActive = (mw.activePanel.getValue() == this);
		createView  .setEnabled(isViewsPanelActive && mw.whenAViewIsSelected.getValue());
		recreateView.setEnabled(isViewsPanelActive && mw.whenAViewIsSelected.getValue());
		deleteView  .setEnabled(isViewsPanelActive && mw.whenAViewIsSelected.getValue());
		saveView	.setEnabled(isViewsPanelActive && mw.whenAViewIsSelected.getValue());
		renameView  .setEnabled(isViewsPanelActive && mw.whenAViewIsSelected.getValue());
		if (jViewsExportButton != null)
			jViewsExportButton.setEnabled(isViewsPanelActive && mw.exportBtn.isEnabled());
		
		getExportContextMenu().setEnabled(!MEMEApp.getPluginManager().getExportPluginInfos().isEmpty() 
										  && mw.whenAViewIsSelected.getValue());

	}

	//-------------------------------------------------------------------------
	/** This method handles the double-click events of the views list. It opens
	 *  the recreate view wizard.
	 */ 
	public void mouseClicked(MouseEvent e) {
		if (!javax.swing.SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 2)
			return;
		try {
			MEMEApp.LONG_OPERATION.execute("Waiting for refresh...", new Runnable() {
				// Do nothing, just wait for the update of the currentRule
				public void run() {}
			});
		} catch (Exception e1) {}
		new ViewCreationDialog().start(browser.currentRule);
	}

	//-------------------------------------------------------------------------
	/** This method shows the context menu of the views list. It is public because
	 *  of implementation side effect. Do not call or override.
	 */
	public void mouseReleased(MouseEvent e) {
		if (!javax.swing.SwingUtilities.isRightMouseButton(e))
			return;
		if (e.getComponent().isEnabled() && e.getSource() == browser.getJViewsList()) {
			final int index = browser.getJViewsList().locationToIndex(e.getPoint());
			
			if (!Utilities.contains(browser.getJViewsList().getSelectedIndices(),index))
				browser.getJViewsList().setSelectedIndex(index);
			
			getViewsContextMenu().show(e.getComponent(),e.getX(),e.getY());
		}
	}

	//-------------------------------------------------------------------------
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}


	
	//=========================================================================
	// private methods

	//-------------------------------------------------------------------------
	/** Displays a file dialog that enable for the user to save the script of the
	 *  view 'vr'. Returns the selected file. 'vr' == null indicates that the user 
	 *  want ot save more views. In this case s/he would select only a directory. The
	 *  script files will be created into this directory with default names.
	 */ 
	private File openSaveDialog(ViewRec vr) {
		JFileChooser chooser = new JFileChooser();
		String fileName = "";
		File dir = MEMEApp.getLastDir();
		if (dir != null) fileName += dir.getPath();
		if (vr == null) {
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		} else {
			if (!fileName.equals("")) fileName += File.separator;
			fileName += vr.getName().trim() + ".xml";
			chooser.addChoosableFileFilter(new SimpleFileFilter("MEME View Script files (*.xml)"));
		}
		if (!fileName.equals("")) chooser.setSelectedFile(new File(fileName));
		int returnVal = chooser.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			MEMEApp.setLastDir(chooser.getSelectedFile());
			return chooser.getSelectedFile();
		}
		return null;
	}
	
	//-------------------------------------------------------------------------
	/** Make and returns a HybridAction object from a dialog plugin. */
	private HybridAction makeAction(PluginInfo<? extends IDialogPlugin<?>> info) {
		HybridAction ans = new HybridAction(this, info.getInstance().getLocalizedName(), null);
		ans.putValue(DynamicMenu.DIALOG_PLUGIN.name(), info);
		return ans;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void renameView(final ViewRec selectedView, final String newName) {
		GUIUtils.setBusy(MEMEApp.getAppWnd(),true);
		try {
			Boolean existed = (Boolean) MEMEApp.LONG_OPERATION.execute("Checking duplicate names...",new Callable<Object>() {
				public Object call() {
					MEMEApp.LONG_OPERATION.progress(0,-1);
					Long id = MEMEApp.getViewsDb().findView(newName);
					return id != null;
				}
			});
			if (existed) {
				MEMEApp.userAlert(String.format("The name %s is already used.",newName));
				return;
			}
			Boolean success = (Boolean) MEMEApp.LONG_OPERATION.execute("Renaming view...",new Callable<Object>() {
				public Object call() {
					MEMEApp.LONG_OPERATION.progress(0,-1);
					return MEMEApp.getViewsDb().renameView(selectedView.getViewID(),selectedView.getName(),newName);
				}
			});
			if (!success)
				MEMEApp.userErrors(null,"The renaming operation is failed.",MEMEApp.seeTheErrorLog("%s"));
		} catch (Exception e) {
			MEMEApp.logException("ViewsPanel.renameView()",e,true);
			MEMEApp.userErrors(null,String.format("Exception occured during the operation: %s",Utils.getLocalizedMessage(e)),
							   MEMEApp.seeTheErrorLog("%s"));
		} finally {
			GUIUtils.setBusy(MEMEApp.getAppWnd(),false);
		}
	}

	//=========================================================================
	//	GUI (View) methods

	private void initialize() {
		this.setLayout(new BorderLayout());
		this.add(browser, BorderLayout.CENTER);
		this.add(getJToolBar(), BorderLayout.NORTH);
		GUIUtils.bind(browser.getJViewsList(), null, "DELETE", deleteView);
	}

	private JToolBar getJToolBar() {
		if (jToolBar == null) {
			jToolBar = new JToolBar();
			jToolBar.add(createView);
			jToolBar.add(recreateView);
			jToolBar.add(MEMEApp.getMainWindow().getResultsPanel().loadView);
			jToolBar.add(saveView);
			jToolBar.add(renameView);
			jToolBar.add(deleteView);
			jToolBar.add(MEMEApp.getMainWindow().getChartsPanel().createChart);
			//jToolBar.add(MEMEApp.getMainWindow().analysis);
			jViewsExportButton = jToolBar.add(MEMEApp.getMainWindow().exportBtn);
			GUIUtils.showTextOnToolbar(jToolBar, true);
		}
		return jToolBar;
	}
	
	private JPopupMenu getViewsContextMenu() {
		if (viewsContextMenu == null) {
			viewsContextMenu = new JPopupMenu();
			viewsContextMenu.add(createView);
			viewsContextMenu.add(recreateView);
			viewsContextMenu.add(MEMEApp.getMainWindow().getResultsPanel().loadView);
			viewsContextMenu.add(saveView);
			viewsContextMenu.add(renameView);
			viewsContextMenu.add(deleteView);
			viewsContextMenu.addSeparator();
			viewsContextMenu.add(MEMEApp.getMainWindow().getChartsPanel().createChart);
			viewsContextMenu.add(getExportContextMenu());
		}
		return viewsContextMenu;
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
}  //  @jve:decl-index=0:visual-constraint="10,10"
