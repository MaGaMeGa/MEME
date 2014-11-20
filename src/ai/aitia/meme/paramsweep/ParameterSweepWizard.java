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
package ai.aitia.meme.paramsweep;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.prefs.Preferences;

import javassist.ClassPath;
import javassist.ClassPool;
import javassist.Loader;
import javassist.NotFoundException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.tree.DefaultMutableTreeNode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ai.aitia.meme.Logger;
import ai.aitia.meme.events.LastDirectoryChanger;
import ai.aitia.meme.gui.Wizard;
import ai.aitia.meme.paramsweep.batch.IModelInformation;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;
import ai.aitia.meme.paramsweep.classloader.RetryLoader;
import ai.aitia.meme.paramsweep.generator.WizardSettingsManager;
import ai.aitia.meme.paramsweep.gui.Page_ChooseMethod;
import ai.aitia.meme.paramsweep.gui.Page_ChoosePlatform;
import ai.aitia.meme.paramsweep.gui.Page_IntelliExtension;
import ai.aitia.meme.paramsweep.gui.Page_LoadModel;
import ai.aitia.meme.paramsweep.gui.Page_ParametersV2;
import ai.aitia.meme.paramsweep.gui.Page_Recorders;
import ai.aitia.meme.paramsweep.gui.WizardPreferences;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.internal.platform.IGUIController.RunOption;
import ai.aitia.meme.paramsweep.internal.platform.InfoConverter;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.plugin.IIntelliDynamicMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IIntelliStaticMethodPlugin;
import ai.aitia.meme.paramsweep.utils.ClassPathPair;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.pluginmanager.IPlugin;
import ai.aitia.meme.pluginmanager.IPlugin.IOnPluginLoad;
import ai.aitia.meme.pluginmanager.PSPluginManager;
import ai.aitia.meme.pluginmanager.PSPluginManager.PluginList;
import ai.aitia.meme.pluginmanager.PluginInfo;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;

/** The core class of the Parameter Sweep Wizard. This class represents the wizard itself.
 *  It is also served as the storage of the global variables. */
public class ParameterSweepWizard extends Wizard implements IPSWInformationProvider  {

	//==============================================================================
	// members
	
	private static final long serialVersionUID = 1L;
	/** The version of the Parameter Sweep Wizard and Monitor applications. */

	/** State constant: represents the state when a new model is loaded.  */
	public static final int NEW_MODEL = 0;
	/** State constant: represents the state when the third page hasn't
	 *  been initialized yet after a new model is loaded. 
	 */
	public static final int NEW_RECORDERS = 1;
	/** State constant: represents the state when all pages of the wizard is 
	 * initialized.
	 */
	public static final int OLD = 2;
	
	public static final int ERROR = 3;
	
	/** Flag that determines whether the wizard is used from the MEME or not. */
	private static boolean fromMEME = false;
	
	/** The directory of the list selected file. */
	private static File g_LastDir = null;
	/** The window that contains the wizard. */
	private static JFrame g_Frame = null; 
	/** Flag that determines whether the application is a release version (e.g. jar mode) or debug version. */
	private static boolean g_IsJARMode = false;
	/** The stream of the log file. */
	private static java.io.PrintStream g_logStream = null;
	/** The manager object that handles the plugins of the application. */
	private static PSPluginManager g_PluginManager = null;
	/** The object that handles the settings of the Parameter Sweep Wizard. */
	private static WizardPreferences g_Preferences = null;
	
	static {
		initPlatformManager();
		g_Preferences = new WizardPreferences();
		LastDirectoryChanger.add(ParameterSweepWizard.class);
		loadPlatforms();
	}
	
	private IModelInformation modelInformation = null;
	/** The parameter file object. */ 
	private File parameterFile = null;
	/** The path of the class file belongs to the model class. */
	private String modelFileName = null;
	private String modelRoot = null;
	
	/** The state of the wizard.
	 * @see #NEW_MODEL
	 * @see #NEW_RECORDERS
	 * @see #OLD
	 */
	private int modelState = OLD;
	/** Flag that determines whether the parameter file is changed or not. */
	private boolean parameterFileChanged = false;
	/** The unique name of the generated model. */
	private String generatedModelName = null;

