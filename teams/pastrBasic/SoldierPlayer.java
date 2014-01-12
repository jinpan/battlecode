package pastrBasic;

import java.util.LinkedList;

import pastrBasic.Action;
import battlecode.common.*;

public class SoldierPlayer extends BaseRobot {

	LinkedList<MapLocation> nav = new LinkedList<MapLocation>();	
	MapLocation targetLoc;
	MapLocation rallyLoc;
	MapLocation pastureloc;
	MapLocation leaderloc;
	MapLocation enemyPastr = this.enemyHQLoc;
	int ourPastrID;
	int squadNumber;
	boolean rallied = false;
	boolean isLeader = false;

	protected static final int DEFENSE_RADIUS= 20;

	public SoldierPlayer(RobotController myRC) throws GameActionException {
		super(myRC);
	}

	@Override
	protected void step() throws GameActionException {

		switch (this.myState) {
		case RALLY: this.rally_step(); return;
		case ATTACK: this.attack_step(); return;
		case ATTACKHIGH: this.attack_step(); return;
		case SCOUTHIGH: this.rally_step(); return;

		case PASTURE: this.pasture_step(); return;
		case DEFENSE: this.defense_step(); return;
		case DEFENSEHIGH: this.defense_step(); return;
		case GATHEROUT: this.gatherout_step(); return;

		default: this.default_step(); return;
		}
	}

	/* protected void checkAttack() throws GameActionException {
    	underAttack= this.myRC.getHealth()<= RobotType.SOLDIER.maxHealth - RobotType.SOLDIER.attackPower;
    }*/

	protected void retreat() throws GameActionException {
		//placeholder
		Action action = new Action(BaseRobot.State.RALLY, rallyLoc, squadNumber);
		this.actionQueue.addFirst(action);
	}

	protected void attack_step() throws GameActionException {
		//Action action = this.actionQueue.getFirst();
		
		ActionMessage action = ActionMessage.decode(this.myRC.readBroadcast(BaseRobot.SQUAD_BASE + this.squadNumber*BaseRobot.SQUAD_OFFSET + SQUAD_ATTACKPT_CHANNEL)); //get our attack assignment
		
		if(!enemyPastr.equals(action.targetLocation))
			nav = pathFind(this.myRC.getLocation(), action.targetLocation);
		/*
		if(isLeader){
			if(!enemyPastr.equals(action.targetLocation))
				nav = pathFind(this.myRC.getLocation(), action.targetLocation);	
		} else {
			leaderloc = ActionMessage.decode(this.myRC.readBroadcast(BaseRobot.SQUAD_BASE + this.squadNumber*BaseRobot.SQUAD_OFFSET + SQUAD_LEADER_CHANNEL)).targetLocation;
			if(Clock.getRoundNum() % 10 == 0)
				nav = pathFind(this.myRC.getLocation(), leaderloc);
		}
		*/
		enemyPastr = action.targetLocation;

		//TODO: figure out when to retreat
		//TODO: check_HQ_announce(); //check for distress signals from the HQ

		ThreatMessage[] threats = new ThreatMessage[BaseRobot.MAX_THREAT_NUM];
		ThreatMessage curThreat;
		int baseChannel = BaseRobot.SQUAD_BASE + this.squadNumber*BaseRobot.SQUAD_OFFSET + SQUAD_THREAT_BASE;
		int curRound = Clock.getRoundNum();

		int threatCount = 0;

		//look at threats on the message board
		for(int i = 0; i < BaseRobot.MAX_THREAT_NUM; i++){
			curThreat = ThreatMessage.decode(this.myRC.readBroadcast(baseChannel+i));
			if((curRound - curThreat.roundNum +10000)%100 > 4){ //if the message is old, remove it
				this.myRC.broadcast(baseChannel+i, 0);
			} else {
				threats[i] = curThreat;
				threatCount++;
			}
		}
		//System.out.println("found " + threatCount + "threats");

		//look at enemy robots we can see, and put them on the threat list
		Robot[] nearbyEnemies = this.myRC.senseNearbyGameObjects(Robot.class, 10000, this.enemyTeam);
		for(Robot r : nearbyEnemies){
			if(this.myRC.senseRobotInfo(r).type == RobotType.HQ || this.myRC.senseRobotInfo(r).type == RobotType.PASTR)
				continue;

			int match = -1;

			for(int i = 0; i < BaseRobot.MAX_THREAT_NUM; i++){
				if(threats[i] != null && threats[i].targetID == r.getID()){
					match = i;
					break;
				}
			}

			if(match == -1){
				//put this new threat on the threat list
				threatCount++;
				for(int i = 0; i < BaseRobot.MAX_THREAT_NUM; i++){
					if(threats[i] == null){ //if there was no real message at this location, put our message here
						ThreatMessage newMessage = new ThreatMessage(curRound, r.getID(), this.myRC.senseRobotInfo(r).location);
						threats[i] = newMessage;
						this.myRC.broadcast(baseChannel+i, newMessage.encode());
						break;
					}
				}
			} else {
				//update position and round number on the threat list
				ThreatMessage updatedMessage = new ThreatMessage(curRound, r.getID(), this.myRC.senseRobotInfo(r).location);
				threats[match] = updatedMessage;
				this.myRC.broadcast(baseChannel+match, updatedMessage.encode());
			}
		}

		if(threatCount != 0){
			skirmish_step(enemyPastr, threats); //figure out which threat we want to address, then attack
		} else if(enemyPastr.equals(this.enemyHQLoc)){
			idle_step(); //try to find a better goal
		} else {
			main_attack_step(enemyPastr); //destroy our assigned PASTR
		}
		
		/*if(isLeader){
			ActionMessage newAction = new ActionMessage(State.ATTACK, 0, this.myRC.getLocation()); //leader transmits where it is
			this.myRC.broadcast(BaseRobot.SQUAD_BASE + this.squadNumber*BaseRobot.SQUAD_OFFSET + SQUAD_LEADER_CHANNEL, newAction.encode());
		}*/

	}

