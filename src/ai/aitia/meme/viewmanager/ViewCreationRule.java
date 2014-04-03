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
package ai.aitia.meme.viewmanager;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ViewRec;
import ai.aitia.meme.pluginmanager.Parameter;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.XMLUtils;

//-----------------------------------------------------------------------------
// See the XML design at the end of this file.
/** This class represents an xml-based description of a view. */ 
public class ViewCreationRule implements Cloneable
{
	public static final String NODENAME			= "viewrule";
	public static final String VERSION_ATTR		= "version";

	public static final String NAME				= "name";
	public static final String DESCRIPTION		= "description";
	
	public static final String INPUT_DATA			= "data";
	public static final String INPUT_TABLE		= "table";
	public static final String INPUT_MODELID_ATTR	= "modelid";
	public static final String INPUT_BATCH_ATTR	= "batch";
	public static final String INPUT_RUN_ATTR		= "run";
	public static final String INPUT_VIEWID_ATTR	= "viewid";
	public static final String INPUT_VIEWNAME_ATTR= "name";
	public static final String INPUT_MODELNAME_ATTR 	= "name";
	public static final String INPUT_MODELVERSION_ATTR	= "version";

	public static final String COLUMNS			= "columns";
	public static final String ORDERATTR			= "ordering";
	public static final String GROUPINGATTR		= "grouping";
	
	public static final String COLUMN				= "column";
	public static final String COL_INITIAL_TYPE	= "datatype";
	public static final String COL_GRP_ATTR		= "grouping";
	public static final String COL_HIDDEN_ATTR	= "hidden";
	public static final String COL_AGGR_FN_ATTR	= "aggrfn";
	public static final String COL_SPLITTER_ATTR	= "splitter";
	public static final String COL_NAME			= "name";
	public static final String COL_SPLITTED		= "splitted";
	public static final String COL_SPLIT_DELIM	= "delim";
	public static final String COL_PROJECTION		= "projection";
	public static final String COL_PROJ_SRC_ATTR	= "source";
	public static final String COL_PROJ_INPUT		= "input";
	public static final String COL_PROJ_OUTPUT	= "output";
	public static final String COL_PROJ_VIEW		= "view";

	public static final String CONDITION		= "condition";

	/** Root XML node. */
	public final Element node;

	//-------------------------------------------------------------------------
	public ViewCreationRule()					{ this(XMLUtils.getDefaultXMLParser().newDocument()); }
	public ViewCreationRule(Document doc)		{ node = doc.createElement(NODENAME);
												  node.setAttribute(VERSION_ATTR, MEMEApp.CURRENT_VERSION);
												}
	public ViewCreationRule(Element node)		{ this.node = node; }
	public ViewCreationRule(String s) throws Exception { this(versionMigration(s)); }
	@Override public ViewCreationRule clone()	{ return new ViewCreationRule((Element)node.cloneNode(true)); }

	// For debugging purposes
	@Override public String toString() {
		try { return XMLUtils.toString(node); }
		catch (Exception e) { throw new RuntimeException(e); }
	}
	/** Trims and returns 's'. */
	public static String trim(String s)		{ return (s == null) ? null : s.trim(); }

	//-------------------------------------------------------------------------
	/** Returns the name of the view. */
	public String getName()						{ return XMLUtils.getTextField(node, NAME).trim(); }
	/** Sets the name of the view. */
	public void setName(String name)			{ XMLUtils.setTextField(node, NAME, name); }

	//-------------------------------------------------------------------------
	/** Returns the description of the view. */
	public String getDescription()				{ return XMLUtils.getTextField(node, DESCRIPTION).trim(); }
	/** Sets the description of the view. */
	public void setDescription(String desc)	{ XMLUtils.setTextField(node, DESCRIPTION, desc); }

