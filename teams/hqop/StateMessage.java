package hqop;


public class StateMessage implements Message {
	
	BaseRobot.State state;
	
	public StateMessage(BaseRobot.State state){
        this.state = state;
    }
    
    public long encode(){
    	int state;

        switch (this.state) {
        	case DEFAULT: state = 0x0; break;
        	case ATTACK: state = 0x1; break;
        	case DEFEND: state = 0x2; break;
        	case HERD: state = 0x3; break;
        	case PASTURIZE: state = 0x4; break;
        	default: state = 0x0;
        }
        return state;
    }
    
    public BaseRobot.State toState(){
    	return this.state;
    }
    
    public static StateMessage decode(long message){
        BaseRobot.State state;
        switch ((int) message) {
	    	case 0x0: state = BaseRobot.State.DEFAULT; break;
	    	case 0x1: state = BaseRobot.State.ATTACK; break;
	    	case 0x2: state = BaseRobot.State.DEFEND; break;
	    	case 0x3: state = BaseRobot.State.HERD; break;
	    	case 0x4: state = BaseRobot.State.PASTURIZE; break;
    	default: state = BaseRobot.State.DEFAULT; break;
    }
        
        return new StateMessage(state);
    }
}
