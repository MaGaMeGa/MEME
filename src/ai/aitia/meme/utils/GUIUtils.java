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
package ai.aitia.meme.utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.utils.Utils.Callback;

/** Collection of graphical user interface related utility functions. */
public class GUIUtils {
	
	// -------------------------------------------------------------------------
	/** Returns a new dimension with the larger width and the larger height. */
	public static Dimension max(Dimension a, Dimension b) {
		return new Dimension(Math.max(a.width, b.width), Math.max(a.height, b.height));
	}

	// -------------------------------------------------------------------------
	/** Returns a new dimension with the lesser width and the lesser height. */
	public static Dimension min(Dimension a, Dimension b) {
		return new Dimension(Math.min(a.width, b.width), Math.min(a.height, b.height));
	}
    
	//-------------------------------------------------------------------------
	/** Returns the 'sum' of the parameters (a new dimension formed from the 
	 *  sum of the widths and the sum of the heights).  */
	public static Dimension add(Dimension a, Dimension b) {
		return new Dimension(a.width + b.width, a.height + b.height);
	}

	// -------------------------------------------------------------------------
	/** Enlarges 'a' to the size of 'b', if necessary. Returns true, if the enlarging
	 *  has been necessary.
	 */
    public static boolean enlarge(Dimension a, Dimension b) {
		boolean ans = false;
		if (b != null) {
			if (b.width > a.width)   { a.width  = b.width;  ans = true; }
			if (b.height > a.height) { a.height = b.height; ans = true; }
		}
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Shrinks 'a' to the size of 'b', if necessary. Returns true, if the shrinking
	 *  has been necessary.
	 */
    public static boolean shrink(Dimension a, Dimension b) {
    	boolean ans = false;
    	if (b != null) {
	    	if (b.width < a.width ) { a.width = b.width;  ans = true; }
	    	if (b.height< a.height) { a.height= b.height; ans = true; }
    	}
    	return ans;
    }

	//-------------------------------------------------------------------------
    /**
     * Converts a color string such as "RED" or "#NNNNNN" to a Color.
     * Note: This will only convert the HTML3.2 color strings or 
     * a string of length 7; otherwise, it will return null.
     * See also: javax.swing.text.html.StyleSheet.stringToColor()
     */
    public static java.awt.Color str2color(String colorStr) {
		if (g_ss == null)
			g_ss = new javax.swing.text.html.StyleSheet();
		return g_ss.stringToColor(colorStr);    	
    }
    private static javax.swing.text.html.StyleSheet g_ss = null;
    
	//-------------------------------------------------------------------------
    /**
     * Converts a color object to a HTML3.2 color string (#NNNNNN). 
     */
    public static String color2html(java.awt.Color color) {
    	return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

	//-------------------------------------------------------------------------
    /** Returns the color of the reference component. */
    public static java.awt.Color getLabelFg() {
    	//return javax.swing.UIManager.getColor("Label.foreground");
    	return getRefComp().getForeground();
    }

	//-------------------------------------------------------------------------
	/** 
	 * Some functions (such as font/screen size calculation) require a component
     * e.g. to access the screen. I call it as 'reference component'.
     * It is recommended to set it to a JLabel/JButton in the main window.
     */
	public static java.awt.Component getRefComp() {
		if (g_referenceComponent == null) {
			g_referenceComponent = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
			if (g_referenceComponent == null)
				g_referenceComponent = new javax.swing.JLabel();
		}
		return g_referenceComponent;
	}
	private static java.awt.Component g_referenceComponent = null;

	public static void setRefComp(java.awt.Component c) {
		g_referenceComponent = c;
	}

	//-------------------------------------------------------------------------
	/** Default root pane - for routines like createDialog(), setBusy() etc. */
	public static javax.swing.JRootPane getDefRoot() {
		javax.swing.JRootPane ans = null;
		if (MEMEApp.getMainWindow() != null)
			ans = MEMEApp.getMainWindow().getJFrame().getRootPane();
		else
			ans = javax.swing.SwingUtilities.getRootPane(getRefComp());
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Resizes table 't'. */
    public static void resizeJTableToContents(javax.swing.JTable t, float factor) {
    	javax.swing.table.TableModel tm = t.getModel();
    	javax.swing.JLabel tmpLabel = new javax.swing.JLabel();
    	for (int col = tm.getColumnCount() - 1; col >= 0; --col) {
    		String s = tm.getColumnName(col);
    		if (s != null) tmpLabel.setText(s);
    		int width = (s == null) ? 0 : tmpLabel.getPreferredSize().width;   
    		for (int row = tm.getRowCount() - 1; row >= 0; --row) {
    			Object val = tm.getValueAt(row, col);
    			s = (val == null) ? null : val.toString();
    			if (s != null) {
    				tmpLabel.setText(s);
    				width = Math.max(width, tmpLabel.getPreferredSize().width);
    			}
    		}
    		t.getColumnModel().getColumn(col).setPreferredWidth((int)(width * factor));
    	}

    	Dimension ps = t.getPreferredScrollableViewportSize();
    	if (shrink(ps, t.getPreferredSize()))
    		t.setPreferredScrollableViewportSize(ps);
    	t.revalidate();
//        java.awt.Container p = t.getParent();
//        if (p instanceof javax.swing.JViewport) {
//            p = p.getParent();
//            if (p instanceof javax.swing.JScrollPane) {
//            	int h = p.getPreferredSize().height; 
//            	if (t.getPreferredSize().height < h * 0.6) {
//            		p.setPreferredSize(new java.awt.Dimension(0, h * 8 / 10));
//            	}
//            }
//        }
    }

	//-------------------------------------------------------------------------
    /** This class assigns a popup menu with 'Select All' item to a list. */
    @SuppressWarnings("serial")
	public static class ToggleClickMultiSelection extends java.awt.event.MouseAdapter
    {
    	/** The host component. */
    	private java.lang.ref.WeakReference<java.awt.Component> comp;
    	/** The popup menu. */
    	protected javax.swing.JPopupMenu contextMenu = new javax.swing.JPopupMenu();
    	/** The action of the 'Select All' menu item. */
    	protected javax.swing.AbstractAction selectAllAction = new javax.swing.AbstractAction() {
    		{ putValue(NAME, "Select All"); }
    		public void actionPerformed(java.awt.event.ActionEvent e) {
    			java.awt.Component c = (comp == null) ? null : comp.get();
    			if (c != null)
    				{
    					selectAll(c);
    		}
    			}
    		
    	};

    	public ToggleClickMultiSelection() {
    		contextMenu.add(selectAllAction);
    		contextMenu.getComponent(contextMenu.getComponentCount()-1).setName("btn_selectall");
    	}

    	@Override public void mousePressed(java.awt.event.MouseEvent e) {
    		if (!javax.swing.SwingUtilities.isLeftMouseButton(e))
    			return;
    		if (e.getSource() instanceof javax.swing.JList) {
    			javax.swing.JList list = (javax.swing.JList)e.getSource();
    			if (!list.isEnabled()) return;
    			int row = list.locationToIndex(e.getPoint());
    			if (e.getClickCount() == 1) {
	    			if (list.isSelectedIndex(row)) {
	    				list.removeSelectionInterval(row, row);
	    			} else {
	    				list.addSelectionInterval(row, row);
	    			}
    			} else {
    				list.setSelectionInterval(row, row);
    				doubleClick(e, row);
    			}
    			e.consume();
    		}
    	}
    
    	@Override public void mouseReleased(java.awt.event.MouseEvent e) {
    		if (!javax.swing.SwingUtilities.isRightMouseButton(e))
    			return;
			if (e.getComponent().isEnabled()) {
	    		comp = new java.lang.ref.WeakReference<java.awt.Component>(e.getComponent());
				contextMenu.show(e.getComponent(), e.getX(), e.getY());
			}
    	}

    	/** Adds this listener to 'jList'. */
    	public void installOn(java.awt.Component jList) {
    		java.awt.event.MouseListener before[] = jList.getMouseListeners();
    		for (java.awt.event.MouseListener l : before) jList.removeMouseListener(l);
    		jList.addMouseListener(this);
    		for (java.awt.event.MouseListener l : before) {
    			if (!l.getClass().getName().matches(".*\\.BasicListUI.Handler"))
    				jList.addMouseListener(l);
    		}
    	}

    	/** Event handler of the double click event. Does nothing by default. */
    	protected void doubleClick(java.awt.event.MouseEvent e, int rowIdx) {}
    	/** Selects all elements of the component 'jList'. */
    	protected void selectAll(java.awt.Component jList) {
			javax.swing.JList list = (javax.swing.JList)jList;
			if (list.getModel().getSize() > 0)
				list.addSelectionInterval(0, list.getModel().getSize()-1);
    	}
    }


	//-------------------------------------------------------------------------
    /**
     * [S]croll[P]ane [M]ax[S]ize [A]ware JEditorPane: 
     * a JEditorPane which, when displayed within a JScrollPane, respects 
     * the maximum size of the JScrollPane. 
     */
	public static class SPMSAEditorPane extends javax.swing.JEditorPane {
		private static final long serialVersionUID = -7465651280145169196L;
		public SPMSAEditorPane()										{ super(); }
		public SPMSAEditorPane(String url) throws java.io.IOException	{ super(url); }
		public SPMSAEditorPane(String type, String text)				{ super(type, text); }
		public SPMSAEditorPane(java.net.URL initialPage) throws java.io.IOException  { super(initialPage); }
	    @Override public Dimension getPreferredScrollableViewportSize() {
	    	return GUIUtils.getPreferredScrollableViewportSize(this, super.getPreferredScrollableViewportSize());
	    }
	}

	//-------------------------------------------------------------------------
	/** Returns the preferred size of the viewport for component 't'. 'ans' is the
	 *  initial preferred size.
	 * @return ans
	 */ 
    public static Dimension getPreferredScrollableViewportSize(javax.swing.text.JTextComponent t, Dimension ans) {
		java.awt.Container p = t.getParent();
		if (p instanceof javax.swing.JViewport) {
		    p = p.getParent();
		    if (p instanceof javax.swing.JScrollPane)
		    	shrink(ans, p.getMaximumSize());
		}
		return ans;
    }

	//-------------------------------------------------------------------------
    public static void setWrapLength(javax.swing.text.JTextComponent tc, int width) {
    	if (tc == null || width <= 0) return; 
		getContentSize(tc, width, true);
    }

    //-------------------------------------------------------------------------
    public static Dimension getContentSize(javax.swing.text.JTextComponent tc) {
    	return getContentSize(tc, 0, false);
    }

    //-------------------------------------------------------------------------
    /** This methods also sets the wrap length according to parameter value of 'setWidth'. */
    private static Dimension getContentSize(javax.swing.text.JTextComponent tc, int width, boolean setWidth) {
    	boolean wasSet = tc.isPreferredSizeSet();
    	Dimension d = tc.getPreferredSize();
    	if (setWidth)
    		d.width = width;
    	tc.setPreferredSize(new Dimension(d.width, Integer.MAX_VALUE));
    	Dimension ans = (Dimension)d.clone();
    	try {
    		java.awt.Rectangle r = tc.modelToView(tc.getDocument().getLength());
    		if (r != null)
    			ans.height = r.y + r.height;
    	} catch (javax.swing.text.BadLocationException e) {
    		Logger.logExceptionCallStack(e);
    		ans.height += getRowHeight(tc);
    	}
    	if (setWidth) d = ans; else if (!wasSet) d = null;
    	tc.setPreferredSize(d);
    	return ans;
    }

    //-------------------------------------------------------------------------
    /** Returns the size of the overhead (e.g. scrollbars) of 'c'. */
    public static Dimension getOverhead(java.awt.Container c) {
		java.awt.Insets overhead = c.getInsets();
		if (c instanceof javax.swing.JScrollPane) {
			javax.swing.JScrollPane jScrollPane = (javax.swing.JScrollPane)c;
			overhead.right  += jScrollPane.getVerticalScrollBar().getPreferredSize().width;
			overhead.bottom += jScrollPane.getHorizontalScrollBar().getPreferredSize().height;
		}
		return new Dimension(overhead.left + overhead.right, overhead.top  + overhead.bottom);
    }

	//-------------------------------------------------------------------------
    /** Sets 'text' as the content of the 'textpane' */
    public static void setTextPane(javax.swing.JEditorPane textpane, String text) {
		// Swing throws IllegalArgumentException if I try to change the content type
		// from "text/html" to "text/plain". Therefore I make the change in one 
    	// direction only (plain -> html) but not back. 

    	if (Utils.isHTML(text)) {
    		textpane.setContentType("text/html");
    		((javax.swing.text.html.HTMLDocument)textpane.getDocument()).setBase(g_baseURL);
    	}

    	textpane.setText(text);
    }
    public static java.net.URL g_baseURL = null;		// initialized by MEMEApp.main()


	//-------------------------------------------------------------------------
    /** Returns the row height (font height) of 'c' of the reference component if 'c'
     *  is null. */
    public static int getRowHeight(java.awt.Component c) {
    	java.awt.Font f = (c != null) ? c.getFont() : null;
    	if (f == null) f = getRefComp().getFont();
    	if (f == null) f = 	javax.swing.UIManager.getFont("Label.font");
    	return (c != null ? c : getRefComp()).getFontMetrics(f).getHeight();
    }

	//-------------------------------------------------------------------------
    /** Converts a distance measured in "GUI unit" to pixels.
     * The "GUI unit" scales automatically with the current font and the
     * monitor's resolution (it is the pixel height of the default JLabel font). 
     */
    public static int GUI_unit(double p_GUI_unit) {
    	if (g_GUI_unit == null) {
    		g_GUI_unit = getRowHeight(null);
    	}
    	return (int)Math.round(p_GUI_unit * g_GUI_unit);
    }
    private static Integer g_GUI_unit = null;

	//-------------------------------------------------------------------------
    /** Converts a distance measured in "GUI unit" to pixels.
     * The "GUI unit" scales automatically with the current font and the
     * monitor's resolution (it is the pixel height of c's font). 
     */
    public static int GUI_unit(double p_GUI_unit, java.awt.Component c) {
    	return (int)Math.round(p_GUI_unit * getRowHeight(c));
    }

	//-------------------------------------------------------------------------
    /**
     * Converts horizontal dialog units and returns pixels. 
     * Honors the resolution, dialog font size, platform, and l&amp;f.
     * 
     * @param amount the horizontal dialog units
     * @param c the component that provides the graphics object
     * @return the given horizontal dialog units as pixels
     */
    public static int dluX(int amount, java.awt.Component c) {
    	return com.jgoodies.forms.layout.Sizes.dialogUnitXAsPixel(amount, c);
    }
    /**
     * Converts vertical dialog units and returns pixels. 
     * Honors the resolution, dialog font size, platform, and l&amp;f.
     * 
     * @param amount the vertical dialog units
     * @param c the component that provides the graphics object
     * @return the given vertical dialog units as pixels
     */
    public static int dluY(int amount, java.awt.Component c) {
    	return com.jgoodies.forms.layout.Sizes.dialogUnitYAsPixel(amount, c);
    }
     /**
      * Converts horizontal dialog units and returns pixels. 
      * Honors the resolution, dialog font size, platform, and l&amp;f.
      * This method uses the graphics object of the reference component.
      * @param amount the horizontal dialog units
      * @return the given horizontal dialog units as pixels
      */
    public static int dluX(int amount) { return dluX(amount, getRefComp()); }
    /**
     * Converts vertical dialog units and returns pixels. 
     * Honors the resolution, dialog font size, platform, and l&amp;f.
     * This method uses the graphics object of the reference component.
     * @param amount the vertical dialog units
     * @return the given vertical dialog units as pixels
     */
    public static int dluY(int amount) { return dluY(amount, getRefComp()); }

    
	//-------------------------------------------------------------------------
    /**
     * Returns the maximum possible size of a fully visible window.
     * Note that this method is slow and expensive for the first time.
     */
    public static java.awt.Dimension getScreenSize() {
    	if (g_screenSize == null) {
    		java.awt.Component c = getRefComp();
    		java.awt.Toolkit tk = c.getToolkit();
    		g_screenSize = tk.getScreenSize();
    		try {
    			java.awt.Insets in = tk.getScreenInsets(c.getGraphicsConfiguration());
    			g_screenSize.width  -= in.left + in.right;
    			g_screenSize.height -= in.top  + in.bottom;
    		} catch (Throwable t) {}
    		//java.awt.Rectangle rect = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    		//g_screenSize = rect.getSize();
    		//System.gc();
    	}
    	return g_screenSize;
    }
    private static java.awt.Dimension g_screenSize = null;
    
    //----------------------------------------------------------------------------------------------------
	public static Dimension getUseableScreenSize() {
		final Dimension d = getScreenSize();
		return new Dimension(d.width,getRelScrH(90));
	}
	
	//----------------------------------------------------------------------------------------------------
	public static Dimension getPreferredSize(final Component comp) {
		final Dimension useableScreenSize = GUIUtils.getUseableScreenSize();
		final Dimension d = GUIUtils.min(comp.getPreferredSize(),useableScreenSize);
		if (!d.equals(comp.getPreferredSize()))
			d.width = Math.min((int)(d.width * 1.1),useableScreenSize.width);
		return d;
	}

    /** Returns the 'percent' percent of the getScreenSize().width. */
    public static int getRelScrW(int percent)		{ return getScreenSize().width * percent / 100; }
    /** Returns the 'percent' percent of the getScreenSize().height. */
    public static int getRelScrH(int percent)		{ return getScreenSize().height * percent / 100; }


	//-------------------------------------------------------------------------
    // EDT only
    /** If b==true, then it disables all mouse moving and clicking event. If b==false,
     *  then enables them.
     */ 
    public static void setBusy(boolean b)			{ setBusy(null, b); }
    /** If b==true, then it disables all mouse moving and clicking event. If b==false,
     *  then enables them.
     * @param c a component that provides reference to root pane, glass pane, etc. If
     *        it is null, then the method uses the reference component.
     */ 
    public static void setBusy(java.awt.Component c, boolean b) {
		// TODO: b=true eseten elkapni minden egermozgast es kattintast a glass pane-eken.
		// b=false eseten visszacsinalni.
    	if (c == null)
    		c = getRefComp();
		javax.swing.JRootPane r = javax.swing.SwingUtilities.getRootPane(c);
		if (r == null)
			r = getDefRoot(); 
		
		if (g_BUSY == null)
			g_BUSY = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR);

		c = r.getGlassPane();
		c.setCursor(b ? g_BUSY : java.awt.Cursor.getDefaultCursor());
		c.setVisible(b);

		c = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		if (c != null && (r = javax.swing.SwingUtilities.getRootPane(c)) != null) {
			c = r.getGlassPane();
			c.setCursor(b ? g_BUSY : java.awt.Cursor.getDefaultCursor());
			c.setVisible(b);
		}
    }
	public static java.awt.Cursor g_BUSY = null;

	
	//-------------------------------------------------------------------------
	/**
	 * Must be called in the event dispatcher thread! The implementation of this 
	 * method exploits the fact that AWT uses {@link java.awt.EventDispatchThread}
	 * objects for event dispatcher threads, and calls its undocumented, 
	 * package-private pumpOneEventForHierarchy() method (in JRE 1.5) or
	 * pumpOneEventForFilters() (in JRE 1.6).
	 * <br>Note that this method has been tested with JRE 1.5 and 1.6 only.
	 * @param modalComponent If null, the event loop will be non-modal (will 
	 *                 dispatch all events). If it is a {@link java.awt.Container},
	 *                 this method makes that Container modal. 
	 *                 Otherwise no window will receive events. 
	 * @param q       The {@link java.awt.EventQueue} to be monitored. 
	 *                 If null, the system's default event queue is used.
	 * @param isModal Represents the loop condition: the loop continues 
	 *                 while isModal.call() returns true (null means false).
	 * @return Any exception that is caught. Note that exceptions raised during
	 *          event dispatching are caught by AWT, thus aren't returned here.
	 * @throws UnsupportedOperationException if the undocumented methods are not
	 * found.
	 */
	@SuppressWarnings("unchecked")
	public static Exception modalEventLoop(java.awt.Component	modalComponent,
											java.awt.EventQueue	q,
											java.util.concurrent.Callable<Boolean>	isModal)
	{
		Exception ans = null;
		Thread thread = Thread.currentThread();
		while (m_pumpOneEvent == null) {
		try {
			m_jre += 1;
			switch (m_jre) {
				case 5 : // JRE 1.5:
					m_pumpOneEvent	= thread.getClass().getDeclaredMethod("pumpOneEventForHierarchy", 
												Integer.TYPE, java.awt.Component.class);
					m_pumpOneEvent.setAccessible(true);
					break;
				case 6 : { // JRE 1.6:
					m_pumpOneEvent	= thread.getClass().getDeclaredMethod("pumpOneEventForFilters", 
							Integer.TYPE);
					m_pumpOneEvent.setAccessible(true);
					
					if (!m_pumpOneEvent.getReturnType().equals(Boolean.TYPE)){
						m_pumpOneEvent = null;
						continue;
					}
					Class cls		= Class.forName("java.awt.EventFilter");
					m_addFilter		= thread.getClass().getDeclaredMethod("addEventFilter", cls);
					m_removeFilter	= thread.getClass().getDeclaredMethod("removeEventFilter", cls);
					m_addFilter.setAccessible(true);
					m_removeFilter.setAccessible(true);
					cls				= Class.forName("java.awt.EventDispatchThread$HierarchyEventFilter");
					m_filter		= cls.getConstructor(new Class[] { java.awt.Component.class });
					m_filter.setAccessible(true);
					break;
				}
				case 7 :
					m_pumpOneEvent	= thread.getClass().getDeclaredMethod("pumpOneEventForFilters", 
							Integer.TYPE);
					m_pumpOneEvent.setAccessible(true);
					
					if (!m_pumpOneEvent.getReturnType().equals(Void.TYPE)){
						m_pumpOneEvent = null;
						continue;
					}
					Class cls		= Class.forName("java.awt.EventFilter");
					m_addFilter		= thread.getClass().getDeclaredMethod("addEventFilter", cls);
					m_removeFilter	= thread.getClass().getDeclaredMethod("removeEventFilter", cls);
					m_addFilter.setAccessible(true);
					m_removeFilter.setAccessible(true);
					cls				= Class.forName("java.awt.EventDispatchThread$HierarchyEventFilter");
					m_filter		= cls.getConstructor(new Class[] { java.awt.Component.class });
					m_filter.setAccessible(true);
					break;
				default : 
					throw new UnsupportedOperationException();
			}
		}
		catch (NoSuchMethodException e)  {}
		catch (ClassNotFoundException e) {}
		}

		if (q == null) {
			q = (modalComponent == null) ? java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue()
			                             : modalComponent.getToolkit().getSystemEventQueue();
		}
		try {
			Object filter = null, args[];
			if (m_jre == 5) {
				args = new Object[] { -1, modalComponent };
			} else {
				args = new Object[] { -1 };
				filter = m_filter.newInstance(modalComponent);
				m_addFilter.invoke(thread, filter);
			}
			try {
				for (boolean loop = true; loop; ) {
					loop = (!thread.isInterrupted() && isModal.call() == Boolean.TRUE);
					if (m_jre < 7){
						if (loop) loop = (Boolean)m_pumpOneEvent.invoke(thread, args);
					} else {
						m_pumpOneEvent.invoke(thread, args);
					}
				}
			}
			catch (Exception e){
				ans = e;
				Logger.logException("in modelEventLoop", e, true);
			} finally {
				if (filter != null)
					m_removeFilter.invoke(thread, filter);
			}
		} catch (Exception e) {
			ans = e;
			Logger.logException("in modelEventLoop", e, true);
		}

		return ans;
	}

	//-------------------------------------------------------------------------
	private static int							m_jre = 4;
	private static java.lang.reflect.Method		m_pumpOneEvent, m_addFilter, m_removeFilter;
	private static java.lang.reflect.Constructor	m_filter;
	
	
	//-------------------------------------------------------------------------
	/** Creates and returns a button group from 'buttons'. */
	public static javax.swing.ButtonGroup createButtonGroup(javax.swing.AbstractButton ... buttons) {
		javax.swing.ButtonGroup bg = new javax.swing.ButtonGroup();
		for (int i = 0; i < buttons.length; ++i)
			bg.add(buttons[i]);
		return bg;
	}

	//-------------------------------------------------------------------------
	/** Creates a DO_NOTHING_ON_CLOSE JDialog from the arguments, but does not
	 *  display it (calls pack() only). If you wish the window-closing button
	 *  to work automatically, call disposeOnClose() on the returned JDialog.
	 *  To display the dialog, call .setVisible(true) on it.
	 */
	// TODO: erdemes lenne atirni olyanra, hogy internalframe-es esetben JDialog helyett
	// JInternalFrame-et csinaljon. Csakhogy ehhez a visszateresi ertek tipusat modositani 
	// kellene java.awt.Container-re.
	public static javax.swing.JDialog createDialog(java.awt.Window parent, boolean modal, String title, java.awt.Container contentPane) {
		if (parent == null) {
			java.awt.Component c = getDefRoot();
			if (c != null && c.getParent() instanceof java.awt.Window)
				parent = (java.awt.Window )c.getParent();
		}

		javax.swing.JDialog ans;
		if (parent instanceof java.awt.Frame)
			ans = new javax.swing.JDialog((java.awt.Frame)parent, title, modal);
		else if (parent instanceof java.awt.Dialog)
			ans = new javax.swing.JDialog((java.awt.Dialog)parent, title, modal);
		else {
			ans = new javax.swing.JDialog();
			ans.setTitle(title);
			ans.setModal(modal);
		}
		ans.setDefaultCloseOperation(javax.swing.JDialog.DO_NOTHING_ON_CLOSE);
		if (contentPane != null) {
			final JScrollPane sp = new JScrollPane(contentPane,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			sp.setBorder(null);
			ans.setContentPane(sp);
			ans.pack();
			Dimension oldD = ans.getPreferredSize();
			ans.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
											   oldD.height + sp.getHorizontalScrollBar().getHeight() + 50)); // magic number
			sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			oldD = ans.getPreferredSize();
			final Dimension newD = getPreferredSize(ans);
			if (!oldD.equals(newD)) 
				ans.setPreferredSize(newD);
			ans.pack();
			ans.setLocationRelativeTo(parent);
			//ans.setVisible(true);
		}
		return ans;
	}

	//-------------------------------------------------------------------------
	/** 
	 * Arranges for clearing the content pane of the dialog when it is closed.
	 * This is a workaround for Swing's memory leaks (Swing usually retains 
	 * a reference to the closed dialog.)
	 */
	public static javax.swing.JDialog disposeOnClose(javax.swing.JDialog d) {
		d.setDefaultCloseOperation(javax.swing.JDialog.DISPOSE_ON_CLOSE);
		d.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosing(java.awt.event.WindowEvent e) {
				javax.swing.JDialog d2 = (javax.swing.JDialog)e.getSource();
				d2.removeWindowListener(this);
				d2.setContentPane(new javax.swing.JPanel());	// clear the content pane
			}
		});
		return d;
	}

	//-------------------------------------------------------------------------
	/** Closes the window belongs to 'c'. */
	public static void closeWindow(java.awt.Component c) {
 		javax.swing.JRootPane root = javax.swing.SwingUtilities.getRootPane(c);
 		java.awt.Container p = (root == null) ? null : root.getParent();
 		if (p instanceof java.awt.Window) {
 			java.awt.Window window = (java.awt.Window)p;
            window.dispatchEvent(new java.awt.event.WindowEvent(
             							window, java.awt.event.WindowEvent.WINDOW_CLOSING));
		}
 		else if (p instanceof javax.swing.JInternalFrame) {
 			javax.swing.JInternalFrame f = (javax.swing.JInternalFrame)p;
 			f.dispatchEvent(new javax.swing.event.InternalFrameEvent(f, 
 										javax.swing.event.InternalFrameEvent.INTERNAL_FRAME_CLOSING));
 		}
	}
	
	//-------------------------------------------------------------------------
	/** Resizes the window belongs to 'c'. */
 	public static void repack(java.awt.Component c) {
 		javax.swing.JRootPane root = javax.swing.SwingUtilities.getRootPane(c);
 		java.awt.Container p = (root == null) ? null : root.getParent();
 		if (p instanceof java.awt.Window)
 			((java.awt.Window)p).pack();
 		else if (p instanceof javax.swing.JInternalFrame)
 			((javax.swing.JInternalFrame)p).pack();
 	}

 	
	//-------------------------------------------------------------------------
 	/** This method shows/hides the text of the buttons on 't' according to the
 	 *  value of 'show'.
 	 */
 	public static void showTextOnToolbar(javax.swing.JToolBar t, boolean show) {
 		for (int i = t.getComponentCount() - 1; i >= 0; --i) {
 			if (t.getComponent(i) instanceof javax.swing.AbstractButton) {
 				javax.swing.AbstractButton button = (javax.swing.AbstractButton)t.getComponent(i);
 				Object hide = button.getClientProperty("hideActionText");
 				javax.swing.Action a = button.getAction();
 				try {
 					// in the case of using JRE6
 					Method m = AbstractButton.class.getMethod("setHideActionText",Boolean.TYPE);
 					m.invoke(button,!show);
 					button.setAction(null);
 					button.setAction(a);
 				} catch (Exception e) {}
 				if (hide instanceof Boolean && (Boolean)hide != (!show)) {
 					button.putClientProperty("hideActionText", !show);
 					// in the case of using JRE5
 					if (a != null) {
 						String name = a.getValue(javax.swing.Action.NAME).toString();
 						a.putValue(javax.swing.Action.NAME, null);
 						a.putValue(javax.swing.Action.NAME, name);
 					}
 				}
 			}
 		}
 	}

	//-------------------------------------------------------------------------
 	/** Adds action listener 'listener' to all components contained by 'args'. */
 	public static void addActionListener(java.awt.event.ActionListener listener, java.awt.Component ... args) {
 		for (int i = 0; i < args.length; ++i) {
 			Object o = args[i];
 			if (o instanceof javax.swing.AbstractButton)
 				((javax.swing.AbstractButton)o).addActionListener(listener);
 			else if (o instanceof javax.swing.JComboBox)
 				((javax.swing.JComboBox)o).addActionListener(listener);
 			else if (o instanceof javax.swing.JTextField)
 				((javax.swing.JTextField)o).addActionListener(listener);
 			else if (o instanceof javax.swing.Timer)
 				((javax.swing.Timer)o).addActionListener(listener);
 			else
 				throw new UnsupportedOperationException("Object of class " + o.getClass().getCanonicalName());
 		}
 	}

 	//-------------------------------------------------------------------------
 	/**
 	 * Example:
     * <pre>	bind(jViewsList, null, "DELETE", deleteView);  </pre>
 	 * @param when one of WHEN_IN_FOCUSED_WINDOW, WHEN_FOCUSED, WHEN_ANCESTOR_OF_FOCUSED_COMPONENT.
 	 *              'null' means WHEN_FOCUSED.
 	 */
 	public static void bind(javax.swing.JComponent c, Integer when, String key, javax.swing.Action action) {
 		javax.swing.InputMap iMap = (when == null) ? c.getInputMap() : c.getInputMap(when);
 		Object name = action.getValue(javax.swing.Action.NAME);
 		iMap.put(javax.swing.KeyStroke.getKeyStroke(key), name);
 		c.getActionMap().put(name, action);
 	}

 	//-------------------------------------------------------------------------
 	/** Calls button.doClick() when 'key' occurs */
 	public static void bind(javax.swing.JComponent c, Integer when, String key, javax.swing.AbstractButton button) {
 		String name = button.getText();
 		if (name == null || name.length() == 0) name = key;
 		bind(c, when, key, new CallbackAction(new Callback(button, "doClick", null), name));
 	}

 	//-------------------------------------------------------------------------
 	/** Calls target.method(args) when 'key' occurs */
 	public static void bind(javax.swing.JComponent c, Integer when, String key, 
 								Object target, String method, Object ... args) {
 		bind(c, when, key, new CallbackAction(new Callback(target, method, args), key));
 	}

	//-------------------------------------------------------------------------
 	/** This action represents a callback 'function'. You must define a task (a 
 	 *  Runnable instance) that will be executed whenever the action is performed. 
 	 */
 	public static class CallbackAction extends javax.swing.AbstractAction {
		private static final long serialVersionUID = 1L;
		public Runnable cb;
 		public CallbackAction(Runnable cb, String name) { this.cb = cb; putValue(NAME, name); }
		public void actionPerformed(java.awt.event.ActionEvent e) { if (cb != null) cb.run(); }
 	}

	//-------------------------------------------------------------------------
 	/** Returns the name of the action 'a'. */
 	public static String getName(javax.swing.Action a) {
 		Object o = a.getValue(javax.swing.Action.NAME);
 		return (o == null) ? null : o.toString();
 	}

	//-------------------------------------------------------------------------
	/** 
	 * An iterator that enumerates all subcomponents of a given component, recursively.
	 * It generates preorder traversal: the given component is the first, then its 0'th 
	 * subcomponent, then that's 0'th subcomponent an so on.
	 * Modifying the component hierarchy during iteration causes undefined behavior.
	 * The starting component may be null: this results an empty iterator. 
	 * 
	 * Note: this class supports at most one iteration at a time!
	 */
	public static class ComponentIterator implements Iterable<java.awt.Component>, java.util.Iterator<java.awt.Component> {
		protected final java.awt.Component start;
		protected ArrayList<Integer> pos = new ArrayList<Integer>();
		protected java.awt.Component current = null;
		protected int maxDepth = -1;

		/** maxDepth = 0 means no recursion: only 'c' is returned. Negative means infitite (the default). */
		public ComponentIterator(java.awt.Component c, int maxDepth)	{ start = c; this.maxDepth = maxDepth; }
		public ComponentIterator(java.awt.Component c)	{ start = c; }
		public void remove()							{ throw new UnsupportedOperationException(); }
		public boolean hasNext()						{ return (current != null); }

		public java.util.Iterator<java.awt.Component> iterator() {
			current = start;
			pos.clear();
			return this;
		}

		// Returns 'current' now, and advances 'current' for the next iteration.
		// Invariant: the last element of 'pos' stores the index of 'current' 
		// within its parent. 'pos' is empty <=> current == start
		public java.awt.Component next() {
			if (current == null) return null;
			java.awt.Component ans = current;
			java.awt.Container parent;
			boolean mayDescend = (maxDepth < 0 || pos.size() < maxDepth);
			if (mayDescend && (current instanceof javax.swing.JMenu || current instanceof java.awt.Container)) {
				parent = (java.awt.Container)current;
				pos.add(-1);
			} else {
				parent = current.getParent();
			}
			current = null;
			if (!pos.isEmpty()) {
				int n = pos.size() - 1, idx = pos.get(n);
				while (true) {
					int componentCount;
					if (parent instanceof javax.swing.JMenu)
						componentCount = ((javax.swing.JMenu)parent).getMenuComponentCount();
					else
						componentCount = parent.getComponentCount();
					if (++idx < componentCount) break;

					pos.remove(n--);
					if (n <= 0)
						return ans;

					idx = pos.get(n);
					parent = parent.getParent();
				}
				if (parent instanceof javax.swing.JMenu)
					current = ((javax.swing.JMenu)parent).getMenuComponent(idx);
				else
					current = parent.getComponent(idx);
				pos.set(n, idx);
			}
			return ans;
		}
	}
	
	//-------------------------------------------------------------------------
	/** Returns an iterator that enumerates all subcomponents of a given component, recursively. */
	public static ComponentIterator iterate(java.awt.Component c) {
		return new ComponentIterator(c);
	}
	/** Returns an iterator that enumerates all subcomponents of a given component, recursively.
	 * @param maxDepth the maximum depth of searching subcomponents */
	public static ComponentIterator iterate(java.awt.Component c, int maxDepth) {
		return new ComponentIterator(c, maxDepth);
	}

	//-------------------------------------------------------------------------
	/** Resets the column widths of 'table' according the width of the scrollpane. */
	public static void updateColumnWidths(JTable table, javax.swing.JScrollPane sc) {
		java.awt.FontMetrics fm = table.getFontMetrics(table.getFont());
		java.awt.Graphics g = table.getGraphics();
		javax.swing.table.TableColumnModel cm = table.getColumnModel();
		int gap = GUIUtils.dluX(4, table);
		int totalw = sc.getWidth()
						- cm.getColumn(0).getMaxWidth()
						- sc.getVerticalScrollBar().getPreferredSize().width 
						- gap;
		float sum = 0, maxw = 0;
		int n = cm.getColumnCount() - 1;		// -1 mert a '#' oszlopot kihagyjuk
		ArrayList<Float> widths = new ArrayList<Float>(n);
		for (int i = 1; i <= n; ++i) {			// i=0-t szandekosan hagyjuk ki ('#' oszlop)
			String header = String.valueOf(cm.getColumn(i).getHeaderValue());
			float width;
			if (g != null) {
				width = (float)fm.getStringBounds(header, 0, header.length(), g).getWidth();
			} else {
				width = fm.stringWidth(header);
			}
			float w = width + gap;
			if (w > maxw) maxw = w;
			sum += w;
			widths.add(w);
		}
		// sum: ennyi hely kell az oszlopoknak "termeszetes modon".
		// totalw: ennyi hely van a kepernyon
		//
		// Ha sum > totalw, akkor ki fog logni a tablazat, ez a vizszintes scrollbarnak
		// ad munkat. Ha sum < totalw, akkor pedig elosztjuk a fennmarado helyet. 
		// A keskenyebb oszlopoknak sulyozottan tobbet adunk mint a szelesebbeknek, 
		// igy az egyforma szelesseg fele tendalnak. Kozben ugyelunk arra is, nehogy 
		// a keskenyebbek a legszelesebbnel is szelesebbe valjanak: ilyen esetben 
		// a szeleseket is noveljuk, es mindegyik egyforma szeles lesz.
		//
		if (--n >= 0 && sum < totalw) {
			float rf = 1.0f;
			if (maxw * (n+1) < totalw) {
				maxw = totalw / (float)(n+1);
			} else {
				rf = (totalw - sum) / ((n+1) * maxw - sum);
			}
			for (int i = 0; i <= n; ++i) {
				float w = widths.get(i);
				widths.set(i, w + (maxw - w) * rf);
			}
		}
		for (int i = n; i >= 0; --i) {
			cm.getColumn(1+i).setPreferredWidth(Math.round(widths.get(i)));
		}
	}
	
	//----------------------------------------------------------------------------
	/** Table cell renderer for double values. It uses US locale, displays maximum six
	 *  fraction digits and doesn't use grouping.
	 */
	@SuppressWarnings("serial")
	public static class USDoubleRenderer extends DefaultTableCellRenderer implements TableCellRenderer {

	   private static final Border SAFE_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);

	   private Color unselectedForeground; 
       private Color unselectedBackground; 
       private NumberFormat nf = null;
       
       public USDoubleRenderer() {
    	   super();
		   nf = NumberFormat.getInstance(Locale.US);
		   nf.setMaximumFractionDigits(6);
		   nf.setGroupingUsed(false);
		   this.setHorizontalAlignment(JTextField.TRAILING);
       }
       
       private static Border getNoFocusBorder() {
           if (System.getSecurityManager() != null) {
               return SAFE_NO_FOCUS_BORDER;
           } else {
               return noFocusBorder;
           }
       }
       
	   @Override
	   public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if (isSelected) {
				   super.setForeground(table.getSelectionForeground());
				   super.setBackground(table.getSelectionBackground());
				}
				else {
				    super.setForeground((unselectedForeground != null) ? unselectedForeground 
				                                                       : table.getForeground());
				    super.setBackground((unselectedBackground != null) ? unselectedBackground 
				                                                       : table.getBackground());
				}
				
				setFont(table.getFont());

				if (hasFocus) {
			            Border border = null;
			            if (isSelected) {
			                border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");
			            }
			            if (border == null) {
			                border = UIManager.getBorder("Table.focusCellHighlightBorder");
			            }
			            setBorder(border);

				    if (!isSelected && table.isCellEditable(row, column)) {
			                Color col;
			                col = UIManager.getColor("Table.focusCellForeground");
			                if (col != null) {
			                    super.setForeground(col);
			                }
			                col = UIManager.getColor("Table.focusCellBackground");
			                if (col != null) {
			                    super.setBackground(col);
			                }
				    }
				} else {
			            setBorder(getNoFocusBorder());
				}
		
				if (value == null)  {
					setValue(value);
					return this;
				}
				
				double dValue = ((Double)value).doubleValue();
				
				setValue(nf.format(dValue));
				
				return this;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static class MessageBoard extends JDialog {
		private static final long serialVersionUID = 1L;
		private JLabel textLabel;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public MessageBoard(Frame owner, String... message) {
			super(owner,false);
			initialize(owner,message);
		}
		
		//----------------------------------------------------------------------------------------------------
		public MessageBoard(Dialog owner, String... message) {
			super(owner,false);
			initialize(owner,message);
		}
		
		//----------------------------------------------------------------------------------------------------
		public void showScreen() {
			setVisible(true);
			toFront();
			update(getGraphics());
		}
		
		//----------------------------------------------------------------------------------------------------
		public void hideScreen() {
			this.setVisible(false);
			dispose();
		}
		
		//----------------------------------------------------------------------------------------------------
		public void setMessage(String... message) {
			String text = "<html><center>";
			for (String s : message) text += s + "<br>";
			text += "<center></html>";
			textLabel.setText(text);
		}
		
		//----------------------------------------------------------------------------------------------------
		private void initialize(Window owner, String... message) {
			String text = "<html><center>";
			for (String s : message) text += s + "<br>";
			text += "<center></html>";
			textLabel = new JLabel(text,JLabel.CENTER);
			JPanel panel = new JPanel();
			panel.add(textLabel);
			panel.setBorder(BorderFactory.createCompoundBorder(
							BorderFactory.createBevelBorder(BevelBorder.RAISED),
						    BorderFactory.createEmptyBorder(10,10,10,10)));
			this.setUndecorated(true);
			this.setContentPane(panel);
			this.pack();
			final Dimension oldD = this.getPreferredSize();
			final Dimension newD = GUIUtils.getPreferredSize(this);
			if (!oldD.equals(newD)) 
				this.setPreferredSize(newD);
			this.pack();
			this.setLocationRelativeTo(owner);
		}
	}
}
