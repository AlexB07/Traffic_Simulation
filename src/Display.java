import javafx.scene.canvas.*;
import javafx.scene.paint.*;
import javafx.geometry.*;

/** Class to display our environment (i.e. the road and the cars) */
public class Display extends Canvas
{
    /** Last known size of our display, or null if not yet known */
    private Bounds bounds;
    /** Height of each lane in pixels */
    private int laneHeight = 24;
    /** Height of car in pixels */
    private int carHeight = 22;
    /** Distance to move the display off the left of the panel */
    private int xOffset = 0;
    /** Distance to move the display off the top of the panel */
    private int yOffset = 16;
    /** The Environment that we are displaying */
    public Environment environment;

    public Display(Environment environment) {
        super(800, 300);
        this.environment = environment;
    }

    /** Set where the end of our road is, in pixels */
    public void setEnd(int end) {
        if (bounds == null) {
            return;
        }

        /* Scroll the road leftwards so that we can see the end of the road at the right of the display */
        if ((end + xOffset) + (bounds.getWidth() / 4) > bounds.getWidth()) {
            xOffset -= bounds.getWidth() / 4;
        }
    }

    /** Reset to our initial state */
    public void reset() {
        xOffset = 0;
    }

    /** Draw a car.
     *  @param position Car position.
     *  @param lane Car lane.
     *  @param color Car colour.
     */
    public void car(int position, int lane, Color color) {
        GraphicsContext gc = getGraphicsContext2D();

        int pad = laneHeight - carHeight;
        gc.setStroke(Color.BLACK);
        gc.strokeRect(xOffset + position, yOffset + lane * laneHeight + pad / 2, (int) environment.carLength(), laneHeight - pad);
        gc.setFill(color);
        gc.fillRect(xOffset + position, yOffset + lane * laneHeight + pad / 2, (int) environment.carLength(), laneHeight - pad);

    }

    /** Draw the whole display; we do the roads, then ask Environment to draw the cars */
    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        bounds = getBoundsInLocal();

        /* Draw the road lines */
        gc.setStroke(Color.LIGHTSTEELBLUE);
        for (int i = 1; i < environment.getLanes(); ++i) {
            gc.beginPath();
            gc.moveTo(0, yOffset + i * laneHeight);
            gc.lineTo(bounds.getWidth(), yOffset + i * laneHeight);
            gc.stroke();
        }

        /* Draw the main road lines */
        gc.setLineDashes(null);
        gc.beginPath();
        gc.setStroke(Color.BLACK);
        gc.moveTo(0, yOffset);
        gc.lineTo(bounds.getWidth(), yOffset);
        gc.moveTo(0, yOffset + environment.getLanes() * laneHeight);
        gc.lineTo(bounds.getWidth(), yOffset + environment.getLanes() * laneHeight);
        gc.stroke();

        /* Draw the cars */
        environment.draw();
    }

    public void setCarHeight(int carHeight) {
        this.carHeight = carHeight;
    }

    public void setLaneHeight(int laneHeight) {
        this.laneHeight = laneHeight;
    }
    //returns xOffSetValue
    public int getXOffSet() {
    	return xOffset;
    }
    
}
