package main.java;

public class RateController {
    long BiM_Multi_Sig_InitialTransactionRate;
    long PM_bidding_InitialTransactionRate;
    long BaM_Offer_InitialTransactionRate;
    Constants.RateType  rateType;
    long incrementBY;
    long initialNumbeOfTransactions;

    public RateController(Constants.RateType rateType, long incrementBY, long initialNumbeOfTransactions){
      this.rateType=rateType;
      this.initialNumbeOfTransactions=initialNumbeOfTransactions;
      this.incrementBY=incrementBY;
    }
    public long getRoundRate(int round, Constants.TransactionType taskType){
        long roundRate=0;
        if(round>0 && (this.rateType== Constants.RateType.INCREMENTAL)){
            if(taskType==Constants.TransactionType.BiM_MultiSigContract){
                roundRate=incrementBY*(round+1);
            }
            else if(taskType==Constants.TransactionType.PM_Bidding){
                roundRate=incrementBY*(round+1);
            }
            else if(taskType==Constants.TransactionType.BaM_Offers){
                roundRate=incrementBY*(round+1);

            }
            else{
                roundRate=1; // All others types of transactions are sent by DSO and they are just one transaction per round
            }
        }
        else {
        if(taskType==Constants.TransactionType.BiM_MultiSigContract){
            roundRate=BiM_Multi_Sig_InitialTransactionRate;
        }
        else if(taskType==Constants.TransactionType.PM_Bidding){
            roundRate=PM_bidding_InitialTransactionRate;
        }
        else if(taskType==Constants.TransactionType.BaM_Offers){
            roundRate=BaM_Offer_InitialTransactionRate;
        }
        else{
            roundRate=1; // All others types of transactions are sent by DSO and they are just one transaction per round
        }
    }
        return roundRate;
    }

    public long getNumberOfTransactionsForThisRound(int round, Constants.TransactionType taskType) {
        long numberOfTransactions=0;

        if(taskType==Constants.TransactionType.BiM_MultiSigContract || taskType==Constants.TransactionType.PM_Bidding || taskType==Constants.TransactionType.BaM_Offers
                ||taskType==Constants.TransactionType.PM_getDispachedEnergy || taskType==Constants.TransactionType.PS_getBalance){
            numberOfTransactions= initialNumbeOfTransactions + incrementBY*round;
        }
        else{
            numberOfTransactions=1; // All others types of transactions are sent by DSO and they are just one transaction per round
        }
        return numberOfTransactions;
    }

}
