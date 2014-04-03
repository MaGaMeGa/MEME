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

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.AbstractThreeLevelMethod;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.utils.FormsUtils;

/**
 * @author Ferschl
 * 
 */
@SuppressWarnings("serial")
public class Static_CentralComposite extends AbstractThreeLevelMethod {

	protected JRadioButton cccRadio = null;
	protected JRadioButton cciRadio = null;
	protected JRadioButton ccfRadio = null;
	protected static final String CC_TYPE_ELEM = "CC_TYPE";
	protected static final String CC_TYPE_CCC = "CCC";
	protected static final String CC_TYPE_CCI = "CCI";
	protected static final String CC_TYPE_CCF = "CCF";
	
	public Static_CentralComposite(){
		super();
		this.PARAMETERS_ELEM = "CC_PARAMETERS";
		this.INFO_ELEM = "CentralCompositeInfo";
	}

	public String getDescription() {
		return "A Box-Wilson Central Composite Design, commonly called 'a central "
				+ "composite design,' contains an embedded factorial or fractional "
				+ "factorial design with center points that is augmented with a "
				+ "group of `star points' that allow estimation of curvature.\n"
				+ "This design is useful for response surface methodology (RSM).\n\n"
				+ "NIST/SEMATECH e-Handbook of Statistical Methods, http://www.itl.nist.gov/div898/handbook/, 2006\n\n";
	}

	@Override
	protected void createThisDesignInStandardMethodSpecificPart() {
		int size = designInfos.size();
		int factorialSize = size;
		int actualRunIndex = 0;
		FactorialDesign fd = new FactorialDesign(factorialSize);
		double alpha = Math.pow(2.0, (1.0 * factorialSize) / 4.0);
		for (actualRunIndex = 0; actualRunIndex < fd.getColumn(0).size(); actualRunIndex++) {
			for (int column = 0; column < factorialSize; column++) {
				Object toSet = null;
				toSet = designInfos.get(column).getLow();
				if (cciRadio.isSelected()) {
					toSet = designInfos.get(column).getScaledLow(1 / alpha);
				}
				if (fd.getColumn(column).get(actualRunIndex) == 1) {
					toSet = designInfos.get(column).getHigh();
					if (cciRadio.isSelected()) {
						toSet = designInfos.get(column).getScaledHigh(1 / alpha);
					}
				}
				standardPlugin.designTableModel.setValueInDesignAt(toSet, actualRunIndex,
						column);
			}
		}
		actualRunIndex = fd.getColumn(0).size();
		// fill this part with the center values:
		for (int i = 0; i < 2 * factorialSize; i++) {
			for (int j = 0; j < factorialSize; j++) {
				Object toSet = designInfos.get(j).getCenter();
				standardPlugin.designTableModel.setValueInDesignAt(toSet, actualRunIndex
						+ i, j);
			}
		}
		for (int i = 0; i < 2 * factorialSize; i++) {
			Object toSet = designInfos.get(i/2).getLow();
			if (cccRadio.isSelected()) {
				toSet = designInfos.get(i/2).getScaledLow(alpha);
			}
			if (i % 2 == 1) {
				toSet = designInfos.get(i/2).getHigh();
				if (cccRadio.isSelected()) {
					toSet = designInfos.get(i/2).getScaledHigh(alpha);
				}
			}
			standardPlugin.designTableModel.setValueInDesignAt(toSet, actualRunIndex + i, i/2);
		}
		//Centerpoint run:
		actualRunIndex += 2 * factorialSize;
		for (int i = 0; i < size; i++) {
			standardPlugin.designTableModel.setValueInDesignAt(designInfos.get(i).getCenter(), actualRunIndex, i);
		}
	}

	public int getDesignSize() {
		int designSize = 1;
		if (designInfos.size() > 0) {
			for (int i = 0; i < designInfos.size(); i++) {
				designSize *= 2;
			}
		}
		designSize += 2 * designInfos.size();
		designSize++; // centerpoint
		return designSize;
	}

	@Override
	public long getNumberOfRuns() {
		return getDesignSize() * rngSeedManipulator.getRunsMultiplierFactor();
	}

	@Override
	protected String getLocalizedNameOfThis() {
		return "Central composite";
	}

	@Override
	public String getMethodDisplayName() {
		return "Central composite";
	}

	public boolean isImplemented() {
		return true;
	}

	@Override
	protected JPanel getExtraDesignPanel() {
		if (cccRadio == null) {
			cccRadio = new JRadioButton("Circumscribed", false);
			cciRadio = new JRadioButton("Inscribed", false);
			ccfRadio = new JRadioButton("Face centered", false);
			ButtonGroup ccGroup = new ButtonGroup();
			ccGroup.add(cccRadio);
			ccGroup.add(cciRadio);
			ccGroup.add(ccfRadio);
			cccRadio.setSelected(true);
			cccRadio.setToolTipText("Circumscribed (to be filled)");
			cciRadio.setToolTipText("Inscribed (to be filled)");
			ccfRadio.setToolTipText("Face centered (to be filled)");

			JPanel extraPanel = FormsUtils.build("~ p p p p ~", "0123 p|",
					"Select Design Type:", cccRadio, cciRadio, ccfRadio).getPanel();

			extraDesignPanel.add(extraPanel, BorderLayout.CENTER);
		}
		return extraDesignPanel;
	}

	@Override
	public void load(IIntelliContext context, Element pluginElem) throws WizardLoadingException {
		super.load(context, pluginElem);
		NodeList nl = null;
		nl = pluginElem.getElementsByTagName(CC_TYPE_ELEM);
		if(nl != null && nl.getLength() > 0){
			Element ccType = (Element) nl.item(0);
			String type = ccType.getAttribute(CC_TYPE_ELEM);
			if(type.equals(CC_TYPE_CCC)) cccRadio.setSelected(true);
			else if(type.equals(CC_TYPE_CCI)) cciRadio.setSelected(true);
			else if(type.equals(CC_TYPE_CCF)) ccfRadio.setSelected(true);
		}
	}

	@Override
	public void save(Node node) {
		super.save(node);
		Element ccTypeElem = node.getOwnerDocument().createElement(CC_TYPE_ELEM);
		if(cccRadio.isSelected()) 
			ccTypeElem.setAttribute(CC_TYPE_ELEM, CC_TYPE_CCC);
		if(cciRadio.isSelected()) 
			ccTypeElem.setAttribute(CC_TYPE_ELEM, CC_TYPE_CCI);
		if(ccfRadio.isSelected()) 
			ccTypeElem.setAttribute(CC_TYPE_ELEM, CC_TYPE_CCF);
		node.appendChild(ccTypeElem);
	}

	@Override
	public boolean getReadyStatus() {
		boolean ret = super.getReadyStatus();
		if(ret){
			if(!ccfRadio.isSelected()){
				boolean stringParameterPresent = false;
				for (int i = 0; i < designInfos.size(); i++) {
					if(designInfos.get(i).getType().equals("String"))
						stringParameterPresent = true;
				}
				if(stringParameterPresent){
					ret = false;
					readyStatusDetail = "There is at least one String type " +
							"parameter in the desing parameters list. Your " +
							"only option for design type is Face centered " +
							"in this case.";
				}
			}
		}
		return ret;
	}
}
