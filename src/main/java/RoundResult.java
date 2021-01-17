package main.java;

public class RoundResult {
    int round;
    Constants.TransactionType transactionType;
    double [] roundLatency = new double[3];//This will hold  the minimum, maximum and average latency
    double roundThroughput =0.0; //This will hold the throughput for the round (Nnumber of successful transactions/ (Last Confirmatin Time -First Submission Time))
    double sendingRate; //The rate at which transactions were sent ((No of Successful Transactions + No of Failed Transactions) -(Last Submission Time - First Submission Time));
    int numberOfSuccessfulTransactions;
    int numberOfFailedTransactions;
    double lastConfirmationTimeMinusFirstSubmissionTime;
    double lastSubmissionTimeMinusFirstSubmissionTime;
    double averageGasUsed;
    public RoundResult(int round, Constants.TransactionType transactionType){
        this.round=round;
        this.transactionType=transactionType;
        roundLatency[0]=1000000000; // Setting the minimum latency to 1000000000
        roundLatency[1]=0; // Setting the maximum latency to 0
    }

}
