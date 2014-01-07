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
	
	public int distThresh= 64;
	public boolean farFromHQ=false;

	public PASTRPlayer(RobotController myRC){
		super(myRC);
		this.myState= State.PASTR;
	}
	
	boolean isFar(){
		return rc.getLocation().distanceSquaredTo(rc.senseHQLocation())> distThresh;
	}
	
	boolean isPASTRable(){
		MapLocation loc= rc.getLocation();
		MapLocation[] pastrs= rc.sensePastrLocations(myTeam);
		boolean good= true;
		if (!isFar()){
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
		if (isPASTRable()){
			rc.construct(RobotType.PASTR);
		} else{
			Direction dir;
			if (isFar()){
				dir= dirs[(int)(ID*Math.random()) % 8];
			} else {
				dir= rc.getLocation().directionTo(rc.senseHQLocation()).opposite();
			}
			if (!rc.canMove(dir)){
				int counter=0;
				//float rand= random();
				//if (rand<0.5){
					while (!rc.canMove(dir) && counter<8){
						dir.rotateRight();
						counter++;
					}
				/*} else{
					while (!rc.canMove(dir) && counter<8){
						dir.rotateLeft();
						counter++;
					}
				}*/
			}
			if (rc.canMove(dir)&& rc.isActive()){
				rc.move(dir);
			}
		}
	}
}
