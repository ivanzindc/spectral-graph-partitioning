import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.*;
import javax.swing.*;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * Class instantiates, calculates, populates and returns
 * all graph features for the program.
 * Graph is in "has a" relationships with both Vertex and Matrix classes.
 * Graph is also completely owned by the SpectralGraphApp class.
 * 
 * @author Ivan Zaluzhnyy
 * @version 2026-05-08
 */

public class Graph {

    private ArrayList<Vertex> vertices; // stores references to each vertex of the graph
    private Matrix adjacencyMatrix; // stores info about each edge of the graph
    private Matrix degreeMatrix; // stores info about number of edges of each vertex (on the main diagonal)
    private Matrix laplacian; // calculated as a difference of degree and adjacency matrices for the graph

    private ArrayList<int []> pendingEdges;

    private boolean partitioned; // flips to true once calculatePartition() finishes successfully;
    // used by draw() to decide whether to highlight cut edges and use group colors

    // special goldenrod web color (based on the Google search results):
    private static final Color DARK_GOLD = new Color(184, 134, 11);
    // will be used for one of partition groups and matches overall color theme

    // Numerical tolerance for treating Fiedler value as zero (disconnected graph).
    // Eliminates "noisy" zeros due to floating point rounding errors
    private static final double LAMBDA2_ZERO_TOLERANCE = 1e-9;

    // Upper limit on lambda2 above which the graph is treated as too well-connected
    // to produce a meaningful partition (per Cheeger's inequality: large lambda2 implies
    // large Cheeger constant, i.e. no good cut exists). 
    // documented in writeup.
    private static final double LAMBDA2_DENSE_TOLERANCE = 0.9;

    // empirically optimized for the model:
    private static final double CUT_RATIO_TOO_HIGH = 0.9;

    /* main() included for testing purposes only: 
    public static void main (String [] args) {

        Graph graph = new Graph("Sample Graph.txt");

        int size = graph.getAdjMatrix().getMatrix().length;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {

                System.out.print( (int) graph.getAdjMatrix().getMatrix()[i][j] + " ");
            }

            System.out.println();
        }

        // graph.calculateDegreeMatrix();
        // graph.calculateLaplacian();

        // EigenDecomposition ed = graph.getLaplacian().eigendecomposition();

        // graph.getLaplacian().calculateEigenValues(ed);
        // graph.getLaplacian().calculateEigenVectors(ed);

        // double [] eigenValues = graph.getLaplacian().getEigenValues();

        // double [][] eigenVectors = graph.getLaplacian().getSpectralVectors();

        graph.calculatePartition();

        double [] eigenValues = graph.getLaplacian().getEigenValues();
        double [][] spectralVectors = graph.getLaplacian().getSpectralVectors();

        System.out.println( "\nEigenvalues:" );
        for ( int i = 0; i < eigenValues.length; i++ ) {
            System.out.printf( "  evs[%d] = %+.4f\n", i, eigenValues[i] );
        }

        System.out.println( "\nFiedler vector (spectralVectors[0]):" );
        for ( int i = 0; i < spectralVectors[0].length; i++ ) {
            System.out.printf( "  v2[%d] = %+.4f\n", i, spectralVectors[0][i] );
        }

        System.out.println( "\nThird eigenvector (spectralVectors[1]):" );
        for ( int i = 0; i < spectralVectors[1].length; i++ ) {
            System.out.printf( "  v3[%d] = %+.4f\n", i, spectralVectors[1][i] );
        }

        System.out.println( "\nVertices partitioned as:" );
        for ( int i = 0; i < graph.getVertices().size(); i++ ) {
            System.out.printf( "  Vertex%d Partition = %d\n", i, graph.getVertices().get(i).getPartitionGroup() );
        }
    } 
    */

    // Constructors:

    /**
     * Constructs a graph through GUI input of vertices and edges by user.
     */
    public Graph() {

        this.partitioned = false;
        this.vertices = new ArrayList<Vertex>();
        this.pendingEdges = new ArrayList<int[]>();
    }

