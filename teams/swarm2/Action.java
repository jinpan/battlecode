package swarm2;

import battlecode.common.MapLocation;

public class Action {
	
	public BaseRobot.State state;
	public MapLocation targetLocation;
	public int targetID;
	
	public Action(BaseRobot.State state, MapLocation target, int targetID){
		this.state = state;
		this.targetLocation = target;
		this.targetID = targetID;
	}

}
