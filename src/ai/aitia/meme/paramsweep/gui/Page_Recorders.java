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
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import ai.aitia.meme.events.HybridAction;
import ai.aitia.meme.events.IHybridActionListener;
import ai.aitia.meme.gui.ExpandedEditor;
import ai.aitia.meme.gui.Wizard;
import ai.aitia.meme.gui.Wizard.Button;
import ai.aitia.meme.gui.Wizard.IArrowsInHeader;
import ai.aitia.meme.gui.Wizard.IWizardPage;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.batch.IModelInformation.ModelInformationException;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.generator.WizardSettingsManager;
import ai.aitia.meme.paramsweep.gui.info.GeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.MultiColumnOperatorGeneratedMemberInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.RecordableElement;
import ai.aitia.meme.paramsweep.gui.info.ResultInfo;
import ai.aitia.meme.paramsweep.gui.info.TimeInfo;
import ai.aitia.meme.paramsweep.gui.info.TimeInfo.Mode;
import ai.aitia.meme.paramsweep.gui.info.TimeInfo.WriteMode;
import ai.aitia.meme.paramsweep.internal.platform.IGUIController;
import ai.aitia.meme.paramsweep.internal.platform.IScriptSupport;
import ai.aitia.meme.paramsweep.internal.platform.InfoConverter;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.ITestRunner;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.platform.PlatformManager.PlatformType;
import ai.aitia.meme.paramsweep.platform.repast.info.GeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.FormsUtils.Separator;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.TestableDialog;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.Utils.Pair;

import com.jgoodies.forms.layout.CellConstraints;

