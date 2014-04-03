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
package ai.aitia.meme.paramsweep.intellisweepPlugin.utils;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ai.aitia.meme.gui.IRngSeedManipulatorChangeListener;
import ai.aitia.meme.gui.RngSeedManipulator;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.generator.RngSeedManipulatorModel;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.intellisweepPlugin.ParameterSelectionPanel;
import ai.aitia.meme.paramsweep.intellisweepPlugin.Static_StandardDOE;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.plugin.IIntelliMethodPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

public abstract class AbstractThreeLevelMethod extends AbstractDoeDisplayer implements
		IRngSeedManipulatorChangeListener {

	protected String PARAMETERS_ELEM = null;
	protected String INFO_ELEM = null;

	protected final class ThreeLevelDesignParamSelectPanel extends
			ParameterSelectionPanel {

		@SuppressWarnings("unchecked")
		@Override
		public Vector<ThreeLevelInfo> getConstantVector() {
			return constantInfos;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Vector<ThreeLevelInfo> getDesignVector() {
			return designInfos;
		}

		@Override
		protected void moveParametersFromDesign(int[] idxs) {
			for (int i = 0; i < idxs.length; i++) {
				getDesignVector().get(idxs[i])
						.setDefinitionType(ThreeLevelInfo.CONST_DEF);
				getConstantVector().add(getDesignVector().get(idxs[i]));
			}
			for (int i = idxs.length - 1; i >= 0; i--) {
				getDesignVector().remove(idxs[i]);
			}
			updateDesignPanel();
		}

		@Override
		protected void moveParametersToDesign(int[] idxs) {
			Vector<Integer> validMovesIdxs = new Vector<Integer>();
			for (int i = 0; i < idxs.length; i++) {
				if (!getConstantVector().get(idxs[i]).getType().equalsIgnoreCase(
						"boolean")) {
					validMovesIdxs.add(idxs[i]);
				}
			}
			for (int i = 0; i < validMovesIdxs.size(); i++) {
				getConstantVector().get(validMovesIdxs.get(i)).setDefinitionType(
						ThreeLevelInfo.LIST_DEF);
				getDesignVector().add(getConstantVector().get(validMovesIdxs.get(i)));
			}
			for (int i = validMovesIdxs.size() - 1; i >= 0; i--) {
				getConstantVector().remove(validMovesIdxs.get(i).intValue());
			}
			if (idxs.length != validMovesIdxs.size()) {
				Utilities.userAlert(ParameterSweepWizard.getFrame(),
						"You cannot put Boolean parameters to the design.");
			}
			updateDesignPanel();
		}

		@Override
		protected void newParametersPressed() {
			context.getNewParametersButton().doClick();
			validateLists();
			updateDesignPanel();
		}

	}

	protected class ThreeLevelInfo extends ParameterInfo {

		private static final long serialVersionUID = 1L;
		private static final String NAME_ATTR = "name";
		private static final String TYPE_ATTR = "type";
		private static final String DEFTYPE_ATTR = "defType";
		private static final String IN_DESIGN_ATTR = "inDesign";
		private static final String LOW_VALUE_ATTR = "lowValue";
		private static final String CENTER_VALUE_ATTR = "centerValue";
		private static final String HIGH_VALUE_ATTR = "highValue";
		private Object low = null;
		private Object center = null;
		private Object high = null;
		private boolean specifyCenter = false;

		public ThreeLevelInfo(ParameterInfo info) {
			super(info.getName(), info.getType(), info.getJavaType());
			this.low = info.getValue();
			this.setValue(info.getValue());
			this.defType = CONST_DEF;
			this.specifyCenter = getType().equals("String");
		}

		@Override
		public String toString() {
			StringBuffer sb = null;
			if (getDefinitionType() == CONST_DEF) {
				sb = new StringBuffer("[" + getName() + "(" + getType() + "): ");
				sb.append("default value = ");
				sb.append(getValue());
			} else if (getDefinitionType() == LIST_DEF) {
				return getName();
			}
			sb.append("]");
			return sb.toString();
		}

		public String toDetailedString() {
			StringBuffer sb = new StringBuffer("[" + getName() + "(" + getType() + "):");
			if (this.type.equals("String")) {
				sb.append(" low = "
						+ (low == null ? "null" : "\"" + low.toString() + "\""));
				sb.append(" center = "
						+ (center == null ? "null" : "\"" + center.toString() + "\""));
				sb.append(" high = "
						+ (high == null ? "null" : "\"" + high.toString() + "\""));
			} else {
				sb.append(" low = " + (low == null ? "null" : low.toString()));
				sb.append(" center = " + (center == null ? "null" : center.toString()));
				sb.append(" high = " + (high == null ? "null" : high.toString()));
			}
			sb.append("]");
			return sb.toString();
		}

		public Object getCenter() {
			return center;
		}

		public void setCenter(Object center) {
			this.center = center;
		}

		public Object getHigh() {
			return high;
		}

		public void setHigh(Object high) {
			this.high = high;
		}

		public Object getLow() {
			return low;
		}

		public void setLow(Object low) {
			this.low = low;
		}

		public boolean isSpecifyCenter() {
			return specifyCenter;
		}

		public void setSpecifyCenter(boolean specifyCenter) {
			this.specifyCenter = specifyCenter;
		}

		public void modifyThreeLevelInfo(String low2, String high2, boolean specCent,
				String center2) {
			this.setLow(ThreeLevelInfo.getValue(low2, this.type));
			this.setHigh(ThreeLevelInfo.getValue(high2, this.type));
			if (specCent) {
				this.setCenter(ThreeLevelInfo.getValue(center2, this.type));
			} else {
				calculateCenter();
			}
		}

		@SuppressWarnings("cast")
		public void calculateCenter() {
			if ("byte".equals(type) || "Byte".equals(type)) {
				Short s = new Short((short) (((Byte) low + (Byte) high) / 2));
				center = new Byte(s.byteValue());
			}
			if ("short".equals(type) || "Short".equals(type)) {
				Integer s = new Integer((int) (((Short) low + (Short) high) / 2));
				center = new Short(s.shortValue());
			}
			if ("int".equals(type) || "Integer".equals(type)) {
				Long s = new Long((long) (((Integer) low + (Integer) high) / 2));
				center = new Integer(s.intValue());
			}
			if ("long".equals(type) || "Long".equals(type)) {
				Long s = new Long((long) (((Long) low + (Long) high) / 2));
				center = s;
			}
			if ("float".equals(type) || "Float".equals(type)) {
				Float s = new Float((float) (((Float) low + (Float) high) / 2));
				center = s;
			}
			if ("double".equals(type) || "Double".equals(type)) {
				Double s = new Double((double) (((Double) low + (Double) high) / 2));
				center = s;
			}
		}
		
		public Object getScaledHigh(double scale){
			return getScaledValue(getType(), high, center, scale);
		}
		public Object getScaledLow(double scale){
			return getScaledValue(getType(), low, center, scale);
		}
		
		@SuppressWarnings("cast")
		protected Object getScaledValue(String type, Object value, Object nullPoint, double scale){
			Object ret = null;
			if ("byte".equals(type) || "Byte".equals(type)) {
				Short s = new Short((short) (scale * ((Byte) value - (Byte) nullPoint) + (Byte) nullPoint));
				ret = new Byte(s.byteValue());
			}
			if ("short".equals(type) || "Short".equals(type)) {
				Integer s = new Integer((int) (scale * ((Short) value - (Short) nullPoint) + (Short) nullPoint));
				ret = new Short(s.shortValue());
			}
			if ("int".equals(type) || "Integer".equals(type)) {
				Long s = new Long((long) (scale * ((Integer) value - (Integer) nullPoint) + (Integer) nullPoint));
				ret = new Integer(s.intValue());
			}
			if ("long".equals(type) || "Long".equals(type)) {
				Long s = new Long((long) (scale * ((Long) value - (Long) nullPoint) + (Long) nullPoint));
				ret = s;
			}
			if ("float".equals(type) || "Float".equals(type)) {
				Float s = new Float((float) (scale * ((Float) value - (Float) nullPoint) + (Float) nullPoint));
				ret = s;
			}
			if ("double".equals(type) || "Double".equals(type)) {
				Double s = new Double((double) (scale * ((Double) value - (Double) nullPoint) + (Double) nullPoint));
				ret = s;
			}
			return ret;
		}

		public Element getInfoElement(Document doc, boolean inDesign) {
			Element elem = doc.createElement(INFO_ELEM);
			elem.setAttribute(NAME_ATTR, this.name);
			elem.setAttribute(TYPE_ATTR, this.type);
			elem.setAttribute(DEFTYPE_ATTR, "" + this.defType);
			elem.setAttribute(IN_DESIGN_ATTR, inDesign ? "true" : "false");
			elem.setAttribute(LOW_VALUE_ATTR, "" + this.low);
			elem.setAttribute(CENTER_VALUE_ATTR, "" + this.center);
			elem.setAttribute(HIGH_VALUE_ATTR, "" + this.high);
			return elem;
		}

		public boolean load(Element elem) {
			boolean inDesign = false;
			this.name = elem.getAttribute(NAME_ATTR);
			this.type = elem.getAttribute(TYPE_ATTR);
			try {
				this.defType = Integer.parseInt(elem.getAttribute(DEFTYPE_ATTR));
			} catch (NumberFormatException e) {
				this.defType = CONST_DEF;
			}
			inDesign = Boolean.parseBoolean(elem.getAttribute(IN_DESIGN_ATTR));
			low = ThreeLevelInfo.getValue(elem.getAttribute(LOW_VALUE_ATTR), this.type);
			center = ThreeLevelInfo.getValue(elem.getAttribute(CENTER_VALUE_ATTR),
					this.type);
			high = ThreeLevelInfo.getValue(elem.getAttribute(HIGH_VALUE_ATTR), this.type);
			return inDesign;
		}
	}

	protected class ThreeLevelInfoListCellRenderer extends JLabel implements
			ListCellRenderer {
		private static final long serialVersionUID = -6164888992893678491L;

		public Component getListCellRendererComponent(JList list, Object cellObject,
				int idx, boolean isSelected, boolean cellHasFocus) {
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			this.setText(((ThreeLevelInfo) cellObject).toDetailedString());
			return this;
		}
	}

	protected class ThreeLevelDesignSettingPanel extends
			CustomThreeLevelDesignSettingPanel {

		public ThreeLevelDesignSettingPanel(Vector<? extends ParameterInfo> infos,
				ListCellRenderer cellRenderer, JPanel extraPanel) {
			super(infos, cellRenderer, extraPanel);
		}

		private static final long serialVersionUID = 1L;

		@Override
		public void controlFiller() {
			int selectedIndex = designList.getSelectedIndex();
			if (selectedIndex > -1)
				displayThreeLevelInfo(selectedIndex);
		}

		@Override
		public void infoFiller() {
			int selectedIndex = designList.getSelectedIndex();
			if (selectedIndex > -1) {
				modifyThreeLevelInfo(designInfos.get(selectedIndex));
			}
		}

		private void displayThreeLevelInfo(int selectedIndex) {
			ThreeLevelInfo info = designInfos.get(selectedIndex);
			if (info.getLow() != null)
				lowValueField.setText(info.getLow().toString());
			else
				lowValueField.setText("");
			if (info.getHigh() != null)
				highValueField.setText(info.getHigh().toString());
			else
				highValueField.setText("");
			if (info.getCenter() != null)
				optionalValueField.setText(info.getCenter().toString());
			else
				optionalValueField.setText("");
			if (info.getType().equalsIgnoreCase("String")) {
				info.setSpecifyCenter(true);
				optionalValueCheckBox.setEnabled(false);
			} else {
				optionalValueCheckBox.setEnabled(true);
			}
			optionalValueCheckBox.setSelected(info.isSpecifyCenter());
		}

		private void modifyThreeLevelInfo(ThreeLevelInfo info) {
			String low = lowValueField.getText();
			String high = highValueField.getText();
			String center = null;
			boolean specCent = optionalValueCheckBox.isSelected();
			if (specCent) {
				center = optionalValueField.getText();
			}
			if (!ThreeLevelInfo.isValid(low, info.getType())) {
				Utilities.userAlert(ParameterSweepWizard.getFrame(),
						"The low value is incompatible with type: " + info.getType());
			} else if (!ThreeLevelInfo.isValid(high, info.getType())) {
				Utilities.userAlert(ParameterSweepWizard.getFrame(),
						"The high value is incompatible with type: " + info.getType());
			} else if (specCent && !ThreeLevelInfo.isValid(center, info.getType())) {
				Utilities.userAlert(ParameterSweepWizard.getFrame(),
						"The center value is incompatible with type: " + info.getType());
			} else {
				info.modifyThreeLevelInfo(low, high, specCent, center);
				updateList(designInfos);
			}
			updateDesignPanel();
		}

	}

	protected ThreeLevelDesignParamSelectPanel paramSelectPanel = null;
	protected ThreeLevelDesignSettingPanel designSettingsPanel = null;
	protected JButton standardViewButton = new JButton("View/Edit design runs...");
	protected RngSeedManipulator rngSeedManipulator = null;
	protected JLabel runCountLabel = new JLabel("1 runs");
	protected JPanel extraDesignPanel = new JPanel(new BorderLayout());

	protected Vector<ThreeLevelInfo> constantInfos = new Vector<ThreeLevelInfo>();
	protected Vector<ThreeLevelInfo> designInfos = new Vector<ThreeLevelInfo>();
	protected ThreeLevelInfoListCellRenderer cellRenderer = new ThreeLevelInfoListCellRenderer();

	protected boolean inStandardPlugin = false;
	protected boolean ready;
	protected String readyStatusDetail;

	public void validateLists() {
		// validate lists (const, design)
		boolean[] found = new boolean[context.getParameters().size()];
		for (int i = 0; i < found.length; i++) {
			found[i] = false;
		}
		// remove the parameters not on the lists anymore:
		for (Iterator iter = constantInfos.iterator(); iter.hasNext();) {
			ThreeLevelInfo info = (ThreeLevelInfo) iter.next();
			boolean foundInfo = false;
			for (int i = 0; i < found.length && !foundInfo; i++) {
				if (context.getParameters().get(i).getName().equals(info.getName())) {
					found[i] = true;
					foundInfo = true;
				}
			}
			if (!foundInfo) {
				iter.remove();
			}
		}
		for (Iterator iter = designInfos.iterator(); iter.hasNext();) {
			ThreeLevelInfo info = (ThreeLevelInfo) iter.next();
			boolean foundInfo = false;
			for (int i = 0; i < found.length && !foundInfo; i++) {
				if (context.getParameters().get(i).getName().equals(info.getName())) {
					found[i] = true;
					foundInfo = true;
				}
			}
			if (!foundInfo) {
				iter.remove();
			}
		}
		// adding the parameters not found on the lists:
		for (int i = 0; i < found.length; i++) {
			if (!found[i]) {
				constantInfos.add(new ThreeLevelInfo(context.getParameters().get(i)));
			}
		}
	}

	public void updateDesignPanel() {
		designSettingsPanel.updateList(designInfos);
		rngSeedManipulator.updatePossibleRandomSeedParameters(constantInfos, designInfos);
		runCountLabel.setText(""
				+ (rngSeedManipulator.getRunsMultiplierFactor() * getDesignSize())
				+ " runs");
//		if (getReadyStatus()) {
//			standardViewButton.setEnabled(true);
//		} else {
//			standardViewButton.setEnabled(false);
//		}
	}

	public JPanel getSettingsPanel(IIntelliContext ctx) {
		if (content == null) {
			context = ctx;
			standardPlugin = new Static_StandardDOE();
			standardPlugin.getSettingsPanel(ctx);
			standardPlugin.clearDesign();
			initDesignFromContext(ctx);
			designSettingsPanel = new ThreeLevelDesignSettingPanel(designInfos,
					cellRenderer, getExtraDesignPanel());
			paramSelectPanel = new ThreeLevelDesignParamSelectPanel();
			paramSelectPanel.getPanel().setBorder(
					BorderFactory.createTitledBorder("Parameter Selection"));
			rngSeedManipulator = new RngSeedManipulator(ctx.getParameters(), this);
			rngSeedManipulator.addChangeListener(this);
			thisContent = FormsUtils.build(
					"~ f:p:g ~ p ~",
					"00 p||" + 
					"11 f:p:g||" + 
					"22 p||" + 
					"33 p||" + 
					"4_ p||",
					paramSelectPanel.getPanel(), designSettingsPanel,
					rngSeedManipulator.getSeedsListPanel(), runCountLabel,
					FormsUtils.buttonStack(standardViewButton).getPanel()).getPanel();
			updateDesignPanel();
			initListeners();
			content = new JPanel(new BorderLayout());
			content.add(thisContent, BorderLayout.CENTER);
		}
		return content;
	}

	protected void initListeners() {
		standardViewButton.setActionCommand(SWITCH_TO_STANDARD);
		GUIUtils.addActionListener(this, standardViewButton);
	}

	protected void initDesignFromContext(IIntelliContext ctx) {
		for (int i = 0; i < ctx.getParameters().size(); i++) {
			constantInfos.add(new ThreeLevelInfo(ctx.getParameters().get(i)));
		}
	}

	public boolean getReadyStatus() {
		if (inStandardPlugin) {
			return standardPlugin.getReadyStatus();
		}
		readyStatusDetail = "Ready";
		ready = designInfos.size() > 1;
		if (!ready)
			readyStatusDetail = "You must have at least two parameters in the design.";
		for (int i = 0; ready && i < constantInfos.size(); i++) {
			if (constantInfos.get(i).getValue() == null) {
				ready = false;
				readyStatusDetail = "One or more constant parameters have null value.";
			}
		}
		for (int i = 0; ready && i < designInfos.size(); i++) {
			if (designInfos.get(i).getLow() == null
					|| designInfos.get(i).getCenter() == null
					|| designInfos.get(i).getHigh() == null) {
				ready = false;
				readyStatusDetail = "One or more design parameters have null value.";
			}
		}
		if (!rngSeedManipulator.isNaturalVariationConsistent()) {
			readyStatusDetail = "You selected Variation analysis, but do not have at least one random seed\nwith Sweep setting, with more than one value.";
			ready = false;
		}
		if (!rngSeedManipulator.isBlockingConsistent()) {
			readyStatusDetail = "You selected Blocking design, but do not have at least one random seed\nwith Sweep setting, with more than one value.";
			ready = false;
		}
		return ready;
	}

	public String getReadyStatusDetail() {
		if (inStandardPlugin) {
			return standardPlugin.getReadyStatusDetail();
		}
		return readyStatusDetail;
	}

	public void invalidatePlugin() {
		content = null;
	}

	public void load(IIntelliContext context, Element pluginElem)
			throws WizardLoadingException {
		invalidatePlugin();
		getSettingsPanel(context); // this creates a new, clean gui and model
		constantInfos.clear();
		designInfos.clear();
		NodeList nl = null;
		nl = pluginElem.getElementsByTagName(PARAMETERS_ELEM);
		if (nl != null && nl.getLength() > 0) {
			Element parametersElem = (Element) nl.item(0);
			List<ParameterInfo> paramList = context.getParameters();
			nl = null;
			nl = parametersElem.getElementsByTagName(INFO_ELEM);
			for (int i = 0; i < nl.getLength(); i++) {
				Element infoElem = (Element) nl.item(i);
				String name = infoElem.getAttribute(ThreeLevelInfo.NAME_ATTR);
				for (int j = 0; j < paramList.size(); j++) {
					if (paramList.get(j).getName().equals(name)) {
						ThreeLevelInfo info = new ThreeLevelInfo(paramList.get(j));
						boolean inDesign = info.load(infoElem);
						if (inDesign)
							designInfos.add(info);
						else
							constantInfos.add(info);
					}
				}
			}
		}
		nl = null;
		nl = pluginElem.getElementsByTagName(RngSeedManipulatorModel.RSM_ELEMENT_NAME);
		if (nl != null && nl.getLength() > 0) {
			Element rsmElement = (Element) nl.item(0);
			rngSeedManipulator.load(rsmElement);
		}
		rngSeedManipulator.updatePossibleRandomSeedParameters(constantInfos, designInfos);
		updateDesignPanel();
	}

	public void save(Node node) {
		if (inStandardPlugin && standardPlugin.isModifiedDesignTable()) {
			standardPlugin.save(node);
		} else {
			Document doc = node.getOwnerDocument();
			Element parametersElem = doc.createElement(PARAMETERS_ELEM);
			for (int i = 0; i < constantInfos.size(); i++) {
				parametersElem.appendChild(constantInfos.get(i)
						.getInfoElement(doc, false));
			}
			for (int i = 0; i < designInfos.size(); i++) {
				parametersElem.appendChild(designInfos.get(i).getInfoElement(doc, true));

				if (designInfos.get(i).isNumeric()) {
					Element varyingParameter = doc.createElement(IIntelliMethodPlugin.VARYING_NUMERIC_PARAMETER);
					varyingParameter.setAttribute("name", designInfos.get(i).getName());
					node.appendChild(varyingParameter);
				}
			}
			Element rsmElement = doc.createElement(RngSeedManipulatorModel.RSM_ELEMENT_NAME);
			rngSeedManipulator.save(rsmElement);
			node.appendChild(rsmElement);
			node.appendChild(parametersElem);
		}
	}

	public String getLocalizedName() {
		if (inStandardPlugin && standardPlugin.isModifiedDesignTable()) {
			return standardPlugin.getLocalizedName();
		} else
			return getLocalizedNameOfThis();
	}

	protected abstract String getLocalizedNameOfThis();

	@Override
	protected void createThisDesignInStandard() {
		standardPlugin.clearDesign();
		standardPlugin.validateLists();
		Vector<String> parameterNames = new Vector<String>();
		for (int i = 0; i < designInfos.size(); i++) {
			parameterNames.add(designInfos.get(i).getName());
		}
		standardPlugin.makeTheseDesignParameters(parameterNames);
		standardPlugin.getConstantParameters().clear();
		for (int i = 0; i < constantInfos.size(); i++) {
			standardPlugin.getConstantParameters().add(standardPlugin.new StandardDOEInfo(
					constantInfos.get(i)));
		}
		standardPlugin.addNRuns(getDesignSize());
		createThisDesignInStandardMethodSpecificPart();
		// copy RandomSeedManipulator settings to the Standard DoE plugin:
		copyRngSeedManipulatorSettingsToStandardPlugin();
	}

	protected abstract void createThisDesignInStandardMethodSpecificPart();

	protected void copyRngSeedManipulatorSettingsToStandardPlugin() {
		DocumentBuilder db = null;
		try {
			db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = db.newDocument();
			Element rsm = doc.createElement("rsmSwap");
			rngSeedManipulator.save(rsm);
			standardPlugin.getRngSeedManipulator().load(rsm);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void rngSeedsChanged() {
		updateDesignPanel();
	}

	protected JPanel getExtraDesignPanel() {
		return extraDesignPanel;
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

}
