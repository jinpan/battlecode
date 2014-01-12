package pastrBasic;

import java.util.ArrayList;
import java.util.HashMap;

import pastrBasic.ActionMessage;
import battlecode.common.*;

public class HQPlayer extends BaseRobot {
	Direction toEnemy;
	
	int currentSquad;
	int[] squadAssignments;
	//MapLocation bestPastureLoc; 
	//might be worth saving best loc if we assume best cow growth rates come in big rectangles
	
	boolean squadsMax = true;
	
	public HQPlayer(RobotController myRC) throws GameActionException {
		super(myRC);

		this.toEnemy = this.myHQLoc.directionTo(this.enemyHQLoc);
		this.currentSquad=0;
		
		this.spawnRates= this.myRC.senseCowGrowth();
		this.locScores= this.computeLocScoresDist();
		
		//numPASTR = find_smart_PASTR_number(); //make this method smart!
		numPASTR= 10000;
		this.PASTRLocs = new MapLocation[numPASTR];
		//PASTRLocs = find_k_best_pasture_locations(numPASTR); //make this method smart!
		squadAssignments= new int[BaseRobot.NUM_SQUADS]; //how many robots in each squad
	}
	
	protected double[][] computeLocScores() {
		double[][] result = new double[this.myRC.getMapWidth()][this.myRC.getMapHeight()];
		for (int i=1; i<this.myRC.getMapWidth()-1; ++i){
			for (int j=1; j<this.myRC.getMapHeight()-1; ++j){
				for (int a=-1; a<2; ++a){
					for (int b=-1; b<2; ++b){
						result[i][j] += this.spawnRates[i-a][j-b];
					}
				}
			}
		}
		return result;
	}

	protected double dist_transform(int distM, int distE){
		if (distM < 25 || distE < 25){
			return 0;
		}
		else {
			return Math.pow((double) (distE-75) / (double) distM, (double) 1/4);
		}
	}

	protected double[][] computeLocScoresDist() throws GameActionException {
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
			if (dist == 0){
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
				if (Clock.getBytecodesLeft() < 100) {
					if (this.myRC.isActive() && this.myRC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
						this.spawn();
					}
				}
				idx = (j-2) % 3;
				idx4 = j-1;
				if (dist == 0){
					result[i][j] = tmp_rows[0] + tmp_rows[1] + tmp_rows[2] - tmp_rows[idx];
				}
				else {
					result[i][j] = result[i][idx4] / dist - tmp_rows[idx];	
				}
				tmp_rows[idx] = this.spawnRates[idx2][idx4] + this.spawnRates[i][idx4] + this.spawnRates[idx3][idx4];
				dist = dist_transform((this.myHQLoc.x - i) * (this.myHQLoc.x - i) + (this.myHQLoc.y - j) * (this.myHQLoc.y - j),
						(this.enemyHQLoc.x - i) * (this.enemyHQLoc.x - i) + (this.enemyHQLoc.y - j) * (this.enemyHQLoc.y - j) );
				result[i][j] += tmp_rows[idx];
				result[i][j] *= dist;
			}
		}

