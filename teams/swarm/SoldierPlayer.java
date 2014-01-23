package swarm;

import java.util.LinkedList;

import battlecode.common.*;

public class SoldierPlayer extends BaseRobot {

	MapLocation targetLoc;
	MapLocation ourPastrLoc;
	MapLocation ourNoiseLoc;
	ActionMessage HQMessage;

	boolean voteRetreat;
	boolean straightMovement;
	protected int soldier_order;
	LinkedList<MapLocation> curPath = new LinkedList<MapLocation>();

	public SoldierPlayer(RobotController myRC) throws GameActionException {
		super(myRC);

		this.soldier_order = this.myRC.readBroadcast(BaseRobot.SOLDIER_ORDER_CHANNEL);
		this.myRC.broadcast(BaseRobot.SOLDIER_ORDER_CHANNEL, this.soldier_order + 1);
	}

	@Override
	protected void setup() throws GameActionException {
		HQMessage = ActionMessage.decode(this.myRC.readBroadcast(HQ_BROADCAST_CHANNEL));
		ourPastrLoc = ActionMessage.decode(this.myRC.readBroadcast(PASTR_LOC_CHANNEL)).targetLocation;
		ourNoiseLoc = ActionMessage.decode(this.myRC.readBroadcast(NOISE_LOC_CHANNEL)).targetLocation;
	}

	@Override
	protected void step() throws GameActionException {
		if(this.myRC.isActive()){
			switch (HQMessage.state) {
				case ATTACK: this.attack_step(); myRC.setIndicatorString(2, "ATTACK"); break;
				case DEFEND: this.defend_step(); myRC.setIndicatorString(2, "DEFEND"); break;
				default: myRC.setIndicatorString(2, "DEFAULT");
			}
		}
	}

	protected void attack_step() throws GameActionException {
		if (!isSafe()) { return; }
		if (respond_to_threat(false)){ return; }

		//if there's nothing we can attack, do a move step
		if ((this.myRC.canSenseSquare(HQMessage.targetLocation) && this.myRC.senseObjectAtLocation(HQMessage.targetLocation) == null)) {
			move_to_target(ourPastrLoc, false);
		} else {
			move_to_target(HQMessage.targetLocation, false);
		}
	}

