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
import rts.Player;
import rts.PlayerAction;
import rts.Trace;
import rts.TraceEntry;
import util.Pair;

public class TGraph {

	private int playerId;
	private int opponentId;
	
	// Heuristic inflation factor
	private double epsilon;

	private Set<Long> tnodes = new HashSet<>();
	private Map<Long, Node> nodes = new HashMap<>();
	private Set<Long> terminalTNodes = new HashSet<>();
	private Set<Long> initTNodes = new HashSet<>();
	private Node currentStateNode;
	
	// The states that are in the current game (not demonstration data)
	private Set<Long> searchNodes = new HashSet<>();
	
	private List<Long> traceNodeOrder = new ArrayList<>();
	
	private IdGenerator idGen = new IdGenerator();

	private Map<NodePair, Double> heuristicT = new HashMap<>();
	private Map<Pair<NodePair, NodePair>, Double> estHeuristicT = new HashMap<>();
	
	private class NodePair extends Pair<Long, Long> {
		public NodePair(Long a, Long b) { super(a, b); }
		
	    @Override
	    public boolean equals(Object obj) {
	    	if (obj == null) return false;
	    	if (!(obj instanceof NodePair)) return false;
	    	NodePair otherPair = (NodePair) obj;
	    	return Objects.equals(m_a, otherPair.m_a) &&
	    		Objects.equals(m_b, otherPair.m_b);
	    }
	}

	public TGraph(int playerId, Trace trace, double epsilon) {
		this.epsilon = epsilon;
		this.playerId = playerId;
		List<Player> otherPlayers = trace.getEntries().get(0).getPhysicalGameState().getPlayers().stream().filter(p -> p.getID() != playerId).collect(Collectors.toList());
		if (otherPlayers.size() > 1) throw new RuntimeException("Too many opponents found: " + otherPlayers.size());
		this.opponentId = otherPlayers.get(0).getID();
		
		Node prevNode = null;
		for (TraceEntry traceEntry : trace.getEntries()) {
			// Adding all trace entries for now
			// TODO only add those with new actions? How will that affect cost?
			Node newNode = new TNode(idGen.nextId(), new TNodeStateTraceEntry(traceEntry, trace.getUnitTypeTable(), playerId, opponentId), prevNode);
			this.addTNode(newNode);
			if (prevNode != null) {
				NodePair edge = new NodePair(prevNode.getId(), newNode.getId());
				prevNode.addSuccessor(newNode);
			}
			prevNode = newNode;
		}
		
		initTNodes.add(traceNodeOrder.get(0));
		terminalTNodes.add(traceNodeOrder.get(traceNodeOrder.size() - 1));
		System.out.println("Finished TGraph constructor");
	}
	
	public PlayerAction getAction(int player, GameState currentState) {
		System.out.println("Entering getAction");
		Map<Long, PlayerAction> nodeIdToPrevAction = createSearchNodes(player, currentState);
		
		Map<Long, Double> nodeIdToHeuristicT = new HashMap<>();
		
		for (Long goalNodeId : searchNodes) {
			System.out.println("Looking at next neighbor node");
			Node goalNode = nodes.get(goalNodeId);
			
			// TODO add connections from T terminals to search graph
			// TODO maybe add connections from all T nodes to search graph
			
			calcHeuristicTForTerminalTNodesToGoal(goalNode);
			System.out.println("Finished h_T for term tnodes to goal");
			calcHeuristicTForNonTerminalTNodesToGoal(goalNode);
			System.out.println("Finished nonterminal tnodes to goal");
			calcEstHeuristicTForTNodesToGoal(currentStateNode, goalNode);
			System.out.println("Finished est h_T for tnodes to goal");
			calcEstHeuristicTForTerminalTNodesToGoal(goalNode);
			System.out.println("Finished est h_T for terminal tnodes to goal");
			calcEstHeuristicTForStartToInitTNodes(goalNode);
			System.out.println("Done with main heuristics calculations");
			
			List<Long> startingNodes = new ArrayList<Long>(nodes.keySet());
			startingNodes.add(currentStateNode.getId());
			
			List<Long> endingNodes = new ArrayList<Long>(nodes.keySet());
			endingNodes.add(goalNode.getId());
			
			NodePair key = new NodePair(currentStateNode.getId(), goalNodeId);
			double minCost = epsilon * currentStateNode.getNodeState().getVector().getDistance(goalNode.getNodeState().getVector());
			for (Long startId : startingNodes) {
				for (Long endId : endingNodes) {
					Pair<NodePair, NodePair> estKey = new Pair<>(
						new NodePair(currentStateNode.getId(), goalNodeId),
						new NodePair(startId, endId)
					);
					minCost = Math.min(minCost, estHeuristicT.get(estKey));
				}
			}
			nodeIdToHeuristicT.put(goalNodeId, minCost);
		}
		Long nextNode = null;
		Double minCost = Double.POSITIVE_INFINITY;
		for (Long neighborNodeId : searchNodes) {
			if (minCost > nodeIdToHeuristicT.get(neighborNodeId)) {
				minCost = nodeIdToHeuristicT.get(neighborNodeId);
				nextNode = neighborNodeId;
			}
		}
		System.out.println("Exiting getAction");
		return nodeIdToPrevAction.get(nextNode);
	}
	
