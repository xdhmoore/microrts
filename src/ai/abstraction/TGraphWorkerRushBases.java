package ai.abstraction;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.abstraction.pathfinding.TGraphPathFinding;
import ai.core.ParameterSpecification;
import ai.demonstration.TGraph;
import rts.GameState;
import rts.PhysicalGameState;
import rts.Player;
import rts.units.Unit;
import rts.units.UnitTypeTable;

public class TGraphWorkerRushBases extends WorkerRush {

	private double epsilon;
	private String traceFile;
	
	public TGraphWorkerRushBases(UnitTypeTable a_utt) {
		super(a_utt);
	}
	
	public void setTraceFile(String file) {
		this.traceFile = file;
	}
	
	public void setEpsilon(double epsilon) {
		this.epsilon = epsilon;
		this.setPathFinding(new TGraphPathFinding(epsilon, this.traceFile));
	}

	public TGraphWorkerRushBases(UnitTypeTable a_utt, double epsilon, String traceFile) {
		super(a_utt, new TGraphPathFinding(epsilon, traceFile));
	}
	
	@Override
	public void meleeUnitBehavior(Unit u, Player p, GameState gs) {
        PhysicalGameState pgs = gs.getPhysicalGameState();
        Unit closestEnemy = null;
        int closestDistance = 0;
        for(Unit u2:pgs.getUnits()) {
        	
        	//Attack only bases
        	if (u2.getType().isStockpile) {
	            if (u2.getPlayer()>=0 && u2.getPlayer()!=p.getID()) { 
	                int d = Math.abs(u2.getX() - u.getX()) + Math.abs(u2.getY() - u.getY());
	                if (closestEnemy==null || d<closestDistance) {
	                    closestEnemy = u2;
	                    closestDistance = d;
	                }
	            }
        	}
        }
        if (closestEnemy!=null) {
            attack(u,closestEnemy);
        }
	}
	
    @Override
    public List<ParameterSpecification> getParameters()
    {
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        parameters.add(new ParameterSpecification("TraceFile", String.class, "./trap_modified_avoid_trace.xml"));
        parameters.add(new ParameterSpecification("Epsilon", double.class, TGraph.DEFAULT_EPSILON));
        //parameters.add(new ParameterSpecification("PathFinding", PathFinding.class, new AStarPathFinding()));

        return parameters;
    }

}
