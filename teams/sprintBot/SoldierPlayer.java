package sprintBot;

import java.util.LinkedList;
import sprintBot.*;
import battlecode.common.*;

public class SoldierPlayer extends BaseRobot {
    
	MapLocation targetLoc;
	protected int soldier_order;
	LinkedList<MapLocation> curPath = new LinkedList<MapLocation>();
	

    public SoldierPlayer(RobotController myRC) throws GameActionException {
        super(myRC);

        this.soldier_order = this.myRC.readBroadcast(BaseRobot.SOLDIER_ORDER_CHANNEL);
        this.myRC.broadcast(BaseRobot.SOLDIER_ORDER_CHANNEL, this.soldier_order + 1);
    }
    
    @Override
    protected void setup() throws GameActionException {
    	super.setup();
    	
    	if (this.newAction != null){
    		this.actionQueue.addFirst(this.newAction);
    		this.myState = this.newAction.state;
    		this.newAction = null;
    		System.out.println("NOT NULL");
    		System.out.println(this.myState);
    	}
    }

    @Override
    protected void step() throws GameActionException {
    	
    	this.myRC.setIndicatorString(2, this.myState.name());
       
        switch (this.myState) {
            case ATTACK: this.attack_step(); break;
            case DEFEND: this.defend_step(); break;
            case HERD: this.herd_step(); break;
            case PASTURIZE: this.pasturize_step(); break;
            
            default: this.default_step(); return;
        }
    }
    
    protected void attack_step() throws GameActionException {
    	Action action = this.actionQueue.getFirst();
		this.myRC.setIndicatorString(3, String.valueOf(this.myRC.getActionDelay()));
    	this.myRC.setIndicatorString(4, action.targetLocation.toString());
    	
    	//handles all attacking actions
    	if(respond_to_threat()){
    		return;
    	}
    	
    	//if there's nothing we can attack, do a move step
    	if (this.myRC.canSenseSquare(action.targetLocation) && this.myRC.senseObjectAtLocation(action.targetLocation) == null) {
			// target was destroyed
			this.actionQueue.removeFirst();
			if (this.actionQueue.size() > 0){
				this.myState = this.actionQueue.getFirst().state;
				this.step();
				return;
			}
			else {
				this.myState = State.DEFAULT;
				this.step();
				return;
			}
		}
    	else {
    		Direction dir = this.myRC.getLocation().directionTo(action.targetLocation);
    		if (action.targetLocation.equals(targetLoc)) {
    			if (myRC.getLocation().equals(curPath.getFirst())) {
            		curPath.remove();
            	} else {
            		Direction moveDirection = myRC.getLocation().directionTo(curPath.getFirst());
            		if (myRC.isActive() && myRC.canMove(moveDirection)) {
            			myRC.move(moveDirection);
            		} 
        			yield();
            	} 
    		} else {
    			curPath = pathFind(myRC.getLocation(), action.targetLocation);
    			targetLoc = action.targetLocation;
    		}
//			for (int i=0; i<7; ++i) {
//				if (this.canMove(dir) && this.myRC.getActionDelay() < 1){
//					
//					this.move(dir);
//					return;
//				}
//				else {
//					dir = dir.rotateRight();
//				}
//			}
    	}
    }
    
