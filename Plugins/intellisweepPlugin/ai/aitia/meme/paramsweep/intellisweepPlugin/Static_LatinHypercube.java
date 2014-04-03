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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
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

import ai.aitia.meme.gui.IRngSeedManipulatorChangeListener;
import ai.aitia.meme.gui.RngSeedManipulator;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.generator.RngSeedManipulatorModel;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.plugin.IIntelliMethodPlugin;
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
public class Static_LatinHypercube implements IIntelliStaticMethodPlugin, ChangeListener, ActionListener, ListSelectionListener, ILatinCubePluginInterface, IRngSeedManipulatorChangeListener, MouseListener, ItemListener{
	private static final String SET_LEVELS = "SET_LEVELS";
	private static final String FACTOR_VALUE = "FACTOR_VALUE";
	private static final String FROM_NUISANCE = "FROM_NUISANCE";
	private static final String TO_NUISANCE = "TO_NUISANCE";
	private static final String NEW_PARAMETERS = "NEW_PARAMETERS";
	//------------------
	//constants:
	//------------------
	public static final int MAX_LEVELS = 100;
	public static final int MIN_LEVELS = 2;
	public static final int DEFAULT_LEVELS = 10;
	public static final int MAX_NUISANCES = 10;
	public static final int MIN_NUISANCES = 2;
	public static final String DESIGN = "design";
	public static final String NUISANCE_COUNT = "nuisances";
	public static final String LEVEL_COUNT = "levels";
	public static final String PRECISION_BOOSTER = "precision_booster";
	public static final String PRECISION_BOOSTER_FACTOR = "boost_factor";
	private static final int MAX_ABSOLUTE_INITIAL_BOOST_FACTOR = 100;
	
	public final static String LHD_RESOURCES = "intelliSweepLHDesigns";
	
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
	private JSpinner factorLevelsSpinner = null;
	private JButton toNuisanceButton = null;
	private JButton fromNuisanceButton = null;
	private JLabel factorLevelsLabel = null;
	private JPanel factorLevelsPanel = null;
	private Vector<JTextField> levels = null;
	private JButton setLevelsButton = null;
	private JLabel totalRunsLabel = null;
	private JButton newParametersButton = null;
	private JSlider boostPrecisionSlider = null;
	private JCheckBox boostPrecisionCheckBox = null;
	private JTextField boostPrecisionField = null;
	private JLabel boostMaxLabel = null;
	//------------------
	//:
	//------------------
	private IIntelliContext context = null;
	private Vector<LatinFactorInfo> factors = null;
	private Vector<LatinFactorInfo> nuisances = null;
	private LatinFactorInfo selectedForLevelsChange = null;
	private String readyStatusDetail = null;
	private int previousFactorLevelCount = 0;
	private RngSeedManipulator rngSeedManipulator = null;
	private int minBoostFactor = 1;
	private int maxBoostFactor = 1;
	private int maxPossibleBoostFactor = 1;
	private int boostFactor = 1;
	//------------------
	//constants:
	//------------------

	//--------------------------
	//interface implementations:	
	//--------------------------
	public boolean alterParameterTree(IIntelliContext ctx) {
		DefaultMutableTreeNode root = ctx.getParameterTreeRootNode();
		DefaultMutableTreeNode newRoot = getAlteredParameterTreeRootNode();
		if(root != null){
			root.removeAllChildren();
			int count = newRoot.getChildCount();
			for (int i = 0; i < count; i++) {
				root.add((DefaultMutableTreeNode) newRoot.getChildAt(0));
			}
			return true;
		} else return false;
	}
	
