//Task is a certain activity inside a round such as bidding in pool market, sending multi signature transaction...
//A round consists of multiple tasks
package main.java;

public class Task {
    Constants.TransactionType taskType;
    long duration;
    String contractID;
    String methodToInvoke;
    String powerData;
    long startTime;
    long endTime;
    long totalNumberOfTransactionsPerRound;
    long systemWideTransactionRate; //Transactions per seconds
    long numberOfTransactionsPerClient; // This is equal to 2 as we only have one consumer and producer client. But each of them have several worker threads
    long transactionRatePerClient; //Transactions per seconds

    public Task(Constants.TransactionType transactionType, long duration){ //Also set the contractID, methodToInvoke and data during Task object creation
        this.taskType=transactionType;
        this.duration=duration;
    }
}
