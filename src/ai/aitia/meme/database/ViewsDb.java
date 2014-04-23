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
package ai.aitia.meme.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.XMLUtils;
import ai.aitia.meme.viewmanager.ViewCreationRule;
import ai.aitia.meme.database.AbstractResultsDb.ChangeAction;
import ai.aitia.meme.database.ColumnType.ValueNotSupportedException;
import ai.aitia.meme.database.LocalAPI.AlterColumns;
import ai.aitia.meme.events.Event;
import static ai.aitia.meme.database.SQLDialect.release;


/**
 * The views database.
 *  
 * This implementation is not thread-safe. It should be used in the Model thread only.
 */
public class ViewsDb implements IViewsDbMinimal, IConnChangedListener 
{

	//=========================================================================
	// The viewsDbChanged event


	//-------------------------------------------------------------------------
	/** See the description of IViewsDbChangeListener.onViewsDbChange() */
    public final Listeners viewsDbChanged = new Listeners();

	//-------------------------------------------------------------------------
	/** Storage for listeners which observe view modification events. */ 
    public static class Listeners extends Event<IViewsDbChangeListener, ViewsDbChangeEvent> {
		public Listeners() { super(IViewsDbChangeListener.class, ViewsDbChangeEvent.class); }
		@Override protected void fire(ViewsDbChangeEvent msg) { super.fire(msg); }
	}

    //-------------------------------------------------------------------------
    /** Interface for listening view modification events. */
	public interface IViewsDbChangeListener extends java.util.EventListener {
		/** 
		 * This method is called when a view is (re)created, appended or deleted.
		 * Note: may be called in any thread (usually in the %Model thread)!
		 */
		public void onViewsDbChange(ViewsDbChangeEvent e);
	}

    //-------------------------------------------------------------------------
	/** 
	 * This class represents a modification in the Views database.
	 * The following modifications can be represented:<br>
	 * - a view is created:  getAction() == ADDED<br>
	 * - a view is appended: getAction() == MODIFIED<br>  
	 * - a view is deleted:  getAction() == REMOVED
	 */
    @SuppressWarnings("serial")
	public static class ViewsDbChangeEvent extends java.util.EventObject
	{
    	private final ChangeAction		action;
    	private ViewRec					view;

		public ViewsDbChangeEvent(Object source, ChangeAction a, ViewRec view) {
			super(source);
			this.action = a;
			this.view = view;
		}

		@Override public AbstractResultsDb	getSource()	{ return (AbstractResultsDb)super.getSource(); }
		public ChangeAction			getAction()			{ return action; }
		public long					getViewID()			{ return view.getViewID(); }
		public String				getViewName()		{ return view.getName(); }
		public ViewRec				getViewRec()		{ return view; }
	}



	//=========================================================================
	// IViewsDbMinimal methods

    //---------------------------------------------------------------------
	public Long findView(String name) {
		Long ans = null;
		PreparedStatement ps = null;
		try {
	    	ps = getConnection().prepareStatement("SELECT View_id FROM VIEWS WHERE Name=?");
	    	ps.setString(1, name);				// Columns are numbered from 1
	    	ResultSet rs = ps.executeQuery();
	    	if (rs.next())
	    		ans = rs.getLong(1);
	    	rs.close();
	    	ps.close(); ps = null;
		} catch (SQLException e) {
			Logger.logException("ViewsDb.findView()", e);
			MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
		} finally {
			release(ps);
		}
		return ans;
	}

    //---------------------------------------------------------------------
	public String getName(long view_id) {
		String ans = null;
		Statement st = null;
		try {
	    	st = getConnection().createStatement();
	    	ResultSet rs = st.executeQuery("SELECT Name FROM VIEWS WHERE View_id=" + view_id);
	    	if (rs.next())
	    		ans = rs.getString(1);
	    	rs.close();
	    	st.close(); st = null;
		} catch (SQLException e) {
			Logger.logException("ViewsDb.getName()", e);
			MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
		} finally {
			release(st);
		}
		return ans;
	}

