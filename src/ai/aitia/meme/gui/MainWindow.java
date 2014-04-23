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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;

import org.jvnet.substance.SubstanceLookAndFeel;
//import org.nlogo.app.HelpMenu;


import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.UserPrefs;
import ai.aitia.meme.database.ConnChangedEvent;
import ai.aitia.meme.database.IConnChangedListener;
import ai.aitia.meme.events.HybridAction;
import ai.aitia.meme.events.IHybridActionListener;
import ai.aitia.meme.events.IProgramStateChangeListener;
import ai.aitia.meme.events.ProgramState;
import ai.aitia.meme.events.ProgramStateChangeEvent;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.paramsweep.PS_ModelWizard;
import ai.aitia.meme.paramsweep.PS_Monitor;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.PS_ModelWizard.RunningMode;
import ai.aitia.meme.paramsweep.gui.MonitorConfigurationDialog;
import ai.aitia.meme.paramsweep.gui.VCloudDownloader;
import ai.aitia.meme.paramsweep.gui.WizardPreferences;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.utils.Utilities.CancelImportException;
import ai.aitia.meme.paramsweep.utils.Utilities.IEBinary;
import ai.aitia.meme.pluginmanager.IDialogPlugin;
import ai.aitia.meme.pluginmanager.IExportPlugin;
import ai.aitia.meme.pluginmanager.IImportPlugin;
import ai.aitia.meme.pluginmanager.PluginInfo;
import ai.aitia.meme.pluginmanager.PluginManager.PluginList;
import ai.aitia.meme.usertools.UserToolGroup;
import ai.aitia.meme.usertools.UserToolManager;
import ai.aitia.meme.usertools.UserToolParser;
import ai.aitia.meme.usertools.UserToolGroup.UserToolItem;
import ai.aitia.meme.usertools.UserToolParser.UserToolParserException;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.TestableDialog;
import ai.aitia.meme.utils.Utils;

/** Logical representation of the main window. It is not a GUI-component itself, but
 *  contains the GUI-components that forms the main window.
 */
