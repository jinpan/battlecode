package testPlayer;

import battlecode.common.*;

public class RobotPlayer {
	public static void run(RobotController rc) {
		try{
			if(rc.getType() == RobotType.HQ){
				while(true){
					Robot[] baddies = rc.senseNearbyGameObjects(Robot.class, 15, rc.getTeam().opponent());
					if (baddies.length == 0 && rc.isActive() && rc.senseRobotCount()<GameConstants.MAX_ROBOTS) {
						Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
						if (rc.canMove(dir))
							rc.spawn(dir);
					} else if (baddies.length != 0) {
						if(rc.isActive())
							rc.attackSquare(rc.senseRobotInfo(baddies[0]).location);
					}
				}
			} else if(rc.getType() == RobotType.SOLDIER){
				if(Clock.getRoundNum() < 15){
					pastrPlayer thisRobot = new pastrPlayer(rc);
					thisRobot.run();
				} else {
					soldierPlayer thisRobot = new soldierPlayer(rc);
					thisRobot.run();
				}
			} else if(rc.getType() == RobotType.PASTR){
				while(true)
					rc.yield();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}