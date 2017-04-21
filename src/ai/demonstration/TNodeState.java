package ai.demonstration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import rts.PhysicalGameState;
import rts.Player;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;

public abstract class TNodeState {
	
	private int playerId;
	private int opponentId;
	
	private List<Unit> _myUnits;
	private List<Unit> _myMobileUnits;
	private List<Unit> _myBuildings;
	private List<Unit> _myBases;
	private List<Unit> _opponentUnits;
	private List<Unit> _opponentMobileUnits;
	private List<Unit> _opponentBuildings;
	private List<Unit> _opponentBases;
	
	private RealVector vector;
	private UnitTypeTable unitTypeTable;

	private RealVector maxStateDistancePerCycleVector;
	
	protected TNodeState(int playerId, int opponentId) {
		this.playerId = playerId;
		this.playerId = opponentId;
	}
	
	private void initStateVector() {
		List<Double> items = new ArrayList<>();
		items.add((double) getNumMyResources());
		items.add((double) getNumMyWorkers());
		items.add((double) getNumMyAttackUnits());
		items.add((double) getNumMyBases());
		items.add((double) getNumMyBarracks());
		
		items.add((double) getNumOpponentResources());
		items.add((double) getNumOpponentWorkers());
		items.add((double) getNumOpponentAttackUnits());
		items.add((double) getNumOpponentBases());
		items.add((double) getNumOpponentBarracks());
		
		vector = new ArrayRealVector(items.toArray(new Double[]{}));
	}
	
	protected void init() {
		initStateVector();
	}

	public abstract int getTime();
	protected abstract PhysicalGameState getPGS();
	protected abstract UnitTypeTable getUnitTypeTable();
	
	public RealVector getVector() {
		return vector;
	}
	
	private Player getPlayer() {
		return getPGS().getPlayer(playerId);
	}
	
	//TODO there has to be an annotation in a library somewhere that does caching like this
	private List<Unit> getMyUnits() {
		if (null != _myUnits) return _myUnits;
		_myUnits = getPGS().getUnits().stream().filter(u -> { return u.getPlayer() == playerId; }).collect(Collectors.toList());
		return _myUnits;
	}
	
	private List<Unit> getMyMobileUnits() {
		if (null != _myMobileUnits) return _myMobileUnits;
		_myMobileUnits = getMyUnits().stream().filter(u -> { return u.getType().canMove; }).collect(Collectors.toList());
		return _myMobileUnits;
	}
	
	private List<Unit> getMyBuildings() {
		if (null != _myBuildings) return _myBuildings;
		_myBuildings = getMyUnits().stream().filter(u -> { return !u.getType().canMove; }).collect(Collectors.toList());
		return _myBuildings;
	}
	
	public List<Unit> getMyBases() {
		if (null != _myBases) return _myBases;
		_myBases = getMyBuildings().stream().filter(u -> { return u.getType().isStockpile; }).collect(Collectors.toList());
		return _myBases;
	}

	public int getNumMyResources() {
		return getPGS().getPlayer(playerId).getResources();
	}

	public int getNumMyWorkers() {
		//Can cast safely because list size() returns an int anyway
		return (int) getMyMobileUnits().stream().filter(u -> { return u.getType().canHarvest; }).count();
	}

	public int getNumMyAttackUnits() {
		//Can cast safely because list size() returns an int anyway
		return (int) getMyMobileUnits().stream().filter(u -> { return !u.getType().canHarvest;}).count();
	}

	public int getNumMyBases() {
		return (int) getPGS().getUnits().stream().filter(u -> { return !u.getType().canMove && u.getType().isStockpile; }).count();
	}

	public int getNumMyBarracks() {
		// Making the assumption that any building not a stockpile is a barracks
		return (int) getPGS().getUnits().stream().filter(u -> { return !u.getType().canMove && u.getType().isStockpile; }).count();
	}
	
	//TODO there has to be an annotation in a library somewhere that does caching like this
	private List<Unit> getOpponentUnits() {
		if (null != _opponentUnits) return _opponentUnits;
		_opponentUnits = getPGS().getUnits().stream().filter(u -> { return u.getPlayer() == opponentId; }).collect(Collectors.toList());
		return _opponentUnits;
	}
	
	private List<Unit> getOpponentMobileUnits() {
		if (null != _opponentMobileUnits) return _opponentMobileUnits;
		_opponentMobileUnits = getOpponentUnits().stream().filter(u -> { return u.getType().canMove; }).collect(Collectors.toList());
		return _opponentMobileUnits;
	}
	
	private List<Unit> getOpponentBuildings() {
		if (null != _opponentBuildings) return _opponentBuildings;
		_opponentBuildings = getOpponentUnits().stream().filter(u -> { return !u.getType().canMove; }).collect(Collectors.toList());
		return _opponentBuildings;
	}
	
	public List<Unit> getOpponentBases() {
		if (null != _opponentBases) return _opponentBases;
		_opponentBases = getOpponentBuildings().stream().filter(u -> { return u.getType().isStockpile; }).collect(Collectors.toList());
		return _opponentBases;
	}
	
	public List<Unit> getResources() {
		return getPGS().getUnits().stream().filter(u -> { return u.getType().isResource; }).collect(Collectors.toList());
	}

	public int getNumOpponentResources() {
		return getPGS().getPlayer(opponentId).getResources();
	}

