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
import java.lang.ref.WeakReference;
import java.net.URI;
import java.text.Collator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import org.w3c.dom.Node;

import ai.aitia.chart.ChartConfigCollection;
import ai.aitia.chart.IDSPCollection;
import ai.aitia.chart.IDataSourceProducer;
import ai.aitia.chart.ds.ConstantColorDataSourceProducer;
import ai.aitia.chart.ds.IElementListProducer;
import ai.aitia.chart.ds.IStringSeriesProducer;
import ai.aitia.chart.util.Utilities;
import ai.aitia.chart.util.XMLLoadingException;
import ai.aitia.visu.data.sequence.Element;
import ai.aitia.visu.ds.AbstractDataProducer;
import ai.aitia.visu.ds.IDataProducer;
import ai.aitia.visu.ds.IObjectProducer;
import ai.aitia.visu.ds.ISeriesProducer;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.gui.lop.LongRunnable;
import ai.aitia.meme.utils.DblArray;
import ai.aitia.meme.utils.Utils;
import static ai.aitia.meme.viewmanager.JTableScrollPane.getColumnTypeIcon;


/**
 * The set of data sources backed by a view table. 
 */
public class ViewDataSources implements IDSPCollection
{
	/** The id of the view table. */
	private volatile long					view_id;
	/** The name of the view table. */
	private volatile String					view_name		= null;
	/** Informations of the columns of the view table. */
	private Columns							columns			= null;
	/** The list of the data source producers created from the columns of the view table. */
	private ArrayList<IDataSourceProducer>	myDataSources	= null;
	/** Cache. We use this object to avoid the frequently database queries. */ 
	private ViewCache						viewCache		= null;
	Object strongReferenceKeeper;	// ez a valtozo azert kell mert viewCache meg nincs hasznalatba veve rendesen

	private static final String ID		= "id";
	private static final String CASE		= "case_sensitive";
	private static final String ASCENDING	= "ascending";
	private static final String LOCALE	= "locale_specific";
	private static final String FMT_VER	= "version";

	/** Special column. Row numbers. */
	private static final String SEQ_LOCALIZED = "#";
	private static final int	  SEQ_IDX		= -1;

	// Az alabbi valtozo ahhoz kell hogy a ViewsDb-ben bekovetkezett modosulasokrol
	// ertesithessuk a mar letrehozott IDataProducer-einket. Ebben tartjuk oket nyilvan.
	// ld. meg addListener() es fireDataChanged() alabb
	//private LinkedList<WeakReference<MyDataProducer>> listeners = new LinkedList<WeakReference<MyDataProducer>>();

	//=========================================================================
	//	Public interface

	//-------------------------------------------------------------------------
	// colInfo-t azert kell megadni, hogy getList()-ig ne kelljen varakozni egy
	// long operation-re sem.
	// [EDT or Model thread]
	/** Constructor.
	 * @param view_id the id of the view table
	 * @param colInfo informations of the columns of the view table
	 */
	public ViewDataSources(long view_id, Columns colInfo) { init(view_id, colInfo); }

	//-------------------------------------------------------------------------
	// Designed for the static load() only - this is why it is private. See load() below. 
	private ViewDataSources() {	}

