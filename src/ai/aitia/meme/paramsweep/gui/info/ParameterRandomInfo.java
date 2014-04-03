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
package ai.aitia.meme.paramsweep.gui.info;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ai.aitia.meme.paramsweep.generator.RngSeedManipulatorModel;
import cern.jet.random.engine.MersenneTwister;

/**
 * A class that contains information about a parameter that is intended to be
 * used as a random number generator seed (RngSeed).
 * @author Ferschl
 */
public class ParameterRandomInfo extends ParameterInfo{
	
    private static final long serialVersionUID = -2982059465235825089L;
	/** The default behavior of the RngSeed, does not change between runs. */
	public static final int DEF_RND = 0;
	/**	The IntelliSweep will do its full sweep multiple times with the seeds in
	 * <code>seedList</code> */
	public static final int SWEEP_RND = 1;
	/** RngSeed will change randomly between runs. It uses a uniform 
	 * distribution on the interval Integer.MIN_VALUE..Integer.MAX_VALUE. */
	public static final int RANDOM_RND = 2;
	/** This setting will change the RngSeed between runs according to <code>seedList.</code>
	 * If it reaches the end of <code>seedList</code>, it goes back to the beginning. */
	public static final int SEQ_RND = 3;
	/**	This setting will create a new RngSeed between runs by incrementing a 
	 * value starting from <code>stepStart</code>, adding <code>stepStep</code>
	 * every time.*/
	public static final int STEP_RND = 4;

	public static final String PRI_ELEM_NAME = "random_parameter_info";
	public static final String RND_TYPE_ATTR = "randomseed_type";
	private static final String STEP_START = "rng_step_start";
	private static final String STEP_STEP = "rng_step_step";
	private static final String SEED_LIST = "seed_list";
	private static final String SEED_COMBINED = "combined";
	private static final String PARAMETER_TYPE = "parameter_type";
	public static final String PARAMETER_NAME = "parameter_name";
	private static final String DEFAULT_VALUE = "default_value";
	
	
	
	/**	Determines the behaviour of the random parameter represented by this object. */
	protected int rndType = DEF_RND;
	
	protected Vector<Object> seedList = null;
	protected long stepStart = 0;
	protected long stepStep = 1;
	protected boolean combined = true;

	public ParameterRandomInfo(String name, String type, Class<?> javaType) {
		super(name, type, javaType);
		this.runs = 1L;
	}
	
	/**
	 * copy constructor
	 * @param other
	 */
	public ParameterRandomInfo(ParameterInfo other){
		super(other.name, other.type, other.javaType);
		this.setValue(other.getValue());
		this.runs = 1L;
		this.seedList = new Vector<Object>();
	}
	
	/**
	 * A method to setup the object. 
	 * @param rndType
	 * @param defValue
	 * @param stepStart
	 * @param stepStep
	 * @param seedList
	 */
	public void setupParameterRandomInfo(
			int rndType, 
			Object defValue,
			long stepStart, 
			long stepStep, 
			Vector<Object> seedList,
			int numberOfRuns){
		this.runs = 1L;
		this.rndType = rndType;
		if(rndType == STEP_RND){
			this.stepStart = stepStart;
			this.stepStep = stepStep;
		} else if(rndType == SWEEP_RND || rndType == SEQ_RND){
			this.seedList = seedList;
		} else if(rndType == DEF_RND){
			setValue(defValue);
			setDefinitionType(CONST_DEF);
		}
		finalizeParameterRandomInfo(numberOfRuns);
	}
	
