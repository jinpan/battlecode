package vye16_1;

import battlecode.common.Robot;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;

import java.util.ArrayList;
import java.util.Arrays;
//import java.util.Comparator;

@SuppressWarnings("unused")
public class HerderPlayer extends BaseRobot {
	
	protected boolean isAttached=false;
	protected int herdingState=0;
	protected MapLocation myPASTR;
	
	public HerderPlayer(RobotController myRC){
		super(myRC);
	}
	
	
	public void attach() throws GameActionException{
		MapLocation[] pastrs= rc.sensePastrLocations(myTeam);
		for (MapLocation pastr: pastrs){
			int read= rc.readBroadcast(1000*pastr.x+ pastr.y);
			if (read==0){
				rc.broadcast(1000*pastr.x+pastr.y, 9999);
				myPASTR= pastr;
				isAttached= true;
				herdingState=1;
			}
		}
	}
	
	public void goToPASTR() throws GameActionException{
		if (rc.getLocation().distanceSquaredTo(myPASTR)<=GameConstants.PASTR_RANGE*1.5){
			herdingState=2;
		} else {
			Direction dir= rc.getLocation().directionTo(myPASTR);
			if (!rc.canMove(dir)) {
				int counter=0;
				//double rand= ID*Math.random()%1;
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
	
	public void herdCows() throws GameActionException{
		Direction dir= rc.getLocation().directionTo(myPASTR);
		if (rc.getLocation().distanceSquaredTo(myPASTR)<=GameConstants.PASTR_RANGE*2){
			if (ID%10 < 5){
				dir.rotateRight().rotateRight();
			}	
			else{
				dir.rotateLeft().rotateLeft();
			}
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
	
	boolean underAttack() throws GameActionException{
		Robot[] enemies= rc.senseNearbyGameObjects(Robot.class, 10, enemyTeam);
		return enemies.length!=0;
	}
	
	public void step() throws GameActionException{
		if (!isAttached){
			attach();
		} else {
			if (herdingState==1){
				goToPASTR();
			}
			if (herdingState==2){
				if (underAttack()){
					Robot[] enemies= rc.senseNearbyGameObjects(Robot.class, 10, enemyTeam);
					RobotInfo enemy= rc.senseRobotInfo(enemies[0]);
					rc.attackSquare(enemy.location);
				} else {
					herdCows();
				}
			}
		}
	}

}
