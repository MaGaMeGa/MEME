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
package ai.aitia.meme.paramsweep;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.skin.ModerateSkin;

import ai.aitia.meme.gui.ExpandedEditor;
import ai.aitia.meme.gui.IPanelManager;
import ai.aitia.meme.gui.Wizard;
import ai.aitia.meme.gui.Wizard.ICloseListener;
import ai.aitia.meme.paramsweep.launch.Launcher;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.Utilities.IEBinary;
import ai.aitia.meme.utils.GUIUtils;

/** The main entry point of the MASS/MEME Parameter Sweep Wizard application/module. */
public class PS_ModelWizard implements ICloseListener {

	//================================================================================
	// members
	
	/** Enumeration type for representing simulations running modes. SIMPLE_RUN
	 *  means that the simulation is run by the local computer. */
	public enum RunningMode { GENERATE_ONLY, SIMPLE_RUN, SERVER_RUN, QCG_RUN, VCLOUD_RUN };
	
	/** The current running mode of the wizard. */ 
	private RunningMode runningMode = RunningMode.SERVER_RUN;
	/** The wizard object. */
	private ParameterSweepWizard wizard = null;
	/** This object is used for synchronization purposes. */  
	private static Object stop = new Object();
	/** The main window of the wizard application. */
	private Window appWindow = null;
	/** Flag that determines whether the wizard is closed by pressing Cancel or not. */
	private boolean canceling = false;
	
	/** Callback function that starts the Repast import plugin (used only in MEME mode). */
	private IEBinary<String[],PlatformType> callback = null;
	/** Panel manager object (used only in MEME mode). */
	private IPanelManager manager = null;
	/** Properties of the monitor (used only in MEME mode). */
	private Properties monitorServerProperties = null;
	private String[] initClassPath = null;
	
	//================================================================================
	// methods
	
	//--------------------------------------------------------------------------------
	/*** Constructor.
	 * @param runningMode the initial runningMode of the wizard
	 * @param initClassPath the predefined classpath
	 */
	public PS_ModelWizard(RunningMode runningMode, String[] initClassPath) {
		this.runningMode = runningMode;
		this.initClassPath = initClassPath;
		initialize();
	}
	
	//--------------------------------------------------------------------------------
	public Window getAppWindow() { return appWindow; }
	public String[] getInitClassPath() { return initClassPath; }
 
	//--------------------------------------------------------------------------------
	public static void main(String[] args) {
		List<String> init = new ArrayList<String>();
		if (args.length == 0)
			createWizard(RunningMode.SERVER_RUN,null,false);
		else {
			RunningMode mode = RunningMode.SERVER_RUN;
			int start = 0;
			if (args[0].equals("-norun")) {
				mode = RunningMode.GENERATE_ONLY;
				start = 1;
			}
			for (int i = start;i < args.length;++i)
				init.add(args[i]);
			createWizard(mode,init.toArray(new String[0]),false);
		}
	}