	private DefaultMutableTreeNode getAlteredParameterTreeRootNode(){
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Parameter file");
		for (int i = 0; i < factors.size(); i++) {
	        factors.get(i).setDefinitionType(ParameterInfo.CONST_DEF);
	        factors.get(i).setRuns(1);
	        root.add(new DefaultMutableTreeNode(factors.get(i)));
        }
		File resourcesDir = new File(context.getPluginResourcesDirectory(), LHD_RESOURCES);
        String points = String.valueOf(getFactorLevelsCount());
        if(points.length() == 1) points = "00" + points;
        if(points.length() == 2) points = "0" + points;
		File lhdFile = new File(resourcesDir, "mml2lhd"+getNuisanceNumber()+"d"+points+".lhd");
		String[] designColumns = new String[nuisances.size()];
		try {
	        BufferedReader br = new BufferedReader(new FileReader(lhdFile));
	        for (int i = 0; i < nuisances.size(); i++) {
	        	String line = br.readLine();
	        	designColumns[i] = line.replace(';', ' ');
	        	nuisances.get(i).setValues(nuisances.get(i).createValuesFromIndexTextOffset0(designColumns[i]));
	        	nuisances.get(i).setDefinitionType(LatinFactorInfo.LIST_DEF);
	        	nuisances.get(i).setRuns(1);
	        	root.add(new DefaultMutableTreeNode(nuisances.get(i)));
	        }
        } catch (FileNotFoundException e) {
        	root = null;
	        e.printStackTrace();
        } catch (IOException e) {
        	root = null;
	        e.printStackTrace();
        }
        if(boostPrecisionCheckBox.isSelected()){
        	Vector<Vector<Integer>> permutations = new Vector<Vector<Integer>>(boostFactor);
        	int[] toPermute = new int[nuisances.size()];
        	for (int i = 0; i < nuisances.size(); i++) {
	            toPermute[i] = i;
            }
        	//the first permutations are the rotations of the array:
        	rotateRight(toPermute);
        	for (int i = 0; i < toPermute.length-1; i++) {
        		Vector<Integer> toAdd = new Vector<Integer>(nuisances.size());
        		for (int j = 0; j < toPermute.length; j++) {
	                toAdd.add(toPermute[j]);
                }
        		permutations.add(toAdd);
        		rotateRight(toPermute);
            }
        	//we got the original 0,1,2,... permutation back after the rotations
        	if(boostFactor > permutations.size()){
        		int factorial = factorial(nuisances.size());
        		int modulens = factorial / boostFactor;
        		int permNumber = 1;
        		Permutation.nextPerm(toPermute); //we skip the already present permutation
        		while(permutations.size() + 1 < boostFactor && permNumber < factorial){
        			if(permNumber % modulens == 0){
                		Vector<Integer> toAdd = new Vector<Integer>(nuisances.size());
                		for (int j = 0; j < toPermute.length; j++) {
        	                toAdd.add(toPermute[j]);
                        }
        				boolean alreadyInTheRotationPart = false;
        				for (int i = 0; i < nuisances.size() - 1; i++) {
	                        if(toAdd.equals(permutations.get(i))){
	                        	alreadyInTheRotationPart = true;
	                        }
                        }
        				if(alreadyInTheRotationPart){
        					Permutation.nextPerm(toPermute);
        					permNumber++;
                    		toAdd = new Vector<Integer>(nuisances.size());
                    		for (int j = 0; j < toPermute.length; j++) {
            	                toAdd.add(toPermute[j]);
                            }
        				}
        				permutations.add(toAdd);
        			}
        			permNumber++;
        			Permutation.nextPerm(toPermute);
        		}
        		for (int i = 0; i < permutations.size(); i++) {
	                applyPermutation(permutations.get(i), designColumns);
                }
        	}
        }
        rngSeedManipulator.getModel().randomizeParameterTree(root);
		return root;
	}
	protected void applyPermutation(Vector<Integer> perm, String[] designColumns) {
		for (int i = 0; i < nuisances.size(); i++) {
	        nuisances.get(i).appendValues(nuisances.get(i).createValuesFromIndexTextOffset0(designColumns[perm.get(i)]));
        }
    }

	private void rotateRight(int[] toRot){
		int last = toRot[toRot.length-1];
		for (int i = toRot.length-1; i > 0; i--) {
	        toRot[i] = toRot[i-1];
        }
		toRot[0] = last;
	}

