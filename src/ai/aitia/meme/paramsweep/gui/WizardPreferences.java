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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.swing.JFileChooser;

//import com.apple.eio.FileManager;


import ai.aitia.meme.Logger;
import ai.aitia.meme.gui.Preferences;
import ai.aitia.meme.gui.PreferencesPage;
import ai.aitia.meme.gui.SimpleFileFilter;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.OSUtils;
import ai.aitia.meme.utils.OSUtils.OSType;


/** This class represents the Preferences dialog of the MASS/MEME Parameter Sweep Wizard. */
public class WizardPreferences extends Preferences {

	//=====================================================================================
	// nested classes
	
	//-------------------------------------------------------------------------------------
	public static interface IReinitalizeable {
		public void reinitialize();
	}

	//=====================================================================================
	// members

	private static final long serialVersionUID = 1L;

	/** The name of the file that stores the configuration of the wizard. */
	public static final String configurationFileName = "configuration.config"; 
	
	public static final String vCloudFileName = "vcloud-connection.cfg";

	/** Default value constant: the path of the model settings files. */
	private static final String DEFAULT_SETTINGS_PATH 			= System.getProperty("user.home");
	/** Default value constant: does the wizard generate source file whenever generates a model? */
	private static final String DEFAULT_SOURCE_GENERATION		= "false";
	/** Default value constant: does the wizard use RngSeed as a parameter even if the getInitParams() hasn't contain it? */
	private static final String DEFAULT_RNGSEED_AS_PARAMETER	= "true";
	private static final String DEFAULT_NETLOGO_RANDOM_SEED_AS_PARAMETER = "true";
	/** Default value constant: does the wizard close the scripting dialog after creating a script? */
	private static final String DEFAULT_CLOSE_AFTER_ONE_SCRIPT	= "false";
	
	private static final String DEFAULT_SAVE_PARAMETER_TREE	= "false";
	private static final String DEFAULT_SKIP_PLATFORM_PAGE	= "false";
	
	/** Default value constant: the host of the server. */
	private static final String DEFAULT_HOST 			= "localhost";
	/** Default value constant: the port of the simulation server. */
	private static final String DEFAULT_PORT 			= "3000";
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
	/** Default value constant: workspace of the server. */
	private static final String DEFAULT_WORKSPACE		= "workspace";
	/** Default value constant: e-mail address. */
	private static final String DEFAULT_EMAIL			= "";
	/** Default value constant: does the wizard starts the monitor application after it is closed? */
	private static final String DEFAULT_START_MONITOR	= "false";
	/** Default value constant: strategy of the simulation running (local, remote or qcg). */
	private static final String DEFAULT_RUN_STRATEGY	= "local";
	/** Default value constant: registered platform directory. */
	private static final String DEFAULT_REGISTERED_PLATFORMS = "";
	
	private static final String DEFAULT_NO_OF_WORKERS	= "1";
	
	private static final String DEFAULT_VC_STRING = "";
	private static final String DEFAULT_VC_HOST = "modelexploration.aitia.ai";
	private static final String DEFAULT_VC_RUN_OPTION = "leased";
	private static final String DEFAULT_VC_NO_OF_WORKERS = "4";
	private static final String DEFAULT_VC_PORT = "6000";
	
	/** Property key constant: the path of the model settings files. */
	public static final String SETTINGS_PATH						= "settings_path";
	/** Property key constant: does the wizard generate source file whenever generates a model? */
	public static final String SOURCE_GENERATION					= "source_generation";
	/** Property key constant: does the wizard generate simphony source file whenever generates a model? */
	public static final String REPASTS_SOURCE_GENERATION			= "repasts_source_generation";
	public static final String REPASTS2_SOURCE_GENERATION			= "repasts2_source_generation";
	public static final String CUSTOM_JAVA_SOURCE_GENERATION 		= "custom_java_source_generation";
	/** Property key constant: does the wizard use RngSeed as a parameter even if the getInitParams() hasn't contain it? */
	public static final String RNGSEED_AS_PARAMETER					= "rngseed_as_parameter";
	public static final String NETLOGO_RANDOM_SEED_AS_PARAMETER		= "netlogo_random_seed_as_parameter";
	public static final String NETLOGO5_RANDOM_SEED_AS_PARAMETER		= "netlogo5_random_seed_as_parameter";
	/** Property key constant: does the wizard close the scripting dialog after creating a script? */
	public static final String CLOSE_AFTER_ONE_SCRIPT				= "close_after_one_script";
	public static final String CUSTOM_JAVA_CLOSE_AFTER_ONE_SCRIPT 	= "custom_java_close_after_one_script";
	public static final String REPASTS2_CLOSE_AFTER_ONE_SCRIPT 	= "repasts2_close_after_one_script";
	public static final String NETLOGO_CLOSE_AFTER_ONE_SCRIPT		= "netlogo_close_after_one_script";
	public static final String NETLOGO5_CLOSE_AFTER_ONE_SCRIPT		= "netlogo5_close_after_one_script";
	public static final String MASON_SOURCE_GENERATION = "mason_source_generation";
	public static final String MASON_CLOSE_AFTER_ONE_SCRIPT = "mason_close_after_one_script";
	
