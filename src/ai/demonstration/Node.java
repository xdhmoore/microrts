package ai.demonstration;

import java.util.HashSet;
import java.util.Set;

import rts.GameState;

public abstract class Node {

	private long id;
	private Set<Long> successors = new HashSet<>();
	private Long previous = null;
	private TNodeState nodeState;
	
	public Node(long id, TNodeState nodeState, Node previousNode) {
		this.id = id;
		
		if (previousNode != null) {
			this.previous = previousNode.getId();
		}
		this.nodeState = nodeState;
	}

	public void setPrevious(Node previous) {
		this.previous = previous.getId();
	}

	public void addSuccessor(Node successor) {
		successors.add(successor.getId());
	}
	
	public Long getPrevious() {
		return previous;
	}
	
	public Set<Long> getSuccessors() {
		return successors;
	}
	
	public long getId() { return id; }

	public double maxDeltaHeuristic(Node nodeB) {
		return nodeState.maxDeltaHeuristic(nodeB.getNodeState());
	}
	
	public double euclideanDistance(Node nodeB) {
		return nodeState.getVector().getDistance(nodeB.getNodeState().getVector());
	}

	public TNodeState getNodeState() {
		return nodeState;
	}
	
	// TODO - Potential "features"
	// order of my units from closest to opponent's center of mass to furthest
	// order of opponent's units from my center of mass to furthest
	// center of mass of each type of unit for me (or even just buildings, workers, and attack units)
	// center of mass of each type of unit for opponent (or even ")
	// time since last attack
	// location of last attack
	// time since my last attack on them
	// location of my last attack on them
}
