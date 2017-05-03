package ai.demonstration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.Trace;
import rts.TraceEntry;
import rts.UnitAction;
import rts.units.Unit;
import rts.units.UnitType;
import util.Pair;

public class TGraph {
	
	public static double DEFAULT_EPSILON = 30.0;
	private int playerId;
	
	//Unit whose trace will be used
	private long unitId;
	private UnitType unitType;
	private int opponentId;
	
	// Heuristic inflation factor
	private double epsilon;

	private Set<Location> tnodes = new HashSet<>();
	private Map<Location, Node> nodes = new HashMap<>();
	private Set<Location> terminalTNodes = new HashSet<>();
	private Set<Location> initTNodes = new HashSet<>();
	
	// The states that are in the current game (not demonstration data)
	private Set<Location> searchNodes = new HashSet<>();
	
	private List<Location> traceNodeOrder = new ArrayList<>();
	
	//private IdGenerator idGen = new IdGenerator();

	private Map<NodePair, Double> heuristicT = new HashMap<>();
	private Map<Pair<NodePair, NodePair>, Double> estHeuristicT = new HashMap<>();
	
	private class NodePair extends Pair<Location, Location> {
		public NodePair(Location a, Location b) { super(a, b); }
		
	    @Override
	    public boolean equals(Object obj) {
	    	if (obj == null) return false;
	    	if (!(obj instanceof NodePair)) return false;
	    	NodePair otherPair = (NodePair) obj;
	    	return Objects.equals(m_a, otherPair.m_a) &&
	    		Objects.equals(m_b, otherPair.m_b);
	    }
	}
	
