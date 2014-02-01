package defensePlus;

import java.util.LinkedList;

import battlecode.common.*;

public class SoldierPlayer extends BaseRobot {
	MapLocation targetLoc;
	MapLocation ourPastrLoc;
	MapLocation ourNoiseLoc;
	ActionMessage HQMessage;

	MapLocation myCOM;
	MapLocation enemyCOM;
	int myRobotCount = 0;
	int myRobotHealth = 0;
	int nearAllyCount = 0;

	int enemyCount = 0;
	int enemyHealth = 0;
	int nearEnemyCount = 0;

	int hittableEnemyCount = 0;
	int hittableAllyCount = 0;

	int reinforcementReq;
	int timer = 250;

	Navigation navigator;

	boolean noiseTowerOffense = false;
	MapLocation dispNoiseLoc = null;

	boolean voteRetreat;
	boolean straightMovement;
	int chilledTurns;
	protected int soldier_order;
	Robot[] allies, enemies;
	LinkedList<MapLocation> curPath = new LinkedList<MapLocation>();

	public SoldierPlayer(RobotController myRC) throws GameActionException {
		super(myRC);

		this.soldier_order = this.myRC.readBroadcast(BaseRobot.SOLDIER_ORDER_CHANNEL);
		this.myRC.broadcast(BaseRobot.SOLDIER_ORDER_CHANNEL, this.soldier_order + 1);

		int mapSize = this.myRC.getMapWidth() * this.myRC.getMapHeight();
		if (mapSize > 1600){
			this.reinforcementReq = 5;
		}
		else if (mapSize > 900){
			this.reinforcementReq = 7;
		}
		else {
			this.reinforcementReq = 9;
		}
		this.navigator = new Navigation(this);

	}

	@Override
	protected void setup() throws GameActionException {
		HQMessage = ActionMessage.decode(this.myRC.readBroadcast(HQ_BROADCAST_CHANNEL));
		ourPastrLoc = ActionMessage.decode(this.myRC.readBroadcast(PASTR_LOC_CHANNEL)).targetLocation;
		ourNoiseLoc = ActionMessage.decode(this.myRC.readBroadcast(NOISE_LOC_CHANNEL)).targetLocation;

		enemies = this.myRC.senseNearbyGameObjects(Robot.class, 35, this.enemyTeam);
		allies = this.myRC.senseNearbyGameObjects(Robot.class, 35, myTeam);
	}

	@Override
	protected void step() throws GameActionException {
		if(this.myRC.isActive()){
			switch (HQMessage.state) {
			case ATTACK: this.attack_step(); myRC.setIndicatorString(2, "ATTACK"); break;
			case DEFEND: this.defend_step(); myRC.setIndicatorString(2, "DEFEND"); break;
			case WAIT: this.rally_step(); myRC.setIndicatorString(2, "WAIT"); break;
			default: myRC.setIndicatorString(2, "DEFAULT");
			}
		}
	}

	protected void attack_step() throws GameActionException {
		if (!isSafe()) { return; }
		if (respond_to_threat(2)){ return; }

		if ((this.myRC.canSenseSquare(HQMessage.targetLocation) && this.myRC.senseObjectAtLocation(HQMessage.targetLocation) == null)) {
			move_to_target(ourPastrLoc, false);
			return;
		} else {
			if (myCOM!= null) {
				if (myCOM.distanceSquaredTo(HQMessage.targetLocation) 
						<= this.myRC.getLocation().distanceSquaredTo(HQMessage.targetLocation)-200) {
					Direction dir = directionTo(myCOM);
					if (this.myRC.isActive() && dir!=null && canMove(dir)) {
						this.myRC.move(dir);
						//System.out.println("moving to com on the way to target");
						return;
					}
				}
			}
			move_to_target(HQMessage.targetLocation, false);
			return;
		}
	}