	//-------------------------------------------------------------------------
	// [EDT or Model thread]
	/** Initializes <code>this</code> object.
	 * @param view_id the id of the view table
	 * @param colInfo informations of the columns of the view table
	 */ 
	private void init(long view_id, Columns colInfo) {
		assert (columns == null && myDataSources == null && viewCache == null);
		this.view_id = view_id;
		columns = colInfo;
		myDataSources = new ArrayList<IDataSourceProducer>();
		viewCache = new ViewCache();

		//ArrayList<ColumnBuffer> buffers = new ArrayList<ColumnBuffer>(columns.size());
		for (int i = 0; i < columns.size(); ++i) {
			myDataSources.add(new ViewDataSource(i));
			//buffers.add(viewCache.getBuffer(i));
		}
		myDataSources.add(new ViewDataSource(SEQ_IDX));
		//buffers.add(viewCache.getBuffer(SEQ_IDX));
		//strongReferenceKeeper = buffers;

		if (view_name == null) {
			MEMEApp.LONG_OPERATION.begin("Querying table name...", new LongRunnable() {
				@Override public void trun() throws Exception {
					if (view_name == null)
						view_name = MEMEApp.getViewsDb().getName(ViewDataSources.this.view_id);
					//long cnt = MEMEApp.getViewsDb().getNrOfRows(ViewDataSources.this.view_id);
					//MEMEApp.LONG_OPERATION.progress(0, cnt);
					//viewCache.load(viewCache.get(0).get());
					//MEMEApp.LONG_OPERATION.checkUserBreak();
				}
			});
		}
	}

	//-------------------------------------------------------------------------
	// [EDT or Model thread]
	public long getViewID() {
		return view_id;
	}

	//-------------------------------------------------------------------------
	// [EDT]
	public String getViewName() {
		return view_name;
	}

	//-------------------------------------------------------------------------
	// [Model thread]
	/** Loads charts configurations from file specified by <code>uri</code>.
	 * @param uri the configuration file
	 * @return the charts collection descriptor object
	 */
	public static ChartConfigCollection load(URI uri) throws Exception {
		return ChartConfigCollection.load(uri, new ViewDataSources());
	}
	
	//----------------------------------------------------------------------------------------------------
	public static ChartConfigCollection apply(final String xml) throws Exception {
		return ChartConfigCollection.imp0rt(xml,new ViewDataSources());
	}

	//=========================================================================
	//	Interface methods

	//-------------------------------------------------------------------------
	public List<IDataSourceProducer> getList() {
		return myDataSources;
	}

	//-------------------------------------------------------------------------
	/** 
	 * This method is public as an implementation side effect. Do not call or override.
	 * Use the static load() method instead!
	 * It is intended to be called from {@link DataSources#load()} only.
	 * Constructs an IDataSourceProducer from an XML node that was created by
	 * {@link ViewDataSource#save()}
	 */
	// [Model thread]
	public IDataSourceProducer load(Node node, Class type) throws Exception {
		assert (!javax.swing.SwingUtilities.isEventDispatchThread());
		IDataSourceProducer ans = null;
		try {
			Properties prop = Utilities.readProperties(node);
			if (prop.containsKey("CONSTANT_COLOR_PRODUCER")) {
				final String colorStr = prop.getProperty("COLOR");
				final Color color = new Color(Integer.decode(colorStr));
				return new ConstantColorDataSourceProducer(color);
			} else {
				// The following parsing corresponds to the format created in ViewDataSource.save()
				// which was different during the history of the program.
				//
				Long id;
				String colName, viewName = null;
				Columns c = null;
				String fmtver = prop.getProperty(FMT_VER, "").toString();
				if (Utils.versionCmp(fmtver, "1.0.70117") <= 0) {
					// id = view_id.colIdx
					String tmp = prop.getProperty(ID, "-1.").toString();
					int i = tmp.indexOf('.');
					id = Long.parseLong(tmp.substring(0, i));
					viewName = MEMEApp.getViewsDb().getName(id);
					colName = tmp.substring(++i);
				} else {
					// id = view_id.view_name;col_name
					String tmp[] = Utils.parseCSVLine(prop.getProperty(ID, "").toString(), ";", '"', Utils.EXC_QUOTE);
					if (tmp.length < 2)
						throw new Exception("Wrong chart configuration");
					id = null;
					viewName = tmp[0];
					colName = tmp[1];
					int i = viewName.indexOf('.');
					if (i >= 0) try {
						id = Long.valueOf(viewName.substring(0, i));
						viewName = viewName.substring(i+1);
					} catch (NumberFormatException nf) {}
					// Keressuk eloszor id szerint, ha ugy nincs, akkor nev szerint is.
					if (id != null) {
						String name = MEMEApp.getViewsDb().getName(id);
						if (name != null)
							viewName = name;
						else
							id = null;
					}
					if (id == null) {
						id = MEMEApp.getViewsDb().findView(viewName);
					}
				}
	
				if (columns == null) {
					c = (id == null) ? null : MEMEApp.getViewsDb().getColumns(id);
					if (c == null) {
						String msg; 
						if (viewName == null)
							msg = "The specified view table does not exist.";
						else
							msg = String.format("The specified view table (%s) does not exist.", viewName);
						throw new Exception(msg);
					}
					if (this.view_name == null)
						this.view_name = viewName;
					init(id, c);
				} else if (id != view_id) {
					// Either this method is called by mistake (instead of the static load()),
					// or the XML refers to more than one view tables 
					throw new IllegalStateException();
				}
	
				int idx = columns.indexOf(colName);
				if (idx < 0) {
					if (SEQ_LOCALIZED.equals(colName))
						idx = SEQ_IDX;					// this is the autogenerated ID column
					else
						return null;					// the specified view does not have a column named 'colName'
				}
				ans = new ViewDataSource(idx, prop);
			}
		} catch (XMLLoadingException e) {
			MEMEApp.logExceptionCallStack("ViewDataSources.load()", e);
		}
		return ans;
	}


