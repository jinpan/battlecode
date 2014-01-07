package jinpan;


public class StateMessage implements Message {
	
	BaseRobot.State myState;
	
	public StateMessage(BaseRobot.State state){
        this.myState = state;
    }
    
    public int encode(){
    	int state;
        switch (this.myState) {
        	case DEFAULT: state = 0x0; break;
        	case ATTACK: state = 0x1; break;
        	case DEFENSE: state = 0x2; break;
        	case PASTURE: state = 0x3; break;
        	case SCOUT: state = 0x4; break;
        	case ATTACKHIGH: state = 0x5; break;
        	case DEFENSEHIGH: state = 0x6; break;
        	case PASTUREHIGH: state = 0x7; break;
        	case SCOUTHIGH: state = 0x8; break;
        	default: state = 0x0;
        }
        return state;
    }
    
    public BaseRobot.State toState(){
    	return this.myState;
    }
    
    public static StateMessage decode(int message){
        BaseRobot.State myState;
        switch (message) {
        	case 0x0: myState = BaseRobot.State.DEFAULT; break;
        	case 0x1: myState = BaseRobot.State.ATTACK; break;
        	case 0x2: myState = BaseRobot.State.DEFENSE; break;
        	case 0x3: myState = BaseRobot.State.PASTURE; break;
        	case 0x4: myState = BaseRobot.State.SCOUT; break;
        	case 0x5: myState = BaseRobot.State.ATTACKHIGH; break;
        	case 0x6: myState = BaseRobot.State.DEFENSEHIGH; break;
        	case 0x7: myState = BaseRobot.State.PASTUREHIGH; break;
        	case 0x8: myState = BaseRobot.State.SCOUTHIGH; break;
        	default: myState = BaseRobot.State.DEFAULT; break;
        }
        
        return new StateMessage(myState);
    }
}
