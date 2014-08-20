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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javassist.ClassPath;
import javassist.ClassPool;
import javassist.NotFoundException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.border.BevelBorder;

import org.jgap.util.JarClassLoader;

import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.gui.IPanelManager;
import ai.aitia.meme.paramsweep.PS_Monitor;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.PS_ModelWizard.RunningMode;
import ai.aitia.meme.paramsweep.batch.BatchException;
import ai.aitia.meme.paramsweep.batch.IBatchController;
import ai.aitia.meme.paramsweep.batch.InvalidEntryPointException;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.cloud.util.IFileTranserService;
import ai.aitia.meme.paramsweep.cloud.util.SFTPFileTransferService;
import ai.aitia.meme.paramsweep.cloud.util.IFileTranserService.OperationFailedException;
import ai.aitia.meme.paramsweep.distr.DefaultJob;
import ai.aitia.meme.paramsweep.distr.DefaultJobConfiguration;
import ai.aitia.meme.paramsweep.distr.IntelliSweepInfo;
import ai.aitia.meme.paramsweep.distr.Job;
import ai.aitia.meme.paramsweep.distr.JobConfiguration;
import ai.aitia.meme.paramsweep.generator.JarGenerator;
import ai.aitia.meme.paramsweep.generator.QCGJobDescriptorGenerator;
import ai.aitia.meme.paramsweep.generator.ZipGenerator;
import ai.aitia.meme.paramsweep.generator.QCGJobDescriptorGenerator.JobDescriptorType;
import ai.aitia.meme.paramsweep.gui.MonitorConfigurationDialog;
import ai.aitia.meme.paramsweep.gui.MonitorGUI;
import ai.aitia.meme.paramsweep.gui.VCloudAuthenticationDialog;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.messages.ErrorMessage;
import ai.aitia.meme.paramsweep.messages.Message;
import ai.aitia.meme.paramsweep.messages.MessageTypes;
import ai.aitia.meme.paramsweep.messages.ScheduleSimulationRequest;
import ai.aitia.meme.paramsweep.messages.SupportedPlatformsMessage;
import ai.aitia.meme.paramsweep.messages.cloud.IdentifiedScheduleSimulationRequest;
import ai.aitia.meme.paramsweep.messages.cloud.TransferDataMessage;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IIntelliDynamicMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IIntelliMethodPlugin;
import ai.aitia.meme.paramsweep.sftp.SftpClient;
import ai.aitia.meme.paramsweep.utils.ClassPathPair;
import ai.aitia.meme.paramsweep.utils.DummyRecorderInfo;
import ai.aitia.meme.paramsweep.utils.SortedListModel;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.Utilities.IEBinary;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.FormsUtils.Separator;
import ai.aitia.meme.utils.Utils.TPair;

import com.jcraft.jsch.SftpException;

public class Launcher {

	//====================================================================================================
	// members
	
	/** This list contains the names of the jar files that aren't necessary to upload
	 *  to the server when the wizard runs the simulations in distributed mode, because
	 *  these files are already on the server host (Repast, ProActive and JRE related jars).
	 */
	private static final List<String> unuploadableCommonJars = new ArrayList<String>();
	private static final List<String> unuploadableProActiveJars = new ArrayList<String>();
	
	private static final String QCG_PACKAGE_DIRECTORY = "QosCosGridPackages";
	
	static {
		fillUnuploadableJarsLists();
	}
	
	private RunningMode runningMode = null;
	private ParameterSweepWizard wizard = null;
	private Window appWindow = null;
	private IPanelManager manager = null;
	private IEBinary<String[],PlatformType> callback = null;
	private Properties monitorServerProperties = null; 
	
	private String[] outputFileNames = null;
	
