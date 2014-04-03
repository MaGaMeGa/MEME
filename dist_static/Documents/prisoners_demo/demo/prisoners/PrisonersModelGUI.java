package demo.prisoners;

import uchicago.src.sim.analysis.*;

/** Iterated Prisoner's Dilemma game. (gui mode)*/
public class PrisonersModelGUI extends PrisonersModel {

	/** A graph, drawn in GUI mode. */
	protected OpenSequenceGraph graph;
	
	class Payoff implements Sequence {
		private int player;
		public Payoff(int player) { this.player = player; }
		public double getSValue() {
			return (double)(player==1?payoff1:payoff2);
		}
	}
	
	public void setup() {
		super.setup();
		winner=4;
		looser=-3;
		both=1;
		neither=0;
		strat1 = PrisonersAgent.RND;
		strat2 = PrisonersAgent.RND;
	}
	
	public void buildModel() {
		super.buildModel();
		if (graph!=null) graph.dispose();
		graph=new OpenSequenceGraph("Payoff",this);
		graph.setXRange(0, 50);
		graph.setYRange(-30, 170);
		graph.setXViewPolicy(OpenSequenceGraph.SHOW_LAST);
		graph.addSequence("1st player", new Payoff(1));
		graph.addSequence("2nd player", new Payoff(2));
		graph.display();
	}
	
	public void step() {
		super.step();
		graph.step();
	}
}
