import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * GUI for informing, accepting input and displaying results to user.
 * 
 * @author Ivan Zaluzhnyy
 * @version 2026-04-18
 */

public class SpectralGraphApp extends JFrame {

    // Variables declared as fields so listeners can reach them:

    private CardLayout cardLayout; // CardLayout is called when flipping cards via listener
    private JPanel screens;

    private JPanel welcomePanel;
    private JPanel inputPanel;
    private JPanel resultsPanel;

    private int inputWidth = 600;
    private int inputHeight = 520;

    private JButton uploadButton;
    private JButton randomButton;
    private JButton drawButton;

    private Graph graph;
    private GraphCanvas canvas;

    private JButton modeButton;       // toggles b/w "DONE ADDING VERTICES" / "DONE ADDING EDGES"
    private JButton partitionButton;
    private JButton backButton;

    // Results screen components:
    private GraphCanvas resultsCanvas;
    //private JTextArea detailsArea;
    private JButton resultsBackButton;

    private Vertex selectedVertex;    // required for doing edge creation
    private boolean addingVertices;   // will be true only in vertex adding mode
    private boolean manualDrawMode;   // need to prevent exception in other modes

    public static void main (String [] args) {

        CreateSplashScreen.main(args);
        // SpectralGraphApp app = new SpectralGraphApp();
    }

    // Constructor:
    public SpectralGraphApp() {

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 650); // Frame will be larger than the panels
        setTitle("SPECTRAL GRAPH PARTITIONING TOOL");
        setLocationRelativeTo(null); // centers the main app window on screen at startup

        cardLayout = new CardLayout(5, 5);
        screens = new JPanel(cardLayout);
        add(screens);

        layoutComponents();

        addListeners();