	/**
	 * This method creates the values list according to this object's settings,
	 * and the numberOfRuns parameter. 
	 * @param numberOfRuns	How many runs the plugin wants to run.
	 */
	protected void finalizeParameterRandomInfo(int numberOfRuns){
		if(rndType == SWEEP_RND){
			//You have to do something on the parameter-tree level to make the
			//whole tree consistent (all lists with the same length)
			int newNumberOfRuns = numberOfRuns * seedList.size();
			ArrayList<Object> newValues = new ArrayList<Object>(newNumberOfRuns);
			setDefinitionType(LIST_DEF);
			for (int i = 0; i < seedList.size(); i++) {
	            for (int j = 0; j < numberOfRuns; j++) {
	                newValues.add(seedList.get(i));
                }
            }
			setValues(newValues);
		} else if(rndType == STEP_RND){
			ArrayList<Object> newValues = new ArrayList<Object>(numberOfRuns);
			setDefinitionType(LIST_DEF);
			long actualStepValue = stepStart;
			for (int j = 0; j < numberOfRuns; j++) {
				newValues.add(new Long(actualStepValue));
				actualStepValue += stepStep;
			}
			setValues(newValues);
		} else if(rndType == SEQ_RND){
			ArrayList<Object> newValues = new ArrayList<Object>(numberOfRuns);
			setDefinitionType(LIST_DEF);
			int actualSequenceIndex = 0;
			for (int j = 0; j < numberOfRuns; j++) {
				newValues.add(seedList.get(actualSequenceIndex));
				actualSequenceIndex++;
				if(actualSequenceIndex >= seedList.size()) actualSequenceIndex = 0;
			}
			setValues(newValues);
		} else if(rndType == DEF_RND){
			setDefinitionType(CONST_DEF); //this may be unnecessary
			//the value has been already set in setupParameterRandomInfo()
		} else if(rndType == RANDOM_RND){
			MersenneTwister gen = new MersenneTwister(new Date());
			ArrayList<Object> newValues = new ArrayList<Object>(numberOfRuns);
			setDefinitionType(LIST_DEF);
			for (int j = 0; j < numberOfRuns; j++) {
				if(type.compareToIgnoreCase("long") == 0){
					newValues.add(new Long(gen.nextLong()));
				} else{
					newValues.add(new Integer(gen.nextInt()));
				}
			}
			setValues(newValues);
		}
	}
	
	
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder(this.name);
		ret.append(" [");
		switch (rndType) {
		case DEF_RND:
			ret.append("Random seed, value = ");
			ret.append(this.getValue());
			break;
		case RANDOM_RND:
			ret.append("(Random) random seed at each run");
			break;
		case SWEEP_RND:
			if(combined){
				ret.append("(Sweep-combined) combined multiple sweep with these seed values: ");
			} else{
				ret.append("(Sweep) multiple sweep with these seed values: ");
			}
			if(seedList != null)
				for (Object seed : seedList) {
					ret.append(seed);
					ret.append(" ");
				}
			if(seedList.size() == 1){
				ret = new StringBuilder(this.name);
				ret.append(" [Random seed, value = ");
				ret.append(this.seedList.get(0));
			}
			break;
		case SEQ_RND:
			ret.append("(Sequence) using this sequence of seeds in a loop, changing at each run: ");
			if(seedList != null)
				for (Object seed : seedList) {
					ret.append(seed);
					ret.append(" ");
				}
			if(seedList.size() == 1){
				ret = new StringBuilder();
				ret.append(" [Random seed, value = ");
				ret.append(this.seedList.get(0));
			}
			break;
		case STEP_RND:
			ret.append("(Step) generating a new seed starting from ");
			ret.append(stepStart);
			ret.append(" incrementing it with ");
			ret.append(stepStep);
			ret.append(" at each run");
			break;
		}
		ret.append("]");
		return ret.toString();
	}

	//	----------------------------------------------------------------------------
	public int getRndType() { return rndType; }
	public void setRndType(int rndType) { this.rndType = rndType; }
//	----------------------------------------------------------------------------	
	public Vector<Object> getSeedList() { return seedList; }
	public void setSeedList(Vector<Object> seedList) { this.seedList = seedList; }
//	----------------------------------------------------------------------------	
	public long getStepStart() { return stepStart; }
	public void setStepStart(long stepStart) { this.stepStart = stepStart; }
//	----------------------------------------------------------------------------	
	public long getStepStep() { return stepStep; }
	public void setStepStep(long stepStep) { this.stepStep = stepStep; }
//	----------------------------------------------------------------------------	
	public boolean isCombined() { return combined; }
	public void setCombined(boolean combined) { this.combined = combined; }
