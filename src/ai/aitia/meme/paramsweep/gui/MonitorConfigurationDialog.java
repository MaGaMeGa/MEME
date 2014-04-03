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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import ai.aitia.meme.gui.SimpleFileFilter;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.sftp.SftpClient;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;

import com.jcraft.jsch.SftpException;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/** This class provides the graphical user interface of the configuration dialog of the
 *  MASS/MEME Monitor.
 */  
public class MonitorConfigurationDialog extends JDialog implements ActionListener,
																		 CaretListener {

	//=================================================================================
	// members
	
	private static final long serialVersionUID = 1L;

	private static final String salt = "FvHBedW4st1dsaa18Y1B1dJ4";
	private static final IvParameterSpec IvParameters = new IvParameterSpec( new byte[] { 12, 34, 56, 78, 90, 87, 65, 43 });
	
	/** The name of the file that stores the configuration of the monitor. */
	public static final String configurationFileName = "monitorconfiguration.mconfig";
	
	/** Default value constant: the host of the servers. */
	private static final String DEFAULT_HOST 			= "localhost";
	/** Default value constant: the port of the simulation server. */
	private static final String DEFAULT_SIMULATION_PORT = "3000";
	/** Default value constant: the username. */
	private static final String DEFAULT_USER 			= "user";
	/** Default value constant: password. */
	private static final String DEFAULT_PASSWORD		= "";
	/** Default value constant: the path of the private key file. */
	private static final String DEFAULT_PRIVATEKEY		= "";
	/** Default value constant: passphrase. */
	private static final String DEFAULT_PASSPHRASE		= "";
	/** Default value constant: SSH port. */
	private static final String DEFAULT_SSH_PORT		= "22";
	/** Default value constant: workspace of the servers. */
	private static final String DEFAULT_WORKSPACE		= "workspace";

	/** Property key constant: the host of the servers. */
	public static final String HOSTNAME			= "hostname";
	/** Property key constant: the port of the simulation server. */
	public static final String SIMULATION_PORT	= "simulation_port";
	/** Property key constant: the username. */
	public static final String USERNAME			= "username";
	/** Property key constant: password. */
	public static final String PASSWORD			= "password";
	/** Property key constant: the path of the private key file. */
	public static final String PRIVATE_KEY_FILE	= "private_key_file";
	/** Property key constant: passphrase. */
	public static final String PASSPHRASE		= "passphrase";
	/** Property key constant: SSH port. */
	public static final String SSH_PORT			= "ssh_port";
	/** Property key constant: workspace of the servers. */
	public static final String WORKSPACE		= "workspace";
	
	/** Property key constant: is the password is encoded? */
	public static final String PASSWORD_ENCODED = "password_encoded";
	/** Property key constant: is the passphrase is encoded*/
	public static final String PASSPHRASE_ENCODED = "passphrase_encoded";
	
	/** Key to encode/decode informations. */
	private static Key key = null;
	/** The parent of the dialog. */
	private Window owner = null;
	/** The object that stores the informations. */
	private Properties properties = null;
	/** Flag that determines whether the Cancel button is enabled or not. */
	private boolean cancelAllowed = true;
	/** Flag that determines whether the user closes this dialog by pressing Cancel or
	 *  the 'x' on right top corner or not. 
	 */
	private boolean isCancel = false;
	/** Flag that determines whether the monitor is called from the MEME or not. */
	private boolean fromMEME = false;

	//=================================================================================
	// GUI members
	
	private JPanel content = new JPanel(new BorderLayout());
	private JTextPane infoPane = new JTextPane();
	private JScrollPane infoScr = new JScrollPane(infoPane);
	private JTextField hostField = new JTextField();
	private JTextField simulationPortField = new JTextField();
	private JTextField userField = new JTextField();
	private JTextField workspaceField = new JTextField();
	private JPasswordField passwordField = new JPasswordField();
	private JTextField privateKeyField = new JTextField();
	private JButton privateBrowseButton = new JButton("Browse...");
	private JPasswordField passphraseField = new JPasswordField();
	private JTextField sshPortField = new JTextField("22");
	private JButton restoreButton = new JButton("Restore");
	private JPanel center = null;
	private JButton okButton = new JButton("OK");
	private JButton cancelButton = new JButton("Cancel");
	private JPanel bottom = new JPanel();
	private JButton saveButton = new JButton("Save configuration...");
	private JButton loadButton = new JButton("Load configuration...");
	private JPanel managePanel = null;
	
	//=================================================================================
	// methods
	
	//---------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the parent of the dialog
	 * @param fromMEME flag that detemines whether the dialog is called from the MEME
	 *        or not
	 */
	public MonitorConfigurationDialog(Frame owner, boolean fromMEME) {
		super(owner,fromMEME ? "Monitor configuration" : "Configuration",true);
		this.owner = owner;
		this.fromMEME = fromMEME;
		initializeKey();
		layoutGUI();
		initialize();
		properties = setSettingsFromFile(null);
	}
	
	//----------------------------------------------------------------------------------------------------
	public MonitorConfigurationDialog(Dialog owner, boolean fromMEME) {
		super(owner,fromMEME ? "Monitor configuration" : "Configuration",true);
		this.owner = owner;
		this.fromMEME = fromMEME;
		initializeKey();
		layoutGUI();
		initialize();
		properties = setSettingsFromFile(null);
	}
	
	//--------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the parent of the dialog
	 * @param cancelAllowed determines whether the Cancel button is enabled or not
 	 * @param fromMEME flag that detemines whether the dialog is called from the MEME
	 *        or not
	 */
	public MonitorConfigurationDialog(Frame owner, boolean cancelAllowed, boolean fromMEME) {
		this(owner,fromMEME);
		this.setTitle("MEME Simulation Monitor Configuration � 2007 - " + Calendar.getInstance().get(Calendar.YEAR) + " Aitia International, Inc.");
		this.cancelAllowed = cancelAllowed;
		cancelButton.setEnabled(this.cancelAllowed);
	}
	
	//----------------------------------------------------------------------------------------------------
	public MonitorConfigurationDialog(Dialog owner, boolean cancelAllowed, boolean fromMEME) {
		this(owner,fromMEME);
		this.setTitle("MEME Simulation Monitor Configuration � 2007 - " + Calendar.getInstance().get(Calendar.YEAR) + " Aitia International, Inc.");
		this.cancelAllowed = cancelAllowed;
		cancelButton.setEnabled(this.cancelAllowed);
	}
	
	//--------------------------------------------------------------------------------
	/** Shows the dialog.
	 * @return the current settings
	 */
	public Properties showDialog() {
		setVisible(true);
		Properties result = properties;
		dispose();
		return result;
	}
	
	//--------------------------------------------------------------------------------
	public Properties getProperties() { return properties; }
	public boolean isCancel() { return isCancel; }
	
	//--------------------------------------------------------------------------------
	/** Creates a new configuration file. 
	 * @param host the host of the servers
	 * @param simulation_port the port of the simulation server
	 * @param download_port the port of the manager server (or download server)
	 * @param user username
	 * @param password password
	 * @param private_key_file the path of the private key file
	 * @param passphrase passphrase
	 * @param workspace the workspace of the server
	 * @return false if any problems occure
	 */
	public static boolean createNewConfiguration(String host, int simulation_port, String user, String password, String private_key_file,
												 String passphrase, int ssh_port, String workspace) {
		initializeKey();
		Properties newProp = new Properties();
		newProp.setProperty(HOSTNAME,host);
		newProp.setProperty(SIMULATION_PORT,String.valueOf(simulation_port));
		newProp.setProperty(USERNAME,user);
		newProp.setProperty(SSH_PORT,String.valueOf(ssh_port));
		boolean isEncoded = true; 
		if (private_key_file == null) {
			String encoded = password;
			try {
				encoded = encode(password);
			} catch (Exception e1) { isEncoded = false; }
			newProp.setProperty(PASSWORD,encoded);
			newProp.setProperty(PASSWORD_ENCODED,String.valueOf(isEncoded));
			newProp.setProperty(PRIVATE_KEY_FILE,"");
			newProp.setProperty(PASSPHRASE,"");
			newProp.setProperty(PASSPHRASE_ENCODED,"false");
		} else {
			newProp.setProperty(PASSWORD,"");
			newProp.setProperty(PASSWORD_ENCODED,"false");
			newProp.setProperty(PRIVATE_KEY_FILE,private_key_file);
			if (passphrase != null) {
				String encoded = passphrase;
				try {
					encoded = encode(passphrase);
				} catch (Exception e1) { isEncoded = false; }
				newProp.setProperty(PASSPHRASE,encoded);
				newProp.setProperty(PASSPHRASE_ENCODED,String.valueOf(isEncoded));
			}
		}
		newProp.setProperty(WORKSPACE,workspace);
		try {
			File newFile = new File(configurationFileName + ".new");
			ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(newFile));
			os.writeObject(newProp);
			os.flush();
			os.close();
			File file = new File(configurationFileName);
			if (file.exists())
				file.delete();
			newFile.renameTo(file);
		} catch (FileNotFoundException e1) {
			// never happens
			throw new IllegalStateException();
		} catch (IOException e1) {
			e1.printStackTrace(ParameterSweepWizard.getLogStream());
			return false;
		}
		return true;
	}
	
	//-------------------------------------------------------------------------------------
	public void saveConfiguration() {
		String[] errors = checkFields();
		if (errors != null) {
			Utilities.userAlert(this,(Object[])errors);
			return;
		}
		if (!checkSFTPConnection()) return;
		JFileChooser chooser = new JFileChooser(ParameterSweepWizard.getLastDir());
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.addChoosableFileFilter(new SimpleFileFilter("Monitor configuration files (*.mconfig)"));
		int result = chooser.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File f = chooser.getSelectedFile();
			if (!f.getName().endsWith(".mconfig"))
				f = new File(f.getPath() + ".mconfig");
			ParameterSweepWizard.setLastDir(f);
			boolean save = true;
			if (f.exists()) {
				int ans = Utilities.askUser(this,false,"Override confirmation","File exists.","Do you want to override?");
				save = ans == 1;
			}
			if (save) { 
				 if (!save(f))
					 Utilities.userAlert(this,"Save failed.");
			}
		}
	}
	
	//-------------------------------------------------------------------------------------
	public void loadConfiguration() {
		JFileChooser chooser = new JFileChooser(ParameterSweepWizard.getLastDir());
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.addChoosableFileFilter(new SimpleFileFilter("Monitor configuration files (*.mconfig)"));
		int result = chooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File f = chooser.getSelectedFile();
			ParameterSweepWizard.setLastDir(f);
			Properties tempProp = setSettingsFromFile(f);
			if (tempProp != null) 
				properties = tempProp;
			else
				Utilities.userAlert(this,"Load failed.");
		}
	}
	
	//=================================================================================
	// implemented interfaces
	
	//---------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("CANCEL")) {
			isCancel = true;
			setVisible(false);
		} else if (command.equals("PRIVATE_BROWSE")) {
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
		} else if (command.equals("OK")) {
			String[] errors = checkFields();
			if (errors != null) {
				Utilities.userAlert(this,(Object[])errors);
				return;
			}
			if (checkSFTPConnection()) {
				save(null);
				isCancel = false;
				setVisible(false);
			}
		} else if (command.equals("SAVE")) 
			saveConfiguration();
		else if (command.equals("LOAD"))
			loadConfiguration();
		else if (command.equals("RESTORE"))
			sshPortField.setText("22");
	}
	
	//--------------------------------------------------------------------------------
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
			boolean enabled = privateKeyField.getText().trim().equals("") && (new String(passphraseField.getPassword()).equals(""));
			passwordField.setEnabled(enabled);
		}
	}
	
	//=================================================================================
	// GUI methods
	
	//---------------------------------------------------------------------------------
	private void layoutGUI() {
		
		managePanel = FormsUtils.build("p:g p ~ p",
				   					   "_01|",
				   					   saveButton,loadButton).getPanel();
		
		center = FormsUtils.build("p ~ p:g ~ p ~ p:g ~ p",
								  "[DialogBorder]01233||" +
								  				"45677||" +
								  				"8999A||" +
								  				"BCCCC||" +
								  				"DEEEF||" +
								  				"GHHHH||" +
								  				"IIIII",
								  "Host: ",hostField,"Port: ",simulationPortField,
								  "Username: ",userField,"Password: ",passwordField,
								  "Private key file: ",privateKeyField,privateBrowseButton,
								  "Passphrase: ",passphraseField,
								  "Remote SSH port: ",sshPortField,restoreButton,
								  "Workspace: ",workspaceField,
								  managePanel).getPanel();
		
		bottom.add(okButton);
		bottom.add(cancelButton);

		Box tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(infoScr);
		tmp.add(new JSeparator());
		content.add(tmp,BorderLayout.NORTH);
		content.add(center,BorderLayout.CENTER);
		tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(new JSeparator());
		tmp.add(bottom);
		content.add(tmp,BorderLayout.SOUTH);
		
		this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (cancelAllowed || fromMEME)
					MonitorConfigurationDialog.this.setVisible(false);
				else
					System.exit(0);
			}
		});
		final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(null);
		this.setContentPane(sp);
		this.setPreferredSize(new Dimension(500,285));
		this.pack();
		Dimension oldD = this.getPreferredSize();
		this.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
										    oldD.height + sp.getHorizontalScrollBar().getHeight()));
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		oldD = this.getPreferredSize();
		final Dimension newD = GUIUtils.getPreferredSize(this);
		if (!oldD.equals(newD)) 
			this.setPreferredSize(newD);
		this.pack();
		this.setLocationRelativeTo(owner);
	}
	
	//--------------------------------------------------------------------------------
	private void initialize() {
		infoScr.setBorder(null);
		
		infoPane.setEditable(false);
		int b = GUIUtils.GUI_unit(0.5);
		infoPane.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
		Utilities.setTextPane(infoPane,Utils.htmlPage("Please define the following settings."));

		okButton.setActionCommand("OK");
		cancelButton.setActionCommand("CANCEL");
		privateBrowseButton.setActionCommand("PRIVATE_BROWSE");
		saveButton.setActionCommand("SAVE");
		loadButton.setActionCommand("LOAD");
		restoreButton.setActionCommand("RESTORE");

		passwordField.addCaretListener(this);
		privateKeyField.addCaretListener(this);
		passphraseField.addCaretListener(this);
		
		GUIUtils.addActionListener(this,okButton,cancelButton,privateBrowseButton,saveButton,loadButton,restoreButton);
	}
	
	//=================================================================================
	// private methods
	
	//--------------------------------------------------------------------------------
	/** Creates and returns a Properties object from the file that stores the configuration. */
	private Properties setSettingsFromFile(File source) {
		File file = source != null ? source : new File(configurationFileName);
		Properties tempProp = null;
		if (file.exists()) {
			ObjectInputStream is = null;
			try {
				is = new ObjectInputStream(new FileInputStream(file));
				tempProp = (Properties) is.readObject();
				if (tempProp.getProperty(PRIVATE_KEY_FILE,"").equals("")) {
					boolean isEncoded = Boolean.parseBoolean(tempProp.getProperty(PASSWORD_ENCODED));
					String pwd = tempProp.getProperty(PASSWORD);
					if (isEncoded)
						tempProp.setProperty(PASSWORD,decode(pwd));
				} else {
					boolean isEncoded = Boolean.parseBoolean(tempProp.getProperty(PASSPHRASE_ENCODED));
					String phrase = tempProp.getProperty(PASSPHRASE);
					if (isEncoded)
						tempProp.setProperty(PASSPHRASE,decode(phrase));
				}
				String ssh_port = tempProp.getProperty(SSH_PORT);
				if (ssh_port == null)
					tempProp.setProperty(SSH_PORT,DEFAULT_SSH_PORT);
			} catch (Exception e) {
				e.printStackTrace(ParameterSweepWizard.getLogStream());
				tempProp = source != null ? null : createDefaultProperties();
			} finally {
				try { is.close(); } catch (IOException e) {}
			}
		} else
			tempProp = createDefaultProperties();
		if (tempProp != null) {
			hostField.setText(tempProp.getProperty(HOSTNAME));
			simulationPortField.setText(tempProp.getProperty(SIMULATION_PORT));
			userField.setText(tempProp.getProperty(USERNAME));
			passwordField.setText(tempProp.getProperty(PASSWORD));
			privateKeyField.setText(tempProp.getProperty(PRIVATE_KEY_FILE));
			passphraseField.setText(tempProp.getProperty(PASSPHRASE));
			sshPortField.setText(tempProp.getProperty(SSH_PORT));
			workspaceField.setText(tempProp.getProperty(WORKSPACE));
		}
		return tempProp;
	}
	
	//--------------------------------------------------------------------------------
	/** Creates and returns a Properties object from the default settings. */
	private Properties createDefaultProperties() {
		Properties p = new Properties();
		p.setProperty(HOSTNAME,DEFAULT_HOST);
		p.setProperty(SIMULATION_PORT,DEFAULT_SIMULATION_PORT);
		p.setProperty(USERNAME,DEFAULT_USER);
		p.setProperty(PASSWORD,DEFAULT_PASSWORD);
		p.setProperty(PRIVATE_KEY_FILE,DEFAULT_PRIVATEKEY);
		p.setProperty(PASSPHRASE,DEFAULT_PASSPHRASE);
		p.setProperty(SSH_PORT,DEFAULT_SSH_PORT);
		p.setProperty(WORKSPACE,DEFAULT_WORKSPACE);
		return p;
	}
	
	//---------------------------------------------------------------------------------
	/** Initializes the key object that is used to encode/decode important informations. */
	private static void initializeKey() {
		try {
			key = SecretKeyFactory.getInstance("DESede").generateSecret(new DESedeKeySpec(salt.getBytes()));
		} catch (Exception e) {
			// never happens
			throw new IllegalStateException();
		}
	}
 	
	//---------------------------------------------------------------------------------
	/** Encodes <code>password</code> and returns the encoded version.
	 * @throws Exception if any problem occurs
	 */
	private static String encode(String password) throws Exception {
		if (key == null)
			throw new IllegalStateException("invalid key");
		try {
			Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE,key,IvParameters);
			byte[] encoded = cipher.doFinal(password.getBytes("UTF-8"));
			return Base64.encode(encoded);
		} catch (InvalidKeyException e) {
			throw new IllegalStateException("invalid key");
		} catch (NoSuchAlgorithmException e) {
			// never happens
			throw new IllegalStateException();
		}
	}
	
	//---------------------------------------------------------------------------------
	/** Decodes <code>encoded_password</code> and returns the decoded version.
	 * @throws Exception if any problem occurs
	 */
	private static String decode(String encoded_password) throws Exception {
		if (key == null)
			throw new IllegalStateException("invalid key");
		try {
			Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE,key,IvParameters);
			byte[] decoded = cipher.doFinal(Base64.decode(encoded_password));
			return new String(decoded,"UTF-8");
		} catch (InvalidKeyException e) {
			throw new IllegalStateException("invalid key");
		} catch (NoSuchAlgorithmException e) {
			// never happens
			throw new IllegalStateException();
		}
	}
	
	//--------------------------------------------------------------------------------
	/** Checks the content of the dialog. It examines the following: <br>
	 * <ul>
	 * <li>non-empty fields</li>
	 * <li>valid port numbers</li>
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
		
		// II. ports ( 1024 - 65535 and 1 - 65535)
		String portStr = simulationPortField.getText().trim();
		int simPortNumber = -1;
		if ("".equals(portStr))
			errors.add("Field 'Port' cannot be empty.");
		else {
			try {
				int portNumber = Integer.parseInt(portStr);
				if (portNumber < 1024 || portNumber > 65535)
					throw new NumberFormatException();
				simPortNumber = portNumber;
			} catch (NumberFormatException e) {
				errors.add("Port number of the simulation server must be an integer between 1024 and 65535.");
			}
		}
		
		String sshPortString = sshPortField.getText().trim();
		int sshPortNumber = -1;
		if ("".equals(sshPortString))
			errors.add("Field 'Remote SSH port' cannot be empty.");
		else {
			try {
				int portNumber = Integer.parseInt(sshPortString);
				if (portNumber < 1 || portNumber > 65535)
					throw new NumberFormatException();
				sshPortNumber = portNumber;
			} catch (NumberFormatException e) {
				errors.add("Remote SSH port number must be an integer between 1 and 65535.");
			}
		}
		if (sshPortNumber != -1 && simPortNumber != -1 && simPortNumber == sshPortNumber)
				errors.add("The port number of the simulation server and the remote SSH port number must be different.");
		
		// III. private key file (if any)
		if (!"".equals(privateKeyField.getText().trim())) {
			File file = new File(privateKeyField.getText().trim());
			if (file == null || !file.exists())
				errors.add("File not found: " + privateKeyField.getText().trim());
		}
		return (errors.size() == 0 ? null : errors.toArray(new String[0]));	
	}
	
	//--------------------------------------------------------------------------------
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
			workspace = workspace.substring(0,workspace.length() - 1);
		SftpClient client = null;
		if (privateKeyFile == null || "".equals(privateKeyFile)) {
			passwd = new String(passwordField.getPassword());
			client = new SftpClient(host,sshPort,user,passwd,workspace);
 		} else {
 			passphrase = new String(passphraseField.getPassword());
 			client = new SftpClient(host,sshPort,user,privateKeyFile,passphrase,workspace);
 		}
		
		String error = client.connect();
		
		if (error != null) {
			Utilities.userAlert(this,error);
			return false;
		}
		
		try {
			client.goToWorkspace();
		} catch (SftpException e) {
			Utilities.userAlert(this,"No such directory on host : " + workspace);
			client.disconnect();
			return false;
		}
		client.disconnect();
		return true;
	}
	
	//--------------------------------------------------------------------------------------
	private boolean save(File dest) {
		Properties newProp = new Properties();
		newProp.setProperty(HOSTNAME,hostField.getText().trim());
		newProp.setProperty(SIMULATION_PORT,simulationPortField.getText().trim());
		newProp.setProperty(USERNAME,userField.getText().trim());
		boolean save = true;
		String pwd = "", phrase = "";
		if (privateKeyField.getText() == null || privateKeyField.getText().trim().equals("")) {
			pwd = new String(passwordField.getPassword());
			String encoded = pwd;
			try {
				encoded = encode(pwd);
			} catch (Exception e1) {
				int result = Utilities.askUser(this,false,"Error","Error during the password encoding.","Do you want to store it without encoding?",
											   "If you choose 'No', you must define the settings next time too.");
				if (result == 0)
					save = false;
			}
			newProp.setProperty(PASSWORD,encoded);
			newProp.setProperty(PASSWORD_ENCODED,String.valueOf(save));
			newProp.setProperty(PRIVATE_KEY_FILE,"");
			newProp.setProperty(PASSPHRASE,"");
			newProp.setProperty(PASSPHRASE_ENCODED,"false");
		} else {
			newProp.setProperty(PASSWORD,"");
			newProp.setProperty(PASSWORD_ENCODED,"false");
			newProp.setProperty(PRIVATE_KEY_FILE,privateKeyField.getText());
			if (passphraseField.getPassword() != null) {
				phrase = new String(passphraseField.getPassword());
				String encoded = phrase;
				try {
					encoded = encode(phrase);
				} catch (Exception e1) {
					int result = Utilities.askUser(this,false,"Error","Error during the passphrase encoding.","Do you want to store it without " +
												   "encoding?","If you chooser 'No', you must define the settings next time too.");
					if (result == 0)
						save = false;
				}
				newProp.setProperty(PASSPHRASE,encoded);
				newProp.setProperty(PASSPHRASE_ENCODED,String.valueOf(save));
			}
		}
		newProp.setProperty(SSH_PORT,sshPortField.getText().trim());
		newProp.setProperty(WORKSPACE,workspaceField.getText().trim());
		if (save) {
			try {
				File newFile = dest != null ? dest : new File(configurationFileName + ".new");
				ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(newFile));
				os.writeObject(newProp);
				os.flush();
				os.close();
				if (dest == null) {
					File file = new File(configurationFileName);
					if (file.exists())
						file.delete();
					newFile.renameTo(file);
				}
			} catch (FileNotFoundException e1) {
				// never happens
				throw new IllegalStateException();
			} catch (IOException e1) {
				e1.printStackTrace(ParameterSweepWizard.getLogStream());
				return false;
			}
			newProp.setProperty(PASSWORD,pwd);
			newProp.setProperty(PASSPHRASE,phrase);
			if (dest == null) properties = newProp;
		}
		return true;
	}
}
