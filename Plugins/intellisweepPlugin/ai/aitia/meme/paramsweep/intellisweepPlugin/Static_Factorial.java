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
package ai.aitia.meme.paramsweep.intellisweepPlugin;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ai.aitia.meme.gui.IRngSeedManipulatorChangeListener;
import ai.aitia.meme.gui.RngSeedManipulator;
import ai.aitia.meme.paramsweep.generator.RngSeedManipulatorModel;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.plugin.IIntelliMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IIntelliStaticMethodPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

/**
 * A plugin that generates a full or fractional factorial design with two levels.
 * @author Ferschl
 */
public class Static_Factorial implements IIntelliStaticMethodPlugin, ActionListener, IRngSeedManipulatorChangeListener, ChangeListener {
	
	private static final long serialVersionUID = 1L;
	public static final String FACTORS = "factors";
	public static final String FACTOR = "factor";
	public static final String PARAMETERS = "parameters";
	public static final String PARAMETER = "parameter";
	public static final String FRACTIONAL = "fractional";
	public static final String GENERATOR = "generator";
	public static final String CENTERPOINT = "centerpoint";
	public static final String CENTERPOINT_COUNT = "centerpoint_count";
	public static final String NORMALIZE = "normalize";
	public static final String NAME = "name";
	public static final String LOW = "low";
	public static final String HIGH = "high";
	public static final String DEFAULT_VALUE = "defvalue";
	public static final String PRODUCT = "product";
	public static final String FRACTIONAL_P = "fractional_p";
	
//	private Vector<FactorialParameterInfo> parameterInfos = null;
//	private Vector<FactorialFactorInfo> factorInfos = null;
//	private Vector<FactorialParameterInfo> hiddenParamsAsSeeds = null;
//	private int centerPointCount = 1;
	private JSpinner centerPointSpinner = new JSpinner(new SpinnerNumberModel(1,1,10,1));
	private String readyStatusDetail = null;
	private FactorialFractionalHelper fractionalHelper = null;
	private RngSeedManipulator rngSeedManipulator = null;
	private IIntelliContext context = null;
	
	// GUI components:
	private JPanel scrollableContent = null;
	private JScrollPane contentScr= null;
	private JPanel content = null;
	private JPanel factorSelectionPanel = null;
	/**The JList containing all available parameters.*/
	private JList parametersList = null;
	private JScrollPane parametersScr = null;
	/**The JList containing the chosen parameters for the experiment (factors).*/
	private JList factorsList = null;
	private JScrollPane factorsScr = null;
	private JButton addButton = new JButton("Add factor>");
	private JButton removeButton = new JButton("<Remove factor");
	private JTextField defaultField = new JTextField(10);
	private JButton defaultChangeButton = new JButton("Set");
	private JTextField highField = new JTextField(10);
	private JTextField lowField = new JTextField(10);
	private JButton levelsChangeButton = new JButton("Set");
	private JCheckBox centerPointCheckBox = new JCheckBox("Create centerpoints:");
	private JCheckBox normalizeParameterSpaceCheckBox = new JCheckBox("Show effects as difference in output between low and high settings");
	private JRadioButton linearModelCoefficientsProcessingRadio = new JRadioButton("Show the coefficients of the linear model built on data");
	private JRadioButton realFactorEffectsProcessingRadio = new JRadioButton("Show effects as difference in output between low and high settings");
	private ButtonGroup processingButtonGroup = new ButtonGroup();
	private JButton showRunsButton = new JButton("Show runs...");
	private JLabel totalRunsLabel = new JLabel("A total of XXX runs");
	private JCheckBox fractionalCheckBox = new JCheckBox("<html>Create a fractional factorial (2<sup>k-p</sup>) design</html>");

	private JPanel fractionalContent = null;
	private JLabel fractionalDesignNameLabel = new JLabel("<html>2<sup>k-p</sup> design</html>");
	private JList fractionalDefinitionList = null;
	private JScrollPane fractionalDefinitionScr = null;
	private JSpinner pSpinner = new JSpinner();
	private JTextField columnGeneratorField = new JTextField(10);
	private JButton moveUpButton = new JButton("Move up");
	private JButton moveDownButton = new JButton("Move down");
	private JButton columnGeneratorSetButton = new JButton("Set");
	private JLabel factorCountLabel = new JLabel("X factors selected for inspection.");
	
	private JTable plusMinusRunsTable = null;
	private JTable realValuesRunsTable = null;
	private JDialog runsDialog = null;
	
	private JButton newParametersButtonCopy = null;
	private JButton newParametersButtonCopyHere = new JButton("Add new parameters...");
	
	
	//------------------------------------------------------------------------------
	public JPanel getSettingsPanel(IIntelliContext ctx) {
		if(context == null){
			context = ctx;
			newParametersButtonCopy = ctx.getNewParametersButton();
			GUIUtils.addActionListener(this, newParametersButtonCopyHere);
		}
		makeSettingsPanel();
		return content;
	}

	private void makeSettingsPanel() {
		if(content == null){
			fractionalHelper = new FactorialFractionalHelper();
			rngSeedManipulator = new RngSeedManipulator(context.getParameters(), this);
			content = FormsUtils.build(	"f:p:g", 
					"0 f:p:g|" +
					"1 p|" +
					"2 f:p:g|" +
					"3 p|" +
					"4 p||" +
					"5 p||" +
					"6 p||" +
					"7 p||",
					getFactorSelectionContent(context.getParameters()),
					fractionalCheckBox,
					getFractionalContent(),
					getCenterPointContent(),
//					normalizeParameterSpaceCheckBox,
					getResultProcessingSelectorContent(),
					rngSeedManipulator.getSeedsListPanel(),
					FormsUtils.buttonStack(showRunsButton).getPanel(),
					totalRunsLabel
			).getPanel();
			//don't show the 'Show runs...' button:
			showRunsButton.setVisible(false);
			centerPointCheckBox.setEnabled(false);
			fractionalCheckBox.setSelected(false);
			setFractionalPanelEnabled(false);
			updateFractionalPanel();
			addButtonListeners();
			addOtherListeners();
			contentScr = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			contentScr.setPreferredSize(new Dimension(GUIUtils.GUI_unit(44.2),GUIUtils.GUI_unit(35)));
			scrollableContent = new JPanel();
			scrollableContent.add(contentScr);
			calculateRunCount();
			fractionalCheckBox.setEnabled(false);
			setNames();
		}
	}
	
	private JPanel getResultProcessingSelectorContent() {
		JPanel ret = FormsUtils.build(
				"p", 
				"0 p|" +
				"1 p",
				realFactorEffectsProcessingRadio,
				linearModelCoefficientsProcessingRadio
				).getPanel();
		if (processingButtonGroup.getButtonCount() == 0) {
			processingButtonGroup.add(realFactorEffectsProcessingRadio);
			processingButtonGroup.add(linearModelCoefficientsProcessingRadio);
		}
		ret.setBorder(BorderFactory.createTitledBorder("Select result processing method"));
		return ret;
	}
	
	private JPanel getCenterPointContent() {
		JPanel centerPointContent = FormsUtils.build(
				"p ~ p ~ p ~", 
				"012 p||", 
				centerPointCheckBox, centerPointSpinner, "instance(s) (3-5 recommended)").getPanel();
		return centerPointContent;
	}

