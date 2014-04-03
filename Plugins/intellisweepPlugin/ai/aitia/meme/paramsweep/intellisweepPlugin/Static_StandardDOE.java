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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ai.aitia.meme.gui.RngSeedManipulator;
import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.generator.RngSeedManipulatorModel;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.IStandardDoeDisplayer;
import ai.aitia.meme.paramsweep.plugin.IIntelliContext;
import ai.aitia.meme.paramsweep.plugin.IIntelliMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IIntelliStaticMethodPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.paramsweep.utils.WizardLoadingException;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;

@SuppressWarnings("serial")
public class Static_StandardDOE implements IIntelliStaticMethodPlugin, ActionListener, DocumentListener, TableModelListener, ListDataListener, ChangeListener{
	
	private static final String RETURN_TO_OTHER_PLUGIN = "RETURN_TO_OTHER_PLUGIN";

	protected final class StandardParameterSelectionPanel extends ParameterSelectionPanel {

		@Override
		protected void moveParametersToDesign(int[] idxs) {
	    	for (int i = 0; i < idxs.length; i++) {
	    		getConstantVector().get(idxs[i]).setDefinitionType(StandardDOEInfo.LIST_DEF);
	    		if(getConstantVector().get(idxs[i]).getValues().size() < designTableModel.getRowCount()){
	    			getConstantVector().get(idxs[i]).fillValuesWithDefaultToRowCount(designTableModel.getRowCount());
	    		}
	    		getDesignVector().add(getConstantVector().get(idxs[i]));
	    	}
	    	for (int i = idxs.length - 1; i >= 0; i--) {
	    		getConstantVector().remove(idxs[i]);
	    	}
	    	designTableModel.fireTableStructureChanged();
	    	setMinimumColumnSizes();
	    	modifiedDesignTable = true;
	    	rngSeedManipulator.updatePossibleRandomSeedParameters(constantParameters, designTableParameters);
	    }

		@Override
		protected void moveParametersFromDesign(int[] idxs) {
	    	for (int i = 0; i < idxs.length; i++) {
	    		getDesignVector().get(idxs[i]).setDefinitionType(StandardDOEInfo.CONST_DEF);
	    		getConstantVector().add(getDesignVector().get(idxs[i]));
	    	}
	    	for (int i = idxs.length - 1; i >= 0; i--) {
	    		getDesignVector().remove(idxs[i]);
	    	}
	    	designTableModel.fireTableStructureChanged();
	    	setMinimumColumnSizes();
	    	modifiedDesignTable = true;
	    	rngSeedManipulator.updatePossibleRandomSeedParameters(constantParameters, designTableParameters);
	    }
	    
        @Override
		@SuppressWarnings("unchecked")
		public Vector<StandardDOEInfo> getConstantVector() {
	        return constantParameters;
        }

        @Override
		@SuppressWarnings("unchecked")
		public Vector<StandardDOEInfo> getDesignVector() {
	        return designTableParameters;
        }

        @Override
		protected void newParametersPressed() {
        	ctx.getNewParametersButton().doClick();
        	validateLists();
        }
    }
	
	public class StandardDOEInfo extends ParameterInfo {
		private static final String STANDARD_DOE_INFO_ELEM = "StandardDOEInfo";
		private static final long serialVersionUID = -5532293260197689727L;
		private static final String NAME_ATTR = "name";
		private static final String TYPE_ATTR = "type";
		private static final String IN_DESIGN_ATTR = "inDesign";
		private static final String DEFTYPE_ATTR = "definitionType";
		private static final String LISTSIZE_ATTR = "valuesListSize";
		private static final String VALUES_VALUE_ELEM = "valueOfParameter";
		private static final String VALUE_INDEX_ATTR = "valueIndex";
		private static final String VALUE_ATTR = "value";
		
        public StandardDOEInfo(String name, String type, Class<?> javaType) {
	        super(name, type, javaType);
	        setDefinitionType(CONST_DEF);
        }
        
        public StandardDOEInfo(ParameterInfo info) {
        	super(info.getName(), info.getType(), info.getJavaType());
        	Vector<Object> newValues = new Vector<Object>(info.getValues().size());
        	for (int i = 0; i < info.getValues().size(); i++) {
	            newValues.add(new Object());
            }
        	Collections.copy(newValues, info.getValues());
        	this.setValues(newValues);
        	this.setDefinitionType(info.getDefinitionType());
        	this.runs = 1;
        }

