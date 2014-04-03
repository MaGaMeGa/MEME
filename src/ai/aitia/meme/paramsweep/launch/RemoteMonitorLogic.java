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
package ai.aitia.meme.paramsweep.launch;

import java.awt.Frame;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;

import ai.aitia.meme.gui.IPanelManager;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.batch.BatchEvent;
import ai.aitia.meme.paramsweep.batch.intellisweep.IntelliSweepBatchEvent;
import ai.aitia.meme.paramsweep.gui.MonitorConfigurationDialog;
import ai.aitia.meme.paramsweep.gui.MonitorGUI;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.launch.Launcher.MessageScreen;
import ai.aitia.meme.paramsweep.messages.BatchEventMessage;
import ai.aitia.meme.paramsweep.messages.BooleanMessage;
import ai.aitia.meme.paramsweep.messages.FinishedSimulationsMessage;
import ai.aitia.meme.paramsweep.messages.IntelliSweepBatchEventMessage;
import ai.aitia.meme.paramsweep.messages.IsLockedRequest;
import ai.aitia.meme.paramsweep.messages.IsLockedResponse;
import ai.aitia.meme.paramsweep.messages.Message;
import ai.aitia.meme.paramsweep.messages.MessageTypes;
import ai.aitia.meme.paramsweep.messages.NewSimulationMessage;
import ai.aitia.meme.paramsweep.messages.RemoveSimulationRequest;
import ai.aitia.meme.paramsweep.messages.WaitingSimulationsMessage;
import ai.aitia.meme.paramsweep.messages.FinishedSimulationsMessage.FinishedSimulation;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.sftp.SftpClient;
import ai.aitia.meme.paramsweep.utils.ServerConstants;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.Utilities.CancelImportException;
import ai.aitia.meme.paramsweep.utils.Utilities.IEBinary;
import ai.aitia.meme.utils.Utils;

import com.jcraft.jsch.SftpException;

public class RemoteMonitorLogic implements IMonitorListener {
	
	//==================================================================================================================
	// members
	
	public static final long DAY = 24*3600*1000L;
	public static final java.text.DateFormat timeFmt = new java.text.SimpleDateFormat("HH:mm:ss");
	static {
		timeFmt.setTimeZone(new java.util.SimpleTimeZone(0, "noDST"));
	}
	
	private MonitorGUI gui = null;
	private Frame owner = null;
	
	private IEBinary<String[],PlatformType> callback = null;
	
	private Socket simulationSocket = null;
	private ObjectInputStream in = null;
	private ObjectOutputStream out = null;
	private UpdaterThread updater = null;
	private boolean reconnect = false;
	private boolean autoUpdate = false;
	private double lastRun = 1;
	private boolean interrupted = false;
	private SftpClient sftpClient = null;
	private String switchToModel = null;
	