		return result;
	}


	/*
	 * Should be called after locScores is saved with computeLocScoresDist return value
	 */
	protected MapLocation findBestPastureLoc(){
		int counter=0;
		for (MapLocation pastr: this.PASTRLocs){
			if (pastr!=null){
				counter++;
			}
		}//System.out.println(counter);
		double bestSoFar = 0;
		boolean good=true;
		int besti = 0, bestj = 0;
		for (int i=1; i<this.myRC.getMapWidth()-1; ++i){
			for (int j=1; j<this.myRC.getMapHeight()-1; ++j){
				for (int k=0; k<counter;k++){
					MapLocation pastr= this.PASTRLocs[k];
					if (i>=pastr.x-pastrClear && i<= pastr.x+pastrClear && j>=pastr.y-pastrClear && j<=pastr.y+pastrClear){
						good=false;
					} 
				}
				if (good& this.locScores[i][j] > bestSoFar){
					bestSoFar = this.locScores[i][j];
					besti = i; bestj = j;
				} good=true;
			}
		}
		return new MapLocation(besti, bestj);
	}

	/*
	 * Should only be called after locScores is initialized and created.
	 * Assumes that loc is not a void spot.
	 */
	/*protected MapLocation findPastureLoc(MapLocation loc) {

		double score;
		int counter = 0;
		int centeri = loc.x, centerj = loc.y;


		while (true) {
			++counter;
			if (counter > 15){ return null;};

			System.out.println("CENTER " + centeri + "," + centerj);
			score = this.locScores[centeri][centerj];

			double[] scores = {
					this.locScores[centeri-1][centerj-1],
					this.locScores[centeri][centerj-1],
					this.locScores[centeri+1][centerj-1],
					this.locScores[centeri+1][centerj],
					this.locScores[centeri+1][centerj+1],
					this.locScores[centeri][centerj+1],
					this.locScores[centeri-1][centerj+1],
					this.locScores[centeri-1][centerj],
			};

			while (true) {
				int bestIdx=0; double bestScore = scores[0];
				for (int i=0; i<scores.length; ++i){
					System.out.print(scores[i] + " ");
					if (scores[i] > bestScore) {
						bestIdx = i;
						bestScore = scores[i];
					}
				}
				System.out.println();
				System.out.println(bestIdx);
				if (score >= bestScore){
					return new MapLocation(centeri, centerj);
				}
				int x = centeri;
				int y = centerj;
				switch (bestIdx) {
				case 0: x = centeri-1; y = centerj-1; break;
				case 1: x = centeri; y = centerj-1; break;
				case 2: x = centeri+1; y = centerj-1; break;
				case 3: x = centeri+1; y = centerj; break;
				case 4: x = centeri+1; y = centerj+1; break;
				case 5: x = centeri; y = centerj+1; break;
				case 6: x = centeri-1; y = centerj+1; break;
				case 7: x = centeri-1; y = centerj-1; break;
				}
				if (this.myRC.senseTerrainTile(new MapLocation(x, y)) == TerrainTile.VOID){
					scores[bestIdx] = 0;
				}
				else {
					centeri = x; centerj = y;
					break;
				}
			}
		}
	}*/

    @Override
    protected void step() throws GameActionException {
		Robot[] nearbyEnemies = this.myRC.senseNearbyGameObjects(Robot.class, 10000, this.enemyTeam);
		
		if (this.myRC.isActive() && nearbyEnemies.length != 0) {
			this.shoot(nearbyEnemies);
		}
    	
		if (this.myRC.isActive() && this.myRC.senseRobotCount() < GameConstants.MAX_ROBOTS) {
			this.spawn();
		}
		
		//macrogame code goes here
		//adjustments if we're losing, game is about to end, if we're winning on milk, etc.
		
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
        		if (squadsMax){
        			assignPastureJob(order, pastrLocs.length);
        		} else {
        			if(enemyPastrs.length <= currentSquad)
        				assignScoutJob(order, this.enemyHQLoc);
        			else
        				assignScoutJob(order, enemyPastrs[currentSquad]);
        		}
        	}
        }
        
        //this.broadcastScoutJobs(enemyPastrs);
    }
    
    private boolean spawn() throws GameActionException {
		if (this.myRC.senseObjectAtLocation(this.myHQLoc.add(this.toEnemy)) == null){
			this.myRC.spawn(this.toEnemy);
			return true;
		}
		else {
	        for (Direction dir: BaseRobot.dirs){
	            if (this.myRC.senseObjectAtLocation(this.myHQLoc.add(dir)) == null){
	                this.myRC.spawn(dir);
	                return true;
	            }
	        }
		}
    	return false;
    }
    
    private boolean shoot(Robot[] nearbyEnemies) throws GameActionException{
    	MapLocation curloc;
    	
    	for(Robot r : nearbyEnemies){ //try to hit something directly
    		curloc = this.myRC.senseRobotInfo(r).location;
    		if(this.myRC.canAttackSquare(curloc)){
    			this.myRC.attackSquare(curloc);
    			return true;
    		}
    	}
    	
    	for(Robot r : nearbyEnemies){ //try to hit something for splash damage
    		curloc = this.myRC.senseRobotInfo(r).location;
    		curloc = curloc.add(curloc.directionTo(this.myHQLoc));
    		if(this.myRC.canAttackSquare(curloc)){
    			this.myRC.attackSquare(curloc);
    			return true;
    		}
    	}
    	
    	return false; //give up    	
    }
    
	private void assignPastureJob(int order, int n) throws GameActionException{
		MapLocation bestloc;
		if (this.PASTRLocs[n]==null){
			//System.out.println("here "+ n);
			bestloc = this.findBestPastureLoc();
			//System.out.println("recalculating");
			this.PASTRLocs[n]= bestloc;
			System.out.println("Assigned " + bestloc);
		} else {
			bestloc= this.PASTRLocs[n];
		}
		//System.out.println(bestloc.x+" "+bestloc.y);
		ActionMessage action = new ActionMessage(BaseRobot.State.PASTURE, 0, bestloc);
		int channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
		this.myRC.broadcast(channel, action.encode());
	}
	
	/*
	private void broadcastScoutJobs(MapLocation[] enemypastrs) throws GameActionException{
		for (int squad= 0; squad < BaseRobot.NUM_SQUADS; squad++){
			int channel = BaseRobot.SQUAD_BULLETIN_BASE+ squad;
			int currentJob = this.myRC.readBroadcast(channel);
			int enemyIndex = currentSquad+squad;
			if (squadAssignments[squad]!=0 && currentJob==0 && enemyIndex<enemypastrs.length){
				MapLocation nextEnemy= enemypastrs[enemyIndex];
				MapLocation rallyPoint= nextEnemy.add(nextEnemy.directionTo(this.myHQLoc), BaseRobot.RALLY_DISTANCE);
				ActionMessage msg= new ActionMessage(BaseRobot.State.SCOUT, squad, rallyPoint);
				this.myRC.broadcast(channel, msg.encode());
			}//only broadcasts enemyPastrs that have not already been assigned to other squads
			//If all squads have already been assigned, the squads that have finished their jobs go help other squads
		}
	}
	*/
	
	private void assignScoutJob(int order, MapLocation target) throws GameActionException{
		if (currentSquad< BaseRobot.NUM_SQUADS && squadAssignments[currentSquad]>=BaseRobot.MAX_SQUAD_SIZE){
			currentSquad++;
		}
		if (currentSquad>= BaseRobot.NUM_SQUADS){
			currentSquad = BaseRobot.NUM_SQUADS-1;
			squadsMax = true;
		}
		
		MapLocation rallyPoint = get_rally_point(target);
		ActionMessage action = new ActionMessage(BaseRobot.State.RALLY, currentSquad, rallyPoint); //tell this robot to go to the rally point
		
		//broadcast the squads to messageboard
		//this.myRC.broadcast(BaseRobot.SQUAD_ID_BASE+currentSquad*3+squadAssignments[currentSquad], order);
		squadAssignments[currentSquad]++;
		
		System.out.println("HQ assigning scout to squad number "+ currentSquad);
		
		int channel = BaseRobot.get_inbox_channel(order, BaseRobot.INBOX_ACTIONMESSAGE_CHANNEL);
		int mainChannel = BaseRobot.SQUAD_BASE + currentSquad * BaseRobot.SQUAD_OFFSET + SQUAD_RALLYPT_CHANNEL;
		
		this.myRC.broadcast(channel, action.encode());
		this.myRC.broadcast(mainChannel, action.encode()); //store rally point in squad channel
	}
	
	private MapLocation get_rally_point(MapLocation target){
		return this.myHQLoc.add(this.myHQLoc.directionTo(target), RALLY_DISTANCE);
	}
	
    
	/*protected MapLocation[] find_k_best_pasture_locations(int k){
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
	}*/
	

}
