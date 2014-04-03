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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import ai.aitia.meme.events.Event;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.Utils.Pair;

/** General GUI component for creating Preferences dialog (a place where all
 *  settings of an application can be accessible). A  Preferences component
 *  has an information panel on the top of it, and a buttons panel on the bottom
 *  of it with buttons Ok and Cancel.  A Preferences component contains a structure 
 *  of pages which appears in the center panel. There is tree on the left side where
 *  the page structure is displayed and it can be used to navigate between the pages.  
 */
public abstract class Preferences extends JPanel implements ActionListener,
												            TreeSelectionListener {

	//=====================================================================================
	// nested classes, types and interfaces
	
	//-------------------------------------------------------------------------------------
	/** Recorder class of buttons. */
	private static class ButtonRec {
		final JButton button;
		public ButtonRec(JButton b) { button = b; }
	}
	
	//-------------------------------------------------------------------------------------
	/** Enum type for representing the buttons of the component. */
	public static enum Button { OK, CANCEL };
	
	//-------------------------------------------------------------------------------------
	/** Interface for representing a page of the Preferences component. */
	public interface IPreferencesPage {
		/** Returns the GUI component of the page. */
		java.awt.Container	getPanel();
		
		/** Returns the short description of the page. */
		String getInfoText(Preferences p);
		
		/** Returns the title of the page. toString() may return the same string. */
		String getTitle(Preferences p);

		/** It returns whether the page allows to be enabled the button 'b' or not.
		 *  This method is consulted by enableDisableButtons() for every button. */
		boolean isEnabled(Button b);

		/** This method should return false if the button should do nothing.
		 *  The OK or CANCEL buttons, when used, are triggered for every page,
		 *  starting at the current page, to determine whether the component should 
		 *  be closed. If all pages return true, then OK/CANCEL is accepted,
		 *  otherwise refused.
		 */
		boolean	onButtonPress(Button b);

		/** This method is called when a new page is displayed.   
		 *  The current page is messaged with show=false before it is made invisible.
		 *  The new page is messaged with show=true after it is made visible. 
		 */
		void onPageChange(boolean show);
		
		/** Never returns 'null'. It returns empty list if there is no subpages.
		 *  The result must be a modifiable list. */ 
		List<IPreferencesPage> getSubpages(Preferences p);
	}
	
	//-------------------------------------------------------------------------------------
	/** Interface for listening Preferences component closing events. */
	public interface ICloseListener {
		/** The Preferences calls this method when OK/Cancel has been accepted.
		 *  It is responsible for closing the 'p' preferences. (It should call
		 *  p.onClose() to release resources.) p.isCanceling() tells 
		 *  the reason of closing.   
		 *  When the preferences is displayed in a separate window, the window-closing 
		 *  'x' button is equivalent to Cancel, and this method is called only if
		 *  Cancel is accepted. 
		 */
		void onClose(Preferences p);
	}
	
	//------------------------------------------------------------------------------------
	/** This event is fired after the Preferences component accepts the OK/Cancel button
	 *  click. */   
	public class CloseListeners extends Event<ICloseListener,Preferences> {
		CloseListeners() { super(ICloseListener.class,Preferences.class); }
		protected void fire() { super.fire(Preferences.this); } 
	}
	
	//------------------------------------------------------------------------------------
	/** Class for iterating the pages (and subpages) of the Preferences component.
	 *  It implements the following strategy:<br>
	 *  <ul>
	 *  <li>It uses a stack as an internal storage. </li>
	 *  <li>First it puts to the stack all pages of the component. </li>
	 *  <li>When the user calls next(), it takes the top page from the 
	 *      stack then puts all subpages of this page instead. </li>
	 *  </ul>
	 */
	public class PageIterator implements Iterator<IPreferencesPage> {
		private Stack<IPreferencesPage> stack = null;
		public PageIterator() {
			stack = new Stack<IPreferencesPage>();
			for (IPreferencesPage page : mainPages)
				stack.push(page);
		}
		public boolean hasNext() { return !stack.empty(); }
		public IPreferencesPage next() {
			IPreferencesPage back = stack.pop();
			for (IPreferencesPage page : back.getSubpages(Preferences.this))
				stack.push(page);
			return back;
		}
		public void remove() { throw new UnsupportedOperationException(); }
	}
	
	//------------------------------------------------------------------------------------
	/** Convenience implementation for InputVerifier:
	 *  - does not verifies the string when focus-lost is caused by the Cancel button<br>
	 *  - does not verifies the empty string (except in strict mode, see the 'strict' variable)<br>
	 *  - remembers the last verified string, and doesn't verifies again while unchanged<br>
	 *  - automatically calls Preferences.enableDisableButtons()<br>
	 *  - works even if not the text component triggers it     
	 */
	public abstract class InputVerifier extends javax.swing.InputVerifier {
		protected String lastExamined = null;
		protected boolean lastResult = false;
		protected boolean strict = false;

		/** Returns the text contained by <code>input</code>. */
		public String getText(JComponent input) {
			return ((javax.swing.text.JTextComponent)input).getText();
		}

		@Override public boolean shouldYieldFocus(JComponent input) {
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
	
	//=====================================================================================
	// members
	
	private static final long serialVersionUID = 1L;
	
	/** The duration of displaying warning messages. */
	private static final int PROBLEM_TIMEOUT = 3 * 1000;

	/** Message type constant: represents normal messages. */
	public static final int MESSAGE	= 0;
	/** Message type constant: represents warning messages. */
	public static final int WARNING	= 1;
	
	/** This object is maps button types to the GUI-components of buttons. */
	private EnumMap<Button,ButtonRec> buttonInfo  = new EnumMap<Button,ButtonRec>(Button.class);
	/** The storage of the pages contained by the component directly. */ 
	protected ArrayList<IPreferencesPage> mainPages = new ArrayList<IPreferencesPage>();
	/** The currently selected page. */
	protected IPreferencesPage currentPage = null;
	/** Flag that determines whether the user cancels the component or not. */
	private boolean canceling = false;
	/** A pair that contains a message for the user. The Integer part identifes the type
	 *  of the message.
	 */ 
	private Pair<String,Integer> warning = null;
	/** Timer. */
	private Utils.TimerHandle warningTimer = null;

	
	/** Storage for listeners which observe wizard closing events.
	 *  The owner (container) of this Preferences should register at this event as listener.
	 */ 
	public final CloseListeners onClose = new CloseListeners();

	
	//=====================================================================================
	// GUI members
	
	private DefaultMutableTreeNode root = new DefaultMutableTreeNode();

	private JPanel buttonsPanel = null;
	private JPanel pagePanel = null;
	private JButton okButton = new JButton("OK");
	private JButton cancelButton = new JButton("Cancel");
	private JButton saveButton = new JButton("Save preferences...");
	private JButton loadButton = new JButton("Load preferences...");
	private JTextPane infoTextPane = new JTextPane();
	private JScrollPane infoScrollPane = new JScrollPane(infoTextPane);
	private JTree pageTree = new JTree(root);
	private JScrollPane treeScrollPane = new JScrollPane(pageTree);
	
	
	//=====================================================================================
	// methods
	
	//------------------------------------------------------------------------------------
	/** Constructor. */
	public Preferences() {
		super();
		layoutGUI();
		initialize();
		buttonInfo.put(Button.OK,new ButtonRec(okButton));
		buttonInfo.put(Button.CANCEL,new ButtonRec(cancelButton));
		enableDisableButtons();
	}
	
	//------------------------------------------------------------------------------------
	/** Returns a dialog that contains the Preferences component.
	 * @param parent the parent of the dialog
	 */
	public JDialog showInDialog(java.awt.Window parent) { return showInDialog(this,parent); }
	
	//------------------------------------------------------------------------------------
	/** Returns a not-yet-visible JDialog, in which contentPane is the specified Preferences
	 *  component.
	 * @param p the Preferences component that must be displayed
	 * @param parent the parent of the dialog
	 */  
	public static JDialog showInDialog(final Preferences p, java.awt.Window parent) {
		final JDialog ans = GUIUtils.createDialog(parent,true,"Preferences",p);
		ans.setName("dial_preferences");
		ans.setLocationRelativeTo(parent);
		final java.awt.event.WindowListener wl = new java.awt.event.WindowAdapter() {
			@Override public void windowClosing(java.awt.event.WindowEvent e) { p.doClose(false); }
		};
		ans.addWindowListener(wl);
		p.onClose.addListener(new ICloseListener() {
			public void onClose(Preferences source) {
				p.onClose();
				ans.setVisible(false); ans.dispose();
				p.onClose.removeListener(this);
				// The followings are necessary because Swing often retains a reference 
				// on the last displayed JDialog, which, in this case, prevents this  
				// wizard object (and all its subcomponents, the pages) from being 
				// finalized. This can have serious effects due to weak listeners...
				ans.removeWindowListener(wl);
				ans.setContentPane(new JPanel());
			}
		});
		return ans;
	}
	
	//------------------------------------------------------------------------------------
	/** Adds page <code>page</code> as a main page to the component. If <code>page<code>
	 *  implements the {@link Preferences.ICloseListener ICloseListener} the method
	 *  automatically registers it to the storage of the listeners. 
	 * @return page
	 */
	public <T extends IPreferencesPage> T addPage(T page) {
		if (page instanceof ICloseListener)
			onClose.addListener((ICloseListener)page);
		int idx = mainPages.indexOf(page);
		DefaultMutableTreeNode pageNode = null;
		if (idx >= 0) {
			mainPages.set(idx,page);
			pageNode = findNode(root,page);
			if (pageNode == null) 
				throw new IllegalStateException("inconsistent tree");
			pageNode.setUserObject(page);
		} else {
			mainPages.add(page);
			pageNode = new DefaultMutableTreeNode();
			pageNode.setUserObject(page);
			root.add(pageNode);
			if (mainPages.size() == 1) {
				gotoPage(0);
				TreePath path = new TreePath(pageNode);
				pageTree.setSelectionPath(path);
			} else
				enableDisableButtons();
		}
		fireTreeStructureChanged(new TreePath(pageNode));
		pageTree.expandRow(0);
		initSize();
		return page;
	}
	
	//------------------------------------------------------------------------------------
	/** Adds page <code>page</code> as a subpage to page <code>parent</code>. If <code>page<code>
	 *  implements the {@link Preferences.ICloseListener ICloseListener} the method
	 *  automatically registers it to the storage of the listeners.<br>
	 * @return page
	 * @throws IllegalArgumentException if <code>parent</code> is not part of the page structure 
	 */
	public <T extends IPreferencesPage> T addSubpage(IPreferencesPage parent, T page) {
		if (page instanceof ICloseListener)
			onClose.addListener((ICloseListener)page);
		DefaultMutableTreeNode parentNode = findParent(parent);
		if (parentNode == null)
			throw new IllegalArgumentException("'parent' is not part of the tree");
		DefaultMutableTreeNode pageNode = null;
		List<IPreferencesPage> pages = parent.getSubpages(this);
		int idx = pages.indexOf(page);
		if (idx >= 0) {
			pages.set(idx,page);
			pageNode = findNode(parentNode,page);
			if (pageNode == null)
				throw new IllegalStateException("inconsistent tree");
			pageNode.setUserObject(page);
		} else {
			pages.add(page);
			pageNode = new DefaultMutableTreeNode();
			pageNode.setUserObject(page);
			parentNode.add(pageNode);
			enableDisableButtons();
		}
		fireTreeStructureChanged(new TreePath(parentNode.getPath()).pathByAddingChild(pageNode));
		initSize();
		return page;
	}
	
	//------------------------------------------------------------------------------------
	/** Adds page <code>page</code> as a subpage to page identified by <code>path</code>.
	 *  If <code>page<code> implements the {@link Preferences.ICloseListener ICloseListener} the method
	 *  automatically registers it to the storage of the listeners.<br>
	 * @param path a list of indices. For example 0,2 means the third (0,1,2) subpage of the first main page. 
	 * @return page or 'null' if <code>path</code> is invalid.
	 * @throws IllegalArgumentException if <code>parent</code> is not part of the page structure 
	 */
	public <T extends IPreferencesPage> T addSubpage(List<Integer> path, T page) {
		if (path == null) return null;
		try {
			IPreferencesPage parent = mainPages.get(path.get(0));
			for (int i = 1;i < path.size();++i)
				parent = parent.getSubpages(this).get(path.get(i));
			return addSubpage(parent,page);
		} catch (IndexOutOfBoundsException e) { return null; }
	}
	
	//------------------------------------------------------------------------------------
	public java.util.List<IPreferencesPage> getMainPages() { return java.util.Collections.unmodifiableList(mainPages); }
	
	//------------------------------------------------------------------------------------
	// just for main pages
	/** The Preferences component shows the i-th main page. */
	public void gotoPage(int i) {
		int j = (0 <= i && i < mainPages.size()) ? i : -1;
		if (j != -1 && mainPages.get(j) == currentPage)
			return;
		
		boolean revalidate = false;
		if (currentPage != null) {
			currentPage.onPageChange(false);
			java.awt.Container p = currentPage.getPanel();
			pagePanel.remove(p);
			revalidate = true;
			p.setVisible(false);
		}
		currentPage = (j == -1) ? null : mainPages.get(j);
		if (currentPage != null) {
			java.awt.Container p = currentPage.getPanel();
			pagePanel.add(p,BorderLayout.CENTER);
			p.setVisible(true);
			revalidate = true;
			currentPage.onPageChange(true);
		}
		enableDisableButtons();
		updateInfo();
		selectPage( i + 1 );
		if (revalidate)
			this.validate();
	}
	
	//------------------------------------------------------------------------------------
	/** The Preferences component shows page <code>page</code> (if the page structure
	 *  contains it).
	 */
	public void gotoPage(IPreferencesPage page) {
		if (page == null)
			return;
		
		boolean revalidate = false;
		if (currentPage != null) {
			currentPage.onPageChange(false);
			java.awt.Container p = currentPage.getPanel();
			pagePanel.remove(p);
			revalidate = true;
			p.setVisible(false);
		}
		currentPage = page;
		if (currentPage != null) {
			java.awt.Container p = currentPage.getPanel();
			pagePanel.add(p,BorderLayout.CENTER);
			p.setVisible(true);
			revalidate = true;
			currentPage.onPageChange(true);
		}
		enableDisableButtons(); 
		updateInfo();
		if (revalidate)
			this.validate();
	}
	
	//------------------------------------------------------------------------------------
	/** Does the same than the OK button. */
	public void ok() { doClose(true); }
	/** Does the same than the Cancel button. */
	public void cancel() { doClose(false); }
	
	protected abstract void save();
	protected abstract void load();
	
	//------------------------------------------------------------------------------------
	/** Returns the actual content of the information panel. This can be the short decription
	 *  of the current page or a warning/message to the user. On the other hand the result always contains
	 *  the title of the current page. */
	public String getInfoText()	{
		String s = "";
		if (currentPage != null) 
			s = "<b>" + (currentPage.getTitle(this) == null ? "" : currentPage.getTitle(this)) + "</b><br>";
		if (warning != null) {
			s += warning.getSecond().intValue() == WARNING ? "<br><img src=\"gui/icons/warning.png\">&nbsp;&nbsp;" : ""; 
			s += Utils.htmlQuote(warning.getFirst());
		} else if (currentPage != null && currentPage.getInfoText(this) != null)
				s += "<br>" + currentPage.getInfoText(this);
		return Utils.htmlPage(s);
	}
	
	//------------------------------------------------------------------------------------
	/** Updates the content of the information panel. */
	public void updateInfo() {
		String s = getInfoText();
		Utilities.setTextPane(infoTextPane,s);
	}
	
	//------------------------------------------------------------------------------------
	/** Displays the <code>message</code> if <code>condition</code> is true.
	 * @param level the type of the message
	 * @param clear flag that determines whether clears the message from the information
	 *  panel after a duration of time or not
	 * @return condition
	 */
	public boolean warning(boolean condition, String message, int level, boolean clear) {
		String before = warning == null ? null : warning.getFirst();
		warning = condition ?  new Pair<String,Integer>(message,level) : null;
		if (warning != null && !Utils.equals(warning.getFirst(),before)) {
			updateInfo();
			if (warningTimer != null)
					warningTimer.stop();
			if (warning != null && clear)
					warningTimer = Utils.invokeAfter(PROBLEM_TIMEOUT,new Runnable() {
						public void run() {  clearProblemText(); }
 					});
		}
		return condition;
	}
	
	//------------------------------------------------------------------------------------
	/** Clears the warning/message from the information panel and updates its content. */
	public void clearProblemText() {
		warning = null;
		if (warningTimer != null) {
				warningTimer.stop();
				warningTimer = null;
		}
		updateInfo();
	}

	//------------------------------------------------------------------------------------
	/** Releases the <code>warningTimer</code>. It should call whenever the Preferences
	 *  component is closed.
	 */
	public void onClose() {
		if (warningTimer != null)
			warningTimer.stop();
		warningTimer = null;
	}
	
	//------------------------------------------------------------------------------------
	/** Returns whether button 'b' is enabled or not. */
	public boolean isEnabled(Button b) {
		JButton butt = getButton(b);
		return (butt != null) && butt.isEnabled();
	}
	
	//-----------------------------------------------------------------------------------
	/** Pressing the Cancel button may cause a focus-lost on a JTextComponent
	 *  of the current page, which may trigger an InputVerifier, which should
	 *  usually <i>skip</i> the verification in this case and let the focus go.
	 *  This function allows detecting this case.
	 */
	public boolean isCanceling() {
		if (canceling)
			return true;
		JButton butt = getButton(Button.CANCEL);
		return (butt != null) && butt.getModel().isArmed();
	}
	
	//-----------------------------------------------------------------------------------
	/** When a button-press triggers an InputVerifier, which in turn displays a
	 *  modal dialog, the button remains in a half-pressed, inconsistent state.
	 *  This method allows correcting it for the Preferences' buttons (ok/cancel).
	 *  It should be called from the InputVerifier code, just before displaying 
	 *  the dialog.
	 *  Note that other buttons (which are contained on the page) cannot be
	 *  corrected with this method.
	 */
	public void disarmAllButtons() {
		for (ButtonRec br : buttonInfo.values()) {
			javax.swing.ButtonModel m = br.button.getModel();
			m.setArmed(false);
			m.setPressed(false);
		}
	}
	
	//-------------------------------------------------------------------------
	/** Updates the status of the Preferences' buttons (OK/CANCEL).
	 *  Enables the OK button only if all pages enable it (the current page is asked first).
	 *  Enables the CANCEL button only if the current page enables that button.
	 */
	public void enableDisableButtons() {
		getButton(Button.CANCEL).setEnabled(currentPage == null || currentPage.isEnabled(Button.CANCEL));
		if (currentPage != null && !currentPage.isEnabled(Button.OK)) {
				getButton(Button.OK).setEnabled(false);
				saveButton.setEnabled(false);
				return;
		}
		PageIterator iterator = new PageIterator();
		boolean enabled = true;
		while (iterator.hasNext()) {
			if (!iterator.next().isEnabled(Button.OK)) { enabled = false; break; }
		}
		getButton(Button.OK).setEnabled(enabled);
		saveButton.setEnabled(enabled);
		loadButton.setEnabled(true);
	}

	
	//------------------------------------------------------------------------------------
	/** Asks every page, starting at the current one, whether or not the component can be closed.
	 *  If it can be closed, fires the 'onClose' event and returns true. Otherwise returns false.  
	 */
	public boolean doClose(boolean isOk) {
		boolean saved_canceling = canceling;
		try {
			canceling = !isOk;
			Button b = isOk ? Button.OK : Button.CANCEL;
			if (currentPage != null && !currentPage.onButtonPress(b))
				return false;
			PageIterator iterator = new PageIterator();
			while (iterator.hasNext()) {
				if (!iterator.next().onButtonPress(b)) return false;
			}
			if (!canClose())
				return false;
			
			onClose.fire();
			
			// due to swing memory leaks, break all connection with JTextPane objects
			if (infoScrollPane != null) {
				infoScrollPane.removeAll();
				infoScrollPane = null;
			}
			this.removeAll();
			return true;
		} finally {
			canceling = saved_canceling;
		}
	}
	
	//------------------------------------------------------------------------------------
	/** This method is provided for descendant classes: it can be used to veto
	 *  component closing after the OK/CANCEL button has been accepted.
	 */
	protected boolean canClose() { return true; }

	
	//=====================================================================================
	// implemented interfaces

	//-------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == okButton) ok();
		else if (e.getSource() == cancelButton) cancel();
		else if (e.getSource() == saveButton) save();
		else if (e.getSource() == loadButton) load();
	}

	//------------------------------------------------------------------------------------
	public void valueChanged(TreeSelectionEvent e) {
		TreePath path = pageTree.getSelectionPath();
		warning = null;
		if (warningTimer != null) {
			warningTimer.stop();
			warningTimer = null;
		}
		if (path == null)
			gotoPage(-1);
		else {
			DefaultMutableTreeNode selected = (DefaultMutableTreeNode)path.getLastPathComponent();
			if (selected.equals(root)) return;
			IPreferencesPage page = (IPreferencesPage)selected.getUserObject();
			gotoPage(page);
		}
	}
	
	//=====================================================================================
	// GUI methods
	
	//------------------------------------------------------------------------------------
	@Override
	public Dimension getPreferredSize() {
		Dimension ans = super.getPreferredSize();
		if (isMinimumSizeSet()) {
			if (ans == null)
				ans = getMinimumSize();
			else
				GUIUtils.enlarge(ans, getMinimumSize());
		}
		// plus = height of info panel + buttons panel, width of the tree 
		// tmp = body height & width 
		Dimension plus = new Dimension(ans), tmp = new Dimension(ans);
		plus.height = infoScrollPane.getPreferredSize().height + buttonsPanel.getPreferredSize().height;
		plus.width = treeScrollPane.getPreferredSize().width;
		tmp.height = Math.max(ans.height - plus.height, 1);
		tmp.width = Math.max(ans.width - plus.width, 1);
		PageIterator iterator = new PageIterator();
		while (iterator.hasNext()) {
			java.awt.Container p = iterator.next().getPanel();
			GUIUtils.enlarge(tmp,p.getMinimumSize());
			GUIUtils.enlarge(tmp,p.getPreferredSize());
		}
		tmp.height += plus.height;	// body height + info + buttons
		tmp.width += plus.width; // body width + tree
		GUIUtils.enlarge(ans,tmp);
		if (isMaximumSizeSet())
			GUIUtils.shrink(ans,getMaximumSize());
		return ans;
	}
	
	//------------------------------------------------------------------------------------
	/** Initializes the sizes of the components of the Preferences. */
	public void initSize() {
		Dimension dim = getPreferredSize();
		treeScrollPane.setPreferredSize(new Dimension((int)(dim.width*0.2),pagePanel.getPreferredSize().height));
		infoTextPane.setPreferredSize(new Dimension((int)(dim.width*0.8),75));
	}
	
	//------------------------------------------------------------------------------------
	private void layoutGUI() {
		this.setSize(GUIUtils.GUI_unit(20),GUIUtils.GUI_unit(12));
		this.setMaximumSize(new Dimension(Integer.MAX_VALUE,GUIUtils.getScreenSize().height - 50));
		this.setLayout(new BorderLayout());
		buttonsPanel = new JPanel(new BorderLayout());
		JPanel tmpPanel = new JPanel();
		tmpPanel.add(Box.createHorizontalStrut(10));
		tmpPanel.add(saveButton);
		tmpPanel.add(loadButton);
		buttonsPanel.add(tmpPanel,BorderLayout.WEST);
		
		tmpPanel = new JPanel();
		tmpPanel.add(okButton);
		okButton.setName("btn_ok");
		tmpPanel.add(cancelButton);
		cancelButton.setName("btn_cancel");
		tmpPanel.add(Box.createHorizontalStrut(10));
		buttonsPanel.add(tmpPanel,BorderLayout.EAST);
		
		pagePanel = new JPanel(new BorderLayout());
		Box tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(infoScrollPane);
		tmp.add(new javax.swing.JSeparator());
		pagePanel.add(tmp,BorderLayout.NORTH);
		
		tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(new javax.swing.JSeparator());
		tmp.add(buttonsPanel);
		this.add(tmp,BorderLayout.SOUTH);
		
		tmp = new Box(BoxLayout.X_AXIS);
		tmp.add(treeScrollPane);
		tmp.add(new javax.swing.JSeparator(SwingConstants.VERTICAL));
		this.add(tmp,BorderLayout.WEST);
		this.add(pagePanel,BorderLayout.CENTER);
	}
	
	//------------------------------------------------------------------------------------
	private void initialize() {
		GUIUtils.addActionListener(this,okButton,cancelButton,saveButton,loadButton);
		
		infoScrollPane.setViewportView(infoTextPane);
		infoScrollPane.setBorder(null);
		infoTextPane.setEditable(false);
		infoTextPane.setBackground(buttonsPanel.getBackground());
		int b = GUIUtils.GUI_unit(0.5);
		infoTextPane.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
		
		treeScrollPane.setBorder(null);
		pageTree.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
		pageTree.setRootVisible(true);
		pageTree.setName("tree_preferences_preftree");
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		renderer.setLeafIcon(null);
		renderer.setClosedIcon(null);
		renderer.setOpenIcon(null);
		pageTree.setCellRenderer(renderer);
		pageTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		pageTree.addTreeSelectionListener(this);
	}
	
	//=====================================================================================
	// private methods
	
	//------------------------------------------------------------------------------------
	private JButton getButton(Button b) { return buttonInfo.get(b).button; }
	
	//------------------------------------------------------------------------------------
	/** Returns the node belongs to <code>page</code>
	 * @param parent the parent node of the wanted node
	 * @return null if the method doesn't find the wanted node
	 */ 
	private DefaultMutableTreeNode findNode(DefaultMutableTreeNode parent, IPreferencesPage page) {
		for (int i = 0;i < parent.getChildCount();++i) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)parent.getChildAt(i);
			IPreferencesPage p = (IPreferencesPage)node.getUserObject();
			if (p.equals(page)) return node;
		}
		return null;
	}
	
	//------------------------------------------------------------------------------------
	/** Returns the node belongs <code>parentPage</code>.
	 * @return null if the method doesn't find the wanted node
	 */
	private DefaultMutableTreeNode findParent(IPreferencesPage parentPage) {
		Enumeration e = root.breadthFirstEnumeration();
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
			if (node.equals(root)) continue;
			IPreferencesPage p = (IPreferencesPage)node.getUserObject();
			if (p.equals(parentPage))
				return node;
		}
		return null;
	}
	
	//------------------------------------------------------------------------------------
	/**
	 *  Notifies all listeners of the page tree that the structure of the tree is
	 *  changed.
	 */
	private void fireTreeStructureChanged(TreePath path) {
        // Guaranteed to return a non-null array
        Object[] listeners = ((DefaultTreeModel)pageTree.getModel()).getListeners(TreeModelListener.class);
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-1; i>=0; i--) {
                // Lazily create the event:
                if (e == null)
                    e = new TreeModelEvent(this, path);
                ((TreeModelListener)listeners[i]).treeStructureChanged(e);
        }
	}
	
	//------------------------------------------------------------------------------------
	/** Selects the first page in the page tree. */
	protected void selectFirstPage() { pageTree.setSelectionRow(1);	}
	
	/** Selects the first page in the page tree. */
	protected void selectPage( int row ) { 
		pageTree.setSelectionRow(row);
		pageTree.expandRow(row);
	}
}
