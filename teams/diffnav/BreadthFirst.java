package diffnav;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;

public class BreadthFirst {
	
	public static RobotController myRC;
	public static int boxsize;
	public static int[][] internalMap;
	public static int mapWidth; 
	public static int mapHeight; 
	
    public static final Direction[] dirs = {
        Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
        Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    
	
	public static void init(int[][] HQMap, int w, int h, RobotController rc, int boxes) {
		myRC = rc;
		boxsize = boxes;
		internalMap = HQMap;
		mapWidth = w;
		mapHeight = h;
	}
	
	public static LinkedList<MapLocation> search(CoarseNode start, MapLocation goal) throws GameActionException {
		LinkedList<CoarseNode> queue = new LinkedList<CoarseNode>();
		ArrayList<MapLocation> visited = new ArrayList<MapLocation>();
		int mincost= start.cost;
		queue.add(start);
		visited.add(start.state);
		if (start.getCenter().distanceSquaredTo(goal) <= 4) {
			return start.getPath();
		}
		while (queue.size()!= 0) {
			CoarseNode current;
			int index= 0;
			for (int i=0; i<queue.size(); i++){
				CoarseNode node = queue.get(i);
				if (node.cost<=mincost) {
					mincost= node.cost; index= i; 
				}
			} current = queue.remove(index);
			ArrayList<CoarseNode> children = getChildren(current, goal);
			for (CoarseNode child: children){
				if (visited.contains(child.state)){
					//System.out.println("continued from " + child.state+ " "+ child.getCenter());
					continue;
				}
				if (child.state.equals(goal)) {
					return child.getPath();
				} 
				queue.add(child);
				//System.out.println("added " + child.state+ " "+ child.getCenter());
				visited.add(child.state);
			}
		} return null;
	}
	
	public static ArrayList<CoarseNode> getChildren(CoarseNode current, MapLocation goal) {
		ArrayList<CoarseNode> children = new ArrayList<CoarseNode>();
		for (int i=0; i<8; i++) {
			MapLocation candidate = current.state.add(dirs[i]);
			if (candidate.x>=internalMap.length || candidate.x<=-1 || candidate.y<=-1|| candidate.y>=internalMap[0].length){
				continue;
			}
			if (internalMap[candidate.x][candidate.y] < 100) {
				CoarseNode child = new CoarseNode(candidate, current, heuristic(candidate, goal), mapWidth, mapHeight);
				//System.out.println(child.state);
				children.add(child);
			}
		}
		return children; 
	}
	
	public static int heuristic(MapLocation current, MapLocation goal) {
		return (current.x - goal.x)*(current.x-goal.x) + (current.y- goal.y)*(current.y-goal.y);
	}

}