    protected boolean respond_to_threat() throws GameActionException{
    	LinkedList<EnemyProfileMessage> soldEnemies = this.getEnemies(0);
    	LinkedList<EnemyProfileMessage> bldgEnemies = this.getEnemies(1);   	
    	
    	int soldCounter = soldEnemies.size();
    	int bldgCounter = bldgEnemies.size();
    	
    	boolean attacked = (this.myRC.getActionDelay() > 1);
    	
    	Robot[] nearbyEnemies = this.myRC.senseNearbyGameObjects(Robot.class, 10, this.enemyTeam);
    	RobotInfo[] nearbyInfo = new RobotInfo[nearbyEnemies.length];
    	for (int i=0; i<nearbyEnemies.length; ++i){
    		nearbyInfo[i] = this.myRC.senseRobotInfo(nearbyEnemies[i]);
    	}
    	
    	//attack the first thing in range that we can
    	for (EnemyProfileMessage enemyProf: soldEnemies){
    		for (int i=0; i<nearbyEnemies.length; ++i){
    			if (nearbyEnemies[i] != null && nearbyEnemies[i].getID() == enemyProf.id){
    				if (attacked || this.myRC.getActionDelay() > 1){
    					//update location and last time we saw it
    					if (nearbyInfo[i].location != enemyProf.lastSeenLoc){
    						enemyProf.lastSeenLoc = nearbyInfo[i].location;
    						enemyProf.lastSeenTime = Clock.getRoundNum();
    						long msg = enemyProf.encode();
    						this.squad_send(BaseRobot.SQUAD_SOLD_HITLIST + 2*i, msg);
    					}
    				}
    				else {
    					//attack it and then update information
	    				this.myRC.attackSquare(nearbyInfo[i].location);
	    				attacked = true;
	    				
	    				enemyProf.lastSeenLoc = nearbyInfo[i].location;
	    				enemyProf.lastSeenTime = Clock.getRoundNum();
	    				enemyProf.health -= 10;
	    				
	    				long msg;
	    				if (enemyProf.health > 0){
		    				msg = enemyProf.encode();
	    				}
	    				else {
	    					msg = -1;
	    				}
	    				
	    				this.squad_send(BaseRobot.SQUAD_SOLD_HITLIST + 2*i, msg);
    				}
    				nearbyEnemies[i] = null; // set to null so we don't count it twice
    			}
    		}
    	}

    	for (int i=0; i<nearbyEnemies.length; ++i){
    		if (nearbyEnemies[i] != null){
				int health = (int) nearbyInfo[i].health;
    			if (nearbyInfo[i].type == RobotType.SOLDIER){
    				if (!attacked && this.myRC.getActionDelay() < 1){
    					this.myRC.attackSquare(nearbyInfo[i].location);
    					attacked = true;
    					health -= 10;
    				}
    				EnemyProfileMessage enemyProf = new EnemyProfileMessage(nearbyEnemies[i].getID(), health, nearbyInfo[i].location, Clock.getRoundNum());
    				if (health <= 0){
    					// they're dead!
    				}
    				else {
    					this.squad_send(BaseRobot.SQUAD_SOLD_HITLIST + 2 * soldCounter, enemyProf.encode());
    					++soldCounter;
    				}
    				nearbyEnemies[i] = null;
    			}
    		}
    	}
    	
    	for (EnemyProfileMessage enemyProf: bldgEnemies){
    		for (int i=0; i<nearbyEnemies.length; ++i){
    			if (nearbyEnemies[i] != null){
    				if (!attacked && this.myRC.getActionDelay() < 1) {
	    				this.myRC.attackSquare(nearbyInfo[i].location);
	    				attacked = true;
	    				enemyProf.health -= 10;
	    			
	    				long msg;
	    				if (enemyProf.health > 0){
		    				msg = enemyProf.encode();
	    				}
	    				else {
	    					msg = -1;
	    				}
	    				
	    				this.squad_send(BaseRobot.SQUAD_BLDG_HITLIST + 2 * bldgCounter, msg);
	    				++bldgCounter;
    				}
    				nearbyEnemies[i] = null; // set to null so we don't count it twice
    			}
    		}
    	}
    	
    	return attacked;
    }
    
    protected void defend_step() throws GameActionException {
        // I'm gonna defend the pasture wheee go me
    }
    
    protected void herd_step() throws GameActionException {
        // I'm gonna scout me some enemies
        
    }
    
    protected void pasturize_step() throws GameActionException {
    	if (this.myRC.isActive()){
	    	Action action = this.actionQueue.getFirst();
	    	
	    	if (this.myRC.getLocation().equals(action.targetLocation)){
	    		this.myRC.construct(RobotType.PASTR);
	    		return;
	    	}
	    	if (this.myRC.canSenseSquare(action.targetLocation)){
	    		Direction dir = this.myRC.getLocation().directionTo(action.targetLocation);
				for (int i=0; i<7; ++i) {
					if (this.canMove(dir) && this.myRC.getActionDelay() < 1){
						
						this.sneak(dir);
						return;
					}
					else {
						dir = dir.rotateRight();
					}
				}
	    	}
    	}
    }    
    