    //---------------------------------------------------------------------
	public java.util.List<ViewRec> getViews() {
		ArrayList<ViewRec> ans = new ArrayList<ViewRec>();
		Statement st = null;
		try {
	    	st = getConnection().createStatement();
	    	ResultSet rs = st.executeQuery("SELECT Name, View_id FROM VIEWS ORDER BY Name");

	    	int c_id  = rs.findColumn("View_id");
	    	int c_name= rs.findColumn("Name");

	    	while (rs.next()) {
	    		ans.add(new ViewRec(rs.getString(c_name), rs.getLong(c_id)));
	    	}
	    	rs.close();
	    	st.close(); st = null;
		} catch (SQLException e) {
			Logger.logException("ViewsDb.getViews()", e);
			MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
		} finally {
			release(st);
		}
		return ans;
	}

    //---------------------------------------------------------------------
	public Columns getColumns(long view_id) {
		ColumnOrder ans = new ColumnOrder();
		Statement st = null;
		try {
	    	st = getConnection().createStatement();
	    	ans.read(view_id, "VIEW_COLUMNS", st);

	    	ResultSet rs = st.executeQuery("SELECT * FROM " + viewTable(view_id) + " WHERE 1=0");
	    	ans.reorder(rs);
	    	rs.close();
	    	st.close(); st = null;
		} catch (Exception e) {
			Logger.logException("ViewsDb.getColumns()", e);
			ans = null;
		} finally {
			release(st);
		}
		return ans;
	}

    //---------------------------------------------------------------------
	public Iterable<GeneralRow> getRows(long view_id, long minid, long limit) {
		return new RowIterator(view_id, minid, limit);
	}

    //---------------------------------------------------------------------
	public void close(Iterable<GeneralRow> rowIt) {
		if (rowIt instanceof RowIterator)
			((RowIterator)rowIt).dispose();
	}

    //---------------------------------------------------------------------
	public void close(java.util.Iterator<GeneralRow> rowIt) {
		if (rowIt instanceof RowIterator)
			((RowIterator)rowIt).dispose();
	}

    //---------------------------------------------------------------------
	/** Row iterator for view tables. */ 
	class RowIterator implements Iterable<GeneralRow>, java.util.Iterator<GeneralRow> {
		/** Id of the view. */
		long		view_id;
		/** The id of the first iterable row. */
		long		idmin;
		/** The maximum numbers of the iterable rows. */
		long		limit;
		/** The columns of the view. */
		ColumnOrder	cols;
		ResultSet	rs = null;
		boolean		ended = true;

		RowIterator(long view_id, long minid, long limit)	{ this.view_id = view_id; idmin = minid; this.limit = limit; }
		public boolean		hasNext()						{ return (rs != null && !ended); }
		public void		remove()						{ throw new UnsupportedOperationException(); }
		@Override 
		protected void		finalize() 						{ dispose(); }

		public Iterator<GeneralRow> iterator() {
			Statement st = null;
			rs = null;
			ended = true;
			try {
		    	st = getConnection().createStatement();
		    	if (limit <=0 || limit > 10000)
		    		st.setFetchSize(10000);
				cols = new ColumnOrder();
		    	cols.read(view_id, "VIEW_COLUMNS", st);

		    	// Ha csak 1 sor kell, akkor probalkozunk gyorsabb SQL-el. 
		    	// (A gyorsitasra ViewCreation-nel szukseg van.)
		    	// Ha nem sikerul, akkor visszaterunk az altalanos megoldashoz
		    	if (limit == 1) {
			    	String cmd = "SELECT * FROM " + viewTable(view_id) + " WHERE ID=" + idmin;
		    		rs = st.executeQuery(cmd);
		    		ended = !rs.next();
		    		if (ended) { rs.close(); rs = null; }
		    	}
		    	if (rs == null) {
			    	String cmd = "SELECT * FROM " + viewTable(view_id) + " WHERE ID >= " + idmin + " ORDER BY ID";
			    	if (limit > 0) cmd = cmd + " LIMIT " + limit;
			    	rs = st.executeQuery(cmd);
					ended = !rs.next();
		    	}
				if (ended) dispose();
				else cols.reorder(rs);
			} catch (SQLException e) {
				Logger.logException("ViewsDb.RowIterator.iterator()", e);
				MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
				release(st);
				dispose();
			}
			return this;
		}