		@Override
		public String toString(){
			StringBuffer sb = null;
			if(getDefinitionType() == CONST_DEF){
				sb = new StringBuffer("["+getName()+": ");
        		sb.append("default value = ");
        		sb.append(values.get(0));
        	} else if(getDefinitionType() == LIST_DEF){
        		return getName();
//        		sb.append("values = ");
//        		for (int i = 0; i < values.size() - 1; i++) {
//            		sb.append(values.get(i));
//	                sb.append(", ");
//                }
//        		sb.append(values.get(values.size()-1));
        	}
        	sb.append("]");
        	return sb.toString();
        }
		
	    public void fillValuesWithDefaultToRowCount(int rowCount) {
	    	while(values.size() < rowCount) {
	    		Object last = values.get(values.size()-1);
	    		addOneDefaultValue();
	    		values.set(values.size() - 1, last);
	    	}
        }
	    
	    public void addOneDefaultValue(){
    		if ("byte".equals(type) || "Byte".equals(type))
    			values.add(new Byte("0"));
    		else if ("short".equals(type) || "Short".equals(type)) 
    			values.add(new Short("0"));
    		else if ("int".equals(type) || "Integer".equals(type))
    			values.add(new Integer(0));
    		else if ("long".equals(type) || "Long".equals(type))
    			values.add(new Long(0));
    		else if ("float".equals(type) || "Float".equals(type))
    			values.add(new Float(0));
    		else if ("double".equals(type) || "Double".equals(type))
    			values.add(new Double(0));
    		else if ("boolean".equals(type) || "Boolean".equals(type))
    			values.add(new Boolean(false));
    		else if ("String".equals(type))
    			values.add("");
	    }
	    
	    public boolean load(Element elem){
	    	boolean inDesign = false;
	    	this.name = elem.getAttribute(NAME_ATTR);
	    	this.type = elem.getAttribute(TYPE_ATTR);
	    	try {
	            this.defType = Integer.parseInt(elem.getAttribute(DEFTYPE_ATTR));
            } catch (NumberFormatException e) {
            	this.defType = CONST_DEF;
            }
            inDesign = Boolean.parseBoolean(elem.getAttribute(IN_DESIGN_ATTR));
            int listSize = 0;
            try {
				listSize = Integer.parseInt(elem.getAttribute(LISTSIZE_ATTR));
            } catch (NumberFormatException e) {
            	listSize = 0;
            }
            this.values = new Vector<Object>(listSize);
            for (int i = 0; i < listSize; i++) {
	            this.values.add(new Object());
            }
            NodeList nl = elem.getElementsByTagName(VALUES_VALUE_ELEM);
            for (int i = 0; i < nl.getLength(); i++) {
	            Element valueElem = (Element) nl.item(i);
	            int idx = Integer.parseInt(valueElem.getAttribute(VALUE_INDEX_ATTR));
	            this.values.set(idx, StandardDOEInfo.getValue(valueElem.getAttribute(VALUE_ATTR), this.type));
            }
	    	return inDesign;
	    }
	    
	    public Element getInfoElement(Document doc, boolean inDesign){
	    	Element elem = doc.createElement(STANDARD_DOE_INFO_ELEM);
			elem.setAttribute(NAME_ATTR, this.name);
			elem.setAttribute(TYPE_ATTR, this.type);
			elem.setAttribute(DEFTYPE_ATTR, ""+this.defType);
	    	elem.setAttribute(IN_DESIGN_ATTR, inDesign ? "true" : "false");
	    	elem.setAttribute(LISTSIZE_ATTR, ""+this.values.size());
	    	for (int i = 0; i < values.size(); i++) {
	            Element listElem = doc.createElement(VALUES_VALUE_ELEM);
	            listElem.setAttribute(VALUE_INDEX_ATTR, ""+i);
	            listElem.setAttribute(VALUE_ATTR, values.get(i).toString());
	            elem.appendChild(listElem);
            }
	    	return elem;
	    }
	}
	
	@SuppressWarnings("serial")
	public class StandardDOETableModel extends AbstractTableModel{
		protected Vector<StandardDOEInfo> tableColumParameters = null;
		protected String firstColumnName = null;
		protected boolean extraFirstColumn = true;
		
		public StandardDOETableModel(Vector<StandardDOEInfo> parameters, String firstColumnName){
			this.tableColumParameters = parameters;
			this.firstColumnName = firstColumnName;
			if(firstColumnName != null) extraFirstColumn = true;
			else extraFirstColumn = false;
		}
		
        @Override
		public String getColumnName(int column) {
        	if(extraFirstColumn){
        		if(column == 0) return firstColumnName;
        		column--;
        	}
	        return tableColumParameters.get(column).getName();
        }

		public int getColumnCount() {
        	if(extraFirstColumn) return 1 + tableColumParameters.size();
        	else return tableColumParameters.size();
        }


