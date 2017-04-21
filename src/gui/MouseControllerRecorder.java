package gui;

import rts.GameState;
import rts.PlayerAction;
import util.XMLWriter;

public class MouseControllerRecorder extends MouseController {

	private XMLWriter writer;
	
	public MouseControllerRecorder(PhysicalGameStateMouseJFrame frame, XMLWriter writer) {
		super(frame);
		this.writer = writer;
	}
	
	@Override
	public PlayerAction getAction(int player, GameState gs) {
		
		//TODO use composition instead of inheritance
		PlayerAction action = super.getAction(player, gs);
		
		//TODO save action
		
		
		return action;
	}

}
