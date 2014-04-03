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
package ai.aitia.meme.paramsweep.generator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ai.aitia.meme.gui.IRngSeedManipulatorChangeListener;
import ai.aitia.meme.paramsweep.gui.info.MemberInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterInfo;
import ai.aitia.meme.paramsweep.gui.info.ParameterRandomInfo;

public class RngSeedManipulatorModel implements Serializable {

	private static final long serialVersionUID = 9060223500779158354L;

	public static final String RSM_ELEMENT_NAME = "rng_seed_manipulator";
	public static final String NATURAL_VARIATION_ELEMENT = "NaturalVariation";
	public static final String BLOCKING_ELEMENT = "BlockingInfo";
	public static final String BLOCKING_NAME = "name";
	public static final String BLOCKING_VALUES = "values";
	public static final String BLOCKING_SIZE = "size";

	/** A list of ParameterInfos that can become random seed parameters (the IntelliSweep method should 
	 * fill in this list with candidates). */
	public Vector<ParameterInfo> possibleRandomSeedParameters = null;
	/** A list of the actual random seeds in use. */
	public Vector<ParameterRandomInfo> randomSeedParameters = null;
	/** The last selected index in the list of random seeds. Used in GUI manipulations. */
	public int lastSelectedIdx = -1;
	/** The table model used in selecting the actual random seeds from the candidates. */
	public SeedsTableModel seedsTableModel = null;
	/** Change listeners that are registered to this RngSeedManipulator. They get notified of changes in settings. */
	public Vector<IRngSeedManipulatorChangeListener> rngSeedManipulatorChangeListeners = null;
	/** Blocking helper, an interface of the IntelliSweep method that affects the way blocking is handled. */
	public IBlockingHelper blockingHelper = null;
	/** Information object for blocking. */
	public Vector<RngSeedManipulatorModel.BlockingParameterInfo> blockingInfos = null;
	/** Information objects for natural variation. */
	public ArrayList<NaturalVariationInfo> naturalVariationInfos = null;
	
	//---------------------------------------------------------------------------
	/**
	 * Constructor that creates the RngSeedManipulatorModel and sets the possible RngSeed parameters list and the 
	 * blocking helper.
	 * @param randomSeedCandidateParameters	the list of parameter infos that can be used as random seeds. 
	 * This method filters out boolean and String type parameters. 
	 * @param blockingHelper	an object that implements the BlockingHelper interface, this is usually the 
	 * IntelliSweep plugin
	 */
	public RngSeedManipulatorModel(List<ParameterInfo> randomSeedCandidateParameters, IBlockingHelper blockingHelper) {
		this.blockingHelper = blockingHelper;
		this.possibleRandomSeedParameters = new Vector<ParameterInfo>();
		for (ParameterInfo info : randomSeedCandidateParameters) {
			if (	!info.getType().equalsIgnoreCase("boolean") && 
					!info.getType().equalsIgnoreCase("string")) 
				possibleRandomSeedParameters.add(info);
		}
		randomSeedParameters = new Vector<ParameterRandomInfo>();
		// we put RngSeed (if present) into randomSeedParameters from
		// model.possibleRandomSeedParameters
		/*
		 * int i = 0; if(model.possibleRandomSeedParameters.size()>0){ ParameterInfo
		 * parInf = model.possibleRandomSeedParameters.get(0); while(i <
		 * model.possibleRandomSeedParameters.size() &&
		 * !parInf.getName().equals("RngSeed")){ i++; if(i <
		 * model.possibleRandomSeedParameters.size()) parInf =
		 * model.possibleRandomSeedParameters.get(i); }
		 * if(parInf.getName().equals("RngSeed")){ randomSeedParameters.add(new
		 * ParameterRandomInfo(parInf)); } }
		 */
		rngSeedManipulatorChangeListeners = new Vector<IRngSeedManipulatorChangeListener>();
		updateBlockingInfos(randomSeedCandidateParameters);
		naturalVariationInfos = new ArrayList<NaturalVariationInfo>();
	}
	
	/**
	 * Returns the names of the random seeds.
	 * @return	A string array of the random seed names.
	 */
	public String[] getRandomSeedNames() {
		String[] ret = new String[randomSeedParameters.size()];
		for (int i = 0; i < randomSeedParameters.size(); i++) {
			ret[i] = randomSeedParameters.get(i).getName();
		}
		return ret;
	}
	
	/**
	 * Removes the random seed from the list.
	 * @param seedName	The name of the random seed to be removed.
	 */
	public void removeFromRandomSeeds(String seedName) {
		for (Iterator iter = randomSeedParameters.iterator(); iter.hasNext();) {
			ParameterRandomInfo info = (ParameterRandomInfo) iter.next();
			if (info.getName().equals(seedName)) iter.remove();
		}
	}
	
