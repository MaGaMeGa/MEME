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
package ai.aitia.meme;

import java.awt.IllegalComponentStateException;
import java.awt.Component;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;

import ai.aitia.meme.database.DatabaseConnection;
import ai.aitia.meme.database.DatabaseSettings;
import ai.aitia.meme.database.DefaultSQLDialect;
import ai.aitia.meme.database.LocalAPI;
import ai.aitia.meme.database.MysqlSQLDialect;
import ai.aitia.meme.database.SQLDialect;
import ai.aitia.meme.database.ViewsDb;
import ai.aitia.meme.events.LastDirectoryChanger;
import ai.aitia.meme.events.ProgramState;
import ai.aitia.meme.gui.LookAndFeel;
import ai.aitia.meme.gui.MainWindow;
import ai.aitia.meme.gui.lop.LOPConsumer;
import ai.aitia.meme.gui.lop.LOPProducer;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.pluginmanager.IPlugin;
import ai.aitia.meme.pluginmanager.PluginManager;
import ai.aitia.meme.utils.OSUtils;
import ai.aitia.meme.utils.TestableDialog;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.OSUtils.OSType;
import ai.aitia.visu.globalhandlers.UserBreakException;

/**
 * The "main" class of the application.
 * It provides routines for application-wide initialization and finalization
 * (start-up and shut-down) and common place for global (singleton) objects.
 * 
 */
public class MEMEApp
{
	/** Current version of the application. */
	public static final String		CURRENT_VERSION		= "2.2.0";	

	public static final int			EXIT_CODE_NORMAL		= 0;
	public static final int			EXIT_CODE_CMDLINE_ERROR	= 1;
	public static final int			EXIT_CODE_OUTOFMEMORY	= 2;
	public static final int			EXIT_CODE_UNSUPP_JAVA	= 3;


	// Do not change the order of the following three elements
	/** Flag that determines whether the application is a release version (e.g. jar mode) or debug version. */
	private static boolean			g_IsJARmode = false;
	/** Base URL for inside-label html pages. */
	public static java.net.URL			g_baseURL	= null;			//!< base URL for inside-label html pages
	/** The directory of the application. It must be an absolute path. */
	public static final String		g_AppDir	= getAppDir();	//!< It must be an absolute path
	/** The last selected directory in any file dialog of the application. */
	private static File 			g_LastDir	= null;
	// Note: other classes use this object at static initialization time (e.g. ColumnType)
	/** The database connection. */
	private static final DatabaseConnection	g_database 		= new DatabaseConnection();

	/** The settings of the database connection. */
	private static final DatabaseSettings		g_dbSettings	= new DatabaseSettings();
	/** The Results database. */
	private static LocalAPI						g_LAPI			= null;
	/** The Views database. */
	private static ViewsDb						g_ViewsDb		= null;
	/** The main window of the application (the representation of the main window,
	 *  it is not a graphical component). */
	private static MainWindow					g_AppWindow		= null;
	/** The manager object that handles the plugins of the application. */
	private static PluginManager				g_PluginManager	= null;
	/** The look&feel object. */
	private static LookAndFeel					g_lf			= null;

	/** The handler object that execute the potential long operations. */ 
	public static final LOPProducer			LONG_OPERATION	= new LOPProducer();
	/** The worker object of the potential long operations. */
	public static final LOPConsumer			MODEL_THREAD	= new LOPConsumer(LONG_OPERATION);
	/** The center of the application's own eventhandler mechanism. */
	public static final ProgramState		PROGRAM_STATE	= ProgramState.SINGLETON;
	/** Session object for storing user settings. */
	public static final Session				SESSION			= new Session();
	/** Reference to the MEME related registry entries. */
	public static final Preferences			userPrefs		= Preferences.userNodeForPackage(MEMEApp.class);

	public static boolean 					WAITING_USER_TOOL = false;
	
	//-------------------------------------------------------------------------
	//	Getter methods