    /**
     * Convenience constructor; uses a default canvas size of 600 by 500.
     * Retained so Graph.main() and other tests still compile.
     * 
     * @param n number of graph vertices
     * @param p probability of an edge between any two vertices
     */
    public Graph(int n, double p) {

        this(n, p, 600, 500);
        this.partitioned = false;
    }

    /**
     * Constructs a random graph based on user-provided number of vertices
     * and a probability of an edge between any two vertices.
     * Vertex coordinates are clamped to the actual canvas dimensions
     * passed in by the caller, so vertices never spill outside the canvas.
     * 
     * @param n number of graph vertices
     * @param p probability of an edge between any two vertices
     * @param width actual canvas width in pixels
     * @param height actual canvas height in pixels
     */
    public Graph(int n, double p, int width, int height) {

        this.partitioned = false;

        this.vertices = new ArrayList<Vertex>();

        adjacencyMatrix = new Matrix(n, n);

        int pad = 25; // pad margins to keep vertices off the canvas edge

        // Create n vertices at random locations:
        for (int i = 0; i < n; i++) {

            int x = (int)(Math.random() * (width  - pad * 2)) + pad;
            int y = (int)(Math.random() * (height - pad * 2)) + pad;

            // currently allows for overlapping vertices:

            addVertex(x, y);
        }

        // Create edges according to the specified probability:
        int size = vertices.size();

        for (int i = 0; i < size; i++) {
            for (int j = i + 1; j < size; j++) {

                double prob = Math.random();

                if ( prob < p ) {
                    adjacencyMatrix.setMatrix(i, j, 1);
                    adjacencyMatrix.setMatrix(j, i, 1);
                }
            }
        }
    }

    /**
     * Convenience constructor; uses default canvas size of 600 by 500.
     * Retained so Graph.main() and other tests still compile.
     * 
     * @param filename name and path, if applicable, of the file containing the graph adjacency matrix
     */
    public Graph(String filename) {

        this(filename, 600, 500);
        this.partitioned = false;
    }

    /**
     * Reads the graph from user-provided text file containing an adjacency matrix of edges.
     * Vertex coordinates are clamped to the actual canvas dimensions
     * passed in by the caller, so vertices never spill outside the canvas.
     * 
     * @param filename name and path, if applicable, of the file containing the graph adjacency matrix
     * @param width actual canvas width in pixels
     * @param height actual canvas height in pixels
     */
    public Graph(String filename, int width, int height) {

        int colNum = 0;
        int rowNum = 0;
        int numVertices = 0; //stores actual number of vertices
    
        try {
            // pass 1: count rows
            Scanner fileScanner = new Scanner(new File(filename));
            while ( fileScanner.hasNextLine() ) {
                String row = fileScanner.nextLine();
                if ( !row.trim().isEmpty() ) { numVertices++; }
            }
            fileScanner.close();
        }
        catch ( FileNotFoundException fnfe ) {
            // fulfills compiler req but propagates exception to GUI for user to find out:
            throw new IllegalArgumentException( "File not found: " + filename );
        }

        this.adjacencyMatrix = new Matrix(numVertices, numVertices);

        this.vertices = new ArrayList<Vertex>();

        int pad = 25; // pad margins to keep vertices off the canvas edge

        for ( int i = 0; i < numVertices; i++ ) {

            // Pick a random point inside the padded canvas area:
            int x = (int)(Math.random() * (width  - pad * 2)) + pad;
            int y = (int)(Math.random() * (height - pad * 2)) + pad;

            this.vertices.add( new Vertex(x, y) );
        }

        rowNum = 0;

        try {
            Scanner fileScanner = new Scanner(new File(filename));

            while (fileScanner.hasNextLine()) {
                String row = fileScanner.nextLine();
                Scanner rowScanner = new Scanner(row);
                rowNum++;
                while (rowScanner.hasNext()) {
                    String edge = rowScanner.next();
                    colNum += 1;

                    int zeroOne = Integer.parseInt(edge);
                    if ( ( zeroOne == 0 || zeroOne == 1 ) && ( colNum <= numVertices ) ) {
                        adjacencyMatrix.setMatrix(rowNum - 1, colNum - 1, zeroOne);
                        //adjacencyMatrix.setMatrix(colNum, rowNum, zeroOne);
                    }
                    else {
                        throw new IllegalArgumentException( "File data does not match a graph adjacency matrix format.");
                    }
                }

                // Each row must have exactly numVertices entries.
                // Otherwise the file is not a square adjacency matrix:
                if ( colNum != numVertices ) {
                    rowScanner.close();
                    throw new IllegalArgumentException( "File data does not match a square adjacency matrix format.");
                }

                colNum = 0;
                rowScanner.close();
            }

            if ( rowNum != numVertices ) {
                throw new IllegalArgumentException( "File data does not match a graph adjacency matrix format.");
            }
            fileScanner.close();
        }
        catch (FileNotFoundException fnfe) {

            throw new IllegalArgumentException( "File not found: " + filename );
        }

    }