	//-------------------------------------------------------------------------
	/** 
	 * Elements of the returned array are either Long[] or ViewRec objects.
	 * Long[] specifies model_id, batch#, run# (the last two are optional);
	 * ViewRec specifies the viewid and name of a view table. 
	 */ 
	public ArrayList<Object> getInputTables() {
		ArrayList<Object> ans = new ArrayList<Object>(); 		
		Element tables = XMLUtils.findFirst(node, INPUT_DATA);
		if (tables != null) {
			for (Element table : XMLUtils.findAll(tables, INPUT_TABLE)) {
				String viewid = table.getAttribute(INPUT_VIEWID_ATTR);
				if (viewid != null && viewid.length() > 0) {
					ans.add(new ViewRec(table.getAttribute(INPUT_VIEWNAME_ATTR), Long.valueOf(viewid)));
				} else {
					ArrayList<Long> tmp = new ArrayList<Long>(3);
					appendLong(tmp, table.getAttribute(INPUT_MODELID_ATTR));
					appendLong(tmp, table.getAttribute(INPUT_BATCH_ATTR));
					appendLong(tmp, table.getAttribute(INPUT_RUN_ATTR));
					if (!tmp.isEmpty())
						ans.add(tmp.toArray(new Long[tmp.size()]));
				}
			}
		}
		return ans;
	}
	/** 
	 * Elements of the collection are either Long[] or ViewRec objects.
	 * Long[] specifies model_id, batch#, run# (the last two are optional);
	 * ViewRec specifies the viewid and name of a view table. 
	 */ 
	public void setInputTables(java.util.Collection<?> inputs) {
		Document d = node.getOwnerDocument();
		Element tables = XMLUtils.findFirst(node, INPUT_DATA);
		if (tables != null) {
			tables.setTextContent(null);
		} else {
			tables = d.createElement(INPUT_DATA);
			node.appendChild(tables);
		}
		for (Object o : inputs) {
			if (o == null) continue;
			Element table = d.createElement(INPUT_TABLE);
			if (o instanceof Long[]) {
				Long[] tmp = (Long[])o;
				if (tmp.length > 0) table.setAttribute(INPUT_MODELID_ATTR, tmp[0].toString());
				if (tmp.length > 1) table.setAttribute(INPUT_BATCH_ATTR, tmp[1].toString());
				if (tmp.length > 2) table.setAttribute(INPUT_RUN_ATTR, tmp[2].toString());
			} else {
				ViewRec rec = (ViewRec)o;
				table.setAttribute(INPUT_VIEWID_ATTR, Long.toString(rec.getViewID()));
				table.setAttribute(INPUT_VIEWNAME_ATTR, rec.getName());
			}
			tables.appendChild(table);
		}
	}
	/** Adds the Long value of 's' to the 'array'. */
	private static void appendLong(ArrayList<Long> array, String s) { 
		if (s != null && s.length() > 0)
			array.add(new Long(s));
	}
	
	//-------------------------------------------------------------------------
	// [Model thread]
	/** Transforms the view creation rule to portable format. Returns the root of
	 *  the modified XML-document. 
	 */
	public Element transformRuleToSave() {
		Element node = (Element)this.node.cloneNode(true);
		List<Element> tables = XMLUtils.findAll(node,INPUT_TABLE);
		for (Element table : tables) { 
			table.removeAttribute(INPUT_VIEWID_ATTR);
			String model_id = table.getAttribute(INPUT_MODELID_ATTR);
			if (model_id != null && model_id.length() != 0) {
				final long mid = Long.parseLong(model_id);
				Model model = MEMEApp.getResultsDb().findModel(mid);
				if (model == null)
					throw new IllegalStateException("'null' model reference in view " + getName());
				table.removeAttribute(INPUT_MODELID_ATTR);
				table.setAttribute(INPUT_MODELNAME_ATTR,model.getName());
				table.setAttribute(INPUT_MODELVERSION_ATTR,model.getVersion());
			}
		}
		return node;
	}
	