		@Override
		public Class<?> getColumnClass(int column) {
			if(extraFirstColumn){
				if(column == 0) return Integer.class;
				column--;
			}
			//System.out.println(tableColumParameters.get(column).getValue().getClass().getName());
	        return tableColumParameters.get(column).getValue().getClass();// tableColumParameters.get(column).getJavaType();
        }

		public int getRowCount() {
			int shortestList = Integer.MAX_VALUE;
			for (int i = 0; i < tableColumParameters.size(); i++) {
	            if(shortestList > tableColumParameters.get(i).getValues().size()){
	            	shortestList = tableColumParameters.get(i).getValues().size();
	            }
            }
			return shortestList == Integer.MAX_VALUE ? 0 : shortestList;
        }

		public Object getValueAt(int row, int column) {
			if(extraFirstColumn){
				if (column == 0){
					return new Integer(row + 1);
				}
				column--;
			}
			Object ret;
            try {
	            ret = tableColumParameters.get(column).getValues().get(row);
            } catch (IndexOutOfBoundsException e) {
	            ret = "";
            }
	        return ret;
        }

        @Override
		public boolean isCellEditable(int row, int column) {
        	if(extraFirstColumn){
        		if (column == 0) return false;
        		else return true;
        	}
        	else return true;
        }

        @Override
		public void setValueAt(Object object, int row, int column) {
        	if(extraFirstColumn){
        		column--;
        	}
        	setValueInDesignAt(object, row, column);
        }

		/**
		 * This method always sets the object at the row and column in the
		 * underlying data structure. It is used in modifications to the model
		 * from outside of the JTable and its editors. 
		 * @param object
		 * @param row
		 * @param column
		 */
		public void setValueInDesignAt(Object object, int row, int column) {
        	if(!getValueAt(row, column).equals(object)) modifiedDesignTable = true;
        	tableColumParameters.get(column).getValues().set(row, object);
		}
		
		
	}
	
	protected static final String ADD_ONE_RUN = "addOneRun";
	protected static final String ADD_N_RUNS = "addNRuns";
	protected static final String INSERT_ONE_RUN = "insertOneRun";
	protected static final String INSERT_N_RUNS = "insertNRuns";
	protected static final int DEFAULT_N = 10;
	protected static final String DELETE_ROWS = "deleteRows";
	private static final int MIN_COLUMN_WIDTH = 100;
	private static final String IMPORT_CSV = "importCSV";
	private static final String PARAMETERS_ELEM = "parametersElement";

	protected Vector<StandardDOEInfo> constantParameters = null;
	protected Vector<StandardDOEInfo> designTableParameters = null;
	protected Vector<StandardDOEInfo> auxiliaryDefaultParameters = null;
	protected Vector<StandardDOEInfo> auxiliaryDefaultConstParameters = null;
	
	protected int rowCount = 0;
	protected int addN = DEFAULT_N;
	protected int insertN = DEFAULT_N;
	
	protected JPanel content = null;
	protected ParameterSelectionPanel paramSelectPanel = null;
	protected JTable designTable = null; 
	protected JScrollPane designTableScr = null;
	protected StandardDOETableModel designTableModel;
	
	protected JPanel rowMoverPanel = null;
	protected JButton toTopButton = new JButton("To top");
	protected JButton oneUpButton = new JButton("One up");
	protected JButton oneDownButton = new JButton("One down");
	protected JButton toBottomButton = new JButton("To bottom");
	protected JButton deleteRowButton = new JButton("Delete row(s)");
	
	protected JPanel rowAdderPanel = null;
	protected JButton addOneRunButton = new JButton("Add 1 run");
	protected JButton addNRunsButton = new JButton("Add runs...");
	protected JTextField addNField = new JTextField(4);
	protected JButton insertOneRunButton = new JButton("Insert 1 run");
	protected JButton insertNRunsButton = new JButton("Insert runs...");
	protected JTextField insertNField = new JTextField(4);
	
	protected JTable auxiliaryDefaultTable = null;
	protected StandardDOETableModel auxiliaryTableModel = null;
	protected JScrollPane auxiliaryScr = null;
	protected JButton importButton = new JButton("Import table from CSV file...");
	
	protected RngSeedManipulator rngSeedManipulator = null;
	protected ImportCSVForDOE importDialog = null;
	
	protected IIntelliContext ctx = null;
	private boolean runFromOtherPlugin = false;
	private boolean modifiedDesignTable = false;
	@SuppressWarnings("unused")
	private JPanel otherPluginContent = null;
	protected JButton returnToOtherPluginButton = new JButton("Return to _original_ method...");
	private IStandardDoeDisplayer otherPlugin;
	private String readyStatusDetail;

