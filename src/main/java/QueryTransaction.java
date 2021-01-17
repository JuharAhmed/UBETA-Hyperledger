package main.java;

public class QueryTransaction {

    public final Constants.TransactionType Type;
    public final int round;
    public final long submissionTime;
    public final Constants.TransactionStatus Status;
    public final long responseTime;
    public String transactionID;

    public QueryTransaction(String transactionID, Constants.TransactionType type, Constants.TransactionStatus status, int round, long submissionTime, long responseTime) {
        this.transactionID=transactionID;
        this.Type=type;
        this.Status=status;
        this.round=round;
        this.submissionTime=submissionTime;
        this.responseTime=responseTime;
    }
}
