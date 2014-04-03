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
package ai.aitia.meme.paramsweep.intellisweepPlugin.utils;

import java.util.Enumeration;

import javax.swing.tree.DefaultMutableTreeNode;

import ai.aitia.meme.paramsweep.plugin.IntelliSweepPluginDescriptor;

public class DefaultMethodCategorization extends MethodCategorization {
	//=========================================================================
	//method categories
	/** Category for manual parameter definition methods. */
	protected DefaultMutableTreeNode manual;
	/** Category for iterative methods. */
	protected DefaultMutableTreeNode iterative;
	/** Category for Design of Experiments methods. */
	protected DefaultMutableTreeNode doe;
	/** DoE sub-category for screening methods. */
	protected DefaultMutableTreeNode screening;
	/** DoE sub-category for response-surface methods. */
	protected DefaultMutableTreeNode responseSurface;
	/** DoE subcategory for randomized block designs. */
	protected DefaultMutableTreeNode rbd;
	
	protected String manualDesc = "Manual methods";
	protected String itDesc = "Iterative methods";
	protected String doeDesc = 
		"Design of Experiments (DoE) methods \n\n\"In an experiment, we deliberately " +
		"change one or more process variables (or factors) in order to observe the " +
		"effect the changes have on one or more response variables. The (statistical) " +
		"design of experiments (DOE) is an efficient procedure for planning experiments " +
		"so that the data obtained can be analyzed to yield valid and objective conclusions. " +
		"\n\n DOE begins with determining the objectives of an experiment and selecting " +
		"the process factors for the study. An Experimental Design is the laying out of a " +
		"detailed experimental plan in advance of doing the experiment. Well chosen " +
		"experimental designs maximize the amount of \"information\" that can be obtained " +
		"for a given amount of experimental effort. \"\n" +
		"(Source: http://www.itl.nist.gov/div898/handbook/pri/section1/pri11.htm)";
	protected String scrDesc = 
		"Screening designs\n\n\"(when) ... the primary purpose of the " +
		"experiment is to select or screen out the few important main effects from the many " +
		"less important ones. These screening designs are also termed main effects designs.\"\n" +
		"(Source: http://www.itl.nist.gov/div898/handbook/pri/section3/pri33.htm)";
	protected String rsDesc = 
		"Response surface analyzer methods\n\n\"The experiment is designed to allow us to " +
		"estimate interaction and even quadratic effects, and therefore give us an idea of " +
		"the (local) shape of the response surface we are investigating. For this reason, " +
		"they are termed response surface method (RSM) designs. RSM designs are used to:\n\t* " +
		"Find improved or optimal process settings\n\t* Troubleshoot process problems and weak " +
		"points\n\t* Make a product or process more robust against external and non-controllable " +
		"influences. \"Robust\" means relatively insensitive to these influences.\"\n" +
		"(Source: http://www.itl.nist.gov/div898/handbook/pri/section3/pri33.htm)";
	protected String rbdDesc = 
		"Randomized block designs\n\n\"For randomized block designs, there is one factor or variable that is of primary " +
		"interest. However, there are also several other nuisance factors.\n\nNuisance factors " +
		"are those that may affect the measured result, but are not of primary interest. For " +
		"example, in applying a treatment, nuisance factors might be the specific operator who " +
		"prepared the treatment, the time of day the experiment was run, and the room " +
		"temperature. All experiments have nuisance factors. The experimenter will typically " +
		"need to spend some time deciding which nuisance factors are important enough to keep " +
		"track of or control, if possible, during the experiment. \n\nWhen we can control " +
		"nuisance factors, an important technique known as blocking can be used to reduce or " +
		"eliminate the contribution to experimental error contributed by nuisance factors. " +
		"The basic concept is to create homogeneous blocks in which the nuisance factors are " +
		"held constant and the factor of interest is allowed to vary. Within blocks, it is " +
		"possible to assess the effect of different levels of the factor of interest without " +
		"having to worry about variations due to changes of the block factors, which are " +
		"accounted for in the analysis. \n\nA nuisance factor is used as a blocking factor if " +
		"every level of the primary factor occurs the same number of times with each level of " +
		"the nuisance factor. The analysis of the experiment will focus on the effect of " +
		"varying levels of the primary factor within each block of the experiment. \n\nThe " +
		"general rule is:\n\n\t\"Block what you can, randomize what you cannot.\" \n\nBlocking " +
		"is used to remove the effects of a few of the most important nuisance variables. " +
		"Randomization is then used to reduce the contaminating effects of the remaining " +
		"nuisance variables. \n\nOne useful way to look at a randomized block experiment is " +
		"to consider it as a collection of completely randomized experiments, each run " +
		"within one of the blocks of the total experiment.\"\n" +
		"(Source: http://www.itl.nist.gov/div898/handbook/pri/section3/pri332.htm)";
	