	//=========================================================================
	//	Internals
	
	//-------------------------------------------------------------------------
	/** Abstract class that implements some of (but not all) the abstract methods of
	 *  the class AbstractDataProducer.
	 */ 
	static abstract class MyDataProducer extends AbstractDataProducer {
		public long getTime() {
			return System.currentTimeMillis();
		}

	    @Override public void fireDataChanged() {
	        super.fireDataChanged();
	    }		
	}

//	//-------------------------------------------------------------------------
//	void addListener(MyDataProducer l) {
//		for (MyDataProducer o : Utils.iterateAutoRemove(listeners))
//			if (o == l) return;
//		listeners.add(new WeakReference<MyDataProducer>(l));
//	}
//	
//	//-------------------------------------------------------------------------
//	void fireDataChanged() {
//		for (MyDataProducer l : Utils.iterateAutoRemove(listeners))
//			l.fireDataChanged();
//	}

//	int		getViewID()		{ return view_id; }
	

	//-------------------------------------------------------------------------
	/** This class represents the data source producers used by the charting package.
	 *  All column of the view table is one data source producer object.
	 */
	class ViewDataSource implements IDataSourceProducer
	{
		/** 
		 * Index within ViewDataSources.columns[].
		 * The value SEQ_IDX indicates the ID column. It is saved with name==SEQ_LOCALIZED.
		 */
		private int					colIdx;

		/** The data source that this producer object creates. */
		private StringSeriesProducer	dataProducer	= null;

		/** Flag that determines whether use locale when ordering a textual data source or not. */ 
		boolean							locale			= false;
		/** Flag that determines whether the ordering of textual data source is case sensitive or not. */ 
		boolean							case_sensitive	= true;
		/** Flag that determines whether the ordering of textual data source is ascending or not. */ 
		boolean							ascending		= true;
		
		double							scale			= 1.0;
		double							shift			= 0.0;

		//=====================================================================
		//	Public interface

		//---------------------------------------------------------------------
		/** Contructor.
		 * @param index of the column within ViewDataSources.columns[]
		 */
		public ViewDataSource(int idx) {
			colIdx = idx;
			assert (columns != null && (0 <= colIdx && colIdx < columns.size() || colIdx == SEQ_IDX));
		}