	public static class Location {
		public Integer m_a, m_b;
		public Location(int x, int y) { m_a = x; m_b = y; }
		public Location(Unit u) { this(u.getX(), u.getY()); }
		public Location(int pos, int width, int height) { this(pos % width, pos / width); }
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null) return false;
			if (!(obj instanceof Location)) return false;
			Location otherLoc = (Location) obj;
			return Objects.equals(m_a,  otherLoc.m_a) &&
					Objects.equals(m_b,  otherLoc.m_b);
		}
		
		public Set<Location> genNeighbors() {
			Set<Location> neighbors = new HashSet<>();
			neighbors.add(new Location(m_a, m_b - 1));
			neighbors.add(new Location(m_a, m_b + 1));
			neighbors.add(new Location(m_a - 1, m_b));
			neighbors.add(new Location(m_a + 1, m_b));
			return neighbors;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(m_a, m_b);
		}
		
		@Override
		public String toString() {
			return "<" + m_a + "," + m_b + ">";
		}
	}

	public TGraph(int playerId, Long unitId, Trace trace, double epsilon) {
		this.epsilon = epsilon;
		this.playerId = playerId;
		List<Player> otherPlayers = trace.getEntries().get(0).getPhysicalGameState().getPlayers().stream().filter(p -> p.getID() != playerId).collect(Collectors.toList());
		if (otherPlayers.size() > 1) throw new RuntimeException("Too many opponents found: " + otherPlayers.size());
		this.opponentId = otherPlayers.get(0).getID();

		Location prevLoc = null;
		Node prevNode = null;
		for (TraceEntry traceEntry : trace.getEntries()) {
			Unit tracedUnit = traceEntry.getPhysicalGameState().getUnit(unitId);
			unitType = tracedUnit.getType();
			Location newLoc = new Location(tracedUnit);
			if (!newLoc.equals(prevLoc)) {
				Node nextNode = null;
				if (!nodes.containsKey(newLoc)) {
					nextNode = new Node(newLoc, new TNodeStateTraceEntry(traceEntry, trace.getUnitTypeTable(), playerId, opponentId), prevNode);
				} else {
					nextNode = nodes.get(newLoc);
				}
				this.addTNode(nextNode);
				if (prevNode != null) {
					//TODO fix this to be able to handle traces with loops. currently, setting the previous upon the 2nd encounter of the node
					//creates an infinite loop
					prevNode.setSuccessor(nextNode);
					nextNode.setPrevious(prevNode);
				}
				prevNode = nextNode;
			}
			prevLoc = newLoc;
		}
		
		initTNodes.add(traceNodeOrder.get(0));
		terminalTNodes.add(traceNodeOrder.get(traceNodeOrder.size() - 1));
		//System.out.println("Finished TGraph constructor");
	}
	
	private void createSearchNodes(int playerId, GameState currentState) {
		PhysicalGameState pgs = currentState.getPhysicalGameState();
		
		for (int x = 0; x < currentState.getPhysicalGameState().getWidth(); x++) {
			for (int y = 0; y < currentState.getPhysicalGameState().getHeight(); y++) {
				if (pgs.getTerrain(x, y) == 0) {
					Location loc = new Location(x, y);
					if (!nodes.containsKey(loc)) {
						this.addSearchNode(new Node(loc, new TNodeStateGameState(currentState, playerId, opponentId), null));
					}
				}
			}
		}
	}
	
	private double calcEstHeuristicT(Node aNode, Node bNode, Node sNode, Node sPrimeNode) {

		//pi can't be NaN
		double pi = sNode.distance(sPrimeNode) == 0 ? 0 : (Math.pow(aNode.distance(sPrimeNode), 2) 
				- Math.pow(sNode.distance(aNode), 2) 
				+ Math.pow(sNode.distance(sPrimeNode),  2)) / (2 * sNode.distance(sPrimeNode));

		double alpha = sNode.distance(sPrimeNode) == 0 ? 1 : pi / sNode.distance(sPrimeNode);
		double ro = Math.sqrt(Math.abs(Math.pow(sNode.distance(aNode), 2) - Math.pow(sNode.distance(sPrimeNode) - pi, 2)));
	
		double e = 0.00001;
		
		if (alpha < e) {
			return epsilon * aNode.distance(sPrimeNode)
				+ heuristicT.get(new NodePair(sPrimeNode.getId(), bNode.getId()));
		} else if (alpha > 1.0 + e) {
			return epsilon * sNode.distance(aNode)
				+ heuristicT.get(new NodePair(sNode.getId(), bNode.getId()));
		} else {
			return epsilon * ro + alpha * calcTransitionCost(sNode, sPrimeNode)
				+ heuristicT.get(new NodePair(sPrimeNode.getId(), bNode.getId()));
		}
	}
	
	private void calcEstHeuristicTForTerminalTNodesToGoal(Node currentStateNode, Node goalNode) {
		for (Location termNodeId : terminalTNodes) {
			
			calcSaveEstHeuristicT(currentStateNode, goalNode, nodes.get(termNodeId), goalNode);
		}
	}
	
	private void calcEstHeuristicTForStartToInitTNodes(Node currentStateNode, Node goalNode) {
		for (Location initTNodeId : initTNodes) {
			calcSaveEstHeuristicT(currentStateNode, goalNode, currentStateNode, nodes.get(initTNodeId));
		}
	}
	
	private void calcSaveEstHeuristicT(Node a, Node b, Node s, Node sPrime) {
		
		//Cost a to b, using s & s' to inform 
		Pair<NodePair, NodePair> key = new Pair<NodePair, NodePair>(
			new NodePair(a.getId(), b.getId()), 
			new NodePair(s.getId(), sPrime.getId())
		);
		
		estHeuristicT.put(key, calcEstHeuristicT(a, b, s, sPrime));
	}
	
	private void calcEstHeuristicTForTNodesToGoal(Node aNode, Node bNode) {
		for (Location nodeId : tnodes) {
			Node sNode = nodes.get(nodeId);
			if (sNode.getSuccessor() != null) {
				Node sPrimeNode = nodes.get(sNode.getSuccessor());
				calcSaveEstHeuristicT(aNode, bNode, sNode, sPrimeNode);
			}
		}
	}
	
	

	private double calcTransitionCost(Node a, Node b) {
		return manhattanDistance(a, b) * (double) unitType.moveTime;
	}
	
	
	private void calcHeuristicTForNonTerminalTNodesToGoal(Node goalNode) {
		for (Location termTNodeId : terminalTNodes) {
			Node currentNode = nodes.get(termTNodeId);
			while (currentNode.getPrevious() != null) {
				NodePair edge = new NodePair(currentNode.getPrevious(), goalNode.getId());
				
				double val = heuristicT.get(new NodePair(currentNode.getId(), goalNode.getId())) + calcTransitionCost(nodes.get(currentNode.getPrevious()), currentNode);
				heuristicT.put(edge, val);
				currentNode = nodes.get(currentNode.getPrevious());
			}
		}
	}

	private void calcHeuristicTForTerminalTNodesToGoal(Node goalNode) {
		for (Location termTNodeId : terminalTNodes) {
			Node termNode = nodes.get(termTNodeId);
			NodePair edge = new NodePair(termNode.getId(), goalNode.getId());
			heuristicT.put(edge, calcHeuristicP(termNode, goalNode));
		}
	}

	// h_P is the shortest path on the graph
	private double calcHeuristicP(Node nodeA, Node nodeB) {
		return ((double) dijkstras(nodeA, nodeB, (a, b) -> a.distance(b)) / ( 1.0 / ((double) unitType.moveTime)));
	}
	
	private static double manhattanDistance(Node a, Node b) {
		return manhattanDistance(a.getX(), a.getY(), b.getX(), b.getY());
	}
	
	private static double manhattanDistance(int x1, int y1, int x2, int y2) {
		return Math.abs(x2 - x1) + Math.abs(y2 - y1); 
	}
	
	private Double dijkstras(Node nodeA, Node nodeB, EdgeCostCalculator edgeCalculator) {
		//System.out.println("Starting dijkstra's");
		Set<Location> vertices = new HashSet<>(nodes.keySet());
		Map<Location, Double> distanceFromATo = new HashMap<>(nodes.size());
		
		for (Location l : vertices) {
			distanceFromATo.put(l, Double.POSITIVE_INFINITY);
		}
		
		distanceFromATo.put(nodeA.getId(), 0.0);
		
		while (!vertices.isEmpty()) {
			Location currentNodeId = null;
			Double currentDist = null;
			for (Location nodeId: vertices) {
				if (currentDist == null || distanceFromATo.get(nodeId) < currentDist) {
					currentNodeId = nodeId;
					currentDist = distanceFromATo.get(currentNodeId);
				}
			}
			
			if (currentNodeId == nodeB.getId()) {
				return distanceFromATo.get(nodeB.getId());
			}
			
			vertices.remove(currentNodeId);
			
			for (Location neighborId : nodes.get(currentNodeId).getNeighbors()) {
				Double newDist = distanceFromATo.get(currentNodeId) + edgeCalculator.calcEdgeCost(nodes.get(currentNodeId), nodes.get(neighborId));
				if (newDist < distanceFromATo.get(neighborId)){
					distanceFromATo.put(neighborId, newDist);
				}
			}
			
		}
		throw new RuntimeException("Node b not found");
	}
	
	private interface EdgeCostCalculator {
		public Double calcEdgeCost(Node a, Node b);
	}
	
	private void addSearchNode(Node node) {
		if (!nodes.containsKey(node.getId())) {
			nodes.put(node.getId(), node);
		}
		searchNodes.add(node.getId());
	}

	private void addTNode(Node tnode) {
		if (!nodes.containsKey(tnode.getId())) {
			nodes.put(tnode.getId(), tnode);
			tnodes.add(tnode.getId());
		}
		traceNodeOrder.add(tnode.getId());
	}
	
	public double getHeuristicT(Location startLoc, Location goalLoc, GameState gs) {
		createSearchNodes(playerId, gs);
		
		Node goalNode = nodes.get(goalLoc);
		
		Node currentStateNode = nodes.get(startLoc);
		
		calcHeuristicTForTNodesToSelf(startLoc, goalLoc);
		calcHeuristicTForTerminalTNodesToGoal(goalNode);
		//System.out.println("Finished h_T for term tnodes to goal.");
		calcHeuristicTForNonTerminalTNodesToGoal(goalNode);
		//System.out.println("Finished nonterminal tnodes to goal");
		calcEstHeuristicTForTNodesToGoal(currentStateNode, goalNode);
		//System.out.println("Finished est h_T for tnodes to goal");
		calcEstHeuristicTForTerminalTNodesToGoal(currentStateNode, goalNode);
		//System.out.println("Finished est h_T for terminal tnodes to goal");
		calcEstHeuristicTForStartToInitTNodes(currentStateNode, goalNode);
		//System.out.println("Done with main heuristics calculations");
		
		List<Location> startingNodes = new ArrayList<Location>(tnodes);
		startingNodes.add(currentStateNode.getId());
		
		List<Location> endingNodes = new ArrayList<Location>(tnodes);
		endingNodes.add(goalNode.getId());
		
		double minCost = epsilon * currentStateNode.distance(goalNode);
		for (Location startId : startingNodes) {
			for (Location endId : endingNodes) {
				if (!startId.equals(endId)) {
					Pair<NodePair, NodePair> estKey = new Pair<>(
						new NodePair(currentStateNode.getId(), goalNode.getId()),
						new NodePair(startId, endId)
					);
					if (estHeuristicT.get(estKey) != null) {
						minCost = Math.min(minCost, estHeuristicT.get(estKey));
					}
				}
			}
		}

		heuristicT.put(new NodePair(currentStateNode.getId(), goalNode.getId()), minCost);
		
		return minCost;
		
	}
	
	private void calcHeuristicTForTNodesToSelf(Location startLoc, Location goalLoc) {
		for (Location nodeLoc : tnodes) {
			heuristicT.put(new NodePair(nodeLoc, nodeLoc), 0.0);
		}
		heuristicT.put(new NodePair(goalLoc, goalLoc), 0.0);
		heuristicT.put(new NodePair(startLoc, startLoc), 0.0);
	}
	
	public static int diffDirection(Location a, Location b) {
		int diffX = b.m_a - a.m_a;
		int diffY = b.m_b - a.m_b;
		
		if (diffX == 0) {
			if (diffY == 0) {
				return UnitAction.DIRECTION_NONE;
			} else if (diffY == 1) {
				return UnitAction.DIRECTION_DOWN;
			} else if (diffY == -1) {
				return UnitAction.DIRECTION_UP;
			} else {
				throw new RuntimeException();
			}
		} else if (diffX == 1) {
			if (diffY == 0) {
				return UnitAction.DIRECTION_RIGHT;
			} else {
				throw new RuntimeException();
			}
		} else if (diffX == -1) {
			if (diffY == 0) {
				return UnitAction.DIRECTION_LEFT;
			} else {
				throw new RuntimeException();
			}
		} else {
			throw new RuntimeException();
		}
	}

	//Since we are using t-graphs regardless of target, this is always true
	public boolean relevantFor(Unit start, int targetpos, GameState gs) {
		return true;
		/*
		Location targetLoc = new Location(targetpos, gs.getPhysicalGameState().getWidth(), gs.getPhysicalGameState().getHeight());
		if (terminalTNodes.size() > 1) throw new RuntimeException();
		for (Location terminalNode : terminalTNodes) {
			return manhattanDistance(targetLoc.m_a, targetLoc.m_b, terminalNode.m_a, terminalNode.m_b) <= 6;
		}
		return false;
		*/
	}

}
