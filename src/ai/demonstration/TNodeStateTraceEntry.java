package ai.demonstration;

import rts.PhysicalGameState;
import rts.TraceEntry;
import rts.units.UnitTypeTable;

public class TNodeStateTraceEntry extends TNodeState {

	TraceEntry traceEntry;
	UnitTypeTable unitTypeTable;
	int playerId;
	int opponentId;
	
	// Assuming single opponent
	public TNodeStateTraceEntry(TraceEntry traceEntry, UnitTypeTable unitTypeTable, int playerId, int opponentId) {
		super(playerId, opponentId);
		this.traceEntry = traceEntry;
		this.unitTypeTable = unitTypeTable;
	}
	
	@Override
	protected PhysicalGameState getPGS() {
		return traceEntry.getPhysicalGameState();
	}

	@Override
	public int getTime() {
		return traceEntry.getTime();
	}
	
	@Override
	protected UnitTypeTable getUnitTypeTable() {
		return unitTypeTable;
	}

}
