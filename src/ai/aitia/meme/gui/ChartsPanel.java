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
package ai.aitia.meme.gui;

import static ai.aitia.meme.utils.GUIUtils.GUI_unit;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.freehep.graphics2d.VectorGraphics;
import org.freehep.graphicsio.emf.EMFGraphics2D;
import org.freehep.graphicsio.ps.PSGraphics2D;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import ai.aitia.chart.AbstractChart;
import ai.aitia.chart.ChartConfig;
import ai.aitia.chart.ChartConfigCollection;
import ai.aitia.chart.DataSources;
import ai.aitia.chart.charttypes.dialogs.AbstractChartDialog;
import ai.aitia.chart.charttypes.dialogs.ChartDialogChangeCenter;
import ai.aitia.chart.dialogs.CCCollectionDialog;
import ai.aitia.chart.dialogs.ChartDialogChangeListener;
import ai.aitia.chart.networkpanel.NetworkPanel;
import ai.aitia.chart.util.ChartConstants;
import ai.aitia.chart.util.XMLLoadingException;
import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.chart.ResultDataSources;
import ai.aitia.meme.chart.ViewDataSources;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.ConnChangedEvent;
import ai.aitia.meme.database.IConnChangedListener;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ViewRec;
import ai.aitia.meme.events.HybridAction;
import ai.aitia.meme.events.IHybridActionListener;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.paramsweep.utils.Utilities;
import ai.aitia.meme.utils.FormsUtils;
import ai.aitia.meme.utils.GUIUtils;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.XMLUtils;

//-----------------------------------------------------------------------------
/** GUI component that enables to create charts from view tables. */
public class ChartsPanel extends JPanel implements IHybridActionListener, ChartDialogChangeListener, ActionListener {
	
	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 1L;
	private static final String CHANGE_VIEW = "Change view...";
	private static final String CHANGE_RESULT = "Change result...";
	
	private static boolean chartingPackageIsinitialized = false;	// TODO: ezt pluginmanagerben kellene
	private static int MIN_WIDTH = 816;


	private boolean panelIsInUse = false;
	private java.awt.Container cccdialog = null;
	private ChartConfigCollection chartConfigCollection = null;
	private boolean viewChart;
	
	private JPanel jCenterPanel = null;
	private JLabel jViewNamesLabel = null;
	private JButton jChangeViewButton = new JButton(CHANGE_VIEW); 

	public final HybridAction createChart = new HybridAction(this, "Create charts...","chart_create.png",HybridAction.SHORT_DESCRIPTION,
															 "Create one or more charts from the currently selected result/view");
	public final HybridAction openChart	= new HybridAction(this,"Open chart","chart_open.png",HybridAction.SHORT_DESCRIPTION,
														   "Load a previously saved chart collection configuration");
	public final HybridAction exportChartAsImage = new HybridAction(this,"Export charts as image...",null,HybridAction.SHORT_DESCRIPTION,
																   "Export one or more previously saved chart collection configurations as images");
	
	
	//====================================================================================================
	// methods

	//-------------------------------------------------------------------------
	public ChartsPanel() {
		super();
		ChartDialogChangeCenter.addChartDialogChangeListener(this);
		initialize();
		
//		MainWindow mw = MEMEApp.getMainWindow();
//		mw.activePanel.addWeakListener(this);
//		mw.whenAViewIsSelected.addWeakListener(this);
	}

