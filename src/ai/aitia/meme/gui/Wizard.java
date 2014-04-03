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
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.EnumMap;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import org.jdesktop.jdic.init.JdicInitException;

import com.jgoodies.forms.util.LayoutStyle;

import ai.aitia.meme.events.Event;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;

/** General GUI component for creating wizards. A wizard has an information
 *  panel on the top of it, and a buttons panel on the bottom of it with buttons
 *  Back, Next, Finish and Cancel. A wizard contains pages which appears in the 
 *  center panel.  
 */
public class Wizard extends JPanel implements java.awt.event.ActionListener 
{
	private static final long serialVersionUID = 1L;
	/** Recorder class of buttons. */
	private static class ButtonRec {
		final JButton		button;
		public ButtonRec(JButton b) { button = b; }
	}
	/** This object is maps button types to the GUI-components of buttons. */
	private EnumMap<Button, ButtonRec> buttonInfo = new EnumMap<Button, ButtonRec>(Button.class);
	/** The pages of the wizard. */
	protected ArrayList<IWizardPage> pages = new ArrayList<IWizardPage>();
	/** The titles of the pages. */
	private ArrayList<String> pageTitles = null;
	/** The index of the current page. */
	protected int	current = -1;
	/** Flag that determines whether the user cancels the wizard or not. */
	private boolean canceling = false;

	private JPanel jButtonsPanel = null;
	private JButton jBackButton = null;
	private JButton jNextButton = null;
	private JButton jFinishButton = null;
	private JButton jCancelButton = null;
	private JScrollPane jInfoScrollPane = null;
	private JTextPane jInfoTextPane = null;

	protected Action customButton = null;
	
	//=========================================================================
	//	Public types and members

	//-------------------------------------------------------------------------
	/** Enum type for representing the navigation buttons. */
	public static enum Button {
		BACK, NEXT, FINISH, CANCEL, CUSTOM
	};

	//-------------------------------------------------------------------------
	/** Interface for representing a page of the wizard. */
	public interface IWizardPage {
		/** Returns the GUI component of the page. */
		java.awt.Container	getPanel();
		/** Returns the short description of the page. */
		String				getInfoText(Wizard w);

		/** It returns whether the page allows to be enabled the button 'b' or not. 
		 *  This method is consulted by enableDisableButtons() for every button. */
		boolean				isEnabled(Button b);

		/**
		 * This method should return false if the button should do nothing.
		 * The FINISH or CANCEL buttons, when used, are triggered for every page,
		 * starting at the current page, to determine whether the wizard should 
		 * be closed. If all pages return true, then FINISH/CANCEL is accepted,
		 * otherwise refused.
		 */
		boolean				onButtonPress(Button b);

		/**
		 * This method is called when a new page is displayed.   
		 * The current page is messaged with show=false before it is made invisible.
		 * The new page is messaged with show=true after it is made visible. 
		 */
		void				onPageChange(boolean show);
	}

	//-------------------------------------------------------------------------
	/** Interface for pages that wants to display their title and positions on the
	 *  information panel in the following form:<br>
	 *  Page1 -> <b>Page2</b> -> Page3<br> */
	public interface IArrowsInHeader {
		/** Returns the title of the page. */
		String				getTitle();
	}

	//-------------------------------------------------------------------------
	/** Storage for listeners which observe wizard closing events.
	 *  The owner (container) of this Wizard should register at this event as listener.
	 */ 
	public final CloseListeners onClose = new CloseListeners();

	//-------------------------------------------------------------------------
	/** Interface for listening wizard closing events. */
	public interface ICloseListener {
		/** 
		 * The Wizard calls this method when Finish/Cancel has been accepted.
		 * It is responsible for closing the 'w' wizard. w.isCanceling() tells 
		 * the reason of closing.   
		 * When the wizard is displayed in a separate window, the window-closing 
		 * 'x' button is equivalent to Cancel, and this method is called only if
		 * Cancel is accepted. 
		 */
		void	onClose(Wizard w);
	}

	//-------------------------------------------------------------------------
	/** This event is fired after the wizard accepts the Finish/Cancel button click.
     *  */   
	public class CloseListeners extends Event<ICloseListener, Wizard> {
		CloseListeners() { super(ICloseListener.class, Wizard.class); }
		protected void fire() { super.fire(Wizard.this); }
	}


	//=========================================================================
	//	Public methods
	
