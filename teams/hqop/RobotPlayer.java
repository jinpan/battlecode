package hqop;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class RobotPlayer {
    public static void run(RobotController rc) {
        BaseRobot myRobot = null;
        
        try {
	        switch(rc.getType()){
	            case HQ: myRobot = new HQPlayer(rc); break;
	            case SOLDIER: myRobot = new SoldierPlayer(rc); break;
	            case PASTR: myRobot = new PastrPlayer(rc); break;
	            case NOISETOWER: myRobot = new NoisePlayer(rc); break;
	        default:
	            myRobot = new DefaultPlayer(rc); break;
	        }
	        
	        myRobot.run();
        } catch (GameActionException e) {
			System.out.println("Soldier Exception");
			e.printStackTrace();
		}
    }
}