	/**
	 * Decides whether the ParameterInfo parameter is a random seed in this model. 
	 * @param info	The ParameterInfo object.
	 * @return	true if the ParameterInfo object is also in the seeds list, false if not.
	 */
	public boolean isSeed(ParameterInfo info) {
		boolean ret = false;
		for (int i = 0; i < randomSeedParameters.size() && !ret; i++) {
			ret = randomSeedParameters.get(i).getName().equals(info.getName());
		}
		return ret;
	}

	/**
	 * Decides whether the argument is a name of a random seed in this model.
	 * @param seedName	The name of the seed.
	 * @return	true if a random seed is present in the model with this name, false if not.
	 */
	public boolean isSeed(String seedName) {
		boolean ret = false;
		for (int i = 0; i < randomSeedParameters.size() && !ret; i++) {
			ret = randomSeedParameters.get(i).getName().equals(seedName);
		}
		return ret;
	}

	/**
	 * Searches for a random seed info object by a ParameterInfo object.
	 * @param info	The ParameterInfo for which a random seed is to be found.
	 * @return	The ParameterRandomInfo object if it is present, null otherwise.
	 */
	public ParameterRandomInfo getSeed(ParameterInfo info) {
		ParameterRandomInfo ret = null;
		for (int i = 0; i < randomSeedParameters.size() && ret == null; i++) {
			if (randomSeedParameters.get(i).getName().equals(info.getName())) ret = randomSeedParameters.get(i);
		}
		return ret;
	}

	/**
	 * Searches for a random seed info object by its name.
	 * @param seedName The name of the seed to be searched.
	 * @return	The ParameterRandomInfo object if it is present, null otherwise.
	 */
	public ParameterRandomInfo getSeed(String seedName) {
		ParameterRandomInfo ret = null;
		for (int i = 0; i < randomSeedParameters.size() && ret == null; i++) {
			if (randomSeedParameters.get(i).getName().equals(seedName)) ret = randomSeedParameters.get(i);
		}
		return ret;
	}
	
	/**
	 * Calculates the factor of this random seed model. This is how many times the experiment is replicated (with 
	 * different random seed settings).
	 * @return	The multiplier factor of this random seed model.
	 */
	public int getRunsMultiplierFactor() {
		int ret = 1;
		/* Need this because uncombined seed lists alternate together between experiments, and stop after the 
		 * shortest uncombined list is exhausted. So the experiment is replicated by the shortest list of the 
		 * uncombined seed lists.*/
		int minUncombinedSeedListSize = Integer.MAX_VALUE;  
		for (ParameterRandomInfo info : randomSeedParameters) {
			if (info.getRndType() == ParameterRandomInfo.SWEEP_RND) { //has to be a "sweep" to have a replicating effect
				if (info.isCombined()) {
					ret *= info.getSeedList().size(); //if combined, then design is replicated by the size of its list
				} else {
					//looking for the shortest in uncombined sweep seeds:
					if (info.getSeedList().size() < minUncombinedSeedListSize) minUncombinedSeedListSize = info
							.getSeedList().size();
				}
			}
		}
		//replicating by uncombined seeds if there is any:
		if (minUncombinedSeedListSize != Integer.MAX_VALUE) ret *= minUncombinedSeedListSize; 
		return ret;
	}