	private JPanel getFactorSelectionContent(List<ParameterInfo> parameters){
		Vector<FactorialParameterInfo> parameterInfos = new Vector<FactorialParameterInfo>();
		for (ParameterInfo parInf : parameters) {
			FactorialParameterInfo factParInf = new FactorialParameterInfo(parInf);
			factParInf.setValue(parInf.getValue());
            parameterInfos.add(factParInf);
        }
		DefaultListModel vv = new DefaultListModel();
		for (FactorialParameterInfo info : parameterInfos) {
            vv.addElement(info);
        }
		int idx = -1;
		for (int i = 0; i < parameterInfos.size(); i++) {
	        if(parameterInfos.get(i).getName().equals("RngSeed"))
	        	idx = i;
        }
		if(idx != -1){
			rngSeedManipulator.addSeed(parameterInfos.get(idx));
		}
		parametersList = new JList(vv);
		parametersScr = new JScrollPane(parametersList);
		DefaultListModel xx = new DefaultListModel();
		factorsList = new JList(xx);
		factorsScr = new JScrollPane(factorsList);
		parametersScr.setPreferredSize(new Dimension(190,155));
		factorsScr.setPreferredSize(new Dimension(230,155));
		factorSelectionPanel = FormsUtils.build("p ~ p ~ f:p:g ~ p ~ p",
				"01233 p|" +
				"45678 p|" +
				"496AB p|" +
				"4_6_C p|" +
				"4D6__ p|" +
				"4_6__ f:p:g|" +
				"E_FFF p",
				"Parameters:", "Default value:", "Factors:", "Factor levels:",
				parametersScr, defaultField, factorsScr, "Low:", lowField,
				defaultChangeButton, "High:", highField,
				levelsChangeButton, FormsUtils.buttonStack(addButton, removeButton).getPanel(),
				newParametersButtonCopyHere,factorCountLabel
				).getPanel();
		factorSelectionPanel.setBorder(BorderFactory.createTitledBorder("Factor selection"));
		newParametersButtonCopyHere.setActionCommand("NEW_PARAMETERS");
		return factorSelectionPanel;
	}
	
	private JPanel getFractionalContent(){
		if(fractionalContent == null){
			fractionalDefinitionList = new JList();
			fractionalDefinitionScr = new JScrollPane(fractionalDefinitionList);
			fractionalDefinitionScr.setPreferredSize(new Dimension(190,60));
			pSpinner.setModel(new SpinnerNumberModel(1,1,7,1));
			pSpinner.setPreferredSize(new Dimension(85, 30));
			
			fractionalContent = FormsUtils.build("p p ~ f:p:g ~ p", 
								"0000 p|" +
								"__1_ p|" + 
								"2345 p|" +
								"__46 p|" +
								"__4_ 5dlu|" +
								"__47 p|" +
								"__48 p|" +
								"__49 p|" +
								"__4_ f:p:g", 
								fractionalDesignNameLabel,"Column generators:",
								"p=",pSpinner,
								fractionalDefinitionScr,moveUpButton,moveDownButton,
								"Column generator:",
								columnGeneratorField,columnGeneratorSetButton).getPanel();
			fractionalContent.setBorder(BorderFactory.createTitledBorder("Fractional settings"));
		}
		return fractionalContent;
	}

	private void moveSelectedParametersToFactors(){
		Object[] selectedParams = parametersList.getSelectedValues();
		if(selectedParams.length > 0){
			for (int i = 0; i < selectedParams.length; i++) {
				FactorialParameterInfo paramInfo = (FactorialParameterInfo) selectedParams[i];
                FactorialFactorInfo factInfo = new FactorialFactorInfo(paramInfo);
                factInfo.setValue(paramInfo.getValue());
                boolean highValueDefaultSet = false;
                boolean lowValueDefaultSet = false;
                if (!factInfo.getType().equalsIgnoreCase("boolean") && !factInfo.getType().equalsIgnoreCase("string")) {
                	if (paramInfo.getHighValue() != null)
                		factInfo.setHighValue(paramInfo.getHighValue());
                	else {
                		Double highValue = 1.1 * ((Number)paramInfo.getValue()).doubleValue();
                		if (factInfo.getType().equalsIgnoreCase("double")) { 
                			factInfo.setHighValue(highValue);
                		} else if (factInfo.getType().equalsIgnoreCase("float")) { 
                			factInfo.setHighValue(highValue.floatValue());
                		} else if (factInfo.getType().equalsIgnoreCase("long")) { 
                			factInfo.setHighValue(highValue.longValue());
                		} else if (factInfo.getType().equalsIgnoreCase("int") || factInfo.getType().equalsIgnoreCase("integer")) { 
                			factInfo.setHighValue(highValue.intValue());
                		} else if (factInfo.getType().equalsIgnoreCase("short")) { 
                			factInfo.setHighValue(highValue.shortValue());
                		} else if (factInfo.getType().equalsIgnoreCase("byte")) { 
                			factInfo.setHighValue(highValue.byteValue());
                		}
                		highValueDefaultSet = true;
                	}
                	if (paramInfo.getLowValue() != null) 
                		factInfo.setLowValue(paramInfo.getLowValue());
                	else {
                		Double lowValue = 0.9 * ((Number)paramInfo.getValue()).doubleValue();
                		if (factInfo.getType().equalsIgnoreCase("double")) { 
                			factInfo.setLowValue(lowValue);
                		} else if (factInfo.getType().equalsIgnoreCase("float")) { 
                			factInfo.setLowValue(lowValue.floatValue());
                		} else if (factInfo.getType().equalsIgnoreCase("long")) { 
                			factInfo.setLowValue(lowValue.longValue());
                		} else if (factInfo.getType().equalsIgnoreCase("int") || factInfo.getType().equalsIgnoreCase("integer")) { 
                			factInfo.setLowValue(lowValue.intValue());
                		} else if (factInfo.getType().equalsIgnoreCase("short")) { 
                			factInfo.setLowValue(lowValue.shortValue());
                		} else if (factInfo.getType().equalsIgnoreCase("byte")) { 
                			factInfo.setLowValue(lowValue.byteValue());
                		}
                		lowValueDefaultSet = true;
                	}
                	if (highValueDefaultSet && factInfo.getValue().equals(factInfo.getHighValue())) {
                		Long newValue = ((Number) factInfo.getValue()).longValue();
                		newValue += (newValue < 0) ? -1 : 1;
                		if (factInfo.getType().equalsIgnoreCase("long")) { 
                			factInfo.setHighValue(newValue.longValue());
                		} else if (factInfo.getType().equalsIgnoreCase("int") || factInfo.getType().equalsIgnoreCase("integer")) { 
                			factInfo.setHighValue(newValue.intValue());
                		} else if (factInfo.getType().equalsIgnoreCase("short")) { 
                			factInfo.setHighValue(newValue.shortValue());
                		} else if (factInfo.getType().equalsIgnoreCase("byte")) { 
                			factInfo.setHighValue(newValue.byteValue());
                		}
                	}
                	if (lowValueDefaultSet && factInfo.getValue().equals(factInfo.getLowValue())) {
                		Long newValue = ((Number) factInfo.getValue()).longValue();
                		newValue += (newValue < 0) ? 1 : -1;
                		if (factInfo.getType().equalsIgnoreCase("long")) { 
                			factInfo.setLowValue(newValue.longValue());
                		} else if (factInfo.getType().equalsIgnoreCase("int") || factInfo.getType().equalsIgnoreCase("integer")) { 
                			factInfo.setLowValue(newValue.intValue());
                		} else if (factInfo.getType().equalsIgnoreCase("short")) { 
                			factInfo.setLowValue(newValue.shortValue());
                		} else if (factInfo.getType().equalsIgnoreCase("byte")) { 
                			factInfo.setLowValue(newValue.byteValue());
                		}
                	}
                }
                DefaultListModel paramListModel = (DefaultListModel) parametersList.getModel();  
                paramListModel.removeElement(paramInfo);
                DefaultListModel factorListModel = (DefaultListModel) factorsList.getModel();
                if (factInfo.getType().equalsIgnoreCase("boolean")) {
                	if (factInfo.getLowValue() != null && factInfo.getHighValue() != null) {
                		if (factInfo.getLowValue().equals(Boolean.FALSE) && 
                				factInfo.getHighValue().equals(Boolean.FALSE)) {
                			factInfo.setHighValue(new Boolean(true));
                		}
                		if (factInfo.getLowValue().equals(Boolean.TRUE) && 
                				factInfo.getHighValue().equals(Boolean.TRUE)) {
                			factInfo.setHighValue(new Boolean(false));
                		}
                	} else {
                		if (factInfo.getLowValue() != null) {
                			boolean low = (Boolean) factInfo.getLowValue();
                			factInfo.setHighValue(new Boolean(!low));
                		} else if (factInfo.getHighValue() != null) {
                			boolean high = (Boolean) factInfo.getHighValue();
                			factInfo.setLowValue(new Boolean(!high));
                		} else {
                			factInfo.setLowValue(new Boolean(false));
                			factInfo.setHighValue(new Boolean(true));
                		}
                	}
                }
                if(	factInfo.getLowValue() == null && 
                	factInfo.getHighValue() == null && 
                	factInfo.getType().equalsIgnoreCase("boolean")) {
                		factInfo.setLowValue(new Boolean(false));
                		factInfo.setHighValue(new Boolean(true));
                }
                factorListModel.addElement(factInfo);
                fractionalHelper.addFactor(factInfo);
                defaultField.setText("");
                factorsList.setSelectedValue(factInfo, true);
                if(factInfo.getLowValue() == null) lowField.requestFocusInWindow();
                else highField.requestFocusInWindow();
            }
			factorsChanged();
		}
	}
	
