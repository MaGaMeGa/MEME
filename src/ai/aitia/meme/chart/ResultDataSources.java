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
package ai.aitia.meme.chart;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import javax.swing.ImageIcon;

import org.w3c.dom.Node;

import ai.aitia.chart.ChartConfigCollection;
import ai.aitia.chart.DataSources;
import ai.aitia.chart.IDSPCollection;
import ai.aitia.chart.IDataSourceProducer;
import ai.aitia.chart.ds.ConstantColorDataSourceProducer;
import ai.aitia.chart.ds.IElementListProducer;
import ai.aitia.chart.ds.IStringSeriesProducer;
import ai.aitia.chart.util.Utilities;
import ai.aitia.chart.util.XMLLoadingException;
import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.chart.ViewDataSources.MyStrComparator;
import ai.aitia.meme.chart.ViewDataSources.ViewDataSource;
import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.Result.Row;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.utils.DblArray;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.viewmanager.JTableScrollPane;
import ai.aitia.visu.data.sequence.Element;
import ai.aitia.visu.ds.AbstractDataProducer;
import ai.aitia.visu.ds.IDataProducer;
import ai.aitia.visu.ds.IObjectProducer;
import ai.aitia.visu.ds.ISeriesProducer;

public class ResultDataSources implements IDSPCollection {
	
	//====================================================================================================
	// members

	private static final String ID = "id";
	private static final String CASE = "case_sensitive";
	private static final String ASCENDING = "ascending";
	private static final String LOCALE = "locale_specific";
	public static final String RESULT = "result";
	
	private volatile long modelId;
	private volatile long[] batches; // null means all batches
	
	private volatile String	modelName = null;
	private volatile String version = null;
	
	private Columns inputColumns = null;
	private Columns outputColumns = null;
	
	private ArrayList<IDataSourceProducer>	dataSources	= null;
	private ResultCache inputResultCache = null;
	private ResultCache outputResultCache = null;
	
	private static final int RUN_IDX = -1;
	private static final int TICK_IDX = -2;
	private static final String RUN_LOCALIZED = "Run";
	private static final String TICK_LOCALIZED = "Tick";

	//=========================================================================
	//	methods

	//----------------------------------------------------------------------------------------------------
	public ResultDataSources(final long modelId, final long[] batches, final Columns inputColumns, final Columns outputColumns) {
		init(modelId,batches,inputColumns,outputColumns);
	}

	//-------------------------------------------------------------------------
	// Designed for the static load() only - this is why it is private. See load() below. 
	private ResultDataSources() {	}

	//-------------------------------------------------------------------------
	// [EDT or Model thread]
	private void init(final long modelId, final long[] batches, final Columns inputColumns, final Columns outputColumns) {
		this.modelId = modelId;
		this.batches = batches;
		this.inputColumns = inputColumns;
		this.outputColumns = outputColumns;
		dataSources = new ArrayList<IDataSourceProducer>();
		inputResultCache = new ResultCache(true);
		outputResultCache = new ResultCache(false);

		for (int i = 0;i < inputColumns.size();++i) 
			dataSources.add(new ResultDataSource(i,true));
		dataSources.add(new ResultDataSource(RUN_IDX,true));
		for (int i = 0;i < outputColumns.size();++i)
			dataSources.add(new ResultDataSource(i,false));
		dataSources.add(new ResultDataSource(TICK_IDX,false));

		if (modelName == null || version == null) {
			MEMEApp.LONG_OPERATION.begin("Querying table name...", new LongRunnable() {
				@Override public void trun() throws Exception {
					if (modelName == null || version == null) {
						final Model m = MEMEApp.getResultsDb().findModel(modelId);
						if (m != null) {
							modelName = m.getName();
							version = m.getVersion();
						}
					}
				}
			});
		}
	}

	//-------------------------------------------------------------------------
	// [EDT or Model thread]
	public long getModelID() { return modelId; }

	//-------------------------------------------------------------------------
	// [EDT]
	public String getModelName() { return modelName; }
	
	//----------------------------------------------------------------------------------------------------
	// [EDT]
	public String getVersion() { return version; }
	
	//----------------------------------------------------------------------------------------------------
	// [EDT]
	public long[] getBatches() { return batches; }

