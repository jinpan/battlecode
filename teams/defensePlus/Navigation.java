package defensePlus;

import java.util.LinkedList;
import java.util.ListIterator;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.TerrainTile;

public class Navigation {
	SoldierPlayer myRobot;
	RobotController rc;
	boolean debug;
	int[][] map;
	
	
	public Navigation(SoldierPlayer r){
		this.myRobot = r;
		this.rc = myRobot.myRC;
		map = new int[rc.getMapWidth()][rc.getMapHeight()];
	}

	public LinkedList<MapLocation> pathFind(MapLocation start, MapLocation target) throws GameActionException {
//		for (int i = 0; i < myRobot.allies.length; i++) {
//			RobotInfo r = rc.senseRobotInfo(myRobot.allies[i]);
//			if (myRobot.allies[i].getID() == myRobot.ID) continue;
//			map[r.location.x][r.location.y] = -2;
//		}
//		for (int i = 0; i < myRobot.enemies.length; i++) {
//			RobotInfo r = rc.senseRobotInfo(myRobot.enemies[i]);
//			map[r.location.x][r.location.y] = -2;
//		}
		int x = Clock.getRoundNum();
		SearchNode bugSearch = bugSearch(start, target);
		SearchNode[] nodes = new SearchNode[bugSearch.length];
		int counter = bugSearch.length-1;
		while (bugSearch.prevLoc != null){
			nodes[counter] = bugSearch;
			bugSearch = bugSearch.prevLoc;
			counter--;
		}
		
		nodes[0] = bugSearch;
		LinkedList<MapLocation> pivots = new LinkedList<MapLocation>();
		pivots.add(nodes[0].loc);
		
		for (int i = 1; i < nodes.length; i++) {
			if (nodes[i].isPivot) {
				pivots.add(nodes[i].loc);
			}
		}
		
		counter = 0;
		ListIterator<MapLocation> li1 = pivots.listIterator(), li2;
		while (li1.hasNext()){
			 li2 = pivots.listIterator(pivots.size());
			 while (li2.hasPrevious() && li2.previousIndex() > li1.nextIndex() + 1){
				 if (canTravel(li1.next(), li2.previous())){
					 pivots.subList(li1.nextIndex(), li2.previousIndex() + 1).clear();
					 li1 = pivots.listIterator(++counter);
					 break;
				 }
				 li1.previous();
			 }
			 li1.next();
		}
		
		System.out.println(Clock.getRoundNum() - x);
		return pivots;
	}
	
	public boolean canTravel(MapLocation source, MapLocation target) {
		 while (!(source.x == target.x && source.y == target.y)) {
			source = source.add(source.directionTo(target));
			if (!isGood(source)) return false;
		}
		return true;
	}
	
