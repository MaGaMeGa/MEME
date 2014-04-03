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

import java.sql.SQLException;

import ai.aitia.meme.MEMEApp;
import static ai.aitia.meme.utils.Utils.getBooleanValue;

/** This class represents the possible types of the columns. */
public class ColumnType implements Cloneable, Comparable<Object>
{
	//=========================================================================
	// Static members
	
	/** A record class that encapsulates a column type and a value. */
	public static class TypeAndValue {
		ColumnType	type;
		Object		value;
		TypeAndValue(ColumnType t, Object v)	{ type = t; value = v; }
		public ColumnType	getType()			{ return type; }
		public Object		getValue()			{ return value; }		
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	public static class ValueNotSupportedException extends Exception {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public ValueNotSupportedException() {}
		public ValueNotSupportedException(final String msg) { super(msg); }
		public ValueNotSupportedException(final Throwable t) { super(t); }
		public ValueNotSupportedException(final String msg, final Throwable t) { super(msg,t); }
	}

// It uses DatabaseConnection.maxVarcharLengthCache instead.
//	static Integer maxVarcharLengthCache = 64;
//	static {
//		// 'maxVarcharLengthCache' caches the result of SQLDialect.getMaxVarcharLength()
//		// because it is needed very frequently. The cached value must be updated  
//		// whenever the database connection changes.
//		if (MEMEApp.getDatabase().getSQLDialect() != null)
//			maxVarcharLengthCache = MEMEApp.getDatabase().getSQLDialect().getMaxVarcharLength();
//
//		MEMEApp.getDatabase().connChanged.addListener(new IConnChangedListener() {
//			public void onConnChange(ConnChangedEvent event) {
//				if (event.getConnection() != null)
//					maxVarcharLengthCache = MEMEApp.getDatabase().getSQLDialect().getMaxVarcharLength();
//			}
//		});
//	}

	/** The 'boolean' column type. */
	public static final ColumnType BOOLEAN = new ColumnType(java.sql.Types.CHAR);
	/** The 'int' column type. */
	public static final ColumnType INT     = new ColumnType(java.sql.Types.INTEGER);
	/** The 'long' column type. */
	public static final ColumnType LONG    = new ColumnType(java.sql.Types.BIGINT);
	/** The 'double' column type. */
	public static final ColumnType DOUBLE  = new ColumnType(java.sql.Types.DOUBLE);
	/** The 'String' column type. */
	public static final ColumnType STRING  = new ColumnType(java.sql.Types.VARCHAR, 64);
	
	public static final double VALUE_BEFORE_MAX_VALUE = Double.longBitsToDouble(Double.doubleToLongBits(Double.MAX_VALUE) - Double.doubleToLongBits(Double.MIN_VALUE));
	public static final double VALUE_AFTER_MIN_VALUE =  - VALUE_BEFORE_MAX_VALUE;

	// The following constants belong to getUnion()
	static final Integer mxvchI = 11, mxvchL = 20, mxvchD = 24, mx = 0;
	/** The 'String' column type with length 11. */
	static final ColumnType vchI = new ColumnType(java.sql.Types.VARCHAR, mxvchI); 
	/** The 'String' column type with length 20. */
	static final ColumnType vchL = new ColumnType(java.sql.Types.VARCHAR, mxvchL); 
	/** The 'String' column type with length 24. */
	static final ColumnType vchD = new ColumnType(java.sql.Types.VARCHAR, mxvchD); 
	
	static final Object extensionTable[][] = {
		//				boolean	int		long	double	vch		lvch
		/* boolean */	{ null,	vchI,	vchL,	vchD,	mx,		mx		},
		/* int     */	{ vchI,	null,	LONG,	DOUBLE,	mxvchI,	mxvchI	},
		/* long    */	{ vchL,	null,	null,	DOUBLE,	mxvchL, mxvchL	},
		/* double  */	{ vchD,	null,	null,	null,	mxvchD,	mxvchD	},
		/* varch   */	{ null,	mxvchI,	mxvchL,	mxvchD,	mx,		mx		},
		/* longvch */	{ null,	mxvchI,	mxvchL,	mxvchD,	mx,		mx		}
	};
	static final int	extensionTableMap[] = {		// it must be in the same order as the table above
		BOOLEAN.javaSqlType,
		INT.javaSqlType,
		LONG.javaSqlType,
		DOUBLE.javaSqlType,
		java.sql.Types.VARCHAR,
		java.sql.Types.LONGVARCHAR
	};

	
	//=========================================================================
	// Variables
	
