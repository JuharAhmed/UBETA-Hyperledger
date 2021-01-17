package main.java;
public class Constants {

    public enum Role {
        CONSUMER,
        PRODUCER;
    }

    public enum TransactionStatus {
        SUCCESS,
        FAILED;
    }

    public enum TransactionType {// 10 of the following tasks are performed by the DSO. only four of them are performed by consumers and producers
        //The tasks are arranged in time sequential order and executed one after the other.
        BiM_Initialization, // DSO Opening the BiM market for users to submit Multi Sig contract
        BiM_MultiSigContract, //Users submitting Multi Signature energy contract to the BiM
        BiM_Resetting,//DSO closes the BiM Market, sends the data to the BaM and also resets the previous data
        PM_Initialization, //DSO changes the status of pool market from Bidding closed to Bidding Open
        PM_Bidding, //Users send Bids and offers for the pool market
        PM_MarketClearance, //DSO performs Market Clearance. Also the status of the pool market is changed to Bidding Closed in this method
        PM_getDispachedEnergy,
        PM_MarketResetting,//DSO sends the data to the BaM and also resets the previous data
        BaM_CalculateMismatch,//DSO Calculating the net energy contract and mismatch in BaM. Then initiating BaM market by sending broadcast message to users if there is mismatch
        //Also the status of BaM market is changed to Bidding Open
        BaM_Offers, // Producers sending offers to the BaM market
        BaM_MarketClearance,// DSO performs market clearance for the BaM market. //Also the status of BaM market is changed to Bidding Closed
        BaM_MarketResetting, // DSO sends all data to payment settlement before deleting the data
        PS_ConsumptionProduction, //Consumers and producers Sending consumption and production data to PS
        PS_SettlingPayments, //DSO Calculates the Net Payment and updates accounts accordingly
        PS_getBalance,
        PS_Resetting; //DSO deletes the previous data ????? Do we really have to do this?
    }

    public enum RateType {
        FIXED,
        INCREMENTAL;
    }
    public enum ConsensusAlgorithm {
        Proof_Of_Work,
        Proof_Of_Authority_IBFT,
        Proof_Of_Authority_Clique;
    }
}