	public static final String SAVE_PARAMETER_TREE					= "save_parameter_tree";
	public static final String SKIP_PLATFORM_PAGE					= "skip_platform_page";
	
	/** Property key constant: the host of the server. */
	public static final String HOSTNAME				= "hostname";
	/** Property key constant: the port of the simulation server. */
	public static final String PORT					= "port";
	/** Property key constant: the username. */
	public static final String USERNAME				= "username";
	/** Property key constant: password. */
	public static final String PASSWORD				= "password";
	/** Property key constant: the path of the private key file. */
	public static final String PRIVATE_KEY_FILE		= "private_key_file";
	/** Property key constant: passphrase. */
	public static final String PASSPHRASE			= "passphrase";
	/** Property key constant: ssh port. */
	public static final String SSH_PORT				= "ssh_port";
	/** Property key constant: e-mail address. */
	public static final String EMAIL				= "email";
	/** Property key constant: host of an SMTP server (different than the host of the server). */
	public static final String WORKSPACE			= "workspace";
	/** Property key constant: does the wizard starts the monitor application after it is closed? */
	public static final String START_MONITOR 		= "start_monitor";
	/** Property key constant: strategy of the simulation running (local, remote or qcg). */
	public static final String RUN_STRATEGY			= "run_strategy";
	public static final String REGISTERED_PLATFORMS = "registered_platforms";
	
	/** Property key constant: is the password is encoded? */
	public static final String PASSWORD_ENCODED 	= "password_encoded";
	/** Property key constant: is the passphrase is encoded*/
	public static final String PASSPHRASE_ENCODED 	= "passphrase_encoded";
	
	public static final String NO_OF_WORKERS		= "requested_workers";
	
	public static final String VCLOUD_USERNAME 			= "vcloud_username";
	public static final String VCLOUD_PASSWORD			= "vcloud_password";
	public static final String VCLOUD_PASSWORD_ENCODED 	= "vcloud_password_encoded";
	public static final String VCLOUD_RUN_OPTION		= "vcloud_run_option";
	public static final String VCLOUD_NO_OF_WORKERS		= "vcloud_requested_workers";

	
	
	/** The object that stores the informations. */
	private Properties properties = null;
	private Properties vCloudConnectionProperties = null;
	
	private Map<PlatformType,PreferencesPage> platformPages = new HashMap<PlatformType,PreferencesPage>();
	
	//=====================================================================================
	// methods
	