	public static File											getLogFile()			{ return Logger.g_logFileLoc; }
	public static File											getLastDir()			{ return g_LastDir; }
	public static DatabaseConnection							getDatabase()			{ return g_database; }
	public static ai.aitia.meme.database.IResultsDbMinimal		getResultsDbMinimal()	{ return g_LAPI; }
	public static ai.aitia.meme.database.AbstractResultsDb		getResultsDb()			{ return g_LAPI; }
	//static ai.aitia.meme.database.LocalAPI					getLocalAPI()			{ return g_LAPI; }
	public static ViewsDb										getViewsDb()			{ return g_ViewsDb; }
	public static MainWindow									getMainWindow()			{ return g_AppWindow; }
	/** Returns whether the application is a debug version (e.g. it isn't in jar mode) or not. */ 
	public static boolean										isDebugVersion()		{ return !g_IsJARmode; }
	public static PluginManager									getPluginManager()		{ return g_PluginManager; }
	public static DatabaseSettings								getDbSettings()			{ return g_dbSettings; }
	/** Returns the object that handle properly the OS related tasks. */
	public static OSUtils										getOSUtils()			{ return OSUtils.getActual(); }
	public static LookAndFeel									getLF()					{ return g_lf; }
	/** Returns whether the application uses Substance look&feel or not. */  
	public static boolean										isSubstance()			{ return (g_lf != null && g_lf.isSubstance()); }

	//-------------------------------------------------------------------------
	/**
	 * The main entry point of the application.
	 * 
	 * @param args
	 */
	@SuppressWarnings("all")
	public static void main(String[] args) {
		/************************************
		 * Before-GUI
		 ************************************/
		
		if (isDebugVersion()) {
			boolean assertsEnabled = false;
			assert assertsEnabled = true; // this should make it true - INTENTIONAL side effect!
			if (!assertsEnabled)
				throw new RuntimeException("Asserts should be enabled! (java -ea)");
		} else {
			String tmp = userPrefs.get(UserPrefs.LOGFILELOC, "MEME.log");
			Logger.g_logFileLoc = new java.io.File(tmp);
			if (!Logger.g_logFileLoc.isAbsolute()) {
				Logger.g_logFileLoc = new java.io.File(g_AppDir, Logger.g_logFileLoc.toString());
			}
		}

		Logger.logError(""); // initializing log
		LastDirectoryChanger.add(MEMEApp.class);

		// Parse command line arguments

		Utils.CMDLINEARGS_EXIT_CODE = EXIT_CODE_CMDLINE_ERROR;
		LinkedList<String> nonFatalErrors = new LinkedList<String>();
		String positional[] = {}, cmdlinespec[] = {
				"", "Usage: MEMEApp [options] [session.mes]",
				"", "Available options:",
				"", "  --help -h -? /? /h   These options show this help.",
				"", "--help -h -? /? /h",
				"positional", "0 1",		// we accept 0..1 positional arguments (filename.mes)
				"-test", "",				// name of a class providing 'static void test(String[] args)'
				"-guitest", "",				// name of a class providing 'static void test(String[] args)'
				"-model", "",				//  
				"-fablesLib", ""//,
//				"-modelclasspath",""
		};
		java.util.Map<String, Object> switches = Utils.cmdLineParse(args, cmdlinespec);

		String nonguitest = null, guitest = null;
		if (switches.containsKey("error"))
			nonFatalErrors.add(String.format("Command line error: %s", switches.get("error").toString()));
		else {
			Utils.cmdLineArgsExit(switches);	// print usage help to stderr, if asked
			positional = (String[])switches.get("positional");
			nonguitest = switches.get("-test").toString();
			//guitest = "ai.aitia.meme.viewmanager.Page_Columns";
			//guitest = "ai.aitia.meme.viewmanager.Page_Sorting";
			//guitest = "ai.aitia.meme.viewmanager.ViewCreation";
			//guitest = "ai.aitia.meme.viewmanager.ViewCreationDialog";
			guitest = switches.get("-guitest").toString();
		}

		// Arrange for catching uncaught errors in all threads (especially in EDT).
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				MEMEApp.uncaughtException("uncaughtException", t, e);
			}
		});
		// Catch errors during modal dialogs, too
		SwingExceptionHandler.install();

		// No-gui tests are runned before everything (in the Model thread)
		if (runtest(nonguitest, positional))
			return;


		/************************************
		 * Load Look & Feel
		 ************************************/
		
		ai.aitia.meme.utils.GUIUtils.g_baseURL = g_baseURL;

		// See java.awt.Window.setLocationByPlatform()
		System.setProperty("java.awt.Window.locationByPlatform", "true");

		// Be able to continue even if our custom Look & Feel fails to initialize.   
		try { g_lf = new LookAndFeel(); g_lf.init(); }
		catch (Throwable t) { Logger.logException(t); }

		// Here we could display a splash screen, showing progress at the bottom 
		// (loading plugins, connecting database, etc.)..
		
		// !!!!! MODIFY !!!!!
		
		if (!ApplicationInstanceManager.registerInstance()) {
			System.err.println("Another instance of this application is already running.  Exiting.");
			System.exit(0);
		}
		ApplicationInstanceManager.setApplicationInstanceListener(new ApplicationInstanceListener() {

			
			//====================================================================================================
			// implemented interfaces
			
			//----------------------------------------------------------------------------------------------------
			public void newInstanceCreated() {
				Logger.logError("New instance of MEME detected");
				if (MEMEApp.g_AppWindow != null)
					MEMEApp.g_AppWindow.getJFrame().toFront();
			}
		});

