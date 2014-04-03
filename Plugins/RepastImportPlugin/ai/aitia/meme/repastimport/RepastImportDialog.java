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
package ai.aitia.meme.repastimport;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.events.Event;
import ai.aitia.meme.gui.SimpleFileFilter;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.paramsweep.utils.SettingsFileFilter;
import ai.aitia.meme.pluginmanager.IImportPluginContext;
import ai.aitia.meme.pluginmanager.Parameter;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.XMLUtils;


class RepastImportDialog extends JDialog {

	//=========================================================================
	// Static members
	
	private static final long serialVersionUID = -512595552734708906L;
	static final String ONE_RUN_CONSTANT = "<<varies between runs>>";
	static final float TABLE_COLUMNS_WIDTH_FACTOR = 1.1f;
	static File lastFile = null;

	//=========================================================================
	// Nested types

	private static enum ErrorType {
		WARNING, ERROR, FATAL 
	};
	private static enum InputParStatus {
		MISSING, FIXED, OTHERPAR 
	};
	private static class InputPar {
		boolean			isNew;
		InputParStatus	status;
		String			name;
		String			value;
		InputPar(boolean w, InputParStatus s, String n, String v) { isNew=w; status=s; name=n; value=v; } 
	}
	enum ColNames { Name, Status, Value };
	
	//-------------------------------------------------------------------------
	class InputParsTableModel extends AbstractTableModel {
		private static final long serialVersionUID = -7810806668082971447L;
		ArrayList<InputPar>	rows = new ArrayList<InputPar>();
		int					otherParBegin = 0;

	    @Override public String	getColumnName(int col) { return ColNames.values()[col].name(); }
	    public int		getColumnCount()			{ return ColNames.values().length; }
	    public int		getRowCount()				{ return otherParBegin + parser.inputIndex + 1; }
	    @Override public boolean isCellEditable(int row, int col) { return false; }
	    @Override public void	setValueAt(Object value, int row, int col) {}

	    public Object getValueAt(int row, int col) {
	    	String ans = "";
	    	InputPar r = rows.get(row);
	    	switch (ColNames.values()[col]) {
		    	case Name :
	    			ans = r.name;
	    			break;
	    		case Status :
	    			if (r.isNew) ans = "new";
	    			else if (r.status == InputParStatus.MISSING) ans = "missing";
	    			else if (r.status == InputParStatus.FIXED) ans = "const.";
	    			break;
	    		case Value :
	    			ans = r.value;
	    			break;
	    	}
	    	if (r.isNew || r.status == InputParStatus.MISSING)
	    		ans = Utils.htmlPage("<b>" + Utils.htmlQuote(ans) + "</b>"); 
//	    	else if (r.status == InputParStatus.MISSING)
//	    		ans = "<html><font color=gray><em>" + ans + "</em></font>";

	    	return ans;
	    }
	}
	
	//-------------------------------------------------------------------------
	static class DelayedUpdate extends Event<RepastImportDialog, Void> {
		DelayedUpdate()		{ super("controlSizes", RepastImportDialog.class, null); }
		void fireLater()	{ super.fireLater(null); }
	}

	//=========================================================================
	// Variables
	
	private IImportPluginContext	app = null;
	private String 					resultType = null;
	private RepastResultParser		parser = null;  //  @jve:decl-index=0:
	private ErrorType				errorStatus = null;
	private InputParsTableModel		inputPars = null;  //  @jve:decl-index=0:visual-constraint="787,155"
	private	 ArrayList<Parameter>	existingOutputPars = null;  //  @jve:decl-index=0:
	private String					lastmsg = null;
	private boolean				scheduleReset = false;
	private boolean				repack = false;
	private DelayedUpdate			delayedUpdate = null;
	private int					textFieldColumnWidth = 1;
	
	private boolean simphony = false;

	
	//=========================================================================
	// GUI components
	
	private JPanel jContentPane = null;  //  @jve:decl-index=0:visual-constraint="10,10"
	private JLabel jLabel = null;
	private JTextField jFilenameTextField = null;
	private JButton jFilenameButton = null;
	private JScrollPane jScrollPane1 = null;
	private JTextPane jProblemsTextPane = null;
	private JPanel jPanel2 = null;
	private JButton jOkButton = null;
	private JButton jCancelButton = null;
	private JScrollPane jScrollPane = null;
	private JTable jInputParsTable = null;
	private JPanel jPanel1 = null;
	private JButton jMoveToOutputButton = null;
	private JButton jMoveToInputButton = null;
	private JScrollPane jScrollPane2 = null;
	private JTextPane jOutputParsTextPane = null;
	private JPanel jPanel3 = null;
	private JLabel jLabel1 = null;
	private JTextField jModelTextField = null;
	private JLabel jLabel2 = null;
	private JTextField jVersionTextField = null;
	private JPanel jPanel4 = null;
	private JLabel jStatisticsLabel = null;
	private JLabel jModelStatusLabel = null;
	private JLabel jLabel3 = null;
	private JTextArea jBatchDescTextArea = null;
	private JScrollPane jBatchDescScrollPane = null;
	private JCheckBox jIntelliSweepCheckBox = null;
	private JLabel settingsLabel1 = null;
	private JTextField settingsFileTextField = null;
	private JButton settingsBrowseButton = null;
	private JScrollPane sp = null;

	
	//-------------------------------------------------------------------------
	//	Ctor for GUI builder
	private RepastImportDialog() {
		this(null,"RepastJ",false);
	}
	
