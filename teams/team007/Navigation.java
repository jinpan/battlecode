package team007;

import java.util.LinkedList;
import team007.SearchNode;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.TerrainTile;

public class Navigation {
	static BaseRobot thisBot;
	
	public static SearchNode bugSearch(MapLocation start, MapLocation target, BaseRobot theBot) throws GameActionException{
		thisBot = theBot;
		boolean mline[][] = new boolean[100][100];
		MapLocation ehqloc = target;
		MapLocation curr = start;
		// optimize, kinda slow right now. -> lol no it's good enough.
		// Initialize mline array for easy lookup later
		while (!(curr.x == ehqloc.x && curr.y == ehqloc.y)) {
			mline[curr.x][curr.y] = true;
			curr = curr.add(curr.directionTo(ehqloc));
		}
		// Actual bug iteration
		SearchNode current = new SearchNode(start, 1, null, thisBot);
		SearchNode currentLeft = new SearchNode(start, 1, null, thisBot);
		current.isPivot = true;
		currentLeft.isPivot = true;
		Direction curDir = current.loc.directionTo(ehqloc);
		Direction curDirLeft = current.loc.directionTo(ehqloc);
		while (!(current.loc.x == ehqloc.x && current.loc.y == ehqloc.y) && !(currentLeft.loc.x == ehqloc.x && currentLeft.loc.y == ehqloc.y)) {
			// If on the m-line, and can move forward, move forward towards the enemy HQ.
			if (thisBot.myRC.getRobot().getID() == 1366) {
				System.out.println("right: " + current.loc + " left: " + currentLeft.loc);
				System.out.println("right dir: " + curDir + " left dir: " + curDirLeft);
			}
			boolean canMoveForward = (thisBot.myRC.senseTerrainTile(current.loc.add(curDir)) == TerrainTile.NORMAL ||
					thisBot.myRC.senseTerrainTile(current.loc.add(curDir)) == TerrainTile.ROAD);
			boolean canMoveForwardLeft = (thisBot.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft)) == TerrainTile.NORMAL ||
					thisBot.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft)) == TerrainTile.ROAD);
			if (mline[current.loc.x][current.loc.y] && (thisBot.myRC.senseTerrainTile(current.loc.add(current.loc.directionTo(ehqloc))) == TerrainTile.NORMAL ||
					thisBot.myRC.senseTerrainTile(current.loc.add(current.loc.directionTo(ehqloc))) == TerrainTile.ROAD)) {
				if (thisBot.myRC.getRobot().getID() == 1366) {
					System.out.println("right step 1");
				}
				curDir = current.loc.directionTo(ehqloc);
				current = new SearchNode(current.loc.add(curDir), current.length+1, current, thisBot);
			}
			// If can move forward, and right hand touching wall, move forward
			else if (canMoveForward && thisBot.myRC.senseTerrainTile(current.loc.add(curDir.rotateRight().rotateRight())) == TerrainTile.VOID) {
				if (thisBot.myRC.getRobot().getID() == 1366) {
					System.out.println("right step 2");
				}

				current = new SearchNode(current.loc.add(curDir), current.length + 1, current, thisBot);
			}
			// If right hand side is empty, turn right and move forward
			else if (canMoveForward && (thisBot.myRC.senseTerrainTile(current.loc.add(curDir.rotateRight().rotateRight())) == TerrainTile.NORMAL ||
					thisBot.myRC.senseTerrainTile(current.loc.add(curDir.rotateRight().rotateRight())) == TerrainTile.ROAD)) {
				if (thisBot.myRC.getRobot().getID() == 1366) {
					System.out.println("right step 3");
				}
				curDir = curDir.rotateRight().rotateRight();
				current.isPivot = true;
				current = new SearchNode(current.loc.add(curDir), current.length + 1, current, thisBot);
			}
			
			// Only condition for this else should be that the robot cannot move forward and has a wall on the right. Therefore just turn left and move. Report corner.
			else {
				if (thisBot.myRC.getRobot().getID() == 1366) {
					System.out.println("right step 4");
				}
				curDir = curDir.rotateLeft();
				if (!(thisBot.myRC.senseTerrainTile(current.loc.add(curDir)) == TerrainTile.NORMAL ||
						thisBot.myRC.senseTerrainTile(current.loc.add(curDir)) == TerrainTile.ROAD)) {
					curDir = curDir.rotateLeft();
				}
				if (!(thisBot.myRC.senseTerrainTile(current.loc.add(curDir)) == TerrainTile.NORMAL ||
						thisBot.myRC.senseTerrainTile(current.loc.add(curDir)) == TerrainTile.ROAD)) {
					curDir = curDir.rotateLeft();
				}
				if (thisBot.myRC.senseTerrainTile(current.loc.add(curDir.rotateRight().rotateRight().rotateRight().rotateRight())) == TerrainTile.VOID) {
					//System.out.println("Corner found at " + current.loc);
				}
				current = new SearchNode(current.loc.add(curDir), current.length + 1, current, thisBot);
			}
			if (mline[currentLeft.loc.x][currentLeft.loc.y] && (thisBot.myRC.senseTerrainTile(currentLeft.loc.add(currentLeft.loc.directionTo(ehqloc))) == TerrainTile.NORMAL ||
					thisBot.myRC.senseTerrainTile(currentLeft.loc.add(currentLeft.loc.directionTo(ehqloc))) == TerrainTile.ROAD)) {
				if (thisBot.myRC.getRobot().getID() == 1366) {
					System.out.println("left step 1");
				}
				curDirLeft = currentLeft.loc.directionTo(ehqloc);
				currentLeft = new SearchNode(currentLeft.loc.add(curDirLeft), currentLeft.length+1, currentLeft, thisBot);
			}
			// If can move forward, and right hand touching wall, move forward
			else if (canMoveForwardLeft && thisBot.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft.rotateLeft().rotateLeft())) == TerrainTile.VOID) {
				if (thisBot.myRC.getRobot().getID() == 1366) {
					System.out.println("left step 2");
				}
				currentLeft = new SearchNode(currentLeft.loc.add(curDirLeft), currentLeft.length + 1, currentLeft, thisBot);
			}
			// If right hand side is empty, turn right and move forward
			else if (canMoveForwardLeft && (thisBot.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft.rotateLeft().rotateLeft())) == TerrainTile.NORMAL ||
					thisBot.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft.rotateLeft().rotateLeft())) == TerrainTile.ROAD)) {
				if (thisBot.myRC.getRobot().getID() == 1366) {
					System.out.println("left step 3");
				}
				curDirLeft = curDirLeft.rotateLeft().rotateLeft();
				currentLeft.isPivot = true;
				currentLeft = new SearchNode(currentLeft.loc.add(curDirLeft), currentLeft.length + 1, currentLeft, thisBot);
			}
			// Only condition for this else should be that the robot cannot move forward and has a wall on the right. Therefore just turn left and move. Report corner.
			else {
				if (thisBot.myRC.getRobot().getID() == 1366) {
					System.out.println("left step 4");
				}
				curDirLeft = curDirLeft.rotateRight();
				if (!(thisBot.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft)) == TerrainTile.NORMAL ||
						thisBot.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft)) == TerrainTile.ROAD)) {
					curDirLeft = curDirLeft.rotateRight();
				}
				if (!(thisBot.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft)) == TerrainTile.NORMAL ||
						thisBot.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft)) == TerrainTile.ROAD)) {
					curDirLeft = curDirLeft.rotateRight();
				}
				if (thisBot.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft.rotateLeft().rotateLeft().rotateLeft().rotateLeft())) == TerrainTile.VOID) {
					//System.out.println("Corner found at " + currentLeft.loc);
				}
				currentLeft = new SearchNode(currentLeft.loc.add(curDirLeft), currentLeft.length + 1, currentLeft, thisBot);
			}
		}
		currentLeft.isPivot = true;
		current.isPivot = true;
		if (current.loc.equals(ehqloc))
			return current;
		else
			return currentLeft;
	}
	public static LinkedList<MapLocation> pathFind(MapLocation start, MapLocation target, BaseRobot theBot) throws GameActionException {
		thisBot = theBot;
		SearchNode bugSearch = bugSearch(start, target, theBot);
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
		// // Get rid of some pivots cause we can. MUCH IMPROVEMENT. SUCH WOW. :')
		// MapLocation pivotArray[] = new MapLocation[pivots.size()];
		// int temp = 0;
		// for (MapLocation pivot: pivots) {
		// pivotArray[temp] = pivot;
		// temp++;
		// }
		// int first = 0;
		// LinkedList<MapLocation> finalList = new LinkedList<MapLocation>();
		// finalList.add(pivotArray[first]);
		// for (int i = pivots.size()-1; i > first; i--) {
		// if (first == pivots.size()-1) {
		// break;
		// }
		// if (canTravel(pivotArray[first], pivotArray[i])) {
		// finalList.add(pivotArray[i]);
		// first = i;
		// i = pivots.size();
		// } else if (i - first == 1) {
		// LinkedList<MapLocation> newpath = pathFind(pivotArray[first], pivotArray[i]);
		// newpath.remove();
		// finalList.addAll(newpath);
		// first = i;
		// i = pivots.size();
		// }
		// }
		//
		// return finalList;
	}
	public static boolean canTravel(MapLocation start, MapLocation target) {
		while (!(start.x == target.x && start.y == target.y)) {
			if (thisBot.myRC.senseTerrainTile(start) == TerrainTile.VOID) return false;
			start = start.add(start.directionTo(target));
		}
		return true;
	}

}
