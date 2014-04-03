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
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import kepletszerkeszto.JDialogKeret;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.events.HybridAction;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.viewmanager.ParameterSet;
import ai.aitia.meme.viewmanager.ParameterSet.Par;
import bemenetek.Fugg;
import bemenetek.Valt;

/** Editor component to expand other text component. It enables for the users to
 *  create longer scripts, conditions, etc. */  
public class ExpandedEditor extends JPanel implements java.awt.event.ActionListener
{
	private static final long serialVersionUID = 1L;
	
	/** The Beanshell help document. */
	private File helpFile = new File(MEMEApp.g_AppDir, "Documents/MEME_Beanshell.pdf"); 

	/** Flag that determines whether the user cancels this dialog or not. */
	private boolean canceled = false;
	/** Flag that determines whether is displayed the 'Help' button on the dialog or not. */
	private boolean withHelp = true;
	
	private JDialog jDialog = null;
	private JLabel jLabel = null;
	private JPanel jButtonsPanel = null;
	private JButton jOKButton = null;
	private JButton jCancelButton = null;
	private JButton jHelpButton = null;
	private JButton jAssistantButton = null;
	private JScrollPane jScrollPane = null;
	private JTextArea jTextArea = null;

	//-------------------------------------------------------------------------
	/** Action class. It displays the dialog and copies its content to a text component
	 *  when the user close (but not cancels) it.
	 */
	public static class Action extends HybridAction {
		private static final long serialVersionUID = 1L;

		/** The expanded text component. */
		final JTextComponent tc;
		/** Does it shows 'Help' button or not? */
		final boolean withHelp;
		
		final ParameterSet parameterSet;

		public Action(final JTextComponent tc, final String name, final boolean withHelp, final ParameterSet parameterSet, final Object... args) {
			super(null, name, "expand_textedit.png", Utils.insert(args, 0, SHORT_DESCRIPTION, "Open multi-line editor"));
			this.tc = tc;
			this.withHelp = withHelp;
			this.parameterSet = parameterSet;
		}

		@Override 
		public void actionPerformed(ActionEvent e) {
			if (parameterSet != null) {
				final List<Par> allPars = parameterSet.getAllPars();
				for (final Par p : allPars) 
					Valt.v.setValtozo(p.getName(),p.getDatatype().getHumanType());
			}
			tc.setText( show(tc.getTopLevelAncestor(), getName(), null, tc.getText(), withHelp) );
		}
	}

	//-------------------------------------------------------------------------
	public static void test(String[] args) {
		show(null, "Description:", null, "",true);
		System.exit(0);
	}

	//-------------------------------------------------------------------------
	/** Creates a button that displays this dialog. 
	 * @param tc the expandable text component
	 * @param name the name of the text component
	 * @param withHelp does the dialog shows the 'Help' button?
	 */
	public static JButton makeEEButton(final JTextComponent tc, final String name, final boolean withHelp, final ParameterSet parameters) {
		JButton ans = new JButton(new Action(tc,name,withHelp,parameters));
		initializeKepletszerkeszto();
		ans.setText("");
		ans.setContentAreaFilled(false);	
		ans.setPreferredSize(new java.awt.Dimension(30, 30));
		return ans;
	}

	//----------------------------------------------------------------------------------------------------
	public static JButton makeEEButton(JTextComponent tc, String name) { return makeEEButton(tc,name,false,null); }
	
	//-------------------------------------------------------------------------
	/** Displays the dialog.
	 * @param relativeTo the parent component
	 * @param name name of the expandable text component
	 * @param title title of the dialog
	 * @param text initial content of the editor
	 * @param withHelp does the dialog shows the 'Help' button?
	 */
	public static String show(java.awt.Component relativeTo, String name, String title, String text, boolean withHelp) {
		name = name.replace("&", "");
		if (title == null)
			title = name.replaceFirst("\\s*:.*", "");

		ExpandedEditor panel = new ExpandedEditor(withHelp);
		panel.jDialog = GUIUtils.disposeOnClose(GUIUtils.createDialog(SwingUtilities.getWindowAncestor(relativeTo), true, title, panel));
		panel.jDialog.setLocationRelativeTo(relativeTo);
		panel.jDialog.setName("dial_eeditor");

		panel.jLabel.setText(name);
		panel.jTextArea.setText(text == null ? "" : text);
		panel.jDialog.setVisible(true);
		return panel.canceled ? text : panel.getText();
	}

	//-------------------------------------------------------------------------
	public String getText() { return jTextArea.getText(); }

	//-------------------------------------------------------------------------
	public void actionPerformed(java.awt.event.ActionEvent e) {
		if (e.getSource() == getJOKButton()) 
			jDialog.setVisible(false);
		else if (e.getSource() == getJCancelButton()) {
			canceled = true;
			jDialog.setVisible(false);
		} else if (e.getSource() == getJHelpButton()) {
			MEMEApp.LONG_OPERATION.begin(String.format("Opening %s...", helpFile.toString()), new LongRunnable() {
				@Override public void trun() throws Exception {
					MEMEApp.getOSUtils().openDocument(helpFile.toURI(), helpFile.getParentFile());
				}
			});
		} else if (e.getSource() == getJAssistantButton()) {
			Fugg.f.javaFugg = true;
			JDialogKeret jdk = new JDialogKeret(jDialog);
			String text = jdk.getSource();
			if (!"".equals(text))
				jTextArea.setText(text);
			jdk.dispose();
		}
	}

