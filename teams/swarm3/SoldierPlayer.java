package swarm3;

import java.util.LinkedList;

import battlecode.common.*;

public class SoldierPlayer extends BaseRobot {

	BaseRobot.State state;
	MapLocation target_loc;
	MapLocation pastr_loc;
	MapLocation noise_loc;
	MapLocation[] pastr_locs = new MapLocation[4];
	MapLocation[] noise_locs = new MapLocation[4];
	ActionMessage HQMessage;
	MapLocation myCOM;
	MapLocation enemyCOM;
	int reinforcementReq;
	
	Navigation navigator;

	MapLocation dispNoiseLoc = null;

	boolean voteRetreat;
	boolean straightMovement;
	protected int soldier_order;
	Robot[] allies, enemies, nearby_enemies;
	LinkedList<MapLocation> curPath = new LinkedList<MapLocation>();
	
	public static final int WAIT_TIME = 2;
	public static final int ENEMY_COW_LIMIT = 1000;
	public static final int COWVERT_NOISE_CONSTRUCT_ROUND = 50;
	public static final int COWVERT_PASTR_CONSTRUCT_ROUND = 25;
	

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
		HQMessage = ActionMessage.decode(this.myRC.readBroadcast(HQ_BROADCAST_CHANNEL));
		enemies = myRC.senseNearbyGameObjects(Robot.class, 35, enemy_team);
		
		if (strategy == null){
			int strategy_ord = myRC.readBroadcast(STRATEGY_CHANNEL);
			if (strategy_ord != 0){
				strategy = Strategy.values()[strategy_ord - 1];
			}
		}
		