	protected void move_to_target(MapLocation target, boolean sneak) throws GameActionException{
		if (target.equals(this.myRC.getLocation())){
			return;
		}
		if (target.equals(targetLoc)) {
			if (myRC.getLocation().equals(curPath.getFirst())) {
				curPath.remove();
				straightMovement = false;
			} else {
				Direction moveDirection = directionTo(curPath.getFirst());
				if (straightMovement && moveDirection == null) {
					moveDirection = directionTo(target);
				}
				if (myRC.isActive() && moveDirection != null && canMove(moveDirection)) {
					chilledTurns = 0;
					if(!sneak)
						myRC.move(moveDirection);
					else
						myRC.sneak(moveDirection);
				} else if (moveDirection == null){
					if (chilledTurns >= 3) {
						chilledTurns = 0;
						myRC.setIndicatorString(1, target.toString());
						LinkedList<MapLocation> newCurPath = navigator.pathFind(myRC.getLocation(), target);
						newCurPath.remove();
						if (!curPath.equals(newCurPath)) {
							curPath = newCurPath;
						} else {
							straightMovement = true;
						}
						myRC.setIndicatorString(1, curPath.toString());
					} else {
						chilledTurns++;
					}
				}
				return;
			}
		} else {
			myRC.setIndicatorString(1, "going to: " + target.toString() + " which is " + myRC.senseTerrainTile(target));
			curPath = navigator.pathFind(myRC.getLocation(), target);
			myRC.setIndicatorString(1, curPath.toString());
			targetLoc = target;
		}
	}

	protected boolean respond_to_threat(int aggression) throws GameActionException{
		if(this.myRC.getActionDelay() > 1)
			return false;

		if(enemies.length == 0)
			return false;

		if(nearEnemyCount - nearAllyCount >= 2){
			System.out.println("BOOM!");
			this.myRC.selfDestruct();
		}

		MapLocation myLoc;
		if (myCOM!= null) {
			myLoc = myCOM;
		} else
			myLoc = this.myRC.getLocation();

		int maxHeur = 0;

		RobotInfo bestRobotInfo = null;
		for(int i = 0; i < enemies.length; i++){
			RobotInfo info = this.myRC.senseRobotInfo(enemies[i]);


			int thisHeur = getHeuristic(info, myLoc, aggression);
			if(thisHeur > maxHeur){
				maxHeur = thisHeur;
				bestRobotInfo = info;
			}
		}

		if(bestRobotInfo != null){
			if(this.myRC.canAttackSquare(bestRobotInfo.location)){
				this.myRC.attackSquare(bestRobotInfo.location);
			}else if (true){
				Direction dir = directionTo(bestRobotInfo.location);
				if(dir != null && canMove(dir))
					this.myRC.move(dir);
			}
			return true;
		}

		return false;
	}

	protected int getHeuristic(RobotInfo r, MapLocation orig, int aggression){
		//value 2: attacking enemy pasture
		//value 1: defending our pasture
		//value 0: never go out of way to attack enemy under any circumstances

		int health = (int)r.health;
		int dist = orig.distanceSquaredTo(r.location); //distance computed with ally CM, to have us agree
		int dist2 = this.myRC.getLocation().distanceSquaredTo(r.location); //distance computed with us, so we don't move then shoot
		RobotType type = r.type;

		int heuristic = 200;
		if(type == RobotType.PASTR){
			heuristic -= health-50; //remember that pasture health is 200
		} else if(type == RobotType.NOISETOWER)
			heuristic -= 1000;
		else if(type == RobotType.SOLDIER)
			heuristic -= health;

		heuristic -= dist;

		if(dist2 > 10){
			if(aggression == 2)
				heuristic -= 100;
			if(aggression == 1)
				heuristic -= 160;
			if(aggression == 0)
				heuristic -= 200;
		}

		return heuristic;
	}