	/** This object controls bytecode modification with Javassist. */
	private ClassPool classPool = ClassPool.getDefault();
	/** The class loader object that is used by the application to load the model-related
	 *  classes.
	 */
	private Loader classLoader = null; 
	/** Class cache. We use this because CtClass.toClass() method can be called only once,
	 *  (after that all calls throws an exception) so we must store the Class objects. The
	 *  keys are the fully qualified names of the classes.
	 */
	private HashMap<String,Class<?>> classCache = new HashMap<String,Class<?>>();
	/** The list model of the class path list. */
	private DefaultListModel cpListModel = new DefaultListModel();
	/** Platform selection page. */
	private Page_ChoosePlatform platformPage = null;
	/** Model selection page. */
	private Page_LoadModel loadPage = null;
	private Page_ChooseMethod methodChoosePage = null;
	/** Parameters page. */
	private Page_ParametersV2 parametersPage = null;
	/** Recorders page. */
	private Page_Recorders recordersPage = null;
	private Page_IntelliExtension intelliExtensionPage = null;
	
	/** The manager object that handles the save/load of the model settings. */
	private WizardSettingsManager wsManager = null;
	
	private boolean saveFlag = false;
	private String[] initClassPath = null;
	private List<String> automaticResources = null;
	
	//==============================================================================
	// methods