		if (strategy != null){
			switch (strategy){
				case COWVERT: {
					LocationMessage locMsg = LocationMessage.decode(this.myRC.readBroadcast(LOC_CHANNELS[0]));
					if (locMsg != null){
						noise_locs[0] = locMsg.noise_loc;
						pastr_locs[0] = locMsg.pastr_loc;
					}
					LocationMessage locMsg1 = LocationMessage.decode(this.myRC.readBroadcast(LOC_CHANNELS[1]));
					if (locMsg1 != null){
						noise_locs[1] = locMsg1.noise_loc;
						pastr_locs[1] = locMsg1.pastr_loc;
					}
					break;
				}
				case FARMVILLE: {
					break;
				}
				default: {
					LocationMessage locMsg = LocationMessage.decode(this.myRC.readBroadcast(LOC_CHANNEL));
					if (locMsg != null){
						noise_loc = locMsg.noise_loc;
						pastr_loc = locMsg.pastr_loc;
					}
					
					if (myRC.readBroadcast(NOISE_DIBS_CHANNEL) == 0){
						myRC.broadcast(NOISE_DIBS_CHANNEL, 1);
						state = State.NOISE;
						System.out.println("DIBS");
					}
				}
			}
		}
	}

	@Override
	protected void step() throws GameActionException {
		if (strategy == null){
			chillStep();
		}
		else {
			switch (strategy){
				case COWVERT: {
					cowvertStep(); break;
				}
				case FARMVILLE: {
					break;
				}
				default: {
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
		}
	}
	
	void cowvertStep() throws GameActionException {
		if (!myRC.isActive()){
			return;
		}
		
		if (enemies.length > 0){
			nearby_enemies = myRC.senseNearbyGameObjects(Robot.class, 10, this.enemy_team);
			if (nearby_enemies.length > 0){
				double min_health = 101;
				RobotInfo best_info = null, info = null;
				for (Robot enemy: nearby_enemies){
					info = myRC.senseRobotInfo(enemy);
					if (info.health < min_health){
						best_info = info;
					}
				}
				if (myRC.isActive()){
					myRC.attackSquare(best_info.location);
				}
				return;
			}
		}
		
		if (myRC.getLocation().equals(pastr_locs[0])){
			if (Clock.getRoundNum() > COWVERT_PASTR_CONSTRUCT_ROUND){
				myRC.construct(RobotType.PASTR);
			}
			else {
				return;
			}
		}
		if (myRC.getLocation().equals(pastr_locs[1])){
			if (Clock.getRoundNum() > COWVERT_PASTR_CONSTRUCT_ROUND){
				myRC.construct(RobotType.PASTR);
			}
			else {
				return;
			}
		}
		if (myRC.getLocation().equals(noise_locs[0])){
			if (Clock.getRoundNum() > COWVERT_NOISE_CONSTRUCT_ROUND){
				myRC.construct(RobotType.NOISETOWER);
			}
			else {
				return;
			}
		}
		if (myRC.getLocation().equals(noise_locs[1])){
			if (Clock.getRoundNum() > COWVERT_NOISE_CONSTRUCT_ROUND){
				//myRC.construct(RobotType.NOISETOWER);
			}
			else {
				return;
			}
		}
		
		Direction dir;
		if (myRC.canSenseSquare(pastr_locs[0]) && myRC.senseObjectAtLocation(pastr_locs[0]) == null){
			dir = directionTo(pastr_locs[0]);
			if (dir != null){
				myRC.move(dir);
			}
		}
		else if (myRC.canSenseSquare(pastr_locs[1]) && myRC.senseObjectAtLocation(pastr_locs[1]) == null){
			dir = directionTo(pastr_locs[1]);
			if (dir != null){
				myRC.move(dir);
			}
		}
		else if (myRC.canSenseSquare(noise_locs[0]) && myRC.senseObjectAtLocation(noise_locs[0]) == null){
			dir = directionTo(noise_locs[0]);
			if (dir != null){
				myRC.move(dir);
			}
		}
		else if (myRC.canSenseSquare(noise_locs[1]) && myRC.senseObjectAtLocation(noise_locs[1]) == null){
			dir = directionTo(noise_locs[1]);
			if (dir != null){
				myRC.move(dir);
			}
		}
		else {
			chillStep();
		}
	}
	

	void chillStep() throws GameActionException {
		// don't block anyone.
		if (myRC.canSenseSquare(my_hq_loc)){
			boolean hq_blocked = true;
			MapLocation candidate;
			for (Direction dir: dirs){
				candidate = my_hq_loc.add(dir);
				if (!navigator.isGood(candidate)
						|| (myRC.canSenseSquare(candidate) && myRC.senseObjectAtLocation(candidate) == null)){
					hq_blocked = false;
					break;
				}
			}
			if (hq_blocked && myRC.isActive()){
				moveTo(HQMessage.targetLocation, true);
			}
		}
		else {
			return;
		}
	}

	void attackStep() throws GameActionException {
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
	
	void defendStep() throws GameActionException {
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
				if(myRC.senseNearbyGameObjects(Robot.class, 35, this.my_team).length > reinforcementReq
						&& myRC.readBroadcast(NOISE_DIBS_CHANNEL) == 2
						&& myRC.readBroadcast(CAUTION_CHANNEL) == 0){
					myRC.construct(RobotType.PASTR);
				}
			}
	
			boolean sneak = (this.myRC.getLocation().distanceSquaredTo(pastr_loc) < 16);
			moveTo(pastr_loc, sneak);
		}
	}
	
	void noiseStep() throws GameActionException {
		if (noise_loc == null){
			
		}
		else {
			if (!isSafe()){ return;}
			
			if (this.myRC.getLocation().equals(noise_loc) && myRC.isActive()){
				this.myRC.construct(RobotType.NOISETOWER);
			}
			
			moveTo(noise_loc, false);
		}
	}

	void moveTo(MapLocation target, boolean sneak) throws GameActionException{
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
					LinkedList<MapLocation> newCurPath = navigator.findPath(myRC.getLocation(), target);
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
			curPath = navigator.findPath(myRC.getLocation(), target);
			myRC.setIndicatorString(1, curPath.toString());
			target_loc = target;
		}
	}

	boolean respondToThreat(boolean defending) throws GameActionException{
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

	int getHeuristic(RobotInfo r, MapLocation orig){
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


	boolean isSafe() throws GameActionException {
		allies = this.myRC.senseNearbyGameObjects(Robot.class, 35, my_team);
		
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


	Direction directionTo(MapLocation loc) throws GameActionException {
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
