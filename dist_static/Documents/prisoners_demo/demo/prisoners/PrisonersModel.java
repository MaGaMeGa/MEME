package demo.prisoners;

import uchicago.src.sim.engine.*;

/**
 * Iterated Prisoner's Dilemma game model.
 */
public class PrisonersModel extends SimpleModel {

	public PrisonersModel() {
		super();
		name = "Prisoner's Dilemma";
	}
		
	/** The match-winner's payoff. */
	protected int winner;
	/** Gets winner's payoff. */
	public int getWinner() { return winner; }
	/** Sets winner's payoff. */
	public void setWinner(int winner) { this.winner=winner; }
	/** The payoff of the looser. */
	protected int looser;
	/** Gets looser's payoff. */
	public int getLooser() { return looser; }
	/** Sets looser's payoff. */
	public void setLooser(int looser) { this.looser=looser; }
	/** payoff of both players', if they're cooperate. */
	protected int both;
	/** Gets payoff, if both cooperate. */
	public int getBoth() { return both; }
	/** Sets payoff, if both cooperate. */
	public void setBoth(int both) { this.both=both; }
	/** payoff of players', if neither cooperate. */
	protected int neither;
	/** Gets payoff, if neither cooperate. */
	public int getNeither() { return neither; }
	/** Sets payoff, if neither cooperate. */
	public void setNeither(int neither) { this.neither=neither; }
	/** Strategy of the 1st player. */
	protected int strat1;
	/** Sets the strategy of the 1st player. */
	public int getStrat1() { return strat1; }
	/** Gets the strategy of the 1st player. */
	public void setStrat1(int strat1) { this.strat1=strat1; }
	/** Strategy of the 2nd player. */
	protected int strat2;
	/** Sets the strategy of the 2nd player. */
	public int getStrat2() { return strat2; }
	/** Gets the strategy of the 2nd player. */
	public void setStrat2(int strat2) { this.strat2=strat2; }
	
	/** Player 1's payoff.*/
	protected int payoff1;
	public void setPayoff1(int i) { payoff1 = i; }
	public int getPayoff1() { return payoff1; }
	/** Player 2's payoff. */
	protected int payoff2;
	public void setPayoff2(int i) { payoff2 = i; }
	public int getPayoff2() { return payoff2; }
	
	public String[] getInitParam() {
		String[] params = {"winner","looser","both","neither",
		    			   "strat1","strat2"};
	    return params;
	}

	
	public void setup() {
		super.setup();
		generateNewSeed();
		payoff1 = 0;
		payoff2 = 0;
	}

	@SuppressWarnings("unchecked")
	public void buildModel() {
		PrisonersAgent a = new PrisonersAgent(strat1);
		agentList.add(a);
		PrisonersAgent b = new PrisonersAgent(strat2);
		agentList.add(b);
	}

	public void step() {
		PrisonersAgent a = (PrisonersAgent)agentList.get(0);
		PrisonersAgent b = (PrisonersAgent)agentList.get(1);
		boolean cA = a.cooperate();
		boolean cB = b.cooperate();
		if (cA && cB) {
			payoff1+=both;
			payoff2+=both;
		}
		if (cA && !cB) {
			payoff1+=looser;
			payoff2+=winner;
		}
		if (!cA && cB) {
			payoff1+=winner;
			payoff2+=looser;
		}
		if (!cA && !cB) {
			payoff1+=neither;
			payoff2+=neither;
		}
		a.setEnemyLast(cB);
		b.setEnemyLast(cA);
	}
	
	public void atEnd() {
		super.atEnd();
	}
	
}
