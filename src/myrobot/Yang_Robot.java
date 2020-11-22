package myrobot;

import robocode.*;
import java.awt.*;
import java.io.IOException;
import java.util.Random;

public class Yang_Robot extends AdvancedRobot{

    public enum Energy {zero, low, medium, high}
    public enum Distance {veryClose, near, far}
    public enum OperationMode {scan, performAction}
    public enum Action {ahead, back, aheadLeft, aheadRight, backLeft, backRight, turnGunFire}

    private Energy currentMyEnergy = Energy.high;
    private Energy currentEnemyEnergy = Energy.high;
    private Distance currentDistanceToEnemy = Distance.near;
    private Distance currentDistanceToCenter = Distance.near;
    private Action currentAction = Action.ahead;

    private Energy previousMyEnergy = Energy.low;
    private Energy previousEnemyEnergy = Energy.low;
    private Distance previousDistanceToEnemy = Distance.veryClose;
    private Distance previousDistanceToCenter = Distance.veryClose;
    private Action previousAction = Action.back;

    private OperationMode operationMode = OperationMode.scan;

    static int trainNumRounds = 0;
    static int trainInterval = 2;
    static int testNumRounds = 0;
    static int testInterval = 2;
    boolean flag = true;
    static int totalNumRounds = 0;
    static int numRoundsTo50 = 0;
    static int numWins = 0;
    static double winningRate = 0.0;

    private double gamma = 0.75;
    private double alpha = 0.5;
    private double epsilon;

    private double bestQ = 0.0;
    private double currentQ = 0.0;
    private double previousQ = 0.0;

    private final double instantPenalty = -0.25;
    private final double terminalPenalty = -0.5;
    private final double instantReward = 1.0;
    private final double terminalReward = 2.0;
    private double reward = 0.0;

    private double xCenter;
    private double yCenter;

    private double my_location_X = 0.0;
    private double my_location_Y = 0.0;
    private double my_energy = 0.0;

    public double enemy_Bearing = 0.0;
    public double enemy_Distance = 0.0;
    public double enemy_energy = 0.0;

    public boolean onPolicy = false;

    public boolean terminalRewardOnly = false;

    static LogFile logFile = null;

    public static StateActionTable LUT = new StateActionTable(
            Energy.values().length,
            Energy.values().length,
            Distance.values().length,
            Distance.values().length,
            Action.values().length);

    public void run(){
        setBulletColor(Color.BLACK);
        setGunColor(Color.GRAY);
        setBodyColor(Color.BLUE);
        setRadarColor(Color.CYAN);

        xCenter = getBattleFieldWidth() / 2;
        yCenter = getBattleFieldHeight() / 2;

        if (logFile == null) {
            logFile = new LogFile(getDataFile("log.dat"));
            logFile.stream.printf("gamma,   %2.2f\n", gamma);
            logFile.stream.printf("alpha,   %2.2f\n", alpha);
            logFile.stream.printf("epsilon, %2.2f\n", epsilon);
            logFile.stream.printf("badInstantReward, %2.2f\n", instantPenalty);
            logFile.stream.printf("badTerminalReward, %2.2f\n", terminalPenalty);
            logFile.stream.printf("goodInstantReward, %2.2f\n", instantReward);
            logFile.stream.printf("goodTerminalReward, %2.2f\n\n", terminalReward);
        }

        while(true){
            epsilon = flag ? 1.0 : 0.0;

            switch (operationMode){
                case scan: {
                    reward = 0.0;
                    turnRadarRight(90);
                    break;
                }
                case performAction: {
                    if (Math.random() <= epsilon)
                        currentAction = selectRandomAction();
                    else currentAction = selectBestAction(
                            getEnergy(my_energy).ordinal(),
                            getEnergy(enemy_energy).ordinal(),
                            getDistance(enemy_Distance).ordinal(),
                            getDistance(getDistanceToCenter(my_location_X, my_location_Y, xCenter, yCenter)).ordinal()
                    );

                    switch (currentAction){
                        case ahead:{
                            setAhead(100);
                            execute();
                            break;
                        }
                        case back:{
                            setBack(100);
                            execute();
                            break;
                        }
                        case aheadLeft:{
                            setTurnLeft(20);
                            setAhead(100);
                            execute();
                            break;
                        }
                        case aheadRight:{
                            setTurnRight(20);
                            setAhead(100);
                            execute();
                            break;
                        }
                        case backLeft:{
                            setTurnLeft(20);
                            setBack(100);
                            execute();
                            break;
                        }
                        case backRight:{
                            setTurnRight(20);
                            setBack(100);
                            execute();
                            break;
                        }
                        case turnGunFire:{
                            turnGunRight(getHeading() - getGunHeading() + enemy_Bearing);
                            fire(3);
                            break;
                        }
                    }
                    double[] x = new double[] {
                            previousMyEnergy.ordinal(),
                            previousEnemyEnergy.ordinal(),
                            previousDistanceToEnemy.ordinal(),
                            previousDistanceToCenter.ordinal(),
                            previousAction.ordinal()
                    };
                    if (flag){
                        LUT.setQValue(x, computeQ(reward, onPolicy));
                        operationMode = OperationMode.scan;
                    }

                }
            }
        }
    }