		public GeneralRow next() {
			GeneralRow ans = null;
			try {
				ans = cols.readRow(rs);
				ended = !rs.next();
				if (ended) dispose();
			} catch (SQLException e) {
				Logger.logException("ViewsDb.RowIterator.next()", e);
				MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
			}
			return ans;
		}
		
		/** Releases the resources. */
		void dispose() {
			cols = null;
			if (rs != null) try {
				Statement st = rs.getStatement();
				rs.close();
				st.close();
			} catch (SQLException e) {
				Logger.logException("ViewsDb.RowIterator.dispose()", e);
				MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
			}
			rs = null;
			ended = true;
		}
	}

	//=========================================================================
	// Public methods

    //---------------------------------------------------------------------
	/** Returns the description of the view specified by 'view_id'. 'mayBeNull'
	 *  indicates the result may be null or the method would use empty string instead.
	 */
	public String getDescription(long view_id, boolean mayBeNull) {
		String ans = null;
		Statement st = null;
		try {
	    	st = getConnection().createStatement();
	    	ResultSet rs = st.executeQuery("SELECT Description FROM VIEWS WHERE View_id=" + view_id);

	    	if (rs.next()) {
	    		ans = rs.getString(1);
	    		if (mayBeNull && rs.wasNull()) {
	    			ans = null;
	    		} else if (!mayBeNull && ans == null) {
	    			ans = "";
	    		}
	    	}
	    	rs.close();
	    	st.close(); st = null;
		} catch (SQLException e) {
			Logger.logException("ViewsDb.getDescription()", e);
			MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
		} finally {
			release(st);
		}
		return ans;
	}

    //---------------------------------------------------------------------
	/** Sets the description of the view specified by 'view_id' to 'description'. */
	public void setDescription(long view_id, String description)
	{
		if (description != null && description.length() == 0)
			description = null;
			
		PreparedStatement ps = null;
		try {
	    	ps = getConnection().prepareStatement("UPDATE VIEWS SET Description=? WHERE View_id=" + view_id);
	    	if (description == null)
	    		ps.setNull(1, java.sql.Types.LONGVARCHAR);
	    	else
		    	ps.setString(1, description);			// Columns are numbered from 1

	    	ps.executeUpdate();
	    	ps.close(); ps = null;
    		viewsDbChanged.fire(new ViewsDbChangeEvent(this, ChangeAction.MODIFIED, toViewRec(view_id)));
		} catch (SQLException e) {
			Logger.logException("ViewsDb.setDescription()", e);
			MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
		} finally {
			release(ps);
		}
	}

    //---------------------------------------------------------------------
	/** Returns the creation rule of the view specified by 'view_id'. */ 
	public ViewCreationRule getRule(long view_id) {
		ViewCreationRule ans = null;
		Statement st = null;
		try {
	    	st = getConnection().createStatement();
	    	ResultSet rs = st.executeQuery("SELECT Creation_rule FROM VIEWS WHERE View_id=" + view_id);

	    	String tmp = null;
	    	if (rs.next()) {
	    		tmp = rs.getString(1);
	    		if (rs.wasNull())
	    			tmp = null;
	    	}
	    	rs.close();
	    	st.close(); st = null;
	    	if (tmp != null)
	    		ans = new ViewCreationRule(tmp);
		} catch (Exception e) {
			Logger.logException("ViewsDb.getRule()", e);
		} finally {
			release(st);
		}
		return ans;
	}