    protected void default_step() throws GameActionException {
    	//handles all attacking actions
    	if(respond_to_threat()){
    		return;
    	}
    	
    	MapLocation bestLoc = this.myRC.getLocation();
		int buildingpastrs = this.myRC.readBroadcast(BaseRobot.PASTR_BUILDING_CHANNEL);
    	if (this.myRC.getLocation().distanceSquaredTo(this.myHQLoc) > 400 && this.myRC.senseNearbyGameObjects(Robot.class, 10, this.myTeam).length > 3 ) {
    		if (this.myRC.readBroadcast(BaseRobot.PASTR_BUILDING_CHANNEL) < 5){
    			double bestCows = this.myRC.senseCowsAtLocation(this.myRC.getLocation());
    			for (MapLocation loc: MapLocation.getAllMapLocationsWithinRadiusSq(this.myRC.getLocation(), 25)){
    				if (cowPotential(loc) > bestCows){
    					bestLoc = loc;
    					bestCows = cowPotential(loc);
    				}
    			}
    			this.myRC.broadcast(BaseRobot.PASTR_BUILDING_CHANNEL, buildingpastrs + 1);
    	    	this.newAction = new Action(BaseRobot.State.PASTURIZE, bestLoc, 0);
    		}
    	}
    }
    
    private double cowPotential(MapLocation loc) throws GameActionException {
    	double num = 0;
    	
    	for (MapLocation otherLoc: MapLocation.getAllMapLocationsWithinRadiusSq(loc, GameConstants.PASTR_RANGE)){
    		if (this.myRC.canSenseSquare(otherLoc)){
    			num += this.myRC.senseCowsAtLocation(otherLoc);
    		}
    	}
    	
    	return num;
    }
    
    private LinkedList<EnemyProfileMessage> getEnemies(int priority) throws GameActionException {
    	LinkedList<EnemyProfileMessage> result = new LinkedList<EnemyProfileMessage>();
    	
    	int channel = 0;
    	switch (priority){
	    	case 0: channel = BaseRobot.SQUAD_SOLD_HITLIST; break;
	    	case 1: channel = BaseRobot.SQUAD_BLDG_HITLIST; break;
    	}
    	
    	for (int i=0; i<ENEMY_MEMORY_LEN*2; i+=2){
    		int msg1 = this.myRC.readBroadcast(BaseRobot.SQUAD_BASE + channel + i);
    		int msg2 = this.myRC.readBroadcast(BaseRobot.SQUAD_BASE + channel + i + 1);
    		long msg = msg1; msg <<= 32; msg |= msg2 & 0xFFFFFFFFL;
    		if (msg == 0){
    			break;
    		}
    		else if (msg == -1){
    			continue;
    		}
    		else {
    			result.add(EnemyProfileMessage.decode(msg));
    		}
    	}
    	return result;
    }
    
    protected Direction directionTo(MapLocation loc) throws GameActionException {
        Direction dir = this.myRC.getLocation().directionTo(loc);
        
        if (this.myRC.canMove(dir)){
            return dir;
        }
        
        Direction dirA, dirB;
        if (this.random() < 0.5){
            dirA = dir.rotateLeft();
            dirB = dir.rotateRight();
        }
        else {
            dirA = dir.rotateRight();
            dirB = dir.rotateLeft();
        }
        
        if (this.myRC.canMove(dirA)){
            return dirA;
        }
        else if (this.myRC.canMove(dirB)){
            return dirB;
        }
        
        return null;        
    }
    
