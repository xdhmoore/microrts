package ai.demonstration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PlayerAction;
import rts.Trace;

public class TrainingGraphAI extends AbstractionLayerAI {

	private int tracePID;
	
	// Maps cycle time to trace
	private Map<Integer, Trace> traceMap = new HashMap<>();
	private TGraph tgraph;
	
	public TrainingGraphAI(int tracePID, Trace trace, Double epsilon, PathFinding pf) {
		super(pf);
		this.tracePID = tracePID;
		//tgraph = new TGraph(trace, epsilon);
	}
	
	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		
		return tgraph.getAction(player, gs);
		// TODO Auto-generated method stub
		
		// TODO I know you're not supposed to do this, but...
		// maybe there's a way to make it faster...?
		
		// TODO Is this really the best way to sync current state with trace? 
		// How do we really want to do this? Actually, I think this should be taken care of
		// by the T-graph algorithm
		// TODO maybe pre-load this into a cycle->state map or tree or something?
		//TraceEntry entry = this.trace.getTraceEntryAtCycle(gs.getTime());

		//trace.get
		
		
		// TODO How do I transfer actions over when there are no ids? A static mapping? Maybe an algorithm that grabs the nearest item?
		// could store in some sort of oct-tree-thingy to make faster, maybe.
		
		
		// Get appropriate trace entry
		
		// Get appropriate PlayerAction from TraceEntry
		
		// Map unit actions to new unit actions in this world, using traceEntry's gamestate to inform mapping
		
		// Create new player action from unit actions

	}

	@Override
	public AI clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		// TODO trace file
		// TODO epsilon
		// TODO Auto-generated method stub
		return null;
	}

}
