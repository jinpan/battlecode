package pastrBasic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import battlecode.common.*;

public class HQPlayer extends BaseRobot {
	Direction toEnemy;
	MapLocation[] PASTRLocs;
	//HashMap<Integer, Integer> idToOrder = new HashMap<Integer, Integer>();

	int numRobots, numProcessed; //total number of robots, number of robots in the hashmap
	int numPASTR = 3;

	public HQPlayer(RobotController myRC) throws GameActionException {
		super(myRC);

		this.toEnemy = this.myHQLoc.directionTo(this.enemyHQLoc);
		this.numRobots = 1;
		this.numProcessed = 0;
		
		numPASTR = find_smart_PASTR_number(); //make this method smart!
		this.PASTRLocs = new MapLocation[numPASTR];
		PASTRLocs = find_k_best_pasture_locations(numPASTR); //make this method smart!
	}

	@Override
	protected void step() throws GameActionException {
		if (this.myRC.isActive() && this.myRC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			this.spawn();
			++this.numRobots;
		}

		/*
		System.out.println(this.numRobots + " " + this.numProcessed);
		if(numRobots > numProcessed){
			int channel = BaseRobot.get_outbox_channel(numProcessed+1, BaseRobot.OUTBOX_ID_CHANNEL);
			int id = this.myRC.readBroadcast(channel);
			idToOrder.put(id, numProcessed+1);
			numProcessed++;
		}
		*/
		
		
		/*
		int channel, id;
		for (int i=1; i<this.numRobots; ++i){
			channel = BaseRobot.get_outbox_channel(i, BaseRobot.OUTBOX_ID_CHANNEL);
			id = this.myRC.readBroadcast(channel);
			idToOrder.put(id,  i);
		}
		*/

		int order, channel;
		StateMessage state;

		for (Robot robot: this.myRC.senseNearbyGameObjects(Robot.class, 4)){ //for every nearby robot
			if (idToOrder(robot.getID()) == 0){
				continue;
			}
			order = idToOrder(robot.getID());
			channel = BaseRobot.get_outbox_channel(order, BaseRobot.OUTBOX_STATE_CHANNEL);
			state = StateMessage.decode(this.myRC.readBroadcast(channel));
			
			if (state.myState == BaseRobot.State.DEFAULT){ //if this robot has no job yet
				assignJob(order); //make this method smart!
			}
		}
	}

	//currently only assigns pasture builders / herders
	private void assignJob(int order) throws GameActionException{
		ActionMessage action = new ActionMessage(BaseRobot.State.PASTURE, 0, this.PASTRLocs[order%numPASTR]);
		int channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
		this.myRC.broadcast(channel, action.encode());
	}

	private boolean spawn() throws GameActionException {
		if (this.myRC.senseObjectAtLocation(this.myHQLoc.add(this.toEnemy)) == null){
			this.myRC.spawn(this.toEnemy);
			return true;
		}
		else {
			for (Direction dir: BaseRobot.dirs){
				if (dir != this.toEnemy
						&& this.myRC.senseObjectAtLocation(this.myHQLoc.add(dir)) == null){
					this.myRC.spawn(dir);
					return true;
				}
			}
		}
		return false;
	}

