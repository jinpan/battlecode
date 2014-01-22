package swarm;

import java.util.ArrayList;

import battlecode.common.*;

public class BasicPathing{
	
	static ArrayList<MapLocation> snailTrail = new ArrayList<MapLocation>();
	
	public static boolean canMove(Direction dir, boolean selfAvoiding, boolean avoidEnemyHQ, SoldierPlayer myRobot){
		//include both rc.canMove and the snail Trail requirements
		MapLocation resultingLocation = myRobot.myRC.getLocation().add(dir);
		if(selfAvoiding){
			for(int i=0;i<snailTrail.size();i++){
				MapLocation m = snailTrail.get(i);
				if(!m.equals(myRobot.myRC.getLocation())){
					if(resultingLocation.isAdjacentTo(m)||resultingLocation.equals(m)){
						return false;
					}
				}
			}
		}
		if(avoidEnemyHQ)
			if(closeToEnemyHQ(resultingLocation, myRobot))
				return false;
		//if you get through the loop, then dir is not adjacent to the icky snail trail
		return myRobot.myRC.canMove(dir);
	}
	
	public static boolean closeToEnemyHQ(MapLocation loc, SoldierPlayer myRobot){
		return myRobot.enemyHQLoc.distanceSquaredTo(loc)<=RobotType.HQ.attackRadiusMaxSquared;
	}
	
	public static void tryToMove(Direction chosenDirection,boolean selfAvoiding,boolean avoidEnemyHQ, boolean sneak, SoldierPlayer myRobot) throws GameActionException{
		while(snailTrail.size()<2)
			snailTrail.add(new MapLocation(-1,-1));
		if(myRobot.myRC.isActive()){
			snailTrail.remove(0);
			snailTrail.add(myRobot.myRC.getLocation());
			for(int directionalOffset:BaseRobot.directionalLooks){
				int forwardInt = chosenDirection.ordinal();
				Direction trialDir = BaseRobot.allDirections[(forwardInt+directionalOffset+8)%8];
				if(canMove(trialDir,selfAvoiding, avoidEnemyHQ, myRobot)){
					if(sneak){
						myRobot.myRC.sneak(trialDir);
					}else{
						myRobot.myRC.move(trialDir);
					}
					//snailTrail.remove(0);
					//snailTrail.add(rc.getLocation());
					myRobot.myRC.yield();
					break;
				}
			}
			//System.out.println("I am at "+rc.getLocation()+", trail "+snailTrail.get(0)+snailTrail.get(1)+snailTrail.get(2));
		}
	}
	
}