	public JPanel getSettingsPanel(IIntelliContext ctx) {
		context = ctx;
		if(content == null){
			rngSeedManipulator = new RngSeedManipulator(context.getParameters(), this);
			rngSeedManipulator.addChangeListener(this);
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
			SpinnerNumberModel spm = new SpinnerNumberModel(DEFAULT_LEVELS,MIN_LEVELS,MAX_LEVELS,1);
			previousFactorLevelCount = DEFAULT_LEVELS;
			spm.addChangeListener(this);
			factorLevelsSpinner = new JSpinner(spm);
			boostPrecisionCheckBox = new JCheckBox("Boost result precision:");
			boostPrecisionCheckBox.addItemListener(this);
			boostPrecisionSlider = new JSlider(JSlider.HORIZONTAL, 2, MAX_ABSOLUTE_INITIAL_BOOST_FACTOR, 2);
//			boostPrecisionSlider.setLabelTable(boostPrecisionSlider.createStandardLabels(10, 10));
//			boostPrecisionSlider.setPaintLabels(true);
//			boostPrecisionSlider.setPaintTicks(true);
			boostPrecisionSlider.addChangeListener(this);
			JPanel boostPanel = new JPanel(new BorderLayout());
			JPanel boostPanel2 = new JPanel(new FlowLayout());
			boostPrecisionField = new JTextField(8);
			boostPrecisionField.setActionCommand("BOOST_FIELD");
			boostPrecisionField.addActionListener(this);
			boostMaxLabel = new JLabel("(max )");
			boostPanel.add(boostPrecisionCheckBox, BorderLayout.WEST);
			boostPanel.add(boostPrecisionSlider, BorderLayout.CENTER);
			boostPanel2.add(boostPrecisionField);
			boostPanel2.add(boostMaxLabel);
			boostPanel.add(boostPanel2, BorderLayout.EAST);
			boostPrecisionSlider.setEnabled(false);
			boostPrecisionField.setEnabled(false);
			
//			factorLevelsArea = new JTextArea();
			factorLevelsLabel = new JLabel("Levels of selected factor:");
			factorLevelsPanel = new JPanel(new GridLayout(0,5));
			levels = new Vector<JTextField>(MAX_LEVELS);
			LevelChangeHandler lch = new LevelChangeHandler();
			for (int i = 0; i < 2; i++) {
				JTextField levelField = new JTextField(10);
	            levels.add(levelField);
	            levelField.addActionListener(lch);
            }
			factorLevelsPanel.add(new JLabel("Low value:"));
			factorLevelsPanel.add(levels.get(0));
			levels.get(0).setActionCommand("LOW_VALUE");
			factorLevelsPanel.add(new JLabel("High value"));
			factorLevelsPanel.add(levels.get(1));
			levels.get(1).setActionCommand("HIGH_VALUE");
			setLevelsButton = new JButton("Set levels");
			enableDisableFactorLevelPanel(false);
			totalRunsLabel = new JLabel("Total runs: XX");
			updateRunCount();
			newParametersButton = new JButton("Add new parameters...");
			content = FormsUtils.build("f:p:g p f:p:g p",
					"0_1_ p||" +
					"2344 p||" +
					"2544 p||" +
					"2644 p||" +
					"2744 p||" +
					"2844 p||" +
					"2_44 f:p:g||" +
					"9_AB p||" +
					"CCCC p||" +
					"DDDD p||" +
					"E___ p||" +
					"FFFF p||" +
					"GGGG p||" +
					"HHHH p",
					"Factors:", "Factors to study: ("+MIN_NUISANCES+"-"+MAX_NUISANCES+")",
					factorsScrollPane, "Default value:", nuisanceScrollPane,
					factorValueField, 
					factorValueButton,
					toNuisanceButton,
					fromNuisanceButton,
					newParametersButton, "Factor levels ("+MIN_LEVELS+"-"+MAX_LEVELS+"):", factorLevelsSpinner,
					factorLevelsLabel,
					factorLevelsPanel,
					FormsUtils.buttonStack(setLevelsButton).getPanel(),
					boostPanel,
					rngSeedManipulator.getSeedsListPanel(),
					totalRunsLabel
					).getPanel();
			initialize();
		}
		return content;
	}