	/** The identifier of the SQL-type of the column. */
	public final int	javaSqlType;	// from java.sql.Types
	/** Paramter of the SQL-type of the column, */
	public int			a,b;


	//=========================================================================
	// Constructors
	
	public 	ColumnType()									{ this.javaSqlType = init(STRING.javaSqlType, STRING.a, STRING.b); }
			ColumnType(int javaSqlType)					{ this.javaSqlType = init(javaSqlType, 0, 0); }
			ColumnType(int javaSqlType, int a)			{ this.javaSqlType = init(javaSqlType, a, 0); }
			ColumnType(int javaSqlType, int a, int b)	{ this.javaSqlType = init(javaSqlType, a, b); }
	public ColumnType(ColumnType other)					{ this.javaSqlType = init(other.javaSqlType, other.a, other.b); }
	public ColumnType(String str) {
		String[] tmp = str.split("\\.");		// see toString()
		if (tmp.length != 3)
			throw new IllegalArgumentException(str);
		this.javaSqlType = init(Integer.parseInt(tmp[0]), Integer.parseInt(tmp[1]), Integer.parseInt(tmp[2]));
	}

	//=========================================================================
	//	Construction methods (that can produce new ColumnType instances)

	//-------------------------------------------------------------------------
	/** 
	 * Designed for incremental type detection (e.g. reading data from a .csv
	 * file), therefore the returned value should produce the same 'str' string  
	 * if toString() is applied. This is why "t" and "f" are not interpreted as 
	 * boolean values.
	 */
	public static TypeAndValue parseValue(String p_str) {
		ColumnType type = null;
		Object value = null;
		if (p_str != null) {
			String s = p_str.trim();
			try {
				Number d = Double.valueOf(s);
				double x = d.doubleValue();
				if (d.intValue() == x) {
					type = ColumnType.INT;
					value= Integer.valueOf(d.intValue());
				}
				else if (Long.MIN_VALUE <= x && x <= Long.MAX_VALUE && s.indexOf('.') < 0) {
					type = ColumnType.LONG;
					value= Long.valueOf(s);
				}
				else {
					type = ColumnType.DOUBLE;
					value = d;
				}
			} catch (NumberFormatException e) {
				s = s.toLowerCase();
				if (s.equals(Boolean.TRUE.toString())) {
					type = ColumnType.BOOLEAN;
					value = Boolean.TRUE;
				}
				else if (s.equals(Boolean.FALSE.toString())) {
					type = ColumnType.BOOLEAN;
					value = Boolean.FALSE;			
				}
				else {
					type = new ColumnType(java.sql.Types.VARCHAR,Math.max(p_str.length(),1));
					value= p_str;
				}
			}
		}
		return new TypeAndValue(type, value);
	}

	//-------------------------------------------------------------------------
	/** Parses 'obj' to an TypeAndValue object. 
	 * obj==null results TypeAndValue(null, null) */
	public static TypeAndValue parseObject(Object obj) {
		ColumnType ans = null;
		if (obj instanceof Boolean)									ans = BOOLEAN;
		else if ((obj instanceof Double) || (obj instanceof Float)) 	ans = DOUBLE;
		else if (obj instanceof Long)									ans = LONG;
		else if (obj instanceof Number)								ans = INT;
		else if (obj == null || (obj instanceof String))
			return parseValue(obj == null ? null : obj.toString());
		if (ans == null) {
			ans = new ColumnType(java.sql.Types.VARCHAR, obj.toString().length());
		}
		return new TypeAndValue(ans, obj);
	}

