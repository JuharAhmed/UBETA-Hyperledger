package main.java;
import java.util.ArrayList;
//A test round consists of multiple tasks
public class TestRound {
    int ID;
    ArrayList<Task> tasks;
    long roundDuration;
    int numberOfTransactionsPerClient;
    int transactionRatePerClient;
    long roundStartTime;
    long roundEndTime;
    TestRound(int ID){
        this.ID=ID;
    }

}
