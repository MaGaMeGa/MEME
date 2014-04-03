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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.tabbed.TabCloseCallback;
import org.jvnet.substance.utils.SubstanceConstants.TabCloseKind;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.events.ProgramState;
import ai.aitia.meme.paramsweep.gui.MonitorGUI;
import ai.aitia.meme.paramsweep.gui.VCloudDownloader;
import ai.aitia.meme.utils.GUIUtils;

/**
 * This class implements the customizable layout of the panels of MainWindow
 * (tabbed, MDI, dockable etc.)
 */
public class MainWindowPanelManager extends ProgramState.Parameter<JPanel> implements ChangeListener,
									 												  IPanelManager {
	//=======================================================================
	//	Nested types

	/** Enum type for representing layouts. */
	static enum GUIMode {
		TABBED,
		MDI,
		DOCKABLE
	}

	/** Record class to encapsulates the informations about the panels. */
	static class PanelRec {
		/** The panel. */
		JPanel		panel;
		/** The title of the panel. */
		String		title;
		/** Is the panel enabled? */
		boolean		enabled;
		/** Can the panel be closed? */
		boolean 	closeable;
		@Override
		public boolean equals(Object obj) {
			return (obj instanceof PanelRec) && ((PanelRec)obj).panel.equals(panel);
		}
	}
	
	/** Close callback function class. */
	class CloseCallback implements TabCloseCallback {
		public String getAreaTooltip(JTabbedPane tabbedPane, int tabIndex) { return null; }
		public String getCloseButtonTooltip(JTabbedPane tabbedPane, int tabIndex) { return "Close"; }
		public TabCloseKind onAreaClick(JTabbedPane tabbedPane, int tabIndex, MouseEvent mouseEvent) { return TabCloseKind.NONE; }
		
		public TabCloseKind onCloseButtonClick(JTabbedPane tabbedPane, int tabIndex, MouseEvent mouseEvent) {
			if (SwingUtilities.isLeftMouseButton(mouseEvent)) {
				int result = MEMEApp.askUser(false,"Confirmation","Are you sure?");
				if (result == 1) {
					Object cc = ((JPanel)tabbedPane.getComponentAt(tabIndex)).getClientProperty(CUSTOM_CALLBACK);
					if (cc != null && cc instanceof Runnable)
						((Runnable)cc).run();
					panels.remove(tabIndex);
					setActive(panels.get(0).panel);
					rebuild();
					return TabCloseKind.NONE;
				}
			}
			return TabCloseKind.NONE;
		}
	}

	//=======================================================================
	//	Member variables

	public static final String CUSTOM_CALLBACK = "CUSTOM CALLBACK";
	
	/** The current layout type. */
	protected GUIMode currentMode = GUIMode.TABBED;
	/** The parent components of the panels. */
	protected JFrame parent = null;
	/** The list of the panels. */
	protected ArrayList<PanelRec> panels = new ArrayList<PanelRec>();
	/** The index of the selected panel */
	protected Integer selected = null; 
	protected Container jContentPane = null;
	protected javax.swing.event.InternalFrameListener ifl = null;
	protected CloseCallback callbackFunction = new CloseCallback(); 

	//=======================================================================
	//	Constructor

	// Not public - only MainWindow should use it
	MainWindowPanelManager(JFrame parent) {
		this.parent = parent;
	}

	//=======================================================================
	//	Public methods
	
	//-----------------------------------------------------------------------
	/** Adds a panel to the manager.
	 *  Does not call rebuild() - caller should do it after the last 'add()'
	 */
	public void add(JPanel panel, String title) { add(panel,title,false); }

	//-----------------------------------------------------------------------
	/** Adds a panel to the manager. 
	 * Does not call rebuild() - caller should do it after the last 'add()' 
	 */
	public void add(JPanel panel, String title, boolean closeable) {	// TODO: extend it with optional icon and tooltip
		int idx = panels.indexOf(panel);
		if (!panels.isEmpty() && idx == panels.size() - 1) return;
		PanelRec newrec = new PanelRec();
		newrec.panel = panel;
		newrec.title = title;
		newrec.enabled = panel.isEnabled();
		newrec.closeable = closeable;
		if (0 <= idx)
			panels.remove(idx);
		panels.add(newrec);
	}
	
	//-----------------------------------------------------------------------
	/** Removes a panel from the manager. */
	public void remove(JPanel panel) {
		int index = indexOf(panel);
		if (index != -1) {
			panels.remove(index);
			setActive(panels.get(0).panel);
			rebuild();
		}
	}

	// TODO: remove(), add(index, panel) etc.

	//-----------------------------------------------------------------------
	/** Changes the layout. Implies rebuild() */
	public void setMode(GUIMode mode) {
		if (mode != currentMode || jContentPane == null) {
			getActive();					// update 'selected'
			currentMode = mode;
			rebuild();						// TODO: generate an event and listen for it with rebuild() and menu radio buttons
		}
	}

	//-----------------------------------------------------------------------
	public GUIMode getMode() {
		return currentMode;
	}

	//-----------------------------------------------------------------------
	/** Rebuilds the content pane of the parent. */
	public void rebuild() {
		jContentPane = null;
		
		parent.setContentPane(getContentPane());
//		parent.pack();
		parent.validate();
		final Dimension screen = GUIUtils.getUseableScreenSize();
		if (parent.getWidth() > screen.width || parent.getHeight() > screen.height) {
			final JScrollPane sp = new JScrollPane(getContentPane(),JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			sp.setBorder(null);
			parent.setContentPane(sp);
//			parent.pack();
			parent.validate();
			Dimension oldD = parent.getPreferredSize();
			parent.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
												  oldD.height + sp.getHorizontalScrollBar().getHeight()));
			sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			oldD = parent.getPreferredSize();
			final Dimension newD = GUIUtils.getPreferredSize(parent);
			if (!oldD.equals(newD)) 
				parent.setPreferredSize(newD);
//			parent.pack();
			parent.validate();
		}
	}

	//-----------------------------------------------------------------------
	public void setEnabled(JPanel panel, boolean enabled) {
		int i = indexOf(panel);
		assert (i >= 0);
		if (i < 0) return;
		panels.get(i).enabled = enabled;
		if (jContentPane == null)
			return;
		switch (currentMode) {
			case TABBED :
				((JTabbedPane)jContentPane).setEnabledAt(i, enabled); 
				break;
			case MDI : 
				// TODO: letiltas megoldasa MDI modban - ld. a getMdi()-ben irottakat.
				break;
			case DOCKABLE :
				throw new java.lang.UnsupportedOperationException(); 
		}
	}

	//-----------------------------------------------------------------------
	public void setActive(JPanel panel) {
		int i = indexOf(panel);
		assert (i >= 0);
		if (i < 0) return;
		selected = i;
		if (jContentPane == null)
			return;
		switch (currentMode) {
			case TABBED :
				((JTabbedPane)jContentPane).setSelectedIndex(i); 
				break;
			case MDI : 
				JInternalFrame f = findMdi(i);
				if (f != null) {
					f.moveToFront();
					try { f.setSelected(true); } catch (Exception e) {}
				}
				break;
			case DOCKABLE :
				throw new java.lang.UnsupportedOperationException(); 
		}
	}

	//-----------------------------------------------------------------------
	public JPanel getActive() {
		if (jContentPane != null) {
			selected = null;
			switch (currentMode) {
				case TABBED :
					selected = ((JTabbedPane)jContentPane).getSelectedIndex(); 
					break;
				case MDI :
					for (int i = panels.size() - 1; i >= 0; --i) {
						JInternalFrame f = findMdi(i);
						if (f != null) {
							javax.swing.JLayeredPane jl = javax.swing.JLayeredPane.getLayeredPaneAbove(f);
							if (jl != null && jl.getPosition(f) == 0) {
								selected = i;
								break;
							}
						}
					}
					break;
				case DOCKABLE :
					throw new java.lang.UnsupportedOperationException(); 
			}
		}
		return (selected == null) ? null : panels.get(selected).panel;
	}
	
	//------------------------------------------------------------------------
	/** Return true if there is a live monitor panel. */
	public boolean hasAliveMonitor() {
		for (int i = panels.size() - 1; i >= 0;--i) {
			if (panels.get(i).panel instanceof MonitorGUI) {
				MonitorGUI gui = (MonitorGUI) panels.get(i).panel;
				if (!gui.isLocal()) return true;
			}
		}
		return false;
	}
	
	//-----------------------------------------------------------------------
	/** Set the monitor panel active. */
	public void setMonitorActive() {
		for (int i = panels.size() - 1; i >= 0;--i) {
			if (panels.get(i).panel instanceof MonitorGUI) {
				MonitorGUI gui = (MonitorGUI) panels.get(i).panel;
				if (!gui.isLocal())
					setActive(panels.get(i).panel);
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean hasAliveDownloader() {
		for (int i = panels.size() - 1; i >= 0;--i) {
			if (panels.get(i).panel instanceof VCloudDownloader) return true;
		}
		return false;
	}

	//-----------------------------------------------------------------------
	// ProgramState.Parameter methods
	@Override public Object getID()		{ return "activePanel"; }
	public JPanel getValue()	{ return getActive(); }

	//=======================================================================
	//	Internal methods

	//-----------------------------------------------------------------------
	// Registered listener for the tabbed pane. Also called in the MDI case,
	// with e == null.
	public void stateChanged(javax.swing.event.ChangeEvent e) {
		this.fireLater();		// announce change of active panel
	}

	//-----------------------------------------------------------------------
	/** Returns the current content pane. */
	protected Container getContentPane() {
		if (jContentPane == null) {
			switch (currentMode) {
				case TABBED : 
					jContentPane = getTabbed(); 
					break;
				case MDI : 
					jContentPane = getMdi(); 
					break;
				case DOCKABLE :
					throw new java.lang.UnsupportedOperationException("dockable mode is not implemented yet"); 
					//break;					
			}
		}
		return jContentPane;
	}

	//-----------------------------------------------------------------------
	/** Returns the content pane in TABBED mode. */
	protected JTabbedPane getTabbed() {
		JTabbedPane ans = new JTabbedPane();
		for (int i = 0; i < panels.size(); ++i) {
			PanelRec r = panels.get(i);
			ans.addTab(r.title, r.panel);
			ans.setEnabledAt(i, r.enabled);
		}
		ans.putClientProperty(SubstanceLookAndFeel.TABBED_PANE_CLOSE_CALLBACK,callbackFunction);
		if (selected != null)
			ans.setSelectedIndex(selected);
		ans.addChangeListener(this);
		//ans.setTabPlacement(JTabbedPane.RIGHT);
		return ans;
	}

	//-----------------------------------------------------------------------
	/** Returns the content pane in MDI mode. */
	protected JDesktopPane getMdi() {
		JDesktopPane ans = new JDesktopPane();
		int dlt = GUIUtils.GUI_unit(2);
		for (int i = 0; i < panels.size(); ++i) {
			PanelRec r = panels.get(i);
			JInternalFrame frame = new JInternalFrame(r.title, true, r.closeable, true, true);
			frame.setDefaultCloseOperation(JInternalFrame.DO_NOTHING_ON_CLOSE);
			frame.setContentPane(r.panel);
			frame.setFrameIcon(null);
			frame.pack();
			frame.setLocation(i * dlt, i * dlt);
			frame.setVisible(true);
			// TODO: ez nem eleg. Ha disabled, akkor minimalizalni kellene es meg kellene tiltani, 
			// hogy kinyissak. Vagy rekurzive minden kontrolt le kellene tiltani rajta; de akkor 
			// azt is meg kellene jegyezni, hogy melyik az amelyik magatol is tiltva volt. Ez
			// maceras, mert az egesz engedelyezese elott egyenkent is engedelyezhetnek...
			frame.setEnabled(r.enabled);
			if (ifl == null)
				ifl = new InternalFrameAdapter() {
				@Override public void internalFrameClosing(InternalFrameEvent e) {
					int result = MEMEApp.askUser(false,"Confirmation","Are you sure?");
					if (result == 1) {
						JPanel panel = (JPanel)e.getInternalFrame().getContentPane(); 
						Object cc = panel.getClientProperty(CUSTOM_CALLBACK);
						if (cc != null && cc instanceof Runnable)
							((Runnable)cc).run();
						int index = indexOf(panel);
						panels.remove(index);
						if (selected == index && index >= panels.size())
							setActive(panels.get(index - 2).panel);
						e.getInternalFrame().setVisible(false);
					}
				}
				@Override public void internalFrameActivated(InternalFrameEvent e) { stateChanged(null); }
			};
			frame.addInternalFrameListener(ifl);

			ans.add(frame);
			if (selected != null && selected.intValue() == i) {
				frame.moveToFront();
				try { frame.setSelected(true); } catch (java.beans.PropertyVetoException e) {}
			}
		}
		//ans.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

		return ans;
	}

	//-----------------------------------------------------------------------
	/** Returns the index of the panel which contains 'component' or -1 if nobody
	 *  contains it.
	 */
	private int indexOf(Object component) {
		for (int i = panels.size() - 1; i >= 0; --i) {
			if (panels.get(i).panel == component)
				return i;
		}
		return -1;
	}

	//-----------------------------------------------------------------------
	private JInternalFrame findMdi(int index) {
		JPanel p = panels.get(index).panel;
		return (JInternalFrame)SwingUtilities.getAncestorOfClass(JInternalFrame.class, p);
	}
}
