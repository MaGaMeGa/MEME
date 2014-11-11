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
package ai.aitia.meme.paramsweep.sftp;

import static ai.aitia.meme.utils.GUIUtils.GUI_unit;

import java.awt.AWTEvent;
import java.awt.ActiveEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.IllegalComponentStateException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import sun.awt.PeerEvent;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;

import com.jcraft.jsch.SftpProgressMonitor;

/** This class provides a progress dialog that monitors the progress of the 
 *  uploading/downloading.
 */
public class SftpFileLoadingMonitor extends JDialog implements SftpProgressMonitor,
															   ActionListener {

	//=================================================================================
	// members
	
	private static final long serialVersionUID = 1L;

	/** Flag that determines whether the user aborts the file transfer. */
	private boolean isCancelled = false;
	/** The current position of the progress. */
	private long count = 0;
	/** The length of the progress. */
	private long max = 0;
	/** The progress in percent format. */
	private long percent = 0;
	
	// members for provide compatibility with MEME
	
	private Field fKeepBlocking = null;
	private Field fWindowClosingException = null;
	private Method mGetEventQueue = null;
	
	private Method mPumpOneEvent = null;
	private Method mAddFilter = null;
	private Method mRemoveFilter = null;
	private Constructor cFilter = null;
	private int JRE = 4;
	
	//=================================================================================
	// GUI-members
	
	private JPanel content = new JPanel(new BorderLayout());
	private JLabel taskLabel = new JLabel();
	private JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL,0,100);
	private JPanel top = null;
	private JButton abortButton = new JButton("Abort");
	private JPanel bottom = new JPanel();
	
	//=================================================================================
	// methods
	
	//---------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the parent of the dialog
	 * @param taskName the name of the task
	 * @param isUpload true in the case of uploading and false in the case of downloading
	 */
	public SftpFileLoadingMonitor(Frame owner, String taskName, boolean isUpload) {
		super(owner,"Please wait...",true);
		String task = isUpload ? "Uploading " : "Downloading ";
		task += taskName + "...";
		taskLabel.setText(task);
		layoutGUI();
		initialize();
		this.setLocationRelativeTo(owner);
	}
	
	//---------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the parent of the dialog
	 * @param taskName the name of the task
	 * @param isUpload true in the case of uploading and false in the case of downloading
	 */
	public SftpFileLoadingMonitor(Dialog owner, String taskName, boolean isUpload) {
		super(owner,"Please wait...",true);
		String task = isUpload ? "Uploading " : "Downloading ";
		task += taskName + "...";
		taskLabel.setText(task);
		layoutGUI();
		initialize();
		this.setLocationRelativeTo(owner);
	}

	//--------------------------------------------------------------------------------
	/** Tests if the user aborts the task. */
	public boolean isAborted() { return isCancelled; }
	
	//=================================================================================
	// implemented interfaces
	
	//---------------------------------------------------------------------------------
	/** Updates the progress. */
	public boolean count(long count) {
		this.count += count;
		if (percent >= this.count * 100 / max) {
			return true;
		}
		
		percent = this.count * 100 / max;
		progressBar.setValue((int)percent);
		progressBar.setString(String.valueOf(percent) + " %");
		
		return !isCancelled;
	}

	//---------------------------------------------------------------------------------
	/** Invokes when the task is done. */
	public void end() {
		progressBar.setString("Completed");
		setVisible(false);
		this.dispose();
	}

	//---------------------------------------------------------------------------------
	/** Initializes the progress. */
	@SuppressWarnings("unchecked")
	public void init(int op, String src, String dest, long max) {
		this.max = max;
		this.count = 0;
		this.percent = 0;
		progressBar.setValue((int)percent);
		progressBar.setString("0 %");
		if (EventQueue.isDispatchThread()) {
			Thread _dispatcher = null;
			final Thread dispatchThread = Thread.currentThread();
			while (mPumpOneEvent == null) {
				try {
					JRE++;
					switch (JRE) {
					case 5 :
						mPumpOneEvent = dispatchThread.getClass().getDeclaredMethod("pumpOneEventForHierarchy",Integer.TYPE, java.awt.Component.class);
						mPumpOneEvent.setAccessible(true);
						break;
					case 6 :
						mPumpOneEvent = dispatchThread.getClass().getDeclaredMethod("pumpOneEventForFilters",Integer.TYPE);
						mPumpOneEvent.setAccessible(true);
						if (!mPumpOneEvent.getReturnType().equals(Boolean.TYPE)){
							mPumpOneEvent = null;
							continue;
						}
						Class cls = Class.forName("java.awt.EventFilter");
						mAddFilter = dispatchThread.getClass().getDeclaredMethod("addEventFilter",cls);
						mRemoveFilter = dispatchThread.getClass().getDeclaredMethod("removeEventFilter",cls);
						mAddFilter.setAccessible(true);
						mRemoveFilter.setAccessible(true);
						cls = Class.forName("java.awt.EventDispatchThread$HierarchyEventFilter");
						cFilter = cls.getConstructor(Component.class);
						cFilter.setAccessible(true);
						break;
					case 7 :
						mPumpOneEvent	= dispatchThread.getClass().getDeclaredMethod("pumpOneEventForFilters", 
								Integer.TYPE);
						mPumpOneEvent.setAccessible(true);
						
						if (!mPumpOneEvent.getReturnType().equals(Void.TYPE)){
							mPumpOneEvent = null;
							continue;
						}
						cls		= Class.forName("java.awt.EventFilter");
						mAddFilter		= dispatchThread.getClass().getDeclaredMethod("addEventFilter", cls);
						mRemoveFilter	= dispatchThread.getClass().getDeclaredMethod("removeEventFilter", cls);
						mAddFilter.setAccessible(true);
						mRemoveFilter.setAccessible(true);
						cls				= Class.forName("java.awt.EventDispatchThread$HierarchyEventFilter");
						cFilter		= cls.getConstructor(new Class[] { java.awt.Component.class });
						cFilter.setAccessible(true);
						break;
					default :
						throw new UnsupportedOperationException();
					}
				} catch (NoSuchMethodException e) {
				} catch (ClassNotFoundException e) {}
			}
			final Runnable pumpEventsForHierarchy = new Runnable() {
				public void run() {
					try {
						Object filter = null, args[];
						if (JRE == 5) 
							args = new Object[] { -1, SftpFileLoadingMonitor.this };
						else {
							args = new Object[] { -1 };
							filter = cFilter.newInstance(SftpFileLoadingMonitor.this);
							mAddFilter.invoke(dispatchThread,filter);
						}
						
						for (boolean loop = true;loop;) {
							loop = (!dispatchThread.isInterrupted() && condition());
							if (loop) {
								try {
									if (JRE < 7){	
										loop = (Boolean)mPumpOneEvent.invoke(dispatchThread,args);
									} else {
										mPumpOneEvent.invoke(dispatchThread,args);
									}
								} catch (Exception e) {
									e.printStackTrace(ParameterSweepWizard.getLogStream());
									// never happens in normal state
									throw new IllegalStateException(e);
								} finally {
									if (filter != null)
										mRemoveFilter.invoke(dispatchThread,filter);
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace(ParameterSweepWizard.getLogStream());
					}
				}
			};
			
			_dispatcher = new Thread() { 
				@Override
				public void run() { 
					EventQueue big_Q = Toolkit.getDefaultToolkit().getSystemEventQueue();
					while(big_Q.peekEvent() != null) {
						if (isCancelled || count >= SftpFileLoadingMonitor.this.max) 
							break;
						try {
							AWTEvent e = big_Q.getNextEvent();
							Object source = e.getSource();
							if (e instanceof ActiveEvent)
								((ActiveEvent)e).dispatch();
							else if (source instanceof Component)
								((Component)source).dispatchEvent(e);
						} catch (java.lang.InterruptedException ex) {
						} catch (IllegalComponentStateException ex) {
						}  catch (ClassCastException t) {
							PeerEvent pe = new PeerEvent(SftpFileLoadingMonitor.this,pumpEventsForHierarchy,PeerEvent.PRIORITY_EVENT);
							try {
								if (mGetEventQueue == null) {
									mGetEventQueue = Toolkit.class.getDeclaredMethod("getEventQueue");
									mGetEventQueue.setAccessible(true);
								}
								((EventQueue)mGetEventQueue.invoke(null)).postEvent(pe);
							} catch (Exception e) {
								e.printStackTrace(ParameterSweepWizard.getLogStream());
								// never happens in normal state
								throw new IllegalStateException(e);
							}
						}
					}
				}
			};
			Thread t = new Thread() {
				@Override
				public void run() {
					SftpFileLoadingMonitor.this.setVisible(true); 
				}
			};
			t.setName("Support-Thread-1");
			t.start();
			_dispatcher.setName("MEME-Temporary-Dispatcher-Thread");
			_dispatcher.start();
		} else 
			Utils.invokeLater(this,"setVisible",Boolean.TRUE);
	}
		
	//----------------------------------------------------------------------------------------------------
	private boolean condition() {
		try {
			if (fKeepBlocking == null) {
				fKeepBlocking = Dialog.class.getDeclaredField("keepBlocking");
				fKeepBlocking.setAccessible(true);
			}
			if (fWindowClosingException == null) {
				fWindowClosingException = Component.class.getDeclaredField("windowClosingException");
				fWindowClosingException.setAccessible(true);
			}
			return (Boolean)fKeepBlocking.get(this) && fWindowClosingException.get(this) == null;
		} catch (Exception e) {
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			// never happened in normal state
			throw new IllegalStateException(e);
		}
	}

	//---------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		isCancelled = true;
		abortButton.setEnabled(false);
		setVisible(false);
		this.dispose();
	}

	//=================================================================================
	// GUI-methods
	
	//---------------------------------------------------------------------------------
	private void layoutGUI() {
		
		top = FormsUtils.build("p",
							   "[DialogBorder]0||" +
							   				 "1",
							   	taskLabel,
							   	progressBar).getPanel();
		
		bottom.add(abortButton);
		
		content.add(top,BorderLayout.CENTER);
		content.add(bottom,BorderLayout.SOUTH);
	}
	
	//---------------------------------------------------------------------------------
	private void initialize() {
		abortButton.addActionListener(this);
		
		progressBar.setStringPainted(true);
		Dimension d = progressBar.getPreferredSize();
		if (d.width < GUI_unit(15)) {
			d.width = GUI_unit(15);
			progressBar.setPreferredSize(d);
		}
		
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) { abortButton.doClick(); }
		});
		final JScrollPane sp = new JScrollPane(content);
		sp.setBorder(null);
		this.setContentPane(sp);
		this.pack();
		final Dimension oldD = this.getPreferredSize();
		final Dimension newD = GUIUtils.getPreferredSize(this);
		if (!oldD.equals(newD)) 
			this.setPreferredSize(newD);
		this.pack();
	}
}