	public boolean alterParameterTree(IIntelliContext ctx) {
		DefaultMutableTreeNode root = ctx.getParameterTreeRootNode();
		DefaultMutableTreeNode newRoot = getAlteredParameterTreeRootNode();
		rngSeedManipulator.getModel().randomizeParameterTree(newRoot);
		if(root != null){
			root.removeAllChildren();
			int count = newRoot.getChildCount();
			for (int i = 0; i < count; i++) {
				root.add((DefaultMutableTreeNode) newRoot.getChildAt(0));
			}
			return true;
		} else return false;
	}
	
	public void validateLists() {
	    // TODO: validate lists (const, design, auxili)
		boolean[] found = new boolean[ctx.getParameters().size()];
		for (int i = 0; i < found.length; i++) {
	        found[i] = false;
        }
		//remove the parameters not on the lists anymore:
		for (Iterator iter = constantParameters.iterator(); iter.hasNext();) {
	        StandardDOEInfo info = (StandardDOEInfo) iter.next();
	        boolean foundInfo = false;
	        for (int i = 0; i < found.length && !foundInfo; i++) {
	            if(ctx.getParameters().get(i).getName().equals(info.getName())){
	            	found[i] = true;
	            	foundInfo = true;
	            }
            }
	        if(!foundInfo) {
	        	iter.remove();
	        }
        }
		for (Iterator iter = designTableParameters.iterator(); iter.hasNext();) {
	        StandardDOEInfo info = (StandardDOEInfo) iter.next();
	        boolean foundInfo = false;
	        for (int i = 0; i < found.length && !foundInfo; i++) {
	            if(ctx.getParameters().get(i).getName().equals(info.getName())){
	            	found[i] = true;
	            	foundInfo = true;
	            }
            }
	        if(!foundInfo) {
	        	iter.remove();
	        }
        }
		for (Iterator iter = auxiliaryDefaultParameters.iterator(); iter.hasNext();) {
	        StandardDOEInfo info = (StandardDOEInfo) iter.next();
	        boolean foundInfo = false;
	        for (int i = 0; i < found.length && !foundInfo; i++) {
	            if(ctx.getParameters().get(i).getName().equals(info.getName())){
	            	foundInfo = true;
	            }
            }
	        if(!foundInfo) {
	        	iter.remove();
	        }
        }
		for (Iterator iter = auxiliaryDefaultConstParameters.iterator(); iter.hasNext();) {
	        StandardDOEInfo info = (StandardDOEInfo) iter.next();
	        boolean foundInfo = false;
	        for (int i = 0; i < found.length && !foundInfo; i++) {
	            if(ctx.getParameters().get(i).getName().equals(info.getName())){
	            	foundInfo = true;
	            }
            }
	        if(!foundInfo) {
	        	iter.remove();
	        }
        }
		//adding the parameters not found on the lists:
		for (int i = 0; i < found.length; i++) {
	        if(!found[i]) {
	        	constantParameters.add(new StandardDOEInfo(ctx.getParameters().get(i)));
				StandardDOEInfo aux = new StandardDOEInfo(ctx.getParameters().get(i));
				aux.setInitValue();
				if(aux.getType().toLowerCase().startsWith("string")) aux.setValue("");
				aux.setDefinitionType(StandardDOEInfo.LIST_DEF);
				auxiliaryDefaultConstParameters.add(aux);
	        }
        }
    }
	
	private DefaultMutableTreeNode getAlteredParameterTreeRootNode(){
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Parameter file");
		for (int i = 0; i < constantParameters.size(); i++) {
	        constantParameters.get(i).setDefinitionType(ParameterInfo.CONST_DEF);
	        constantParameters.get(i).setRuns(1);
	        StandardDOEInfo newInfo = new StandardDOEInfo(constantParameters.get(i));
	        root.add(new DefaultMutableTreeNode(newInfo));
        }
        for (int i = 0; i < designTableParameters.size(); i++) {
        	designTableParameters.get(i).setDefinitionType(LatinFactorInfo.LIST_DEF);
        	designTableParameters.get(i).setRuns(1);
	        StandardDOEInfo newInfo = new StandardDOEInfo(designTableParameters.get(i));
        	root.add(new DefaultMutableTreeNode(newInfo));
        }
        return root;
	}