	protected void skirmish_step(MapLocation enemyPastr, ThreatMessage[] threats) throws GameActionException{ //deal with a small threat
		MapLocation bestThreat = null; //below, we decide which threat we should address
		int minID = 9999;

		//attack the robot with the lowest ID
		for(int i = 0; i < BaseRobot.MAX_THREAT_NUM; i++){
			if(threats[i] == null)
				continue;
			if(threats[i].targetID < minID){
				bestThreat = threats[i].targetLocation;
				minID = threats[i].targetID;
			}
		}

		if(bestThreat != null){
			if(this.myRC.isActive()){
				if (this.myRC.getLocation().distanceSquaredTo(bestThreat)<10){
					this.myRC.attackSquare(bestThreat);
				} else if (directionTo(bestThreat)!= null){
					this.myRC.move(directionTo(bestThreat));
				}
			}
		} else {
			System.out.println("shouldn't be here");
		}

	}

	protected void idle_step() throws GameActionException{ //there are no surviving enemy PASTRs
		System.out.println("trying to get new target");
		if((this.myRC.getRobot().getID() + Clock.getRoundNum()) % 5 == 0){
			MapLocation newGoal = get_next_attack_loc(); //try to get a new good target
			if(!newGoal.equals(this.enemyHQLoc)){
				System.out.println("made new target at " + newGoal.x + " " + newGoal.y + " and broadcasting to " + (BaseRobot.SQUAD_BASE + this.squadNumber*BaseRobot.SQUAD_OFFSET + SQUAD_ATTACKPT_CHANNEL));
				ActionMessage attackAction = new ActionMessage(BaseRobot.State.ATTACK, 0, newGoal);
				this.myRC.broadcast(BaseRobot.SQUAD_BASE + this.squadNumber*BaseRobot.SQUAD_OFFSET + SQUAD_ATTACKPT_CHANNEL, attackAction.encode());    
			}
		}
	}

