package main.java;

public class SummaryResult {
    Constants.TransactionType transactionType;
    double overallThroughPut = 0; //Overall Average Throughput
    double[] overallLatency = new double[3]; //Overall minimum, maximum and average latencies
    double sendingRate; //The rate at which transactions were sent ((No of Successful Transactions + No of Failed Transactions) -(Last Submission Time - First Submission Time));
    int numberOfSuccessfulTransactions;
    int numberOfFailedTransactions;
    double overAllAverageGasUsed;

    public SummaryResult(Constants.TransactionType transactionType){
        this.transactionType=transactionType;
    }

}