	//-------------------------------------------------------------------------
	// [Model thread]
	/** Transforms view creation rule from the portable format to the internal representation
	 *  format. 
	 * @param node root of the portable document
	 * @param view_id id of the view belongs to the rule
	 * @return node
	 */
	public Element restoreRule(Element node, long view_id) {
		Element columns = XMLUtils.findFirst(node,COLUMNS);
		List<Element> columnList = XMLUtils.findAll(columns,COLUMN);
		Columns info = null;
		for (Element column : columnList) {
			String datatype = column.getAttribute(COL_INITIAL_TYPE);
			if (datatype == null || datatype.length() == 0) {
				if (info == null)
					info = MEMEApp.getViewsDb().getColumns(view_id);
				String name = XMLUtils.getTextField(column,COL_NAME);
				String columnType = null;
				for (Parameter p : info) {
					if (p.getName().equals(name)) {
						columnType = p.getDatatype().toString();
						break;
					}
				}
				if (columnType == null) {
					String projection = XMLUtils.getTextField(column,COL_PROJECTION);
					Element tables = XMLUtils.findFirst(node,INPUT_DATA);
					List<Element> tableList = XMLUtils.findAll(tables,INPUT_TABLE);
					TABLES:
					for (Element table : tableList) {
						String tableName = table.getAttribute(NAME);
						String tableVersion = table.getAttribute(VERSION_ATTR);
						if (null != tableVersion && !"".equals(tableVersion)) {
							long id = MEMEApp.getResultsDb().findModel(tableName,tableVersion).getModel_id();
							Columns[] cols_array = MEMEApp.getResultsDb().getModelColumns(id);
							for (Columns cols : cols_array) {
								for (Parameter p : cols) {
									if (p.getName().equals(projection)) {
										columnType = p.getDatatype().toString();
										break TABLES;
									}
								}
							}
						} else {
							long id = findView(tableName);
							if (id != -1) {
								Columns cols = MEMEApp.getViewsDb().getColumns(id);
								for (Parameter p : cols) {
									if (p.getName().equals(projection)) {
										columnType = p.getDatatype().toString();
										break TABLES;
									}
								}
							}  
						}
					}
				}
				if (columnType != null) 
					column.setAttribute(COL_INITIAL_TYPE,columnType);
			}
		}
		return node;
	}
	
	private long findView(String name) {
		List<ViewRec> views = MEMEApp.getViewsDb().getViews();
		long id = -1;
		for (ViewRec vr : views) {
			if (vr.getName().equals(name)) {
				id = vr.getViewID();
				break;
			}
		}
		return id;
	}
	
