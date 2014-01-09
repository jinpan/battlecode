package jinpan;

import java.util.ArrayList;
import java.util.HashMap;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;

public class HQPlayer extends BaseRobot {
	
	Direction toEnemy;
	MapLocation[] myCorners;
	int[][] pastureLocs;
	
	int numRobots;

    public HQPlayer(RobotController myRC) throws GameActionException {
        super(myRC);
        
        this.toEnemy = this.myHQLoc.directionTo(this.enemyHQLoc);
        this.numRobots = 1;
        
        this.pastureLocs = new int[1000][2];
        
        this.myCorners = new MapLocation[2];
        
        if (this.myHQLoc.x < this.myRC.getMapWidth() / 2){
            this.myCorners[0] = new MapLocation(GameConstants.PASTR_RANGE, GameConstants.PASTR_RANGE);
            this.myCorners[1] = new MapLocation(GameConstants.PASTR_RANGE, this.myRC.getMapWidth() - GameConstants.PASTR_RANGE);
        }
        else {
            this.myCorners[0] = new MapLocation(this.myRC.getMapWidth() - GameConstants.PASTR_RANGE, GameConstants.PASTR_RANGE);
            this.myCorners[1] = new MapLocation(this.myRC.getMapWidth() - GameConstants.PASTR_RANGE, this.myRC.getMapWidth() - GameConstants.PASTR_RANGE);
        }
    }

    @Override
    protected void step() throws GameActionException {
        if (this.myRC.isActive() && this.myRC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
            this.spawn();
            ++this.numRobots;
        }
        
        // TODO: preserve state so we don't recompute all of idToOrder each step
        /*HashMap<Integer, Integer> idToOrder = new HashMap<Integer, Integer>();
        int channel, id;
        for (int i=1; i<this.numRobots; ++i){
        	channel = BaseRobot.get_outbox_channel(i, BaseRobot.OUTBOX_ID_CHANNEL);
        	id = this.myRC.readBroadcast(channel);
        	idToOrder.put(id,  i);
        }
        
        int order;
        StateMessage state;
        
        for (Robot robot: this.myRC.senseNearbyGameObjects(Robot.class)){
        	if (idToOrder.get(robot.getID()) == null){
        		continue;
        	}
        	order = idToOrder.get(robot.getID());
        	channel = BaseRobot.get_outbox_channel(order, BaseRobot.OUTBOX_STATE_CHANNEL);
        	state = StateMessage.decode(this.myRC.readBroadcast(channel));
        	if (state.myState == BaseRobot.State.DEFAULT){
        		int idx = (int) (this.random() * this.myCorners.length);
        		ActionMessage action = new ActionMessage(BaseRobot.State.PASTURE, 0, this.myCorners[idx]);
        		channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
        		this.myRC.broadcast(channel, action.encode());
        	}
        }*/
        
        HashMap<int[], ArrayList<Integer>> attackers = new HashMap<int[], ArrayList<Integer>>();
        ArrayList<int[]> enemyList = new ArrayList<int[]>();
        
        int maxOrder= this.myRC.readBroadcast(BaseRobot.ORDER_CHANNEL);
        int channel; StateMessage state;
        
        for (int order=0; order<=maxOrder; order++){
        	channel= BaseRobot.get_outbox_channel(order, BaseRobot.OUTBOX_STATE_CHANNEL);
        	state= StateMessage.decode(this.myRC.readBroadcast(channel));
        	if (state.myState== BaseRobot.State.DEFAULT){
        		int idx = (int) (this.random() * this.myCorners.length);
        		ActionMessage action = new ActionMessage(BaseRobot.State.PASTURE, 0, this.myCorners[idx]);
        		channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
        		this.myRC.broadcast(channel, action.encode());
        	}
        	/*if (state.myState.name().contains("ATTACK")){
        		channel= BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
        		ActionMessage act= ActionMessage.decode(this.myRC.readBroadcast(channel));
        		ArrayList<Integer> bots;
        		int[] newKey= {act.targetID, 1000*act.targetLocation.x+act.targetLocation.y};
        		if (!attackers.containsKey(newKey)){
        			bots= new ArrayList<Integer>();
        			enemyList.add(newKey);
        		} else {
        			bots= attackers.get(newKey);
        		}
        		bots.add(order);
        		attackers.put(newKey, bots);
        	}*/
        }
        /*for (int[] enemy: enemyList){
        	ArrayList<Integer> inboxes= attackers.get(enemy);
        	for (int order: inboxes){
        		MapLocation goTo= new MapLocation(enemy[1]/1000, enemy[1]%1000);
        		ActionMessage msg= new ActionMessage(BaseRobot.State.ATTACKHIGH, enemy[0], goTo);
        		channel= BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
        		this.myRC.broadcast(channel,  msg.encode());
        	}
        }*/
        
    }
    
    /*private MapLocation averageLoc(ArrayList<MapLocation> locs){
    	int x= 0; int y=0;
    	for (MapLocation loc: locs){
    		x+= loc.x; y+= loc.y;
    	} x/=locs.size(); y/=locs.size();
    	return new MapLocation(x, y);
    }*/
    
    private void assignPasture(SoldierPlayer soldier, MapLocation loc) throws GameActionException {
    	ActionMessage msg = new ActionMessage(BaseRobot.State.PASTURE, 0, loc);
    	int channel = soldier.get_inbox_channel(0);
    	this.myRC.broadcast(channel, msg.encode());
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
