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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import javassist.ClassPath;
import javassist.NotFoundException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.gui.SimpleFileFilter;
import ai.aitia.meme.gui.Wizard;
import ai.aitia.meme.gui.Wizard.Button;
import ai.aitia.meme.gui.Wizard.IArrowsInHeader;
import ai.aitia.meme.gui.Wizard.IWizardPage;
import ai.aitia.meme.paramsweep.PS_ModelWizard;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.generator.WizardSettingsManager;
import ai.aitia.meme.paramsweep.internal.platform.IGUIController;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.internal.platform.IGUIController.ModelDefinition;
import ai.aitia.meme.paramsweep.platform.Platform;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.plugin.IIntelliDynamicMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IIntelliStaticMethodPlugin;
import ai.aitia.meme.paramsweep.utils.ClassPathFileFilter;
import ai.aitia.meme.paramsweep.utils.ClassPathPair;
import ai.aitia.meme.paramsweep.utils.SettingsFileFilter;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.pluginmanager.PluginInfo;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;

import com.jgoodies.forms.layout.CellConstraints;

/** This class provides the Model selection page of the Parameter Sweep Wizard. */
public class Page_LoadModel extends JPanel implements IWizardPage,
													  IArrowsInHeader,
													  ActionListener {
	
	//===============================================================================
	// members

	private static final long serialVersionUID = 1L;
	/** The owner of the page. */
	private ParameterSweepWizard owner = null;
	
	/** The main entry point of the wizard that contained this page. */
	private PS_ModelWizard mEntry = null;
	
	/** The file object of the selected model class file. */
	private File modelFile = null;
	/** Reference to the Resources dialog. */ 
	private ResourcesDialog resourcesDialog = null;
	
	///==============================================================================
	// GUI members
	
	private JPanel content = null;
	private JLabel modelDirectoryLabel = new JLabel("Model directory: ");
	private JTextField modelDirectoryField = new JTextField();
	private JButton dBrowseButton = new JButton("Browse...");
	private JLabel modelFileLabel = new JLabel("Model file: ");
	private JTextField modelFileField = new JTextField();
	private JButton browseButton = new JButton("Browse...");
	private JLabel parameterFileLabel = new JLabel("Parameter file (optional): ");
	private JTextField parameterFileField = new JTextField();
	private JButton pBrowseButton = new JButton("Browse...");
	private JLabel classPathLabel = new JLabel("Class path: ");
	private JList classPathList = new JList();
	private JScrollPane classPathScr = new JScrollPane(classPathList);
	private JButton addButton = new JButton("Add...");
	private JButton removeButton = new JButton("Remove");
	private JButton moveUpButton = new JButton("Move up");
	private JButton moveDownButton = new JButton("Move down");
	private JButton topButton = new JButton("Top");
	private JButton bottomButton = new JButton("Bottom");
	private JButton loadSettingsButton = new JButton("Load wizard settings...");
	private JButton resourcesButton = new JButton("Resources...");
	private JButton preferencesButton = new JButton("Preferences...");
	private JButton aboutButton = new JButton("About...");
	private JPanel buttonsPanel = null;
	private JTextPane descriptionArea = new JTextPane();
	private JScrollPane descriptionScr = new JScrollPane(descriptionArea);
	
	//===============================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the owner of the page
	 */
	public Page_LoadModel(ParameterSweepWizard owner, PS_ModelWizard mEntry) {
		this.owner = owner;
		this.mEntry = mEntry;
		classPathList.setModel(owner.getClassPathListModel());
		layoutGUI();
	}
	
	//-------------------------------------------------------------------------------
	/** Returns the description of the model. */
	public String getDescription() { return descriptionArea.getText(); }
	/** Returns the paths of the resources of the model in an Object array. */
	public Object[] getResources() { return (resourcesDialog == null) ? new Object[0] : resourcesDialog.getResources(); }
	/** Returns the path of the base directory of the model.
	 * @see ResourcesDialog#getModelDirectory()
	 */
	public String getModelDirectory() { return (resourcesDialog == null) ? null : resourcesDialog.getModelDirectory(); }
	public File getModelFile() { return modelFile; }
	public void setModelFile(File modelFile) { this.modelFile = modelFile; } 

	//-------------------------------------------------------------------------------
	/** Saves the page-related model settings to the XML node <code>node</code>. This
	 *  method is part of the model settings storing performed by {@link ai.aitia.meme.paramsweep.generator.WizardSettingsManager 
	 *  WizardSettingsManager}.
	 */
	public void save(Node node) { 
		if (node instanceof Element) {
			Document document = node.getOwnerDocument();
			Element element = (Element) node;
			element = document.createElement(WizardSettingsManager.DESCRIPTION);
			element.appendChild(document.createTextNode(descriptionArea.getText()));
			node.appendChild(element);
			element = document.createElement(WizardSettingsManager.RESOURCES);
			Object[] resources = getResources();
			for (int i = 0;i < resources.length;++i) {
				Element re = document.createElement(WizardSettingsManager.RESOURCE_ELEMENT);
				re.appendChild(document.createTextNode(resources[i].toString()));
				element.appendChild(re);
			}
			if (owner.getAutomaticResources() != null) {
				for (String ar : owner.getAutomaticResources()) {
					Element re = document.createElement(WizardSettingsManager.AUTO_RESOURCE_ELEMENT);
					re.appendChild(document.createTextNode(ar));
					element.appendChild(re);
				}
			}
			node.appendChild(element);
		} else throw new IllegalStateException();
	}
	
	//-------------------------------------------------------------------------------
	/** Loads the page-related model settings from the XML element <code>element</code>. This
	 *  method is part of the model settings retrieving performed by {@link ai.aitia.meme.paramsweep.generator.WizardSettingsManager 
	 *  WizardSettingsManager}.<br>
	 *  Pre-condition: <code>owner.load()</code> has finished
	 * @throws WizardLoadingException if the XML document is invalid
	 */
	public void load(Element element) throws WizardLoadingException {
		NodeList nl = element.getElementsByTagName(WizardSettingsManager.DESCRIPTION);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.DESCRIPTION);
		Element descElement = (Element) nl.item(0);
		nl = null;
		nl = descElement.getChildNodes();
		String description = nl.getLength() == 0 ? "" : ((Text)nl.item(0)).getNodeValue();
		descriptionArea.setText(description);
		
		List<String> resources = new ArrayList<String>();
		nl = null;
		nl = element.getElementsByTagName(WizardSettingsManager.RESOURCES);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.RESOURCES);
		Element resourcesElement = (Element) nl.item(0);
		nl = null;
		nl = resourcesElement.getElementsByTagName(WizardSettingsManager.RESOURCE_ELEMENT);
		if (nl != null) {
			for (int i = 0;i < nl.getLength();++i) {
				Node node = nl.item(i);
				NodeList content = node.getChildNodes();
				if (content == null || content.getLength() == 0) continue;
				String resElement = ((Text)content.item(0)).getNodeValue();
				resources.add(resElement);
			}
		}
		
		List<String> autoResources = new ArrayList<String>();
		nl = null;
		nl = resourcesElement.getElementsByTagName(WizardSettingsManager.AUTO_RESOURCE_ELEMENT);
		if (nl != null) {
			for (int i = 0;i < nl.getLength();++i) {
				Node node = nl.item(i);
				NodeList content = node.getChildNodes();
				if (content == null || content.getLength() == 0) continue;
				String resElement = ((Text)content.item(0)).getNodeValue();
				autoResources.add(resElement);
			}
		}
		owner.setAutomaticResources(autoResources);
		
		modelDirectoryField.setText(owner.getModelRoot());
		modelFileField.setText(owner.getModelFileName());
		parameterFileField.setText(owner.getParameterFile() == null ? "" : owner.getParameterFile().getPath());

		if (owner.isServerMode()) {
			resourcesDialog = new ResourcesDialog(ParameterSweepWizard.getFrame(),owner.getModelRoot(),resources.toArray(new String[0]));
			resourcesButton.setEnabled(PlatformSettings.getGUIControllerForPlatform().isResourcesEnabled());
		}
	}
	
	//===============================================================================
	// interface implementations
	
	//-------------------------------------------------------------------------------
	public String getTitle() { return "Model selection"; }
	public String getInfoText(Wizard w) { return w.getArrowsHeader("Select model (and optional parameter file) for batch run"); }
	public Container getPanel() { return this; }
	public boolean isEnabled(Button b) { return PlatformSettings.isEnabledForPageLoadModel(owner,b); }

	//-------------------------------------------------------------------------------
	public boolean onButtonPress(Button b) {  
		if (b == Button.NEXT) {
			String text = parameterFileField.getText().trim();
			if  (text.equals("")) {
				owner.setParameterFile(null);
				return isEnabled(b);
			}
			File file = new File(text);
			if (!file.exists() && file.getParentFile() != null && !file.getParentFile().exists()) {
				Utilities.userAlert(owner,"The path of " + file.getName() + " does not exists.");
				parameterFileField.grabFocus();
				return false;
			}
			File old = owner.getParameterFile();
			owner.setParameterFile(file);
			if (!file.equals(old))
				owner.setParameterFileChanged(true);
			parameterFileField.setToolTipText(file.getPath());
		}
		return isEnabled(b);
	}

	//-------------------------------------------------------------------------------
	public void onPageChange(boolean show) { 
		if (show) {
			owner.createClassLoader();
			initializePageForPlatform();
			if (owner.getModelFileName() == null) {
				modelFileField.setText("");
				modelFileField.setToolTipText("");
			}
			if (owner.getModelRoot() == null) {
				modelDirectoryField.setText("");
				modelDirectoryField.setToolTipText("");
			}
		}
	}

	//-------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) { 
		String command = e.getActionCommand();
		if (command.equals("BROWSE_PARAM")) {
			File f = fileDialog(false,owner.getParameterFile(),null);
			if (f == null) return;
			while (f.exists()) {
				int result = Utilities.askUser(this,false,"Override comfirmation",
											   String.format("%s exists. The wizard may change its" +
											   				" content.",f.getName()),"Are you sure?");
				if (result == 0) {
					f = fileDialog(false,f,null);
					if (f == null) return;
				} else break;
			}
			owner.setParameterFile(f);
			owner.setParameterFileChanged(true);
			parameterFileField.setText(f.getAbsolutePath());
			parameterFileField.setToolTipText(f.getAbsolutePath());
		} else if (command.equals("BROWSE_DIR")) {
			File[] directory = fileDialog(true,null,JFileChooser.DIRECTORIES_ONLY,false);
			if (directory == null || !directory[0].isDirectory()) return;
			changeModelDirectory(directory[0]);
		} else if (command.equals("BROWSE_MODEL")) {
			File previous = modelFile;
			if (previous == null && owner.getModelRoot() != null)
				previous = new File(owner.getModelRoot());
			File f = fileDialog(true,previous,PlatformSettings.getGUIControllerForPlatform().getModelFileFilter());
			if (f == null) return;
			changeModelName(f);
		} else if (command.equals("DIR_FIELD")) {
			String text = modelDirectoryField.getText().trim();
			if (text.equals("")) return;
			File dir = new File(text);
			if (!dir.exists()) {
				Utilities.userAlert(owner,dir.getName() + " does not exists.");
				resetResources();
				modelDirectoryField.grabFocus();
				return;
			} else if (!dir.isDirectory()) {
				Utilities.userAlert(owner,dir.getName() + " is not a directory.");
				resetResources();
				modelDirectoryField.grabFocus();
				return;
			} else 
				changeModelDirectory(dir);
		} else if (command.equals("MODEL_FIELD")) {
			String text = modelFileField.getText().trim();
			if  (text.equals("")) return;
			File file = new File(text);
			if (!file.exists()) {
				Utilities.userAlert(owner,file.getName() + " does not exists.");
				resetResources();
				modelFileField.grabFocus();
				return;
			} else if (!text.endsWith(".class")) {
				Utilities.userAlert(owner,file.getName() + " is not a Java class file.");
				resetResources();
				modelFileField.grabFocus();
				return;
			} else 
				changeModelName(file);
		} else if (command.equals("ADD_CP")) {
			File[] files = fileDialog(true,new ClassPathFileFilter(),JFileChooser.FILES_AND_DIRECTORIES,true);
			if (files == null) return;
			try {
				for (File f : files) {
					ClassPathPair pair = new ClassPathPair(f.getAbsolutePath(),null);
					if (owner.getClassPathListModel().contains(pair)) continue;
					ClassPath cp = owner.getClassPool().insertClassPath(f.getAbsolutePath());
					pair.setClassPath(cp);
					owner.getClassPathListModel().add(0,pair);
				}
			} catch (NotFoundException e1) {}
		} else if (command.equals("REMOVE_CP")) {
			Object[] objects = classPathList.getSelectedValues();
			for (Object o : objects) {
				ClassPathPair pair = (ClassPathPair) o;
				owner.getClassPool().removeClassPath(pair.getClassPath());
				owner.getClassPathListModel().removeElement(pair);
			}
		} else if (command.equals("UP")) {
			move(-1);
			updateClassPath();
		} else if (command.equals("DOWN")) {
			move(1);
			updateClassPath();
		} else if ("TOP".equals(command)) {
			moveTop();
			updateClassPath();
		} else if ("BOTTOM".equals(command)) {
			moveBottom();
			updateClassPath();
		} else if (command.equals("SETTINGS")) { 
			String settingsPath = ParameterSweepWizard.getPreferences().getSettingsPath();
			if (settingsPath == null) {
				MEMEApp.logException("No settings path in preferences: "+settingsPath, new Exception("Preferences not found"));
				settingsPath = System.getProperty("user.home");
			}
			File f = fileDialog(true,new File(settingsPath),new SettingsFileFilter());
			if (f == null) return;
			try {
				invalidateIntelliSweepPlugins();
				owner.getWSManager().load(f.toURI());
			} catch (WizardLoadingException e1) {
				Utilities.userAlert(owner,"Error while loading the settings file.", "Reason: " + Util.getLocalizedMessage(e1) + ".");
				if (e1.getFatal()) {
					if (mEntry != null) {
						try {
							mEntry.reinitialize(mEntry.getInitClassPath(),false);
							mEntry.start();
						} catch (Exception e2) {
							if (ParameterSweepWizard.isFromMEME()) {
								JDialog appWindow = (JDialog) mEntry.getAppWindow();
								appWindow.setVisible(false);
								owner.dispose();
								appWindow.dispose();
								appWindow = null;
								System.gc();
							} else
								System.exit(1);
						} 
					} else
						System.exit(1);
				}
			} catch (Exception e1) {
				Utilities.userAlert(owner,"Error while loading the settings file.");
				e1.printStackTrace(ParameterSweepWizard.getLogStream());
			}
		} else if (command.equals("PREFERENCES")) {
			ParameterSweepWizard.setPreferences(new WizardPreferences());
			JDialog dialog = ParameterSweepWizard.getPreferences().showInDialog(ParameterSweepWizard.getFrame());
			dialog.setVisible(true);
			if (PlatformSettings.getGUIControllerForPlatform().isResourcesEnabled() && modelFile != null && owner.isServerMode()) {
				resourcesDialog = new ResourcesDialog(ParameterSweepWizard.getFrame(),owner.getModelRoot());
				resourcesButton.setEnabled(true);
			}
		} else if (command.equals("ABOUT")) {
			Object[] options = { "OK", "License agreement" };
			int ret = javax.swing.JOptionPane.showOptionDialog(ParameterSweepWizard.getFrame(),
															   getAboutContentPane(),"About MEME Parameter Sweep Tools",JOptionPane.DEFAULT_OPTION,
															   JOptionPane.PLAIN_MESSAGE,null,options,options[0]);
			if (ret == 1) 
				showLicense();
		} else if (command.equals("RESOURCES")) {
			if (resourcesDialog != null)
				resourcesDialog.showDialog();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void simulateBrowsePressed(String modelFileName) { //TODO: Ezt ellen�rizni FABLES-szel �s MEME-vel  
		File f = new File(modelFileName);
		changeModelName(f);
	}
	
	//===============================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private void changeModelDirectory(File directory) {
		try {
			owner.setModelRoot(directory.getCanonicalPath());
			ModelDefinition definitionType = PlatformSettings.getGUIControllerForPlatform().getModelDefinitionType();
			switch (definitionType) {
			case JUST_MODEL_DIRECTORY:
				String model = PlatformSettings.getGUIControllerForPlatform().calculateOtherBasicModelInformation(owner,directory.getCanonicalPath());
				File f = new File(model);
				modelFile = f;
				owner.setModelFileName(f.getCanonicalPath());
				String errorCode = PlatformManager.getPlatform(PlatformSettings.getPlatformType()).checkModel(owner);
				if (errorCode != null) {
					Utilities.userAlert(this,"Invalid model: " + f.getName() + " [" + directory.getCanonicalPath() + "]",errorCode);
					owner.setModelRoot(null);
					owner.setModelFileName(null);
					modelFile = null;
					return;
				}
				if (!PlatformSettings.additionalModelCheck(owner)) {
					owner.setModelRoot(null);
					owner.setModelFileName(null);
					modelFile = null;
					return;
				}
				modelFileField.setText(f.getAbsolutePath());
				modelFileField.setToolTipText(f.getAbsolutePath());
				if (PlatformSettings.getGUIControllerForPlatform().isResourcesEnabled() && owner.isServerMode()) {
					resourcesDialog = new ResourcesDialog(ParameterSweepWizard.getFrame(),directory.getCanonicalPath());
					resourcesButton.setEnabled(true);
				}
				break;
			case JUST_MODEL_FILE: throw new IllegalStateException();
			case BOTH_MODEL_FILE_AND_DIRECTORY:
				modelFile = null;
				modelFileField.setText("");
				modelFileField.setToolTipText("");
				owner.setModelFileName(null);
				break;
			}
			invalidateIntelliSweepPlugins();
			modelDirectoryField.setText(directory.getCanonicalPath());
			modelDirectoryField.setToolTipText(directory.getCanonicalPath());
			owner.setModelState(ParameterSweepWizard.NEW_MODEL);
			owner.enableDisableButtons();
		} catch (IOException e1) {
			e1.printStackTrace(ParameterSweepWizard.getLogStream());
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void changeModelName(File f) {
		try {
			owner.setModelFileName(f.getCanonicalPath());
			modelFile = f;
			ModelDefinition definitionType = PlatformSettings.getGUIControllerForPlatform().getModelDefinitionType();
			boolean calculatedDir = false;
			switch (definitionType) {
			case JUST_MODEL_DIRECTORY: throw new IllegalStateException();
			case JUST_MODEL_FILE: 
				String dirStr = PlatformSettings.getGUIControllerForPlatform().calculateOtherBasicModelInformation(owner,f.getCanonicalPath());
				File dir = new File(dirStr);
				owner.setModelRoot(dir.getCanonicalPath());
				modelDirectoryField.setText(dir.getCanonicalPath());
				modelDirectoryField.setToolTipText(dir.getCanonicalPath());
				calculatedDir = true;
			case BOTH_MODEL_FILE_AND_DIRECTORY: // do nothing
			}
			String errorCode = PlatformManager.getPlatform(PlatformSettings.getPlatformType()).checkModel(owner);
			if (errorCode != null) { 
				Utilities.userAlert(this,"Invalid model: " + f.getName() + " [" + owner.getModelRoot() + "]",errorCode);
				owner.setModelFileName(null);
				modelFile = null;
				if (calculatedDir) {
					modelDirectoryField.setText("");
					modelDirectoryField.setToolTipText("");
					owner.setModelRoot(null);
				}
			} else {
				if (!PlatformSettings.additionalModelCheck(owner)) {
					owner.setModelFileName(null);
					modelFile = null;
					if (calculatedDir) {
						modelDirectoryField.setText("");
						modelDirectoryField.setToolTipText("");
						owner.setModelRoot(null);
					}
					return;
				}
				invalidateIntelliSweepPlugins();
				modelFileField.setText(f.getAbsolutePath());
				modelFileField.setToolTipText(f.getAbsolutePath());
				if (PlatformSettings.getGUIControllerForPlatform().isResourcesEnabled() && owner.isServerMode()) {
					resourcesDialog = new ResourcesDialog(ParameterSweepWizard.getFrame(),owner.getModelRoot());
					resourcesButton.setEnabled(true);
				}
				owner.setModelFileName(f.getCanonicalPath());
				owner.setModelState(ParameterSweepWizard.NEW_MODEL);
				owner.enableDisableButtons();
			}
		} catch (IOException e1) {
			e1.printStackTrace(ParameterSweepWizard.getLogStream());
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Shows a file dialog and returns the selected file object.
	 * @param open true if dialog is for open file, false if it is for save to file 
	 * @param file the initial selected file (null if there is not)
	 * @param filter an filter object (null is permitted)
	 * @return the selected file 
	 */
	private File fileDialog(boolean open, File file, FileFilter filter) {
		File ans = null;
		JFileChooser chooser = new JFileChooser(file == null ? ParameterSweepWizard.getLastDir() : file);
		chooser.setName("filechooser_paramsweep");
		if (file != null && !file.isDirectory()) chooser.setSelectedFile(file);
		chooser.setAcceptAllFileFilterUsed(!open);
		if (filter != null) 
			chooser.addChoosableFileFilter(filter);
		int result = -1;
		if (open)
			result = chooser.showOpenDialog(this);
		else
			result = chooser.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			ans = chooser.getSelectedFile();
			ParameterSweepWizard.setLastDir(ans);
		}
		return ans;
	}
	
	//-------------------------------------------------------------------------------
	/** Shows a file dialog and returns the selected file objects in an array.
	 * @param open true if dialog is for open file, false if it is for save to file 
	 * @param filter an filter object (null is permitted)
	 * @param what what kind of file object can be selected (only files, only directories,
	 *        files and directories). See the related constants of the class JFileChooser.
	 * @param multiSelection flag that determines whether the multi selection is enabled in the dialog or not 
	 * @return the selected files (or directories) 
	 */
	private File[] fileDialog(boolean open, FileFilter filter, int what, boolean multiSelection) {
		File[] ans = null;
		if (what != JFileChooser.FILES_ONLY && what != JFileChooser.FILES_AND_DIRECTORIES &&
			what != JFileChooser.DIRECTORIES_ONLY)
			throw new IllegalArgumentException();
		JFileChooser chooser = new JFileChooser(ParameterSweepWizard.getLastDir());
		chooser.setName("filechooser_paramsweep");
		chooser.setAcceptAllFileFilterUsed(!open);
		if (filter != null)
			chooser.addChoosableFileFilter(filter);
		chooser.setMultiSelectionEnabled(multiSelection);
		chooser.setFileSelectionMode(what);
		int result = -1;
		if (open)
			result = chooser.showOpenDialog(this);
		else
			result = chooser.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION) {
			ans = multiSelection ? chooser.getSelectedFiles() : new File[] { chooser.getSelectedFile() };
			ParameterSweepWizard.setLastDir(ans[0]);
		}
		return ans;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void move(boolean top) {
		int[] selected = classPathList.getSelectedIndices();
		if  (selected.length == 0) return;
		
		final List<ClassPathPair> selectedEntries = new ArrayList<ClassPathPair>();
		final List<ClassPathPair> unselectedEntries = new ArrayList<ClassPathPair>();
		for (int i = 0;i < owner.getClassPathListModel().size();++i) {
			if (Utilities.contains(selected,i))
				selectedEntries.add((ClassPathPair)owner.getClassPathListModel().get(i));
			else
				unselectedEntries.add((ClassPathPair)owner.getClassPathListModel().get(i));
		}
		
		owner.getClassPathListModel().removeAllElements();
		
		if (top) {
			for (final ClassPathPair cpp : selectedEntries)
				owner.getClassPathListModel().addElement(cpp);
		}
		for (final ClassPathPair cpp : unselectedEntries)
			owner.getClassPathListModel().addElement(cpp);
		
		if (!top) {
			for (final ClassPathPair cpp : selectedEntries)
				owner.getClassPathListModel().addElement(cpp);
		}
		
		if (top)
			classPathList.getSelectionModel().addSelectionInterval(0,selected.length - 1);
		else
			classPathList.getSelectionModel().addSelectionInterval(owner.getClassPathListModel().size() - selected.length,
																   owner.getClassPathListModel().size() - 1);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void moveTop() { move(true); }
	private void moveBottom() { move(false); }
	
	//-------------------------------------------------------------------------------
	/** Moves the selected elements in the classpath list.
	 * @param offset the direction of the move (possible values: -1,1)
	 */
	private void move(int offset) {
		int[] selected = classPathList.getSelectedIndices();
		
		if (selected.length == 0) return;
		
		List<int[]> intervals = new ArrayList<int[]>();
		int start = selected[0], end = -1, previous = selected[0] - 1;

		for (int i = 0;i < selected.length;++i) {
				if (selected[i] == previous + 1) 
					previous = selected[i];
				else {
					end = previous;
					int[] intv = { start, end };
					intervals.add(intv);
					end = -1;
					start = previous = selected[i];
				}
		}
		intervals.add(new int[] { start, selected[selected.length-1] });
		
		classPathList.clearSelection();
		for (int[] intv : intervals) {
			int to = intv[0] + offset;
			if (0 <= intv[0] && 0 <= to && intv[1] + offset < owner.getClassPathListModel().size()) {
				moveInterval(intv[0],intv[1],to);
				classPathList.getSelectionModel().addSelectionInterval(intv[0] + offset,intv[1] + offset);
			} else 
				classPathList.getSelectionModel().addSelectionInterval(intv[0],intv[1]);
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Moves the elements in the classpath list. The indices of the moved elements
	 *  are in the interval [<code>start</code>,<code>end</code>]. 
	 * @param to the new index of the first element
	 */
	private void moveInterval(int start, int end, int to) {
		int temp = to;
		ClassPathPair[] path = new ClassPathPair[end - start + 1];
		for (int i = start;i <= end; ++i) 
			path[i - start] = (ClassPathPair) owner.getClassPathListModel().get(i);
		owner.getClassPathListModel().removeRange(start,end);
		for (ClassPathPair pair : path)
			owner.getClassPathListModel().add(temp++,pair);
	}
	
	//-------------------------------------------------------------------------------
	/** Updates (add or delete an element) the classpath. */
	private void updateClassPath() {
		// delete
		for (int i = 0;i < owner.getClassPathListModel().size();++i) {
			ClassPathPair pair = (ClassPathPair) owner.getClassPathListModel().get(i);
			owner.getClassPool().removeClassPath(pair.getClassPath());
		}
		// add
		for (int i = owner.getClassPathListModel().size() - 1;i >= 0;--i) {
			ClassPathPair pair = (ClassPathPair) owner.getClassPathListModel().get(i);
			try {
				ClassPath cp = owner.getClassPool().insertClassPath(pair.toString());
				pair.setClassPath(cp);
			} catch (NotFoundException e) {
				// Previously found
				throw new IllegalStateException(e);
			}
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Resets the Resources dialog (deletes all resource entries). */
	private void resetResources() {
		resourcesDialog = null;
		resourcesButton.setEnabled(false);
	}
	
	//-------------------------------------------------------------------------------
	/** Returns the component that contains the content of the About dialog. */
	private JEditorPane getAboutContentPane() {
		JEditorPane aboutContentPane = new JEditorPane();
		aboutContentPane.setEditable(false);
		aboutContentPane.setBorder(null);
		try {
			java.io.InputStream is = SimpleFileFilter.class.getResourceAsStream("icons/about/about.html");
			java.io.StringWriter s = new java.io.StringWriter();
			if (is != null) Utils.copyRW(new java.io.InputStreamReader(is), s);
			String htmlPage = s.toString();
			htmlPage = htmlPage.replace("$VER$", MEMEApp.CURRENT_VERSION);
			htmlPage = htmlPage.replace("<body>", Utils.htmlBody());
			htmlPage = htmlPage.replace("src=\"", "src=\"gui/icons/about/");
			GUIUtils.setTextPane(aboutContentPane, htmlPage);
		} catch (Exception e) {
			e.printStackTrace(ParameterSweepWizard.getLogStream());
		}
		GUIUtils.setWrapLength(aboutContentPane, GUIUtils.dluX(465, aboutContentPane));
		return aboutContentPane;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void invalidateIntelliSweepPlugins() {
		for (PluginInfo<IIntelliStaticMethodPlugin> intelliStaticPlugin : owner.getIntelliStaticPluginVector()) 
	        intelliStaticPlugin.getInstance().invalidatePlugin();
		for (PluginInfo<IIntelliDynamicMethodPlugin> intelliDynamicPlugin : owner.getIntelliDynamicPluginVector()) 
	        intelliDynamicPlugin.getInstance().invalidatePlugin();
    }

	
	//-------------------------------------------------------------------------------
	/** Shows the license file. */
	private void showLicense() {
		java.net.URL url = SimpleFileFilter.class.getResource("icons/about/license.txt");
		GUIUtils.SPMSAEditorPane message;
		try { message = new GUIUtils.SPMSAEditorPane(url); }
		catch (Exception e) { e.printStackTrace(ParameterSweepWizard.getLogStream()); return; }
		message.setEditable(false);
		message.setPreferredSize(new Dimension(GUIUtils.dluX(380),Integer.MAX_VALUE));
		javax.swing.JScrollPane sp = new javax.swing.JScrollPane(message);
		sp.setMaximumSize(new Dimension(GUIUtils.dluX(400),GUIUtils.getRelScrH(80)));
		javax.swing.JOptionPane.showMessageDialog(ParameterSweepWizard.getFrame(),sp,"MEME Parameter Sweep Tools License",
												  JOptionPane.PLAIN_MESSAGE);
	}
	
	//===============================================================================
	// GUI methods
	
	//-------------------------------------------------------------------------------
	private void layoutGUI() { 
		buttonsPanel = FormsUtils.build("p ~ p ~ p ~ p ~ p:g",
										"0123",
										loadSettingsButton,resourcesButton,preferencesButton,aboutButton).getPanel();
		
		content = FormsUtils.build("p ~ p:g ~ p",
								   "[DialogBorder]012 p||" +
								   				 "345||" +
								   				 "678||" +
								   				 "_79||" +
								   				 "_7A||" +
								   				 "_7B||" +
								   				 "_7C||" +
								   				 "_7D||" +
								   				 "_7_ p:g||" +
								   				 "EF_ p:g(0.2)|" +
								   				 "_F_||" +
								   				 "GHI p||||" +
								   				 "_J_|" +
								   				 "_K_|" +
								   				 "_L_|" +
								   				 "_M_|" +
								   				 "NNN",
								   modelDirectoryLabel,modelDirectoryField,dBrowseButton,
								   modelFileLabel,modelFileField,browseButton,
								   classPathLabel,classPathScr,addButton,
								   removeButton,
								   topButton,
								   moveUpButton,
								   moveDownButton,
								   bottomButton,
								   "Description (optional): ",CellConstraints.TOP,descriptionScr,
								   parameterFileLabel,parameterFileField,pBrowseButton,
								   "<html><b>Note: Use 'Parameter file (optional)' field only if you have an existing parameter </b></html>",
								   "<html><b> file. If the selected file is appropriate the wizard initializes the batch runs with </b></html>",
								   "<html><b> its contents.</b></html>"," ",
								   buttonsPanel).getPanel();
		dBrowseButton.setActionCommand("BROWSE_DIR");
		browseButton.setActionCommand("BROWSE_MODEL");
		pBrowseButton.setActionCommand("BROWSE_PARAM");
		modelDirectoryField.setActionCommand("DIR_FIELD");
		modelFileField.setActionCommand("MODEL_FIELD");
		modelDirectoryField.setPreferredSize(new Dimension(200,20));
		modelFileField.setPreferredSize(new Dimension(200,20));
		
		classPathList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		classPathList.setCellRenderer(new ClassPathListRenderer());
		classPathList.setBackground(modelFileField.getBackground());
		classPathScr.setPreferredSize(new Dimension(200,50));
		
		addButton.setActionCommand("ADD_CP");
		removeButton.setActionCommand("REMOVE_CP");
		moveUpButton.setActionCommand("UP");
		moveDownButton.setActionCommand("DOWN");
		topButton.setActionCommand("TOP");
		bottomButton.setActionCommand("BOTTOM");
		
		descriptionArea.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
		
		loadSettingsButton.setActionCommand("SETTINGS");
		resourcesButton.setActionCommand("RESOURCES");
		preferencesButton.setActionCommand("PREFERENCES");
		aboutButton.setActionCommand("ABOUT");
		aboutButton.setVisible(!ParameterSweepWizard.isFromMEME());
		
		dBrowseButton.setName("btn_wizard_modelselection_dirbrowse");
		browseButton.setName("btn_wizard_modelselection_browse");
		pBrowseButton.setName("btn_wizard_modelselection_parambrowse");
		modelDirectoryField.setName("fld_wizard_modelselection_moddirfld");
		modelFileField.setName("fld_wizard_modelselection_modfilefld");
		addButton.setName("btn_wizard_modelselection_add");
		removeButton.setName("btn_wizard_modelselection_remove");
		moveUpButton.setName("btn_wizard_modelselection_up");
		moveDownButton.setName("btn_wizard_modelselection_down");
		topButton.setName("btn_wizard_modelselection_top");
		bottomButton.setName("btn_wizard_modelselection_bottom");
		loadSettingsButton.setName("btn_wizard_modelselection_load");
		resourcesButton.setName("btn_wizard_modelselection_resources");
		preferencesButton.setName("btn_wizard_modelselection_preferences");
		aboutButton.setName("btn_wizard_modelselection_about");
		classPathList.setName("lst_wizard_modelselection_classpath");
		
		ai.aitia.meme.utils.GUIUtils.addActionListener(this,browseButton,pBrowseButton,addButton,removeButton,modelFileField,moveUpButton,
													   moveDownButton,loadSettingsButton,resourcesButton,preferencesButton,aboutButton,dBrowseButton,
													   topButton,bottomButton);
		this.setLayout(new BorderLayout());
		final JScrollPane sp = new JScrollPane(content); 
		this.add(sp,BorderLayout.CENTER);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializePageForPlatform() {
		IGUIController controller = PlatformSettings.getGUIControllerForPlatform();
		modelDirectoryLabel.setEnabled(controller.isModelDirectoryEnabled());
		modelDirectoryField.setEnabled(controller.isModelDirectoryEnabled());
		dBrowseButton.setEnabled(controller.isModelDirectoryEnabled());

		modelFileLabel.setEnabled(controller.isModelFileEnabled());
		modelFileField.setEnabled(controller.isModelFileEnabled());
		browseButton.setEnabled(controller.isModelFileEnabled());
		
		classPathLabel.setEnabled(controller.isClassPathEnabled());
		classPathList.setEnabled(controller.isClassPathEnabled());
		if (controller.isClassPathEnabled()) {
			DefaultListModel model = (DefaultListModel) classPathList.getModel();
			for (int i = 0; i < model.size();++i) {
				ClassPathPair pair = (ClassPathPair) model.get(i);
				owner.getClassPool().removeClassPath(pair.getClassPath());
			}
			owner.initializeClassPathList();
		} else 
			owner.getClassPathListModel().clear();

		
		// load the platform plugin classpath into the classpool
//		Platform platform = PlatformManager.getPlatform(PlatformSettings.getPlatformType());
//		URLClassLoader classLoader = (URLClassLoader) platform.getClass().getClassLoader();
//		for (URL url : classLoader.getURLs()) {
//			try {
//				File file = new File(url.getPath());
//				ClassPath classPath = owner.getClassPool().insertClassPath(url.getPath());
//				owner.getClassPathListModel().addElement(new ClassPathPair(file.getCanonicalPath(), classPath));
//			} catch (NotFoundException e) {
//				// could not find something
//				throw new IllegalStateException(e);
//			} catch (IOException e) {
//				throw new IllegalStateException(e);
//			}
//		}
		
		// load the simulation environment jars into the classpool
		List<String> defaultClassPathEntries = PlatformSettings.getDefaultClassPathEntries(owner);
		for (String cpEntry : defaultClassPathEntries) {
			File file = new File(cpEntry);
			ClassPath classPath;
			try {
				classPath = owner.getClassPool().insertClassPath(cpEntry);
				owner.getClassPathListModel().addElement(new ClassPathPair(file.getCanonicalPath(), classPath));
			} catch (NotFoundException e) {
				// could not find something
				throw new IllegalStateException(e);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		
		addButton.setEnabled(controller.isClassPathEnabled());
		removeButton.setEnabled(controller.isClassPathEnabled());
		moveUpButton.setEnabled(controller.isClassPathEnabled());
		moveDownButton.setEnabled(controller.isClassPathEnabled());
		topButton.setEnabled(controller.isClassPathEnabled());
		bottomButton.setEnabled(controller.isClassPathEnabled());

		parameterFileLabel.setEnabled(controller.isParameterFileEnabled());
		parameterFileField.setEnabled(controller.isParameterFileEnabled());
		pBrowseButton.setEnabled(controller.isParameterFileEnabled());
		
		resourcesButton.setEnabled(PlatformSettings.getGUIControllerForPlatform().isResourcesEnabled() && modelFile != null && owner.isServerMode());
	}
	
	//===============================================================================
	// nested classes
	
	/** This class renders the classpath list. */
	@SuppressWarnings("serial")
	private static class ClassPathListRenderer extends DefaultListCellRenderer {
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			Component com = super.getListCellRendererComponent(list, value, index, isSelected,cellHasFocus);
			((JLabel)com).setToolTipText(value.toString());
			return com;
		}
	}
}