	//-------------------------------------------------------------------------
	/**
	 * Returns a type which is able to represent values of both 'this' type 
	 * and 'other'. If 'this' is broad enough to represent 'other', the method 
	 * returns a reference to 'this'.   
	 */
	public ColumnType getUnion(ColumnType other) {
		if (other == null || other == this)
			return this;

		int t = extensionTableMap.length, ot = t;
		while (-- t >= 0 && extensionTableMap[ t] !=       javaSqlType) {};
		while (--ot >= 0 && extensionTableMap[ot] != other.javaSqlType) {};
		if ( t < 0) throw new IllegalStateException();
		if (ot < 0) throw new IllegalArgumentException();

		Object action = extensionTable[t][ot];

		if (action == null)
			return this;

		if (action instanceof ColumnType)
			return (ColumnType)action;

		if (action instanceof Integer) {
			int len = Math.max(this.a, Math.max(other.a, (Integer)action));
			return new ColumnType(java.sql.Types.VARCHAR, len);
		}
		assert false;
		return this;
	}
	
	//=========================================================================
	// Public methods

	//-------------------------------------------------------------------------
	@Override public String		toString()			{ return String.format("%d.%d.%d", javaSqlType, a, b); }
	@Override public boolean	equals(Object other){ return compareTo(other) == 0; }
	/** Analogous to {@link #java.util.Arrays.hashCode(int[])} */
	@Override public int		hashCode()			{ return ((javaSqlType * 31) + a) * 31 + b; }
	@Override public Object		clone()				{ return cloneImpl(); }
	public ColumnType			cloneImpl()			{ return new ColumnType(this); }

	//-------------------------------------------------------------------------
	/** Considers nulls and non-ColumnType objects <i>smaller</i> than 'this' */
	// Olyan osszehasonlitast nem lehet irni, ami konvertalhatosag alapjan 
	// hasonlit (pl. ta > tb <=> ta bovebb mint tb) mert vannak olyan esetek
	// amikor egyik sem konvertalhato a masikra (pl. double <-> long, 
	// int <-> char(3) stb.)
	// Ezert ez az osszehasonlitas inkabb gyors, mintsem ertelmes sorrendet eredmenyezo.
	public int compareTo(Object o) {
		if (o == this) return 0;
		if (!(o instanceof ColumnType)) return 2;
		ColumnType other = (ColumnType)o;
		if (javaSqlType < other.javaSqlType) return -1;
		if (javaSqlType == other.javaSqlType) {
			if (a < other.a) return -1;
			if (a == other.a) {
				return (b < other.b) ? -1 : (b == other.b ? 0 : 1);
			}
		}
		return 1;
	}

	//-------------------------------------------------------------------------
	/** Write value 'value' to the prepared statement 'ps' (in position 'colIdx').
	 * 'value' is automatically converted if necessary, e.g. a numeric 'value'
	 * is converted to string, or a string 'value' is converted to boolean 
	 * (or even numeric if possible).
	 * @throws ClassCastException if conversion is not possible: 'this' ColumnType
	 *          is a numeric type and 'value' cannot be converted to Number.
	 */
	public void writeValue(Object value, java.sql.PreparedStatement ps, int colIdx) throws SQLException, ValueNotSupportedException {
		if (value == null) {
			ps.setNull(colIdx, javaSqlType);
		} else {
			switch (javaSqlType) {
				case java.sql.Types.VARCHAR :
				case java.sql.Types.LONGVARCHAR : 
					ps.setString(colIdx, value.toString());
					break;

				case java.sql.Types.CHAR :
					ps.setString(colIdx, getBooleanValue(value) ? "t" : "f");
					break;

				case java.sql.Types.INTEGER : 
					if (!(value instanceof Number)) value = parseValue(value.toString()).getValue();
					ps.setInt(colIdx, ((Number)value).intValue());
					break;

				case java.sql.Types.BIGINT : 
					if (!(value instanceof Number)) value = parseValue(value.toString()).getValue();
					ps.setLong(colIdx, ((Number)value).longValue());
					break;

				case java.sql.Types.DOUBLE :
					if (!(value instanceof Number)) value = parseValue(value.toString()).getValue();
					final double doubleValue = ((Number)value).doubleValue();
					if (Double.isNaN(doubleValue))
						ps.setNull(colIdx,javaSqlType);
					else if (Double.isInfinite(doubleValue)) {
						ps.setDouble(colIdx,doubleValue < 0 ? - Double.MAX_VALUE : Double.MAX_VALUE);
					} else if (Double.doubleToLongBits(doubleValue) == Double.doubleToLongBits(Double.MAX_VALUE)) {
						ps.setDouble(colIdx,VALUE_BEFORE_MAX_VALUE);
						throw new ValueNotSupportedException(Double.MAX_VALUE + " is not supported. MEME uses " + VALUE_BEFORE_MAX_VALUE + " to " +
															 "replace it");
					} else if (Double.doubleToLongBits(doubleValue) == Double.doubleToLongBits(- Double.MAX_VALUE)) {
						ps.setDouble(colIdx,VALUE_AFTER_MIN_VALUE);
						throw new ValueNotSupportedException(- Double.MAX_VALUE + " is not supported. MEME uses " + VALUE_AFTER_MIN_VALUE + " to " +
						 									 "replace it");
					} else
						ps.setDouble(colIdx,doubleValue);
					break;

				default :
					assert false;
			}
		}
	}
	