//	----------------------------------------------------------------------------	

	
	public void save(Node priNode){
		//name
		((Element) priNode).setAttribute(PARAMETER_NAME, name);
		//rndType
		((Element) priNode).setAttribute(RND_TYPE_ATTR, ""+rndType);
		//stepStart
		((Element) priNode).setAttribute(STEP_START, ""+stepStart);
		//stepStep
		((Element) priNode).setAttribute(STEP_STEP, ""+stepStep);
		//default value
		((Element) priNode).setAttribute(DEFAULT_VALUE, getValue().toString());
		//seedList -> as a space separated list
		StringBuffer seedListStr = new StringBuffer(200);
		if(seedList != null){
			for (int i = 0; i < seedList.size()-1; i++) {
				seedListStr.append(seedList.get(i));
				seedListStr.append(" ");
			}
			seedListStr.append(seedList.get(seedList.size()-1));
		}
        ((Element) priNode).setAttribute(SEED_LIST, seedListStr.toString());
		//combined
        ((Element) priNode).setAttribute(SEED_COMBINED, combined ? "true" : "false");
        //((Element) priNode).setAttribute(SEED_COMBINED, "true");
		//type (from ParameterInfo) -> for the loading
        ((Element) priNode).setAttribute(PARAMETER_TYPE, type);
	}
	
	public void load(Element priElem){
		this.name = priElem.getAttribute(PARAMETER_NAME);
		this.type = priElem.getAttribute(PARAMETER_TYPE);
		this.rndType = Integer.parseInt(priElem.getAttribute(RND_TYPE_ATTR));
		this.stepStart = Long.parseLong(priElem.getAttribute(STEP_START));
		this.stepStep = Long.parseLong(priElem.getAttribute(STEP_STEP));
		this.combined = Boolean.parseBoolean(priElem.getAttribute(SEED_COMBINED));
//		this.combined = true;
		this.setValue(priElem.getAttribute(DEFAULT_VALUE));
		this.seedList = RngSeedManipulatorModel.parseTypeVector(priElem.getAttribute(SEED_LIST), this.type);

		//eliminating the "default" setting of the Seed, changing it to the new
		//default: Combined Sweep RandomSeed
		if(rndType == DEF_RND){
			rndType = SWEEP_RND;
			combined = true;
			seedList = new Vector<Object>();
			if(getValue().toString().length() > 0)
				seedList.add(getValue());
		}
	}

	/**
	 * This method adds an extra seed value to the list of this random seed info. This is needed when adding more 
	 * than one centerpoint to the design, because multiple centerpoint runs require additional random seed values 
	 * to achieve different responses for the multiple runs (in the case of a deterministic model).
	 */
	public void addOneExtraSeedValue() {
		if (getType().toLowerCase().startsWith("int")) {
			Integer toAdd = ((Integer) getSeedList().get(getSeedList().size() - 1)) + 1;
			getSeedList().add(toAdd);
		} else if (getType().toLowerCase().startsWith("long")) {// it is long
			Long toAdd = ((Long) getSeedList().get(getSeedList().size() - 1)) + 1;
			getSeedList().add(toAdd);
		} else if (getType().toLowerCase().startsWith("float")) {
			Float toAdd = ((Float) getSeedList().get(getSeedList().size() - 1)) + 1;
			getSeedList().add(toAdd);
		} else if (getType().toLowerCase().startsWith("double")) {
			Double toAdd = ((Double) getSeedList().get(getSeedList().size() - 1)) + 1;
			getSeedList().add(toAdd);
		}
	}
	
	public Comparator<ParameterRandomInfo> getComparator(){
		return new ParameterRandomInfoComparator();
	}
	
	public class ParameterRandomInfoComparator implements Comparator<ParameterRandomInfo>{
		public int compare(ParameterRandomInfo a, ParameterRandomInfo b) {
			Integer aRank = -1;
			Integer bRank = -1;
			switch (a.rndType) {
            case DEF_RND:
	            aRank = 0;
	            break;
            case SEQ_RND:
	            aRank = 1;
	            break;
            case STEP_RND:
	            aRank = 2;
	            break;
            case SWEEP_RND:
	            if(a.combined) aRank = 3;
	            else aRank = 4;
	            break;
            case RANDOM_RND:
	            aRank = 5;
            }
			switch (b.rndType) {
            case DEF_RND:
	            bRank = 0;
	            break;
            case SEQ_RND:
	            bRank = 1;
	            break;
            case STEP_RND:
	            bRank = 2;
	            break;
            case SWEEP_RND:
	            if(b.combined) bRank = 3;
	            else bRank = 4;
	            break;
            case RANDOM_RND:
	            bRank = 5;
            }
			if(aRank == bRank) return a.name.compareTo(b.name);
			else{
				return aRank.compareTo(bRank);
			}
		}
	}
}
