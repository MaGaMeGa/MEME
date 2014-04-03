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
package ai.aitia.meme.paramsweep.intellisweepPlugin.utils.ga.jgap;

import java.util.Map;

import org.jgap.IChromosome;

public class MEMEFitnessFunction extends org.jgap.FitnessFunction{
	//=========================================================================
	//members
	static final long serialVersionUID = 123342215573L;
	
	protected Map<String,Double> fitnessValues;
	
	//=========================================================================
	//constructors
	public MEMEFitnessFunction( Map<String,Double> fitnessValues ){
		this.fitnessValues = fitnessValues;
	}
	
	//=========================================================================
	//private & protected functions
	@Override
	protected double evaluate( IChromosome arg0 ) {
		String key = "";
		for( int i = 0; i < arg0.getGenes().length; ++i ){
			key += arg0.getGene( i ).getAllele();
			//TODO: add delimiter to STOP THE WAR!!! - look out for the Map<> creation as well!
		}
		
		Double fitness = fitnessValues.get( key );
		
		return fitness == null ? 0.0: fitness;
	}

}