	//-------------------------------------------------------------------------
	// [Model thread]
	/** Changes the name of the view. */
	public void changeName(String old, String upd) {
		Element columns = XMLUtils.findFirst(this.node,COLUMNS);
		List<Element> columnList = XMLUtils.findAll(columns,COLUMN);
		for (Element column : columnList) {
			String name = XMLUtils.getTextField(column,COL_NAME);
			if (old.equals(name)) {
				Element nameElement = XMLUtils.findFirst(column,COL_NAME);
				nameElement.setTextContent(upd);
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void changeViewName(String newName) {
		Element name = XMLUtils.findFirst(this.node,NAME);
		name.setTextContent(newName);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void changeReference(String old, String upd) {
		Element data = XMLUtils.findFirst(this.node,INPUT_DATA);
		List<Element> tables = XMLUtils.findAll(data,INPUT_TABLE);
		for (Element table : tables) {
			String viewName = table.getAttribute(INPUT_VIEWNAME_ATTR);
			if (viewName != null && old.equals(viewName))
				table.setAttribute(INPUT_VIEWNAME_ATTR,upd);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void changeReference(final String oldName, final String oldVersion, final String updName,  final String updVersion) {
		final Element data = XMLUtils.findFirst(this.node,INPUT_DATA);
		final List<Element> tables = XMLUtils.findAll(data,INPUT_TABLE);
		for (final Element table : tables) {
			final String modelName = table.getAttribute(INPUT_MODELNAME_ATTR);
			final String modelVersion = table.getAttribute(INPUT_MODELVERSION_ATTR);
			if (modelName != null && oldName.equals(modelName) && modelVersion != null && oldVersion.equals(oldVersion)) {
				table.setAttribute(INPUT_MODELNAME_ATTR,updName);
				table.setAttribute(INPUT_MODELVERSION_ATTR,updVersion);
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public Column findColumn(final String name) {
		if (name == null) return null;
		final String _name = name.trim();
		final Element columns = XMLUtils.findFirst(node,COLUMNS);
		if (columns != null) {
			for (Element column : XMLUtils.findAll(columns,COLUMN)) {
				final Column c = new Column(column);
				if (c.getName().equals(_name))
					return c;
			}
		}
		return null;
	}

	//-------------------------------------------------------------------------
	/** This class represent a column in a creation rule. */
	public static class Column implements Cloneable {
		/** root of the XML-document belongs to the creation rule.*/
		protected Element node;

		//public Column()							{ this(XMLUtils.getDefaultXMLParser().newDocument()); }
		public Column(ViewCreationRule owner)	{ this(owner.node.getOwnerDocument()); }
		public Column(Document doc)				{ this(doc.createElement(COLUMN)); }
		public Column(Element node)				{ this.node = node; }
		@Override public Column clone()			{ return new Column((Element)node.cloneNode(true)); }

		/** Precondition: this Column is not part of the columns list, or 'name' is unique within that list */
		public void setName(String name)		{ XMLUtils.setTextField(node, COL_NAME, name); }
		public String getName()					{ return XMLUtils.getTextField(node, COL_NAME).trim(); }

		/** Returns true if this column is used in grouping: its values are used when forming groups of rows */
		public boolean isGrouping()			{ return Utils.getBooleanValue(node.getAttribute(COL_GRP_ATTR)); }
		public void setGrouping(Boolean b)		{ node.setAttribute(COL_GRP_ATTR, b.toString()); }
		/** Returns true if this column does not require vector-mode calculation */
		public boolean isScalar()				{ return isGrouping();  }
		
		public boolean hasScalarScriptTag()	{ return XMLUtils.findFirst(node,SrcType.SCALAR_SCRIPT.toString()) != null;	}

		public boolean isHidden()				{ return Utils.getBooleanValue(node.getAttribute(COL_HIDDEN_ATTR)); }
		public void setHidden(Boolean b)		{ node.setAttribute(COL_HIDDEN_ATTR, b.toString()); }

		public boolean isSplitter()			{ return Utils.getBooleanValue(node.getAttribute(COL_SPLITTER_ATTR)); }
		public void setSplitter(Boolean b)		{ node.setAttribute(COL_SPLITTER_ATTR, b.toString()); }

		public String getAggrFn()				{ return node.getAttribute(COL_AGGR_FN_ATTR).trim(); }
		public void setAggrFn(String fn)		{ if (fn == null || fn.length() == 0) node.removeAttribute(COL_AGGR_FN_ATTR);
												  else node.setAttribute(COL_AGGR_FN_ATTR, fn); }

		public boolean isSplitted()			{ return (getSplitted() != null); }
		/** Returns the splitted delimiter string. */
		public String getSplitted()				{ return XMLUtils.getTextField(node, COL_SPLITTED, true); }
		public void setSplitted(String delim)	{ XMLUtils.setTextField(node, COL_SPLITTED, delim); }

		/** 
		 * "Initial data type" is only used when creating a view table.
		 * Note: this information becomes redundant when the view table is created (since the 
		 * datatype will be stored in the VIEW_COLUMNS mapping table, too). Therefore this field
		 * will be ignored after the view table is created, and the actual column type will be 
		 * used instead (from the database, the VIEW_COLUMNS column mapping table). 
		 * It is used during the creation/re-creation (i.e. for creation) only.
		 * (This is why projected columns do not have this kind of datatype at all: it is cleared
		 *  before saving).
		 */
		public ColumnType getInitialDataType()	{
			String tmp = node.getAttribute(COL_INITIAL_TYPE);
			if (tmp != null && tmp.length() > 0)
				return new ColumnType(tmp);
			// Return "factory default" datatype:
			ArrayList<Utils.Pair<SrcType, String>> src = getSource();
			if (!src.isEmpty() && src.get(0).getFirst().isProjection())
				return null;				// Projected columns don't have "initial datatype" by default
			return ColumnType.STRING;		// default datatype for computed columns
		}
		public ColumnType setInitialDataType(ColumnType dataType) {
			if (dataType == null)
				node.removeAttribute(COL_INITIAL_TYPE);
			else 
				node.setAttribute(COL_INITIAL_TYPE, dataType.toString());
			return dataType;
		}
		
		/** Returns a list that contains (Column source type, column source name) pairs.
		 *  Name of a script is also appeared instead a column source name 
		 * 
		 * @see SrcType SrcType
		 */
		public ArrayList<Utils.Pair<SrcType, String>> getSource() {
			SrcType t;
			ArrayList<Utils.Pair<SrcType, String>> ans = new ArrayList<Utils.Pair<SrcType, String>>();
			for (Element e : XMLUtils.findAll(node, COL_PROJECTION)) {
				String type = e.getAttribute(COL_PROJ_SRC_ATTR).trim();
				if (COL_PROJ_INPUT.equals(type))		t = SrcType.PROJECTION_INPUT;
				else if (COL_PROJ_OUTPUT.equals(type))	t = SrcType.PROJECTION_OUTPUT;
				else if (COL_PROJ_VIEW.equals(type))	t = SrcType.PROJECTION_VIEW;
				else continue;
				ans.add(new Utils.Pair<SrcType, String>(t, XMLUtils.getText(e).trim()));
			}
			if (ans.isEmpty()) {
				t = SrcType.SCALAR_SCRIPT;
				String src = XMLUtils.getTextField(node, t.toString(), true);
				if (src == null) {
					t = SrcType.GROUP_SCRIPT;
					src = XMLUtils.getTextField(node, t.toString(), true);
				}
				if (src != null)
					ans.add(new Utils.Pair<SrcType, String>(t, src.trim()));
			}
			return ans;
		}
		public void setSource(java.util.Collection<Utils.Pair<SrcType, String>> src) {
			XMLUtils.removeAll(node, COL_PROJECTION);
			XMLUtils.removeAll(node, SrcType.SCALAR_SCRIPT.toString());
			XMLUtils.removeAll(node, SrcType.GROUP_SCRIPT.toString());
			Document d = node.getOwnerDocument();
			Element e;
			assert(!src.isEmpty());
			for (Utils.Pair<SrcType, String> p : src) {
				switch (p.getFirst()) {
				case PROJECTION_INPUT : 
					e = d.createElement(COL_PROJECTION); e.setAttribute(COL_PROJ_SRC_ATTR, COL_PROJ_INPUT); break;
				case PROJECTION_OUTPUT : 
					e = d.createElement(COL_PROJECTION); e.setAttribute(COL_PROJ_SRC_ATTR, COL_PROJ_OUTPUT); break;
				case PROJECTION_VIEW : 
					e = d.createElement(COL_PROJECTION); e.setAttribute(COL_PROJ_SRC_ATTR, COL_PROJ_VIEW); break;
				default:
					e = d.createElement(p.getFirst().toString()); break;
				}
				XMLUtils.setText(e, p.getSecond());
				node.appendChild(e);
			}
		}

		/** Returns the return type of the script that created the column. */
		public ColumnType getScriptDataType() {
			for (Element e : XMLUtils.findAll(node, SrcType.SCALAR_SCRIPT.toString())) {
				String tmp = e.getAttribute(COL_INITIAL_TYPE);
				if (tmp != null && tmp.length() > 0)
					return new ColumnType(tmp);
				break;
			}
			return getInitialDataType();
		}
		public void setScriptDataType(ColumnType dt) {
			for (Element e : XMLUtils.findAll(node, SrcType.SCALAR_SCRIPT.toString())) {
				e.setAttribute(COL_INITIAL_TYPE, dt.toString());
				return;
			}
			setInitialDataType(dt);
		}

		@Override public int hashCode()	{ return getName().hashCode(); }		
		@Override public boolean equals(Object obj) {
			if (super.equals(obj)) return true;
			if (obj instanceof Column) return getName().equals(((Column)obj).getName()); 
			return getName().equals(obj);
		}
		@Override public String toString() {		// For debugging purposes
			try { return XMLUtils.toString(node); }
			catch (Exception e) { throw new RuntimeException(e); }
		}
	}

	//-------------------------------------------------------------------------
	/** Returns all columns of the rule. */
	public ArrayList<Column> getColumns() {	
		ArrayList<Column> ans = new ArrayList<Column>(); 		
		Element columns = XMLUtils.findFirst(node, COLUMNS);
		if (columns != null) {
			for (Element column : XMLUtils.findAll(columns, COLUMN))
				ans.add(new Column(column));
		}
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Removes all columns of the rule. */
	public void clearColumns() {
		Element columns = XMLUtils.findFirst(node, COLUMNS);
		if (columns != null) {
			columns.setTextContent(null);
		}
	}

	//-------------------------------------------------------------------------
	/** Adds a column to the rule. */
	public void addColumn(Column col) {
		Element columns[] = { null };
		if (findColumnsNode(columns)) {
			// If there's already a column with this name, overwrite it
			String name = col.getName();
			for (Element column : XMLUtils.findAll(columns[0], COLUMN))
				if (name.equals(XMLUtils.getTextField(column, COL_NAME, true))) {
					columns[0].replaceChild(col.node, column);
					return;
				}
		}
		columns[0].appendChild(col.node);
	}

	//-------------------------------------------------------------------------
	/** Returns false if newly created. */
	private boolean findColumnsNode(Element[] columns_ref) {
		Element columns = XMLUtils.findFirst(node, COLUMNS);
		boolean ans = (columns != null);
		if (!ans) {
			columns = node.getOwnerDocument().createElement(COLUMNS);
			node.appendChild(columns);
		}
		if (columns_ref != null && columns_ref.length > 0)
			columns_ref[0] = columns;
		return ans;
	}

	//-------------------------------------------------------------------------
	public boolean getGrouping() {
		boolean ans = false;
		Element columns = XMLUtils.findFirst(node, COLUMNS);
		if (columns != null)
			ans = Utils.getBooleanValue(columns.getAttribute(GROUPINGATTR));
		return ans;
	}
	public void setGrouping(boolean b) {
		Element columns[] = { null };
		findColumnsNode(columns);
		columns[0].setAttribute(GROUPINGATTR, Boolean.valueOf(b).toString());
	}

	//-------------------------------------------------------------------------
	/** Returns null if there's no condition */
	public String getCondition() {
		String ans = null;
		Element condition = XMLUtils.findFirst(node, CONDITION);
		if (condition != null)
			ans = trim(XMLUtils.getTextField(condition, SrcType.SCALAR_SCRIPT.toString(), true));
		return ans;
	}
	public void setCondition(String condition) {
		Element condNode = XMLUtils.clear(node, CONDITION);
		XMLUtils.setTextField(condNode, SrcType.SCALAR_SCRIPT.toString(), condition);
	}

	//-------------------------------------------------------------------------
	/** Returns an empty array if there's no ordering */
	public int[] getOrdering() {
		int[] ans = {};
		Element e = XMLUtils.findFirst(node, COLUMNS);
		if (e != null) {
			String s = e.getAttribute(ORDERATTR);
			if (s != null && s.length() > 0) {
				String[] tmp = s.split(" ");
				ans = new int[tmp.length];
				for (int i = 0; i < tmp.length; ++i)
					ans[i] = Integer.parseInt(tmp[i]);
			}
		}
		return ans;
	}
	public void setOrdering(int[] order) {
		Element columns[] = { null };
		findColumnsNode(columns);
		if (order == null || order.length == 0) {
			columns[0].removeAttribute(ORDERATTR);
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(order[0]);
			for (int i = 1; i < order.length; ++i) { sb.append(' '); sb.append(order[i]); }
			columns[0].setAttribute(ORDERATTR, sb.toString());
		}
	}

	//-------------------------------------------------------------------------
	/** Transforms a rule from an older version to the current version. */
	private static Element versionMigration(String str) throws Exception {
		Element ans = null;

		// Get version number
		String version = "1.0.70220";
		if (0 > Utils.left(str, 64).indexOf('<' + NODENAME + '>')) {
			// There's some chance for finding the version attribute  
			ans = XMLUtils.parse(str);
			if (ans != null) {
				String s = ans.getAttribute(VERSION_ATTR).trim();
				if (s != null && s.length() > 0)
					version = s;
			}
		}

		if (Utils.versionCmp(version, "1.0.70221") < 0) {
			// dbcolumn_input -> projection_input
			// dbcolumn_output -> projection_output
			str = str.replaceAll("</DBCOLUMN_(INPUT|OUTPUT)>", "</"+COL_PROJECTION+">");
			str = str.replaceAll("<DBCOLUMN_INPUT>", String.format("<%s %s=\"%s\">", 
					COL_PROJECTION, COL_PROJ_SRC_ATTR, COL_PROJ_INPUT
			));
			str = str.replaceAll("<DBCOLUMN_OUTPUT>", String.format("<%s %s=\"%s\">", 
					COL_PROJECTION, COL_PROJ_SRC_ATTR, COL_PROJ_OUTPUT
			));
			ans = XMLUtils.parse(str);
			ans.setAttribute(VERSION_ATTR, version);
		}

		return ans;
	}

}

//-----------------------------------------------------------------------------
/* XML design:

Since 1.0.70221:

<viewrule version="1.0.70221">
  <name> name of the view </name>
  <description> narrative text </description>
  <data>
     <table modelid="1161713073337" />                    <!-- all results from all batches of a model -->
     <table modelid="1161714829325" batch="12" />         <!-- all results from one particular batch -->
     <table modelid="1161715564297" batch="12" run="3" /> <!-- one particular result -->
     <table viewid="2" name="view3" />                    <!-- view tables are referenced primarily by id then by name -->
     <!-- ilyenekb�l ak�rmennyi lehet -->
  </data>
  <columns ordering="0 1" grouping="true">
        <!-- Az 'ordering' attrib�tum opcion�lis. A sz�mok az al�bb felsorolt
             oszlopok sorsz�mai, 0 az els� oszlop. Alap�rtelmez�se �res.
             Negat�v �rt�kek NOT-olva �rtend�k �s cs�kken� rendez�st jelentenek:
             i < 0 a -i-1. oszlopot jelenti (pl. -1 az els� oszlopot jelenti, 
             -2 a m�sodikat stb).
             A 'grouping' attrib�tum �rt�ke logikai (true/false), alap�rtelmez�s false.  
          -->
     <column datatype="12.64.0" grouping="false" hidden="false" aggrfn="AVG" splitter="false">
   			<!-- datatype="12.64.0" means varchar(64). This datatype string is 
   			     generated/interpreted by ColumType.
   			     Note: this information becomes redundant when the view table is created
   			     (since the datatype will be recorded in the VIEW_COLUMNS mapping table).
   			     Therefore this field will be ignored after the view table is created. 
   			     It is used during the creation/re-creation (i.e. for creation).
	      	  --> 
   			<!-- grouping=true es aggrfn<>"" kizarjak egymast -->
   			<!-- splitter=true eseten kotelezoen grouping=true --> 
        <name> colX </name>     <!-- ez a neve a view tablaban -->
        <splitted>_</splitted>  <!-- ez a tag csak akkor van, ha bontott oszlop.
                                     Ilyenkor kotelezoen hidden=false es grouping=false --> 

		<!-- projection_* -tagokbol tobb is lehet, de akkor *_script tag nincs
		     *_script tagbol csak 1 lehet -->

        <projection source="input"> anInputColumn </projection>
        <projection source="output"> anOutputColumn </projection>
        <projection source="view"> previousColumnName </projection>

        <scalar_script datatype="..."> beanshell expression </scalar_script>
           <!-- 'datatype'-ot csak olyankor hasznaljuk amikor aggrfn!="" --> 
        <group_script> beanshell expression </group_script>
           <!-- group_script eseten kotelezoen grouping=false es aggrfn="" --> 

     </column>
  </columns>
  <condition>
     <scalar_script> col1 == 3 </scalar_script>
  </condition>
</viewrule>

Before 1.0.70221:

<viewrule>
  <name> name of the view </name>
  <description> narrative text </description>
  <data>
     1161713073337      <!-- all results from all batches of a model -->
     1161714829325.12   <!-- all results from one particular batch -->
     1161715564297.12.3 <!-- one particular result -->
     <!-- ilyenekb�l ak�rmennyi lehet -->
  </data>
  <columns ordering="0 1">
        <!-- az 'ordering' attrib�tum opcion�lis. A sz�mok az al�bb felsorolt
             oszlopok sorsz�mai, 0 az els� oszlop -->
     <column datatype="4.0.0">		               <!-- DBCOLUMN columns don't have 'datatype' attribute -->
        <name> col1 </name>
		<!-- Exactly one of the followings -->
        <DBCOLUMN_INPUT> anInputColumn </DBCOLUMN_INPUT>
        <DBCOLUMN_OUTPUT> anOutputColumn </DBCOLUMN_OUTPUT>
     </column>
  </columns>
</viewrule>


 */

