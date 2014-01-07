package vye16_1;

import battlecode.common.Robot;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.GameActionException;

//import java.util.Comparator;
import java.util.Arrays;

@SuppressWarnings("unused")
public abstract class BaseRobot {
	
	public enum State {Herder, PASTR, NoiseTower};
	
	public RobotController rc;
	public Team myTeam;
	public Team enemyTeam;
	public int ID;
	public State myState;
	
	public static final Direction[] dirs = {
        Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, 
        Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	public BaseRobot(RobotController myRC){
		this.rc= myRC;
		this.myTeam= this.rc.getTeam();
		this.enemyTeam= this.myTeam.opponent();
		this.ID= this.rc.getRobot().getID();
	}
	
	public void run(){
        while (true){
            try {
                this.transition();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public void transition() throws GameActionException {
        this.step();
        rc.yield();
    }
    
    public void step() throws GameActionException{
    	
    }
    
    public float random(){
    	float rand = (float)(this.ID*Math.random());
    	return rand%1;
    }
	
    /*public class distComparator implements Comparator<MapLocation>{
    	MapLocation startLoc;
    	
    	public distComparator(MapLocation loc){
    		startLoc= loc;
    	}
    	
    	public int compare(MapLocation loc1, MapLocation loc2){
    		int out= startLoc.distanceSquaredTo(loc1)-startLoc.distanceSquaredTo(loc2);
    		return out/Math.abs(out);
    	}
    }*/
    
    
}