    // Instance methods:

    /**
     * Method creates and adds a vertex to the graph.
     * 
     * @param x the x-coordinate of the vertex
     * @param y the y-coordinate of the vertex
     */
    void addVertex(int x, int y) {

        vertices.add(new Vertex(x, y));
    }

    /**
     * Creates an edge between two vertices.
     * Stores edge data in pendingEdges.
     * Used for manually drawn graphs only.
     * calculatePartition() later converts pendingEdges into the adjacency matrix.
     * 
     * @param v1 first vertex in a pair
     * @param v2 second vertex in a pair
     */
    void addEdge(Vertex v1, Vertex v2) {

        int i = this.vertices.indexOf(v1);
        int j = this.vertices.indexOf(v2);

        this.pendingEdges.add(new int [] {i, j});
    }

    /**
     * Helper method for addEdge() method above:
     * IDs Vertex clicked in GUI by user to create an edge between two vertices.
     * 
     * @param x x-coordinate
     * @param y y-coordinate
     * @return Vertex object IDed by those 2 coordinates
     */
    public Vertex findVertexAt(int x, int y) {

        int radius = 10; // "sensitivity" radius: first vertex w/i 10 pixels of the mouse click will be chosen

        for (Vertex v : vertices) {
            int dx = x - v.getX();
            int dy = y - v.getY();

            // if clicked point lies w/i sensitivity radius of a vertex, choose this vertex:
            if (dx * dx + dy * dy <= radius * radius) {
                return v;
            }
        }

        return null;
    }

    /**
     * Draws edges of appropriate color, depending on when and where in the program it's invoked.
     * Uses Vertex draw() method for drawing vertices.
     * Edges come from an adjacencyMatrix object for upload and random
     * graph generation and from pendingEdges for graph via user input.
     * 
     * @param g required Graphics object argument for drawing in Swing 
     */
    public void draw(Graphics g) {

        g.setColor(Color.GRAY);

        if ( this.adjacencyMatrix != null ) {

            int n = this.adjacencyMatrix.getMatrix().length;

            // handles the visual difference between input mode and results mode -- 
            // before partitioning it draws plain gray edges, while after partitioning 
            // it highlights cut edges in black and within-group edges in light gray:
            for (int i = 0; i < n; i++) {
                // symmetric matrices with zeros on main diagonal only need to iterate upper triangle:
                for (int j = i + 1; j < n; j++) {

                    if ( this.adjacencyMatrix.getMatrix()[i][j] == 1 ) {

                        Vertex v1 = this.vertices.get(i);
                        Vertex v2 = this.vertices.get(j);

                        // pick edge color based on whether partition has run
                        // and whether this edge crosses the partition boundary:
                        if ( this.partitioned ) {
                            if ( v1.getPartitionGroup() != v2.getPartitionGroup() ) {
                                g.setColor(Color.BLACK);  // cut edge
                            }
                            else {
                                g.setColor(Color.LIGHT_GRAY);  // within-group edge
                            }
                        }
                        else {
                            g.setColor(Color.GRAY);  // input screen: all edges plain
                        }

                        g.drawLine(v1.getX(), v1.getY(), v2.getX(), v2.getY());
                    }
                }
            }
        }
        else if ( this.pendingEdges != null ) {

            for (int [] edge : this.pendingEdges) {

                Vertex v1 = this.vertices.get(edge[0]);
                Vertex v2 = this.vertices.get(edge[1]);

                g.drawLine(v1.getX(), v1.getY(), v2.getX(), v2.getY());
            }
        }

        // Draw vertices last so they appear on top of edge lines:
        for (Vertex v : this.vertices) {

            v.draw(g);
        }
    }