	//--------------------------------------------------------------------------------
	public static void createWizard(RunningMode mode, String clz, String[] init, boolean fromMEME) {
		createWizard(mode,init,fromMEME,null,null,null,clz);
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Creates the Parameter Sweep wizard.
	 * @param mode initial running mode
	 * @param init additional class path entries  
	 * @param fromMEME flag that determines whether wizard is used by the MEME or as a
	 *        standalone application
	 */
	public static void createWizard(RunningMode mode, String[] init, boolean fromMEME) {
		createWizard(mode,init,fromMEME,null,null,null,null);
	}
	
	//--------------------------------------------------------------------------------
	/** Creates the Parameter Sweep wizard.
	 * @param mode initial running mode
	 * @param init additional class path entries  
	 * @param fromMEME flag that determines whether wizard is used by the MEME or as a
	 *        standalone application
	 * @param hasRepastPlugin true if the MEME has Repast import plugin (used only in MEME mode)
	 * @param callback callback function that starts the Repast import plugin (used only in MEME mode)
	 * @param manager panel manager object (used only in MEME mode)
	 * @param serverProperties properties of the monitor (used only in MEME mode)
	 */
	public static void createWizard(RunningMode mode, String[] init, boolean fromMEME, IEBinary<String[],PlatformType> callback, 
									IPanelManager manager, Properties serverProperties, String clz) {
		ParameterSweepWizard.setFromMEME(fromMEME);
		RunningMode newMode = fromMEME ? RunningMode.SERVER_RUN : mode;
		PS_ModelWizard mw = new PS_ModelWizard(newMode,init);
		mw.callback = callback;
		mw.manager = manager;
		mw.monitorServerProperties = serverProperties;
		if (clz != null) 
			mw.wizard.simulateBrowsePressed(clz);
		mw.start();
		if (!fromMEME) {
			synchronized(stop) {
				try {
					stop.wait();
				} catch (InterruptedException e) {}
			}
		}
		while (!mw.runSimulation()) {
			try {
				mw.reinitialize(init,true);
			} catch (Exception e) {
				Utilities.userAlert(ParameterSweepWizard.getFrame(),"Error while reinitializing the wizard.","The previous settings cannot be " +
									"restored.");
				ParameterSweepWizard.logError(Util.getLocalizedMessage(e) + "\nStacktrace:");
				e.printStackTrace(ParameterSweepWizard.getLogStream());
				try {
					mw.reinitialize(init,false);
				} catch (Exception e1) {
					ParameterSweepWizard.logError(Util.getLocalizedMessage(e1) + "\nStacktrace:");
					e1.printStackTrace(ParameterSweepWizard.getLogStream());
					if (fromMEME) {
						mw.end();
						return;
					} else System.exit(-1);
				}
			}
			mw.start();
			if (!fromMEME) {
				synchronized(stop) {
					try {
						stop.wait();
					} catch (InterruptedException e) {}
				}
			}
		}
		if (fromMEME)
			mw.end();
		else
			System.exit(0);
	}
	
	//--------------------------------------------------------------------------------
	/** Shows the MASS/MEME Parameter Sweep Wizard. */
	public void start() { appWindow.setVisible(true); }
	
	//--------------------------------------------------------------------------------
	/** Hides and releases the wizard. */
	public void end() {
		appWindow.setVisible(false);
		appWindow.dispose();
		appWindow = null;
		System.gc();
		ParameterSweepWizard.getFrame().repaint();
	}
	
	//--------------------------------------------------------------------------------
	/** Runs the (generated) model with the generated parameter file according to the 
	 *  running mode.
	 * @return true if the the simulation starts without error, false otherwise
	 */
	@SuppressWarnings("unchecked")
	public boolean runSimulation() {
		if (canceling) return true;
		Launcher launcher = new Launcher(appWindow,wizard,runningMode,manager,callback,monitorServerProperties);
		return launcher.launch();
	}
	
	//---------------------------------------------------------------------------------
	/** Sets the wizard with the last saved model settings. This method is used when
	 *  a problem occurs during the starting of the simulation. In this case after a 
	 *  warning message the user may want to modify the content of the wizard so the
	 *  application must restore the wizard.
	 */
	public void reinitialize(String[] initClassPath, boolean load, boolean platformChange) throws Exception {
		String path = null, name = null;
		File file = null;
		if (load) {
			path = ParameterSweepWizard.getPreferences().getSettingsPath();
			name = wizard.getGeneratedModelName() + ".settings.xml";
			file = new File(path + File.separator + name);
		}
		wizard.dispose();
		wizard = new ParameterSweepWizard(this,initClassPath);
		if( platformChange ) 
			wizard.gotoModelselectionPage();
		wizard.onClose.addListener(this);
		if (load)
			wizard.getWSManager().load(file.toURI());
		appWindow.setVisible(false);
		if (!ParameterSweepWizard.isFromMEME()) { 
			final JFrame frame = (JFrame) appWindow;
			
			frame.setTitle("MEME Model Wizard © 2007 - " + Calendar.getInstance().get(Calendar.YEAR) + " Aitia International, Inc.");
			frame.setContentPane(wizard);
			frame.pack();
			final Dimension oldD = frame.getPreferredSize();
			final Dimension newD = GUIUtils.getPreferredSize(frame);
			if (!oldD.equals(newD)) 
				frame.setPreferredSize(newD);
			frame.pack();
		} else {
			final JDialog dlg = (JDialog) appWindow;
			dlg.setContentPane(wizard);
			dlg.pack();
			final Dimension oldD = dlg.getPreferredSize();
			final Dimension newD = GUIUtils.getPreferredSize(dlg);
			if (!oldD.equals(newD)) 
				dlg.setPreferredSize(newD);
			dlg.pack();
			appWindow.toFront();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void reinitialize(String[] initClassPath, boolean load) throws Exception {
		reinitialize(initClassPath,load,false);
	}

	//================================================================================
	// implemented interfaces
	
	//--------------------------------------------------------------------------------
	public void onClose(Wizard w) {
		((ParameterSweepWizard)w).writeClassPathToRegistry();
		ParameterSweepWizard.getPluginManager().unloadPlugins(null);
		if (w.isCanceling()) {
			canceling = true;
			if (ParameterSweepWizard.isFromMEME()) {
				appWindow.setVisible(false);
				return;
			} else
				System.exit(0);
		}
		if (runningMode != RunningMode.GENERATE_ONLY) {
			runningMode = ParameterSweepWizard.getPreferences().isLocalRun() ? RunningMode.SIMPLE_RUN : 
						  (ParameterSweepWizard.getPreferences().isQCGRun() ? RunningMode.QCG_RUN :
						  (ParameterSweepWizard.getPreferences().isRemoteRun() ? RunningMode.SERVER_RUN : RunningMode.VCLOUD_RUN));
			if (ParameterSweepWizard.isFromMEME()) {
				appWindow.setVisible(false);
				ParameterSweepWizard.getFrame().update(ParameterSweepWizard.getFrame().getGraphics());
			} else {
				synchronized (stop) {
					stop.notify();
				}
			}
		} else {
			Utilities.userAlert(wizard,"Generation done.");
			System.exit(0);
		}
	}
	
	//================================================================================
	// private methods
	
	//--------------------------------------------------------------------------------
	private void initialize() {
		if (!ParameterSweepWizard.isFromMEME()) {
			try {
				System.setProperty("substancelaf.noExtraElements", "");
				UIManager.setLookAndFeel(new SubstanceLookAndFeel());
				SubstanceLookAndFeel.setSkin(new ModerateSkin());
				System.setProperty("sun.awt.noerasebackground","true");
			} catch (UnsupportedLookAndFeelException e1) {}
		
			String name = System.getProperty("os.name").toLowerCase();
			if (name.indexOf("windows") >= 0) {
				if (Utilities.isCompatibleWithMaskedShellFolderManager()) {
					// To evade the slow file dialog bug
					try {
						Class folderManagerClass = Class.forName("com.t4l.win.MaskedShellFolderManager");
						folderManagerClass.newInstance();
						Method m = Toolkit.class.getDeclaredMethod("setDesktopProperty",new Class[] { String.class, Object.class });
						m.setAccessible(true);
						m.invoke(Toolkit.getDefaultToolkit(),new Object[] { "Shell.shellFolderManager", folderManagerClass });
					} catch (Throwable t) {
						ParameterSweepWizard.logError("Failed to initialized MaskedShellFolderManager.");
						t.printStackTrace(ParameterSweepWizard.getLogStream());
					}
				} else {
					// evades ClassCastException but the file dialog appearances slower than if we don't do anything 
//					try {
//						Method m = Toolkit.class.getDeclaredMethod("setDesktopProperty",new Class[] { String.class, Object.class });
//						m.setAccessible(true);
//						m.invoke(Toolkit.getDefaultToolkit(),new Object[] { "Shell.shellFolderManager", "com.t4l.win.MaskedShellFolderManager" });
//					} catch (Throwable t) {
//						ParameterSweepWizard.logError("Failed to initialized MaskedShellFolderManager.");
//						t.printStackTrace(ParameterSweepWizard.getLogStream());
//					}
				}
			}
		
			appWindow = new JFrame("MEME Model Wizard © 2007 - " + Calendar.getInstance().get(Calendar.YEAR) + " Aitia International, Inc.");
			JFrame frame = (JFrame) appWindow;
			frame.setIconImage(ExpandedEditor.getIcon("MEME.png").getImage());
			ParameterSweepWizard.setFrame(frame);
		} else 
			appWindow = new JDialog(ParameterSweepWizard.getFrame(),"MEME Model Wizard",true);
			appWindow.setName("dial_wizard");

		wizard = new ParameterSweepWizard(this,initClassPath);
		wizard.onClose.addListener(this);
		if (appWindow instanceof JFrame) {
			JFrame frame = (JFrame) appWindow;
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setContentPane(wizard);
			frame.pack();
			final Dimension oldD = frame.getPreferredSize();
			final Dimension newD = GUIUtils.getPreferredSize(frame);
			if (!oldD.equals(newD)) 
				frame.setPreferredSize(newD);
			frame.pack();
			appWindow.setLocationByPlatform(true);
		} else {
			JDialog dialog = (JDialog) appWindow;
			dialog.addWindowListener(new java.awt.event.WindowAdapter() {
				@Override public void windowClosing(java.awt.event.WindowEvent e) { wizard.doClose(false); }
			});
			dialog.setContentPane(wizard);
			dialog.pack();
			final Dimension oldD = dialog.getPreferredSize();
			final Dimension newD = GUIUtils.getPreferredSize(dialog);
			if (!oldD.equals(newD)) 
				dialog.setPreferredSize(newD);
			dialog.pack();
			dialog.setLocationRelativeTo(ParameterSweepWizard.getFrame());
		}
	}
}