	protected void defend_step() throws GameActionException {
		if (HQMessage.state == BaseRobot.State.DEFEND && this.myRC.getLocation().distanceSquaredTo(HQMessage.targetLocation) < 30){
			if (enemies.length > 2){
				this.myRC.broadcast(CAUTION_CHANNEL, Clock.getRoundNum());
			}
		}
		if (!isSafe()) { return; }

		int atPastr = 2;
		if(this.myRC.canSenseSquare(ourPastrLoc) && this.myRC.senseObjectAtLocation(ourPastrLoc) != null)
			atPastr = 1;
		if(respond_to_threat(atPastr)){ return; }

		MapLocation target = ourNoiseLoc;
		if (this.myRC.getLocation().equals(ourNoiseLoc)){
			//wait for sufficient reinforcements before building shit
			timer--;
			if(this.myRC.senseNearbyGameObjects(Robot.class, 10000, this.myTeam).length > reinforcementReq || timer < 0){
				if (this.myRC.readBroadcast(CAUTION_CHANNEL) == 0)
					this.myRC.construct(RobotType.NOISETOWER);
			}
		}

		if (this.myRC.canSenseSquare(target)){
			GameObject squattingRobot = this.myRC.senseObjectAtLocation(target);
			if (squattingRobot != null && squattingRobot.getTeam() == this.myTeam){
				target = ourPastrLoc;

				RobotInfo pastrInfo = this.myRC.senseRobotInfo((Robot) squattingRobot);
				if(this.myRC.getLocation().equals(ourPastrLoc)) {
					if ((pastrInfo.isConstructing && pastrInfo.constructingRounds <= 55) || pastrInfo.type == RobotType.NOISETOWER){
						if (this.myRC.readBroadcast(CAUTION_CHANNEL) == 0)
							this.myRC.construct(RobotType.PASTR);
					}
				}
			}
		}

		move_to_target(target, false);
	}    

	protected void rally_step() throws GameActionException{
		if(this.HQMessage.targetLocation.equals(this.myHQLoc)){
			if(this.myRC.getLocation().distanceSquaredTo(this.myHQLoc) > 100){
				defend_step();
				return;
			}


			//gather up soldiers near our HQ, moving out of way if squished
			calculate_COM_stats();
			if(respond_to_threat(0)){ return; }
			if(myRobotCount >= this.myRC.readBroadcast(274)-1 && this.myRC.getLocation().distanceSquaredTo(this.myHQLoc) < 4){
				Direction dir = this.myRC.getLocation().directionTo(this.myHQLoc).opposite();
				if(this.myRC.canMove(dir)){
					this.myRC.move(dir);
					return;
				}
			}

			if(myRobotCount >= this.myRC.readBroadcast(274)+1 && this.myRC.getLocation().distanceSquaredTo(this.myHQLoc) < 5){
				Direction dir = this.myRC.getLocation().directionTo(this.myHQLoc).opposite();
				if(this.myRC.canMove(dir)){
					this.myRC.move(dir);
					return;
				}
			}

			if(myRobotCount >= this.myRC.readBroadcast(274)+4 && this.myRC.getLocation().distanceSquaredTo(this.myHQLoc) < 9){
				Direction dir = this.myRC.getLocation().directionTo(this.myHQLoc).opposite();
				if(this.myRC.canMove(dir)){
					this.myRC.move(dir);
					return;
				}
			}

		} else if(this.HQMessage.targetLocation.equals(this.enemyHQLoc)){
			//surround the enemy HQ
			calculate_COM_stats();
			if(respond_to_threat(1)){ return; }

			MapLocation ourLoc = this.myRC.getLocation();

			Direction dir;
			if(ourLoc.distanceSquaredTo(this.enemyHQLoc) > 81){
				move_to_target(this.enemyHQLoc.add(this.enemyHQLoc.directionTo(this.myHQLoc), 6), false);
				return;
			} else if(ourLoc.distanceSquaredTo(this.enemyHQLoc) > 50){
				dir = ourLoc.directionTo(this.enemyHQLoc);
			} else {
				Direction dir1, dir2;
				if(Clock.getRoundNum() % 100 < 50){
					dir1 = ourLoc.directionTo(this.enemyHQLoc).rotateLeft().rotateLeft();
					dir2 = dir1.rotateLeft();
				} else {
					dir1 = ourLoc.directionTo(this.enemyHQLoc).rotateRight().rotateRight();
					dir2 = dir1.rotateRight();
				}

				if(ourLoc.add(dir1).distanceSquaredTo(this.enemyHQLoc) > 30)
					dir = dir1;
				else if(ourLoc.add(dir2).distanceSquaredTo(this.enemyHQLoc) > 30)
					dir = dir2;
				else
					dir = dir2.rotateRight();
			}

			if(dir != null && this.myRC.canMove(dir)){
				this.myRC.move(dir);
				return;
			} 

		} else{
			//TODO: line up near enemy PASTR




		}


		if (!isSafe()) { return; }
		if(respond_to_threat(0)){ return; }

	}