	//-------------------------------------------------------------------------
	// [Model thread]
	public static ChartConfigCollection load(URI uri) throws Exception {
		return ChartConfigCollection.load(uri,new ResultDataSources());
	}
	
	//----------------------------------------------------------------------------------------------------
	public static ChartConfigCollection apply(final String xml) throws Exception {
		return ChartConfigCollection.imp0rt(xml,new ResultDataSources());
	}

	//=========================================================================
	//	implemented interfaces

	//-------------------------------------------------------------------------
	public List<IDataSourceProducer> getList() { return dataSources; }

	//-------------------------------------------------------------------------
	/** 
	 * This method is public as an implementation side effect. Do not call or override.
	 * Use the static load() method instead!
	 * It is intended to be called from {@link DataSources#load()} only.
	 * Constructs an IDataSourceProducer from an XML node that was created by
	 * {@link ViewDataSource#save()}
	 */
	// [Model thread]
	public IDataSourceProducer load(final Node node, final Class type) throws Exception {
		assert (!javax.swing.SwingUtilities.isEventDispatchThread());
		IDataSourceProducer ans = null;
		try {
			Properties prop = Utilities.readProperties(node);
			if (prop.containsKey("CONSTANT_COLOR_PRODUCER")) {
				final String colorStr = prop.getProperty("COLOR");
				final Color color = new Color(Integer.decode(colorStr));
				return new ConstantColorDataSourceProducer(color);
			} else {
				long id = -1, batch[];
				String colName, modelName, version = null;
				Columns ic = null, oc = null;

				// ID = modelId.<batches delimited by ?>.modelName.version;col_name
				String tmp[] = Utils.parseCSVLine(prop.getProperty(ID,"").toString(),";",'"',Utils.EXC_QUOTE);
				if (tmp.length < 2)
					throw new Exception("Wrong chart configuration");
				id = -1;
				modelName = tmp[0];
				colName = tmp[1];
				int i = modelName.indexOf('.');
				if (i >= 0) {
					try {
						id = Long.valueOf(modelName.substring(0,i));
					} catch (final NumberFormatException nf) {}
					tmp = modelName.substring(i + 1).split("\\.");
					if (tmp.length < 3)
						throw new Exception("Wrong chart configuration");
					modelName = tmp[1];
					version = tmp[2];
					if ("null".equals(tmp[0]))
						batch = null;
					else {
						tmp = tmp[0].split("\\?");
						batch = new long[tmp.length];
						for (int j = 0;j < tmp.length;++j)
							batch[j] = Long.valueOf(tmp[j]);
					}
				}
					
				// Keressuk eloszor id szerint, ha ugy nincs, akkor nev szerint is.
				if (id >= 0) { 
					Model m = MEMEApp.getResultsDb().findModel(id);
					if (m != null) {
						modelName = m.getName();
						version = m.getVersion();
					} else
						id = -1;
					if (id < 0) {
						m = MEMEApp.getResultsDb().findModel(modelName,version);
						if (m != null)
							id = m.getModel_id();
					}
				}
	
				if (inputColumns == null || outputColumns == null) {
					ic = (id < 0) ? null : MEMEApp.getResultsDb().getModelColumns(id)[0];
					oc = (id < 0) ? null : MEMEApp.getResultsDb().getModelColumns(id)[1];
					if (ic == null || oc == null) {
						String msg; 
						if (modelName == null || version == null)
							msg = "The specified result table does not exist.";
						else
							msg = String.format("The specified result table (%s) does not exist.",modelName + "/" + version);
						throw new Exception(msg);
					}
					if (this.modelName == null)
						this.modelName = modelName;
					if (this.version == null)
						this.version = version;
					init(id,batches,ic,oc);
				} else if (id != modelId) {
					// Either this method is called by mistake (instead of the static load()),
					// or the XML refers to more than one result tables 
					throw new IllegalStateException();
				}
				
				boolean input = true;
				int idx = inputColumns.indexOf(colName);
				if (idx < 0) {
					input = false;
					idx = outputColumns.indexOf(colName);
					if (idx < 0) {
						if (RUN_LOCALIZED.equals(colName)) {
							idx = RUN_IDX;
							input = true;
						} else if (TICK_LOCALIZED.equals(colName))
							idx = TICK_IDX;
					} else 
						return null; // the specified result does not have a column named 'colName'
				}
				ans = new ResultDataSource(idx,input,prop);
			}
		} catch (XMLLoadingException e) {
			Logger.logExceptionCallStack("ResultDataSources.load()",e);
		}
		return ans;
	}

