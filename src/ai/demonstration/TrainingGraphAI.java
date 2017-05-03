package ai.demonstration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.abstraction.pathfinding.PathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import rts.GameState;
import rts.PlayerAction;
import rts.Trace;
import rts.units.UnitTypeTable;

public class TrainingGraphAI {// extends AbstractionLayerAI {
/*
	private int tracePID;
	private double epsilon;
	private Trace trace;
	private UnitTypeTable utt;
	
	public TrainingGraphAI(UnitTypeTable utt) throws JDOMException, IOException {
		this(0,  new Trace(new SAXBuilder().build("./trace1.xml").getRootElement(), utt), 5.0, new AStarPathFinding(), utt);
	}
	
	public TrainingGraphAI(int tracePID, Trace trace, Double epsilon, PathFinding pf, UnitTypeTable utt) {
		super(pf);
		this.epsilon = epsilon;
		this.tracePID = tracePID;
		this.trace = trace;
		this.utt = utt;
	}
	
	@Override
	public PlayerAction getAction(int player, GameState gs) throws Exception {
		TGraph tgraph = new TGraph(tracePID, trace, epsilon);
		return tgraph.getAction(player, gs);
	}

	@Override
	public AI clone() {
		try {
			return new TrainingGraphAI(tracePID, trace, epsilon, pf, utt);
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public List<ParameterSpecification> getParameters() {
		// TODO This returns the parameters that will be a dropdown in the GUI
		// TODO trace file
		// TODO epsilon
        List<ParameterSpecification> parameters = new ArrayList<>();
        
        //parameters.add(new ParameterSpecification("Epsilon",double.class,5));
        //parameters.add(new ParameterSpecification("PlayoutLookahead",int.class,100));
        //parameters.add(new ParameterSpecification("PlayoutAI",AI.class, playoutAI));
        //parameters.add(new ParameterSpecification("EvaluationFunction", EvaluationFunction.class, new SimpleSqrtEvaluationFunction3()));
        
        return parameters;
    }  
    */     
}
