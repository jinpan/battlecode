package team007oldoldold;

import battlecode.common.MapLocation;
import java.util.ArrayList;

public class PastureBlock {
	
	MapLocation vertex;
	int width; int height;
	int dx; int dy;
	
	public PastureBlock(MapLocation vert, int x, int y, int xadd, int yadd){
		this.vertex= vert; 
		this.width= x; 
		this.height=y;
		this.dx = xadd; this.dy = yadd;
	}
	
	public MapLocation center(){
		return new MapLocation(vertex.x+(width/2)*dx, vertex.y+(height/2)*dy);
	}
	
	public boolean contains(MapLocation loc){
		boolean xin= loc.x>=vertex.x && loc.x<=vertex.x+width;
		boolean yin= loc.y>=vertex.y && loc.y<=vertex.y+height;
		return xin && yin;
	}
	
	public String toString() {
		return "Vertex: " + vertex + ", width: " + width + ", height: " + height;
	}

}
