package main.java.chaincode;

import main.java.*;
import main.java.client.CAClient;
import main.java.client.ChannelClient;
import main.java.client.FabricClient;
import main.java.config.Config;
import main.java.user.UserContext;
import main.java.util.Util;
import org.hyperledger.fabric.sdk.*;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

public class QueryBalance implements Runnable {

    private final UserContext userContext;
    private QueryTransaction queryTransaction;
    private long submissionTime;
    private long responseTime;
    int round;
    int smartMeterID;
    ArrayList<String> urls;
    long sendingTime;
    private static String transactionID;

    public QueryBalance(long sendingTime, ArrayList<String> urls, int round, int smartMeterID, UserContext userContext){
        this.smartMeterID=smartMeterID;
        this.round=round;
        this.urls=urls;
        this.sendingTime=sendingTime;
        this.userContext=userContext;
    }

    @Override
    public void run() {
        Random rand = new Random();
        int peerNode = rand.nextInt(MainJavaClass.peers.size()-1); //The first two nodes are allocated for DSO and Monitor
        int ordererNode = rand.nextInt(MainJavaClass.orderers.size()-1); //The first two nodes are allocated for DSO and Monitor
        Channel channel=null;
     try {
        FabricClient fabClient = new FabricClient(userContext);

        ChannelClient channelClient = fabClient.createChannelClient(Config.CHANNEL_NAME);
        channel = channelClient.getChannel();
        Peer peer = fabClient.getInstance().newPeer(MainJavaClass.peers.get(peerNode).getName(), MainJavaClass.peers.get(peerNode).getUrl());
       // EventHub eventHub = fabClient.getInstance().newEventHub("eventhub01", "grpc://localhost:7053");
        Orderer orderer = fabClient.getInstance().newOrderer(MainJavaClass.orderers.get(ordererNode).getName(), MainJavaClass.orderers.get(ordererNode).getUrl());
        channel.addPeer(peer);
       // channel.addEventHub(eventHub);
        channel.addOrderer(orderer);
        channel.initialize();
        if(sendingTime-System.currentTimeMillis() >100)
            Thread.sleep(sendingTime-System.currentTimeMillis()); //  For now just send immediately. Correct it later
         submissionTime=System.currentTimeMillis();
         System.out.println(" Transaction Submission Time: "+submissionTime);

         String[] args = {""};
         Logger.getLogger(QueryBalance.class.getName()).log(Level.INFO, "Getting Balance for Round " + round);

         Collection<ProposalResponse>  responsesQuery = channelClient.queryByChainCode(Config.CHAINCODE_1_NAME, "getBalance", args);
         responseTime=System.currentTimeMillis();
         for (ProposalResponse pres : responsesQuery) {
             String stringResponse = new String(pres.getChaincodeActionResponsePayload());
             transactionID= pres.getTransactionID(); // This is my code
             Logger.getLogger(QueryBalance.class.getName()).log(Level.INFO, stringResponse);
         }

         queryTransaction =new QueryTransaction(transactionID, Constants.TransactionType.PS_getBalance,
                 Constants.TransactionStatus.SUCCESS,round, submissionTime,responseTime);
    }
     catch (Exception e) {
        responseTime=System.currentTimeMillis();
        System.err.println("Round: "+round+" Smart Meter: "+smartMeterID+" Transaction Failed, Duration: "+ (responseTime-submissionTime));
         queryTransaction =new QueryTransaction(null,Constants.TransactionType.PS_getBalance,
                 Constants.TransactionStatus.FAILED,round,submissionTime,responseTime);
        e.printStackTrace();
    }
        channel.shutdown(true);
        Report.pushQueryTransactionReport(queryTransaction);
    }
}