	protected void move_to_target(MapLocation target, boolean sneak) throws GameActionException{
		if (target.equals(this.myRC.getLocation())){
			return;
		}
		myRC.setIndicatorString(2, Boolean.toString(straightMovement));
		if (target.equals(targetLoc)) {
			// System.out.println(curPath.getFirst());
			if (myRC.getLocation().equals(curPath.getFirst())) {
				curPath.remove();
				straightMovement = false;
				myRC.setIndicatorString(2, Boolean.toString(straightMovement));
			} else {
				Direction moveDirection = directionTo(curPath.getFirst());
				if (straightMovement && moveDirection == null) {
					moveDirection = directionTo(target);
				}
				// System.out.println(moveDirection);
				if (myRC.isActive() && moveDirection != null && canMove(moveDirection)) {
					this.myRC.setIndicatorString(0, "going to pasture");
					if(!sneak)
						myRC.move(moveDirection);
					else
						myRC.sneak(moveDirection);
				} else if (moveDirection == null){
					LinkedList<MapLocation> newCurPath = Navigation.pathFind(myRC.getLocation(), target, this);
					newCurPath.remove();
					if (!curPath.equals(newCurPath)) {
						curPath = newCurPath;
					} else {
						straightMovement = true;
						myRC.setIndicatorString(2, Boolean.toString(straightMovement));
					}
					myRC.setIndicatorString(1, curPath.toString());

				}
				//this.myRC.yield();
				return;
			}
		} else {
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
		Robot[] robots = this.myRC.senseNearbyGameObjects(Robot.class, 35);
		MapLocation myCOM = this.calculateCOM(robots, this.myTeam);
		MapLocation enemyCOM = this.calculateCOM(robots, this.enemyTeam);

		if (enemyCOM != null && this.myRC.getLocation().distanceSquaredTo(HQMessage.targetLocation) < 50){
			// ahoy! there are enemies in our midst
			Robot[] enemies = this.myRC.senseNearbyGameObjects(Robot.class, 10, this.enemyTeam);
			
			if (myCOM == null || this.myRC.getLocation().distanceSquaredTo(myCOM) > 0.8 * this.myRC.getLocation().distanceSquaredTo(enemyCOM)){
				// I'm at the front of the battle
				if (enemies.length > 0){
					double minHealth = 101;
					RobotInfo info;
					MapLocation target = null;
					
					for (Robot enemy: enemies){
						info = this.myRC.senseRobotInfo(enemy);
						if (info.health < minHealth){
							minHealth = info.health;
							target = info.location;
						}
					}

					if (this.myRC.isActive() && target != null)
						this.myRC.attackSquare(target);
				}
			}
			else {
				if (enemies.length > 0){
					// I'm in the back but I can still attack something
					double minHealth = 101;
					RobotInfo info;
					MapLocation target = null;
					
					for (Robot enemy: enemies){
						info = this.myRC.senseRobotInfo(enemy);
						if (info.health < minHealth){
							minHealth = info.health;
							target = info.location;
						}
					}
					
					if (this.myRC.isActive() && target != null)
						this.myRC.attackSquare(target);
				}
				else {
					// I'm in the back but I want to move to the front
					Direction dir = this.directionTo(enemyCOM);
					if (dir != null){
						this.myRC.move(dir);
					}
					
				}
			}
		}

		MapLocation target = ourNoiseLoc;
		if (this.myRC.getLocation().equals(ourNoiseLoc)){
			//wait for sufficient reinforcements before building
			if(this.myRC.senseNearbyGameObjects(Robot.class, 10000, this.myTeam).length > 5)
				System.out.println("MAKING A NOISE TOWER");
				if (this.myRC.isActive())
					this.myRC.construct(RobotType.NOISETOWER);
		}

		if (this.myRC.canSenseSquare(target)){
			GameObject squattingRobot = this.myRC.senseObjectAtLocation(target);
			if (squattingRobot != null && squattingRobot.getTeam() == this.myTeam){
				target = ourPastrLoc;

				RobotInfo pastrInfo = this.myRC.senseRobotInfo((Robot)squattingRobot);
				if(this.myRC.getLocation().equals(ourPastrLoc) && pastrInfo.isConstructing && pastrInfo.constructingRounds <= 50) {
					System.out.println("MAKING A PASTR");
					if (this.myRC.isActive())
						this.myRC.construct(RobotType.PASTR);
				}
			}
		}

		boolean sneak = this.myRC.getLocation().distanceSquaredTo(target) < 16;
		move_to_target(target, sneak);
	}
	
	private MapLocation selfDestructSpot(Robot[] robots, MapLocation enemyCOM) throws GameActionException{
		int[][] map = new int[this.myRC.getMapWidth()][this.myRC.getMapHeight()];
		RobotInfo info;
		for (Robot robot: robots){
			info = this.myRC.senseRobotInfo(robot);
			if (info.team == this.enemyTeam && info.type == RobotType.SOLDIER){
				map[info.location.x][info.location.y] = 1;
			}
		}
		if (map[enemyCOM.x][enemyCOM.y] != 1){
			return enemyCOM;
		}
		MapLocation candidate;
		for (Direction dir: BaseRobot.dirs){
			candidate = enemyCOM.add(dir);
			if (map[candidate.x][candidate.y] != 1){
				return candidate;
			}
		}
		return null;
	}
	
	private MapLocation calculateCOM(Robot[] robots, Team team) throws GameActionException{
		int x = 0, y = 0;
		int count = 0;
		RobotInfo info;
		for (Robot robot: robots){
			info = this.myRC.senseRobotInfo(robot);
			if (info.team == team && info.type == RobotType.SOLDIER){
				x += info.location.x;
				y += info.location.y;
				++count;
			}
		}
		if (count == 0){
			return null;
		}
		return new MapLocation(x/count, y/count);
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