	@SuppressWarnings("unused")
	private Boolean qcgResult = null;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public Launcher(Window appWindow, ParameterSweepWizard wizard, RunningMode runningMode, IPanelManager manager,
					IEBinary<String[],PlatformType> callback, Properties monitorServerProperties) {
		this.appWindow = appWindow;
		this.wizard = wizard;
		this.runningMode = runningMode;
		this.manager = manager;
		this.callback = callback;
		this.monitorServerProperties = monitorServerProperties;
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean launch() {
		if (!preCheck()) return false;
		if (runningMode == RunningMode.SIMPLE_RUN)
			return localLaunch();
		else if (runningMode == RunningMode.SERVER_RUN)
			return remoteLaunch();
		else if (runningMode == RunningMode.QCG_RUN)
			return qcgPackageCreation();
		else
			return vCloudLaunch();
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private boolean preCheck() {
		if (runningMode == RunningMode.SERVER_RUN || runningMode == RunningMode.QCG_RUN || runningMode == RunningMode.VCLOUD_RUN) {
			boolean needModelGeneration = (wizard.getNewParameters_internal() != null && wizard.getNewParameters_internal().size() > 0) ||
										   wizard.getRecorders().size() > 0;
			if (wizard.getRecorders().size() == 0 && runningMode == RunningMode.QCG_RUN) {
				Utilities.userAlert(wizard,"QosCosGrid doesn't support hand-coded batch simulations.",
										   "You may run your simulation on the local machine or a conventional grid.");
				return false;
			}
			if (!needModelGeneration) {
				RecorderFilesDialog dlg = new RecorderFilesDialog(ParameterSweepWizard.getFrame());
				int result = dlg.showDialog();
				if (result == RecorderFilesDialog.OK_OPTION) 
					outputFileNames = dlg.getOutputFileNames();
				else {
					outputFileNames = null;
					runningMode = RunningMode.SIMPLE_RUN;
				}
				dlg.dispose();
			}
		}

		if (runningMode == RunningMode.SERVER_RUN) {
			Boolean supportedPlatform = isServerSupportedPlatform();
			if (supportedPlatform == null) return false;
			if (!supportedPlatform) {
				String platform = PlatformManager.getPlatform(PlatformSettings.getPlatformType()).getDisplayableName();
				int result = Utilities.askUser(wizard,false,"Simulation platform not supported",platform + " is not supported by the specified server.",
											   "Do you want to run your simulation on the local machine?","Click 'No' to return to the wizard.");
				if (result == 1) {
					runningMode = RunningMode.SIMPLE_RUN;
					return true;
				}
				return false;
			}
		}
		return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	private Boolean isServerSupportedPlatform() {
		MessageScreen mScreen = new MessageScreen(ParameterSweepWizard.getFrame(),"Checking server...","Please wait!");
		mScreen.showScreen();
		String host = ParameterSweepWizard.getPreferences().getHostName();
		int port = ParameterSweepWizard.getPreferences().getPort();
		Socket socket = null;
		try {
			socket = new Socket(host,port);
		} catch (IOException e) {
			mScreen.hideScreen();
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"Unable to connect to the server: " + host + "/" + String.valueOf(port),
								Util.getLocalizedMessage(e),"Please check the network properties.");
			ParameterSweepWizard.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			return null;
		}
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		try {
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			out.writeObject(new Message(MessageTypes.MSG_CLIENT_SUPPORTED_PLATFORMS));
			out.flush();
			Object response = in.readObject();
			SupportedPlatformsMessage spm = (SupportedPlatformsMessage) response;
			final String actualPlatform = PlatformManager.idStringForPlatform(PlatformSettings.getPlatformType());
			final String actualPlatformVersion = PlatformManager.getPlatform(PlatformSettings.getPlatformType()).getVersion();
			return spm.getSupportedPlatforms().contains(new TPair<String,String>(actualPlatform,actualPlatformVersion));
		} catch (IOException e) {
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"Connection error.",Util.getLocalizedMessage(e));
			if (ParameterSweepWizard.isFromMEME())
				ParameterSweepWizard.getFrame().update(ParameterSweepWizard.getFrame().getGraphics());
			ParameterSweepWizard.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			return null;
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		} finally {
			mScreen.hideScreen();
			try { out.close(); } catch (IOException e) {}
			try { in.close(); } catch (IOException e) {}
			try { socket.close(); } catch (IOException e) {}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean localLaunch() {
		wizard.writeClassPathToRegistry();
		extendClassPathWithPlatformSpecificEntries();
		IBatchController controller = null;
		if (wizard.getSweepingMethodID() == 2)  
			controller = new DynamicIntelliSweepBatchController(wizard.getIntelliDynamicMethodPlugin());
		else {
			try {
				controller = PlatformManager.getPlatform(PlatformSettings.getPlatformType()).getBatchController();
			} catch (RuntimeException e) {
				if (e.getCause() instanceof ClassCastException || e.getCause() instanceof ClassNotFoundException ||
					e.getCause() instanceof InstantiationException || e.getCause() instanceof IllegalAccessException) {
					String error = Util.getLocalizedMessage(e.getCause());
					if (ParameterSweepWizard.isFromMEME())
						Utilities.userAlert(ParameterSweepWizard.getFrame(),error);
					else 
						Utilities.userAlert(appWindow,error);
					wizard.writeClassPathToRegistry();
					wizard.writeUsedPlatformToRegistry();
					return false;
				} else if (e.getCause() instanceof SecurityException || e.getCause() instanceof NoSuchMethodException ||
						   e.getCause() instanceof InvocationTargetException)
					throw new IllegalStateException(e.getCause());
				else
					throw e;
			}
		}
		controller.setParameters(wizard.getPublicParameterTree());
		controller.setRecorders(wizard.getRecorders());
		controller.setStopCondition(wizard.getStoppingCondition());
		controller.setModelPath(wizard.getModelRoot());
		try {
			controller.setEntryPoint(wizard.getModelFileName());
		} catch (InvalidEntryPointException e) {
			ParameterSweepWizard.logError("Error while launching the simulation: %s",wizard.getModelFileName());
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			if (ParameterSweepWizard.isFromMEME())
				Utilities.userAlert(ParameterSweepWizard.getFrame(),"Error while launching the simulation: " + wizard.getModelFileName());
			else 
				Utilities.userAlert(appWindow,"Error while launching the simulation: " + wizard.getModelFileName());
			return false;
		}

		if (ParameterSweepWizard.isFromMEME()) 
			appWindow.setVisible(false);
		else	
			((JFrame)appWindow).setTitle("MEME Simulation Monitor � 2007 - " + Calendar.getInstance().get(Calendar.YEAR) + 
										 " Aitia International, Inc.");
		Frame owner = !ParameterSweepWizard.isFromMEME() ? (Frame) appWindow : ParameterSweepWizard.getFrame();
		String details = "";
		if (wizard.getDescription() != null && wizard.getDescription().length() > 0)
			details = "Description:\n" + wizard.getDescription() + "\n";
		final LocalMonitorLogic logicalMonitor = new LocalMonitorLogic(owner,controller,wizard.getGeneratedModelName(),details,
																	   wizard.getOutputFiles(),ParameterSweepWizard.isFromMEME(),manager,callback,
																	   wizard.getSettingsFile(),wizard.isIntelliRun(),
																	   wizard.getSweepingMethodID() == IIntelliMethodPlugin.DYNAMIC_METHOD);
		MonitorGUI gui = logicalMonitor.getGUI();
		if (ParameterSweepWizard.isFromMEME()) {
			manager.add(gui,"Local Monitor",false);
			manager.rebuild();
			manager.setActive(gui);
		} else {
			final JFrame frame = (JFrame) appWindow;
			final JScrollPane sp = new JScrollPane(gui,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			sp.setBorder(null);
			frame.setContentPane(sp);
			frame.pack();
			Dimension oldD = frame.getPreferredSize();
			frame.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
										         oldD.height + sp.getHorizontalScrollBar().getHeight()));
			sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			oldD = frame.getPreferredSize();
			final Dimension newD = GUIUtils.getPreferredSize(frame);
			if (!oldD.equals(newD)) 
				frame.setPreferredSize(newD);
			frame.pack();
		}
		
		final String[] error = { null };
		final IBatchController _controller = controller;
		final Runnable r = new Runnable() {
			public void run() {
				try {
					logicalMonitor.startMonitor();
					_controller.startBatch();
				} catch (BatchException e) {
					Logger.logExceptionCallStack(e);
					if (Util.getLocalizedMessage(e) != null)
						error[0] = Util.getLocalizedMessage(e);
					else
						error[0] = e.toString();
				}
			}
		};
		if (ParameterSweepWizard.isFromMEME()) {
			// bug #1215: without it deadlock easily occurs on multi-core machines
			// In the multi-platform MEME this solution not work.
			// However, the simple r.run() call didn't cause deadlock for me on multi-core machine
			Thread t = new Thread() {
				@Override
				public void run() {
					// just in case
					Thread.currentThread().setContextClassLoader(_controller.getClass().getClassLoader());
					r.run();
					if (error[0] != null) {
						Utilities.userAlert(ParameterSweepWizard.getFrame(),error[0]);
						ParameterSweepWizard.logError(error[0]);
						manager.remove(logicalMonitor.getGUI());
					} 

//					try {
//						SwingUtilities.invokeAndWait(r);
//					} catch (InterruptedException e) {
//						ParameterSweepWizard.logError("Wizard error: %s",Util.getLocalizedMessage(e));
//					} catch (InvocationTargetException e) {
//						ParameterSweepWizard.logError("Wizard error: %s",Util.getLocalizedMessage(e));
//					}
				}
			};
			t.setName("MEME-Monitor-Thread");
			t.start();
		} else 
			r.run();
		
		if (error[0] != null) {
			Utilities.userAlert(appWindow,error[0]);
			ParameterSweepWizard.logError(error[0]);
			return false;
		}
		wizard.dispose();
		if (!ParameterSweepWizard.isFromMEME()) {
			try {
				Thread.currentThread().join(); // WTF Waiting itself?
			} catch (InterruptedException e) {}
		}
		return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean remoteLaunch() {
		final MessageScreen mScreen = new MessageScreen(ParameterSweepWizard.getFrame(),"Deploying model...","Please wait!");
		mScreen.showScreen();
		if (wizard.getGeneratedModelName() == null) {
			mScreen.hideScreen();
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"Model deployment error.","Reason: Model name is null.");
			ParameterSweepWizard.logError("Model name is null.");
			return false;
		} 
		List<String> zipSources = new ArrayList<String>(); // list of files that must zip and upload to the server
		List<String> resourceZipSources = new ArrayList<String>(); // list of files that must zip and upload to the server preserving its relative
																   // path
		List<String> sources = new ArrayList<String>();
		
		// O. model file(s) (if it is not a .class file)
		if (!wizard.getModelFileName().endsWith(".class")) {
			zipSources.add(wizard.getModelFileName());
			if (wizard.getGeneratedModelName() != null)  {
				int idx = wizard.getModelFileName().lastIndexOf('.');
				String extension = "";
				if (idx != -1)
					extension = wizard.getModelFileName().substring(idx);
				zipSources.add(wizard.getModelRoot() + File.separator + wizard.getGeneratedModelName() + extension);
			}
		}

		if (PlatformSettings.getGUIControllerForPlatform().isClassPathEnabled()) {
			Object[] cp = wizard.getClassPathListModel().toArray();
			
			// I. jars from the classpath (except platform-related jars)
			for (int i = 0;i < cp.length;++i) {
				String element = cp[i].toString();
				if (!element.endsWith(".jar"))
					sources.add(element);
				else if (isUploadable(element)) 
					zipSources.add(element);
			}
		}
			
		// II. jars from the plugin classpath (except built-in plugins)
		List<String> pluginClassPath = ParameterSweepWizard.getPluginManager().getPluginClassPath();
		for (String element : pluginClassPath) {
			if (!element.endsWith(".jar"))
				sources.add(element);
			else if (isUploadable(element))
				zipSources.add(element);
		}
		
		// III. jars from the system classpath (expect platform- and jre-related jars)
		zipSources.add("meme-paramsweep.jar");
		String systemClassPath = System.getProperty("java.class.path");
		String[] scp = systemClassPath.split(File.pathSeparator);
		for (int i = 0;i < scp.length;++i) {
			String element = scp[i].trim();
			if (!element.endsWith(".jar"))
				sources.add(element);
			else if (isUploadable(element))
				zipSources.add(element);
		}
		
		if (sources.size() > 0)  {
			JarGenerator generator = new JarGenerator(sources,wizard.getGeneratedModelName() + ".jar");
			try {
				generator.generateJar();
				// IV. created jar
				zipSources.add(0,wizard.getGeneratedModelName() + ".jar");
			} catch (IOException e) {
				mScreen.hideScreen();
				Utilities.userAlert(ParameterSweepWizard.getFrame(),"Error while packing the model files.");
				ParameterSweepWizard.logError(Util.getLocalizedMessage(e) + "\nStacktrace:");
				e.printStackTrace(ParameterSweepWizard.getLogStream());
				return false;
			}
		}
		
		// V. wizard settings xml file
		zipSources.add(wizard.getSettingsFile());
		
		// VI. resources
		Object[] resources = wizard.getResources();
		if (resources.length != 0) {
			for (Object o : resources) {
				String resource = (String) o;
				resourceZipSources.add(resource);
			}
		}
		List<String> automaticResources = wizard.getAutomaticResources();
		if (automaticResources != null && automaticResources.size() > 0) {
			for (String r : automaticResources)
				resourceZipSources.add(r);
		}
		
		String jobPackage = wizard.getGeneratedModelName() + ".zip";
		// creating zip for the job
		ZipGenerator zipGenerator = new ZipGenerator(zipSources,resourceZipSources,jobPackage,wizard.getModelRoot());
		try {
			zipGenerator.zip();
		} catch (IOException e) {
			mScreen.hideScreen();
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"Error while packing the model files.");
			ParameterSweepWizard.logError(Util.getLocalizedMessage(e) + "\nStacktrace:");
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			deleteGeneratedFiles();
			return false;
		}
		
		// using sftp 
		final String host = ParameterSweepWizard.getPreferences().getHostName();
		final String user = ParameterSweepWizard.getPreferences().getUserName();
		final String passwd = ParameterSweepWizard.getPreferences().getPassword();
		final String privateKeyFile = ParameterSweepWizard.getPreferences().getPrivateKeyFile();
		final String passphrase = ParameterSweepWizard.getPreferences().getPassphrase();
		final int sshPort = ParameterSweepWizard.getPreferences().getSSHPort();
		final String workspace = ParameterSweepWizard.getPreferences().getWorkspace();
		SftpClient client = null;
		if (privateKeyFile == null || "".equals(privateKeyFile.trim()))
			client = new SftpClient(host,sshPort,user,passwd,workspace);
		else {
			File file = new File(privateKeyFile);
			if (!file.exists()) {
				mScreen.hideScreen();
				Utilities.userAlert(ParameterSweepWizard.getFrame(),"Private key file not found: " + privateKeyFile);
				ParameterSweepWizard.logError("Private key file " + privateKeyFile + " is not found.");
				deleteGeneratedFiles();
				return false;
			}
			client = new SftpClient(host,sshPort,user,privateKeyFile,passphrase,workspace);
		}
		String error = client.connect();
		if (error != null) {
			mScreen.hideScreen();
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"Error while connecting to the server.",error);
			ParameterSweepWizard.logError(error);
			deleteGeneratedFiles();
			return false;
		}

		try {
			boolean notAbort = client.upload(ParameterSweepWizard.getFrame(),jobPackage,ParameterSweepWizard.getPreferences().getWorkspace());
			if (!notAbort) {
				client.removeFile(jobPackage);
				client.disconnect();
				mScreen.hideScreen();
				Utilities.userAlert(ParameterSweepWizard.getFrame(),"The operation is aborted by the user.");
				deleteGeneratedFiles();
				return false;
			} else if (ParameterSweepWizard.isFromMEME()) {
				ParameterSweepWizard.getFrame().update(ParameterSweepWizard.getFrame().getGraphics());
				mScreen.update(mScreen.getGraphics());
			} else {
				// TODO nem friss�l a k�perny�
//				mScreen.update(mScreen.getGraphics());
//				wizard.invalidate();
//				wizard.validate();
			}
		} catch (SftpException e) {
			mScreen.hideScreen();
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"Error while transfering data to the server.",Util.getLocalizedMessage(e));
			ParameterSweepWizard.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			try { client.removeFile(jobPackage); } catch (SftpException e1) {}
			client.disconnect();
			deleteGeneratedFiles();
			return false;
		} catch (FileNotFoundException e) {
			mScreen.hideScreen();
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"File error while transfering data to the server.",Util.getLocalizedMessage(e));
			ParameterSweepWizard.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			client.disconnect();
			deleteGeneratedFiles();
			return false;
		}
		
		Job job = createJob(jobPackage,zipSources,true); 
		ScheduleSimulationRequest request = new ScheduleSimulationRequest(job);
		
		final int port = ParameterSweepWizard.getPreferences().getPort();
		Socket socket = null;
		try {
			socket = new Socket(host,port);
		} catch (IOException e) {
			mScreen.hideScreen();
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"Unable to connect to the server: " + host + "/" + String.valueOf(port),
								Util.getLocalizedMessage(e),"Please check the network properties.");
			ParameterSweepWizard.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			try { client.removeFile(jobPackage); } catch (SftpException e1) {}
			client.disconnect();
			deleteGeneratedFiles();
			return false;
		}
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		try {
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			out.writeObject(request);
			out.flush();
			Message response = (Message) in.readObject();
			if (response.getMessageType() == MessageTypes.MSG_SERVER_OK) {
				client.disconnect();
				deleteGeneratedFiles();
				mScreen.hideScreen();
				Utilities.userAlert(ParameterSweepWizard.getFrame(),"The simulation server has acknowledged the request.");
				if (ParameterSweepWizard.getPreferences().startMonitor()) {
					if (ParameterSweepWizard.isFromMEME() && manager.hasAliveMonitor())
						manager.setMonitorActive();
					else {
						Thread remoteMonitor = new Thread(new Runnable() {
							public void run() {
								String private_key_file = privateKeyFile.trim().equals("") ? null : privateKeyFile;
								String phrase = passphrase.trim().equals("") ? null : passphrase;
								boolean success = MonitorConfigurationDialog.createNewConfiguration(host,port,user,passwd,private_key_file,
																										  phrase,sshPort,workspace);
								if (!success) 
									Utilities.userAlert(ParameterSweepWizard.getFrame(),"The automatic update of the monitor configuration is failed.",
																						"You may update the monitor's settings manually.");	

								if (ParameterSweepWizard.isFromMEME()) 
									PS_Monitor.createMonitor(ParameterSweepWizard.getFrame(),ParameterSweepWizard.isFromMEME(),
															 monitorServerProperties,callback,manager);
								else 
									PS_Monitor.createMonitor(false);
							}
						});
						remoteMonitor.setName("MEME-RemoteMonitor-Thread");
						remoteMonitor.start();
					}
					try {
						appWindow.setVisible(false);
						if (!ParameterSweepWizard.isFromMEME()) 
							Thread.currentThread().join(); // WTF ez itt mit csin�l?
					} catch (InterruptedException e) {}
				}
				return true;
			} else if (response.getMessageType() == MessageTypes.MSG_SERVER_FAILURE) {
				// error is on server side
				mScreen.hideScreen();
				Utilities.userAlert(ParameterSweepWizard.getFrame(),"Simulation server error: doesn't accomplish the request.",
									"Try to run your simulation on an other cluster or on localhost.");
				if (ParameterSweepWizard.isFromMEME())
					ParameterSweepWizard.getFrame().update(ParameterSweepWizard.getFrame().getGraphics());
				ParameterSweepWizard.logError("Simulation server error: %s",wizard.getGeneratedModelName());
				try { client.removeFile(jobPackage); } catch (SftpException e1) {}
				client.disconnect();
				deleteGeneratedFiles();
				return false; // user can try again on an other cluster or on localhost
			} else
				throw new ClassCastException();
		} catch (ClassCastException e) {
			// error is on server side
			mScreen.hideScreen();
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"Simultation server error: unknown server response.",
								"Try to run your simulation on an other cluster or on localhost.");
			if (ParameterSweepWizard.isFromMEME())
				ParameterSweepWizard.getFrame().update(ParameterSweepWizard.getFrame().getGraphics());
			ParameterSweepWizard.logError("Unknown server response: %s",wizard.getGeneratedModelName());
			try { client.removeFile(jobPackage); } catch (SftpException e1) {}
			client.disconnect();
			deleteGeneratedFiles();
			return false; // user can try again on an other cluster or on localhost
		} catch (IOException e) {
			mScreen.hideScreen();
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"Connection error.",Util.getLocalizedMessage(e));
			if (ParameterSweepWizard.isFromMEME())
				ParameterSweepWizard.getFrame().update(ParameterSweepWizard.getFrame().getGraphics());
			ParameterSweepWizard.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			try { client.removeFile(jobPackage); } catch (SftpException e1) {}
			client.disconnect();
			deleteGeneratedFiles();
			return false;
		} catch (ClassNotFoundException e) {
			mScreen.hideScreen();
			throw new IllegalStateException(e);
		} finally {
			mScreen.hideScreen();
			try { out.close(); } catch (IOException e) {}
			try { in.close(); } catch (IOException e) {}
			try { socket.close(); } catch (IOException e) {}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean qcgPackageCreation() {
		MessageScreen ms = new MessageScreen(ParameterSweepWizard.getFrame(),"Creating QosCosGrid package...","Please wait!");
		try {
			ms.showScreen();
			File jobDir = prepareQCGDirectories();
			createFatJar();
			copyOtherFiles();
			createResourcesZip();
			createQCGJobDescriptorFile(jobDir);
			ms.hideScreen();
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"QosCosGrid package is successfully created.","The files you have to upload to " +
								"the QosCosGrid website can be found in the",jobDir.getAbsolutePath() + " directory.");
		} catch (Exception e) {
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"Error while creating the QosCosGrid package: " + Util.getLocalizedMessage(e));
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			qcgClean();
			ms.hideScreen();
			return false;
		}
		return true;