	//-------------------------------------------------------------------------
	/** Converts 'value' to this type */
	public Object convert(Object value) {
		if (value == null)
			return null;
		switch (javaSqlType) {
			case java.sql.Types.VARCHAR :
			case java.sql.Types.LONGVARCHAR :
				return String.valueOf(value);

			case java.sql.Types.CHAR :
				return getBooleanValue(value);

			case java.sql.Types.INTEGER :
				if (!(value instanceof Number)) value = parseValue(value.toString()).getValue();
				return (value instanceof Integer) ? value : ((Number)value).intValue();

			case java.sql.Types.BIGINT : 
				if (!(value instanceof Number)) value = parseValue(value.toString()).getValue();
				return (value instanceof Long) ? value : ((Number)value).longValue();

			case java.sql.Types.DOUBLE : 
				if (!(value instanceof Number)) value = parseValue(value.toString()).getValue();
				return (value instanceof Double) ? value : ((Number)value).doubleValue();
	
			default :
				assert false : this;
				return value;
		}
	}

	
	//-------------------------------------------------------------------------
	/** Reads value specified by 'colIdx' from 'rs'. */
	public Object readValue(java.sql.ResultSet rs, int colIdx) throws SQLException {
		Object ans = null;
		switch (javaSqlType) {
			case java.sql.Types.VARCHAR :
			case java.sql.Types.LONGVARCHAR :
				ans = rs.getString(colIdx);
				break;

			case java.sql.Types.CHAR :
				ans = getBooleanValue(rs.getString(colIdx));
				break;

			case java.sql.Types.INTEGER :
				ans = Integer.valueOf(rs.getInt(colIdx));
				break;

			case java.sql.Types.BIGINT : 
				ans = Long.valueOf(rs.getLong(colIdx));
				break;
				
			case java.sql.Types.DOUBLE :
				ans = Double.valueOf(rs.getDouble(colIdx));
				final double value = ((Number)ans).doubleValue(); 
				final long valueBits = Double.doubleToLongBits(value);
				if (valueBits == Double.doubleToLongBits(Double.MAX_VALUE))
					ans = Double.valueOf(Double.POSITIVE_INFINITY);
				else if (valueBits == Double.doubleToLongBits(- Double.MAX_VALUE))
					ans = Double.valueOf(Double.NEGATIVE_INFINITY);
				break;
				
			default :
				assert false;
				return null;
		}
		return (rs.wasNull()) ? null : ans;
	}

