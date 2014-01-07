package testPlayer;

import battlecode.common.*;

public class RobotPlayer {
	public static void run(RobotController rc) {
		try{
			if(rc.getType() == RobotType.HQ){
				while(true){
					if (rc.isActive() && rc.senseRobotCount()<GameConstants.MAX_ROBOTS) {
						Direction dir = rc.getLocation().directionTo(rc.senseEnemyHQLocation());
						if (rc.canMove(dir))
							rc.spawn(dir);
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
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}