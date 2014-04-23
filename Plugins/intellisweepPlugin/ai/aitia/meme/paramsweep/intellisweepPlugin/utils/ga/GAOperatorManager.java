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

import static ai.aitia.meme.utils.Utils.getLocalizedMessage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import ai.aitia.meme.Logger;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.jgap.GAOperationException;
import ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.jgap.JGAPOperatorWrapper;

public class GAOperatorManager implements Serializable {
	//=========================================================================
	//members
	private static final long serialVersionUID = 7002467039412662131L;
	protected List<IGAOperator> operators = new ArrayList<IGAOperator>();
	protected List<IGAOperator> selectedOps;
	
	protected String[] knownPackageNames = { "JGAP" };
	protected Map<String,Class<?>> opTypes = new HashMap<String,Class<?>>();

	//=========================================================================
	//constructors
	public GAOperatorManager() {
		operators = new ArrayList<IGAOperator>();
		selectedOps = new ArrayList<IGAOperator>();
		
		opTypes.put( knownPackageNames[0], org.jgap.GeneticOperator.class );
	}
	
	//=========================================================================
	//public functions
	public void loadOperators(){
		if( getOperatorByName( new OnePointCrossover().getName() ) == null )
			operators.add( new OnePointCrossover() );
		if( getOperatorByName( new UniformCrossover().getName() ) == null )
			operators.add( new UniformCrossover() );
		if( getOperatorByName( new AveragingCrossover().getName() ) == null )
			operators.add( new AveragingCrossover() );
		if( getOperatorByName( new DefaultMutation().getName() ) == null )
			operators.add( new DefaultMutation() );
		if( getOperatorByName( new TournamentSelection().getName() ) == null )
			operators.add( new TournamentSelection() );
		if( getOperatorByName( new FitnessProportionateSelection().getName() ) == null )
			operators.add( new FitnessProportionateSelection() );
		if( getOperatorByName( new BestSelection().getName() ) == null )
			operators.add( new BestSelection() );
	}
	
	public List<IGAOperator> getOperators(){
		return operators;
	}
	
	public List<IGAOperator> getSelectedOperators(){
		return selectedOps;
	}
	
	public void applySelectedOps( List<Chromosome> population, 
								List<Chromosome> nextPopulation, 
								boolean maximizeFitness)
										throws GAOperationException{
		for( IGAOperator operator : selectedOps ){
			operator.operate( population, nextPopulation, maximizeFitness ); //TODO: change 'true' to true value :)
		}
	}
	
	public void selectOp( IGAOperator op ){
		if( selectedOps != null && !selectedOps.contains( op ) )
			selectedOps.add( op );
	}
	
	public void selectOp( int idx, IGAOperator op ){
		if( !selectedOps.contains( op ) && selectedOps != null 
				&& idx > -1 && idx < selectedOps.size() )
			selectedOps.add( idx, op );
	}
	
	public void deselectOp( IGAOperator op ){
		if( selectedOps != null ) selectedOps.remove( op );
	}
	
	public IGAOperator selectedOpAt( int idx ){
		if( selectedOps != null && idx > -1 && idx < selectedOps.size() )
			return selectedOps.get( idx );
		return null;
	}
	
	public IGAOperator operatorAt( int idx ){
		if( operators != null && idx > -1 && idx < operators.size() )
			return operators.get( idx );
		return null;
	}
	
	public IGAOperator getOperatorByName( String name ){
		if( name == null ) return null;
		for( IGAOperator op : operators ){
			if( op.getName().equals( name ) ) return op;
		}
		
		return null;
	}
	
	public List<IGAOperator> getOperatorsByPackage( String packageName ){
		if( packageName == null ) return null;
		List<IGAOperator> ops = new ArrayList<IGAOperator>();
		
		for( IGAOperator op : operators ){
			if( packageName.equals( op.getPackageName() ) ) ops.add( op );
		}
		
		return ops;
	}
	
	public void removeOperatorsByPackage( String packageName ){
		if( packageName == null ) return;
		
		for( int i = 0; i < operators.size(); ){
			IGAOperator op = operators.get( i );
			if( packageName.equals( op.getPackageName() ) ){
				operators.remove( i );
			}else{
				++i;
			}
		}
		
	}
	
	public String[] getKnownPackageNames() {
		return knownPackageNames;
	}
	
	@SuppressWarnings({ "deprecation", "unchecked" })
	public void loadOperators( String packageName, String jarPath ) throws Exception{
		Class<?> opType = opTypes.get( packageName );
		File jarFile = new File( jarPath );
		@SuppressWarnings("unused")
		String files[], clsname;
		ZipFile zf = null;
		@SuppressWarnings("unused")
		InputStream is;
		String err = null;
		
		if (!jarFile.exists()) {
			Logger.logError( "Cannot locate the jar file '%s'", jarPath );
			throw new Exception("Cannot locate the jar file");
		}
		try{
			zf = new ZipFile( jarFile );
		}
		catch (IOException ex) {
			Logger.logError( "'%s' is not a jar file", jarPath );
			throw new Exception("The specified file is not a jar file");
		}
		try {
			URLClassLoader opLoader = new URLClassLoader(new URL [] {jarFile.toURL()}); 
			
			try {
		         // Loop through the zip entries and print the name of each one.
		      
		         for (Enumeration list = zf.entries(); list.hasMoreElements(); ) {
		            ZipEntry entry = (ZipEntry) list.nextElement();
		            //String className = entry.getName().replace( "/", "." );
		            String clsName = entry.getName();
		            if( clsName.endsWith( ".class" ) ){
		            	clsName = clsName.substring( 0, clsName.length() - 6 ); //cut ".class".length()
		            	clsName = clsName.replace( "/", "." );
			            try{
				            Class cls = opLoader.loadClass( clsName );
				            
				            if( opType.isAssignableFrom( cls ) ){
					            cls.getConstructor(); //throws NoSuchMethoException if there's no nullary const.
				            	operators.add(  new JGAPOperatorWrapper( cls ) );
				            }
			            }catch( ClassNotFoundException ex ){
			    			//couldn't load class
			            	Logger.logError("(GAOperatorManager) Error while loading " +
			            						"class %s", clsName);
			    		}catch( NoSuchMethodException ex ){
			    			//there's no nullary constructor
			    		}catch( SecurityException ex ){
			    			//there's public no nullary constructor
			    		}catch( NoClassDefFoundError ex ){
			    			Logger.logError("(GAOperatorManager) Error while loading class %s: " +
			    								"cannot find %s", clsName, ex.getMessage());
			    		}
		            }
		         }
		      }
		      finally {
		         zf.close();
		      }
		}catch (IOException ex) {
			err = getLocalizedMessage(ex);
		}
		if (err != null) {
			Logger.logError("Error reading file %s: %s", jarFile, err);
		}
	}
	
	//=========================================================================
	//private & protected functions
}
