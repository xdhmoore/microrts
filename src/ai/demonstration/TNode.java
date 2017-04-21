package ai.demonstration;

import rts.GameState;
import rts.TraceEntry;

public class TNode extends Node {

	//TODO do we really need two classes for node types?
	public TNode(long id, TNodeState nodeState, Node prevNode) {
		super(id, nodeState, prevNode);
	}
	
}
