import java.awt.*;

/**
 * Class used for creating and classifying individual graph vertices.
 * Stores vertex-level data and contains a drawing method for vertex input by user through GUI.
 * 
 * @author Ivan Zaluzhnyy
 * @version 2026-04-18
 */

public class Vertex {

    private int x; // x-coordinate of the vertex
    private int y; // y-coordinate of the vertex
    private Color color; //current color of the vertex
    private double fiedlerComponent; // Fiedler vector coordinate of the vertex
    private int partitionGroup; // connectivity group number of the vertex (if applicable)

    // Constructor:
    public Vertex() {

        this(0, 0);
    }
    
    public Vertex(int x, int y) {

        this.x = x;
        this.y = y;
        this.color = Color.LIGHT_GRAY;
    }

    /**
     * Method for manually drawing vertex in GUI.
     * 
     * @param g must-pass Grphics object for the draw() method
     */
    void draw(Graphics g) {

        int radius = 10;    // radius of the vertex circle in pixels

        g.setColor(color);
        g.fillOval(x - radius, y - radius, radius * 2, radius * 2);
    }
    
    // Accessors and modifiers:

    public int getX() {
        
        return this.x;
    }

    public int getY() {

        return this.y;
    }

    public void setX(int x) {

        this.x = x;
    }

    public void setY(int y) {

        this.y = y;
    }

    public Color getColor() {

        return this.color;
    }

    public void setColor(Color color) {

        this.color = color;
    }

    public double getFiedlerComponent() {

        return this.fiedlerComponent;
    }

    public void setFiedlerComponent(double fiedlerComponent) {

        this.fiedlerComponent = fiedlerComponent;
    }

    public int getPartitionGroup() {

        return this.partitionGroup;
    }

    public void setPartitionGroup(int partitionGroup) {

        this.partitionGroup = partitionGroup;
    }
}
