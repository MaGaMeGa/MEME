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
package ai.aitia.meme.netlogoimport;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ai.aitia.meme.Logger;
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

public class NetLogoImportDialog extends JDialog {
	
	//=========================================================================
	// Static members
	
	private static final long serialVersionUID = 1L;
	
	static final float TABLE_COLUMNS_WIDTH_FACTOR = 1.1f;
	static File lastFile = null;

	//=========================================================================
	// Nested types

	//----------------------------------------------------------------------------------------------------
	private static enum ErrorType {
		WARNING, ERROR, FATAL 
	};
	
	//----------------------------------------------------------------------------------------------------
	private static enum ParType {
		INPUT, OUTPUT
	}

	//----------------------------------------------------------------------------------------------------
	private static class Par {
		
		//====================================================================================================
		// members
		
		boolean	isNew;
		boolean	isMissing;
		ParType	type;
		String name;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		Par(boolean w, boolean m, ParType t, String n) { isNew = w; isMissing = m; type = t; name = n; } 
	}
	
	//----------------------------------------------------------------------------------------------------
	enum ColNames { Name, Type, Comment };
	
	//-------------------------------------------------------------------------
	@SuppressWarnings("serial")
	class ParsTableModel extends AbstractTableModel {
		
		//====================================================================================================
		// members

		ArrayList<Par> rows = new ArrayList<Par>();
		
		//====================================================================================================
		// methods

	    @Override public String	getColumnName(int col) { return ColNames.values()[col].name(); }
	    @Override public boolean isCellEditable(int row, int col)	{ return false; }
	    @Override public void	setValueAt(Object value, int row, int col) {}
	    
	    //====================================================================================================
		// implemented interfaces
	    
	    //----------------------------------------------------------------------------------------------------
		public int getColumnCount() { return ColNames.values().length; }
		public int getRowCount() { return rows.size(); }
		
		//----------------------------------------------------------------------------------------------------
	    public Object getValueAt(int row, int col) {
	    	String ans = "";
	    	Par r = rows.get(row);
	    	switch (ColNames.values()[col]) {
		    case Name 		: ans = r.name;
	    			    	  break;
		    case Type 		: if (r.type == ParType.INPUT)
		    				  	  ans = "Input";
		    				  else
		    					  ans = "Output";
		    				  break;
	    	case Comment 	: if (r.isNew)
	    					  	  ans = "new";
	    					  else if (r.isMissing)
	    						  ans = "missing";
	    					  break;
	    	}
	    	if (r.isNew || r.isMissing)
	    		ans = Utils.htmlPage("<b>" + Utils.htmlQuote(ans) + "</b>"); 
	    	return ans;
	    }
	}
	
	//-------------------------------------------------------------------------
	static class DelayedUpdate extends Event<NetLogoImportDialog,Void> {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		DelayedUpdate() { super("controlSizes",NetLogoImportDialog.class,null); }
		void fireLater() { super.fireLater(null); }
	}

	//=========================================================================
	// members
	
	private IImportPluginContext app = null;
	// bug fix #1478
//	private NetLogoResultParser	parser = null;
	private BufferedNetLogoResultParser parser = null;
	private ErrorType errorStatus = null;
	private ParsTableModel pars = null;  
	private String lastmsg = null;
	private boolean	scheduleReset = false;
	private boolean	repack = false;
	private DelayedUpdate delayedUpdate = null;
	private int	textFieldColumnWidth = 1;
	
	//=========================================================================
	// GUI components
	
	private JPanel jContentPane = null;  
	private JLabel jLabel = null;
	private JTextField jFilenameTextField = null;
	private JButton jFilenameButton = null;
	private JScrollPane jScrollPane1 = null;
	private JTextPane jProblemsTextPane = null;
	private JPanel jPanel2 = null;
	private JButton jOkButton = null;
	private JButton jCancelButton = null;
	private JScrollPane jScrollPane = null;
	private JTable jParsTable = null;
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
	private JLabel jIntelliSweepLabel1 = null;
	private JTextField jIntelliSweepFileTextField = null;
	private JButton jIntelliSweepBrowseButton = null;
	private JCheckBox include0thTickCheckBox = null;
	