	//-------------------------------------------------------------------------------------
	/** Constructor. */
	public WizardPreferences() {
		
		IPreferencesPage platformPage = new Page_Platforms(this);
		
		this.properties = setSettingsFromFile(null);
		this.vCloudConnectionProperties = setVCloudConnectionSettings();
		this.addPage(new Page_General(this));
		this.addPage(platformPage);
		this.addPage(new Page_Network(this));
		platformPages.put(PlatformType.REPAST,this.addSubpage(platformPage,new Page_RepastJPrefs(this)));
		
		if (PlatformManager.isSupportedPlatform(PlatformType.CUSTOM))
			platformPages.put(PlatformType.CUSTOM,this.addSubpage(platformPage,new Page_CustomJavaPrefs(this)));
		if (PlatformManager.isSupportedPlatform(PlatformType.SIMPHONY))
			platformPages.put(PlatformType.SIMPHONY,this.addSubpage(platformPage,new Page_SimphonyPrefs(this)));
		if (PlatformManager.isSupportedPlatform(PlatformType.SIMPHONY2))
			platformPages.put(PlatformType.SIMPHONY2,this.addSubpage(platformPage,new Page_Simphony2Prefs(this)));
		if (PlatformManager.isSupportedPlatform(PlatformType.NETLOGO))
			platformPages.put(PlatformType.NETLOGO,this.addSubpage(platformPage,new Page_NetLogoPrefs(this)));
		if (PlatformManager.isSupportedPlatform(PlatformType.NETLOGO5))
			platformPages.put(PlatformType.NETLOGO5,this.addSubpage(platformPage,new Page_NetLogo5Prefs(this)));
		if (PlatformManager.isSupportedPlatform(PlatformType.TRASS))
			platformPages.put(PlatformType.TRASS,this.addSubpage(platformPage,new Page_TrassPrefs(this)));
		if (PlatformManager.isSupportedPlatform(PlatformType.MASON))
			platformPages.put(PlatformType.MASON,this.addSubpage(platformPage,new Page_MasonPrefs(this)));
//		if (PlatformManager.isSupportedPlatform(PlatformType.EMIL)) //TODO: kell regisztr�l� oldal az EMIL-nek is
//			platformPages.put(PlatformType.EMIL,this.addSubpage(platformPage,new Page_EMILPrefs(this)));

		gotoPage(2);
		gotoPage(1);
		gotoPage(0);
		selectFirstPage();
	}
	
	//-------------------------------------------------------------------------------------
	public Properties getProperties() { return properties; }
	public String getProperty(String key) { return properties.getProperty(key); }
	public String getSettingsPath() { return properties.getProperty(SETTINGS_PATH); }
	public Properties getVCloudConnectionProperties() { return vCloudConnectionProperties; }
	public String getVCloudConnectionProperty(final String key) { return vCloudConnectionProperties.getProperty(key); }
	
