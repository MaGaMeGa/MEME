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
package ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import cern.jet.random.Uniform;



public class Gene implements Serializable {
	//=========================================================================
	//members
	private static final long serialVersionUID = -496417549120419393L;
	protected GeneInfo info;
	protected Object value;

	//=========================================================================
	//constructors
	public Gene( GeneInfo info, Object value ){
		this.info = info;
		this.value = value;
	}

	//=========================================================================
	//public functions
	public Object getUniformRandomValue( Uniform rnd ){
		if( info != null ){
			if( info.getValueType().equals( GeneInfo.LIST ) ){
				int idx = rnd.nextIntFromTo( 0, info.getValueRange().size() );
				return info.getValueRange().get( idx );
			}else if( info.getMinValue() != null && info.getMaxValue() != null ){
				Number min = info.getMinValue();
				Number max = info.getMaxValue();
				String javaType = info.getType();
				if( "double".equalsIgnoreCase( javaType ) ){
					if( info.isIntegerVals() ){
						long val = rnd.nextLongFromTo( Math.round( min.doubleValue() ), 
													   Math.round( max.doubleValue() ));
						return Double.valueOf( val );
					}else
						return rnd.nextDoubleFromTo( min.doubleValue(), max.doubleValue() );
				}else if( "float".equalsIgnoreCase( javaType ) ){
					if( info.isIntegerVals() ){
						long val = rnd.nextLongFromTo( Math.round( min.doubleValue() ), 
													   Math.round( max.doubleValue() ));
						return Float.valueOf( val );
					}else
						return rnd.nextFloatFromTo( min.floatValue(), max.floatValue() );
				}else if( "int".equalsIgnoreCase( javaType ) ||
					"integer".equalsIgnoreCase( javaType ) ||
					"short".equalsIgnoreCase( javaType ) ){
					return rnd.nextIntFromTo( min.intValue(), max.intValue() );
				}else if( "long".equalsIgnoreCase( javaType ) ){
						return rnd.nextLongFromTo( min.longValue(), max.longValue() );
					}else if( "boolean".equalsIgnoreCase( javaType ) ){
					return rnd.nextBoolean() ;
				}else if( "string".equalsIgnoreCase( javaType ) ){
					return "";
				}
			}else{
				String javaType = info.getType();
				if( "double".equalsIgnoreCase( javaType ) ){
					if( info.isIntegerVals() ){
						long val = rnd.nextLongFromTo( -1000000, 1000000 );
						return Double.valueOf( val );
					}else
						return rnd.nextDoubleFromTo( -1000000, 1000000 );
				}else if( "float".equalsIgnoreCase( javaType ) ){
					if( info.isIntegerVals() ){
						long val = rnd.nextLongFromTo( -1000000, 1000000 );
						return Float.valueOf( val );
					}else
						return rnd.nextFloatFromTo( -1000000, 1000000 );
				}else if( "int".equalsIgnoreCase( javaType ) ||
					"integer".equalsIgnoreCase( javaType ) ||
					"short".equalsIgnoreCase( javaType ) ){
					return rnd.nextIntFromTo( Integer.MIN_VALUE, Integer.MAX_VALUE );
				}else if( "long".equalsIgnoreCase( javaType ) ){
					//return rnd.nextLongFromTo( Long.MIN_VALUE, Long.MAX_VALUE );
					//RepastJ 3.1 sometimes (in case of incr params) reads parameters
					//as double from file. It implies that when reading from file later
					//on (when reading results in a dynamic plugin) using repast, only
					//the double's value range can be taken 'safe' (no data loss).
					//Therefore the range of generated longs are restricted here.
					//Sorry about the inconvenience.
					return rnd.nextLongFromTo( -1000000000000000L, 1000000000000000L );
					}else if( "boolean".equalsIgnoreCase( javaType ) ){
					return rnd.nextBoolean() ;
				}else if( "string".equalsIgnoreCase( javaType ) ){
					return "";
				}
			}
		}
		
		return null;
	}
	
	public GeneInfo getInfo() {
		return info;
	}

	public void setInfo(GeneInfo info) {
		this.info = info;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	    
	public Gene cloneGene() throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException{
		Constructor c = null;
		try{
			c = info.getJavaType().getConstructor( info.getJavaType() );
		}catch( NoSuchMethodException e ){
			String javaType = info.getType();
			if( "double".equalsIgnoreCase( javaType ) ){
				c = Double.class.getConstructor( double.class );
			}else if( "float".equalsIgnoreCase( javaType ) ){
				c = Float.class.getConstructor( float.class );
			}else if( "int".equalsIgnoreCase( javaType ) ){
				c = Integer.class.getConstructor( int.class );
			}else if( "integer".equalsIgnoreCase( javaType ) ){
				c = Integer.class.getConstructor( int.class );
			}else if( "short".equalsIgnoreCase( javaType ) ){
				c = Short.class.getConstructor( short.class );
			}else if( "long".equalsIgnoreCase( javaType ) ){
				c = Long.class.getConstructor( long.class );
			}else if( "boolean".equalsIgnoreCase( javaType ) ){
				c = Boolean.class.getConstructor( boolean.class );
			}
		}
		Object newValue = c.newInstance( value );
		return new Gene( info.cloneGeneInfo(), newValue );
	}
}
