package demo.prisoners;

import uchicago.src.sim.util.Random;

/** Agent class, represents a player with null or one-step memory in Iterated Prisoner's Dilemma game. */
public class PrisonersAgent {

	/** Strategy: Random */
	public static final int RND = 0;
	/** Strategy: Allways defect */
	public static final int ALLD = 1;
	/** Strategy: Allways cooperate */
	public static final int ALLC = 2;
	/** Strategy: Tit-for-tat */
	public static final int TFT = 3;
	/** Strategy: Anti Tit-for-tat */
	public static final int ATFT = 4;

	/** The players current strategy. */
	protected int strategy;
	/** The last step of the enemy. */
	protected boolean enemyLast;
	
	public PrisonersAgent(int strategy) {
		Random.createUniform();
		this.strategy=strategy;
		enemyLast=true;
	}
	
	/** Notes the last step of the enemy. */
	public void setEnemyLast(boolean b) {
		enemyLast = b;
	}
	
	/** Returns true, if cooperates. */
	public boolean cooperate() {
		switch (strategy) {
			case TFT:	return enemyLast;
			case ATFT:	return !enemyLast;
			case ALLD:	return false;
			case ALLC:	return true;
			case RND:	return //Random.uniform.nextBoolean();
			uchicago.src.sim.util.Random.uniform.nextBoolean();
		}
		return true;
	}
}