	public JPanel getSettingsPanel(IIntelliContext ctx) {
		this.ctx = ctx;
		if(content == null){
			importDialog = new ImportCSVForDOE(ParameterSweepWizard.getFrame(), "Import table from a CSV file", true, ctx.getParameters());
			initInfosFromContext(ctx);
			designTableModel = new StandardDOETableModel(designTableParameters, "Run #");
			designTable = new JTable(designTableModel);
			designTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			designTableScr = new JScrollPane(designTable);
			designTableScr.setPreferredSize(new Dimension(400, 150));
			//designTableScr.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			//designTableScr.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			
			auxiliaryTableModel = new StandardDOETableModel(auxiliaryDefaultParameters, "#");
			auxiliaryDefaultTable = new JTable(auxiliaryTableModel);
			auxiliaryDefaultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			auxiliaryScr = new JScrollPane(auxiliaryDefaultTable);
			auxiliaryScr.setPreferredSize(new Dimension(400, 70));
			designTableScr.getHorizontalScrollBar().getModel().addChangeListener(this);
			//auxiliaryScr.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			//auxiliaryScr.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			setMinimumColumnSizes();

			paramSelectPanel = new StandardParameterSelectionPanel();
			rngSeedManipulator = new RngSeedManipulator(ctx.getParameters(), this);
			rngSeedManipulator.updatePossibleRandomSeedParameters(constantParameters, designTableParameters);
			rowMoverPanel = FormsUtils.build(
					"~ p ~", 
					"0 p||" +
					"1 p||" +
					"2 p||" +
					"3 p||" +
					"4 p|||" +
					"5 p||",
					toTopButton,
					oneUpButton,
					oneDownButton,
					toBottomButton,
					deleteRowButton,
					importButton).getPanel();
			//TODO: import rendberak�sa
			importButton.setVisible(false);
			rowAdderPanel = FormsUtils.build(
					"~ p p ~", 
					"00 p||" +
					"12 p||" +
					"33 p||" +
					"45 p", 
					addOneRunButton,
					addNRunsButton, addNField,
					insertOneRunButton,
					insertNRunsButton, insertNField).getPanel();
			//TODO: insert-et csin�lni (egyszer�)
			insertNField.setVisible(false);
			insertNRunsButton.setVisible(false);
			insertOneRunButton.setVisible(false);
			content = FormsUtils.build(
					"~ p f:p:g ~", 
					"00 p||" +
					"12 f:p:g||" +
					"34 p|" +
					"55 p||" +
					"6_ p|", 
					paramSelectPanel.getPanel(),
					rowMoverPanel, designTableScr,
					rowAdderPanel, auxiliaryScr,
					rngSeedManipulator.getSeedsListPanel(),
					returnToOtherPluginButton).getPanel();
			//TODO: seg�dt�bl�s beilleszt�st, hozz�ad�st megcsin�lni
			auxiliaryScr.setVisible(false);
			returnToOtherPluginButton.setVisible(runFromOtherPlugin);
			toTopButton.setEnabled(false);
			oneUpButton.setEnabled(false);
			oneDownButton.setEnabled(false);
			toBottomButton.setEnabled(false);
			importDialog.makeGUI();
			initialize();
		}
	    return content;
    }

	private void initInfosFromContext(IIntelliContext ctx) {
	    constantParameters = new Vector<StandardDOEInfo>();
	    designTableParameters = new Vector<StandardDOEInfo>();
	    auxiliaryDefaultParameters = new Vector<StandardDOEInfo>();
	    auxiliaryDefaultConstParameters = new Vector<StandardDOEInfo>();
	    for (int i = 0; i < ctx.getParameters().size(); i++) {
	    	StandardDOEInfo info = new StandardDOEInfo(ctx.getParameters().get(i));
	    	StandardDOEInfo aux = new StandardDOEInfo(ctx.getParameters().get(i));
	    	aux.setInitValue();
	    	if(aux.getType().toLowerCase().startsWith("string")) aux.setValue("");
	    	aux.setDefinitionType(StandardDOEInfo.LIST_DEF);
	    	auxiliaryDefaultConstParameters.add(aux);
	    	if (info.getDefinitionType() == StandardDOEInfo.CONST_DEF){
	    		constantParameters.add(info);
	    	} else if (info.getDefinitionType() == StandardDOEInfo.LIST_DEF){
	    		designTableParameters.add(info);
	    		auxiliaryDefaultParameters.add(aux);
	    	} else{
	    		info.setDefinitionType(StandardDOEInfo.CONST_DEF);
	    		constantParameters.add(info);
	    	}
	    }
    }

	private void initialize() {
		addOneRunButton.setActionCommand(ADD_ONE_RUN);
		addNRunsButton.setActionCommand(ADD_N_RUNS);
		addNField.setText(new Integer(addN).toString());
		addNRunsButton.setText("Add " + addN + " runs");
		addNField.getDocument().addDocumentListener(this);
		insertNField.setText(new Integer(insertN).toString());
		insertNRunsButton.setText("Insert " + insertN + " runs");
		insertNField.getDocument().addDocumentListener(this);
		
		deleteRowButton.setActionCommand(DELETE_ROWS);
		importButton.setActionCommand(IMPORT_CSV);
		
		returnToOtherPluginButton.setActionCommand(RETURN_TO_OTHER_PLUGIN);
		
		GUIUtils.addActionListener(
				this, 
				addOneRunButton, addNRunsButton, insertOneRunButton, insertNRunsButton, 
				deleteRowButton, importButton, returnToOtherPluginButton);
		designTableModel.addTableModelListener(this);
		paramSelectPanel.addDesignListDataListener(this);
    }