	//=========================================================================
	//constructors
	public DefaultMethodCategorization(){
		manual = new DefaultMutableTreeNode( new MethodCategory( "Manual", manualDesc, null ) );
		root.add( manual );
		iterative = new DefaultMutableTreeNode( new MethodCategory( "Iterative", itDesc, null ) );
		root.add( iterative );
		
		MethodCategory cat = new MethodCategory( "Design of Experiments Methods", doeDesc, null );
		doe = new DefaultMutableTreeNode( cat );
		root.add( doe );
		screening = 
			new DefaultMutableTreeNode( new MethodCategory( "Screening Methods", scrDesc, cat ) );
		doe.add( screening );
		responseSurface = 
			new DefaultMutableTreeNode( new MethodCategory( "Response Surface Methods", 
					rsDesc, cat ) );
		doe.add( responseSurface );
		rbd = 
			new DefaultMutableTreeNode( new MethodCategory( "Randomized Block Designs", 
					rbdDesc, cat ) );
		doe.add( rbd );
	}

	//=========================================================================
	//implemented interfaces
	/**
	 * Adds a plugin descriptor to the categorization.
	 * 
	 * @param desc is the plugin descriptor to be added.
	 */
	@Override
	public void putMethodToCategorization(IntelliSweepPluginDescriptor desc) {
		if( "Manual method".equals( desc.getLocalizedName() ) ){
			//manual method
			manual.add( new DefaultMutableTreeNode( desc ) );
			rebuildTree( manual );
		}else{
			if( "Iterative Uniform Interpolation".equals( desc.getLocalizedName() ) ){
				iterative.add( new DefaultMutableTreeNode( desc ) );
				rebuildTree( iterative );
			}else if( "Factorial".equals( desc.getLocalizedName() ) ){
				screening.add( new DefaultMutableTreeNode( desc ) );
				rebuildTree( screening );
			}else if( "Latin Hypercube Design".equals( desc.getLocalizedName() ) ){
				rbd.add( new DefaultMutableTreeNode( desc ) );
				rebuildTree( rbd );
			}else if( "Box-Behnken".equals( desc.getLocalizedName() ) ){
				responseSurface.add( new DefaultMutableTreeNode( desc ) );
				rebuildTree( responseSurface );
			}else if( "Standard Design of Experiments plugin".equals( desc.getLocalizedName() ) ){
				manual.add( new DefaultMutableTreeNode( desc ) );
				rebuildTree( manual );
			}else if( "Three level factorial".equals( desc.getLocalizedName() ) ){
				screening.add( new DefaultMutableTreeNode( desc ) );
				rebuildTree( screening );
			}else if( "Central composite".equals( desc.getLocalizedName() ) ){
				responseSurface.add( new DefaultMutableTreeNode( desc ) );
				rebuildTree( responseSurface );
			}else if( "Adaptive Nonlinear Tests/Genetic Algorithm".equals( desc.getLocalizedName() ) ){
				iterative.add( new DefaultMutableTreeNode( desc ) );
				rebuildTree( iterative );
			}else{ 
				root.add( new DefaultMutableTreeNode( desc ) );
			}
		}
	}

	//=========================================================================
	//private & protected functions
	/**
	 * Addds the category to the category tree if its not present already.
	 * 
	 * @param catNode is the category to be added.
	 */
	@SuppressWarnings("cast")
	private DefaultMutableTreeNode rebuildTree( DefaultMutableTreeNode catNode ){
		if( catNode == null ) return null;
		
		if( !(catNode.getUserObject() instanceof MethodCategory) ) return null;
		
		MethodCategory cat = (MethodCategory)catNode.getUserObject();
		
		Enumeration preorder = root.preorderEnumeration();
		while( preorder.hasMoreElements() ){
			DefaultMutableTreeNode actNode = 
								(DefaultMutableTreeNode)preorder.nextElement();
			Object nodeObject = actNode.getUserObject();
			if( nodeObject instanceof MethodCategory &&
					cat.equals( (MethodCategory)nodeObject ) ){
				return actNode;
			}
			
		}
		
		if( cat.getParent() == null ){
			root.add( catNode );
		}else{
			DefaultMutableTreeNode parent = 
				rebuildTree( new DefaultMutableTreeNode( cat.getParent() ) );
			parent.add( catNode );
		}
		

		return catNode;
	}
}