    @Override
    public void onScannedRobot (ScannedRobotEvent e){
        my_location_X = getX();
        my_location_Y = getY();
        my_energy = getEnergy();
        enemy_Bearing = e.getBearing();
        enemy_Distance = e.getDistance();
        enemy_energy = e.getEnergy();

        previousMyEnergy = currentMyEnergy;
        previousEnemyEnergy = currentEnemyEnergy;
        previousDistanceToEnemy = currentDistanceToEnemy;
        previousDistanceToCenter = currentDistanceToCenter;
        previousAction = currentAction;

        currentMyEnergy = getEnergy(my_energy);
        currentEnemyEnergy = getEnergy(enemy_energy);
        currentDistanceToEnemy = getDistance(enemy_Distance);
        currentDistanceToCenter = getDistance(getDistanceToCenter(my_location_X, my_location_Y, xCenter, yCenter));

        operationMode = OperationMode.performAction;
    }

    public Action selectRandomAction(){
        Random random = new Random();
        return Action.values()[random.nextInt(Action.values().length)];
    }

    public Action selectBestAction(double myEnergy, double enemyEnergy, double enemyDistance, double centerDistance){
        double maxReward = -Double.MAX_VALUE;
        Action bestAction = null;
        int e1 = getEnergy(myEnergy).ordinal();
        int d1 = getEnergy(enemyEnergy).ordinal();
        int e2 = getDistance(enemyDistance).ordinal();
        int d2 = getEnergy(centerDistance).ordinal();

        for (int i = 0; i < Action.values().length; i++){
            double[] x = new double[] {e1, d1, e2, d2, i};
            if (LUT.getQValue(x) > maxReward)
                bestAction = Action.values()[i];
        }
        return bestAction;
    }

    public double getDistanceToCenter (double my_location_X, double my_location_Y, double xCenter, double yCenter){
        return Math.sqrt(Math.pow((my_location_X - xCenter), 2) + Math.pow((my_location_Y - yCenter), 2));
    }

    public Energy getEnergy (double energy){
        Energy enumEnergy = null;
        if (energy == 0) enumEnergy = Energy.zero;
        else if (energy <= 25.0) enumEnergy = Energy.low;
        else if (25.0 < energy && energy < 50.0) enumEnergy = Energy.medium;
        else enumEnergy = Energy.high;
        return enumEnergy;
    }

    public Distance getDistance (double distance) {
        Distance enumDistance = null;
        if (distance <= 50.0) enumDistance = Distance.veryClose;
        else if (50.0 < distance && distance <= 500.0) enumDistance = Distance.near;
        else enumDistance = Distance.far;
        return enumDistance;
    }

    public double computeQ(double reward, boolean onPolicy){
        Action bestAction = selectBestAction(currentMyEnergy.ordinal(), currentEnemyEnergy.ordinal(), currentDistanceToEnemy.ordinal(), currentDistanceToCenter.ordinal());
        double[] previousStateAction = new double[] {
                previousMyEnergy.ordinal(),
                previousEnemyEnergy.ordinal(),
                previousDistanceToEnemy.ordinal(),
                previousDistanceToCenter.ordinal(),
                previousAction.ordinal()
        };
        double[] currentStateAction = new double[]{
                currentMyEnergy.ordinal(),
                currentEnemyEnergy.ordinal(),
                currentDistanceToEnemy.ordinal(),
                currentDistanceToCenter.ordinal(),
                currentAction.ordinal()
        };
        double [] bestStateAction = new double[]{
                currentMyEnergy.ordinal(),
                currentEnemyEnergy.ordinal(),
                currentDistanceToEnemy.ordinal(),
                currentDistanceToCenter.ordinal(),
                bestAction.ordinal()
        };

        previousQ = LUT.getQValue(previousStateAction);
        currentQ = LUT.getQValue(currentStateAction);
        bestQ = LUT.getQValue(bestStateAction);

        return (onPolicy) ? previousQ + alpha * (reward + gamma * currentQ - previousQ)
                : previousQ + alpha * (reward + gamma * bestQ - previousQ);
    }