	//-------------------------------------------------------------------------
	public ExpandedEditor(boolean withHelp) {
		this.withHelp = withHelp;
		initialize();
	}
	
	//----------------------------------------------------------------------------------------------------
	public ExpandedEditor() { this(true); }
	public static ImageIcon getIcon(String name) { return MainWindow.getIcon(name); }

	//----------------------------------------------------------------------------------------------------
	private void initialize() {
		this.setLayout(new BorderLayout());
		
		int b = GUIUtils.GUI_unit(0.5);
		this.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
		
		jLabel = new JLabel();
		jLabel.setText("Description:");
		this.add(jLabel, BorderLayout.NORTH);
		this.add(getJButtonsPanel(), BorderLayout.SOUTH);
		this.add(getJScrollPane(), BorderLayout.CENTER);
	}

	//----------------------------------------------------------------------------------------------------
	private JPanel getJButtonsPanel() {
		if (jButtonsPanel == null) {
			jButtonsPanel = new JPanel();
			jButtonsPanel.setLayout(new BoxLayout(getJButtonsPanel(), BoxLayout.X_AXIS));
			jButtonsPanel.add(Box.createHorizontalGlue());
			jButtonsPanel.add(getJOKButton(), null);
			jButtonsPanel.add(Box.createHorizontalGlue());
			jButtonsPanel.add(getJCancelButton(), null);
			jButtonsPanel.add(Box.createHorizontalGlue());
			if (withHelp) {
				jButtonsPanel.add(getJHelpButton(), null);
				jButtonsPanel.add(Box.createHorizontalGlue());
				jButtonsPanel.add(getJAssistantButton(), null);
				jButtonsPanel.add(Box.createHorizontalGlue());
			}
		}
		return jButtonsPanel;
	}

	//----------------------------------------------------------------------------------------------------
	private JButton getJOKButton() {
		if (jOKButton == null) {
			jOKButton = new JButton();
			jOKButton.setText("OK");
			jOKButton.setName("btn_ok");
			jOKButton.setPreferredSize(getJCancelButton().getPreferredSize());
			jOKButton.addActionListener(this);
		}
		return jOKButton;
	}

	//----------------------------------------------------------------------------------------------------
	private JButton getJCancelButton() {
		if (jCancelButton == null) {
			jCancelButton = new JButton();
			jCancelButton.setText("Cancel");
			jCancelButton.setName("btn_cancel");
			jCancelButton.addActionListener(this);
		}
		return jCancelButton;
	}

	//----------------------------------------------------------------------------------------------------
	private JButton getJHelpButton() {
		if (jHelpButton == null) {
			jHelpButton = new JButton();
			jHelpButton.setText("Help");
			jHelpButton.setName("btn_help");
			jHelpButton.setPreferredSize(getJCancelButton().getPreferredSize());
			jHelpButton.addActionListener(this);
		}
		return jHelpButton;
	}
	
	//----------------------------------------------------------------------------------------------------
	private JButton getJAssistantButton() {
		if (jAssistantButton == null) {
			jAssistantButton = new JButton();
			jAssistantButton.setText("Assistant");
			jAssistantButton.setPreferredSize(getJCancelButton().getPreferredSize());
			jAssistantButton.addActionListener(this);
		}
		return jAssistantButton;
	}
	
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setViewportView(getJTextArea());
			int u = GUIUtils.GUI_unit(18); 
			jScrollPane.setBorder(BorderFactory.createEmptyBorder(u/30, 0, u/20, 0));
			jScrollPane.setPreferredSize(new java.awt.Dimension(u*6/5,u*13/15));
		}
		return jScrollPane;
	}

	//----------------------------------------------------------------------------------------------------
	private JTextArea getJTextArea() {
		if (jTextArea == null) 
			jTextArea = new JTextArea();
		jTextArea.setName("fld_eeditor");
		return jTextArea;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void initializeKepletszerkeszto() {
		Valt.v.clear();
		Valt.v.setValtozo("$Run$","real");
		Valt.v.setValtozo("$Tick$","real");
				
		Fugg.f.clear();
		Fugg.f.setFuggveny("get(\"name\")","Returns the value of the parameter named 'name'.");
		Fugg.f.setFuggveny("geti(\"name\")","Returns the value of the input parameter named 'name'.");
		Fugg.f.setFuggveny("geto(\"name\")","Returns the value of the output parameter named 'name'.");
		Fugg.f.setFuggveny("getv(\"name\")","Returns the value of the 'this view' parameter named 'name'.");
		Fugg.f.setFuggveny("gett(\"name\")","Returns technical parameter values.");
		Fugg.f.setFuggveny("call(\"fn\", args...)","Calls the aggregation operation named 'fn'.");
		Fugg.f.setFuggveny("round(\"name\", precision)","Rounds the value of the parameter named 'name'.");
	}
}
