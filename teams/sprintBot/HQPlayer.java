package sprintBot;

import java.util.ArrayList;

import sprintBot.BaseRobot;
import sprintBot.BaseRobot.State;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class HQPlayer extends BaseRobot {
	
	Direction toEnemy;
	MapLocation[] myCorners;
	
	int numRobots;

    public HQPlayer(RobotController myRC) throws GameActionException {
        super(myRC);
        
        this.toEnemy = this.myHQLoc.directionTo(this.enemyHQLoc);
        this.numRobots = 1;
    }

    @Override
    protected void step() throws GameActionException {
    	Robot[] nearbyEnemies = this.myRC.senseNearbyGameObjects(Robot.class, 10000, this.enemyTeam);
		if (this.myRC.isActive() && nearbyEnemies.length != 0) {
			this.shoot(nearbyEnemies);
		}    	
    	
        if (this.myRC.isActive() && this.myRC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
            this.spawn();
            ++this.numRobots;
        }
        
        int dist = this.myRC.getMapHeight() * this.myRC.getMapHeight() + this.myRC.getMapWidth() * this.myRC.getMapWidth();
        MapLocation closestTarget = null;
        
        MapLocation[] targets = this.myRC.sensePastrLocations(this.enemyTeam);
        for (int i=0; i<targets.length; ++i){
        	if (targets[i].distanceSquaredTo(this.enemyHQLoc) > 15){
        		if ((targets[i].distanceSquaredTo(this.myHQLoc) < dist)){
        			closestTarget = targets[i];
        		}
				EnemyProfileMessage enemyProf = new EnemyProfileMessage(0, 200, targets[i], Clock.getRoundNum());
				this.squad_send(BaseRobot.SQUAD_BLDG_HITLIST + 2 * i, enemyProf.encode());
        	}
        }
        
        int order, channel;
        StateMessage state;
        
        if (closestTarget != null){
	        for (Robot robot: this.myRC.senseNearbyGameObjects(Robot.class)) {
	        	if (idToOrder(robot.getID()) == 0){
	        		continue;
	        	}
	        	order = idToOrder(robot.getID());
	        	channel = BaseRobot.get_outbox_channel(order, BaseRobot.OUTBOX_STATE_CHANNEL);
	        	state = StateMessage.decode(this.myRC.readBroadcast(channel));
	        	if (state.state == BaseRobot.State.DEFAULT){
	        		ActionMessage action = new ActionMessage(BaseRobot.State.ATTACK, 0, closestTarget);
	        		channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
	        		this.myRC.broadcast(channel, (int) action.encode());
	        	}
	        }
        }
    }
    
    private boolean spawn() throws GameActionException {
		if (this.myRC.senseObjectAtLocation(this.myHQLoc.add(this.toEnemy)) == null){
			this.myRC.spawn(this.toEnemy);
			return true;
		}
		else {
	        for (Direction dir: BaseRobot.dirs){
	            if (dir != this.toEnemy
	            		&& this.myRC.senseObjectAtLocation(this.myHQLoc.add(dir)) == null){
	                this.myRC.spawn(dir);
	                return true;
	            }
	        }
		}
    	return false;
    }
    
    private boolean shoot(Robot[] nearbyEnemies) throws GameActionException{
    	MapLocation curloc;
    	
    	for(Robot r : nearbyEnemies){ //try to hit something directly
    		curloc = this.myRC.senseRobotInfo(r).location;
    		if(this.myRC.canAttackSquare(curloc)){
    			this.myRC.attackSquare(curloc);
    			return true;
    		}
    	}
    	
    	for(Robot r : nearbyEnemies){ //try to hit something for splash damage
    		curloc = this.myRC.senseRobotInfo(r).location;
    		curloc = curloc.add(curloc.directionTo(this.myHQLoc));
    		if(this.myRC.canAttackSquare(curloc)){
    			this.myRC.attackSquare(curloc);
    			return true;
    		}
    	}
    	
    	return false; //give up    	
    }

}