	//==================================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public RemoteMonitorLogic(Frame owner, boolean fromMEME, IPanelManager panelManager, IEBinary<String[],PlatformType> callback) {
		this.owner = owner;
		this.gui = new MonitorGUI(owner,fromMEME,panelManager,false);
		this.gui.addMonitorListener(this);
		this.callback = callback;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void start() {
		gui.setEnabledTabs(true);
		String error = initializeSimulationServerConnection();
		if (error != null) {
			Utilities.userAlert(owner,error);
			gui.setEnabledTabs(false);
			return;
		}
		error = updateModelListRequest();
		if (error != null) {
			Utilities.userAlert(owner,error);
			gui.setEnabledTabs(false);
			return;
		}
		String host = gui.getServerProperties().getProperty(MonitorConfigurationDialog.HOSTNAME);
		String user = gui.getServerProperties().getProperty(MonitorConfigurationDialog.USERNAME);
		String passwd = gui.getServerProperties().getProperty(MonitorConfigurationDialog.PASSWORD);
		String privateKeyFile = gui.getServerProperties().getProperty(MonitorConfigurationDialog.PRIVATE_KEY_FILE);
		String passphrase = gui.getServerProperties().getProperty(MonitorConfigurationDialog.PASSPHRASE);
		int sshPort = Integer.parseInt(gui.getServerProperties().getProperty(MonitorConfigurationDialog.SSH_PORT,"22"));
		String workspace = gui.getServerProperties().getProperty(MonitorConfigurationDialog.WORKSPACE);
		if (privateKeyFile == null || privateKeyFile.trim().equals(""))
			sftpClient = new SftpClient(host,sshPort,user,passwd,workspace);
		else {
			File file = new File(privateKeyFile);
			if (!file.exists()) {
				Utilities.userAlert(owner,"Private key file not found: " + privateKeyFile);
				gui.setEnabledTabs(false);
				return;
			}
			sftpClient = new SftpClient(host,sshPort,user,privateKeyFile,passphrase,workspace);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setServerProperties(Properties serverProperties) { gui.setServerProperties(serverProperties); }
	public MonitorGUI getGUI() { return gui; }
	
	//----------------------------------------------------------------------------------------------------
	public void dispose() {
		closeSimulationServerConnection();
		if (sftpClient != null) {
			sftpClient.disconnect();
			sftpClient = null;
		}
		gui.dispose();
	}
	
	//==================================================================================================================
	// implemented interfaces

	//------------------------------------------------------------------------------------------------------------------
	public void downloadSimulationResultSignal(MonitorEvent event) {
		String error = sftpClient.connect();
		if (error != null) 
			Utilities.userAlert(owner,error);
		boolean downloadEnabled = error == null;
	
		if (!downloadEnabled) {
			Utilities.userAlert(owner,"The download service is unavailable because of an error of the SFTP-server.");
			return;
		}
		
		JFileChooser chooser = new JFileChooser(ParameterSweepWizard.getLastDir());
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int result = chooser.showSaveDialog(owner);
		if (result == JFileChooser.APPROVE_OPTION) {
			ParameterSweepWizard.setLastDir(chooser.getSelectedFile());
			final String destDir = chooser.getSelectedFile().getAbsolutePath();
			chooser.setVisible(false);
			gui.repaint();
			final ModelElement me = event.getElement(); 
			final List<String> selected = event.getOutputs();
			Thread downloader = new Thread(new Runnable() {
				public void run() {
					String lockFileName = ServerConstants.LOCK_FILE_NAME_PREFIX + "__" + Util.getTimeStamp();
					String dir = "results/" + me.getDirectory() + "/";
					try {
						sftpClient.goToWorkspace();
						File f = createLockFile(lockFileName);
						sftpClient.upload(null,f.getPath(),"results/" + me.getDirectory());
						f.delete();
						for (String file : selected) {
							boolean notAborted = sftpClient.download(owner,dir + file,destDir);
							if (!notAborted) {
								try {
									sftpClient.removeFile(dir + lockFileName);
								} catch (SftpException e) {
									e.printStackTrace(ParameterSweepWizard.getLogStream());
								}
								Utilities.userAlert(owner,"The downloading is interrupted by the user.");
								sftpClient.disconnect();
								return;
							}
						}
					} catch (FileNotFoundException e1) {
						// never happens
						try {
							sftpClient.removeFile(dir + lockFileName);
						} catch (SftpException e) {
							e.printStackTrace(ParameterSweepWizard.getLogStream());
						}
						throw new IllegalStateException(e1);
					} catch (SftpException e1) {
						try {
							sftpClient.removeFile(dir + lockFileName);
						} catch (SftpException e) {
							e.printStackTrace(ParameterSweepWizard.getLogStream());
						}
						Utilities.userAlert(owner,"Error while transfering data from the server.",Util.getLocalizedMessage(e1));
						sftpClient.disconnect();
						return;
					}
					try {
						sftpClient.removeFile(dir + lockFileName);
					} catch (SftpException e) {
						e.printStackTrace(ParameterSweepWizard.getLogStream());
					}
					sftpClient.disconnect();
					Utilities.userAlert(owner,"Downloading is done.");
				}
			});
			downloader.setName("MEME-Downloader-Thread");
			downloader.start();
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	public void importSimulationResultSignal(MonitorEvent event) {
		String error = sftpClient.connect();
		if (error != null) 
			Utilities.userAlert(owner,error);
		boolean importEnabled = error == null;
	
		if (!importEnabled) {
			Utilities.userAlert(owner,"The import service is unavailable because of an error of the SFTP-server.");
			return;
		}
		final String destDir = "tempResultFiles_" + System.currentTimeMillis();
		final File dir = new File(destDir);
		if (!dir.exists())
			dir.mkdir();
		String lockFileName = ServerConstants.LOCK_FILE_NAME_PREFIX + "__" + Util.getTimeStamp();
		final ModelElement me = event.getElement();
		final List<String> selected = event.getOutputs();
		String remoteDir = "results/" + me.getDirectory() + "/";
		try {
			sftpClient.goToWorkspace();
			File lockFile = createLockFile(lockFileName);
			sftpClient.upload(null,lockFile.getPath(),"results/" + me.getDirectory());
			lockFile.delete();
			for (String file : selected) {
				if (ServerConstants.PROACTIVE_OUT_FILE_NAME.equals(file) ||
					ServerConstants.PROACTIVE_ERROR_FILE_NAME.equals(file))
					continue;
				boolean notAborted = sftpClient.download(owner,remoteDir + file,destDir);
				if (!notAborted) {
					try {
						sftpClient.removeFile(remoteDir + lockFileName);
					} catch (SftpException e1) {
						e1.printStackTrace(ParameterSweepWizard.getLogStream());
					}
					Utilities.userAlert(owner,"The importing is interrupted by the user.");
					sftpClient.disconnect();
					File[] fs = dir.listFiles();
					for (File f : fs)
						f.delete();
					dir.delete();
					return;
				}
			}
			String generatedModelName = me.getDirectory().substring(me.getDirectory().lastIndexOf('.') + 1);
			String source = remoteDir + generatedModelName + ".settings.xml";
			try {
				sftpClient.download(owner,source,destDir);
			} catch (FileNotFoundException e2) {
				if (!e2.getMessage().equals("Cannot find file: " + source))
					throw e2;
			}
		} catch (FileNotFoundException e1) {
			try {
				sftpClient.removeFile(remoteDir + lockFileName);
			} catch (SftpException e2) {
				e2.printStackTrace(ParameterSweepWizard.getLogStream());
			}
			// never happens
			throw new IllegalStateException();
		} catch (SftpException e1) {
			try {
				sftpClient.removeFile(remoteDir + lockFileName);
			} catch (SftpException e2) {
				e2.printStackTrace(ParameterSweepWizard.getLogStream());
			}
			Utilities.userAlert(owner,"Error while transfering data from the server.",Util.getLocalizedMessage(e1));
			sftpClient.disconnect();
			File[] fs = dir.listFiles();
			for (File f : fs)
				f.delete();
			dir.delete();
			return;
		}
		
		try {
			sftpClient.removeFile(remoteDir + lockFileName);
		} catch (SftpException e1) {
			e1.printStackTrace(ParameterSweepWizard.getLogStream());
		}
		sftpClient.disconnect();
		File[] fs = dir.listFiles();
		if (fs.length == 0) {
			String msg_end = selected.size() == 1 ? "file is log file, not result." : "files are log files, not results.";
			Utilities.userAlert(gui,"The selected " + msg_end);
			dir.delete();
			return;
		}
		String[] results = new String[fs.length];
		for (int i = 0;i < fs.length;results[i] = fs[i++].getAbsolutePath());
		try {
			callback.run(results,me.getPlatform());
		} catch (CancelImportException e) {
		} catch (Exception e) {
			ParameterSweepWizard.logError("Error at automatic importing");
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			Utilities.userAlert(owner,"Error at automatic importing.",Util.getLocalizedMessage(e));
		}
		for (File f : fs)
			f.delete();
		dir.delete();
		gui.getRootPane().getGlassPane().setCursor(java.awt.Cursor.getDefaultCursor());
		Utilities.userAlert(owner,"Importing is done.");
	}

	//------------------------------------------------------------------------------------------------------------------
	public void stopCurrentRunSignal(MonitorEvent event) {}
	public void killSimulationSignal(MonitorEvent event) { sendStopMessage(true); }
	public void stopSimulationSignal(MonitorEvent event) { sendStopMessage(false); }
	public void updateSimulationListSignal(MonitorEvent event) { updateModelListRequest(); }

	//------------------------------------------------------------------------------------------------------------------
	public void reconnectSignal(MonitorEvent event) {
		closeSimulationServerConnection();
		if (sftpClient != null) {
			sftpClient.disconnect();
			sftpClient = null;
		}
		reconnect = true;
		start();
	}

	//------------------------------------------------------------------------------------------------------------------
	public void removeSimulationSignal(MonitorEvent event) {
		MessageScreen screen = new MessageScreen(owner,"Removing simulation results...");
		try {
			gui.setEnabledRemoveSimulationButton(false);
			gui.update(gui.getGraphics());
			screen.showScreen();

			List<ModelElement> deletables = event.getElements();
			List<String> unsuccessfuls = new ArrayList<String>();
			for (final ModelElement me : deletables) {
				String err[] = new String[1];
				boolean isLocked = isModelLocked(me.getDirectory(),err);
				if (err[0] != null) {
					Utilities.userAlert(owner,err[0]);
					continue;
				}
				if (isLocked) {
					String[] mNT = Utilities.getModelNameAndTimeStamp(me.getModelName());
					int res = Utilities.askUser(gui,false,"Warning",mNT[0] + "(" + mNT[1] + ") is locked because an other monitor is" +
												" downloading its results","Do you want to continue?");
					isLocked = res != 1;
				}
				
				if (!isLocked) {
					if (!removeSimulation(me.getDirectory()))
						unsuccessfuls.add(Util.restoreNameAndTimeStamp(me.getModelName()));
				}
				
				
			}
			if (unsuccessfuls.size() > 0)
				Utilities.userAlert(owner,"The following simulations cannot be removed: ",Utils.join(unsuccessfuls,"\n"));
			gui.setEnabledRemoveSimulationButtonIfNeed();
			updateModelListRequest(); 
		} finally {
			screen.hideScreen();
		}
	}
	
	//==================================================================================================================
	// assistant methods
	
	//------------------------------------------------------------------------------------------------------------------
	private File createLockFile(String name) {
		try {
			File f = new File(name);
			PrintWriter w = new PrintWriter(f);
			w.println("Yeah come on. That's right. Business.");
			w.flush();
			w.close();
			return f;
		} catch (FileNotFoundException e) {
			// never happens
			throw new IllegalStateException(e);
		}
	}
	
	//------------------------------------------------------------------------------------------------------------------
	private void sendStopMessage(boolean kill) {
		if (simulationSocket != null && !simulationSocket.isClosed()) {
			gui.setEnabledInterruptButton(false);
			gui.setEnabledHardStopButton(false);
			try {
				synchronized (out) {
					out.writeObject(new BooleanMessage(MessageTypes.MSG_CLIENT_STOP_SIMULATION,kill));
					out.flush();
				}
			} catch (IOException e) {
				String suffix = "top request is failed.";
				Utilities.userAlert(owner,(kill ? "Immediate s" : "S") + suffix,e.getLocalizedMessage());
				gui.setEnabledInterruptButton(true);
				gui.setEnabledHardStopButton(true);
			}
		} 
	}
	
	//--------------------------------------------------------------------------------
	/** Returns whether the model directory specified by the first parameter is locked or not.
	 * @param dirName the directory name
	 * @param error output parameter error[0] is the error message (or null if there is no error)
	 */
	private Boolean isModelLocked(String dirName, String[] error) {
		String host = gui.getServerProperties().getProperty(MonitorConfigurationDialog.HOSTNAME);
		int port = Integer.parseInt(gui.getServerProperties().getProperty(MonitorConfigurationDialog.SIMULATION_PORT));
		error[0] = null;
		try {
			Socket socket = new Socket(host,port);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			out.writeObject(new IsLockedRequest(dirName));
			out.flush();
			
			try {
				IsLockedResponse response = (IsLockedResponse) in.readObject();
				return response.getLogicalValue();
			} catch (ClassCastException e) {
				error[0] = "Invalid answer to the following question: Is the directory " + dirName + " locked?";
				return null;
			} catch (ClassNotFoundException e) {
				// never happens
				throw new IllegalStateException(e);
			} finally {
				out.close();
				in.close();
				socket.close();
			}
		} catch (UnknownHostException e) {
			error[0] = "Unknown host.";
		} catch (IOException e) {
			error[0] = "Error during the connection to the download server: " + Util.getLocalizedMessage(e);
		}
		return null;
	} 
	
	//--------------------------------------------------------------------------------
	/** Returns whether the model directory specified by the first parameter is locked or not.
	 * @param dirName the directory name
	 * @param error output parameter error[0] is the error message (or null if there is no error)
	 */
	private boolean removeSimulation(String dirName) {
		String host = gui.getServerProperties().getProperty(MonitorConfigurationDialog.HOSTNAME);
		int port = Integer.parseInt(gui.getServerProperties().getProperty(MonitorConfigurationDialog.SIMULATION_PORT));
		try {
			Socket socket = new Socket(host,port);
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			out.writeObject(new RemoveSimulationRequest(dirName));
			out.flush();
			
			try {
				Message response = (Message) in.readObject();
				return response.getMessageType() == MessageTypes.MSG_SERVER_OK;
			} catch (ClassNotFoundException e) {
				// never happens
				throw new IllegalStateException(e);
			} finally {
				out.close();
				in.close();
				socket.close();
			}
		} catch (UnknownHostException e) {
		} catch (IOException e) {}
		return false;
	} 
	
	//----------------------------------------------------------------------------------------------------
	/** Initializes the socket connection to the simulation server.
	 * @return error message (or null if there is no error)
	 */
	private String initializeSimulationServerConnection() {
		String host = gui.getServerProperties().getProperty(MonitorConfigurationDialog.HOSTNAME);
		int port = Integer.parseInt(gui.getServerProperties().getProperty(MonitorConfigurationDialog.SIMULATION_PORT));
		String error = null;
		try {
			simulationSocket = new Socket(host,port);
			out = new ObjectOutputStream(simulationSocket.getOutputStream());
			in = new ObjectInputStream(simulationSocket.getInputStream());
			out.writeObject(new Message(MessageTypes.MSG_CLIENT_MONITOR));
			out.flush();
			updater = new UpdaterThread();
			updater.start();
			(new KeepAliveThread()).start();
		} catch (UnknownHostException e) {
			error = "Unknown host";
		} catch (IOException e) {
			error = "Error during the connection to the simulation server: " + Util.getLocalizedMessage(e);
		}
		return error;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void closeSimulationServerConnection() {
		if (updater != null) {
			updater.stopUpdate();
			updater = null;
		}
		if (simulationSocket != null) {
			try {
				if (out != null)
					out.close();
				if (in != null)
					in.close();
				simulationSocket.close();
			} catch (IOException e) {
			} finally {
				simulationSocket = null; // this stops the KeepAlive thread too
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private String updateModelListRequest() {
		if (simulationSocket != null && !simulationSocket.isClosed()) {
			try {
				synchronized (out) {
					out.writeObject(new Message(MessageTypes.MSG_CLIENT_MODEL_LIST));
					out.flush();
				}
			} catch (IOException e) {
				return "Error during the connection to the server: " + Util.getLocalizedMessage(e);
			}
			return null;
		}
		return "Connection lost.";
	}
	
	//==================================================================================================================
	// nested classes
	
	//--------------------------------------------------------------------------------
	/** This thread sends an "I'm alive!" message to the simulation server from
	 *  time to time.
	 */
	private class KeepAliveThread extends Thread {
		
		{
			setName("MEME-KeepAlive-Thread");
		}
		
		//==================================================================================================================
		// methods
		
		//------------------------------------------------------------------------------------------------------------------
		@Override
		public void run() {
			try {
				while (simulationSocket != null && !simulationSocket.isClosed()) {
					synchronized (this) {
						try {
							this.wait(3000);
						} catch (InterruptedException e) {}
					}
					synchronized (out) {
						out.writeObject(new Message(MessageTypes.MSG_ALIVE));
						out.flush();
					}
					Thread.yield();
				}
			} catch (IOException e) {}
		}
	}
	
	//--------------------------------------------------------------------------------
	private class UpdaterThread extends Thread {
		
		{
			setName("MEME-Updater-Thread");
		}
		
		//==================================================================================================================
		// members
		
		private boolean loop = true;
		
		//==================================================================================================================
		// methods
		
		// ------------------------------------------------------------------------------------------------------------------
		@Override
		public void run() {
			try {
				while (loop) {
					Message msg = (Message) in.readObject();
					switch (msg.getMessageType()) {
					case MessageTypes.MSG_SERVER_BATCH_EVENT 	: updateProgress(msg);
															   	  break;
					case MessageTypes.MSG_SERVER_BATCH_END   	: endSimulation();
															      break;
					case MessageTypes.MSG_SERVER_STOP_SIM_EVENT	: 
					case MessageTypes.MSG_SERVER_KILL_SIM_EVENT : interrupted = true;
																  break;
					case MessageTypes.MSG_SERVER_MODELS			: updateFinishedSimulations(msg);
																  break;
					case MessageTypes.MSG_SERVER_NEW_SIM		: updateSimulationInformation(msg);
																  break;
					case MessageTypes.MSG_SERVER_NO_SIMULATION	: resetSimulationInformation();
																  break;
					case MessageTypes.MSG_SERVER_WAITING_SIMS	: updateWaitingSimulations(msg);
																  break;
					}
					Thread.yield();
				}
			} catch (IOException e) {
				if (reconnect)
					reconnect = false;
				else if (!loop);
				else {
					Utilities.userAlert(owner,"Error: connection lost.");
					gui.notifyForHide();
				}
			} catch (ClassNotFoundException e) {
				// never happens
				throw new IllegalStateException(e);
			}
		}
		
		// ------------------------------------------------------------------------------------------------------------------
		/** Stops the message receiving from the simulation server. */
		public void stopUpdate() { loop = false; }
		
		//==================================================================================================================
		// assistant methods
		
		//----------------------------------------------------------------------------------------------------
		private void updateWaitingSimulations(Message response) {
			try {
				WaitingSimulationsMessage _response = (WaitingSimulationsMessage) response;
				DefaultListModel waitingModel = new DefaultListModel();
				for (String model : _response.getWaitingSimulations())
					waitingModel.addElement(Util.restoreNameAndTimeStamp(model));
				gui.setWaitingSimulationsListModel(waitingModel);
			} catch (ClassCastException e) {
				// never happens
				throw new IllegalStateException(e);
			}
		}
		
		//----------------------------------------------------------------------------------------------------
		private void updateFinishedSimulations(Message response) {
			try {
				FinishedSimulationsMessage _response = (FinishedSimulationsMessage) response;
				List<FinishedSimulation> finishedSimulations = _response.getFinishedSimulations();
				List<ModelElement> list = new ArrayList<ModelElement>(finishedSimulations.size());
				for (FinishedSimulation fs : finishedSimulations) 
					list.add(new ModelElement(fs.getModelName(),fs.getDescription(),fs.getModelDirectory(),fs.isWrong(),fs.getOutputFiles(),
											  PlatformManager.platformTypeFromString(fs.getPlatform())));
				gui.setFinishedSimulations(list);
				gui.updateColumnWidths();
				if (switchToModel != null) {
					gui.switchToResults(switchToModel);
					switchToModel = null;
				}
			} catch (ClassCastException e) {
				// never happens
				throw new IllegalStateException(e);
			}
		}
		
		//----------------------------------------------------------------------------------------------------
		private void updateSimulationInformation(Message response) {
			try {
				NewSimulationMessage _response = (NewSimulationMessage) response;
				interrupted = false;
				autoUpdate = true;
				gui.setSimulationName(Util.restoreNameAndTimeStamp(_response.getModelName()));
				String _details = "";
				if (_response.getDescription() != null && _response.getDescription().length() > 0) { 
					_details = _response.getDescription();
					if (_response.getResultFiles() != null && _response.getResultFiles().size() > 0)
						_details += "\n";
				}
				if (_response.getResultFiles() != null && _response.getResultFiles().size() > 0)
					_details += "Result file(s):\n" + Utils.join(_response.getResultFiles(),", ");
				gui.setDetails(_details);
				gui.setProgress("Deploying model");
				gui.setTime("Estimated time left: Calculating...");
				gui.setEnabledInterruptButton(true);
				gui.setEnabledHardStopButton(true);
			} catch (ClassCastException e) {
				// never happens
				throw new IllegalStateException(e);
			}
		}
		
		//----------------------------------------------------------------------------------------------------
		private void resetSimulationInformation() {
			gui.setSimulationName("There is no currently running simulation!");
			gui.setDetails("");
			gui.setProgress("");
			gui.setTime("");
			gui.setEnabledInterruptButton(false);
			gui.setEnabledHardStopButton(false);
		}

		//----------------------------------------------------------------------------------------------------
		@SuppressWarnings("cast")
		private void updateProgress(Message response) {
			try {
				BatchEventMessage _response = (BatchEventMessage) response;
				String progressStr = PlatformSettings.getGUIControllerForPlatform().getProgressInfo(convertMessageToEvent(_response),false,lastRun,
						   																		    (double)_response.getMaxRun()); 
				if (_response.getLastHost().length() > 0) {
					String space = "\n";
					if (progressStr.endsWith("\n"))
						space = "";
					progressStr += space + "Last sender host: " + _response.getLastHost() + "";
				}
				gui.setProgress(progressStr);
				lastRun = _response.getRun() + 1;
				setEstimatedTime(_response);
			} catch (ClassCastException e) {
				// never happens
				throw new IllegalStateException(e);
			}
		}
		
		//----------------------------------------------------------------------------------------------------
		private BatchEvent convertMessageToEvent(BatchEventMessage msg) {
			if (msg instanceof IntelliSweepBatchEventMessage) {
				IntelliSweepBatchEventMessage _msg = (IntelliSweepBatchEventMessage) msg;
				return new IntelliSweepBatchEvent(this,BatchEvent.EventType.RUN_ENDED,_msg.getRun(),_msg.getIteration(),"");
			}
			return new BatchEvent(this,BatchEvent.EventType.RUN_ENDED,msg.getRun());
		}
		
		//----------------------------------------------------------------------------------------------------
		private void setEstimatedTime(BatchEventMessage msg) {
			if (msg.getRun() == 0) return;
			String leftStr = "";
			long left = (msg.getCurrentTime() - msg.getStartTime())/msg.getRun() * (msg.getMaxRun() - msg.getRun());
			if (left < DAY)
				leftStr = timeFmt.format(left);
			else
				leftStr = String.format("%d day(s)", Math.round(left/(double)DAY));
			if (!leftStr.equals("")) {
				String newTime = "Estimated time left";
				if (msg instanceof IntelliSweepBatchEventMessage) 
					newTime += " in the current iteration";
				newTime += ": " + leftStr;
				gui.setTime(newTime);
			}
		}
		
		//----------------------------------------------------------------------------------------------------
		private void endSimulation() {
			if (interrupted) 
				gui.setProgress("Batch interrupted.");
			else
				gui.setProgress("Batch done.");
			gui.setTime("Estimated time left: 00:00:00");
			gui.setEnabledInterruptButton(false);
			gui.setEnabledHardStopButton(false);
			if (autoUpdate) {
				autoUpdate = false;
				switchToModel = gui.getSimulationName();
				RemoteMonitorLogic.this.updateModelListRequest();
			}
		}
	}
}
