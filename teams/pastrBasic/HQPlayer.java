package pastrBasic;

import java.util.ArrayList;
import java.util.HashMap;

import pastrBasic.ActionMessage;
import battlecode.common.*;

public class HQPlayer extends BaseRobot {
	Direction toEnemy;
	MapLocation[] PASTRLocs;
	double[][] spawnRates;
	double[][] locScores;

	int numRobots, numProcessed; //total number of robots, number of robots in the hashmap
	int numPASTR, herderCount;
	
	public HQPlayer(RobotController myRC) throws GameActionException {
		super(myRC);

		this.toEnemy = this.myHQLoc.directionTo(this.enemyHQLoc);
		this.numRobots = 1;
		this.numProcessed = 0;
		herderCount = 0;
		
		numPASTR = find_smart_PASTR_number(); //make this method smart!
		this.PASTRLocs = new MapLocation[numPASTR];
		
		this.spawnRates = this.myRC.senseCowGrowth();
		this.locScores = this.computeLocScoresDist();
		
		PASTRLocs[0] = this.findFirstPastureLoc();
		
		
	}

    @Override
    protected void step() throws GameActionException {
		if (this.myRC.isActive() && this.myRC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			this.spawn();
			++this.numRobots;
		}	
        
        int order, channel;
        State state;
        
    	MapLocation[] pastrLocs = this.myRC.sensePastrLocations(this.myTeam);
		MapLocation[] enemyPastrs= this.myRC.sensePastrLocations(this.enemyTeam);
        
        for (Robot robot: this.myRC.senseNearbyGameObjects(Robot.class, 4)){ //for every nearby robot
        	
			order = idToOrder(robot.getID()); 
			if (order == 0)
				continue;
			
        	channel = BaseRobot.get_outbox_channel(order, BaseRobot.OUTBOX_STATE_CHANNEL);
        	state = StateMessage.decode(this.myRC.readBroadcast(channel)).myState;
        	
        	if (state == BaseRobot.State.DEFAULT){ //if robot hasn't been given a job yet
        		if (pastrLocs.length < BaseRobot.MAX_PASTURES || herderCount < BaseRobot.MAX_PASTURES*3 || enemyPastrs.length == 0){
        			assignPastureJob(order);
        			herderCount++;
        		} else {
    				assignScoutJob(order, enemyPastrs[0]);
        		}
        	}
        }
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
    
	private void assignPastureJob(int order) throws GameActionException{
		ActionMessage action = new ActionMessage(BaseRobot.State.PASTURE, 0, this.PASTRLocs[order%numPASTR]);
		int channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
		this.myRC.broadcast(channel, action.encode());
	}
	
	private void assignScoutJob(int order, MapLocation target) throws GameActionException{
		MapLocation rallyPoint = target.add(this.myHQLoc.directionTo(target).opposite(), BaseRobot.RALLY_DISTANCE);
		ActionMessage action = new ActionMessage(BaseRobot.State.SCOUT, 0, rallyPoint);
		int channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
		this.myRC.broadcast(channel, action.encode());
	}
	
	protected double[][] computeLocScores() {
		// explicit loops for bytecode optimization, partial updates
		double[][] result = new double[this.myRC.getMapWidth()][this.myRC.getMapHeight()];

		double[] tmp_cols = new double[3];
		int idx, idx2, idx3, idx4;
		tmp_cols[0] = this.spawnRates[0][0] + this.spawnRates[0][1] + this.spawnRates[0][2];
		tmp_cols[0] = this.spawnRates[1][0] + this.spawnRates[1][1] + this.spawnRates[1][2];
		tmp_cols[0] = this.spawnRates[2][0] + this.spawnRates[2][1] + this.spawnRates[2][2];

		result[1][1] = tmp_cols[0] + tmp_cols[1] + tmp_cols[2];
		for (int i=2; i<this.myRC.getMapWidth()-1; ++i){
			idx = (i-2) % 3;
			result[i][1] = result[i-1][1] - tmp_cols[idx];
			tmp_cols[idx] = this.spawnRates[i+1][0] + this.spawnRates[i+1][1] + this.spawnRates[i+1][2];
			result[i][1] += tmp_cols[idx];
		}
		double[] tmp_rows = new double[3];
		for (int i=1; i<this.myRC.getMapWidth()-1; ++i){
			idx2 = i-1; idx3 = i+1;
			tmp_rows[0] = this.spawnRates[idx2][0] + this.spawnRates[i][0] + this.spawnRates[idx3][0];
			tmp_rows[1] = this.spawnRates[idx2][1] + this.spawnRates[i][1] + this.spawnRates[idx3][1];
			tmp_rows[2] = this.spawnRates[idx2][2] + this.spawnRates[i][2] + this.spawnRates[idx3][2];
			result[i][1] = tmp_rows[0] + tmp_rows[1] + tmp_rows[2];
			for (int j=2; j<this.myRC.getMapHeight()-1; ++j){
				idx = (j-2) % 3;
				idx4 = j-1;
				result[i][j] = result[i][idx4] - tmp_rows[idx];
				tmp_rows[idx] = this.spawnRates[idx2][idx4] + this.spawnRates[i][idx4] + this.spawnRates[idx3][idx4];
				result[i][j] += tmp_cols[idx];
			}
		}
		return result;
	}
	protected double dist_transform(int distM, int distE){
		if (distM < 25 || distE < 25){
			return 0;
		}
		else {
			return (double) distE / (double) distM;
		}
	}
	protected double[][] computeLocScoresDist() {
		// explicit loops for bytecode optimization, partial updates
		double[][] result = new double[this.myRC.getMapWidth()][this.myRC.getMapHeight()];

		double[] tmp_cols = new double[3];
		int idx, idx2, idx3, idx4;
		double dist;
		tmp_cols[0] = this.spawnRates[0][0] + this.spawnRates[0][1] + this.spawnRates[0][2];
		tmp_cols[0] = this.spawnRates[1][0] + this.spawnRates[1][1] + this.spawnRates[1][2];
		tmp_cols[0] = this.spawnRates[2][0] + this.spawnRates[2][1] + this.spawnRates[2][2];

		dist = dist_transform((this.myHQLoc.x - 1) * (this.myHQLoc.x - 1) + (this.myHQLoc.y - 1) * (this.myHQLoc.y - 1),
				(this.enemyHQLoc.x - 1) * (this.enemyHQLoc.x - 1) + (this.enemyHQLoc.y - 1) * (this.enemyHQLoc.y - 1));

		result[1][1] = (tmp_cols[0] + tmp_cols[1] + tmp_cols[2]) * dist;
		for (int i=2; i<this.myRC.getMapWidth()-1; ++i){
			idx = (i-2) % 3;
			if (dist == 0 || dist == Double.NaN){
				result[i][1] = tmp_cols[0] + tmp_cols[1] + tmp_cols[2] - tmp_cols[idx];
			}
			else {
				result[i][1] = result[i-1][1] / dist - tmp_cols[idx];	
			}
			tmp_cols[idx] = this.spawnRates[i+1][0] + this.spawnRates[i+1][1] + this.spawnRates[i+1][2];
			dist = dist_transform((this.myHQLoc.x - i) * (this.myHQLoc.x - i) + (this.myHQLoc.y - 1) * (this.myHQLoc.y - 1),
					(this.enemyHQLoc.x - i) * (this.enemyHQLoc.x - i) + (this.enemyHQLoc.y - 1) * (this.enemyHQLoc.y - 1));

			result[i][1] += tmp_cols[idx];
			result[i][1] *= dist;
		}
		double[] tmp_rows = new double[3];
		for (int i=1; i<this.myRC.getMapWidth()-1; ++i){
			idx2 = i-1; idx3 = i+1;
			tmp_rows[0] = this.spawnRates[idx2][0] + this.spawnRates[i][0] + this.spawnRates[idx3][0];
			tmp_rows[1] = this.spawnRates[idx2][1] + this.spawnRates[i][1] + this.spawnRates[idx3][1];
			tmp_rows[2] = this.spawnRates[idx2][2] + this.spawnRates[i][2] + this.spawnRates[idx3][2];

			dist = dist_transform((this.myHQLoc.x - i) * (this.myHQLoc.x - i) + (this.myHQLoc.y - 1) * (this.myHQLoc.y - 1),
					(this.enemyHQLoc.x - i) * (this.enemyHQLoc.x - i) + (this.enemyHQLoc.y - 1) * (this.enemyHQLoc.y - 1));
			result[i][1] = (tmp_rows[0] + tmp_rows[1] + tmp_rows[2]) * dist;
			for (int j=2; j<this.myRC.getMapHeight()-1; ++j){
				idx = (j-2) % 3;
				idx4 = j-1;
				if (dist == 0 || dist == Double.NaN){
					result[i][j] = tmp_rows[0] + tmp_rows[1] + tmp_rows[2] - tmp_rows[idx];
				}
				else {
					result[i][j] = result[i-1][idx4] / dist - tmp_rows[idx];	
				}
				tmp_rows[idx] = this.spawnRates[idx2][idx4] + this.spawnRates[i][idx4] + this.spawnRates[idx3][idx4];
				dist = dist_transform((this.myHQLoc.x - i) * (this.myHQLoc.x - i) + (this.myHQLoc.y - j) * (this.myHQLoc.y - j),
						(this.enemyHQLoc.x - i) * (this.enemyHQLoc.x - i) + (this.enemyHQLoc.y - j) * (this.enemyHQLoc.y - j) );
				result[i][j] += tmp_rows[idx];
				result[i][j] *= dist;
				if (i == 12){
					System.out.print(tmp_rows[0] + tmp_rows[1] + tmp_rows[2]);
					System.out.print(result[i][j] + " ");
					System.out.println(dist);
				}
			}
		}

		return result;
	}
	/*
	 * Should be called after locScores is saved with computeLocScoresDist return value
	 */
	protected MapLocation findFirstPastureLoc(){
		double bestSoFar = 0;
		int besti = 0, bestj = 0;
		for (int i=1; i<this.myRC.getMapWidth()-1; ++i){
			for (int j=1; j<this.myRC.getMapHeight()-1; ++j){
				if (this.locScores[i][j] > bestSoFar){
					bestSoFar = this.locScores[i][j];
					besti = i; bestj = j;
				}
			}
		}
		return new MapLocation(besti, bestj);
	}
	

    /*//old method for pasture finding, pretty simple
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

			eLoc = eLoc.add(d.opposite(), 2); //back away from wall

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
	*/
	protected int find_smart_PASTR_number(){
		return 1;
	}

}