	protected void main_attack_step(MapLocation target) throws GameActionException{ //move towards / attack our main goal, an enemy PASTR
		if (this.myRC.canSenseSquare(target)) { //sense if the pasture is destroyed
			GameObject onSquare = this.myRC.senseObjectAtLocation(target);
			if (onSquare==null) {
				System.out.println("pasture is dead");
				MapLocation nextTarget = get_next_attack_loc();
				int channel = BaseRobot.SQUAD_BASE + this.squadNumber*BaseRobot.SQUAD_OFFSET + SQUAD_ATTACKPT_CHANNEL;
				ActionMessage attackAction = new ActionMessage(BaseRobot.State.ATTACK, 0, nextTarget);
				this.myRC.broadcast(channel, attackAction.encode());
			}
		}

		if(this.myRC.isActive()){
			if (this.myRC.getLocation().distanceSquaredTo(target)<10){
				this.myRC.attackSquare(target);
			} else if (true){
				if (isLeader) System.out.println("Checking list. Currently at " + myRC.getLocation() + " going to " + nav.getFirst());
				if(this.myRC.getLocation().equals(nav.getFirst())) {
					nav.remove();
				}
				else{
					this.myRC.setIndicatorString(0, nav.toString());
					
					MapLocation nextLoc;
					if(isLeader){
						nextLoc = nav.getFirst();
					} else {
						nextLoc = nav.getFirst();
					}
					
					Direction moveDirection = myRC.getLocation().directionTo(nextLoc);
//					if (!canTravel(myRC.getLocation(), nav.getFirst())) {
//						nav = pathFind(myRC.getLocation(), nav.getFirst());
//					}
					if (myRC.isActive() && myRC.canMove(moveDirection)) {
						myRC.move(moveDirection);
					} 
				}
			}
		}
	}

	protected void defense_step() throws GameActionException {
		if(this.myRC.isActive()){
			Action action = this.actionQueue.getFirst();
			targetLoc = action.targetLocation;

			Direction dir = this.directionTo(targetLoc);
			if (dir != null)
				this.myRC.move(dir);

			//flesh out later

			if(this.myRC.getLocation().distanceSquaredTo(targetLoc) < 5 && this.actionQueue.size() > 1)
				this.actionQueue.removeFirst();
		}
	}

	protected void pasture_step() throws GameActionException {
		//try to make a pasture at some location
		//if a pasture is already there, become one of its herders
		if (this.myRC.isActive()){
			Action action = this.actionQueue.getFirst();

			if (this.myRC.getLocation().equals(action.targetLocation)){
				this.myRC.construct(RobotType.PASTR);
				return;
			}
			if (this.myRC.canSenseSquare(action.targetLocation)){
				GameObject squattingRobot = this.myRC.senseObjectAtLocation(action.targetLocation); //if one of our PASTRs is already there
				if (squattingRobot != null && squattingRobot.getTeam() == this.myTeam && this.myRC.senseRobotInfo((Robot)squattingRobot).type == RobotType.PASTR){
					ourPastrID = squattingRobot.getID(); //gets and stores the PASTR id
					pastureloc = action.targetLocation;
					Action newAction = new Action(BaseRobot.State.DEFENSE, pastureloc, ourPastrID);
					this.actionQueue.removeFirst();
					this.actionQueue.addFirst(newAction);
					return;
				}
			}
			Direction dir = this.directionTo(action.targetLocation);
			if (dir == null){
				System.out.println("with new navigation, this should not happen");
				this.myRC.construct(RobotType.PASTR);
				return;
			}
			else {
				if(action.targetLocation.distanceSquaredTo(this.myRC.getLocation()) < 16)
					this.myRC.sneak(dir);
				else 
					this.myRC.move(dir);
				return;
			}
		}
	}    