	//----------------------------------------------------------------------------------------------------
	public boolean needSource() { 
		switch (PlatformSettings.getPlatformType()) {
		case REPAST 	: return Boolean.parseBoolean(properties.getProperty(SOURCE_GENERATION));
		case SIMPHONY 	: return Boolean.parseBoolean(properties.getProperty(REPASTS_SOURCE_GENERATION));
		case SIMPHONY2 	: return Boolean.parseBoolean(properties.getProperty(REPASTS2_SOURCE_GENERATION));
		case CUSTOM		: return Boolean.parseBoolean(properties.getProperty(CUSTOM_JAVA_SOURCE_GENERATION));
		case MASON		: return Boolean.parseBoolean(properties.getProperty(MASON_SOURCE_GENERATION));
		default 		: return false;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean rngSeedAsParameter() {
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		: return Boolean.parseBoolean(properties.getProperty(RNGSEED_AS_PARAMETER));  
		case NETLOGO	: return Boolean.parseBoolean(properties.getProperty(NETLOGO_RANDOM_SEED_AS_PARAMETER));
		case NETLOGO5	: return Boolean.parseBoolean(properties.getProperty(NETLOGO5_RANDOM_SEED_AS_PARAMETER));
		default			: return false;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean closeAfterOneScript() {
		switch (PlatformSettings.getPlatformType()) {
		case REPAST		: return Boolean.parseBoolean(properties.getProperty(CLOSE_AFTER_ONE_SCRIPT));
		case CUSTOM		: return Boolean.parseBoolean(properties.getProperty(CUSTOM_JAVA_CLOSE_AFTER_ONE_SCRIPT));
		case NETLOGO	: return Boolean.parseBoolean(properties.getProperty(NETLOGO_CLOSE_AFTER_ONE_SCRIPT,"false"));
		case NETLOGO5	: return Boolean.parseBoolean(properties.getProperty(NETLOGO5_CLOSE_AFTER_ONE_SCRIPT,"false"));
		case SIMPHONY2	: return Boolean.parseBoolean(properties.getProperty(REPASTS2_CLOSE_AFTER_ONE_SCRIPT,"false"));
		case MASON		: return Boolean.parseBoolean(properties.getProperty(MASON_CLOSE_AFTER_ONE_SCRIPT,"false"));
		default			: return false;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getHostName() { return properties.getProperty(HOSTNAME); }
	public int getPort() { return Integer.parseInt(properties.getProperty(PORT)); }
	public String getUserName() { return properties.getProperty(USERNAME); }
	public String getPassword() { return properties.getProperty(PASSWORD); }
	public String getPrivateKeyFile() { return properties.getProperty(PRIVATE_KEY_FILE); }
	public String getPassphrase() { return properties.getProperty(PASSPHRASE); }
	public int getSSHPort() { return Integer.parseInt(properties.getProperty(SSH_PORT)); }
	public String getEmail() { return properties.getProperty(EMAIL); }
	public String getWorkspace() { return properties.getProperty(WORKSPACE); }
	public boolean startMonitor() { return Boolean.parseBoolean(properties.getProperty(START_MONITOR)); }
	public boolean isLocalRun() { return "local".equals(properties.getProperty(RUN_STRATEGY)); }
	public boolean isQCGRun() { return "qcg".equals(properties.getProperty(RUN_STRATEGY)); }
	public boolean isRemoteRun() { return "remote".equals(properties.getProperty(RUN_STRATEGY)); }
	public boolean isVCloudRun() { return "vcloud".equals(properties.getProperty(RUN_STRATEGY)); }
	public int getNumberOfRequestedWorkers() { return Integer.parseInt(properties.getProperty(NO_OF_WORKERS,"1")); }
	public boolean saveParameterTree() { return Boolean.parseBoolean(properties.getProperty(SAVE_PARAMETER_TREE,"false")); }
	public boolean skipPlatformPage() { return Boolean.parseBoolean(properties.getProperty(SKIP_PLATFORM_PAGE,"false")); }
	public Map<PlatformType,PreferencesPage> getPlatformPages() { return platformPages; } 
	
	public String getVCloudHostName() { return vCloudConnectionProperties.getProperty(HOSTNAME); }
	public int getVCloudPort() { return Integer.parseInt(vCloudConnectionProperties.getProperty(PORT,DEFAULT_VC_PORT)); }
	public String getVCloudUserName() { return properties.getProperty(VCLOUD_USERNAME); }
	public String getVCloudPassword() { return properties.getProperty(VCLOUD_PASSWORD); }
	public boolean isBestEffortService() { return "best-effort".equals(properties.getProperty(VCLOUD_RUN_OPTION)); }
	public boolean isLeasedService() { return "leased".equals(properties.getProperty(VCLOUD_RUN_OPTION)); }
	public int getNumberOfRequestedVCloudWorkers() { return isLeasedService() ? Integer.parseInt(properties.getProperty(VCLOUD_NO_OF_WORKERS)) : 0; }
	
	//-------------------------------------------------------------------------------------
	public void setSkipPlatformPage(boolean toSkip) { 
		String value = String.valueOf(toSkip);
		properties.setProperty(SKIP_PLATFORM_PAGE,value); 
	}
	
	//-------------------------------------------------------------------------------------
	/** It is used to save the setting into file. */
	@Override
	protected boolean canClose() { 
		if (!isCanceling()) {
			save(null);
			saveVCloudConnection();
		}
		return super.canClose();
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	protected void save() {
		if (!savePreferences()) 
			Utilities.userAlert(this,"Save failed.");
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	protected void load() {
		if (!loadPreferences())
			Utilities.userAlert(this,"Load failed.");
	}
	
	//-------------------------------------------------------------------------------------
	public boolean savePreferences() {
		PageIterator iterator = new PageIterator();
		while (iterator.hasNext()) {
			IPreferencesPage page = iterator.next();
			if (!page.onButtonPress(Button.OK))
				return true;
		}
		JFileChooser chooser = new JFileChooser(ParameterSweepWizard.getLastDir());
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.addChoosableFileFilter(new SimpleFileFilter("Wizard configuration files (*.config)"));
		int result = chooser.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File f = chooser.getSelectedFile();
			if (!f.getName().endsWith(".config"))
				f = new File(f.getPath() + ".config");
			ParameterSweepWizard.setLastDir(f);
			boolean save = true;
			if (f.exists()) {
				int ans = Utilities.askUser(this,false,"Override confirmation","File exists.","Do you want to override?");
				save = ans == 1;
			}
			if (save) 
				return save(f);
		}
		return true;
	}
	
	//-------------------------------------------------------------------------------------
	/** Saves the current configuration into the configuration file. */
	public boolean save(File dest) {
		final Properties clone = cloneProperties();
		properties.setProperty(PASSWORD_ENCODED,"false");
		properties.setProperty(PASSPHRASE_ENCODED,"false");
		properties.setProperty(VCLOUD_PASSWORD_ENCODED,"false");
		final String purePassword = properties.getProperty(PASSWORD);
		final String purePassphrase = properties.getProperty(PASSPHRASE);
		final String pureVCloudPassword = properties.getProperty(VCLOUD_PASSWORD);
		boolean save = true;
		if (isRemoteRun()) {
			if (properties.getProperty(PRIVATE_KEY_FILE,"").equals("")) {
				String encoded = purePassword;
				try {
					encoded = Utilities.encode(purePassword);
				} catch (Exception e) {
					int result = Utilities.askUser(this,false,"Error","Error during the password encoding.",
												   "Do you want to store it without encoding?","If you choose 'No', you must define" +
										   	   	   " the settings next time too.");
					if (result == 0)
						save = false;
				}
				properties.setProperty(PASSWORD,encoded);
				properties.setProperty(PASSWORD_ENCODED,String.valueOf(!encoded.equals(purePassword)));
				properties.setProperty(PASSPHRASE_ENCODED,"false");
			} else {
				properties.setProperty(PASSWORD_ENCODED,"false");
				String encoded = purePassphrase;
				try {
					encoded = Utilities.encode(purePassphrase);
				} catch (Exception e) {
					int result = Utilities.askUser(this,false,"Error","Error during the passphrase encoding.",
												   "Do you want to store it without encoding?","If you chooser 'No', you must define" +
												   " the settings next time too.");
					if (result == 0)
						save = false;
				}
				properties.setProperty(PASSPHRASE,encoded);
				properties.setProperty(PASSPHRASE_ENCODED,String.valueOf(!encoded.equals(purePassphrase)));
			}
		} else if (isVCloudRun()) {
			String encoded = pureVCloudPassword;
			if (!encoded.trim().isEmpty()) {
				try {
					encoded = Utilities.encode(pureVCloudPassword);
				} catch (final Exception e) {
					int result = Utilities.askUser(this,false,"Error","Error during the password encoding.","Do you want to store it without encoding?",
												   "If you choose 'No', you must define the settings next time too.");
					if (result == 0)
						save = false;
				}
				properties.setProperty(VCLOUD_PASSWORD,encoded);
				properties.setProperty(VCLOUD_PASSWORD_ENCODED,String.valueOf(!encoded.equals(pureVCloudPassword)));
			}
		}
		final StringBuilder sb = new StringBuilder();
		for (final PlatformType type : PlatformManager.getRegisteredPlatforms()) {
			if (type != PlatformType.REPAST)
				sb.append(PlatformManager.idStringForPlatform(type) + "?" + PlatformManager.getInstallationDirectory(type) + ";");
		}
		String rgStr = sb.toString();
		if (rgStr.length() > 0)
			rgStr = rgStr.substring(0,rgStr.length() - 1);
		properties.setProperty(REGISTERED_PLATFORMS,rgStr);
		if (save) {
			try {
				File newFile = dest != null ? dest : new File(configurationFileName + ".new");
				ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(newFile));
				os.writeObject(properties);
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
		}
		properties.setProperty(PASSWORD,purePassword);
		properties.setProperty(PASSPHRASE,purePassphrase);
		properties.setProperty(VCLOUD_PASSWORD,pureVCloudPassword);
		if (dest != null)
			properties = clone;
		return true;
	}
	
	//-------------------------------------------------------------------------------------
	public boolean loadPreferences() {
		JFileChooser chooser = new JFileChooser(ParameterSweepWizard.getLastDir());
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.addChoosableFileFilter(new SimpleFileFilter("Wizard configuration files (*.config)"));
		int result = chooser.showOpenDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			File f = chooser.getSelectedFile();
			ParameterSweepWizard.setLastDir(f);
			Properties tempProp = setSettingsFromFile(f);
			if (tempProp != null) {
				properties = tempProp;
				PageIterator iterator = new PageIterator();
				while (iterator.hasNext()) {
					IPreferencesPage page = iterator.next();
					if (page instanceof IReinitalizeable) {
						IReinitalizeable rPage = (IReinitalizeable) page;
						rPage.reinitialize();
					}
				}
				enableDisableButtons();
			}
			return tempProp != null;
		}
		return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void saveVCloudConnection() {
		if (vCloudConnectionProperties.getProperty(HOSTNAME,"").trim().length() > 0 &&
			vCloudConnectionProperties.getProperty(PORT,"").trim().length() > 0) {
			final File newFile = new File(vCloudFileName + ".new");
			FileWriter fileWriter = null;
			try {
				fileWriter = new FileWriter(newFile);
				vCloudConnectionProperties.store(fileWriter,"");
				final File file = new File(vCloudFileName);
				Logger.logError("vcloud-connection.cfg path: %s", file.getAbsolutePath());
				if (file.exists())
					file.delete();
				fileWriter.flush();
				fileWriter.close();
				fileWriter = null;
				newFile.renameTo(file);
			} catch (final IOException e) {
				e.printStackTrace(ParameterSweepWizard.getLogStream());
			} finally {
				if (fileWriter != null)
					try { fileWriter.close(); } catch (final IOException _) {}
			}
		}
	}
	
	//=====================================================================================
	// private methods
	
	//-------------------------------------------------------------------------------------
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
						tempProp.setProperty(PASSWORD,Utilities.decode(pwd));
				} else {
					boolean isEncoded = Boolean.parseBoolean(tempProp.getProperty(PASSPHRASE_ENCODED));
					String phrase = tempProp.getProperty(PASSPHRASE);
					if (isEncoded)
						tempProp.setProperty(PASSPHRASE,Utilities.decode(phrase));
				}
				if (!tempProp.getProperty(VCLOUD_PASSWORD,"").equals("")) {
					final boolean isEncoded = Boolean.parseBoolean(tempProp.getProperty(VCLOUD_PASSWORD_ENCODED));
					final String pwd = tempProp.getProperty(VCLOUD_PASSWORD);
					if (isEncoded)
						tempProp.setProperty(VCLOUD_PASSWORD,Utilities.decode(pwd));
				}
				String ssh_port = tempProp.getProperty(SSH_PORT);
				if (ssh_port == null)
					tempProp.setProperty(SSH_PORT,DEFAULT_SSH_PORT);
			} catch (Exception e) {
				e.printStackTrace(ParameterSweepWizard.getLogStream());
				tempProp = source != null ? null : createDefaultProperties();
			} finally {
				try { if (is != null) is.close(); } catch (IOException e) {}
			}
		} else
			tempProp = createDefaultProperties();
		return tempProp;
	}
	

	//-------------------------------------------------------------------------------------
	/** Creates and returns a Properties object from the default settings. */
	private Properties createDefaultProperties() {
		Properties p = new Properties();
		p.setProperty(SETTINGS_PATH,DEFAULT_SETTINGS_PATH);
		p.setProperty(SOURCE_GENERATION,DEFAULT_SOURCE_GENERATION);
		p.setProperty(REPASTS_SOURCE_GENERATION,DEFAULT_SOURCE_GENERATION);
		p.setProperty(REPASTS2_SOURCE_GENERATION,DEFAULT_SOURCE_GENERATION);
		p.setProperty(CUSTOM_JAVA_SOURCE_GENERATION,DEFAULT_SOURCE_GENERATION);
		p.setProperty(RNGSEED_AS_PARAMETER,DEFAULT_RNGSEED_AS_PARAMETER);
		p.setProperty(NETLOGO_RANDOM_SEED_AS_PARAMETER,DEFAULT_NETLOGO_RANDOM_SEED_AS_PARAMETER);
		p.setProperty(NETLOGO5_RANDOM_SEED_AS_PARAMETER,DEFAULT_NETLOGO_RANDOM_SEED_AS_PARAMETER);
		p.setProperty(CLOSE_AFTER_ONE_SCRIPT,DEFAULT_CLOSE_AFTER_ONE_SCRIPT);
		p.setProperty(CUSTOM_JAVA_CLOSE_AFTER_ONE_SCRIPT,DEFAULT_CLOSE_AFTER_ONE_SCRIPT);
		p.setProperty(REPASTS2_CLOSE_AFTER_ONE_SCRIPT,DEFAULT_CLOSE_AFTER_ONE_SCRIPT);
		p.setProperty(NETLOGO_CLOSE_AFTER_ONE_SCRIPT,DEFAULT_CLOSE_AFTER_ONE_SCRIPT);
		p.setProperty(NETLOGO5_CLOSE_AFTER_ONE_SCRIPT,DEFAULT_CLOSE_AFTER_ONE_SCRIPT);
		p.setProperty(HOSTNAME,DEFAULT_HOST);
		p.setProperty(PORT,DEFAULT_PORT);
		p.setProperty(USERNAME,DEFAULT_USER);
		p.setProperty(PASSWORD,DEFAULT_PASSWORD);
		p.setProperty(PRIVATE_KEY_FILE,DEFAULT_PRIVATEKEY);
		p.setProperty(PASSPHRASE,DEFAULT_PASSPHRASE);
		p.setProperty(SSH_PORT,DEFAULT_SSH_PORT);
		p.setProperty(WORKSPACE,DEFAULT_WORKSPACE);
		p.setProperty(EMAIL,DEFAULT_EMAIL);
		p.setProperty(START_MONITOR,DEFAULT_START_MONITOR);
		p.setProperty(RUN_STRATEGY,DEFAULT_RUN_STRATEGY);
		p.setProperty(SAVE_PARAMETER_TREE,DEFAULT_SAVE_PARAMETER_TREE);
		p.setProperty(SKIP_PLATFORM_PAGE,DEFAULT_SKIP_PLATFORM_PAGE);
		p.setProperty(REGISTERED_PLATFORMS,DEFAULT_REGISTERED_PLATFORMS);
		p.setProperty(NO_OF_WORKERS,DEFAULT_NO_OF_WORKERS);
		p.setProperty(VCLOUD_USERNAME,DEFAULT_VC_STRING);
		p.setProperty(VCLOUD_PASSWORD,DEFAULT_VC_STRING);
		p.setProperty(VCLOUD_RUN_OPTION,DEFAULT_VC_RUN_OPTION);
		p.setProperty(VCLOUD_NO_OF_WORKERS,DEFAULT_VC_NO_OF_WORKERS);
		return p;
	}

	//------------------------------------------------------------------------------------
	private Properties cloneProperties() {
		Properties clone = new Properties();
		for (Entry<Object,Object> entry : properties.entrySet()) {
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			clone.setProperty(key,value);
		}
		return clone;
	}
	
	//----------------------------------------------------------------------------------------------------
	private Properties setVCloudConnectionSettings() {
		File file = new File(vCloudFileName);
		try {
			Logger.logError("user.dir=%s", new File(".").getCanonicalPath());
			Logger.logError("user.dir=%s", System.getProperty("user.dir"));
			Logger.logError(vCloudFileName + "=%s", file.getAbsolutePath());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		Properties tempProp = null;
		if (file.exists()) {
			FileReader fileReader = null;
			try {
				tempProp = new Properties();
				fileReader = new FileReader(file);
				tempProp.load(fileReader);
			} catch (final IOException e) {
				e.printStackTrace(ParameterSweepWizard.getLogStream());
				tempProp = createDefaultVCloudConnectionSettings();
			} finally {
				if (fileReader != null) 
					try { fileReader.close(); } catch (final IOException _) {}
			}
		} else {
			Utilities.userAlert(this,"Missing cloud connection settings (" + vCloudFileName + ").");
			Logger.logError("File not found: %s", file.getAbsolutePath());
			tempProp = createDefaultVCloudConnectionSettings();
		}
		return tempProp;
	}
	
	//----------------------------------------------------------------------------------------------------
	private Properties createDefaultVCloudConnectionSettings() {
		final Properties p = new Properties();
		p.setProperty(HOSTNAME,DEFAULT_VC_HOST);
		p.setProperty(PORT,DEFAULT_VC_PORT);
		return p;
	}
}