	//----------------------------------------------------------------------------------------------------
	public RepastImportDialog(IImportPluginContext app, String resultType) {
		this(app,resultType,false);
	}

	//-------------------------------------------------------------------------
	public RepastImportDialog(IImportPluginContext app, String resultType, boolean simphony) {
		super((app != null ? app.getAppWindow() : null),resultType  + " Import Settings",true);
		this.setName("dial_repastimport");
		this.app = app;
		this.resultType = resultType;
		this.simphony = simphony;
		initialize();
	}

	//-------------------------------------------------------------------------
	private void initialize() {
		getJContentPane().setPreferredSize(new Dimension(600,500));
		sp = new JScrollPane(getJContentPane(),JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		sp.setBorder(null);
		this.setContentPane(sp);
		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.pack();
		Dimension oldD = this.getPreferredSize();
		this.setPreferredSize(new Dimension(oldD.width + 15,oldD.height + 15));
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		oldD = this.getPreferredSize();
		final Dimension newD = GUIUtils.getPreferredSize(this);
		if (!oldD.equals(newD)) 
			this.setPreferredSize(newD);
		this.pack();
	}


	//-------------------------------------------------------------------------
	static File[] openFileDialog(java.awt.Frame parent, String resultType, boolean multiSelection) {
	    JFileChooser chooser = (lastFile == null) ? new JFileChooser(MEMEApp.getLastDir()) : new JFileChooser(lastFile);
	    chooser.addChoosableFileFilter(new SimpleFileFilter(resultType + " result files (*.res;*.txt;*.out)"));
	    chooser.setMultiSelectionEnabled(multiSelection);
	    int returnVal = chooser.showOpenDialog(parent);
	    if (returnVal != JFileChooser.APPROVE_OPTION)
	    	return null;
	    lastFile = multiSelection ? chooser.getSelectedFiles()[0] : chooser.getSelectedFile();
	    MEMEApp.setLastDir(lastFile);
	    return multiSelection ? chooser.getSelectedFiles() : new File[] { chooser.getSelectedFile() };

		// NOTE: alternative file selection dialog (does not allow file type filters):
		// new java.awt.FileDialog(g_AppWindow.getJFrame()).setVisible(true);		
	}

	//-------------------------------------------------------------------------
	static File[] openFileDialog(java.awt.Frame parent, boolean multiSelection, final Object fileFilter) {
	    JFileChooser chooser = (lastFile == null) ? new JFileChooser(MEMEApp.getLastDir()) : new JFileChooser(lastFile);
	    chooser.addChoosableFileFilter(getFileFilter(fileFilter));
	    chooser.setMultiSelectionEnabled(multiSelection);
	    int returnVal = chooser.showOpenDialog(parent);
	    if (returnVal != JFileChooser.APPROVE_OPTION)
	    	return null;
	    lastFile = multiSelection ? chooser.getSelectedFiles()[0] : chooser.getSelectedFile();
	    MEMEApp.setLastDir(lastFile);
	    return multiSelection ? chooser.getSelectedFiles() : new File[] { chooser.getSelectedFile() };
	}
	
	//----------------------------------------------------------------------------------------------------
	private static FileFilter getFileFilter(final Object source) {
		if (source == null) return null;
		if (source instanceof FileFilter) 
			return (FileFilter) source;
		if (source instanceof String)
			return new SimpleFileFilter((String)source);
		return null;
	}

	//-------------------------------------------------------------------------
	/**
	 * This method blocks the caller until the window is closed.
	 */
	public void start(File f) {
		delayedUpdate = new DelayedUpdate(); 
		delayedUpdate.addWeakListener(this);

		start1(f);
		this.setVisible(true);		// blocks until the window is closed
	}

	//-------------------------------------------------------------------------
	private void start1(File f) {
		
		String settings = (String)app.get("SETTINGS");
		try {
			parser = new RepastResultParser(f,simphony);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 

		Model[] models = app.getSelectedModels();
		if (models != null && models.length > 0) {
			getJModelTextField().setText(models[0].getName());
			getJVersionTextField().setText(models[0].getVersion());
		}

		getJFilenameTextField().setText(parser.files[0].getPath());
		if(settings != null){
			getJIntelliSweepCheckBox().setEnabled(false);
            getJIntelliSweepCheckBox().setSelected(false);
			getJIntelliSweepFileTextField().setText("");
            try {
	            Element settingsXML = XMLUtils.load((new File(settings)).toURI());
	            NodeList nl = null;
	            nl = settingsXML.getElementsByTagName("intellisweep_page");
	            if(nl != null && nl.getLength() > 0){
	            	Element isElem = (Element) nl.item(0);
	            	if(!isElem.getAttribute("type").equals("none")){
	            		getJIntelliSweepCheckBox().setEnabled(true);
	            		getJIntelliSweepCheckBox().setSelected(true);
	            		getJIntelliSweepFileTextField().setText(settings);
	            	}
	            }
            } catch (Exception e) {
	            getJIntelliSweepCheckBox().setEnabled(true);
    			getJIntelliSweepCheckBox().setSelected(false);
        		getJIntelliSweepFileTextField().setText(settings);
            }
		} else{
			getJIntelliSweepCheckBox().setSelected(false);
			if (app.get("USER_SELECTED_FILES") != null)
				getJIntelliSweepCheckBox().setEnabled(true);
			else
				getJIntelliSweepCheckBox().setEnabled(false);
			getJIntelliSweepFileTextField().setText("");
		}
		repack = true;
		displayInfo("Loading " + f.getAbsolutePath(), ErrorType.FATAL);
		enableDisableControls();

//		Elobb tegyuk ki az ablakot, es utana kezdjuk el a fajl beolvasasat.
//		A progressbar kirakasat bizzuk LongOperation-re. 
//		Viszont ha hibat tapasztalunk a fajl beolvasasa kozben, akkor azt
//		az ablakba kell kiirni. Erre kell fenntartani egy warning teruletet.
//		Ha a fajl abszolut hibas (pl. nem is szovegfajl) azt is ide kell
//		kiirni. Ilyenkor persze minden control tiltva van, csak a Cancel gomb
//		es az ablakbezaro 'x' hasznalhato.

//		TODO: kulon olvassuk be a Repast fajl fejreszet es utana a sorokat.
//		a fejresz beolvasasa nagy fajlnal is gyors, es a legtobb minden kiderul
//		belole.  

//		TODO: display timestamps!

		GUIUtils.setBusy(this, true);
		MEMEApp.LONG_OPERATION.begin("Loading...", new LongRunnable() {
			// [Model thread]
			@Override
			public void trun() throws Exception {
				getReq().setErrorDisplay(false);
				parser.readFile();	// TODO: kulon a fejreszt, es utana a sorokat!
			}
			// [EDT]
			@Override
			public void finished() {
				GUIUtils.setBusy(RepastImportDialog.this, false);
				errorStatus = null;
				if (getReq().getError() != null) {
					displayInfo("Error while reading file, " + Utils.getLocalizedMessage(getReq().getError()), ErrorType.FATAL);
					GUIUtils.resizeJTableToContents(getJInputParsTable(), TABLE_COLUMNS_WIDTH_FACTOR);
					RepastImportDialog.this.pack();
				} else {
					if (0 < parser.warnings.length()) 
						displayInfo(parser.warnings, ErrorType.WARNING);
					enableDisableControls();
					start2(true);
				}
			}
		});
	}

	//-------------------------------------------------------------------------
	/**
	 * This method continues initialization after parser.readFile() succeeded. 
	 */
	// [EDT]
	private void start2(boolean first) {
		inputPars			= new InputParsTableModel();
		existingOutputPars	= null;
		scheduleReset		= false;
//		repack				= true;
		repack				= first; // fix redmine bug #588

		int			saveIndex = parser.inputIndex;
		Columns[]	io = null;
		String modelStatus;
		String modelName = getJModelTextField().getText().trim();
		String modelVer  = getJVersionTextField().getText().trim();
		// TODO: ezeknek az adatbazismuveleteknek a Model szalban kellene menniuk!!
		parser.model = app.getResultsDb().findModel(modelName, modelVer); 
		if (parser.model != null) {
			io = app.getResultsDb().getModelColumns(parser.model.getModel_id());
			existingOutputPars = io[1].getSorted();
			modelStatus = "(Exists)";
		}
		else if (isModelValid(modelName, modelVer)) {
			modelStatus = "(New)";
		}
		else {
			modelStatus = "(Invalid!)";
		}
		jModelStatusLabel.setText(modelStatus);
		
		// Add fixed parameters

		for (int i = 0, n = parser.fixedPar.size(); i < n; ++i) {
			String name = parser.fixedPar.getColumns().get(i).getName();
			boolean isNew = (io != null && !io[0].contains(name));
			inputPars.rows.add(new InputPar(isNew, InputParStatus.FIXED, name, parser.fixedPar.getLocalizedString(i))); 
		}

		// Add potential input parameters from parser.otherPar[]

		inputPars.otherParBegin = inputPars.rows.size();
		parser.inputIndex = first ? parser.canBeInput.size() - 1 : saveIndex;  
		for (int i = 0; i <= parser.inputIndex; ++i) {
			String name = parser.otherPar.get(i).getName();
			boolean isNew = (io != null && !contains(io[0],name));
			inputPars.rows.add(new InputPar(isNew, InputParStatus.OTHERPAR, name, parser.canBeInput.get(i).toString())); 
		}

		// Add missing input parameters (to the beginning)
		// TODO: jelezni kellene a tipusvaltozast is! pl. tooltipben?
		// A statuszsort ki lehetne egesziteni azzal h. van-e new/missing/modosult tipusu parameter?
		// Az output parametereknel is jelezni kellene a new-ot is meg a tipusvaltozast is!

		if (io != null) {
			ArrayList<Parameter> tmp = io[0].getSorted();
			for (int i = tmp.size() - 1; i >= 0; --i) {
				String name = tmp.get(i).getName();
				for (int j = inputPars.rows.size() - 1; j >= 0; --j) {
					if (name.equals(inputPars.rows.get(j).name)) { name = null; break; }
				}
				if (name != null) {
					inputPars.rows.add(0, new InputPar(false, InputParStatus.MISSING, name, ONE_RUN_CONSTANT));
					inputPars.otherParBegin += 1;
				}
			}
		}

		getJInputParsTable().setModel(inputPars);
		GUIUtils.resizeJTableToContents(getJInputParsTable(), TABLE_COLUMNS_WIDTH_FACTOR);

		jStatisticsLabel.setText(String.format("Number of runs: %d     Max. number of records per run: %d",
				parser.runs.size(), parser.maxRowsPerRun
		));

		movePar(0);
	}

	//-------------------------------------------------------------------------
	/**
	 * @param dir negative=move to input, 0=only update GUI, positive=move to output
	 */
	private void movePar(int dir) {
		if (dir < 0) {
			if (parser.inputIndex >= parser.canBeInput.size() - 1) return;
			parser.inputIndex += 1;
			inputPars.fireTableRowsInserted(inputPars.getRowCount() - 1, inputPars.getRowCount() - 1);
		}
		else if (0 < dir) { 
			if (parser.inputIndex < 0) return;
			parser.inputIndex -= 1;
			inputPars.fireTableRowsDeleted(inputPars.getRowCount(), inputPars.getRowCount());
		}
		updateOutputPars();

		if (errorStatus == null || errorStatus.compareTo(ErrorType.WARNING) <= 0) {
			String msg = "Please adjust the input and output parameter sets " +
						 "using the arrow buttons below the table!";

			if (!isModelValid(null, null))
				msg = Utils.htmlPage("<font color=blue>Please enter name and version!</font><br>" + msg);

			displayInfo(msg, ErrorType.WARNING);
		}
		enableDisableControls();
		delayedUpdate.fireLater();
	}

	//-------------------------------------------------------------------------
	private void updateOutputPars() {
		Columns c = parser.otherPar;
		List<Parameter> output = c.subList(parser.inputIndex+1, c.size());
		StringBuilder text = new StringBuilder( Utils.join(output, ", ") );
		if (existingOutputPars != null) {
			LinkedList<Parameter> tmp = new LinkedList<Parameter>();
			for (int i = existingOutputPars.size() - 1; i >= 0; --i) {
				if (!output.contains(existingOutputPars.get(i)))
					tmp.add(0, existingOutputPars.get(i));
			}
			if (!tmp.isEmpty()) {
				text.replace(0, text.length(), Utils.htmlQuote(text.toString()));
				text.append("<br><font color=gray><em>Missing: ");
				text.append(Utils.htmlQuote(Utils.join(tmp, ", ")));
				text.append("</em></font>");
				text.replace(0, text.length(), Utils.htmlPage(text.toString()));
			}
		}
		GUIUtils.setTextPane(getJOutputParsTextPane(), text.toString());
		delayedUpdate.fireLater();
	}
	
	//-------------------------------------------------------------------------
	private void displayInfo(String msg, ErrorType level) {
		if (msg == null) msg = "null";
		if (level != ErrorType.WARNING && !Utils.isHTML(msg))
			msg = Utils.htmlPage("<font color=red>" + msg); 
		if (lastmsg == null || !lastmsg.equals(msg)) { 
			errorStatus = level;
			GUIUtils.setTextPane(getJProblemsTextPane(), msg);
			// TODO: a JScrollPane-nel automatikusan scrollozzunk az elejere!
			lastmsg = msg;
			delayedUpdate.fireLater();
		}
	}

	//-------------------------------------------------------------------------
	private boolean isModelValid(String modelName, String modelVer) {
		if (modelName == null) modelName = getJModelTextField().getText().trim();
		if (modelVer == null)  modelVer  = getJVersionTextField().getText().trim();
		return (modelName.length() > 0 && modelVer.length() > 0);
	}

	//-------------------------------------------------------------------------
	public void controlSizes() {
		if (app == null) return;	// for GUI builder
		Dimension t = getJInputParsTable().getPreferredSize();
		Dimension oh = GUIUtils.getOverhead(getJScrollPane());
		int rowHeight = GUIUtils.getRowHeight(getJProblemsTextPane());
		int delta = 0, h;

		t.width = Math.min(t.width + oh.width, GUIUtils.getRelScrW(40));	// calc max. width of window
		
		Dimension d; 
		try {
			d = getJProblemsTextPane().getPreferredSize();
		} catch (NullPointerException e){
			MEMEApp.logException("RepastImportDialog (mostly harmless)", e);
			return;
		}
		if (d.width > t.width) {
			d.height = Math.min( d.height + (d.width / t.width) * rowHeight, 3*rowHeight ); 
		}
		d.height += oh.height;
		d.width = t.width;
		delta += d.height - getJScrollPane1().getHeight();
		getJScrollPane1().setPreferredSize(d);							// info/problems area [top] 

		rowHeight = GUIUtils.getRowHeight(getJOutputParsTextPane());
		d = getJOutputParsTextPane().getPreferredSize();
		h = d.height;
		if (d.width > t.width) {
			d.height = Math.min( d.height + (d.width / t.width) * rowHeight, 3*rowHeight ); 
		}
		d.height += oh.height;
		d.width = t.width;
		delta += d.height - getJScrollPane2().getHeight();
		getJScrollPane2().setPreferredSize(d);							// output pars [bottom]
		
		getJFilenameTextField().setColumns(t.width / textFieldColumnWidth);

		if (repack) {
			// Size of the table 

			h  = this.getHeight();
			h -= jScrollPane.getHeight();
			h += delta;		// size of "the rest" (size of window minus size of table) 
			h  = GUIUtils.getRelScrH(100) - h;	// max. size of table
			int minh = oh.height+3*rowHeight;
			if (inputPars == null || inputPars.getRowCount() < 3)
				h = Math.min(minh, h);
			h  = Math.max( Math.min(h, GUIUtils.getRelScrH(40)), minh ); 
			t.height= Math.min(t.height + oh.height, h);
			jScrollPane.setPreferredSize(t);
//			jScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

			repack = false;
			this.pack();
			
			Dimension oldD = this.getPreferredSize();
			this.setPreferredSize(new Dimension(oldD.width + 15,oldD.height + 15));
			oldD = this.getPreferredSize();
			final Dimension newD = GUIUtils.getPreferredSize(this);
			if (!oldD.equals(newD)) 
				this.setPreferredSize(newD);
			this.pack();
			
		} else {
			this.validate();
		}

		// Scroll to the beginning of list
		getJOutputParsTextPane().scrollRectToVisible(new java.awt.Rectangle(0,0,1,1));
	}

	//-------------------------------------------------------------------------
	@Override
	public void dispose() {
		super.dispose();
		if (parser != null) {
//			if (parser.dirty && app != null) {
//				app.refreshResultsBrowser();
//			}
			parser.close();
			parser = null;
		}
	}

	//-------------------------------------------------------------------------
	private void enableDisableControls() {
		if (errorStatus == ErrorType.FATAL) {
			// Disable everything except Cancel and filename
			getJModelTextField().setEnabled(false);
			getJVersionTextField().setEnabled(false);
			getJMoveToInputButton() .setEnabled(false);
			getJMoveToOutputButton().setEnabled(false);
			getJOkButton().setEnabled(false);
			return;
		} else {
			getJModelTextField().setEnabled(true);
			getJVersionTextField().setEnabled(true);
		}

		// Move to input button
		boolean enable = (parser.canBeInput != null && parser.inputIndex < parser.canBeInput.size() - 1);
		getJMoveToInputButton() .setEnabled(enable);

		// Move to output button
		enable = (0 <= parser.inputIndex);
		getJMoveToOutputButton().setEnabled(enable);

		// OK button
		enable = (errorStatus != ErrorType.ERROR && isModelValid(null, null));
		getJOkButton().setEnabled(enable);
	}

	//-------------------------------------------------------------------------
	private void modelVersionChanged(boolean edit) {
		if (edit) scheduleReset = true;					// typing/pasting text
		else if (scheduleReset) start2(false);			// focus lost or ENTER key 
	}

	//-------------------------------------------------------------------------
	private void modelVersionBindings(JTextField tf) {
		tf.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			 public void changedUpdate(DocumentEvent e)	{ modelVersionChanged(true); } 
			 public void insertUpdate(DocumentEvent e)		{ modelVersionChanged(true); }
			 public void removeUpdate(DocumentEvent e)		{ modelVersionChanged(true); }
		});
		tf.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e) { modelVersionChanged(false); }
		});
		// TODO: consider using InputVerifier instead! It is consulted whenever the component is about to lose the focus
		tf.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override public void focusLost(java.awt.event.FocusEvent e) { modelVersionChanged(false); }
		});
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BoxLayout(getJContentPane(), BoxLayout.Y_AXIS));
			jContentPane.add(getJScrollPane1(), null);
			jContentPane.add(getJPanel3(), null);
			jContentPane.add(getJPanel4(), null);
			jContentPane.add(getJScrollPane(), null);
			jContentPane.add(getJPanel1(), null);
			jContentPane.add(getJScrollPane2(), null);
			jContentPane.add(getJPanel2(), null);
		}
		return jContentPane;
	}

	/**
	 * This method initializes jFilenameTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	@SuppressWarnings("serial")
	private JTextField getJFilenameTextField() {
		if (jFilenameTextField == null) {
			jFilenameTextField = new JTextField() {{
				textFieldColumnWidth = getColumnWidth();
			}};
			jFilenameTextField.setName("fld_repast_filename");
			jFilenameTextField.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					start1(new java.io.File(jFilenameTextField.getText()));
				}
			});
		}
		return jFilenameTextField;
	}

	/**
	 * This method initializes jFilenameButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJFilenameButton() {
		if (jFilenameButton == null) {
			jFilenameButton = new JButton();
			jFilenameButton.setName("btn_repast_openfile");
			jFilenameButton.setText("...");
			jFilenameButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					java.io.File[] f = openFileDialog(app.getAppWindow(),resultType,false);
					if (f != null && f.length != 0)
						start1(f[0]);
				}
			});
		}
		return jFilenameButton;
	}

	/**
	 * This method initializes jScrollPane1	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane1() {
		if (jScrollPane1 == null) {
			jScrollPane1 = new JScrollPane();
			jScrollPane1.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			jScrollPane1.setViewportBorder(BorderFactory.createEmptyBorder(8,15,8,15));
			jScrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			jScrollPane1.setViewportView(getJProblemsTextPane());
			jScrollPane1.setBackground(getJProblemsTextPane().getBackground());

			int h = getJProblemsTextPane().getPreferredSize().height;
			jScrollPane1.setMinimumSize(new Dimension(100, 2 * h));
		}
		return jScrollPane1;
	}

	/**
	 * This method initializes jProblemsTextPane	
	 * 	
	 * @return javax.swing.JTextPane	
	 */
	private JTextPane getJProblemsTextPane() {
		if (jProblemsTextPane== null) {
			jProblemsTextPane= new JTextPane();
			jProblemsTextPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			jProblemsTextPane.setFont(new Font("Dialog", Font.BOLD, 12));
			jProblemsTextPane.setEditable(false);
		}
		return jProblemsTextPane;
	}

	/**
	 * This method initializes jScrollPane2	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane2() {
		if (jScrollPane2 == null) {
			jScrollPane2 = new JScrollPane();
			jScrollPane2.setBorder(BorderFactory.createTitledBorder(null, "Output parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), GUIUtils.getLabelFg()));
			jScrollPane2.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			jScrollPane2.setViewportView(getJOutputParsTextPane());
			
			Dimension d = getJOutputParsTextPane().getPreferredSize();
			jScrollPane2.setMinimumSize(new Dimension(100, d.height * 2));
//			jScrollPane2.setMaximumSize(new Dimension(300, d.height * 5));
		}
		return jScrollPane2;
	}

	/**
	 * This method initializes jOutputParsTextPane	
	 * 	
	 * @return javax.swing.JTextPane	
	 */
	private JTextPane getJOutputParsTextPane() {
		if (jOutputParsTextPane == null) {
			jOutputParsTextPane = new JTextPane();
			jOutputParsTextPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			jOutputParsTextPane.setEditable(false);
		}
		return jOutputParsTextPane;
	}

	/**
	 * This method initializes jPanel2	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			jPanel2 = new JPanel();
			jPanel2.setLayout(new BoxLayout(getJPanel2(), BoxLayout.X_AXIS));
			jPanel2.add(getJOkButton(), null);
			jPanel2.add(Box.createRigidArea(new Dimension(50, getJOkButton().getPreferredSize().height + 10)));
			jPanel2.add(getJCancelButton(), null);
		}
		return jPanel2;
	}

	/**
	 * This method initializes jOkButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJOkButton() {
		if (jOkButton == null) {
			jOkButton = new JButton();
			jOkButton.setText("OK");
			jOkButton.setName("btn_ok");
			jOkButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					MEMEApp.LONG_OPERATION.begin("Writing to database...", new LongRunnable() {
						String model   = getJModelTextField().getText().trim();
						String version = getJVersionTextField().getText().trim();
						String batchDesc = getJBatchDescTextArea().getText().trim();

						@Override
						public void trun() throws Exception {
							String isPluginXML = getJIntelliSweepFileTextField().getText().trim();
							if(getJIntelliSweepCheckBox().isSelected() && (new File(isPluginXML)).exists()){
//								try{
									parser.writeIntelliSweepResults(app.getResultsDb(), isPluginXML, model, version);
//								} catch(WizardLoadingException wle){
//									if(wle.getMessage().startsWith("Missing intelliSweep tag")){
//										Utilities.userAlert(getRootPane(), "The specified IntelliSweep settings XML(" + isPluginXML + ") does not contain IntelliSweep information.");
//										return;
//									} else{
//										throw wle;
//									}
//								}
							}
							getReq().setErrorDisplay(false);
							String pluginXML = isPluginXML==null ? (String)app.get("SETTINGS") : isPluginXML;
							parser.write(app.getResultsDb(), model, version, batchDesc, pluginXML);
						}

						@Override
						public void finished() {
							if (getReq().getError() == null) {
								app.put("EXIT_CODE","OK");
								RepastImportDialog.this.dispose();
							} else {
								MEMEApp.logException("RepastImportDialog.jOkButton", getReq().getError());
								if (getReq().getError().getMessage().startsWith("Missing intelliSweep tag")){
									displayInfo("The specified IntelliSweep settings XML(" + getJIntelliSweepFileTextField().getText() + ") does not contain IntelliSweep information.", ErrorType.WARNING);
								} else{
									displayInfo("Error while writing to database: " + Utils.getLocalizedMessage(getReq().getError()), ErrorType.FATAL);
								}
								//enableDisableControls();
							}
						}
					});
				}
			});
		}
		return jOkButton;
	}

	/**
	 * This method initializes jCancelButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJCancelButton() {
		if (jCancelButton == null) {
			jCancelButton = new JButton();
			jCancelButton.setName("btn_cancel");
			jCancelButton.setText("Cancel");
			jCancelButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					app.put("EXIT_CODE","CANCEL");
					RepastImportDialog.this.dispose();
				}
			});
		}
		return jCancelButton;
	}

	/**
	 * This method initializes jScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setBorder(BorderFactory.createTitledBorder(null, "Input parameters", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), GUIUtils.getLabelFg()));
			jScrollPane.setViewportView(getJInputParsTable());
		}
		return jScrollPane;
	}

	/**
	 * This method initializes jInputParsTable	
	 * 	
	 * @return javax.swing.JTable	
	 */
	private JTable getJInputParsTable() {
		if (jInputParsTable == null) {
			jInputParsTable = new JTable();
			jInputParsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			jInputParsTable.setEnabled(false);
		}
		return jInputParsTable;
	}

	/**
	 * This method initializes jPanel1	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel1() {
		if (jPanel1 == null) {
			jPanel1 = new JPanel();
			jPanel1.setLayout(new BoxLayout(getJPanel1(), BoxLayout.X_AXIS));
			jPanel1.add(getJMoveToOutputButton(), null);
			jPanel1.add(Box.createHorizontalStrut(50));
			jPanel1.add(getJMoveToInputButton(), null);
		}
		return jPanel1;
	}

	/**
	 * This method initializes jMoveToOutputButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJMoveToOutputButton() {
		if (jMoveToOutputButton == null) {
			jMoveToOutputButton = new JButton();
			jMoveToOutputButton.setName("btn_repast_output");
			jMoveToOutputButton.setIcon(new ImageIcon(getClass().getResource("down.png")));
			jMoveToOutputButton.setToolTipText("Add last as output");
			jMoveToOutputButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) { movePar(+1); }
			});
		}
		return jMoveToOutputButton;
	}

	/**
	 * This method initializes jMoveToInputButton	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getJMoveToInputButton() {
		if (jMoveToInputButton == null) {
			jMoveToInputButton = new JButton();
			jMoveToInputButton.setName("btn_repast_input");
			jMoveToInputButton.setIcon(new ImageIcon(getClass().getResource("up.png")));
			jMoveToInputButton.setToolTipText("Remove first from output");
			jMoveToInputButton.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) { movePar(-1); }
			});
		}
		return jMoveToInputButton;
	}

	/**
	 * This method initializes jPanel3	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel3() {
		if (jPanel3 == null) {
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.fill = GridBagConstraints.BOTH;
			gridBagConstraints3.gridy = 5;
			gridBagConstraints3.weightx = 1.0;
			gridBagConstraints3.gridwidth = 3;
			gridBagConstraints3.insets = new Insets(15, 0, 0, 0);
			gridBagConstraints3.gridx = 1;
			GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
			gridBagConstraints11.gridx = 0;
			gridBagConstraints11.insets = new Insets(16, 4, 10, 4);
			gridBagConstraints11.anchor = GridBagConstraints.WEST;
			gridBagConstraints11.gridy = 5;
			jLabel3 = new JLabel();
			jLabel3.setText(Utils.htmlPage("Batch<br>description:"));
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 4;
			gridBagConstraints2.insets = new Insets(0, 0, 10, 0);
			gridBagConstraints2.gridheight = 2;
			gridBagConstraints2.anchor = GridBagConstraints.NORTH;
			gridBagConstraints2.gridy = 0;
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints1.gridy = 0;
			gridBagConstraints1.weightx = 1.0;
			gridBagConstraints1.gridwidth = 3;
			gridBagConstraints1.gridx = 1;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.anchor = GridBagConstraints.WEST;
			gridBagConstraints.insets = new Insets(0, 4, 0, 0);
			gridBagConstraints.gridy = 0;
			GridBagConstraints gridBagConstraints10 = new GridBagConstraints();
			gridBagConstraints10.insets = new Insets(0, 10, 0, 5);
			gridBagConstraints10.gridy = 4;
			jModelStatusLabel = new JLabel();
			jModelStatusLabel.setText("(New)");
			jModelStatusLabel.setFont(new Font("Dialog", Font.ITALIC, 12));
			jModelStatusLabel.setPreferredSize(new Dimension(50,20));
			jModelStatusLabel.setMaximumSize(new Dimension(50,20));
			GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
			gridBagConstraints9.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints9.gridx = 3;
			gridBagConstraints9.gridy = 4;
			gridBagConstraints9.weightx = 1.0;
			GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
			gridBagConstraints8.gridy = 4;
			gridBagConstraints8.insets = new Insets(0, 8, 0, 4);
			gridBagConstraints8.gridx = 2;
			GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints7.gridx = 1;
			gridBagConstraints7.gridy = 4;
			gridBagConstraints7.weightx = 1.0;
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.gridy = 4;
			gridBagConstraints5.insets = new Insets(0, 4, 0, 4);
			gridBagConstraints5.gridx = 0;
			//IS checkbox:
			GridBagConstraints gridBagConstraintsIS1 = new GridBagConstraints();
			gridBagConstraintsIS1.gridx = 0;
			gridBagConstraintsIS1.gridy = 2;
			gridBagConstraintsIS1.insets = new Insets(8, 0, 0, 4);
			gridBagConstraintsIS1.weightx = 1.0;
//			gridBagConstraintsIS1.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraintsIS1.anchor = GridBagConstraints.WEST;
			gridBagConstraintsIS1.gridwidth = 3;
			//IS label:
			GridBagConstraints gridBagConstraintsIS2 = new GridBagConstraints();
			gridBagConstraintsIS2.gridx = 0;
			gridBagConstraintsIS2.gridy = 3;
			gridBagConstraintsIS2.insets = new Insets(2, 4, 8, 4);
			gridBagConstraintsIS2.weightx = 1.0;
			//gridBagConstraintsIS2.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraintsIS2.anchor = GridBagConstraints.WEST;
			gridBagConstraintsIS2.gridwidth = 1;
			//IS XML filename textfield:
			GridBagConstraints gridBagConstraintsIS3 = new GridBagConstraints();
			gridBagConstraintsIS3.gridx = 1;
			gridBagConstraintsIS3.gridy = 3;
			gridBagConstraintsIS3.insets = new Insets(2, 4, 8, 4);
			gridBagConstraintsIS3.weightx = 1.0;
			gridBagConstraintsIS3.fill = GridBagConstraints.HORIZONTAL;
			//gridBagConstraintsIS3.anchor = GridBagConstraints.WEST;
			gridBagConstraintsIS3.gridwidth = 3;
			//IS XML file browser button:
			GridBagConstraints gridBagConstraintsIS4 = new GridBagConstraints();
			gridBagConstraintsIS4.gridx = 4;
			gridBagConstraintsIS4.gridy = 3;
			gridBagConstraintsIS4.insets = new Insets(2, 4, 8, 4);
			gridBagConstraintsIS4.weightx = 1.0;
			//gridBagConstraintsIS4.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraintsIS4.anchor = GridBagConstraints.NORTH;
			//gridBagConstraintsIS4.gridwidth = 1;
			
			jLabel = new JLabel();
			jLabel.setText("Filename:");
			jLabel.setLabelFor(getJFilenameTextField());
			jLabel2 = new JLabel();
			jLabel2.setText("Version:");
			jLabel2.setLabelFor(getJVersionTextField());
			jLabel1 = new JLabel();
			jLabel1.setText("Model name:");
			jLabel1.setLabelFor(getJModelTextField());
			settingsLabel1 = new JLabel("settings XML:");
			settingsLabel1.setLabelFor(getJIntelliSweepFileTextField());
			jPanel3 = new JPanel();
			jPanel3.setLayout(new GridBagLayout());
			jPanel3.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
			jPanel3.add(jLabel, gridBagConstraints);
			jPanel3.add(getJFilenameTextField(), gridBagConstraints1);
			jPanel3.add(getJFilenameButton(), gridBagConstraints2);
			jPanel3.add(getJIntelliSweepCheckBox(),gridBagConstraintsIS1);
			jPanel3.add(settingsLabel1,gridBagConstraintsIS2);
			jPanel3.add(getJIntelliSweepFileTextField(),gridBagConstraintsIS3);
			jPanel3.add(getJIntelliSweepBrowseButton(),gridBagConstraintsIS4);
			jPanel3.add(jLabel1, gridBagConstraints5);
			jPanel3.add(getJModelTextField(), gridBagConstraints7);
			jPanel3.add(jLabel2, gridBagConstraints8);
			jPanel3.add(getJVersionTextField(), gridBagConstraints9);
			jPanel3.add(jModelStatusLabel, gridBagConstraints10);
			jPanel3.setMaximumSize(new Dimension(Integer.MAX_VALUE, jPanel3.getPreferredSize().height));
			jPanel3.add(jLabel3, gridBagConstraints11);
			jPanel3.add(getJBatchDescScrollPane(), gridBagConstraints3);
		}
		return jPanel3;
	}

	/**
	 * This method initializes jModelTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJModelTextField() {
		if (jModelTextField == null) {
			jModelTextField = new JTextField();
			jModelTextField.setName("fld_repast_modelname");
			modelVersionBindings(jModelTextField);
			jModelTextField.setPreferredSize(new Dimension(150,20));
			jModelTextField.setMaximumSize(new Dimension(150,20));
		}
		return jModelTextField;
	}
	
	/**
	 * This method initializes jVersionTextField	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getJVersionTextField() {
		if (jVersionTextField == null) {
			jVersionTextField = new JTextField();
			jVersionTextField.setName("fld_repast_version");
			jVersionTextField.setText("1");
			modelVersionBindings(jVersionTextField);
			jVersionTextField.setPreferredSize(new Dimension(100,20));
			jVersionTextField.setMaximumSize(new Dimension(100,20));
		}
		return jVersionTextField;
	}

	/**
	 * This method initializes jPanel4	
	 * 	
	 * @return javax.swing.JPanel	
	 */
	private JPanel getJPanel4() {
		if (jPanel4 == null) {
			FlowLayout flowLayout2 = new FlowLayout();
			flowLayout2.setAlignment(FlowLayout.LEFT);
			flowLayout2.setHgap(8);
			jStatisticsLabel = new JLabel();
			jStatisticsLabel.setText(" ");
			jStatisticsLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
			jPanel4 = new JPanel();
			jPanel4.setLayout(flowLayout2);
			jPanel4.add(jStatisticsLabel, null);
		}
		return jPanel4;
	}

	/**
	 * This method initializes jBatchDescTextArea	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getJBatchDescTextArea() {
		if (jBatchDescTextArea == null) {
			jBatchDescTextArea = new JTextArea();
			jBatchDescTextArea.setLineWrap(true);
			jBatchDescTextArea.setWrapStyleWord(true);
			jBatchDescTextArea.setName("fld_repast_batch");
		}
		return jBatchDescTextArea;
	}

	/**
	 * This method initializes jBatchDescScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJBatchDescScrollPane() {
		if (jBatchDescScrollPane == null) {
			jBatchDescScrollPane = new JScrollPane();
			jBatchDescScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			jBatchDescScrollPane.setViewportView(getJBatchDescTextArea());
		}
		return jBatchDescScrollPane;
	}

	private JButton getJIntelliSweepBrowseButton() {
		if(settingsBrowseButton == null){
			settingsBrowseButton = new JButton("...");
			settingsBrowseButton.setName("btn_repast_intellibrowse");
			settingsBrowseButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					File[] files = openFileDialog(app.getAppWindow(),false,new SettingsFileFilter());
					if(files != null && files.length > 0){
						getJIntelliSweepFileTextField().setText(files[0].getAbsolutePath());
					}
				}
			});
		}
    	return settingsBrowseButton;
    }

	private JCheckBox getJIntelliSweepCheckBox() {
		if(jIntelliSweepCheckBox == null){
			jIntelliSweepCheckBox = new JCheckBox("IntelliSweep result");
			jIntelliSweepCheckBox.setName("cbox_repast_intellicbox");
			jIntelliSweepCheckBox.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					//enableDisableIntelliSweepPart();
                }
			});
		}
    	return jIntelliSweepCheckBox;
    }

	protected void enableDisableIntelliSweepPart() {
		boolean toThis = getJIntelliSweepCheckBox().isSelected();
		settingsLabel1.setEnabled(toThis);
		getJIntelliSweepFileTextField().setEnabled(toThis);
		getJIntelliSweepBrowseButton().setEnabled(toThis);
    }

	private JTextField getJIntelliSweepFileTextField() {
		if(settingsFileTextField == null){
			settingsFileTextField = new JTextField();
			settingsFileTextField.setName("fld_repast_intellisweep");
		}
    	return settingsFileTextField;
    }
	
	//----------------------------------------------------------------------------------------------------
	private boolean contains(List list, Object o) {
		if (o == null) {
		    for (int i = 0;i < list.size();i++) {
		    	if (list.get(i) == null) return true;
		    }
		} else {
		    for (int i = 0;i < list.size();i++) {
		    	if (list.get(i) != null && list.get(i).equals(o)) return true;
		    }
		}
		return false;
	}

}  //  @jve:decl-index=0:visual-constraint="43,13"
