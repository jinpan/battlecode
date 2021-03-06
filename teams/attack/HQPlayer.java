package attack;

import java.util.ArrayList;
import java.util.HashMap;

import attack.BaseRobot.State;
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
				this.squad_send(BaseRobot.SQUAD_LOW_HITLIST + 2 * i, enemyProf.encode());
        	}
        }
        
        HashMap<Integer, Integer> idToOrder = new HashMap<Integer, Integer>();
        int channel, id;
        for (int i=1; i<this.numRobots; ++i){
        	channel = BaseRobot.get_outbox_channel(i, BaseRobot.OUTBOX_ID_CHANNEL);
        	id = this.myRC.readBroadcast(channel);
        	idToOrder.put(id, i);
        }
        
        int order;
        StateMessage state;
        
        if (closestTarget != null){
	        for (Robot robot: this.myRC.senseNearbyGameObjects(Robot.class)) {
	        	if (idToOrder.get(robot.getID()) == null){
	        		continue;
	        	}
	        	order = idToOrder.get(robot.getID());
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

}