/** This class provides the Data Collection page of the Parameter Sweep Wizard. */
public class Page_Recorders extends JPanel implements IWizardPage,
													  IArrowsInHeader,
													  ActionListener,
													  ListSelectionListener,
													  TreeSelectionListener,
													  IHybridActionListener,
													  CaretListener,
													  ChangeListener {
	
	//===============================================================================
	// members
	
	private static final long serialVersionUID = 1L;
	
	/** The name of the types which are used as a type of a tick number. */
	private static List<String> iterationTypeNames = new ArrayList<String>(12);
	
	static {
		iterationTypeNames.add("byte");
		iterationTypeNames.add("Byte");
		iterationTypeNames.add("short");
		iterationTypeNames.add("Short");
		iterationTypeNames.add("int");
		iterationTypeNames.add("Integer");
		iterationTypeNames.add("long");
		iterationTypeNames.add("Long");
		iterationTypeNames.add("float");
		iterationTypeNames.add("Float");
		iterationTypeNames.add("double");
		iterationTypeNames.add("Double");
	}
	
	/** The owner of the page. */
	private ParameterSweepWizard owner = null;
	
	private HybridAction newRecorderAction = new HybridAction(this,"New recorder",null);
	private HybridAction editRecorderAction = new HybridAction(this,"Edit recorder",null);
	private HybridAction removeAction = new HybridAction(this,"Remove",null);
	
	/** Flag that determines whether the recorders tree are disabled or not. */
	private boolean disableRectree = false;
	
	/** The node that belongs to the edited recorder. */
	private DefaultMutableTreeNode editedRecorder = null;
	/** Reference to the Advanced dialog. */
	private WriteToFileDialog advancedDialog = null;
	private IScriptSupport scriptSupport = null;
	private boolean modelInformationException = false;
	private RecordableElement lastCreatedRecordableElement = null;

	//===============================================================================
	// GUI members
	
	private JPanel content = null;
	private JRadioButton iterationRadioButton = new JRadioButton("At given number of iteration");
	private JRadioButton conditionRadioButton = new JRadioButton("When conditon is true");
	private JLabel stopLabel = new JLabel("        Value: ");
	private JTextField stopField = new JTextField();
	private JButton stopEEButton = ExpandedEditor.makeEEButton(stopField,"Stop condition expression");
	private JPanel stopPanel = null;
	private DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	private JTree recorderTree = new JTree(root);
	private JScrollPane recorderScr = new JScrollPane(recorderTree);
	private JButton newRecorderButton = new JButton(newRecorderAction);
	private JButton editRecorderButton = new JButton(editRecorderAction);
	private JButton removeButton = new JButton(removeAction);
	private JPanel treePanel = null;
	private JTextField nameField = new JTextField();
	private JTextField outputField = new JTextField();
	private JButton outputBrowseButton = new JButton("..");
	private JPanel generalPanel = null;
	private JRadioButton whenIterationButton = new JRadioButton("At the end of every iteration");
	private JRadioButton whenIterationIntervalButton = new JRadioButton("After every ");
	private JLabel wiiLabel = new JLabel(" iterations");
	private JRadioButton whenRunRadioButton = new JRadioButton("At the end of runs");
	private JRadioButton whenConditionRadioButton = new JRadioButton("When condition is true");
	private JTextField whenIterationIntervalField = new JTextField();
	private JLabel whenConditionLabel = new JLabel("        Condition: ");
	private JTextField whenConditionField = new JTextField();
	private JButton whenEEButton = ExpandedEditor.makeEEButton(whenConditionField,"Record condition expression");
	private JPanel whenPanel = null;
	private JButton advancedButton = new JButton("Advanced...");
	private JButton addRecorderButton = new JButton(" Add recorder ");
	private JButton cancelButton = new JButton("Cancel");
	private JTabbedPane recordableTabbedPane = new JTabbedPane();
	private JList recordableVariables = new JList();
	private JScrollPane recordableVariableScr = new JScrollPane(recordableVariables);
	private JList recordableMethods = new JList();
	private JScrollPane recordableMethodScr = new JScrollPane(recordableMethods);
	private JList recordableScripts = new JList();
	private JScrollPane recordableScriptScr = new JScrollPane(recordableScripts);
	private JList nonRecordableScripts = new JList();
	private JScrollPane nonRecordableScriptScr = new JScrollPane(nonRecordableScripts);
	private JButton addButton = new JButton("Add");
	private JButton addAsButton = new JButton("Add as...");
	private JButton createScriptButton = new JButton("Create");
	private JButton removeScriptButton = new JButton("Remove");
	private JButton editScriptButton = new JButton("Edit");
	private JButton testScriptButton = new JButton("Test");
	private JPanel recordablePanel = null;
	private JPanel editPanel = null;
	
	//===============================================================================
	// methods
	
	//-------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner owner of the page
	 */
	public Page_Recorders(ParameterSweepWizard owner) {
		this.owner = owner;
		layoutGUI();
		initialize();
	}
	
	//-------------------------------------------------------------------------------
	/** Returns the root node of the recorders tree. */
	public DefaultMutableTreeNode getRecorders() { return root; } 
	
	//----------------------------------------------------------------------------------------------------
	/** Returns false if the simulation stopping condition is a logical condition, true
	 *  otherwise.
	 */
	public boolean isStopAfterFixInterval() { return iterationRadioButton.isSelected(); }
	
	//----------------------------------------------------------------------------------------------------
	/** Returns the content of the simulation stopping condition field (named as 'Value'
	 *  or 'Condition' field).
	 */
	public String getStopData() { return stopField.getText().trim(); }
	
	//----------------------------------------------------------------------------------------------------
	public JTree getRecorderTree() { return recorderTree; }
	public JList getRecordableVariablesList() { return recordableVariables; }
	public JList getRecordableMethodsList() { return recordableMethods; }
	public int getSelectedRecordableTab() { return recordableTabbedPane.getSelectedIndex(); }
	
	//----------------------------------------------------------------------------------------------------
	public void clearAllSelection() {
		recordableVariables.clearSelection();
		recordableMethods.clearSelection();
		recordableScripts.clearSelection();
		nonRecordableScripts.clearSelection();
	}
	
	//-------------------------------------------------------------------------------
	/** Initializes the three lists (variables/methods/scripts) of recordable elements. */
	public void initializeRecordableLists() { initializeRecordableLists(new ArrayList<ParameterInfo>(0)); }
	
	//-------------------------------------------------------------------------------
	/** Initializes the three lists (variables/methods/scripts) of recordable elements.
	 * @param illegalParameters the list of information objects of ceased (new) parameters. 
	 */
	public void initializeRecordableLists(List<ParameterInfo> illegalParameters) { 
		List<RecordableInfo> originalRecordables = null;
		try {
			originalRecordables = owner.getModelInformation().getRecordables();
		} catch (ModelInformationException e) {
			Utilities.userAlert(owner,"Identification of recordable elements is failed:",Util.getLocalizedMessage(e));
			modelInformationException = true;
			owner.enableDisableButtons();
			owner.setModelState(ParameterSweepWizard.ERROR);
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			return;
		}
		if (PlatformSettings.getGUIControllerForPlatform().isScriptingSupport()) {
			if (scriptSupport == null)
				scriptSupport = PlatformSettings.getScriptSupport(this.owner,this);
			DefaultListModel[] listModels = scriptSupport.initializeScriptSupport(illegalParameters);
			recordableScripts.setModel(listModels[0]);
			nonRecordableScripts.setModel(listModels[1]);
		}
		DefaultListModel rVModel = new DefaultListModel();
		DefaultListModel rMModel = new DefaultListModel();
		for (RecordableInfo ri : originalRecordables) {
			if (isNewParameter(ri)) continue;
			if (InfoConverter.isVariable(ri))  // variable
				rVModel.addElement(InfoConverter.recordableInfo2RecordableElement(ri));
			else  // method
				rMModel.addElement(InfoConverter.recordableInfo2RecordableElement(ri));
		}
		recordableVariables.setModel(rVModel);
		recordableMethods.setModel(rMModel);
		recordableScripts.getModel().addListDataListener(new ListDataListener() {
			@Override public void intervalRemoved(ListDataEvent e) {
			}
			@Override public void intervalAdded(ListDataEvent e) {
				if (e.getIndex0() == e.getIndex1()) {
					//single element added
					lastCreatedRecordableElement = (RecordableElement) recordableScripts.getModel().getElementAt(e.getIndex0());
				}
			}
			@Override public void contentsChanged(ListDataEvent e) {
			}
		});
		if (PlatformSettings.getGUIControllerForPlatform().isScriptingSupport()) {
			if (recordableTabbedPane.getTabCount() != 4) {
				recordableTabbedPane.addTab("Data sources",recordableScriptScr);
				recordableTabbedPane.addTab("Misc.",nonRecordableScriptScr);
				recordableTabbedPane.setToolTipTextAt(2,"Recordable calculated derivative values (e.g. statistics).");
				recordableTabbedPane.setToolTipTextAt(3,"<html>Non-recordable calculated derivative values (e.g. statistics). These<br>can't" +
														" be used as a recordable element only in other data sources.<br>In this page" +
														" you can edit or remove them.</html>");

			}
		} else {
			if (recordableTabbedPane.getTabCount() > 2) {
				recordableTabbedPane.removeTabAt(3);
				recordableTabbedPane.removeTabAt(2);
			}
		}
	}

//	public void initalizeRecorders(){
//		@SuppressWarnings("unchecked")
//		DefaultListModel<RecordableElement> model = (DefaultListModel<RecordableElement>)recordableScripts.getModel();
//		try {
//			List<RecorderInfo> recorders = owner.getModelInformation().getRecorders();
//			for (RecorderInfo recorderInfo : recorders) {
//				DefaultMutableTreeNode recorder = new DefaultMutableTreeNode(recorderInfo.getName());
//				ResultInfo ri = new ResultInfo(recorderInfo.getOutputFile().getName());
//				DefaultMutableTreeNode riNode = new DefaultMutableTreeNode(ri);
//				recorder.add(riNode);
//				String recordType = recorderInfo.getRecordType();
//				String arg = null;
//				int indexOfColon = recordType.indexOf(':');
//				if (indexOfColon != -1){
//					arg = recordType.substring(indexOfColon + 1).trim();
//					recordType = recordType.substring(0, indexOfColon).trim();
//				}
//				TimeInfo ti = new TimeInfo(recordType, arg);
//				
//				String writeType = recorderInfo.getWriteType();
//				long writeArg = 0;
//				indexOfColon = writeType.indexOf(':');
//				if (indexOfColon != -1){
//					writeArg = Long.parseLong(writeType.substring(indexOfColon + 1));
//					writeType = writeType.substring(0, indexOfColon).trim();
//				}
//				ti.setWriteMode(writeType, writeArg);
//				DefaultMutableTreeNode tiNode = new DefaultMutableTreeNode(ti);
//				recorder.add(tiNode);
//				DefaultTreeModel treeModel = (DefaultTreeModel) recorderTree.getModel();
//				treeModel.insertNodeInto(recorder,root,root.getChildCount());
//
//				List<RecordableInfo> recordables = recorderInfo.getRecordables();
//				for (RecordableInfo recordableInfo : recordables) {
//					RecordableElement recordableElement = InfoConverter.recordableInfo2RecordableElement(recordableInfo);
//					if (!model.contains(recordableElement)){
//						model.addElement(recordableElement);
//					}
//					if (!(MultiColumnOperatorGeneratedMemberInfo.class.isAssignableFrom(recordableElement.getInfo().getClass()))
//							&& (Collection.class.isAssignableFrom(recordableElement.getInfo().getJavaType()) || recordableElement.getInfo().getJavaType().isArray())) {
//						scriptSupport.createMultiColumnScript(recordableElement.getInfo(), (DefaultListModel)recordableScripts.getModel(),(DefaultListModel)nonRecordableScripts.getModel(), null);
//						//look for the newly created recordable and add it to the recorder
//						if (lastCreatedRecordableElement != null) {
//							if (!contains(recorder, lastCreatedRecordableElement.getInfo())) {
//								treeModel.insertNodeInto(new DefaultMutableTreeNode(lastCreatedRecordableElement),recorder,recorder.getChildCount());
//							}
//							lastCreatedRecordableElement = null;
//						}
//					} else if (!contains(recorder,recordableElement.getInfo())) {
//						treeModel.insertNodeInto(new DefaultMutableTreeNode(recordableElement),recorder,recorder.getChildCount());
//					}
//					scriptSupport.getAllMembers().add(recordableElement.getInfo());
//					Fugg.f.setFuggveny(recordableElement.getInfo().getName() + " : " + recordableElement.getInfo().getType(),"");
//
//				}
//				
//
//				TreePath path = new TreePath(recorder.getPath());
//				recorderTree.setSelectionPath(path);
//				recorderTree.expandPath(path);
//
//			}
//		} catch (ModelInformationException e) {
//			Utilities.userAlert(owner,"Identification of pre-configured recorders is failed:",Util.getLocalizedMessage(e));
//			modelInformationException = true;
//			owner.enableDisableButtons();
//			owner.setModelState(ParameterSweepWizard.ERROR);
//			e.printStackTrace(ParameterSweepWizard.getLogStream());
//			return;
//		}
//	}

	public void initalizeRecorders(){
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(owner.getCustomClassLoader());
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder parser = factory.newDocumentBuilder();
			String recordersXML = owner.getModelInformation().getRecordersXML();
			if(recordersXML !=null)
			{
				Document document = parser.parse(new ByteArrayInputStream(recordersXML.getBytes("UTF-8")));
				
				Element rootElement = document.getDocumentElement();
	
				load(rootElement);
			}
		} catch (ModelInformationException e) {
			Utilities.userAlert(owner,"Identification of pre-configured recorders is failed:",Util.getLocalizedMessage(e));
			modelInformationException = true;
			owner.enableDisableButtons();
			owner.setModelState(ParameterSweepWizard.ERROR);
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			return;
		} catch (ParserConfigurationException e) {
			Utilities.userAlert(owner,"Identification of pre-configured recorders is failed:",Util.getLocalizedMessage(e));
			modelInformationException = true;
			owner.enableDisableButtons();
			owner.setModelState(ParameterSweepWizard.ERROR);
			e.printStackTrace(ParameterSweepWizard.getLogStream());
		} catch (SAXException e) {
			Utilities.userAlert(owner,"Identification of pre-configured recorders is failed:",Util.getLocalizedMessage(e));
			modelInformationException = true;
			owner.enableDisableButtons();
			owner.setModelState(ParameterSweepWizard.ERROR);
			e.printStackTrace(ParameterSweepWizard.getLogStream());
		} catch (IOException e) {
			Utilities.userAlert(owner,"Identification of pre-configured recorders is failed:",Util.getLocalizedMessage(e));
			modelInformationException = true;
			owner.enableDisableButtons();
			owner.setModelState(ParameterSweepWizard.ERROR);
			e.printStackTrace(ParameterSweepWizard.getLogStream());
		} catch (WizardLoadingException e) {
			Utilities.userAlert(owner,"Identification of pre-configured recorders is failed:",Util.getLocalizedMessage(e));
			modelInformationException = true;
			owner.enableDisableButtons();
			owner.setModelState(ParameterSweepWizard.ERROR);
			e.printStackTrace(ParameterSweepWizard.getLogStream());
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}

	//-------------------------------------------------------------------------------
	/** Deletes all illegal recordable elements from the recorders. */
	public void cleanRecorders() { 
		List<String> reserved = Arrays.asList(owner.getInitParam());
		DefaultTreeModel treeModel = (DefaultTreeModel) recorderTree.getModel();
		for (int i = 0;i < root.getChildCount();++i) {
			List<DefaultMutableTreeNode> illegal = new ArrayList<DefaultMutableTreeNode>();
			DefaultMutableTreeNode recorderNode = (DefaultMutableTreeNode) root.getChildAt(i);
			if (recorderNode.getChildCount() > 2) {
				for (int j = 2;j < recorderNode.getChildCount();++j) {
					DefaultMutableTreeNode recordable = (DefaultMutableTreeNode) recorderNode.getChildAt(j);
					RecordableElement re = (RecordableElement) recordable.getUserObject();
					MemberInfo mi = re.getInfo();
					if (reserved.contains(mi.getName()))
						illegal.add(recordable);
					else if (PlatformSettings.getGUIControllerForPlatform().isScriptingSupport()) {
						if (scriptSupport.getIllegalGenerateds() != null && mi instanceof GeneratedMemberInfo) {
							GeneratedMemberInfo gmi = (GeneratedMemberInfo) mi;
							if (scriptSupport.getIllegalGenerateds().contains(gmi))
								illegal.add(recordable);
						}
					}
				}
			}
			for (DefaultMutableTreeNode node : illegal)
				treeModel.removeNodeFromParent(node);
		}
		if (PlatformSettings.getGUIControllerForPlatform().isScriptingSupport())
			scriptSupport.illegalGeneratedsToNull();
		owner.enableDisableButtons();
	}
	
	//-------------------------------------------------------------------------------
	/** Saves the page-related model settings to the XML node <code>node</code>. This
	 *  method is part of the model settings storing performed by {@link ai.aitia.meme.paramsweep.generator.WizardSettingsManager 
	 *  WizardSettingsManager}.
	 */
	public void save(Node node) { 
		Document document = node.getOwnerDocument();
		
		Element element = document.createElement(WizardSettingsManager.STOP_DATA);
		element.setAttribute(WizardSettingsManager.IS_CONDITION,String.valueOf(!isStopAfterFixInterval()));
		element.appendChild(document.createTextNode(stopField.getText().trim()));
		node.appendChild(element);
		
		if (PlatformSettings.getGUIControllerForPlatform().isScriptingSupport()) 
			scriptSupport.saveScripts(node);
		
		Element recorders = document.createElement(WizardSettingsManager.RECORDERS);
		for (int i = 0;i < root.getChildCount();++i) {
			DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) root.getChildAt(i);
			Element recorder = document.createElement(WizardSettingsManager.RECORDER);
			recorder.setAttribute(WizardSettingsManager.NAME,treeNode.toString());
			
			DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) treeNode.getFirstChild();
			ResultInfo ri = (ResultInfo) childNode.getUserObject();
			element = document.createElement(WizardSettingsManager.OUTPUT_FILE);
			element.appendChild(document.createTextNode(ri.getFile()));
			recorder.appendChild(element);
			
			childNode = (DefaultMutableTreeNode) treeNode.getChildAt(1);
			TimeInfo ti = (TimeInfo) childNode.getUserObject();
			element = document.createElement(WizardSettingsManager.TIME);
			element.setAttribute(WizardSettingsManager.TYPE,ti.getType().toString());
			if (ti.getArg() != null)
				element.appendChild(document.createTextNode(ti.getArg()));
			recorder.appendChild(element);
			
			element = document.createElement(WizardSettingsManager.WRITING_TIME);
			element.setAttribute(WizardSettingsManager.TYPE,ti.getWriteType().toString());
			if (ti.getWriteArg() > 0)
				element.appendChild(document.createTextNode(String.valueOf(ti.getWriteArg())));
			recorder.appendChild(element);
			InfoConverter.saveRecorderElementsToXml(recorder,treeNode);
			recorders.appendChild(recorder);
		}
		node.appendChild(recorders);
	}
	
	//-------------------------------------------------------------------------------
	/** Loads the page-related model settings from the XML element <code>element</code>. This
	 *  method is part of the model settings retrieving performed by {@link ai.aitia.meme.paramsweep.generator.WizardSettingsManager 
	 *  WizardSettingsManager}.<br>
	 *  Pre-condition : <code>Page_LoadModel.load()</code> has finished
	 * @throws WizardLoadingException if the XML document is invalid
	 */
	public void load(Element element) throws WizardLoadingException {
		if (scriptSupport == null && PlatformSettings.getGUIControllerForPlatform().isScriptingSupport())
			scriptSupport = PlatformSettings.getScriptSupport(this.owner,this);
		initializeRecordableLists();
		
		NodeList nl = element.getElementsByTagName(WizardSettingsManager.STOP_DATA);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.STOP_DATA);
		Element stopElement = (Element) nl.item(0);
		String condition = stopElement.getAttribute(WizardSettingsManager.IS_CONDITION);
		if (condition.equals(""))
			throw new WizardLoadingException(true,"missing 'condition' attribute at node: " + WizardSettingsManager.STOP_DATA);
		if (Boolean.parseBoolean(condition))
			conditionRadioButton.setSelected(true);
		else
			iterationRadioButton.setSelected(true);
		NodeList content = stopElement.getChildNodes();
		if (content != null && content.getLength() != 0) {
			String value = ((Text)content.item(0)).getNodeValue();
			stopField.setText(value);
		}
		
		if (PlatformSettings.getGUIControllerForPlatform().isScriptingSupport()) 
			scriptSupport.createScriptList(element,(DefaultListModel)recordableScripts.getModel(),(DefaultListModel)nonRecordableScripts.getModel());
		
		nl = null;
		nl = element.getElementsByTagName(WizardSettingsManager.RECORDERS);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.RECORDERS);
		Element recorders = (Element) nl.item(0);
		createRecorderTree(recorders);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void removeFromRecorders(GeneratedMemberInfo info) {  
		DefaultTreeModel treeModel = (DefaultTreeModel) recorderTree.getModel();
		for (int i = 0;i < root.getChildCount();++i) {
			List<DefaultMutableTreeNode> illegal = new ArrayList<DefaultMutableTreeNode>();
			DefaultMutableTreeNode recorderNode = (DefaultMutableTreeNode) root.getChildAt(i);
			if (recorderNode.getChildCount() > 2) {
				for (int j = 2;j < recorderNode.getChildCount();++j) {
					DefaultMutableTreeNode recordable = (DefaultMutableTreeNode) recorderNode.getChildAt(j);
					RecordableElement re = (RecordableElement) recordable.getUserObject();
					MemberInfo mi = re.getInfo();
					if (mi.equals(info))
						illegal.add(recordable);
				}
			}
			for (DefaultMutableTreeNode node : illegal)
				treeModel.removeNodeFromParent(node);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void removeFromStoppingCondition(GeneratedMemberInfo info) { 
		String candidate = new String(stopField.getText().trim());
		if ("".equals(candidate)) return;
		if (iterationRadioButton.isSelected()) { // fix iteration
			if (!candidate.endsWith("()")) 
				candidate += "()";
			MemberInfo candidateInfo = new MemberInfo(candidate,"",null);
			if (candidateInfo.equals(info))
				stopField.setText("");
		} else {
			if (candidate.contains(info.getName()))
				stopField.setText("");
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Checks whether the recorders tree is valid or not. */
	public boolean isTreeValid() { return root.getChildCount() > 0; }
	public void setStopFieldText(String text) { stopField.setText(text); }
	
	//-------------------------------------------------------------------------------
	public IScriptSupport getScriptSupport(){
		if (scriptSupport == null)
			scriptSupport = PlatformSettings.getScriptSupport(this.owner,this);
		
		return scriptSupport;
	}
	
	//===============================================================================
	// interface implementations
	
	//-------------------------------------------------------------------------------
	public String getInfoText(Wizard w) { return w.getArrowsHeader("Specify data collection"); }
	public Container getPanel() { return this; }
	public String getTitle() { return "Data collection"; }
	public boolean isEnabled(Button b) {
		if (modelInformationException && (b == Button.NEXT || b == Button.FINISH)) return false;
		return PlatformSettings.isEnabledForPageRecorders(owner,b);
	}

	//-------------------------------------------------------------------------------
	public boolean onButtonPress(Button b) { 
		if (b == Button.FINISH || b == Button.CUSTOM)  {
			boolean[] checkThis = PlatformSettings.finishCheck(owner,this); // inspired by Speak
			if (!checkThis[0]) return false;
			if (checkThis[1]) {
				List<String> emptyRecorder = emptyRecorders();
				if (emptyRecorder.size() > 0) {
					int result = Utilities.askUserOK(ParameterSweepWizard.getFrame(),"Warning",
													 "The following data recorders do not contain any" +
													 "recordable elements:",Utils.join(emptyRecorder,", ") + ".",
													 "Click 'OK' to continue anyway.",
													 "If you would like to add recordable elements to these" +
													 " data recorders click 'Cancel'.");
					if (result == 0) return false;
				}
				
				List<String> invalidRecorders = invalidRecorderPath();
				if (invalidRecorders.size() > 0) {
					Utilities.userAlert(ParameterSweepWizard.getFrame(),
										"Invalid output path(s) at the following data recorder(s):",Utils.join(invalidRecorders,", ") + ".",
										"","At distributed simulation running you can only define the name of the result files.");
					return false;
				}
				return checkEndOfSimulation();
			}
			return true;
		} else if (b == Button.BACK) {
			if (isEnabled(b) && owner.getSweepingMethodID() != 0)
				owner.gotoPage(3);
		}
		return isEnabled(b);
	}

	//-------------------------------------------------------------------------------
	public void onPageChange(boolean show) { 
		if (show) {
			if (owner.getModelState() == ParameterSweepWizard.ERROR) return;
			initializePageForPlatform();
			if (owner.getModelState() == ParameterSweepWizard.NEW_RECORDERS) {
				modelInformationException = false;
				resetPage();
				if (scriptSupport == null && PlatformSettings.getGUIControllerForPlatform().isScriptingSupport())
					scriptSupport = PlatformSettings.getScriptSupport(this.owner,this);
				initializeRecordableLists();
				initalizeRecorders();
				owner.setModelState(ParameterSweepWizard.OLD);
			}
			if (root.getChildCount() == 0) {
				newRecorderButton.doClick();
				Thread hackThread = new Thread(new Runnable() {
					// do this because <component>.grabFocus() does nothing if the <component> is not visible
					Object o = new Object();
					public void run() {
						try {
							synchronized (o) { o.wait(1); }
						} catch (InterruptedException e) {}
						nameField.grabFocus();
					}
				});
				hackThread.setName("Support-Thread-0");
				hackThread.start();
			}
		} else if (cancelButton.isEnabled())
			cancelButton.doClick();
	}

	//-------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) { hybridAction(e,null); }

	//-------------------------------------------------------------------------------
	/** This method is intended to be a common place for handling "related" actionPerformed()
	 *  messages (here "related" usually means that belong to the same window).
	 * @param a Non-null only if this actionPerformed() message is originated from a
	 *           HybridAction object.     
	 */
	public void hybridAction(ActionEvent e, HybridAction a) { 
		if (a != null) {
			if (a == newRecorderAction) {
				resetSettings();
				enableDisableSettings(true);
				disableRecTree();
				String defaultName = generateName();
				nameField.setText(defaultName);
				if (owner.getModelFileName() != null) {
					String defaultFile = defaultName + ".txt";
					outputField.setText(defaultFile);
					outputField.setToolTipText(defaultFile);
					addRecorderButton.setEnabled(true);
					advancedDialog = new WriteToFileDialog(ParameterSweepWizard.getFrame(),WriteMode.RUN);
				}				
				nameField.grabFocus();
			} else if (a == editRecorderAction) {
				TreePath path = recorderTree.getSelectionPath();
				if (path == null) return;
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				while (!(node.getUserObject() instanceof String)) {
					if (node.equals(root)) return;
					node = (DefaultMutableTreeNode) node.getParent();
				}
				resetSettings();
				enableDisableSettings(true);
				disableRecTree();
				addRecorderButton.setText("Modify recorder");
				edit(node);
			} else if (a == removeAction) {
				TreePath path = recorderTree.getSelectionPath();
				if (path == null) return;
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
				
				DefaultTreeModel treeModel = (DefaultTreeModel) recorderTree.getModel();
				if (node.getUserObject() instanceof RecordableElement) {
					treeModel.removeNodeFromParent(node);
				} else {
					while (!(node.getUserObject() instanceof String)) {
						if (node.equals(root)) return;
						node = (DefaultMutableTreeNode) node.getParent();
					}
					int result = Utilities.askUser(owner,false,"Delete recorder","Do you really want to delete this recorder: " + node.toString() +
												   "?");
					if (result == 1) 
						treeModel.removeNodeFromParent(node);
				}
				owner.enableDisableButtons();
			}
		} else {
			String command = e.getActionCommand();
			if (command.equals("STOP_ITERATION") || command.equals("STOP_CONDITION")) { 
				setStopLabel(command);
				stopEEButton.setEnabled(!stopEEButton.isEnabled());
				stopField.setText("");
				stopField.grabFocus();
			} else if ("OUTPUT_BROWSE".equals(command)) {
				File original = null;
				if (outputField.getText().trim().length() != 0) {
					original = new File(outputField.getText().trim());
					if (!original.exists() || original.isDirectory())
						original = null;
				}
				JFileChooser chooser = new JFileChooser(original == null ? ParameterSweepWizard.getLastDir() : original);
				if (original != null)
					chooser.setSelectedFile(original);
				chooser.setAcceptAllFileFilterUsed(true);
				int result = chooser.showSaveDialog(this);
				File f = null;
				if (result == JFileChooser.APPROVE_OPTION) {
					f = chooser.getSelectedFile();
					ParameterSweepWizard.setLastDir(f);
					if (f.exists()) {
						int ans = Utilities.askUser(this,false,"Override comfirmation",String.format("%s is exists.",f.getName()),"Are you sure?");
						if (ans == 0) return;
					} 
					outputField.setText(f.getAbsolutePath());
					outputField.setToolTipText(f.getAbsolutePath());
					addRecorderButton.setEnabled(nameField.getText().trim().length() != 0);
				}
			} else if (command.equals("WHEN_ITERATION") || command.equals("WHEN_RUN")) {
				whenIterationIntervalField.setEnabled(false);
				whenIterationIntervalField.setText("");
				whenConditionField.setEnabled(false);
				whenConditionField.setText("");
				whenEEButton.setEnabled(false);
			} else if (command.equals("WHEN_ITERATION_INTERVAL")) {
				whenIterationIntervalField.setEnabled(true);
				whenIterationIntervalField.grabFocus();
				whenConditionField.setEnabled(false);
				whenConditionField.setText("");
				whenEEButton.setEnabled(false);
			} else if (command.equals("WHEN_CONDITION")) {
				whenIterationIntervalField.setEnabled(false);
				whenIterationIntervalField.setText("");
				whenConditionField.setEnabled(true);
				whenConditionField.grabFocus();
				whenEEButton.setEnabled(true);
			} else if (command.equals("ADD_OR_MODIFY")) {
				boolean wrongName = false;
				if (editedRecorder == null)
					wrongName = !isNewName(nameField.getText().trim());
				else
					wrongName = !isNewName(nameField.getText().trim()) &&
								!editedRecorder.toString().equals(nameField.getText().trim());
			   	if (wrongName) {
			    	 Utilities.userAlert(owner,"This name is already assigned to another recorder.");
			    	 return;
			    }
				String[] error = new String[1];
				TimeInfo ti = getTimeInfo(error);
				if (ti == null) {
					Utilities.userAlert(owner,error[0]);
					return;
				}
				applyAdvancedSettings(ti);
				String text = outputField.getText().trim();
				if (isAlreadyAssigned(text)) {
					Utilities.userAlert(owner,"This file is already assigned to another recorder.");
					return;
				}
				
				final File recFile = new File(text);
				if (!isValidRecorderFile(recFile.getName())) {
					Utilities.userAlert(owner,"The name of the output file is invalid (only alphanumeric characters are allowed).");
					return;
				}
				
				if (addRecorderButton.getText().startsWith("Modify")) {
					editedRecorder.setUserObject(new String(nameField.getText().trim()));
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) editedRecorder.getFirstChild();
					node.setUserObject(new ResultInfo(text));
					node = (DefaultMutableTreeNode) editedRecorder.getChildAt(1);
					node.setUserObject(ti);
					TreePath path = new TreePath(editedRecorder.getPath());
					recorderTree.setSelectionPath(path);
					recorderTree.expandPath(path);
					editedRecorder = null;
					fireTreeNodesChanged(path);
				} else {
					DefaultMutableTreeNode recorder = new DefaultMutableTreeNode(nameField.getText().trim());
					ResultInfo ri = new ResultInfo(text);
					DefaultMutableTreeNode riNode = new DefaultMutableTreeNode(ri);
					recorder.add(riNode);
					DefaultMutableTreeNode tiNode = new DefaultMutableTreeNode(ti);
					recorder.add(tiNode);
					DefaultTreeModel model = (DefaultTreeModel) recorderTree.getModel();
					model.insertNodeInto(recorder,root,root.getChildCount());
					TreePath path = new TreePath(recorder.getPath());
					recorderTree.setSelectionPath(path);
					recorderTree.expandPath(path);
				}
				resetSettings();
				enableDisableSettings(false);
				addRecorderButton.setEnabled(false);
				disableRectree = false;
				newRecorderAction.setEnabled(true);
				removeAction.setEnabled(true);
				editRecorderAction.setEnabled(true);
				owner.enableDisableButtons();
				advancedDialog = null;
			} else if (command.equals("CANCEL")) {
				resetSettings();
				enableDisableSettings(false);
				addRecorderButton.setEnabled(false);
				disableRectree = false;
				newRecorderAction.setEnabled(true);
				TreePath path = recorderTree.getSelectionPath();
				if (path != null) {
					DefaultMutableTreeNode selected = (DefaultMutableTreeNode) path.getLastPathComponent();
					if (selected != null) {
						removeAction.setEnabled(true);
						editRecorderAction.setEnabled(true);
					}
				}
			} else {
				IGUIController controller = PlatformSettings.getGUIControllerForPlatform();
				if (command.equals("ADD")) {
					DefaultTreeModel treeModel = (DefaultTreeModel) recorderTree.getModel();
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) recorderTree.getSelectionPath().getLastPathComponent();

					if (disableRectree) {
						node = editedRecorder;
						recorderTree.setSelectionPath(new TreePath(node.getPath()));
					}

					while (!(node.getUserObject() instanceof String)) {
						if (node.equals(root)) return;
						node = (DefaultMutableTreeNode) node.getParent();
					}
					
					DefaultListModel listModel = (DefaultListModel) recordableVariables.getModel();
					int[] indices = recordableVariables.getSelectedIndices();
					
					JPopupMenu pMenu = controller.getAddPopupMenu(owner);
					if (pMenu != null) {
						pMenu.setName("cmenu_wizard_record_recordablescmenu");
						if (indices.length > 0 && recordableTabbedPane.getSelectedIndex() == 0) {
							RecordableElement re = (RecordableElement) listModel.get(indices[0]);
							controller.enabledDisabledPopupMenuElements(re);
						    int menuX = addButton.getX() + addButton.getWidth() + recordablePanel.getX() + editPanel.getX();
						    int menuY = addButton.getY() + addButton.getHeight() + recordablePanel.getY() + editPanel.getY();
						    pMenu.show(content,menuX,menuY);
						}
					    
						listModel = (DefaultListModel) recordableMethods.getModel();
					    indices = recordableMethods.getSelectedIndices();
					     
					    if (indices.length > 0 && recordableTabbedPane.getSelectedIndex() == 1) {
					    	RecordableElement re = (RecordableElement) listModel.get(indices[0]);
							controller.enabledDisabledPopupMenuElements(re);
						    int menuX = addButton.getX() + addButton.getWidth() + recordablePanel.getX() + editPanel.getX();
						    int menuY = addButton.getY() + addButton.getHeight() + recordablePanel.getY() + editPanel.getY();
						    pMenu.show(content,menuX,menuY);
					    }
					} else {
						for (int index : indices) {
							RecordableElement re = (RecordableElement) listModel.get(index);
							if (Collection.class.isAssignableFrom(re.getInfo().getJavaType()) || re.getInfo().getJavaType().isArray()) {
								scriptSupport.createMultiColumnScript(re.getInfo(), (DefaultListModel)recordableScripts.getModel(),(DefaultListModel)nonRecordableScripts.getModel(), null);
								//look for the newly created recordable and add it to the recorder
								if (lastCreatedRecordableElement != null) {
									if (!contains(node, lastCreatedRecordableElement.getInfo())) {
										treeModel.insertNodeInto(new DefaultMutableTreeNode(lastCreatedRecordableElement),node,node.getChildCount());
									}
									lastCreatedRecordableElement = null;
								}
							} else if (!contains(node,re.getInfo()))
								treeModel.insertNodeInto(new DefaultMutableTreeNode(re),node,node.getChildCount());
						}
						
						listModel = (DefaultListModel) recordableMethods.getModel();
						indices = recordableMethods.getSelectedIndices();
						for (int index : indices) {
							RecordableElement re = (RecordableElement) listModel.get(index);
							if (Collection.class.isAssignableFrom(re.getInfo().getJavaType()) || re.getInfo().getJavaType().isArray()) {
								scriptSupport.createMultiColumnScript(re.getInfo(), (DefaultListModel)recordableScripts.getModel(),(DefaultListModel)nonRecordableScripts.getModel(), null);
								//look for the newly created recordable and add it to the recorder
								if (lastCreatedRecordableElement != null) {
									if (!contains(node, lastCreatedRecordableElement.getInfo())) {
										treeModel.insertNodeInto(new DefaultMutableTreeNode(lastCreatedRecordableElement),node,node.getChildCount());
									}
									lastCreatedRecordableElement = null;
								}
							} else if (!contains(node,re.getInfo())) {
								treeModel.insertNodeInto(new DefaultMutableTreeNode(re),node,node.getChildCount());
							}
						}
						
						if (controller.isScriptingSupport()) {
							listModel = (DefaultListModel) recordableScripts.getModel();
							indices = recordableScripts.getSelectedIndices();
							for (int index : indices) {
								RecordableElement re = (RecordableElement) listModel.get(index);
								if (!(MultiColumnOperatorGeneratedMemberInfo.class.isAssignableFrom(re.getInfo().getClass()))
										&& (Collection.class.isAssignableFrom(re.getInfo().getJavaType()) || re.getInfo().getJavaType().isArray())) {
									scriptSupport.createMultiColumnScript(re.getInfo(), (DefaultListModel)recordableScripts.getModel(),(DefaultListModel)nonRecordableScripts.getModel(), null);
									//look for the newly created recordable and add it to the recorder
									if (lastCreatedRecordableElement != null) {
										if (!contains(node, lastCreatedRecordableElement.getInfo())) {
											treeModel.insertNodeInto(new DefaultMutableTreeNode(lastCreatedRecordableElement),node,node.getChildCount());
										}
										lastCreatedRecordableElement = null;
									}
								} else if (!contains(node,re.getInfo())) {
									treeModel.insertNodeInto(new DefaultMutableTreeNode(re),node,node.getChildCount());
								}
							}
							recordableMethods.clearSelection();
							recordableScripts.clearSelection();
						}
						recordableVariables.clearSelection();
						nonRecordableScripts.clearSelection();
						owner.enableDisableButtons();
					}
				} else if ("ADD_AS".equals(command)) {
					DefaultTreeModel treeModel = (DefaultTreeModel) recorderTree.getModel();
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) recorderTree.getSelectionPath().getLastPathComponent();

					if (disableRectree) {
						node = editedRecorder;
						recorderTree.setSelectionPath(new TreePath(node.getPath()));
					}

					while (!(node.getUserObject() instanceof String)) {
						if (node.equals(root)) return;
						node = (DefaultMutableTreeNode) node.getParent();
					}
					
					RecordableElement selected = (RecordableElement) recordableVariables.getSelectedValue();
					if (selected == null) 
						selected = (RecordableElement) recordableMethods.getSelectedValue();
					if (selected == null && controller.isScriptingSupport())
						selected = (RecordableElement) recordableScripts.getSelectedValue();

					if (selected != null) {
						if (contains(node,selected.getInfo())) {
							Utilities.userAlert(owner,"The selected recordable element " + selected.getInfo().toString() + " is already contained by" +
												" the selected recorder.");
							return;
						}
						/*String alias = (String) JOptionPane.showInputDialog(owner,"Alias name for " + selected.getInfo().toString() + ":","Add as...",
																			JOptionPane.PLAIN_MESSAGE,null,null,selected.getNameForRecorders());*/
						
							String alias = (String)TestableDialog.showInputDialog(owner, "Alias name for " + selected.getInfo().toString() + ":", "Add as...", JOptionPane.PLAIN_MESSAGE,
									null,null, selected.getNameForRecorders(), "dial_wizard_record_addas");
							
							/*String alias2 = (String) JOptionPane.showInputDialog(owner,"Alias name for " + selected.getInfo().toString() + ":","Add as...",
																			JOptionPane.PLAIN_MESSAGE,null,null,selected.getNameForRecorders());*/
						if (null != alias && alias.trim().length() > 0) {
							selected.setAlias(alias);
							if (!(MultiColumnOperatorGeneratedMemberInfo.class.isAssignableFrom(selected.getInfo().getClass())) && (Collection.class.isAssignableFrom(selected.getInfo().getJavaType()) || selected.getInfo().getJavaType().isArray())) {
								scriptSupport.createMultiColumnScript(selected.getInfo(), (DefaultListModel)recordableScripts.getModel(),(DefaultListModel)nonRecordableScripts.getModel(), alias);
								//look for the newly created recordable and add it to the recorder
								if (lastCreatedRecordableElement != null) {
									if (!contains(node, lastCreatedRecordableElement.getInfo())) {
										treeModel.insertNodeInto(new DefaultMutableTreeNode(lastCreatedRecordableElement),node,node.getChildCount());
									}
									lastCreatedRecordableElement = null;
								}
							} else {
								treeModel.insertNodeInto(new DefaultMutableTreeNode(selected),node,node.getChildCount());
							}
						}
					}
					recordableVariables.clearSelection();
					recordableMethods.clearSelection();
					if (controller.isScriptingSupport()) {
						recordableScripts.clearSelection();
						nonRecordableScripts.clearSelection();
					}
					owner.enableDisableButtons();
				} else if (command.equals("CREATE")) 
					scriptSupport.createScript((DefaultListModel)recordableScripts.getModel(),(DefaultListModel)nonRecordableScripts.getModel());
				else if (command.equals("EDIT_SCRIPT")) {
					RecordableElement selected = null;
					if (recordableTabbedPane.getSelectedIndex() == 2)
						selected = (RecordableElement) recordableScripts.getSelectedValue();
					else if (recordableTabbedPane.getSelectedIndex() == 3)
						selected = (RecordableElement) nonRecordableScripts.getSelectedValue();
					recordableScripts.clearSelection();
					nonRecordableScripts.clearSelection();
					if (selected != null) 
						scriptSupport.editScript(selected,(DefaultListModel)recordableScripts.getModel(),
												 (DefaultListModel)nonRecordableScripts.getModel());
				} else if (command.equals("ADVANCED")) {
					long arg = 0;
					String error[] = new String[1];
					TimeInfo ti = getTimeInfo(error);
					if (error[0] != null) {
						Utilities.userAlert(owner,error[0]);
						return;
					}
					if (ti.getType() == Mode.ITERATION_INTERVAL)
						arg = Long.parseLong(ti.getArg());
					advancedDialog.showDialog(ti.getType(),arg);
				} else if (command.equals("REMOVE_SCRIPT")) 
					scriptSupport.removeScript(recordableScripts,nonRecordableScripts);
				else if (command.equals("TEST_SCRIPT")) {
					List<GeneratedRecordableInfo> dataSources = new ArrayList<GeneratedRecordableInfo>();
					for (Object o : recordableScripts.getSelectedValues()) {
						RecordableElement re = (RecordableElement) o;
						dataSources.add((GeneratedRecordableInfo)InfoConverter.convertRecordableElement2RecordableInfo(re));
					}
					ITestRunner runner = (ITestRunner) PlatformManager.getPlatform(PlatformSettings.getPlatformType());
					String msg = "Everything seems ok.";
					Throwable t = null;
					try {
						Pair<String,Throwable> result = runner.testRun(owner,dataSources);
						if (result != null) { 
							msg = result.getFirst() != null ? "The following problem occured during the test:\n" + result.getFirst() : null;
							t = result.getSecond();
						}	
					} catch (IOException e1) {
						msg = "I/O problem occured during the test:\n" + Util.getLocalizedMessage(e1);
						t = e1;
					}
					NetLogoTestResultsDialog dlg = new NetLogoTestResultsDialog(ParameterSweepWizard.getFrame(),msg,t);
					dlg.showDialog();
				}
			}
		}
	}

	//------------------------------------------------------------------------------
	public void caretUpdate(CaretEvent e) {
		if (e.getSource().equals(nameField) || e.getSource().equals(outputField)) 
			addRecorderButton.setEnabled(outputField.getText().trim().length() != 0 && nameField.getText().trim().length() != 0);
		else
			owner.enableDisableButtons();
	}

	//-------------------------------------------------------------------------------
	public void valueChanged(ListSelectionEvent e) {  
		TreePath path = recorderTree.getSelectionPath();
		if (path == null || (disableRectree && editedRecorder == null)) {
			addButton.setEnabled(false);
			addAsButton.setEnabled(false);
		} else {
			addButton.setEnabled(recordableTabbedPane.getSelectedIndex() != 3 && (recordableVariables.getSelectedIndices().length != 0 ||
								 recordableMethods.getSelectedIndices().length != 0 || recordableScripts.getSelectedIndices().length != 0));
			addAsButton.setEnabled(PlatformSettings.getGUIControllerForPlatform().hasAddAsOption() && recordableTabbedPane.getSelectedIndex() != 3
								   && (recordableVariables.getSelectedIndices().length != 0 || recordableMethods.getSelectedIndices().length != 0 ||
								   recordableScripts.getSelectedIndices().length != 0));
		}
		removeScriptButton.setEnabled(PlatformSettings.getGUIControllerForPlatform().isScriptingSupport() && 
								      (recordableScripts.getSelectedIndices().length != 0 || nonRecordableScripts.getSelectedIndices().length != 0));
		editScriptButton.setEnabled(PlatformSettings.getGUIControllerForPlatform().isScriptingSupport() && 
									((recordableTabbedPane.getSelectedIndex() == 2 && recordableScripts.getSelectedIndices().length != 0) ||
									(recordableTabbedPane.getSelectedIndex() == 3 && nonRecordableScripts.getSelectedIndices().length != 0)));
		testScriptButton.setEnabled(PlatformSettings.getGUIControllerForPlatform().isScriptingSupport() && 
									(PlatformManager.getPlatform(PlatformSettings.getPlatformType()) instanceof ITestRunner) &&
									 recordableTabbedPane.getSelectedIndex() == 2 && recordableScripts.getSelectedIndices().length != 0);
	}

	//-------------------------------------------------------------------------------
	public void valueChanged(TreeSelectionEvent e) { 
		TreePath path = recorderTree.getSelectionPath();
		if (path == null || disableRectree) {
			editRecorderAction.setEnabled(false);
			removeAction.setEnabled(false);
			if (path == null) {
				addButton.setEnabled(false);
				addAsButton.setEnabled(false);
			}
		} else {
			removeAction.setEnabled(true);
			editRecorderAction.setEnabled(true);
			addButton.setEnabled(recordableTabbedPane.getSelectedIndex() != 3 && (recordableVariables.getSelectedIndices().length != 0 ||
								 recordableMethods.getSelectedIndices().length != 0 || recordableScripts.getSelectedIndices().length != 0));
			addAsButton.setEnabled(PlatformSettings.getGUIControllerForPlatform().hasAddAsOption() && recordableTabbedPane.getSelectedIndex() != 3
								   && (recordableVariables.getSelectedIndices().length != 0 ||recordableMethods.getSelectedIndices().length != 0 ||
								   recordableScripts.getSelectedIndices().length != 0));

		}
	}
	
	//-------------------------------------------------------------------------------
	public void stateChanged(ChangeEvent e) { 
		TreePath path = recorderTree.getSelectionPath();
		if (path == null || (disableRectree && editedRecorder == null)) {
			addButton.setEnabled(false);
			addAsButton.setEnabled(false);
		} else {
			addButton.setEnabled(recordableTabbedPane.getSelectedIndex() != 3 && (recordableVariables.getSelectedIndices().length != 0 ||
								 recordableMethods.getSelectedIndices().length != 0 || recordableScripts.getSelectedIndices().length != 0));
			addAsButton.setEnabled(PlatformSettings.getGUIControllerForPlatform().hasAddAsOption() && recordableTabbedPane.getSelectedIndex() != 3
								   && (recordableVariables.getSelectedIndices().length != 0 ||recordableMethods.getSelectedIndices().length != 0 ||
								   recordableScripts.getSelectedIndices().length != 0));
		}
		createScriptButton.setVisible(PlatformSettings.getGUIControllerForPlatform().isScriptingSupport() && 
									  (recordableTabbedPane.getSelectedIndex() == 2 || recordableTabbedPane.getSelectedIndex() == 3));
		removeScriptButton.setVisible(PlatformSettings.getGUIControllerForPlatform().isScriptingSupport() && 
									  (recordableTabbedPane.getSelectedIndex() == 2 || recordableTabbedPane.getSelectedIndex() == 3));
		editScriptButton.setVisible(PlatformSettings.getGUIControllerForPlatform().isScriptingSupport() && 
									(recordableTabbedPane.getSelectedIndex() == 2 || recordableTabbedPane.getSelectedIndex() == 3));
		testScriptButton.setVisible(PlatformSettings.getGUIControllerForPlatform().isScriptingSupport() && 
									(PlatformManager.getPlatform(PlatformSettings.getPlatformType()) instanceof ITestRunner) &&
									 recordableTabbedPane.getSelectedIndex() == 2);
		testScriptButton.setEnabled(PlatformSettings.getGUIControllerForPlatform().isScriptingSupport() && 
									(PlatformManager.getPlatform(PlatformSettings.getPlatformType()) instanceof ITestRunner) &&
									recordableTabbedPane.getSelectedIndex() == 2 && recordableScripts.getSelectedIndices().length != 0);


	}
	
	//===============================================================================
	// GUI methods

	//-------------------------------------------------------------------------------
	private void layoutGUI() { 
		
		stopPanel = FormsUtils.build("p ~ p:g p:g ~ p",
									 "0011|" +
									 "22__|" +
									 "33__||" +
									 "4556",
									 iterationRadioButton,conditionRadioButton,
									 "      (constant or appropriate",
									 "        method or variable)",
									stopLabel,stopField,stopEEButton).getPanel();
									
		treePanel = FormsUtils.build("p:g ~ p ~ p ~ p ~ p:g",
									 "00000 f:p:g||" +
									 "_123_ p",
									 recorderScr,
									 newRecorderButton,editRecorderButton,removeButton).getPanel();
		
		generalPanel = FormsUtils.build("p ~ p:g ' p",
										"011||" +
										"234",
										" Name: ",nameField,
										" Output: ",outputField,outputBrowseButton).getPanel();

		whenPanel = FormsUtils.build("p ~ p:g ~ p ~ p",
									 "0000|" +
				                     "1111|" +
				                     "234_|" +
				                     "5555|" +
				                     "6666|" +
				                     "7889|",
				                     new Separator("Record"),
				                     whenIterationButton,
				                     whenIterationIntervalButton,whenIterationIntervalField,wiiLabel,
				                     whenRunRadioButton,
				                     whenConditionRadioButton,
				                     whenConditionLabel,whenConditionField,whenEEButton).getPanel();
		
		recordableTabbedPane.addTab("Variables",recordableVariableScr);
		recordableTabbedPane.addTab("Methods",recordableMethodScr);
		recordableTabbedPane.addChangeListener(this);
		
		recordablePanel = FormsUtils.build("p:g ~ p ~ p ~ p",
										   "_012 d||" +
										   "3333 p||" +
										   "4445|" +
										   "4446||" +
										   "4447||" +
										   "4448||" + 
										   "4449||" +
										   "444A||" +
										   "444_ f:p:g||" +
										   "444B p",
										   advancedButton,addRecorderButton,cancelButton,CellConstraints.RIGHT,
										   new Separator("Select recording related elements"),
										   recordableTabbedPane," ",
										   addButton,
										   addAsButton,
										   createScriptButton,
										   editScriptButton,
										   removeScriptButton,
										   testScriptButton).getPanel();

		editPanel = FormsUtils.build("p:g",
									 "0||" +
									 "1||" +
									 "2",
									 generalPanel,
									 whenPanel,
									 recordablePanel).getPanel();
		
		content = FormsUtils.build("~ f:p:g(0.4) f:p:g(0.6) ~",
								   "01|" +
								   "21 f:p:g|",
								   stopPanel,treePanel,
								   editPanel).getPanel();
		
		this.setLayout(new BorderLayout());
		final JScrollPane sp = new JScrollPane(content);
		this.add(sp,BorderLayout.CENTER);
		this.setPreferredSize(new Dimension(579,636));
		
		iterationRadioButton.setName("rbtn_wizard_record_stopiteration");
		conditionRadioButton.setName("rbtn_wizard_record_stopcondition");
		whenIterationButton.setName("rbtn_wizard_record_enditeration");
		whenIterationIntervalButton.setName("rbtn_wizard_record_afteriteration");
		whenRunRadioButton.setName("rbtn_wizard_record_endrun");
		whenConditionRadioButton.setName("rbtn_wizard_record_whencnd");
		stopField.setName("fld_wizard_record_stopfld");
		nameField.setName("fld_wizard_record_namefld");
		outputField.setName("fld_wizard_record_output");
		whenIterationIntervalField.setName("fld_wizard_record_afteriteration");
		whenConditionField.setName("fld_wizard_record_whencnd");
		stopEEButton.setName("btn_wizard_record_stopextended");
		whenEEButton.setName("btn_wizard_record_whenextended");
		outputBrowseButton.setName("btn_wizard_record_browseoutput");
		advancedButton.setName("btn_wizard_record_advanced");
		addRecorderButton.setName("btn_wizard_record_addrecorder");
		cancelButton.setName("btn_wizard_record_cancel");
		recordableTabbedPane.setName("pane_wizard_record_recordables");
		addButton.setName("btn_wizard_record_add");
		addAsButton.setName("btn_wizard_record_addas");
		createScriptButton.setName("btn_wizard_record_create");
		editScriptButton.setName("btn_wizard_record_edit");
		removeScriptButton.setName("btn_wizard_record_remove");
		recordableVariables.setName("lst_wizard_record_varlst");
		recordableMethods.setName("lst_wizard_record_methodlst");
		recordableScripts.setName("lst_wizard_record_datasrclst");
		nonRecordableScripts.setName("lst_wizard_record_misclst");
		recorderTree.setName("tree_wizard_record_recordertree");
		createScriptButton.setName("btn_wizard_record_createscript");
		removeScriptButton.setName("btn_wizard_record_removescript");
		editScriptButton.setName("btn_wizard_record_editscript");
		testScriptButton.setName("btn_wizard_record_testscript");
	}
	
	//-------------------------------------------------------------------------------
	private void initialize() { 
		stopPanel.setBorder(BorderFactory.createTitledBorder("Stop each run"));
		stopPanel.setPreferredSize(new Dimension(300,110));
		
		ai.aitia.meme.utils.GUIUtils.createButtonGroup(iterationRadioButton,conditionRadioButton);
		iterationRadioButton.setActionCommand("STOP_ITERATION");
		conditionRadioButton.setActionCommand("STOP_CONDITION");

		stopField.addCaretListener(this);
		stopEEButton.setEnabled(false);
		
		nameField.setPreferredSize(new Dimension(150,20));
		outputField.setPreferredSize(new Dimension(130,20));
		outputBrowseButton.setActionCommand("OUTPUT_BROWSE");
		outputBrowseButton.setPreferredSize(new Dimension(20,20));
		outputBrowseButton.setToolTipText("Can be used only at local simulation running.");
		
		whenIterationButton.setActionCommand("WHEN_ITERATION");
		whenIterationIntervalButton.setActionCommand("WHEN_ITERATION_INTERVAL");
		whenRunRadioButton.setActionCommand("WHEN_RUN");
		whenConditionRadioButton.setActionCommand("WHEN_CONDITION");
		
		advancedButton.setActionCommand("ADVANCED");
		addRecorderButton.setActionCommand("ADD_OR_MODIFY");
		cancelButton.setActionCommand("CANCEL");
		
		recordableVariables.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		recordableMethods.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		recordableScripts.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		recordableScripts.setModel(new DefaultListModel());
		nonRecordableScripts.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		nonRecordableScripts.setModel(new DefaultListModel());
		
		treePanel.setBorder(BorderFactory.createTitledBorder("Recorders"));
		
		recorderScr.setPreferredSize(new Dimension(200,400));
		recorderScr.setBorder(null);
		recorderTree.setRootVisible(false);
		recorderTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		recorderTree.setCellRenderer(new RecorderTreeRenderer());
		recorderTree.addTreeSelectionListener(this);
		
		recorderTree.addMouseListener(new MouseAdapter() {
			JPopupMenu contextMenu = null;
			@Override
			public void mouseClicked(MouseEvent e) {
				DefaultMutableTreeNode node = mouseOnNode(e);
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 &&
					node != null && !disableRectree)
					hybridAction(null,editRecorderAction);
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e) && e.getComponent().isEnabled()) {
					if (contextMenu == null) {
						contextMenu = new JPopupMenu();
						contextMenu.setName("cmenu_wizard_record_recorderscmenu");
						contextMenu.add(newRecorderAction);
						contextMenu.getComponent(contextMenu.getComponentCount()-1).setName("btn_newrecorder");
						contextMenu.add(editRecorderAction);
						contextMenu.getComponent(contextMenu.getComponentCount()-1).setName("btn_editrecorder");
						contextMenu.addSeparator();
						contextMenu.add(removeAction);
						contextMenu.getComponent(contextMenu.getComponentCount()-1).setName("btn_removerecorder");
					}
					DefaultMutableTreeNode node = mouseOnNode(e);
					if (node != null) 
						recorderTree.setSelectionPath(new TreePath(node.getPath()));
					else
						recorderTree.setSelectionPath(recorderTree.getPathForRow(recorderTree.getRowCount() - 1));
					editRecorderAction.setEnabled(!disableRectree);
					removeAction.setEnabled(!disableRectree);
					newRecorderAction.setEnabled(!disableRectree);
					contextMenu.show(e.getComponent(),e.getX(),e.getY());
				}
			}
			private DefaultMutableTreeNode mouseOnNode(MouseEvent e) {
				Point loc = e.getPoint();
				TreePath path = recorderTree.getPathForLocation(20,loc.y);
				if (path != null)
					return (DefaultMutableTreeNode) path.getLastPathComponent();
				return null;
			}
		});
		
		editPanel.setBorder(BorderFactory.createTitledBorder("Recorder settings"));
		
		nameField.addCaretListener(this);
		outputField.addCaretListener(this);
		
		ai.aitia.meme.utils.GUIUtils.createButtonGroup(whenRunRadioButton,whenIterationButton,whenIterationIntervalButton,whenConditionRadioButton);
		
		recordableTabbedPane.setPreferredSize(new Dimension(200,260));
		recordableTabbedPane.setToolTipTextAt(0,"Recordable variables of the model.");
		recordableTabbedPane.setToolTipTextAt(1,"Recordable methods of the model.");
		
		recordableMethodScr.setBorder(null);
		recordableVariableScr.setBorder(null);
		recordableScriptScr.setBorder(null);
		nonRecordableScriptScr.setBorder(null);
	
		GUIUtils.ToggleClickMultiSelection ms = new GUIUtils.ToggleClickMultiSelection() {
	    	@Override public void mousePressed(java.awt.event.MouseEvent e) {
	    		if (recordableTabbedPane.getSelectedIndex() >= 2 && e.getClickCount() == 2)
	    			editScriptButton.doClick(0);
	    	}
		};
		
		recordableVariables.addMouseListener(ms);
		recordableMethods.addMouseListener(ms);
		recordableScripts.addMouseListener(ms);
		nonRecordableScripts.addMouseListener(ms);
		recordableVariables.addListSelectionListener(this);
		recordableMethods.addListSelectionListener(this);
		recordableScripts.addListSelectionListener(this);
		nonRecordableScripts.addListSelectionListener(this);
				
		addButton.setActionCommand("ADD");
		addAsButton.setActionCommand("ADD_AS");
		createScriptButton.setActionCommand("CREATE");
		createScriptButton.setVisible(false);
		editScriptButton.setActionCommand("EDIT_SCRIPT");
		editScriptButton.setVisible(false);
		removeScriptButton.setActionCommand("REMOVE_SCRIPT");
		removeScriptButton.setVisible(false);
		testScriptButton.setActionCommand("TEST_SCRIPT");
		testScriptButton.setVisible(false);
		
		ai.aitia.meme.utils.GUIUtils.addActionListener(this,iterationRadioButton,conditionRadioButton,whenIterationButton,
													   whenIterationIntervalButton,whenRunRadioButton,whenConditionRadioButton,addButton,addAsButton,
													   addRecorderButton,cancelButton,advancedButton,createScriptButton,outputBrowseButton,
				   									   removeScriptButton,editScriptButton,testScriptButton);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializePageForPlatform() {
		IGUIController controller = PlatformSettings.getGUIControllerForPlatform();
		
		if (!controller.isStopFixIntervalEnabled() && !controller.isStopConditionEnabled()) 
			throw new IllegalStateException("Platform " + PlatformSettings.getPlatformType().toString() + " does not support any type of " +
											"stopping condition.");
			
		if (!controller.isRecordEveryIterationEnabled() && !controller.isRecordNIterationEnabled() && !controller.isRecordEndOfTheRunsEnabled() &&
			!controller.isRecordConditionEnabled()) 
			throw new IllegalStateException("Platform " + PlatformSettings.getPlatformType().toString() + " does not support any type of " +
											"recording condition.");
		
		iterationRadioButton.setEnabled(controller.isStopFixIntervalEnabled());
		conditionRadioButton.setEnabled(controller.isStopConditionEnabled());
			
		if (controller.isStopFixIntervalEnabled() && !controller.isStopConditionEnabled()) {
			iterationRadioButton.setSelected(true);
			if (stopField.getText().trim().length() == 0)
				stopField.setText("100");
			setStopLabel("STOP_ITERATION");
		} else if (!controller.isStopFixIntervalEnabled() && controller.isStopConditionEnabled()) {
			conditionRadioButton.setSelected(true);
			setStopLabel("STOP_CONDITION");
		}
		
		if (controller.isRecordEveryIterationEnabled())
			whenIterationButton.setSelected(true); 
		else if (controller.isRecordNIterationEnabled()) {
			whenIterationIntervalButton.setSelected(true);
			whenIterationIntervalField.setEnabled(true);
			wiiLabel.setEnabled(true);
		} else if (controller.isRecordEndOfTheRunsEnabled())
			whenRunRadioButton.setSelected(true);
		else {
			whenConditionRadioButton.setSelected(true);
			whenConditionLabel.setEnabled(true);
			whenConditionField.setEnabled(true);
			whenEEButton.setEnabled(true);
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Resets the page. */
	private void resetPage() {
		IGUIController controller = PlatformSettings.getGUIControllerForPlatform();
		disableRectree = false;
		if (controller.isStopFixIntervalEnabled()) {
			iterationRadioButton.setSelected(true);
			stopField.setText("100");
		} else {
			conditionRadioButton.setSelected(true);
			stopField.setText("");
		}
		enableDisableSettings(false);
		addRecorderButton.setEnabled(false);
		resetSettings();
		root = new DefaultMutableTreeNode();
		recorderTree.setModel(new DefaultTreeModel(root));
		editRecorderAction.setEnabled(false);
		removeAction.setEnabled(false);
	}
	
	//-------------------------------------------------------------------------------
	/** Enables/disables the recorder editing components of the page according to the
	 *  value of <code>enabled</code>.
	 */
	private void enableDisableSettings(boolean enabled) { 
		nameField.setEnabled(enabled);
		outputField.setEnabled(enabled);
		outputBrowseButton.setEnabled(enabled && ParameterSweepWizard.getPreferences().isLocalRun());
		whenIterationButton.setEnabled(enabled && PlatformSettings.getGUIControllerForPlatform().isRecordEveryIterationEnabled());
		whenIterationIntervalButton.setEnabled(enabled && PlatformSettings.getGUIControllerForPlatform().isRecordNIterationEnabled());
		wiiLabel.setEnabled(enabled && enabled && PlatformSettings.getGUIControllerForPlatform().isRecordNIterationEnabled());
		whenRunRadioButton.setEnabled(enabled && PlatformSettings.getGUIControllerForPlatform().isRecordEndOfTheRunsEnabled());
		whenConditionRadioButton.setEnabled(enabled && PlatformSettings.getGUIControllerForPlatform().isRecordConditionEnabled());
		whenConditionLabel.setEnabled(enabled && PlatformSettings.getGUIControllerForPlatform().isRecordConditionEnabled());
		advancedButton.setEnabled(enabled && isAdvancedButtonEnabled());
		cancelButton.setEnabled(enabled);
	}
	
	//-------------------------------------------------------------------------------
	/** Resets the recorder editing components. */
	private void resetSettings() { 
		IGUIController controller = PlatformSettings.getGUIControllerForPlatform();
		nameField.setText("");
		outputField.setText("");
		outputField.setToolTipText("");
		outputBrowseButton.setEnabled(ParameterSweepWizard.getPreferences().isLocalRun());
		whenIterationIntervalField.setEnabled(false);
		whenIterationIntervalField.setText("");
		whenConditionField.setText("");
		whenConditionField.setEnabled(false);
		whenEEButton.setEnabled(false);
		if (controller.isRecordEveryIterationEnabled())
			whenIterationButton.setSelected(true); 
		else if (controller.isRecordNIterationEnabled()) {
			whenIterationIntervalButton.setSelected(true);
			whenIterationIntervalField.setEnabled(true);
			wiiLabel.setEnabled(true);
		} else if (controller.isRecordEndOfTheRunsEnabled())
			whenRunRadioButton.setSelected(true);
		else {
			whenConditionRadioButton.setSelected(true);
			whenConditionLabel.setEnabled(true);
			whenConditionField.setEnabled(true);
			whenEEButton.setEnabled(true);
		}
		recordableTabbedPane.setSelectedIndex(0);
		recordableVariables.clearSelection();
		recordableMethods.clearSelection();
		recordableScripts.clearSelection();
		nonRecordableScripts.clearSelection();
		addButton.setEnabled(false);
		addAsButton.setEnabled(false);
		removeScriptButton.setEnabled(false);
		editScriptButton.setEnabled(false);
		testScriptButton.setEnabled(false);
		addRecorderButton.setText(" Add recorder ");
	}
	
	//-------------------------------------------------------------------------------
	/** Disables the recoders tree. */
	private void disableRecTree() {
		disableRectree = true;
		newRecorderAction.setEnabled(false);
		editRecorderAction.setEnabled(false);
		removeAction.setEnabled(false);
	}
	
	//===============================================================================
	// private methods
	
	//----------------------------------------------------------------------------------------------------
	private boolean isNewParameter(RecordableInfo ri) {
		if (PlatformSettings.getGUIControllerForPlatform().canNewParametersAlsoRecordables()) return false; // we don't care
		List<ParameterInfo> newParameters = owner.getNewParameters_internal();
		if (!PlatformSettings.getGUIControllerForPlatform().isNewParametersEnabled() || newParameters == null || newParameters.size() == 0)
			return false;
		String name = null;
		if (InfoConverter.isVariable(ri))
			name = ri.getName();
		else if (ri.getAccessibleName().startsWith("get"))
			name = ri.getAccessibleName().substring(3,ri.getAccessibleName().length() - 2);
		if (name == null) return false;
		ParameterInfo dummy = new ParameterInfo(Util.capitalize(name),"",null);
		if (newParameters.contains(dummy)) return true;
		dummy = new ParameterInfo(Util.uncapitalize(name),"",null);
		return newParameters.contains(dummy);
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isAdvancedButtonEnabled() {
		IGUIController controller = PlatformSettings.getGUIControllerForPlatform();
		int count = 0;
		if (controller.isWriteEveryRecordingEnabled()) 
			count++;
		if (controller.isWriteNIterationEnabled())
			count++;
		if (controller.isWriteEndOfTheRunsEnabled())
			count++;
		return count > 1;
	}
	
	//-------------------------------------------------------------------------------
	/** Changes the label of the simulation stopping condition field. */
	private void setStopLabel(String command) {
		if (command.equals("STOP_ITERATION"))
			stopLabel.setText("       Value: ");
		else
			stopLabel.setText(" Condition: ");
	}
	
	//-------------------------------------------------------------------------------
	/** Returns whether the recorder name <code>name</code> is new or is already used
	 *  by an other recorder.
	 */
	private boolean isNewName(String name) {
		for (int i = 0;i < root.getChildCount();++i) {
			String recName = root.getChildAt(i).toString();
			if (name.equals(recName)) return false;
		}
		return true;
	}
	
	//----------------------------------------------------------------------------------------------------
	private List<String> invalidRecorderPath() {
		List<String> result = new ArrayList<String>();
		if (ParameterSweepWizard.getPreferences().isLocalRun())
			return result;
		for (int i = 0;i < root.getChildCount();++i) {
			DefaultMutableTreeNode recorderNode = (DefaultMutableTreeNode) root.getChildAt(i);
			DefaultMutableTreeNode resultNode = (DefaultMutableTreeNode) recorderNode.getChildAt(0);
			ResultInfo ri = (ResultInfo) resultNode.getUserObject();
			if (ri.getFile().contains("\\") || ri.getFile().contains("/"))
				result.add(recorderNode.toString());
		}
		return result;
	}
	
	//-------------------------------------------------------------------------------
	/** Checks whether the simulation stopping condition is valid or not. */
	private boolean checkEndOfSimulation() { 
		String candidate = new String(stopField.getText().trim());
		if ("".equals(candidate)) {
			Utilities.userAlert(owner,"Stop condition is empty.");
			return false;
		}
		if (iterationRadioButton.isSelected()) { // fix iteration
			try {
				double d = Double.parseDouble(candidate); // fix iteration
				if (d <= 0) {
					Utilities.userAlert(owner,"Number of iterations must be greater than 0.");
					return false;
				}
				return true;
			} catch (NumberFormatException e) {
				String error = null;
				// variable or method
				MemberInfo candidateInfo = new MemberInfo(candidate,"",null);
				RecordableElement candidateElement = new RecordableElement(candidateInfo);
				DefaultListModel listModel = (DefaultListModel) recordableVariables.getModel();
				int index = listModel.indexOf(candidateElement);
				if (index != -1) { // there is variable with the same name
					MemberInfo mi = ((RecordableElement)listModel.get(index)).getInfo();
					if (iterationTypeNames.contains(mi.getType())) return true;
					else 
						error = " is an inappropriate variable.";
				}
				
				if (PlatformSettings.getPlatformType() == PlatformType.NETLOGO || PlatformSettings.getPlatformType() == PlatformType.NETLOGO5)
					candidate += " ";
				else if (!candidate.endsWith("()"))
					candidate += "()";
				candidateInfo.setName(candidate);
				listModel = (DefaultListModel) recordableMethods.getModel();
				index = listModel.indexOf(candidateElement);
				if (index != -1) { // there is a method with the same name
					MemberInfo mi = ((RecordableElement)listModel.get(index)).getInfo();
					if (iterationTypeNames.contains(mi.getType())) return true;
					else {
						error = stopField.getText().trim().endsWith("()") ? "" : "()";
						error += " is an inappropriate method.";
					}
				}

				listModel = (DefaultListModel) recordableScripts.getModel();
				index = listModel.indexOf(candidateElement);
				if (index != -1) { // there is a script with the same name
					MemberInfo mi = ((RecordableElement)listModel.get(index)).getInfo();
					if (iterationTypeNames.contains(mi.getType())) return true;
					else {
						error = stopField.getText().trim().endsWith("()") ? "" : "()";
						error += " is an inappropriate method.";
					}
				}
				
				if (error == null)
					error = " is an unknown name or a wrong constant value.";
				Utilities.userAlert(owner,stopField.getText().trim() + error);
				return false;
			}
		} else { // condition
			String error = PlatformManager.getPlatform(PlatformSettings.getPlatformType()).checkCondition(candidate,owner);
			if (error == null) return true;
			Utilities.userAlert(owner,error);
			return false;
		}
	}
	
	//------------------------------------------------------------------------------
	/** Creates a {@link ai.aitia.meme.paramsweep.gui.info.TimeInfo TimeInfo} object
	 *  from the settings of the new recorder. A TimeInfo object represents the recording
	 *  time or conditon (and the writing time) of a recorder.
	 * @param error output parameter for the error message (if any)
	 */
	private TimeInfo getTimeInfo(String[] error) { 
		if (whenIterationButton.isSelected())
			return new TimeInfo(Mode.ITERATION);
		else if (whenRunRadioButton.isSelected())
			return new TimeInfo(Mode.RUN);
		else if (whenIterationIntervalButton.isSelected()) {
			String no = whenIterationIntervalField.getText().trim();
			if (no.length() == 0) {
				error[0] = "The selected recording time option is incomplete: empty field.";
				return null;
			}
			try {
				long l = Long.parseLong(no);
				if (l < 1)
					throw new NumberFormatException();
			} catch (NumberFormatException e) {
				error[0] = "The selected recording time option is wrong: the contain of the field is not a positive integer.";
				return null;
			}
			return new TimeInfo(Mode.ITERATION_INTERVAL,no);
		} else {
			String condition = whenConditionField.getText().trim();
			String condError = PlatformManager.getPlatform(PlatformSettings.getPlatformType()).checkCondition(condition,owner);
			if (condError != null) {
				error[0] = condError;
				return null;
			}
			return new TimeInfo(Mode.CONDITION,condition);
		}
	}
	
	//------------------------------------------------------------------------------
	/** Sets the writing mode (which is set in the Advanced dialog) in a 
	 *  {@link ai.aitia.meme.paramsweep.gui.info.TimeInfo TimeInfo} object.
	 */
	private void applyAdvancedSettings(TimeInfo info) {
		if (advancedDialog == null)
			return;
		long[] arg = new long[1];
		WriteMode mode = advancedDialog.getWriteType(arg);
		mode = postCheck(info,mode,arg[0]);
		info.setWriteMode(mode,arg[0]);
	}
	
	//------------------------------------------------------------------------------
	/** Checks whether the writing time <code>orig</code> and the recording time (or
	 *  condition) in <code>info</code> are compatible or not. If not the writing time
	 *  is changed.
	 * @param arg the argument of the writing time (if any)
	 * @return the original or the modified writing time
	 */
	private WriteMode postCheck(TimeInfo info, WriteMode orig, long arg) {
		boolean mustChange = false;
		switch(info.getType()) {
		case RUN				: 
		case CONDITION			: mustChange = (orig == WriteMode.ITERATION_INTERVAL);
								  break;
		case ITERATION_INTERVAL : mustChange = (arg > 0 && arg < Long.parseLong(info.getArg()));
		}
		return (mustChange ? WriteMode.RECORD : orig);
	}
	
	//-------------------------------------------------------------------------------
	/** Checks whether the output file specified by <code>file</code> is already
	 *  assigned to an other recorder or not.
	 */
	private boolean isAlreadyAssigned(String file) {
		if (root.getChildCount() == 0) return false;
		for (int i = 0;i < root.getChildCount();++i) {
			DefaultMutableTreeNode recorder = (DefaultMutableTreeNode) root.getChildAt(i);
			if (recorder.equals(editedRecorder)) continue;
			DefaultMutableTreeNode resNode = (DefaultMutableTreeNode) recorder.getFirstChild();
			ResultInfo ri = (ResultInfo) resNode.getUserObject();
			if (file.trim().equals(ri.getFile().trim())) return true;
		}
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isValidRecorderFile(final String file) {
		for (final char c : file.toCharArray()) {
			if (!Character.isLetterOrDigit(c) && c != '_' && c !='.') return false;
		}
		return true;
	}
	
	//-------------------------------------------------------------------------------
	/** Returns whether the recorder belongs to <code>node</code> contains the
	 *  recodable element <code>info</code> as a child or not.
	 */
	private boolean contains(DefaultMutableTreeNode node, MemberInfo info) {
		for (int i = 2;i < node.getChildCount();++i) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
			RecordableElement re = (RecordableElement) child.getUserObject();
			if (info.equals(re.getInfo())) return true;
		}
		return false;
	}
	
	//-------------------------------------------------------------------------------
	/** Generates a unique recorder name. */
	private String generateName() {
		List<Integer> nums = new ArrayList<Integer>();
		for (int i = 0;i < root.getChildCount();++i) {
			String candidate = root.getChildAt(i).toString().toLowerCase();
			if (candidate.startsWith("recorder")) {
				String num = candidate.substring(8);
				try {
					nums.add(new Integer(num));
				} catch (NumberFormatException e) {}
			}
		}
		Collections.sort(nums);
		return nums.size() == 0 ? "Recorder0" : "Recorder" + String.valueOf((nums.get(nums.size() - 1) + 1));
	}
	
	//-------------------------------------------------------------------------------
	/** Edits the recorder that belongs to <code>node</code> so this method fills the appropriate
	 *  components with the informations of the recorder.
	 */
	private void edit(DefaultMutableTreeNode recorder) {
		editedRecorder = recorder;
		nameField.setText(recorder.toString());
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) recorder.getFirstChild();
		ResultInfo ri = (ResultInfo) node.getUserObject();
		outputField.setText(ri.getFile());
		outputField.setToolTipText(ri.getFile());
		node = (DefaultMutableTreeNode) recorder.getChildAt(1);
		TimeInfo ti = (TimeInfo) node.getUserObject();
		switch (ti.getType()) {
		case ITERATION			: whenIterationButton.doClick();
								  break;
		case ITERATION_INTERVAL : whenIterationIntervalButton.doClick();
								  whenIterationIntervalField.setText(ti.getArg());
								  break;
		case RUN				: whenRunRadioButton.doClick();
								  break;
		case CONDITION			: whenConditionRadioButton.doClick();
								  whenConditionField.setText(ti.getArg());
		}
		nameField.grabFocus();
		addRecorderButton.setEnabled(true);
		advancedDialog = new WriteToFileDialog(ParameterSweepWizard.getFrame(),
											   ti.getWriteType(),ti.getWriteArg());
	}
	
	//------------------------------------------------------------------------------
	/** Notifies all listeners of the recorder tree that the nodes of the tree are
	 *  changed.
	 */
	private void fireTreeNodesChanged(TreePath path) {
        // Guaranteed to return a non-null array
        Object[] listeners = ((DefaultTreeModel)recorderTree.getModel()).getListeners(TreeModelListener.class);
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 1;i >= 0;i--) {
        	// Lazily create the event:
            if (e == null)
                e = new TreeModelEvent(this, path);
            ((TreeModelListener)listeners[i]).treeNodesChanged(e);
        }
	}
	
	//------------------------------------------------------------------------------
	/** Creates the recorder tree from an XML document.
	 * @param recordersElement the node of the XML document that belongs to the recorders
	 * @throws WizardLoadingException if the XML document is invalid
	 */
	private void createRecorderTree(Element recordersElement) throws WizardLoadingException {  
		// clear tree
		root = new DefaultMutableTreeNode();
		DefaultTreeModel treeModel = new DefaultTreeModel(root); 
		recorderTree.setModel(treeModel);
		
		NodeList nodes = recordersElement.getElementsByTagName(WizardSettingsManager.RECORDER);
		if (nodes == null)
			return;
		for (int i = 0;i < nodes.getLength();++i) {
			Element recorder = (Element) nodes.item(i);
			String name = recorder.getAttribute(WizardSettingsManager.NAME);
			if (name.equals(""))
				throw new WizardLoadingException(true,"missing 'name' attribute at node: " + WizardSettingsManager.RECORDER);
			DefaultMutableTreeNode recorderTreeNode = new DefaultMutableTreeNode(name);
			treeModel.insertNodeInto(recorderTreeNode,root,i);
			
			NodeList nl = recorder.getElementsByTagName(WizardSettingsManager.OUTPUT_FILE);
			if (nl == null || nl.getLength() == 0)
				throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.OUTPUT_FILE);
			NodeList content = nl.item(0).getChildNodes();
			if (content == null || content.getLength() == 0)
				throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.OUTPUT_FILE);
			String output = ((Text)content.item(0)).getNodeValue();
			ResultInfo ri = new ResultInfo(output.trim());
			DefaultMutableTreeNode outputNode = new DefaultMutableTreeNode(ri);
			treeModel.insertNodeInto(outputNode,recorderTreeNode,0);
			
			nl = null;
			nl = recorder.getElementsByTagName(WizardSettingsManager.TIME);
			if (nl == null || nl.getLength() == 0)
				throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.TIME);
			Element timeElement = (Element) nl.item(0);
			String type = timeElement.getAttribute(WizardSettingsManager.TYPE);
			if (type == null || type.equals(""))
				throw new WizardLoadingException(true,"missing 'type' attribute at node: " + WizardSettingsManager.TIME);
			TimeInfo ti = null;
			try {
				if (type.equals("ITERATION") || type.equals("RUN"))
					ti = new TimeInfo(type);
				else {
					content = null;
					content = timeElement.getChildNodes();
					if (content == null || content.getLength() == 0)
						throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.TIME);
					String arg = ((Text)content.item(0)).getNodeValue();
					ti = new TimeInfo(type,arg);
				}
			} catch (IllegalArgumentException e) {
				throw new WizardLoadingException(true,"invalid 'type' attribute at node: " + WizardSettingsManager.TIME);
			}
			
			nl = null;
			nl = recorder.getElementsByTagName(WizardSettingsManager.WRITING_TIME);
			if (nl == null || nl.getLength() == 0)
				throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.WRITING_TIME);
			Element writingElement = (Element) nl.item(0);
			type = null;
			type = writingElement.getAttribute(WizardSettingsManager.TYPE);
			if (type == null || type.equals(""))
				throw new WizardLoadingException(true,"missing 'type' attribute at node: " + WizardSettingsManager.WRITING_TIME);
			long arg = -1;
			if (type.equals("ITERATION_INTERVAL")) {
				content = null;
				content = writingElement.getChildNodes();
				if (content == null || content.getLength() == 0)
					throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.WRITING_TIME);
				try {
					arg = Long.parseLong(((Text)content.item(0)).getNodeValue());
				} catch (NumberFormatException e) {
					throw new WizardLoadingException(true,"invalid content at node: " + WizardSettingsManager.WRITING_TIME);
				}
			}
			try {
				ti.setWriteMode(type,arg);
			} catch (IllegalArgumentException e) {
				throw new WizardLoadingException(true,"invalid content at node: " + WizardSettingsManager.WRITING_TIME);
			}
			
			DefaultMutableTreeNode timeNode = new DefaultMutableTreeNode(ti);
			treeModel.insertNodeInto(timeNode,recorderTreeNode,1);
			
			nl = null;
			nl = recorder.getElementsByTagName(WizardSettingsManager.MEMBER);
			if (nl == null) continue;
			InfoConverter.loadRecorderElementsFromXml(owner.getClassLoader(),treeModel,recorderTreeNode,nl,scriptSupport);
			recorderTree.expandPath(new TreePath(recorderTreeNode.getPath()));
		}
		
		if (root.getChildCount() > 0){
			recorderTree.setSelectionPath(new TreePath(((DefaultMutableTreeNode)root.getChildAt(0)).getPath()));
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private List<String> emptyRecorders() {
		List<String> result = new ArrayList<String>();
		for (int i = 0;i < root.getChildCount();++i) {
			DefaultMutableTreeNode recorder = (DefaultMutableTreeNode) root.getChildAt(i);
			if (recorder.getChildCount() <= 2)
				result.add(recorder.toString());
		}
		return result;
	}
	
	//===============================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	/** This class renders the recorder tree. */
	@SuppressWarnings("serial")
	private class RecorderTreeRenderer extends DefaultTreeCellRenderer {
		
		//====================================================================================================
		// members
		
		private ImageIcon recorderIcon 	= ExpandedEditor.getIcon("recorder.png");
		private ImageIcon timeIcon	   	= ExpandedEditor.getIcon("time.png");
		private ImageIcon conditionIcon	= ExpandedEditor.getIcon("warning.png");
		private ImageIcon variableIcon	= ExpandedEditor.getIcon("model_version.png");
		private ImageIcon methodIcon	= ExpandedEditor.getIcon("model_name.png");
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row,
													  boolean hasFocus) {
			Component com = super.getTreeCellRendererComponent(tree,value,sel,expanded,leaf,row,hasFocus);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			Object userObject = node.getUserObject();
			Icon icon = null;
			if (userObject instanceof String) {
				icon = recorderIcon;
				String name = (String) userObject;
				if (node.getChildCount() <= 2)
					((JLabel)com).setText("<html><font color=grey><i>" + name + "</i></font></html>");
			} else if (userObject instanceof TimeInfo) {
				TimeInfo ti = (TimeInfo) userObject;
				if (ti.getType() == Mode.CONDITION)
					icon = conditionIcon;
				else 
					icon = timeIcon;
			} else if (userObject instanceof RecordableElement) {
				RecordableElement re = (RecordableElement) userObject;
				if (re.getInfo().getName().endsWith(")"))
					icon = methodIcon;
				else
					icon = variableIcon;
			}
			if (icon != null) 
				((JLabel)com).setIcon(icon);
			tree.setToolTipText(value.toString());
			((JLabel)com).setToolTipText(value.toString());
			return com;
		}
	}
}
