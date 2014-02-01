package team007;

import team007oldoldold.BaseRobot.State;
import battlecode.common.MapLocation;

public class Action {
	
	public State state;
	public MapLocation targetLocation;
	public int targetID;
	
	public Action(State state, MapLocation target, int targetID){
		this.state = state;
		this.targetLocation = target;
		this.targetID = targetID;
	}

}