	//-------------------------------------------------------------------------
	/** Displays the panel.
	 * @param selectedView the selected view
	 * @param viewCols column information about the selected view
	 */
	public void start(final ViewRec selectedView, final Columns viewCols) {
		init();
		final ViewDataSources dspc = new ViewDataSources(selectedView.getViewID(),viewCols);
		chartConfigCollection = new ChartConfigCollection(new DataSources(dspc),false);
		chartConfigCollection.setWindowMinWidth(MIN_WIDTH);
		start(chartConfigCollection,selectedView.getName(),true);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void start(final Long[][] selectedResult, final Columns inputColumns, final Columns outputColumns) {
		init();
		final long modelId = selectedResult[0][0];
		final List<Long> batchesList = new ArrayList<Long>();
		for (Long[] comb : selectedResult) {
			if (comb[0] == modelId && comb.length > 1)
				batchesList.add(comb[1]);
		}
		Collections.sort(batchesList);
		final long[] batches = batchesList.isEmpty() ? null : new long[batchesList.size()];
		if (batches != null) {
			for (int i = 0;i < batchesList.size();++i)
				batches[i] = batchesList.get(i);
		}
		
		Model m = null;
		try {
			m = (Model) MEMEApp.LONG_OPERATION.execute("Searching model...",new Callable<Object>() {
				public Object call() {
					return MEMEApp.getResultsDb().findModel(modelId);
				}
			});
			
			if (m != null) {
				final ResultDataSources dspc = new ResultDataSources(modelId,batches,inputColumns,outputColumns);
				chartConfigCollection = new ChartConfigCollection(new DataSources(dspc),false);
				chartConfigCollection.setWindowMinWidth(MIN_WIDTH);
				start(chartConfigCollection,m.getName() + "/" + m.getVersion(),false);
			}
		} catch (Exception e) {
			Logger.logError("Unable to create Charts panel.");
			Logger.logException(e);
		}
	}
	
	
	//----------------------------------------------------------------------------------------------------
	public void saveChartsAsXML(final File file) throws Exception {
		init();
		if (panelIsInUse && chartConfigCollection != null) {
			if (((CCCollectionDialog)cccdialog).updateAll()) 
				chartConfigCollection.save(file);
			else
				throw new Exception("Some of the charts are not saveable");
		} else
			throw new Exception("There is no created chart collection");
	}
	
	//----------------------------------------------------------------------------------------------------
	public int saveChartsAsPNG(final File dir, final  String prefix) throws Exception {
		init();
		if (panelIsInUse && chartConfigCollection != null) {
			int nrOfFiles = 0;
			if (!((CCCollectionDialog)cccdialog).updateAll())  
				throw new Exception("Some of the charts are not saveable");
			GUIUtils.setBusy(MEMEApp.getAppWnd(),true);
			try {
				nrOfFiles = (Integer) MEMEApp.LONG_OPERATION.execute("Exporting charts...",new Callable<Object>() {
					public Object call() {
						int processed = 0;
						int index = 0;
						for (final Object o : chartConfigCollection) {
							try {
								final ChartConfig cc = (ChartConfig) o;
								final String imgName = prefix + "_" + index++ + "_" + getTitleFromConfig(cc,true) + ".png";
								final AbstractChart chart = AbstractChart.find(cc.getChartType());
								final JPanel panel = (JPanel) chart.createChart(cc);
								final JFrame fr = new JFrame();
								fr.setContentPane(panel);
								fr.pack();
								
								try {
									final File image = new File(dir,imgName);
									if (panel instanceof NetworkPanel) 
										((NetworkPanel)panel).saveGraphImage(image);
									else {
										final BufferedImage bufferedImage = new BufferedImage(panel.getWidth(),panel.getHeight(),
																						  BufferedImage.TYPE_INT_RGB);
										final Graphics2D g2d = bufferedImage.createGraphics();
										panel.print(g2d);
										g2d.dispose();
										ImageIO.write(bufferedImage,"png",image);
									}
									processed++;
								} catch (final IOException ex) {
									Logger.logError("Unable to save file: %s",imgName);
									Logger.logException(ex);
								}
							} catch (final Throwable ex) {
								Logger.logError("Unable to create chart");
								Logger.logException(ex);
							}
						}
						return processed;
					}
				});
			} catch (Exception e1) {}
			GUIUtils.setBusy(MEMEApp.getAppWnd(),false);
			return nrOfFiles;
		}
		return -1;
	}

	//-------------------------------------------------------------------------
	private void start(final ChartConfigCollection ccc, final String name, final boolean view) {
		this.viewChart = view;
		jViewNamesLabel.setVisible(name != null);
		if (name == null)
			jViewNamesLabel.setText("");
		else
			jViewNamesLabel.setText(String.format("Creating charts from the '%s' %s table",name,view ? "view" : "result"));

		ccc.setDefaultFireInitialEvent(true);
		cccdialog = ccc.createDialog();
		
		jChangeViewButton.setText(view ? CHANGE_VIEW : CHANGE_RESULT);
		jChangeViewButton.setEnabled(cccdialog != null && (cccdialog instanceof CCCollectionDialog) && ((CCCollectionDialog)cccdialog).isSaveable());

		getJCenterPanel().removeAll();
		getJCenterPanel().add(cccdialog);

		panelIsInUse = true;

		final MainWindowPanelManager pm = MEMEApp.getMainWindow().panelManager;
		pm.setEnabled(this,true);
		pm.setActive(this);

		// bug#36 atmeneti javitasa
		// TODO: Ha majd a chPkg. dialogjai rendesen tudnak keskenyedni, akkor
		// ezt erdemes lesz kivenni, mert ez igy elrontja a user altal a
		// foablaknak beallitott meretet (pl. maximalizalast).
		// most m�r nem rontja el a maximaliz�l�st, b�r ronda megold�s
		if (MEMEApp.getAppWnd().getSize().width < MIN_WIDTH)
			GUIUtils.repack(getJCenterPanel());
	}

	//=========================================================================
	//	Controller methods
	
	//-------------------------------------------------------------------------
	private void init() {
		if (!chartingPackageIsinitialized) {
			AbstractChart.registerAll();
			chartingPackageIsinitialized = true;
		}
	}


	//-------------------------------------------------------------------------
	/** Hides the panel. */
	private boolean finish(final String ask_title) {
		if (ask_title != null) {
			MEMEApp.getMainWindow().panelManager.setActive(this);
			if (1 != MEMEApp.askUser(false, ask_title,"This operation will cancel the currently edited chart collection without saving.",
									 "Are you sure?")) 
				return false;
		}
		cccdialog = null;
		chartConfigCollection = null;
		getJCenterPanel().removeAll();
		panelIsInUse = false;
		final MainWindowPanelManager pm = MEMEApp.getMainWindow().panelManager;
		pm.setActive(MEMEApp.getMainWindow().getViewsPanel());
		pm.setEnabled(this,false);
		return true;
	}
	
	//====================================================================================================
	// implemented interfaces

//	//-------------------------------------------------------------------------
//	public void onProgramStateChange(ProgramStateChangeEvent parameters) {
//		MainWindow mw = MEMEApp.getMainWindow();
//		boolean enabled = (!this.panelIsInUse && mw.whenAViewIsSelected.getValue() && mw.activePanel.getValue() == mw.getViewsPanel());
//		createChart.setEnabled(enabled);
//		openChart.setEnabled(!this.panelIsInUse);
//	}
	
	//----------------------------------------------------------------------------------------------------
	public void dataSourcesChanged() {}
	public void templateChanged() {}
	public void titleChanged(AbstractChartDialog where) {}

	//----------------------------------------------------------------------------------------------------
	public void saveStatusChanged(AbstractChartDialog where, boolean enabled) {
		final boolean isEnabled = cccdialog != null && (cccdialog instanceof CCCollectionDialog) && ((CCCollectionDialog)cccdialog).isSaveable();
		jChangeViewButton.setEnabled(isEnabled);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void actionPerformed(final ActionEvent e) { hybridAction(e,null); }
	
	//-------------------------------------------------------------------------
	public void hybridAction(final ActionEvent e, final HybridAction a) {
		if (a == null) {
			final String cmd = e.getActionCommand();
			if ("CHANGE_VIEW".equals(cmd))
				changeViewOrResult();
		} else if (a == createChart) {
			final ViewsPanel vp = MEMEApp.getMainWindow().getViewsPanel();
			final ResultsPanel rp = MEMEApp.getMainWindow().getResultsPanel();
			if (MEMEApp.getMainWindow().panelManager.getActive().equals(vp)) {
				if (vp.getSelectedView() == null) {
					MEMEApp.getMainWindow().panelManager.setActive(vp);
					MEMEApp.userErrors("Create charts","Please select a view table before starting this operation.");
					return;
				}
				if (vp.getNrOfRowsOfSelectedView() == 0) {
					MEMEApp.getMainWindow().panelManager.setActive(vp);
					MEMEApp.userErrors("Create charts","The selected view table does not contain any data.");
					return;
				}
				if (this.panelIsInUse && !finish(createChart.getValue(HybridAction.NAME).toString())) return;
				start(vp.getSelectedView(),vp.getColumnsOfSelectedView());
			} else if (MEMEApp.getMainWindow().panelManager.getActive().equals(rp)) {
				final Long[][] selection = rp.getResultsBrowser().getSelection();
				if (selection.length == 0) {
					MEMEApp.getMainWindow().panelManager.setActive(rp);
					MEMEApp.userErrors("Create charts","Please select a result table before starting this operation.");
					return;
				}
				if (this.panelIsInUse && !finish(createChart.getValue(HybridAction.NAME).toString())) return;
				Columns[] modelColumns = MEMEApp.getResultsDb().getModelColumns(selection[0][0]);
				start(selection,modelColumns[0],modelColumns[1]);
			} else {
				MEMEApp.userErrors("Create charts","Please select a result or view table before starting this operation.");
				return;
			}
		} else if (a == openChart) {
			if (this.panelIsInUse && !finish(openChart.getValue(HybridAction.NAME).toString())) return; 
			final JFileChooser filedialog = new JFileChooser(MEMEApp.getLastDir());
		    filedialog.addChoosableFileFilter(new SimpleFileFilter("Saved chart configurations (*.xml)"));
		    final int returnVal = filedialog.showOpenDialog(this);
		    if (returnVal == JFileChooser.APPROVE_OPTION) {
		    	final File f = filedialog.getSelectedFile();
		    	MEMEApp.setLastDir(f);
		    	init();
//		    	MEMEApp.LONG_OPERATION.begin("Loading chart configuration...",new LongRunnable() {
//		    		private String viewName = null;
//		    		
//		    		@Override public void trun() throws Exception {
//		    			chartConfigCollection = ViewDataSources.load(f.toURI());
//		    			final DataSources ds = chartConfigCollection.getDataSources();
//		    			if (ds != null && ds.getDSPCollection() instanceof ViewDataSources)
//		    				viewName = ((ViewDataSources)ds.getDSPCollection()).getViewName();
//		    		}
//		    		
//		    		@Override public void finished() throws Exception {
//		    			if (chartConfigCollection != null)
//		    				start(chartConfigCollection,viewName);
//		    		}
//		    	});
		    	// fix Redmine bug #561
				final Throwable[] error = new Throwable[1];
				MEMEApp.LONG_OPERATION.executeNE("Loading chart configuration...",error,new Callable<Object>() {

					public Object call() throws Exception {
						String name = null;
						
						final boolean isViewChartXML = isViewChartXML(f.toURI());
						if (isViewChartXML) 
							chartConfigCollection = ViewDataSources.load(f.toURI());
 						else 
 							chartConfigCollection = ResultDataSources.load(f.toURI());
						chartConfigCollection.setWindowMinWidth(MIN_WIDTH);
						final DataSources ds = chartConfigCollection.getDataSources();
						if (ds != null && ds.getDSPCollection() instanceof ViewDataSources)
							name = ((ViewDataSources)ds.getDSPCollection()).getViewName();
						else if (ds != null && ds.getDSPCollection() instanceof ResultDataSources) {
							ResultDataSources dsp = (ResultDataSources) ds.getDSPCollection();
							name =  dsp.getModelName() + "/" + dsp.getVersion();
						}
						if (chartConfigCollection != null)
							start(chartConfigCollection,name,isViewChartXML);
						return null;
					}
					
				});
				if (error[0] != null) 
					Logger.logException("ChartsPanel.openChart",error[0]);
		    	// end of fix 
		    }
		} else if (a == exportChartAsImage) {
			final JFileChooser filedialog = new JFileChooser(MEMEApp.getLastDir());
			filedialog.addChoosableFileFilter(new SimpleFileFilter("Saved chart configurations (*.xml)"));
			filedialog.setAcceptAllFileFilterUsed(false);
			filedialog.setMultiSelectionEnabled(true);
			final int returnVal = filedialog.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				final java.io.File[] fs = filedialog.getSelectedFiles();
				MEMEApp.setLastDir(fs[0]);
				
				final FormatChooserDialog dlg = new FormatChooserDialog(MEMEApp.getMainWindow().getJFrame());
				int ret = dlg.showDialog();
				if (ret == FormatChooserDialog.CANCEL) return;
				final boolean png = dlg.isPNG();
				final boolean eps = dlg.isEPS();
				final boolean emf = dlg.isEMF();
				dlg.dispose();
				
				init();
				Integer[] errno = null;
				GUIUtils.setBusy(MEMEApp.getAppWnd(),true);
				try {
					errno = (Integer[])MEMEApp.LONG_OPERATION.execute("Exporting charts...",new Callable<Object>() {
						public Object call() {
							int errorNumberFiles = 0, errorNumberImgs = 0;
							
							int processed = 0;
							for (int i = 0;i < fs.length;++i) {
								if (MEMEApp.LONG_OPERATION.isUserBreak()) 
									return new Integer[] {-1, -1};
//								MEMEApp.LONG_OPERATION.setTaskName(String.format("Exporting charts... (Processed %d files out of %d)",
//																				 processed,fs.length));
								MEMEApp.LONG_OPERATION.setTitle(String.format("Please wait... (%d/%d)",processed + 1,fs.length),null);
								MEMEApp.LONG_OPERATION.progress(processed,fs.length);
								System.gc(); System.gc();
								ChartConfigCollection ccc = null;
								try {
									ccc = isViewChartXML(fs[i].toURI()) ? ViewDataSources.load(fs[i].toURI()) : ResultDataSources.load(fs[i].toURI());
								} catch (Exception ex) {
									Logger.logError("Unable to load file: %s.",fs[i].getName());
									Logger.logException(ex);
									errorNumberFiles++;
									continue;
								}
								int index = 0;
								for (final ChartConfig cc : ccc) {
									try {
										final AbstractChart chart = AbstractChart.find(cc.getChartType());
										final JPanel panel = (JPanel) chart.createChart(cc);
										final JFrame fr = new JFrame();
										fr.setContentPane(panel);
										fr.pack();
										
										final String imgName = fs[i].getName().substring(0,fs[i].getName().lastIndexOf('.')) + "_" + index++ + 
															   "_" + getTitleFromConfig(cc,true);
										String extension = "";
										try {
											if (png) {
												extension = ".png";
												exportChart(panel,imgName,extension);
											}
											if (eps) {
												extension = ".eps";
												exportChart(panel,imgName,extension);
											}
											if (emf) {
												extension = ".emf";
												exportChart(panel,imgName,extension);
											}
										} catch (final IOException ex) {
											Logger.logError("Unable to save file: %s",imgName + extension);
											Logger.logException(ex);
											errorNumberImgs++;
										}
									} catch (final Throwable ex) {
										Logger.logError("Unable to create chart");
										Logger.logException(ex);
										Logger.logExceptionCallStack(ex);
										errorNumberImgs++;
									}
								}
								ccc = null;
								processed++;
							}
							MEMEApp.LONG_OPERATION.setTaskName(String.format("Exporting charts... (Processed %d files out of %d)",processed,fs.length));
							MEMEApp.LONG_OPERATION.progress(processed,fs.length);
							return new Integer[] { errorNumberFiles, errorNumberImgs };
						}
					});
				} catch (Exception e1) {}
				GUIUtils.setBusy(MEMEApp.getAppWnd(),false);
				if (errno[0].intValue() > 0 || errno[1].intValue() > 0) {
					String msg2 = "", msg3 = "";
					if (errno[0].intValue() > 0)
						msg2 = "Number of unprocessed files: " + errno[0].toString();
					if (errno[1].intValue() > 0)
						msg3 = "Number of unexported charts: " + errno[1].toString();
					MEMEApp.userErrors("Error during the chart exporting",msg2,msg3,MEMEApp.seeTheErrorLog("%s %s"));
				} else 
					MEMEApp.userAlert("Chart exporting successful!");
			}
		}
	}

	//----------------------------------------------------------------------------------------------------
	public void loadChartFromXMLAndDelete(final File chartFile){
    	MEMEApp.setLastDir(chartFile);
    	init();
    	MEMEApp.LONG_OPERATION.begin("Loading chart configuration...", new LongRunnable() {
    		private String name = null;
    		
    		@Override public void trun() throws Exception {
				final boolean isViewChartXML = isViewChartXML(chartFile.toURI());
				if (isViewChartXML) 
					chartConfigCollection = ViewDataSources.load(chartFile.toURI());
				else 
					chartConfigCollection = ResultDataSources.load(chartFile.toURI());
				chartConfigCollection.setWindowMinWidth(MIN_WIDTH);
				final DataSources ds = chartConfigCollection.getDataSources();
				if (ds != null && ds.getDSPCollection() instanceof ViewDataSources)
					name = ((ViewDataSources)ds.getDSPCollection()).getViewName();
				else if (ds != null && ds.getDSPCollection() instanceof ResultDataSources) {
					ResultDataSources dsp = (ResultDataSources) ds.getDSPCollection();
					name =  dsp.getModelName() + "/" + dsp.getVersion();
				}
    			
    			if (chartConfigCollection != null){ 
    				start(chartConfigCollection,name,isViewChartXML);
    				chartFile.delete();
      				for (final Object o : chartConfigCollection) {
    					try {
    						final ChartConfig cc = (ChartConfig) o;
    						final AbstractChart chart = AbstractChart.find(cc.getChartType());
    						final JPanel panel = (JPanel) chart.createChart(cc);
    						final String title = getTitleFromConfig(cc,false);
    						final JFrame fr = new JFrame(title);
    						final JScrollPane sp = new JScrollPane(panel,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    						sp.setBorder(null);
    						fr.setContentPane(sp);
    						fr.pack();
    						Dimension oldD = fr.getPreferredSize();
    						fr.setPreferredSize(new Dimension(oldD.width + sp.getVerticalScrollBar().getWidth(), 
    							            				  oldD.height + sp.getHorizontalScrollBar().getHeight()));
    						sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    						sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    						oldD = fr.getPreferredSize();
    						final Dimension newD = GUIUtils.getPreferredSize(fr);
    						if (!oldD.equals(newD)) 
    							fr.setPreferredSize(newD);
    						fr.pack();    						
    						fr.setVisible(true);
    					} catch (final Throwable ex) {
    						Logger.logError("Unable to create chart");
    						Logger.logException(ex);
    						Logger.logExceptionCallStack(ex);
    					}
    				}
    			}

    		}
    		
    		@Override public void finished() throws Exception {
    			chartFile.delete();
    		}
   		});
	}

	//----------------------------------------------------------------------------------------------------
	private String getTitleFromConfig(final ChartConfig config, final boolean replaceInvalidChars) {
		final Object configObj = config.getChartProperties();
		try {
			if (configObj instanceof Properties) {
				final Properties properties = (Properties) configObj;
				String title = properties.getProperty(ChartConstants.TITLE,"");
				if (replaceInvalidChars)
					title = replaceInvalidChars(title);
				return title;
			} else if (configObj instanceof List) {
				@SuppressWarnings("unchecked") final List<Properties> props = (List<Properties>) configObj;
				String title = props.get(0).getProperty(ChartConstants.TITLE,"");
				if (replaceInvalidChars) 
					title = replaceInvalidChars(title);
				return title;
			}
		} catch (final Exception e) {
			// do nothing
		}
		return "";
	}
	
	//----------------------------------------------------------------------------------------------------
	private String replaceInvalidChars(final String orig) {
		String result = orig.replace(' ','_');
		result = result.replace('\\','_');
		result = result.replace('/','_');
		result = result.replace(':','_');
		result = result.replace('"','_');
		result = result.replace('*','_');
		result = result.replace('?','_');
		result = result.replace('<','_');
		result = result.replace('>','_');
		result = result.replace('|','_');
		result = result.replaceAll("[_]+","_");
		return result;

	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void exportChart(final JPanel panel, final String imgName, final String extension) throws Throwable {
		final File image = new File(MEMEApp.getLastDir(),imgName + extension);
		if (panel instanceof NetworkPanel) 
			((NetworkPanel)panel).saveGraphImage(image);
		else if (".png".equals(extension)) {
			final BufferedImage bufferedImage = new BufferedImage(panel.getWidth(),panel.getHeight(),
																  BufferedImage.TYPE_INT_RGB);
			final Graphics2D g2d = bufferedImage.createGraphics();
			panel.print(g2d);
			g2d.dispose();
			ImageIO.write(bufferedImage,"png",image);
		} else if (".eps".equals(extension)) {
			VectorGraphics g = new PSGraphics2D(image,new Dimension(panel.getWidth(),panel.getHeight()));
			g.startExport();
			panel.print(g);
			g.endExport();
		} else if (".emf".equals(extension)) {
			VectorGraphics g = new EMFGraphics2D(image,new Dimension(panel.getWidth(),panel.getHeight()));
			g.startExport();
			panel.print(g);
			g.endExport();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void changeViewOrResult() {
		if (viewChart)
			changeView();
		else
			changeResult();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void changeView() {
		final ViewsBrowser browser = new ViewsBrowser(false,false);
		browser.valueChanged(null);
		final String viewName = ((ViewDataSources)chartConfigCollection.getDataSources().getDSPCollection()).getViewName();
		if (browser.showInDialog(SwingUtilities.getWindowAncestor(this),null,viewName)) {
			final ViewRec newView = browser.getSelectedView();
			String xml = null;
			try {
				xml = ((CCCollectionDialog)cccdialog).exportToXMLString();
			} catch (final Exception e) {
				MEMEApp.userErrors("Switch view","Error while switching view.","See error log for details.");
				Logger.logExceptionCallStack("ChartsPanel.changeView",e);
			}
			if (xml != null) {
				final ViewRec[] oldView = new ViewRec[1];
				final String newXml = changeXml(xml,newView,oldView);
				
				final Throwable[] error = new Throwable[1];
				final boolean[] applyError = new boolean[1];
				try {
					MEMEApp.LONG_OPERATION.execute("Switching view...",new Runnable() {

						public void run() {
							ChartConfigCollection ccc = null;
							try {
								ccc = ViewDataSources.apply(newXml);
								if (ccc != null) {
									start(ccc,newView.getName(),true);
									chartConfigCollection = ccc;
									chartConfigCollection.setWindowMinWidth(MIN_WIDTH);
								}
							} catch (final Exception e) {
								applyError[0] = true;
								error[0] = e;
								start(chartConfigCollection,oldView[0].getName(),true);
							}
							SwingUtilities.invokeLater(new Runnable() {
									public void run() { jChangeViewButton.transferFocus(); }
							});
						}
						
					});
				} catch (final Exception e) {
					Logger.logExceptionCallStack("ChartsPanel.changeView",e);
				}
				if (error[0] != null) 
					if (applyError[0]) {
						MEMEApp.userAlert("The defined charts cannot be applied to the selected view.","MEME switched back to the original view.");
					} else
						Logger.logExceptionCallStack("ChartsPanel.changeView",error[0]);
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void changeResult() {
		final ResultsBrowser browser = new ResultsBrowser();
		browser.valueChanged(null);
		final ResultDataSources rds = (ResultDataSources) chartConfigCollection.getDataSources().getDSPCollection();
		final String name = rds.getModelName() + "/" + rds.getVersion();
		if (browser.showInDialog(SwingUtilities.getWindowAncestor(this),name)) {
			final Long[][] newResult = browser.getSelection();
			String xml = null;
			try {
				xml = ((CCCollectionDialog)cccdialog).exportToXMLString();
			} catch (final Exception e) {
				MEMEApp.userErrors("Switch result","Error while switching result.","See error log for details.");
				Logger.logExceptionCallStack("ChartsPanel.changeResult",e);
			}
			if (xml != null) {
				
				final Throwable[] error = new Throwable[1];
				final boolean[] applyError = new boolean[1];
				try {
					final String[] oldName = new String[1];
					final String newXml = changeResultXml(xml,newResult,oldName);
					MEMEApp.LONG_OPERATION.execute("Switching result...",new Runnable() {

						public void run() {
							ChartConfigCollection ccc = null;
							try {
								ccc = ResultDataSources.apply(newXml);
								if (ccc != null) {
									final ResultDataSources rds = (ResultDataSources) ccc.getDataSources().getDSPCollection();
									final String name = rds.getModelName() + "/" + rds.getVersion();
									start(ccc,name,false);
									chartConfigCollection = ccc;
									chartConfigCollection.setWindowMinWidth(MIN_WIDTH);
								}
							} catch (final Exception e) {
								applyError[0] = true;
								error[0] = e;
								start(chartConfigCollection,oldName[0],false);
							}
							SwingUtilities.invokeLater(new Runnable() {
									public void run() { jChangeViewButton.transferFocus(); }
							});
						}
						
					});
				} catch (final Exception e) {
					Logger.logExceptionCallStack("ChartsPanel.changeResult",e);
				}
				if (error[0] != null) 
					if (applyError[0]) {
						MEMEApp.userAlert("The defined charts cannot be applied to the selected result.","MEME switched back to the original result.");
					} else
						Logger.logExceptionCallStack("ChartsPanel.changeResult",error[0]);
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private String changeXml(final String xml, final ViewRec newView, final ViewRec[] oldView) {
		final String[] lines = xml.split("\n");
		boolean oldViewSet = false;
		final String prefix = "<property key=\"id\">";
		final String suffix = "</property>";
		for (int i = 0;i < lines.length;++i) {
			lines[i] = lines[i].trim();
			if (lines[i].startsWith(prefix)) {
				final int idx = lines[i].indexOf(suffix);
				final String tmp = lines[i].substring(prefix.length(),idx).trim();
				final String[] parts = tmp.split(";");
				if (!oldViewSet) {
					oldViewSet = true;
					final String[] parts2 = parts[0].split("\\.");
					final long viewId = Long.parseLong(parts2[0]);
					oldView[0] = new ViewRec(parts2[1],viewId);
				}
				lines[i] = prefix + newView.getViewID() + "." + newView.getName() + ";" + parts[1] + suffix;
			}
		}
		return Utils.join("\r\n",(Object[])lines);
	}
	
	//----------------------------------------------------------------------------------------------------
	private String changeResultXml(final String xml, final Long[][] newResult, final String[] oldName) throws Exception {
		final long modelId = newResult[0][0];
		final Model m = (Model) MEMEApp.LONG_OPERATION.execute("Searching model...",new Callable<Object>() {
				public Object call() {
					return MEMEApp.getResultsDb().findModel(modelId);
				}
		});
		
		final List<Long> batchesList = new ArrayList<Long>();
		for (Long[] comb : newResult) {
			if (comb[0] == modelId && comb.length > 1)
				batchesList.add(comb[1]);
		}
		Collections.sort(batchesList);

		final String[] lines = xml.split("\n");
		boolean oldResultSet = false;
		final String prefix = "<property key=\"id\">";
		final String suffix = "</property>";
		for (int i = 0;i < lines.length;++i) {
			lines[i] = lines[i].trim();
			if (lines[i].startsWith(prefix)) {
				final int idx = lines[i].indexOf(suffix);
				final String tmp = lines[i].substring(prefix.length(),idx).trim();
				final String[] parts = tmp.split(";");
				if (!oldResultSet) {
					oldResultSet = true;
					final String[] parts2 = parts[0].split("\\.");
					oldName[0] = parts2[2] + "/" + parts2[3];
				}
				lines[i] = prefix + m.getModel_id() + "." + (batchesList.isEmpty() ? "null" : Utils.join(batchesList,"?")) + "." 
						 + m.getName() + "." + m.getVersion() + ";" + parts[1] + suffix;
			}
		}
		return Utils.join("\r\n",(Object[])lines);
	}
	
	//=========================================================================
	//	GUI (view) methods

	//----------------------------------------------------------------------------------------------------
	/**
	 * This method initializes this
	 */
	private void initialize() {
		setSize(300, 200);
		jViewNamesLabel = new JLabel();
		jViewNamesLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
//		jViewNamesLabel.setBorder(BorderFactory.createEmptyBorder(GUI_unit(0.6),0,GUI_unit(0.6),0));
		
		jChangeViewButton.setMinimumSize(new Dimension(50,26));
		jChangeViewButton.setEnabled(false);
		jChangeViewButton.setActionCommand("CHANGE_VIEW");
		jChangeViewButton.addActionListener(this);
		
		final JPanel p = new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(GUI_unit(0.6),0,GUI_unit(0.6),GUI_unit(1.6)));
		p.add(jChangeViewButton,BorderLayout.EAST);
		p.add(jViewNamesLabel,BorderLayout.CENTER);

		setLayout(new BorderLayout());
//		add(jViewNamesLabel,BorderLayout.NORTH);
		add(p,BorderLayout.NORTH);
		add(getJCenterPanel(),BorderLayout.CENTER);
		MEMEApp.getDatabase().connChanged.addListener(new IConnChangedListener() {
			public void onConnChange(final ConnChangedEvent event) {
				if (panelIsInUse) {
					cccdialog = null;
					getJCenterPanel().removeAll();
					panelIsInUse = false;
					final MainWindowPanelManager pm = MEMEApp.getMainWindow().panelManager;
					pm.setActive(MEMEApp.getMainWindow().getViewsPanel());
					pm.setEnabled(ChartsPanel.this,false);
				}
			}
		});
	}

	//----------------------------------------------------------------------------------------------------
	private JPanel getJCenterPanel() {
		if (jCenterPanel == null) {
			jCenterPanel = new JPanel();
			jCenterPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
			jCenterPanel.addComponentListener(new ComponentAdapter() {
				@Override public void componentHidden(final ComponentEvent e) {
					if (e.getSource() == cccdialog)
						finish(null);
				}
			});
		}
		return jCenterPanel;
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isViewChartXML(final URI uri) throws ParserConfigurationException, SAXException, IOException, XMLLoadingException {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder parser = factory.newDocumentBuilder();
		final Document document = parser.parse(uri.toString());
		final Element dss = XMLUtils.findFirst(document.getDocumentElement(),"datasources");
		final Element firstDs = XMLUtils.findFirst(dss,"datasource");
		final Properties p = ai.aitia.chart.util.Utilities.readProperties(firstDs);
		return !Boolean.parseBoolean(p.getProperty(ResultDataSources.RESULT,"false"));
	}
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	public class FormatChooserDialog extends JDialog implements ActionListener {

		//====================================================================================================
		// members

		private static final long serialVersionUID = 1L;
		
		public static final int OK = 0;
		public static final int CANCEL = 1;
		
		private static final String DEFAULT_TEXT = "Please select one or more from the following image file formats:";  
		
		private final Frame owner;
		private int returnValue = 0;

		private JPanel content = new JPanel(new BorderLayout());
		private JPanel center = null;
		private JTextPane infoPane = new JTextPane();
		private JScrollPane infoScr = new JScrollPane(infoPane);
		private JCheckBox pngBox = new JCheckBox("PNG Image File");
		private JCheckBox emfBox = new JCheckBox("Enhanced Metafile");
		private JCheckBox epsBox = new JCheckBox("Encapsulated Postscript File");
		private JPanel bottom = new JPanel();
		private JButton okButton = new JButton("OK");
		private JButton cancelButton = new JButton("Cancel");
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public FormatChooserDialog(final Frame owner) {
			super(owner,"Format",true);
			this.owner = owner;
			layoutGUI();
			initialize();
		}
		
		//----------------------------------------------------------------------------------------------------
		public int showDialog() {
			setVisible(true);
			return returnValue;
		}
		
		//----------------------------------------------------------------------------------------------------
		public boolean isPNG() { return pngBox.isSelected(); }
		public boolean isEPS() { return epsBox.isSelected(); }
		public boolean isEMF() { return emfBox.isSelected(); }

		//====================================================================================================
		// implemented interfaces

		//----------------------------------------------------------------------------------------------------
		public void actionPerformed(ActionEvent e) {
			final String command = e.getActionCommand();
			if ("PNG".equals(command) || "EPS".equals(command) || "EMF".equals(command)) 
				okButton.setEnabled(pngBox.isSelected() || epsBox.isSelected() || emfBox.isSelected());
			else if ("CANCEL".equals(command)) {
				returnValue = CANCEL;
				setVisible(false);
			} else if ("OK".equals(command)) {
				returnValue = OK;
				setVisible(false);
			}
		}
		
		//====================================================================================================
		// GUI methods
		
		//----------------------------------------------------------------------------------------------------
		private void layoutGUI() {
			
			center = FormsUtils.build("p:g",
					   				  "[DialogBorder]0||" +
					   				  				"1||" +
					                                "2|",
					                  pngBox,
					                  epsBox,
					                  emfBox).getPanel();

			bottom.add(okButton);
			bottom.add(cancelButton);

			Box tmp = new Box(BoxLayout.Y_AXIS);
			tmp.add(infoScr);
			tmp.add(new JSeparator());
			content.add(tmp,BorderLayout.NORTH);
			content.add(center,BorderLayout.CENTER);
			tmp = new Box(BoxLayout.Y_AXIS);
			tmp.add(new JSeparator());
			tmp.add(bottom);
			content.add(tmp,BorderLayout.SOUTH);
			
			this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
			this.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					returnValue = CANCEL;
				}
			});
		}
		
		//----------------------------------------------------------------------------------------------------
		private void initialize() {
			infoScr.setBorder(null);
			
			infoPane.setEditable(false);
			int b = GUIUtils.GUI_unit(0.5);
			infoPane.setBorder(BorderFactory.createEmptyBorder(b,b,b,b));
			Utilities.setTextPane(infoPane,Utils.htmlPage(DEFAULT_TEXT));
			infoPane.setPreferredSize(new Dimension(230,42));

			pngBox.setActionCommand("PNG");
			epsBox.setActionCommand("EPS");
			emfBox.setActionCommand("EMF");
			okButton.setActionCommand("OK");
			cancelButton.setActionCommand("CANCEL");
			
			pngBox.setSelected(true);
			GUIUtils.addActionListener(this,pngBox,epsBox,emfBox,okButton,cancelButton);
			
			final JScrollPane sp = new JScrollPane(content,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			sp.setBorder(null);
			this.setContentPane(sp);
			this.setPreferredSize(new Dimension(230,210)); 
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
			this.setLocationRelativeTo(owner);
		}
	}
}