    /**
     * Calculates graph degree matrix based on its adjacency matrix.
     */
    private void calculateDegreeMatrix() {

        int n = adjacencyMatrix.getMatrix().length;
        degreeMatrix = new Matrix(n, n);

        for (int i = 0; i < n; i++) {

            int sumEdges = 0;

            for (int j = 0; j < n; j++) {

                sumEdges += adjacencyMatrix.getMatrix()[i][j];
            }

            degreeMatrix.setMatrix(i, i, sumEdges);
        }
    }

    /**
     * Calculates graph Laplacian based on its degree and adjacency matrices.
     */
    private void calculateLaplacian() {

        int n = adjacencyMatrix.getMatrix().length;

        laplacian = new Matrix(n, n);

        laplacian = degreeMatrix.subtract(adjacencyMatrix);
    }

    /**
     * Bubble sorts vertex indices in ascending order of their Fiedler component.
     * 
     * @param fiedler array of Fiedler vector components
     * @return array of vertex indices sorted by ascending fiedler[i]
     */
    private int [] sortVerticesByFiedler(double [] fiedler) {

        int n = fiedler.length;
        int [] order = new int [n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }

        boolean sorted = true;
        for (int i = 1; i < n; i++) {
            for (int j = n - 1; j >= i; j--) {
                // compare via fiedler[order[j]], not order[j] directly,
                // i.e. sort indices by values of Fiedler components
                if ( fiedler[order[j - 1]] > fiedler[order[j]] ) {
                    int temp = order[j - 1];
                    order[j - 1] = order[j];
                    order[j] = temp;
                    sorted = false;
                }
            }
            if (sorted) {
                return order;
            }
            else sorted = true;
        }
        return order;
    }

    /**
     * Computes the cut ratio for one proposed split along the Fiedler ordering.
     * The first side contains order[0] through order[k], and the second side
     * contains order[k + 1] through order[n - 1]. The order array stores original
     * vertex indices sorted by Fiedler-vector component, so adj[order[i]][order[j]]
     * checks whether the two original vertices at those Fiedler ranks share an edge.
     * 
     * Important: order is an array of original vertex indices sorted by Fiedler
     * component, which is why edge lookup uses adj[order[i]][order[j]].
     *
     * This is a Java translation/adaptation of Roch's Python cut_ratio(A, order, k)
     * helper. The formula is:
     *   cut ratio = crossing edges / min(size of side 1, size of side 2)
     * The denominator discourages splits that simply isolate a tiny group.
     *
     * Source:
     *   Sebastien Roch, "Mathematical Methods in Data Science",
     *   Section 5.5: Application -- graph partitioning via spectral clustering,
     *   https://mmids-textbook.github.io/chap05_specgraph/05_partitioning/roch-mmids-specgraph-partitioning.html
     *   Accompanying source code: utils/mmids.py, cut_ratio(A, order, k),
     *   https://github.com/MMiDS-textbook/MMiDS-textbook.github.io/blob/main/utils/mmids.py
     *
     * @param order array of original vertex indices sorted by Fiedler component
     * @param k     split point
     * @return cut ratio for this candidate partition
     */
    private double cutRatio(int [] order, int k) {

        // order.length equals the number of vertices because order contains
        // each original vertex index exactly once, sorted by Fiedler vector component;
        int n = order.length;

        // Get the plain 2D array inside the adjacency Matrix object.
        // adj[a][b] is 1 if original vertices a and b share an edge, 0 otherwise.
        double [][] adj = adjacencyMatrix.getMatrix();

        // edgeBoundary will count how many edges cross from one proposed side
        // of the cut to the other proposed side.
        int edgeBoundary = 0;

        // i walks through the left side of the proposed split:
        // order[0], order[1], ..., order[k].
        for (int i = 0; i <= k; i++) {

            // j walks through the right side of the proposed split:
            // order[k + 1], ..., order[n - 1].
            for (int j = k + 1; j < n; j++) {

                // order[i] and order[j] convert Fiedler-order positions back
                // into original vertex indices, which are the indices used by adj.
                // If there is an edge between those two original vertices,
                // adj[...] is 1 and edgeBoundary increases by 1.
                edgeBoundary += adj[order[i]][order[j]];
            }
        }

        // Left side size is k + 1 because positions 0 through k are included.
        // Right side size is n - k - 1 because positions k + 1 through n - 1 remain.
        // Roch's denominator uses the smaller side to penalize tiny one-sided cuts.
        int denominator = Math.min(k + 1, n - k - 1);

        return (double) edgeBoundary / denominator;
    }