	public boolean getReadyStatus() {
		if(nuisances.size()<MIN_NUISANCES || nuisances.size()>MAX_NUISANCES){
			readyStatusDetail = "The number of nuisance factors must be between"+MIN_NUISANCES+"  and "+MAX_NUISANCES+".";
			return false;
		}
		for (int i = 0; i < nuisances.size(); i++) {
	        for (int j = 0; j < getFactorLevelsCount(); j++) {
	            if(!LatinFactorInfo.isValid(nuisances.get(i).getLevels().get(j).toString(), nuisances.get(i).getType())){
	    			readyStatusDetail = "The nuisance factor("+nuisances.get(i).getName()+") has got one or more invalid level values.";
	    			return false;
	            }
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

	public void invalidatePlugin() {
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
					if(Boolean.parseBoolean(lfiElem.getAttribute(LatinFactorInfo.NUISANCE))){
						int nuisIdx = Integer.parseInt(lfiElem.getAttribute(LatinFactorInfo.NUISANCE_INDEX));
						nuisances.set(nuisIdx, factors.get(idx));
						factors.remove(idx);
					}
				}
			}
		}
		updateFactorDisplays();
		updateRunCount();
		nl = null;
		nl = pluginElem.getElementsByTagName(PRECISION_BOOSTER);
		if (nl != null && nl.getLength() > 0){
			Element boostElem = (Element) nl.item(0);
			boostPrecisionCheckBox.setSelected(boostElem.getAttribute("enabled").equals("true"));
			boostPrecisionField.setText(boostElem.getAttribute(PRECISION_BOOSTER_FACTOR));
			boostPrecisionField.postActionEvent();
		}
		nl = null;
		nl = pluginElem.getElementsByTagName(RngSeedManipulatorModel.RSM_ELEMENT_NAME);
		if (nl != null && nl.getLength() > 0){
			Element rsmElement = (Element) nl.item(0);
			rngSeedManipulator.load(rsmElement);
		}
		rngSeedManipulator.updatePossibleRandomSeedParameters(factors, nuisances);
		updateFactorDisplays();
		updateRunCount();
	}

	public void save(Node node) {
		Document doc = node.getOwnerDocument();
		Element rsmElement = doc.createElement(RngSeedManipulatorModel.RSM_ELEMENT_NAME);
		rngSeedManipulator.save(rsmElement);
		node.appendChild(rsmElement);
		Element boostElem = doc.createElement(PRECISION_BOOSTER);
		boostElem.setAttribute(PRECISION_BOOSTER_FACTOR, ""+boostFactor);
		boostElem.setAttribute("enabled", boostPrecisionCheckBox.isSelected() ? "true":"false");
		node.appendChild(boostElem);
		Element designElem = doc.createElement(DESIGN);
		node.appendChild(designElem);
		for (int i = 0; i < factors.size(); i++) {
			designElem.appendChild(factors.get(i).getLatinFactorInfoElement(false, -1, doc));
		}
		for (int i = 0; i < nuisances.size(); i++) {
	        designElem.appendChild(nuisances.get(i).getLatinFactorInfoElement(false, i, doc));
	        
	        if (nuisances.get(i).isNumeric()) {
	        	Element varyingParameter = doc.createElement(IIntelliMethodPlugin.VARYING_NUMERIC_PARAMETER);
	        	varyingParameter.setAttribute("name", nuisances.get(i).getName());
	        	node.appendChild(varyingParameter);
	        }
        }
		designElem.setAttribute(LEVEL_COUNT, String.valueOf(getFactorLevelsCount()));
		designElem.setAttribute(NUISANCE_COUNT, String.valueOf(getNuisanceNumber()));
	}

	public String getLocalizedName() {
		return "Latin Hypercube Design";
	}


	//--------------------------
	//member methods:	
	//--------------------------
	private void initialize() {
    	//init the buttons:
    	toNuisanceButton.setActionCommand(TO_NUISANCE);
    	fromNuisanceButton.setActionCommand(FROM_NUISANCE);
    	factorValueButton.setActionCommand(FACTOR_VALUE);
    	setLevelsButton.setActionCommand(SET_LEVELS);
    	newParametersButton.setActionCommand(NEW_PARAMETERS);
    	GUIUtils.addActionListener(this, toNuisanceButton, 
    			fromNuisanceButton, factorValueButton, newParametersButton, 
    			setLevelsButton);
    	nuisanceList.addMouseListener(this);
    	//init the lists:
    	initFactors();
    	updateFactorDisplays();
    }

	/**
	 * Sets the levels of the selected factor (if there is one), according to
	 * the low and high interval bounds, creating equidistant partitions.
	 */
	protected void setSelectedFactorLevels() {
		setFactorLevels(selectedForLevelsChange);
	}
	
