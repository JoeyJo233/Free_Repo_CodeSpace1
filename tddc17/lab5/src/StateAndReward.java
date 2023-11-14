public class StateAndReward {

	
	/* State discretization function for the angle controller */
	public static String getStateAngle(double angle, double vx, double vy) {

		int discreteAngle = discretize(angle, 200, -Math.PI, Math.PI); // Finer discretization over the full angular range.
		return "AngleState_" + discreteAngle;
	}

	/* Reward function for the angle controller */
	public static double getRewardAngle(double angle, double vx, double vy) {
		double absAngle = Math.abs(angle); // Use the absolute angle for comparison
	
		// Define the boundaries of the angle tiers
		final double TIER1 = 0.1 * Math.PI;
		final double TIER2 = 0.25 * Math.PI;
		final double TIER3 = 0.5 * Math.PI;
		final double TIER4 = 0.75 * Math.PI;
		final double TIER5 = Math.PI;
	
		// Set rewards or penalties for each tier
		final double REWARD_TIER1 = 1;
		final double PENALTY_TIER2 = -1;
		final double PENALTY_TIER3 = -10;
		final double PENALTY_TIER4 = -50;
		final double PENALTY_TIER5 = -100;
	
		// Assign rewards or penalties based on the tier
		double reward;
		if (absAngle < TIER1) {
			reward = REWARD_TIER1; // Encourage being upright
		} else if (absAngle < TIER2) {
			reward = PENALTY_TIER2; // Small penalty for slight tilt
		} else if (absAngle < TIER3) {
			reward = PENALTY_TIER3; // Larger penalty as tilt increases
		} else if (absAngle < TIER4) {
			reward = PENALTY_TIER4; // Even larger penalty for significant tilt
		} else {
			reward = PENALTY_TIER5; // Maximum penalty for being nearly inverted
		}
	
		return reward;
	}
	
	
	

	/* State discretization function for the full hover controller */
	public static String getStateHover(double angle, double vx, double vy) {
		int discreteAngle = discretize(angle, 20, -Math.PI, Math.PI);
		int discreteVx = discretize(vx, 10000, -50, 50);
		int discreteVy = discretize(vy, 10000, -50, 50);
		
		return "HoverState_" + discreteAngle + "_" + discreteVx + "_" + discreteVy;
	}
	
	

	public static double getRewardHover(double angle, double vx, double vy, double prevVx, double prevVy) {
		double reward = 1; // Start with the maximum reward
		
		// Apply a high penalty for any horizontal movement (vx not zero)
		if (Math.abs(vx) > 0) {
			reward -= Math.exp(Math.abs(vx)) - 1; // Exponential penalty for horizontal movement
		}
		
		// Apply a penalty for vertical movement, more if vy is negative
		if (vy < 0) {
			reward -= Math.exp(Math.abs(vy)) - 1; // Higher penalty for downward movement
		} else if (vy > 0) {
			reward -= Math.abs(vy); // Lesser penalty for slight upward movement
		}
		
		// Calculate accelerations
		double ax = vx - prevVx; // Acceleration in horizontal direction
		double ay = vy - prevVy; // Acceleration in vertical direction
		
		// Apply penalty based on acceleration
		if (ay < 0 || ax != 0) {
			reward -= Math.exp(Math.abs(ax) + Math.abs(ay)) - 1; // Exponential penalty for negative acceleration or any horizontal acceleration
		}
		
		// Provide a recovery path when upside down
		if (Math.abs(angle) > Math.PI / 2) { // Rocket is upside down
			// Adjust the recovery reward based on how quickly the rocket is moving towards an upright position
			double recoverySignal = Math.cos(angle); // This will be positive when the rocket is upright
			reward += recoverySignal; // Encourage the rocket to get back to an upright position
		}
		
		// Ensure that the reward does not become negative
		return Math.max(0, reward);
	}
	
	
	

	// ///////////////////////////////////////////////////////////
	// discretize() performs a uniform discretization of the
	// value parameter.
	// It returns an integer between 0 and nrValues-1.
	// The min and max parameters are used to specify the interval
	// for the discretization.
	// If the value is lower than min, 0 is returned
	// If the value is higher than min, nrValues-1 is returned
	// otherwise a value between 1 and nrValues-2 is returned.
	//
	// Use discretize2() if you want a discretization method that does
	// not handle values lower than min and higher than max.
	// ///////////////////////////////////////////////////////////
	public static int discretize(double value, int nrValues, double min,
			double max) {
		if (nrValues < 2) {
			return 0;
		}

		double diff = max - min;

		if (value < min) {
			return 0;
		}
		if (value > max) {
			return nrValues - 1;
		}

		double tempValue = value - min;
		double ratio = tempValue / diff;

		return (int) (ratio * (nrValues - 2)) + 1;
	}

	// ///////////////////////////////////////////////////////////
	// discretize2() performs a uniform discretization of the
	// value parameter.
	// It returns an integer between 0 and nrValues-1.
	// The min and max parameters are used to specify the interval
	// for the discretization.
	// If the value is lower than min, 0 is returned
	// If the value is higher than min, nrValues-1 is returned
	// otherwise a value between 0 and nrValues-1 is returned.
	// ///////////////////////////////////////////////////////////
	public static int discretize2(double value, int nrValues, double min,
			double max) {
		double diff = max - min;

		if (value < min) {
			return 0;
		}
		if (value > max) {
			return nrValues - 1;
		}

		double tempValue = value - min;
		double ratio = tempValue / diff;

		return (int) (ratio * nrValues);
	}

}
