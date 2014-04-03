/*
 * Version info:
 *     $HeadURL: https://cscs-repast-demos.googlecode.com/svn/richard/StupidModel/tags/2011_05_26_model_01/src/stupidmodel/Bug.java $
 *     $LastChangedDate: 2011-07-13 13:31:08 +0200 (Sze, 13 júl. 2011) $
 *     $LastChangedRevision: 615 $
 *     $LastChangedBy: richard.legendi@gmail.com $
 */
package stupidmodel;

import java.util.List;

import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;

/**
 * Bug agent implementation for <i>StupidModel 1</i>.
 * 
 * <p>
 * Agents in this model are very simple: they can access their neihgbourhood at
 * a grid and move to a random position within sight range (that is a fixed
 * constant for now, see {@link Constants#BUG_VISION_RANGE}.
 * </p>
 * 
 * <p>
 * If there is no free cell around the agent, it cannot move, so it stays in its
 * current position.
 * </p>
 * 
 * @author Richard O. Legendi (richard.legendi)
 * @since 2.0-beta, 2011 @
 * @version $Id: Bug.java 615 2011-07-13 11:31:08Z richard.legendi@gmail.com $
 */
public class Bug {

	/** A reference to the grid on which the agent is located at. */
	private final Grid<Object> grid;

	/**
	 * Creates a new instance of <code>Bug</code> associated with the specified
	 * {@link Grid}.
	 * 
	 * @param grid
	 *            the <code>Grid</code> on which the agent is located; <i>cannot
	 *            be <code>null</code></i>
	 */
	public Bug(final Grid<Object> grid) {
		super();

		if (null == grid) {
			throw new IllegalArgumentException("Parameter grid cannot be null.");
		}

		this.grid = grid;
	}

	/**
	 * Implementation of the agent activity in each turn.
	 * 
	 * <p>
	 * Using the annotation {@link ScheduledMethod} makes this
	 * <code>step()</code> method executed from the first simulation tick, and
	 * with specifying interval it is executed each tick afterwards.
	 * </p>
	 * 
	 * <p>
	 * Agents work in a very simple way: they gather their neighbourhood and
	 * check for empty locations. If any is found, one of them is randomly
	 * chosen and the agent is relocated to that location.
	 * </p>
	 */
	@ScheduledMethod(start = 1, interval = 1)
	public void step() {
		// Get the grid location of this Bug
		final GridPoint location = grid.getLocation(this);

		// We use the GridCellNgh class to create GridCells for the surrounding
		// neighborhood. It contains the locations, and a list of objects from
		// the specified class which is accessible from that location

		final List<GridCell<Bug>> bugNeighborhood = new GridCellNgh<Bug>(grid,
				location, Bug.class, Constants.BUG_VISION_RANGE,
				Constants.BUG_VISION_RANGE).getNeighborhood(false);

		// We have a utility function that returns the filtered list of empty
		// GridCells objects
		final List<GridCell<Bug>> freeCells = SMUtils
				.getFreeGridCells(bugNeighborhood);

		// Model specifies if there is no empty location in vision range, the
		// Bug agent cannot move
		if (freeCells.isEmpty()) {
			return;
		}

		// This is the way to shuffle an arbitrary list
		// The description of the model suggests using it, however, we choose a
		// random element from the list instead of the first one so it is not
		// vital
		SimUtilities.shuffle(freeCells, RandomHelper.getUniform());

		// Get a random free location within sight range
		final GridCell<Bug> chosenFreeCell = SMUtils.randomElementOf(freeCells);

		// We have our new GridPoint to move to, so relocate agent
		final GridPoint newGridPoint = chosenFreeCell.getPoint();
		grid.moveTo(this, newGridPoint.getX(), newGridPoint.getY());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// Override default Java implementation just to have a nicer
		// representation
		return String.format("Bug @ location ", grid.getLocation(this));
	}

}
