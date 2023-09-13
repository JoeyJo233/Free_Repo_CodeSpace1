package tddc17;


import aima.core.environment.liuvacuum.*;
import aima.core.agent.Action;
import aima.core.agent.AgentProgram;
import aima.core.agent.Percept;
import aima.core.agent.impl.*;

import java.util.*;



class MyAgentState
{
	public static final int WIDTH = 30;
	public static final int HEIGHT = 30;
	public int[][] world = new int[HEIGHT][WIDTH];
	public boolean initialized = false;
	final int WALL 		= 0;
	final int UNKNOWN 	= 1;//0b0001
	final int DIRT		= 2;//0b0010
	final int CLEAR 	= 4;//0b0100
	final int HOME		= 8;//0b1000
	final int ERROR		= -1;
	final int ACTION_NONE 			= 0;
	final int ACTION_MOVE_FORWARD 	= 1;
	final int ACTION_TURN_RIGHT 	= 2;
	final int ACTION_TURN_LEFT 		= 3;
	final int ACTION_SUCK	 		= 4;
	
	public int agent_row_idx = 1;
	public int agent_col_idx = 1;
	public int agent_last_action = ACTION_NONE;
	
	public static final int EAST = 0;
	public static final int NORTH = 1;
	public static final int WEST = 2;
	public static final int SOUTH = 3;
	public int agent_direction = EAST;
	
	MyAgentState()
	{
        for (int[] ints : world) Arrays.fill(ints, UNKNOWN);
//		world[1][1] = HOME;
		agent_last_action = ACTION_NONE;
	}
	public void reset(){
		for (int[] ints : world) Arrays.fill(ints, UNKNOWN);
		agent_last_action = ACTION_NONE;
		agent_row_idx = 1;
		agent_col_idx = 1;
		agent_direction = EAST;
		initialized = false;
	}
	// Based on the last action and the received percept updates the x & y agent position
	public void updatePosition(DynamicPercept p)//todo: does this operation synced with Pacman actions?
	{
		Boolean bump = (Boolean)p.getAttribute("bump");

		if (agent_last_action==ACTION_MOVE_FORWARD && !bump)// agent move forward and doesn't bump into obstacles
	    {
            switch (agent_direction) {
                case MyAgentState.NORTH -> {
						agent_row_idx--;
				}
                case MyAgentState.EAST -> {
						agent_col_idx++;
				}
                case MyAgentState.SOUTH ->{
						agent_row_idx++;
				}
                case MyAgentState.WEST -> {
						agent_col_idx--;
				}
            }
	    }
	}
	
	public void updateWorld(int row_idx, int col_idx, int info)
	{
		// -3 out of boundary
		world[row_idx][col_idx] = info;
	}

	public void printWorldDebug() {
		for (int[] row : world) {
			for (int cell : row) {
                switch (cell) {
                    case UNKNOWN -> System.out.print(" ? ");
                    case WALL -> System.out.print(" # ");
                    case CLEAR -> System.out.print(" . ");
                    case DIRT -> System.out.print(" * ");
                    case HOME -> System.out.print(" H ");
                }
			}
			System.out.println();
		}
	}

	public static boolean rowIsValid(int row_idx){
		return (0 <= row_idx && row_idx < WIDTH);
	}
	public static boolean colIsValid(int col_idx){ return (0 <= col_idx && col_idx < HEIGHT);}
	public static boolean rowColIsValid(int row_idx, int col_idx){
		return (0 <= row_idx && row_idx < WIDTH) && (0 <= col_idx && col_idx < HEIGHT);
	}
	public int getState(int row_idx, int col_idx){
		if(rowColIsValid(row_idx,col_idx)){
			return this.world[row_idx][col_idx];
		}else {
			return ERROR;
		}

	}
}

class MyAgentProgram implements AgentProgram {
	public static class Pair implements Comparable<Pair>{
		private int row;
		private int col;
		public Pair(){
			this.row = 0;
			this.col = 0;
		}

