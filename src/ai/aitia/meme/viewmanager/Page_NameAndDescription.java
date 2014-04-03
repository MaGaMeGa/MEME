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
package ai.aitia.meme.viewmanager;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.w3c.dom.Element;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ViewRec;
import ai.aitia.meme.gui.SimpleFileFilter;
import ai.aitia.meme.gui.Wizard;
import ai.aitia.meme.gui.Wizard.Button;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.XMLUtils;

import com.jgoodies.forms.layout.CellConstraints;

/** Wizard page "Name and description" in the View Creation Wizard. */
@SuppressWarnings("serial")
public class Page_NameAndDescription extends JPanel implements Wizard.IWizardPage, 
															   Wizard.ICloseListener,
															   Wizard.IArrowsInHeader,
															   ActionListener,
															   CaretListener {
	private ViewCreationDialog owner = null;
//	private String acceptedName = null;
//	private DelayedVerifyTimer delayedVerifyTimer = null; 
//	private boolean forceAsk = false;			// used in case of recreate view 
//	private boolean repeatFinish = false;
//	private boolean inSave = false;

	private JTextField	jNameTextField = new JTextField();
	private JTextArea	jDescTextArea = new JTextArea();
	private JButton		jSaveScriptButton = new JButton("Save view script...");

	//-------------------------------------------------------------------------
//	@SuppressWarnings("serial")
	/** Fires view name verify events after a specified delay.  */
//	public class DelayedVerifyTimer extends javax.swing.Timer implements java.awt.event.ActionListener {
//		private static final int VERIFY_DELAY = 1000;		// msec
//		public DelayedVerifyTimer() {
//			super(VERIFY_DELAY, null);
//			addActionListener(this);
//			setCoalesce(true);
//		}
//		public void actionPerformed(java.awt.event.ActionEvent e) {
//			jNameTextField.getInputVerifier().shouldYieldFocus(jNameTextField);
//		}
//		@Override public void start() {
//			super.stop();
//			super.start();
//		}
//	}

	//=========================================================================
	//	Public methods

	/**
	 * This is the default constructor
	 */
	public Page_NameAndDescription(ViewCreationDialog owner) {
		this.owner = owner;  
		initialize();
	}

	//-------------------------------------------------------------------------
//	public String getViewTableName()			{ return acceptedName; }
	public String getViewTableName()			{ return jNameTextField.getText().trim(); }
	public String getUncheckedViewTableName() 	{ return jNameTextField.getText().trim(); }
	public String getDescription()				{ return jDescTextArea.getText(); }
//	public synchronized boolean getInSave()		{ return inSave; } 
//	public synchronized void setInSave(boolean inSave) { this.inSave = inSave; }


	//=========================================================================
	//	Interface methods

	//-------------------------------------------------------------------------
	public String getTitle() {
		return "Name and description";
	}
	
	//-------------------------------------------------------------------------
	public String getInfoText(Wizard w) {
		return w.getArrowsHeader("The name of the view table must be non-empty and unique");
	}

	//-------------------------------------------------------------------------
	public Container getPanel() {
		return this;
	}

	//-------------------------------------------------------------------------
	public boolean isEnabled(Button b) {
		switch (b) {
			case FINISH	:
			case NEXT	: return (getViewTableName().length() > 0); 
//			case NEXT	: return (acceptedName != null && acceptedName.length() > 0); 
			case BACK	: 
			case CANCEL	:
			default    	: return true;
		}
	}

	//-------------------------------------------------------------------------
	public boolean onButtonPress(Button b) {
		if (b == Button.FINISH)
			return checkViewName();
//		{	
//			if (forceAsk) { acceptedName = null; repeatFinish = true; }
//			if (forceAsk || delayedVerifyTimer.isRunning()) {
//				delayedVerifyTimer.actionPerformed(null);
//			}
//		}
		return isEnabled(b);
	}

	//-------------------------------------------------------------------------
	public void onPageChange(boolean show) {
		if (show) {
			Utils.invokeLater(jNameTextField, "requestFocusInWindow");
//		} else {
//			delayedVerifyTimer.stop();
		}
	}

	//-------------------------------------------------------------------------
	public void onClose(Wizard w) {
//		try {
//			Thread.sleep(DelayedVerifyTimer.VERIFY_DELAY);
//		} catch (InterruptedException e) {}
//		if (delayedVerifyTimer != null)
//			delayedVerifyTimer.stop();
//		delayedVerifyTimer = null;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void caretUpdate(CaretEvent e) {
		owner.updateViewNameOnWindowTitle(jNameTextField.getText().trim());
		owner.getWizard().enableDisableButtons();
	}

	//-------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(jSaveScriptButton)) {
//			setInSave(true);
			File f = openSaveDialog();
			while (f != null && f.exists()) {
				int result = MEMEApp.askUser(false,"Override confirmation",
											 f.getName() + " already exists.",
											 "Do you want to replace it?");
				if (result == 1) break;
				f = openSaveDialog();
			}
			if (f == null) {
//				setInSave(false);
				jNameTextField.grabFocus();
//				delayedVerifyTimer.start();
				return;
			}
			final ViewCreationRule rule = owner.updateRule();
			Element node = null;
			try {
				GUIUtils.setBusy(this,true);
				node = (Element)MEMEApp.LONG_OPERATION.execute("Searching models...",
															   new Callable<Object>() {
					public Object call() {
						return rule.transformRuleToSave();	
					}
				});
				GUIUtils.setBusy(this,false);
			} catch (Exception e2) {}
			try {
				XMLUtils.write(f,node);
			} catch (Exception e1) {
				MEMEApp.logExceptionCallStack("Saving view script", e1);
				throw new IllegalStateException(e1);
			} finally {
//				setInSave(false);
				jNameTextField.grabFocus();
//				delayedVerifyTimer.start();
			}
		} else if (e.getSource().equals(jNameTextField)) {
			owner.getWizard().finish();
		}
	}

	//=========================================================================
	//	Controller methods