	//=========================================================================
	//	internals

	//-------------------------------------------------------------------------
	class ResultDataSource implements IDataSourceProducer {
		
		//====================================================================================================
		// members
		
		private int colIdx;
		private boolean input;
		private ResultStringSeriesProducer dataProducer = null;
		boolean	locale = false;
		boolean	case_sensitive	= true;
		boolean	ascending = true;

		//=====================================================================
		//	methods

		//---------------------------------------------------------------------
		public ResultDataSource(final int idx, final boolean input) {
			colIdx = idx;
			this.input = input;
		}

		//---------------------------------------------------------------------
		public ResultDataSource(final int idx, final boolean input, final Properties p) {
			this(idx,input);
			case_sensitive = Boolean.parseBoolean(p.getProperty(CASE, "true"));  
			ascending = Boolean.parseBoolean(p.getProperty(ASCENDING, "true")); 
			locale = Boolean.parseBoolean(p.getProperty(LOCALE, "false")); 
		}
		
		//---------------------------------------------------------------------
		@Override
		public boolean equals(Object o) {
			if (o instanceof ResultDataSource) {
				ResultDataSource that = (ResultDataSource) o;
				return this.colIdx == that.colIdx && this.input == that.input;
			}
			return false;
		}

		//=====================================================================
		//	implemented interfaces

		//---------------------------------------------------------------------
		public List<Class> getSupportedIntfs() {
			return Arrays.asList(new Class[] { ISeriesProducer.class, IStringSeriesProducer.class, IObjectProducer.class, IElementListProducer.class }); 
		}

		//----------------------------------------------------------------------------------------------------
		public String getDisplayName() {
			if (colIdx == RUN_IDX)
				return RUN_LOCALIZED;
			if (colIdx == TICK_IDX)
				return TICK_LOCALIZED;
			return (input ? inputColumns.get(colIdx) : outputColumns.get(colIdx)).getName();
		}
		//---------------------------------------------------------------------
		public List<Class>			getDisabledIntfs() { return null; }
		@Override public String		toString() { return getDisplayName(); }
		public List<NumberStrPair>	getElements() { return getDataProducer().getElements(); }
		public List<Double>			getRange() { return getDataProducer().getRange(); }
		public boolean				hasAdvancedSettings() { return false; }
		public IDataSourceProducer	advancedSettingsDialog(Component parent) { return null; }
		public IDataProducer		createDataProducer(Class intf, IDataSourceProducer grp)	{ return getDataProducer(); }
		
		//----------------------------------------------------------------------------------------------------
		public ImageIcon getDisplayableIcon() {
			ColumnType dt = null;
			if (colIdx == RUN_IDX || colIdx == TICK_IDX)
				dt = ColumnType.INT;
			else
				dt = (input ? inputColumns.get(colIdx) : outputColumns.get(colIdx)).getDatatype();
			return JTableScrollPane.getColumnTypeIcon(dt); 
		}

		//---------------------------------------------------------------------
		// [EDT]
		public void save(Node node) {
			Properties p = new Properties();
			// ID = modelId.<batches delimited by ?>.modelName.version;col_name

			String id = getModelID() + "." + getBatchesStr() + "." + getModelName() + "." + getVersion();
			id = Utils.composeCSVLine(";",true,id,getDisplayName());
			p.put(ID,id);
			p.put(CASE,case_sensitive);
			p.put(ASCENDING,ascending);
			p.put(LOCALE,locale);
			p.put(RESULT,String.valueOf(true));
			Utilities.writeProperties(node,p);
		}

		//---------------------------------------------------------------------
		protected ResultStringSeriesProducer getDataProducer() {
			if (dataProducer == null) 
				dataProducer = new ResultStringSeriesProducer();
			return dataProducer;
		}
		