//		if (System.getProperty("java.version").startsWith("1.7")){
//			//MEMEApp.userErrors("Unsupported Java Version 1.7", "Please use Java 1.6 to run MEME!");
//			int askUser = MEMEApp.askUser(false, "Unsupported Java Version 1.7", "It is better to use Java 1.6 to run MEME. You can continue with Java 1.7, but certain features (e.g. charting) will not work.\n\nDo you want to continue using MEME with Java 1.7?");
//			if (askUser != 1){
//				System.exit(EXIT_CODE_UNSUPP_JAVA);
//			}
//		}
		

		clearTempDir();
		
		if (getOSUtils().getOSType() == OSType.WINDOWS) {
			if (Utilities.isCompatibleWithMaskedShellFolderManager()) {
				// To evade the slow file dialog bug
				try {
					Class folderManagerClass = Class.forName("com.t4l.win.MaskedShellFolderManager");
					folderManagerClass.newInstance();
					Method m = Toolkit.class.getDeclaredMethod("setDesktopProperty",new Class[] { String.class, Object.class });
					m.setAccessible(true);
					m.invoke(Toolkit.getDefaultToolkit(),new Object[] { "Shell.shellFolderManager",folderManagerClass });
				} catch (Throwable t) {
					Logger.logException("Failed to initialized MaskedShellFolderManager",t);
				}
			} else {
				// see PS_ModelWizard.initialze() method for explanation
			}
		}
		
		// !!!!! END OF MODIFY !!!!!

		/************************************
		 * Initialize services - before plugins
		 ************************************/

		// These objects can be observed by GUI components & plugins 
		g_LAPI = new LocalAPI();
		g_database.connChanged.addListener(g_LAPI);

		g_ViewsDb = new ViewsDb();
		g_database.connChanged.addListener(g_ViewsDb);

		/************************************
		 * Load plugins
		 ************************************/

		// To simplify the GUI-creation, we load the plugins before. 
		// (Because plugins can have effects on the GUI, e.g. 
		//  File/Import, File/Export menus).

		g_PluginManager = new PluginManager();
		g_PluginManager.setPluginsDir(new File(g_AppDir, "Plugins").getAbsolutePath());
		try { g_PluginManager.scanPlugins(new DefaultOnPluginLoadContext()); }
		catch (Exception e) { nonFatalErrors.add(e.getLocalizedMessage()); }


		/************************************
		 * GUI Start
		 ************************************/

		// Create the main window first..
		try {
			javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					g_AppWindow = new MainWindow();
					
					g_AppWindow.getJFrame().setName("wnd_main");
					
					LONG_OPERATION.setParent(g_AppWindow.getJFrame());
				}
			});
		} catch (InvocationTargetException e) {
			Logger.logExceptionCallStack("MainWindow+JFrame creation", e.getCause());
			onClose(false);
		} catch (InterruptedException e) {
			onClose(false);
		}

		// ..then display it asynchronously (we can continue here while EDT works on it)
		Utils.invokeLater(getMainWindow().getJFrame(), "setVisible", true);
		getMainWindow().scanCustomGroups();

		/************************************
		 * Initialize services - after plugins
		 ************************************/

		ai.aitia.visu.globalhandlers.GlobalHandlers.setExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				MEMEApp.uncaughtException("visu uncaught exception", t, e);
			}
		});
		ai.aitia.visu.globalhandlers.GlobalHandlers.setLOPExecutor(new LongOpSupportForVISU());

    	// Register the default SQL dialect, which will try to serve all DBMS.
		SQLDialect.register(new MysqlSQLDialect());
		// Further SQLDialects should be registered _before_ this one (e.g. from plugins).
		SQLDialect.register(new DefaultSQLDialect());

		// Initialize OS-specific routines (e.g. opening a PDF doc).
		// This must _follow_ plugin initialization because plugins
		// may provide additional implementations. 
		OSUtils.init();

		if (positional.length == 1) {
			// Load session settings

			File f = new File(positional[0]);
			try { f = f.getCanonicalFile(); } catch (Exception e) {};
			if (!f.exists()) {
				nonFatalErrors.add("Error while loading MEME session: cannot find file");
				nonFatalErrors.add('"' + f.toString() + "\".");
				nonFatalErrors.add("The file will be created when exiting from the program.");
			}
			SESSION.setFile(f);
		} else if (positional.length > 1) {
			nonFatalErrors.add("too many arguments! For usage information, use --help.");
		}

		try {
			SESSION.load();
			getDbSettings().init();
		} catch (Exception e) {
			if (SESSION.getFile() == null)			// the user's registry is corrupted
				Logger.logException("SESSION.load()", e);
			else
				nonFatalErrors.add(String.format("Error while opening file %s (%s)", 
						SESSION.getFile().toString(), Utils.getLocalizedMessage(e)));
		}


		// ..display initialization errors in a dialog

		if (!nonFatalErrors.isEmpty()) {
			// Display the messages
			userErrors("Initialization error" + (nonFatalErrors.size() > 1 ? "s" : ""), nonFatalErrors.toArray());
			nonFatalErrors = null;
		}

		ParameterSweepWizard.init(Logger.g_logStream,g_AppWindow.getJFrame());

		/************************************
		 * Activation
		 ************************************/
		
		boolean activationIsOk = true;
