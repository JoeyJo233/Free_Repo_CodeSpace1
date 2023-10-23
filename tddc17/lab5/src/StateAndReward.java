public class StateAndReward {

	
	/* State discretization function for the angle controller */
	public static String getStateAngle(double angle, double vx, double vy) {

		/* TODO: IMPLEMENT THIS FUNCTION */
		int discreteAngle = discretize(angle, 20, -Math.PI, Math.PI); // Finer discretization over the full angular range.
		return "AngleState_" + discreteAngle;
	}

	/* Reward function for the angle controller */
	public static double getRewardAngle(double angle, double vx, double vy) {
		double reward = 0;
		if (Math.abs(angle) < 0.1) { // Close to upright
			reward = 1;
		} else if (Math.abs(angle) > 2.8) { // Close to being inverted
			reward = -1; // Maximum penalty
		} else {
			reward = -1 * Math.abs(angle) / Math.PI; // Penalty proportional to deviation from upright
		}
		return reward;
	}
	

	/* State discretization function for the full hover controller */
	public static String getStateHover(double angle, double vx, double vy) {
		int discreteAngle = discretize(angle, 20, -Math.PI, Math.PI);
		int discreteVx = discretize(vx, 5, -1, 1);
		int discreteVy = discretize(vy, 5, -1, 1);
		
		return "HoverState_" + discreteAngle + "_" + discreteVx + "_" + discreteVy;
	}
	
	

	/* Reward function for the full hover controller */
	public static double getRewardHover(double angle, double vx, double vy) {
		double anglePenalty = Math.abs(angle) / Math.PI;  // Assuming max angle deviation is 50°.
		double vyPenalty = Math.abs(vy);               // Penalizing based on the vertical speed.
		double vxPenalty = Math.abs(vx);               // Penalizing based on the horizontal speed.
	
		double reward = 1 - anglePenalty - vyPenalty - vxPenalty; // Maximum reward is 1 when rocket is perfectly hovering.
		
		return reward;
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
