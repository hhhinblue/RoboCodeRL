package myrobot;

import robocode.RobocodeFileOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class LogFile {
    PrintStream stream;
    public LogFile (File logFile){
        try {
            stream = new PrintStream(new RobocodeFileOutputStream(logFile));
            System.out.println("Log file created");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
