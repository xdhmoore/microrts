package ai.abstraction.pathfinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import ai.demonstration.TGraph;
import ai.demonstration.TGraph.Location;
import rts.GameState;
import rts.ResourceUsage;
import rts.Trace;
import rts.UnitAction;
import rts.units.Unit;
import util.Pair;

public class TGraphPathFinding extends PathFinding {

	private PathFinding pf = new AStarPathFinding();
	private Element traceXml;
	private int playerId;
	private Long guidedUnitId = null;
	private long tracedUnitId;
	
	private List<Location> path = new ArrayList<>();
	private Map<Location, List<Location>> endLocToPath = new HashMap<>();
	
	private double epsilon;
	
	public TGraphPathFinding(double epsilon, String file) {
		this(0, 22, file, epsilon);
	}
	
	//modified: iiii
	//top path: 16.1
	//bottom path: 16.05
	
	//original:
	//top path: 17.028
	//bottom path: 17.0275
	
	public TGraphPathFinding(int playerId, long tracedUnitId, String traceFileName, double epsilon) {
		this.playerId = playerId;
		this.epsilon = epsilon;
		this.tracedUnitId = tracedUnitId;
		try {
			this.traceXml = new SAXBuilder().build(traceFileName).getRootElement();
		} catch (JDOMException | IOException e) {
			throw new RuntimeException("Failed to open trace file: '" + traceFileName + "'", e);
		}
	}

	public TGraphPathFinding() {
		this(TGraph.DEFAULT_EPSILON, "./trap_modified_avoid_trace.xml");
	}

	@Override
	public boolean pathExists(Unit start, int targetpos, GameState gs, ResourceUsage ru) {
		return pf.pathExists(start, targetpos, gs, ru);
	}

	@Override
	public boolean pathToPositionInRangeExists(Unit start, int targetpos, int range, GameState gs, ResourceUsage ru) {
		return pf.pathToPositionInRangeExists(start, targetpos, range, gs, ru);
	}

	@Override
	public UnitAction findPath(Unit start, int targetpos, GameState gs, ResourceUsage ru) {
		if (guidedUnitId == null || gs.getUnit(guidedUnitId) == null || gs.getUnit(guidedUnitId).getHitPoints() <= 0) {
			guidedUnitId = null;
		}
		
		if (guidedUnitId == null || start.getID() == guidedUnitId) {
			TGraph tgraph = new TGraph(playerId, tracedUnitId, new Trace(traceXml, gs.getUnitTypeTable()), epsilon);
			//TODO remove "relevantFor" everywhere
			if (tgraph.relevantFor(start, targetpos, gs)) {
				guidedUnitId = start.getID();
				return new UnitAction(UnitAction.TYPE_MOVE, getNextMoveDirection(start, targetpos, gs));
			}
		}
		return pf.findPath(start, targetpos, gs, ru);
	}

	@Override
	public UnitAction findPathToPositionInRange(Unit start, int targetpos, int range, GameState gs, ResourceUsage ru) {
		if (guidedUnitId == null || gs.getUnit(guidedUnitId) == null || gs.getUnit(guidedUnitId).getHitPoints() <= 0) {
			guidedUnitId = null;
		}
		
		if (guidedUnitId == null || start.getID() == guidedUnitId) {
			TGraph tgraph = new TGraph(playerId, tracedUnitId, new Trace(traceXml, gs.getUnitTypeTable()), epsilon);
			if (tgraph.relevantFor(start, targetpos, gs)) {
				guidedUnitId = start.getID();
				return findPath(start, targetpos, gs, ru);
			}
		}
		return pf.findPathToPositionInRange(start, targetpos, range, gs, ru);
	}

	@Override
	public UnitAction findPathToAdjacentPosition(Unit start, int targetpos, GameState gs, ResourceUsage ru) {
		if (guidedUnitId == null || gs.getUnit(guidedUnitId) == null || gs.getUnit(guidedUnitId).getHitPoints() <= 0) {
			guidedUnitId = null;
		}
		
		if (guidedUnitId == null || start.getID() == guidedUnitId) {
			TGraph tgraph = new TGraph(playerId, tracedUnitId, new Trace(traceXml, gs.getUnitTypeTable()), epsilon);
			if (tgraph.relevantFor(start, targetpos, gs)) {
				guidedUnitId = start.getID();
				return findPath(start, targetpos, gs, ru);
			}
		}
		return pf.findPathToAdjacentPosition(start, targetpos, gs, ru);
	}
	
