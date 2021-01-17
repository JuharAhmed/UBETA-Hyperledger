package main.java;

import java.util.ArrayList;
// A test schedule consist of multiple rounds
public class TestSchedule {

    int numberOfRounds;
    long startTime;
    long endTime;
    ArrayList<TestRound> testRounds;

    public TestSchedule(long startTime, long endTime, int numberOfRounds, ArrayList<TestRound> testRounds){
    this.startTime=startTime;
    this.endTime=endTime;
    this.numberOfRounds=numberOfRounds;
    this.testRounds=testRounds;
    }

}
