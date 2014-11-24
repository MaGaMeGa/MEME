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
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

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
import ai.aitia.meme.paramsweep.gui.info.MasonChooserParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.internal.platform.IGUIController;
import ai.aitia.meme.paramsweep.internal.platform.IGUIController.RunOption;
import ai.aitia.meme.paramsweep.internal.platform.InfoConverter;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.utils.ParameterEnumeration;
import ai.aitia.meme.paramsweep.utils.ParameterParserException;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.FormsUtils.Separator;
import ai.aitia.meme.utils.GUIUtils;

import com.jgoodies.forms.layout.CellConstraints;

import edu.thirdparty.DnDTree;
import edu.thirdparty.DnDTree.DnDTreeCellRenderer;
import edu.thirdparty.DnDTree.DnDTreeDropSuccessfulEvent;
import edu.thirdparty.DnDTree.DnDTreeDropSuccessfulListener;

/** This class provides the Parameters page of the Parameter Sweep Wizard. */
public class Page_Parameters extends JPanel implements IWizardPage,
													   IArrowsInHeader,
													   ActionListener,
													   IHybridActionListener,
													   TreeSelectionListener,
													   FocusListener,
													   DnDTreeDropSuccessfulListener {
	
	//===============================================================================
	// members

	private static final long serialVersionUID = 1L;
	
	private static final String orderPart1 = "Select a parameter from the tree";
	private static final String orderPart2 = "to modify its settings.";
	
	/** The owner of the page. */
	private ParameterSweepWizard owner = null;
	/** The list of information objects of the parameters. */ 
	private List<ParameterInfo> parameters = null;
	/** The names of the parameters. */
	private String[] initParamResult = null;
	/** The names of the parameters of the original model. */
	private String[] originalInitParamResult = null;
	/** The list of information objects of the new (defined by this wizard) parameters. */
	private List<ParameterInfo> newParameters = null;
	
	HybridAction moveUpAction = new HybridAction(this,"Move up",null);
	HybridAction moveDownAction = new HybridAction(this,"Move down",null);
	HybridAction editAction = new HybridAction(this,"Edit",null);
	
	/** The edited node of the parameter tree. */
	private DefaultMutableTreeNode editedNode = null;
	
	private IntelliContext intelliContext = null;
	private boolean modelInformationException = false;
	
	//===============================================================================
	// GUI members
	
	private JPanel content = null;
	private JPanel left = null;
	private JPanel leftFirst = new JPanel(new BorderLayout());
	private JPanel leftSecond = new JPanel();
	private JLabel text1 = new JLabel(orderPart1);
	private JLabel text2 = new JLabel(orderPart2);
	private JLabel runsLabel = new JLabel("Runs: ");
	private JTextField runsField = new JTextField("");
	private JRadioButton constDef = new JRadioButton("Constant");
	private JRadioButton listDef = new JRadioButton("List");
	private JRadioButton incrDef = new JRadioButton("Increment");
	private JPanel leftTop = null;
	private JButton modifyButton = new JButton("Modify");
	private JButton cancelButton = new JButton("Cancel");
	private JButton addEnumButton = new JButton(">>");
	private JButton removeEnumButton = new JButton("<<");
	private JTextField constDefField = new JTextField();
	private JPanel constDefPanel = null;
	private JPanel enumListPanel = null;
	private JPanel enumConstPanel =null;
	private JButton browseFileButton = new JButton(FileSystemView.getFileSystemView().getSystemIcon(new File(".")));
	private JTextArea listDefArea = new JTextArea();
	private DefaultListModel<String> leftEnumValueModel= new DefaultListModel<String>();
	private DefaultListModel<String> rightEnumValueModel = new DefaultListModel<String>();
	private DefaultComboBoxModel<String> enumComboBoxModel = new DefaultComboBoxModel<String>();
	private JList leftEnumValueList = new JList<String>(leftEnumValueModel);
	private JList rightEnumValueList = new JList<String>(rightEnumValueModel);
	private JScrollPane listDefScr = new JScrollPane(listDefArea,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	private JScrollPane leftEnumValuePane = new JScrollPane(leftEnumValueList,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	private JScrollPane RightEnumValuePane = new JScrollPane(rightEnumValueList,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	private JComboBox<String> enumComboBox = new JComboBox<String>(enumComboBoxModel);
	private JPanel listDefPanel = null;
	private JTextField incrStartValueField = new JTextField();
	private JTextField incrEndValueField = new JTextField();
	private JTextField incrStepField = new JTextField();
	private JPanel incrDefPanel = null;
	private JPanel leftMiddle = new JPanel(new CardLayout());
	private JPanel leftBottom = null;
	private JButton newParameterButton = new JButton("Add new parameters...");
	private DefaultMutableTreeNode root = new DefaultMutableTreeNode("Parameter file");
	private DnDTree outputTree = new DnDTree(new DefaultTreeModel(root)) {
		private static final long serialVersionUID = -3266082500122393494L;
		@Override
		protected void illegalDrop(Exception e) {
			Utilities.userAlert(this,"Swap not supported.");
		}
		//@Override
		protected boolean mouseReleasedCondition() {
			return editedNode == null || modify();
		}
		
	};
	private JScrollPane treeScr = new JScrollPane(outputTree,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	private JButton moveUpButton = new JButton(moveUpAction);
	private JButton moveDownButton = new JButton(moveDownAction);
	private JButton editButton = new JButton(editAction);
	private JPanel right = null;
	private JButton newParameterButtonCopy = new JButton("Add new parameters...");
	private JLabel numberOfRunsLabel = new JLabel(" A total of 1 run.");
	
	//===============================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/** Constructor.
	 * @param owner the owner of the page
	 */
	public Page_Parameters(ParameterSweepWizard owner) {
		this.owner = owner;
		outputTree.setToggleClickCount(5);
		layoutGUI();
		initialize();
	}
	
	//-------------------------------------------------------------------------------
	public List<ParameterInfo> getParameters() { return parameters; }
	/** Returns the root node of the parameters tree. */
	public DefaultMutableTreeNode getParameterTreeRoot() { return root; }
	public String[] getInitParamResult() { return initParamResult; }
	public List<ParameterInfo> getNewParametersList() { return newParameters; }
	public JButton getNewParametersButton(){ return newParameterButtonCopy; }
	
	//-------------------------------------------------------------------------------
	/** Checks whether a constant parameter has child in the tree which results 
	 *  an illegal parameter file. 
	 * @return the name of the first invalid constant paramter (or null)
	 */
	public String checkConstants() {
		String res = null;
		ParameterEnumeration e = new ParameterEnumeration(root);
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode node = e.nextElement();
			if (node.equals(root)) continue;
			ParameterInfo info = (ParameterInfo) node.getUserObject();
			if (info.isConstant() && node.getChildCount() > 0)
				return info.getName();
		}
		return res;
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean needTreeFormatWarning() {
		int nrOfNonConstants = 0;
		for (int i = 0;i < root.getChildCount();++i) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
			ParameterInfo info = (ParameterInfo) node.getUserObject();
			if (!info.isConstant())
				nrOfNonConstants++;
			if (nrOfNonConstants > 1) return true;
		}
		return false;
	}
	
	//-------------------------------------------------------------------------------
	/** Saves the page-related model settings to the XML node <code>node</code>. This
	 *  method is part of the model settings storing performed by {@link ai.aitia.meme.paramsweep.generator.WizardSettingsManager 
	 *  WizardSettingsManager}.
	 * @throws TransformerException 
	 * @throws ParserConfigurationException 
	 */
	public void save(Node node) throws ParserConfigurationException, TransformerException { 
		Document document = node.getOwnerDocument();
		
		Element element = document.createElement(WizardSettingsManager.PARAMETER_FILE);
		element.appendChild(document.createTextNode(PlatformSettings.generateParameterTreeOutput(root)));
		node.appendChild(element);
		
		if (newParameters != null) {
			Element np = document.createElement(WizardSettingsManager.NEW_PARAMETERS);
			for (ParameterInfo pi : newParameters) {
				element = document.createElement(WizardSettingsManager.PARAMETER);
				element.setAttribute(WizardSettingsManager.TYPE,pi.getType());
				element.setAttribute(WizardSettingsManager.JAVA_TYPE,pi.getJavaType().getName());
				String name = pi.getName();
				RecordableInfo dummy = new RecordableInfo(name,Integer.TYPE,null,name);
				try {
					if (owner.getModelInformation().getRecordables().indexOf(dummy) == -1)
						name = Util.uncapitalize(name);
				} catch (ModelInformationException e) { 
					throw new IllegalStateException(e);
				}
				element.appendChild(document.createTextNode(name));
				np.appendChild(element);
			}
			node.appendChild(np);
		}
	}
	
	//-------------------------------------------------------------------------------
	/** Loads the page-related model settings from the XML element <code>element</code>. This
	 *  method is part of the model settings retrieving performed by {@link ai.aitia.meme.paramsweep.generator.WizardSettingsManager 
	 *  WizardSettingsManager}.
	 * @throws WizardLoadingException if the XML document is invalid
	 */
	public void load(Element element) throws WizardLoadingException { 
		NodeList nl = element.getElementsByTagName(WizardSettingsManager.NEW_PARAMETERS);
		if (nl == null || nl.getLength() == 0)
			newParameters = null;
		else {
			NodeList nodes = ((Element)nl.item(0)).getElementsByTagName(WizardSettingsManager.PARAMETER);
			if (nodes == null || nodes.getLength() == 0)
				throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.PARAMETER);
			newParameters = new ArrayList<ParameterInfo>(nodes.getLength());
			for (int i = 0;i < nodes.getLength();++i) {
				Element param = (Element) nodes.item(i);
				String type = param.getAttribute(WizardSettingsManager.TYPE);
				if (type == null || type.equals("")) 
					throw new WizardLoadingException(true,"missing 'type' attribute at node: " + WizardSettingsManager.PARAMETER);
				String javaTypeStr = param.getAttribute(WizardSettingsManager.JAVA_TYPE);
				if (javaTypeStr == null || "".equals(javaTypeStr))
					throw new WizardLoadingException(true,"missing 'java_type' attribute at node: " + WizardSettingsManager.PARAMETER);
				NodeList content = param.getChildNodes();
				if (content == null || content.getLength() == 0)
					throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.PARAMETER);
				String name = ((Text)content.item(0)).getNodeValue();
				Class<?> javaType = null;
				try {
					javaType = Utilities.toClass(owner.getClassLoader(),javaTypeStr);
				} catch (ClassNotFoundException e) {
					throw new WizardLoadingException(true,"invalid type at parameter: " + name);
				}
				ParameterInfo pi = new ParameterInfo(name,type,javaType);
				if (!pi.isNumeric() && !pi.isBoolean() && !pi.getType().equals("String"))
					throw new WizardLoadingException(true,"invalid 'type' attribute at node: " + WizardSettingsManager.PARAMETER);
				newParameters.add(pi);
			}
		}
		
		clearTree();
		parameters = createParameters();
		
		nl = null;
		nl = element.getElementsByTagName(WizardSettingsManager.PARAMETER_FILE);
		if (nl == null || nl.getLength() == 0)
			throw new WizardLoadingException(true,"missing node: " + WizardSettingsManager.PARAMETER_FILE);
		NodeList content = nl.item(0).getChildNodes();
		if (content == null || content.getLength() == 0)
			throw new WizardLoadingException(true,"missing content at node: " + WizardSettingsManager.PARAMETER_FILE);
		String paramFileContent = ((Text)content.item(0)).getNodeValue();
		try {
			root = PlatformSettings.parseParameterFile(parameters,paramFileContent);
			outputTree.setModel(new DefaultTreeModel(root)); 
			outputTree.expandRow(0);
		} catch (ParameterParserException e) {
			throw new WizardLoadingException(true,e);
		}
		if (cancelButton.isEnabled()) 
			cancelButton.doClick();
		setGlobalRunsField();
		setNumberOfRuns();
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setGlobalRuns() {
		final long runs = parseGlobalRuns();
		ParameterEnumeration pe = new ParameterEnumeration(root);
		while (pe.hasMoreElements()) {
			DefaultMutableTreeNode node = pe.nextElement();
			if (root.equals(node)) continue;
			ParameterInfo info = (ParameterInfo) node.getUserObject();
			info.setRuns(runs);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void transformIncrementsIfNeed() {
		ParameterEnumeration pe = new ParameterEnumeration(root);
		for (DefaultMutableTreeNode node = pe.nextElement();pe.hasMoreElements();node = pe.nextElement()) {
			if (root.equals(node)) continue;
			ParameterInfo info = (ParameterInfo) node.getUserObject();
			if (info.getDefinitionType() == ParameterInfo.INCR_DEF)
				info.transformIfNeed();
		}
	}
	
	//===============================================================================
	// interface implementations

	//----------------------------------------------------------------------------------------------------
	public String getInfoText(Wizard w) {
		return w.getArrowsHeader("Specify input parameter values: Set the number of runs for each parameter value, define the parameter space and" +
								 " embed parameter runs into each other");
	}

	//-------------------------------------------------------------------------------
	public Container getPanel() { return this; }
	public String getTitle() { return "Parameters"; }
	
	//----------------------------------------------------------------------------------------------------
	public boolean onButtonPress(Button b) { 
		boolean enabled = isEnabled(b);
		if (enabled) {
			if (b == Button.CANCEL || editedNode == null || modify()) return true;
		}
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean isEnabled(Button b) {
		if (modelInformationException && (b == Button.NEXT || b == Button.FINISH)) return false;
		return PlatformSettings.isEnabledForPageParameters(owner,b);
	}

	//-------------------------------------------------------------------------------
	public void onPageChange(boolean show) { 
		if (show) {
			if (owner.getModelState() == ParameterSweepWizard.ERROR) return;
			initializePageForPlatform();
			if (owner.getModelState() == ParameterSweepWizard.NEW_MODEL) {
				modelInformationException = false;
			 // build the tree
				clearTree();
				parameters = createParameters();
				if (parameters == null) return;
				if (owner.getParameterFile() != null && owner.getParameterFile().exists()) {
					try {
						root = PlatformSettings.parseParameterFile(parameters,owner.getParameterFile());
						root.setUserObject(new String(owner.getParameterFile().getName()));
						setGlobalRunsField();
						outputTree.setModel(new DefaultTreeModel(root));
						outputTree.expandRow(0);
					} catch (ParameterParserException e) {
						Utilities.userAlert(owner,"Cannot initialize from the defined parameter file.","Reason: " + Util.getLocalizedMessage(e));
						e.printStackTrace(ParameterSweepWizard.getLogStream());
						createDefaultTree();
					}
				} else
					createDefaultTree();
				enableDisableSettings(false);
				modifyButton.setEnabled(false);
				owner.setModelState(ParameterSweepWizard.NEW_RECORDERS);
			}
			if (owner.getParameterFile() != null && owner.isParameterFileChanged()) { 
				if (owner.getModelState() != ParameterSweepWizard.NEW_MODEL && owner.getParameterFile().exists()) {
					try {
						root = PlatformSettings.parseParameterFile(parameters,owner.getParameterFile());
						root.setUserObject(new String(owner.getParameterFile().getName()));
						setGlobalRunsField();
						outputTree.setModel(new DefaultTreeModel(root));
						outputTree.expandRow(0);
					} catch (ParameterParserException e) {
						Utilities.userAlert(owner,"Cannot initialize from the defined parameter file.","Reason: " + Util.getLocalizedMessage(e));
						e.printStackTrace(ParameterSweepWizard.getLogStream());
					}
				}
			}
			owner.setParameterFileChanged(false);
			setNumberOfRuns();
		} else {
			if (cancelButton.isEnabled())
				cancelButton.doClick();
		}
	}

	//-------------------------------------------------------------------------------
	public void actionPerformed(ActionEvent e) { hybridAction(e,null); }

	//--------------------------------------------------------------------------------
	/** This method is intended to be a common place for handling "related" actionPerformed()
	 *  messages (here "related" usually means that belong to the same window).
	 * @param a Non-null only if this actionPerformed() message is originated from a
	 *           HybridAction object.     
	 */
	public void hybridAction(ActionEvent e, HybridAction a) { 
		if (a != null) {
			if (a == moveUpAction) {
				if (editedNode == null || modify()) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) outputTree.getSelectionPath().getLastPathComponent();
					DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
					if (parent == null || parent.getFirstChild().equals(node)) return;
					int index = parent.getIndex(node);
					DefaultTreeModel treemodel = (DefaultTreeModel) outputTree.getModel();
					treemodel.removeNodeFromParent(node);
					treemodel.insertNodeInto(node,parent,index - 1);
					outputTree.setSelectionPath(new TreePath(node.getPath()));
				}
			} else if (a == moveDownAction) {
				if (editedNode == null || modify()) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) outputTree.getSelectionPath().getLastPathComponent();
					DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
					if (parent == null || parent.getLastChild().equals(node)) return;
					int index = parent.getIndex(node);
					DefaultTreeModel treemodel = (DefaultTreeModel) outputTree.getModel();
					treemodel.removeNodeFromParent(node);
					treemodel.insertNodeInto(node,parent,index + 1);
					outputTree.setSelectionPath(new TreePath(node.getPath()));
				}
			} else if (a == editAction) {
				if (editedNode == null || modify()) {
					DefaultMutableTreeNode node = (DefaultMutableTreeNode) outputTree.getSelectionPath().getLastPathComponent();
					if (node.equals(root)) return;
					ParameterInfo info = (ParameterInfo) node.getUserObject();
					editedNode = node;
					outputTree.expandPath(outputTree.getSelectionPath());
					edit(info);
				}
			}
		} else {
			String command = e.getActionCommand();
			if (command.equals("CONST")) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) outputTree.getSelectionPath().getLastPathComponent();
				if (node.equals(root)) return;
				ParameterInfo info = (ParameterInfo) node.getUserObject();
				CardLayout cl = (CardLayout) leftMiddle.getLayout();
				if(info instanceof MasonChooserParameterInfo || info.isEnum())cl.show(leftMiddle,"ENUMCONST");
				else cl.show(leftMiddle,"CONST");
			}
			else if (command.equals("LIST")) {
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) outputTree.getSelectionPath().getLastPathComponent();
				if (node.equals(root)) return;
				ParameterInfo info = (ParameterInfo) node.getUserObject();
				CardLayout cl = (CardLayout) leftMiddle.getLayout();
				if(info instanceof MasonChooserParameterInfo || info.isEnum())cl.show(leftMiddle,"ENUMLIST");
				else cl.show(leftMiddle,"LIST");
			}
			else if (command.equals("INCREMENT")) {
				CardLayout cl = (CardLayout) leftMiddle.getLayout();
				cl.show(leftMiddle,"INCREMENT");
			}else if (command.equals("MODIFY"))
			{
				browseFileButton.setVisible(false);
				modify();
			}
			else if(command.equals("ADD"))
			{
				for(Object element : leftEnumValueList.getSelectedValuesList())
				{
					rightEnumValueModel.addElement((String)element);
				}
			}
			else if(command.equals("REMOVE"))
			{
				for(Object element : rightEnumValueList.getSelectedValuesList())
				{
					rightEnumValueModel.removeElement(element);
				}
			}
			else if (command.equals("CANCEL")) {
				resetSettings();
				enableDisableSettings(false);
				modifyButton.setEnabled(false);
				browseFileButton.setVisible(false);
				editedNode = null;
			} else if (command.equals("RUNS_FIELD") && PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL)  
				constDef.grabFocus();
			else if (command.equals("CONST_FIELD") || command.equals("STEP_FIELD"))
				modifyButton.doClick();
			else if (command.equals("START_FIELD"))
				incrEndValueField.grabFocus();
			else if (command.equals("END_FIELD"))
				incrStepField.grabFocus();
			else if (command.equals("BROWSE")) {
				JFileChooser jfc = new JFileChooser();
				int val = jfc.showOpenDialog(this);
		        if (val == JFileChooser.APPROVE_OPTION) {
		            File file = jfc.getSelectedFile();
		            if (file == null) return;
					constDefField.setText(file.getAbsolutePath());
		            }
			}
			else if (command.equals("NEW_PARAMETER")) { 
				int result = Utilities.askUser(owner,false,"Warning","All user defined parameter settings will be lost.",
											   "Do you want to continue?");
				if (result == 1) { // yes
					List<RecordableInfo> candidates = null;
					try {
						candidates = owner.getModelInformation().getRecordables();
					} catch (ModelInformationException e1) {
						Utilities.userAlert(owner,"Error while collecting new parameter candidates.","Reason: " +
											Util.getLocalizedMessage(e1));
						e1.printStackTrace(ParameterSweepWizard.getLogStream());
						return;
					}
					NewParametersDialog dialog = new NewParametersDialog(ParameterSweepWizard.getFrame(),candidates,originalInitParamResult);
					result = dialog.showDialog();
					if (result == NewParametersDialog.OK_OPTION) {
						List<ParameterInfo> oldNewParameters = newParameters == null ? new ArrayList<ParameterInfo>() : newParameters; 
						newParameters = dialog.getNewParameterList();
						if (newParameters.size() == 0)
							newParameters = null;
						clearTree();
						parameters = createParameters();
						if (owner.getParameterFile() != null && owner.getParameterFile().exists()) {
							try {
								root = PlatformSettings.parseParameterFile(parameters,owner.getParameterFile());
								root.setUserObject(owner.getParameterFile().getName());
								setGlobalRunsField();
								outputTree.setModel(new DefaultTreeModel(root));
								outputTree.expandRow(0);
							} catch (ParameterParserException e1) {
								createDefaultTree();
								Utilities.userAlert(owner,"Cannot initialize from the defined parameter file.","Reason: " +
													Util.getLocalizedMessage(e1));
								e1.printStackTrace(ParameterSweepWizard.getLogStream());
							}
						} else
							createDefaultTree();
						enableDisableSettings(false);
						modifyButton.setEnabled(false);
						owner.reinitializeRecordableLists(newParameters == null ? oldNewParameters : 
																				  Utilities.listSubtract(oldNewParameters,newParameters));
						owner.cleanRecorders();
						setNumberOfRuns();
					}
				}
			}
		}
	}
	
	//-------------------------------------------------------------------------------
	public void valueChanged(TreeSelectionEvent e) {
		TreePath path = outputTree.getSelectionPath();
		editAction.setEnabled(path != null);
		moveUpAction.setEnabled(path != null);
		moveDownAction.setEnabled(path != null);
	}
	
	//------------------------------------------------------------------------------
    public IIntelliContext getIntelliContext(){
		if (intelliContext == null)
			intelliContext = new IntelliContext();
		return intelliContext;
	}
    
	//----------------------------------------------------------------------------------------------------
	public void focusGained(FocusEvent e) {}
	
	//----------------------------------------------------------------------------------------------------
	public void focusLost(FocusEvent e) {
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.GLOBAL)
			setNumberOfRuns();
	}
	
	//----------------------------------------------------------------------------------------------------
	public void dropSuccessful(DnDTreeDropSuccessfulEvent e) { 
		setNumberOfRuns();
		outputTree.expandPath(new TreePath(e.getNewParent().getPath()));
	}

	//===============================================================================
	// GUI methods
	
	//-------------------------------------------------------------------------------
	private void layoutGUI() { 
		leftTop = FormsUtils.build("p ' p:g",
								   "[DialogBorder]00|" +
									             "11||" +
									             "23||" +
									             "44||" +
									             "5_|" +
									             "6_|" +
									             "7_",
  					                text1,
								    text2,
									runsLabel,runsField,
									new Separator("Define parameter space"),
									constDef,
									listDef,
									incrDef).getPanel();
		
		constDefPanel = FormsUtils.build("p ~ p:g p",
										 "[DialogBorder]012 p",
										 "Constant value: ",CellConstraints.TOP,constDefField,browseFileButton).getPanel();
		
		enumConstPanel = FormsUtils.build("p ~ p:g",
										  "[DialogBorder]01 p",
										  "Constant value: ",CellConstraints.TOP,enumComboBox).getPanel();
		
		
		listDefPanel = FormsUtils.build("p ~ p:g",
										"[DialogBorder]01|" +
										              "_1 f:p:g||" +
										              "_2 p",
										"Value list: ",listDefScr,
										"(Separate values with spaces!)").getPanel();
		
		incrDefPanel = FormsUtils.build("p ~ p:g",
										"[DialogBorder]01||" +
													  "23||" +
													  "45",
													  "Start value: ",incrStartValueField,
													  "End value: ",incrEndValueField,
													  "Step: ",incrStepField).getPanel();
		
		enumListPanel = FormsUtils.build("p ~ p",
										 "[DialogBorder]01||" +
													   "23||",
													   leftEnumValuePane,RightEnumValuePane,
													   addEnumButton,removeEnumButton).getPanel();

		constDefPanel.setName("CONST");
		listDefPanel.setName("LIST");
		incrDefPanel.setName("INCREMENT");
		enumListPanel.setName("ENUMLIST");
		enumConstPanel.setName("ENUMCONST");
		leftMiddle.add(constDefPanel,constDefPanel.getName());
		leftMiddle.add(listDefPanel,listDefPanel.getName());
		leftMiddle.add(incrDefPanel,incrDefPanel.getName());
		leftMiddle.add(enumListPanel,enumListPanel.getName());
		leftMiddle.add(enumConstPanel,enumConstPanel.getName());

		leftBottom = FormsUtils.build("p:g p ~ p ~ p:g",
									  "[DialogBorder]_01_ p",
									  modifyButton,cancelButton).getPanel();
		
		// left 
		leftFirst.add(leftTop,BorderLayout.NORTH);
		leftFirst.add(leftMiddle,BorderLayout.CENTER);
		leftFirst.add(leftBottom,BorderLayout.SOUTH);
		
		leftSecond.add(newParameterButton);
		
		left = FormsUtils.build("p",
								"0 f:p:g||" +
								"1 p",
								leftFirst,
								leftSecond).getPanel();
		
//		right = FormsUtils.build("p:g ~ p ~",
//								 "01|" +
//								 "0_ f:p:g||" +
//								 "22 p",
//								 treeScr,FormsUtils.buttonStack(moveUpButton,moveDownButton,editButton).getPanel(),
//								 numberOfRunsLabel).getPanel();
		
		right = FormsUtils.build("p:g ~ p ~",
				 "00||" +
				 "12|" +
				 "1_ f:p:g||",
				 numberOfRunsLabel,
				 treeScr,FormsUtils.buttonStack(moveUpButton,moveDownButton,editButton).getPanel()).getPanel();

		
		content = FormsUtils.build("p p:g",
								   "01 f:p:g",
								   left,right).getPanel();
		
		this.setLayout(new BorderLayout());
		final JScrollPane sp = new JScrollPane(content);
		this.add(sp,BorderLayout.CENTER);
		
		outputTree.setName("tree_wizard_params");
		runsField.setName("fld_wizard_params_runs");
		constDef.setName("rbtn_wizard_params_const");
		listDef.setName("rbtn_wizard_params_list");
		incrDef.setName("rbtn_wizard_params_incr");
		modifyButton.setName("btn_wizard_params_modify");
		cancelButton.setName("btn_wizard_params_cancel");
		incrStartValueField.setName("fld_wizard_params_incrstart");
		incrEndValueField.setName("fld_wizard_params_incrend");
		incrStepField.setName("fld_wizard_params_incrstep");
		constDefField.setName("fld_wizard_params_constval");
		moveUpButton.setName("btn_wizard_params_moveup");
		moveDownButton.setName("btn_wizard_params_movedown");
		editButton.setName("btn_wizard_params_edit");
		listDefArea.setName("fld_wizard_params_paramlist");

		
		
	}
	
	//------------------------------------------------------------------------------
	private void initialize() {  
		leftFirst.setBorder(BorderFactory.createTitledBorder("Parameter settings"));
		
		runsField.setActionCommand("RUNS_FIELD");
		runsField.addFocusListener(this);
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.GLOBAL)
			runsField.setEnabled(true);
		
		GUIUtils.createButtonGroup(constDef,listDef,incrDef);
		constDef.setSelected(true);
		constDef.setActionCommand("CONST");
		listDef.setActionCommand("LIST");
		incrDef.setActionCommand("INCREMENT");
		
		constDefField.setActionCommand("CONST_FIELD");
		incrStartValueField.setActionCommand("START_FIELD");
		incrEndValueField.setActionCommand("END_FIELD");
		incrStepField.setActionCommand("STEP_FIELD");
		browseFileButton.setActionCommand("BROWSE");

		modifyButton.setActionCommand("MODIFY");
		cancelButton.setActionCommand("CANCEL");
		addEnumButton.setActionCommand("ADD");
		removeEnumButton.setActionCommand("REMOVE");
		
		
		newParameterButton.setActionCommand("NEW_PARAMETER");
		newParameterButtonCopy.setActionCommand("NEW_PARAMETER");

		listDefArea.setLineWrap(true);
		listDefArea.setWrapStyleWord(true);
		listDefScr.setPreferredSize(new Dimension(100,200));

		outputTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		outputTree.setCellRenderer(new ParameterTreeRenderer());
		outputTree.addTreeSelectionListener(this);
		outputTree.addDropSuccessfulListener(this);
		outputTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (editedNode == null || modify()) {
					Point loc = e.getPoint();
					TreePath path = outputTree.getPathForLocation(loc.x,loc.y);
					DefaultMutableTreeNode actNode = null;
					if (path != null)
						actNode = (DefaultMutableTreeNode) path.getLastPathComponent();
					if (root.equals(actNode)) {
						if (cancelButton.isEnabled()) 
							cancelButton.doClick();
						return;
					}
					if (actNode != null && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
						hybridAction(null,editAction);
				}
			}
		});
		
		right.setBorder(BorderFactory.createTitledBorder("Output structure"));
		treeScr.setPreferredSize(new Dimension(300,400));
		browseFileButton.setVisible(false);
		
		initializeContextMenu();

		GUIUtils.addActionListener(this,modifyButton,cancelButton,constDef,listDef,incrDef,runsField,constDefField,incrStartValueField,
								   incrEndValueField,incrStepField,newParameterButton,newParameterButtonCopy,browseFileButton,addEnumButton,removeEnumButton);
	}
	
	//------------------------------------------------------------------------------
	/** Initializes the context menu of the parameters tree. */
	private void initializeContextMenu() {
		JPopupMenu context = outputTree.getContextMenu();
		context.setName("cmenu_wizard_parameters_treecmenu");
		context.addSeparator();
		context.add(editAction);
		context.add(moveUpAction);
		context.add(moveDownAction);
		
		context.getComponent(3).setName("btn_wizard_params_cmenuedit");
		context.getComponent(4).setName("btn_wizard_params_cmenumoveup");
		context.getComponent(5).setName("btn_wizard_params_cmenumovedown");
				
		editAction.setEnabled(false);
		moveUpAction.setEnabled(false);
		moveDownAction.setEnabled(false);
	}
	
	//------------------------------------------------------------------------------
	/** Enables/disables the parameter editing components of the page according to
	 *  the value of <code>enabled</code>.
	 */
	private void enableDisableSettings(boolean enabled) {
		runsField.setEnabled(enabled || PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.GLOBAL);
		constDef.setEnabled(enabled);
		listDef.setEnabled(enabled);
		incrDef.setEnabled(enabled);
		constDefField.setEnabled(enabled);
		listDefArea.setEnabled(enabled);
		incrStartValueField.setEnabled(enabled);
		incrEndValueField.setEnabled(enabled);
		incrStepField.setEnabled(enabled);
		cancelButton.setEnabled(enabled);
		enumComboBox.setEnabled(enabled);
		addEnumButton.setEnabled(enabled);
		removeEnumButton.setEnabled(enabled);
		newParameterButton.setEnabled(PlatformSettings.getGUIControllerForPlatform().isNewParametersEnabled() && !enabled);
	}
	
	//------------------------------------------------------------------------------
	/** Changes the text on the left top corner of the page. The text can be the initial
	 *  commentary text ("Select a parameter from the tree to modify its settings") or
	 *  the name and type of the edited parameter.
	 * @param name name of the edited parameter (null if there is no editing parameter)
	 * @param type name of type of the edited parameter
	 */
	private void changeText(String name, String type) {
		if (name == null) {
			text1.setText(orderPart1);
			text2.setText(orderPart2);
		} else {
			text1.setText("<html><b>" + name + "</b>: " + type + "</html>");
			text2.setText(" ");
		}
	}
	
	//------------------------------------------------------------------------------
	/** Resets the parameter editing related components. */
	private void resetSettings() { 
		changeText(null,null);
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL)
			runsField.setText(""); 
		constDefField.setText("");
		listDefArea.setText("");
		incrStartValueField.setText("");
		incrEndValueField.setText("");
		incrStepField.setText("");
		leftEnumValueModel.clear();
		rightEnumValueModel.clear();
		enumComboBoxModel.removeAllElements();
	}
	
	//==============================================================================
	// private methods
	
	//------------------------------------------------------------------------------
	/** Deletes all parameters from the parameter tree. */
	private void clearTree() {
		root = new DefaultMutableTreeNode("Parameter file");
		outputTree.setModel(new DefaultTreeModel(root));
	}
	
	//------------------------------------------------------------------------------
	/** Creates the list of information objects of parameters. */
	@SuppressWarnings("unchecked")
	private List<ParameterInfo> createParameters() { 
		List<ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?>> paramList = null;
		try {
			paramList = owner.getModelInformation().getParameters();
		} catch (ModelInformationException e) {
			Utilities.userAlert(owner,"Identification of parameters is failed:", Util.getLocalizedMessage(e));
			modelInformationException = true;
			owner.enableDisableButtons();
			owner.setModelState(ParameterSweepWizard.ERROR);
			e.printStackTrace(ParameterSweepWizard.getLogStream());
			return null;
		}
		originalInitParamResult = new String[paramList.size()];
		for (int i = 0;i < paramList.size();++i)
			originalInitParamResult[i] = paramList.get(i).getName();
		List<ParameterInfo> info = new ArrayList<ParameterInfo>();
		for (ai.aitia.meme.paramsweep.batch.param.ParameterInfo<?> parameter : paramList) {
			ParameterInfo pi = InfoConverter.parameterInfo2ParameterInfo(parameter);
			if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL)
				pi.setRuns(1);
			pi.setDefinitionType(ParameterInfo.CONST_DEF);
			pi.setValue(parameter.getDefaultValue());
			info.add(pi);
			
		}
		if (newParameters != null) { 
			initParamResult = new String[originalInitParamResult.length + newParameters.size()];
			for (int i = 0;i < originalInitParamResult.length;++i)
				initParamResult[i] = originalInitParamResult[i];
			for (int i = 0;i < newParameters.size();++i) {
				String name = newParameters.get(i).getName();
				initParamResult[originalInitParamResult.length + i] = name;
				newParameters.get(i).setName(Util.capitalize(name));
			}
			info.addAll(newParameters);
		} else 
			initParamResult = originalInitParamResult;
		Collections.sort(info);
		return info;
	}
	
	//------------------------------------------------------------------------------
	/** Builds the parameter tree from the <code>parameters</code> list. */
	private void createDefaultTree() {
		if (parameters == null) return;
		for (ParameterInfo pi : parameters) 
			root.add(new DefaultMutableTreeNode(pi));
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.GLOBAL)
			runsField.setText("1");
		outputTree.expandRow(0);
	}
	
	//------------------------------------------------------------------------------
	/** Edits the information object <code>info</code> so this method fills the appropriate
	 *  components with the informations of <code>info</code>.
	 */
	private void edit(ParameterInfo info) { 
		changeText(info.getName(),info.getType());
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL)
			runsField.setText(String.valueOf(info.getRuns()));
		String view = null;
		
		if(info instanceof MasonChooserParameterInfo)
		{
			leftEnumValueModel.clear();
			enumComboBoxModel.removeAllElements();
			for(String s : ((MasonChooserParameterInfo)info).getValidStrings())
			{
				enumComboBoxModel.addElement(s);
				leftEnumValueModel.addElement(s);
			}
		}
		if(info.isEnum())
		{
			leftEnumValueModel.clear();
			enumComboBoxModel.removeAllElements();
			Object[] elements =  info.getJavaType().getEnumConstants();
			for(Object element  : elements)
			{
				enumComboBoxModel.addElement(((Enum)element).name());
				leftEnumValueModel.addElement(((Enum)element).name());
			}
		}
		
		switch (info.getDefinitionType()) {
		case ParameterInfo.CONST_DEF :	if(info instanceof MasonChooserParameterInfo || info.isEnum())view = "ENUMCONST";
										else view = "CONST"; 
								   		constDef.setSelected(true);
								   		constDefField.setText(info.valuesToString());
								   		break;
		case ParameterInfo.LIST_DEF  : if(info instanceof MasonChooserParameterInfo || info.isEnum())view = "ENUMLIST";
									   else view = "LIST";
									   listDef.setSelected(true);
									   listDefArea.setText(info.valuesToString());
									   break;
		case ParameterInfo.INCR_DEF  : view = "INCREMENT";
									   incrDef.setSelected(true);
									   incrStartValueField.setText(info.startToString());
									   incrEndValueField.setText(info.endToString());
									   incrStepField.setText(info.stepToString());
		}
		CardLayout cl = (CardLayout) leftMiddle.getLayout();
		cl.show(leftMiddle,view);
		browseFileButton.setEnabled(false);
		enableDisableSettings(true);
		if (!info.isNumeric()) 
			incrDef.setEnabled(false);
		modifyButton.setEnabled(true);
		if(info.isFile())
		{
			listDef.setEnabled(false);
			browseFileButton.setVisible(true);
		}
	}
	
	//------------------------------------------------------------------------------
	/** Checks the content of the editing components. 
	 * @param node the node that belongs to the edited parameter
	 * @return the error messages in an array (or null if there is no error)
	 */
	private String[] checkInput(DefaultMutableTreeNode node) {
		ArrayList<String> errors = new ArrayList<String>();
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL) {
			DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
			boolean allowEmptyRun = (parent.equals(root) && !parent.getFirstChild().equals(node))
			 						 || constDef.isSelected();
			if (runsField.getText().trim().equals("") && !allowEmptyRun) 
				errors.add("'Runs' cannot be empty.");
			else if (!runsField.getText().trim().equals("")) {
				try {
					long i = new Long(runsField.getText().trim()).longValue();
					if (i <= 0)
						throw new NumberFormatException();
				} catch (NumberFormatException e) {
					errors.add("'Runs' must be a positive integer.");
				}
			}
		}
		ParameterInfo info = (ParameterInfo) node.getUserObject();
		if (constDef.isSelected()) {
			if(!(info instanceof MasonChooserParameterInfo || info.isEnum()))
			{
				if (constDefField.getText().trim().equals(""))
					errors.add("'Constant value' cannot be empty.");
				else {
					boolean valid = ParameterInfo.isValid(constDefField.getText().trim(),info.getType());
					if (!valid)
						errors.add("'Constant value' must be a valid " + getTypeText(info.getType()));
					errors.addAll(PlatformSettings.additionalParameterCheck(info,new String[] {constDefField.getText().trim()},ParameterInfo.CONST_DEF));
				}
			}
		} else if (listDef.isSelected()) {
			if(info instanceof MasonChooserParameterInfo || info.isEnum())
			{
				if(rightEnumValueModel.isEmpty())
				{
					errors.add("'List of values' cannot be empty." + getTypeText(info.getType()));
				}
			}
			else
			{
				String text = listDefArea.getText().trim();
				if (text.equals(""))
					errors.add("'List of values' cannot be empty.");
				else {
					text = text.replaceAll("[\\s]+"," ");
					String[] elements = text.split(" ");
					boolean goodList = true;
					for (String element : elements)
						goodList = goodList && ParameterInfo.isValid(element.trim(),info.getType());
					if (!goodList)
						errors.add("All elements of the list must be a valid " + getTypeText(info.getType()));
					else {
						List<String> previous = new ArrayList<String>(elements.length);
						for (String element : elements) {
							if (previous.contains(element)) {
								goodList = false;
								break;
							}
							previous.add(element);
						}
						if (!goodList) {
							int result = Utilities.askUser(owner,false,"Warning", "The list contains repetitive element(s).\nAre you sure?");
							if (result == 0 && errors.size() == 0)
								return new String[0];
						}
					}
					errors.addAll(PlatformSettings.additionalParameterCheck(info,elements,ParameterInfo.LIST_DEF));
				}
		}
		} else {
			boolean s = false, e = false, st = false; 
			if (incrStartValueField.getText().trim().equals("")) {
				errors.add("'Start value' cannot be empty.");
				s = true;
			} else {
				boolean valid = ParameterInfo.isValid(incrStartValueField.getText().trim(),info.getType());
				if (!valid)
					errors.add("'Start value' must be a valid " + getTypeText(info.getType()));
			}
			if (incrEndValueField.getText().trim().equals("")) {
				errors.add("'End value' cannot be empty.");
				e = true;
			} else {
				boolean valid = ParameterInfo.isValid(incrEndValueField.getText().trim(),info.getType());
				if (!valid)
					errors.add("'End value' must be a valid " + getTypeText(info.getType()));
			}
			if (incrStepField.getText().trim().equals("")) {
				errors.add("'Step' cannot be empty.");
				st = true;
			} else {
				double d = Double.valueOf(incrStepField.getText().trim());
				if (d == 0)
					errors.add("'Step' cannot be zero.");
				else {
					boolean valid = ParameterInfo.isValid(incrStepField.getText().trim(),info.getType());
					if (!valid)
						errors.add("'Step' must be a valid " + getTypeText(info.getType()));
				}
			}
			if (!(s || e || st))
				errors.addAll(PlatformSettings.additionalParameterCheck(info,new String[] { incrStartValueField.getText().trim(), 
																							incrEndValueField.getText().trim(), 
																							incrStepField.getText().trim()},
																	    ParameterInfo.INCR_DEF));
		}
		return errors.size()== 0 ? null : errors.toArray(new String[0]);
	}
	
	//------------------------------------------------------------------------------
	/** Returns the appropriate error message part according to the value of <code>type</code>. */
	private String getTypeText(String type) {
		if ("int".equals(type) || "Integer".equals(type))
			return "integer value.";
		if ("float".equals(type) || "Float".equals(type)
			|| "double".equals(type) || "Double".equals(type))
			return "real value.";
		if ("boolean".equals(type) || "Boolean".equals(type))
			return "boolean value (\"true\" or \"false\").";
		if ("String".equals(type))
			return "string that not contains any white space.";
		return type + ".";
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean modify() {
		String[] errors = checkInput(editedNode);
		if (errors != null && errors.length != 0) {
			Utilities.userAlert(this,(Object)errors);
			return false;
		} else {
			modifyParameterInfo(editedNode);
			resetSettings();
			enableDisableSettings(false);
			modifyButton.setEnabled(false);
			outputTree.repaint();
			TreePath path = new TreePath(editedNode.getPath());
			outputTree.setSelectionPath(path);
			fireTreeStructureChanged(path);
			editedNode = null;
			setNumberOfRuns();
			return true;
		}
	}
	
	//------------------------------------------------------------------------------
	// Pre-condition : all input are valid
	/** Modifies the information object that belongs to <code>node</code> from the
	 *  contents of the editing components of the page.<br>
	 *  Pre-condition: all input values are valid.
	 */
	private void modifyParameterInfo(DefaultMutableTreeNode node) { 
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
		ParameterInfo info = (ParameterInfo) node.getUserObject();
		info.clear();
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.LOCAL) {
			if (runsField.getText().trim().equals("")) {
				if (constDef.isSelected())
					info.setRuns(1);
				else {
					ParameterInfo prev = (ParameterInfo) ((DefaultMutableTreeNode)parent.getChildBefore(node)).getUserObject();
					info.setRuns(prev.getRuns());
				}
			} else {
				long i = Long.parseLong(runsField.getText().trim());
				info.setRuns(i);
			}
		}
	
		int defType = ParameterInfo.CONST_DEF;
		if (listDef.isSelected())
			defType = ParameterInfo.LIST_DEF;
		else if (incrDef.isSelected())
			defType = ParameterInfo.INCR_DEF;
		info.setDefinitionType(defType);
		switch (defType) {
		case ParameterInfo.CONST_DEF :	if(info instanceof MasonChooserParameterInfo || info.isEnum())info.setValue(enumComboBox.getSelectedIndex());
										else info.setValue(ParameterInfo.getValue(constDefField.getText().trim(),info.getType()));
										break;
		case ParameterInfo.LIST_DEF  : 
										if(info instanceof MasonChooserParameterInfo || info.isEnum())
										{
											List<Object> values = new ArrayList<Object>();
											for(int i = 0; i<rightEnumValueModel.size();i++)
											{
												values.add(leftEnumValueModel.indexOf(rightEnumValueModel.elementAt(i)));
											}
											info.setValues(values);
										}
										else
										{
											String text = listDefArea.getText().trim();
											text = text.replaceAll("[\\s]+", " ");
											String[] elements = text.split(" ");
											List<Object> list = new ArrayList<Object>(elements.length);
											for (String element : elements)
												list.add(ParameterInfo.getValue(element, info.getType()));
											info.setValues(list);
										}
										break;
		case ParameterInfo.INCR_DEF  : info.setStartValue((Number)ParameterInfo.getValue(incrStartValueField.getText().trim(),info.getType()));
									   info.setEndValue((Number)ParameterInfo.getValue(incrEndValueField.getText().trim(),info.getType()));
									   info.setStep((Number)ParameterInfo.getValue(incrStepField.getText().trim(),info.getType()));
									   break;
		}
	}
	
	//------------------------------------------------------------------------------
	/** Notifies all listeners of the parameters tree that the structure of the tree is
	 *  changed.
	 */
	private void fireTreeStructureChanged(TreePath path) {
        // Guaranteed to return a non-null array
        Object[] listeners = ((DefaultTreeModel)outputTree.getModel()).getListeners(TreeModelListener.class);
        TreeModelEvent e = null;
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 1;i >= 0;i--) {
                // Lazily create the event:
                if (e == null)
                    e = new TreeModelEvent(this, path);
                ((TreeModelListener)listeners[i]).treeStructureChanged(e);
        }
	}
	
	//----------------------------------------------------------------------------------------------------
	private void initializePageForPlatform() {
		IGUIController controller = PlatformSettings.getGUIControllerForPlatform();
		newParameterButton.setEnabled(controller.isNewParametersEnabled());
		newParameterButtonCopy.setEnabled(controller.isNewParametersEnabled());
		switch (controller.getRunOption()) {
		case NONE 	: runsLabel.setVisible(false);
					  runsField.setVisible(false);
					  break;
		case GLOBAL	: runsLabel.setText("Runs (global): ");
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private long parseGlobalRuns() {
		long runs = 1;
		final String runsStr = runsField.getText().trim();
		if (runsStr != null && runsStr.length() > 0) {
			try {
				runs = Long.parseLong(runsStr);
			} catch (NumberFormatException e) {}
		}
		return runs;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void setGlobalRunsField() {
		if (PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.GLOBAL) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(0);
			if (node != null) {
				ParameterInfo pi = (ParameterInfo) node.getUserObject();
				if (pi != null)
					runsField.setText(String.valueOf(pi.getRuns()));
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void setNumberOfRuns() {
		final long globalRuns = PlatformSettings.getGUIControllerForPlatform().getRunOption() == RunOption.GLOBAL ? parseGlobalRuns() : -1;
		final long nrRuns = Utilities.calculateNumberOfRuns(root,globalRuns);
		String text = "<html>&nbsp;A total of " + nrRuns + " run" + (nrRuns > 1 ? "s" : "") + ".";
		
		if (needTreeFormatWarning())
			text += "<font color=\"#FF0000\"> (Warning: Multiple non-constant branches)</font>";
		text += "</html>";
		numberOfRunsLabel.setText(text);
	}
	
	//==============================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	/** This class renders the parameters tree. */ 
	@SuppressWarnings("serial")
	private class ParameterTreeRenderer extends DnDTreeCellRenderer {
		
		//====================================================================================================
		// members
		
		private ImageIcon parameterIcon = ExpandedEditor.getIcon("type_parameter.png"); 

		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		@Override
		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean focus) {
			Component com = super.getTreeCellRendererComponent(tree,value,sel,expanded,leaf,row,focus);
			Icon icon = ((DefaultMutableTreeNode)value).equals(root) ? leafIcon : parameterIcon;
			if (icon != null) 
				((JLabel)com).setIcon(icon);
			tree.setToolTipText(value.toString());
			((JLabel)com).setToolTipText(value.toString());
			return com;
		}
		
	}

	//----------------------------------------------------------------------------------------------------
	private class IntelliContext extends HashMap<Object, Object> implements IIntelliContext{
		{
			put( "scriptSupport", owner.getRecordersPage().getScriptSupport() );
		}
        private static final long serialVersionUID = -7383965550748377433L;
		public List<ParameterInfo> getParameters() { return parameters; }
		public DefaultMutableTreeNode getParameterTreeRootNode() { return root; }
		public JButton getNewParametersButton() { return Page_Parameters.this.getNewParametersButton(); }
		public File getPluginResourcesDirectory() { return new File(System.getProperty("user.dir")+"/resources/Plugins"); }
	}
}