		public Pair(int row, int col) {
			this.row = row;
			this.col = col;
		}

		public int getRow() {
			return row;
		}

		public int getCol() {
			return col;
		}

		public void setXY(int x, int y){
			this.row = x;
			this.col = y;
		}
		public Pair getEast(){
			return new Pair(this.row, this.col + 1);
		}
		public Pair getWest(){
			return new Pair(this.row, this.col-1);
		}
		public Pair getNorth(){
			return new Pair(this.row-1, this.col);
		}
		public Pair getSouth(){	return new Pair(this.row + 1, this.col);}
		@Override
		public int compareTo(Pair other) {
			int xComparison = Integer.compare(this.row, other.row);
			if (xComparison != 0) {
				return xComparison;
			}
			return Integer.compare(this.col, other.col);
		}
	}



	private int initialRandomActions = 5;
	private Random random_generator = new Random();
	
	//todo: Here you can define your variables!
	private TreeMap<Pair, Boolean> barriers = new TreeMap<>();//store the x,y of barriers
	private TreeMap<Pair, Boolean> dust = new TreeMap<>();//store the xy of dirt
	private Stack<Pair> stack = new Stack<>();
	private Pair cur_pair = new Pair();
	public static Action ACTION_TURN_LEFT = LIUVacuumEnvironment.ACTION_TURN_LEFT;
	public static Action ACTION_TURN_RIGHT = LIUVacuumEnvironment.ACTION_TURN_RIGHT;
	public static Action ACTION_MOVE_FORWARD = LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
	public static Action ACTION_SUCK = LIUVacuumEnvironment.ACTION_SUCK;
	Action myAction;
	public int iterationCounter = 0;//for counting continuously steering direction steps
	public MyAgentState state = new MyAgentState();
	private void steeringDirection(int dir){
		int formerDirection = state.agent_direction;
		state.agent_direction = dir;
		iterationCounter = formerDirection - state.agent_direction;
		if(Math.abs(iterationCounter) > 2){
			iterationCounter = iterationCounter > 0 ? iterationCounter - 4 : iterationCounter + 4;
		}
		if(iterationCounter < 0 ){
			myAction = ACTION_TURN_LEFT;
		}else if (iterationCounter > 0) {
			myAction = ACTION_TURN_RIGHT;
		}else {//at same direction
			myAction = ACTION_MOVE_FORWARD;
		}
		iterationCounter = Math.abs(iterationCounter);
	}


	//moves the Agent to a random start position
	// uses percepts to update the Agent position - only the position, other percepts are ignored
	//returns a random action, this function is used to initialization for random start point
	private Action moveToRandomStartPosition(DynamicPercept percept) {
		int action = random_generator.nextInt(6);
		initialRandomActions--;
		state.updatePosition(percept);
		if(action==0) {
			state.agent_direction = ((state.agent_direction+1) % 4);
			if (state.agent_direction<0) {
				state.agent_direction +=4;
			}
			state.agent_last_action = state.ACTION_TURN_LEFT;
			return LIUVacuumEnvironment.ACTION_TURN_LEFT;
		} else if (action==1) {
			state.agent_direction = ((state.agent_direction + 4 -1) % 4);
			state.agent_last_action = state.ACTION_TURN_RIGHT;
			return LIUVacuumEnvironment.ACTION_TURN_RIGHT;
		}
		state.agent_last_action=state.ACTION_MOVE_FORWARD;
		return LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
	}

	public void stackPrint() {
		System.out.println("Stack Contents:");
		for (Pair pair : stack) {
			System.out.println("(" + pair.getRow() + ", " + pair.getCol() + ")");
		}
	}
	public void reset(){
		stack.clear();
		initialRandomActions = 10;
	}
	void nextStepIsValid(Stack<Pair> stack){
		return ;
	}
	