//	//-------------------------------------------------------------------------
//	void setViewName(String name) {
//		jNameTextField.setText(name);
//	}

	//-------------------------------------------------------------------------
//	/** This class verifies the given view name. If the given name is already in the 
//	 *  database, the program will asks user the override confirmation. */
//	class MyInputVerifier extends Wizard.InputVerifier implements Runnable, ITRunnable, ILopListener {
//		String current;
//		boolean exists = false;
//		MyInputVerifier(Wizard w) { w.super(); }
//		@Override public boolean verify(JComponent input) {
//			delayedVerifyTimer.stop();
//			current = getText(input);
//			if (current.length() == 0) {
//				acceptedName = null;
//			} else {
//				MEMEApp.LONG_OPERATION.begin2("Checking name...", this);
//			}
//			return true;
//		}
//		public void trun() {
//			exists = (MEMEApp.getViewsDb().findView(current) != null);
//		}
//		public void finished() {
//			// Ezzel a kesobbrehalasztassal elerjuk, hogy becsukodjon a Progress ablak
//			// ha kinyilt volna, mielott kitennenk a kerdest. Ez fontos mert ha a kerdest
//			// veletlenul elobb tennenk ki, akkor a ket ablak osszeakad ugy hogy egyiket se
//			// lehet becsukni.
//			javax.swing.SwingUtilities.invokeLater(this);
//		}
//		public void run() {
//			if (getInSave()) {
////				javax.swing.SwingUtilities.invokeLater(this);
//				lastExamined = "";
//				return;
//			}
//			boolean rf = (forceAsk && repeatFinish);
//			forceAsk = false;
//			repeatFinish = false;
//			boolean ok = !exists;
//			if (!ok) {
//				owner.getWizard().disarmAllButtons();
//				ok = JOptionPane.showConfirmDialog(owner.getWizard(), String.format(
//						"A view named \"%s\" already exists, and will be overwritten. " +
//						"Are you sure?", current), "Warning", 
//						JOptionPane.YES_NO_OPTION,
//						JOptionPane.WARNING_MESSAGE
//					) == JOptionPane.YES_OPTION;
//			}
//			acceptedName = (ok && current.length() > 0) ? current : null;
//			if (!ok) {
//				lastResult = false;
//				owner.getWizard().gotoPage(Page_NameAndDescription.this);
//				jNameTextField.requestFocus();
//			} else {
//				owner.updateViewNameOnWindowTitle(acceptedName);
//			}
//			owner.getWizard().enableDisableButtons();
//			if (ok && rf) owner.getWizard().finish();
//		}
//		/** Event handler method for pressing ENTER. */
//		public void onEnterPress() {
//			setLastExamined(null);
//			owner.getWizard().finish();
//		}
//	}
	
	//-------------------------------------------------------------------------
	/** Shows a file dialog where the user would define the path and nam of the 
	 *  new script file. 
	 * @return the selected file
	 */
	private File openSaveDialog() {
		JFileChooser chooser = new JFileChooser();
		String fileName = "";
		File dir = MEMEApp.getLastDir();
		if (dir != null) fileName += dir.getPath();
		if (!jNameTextField.getText().trim().equals("")) {
			if (!fileName.equals("")) fileName += File.separator;
			fileName += jNameTextField.getText().trim() + ".xml";
		}
		if (!fileName.equals("")) chooser.setSelectedFile(new File(fileName));
		chooser.addChoosableFileFilter(new SimpleFileFilter("MEME View Script files (*.xml)"));
		int returnVal = chooser.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			MEMEApp.setLastDir(chooser.getSelectedFile());
			return chooser.getSelectedFile();
		}
		return null;
	}

	//=========================================================================
	//	GUI (View) methods

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
//		delayedVerifyTimer = new DelayedVerifyTimer();

		if (owner != null) {	// not test()-case
			String viewName = owner.getRule().getName();
			jNameTextField.setText(viewName);
			owner.updateViewNameOnWindowTitle(viewName);
			if (viewName == null || viewName.length() == 0) {
				MEMEApp.LONG_OPERATION.begin("Generating view name...", new LongRunnable() {
					
					String name = "View";
					@Override public void run() {
						ArrayList<Object> inputTables = owner.page_input.getInputTables();
						if (inputTables.size() == 1) {
							if (inputTables.get(0) instanceof ViewRec)
								name = ((ViewRec)inputTables.get(0)).getName();
							else {
								final long modelId = ((Long[])inputTables.get(0))[0];
								final Model model = MEMEApp.getResultsDb().findModel(modelId);
								if (model != null)
									name = model.getName() + "." + model.getVersion();
							}
						}
						
						String numberedName = name;
						final char[] tmp = numberedName.toCharArray();
						final boolean endsWithNumber = Character.isDigit(tmp[tmp.length - 1]);
						for (int i = 1; true; ++i) { 
							if (i > 1) 
								numberedName = String.format("%s%s%02d", name,endsWithNumber ? "_" : "", i);
							if (MEMEApp.getViewsDb().findView(numberedName) == null) break;
						}
						name = numberedName;
					}
					@Override public void finished() throws Exception {
						jNameTextField.setText(name);
						owner.updateViewNameOnWindowTitle(name);
//						acceptedName = name;
					}
				});
//				forceAsk = false;	// Nem kell kerdezni, mert nincs mit felulirni, hisz' nemletezo nevet kap 
//			} else {
//				acceptedName = viewName;
//				forceAsk = true;	// Legkesobb Finish-nel kikenyszeritjuk a rakerdezest
			}

//			final MyInputVerifier verifier = new MyInputVerifier(owner.getWizard());
//			jNameTextField.setInputVerifier(verifier);
//			jNameTextField.getDocument().addDocumentListener(
//					java.beans.EventHandler.create(DocumentListener.class, delayedVerifyTimer, "start")
//			);
			jNameTextField.addCaretListener(this);
			jNameTextField.addActionListener(this);
			jDescTextArea.setText(owner.getRule().getDescription());
//			GUIUtils.bind(jNameTextField, null, "ENTER", new GUIUtils.CallbackAction(new Runnable() {
//				public void run() { verifier.onEnterPress(); }
//			}, "ENTER"));
			jSaveScriptButton.addActionListener(this);
		}

		layoutGUI();
	}

	//-------------------------------------------------------------------------
	private void layoutGUI() {
		this.setLayout(new java.awt.BorderLayout());
		this.add(FormsUtils.build("r:p ~ f:p:g", "[DialogBorder]01|~|23 f:max(60dlu;pref)|~|_4 pref", 
				"&Name:", jNameTextField,
				"&Description:", jDescTextArea,
				jSaveScriptButton, CellConstraints.LEFT
		).getPanel(), java.awt.BorderLayout.CENTER);
	}

	//-------------------------------------------------------------------------
	public static void test(String[] args) {
		new Wizard(new Page_NameAndDescription(null)).showInDialog(null).setVisible(true);
		System.exit(0);
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean checkViewName() {
		try {
			boolean exists = (Boolean) MEMEApp.LONG_OPERATION.execute("Checking name...",new Callable<Object>() {

				//----------------------------------------------------------------------------------------------------
				public Object call() throws Exception {
					return MEMEApp.getViewsDb().findView(getViewTableName()) != null;
				}
			});
			boolean ok = !exists;
			if (!ok) {
				owner.getWizard().disarmAllButtons();
				ok = JOptionPane.showConfirmDialog(owner.getWizard(),String.format("A view named \"%s\" already exists, and will be overwritten. " +
												   "Are you sure?",getViewTableName()),"Warning",JOptionPane.YES_NO_OPTION,
												   JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
			}
			if (!ok) {
				owner.getWizard().gotoPage(Page_NameAndDescription.this);
				jNameTextField.requestFocus();
			} else 
				owner.updateViewNameOnWindowTitle(getViewTableName());
			owner.getWizard().enableDisableButtons();
			return ok;
		} catch (final Exception e) {
			MEMEApp.logException("Page_NameAndDescription.checkViewName",e);
			return false;
		}
	}
}
