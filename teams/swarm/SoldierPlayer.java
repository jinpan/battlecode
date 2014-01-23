package swarm;

import java.util.LinkedList;

import battlecode.common.*;

public class SoldierPlayer extends BaseRobot {

	MapLocation targetLoc; //the previous target we were assigned to
	MapLocation ourPastrLoc; //the location of the PASTR we're herding, if any
	MapLocation ourNoiseLoc;
	ActionMessage HQMessage;
	MapLocation rallyPasture;
	boolean pastureBuilt = false;
	
	boolean voteRetreat;
	protected int soldier_order;
	LinkedList<MapLocation> curPath = new LinkedList<MapLocation>();

	public SoldierPlayer(RobotController myRC) throws GameActionException {
		super(myRC);

		this.soldier_order = this.myRC.readBroadcast(BaseRobot.SOLDIER_ORDER_CHANNEL);
		this.myRC.broadcast(BaseRobot.SOLDIER_ORDER_CHANNEL, this.soldier_order + 1);
	}

	@Override
	protected void setup() throws GameActionException {
		ActionMessage actionmessage = ActionMessage.decode(this.myRC.readBroadcast(HQ_BROADCAST_CHANNEL));
		if (!actionmessage.equals(HQMessage)) {
			HQMessage = actionmessage;
		}
	}

	@Override
	protected void step() throws GameActionException {
		switch (HQMessage.state) {
		case ATTACK: this.attack_step(); myRC.setIndicatorString(2, "ATTACK"); break;
		case DEFEND: this.defend_step(); myRC.setIndicatorString(2, "DEFEND"); break;
		default: myRC.setIndicatorString(2, "DEFAULT");
		}
	}

	protected void attack_step() throws GameActionException {
		/*ActionMessage pastrDistress= ActionMessage.decode(this.myRC.readBroadcast(PASTR_DISTRESS_CHANNEL));
		
		if (pastrDistress.targetID > 0){
			this.actionQueue.clear();
			this.actionQueue.add(pastrDistress.toAction());
			this.myRC.setIndicatorString(2, "Going to aid distressed pasture");
		}*/
		

		if (!isSafe()) {
			return;
		}

		//handles all attacking actions
		if(respond_to_threat(false)){
			return;
		}

		//if there's nothing we can attack, do a move step
		if ((this.myRC.canSenseSquare(HQMessage.targetLocation) 
				&& this.myRC.senseObjectAtLocation(HQMessage.targetLocation) == null) 
				|| HQMessage.targetLocation.equals(this.enemyHQLoc)) {
			if (rallyPasture != null)
				move_to_target(rallyPasture.add(rallyPasture.directionTo(enemyHQLoc), 5), false);
			else
				move_to_target(myHQLoc.add(myHQLoc.directionTo(myHQLoc), 5), false);
			return;
		} else {
			move_to_target(HQMessage.targetLocation, false);
			//System.out.println(action.targetLocation);
			this.myRC.setIndicatorString(1,  "going to enemy pasture");
		}
	}

	protected void move_to_target(MapLocation target, boolean sneak) throws GameActionException{
		System.out.println("moving");
		if (target.equals(this.myRC.getLocation())){
			return;
		}
		if (target.equals(targetLoc)) {
			System.out.println(curPath.getFirst());
			if (myRC.getLocation().equals(curPath.getFirst())) {
				System.out.println("here");
				curPath.remove();
			} else {
				Direction moveDirection = directionTo(curPath.getFirst());
				System.out.println(moveDirection);
				this.myRC.setIndicatorString(0, moveDirection.toString());
				if (myRC.isActive() && moveDirection != null && canMove(moveDirection)) {
					this.myRC.setIndicatorString(0,  "going to pasture");
					if(!sneak)
						myRC.move(moveDirection);
					else
						myRC.sneak(moveDirection);
				} else if (moveDirection == null){
					curPath = Navigation.pathFind(myRC.getLocation(), target, this);
				}
				this.myRC.yield();
			} 
		} else {
			System.out.println("calculating path " + target);
			curPath = Navigation.pathFind(myRC.getLocation(), target, this);
			myRC.setIndicatorString(1, curPath.toString());
			targetLoc = target;
		}
	}

	protected boolean respond_to_threat(boolean defending) throws GameActionException{
		if(this.myRC.getActionDelay() > 1)
			return false;

		Robot[] nearbyEnemies = this.myRC.senseNearbyGameObjects(Robot.class, 36, this.enemyTeam);
		
		if(nearbyEnemies.length == 0)
			return false;

		MapLocation ourLoc = this.myRC.getLocation();
		int maxHeur = 0;
		if(defending)
			maxHeur = ourLoc.distanceSquaredTo(HQMessage.targetLocation);
		RobotInfo bestRobotInfo = null;
		
		for(int i = 0; i < nearbyEnemies.length; i++){
			RobotInfo cur = this.myRC.senseRobotInfo(nearbyEnemies[i]);
			int thisHeur = getHeuristic(cur, ourLoc);
			if(thisHeur > maxHeur){
				maxHeur = thisHeur;
				bestRobotInfo = cur;
			}
		}

		if(bestRobotInfo != null){
			if(this.myRC.canAttackSquare(bestRobotInfo.location)){
				this.myRC.attackSquare(bestRobotInfo.location);
			}else{
				Direction dir = directionTo(bestRobotInfo.location);
				if(dir != null)
					this.myRC.move(dir);
			}
			return true;
		}
		
		return false;
	}
	
