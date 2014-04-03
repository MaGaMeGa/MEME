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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.gui.IPanelManager;
import ai.aitia.meme.paramsweep.cloud.util.IFileTranserService;
import ai.aitia.meme.paramsweep.cloud.util.SFTPFileTransferService;
import ai.aitia.meme.paramsweep.cloud.util.IFileTranserService.ExperimentDescriptor;
import ai.aitia.meme.paramsweep.cloud.util.IFileTranserService.OperationFailedException;
import ai.aitia.meme.paramsweep.generator.WizardSettingsManager;
import ai.aitia.meme.paramsweep.gui.MonitorGUI.TableSorter;
import ai.aitia.meme.paramsweep.launch.Launcher.MessageScreen;
import ai.aitia.meme.paramsweep.messages.ErrorMessage;
import ai.aitia.meme.paramsweep.messages.Message;
import ai.aitia.meme.paramsweep.messages.MessageTypes;
import ai.aitia.meme.paramsweep.messages.cloud.ExperimentDownloadDoneMessage;
import ai.aitia.meme.paramsweep.messages.cloud.ExperimentDownloadRequest;
import ai.aitia.meme.paramsweep.messages.cloud.ExperimentsListRequest;
import ai.aitia.meme.paramsweep.messages.cloud.FinishedExperimentsMessage;
import ai.aitia.meme.paramsweep.messages.cloud.TransferDataMessage;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.Utilities.CancelImportException;
import ai.aitia.meme.paramsweep.utils.Utilities.IEBinary;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