	@Override
	public Action execute(Percept percept) {
		//step 0: check initialization
		System.out.printf("@@@@@Now: (%d, %d):  %d\n",state.agent_row_idx, state.agent_col_idx, state.agent_direction);
		DynamicPercept p = (DynamicPercept) percept;
		Boolean bump = (Boolean)p.getAttribute("bump");//1: bump into something
		Boolean dirt = (Boolean)p.getAttribute("dirt");
		Boolean home = (Boolean)p.getAttribute("home");
		System.out.println("perception : " + p);

		if(!state.initialized){
			// DO NOT REMOVE this if condition!!!
			if (initialRandomActions >0) {
				return moveToRandomStartPosition((DynamicPercept) percept);
			} else if (initialRandomActions ==0) {
				//process perception for the last step of the initial random actions
				initialRandomActions--;
				state.updatePosition((DynamicPercept) percept);
				System.out.println("Processing percepts after the last execution of moveToRandomStartPosition()");
				state.agent_last_action=state.ACTION_SUCK;
				state.initialized = true;

				//Push into stack
				cur_pair.setXY(state.agent_row_idx, state.agent_col_idx);
				stack.push(cur_pair);
				state.initialized = true;
				return LIUVacuumEnvironment.ACTION_SUCK;
			}
		}

		// This example agent program will update the internal agent state while only moving forward.
		// todo: START HERE - code below should be modified!
		state.printWorldDebug();
		if(iterationCounter > 1){//continuously change direction
			iterationCounter--;
			return myAction;
		}else if(iterationCounter == 1) {//pass the perception test: move forward
			iterationCounter--;//single turning operation has been done after the cell Pacman facing at is pushed into stack
			myAction = ACTION_MOVE_FORWARD;
			return myAction;
		}




	    // State update based on the perception value and the last action
		// Update current position status
	    if (bump) {//bump in the wall
			switch (state.agent_direction) {//todo: do we need to steer directions?
				case MyAgentState.NORTH -> {
					state.updateWorld(cur_pair.getNorth().getRow(), cur_pair.getNorth().getCol(), state.WALL);
				}
				case MyAgentState.EAST -> {
					state.updateWorld(cur_pair.getEast().getRow(), cur_pair.getEast().getCol(), state.WALL);
				}
				case MyAgentState.SOUTH -> {
					state.updateWorld(cur_pair.getSouth().getRow(), cur_pair.getSouth().getCol(), state.WALL);
				}
				case MyAgentState.WEST -> {
					state.updateWorld(cur_pair.getWest().getRow(), cur_pair.getWest().getCol(), state.WALL);
				}
			}
			if(!stack.isEmpty()){
				stack.pop();
			}
			//add walls into Treemap
			if(!barriers.containsKey(cur_pair)){//for looking up when doing search
				barriers.put(cur_pair, Boolean.TRUE);
			}
	    }
	    else if (dirt) {//dirt for Pacman not for "world"
			if(!stack.isEmpty()){
				cur_pair = stack.peek();
			}
//			cur_pair.setXY(state.agent_x_position, state.agent_y_position);
			state.updateWorld(cur_pair.getRow(), cur_pair.getCol(), state.CLEAR);//set as dirt
			dust.put(cur_pair, Boolean.TRUE);
			//dirt action
			System.out.println("DIRT -> choosing SUCK action!");

			myAction = ACTION_SUCK;
			return myAction;
		}else{//CLEAR || HOME || UNKNOWN
			if(!stack.isEmpty()){
				cur_pair = stack.peek();
			}
			if (state.getState(cur_pair.getRow(), cur_pair.getCol()) == state.UNKNOWN){
				state.updateWorld(cur_pair.getRow(),cur_pair.getCol(),state.CLEAR);
			}
			state.agent_row_idx = cur_pair.getRow();
			state.agent_col_idx = cur_pair.getCol();
			myAction = ACTION_MOVE_FORWARD;
			state.updatePosition((DynamicPercept)percept);

			//set as clear, move forward
		}

		stackPrint();
		//DFS here, modeling a map
		if (!stack.isEmpty()){
			cur_pair = stack.peek();
			//Traveling order: North->East->South->West

			//todo: turning direction of Pacman, some of situation don't need to change direction, but some need
			if ((state.getState(cur_pair.getNorth().getRow(), cur_pair.getNorth().getCol()) == state.UNKNOWN)
					){
				steeringDirection(MyAgentState.NORTH);
				state.agent_direction = MyAgentState.NORTH;
				stack.push(cur_pair.getNorth());
				return myAction;
			}else if ((state.getState(cur_pair.getEast().getRow(), cur_pair.getEast().getCol()) == state.UNKNOWN)
					){
				steeringDirection(MyAgentState.EAST);
				state.agent_direction = MyAgentState.EAST;
				stack.push(cur_pair.getEast());
				return myAction;
			} else if ((state.getState(cur_pair.getSouth().getRow(), cur_pair.getSouth().getCol()) == state.UNKNOWN)
					) {
				steeringDirection(MyAgentState.SOUTH);
				state.agent_direction = MyAgentState.SOUTH;
				stack.push(cur_pair.getSouth());
				return myAction;
			} else if ((state.getState(cur_pair.getWest().getRow(), cur_pair.getWest().getCol()) == state.UNKNOWN)
					){
				steeringDirection(MyAgentState.WEST);
				state.agent_direction = MyAgentState.WEST;
				stack.push(cur_pair.getWest());
				return myAction;
			} else{
				//backtracking
				stack.pop();
				if(stack.isEmpty()){
					if ((state.getState(cur_pair.getNorth().getRow(), cur_pair.getNorth().getCol()) == state.UNKNOWN)){
						steeringDirection(MyAgentState.NORTH);
						state.agent_direction = MyAgentState.NORTH;
						stack.push(cur_pair.getNorth());
						return myAction;
					}else if ((state.getState(cur_pair.getEast().getRow(), cur_pair.getEast().getCol()) == state.UNKNOWN)){
						steeringDirection(MyAgentState.EAST);
						state.agent_direction = MyAgentState.EAST;
						stack.push(cur_pair.getEast());
						return myAction;
					} else if ((state.getState(cur_pair.getSouth().getRow(), cur_pair.getSouth().getCol()) == state.UNKNOWN)) {
						steeringDirection(MyAgentState.SOUTH);
						state.agent_direction = MyAgentState.SOUTH;
						stack.push(cur_pair.getSouth());
						return myAction;
					} else if ((state.getState(cur_pair.getWest().getRow(), cur_pair.getWest().getCol()) == state.UNKNOWN)){
						steeringDirection(MyAgentState.WEST);
						state.agent_direction = MyAgentState.WEST;
						stack.push(cur_pair.getWest());
						return myAction;
					}
					else{
						System.out.println("COMPLETED HERE!");
						this.reset();
						state.reset();
						return NoOpAction.NO_OP;
					}
				}
				Pair undo_pair = stack.peek();
				int row_diff = undo_pair.getRow() - cur_pair.getRow();
				if(row_diff != 0){
					if(row_diff < 0){//to west
						steeringDirection(MyAgentState.NORTH);
					}else{
						steeringDirection(MyAgentState.SOUTH);
					}
				}else{
					int col_diff = undo_pair.getCol() - cur_pair.getCol();
					if(col_diff < 0){
						steeringDirection(MyAgentState.WEST);
					}else {
						steeringDirection(MyAgentState.EAST);
					}
				}
				cur_pair = stack.peek();
			}
		}
		return myAction;
	}
}

public class MyVacuumAgent extends AbstractAgent {
    public MyVacuumAgent() {

    	super(new MyAgentProgram());
		AgentProgram agentProgram = super.program;

	}

	@Override
	public Action execute(Percept p) {

		return super.execute(p);
	}
}
