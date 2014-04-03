/*
 * Version info:
 *     $HeadURL: https://cscs-repast-demos.googlecode.com/svn/richard/StupidModel/tags/2011_05_26_model_01/src/stupidmodel/Constants.java $
 *     $LastChangedDate: 2011-05-26 21:03:50 +0200 (Cs, 26 máj. 2011) $
 *     $LastChangedRevision: 149 $
 *     $LastChangedBy: richard.legendi@gmail.com $
 */
package stupidmodel;

/**
 * Dedicated class to store some model-specific constant values (e.g. bug vision
 * ranges and context IDs) that is uninstantiable.
 * 
 * <p>
 * The different IDs are usually set in
 * {@link StupidModelContextBuilder#build(repast.simphony.context.Context)} and
 * used to access the shared structures (<code>Grid</code>, <code>Space</code>
 * or <code>Context</code>) easily.
 * </p>
 * 
 * <p>
 * Final class, cannot be extended.
 * </p>
 * 
 * @author Richard O. Legendi (richard.legendi)
 * @since 2.0-beta, 2011
 * @version $Id: Constants.java 149 2011-05-26 19:03:50Z richard.legendi@gmail.com $
 */
public final class Constants {

	/** The root <code>Context</code> ID. */
	public static final String CONTEXT_ID = "StupidModel";

	/** The <code>Space</code> ID. */
	public static final String SPACE_ID = "space";

	/** The <code>Grid</code> ID. */
	public static final String GRID_ID = "grid";

	/** The first model specified a two-dimensional grid <code>100 * 100</code>. */
	public static final int GRID_SIZE = 100;

	/**
	 * The first model specified to create <code>100</code> {@link Bug} agents.
	 */
	public static final int BUG_COUNT = 100;

	/**
	 * The first model specified an agent vision range of <code>4 * 4</code>
	 * cells.
	 */
	public static final int BUG_VISION_RANGE = 4;

	/**
	 * Hidden constructor to ensure no instances are created.
	 */
	private Constants() {
		;
	}

}