public class VCloudDownloader extends JPanel implements ActionListener,
														ListSelectionListener,
														MouseListener {

	//=================================================================================
	// members

	private static final long serialVersionUID = 1L;
	
	private Frame owner = null;
	private Properties serverProperties = null;
	private IPanelManager manager = null;
	private IEBinary<String[],PlatformType> callback = null;
	private String hostname = null;
	private int port;
	
	private ExperimentTableModel tmodel = new ExperimentTableModel();
	private TableSorter sorter = new TableSorter(tmodel);

	//=================================================================================
	// GUI-members
	
	private JPanel top = null;
	private JTextField userField = new JTextField();
	private JPasswordField passwordField = new JPasswordField();
	private JButton switchUserButton = new JButton("Switch user");
	private JPanel left = null;
	private JPanel right = null;
	private JTable modelTable = new JTable(sorter) {
		private static final long serialVersionUID = 1L;
		{
			tableHeader.setReorderingAllowed(false);
		}
	};
	private JScrollPane scrModelTable = new JScrollPane(modelTable);
	private JButton updateButton = new JButton("Update list");
	private JList modelOutputList = new JList();
	private JScrollPane scrModelOutputList = new JScrollPane(modelOutputList,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
															 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	private JButton downloadButton = new JButton("Download");
	private JButton importButton = new JButton("Import to database");
	private JPanel bottomPanel = new JPanel(new BorderLayout());
	private JButton configureButton = new JButton("Configure connection...");
	private JButton closeButton = new JButton("Close");
	
	//=================================================================================
	// methods
	
	//--------------------------------------------------------------------------------
	public VCloudDownloader(final Frame owner, final IPanelManager manager, final IEBinary<String[],PlatformType> callback) {
		this.owner = owner;
		this.manager = manager;
		this.callback = callback;
		layoutGUI();
		initialize();
		initializeConnectionInformation();
	}
	
	//----------------------------------------------------------------------------------------------------
	public void start() {
		firstUpdate();
	}
	
	//--------------------------------------------------------------------------------
	public void notifyForHide() {
		dispose();
		manager.remove(this);
	}
	
	//=================================================================================
	// implemented interfaces
	
	//---------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("CONFIGURE")) { 
			userDefinedConnection(true,this.hostname,this.port);
			updateButton.doClick(0);
		} else if (command.equals("CLOSE")) 
			notifyForHide();
		else if (command.equals("UPDATE"))
			update(); 
		else if (command.equals("DOWNLOAD")) 
			download();
		else if (command.equals("IMPORT")) 
			imp0rt();
		else if (command.equals("SWITCH")) {
			List<String> errors = checkFields();
			if (errors.size() > 0)
				Utilities.userAlert(this,errors);
			else if (update()) 
				left.setBorder(BorderFactory.createTitledBorder(createTitle(userField.getText().trim())));
		} else if (command.equals("USER")) 
			passwordField.grabFocus();
		else if (command.equals("PASSWORD")) 
			switchUserButton.doClick(0);
	}
	
	//--------------------------------------------------------------------------------
	public void valueChanged(ListSelectionEvent e) {
		if (e.getSource().equals(modelTable.getSelectionModel())) {
			if (e.getValueIsAdjusting()) return;
			int index = modelTable.getSelectedRow();
			if (index == -1)
				resetSecondPage();
			else {
				ExperimentDescriptor ed = (ExperimentDescriptor) sorter.getValueAt(modelTable.getSelectedRow(),0);;
				DefaultListModel outputModel = new DefaultListModel();
				for (String s : ed.files)
					outputModel.addElement(s);
				modelOutputList.setModel(outputModel);
				modelOutputList.setSelectionInterval(0,outputModel.getSize() - 1);
				downloadButton.setEnabled(true);
				importButton.setEnabled(true);
			}
		} else if (e.getSource().equals(modelOutputList)) {
			downloadButton.setEnabled(!modelOutputList.isSelectionEmpty());
			importButton.setEnabled(!modelOutputList.isSelectionEmpty());
		}
	}
	
	//---------------------------------------------------------------------------------
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}

	//---------------------------------------------------------------------------------
	public void mousePressed(MouseEvent e) {
		if (!SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 2 && e.getSource() instanceof JList && 
			modelOutputList.getSelectedIndex() != -1) 
			downloadButton.doClick();
	}
	
	//=================================================================================
	// GUI-methods
	
	//---------------------------------------------------------------------------------
	private void layoutGUI() {
		
		top = FormsUtils.build("p ~ p:g ~ p ~ p:g ~ p",
							   "[DialogBorder]01234|",
							   "(Registrated) E-mail address: ",userField,"Password: ",passwordField,switchUserButton).getPanel();
		
		left = FormsUtils.build("p:g ~ p",
				                "00 f:p:g||" +
				                "_1 p|",
				                scrModelTable,
				                updateButton).getPanel();
		
		right = FormsUtils.build("p:g ~ p ~ p",
								 "000 f:p:g||" +
				                 "_12 p|",
				                 scrModelOutputList,
				                 downloadButton,importButton).getPanel();
		
		bottomPanel.add(configureButton,BorderLayout.WEST);
		bottomPanel.add(closeButton,BorderLayout.EAST);
		
		this.setLayout(new BorderLayout());
		
		this.add(top,BorderLayout.NORTH);
		
		final JPanel p = FormsUtils.build("p:g(0.7) ~ p:g(0.3)",
										  "01 f:p:g",
										  left,right).getPanel();
		this.add(p,BorderLayout.CENTER);
		
		final Box tmp = new Box(BoxLayout.Y_AXIS);
		tmp.add(new JSeparator());
		tmp.add(bottomPanel);
		this.add(tmp,BorderLayout.SOUTH);
	}
	
	//---------------------------------------------------------------------------------
	private void initialize() {
		userField.setActionCommand("USER");
		passwordField.setActionCommand("PASSWORD");
		switchUserButton.setActionCommand("SWITCH");

		left.setBorder(BorderFactory.createTitledBorder(createTitle()));
		modelTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		modelTable.getSelectionModel().addListSelectionListener(this);
		updateButton.setActionCommand("UPDATE");
		right.setBorder(BorderFactory.createTitledBorder("Available results of the experiment"));
		modelOutputList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		modelOutputList.addListSelectionListener(this);
		modelOutputList.addMouseListener(this);
		downloadButton.setActionCommand("DOWNLOAD");
		downloadButton.setEnabled(false);
		importButton.setActionCommand("IMPORT");
		importButton.setEnabled(false);
		
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
		configureButton.setActionCommand("CONFIGURE");
		closeButton.setActionCommand("CLOSE");

		scrModelTable.setPreferredSize(new Dimension(300,100));
		modelTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		modelTable.setDefaultRenderer(Date.class,new DateRenderer());
		sorter.setTableHeader(modelTable.getTableHeader());
		sorter.setSortingStatus(1,TableSorter.DESCENDING);

		GUIUtils.addActionListener(this,updateButton,downloadButton,importButton,configureButton,closeButton,userField,passwordField,
								   switchUserButton);
		
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializeConnectionInformation() {
		final File configFile = new File(WizardPreferences.vCloudFileName);
		if (configFile.exists()) {
			FileReader fileReader = null;
			try {
				final Properties tempProp = new Properties();
				fileReader = new FileReader(configFile);
				tempProp.load(fileReader);
				this.hostname = tempProp.getProperty(WizardPreferences.HOSTNAME);
				this.port = Integer.parseInt(tempProp.getProperty(WizardPreferences.PORT));
				if (this.hostname == null || this.hostname.trim().length() == 0) 
					userDefinedConnection(false,null,-1);
			} catch (final Exception e) {
				userDefinedConnection(false,null,-1);
			} finally {
				if (fileReader != null) 
					try { fileReader.close(); } catch (final IOException _) {}
			}
		} else 
			userDefinedConnection(false,null,-1);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void userDefinedConnection(final boolean cancel, final String host, final int port) {
		final VCloudConnectionDialog dlg = new VCloudConnectionDialog(owner,cancel);
		final int result = dlg.showDialog(host,port);
		if (result == VCloudConnectionDialog.OK) {
			this.hostname = dlg.getHostname();
			this.port = dlg.getPort();
		}
		dlg.dispose();
		if (result == VCloudConnectionDialog.ERROR) 
			notifyForHide();
	}
	
	//=================================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private void firstUpdate() {
		final WizardPreferences pref = new WizardPreferences();
		this.serverProperties = pref.getProperties();
		if (serverProperties.getProperty(WizardPreferences.VCLOUD_USERNAME,"").trim().length() > 0 &&
			serverProperties.getProperty(WizardPreferences.VCLOUD_PASSWORD,"").trim().length() > 0) {
			left.setBorder(BorderFactory.createTitledBorder(createTitle()));
			updateButton.doClick(0);
		}
	}
	
	//---------------------------------------------------------------------------------
	public void dispose() { 
		resetSecondPage();
		tmodel.clearData();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String createTitle() {
		if (serverProperties == null || serverProperties.getProperty(WizardPreferences.VCLOUD_USERNAME,"").trim().length() == 0) {
			updateButton.setEnabled(false);
			return "No user selected";
		} else {
			updateButton.setEnabled(true);
			final String name = serverProperties.getProperty(WizardPreferences.VCLOUD_USERNAME);
			return name + "'" + (name.endsWith("s") ? " " : "s ") + "experiments"; 
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private String createTitle(final String user) {
		if (user == null || user.trim().length() == 0) {
			updateButton.setEnabled(false);
			return "No user selected";
		} else {
			updateButton.setEnabled(true);
			return user + "'" + (user.endsWith("s") ? " " : "s ") + "experiments"; 
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private List<String> checkFields() {
		List<String> errors = new ArrayList<String>(); 

		if ("".equals(userField.getText().trim()))
			errors.add("Field '(Registrated) E-mail address' cannot be empty.");
		if (passwordField.getPassword().length == 0)
			errors.add("Field 'Password' cannot be empty.");
		return errors;	
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean update() {
		String username = userField.getText().trim();
		char[] password = passwordField.getPassword();
		if (username == null || username.length() == 0) {
			username = serverProperties.getProperty(WizardPreferences.VCLOUD_USERNAME,"");
			password = serverProperties.getProperty(WizardPreferences.VCLOUD_PASSWORD,"").toCharArray();
		}
		if (username.length() > 0 && password.length > 0) {
			
			final MessageScreen mScreen = new MessageScreen(owner,"Updating experiment list...");
			mScreen.showScreen();
			Socket socket = null;
			ObjectOutputStream out = null;
			ObjectInputStream in = null;
			try {
				socket = new Socket(this.hostname,this.port);
				out = new ObjectOutputStream(socket.getOutputStream());
				in = new ObjectInputStream(socket.getInputStream());
				
				final String encodedPassword = Utilities.md5(new String(password));
				final ExperimentsListRequest request = new ExperimentsListRequest(username,encodedPassword.toCharArray());
				
				out.writeObject(request);
				out.flush();
				
				Message response = (Message) in.readObject();
				
				if (response.getMessageType() == MessageTypes.MSG_SERVER_FAILURE) {
					final ErrorMessage error = (ErrorMessage) response;
					Utilities.userAlert(this,error.getDescription());
				} else if  (response.getMessageType() == MessageTypes.MSG_SERVER_MODELS) {
					final FinishedExperimentsMessage msg = (FinishedExperimentsMessage) response;
					setFinishedSimulations(msg.getFinishedSimulations());
					updateColumnWidths(modelTable,scrModelTable);
					return true;
				}
			} catch (final UnknownHostException e) {
				MEMEApp.logException(e);
				Utilities.userAlert(this,"Host " + this.hostname + " is unknown.");
			} catch (final IOException e) {
				MEMEApp.logException(e);
				Utilities.userAlert(this,"Error during the connection to the Model Exploration Server: " + Util.getLocalizedMessage(e));
			} catch (ClassNotFoundException e) {
				MEMEApp.logException(e);
				throw new IllegalStateException(e);
			}  finally {
				if (socket != null) {
					try { socket.close(); } catch (final IOException _) {}
				}
				mScreen.hideScreen();
			}
			return false;
		} else 
			Utilities.userAlert(this,"E-mail address and/or password is invalid.");
			return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void setFinishedSimulations(final List<ExperimentDescriptor> list) { 
		tmodel.clearData();
		for (final ExperimentDescriptor ed : list)
			tmodel.addModel(ed);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void updateColumnWidths(JTable table, javax.swing.JScrollPane sc) {
		java.awt.FontMetrics fm = table.getFontMetrics(table.getFont());
		java.awt.Graphics g = table.getGraphics();
		javax.swing.table.TableColumnModel cm = table.getColumnModel();
		int gap = GUIUtils.dluX(4,table);
		int totalw = sc.getWidth() - sc.getVerticalScrollBar().getPreferredSize().width	- gap;
		float sum = 0, maxw = 0;
		int n = cm.getColumnCount();		
		ArrayList<Float> widths = new ArrayList<Float>(n);
		for (int i = 0;i < n;++i) {			
			String header = String.valueOf(cm.getColumn(i).getHeaderValue());
			float width;
			if (g != null) 
				width = (float) fm.getStringBounds(header,0,header.length(),g).getWidth();
			else
				width = fm.stringWidth(header);
			float w = width + gap;
			if (w > maxw)
				maxw = w;
			sum += w;
			widths.add(w);
		}
		if (n >= 0 && sum < totalw) {
			float rf = 1.0f;
			if (maxw * n < totalw) 
				maxw = totalw / (float) n;
			else
				rf = (totalw - sum) / (n * maxw - sum);
			for (int i = 0; i < n; ++i) {
				float w = widths.get(i);
				widths.set(i,w + (maxw - w) * rf);
			}
		}
		for (int i = n - 1 ; i >= 0; --i) {
			int computedWidth = Math.round(widths.get(i));
			if (i == n - 1)
				computedWidth = Math.max(computedWidth,250);
			cm.getColumn(i).setPreferredWidth(computedWidth);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void download() {
		String username = userField.getText().trim();
		char[] password = passwordField.getPassword();
		if (username == null || username.length() == 0) {
			username = serverProperties.getProperty(WizardPreferences.VCLOUD_USERNAME,"");
			password = serverProperties.getProperty(WizardPreferences.VCLOUD_PASSWORD,"").toCharArray();
		}
		
		if (username.length() > 0 && password.length > 0) {
			
			final ExperimentDescriptor selectedExperiment = (ExperimentDescriptor) sorter.getValueAt(modelTable.getSelectedRow(),0);
			if (selectedExperiment == null || modelOutputList.getSelectedValues().length == 0) return;
			
			final MessageScreen mScreen = new MessageScreen(owner,"Downloading results...");
			mScreen.showScreen();
			Socket socket = null;
			ObjectOutputStream out = null;
			ObjectInputStream in = null;
			try {
				socket = new Socket(this.hostname,this.port);
				out = new ObjectOutputStream(socket.getOutputStream());
				in = new ObjectInputStream(socket.getInputStream());
				
				final String encodedPassword = Utilities.md5(new String(password));
				final ExperimentDownloadRequest request = new ExperimentDownloadRequest(username,encodedPassword.toCharArray(),
																						selectedExperiment.modelId);
				
				out.writeObject(request);
				out.flush();
				
				Message response = (Message) in.readObject();
				socket.close();
				
				if (response.getMessageType() == MessageTypes.MSG_SERVER_FAILURE) {
					final ErrorMessage error = (ErrorMessage) response;
					Utilities.userAlert(this,error.getDescription());
				} else if  (response.getMessageType() == MessageTypes.MSG_TRANSFER_DATA) {
					
					final JFileChooser chooser = new JFileChooser(MEMEApp.getLastDir());
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					final int result = chooser.showSaveDialog(owner);
					if (result == JFileChooser.APPROVE_OPTION) {
						MEMEApp.setLastDir(chooser.getSelectedFile());
						final String destDir = chooser.getSelectedFile().getAbsolutePath();
						chooser.setVisible(false);
						owner.update(owner.getGraphics());
						mScreen.update(mScreen.getGraphics());
					
						final Object[] selected = modelOutputList.getSelectedValues();
						final String[] selectedFiles = new String[selected.length];
						for (int i = 0;i < selected.length;selectedFiles[i] = selected[i++].toString().trim());
						
						final TransferDataMessage msg = (TransferDataMessage) response;
						
						final String sshHostname = msg.getHostname();
						final int sshPort = msg.getPort();
						final String sshUser = msg.getUserName();
						String sshPassword = new String(msg.getPassword());
						try {
							sshPassword = msg.isPasswordEncoded() ? Utilities.decode(sshPassword) : sshPassword;
						} catch (final Exception e) {
							mScreen.hideScreen();
							Utilities.userAlert(this,"Unable to transfer the results from the Model Exploration Server",
												Util.getLocalizedMessage(e));
							MEMEApp.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
							MEMEApp.logException(e);
							return;
						} 
						String workspace = msg.getWorkspace();
						
						final int idx = workspace.indexOf(":\\");
						if (idx != -1)
							workspace = "/" + workspace.substring(idx + 2);
						
						final IFileTranserService fts = new SFTPFileTransferService(sshHostname,sshUser,sshPassword,sshPort,workspace);

						try {
							final boolean notAborted = fts.downloadFiles(owner,destDir,selectedFiles);
							
							if (!notAborted) { // downloading aborted
								mScreen.hideScreen();
								Utilities.userAlert(this,"The operation is aborted by the user.");
								return;
							} else {
								owner.update(owner.getGraphics());
								mScreen.update(mScreen.getGraphics());
								
								try {
									final ExperimentDownloadDoneMessage message = new ExperimentDownloadDoneMessage(username,
																													encodedPassword.toCharArray(),
																													selectedExperiment.modelId);
									socket = new Socket(this.hostname,this.port);
									out = new ObjectOutputStream(socket.getOutputStream());
									in = new ObjectInputStream(socket.getInputStream());

									out.writeObject(message);
									out.flush();
								} catch (final IOException e) {
									MEMEApp.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
									MEMEApp.logException(e);
								}
								Utilities.userAlert(owner,"Downloading is done.");
							}
						} catch (final OperationFailedException e) {
							mScreen.hideScreen();
							Utilities.userAlert(this,"Error while downloading result(s).",Util.getLocalizedMessage(e));
							MEMEApp.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
							MEMEApp.logException(e);
							return;
						}
					}
				}
			} catch (final UnknownHostException e) {
				MEMEApp.logException(e);
				Utilities.userAlert(this,"Host " + this.hostname + " is unknown.");
			} catch (final IOException e) {
				MEMEApp.logException(e);
				Utilities.userAlert(this,"Error during the connection to the Model Exploration Server: " + Util.getLocalizedMessage(e));
			} catch (ClassNotFoundException e) {
				MEMEApp.logException(e);
				throw new IllegalStateException(e);
			}  finally {
				if (socket != null) {
					try { socket.close(); } catch (final IOException _) {}
				}
				if (mScreen != null)
					mScreen.hideScreen();
			}
		} else 
			Utilities.userAlert(this,"E-mail address and/or password is invalid.");
	}
	
	//----------------------------------------------------------------------------------------------------
	private void imp0rt() {
		String username = userField.getText().trim();
		char[] password = passwordField.getPassword();
		if (username == null || username.length() == 0) {
			username = serverProperties.getProperty(WizardPreferences.VCLOUD_USERNAME,"");
			password = serverProperties.getProperty(WizardPreferences.VCLOUD_PASSWORD,"").toCharArray();
		}
		
		if (username.length() > 0 && password.length > 0) {
			
			final ExperimentDescriptor selectedExperiment = (ExperimentDescriptor) sorter.getValueAt(modelTable.getSelectedRow(),0);
			if (selectedExperiment == null || modelOutputList.getSelectedValues().length == 0) return;
			
			if (modelOutputList.getSelectedValues().length == 1 &&
				modelOutputList.getSelectedValue().toString().equals(selectedExperiment.modelId + ".settings.xml")) {
				Utilities.userAlert(this,"Please, select a result file to import to the database.");
				return;
			}
			
			final MessageScreen mScreen = new MessageScreen(owner,"Importing results...");
			mScreen.showScreen();
			Socket socket = null;
			ObjectOutputStream out = null;
			ObjectInputStream in = null;
			try {
				socket = new Socket(this.hostname,this.port);
				out = new ObjectOutputStream(socket.getOutputStream());
				in = new ObjectInputStream(socket.getInputStream());
				
				final String encodedPassword = Utilities.md5(new String(password));
				final ExperimentDownloadRequest request = new ExperimentDownloadRequest(username,encodedPassword.toCharArray(),
																						selectedExperiment.modelId);
				
				out.writeObject(request);
				out.flush();
				
				Message response = (Message) in.readObject();
				socket.close();
				
				if (response.getMessageType() == MessageTypes.MSG_SERVER_FAILURE) {
					final ErrorMessage error = (ErrorMessage) response;
					Utilities.userAlert(this,error.getDescription());
				} else if  (response.getMessageType() == MessageTypes.MSG_TRANSFER_DATA) {
					
					final String destDir = "tempResultFiles_" + System.currentTimeMillis();
					final File dir = new File(destDir);
					if (!dir.exists())
						dir.mkdir();
					
					final Object[] selected = modelOutputList.getSelectedValues();
					final DefaultListModel olm = (DefaultListModel) modelOutputList.getModel();
					boolean xmlSelected = modelOutputList.isSelectedIndex(olm.indexOf(selectedExperiment.modelId + ".settings.xml"));
					
					
					final String[] selectedFiles = new String[xmlSelected ? selected.length : selected.length + 1];
					for (int i = 0;i < selected.length;selectedFiles[i] = selected[i++].toString().trim());
					if (!xmlSelected)
						selectedFiles[selectedFiles.length - 1] = selectedExperiment.modelId + ".settings.xml";
					
					final TransferDataMessage msg = (TransferDataMessage) response;
					
					final String sshHostname = msg.getHostname();
					final int sshPort = msg.getPort();
					final String sshUser = msg.getUserName();
					String sshPassword = new String(msg.getPassword());
					try {
						sshPassword = msg.isPasswordEncoded() ? Utilities.decode(sshPassword) : sshPassword;
					} catch (final Exception e) {
						mScreen.hideScreen();
						Utilities.userAlert(this,"Unable to transfer the results from the Model Exploration Server",
											Util.getLocalizedMessage(e));
						MEMEApp.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
						MEMEApp.logException(e);
						dir.delete();
						return;
					} 
					String workspace = msg.getWorkspace();
					
					final int idx = workspace.indexOf(":\\");
					if (idx != -1)
						workspace = "/" + workspace.substring(idx + 2);
					
					final IFileTranserService fts = new SFTPFileTransferService(sshHostname,sshUser,sshPassword,sshPort,workspace);

					try {
						final boolean notAborted = fts.downloadFiles(owner,destDir,selectedFiles);
						
						if (!notAborted) { // downloading aborted
							final File[] fs = dir.listFiles();
							for (final File f : fs)
								f.delete();
							dir.delete();
							mScreen.hideScreen();
							Utilities.userAlert(this,"The operation is aborted by the user.");
							return;
						} else {
							owner.update(owner.getGraphics());
							mScreen.update(mScreen.getGraphics());
							
							try {
								final ExperimentDownloadDoneMessage message = new ExperimentDownloadDoneMessage(username,
																												encodedPassword.toCharArray(),
																												selectedExperiment.modelId);
								socket = new Socket(this.hostname,this.port);
								out = new ObjectOutputStream(socket.getOutputStream());
								in = new ObjectInputStream(socket.getInputStream());

								out.writeObject(message);
								out.flush();
							} catch (final IOException e) {
								MEMEApp.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
								MEMEApp.logException(e);
							}
							
							boolean canceled = false;
							File[] fs = dir.listFiles();
							String[] results = new String[fs.length];
							for (int i = 0;i < fs.length;i++) {
								if (fs[i].getName().endsWith(".zip")) {
									Util.unzip(fs[i],dir.getName() + File.separator);
									results[i] = new File(dir,fs[i].getName().substring(0,fs[i].getName().length() - 4)).getAbsolutePath();
								} else
									results[i] = fs[i].getAbsolutePath();	
							}
							try {
								callback.run(results,getPlatformTypeFromSettingsFile(dir,selectedExperiment.modelId + ".settings.xml"));
							} catch (final CancelImportException _) {
								canceled = true;
							} catch (final Exception e) {
								MEMEApp.logError("Error at automatic importing");
								MEMEApp.logException(e);
								Utilities.userAlert(owner,"Error at automatic importing.",Util.getLocalizedMessage(e));
							}
							fs = dir.listFiles();
							for (final File f : fs)
								f.delete();
							dir.delete();
							Utilities.userAlert(owner,"Importing is " + (canceled ? " canceled." : "done."));
						}
					} catch (final OperationFailedException e) {
						mScreen.hideScreen();
						Utilities.userAlert(this,"Error while downloading result(s).",Util.getLocalizedMessage(e));
						MEMEApp.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
						MEMEApp.logException(e);
						return;
					}
				}
			} catch (final UnknownHostException e) {
				MEMEApp.logException(e);
				Utilities.userAlert(this,"Host " + this.hostname + " is unknown.");
			} catch (final IOException e) {
				MEMEApp.logException(e);
				Utilities.userAlert(this,"Error during the connection to the Model Exploration Server: " + Util.getLocalizedMessage(e));
			} catch (ClassNotFoundException e) {
				MEMEApp.logException(e);
				throw new IllegalStateException(e);
			}  finally {
				if (socket != null) {
					try { socket.close(); } catch (final IOException _) {}
				}
				if (mScreen != null)
					mScreen.hideScreen();
			}
		} else 
			Utilities.userAlert(this,"E-mail address and/or password is invalid.");
	}
	
	//----------------------------------------------------------------------------------------------------
	private PlatformType getPlatformTypeFromSettingsFile(final File dir, final String filename) throws Exception {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder parser = factory.newDocumentBuilder();
		final Document document = parser.parse(new File(dir,filename));
		final Element settings = document.getDocumentElement();
		final String platform = settings.getAttribute(WizardSettingsManager.SIMULATION_PLATFORM);
		return PlatformManager.platformTypeFromString(platform);
	}
	
	//--------------------------------------------------------------------------------
	private void resetSecondPage() {
		modelOutputList.setModel(new DefaultListModel());
		downloadButton.setEnabled(false);
		importButton.setEnabled(false);
	}
	
	//----------------------------------------------------------------------------------------------------
	private static class ExperimentTableModel extends AbstractTableModel implements TableModelListener {

		//====================================================================================================
		// members
		
		private static final long serialVersionUID = 1L;
		
		private String[] columnNames = new String[] { "Name", "Date" };
		private List<Object[]> data = null;
		
		//========================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public ExperimentTableModel() {
			data = new ArrayList<Object[]>();
		}
		
		//------------------------------------------------------------------------
		public void clearData() { 
			data = new ArrayList<Object[]>();
			fireTableChanged(new TableModelEvent(this));
		}
		
		//----------------------------------------------------------------------------------------------------
		@Override public String getColumnName(final int col) { return columnNames[col]; }
		public int getColumnCount() { return columnNames.length; }
		public int getRowCount() { return data.size(); }
		public Object getValueAt(final int rowIndex, final int columnIndex) { return data.get(rowIndex)[columnIndex]; }
		public ExperimentDescriptor getModel(final int rowIndex) { return (ExperimentDescriptor) data.get(rowIndex)[0]; }
		public void addModel(final ExperimentDescriptor element) { data.add(new Object[] { element, element.submitDate }); }
		
		//----------------------------------------------------------------------------------------------------
		@Override
		public Class<?> getColumnClass(final int c) {
	        switch (c) {
	        case 0  : return ExperimentDescriptor.class;
	        case 1  : return Date.class;
	        default : throw new IllegalStateException();
	        }
	    }
		
		//====================================================================================================
		// implemented interfaces

		//----------------------------------------------------------------------------------------------------
		public void tableChanged(final TableModelEvent e) { fireTableChanged(e); }
	}
	
	//----------------------------------------------------------------------------------------------------
	private static class DateRenderer extends JLabel implements TableCellRenderer {

		private static final long serialVersionUID = 1L;
		private static final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.FULL);

		//----------------------------------------------------------------------------------------------------
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if (isSelected) {
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
			} else {
				setForeground(table.getForeground());
				setBackground(table.getBackground());
			}
			setBorder(null);
			if (hasFocus) {
		            Border border = null;
		            if (isSelected) 
		                border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");
		            if (border == null) 
		                border = UIManager.getBorder("Table.focusCellHighlightBorder");
		            setBorder(border);
		    }
			setHorizontalAlignment(JLabel.CENTER);
			setFont(table.getFont());
			if (value == null) 
				setText("");
			else {
				final Date date = (Date) value;
				setText(format.format(date));
			}
			return this;
		}
	}
}