	//------------------------------------------------------------------------------
	/** Constructor. 
	 * @param classPath the initial class path. It can be 'null'.
	 */
	public ParameterSweepWizard(final PS_ModelWizard mw, String[] classPath) {
		this.initClassPath = classPath;
		AbstractAction abstractAction = new AbstractAction() {
			{
				putValue(Action.NAME,"Save wizard settings");
			}
			private static final long serialVersionUID = 1L;
			public void actionPerformed(ActionEvent e) {
				if (pages.get(current).onButtonPress(Button.CUSTOM)) {
					saveFlag = true;
					try {
						if (!canClose()) 
							return; 
						Utilities.userAlert(ParameterSweepWizard.this,"Save complete.");
						mw.reinitialize(null,false);
						mw.start();
					} catch (Exception e1) {
						e1.printStackTrace(ParameterSweepWizard.getLogStream());
					} finally {
						saveFlag = false;
					}
				}
			}
		};
		setCustomButton(abstractAction);
		
		try	{
			URL baseURL = ParameterSweepWizard.class.getResource("/ai/aitia/meme/paramsweep/ParameterSweepWizard.class");
		    if (baseURL.toString().startsWith("jar:")) 
		    	g_IsJARMode = true;
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
		initializePluginManager();
		platformPage = addPage(new Page_ChoosePlatform(this,mw));
		loadPage = addPage(new Page_LoadModel(this,mw));
		methodChoosePage = addPage(new Page_ChooseMethod(this));
		parametersPage = addPage(new Page_ParametersV2(this));
 		recordersPage =  addPage(new Page_Recorders(this));
 		intelliExtensionPage = addPage(new Page_IntelliExtension(this));
		gotoPage(4); //visiting the Recorders Page to have a side-effect of onPageChange
		gotoPage(5); //visiting the last page to have the arrows and the captions done on the top of the wizard 
		
		if (g_Preferences.skipPlatformPage()) {
			Preferences registry = Preferences.userNodeForPackage(ParameterSweepWizard.class);
			String platformID = registry.get("SIMULATION_PLATFORM",null);
			if (platformID == null)
				gotoPage(0);
			else {
				PlatformType type = PlatformManager.platformTypeFromString(platformID);
				platformPage.setSelectedPlatform(type);
				PlatformSettings.setSelectedPlatform(type);
				gotoPage(1); 
			}
		} else
			gotoPage(0); //going back to the start page
		setPreferredSize(new Dimension(GUIUtils.GUI_unit(42),GUIUtils.GUI_unit(46)));
		//classLoader = new RetryLoader(this);
		wsManager = new WizardSettingsManager(this,loadPage,parametersPage,recordersPage,intelliExtensionPage);
	}
	
	
	//------------------------------------------------------------------------------
	public void gotoModelselectionPage() { gotoPage(1); }
	public static boolean isFromMEME() { return fromMEME; }
	public static File getLastDir() { return g_LastDir; }
	public static JFrame getFrame() { return g_Frame; }
	public static PSPluginManager getPluginManager() { return g_PluginManager; }
	public static boolean isDebugVersion() { return !g_IsJARMode; }
	public static PrintStream getLogStream() { logError("[Log]"); return g_logStream; }
	/** Returns the object that handles the settings of the Parameter Sweep Wizard. */
	public static WizardPreferences getPreferences() { return g_Preferences; }
	public File getParameterFile() { return parameterFile; }
	public ClassPool getClassPool() { return classPool; }
	public Loader getClassLoader() { return classLoader; }
	public void createClassLoader(){
		classLoader = new RetryLoader(this);
	}
	public DefaultListModel getClassPathListModel() { return cpListModel; }
	public String getModelFileName() { return modelFileName; }
	public String getModelRoot() { return modelRoot; }
	public int getModelState() { return modelState; }
	public String getGeneratedModelName() { return generatedModelName; }
	public boolean isParameterFileChanged() { return parameterFileChanged; }
	/** Returns the parameter names of the model. */
	public String[] getInitParam() { return parametersPage.getInitParamResult(); }
	/** Returns the root of the the recorders tree. */
	public WizardSettingsManager getWSManager() { return wsManager; }
	/** Returns whether the simulation running is performed by the server application
	 *  or by this application on the local host.
	 */
	public boolean isServerMode() { return !g_Preferences.isLocalRun(); }
	/** Returns the paths of the the resource files of the model. */
	public Object[] getResources() { return loadPage.getResources(); }
	/** Returns the base directory of the model (the starting point of the
	 *  package structure.
	 */
	public String getModelDirectory() { return loadPage.getModelDirectory(); }
	/** Returns whether the recorders tree is valid or not. */
	public boolean isRecordersTreeValid() { return recordersPage.isTreeValid(); }
	/** Returns whether the user defines new parameter(s) or not. */
	public boolean hasNewParameters() { return parametersPage.getNewParametersList() != null && parametersPage.getNewParametersList().size() > 0; }
	/** Returns the information objects of the new parameters in a list. */
	public List<ParameterInfo> getNewParameters_internal() { return parametersPage.getNewParametersList(); }
	public String getSettingsFile() { return wsManager.getSettingsFile(); }
	public boolean isIntelliRun() { return getSweepingMethodID() > 0; }
	public int getSweepingMethodID() { return methodChoosePage.getSweepingMethodID(); }
	public String getSweepingMethodTitleName() {return methodChoosePage != null ? methodChoosePage.getSweepingMethodTitleName() : "";}
	public PluginList<IIntelliStaticMethodPlugin> getIntelliStaticPluginInfos() { return g_PluginManager.getIntelliStaticPluginInfos(); }
	public PluginList<IIntelliDynamicMethodPlugin> getIntelliDynamicPluginInfos() { return g_PluginManager.getIntelliDynamicPluginInfos(); }
	public IIntelliStaticMethodPlugin getIntelliStaticMethodPlugin() { return methodChoosePage.getSelectedIntelliStaticMethodPlugin(); }
	public IIntelliContext getIntelliContext() { return parametersPage.getIntelliContext(); }
	public IIntelliDynamicMethodPlugin getIntelliDynamicMethodPlugin() { return methodChoosePage.getSelectedIntelliDynamicMethodPlugin(); }
	public Page_Recorders getRecordersPage() { return recordersPage; }

	//----------------------------------------------------------------------------------------------------
	/**	Returns a <code>Vector</code> of <code>PluginInfo</code>s that contain all IntelliSweep static method plugins. */
	public Vector<PluginInfo<IIntelliStaticMethodPlugin>> getIntelliStaticPluginVector() { 
		PluginList<IIntelliStaticMethodPlugin> list1 = g_PluginManager.getIntelliStaticPluginInfos();
		Vector<PluginInfo<IIntelliStaticMethodPlugin>> ret = new Vector<PluginInfo<IIntelliStaticMethodPlugin>>(list1.size());
		for (PluginInfo<IIntelliStaticMethodPlugin> info : list1) {
	        ret.add(info);
        }
		return ret;
	}
	
	//----------------------------------------------------------------------------------------------------
	/**	Returns a <code>Vector</code> of <code>PluginInfo</code>s that contain all IntelliSweep dynamic method plugins. */
	public Vector<PluginInfo<IIntelliDynamicMethodPlugin>> getIntelliDynamicPluginVector() {
		PluginList<IIntelliDynamicMethodPlugin> list1 = g_PluginManager.getIntelliDynamicPluginInfos();
		Vector<PluginInfo<IIntelliDynamicMethodPlugin>> ret = new Vector<PluginInfo<IIntelliDynamicMethodPlugin>>(list1.size());
		for (PluginInfo<IIntelliDynamicMethodPlugin> info : list1) {
	        ret.add(info);
        }
		return ret;
	}
	
    //------------------------------------------------------------------------------
	public static void setFromMEME(boolean fromMEME) { ParameterSweepWizard.fromMEME = fromMEME; }
	public static void setFrame(JFrame g_Frame) { ParameterSweepWizard.g_Frame = g_Frame; }
	public static void setPreferences(WizardPreferences g_Preferences) { ParameterSweepWizard.g_Preferences = g_Preferences; }
	public void setParameterFile(File file) { parameterFile = file; }
	public void setModelFileName(String modelFileName) { this.modelFileName = modelFileName; }
	public void setModelRoot(String modelRoot) { this.modelRoot = modelRoot; }
	public void setParameterFileChanged(boolean parameterFileChanged) { this.parameterFileChanged = parameterFileChanged; }
	/** Resets the three lists (variables/methods/scripts) of recordable elements on the
	 *  third page of the wizard.
	 * @param illegalParameters information objects of the parameters that becomes illegal
	 *        because the user overrides the group of new parameters.
	 */
	public void reinitializeRecordableLists(List<ParameterInfo> illegalParameters) { 
		recordersPage.initializeRecordableLists(illegalParameters);
		recordersPage.initalizeRecorders();
		
	}
	/** Deletes from the recorders tree the illegal scripts. */
	public void cleanRecorders() { recordersPage.cleanRecorders(); }
	
	//-------------------------------------------------------------------------------
	public void setModelState(int modelChanged) {
		if (modelChanged < NEW_MODEL || modelChanged > ERROR)
			throw new IllegalArgumentException();
		this.modelState = modelChanged;
	}
	
	//------------------------------------------------------------------------------
	public static void setLastDir(File dir) {
		setLastDir_(dir);
		LastDirectoryChanger.fireDirectoryChanged(dir,ParameterSweepWizard.class);
	}
	
    //----------------------------------------------------------------------------------------------------
	public static void setLastDir_(File dir) {
		if (dir.isDirectory())
			g_LastDir = dir;
		else {
			File parent = dir.getParentFile();
			if (parent != null) 
				g_LastDir = parent;
		}
    }
    
    //----------------------------------------------------------------------------------------------------
	public IModelInformation getModelInformation() {
		if (modelInformation == null)
			modelInformation = PlatformManager.getPlatform(PlatformSettings.getPlatformType()).getModelInformation(this);
		return modelInformation;
	}
    
    //-------------------------------------------------------------------------------
	/** Writes the error specified by <code>format</code> and <code>args</code>
	 *  to the log file.
	 * @param format the format string of the error message
	 * @param args the parameters of the format string
	 */  
    public static void logError(String format, Object ... args) {
    	String pfx = String.format("%1$tT.%1$tL\t", System.currentTimeMillis()); 
    	if (g_logStream == null) {
			g_logStream = System.err;
			try {
    			g_logStream = new java.io.PrintStream("PSW.log");
	    		g_logStream.println(String.format(
	    				"%tF %sLog file created", System.currentTimeMillis(), pfx
	    		));
	    		if (isDebugVersion()) g_logStream = System.err;
			} catch (Exception e) {
				logError("Cannot create log file " + Util.getLocalizedMessage(e));
			}
    	}
    	g_logStream.println(pfx + Utils.formatv(format, args));
    }
    
    //------------------------------------------------------------------------------
    /** Sets the log stream and the frame global variables. Used only by MEME to bind
     *  the appropriate global variables.
     */
    public static void init(PrintStream g_logStream, JFrame g_Frame) {
    	ParameterSweepWizard.g_logStream = g_logStream;
    	ParameterSweepWizard.g_Frame = g_Frame;
    }
	
	//------------------------------------------------------------------------------
	/** This method performs the parameter file generation and the model generation
	 *  (if need) after the user presses the Finish button. It also saves the model
	 *  settings.
	 */
	@Override
	public boolean canClose() {
		if (isCanceling()) { 
			return true;
		} else {
			if (!lastCheck()) return false;
			if (!prepare()) return false;
			if (generatedModelName == null) {
				File modelFile = new File(modelFileName);
				int idx = modelFile.getName().lastIndexOf(File.separatorChar);
				String prefix = idx > 0 ? modelFile.getName().substring(idx + 1) : modelFile.getName();
				generatedModelName = prefix + "__" + Util.getTimeStamp();
			}
			try {
				wsManager.save();
			} catch (FileNotFoundException e) {
				Utilities.userAlert(this,"Error while saving the wizard settings.","Wizard settings folder",g_Preferences.getSettingsPath(),"cannot accessible or does not exist.");
			} catch (Exception e) {
				Utilities.userAlert(this,"Error while saving the wizard settings.","See error log for details.");
				e.printStackTrace(getLogStream());
			}
		}
		return true;
	}
	
	//-------------------------------------------------------------------------------
	/** Releases the members of the class. */
	public void dispose() {
		parameterFile = null;
		modelFileName = null;
		modelRoot = null;
		generatedModelName = null;
		classLoader = null;
		classCache.clear();
		classCache = null;
		platformPage = null;
		loadPage = null;
		parametersPage = null;
		recordersPage = null;
		intelliExtensionPage = null;
	}

	//-------------------------------------------------------------------------------
	/** Writes the current classpath to the registry (it uses java.util.prefs.Preferences
	 *  to access the reqistry of the Windows. On other platforms the classpath is stored
	 *  in other format (it depends on the implementation of the Preferences.
	 */
	public void writeClassPathToRegistry() {
		String classPath = "";
		Object[] cpArray = cpListModel.toArray();
		if (cpArray.length == 0) return;
		for (int i = cpArray.length - 1;i >= 0;--i) {
			classPath += cpArray[i].toString() + ";";
		}
		try {
			Preferences registry = Preferences.userNodeForPackage(ParameterSweepWizard.class);
			registry.put(PlatformManager.idStringForPlatform(PlatformSettings.getPlatformType()) + "_CLASSPATH",
					classPath.substring(0,classPath.length() - 1));
		} catch (IllegalArgumentException e){
			Logger.logException(e);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void writeUsedPlatformToRegistry() {
		if (g_Preferences.skipPlatformPage()) {
			Preferences registry = Preferences.userNodeForPackage(ParameterSweepWizard.class);
			registry.put("SIMULATION_PLATFORM",PlatformManager.idStringForPlatform(PlatformSettings.getPlatformType()));
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Saves the class-related model settings to the XML node <code>node</code>. This
	 *  method is part of the model settings storing performed by {@link ai.aitia.meme.paramsweep.generator.WizardSettingsManager 
	 *  WizardSettingsManager}.
	 */
	public void save(Node node) { 
		Document document = node.getOwnerDocument();
		
		Element root = (Element) node;
		root.setAttribute(WizardSettingsManager.SIMULATION_PLATFORM,PlatformManager.idStringForPlatform(PlatformSettings.getPlatformType()));
		
		Element element = document.createElement(WizardSettingsManager.MODEL_FILE);
		element.appendChild(document.createTextNode(modelFileName));
		node.appendChild(element);
		
		element = document.createElement(WizardSettingsManager.MODEL_ROOT);
		element.appendChild(document.createTextNode(modelRoot));
		node.appendChild(element);
		
		element = document.createElement(WizardSettingsManager.GENERATED_MODEL);
		element.appendChild(document.createTextNode(generatedModelName));
		node.appendChild(element);
		
		if (parameterFile != null) {
			element = document.createElement(WizardSettingsManager.PARAMETER_FILE);
			element.appendChild(document.createTextNode(parameterFile.getPath()));
			node.appendChild(element);
		}
		
		if (cpListModel.size() != 0 && PlatformSettings.getGUIControllerForPlatform().isClassPathEnabled()) {
			Element cp = document.createElement(WizardSettingsManager.CLASSPATH);
			Object[] cpArray = cpListModel.toArray();
			for (int i = cpArray.length - 1;i >= 0;--i) {
				element = document.createElement(WizardSettingsManager.CLASSPATH_ELEMENT);
				element.appendChild(document.createTextNode(cpArray[i].toString()));
				cp.appendChild(element);
			}
			node.appendChild(cp);
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Loads the class-related model settings from the XML element <code>root</code>. This
	 *  method is part of the model settings retrieving performed by {@link ai.aitia.meme.paramsweep.generator.WizardSettingsManager 
	 *  WizardSettingsManager}.
	 * @throws WizardLoadingException if the XML document is invalid
	 * @throws IOException if the model class file referenced by the XML document isn't found 
	 */
	public void load(Element root) throws WizardLoadingException, IOException { 
		String platformID = root.getAttribute(WizardSettingsManager.SIMULATION_PLATFORM);
		PlatformType platform = PlatformManager.platformTypeFromString(platformID);
		platformPage.setSelectedPlatform(platform);
		PlatformSettings.setSelectedPlatform(platform);
		writeUsedPlatformToRegistry();
		
		NodeList nl = root.getElementsByTagName(WizardSettingsManager.PARAMETER_FILE);
		if  (nl != null && nl.getLength() > 0) {
			Element parameterFileElement = (Element) nl.item(0);
			if (parameterFileElement.getParentNode().equals(root)) {
				NodeList content = parameterFileElement.getChildNodes();
				if (content == null || content.getLength() == 0)
						throw new WizardLoadingException(false,"missing content at node: " + WizardSettingsManager.PARAMETER_FILE);
				String parameterFileName = ((Text)content.item(0)).getNodeValue();
				parameterFile = new File(parameterFileName);
				parameterFileChanged = false;
			}
		}

//		nl = null;
//		nl = root.getElementsByTagName(WizardSettingsManager.GENERATED_MODEL);
//		if (nl == null || nl.getLength() == 0)
//			throw new WizardLoadingException(false,"missing node: " + WizardSettingsManager.GENERATED_MODEL);
//		Element generatedNameElement = (Element) nl.item(0);
//		NodeList content = generatedNameElement.getChildNodes();
//		if (content == null || content.getLength() == 0)
//			throw new WizardLoadingException(false,"missing content at node: " + WizardSettingsManager.GENERATED_MODEL);
//		generatedModelName = ((Text)content.item(0)).getNodeValue();
		
		clearClassPath();
		nl = null;
		nl = root.getElementsByTagName(WizardSettingsManager.CLASSPATH);
		if (nl != null && nl.getLength() != 0) {
			Element cp = (Element) nl.item(0);
			recreateClassPath(cp);
		}

		nl = null;
		nl = root.getElementsByTagName(WizardSettingsManager.MODEL_FILE);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.MODEL_FILE);
		Element modelFileNameElement = (Element) nl.item(0);
		NodeList content = null;
		content = modelFileNameElement.getChildNodes();
		if (content == null || content.getLength() == 0)
			throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.MODEL_FILE);
		modelFileName = ((Text)content.item(0)).getNodeValue();
		
		nl = null;
		nl = root.getElementsByTagName(WizardSettingsManager.MODEL_ROOT);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.MODEL_ROOT);
		Element modelRootElement = (Element) nl.item(0);
		content = null;
		content = modelRootElement.getChildNodes();
		if (content == null || content.getLength() == 0)
			throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.MODEL_ROOT);
		modelRoot = ((Text)content.item(0)).getNodeValue();
		modelState = OLD;
		
		File modelFile = new File(modelFileName);
		loadPage.setModelFile(modelFile);
		
		String error = PlatformManager.getPlatform(PlatformSettings.getPlatformType()).checkModel(this);
		if (error != null)
			throw new WizardLoadingException(true,"cannot load model: " + error);
	}
	
	//--------------------------------------------------------------------------------
	/** Returns the names of the output files of the recorders in an array. */
	public String[] getOutputFiles() {
		List<RecorderInfo> recorders = getRecorders();
		List<String> result = new ArrayList<String>(recorders.size());
		try {
			for (RecorderInfo info : recorders)
				result.add(info.getOutputFile().getCanonicalPath());
		} catch (IOException e) {
			e.printStackTrace(g_logStream);
		}
		return result.toArray(new String[0]);
	}
	
	//----------------------------------------------------------------------------------------------------
	public ParameterTree getPublicParameterTree() { return InfoConverter.node2ParameterTree(parametersPage.createTreeFromParameterPage()); }
	
	//-------------------------------------------------------------------------------
	/** Initializes the classpath (and the list model that displays that) from the
	 *  registry (java.util.prefs.Preferences) OR from the initialClasspath.
	 */
	public void initializeClassPathList() {
		cpListModel.clear();
		if (initClassPath == null) {
			Preferences registry = Preferences.userNodeForPackage(ParameterSweepWizard.class);
			if (PlatformSettings.getPlatformType() == null) return;
			String stored_cp = registry.get(PlatformManager.idStringForPlatform(PlatformSettings.getPlatformType()) + "_CLASSPATH","");
			if (!stored_cp.equals("")) {
				String[] pathElements = stored_cp.split(";");
				for (String s : pathElements) {
					try {
						// check if it is already in the list, use a template for it
						if (cpListModel.contains(new ClassPathPair(s,null))) continue;	 
						ClassPath cp = classPool.insertClassPath(s);
						cpListModel.add(0,new ClassPathPair(s,cp));
					} catch (NotFoundException e) {
						logError("Cannot find this path: %s",s);
						continue;
					}
				}
			}
		} else {
			for (int i = initClassPath.length - 1;i >= 0;--i) {
				try {
					// check if it is already in the list, use a template for it
					if (cpListModel.contains(new ClassPathPair(initClassPath[i],null))) continue;	 
					ClassPath cp = classPool.insertClassPath(initClassPath[i]);
					cpListModel.add(0,new ClassPathPair(initClassPath[i],cp));
				} catch (NotFoundException e) {
					logError("Cannot find this path: %s",initClassPath[i]);
					continue;
				}
			}
		}
	}
	
	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public String getEntryPoint() { return modelFileName; }
	public ClassLoader getCustomClassLoader() { return classLoader; }
	public Map<String,Class<?>> getClassCache() { return classCache; }
	public String getDescription() { return loadPage.getDescription().trim(); }
	public File getModelFile() { return loadPage.getModelFile(); }
	public boolean isNumberStoppingCondition() { return recordersPage.isStopAfterFixInterval(); }
	public boolean rngSeedAsParameter() { return g_Preferences.rngSeedAsParameter(); }
	public void setGeneratedModelName(String generatedModelName) { this.generatedModelName = generatedModelName; }
	public boolean writeSource() { return g_Preferences.needSource(); }
	public boolean isLocalRun() { return g_Preferences.isLocalRun(); }
	public void setAutomaticResources(List<String> resources) { this.automaticResources = resources; }
	public List<String> getAutomaticResources() { return automaticResources; }
	
	//----------------------------------------------------------------------------------------------------
	public String getStoppingCondition() { 
		return InfoConverter.getPlatformSpecificStoppingCondition(recordersPage.getStopData(),!isNumberStoppingCondition());
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<AbstractParameterInfo> getNewParameters() {
		List<ParameterInfo> newParameters = getNewParameters_internal();
		if (newParameters == null)
			return null;
		List<AbstractParameterInfo> result = new ArrayList<AbstractParameterInfo>(newParameters.size());
		for (ParameterInfo info : newParameters)
			result.add(InfoConverter.parameterInfo2ParameterInfo(info));
		return result;
	}

	//----------------------------------------------------------------------------------------------------
	public List<RecorderInfo> getRecorders() { return InfoConverter.convertRecorderTree2List(recordersPage.getRecorders()); }
	
	//----------------------------------------------------------------------------------------------------
	
	public DefaultMutableTreeNode getRecorderTreeRoot() { return recordersPage.getRecorders(); }

	//----------------------------------------------------------------------------------------------------
	public void setSelectedSweepingMethodByLocalizedName(String pluginName) {
		methodChoosePage.setSelectedMethodByLocalizedName(pluginName);
    }

	//----------------------------------------------------------------------------------------------------
	public DefaultMutableTreeNode getParameterTreeRoot() {
		return parametersPage.createTreeFromParameterPage();
	}

	//----------------------------------------------------------------------------------------------------
	public List<ParameterInfo> getOriginalParameterInfos() {
		return parametersPage.getParameters();
	}

	//===============================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private boolean lastCheck() {
		String problemName = "";
//		if ((problemName = parametersPage.checkConstants()) != null) {
//			Utilities.userAlert(this,"Invalid parameter tree: " + problemName + " is a constant parameter.","It cannot have children.");
//			gotoPage(3);
//			return false;
//		}
		if (!isIntelliRun() && parametersPage.needWarning()) {
			int result = Utilities.askUser(this,false,"Warning","There are two or more non-constant parameters on the top level of the parameter" +
										   " tree. Parameters","are unsynchronized: simulation may exit before all parameter values are assigned.",
										   " ","To explore all possible combinations you must create only one branch in the tree from the " +
										   "non-constant","parameters."," ","Do you want to " + (saveFlag ? "save " : "run ") + "simulation with " +
										   "these parameter settings?"); 
			if (result == 0) {
				gotoPage(3);
				return false;
			}
		}
		return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean prepare() {
		final DefaultMutableTreeNode parameterTreeRoot = parametersPage.createTreeFromParameterPage();
		
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.GLOBAL)
			parametersPage.setGlobalRuns(parameterTreeRoot);
		parametersPage.transformIncrementsIfNeed(parameterTreeRoot);
		
		if (g_Preferences.saveParameterTree()) {
			// generating parameter file
			File f = getParameterFile();
			if (f == null) {
				String path = PlatformSettings.generateParameterFilePath(this);
				f = new File(path);
			}
			parameterFile = f;
			try {
				PlatformSettings.generateParameterFile(f,parameterTreeRoot);
			} catch (IOException e) {
				e.printStackTrace(getLogStream());
				Utilities.userAlert(this,"Error while generating the parameter file: " + Util.getLocalizedMessage(e));
			}
		}
		String error = PlatformManager.getPlatform(PlatformSettings.getPlatformType()).prepareModel(this);
		
		if (error != null) {
			Utilities.userAlert(this,error);
			return false;
		}
		return true;
	}
	
	//-------------------------------------------------------------------------------
	/** Creates the plugin manager object <code>g_PluginManager</code> that reads
	 *  the available plugins from the Plugins directory.
	 */
	private void initializePluginManager() {
		g_PluginManager = new PSPluginManager();
		try {
			g_PluginManager.setPluginsDir(new File(".","Plugins").getCanonicalPath()); 
			g_PluginManager.scanPlugins(new DefaultOnPluginLoadContext());
		} catch (Exception e) { logError(Util.getLocalizedMessage(e)); }
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void initPlatformManager() {
		try {
			PlatformManager.setPlatformPluginsDir(new File(".","PlatformPlugins").getCanonicalPath());
			String[] errs = PlatformManager.scanPlatformPlugins(new IOnPluginLoad() {});
			if (errs.length > 0) {
				for (String err : errs) 
					logError("Warning: " + err);
			}
		} catch (Exception e) {
			logError("Error while initializing the platform manager: " + Util.getLocalizedMessage(e));
			e.printStackTrace(getLogStream());
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Deletes all entries from the classpath (and the list model that displays that). */
	private void clearClassPath() {
		// delete
		for (int i = 0;i < cpListModel.size();++i) {
			ClassPathPair pair = (ClassPathPair) cpListModel.get(i);
			classPool.removeClassPath(pair.getClassPath());
		}
		cpListModel.clear();
	}
	
	//-------------------------------------------------------------------------------
	/** Extends the classpath (and the list model that displays that) from the
	 *  XML element <code>cp</code>.
	 * @throws WizardLoadingException if the XML document is invalid
	 */
	private void recreateClassPath(Element cp) throws WizardLoadingException {
		NodeList nl = cp.getChildNodes();
		if (nl == null) return;
		for (int i = 0;i < nl.getLength();++i) {
			Node node = nl.item(i);
			NodeList content = node.getChildNodes();
			if (content == null || content.getLength() == 0) continue;
			String cpElement = ((Text)content.item(0)).getNodeValue();
			try {
				ClassPath classpath = classPool.insertClassPath(cpElement);
				cpListModel.add(0,new ClassPathPair(cpElement,classpath));
			} catch (NotFoundException e) {
				logError("Cannot find this path: %s",cpElement);
				continue;
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	void simulateBrowsePressed(String modelFileName) {
		PlatformSettings.setSelectedPlatform(PlatformType.REPAST);
		writeUsedPlatformToRegistry();
		gotoPage(1);
		loadPage.simulateBrowsePressed(modelFileName);
	}
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * Loads previously registered platforms.
	 */
	private static void loadPlatforms(){
		String str = g_Preferences.getProperty(WizardPreferences.REGISTERED_PLATFORMS);
		if (str != null) {
			String[] platforms = str.split(";");
			for (String platform : platforms) {
				String[] parts = platform.split("\\?");
				PlatformType type = PlatformManager.platformTypeFromString(parts[0]);
				if (type != null) 
					PlatformManager.registerPlatform(type,parts[1]);
			}
			Map<PlatformType,String> invalidPlatforms = PlatformManager.checkRegisteredPlatform();
			if (invalidPlatforms.keySet().size() > 0) {
				for (PlatformType type : invalidPlatforms.keySet()) 
					logError("Invalid platform: %s (%s)",type.toString(),invalidPlatforms.get(type));
				g_Preferences.save(null);
			}
		}
	}
	
	//===============================================================================
	// nested classes
	
	//-------------------------------------------------------------------------
	/** Default (empty) implementation of the interface IPlugin.IOnPluginLoad. */
	static class DefaultOnPluginLoadContext implements IPlugin.IOnPluginLoad {
        private static final long serialVersionUID = -5415975361104256687L;
    }
}