		//----------------------------------------------------------------------------------------------------
		protected String getBatchesStr() {
			if (getBatches() == null) 
				return "null";
			long[] orig = getBatches();
			Long[] tmp = new Long[orig.length];
			for (int i = 0;i < orig.length;++i)
				tmp[i] = orig[i];
			return Utils.join("?",(Object[])tmp);
		}


		//=====================================================================
		// nested classes
		
		/** The data source class. It implements all types of data sources. */
		class ResultStringSeriesProducer extends AbstractDataProducer implements ISeriesProducer,
																				 IStringSeriesProducer,
																				 IObjectProducer,
																				 IElementListProducer {

			//====================================================================================================
			// members
			
			ResultColumnBuffer buffer = (input ? inputResultCache : outputResultCache).getBuffer(colIdx);
			List<Double>		numbers = null;
			ArrayList<String>	strings = null;
			List<NumberStrPair>	elements = null;
			List<Double>		range = null;
			
			//====================================================================================================
			// methods
			
			//----------------------------------------------------------------------------------------------------
			public long getTime() { return System.currentTimeMillis(); }
			public String getName() { return getDisplayName(); }

			//----------------------------------------------------------------------------------------------------
			// ISeriesProducer interface
			@SuppressWarnings("unchecked")
			public List<Double> produceSeries() {
				if (numbers == null) {
					Object data = buffer.getData();
					if (buffer.isNumeric) {
						double[] temp = ((DblArray)data).toArray(); 
						numbers = new ArrayList<Double>(temp.length);
						for (int i = 0;i < temp.length;++i)
							numbers.add(new Double(temp[i]));
						strings = null;
						buffer = null;
					} else {															// ok
						strings = (ArrayList<String>) data;
						buffer  = null;
						numbers = generateNumbers(ResultDataSource.this,strings);
					}
				}
				return numbers;
			}

			//----------------------------------------------------------------------------------------------------
			// IStringSeriesProducer interface
			public String getNumberName(double number) {								// ???
				if (numbers == null)
					produceSeries();

				if (isNumeric()) {
					if (number == (int)number)
						return String.valueOf((int)number);
					return Double.valueOf(number).toString();
				} else
					return strings.get((int)number);
			}

			//----------------------------------------------------------------------------------------------------
			// IElementListProducer interface
			public List<Element> produceElementList() {
				if (numbers == null)
					produceSeries();
				
				ArrayList<Element> ans = new ArrayList<Element>(numbers.size());
				for (final Double d : numbers) 
					ans.add(new Element(d.doubleValue()));
				return ans;
			}
			
			//----------------------------------------------------------------------------------------------------
			// IObjectProducer interface
			public Object produceObject() {
				if (numbers == null)
					produceSeries();

				ArrayList<String> ans = new ArrayList<String>(numbers.size());			// ???
				for (final Double d : numbers) 
					ans.add(getNumberName(d.doubleValue()));
				return ans;
			}

			//----------------------------------------------------------------------------------------------------
			@Override
			public void fireDataChanged() {											// ???
				numbers = null; strings = null; elements = null; range = null;
				buffer = (input ? inputResultCache : outputResultCache).getBuffer(colIdx);
				super.fireDataChanged();
			}

			//----------------------------------------------------------------------------------------------------
			boolean isNumeric() { return (numbers == null) ? buffer.isNumeric : (strings == null); }

			//----------------------------------------------------------------------------------------------------
			List<NumberStrPair> getElements() {
				if (elements == null) {													// ???
					List<Double> numbers = produceSeries();	
					if (numbers != null && numbers.size() > 0) {
						TreeSet<NumberStrPair> tmp = new TreeSet<NumberStrPair>();
						for (Double val : numbers) 
							tmp.add(new NumberStrPair(val,getNumberName(val.doubleValue())));
						elements = new ArrayList<NumberStrPair>(tmp);
					}
				}
				return elements;
			}

			//----------------------------------------------------------------------------------------------------
			List<Double> getRange() {
				if (range == null) {
					List<Double> numbers = produceSeries();
					if (numbers != null && numbers.size() > 0) {						// ok
						Double minmax[] = { Double.MAX_VALUE, -Double.MAX_VALUE };
						for (Double d : numbers) {
							if (d < minmax[0])
								minmax[0] = d;
							if (d > minmax[1])
								minmax[1] = d;
						}
						range = java.util.Arrays.asList(minmax);
					}
				}
				return range;
			}
		}
	}


