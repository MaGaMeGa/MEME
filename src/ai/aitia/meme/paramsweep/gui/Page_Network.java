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
package ai.aitia.meme.paramsweep.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import ai.aitia.meme.gui.Preferences;
import ai.aitia.meme.gui.PreferencesPage;
import ai.aitia.meme.gui.Preferences.Button;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.gui.WizardPreferences.IReinitalizeable;
import ai.aitia.meme.paramsweep.sftp.SftpClient;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.FormsUtils.Separator;

import com.jcraft.jsch.SftpException;

/** This class provides the Network page of the Preferences dialog of the wizard. */
public class Page_Network extends PreferencesPage implements ActionListener,
															 CaretListener,
															 IReinitalizeable {

	private enum Type { LOCAL, REMOTE, QCG, VCLOUD };
	

	//=====================================================================================
	//members
	
	private static final long serialVersionUID = 1L;
	public static final int MAX_WORKERS = 59;
	
    private static final ExperimentType[] types = new ExperimentType[] { new ExperimentType("STANDALONE","Run simulations on local computer",
    																						Type.LOCAL),
    																	 new ExperimentType("VCLOUD","Run simulations on the Model Exploration Server",
    																			 			Type.VCLOUD),
//    																	 new ExperimentType("NETWORK","Run simulations on a dedicated cluster/grid",
//    																			 		    Type.REMOTE),
//    																	 new ExperimentType("QCG","Create simulation job for QosCosGrid",Type.QCG)
    																   };

	
	/** The owner of the page. */
	private WizardPreferences owner = null;
	
	//=====================================================================================
	// GUI members
	
	private JPanel choices = null;
	private JComboBox choicesBox = new JComboBox(types); 
	
	private JPanel centerPanel = new JPanel();
	private JLabel localPanel = new JLabel("");
	private JPanel networkPanel = null;
	private JPanel qcgPanel = null;
	private JPanel vCloudPanel = null;
	private JPanel advancedPanel = null;
	
	private JTextField hostField = new JTextField();
	private JTextField portField = new JTextField();
	private JTextField userField = new JTextField();
	private JTextField workspaceField = new JTextField();
	private JPasswordField passwordField = new JPasswordField();
	private JTextField privateKeyField = new JTextField();
	private JButton privateBrowseButton = new JButton("Browse...");
	private JPasswordField passphraseField = new JPasswordField();
	private JTextField sshPortField = new JTextField("22");
	private JButton restoreButton = new JButton("Restore");
	private JTextField addressField = new JTextField();
	private JCheckBox startMonitorCheckBox = new JCheckBox("Start monitor after wizard is closed");
	
	private JLabel hostLabel = new JLabel("Hostname: ");
	private JLabel portLabel = new JLabel("Port: ");
	private JLabel userLabel = new JLabel("Username: ");
	private JLabel passwordLabel = new JLabel("Password: ");
	private JLabel privateLabel = new JLabel("Private key file: ");
	private JLabel passphraseLabel = new JLabel("Passphrase: ");
	private JLabel sshPortLabel = new JLabel("Remote SSH port: ");
	private JLabel workspaceLabel = new JLabel("Workspace: ");
	private JLabel emailLabel = new JLabel("E-mail address: ");
	private JLabel requestedWorkersLabel = new JLabel("Number of requested workers: ");
	
	private JSpinner workersNumber = new JSpinner();
	
	private JLabel maxWorkersLabel = new JLabel("(max." + MAX_WORKERS + " workers)");
	
	private JTextField vCloudUserField = new JTextField();
	private JPasswordField vCloudPasswordField = new JPasswordField();
	private JRadioButton leasedMode = new JRadioButton("Leased mode");
	private JRadioButton bestEffortMode = new JRadioButton("Best effort mode");
	private JSpinner vCloudWorkersNumber = new JSpinner();
	private JPanel leasedPanel = null;
	private Separator advancedSeparator = new Separator("Advanced settings");
	private JButton advancedButton = new JButton("Show advanced");
	private JTextField vCloudHostname = new JTextField();
	private JTextField vCloudPort = new JTextField();
	
	private JLabel vCloudUserLabel = new JLabel("(Registrated) E-mail address: ");
	private JLabel vCloudPasswordLabel = new JLabel("Password: ");
	private JLabel vCloudRunOptionLabel = new JLabel("Run option: ");
	private JLabel vCloudWorkersLabel = new JLabel("Number of requested workers: ");
	private JLabel vCloudHostLabel = new JLabel("Hostname: ");
	private JLabel vCloudPortLabel = new JLabel("Port: ");
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the owner of the page
	 */
	public Page_Network(WizardPreferences owner) {
		super("Network");
		this.owner = owner;
		layoutGUI();
		initialize();
	}
	
	//=====================================================================================
	// implemented interfaces
	
	//-------------------------------------------------------------------------------------
	public void reinitialize() {
		Type type = Type.LOCAL;
		if (owner.isLocalRun())
			choicesBox.setSelectedItem(new ExperimentType("STANDALONE","",null));
		else if (owner.isVCloudRun()) {
			choicesBox.setSelectedItem(new ExperimentType("VCLOUD","",null));
			type = Type.VCLOUD;
		} else if (owner.isQCGRun()) { 
			choicesBox.setSelectedItem(new ExperimentType("QCG","",null));
			type = Type.QCG;
		} else {
			choicesBox.setSelectedItem(new ExperimentType("NETWORK","",null));
			type = Type.REMOTE;
		}
		hostField.setText(owner.getHostName());
		portField.setText(String.valueOf(owner.getPort()));
		userField.setText(owner.getUserName());
		passwordField.setText(owner.getPassword());
		privateKeyField.setText(owner.getPrivateKeyFile());
		passphraseField.setText(owner.getPassphrase());
		sshPortField.setText(String.valueOf(owner.getSSHPort()));
		workspaceField.setText(owner.getWorkspace());
		addressField.setText(owner.getEmail());
		startMonitorCheckBox.setSelected(owner.startMonitor());
		workersNumber.setValue(owner.getNumberOfRequestedWorkers());
		vCloudUserField.setText(owner.getVCloudUserName());
		vCloudPasswordField.setText(owner.getVCloudPassword());
		if (owner.isLeasedService())
			leasedMode.setSelected(true);
		else 
			bestEffortMode.setSelected(true);
		vCloudWorkersNumber.setValue(owner.getNumberOfRequestedVCloudWorkers());
		vCloudHostname.setText(owner.getVCloudHostName());
		vCloudPort.setText(String.valueOf(owner.getVCloudPort()));
		enableWidgets(type);
	}
	
	//-------------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("PRIVATE_BROWSE")) {
			File f = new File(privateKeyField.getText().trim());
			if (f == null || !f.exists())
				f = ParameterSweepWizard.getLastDir();
			JFileChooser chooser = new JFileChooser(f);
			chooser.setAcceptAllFileFilterUsed(true);
			if (f != ParameterSweepWizard.getLastDir())
				chooser.setSelectedFile(f);
			int result = chooser.showOpenDialog(this);
			if (result == JFileChooser.APPROVE_OPTION) {
				ParameterSweepWizard.setLastDir(chooser.getSelectedFile());
				privateKeyField.setText(chooser.getSelectedFile().getPath());
				passwordField.setText(null);
				passwordField.setEnabled(false);
			}
		} else if (command.equals("RESTORE"))
			sshPortField.setText("22");
		else if (command.equals("BOX")) {
			final ExperimentType type = (ExperimentType) choicesBox.getSelectedItem();
			enableWidgets(type.type);
			final CardLayout cl = (CardLayout) centerPanel.getLayout();
			cl.show(centerPanel,type.id);
			owner.enableDisableButtons();
		} else if (command.equals("ADVANCED")) {
			if (advancedButton.getText().equals("Show advanced")) {
				advancedButton.setText("  Hide advanced");
				advancedPanel.setVisible(true);
			} else {
				advancedButton.setText("Show advanced");
				advancedPanel.setVisible(false);
			}
		} else if (command.equals("LEASED")) {
			vCloudWorkersLabel.setEnabled(true);
			vCloudWorkersNumber.setEnabled(true);
			vCloudWorkersNumber.setValue(1);
		} else if (command.equals("BEST_EFFORT")) {
			vCloudWorkersLabel.setEnabled(false);
			vCloudWorkersNumber.setEnabled(false);
			vCloudWorkersNumber.setValue(0);
		}
	}

	//-------------------------------------------------------------------------------------
	public void caretUpdate(CaretEvent e) {
		Object source = e.getSource();
		if (!((JTextField)source).isEnabled()) return;
		if (source.equals(passwordField)) {
			privateKeyField.setText("");
			passphraseField.setText("");
			boolean enabled = (new String(passwordField.getPassword())).equals("");
			privateKeyField.setEnabled(enabled);
			privateBrowseButton.setEnabled(enabled);
			passphraseField.setEnabled(enabled);
		} else {
			passwordField.setText("");
			boolean enabled = privateKeyField.getText().trim().equals("") &&
			 				  (new String(passphraseField.getPassword()).equals(""));
			passwordField.setEnabled(enabled);
		}
	}

	//-------------------------------------------------------------------------------------
	@Override public String getInfoText(Preferences p) { return "Network connections and settings."; }
	@Override public boolean isEnabled(Button b) { return true; }

	//-------------------------------------------------------------------------------------
	@Override
	public boolean onButtonPress(Button b) {
		if (b == Button.CANCEL) return true;
		boolean ok = true;
		ExperimentType choice = (ExperimentType) choicesBox.getSelectedItem();
		if (choice.id.equals("NETWORK")) {
			String[] errors = checkFields();
			if (errors != null) {
				owner.warning(true,Utils.join(Arrays.asList(errors),"\n"),Preferences.WARNING,true);
				return false;
			}
			ok = checkSFTPConnection();
		} else if (choice.id.equals("VCLOUD")) {
			final List<String> errors = new ArrayList<String>(3);
			if (vCloudHostname.getText().trim().length() == 0)
				errors.add("The hostname of the Model Exploration Server is missing."); 
			if (vCloudPort.getText().trim().length() == 0)
				errors.add("The port of the Model Exploration Server is missing."); 
			if (!checkPort())
				errors.add("The port of the Model Exploration Server is invalid.");
			if (leasedMode.isSelected()) {
				final WorkersNumberState state = checkNoOfWorkers();
				
				if (state == WorkersNumberState.INVALID || state == WorkersNumberState.NONPOSITIVE)
					errors.add("The number of requested workers is invalid.");
				else if (state == WorkersNumberState.NOT_4_MULTIPLE)
					errors.add("The number of requested workers must be divisible by 4 because of technical reasons");
			}
			if (errors.size() > 0) {
				owner.warning(true,Utils.join(errors,"\n"),Preferences.WARNING,true); 
				return false;
			}
		}
		if (ok) {
			Properties p = owner.getProperties();
			p.setProperty(WizardPreferences.RUN_STRATEGY,(choice.id.equals("STANDALONE") ? "local" : (choice.id.equals("QCG") ? "qcg" :
														  choice.id.equals("NETWORK") ? "remote" : "vcloud" )));
			p.setProperty(WizardPreferences.HOSTNAME,hostField.getText().trim());
			p.setProperty(WizardPreferences.PORT,portField.getText().trim());
			p.setProperty(WizardPreferences.USERNAME,userField.getText().trim());
			p.setProperty(WizardPreferences.PASSWORD,new String(passwordField.getPassword()));
			p.setProperty(WizardPreferences.PRIVATE_KEY_FILE,privateKeyField.getText().trim());
			p.setProperty(WizardPreferences.PASSPHRASE,new String(passphraseField.getPassword()));
			p.setProperty(WizardPreferences.SSH_PORT,sshPortField.getText().trim());
			p.setProperty(WizardPreferences.WORKSPACE,workspaceField.getText().trim());
			p.setProperty(WizardPreferences.EMAIL,addressField.getText().trim());
			p.setProperty(WizardPreferences.START_MONITOR,String.valueOf(startMonitorCheckBox.isSelected()));
			p.setProperty(WizardPreferences.NO_OF_WORKERS,workersNumber.getValue().toString());
			p.setProperty(WizardPreferences.VCLOUD_USERNAME,vCloudUserField.getText().trim());
			p.setProperty(WizardPreferences.VCLOUD_PASSWORD,new String(vCloudPasswordField.getPassword()));
			p.setProperty(WizardPreferences.VCLOUD_RUN_OPTION,leasedMode.isSelected() ? "leased" : "best-effort");
			p.setProperty(WizardPreferences.VCLOUD_NO_OF_WORKERS,vCloudWorkersNumber.getValue().toString());
			owner.getVCloudConnectionProperties().setProperty(WizardPreferences.HOSTNAME,vCloudHostname.getText().trim());
			owner.getVCloudConnectionProperties().setProperty(WizardPreferences.PORT,vCloudPort.getText().trim());
		}
		return ok;
	}

	//=====================================================================================
	// GUI methods
	
	//-------------------------------------------------------------------------------------
	private void layoutGUI() {
		choices = FormsUtils.build("p ~ p ~ p",
								   "[DialogBorder]01",
								   	"Experiment option: ",choicesBox).getPanel();
		
		qcgPanel = FormsUtils.build("p ~ p ~ p ~ p:g",
									"[DialogBorder]000||" +
												  "123",
									new Separator("QosCosGrid settings"),
									requestedWorkersLabel,workersNumber,maxWorkersLabel).getPanel();
		
		networkPanel = FormsUtils.build("p ~ p:g(0.7) ~ p ~ p:g(0.3) ~ p",
				  						"[DialogBorder]00000||" +
				  									  "12344||" +
				  									  "56788||" +
				  									  "9AAAB||" +
				  									  "CDEFG||" +
				  									  "HIIII||" +
				  									  "JJJJJ||" +
				  									  "KLLLL||" +	
				  									  "MMMMM||" +
				  									  "NNNNN|",
				  						new Separator("Simulation server"),
				  						hostLabel,hostField,portLabel,portField,
				  						userLabel,userField,passwordLabel,passwordField,
				  						privateLabel,privateKeyField,privateBrowseButton,
				  						passphraseLabel,passphraseField,sshPortLabel,sshPortField,restoreButton,
				  						workspaceLabel,workspaceField,
				  						new Separator("E-mail settings"),
				  						emailLabel,addressField,
				  						new Separator("Other settings"),
				  						startMonitorCheckBox).getPanel();
		
		leasedPanel = FormsUtils.build("p ~ p:g(0.2) ~ p:g",
									   "01_|",
									   vCloudWorkersLabel,vCloudWorkersNumber).getPanel();
		
		advancedPanel = FormsUtils.build("p ~ p:g(0.35) ~ p:g(0.35) ~ p ~ p:g(0.3) ~ p",
										 "000000||" +
										 "12234_",
										 advancedSeparator,
										 vCloudHostLabel,vCloudHostname,vCloudPortLabel,vCloudPort).getPanel();
		
		vCloudPanel = FormsUtils.build("p ~ p:g(0.25) ~ p:g(0.25) ~ p ~ p:g(0.5) ~ p",
									   "[DialogBorder]000000||" +
									                 "122344||" +
									                 "567___||" +
									                 "888888||" +
									                 "99999A||" +
									                 "BBBBBB|",
									   new Separator("Model Exploration Server"),
									   vCloudUserLabel,vCloudUserField,vCloudPasswordLabel,vCloudPasswordField,
									   vCloudRunOptionLabel,leasedMode,bestEffortMode,
									   new Separator("Leased mode settings"),
									   leasedPanel,advancedButton,
									   advancedPanel).getPanel();
		
		final CardLayout cardLayout = new CardLayout();
		centerPanel.setLayout(cardLayout);
		centerPanel.add(localPanel,"STANDALONE");
		centerPanel.add(vCloudPanel,"VCLOUD");
		centerPanel.add(networkPanel,"NETWORK");
		centerPanel.add(qcgPanel,"QCG");
		
		this.setLayout(new BorderLayout());
		this.add(choices,BorderLayout.NORTH);
		this.add(centerPanel,BorderLayout.CENTER);
	}
	
	//-------------------------------------------------------------------------------------
	private void initialize() {
		choicesBox.setActionCommand("BOX");
		privateBrowseButton.setActionCommand("PRIVATE_BROWSE");
		restoreButton.setActionCommand("RESTORE");
		advancedButton.setActionCommand("ADVANCED");
		leasedMode.setActionCommand("LEASED");
		bestEffortMode.setActionCommand("BEST_EFFORT");
		
		workersNumber.setModel(new SpinnerNumberModel(1,1,59,1));
		vCloudWorkersNumber.setModel(new SpinnerNumberModel(1,1,100000,1));
		
		passwordField.addCaretListener(this);
		privateKeyField.addCaretListener(this);
		passphraseField.addCaretListener(this);
		
		GUIUtils.createButtonGroup(leasedMode,bestEffortMode);
		GUIUtils.addActionListener(this,choicesBox,privateBrowseButton,restoreButton,advancedButton,leasedMode,bestEffortMode);
		advancedPanel.setVisible(false);
		reinitialize();
	}

	//=====================================================================================
	// assistant methods
	
	//------------------------------------------------------------------------------------
	/** Enables/disables the components of the page according to the value of parameter
	 *  <code>isNetwork</code>.
	 */
	private void enableWidgets(Type type) {
		boolean isNetwork = type == Type.REMOTE;
		
		hostField.setEnabled(isNetwork);
		portField.setEnabled(isNetwork);
		userField.setEnabled(isNetwork);
		workspaceField.setEnabled(isNetwork);
		passwordField.setEnabled(isNetwork && privateKeyField.getText().trim().equals("") && (new String(passphraseField.getPassword())).equals(""));
		privateKeyField.setEnabled(isNetwork && (new String(passwordField.getPassword())).equals(""));
		privateBrowseButton.setEnabled(isNetwork && (new String(passwordField.getPassword())).equals(""));
		passphraseField.setEnabled(isNetwork && (new String(passwordField.getPassword())).equals(""));
		sshPortField.setEnabled(isNetwork);
		restoreButton.setEnabled(isNetwork);
		addressField.setEnabled(isNetwork);
		startMonitorCheckBox.setEnabled(isNetwork);
		
		hostLabel.setEnabled(isNetwork);
		portLabel.setEnabled(isNetwork);
		userLabel.setEnabled(isNetwork);
		passwordLabel.setEnabled(isNetwork);
		privateLabel.setEnabled(isNetwork);
		passphraseLabel.setEnabled(isNetwork);
		sshPortLabel.setEnabled(isNetwork);
		workspaceLabel.setEnabled(isNetwork);
		emailLabel.setEnabled(isNetwork);
		
		boolean isQCG = type == Type.QCG;
		
		requestedWorkersLabel.setEnabled(isQCG);
		workersNumber.setEnabled(isQCG);
		maxWorkersLabel.setEnabled(isQCG);
		
		boolean isCloud = type == Type.VCLOUD;
		
		vCloudUserLabel.setEnabled(isCloud);
		vCloudUserField.setEnabled(isCloud);
		vCloudPasswordLabel.setEnabled(isCloud);
		vCloudPasswordField.setEnabled(isCloud);
		vCloudRunOptionLabel.setEnabled(isCloud);
		leasedMode.setEnabled(isCloud);
		bestEffortMode.setEnabled(isCloud);
		vCloudWorkersLabel.setEnabled(isCloud && leasedMode.isSelected());
		vCloudWorkersNumber.setEnabled(isCloud && leasedMode.isSelected());
		advancedButton.setEnabled(isCloud);
		vCloudHostLabel.setEnabled(isCloud);
		vCloudHostname.setEnabled(isCloud);
		vCloudPortLabel.setEnabled(isCloud);
		vCloudPort.setEnabled(isCloud);
	}
	
	//------------------------------------------------------------------------------------
	/** Checks the content of the page. It examines the following: <br>
	 * <ul>
	 * <li>non-empty fields</li>
	 * <li>valid port number</li>
	 * <li>well-formed e-mail address</li>
	 * <li>existing private key file</li>
	 * </ul>
	 * @return the error messages (or null if there is no error)
	 */
	private String[] checkFields() {
		List<String> errors = new ArrayList<String>(); 
		// I. empty fields
		if ("".equals(hostField.getText().trim()))
			errors.add("Field 'Host' cannot be empty.");
		if ("".equals(userField.getText().trim()))
			errors.add("Field 'Username' cannot be empty.");
		
		// II. ports ( 1024 - 65535 and 1 - 65535 )
		String portStr = portField.getText().trim();
		int portNumber = -1;
		if ("".equals(portStr))
			errors.add("Field 'Port' cannot be empty.");
		else {
			try {
				int portNumberTmp = Integer.parseInt(portStr);
				if (portNumberTmp < 1024 || portNumberTmp > 65535)
					throw new NumberFormatException();
				portNumber = portNumberTmp;
			} catch (NumberFormatException e) {
				errors.add("Port number must be an integer between 1024 and 65535.");
			}
		}
		
		String sshPortStr = sshPortField.getText().trim();
		int sshPortNumber = -1;
		if ("".equals(sshPortStr))
			errors.add("Field 'Remote SSH Port' cannot be empty.");
		else {
			try {
				int sshPortNumberTmp = Integer.parseInt(sshPortStr);
				if (sshPortNumberTmp < 1 || sshPortNumberTmp > 65535)
					throw new NumberFormatException();
				sshPortNumber = sshPortNumberTmp;
			} catch (NumberFormatException e) {
				errors.add("Remote SSH port number must be an integer between 1 and 65535.");
			}
		} 
		
		if (portNumber != -1 && sshPortNumber != -1 && portNumber == sshPortNumber)
			errors.add("The port number and the remote SSH port number must be different.");
		
		// III.  email (empty or a@b.c)
		if (!"".equals(addressField.getText().trim())) {
				if (!Utilities.validateEmail(addressField.getText().trim()))
					errors.add("The format of the e-mail address is invalid.");
		}
		
		// IV. private key file (if any)
		if (!"".equals(privateKeyField.getText().trim())) {
			File file = new File(privateKeyField.getText().trim());
			if (file == null || !file.exists())
				errors.add("File not found: " + privateKeyField.getText().trim());
		}
		return (errors.size() == 0 ? null : errors.toArray(new String[0]));	
	}
	
	//------------------------------------------------------------------------------------
	/** Checks the SFTP connection with the server host. */
	private boolean checkSFTPConnection() {
		String host = hostField.getText().trim();
		String user = userField.getText().trim();
		String passwd = null; 
		String privateKeyFile = privateKeyField.getText().trim();
		String passphrase = null;
		int sshPort = -1;
		try {
			sshPort = Integer.parseInt(sshPortField.getText().trim());
		} catch (NumberFormatException e) { return false; }
		String workspace = workspaceField.getText().trim();
		if (workspace.endsWith("/") || workspace.endsWith("\\"))
			workspace = workspace.substring(0,workspace.length()-1);
		SftpClient client = null;
		if (privateKeyFile == null || "".equals(privateKeyFile)) {
			passwd = new String(passwordField.getPassword());
			client = new SftpClient(host,sshPort,user,passwd,workspace);
 		} else {
 			passphrase = new String(passphraseField.getPassword());
 			client = new SftpClient(host,sshPort,user,privateKeyFile,passphrase,workspace);
 		}
		
		String error = client.connect();
		
		if (owner.warning(error != null,error,Preferences.WARNING,false))
			return false;
		
		try {
			client.goToWorkspace();
		} catch (SftpException e) {
			owner.warning(true,"No such directory on host : " + workspace,Preferences.WARNING,true);
			client.disconnect();
			return false;
		}
		client.disconnect();
		return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean checkPort() {
		String portStr = vCloudPort.getText().trim();
		if ("".equals(portStr))	return true;
		else {
			try {
				int portNumberTmp = Integer.parseInt(portStr);
				if (portNumberTmp < 1024 || portNumberTmp > 65535)
					throw new NumberFormatException();
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private WorkersNumberState checkNoOfWorkers() {
		try {
			int workers = (Integer) vCloudWorkersNumber.getValue();
			if (workers <= 0) 
				return WorkersNumberState.NONPOSITIVE;
//			else if (workers % 4 > 0)
//				return WorkersNumberState.NOT_4_MULTIPLE;
			return WorkersNumberState.ACCEPTABLE;
		} catch (final ClassCastException _) {
			return WorkersNumberState.INVALID;
		}
	}
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	public static enum WorkersNumberState {
		ACCEPTABLE, NONPOSITIVE, NOT_4_MULTIPLE ,INVALID 
	}
	
	//----------------------------------------------------------------------------------------------------
	private static class ExperimentType {
		
		//====================================================================================================
		// members
		
		public final String id;
		public final String label;
		public final Type type;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public ExperimentType(final String id, final String label, final Type type) {
			if (id == null || id.trim().length() == 0)
				throw new IllegalArgumentException("'id' is null.");
			this.id = id;
			this.label = label;
			this.type = type;
		}
		
		//----------------------------------------------------------------------------------------------------
		@Override public String toString() { return label; }
		@Override public int hashCode() { return id.hashCode(); }
		
		//----------------------------------------------------------------------------------------------------
		@Override 
		public boolean equals(final Object o) {
			if (o instanceof ExperimentType) {
				ExperimentType that = (ExperimentType) o;
				return this.id.equals(that.id);
			}
			return false;
		}
	}
}