	private Map<Long, PlayerAction> createSearchNodes(int playerId, GameState currentState) {
		Map<Long, PlayerAction> nodeIdToPlayerAction = new HashMap<>();
		currentStateNode = new SearchNode(playerId, new TNodeStateGameState(currentState, playerId, opponentId), null);
				
		for (PlayerAction playerAction: currentState.getPlayerActions(playerId)) {
			GameState tmpGS = currentState.clone();
				
			boolean didSomething = tmpGS.issue(playerAction);
			
			if (didSomething) {
				Node newNode = new SearchNode(idGen.nextId(), new TNodeStateGameState(tmpGS, playerId, opponentId), currentStateNode);
				this.addSearchNode(newNode);
				NodePair edge = new NodePair(currentStateNode.getId(), newNode.getId());
				nodeIdToPlayerAction.put(newNode.getId(), playerAction);
			}
		}
		return nodeIdToPlayerAction;
	}
	
	private double calcEstHeuristicT(Node aNode, Node bNode, Node sNode, Node sPrimeNode) {

		double pi = (Math.pow(aNode.euclideanDistance(sPrimeNode), 2) 
				- Math.pow(sNode.euclideanDistance(aNode), 2) 
				+ Math.pow(sNode.euclideanDistance(sPrimeNode),  2)) / 2 * sNode.euclideanDistance(sPrimeNode);

		double alpha = pi / sNode.euclideanDistance(sPrimeNode);
		double ro = Math.sqrt(Math.pow(sNode.euclideanDistance(aNode), 2) - Math.pow(sNode.euclideanDistance(sPrimeNode) - pi, 2));
		
		if (alpha < 0) {
			return epsilon * aNode.euclideanDistance(sPrimeNode)
				+ heuristicT.get(new NodePair(sPrimeNode.getId(), bNode.getId()));
		} else if (alpha > 1) {
			return epsilon * sNode.euclideanDistance(aNode)
				+ heuristicT.get(new NodePair(sNode.getId(), bNode.getId()));
		} else {
			return epsilon * ro + alpha * calcTransitionCost(sNode, sPrimeNode)
				+ heuristicT.get(new NodePair(sPrimeNode.getId(), bNode.getId()));
		}
	}
	
	private void calcEstHeuristicTForTerminalTNodesToGoal(Node goalNode) {
		for (Long termNodeId : terminalTNodes) {
			
			//TODO pass in heuristicP instead of the default euclidean distance
			calcSaveEstHeuristicT(currentStateNode, goalNode, nodes.get(termNodeId), goalNode);
		}
	}
	
	private void calcEstHeuristicTForStartToInitTNodes(Node goalNode) {
		for (Long initTNodeId : initTNodes) {
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
		for (Long termNodeId : terminalTNodes) {
			Node sPrimeNode = nodes.get(termNodeId);
			while (sPrimeNode.getPrevious() != null) {
				Node sNode = nodes.get(sPrimeNode.getPrevious());
				calcSaveEstHeuristicT(aNode, bNode, sNode, sPrimeNode);
			}
		}
	}
	
	
	//TODO is this valid? Can we just use the time like this or do we need to run a simulation?
	private double calcTransitionCost(Node a, Node b) {
		return b.getNodeState().getTime() - b.getNodeState().getTime();
	}
	
	
	private void calcHeuristicTForNonTerminalTNodesToGoal(Node goalNode) {
		for (Long termTNodeId : terminalTNodes) {
			Node currentNode = nodes.get(termTNodeId);
			while (currentNode.getPrevious() != null) {
				NodePair edge = new NodePair(currentNode.getPrevious(), goalNode.getId());
				
				double val = heuristicT.get(currentNode) + calcTransitionCost(nodes.get(currentNode.getPrevious()), goalNode);
				if (heuristicT.get(edge) > val) {
					heuristicT.put(edge, val);
				}
			}
		}
	}

	private void calcHeuristicTForTerminalTNodesToGoal(Node goalNode) {
		for (Long termTNodeId : terminalTNodes) {
			Node termNode = nodes.get(termTNodeId);
			NodePair edge = new NodePair(termNode.getId(), goalNode.getId());
			//TODO can't calc heuristicP because termNode has no successors. Do we need to add some?
			heuristicT.put(edge, calcHeuristicP(termNode, goalNode));
		}
	}

	// h_P is the shortest path on the graph
	private double calcHeuristicP(Node nodeA, Node nodeB) {
		return dijkstras(nodeA, nodeB, (a, b) -> a.maxDeltaHeuristic(b));
	}
	
	private Double dijkstras(Node nodeA, Node nodeB, EdgeCostCalculator edgeCalculator) {
		System.out.println("Starting dijkstra's");
		Set<Long> vertices = new HashSet<>(nodes.keySet());
		Map<Long, Double> distanceFromATo = new HashMap<>(nodes.size());
		
		for (Long l : vertices) {
			distanceFromATo.put(l, Double.POSITIVE_INFINITY);
		}
		
		distanceFromATo.put(nodeA.getId(), 0.0);
		
		while (!vertices.isEmpty()) {
			System.out.println("Looking at another vertex");
			Long currentNodeId = -1L;
			Double currentDist = null;
			for (Long nodeId: distanceFromATo.keySet()) {
				if (currentDist == null || distanceFromATo.get(nodeId) < currentDist) {
					currentNodeId = nodeId;
					currentDist = distanceFromATo.get(currentNodeId);
				}
			}
			
			if (currentNodeId == nodeB.getId()) {
				return distanceFromATo.get(nodeB.getId());
			}
			
			vertices.remove(currentNodeId);
			
			for (Long neighborId : nodes.get(currentNodeId).getSuccessors()) {
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
	
	private long addSearchNode(Node node) {
		nodes.put(node.getId(), node);
		searchNodes.add(node.getId());
		return node.getId();
	}

	private long addTNode(Node tnode) {
		nodes.put(tnode.getId(), tnode);
		tnodes.add(tnode.getId());
		traceNodeOrder.add(tnode.getId());
		return tnode.getId();
	}
	
	private static class IdGenerator {
		private long nextId = 0;
		public long nextId() { return nextId++; }		
	}
}
