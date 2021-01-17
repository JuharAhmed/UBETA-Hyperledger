package main.java;
import java.math.BigInteger;

public class Transaction {
    String ID;
    int round;
    Constants.TransactionType Type;
    double submissionTime;
    double resPonseTime;
    double confimationTime;
    Constants.TransactionStatus Status;
    BigInteger blockNumber;
    double gasUSed;

    public Transaction(String ID, Constants.TransactionType type, int round, Constants.TransactionStatus status, long submissionTime, long resPonseTime){
        this.ID=ID;
        this.Type=type;
        this.Status=status;
        this.submissionTime=submissionTime;
        this.resPonseTime=resPonseTime;
        this.blockNumber=blockNumber;
        this.round=round;
    }

    public double getComittedTime() {
        return confimationTime;
    }

    public void setComittedTime(long comittedTime) {
        this.confimationTime = comittedTime;
    }
}
