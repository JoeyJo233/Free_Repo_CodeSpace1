package tddc17;


import aima.core.environment.liuvacuum.*;
import aima.core.agent.Action;
import aima.core.agent.AgentProgram;
import aima.core.agent.Percept;
import aima.core.agent.impl.*;

import java.util.*;



class MyAgentState
{
	public int[][] world = new int[30][30];
	public boolean initialized = false;
	final int UNKNOWN 	= 0;
	final int WALL 		= 1;
	final int CLEAR 	= 2;
	final int DIRT		= 3;
	final int HOME		= 4;
	final int VISITED = 5;
	final int ACTION_NONE 			= 0;
	final int ACTION_MOVE_FORWARD 	= 1;
	final int ACTION_TURN_RIGHT 	= 2;
	final int ACTION_TURN_LEFT 		= 3;
	final int ACTION_SUCK	 		= 4;
	
	public int agent_x_position = 1;
	public int agent_y_position = 1;
	public int agent_last_action = ACTION_NONE;
	
	public static final int NORTH = 0;
	public static final int EAST = 1;
	public static final int SOUTH = 2;
	public static final int WEST = 3;
	public int agent_direction = EAST;
	
	MyAgentState()
	{
        for (int[] ints : world) Arrays.fill(ints, UNKNOWN);
		world[1][1] = HOME;
		agent_last_action = ACTION_NONE;
	}
	// Based on the last action and the received percept updates the x & y agent position
	public void updatePosition(DynamicPercept p)
	{
		Boolean bump = (Boolean)p.getAttribute("bump");

		if (agent_last_action==ACTION_MOVE_FORWARD && !bump)// agent move forward and doesn't bump into obstacles
	    {
            switch (agent_direction) {
                case MyAgentState.NORTH -> agent_y_position--;
                case MyAgentState.EAST -> agent_x_position++;
                case MyAgentState.SOUTH -> agent_y_position++;
                case MyAgentState.WEST -> agent_x_position--;
            }
	    }
		
	}
	
	public void updateWorld(int x_position, int y_position, int info)
	{
		world[x_position][y_position] = info;
	}
	
	public void printWorldDebug()
	{
		for (int i=0; i < world.length; i++)
		{
			for (int j=0; j < world[i].length ; j++)
			{
				if (world[j][i]==UNKNOWN)
					System.out.print(" ? ");
				if (world[j][i]==WALL)
					System.out.print(" # ");
				if (world[j][i]==CLEAR)
					System.out.print(" . ");
				if (world[j][i]==DIRT)
					System.out.print(" D ");
				if (world[j][i]==HOME)
					System.out.print(" H ");
			}
		}
		System.out.println("printWorldDebug()");
	}
}

class MyAgentProgram implements AgentProgram {
	public static class Pair implements Comparable<Pair>{
		private int x;
		private int y;

		public Pair(int x, int y) {
			this.x = x;
			this.y = y;
		}

		public int getX() {
			return x;
		}

		public int getY() {
			return y;
		}

		@Override
		public int compareTo(Pair other) {
			int xComparison = Integer.compare(this.x, other.x);
			if (xComparison != 0) {
				return xComparison;
			}
			return Integer.compare(this.y, other.y);
		}
	}

	private int initialRandomActions = 10;
	private Random random_generator = new Random();
	
	//todo: Here you can define your variables!
	private TreeMap<Pair, Boolean> Barriers = new TreeMap<>();//store the x,y of barriers
	private TreeMap<Pair, Boolean> Dust = new TreeMap<>();//store the xy of dirt
	private Queue<Pair> queue = new LinkedList<>();

	public int iterationCounter = 10;//steps for start point initialization
	public MyAgentState state = new MyAgentState();
	
	//moves the Agent to a random start position
	// uses percepts to update the Agent position - only the position, other percepts are ignored
	//returns a random action, this function is used to initialization for random start point
	private Action moveToRandomStartPosition(DynamicPercept percept) {
		int action = random_generator.nextInt(6);
		initialRandomActions--;
		state.updatePosition(percept);
		if(action==0) {
		    state.agent_direction = ((state.agent_direction-1) % 4);
		    if (state.agent_direction<0)
		    	state.agent_direction +=4;
		    state.agent_last_action = state.ACTION_TURN_LEFT;
			return LIUVacuumEnvironment.ACTION_TURN_LEFT;
		} else if (action==1) {
			state.agent_direction = ((state.agent_direction+1) % 4);
		    state.agent_last_action = state.ACTION_TURN_RIGHT;
		    return LIUVacuumEnvironment.ACTION_TURN_RIGHT;
		}
		state.agent_last_action=state.ACTION_MOVE_FORWARD;
		return LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
	}
	
	
	@Override
	public Action execute(Percept percept) {

		//step 0: check initialization
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
				return LIUVacuumEnvironment.ACTION_SUCK;
			}
		}

    	// This example agent program will update the internal agent state while only moving forward.
    	// todo: START HERE - code below should be modified!
    	    	
    	System.out.println("x=" + state.agent_x_position);
    	System.out.println("y=" + state.agent_y_position);
    	System.out.println("dir=" + state.agent_direction);

		//BFS here

		
	    iterationCounter--;
	    
//	    if (iterationCounter==0)
//	    	return NoOpAction.NO_OP;

	    DynamicPercept p = (DynamicPercept) percept;
	    Boolean bump = (Boolean)p.getAttribute("bump");//1: bump into something
	    Boolean dirt = (Boolean)p.getAttribute("dirt");
	    Boolean home = (Boolean)p.getAttribute("home");
	    System.out.println("perception : " + p);

		//todo: CODE FROM HERE!!!!!!!!!
	    // State update based on the perception value and the last action
	    state.updatePosition((DynamicPercept)percept);
	    if (bump) {
            switch (state.agent_direction) {
                case MyAgentState.NORTH ->
                        state.updateWorld(state.agent_x_position, state.agent_y_position - 1, state.WALL);
                case MyAgentState.EAST ->
                        state.updateWorld(state.agent_x_position + 1, state.agent_y_position, state.WALL);
                case MyAgentState.SOUTH ->
                        state.updateWorld(state.agent_x_position, state.agent_y_position + 1, state.WALL);
                case MyAgentState.WEST ->
                        state.updateWorld(state.agent_x_position - 1, state.agent_y_position, state.WALL);
            }
	    }
	    if (dirt)
	    	state.updateWorld(state.agent_x_position,state.agent_y_position,state.DIRT);
	    else
	    	state.updateWorld(state.agent_x_position,state.agent_y_position,state.CLEAR);
	    
	    state.printWorldDebug();
	    
	    
	    //todo: Next action selection based on the percept value
	    if (dirt)
	    {
	    	System.out.println("DIRT -> choosing SUCK action!");
	    	state.agent_last_action=state.ACTION_SUCK;
	    	return LIUVacuumEnvironment.ACTION_SUCK;
	    } 
	    else
	    {
	    	if (bump)
	    	{
	    		state.agent_last_action=state.ACTION_NONE;
		    	return NoOpAction.NO_OP;
	    	}
	    	else
	    	{
	    		state.agent_last_action=state.ACTION_MOVE_FORWARD;
	    		return LIUVacuumEnvironment.ACTION_MOVE_FORWARD;
	    	}
	    }
	}
}

public class MyVacuumAgent extends AbstractAgent {
    public MyVacuumAgent() {
    	super(new MyAgentProgram());
		System.out.println("ohao, niua!");
	}

}