	protected void rally_step() throws GameActionException {
		// scout in squads; HQ assigns all scouts to the same pastr and rallying point. 
		// Once at rallying point, if it senses other robots of its team in the vicinity, it waits.
		// Otherwise it goes into attack mode.

		Action action = this.actionQueue.getFirst();
		rallyLoc = action.targetLocation;
		squadNumber = action.targetID;

		if (this.myRC.getLocation().distanceSquaredTo(rallyLoc)<9){ //if we're near the rally point
			int numChannel = BaseRobot.SQUAD_BASE + squadNumber*BaseRobot.SQUAD_OFFSET + SQUAD_RALLYAMT_CHANNEL; //how many robots are already at the rally point    		

			//increment the count of robots already here
			int membersThere= this.myRC.readBroadcast(numChannel);
			if (!rallied){
				System.out.println("Scout arrived, squad number " + squadNumber + ", " + membersThere + " members there too.");
				membersThere++;
				rallied= true;
			}
			
			if(membersThere == 1){
				isLeader = true;
			}

			this.myRC.broadcast(numChannel, membersThere);

			if (membersThere >= MAX_SQUAD_SIZE || membersThere < 0){ //if the whole squad is there, or people have already left
				int targetChannel = SQUAD_BASE + this.squadNumber*BaseRobot.SQUAD_OFFSET + SQUAD_ATTACKPT_CHANNEL;
				int theTarget = this.myRC.readBroadcast(targetChannel);

				MapLocation nextTarget;

				if(theTarget == 0){ //if this squad doesn't have a target, assign one
					nextTarget = get_next_attack_loc();
					ActionMessage attackAction = new ActionMessage(BaseRobot.State.ATTACK, 0, nextTarget);
					this.myRC.broadcast(targetChannel, attackAction.encode());
				} else {
					nextTarget = ActionMessage.decode(theTarget).targetLocation;
				}

				Action newAction= new Action(BaseRobot.State.ATTACK, nextTarget, this.squadNumber);
				this.actionQueue.removeFirst();
				this.actionQueue.addFirst(newAction);

				//announce that we've left
				this.myRC.broadcast(numChannel, -10);
			}
		} 

		Direction dir = directionTo(rallyLoc);
		if(this.myRC.isActive() && dir != null){
			this.myRC.move(dir);
		}	
	}

	protected MapLocation get_next_attack_loc() throws GameActionException{
		//placeholder! make this much smarter later
		MapLocation[] pastrs = this.myRC.sensePastrLocations(this.enemyTeam);

		if(pastrs.length == 0){
			return this.enemyHQLoc; //if no pastures, patrol the enemy HQ
		} else{
			int mindist = 1000000;
			int ind = 0;
			boolean seenOpenPastr = false;
			for(int i = 0; i < pastrs.length; i++){
				if(pastrs[i].distanceSquaredTo(this.enemyHQLoc) < 25)
					continue;
				if(this.myRC.getLocation().distanceSquaredTo(pastrs[i]) < mindist){
					ind = i;
					mindist = this.myRC.getLocation().distanceSquaredTo(pastrs[i]);
					seenOpenPastr = true;
				}
			}

			if(seenOpenPastr)
				return pastrs[ind]; //go to the closest pasture
			else
				return this.enemyHQLoc; //if all pastures next to enemy HQ, patrol the enemy HQ
		}
	}

	protected boolean loneRanger() throws GameActionException { //nobody is nearby
		return this.myRC.senseNearbyGameObjects(Robot.class, 20, this.myTeam).length==0;
	}

	protected void gatherout_step() throws GameActionException{
		if(this.myRC.isActive()){
			Action action = this.actionQueue.getFirst();
			MapLocation target = action.targetLocation;

			if(this.myRC.getLocation().distanceSquaredTo(target) < 3){
				this.actionQueue.removeFirst();
				Action newAction = new Action(BaseRobot.State.DEFENSE, targetLoc, ourPastrID);
				this.actionQueue.addFirst(newAction);
			} else {
				Direction dir = this.directionTo(target);
				if (dir != null)
					this.myRC.sneak(dir);
			}
		}
	}

	protected void default_step() throws GameActionException {
		//stop doing nothing if told to do something else
		if(this.actionQueue.size() > 0)
			this.actionQueue.remove(0);
	}

	protected Direction directionTo(MapLocation loc) throws GameActionException {
		Direction dir = this.myRC.getLocation().directionTo(loc);

		if(dir == Direction.NONE || dir == Direction.OMNI){
			return null;
		}

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