    //---------------------------------------------------------------------
	/** Returns the number of rows of the view specified by 'view_id'. */
	public long getNrOfRows(long view_id) {
		long ans = 0;
		Statement st = null;
		try {
	    	st = getConnection().createStatement();
	    	ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + viewTable(view_id));

	    	if (rs.next()) {
	    		ans = rs.getLong(1);
	    	}
	    	rs.close();
	    	st.close(); st = null;
		} catch (Exception e) {
			Logger.logException("ViewsDb.getNrOfRows()", e);
		} finally {
			release(st);
		}
		return ans;
	}
	
	//---------------------------------------------------------------------
	/** Renames column 'old_name' to 'new_name' in the view table specified by 
	 *  'view_id'.
	 */ 
	public boolean renameColumn(long view_id, String old_name, String new_name) {
		if (view_id < 0 || old_name == null || old_name.trim().length() == 0
			|| new_name == null || new_name.trim().length() == 0)
			throw new IllegalArgumentException();
		if (old_name.equals(new_name)) return true;
		
		boolean result = true;
		Statement st = null;
		PreparedStatement ps = null;
		Transaction t = null;
		try {
			st = getConnection().createStatement();
			ResultSet rs = st.executeQuery("SELECT Name FROM VIEW_COLUMNS WHERE Id=" + view_id + " AND " +
										   "Name='" + new_name + "'");
			if (rs.next()) result = false;
			rs.close(); rs = null;
			st.close(); st = null;
			if (result) {
				t = new Transaction(getConnection());
				t.start();
				ps  = getConnection().prepareStatement("UPDATE VIEW_COLUMNS SET Name=? WHERE " +
				  									   "Id=? AND Name=?");
				ps.setString(1,new_name);
				ps.setLong(2,view_id);
				ps.setString(3,old_name);
				ps.executeUpdate();
				ps.close(); ps = null;
				
				ViewCreationRule rule = getRule(view_id);
				rule.changeName(old_name,new_name);
				
				ps = getConnection().prepareStatement("UPDATE VIEWS SET CREATION_RULE=? WHERE " +
													  "View_id=?");
				ps.setString(1,rule.toString());
				ps.setLong(2,view_id);
				ps.executeUpdate();
				ps.close(); ps = null;
				
		   		t.commit();
		   		viewsDbChanged.fire(new ViewsDbChangeEvent(this, ChangeAction.MODIFIED, toViewRec(view_id)));
			}
		} catch (SQLException e) {
			result = false;
			Logger.logException("ViewsDb.renameColumn()", e);
			MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
			if (t != null) {
				try {
					t.rollback();
				} catch (SQLException e1) {
					Logger.logException("ViewsDb.renameColumn()", e1);
					MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e1);
				}
			}
		} finally {
			release(st);
			release(ps);
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean renameView(long view_id, String old_name, String new_name) {
		if (view_id < 0 || old_name == null || old_name.trim().length() == 0 ||
			new_name == null || new_name.trim().length() == 0)
			throw new IllegalArgumentException();
		
		boolean result = true;
		Statement st = null;
		PreparedStatement ps = null;
		Transaction t = null;
		try {
			t = new Transaction(getConnection());
			t.start();
			ViewCreationRule rule = getRule(view_id);
			rule.changeViewName(new_name);
			ps  = getConnection().prepareStatement("UPDATE VIEWS SET Name=?, Creation_rule=?" +
												   " WHERE View_Id=?");
			ps.setString(1,new_name);
			ps.setString(2,rule.toString());
			ps.setLong(3,view_id);
			ps.executeUpdate();
			ps.close(); ps = null;

	    	st = getConnection().createStatement();
	    	ResultSet rs = st.executeQuery("SELECT View_id, Creation_rule FROM VIEWS WHERE View_id<>" + view_id +
	    								   " ORDER BY View_id");

	    	int c_id  = rs.findColumn("View_id");
	    	int c_rule = rs.findColumn("Creation_rule");

	    	while (rs.next()) {
	    		ViewCreationRule _rule = new ViewCreationRule(rs.getString(c_rule));
	    		_rule.changeReference(old_name,new_name);
	    		
	    		ps = getConnection().prepareStatement("UPDATE VIEWS SET Creation_rule=? WHERE" +
	    											  " View_Id=?");
	    		ps.setString(1,_rule.toString());
	    		ps.setLong(2,rs.getLong(c_id));
	    		ps.executeUpdate();
	    		ps.close(); ps = null;
	    	}
	    	rs.close();
	    	st.close(); st = null;
				
	   		t.commit();
	   		viewsDbChanged.fire(new ViewsDbChangeEvent(this,ChangeAction.MODIFIED,toViewRec(view_id)));
		} catch (Exception e) {
			result = false;
			Logger.logException("ViewsDb.renameView()", e);
			if (e instanceof SQLException)
				MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory((SQLException)e);
			if (t != null) {
				try {
					t.rollback();
				} catch (SQLException e1) {
					Logger.logException("ViewsDb.renameView()", e1);
					MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e1);
				}
			}
		} finally {
			release(st);
			release(ps);
		}
		return result;
	}
	
    //---------------------------------------------------------------------
	/** 
	 * (Re)Creates the named view table.
	 * Adds it to the VIEWS table, creates column mappings into VIEW_COLUMNS,
	 * and creates the VIEW_nn table.
	 * Call addRows() to enter data into the table.
	 * @param description Textual comments for the view table, may be null.
	 * @param rule If null, the Creation_rule column will receive the empty string.
	 * @param cols If null, the method creates it automatically from 'rule'.
	 *              Cannot be null when 'rule' is null.
	 * @return The view_id of the new table.
	 * @throws SQLException if SQL error occurs.
	 */
	public long createView(String name, String description, ViewCreationRule rule, Columns cols) throws SQLException
	{
		// Verify & prepare arguments
		if (name == null || name.length() == 0)
			throw new IllegalArgumentException();    	

		String rulestr = null;
    	if (rule != null) try {
    		rulestr = XMLUtils.toString(rule.node);

    		if (cols == null) {
    			cols = new Columns();
	    		for (ViewCreationRule.Column c : rule.getColumns())
	    			cols.append(c.getName(), c.getInitialDataType());
    		}
    	} catch (Exception e) {
    		throw new IllegalArgumentException("invalid ViewCreationRule", e);
    	}
    	if (cols == null)
    		throw new NullPointerException("both rule and cols are null");

		// Make sure that this name does not exist
		Long id = findView(name);
		if (id != null)
			deleteView(new ViewRec(name, id));

		SQLDialect dialect = MEMEApp.getDatabase().getSQLDialect();
		Statement st = null;
		PreparedStatement ps = null; 
		Transaction t = new Transaction(getConnection());
		t.start();
		try {
			st = getConnection().createStatement();

			// Generate view_id if the table is new
			if (id == null) {
				// I don't verify the value returned by IDGEN - I assume it is really unused. 
				id = dialect.getSeqNext("IDGEN", st);
			}

			// Create the VIEWS_nn table for the data rows

    		String identity_column  = dialect.autoIncrementColumn("ID");
    		st.execute(dialect.createBigTable(
    				viewTable(id) + " ( " + identity_column + cols.compose() + ')'
    		));
    		st.close(); st = null;

    		// Register in the VIEWS table

	    	ps = getConnection().prepareStatement(
	    			"INSERT INTO VIEWS (View_id, Name, Description, Creation_rule) VALUES (?,?,?,?)");
	    	ps.setLong  (1, id);
	    	ps.setString(2, name);
	    	if (description == null)
	    		ps.setNull(3, java.sql.Types.LONGVARCHAR);
	    	else
	    		ps.setString(3, description);
	    	if (rulestr == null)
	    		ps.setNull(4, java.sql.Types.LONGVARCHAR);
	    	else
	    		ps.setString(4, rulestr);
    		ps.executeUpdate();
    		ps.close(); ps = null;

    		// Store column mappings

    		cols.updateColMapping("VIEW_COLUMNS", id, getConnection());

    		t.commit();
    		viewsDbChanged.fire(new ViewsDbChangeEvent(this, ChangeAction.ADDED, new ViewRec(name, id)));

		} finally {
			release(ps);
			release(st);
			t.rollback();
		}
		return id;
	}

    //---------------------------------------------------------------------
	/**
	 * Appends rows to the specified view table. All the new rows should share 
	 * a single Columns object. If these new columns are different from the 
	 * existing columns, the view table will be altered to accomodate to the 
	 * new data (new columns may be added, or existing columns may be converted
	 * to a wider data type).<br>
	 */
	/* FONTOS: Ha a hivo felbehagyja a munkat pl. nala bekovetkezett exception
	 * miatt, akkor az itteni dispose() muveletet feltetlenul hivja meg! Normal
	 * befejezeshez finish()-t kell meghivni. A legjobb, ha dispose() hivasa
	 * finally blokkban van. Ha finish() utan is meghivodik, az nem baj.
	 */
	public class AppendView {
		long				view_id;
		PreparedStatement	ps;
		Columns				newcols;
		Transaction			t;
		int					commitFreq, counter;

		/* @param commitFreq ennyi soronkent commit-et mond. 0 eseten nem lesz commit */
		public void init(long view_id, int commitFreq) throws SQLException {
			this.view_id = view_id;
			ps = null;
			newcols = null;
			counter = 0;
			this.commitFreq = commitFreq;
			t = new Transaction(getConnection());
			t.start();
		}

		public void addRow(GeneralRow row) throws SQLException, ValueNotSupportedException {
			ValueNotSupportedException ex = null;
			if (newcols == null) {
				if (t == null) throw new IllegalStateException("init() was not called");
				Statement st = null;
				AlterColumns alter;
				try {
					newcols = row.getColumns();
					st = getConnection().createStatement();
					alter = new AlterColumns("VIEW_COLUMNS", viewTable(view_id), view_id, newcols, st);
					st.close(); st = null;
				} finally {
					release(st);
				}
				alter.updateColMapping(getConnection());
				alter.order = alter.order .replaceFirst(",", "");
				alter.orderQ= alter.orderQ.replaceFirst(",", "");

		    	ps = getConnection().prepareStatement(
		    			"INSERT INTO " + viewTable(view_id) + " (" + alter.order + ") VALUES (" + alter.orderQ + ')');
			}
			else {
				assert (row.getColumns() == newcols) : row;
			}
			try {
				row.writeValues(ps, 1); // columns are numbered from 1
			} catch (final ValueNotSupportedException e) {
				ex = e;
			}		
	    	ps.executeUpdate();
	    	if (commitFreq > 0 && (++counter % commitFreq) == 0)
	    		getConnection().commit();
	    	if (ex != null)
	    		throw ex;
		}

		/** May be called without calling init(). Does nothing if no rows were added.
		 * @throws SQLException */
		public void finish() throws SQLException {
			if (ps != null) { ps.close(); ps = null; }
			if (t != null) t.commit();
			ViewsDbChangeEvent event = (newcols == null) ? null :
				new ViewsDbChangeEvent(this, ChangeAction.MODIFIED, toViewRec(view_id)); 
			dispose();
			if (event != null) viewsDbChanged.fire(event);
		}

		/** May be called without calling init().
		 * @throws SQLException */
		public void dispose() throws SQLException {
			release(ps); ps = null;
			if (t != null) { t.rollback(); t = null; }
			newcols = null;
		}
	}

    //---------------------------------------------------------------------
	/** Deletes view table specified by 'v'. */ 
	public void deleteView(ViewRec v) {
		Statement st = null;
		try {
			Columns.deleteColMapping("VIEW_COLUMNS", v.getViewID(), getConnection());

			st = getConnection().createStatement();
			st.executeUpdate("DELETE FROM VIEWS WHERE View_id=" + v.getViewID());
			st.close(); st = null;

			MEMEApp.getDatabase().getSQLDialect().deleteTableIfExists( viewTable(v.getViewID()) );
    		viewsDbChanged.fire(new ViewsDbChangeEvent(this, ChangeAction.REMOVED, v));

		} catch (SQLException e) {
			Logger.logException("ViewsDb.deleteView()", e);
			MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
		} finally {
			release(st);
		}
	}

    //---------------------------------------------------------------------
	/** Reorders the rows in the view table identified by 'view_id'. It uses
	 *  the 'orderby' column to form the new order.
	 */
	public long reorderTable(long view_id, String orderby) {
		String tmpName = null, seq = null;
		Statement st = null;
		SQLDialect dialect = MEMEApp.getDatabase().getSQLDialect();
		try {
			String name				= getName(view_id);
			String description		= getDescription(view_id, true);
			ViewCreationRule rule	= getRule(view_id);
			Columns cols			= getColumns(view_id);

			tmpName = getTmpTableName();

			st = getConnection().createStatement();
			seq = dialect.makeTmpSeq(st);

			st.executeUpdate("ALTER TABLE " + viewTable(view_id) + " RENAME TO " + tmpName);

			view_id = createView(name, description, rule, cols);
			String command = "INSERT INTO " + viewTable(view_id) 
								+ " SELECT " + dialect.selectSeqNext(seq) 
								+ ", " + Utils.join(cols.getSQLNames(), ",") 
								+ " FROM " + tmpName + " ORDER BY " + orderby;
			st.executeUpdate(command);

			st.close(); st = null;

		} catch (SQLException e) {
			Logger.logException("ViewsDb.reorderTable()", e);
			MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
		} finally {
			release(st);
			try {
				if (tmpName != null) dialect.deleteTableIfExists(tmpName);
				if (seq != null) dialect.deleteSeqIfExists(seq);
			} catch (SQLException e) {
				Logger.logException("ViewsDb.reorderTable()", e);
				MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
			}
		}
		return view_id;
	}

	//=========================================================================
	// Internal methods

	/**
	 * This method is public as an implementation side effect. Do not call or override.
	 * As the new database may be empty, this method tries to create the fixed tables  
	 * (without overwriting).
	 */
	public void onConnChange(ConnChangedEvent event) {
		if (event.getConnection() != null)
			createTables();
    }

    //---------------------------------------------------------------------
	/** Returns the JDBC connection. */
    private Connection getConnection() {
    	return MEMEApp.getDatabase().getConnection();
    }

    //---------------------------------------------------------------------
	/** It creates all the administration tables belongs to the views tables. */
    private void createTables() {
    	String[] cmds = MEMEApp.getDatabase().getSQLDialect().createViewsTables();
		try {
			LocalAPI.runCreateCommands(getConnection(), cmds);
		} catch (SQLException e) {
			Logger.logException("LocalAPI.createTables()", e);    			
			MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
		}
    }
    
    //---------------------------------------------------------------------
    /** Returns the name of the view table identified by 'view_id'. */
    private static String viewTable(long view_id) {
    	return "VIEW_" + view_id;
    }

    //---------------------------------------------------------------------
    /** Returns the name of the temporary view table. */
    private static String getTmpTableName() throws SQLException {
		String ans = "VIEWTMP";
		MEMEApp.getDatabase().getSQLDialect().deleteTableIfExists(ans);
    	return ans;
    }

    //---------------------------------------------------------------------
    /** Create a ViewRec object from the view table identified by 'view_id'. */
    private ViewRec toViewRec(long view_id) {
		String name = null;
		Statement st = null;
		try {
	    	st = getConnection().createStatement();
	    	ResultSet rs = st.executeQuery("SELECT Name FROM VIEWS WHERE View_id=" + view_id);

	    	if (rs.next())
	    		name = rs.getString(1);
	    	rs.close();
	    	st.close(); st = null;
		} catch (SQLException e) {
			Logger.logException("ViewsDb.toViewRec()", e);
			MEMEApp.getDatabase().getSQLDialect().checkOutOfMemory(e);
		} finally {
			release(st);
		}
		return new ViewRec(name, view_id);
    }


}
