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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.skin.ModerateSkin;

import ai.aitia.meme.gui.ExpandedEditor;
import ai.aitia.meme.gui.IPanelManager;
import ai.aitia.meme.paramsweep.launch.RemoteMonitorLogic;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.Utilities.IEBinary;
import ai.aitia.meme.pluginmanager.IPlugin.IOnPluginLoad;
import ai.aitia.meme.utils.GUIUtils;

/** The main entry point of the MASS/MEME Monitor application. */
public class PS_Monitor {
	
	//====================================================================================================
	// members

	public static final String CUSTOM_CALLBACK = "CUSTOM CALLBACK";
	
	//====================================================================================================
	// methods
	
	//--------------------------------------------------------------------------------
	public static void main(String[] args) {
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
					m.invoke(Toolkit.getDefaultToolkit(),new Object[] { "Shell.shellFolderManager",folderManagerClass });
				} catch (Throwable t) {
					ParameterSweepWizard.logError("Failed to initialized MaskedShellFolderManager.");
					t.printStackTrace(ParameterSweepWizard.getLogStream());
				}
			} else {
				// see PS_ModelWizard.initialze() method for explanation
			}
		}
		initPlatformManager();
		createMonitor(false);
	}
	
	//------------------------------------------------------------------------------------
	/** Creates the Parameter Sweep Monitor.
	 * @param fromMEME flag that determines whether monitor is used by the MEME or as a
	 *        standalone application
	 */
	public static void createMonitor(boolean fromMEME) { createMonitor(null,fromMEME,null,null,null); }
	
	//------------------------------------------------------------------------------------
	/** Creates the Parameter Sweep Monitor.
	 * @param owner the owner of the monitor (if any)
	 * @param fromMEME flag that determines whether monitor is used by the MEME or as a
	 *        standalone application
	 * @param serverProperties properties of the servers
	 * @param callback callback function that starts the Repast import plugin (used only in MEME mode)
	 * @param manager panel manager object (used only in MEME mode)
	 */
	public static void createMonitor(JFrame owner, final boolean fromMEME, Properties serverProperties, IEBinary<String[],PlatformType> callback,
									 final IPanelManager manager) {
		ParameterSweepWizard.setFromMEME(fromMEME);
		final JFrame appWindow = fromMEME ? owner : new JFrame("MEME Simulation Monitor � 2007 - " + Calendar.getInstance().get(Calendar.YEAR) +
								 " Aitia International, Inc.");
		final RemoteMonitorLogic monitor = new RemoteMonitorLogic(appWindow,fromMEME,manager,callback);
		if (serverProperties != null)
			monitor.setServerProperties(serverProperties);
		if (fromMEME) {
			monitor.getGUI().putClientProperty(SubstanceLookAndFeel.TABBED_PANE_CLOSE_BUTTONS_PROPERTY,Boolean.TRUE);
			monitor.getGUI().putClientProperty(CUSTOM_CALLBACK,new Runnable() {
				public void run() {
					monitor.dispose();
					System.gc();
				}
			});
			manager.add(monitor.getGUI(),"Monitor",true);
			manager.rebuild();
			manager.setActive(monitor.getGUI());
		} else {
			appWindow.setIconImage(ExpandedEditor.getIcon("MEME.png").getImage());
			appWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			appWindow.addComponentListener(new ComponentAdapter() {
				@Override public void componentHidden(ComponentEvent e) { PS_Monitor.closeMonitor(appWindow,monitor,fromMEME); }
			});
			appWindow.addWindowListener(new WindowAdapter() {
				@Override public void windowClosing(WindowEvent e) { PS_Monitor.closeMonitor(appWindow,monitor,fromMEME); }
			});
			final JScrollPane sp = new JScrollPane(monitor.getGUI(),JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			sp.setBorder(null);
			appWindow.setContentPane(sp);
			appWindow.pack();
			Dimension oldD = appWindow.getPreferredSize();
			appWindow.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
											         oldD.height + sp.getHorizontalScrollBar().getHeight()));
			sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			oldD = appWindow.getPreferredSize();
			final Dimension newD = GUIUtils.getPreferredSize(appWindow);
			if (!oldD.equals(newD)) 
				appWindow.setPreferredSize(newD);
			appWindow.pack();
			appWindow.setVisible(true);
		}
		monitor.start();
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private static void closeMonitor(JFrame appWindow, RemoteMonitorLogic monitor, boolean fromMEME) {
		monitor.dispose();
		if (fromMEME) {
			appWindow.setVisible(false);
			appWindow.dispose();
			System.gc();
		} else
			System.exit(0);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void initPlatformManager() {
		try {
			PlatformManager.setPlatformPluginsDir(new File(".","PlatformPlugins").getCanonicalPath());
			String[] errs = PlatformManager.scanPlatformPlugins(new IOnPluginLoad() {});
			if (errs.length > 0) {
				for (String err : errs) 
					ParameterSweepWizard.logError("Warning: " + err);
			}
		} catch (Exception e) {
			ParameterSweepWizard.logError("Error while initializing the platform manager: " + Util.getLocalizedMessage(e));
			e.printStackTrace(ParameterSweepWizard.getLogStream());
		}
	}
}