	public String getDescription() {
	    return "This method provides a table of runs that the user can modify.";
    }

	public int getMethodType() {
	    return IIntelliMethodPlugin.STATIC_METHOD;
    }

	public boolean getReadyStatus() {
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
		content = null;
    }

	public boolean isImplemented() {
	    return true;
    }

	public void load(IIntelliContext context, Element pluginElem) throws WizardLoadingException {
		invalidatePlugin();
		NodeList nl = null;
		nl = pluginElem.getElementsByTagName(PARAMETERS_ELEM);
		if (nl != null && nl.getLength() > 0){
			Element parametersElem = (Element) nl.item(0);
			List<ParameterInfo> paramList = context.getParameters();
			nl = null;
			nl = parametersElem.getElementsByTagName(StandardDOEInfo.STANDARD_DOE_INFO_ELEM);
			for (int i = 0; i < nl.getLength(); i++) {
				Element infoElem = (Element) nl.item(i);
				String name = infoElem.getAttribute(StandardDOEInfo.NAME_ATTR);
				for (int j = 0; j < paramList.size(); j++) {
	                if(paramList.get(j).getName().equals(name)){
	                	StandardDOEInfo info = new StandardDOEInfo(paramList.get(j));
	                	boolean inDesign = info.load(infoElem);
	                	paramList.get(j).setRuns(1);
	                	paramList.get(j).setDefinitionType(inDesign ? ParameterInfo.LIST_DEF : ParameterInfo.CONST_DEF);
	                	paramList.get(j).setValues(info.getValues());
	                }
                }
            }
		}
		getSettingsPanel(context); //this creates a new, clean gui and model
		nl = null;
		nl = pluginElem.getElementsByTagName(RngSeedManipulatorModel.RSM_ELEMENT_NAME);
		if (nl != null && nl.getLength() > 0){
			Element rsmElement = (Element) nl.item(0);
			rngSeedManipulator.load(rsmElement);
		}
		rngSeedManipulator.updatePossibleRandomSeedParameters(constantParameters, designTableParameters);
    }

	public void save(Node node) {
		Document doc = node.getOwnerDocument();
		Element parametersElem = doc.createElement(PARAMETERS_ELEM);
		for (int i = 0; i < constantParameters.size(); i++) {
	        parametersElem.appendChild(constantParameters.get(i).getInfoElement(doc, false));
        }
		for (int i = 0; i < designTableParameters.size(); i++) {
	        parametersElem.appendChild(designTableParameters.get(i).getInfoElement(doc, true));

	        if (designTableParameters.get(i).isNumeric()) {
	        	Element varyingParameter = doc.createElement(IIntelliMethodPlugin.VARYING_NUMERIC_PARAMETER);
	        	varyingParameter.setAttribute("name", designTableParameters.get(i).getName());
	        	node.appendChild(varyingParameter);
	        }
		}
		Element rsmElement = doc.createElement(RngSeedManipulatorModel.RSM_ELEMENT_NAME);
		rngSeedManipulator.save(rsmElement);
		node.appendChild(rsmElement);
		node.appendChild(parametersElem);
    }

