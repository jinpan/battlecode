package sprintBot;

import battlecode.common.TerrainTile;

public class temp {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

if (mline[currentLeft.loc.x][currentLeft.loc.y] && (this.myRC.senseTerrainTile(currentLeft.loc.add(currentLeft.loc.directionTo(ehqloc))) == TerrainTile.NORMAL || 
this.myRC.senseTerrainTile(currentLeft.loc.add(currentLeft.loc.directionTo(ehqloc))) == TerrainTile.ROAD)) {
	curDirLeft = currentLeft.loc.directionTo(ehqloc);
	currentLeft = new SearchNode(currentLeft.loc.add(curDirLeft), currentLeft.length+1, currentLeft, this);
}
// If can move forward, and right hand touching wall, move forward
else if (canMoveForwardLeft && this.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft.rotateRight().rotateRight())) == TerrainTile.VOID) {
	currentLeft = new SearchNode(currentLeft.loc.add(curDirLeft), currentLeft.length + 1, currentLeft, this);
}
// If right hand side is empty, turn right and move forward
else if (canMoveForwardLeft && (this.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft.rotateRight().rotateRight())) == TerrainTile.NORMAL || 
this.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft.rotateRight().rotateRight())) == TerrainTile.ROAD)) {
	curDirLeft = curDirLeft.rotateRight().rotateRight();
	currentLeft.isPivot = true;
	currentLeft = new SearchNode(currentLeft.loc.add(curDirLeft), currentLeft.length + 1, currentLeft, this);
} 
// Only condition for this else should be that the robot cannot move forward and has a wall on the right. Therefore just turn left and move. Report corner.
else {
	curDirLeft = curDirLeft.rotateLeft();
	if (!(this.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft)) == TerrainTile.NORMAL || 
			this.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft)) == TerrainTile.ROAD)) {
		curDirLeft = curDirLeft.rotateLeft();
	}
	if (!(this.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft)) == TerrainTile.NORMAL || 
			this.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft)) == TerrainTile.ROAD)) {
		curDirLeft = curDirLeft.rotateLeft();
	}
	if (this.myRC.senseTerrainTile(currentLeft.loc.add(curDirLeft.rotateRight().rotateRight().rotateRight().rotateRight())) == TerrainTile.VOID) {
		//System.out.println("Corner found at " + currentLeft.loc);
	}
	currentLeft = new SearchNode(currentLeft.loc.add(curDirLeft), currentLeft.length + 1, currentLeft, this);
}