	//-------------------------------------------------------------------------
	/** Returns the string representation of the SQL-type belongs to 'this' type. */
	public String getSQLTypeStr() {
		switch (javaSqlType) {
			case java.sql.Types.VARCHAR :
				return "VARCHAR(" + a + ')';

			case java.sql.Types.LONGVARCHAR :
				return MEMEApp.getDatabase().getSQLDialect().getSQLType(javaSqlType) + '(' +  a + ')';
	
			case java.sql.Types.CHAR :
				return "CHAR(1)";
	
			case java.sql.Types.INTEGER :
				return "INT";

			case java.sql.Types.BIGINT : 
			case java.sql.Types.DOUBLE : 
				return MEMEApp.getDatabase().getSQLDialect().getSQLType(javaSqlType);

			default :
				throw new IllegalStateException();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getHumanType() {
		if (BOOLEAN.equals(this))
			return "logical"; 
		if (INT.equals(this) || LONG.equals(this))
			return "integer";
		if (DOUBLE.equals(this))
			return "real";
		if (STRING.equals(this))
			return "string";
		return toString();
	}
	
	//-------------------------------------------------------------------------
	/** Returns the Class object of the representation class of the values with 'this' type. */ 
	public Class getJavaClass() {
		switch (javaSqlType) {
			case java.sql.Types.VARCHAR : 
			case java.sql.Types.LONGVARCHAR :	return String.class;
			case java.sql.Types.CHAR :			return Boolean.class;
			case java.sql.Types.INTEGER :		return Integer.class;
			case java.sql.Types.BIGINT :		return Long.class; 
			case java.sql.Types.DOUBLE :		return Double.class;
			default :
				throw new IllegalStateException();
		}
	}

	//-------------------------------------------------------------------------
	/** Returns the column type object belongs to the Class object 'cls' */
	public static ColumnType convertJavaClass(Class cls) {
		if (Boolean.class.isAssignableFrom(cls))	return BOOLEAN;
		if (Number.class.isAssignableFrom(cls)) {
			if (cls.isPrimitive()) {
				if (Long.TYPE.equals(cls))			return LONG;
				if (Integer.TYPE.equals(cls)
					|| Short.TYPE.equals(cls)
					|| Byte.TYPE.equals(cls))		return INT;
			} else {
				if (Long.class.equals(cls))			return LONG;
				if (Integer.class.equals(cls)
					|| Short.class.equals(cls)
					|| Byte.class.equals(cls))		return INT;
			}
			return DOUBLE;
		}
		return STRING;
	}

	
	//-------------------------------------------------------------------------
//	/** 
//	 * Returns true if 'this' type is wider than 'other' (or identical).
//	 * Note that for some ta,tb pairs <code>!ta.isWider(tb) && !tb.isWider(ta)</code>. 
//	 */
//	public boolean isWider(ColumnType other) {
//		other = getUnion(other);
//		return (other == this) || other != null && a == other.a && b == other.b && (
//					(javaSqlType == other.javaSqlType)
//					|| (javaSqlType == java.sql.Types.VARCHAR && other.javaSqlType == java.sql.Types.LONGVARCHAR)
//					|| (javaSqlType == java.sql.Types.LONGVARCHAR && other.javaSqlType == java.sql.Types.VARCHAR)
//				);
//	}

	//-------------------------------------------------------------------------
//	public void assign(ColumnType other) {
//		init(other.javaSqlType, other.a, other.b);
//	}

	
	//=========================================================================
	// Internal methods
	
	//-------------------------------------------------------------------------
	/**
	 * Automatically adjusts the arguments from VARCHAR to LONGVARCHAR, 
	 * according to the capabilities of the current database engine.
	 * @trhows  IllegalArgumentException if javaSqlType is not one of the 
	 *           supported types. 
	 */
	int init(int javaSqlType, int a, int b) {
		switch (javaSqlType) {
			case java.sql.Types.VARCHAR :
				if (a > DatabaseConnection.maxVarcharLengthCache.intValue())
					javaSqlType = java.sql.Types.LONGVARCHAR;
				// no break
			case java.sql.Types.LONGVARCHAR :
				b = 0;
				break;

			case java.sql.Types.CHAR :
				a = 1; b = 0;
				break;

			case java.sql.Types.INTEGER : 
			case java.sql.Types.BIGINT : 
			case java.sql.Types.DOUBLE : 
				a = b = 0;
				break;

			default :
				throw new IllegalArgumentException(this.toString());
		}
		this.a = a;
		this.b = b;
		//this.javaSqlType = javaSqlType;
		return javaSqlType;
	}

	//-------------------------------------------------------------------------
//	/** See also {@link Utils.getBooleanValue()} */ 
//	public static boolean getBoolValue(Object o) {
//		if (o instanceof Boolean)
//			return (Boolean)o;
//		if (o instanceof Number)
//			return ((Number)o).intValue() != 0;
//		String s = o.toString();
//		return (s != null && s.length() > 0 && s.substring(0, 1).toLowerCase().charAt(0) == 't');
//	}

}