	public String getLocalizedName() {
	    return "Standard Design of Experiments plugin";
    }

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(ADD_ONE_RUN)){
			int lastRunIdx = addOneRun();
			designTableModel.fireTableRowsInserted(lastRunIdx, lastRunIdx);
			modifiedDesignTable = true;
			//TODO: "odatekerni" a t�bl�zatot
		} else if(e.getActionCommand().equals(ADD_N_RUNS)){
			addNRuns(addN);
			modifiedDesignTable = true;
		} else if(e.getActionCommand().equals(DELETE_ROWS)){
			deleteSelectedRows();
			modifiedDesignTable = true;
		} else if(e.getActionCommand().equals(IMPORT_CSV)){
			showCSVImportDialog();
		} else if(e.getActionCommand().equals(RETURN_TO_OTHER_PLUGIN)){
			int result = 0;
			if(modifiedDesignTable){
				result = Utilities.askUser(ParameterSweepWizard.getFrame(),
						false,
						"Warning",
						"All modifications will be lost.",
				   		"Do you want to continue?");
			} else result = 1;
			if (result == 1) { //yes
				otherPlugin.returnToOriginalMethod();
				setRunFromOtherPlugin(false, otherPlugin);
			}
		}
    }
	
	public void clearDesign(){
		for (int i = 0; i < constantParameters.size(); i++) {
	        constantParameters.get(i).getValues().clear();
        }
		for (int i = 0; i < designTableParameters.size(); i++) {
			designTableParameters.get(i).getValues().clear();
        }
	}
	
	public void makeTheseDesignParameters(List<String> parameterNames){
		//emptying the design list:
		int length = designTableParameters.size();
		int[] idxs = new int[length];
		for (int i = 0; i < idxs.length; i++) {
	        idxs[i] = i;
        }
		paramSelectPanel.moveParametersFromDesign(idxs);
		//search for the parameterNames and put into design:
		for (int i = 0; i < parameterNames.size(); i++) {
			int j = 0;
			while(!constantParameters.get(j).getName().equalsIgnoreCase(parameterNames.get(i))) j++;
			int[] idx = new int[1];
			idx[0] = j;
			paramSelectPanel.moveParametersToDesign(idx);
        }
	}

	public void addNRuns(int n) {
	    int lastRunIdx = 0;
	    for (int i = 0; i < n; i++) {
	    	lastRunIdx = addOneRun();
	    }
	    designTableModel.fireTableRowsInserted(lastRunIdx, lastRunIdx);
    }
	
	private void showCSVImportDialog(){
		importDialog.setVisible(true);
		if(importDialog.isOkPressed()){
			//if OK was pressed on the dialog:
			if(importDialog.isClearData()){
				clearData();
				initInfosFromContext(ctx);
			} else {
				String[] toImport = importDialog.getImportedParameterNames();
				for (int i = 0; i < toImport.length; i++) {
					@SuppressWarnings("unused")
					boolean inDesign = false;
                    Vector<StandardDOEInfo> foundInThisList = designTableParameters;
                    int j = 0;
                    boolean found = false;
                    while(j < foundInThisList.size() && !found){
                    	if(foundInThisList.get(j).getName().equals(toImport[i])){
                    		found = true;
                    		inDesign = true;
                    	}
                    	if(!found) j++;
                    }
                    if(found){
                    	ParameterInfo parInfo = null;
                    	for (int k = 0; k < ctx.getParameters().size(); k++) {
                            if(toImport[i].equals(ctx.getParameters().get(k).getName())){
                            	parInfo = ctx.getParameters().get(k);
                            }
                        }
                    	foundInThisList.set(j, new StandardDOEInfo(parInfo));
                    } else {
                    	foundInThisList = constantParameters;
                    	j = 0;
	                    while(j < foundInThisList.size() && !found){
	                    	if(foundInThisList.get(j).getName().equals(toImport[i])){
	                    		found = true;
	                    	}
	                    	if(!found) j++;
	                    }
	                    if(found){
	                    	ParameterInfo parInfo = null;
	                    	for (int k = 0; k < ctx.getParameters().size(); k++) {
	                            if(toImport[i].equals(ctx.getParameters().get(k).getName())){
	                            	parInfo = ctx.getParameters().get(k);
	                            }
                            }
	                    	foundInThisList.set(j, new StandardDOEInfo(parInfo));
	                    	int[] moveIdx = new int[1];
	                    	moveIdx[0] = j;
	                    	paramSelectPanel.moveParametersToDesign(moveIdx);
	                    }
                    }
                }
			}
		}
	}
	
	public void clearData() {
		//removing all parameters from the design
		int[] idxs = new int[designTableParameters.size()];
		for (int i = 0; i < designTableParameters.size(); i++) idxs[i] = i;
		paramSelectPanel.moveParametersFromDesign(idxs);
		//the parameters in the ctx.getParameters are OK...
		
    }

	protected void deleteSelectedRows() {
		int[] idxs = designTable.getSelectedRows();
		if(idxs.length > 0){
			for (int i = idxs.length - 1; i >= 0; i--) {
				deleteOneRow(idxs[i]);
            }
		}
		designTableModel.fireTableDataChanged();
    }
	
	public void deleteOneRow(int row){
        for (int j = 0; j < designTableParameters.size(); j++) {
            designTableParameters.get(j).getValues().remove(row);
        }
	}

	public int addOneRun(){
		int runCount = designTableModel.getRowCount();
		for (int i = 0; i < designTableParameters.size(); i++) {
            if(designTableParameters.get(i).getValues().size() <= runCount){
            	if(designTableParameters.get(i).getValues().size() > 0){
            		Object last = designTableParameters.get(i).getValues().get(designTableParameters.get(i).getValues().size()-1);
            		designTableParameters.get(i).addOneDefaultValue();
            		designTableParameters.get(i).getValues().set(designTableParameters.get(i).getValues().size()-1, last);
            	} else{
            		designTableParameters.get(i).addOneDefaultValue();
            	}
            }
        }
		return runCount;
	}
	
	public void setRunFromOtherPlugin(boolean runFromOtherPlugin, IStandardDoeDisplayer otherPlugin){
		this.runFromOtherPlugin = runFromOtherPlugin;
		this.otherPlugin = otherPlugin;
		returnToOtherPluginButton.setVisible(runFromOtherPlugin);
		returnToOtherPluginButton.setText("Return to " + otherPlugin.getMethodDisplayName() + " method...");
		paramSelectPanel.newParametersButton.setVisible(!runFromOtherPlugin);
		modifiedDesignTable = false;
	}

	//DocumentListener methods
	public void changedUpdate(DocumentEvent de) {
    }
	public void insertUpdate(DocumentEvent de) {
		if (de.getDocument().equals(addNField.getDocument()))
			setAddInsertButtonText(addNField, addNRunsButton);
		else if (de.getDocument().equals(insertNField.getDocument()))
			setAddInsertButtonText(insertNField, insertNRunsButton);
    }
	public void removeUpdate(DocumentEvent de) {
		if (de.getDocument().equals(addNField.getDocument()))
			setAddInsertButtonText(addNField, addNRunsButton);
		else if (de.getDocument().equals(insertNField.getDocument()))
			setAddInsertButtonText(insertNField, insertNRunsButton);
    }
	
	protected void setAddInsertButtonText(JTextField field, JButton button){
		String numberStr = field.getText();
		int number = 0;
		try {
	        number = Integer.parseInt(numberStr);
	        if(number > 0){
	        	if(button.getText().startsWith("Add")){
	        		addN = number;
	        		addNRunsButton.setText("Add " + addN + " runs");
	        	} else if(button.getText().startsWith("Insert")){
	        		insertN = number;
	        		insertNRunsButton.setText("Insert " + insertN + " runs");
	        	}
	        }
        } catch (NumberFormatException e) {
        }
	}

	public void tableChanged(TableModelEvent e) {
		if (e.getFirstRow() == TableModelEvent.HEADER_ROW){
			auxiliaryDefaultParameters.clear();
			for (int i = 0; i < designTableParameters.size(); i++) {
	            StandardDOEInfo info = designTableParameters.get(i);
	            String name = info.getName();
	            for (int j = 0; j < auxiliaryDefaultConstParameters.size(); j++) {
	                if(auxiliaryDefaultConstParameters.get(j).getName().equals(name)){
	                	auxiliaryDefaultParameters.add(auxiliaryDefaultConstParameters.get(j));
	                }
                }
            }
			auxiliaryTableModel.fireTableStructureChanged();
			//TODO: ezt a g�nys�got t�ntesd el
			SwingUtilities.invokeLater(new Runnable(){
				public void run() {
					try {
	                    Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
					setMinimumColumnSizes();
                }});
		}
    }
	
	protected void setMinimumColumnSizes(){
		for (int i = 0; i < designTable.getColumnCount(); i++) {
	        designTable.getColumnModel().getColumn(i).setMinWidth(MIN_COLUMN_WIDTH);
        }
		for (int i = 0; i < auxiliaryDefaultTable.getColumnCount(); i++) {
			auxiliaryDefaultTable.getColumnModel().getColumn(i).setMinWidth(MIN_COLUMN_WIDTH);
        }
	}

	//ListDataListener:
	public void contentsChanged(ListDataEvent e) {
		designTableModel.fireTableStructureChanged();
    }

	public void intervalAdded(ListDataEvent e) {
		contentsChanged(e);
    }

	public void intervalRemoved(ListDataEvent e) {
		contentsChanged(e);
    }

	//ChangeListener:
	public void stateChanged(ChangeEvent e) {
		if(e.getSource().equals(designTableScr.getHorizontalScrollBar().getModel())){
			int min = designTableScr.getHorizontalScrollBar().getModel().getMinimum();
			int max = designTableScr.getHorizontalScrollBar().getModel().getMaximum();
			int val = designTableScr.getHorizontalScrollBar().getModel().getValue();
			double scale = 1.0 * (val - min) / (max - min);
			min = auxiliaryScr.getHorizontalScrollBar().getModel().getMinimum();
			max = auxiliaryScr.getHorizontalScrollBar().getModel().getMaximum();
			auxiliaryScr.getHorizontalScrollBar().getModel().setValue((int) (min + scale * (max - min)));
		}
    }

	public boolean isModifiedDesignTable() {
    	return modifiedDesignTable;
    }

	public Vector<StandardDOEInfo> getConstantParameters() {
		return constantParameters;
	}

	public RngSeedManipulator getRngSeedManipulator() {
		return rngSeedManipulator;
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
		return designTableModel.getRowCount();
	}

	@Override
	public long getNumberOfRuns() {
		return getDesignSize() * rngSeedManipulator.getRunsMultiplierFactor();
	}

}