	public int getNumOpponentWorkers() {
		//Can cast safely because list size() returns an int anyway
		return (int) getOpponentMobileUnits().stream().filter(u -> { return u.getType().canHarvest; }).count();
	}

	public int getNumOpponentAttackUnits() {
		//Can cast safely because list size() returns an int anyway
		return (int) getOpponentMobileUnits().stream().filter(u -> { return !u.getType().canHarvest;}).count();
	}

	public int getNumOpponentBases() {
		return (int) getPGS().getUnits().stream().filter(u -> { return !u.getType().canMove && u.getType().isStockpile; }).count();
	}

	public int getNumOpponentBarracks() {
		// Making the assumption that any building not a stockpile is a barracks
		return (int) getPGS().getUnits().stream().filter(u -> { return !u.getType().canMove && u.getType().isStockpile; }).count();
	}
	
	private int getUnitProductionTime(String unitTypeName) {
		return getUnitTypeTable().getUnitType(unitTypeName).produceTime;
	}
	
	private static class UnitPredicates {
		public static boolean isAttackUnitType(UnitType t) {
			return t.canMove && !t.canHarvest;
		}
	}
	
	private int getMinAttackUnitProductionTime() {
		//TODO this could be cached, or maybe even static
		return getUnitTypeTable().getUnitTypes().stream().filter(UnitPredicates::isAttackUnitType).map(t -> t.produceTime).min(Integer::compare).get();
	}
	
	private UnitType getWorkerType() {
		return getUnitTypeTable().getUnitType("worker");
	}
	
	private int getMinResourceRouteTime(Iterable<Unit> bases) {
		return (int) (getWorkerType().harvestTime + 2.0 * getShortestResourceRouteDistance(bases) * getWorkerType().moveTime + getWorkerType().returnTime);
	}
	
	private int getMyMinResourceRouteTime() {
		return getMinResourceRouteTime(getMyBases());
	}
	
	private int getOpponentMinResourceRouteTime() {
		return getMinResourceRouteTime(getOpponentBases());
	}
	
	private double getShortestResourceRouteDistance(Iterable<Unit> bases) {
		double minDistance = Double.POSITIVE_INFINITY;
		
		for (Unit base : bases) {
			for (Unit resource : getResources()) {
				minDistance = Math.min(minDistance, base.euclideanDistance(resource));
			}
		}
		return minDistance;
	}
	
	
	//All these values are the largest deltas that can be achieved in a single cycle. For example, the most
	//that a player can possibly increase their resources (on average) in a single cycle.
	private RealVector createMaxStepVector(TNodeState destNode) {
		List<Double> items = new ArrayList<>();
		
		//Max resources collected per cycle
		//SM - add ability to add new resource collection sites
		items.add(((double) destNode.getNumMyWorkers() * getWorkerType().harvestAmount) / (double) getMyMinResourceRouteTime());
		
		//TODO this is a gross overestimate
		//Max workers created per cycle
		//SM - limit this by the resource collection rate:
		//SM - factor in the potential for adding new bases
		items.add(((double) destNode.getNumMyBases()) / ((double) getUnitProductionTime("worker")));
		
		//Max attack units created per cycle
		//SM - same as above
		items.add((double) destNode.getNumMyBarracks() / ((double) getMinAttackUnitProductionTime()));

		//Max bases built per cycle
		//TODO are workers occupied during building creation?
		//SM - factor in creation of new workers
		items.add((double) destNode.getNumMyWorkers() / (double) getUnitProductionTime("base"));

		//Max barracks built per cycle
		//SM - factor in creation of new workers
		//SM - factor in creation of new bases that create new workers...:S
		items.add((double) destNode.getNumMyWorkers() / (double) getUnitProductionTime("barracks"));
		
		
		//Max resources collected per cycle
		items.add(((double) destNode.getNumOpponentWorkers() * getWorkerType().harvestAmount) / (double) getOpponentMinResourceRouteTime());
		
		//TODO this is a gross overestimate
		//Max workers created per cycle
		items.add(((double) destNode.getNumOpponentBases()) / ((double) getUnitProductionTime("worker")));
		
		//Max attack units created per cycle
		items.add((double) destNode.getNumOpponentBarracks() / ((double) getMinAttackUnitProductionTime()));

		//Max bases built per cycle
		items.add((double) destNode.getNumOpponentWorkers() / (double) getUnitProductionTime("base"));

		//Max barracks built per cycle
		items.add((double) destNode.getNumOpponentWorkers() / (double) getUnitProductionTime("barracks"));
		
		return new ArrayRealVector(items.toArray(new Double[]{}));
	}
	

	//TODO this needs to be fixed. it's not the euclidean distance
	public double maxDeltaHeuristic(TNodeState nodeState) {
		//distance between two points is shortest dist divided by max distance gained per cycle
		
		RealVector maxStepVector = createMaxStepVector(nodeState);
		
		//End state - Current State / largest step in one cycle = min cycles
		return vector.subtract(nodeState.getVector()).ebeDivide(maxStepVector).getMaxValue(); 
	}

	// TODO
	// Number of my uncollected resources
	// Number of opponent's uncollected resources
	// order of my units from closest to opponent's center of mass to furthest
	// order of opponent's units from my center of mass to furthest
	// center of mass of each type of unit for me (or even just buildings, workers, and attack units)
	// center of mass of each type of unit for opponent (or even ")
	// time since last attack
	// location of last attack
	// time since my last attack on them
	// location of my last attack on them
}
