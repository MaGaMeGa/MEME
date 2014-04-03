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

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ai.aitia.meme.gui.RngSeedManipulator;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.generator.RngSeedManipulatorModel;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.plugin.IIntelliStaticMethodPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

/**
 * @author Ferschl
 *
 */
@SuppressWarnings("serial")
@Deprecated
public class Static_Latin implements 	IIntelliStaticMethodPlugin, 
										ActionListener, 
										ChangeListener,
										ListSelectionListener,
										FocusListener, ILatinCubePluginInterface{
	//------------------
	//constants:
	//------------------
	public static final int MAX_LEVELS = 5;
	public static final int MIN_LEVELS = 3;
	public static final int DEFAULT_LEVELS = 4;
	public static final int MAX_NUISANCES = 4;
	public static final int MIN_NUISANCES = 2;
	public static final String DESIGN = "design";
	public static final String NUISANCE_COUNT = "nuisances";
	public static final String LEVEL_COUNT = "levels";
	
	//------------------
	//GUI members:
	//------------------
	private JPanel content = null;
	private JScrollPane factorsScrollPane = null;
	private JList factorsList = null;
	private JScrollPane nuisanceScrollPane = null;
	private JList nuisanceList = null;
	private JTextField factorValueField = null;
	private JButton factorValueButton = null;
	private JTextField primaryFactorField = null;
	private JSpinner factorLevelsSpinner = null;
	private JButton toNuisanceButton = null;
	private JButton fromNuisanceButton = null;
	private JButton toPrimaryButton = null;
//	private JTextArea factorLevelsArea = null;
	private JLabel factorLevelsLabel = null;
	private JPanel factorLevelsPanel = null;
	private Vector<JTextField> levels = null;
	private JButton setLevelsButton = null;
	private JLabel totalRunsLabel = null;
	private JButton newParametersButton = null;
	//------------------
	//:
	//------------------
	private IIntelliContext context = null;
	private Vector<LatinFactorInfo> factors = null;
	private Vector<LatinFactorInfo> nuisances = null;
	private LatinFactorInfo primary = null;
	private LatinFactorInfo selectedForLevelsChange = null;
	private String readyStatusDetail = null;
	private RngSeedManipulator rngSeedManipulator = null;
	

//	--------------------------------------------------------------------------
//	Interface implementations:
//	--------------------------------------------------------------------------

	public JPanel getSettingsPanel(IIntelliContext ctx) {
		context = ctx;
		rngSeedManipulator = new RngSeedManipulator(ctx.getParameters(), this);
		if(content == null){
			factorsList = new JList();
			factorsScrollPane = new JScrollPane(factorsList);
			factorsScrollPane.setPreferredSize(new Dimension(250,250));
			nuisanceList = new JList();
			nuisanceScrollPane = new JScrollPane(nuisanceList);
			nuisanceScrollPane.setPreferredSize(new Dimension(300,250));
			factorValueField = new JTextField(10);
			factorValueButton = new JButton("Set");
			toNuisanceButton = new JButton("->");
			fromNuisanceButton = new JButton("<-");
			toPrimaryButton = new JButton("Select primary");
			primaryFactorField = new JTextField(25);
			primaryFactorField.addFocusListener(this);
			primaryFactorField.setEditable(false);
			SpinnerNumberModel spm = new SpinnerNumberModel(DEFAULT_LEVELS,MIN_LEVELS,MAX_LEVELS,1);
			spm.addChangeListener(this);
			factorLevelsSpinner = new JSpinner(spm);
//			factorLevelsArea = new JTextArea();
			factorLevelsLabel = new JLabel("Levels of selected factor:");
			factorLevelsPanel = new JPanel(new GridLayout(0,5));
			levels = new Vector<JTextField>(MAX_LEVELS);
			LevelChangeHandler lch = new LevelChangeHandler();
			for (int i = 0; i < MAX_LEVELS; i++) {
			    JTextField levelField = new JTextField(10);
			    levels.add(levelField);
			    factorLevelsPanel.add(levelField);
			    levelField.setActionCommand(String.valueOf(i));
			    levelField.addActionListener(lch);
			}
			setLevelsButton = new JButton("Set levels");
			totalRunsLabel = new JLabel("Total runs: ");
			newParametersButton = new JButton("Add new parameters...");
			content = FormsUtils.build("f:p:g p f:p:g p",
					"0_1_ p||" +
					"2344 p||" +
					"2544 p||" +
					"2644 p||" +
					"2744 p||" +
					"2844 p||" +
					"2_44 f:p:g||" +
					"9___ p||" +
					"A___ p||" +
					"B_CD p||" +
					"EEEE p||" +
					"FFFF p||" +
					"G___ p||" +
					"HHHH p||" + 
					"IIII p",
					"Factors:", "Nuisance factors: ("+MIN_NUISANCES+"-"+MAX_NUISANCES+")",
					factorsScrollPane, "Default value:", nuisanceScrollPane,
					factorValueField, 
					factorValueButton,
					toNuisanceButton,
					fromNuisanceButton,
					toPrimaryButton,
					"Primary factor:",
					primaryFactorField, "Factor levels ("+MIN_LEVELS+"-"+MAX_LEVELS+"):", factorLevelsSpinner,
					factorLevelsLabel,
					factorLevelsPanel,
					FormsUtils.buttonStack(setLevelsButton).getPanel(),
					rngSeedManipulator.getSeedsListPanel(),
					totalRunsLabel
					).getPanel();
			initialize();
		}
		return content;
	}

	public boolean getReadyStatus() {
		if(getFactorLevelsCount() == 3 && getNuisanceNumber() == 4){
			readyStatusDetail = "There is no design with 4 nuisance factors and 3 levels.";
			return false;
		}else if(primary == null){
			readyStatusDetail = "No primary factor selected.";
			return false;
		} else if(nuisances.size()<MIN_NUISANCES || nuisances.size()>MAX_NUISANCES){
			readyStatusDetail = "The number of nuisance factors must be between"+MIN_NUISANCES+"  and "+MAX_NUISANCES+".";
			return false;
		} else{
			for (int i = 0; i < getFactorLevelsCount(); i++) {
	            if(!LatinFactorInfo.isValid(primary.getLevels().get(i).toString(), primary.getType())){
	    			readyStatusDetail = "The primary factor("+primary.getName()+") has got one or more invalid level values.";
	    			return false;
	            }
            }
		}
		for (int i = 0; i < nuisances.size(); i++) {
	        for (int j = 0; j < getFactorLevelsCount(); j++) {
	            if(!LatinFactorInfo.isValid(nuisances.get(i).getLevels().get(j).toString(), nuisances.get(i).getType())){
	    			readyStatusDetail = "The nuisance factor("+nuisances.get(i).getName()+") has got one or more invalid level values.";
	    			return false;
	            }
            }
        }
		readyStatusDetail = "Ready.";
		return true;
	}
	
	public String getReadyStatusDetail(){
		getReadyStatus();
		return readyStatusDetail;
	}

	public void invalidatePlugin() {
		context = null;
		content = null;
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
	
	private DefaultMutableTreeNode getAlteredParameterTreeRootNode(){
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Parameter file");
		for (int i = 0; i < factors.size(); i++) {
	        factors.get(i).setDefinitionType(ParameterInfo.CONST_DEF);
	        factors.get(i).setRuns(1);
	        root.add(new DefaultMutableTreeNode(factors.get(i)));
        }
		primary.fillInLatinSquareValues(true, -1);
		root.add(new DefaultMutableTreeNode(primary));
		for (int i = 0; i < nuisances.size(); i++) {
	        nuisances.get(i).fillInLatinSquareValues(false, i);
	        root.add(new DefaultMutableTreeNode(nuisances.get(i)));
        }
		rngSeedManipulator.getModel().randomizeParameterTree(root);
		return root;
	}

	public void load(IIntelliContext context, Element pluginElem)
	throws WizardLoadingException {
		invalidatePlugin();
		getSettingsPanel(context); //this creates a new, clean gui and model
		NodeList nl = null;
		nl = pluginElem.getElementsByTagName(DESIGN);
		if (nl != null && nl.getLength() > 0){
			Element designElem = (Element) nl.item(0);
			int factorLevelAttr = Integer.parseInt(designElem.getAttribute(LEVEL_COUNT));
			factorLevelsSpinner.setValue(new Integer(factorLevelAttr));
			int nuisanceSizeAttr = Integer.parseInt(designElem.getAttribute(NUISANCE_COUNT));
			for (int i = 0; i < nuisanceSizeAttr; i++) {
	            nuisances.add(new LatinFactorInfo("blah", "bloh", null, this)); //these will be disposed soon
            }
			nl = null;
			nl = designElem.getElementsByTagName(LatinFactorInfo.LFI_ELEMENT);
			if (nl != null && nl.getLength() > 0){
				for (int i = 0; i < nl.getLength(); i++) {
					Element lfiElem = (Element) nl.item(i);
					int idx = LatinFactorInfo.findInfo(factors, lfiElem.getAttribute(LatinFactorInfo.NAME));
					factors.get(idx).load(lfiElem);
					if(Boolean.parseBoolean(lfiElem.getAttribute(LatinFactorInfo.PRIMARY))){
						if(idx > -1){
							primary = factors.get(idx);
							factors.remove(idx);
						}
					} else if(Boolean.parseBoolean(lfiElem.getAttribute(LatinFactorInfo.NUISANCE))){
						int nuisIdx = Integer.parseInt(lfiElem.getAttribute(LatinFactorInfo.NUISANCE_INDEX));
						nuisances.set(nuisIdx, factors.get(idx));
						factors.remove(idx);
					}
				}
			}
		}
		updateFactorDisplays();
		updateLevelsPanel();
	}
	

	public void save(Node node) {
		Document doc = node.getOwnerDocument();
		Element designElem = doc.createElement(DESIGN);
		node.appendChild(designElem);
		designElem.appendChild(primary.getLatinFactorInfoElement(true, -1, doc));
		for (int i = 0; i < factors.size(); i++) {
			designElem.appendChild(factors.get(i).getLatinFactorInfoElement(false, -1, doc));
		}
		for (int i = 0; i < nuisances.size(); i++) {
	        designElem.appendChild(nuisances.get(i).getLatinFactorInfoElement(false, i, doc));
        }
		designElem.setAttribute(LEVEL_COUNT, String.valueOf(getFactorLevelsCount()));
		designElem.setAttribute(NUISANCE_COUNT, String.valueOf(getNuisanceNumber()));
	}

	public String getLocalizedName() {
		return "Latin Square";
	}

	private void initialize() {
    	//init the buttons:
    	toPrimaryButton.setActionCommand("TO_PRIMARY");
    	toNuisanceButton.setActionCommand("TO_NUISANCE");
    	fromNuisanceButton.setActionCommand("FROM_NUISANCE");
    	factorValueButton.setActionCommand("FACTOR_VALUE");
    	setLevelsButton.setActionCommand("SET_LEVELS");
    	newParametersButton.setActionCommand("NEW_PARAMETERS");
    	GUIUtils.addActionListener(this, toPrimaryButton, toNuisanceButton, 
    			fromNuisanceButton, factorValueButton, newParametersButton, 
    			setLevelsButton);
    	//init the lists:
    	initFactors();
    	updateFactorDisplays();
    	updateLevelsPanel();
    	updateTotalRuns();
    }

	protected void initFactors(){
    	factors = new Vector<LatinFactorInfo>();
    	nuisances = new Vector<LatinFactorInfo>();
    	List<ParameterInfo> parList = context.getParameters();
    	for (ParameterInfo par : parList) {
    		LatinFactorInfo latFac = new LatinFactorInfo(par.getName(), par.getType(), par.getJavaType(),this);
    		latFac.setValues(new Vector<Object>(par.getValues()));
    		latFac.setSelectedForInspection(false);
    		factors.add(latFac);
    	}
    }

	protected void validateFactors(){
    	if(factors == null){
    		initFactors();
    	}else{
    		Vector<ParameterInfo> parList = new Vector<ParameterInfo>(context.getParameters());
    		//factors:
    		for (Iterator iter1 = factors.iterator(); iter1.hasNext();) {
                LatinFactorInfo latFac = (LatinFactorInfo) iter1.next();
                String actualName = latFac.getName();
                boolean found = false;
                for (Iterator iter2 = parList.iterator(); iter2.hasNext() && !found;) {
                    ParameterInfo par = (ParameterInfo) iter2.next();
                    if(par.getName().equals(actualName)){
                    	found = true;
                    	iter2.remove();
                    }
                }
                if(!found){
                	iter1.remove();
                }
            }
    		//nuisances:
    		for (Iterator iter1 = nuisances.iterator(); iter1.hasNext();) {
                LatinFactorInfo latFac = (LatinFactorInfo) iter1.next();
                String actualName = latFac.getName();
                boolean found = false;
                for (Iterator iter2 = parList.iterator(); iter2.hasNext() && !found;) {
                    ParameterInfo par = (ParameterInfo) iter2.next();
                    if(par.getName().equals(actualName)){
                    	found = true;
                    	iter2.remove();
                    }
                }
                if(!found){
                	iter1.remove();
                }
            }
    		//primary:
    		if(primary != null){
    			String actualName = primary.getName();
                boolean found = false;
                for (Iterator iter2 = parList.iterator(); iter2.hasNext() && !found;) {
                    ParameterInfo par = (ParameterInfo) iter2.next();
                    if(par.getName().equals(actualName)){
                    	found = true;
                    	iter2.remove();
                    }
                }
                if(!found) primary = null;
    		}
    		//adding the rest of parList to factors:
    		for (ParameterInfo par : parList) {
    			LatinFactorInfo latFac = new LatinFactorInfo(par.getName(), par.getType(), par.getJavaType(), this);
    			latFac.setValues(new Vector<Object>(par.getValues()));
    			latFac.setSelectedForInspection(false);
    			factors.add(latFac);
            }
    	}
    }

	protected void updateFactorDisplays(){
    	factorsList = new JList(factors);
    	factorsScrollPane.setViewportView(factorsList);
    	nuisanceList = new JList(nuisances);
    	nuisanceScrollPane.setViewportView(nuisanceList);
    	primaryFactorField.setText(primary != null ? primary.toString() : "");
    	factorsList.addListSelectionListener(this);
    	nuisanceList.addListSelectionListener(this);
    }

	protected void updateLevelsPanel(){
		int value = getFactorLevelsCount();
		for (int i = 0; i < value; i++) {
			levels.get(i).setEnabled(true);
		}
		for (int i = value; i < MAX_LEVELS; i++) {
			levels.get(i).setEnabled(false);
		}
	}
	
	//getters, setters:
	public int getFactorLevelsCount(){ return (Integer) factorLevelsSpinner.getValue(); }
	public int getNuisanceNumber(){ return nuisances.size(); }
	public int getMaxFactorLevelsCount() { return MAX_LEVELS; }
	
	//listener interface method implementations:
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if(command.equals("TO_PRIMARY")){
			int idx = factorsList.getSelectedIndex();
			if(idx != -1){
				LatinFactorInfo oldPrimary = primary;
				if(primary != null) {
					factors.add(oldPrimary);
					oldPrimary.setSelectedForInspection(false);
				}
				primary = factors.get(idx);
				primary.setSelectedForInspection(true);
				factors.remove(idx);
				updateFactorDisplays();
			}
		}else if(command.equals("TO_NUISANCE")){
			int[] idxs = factorsList.getSelectedIndices();
			if(idxs != null && idxs.length > 0){
				int howManyMoved = 0;
				for (int i = 0; i < idxs.length && nuisances.size() < MAX_NUISANCES; i++) {
					factors.get(idxs[i]).setSelectedForInspection(true);
					nuisances.add(factors.get(idxs[i]));
					howManyMoved++;
                }
				for (int i = howManyMoved - 1; i >= 0; i--) {
	                factors.remove(idxs[i]);
                }
				updateFactorDisplays();
			}
		}else if(command.equals("FROM_NUISANCE")){
			int[] idxs = nuisanceList.getSelectedIndices();
			if(idxs != null && idxs.length > 0){
				for (int i = 0; i < idxs.length; i++) {
					nuisances.get(idxs[i]).setSelectedForInspection(false);
					factors.add(nuisances.get(idxs[i]));
                }
				for (int i = idxs.length - 1; i >= 0; i--) {
	                nuisances.remove(idxs[i]);
                }
				updateFactorDisplays();
			}
		}else if(command.equals("FACTOR_VALUE")){
			int whichFactor = factorsList.getSelectedIndex();
			if(whichFactor > -1){
				String text = factorValueField.getText();
				if(LatinFactorInfo.isValid(text, factors.get(whichFactor).getType())){
					factors.get(whichFactor).setValue(LatinFactorInfo.getValue(text, factors.get(whichFactor).getType()));
					updateFactorDisplays();
				} else{
					Utilities.userAlert(ParameterSweepWizard.getFrame(), "The value entered ("+text+") is not compatible with the type ("+factors.get(whichFactor).getType()+").");
				}
			}
		}else if(command.equals("NEW_PARAMETERS")){
			context.getNewParametersButton().doClick();
			validateFactors();
			updateFactorDisplays();
		}else if(command.equals("SET_LEVELS")){
			for (int i = 0; i < getFactorLevelsCount(); i++) {
				String text = levels.get(i).getText();
				if(LatinFactorInfo.isValid(text, selectedForLevelsChange.getType())){
					selectedForLevelsChange.getLevels().set(i, LatinFactorInfo.getValue(text, selectedForLevelsChange.getType()));
					updateLevelsPanel();
					updateFactorDisplays();
				}else{
					Utilities.userAlert(ParameterSweepWizard.getFrame(), "The value entered ("+text+") is not compatible with the type ("+selectedForLevelsChange.getType()+").");
				}

            }
		}
    }

	public void stateChanged(ChangeEvent e) {
		if(e.getSource().equals(factorLevelsSpinner.getModel())){
			updateLevelsPanel();
			updateFactorDisplays();
			updateTotalRuns();
		}
    }
	public void updateTotalRuns() {
		int runCount = rngSeedManipulator.getRunsMultiplierFactor()*getFactorLevelsCount()*getFactorLevelsCount();
		totalRunsLabel.setText("Total runs: "+runCount);
	}
	
	public void valueChanged(ListSelectionEvent e) {
		if(e.getSource().equals(factorsList)){
			int idx = factorsList.getSelectedIndex();
			if(idx>-1){
				nuisanceList.setSelectedIndices(new int[] {});
				Object value = factors.get(idx).getValue();
				if(value != null){
					factorValueField.setText(value.toString());
				} else{
					factorValueField.setText("");
				}
			}
		}else if(e.getSource().equals(nuisanceList)){
			int idx = nuisanceList.getSelectedIndex();
			if(idx>-1){
				factorsList.setSelectedIndices(new int[] {});
				displayFactorLevels(nuisances.get(idx));
				selectedForLevelsChange = nuisances.get(idx);
			}
		}
    }
	
	public void displayFactorLevels(LatinFactorInfo lfi){
		int levelCount = getFactorLevelsCount();
		Vector<Object> values = lfi.getLevels();
		for (JTextField levelField : levels) {
            levelField.setText("");
        }
		for (int i = 0; i < levelCount && i < values.size(); i++) {
            if(values.get(i) != null){
            	levels.get(i).setText(values.get(i).toString());
            }
        }
		if(lfi.equals(primary)){
			factorLevelsLabel.setText("Levels of "+lfi.getName()+" PRIMARY factor:");
		}else{
			factorLevelsLabel.setText("Levels of "+lfi.getName()+" factor:");
		}
	}
	
	private class LevelChangeHandler implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			int whichLevel = Integer.parseInt(e.getActionCommand());
			String text = levels.get(whichLevel).getText();
			if(selectedForLevelsChange != null){
				if(LatinFactorInfo.isValid(text, selectedForLevelsChange.getType())){
					selectedForLevelsChange.getLevels().set(whichLevel, LatinFactorInfo.getValue(text, selectedForLevelsChange.getType()));
					updateLevelsPanel();
					updateFactorDisplays();
					if(whichLevel < getFactorLevelsCount()-1){
						levels.get(whichLevel+1).requestFocusInWindow();
						levels.get(whichLevel+1).setSelectionStart(0);
						levels.get(whichLevel+1).setSelectionEnd(levels.get(whichLevel+1).getText().length());
					}
				}else{
					Utilities.userAlert(ParameterSweepWizard.getFrame(), "The value entered ("+text+") is not compatible with the type ("+selectedForLevelsChange.getType()+").");
				}
			}
        }
	}

	public void focusGained(FocusEvent e) {
		if(primary != null){
			selectedForLevelsChange = primary;
			displayFactorLevels(primary);
			primaryFactorField.setSelectionStart(0);
			primaryFactorField.setSelectionEnd(primaryFactorField.getText().length());
			nuisanceList.setSelectedIndices(new int[] {});
			factorsList.setSelectedIndices(new int[] {});
		}
    }

	public void focusLost(FocusEvent e) {
		//nothing to do
    }

	public String getDescription() {
	    return "This is the old Latin Cube Design plugin. Obsolete.";
    }

	public int getMethodType() {
	    return STATIC_METHOD;
    }

	public boolean isImplemented() {
	    return false;
    }

	public int getBlockingVariableValueCountsFor(int blockingVariableCount) {
		return 0;
	}

	public int getMaxBlockingVariables() {
		return 0;
	}

	public boolean isBlockingHelpSupported() {
		return false;
	}

	public List<Object> getBlockingVariableValues(RngSeedManipulatorModel.BlockingParameterInfo info) {
		return null;
	}

	public int getDesignSize() {
		return getFactorLevelsCount()*getFactorLevelsCount();
	}

	@Override
	public long getNumberOfRuns() {
		return getDesignSize() * rngSeedManipulator.getRunsMultiplierFactor();
	}

}