public class MainWindow implements IProgramStateChangeListener, IHybridActionListener, 
									  java.awt.event.ActionListener,
									  java.awt.event.ItemListener,
									  javax.swing.event.HyperlinkListener,
									  IConnChangedListener {
	
	private JFrame jFrame = null;  //  @jve:decl-index=0:visual-constraint="6,12"
	private JMenuBar jJMenuBar = null;
	private JMenu fileMenu = null;
	private JMenu helpMenu = null;
	private JMenuItem exitMenuItem = null;
	private JMenuItem aboutMenuItem = null;
	private JMenuItem logMenuItem = null;
	private JMenuItem userManMenuItem = null;
	private JMenuItem tutorialMenuItem = null;
	private JMenuItem dbSettingsMenuItem = null;
	private JMenu ImportMenu = null;
	private JMenu ExportMenu = null;
	private JMenu ViewsMenu = null;
	private JMenuItem ViewsCreateMenuItem = null;
	private JMenu PSMenu = null;
	private JMenu ChartsMenu = null;
	private JMenu WindowMenu = null;
	private ButtonGroup WindowsRadioButtonGroup = null;  //  @jve:decl-index=0:
	private JRadioButtonMenuItem WindowsTabbedRadioButtonMenuItem = null;
	private JRadioButtonMenuItem WindowsMDIRadioButtonMenuItem = null;
	private JRadioButtonMenuItem WindowsDockableRadioButtonMenuItem = null;
	private JEditorPane aboutContentPane = null;

	private ResultsPanel resultsPanel = null; 
	private ViewsPanel viewsPanel = null; 
	private ChartsPanel chartsPanel = null;
	/** The user manual documentation. */
	private java.io.File userManFile = new java.io.File(MEMEApp.g_AppDir, "Documents/MEME_Manual.pdf");
	/** The tutorial documentation. */
	private java.io.File tutorialFile = new java.io.File(MEMEApp.g_AppDir, "Documents/MEME_Tutorial.pdf");


	private UserToolManager userToolManager = UserToolManager.newInstance();
	
	private JMenu toolsMenu = null;
	private JMenu groupsMenu = null; // dynamic
	private JMenuItem CSVExportSettingsMenuItem = null;
	private JMenuItem configureToolsMenuItem = null;
	private JMenuItem defineEnvironmentVariablesItem = null;
	private JCheckBoxMenuItem verboseModeMenuItem = null;
	private static int[] numberKeys = { KeyEvent.VK_0, KeyEvent.VK_1,
										KeyEvent.VK_2, KeyEvent.VK_3,
										KeyEvent.VK_4, KeyEvent.VK_5,
										KeyEvent.VK_6, KeyEvent.VK_7,
										KeyEvent.VK_8, KeyEvent.VK_9, };
	private String autoRunClass;
	private String[] autoRunClassPath;
	private boolean autoRunnable = false;
	
	/************************************************************************
	 * 																		*
	 *						Controller facilities							*
	 * 																		*
	 ************************************************************************/
	
	/** Enum type for representing menus with dynamic menu items. */
	static enum DynamicMenu {
		DIALOG_PLUGIN, LOOK_AND_FEEL
	};
	
	/** The panel layout manager. */
	public MainWindowPanelManager panelManager = null;
	public final ResultsTree resultsTreeModel; 
	public HybridAction importBtn	= new HybridAction(this, "Import...", "import.png", Action.SHORT_DESCRIPTION, "Add recordings to the database");
	public HybridAction exportBtn	= new HybridAction(this, "Export...", "export.png", Action.SHORT_DESCRIPTION, "Export the selected item from the database");
	public HybridAction dbSettings	= new HybridAction(this, "Database settings", null, Action.SHORT_DESCRIPTION, "Connect to a different database");
	public HybridAction loadSession = new HybridAction(this, "Load session", null, Action.SHORT_DESCRIPTION, "Load database settings from a file.");
	public HybridAction saveSession	= new HybridAction(this, "Save session", null, Action.SHORT_DESCRIPTION, "Save current database settings to a file.");
	public HybridAction analysis	= new HybridAction(this, "Analysis...", null, Action.SHORT_DESCRIPTION, "Send selected items to an external analysis software");

	public HybridAction runSimulation		= new HybridAction(this, "Run simulation...", "expl_wizard.png", HybridAction.SHORT_DESCRIPTION, "Start the Parameter Sweep Wizard to run a simulation");
	public HybridAction monitorSimulation 	= new HybridAction(this, "Monitor simulation", "expl_monitor.png", HybridAction.SHORT_DESCRIPTION, "Monitor the running simulation on a specified grid");
	public HybridAction prefSimulation 		= new HybridAction(this, "Wizard preferences...", null, Action.SHORT_DESCRIPTION, "Settings of the Parameter Sweep Wizard");
	public HybridAction monitorConfig		= new HybridAction(this, "Monitor preferences...", null, Action.SHORT_DESCRIPTION, "Settings of the Parameter Sweep Monitor");
	public HybridAction cloudDownload		= new HybridAction(this,"Download results...","download.png",Action.SHORT_DESCRIPTION,"Download experiment results from the Model Exploration Server");
	private Properties serverProperties = null;
	
	
	/** Contoller object. Its getValue() method returns the number of the selected models. */
	public final WhenNrOfSelectedModels	whenNrOfSelectedModels;
	/** Controller object. Its getValue() method returns whether one or more view is selected or not. */
	public final WhenAViewIsSelected		whenAViewIsSelected;
	/** Controller object. Its getValue() method returns whether one or more view is selected or not. */
	public final WhenMonitorIsEnabled 		whenMonitorIsEnabled;
	/** Parameter object for representing the active panel. */
	public final WhenDownloaderIsEnabled 	whenDownloaderIsEnabled;
	public ProgramState.Parameter<JPanel>	activePanel = null;

	public MainWindow() {
		resultsTreeModel = new ResultsTree();

		// ID of this ProgramState-parameter: 
		//   "ai.aitia.meme.gui.MainWindow$WhenNrOfSelectedModels"
		whenNrOfSelectedModels	= MEMEApp.PROGRAM_STATE.registerParameter(new WhenNrOfSelectedModels());
		whenAViewIsSelected		= MEMEApp.PROGRAM_STATE.registerParameter(new WhenAViewIsSelected());
		whenMonitorIsEnabled	= MEMEApp.PROGRAM_STATE.registerParameter(new WhenMonitorIsEnabled());
		whenDownloaderIsEnabled = MEMEApp.PROGRAM_STATE.registerParameter(new WhenDownloaderIsEnabled());
		
	}
	
	public void scanCustomGroups() {
		userToolManager.scanCustomGroups();
		int index = getActiveGroup();
		generateToolsMenu(index,verboseModeMenuItem.isSelected());
	}
	
	/** Controller class.  Its getValue() method returns the number of the selected models. */
	public class WhenNrOfSelectedModels extends ProgramState.CompCtrl<Integer> {
		public Integer getValue() {
			if (resultsPanel != null && activePanel.getValue() == resultsPanel) {
				ResultsBrowser br = getResultsBrowser();
				return (br == null) ? 0 : br.getSelectedModels().length;
			}
			return 0;
		}
	}
	
	/** Controller class. Its getValue() method returns whether one or more view is selected or not. */
	public class WhenAViewIsSelected extends ProgramState.CompCtrl<Boolean> {
		public Boolean getValue() {
			if (viewsPanel != null && activePanel.getValue() == viewsPanel) {
				Object selectedView = (viewsPanel == null) ? null : viewsPanel.getSelectedView();
				return (selectedView != null);
			}
			return Boolean.FALSE;
		}
	}
	
	/** Controller class. Its getValue() method returns true if no remote monitor is running and the database connection
	 *  is live.
	 */
	public class WhenMonitorIsEnabled extends ProgramState.CompCtrl<Boolean> {
		public Boolean getValue() {
			return MEMEApp.getDatabase().getConnection() != null && !panelManager.hasAliveMonitor();
		}
	}
	
	public class WhenDownloaderIsEnabled extends ProgramState.CompCtrl<Boolean> {
		public Boolean getValue() {
			return MEMEApp.getDatabase().getConnection() != null && !panelManager.hasAliveDownloader();
		}
	}

	/** Callback function class that starts the appropriate import plugin with
	 *  the files specified by <code>arg</code>.
	 */
	public class AutomaticImporter implements IEBinary<String[],PlatformType> {
		public void run(String[] arg1, PlatformType platform)  throws Exception { // TODO: finish
			switch (platform) {
			case REPAST 	: runOther(arg1,PlatformType.REPAST); break;
			case EMIL		: runOther(arg1,PlatformType.EMIL); break;
			case TRASS		: runOther(arg1,PlatformType.TRASS); break;
			case CUSTOM 	: runOther(arg1,PlatformType.CUSTOM); break;
			case SIMPHONY	: runOther(arg1,PlatformType.SIMPHONY); break;
			case SIMPHONY2	: runOther(arg1,PlatformType.SIMPHONY2); break;
			case MASON		: runOther(arg1,PlatformType.MASON); break;
			case NETLOGO5	:
			case NETLOGO	: runNetLogo(arg1); break;
			default : throw new Exception("Unsupported platform: " + PlatformManager.getPlatform(platform).getDisplayableName());
			}
		}
		
		private void runOther(String[] arg, PlatformType platform) throws Exception {
			DialogPluginContext ctx = new DialogPluginContext(null);
			int settingsIdx = -1;
			String settingsFile = null;
			for (int i = 0;i < arg.length && settingsIdx == -1;i++) {
	            if (arg[i].trim().endsWith(".settings.xml"))
	            	settingsIdx = i;
            }
			if (settingsIdx != -1) {
				settingsFile = arg[settingsIdx];
				ctx.put("SETTINGS", settingsFile);
				//removing the xml file from the array:
				String[] arg2 = new String[arg.length-1];
				for (int i = 0;i < arg.length - 1;i++) 
	                arg2[i] = i < settingsIdx ? arg[i] : arg[i+1];
				arg = arg2;
			}
			String files = Utils.join(Arrays.asList(arg),";");
			ctx.put("FILES",files);
			IImportPlugin importPlugin = findImportPlugin(platform);
			if (importPlugin == null) {
				String name = "";
				switch (platform) {
				case REPAST		: name = "RepastJ"; break;
				case TRASS		:
				case EMIL		: name = "EMIL-S"; break;
				case CUSTOM		: name = "Custom Java"; break;
				case MASON		: name = "MASON"; break;
				case SIMPHONY2	: name = "Repast Simphony 2"; break;
				case SIMPHONY  	: name = "Repast Simphony"; break;
				}
				throw new IllegalStateException(name + " import plugin is missing.");
			}
			importPlugin.showDialog(ctx);
			Object exitCode = ctx.get("EXIT_CODE");
			if (exitCode == null) 
				throw new Exception();
			else if (!"OK".equals(exitCode.toString()))
				throw new CancelImportException();
		}
		
		private void runNetLogo(String[] arg) throws Exception {
			DialogPluginContext ctx = new DialogPluginContext(null);
			int settingsIdx = -1;
			String settingsFile = null;
			for (int i = 0;i < arg.length && settingsIdx == -1;i++) {
	            if (arg[i].trim().endsWith(".settings.xml"))
	            	settingsIdx = i;
            }
			if (settingsIdx != -1) {
				settingsFile = arg[settingsIdx];
				ctx.put("SETTINGS",settingsFile);
				//removing the xml file from the array:
				String[] arg2 = new String[arg.length - 1];
				for (int i = 0;i < arg.length - 1;i++) 
	                arg2[i] = i < settingsIdx ? arg[i] : arg[i + 1];
				arg = arg2;
			}
			String files = Utils.join(Arrays.asList(arg),";");
			ctx.put("FILES",files);
			IImportPlugin importPlugin = findNetLogoImportPlugin();
			if (importPlugin == null) 
				throw new IllegalStateException("NetLogo import plugin is missing.");
			importPlugin.showDialog(ctx);
			Object exitCode = ctx.get("EXIT_CODE");
			if (exitCode == null) 
				throw new Exception();
			else if (!"OK".equals(exitCode.toString()))
				throw new CancelImportException();
		}

	}
	
	/** Finds and returns the Repast import plugin object. If the plugin is missing,
	 *  the method returns null;
	 */
	private IImportPlugin findImportPlugin(PlatformType platform) {
		PluginList<IImportPlugin> list = MEMEApp.getPluginManager().getImportPluginInfos();
		String name = null; 
		switch (platform) {
		case REPAST		: name = "Repast"; break;
		case TRASS		: 
		case EMIL		: name = "EMIL"; break;
		case CUSTOM		: name = "CustomJava"; break;
		case MASON		: name = "Mason"; break;
		case SIMPHONY2	: name = "Simphony2"; break;
		case SIMPHONY 	: name = "Simphony"; break;
		}
		if (name == null) return null;
		String pluginName = "ai.aitia.meme.repastimport." + name + "ImportPlugin";  
		for (PluginInfo<IImportPlugin> info : list) {
			if (pluginName.equals(info.getInternalName())) 
				return info.getInstance();
		}
		return null;
	}
	
	private IImportPlugin findNetLogoImportPlugin() {
		PluginList<IImportPlugin> list = MEMEApp.getPluginManager().getImportPluginInfos();
		for (PluginInfo<IImportPlugin> info : list) {
			if ("ai.aitia.meme.netlogoimport.NetLogoImportPlugin".equals(info.getInternalName()))
				return info.getInstance();
		}
		return null;
	}
	
	/** Finds and returns the CSV export plugin object. If the plugin is missing,
	 *  the method returns null;
	 */
	public IExportPlugin findCSVExportPlugin() {
		PluginList<IExportPlugin> list = MEMEApp.getPluginManager().getExportPluginInfos();
		for (PluginInfo<IExportPlugin> info : list) {
			if ("ai.aitia.meme.csvExport.CSVExportPlugin".equals(info.getInternalName())) 
				return info.getInstance();
		}
		return null;
	}

	public void onProgramStateChange(ProgramStateChangeEvent parameters) {
		if (parameters.contains(MEMEApp.SESSION.dirty)) {
			String fname = "";
			java.io.File f = MEMEApp.SESSION.getFile();
			if (f != null) {
				String asterisk = MEMEApp.SESSION.isDirty() ? " - *" : " - ";
				fname = asterisk + f.getName();
			}
			
			String title = String.format("MEME%s - © 2006-%d Aitia International, Inc.",fname,Calendar.getInstance().get(Calendar.YEAR));
			getJFrame().setTitle(title);
		}
		int nrOfSelectedModels = whenNrOfSelectedModels.getValue();
		boolean enabled = !MEMEApp.getPluginManager().getImportPluginInfos().isEmpty()
							&& this.activePanel.getValue() == getResultsPanel() && nrOfSelectedModels <= 1;
		getImportMenu().setEnabled(enabled);
		importBtn.setEnabled(enabled);

		enabled = !MEMEApp.getPluginManager().getExportPluginInfos().isEmpty() 
					&& (nrOfSelectedModels >= 1 || whenAViewIsSelected.getValue());
		getExportMenu().setEnabled(enabled);
		exportBtn.setEnabled(enabled);
		analysis.setEnabled(false);
		monitorSimulation.setEnabled(whenMonitorIsEnabled.getValue());
		cloudDownload.setEnabled(whenDownloaderIsEnabled.getValue());
	}

	public void actionPerformed(ActionEvent e) {
		hybridAction(e, null);
	}

	@SuppressWarnings("unchecked")
	public void hybridAction(ActionEvent e, HybridAction a) {
		Object o = null;
		if (a == dbSettings) {
			boolean changed = new DbSettingsDialog().start(getJFrame());
			if (changed)
				MEMEApp.reconnect();	// Reconnect. This will notify LocalAPI, ViewsDb etc.
		} else if (a == saveSession) {
		    JFileChooser filedialog = new JFileChooser(MEMEApp.getLastDir());
		    filedialog.addChoosableFileFilter(new SimpleFileFilter("MEME session files (*.mes)"));
		    int returnVal = filedialog.showSaveDialog(getJFrame());
		    if (returnVal == JFileChooser.APPROVE_OPTION) {
		    	java.io.File f = filedialog.getSelectedFile();
		    	MEMEApp.setLastDir(f);
		    	if (!Utils.getExt(f).toLowerCase().equals("mes")) 
		    		f = new java.io.File(f.toString() + ".mes");
		    	try {
					MEMEApp.SESSION.tryToSave(f);
				} catch (IOException ex) {
					MEMEApp.userErrors(null, "Error while saving " + ex.getLocalizedMessage());
				}
		    }
		} else if (a == loadSession) {
			JFileChooser fileDialog = new JFileChooser(MEMEApp.getLastDir());
			fileDialog.setAcceptAllFileFilterUsed(false);
			fileDialog.addChoosableFileFilter(new SimpleFileFilter("MEME session files (*.mes)"));
			int returnVal = fileDialog.showOpenDialog(getJFrame());
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				java.io.File f = fileDialog.getSelectedFile();
				MEMEApp.setLastDir(f);
				MEMEApp.SESSION.setFile(f);
				try {
					MEMEApp.SESSION.load();
					MEMEApp.getDbSettings().init();
				} catch (Exception e1) {
					Logger.logException("SESSION.load()", e1);
					MEMEApp.userErrors(null, "Error while loading " + e1.getLocalizedMessage());
				}
				MEMEApp.reconnect();
			}
		} else if (a == importBtn) 
			showMenuAtButton(getImportMenu(), (java.awt.Component)e.getSource());
		else if (a == exportBtn) 
			showMenuAtButton(getExportMenu(), (java.awt.Component)e.getSource());
		else if (a == runSimulation) 
			PS_ModelWizard.createWizard(RunningMode.SERVER_RUN,null,true,new AutomaticImporter(),panelManager,serverProperties,null);
		else if (a == monitorSimulation) {
			PS_Monitor.createMonitor(getJFrame(),true,serverProperties,new AutomaticImporter(),panelManager);
		} else if (a == prefSimulation) {
			ParameterSweepWizard.setPreferences(new WizardPreferences());
			JDialog dialog = ParameterSweepWizard.getPreferences().showInDialog(getJFrame());
			dialog.setVisible(true);
		} else if (a == monitorConfig) {
			MonitorConfigurationDialog dlg = new MonitorConfigurationDialog(getJFrame(),true);
			serverProperties = dlg.showDialog();
		} else if (a == cloudDownload) {
			final VCloudDownloader panel = new VCloudDownloader(getJFrame(),panelManager,new AutomaticImporter());
			panel.putClientProperty(SubstanceLookAndFeel.TABBED_PANE_CLOSE_BUTTONS_PROPERTY,Boolean.TRUE);
			panel.putClientProperty(MainWindowPanelManager.CUSTOM_CALLBACK,new Runnable() {
				public void run() {
					panel.dispose();
					System.gc();
				}
			});
			panelManager.add(panel,"Downloader",true);
			panelManager.rebuild();
			panelManager.setActive(panel);
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {	panel.start(); }
			});
		} else if (e.getSource() == getExitMenuItem()) 
			MEMEApp.onClose(true);
		else if (e.getSource() == configureToolsMenuItem) {
			int index = getActiveGroup();
			UserToolsDialog dialog = new UserToolsDialog(getJFrame(),userToolManager,index);
			if (dialog.start())
				generateToolsMenu(index,verboseModeMenuItem.isSelected());
		} else if (e.getSource() == defineEnvironmentVariablesItem) {
			int index = getActiveGroup();
			UTEnvironmentVariableDialog dialog = new UTEnvironmentVariableDialog(getJFrame(),userToolManager,index - 1);
			dialog.showDialog();
		} else if (e.getSource() == CSVExportSettingsMenuItem) {
			IExportPlugin cp = findCSVExportPlugin();
			if (cp == null) {
				MEMEApp.userErrors("Warning","CSV Export plugin is not available.");
				return;
			}
			DialogPluginContext ctx = new DialogPluginContext(null);
			ctx.put("JUST_SETTINGS",new Object());
			cp.showDialog(ctx);
		} else if (e.getSource() == getUserManMenuItem()) 
			openDoc(userManFile);
		else if (e.getSource() == getTutorialMenuItem()) 
			openDoc(tutorialFile);
		else if (e.getSource() == getLogMenuItem()) 
			showLog();
		else if (e.getSource() == getAboutMenuItem()) {
			Object[] options = { "OK", "License agreement" };
			/*int ret = javax.swing.JOptionPane.showOptionDialog(getJFrame(), getAboutContentPane(), "About MEME", 
															   JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
															   null, options, options[0]);*/
			int ret = TestableDialog.showOptionDialog(getJFrame(), getAboutContentPane(), "About MEME", 
					   JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
					   null, options, options[0], "dial_mainwindow_about");
			if (ret == 1) 
				showLicense();
		} else if (e.getSource() == verboseModeMenuItem)
			MEMEApp.userPrefs.put(UserPrefs.USER_TOOL_VERBOSE_MODE,String.valueOf(verboseModeMenuItem.isSelected()));
		else if (null != (o = a.getValue(DynamicMenu.DIALOG_PLUGIN.name()))) {
			PluginInfo<?> info = (PluginInfo) o;
			((IDialogPlugin)(info.getInstance())).showDialog(new DialogPluginContext(a));
		}
		else if (null != (o = a.getValue(DynamicMenu.LOOK_AND_FEEL.name()))) {
			//MEMEApp.userAlert(String.format("You have selected %s %s", a.getValue(Action.NAME), o.toString()));
			MEMEApp.getLF().set((LookAndFeel.ListType) o, a.getValue(Action.NAME).toString());
		}
	}
	
	public void onConnChange(ConnChangedEvent event) {
		boolean enabled = event.getConnection() != null;
		runSimulation.setEnabled(enabled);
		setEnabled(getToolsMenu(),enabled);
		if ( enabled && autoRunnable ) {
			// this blocks
			//autoRunSimulation();
			Utils.invokeLaterNonPublic(this, "autoRunSimulation");
		}
	}
	
	private void setEnabled(JMenu menu, boolean enabled) {
		for (int i = 0;i < menu.getPopupMenu().getComponentCount()-1;++i) {
			Component c = menu.getPopupMenu().getComponent(i);
			if (!"EMPTY".equals(c.getName()))
				menu.getPopupMenu().getComponent(i).setEnabled(enabled);
		}
	}

	/** Opens the documentation 'doc'. */
	private void openDoc(final java.io.File doc) {
		MEMEApp.LONG_OPERATION.begin(String.format("Opening %s...", doc.toString()), new LongRunnable() {
			@Override public void trun() throws Exception {
				MEMEApp.getOSUtils().openDocument(doc.toURI(), doc.getParentFile());
			}
		});
	}

	public void itemStateChanged(java.awt.event.ItemEvent e) {
		if (e.getSource() == getWindowsTabbedRadioButtonMenuItem()) {
			if (getWindowsTabbedRadioButtonMenuItem().isSelected())
				panelManager.setMode(MainWindowPanelManager.GUIMode.TABBED);
		}
		else if (e.getSource() == getWindowsMDIRadioButtonMenuItem()) {
			if (getWindowsMDIRadioButtonMenuItem().isSelected())
				panelManager.setMode(MainWindowPanelManager.GUIMode.MDI);
		}
		else if (e.getSource() == getWindowsDockableRadioButtonMenuItem()) {
			if (getWindowsDockableRadioButtonMenuItem().isSelected())
				panelManager.setMode(MainWindowPanelManager.GUIMode.DOCKABLE);
		} 
	}

	//-------------------------------------------------------------------------
	/** Shows a view table.
	 * Looks for the specified view primarily by view_id, if not found then by name.
	 * Returns false if the view is not found.
	 */
	public boolean showViewTable(Long view_id, String viewName) {
		panelManager.setActive(getViewsPanel());
		return getViewsPanel().setSelectedView(view_id, viewName);
	}


	/************************************************************************
	 * 																		*
	 *								 Content Area							*
	 * 																		*
	 ************************************************************************/

	public JFrame getJFrame() {
		if (jFrame == null) {
			jFrame = new JFrame();
			panelManager = new MainWindowPanelManager(jFrame);
			activePanel  = panelManager;

			GUIUtils.setRefComp(getHelpMenu());
			MEMEApp.getLF().showHeapStatusMonitorIfPossible(jFrame.getRootPane());

			// Create the panels
			chartsPanel = new ChartsPanel();	// creation order is important: chartsPanel uses nothing
			resultsPanel= new ResultsPanel();	// resultsPanel uses MainWindow only
			viewsPanel	= new ViewsPanel();		// viewsPanel uses each of them
			
			// Create menus
			jFrame.setJMenuBar(getJJMenuBar());

			// Initialize panels layout
			panelManager.add(resultsPanel,"Results");
			panelManager.add(viewsPanel,  "Views");
			panelManager.add(chartsPanel, "Charts");
			panelManager.setEnabled(chartsPanel, false);
			panelManager.setActive(resultsPanel);

			MainWindowPanelManager.GUIMode m = MainWindowPanelManager.GUIMode.TABBED; 
			panelManager.setMode(m);
			//panelManager.rebuild();

			getWindowsTabbedRadioButtonMenuItem()  .setSelected(m == MainWindowPanelManager.GUIMode.TABBED);
			getWindowsMDIRadioButtonMenuItem()     .setSelected(m == MainWindowPanelManager.GUIMode.MDI);
			getWindowsDockableRadioButtonMenuItem().setSelected(m == MainWindowPanelManager.GUIMode.DOCKABLE);

			// Set up GUI control

			MEMEApp.SESSION.dirty.addWeakListener(this);
			activePanel.addListener(this);
			whenNrOfSelectedModels.addListener(this);
			whenAViewIsSelected.addListener(this);
			whenMonitorIsEnabled.addListener(this);
			setEnabled(getToolsMenu(),false);

			//	Enable/disable menu items and toolbar buttons initially
			// (this triggers onProgramStateChange())
			whenAViewIsSelected.fireLater();	
			MEMEApp.SESSION.dirty.fireLater();

			MEMEApp.getDatabase().connChanged.addWeakListener(resultsTreeModel);

			jFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			jFrame.addWindowListener(new java.awt.event.WindowAdapter() {
				@Override
				public void windowClosing(java.awt.event.WindowEvent e) {
					if (MEMEApp.onClose(true))
						resultsPanel.onClose();
				}
			});
			jFrame.setIconImage(getIcon("MEME.png").getImage());
			jFrame.pack();
		}
		return jFrame;
	}

	/** Returns the results browser component. */
	public ResultsBrowser getResultsBrowser() {	return (resultsPanel == null) ? null : resultsPanel.getResultsBrowser(); }
	public ResultsPanel getResultsPanel() {	return resultsPanel; }
	public ViewsPanel getViewsPanel() {	return viewsPanel; }
	public ChartsPanel getChartsPanel() { return chartsPanel; }
	
	/** Sets the GUI to busy/unbusy according to the value of the parameter. When a GUI
	 *  is busy, changes the icon, and doesn't response any mouse motion and click event.
	 */
	public void setBusy(boolean b) {
		GUIUtils.setBusy(getJFrame(), b);
	}
	
	/** Returns the icon specified by 'name'. If the method hasn't find the icon, 
	 *  it return null.
	 */ 
	public static javax.swing.ImageIcon getIcon(String name) {
		String path = "icons/" + name;
		java.net.URL imgURL = MainWindow.class.getResource(path);
		if (imgURL != null)
			return 	new ImageIcon(imgURL);
		Logger.logError("Cannot find icon: " + path);
		return null;
	}

	
	/************************************************************************
	 * 																		*
	 *								About window							*
	 * 																		*
	 ************************************************************************/

	private JEditorPane getAboutContentPane() {
		if (aboutContentPane == null) {
			aboutContentPane = new JEditorPane();
			aboutContentPane.setEditable(false);
			aboutContentPane.setBorder(null);
			aboutContentPane.addHyperlinkListener(this);
			try {
				java.io.InputStream is = MEMEApp.class.getResourceAsStream("gui/icons/about/about.html");
				java.io.StringWriter s = new java.io.StringWriter();
				if (is != null) 
					Utils.copyRW(new java.io.InputStreamReader(is), s);
				String htmlPage = s.toString();
				htmlPage = htmlPage.replace("$VER$", MEMEApp.CURRENT_VERSION);
				htmlPage = htmlPage.replace("<body>", Utils.htmlBody()+"jvm:"+System.getProperty("java.version")+"<br>");
				htmlPage = htmlPage.replace("src=\"", "src=\"gui/icons/about/");
				GUIUtils.setTextPane(aboutContentPane, htmlPage);
			} catch (Exception e) {
				Logger.logException("MainWindow.getAboutContentPane()", e);
			}
			GUIUtils.setWrapLength(aboutContentPane, GUIUtils.dluX(350, aboutContentPane));
		}
		return aboutContentPane;
	}

	public void hyperlinkUpdate(HyperlinkEvent e) { 
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			if (e instanceof javax.swing.text.html.HTMLFrameHyperlinkEvent) {
				((javax.swing.text.html.HTMLDocument)
				((javax.swing.text.JTextComponent)e.getSource())
				.getDocument())
				.processHTMLFrameHyperlinkEvent((javax.swing.text.html.HTMLFrameHyperlinkEvent) e); 
			} else {
				try {
					MEMEApp.getOSUtils().openDocument(e.getURL().toURI(), new java.io.File(MEMEApp.g_AppDir));
				} catch (Exception ex) {
					MEMEApp.uncaughtException(null, null, ex); 
				}
			}
		}
	}
	
	/** Shows the license file. */
	private void showLicense() {
		java.net.URL url = MEMEApp.class.getResource("gui/icons/about/license.txt");
		GUIUtils.SPMSAEditorPane message;
		try { 
			message = new GUIUtils.SPMSAEditorPane(url);
		} catch (Exception e) { 
			Logger.logException("MainWindow.showLicense()", e);
			return;
		}
		message.setEditable(false);
		message.setPreferredSize(new Dimension(GUIUtils.dluX(380), Integer.MAX_VALUE));
		javax.swing.JScrollPane sp = new javax.swing.JScrollPane(message);
		sp.setMaximumSize(new Dimension(GUIUtils.dluX(400), GUIUtils.getRelScrH(80)));
		javax.swing.JOptionPane.showMessageDialog(getJFrame(), sp, "MEME License", JOptionPane.PLAIN_MESSAGE);
	}
	
	/** Shows the log. */
	private void showLog() {
		try {
			if (MEMEApp.getLogFile() == null) 				
				throw new Exception("Log file not found.");
				java.net.URL url = MEMEApp.getLogFile().toURI().toURL();
				GUIUtils.SPMSAEditorPane message = new GUIUtils.SPMSAEditorPane(url);
				message.setEditable(false);
				message.setPreferredSize(new Dimension(GUIUtils.dluX(380), Integer.MAX_VALUE));
				javax.swing.JScrollPane sp = new javax.swing.JScrollPane(message);
				sp.setMaximumSize(new Dimension(GUIUtils.dluX(400), GUIUtils.getRelScrH(80)));
				javax.swing.JOptionPane.showMessageDialog(getJFrame(), sp, "MEME log", JOptionPane.PLAIN_MESSAGE);
		} catch (Exception e) {
			Logger.logException("MainWindow.showLog()",e);
			MEMEApp.userErrors("Error",Utils.getLocalizedMessage(e));
		}
	}

	/************************************************************************
	 * 																		*
	 *								MENU 									*
	 * 																		*
	 ************************************************************************/
	

	public void generateToolsMenu(int selectedGroup, boolean verboseMode) {
		toolsMenu.removeAll();
		configureToolsMenuItem = new JMenuItem("Configure User Tools...");
		configureToolsMenuItem.setName("menuitem_tools_configure");
		configureToolsMenuItem.addActionListener(this);
		toolsMenu.add(configureToolsMenuItem);
		defineEnvironmentVariablesItem = new JMenuItem("Define Environment Variables...");
		defineEnvironmentVariablesItem.addActionListener(this);
		defineEnvironmentVariablesItem.setName("menuitem_tools_define");
		toolsMenu.add(defineEnvironmentVariablesItem);
		if (findCSVExportPlugin() != null) {
			CSVExportSettingsMenuItem = new JMenuItem("Change CSV Export Settings...");
			CSVExportSettingsMenuItem.addActionListener(this);
			CSVExportSettingsMenuItem.setName("menuitem_tools_csvexport");
			toolsMenu.add(CSVExportSettingsMenuItem);
		}
		groupsMenu = new JMenu("Change Group");
		groupsMenu.setName("menuitem_tools_groups");
		List<UserToolGroup> groups = userToolManager.getGroups();
		for (int i = 0;i < groups.size();++i) {
			JRadioButtonMenuItem item = new JRadioButtonMenuItem(makeAction(groups.get(i),i + 1));
			groupsMenu.add(item);
			item.setName("menuitem_tools_group".concat(Integer.toString(i+1)));
			if (i + 1 == selectedGroup)
				item.setSelected(true); 
		}
		toolsMenu.add(groupsMenu);
		verboseModeMenuItem = new JCheckBoxMenuItem("Verbose mode",verboseMode);
		verboseModeMenuItem.setName("menuitem_tools_verbose");
		verboseModeMenuItem.addActionListener(this);
		toolsMenu.add(verboseModeMenuItem);
		toolsMenu.addSeparator();
		UserToolGroup group = userToolManager.getGroup(selectedGroup - 1);
		List<UserToolItem> items = group.getTools();
		if (items.size() == 0) {
			JMenuItem emptyItem = new JMenuItem("(Empty)");
			emptyItem.setName("EMPTY");
			emptyItem.setEnabled(false);
			toolsMenu.add(emptyItem);
		} else {
			for (int i = 0;i < items.size();++i) {
				JMenuItem menuItem = new JMenuItem(makeAction(items.get(i),group,i + 1));
				menuItem.setName("menuitem_tools_usertool".concat(Integer.toString(i+1)));
				toolsMenu.add(menuItem);
			}
		}
	}
	
	@SuppressWarnings("serial")
	public Action makeAction(final UserToolGroup group, final int no) {
		return new AbstractAction() {
			{
				putValue(NAME,group.toString());
				putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(numberKeys[no % 10],KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK,true));
				putValue("INDEX",no);
			}
			
			public void actionPerformed(ActionEvent e) {
				int index = (Integer) getValue("INDEX");
				MEMEApp.userPrefs.put(UserPrefs.USER_TOOL_GROUP_INDEX,String.valueOf(index));
				generateToolsMenu(index,verboseModeMenuItem.isSelected());
			}
		};
	}
	
	@SuppressWarnings("serial")
	public Action makeAction(final UserToolItem item, final UserToolGroup group, final int no) {
		return new AbstractAction() {
			{
				putValue(NAME,item.getMenuText());
				putValue(ACCELERATOR_KEY,KeyStroke.getKeyStroke(numberKeys[no %10],KeyEvent.CTRL_DOWN_MASK,true));
				putValue("ITEM",item);
				putValue("GROUP",group);
			}
			
			public void actionPerformed(ActionEvent e) {
				UserToolItem item = (UserToolItem) getValue("ITEM");
				UserToolGroup group = (UserToolGroup) getValue("GROUP");
				UserToolParser parser = new UserToolParser(MainWindow.this,item,group,verboseModeMenuItem.isSelected());
				try {
					parser.execute();
				} catch (UserToolParserException e1) {
					MEMEApp.userErrors("User Tool Error - " + item.getMenuText(),e1.getLocalizedMessage() + ".");
					Logger.logExceptionCallStack(e1);
				}
			}
		};
	}
	
	private int getActiveGroup() {
		for (int i = 0;i < groupsMenu.getPopupMenu().getComponentCount()-1;++i) {
			if (((JRadioButtonMenuItem)groupsMenu.getPopupMenu().getComponent(i)).isSelected())
				return i + 1;
		}
		return 1;
	}
	
	/** Shows menu 'menu' as a popup menu which use 'atButton' as invoker. */
	public static void showMenuAtButton(JMenu menu, java.awt.Component atButton) {
		Dimension d = atButton.getSize();
		menu.getPopupMenu().show(atButton, d.width, d.height / 2);
		menu.getPopupMenu().setInvoker(menu);
	}

	/** Make and returns a HybridAction object from a dialog plugin. */
	private HybridAction makeAction(PluginInfo<? extends IDialogPlugin<?>> info) {
		HybridAction ans = new HybridAction(this, info.getInstance().getLocalizedName(), null);
		//System.out.println(info.getInstance().getLocalizedName());
		ans.putValue(DynamicMenu.DIALOG_PLUGIN.name(), info);
		return ans;
	}

	/** Make and returns a HybridAction object from a skin/theme/watermark and a name. */
	private HybridAction makeAction(LookAndFeel.ListType t, String name) {
		HybridAction ans = new HybridAction(this, name, null);
		ans.putValue(DynamicMenu.LOOK_AND_FEEL.name(), t);
		return ans;
	}

	private JMenuBar getJJMenuBar() {
		if (jJMenuBar == null) {
			jJMenuBar = new JMenuBar();
			jJMenuBar.setName("menubar_mainwindow");
			jJMenuBar.add(getFileMenu());
			jJMenuBar.add(getViewsMenu());
			jJMenuBar.add(getChartsMenu());
			jJMenuBar.add(getPSMenu());
			jJMenuBar.add(getToolsMenu());
			jJMenuBar.add(getWindowMenu());
			jJMenuBar.add(getHelpMenu());
		}
		return jJMenuBar;
	}

	private JMenu getFileMenu() {
		if (fileMenu == null) {
			fileMenu = new JMenu();
			fileMenu.setText("File");
			fileMenu.setName("menu_mainwindow_file");
			fileMenu.add(getDbSettingsMenuItem()).setName("menuitem_file_database");
			fileMenu.add(getImportMenu()).setName("menuitem_file_import");
			fileMenu.add(getExportMenu()).setName("menuitem_file_export");
			fileMenu.add(loadSession).setName("menuitem_file_load");
			fileMenu.add(saveSession).setName("menuitem_file_save");
			fileMenu.add(getExitMenuItem()).setName("menuitem_file_exit");
		}
		return fileMenu;
	}

	private JMenu getHelpMenu() {
		if (helpMenu == null) {
			helpMenu = new JMenu();
			helpMenu.setText("Help");
			helpMenu.setName("menu_mainwindow_help");
			helpMenu.add(getTutorialMenuItem()).setName("menuitem_help_tutorial");
			helpMenu.add(getUserManMenuItem()).setName("menuitem_help_manual");
			helpMenu.addSeparator();
			helpMenu.add(getLogMenuItem()).setName("menuitem_help_log");
			helpMenu.add(getAboutMenuItem()).setName("menuitem_help_about");
		}
		return helpMenu;
	}

	private JMenuItem getUserManMenuItem() {
		if (userManMenuItem == null) {
			userManMenuItem = new JMenuItem();
			userManMenuItem.setText("MEME Manual");
			userManMenuItem.addActionListener(this);
			userManMenuItem.setEnabled(userManFile.exists() && userManFile.isFile());
		}
		return userManMenuItem;
	}

	private JMenuItem getTutorialMenuItem() {
		if (tutorialMenuItem == null) {
			tutorialMenuItem = new JMenuItem();
			tutorialMenuItem.setText("MEME Tutorial");
			tutorialMenuItem.addActionListener(this);
			tutorialMenuItem.setEnabled(tutorialFile.exists() && tutorialFile.isFile());
		}
		return tutorialMenuItem;
	}
	
	private JMenuItem getLogMenuItem() {
		if (logMenuItem == null) {
			logMenuItem = new JMenuItem("Show log");
			logMenuItem.addActionListener(this);
		}
		return logMenuItem;
	}

	private JMenuItem getExitMenuItem() {
		if (exitMenuItem == null) {
			exitMenuItem = new JMenuItem();
			exitMenuItem.setText("Exit");
			exitMenuItem.addActionListener(this);
		}
		return exitMenuItem;
	}

	private JMenuItem getAboutMenuItem() {
		if (aboutMenuItem == null) {
			aboutMenuItem = new JMenuItem();
			aboutMenuItem.setText("About");
			aboutMenuItem.addActionListener(this);
		}
		return aboutMenuItem;
	}

	private JMenuItem getDbSettingsMenuItem() {
		if (dbSettingsMenuItem == null) 
			dbSettingsMenuItem = new JMenuItem(dbSettings);
		return dbSettingsMenuItem;
	}

	private JMenu getImportMenu() {
		if (ImportMenu == null) {
			ImportMenu = new JMenu();
			ImportMenu.setText(importBtn.getValue(Action.NAME).toString());

			for (PluginInfo<IImportPlugin> info : MEMEApp.getPluginManager().getImportPluginInfos())
			{
				ImportMenu.add(new JMenuItem(makeAction(info)));
				ImportMenu.getItem(ImportMenu.getItemCount()-1).
					setName("menuitem_file_import_".concat(info.getInstance().
						getLocalizedName().toLowerCase().replace(" ", "")));
		}
		}
		return ImportMenu;
	}
	
	private JMenu getExportMenu() {
		if (ExportMenu == null) {
			ExportMenu = new JMenu();
			ExportMenu.setName("menu_mainwindow_file");
			ExportMenu.setText(exportBtn.getValue(Action.NAME).toString());

			for (PluginInfo<IExportPlugin> it : MEMEApp.getPluginManager().getExportPluginInfos())
			{
				ExportMenu.add(new JMenuItem(makeAction(it)));
				ExportMenu.getItem(ExportMenu.getItemCount()-1).
				setName("menuitem_file_export_".concat(it.getInstance().
					getLocalizedName().toLowerCase().replace(" ", "")));
		}
		}
		return ExportMenu;
	}

	private JMenu getViewsMenu() {
		if (ViewsMenu == null) {
			ViewsMenu = new JMenu();
			ViewsMenu.setText("Views");
			ViewsMenu.setName("menu_mainwindow_views");
			ViewsMenu.add(getViewsCreateMenuItem());
			ViewsMenu.getItem(ViewsMenu.getItemCount()-1).setName("menuitem_views_create");
			ViewsMenu.add(new JMenuItem(resultsPanel.loadView)).setIcon(null);
			ViewsMenu.getItem(ViewsMenu.getItemCount()-1).setName("menuitem_views_load");
			ViewsMenu.add(new JMenuItem(getViewsPanel().saveView)).setIcon(null);
			ViewsMenu.getItem(ViewsMenu.getItemCount()-1).setName("menuitem_views_save");
			ViewsMenu.add(new JMenuItem(getViewsPanel().recreateView)).setIcon(null);
			ViewsMenu.getItem(ViewsMenu.getItemCount()-1).setName("menuitem_views_recreate");
			ViewsMenu.add(new JMenuItem(getViewsPanel().renameView)).setIcon(null);
			ViewsMenu.getItem(ViewsMenu.getItemCount()-1).setName("menuitem_views_rename");
			ViewsMenu.add(new JMenuItem(getViewsPanel().deleteView)).setIcon(null);
			ViewsMenu.getItem(ViewsMenu.getItemCount()-1).setName("menuitem_views_delete");
		}
		return ViewsMenu;
	}

	JMenuItem getViewsCreateMenuItem() {
		if (ViewsCreateMenuItem == null) {
			ViewsCreateMenuItem = new JMenuItem(resultsPanel.createView);
			ViewsCreateMenuItem.setIcon(null);
		}
		return ViewsCreateMenuItem;
	}

	private JMenu getChartsMenu() {
		if (ChartsMenu == null) {
			ChartsMenu = new JMenu();
			ChartsMenu.setName("menu_mainwindow_charts");
			ChartsMenu.setText("Charts");
			ChartsMenu.add(getChartsPanel().createChart).setIcon(null);
			ChartsMenu.getItem(ChartsMenu.getItemCount()-1).setName("menuitem_charts_create");
			ChartsMenu.add(getChartsPanel().openChart).setIcon(null);
			ChartsMenu.getItem(ChartsMenu.getItemCount()-1).setName("menuitem_charts_open");
			ChartsMenu.add(getChartsPanel().exportChartAsImage).setName("menuitem_charts_export");
		}
		return ChartsMenu;
	}
	
	private JMenu getPSMenu() {
		if (PSMenu == null) {
			PSMenu = new JMenu("Parameter Sweep");
			PSMenu.setName("menu_mainwindow_paramsweep");
			PSMenu.add(runSimulation).setIcon(null);
			PSMenu.getItem(PSMenu.getItemCount()-1).setName("menuitem_paramsweep_run");
			PSMenu.add(monitorSimulation).setIcon(null);
			PSMenu.getItem(PSMenu.getItemCount()-1).setName("menuitem_paramsweep_monitorsim");
			PSMenu.addSeparator();
			PSMenu.add(prefSimulation).setIcon(null);
			PSMenu.getItem(PSMenu.getItemCount()-1).setName("menuitem_paramsweep_wizard");
			PSMenu.add(monitorConfig).setIcon(null);
			PSMenu.getItem(PSMenu.getItemCount()-1).setName("menuitem_paramsweep_monitorprefs");
			PSMenu.addSeparator();
			PSMenu.add(cloudDownload).setIcon(null);
			PSMenu.getItem(PSMenu.getItemCount()-1).setName("menuitem_paramsweep_download");
			runSimulation.setEnabled(false);
			monitorSimulation.setEnabled(false);
			cloudDownload.setEnabled(false);
			MEMEApp.getDatabase().connChanged.addListener(this);
		}
		return PSMenu;
	}

	private JMenu getWindowMenu() {
		if (WindowMenu == null) {
			WindowMenu = new JMenu();
			WindowMenu.setText("Window");
			WindowMenu.setName("menu_mainwindow_window");
			
			JMenu m = new JMenu("Layout");
			m.setName("menuitem_window_layout");
			WindowMenu.add(m);
				WindowsRadioButtonGroup = new ButtonGroup(); 
				WindowsRadioButtonGroup.add(getWindowsTabbedRadioButtonMenuItem());
				WindowsRadioButtonGroup.add(getWindowsMDIRadioButtonMenuItem());
				WindowsRadioButtonGroup.add(getWindowsDockableRadioButtonMenuItem());
				m.add(getWindowsTabbedRadioButtonMenuItem());
				m.getItem(m.getItemCount()-1).setName("menuitem_window_tabbed");
				m.add(getWindowsMDIRadioButtonMenuItem());
				m.getItem(m.getItemCount()-1).setName("menuitem_window_mdi");
				m.add(getWindowsDockableRadioButtonMenuItem());
				m.getItem(m.getItemCount()-1).setName("menuitem_window_dockable");
				
			if (MEMEApp.getLF() != null) {
				WindowMenu.addSeparator();
				makeLFMenu(WindowMenu, "Skin", LookAndFeel.ListType.SKIN);
				makeLFMenu(WindowMenu, "Theme", LookAndFeel.ListType.THEME);
				makeLFMenu(WindowMenu, "Watermark", LookAndFeel.ListType.WATERMARK);
			}
		}
		return WindowMenu;
	}

	/** Creates the Skin/Theme/Watermark submenu. */
	private JMenu makeLFMenu(JMenu parent, String menuName, LookAndFeel.ListType t) {
		java.util.ArrayList<String> names = new java.util.ArrayList<String>(MEMEApp.getLF().getAll(t));
		if (names.isEmpty())
			return null;
		java.util.Collections.sort(names);
		names.add(0, LookAndFeel.DEFAULT_DISPLAYNAME);

		String current = MEMEApp.getLF().getCurrent(t);
		if (!names.contains(current))
			current = LookAndFeel.DEFAULT_DISPLAYNAME;
		JMenu ans = new JMenu(menuName);
		ans.setName("menuitem_window_".concat(menuName).toLowerCase());
		ButtonGroup bg = new ButtonGroup();
		for (String name : names) {
			JRadioButtonMenuItem mi = new JRadioButtonMenuItem(makeAction(t, name));
			if (name.equals(current))
				mi.setSelected(true);
			bg.add(mi);
			ans.add(mi);
			mi.setName("menuitem_window_".concat(menuName).toLowerCase().concat("_button").concat(Integer.toString(ans.getItemCount())));
		}
		parent.add(ans);
		return ans;
	}


	private JRadioButtonMenuItem getWindowsTabbedRadioButtonMenuItem() {
		if (WindowsTabbedRadioButtonMenuItem == null) {
			WindowsTabbedRadioButtonMenuItem = new JRadioButtonMenuItem();
			WindowsTabbedRadioButtonMenuItem.setText("Tabbed");
			WindowsTabbedRadioButtonMenuItem.addItemListener(this);
		}
		return WindowsTabbedRadioButtonMenuItem;
	}

	private JRadioButtonMenuItem getWindowsMDIRadioButtonMenuItem() {
		if (WindowsMDIRadioButtonMenuItem == null) {
			WindowsMDIRadioButtonMenuItem = new JRadioButtonMenuItem();
			WindowsMDIRadioButtonMenuItem.setText("MDI");
			WindowsMDIRadioButtonMenuItem.addItemListener(this);
		}
		return WindowsMDIRadioButtonMenuItem;
	}

	private JRadioButtonMenuItem getWindowsDockableRadioButtonMenuItem() {
		if (WindowsDockableRadioButtonMenuItem == null) {
			WindowsDockableRadioButtonMenuItem = new JRadioButtonMenuItem();
			WindowsDockableRadioButtonMenuItem.setText("Dockable");
			WindowsDockableRadioButtonMenuItem.setEnabled(false);
			WindowsDockableRadioButtonMenuItem.addItemListener(this);
		}
		return WindowsDockableRadioButtonMenuItem;
	}
	
	private JMenu getToolsMenu() {
		if (toolsMenu == null) {
			toolsMenu = new JMenu("Tools");
			toolsMenu.setName("menu_mainwindow_tools");
			String index_str = MEMEApp.userPrefs.get(UserPrefs.USER_TOOL_GROUP_INDEX,"1");
			int index;
			try {
				index = Integer.parseInt(index_str);
				if (index > 10) 
					index = 10;
			} catch (NumberFormatException e) { index = 1; }
			String verbose_str = MEMEApp.userPrefs.get(UserPrefs.USER_TOOL_VERBOSE_MODE,"false");
			boolean verbose = Boolean.parseBoolean(verbose_str);
			generateToolsMenu(index,verbose);
		}
		return toolsMenu;
	}
	
 
	public synchronized void setAutoRunParams(String clz, String[] classPath) {
		autoRunClass = clz;
		autoRunClassPath = classPath;
		autoRunnable = true;
	}
	
	@SuppressWarnings("unused")
	private void autoRunSimulation() {
		PS_ModelWizard.createWizard(RunningMode.SERVER_RUN,autoRunClassPath,true,new AutomaticImporter(),panelManager,null,autoRunClass);
		// prevent auto-run on next connection change
		autoRunnable = false;
	}
}
