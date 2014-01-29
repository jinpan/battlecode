package team007;
import battlecode.common.MapLocation;

public class SearchNode {
	public SearchNode prevLoc;
	public MapLocation loc;
	public int length;
	public boolean isPivot = false;
	public SearchNode(MapLocation location, int length, SearchNode prevLoc, BaseRobot br){
		this.loc = location;
		this.prevLoc = prevLoc;
		this.length = length;
	}
	@Override
	public boolean equals(Object otherSN) {
		return (this.loc.x == ((SearchNode) otherSN).loc.x) && (this.loc.y == ((SearchNode) otherSN).loc.y);
	}
}