		//---------------------------------------------------------------------
		/** Contructor.
		 * @param index of the column within ViewDataSources.columns[]
		 * @param p Properties object that contains the current values of the flags.
		 */
		public ViewDataSource(int idx, Properties p) {
			this(idx);
			case_sensitive	= Boolean.parseBoolean(p.getProperty(CASE, "true"));  
			ascending		= Boolean.parseBoolean(p.getProperty(ASCENDING, "true")); 
			locale			= Boolean.parseBoolean(p.getProperty(LOCALE, "false")); 
		}
		
		//---------------------------------------------------------------------
		@Override
		public boolean equals(Object o) {
			if (o instanceof ViewDataSource) {
				ViewDataSource that = (ViewDataSource)o;
				//TODO: ha t�bb view-b�l lesz, vagy valami hiba arra utal, akkor
				// sz�ks�g lehet a view_id-ra is (ekkor kell ilyen adattag)
				return this.colIdx == that.colIdx;
			}
			return false;
		}

		//=====================================================================
		//	Interface methods

		//---------------------------------------------------------------------
		public List<Class> getSupportedIntfs() {
			return java.util.Arrays.asList(new Class[] {
					ISeriesProducer.class,
					IStringSeriesProducer.class,
					IObjectProducer.class,
					IElementListProducer.class
			} ); 
		}

		//---------------------------------------------------------------------
		public List<Class>			getDisabledIntfs()		{ return null; }
		public String				getDisplayName()		{ return colIdx == SEQ_IDX ? SEQ_LOCALIZED : columns.get(colIdx).getName(); }
		@Override public String		toString()				{ return getDisplayName(); }
		public List<NumberStrPair>	getElements() 			{ return getDataProducer().getElements(); }
		public List<Double>			getRange() 				{ return getDataProducer().getRange(); }
		public boolean				hasAdvancedSettings()	{ return false; }
		public IDataSourceProducer	advancedSettingsDialog(Component parent)	{ return null; }
		public IDataProducer		createDataProducer(Class intf, IDataSourceProducer grp)	{ return getDataProducer(); }
		public javax.swing.ImageIcon getDisplayableIcon() {
			ColumnType dt = (colIdx == SEQ_IDX) ? ColumnType.INT : columns.get(colIdx).getDatatype();
			return getColumnTypeIcon(dt); 
		}

		//---------------------------------------------------------------------
		// [EDT]
		public void save(Node node) {
			Properties p = new Properties();
			// id = view_id.view_name;column_name
			String id = getViewID() + "." + getViewName();
			id = Utils.composeCSVLine(";", true, id, getDisplayName());
			p.put(ID, id);
			p.put(FMT_VER, MEMEApp.CURRENT_VERSION);
			p.put(CASE, case_sensitive);
			p.put(ASCENDING, ascending);
			p.put(LOCALE, locale);
			Utilities.writeProperties(node, p);
		}

		//---------------------------------------------------------------------
		protected StringSeriesProducer getDataProducer() {
			if (dataProducer == null) {
				dataProducer = new StringSeriesProducer();
//				addListener(dataProducer);
			}
			return dataProducer;
		}