        setVisible(true);
    }

    /**
     * Provides GUI elements for the three app screens.
     */
    private void layoutComponents() {

        // "Welcome" Screen Set-Up:

        // Title at the top:
        welcomePanel = new JPanel(new BorderLayout());
        // SwingConstants.CENTER aligns JLabel text in the middle of the label
        JLabel welcomeTitle = new JLabel("Welcome to the Spectral Partitioning Application!",
                                            SwingConstants.CENTER);
        welcomeTitle.setFont( new Font( "Segoe UI", Font.BOLD, 30 ) );

        // Empty border used as padding around the title.
        // Pattern from Oracle Java Tutorials, "How to Use Borders":
        // https://docs.oracle.com/javase/tutorial/uiswing/components/border.html
        welcomeTitle.setBorder( BorderFactory.createEmptyBorder( 20, 10, 10, 10 ) );

        // Instructions in the middle:

        JTextArea welcomeInfo = new JTextArea(
            "This program splits an undirected graph into groups by finding its weakest connection points -- " +
            "the edges whose removal would most cleanly separate the graph. The split is computed from the Fiedler " +
            "vector: the eigenvector associated with the second smallest eigenvalue of the graph's Laplacian matrix. " +
            "Vertices are re-colored to show which group they belong to.\n\n" +
            "Choose an input method below:\n\n" +
            "  \u2022 Load from File                    Read an n by n adjacency matrix from a text file\n" +
            "                                                 (0/1 entries, space-separated, symmetric).\n\n" +
            "  \u2022 Generate Random             Specify n vertices and edge probability p in [0, 1].\n\n" +
            "  \u2022 Draw                                    Click a canvas to place vertices, then click pairs\n" +
            "                                                 of vertices to draw edges.");
        // Unicode char \u2022 is a bullet
        welcomeInfo.setFont(  new Font( "Segoe UI", Font.PLAIN, 18 ) );

        welcomeInfo.setEditable( false );
        welcomeInfo.setLineWrap( true );
        welcomeInfo.setWrapStyleWord( true );
        // built-in color if white; i want it to be background color:
        welcomeInfo.setOpaque( false );
        // default border too small
        welcomeInfo.setBorder( BorderFactory.createEmptyBorder( 10, 30, 10, 30 ) );
        welcomeInfo.setFocusable( false );

        JPanel welcomeButtons = new JPanel(new FlowLayout()); // single row, left to right

        uploadButton = new JButton("LOAD FROM FILE");
        randomButton = new JButton("GENERATE RANDOM");
        drawButton = new JButton("DRAW");

        Font buttonFont = new Font("Segoe UI", Font.BOLD, 22);
        Dimension buttonSize = new Dimension(270, 80);

        uploadButton.setFont(buttonFont);
        randomButton.setFont(buttonFont);
        drawButton.setFont(buttonFont);
        uploadButton.setPreferredSize(buttonSize);
        randomButton.setPreferredSize(buttonSize);
        drawButton.setPreferredSize(buttonSize);

        welcomeButtons.add(uploadButton);
        welcomeButtons.add(randomButton);
        welcomeButtons.add(drawButton);

        welcomePanel.add(welcomeTitle, BorderLayout.NORTH);
        welcomePanel.add(welcomeInfo, BorderLayout.CENTER);
        welcomePanel.add(welcomeButtons, BorderLayout.SOUTH);

        welcomeButtons.setBorder( BorderFactory.createEmptyBorder( 10, 0, 30, 0 ) );

        // "Input" Screen Set-Up:
        inputPanel = new JPanel(new BorderLayout());

        canvas = new GraphCanvas();
        inputPanel.add(canvas, BorderLayout.CENTER);

        JPanel inputControls = new JPanel(new FlowLayout());
        modeButton = new JButton("DONE ADDING VERTICES");
        partitionButton = new JButton("PARTITION");
        backButton = new JButton("BACK");

        Font controlFont = new Font("Segoe UI", Font.BOLD, 16);
        Dimension  controlSize = new Dimension(220, 50);

        modeButton.setFont(controlFont);
        partitionButton.setFont(controlFont);
        backButton.setFont(controlFont);
        modeButton.setPreferredSize(new Dimension(300, 50)); // requires larger width
        partitionButton.setPreferredSize(controlSize);
        backButton.setPreferredSize(controlSize);

        inputControls.add(backButton);
        inputControls.add(modeButton);
        inputControls.add(partitionButton);
        // empty border here and elsewhere in program code ensures that there is "breathing room" b/w elements
        inputControls.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

        inputPanel.add(inputControls, BorderLayout.SOUTH);

        // "results" Screen Set-Up:
        resultsPanel = new JPanel(new BorderLayout());

        // Top: separate canvas instance that draws the same Graph object
        // post-partition. Reusing GraphCanvas inner class so partitioned
        // colors render automatically via Vertex.draw().
        resultsCanvas = new GraphCanvas();
        resultsPanel.add(resultsCanvas, BorderLayout.CENTER);

        /* DETAILS PANE COMMENTED OUT FOR NOW; revisit alongside spectral embedding work.
         *
         * // Bottom region holds both the eigendecomposition details (CENTER)
         * // and the BACK button (SOUTH), so they share BorderLayout.SOUTH
         * // of resultsPanel without overwriting each other.
         * JPanel resultsSouth = new JPanel(new BorderLayout());
         *
         * detailsArea = new JTextArea();
         * detailsArea.setEditable(false);
         * detailsArea.setFont(new Font("Consolas", Font.PLAIN, 13));
         * // Monospaced font keeps the lambda[i] = +0.0000 columns aligned.
         * detailsArea.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
         *
         * JScrollPane detailsScroll = new JScrollPane(detailsArea);
         * // Bounded preferred height so detailsArea does not grow without limit
         * // when Fiedler vector is long for large graphs:
         * detailsScroll.setPreferredSize(new Dimension(0, 180));
         *
         * resultsSouth.add(detailsScroll, BorderLayout.CENTER);
         */

        // Bottom: just the BACK button (mirrors input-screen button strip).
        JPanel resultsButtons = new JPanel(new FlowLayout());
        resultsBackButton = new JButton("BACK");
        resultsBackButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        resultsBackButton.setPreferredSize(new Dimension(220, 50));
        resultsButtons.add(resultsBackButton);
        resultsButtons.setBorder(BorderFactory.createEmptyBorder(10, 0, 20, 0));

        resultsPanel.add(resultsButtons, BorderLayout.SOUTH);

        screens.add( welcomePanel, "welcome" );
        screens.add( inputPanel,   "input"   );
        screens.add( resultsPanel, "results" );

    }

    /**
     * Adds listeners to interactive elements.
     */
    private void addListeners() {

        drawButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {

                graph = new Graph();
                // here and below mode settings are used in part 
                // in order to prevent exceptions due to user error:
                manualDrawMode = true;
                selectedVertex = null;
                modeButton.setEnabled(true);
                addingVertices = true;
                modeButton.setText("DONE ADDING VERTICES");
                canvas.repaint();
                cardLayout.show(screens, "input");
            }
        });

        randomButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {

                String numVertices = JOptionPane.showInputDialog(SpectralGraphApp.this,
                        "Please enter desired number of vertices (n): ");
                String probEdge = JOptionPane.showInputDialog(SpectralGraphApp.this,
                        "Please enter desired probability of an edge (p) in [0, 1.0]: ");
                
                if ( numVertices == null || probEdge == null ) { return; }

                try {
                    int n = Integer.parseInt(numVertices.trim());
                    double p = Double.parseDouble(probEdge.trim());

                    if ( n < 2 || p < 0 || p > 1 ) {
                        JOptionPane.showMessageDialog(SpectralGraphApp.this,
                            "Invalid input. n must be at least 2, and p must be in [0, 1].",
                            "Input Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    graph = new Graph(n, p, canvas.getWidth(), canvas.getHeight());
                    manualDrawMode = false;
                    addingVertices = false;
                    selectedVertex = null;
                    modeButton.setEnabled(false);
                    modeButton.setText("GRAPH LOADED");
                    canvas.repaint();
                    cardLayout.show(screens, "input");
                }
                // NumberFormatException is an unchecked exception for parseInt() and similar
                //Source: https://stackoverflow.com/questions/6456219/java-checking-if-parseint-throws-exception 
                catch (NumberFormatException nfe ) {
                        JOptionPane.showMessageDialog(SpectralGraphApp.this,
                        "Invalid input. n must be an integer, p a decimal in [0, 1].",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                        // "Input Error" is the title; last argument invokes pane with appropriate style
                }
            }
        });

        uploadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {

                String filename = JOptionPane.showInputDialog(SpectralGraphApp.this, 
                    "Please enter the name of your file containing an adjacency matrix" +
                    " or \"Sample Graph.txt\" provided for testing.\n" +
                    "Please include full file path if it is not in the working directory.");

                if ( filename == null ) { return; }

                try {
                    graph = new Graph(filename.trim(), canvas.getWidth(), canvas.getHeight());
                    manualDrawMode = false;
                    addingVertices = false;
                    selectedVertex = null;
                    modeButton.setEnabled(false);
                    modeButton.setText("GRAPH LOADED");
                    canvas.repaint();
                    cardLayout.show(screens, "input");
                }
                catch (IllegalArgumentException iae) { // iae is a generic "bad animal" exception
                    JOptionPane.showMessageDialog(SpectralGraphApp.this, iae.getMessage(),
                                                    "File Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        canvas.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {

                if ( graph == null || !manualDrawMode ) { return; }

                int x = me.getX();
                int y = me.getY();

                if (addingVertices) {

                    // empty spot: add new vertex; on existing vertex: ignore
                    if (graph.findVertexAt(x, y) == null) {

                        int r = 10; // Vertex radius

                        // check if vertex is outside canvas:
                        if ( x < r || x > canvas.getWidth() - r ||
                            y < r || y > canvas.getHeight() - r ) {
                            return;
                        }

                        graph.addVertex(x, y);
                        canvas.repaint();
                    }
                }
                else {

                    // edge mode: pick two existing vertices to connect
                    Vertex clicked = graph.findVertexAt(x, y);
                    if ( clicked == null ) { return; }

                    // if-else logic below assigns first selected vertex to a var
                    // and, if one already assigned, it draws an edge b/w now 2 clicked vertices: 
                    if ( selectedVertex == null ) {
                        selectedVertex = clicked;
                    }
                    else if ( selectedVertex != clicked ) {
                        graph.addEdge(selectedVertex, clicked);
                        selectedVertex = null;
                        canvas.repaint();
                    }
                }
            }
        });

        modeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {

                if ( !manualDrawMode ) { return; }

                addingVertices = !addingVertices; //flips the var value to ensure toggle below
                selectedVertex = null; // clears the var in case edge wasn't finished

                if ( addingVertices ) {
                    modeButton.setText("DONE ADDING VERTICES");
                }
                else {
                    modeButton.setText("ADD MORE VERTICES");
                }
            }
        });

        partitionButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {

                if ( graph == null ) { return; } //safeguard

                // calculatePartition now takes canvas dimensions for results-screen layout
                // and returns false if the partition was refused (disconnected or too dense).
                // resultsCanvas may not yet be realized when partition is first invoked
                // (getWidth/Height return 0 before first show), so fall back to its
                // preferred size in that case:
                int rcw = resultsCanvas.getWidth();     //rcw = results canvas width
                int rch = resultsCanvas.getHeight();    //rch = results canvas height

                // resultsCanvas can report 0 width/height before it is first shown,
                // so its preferred size is used as a fallback.
                if ( rcw <= 0 || rch <= 0 ) {
                    Dimension pref = resultsCanvas.getPreferredSize();
                    rcw = pref.width;
                    rch = pref.height;
                }
                boolean ok = graph.calculatePartition(rcw, rch);
                if ( !ok ) { return; }

                cardLayout.show(screens, "results");
                resultsCanvas.revalidate();
                resultsCanvas.repaint();
    }});

        // Full reset of the graph interface which enables user to start over
        backButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {

                graph = null;
                manualDrawMode = false;
                selectedVertex = null;
                addingVertices = true;
                modeButton.setEnabled(true);
                modeButton.setText("DONE ADDING VERTICES");
                cardLayout.show(screens, "welcome");
            }
        });

        resultsBackButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {

                graph = null;
                manualDrawMode = false;
                selectedVertex = null;
                addingVertices = true;
                modeButton.setEnabled(true);
                modeButton.setText("DONE ADDING VERTICES");
                cardLayout.show(screens, "welcome");

                canvas.repaint();
                resultsCanvas.repaint();
            }
        });
    }
       

    // Accessors and modifiers:

    public int getInputWidth() {

        return inputWidth;
    }

    public int getInputHeight() {

        return inputHeight;
    }

    /**
     * Canvas displaying the current graph as an inner class.
     * Calls Graph.draw(g) to draw the current state of the Graph object.
     */
    private class GraphCanvas extends JPanel {

        // Constructor:
        public GraphCanvas() {

            this.setPreferredSize(new Dimension(inputWidth, inputHeight));
            this.setBackground(Color.WHITE);
        }

        /**
         * Paints the current graph onto the canvas.
         * Called by Swing whenever the canvas changes in any way.
         * Never called directly by application code.
         * 
         * @param g the Graphics object provided by Swing
         */
        public void paintComponent(Graphics g) {

            super.paintComponent(g);

            if ( graph != null ) {

                graph.draw(g);
            }
        }
    }
}
