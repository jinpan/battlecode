package team007;

import java.util.LinkedList;

import battlecode.common.*;

public class SoldierPlayer extends BaseRobot {

	MapLocation targetLoc;
	MapLocation ourPastrLoc;
	MapLocation ourNoiseLoc;
	ActionMessage HQMessage;
	MapLocation myCOM;
	MapLocation enemyCOM;
	int reinforcementReq;
	
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
			//this.myRC.broadcast(ALIVE_OR_DEAD, 0);
			if (HQMessage.targetID == 2 || noiseTowerOffense) {
				this.noise_tower_offense();
				return;
			}

			switch (HQMessage.state) {
			case ATTACK: this.attack_step(); myRC.setIndicatorString(2, "ATTACK"); break;
			case DEFEND: this.defend_step(); myRC.setIndicatorString(2, "DEFEND"); break;
			default: myRC.setIndicatorString(2, "DEFAULT");
			}
		}
	}


	protected void noise_tower_offense() throws GameActionException {
		int noisebuilt = this.myRC.readBroadcast(NOISE_OFFENSE_CHANNEL);

		if (noisebuilt==0 || noiseTowerOffense){			
			if(!noiseTowerOffense){
				ActionMessage noiseAction = new ActionMessage(BaseRobot.State.DEFEND, 2, HQMessage.targetLocation);
				this.myRC.broadcast(NOISE_OFFENSE_CHANNEL, (int)noiseAction.encode());
				
				this.noiseTowerOffense = true;
				dispNoiseLoc = HQMessage.targetLocation;
				for(Direction d: dirs){
					int shift = 0;
					if(d.isDiagonal()){
						shift = 12;
					} else {
						shift = 17;
					}

					dispNoiseLoc = dispNoiseLoc.add(d, shift);
					if(isGoodLoc(dispNoiseLoc) && dispNoiseLoc.distanceSquaredTo(ourPastrLoc)>100 && dispNoiseLoc.distanceSquaredTo(this.enemyHQLoc)> 50 )
						break;
					else
						dispNoiseLoc = dispNoiseLoc.add(d, -shift);
				}
			}


			if (this.myRC.getLocation().equals(dispNoiseLoc)){
				this.myRC.construct(RobotType.NOISETOWER);
			} else {
				move_to_target(dispNoiseLoc, false);
			}

		} else {
			this.attack_step();
		}
	}

	protected void attack_step() throws GameActionException {
		if (!isSafe()) { return; }
		if (respond_to_threat(false)){ return; }

		if ((this.myRC.canSenseSquare(HQMessage.targetLocation) && this.myRC.senseObjectAtLocation(HQMessage.targetLocation) == null)) {
			move_to_target(ourPastrLoc, false);
		} else {
			if (myCOM!= null && HQMessage.state == BaseRobot.State.ATTACK ) {
				if (myCOM.distanceSquaredTo(HQMessage.targetLocation) 
						<= 25 + this.myRC.getLocation().distanceSquaredTo(HQMessage.targetLocation)) {
					Direction dir = this.myRC.getLocation().directionTo(myCOM);
					if (this.myRC.isActive() && dir!=null && canMove(dir)) {
						this.myRC.move(dir);
						System.out.println("moving to com on the way to target");
						this.myRC.yield();
					}
				}
			}
			move_to_target(HQMessage.targetLocation, false);

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
				this.myRC.yield();
			}
		} else {
			myRC.setIndicatorString(1, "going to: " + target.toString() + " which is " + myRC.senseTerrainTile(target));
			curPath = navigator.pathFind(myRC.getLocation(), target);
			myRC.setIndicatorString(1, curPath.toString());
			targetLoc = target;
		}
	}

	protected boolean respond_to_threat(boolean defending) throws GameActionException{
		if(this.myRC.getActionDelay() > 1)
			return false;

		if(enemies.length == 0)
			return false;

		//MapLocation myLoc = this.myRC.getLocation();
		MapLocation myLoc;
		if (myCOM!= null) {
			myLoc = myCOM;
		} else
			myLoc = this.myRC.getLocation();

		int maxHeur = 0;
		if(defending)
			maxHeur = myLoc.distanceSquaredTo(HQMessage.targetLocation);
		
		RobotInfo bestRobotInfo = null;
		for(int i = 0; i < enemies.length; i++){
			RobotInfo info = this.myRC.senseRobotInfo(enemies[i]);
			int thisHeur = getHeuristic(info, myLoc);
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

	protected int getHeuristic(RobotInfo r, MapLocation orig){
		int health = (int)r.health;
		int dist = orig.distanceSquaredTo(r.location);
		RobotType type = r.type;

		int heuristic = 200;
		if(type == RobotType.PASTR)
			heuristic -= health * 3 / 10; //remember that pasture health is 200
		else
			heuristic -= health;
		heuristic -= dist * 4;
		return heuristic;
	}

	protected void defend_step() throws GameActionException {
		if (HQMessage.state == BaseRobot.State.DEFEND && this.myRC.getLocation().distanceSquaredTo(HQMessage.targetLocation) < 30){
			if (enemies.length > 2){
				this.myRC.broadcast(CAUTION_CHANNEL, Clock.getRoundNum());
			}
		}
		if (!isSafe()) { return; }

		boolean atPastr = false;
		if(this.myRC.canSenseSquare(ourPastrLoc) && this.myRC.senseObjectAtLocation(ourPastrLoc) != null)
			atPastr = true;
		if(respond_to_threat(atPastr)){ return; }

		MapLocation target = ourNoiseLoc;
		if (this.myRC.getLocation().equals(ourNoiseLoc)){
			//wait for sufficient reinforcements before building shit
			if(this.myRC.senseNearbyGameObjects(Robot.class, 10000, this.myTeam).length > reinforcementReq){
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

		boolean sneak = false;
		if(this.myRC.getLocation().distanceSquaredTo(target) < 16){
			sneak = true;
		}
		move_to_target(target, sneak);
	}    


	protected boolean isSafe() throws GameActionException {
		
		int avgx = 0;
		int avgy = 0;
		int enemyCount = 0;
		int enemyHealth = 0;

		for (int i = 0; i < enemies.length; i++) {
			RobotInfo ri = this.myRC.senseRobotInfo(enemies[i]);
			if (ri.type== RobotType.SOLDIER){
				enemyHealth+= ri.health;
				enemyCount++;
				avgx += ri.location.x;
				avgy += ri.location.y;	
			}
		}
		avgx /= (double) enemyCount;
		avgy /= (double) enemyCount;
		enemyCOM = new MapLocation(avgx, avgy);
		
		int myRobotCount = 0;
		int myRobotHealth = 0;
		int myx = 0; int myy = 0;
		myCOM = null;


		for (int i = 0; i < allies.length; i++) {
			RobotInfo ri = this.myRC.senseRobotInfo(allies[i]);
			if (ri.location.distanceSquaredTo(enemyCOM) <= 35) {
				myRobotCount++;
				myRobotHealth+= ri.health;
				myx+= ri.location.x; myy+= ri.location.y;
			}
		} if (myRobotCount!=0){
			myx/= myRobotCount; myy/= myRobotCount;
			myCOM = new MapLocation(myx, myy);
		}

		if (myRobotHealth < 0.5*enemyHealth && myRobotCount < 0.5*enemyCount) {
			if (this.myRC.getHealth() < 25){
				//TODO: evaluate whether returning true here is really better. otherwise consider self-destruct

				/*if (this.myRC.getLocation().distanceSquaredTo(com)<= 4){
					System.out.println("self destructing");
					this.myRC.selfDestruct(); 
					return false;
				}*/

				/*Direction moveDirection = directionTo(com);
				if (myRC.isActive() && moveDirection != null && canMove(moveDirection)) {
					myRC.move(moveDirection);
				}*/ return true;
			} else {
				Direction moveDirection;
				if (myCOM!= null)
					moveDirection = this.myRC.getLocation().directionTo(myCOM);
				else 
					moveDirection = this.myRC.getLocation().directionTo(enemyCOM.add(enemyCOM.directionTo(ourPastrLoc), 7));
				if (myRC.isActive() && moveDirection != null && canMove(moveDirection)) {
					myRC.move(moveDirection);
					return false;
				}
			}
		}

		return true;
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