	protected boolean isSafe() throws GameActionException {
		calculate_COM_stats();

		if (myRobotHealth < enemyHealth && hittableAllyCount < enemyCount) {
			this.myRC.setIndicatorString(1, "WHOOPS NOT SAFE");
			Direction moveDirection = null;

			Direction d1;
			if(HQMessage.state == State.DEFEND){
				d1 = directionTo(ourPastrLoc);
			} else {
				d1 = directionTo(enemyCOM, myCOM);
			}

			if(d1 != null){
				moveDirection = directionTo(this.myCOM.add(d1, 8));
			} else {
				return true;
			}

			if (myRC.isActive() && moveDirection != null) {
				myRC.move(moveDirection);
				return false;
			}
		}

		this.myRC.setIndicatorString(1, "TOTALLY SAFE");
		return true;
	}

	protected void calculate_COM_stats() throws GameActionException{
		int avgx = 0;
		int avgy = 0;

		MapLocation ourLoc = this.myRC.getLocation();
		nearEnemyCount = 0;
		nearAllyCount = 0;
		enemyHealth = 0;
		enemyCount = 0;
		hittableAllyCount = 0;


		for (int i = 0; i < enemies.length; i++) {
			RobotInfo ri = this.myRC.senseRobotInfo(enemies[i]);
			if (ri.type== RobotType.SOLDIER){
				enemyHealth+= ri.health;
				enemyCount++;
				if(ri.location.distanceSquaredTo(ourLoc) <= 2)
					nearEnemyCount++;
				avgx += ri.location.x;
				avgy += ri.location.y;	
			}
		}
		avgx /= (double) enemyCount;
		avgy /= (double) enemyCount;
		enemyCOM = new MapLocation(avgx, avgy);

		int myx = this.myRC.getLocation().x; int myy = this.myRC.getLocation().y	;
		myCOM = null;

		myRobotCount = 1;
		myRobotHealth = (int)this.myRC.getHealth();

		for (int i = 0; i < allies.length; i++) {
			RobotInfo ri = this.myRC.senseRobotInfo(allies[i]);
			//if (ri.location.distanceSquaredTo(enemyCOM) <= 35) {
			myRobotCount++;
			if(ri.location.distanceSquaredTo(ourLoc) <= 2)
				nearAllyCount++;
			if(ri.location.distanceSquaredTo(enemyCOM) <= 14){
				hittableAllyCount++;
				myRobotHealth+= ri.health;
			}

			myx+= ri.location.x; myy+= ri.location.y;
			//}
		} if (myRobotCount!=0){
			myx/= myRobotCount; myy/= myRobotCount;
			myCOM = new MapLocation(myx, myy);
		}


	}

	protected Direction directionTo(MapLocation loc1, MapLocation loc2) throws GameActionException {
		Direction dir = loc1.directionTo(loc2);

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

}
