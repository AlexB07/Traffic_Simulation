import javafx.scene.layout.*;
import javafx.scene.*;
import javafx.scene.control.*;
import java.util.*;
import javafx.scene.paint.*;
import javafx.application.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.stage.*;

public class Main extends Application {

	private static Label lblDistance = new Label("Distance: 0 Pixels");
		
	public static void main(String[] args) {
		launch(args);
	}

	public void start(Stage stage) {

		final Environment environment = new Environment();
		final Display display = new Display(environment);
		environment.setDisplay(display);

		// Data analysis labels
		Label lblMean = new Label("Mean: N/a");
		Label lblMedian = new Label("Median: N/a");
		Label lblMode = new Label("Mode: N/a");
		Label lblSD = new Label("Standard Deviation: N/a");
		Label lblRange = new Label("Range: N/a");

		VBox box = new VBox();
		// Store textfield that are numbers only
		ArrayList<TextField> textBoxes = new ArrayList<TextField>();
		stage.setTitle("Traffic");
		stage.setScene(new Scene(box, 800, 600));

		HBox controls = new HBox();
		Button restart = new Button("Restart");
		controls.getChildren().add(restart);
		
		//Braking user input
		ComboBox<String> braking = new ComboBox<String>();
		braking.getItems().addAll("Normal 10", "Wet 25", "Snow 35", "Icy 50");
		braking.setValue("Road Conditions");
		controls.getChildren().add(braking);
		
		// Bad Lane Discipline
		CheckBox cbBld = new CheckBox();
		cbBld.setText("Bad Lane Disapline");
		cbBld.setSelected(false);
		controls.getChildren().add(cbBld);

		// RubberNecking user input
		VBox vBRubberNeckingTextFields = new VBox();
		
		//RubberNecking slow down user input
		TextField txtRubberNecking = new TextField();
		txtRubberNecking.setPromptText("Slow down(In Pixels)");
		txtRubberNecking.setDisable(true);
		textBoxes.add(txtRubberNecking);
		vBRubberNeckingTextFields.getChildren().add(txtRubberNecking);
		
		//RubberNecking speed up user input
		TextField txtSpeedUp = new TextField();
		txtSpeedUp.setPromptText("Speed up (In Pixels)");
		txtSpeedUp.setDisable(true);
		textBoxes.add(txtSpeedUp);
		vBRubberNeckingTextFields.getChildren().add(txtSpeedUp);
		controls.getChildren().add(vBRubberNeckingTextFields);
		
		//RubberNecking checkbox user input
		CheckBox cbRn = new CheckBox();
		cbRn.setSelected(false);
		cbRn.setText("Rubbernecking");
		//Change text-fields depending on whether its true (enabled) or false (disabled)
		cbRn.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue) {
					txtRubberNecking.setDisable(false);
					txtSpeedUp.setDisable(false);
				} else {
					txtRubberNecking.setDisable(true);
					txtSpeedUp.setDisable(true);
				}

			}
		});
		controls.getChildren().add(cbRn);

		//Amount of lanes user input
		ComboBox<String> cmbLanes = new ComboBox<>();
		cmbLanes.getItems().addAll("2", "3", "4", "5", "6", "7", "8", "9", "10");
		cmbLanes.setValue("Lanes");
		controls.getChildren().add(cmbLanes);
		controls.setSpacing(2);
		
		//Max speed user input
		VBox vBTopRight = new VBox();
		TextField txtMaxSpeed = new TextField();
		txtMaxSpeed.setPromptText("Max Speed (In Pixels");
		textBoxes.add(txtMaxSpeed);
		vBTopRight.getChildren().add(txtMaxSpeed);
		
		
		
		vBTopRight.getChildren().add(lblDistance);
		controls.getChildren().add(vBTopRight);
		
		// Validation for text-fields user number only input (Stops user entering anything but numbers)
		for (TextField t : textBoxes) {
			t.textProperty().addListener(new ChangeListener<String>() {

				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					if (!newValue.matches("\\d*")) {
						t.setText(newValue.replaceAll("[^\\d]", ""));
					}

				}
			});
		}
		box.getChildren().add(controls);
		//Restart updates changes from user input
		restart.setOnMouseClicked(e -> {
			// Data analysis resets labels
			lblMean.setText("Mean: N/a");
			lblMedian.setText("Median: N/a");
			lblMode.setText("Mode: N/a ");
			lblRange.setText("Range: N/a");
			lblSD.setText("Standard Deviation: N/a");

			// User condition for braking efficiency
			environment.setBrakingConditions(braking.getValue());

			/*
			 * User rubber necking, changing values for rubber necking to be enabled or
			 * disabled Users can only input positive numbers therefore users input will
			 * always be made a negative number
			 */
			environment.isRubberNeckingTrue(cbRn.isSelected(), txtRubberNecking.getText(), txtSpeedUp.getText());

			// User inside lane
			environment.setInsideLanes(cbBld.isSelected());

			// User lane input
			environment.setLanes(cmbLanes.getValue());

			// User max speed
			environment.setMaxSpeed(txtMaxSpeed.getText());

			//Reset environment and display
			environment.clear();
			display.reset();
			addCars(environment);
		});
		
		box.getChildren().add(display);
		addCars(environment);

		//Data Analysis on car speeds informationCars is the group which data is added to
		VBox infromationCars = new VBox();
		Button btnUpdate = new Button("Update Car Analysis");
		//Updates car speed data
		btnUpdate.setOnAction(e -> {
			ArrayList<Double> temporaryList = getSpeedList(environment);
			double temporaryMean = MeanCalculation(environment, temporaryList);
			double temporaryMode = ModeCalculations(environment, temporaryList);
			lblMean.setText("Mean: " + temporaryMean);
			lblMedian.setText("Median: " + medianCalculations(environment, temporaryList));
			/*Check to see if there is a mode , if not changes to N/a*/
			if (temporaryMode != -1) {
				lblMode.setText("Mode : " + temporaryMode);
			} else {
				lblMode.setText("Mode : N/a");
			}
			lblSD.setText("Standard Deviation " + calculateStandardDeviation(environment, temporaryMean, temporaryList));
			lblRange.setText("Range " + calculateRange(environment, temporaryList));
		});
		infromationCars.getChildren().addAll(btnUpdate, lblMean, lblMedian, lblMode, lblSD, lblRange);
		box.getChildren().add(infromationCars);

		stage.show();
	}

	/**
	 * Add the required cars to an environment.
	 * 
	 * @param e
	 *            Environment to use.
	 */
	private static void addCars(Environment e) {
		/* Add an `interesting' set of cars */
		Random r = new Random();

		e.add(new Car(0, r.nextInt(e.getMaxSpeed()), r.nextInt(e.getLanes()),new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1.0)));
		e.add(new Car(20, r.nextInt(e.getMaxSpeed()), r.nextInt(e.getLanes()),new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1.0)));
		e.add(new Car(170, r.nextInt(e.getMaxSpeed()), r.nextInt(e.getLanes()),new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1.0)));
		e.add(new Car(340, r.nextInt(e.getMaxSpeed()), r.nextInt(e.getLanes()),new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1.0)));
		e.add(new Car(540, r.nextInt(e.getMaxSpeed()), r.nextInt(e.getLanes()),new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1.0)));
		e.add(new Car(300, r.nextInt(e.getMaxSpeed()), r.nextInt(e.getLanes()),new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1.0)));
		e.add(new Car(380, r.nextInt(e.getMaxSpeed()), r.nextInt(e.getLanes()),new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1.0)));
		e.add(new Car(384, r.nextInt(e.getMaxSpeed()), r.nextInt(e.getLanes()),new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1.0)));
		e.add(new Car(460, r.nextInt(e.getMaxSpeed()), r.nextInt(e.getLanes()),new Color(r.nextFloat(), r.nextFloat(), r.nextFloat(), 1.0)));
	}

	// DATA ANALYSIS
	// Calculating the mean of the set of cars
	public static int MeanCalculation(Environment e, ArrayList<Double> list) {
		int mean = 0;
		for (double speed : list) {
			mean += speed;
		}
		mean = (mean / (list.size()));
		return mean;
	}

	// Calculating the median of the cars data set
	public static double medianCalculations(Environment e, ArrayList<Double> carSpeeds) {
		Collections.sort(carSpeeds);
		int middle = ((carSpeeds.size() - 1) / 2);
		if (carSpeeds.size() % 2 == 1) {
			return carSpeeds.get(middle);
		} else {
			return (carSpeeds.get(middle - 1) + carSpeeds.get(middle) / 2.0);
		}
	}

	// Calculating the set of speed data and returning it in a list
	public static ArrayList<Double> getSpeedList(Environment e) {
		ArrayList<Double> carSpeeds = new ArrayList<Double>();
		for (Car i : e.getCars()) {
			carSpeeds.add(i.getSpeed());
		}
		return carSpeeds;
	}

	// Calculating the mode from the speed data set
	public static double ModeCalculations(Environment e, ArrayList<Double> list) {
		double modeValue = -1;
		int maxCount = 0;
		for (int i = 0; i < list.size(); i++) {
			int c = 0;
			for (int k = 0; k < list.size(); k++) {
				if (list.get(k) == list.get(i)) {
					c++;
				}
				if (c > maxCount) {
					maxCount = c;
					modeValue = list.get(i);
				}
				if (maxCount == 1) {
					return -1.0;
				}
			}
		}
		return modeValue;
	}

	// Calculating the standard deviation of the speed data set
	public String calculateStandardDeviation(Environment e, double mean, ArrayList<Double> list) {
		double sd = 0.0;
		for (double n : list) {
			sd += Math.pow(n - mean, 2);
		}
		return  String.format( "%.5f", Math.sqrt(sd / (list.size() - 1)) );
	}

	// Returns the range in string form of the speed data set
	public String calculateRange(Environment e, ArrayList<Double> list) {
		Collections.sort(list);
		return "Minimum: " + String.format( "%.2f", list.get(0) ) + " | Maximum " + String.format( "%.2f", list.get(list.size()-1));
	}
	
	//Sets label to distance
	public static void setDistance(int dist) {
		lblDistance.setText("Distance: " + Integer.toString(dist)+ " Pixels");
	}

};
