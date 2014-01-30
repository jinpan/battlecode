package diffnav;

import battlecode.common.*;
import java.util.ArrayList;
import java.util.LinkedList;

public class CoarseNode {
	
	public static final int BOX_SIZE = 4;
	public MapLocation state;
	public CoarseNode parent; 
	public int cost;
	public int mapHeight; public int mapWidth;
	
	public CoarseNode(MapLocation curLoc, CoarseNode prev, int actioncost, int w, int h) {
		this.state= new MapLocation(curLoc.x, curLoc.y);
		this.parent= prev;
		this.cost= actioncost;
		mapWidth= w; mapHeight = h;
	}
	
	public MapLocation getCenter() {
		int x= min(state.x*BOX_SIZE + BOX_SIZE/2, mapWidth-1);
		int y= min(state.y*BOX_SIZE + BOX_SIZE/2, mapHeight-1);
		return new MapLocation(x, y);
	}
	
	public int min(int a, int b){
		if (a<b)
			return a;
		return b;
	}
	
	public LinkedList<MapLocation> getPath() throws GameActionException {
		LinkedList<MapLocation> path= new LinkedList<MapLocation>();
		CoarseNode current = this;
		while (current!=null){
			path.add(0, current.getCenter());
			current = current.parent;
		}
		return path;
	}

}