	public SearchNode bugSearch(MapLocation start, MapLocation target) throws GameActionException{
		MapLocation s = start, t = target;
		int closest = s.distanceSquaredTo(t);
		int closestL = closest, closestR = closest;
		SearchNode current = new SearchNode(start, 1, null, true), currentL = new SearchNode(start, 1, null, true), currentR = new SearchNode(start, 1, null, true);
		boolean isTracingL = false, isTracingR = false;
		Direction curDir = current.loc.directionTo(t);
		Direction curDirL = curDir, curDirR = curDir;
		
		if (debug) System.out.println("Source: " + s + "; Target: " + t);
		while (!(current.loc.x == t.x && current.loc.y == t.y) 
				&& !(currentL.loc.x == t.x && currentL.loc.y == t.y)
				&& !(currentR.loc.x == t.x && currentR.loc.y == t.y)) {
			if (debug) System.out.println("Current: " + current.loc + ";Right " + currentR.loc + ";Left " + currentL.loc);
			
			if (!isTracingL || !isTracingR){
				// we're done tracing
				if (isTracingL){
					// the right bug finished first
					isTracingL = false;
					current = currentR;
					closest = closestR;
				}
				else if (isTracingR){
					// the left bug finished first
					isTracingR = false;
					current = currentL;
					closest = closestL;
				}
				curDir = directionTo(current.loc, t);
				if (curDir != null){
					current = current.update(curDir);
				}
				else {
					current.isPivot = true;
					closest = current.loc.distanceSquaredTo(t);
					closestL = closest; closestR = closest;
					isTracingL = true; isTracingR = true;
					curDir = current.loc.directionTo(t);
					curDirL = curDir; curDirR = curDir;
					
					while (!isGood(current.loc.add(curDirL))){
						curDirL = curDirL.rotateRight();
					}
					while (!isGood(current.loc.add(curDirR))){
						curDirR = curDirR.rotateLeft();
					}
					currentL = current.update(curDirL);
					currentR = current.update(curDirR);
					if (currentL.loc.distanceSquaredTo(t) < closestL)
						closestL = currentL.loc.distanceSquaredTo(t);
					if (currentR.loc.distanceSquaredTo(t) < closestR)
						closestR = currentR.loc.distanceSquaredTo(t);
				}
			}
			else { // we're tracing
				if (currentL.loc.distanceSquaredTo(t) < currentR.loc.distanceSquaredTo(t)){
					if (debug) System.out.println("LEFT TRACE");
					// the left trace is closer
					curDirL = directionTo(currentL.loc, t);
					if (curDirL != null && currentL.loc.add(curDirL).distanceSquaredTo(t) < closestL) {
						if (debug) System.out.print("FINISH LEFT TRACE. GOING " + curDirL);
						isTracingL = false;
						currentL.isPivot = true;
						if (debug) System.out.println("LEFT PIVOT");
						currentL = currentL.update(curDirL);
						if (currentL.loc.distanceSquaredTo(t) < closestL)
							closestL = currentL.loc.distanceSquaredTo(t);
					} else {
						curDirL = currentL.loc.directionTo(currentL.prevLoc.loc).rotateRight().rotateRight();
						int i = 2;
						while (!isGood(currentL.loc.add(curDirL))) {
							curDirL = curDirL.rotateRight();
							i++;
						}
						if (i < 4 || curDirL != Direction.EAST && curDirL != Direction.WEST && curDirL != Direction.NORTH && curDirL != Direction.SOUTH) {
							currentL.isPivot = true;
							if (debug) System.out.println("LEFT PIVOT");
						}
						if (curDirL != directionTo(currentL.prevLoc.loc, currentL.loc)){
							currentL.isPivot = true;
							if (debug) System.out.println("LEFT PIVOT");
						}
						currentL = currentL.update(curDirL);
						if (currentL.loc.distanceSquaredTo(t) < closestL)
							closestL = currentL.loc.distanceSquaredTo(t);
					}
				}
				
				else {
					// the right trace is closer
					if (debug) System.out.println("RIGHT TRACE");
					curDirR = directionTo(currentR.loc, t);
					if (curDirR != null && currentR.loc.add(curDirR).distanceSquaredTo(t) < closestR) {
						if (debug) System.out.println("FINISH RIGHT TRACE. GOING " + curDirR);
						isTracingR = false;
						currentR.isPivot = true;
						if (debug) System.out.println("RIGHT PIVOT");
						currentR = currentR.update(curDirR);
						if (currentR.loc.distanceSquaredTo(t) < closestR)
							closestR = currentR.loc.distanceSquaredTo(t);
					} else {
						curDirR = currentR.loc.directionTo(currentR.prevLoc.loc).rotateLeft().rotateLeft();
						int i = 2;
						while (!isGood(currentR.loc.add(curDirR))) {
							curDirR = curDirR.rotateLeft();
							i++;
						}
						if (i < 4 || curDirR != Direction.EAST && curDirR != Direction.WEST && curDirR != Direction.NORTH && curDirR != Direction.SOUTH) {
							currentR.isPivot = true;
							if (debug) System.out.println("RIGHT PIVOT");
						}
						if (curDirR != directionTo(currentR.prevLoc.loc, currentR.loc)){
							currentR.isPivot = true;
							if (debug) System.out.println("RIGHT PIVOT");
						}
						currentR = currentR.update(curDirR);
						if (currentR.loc.distanceSquaredTo(t) < closestR)
							closestR = currentR.loc.distanceSquaredTo(t);
					}
				}
			}
		}

		current.isPivot = true;
		currentL.isPivot = true;
		currentR.isPivot = true;
		if (current.loc.equals(t)){
			return current;
		}
		if (currentL.loc.equals(t)){
			return currentL;
		}
		if (currentR.loc.equals(t)){
			return currentR;
		}
		throw new GameActionException(null, "Unable to find a path from " + s + " to " + t);
	}
	
	protected Direction directionTo(MapLocation source, MapLocation target) throws GameActionException {
		Direction dir = source.directionTo(target);
		if (rc.senseTerrainTile(source.add(dir)).ordinal() < 2)
			return dir;
		else {
			Direction candidateDir = dir.rotateLeft();
			if (isGood(source.add(candidateDir))){
				return candidateDir;
			}
			candidateDir = dir.rotateLeft();
			if (isGood(source.add(candidateDir))){
				return candidateDir;
			}
			return null;
		}
	}
	
	public boolean isGood(MapLocation loc) {
		if (loc.x >= map.length || loc.y >= map[0].length) return false;
		if (loc.distanceSquaredTo(this.myRobot.enemyHQLoc) <= 25) return false;
		int ans = map[loc.x][loc.y];
		if (ans < 0) return false;
		if (ans > 0) return true;
		
		TerrainTile tile = rc.senseTerrainTile(loc);
		if (tile == TerrainTile.NORMAL){
			map[loc.x][loc.y] = 1;
			return true;
		}
		if (tile == TerrainTile.ROAD){
			map[loc.x][loc.y] = 2;
			return true;
		}
		else {
			map[loc.x][loc.y] = -1;
			return false;
		}
	}
}
