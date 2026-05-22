import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * The Matrix class stores its data as a two-dimensional array 
 * and provides methods for matrix subtraction and eigenvalue decomposition.
 * 
 * @author Ivan Zaluzhnyy
 * @version 2026-04-19
 */

public class Matrix {

    private double [][] data;
    private double [] eigenValues;
    private double [][] spectralVectors;

    //Constructors:

    public Matrix() {

        this(0, 0);
    }

    public Matrix(int rows, int cols) {

        this.data = new double [rows][cols];
    }

    /**
     * Does element-wise subtraction.
     * Assumes arrays are not null, have equal dimensions and are not the same instance
     * because for the purposes of this program this will be the case.
     * 
     * @param other Matrix object compatible with the Matrix instance that calls the method.
     */
    public Matrix subtract(Matrix other) {

        int numRows = this.getMatrix().length;
        int numCols = this.getMatrix()[0].length;

        Matrix result = new Matrix(numRows, numCols);


        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++ ) {

                result.setMatrix(i, j, this.getMatrix()[i][j] - other.getMatrix()[i][j]);
            }
        }

        return result;
    }

    /**
     * Uses Apache Commons Math to compute the eigendecomposition of this matrix.
     *
     * @return Apache Commons Math EigenDecomposition object for this matrix
     */
    public EigenDecomposition eigendecomposition() {

        // Array2DRowRealMatrix converts internal double[][] matrix data 
        // into the RealMatrix type expected by Apache Commons Math.
        RealMatrix m = new Array2DRowRealMatrix( data );
        EigenDecomposition ed = new EigenDecomposition( m );
        
        return ed;
    }

    /**
     * Stores the eigenvalues returned by Apache Commons Math.
     *
     * @param ed eigendecomposition object computed from this matrix
     */
    public void calculateEigenValues(EigenDecomposition ed) {

        eigenValues = ed.getRealEigenvalues();
    }

    /**
     * Stores the Fiedler vector from the eigendecomposition.
     * Apache Commons Math returns eigenvalues in descending order, so the
     * second-smallest eigenvalue is read from index n - 2.
     * The matching eigenvector is used by Graph.calculatePartition().
     *
     * @param ed object storing matrix eigenvalues and eigenvectors
     */
    public void calculateEigenVectors(EigenDecomposition ed) {

        //double[] eigenvalues = ed.getRealEigenvalues();
        int n = eigenValues.length; // allows to locate second smallest element in an array of any length
        double fiedlerValue = eigenValues[n - 2]; // second samllest eigenvalue


        spectralVectors = new double[1][]; // second spectral vector no longer used

        // ed.getEigenvector() returns a RealVector — Apache's own class for a vector of doubles.
        // To convert an Apache RealVector into a plain double[], its toArray() method is called.
        // The current app only needs the Fiedler vector.
        spectralVectors[0] = ed.getEigenvector( n - 2 ).toArray();  // Fiedler (2nd smallest)
        //spectralVectors[1] = ed.getEigenvector( n - 3 ).toArray();  // 3rd smallest,
        //used is spectral embedding, which was not accomplished
    }

    // getters/setters for all fields
    public double [][] getMatrix() {
        return this.data;
    }

    public void setMatrix(int row, int col, double value) {
        this.data[row][col] = value;
    }

    public double [] getEigenValues() {

        return eigenValues;
    }

    public double [][] getSpectralVectors() {

        return spectralVectors;
    }

}
