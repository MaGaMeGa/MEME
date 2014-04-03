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

import static ai.aitia.meme.utils.GUIUtils.GUI_unit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.DatabaseSettings;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.TestableDialog;
import ai.aitia.meme.utils.Utils;

/** GUI-component that enables to change database settings. */
public class DbSettingsDialog implements java.awt.event.ActionListener 
{
	private JPanel jChoosePanel = null;

	private JLabel jLabel = null;

	private JRadioButton jIntDbRadioButton = null;

	private JRadioButton jExtDbRadioButton = null;

	private JPanel jIntDbSettingsPanel = null;  //  @jve:decl-index=0:visual-constraint="145,80"

	private JLabel jLabel1 = null;

	private JTextField jIntDbTextField = null;

	private JButton jBrowseButton = null;

	private JPanel jExtDbSettingsPanel = null;  //  @jve:decl-index=0:visual-constraint="31,171"

	private JLabel jLabel2 = null;

	private JTextField jExtDbTextField = null;

	private JLabel jLabel3 = null;

	private JTextField jLoginTextField = null;

	private JLabel jLabel4 = null;

	private JPasswordField jPasswordField = null;

	private JPanel jSettingsPanel = null;  //  @jve:decl-index=0:visual-constraint="552,194"
	

	//=========================================================================
	//	Public methods

	//-------------------------------------------------------------------------
	public DbSettingsDialog() {
		jLabel = new JLabel();
		jLabel.setText("Choose database engine:");
	}

	//-------------------------------------------------------------------------
	/** Shows the dialog. 
	 * Returns true if the settings has been changed thus re-connection is needed */
	public boolean start(java.awt.Component parent) 
	{
		DatabaseSettings settings = MEMEApp.getDbSettings();

		boolean ans = (MEMEApp.getDatabase().getConnection() == null);
		boolean origExternal	= settings.isExternal();
		String origIntDbPath 	= settings.getIntDbPath();
		String origExtConnStr	= settings.getExtConnStr();
		String origLogin		= settings.getLogin();
		String origPwd			= settings.getPwd();
		
		getJIntDbTextField().setText( DatabaseSettings.fromUnixStylePath(origIntDbPath) );
		getJExtDbTextField().setText( origExtConnStr );
		// hack to preserve externals data as far as it is possible
		if ( !origLogin.equals(DatabaseSettings.INTERNAL_USER) ) {
			getJLoginTextField().setText( origLogin );
			getJPasswordField().setText( origPwd );
		}
		else {
			getJLoginTextField().setText( "" );
			getJPasswordField().setText( "" );
		}

		Object[] options = {"OK", "Cancel"};
//		Object[] components = { getJChoosePanel(), getJSettingsPanel() };
		
		final JPanel p = FormsUtils.build("p:g","0|1",getJChoosePanel(),getJSettingsPanel()).getPanel();
		final JScrollPane sp = new JScrollPane(p);
		sp.setBorder(null);
		
		getJSettingsPanel().add(getJIntDbSettingsPanel(), BorderLayout.CENTER);

		Utils.invokeLater(settings.isExternal() ? getJExtDbRadioButton() : getJIntDbRadioButton(), "doClick");
		
		int choice = TestableDialog.showOptionDialog(parent, sp, "Database settings", 
									JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
									null, options, options[0],"");
		if (choice == 0) {
			if (getJIntDbRadioButton().isSelected()) {
		    	Utils.Ref<String> dbpath = new Utils.Ref<String>(getJIntDbTextField().getText());
				String err = checkLocation(dbpath, true);
				if (err != null) {
					ans = false;
					MEMEApp.userErrors("MEME Error", "Bad database location:", err );
				} else if (ans || origExternal || !origIntDbPath.equals(dbpath.get())) {
					ans = true;
					settings.setToInternal(dbpath.get());
					settings.restoreInternal();	// hack to restore user/pwd for internal
				}
			}
			else if (getJExtDbTextField().getText().length() > 0) {
				String pwd = new String(getJPasswordField().getPassword());
				ans = ans || (!origExternal || !origExtConnStr.equals(getJExtDbTextField().getText())
											|| !origLogin.equals(getJLoginTextField().getText())
											|| !origPwd.equals(pwd));
				if (ans) {
					//settings.setToExternal(getJExtDbTextField().getText());
					settings.setToExternal(getConnectionString(getJExtDbTextField().getText()));
					settings.setLogin(getJLoginTextField().getText());
					settings.setPwd(pwd);
				}
			}
		}
		return ans;
	}
	