/*!!!	ACTIVATION kikommentezve
		File nativeLibDir = new File(g_AppDir, "lib");
		if (!nativeLibDir.isDirectory()) {
			nativeLibDir = new File(g_AppDir, "3rdParty/Activation/lib");
		}
		boolean activationIsOk = false;
		try {
			Activation.setNativeLibDir(nativeLibDir);
			activationIsOk = Activation.isActivated();
			while (!activationIsOk) {
				java.awt.Component wnd = getMainWindow().getJFrame();
				byte[] actData = Activation.showMessage(wnd, Activation.Message.NOT_ACTIVATED);
				if (actData == null) break;
				activationIsOk = Activation.verifyAndActivate(actData);
				if (activationIsOk)
					Activation.showMessage(wnd, Activation.Message.SUCCESSFUL);
				else
					Activation.showMessage(wnd, Activation.Message.BAD_ACTIVATION);
			}
		} catch (Throwable t) {
			activationIsOk = false;
			logExceptionCallStack("Activation", t);
			Activation.showActivationErrorMsg(getMainWindow().getJFrame(), t);
		}
*/

		/************************************
		 * Connect to the database
		 ************************************/
		
		if (activationIsOk) {
			reconnect();
		} else {
			Utils.invokeLater(MEMEApp.class, "onClose", false);
		}
		
		// Run the GUI test, if asked, when everything is initialized
		// (even long operations' support)
		// Note: GUI tests are called from the EDT thread
		// To create a GUI test for a class X in Eclipse: 
		// - copy the qualified name of X to the clipboard (...X) 
		// - duplicate the 'Run MEME' launch, rename it to X. 
		//   Set cmdline arguments to '-guitest ...X'
		//   On the 'Common' tab, clear the 'Display in Favorites' checkbox
		// - add a 'static void test(String[])' method to X
		if (!Utils.isEmpty(guitest)) {
			Utils.invokeLaterNonPublic(MEMEApp.class, "runtest", guitest, positional);
		}
		
		// there was model data passed thru command line
		if (switches.containsKey("-model") && !switches.get("-model").equals("")) {
			String cl = (String)switches.get("-model");
			String[] cp = {};
			if (switches.containsKey("-fablesLib")) {
				String lib = (String)switches.get("-fablesLib");
				cp = new String[]{lib + "/compiler.jar", lib + "/massgui.jar", lib + "/colt.jar", lib + "/commons-logging.jar",
								  lib + "/datasourceeditor-statistics.jar", lib + "/flexdock-0.5.0.jar", lib + "/jcommon-1.0.0.jar",
								  lib + "/jfreechart-1.0.1.jar", lib + "/jmock-1.1.0RC1.jar", lib + "/junit-4.0.jar", lib + "/log4j-1.2.15.jar",
								  lib + "/looks-1.3.1.jar", lib + "/pet-visu.jar", lib + "/repast.jar", lib + "/skinlf-1.2.11.jar",
								  lib + "/substance.jar", lib + "/swingx-0.9.2.jar", lib + "/trove.jar", lib + "/visu.jar" };
			}
//			if (switches.containsKey("-modelclasspath")){
//				cp = ((String)switches.get("-modelclasspath")).split(File.pathSeparator);
//			}
			getMainWindow().setAutoRunParams(cl, cp);
		}
		/************************************
		 * Wait for exiting
		 ************************************/

		try {
			MODEL_THREAD.start();
		
		} catch (Throwable t) {
			Logger.logExceptionCallStack("MODEL_THREAD.start()", t);
		}
		System.exit(EXIT_CODE_NORMAL);
	}

	//-------------------------------------------------------------------------
	/** Connects to the database. */
	public static void reconnect() {
		LONG_OPERATION.begin("Connecting to database...", new LongRunnable() {
			@Override
			public void trun() throws Exception {
				getDatabase().connect(getDbSettings());

//				try { Thread.sleep(7000); } catch (InterruptedException e) {}
//				throw new RuntimeException("Hello, world!");
			}
		});
	}
	
	//-------------------------------------------------------------------------
	// EDT
	/** This method must be called before the application is closed. 
	 * @param ask does the application ask from the user to save the session if it is changed
	 * @return  false if the user is asked and selects Cancel (not to close), otherwise true */
	public static boolean onClose(boolean ask) {
		if (WAITING_USER_TOOL) {
			int ans = askUser(false,"MEME","There are some waiting user tool executions.",
										   "If you close the application, these executions are erased.",
										   "Are you sure?");
			if (ans <= 0)
				return false;
		}
		
		boolean saveSession = ask;
		if (ask) {
			if (SESSION.getFile() != null && SESSION.isDirty()) {
				int ans = askUser(true, "MEME", "The MEME session have been changed.",
										String.format("Do you wish to save changes to %s?", 
														SESSION.getFile().getName()));
				if (ans < 0)
					return false;
				saveSession = (ans > 0);
			}
		}

		if (saveSession) {
			try {
				SESSION.save();
			} catch (IOException e) {
				userErrors("Save error", e.getLocalizedMessage());
			}
		}

		getPluginManager().unloadPlugins(null);

		LONG_OPERATION.begin("Closing database connection...", new Runnable() {
			public void run() {
				getDatabase().shutdown();
				MODEL_THREAD.stop();
			}
		});
		return true;
	}

	//-------------------------------------------------------------------------
	/** Default (empty) implementation of the interface IPlugin.IOnPluginLoad. */
	static class DefaultOnPluginLoadContext implements IPlugin.IOnPluginLoad {
	}

	//-------------------------------------------------------------------------
	/** Implementation of the interface ILOPExecutor which is the part of the long
	 *  opertation support of the visualisation package. It uses the MEME's long
	 *  operation support in the implementation so it is a wrapper class.
	 */
	static class LongOpSupportForVISU implements ai.aitia.visu.globalhandlers.ILOPExecutor {
		public Object execute(String taskName, Object target, String methodName, Object... args) throws Exception {
			return LONG_OPERATION.execute(taskName, new java.beans.Expression(target, methodName, args));
		}
		public Object execute(String taskName, java.beans.Expression ex) throws Exception {
			return LONG_OPERATION.execute(taskName, ex);
		}
		public Object execute(String taskName, Callable<Object> r) throws Exception {
			return LONG_OPERATION.execute(taskName, r);
		}
		public void execute(String taskName, Runnable r) throws Exception {
			LONG_OPERATION.execute(taskName, r);
		}
		public void setAbortable(boolean abortable) { LONG_OPERATION.setAbortable(abortable); }
		public boolean isUserBreak() { return LONG_OPERATION.isUserBreak(); }
		public void checkUserBreak() throws UserBreakException { LONG_OPERATION.checkUserBreak(); }
		public void progress(double current) throws UserBreakException { LONG_OPERATION.progress(current); }
		public void progress(double current, double end) {LONG_OPERATION.progress(current,end);	}
	}

	//-------------------------------------------------------------------------
	/** Returns the directory of the application. It has side-effects: initializes the
	 *  g_baseURL and g_IsJARmode global variables. 
	 * 
	 */
	private static String getAppDir() {
		// A clever trick for getting the location of the jar file, which David Smiley
		// posted to the Apple java-dev mailing list on April 14, 2002.  It works on
		// most, but not all, platforms, so in case of a problem we fall back to using
		// 'user.dir'.
		String dir = System.getProperty("user.dir");	// Usually it's an absolute path
		try	{
			g_baseURL = MEMEApp.class.getResource("/ai/aitia/meme/MEMEApp.class");
		    if (g_baseURL.toString().startsWith("jar:")) {
		    	g_IsJARmode = true;
		    	String furl = g_baseURL.getFile();
		        furl = furl.substring(0, furl.indexOf('!'));
		        File f = new File(new URL(furl).getFile()).getParentFile();
		        if (f != null && f.exists())
		        	dir = f.getAbsolutePath();
		    }
		}
		catch (Exception ex)
		{
			if (g_baseURL == null)
				try { g_baseURL = new java.io.File(dir).toURI().toURL(); }
				catch (Exception e) {};
		}
		return dir;
	}

	//-------------------------------------------------------------------------
	private static boolean runtest(String name, String[] args) {
		if (name == null || name.length() == 0)
			return false;
		try {
			Class<?> testClass = Class.forName(name);
			java.lang.reflect.Method m = testClass.getMethod("test", String[].class);
			m.invoke(testClass, (Object)args);
		} catch (Throwable t) {
			if (t instanceof java.lang.reflect.InvocationTargetException)
				t = t.getCause();
			String before= "While running test " + name + ":\n ";
			String after = (isDebugVersion() || Logger.g_logFileLoc == null) ? null : 
							" \nSee this in " + Logger.g_logFileLoc;
			userErrors("Test error", (Object[])Utils.getStackTraceLines(t, before, after));
		}
		return true;
	}

	//-------------------------------------------------------------------------
	/** Returns the graphical component of the main window of the application (if any). */
	public static java.awt.Window getAppWnd() {
		return (getMainWindow() == null) ? null : getMainWindow().getJFrame();
	}

	//-------------------------------------------------------------------------
	// Note: this method may be called BEFORE the main window is created,
	// e.g. from PluginManager.scanPlugins()
	/** It shows the <code>messages</code> in a modal dialog. */ 
	public static void userAlert(Object ... messages) {
		if (messages.length == 1 && messages[0] instanceof Object[])
			messages = (Object[])messages[0];
		Logger.logError("[userAlert] %s", Utils.join("\n", messages));
		//javax.swing.JOptionPane.showMessageDialog(getAppWnd(), messages);
		TestableDialog.showMessageDialog(getAppWnd(), messages, "");
	}

	//-------------------------------------------------------------------------
	/** It shows the the <code>messages</code> in a model dialog.
	 *  Note: this method may be called BEFORE the main window is created (in this case
	 *  it is not modal)
	 * @param title the title of the dialog
	 * @param messages String or array of strings, or array of Components - see JOptionPane  
	 */
	public static void userErrors(String title, Object ... messages) {
		if (messages.length == 1 && messages[0] instanceof Object[])
			messages = (Object[])messages[0];
		if (title == null) {
			title = (messages.length > 1) ? "MEME Errors" : "MEME Error";
		}
		Logger.logError("[userErrors] %s: %s", title, Utils.join("\n", messages));
		/*javax.swing.JOptionPane.showMessageDialog(getAppWnd(), messages, title, javax.swing.JOptionPane.ERROR_MESSAGE);*/
		TestableDialog.showMessageDialog(getAppWnd(), messages, title, javax.swing.JOptionPane.ERROR_MESSAGE,"");
	}

	//-------------------------------------------------------------------------
	/** It shows a confirmation dialog. 
	 *  Return values: 1=yes, 0=no, -1=cancel or the user closed the window with the right-upper 'x'.
	 *  @param allowCancel is 'Cancel' button on the dialog or not.
	 *  @param title the title of the dialog
	 *  @param messages messages on the dialog
	 *  @return the response of the user
	 */ 
	public static int askUser(boolean allowCancel, String title, Object ... messages) {
		if (messages.length == 1 && messages[0] instanceof Object[])
			messages = (Object[])messages[0];
		if (title == null) {
			title = "Confirmation";
		}
		/*int ans = JOptionPane.showConfirmDialog(getAppWnd(), messages, title,
					allowCancel ? JOptionPane.YES_NO_CANCEL_OPTION : JOptionPane.YES_NO_OPTION);*/
		int ans = TestableDialog.showConfirmDialog(getAppWnd(), messages, title,
					allowCancel ? JOptionPane.YES_NO_CANCEL_OPTION : JOptionPane.YES_NO_OPTION, "");
		if (ans == JOptionPane.YES_OPTION)
			return 1;
		if (ans == JOptionPane.NO_OPTION)
			return 0;
		return -1;
	}
	

	//-------------------------------------------------------------------------
	/** Exception handler class that can catch any uncaught exception */ 
	public static class SwingExceptionHandler {
		/** Handles the exception <code>t</code>.
		 * @param t the exception */
		public void handle(Throwable t) {
			uncaughtException("SwingExceptionHandler", Thread.currentThread(), t);
		}
		/** 
		 * Arranges for catching uncaught exceptions in EDT threads, even if modal
		 * dialogs are active. This code uses undocumented API facilities.
		 * See {@link java.awt.EventDispatchThread.handleException()}
		 */
		static void install() {
			System.setProperty("sun.awt.exception.handler", SwingExceptionHandler.class.getName());
		}
	}

	//-------------------------------------------------------------------------
	/** Handles the uncaught exceptions. Any of the parameters may be null.
	 * @param msg a message
	 * @param t the thread where the exception is thrown
	 * @param e the exception */
	public static void uncaughtException(String msg, Thread t, Throwable e) {
//		if (e instanceof StackOverflowError && "AWT-EventQueue-0".equals(t.getName())) {
//			String name = MEMEApp.userPrefs.get(UserPrefs.LF_WATERMARK, null);
//			if (name != null) {
//				Object o = SubstanceLookAndFeel.getAllWatermarks().get(name);
//				if (o != null && (o instanceof WatermarkInfo))
//					SubstanceLookAndFeel.setCurrentWatermark(((WatermarkInfo)o).getClassName());
//			} else {
//				java.io.InputStream is = MainWindow.class.getResourceAsStream("icons/MEME_logo_big.png");
//				if (is != null) {
//					SubstanceLookAndFeel.setImageWatermarkKind(SubstanceConstants.ImageWatermarkKind.APP_CENTER);
//					SubstanceLookAndFeel.setCurrentWatermark(new SubstanceImageWatermark(is));
//					SubstanceLookAndFeel.setImageWatermarkOpacity(0.05f);
//				}
//			}
//			return;
//		}
		// A logba minden hibat kiirunk
		String threadName = t.getName();
		if ("main".equalsIgnoreCase(threadName))
			threadName = "MEME-Worker-Thread";
		else {
			if (threadNamePattern == null) 
				threadNamePattern = Pattern.compile("^Thread-[0-9]+$");
			Matcher m = threadNamePattern.matcher(threadName);
			if (m.matches())
				threadName = "Simulation-Thread";
		}
		
		Logger.logError("in Thread: %s",threadName);
		Logger.logExceptionCallStack(msg, e);
		
		if ((e instanceof NullPointerException) && "javax.swing.SwingUtilities".equals(e.getStackTrace()[0].getClassName()) &&
			 "computeIntersection".equals(e.getStackTrace()[0].getMethodName())) return;
		
		if ((e instanceof IllegalComponentStateException) && "java.awt.Component".equals(e.getStackTrace()[0].getClassName()) &&
				"getLocationOnScreen_NoTreeLock".equals(e.getStackTrace()[0].getMethodName())){
			return;
		}
		
		// A usernek csak akkor tesszuk ki a hibat, amikor jelenleg nincs kint hiba.
		// Ezzel elkeruljuk a Stack Overflow-t amikor sorozatban jonnek a hibak es 
		// a user nem gyozi leokezgatni oket.
		synchronized (errorToBeDisplayed) {
			Throwable tmp = errorToBeDisplayed[0];
			errorToBeDisplayed[0] = e;
			if (tmp != null) return;
		}
		while (e != null) {
			msg = Utils.getLocalizedMessage(e);
			if (msg != null && msg.length() > 0)
				MEMEApp.userErrors(null,"in Thread: " + threadName, msg);
			synchronized (errorToBeDisplayed) {
				if (errorToBeDisplayed[0] == e) {
					errorToBeDisplayed[0] = null;
					return;
				}
				e = errorToBeDisplayed[0];
			}
		}
	}
	private static volatile Throwable[] errorToBeDisplayed = { null }; 
	private static Pattern threadNamePattern = null;

	//-------------------------------------------------------------------------
	/** Returns the "See the error log for details" message.
	 * @param fmt format string. It must contain two %s format characters. The first 
	 * has been changed to the string "See the error log for details" and in the place of
	 * the second appeared the current location of the log file. 
	 * @return the well formed message
	 */ 
	public static String seeTheErrorLog(String fmt) {
		String fn = (Logger.g_logFileLoc == null) ? "" : '(' + Logger.g_logFileLoc.toString() + ')';
		return String.format(fmt, "See the error log for details", fn);
	}

	//-------------------------------------------------------------------------
	/** Creates a new exception with the message specified by <code>format</code>
	 *  and <code>args</code>
	 * @param format the format string of the message
	 * @param args the parameters of the format string
	 * @return the exception
	 */
	public static Exception exception(String format, Object ... args) {
    	return new Exception(Utils.formatv(format, args));		 
	}

