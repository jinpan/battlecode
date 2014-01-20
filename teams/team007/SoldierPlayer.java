package team007;

import java.util.LinkedList;

import team007.*;
import battlecode.common.*;

public class SoldierPlayer extends BaseRobot {

	MapLocation targetLoc; //the previous target we were assigned to
	MapLocation ourPastrLoc; //the location of the PASTR we're herding, if any
	MapLocation ourNoiseLoc;
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

		if (!isSafe()) {
			return;
		}

		//handles all attacking actions
		if(respond_to_threat()){
			return;
		}

		//if there's nothing we can attack, do a move step
		if ((this.myRC.canSenseSquare(action.targetLocation) && this.myRC.senseObjectAtLocation(action.targetLocation) == null) || action.targetLocation.equals(this.enemyHQLoc)) {
			// target was destroyed
			this.actionQueue.removeFirst();
			if (this.actionQueue.size() > 0){
				this.myState = this.actionQueue.getFirst().state;
				//this.step();
				return;
			}
			else {
				MapLocation[] enemies= this.myRC.sensePastrLocations(this.enemyTeam);
				Action newAction;
				if(enemies.length != 0){
					System.out.println("there");
					newAction = new Action(BaseRobot.State.ATTACK, enemies[0], 0);
				} else {
					System.out.println("here");
					newAction = new Action(BaseRobot.State.ATTACK, this.enemyHQLoc, 0);
				}
				this.actionQueue.addFirst(newAction);
				return;

				//this.myState = BaseRobot.State.DEFAULT;
				//this.step();
				//return;
			}
		} else {
			move_to_target(action.targetLocation, false);
		}
	}

	protected void move_to_target(MapLocation target, boolean sneak) throws GameActionException{
		if (target.equals(this.myRC.getLocation())){
			return;
		}
		if (target.equals(targetLoc)) {
			if (myRC.getLocation().equals(curPath.getFirst())) {
				curPath.remove();
			} else {
				Direction moveDirection = directionTo(curPath.getFirst());
				if (myRC.isActive() && moveDirection != null && canMove(moveDirection)) {
					if(!sneak)
						myRC.move(moveDirection);
					else
						myRC.sneak(moveDirection);
				} else if (moveDirection == null){
					curPath = Navigation.pathFind(myRC.getLocation(), target, this);
				}
				yield();
			} 
		} else {
			curPath = Navigation.pathFind(myRC.getLocation(), target, this);
			myRC.setIndicatorString(1, curPath.toString());
			targetLoc = target;
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
		int minHealth = 101;
		EnemyProfileMessage bestProf = null;
		RobotInfo bestRobotInfo = null;
		int bestInd = 0;

		for (int i=0; i<nearbyEnemies.length; ++i){
			if(nearbyEnemies[i] == null)
				continue;
			for (EnemyProfileMessage enemyProf: soldEnemies){
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
						if(enemyProf.health < minHealth){
							minHealth = enemyProf.health;
							bestProf = enemyProf;
							bestRobotInfo = nearbyInfo[i];
							bestInd = i;
						}

						/*
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
						 */
					}
					nearbyEnemies[i] = null; // set to null so we don't count it twice
				}
			}
		}

		if(!attacked && bestProf != null){
			this.myRC.attackSquare(bestRobotInfo.location);
			bestProf.lastSeenLoc = bestRobotInfo.location;
			bestProf.lastSeenTime = Clock.getRoundNum();
			bestProf.health -= 10;

			long msg;
			if(bestProf.health > 0){
				msg = bestProf.encode();
			} else {
				msg = -1;
			}

			this.squad_send(BaseRobot.SQUAD_SOLD_HITLIST + 2*bestInd, msg);
			attacked = true;
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

		for (int i=0; i<nearbyEnemies.length; ++i){
			for (EnemyProfileMessage enemyProf: bldgEnemies){
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
		//handles all attacking actions
		if(respond_to_threat()){
			return;
		}

		//System.out.println("here, defending");
		if(this.actionQueue.size() > 1){
			this.actionQueue.removeFirst();
		} else {
			Action action = this.actionQueue.getFirst();
			move_to_target(action.targetLocation, false);
		}
	}

	protected void herd_step() throws GameActionException {
		//handles all attacking actions
		if(respond_to_threat()){
			return;
		}

		Action action = this.actionQueue.getFirst();
		if(this.myRC.getLocation().equals(action.targetLocation)){
			this.actionQueue.removeFirst();
			Action newAction = new Action(BaseRobot.State.DEFEND, ourPastrLoc, 0);
			this.actionQueue.addFirst(newAction);
		} else {
			move_to_target(action.targetLocation, true);
		}
	}

	protected void pasturize_step() throws GameActionException {
		if (this.myRC.isActive()){
			//handles all attacking actions
			
			if (!isSafe()) {
				return;
			}
			
			if(respond_to_threat()){
				return;
			}

			Action action = this.actionQueue.getFirst();

			if (this.myRC.getLocation().equals(action.targetLocation)){
				this.myRC.construct(RobotType.PASTR);
				return;
			}

			MapLocation target = action.targetLocation;

			if (this.myRC.canSenseSquare(target)){
				GameObject squattingRobot = this.myRC.senseObjectAtLocation(target);
				if (squattingRobot != null && squattingRobot.getTeam() == this.myTeam){
					ourNoiseLoc = target.add(target.directionTo(this.enemyHQLoc));
					target = ourNoiseLoc;

					if(this.myRC.getLocation().equals(ourNoiseLoc))
						this.myRC.construct(RobotType.NOISETOWER);

					if(this.myRC.canSenseSquare(ourNoiseLoc)){
						GameObject otherRobot = this.myRC.senseObjectAtLocation(ourNoiseLoc);
						if(otherRobot != null && otherRobot.getTeam() == this.myTeam){
							ourPastrLoc = action.targetLocation;
							Action newAction = new Action(BaseRobot.State.DEFEND, action.targetLocation, squattingRobot.getID());
							this.actionQueue.removeFirst();
							this.actionQueue.addFirst(newAction);
							return;
						}	
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

	protected void default_step() throws GameActionException {
		//handles all attacking actions
		if(respond_to_threat()){
			return;
		}

		if(this.myRC.getLocation().distanceSquaredTo(this.myHQLoc) < 5){
			Direction dir = this.myHQLoc.directionTo(this.myRC.getLocation());
			if(this.myRC.isActive() && this.myRC.canMove(dir)){
				this.myRC.move(dir);
			}
		}

		if(this.myRC.getLocation().distanceSquaredTo(this.enemyHQLoc) < 100)
			move_to_target(this.enemyHQLoc, false);

		/*
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
		 */
	}

	/*
	private double cowPotential(MapLocation loc) throws GameActionException {
		double num = 0;

		for (MapLocation otherLoc: MapLocation.getAllMapLocationsWithinRadiusSq(loc, GameConstants.PASTR_RANGE)){
			if (this.myRC.canSenseSquare(otherLoc)){
				num += this.myRC.senseCowsAtLocation(otherLoc);
			}
		}

		return num;
	}
	 */

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

	protected boolean isSafe() throws GameActionException {
		Robot[] myRobots = this.myRC.senseNearbyGameObjects(Robot.class, 10000000, myTeam);
		Robot[] nearbyRobots = this.myRC.senseNearbyGameObjects(Robot.class, 35, enemyTeam);
		int avgx = 0;
		int avgy = 0;
		int enemyCount = 0;
		for (int i = 0; i < nearbyRobots.length; i++) {
			RobotInfo ri = this.myRC.senseRobotInfo(nearbyRobots[i]);
//			if (ri.location.distanceSquaredTo(this.myRC.getLocation()) > 10) {
//				continue;
//			}
			enemyCount++;
			avgx += ri.location.x;
			avgy += ri.location.y;
		}
		avgx /= (double) nearbyRobots.length;
		avgy /= (double) nearbyRobots.length;
		MapLocation com = new MapLocation(avgx, avgy);
		int myRobotCount = 0;
		for (int i = 0; i < myRobots.length; i++) {
			RobotInfo ri = this.myRC.senseRobotInfo(myRobots[i]);
			if (ri.location.distanceSquaredTo(com) <= 35) myRobotCount++;
		}
		if (myRobotCount < enemyCount) {
			Direction moveDirection = directionTo(com);
			if (moveDirection != null) moveDirection = moveDirection.opposite();
			if (myRC.isActive() && moveDirection != null && canMove(moveDirection)) {
				myRC.move(moveDirection);
				return false;
			}
		}
		return true;
	}


}
