package swarm2;

import java.util.LinkedList;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.TerrainTile;

public class Navigation {
	static BaseRobot thisBot;
	static boolean debug;

	public static LinkedList<MapLocation> pathFind(MapLocation start, MapLocation target, BaseRobot theBot) throws GameActionException {
		thisBot = theBot;
		SearchNode bugSearch = bugSearchNew(start, target, theBot);
		SearchNode[] nodes = new SearchNode[bugSearch.length];
		int counter = bugSearch.length-1;
		while (!bugSearch.loc.equals(start)) {
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
	public static SearchNode bugSearchNew(MapLocation start, MapLocation target, BaseRobot theBot) throws GameActionException{
		thisBot = theBot;
		MapLocation ehqloc = target;
		MapLocation curr = start;
		int closestRight = curr.distanceSquaredTo(ehqloc);
		int closestLeft = curr.distanceSquaredTo(ehqloc);
		SearchNode current = new SearchNode(start, 1, null);
		SearchNode currentLeft = new SearchNode(start, 1, null);
		current.isPivot = true;
		currentLeft.isPivot = true;
		boolean isTracing = false;
		boolean isTracingLeft = false;
		Direction curDir = current.loc.directionTo(ehqloc);
		Direction curDirLeft = current.loc.directionTo(ehqloc);
		while (!(current.loc.x == ehqloc.x && current.loc.y == ehqloc.y) && !(currentLeft.loc.x == ehqloc.x && currentLeft.loc.y == ehqloc.y)) {
			if (debug) System.out.println("Right " + current.loc + " Left " + currentLeft.loc);
			if (!isTracing) {
				curDir = directionTo(current.loc, ehqloc);
				if (curDir != null /* && current.loc.distanceSquaredTo(ehqloc) <= closestRight*/) {
					if (debug) System.out.println("right step 1");
					current = new SearchNode(current.loc.add(curDir), current.length+1, current);
					if (current.loc.distanceSquaredTo(ehqloc) < closestRight)
						closestRight = current.loc.distanceSquaredTo(ehqloc);
				} else {
					isTracing = true;
					curDir = current.loc.directionTo(ehqloc);
					while (!isGood(current.loc.add(curDir))) {
						curDir = curDir.rotateLeft();
					}
					if (debug) System.out.println("right step 2");
					current = new SearchNode(current.loc.add(curDir), current.length+1, current);
					if (current.loc.distanceSquaredTo(ehqloc) < closestRight)
						closestRight = current.loc.distanceSquaredTo(ehqloc);
				}
			} else {
				curDir = directionTo(current.loc, ehqloc);
				if (curDir != null && current.loc.add(curDir).distanceSquaredTo(ehqloc) < closestRight) {
					isTracing = false;
					if (debug) System.out.println("Not tracing anymore. Distance of " + current.loc.add(curDir).distanceSquaredTo(ehqloc));
				} else {
					curDir = current.loc.directionTo(current.prevLoc.loc).rotateLeft().rotateLeft();
					int i = 2;
					while (!isGood(current.loc.add(curDir))) {
						curDir = curDir.rotateLeft();
						i++;
					}
					if (i < 4 || curDir != Direction.EAST && curDir != Direction.WEST && curDir != Direction.NORTH && curDir != Direction.SOUTH) {
						current.isPivot = true;
					}
					if (debug) System.out.println("right step 3");
					current = new SearchNode(current.loc.add(curDir), current.length+1, current);
					if (current.loc.distanceSquaredTo(ehqloc) < closestRight)
						closestRight = current.loc.distanceSquaredTo(ehqloc);
				}
			}
			
			if ((current.loc.x == ehqloc.x && current.loc.y == ehqloc.y) || (currentLeft.loc.x == ehqloc.x && currentLeft.loc.y == ehqloc.y)){
				break;
			}
			
			if (debug) System.out.println(currentLeft.loc);
			if (!isTracingLeft) {
				curDirLeft = directionTo(currentLeft.loc, ehqloc);
				if (curDirLeft != null /* && current.loc.distanceSquaredTo(ehqloc) <= closestRight*/) {
					if (debug) System.out.println("left step 1");
					currentLeft = new SearchNode(currentLeft.loc.add(curDirLeft), currentLeft.length+1, currentLeft);
					if (currentLeft.loc.distanceSquaredTo(ehqloc) < closestLeft)
						closestLeft = currentLeft.loc.distanceSquaredTo(ehqloc);
				} else {
					isTracingLeft = true;
					curDirLeft = currentLeft.loc.directionTo(ehqloc);
					while (!isGood(currentLeft.loc.add(curDirLeft))) {
						curDirLeft = curDirLeft.rotateRight();
					}
					if (debug) System.out.println("left step 2");
					currentLeft = new SearchNode(currentLeft.loc.add(curDirLeft), currentLeft.length+1, currentLeft);
					if (currentLeft.loc.distanceSquaredTo(ehqloc) < closestLeft)
						closestLeft = currentLeft.loc.distanceSquaredTo(ehqloc);
				}
			} else {
				curDirLeft = directionTo(currentLeft.loc, ehqloc);
				if (curDirLeft != null && currentLeft.loc.add(curDirLeft).distanceSquaredTo(ehqloc) < closestLeft) {
					isTracingLeft = false;
				} else {
					curDirLeft = currentLeft.loc.directionTo(currentLeft.prevLoc.loc).rotateRight().rotateRight();
					int j = 2;
					while (!isGood(currentLeft.loc.add(curDirLeft))) {
						curDirLeft = curDirLeft.rotateRight();
						j++;
					}
					if (j < 4 || curDirLeft != Direction.EAST && curDirLeft != Direction.WEST && curDirLeft != Direction.NORTH && curDirLeft != Direction.SOUTH) {
						currentLeft.isPivot = true;
					}
					if (debug) System.out.println("left step 3");
					currentLeft = new SearchNode(currentLeft.loc.add(curDirLeft), currentLeft.length+1, currentLeft);
					if (currentLeft.loc.distanceSquaredTo(ehqloc) < closestLeft)
						closestLeft = currentLeft.loc.distanceSquaredTo(ehqloc);
				}
			}
		}
		currentLeft.isPivot = true;
		current.isPivot = true;
		if (current.loc.equals(ehqloc)) {
			if (debug) System.out.println("Right bug");
			return current;
		} else {
			if (debug) System.out.println("Left bug");
			return currentLeft;
		}
	}
	
	public static boolean isGood(MapLocation loc) {
		return (thisBot.myRC.senseTerrainTile(loc) == TerrainTile.NORMAL ||
				thisBot.myRC.senseTerrainTile(loc) == TerrainTile.ROAD);
	}
}
