import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Random;

/* TODO: 
 * -Define state and reward functions (StateAndReward.java) suitable for your problem 
 * -Define actions
 * -Implement missing parts of Q-learning
 * -Tune state and reward function, and parameters below if the result is not satisfactory */

public class QLearningController extends Controller {
	
	/* These are the agents senses (inputs) */
	DoubleFeature x; /* Positions */
	DoubleFeature y;
	DoubleFeature vx; /* Velocities */
	DoubleFeature vy;
	DoubleFeature angle; /* Angle */

	/* These are the agents actuators (outputs)*/
	RocketEngine leftEngine;
	RocketEngine middleEngine;
	RocketEngine rightEngine;

	final static int NUM_ACTIONS = 7; /* The takeAction function must be changed if this is modified */
	
	/* Keep track of the previous state and action */
	String previous_state = null;
	double previous_vx = 0;
	double previous_vy = 0;
	double previous_angle = 0;
	int previous_action = 0; 
	double previous_x = 0;
	double previous_y = 0;

	Boolean toggleHover = false;
	// Define angle thresholds

	final double SMALL_ANGLE = 0.1 * Math.PI;
	final double MEDIUM_ANGLE = 0.2 * Math.PI;
	final double LARGE_ANGLE = 0.333 * Math.PI;
	final double CRITICAL_ANGLE = 0.75 * Math.PI;

	//minX: -1638.353297044766 maxX: 3202.4366775615435 minY: -2115.8942650529275 maxY: 1197.829041678518 minAngle: -3.1410734732421792 maxAngle: 3.1415204812065234
	final static double minX = -1638.353297044766;
	final static double maxX = 3202.4366775615435;
	final static double minY = -2115.8942650529275;
	final static double maxY = 1197.829041678518;

	boolean correctingAngle = false;
int correctiveAction = -1; // Default, -1 means no action.


	/* The tables used by Q-learning */
	Hashtable<String, Double> Qtable = new Hashtable<String, Double>(); /* Contains the Q-values - the state-action utilities */
	Hashtable<String, Integer> Ntable = new Hashtable<String, Integer>(); /* Keeps track of how many times each state-action combination has been used */

	/* PARAMETERS OF THE LEARNING ALGORITHM - THESE MAY BE TUNED BUT THE DEFAULT VALUES OFTEN WORK REASONABLY WELL  */
	static final double GAMMA_DISCOUNT_FACTOR = 0.95; /* Must be < 1, small values make it very greedy */
	static final double LEARNING_RATE_CONSTANT = 10; /* See alpha(), lower values are good for quick results in large and deterministic state spaces */
	double explore_chance = 0.5; /* The exploration chance during the exploration phase */
	final static int REPEAT_ACTION_MAX = 30; /* Repeat selected action at most this many times trying reach a new state, without a max it could loop forever if the action cannot lead to a new state */

	/* Some internal counters */
	int iteration = 0; /* Keeps track of how many iterations the agent has run */
	int action_counter = 0; /* Keeps track of how many times we have repeated the current action */
	int print_counter = 0; /* Makes printouts less spammy */ 

	/* These are just internal helper variables, you can ignore these */
	boolean paused = false;
	boolean explore = true; /* Will always do exploration by default */

	DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US); 
	public SpringObject object;
	ComposedSpringObject cso;
	long lastPressedExplore = 0;

	public void init() {
		cso = (ComposedSpringObject) object;
		x = (DoubleFeature) cso.getObjectById("x");
		y = (DoubleFeature) cso.getObjectById("y");
		vx = (DoubleFeature) cso.getObjectById("vx");
		vy = (DoubleFeature) cso.getObjectById("vy");
		angle = (DoubleFeature) cso.getObjectById("angle");
		
		previous_vy = vy.getValue();
		previous_vx = vx.getValue();
		previous_angle = angle.getValue();

		leftEngine = (RocketEngine) cso.getObjectById("rocket_engine_left");
		rightEngine = (RocketEngine) cso.getObjectById("rocket_engine_right");
		middleEngine = (RocketEngine) cso.getObjectById("rocket_engine_middle");
	}

	
	/* Turn off all rockets */
	void resetRockets() {
		leftEngine.setBursting(false);
		rightEngine.setBursting(false);
		middleEngine.setBursting(false);
	}

	/* Performs the chosen action */
	enum Action {
		NO_ACTION(0),
		LEFT_ENGINE(1),
		RIGHT_ENGINE(2),
		MIDDLE_ENGINE(3),
		LEFT_MIDDLE_ENGINES(4),
		RIGHT_MIDDLE_ENGINES(5),
		ALL_ENGINES(6);

		private final int value;

		Action(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
		public int getValue(int value) {
			return value;
		}
			
	}

	void performAction(Action action) {
		resetRockets(); // First, turn off all rockets.
		switch (action) {
			case NO_ACTION: // No action, all rockets remain off.
				break;
			case LEFT_ENGINE:
				leftEngine.setBursting(true);
				break;
			case RIGHT_ENGINE:
				rightEngine.setBursting(true);
				break;
			case MIDDLE_ENGINE:
				middleEngine.setBursting(true);
				break;
			case LEFT_MIDDLE_ENGINES:
				leftEngine.setBursting(true);
				middleEngine.setBursting(true);
				break;
			case RIGHT_MIDDLE_ENGINES:
				rightEngine.setBursting(true);
				middleEngine.setBursting(true);
				break;
			case ALL_ENGINES: // All engines on for faster acceleration.
				leftEngine.setBursting(true);
				rightEngine.setBursting(true);
				middleEngine.setBursting(true);
				break;
			default: // Optional: Handle unexpected values.
				System.out.println("Warning: Unknown action " + action);
				break;
		}
	}
	
	/* Main decision loop. Called every iteration by the simulator */
	public void tick(int currentTime) {
		iteration++;
		
		if (!paused) {
			String new_state = null;
			double previous_reward = 0.;
			if(toggleHover) {
				/* Hover mode */
				new_state = StateAndReward.getStateHover(angle.getValue(), vx.getValue(), vy.getValue());
				action_counter++;
				if (new_state.equals(previous_state) && action_counter < REPEAT_ACTION_MAX) {
					return;
				}
				previous_reward = StateAndReward.getRewardHover(angle.getValue(), vx.getValue(), vy.getValue(), previous_vx, previous_vy );
				action_counter = 0;
			}else{
				/* Angle mode */
				new_state = StateAndReward.getStateAngle(angle.getValue(), vx.getValue(), vy.getValue());
				action_counter++;
				if (new_state.equals(previous_state) && action_counter < REPEAT_ACTION_MAX) {
					return;
				}
				previous_reward = StateAndReward.getRewardAngle(angle.getValue(), vx.getValue(), vy.getValue());
				action_counter = 0;
			}
			double currentAngle = angle.getValue();
			double absAngle = Math.abs(currentAngle);

			// Check the angle threshold and decide which action to perform
			if (absAngle < SMALL_ANGLE) {
			    // Rocket is almost upright, minimal or no action required
			    performAction(Action.ALL_ENGINES);
			} else if (absAngle < MEDIUM_ANGLE) {
			    performAction(currentAngle > 0 ? Action.LEFT_MIDDLE_ENGINES : Action.RIGHT_MIDDLE_ENGINES);
			    // Slight tilt, gentle correction needed
			} else if (absAngle < LARGE_ANGLE) {
			    performAction(currentAngle > 0 ? Action.LEFT_ENGINE : Action.RIGHT_ENGINE);

				// Moderate tilt, more assertive correction needed
			} else {
			    // Severe tilt, engage a strong correction
			    performAction(currentAngle > 0 ? Action.LEFT_ENGINE : Action.RIGHT_ENGINE);
			} 

			/* The agent is in a new state, do learning and action selection */
			if (previous_state != null) {
				/* Create state-action key */
				String prev_stateaction = previous_state + previous_action;

				/* Increment state-action counter */
				if (Ntable.get(prev_stateaction) == null) {
					Ntable.put(prev_stateaction, 0);
				}
				Ntable.put(prev_stateaction, Ntable.get(prev_stateaction) + 1);

				/* Update Q value */
				if (Qtable.get(prev_stateaction) == null) {
					Qtable.put(prev_stateaction, 0.0);
				} 

				
				int action = selectAction(new_state); /* Make sure you understand how it selects an action */
				System.out.println("Action: " + action + "New State: " + new_state);
						


			    String current_stateaction = new_state + action;
	            double alpha = alpha(Ntable.getOrDefault(prev_stateaction, 0));
	            double oldQ = Qtable.getOrDefault(prev_stateaction, 0.0);
	            double maxQ = getMaxActionQValue(new_state);
	            double newQ = (1 - alpha) * oldQ + alpha * (previous_reward + GAMMA_DISCOUNT_FACTOR * maxQ);
	            Qtable.put(prev_stateaction, newQ);

				performAction(Action.values()[action]);
				
				/* Only print every 10th line to reduce spam */
				// print_counter++;
				// if (print_counter % 10000 == 0) {
				// 	System.out.println("ITERATION: " + iteration + " SENSORS: a=" + df.format(angle.getValue()) + " vx=" + df.format(vx.getValue()) + 
				// 			" vy=" + df.format(vy.getValue()) + " P_STATE: " + previous_state + " P_ACTION: " + previous_action + 
				// 			" P_REWARD: " + df.format(previous_reward) + " P_QVAL: " + df.format(Qtable.get(prev_stateaction)) + " Tested: "
				// 			+ Ntable.get(prev_stateaction) + " times.");
				// }
				
				previous_vy = vy.getValue();
				previous_vx = vx.getValue();
				previous_x = x.getValue();
				previous_y = y.getValue();
				previous_angle = angle.getValue();
				previous_action = action;
				previous_state = new_state;

			}
			previous_state = new_state;
		}

	}

	/* Computes the learning rate parameter alpha based on the number of times the state-action combination has been tested */
	public double alpha(int num_tested) {
		/* Lower learning rate constants means that alpha will become small faster and therefore make the agent behavior converge to 
		 * to a solution faster, but if the state space is not properly explored at that point the resulting behavior may be poor.
		 * If your state-space is really huge you may need to increase it. */
		double alpha = (LEARNING_RATE_CONSTANT/(LEARNING_RATE_CONSTANT + num_tested));
		return alpha;
	}
	
	/* Finds the highest Qvalue of any action in the given state */
	public double getMaxActionQValue(String state) {
		double maxQval = Double.NEGATIVE_INFINITY;
		
		for (int action = 0; action < NUM_ACTIONS; action++) {
			Double Qval = Qtable.get(state+action);
			if (Qval != null && Qval > maxQval) {
				maxQval = Qval;
			} 
		}

		if (maxQval == Double.NEGATIVE_INFINITY) {
			/* Assign 0 as that corresponds to initializing the Qtable to 0. */
			maxQval = 0;
		}
		return maxQval;
	}
	
	/* Selects an action in a state based on the registered Q-values and the exploration chance */
	public int selectAction(String state) {
		Random rand = new Random();

		int action = 0;
		/* May do exploratory move if in exploration mode */
		if (explore && Math.abs(rand.nextDouble()) < explore_chance) {
			/* Taking random exploration action! */
			action = Math.abs(rand. nextInt()) % NUM_ACTIONS;
			return action;
		}

		/* Find action with highest Q-val (utility) in given state */
		double maxQval = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < NUM_ACTIONS; i++) {
			String test_pair = state + i; /* Generate a state-action pair for all actions */
			double Qval = 0;
			if (Qtable.get(test_pair) != null) {
				Qval = Qtable.get(test_pair);
			}
			if (Qval > maxQval) {
				maxQval = Qval;
				action = i;
			}
		}
		return action;
	}

	
	
	/* The 'E' key will toggle the agents exploration mode. Turn this off to test its behavior */
	public void toggleExplore() {
		/* Make sure we don't toggle it multiple times */
		if (System.currentTimeMillis() - lastPressedExplore < 1000) {
			return;
		}
		if (explore) {
			System.out.println("Turning OFF exploration!");
			explore = false;
		} else {
			System.out.println("Turning ON exploration!");
			explore = true;
		}
		lastPressedExplore = System.currentTimeMillis(); 
	}

	/* Keys 1 and 2 can be customized for whatever purpose if you want to */
	public void toggleCustom1() {
		toggleHover = false;
		System.out.println("toggle to upright mode!");
	}

	/* Keys 1 and 2 can be customized for whatever purpose if you want to */
	public void toggleCustom2() {
		toggleHover = true;
		System.out.println("toggle to hover mode!");
	}
	
	public void pause() {
		paused = true;
		resetRockets();
	}

	public void run() {
		paused = false;
	}
}