    private SearchNode bugSearch(MapLocation start, MapLocation target) throws GameActionException{
		boolean mline[][] = new boolean[100][100];
		MapLocation ehqloc = target;
		MapLocation curr = start;
		// optimize, kinda slow right now. -> lol no it's good enough.
		// Initialize mline array for easy lookup later
		while (!(curr.x == ehqloc.x && curr.y == ehqloc.y)) {
			mline[curr.x][curr.y] = true;
			curr = curr.add(curr.directionTo(ehqloc));
		}
		// Actual bug iteration
		SearchNode current = new SearchNode(start, 1, null, this);
		current.isPivot = true;
		Direction curDir = current.loc.directionTo(ehqloc);
		while (current.loc.x != ehqloc.x || current.loc.y != ehqloc.y) {
			// If on the m-line, and can move forward, move forward towards the enemy HQ.
			boolean canMoveForward = (this.myRC.senseTerrainTile(current.loc.add(curDir)) == TerrainTile.NORMAL ||
					this.myRC.senseTerrainTile(current.loc.add(curDir)) == TerrainTile.ROAD);
			if (mline[current.loc.x][current.loc.y] && (this.myRC.senseTerrainTile(current.loc.add(current.loc.directionTo(ehqloc))) == TerrainTile.NORMAL ||
					this.myRC.senseTerrainTile(current.loc.add(current.loc.directionTo(ehqloc))) == TerrainTile.ROAD)) {
				curDir = current.loc.directionTo(ehqloc);
				current = new SearchNode(current.loc.add(curDir), current.length+1, current, this);
			}
			// If can move forward, and right hand touching wall, move forward
			else if (canMoveForward && this.myRC.senseTerrainTile(current.loc.add(curDir.rotateRight().rotateRight())) == TerrainTile.VOID) {
				current = new SearchNode(current.loc.add(curDir), current.length + 1, current, this);
			}
			// If right hand side is empty, turn right and move forward
			else if (canMoveForward && (this.myRC.senseTerrainTile(current.loc.add(curDir.rotateRight().rotateRight())) == TerrainTile.NORMAL ||
					this.myRC.senseTerrainTile(current.loc.add(curDir.rotateRight().rotateRight())) == TerrainTile.ROAD)) {
				curDir = curDir.rotateRight().rotateRight();
				current.isPivot = true;
				current = new SearchNode(current.loc.add(curDir), current.length + 1, current, this);
			}
			// Only condition for this else should be that the robot cannot move forward and has a wall on the right. Therefore just turn left and move. Report corner.
			else {
				curDir = curDir.rotateLeft();
				if (!(this.myRC.senseTerrainTile(current.loc.add(curDir)) == TerrainTile.NORMAL ||
						this.myRC.senseTerrainTile(current.loc.add(curDir)) == TerrainTile.ROAD)) {
					curDir = curDir.rotateLeft();
				}
				if (!(this.myRC.senseTerrainTile(current.loc.add(curDir)) == TerrainTile.NORMAL ||
						this.myRC.senseTerrainTile(current.loc.add(curDir)) == TerrainTile.ROAD)) {
					curDir = curDir.rotateLeft();
				}
				if (this.myRC.senseTerrainTile(current.loc.add(curDir.rotateRight().rotateRight().rotateRight().rotateRight())) == TerrainTile.VOID) {
					//System.out.println("Corner found at " + current.loc);
				}
				current = new SearchNode(current.loc.add(curDir), current.length + 1, current, this);
			}
		}
		current.isPivot = true;
		return current;
	}
	private LinkedList<MapLocation> pathFind(MapLocation start, MapLocation target) throws GameActionException {
		SearchNode bugSearch = bugSearch(start, target);
		SearchNode[] nodes = new SearchNode[bugSearch.length];
		int counter = bugSearch.length-1;
		while (!bugSearch.loc.equals(start)) {
			nodes[counter] = bugSearch;
			bugSearch = bugSearch.prevLoc;
			counter--;
		}
		nodes[0] = bugSearch;
		LinkedList<MapLocation> pivots = new LinkedList<MapLocation>();
		pivots.add(nodes[0].loc);
		for (int i = 1; i < nodes.length; i++) {
			if (nodes[i].isPivot) {
				pivots.add(nodes[i].loc);
			}
		}
		// Get rid of some pivots cause we can. MUCH IMPROVEMENT. SUCH WOW. :')
		MapLocation pivotArray[] = new MapLocation[pivots.size()];
		int temp = 0;
		for (MapLocation pivot: pivots) {
			pivotArray[temp] = pivot;
			temp++;
		}
		int first = 0;
		LinkedList<MapLocation> finalList = new LinkedList<MapLocation>();
		finalList.add(pivotArray[first]);
		for (int i = pivots.size()-1; i > first; i--) {
			if (first == pivots.size()-1) {
				break;
			}
			if (canTravel(pivotArray[first], pivotArray[i])) {
				finalList.add(pivotArray[i]);
				first = i;
				i = pivots.size();
			} else if (i - first == 1) {
				LinkedList<MapLocation> newpath = pathFind(pivotArray[first], pivotArray[i]);
				newpath.remove();
				finalList.addAll(newpath);
				first = i;
				i = pivots.size();
			}
		}
		return finalList;
	}
	public boolean canTravel(MapLocation start, MapLocation target) {
		while (!(start.x == target.x && start.y == target.y)) {
			if (this.myRC.senseTerrainTile(start) == TerrainTile.VOID) return false;
			start = start.add(start.directionTo(target));
		}
		return true;
	}

}