	protected void setFactorLevels(LatinFactorInfo info){
		if(info != null){
			String lowValue = levels.get(0).getText().trim();
			String highValue = levels.get(1).getText().trim();
			setFactorLevels(info, lowValue, highValue);
		}
	}
	
	@SuppressWarnings("cast")
	protected void setFactorLevels(LatinFactorInfo info, String lowValue, String highValue){
		if(LatinFactorInfo.isValid(lowValue, info.getType())){
			if(LatinFactorInfo.isValid(highValue, info.getType())){
				Number actualLevelValue = null;
				for (int i = 0; i < getFactorLevelsCount(); i++) {
					if ("byte".equals(info.getType()) || "Byte".equals(info.getType()))
						actualLevelValue = new Byte(	(byte)((1.0*i*((new Byte(highValue.toString())) - 
								(new Byte(lowValue.toString()))) / (getFactorLevelsCount() - 1)) + (new Byte(lowValue.toString()))));
					else if ("short".equals(info.getType()) || "Short".equals(info.getType())) 
						actualLevelValue = new Short(	(short)((1.0*i*((new Short(highValue.toString())) - 
								(new Short(lowValue.toString()))) / (getFactorLevelsCount() - 1)) + (new Short(lowValue.toString()))));
					else if ("int".equals(info.getType()) || "Integer".equals(info.getType()))
						actualLevelValue = new Integer(	(int)((1.0*i*((new Integer(highValue.toString())) - 
								(new Integer(lowValue.toString()))) / (getFactorLevelsCount() - 1)) + (new Integer(lowValue.toString()))));
					else if ("long".equals(info.getType()) || "Long".equals(info.getType()))
						actualLevelValue = new Long(	(long)((1.0*i*((new Long(highValue.toString())) - 
								(new Long(lowValue.toString()))) / (getFactorLevelsCount() - 1)) + (new Long(lowValue.toString()))));
					else if ("float".equals(info.getType()) || "Float".equals(info.getType()))
						actualLevelValue = new Float(	(float)((1.0*i*((new Float(highValue.toString())) - 
								(new Float(lowValue.toString()))) / (getFactorLevelsCount() - 1)) + (new Float(lowValue.toString()))));
					else if ("double".equals(info.getType()) || "Double".equals(info.getType()))
						actualLevelValue = new Double(	(double)((1.0*i*((new Double(highValue.toString())) - 
								(new Double(lowValue.toString()))) / (getFactorLevelsCount() - 1)) + (new Double(lowValue.toString()))));
					//end of (if-else)s
					info.getLevels().set(i, actualLevelValue);
				}
			} else{
				Utilities.userAlert(ParameterSweepWizard.getFrame(), "The value entered ("+highValue+") is not compatible with the type ("+info.getType()+").");				}
		} else{
			Utilities.userAlert(ParameterSweepWizard.getFrame(), "The value entered ("+lowValue+") is not compatible with the type ("+info.getType()+").");			
		}
	}

	protected void initFactors(){
    	factors = new Vector<LatinFactorInfo>();
    	nuisances = new Vector<LatinFactorInfo>();
    	List<ParameterInfo> parList = context.getParameters();
    	for (ParameterInfo par : parList) {
    		LatinFactorInfo latFac = new LatinFactorInfo(par.getName(), par.getType(), par.getJavaType(), this);
    		latFac.setValues(new Vector<Object>(par.getValues()));
    		latFac.setSelectedForInspection(false);
    		factors.add(latFac);
    		if(par.getName().equals("RngSeed"))
    			rngSeedManipulator.addSeed(par);
    	}
    	rngSeedManipulator.updatePossibleRandomSeedParameters(factors, nuisances);
    }
	