    /**
     * Tries every possible split point along the Fiedler-sorted ordering and
     * returns the split that gives the smallest cut ratio. This is the sweep step:
     * each candidate k is scored by cutRatio(order, k), and the best score is kept
     * in bestPhi with its split point stored in bestK.
     *
     * This adapts the split-selection part of Roch's Python spectral_cut2(A).
     * Roch's function computes the Laplacian, gets the Fiedler vector, sorts the
     * vertices, computes all cut ratios, and takes the argmin. In this project,
     * calculatePartition() and Matrix handle the eigendecomposition upstream; this
     * helper only performs the final sweep over candidate split points.
     *
     * Source:
     *   Sebastien Roch, "Mathematical Methods in Data Science",
     *   Section 5.5: Application -- graph partitioning via spectral clustering,
     *   https://mmids-textbook.github.io/chap05_specgraph/05_partitioning/roch-mmids-specgraph-partitioning.html
     *   Accompanying source code: utils/mmids.py, spectral_cut2(A),
     *   https://github.com/MMiDS-textbook/MMiDS-textbook.github.io/blob/main/utils/mmids.py
     *
     * @param order array of original vertex indices sorted by Fiedler component
     * @return split index k
     */
    private int findBestSplitIndex(int [] order) {

        int n = order.length;

        // bestPhi stores the lowest cut ratio seen so far.
        //Positive infinity guarantees that the first real candidate will replace it.
        double bestPhi = Double.POSITIVE_INFINITY;

        //bestK stores the split point that produced bestPhi.
        //starts at 0 because k = 0 is the first valid split.
        int bestK = 0;

        // Try every valid split point.
        // k stops at n - 2 because k = n - 1 would put all vertices on one side
        // and leave the other side empty, which is not a valid cut.
        for (int k = 0; k < n - 1; k++) {

            //candidate split:
            double phi = cutRatio(order, k);

            //If this candidate is better, update the best score and the split point.
            if ( phi < bestPhi ) {
                bestPhi = phi;
                bestK = k;
            }
        }
        return bestK;
    }

    /**
     * Repositions vertices on the results canvas: group 0 in the left half,
     * group 1 in the right half. Within each half, vertices are placed around
     * a circle. Cut edges become the only edges that span the canvas midline,
     * so they stand out visually after draw() colors them black.
     *
     * @param canvasWidth  width of the results canvas in pixels
     * @param canvasHeight height of the results canvas in pixels
     */
    private void layoutByGroup(int canvasWidth, int canvasHeight) {

        ArrayList<Vertex> group0 = new ArrayList<Vertex>();
        ArrayList<Vertex> group1 = new ArrayList<Vertex>();
        for (Vertex v : vertices) {
            if ( v.getPartitionGroup() == 0 ) { group0.add(v); }
            else                              { group1.add(v); }
        }

        int margin = 30;
        int halfWidth = canvasWidth / 2;

        placeGroupInRegion(group0, margin, margin,
                            halfWidth - margin, canvasHeight - margin);
        placeGroupInRegion(group1, halfWidth + margin, margin,
                            canvasWidth - margin, canvasHeight - margin);
    }