//		final ProgressMonitor monitor = new ProgressMonitor(ParameterSweepWizard.getFrame(),"Creating QosCosGrid package...","",0,5);
//		monitor.setMillisToPopup(0);
//		monitor.setMillisToDecideToPopup(0);
//		monitor.setProgress(0);
//		final QCGTask aTask = new QCGTask();
//		aTask.addPropertyChangeListener(new PropertyChangeListener() {
//			public void propertyChange(PropertyChangeEvent evt) {
//		        if ("progress".equals(evt.getPropertyName())) {
//		            int progress = (Integer) evt.getNewValue();
//		            monitor.setProgress(progress);
//		            String message = String.format("Completed %d/5 stages.\n",progress);
//		            monitor.setNote(message);
//		            if (monitor.isCanceled()) {
//	                    aTask.cancel(true);
//	                    qcgClean();
//	                    qcgResult = false;
//		            }
//		        }
//			}
//			
//		});
//		aTask.execute();
//		
//		while (qcgResult == null) {
//			try {
//				Thread.sleep(300);
//			} catch (InterruptedException e) {}
//		}
//		return (qcgResult == null ? false : qcgResult);
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean vCloudLaunch() {
		final VCloudAuthenticationDialog authentication = new VCloudAuthenticationDialog(ParameterSweepWizard.getFrame());
		int result = authentication.showDialog();
		
		if (result == VCloudAuthenticationDialog.CANCEL) {
			authentication.dispose();
			return false;
		}
		
		final String username = authentication.getUsername();
		final char[] password = authentication.getPassword();
		final int workers = authentication.getNumberOfRequestedWorkers();
		authentication.dispose();
		
		if (ParameterSweepWizard.isFromMEME())
			ParameterSweepWizard.getFrame().update(ParameterSweepWizard.getFrame().getGraphics());

		
		final MessageScreen mScreen = new MessageScreen(ParameterSweepWizard.getFrame(),"Deploying model...","Please wait!");
		mScreen.showScreen();
		if (wizard.getGeneratedModelName() == null) {
			mScreen.hideScreen();
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"Model deployment error.","Reason: Model name is null.");
			ParameterSweepWizard.logError("Model name is null.");
			return false;
		} 
		
		final List<String> zipSources = new ArrayList<String>(); // list of files that must zip and upload to the server
		final List<String> resourceZipSources = new ArrayList<String>(); // list of files that must zip and upload to the server preserving its relative
																         // path
		final List<String> sources = new ArrayList<String>();
		
		// O. model file(s) (if it is not a .class file)
		if (!wizard.getModelFileName().endsWith(".class")) {
			zipSources.add(wizard.getModelFileName());
			if (wizard.getGeneratedModelName() != null)  {
				final int idx = wizard.getModelFileName().lastIndexOf('.');
				String extension = "";
				if (idx != -1)
					extension = wizard.getModelFileName().substring(idx);
				zipSources.add(wizard.getModelRoot() + File.separator + wizard.getGeneratedModelName() + extension);
			}
		}

		if (PlatformSettings.getGUIControllerForPlatform().isClassPathEnabled()) {
			final Object[] cp = wizard.getClassPathListModel().toArray();
			
			// I. jars from the classpath (except platform-related jars)
			for (int i = 0;i < cp.length;++i) {
				final String element = cp[i].toString();
				if (!element.endsWith(".jar"))
					sources.add(element);
				else if (isUploadableToVCloud(element)) 
					zipSources.add(element);
			}
		}
			
		// II. jars from the plugin classpath (except built-in plugins)
		final List<String> pluginClassPath = ParameterSweepWizard.getPluginManager().getPluginClassPath();
		for (final String element : pluginClassPath) {
			if (!element.endsWith(".jar"))
				sources.add(element);
			else if (isUploadableToVCloud(element))
				zipSources.add(element);
		}
		
		// III. jars from the system classpath (expect platform- and jre-related jars)
		zipSources.add("meme-paramsweep.jar");
		final String systemClassPath = System.getProperty("java.class.path");
		final String[] scp = systemClassPath.split(File.pathSeparator);
		for (int i = 0;i < scp.length;++i) {
			final String element = scp[i].trim();
			if (!element.endsWith(".jar"))
				sources.add(element);
			else if (isUploadableToVCloud(element))
				zipSources.add(element);
		}
		
		if (sources.size() > 0) {
			// IV. created jar (the real creating process is delayed)
			zipSources.add(0,wizard.getGeneratedModelName() + ".jar");
		}

		// V. wizard settings xml file
		zipSources.add(wizard.getSettingsFile());
		
		// VI. resources
		final Object[] resources = wizard.getResources();
		if (resources.length != 0) {
			for (final Object o : resources) {
				final String resource = (String) o;
				resourceZipSources.add(resource);
			}
		}
		final List<String> automaticResources = wizard.getAutomaticResources();
		if (automaticResources != null && automaticResources.size() > 0) {
			for (final String r : automaticResources)
				resourceZipSources.add(r);
		}
		
		final String jobPackage = wizard.getGeneratedModelName() + ".zip";
		final Job job = createJob(jobPackage,zipSources,false);
		Date date = null;
		try {
			date = Utilities.getTimeStamp(wizard.getGeneratedModelName());
		} catch (final ParseException e) {
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			date = new Date();
		} 
		final Calendar c = Calendar.getInstance();
		
		final String encodedPassword = Utilities.md5(new String(password));
		final IdentifiedScheduleSimulationRequest request = new IdentifiedScheduleSimulationRequest(MessageTypes.MSG_IDENTIFIED_CLIENT_RUN,job,
																									username,encodedPassword.toCharArray(),workers,
																									date,c.getTimeZone());

		final String host = ParameterSweepWizard.getPreferences().getVCloudHostName();
		final int port = ParameterSweepWizard.getPreferences().getVCloudPort();
		Socket socket = null;
		try {
			socket = new Socket(host,port);
		} catch (final IOException e) {
			mScreen.hideScreen();
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"Unable to connect to the Model Exploration Server: " + host + "/" + 
								String.valueOf(port),Util.getLocalizedMessage(e),"Please check the network properties.");
			ParameterSweepWizard.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			return false;
		}
		ObjectOutputStream out = null;
		ObjectInputStream in = null;
		try {
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			out.writeObject(request);
			out.flush();
			
			final Message response = (Message) in.readObject();
			
			if (response.getMessageType() == MessageTypes.MSG_SERVER_FAILURE) {
				mScreen.hideScreen();
				final ErrorMessage error = (ErrorMessage) response;
				Utilities.userAlert(ParameterSweepWizard.getFrame(),error.getDescription());
				return false;
			} else if (response.getMessageType() == MessageTypes.MSG_TRANSFER_DATA) {
				final TransferDataMessage msg = (TransferDataMessage) response;
				
				final int sshPort = msg.getPort();
				final String sshUser = msg.getUserName();
				String sshPassword = new String(msg.getPassword());
				try {
					sshPassword = msg.isPasswordEncoded() ? Utilities.decode(sshPassword) : sshPassword;
				} catch (final Exception e) {
					out.writeObject(new ErrorMessage("Problem occured during the file transfer"));
					out.flush();
					mScreen.hideScreen();
					Utilities.userAlert(ParameterSweepWizard.getFrame(),"Unable to transfer the model files to the Model Exploration Server",
										Util.getLocalizedMessage(e));
					if (ParameterSweepWizard.isFromMEME())
						ParameterSweepWizard.getFrame().update(ParameterSweepWizard.getFrame().getGraphics());
					ParameterSweepWizard.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
					e.printStackTrace(ParameterSweepWizard.getLogStream());
					return false;
				} 
				String workspace = msg.getWorkspace();
				
				final int idx = workspace.indexOf(":\\");
				if (idx != -1)
					workspace = "/" + workspace.substring(idx + 2);

				if (sources.size() > 0)  {
					final JarGenerator generator = new JarGenerator(sources,wizard.getGeneratedModelName() + ".jar");
					try {
						generator.generateJar();
					} catch (final IOException e) {
						out.writeObject(new ErrorMessage("Problem occured during the experiment package creation."));
						out.flush();
						mScreen.hideScreen();
						Utilities.userAlert(ParameterSweepWizard.getFrame(),"Error while packing the model files.");
						ParameterSweepWizard.logError(Util.getLocalizedMessage(e) + "\nStacktrace:");
						e.printStackTrace(ParameterSweepWizard.getLogStream());
						return false;
					}
				}
				
				// creating zip for the job
				final ZipGenerator zipGenerator = new ZipGenerator(zipSources,resourceZipSources,jobPackage,wizard.getModelRoot());
				try {
					zipGenerator.zip();
				} catch (final IOException e) {
					out.writeObject(new ErrorMessage("Problem occured during the experiment package creation."));
					out.flush();
					mScreen.hideScreen();
					Utilities.userAlert(ParameterSweepWizard.getFrame(),"Error while packing the model files.");
					ParameterSweepWizard.logError(Util.getLocalizedMessage(e) + "\nStacktrace:");
					e.printStackTrace(ParameterSweepWizard.getLogStream());
					deleteGeneratedFiles();
					return false;
				}
				
				final IFileTranserService fts = new SFTPFileTransferService(ParameterSweepWizard.getPreferences().getVCloudHostName(),sshUser,
																			sshPassword,sshPort,workspace);

				try {
					final boolean notAborted = fts.uploadFiles(ParameterSweepWizard.getFrame(),username,jobPackage);
					
					if (!notAborted) { // uploading aborted
						out.writeObject(new ErrorMessage("Experiment is aborted by client."));
						out.flush();
						mScreen.hideScreen();
						Utilities.userAlert(ParameterSweepWizard.getFrame(),"The operation is aborted by the user.");
						deleteGeneratedFiles();
						return false;
					} else if (ParameterSweepWizard.isFromMEME()) {
						ParameterSweepWizard.getFrame().update(ParameterSweepWizard.getFrame().getGraphics());
						mScreen.update(mScreen.getGraphics());
					}
				} catch (final OperationFailedException e) {
					out.writeObject(new ErrorMessage("Problem occured during the file transfer"));
					out.flush();
					mScreen.hideScreen();
					Utilities.userAlert(ParameterSweepWizard.getFrame(),"Error while transfering data to the server.",Util.getLocalizedMessage(e));
					ParameterSweepWizard.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
					e.printStackTrace(ParameterSweepWizard.getLogStream());
					deleteGeneratedFiles();
					return false;
				}
				
				out.writeObject(new Message(MessageTypes.MSG_ACK));
				out.flush();
				
				final Message answer = (Message) in.readObject();
				
				if (answer.getMessageType() == MessageTypes.MSG_SERVER_FAILURE) {
					mScreen.hideScreen();
					final ErrorMessage error = (ErrorMessage) answer;
					Utilities.userAlert(ParameterSweepWizard.getFrame(),error.getDescription());
					deleteGeneratedFiles();
					return false;
				} else if (answer.getMessageType() == MessageTypes.MSG_SERVER_OK) {
					deleteGeneratedFiles();
					mScreen.hideScreen();
					Utilities.userAlert(ParameterSweepWizard.getFrame(),"The Model Exploration Server has acknowledged the request.");
					return true;
				} else
					throw new ClassCastException();
			} else
				throw new ClassCastException();
		} catch (final ClassCastException e) {
			// error is on server side
			mScreen.hideScreen();
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"Model Exploration Server error: unknown server response.",
								"Try to run your simulation on localhost or try again later.");
			if (ParameterSweepWizard.isFromMEME())
				ParameterSweepWizard.getFrame().update(ParameterSweepWizard.getFrame().getGraphics());
			ParameterSweepWizard.logError("Unknown server response: %s",wizard.getGeneratedModelName());
			deleteGeneratedFiles();
			return false; // user can try again or on localhost
		} catch (final IOException e) {
			mScreen.hideScreen();
			Utilities.userAlert(ParameterSweepWizard.getFrame(),"Connection error.",Util.getLocalizedMessage(e));
			if (ParameterSweepWizard.isFromMEME())
				ParameterSweepWizard.getFrame().update(ParameterSweepWizard.getFrame().getGraphics());
			ParameterSweepWizard.logError(Util.getLocalizedMessage(e) + "\nStacktrace: ");
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			deleteGeneratedFiles();
			return false;
		} catch (final ClassNotFoundException e) {
			throw new IllegalStateException(e);
		} finally {
			mScreen.hideScreen();
			try { socket.close(); } catch (IOException e) {}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private Job createJob(final String packageName, final List<String> zipSources, final boolean needJobConfiguration) {
		Job job = new DefaultJob(wizard.getGeneratedModelName());
		job.setPlatform(PlatformSettings.getPlatformType());
		job.setPlatformVersion(PlatformManager.getPlatform(PlatformSettings.getPlatformType()).getVersion());
		job.setModelRoot(wizard.getModelRoot());
		
		String entryPoint = wizard.getEntryPoint();
		if (PlatformSettings.isUseGeneratedModel() && 
		   ((job.getPlatform() != PlatformType.NETLOGO && job.getPlatform() != PlatformType.NETLOGO5) || wizard.getGeneratedModelName().indexOf("nlogo") == -1)) {
			String extension = (job.getPlatform() == PlatformType.NETLOGO || job.getPlatform() == PlatformType.NETLOGO5) ? "nlogo" : "class";
			String simpleName = wizard.getGeneratedModelName().replaceAll("\\." + extension,"");
			int idx = simpleName.lastIndexOf('.');
			simpleName = simpleName.substring(idx + 1);
			idx = entryPoint.lastIndexOf(File.separatorChar);
			entryPoint = entryPoint.substring(0,idx + 1) + simpleName + "." + extension;
		}
		job.setEntryPoint(entryPoint);
		job.setPackageName(packageName);
		job.setDescription(wizard.getDescription());
		
		if (needJobConfiguration) {
			// TODO: currently we don't use priority nor worker number information
			JobConfiguration jc = new DefaultJobConfiguration(5,5,ParameterSweepWizard.getPreferences().getEmail());
			job.setJobConfiguration(jc);
		}
		
		job.setParameterTree(wizard.getPublicParameterTree());
		job.setStoppingCondition(wizard.getStoppingCondition());
		job.setRecorders(outputFileNames == null ? wizard.getRecorders() : getDummyRecorders());
		if (wizard.getSweepingMethodID() == 2) {
			Class<? extends IIntelliDynamicMethodPlugin> pluginClass = wizard.getIntelliDynamicMethodPlugin().getClass();
			job.setIntelliSweepInfo(new IntelliSweepInfo(pluginClass));
		}
		
		List<String> jarOrder = new ArrayList<String>();
		for (String source : zipSources) {
			if (source.endsWith(".jar")) {
				File f = new File(source);
				jarOrder.add(f.getName());
			}
		}
		job.setJarOrder(jarOrder);
		
		return job;
	}
	
	//----------------------------------------------------------------------------------------------------
	// pre-condition: outputFileNames != null
	public List<RecorderInfo> getDummyRecorders() {
		List<RecorderInfo> result = new ArrayList<RecorderInfo>(outputFileNames.length);
		for (String s : outputFileNames)
			result.add(new DummyRecorderInfo(s,new File(s)));
		return result;
	}
	
	//--------------------------------------------------------------------------------
	/** Returns true if the application must upload the jar file specified by <code>jarFileName</code>
	 *  before the starting of the simulation running in distributed mode.
	 */ 
	private boolean isUploadable(String jarFileName) {
		if (jarFileName == null || "".equals(jarFileName.trim()))
			return false;
		File file = new File(jarFileName.trim());
		return !(unuploadableCommonJars.contains(file.getName()) || unuploadableProActiveJars.contains(file.getName()));
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isUploadableToVCloud(String jarFileName) {
		if (jarFileName == null || "".equals(jarFileName.trim()))
			return false;
		File file = new File(jarFileName.trim());
		return !unuploadableCommonJars.contains(file.getName());
	}
	
	//--------------------------------------------------------------------------------
	/** Deletes the generated temporary files. */ 
	private void deleteGeneratedFiles() {
		File jar = new File(wizard.getGeneratedModelName() + ".jar");
		if (jar.exists())
			jar.delete();
		File zip = new File(wizard.getGeneratedModelName() + ".zip");
		if (zip.exists())
			zip.delete();
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void fillUnuploadableJarsLists() {
		File file = new File("resources/unuploadableCommonJars");
		if (file.exists()) {
			unuploadableCommonJars.clear();
			BufferedReader reader = null;;
			try {
				reader = new BufferedReader(new FileReader(file));
				String line = null;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.length() == 0 || line.startsWith("//")) continue;
					if (!line.endsWith(".jar")) {
						ParameterSweepWizard.logError("invalid line in unuploadableCommonJars: \"%s\" [ignored]",line);
						continue;
					}
					unuploadableCommonJars.add(line);
				}
			} catch (FileNotFoundException e) {
				// never happens; we check this in the if statement
				throw new IllegalStateException(e);
			} catch (IOException e) {
				e.printStackTrace(ParameterSweepWizard.getLogStream());
			} finally {
				try { reader.close(); } catch (IOException e1) {}
			}
		}
		
		file = new File("resources/unuploadableProActiveJars");
		if (file.exists()) {
			unuploadableProActiveJars.clear();
			BufferedReader reader = null;;
			try {
				reader = new BufferedReader(new FileReader(file));
				String line = null;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.length() == 0 || line.startsWith("//")) continue;
					if (!line.endsWith(".jar")) {
						ParameterSweepWizard.logError("invalid line in unuploadableProActiveJars: \"%s\" [ignored]",line);
						continue;
					}
					unuploadableProActiveJars.add(line);
				}
			} catch (FileNotFoundException e) {
				// never happens; we check this in the if statement
				throw new IllegalStateException(e);
			} catch (IOException e) {
				e.printStackTrace(ParameterSweepWizard.getLogStream());
			} finally {
				try { reader.close(); } catch (IOException e1) {}
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void extendClassPathWithPlatformSpecificEntries() {
		// it has already been added to the classpool when the platform was selected
//		List<String> defaultClassPathEntries = PlatformSettings.getDefaultClassPathEntries( wizard );
//		if (defaultClassPathEntries != null && defaultClassPathEntries.size() > 0) {
//			//String dirStr = PlatformManager.getInstallationDirectory(PlatformSettings.getPlatformType());
//			//File platformDir = new File(dirStr);
//			for (String entryStr : defaultClassPathEntries) {
//				File entry = new File(entryStr); 
//				try {
//					ClassPath classPath = wizard.getClassPool().appendClassPath(entry.getAbsolutePath());
//					wizard.getClassPathListModel().add(0, new ClassPathPair(entry.getName(), classPath));
//				} catch (NotFoundException e) {
//					ParameterSweepWizard.logError("The following exception occurs during the launching:");
//					e.printStackTrace(ParameterSweepWizard.getLogStream());
//				}
//			}
//		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private File prepareQCGDirectories() {
		File qcgMainDir = new File(QCG_PACKAGE_DIRECTORY);
		if (!qcgMainDir.exists())
			qcgMainDir.mkdir();
		File jobDir = new File(qcgMainDir,"Package_for_" + wizard.getGeneratedModelName());
		jobDir.mkdir();
		File tempDir = new File(qcgMainDir,"Temp");
		if (!tempDir.exists())
			tempDir.mkdir();
		else
			cleanDir(tempDir);
		return jobDir;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void cleanDir(File dir) {
		File[] files = dir.listFiles();
		for (File f : files)
			_clean(f);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void _clean(File f) {
		if (f.isDirectory()) {
			File[] files = f.listFiles();
			for (File _f : files)
				_clean(_f);
		}
		f.delete();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void qcgClean() {
		File qcgMainDir = new File(QCG_PACKAGE_DIRECTORY);
		File jobDir = new File(qcgMainDir,"Package_for_" + wizard.getGeneratedModelName());
		cleanDir(jobDir);
		jobDir.delete();
		File tempDir = new File(qcgMainDir,"Temp");
		cleanDir(tempDir);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void createFatJar() throws IOException {
		File qcgMainDir = new File(QCG_PACKAGE_DIRECTORY);
		File tempDir = new File(qcgMainDir,"Temp");
		File jobDir = new File(qcgMainDir,"Package_for_" + wizard.getGeneratedModelName());
		File fatJar = new File(jobDir,wizard.getGeneratedModelName() + ".jar");
		
		if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) {
			File netLogoDir = calculateNetLogoDir();
			Util.unzip(new File(netLogoDir,"NetLogo.jar"),tempDir);
			File libDir = new File(netLogoDir,"lib");
			File[] files = libDir.listFiles();
			for (File f : files) {
				if (!f.isDirectory() && f.getName().toLowerCase().endsWith(".jar"))
					Util.unzip(f,tempDir);
			}
		}
		
		File aDir = new File("lib");
		File[] files = aDir.listFiles();
		for (File f : files) {
			if (!f.isDirectory() && f.getName().toLowerCase().endsWith(".jar"))
				Util.unzip(f,tempDir);
		}
		
		// dps-3.90.jar
		Util.unzip(new File("resources/dps-3.90.jar"),tempDir); 
		
		aDir = new File("plugins");
		files = aDir.listFiles();
		for (File f : files) {
			if (!f.isDirectory() && f.getName().toLowerCase().endsWith(".jar"))
				Util.unzip(f,tempDir);
		}

		aDir = new File("PlatformPlugins");
		files = aDir.listFiles();
		for (File f : files) {
			if (!f.isDirectory() && f.getName().toLowerCase().endsWith(".jar"))
				Util.unzip(f,tempDir);
		}
		Util.unzip(new File("meme-paramsweep.jar"),tempDir);
		Util.unzip(new File("MEME.jar"),tempDir);
//		Util.unzip(new File("ParamSweep.jar"),tempDir);

		if (PlatformSettings.getGUIControllerForPlatform().isClassPathEnabled()) {
			List<String> dirsInClassPath = new ArrayList<String>();
			Object[] cp = wizard.getClassPathListModel().toArray();
			
			for (int i = 0;i < cp.length;++i) {
				String element = cp[i].toString();
				if (!element.toLowerCase().endsWith(".jar"))
					dirsInClassPath.add(element);
				else  
					Util.unzip(new File(element),tempDir);
			}
			
			for (String dirPath : dirsInClassPath)
				copyContent(new File(dirPath),tempDir);
		}
		
		replaceManifestFile();
		removeIndexListFile();
		JarGenerator.jarDirectory(tempDir,fatJar);
		cleanDir(tempDir);
	}
	
	//----------------------------------------------------------------------------------------------------
	private File calculateNetLogoDir() throws IOException {
		Util.unzipHere(new File("MEME.jar"),"MANIFEST.MF");
		BufferedReader reader = new BufferedReader(new FileReader("MANIFEST.MF"));
		String line = null;
		while ((line = reader.readLine()) != null) {
			if (line.contains("NetLogo.jar")) break;
		}
		reader.close();
		new File("MANIFEST.MF").delete();
		if (line == null)
			throw new IOException("missing NetLogo.jar reference in MANIFEST.MF");
		int idx = line.trim().lastIndexOf("file:");
		URL url = new URL(line.substring(idx));
		try {
			return new File(url.toURI()).getParentFile();
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void copyContent(File dir, File destDir) throws IOException {
		File[] files = dir.listFiles();
		for (File f : files) {
			if (f.isDirectory()) {
				File newDir = new File(destDir,f.getName());
				newDir.mkdir();
				copyContent(f,newDir);
			} else {
				FileInputStream is = new FileInputStream(f);
				FileOutputStream os = new FileOutputStream(new File(destDir,f.getName()));
				Util.copyInputStream(is,os);
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void replaceManifestFile() throws IOException {
		File manifestFile = new File("resources/MANIFEST.MF");
		File destFile = new File(QCG_PACKAGE_DIRECTORY + "/Temp/META-INF/MANIFEST.MF");
		FileInputStream is = new FileInputStream(manifestFile);
		FileOutputStream os = new FileOutputStream(destFile);
		Util.copyInputStream(is,os);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void removeIndexListFile() {
		File indexList = new File(QCG_PACKAGE_DIRECTORY + "/Temp/META-INF/INDEX.LIST");
		if (indexList.exists())
			indexList.delete();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void createQCGJobDescriptorFile(File jobDir) throws Exception {
		QCGJobDescriptorGenerator generator = new QCGJobDescriptorGenerator(JobDescriptorType.BASIC_DORTMUND,jobDir,wizard.getGeneratedModelName(),
																			wizard.getRecorders());
		generator.generateJobDescriptor();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void copyOtherFiles() throws IOException {
		File qcgMainDir = new File(QCG_PACKAGE_DIRECTORY);
		File jobDir = new File(qcgMainDir,"Package_for_" + wizard.getGeneratedModelName());
		
		File settingsFile = new File(ParameterSweepWizard.getPreferences().getSettingsPath() + File.separator + wizard.getGeneratedModelName() +
									 ".settings.xml");
		FileInputStream is = new FileInputStream(settingsFile);
		FileOutputStream os = new FileOutputStream(new File(jobDir,settingsFile.getName()));
		Util.copyInputStream(is,os);
		
		if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5) {
			is = new FileInputStream(wizard.getModelFile());
			os = new FileOutputStream(new File(jobDir,wizard.getModelFile().getName()));
			Util.copyInputStream(is,os);
			
			if (wizard.getGeneratedModelName() != null)  {
				int idx = wizard.getModelFileName().lastIndexOf('.');
				String extension = "";
				if (idx != -1)
					extension = wizard.getModelFileName().substring(idx);
				File generatedFile = new File(wizard.getModelRoot() + File.separator + wizard.getGeneratedModelName() + extension);
				is = new FileInputStream(generatedFile);
				os = new FileOutputStream(new File(jobDir,generatedFile.getName()));
				Util.copyInputStream(is,os);
			}
			
			is = new FileInputStream("resources/typechecker.jar");
			os = new FileOutputStream(new File(jobDir,"typechecker.jar"));
			Util.copyInputStream(is,os);
			
			is = new FileInputStream("resources/typechecker5.jar");
			os = new FileOutputStream(new File(jobDir,"typechecker5.jar"));
			Util.copyInputStream(is,os);
		}
 	}
	
	//----------------------------------------------------------------------------------------------------
	private void createResourcesZip() throws IOException {
		List<String> resourceZipSources = new ArrayList<String>();
		Object[] resources = wizard.getResources();
		if (resources.length != 0) {
			for (Object o : resources) {
				String resource = (String) o;
				resourceZipSources.add(resource);
			}
		}
		List<String> automaticResources = wizard.getAutomaticResources();
		if (automaticResources != null && automaticResources.size() > 0) {
			for (String r : automaticResources)
				resourceZipSources.add(r);
		}
		if (resourceZipSources.size() > 0) {
			String jobPackage = wizard.getGeneratedModelName() + ".zip";
			File qcgMainDir = new File(QCG_PACKAGE_DIRECTORY);
			File jobDir = new File(qcgMainDir,"Package_for_" + wizard.getGeneratedModelName());
			ZipGenerator zipGenerator = new ZipGenerator(new ArrayList<String>(),resourceZipSources,
														 jobDir.getAbsolutePath() + File.separator + jobPackage,wizard.getModelRoot());
			zipGenerator.zip();
		}
	}

	//======================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	public static class MessageScreen extends JDialog {
		
		//====================================================================================================
		// members
		
		private static final long serialVersionUID = 1L;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public MessageScreen(Frame owner, String... message) {
			super(owner,false);
			String text = "<html><font size=\"+1\"><b><center>";
			for (String s : message)
				text += s + "<br>";
			text += "<center></b></font></html>";
			JLabel textLabel = new JLabel(text,JLabel.CENTER);
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
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	private static class RecorderFilesDialog extends JDialog implements ActionListener {

		//====================================================================================================
		// members
		
		public static final int OK_OPTION = 0;
		public static final int CANCEL_OPTION = 1;
		
		private int returnType = CANCEL_OPTION;
		
		//====================================================================================================
		// GUI members
		
		JPanel content = new JPanel(new BorderLayout());
		JTextArea infoArea = new JTextArea();
		JPanel center = null;
		JTextField entryField = new JTextField();
		JButton addButton = new JButton("Add");
		JList entryList = new JList();
		JScrollPane entryListScr = new JScrollPane(entryList);
		JButton removeButton = new JButton("Remove");
		JPanel buttonsPanel = new JPanel();
		JButton okButton = new JButton("Ok");
		JButton cancelButton = new JButton("Cancel");
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public RecorderFilesDialog(JFrame owner) {
			super(owner,"Specify output files",true);
			layoutGUI();
		}
		
		//----------------------------------------------------------------------------------------------------
		public int showDialog() {
			this.setVisible(true);
			return returnType;
		}
		
		//----------------------------------------------------------------------------------------------------
		public String[] getOutputFileNames() {
			SortedListModel model = (SortedListModel) entryList.getModel();
			String[] result = new String[model.size()];
			for (int i = 0;i < model.size();++i)
				result[i] = (String) model.get(i);
			return result;
		}
		
		//====================================================================================================
		// implemented interfaces
		
		//----------------------------------------------------------------------------------------------------
		public void actionPerformed(ActionEvent e) {
			String command = e.getActionCommand();
			if ("OK".equals(command)) {
				returnType = OK_OPTION;
				this.setVisible(false);
			} else if ("CANCEL".equals(command)) {
				returnType = CANCEL_OPTION;
				this.setVisible(false);
			} else if ("ADD".equals(command)) {
				String entry = entryField.getText().trim();
				SortedListModel model = (SortedListModel) entryList.getModel();
				if (!model.contains(entry)) {
					model.addElement(entry);
					model.sort();
				}
				entryField.setText("");
				entryField.grabFocus();
			} else if ("REMOVE".equals(command)) {
				Object[] selected = entryList.getSelectedValues();
				if (selected.length > 0) {
					SortedListModel model = (SortedListModel) entryList.getModel();
					for (Object o : selected)
						model.removeElement(o);
					model.sort();
				}
			}
		}
		
		//====================================================================================================
		// GUI methods
		
		//----------------------------------------------------------------------------------------------------
		private void layoutGUI() {
			content.add(infoArea,BorderLayout.NORTH);
			
			center = FormsUtils.build("~ p ~ p:g ~ p ~",
									  "|012||" +
									  "334|" +
									  "33_ p:g||" +
									  "555 p|",
									  "Filename: ",entryField,addButton,
									  entryListScr,removeButton,
									  new Separator("")).getPanel();
			content.add(center,BorderLayout.CENTER);
			buttonsPanel.add(okButton);
			buttonsPanel.add(cancelButton);
			content.add(buttonsPanel,BorderLayout.SOUTH);
			
			infoArea.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
			infoArea.setEditable(false);
			infoArea.setLineWrap(true);
			infoArea.setWrapStyleWord(true);
			infoArea.setColumns(60);
			infoArea.setText("The wizard needs to know the name of the output files of the model to run in distributed mode.\nPlease specify" +
					         " them below or press 'Cancel' to run your simulation on the local machine.\n");
			
			entryField.setActionCommand("ADD");
			addButton.setActionCommand("ADD");
			
			entryList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			entryList.setModel(new SortedListModel());
			
			removeButton.setActionCommand("REMOVE");
			okButton.setActionCommand("OK");
			cancelButton.setActionCommand("CANCEL");
			
			GUIUtils.addActionListener(this,entryField,addButton,removeButton,okButton,cancelButton);
			
			this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
			this.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) { 
					returnType = CANCEL_OPTION;
					RecorderFilesDialog.this.setVisible(false);
				}
			});
			final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			sp.setBorder(null);
			this.setContentPane(sp);
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
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unused")
	private class QCGTask extends SwingWorker<Void,Void> {
		
		//====================================================================================================
		// members
		
		private File jobDir = null;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		@Override
		protected Void doInBackground() throws Exception {
			try {
				firePropertyChange("progress",6,0);
				jobDir = prepareQCGDirectories();
				setProgress(1);
				Thread.sleep(100);
				createFatJar();
				setProgress(2);
				Thread.sleep(1000);
				copyOtherFiles();
				setProgress(3);
				Thread.sleep(1000);
				createResourcesZip();
				setProgress(4);
				Thread.sleep(1000);
				createQCGJobDescriptorFile(jobDir);
				setProgress(5);
			} catch (InterruptedException _) {
			} catch (Exception e) {
				Utilities.userAlert(ParameterSweepWizard.getFrame(),"Error while creating the QosCosGrid package: " + Util.getLocalizedMessage(e));
				e.printStackTrace(ParameterSweepWizard.getLogStream());
				qcgClean();
				qcgResult = false;
			}
			return null;
		}
		
		//----------------------------------------------------------------------------------------------------
		@Override
		protected void done() {
			if (!isCancelled()) {
				Utilities.userAlert(ParameterSweepWizard.getFrame(),"QosCosGrid package is successfully created.","The files you have to upload to " +
						"the QosCosGrid website can be found in the",jobDir.getAbsolutePath() + " directory.");
				qcgResult = true;
			}
		}
	}
}