	//====================================================================================================
	// methods 
	
	//-------------------------------------------------------------------------
	public NetLogoImportDialog(IImportPluginContext app) {
		super((app != null ? app.getAppWindow() : null),"NetLogo Import Settings",true);
		this.app = app;
		this.setName("dial_nlogoimport");
		initialize();
	}
	
	//-------------------------------------------------------------------------
	public void start(File f) {
		delayedUpdate = new DelayedUpdate(); 
		delayedUpdate.addWeakListener(this);
		
		start1(f);
		this.setVisible(true);		// blocks until the window is closed
	}
	
	//-------------------------------------------------------------------------
	static File[] openFileDialog(Frame parent, boolean multiSelection) {
		return openFileDialog(parent,multiSelection,"NetLogo result files (*.csv;*.txt;*.res;*.out)");
	}
	
	//-------------------------------------------------------------------------
	static File[] openFileDialog(Frame parent, boolean multiSelection, final Object fileFilter) {
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
	public void controlSizes() {
		if (app == null) return;	// for GUI builder
		Dimension t = getJParsTable().getPreferredSize();
		Dimension oh = GUIUtils.getOverhead(getJScrollPane());
		int rowHeight = GUIUtils.getRowHeight(getJProblemsTextPane());
		int delta = 0, h;

		t.width = Math.min(t.width + oh.width,GUIUtils.getRelScrW(40));	// calc max. width of window
		
		Dimension d;
		try {
			d = getJProblemsTextPane().getPreferredSize();
		} catch (Exception e){
			Logger.logException("NetlogoImportDialog (mostly harmless)", e);
			return;
		}
		if (d.width > t.width) 
			d.height = Math.min(d.height + (d.width / t.width) * rowHeight,3*rowHeight); 

		d.height += oh.height;
		d.width = t.width;
		delta += d.height - getJScrollPane1().getHeight();
		getJScrollPane1().setPreferredSize(d);							// info/problems area [top] 
		
		getJFilenameTextField().setColumns(t.width / textFieldColumnWidth);

		if (repack) {
			// Size of the table 

			h = this.getHeight();
			h -= jScrollPane.getHeight();
			h += delta;		// size of "the rest" (size of window minus size of table) 
			h = GUIUtils.getRelScrH(100) - h;	// max. size of table
			int minh = oh.height + 3 * rowHeight;
			if (pars == null || pars.getRowCount() < 3)
				h = Math.min(minh,h);
			h = Math.max(Math.min(h,GUIUtils.getRelScrH(40)),minh); 
			t.height = Math.min(t.height + oh.height,h);
			jScrollPane.setPreferredSize(t);
//			jScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));

			repack = false;
			this.pack();
		} else 
			this.validate();
	}
	
	//-------------------------------------------------------------------------
	@Override
	public void dispose() {
		super.dispose();
		parser = null;
	}
	
	//====================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------
	private void start1(File f) {
		// bug fix #1478
//		parser = new NetLogoResultParser(f); 
		parser = new BufferedNetLogoResultParser(f);
		Model[] models = app.getSelectedModels();
		if (models != null && models.length > 0) {
			getJModelTextField().setText(models[0].getName());
			getJVersionTextField().setText(models[0].getVersion());
		}

		getJFilenameTextField().setText(parser.file.getPath());
		String settings = (String) app.get("SETTINGS");
		if (settings != null){
			getJIntelliSweepCheckBox().setEnabled(false);
            getJIntelliSweepCheckBox().setSelected(false);
			getJIntelliSweepFileTextField().setText("");
            try {
	            Element settingsXML = XMLUtils.load((new File(settings)).toURI());
	            NodeList nl = null;
	            nl = settingsXML.getElementsByTagName("intellisweep_page");
	            if (nl != null && nl.getLength() > 0) {
	            	Element isElem = (Element) nl.item(0);
	            	if (!isElem.getAttribute("type").equals("none")){
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
		enableDisableIntelliSweepPart();
		repack = true;
		displayInfo("Loading " + f.getAbsolutePath(), ErrorType.FATAL);
		enableDisableControls();

		GUIUtils.setBusy(this, true);
		MEMEApp.LONG_OPERATION.begin("Loading...", new LongRunnable() {
			// [Model thread]
			@Override
			public void trun() throws Exception {
				getReq().setErrorDisplay(false);
				parser.readFile();	
			}
			// [EDT]
			@Override
			public void finished() {
				GUIUtils.setBusy(NetLogoImportDialog.this,false);
				errorStatus = null;
				if (getReq().getError() != null) {
					displayInfo("Error while reading file, " + Utils.getLocalizedMessage(getReq().getError()),ErrorType.FATAL);
					GUIUtils.resizeJTableToContents(getJParsTable(),TABLE_COLUMNS_WIDTH_FACTOR);
					NetLogoImportDialog.this.pack();
				} else {
					if (0 < parser.warnings.length()) 
						displayInfo(parser.warnings,ErrorType.WARNING);
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
		pars = new ParsTableModel();
		ArrayList<Parameter> existingOutputPars = null;
		ArrayList<Parameter> existingInputPars = null;
		scheduleReset = false;
		repack = true;

		int	stepIndex = parser.stepIndex;
		Columns[] io = null;
		String modelStatus;
		String modelName = getJModelTextField().getText().trim();
		String modelVer  = getJVersionTextField().getText().trim();
		parser.model = app.getResultsDb().findModel(modelName, modelVer); 
		if (parser.model != null) {
			io = app.getResultsDb().getModelColumns(parser.model.getModel_id());
			existingInputPars = io[0].getSorted();
			existingOutputPars = io[1].getSorted();
			modelStatus = "(Exists)";
		} else if (isModelValid(modelName,modelVer)) 
			modelStatus = "(New)";
		else
			modelStatus = "(Invalid!)";
		jModelStatusLabel.setText(modelStatus);
		
		// Add parameters
		
		for (int i = 0;i < parser.par.size();++i) {
			String name = parser.par.get(i).getName();
			boolean isNew = false;
			ParType parType = null;
			if (i < stepIndex - 1) {
				isNew = io != null && !contains(existingInputPars,name);
				parType = ParType.INPUT;
			} else {
				isNew = io != null && !contains(existingOutputPars,name);
				parType = ParType.OUTPUT;
			}
			pars.rows.add(new Par(isNew,false,parType,name));
			
		}
		
		if (io != null) {
			for (int i = existingOutputPars.size() - 1;i >= 0;--i) {
				String name = existingOutputPars.get(i).getName();
				for (int j = pars.rows.size() - 1;j >= 0;--j) {
					if (name.equals(pars.rows.get(j).name)) {
						name = null;
						break;
					}
				}
				if (name != null)
					pars.rows.add(0,new Par(false,true,ParType.OUTPUT,name));
			}
			for (int i = existingInputPars.size() - 1;i >= 0;--i) {
				String name = existingInputPars.get(i).getName();
				for (int j = pars.rows.size() - 1;j >= 0;--j) {
					if (name.equals(pars.rows.get(j).name)) {
						name = null;
						break;
					}
				}
				if (name != null)
					pars.rows.add(0,new Par(false,true,ParType.INPUT,name));
			}
		}

		getJParsTable().setModel(pars);
		GUIUtils.resizeJTableToContents(getJParsTable(),TABLE_COLUMNS_WIDTH_FACTOR);

		// bug fix #1478
//		jStatisticsLabel.setText(String.format("Number of runs: %d     Max. number of records per run: %d",parser.runs.size(),parser.maxRowsPerRun));
		jStatisticsLabel.setText(String.format("Number of runs: %d     Max. number of records per run: %d",parser.numberOfRuns,parser.maxRowsPerRun));
		enableDisableControls();
		delayedUpdate.fireLater();
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
	
	//-------------------------------------------------------------------------
	private void displayInfo(String msg, ErrorType level) {
		if (msg == null)
			msg = "null";
		if (level != ErrorType.WARNING && !Utils.isHTML(msg))
			msg = Utils.htmlPage("<font color=red>" + msg); 
		if (lastmsg == null || !lastmsg.equals(msg)) { 
			errorStatus = level;
			GUIUtils.setTextPane(getJProblemsTextPane(),msg);
			lastmsg = msg;
			delayedUpdate.fireLater();
		}
	}

	//-------------------------------------------------------------------------
	private boolean isModelValid(String modelName, String modelVer) {
		if (modelName == null)
			modelName = getJModelTextField().getText().trim();
		if (modelVer == null)
			modelVer  = getJVersionTextField().getText().trim();
		return (modelName.length() > 0 && modelVer.length() > 0);
	}
	
	//-------------------------------------------------------------------------
	private void modelVersionChanged(boolean edit) {
		if (edit)
			scheduleReset = true; // typing/pasting text
		else if (scheduleReset)
			start2(false);	// focus lost or ENTER key 
	}
	
	//-------------------------------------------------------------------------
	private void modelVersionBindings(JTextField tf) {
		tf.getDocument().addDocumentListener(new DocumentListener() {
			 public void changedUpdate(DocumentEvent e)	{ modelVersionChanged(true); } 
			 public void insertUpdate(DocumentEvent e) { modelVersionChanged(true); }
			 public void removeUpdate(DocumentEvent e)	{ modelVersionChanged(true); }
		});
		tf.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { modelVersionChanged(false); }
		});
		tf.addFocusListener(new FocusAdapter() {
			@Override public void focusLost(FocusEvent e) { modelVersionChanged(false); }
		});
	}
	
	//----------------------------------------------------------------------------------------------------
	protected void enableDisableIntelliSweepPart() {
		boolean toThis = getJIntelliSweepCheckBox().isSelected();
		jIntelliSweepLabel1.setEnabled(toThis);
		getJIntelliSweepFileTextField().setEnabled(toThis);
		getJIntelliSweepBrowseButton().setEnabled(toThis);
    }

	//====================================================================================================
	// GUI methods

	//-------------------------------------------------------------------------
	private void initialize() {
		getJContentPane().setPreferredSize(new Dimension(650,600));
		final JScrollPane sp = new JScrollPane(getJContentPane(),JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
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

	//-------------------------------------------------------------------------
	private void enableDisableControls() {
		if (errorStatus == ErrorType.FATAL) {
			// Disable everything except Cancel and filename
			getJModelTextField().setEnabled(false);
			getJVersionTextField().setEnabled(false);
			getJOkButton().setEnabled(false);
			return;
		} else {
			getJModelTextField().setEnabled(true);
			getJVersionTextField().setEnabled(true);
		}

		// OK button
		boolean enable = (errorStatus != ErrorType.ERROR && isModelValid(null,null));
		getJOkButton().setEnabled(enable);
	}

	//----------------------------------------------------------------------------------------------------
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BoxLayout(getJContentPane(),BoxLayout.Y_AXIS));
			jContentPane.add(getJScrollPane1(),null);
			jContentPane.add(getJPanel3(),null);
			jContentPane.add(getJPanel4(),null);
			jContentPane.add(getJScrollPane(),null);
			jContentPane.add(getJPanel2(),null);
		}
		return jContentPane;
	}

	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	private JTextField getJFilenameTextField() {
		if (jFilenameTextField == null) {
			jFilenameTextField = new JTextField() {
				{ textFieldColumnWidth = getColumnWidth(); }
			};
			jFilenameTextField.setName("fld_nlogo_filename");
			jFilenameTextField.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					start1(new File(jFilenameTextField.getText()));
				}
			});
		}
		return jFilenameTextField;
	}

	//----------------------------------------------------------------------------------------------------
	private JButton getJFilenameButton() {
		if (jFilenameButton == null) {
			jFilenameButton = new JButton("...");
			jFilenameButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					java.io.File[] f = openFileDialog(app.getAppWindow(),false);
					if (f != null && f.length != 0)
						start1(f[0]);
				}
			});
		}
		return jFilenameButton;
	}

	//----------------------------------------------------------------------------------------------------
	private JScrollPane getJScrollPane1() {
		if (jScrollPane1 == null) {
			jScrollPane1 = new JScrollPane();
			jScrollPane1.setBorder(null);
			jScrollPane1.setViewportBorder(BorderFactory.createEmptyBorder(8,15,8,15));
			jScrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			jScrollPane1.setViewportView(getJProblemsTextPane());
			jScrollPane1.setBackground(getJProblemsTextPane().getBackground());
			int h = getJProblemsTextPane().getPreferredSize().height;
			jScrollPane1.setMinimumSize(new Dimension(100, 2 * h));
		}
		return jScrollPane1;
	}

	//----------------------------------------------------------------------------------------------------
	private JTextPane getJProblemsTextPane() {
		if (jProblemsTextPane == null) {
			jProblemsTextPane = new JTextPane();
			jProblemsTextPane.setBorder(null);
			jProblemsTextPane.setFont(new Font("Dialog",Font.BOLD,12));
			jProblemsTextPane.setEditable(false);
		}
		return jProblemsTextPane;
	}

	//----------------------------------------------------------------------------------------------------
	private JPanel getJPanel2() {
		if (jPanel2 == null) {
			jPanel2 = new JPanel();
			jPanel2.setLayout(new BoxLayout(getJPanel2(),BoxLayout.X_AXIS));
			jPanel2.add(getJOkButton(),null);
			jPanel2.add(Box.createRigidArea(new Dimension(50,getJOkButton().getPreferredSize().height + 10)));
			jPanel2.add(getJCancelButton(),null);
		}
		return jPanel2;
	}

	//----------------------------------------------------------------------------------------------------
	private JButton getJOkButton() {
		if (jOkButton == null) {
			jOkButton = new JButton("OK");
			jOkButton.setName("btn_ok");
			jOkButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					MEMEApp.LONG_OPERATION.begin("Writing to database...",new LongRunnable() {
						String model   = getJModelTextField().getText().trim();
						String version = getJVersionTextField().getText().trim();
						String batchDesc = getJBatchDescTextArea().getText().trim();

						//----------------------------------------------------------------------------------------------------
						@Override
						public void trun() throws Exception {
							String isPluginXML = getJIntelliSweepFileTextField().getText().trim();
							if (getJIntelliSweepCheckBox().isSelected() && new File(isPluginXML).exists()) {
								// bug fix #1478
								parser.writeIntelliSweepResults(app.getResultsDb(),isPluginXML,model,version,batchDesc,include0thTickCheckBox.isSelected());
//								parser.writeIntelliSweepResults(app.getResultsDb(),isPluginXML,model,version,include0thTickCheckBox.isSelected());
							} else { // bug fix #1478
								getReq().setErrorDisplay(false);
								parser.write(app.getResultsDb(),model,version,batchDesc,include0thTickCheckBox.isSelected());
							} // bug fix #1478
						}

						//----------------------------------------------------------------------------------------------------
						@Override
						public void finished() {
							if (getReq().getError() == null) {
								app.put("EXIT_CODE","OK");
								NetLogoImportDialog.this.dispose();
							} else {
								Logger.logException("NetLogoImportDialog.jOkButton",getReq().getError());
								if (getReq().getError().getMessage().startsWith("Missing intelliSweep tag"))
									displayInfo("The specified IntelliSweep settings XML(" + getJIntelliSweepFileTextField().getText() + ") does not" +
												" contain IntelliSweep information.",ErrorType.WARNING);
								else
									displayInfo("Error while writing to database: " + Utils.getLocalizedMessage(getReq().getError()),ErrorType.FATAL);
								enableDisableControls();
							}
						}
					});
				}
			});
		}
		return jOkButton;
	}

	//----------------------------------------------------------------------------------------------------
	private JButton getJCancelButton() {
		if (jCancelButton == null) {
			jCancelButton = new JButton("Cancel");
			jCancelButton.setName("btn_cancel");
			jCancelButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					app.put("EXIT_CODE","CANCEL");
					NetLogoImportDialog.this.dispose();
				}
			});
		}
		return jCancelButton;
	}

	//----------------------------------------------------------------------------------------------------
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setBorder(BorderFactory.createTitledBorder(null,"Parameters",TitledBorder.DEFAULT_JUSTIFICATION,
																   TitledBorder.DEFAULT_POSITION,new Font("Dialog",Font.BOLD,12),
																   GUIUtils.getLabelFg()));
			jScrollPane.setViewportView(getJParsTable());
		}
		return jScrollPane;
	}

	//----------------------------------------------------------------------------------------------------
	private JTable getJParsTable() {
		if (jParsTable == null) {
			jParsTable = new JTable();
			jParsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			jParsTable.setEnabled(false);
		}
		return jParsTable;
	}

	//----------------------------------------------------------------------------------------------------
	private JPanel getJPanel3() {
		if (jPanel3 == null) {
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.fill = GridBagConstraints.BOTH;
			gridBagConstraints3.gridy = 6;
			gridBagConstraints3.weightx = 1.0;
			gridBagConstraints3.gridwidth = 3;
			gridBagConstraints3.insets = new Insets(15, 0, 0, 0);
			gridBagConstraints3.gridx = 1;
			GridBagConstraints gridBagConstraints11 = new GridBagConstraints();
			gridBagConstraints11.gridx = 0;
			gridBagConstraints11.insets = new Insets(16, 4, 10, 4);
			gridBagConstraints11.anchor = GridBagConstraints.WEST;
			gridBagConstraints11.gridy = 6;
			jLabel3 = new JLabel("Batch description:");
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
			gridBagConstraints10.gridy = 5;
			jModelStatusLabel = new JLabel();
			jModelStatusLabel.setText("(New)");
			jModelStatusLabel.setFont(new Font("Dialog", Font.ITALIC, 12));
			GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
			gridBagConstraints9.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints9.gridx = 3;
			gridBagConstraints9.gridy = 5;
			gridBagConstraints9.weightx = 1.0;
			GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
			gridBagConstraints8.gridy = 5;
			gridBagConstraints8.insets = new Insets(0, 8, 0, 4);
			gridBagConstraints8.gridx = 2;
			GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints7.gridx = 1;
			gridBagConstraints7.gridy = 5;
			gridBagConstraints7.weightx = 1.0;
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.gridy = 5;
			gridBagConstraints5.insets = new Insets(0, 4, 0, 4);
			gridBagConstraints5.gridx = 0;
			//0th tick checkbox:
			GridBagConstraints gridBagConstraints0thTick = new GridBagConstraints();
			gridBagConstraints0thTick.gridx = 0;
			gridBagConstraints0thTick.gridy = 2;
			gridBagConstraints0thTick.insets = new Insets(4, 0, 0, 4);
			gridBagConstraints0thTick.weightx = 1.0;
			gridBagConstraints0thTick.anchor = GridBagConstraints.WEST;
			gridBagConstraints0thTick.gridwidth = 3;
			//IS checkbox:
			GridBagConstraints gridBagConstraintsIS1 = new GridBagConstraints();
			gridBagConstraintsIS1.gridx = 0;
			gridBagConstraintsIS1.gridy = 3;
			gridBagConstraintsIS1.insets = new Insets(2, 0, 0, 4);
			gridBagConstraintsIS1.weightx = 1.0;
			gridBagConstraintsIS1.anchor = GridBagConstraints.WEST;
			gridBagConstraintsIS1.gridwidth = 3;
			//IS label:
			GridBagConstraints gridBagConstraintsIS2 = new GridBagConstraints();
			gridBagConstraintsIS2.gridx = 0;
			gridBagConstraintsIS2.gridy = 4;
			gridBagConstraintsIS2.insets = new Insets(2, 4, 8, 4);
			gridBagConstraintsIS2.weightx = 1.0;
			gridBagConstraintsIS2.anchor = GridBagConstraints.WEST;
			gridBagConstraintsIS2.gridwidth = 1;
			//IS XML filename textfield:
			GridBagConstraints gridBagConstraintsIS3 = new GridBagConstraints();
			gridBagConstraintsIS3.gridx = 1;
			gridBagConstraintsIS3.gridy = 4;
			gridBagConstraintsIS3.insets = new Insets(2, 4, 8, 4);
			gridBagConstraintsIS3.weightx = 1.0;
			gridBagConstraintsIS3.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraintsIS3.gridwidth = 3;
			//IS XML file browser button:
			GridBagConstraints gridBagConstraintsIS4 = new GridBagConstraints();
			gridBagConstraintsIS4.gridx = 4;
			gridBagConstraintsIS4.gridy = 4;
			gridBagConstraintsIS4.insets = new Insets(2, 4, 8, 4);
			gridBagConstraintsIS4.weightx = 1.0;
			gridBagConstraintsIS4.anchor = GridBagConstraints.NORTH;
			
			jLabel = new JLabel("Filename:");
			jLabel.setLabelFor(getJFilenameTextField());
			jLabel2 = new JLabel("Version:");
			jLabel2.setLabelFor(getJVersionTextField());
			jLabel1 = new JLabel("Model name:");
			jLabel1.setLabelFor(getJModelTextField());
			jIntelliSweepLabel1 = new JLabel("IntelliSweep settings XML:");
			jIntelliSweepLabel1.setLabelFor(getJIntelliSweepFileTextField());
			jPanel3 = new JPanel();
			jPanel3.setLayout(new GridBagLayout());
			jPanel3.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
			jPanel3.add(jLabel,gridBagConstraints);
			jPanel3.add(getJFilenameTextField(),gridBagConstraints1);
			jPanel3.add(getJFilenameButton(),gridBagConstraints2);
			jPanel3.add(getInclude0thTickCheckBox(),gridBagConstraints0thTick);
			jPanel3.add(getJIntelliSweepCheckBox(),gridBagConstraintsIS1);
			jPanel3.add(jIntelliSweepLabel1,gridBagConstraintsIS2);
			jPanel3.add(getJIntelliSweepFileTextField(),gridBagConstraintsIS3);
			jPanel3.add(getJIntelliSweepBrowseButton(),gridBagConstraintsIS4);
			jPanel3.add(jLabel1,gridBagConstraints5);
			jPanel3.add(getJModelTextField(),gridBagConstraints7);
			jPanel3.add(jLabel2,gridBagConstraints8);
			jPanel3.add(getJVersionTextField(),gridBagConstraints9);
			jPanel3.add(jModelStatusLabel,gridBagConstraints10);
			jPanel3.setMaximumSize(new Dimension(Integer.MAX_VALUE,jPanel3.getPreferredSize().height));
			jPanel3.add(jLabel3,gridBagConstraints11);
			jPanel3.add(getJBatchDescScrollPane(),gridBagConstraints3);
		}
		return jPanel3;
	}

	//----------------------------------------------------------------------------------------------------
	private JTextField getJModelTextField() {
		if (jModelTextField == null) {
			jModelTextField = new JTextField();
			jModelTextField.setName("fld_nlogo_modelname");
			jModelTextField.setPreferredSize(new Dimension(200,26));
			modelVersionBindings(jModelTextField);
		}
		return jModelTextField;
	}
	
	//----------------------------------------------------------------------------------------------------
	private JTextField getJVersionTextField() {
		if (jVersionTextField == null) {
			jVersionTextField = new JTextField();
			jVersionTextField.setPreferredSize(new Dimension(150,26));
			jVersionTextField.setText("1");
			jVersionTextField.setName("fld_nlogo_version");
			modelVersionBindings(jVersionTextField);
		}
		return jVersionTextField;
	}

	//----------------------------------------------------------------------------------------------------
	private JPanel getJPanel4() {
		if (jPanel4 == null) {
			FlowLayout flowLayout2 = new FlowLayout();
			flowLayout2.setAlignment(FlowLayout.LEFT);
			flowLayout2.setHgap(8);
			jStatisticsLabel = new JLabel(" ");
			jStatisticsLabel.setFont(new Font("Dialog",Font.PLAIN,12));
			jPanel4 = new JPanel();
			jPanel4.setLayout(flowLayout2);
			jPanel4.add(jStatisticsLabel,null);
		}
		return jPanel4;
	}

	//----------------------------------------------------------------------------------------------------
	private JTextArea getJBatchDescTextArea() {
		if (jBatchDescTextArea == null) {
			jBatchDescTextArea = new JTextArea();
			jBatchDescTextArea.setBorder(null);
			jBatchDescTextArea.setLineWrap(true);
			jBatchDescTextArea.setWrapStyleWord(true);
			jBatchDescTextArea.setName("fld_nlogo_batch");
		}
		return jBatchDescTextArea;
	}

	//----------------------------------------------------------------------------------------------------
	private JScrollPane getJBatchDescScrollPane() {
		if (jBatchDescScrollPane == null) {
			jBatchDescScrollPane = new JScrollPane();
			jBatchDescScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			jBatchDescScrollPane.setViewportView(getJBatchDescTextArea());
		}
		return jBatchDescScrollPane;
	}

	//----------------------------------------------------------------------------------------------------
	private JButton getJIntelliSweepBrowseButton() {
		if (jIntelliSweepBrowseButton == null) {
			jIntelliSweepBrowseButton = new JButton("...");
			jIntelliSweepBrowseButton.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					File[] files = openFileDialog(app.getAppWindow(),false,new SettingsFileFilter());
					if (files != null && files.length > 0)
						getJIntelliSweepFileTextField().setText(files[0].getAbsolutePath());
				}
			});
		}
    	return jIntelliSweepBrowseButton;
    }
	
	//----------------------------------------------------------------------------------------------------
	private JCheckBox getInclude0thTickCheckBox() {
		if (include0thTickCheckBox == null) {
			include0thTickCheckBox = new JCheckBox("Include 0th ticks (initial states)");
			include0thTickCheckBox.setSelected(true);
		}
		return include0thTickCheckBox;
	}

	//----------------------------------------------------------------------------------------------------
	private JCheckBox getJIntelliSweepCheckBox() {
		if(jIntelliSweepCheckBox == null) {
			jIntelliSweepCheckBox = new JCheckBox("IntelliSweep result");
			jIntelliSweepCheckBox.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					enableDisableIntelliSweepPart();
                }
			});
			enableDisableIntelliSweepPart();
		}
    	return jIntelliSweepCheckBox;
    }

	//----------------------------------------------------------------------------------------------------
	private JTextField getJIntelliSweepFileTextField() {
		if (jIntelliSweepFileTextField == null) 
			jIntelliSweepFileTextField = new JTextField();
		jIntelliSweepFileTextField.setName("fld_nlogo_intellisweep");
    	return jIntelliSweepFileTextField;
    }
} 
