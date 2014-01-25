package swarm3;
import battlecode.common.Direction;
import battlecode.common.MapLocation;

public class SearchNode {
	public SearchNode prevLoc;
	public MapLocation loc;
	public int length;
	public boolean isPivot = false;
	
	public SearchNode(MapLocation location, int length, SearchNode prevLoc){
		this.loc = location;
		this.length = length;
		this.prevLoc = prevLoc;
	}
	
	public SearchNode(MapLocation location, int length, SearchNode prevLoc, boolean isPivot){
		this.loc = location;
		this.length = length;
		this.prevLoc = prevLoc;
		this.isPivot = isPivot;
	}
	
	public SearchNode update(MapLocation location){
		return new SearchNode(location, this.length + 1, this);
	}
	
	public SearchNode update(MapLocation location, boolean isPivot){
		return new SearchNode(location, this.length + 1, this, isPivot);
	}
	
	public SearchNode update(Direction dir){
		return new SearchNode(this.loc.add(dir), this.length + 1, this);
	}
	
	public SearchNode update(Direction dir, boolean isPivot){
		return new SearchNode(this.loc.add(dir), this.length + 1, this, isPivot);
	}
	
	@Override
	public boolean equals(Object otherSN) {
		return (this.loc.x == ((SearchNode) otherSN).loc.x) && (this.loc.y == ((SearchNode) otherSN).loc.y);
	}
}