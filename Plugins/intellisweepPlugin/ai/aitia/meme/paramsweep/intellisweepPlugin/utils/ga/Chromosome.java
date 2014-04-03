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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class Chromosome implements Serializable, Comparable<Chromosome> {
	//=========================================================================
	//members
	private static final long serialVersionUID = -6811547486203309733L;
	protected List<Gene> genes;
	protected Double fitness = Double.NaN;
	
	//=========================================================================
	//constructors
	public Chromosome(){
		genes = new ArrayList<Gene>();
	}
	
	//=========================================================================
	//public functions
	public void addGene( Gene gene ){
		if( !genes.contains( gene ) ) genes.add( gene );
	}

	public void addGene( int idx, Gene gene ){
		if( !genes.contains( gene ) && genes != null 
				&& idx > -1 && idx < genes.size() ) genes.add( idx, gene );
	}
	
	public void removeGene( Gene gene ){
		if( genes != null ) genes.remove( gene );
	}
	
	public Gene geneAt( int idx ){
		if( genes != null && idx > -1 && idx < genes.size() ) 
			return genes.get( idx );
		return null;
	}
	
	public int getSize(){
		if( genes == null ) return 0;
		return genes.size();
	}

	public Double getFitness() {
		return fitness;
	}

	public void setFitness(Double fitness) {
		this.fitness = fitness;
	}
	
	public Chromosome cloneChromosome() throws SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException{
		Chromosome ch = new Chromosome();
		for( Gene g : genes ){
			ch.addGene( g.cloneGene() );
		}
		ch.fitness = new Double( fitness );
		return ch;
	}

	public int compareTo(Chromosome o) {
		return this.fitness.compareTo(o.fitness);
	}
	
}
