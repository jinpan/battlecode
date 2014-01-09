package pastrBasic;

import java.util.ArrayList;
import java.util.HashMap;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.TerrainTile;

public class HQPlayer extends BaseRobot {
	
	Direction toEnemy;
	MapLocation[] myCorners; //these are the places we want to set up PASTRs
	
	int numRobots;

    public HQPlayer(RobotController myRC) throws GameActionException {
        super(myRC);
        
        this.toEnemy = this.myHQLoc.directionTo(this.enemyHQLoc);
        this.numRobots = 1;
        
        /*//tries to put PASTRs in map corners
        this.myCorners = new MapLocation[2];
        
        if (this.myHQLoc.x < this.myRC.getMapWidth() / 2){
            this.myCorners[0] = new MapLocation(1, 1);
            this.myCorners[1] = new MapLocation(1, this.myRC.getMapWidth() - 1);
        }
        else {
            this.myCorners[0] = new MapLocation(this.myRC.getMapWidth() - 1, 1);
            this.myCorners[1] = new MapLocation(this.myRC.getMapWidth() - 1, this.myRC.getMapWidth() - 1);
        }
        */
        
        this.myCorners = new MapLocation[4];
        
        int allocated = 0;
        
        for(int i = 0; i < 8; i++){ //attempt to place PASTRs in 'open' directions, a reasonable distance away
        	MapLocation eLoc = myHQLoc;
        	Direction d = dirs[i];
        	
        	TerrainTile eTerrain = myRC.senseTerrainTile(eLoc);
        	int stepsToWall = 0;
        	while((eTerrain == TerrainTile.NORMAL || eTerrain == TerrainTile.ROAD) && stepsToWall < 40){
        		eLoc = eLoc.add(d);
        		stepsToWall+=2;
        		if(d.isDiagonal())
        			stepsToWall++;
        		eTerrain = myRC.senseTerrainTile(eLoc);
        	}
        	
        	if(stepsToWall > 20){ //if it's far enough away, put it in our list
        		this.myCorners[allocated] = eLoc;
        		allocated++;
        	}
        	
        	if(allocated > 3)
        		break;
        }
        
        while(allocated < 4){ //fill in unoccupied slots
        	this.myCorners[allocated] = myHQLoc.add(dirs[(int)(Math.random()*8)], 15);
        	allocated++;
        }
        
    }

    @Override
    protected void step() throws GameActionException {
        if (this.myRC.isActive() && this.myRC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
            this.spawn();
            ++this.numRobots;
        }
        
        int now = Clock.getBytecodesLeft();
        
        // TODO: preserve state so we don't recompute all of idToOrder each step
        HashMap<Integer, Integer> idToOrder = new HashMap<Integer, Integer>();
        int channel, id;
        for (int i=1; i<this.numRobots; ++i){
        	channel = BaseRobot.get_outbox_channel(i, BaseRobot.OUTBOX_ID_CHANNEL);
        	id = this.myRC.readBroadcast(channel);
        	idToOrder.put(id,  i);
        }
        
        now -= Clock.getBytecodesLeft();
        this.myRC.setIndicatorString(0, Integer.toString(now));
        
        int order;
        StateMessage state;
        
        for (Robot robot: this.myRC.senseNearbyGameObjects(Robot.class, 4)){ //for every nearby robot
        	if (idToOrder.get(robot.getID()) == null){
        		continue;
        	}
        	order = idToOrder.get(robot.getID());
        	channel = BaseRobot.get_outbox_channel(order, BaseRobot.OUTBOX_STATE_CHANNEL);
        	state = StateMessage.decode(this.myRC.readBroadcast(channel));
        	if (state.myState == BaseRobot.State.DEFAULT){
        		/*
        		int idx = (int) (this.random() * this.myCorners.length); //make it try to build at a random good pasture location
        		ActionMessage action = new ActionMessage(BaseRobot.State.PASTURE, 0, this.myCorners[idx]);
        		*/
        		
        		ActionMessage action;
        		action = new ActionMessage(BaseRobot.State.PASTURE, 0, this.myCorners[order%4]);

        		channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
        		this.myRC.broadcast(channel, action.encode());
        		
        	}
        }
    }
    
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
