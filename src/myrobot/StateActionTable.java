package myrobot;

import robocode.RobocodeFileOutputStream;

import java.io.*;

public class StateActionTable {
    //define LUT and Visited array as well as array dimensions
    private double[][][][][] lut;
    private int[][][][][] visits;
    private int numDim1;
    private int numDim2;
    private int numDim3;
    private int numDim4;
    private int numDim5;

    //constructor
    public StateActionTable (int numDim1, int numDim2, int numDim3, int numDim4,
                             int numDim5) {
        this.numDim1 = numDim1;
        this.numDim2 = numDim2;
        this.numDim3 = numDim3;
        this.numDim4 = numDim4;
        this.numDim5 = numDim5;
        lut = new double[numDim1][numDim2][numDim3][numDim4][numDim5];
        visits = new int[numDim1][numDim2][numDim3][numDim4][numDim5];
        this.initialize();
    }

    //initialize lum and visits arrays
    public void initialize () {
        for (int i = 0; i < numDim1; i++){
            for (int j = 0; j < numDim2; j++){
                for (int k = 0; k < numDim3; k++){
                    for (int l = 0; l < numDim4; l++){
                        for (int m = 0; m < numDim5; m++){
                            lut[i][j][k][l][m] = Math.random();
                            visits[i][j][k][l][m] = 0;
                        }
                    }
                }
            }
        }
    }

    public double getQValue (double[] x) throws ArrayIndexOutOfBoundsException {
        if (x.length != 5){
            throw new ArrayIndexOutOfBoundsException();
        }
        else{
            int a = (int)x[0];
            int b = (int)x[1];
            int c = (int)x[2];
            int d = (int)x[3];
            int e = (int)x[4];
            return lut[a][b][c][d][e];
        }
    }

    public void setQValue (double[] x, double target) throws ArrayIndexOutOfBoundsException {
        if (x.length != 5){
            throw new ArrayIndexOutOfBoundsException();
        }
        else{
            int a = (int)x[0];
            int b = (int)x[1];
            int c = (int)x[2];
            int d = (int)x[3];
            int e = (int)x[4];
            lut[a][b][c][d][e] = target;
            visits[a][b][c][d][e]++;
        }
    }

    public void save(File fileName) {
        PrintStream saveFile = null;
        try {
            saveFile = new PrintStream(new RobocodeFileOutputStream(fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        saveFile.println(numDim1 * numDim2 * numDim3 * numDim4 * numDim5 );
        saveFile.println(5);
        for (int i = 0; i < numDim1; i++) {
            for (int j = 0; j < numDim2; j++) {
                for (int k = 0; k < numDim3; k++) {
                    for (int l = 0; l < numDim4; l++) {
                        for (int m = 0; m < numDim5; m++) {
                            String row = String.format("%d, %d, %d, %d, %d, %2.5f, %d",
                                    i, j, k, l, m,
                                    lut[i][j][k][l][m],
                                    visits[i][j][k][l][m]
                            );
                            saveFile.println(row);
                        }
                    }
                }
            }
        }
        saveFile.close();
    }

    public void load(String fileName) throws IOException {
        FileInputStream inputFile = new FileInputStream(fileName);
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(inputFile));
        int numExpectedRows = numDim1 * numDim2 * numDim3 * numDim4 * numDim5;
        int numRows = Integer.valueOf(inputReader.readLine());
        int numDimensions = Integer.valueOf(inputReader.readLine());

        if (numRows != numExpectedRows || numDimensions != 5) {
            System.out.printf(
                    "*** rows/dimensions expected is %s/%s but %s/%s encountered\n",
                    numExpectedRows, 5, numRows, numDimensions
            );
            inputReader.close();
            throw new IOException();
        }
        for (int i = 0; i < numDim1; i++) {
            for (int j = 0; j < numDim2; j++) {
                for (int k = 0; k < numDim3; k++) {
                    for (int l = 0; l < numDim4; l++) {
                        for (int m = 0; m < numDim5; m++) {
                            String line = inputReader.readLine();
                            String tokens[] = line.split(",");
                            int dim1 = Integer.parseInt(tokens[0]);
                            int dim2 = Integer.parseInt(tokens[1]);
                            int dim3 = Integer.parseInt(tokens[2]);
                            int dim4 = Integer.parseInt(tokens[3]);
                            int dim5 = Integer.parseInt(tokens[4]);//action
                            double reward = Double.parseDouble(tokens[5]);
                            int numVisits = Integer.parseInt(tokens[6]);
                            lut[i][j][k][l][m] = reward;
                            visits[i][j][k][l][m] = numVisits;
                        }
                    }
                }
            }
        }
        inputReader.close();
    }
}