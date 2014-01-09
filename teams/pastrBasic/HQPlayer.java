package pastrBasic;

import java.util.ArrayList;
import java.util.HashMap;

import pastrBasic.ActionMessage;
import battlecode.common.*;

public class HQPlayer extends BaseRobot {
	Direction toEnemy;
	MapLocation[] PASTRLocs;

	int numRobots, numProcessed; //total number of robots, number of robots in the hashmap
	int numPASTR;
	
	public HQPlayer(RobotController myRC) throws GameActionException {
		super(myRC);

		this.toEnemy = this.myHQLoc.directionTo(this.enemyHQLoc);
		this.numRobots = 1;
		this.numProcessed = 0;
		
		numPASTR = find_smart_PASTR_number(); //make this method smart!
		this.PASTRLocs = new MapLocation[numPASTR];
		PASTRLocs = find_k_best_pasture_locations(numPASTR); //make this method smart!
	}

    @Override
    protected void step() throws GameActionException {
		if (this.myRC.isActive() && this.myRC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			this.spawn();
			++this.numRobots;
		}	
        
        int order, channel;
        StateMessage state;
        
    	//MapLocation[] pastrLocs = this.myRC.sensePastrLocations(this.myTeam);
		//MapLocation[] enemyPastrs= this.myRC.sensePastrLocations(this.enemyTeam);
        
        for (Robot robot: this.myRC.senseNearbyGameObjects(Robot.class, 4)){ //for every nearby robot
        	/*
        	if (idToOrder.get(robot.getID()) == null){
        		continue;
        	}
        	order = idToOrder.get(robot.getID());
        	channel = BaseRobot.get_outbox_channel(order, BaseRobot.OUTBOX_STATE_CHANNEL);
        	state = StateMessage.decode(this.myRC.readBroadcast(channel)).myState;
        	if (state == BaseRobot.State.DEFAULT){
        		if (pastrLocs.length<BaseRobot.MAX_PASTURES){
        			state= BaseRobot.State.PASTURE;
        		} else {
        			state= BaseRobot.State.SCOUT;
        		}
        	}
        	
        	if (state==BaseRobot.State.PASTURE) {
				ActionMessage action;
				action = new ActionMessage(BaseRobot.State.PASTURE, 0, this.myCorners[order % 4]);
				channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
				this.myRC.broadcast(channel, action.encode());
			} else if (state==BaseRobot.State.SCOUT) {
				MapLocation enemyLoc= enemyPastrs[0];
				Robot enemyBot= this.myRC.senseObjectAtLocation(enemyLoc);
				Direction dirToPastr= this.myHQLoc.directionTo(enemyLoc);
				MapLocation rallyPoint= enemyLoc.add(dirToPastr, this.RALLY_DISTANCE);
				ActionMessage action = new ActionMessage(BaseRobot.State.SCOUT, enemyLoc, enemyBot.getID());
				channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
				this.myRC.broadcast(channel, action.encode());
			}
			*/
        	
			order = idToOrder(robot.getID()); 
			if (order == 0)
				continue;
			
			channel = BaseRobot.get_outbox_channel(order, BaseRobot.OUTBOX_STATE_CHANNEL);
			state = StateMessage.decode(this.myRC.readBroadcast(channel));
			
			if (state.myState == BaseRobot.State.DEFAULT){ //if this robot has no job yet
				assignJob(order); //then give it a job; make this method smart!
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
    
	private void assignJob(int order) throws GameActionException{
		ActionMessage action = new ActionMessage(BaseRobot.State.PASTURE, 0, this.PASTRLocs[order%numPASTR]);
		int channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
		this.myRC.broadcast(channel, action.encode());
	}
    
	protected MapLocation[] find_k_best_pasture_locations(int k){
		MapLocation[] results = new MapLocation[k];
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

			eLoc = eLoc.add(d.opposite(), 2); //back away from wall

			if(stepsToWall > 20){ //if it's far enough away, put it in our list
				results[allocated] = eLoc;
				allocated++;
			}

			if(allocated > numPASTR-1)
				break;
		}

		while(allocated < numPASTR){ //fill in unoccupied slots dumbly
			results[allocated] = myHQLoc.add(dirs[(int)(Math.random()*8)], 15);
			allocated++;
		}
		
		return results;
	}
	
	protected int find_smart_PASTR_number(){
		return 4;
	}

}
