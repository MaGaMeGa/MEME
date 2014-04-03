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

import java.awt.CardLayout;
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
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.gui.SimpleFileFilter;
import ai.aitia.meme.pluginmanager.IImportPluginContext;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.FormsUtils.Separator;
import ai.aitia.visu.globalhandlers.UserBreakException;

import com.jgoodies.forms.layout.CellConstraints;

public class CSVMultiImportSettingsDialog extends CSVImportSettingsDialog implements ActionListener,
																                     ItemListener,
																                     FocusListener {
	
	private static final long serialVersionUID = 8930698118410226529L;
	
	//==============================================================================
	// nested classes

	//------------------------------------------------------------------------------
	/** In the case of 'Every file a new version' we not allow to delete the %
	 *  character from the version string. */
	private class VersionTextFieldFilter extends javax.swing.text.DocumentFilter {
		<T extends javax.swing.text.JTextComponent> T install(T comp) { 
			((javax.swing.text.AbstractDocument)comp.getDocument()).setDocumentFilter(this);
			return comp;
		}
		@Override 
		public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
			if (versionButton.isSelected()) {
				int len = fb.getDocument().getLength();
				StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, len));
				sb.delete(offset, offset+length);
				int i = sb.indexOf("%");
				if (i < 0) {
					fb.insertString(len, "%", null);
				}
			}
			super.remove(fb, offset, length);
		}
		@Override
		public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
			if (versionButton.isSelected()) {
				int len = fb.getDocument().getLength();
				StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, len));
				sb.replace(offset, offset+length, text);
				int i = sb.indexOf("%");
				if (i < 0) {
					fb.insertString(len, "%", null);
				}
			}
			super.replace(fb, offset, length, text, attrs);
		}
	}
	
	//================================================================================
	// variables
	
	private boolean scheduleReset = false;
	private File[] files = null;
	
	//================================================================================
	// GUI components
	
	private JTextPane messagePane = new JTextPane();
	private JScrollPane messageScrollPane = new JScrollPane(messagePane,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	private JRadioButton batchButton = new JRadioButton(" Every file a new batch ",true);
	private JRadioButton versionButton = new JRadioButton(" Every file a new version ");
	@SuppressWarnings("unused")
	private ButtonGroup group = GUIUtils.createButtonGroup(batchButton,versionButton);
	private JTextField modelField = new JTextField();
	private JTextField versionField = new JTextField();
	private JLabel statusLabel = new JLabel(" (Invalid!) ");
	private JPanel strategyPanel = FormsUtils.build("p ~ p:g ~ p ~ p:g ~ p",
											   "00000 p||" +
											   "12345||" +
											   "66666||" +
											   "7788_",
											   messageScrollPane,
											   " Model name: ",modelField," Version: ",versionField,statusLabel,
											   new Separator("Import strategy"),
											   batchButton,versionButton).getPanel();
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
	private JPanel content = FormsUtils.build("p:g p",
			 								  "[DialogBorder]0||" +
			 												"1||" +
			 												"2||" +
			 												"3|" +
			 												"4||" +
			 												"5||" +
			 												"6|" +
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
			 									strategyPanel,
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
			 									new Separator("Preview of the first file"),
			 									scrPreview,
			 									new Separator(""),
			 									FormsUtils.build("d ~ d",
			 													 "01|",
			 													 fOkButton,fCancelButton).getPanel(),CellConstraints.CENTER
			  ).getPanel();
	
	//================================================================================
	// methods
	
	public CSVMultiImportSettingsDialog(IImportPluginContext ctx, Properties settings) {
		super(ctx,settings,"CSV Multi Import Settings");
		initialize();
		setLastSettings();
	}
	
	//--------------------------------------------------------------------------------
	private void initialize() {

		GUIUtils.createButtonGroup(tNullButton,tNAButton,tnaButton,tNothingButton,tOtherButton);
		GUIUtils.createButtonGroup(contains,notcontains);
		GUIUtils.createButtonGroup(oneRunButton,rprButton,tickButton);
		GUIUtils.createButtonGroup(normalButton,advancedButton);

		// message pane
		messagePane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		messagePane.setFont(new Font("Dialog", Font.BOLD, 12));
		messagePane.setEditable(false);
		messagePane.setPreferredSize(new Dimension(400,70));
		messagePane.setMaximumSize(new Dimension(600,70));
		
		// message scrollpane
		messageScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		messageScrollPane.setViewportBorder(BorderFactory.createEmptyBorder(8,15,8,15));
		messageScrollPane.setBackground(messagePane.getBackground());
		messageScrollPane.setPreferredSize(new Dimension(600,100));
		
		// model name and version
		modelVersionBindings(modelField);
		modelField.setPreferredSize(new Dimension(150,20));
		modelField.setMaximumSize(new Dimension(150,20));
		modelVersionBindings(versionField);
		versionField.setPreferredSize(new Dimension(100,20));
		versionField.setMaximumSize(new Dimension(100,20));
		new VersionTextFieldFilter().install(versionField);
		
		// radiobuttons
		batchButton.setActionCommand("BATCH_BUTTON");
		versionButton.setActionCommand("VERSION_BUTTON");
		
		// status label
		statusLabel.setFont(new Font("Dialog",Font.ITALIC,12));
		statusLabel.setPreferredSize(new Dimension(60,20));
		statusLabel.setMaximumSize(new Dimension(60,20));
		
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
		oneRunButton.setActionCommand("ONERUN");
		rprButton.setActionCommand("RPR");
		tickButton.setActionCommand("TICK");
		fOkButton.setActionCommand("F_OK");
		fCancelButton.setActionCommand("CANCEL");
		contains.setActionCommand("CONTAINS");
		notcontains.setActionCommand("NOTCONTAINS");
		normalButton.setActionCommand("NORMAL_CSV");
		advancedButton.setActionCommand("ADVANCED_CSV");

		fOkButton.setEnabled(false);
		commaBox.setSelected(true);
		otherField.setEnabled(false);
		delimiterPanel.setBorder(BorderFactory.createTitledBorder("Delimiters"));
		commentField.addFocusListener(this);
		tNullButton.setSelected(true);
		tOtherField.setEnabled(false);
		contains.setSelected(true);
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
		
		linePatternField.setBorder(null);
		linePatternField.setLineWrap(true);
		linePatternField.setWrapStyleWord(true);
		normalButton.setSelected(true);
		headerPatternLabel.setVisible(false);
		headerPatternField.setVisible(false);
		
		GUIUtils.addActionListener(this,saveSettingsButton,loadSettingsButton,tabBox,semiColonBox,commaBox,spaceBox,otherBox,
								   tNullButton,tNAButton,tnaButton,tNothingButton,tOtherButton,oneRunButton,rprButton,tickButton,fOkButton,
								   fCancelButton,batchButton,versionButton,normalButton,advancedButton,contains,notcontains);

		stringSettingsPanel.add(normalSettingsPanel,NORMAL_PANEL);
		stringSettingsPanel.add(advancedSettingsPanel,ADVANCED_PANEL);
		
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
	public void start(File[] files) {
		if (files == null || files.length < 2)
			throw new IllegalArgumentException();
		this.files = files;
		updatePreview();
		this.setVisible(true);
	}
	
	//-------------------------------------------------------------------------------
	private boolean is_valid(String text, boolean acceptZero) {
		Pattern p = (acceptZero ?  Pattern.compile("^[0-9]|[1-9][0-9]+$") :
								   Pattern.compile("^[1-9]|[1-9][0-9]+$"));
		Matcher m = p.matcher(text);
		return m.matches();
	}
	
	
	//-------------------------------------------------------------------------
	// MYTODO: kell ez?
	private boolean isModelValid(String modelName, String modelVer) {
		if (modelName == null) modelName = modelField.getText().trim();
		if (modelVer == null)  modelVer  = versionField.getText().trim();
		return (modelName.length() > 0 && modelVer.length() > 0);
	}
	
	//-------------------------------------------------------------------------
	// MYTODO: jav�tani
	private void modelVersionChanged(boolean edit) {
		if (edit) {
			fOkButton.setEnabled(false);
			scheduleReset = true;
		}
		else if (scheduleReset) {
			scheduleReset = false;
			String modelStatus;
			final String modelName = modelField.getText().trim();
			final String modelVer  = versionField.getText().trim();
			Model model = null;
			if (!modelName.equals("") && !modelVer.equals("")) {
				GUIUtils.setBusy(this,true);
				model = (Model)ai.aitia.meme.MEMEApp.LONG_OPERATION.executeNE("Searching...",null,new Callable<Object>(){
					public Object call() throws Exception {
						if (batchButton.isSelected()) return ctx.getResultsDb().findModel(modelName, modelVer);
						else {
							for (int i=0;i<files.length;++i) {
								String ver_str = modelVer.replace("%",String.valueOf(i));
								Model temp = ctx.getResultsDb().findModel(modelName,ver_str);
								// if it found at least one model, it displays 'Exists' status.
								if (temp != null) return temp;
							}
							return null;
						}
					}
				});
				GUIUtils.setBusy(this,false);
			}
			if (model != null) {
				modelStatus = " (Exists) ";
				fOkButton.setEnabled(true);
			}
			else if (isModelValid(modelName, modelVer)) { 
				// if modelVersion as pattern is valid, all version string will
				// be valid, too.
				modelStatus = " (New) ";
				fOkButton.setEnabled(true);
			}
			else {
				modelStatus = " (Invalid!) ";
				fOkButton.setEnabled(false);
			}
			statusLabel.setText(modelStatus);
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
//		fOkButton.setEnabled(true);
		modelVersionChanged(false);
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

	//--------------------------------------------------------------------------------
	private void disableWidgets() {
		fOkButton.setEnabled(false);
		batchButton.setEnabled(false);
		versionButton.setEnabled(false);
		modelField.setEnabled(false);
		versionField.setEnabled(false);
		saveSettingsButton.setEnabled(false);
		loadSettingsButton.setEnabled(false);
		tabBox.setEnabled(false);
		semiColonBox.setEnabled(false);
		commaBox.setEnabled(false);
		spaceBox.setEnabled(false);
		otherBox.setEnabled(false);
		otherField.setEnabled(false);
		mergeBox.setEnabled(false);
		quoteBox.setEnabled(false);
		commentField.setEnabled(false);
		tNullButton.setEnabled(false);
		tNothingButton.setEnabled(false);
		tNAButton.setEnabled(false);
		tnaButton.setEnabled(false);
		tOtherButton.setEnabled(false);
		tOtherField.setEnabled(false);
		contains.setEnabled(false);
		notcontains.setEnabled(false);
		ignore.setEnabled(false);
		ignoreField.setEnabled(false);
		oneRunButton.setEnabled(false);
		rprButton.setEnabled(false);
		rprField.setEnabled(false);
		tickButton.setEnabled(false);
		tickField.setEnabled(false);
		fCancelButton.setText("Close");
		fCancelButton.grabFocus();
	}

	//================================================================================
	// interface implementations

	@Override
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("SAVE_SETTINGS")) {
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
		} else if (command.equals("BATCH_BUTTON")) {
			if (versionField.getText().endsWith("%")) {
				messagePane.setText("");
				String text = versionField.getText().substring(0,versionField.getText().length()-1);
				versionField.setText(text);
				modelVersionChanged(false);
			}
		} else if (command.equals("VERSION_BUTTON")) {
			if (versionField.getText().indexOf('%') == -1) {
				messagePane.setText("In the version field there is a % character. This " +
									"character will be replaced by the zero-based index " +
									"of the actual file.");		
				String text = versionField.getText() + "%";
				versionField.setText(text);
				modelVersionChanged(false);
			}
		} else if (command.equals("CANCEL")) {
			setVisible(false);
			System.gc();
		} else if (command.equals("F_OK")) {
			if (!updateVariables()) return;
			storeSettings();
			int errorNumber = 0;
			boolean interrupted = false;
			messagePane.setText("");
			for (int i=0;i<files.length;++i) {
				System.gc();
				String text = messagePane.getText();
				if (!text.equals("")) text += "\n";
				if (MEMEApp.LONG_OPERATION.isUserBreak()) {
					interrupted = true;
					break;
				}
				text += String.format("Completed %d out of %d. Actual file: %s",i,files.length,files[i].getName());
				messagePane.setText(text);
				final CSVResultParser parser = new CSVResultParser(this,files[i]);
				Throwable t = null;
				try {
					t = (Throwable) MEMEApp.LONG_OPERATION.execute("Loading...", new Callable<Object>() {
						public Object call() {
							try {
								parser.readFile();
								return null;
							} catch (Exception e) {
								return e;
							}
						}
					});
				} catch (Exception e1) {};
				text = messagePane.getText();
				if (t != null) {
					if (t instanceof UserBreakException) {
						interrupted = true;
						break;
					} else {
						text += "\nERROR: " + t.getLocalizedMessage() + "\nIgnoring file: " + files[i].getName();
						messagePane.setText(text);
						errorNumber += 1;
						continue;
					}
				} else if (1 == parser.warnings) {
					text += String.format("\nThere is a warning in the file %s. %s",files[i].getName(),MEMEApp.seeTheErrorLog("%s"));
					messagePane.setText(text);
				} else if (1 < parser.warnings) { 
						text += String.format("\nThere are %d warnings in the file %s. %s",parser.warnings,files[i].getName(),MEMEApp.seeTheErrorLog("%s"));
						messagePane.setText(text);
				}
				try {
					final int nr = i;
					final String[] types = new String[parser.parameters.size()];
					for (int j = 0;j < types.length;++j)
						types[j] = (parser.inputStatus[j] == parser.ONE_RUN_CONSTANT ? "Input" : "Output");
					t = (Throwable)MEMEApp.LONG_OPERATION.execute("Writing to database...", new Callable<Object>() {
						String model = modelField.getText().trim();
						String version = (batchButton.isSelected()) ? versionField.getText().trim() 
																    : versionField.getText().trim().replace("%",String.valueOf(nr));

						public Object call() {
							try {
								parser.write(ctx.getResultsDb(),model,version,null,types);
								return null;
							} catch (Exception e) {
								return e;
							}
						}
					});
					if (t != null) {
						if (t instanceof UserBreakException) {
							interrupted = true;
							break;
						} else {
							MEMEApp.logException("CSVMultiImportSettingsDialog.okButton", t);
							text = messagePane.getText();
							text += "\nError while writing to database: " + Utils.getLocalizedMessage(t);
							text += "\nFile: " + files[i].getName();
							messagePane.setText(text);
							errorNumber += 1;
						}
					}
				} catch (Exception e1) {}
				
			}
			GUIUtils.setBusy(this,false);
			String text = messagePane.getText();
			if (interrupted)
				text += "\nLoading is interrupted by the user.";
			else
				text += String.format("\nCompleted %d out of %d. Done.",files.length,files.length);
			if (errorNumber != 0)
				text += String.format("\nNumber of unparseable files: %d",errorNumber);
			messagePane.setText(text);
			disableWidgets();			
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
	@Override
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
	@Override public void focusGained(FocusEvent e) {}

	//--------------------------------------------------------------------------------
	@Override
	public void focusLost(FocusEvent e) {
		if (e.getSource().equals(commentField)) {
			try {
				if (commentField.getText().length() == 0) commentField.setText("#");
				else if (commentField.getText().length() > 1) commentField.setText(commentField.getText(0,1));
				updatePreview();
			} catch (BadLocationException e1) {}
		} else if (e.getSource().equals(ignoreField)){
			updatePreview();
		} 
	}
}
