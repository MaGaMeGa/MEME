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
package ai.aitia.meme.gui.lop;

import javax.swing.JPanel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JButton;

import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.GUIUtils;
import static ai.aitia.meme.utils.GUIUtils.GUI_unit;
import static ai.aitia.meme.utils.GUIUtils.setBusy;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Date;

/** The class provides the GUI of the progress dialog. */
public class LOPDialog extends JDialog implements IProgressUpdate
{
	private static Integer MAX_WIDTH = null; 
	
	/** The producer object. */
	private LOPProducer owner = null;
	/** The information text of the dialog. */
	private String infoText = "";
	/** Flag that determines whether the related task is done or not. */
	private boolean isDone = false; 

	private static final long serialVersionUID = 1L;

	private JPanel jContentPane = null;

	private JScrollPane jScrollPane = null;

	private JEditorPane jEditorPane = null;

	private JPanel jPanel = null;

	private JLabel jTaskNameLabel = null;

	private JProgressBar jProgressBar = null;

	private JButton jAbortButton = null;
	
	private JLabel jElapsedLabel = null;
	
	private JLabel jEstimatedLabel = null;

	/**
	 * @param owner
	 */
	public LOPDialog(LOPProducer owner) {
		super(owner == null ? null : owner.getParent(), true);
		this.owner = owner; 
		initialize();
	}

	/** Shows the dialog and sets the taskname. It enables the Abort button and calls
	 *  the operation LOPProducder.setVisible(true). If the dialog was visible before
	 *  the calling this methods, returns immediatly. Otherwise it blocks until the 
	 *  dialog is closed. 
	 */
	public void begin() {
		if (isDone) {	// There is a Close button 
			getJAbortButton().setText("Abort");		// Change it 
		}
		getJAbortButton().setEnabled(true);
		setBusy(owner.getParent(), false);
		setBusy(this, false);
		isDone = false;
		if (!isVisible()) {
			//Utils.invokeLater(this, "setLocationRelativeTo", getParent());
			this.setLocationRelativeTo(getParent());
		}

		RQ.Request curr = owner.queue.getCurrent();
		if (curr != null)
			setTaskName(curr.getTaskName());

		owner.setVisible(true);
		this.setVisible(true);						// this may block!
	}

	/*
	 * Az Abort gombot Close-ra valtja (isDone = true) es engedelyezi.
	 * Torli a userBreak flag-et.
	 * Az egermutato homokora-mivoltat megszunteti, es leallitja a progressbar
	 * indeterminate animaciojat.
	 * 
	 * Bezarja az ablakot, ha kint van es nincs hibauzenet.
	 * Ha van hibauzenet, akkor kiteszi az ablakot (vagy kinthagyja) es  
	 * ennek megfeleloen meghivja az LOPProducer.setVisible(true) muveletet. 
	 * Ha az ablakot most kell kitennie, akkor beallitja a taskName-et, es
	 * nem ter vissza az ablak bezarasaig.
	 */
	/** Executes the necessary operations after the task is done. */
	public void done() {
		isDone = true;
		getJAbortButton().setEnabled(true);
		setBusy(owner.getParent(), false);
		setBusy(this, false);
		if (jProgressBar.isIndeterminate())
			jProgressBar.setIndeterminate(false);
		if (isInfo()) {
			if (!getJAbortButton().isVisible())
				getJAbortButton().setVisible(true);
													// Let the user read the info and then
			getJAbortButton().setText("Close");		// close the window manually.
			owner.setUserBreak(false);
			if (!isVisible()) {
				RQ.Request curr = owner.queue.getCurrent();
				if (curr != null)
					setTaskName(curr.getTaskName());
				owner.setVisible(true);
				setVisible(true);					// this may block!
			}
		} else {		// There's no info to read: dismiss the window
			getJAbortButton().setText("Abort");
			onClose();								// clears the userBreak flag
		}
	}

	/** Resets the title. */ 
	public void	resetTitle()					{ setTitle("Please wait..."); }
	public String	getTaskName()					{ return jTaskNameLabel.getText(); }

	public void setTaskName(String name) {
		jTaskNameLabel.setText(name);
		Dimension d = jTaskNameLabel.getPreferredSize();
		if (d != null && d.width < getMaxWidth()) updateInfo();		// repack
	}
	private int getMaxWidth() { 
		if (MAX_WIDTH == null) MAX_WIDTH = GUIUtils.GUI_unit(20, owner.getParent());
		return MAX_WIDTH;
	}

	public void onProgressUpdate(double percent, long elapsed, String left) {
		jElapsedLabel.setText("Elapsed: " + elapsedTime(elapsed));
		jEstimatedLabel.setText(left.length() > 0 ? ("Left: " + left) : "");
		if (percent < 0) {
			if (!jProgressBar.isIndeterminate()) {
				jProgressBar.setIndeterminate(true);
				jProgressBar.setString("");			// suppress "0%"-like percent display
			}
		} else {
			if (jProgressBar.isIndeterminate()) {
	 			jProgressBar.setIndeterminate(false);
				jProgressBar.setString(null);
			}
 			jProgressBar.setValue(Double.valueOf(percent).intValue());
		}
		pack();
	}
	
	/** Returns whether has information text or not. */
	public boolean isInfo() {
		return infoText.length() > 0;
	}

	/** Appends 'text' to the information text. */
	public void appendInfo(String text) {
		infoText = Utils.htmlOrPlainJoin(infoText, text, true);
		updateInfo();
	}