	//part of Jin's smart pasture location finder, but very slow
	private double score_pasture_location(MapLocation loc, double[][] spawnRate){
		double base_score = 0, multiplier = 1;
		MapLocation tmpLoc;
		TerrainTile tmpTile;
		if (this.myRC.senseTerrainTile(loc) == TerrainTile.VOID){
			return -1;
		}
		for (Direction dir: BaseRobot.dirs){
			tmpLoc = loc.add(dir);
			tmpTile = this.myRC.senseTerrainTile(tmpLoc);
			switch (tmpTile) {
			case OFF_MAP: return -1;
			case VOID: return -1;
			default: break;
			}
			base_score += spawnRate[tmpLoc.x][tmpLoc.y];
		}
		int normal_count = 0, offmap_count = 0, road_count = 0, void_count = 0;
		for (int i=0; i<3; ++i){
			tmpLoc = new MapLocation(loc.x - 2 + i, loc.y + 2);
			tmpTile = this.myRC.senseTerrainTile(tmpLoc);
			switch (tmpTile) {
			case NORMAL: ++normal_count; break;
			case OFF_MAP: ++offmap_count; break;
			case ROAD: ++road_count; break;
			case VOID: ++void_count; break;
			}
			base_score += 0.5 * spawnRate[tmpLoc.x][tmpLoc.y];
			tmpLoc = new MapLocation(loc.x + 2, loc.y + 2 - i);
			tmpTile = this.myRC.senseTerrainTile(tmpLoc);
			switch (tmpTile) {
			case NORMAL: ++normal_count; break;
			case OFF_MAP: ++offmap_count; break;
			case ROAD: ++road_count; break;
			case VOID: ++void_count; break;
			}
			base_score += 0.5 * spawnRate[tmpLoc.x][tmpLoc.y];
			tmpLoc = new MapLocation(loc.x + 2 - i, loc.y - 2);
			tmpTile = this.myRC.senseTerrainTile(tmpLoc);
			switch (tmpTile) {
			case NORMAL: ++normal_count; break;
			case OFF_MAP: ++offmap_count; break;
			case ROAD: ++road_count; break;
			case VOID: ++void_count; break;
			}
			base_score += 0.5 * spawnRate[tmpLoc.x][tmpLoc.y];
			tmpLoc = new MapLocation(loc.x - 2, loc.y - 2 + i);
			tmpTile = this.myRC.senseTerrainTile(tmpLoc);
			switch (tmpTile) {
			case NORMAL: ++normal_count; break;
			case OFF_MAP: ++offmap_count; break;
			case ROAD: ++road_count; break;
			case VOID: ++void_count; break;
			}
			base_score += 0.5 * spawnRate[tmpLoc.x][tmpLoc.y];
		}
		multiplier += offmap_count + void_count + 0.1 * road_count;
		return base_score * multiplier / Math.sqrt(loc.distanceSquaredTo(this.myHQLoc));
	}
	
	//Jin's smart pasture location finder, but very slow
	protected MapLocation[] find_k_best_pasture_locations_2(int k){
		// current implementation horribly inefficient, does not deal with adjacent pastures correctly.
		double[][] spawnRate = this.myRC.senseCowGrowth();
		MapLocation[] result = new MapLocation[k];
		TreeMap< Double, ArrayList<MapLocation> > resultTree = new TreeMap< Double, ArrayList<MapLocation> >();

		MapLocation temp;
		double score;
		ArrayList<MapLocation> candidates = new ArrayList<MapLocation>();
		for (int i=0; i<this.myRC.getMapWidth(); ++i){
			for (int j=0; j<this.myRC.getMapHeight(); ++j){
				temp = new MapLocation(i, j);
				score = this.score_pasture_location(temp, spawnRate);
				if (score > resultTree.firstKey()){
					if (k == 0){
						// delete one of the worst of the best so far, no more room
						if (resultTree.get(resultTree.firstKey()).size() == 1){
							resultTree.remove(resultTree.firstKey());
						}
						else {
							resultTree.get(resultTree.firstKey()).remove(0);
						}
					}
					else {
						--k;
					}
					// insert the spot in
					if (resultTree.containsKey(score)){
						resultTree.get(score).add(new MapLocation(i, j));
					}
					else {
						resultTree.put(score, new ArrayList<MapLocation>());
						resultTree.get(score).add(new MapLocation(i, j));
					}
				}
			}
		}
		int counter = 0;
		for (ArrayList<MapLocation> maplocs: resultTree.values()){
			for (MapLocation loc: maplocs){
				result[counter] = loc;
				++counter;
			}
		}
		return result;
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

			eLoc = eLoc.add(d.opposite(), 1); //back away from wall

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
