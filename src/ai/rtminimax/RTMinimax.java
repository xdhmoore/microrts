/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ai.rtminimax;

import ai.evaluation.EvaluationFunction;
import ai.evaluation.EvaluationFunctionForwarding;
import ai.AI;
import java.util.List;
import rts.GameState;
import rts.PlayerAction;
import rts.PlayerActionGenerator;
import util.Pair;

/**
 *
 * @author santi
 */
public class RTMinimax extends AI {
    // reset at each execution of minimax:
    static int minCT = -1;
    static int maxCT = -1;
    static int nLeaves = 0;
    
    public static long MAX_BRANCHING = 0;
    public static int MAX_LEAVES = 0;
    
    int LOOKAHEAD = 40;
    
    public RTMinimax(int la) {
        LOOKAHEAD = la;
    }
            
    
    public void reset() {
    }
    
    public AI clone() {
        return new RTMinimax(LOOKAHEAD);
    }     
    
    public PlayerAction getAction(int player, GameState gs) throws Exception {
        
        if (gs.canExecuteAnyAction(player) && gs.winner()==-1) {
            PlayerAction pa = realTimeMinimaxAB(player, gs, LOOKAHEAD); 
            pa.fillWithNones(gs, player);
            return pa;
        } else {
            return new PlayerAction();
        }

    }
    
    
    public static PlayerAction greedyActionScan(GameState gs, int player, long cutOffTime) throws Exception {        
        PlayerAction best = null;
        float bestScore = 0;
        PlayerActionGenerator pag = new PlayerActionGenerator(gs,player);
        PlayerAction pa = null;

//        System.out.println(gs.getUnitActions());
//        System.out.println(pag);
        do{
            pa = pag.getNextAction(cutOffTime);
            if (pa!=null) {
                GameState gs2 = gs.cloneIssue(pa);
                float score = EvaluationFunctionForwarding.evaluate(player, 1 - player, gs2);
                if (best==null || score>bestScore) {
                    best = pa;
                    bestScore = score; 
                }                
            }
            if (System.currentTimeMillis()>cutOffTime) return best;
        }while(pa!=null);
        return best;
    }
    
    
    public PlayerAction realTimeMinimaxAB(int player, GameState gs, int lookAhead) {
        long start = System.currentTimeMillis();
        float alpha = -EvaluationFunction.VICTORY;
        float beta = EvaluationFunction.VICTORY;
        int maxplayer = player;
        int minplayer = 1 - player;
        System.out.println("Starting realTimeMinimaxAB...");
        if (nLeaves>MAX_LEAVES) MAX_LEAVES = nLeaves;
        minCT = -1;
        maxCT = -1;
        nLeaves = 0;
        MiniMaxResult bestMove = realTimeMinimaxAB(gs, maxplayer, minplayer, alpha, beta, gs.getTime() + lookAhead, 0);
        System.out.println("realTimeMinimax: " + bestMove + " in " + (System.currentTimeMillis()-start));
        return bestMove.action;
    }
    

    public MiniMaxResult realTimeMinimaxAB(GameState gs, int maxplayer, int minplayer, float alpha, float beta, int lookAhead, int depth) {
//        System.out.println("realTimeMinimaxAB(" + alpha + "," + beta + ") at " + gs.getTime());
//        gs.dumpActionAssignments();
        
        if (gs.getTime()>=lookAhead || gs.winner()!=-1) {
            int CT = gs.getNextChangeTime();
            if (minCT==-1 || CT<minCT) minCT = CT;
            if (maxCT==-1 || CT>maxCT) maxCT = CT;
            nLeaves++;
//            System.out.println("Eval (at " + gs.getTime() + "): " + EvaluationFunction.evaluate(maxplayer, minplayer, gs));
//            System.out.println(gs);
            return new MiniMaxResult(null,EvaluationFunction.evaluate(maxplayer, minplayer, gs), gs);
        }

        if (gs.canExecuteAnyAction(maxplayer)) {
            List<PlayerAction> actions_max = gs.getPlayerActions(maxplayer);
            int l = actions_max.size();
            if (l>MAX_BRANCHING) MAX_BRANCHING = l;
            MiniMaxResult best = null;
//            System.out.println("realTimeMinimaxAB.max: " + actions_max.size());
            for(PlayerAction action_max:actions_max) {
                GameState gs2 = gs.cloneIssue(action_max);
//                System.out.println("action_max: " + action_max);
                MiniMaxResult tmp = realTimeMinimaxAB(gs2, maxplayer, minplayer, alpha, beta, lookAhead, depth+1);
//                System.out.println(action_max + " -> " + tmp.evaluation);
                alpha = Math.max(alpha,tmp.evaluation);
                if (best==null || tmp.evaluation>best.evaluation) {
                    best = tmp;
                    best.action = action_max;
                }
                
//                if (depth==0) {
//                    System.out.println(action_max + " -> " + tmp.evaluation);
//                    System.out.println(tmp.gs);
//                }
                
                if (beta<=alpha) return best;
            }
            return best;
        } else if (gs.canExecuteAnyAction(minplayer)) {
            List<PlayerAction> actions_min = gs.getPlayerActions(minplayer);
            int l = actions_min.size();
            if (l>MAX_BRANCHING) MAX_BRANCHING = l;
            MiniMaxResult best = null;
//            System.out.println("realTimeMinimaxAB.min: " + actions_min.size());
            for(PlayerAction action_min:actions_min) {
                GameState gs2 = gs.cloneIssue(action_min);
//                System.out.println("action_min: " + action_min);
                MiniMaxResult tmp = realTimeMinimaxAB(gs2, maxplayer, minplayer, alpha, beta, lookAhead, depth+1);
                beta = Math.min(beta,tmp.evaluation);
                if (best==null || tmp.evaluation<best.evaluation) {
                    best = tmp;
                    best.action = action_min;
                }
                if (beta<=alpha) return best;
            }
            return best;
        } else {
            GameState gs2 = gs.clone();
            while(gs2.winner()==-1 && 
//                  gs2.getTime()<lookAhead && 
                  !gs2.canExecuteAnyAction(maxplayer) && 
                  !gs2.canExecuteAnyAction(minplayer)) gs2.cycle();
            return realTimeMinimaxAB(gs2, maxplayer, minplayer, alpha, beta, lookAhead, depth+1);
        }
    }    
}