	protected void updateFactorDisplays(){
    	factorsList = new JList(factors);
    	factorsScrollPane.setViewportView(factorsList);
    	nuisanceList = new JList(nuisances);
    	nuisanceScrollPane.setViewportView(nuisanceList);
    	factorsList.addListSelectionListener(this);
    	nuisanceList.addListSelectionListener(this);
    	minBoostFactor = nuisances.size() == 0 ? 1 : nuisances.size();
    	int n = factorial(nuisances.size());
    	if(n < 1) {
    		maxBoostFactor = minBoostFactor;
    		boostMaxLabel.setText("(min: "+minBoostFactor+", max: "+maxBoostFactor+")");
    	}
    	else { 
    		maxBoostFactor = n > MAX_ABSOLUTE_INITIAL_BOOST_FACTOR ? MAX_ABSOLUTE_INITIAL_BOOST_FACTOR : n;
    		maxPossibleBoostFactor = n;
    		boostMaxLabel.setText("(min:"+minBoostFactor+", max:"+maxPossibleBoostFactor+")");
    	}
    	int old = boostFactor;
    	boostPrecisionSlider.setMinimum(minBoostFactor);
    	boostPrecisionSlider.setMaximum(maxBoostFactor);
    	if(old > maxPossibleBoostFactor) {
    		boostPrecisionSlider.setValue(maxBoostFactor);
    		boostFactor = maxPossibleBoostFactor;
    		boostPrecisionField.setText(""+boostFactor);
    	}
    	else if(old < minBoostFactor) {
    		boostPrecisionSlider.setValue(minBoostFactor);
    		boostFactor = minBoostFactor;
    	}
    	else boostPrecisionSlider.setValue(old);
		if(boostFactor > maxBoostFactor) {
			boostPrecisionSlider.setValue(maxBoostFactor);
			boostPrecisionField.setText(""+boostFactor);
		}
		else {
			boostPrecisionSlider.setValue(boostFactor);
		}

	}
	private static int factorial(int n){
		if(n>MAX_NUISANCES || n > 12){
			return -1;
		}else{
			if(n > 2) return n*factorial(n-1);
			else return 2;
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
    		//adding the rest of parList to factors:
    		for (ParameterInfo par : parList) {
    			LatinFactorInfo latFac = new LatinFactorInfo(par.getName(), par.getType(), par.getJavaType(), this);
    			latFac.setValues(new Vector<Object>(par.getValues()));
    			latFac.setSelectedForInspection(false);
    			factors.add(latFac);
            }
    	}
    	rngSeedManipulator.updatePossibleRandomSeedParameters(context.getParameters(), nuisances);
    }
	
	public void displayFactorLevels(LatinFactorInfo lfi){
		int lastLevelIdx = getFactorLevelsCount()-1;
		Vector<Object> values = lfi.getLevels();
		for (JTextField levelField : levels) {
            levelField.setText("");
        }
		if(values.get(0) != null){
			levels.get(0).setText(values.get(0).toString());
		}
		if(values.get(lastLevelIdx) != null){
			levels.get(1).setText(values.get(lastLevelIdx).toString());
		}
		factorLevelsLabel.setText("Levels of "+lfi.getName()+" factor:");
	}
	
	public void updateFactorLevels(){
		for (LatinFactorInfo nuisInfo : nuisances) {
	        String highValue = nuisInfo.getLevels().get(previousFactorLevelCount-1).toString();
	        if(!nuisInfo.getLevels().get(0).toString().equals("") && !highValue.equals("")){
	        	setFactorLevels(nuisInfo, nuisInfo.getLevels().get(0).toString(), highValue);
	        }
        }
		for (LatinFactorInfo factorInfo : factors) {
	        String highValue = factorInfo.getLevels().get(previousFactorLevelCount-1).toString();
	        if(!factorInfo.getLevels().get(0).toString().equals("") && !highValue.equals("")){
	        	setFactorLevels(factorInfo, factorInfo.getLevels().get(0).toString(), highValue);
	        }
        }
	}
	
	public void enableDisableFactorLevelPanel(boolean b){
		for (JTextField levelTF : levels) {
	        levelTF.setEnabled(b);
        }
		setLevelsButton.setEnabled(b);
	}
	
	public void updateRunCount(){
		totalRunsLabel.setText("Total runs: " + (boostPrecisionCheckBox.isSelected() ? boostFactor : 1) * rngSeedManipulator.getRunsMultiplierFactor() * getFactorLevelsCount());
	}

