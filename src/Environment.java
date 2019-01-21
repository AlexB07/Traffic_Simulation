import java.util.ArrayList;
import java.util.Random;

import javafx.animation.*;

/**
 * A class which represents the environment that we are working in. In other
 * words, this class describes the road and the cars that are on the road.
 */
public class Environment implements Cloneable {

	/** All the cars that are on our road */
	private ArrayList<Car> cars = new ArrayList<Car>();
	/** The Display object that we are working with */
	private Display display;
	/** Number of lanes to have on the road */
	private int lanes = 4;
	private long last;
	private Random rd = new Random();

	// User condition (Braking efficiency)
	private int brakingConditions = 10;

	// User condition (rubber-necking values)
	private boolean rubberNecking = false;
	private int minRubb;
	private int maxRubb;
	// User condition (Maximum speed)
	private int maxSpeed = 80;

	// User condition Bad Lane Discipline
	private boolean insideLanes = false;
	
	/**
	 * Set the Display object that we are working with. This must be called before
	 * anything will happen.
	 */
	public void setDisplay(Display display) {
		this.display = display;

		/* Start a timer to update things */
		new AnimationTimer() {
			public void handle(long now) {
				if (last == 0) {
					last = now;
				}

				/* Update the model */
				tick((now - last) * 1e-9);
				/* Update the view */
				double furthest = 0;
				for (Car i : cars) {
					rubberNecking();
					// User input control for inside lane
					moveBackInsideLane();
					carOverTaking();
					carSpeedUp();
					brakeDetection();
					collisionDect();
					if (i.getPosition() > furthest) {

						furthest = i.getPosition();
					}
					//Update distance on Main
					Main.setDistance(display.getXOffSet()*-1);
				}
				display.setEnd((int) furthest);
				display.draw();
				last = now;
			}
		}.start();
	}

	/** Return a copy of this environment */
	public Environment clone() {
		Environment c = new Environment();
		for (Car i : cars) {
			c.cars.add(i.clone());
		}
		return c;
	}

	/** Draw the current state of the environment on our display */
	public void draw() {
		for (Car i : cars) {
			display.car((int) i.getPosition(), i.getLane(), i.getColor());
		}
	}

	/**
	 * Add a car to the environment.
	 * 
	 * @param car
	 *            Car to add.
	 */
	public void add(Car car) {
		cars.add(car);
	}

	public void clear() {
		cars.clear();
	}

	/** @return length of each car (in pixels) */
	public double carLength() {
		return 40;
	}

	/** Update the state of the environment after some short time has passed */
	private void tick(double elapsed) {
		Environment before = Environment.this.clone();
		for (Car i : cars) {
			i.tick(before, elapsed);
		}
	}

	/**
	 * @param behind
	 *            A car.
	 * @return The next car in front of @ref behind in the same lane, or null if
	 *         there is nothing in front in the same lane.
	 */
	public Car nextCar(Car behind) {
		Car closest = null;
		for (Car i : cars) {
			if (i != behind && i.getLane() == behind.getLane() && i.getPosition() > behind.getPosition()
					&& (closest == null || i.getPosition() < closest.getPosition())) {
				closest = i;
			}
		}
		return closest;
	}

	// Is the lane free for the car to move into that specific lane
	public boolean isOvertake(Car check, int lane, int min, int max) {
		for (Car i : cars) {
			if (i != check && i.getLane() == check.getLane() + lane && check.getPosition() > i.getPosition() - min
					&& check.getPosition() < i.getPosition() + max) {
				return false;
			}
		}
		return true;
	}

	// Checking to see if a car needs to overtake
	public void carOverTaking() {
		for (Car i : cars) {
			Car inFront = nextCar(i);
			if (inFront != null && i.getLane() != (lanes - 1) && i.getSpeed() != 0 && i.getPosition() + carLength() + 50 > inFront.getPosition() && isOvertake(i, 1, 60, 55)) {
				i.setLane(i.getLane() + 1);
			}

		}
	}

	// Moving the car to the most inside lane possible
	public void moveBackInsideLane() {
		if (!insideLanes) {
			for (Car i : cars) {
				if (i.getLane() != 0 && i.getSpeed() != 0 && isOvertake(i, -1, 60, 90)) {
					i.setLane(i.getLane() - 1);
				}
			}
		}
	}