    /**
     * Places the given list of vertices around a circle inside the rectangle
     * from (xMin, yMin) to (xMax, yMax). This is used after partitioning so
     * each partition group is displayed in its own region of the results canvas.
     *
     * Adapted from JGraphT's CircularLayoutAlgorithm2D, which places graph
     * vertices evenly spaced around a circle. The simplified version here uses
     * the same basic idea but stores the coordinates directly in Vertex objects.
     *
     * Source:
     *   JGraphT, CircularLayoutAlgorithm2D.java
     *   https://github.com/jgrapht/jgrapht/blob/master/jgrapht-core/src/main/java/org/jgrapht/alg/drawing/CircularLayoutAlgorithm2D.java
     *
     * License:
     *   EPL 2.0 or LGPL 2.1-or-later
     *
     * @param group list of vertices to place
     * @param xMin  left edge of placement region
     * @param yMin  top edge of placement region
     * @param xMax  right edge of placement region
     * @param yMax  bottom edge of placement region
     */
    private void placeGroupInRegion(ArrayList<Vertex> group,
                                    int xMin, int yMin, int xMax, int yMax) {

        int count = group.size();
        if ( count == 0 ) { return; }

        //method first computes the center and usable radius 
        // of each group’s rectangular display region, 
        //then spaces the group’s vertices evenly around that circle 
        //using sin and cos coordinates.

        int centerX = ( xMin + xMax ) / 2;
        int centerY = ( yMin + yMax ) / 2;

        int width = xMax - xMin;
        int height = yMax - yMin;

        // Circle radius for arranging this group inside its display region.
        int radius = Math.min( width, height ) / 2 - 25;

        if ( radius < 20 ) {
            radius = 20;
        }

        if ( count == 1 ) {
            group.get(0).setX(centerX);
            group.get(0).setY(centerY);
            return;
        }

        //Divide a full circle into equal angle steps so vertices are spaced evenly.
        double angleStep = 2 * Math.PI / count;

        for (int i = 0; i < count; i++) {
            int x = centerX + (int)( radius * Math.cos( angleStep * i ) );
            int y = centerY + (int)( radius * Math.sin( angleStep * i ) );

            group.get(i).setX(x);
            group.get(i).setY(y);
        }
    }

