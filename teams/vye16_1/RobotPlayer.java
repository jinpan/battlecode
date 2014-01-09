package vye16_1;

import battlecode.common.Robot;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;


@SuppressWarnings("unused")
public class RobotPlayer {
	
	public static final Direction[] dirs = {
        Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST, 
        Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
    public static void run(RobotController rc) {
    	try{
            if(rc.getType() == RobotType.HQ){
           		while(true){
                   	if (rc.isActive() && rc.senseRobotCount()<GameConstants.MAX_ROBOTS) {
                   		Direction dir = dirs[(int)(rc.getRobot().getID()*Math.random()) %8];
               			if (rc.canMove(dir)){
                   				rc.spawn(dir);
               			}
                   	}
           		}
           	} else if(rc.getType() == RobotType.SOLDIER){
           		if(Clock.getRoundNum() < 1500){
           			PASTRPlayer thisRobot = new PASTRPlayer(rc);
           			thisRobot.run();
       			} else {
       				HerderPlayer thisRobot = new HerderPlayer(rc);
       				thisRobot.run();
       			}
       		} else if(rc.getType() == RobotType.PASTR){
           		while(true){
           			rc.yield();
           		}
           	}
       	} catch (Exception e) {
       		e.printStackTrace();
       	}
    }
}