	//-----------------
	//getters, setters:
	//-----------------
	public int getFactorLevelsCount(){ return (Integer) factorLevelsSpinner.getValue(); }
	public int getNuisanceNumber(){ return nuisances.size(); }
	public int getMaxFactorLevelsCount() { return MAX_LEVELS; }
	//--------------------------
	//listener implementations:	
	//--------------------------
	public void stateChanged(ChangeEvent e) {
		if(e.getSource().equals(factorLevelsSpinner.getModel())){
			updateFactorLevels();
			previousFactorLevelCount = getFactorLevelsCount();
			updateFactorDisplays();
			updateRunCount();
		} else if(e.getSource().equals(boostPrecisionSlider)){
			boostPrecisionField.setText(""+boostPrecisionSlider.getValue());
			boostFactor = boostPrecisionSlider.getValue();
			updateRunCount();
		}
    }
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if(command.equals(TO_NUISANCE)){
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
				rngSeedManipulator.updatePossibleRandomSeedParameters(factors, nuisances);
				updateFactorDisplays();
			}
		}else if(command.equals(FROM_NUISANCE)){
			int[] idxs = nuisanceList.getSelectedIndices();
			if(idxs != null && idxs.length > 0){
				for (int i = 0; i < idxs.length; i++) {
					nuisances.get(idxs[i]).setSelectedForInspection(false);
					factors.add(nuisances.get(idxs[i]));
                }
				for (int i = idxs.length - 1; i >= 0; i--) {
	                nuisances.remove(idxs[i]);
                }
				rngSeedManipulator.updatePossibleRandomSeedParameters(factors, nuisances);
				updateFactorDisplays();
			}
		}else if(command.equals(FACTOR_VALUE)){
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
		}else if(command.equals(NEW_PARAMETERS)){
			context.getNewParametersButton().doClick();
			validateFactors();
			updateFactorDisplays();
		}else if(command.equals(SET_LEVELS)){
			setSelectedFactorLevels();
//			for (int i = 0; i < getFactorLevelsCount(); i++) {
//				String text = levels.get(i).getText();
//				if(LatinFactorInfo.isValid(text, selectedForLevelsChange.getType())){
//					selectedForLevelsChange.getLevels().set(i, LatinFactorInfo.getValue(text, selectedForLevelsChange.getType()));
//					updateFactorDisplays();
//				}else{
//					Utilities.userAlert(ParameterSweepWizard.getFrame(), "The value entered ("+text+") is not compatible with the type ("+selectedForLevelsChange.getType()+").");
//				}
//
//            }
			updateFactorDisplays();
		} else if(command.equals("BOOST_FIELD")){
//			System.out.println("BOOST_FIELD, hajr�");//megy ez, j�l van :)
			int n = 1;
			try{
				n = Integer.parseInt(boostPrecisionField.getText().trim());
				if(n >= minBoostFactor && n <= maxPossibleBoostFactor) {
					boostFactor = n;
					if(boostFactor > maxBoostFactor) {
						boostPrecisionSlider.setValue(maxBoostFactor);
						boostPrecisionField.setText(""+boostFactor);
					}
					else {
						boostPrecisionSlider.setValue(boostFactor);
					}
				}else{
					Utilities.userAlert(ParameterSweepWizard.getFrame(), "The value entered must be between "+minBoostFactor+" and "+maxPossibleBoostFactor+"!");
				}
			} catch(NumberFormatException nfe){
				Utilities.userAlert(ParameterSweepWizard.getFrame(), "The value entered must be a valid integer!");
			}
			
		}
    }
	
	public void valueChanged(ListSelectionEvent e) {
		if(e.getSource().equals(factorsList)){
			int idx = factorsList.getSelectedIndex();
			if(idx>-1){
				nuisanceList.setSelectedIndices(new int[] {});
				enableDisableFactorLevelPanel(false);
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
				enableDisableFactorLevelPanel(true);
				displayFactorLevels(nuisances.get(idx));
				selectedForLevelsChange = nuisances.get(idx);
				levels.get(0).requestFocusInWindow();
				levels.get(0).setSelectionStart(0);
				levels.get(0).setSelectionEnd(levels.get(0).getText().length());
			}
		}
    }

	public void rngSeedsChanged() {
		updateRunCount();
    }

	//---------------
	//nested classes:
	//---------------
	private class LevelChangeHandler implements ActionListener{
		public void actionPerformed(ActionEvent e) {
			if(e.getActionCommand().equals("LOW_VALUE")){
				String text = levels.get(0).getText();
				if(selectedForLevelsChange != null){
					if(LatinFactorInfo.isValid(text, selectedForLevelsChange.getType())){
						if(LatinFactorInfo.isValid(levels.get(1).getText(), selectedForLevelsChange.getType())){
							setSelectedFactorLevels();
							updateFactorDisplays();
						}
						selectAndFocusTextField(levels.get(1));
					} else {
						Utilities.userAlert(ParameterSweepWizard.getFrame(), "The value entered ("+text+") is not compatible with the type ("+selectedForLevelsChange.getType()+").");
					}
				} else {
					Utilities.userAlert(ParameterSweepWizard.getFrame(), "No nuisance selected to edit!");
				}
			} else if(e.getActionCommand().equals("HIGH_VALUE")){
				String text = levels.get(0).getText();
				if(selectedForLevelsChange != null){
					if(!LatinFactorInfo.isValid(text, selectedForLevelsChange.getType())){
						Utilities.userAlert(ParameterSweepWizard.getFrame(), "The value entered ("+text+") for the low value is not compatible with the type ("+selectedForLevelsChange.getType()+").");
						selectAndFocusTextField(levels.get(0));
					} else {
						text = levels.get(1).getText();
						if(LatinFactorInfo.isValid(text, selectedForLevelsChange.getType())){
							setSelectedFactorLevels();
							updateFactorDisplays();
							selectNextNuisance();
							displayFactorLevels(selectedForLevelsChange);
							selectAndFocusTextField(levels.get(0));
						} else {
							Utilities.userAlert(ParameterSweepWizard.getFrame(), "The value entered ("+text+") is not compatible with the type ("+selectedForLevelsChange.getType()+").");
						}
					}
				} else {
					Utilities.userAlert(ParameterSweepWizard.getFrame(), "No nuisance selected to edit!");
				}
			}
        }

		/**
         * @param focusToWhich
         */
        private void selectAndFocusTextField(JTextField which) {
	        which.requestFocusInWindow();
	        which.setSelectionStart(0);
	        which.setSelectionEnd(which.getText().length());
        }

		/**
         * 
         */
        private void selectNextNuisance() {
	        int idx = LatinFactorInfo.findInfo(nuisances, selectedForLevelsChange.getName());
	        if(idx < nuisances.size()-1){
	        	selectedForLevelsChange = nuisances.get(idx+1);
	        	idx++;
	        } else{
	        	selectedForLevelsChange = nuisances.get(0);
	        	idx = 0;
	        }
	        nuisanceList.setSelectedIndex(idx);
        }
	}

	public void mouseClicked(MouseEvent e) {
		if(e.getButton() == MouseEvent.BUTTON2 && e.getSource() == nuisanceList){
//			int idx = nuisanceList.locationToIndex(e.getPoint());
//			System.out.println(" "+idx);
		}
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}

	public void itemStateChanged(ItemEvent e) {
		if(e.getItemSelectable() == boostPrecisionCheckBox){
			boolean to = false;
			if(boostPrecisionCheckBox.isSelected()){
				to = true;
			} else{
				to = false;
			}
			boostPrecisionSlider.setEnabled(to);
			boostPrecisionField.setEnabled(to);
		}
		updateRunCount();
    }

	public String getDescription() {
	    return "The Latin Hypercube Design method. This screening method " +
	    		"utilizes Latin Hypercube Sampling to eliminate the disturbing " +
	    		"confounding effects of various experimental factors. \n" +
	    		"This design uses every specified value of every selected " +
	    		"parameter no matter which value turns out to be more important. \n" +
	    		"The maximin desingns used by this method are obtained from \n " +
	    		"http://www.spacefillingdesigns.nl \n";
    }

	public int getMethodType() {
	    return STATIC_METHOD;
    }

	public boolean isImplemented() {
	    return true;
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
		return (boostPrecisionCheckBox.isSelected() ? boostFactor : 1) * getFactorLevelsCount();
	}

	@Override
	public long getNumberOfRuns() {
		return getDesignSize() * rngSeedManipulator.getRunsMultiplierFactor();
	}

}
