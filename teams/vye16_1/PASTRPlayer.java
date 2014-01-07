package vye16_1;

import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("unused")
public class PASTRPlayer extends BaseRobot{
	
	//protected MapLocation destination;
	public int distThresh= 16;

	public PASTRPlayer(RobotController myRC){
		super(myRC);
		this.myState= State.PASTR;
		//this.getDestination();
	}
	
	/*protected void getDestination(){
		
	}*/
	
	boolean isPASTRable(MapLocation loc){
		MapLocation[] pastrs= rc.sensePastrLocations(myTeam);
		boolean good= true;
		if (loc.distanceSquaredTo(rc.senseHQLocation())< distThresh){
			good= false;
		}
		if (good==true){
			for (MapLocation pastr: pastrs){
				if (pastr.distanceSquaredTo(loc)<= GameConstants.PASTR_RANGE*4){
					good= false;
				}
			}	
		}
		return good;
	}
	
	public void step() throws GameActionException{
		if (isPASTRable(rc.getLocation()) && rc.isActive()){
			rc.construct(RobotType.PASTR);
		} else{
			Direction dir= Direction.NORTH;
			if (!rc.canMove(dir)) {
				int counter=0;
				while (!rc.canMove(dir)&&counter<8){
					dir.rotateLeft();
					counter++;
				}
			}
			if (rc.canMove(dir) && rc.isActive()){
				rc.move(dir);
			}
		}
	}
}