	private int getNextMoveDirection(Unit unit, int targetpos, GameState gs) {
		
		Location startLoc = new Location(unit.getX(), unit.getY());
		Location endLoc = new Location(targetpos, gs.getPhysicalGameState().getWidth(), gs.getPhysicalGameState().getHeight());
		
		if (startLoc.equals(endLoc)) return UnitAction.DIRECTION_NONE;
		
		List<Location> path = endLocToPath.get(endLoc);
		if (path != null && !path.isEmpty() && path.get(path.size() - 1).equals(endLoc)) {
			int currLocIdx = path.indexOf(startLoc);
			if (currLocIdx != -1) {
				return TGraph.diffDirection(startLoc, path.get(currLocIdx + 1));
			}
		}
		
		Map<Location, Location> locToPrevLoc = new HashMap<>();
		Map<Location, Integer> locToCost = new HashMap<>();
		
		PriorityQueue<Pair<Location, Double>> frontier = new PriorityQueue<>(gs.getPhysicalGameState().getHeight() * gs.getPhysicalGameState().getWidth(),
			(locAndCostA, locAndCostB) -> {
				return Double.compare(locAndCostA.m_b, locAndCostB.m_b);
		});
		Set<Location> explored = new HashSet<>();
		
		for (Location neighbor : startLoc.genNeighbors()) {
			if (gs.getPhysicalGameState().isValidSpace(neighbor.m_a, neighbor.m_b)) {
				frontier.add(new Pair<Location, Double>(neighbor, heuristicT(neighbor, endLoc, gs)));
				locToPrevLoc.put(neighbor, startLoc);
				locToCost.put(neighbor, unit.getType().moveTime);
			}
		}
		
		while (!frontier.isEmpty()) {
			Pair<Location, Double> currentLocAndValue = frontier.poll();
			Location currentLoc = currentLocAndValue.m_a;
			if (currentLoc.equals(endLoc)) {
				return getNextMoveFromPath(startLoc, endLoc, locToPrevLoc);
			}
			explored.add(currentLoc);
			for (Location neighbor : currentLoc.genNeighbors()) {
				if (gs.getPhysicalGameState().isValidSpace(neighbor.m_a, neighbor.m_b)) {
					if (!explored.contains(neighbor) && !frontier.contains(new Object() {
						@Override
						public boolean equals(Object obj) {
							@SuppressWarnings("unchecked")
							Pair<Location, Double> pair = (Pair<Location, Double>) obj;
							return neighbor.equals(pair.m_a);
						}
					})) {
						int cost = locToCost.get(currentLoc) + unit.getType().moveTime;
						frontier.add(new Pair<Location, Double>(neighbor, cost + heuristicT(neighbor, endLoc, gs)));
						locToCost.put(neighbor, cost);
						locToPrevLoc.put(neighbor, currentLoc);
					}
				}
			}
			
		}
		
		return UnitAction.DIRECTION_NONE;
	}
	
	private int getNextMoveFromPath(Location startLoc, Location endLoc, Map<Location, Location> locToPrevLoc) {
		List<Location> path = new ArrayList<>();
		Location currentLoc = endLoc;
		path.add(currentLoc);
		while(locToPrevLoc.get(currentLoc) != startLoc) {
			currentLoc = locToPrevLoc.get(currentLoc);
			path.add(currentLoc);
		}
		path.add(startLoc);
		
		Collections.reverse(path);
		
		endLocToPath.put(endLoc, path);
		
		return TGraph.diffDirection(startLoc, currentLoc);
	}
	
	private double heuristicT(Location startLoc, Location endLoc, GameState gs) {
		
		//TODO make this a cache
		// TODO make this so we're not creating new tgraphs all over the place
		TGraph tgraph = new TGraph(playerId, tracedUnitId, new Trace(traceXml, gs.getUnitTypeTable()), epsilon);
		return tgraph.getHeuristicT(startLoc, endLoc, gs);
	}
	
	//Doesn't do c + h search, just greedily looks at h
	private int getNextMoveDirectionGreedy(Unit unit, int targetpos, GameState gs) {

		Location unitLoc = new Location(unit.getX(), unit.getY());
		Location endLoc = new Location(targetpos, gs.getPhysicalGameState().getWidth(), gs.getPhysicalGameState().getHeight());
	
		Location bestLoc = null;
		double minHeuristic = Double.POSITIVE_INFINITY;
		for (Location neighbor : unitLoc.genNeighbors()) {
			if (gs.getPhysicalGameState().isValidSpace(neighbor.m_a, neighbor.m_b)) {
				TGraph tgraph = new TGraph(playerId, tracedUnitId, new Trace(traceXml, gs.getUnitTypeTable()), epsilon);
				double heuristic = tgraph.getHeuristicT(neighbor, endLoc, gs);
				if (heuristic < minHeuristic) {
					bestLoc = neighbor;
					minHeuristic = heuristic;
				}
			}
		}
		
		return TGraph.diffDirection(unitLoc, bestLoc);
	}
}
