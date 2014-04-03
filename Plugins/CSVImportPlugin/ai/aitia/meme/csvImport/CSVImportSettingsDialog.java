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
package ai.aitia.meme.csvImport;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.gui.SimpleFileFilter;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.pluginmanager.IImportPluginContext;
import ai.aitia.meme.pluginmanager.Parameter;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.FormsUtils.Separator;

import com.jgoodies.forms.layout.CellConstraints;

public class CSVImportSettingsDialog extends JDialog implements ActionListener,
																ItemListener,
																FocusListener {

	private static final long serialVersionUID = -3464816776375612415L;

	//================================================================================
	// nested classes
	static class Item<P> {
		private P item;
		private String title;
		public Item(P item, String title) { this.item = item; this.title = title; }
		public P getItem() { return item; }
		@Override public String toString() { return title; }
		@SuppressWarnings("unchecked")
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Item) {
				Item<P> other = (Item<P>)obj;
				return item.equals(other.item);
			}
			return false;
		}
	}
	
	@SuppressWarnings("serial")
	private static class ParameterTableModel extends AbstractTableModel {
		CSVResultParser parser = null;
//		BufferedCSVResultParser parser = null;
		private static String[] columnNames = new String[] { "Name", "Type" };
		final private String[] originalTypes;
		private String[] types;
		public ParameterTableModel(/*Buffered*/CSVResultParser parser) {
			if (parser == null)
				throw new IllegalArgumentException("'null' parameter is not permitted.");
			this.parser = parser;
			types = new String[parser.parameters.size()];
			originalTypes = new String[types.length];
			for (int i=0;i<types.length;++i)
				originalTypes[i] = types[i] = (parser.inputStatus[i] == parser.ONE_RUN_CONSTANT ? "Input" : "Output");
		}
		public int getColumnCount() { return 2; }
		public int getRowCount() { return parser.parameters.size();	}
		@Override public String getColumnName(int column) { return columnNames[column]; }
		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			if (columnIndex == 0) return true;
			return originalTypes[rowIndex].equals("Input");
		}
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case 0 : return parser.parameters.get(rowIndex).getName();
			case 1 : return types[rowIndex];
			}
			return null;
		}
		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case 0 : if (!findName(rowIndex,aValue.toString())) {
						 parser.parameters.get(rowIndex).setName(aValue.toString());
						 fireTableRowsUpdated(rowIndex,rowIndex);
					 }
					 break;
			case 1 : types[rowIndex] = aValue.toString();
			}
		}
		private boolean findName(int index, String name) {
			for (int i=0;i<parser.parameters.size();++i) {
				if (i == index) continue;
				Parameter p = parser.parameters.get(i);
				if (p.getName().equals(name)) return true;
			}
			return false;
		}
		
	}
	
	//================================================================================
	// constants
	
	final static String FIRST_PART = "FIRST_PART";
	final static String SECOND_PART = "SECOND_PART";
	
	final static String NORMAL_PANEL = "NORMAL_PANEL";
	final static String ADVANCED_PANEL = "ADVANCED_PANEL";
	
	final static int ONE_RUN = 0;
	final static int RECORD_PER_RUN = 1;
	final static int USE_TICK = 2;
	
	protected final static Vector<Item<String>> quotes = new Vector<Item<String>>();
	
	static {
		quotes.add(new Item<String>("\"","Double qoute (\")"));
		quotes.add(new Item<String>("'", "Single qoute (')"));
	}
		
	//================================================================================
	// variables
	
	static File lastFile = null;
	
	private CSVResultParser parser = null;