	private String getConnectionString(String c) {
		String n = Utils.getCatalogName(c);
		if ( n.equals("") ) { //partial connection string, add default name
			if ( c.endsWith("/") ) {	// there is separator
				return c + DatabaseSettings.DEFAULT_EXTERNAL_DATABASE_NAME;
			}
			else {	// no separator, add it, too
				return c + "/" + DatabaseSettings.DEFAULT_EXTERNAL_DATABASE_NAME;
			}
		}
		return c;	// whole, nothing to do	
	}
	
	//=========================================================================
	//	Internals

	//-------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == getJIntDbRadioButton()) {
			getJSettingsPanel().removeAll();
			getJSettingsPanel().add(getJIntDbSettingsPanel(), BorderLayout.CENTER);
			((java.awt.Window)javax.swing.SwingUtilities.getRoot(getJSettingsPanel())).pack();
		}
		else if (e.getSource() == getJExtDbRadioButton()) {
			getJSettingsPanel().removeAll();
			getJSettingsPanel().add(getJExtDbSettingsPanel(), BorderLayout.CENTER);
			((java.awt.Window)javax.swing.SwingUtilities.getRoot(getJSettingsPanel())).pack();
		}
		else if (e.getSource() == getJBrowseButton()) {
			java.io.File f = new java.io.File(jIntDbTextField.getText());

		    JFileChooser chooser = new JFileChooser();
		    chooser.setName("filechooser_dbchooser");
		    chooser.setAcceptAllFileFilterUsed(false);
		    chooser.addChoosableFileFilter(new SimpleFileFilter("HSQLDB database files (*.script)"));
		    chooser.setCurrentDirectory(f.getParentFile());
		    int returnVal = chooser.showDialog(getJBrowseButton().getTopLevelAncestor(),"Select");
		    if (returnVal == JFileChooser.APPROVE_OPTION) {
		    	Utils.Ref<String> s = new Utils.Ref<String>(chooser.getSelectedFile().toString());
		    	if (checkLocation(s, false) == null)
		    		jIntDbTextField.setText(s.get());
		    }
		}
	}

	//-------------------------------------------------------------------------
	/** Checks whether 's' is a valid location of a HSQLDB file or not. */
	private String checkLocation(Utils.Ref<String> s, boolean isfinal) {
    	try {
    		String u = DatabaseSettings.toUnixStylePath(s.get());
    		s.set(DatabaseSettings.fromUnixStylePath(u));
    		if (isfinal) s.set(u);
    	} catch (Exception e) {
    		return Utils.getLocalizedMessage(e);
    	}
    	return null;
	}


	private JPanel getJChoosePanel() {
		if (jChoosePanel == null) {
			jChoosePanel = new JPanel();
			jChoosePanel.setLayout(new BoxLayout(getJChoosePanel(), BoxLayout.Y_AXIS));
			jChoosePanel.add(jLabel);
			jChoosePanel.add(Box.createVerticalStrut(GUI_unit(0.6)));
			jChoosePanel.add(getJIntDbRadioButton());
			jChoosePanel.add(getJExtDbRadioButton());
			
			ButtonGroup grp = new ButtonGroup();
			grp.add(getJIntDbRadioButton());
			grp.add(getJExtDbRadioButton());
		}
		return jChoosePanel;
	}

	private JRadioButton getJIntDbRadioButton() {
		if (jIntDbRadioButton == null) {
			jIntDbRadioButton = new JRadioButton();
			jIntDbRadioButton.setName("rbtn_dbchooser_internal");
			jIntDbRadioButton.setText("Built-in database engine");
			jIntDbRadioButton.addActionListener(this);
		}
		return jIntDbRadioButton;
	}

	private JRadioButton getJExtDbRadioButton() {
		if (jExtDbRadioButton == null) {
			jExtDbRadioButton = new JRadioButton();
			jExtDbRadioButton.setName("rbtn_dbchooser_external");
			jExtDbRadioButton.setText("Other database engine");
			jExtDbRadioButton.addActionListener(this);
		}
		return jExtDbRadioButton;
	}

	private JPanel getJIntDbSettingsPanel() {
		if (jIntDbSettingsPanel == null) {
			GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
			gridBagConstraints8.gridx = 1;
			gridBagConstraints8.insets = new Insets(GUI_unit(0.25), GUI_unit(0.25), 0, 0);
			gridBagConstraints8.gridy = 1;
			GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints7.gridy = 1;
			gridBagConstraints7.weightx = 1.0;
			gridBagConstraints7.insets = new Insets(GUI_unit(0.5), 0, 0, 0);
			gridBagConstraints7.anchor = GridBagConstraints.NORTH;
			gridBagConstraints7.gridx = 0;
			GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.gridwidth = 2;
			gridBagConstraints6.gridy = 0;
			gridBagConstraints6.anchor = GridBagConstraints.WEST;
			gridBagConstraints6.gridx = 0;
			jLabel1 = new JLabel();
			jLabel1.setText("Location of database files:");
			jIntDbSettingsPanel = new JPanel();
			jIntDbSettingsPanel.setLayout(new GridBagLayout());
			jIntDbSettingsPanel.add(jLabel1, gridBagConstraints6);
			jIntDbSettingsPanel.add(getJIntDbTextField(), gridBagConstraints7);
			jIntDbSettingsPanel.add(getJBrowseButton(), gridBagConstraints8);
		}
		return jIntDbSettingsPanel;
	}

	private JTextField getJIntDbTextField() {
		if (jIntDbTextField == null) {
			jIntDbTextField = new JTextField();
			jIntDbTextField.setName("fld_dbchooser_intdb");
			int h = jIntDbTextField.getPreferredSize().height;
			jIntDbTextField.setPreferredSize(new java.awt.Dimension(GUI_unit(25), h));
		}
		return jIntDbTextField;
	}

	private JButton getJBrowseButton() {
		if (jBrowseButton == null) {
			jBrowseButton = new JButton();
			jBrowseButton.setText("Browse");
			jBrowseButton.setName("btn_dbchooser_browse");
			jBrowseButton.addActionListener(this);
		}
		return jBrowseButton;
	}

	private JPanel getJExtDbSettingsPanel() {
		if (jExtDbSettingsPanel == null) {
			int b = GUI_unit(0.6);
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints5.gridy = 2;
			gridBagConstraints5.weightx = 1.0;
			gridBagConstraints5.insets = new Insets(b, b, b, 0);
			gridBagConstraints5.gridx = 3;
			GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.gridx = 2;
			gridBagConstraints4.gridy = 2;
			jLabel4 = new JLabel();
			jLabel4.setText("Pasword:");
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints3.gridy = 2;
			gridBagConstraints3.weightx = 1.0;
			gridBagConstraints3.insets = new Insets(b, b, b, b);
			gridBagConstraints3.gridx = 1;
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 0;
			gridBagConstraints2.anchor = GridBagConstraints.WEST;
			gridBagConstraints2.gridy = 2;
			jLabel3 = new JLabel();
			jLabel3.setText("Username:");
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints1.gridy = 1;
			gridBagConstraints1.weightx = 1.0;
			gridBagConstraints1.gridwidth = 4;
			gridBagConstraints1.insets = new Insets(b, 0, 0, 0);
			gridBagConstraints1.gridx = 0;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.gridwidth = 4;
			gridBagConstraints.anchor = GridBagConstraints.WEST;
			gridBagConstraints.gridy = 0;
			jLabel2 = new JLabel();
			jLabel2.setText("Connection string: [if no database given 'meme' is used]");
			jExtDbSettingsPanel = new JPanel();
			jExtDbSettingsPanel.setLayout(new GridBagLayout());
			jExtDbSettingsPanel.add(jLabel2, gridBagConstraints);
			jExtDbSettingsPanel.add(getJExtDbTextField(), gridBagConstraints1);
			jExtDbSettingsPanel.add(jLabel3, gridBagConstraints2);
			jExtDbSettingsPanel.add(getJLoginTextField(), gridBagConstraints3);
			jExtDbSettingsPanel.add(jLabel4, gridBagConstraints4);
			jExtDbSettingsPanel.add(getJPasswordField(), gridBagConstraints5);
		}
		return jExtDbSettingsPanel;
	}

	private JTextField getJExtDbTextField() {
		if (jExtDbTextField == null) {
			jExtDbTextField = new JTextField();
			jExtDbTextField.setName("fld_dbchooser_extdb");
			jExtDbTextField.setPreferredSize(new Dimension(GUI_unit(25), jExtDbTextField.getPreferredSize().height));
		}
		return jExtDbTextField;
	}

	private JTextField getJLoginTextField() {
		if (jLoginTextField == null) {
			jLoginTextField = new JTextField();
			jLoginTextField.setName("fld_dbchooser_login");
		}
		return jLoginTextField;
	}

	private JPasswordField getJPasswordField() {
		if (jPasswordField == null) {
			jPasswordField = new JPasswordField();
			jPasswordField.setName("fld_dbchooser_password");
		}
		return jPasswordField;
	}

	private JPanel getJSettingsPanel() {
		if (jSettingsPanel == null) {
			jSettingsPanel = new JPanel();
			jSettingsPanel.setLayout(new BorderLayout());
			jSettingsPanel.setBorder(BorderFactory.createEmptyBorder(GUI_unit(0.6), 0, GUI_unit(0.6), 0));
		}
		return jSettingsPanel;
	}
}
