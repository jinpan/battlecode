package pastrBasic;

import pastrBasic.BaseRobot.State;
import battlecode.common.MapLocation;

public class Action {
	
	public BaseRobot.State myState;
	public MapLocation targetLocation;
	public int targetID;
	
	public Action(BaseRobot.State state, MapLocation target, int targetID){
		this.myState = state;
		this.targetLocation = target;
		this.targetID = targetID;
	}

}
