package ai.demonstration;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import ai.demonstration.TGraph.Location;

public class Node {

	private Location id;
	private Location successor = null;
	private Location previous = null;
	protected TNodeState nodeState;
	private Set<Location> neighbors = new HashSet<>();
	
	public Node(Location id, TNodeState nodeState, Node previousNode) {
		this.id = id;
		
		if (previousNode != null) {
			this.previous = previousNode.getId();
		}
		this.nodeState = nodeState;
		
		for (Location loc : id.genNeighbors()) {
			addNeighbor(loc);
		}
	}
	
	public int getX() {
		return id.m_a;
	}
	
	public int getY() {
		return id.m_b;
	}

	public void setPrevious(Node previous) {
		this.previous = previous.getId();
		addNeighbor(previous);
	}

	public void setSuccessor(Node successor) {
		this.successor = successor.getId();
		addNeighbor(successor);
	}
	
	public Location getPrevious() {
		return previous;
	}
	
	public Location getSuccessor() {
		return successor;
	}
	
	public Location getId() { return id; }

	public double maxDeltaHeuristic(Node nodeB) {
		return nodeState.maxDeltaHeuristic(nodeB.getNodeState());
	}
	
	public double euclideanDistance(Node nodeB) {
		return Math.sqrt(Math.pow(getX() - nodeB.getX(), 2) + Math.pow(getY() - nodeB.getY(), 2));
	}
	
	public double manhattanDistance(Node nodeB) {
		return Math.abs(getX() - nodeB.getX()) + Math.abs(getY() - nodeB.getY());
	}
	
	public double distance(Node nodeB) {
		return euclideanDistance(nodeB);
	}

	public TNodeState getNodeState() {
		return nodeState;
	}
	
	public Set<Location> getNeighbors() {
		return neighbors;
	}
	
	public void addNeighbor(Node neighborNode) {
		addNeighbor(neighborNode.getId());
	}
	
	//Adds neighbor if it isn't there already, duplicates not added
	public void addNeighbor(Location neighbor) {
		if (nodeState.getPGS().isValidSpace(neighbor.m_a, neighbor.m_b) &&
				(neighbor.m_a != getX() || neighbor.m_b != getY())) {
			neighbors.add(neighbor);
		}
	}
	
	
	public void addNeighbor(int x, int y) {
		addNeighbor(new Location(x, y));
	}
	
	@Override
	public String toString() {
		return id.toString();
	}

}