	//-------------------------------------------------------------------------
	/**
	 * This is the default constructor
	 */
	public Wizard() {
		super();
		initialize();
		buttonInfo.put(Button.BACK,   new ButtonRec(getJBackButton()));
		buttonInfo.put(Button.NEXT,   new ButtonRec(getJNextButton()));
		buttonInfo.put(Button.FINISH, new ButtonRec(getJFinishButton()));
		buttonInfo.put(Button.CANCEL, new ButtonRec(getJCancelButton()));
		enableDisableButtons();
	}

	//-------------------------------------------------------------------------
	public Wizard(IWizardPage... pages) {
		this();
		for (int i = 0; i < pages.length; ++i)
			addPage(pages[i]);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setCustomButton(Action customButton) {
		this.customButton = customButton;
		if (this.customButton == null)
			buttonInfo.put(Button.CUSTOM,null);
		else
			buttonInfo.put(Button.CUSTOM,new ButtonRec(new JButton(this.customButton)));
		jButtonsPanel = null;
		initialize();
		enableDisableButtons();
	}
	
	//-------------------------------------------------------------------------
	/** Returns a dialog that contains the wizard. */
	public JDialog showInDialog(java.awt.Window parent) {
		return showInDialog(this, parent);
	}

	//-------------------------------------------------------------------------
	/** Returns a not-yet-visible JDialog, in which contentPane is the specified Wizard. */  
	public static JDialog showInDialog(final Wizard w, java.awt.Window parent) {
		final JDialog ans = GUIUtils.createDialog(parent, true, "Wizard", w);
		final java.awt.event.WindowListener wl = new java.awt.event.WindowAdapter() {
			@Override public void windowClosing(java.awt.event.WindowEvent e) { w.doClose(false); }
		};
		ans.addWindowListener(wl);
		w.onClose.addListener(new ICloseListener() {	// it cannot be a weak listener because it would be destroyed immediately  
			public void onClose(Wizard source) {
				ans.setVisible(false); ans.dispose();
				w.onClose.removeListener(this);
				// The followings are necessary because Swing often retains a reference 
				// on the last displayed JDialog, which, in this case, prevents this  
				// wizard object (and all its subcomponents, the pages) from being 
				// finalized. This can have serious effects due to weak listeners...
				//
				ans.removeWindowListener(wl);
				ans.setContentPane(new JPanel());
			}
		});
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Adds page 'page' to the wizard.
	 * @return page
	 */
	public <T extends IWizardPage> T addPage(T page) {
		if (page instanceof ICloseListener)
			onClose.addListener((ICloseListener)page);
		pageTitles = null;

		int idx = pages.indexOf(page);
		if (idx >= 0) {
			pages.set(idx, page);
		} else {
			pages.add(page);
			if (pages.size() == 1)
				gotoPage(0);
			else
				enableDisableButtons();
		}
		return page;
	}

	//-------------------------------------------------------------------------
	/** Returns the list of the pages of the wizard. */
	public java.util.List<IWizardPage> getPages() {
		return java.util.Collections.unmodifiableList(pages);
	}

	//-------------------------------------------------------------------------
	/** The wizard shows the i-th page. */
	public void gotoPage(int i) {
		int j = (0 <= i && i < pages.size()) ? i : -1;
		if (j == current)
			return;

		boolean revalidate = false;
		if (current >= 0) {
			pages.get(current).onPageChange(false);
			java.awt.Container p = pages.get(current).getPanel();
			this.remove(p);
			revalidate = true;
			p.setVisible(false);
		}
		current = j;
		if (current >= 0) {
			java.awt.Container p = pages.get(current).getPanel();
			this.add(p, BorderLayout.CENTER);
			p.setVisible(true);
			revalidate = true;
			pages.get(current).onPageChange(true);
		}		
		enableDisableButtons();
		updateInfo();
		if (revalidate)
			this.validate();
	}
	
	//-------------------------------------------------------------------------
	/** The wizard shows page 'page' (if contains). */
	public void gotoPage(IWizardPage page) {
		int idx = pages.indexOf(page);
		if (idx >= 0) gotoPage(idx);
	}

	//-------------------------------------------------------------------------
	/** The wizard shows the page that is in 'delta' distance from the current page. */
	public void gotoPageRel(int delta) {
		int i = current + delta;
		if (0 <= i && i < pages.size())
			gotoPage(i);
	}
	/** The wizard shows the previous page. */
	public void back()				{ gotoPageRel(-1); }
	/** The wizard shows the next page. */
	public void next()				{ gotoPageRel(+1); }
	/** Does the same than the Finish button. */
	public void finish()			{ doClose(true); }
	/** Does the same than the Cancel button. */
	public void cancel()			{ doClose(false); }

	//-------------------------------------------------------------------------
	/** Updates the content of the information panel. */
	public void updateInfo() {
		String s = (current >= 0) ? pages.get(current).getInfoText(this) : "";
		GUIUtils.setTextPane(getJInfoTextPane(), s == null ? "" : s);
//		this.revalidate();
//		getJInfoTextPane().revalidate();
	}

	//-------------------------------------------------------------------------
	/** Returns the header line (titles with arrows). */
	public String getArrowsHeader()	 {
		return getArrowsHeader(null);
	}
	/** Returns the header line (titles with arrows) and the parameter in a well formed
	 *  string. */
	public String getArrowsHeader(String secondLine) {
		return getArrowsHeader( current < 0 ? null : pages.get(current), secondLine );
	}
	/** Returns the header line (titles with arrows) and parameter 'secondLine'in a well
	 *  formed string. It creates the header line using page 'page' as current page. */
	public String getArrowsHeader(Object page, String secondLine) {
		if (page == null)
			return "";
		if (pageTitles == null) {
			pageTitles = new ArrayList<String>(pages.size());
			for (int i = 0; i < pages.size(); ++i) {
				Object p = pages.get(i);
				String o = (p instanceof IArrowsInHeader) ? ((IArrowsInHeader)p).getTitle() : null;
				pageTitles.add(o == null ? "" : o);
			}
		}
		StringBuilder ans = new StringBuilder("<b>");
		boolean dimmed = false;
		for (int i = 0; i < pageTitles.size(); ++i) {
			Object ip = pages.get(i);
			if (!dimmed && (i > 0 || ip != page)) {
				ans.append("<font color=silver>");
				dimmed = true;
			}
			if (i > 0) ans.append(" \u2192 ");
			if (ip == page && dimmed) {
				ans.append("</font>");
				dimmed = false;
			}
			ans.append(pageTitles.get(i));
		}
		if (dimmed) ans.append("</font>");
		ans.append("</b>");
		if (secondLine != null) {
			ans.append("<br><br>");
			ans.append(secondLine);
		}
		return Utils.htmlPage(ans.toString());
	}

	//-------------------------------------------------------------------------
	/** Returns whether button 'b' is enabled or not. */
	public boolean isEnabled(Button b) {
		JButton butt = getButton(b);
		return (butt != null) && butt.isEnabled();
	}

	//-------------------------------------------------------------------------
	/**
	 * Pressing the Cancel button may cause a focus-lost on a JTextComponent
	 * of the current page, which may trigger an InputVerifier, which should
	 * usually <i>skip</i> the verification in this case and let the focus go.
	 * This function allows detecting this case.
	 */
	public boolean isCanceling() {
		if (canceling)
			return true;
		JButton butt = getButton(Button.CANCEL);
		return (butt != null) && butt.getModel().isArmed();
	}

	//-------------------------------------------------------------------------
	/**
	 * When a button-press triggers an InputVerifier, which in turn displays a
	 * modal dialog, the button remains in a half-pressed, inconsistent state.
	 * This method allows correcting it for the Wizard's buttons (back/next/...).
	 * It should be called from the InputVerifier code, just before displaying 
	 * the dialog.
	 * Note that other buttons (which are contained on the page) cannot be
	 * corrected with this method.
	 */
	public void disarmAllButtons() {
		for (ButtonRec br : buttonInfo.values()) {
			javax.swing.ButtonModel m = br.button.getModel();
			m.setArmed(false);
			m.setPressed(false);
		}
	}

	//-------------------------------------------------------------------------
	/**
	 * Updates the status of the Wizard's buttons (back/next/...).
	 * Enables the FINISH button only if all pages enable it (the current page is asked first).
	 * Enables the other buttons only if the current page enables that button.
	 */
	public void enableDisableButtons() {
		getButton(Button.BACK  ).setEnabled( (0 < current) && pages.get(current).isEnabled(Button.BACK) );

		getButton(Button.NEXT  ).setEnabled( !pages.isEmpty() && (current < pages.size() - 1) &&
											 (current < 0 || pages.get(current).isEnabled(Button.NEXT)) );

		getButton(Button.CANCEL).setEnabled( current < 0 || pages.get(current).isEnabled(Button.CANCEL) );

		boolean enabled = true;
		for (IWizardPage p : askCurrentPageFirst()) {
			if (!p.isEnabled(Button.FINISH)) { enabled = false; break; }
		}
		getButton(Button.FINISH).setEnabled(enabled);
		if (customButton != null) {
			enabled = true;
			for (IWizardPage p : askCurrentPageFirst()) {
				if (!p.isEnabled(Button.CUSTOM)) { enabled = false; break; }
			}
			getButton(Button.CUSTOM).setEnabled(enabled);
		}
	}

	//-------------------------------------------------------------------------
	/**
	 * Asks every page, starting at the current one, whether or not the wizard can be closed.
	 * If it can be closed, fires the 'onClose' event and returns true. Otherwise returns false.  
	 */
	public boolean doClose(boolean isFinish) {
		boolean saved_canceling = canceling; 
		try {
			canceling = !isFinish;
			Button b = isFinish ? Button.FINISH : Button.CANCEL;
			for (IWizardPage p : askCurrentPageFirst()) {
				if (!p.onButtonPress(b)) return false;
			}
			if (!canClose())
				return false;

			onClose.fire();

			// due to swing memory leaks, break all connection with JTextPane objects
			if (jInfoScrollPane != null) {
				jInfoScrollPane.removeAll();
				jInfoScrollPane = null;
			}
			this.removeAll();

			return true;
		} finally {
			canceling = saved_canceling;
		}
	}

	//=========================================================================
	//	Controller methods

	//-------------------------------------------------------------------------
	/** 
	 * This method is provided for descendant classes: it can be used to veto
	 * wizard closing after the FINISH/CANCEL button has been accepted.
	 */
	protected boolean canClose() {
		return true;
	}

	//-------------------------------------------------------------------------
	/** Returns the pages of this wizard in a modified order: the current page is the first.
	 */
	ArrayList<IWizardPage> askCurrentPageFirst() {
		if (current <= 0) return pages;
		ArrayList<IWizardPage> ans = new ArrayList<IWizardPage>(pages);
		ans.add(0, ans.remove(current));
		return ans;
	}

	//-------------------------------------------------------------------------
	/**
	 * Convenience implementation for InputVerifier:
	 * - does not verifies the string when focus-lost is caused by the Cancel button
	 * - does not verifies the empty string (except in strict mode, see the 'strict' variable)
	 * - remembers the last verified string, and doesn't verifies again while unchanged
	 * - automatically calls Wizard.enableDisableButtons()
	 * - works even if not the text component triggers it     
	 */
	public abstract class InputVerifier extends javax.swing.InputVerifier {
		protected String lastExamined = null;
		protected boolean lastResult = false;
		protected boolean strict = false;

		/** Text of the text component. */
		public String getText(JComponent input) {
			return ((javax.swing.text.JTextComponent)input).getText();
		}

		@Override
		public boolean shouldYieldFocus(JComponent input) {
			// Do not verify if focus is caused by the Cancel button
			if (isCanceling())
				return true;

			// Do not verify again the same string that was verified last time 
			String current = getText(input);
			if (current.equals(lastExamined))
				return lastResult;

			setLastExamined(current);
			if (!strict && current.length() == 0)
				// Skip the verification of the empty string (except in strict mode)
				// but not the updating of buttons' status
				lastResult = true;
			else {
				// Verify it
				lastResult = super.shouldYieldFocus(input);
			}
			enableDisableButtons();		// update buttons' status 
			return lastResult;
		}

		public String setLastExamined(String s) {
			String ans = lastExamined;
			lastExamined = s;
			return ans;
		}
	}

	//-------------------------------------------------------------------------
	public void actionPerformed(java.awt.event.ActionEvent e) {
		if (e.getSource() == getJBackButton()) {
			if (current < 0 || pages.get(current).onButtonPress(Button.BACK)) {
				gotoPage(current - 1);
			}
		}
		else if (e.getSource() == getJNextButton()) {
			if (current < 0 || pages.get(current).onButtonPress(Button.NEXT)) {
				gotoPage(current + 1);
			}
		}
		else if (e.getSource() == getJFinishButton()) {
			finish();
		}
		else if (e.getSource() == getJCancelButton()) {
			cancel();
		}
	}

	//=========================================================================
	//	GUI (View) methods

	/**
	 * This method initializes this
	 * @return void
	 */
	private void initialize() {
		this.setSize(GUIUtils.GUI_unit(20), GUIUtils.GUI_unit(12));
		this.setMaximumSize(new Dimension(Integer.MAX_VALUE, GUIUtils.getScreenSize().height - 50));
		this.setLayout(new BorderLayout());

		// Draw a separator between the top info panel and the following body
		Box tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(getJInfoScrollPane());
		tmp.add(new javax.swing.JSeparator());
		this.add(tmp, BorderLayout.NORTH);

		// Draw a separator between the body and the bottom buttons panel
		tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(new javax.swing.JSeparator());
		tmp.add(getJButtonsPanel());
		this.add(tmp, BorderLayout.SOUTH);
	}

	//-------------------------------------------------------------------------
	private JButton getButton(Button b) {
		return buttonInfo.get(b).button;
	}

	//-------------------------------------------------------------------------
	@Override
	public Dimension getPreferredSize() {
		Dimension ans = super.getPreferredSize();
		if (isMinimumSizeSet()) {
			if (ans == null)
				ans = getMinimumSize();
			else
				GUIUtils.enlarge(ans, getMinimumSize());
		}
		// plus = height of info panel + buttons panel
		// tmp = body height 
		Dimension plus = new Dimension(ans), tmp = new Dimension(ans);
		plus.height = getJInfoScrollPane().getPreferredSize().height + getJButtonsPanel().getPreferredSize().height;
		tmp.height = Math.max(ans.height - plus.height, 1);
		for (IWizardPage page : pages) {
			java.awt.Container p = page.getPanel();
			GUIUtils.enlarge(tmp, p.getMinimumSize());
			GUIUtils.enlarge(tmp, p.getPreferredSize());
		}
		tmp.height += plus.height;	// body height + info + buttons
		GUIUtils.enlarge(ans, tmp);
		if (isMaximumSizeSet())
			GUIUtils.shrink(ans, getMaximumSize());
		return ans;
	}

	private JPanel getJButtonsPanel() {
		if (jButtonsPanel == null) {
			int b = GUIUtils.GUI_unit(2);
			int bb = LayoutStyle.getCurrent().getDialogMarginX().getPixelSize(this);
			jButtonsPanel = new JPanel();
			jButtonsPanel.setLayout(new BoxLayout(getJButtonsPanel(), BoxLayout.X_AXIS));
			jButtonsPanel.setBorder(BorderFactory.createEmptyBorder(b/6,customButton == null ? b : bb,b/6,b));
			if (customButton != null)
			{
				JButton JCustomButton = getButton(Button.CUSTOM);
				JCustomButton.setName("btn_wizard_savewizard");
				jButtonsPanel.add(JCustomButton);
			}
			jButtonsPanel.add(Box.createHorizontalGlue());
			jButtonsPanel.add(getJBackButton(), null);
			jButtonsPanel.add(getJNextButton(), null);
			jButtonsPanel.add(Box.createHorizontalStrut(b/6));
			jButtonsPanel.add(getJFinishButton(), null);
			jButtonsPanel.add(Box.createHorizontalStrut(b/6));
			jButtonsPanel.add(getJCancelButton(), null);
		}
		return jButtonsPanel;
	}
	
	protected JButton getJBackButton() {
		if (jBackButton == null) {
			jBackButton = new JButton();
			jBackButton.setName("btn_back");
			jBackButton.setText("< Back");
			jBackButton.addActionListener(this);
		}
		return jBackButton;
	}

	protected JButton getJNextButton() {
		if (jNextButton == null) {
			jNextButton = new JButton();
			jNextButton.setName("btn_next");
			jNextButton.setText("Next >");
			jNextButton.addActionListener(this);
		}
		return jNextButton;
	}

	protected JButton getJFinishButton() {
		if (jFinishButton == null) {
			jFinishButton = new JButton();
			jFinishButton.setName("btn_finish");
			jFinishButton.setText("Finish");
			jFinishButton.addActionListener(this);
		}
		return jFinishButton;
	}

	protected JButton getJCancelButton() {
		if (jCancelButton == null) {
			jCancelButton = new JButton();
			jCancelButton.setName("btn_cancel");
			jCancelButton.setText("Cancel");
			jCancelButton.addActionListener(this);
		}
		return jCancelButton;
	}

	public JScrollPane getJInfoScrollPane() {
		if (jInfoScrollPane == null) {
			jInfoScrollPane = new JScrollPane();
			jInfoScrollPane.setViewportView(getJInfoTextPane());
			jInfoScrollPane.setBorder(null);
		}
		return jInfoScrollPane;
	}

	private JTextPane getJInfoTextPane() {
		if (jInfoTextPane == null) {
			jInfoTextPane = new JTextPane();
			jInfoTextPane.setEditable(false);
			int b = GUIUtils.GUI_unit(0.5);
			jInfoTextPane.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
		}
		return jInfoTextPane;
	}
}