	//=========================================================================
	/** Buffer class for columns. */
	class ResultColumnBuffer {
		
		//====================================================================================================
		// members
		
		int	colIdx;
		boolean input;

		final boolean isNumeric;
		private Object data;
		private double nullNumber = 0;
		
		//====================================================================================================
		// methods

		//----------------------------------------------------------------------------------------------------
		ResultColumnBuffer(final boolean input, final int colIdx) {
			this.input = input;
			this.colIdx = colIdx;
			ColumnType t = null;
			if (colIdx == RUN_IDX || colIdx == TICK_IDX)
				t = ColumnType.INT;
			else
				t = (input ? inputColumns.get(colIdx) : outputColumns.get(colIdx)).getDatatype();
			isNumeric = (ColumnType.BOOLEAN.equals(t) || ColumnType.DOUBLE.equals(t) || ColumnType.INT.equals(t) || ColumnType.LONG.equals(t));
		}

		//----------------------------------------------------------------------------------------------------
		@SuppressWarnings("unchecked")
		boolean	 isEmpty() {
			if (data == null) return true;
			return (isNumeric) ? ((DblArray)data).isEmpty() : ((ArrayList<String>)data).isEmpty(); 
		}
		
		//----------------------------------------------------------------------------------------------------
		@SuppressWarnings("unchecked")
		int	size() {
			if (data == null) return 0;
			return (isNumeric) ? ((DblArray)data).size() : ((ArrayList<String>)data).size();
		}

		//----------------------------------------------------------------------------------------------------
		void clear() { data = null; }
		@Override public int hashCode()	{ return 42 * Integer.valueOf(colIdx).hashCode() + Boolean.valueOf(input).hashCode(); }
		
		//----------------------------------------------------------------------------------------------------
		@Override
		public boolean equals(Object obj) {
			if (super.equals(obj)) return true;
			if (obj instanceof ResultColumnBuffer) {
				final ResultColumnBuffer that = (ResultColumnBuffer) obj;
				return this.colIdx == that.colIdx && this.input == that.input;
			}
			return false;
		}

		//----------------------------------------------------------------------------------------------------
		Object getData() {
			if (isEmpty()) {
				if (!EventQueue.isDispatchThread()) {
					if (input)
						inputResultCache.load(this);
					else
						outputResultCache.load(this);
				}
				else if (data == null)
					data = isNumeric ? new DblArray() : new ArrayList<String>();
			}
			return data;
		}

