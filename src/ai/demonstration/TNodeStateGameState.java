 package ai.demonstration;

import rts.GameState;
import rts.PhysicalGameState;
import rts.units.UnitTypeTable;

public class TNodeStateGameState extends TNodeState {

	private GameState gs;

	
	// Assuming a single opponent
	public TNodeStateGameState(GameState gs, int playerId, int opponentId) {
		super(playerId, opponentId);
		this.gs = gs;
	}
	
	@Override
	protected PhysicalGameState getPGS() {
		return gs.getPhysicalGameState();
	}

	@Override
	public int getTime() {
		return gs.getTime();
	}
	
	@Override
	protected UnitTypeTable getUnitTypeTable() {
		return gs.getUnitTypeTable();
	}
	

}