	/**
	 * This method does the replication/randomization by altering the parameter tree provided as argument.
	 * @param oldRoot	The parameter tree to be randomized.
	 */
	public void randomizeParameterTree(DefaultMutableTreeNode oldRoot) {
		int runCount = runCount(oldRoot);
		// As a zero step, care about blocking. This is basically randomizing non-seed parameters between runs.
		// This is done by a fixed random source:
		Random blockingRandom = new Random(321354L);
		Vector<ParameterRandomInfo> addedBlockingSeeds = new Vector<ParameterRandomInfo>();
		for (RngSeedManipulatorModel.BlockingParameterInfo info : blockingInfos) {
			if (info.isBlocking()){
				ParameterRandomInfo randInfoToAdd = null;
				for (int i = 0; i < possibleRandomSeedParameters.size() && randInfoToAdd == null; i++) {
					if (possibleRandomSeedParameters.get(i).getName().equals(info.getName())) {
						randInfoToAdd = new ParameterRandomInfo(possibleRandomSeedParameters.get(i));
						// adding to the random seeds as a SEQ type seed, because that is a run-by-run
						// altering randomizing seed that we need for blocking
						randInfoToAdd.setRndType(ParameterRandomInfo.SEQ_RND);
					}
				}
				if (randInfoToAdd != null) {
					int blIndex = 0;
					for (int i = 0; i < runCount; i++) {
						// filling the seed list with the blocking values provided by the user
						randInfoToAdd.getSeedList().add(info.getBlockingValues().get(blIndex));
						blIndex++;
						// using blocking values as a circular list
						if (blIndex >= info.getSize()) blIndex = 0;
					}
					//introducing randomness into seed list
					Collections.shuffle(randInfoToAdd.getSeedList(), blockingRandom);
					addedBlockingSeeds.add(randInfoToAdd);
					randomSeedParameters.add(randInfoToAdd);
				}
			}
		}
		Enumeration children = oldRoot.children();
		// First, we get the SEQ and STEP randomseed settings done:
		// Only have to create the lists in the ParameterInfos, and
		// ParameterRandomInfo.setupParameterRandomInfo(...) does that job.
		for (; children.hasMoreElements();) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
			ParameterInfo info = (ParameterInfo) node.getUserObject();
			if (isSeed(info)) {
				ParameterRandomInfo randInfo = getSeed(info);
				if (randInfo.getRndType() == ParameterRandomInfo.SEQ_RND
						|| randInfo.getRndType() == ParameterRandomInfo.STEP_RND) {
					randInfo.setupParameterRandomInfo(randInfo.getRndType(), randInfo.getValue(), randInfo.getStepStart(),
							randInfo.getStepStep(), randInfo.getSeedList(), runCount);
					node.setUserObject(randInfo);
				}
			}
		}
		// Now we care about the combined SWEEP randomseeds. We have to multiply
		// the lists (in length) as many times as the length of the list of the
		// SWEEP randomseed,
		// and we have to do it for every combined SWEEP seed.
		children = oldRoot.children();
		for (; children.hasMoreElements();) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
			ParameterInfo info = (ParameterInfo) node.getUserObject();
			if (isSeed(info)) {
				ParameterRandomInfo randInfo = getSeed(info);
				if (randInfo.getRndType() == ParameterRandomInfo.SWEEP_RND && randInfo.isCombined()) {
					runCount = runCount(oldRoot);
					randInfo.setupParameterRandomInfo(randInfo.getRndType(), randInfo.getValue(), randInfo.getStepStart(),
							randInfo.getStepStep(), randInfo.getSeedList(), runCount);
					node.setUserObject(randInfo);
					applyCombinedSweepSeedToParameterTree(randInfo, oldRoot);
				}
			}
		}
		// Now the uncombined SWEEP randomseeds get processed (if there is any):
		applyUncombinedSweepSeedsToParameterTree(oldRoot);
		// And last, the RANDOM randomseeds get handled:
		runCount = runCount(oldRoot);
		children = oldRoot.children();
		for (; children.hasMoreElements();) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
			ParameterInfo info = (ParameterInfo) node.getUserObject();
			if (isSeed(info)) {
				ParameterRandomInfo randInfo = getSeed(info);
				if (randInfo.getRndType() == ParameterRandomInfo.RANDOM_RND) {
					randInfo.setupParameterRandomInfo(randInfo.getRndType(), randInfo.getValue(), randInfo.getStepStart(),
							randInfo.getStepStep(), randInfo.getSeedList(), runCount);
					node.setUserObject(randInfo);
				}
			}
		}
		//remove the seeds that were inserted because of blocking:
		for (ParameterRandomInfo info : addedBlockingSeeds) {
			randomSeedParameters.remove(info);
		}
	}
	/**
	 * Applies the uncombined sweep random seeds to the parameter tree. This is done by multiplying the 
	 * lengths of all lists by the length of the shortest uncombined sweep list, copying the old ones to 
	 * fill the longer lists, then filling all uncombined seed lists accordingly.
	 * @param root	The parameter tree to be modified.
	 */
	private void applyUncombinedSweepSeedsToParameterTree(DefaultMutableTreeNode root) {
		Vector<ParameterInfo> parametersWithList = new Vector<ParameterInfo>();
		Vector<ParameterRandomInfo> uncombinedSweepSeeds = new Vector<ParameterRandomInfo>();
		Vector<ParameterInfo> uncombinedSweepSeedsInTree = new Vector<ParameterInfo>();
		Enumeration children = root.children();
		//separating parameters with lists and seeds into two groups: uncombined sweep seeds and others
		for (; children.hasMoreElements();) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
			ParameterInfo info = (ParameterInfo) node.getUserObject();
			if (isSeed(info)) {
				ParameterRandomInfo randInfo = getSeed(info);
				if (randInfo.getRndType() == ParameterRandomInfo.SWEEP_RND && !randInfo.isCombined()) {
					uncombinedSweepSeeds.add(randInfo);
					uncombinedSweepSeedsInTree.add(info);
				} else if (randInfo.getRndType() == ParameterRandomInfo.SEQ_RND
						|| randInfo.getRndType() == ParameterRandomInfo.STEP_RND
						|| randInfo.getRndType() == ParameterRandomInfo.SWEEP_RND) {
					parametersWithList.add(info);
				}
			} else if (info.getDefinitionType() == ParameterInfo.LIST_DEF) {
				parametersWithList.add(info);
			}
		}
		// if there are uncombined sweep seeds:
		if (uncombinedSweepSeeds.size() > 0) {
			int minSeedListSize = Integer.MAX_VALUE;
			int runCount = 0;
			int newRunCount = 0;
			// searching minimum length of seed lists
			for (ParameterRandomInfo actualRandomInfo : uncombinedSweepSeeds) {
				if (actualRandomInfo.getSeedList().size() < minSeedListSize) minSeedListSize = actualRandomInfo
						.getSeedList().size();
			}
			for (ParameterInfo actualInfo : parametersWithList) {
				Vector<Object> oldList = new Vector<Object>(actualInfo.getValues());
				runCount = oldList.size();
				newRunCount = runCount * minSeedListSize;
				Vector<Object> newList = new Vector<Object>(newRunCount);
				// adding original list minSeedList times
				for (int i = 0; i < minSeedListSize; i++) {
					newList.addAll(oldList);
				}
				actualInfo.setValues(newList);
			}
			// creating simultaneously counting seed lists, like:
			// a = [1,1,1,1,2,2,2,2,3,3,3,3]
			// b = [5,5,5,5,6,6,6,6,7,7,7,7]
			for (int i = 0; i < uncombinedSweepSeeds.size(); i++) {
				ParameterRandomInfo seed = uncombinedSweepSeeds.get(i);
				ParameterInfo inTreeInfo = uncombinedSweepSeedsInTree.get(i);
				Vector<Object> newValues = new Vector<Object>(newRunCount);
				for (int j = 0; j < minSeedListSize; j++) {
					for (int k = 0; k < runCount; k++) {
						newValues.add(seed.getSeedList().get(j));
					}
				}
				inTreeInfo.setDefinitionType(ParameterInfo.LIST_DEF);
				inTreeInfo.setRuns(1);
				inTreeInfo.setValues(newValues);
			}
		}
	}

	/**
	 * Applies a combined sweep seed to the parameter tree. That is by replicating the experiment by multiplying it  
	 * by the size of the seed list. Applying combined sweep seed can be done one at a time.
	 * @param randInfo	 the combined sweep seed
	 * @param root	the parameter tree to be replicated
	 */
	private void applyCombinedSweepSeedToParameterTree(ParameterRandomInfo randInfo, DefaultMutableTreeNode root) {
		int runCount = runCount(root);
		int seedListSize = randInfo.getSeedList().size();
		int newRunCount = runCount * seedListSize;
		Enumeration children = root.children();
		for (; children.hasMoreElements();) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
			ParameterInfo info = (ParameterInfo) node.getUserObject();
			if (info.getDefinitionType() == ParameterInfo.LIST_DEF) {
				if (!(isSeed(info) && getSeed(info).getName().equals(randInfo.getName()))) {
					Vector<Object> oldList = new Vector<Object>(info.getValues());
					Vector<Object> newList = new Vector<Object>(newRunCount);
					for (int i = 0; i < seedListSize; i++) {
						newList.addAll(oldList);
					}
					info.setValues(newList);
				}
			}
		}
	}

	/**
	 * Creates a randomized parameter tree for the centerpoint. It will contain
	 * list definitions for the seeds. Returns null on success, an error message
	 * otherwise.
	 * @param centerPointTreeRoot
	 * @param times
	 * @return null if the method was successful
	 */
	public String randomizeCenterPoint(DefaultMutableTreeNode centerPointTreeRoot, int times) {
		int desiredRuncount = getRunsMultiplierFactor() * times;
		Vector<ParameterRandomInfo> old = randomSeedParameters;
		Vector<ParameterRandomInfo> cp = new Vector<ParameterRandomInfo>(old.size());
		for (int i = 0; i < old.size(); i++) {
			ParameterRandomInfo pri = new ParameterRandomInfo(old.get(i));
			pri.setRndType(old.get(i).getRndType());
			for (int j = 0; j < old.get(i).getSeedList().size(); j++) {
				pri.getSeedList().add(old.get(i).getSeedList().get(j));
			}
			cp.add(pri);
		}
		// �j seed-�rt�kek k�sz�t�se:
		boolean sweepPresent = false;
		for (int i = 0; i < cp.size(); i++) {
			ParameterRandomInfo pri = cp.get(i);
			if (pri.getRndType() == ParameterRandomInfo.SWEEP_RND) {
				sweepPresent = true;
				if (pri.getType().toLowerCase().startsWith("int")) {
					int min = Integer.MAX_VALUE;
					int max = Integer.MIN_VALUE;
					for (int j = 0; j < pri.getSeedList().size(); j++) {
						if (((Integer) pri.getSeedList().get(j)) < min) {
							min = (Integer) pri.getSeedList().get(j);
						}
						if (((Integer) pri.getSeedList().get(j)) > max) {
							max = (Integer) pri.getSeedList().get(j);
						}
					}
					//starting above max:
					int start = max + 1;
					//adding new seed values for the centerpoint runs
					for (int j = 0; j < pri.getSeedList().size(); j++) {
						pri.getSeedList().set(j, start + j);
					}
				} else if (pri.getType().toLowerCase().startsWith("long")) {// it is long
					long min = Long.MAX_VALUE;
					long max = Long.MIN_VALUE;
					for (int j = 0; j < pri.getSeedList().size(); j++) {
						if (((Long) pri.getSeedList().get(j)) < min) {
							min = (Long) pri.getSeedList().get(j);
						}
						if (((Long) pri.getSeedList().get(j)) > max) {
							max = (Long) pri.getSeedList().get(j);
						}
					}
					//starting above max:
					long start = max + 1;
					//adding new seed values for the centerpoint runs
					for (int j = 0; j < pri.getSeedList().size(); j++) {
						pri.getSeedList().set(j, start + j);
					}
				} else if (pri.getType().toLowerCase().startsWith("double")) {// it is double
					double min = Double.POSITIVE_INFINITY;
					double max = Double.NEGATIVE_INFINITY;
					for (int j = 0; j < pri.getSeedList().size(); j++) {
						if (((Double) pri.getSeedList().get(j)) < min) {
							min = (Double) pri.getSeedList().get(j);
						}
						if (((Double) pri.getSeedList().get(j)) > max) {
							max = (Double) pri.getSeedList().get(j);
						}
					}
					//starting above max:
					double start = max + 1;
					//adding new seed values for the centerpoint runs
					for (int j = 0; j < pri.getSeedList().size(); j++) {
						pri.getSeedList().set(j, start + j);
					}
				} else if (pri.getType().toLowerCase().startsWith("float")) {// it is float
					float min = Float.POSITIVE_INFINITY;
					float max = Float.NEGATIVE_INFINITY;
					for (int j = 0; j < pri.getSeedList().size(); j++) {
						if (((Float) pri.getSeedList().get(j)) < min) {
							min = (Float) pri.getSeedList().get(j);
						}
						if (((Float) pri.getSeedList().get(j)) > max) {
							max = (Float) pri.getSeedList().get(j);
						}
					}
					//starting above max:
					float start = max + 1;
					//adding new seed values for the centerpoint runs
					for (int j = 0; j < pri.getSeedList().size(); j++) {
						pri.getSeedList().set(j, start + j);
					}
				}
			}
		}// �j �rt�kek a kombin�lt sweepes iz�kben
		if (!sweepPresent) return "There were no sweep seeds, which could be manipulated";
		// seedcsere:
		randomSeedParameters = cp;
		// n�velgetni a seedlist�kat a combined sweepekn�l, hogy t�bb legyen a
		// centerpointfut�s:
		Vector<ParameterRandomInfo> sweeps = new Vector<ParameterRandomInfo>();
		for (int i = 0; i < cp.size(); i++) {
			if (cp.get(i).getRndType() == ParameterRandomInfo.SWEEP_RND) sweeps.add(cp.get(i));
		}
		int sweepIdx = 0;
		while (getRunsMultiplierFactor() < desiredRuncount) {
			if (sweepIdx >= sweeps.size()) sweepIdx = 0;
			sweeps.get(sweepIdx).addOneExtraSeedValue();
			sweepIdx++;
		}
		randomizeParameterTree(centerPointTreeRoot);
		//in case the created design is larger than desired
		Vector<Integer> indicesToRemove = new Vector<Integer>();
		// itt egyenletesen kijel�li a kiveend�ket, azt�n ki is veszi, �s k�sz
		int originalSize = getRunsMultiplierFactor();
		int removeSize = originalSize - desiredRuncount;
		double removeRatio = ((double) removeSize) / originalSize;
		double counter = 0.0;
		for (int i = 0; i < originalSize; i++) {
			counter += removeRatio;
			if (counter - Math.floor(counter) >= 1.0) {
				indicesToRemove.add(i);
				counter -= 1.0;
			}
		}
		removeIndicesFromTree(centerPointTreeRoot, indicesToRemove);
		randomSeedParameters = old;
		return null;
	}
	
	/**
	 * Looks for the shortest list in the parameter tree: that is the number of runs in the experiment.
	 * @param root	The parameter tree to be examined.
	 * @return	the number of runs = the shortest list in the tree
	 */
	protected int runCount(DefaultMutableTreeNode root) {
		Enumeration children = root.children();
		int minRun = Integer.MAX_VALUE;
		for (; children.hasMoreElements();) {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) children.nextElement();
			ParameterInfo info = (ParameterInfo) node.getUserObject();
			if (info.getDefinitionType() == ParameterInfo.LIST_DEF) {
				if (info.getValues().size() < minRun) minRun = info.getValues().size();
			}
		}
		return minRun;
	}

	/**
	 * Removes the the indices in the argument from parameter lists in the tree in the argument. This is 
	 * used in adding centerpoint runs to the (factorial) design, when creating too much, then pruning them. 
	 * @param centerPointTreeRoot	The centerpoint tree.
	 * @param indicesToRemove	The indices to remove.
	 */
	protected static void removeIndicesFromTree(DefaultMutableTreeNode centerPointTreeRoot, List<Integer> indicesToRemove) {
		for (int i = indicesToRemove.size() - 1; i >= 0; i--) {
			int idx = indicesToRemove.get(i);
			for (int j = 0; j < centerPointTreeRoot.getChildCount(); j++) {
				DefaultMutableTreeNode tn = (DefaultMutableTreeNode) centerPointTreeRoot.getChildAt(j);
				ParameterInfo pi = (ParameterInfo) tn.getUserObject();
				if (pi.getDefinitionType() == ParameterInfo.LIST_DEF) {
					pi.getValues().remove(idx);
				}
			}
		}
	}

	public void newSeedsTableModel() {
		seedsTableModel = new SeedsTableModel();
	}

	/**
	 * Updates the list of possible blocking variables, will contain the same elements as in the list 
	 * in the argument.
	 * @param newList	The new list of possible blocking infos.
	 */
	public void updateBlockingInfos(List<? extends ParameterInfo> newList) {
		// if no blocking infos were used, then create a list for them:
		if (blockingInfos == null) blockingInfos = new Vector<RngSeedManipulatorModel.BlockingParameterInfo>();
		// if an element in the old list is not present in the new one, then remove it:
		for (Iterator<RngSeedManipulatorModel.BlockingParameterInfo> blInforIter = blockingInfos.iterator(); blInforIter.hasNext();) {
			RngSeedManipulatorModel.BlockingParameterInfo blInfo = blInforIter.next();
			boolean removeFromList = true;
			for (int i = 0; i < newList.size(); i++) {
				if (newList.get(i).getName().equals(blInfo.getName())) {
					removeFromList = false;
				}
			}
			if (!removeFromList) {
				for (ParameterRandomInfo randInfo : randomSeedParameters) {
					if (randInfo.getName().equals(blInfo.getName())) {
						removeFromList = true;
					}
				}
			}
			if (removeFromList) {
				blInforIter.remove();
			}
		}
		// is an element in the new list is not in the old list, then add it to the old list
		for (Iterator newListIter = newList.iterator(); newListIter.hasNext();) {
			ParameterInfo paramInfo = (ParameterInfo) newListIter.next();
			boolean present = false;
			for (ParameterRandomInfo rngInfo : randomSeedParameters) {
				if (rngInfo.getName().equals(paramInfo.getName())) {
					present = true;
				}
			}
			if (!present) {
				for (RngSeedManipulatorModel.BlockingParameterInfo blockInfo : blockingInfos) {
					if (blockInfo.getName().equals(paramInfo.getName())) {
						present = true;
					}
				}
			}
			if (!present) {
				blockingInfos.add(new RngSeedManipulatorModel.BlockingParameterInfo(paramInfo.getName(), false, paramInfo.getType(), paramInfo
						.getJavaType()));
			}
		}
		// now the old list (blockingInfos field) contains the desired blocking info objects 
		// (maybe not in that order as in the argument)
	}
	
	/**
	 * Loads the random seed model from the provided element.
	 * @param rsmElem	The XML element containing the random seed model to be loaded.
	 */
	public void load(Element rsmElem) {
		NodeList nl = null;
		// getting random seed infos:
		nl = rsmElem.getElementsByTagName(ParameterRandomInfo.PRI_ELEM_NAME);
		if (nl != null && nl.getLength() > 0) {
			randomSeedParameters.clear();
			for (int i = 0; i < nl.getLength(); i++) {
				Element rsmSeedElem = (Element) nl.item(i);
				String name = rsmSeedElem.getAttribute(ParameterRandomInfo.PARAMETER_NAME);
				//looking for the original info in the possible random seed info list:
				ParameterInfo originalInfo = null;
				for (int j = 0; j < possibleRandomSeedParameters.size() && originalInfo == null; j++) {
					if (possibleRandomSeedParameters.get(j).getName().equals(name)) {
						originalInfo = possibleRandomSeedParameters.get(j);
					}
				}
				// creating the random seed info, either based on the original ParameterInfo, or from scratch 
				ParameterRandomInfo tempInfo = null;
				if (originalInfo != null) {
					tempInfo = new ParameterRandomInfo(originalInfo);
				} else {
					tempInfo = new ParameterRandomInfo("To be overwritten", "int", null);
				}
				// loading the random seed info from the XML element
				tempInfo.load(rsmSeedElem);
				randomSeedParameters.add(tempInfo);
			}
		}
		// loading natural variation infos
		nl = null;
		nl = rsmElem.getElementsByTagName(NATURAL_VARIATION_ELEMENT);
		naturalVariationInfos = new ArrayList<NaturalVariationInfo>();
		if (nl != null && nl.getLength() > 0) {
			for (int i = 0; i < nl.getLength(); i++) {
				Element natVarElem = (Element) nl.item(i);
				if (natVarElem.hasAttribute("selected")){
					if (natVarElem.getAttribute("selected").equalsIgnoreCase("true")) {
						naturalVariationInfos.add(new NaturalVariationInfo(natVarElem.getAttribute("name"), true));
					}
					else {
						naturalVariationInfos.add(new NaturalVariationInfo(natVarElem.getAttribute("name"), false));
					}
				} else {
					naturalVariationInfos.add(new NaturalVariationInfo(natVarElem.getAttribute("name"), true));
				}
			}
		}
		// loading blocking infos
		nl = null;
		nl = rsmElem.getElementsByTagName(BLOCKING_ELEMENT);
		if (nl != null && nl.getLength() > 0) {
			for (int i = 0; i < nl.getLength(); i++) {
				Element blElem = (Element) nl.item(i);
				for (int j = 0; j < blockingInfos.size(); j++) {
					if (blockingInfos.get(j).getName().equals(blElem.getAttribute(BLOCKING_NAME))) {
						blockingInfos.get(j).setBlocking(true);
						blockingInfos.get(j).setSize(Integer.parseInt(blElem.getAttribute(BLOCKING_SIZE)));
						blockingInfos.get(j).setBlockingValues(
								parseTypeVector(blElem.getAttribute(BLOCKING_VALUES), blockingInfos.get(j).getType()));
					}
				}
			}
		}
	}

	/**
	 * Saves the random seeds to the provided XML node.
	 * @param rsmNode	The XML node to write to.
	 */
	public void save(Node rsmNode) {
		Document doc = rsmNode.getOwnerDocument();
		//saving random seed infos
		for (int i = 0; i < randomSeedParameters.size(); i++) {
			Element rsmSeedElem = doc.createElement(ParameterRandomInfo.PRI_ELEM_NAME);
			randomSeedParameters.get(i).save(rsmSeedElem);
			rsmNode.appendChild(rsmSeedElem);
		}
		//saving natural variation elements
		for (int i = 0; i < naturalVariationInfos.size(); i++) {
			Element natVarElem = doc.createElement(NATURAL_VARIATION_ELEMENT);
			natVarElem.setAttribute("name", naturalVariationInfos.get(i).getName());
			natVarElem.setAttribute("selected", ""+naturalVariationInfos.get(i).isSelected());
			rsmNode.appendChild(natVarElem);
		}
		//saving blocking infos
		for (int i = 0; i < blockingInfos.size(); i++) {
			if (blockingInfos.get(i).isBlocking()) {
				Element blElem = doc.createElement(BLOCKING_ELEMENT);
				blElem.setAttribute(BLOCKING_NAME, blockingInfos.get(i).getName());
				blElem.setAttribute(BLOCKING_VALUES, listToString(blockingInfos.get(i).getBlockingValues(), " ", 0));
				blElem.setAttribute(BLOCKING_SIZE, "" + blockingInfos.get(i).getSize());
				rsmNode.appendChild(blElem);
			}
		}
	}

	/**
	 * Parses the text argument as a list of space separated values. 
	 * @param text	The text to be parsed.
	 * @param type	The type to be used.
	 * @return	A list of objects that have the runtime type that the type argument indicates. 
	 */
	public static Vector<Object> parseTypeVector(String text, String type) {
		Vector<Object> ret = new Vector<Object>();
		//replacing consecutive whitespace characters to one space, then trim
		text = text.replaceAll("[\\s]+", " ").trim();
		String[] elements = text.split(" ");
		for (String element : elements) {
			Object toAdd = MemberInfo.getValue(element, type);
			if (toAdd != null) {
				ret.add(toAdd);
			}
		}
		return ret;
	}

	/**
	 * Creates a separated String from the list of objects provided.
	 * @param list	The list of objects.
	 * @param separator	The separator to be used.
	 * @param maxElementDisplayed	Limit of the number of converted elements of the list. (Maximum length.) 
	 * If 0 or less, it is unlimited.
	 * @return	A String with the elements converted to String (with toString()), separated with separator.
	 */
	public static String listToString(List<?> list, String separator, int maxElementDisplayed) {
		if (maxElementDisplayed < 1) maxElementDisplayed = list.size();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < maxElementDisplayed - 1 && i < list.size() - 1; i++) {
			sb.append(list.get(i).toString());
			sb.append(separator);
		}
		int end = Math.min(maxElementDisplayed - 1, list.size() - 1);
		if (end > -1) {
			sb.append(list.get(end).toString());
		}
		return sb.toString();
	}

	/**
	 * Creates a separated String from the list of objects provided.
	 * @param list	The list of objects.
	 * @param separator	The separator to be used.
	 * @return	A String with the elements converted to String (with toString()), separated with separator.
	 */
	public static String listToString(List<?> list, String separator) {
		return listToString(list, separator, 0);
	}

	/**
	 * Checks whether blocking is consistent: either there are no blocking parameters, or if there is at least one, 
	 * the random seeds have at least one sweep seed with a list containing more than one element.
	 * @return	true if consistent, false if not
	 */
	public boolean isBlockingConsistent() {
		boolean ret = false;
		boolean blockingSelected = false;
		for (int i = 0; i < blockingInfos.size(); i++) {
			if (blockingInfos.get(i).isBlocking()) {
				blockingSelected = true;
			}
		}
		if (blockingSelected) {
			for (int i = 0; i < randomSeedParameters.size(); i++) {
				if (randomSeedParameters.get(i).getRndType() == ParameterRandomInfo.SWEEP_RND) {
					if (randomSeedParameters.get(i).getSeedList().size() > 1) {
						ret = true;
					}
				}
			}
		} else {
			ret = true;
		}
		return ret;
	}

	/**
	 * Checks if the natural variation settings are valid: either there are natural variation parameters, or 
	 * if there is at least one, the random seeds have at least one sweep seed with a list containing more 
	 * than one element.
	 * @return	true if consistent, false if not
	 */
	public boolean isNaturalVariationConsistent() {
		boolean ret = false;
		boolean natVarSelected = false;
		// see if there is any selected:
		for (int i = 0; i < naturalVariationInfos.size(); i++) {
			if (naturalVariationInfos.get(i).isSelected()) {
				natVarSelected = true;
			}
		}
		if (natVarSelected) {
			// you have to have at least one sweep random seed with more than one element in its list
			for (int i = 0; i < randomSeedParameters.size(); i++) {
				if (randomSeedParameters.get(i).getRndType() == ParameterRandomInfo.SWEEP_RND) {
					if (randomSeedParameters.get(i).getSeedList().size() > 1) {
						ret = true;
					}
				}
			}
		} else {
			ret = true;
		}
		return ret;
	}


	// --------------------------------------------------------------------------
	/**
	 * A TableModel class for the GUI where you can add or remove random seed parameters.
	 * @author Ferschl
	 *
	 */
	public class SeedsTableModel extends AbstractTableModel {
		private static final long serialVersionUID = -3306302755389570858L;
		Vector<Boolean> isSeed = null;

		public SeedsTableModel() {
			isSeed = new Vector<Boolean>();
			for (int i = 0; i < possibleRandomSeedParameters.size(); i++) {
				isSeed.add(new Boolean(isSeed(possibleRandomSeedParameters.get(i))));
			}
		}

		@Override
		public String getColumnName(int columnIndex) {
			switch (columnIndex) {
			case 0:
				return "Selected";
			case 1:
				return "Parameter name";
			case 2:
				return "Type";
			case 3:
				return "Default value";
			default:
				throw new IllegalStateException();
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return (columnIndex == 0);
		}

		public int getColumnCount() {
			return 4;
		}

		public int getRowCount() {
			return possibleRandomSeedParameters.size();
		}

		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
			case 0:
				return isSeed.get(rowIndex);
			case 1:
				return possibleRandomSeedParameters.get(rowIndex).getName();
			case 2:
				return possibleRandomSeedParameters.get(rowIndex).getType();
			case 3:
				return possibleRandomSeedParameters.get(rowIndex).getValue() != null ? possibleRandomSeedParameters.get(
						rowIndex).getValue().toString() : "null";
			default:
				throw new IllegalStateException();
			}
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			if (columnIndex == 0) {
				isSeed.set(rowIndex, (Boolean) aValue);
			}
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return columnIndex == 0 ? Boolean.class : String.class;
		}

		public Vector<Boolean> getSelectedList() {
			return isSeed;
		}
	}


	/**
	 * Parameter info class for the blocking parameters.
	 * @author Ferschl
	 *
	 */
	public static class BlockingParameterInfo extends ParameterInfo {
		private static final long serialVersionUID = -2596604000638078450L;

		private int size;
		private Vector<Object> blockingValues;
		private boolean isBlocking;
	
		public BlockingParameterInfo(String name, boolean isBlocking, String type, Class<?> javaType) {
			super(name, type, javaType);
			this.isBlocking = isBlocking;
			this.blockingValues = new Vector<Object>();
			this.size = 2;
		}
	
		public boolean isBlocking() {
			return isBlocking;
		}
	
		public void setBlocking(boolean isBlocking) {
			this.isBlocking = isBlocking;
		}
	
		public int getSize() {
			return size;
		}
	
		public void setSize(int size) {
			this.size = size;
		}
	
		public Vector<Object> getBlockingValues() {
			return blockingValues;
		}
	
		public void setBlockingValues(Vector<Object> values) {
			this.blockingValues = values;
		}
	
		@Override
		public String toString() {
			if (isBlocking) {
				return getName() + " [" + " " + listToString(blockingValues, ", ", size) + "]";
			} else {
				return "" + getName() + "";
			}
		}
	}
	
	/**
	 * A parameter info class for natural variation parameters.
	 * @author Ferschl
	 *
	 */
	public static class NaturalVariationInfo implements Serializable{
		private static final long serialVersionUID = -4994691357627445127L;

		protected String name = null;
		protected boolean selected = false;
		
		public NaturalVariationInfo(String name, boolean selected) {
			this.name = name;
			this.selected = selected;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean isSelected() {
			return selected;
		}

		public void setSelected(boolean selected) {
			this.selected = selected;
		}
	}


}