		/** Adds value to the buffer from the row <code>row</code>
		 * @param row a row of the view table
		 */
		@SuppressWarnings("unchecked")
		void addValueFrom(GeneralRow row, int batchId, int runId, int tickId) {															// ???
			Object val = null;
			if (colIdx == RUN_IDX)
				val = runId;
			else if (colIdx == TICK_IDX)
				val = tickId;
			else
				val = row.get(colIdx);
			int multiplier = 1;
			if (input) 
				multiplier = MEMEApp.getResultsDb().getNumberOfRows(modelId,new Long[] { new Long(batchId), new Long(runId) });
			
			if (isNumeric) {
				DblArray numbers;
				if (data == null)
					data = numbers = new DblArray();
				else
					numbers = (DblArray)data;

				if (val == null) {
					for (int i = 0;i < multiplier;++i)
						numbers.add(nullNumber);
				} else if (val instanceof Boolean) {
					for (int i = 0;i < multiplier;++i)
						numbers.add(((Boolean)val).booleanValue() ? 1 : 0);
				} else {
					for (int i = 0;i < multiplier;++i)
						numbers.add(((Number)val).doubleValue());
				}
			} else {
				ArrayList<String> strings;
				if (data == null)
					data = strings = new ArrayList<String>();
				else
					strings = (ArrayList<String>)data;
				for (int i = 0;i < multiplier;++i)
					strings.add(val == null ? null : val.toString());
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	class ResultCache extends ArrayList<WeakReference<ResultColumnBuffer>>	{
		
		//====================================================================================================
		// members
		
		final boolean input;
		boolean needReload = false;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public ResultCache(final boolean input) {
			this.input = input;
		}
		
		//----------------------------------------------------------------------------------------------------
		@Override
		public int indexOf(final Object elem) {
			if (elem != null)
				for (int i = size() - 1; i >= 0; --i) {
					ResultColumnBuffer cb = get(i).get();
					if (cb == null)
						remove(i);
					else if (cb.equals(elem))
						return i;
				}
			return -1;
		}

		//----------------------------------------------------------------------------------------------------
		ResultColumnBuffer getBuffer(final int colIdx) { 
			ResultColumnBuffer ans;
			int i = indexOf(colIdx);
			if (i < 0) {
				ans = new ResultColumnBuffer(input,colIdx);
				add(new WeakReference<ResultColumnBuffer>(ans));
				needReload = true;
			} else 
				ans = get(i).get();
			return ans;
		}

		//----------------------------------------------------------------------------------------------------
		// [Model thread]
		void load(ResultColumnBuffer cb) {
			if (!needReload)
				return;
			Utils.WeakIter<ResultColumnBuffer> it = Utils.iterateAutoRemove(this);
			for (ResultColumnBuffer c : it)
				c.clear();

			System.gc();									// clear weak references
			String savedTaskName = MEMEApp.LONG_OPERATION.getTaskName();
			long nRows = 0, progress = 0;
			if (batches == null)
				nRows = MEMEApp.getResultsDb().getNumberOfRows(modelId,null);
			else {
				for (long b : batches)
					nRows += MEMEApp.getResultsDb().getNumberOfRows(modelId,new Long[] { b });
			}
			
			MEMEApp.LONG_OPERATION.setTaskName("Loading result data...");
			MEMEApp.LONG_OPERATION.progress(progress,nRows);

			if (batches == null)
				progress = loadFromIterator(MEMEApp.getResultsDb().getResults(modelId,null),it,progress);
			else {
				for (long b : batches) {
					progress = loadFromIterator(MEMEApp.getResultsDb().getResults(modelId,new Long[] { b }),it,progress);
					if (progress < 0) break;
				}
			}
				
			needReload = false;
			MEMEApp.LONG_OPERATION.setTaskName(savedTaskName);
		}
	
		//----------------------------------------------------------------------------------------------------
		private long loadFromIterator(final List<Result> results, final Utils.WeakIter<ResultColumnBuffer> wit, long progress) {
			OUTER_SPACE:
			for (final Result r : results) {
				if (input) {
					for (final ResultColumnBuffer c : wit)
						c.addValueFrom(r.getParameterComb().getValues(),r.getBatch(),r.getRun(),0);
					if (MEMEApp.LONG_OPERATION.isUserBreak()) {
						progress = -1;
						break OUTER_SPACE;
					}
				} else {
					for (final Row row : r.getAllRows()) {
						for (final ResultColumnBuffer c : wit)
							c.addValueFrom(row,r.getBatch(),r.getRun(),row.getTick());
						if (MEMEApp.LONG_OPERATION.isUserBreak()) {
							progress = -1;
							break OUTER_SPACE;
						}
						MEMEApp.LONG_OPERATION.progress(++progress,-1);
					}
				}
			}
			return progress;
		}
	}

	//-------------------------------------------------------------------------
	static List<Double> generateNumbers(ResultDataSource ds, ArrayList<String> strings) {
		MyStrComparator cmp = new MyStrComparator(ds.case_sensitive,ds.ascending,ds.locale);
		LinkedHashMap<String,Double> hash = new LinkedHashMap<String,Double>();
		Double dummy = 0.;
		for (String s : strings)
			hash.put(s,dummy);
		ArrayList<String> ord = new ArrayList<String>(hash.keySet());
		Collections.sort(ord,cmp);
		cmp = null;
		int i = 0;
		for (String s : ord)
			hash.put(s,new Double(i++));
		List<Double> ans = new ArrayList<Double>(strings.size());
		for (String s : strings)
			ans.add(hash.get(s));
		hash = null;
		strings.clear();
		strings.addAll(ord);
		ord = null;
		strings.trimToSize();
		return ans;
	}
}