	// Braking Detection for cars, and the efficiency of braking
	public void brakeDetection() {
		for (Car i : cars) {
			Car inFront = nextCar(i);
			if (inFront != null && i.getPosition() + carLength() >= inFront.getPosition() - 40) {
				if (i.getSpeed() >= inFront.getSpeed()) {
					double braking = (Math.abs(i.getSpeed() - inFront.getSpeed()) / brakingConditions*2);
					i.setSpeed(i.getSpeed() - braking);
				}
			}
		}
	}

	// Detection to see whether a car has crashed or not.
	public void collisionDect() {
		for (Car i : cars) {
			Car inFront = nextCar(i);
			if (inFront != null && i.getLane() == inFront.getLane() && i.getPosition() + carLength() >= inFront.getPosition()) {
				inFront.updateOnCollision();
				i.updateOnCollision();
			}
		}
	}

	// Speed up car given traffic condition and road conditions
	public void carSpeedUp() {
		for (Car i : cars) {
			Car inFront = nextCar(i);
			if (inFront != null && i.getPosition() + carLength() + 120 < inFront.getPosition()) {
				double diffSpeed = rd.nextInt(maxSpeed) + maxSpeed - brakingConditions;
				if (i.getSpeed() + diffSpeed <= maxSpeed) {
					// Working out what speed to set the car, when provided road conditions
					i.setSpeed(diffSpeed);
				}
			
			}
			//Sets car speed to max if no cars are in front
			if (inFront == null && i.getSpeed() < maxSpeed) {
				i.setSpeed(maxSpeed);
			}
			//Check to see if cars != 0 speed if they are and not collided then keeps them going. Make sure cars keep moving. e.g
			//once stopped for crashed car infront, overtakes and keeps going
			if (i.getSpeed() < 1 && i.getFlag() == false && inFront != null && i.getPosition() + carLength() + 60 < inFront.getPosition()) {
				i.setSpeed(rd.nextInt(maxSpeed));
			}
		}
	}

	// Setting rubber necking values, and also doing some user validation
	public void setRubberNeckingValues(int minRubb, int maxRubb) {
		// Making sure the minimum and maximum are the correct way around
		if (minRubb > maxRubb) {
			this.minRubb = minRubb;
			this.maxRubb = maxRubb;
		} else {
			this.minRubb = maxRubb;
			this.maxRubb = minRubb;
		}
		int validation = this.minRubb % 200;
		if (validation != 0) {
			this.minRubb -= (-200 + validation);
		}
		validation = this.maxRubb % 200;
		if (validation != 0) {
			this.maxRubb -= (-200 + validation);
		}

	}

	public void isRubberNeckingTrue(boolean flag, String min, String max) {
		if (!min.isEmpty() && !max.isEmpty() && flag) {
			setRubberNeckingValues(Integer.parseInt(min) * -1, Integer.parseInt(max) * -1);
			this.rubberNecking = true;
		} else {
			this.rubberNecking = false;
		}
	}

	public void rubberNecking() {
		if (rubberNecking) {
			settingSpeed(30, 20, display.getXOffSet(), minRubb);
			settingSpeed(70, 30, display.getXOffSet(), maxRubb);
		}
	}

	// set speed of all cars at a certain point
	public void settingSpeed(int maxSpeed, int minSpeed, int xValue, int value) {
		if (xValue == value) {
			for (Car i : cars) {
				i.setSpeed(rd.nextInt(maxSpeed) + minSpeed);
			}
		}
	}

	/** @return Number of lanes */
	public int getLanes() {
		return lanes;
	}

	public void setLanes(String lanes) {
		if (!lanes.equals("Lanes")) {
			this.lanes = Integer.parseInt(lanes);
		} else {
			this.lanes = 4;
		}
	}

	public void setBrakingConditions(String efficiency) {
		if (!efficiency.equals("Road Conditions")) {
			String[] split = efficiency.split(" ");
			this.brakingConditions = Integer.parseInt(split[1]);
		} else {
			this.brakingConditions = 10;
		}
	}

	// sets the amount of lanes by user input
	public void setInsideLanes(boolean insideLane) {
		this.insideLanes = insideLane;
	}

	// Sets maximum speed, and validation of user input
	public void setMaxSpeed(String maxSpeed) {
		if (maxSpeed.isEmpty()) {
			maxSpeed = "80";
		}
		int ms = Integer.parseInt(maxSpeed);
		if (ms != 0) {
			this.maxSpeed = ms;
		} else {
			this.maxSpeed = 1;
		}
	}

	// returns the maximum speed, used in the Main
	public int getMaxSpeed() {
		return this.maxSpeed;
	}

	// returns the set of cars list
	public ArrayList<Car> getCars() {
		return cars;
	}

}
