package swarm3;

import java.util.LinkedList;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.TerrainTile;

public class Navigation {
	static BaseRobot thisBot;
	static boolean debug;

	public static LinkedList<MapLocation> pathFind(MapLocation start, MapLocation target, BaseRobot theBot) throws GameActionException {
		thisBot = theBot;
		SearchNode bugSearch = bugSearch(start, target, theBot);
		SearchNode[] nodes = new SearchNode[bugSearch.length];
		int counter = bugSearch.length-1;
		while (bugSearch.prevLoc != null){
			if (theBot.ID == 223 && Clock.getRoundNum() < 50){
				System.out.println(bugSearch.loc + " " + bugSearch.isPivot);
			}
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
		return pivots;
	}
	public static boolean canTravel(MapLocation start, MapLocation target) {
		while (!(start.x == target.x && start.y == target.y)) {
			if (thisBot.myRC.senseTerrainTile(start) == TerrainTile.VOID) return false;
			start = start.add(start.directionTo(target));
		}
		return true;
	}
	
	protected static Direction directionTo(MapLocation curr, MapLocation loc) throws GameActionException {
		if (thisBot.myRC.senseTerrainTile(curr.add(curr.directionTo(loc))) == TerrainTile.NORMAL ||
					thisBot.myRC.senseTerrainTile(curr.add(curr.directionTo(loc))) == TerrainTile.ROAD)
			return curr.directionTo(loc);
		else
			return null;
	}
	public static SearchNode bugSearch(MapLocation start, MapLocation target, BaseRobot theBot) throws GameActionException{
		thisBot = theBot;
		MapLocation s = start, t = target;
		int closestL = s.distanceSquaredTo(t);
		int closestR = closestL;
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
				}
				else if (isTracingR){
					// the left bug finished first
					isTracingR = false;
					current = currentL;
				}
				curDir = directionTo(current.loc, t);
				if (curDir != null){
					current = current.update(curDir);
				}
				else {
					current.isPivot = true;
					isTracingL = true; isTracingR = true;
					curDir = current.loc.directionTo(t);
					curDirL = curDir; curDirR = curDir;
					
					while (!isGood(currentL.loc.add(curDirL))){
						curDirL = curDirL.rotateRight();
					}
					while (!isGood(currentR.loc.add(curDirR))){
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
					// the left trace is closer
					curDirL = directionTo(currentL.loc, t);
					if (curDirL != null && currentL.loc.add(curDirL).distanceSquaredTo(t) < closestL) {
						isTracingL = false;
						currentL.isPivot = true;
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
						}
						currentL = currentL.update(curDirL);
						if (currentL.loc.distanceSquaredTo(t) < closestL)
							closestL = currentL.loc.distanceSquaredTo(t);
					}
				}
				
				else {
					// the right trace is closer
					curDirR = directionTo(currentR.loc, t);
					if (curDirR != null && currentR.loc.add(curDirR).distanceSquaredTo(t) < closestR) {
						isTracingR = false;
						currentR.isPivot = true;
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
	
	public static boolean isGood(MapLocation loc) {
		return (thisBot.myRC.senseTerrainTile(loc).ordinal() < 2);
	}
}
