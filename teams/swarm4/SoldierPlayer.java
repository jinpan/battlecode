package swarm4;

import java.util.LinkedList;

import battlecode.common.*;

public class SoldierPlayer extends BaseRobot {

	BaseRobot.State state;
	MapLocation target_loc;
	MapLocation pastr_loc;
	MapLocation noise_loc;
	ActionMessage HQMessage;
	MapLocation myCOM;
	MapLocation enemyCOM;
	int reinforcementReq;
	
	Navigation navigator;

	MapLocation dispNoiseLoc = null;

	boolean voteRetreat;
	boolean straightMovement;
	protected int soldier_order;
	Robot[] allies, enemies;
	LinkedList<MapLocation> curPath = new LinkedList<MapLocation>();

	public SoldierPlayer(RobotController myRC) throws GameActionException {
		super(myRC);

		soldier_order = myRC.readBroadcast(BaseRobot.SOLDIER_ORDER_CHANNEL);
		myRC.broadcast(BaseRobot.SOLDIER_ORDER_CHANNEL, this.soldier_order + 1);
		
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
		this.navigator = new Navigation(myRC);
		
	}

	@Override
	protected void setup() throws GameActionException {
		if (myRC.readBroadcast(NOISE_DIBS_CHANNEL) == 0){
			myRC.broadcast(NOISE_DIBS_CHANNEL, 1);
			state = State.NOISE;
			System.out.println("DIBS");
		}
		HQMessage = ActionMessage.decode(this.myRC.readBroadcast(HQ_BROADCAST_CHANNEL));
		LocationMessage locMsg = LocationMessage.decode(this.myRC.readBroadcast(LOC_CHANNEL));
		if (locMsg != null){
			noise_loc = locMsg.noise_loc;
			pastr_loc = locMsg.pastr_loc;
		}
		enemies = this.myRC.senseNearbyGameObjects(Robot.class, 35, this.enemyTeam);
	}

	@Override
	protected void step() throws GameActionException {
		if(this.myRC.isActive()){
			if (state == State.NOISE) {
				noiseStep(); myRC.setIndicatorString(2, "NOISE");
				return;
			}
			switch (HQMessage.state) {
				case ATTACK: attackStep(); myRC.setIndicatorString(2, "ATTACK"); break;
				case DEFEND: defendStep(); myRC.setIndicatorString(2, "DEFEND"); break;
				default: defendStep(); myRC.setIndicatorString(2, "DEFAULT");
			}
		}
	}

	protected void attackStep() throws GameActionException {
		if (!isSafe()) { return; }
		if (respondToThreat(false)){ return; }

		if ((this.myRC.canSenseSquare(HQMessage.targetLocation) && this.myRC.senseObjectAtLocation(HQMessage.targetLocation) == null)) {
			moveTo(pastr_loc, false);
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
			moveTo(HQMessage.targetLocation, false);

		}
	}
	
	protected void defendStep() throws GameActionException {
		if (pastr_loc == null){
			
		}
		else {
			if (myRC.getLocation().distanceSquaredTo(pastr_loc) < 30){
				if (enemies.length > 2){
					myRC.broadcast(CAUTION_CHANNEL, Clock.getRoundNum());
				}
			}
			if (!isSafe()) { return; }
	
			boolean atPastr = false;
			if(myRC.canSenseSquare(pastr_loc) && this.myRC.senseObjectAtLocation(pastr_loc) != null)
				atPastr = true;
			if (respondToThreat(atPastr)){ return; }
	
			if (this.myRC.getLocation().equals(pastr_loc)){
				//wait for sufficient reinforcements before building shit
				if(myRC.senseNearbyGameObjects(Robot.class, 35, this.myTeam).length > reinforcementReq
						&& myRC.readBroadcast(NOISE_DIBS_CHANNEL) == 2
						&& myRC.readBroadcast(CAUTION_CHANNEL) == 0){
					myRC.construct(RobotType.PASTR);
				}
			}
	
			boolean sneak = (this.myRC.getLocation().distanceSquaredTo(pastr_loc) < 16);
			moveTo(pastr_loc, sneak);
		}
	}
	
	protected void noiseStep() throws GameActionException {
		if (noise_loc == null){
			
		}
		else {
			if (!isSafe()){ return;}
			
			if (this.myRC.getLocation().equals(noise_loc)){
				this.myRC.construct(RobotType.NOISETOWER);
			}
			
			moveTo(noise_loc, false);
		}
	}

	protected void moveTo(MapLocation target, boolean sneak) throws GameActionException{
		if (target.equals(this.myRC.getLocation())){
			return;
		}
		if (target.equals(target_loc)) {
			if (myRC.getLocation().equals(curPath.getFirst())) {
				curPath.remove();
				straightMovement = false;
			} else {
				Direction moveDirection = directionTo(curPath.getFirst());
				if (straightMovement && moveDirection == null) {
					moveDirection = directionTo(target);
				}
				if (myRC.isActive() && moveDirection != null && canMove(moveDirection)) {
					if(!sneak)
						myRC.move(moveDirection);
					else
						myRC.sneak(moveDirection);
				} else if (moveDirection == null){
					LinkedList<MapLocation> newCurPath = navigator.pathFind(myRC.getLocation(), target);
					newCurPath.remove();
					if (!curPath.equals(newCurPath)) {
						curPath = newCurPath;
					} else {
						straightMovement = true;
					}
					myRC.setIndicatorString(1, curPath.toString());

				}
				this.myRC.yield();
			}
		} else {
			curPath = navigator.pathFind(myRC.getLocation(), target);
			myRC.setIndicatorString(1, curPath.toString());
			target_loc = target;
		}
	}

	protected boolean respondToThreat(boolean defending) throws GameActionException{
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


	protected boolean isSafe() throws GameActionException {
		allies = this.myRC.senseNearbyGameObjects(Robot.class, 35, myTeam);
		
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
					moveDirection = this.myRC.getLocation().directionTo(enemyCOM.add(enemyCOM.directionTo(pastr_loc), 7));
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

		Direction candidate = dir.rotateLeft();

		if (this.myRC.canMove(candidate)){
			return candidate;
		}
		candidate = dir.rotateRight();
		if (this.myRC.canMove(candidate)){
			return candidate;
		}

		return null;        
	}

}