		//=====================================================================
		/** The data source class. It implements all types of data sources. */
		class StringSeriesProducer extends MyDataProducer implements ISeriesProducer, 
																	 IStringSeriesProducer,
																	 IObjectProducer,
																	 IElementListProducer
		{
			/** The buffer of the represented column. */ 
			ColumnBuffer		buffer = viewCache.getBuffer(colIdx);
			/** The list of the number values (for textual columns these are order numbers. */
			List<Double>		numbers = null;
			/** The list of the string values (for number columns these are string representations. */
			ArrayList<String>	strings = null;
			/** The elements of the column. (Only one occurance for each element. */
			List<NumberStrPair>	elements = null;
			/** The minimal and maximal elements of the column. */
			List<Double>		range = null;

			public String getName() {
				return getDisplayName();
			}

			// ISeriesProducer interface
			@SuppressWarnings("unchecked")
			public List<Double> produceSeries() {
				if (numbers == null) {
					Object data = buffer.getData();
					if (buffer.isNumeric) {
						double[] temp = ((DblArray)data).toArray(); 
						numbers = new ArrayList<Double>(temp.length);
						for (int i=0;i<temp.length;++i) numbers.add(new Double(temp[i]));
						strings = null;
						buffer  = null;
					} else {															// ok
						strings = (ArrayList<String>)data;
						buffer  = null;
						numbers = generateNumbers(ViewDataSource.this, strings);
					}
				}
				// TODO: itt lehetne scale & shift
				return numbers;
			}

			// IStringSeriesProducer interface
			public String getNumberName(double number) {								// ???
				if (numbers == null)
					produceSeries();

				if (isNumeric()) {
					if (number == (int)number)
						return String.valueOf((int)number);
					return Double.valueOf(number).toString();
				} else
					// TODO: scale & shift visszaszamolasa
					return strings.get((int)number);
			}

			// IElementListProducer interface
			public List<Element> produceElementList() {
				if (numbers == null)
					produceSeries();
				
				ArrayList<Element> ans = new ArrayList<Element>(numbers.size());
				for (Double d : numbers) 
					ans.add(new Element(d.doubleValue()));
				return ans;
			}
			
			// IObjectProducer interface
			public Object produceObject() {
				if (numbers == null)
					produceSeries();

				ArrayList<String> ans = new ArrayList<String>(numbers.size());			// ???
				for (Double d : numbers) 
					ans.add(getNumberName(d.doubleValue()));
				return ans;
			}

			@Override
			public void fireDataChanged() {											// ???
				numbers = null; strings = null; elements = null; range = null;
				buffer = viewCache.getBuffer(colIdx);
				super.fireDataChanged();
			}

			/** Returns whether the data source is numeric or not. */
			boolean isNumeric() {
				return (numbers == null) ? buffer.isNumeric : (strings == null);
			}

			/* 
			 * A visszaadott elemekben elofordulhat hogy NumberStrPair.toString()==null.
			 * Ez olyankor fordul elo, amikor isNumeric()==false es strings[]-ben  
			 * vannak null ertekek.
			 */
			List<NumberStrPair> getElements() {
				if (elements == null) {													// ???
					List<Double> numbers = produceSeries();	
					if (numbers != null && numbers.size() > 0) {
						TreeSet<NumberStrPair> tmp = new TreeSet<NumberStrPair>();
						for (Double val : numbers) {
							tmp.add(new NumberStrPair(val, getNumberName(val.doubleValue())));
						}
						elements = new ArrayList<NumberStrPair>(tmp);
					}
				}
				return elements;
			}

			List<Double> getRange() {
				if (range == null) {
					List<Double> numbers = produceSeries();
					if (numbers != null && numbers.size() > 0) {						// ok
						Double minmax[] = { Double.MAX_VALUE, -Double.MAX_VALUE };
						for (Double d : numbers) {
							if (d < minmax[0]) minmax[0] = d;
							if (d > minmax[1]) minmax[1] = d;
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
	class ColumnBuffer {
		/** Index of the column. */
		int					colIdx;		// may be SEQ_IDX

		/** Flag that determines whether the column is numeric or not. */
		final boolean		isNumeric;
		/** The data of the column. */
		private Object		data;
		private double		nullNumber = 0;	// TODO: make it configurable in the Advanced dialog!

		/** Constructor.
		 * @param colIdx index of the column.
		 */
		ColumnBuffer(int colIdx) {
			this.colIdx = colIdx;
			ColumnType t = (colIdx == SEQ_IDX) ? ColumnType.INT : columns.get(colIdx).getDatatype();
			isNumeric = (ColumnType.BOOLEAN.equals(t)
						|| ColumnType.DOUBLE.equals(t)
						|| ColumnType.INT.equals(t) 
						|| ColumnType.LONG.equals(t));
		}

		@SuppressWarnings("unchecked")
		boolean	 isEmpty() {
			if (data == null) return true;
			return (isNumeric) ? ((DblArray)data).isEmpty() : ((ArrayList<String>)data).isEmpty(); 
		}
		
		@SuppressWarnings("unchecked")
		int	size() {
			if (data == null) return 0;
			return (isNumeric) ? ((DblArray)data).size() : ((ArrayList<String>)data).size();
		}

		/** Clears the data from the buffer. */
		void		clear()		{ data = null; }
		@Override
		public int hashCode()	{ return Integer.valueOf(colIdx).hashCode(); }
		@Override
		public boolean equals(Object obj) {
			if (super.equals(obj)) return true;
			if (obj instanceof ColumnBuffer) return (((ColumnBuffer)obj).colIdx == colIdx);
			if (obj instanceof Integer) return ((Integer)obj == colIdx);
			return false;
		}

		/** Returns the data of the column. If <code>data</code> is null, it loads 
		 *  first to the cache object. Returns either a DblArray or an ArrayList&lt;String&gt;
		 */
		Object getData() {
			if (isEmpty()) {
				if (!java.awt.EventQueue.isDispatchThread())
					viewCache.load(this);
				else if (data == null)
					data = (isNumeric) ? new DblArray() : new ArrayList<String>();
			}
			return data;
		}

		/** Adds value to the buffer from the row <code>row</code>
		 * @param row a row of the view table
		 */
		@SuppressWarnings("unchecked")
		void addValueFrom(GeneralRow row) {															// ???
			assert(row.getColumns().equals(columns)) : new Object[] { columns, row.getColumns() };
			Object val = (colIdx == SEQ_IDX) ? size() : row.get(colIdx);
			if (isNumeric) {
				DblArray numbers;
				if (data == null)
					data = numbers = new DblArray();
				else
					numbers = (DblArray)data;

				if (val == null)
					numbers.add(nullNumber);
				else if (val instanceof Boolean)
					numbers.add(((Boolean)val).booleanValue() ? 1 : 0);
				else
					numbers.add(((Number)val).doubleValue());
			} else {
				ArrayList<String> strings;
				if (data == null)
					data = strings = new ArrayList<String>();
				else
					strings = (ArrayList<String>)data;
				strings.add(val == null ? null : val.toString());
			}
		}

	}

	
	//=========================================================================
	/** Cache class to store data from view table. We use this to avoid the frequently
	 *  and unnecessary database queries.  
	 */
	@SuppressWarnings("serial")
	class ViewCache	 extends ArrayList<WeakReference<ColumnBuffer>>
	{
		/** Flag that determines whether need to reload the cached data or not. */
		boolean needReload = false;

		@Override
		public int indexOf(Object elem) {
			if (elem != null)
				for (int i = size() - 1; i >= 0; --i) {
					ColumnBuffer cb = get(i).get();
					if (cb == null) remove(i);
					else if (cb.equals(elem)) return i;
				}
			return -1;
		}

		/** Creates a new buffer or returns an existing one for the specified column.
		/*  Note: colIdx may be SEQ_IDX
		 * @param colIdx the index of the column
		 * @return the buffer of the specified column
		 */
		ColumnBuffer getBuffer(int colIdx) { 
			ColumnBuffer ans;
			int i = indexOf(colIdx);
			if (i < 0) {
				ans = new ColumnBuffer(colIdx);
				add(new WeakReference<ColumnBuffer>(ans));
				needReload = true;
			} else {
				ans = get(i).get();
			}
			return ans;
		}

		// [Model thread]
		/** Reloads the view and fills all buffers, including the specified one. 
		/*  Precondition: <code>cb</code> is already in this cache and <code>cb.isEmpty()</code>.
		 * @param cb an empty column buffer
		 */
		void load(ColumnBuffer cb) {
			assert(cb.isEmpty() && this.contains(cb));		// contains() uses indexOf()
			if (!needReload)
				return;
			Utils.WeakIter<ColumnBuffer> it = Utils.iterateAutoRemove(this);
			for (ColumnBuffer c : it)
				c.clear();

			long nRows = MEMEApp.getViewsDb().getNrOfRows(view_id), progress = 0;
			System.gc();									// clear weak references
			String savedTaskName = MEMEApp.LONG_OPERATION.getTaskName();
			MEMEApp.LONG_OPERATION.setTaskName("Loading view data...");
			MEMEApp.LONG_OPERATION.progress(progress, nRows);
			for (GeneralRow row : MEMEApp.getViewsDb().getRows(view_id, 0, -1)) {
				for (ColumnBuffer c : it) {
					c.addValueFrom(row);
				}
				if (MEMEApp.LONG_OPERATION.isUserBreak())
					break;
				MEMEApp.LONG_OPERATION.progress(++progress, -1);
			}
			needReload = false;
			MEMEApp.LONG_OPERATION.setTaskName(savedTaskName);
		}
	}

	//-------------------------------------------------------------------------
	/** Modifies the content of <code>strings</code>. Sorts the list into lexicographic
	 *  order and eliminates any duplications. The return value contains the indices of 
	 *  the elements of the modified <code>strings</codes> list. The order of the return
	 *  list is the same than the original order of <code>strings</code> list.<br>
	 *  Example:<br>
	 *  Original list: b,c,a,b<br>
	 *  Modified list: a,b,c<br>
	 *  Return list: 1,2,0,1<br>
	 * @param strings may contain null values!
	 */
	static List<Double> generateNumbers(ViewDataSource ds, ArrayList<String> strings) {
		MyStrComparator cmp = new MyStrComparator(ds.case_sensitive, ds.ascending, ds.locale);
		// I assume that 'strings' contains many duplications. Therefore I put the
		// strings into a hashmap first, because this eliminates the duplications, 
		// and then sort this smaller number of strings. This is especially important
		// when locale-specific collation is requested, because Collator.compare()
		// is very slow.
		LinkedHashMap<String, Double> hash = new LinkedHashMap<String, Double>();
		Double dummy = 0.;
		for (String s : strings) hash.put(s, dummy);
		ArrayList<String> ord = new ArrayList<String>(hash.keySet());
		java.util.Collections.sort(ord, cmp);
		cmp = null;
		int i = 0;
		for (String s : ord) hash.put(s, new Double(i++));
		List<Double> ans = new ArrayList<Double>(strings.size());
		for (String s : strings) ans.add(hash.get(s));
		hash = null;
		strings.clear();
		strings.addAll(ord);
		ord = null;
		strings.trimToSize();
		return ans;
	}

	//-------------------------------------------------------------------------
	/** String compararator class. Works for nulls, too */ 
	public static class MyStrComparator implements java.util.Comparator<String> {
		boolean		case_sensitive	= true;
		boolean		ascending		= true;
		boolean		nullsAtFirst	= false;
		Collator	locale			= null;

		public MyStrComparator(boolean case_sensitive, boolean ascending, boolean locale) {
			this.case_sensitive	= case_sensitive;
			this.ascending		= ascending;
			if (locale)
				this.locale = Collator.getInstance();
			if (this.locale != null && !case_sensitive)
				this.locale.setStrength(Collator.PRIMARY);
		}

		public int compare(String o1, String o2) {
			int ans;
			if (o1 == null && o2 == null) return 0;
			if (o1 == null) return nullsAtFirst ? -1 :  1;
			if (o2 == null) return nullsAtFirst ?  1 : -1;
			if (locale != null)
				ans = locale.compare(o1, o2);
			else 
				ans = (case_sensitive) ? o1.compareTo(o2) : o1.compareToIgnoreCase(o2);
			return (ascending) ? ans : -ans;
		}
	}

}