    @Override
    public void onBulletHit (BulletHitEvent e){
        if (terminalRewardOnly == false)
            reward = instantReward;
    }

    @Override
    public void onHitByBullet (HitByBulletEvent e){
        if (terminalRewardOnly == false)
            reward = instantPenalty;
    }

    @Override
    public void onBulletMissed (BulletMissedEvent e){
        if (terminalRewardOnly == false)
            reward = instantPenalty;
    }

    @Override
    public void onHitRobot (HitRobotEvent e){
        if (terminalRewardOnly == false)
            reward = instantPenalty;
    }

    @Override
    public void onHitWall (HitWallEvent e){
        if (terminalRewardOnly == false)
            reward = instantPenalty;
    }

    @Override
    public void onWin(WinEvent e){
        System.out.println(flag);
        if (flag){
            reward = terminalReward;
            double[] x = new double[] {
                    previousMyEnergy.ordinal(),
                    previousEnemyEnergy.ordinal(),
                    previousDistanceToEnemy.ordinal(),
                    previousDistanceToCenter.ordinal(),
                    previousAction.ordinal()
            };
            LUT.setQValue(x, computeQ(reward, onPolicy));
            updateTable();
            trainNumRounds ++;
            System.out.println("training" + trainNumRounds);
            if (trainNumRounds == trainInterval){
                trainNumRounds = 0;
                flag = false;
            }
        }

        else{
            testNumRounds++;
            System.out.println("testing" + testNumRounds);
            if(testNumRounds == testInterval) {
                testNumRounds = 0;
                flag = true;
            }
            if (numRoundsTo50 < 50){
                numRoundsTo50 += 1;
                totalNumRounds += 1;
                numWins += 1;
            }
            else {
                winningRate = 100.0 * numWins / numRoundsTo50;
                logFile.stream.printf("Winning rate: %2.1f\n ", winningRate);
                logFile.stream.flush();
                numRoundsTo50 = 0;
                numWins = 0;
            }
        }
    }

    @Override
    public void onDeath(DeathEvent e){
        System.out.println(flag);
        if (flag) {
            reward = terminalPenalty;
            double[] x = new double[]{
                    previousMyEnergy.ordinal(),
                    previousEnemyEnergy.ordinal(),
                    previousDistanceToEnemy.ordinal(),
                    previousDistanceToCenter.ordinal(),
                    previousAction.ordinal()
            };
            LUT.setQValue(x, computeQ(reward, onPolicy));
            updateTable();

            trainNumRounds ++;
            System.out.println("training" + trainNumRounds);
            if (trainNumRounds == trainInterval){
                trainNumRounds = 0;
                flag = false;
            }
            else{
                
            }
        }

        else{
            testNumRounds++;
            System.out.println("testing" + testNumRounds);
            if (numRoundsTo50 < 50){
                numRoundsTo50 += 1;
                totalNumRounds += 1;
            }
            else{
                winningRate = 100.0 * numWins / numRoundsTo50;
                logFile.stream.printf("Winning rate: %2.1f\n ", winningRate);
                logFile.stream.flush();
                numRoundsTo50 = 0;
                numWins = 0;
            }
            if(testNumRounds == testInterval){
                testNumRounds = 0;
                flag = true;
            }
            else{

            }
        }
    }

    public void updateTable() {
        try{
            LUT.save(getDataFile("LUT.dat"));
        }
        catch (Exception e){
            System.out.println("Load Table Failed!");
            System.out.println("Error is " + e);
        }
    }

    public void loadTable() {
        try{
            LUT.load("LUT.dat");
        } catch (IOException e) {
            System.out.println("Load Table Failed!");
            System.out.println("Error is " + e);
        }
    }

}