	private void moveSelectedFactorsToParameters(){
		Object[] selectedFactors = factorsList.getSelectedValues();
		if(selectedFactors.length > 0){
			for (int i = 0; i < selectedFactors.length; i++) {
				FactorialFactorInfo factInfo = (FactorialFactorInfo) selectedFactors[i];
                FactorialParameterInfo paramInfo = new FactorialParameterInfo(factInfo);
                paramInfo.setValue(factInfo.getValue());
                paramInfo.setHighValue(factInfo.getHighValue());
                paramInfo.setLowValue(factInfo.getLowValue());
                ((DefaultListModel) (factorsList.getModel())).removeElement(factInfo);
                fractionalHelper.removeFactor(factInfo);
                ((DefaultListModel) (parametersList.getModel())).addElement(paramInfo);
                highField.setText("");
                lowField.setText("");
            }
			factorsChanged();
		}
	}
	//------------------------------------------------------------------------------
	private void addButtonListeners() {
		addButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				moveSelectedParametersToFactors();
            }
		});
		removeButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				moveSelectedFactorsToParameters();
            }
		});
		defaultChangeButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				FactorialParameterInfo factParamInfo = (FactorialParameterInfo) parametersList.getSelectedValue();
				String newValue = defaultField.getText();
				if(factParamInfo != null){
					try{
						if ("byte".equals(factParamInfo.getType()) || "Byte".equals(factParamInfo.getType()))
							factParamInfo.setValue(new Byte(newValue));
						else if ("short".equals(factParamInfo.getType()) || "Short".equals(factParamInfo.getType())) 
							factParamInfo.setValue(new Short(newValue));
						else if ("int".equals(factParamInfo.getType()) || "Integer".equals(factParamInfo.getType()))
							factParamInfo.setValue(new Integer(newValue));
						else if ("long".equals(factParamInfo.getType()) || "Long".equals(factParamInfo.getType()))
							factParamInfo.setValue(new Long(newValue));
						else if ("float".equals(factParamInfo.getType()) || "Float".equals(factParamInfo.getType()))
							factParamInfo.setValue(new Float(newValue));
						else if ("double".equals(factParamInfo.getType()) || "Double".equals(factParamInfo.getType()))
							factParamInfo.setValue(new Double(newValue));
						else if ("boolean".equals(factParamInfo.getType()) || "Boolean".equals(factParamInfo.getType()))
							factParamInfo.setValue(new Boolean(newValue));
						else if ("String".equals(factParamInfo.getType()))
							factParamInfo.setValue(newValue);
						parametersList.setVisible(false);
						parametersList.setVisible(true);
					} catch(NumberFormatException ne){
						Utilities.userAlert(content, "The default value is not compatible with the type.");
					}
				}
			}
		});
		levelsChangeButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				FactorialFactorInfo factFactorInfo = (FactorialFactorInfo) factorsList.getSelectedValue();
				String newLowValue = lowField.getText();
				String newHighValue = highField.getText();
				if(factFactorInfo != null){
					try{
						if ("byte".equals(factFactorInfo.getType()) || "Byte".equals(factFactorInfo.getType()))
							factFactorInfo.setLowValue(new Byte(newLowValue));
						else if ("short".equals(factFactorInfo.getType()) || "Short".equals(factFactorInfo.getType())) 
							factFactorInfo.setLowValue(new Short(newLowValue));
						else if ("int".equals(factFactorInfo.getType()) || "Integer".equals(factFactorInfo.getType()))
							factFactorInfo.setLowValue(new Integer(newLowValue));
						else if ("long".equals(factFactorInfo.getType()) || "Long".equals(factFactorInfo.getType()))
							factFactorInfo.setLowValue(new Long(newLowValue));
						else if ("float".equals(factFactorInfo.getType()) || "Float".equals(factFactorInfo.getType()))
							factFactorInfo.setLowValue(new Float(newLowValue));
						else if ("double".equals(factFactorInfo.getType()) || "Double".equals(factFactorInfo.getType()))
							factFactorInfo.setLowValue(new Double(newLowValue));
						else if ("boolean".equals(factFactorInfo.getType()) || "Boolean".equals(factFactorInfo.getType()))
							factFactorInfo.setLowValue(new Boolean(newLowValue));
						else if ("String".equals(factFactorInfo.getType()))
							factFactorInfo.setLowValue(newLowValue);
					} catch(NumberFormatException ne){
						Utilities.userAlert(content, "The low level value is not compatible with the type.");
					}
					try{
						if ("byte".equals(factFactorInfo.getType()) || "Byte".equals(factFactorInfo.getType()))
							factFactorInfo.setHighValue(new Byte(newHighValue));
						else if ("short".equals(factFactorInfo.getType()) || "Short".equals(factFactorInfo.getType())) 
							factFactorInfo.setHighValue(new Short(newHighValue));
						else if ("int".equals(factFactorInfo.getType()) || "Integer".equals(factFactorInfo.getType()))
							factFactorInfo.setHighValue(new Integer(newHighValue));
						else if ("long".equals(factFactorInfo.getType()) || "Long".equals(factFactorInfo.getType()))
							factFactorInfo.setHighValue(new Long(newHighValue));
						else if ("float".equals(factFactorInfo.getType()) || "Float".equals(factFactorInfo.getType()))
							factFactorInfo.setHighValue(new Float(newHighValue));
						else if ("double".equals(factFactorInfo.getType()) || "Double".equals(factFactorInfo.getType()))
							factFactorInfo.setHighValue(new Double(newHighValue));
						else if ("boolean".equals(factFactorInfo.getType()) || "Boolean".equals(factFactorInfo.getType()))
							factFactorInfo.setHighValue(new Boolean(newHighValue));
						else if ("String".equals(factFactorInfo.getType()))
							factFactorInfo.setHighValue(newHighValue);
					} catch(NumberFormatException ne){
						Utilities.userAlert(content, "The high level value is not compatible with the type.");
					}
					factorsList.setVisible(false);
					factorsList.setVisible(true);
					factorsChanged();
				}
			}
		});
		ActionListener factorMoveListener = new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				int newPos = 0;
				if(e.getActionCommand().compareTo("Move up") == 0){
					newPos = fractionalHelper.factorMoveUp(fractionalDefinitionList.getSelectedIndex());
				}
				if(e.getActionCommand().compareTo("Move down") == 0){
					newPos = fractionalHelper.factorMoveDown(fractionalDefinitionList.getSelectedIndex());
				}
				updateFractionalPanel();
				fractionalDefinitionList.setSelectedIndex(newPos);
				fractionalDefinitionList.ensureIndexIsVisible(newPos);
            }
		};
		moveUpButton.addActionListener(factorMoveListener);
		moveDownButton.addActionListener(factorMoveListener);
		columnGeneratorSetButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(fractionalDefinitionList.getSelectedIndex() > 0){
					fractionalHelper.getActualDesign().setColumnAtByMultiplying(fractionalDefinitionList.getSelectedIndex(), columnGeneratorField.getText());
					updateFractionalPanel();
				}
            }
		});
		showRunsButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				showRunsDialog();
            }
		});
    }

	//------------------------------------------------------------------------------
	/**
	 * Runs after the list of factors changed. Calculates the new centerpoint, 
	 * the number of runs, updates the Fractional Settings panel, 
	 */
	protected void factorsChanged() {
		boolean noStringsOrBooleansOrNullValuesOrEmptyList = true;
		if(factorsList.getModel().getSize() == 0) {
			noStringsOrBooleansOrNullValuesOrEmptyList = false;
			factorCountLabel.setText("No factors selected for inspection.");
		} else
			if(factorsList.getModel().getSize() == 1) {
				factorCountLabel.setText("1 factor selected for inspection.");
			} else{
				factorCountLabel.setText("" + factorsList.getModel().getSize() + " factors selected for inspection.");
			}
			for (int i = 0; i < factorsList.getModel().getSize(); i++) {
				if(		((FactorialFactorInfo) factorsList.getModel().getElementAt(i)).getType().equalsIgnoreCase("boolean") ||
						((FactorialFactorInfo) factorsList.getModel().getElementAt(i)).getType().equalsIgnoreCase("string") ||
						((FactorialFactorInfo) factorsList.getModel().getElementAt(i)).getLowValue() == null ||
						((FactorialFactorInfo) factorsList.getModel().getElementAt(i)).getHighValue() == null){
					noStringsOrBooleansOrNullValuesOrEmptyList = false;
				}
			}
		if(noStringsOrBooleansOrNullValuesOrEmptyList){
			if(centerPointCheckBox.isSelected() && centerPointCheckBox.isEnabled())
				calculateCenterpoint();
		}
		else {
			centerPointCheckBox.setSelected(false);
			centerPointCheckBox.setEnabled(false);
		}
		if(!centerPointCheckBox.isEnabled() && noStringsOrBooleansOrNullValuesOrEmptyList){
			centerPointCheckBox.setEnabled(true);
		}
		Vector<ParameterInfo> newParamList = new Vector<ParameterInfo>();
		//adding the hidden randomseeds first:
//TODO: megcsin�lni, hogy ne l�tsszanak a seed-ek a param�terek k�zt
//		newParamList.addAll(hiddenParamsAsSeeds);
		for (int i = 0; i < parametersList.getModel().getSize(); i++) {
	        newParamList.add((ParameterInfo) parametersList.getModel().getElementAt(i));
        }
		Vector<ParameterInfo> newFactorList = new Vector<ParameterInfo>();
		for (int i = 0; i < factorsList.getModel().getSize(); i++) {
	        newFactorList.add((ParameterInfo) factorsList.getModel().getElementAt(i));
        }
		rngSeedManipulator.updatePossibleRandomSeedParameters(newParamList, newFactorList);
		//moving the seeds from the parameters list into a hidden list:
/*		Vector<Integer> toRemoveIndices = new Vector<Integer>();
		hiddenParamsAsSeeds = new Vector<FactorialParameterInfo>();
		for (int i = 0; i < parametersList.getModel().getSize(); i++) {
			if(rngSeedManipulator.isSeed((ParameterInfo) parametersList.getModel().getElementAt(i))){
				toRemoveIndices.add(i);
				hiddenParamsAsSeeds.add((FactorialParameterInfo) parametersList.getModel().getElementAt(i));
			}
		} 
		for (FactorialParameterInfo info : hiddenParamsAsSeeds) {
	        ((DefaultListModel) parametersList.getModel()).removeElement(info);
        }*/
		int factorCount = factorsList.getModel().getSize();
		if(factorCount < 3){
			fractionalCheckBox.setSelected(false);
			fractionalCheckBox.setEnabled(false);
			setFractionalPanelEnabled(false);
		} else{
			fractionalCheckBox.setEnabled(true);
		}
		calculateRunCount();
		updateFractionalPanel();
    }

	private void updateParametersList(){
		Vector<ParameterInfo> params = new Vector<ParameterInfo>(context.getParameters());
		Vector<FactorialFactorInfo> factorsToRemove = new Vector<FactorialFactorInfo>();
		Vector<FactorialParameterInfo> parametersToRemove = new Vector<FactorialParameterInfo>();
//		((DefaultListModel)(factorsList.getModel())).
		//Check if the chosen factors are still present:
		for (int i = 0; i < factorsList.getModel().getSize(); i++) {
	        FactorialFactorInfo info = (FactorialFactorInfo) factorsList.getModel().getElementAt(i);
	        boolean present = false;
	        for (Iterator iter = params.iterator(); iter.hasNext();) {
	            ParameterInfo info2 = (ParameterInfo) iter.next();
	            if(info2.getName().equals(info.getName())){
	            	present = true;
	            	//remove, we do not want to check it again:
	            	iter.remove();
	            }
            }
	        if(!present) factorsToRemove.add(info);
        }
		//Remove the non-existent factors:
		for (FactorialFactorInfo info : factorsToRemove) {
			((DefaultListModel)(factorsList.getModel())).removeElement(info);
        }
		//Check if the listed parameters are still present:
		for (int i = 0; i < parametersList.getModel().getSize(); i++) {
	        FactorialParameterInfo info = (FactorialParameterInfo) parametersList.getModel().getElementAt(i);
	        boolean present = false;
	        for (Iterator iter = params.iterator(); iter.hasNext();) {
	            ParameterInfo info2 = (ParameterInfo) iter.next();
	            if(info2.getName().equals(info.getName())){
	            	present = true;
	            	//remove, we do not want to check it again:
	            	iter.remove();
	            }
            }
	        if(!present) parametersToRemove.add(info);
        }
		//Remove the non-existent parameters:
		for (FactorialParameterInfo info : parametersToRemove) {
			((DefaultListModel)(parametersList.getModel())).removeElement(info);
        }
		//Add the new parameters to parameters:
		//The paramters to add remained in the params Vector:
		for (ParameterInfo info : params) {
	        FactorialParameterInfo newParam = new FactorialParameterInfo(info);
	        ((DefaultListModel)(parametersList.getModel())).addElement(newParam);
        }
	}
	
	private void calculateCenterpoint() {
    }
	
	private int calculateRunCount(){
		int runCount = getDesignSize();
		runCount *= rngSeedManipulator.getRunsMultiplierFactor();
		totalRunsLabel.setText("A total of "+runCount+" runs.");
		return runCount;
	}

	//------------------------------------------------------------------------------
	private void addOtherListeners() {
		parametersList.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				FactorialParameterInfo paramInfo = (FactorialParameterInfo) parametersList.getSelectedValue();
				if(paramInfo != null){
					defaultField.setText(paramInfo.getValue() == null ? "" : paramInfo.getValue().toString());
				}
            }
		});
		factorsList.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				FactorialFactorInfo factInfo = (FactorialFactorInfo) factorsList.getSelectedValue();
				if(factInfo != null){
					lowField.setText(factInfo.getLowValue() == null ? "" : factInfo.getLowValue().toString());
					highField.setText(factInfo.getHighValue() == null ? "" : factInfo.getHighValue().toString());
				}
            }
		});
		centerPointCheckBox.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				if(centerPointCheckBox.isSelected()) calculateCenterpoint();
				calculateRunCount();
            }
		});
		fractionalCheckBox.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				if(fractionalCheckBox.isSelected()) {
					setFractionalPanelEnabled(true);
					updateFractionalPanel();
				}
				else{
					setFractionalPanelEnabled(false);
				}
				calculateRunCount();
            }
		});
		pSpinner.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e) {
				updateFractionalPanel();
            }
		});
		rngSeedManipulator.addChangeListener(this);
		centerPointSpinner.addChangeListener(this);
    }
	
	private void setFractionalPanelEnabled(boolean b){
		Component[] comps = fractionalContent.getComponents();
		for (Component component : comps) {
            component.setEnabled(b);
        }
		fractionalDefinitionList.setEnabled(b);
		fractionalDesignNameLabel.setEnabled(b);
		fractionalContent.setEnabled(b);
	}

	private void updateFractionalPanel() {
		int k = factorsList.getModel().getSize();
		if(k < 3){
			fractionalDesignNameLabel.setText("No possible fractional design.");
			fractionalDefinitionList = new JList();
		} else{
			int value = ((SpinnerNumberModel) pSpinner.getModel()).getNumber().intValue();
			if(value > k - 2){
				((SpinnerNumberModel) pSpinner.getModel()).setValue(k-2);
			}
			((SpinnerNumberModel) pSpinner.getModel()).setMaximum(k-2);
			int p = ((SpinnerNumberModel) pSpinner.getModel()).getNumber().intValue();
			fractionalHelper.setP(p);
			if(		fractionalHelper.getActualDesign() != null && 
					fractionalHelper.getActualDesign().getK() == k &&
					fractionalHelper.getActualDesign().getP() == p){
				;//do nothing ???
			} else{
				fractionalHelper.setActualDesign(new FactorialDesign(k,p));
			}
			fractionalDefinitionList = new JList(fractionalHelper.getFactorsNamesConfoundings());
			fractionalDefinitionScr.setViewportView(fractionalDefinitionList);
			fractionalDefinitionList.setEnabled(fractionalContent.isEnabled());
			fractionalDefinitionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			fractionalDefinitionList.addListSelectionListener(new ListSelectionListener(){
				public void valueChanged(ListSelectionEvent e) {
					String genStr = fractionalHelper.getActualDesign().getColumnGeneratorAt(fractionalDefinitionList.getSelectedIndex());
					if(genStr.compareTo("?") != 0){
						columnGeneratorField.setText(genStr);
						if(genStr.length() == 1){
							columnGeneratorField.setEnabled(false);
						} else{
							columnGeneratorField.setEnabled(true);
						}
					} else{
						columnGeneratorField.setText("");
						columnGeneratorField.setEnabled(true);
					}
                }
			});
			String resolutionText = "";
			String resolutionWarningText = "";
			if (fractionalHelper.getActualDesign() != null) {
				int resolution = fractionalHelper.getActualDesign().getDesignResolution();
				resolutionText = ", resolution = " + (resolution == 0 ? "not defined":resolution);
				if (!FactorialFractionalPresets.isAPreset(fractionalHelper.getActualDesign()))
					resolutionWarningText = " <b>User defined columns generators, no guarantee for correct resolution calculation.</b>";
			}
			String fractionalDesignLabelText = "<html>2<sup>" +
				factorsList.getModel().getSize()+ "-" +
				pSpinner.getValue() + "</sup> design" +
						resolutionText +
						resolutionWarningText +
						"</html>";	
			fractionalDesignNameLabel.setText(fractionalDesignLabelText);
		}
		calculateRunCount();
    }

	//------------------------------------------------------------------------------
	public boolean getReadyStatus() {
		DefaultListModel factors = (DefaultListModel) (factorsList.getModel());
		DefaultListModel params = (DefaultListModel) (parametersList.getModel());
		if (factors.getSize() == 0) {
			readyStatusDetail = "There are no factors selected";
			return false;
		}
		for(int i = 0; i < factors.getSize(); i++){
			if (((FactorialFactorInfo) factors.get(i)).getLowValue() == null) {
				readyStatusDetail = ((FactorialFactorInfo) factors.get(i)).getName() + " factor does not have low value";
				return false;
			}
			if (((FactorialFactorInfo) factors.get(i)).getHighValue() == null) {
				readyStatusDetail = ((FactorialFactorInfo) factors.get(i)).getName() + " factor does not have high value";
				return false;
			}
		}
		for(int i = 0; i < params.getSize(); i++){
			if (((FactorialParameterInfo) params.get(i)).getValue() == null) {
				readyStatusDetail = ((FactorialParameterInfo) params.get(i)).getName() + " parameter has no default value";
				return false;
			}
		}
		if (fractionalCheckBox.isSelected()) {
			if (!fractionalHelper.getActualDesign().getReadyStatus()) {
				readyStatusDetail = "There are empty design column generator(s) in the fractional settings (displayed as '?'), please fill them in.";
				return false;
			}
		}
		if (centerPointCheckBox.isSelected() && (Integer) centerPointSpinner.getValue() > 1) {
			if (rngSeedManipulator.getRandomSeedNames().length == 0) {
				readyStatusDetail = "There are more than 1 center point runs with no random seeds.\nYou should configure at least one random seed or have only one centerpoint run.";
				return false;
			}
		}
		if (!rngSeedManipulator.isNaturalVariationConsistent()) {
			readyStatusDetail = "You selected Variation analysis, but do not have at least one random seed\nwith Sweep setting, with more than one value.";
			return false;
		}
		if (!rngSeedManipulator.isBlockingConsistent()) {
			readyStatusDetail = "You selected Blocking design, but do not have at least one random seed\nwith Sweep setting, with more than one value.";
			return false;
		}
		readyStatusDetail = "Ready.";
		return true;
	}
	
	public String getReadyStatusDetail() {
		getReadyStatus();
		return readyStatusDetail;
    }
	

	//------------------------------------------------------------------------------
	public String getLocalizedName() {
		return "Factorial";
	}
	
	private DefaultMutableTreeNode getAlteredParameterTreeRootNode(){
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Parameter file");
		for (int i = 0; i < parametersList.getModel().getSize(); i++) {
	        FactorialParameterInfo defaultParamInfo = (FactorialParameterInfo) parametersList.getModel().getElementAt(i);
	        defaultParamInfo.setDefinitionType(ParameterInfo.CONST_DEF);
	        defaultParamInfo.setRuns(1);
	        root.add(new DefaultMutableTreeNode(defaultParamInfo));
        }
		int runsNoCenterPoints = calculateRunCount();
		if(centerPointCheckBox.isSelected()) runsNoCenterPoints -= (Integer) centerPointSpinner.getValue();
		if(fractionalCheckBox.isSelected()){
			for (int i = 0; i < fractionalDefinitionList.getModel().getSize(); i++) {
		        FactorialFactorInfo factorInfo = fractionalHelper.getFactorAt(i);
				factorInfo.setDefinitionType(ParameterInfo.LIST_DEF);
				factorInfo.setRuns(1);
				ArrayList<Object> values = new ArrayList<Object>(factorInfo.getValues());
				values.clear();
				for (int j = 0; j < fractionalHelper.getActualDesign().getColumn(i).size(); j++) {
		            if(fractionalHelper.getActualDesign().getColumn(i).get(j) < 0){
		            	values.add(factorInfo.getLowValue());
		            }else{
		            	values.add(factorInfo.getHighValue());
		            }
				}
				factorInfo.setValues(values);
				root.add(new DefaultMutableTreeNode(factorInfo));
            }
		}else{
			FactorialDesign fullDesign = new FactorialDesign(factorsList.getModel().getSize());
			for (int i = 0; i < factorsList.getModel().getSize(); i++) {
		        FactorialFactorInfo factorInfo = (FactorialFactorInfo) factorsList.getModel().getElementAt(i);
				factorInfo.setDefinitionType(ParameterInfo.LIST_DEF);
				factorInfo.setRuns(1);
				ArrayList<Object> values = new ArrayList<Object>(factorInfo.getValues());
				values.clear();
				for (int j = 0; j < fullDesign.getColumn(i).size(); j++) {
		            if(fullDesign.getColumn(i).get(j) < 0){
		            	values.add(factorInfo.getLowValue());
		            }else{
		            	values.add(factorInfo.getHighValue());
		            }
				}
				factorInfo.setValues(values);
				root.add(new DefaultMutableTreeNode(factorInfo));
            }
		}
		rngSeedManipulator.getModel().randomizeParameterTree(root);
		DefaultMutableTreeNode cpRoot = new DefaultMutableTreeNode("Parameter file");
		if(centerPointCheckBox.isSelected()){
			for (int i = 0; i < parametersList.getModel().getSize(); i++) {
		        FactorialParameterInfo defaultParamInfo = (FactorialParameterInfo) parametersList.getModel().getElementAt(i);
		        ParameterInfo paramInfo = new ParameterInfo(defaultParamInfo.getName(),defaultParamInfo.getType(),defaultParamInfo.getJavaType());
		        paramInfo.setDefinitionType(ParameterInfo.CONST_DEF);
		        paramInfo.setValue(defaultParamInfo.getValue());
		        cpRoot.add(new DefaultMutableTreeNode(paramInfo));
	        }
			for (int i = 0; i < factorsList.getModel().getSize(); i++) {
		        FactorialFactorInfo factorInfo = (FactorialFactorInfo) factorsList.getModel().getElementAt(i);
		        ParameterInfo paramInfo = new ParameterInfo(factorInfo.getName(),factorInfo.getType(),factorInfo.getJavaType());
		        paramInfo.setValue(calculateCenterPointValue(factorInfo));
		        paramInfo.setDefinitionType(ParameterInfo.LIST_DEF);
		        cpRoot.add(new DefaultMutableTreeNode(paramInfo));
			}
			rngSeedManipulator.getModel().randomizeCenterPoint(cpRoot, (Integer)(centerPointSpinner.getValue()));
			unifyTreeAndCenterpoint(root, cpRoot);
		}
		return root;
	}

	private static void unifyTreeAndCenterpoint(DefaultMutableTreeNode root,
			DefaultMutableTreeNode cpRoot) {
		Vector<Integer> listIndices = new Vector<Integer>();
		int toAddLength = -1;
		for (int i = 0; i < root.getChildCount(); i++) {
			ParameterInfo pi = (ParameterInfo)((DefaultMutableTreeNode) root.getChildAt(i)).getUserObject();
			if(pi.getDefinitionType() == ParameterInfo.LIST_DEF){
				listIndices.add(i);
				int toAddLength2 = ((ParameterInfo)((DefaultMutableTreeNode) cpRoot.getChildAt(i)).getUserObject()).getValues().size();
				if(toAddLength2 > toAddLength) toAddLength = toAddLength2;
			}
		}
		for (int i = 0; i < listIndices.size(); i++) {
			int idx = listIndices.get(i);
			ParameterInfo pi = (ParameterInfo)((DefaultMutableTreeNode) root.getChildAt(idx)).getUserObject();
			ParameterInfo cp = (ParameterInfo)((DefaultMutableTreeNode) cpRoot.getChildAt(idx)).getUserObject();
			if (cp.getDefinitionType() == ParameterInfo.LIST_DEF){
				pi.getValues().addAll(cp.getValues());
			} else {
				for (int j = 0; j < toAddLength; j++) {
					pi.getValues().add(cp.getValue());
				}
			}
		}
	}
	
	public void setNames()
	{
		parametersList.setName("lst_factorial_paramlist");
		addButton.setName("btn_factorial_add");
		removeButton.setName("btn_factorial_remove");
		defaultChangeButton.setName("btn_factorial_setdefault");
		levelsChangeButton.setName("btn_factorial_setlevel");
		factorsList.setName("lst_factorial_factors");
		defaultField.setName("fld_factorial_default");
		highField.setName("fld_factorial_high");
		lowField.setName("fld_factorial_low");
		linearModelCoefficientsProcessingRadio.setName("rbtn_factorial_difference");
		realFactorEffectsProcessingRadio.setName("rbtn_factorial_coeffs");
		centerPointCheckBox.setName("cbox_factorial_centerpoint");
		centerPointSpinner.setName("fld_factorial_centerspinner");
		pSpinner.setName("fld_factorial_pspinner");
		moveUpButton.setName("btn_factorial_moveup");
		moveDownButton.setName("btn_factorial_movedown");
		columnGeneratorSetButton.setName("btn_factorial_setcolumn");
		fractionalCheckBox.setName("cbox_factorial_fractional");		
	}

	public boolean alterParameterTree(IIntelliContext ctx) {
		DefaultMutableTreeNode root = ctx.getParameterTreeRootNode();
		DefaultMutableTreeNode newRoot = getAlteredParameterTreeRootNode();
		root.removeAllChildren();
		int count = newRoot.getChildCount();
		for (int i = 0; i < count; i++) {
	        root.add((DefaultMutableTreeNode) newRoot.getChildAt(0));
        }
		return true;
    }

	@SuppressWarnings("cast")
	public static Object calculateCenterPointValue(FactorialFactorInfo factorInfo) {
		Object ret = null;
		if ("byte".equals(factorInfo.getType()) || "Byte".equals(factorInfo.getType()))
			ret = new Byte(	(byte)(((new Byte(factorInfo.getLowValue().toString())) + 
							(new Byte(factorInfo.getHighValue().toString()))) / 2));
		else if ("short".equals(factorInfo.getType()) || "Short".equals(factorInfo.getType())) 
			ret = new Short((short)(((new Short(factorInfo.getLowValue().toString())) + 
							(new Short(factorInfo.getHighValue().toString()))) / 2));
		else if ("int".equals(factorInfo.getType()) || "Integer".equals(factorInfo.getType()))
			ret = new Integer(	(int)(((new Integer(factorInfo.getLowValue().toString())) + 
								(new Integer(factorInfo.getHighValue().toString()))) / 2));
		else if ("long".equals(factorInfo.getType()) || "Long".equals(factorInfo.getType()))
			ret = new Long(	(long)(((new Long(factorInfo.getLowValue().toString())) + 
							(new Long(factorInfo.getHighValue().toString()))) / 2));
		else if ("float".equals(factorInfo.getType()) || "Float".equals(factorInfo.getType()))
			ret = new Float(	(float)(((new Float(factorInfo.getLowValue().toString())) + 
								(new Float(factorInfo.getHighValue().toString()))) / 2));
		else if ("double".equals(factorInfo.getType()) || "Double".equals(factorInfo.getType()))
			ret = new Double(	(double)(((new Double(factorInfo.getLowValue().toString())) + 
								(new Double(factorInfo.getHighValue().toString()))) / 2));
	    return ret;
    }
	
	private void showRunsDialog(){
		if(runsDialog == null){
			JTabbedPane tabPane = new JTabbedPane();
			JButton closeDialogButton = new JButton("Close");
			JPanel closeDialogButtonPanel = FormsUtils.buttonStack(closeDialogButton).getPanel();
			JPanel content = new JPanel(new BorderLayout());
			content.add(tabPane,BorderLayout.CENTER);
			content.add(closeDialogButtonPanel, BorderLayout.SOUTH);
			realValuesRunsTable = new JTable();
			plusMinusRunsTable = new JTable();
			JScrollPane realValuesRunsTableScr = new JScrollPane(realValuesRunsTable);
			JScrollPane plusMinusRunsTableScr = new JScrollPane(plusMinusRunsTable);
			tabPane.addTab("Values", null, realValuesRunsTableScr, "Table of runs with the values");
			tabPane.addTab("+/- signs", null, plusMinusRunsTableScr, "Table of runs with +/- signs");
			runsDialog = new JDialog();
			runsDialog.setModal(true);
			runsDialog.setTitle("Show runs");
			runsDialog.setPreferredSize(new Dimension(400,250));
			final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			sp.setBorder(null);
			runsDialog.setContentPane(sp);
			runsDialog.pack();
			Dimension oldD = runsDialog.getPreferredSize();
			runsDialog.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
											     	  oldD.height + sp.getHorizontalScrollBar().getHeight()));
			sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			oldD = runsDialog.getPreferredSize();
			final Dimension newD = GUIUtils.getPreferredSize(runsDialog);
			if (!oldD.equals(newD)) 
				runsDialog.setPreferredSize(newD);
			runsDialog.pack();
			closeDialogButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					runsDialog.setVisible(false);
                }
			});
		}
		//creating the JTables:
		realValuesRunsTable.setModel(new FactorialTableModel(true));
		realValuesRunsTable.createDefaultColumnsFromModel();
		plusMinusRunsTable.setModel(new FactorialTableModel(false));
		plusMinusRunsTable.createDefaultColumnsFromModel();
		runsDialog.setVisible(true);
	}
	
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().compareTo("NEW_PARAMETERS") == 0){
			newParametersButtonCopy.doClick();
			updateParametersList();
			factorsChanged();
		}
	}
	
	private class FactorialTableModel implements TableModel{
		DefaultMutableTreeNode root = null;
		int constantParameterCount = -1;
		boolean showValues = true;
		
		public FactorialTableModel(boolean showValues){
			root = getAlteredParameterTreeRootNode();
	        int idx = 0;
	        while(((DefaultMutableTreeNode)(root.getChildAt(idx))).getUserObject().getClass().getName().endsWith("FactorialParameterInfo")){
	        	idx++;
	        }
	        constantParameterCount = idx;
	        this.showValues = showValues;
		}

		public void addTableModelListener(TableModelListener l) {
        }

		public Class<?> getColumnClass(int columnIndex) {
			if(columnIndex == 0) return Integer.class;
			columnIndex--;
	        return ((FactorialFactorInfo)(((DefaultMutableTreeNode)(root.getChildAt(constantParameterCount+columnIndex))).getUserObject())).getClass();
        }

		public int getColumnCount() {
	        return root.getChildCount() - constantParameterCount + 1;
        }

		public String getColumnName(int columnIndex) {
			if(columnIndex == 0) return "Run #";
			columnIndex--;
	        return ((FactorialFactorInfo)(((DefaultMutableTreeNode)(root.getChildAt(constantParameterCount+columnIndex))).getUserObject())).getName();
        }

		public int getRowCount() {
	        return ((FactorialFactorInfo)(((DefaultMutableTreeNode)(root.getChildAt(constantParameterCount))).getUserObject())).getValues().size();
        }

		public Object getValueAt(int rowIndex, int columnIndex) {
			if(columnIndex == 0) return rowIndex + 1;
			columnIndex--;
			if(((FactorialFactorInfo)(((DefaultMutableTreeNode)(root.getChildAt(constantParameterCount+columnIndex))).getUserObject())).getValues().get(rowIndex) != null)
			if(!showValues){
				if(((FactorialFactorInfo)(((DefaultMutableTreeNode)(root.getChildAt(constantParameterCount+columnIndex))).getUserObject())).getValues().get(rowIndex).equals(((FactorialFactorInfo)(((DefaultMutableTreeNode)(root.getChildAt(constantParameterCount+columnIndex))).getUserObject())).lowValue)) return "-";
				else if(((FactorialFactorInfo)(((DefaultMutableTreeNode)(root.getChildAt(constantParameterCount+columnIndex))).getUserObject())).getValues().get(rowIndex).equals(((FactorialFactorInfo)(((DefaultMutableTreeNode)(root.getChildAt(constantParameterCount+columnIndex))).getUserObject())).highValue)) return "+";
				else return "0";
			}else{
				return ((FactorialFactorInfo)(((DefaultMutableTreeNode)(root.getChildAt(constantParameterCount+columnIndex))).getUserObject())).getValues().get(rowIndex);
			} else return showValues ? "NULL" : "?";
        }

		public boolean isCellEditable(int rowIndex, int columnIndex) {
	        return false;
        }

		public void removeTableModelListener(TableModelListener l) {
        }

		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        }
		
	}
	
	@SuppressWarnings("unused")
    private class ParameterInfoComparator implements Comparator<ParameterInfo>{
		public int compare(ParameterInfo a, ParameterInfo b) {
	        if(a.getDefinitionType() == ParameterInfo.CONST_DEF &&
	        		b.getDefinitionType() != ParameterInfo.CONST_DEF){
	        	return -1;
	        } else if(a.getDefinitionType() != ParameterInfo.CONST_DEF &&
	        		b.getDefinitionType() == ParameterInfo.CONST_DEF){
	        	return 1;
	        } else return 0;
        }
	}

	public void load(IIntelliContext ctx, final Element element) throws WizardLoadingException{
		context = ctx;
		makeSettingsPanel();
		NodeList nl = null;
		nl = element.getElementsByTagName(PARAMETERS);
		if (nl != null && nl.getLength() > 0){
			Element parametersElement = (Element)nl.item(0);
			NodeList paramNodes = parametersElement.getChildNodes();
			for (int i = 0; i < paramNodes.getLength(); i++) {
				Element paramNode = (Element)paramNodes.item(i);
				String paramName = paramNode.getAttribute(NAME);
				String paramDefValue = paramNode.getAttribute(DEFAULT_VALUE);
				String paramLowValue = paramNode.getAttribute(LOW);
				String paramHighValue = paramNode.getAttribute(HIGH);
				int j = 0;
				while(j < parametersList.getModel().getSize() && !((FactorialParameterInfo)(parametersList.getModel().getElementAt(j))).getName().equals(paramName)) j++;
				FactorialParameterInfo foundParamInfo = (FactorialParameterInfo)(parametersList.getModel().getElementAt(j));
				foundParamInfo.setValue(MemberInfo.getValue(paramDefValue, foundParamInfo.getType()));
				foundParamInfo.setLowValue(MemberInfo.getValue(paramLowValue, foundParamInfo.getType()));
				foundParamInfo.setHighValue(MemberInfo.getValue(paramHighValue, foundParamInfo.getType()));
			}
		} else{
		}
		nl = null;
		nl = element.getElementsByTagName(FACTORS);
		Vector<Integer> parametersToMove = new Vector<Integer>();
		if (nl != null && nl.getLength() > 0){
			Element factorsElement = (Element)nl.item(0);
			NodeList factorNodes = factorsElement.getChildNodes();
			for (int i = 0; i < factorNodes.getLength(); i++) {
				Element factorNode = (Element)factorNodes.item(i);
				String factorName = factorNode.getAttribute(NAME);
				String factorDefValue = factorNode.getAttribute(DEFAULT_VALUE);
				String factorLowValue = factorNode.getAttribute(LOW);
				String factorHighValue = factorNode.getAttribute(HIGH);
				int j = 0;
				while(j < parametersList.getModel().getSize() && !((FactorialParameterInfo)(parametersList.getModel().getElementAt(j))).getName().equals(factorName)) j++;
				FactorialParameterInfo foundParamInfo = (FactorialParameterInfo)(parametersList.getModel().getElementAt(j));
				foundParamInfo.setValue(MemberInfo.getValue(factorDefValue, foundParamInfo.getType()));
				foundParamInfo.setLowValue(MemberInfo.getValue(factorLowValue, foundParamInfo.getType()));
				foundParamInfo.setHighValue(MemberInfo.getValue(factorHighValue, foundParamInfo.getType()));
				parametersToMove.add(j);
			}
		} else{
		}
		int[] indices = new int[parametersToMove.size()];
		for (int i = 0; i < parametersToMove.size(); i++) {
			indices[i] = parametersToMove.get(i);
		}
		parametersList.setSelectedIndices(indices);
		moveSelectedParametersToFactors();
		//fractional:
		nl = null;
		nl = element.getElementsByTagName(FRACTIONAL);
		int k = factorsList.getModel().getSize();
		int p = 0;
		if (nl != null && nl.getLength() > 0){
			Element fractionalElement = (Element)nl.item(0);
			if(!fractionalElement.getAttribute(FRACTIONAL_P).equals("0")){
				p = Integer.parseInt(fractionalElement.getAttribute(FRACTIONAL_P));
				NodeList generatorNodes = fractionalElement.getChildNodes();
				Vector<String> generatorVector = new Vector<String>(k);
				Vector<String> factorVector = new Vector<String>(k);
				Vector<String> nameVector = new Vector<String>(k);
				for (int i = 0; i < generatorNodes.getLength(); i++) {
	                generatorVector.add(((Element)generatorNodes.item(i)).getAttribute(PRODUCT));
	                factorVector.add(((Element)generatorNodes.item(i)).getAttribute(FACTOR));
	                nameVector.add(((Element)generatorNodes.item(i)).getAttribute(NAME));
                }
				fractionalHelper.setActualDesign(new FactorialDesign(k-p,generatorVector.subList(k-p, k)));
				//getting the factors in order in the fractional design:
				//We will iterate through the list of the desired factor-order and
				//search for the actual factor name in the actual list, and move it up
				//to the right place.
				for (int i = 0; i < factorVector.size(); i++) {
	                String desiredFactorName = factorVector.get(i);
	                //a fractionalHelper-rel �gyeskedj�k ki a dolgot!
	                if(!fractionalHelper.getFactorAt(i).getName().equals(desiredFactorName)){
	                	int howManyMoveUps = 1;
	                	for(	int j = i+1; 
	                			j < factorVector.size() && 
	                				!fractionalHelper.getFactorAt(j).getName().equals(desiredFactorName); 
	                			j++) howManyMoveUps++;
	                	for(int j = i + howManyMoveUps; j > i; j--){
	                		fractionalHelper.factorMoveUp(j);
	                	}
	                }
                }
				fractionalCheckBox.setSelected(true);
				((SpinnerNumberModel)pSpinner.getModel()).setMaximum(p);
				((SpinnerNumberModel)pSpinner.getModel()).setValue(p);
				setFractionalPanelEnabled(true);
				updateFractionalPanel();
			}
		}
		nl = null;
		nl = element.getElementsByTagName(CENTERPOINT);
		if (nl != null && nl.getLength() > 0){
			centerPointCheckBox.setSelected(true);
			Element centerPointElement = (Element) nl.item(0);
			if(!centerPointElement.getAttribute(CENTERPOINT_COUNT).equals("")){
				try {
					int cpCount = Integer.parseInt(centerPointElement.getAttribute(CENTERPOINT_COUNT));
					((SpinnerNumberModel) centerPointSpinner.getModel()).setValue(cpCount);
				} catch (NumberFormatException e) {
					((SpinnerNumberModel) centerPointSpinner.getModel()).setValue(1);
				}
			} else {
				((SpinnerNumberModel) centerPointSpinner.getModel()).setValue(1);
			}
		}
		nl = null;
		nl = element.getElementsByTagName(NORMALIZE);
		normalizeParameterSpaceCheckBox.setSelected(false);
		linearModelCoefficientsProcessingRadio.setSelected(true);
		if (nl != null && nl.getLength() > 0){
			normalizeParameterSpaceCheckBox.setSelected(true);
			realFactorEffectsProcessingRadio.setSelected(true);
		}
		
		nl = null;
		nl = element.getElementsByTagName(RngSeedManipulatorModel.RSM_ELEMENT_NAME);
		if (nl != null && nl.getLength() > 0){
			Element rsmElement = (Element) nl.item(0);
			rngSeedManipulator.load(rsmElement);
		}

		Vector<ParameterInfo> newParamList = new Vector<ParameterInfo>();
		for (int i = 0; i < parametersList.getModel().getSize(); i++) {
	        newParamList.add((ParameterInfo) parametersList.getModel().getElementAt(i));
        }
		Vector<ParameterInfo> newFactorList = new Vector<ParameterInfo>();
		for (int i = 0; i < factorsList.getModel().getSize(); i++) {
			newFactorList.add((ParameterInfo) factorsList.getModel().getElementAt(i));
        }
		rngSeedManipulator.updatePossibleRandomSeedParameters(newParamList, newFactorList);
    }

	public void save(Node node) {
		Document doc = node.getOwnerDocument();
		Element rsmElement = doc.createElement(RngSeedManipulatorModel.RSM_ELEMENT_NAME);
		rngSeedManipulator.save(rsmElement);
		node.appendChild(rsmElement);
		Element parametersElement = doc.createElement(PARAMETERS);
		node.appendChild(parametersElement);
		for (int i = 0; i < parametersList.getModel().getSize(); i++) {
	        FactorialParameterInfo paramInfo = (FactorialParameterInfo) parametersList.getModel().getElementAt(i);
	        Element parameterElement = doc.createElement(PARAMETER);
	        parameterElement.setAttribute(NAME, paramInfo.getName());
	        parameterElement.setAttribute(DEFAULT_VALUE, paramInfo.getValue() == null ? "null" : paramInfo.getValue().toString());
	        parameterElement.setAttribute(LOW, paramInfo.getLowValue() == null ? "null" : paramInfo.getLowValue().toString());
	        parameterElement.setAttribute(HIGH, paramInfo.getHighValue() == null ? "null" : paramInfo.getHighValue().toString());
	        parametersElement.appendChild(parameterElement);
		}
		Element factorsElement = doc.createElement(FACTORS);
		node.appendChild(factorsElement);
		for (int i = 0; i < factorsList.getModel().getSize(); i++) {
	        FactorialFactorInfo factorInfo = (FactorialFactorInfo) factorsList.getModel().getElementAt(i);
	        Element factorElement = doc.createElement(FACTOR);
	        factorElement.setAttribute(NAME, factorInfo.getName());
	        factorElement.setAttribute(DEFAULT_VALUE, factorInfo.getValue() == null ? "null" : factorInfo.getValue().toString());
	        factorElement.setAttribute(LOW, factorInfo.getLowValue() == null ? "null" : factorInfo.getLowValue().toString());
	        factorElement.setAttribute(HIGH, factorInfo.getHighValue() == null ? "null" : factorInfo.getHighValue().toString());
	        factorsElement.appendChild(factorElement);
	        
	        if (factorInfo.isNumeric()) {
	        	Element varyingParameter = doc.createElement(IIntelliMethodPlugin.VARYING_NUMERIC_PARAMETER);
	        	varyingParameter.setAttribute("name", factorInfo.getName());
	        	node.appendChild(varyingParameter);
	        }
		}
		Element fractionalElement = doc.createElement(FRACTIONAL);
		node.appendChild(fractionalElement);
		if(!fractionalCheckBox.isSelected()){
			fractionalElement.setAttribute(FRACTIONAL_P, "0");
		} else{
			fractionalElement.setAttribute(FRACTIONAL_P, new Integer(fractionalHelper.getP()).toString());
			int factorCount = factorsList.getModel().getSize();
			for (int i = 0; i < factorCount; i++) {
	            Element generatorElement = doc.createElement(GENERATOR);
	            String generator = fractionalHelper.getFactorNameConfounding(i);
	            String items[] = generator.split(":");
	            String genName = items[0].trim();
	            String genProductItems[] = items[1].split("=");
	            String genProduct = genProductItems[1].trim();
	            generatorElement.setAttribute(FACTOR, fractionalHelper.getFactorAt(i).getName());
	            generatorElement.setAttribute(NAME, genName);
	            generatorElement.setAttribute(PRODUCT, genProduct);
	            fractionalElement.appendChild(generatorElement);
            }
		}
		if(centerPointCheckBox.isSelected()){
			Element centerPointElement = doc.createElement(CENTERPOINT);
			centerPointElement.setAttribute(CENTERPOINT_COUNT, centerPointSpinner.getModel().getValue().toString());
			node.appendChild(centerPointElement);
		}
		if(realFactorEffectsProcessingRadio.isSelected()){
			Element normalizeElement = doc.createElement(NORMALIZE);
			node.appendChild(normalizeElement);
		}
	}

	public void invalidatePlugin() {
		content = null;
    }

	public String getDescription() {
	    return "The Factorial Design method. This screening method can examine " +
	    		"the selected parameters of the model on a user-selected range, " +
	    		"and calculate the effects of the chosen parameters (factors).\n " +
	    		"This method can be used to determine the important factors.\n " +
	    		"Full/fractional factorial designs are available.";
    }

	public int getMethodType() {
	    return STATIC_METHOD;
    }

	public void rngSeedsChanged() {
		calculateRunCount();
    }

	public boolean isImplemented() {
	    return true;
    }

	public void stateChanged(ChangeEvent e) {
		if (e.getSource().equals(centerPointSpinner)){
			calculateRunCount();
		}
	}

	public int getMaxBlockingVariables() {
		return 0;
	}
	
	public int getBlockingVariableValueCountsFor(int blockingVariableCount) {
		return 0;
	}

	public boolean isBlockingHelpSupported() {
		return false;
	}

	public List<Object> getBlockingVariableValues(RngSeedManipulatorModel.BlockingParameterInfo info) {
		return null;
	}
	public int getDesignSize() { 
		int factorCount = factorsList.getModel().getSize();
		int runCount = 0;
		int p = 0;
		if(fractionalCheckBox.isSelected()){
			p = ((SpinnerNumberModel) pSpinner.getModel()).getNumber().intValue();
		}
		runCount = (int) Math.round(Math.pow(2, factorCount-p));
		if(centerPointCheckBox.isSelected()) runCount += (Integer) centerPointSpinner.getValue();
		return runCount;
	}
	
	@Override
	public long getNumberOfRuns() {
		return getDesignSize() * rngSeedManipulator.getRunsMultiplierFactor();
	}

}