//	private BufferedCSVResultParser parser = null;
	protected IImportPluginContext ctx = null;
	protected Properties settings = null;
	
	List<String> delimiters = null;
	boolean mergeConsecutive = false;
	String quote, comment = "#";
	String nullToken = null;
	boolean containsHeader, isIgnore = false;
	int ignoreNumber = 0;
	int runStrategy = ONE_RUN;
	int recordsPerRun = -1;
	String tickColumn = null;
	boolean selectedModel = false;
	private boolean scheduleReset = false;
	boolean isAdvancedCSVType = false;
	String linePattern = null, headerPattern = null;
	
	//================================================================================
	// GUI components
	
	private JPanel content = new JPanel(new CardLayout());
	private JTextField fileTextField = new JTextField();
	private JButton browseButton = new JButton("Browse...");
	private JButton loadSettingsButton = new JButton("Load settings...");
	private JButton saveSettingsButton = new JButton("Save settings...");
	private JRadioButton normalButton = new JRadioButton("Normal CSV format");
	private JRadioButton advancedButton = new JRadioButton("Advanced CSV format");
	private JPanel stringSettingsPanel = new JPanel(new CardLayout());
	private JCheckBox tabBox = new JCheckBox("Tab");
	private JCheckBox semiColonBox = new JCheckBox("Semi-colon");
	private JCheckBox commaBox = new JCheckBox("Comma");
	private JCheckBox spaceBox = new JCheckBox("Space");
	private JCheckBox otherBox = new JCheckBox("Other:");
	private JTextField otherField = new JTextField(1);
	private JPanel other = FormsUtils.build("p d",
											"01",
											otherBox,otherField).getPanel();
	private JCheckBox mergeBox = new JCheckBox("Merge consecutive delimiters");
	private JPanel delimiterPanel = FormsUtils.build("p ~ p ~ p ~ p ~ p" ,
			 										 "01234",
			 										 tabBox,semiColonBox,commaBox,
			 										 spaceBox,other).getPanel();
	private JComboBox quoteBox = new JComboBox(quotes);
	private JTextField commentField = new JTextField("#");
	private JTextField commentField2 = new JTextField("#");
	private JPanel normalSettingsPanel = FormsUtils.build("p:g p",
														  "0||" +
														  "1|",
														  FormsUtils.build("p ~ p",
																  		   "01",
																  		   delimiterPanel,mergeBox).getPanel(),
														  FormsUtils.build("p p:g(0.2) p:g(0.8)",
																  		   "01_|" +
																  		   "233",
																  		   "Quote string: ",quoteBox,
																  		   "Comment character: ",commentField, CellConstraints.LEFT).getPanel()).getPanel();
	private JTextArea linePatternField = new JTextArea();
	private JScrollPane linePatternScr = new JScrollPane(linePatternField);
	private JPanel advancedSettingsPanel = FormsUtils.build("p p:g",
															"|01||" +
															"_1 p:g|" +
															"23 p",
															"Line pattern: ",linePatternScr,
															"Comment character: ",commentField2, CellConstraints.LEFT).getPanel();
	private JRadioButton tNullButton = new JRadioButton("null");
	private JRadioButton tNAButton = new JRadioButton("N\\A");
	private JRadioButton tnaButton = new JRadioButton("na");
	private JRadioButton tNothingButton = new JRadioButton("<nothing>");
	private JRadioButton tOtherButton = new JRadioButton("Other:");
	private JTextField tOtherField = new JTextField(10);
	private JPanel tOther = FormsUtils.build("p d",
											  "01",
											  tOtherButton,tOtherField).getPanel();
	private JRadioButton contains = new JRadioButton("Contains column names");
	private JRadioButton notcontains = new JRadioButton("Not contains column names");
	private JLabel headerPatternLabel = new JLabel(" Header pattern: ");
	private JTextField headerPatternField = new JTextField();
	private JCheckBox ignore = new JCheckBox("Ignore first ");
	private JTextField ignoreField = new JTextField();
	private JRadioButton oneRunButton = new JRadioButton("The whole file is one run");
	private JRadioButton rprButton = new JRadioButton("Number of records per run:");
	private JTextField rprField = new JTextField(5);
	private JRadioButton tickButton = new JRadioButton("Use this column as 'tick' to separate runs:");
	private JTextField tickField = new JTextField(12);
	private JTextArea preview = new JTextArea(6,50);
	private JScrollPane scrPreview = new JScrollPane(preview);
	private JButton fOkButton = new JButton("OK");
	private JButton fCancelButton = new JButton("Cancel");
	private JPanel firstPart = FormsUtils.build("p:g p",
			 									"[DialogBorder]0||" +
			 												  "1||" +
			 												  "2|" +
			 												  "3||" +
			 												  "4||" +
			 												  "5|" +
			 												  "6||" +
			 												  "7||" +
			 												  "8||" +
			 												  "9||" +
			 												  "A||" +
			 												  "B||" +
			 												  "C||" +
			 												  "D||" +
			 												  "E||" +
			 												  "F||" +
			 												  "G",
			 									FormsUtils.build("p ~ f:p:g ~ p",
			 													 "012",
			 													 "Filename:",fileTextField,browseButton).getPanel(),
			 									new Separator("Settings management"),
			 									FormsUtils.build("p:g p ~ p",
			 													 "_01",
			 													saveSettingsButton,loadSettingsButton).getPanel(),
			 									new Separator("CSV Type"),
			 									FormsUtils.build("p p ~ p p:g",
			 													 "012_",
			 													 "               ",normalButton,advancedButton).getPanel(),
			 									new Separator("String settings"),
			 									stringSettingsPanel,
			 									new Separator("Token of the 'empty' values"),
			 									FormsUtils.build("p ~ p ~ p ~ p ~ p",
	 													 "01234",
	 													 tNullButton,tNothingButton,tNAButton,
	 													 tnaButton,tOther).getPanel(),
	 											new Separator("Reading options"),
			 									FormsUtils.build("p p:g(0.2) p p:g(0.8)",
			 													 "0012|" +
			 													 "3333||" +
			 													 "456_",
			 													 contains,headerPatternLabel,headerPatternField,
			 													 notcontains,
			 													 ignore,ignoreField," rows").getPanel(),
			 									new Separator("Runs in the file"),
			 									FormsUtils.build("150dlu p:g(0.4) p:g(0.6)",
			 													 "000|" +
			 													 "12_|" +
			 													 "34_|" +
			 													 "555",
			 													 oneRunButton,
			 													 rprButton,rprField,
			 													 tickButton,tickField,
			 													 "       (Name or zero-based index of the column)").getPanel(),
			 									new Separator("Preview"),
			 									scrPreview,
			 									new Separator(""),
			 									FormsUtils.build("d ~ d",
			 													 "01|",
			 													 fOkButton,fCancelButton).getPanel(),CellConstraints.CENTER
			  ).getPanel();
	private JPanel secondPart = new JPanel(new BorderLayout());
	private JTextField modelField = new JTextField();
	private JTextField versionField = new JTextField("1");
	private JTextArea description = new JTextArea(5,50);
	private JScrollPane scrDescription = new JScrollPane(description);
	private JLabel message = new JLabel("(Invalid!)");
	private JLabel nrOfRuns = new JLabel("");
	private JLabel maxRec = new JLabel("");
	private JPanel topPanel = FormsUtils.build("p ~ p:g ~ p ~ p:g ~ p",
									   		   "[DialogBorder]01234||" +
									   				 	     "5666_|" +
									   				 	     "7666_||" +
									   				 	     "89AAB||" +
									   				 	     "CCCCC|",
									   				 	     "Model name:",modelField,"Version:",versionField,message,
									   				 	     "Batch description:",scrDescription,
									   				 	     " ",
									   				 	     "Number of runs:",nrOfRuns,"Max. number of records per run:",CellConstraints.RIGHT,maxRec,
									   				 	     new Separator("Parameters")
									   				 		 ).getPanel();
	private JTable paramTable = new JTable(); 
	private JScrollPane scrTable = new JScrollPane(paramTable);
	private JButton sOkButton = new JButton("OK");
	private JButton sCancelButton = new JButton("Cancel");
	private JPanel bottomPanel = FormsUtils.build("p:g p ~ p p:g",
												  "[DialogBorder]0000||" +
										                		"_12_",
										                		new Separator(""),
										                		sOkButton,sCancelButton
										  						).getPanel();
	
	//================================================================================
	// methods
	
	public CSVImportSettingsDialog(IImportPluginContext ctx, Properties settings) {
		this(ctx,settings,"CSV Import Settings");
		initialize();
		setLastSettings();
		Model[] models = ctx.getSelectedModels();
		if (models != null && models.length>0) {
			modelField.setText(models[0].getName());
			versionField.setText(models[0].getVersion());
			message.setText("(Exists)");
			sOkButton.setEnabled(true);
			selectedModel = true;
		}
	}
	
	//--------------------------------------------------------------------------------
	protected CSVImportSettingsDialog(IImportPluginContext ctx, Properties settings, String title) {
		super((ctx != null ? ctx.getAppWindow() : null),title,true);
		this.ctx = ctx;
		this.settings = settings;
		this.setName("dial_csvimport");
	}
	
	//--------------------------------------------------------------------------------
	private void initialize() {
		//set buttons action (part 1)
		browseButton.setActionCommand("BROWSE");
		saveSettingsButton.setActionCommand("SAVE_SETTINGS");
		loadSettingsButton.setActionCommand("LOAD_SETTINGS");
		tabBox.setActionCommand("NOTOTHER");
		semiColonBox.setActionCommand("NOTOTHER");
		commaBox.setActionCommand("NOTOTHER");
		spaceBox.setActionCommand("NOTOTHER");
		otherBox.setActionCommand("OTHER");
		tNullButton.setActionCommand("TNO");
		tNAButton.setActionCommand("TNO");
		tnaButton.setActionCommand("TNO");
		tNothingButton.setActionCommand("TNO");
		tOtherButton.setActionCommand("TO");
		contains.setActionCommand("CONTAINS");
		notcontains.setActionCommand("NOTCONTAINS");
		oneRunButton.setActionCommand("ONERUN");
		rprButton.setActionCommand("RPR");
		tickButton.setActionCommand("TICK");
		fOkButton.setActionCommand("F_OK");
		fCancelButton.setActionCommand("CANCEL");
		fCancelButton.setName("btn_csvimport_fcancel");
		normalButton.setActionCommand("NORMAL_CSV");
		advancedButton.setActionCommand("ADVANCED_CSV");
		
		//init (part 1)
		GUIUtils.createButtonGroup(normalButton,advancedButton);
		GUIUtils.createButtonGroup(tNullButton,tNAButton,tnaButton,tNothingButton,tOtherButton);
		GUIUtils.createButtonGroup(contains,notcontains);
		GUIUtils.createButtonGroup(oneRunButton,rprButton,tickButton);
		
		linePatternField.setBorder(null);
		linePatternField.setLineWrap(true);
		linePatternField.setWrapStyleWord(true);

		fileTextField.setText(lastFile.getAbsolutePath());
		fileTextField.addFocusListener(this);
		normalButton.setSelected(true);
		commaBox.setSelected(true);
		otherField.setEnabled(false);
		delimiterPanel.setBorder(BorderFactory.createTitledBorder("Delimiters"));
		commentField.addFocusListener(this);
		tNullButton.setSelected(true);
		tOtherField.setEnabled(false);
		contains.setSelected(true);
		headerPatternLabel.setVisible(false);
		headerPatternField.setVisible(false);
		ignore.addItemListener(this);
		ignoreField.setEnabled(false);
		ignoreField.setHorizontalAlignment(JTextField.TRAILING);
		ignoreField.addFocusListener(this);
		oneRunButton.setSelected(true);
		rprField.setEnabled(false);
		rprField.setHorizontalAlignment(JTextField.TRAILING);
		tickField.setEnabled(false);
		preview.setEditable(false);
		preview.setLineWrap(false);
		scrPreview.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrPreview.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		
		// set buttons action (part 2)
		sOkButton.setActionCommand("S_OK");
		sCancelButton.setActionCommand("CANCEL");
		sCancelButton.setName("btn_csvimport_scancel");

		
		GUIUtils.addActionListener(this,browseButton,saveSettingsButton,loadSettingsButton,tabBox,semiColonBox,commaBox,spaceBox,otherBox,
								   tNullButton,tNAButton,tnaButton,tNothingButton,tOtherButton,oneRunButton,rprButton,tickButton,fOkButton,fCancelButton,
								   sOkButton,sCancelButton,normalButton,advancedButton,contains,notcontains);

		// init (part 2)
		modelVersionBindings(modelField);
		modelField.setPreferredSize(new Dimension(150,20));
		modelField.setMaximumSize(new Dimension(150,20));
		modelVersionBindings(versionField);
		versionField.setPreferredSize(new Dimension(100,20));
		versionField.setMaximumSize(new Dimension(100,20));
		message.setFont(new Font("Dialog",Font.ITALIC,12));
		message.setPreferredSize(new Dimension(50,20));
		message.setMaximumSize(new Dimension(50,20));
		scrDescription.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrDescription.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		description.setBorder(null);
		description.setLineWrap(true);
		description.setWrapStyleWord(true);
		scrTable.setBorder(BorderFactory.createCompoundBorder(
						   BorderFactory.createEmptyBorder(0,10,0,10),
						   BorderFactory.createLineBorder(Color.BLACK)));
		scrTable.setPreferredSize(new Dimension(400,250));
		sOkButton.setEnabled(false);
		
		secondPart.add(topPanel, BorderLayout.NORTH);
		secondPart.add(scrTable, BorderLayout.CENTER);
		secondPart.add(bottomPanel, BorderLayout.SOUTH);
		
		stringSettingsPanel.add(normalSettingsPanel,NORMAL_PANEL);
		stringSettingsPanel.add(advancedSettingsPanel,ADVANCED_PANEL);
		
		content.add(firstPart,FIRST_PART);
		content.add(secondPart,SECOND_PART);
		
		final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(null);
		this.setContentPane(sp);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.pack();
		Dimension oldD = this.getPreferredSize();
		this.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
											oldD.height + sp.getHorizontalScrollBar().getHeight()));
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		oldD = this.getPreferredSize();
		final Dimension newD = GUIUtils.getPreferredSize(this);
		if (!oldD.equals(newD)) 
			this.setPreferredSize(newD);
		this.pack();
	}
	
	//--------------------------------------------------------------------------------
	public void start(String part) {
		CardLayout l = (CardLayout)content.getLayout();
		l.show(content,part);
		if (part.equals(SECOND_PART)) {
			initTableModel();
			initStatistics();
			if (selectedModel && parser.generatedParameterNames) 
				questionChangeColumnNames();
		} else updatePreview();
		if (!isVisible()) setVisible(true);
	}
	
	
	//--------------------------------------------------------------------------------
	static File[] openFileDialog(java.awt.Window parent, boolean multiSelection) {
		JFileChooser chooser = (lastFile == null) ? new JFileChooser(ai.aitia.meme.MEMEApp.getLastDir()) : new JFileChooser(lastFile);
		SimpleFileFilter filter = new SimpleFileFilter("CSV files (*.csv;*.txt;*.dat)");
		chooser.addChoosableFileFilter(filter);
		chooser.setFileFilter(filter);
		chooser.setName("filechooser_csvimport");
		//chooser.addChoosableFileFilter(new SimpleFileFilter("CSV files (*.csv;*.txt;*.dat)"));
		chooser.setMultiSelectionEnabled(multiSelection);
		int returnVal = chooser.showOpenDialog(parent);
		if (returnVal != JFileChooser.APPROVE_OPTION) return null;
		lastFile = multiSelection ? chooser.getSelectedFiles()[0] : chooser.getSelectedFile();
		ai.aitia.meme.MEMEApp.setLastDir(lastFile);
		return multiSelection ? chooser.getSelectedFiles() : new File[] { lastFile };
	}
	
	//-------------------------------------------------------------------------------
	private boolean is_valid(String text, boolean acceptZero) {
		Pattern p = (acceptZero ?  Pattern.compile("^[0-9]|[1-9][0-9]+$") :
								   Pattern.compile("^[1-9]|[1-9][0-9]+$"));
		Matcher m = p.matcher(text);
		return m.matches();
	}
	
	//--------------------------------------------------------------------------------
	private void initTableModel() {
		if (parser == null) throw new IllegalStateException();
		paramTable.setModel(new ParameterTableModel(parser));
		JComboBox combobox = new JComboBox();
		combobox.addItem("Input");
		combobox.addItem("Output");
		paramTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(combobox));
	}
	
	//--------------------------------------------------------------------------------
	private void initStatistics() {
		if (parser == null) return;
		nrOfRuns.setText(String.valueOf(parser.runs.size()));
//		nrOfRuns.setText(String.valueOf(parser.numberOfRuns));
		maxRec.setText(String.valueOf(parser.maxRowsPerRun));
	}
	
	//--------------------------------------------------------------------------------
	private void questionChangeColumnNames() {
		int result = ai.aitia.meme.MEMEApp.askUser(false,"Change column names","Do you want to replace the generated column names",
									               "with the column names of the existing model version?");
		if (result == 1) { // YES
			final String modelName = modelField.getText().trim();
			final String modelVer  = versionField.getText().trim();
			Columns[] allColumns = (Columns[])ai.aitia.meme.MEMEApp.LONG_OPERATION.executeNE("Collecting info...",null,new Callable<Object>() {

				public Object call() throws Exception {
					parser.model = ctx.getResultsDb().findModel(modelName,modelVer);
					if (parser.model == null) 
						throw new IllegalStateException();
					return ctx.getResultsDb().getModelColumns(parser.model.getModel_id());
				}
				
			});
			
			if (allColumns == null || allColumns.length < 2) 
				throw new IllegalStateException();
			TableModel model = paramTable.getModel();
			int iindex = 0, oindex = 0;
			for (int i=0;i<model.getRowCount();++i) {
				Parameter p = null;
				if (model.getValueAt(i,1).equals("Input")) p = allColumns[0].size() > iindex ? allColumns[0].get(iindex++) : null;
				else p = allColumns[1].size() > oindex ? allColumns[1].get(oindex++) : null;
				if (p != null) model.setValueAt(p.getName(),i,0);
			}
			parser.generatedParameterNames = false;
		}
	}
	
	//--------------------------------------------------------------------------------
	@Override
	public void dispose() {
		if (parser != null) parser.dispose();
		parser = null;
		super.dispose();
	}
	
	//-------------------------------------------------------------------------
	private boolean isModelValid(String modelName, String modelVer) {
		if (modelName == null) modelName = modelField.getText().trim();
		if (modelVer == null)  modelVer  = versionField.getText().trim();
		return (modelName.length() > 0 && modelVer.length() > 0);
	}
	
	//-------------------------------------------------------------------------
	private void modelVersionChanged(boolean edit) {
		if (edit) {
			sOkButton.setEnabled(false);
			scheduleReset = true;
		}
		else if (scheduleReset) {
			scheduleReset = false;
			String modelStatus;
			final String modelName = modelField.getText().trim();
			final String modelVer  = versionField.getText().trim();
			if (parser != null) {
				parser.model = (Model)ai.aitia.meme.MEMEApp.LONG_OPERATION.executeNE("Searching...",null,new Callable<Object>(){
					public Object call() throws Exception {
						return 	ctx.getResultsDb().findModel(modelName, modelVer); 
					}
				});
				if (parser.model != null) {
					modelStatus = "(Exists)";
					sOkButton.setEnabled(true);
					if (parser.generatedParameterNames) questionChangeColumnNames();
				}
				else if (isModelValid(modelName, modelVer)) {
					modelStatus = "(New)";
					sOkButton.setEnabled(true);
				}
				else {
					modelStatus = "(Invalid!)";
					sOkButton.setEnabled(false);
				}
				message.setText(modelStatus);
			}
		}
	}

	//--------------------------------------------------------------------------------
	private void modelVersionBindings(JTextField tf) {
		tf.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			 public void changedUpdate(DocumentEvent e)	{ modelVersionChanged(true); } 
			 public void insertUpdate(DocumentEvent e)		{ modelVersionChanged(true); }
			 public void removeUpdate(DocumentEvent e)		{ modelVersionChanged(true); }
		});
		tf.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) { modelVersionChanged(false); }
		});
		tf.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusLost(java.awt.event.FocusEvent e) { modelVersionChanged(false); }
		});
	}
	
	//--------------------------------------------------------------------------------
	private void updatePreview() {
		fOkButton.setEnabled(true);
		if (lastFile == null || !lastFile.exists() || lastFile.isDirectory()) {
			preview.setFont(new Font("Dialog",Font.ITALIC,16));
			preview.setText("Cannot find file!");
			fOkButton.setEnabled(false);
			return;
		}
		try {
			java.io.BufferedReader in = new java.io.BufferedReader(new java.io.FileReader(lastFile));
			int lineNo = 0;
			int ignoreNo = 0;
			if (ignore.isSelected()) {
				try {
					ignoreNo = Integer.parseInt(ignoreField.getText().trim());
				} catch (NumberFormatException e) {}
			}
			String commentStr = commentField.getText().trim();
			String line = null;
			String msg = "";
			int index = 0;
			do {
				line = in.readLine();
				if (line == null) break;
				if (ignore.isSelected() && index < ignoreNo) {
					index++;
					continue;
				}
				if (line.equals("") || line.startsWith(commentStr)) continue;
				msg += line + "\n";
				lineNo += 1;
			} while (line != null && lineNo != 5);
			in.close();
			preview.setFont(new Font("Dialog",Font.PLAIN,12));
			preview.setText(msg);
			preview.setCaretPosition(0);
		} catch (IOException e) {
			preview.setFont(new Font("Dialog",Font.ITALIC,16));
			preview.setText("Preview is unavailable!");
		}
	}
	
	//--------------------------------------------------------------------------------
	private void setLastSettings() {
		advancedButton.setSelected(Boolean.parseBoolean(settings.getProperty(CSVImportPlugin.IS_ADVANCED_CSV_TYPE,"false")));
		if (advancedButton.isSelected()) 
			advancedButton.doClick();
		String delimiter = settings.getProperty(CSVImportPlugin.DELIMITER);
		boolean hasDelimiter = false;
		if (delimiter != null) {
			selectDelimiter(delimiter);
			hasDelimiter = true;
		}
		delimiter = null;
		int index = 1;
		while (null != (delimiter = settings.getProperty(CSVImportPlugin.DELIMITER + index++))) { 
			selectDelimiter(delimiter);
			hasDelimiter = true;
		}
		if (!hasDelimiter)
			selectDelimiter(",");
		
		mergeBox.setSelected(Boolean.parseBoolean(settings.getProperty(CSVImportPlugin.MERGE,"false")));
		
		String quote = settings.getProperty(CSVImportPlugin.QUOTE,"\"");
		if (quote.equals("'")) quoteBox.setSelectedIndex(1);
		else quoteBox.setSelectedIndex(0);
		
		String comment = settings.getProperty(CSVImportPlugin.COMMENT,"#");
		commentField.setText(comment.trim());
		commentField2.setText(comment.trim());
		
		linePatternField.setText(settings.getProperty(CSVImportPlugin.LINE_PATTERN,""));
		
		String emptyToken = settings.getProperty(CSVImportPlugin.EMPTY_TOKEN,"null");
		if (emptyToken.equals("null")) tNullButton.setSelected(true);
		else if (emptyToken.equals("")) tNothingButton.setSelected(true);
		else if (emptyToken.equals("N\\A")) tNAButton.setSelected(true);
		else if (emptyToken.equals("na")) tnaButton.setSelected(true);
		else {
			tOtherButton.setSelected(true);
			tOtherField.setEnabled(true);
			tOtherField.setText(emptyToken.trim());
		}
		
		boolean hasHeader = true;
		try {
			hasHeader = Boolean.parseBoolean(settings.getProperty(CSVImportPlugin.HAS_COLUMN_NAMES,"true"));
		} catch (NumberFormatException e) {}
		if (!hasHeader) notcontains.setSelected(true);
		else contains.setSelected(true);

		headerPatternField.setText(settings.getProperty(CSVImportPlugin.HEADER_PATTERN,""));
		
		int ignoreNo = 0;
		try {
			ignoreNo = Integer.parseInt(settings.getProperty(CSVImportPlugin.NR_IGNORED_LINES,"0"));
		} catch (NumberFormatException e) {}
		if (ignoreNo != 0) {
			ignore.setSelected(true);
			ignoreField.setText(String.valueOf(ignoreNo));
		} else {
			ignore.setSelected(false);
		}
		
		int runs = ONE_RUN;
		try {
			runs = Integer.parseInt(settings.getProperty(CSVImportPlugin.RUNS,String.valueOf(ONE_RUN)));
		} catch (NumberFormatException e) {}
		if (runs == ONE_RUN) {
			oneRunButton.setSelected(true);
			rprField.setEnabled(false);
			rprField.setText("");
			tickField.setEnabled(false);
			tickField.setText("");
		}
		else if (runs == RECORD_PER_RUN) {
			rprButton.setSelected(true);
			rprField.setEnabled(true);
			try {
				int nrRec = Integer.parseInt(settings.getProperty(CSVImportPlugin.NR_RECORDS));
				rprField.setText(String.valueOf(nrRec));
			} catch (NumberFormatException e) {}
		} else if (runs == USE_TICK) {
			tickButton.setSelected(true);
			tickField.setEnabled(true);
			String tickName = settings.getProperty(CSVImportPlugin.TICK_COLUMN,"");
			tickField.setText(tickName.trim());
		}
	}
	
	//--------------------------------------------------------------------------------
	private void selectDelimiter(String delimiter) {
		if (delimiter.equals("\t")) tabBox.setSelected(true);
		else if (delimiter.equals(";")) semiColonBox.setSelected(true);
		else if (delimiter.equals(",")) commaBox.setSelected(true);
		else if (delimiter.equals(" ")) spaceBox.setSelected(true);
		else {
			otherBox.setSelected(true);
			otherField.setEnabled(true);
			otherField.setText(delimiter.trim());
		}
	}
	
	//--------------------------------------------------------------------------------
	/** Pre-condition: the member variables are updated. */
	protected void storeSettings() {
		settings.setProperty(CSVImportPlugin.IS_ADVANCED_CSV_TYPE,String.valueOf(isAdvancedCSVType));
		if (isAdvancedCSVType) 
			settings.setProperty(CSVImportPlugin.LINE_PATTERN,linePattern);
		else {
			int index = 1;
			for (String s : delimiters) 
				settings.setProperty(CSVImportPlugin.DELIMITER + index++,s);
			settings.setProperty(CSVImportPlugin.MERGE,String.valueOf(mergeConsecutive));
			settings.setProperty(CSVImportPlugin.QUOTE,this.quote);
		}
		settings.setProperty(CSVImportPlugin.COMMENT,this.comment);
		settings.setProperty(CSVImportPlugin.EMPTY_TOKEN,this.nullToken);
		settings.setProperty(CSVImportPlugin.HAS_COLUMN_NAMES,String.valueOf(this.containsHeader));
		if (this.containsHeader && isAdvancedCSVType) 
			settings.setProperty(CSVImportPlugin.HEADER_PATTERN,headerPattern);
		if (this.isIgnore)
			settings.setProperty(CSVImportPlugin.NR_IGNORED_LINES,String.valueOf(this.ignoreNumber));
		else 
			settings.setProperty(CSVImportPlugin.NR_IGNORED_LINES,"0");
		settings.setProperty(CSVImportPlugin.RUNS,String.valueOf(this.runStrategy));
		if (this.runStrategy == RECORD_PER_RUN)
			settings.setProperty(CSVImportPlugin.NR_RECORDS,String.valueOf(this.recordsPerRun));
		else if (this.runStrategy == USE_TICK)
			settings.setProperty(CSVImportPlugin.TICK_COLUMN,this.tickColumn);
	}
	
	//--------------------------------------------------------------------------------
	/** Returns <code>false</code> if it has found error(s). */ 
	@SuppressWarnings("unchecked")
	private boolean updateVariables() {
		// CSV type
		isAdvancedCSVType = advancedButton.isSelected();
		if (isAdvancedCSVType) {
			// line pattern
			linePattern = linePatternField.getText();
			if (linePattern == null || "".equals(linePattern.trim())) {
					ai.aitia.meme.MEMEApp.userAlert("Line pattern is empty.");
					return false;
			}
			if (linePattern.endsWith("\n")) 
				linePattern = linePattern.substring(0,linePattern.length()-1);
		} else {
			// delimiter
			delimiters = new ArrayList<String>();
			if (commaBox.isSelected()) delimiters.add(",");
			if (tabBox.isSelected()) delimiters.add("\t");
			if (semiColonBox.isSelected()) delimiters.add(";");
			if (spaceBox.isSelected()) delimiters.add(" ");
			if (otherBox.isSelected()) delimiters.add(otherField.getText());
			if (delimiters.size() == 0) {
				ai.aitia.meme.MEMEApp.userAlert("There is no selected delimiter.");
				return false;
			}
		
			// merge
			mergeConsecutive = mergeBox.isSelected();
		
			// quote
			quote = ((Item<String>)quoteBox.getSelectedItem()).getItem();
			if (delimiters.contains(quote)) {
				ai.aitia.meme.MEMEApp.userAlert("Invalid combination","A delimiter and the qoute string cannot be the same.");
				return false;
			} else if (containsAny(quote)) {
				ai.aitia.meme.MEMEApp.userAlert("Invalid combination","A delimiter cannot contain the qoute string.");
				return false;
			}
		}
		
		// comment
		comment = isAdvancedCSVType ? commentField2.getText() : commentField.getText();
		if (!isAdvancedCSVType && delimiters.contains(comment)) {
			ai.aitia.meme.MEMEApp.userAlert("Invalid combination","A delimiter and the comment character cannot be the same.");
			return false;
		} else if (!isAdvancedCSVType && containsAny(comment)) {
			ai.aitia.meme.MEMEApp.userAlert("Invalid combination","A delimiter cannot contain the comment character.");
			return false;
		}
		
		// nullString
		if (tNullButton.isSelected()) nullToken = "null";
		else if (tNothingButton.isSelected()) nullToken = "";
		else if (tNAButton.isSelected()) nullToken = "N\\A";
		else if (tnaButton.isSelected()) nullToken = "na";
		else nullToken = tOtherField.getText();
		if (!isAdvancedCSVType && mergeConsecutive && nullToken == "") {
			ai.aitia.meme.MEMEApp.userAlert("Invalid combination","The 'Merge consecutive delimiters' option cannot be used if the 'null' token is the empty string.");
			return false;
		}
		
		// header
		containsHeader = contains.isSelected();
		
		if (isAdvancedCSVType && containsHeader) {
			headerPattern = headerPatternField.getText();
			if (headerPattern == null || "".equals(headerPattern.trim())) {
				ai.aitia.meme.MEMEApp.userAlert("Header pattern is empty.");
				return false;
			}
		}
		
		// ignore
		isIgnore = ignore.isSelected();
		if (isIgnore) {
			if (is_valid(ignoreField.getText(),true))
				ignoreNumber = Integer.parseInt(ignoreField.getText());
			else {
				ai.aitia.meme.MEMEApp.userAlert("Warning","The number of ignored rows must be a nonnegative integer.");
				return false;
			}
		}
		
		// run strategy
		if (oneRunButton.isSelected()) runStrategy = ONE_RUN;
		else if (rprButton.isSelected()) {
			runStrategy = RECORD_PER_RUN;
			if (is_valid(rprField.getText(),false))
				recordsPerRun = Integer.parseInt(rprField.getText());
			else {
				ai.aitia.meme.MEMEApp.userAlert("Warning","The number of records per run must be a positive integer.");
				return false;
			}
		} else {
			runStrategy = USE_TICK;
			if (!containsHeader && !is_valid(tickField.getText(),true)) {
				ai.aitia.meme.MEMEApp.userAlert("Warning","If the file not contains the column names",
												"then you must identifiy the 'tick' column with",
												"its zero-based index.");
				return false;
			}
			tickColumn = tickField.getText();
		}
		return true;
	}
	
	//--------------------------------------------------------------------------------
	private boolean containsAny(String text) {
		for (String s : delimiters) {
			if (s.contains(text)) return true; 
		}
		return false;
	}
	
	//================================================================================
	// interface implementations

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("BROWSE")) {
			openFileDialog(this,false);
			fileTextField.setText(lastFile.getAbsolutePath());
			updatePreview();
		} else if (command.equals("SAVE_SETTINGS")) {
			if (!updateVariables()) return;
			JFileChooser chooser = new JFileChooser(ai.aitia.meme.MEMEApp.getLastDir());
			chooser.setAcceptAllFileFilterUsed(false);
			chooser.addChoosableFileFilter(new SimpleFileFilter("CSV settings (*.csvsettings)"));
			File f = null;
			do {
				int resultVal = chooser.showSaveDialog(this);
				if (resultVal == JFileChooser.APPROVE_OPTION) {
					f = chooser.getSelectedFile();
					if (!f.getName().endsWith(".csvsettings"))
						f = new File(f.getPath() + ".csvsettings");
					if (f.exists()) {
						int result = ai.aitia.meme.MEMEApp.askUser(false,"Override comfirmation",
													               f.getName() + "already exists.",
													               "Do you want to replace it?");
						if (result != 1) f = null;
						
					}
				} else return;
			} while (f == null);
			ai.aitia.meme.MEMEApp.setLastDir(f);
			storeSettings();
			try {
				FileOutputStream os = new FileOutputStream(f);
				settings.store(os,"CSV Import settings");
				os.flush();
				os.close();
			} catch (IOException e1) {
				ai.aitia.meme.MEMEApp.userAlert("Save failed.");
				ai.aitia.meme.MEMEApp.logException(e1);
			}
		} else if (command.equals("LOAD_SETTINGS")) {
			JFileChooser chooser = new JFileChooser(ai.aitia.meme.MEMEApp.getLastDir());
			chooser.setAcceptAllFileFilterUsed(false);
			chooser.addChoosableFileFilter(new SimpleFileFilter("CSV settings (*.csvsettings)"));
			int resultVal = chooser.showOpenDialog(this);
			if (resultVal == JFileChooser.APPROVE_OPTION) {
				File f = chooser.getSelectedFile();
				try {
					FileInputStream is = new FileInputStream(f);
					settings.load(is);
					is.close();
					setLastSettings();
				} catch (IOException e1) {
					ai.aitia.meme.MEMEApp.userAlert("Load failed.");
					ai.aitia.meme.MEMEApp.logException(e1);
				}
			}
		} else if (command.equals("NOTOTHER")) {
			otherField.setText("");
			otherField.setEnabled(false);
		} else if (command.equals("OTHER")) {
			otherField.setEnabled(true);
			otherField.grabFocus();
		} else if (command.equals("TNO")) {
			tOtherField.setText("");
			tOtherField.setEnabled(false);
		} else if (command.equals("TO")) {
			tOtherField.setEnabled(true);
			tOtherField.grabFocus();
		} else if (command.equals("ONERUN")) {
			rprField.setEnabled(false);
			rprField.setText("");
			tickField.setEnabled(false);
			tickField.setText("");
		} else if (command.equals("RPR")) {
			rprField.setEnabled(true);
			rprField.grabFocus();
			tickField.setEnabled(false);
			tickField.setText("");
		} else if (command.equals("TICK")) {
			tickField.setEnabled(true);
			tickField.grabFocus();
			rprField.setEnabled(false);
			rprField.setText("");
		} else if (command.equals("CANCEL")) {
			setVisible(false);
			dispose();
		} else if (command.equals("F_OK")) {
			if (!updateVariables()) return;
			parser = new CSVResultParser(this);
//			parser = new BufferedCSVResultParser(this);
			GUIUtils.setBusy(this, true);
			try {
				ai.aitia.meme.MEMEApp.LONG_OPERATION.execute("Loading...", new Callable<Object>() {
					// [Model thread]
					public Object call() throws Exception {
						parser.readFile();
						return null;
					}

				});
				GUIUtils.setBusy(this,false);
				if (1 == parser.warnings)
					ai.aitia.meme.MEMEApp.userAlert(String.format("There is a warning in the file. %s",MEMEApp.seeTheErrorLog("%s")));
				else if (1 < parser.warnings) 
					ai.aitia.meme.MEMEApp.userAlert(String.format("There are %d warnings in the file. %s",parser.warnings,MEMEApp.seeTheErrorLog("%s")));
				start(SECOND_PART);
			} catch (Exception e1) {
				GUIUtils.setBusy(this,false);
				ai.aitia.meme.MEMEApp.logExceptionCallStack("during the CSV file parsing",e1);
				ai.aitia.meme.MEMEApp.userErrors("Error while reading the file",Utils.getLocalizedMessage(e1)+".");
			}
		} else if (command.equals("S_OK")) {
			storeSettings();
			ai.aitia.meme.MEMEApp.LONG_OPERATION.begin("Writing to database...", new LongRunnable() {
				String model = modelField.getText().trim();
				String version = versionField.getText().trim();
				String batchDesc = description.getText().trim();
				String[] types = ((ParameterTableModel)paramTable.getModel()).types;
				
				@Override
				public void trun() throws Exception {
					parser.write(ctx.getResultsDb(),model,version,batchDesc,types);
				}
				
				@Override
				public void finished() {
					if (getReq().getError() != null) {
						ai.aitia.meme.MEMEApp.logException(getReq().getError());
					}
					setVisible(false);
					dispose();
				}
				
			});
		} else if (command.equals("CONTAINS")) {
			headerPatternLabel.setEnabled(true);
			headerPatternField.setEnabled(true);
			if (headerPatternField.isVisible())
				headerPatternField.grabFocus();
		} else if (command.equals("NOTCONTAINS")) {
			headerPatternLabel.setEnabled(false);
			headerPatternField.setEnabled(false);
		} else if (command.equals("NORMAL_CSV")) {
			CardLayout l = (CardLayout)stringSettingsPanel.getLayout();
			l.show(stringSettingsPanel,NORMAL_PANEL);
			headerPatternLabel.setVisible(false);
			headerPatternField.setVisible(false);
		} else if (command.equals("ADVANCED_CSV")) {
			CardLayout l = (CardLayout)stringSettingsPanel.getLayout();
			l.show(stringSettingsPanel,ADVANCED_PANEL);
			headerPatternLabel.setVisible(true);
			headerPatternField.setVisible(true);
		}
	}

	//--------------------------------------------------------------------------------
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			ignoreField.setEnabled(true);
			ignoreField.grabFocus();
		} else {
			ignoreField.setEnabled(false);
			ignoreField.setText("");
			updatePreview();
		}
	}

	//--------------------------------------------------------------------------------
	public void focusGained(FocusEvent e) {}

	//--------------------------------------------------------------------------------
	public void focusLost(FocusEvent e) {
		if (e.getSource().equals(commentField)) {
			try {
				if (commentField.getText().length() == 0) commentField.setText("#");
				else if (commentField.getText().length() > 1) commentField.setText(commentField.getText(0,1));
				updatePreview();
			} catch (BadLocationException e1) {}
		} else if (e.getSource().equals(ignoreField)){
			updatePreview();
		} else {
			lastFile = new File(fileTextField.getText().trim());
			updatePreview();
		}
	}
}
