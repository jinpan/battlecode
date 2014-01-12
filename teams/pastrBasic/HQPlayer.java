package pastrBasic;

import java.util.ArrayList;
import java.util.HashMap;

import pastrBasic.ActionMessage;
import battlecode.common.*;

public class HQPlayer extends BaseRobot {
	Direction toEnemy;
	MapLocation[] PASTRLocs;

	int numPASTR;
	int currentSquad;
	int[] squadAssignments;
	
	boolean squadsMax = false;
	
	public HQPlayer(RobotController myRC) throws GameActionException {
		super(myRC);

		this.toEnemy = this.myHQLoc.directionTo(this.enemyHQLoc);
		this.currentSquad=0;
		
		numPASTR = find_smart_PASTR_number(); //make this method smart!
		this.PASTRLocs = new MapLocation[numPASTR];
		PASTRLocs = find_k_best_pasture_locations(numPASTR); //make this method smart!
		squadAssignments= new int[BaseRobot.NUM_SQUADS]; //how many robots in each squad
	}

    @Override
    protected void step() throws GameActionException {
		Robot[] nearbyEnemies = this.myRC.senseNearbyGameObjects(Robot.class, 10000, this.enemyTeam);
		
		if (this.myRC.isActive() && nearbyEnemies.length != 0) {
			this.shoot(nearbyEnemies);
		}
    	
		if (this.myRC.isActive() && this.myRC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			this.spawn();
		}
		
		//macrogame code goes here
		//adjustments if we're losing, game is about to end, if we're winning on milk, etc.
		
        int order, channel;
        State state;
        
    	MapLocation[] pastrLocs = this.myRC.sensePastrLocations(this.myTeam);
		MapLocation[] enemyPastrs= this.myRC.sensePastrLocations(this.enemyTeam);
        
        for (Robot robot: this.myRC.senseNearbyGameObjects(Robot.class, 4)){ //for every nearby robot
        	
			order = idToOrder(robot.getID()); 
			if (order == 0)
				continue;
			
        	channel = BaseRobot.get_outbox_channel(order, BaseRobot.OUTBOX_STATE_CHANNEL);
        	state = StateMessage.decode(this.myRC.readBroadcast(channel)).myState;
        	
        	if (state == BaseRobot.State.DEFAULT){ //if robot hasn't been given a job yet
        		if (squadsMax){
        			assignPastureJob(order);
        		} else {
        			if(enemyPastrs.length <= currentSquad)
        				assignScoutJob(order, this.enemyHQLoc);
        			else
        				assignScoutJob(order, enemyPastrs[currentSquad]);
        		}
        	}
        }
        
        //this.broadcastScoutJobs(enemyPastrs);
    }
    
    private boolean spawn() throws GameActionException {
		if (this.myRC.senseObjectAtLocation(this.myHQLoc.add(this.toEnemy)) == null){
			this.myRC.spawn(this.toEnemy);
			return true;
		}
		else {
	        for (Direction dir: BaseRobot.dirs){
	            if (this.myRC.senseObjectAtLocation(this.myHQLoc.add(dir)) == null){
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
    
	private void assignPastureJob(int order) throws GameActionException{
		ActionMessage action = new ActionMessage(BaseRobot.State.PASTURE, 0, this.PASTRLocs[order%numPASTR]);
		int channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
		this.myRC.broadcast(channel, action.encode());
	}
	
	/*
	private void broadcastScoutJobs(MapLocation[] enemypastrs) throws GameActionException{
		for (int squad= 0; squad < BaseRobot.NUM_SQUADS; squad++){
			int channel = BaseRobot.SQUAD_BULLETIN_BASE+ squad;
			int currentJob = this.myRC.readBroadcast(channel);
			int enemyIndex = currentSquad+squad;
			if (squadAssignments[squad]!=0 && currentJob==0 && enemyIndex<enemypastrs.length){
				MapLocation nextEnemy= enemypastrs[enemyIndex];
				MapLocation rallyPoint= nextEnemy.add(nextEnemy.directionTo(this.myHQLoc), BaseRobot.RALLY_DISTANCE);
				ActionMessage msg= new ActionMessage(BaseRobot.State.SCOUT, squad, rallyPoint);
				this.myRC.broadcast(channel, msg.encode());
			}//only broadcasts enemyPastrs that have not already been assigned to other squads
			//If all squads have already been assigned, the squads that have finished their jobs go help other squads
		}
	}
	*/
	
	private void assignScoutJob(int order, MapLocation target) throws GameActionException{
		if (currentSquad< BaseRobot.NUM_SQUADS && squadAssignments[currentSquad]>=BaseRobot.MAX_SQUAD_SIZE){
			currentSquad++;
		}
		if (currentSquad>= BaseRobot.NUM_SQUADS){
			currentSquad = BaseRobot.NUM_SQUADS-1;
			squadsMax = true;
		}
		
		MapLocation rallyPoint = get_rally_point(target);
		ActionMessage action = new ActionMessage(BaseRobot.State.RALLY, currentSquad, rallyPoint); //tell this robot to go to the rally point
		
		//broadcast the squads to messageboard
		//this.myRC.broadcast(BaseRobot.SQUAD_ID_BASE+currentSquad*3+squadAssignments[currentSquad], order);
		squadAssignments[currentSquad]++;
		
		System.out.println("HQ assigning scout to squad number "+ currentSquad);
		
		int channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
		int mainChannel = BaseRobot.SQUAD_BASE + currentSquad * BaseRobot.SQUAD_OFFSET + SQUAD_RALLYPT_CHANNEL;
		
		this.myRC.broadcast(channel, action.encode());
		this.myRC.broadcast(mainChannel, action.encode()); //store rally point in squad channel
	}
	
	private MapLocation get_rally_point(MapLocation target){
		return this.myHQLoc.add(this.myHQLoc.directionTo(target), RALLY_DISTANCE);
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
		return 3;
	}

}