    /**
     * Partitions graph based on its eigendecomposition information.
     * 
     * Builds adjacency matrix for user-input mode only.
     * Computed degree matrix, Laplacian, and eigendecomposition.
     * Reads Fiedler value and refuses to partition, if it's zero 
     * (disconnected graph) or too large (no clear bottleneck,
     * according to Cheeger's inequality: more info in project references)
     * Reads Fiedler vector, sorts vertices.
     * Picks the split with the lowest cut ratio among candidate splits.
     * This had to be added because original sign-change rule did not produce correct
     * results for less obvious partitions (more in the write-up)
     * Assigns group colors based on the chosen split.
     * Repositions vertices into two regions for visual clarity on the results canvas.
     * Within each region, vertices are placed around a circle using a simplified
     * layout adapted from JGraphT's CircularLayoutAlgorithm2D.
     * 
     * @param canvasWidth width of the results canvas for new layout
     * @param canvasHeight height of the results canvas
     * @return true if partition succeeded, false o.w.
     */
    public boolean calculatePartition( int canvasWidth, int canvasHeight ) {

        // For GUI-driven graph construction:
        // If no adjacency matrix exists yet, build it from pendingEdges.
        // This also handles the case where the user drew vertices but no edges.
        if ( this.adjacencyMatrix == null ) {

            int n = this.vertices.size();
            this.adjacencyMatrix = new Matrix( n, n );

            if ( this.pendingEdges != null ) {
                for ( int[] edge : this.pendingEdges ) {
                    this.adjacencyMatrix.setMatrix( edge[0], edge[1], 1 );
                    this.adjacencyMatrix.setMatrix( edge[1], edge[0], 1 );
                }
            }
        }

        // safeguard: reject graphs with fewer than 2 vertices
        if ( this.vertices.size() < 2 ) {
            JOptionPane.showMessageDialog( null,
                "Graph must contain at least 2 vertices to be partitioned.",
                "Partition Refused", JOptionPane.WARNING_MESSAGE );
            return false;
        }

        calculateDegreeMatrix();
        calculateLaplacian();

        EigenDecomposition ed = getLaplacian().eigendecomposition();

        getLaplacian().calculateEigenValues( ed );
        getLaplacian().calculateEigenVectors( ed );

        // Apache Commons returns eigenvalues in descending order, so the
        // 2nd smallest (Fiedler value, lambda2) is at index n-2:
        double [] eigenValues = getLaplacian().getEigenValues();
        int n = eigenValues.length;
        double lambda2 = eigenValues[ n - 2 ];

        // Disconnected graph check (lambda2 ~ 0 if graph is disconnected):
        if ( Math.abs( lambda2 ) < LAMBDA2_ZERO_TOLERANCE ) {
            JOptionPane.showMessageDialog( null,
                "Graph appears to be disconnected (Fiedler value is approximately zero).\n" +
                "Spectral partitioning is only meaningful for connected graphs.\n\n",
                "Partition Refused", JOptionPane.WARNING_MESSAGE );
            return false;
        }

        // Dense graph check (large lambda2 means no good cut exists per Cheeger's
        // inequality: lambda2/2 <= h(G) where h(G) is the Cheeger constant):
        if ( lambda2 > LAMBDA2_DENSE_TOLERANCE ) {
            JOptionPane.showMessageDialog( null,
                "Graph is too well-connected to produce a clear partition.\n",
                "Partition Refused", JOptionPane.WARNING_MESSAGE );
            return false;
        }

        // store Fiedler component per vertex:
        double [][] spectralVectors = getLaplacian().getSpectralVectors();
        double [] fiedler = spectralVectors[0];

        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).setFiedlerComponent(fiedler[i]);
        }

        // sort vertices by Fiedler component, then iterate to find the best split:
        int [] order = sortVerticesByFiedler(fiedler);
        // best is the split index in the Fiedler-sorted order array. 
        // Ranks 0 through best go to group 0, and ranks best + 1 through n - 1 go to group 1.
        int best = findBestSplitIndex(order);

        // number of edges crossing between the two groups / size of the smaller group
        double bestPhi = cutRatio(order, best);

        // Refuse partitions whose best available split is still messy.
        // A high cut ratio means the smaller group has too many edges
        // crossing back to the other group, so the graph does not have
        // a visually clear weak-link partition.
        if ( bestPhi > CUT_RATIO_TOO_HIGH ) {
            JOptionPane.showMessageDialog(null,
                "No clear partition was found.\n" +
                "Even the best available cut has too many crossing edges.",
                "Partition Refused", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        //Assign groups by walking through the Fiedler-sorted order.
        //rank is a position in order[], not the original vertex number.
        //order[rank] gives the original vertex index at that Fiedler rank.
        //Ranks 0 through best go to group 0; all later ranks go to group 1:
        for (int rank = 0; rank < n; rank++) { // rank is position in the sorted array of indices
            Vertex v = vertices.get(order[rank]);
            if ( rank <= best ) {
                v.setPartitionGroup(0);
                v.setColor(Color.DARK_GRAY);
            }
            else {
                v.setPartitionGroup(1);
                v.setColor(DARK_GOLD);
            }
        }

        // reposition vertices into two circle-based regions on canvas:
        layoutByGroup(canvasWidth, canvasHeight);

        this.partitioned = true;
        return true;
    }


    // Accessors and modifiers:

    public Matrix getAdjMatrix() {
        return this.adjacencyMatrix;
    }

    public Matrix getDegreeMatrix() {
        return this.degreeMatrix;
    }

    public Matrix getLaplacian() {
        return this.laplacian;
    }

    public ArrayList<Vertex> getVertices() {

        return this.vertices;
    }

    public ArrayList<int[]> getPendingEdges() {

    return this.pendingEdges;
    }

    public boolean isPartitioned() {

    return this.partitioned;
    }
}