	/** Updates the display of the information text. */
	private void updateInfo() {
		GUIUtils.setTextPane(getJEditorPane(), infoText);
		boolean delayedPack = false;
		if (infoText.length() == 0) {
			getJScrollPane().setVisible(false);
		} else {
//			if (this.isVisible()) {
//				int width = getJScrollPane().getWidth() - Utils.getOverhead(getJScrollPane()).width;
//				Utils.setWrapLength(getJTextPane(), Math.max(width, 400));
//			} else {
//				Utils.invokeLater(this, "appendInfo", "");	// will call back updateInfo() when the window is visible
//			}
			
			delayedPack = !getJScrollPane().isVisible();
			getJScrollPane().setVisible(true);
		}
		this.pack();		// legelso alkalommal 2 pack kell: egy az ablak megjelenese elott, s meg egy utana
		if (delayedPack)
			Utils.invokeLater(this, "pack");
	}
	
	//----------------------------------------------------------------------------------------------------
	private String elapsedTime(long duration) {
		String dayPrefix = "";
		if (duration >= Progress.DAY)
			dayPrefix = String.format("%d day(s), ",duration / Progress.DAY);
		return dayPrefix + Progress.timeFmt.format(new Date(duration));
	}


	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setContentPane(getJContentPane());
		resetTitle();
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override public void windowClosing(java.awt.event.WindowEvent e) { onClose(); }
		});
		if (owner != null)	// for GUI builders
			updateInfo();
	}

	/** Releases the resources and hides the dialog. */
	private void onClose() {
		if (!isDone) {
			if (getJAbortButton().isEnabled())
				getJAbortButton().doClick(0);
			return;
		}

		owner.setUserBreak(false);
		owner.setVisible(false);
		clearInfo();
		setVisible(false);
	}

	/** Clears the information text. */
	public void clearInfo() {
		if (infoText.length() > 0) {
			infoText = "";
			updateInfo();
		}
	}
	
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BoxLayout(getJContentPane(), BoxLayout.Y_AXIS));
			jContentPane.add(getJScrollPane(), null);
			jContentPane.add(getJPanel(), null);
			jContentPane.add(getJAbortButton(), null);
			jContentPane.add(Box.createVerticalStrut(GUI_unit(0.6)));
		}
		return jContentPane;
	}

	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			jScrollPane.setMinimumSize(null);	// because of BoxLayout I have to force minSize == preferredSize
			jScrollPane.setMaximumSize(new Dimension(GUIUtils.getRelScrW(60), GUIUtils.getRelScrH(60)));
			jScrollPane.setViewportView(getJEditorPane());
		}
		return jScrollPane;
	}

	private JEditorPane getJEditorPane() {
		if (jEditorPane == null) {
			jEditorPane = new GUIUtils.SPMSAEditorPane();		// pays attention to jScrollPane's maxSize
			jEditorPane.setEditable(false);
		}
		return jEditorPane;
	}

	private JPanel getJPanel() {
		if (jPanel == null) {
			int b = GUI_unit(0.4);
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.gridx = 1;
			gridBagConstraints4.gridwidth = 1;
			gridBagConstraints4.anchor = GridBagConstraints.EAST;
			gridBagConstraints4.insets = new Insets(b, b, b, b);
			gridBagConstraints4.gridy = 2;
			jEstimatedLabel = new JLabel();
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.gridx = 0;
			gridBagConstraints3.anchor = GridBagConstraints.WEST;
			gridBagConstraints3.insets = new Insets(b, b, b, b);
			gridBagConstraints3.gridy = 2;
			jElapsedLabel = new JLabel();
			jElapsedLabel.setText(" ");
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.gridx = 0;
			gridBagConstraints1.gridwidth = 2;
			gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints1.weightx = 1.0D;
			gridBagConstraints1.insets = new Insets(0, b, 0, b);
			gridBagConstraints1.gridy = 1;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.insets = new Insets(b, b, b, b);
			gridBagConstraints.gridy = 0;
			gridBagConstraints.weightx = 1.0D;
			gridBagConstraints.anchor = GridBagConstraints.WEST;
			gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints.gridwidth = 2;
			gridBagConstraints.gridx = 0;
			jTaskNameLabel = new JLabel();
			jTaskNameLabel.setText("Current Task");
			jTaskNameLabel.setAlignmentX(0.0f);
			jPanel = new JPanel();
			jPanel.setLayout(new GridBagLayout());
			jPanel.setAlignmentX(0.5f);
			jPanel.add(jTaskNameLabel, gridBagConstraints);
			jPanel.add(getJProgressBar(), gridBagConstraints1);
			jPanel.add(jElapsedLabel, gridBagConstraints3);
			jPanel.add(jEstimatedLabel, gridBagConstraints4);
			jPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, jPanel.getPreferredSize().height));
		}
		return jPanel;
	}

	private JProgressBar getJProgressBar() {
		if (jProgressBar == null) {
			jProgressBar = new JProgressBar();
			jProgressBar.setAlignmentX(0.0f);
			jProgressBar.setStringPainted(true);
 			jProgressBar.setMinimum(0);
 			jProgressBar.setMaximum(100);
 			Dimension d = jProgressBar.getPreferredSize();
 			if (d.width < GUI_unit(15)) {
 				d.width = GUI_unit(15);
 				jProgressBar.setPreferredSize(d);
 			}
		}
		return jProgressBar;
	}

	JButton getJAbortButton() {
		if (jAbortButton == null) {
			jAbortButton = new JButton();
			jAbortButton.setText("Abort");
			jAbortButton.setAlignmentX(0.5f);
			jAbortButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					if (isDone) {	// Close button
						onClose();
					} else {		// Abort button
						getJAbortButton().setEnabled(false);
						owner.setUserBreak(true);
						setBusy(LOPDialog.this, true);
						setBusy(owner.getParent(), true);
					}
				}
			});
		}
		return jAbortButton;
	}
	
	
}
