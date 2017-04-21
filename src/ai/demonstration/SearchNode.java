package ai.demonstration;

import rts.GameState;

public class SearchNode extends Node {
	
	public SearchNode(long id, TNodeState nodeState, Node previousNode) {
		super(id, nodeState, previousNode);
	}
}