	protected int getHeuristic(RobotInfo r, MapLocation orig){
		int health = (int)r.health;
		int dist = orig.distanceSquaredTo(r.location);
		RobotType type = r.type;
		
		int heuristic = 200;
		if(type == RobotType.PASTR)
			heuristic -= health * 7 / 10;
		else
			heuristic -= health;
		heuristic -= dist * 4;
		
		
		return heuristic;
	}

	protected void defend_step() throws GameActionException {		
		if (this.myRC.isActive()){
			//handles all attacking actions

			if (!isSafe()) {
				return;
			}
			
			MapLocation target = HQMessage.targetLocation;
			if (target != null)
				rallyPasture = target;
			
			boolean atPastr = false;
			if(this.myRC.canSenseSquare(target) && this.myRC.senseObjectAtLocation(target) != null)
				atPastr = true;

			if(respond_to_threat(atPastr)){
				return;
			}

			if (this.myRC.getLocation().equals(target)){
				System.out.println("constructing noisetower");
				this.myRC.construct(RobotType.NOISETOWER);
			}

			if (this.myRC.canSenseSquare(target)){
				GameObject squattingRobot = this.myRC.senseObjectAtLocation(target);
				if (squattingRobot != null && squattingRobot.getTeam() == this.myTeam){
					ourNoiseLoc = target.add(target.directionTo(this.enemyHQLoc), -1);
					target = ourNoiseLoc;

					if(this.myRC.getLocation().equals(ourNoiseLoc) 
							&& this.myRC.senseRobotInfo((Robot)squattingRobot).isConstructing 
							&& this.myRC.senseRobotInfo((Robot)squattingRobot).constructingRounds <= 50) {
						this.myRC.construct(RobotType.PASTR);
						System.out.println("constructing pastr");
					}
				}
			}

			boolean sneak = false;
			if(this.myRC.getLocation().distanceSquaredTo(target) < 16){
				sneak = true;
			}
			move_to_target(target, sneak);
				
		}
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

	protected boolean isSafe() throws GameActionException {
		int retreaters= this.myRC.readBroadcast(SQUAD_RETREAT_CHANNEL);
		
		Robot[] myRobots = this.myRC.senseNearbyGameObjects(Robot.class, 10000000, myTeam);
		Robot[] nearbyRobots = this.myRC.senseNearbyGameObjects(Robot.class, 35, enemyTeam);
		
		int avgx = 0;
		int avgy = 0;
		int enemyCount = 0;
		int enemyHealth = 0;
		
		for (int i = 0; i < nearbyRobots.length; i++) {
			RobotInfo ri = this.myRC.senseRobotInfo(nearbyRobots[i]);
			if (ri.type== RobotType.SOLDIER){
				enemyHealth+= ri.health;
				enemyCount++;
				avgx += ri.location.x;
				avgy += ri.location.y;	
			}
		}
		avgx /= (double) enemyCount;
		avgy /= (double) enemyCount;
		MapLocation com = new MapLocation(avgx, avgy);
		int myRobotCount = 0;
		int myRobotHealth = 0;
		
		for (int i = 0; i < myRobots.length; i++) {
			RobotInfo ri = this.myRC.senseRobotInfo(myRobots[i]);
			if (ri.location.distanceSquaredTo(com) <= 35) {
				myRobotCount++;
				myRobotHealth+= ri.health;
				
			}
		}
		
		//double myHeuristic= myRobotCount*50 + myRobotHealth;
		//double enemyHeuristic= enemyCount*50 + enemyHealth;

		if (myRobotHealth < 0.5*enemyHealth && myRobotCount < 0.5*enemyCount) {
			if (!voteRetreat){
				retreaters++;
				voteRetreat= true;
				this.myRC.broadcast(SQUAD_RETREAT_CHANNEL, retreaters);
			}
			
			int allies = this.myRC.readBroadcast(ALLY_NUMBERS);
			if (retreaters> 0.8*allies){
				if (this.myRC.getHealth() < 25){
					if (this.myRC.getLocation().distanceSquaredTo(com)<= 9){
						this.myRC.selfDestruct(); 
						return false;
					}

					Direction moveDirection = directionTo(com);
					if (myRC.isActive() && moveDirection != null && canMove(moveDirection)) {
						myRC.move(moveDirection);
						return false;
					}
				} else {
					Direction moveDirection = this.myRC.getLocation().directionTo(com.add(com.directionTo(myHQLoc), 10));
					if (myRC.isActive() && moveDirection != null && canMove(moveDirection)) {
						myRC.move(moveDirection);
						return false;
					}
				}
			}
		}
		return true;
	}


}