//	//-------------------------------------------------------------------------
//    public static void log(String format, Object ... args) {
//    	System.out.println(Utils.formatv(format, args));
//    }

	//-------------------------------------------------------------------------
    /** Writes the the error to the log file that occured when method <code>method</code> is invoked.
     * @param where the place of the exception
     * @param target the target object of the method
     * @param method the method
     * @param t the exception
     */
    public static void logInvocationError(String where, Object target, java.lang.reflect.Method method, Throwable t) {
    	logInvocationError(where, target, method.getName(), t);
    }
    /** Writes the the error to the log file that occured when method named <code>methodName</code> is invoked.
     * @param where the place of the exception
     * @param target the target object of the method
     * @param methodName the name of the method
     * @param t the exception
     */public static void logInvocationError(String where, Object target, String methodName, Throwable t) {
    	Utils.Callback.log(where, target, methodName, t);
    }
    
    //-------------------------------------------------------------------------------
    
    public static void setLastDir(File dir) {
    	setLastDir_(dir);
    	LastDirectoryChanger.fireDirectoryChanged(dir,MEMEApp.class);
    }
     
    // It also sets the similar global variable in the VISU.
    public static void setLastDir_(File dir) {
		if (dir.isDirectory()) {
			g_LastDir = dir;
			ai.aitia.visu.globalhandlers.GlobalHandlers.setLastDirectory(dir);
		} else {
			java.io.File parent = dir.getParentFile();
			if (parent != null) {
				g_LastDir = parent;
				ai.aitia.visu.globalhandlers.GlobalHandlers.setLastDirectory(parent);
			}
		}
    }
    
    //---------------------------------------------------------------------------------
    private static void clearTempDir() {
    	File tempDir = new File("Temp");
    	if (tempDir.exists() && tempDir.isDirectory()) {
    		for (File f : tempDir.listFiles())
    			f.delete();
    	}
    }
}
