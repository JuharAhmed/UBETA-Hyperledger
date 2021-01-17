package main.java.chaincode;
import main.java.*;
import main.java.client.ChannelClient;
import main.java.client.FabricClient;
import main.java.config.Config;
import main.java.user.UserContext;
import org.hyperledger.fabric.sdk.*;

import java.util.*;

import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BidSender implements Runnable {
    private static final byte[] EXPECTED_EVENT_DATA = "!".getBytes(UTF_8);
    private static final String EXPECTED_EVENT_NAME = "event";
    private final UserContext userContext;
    private Transaction transaction;
    private long submissionTime;
    private long responseTime;
   // private PoolMarket poolMarketContract;
    String energyAmount;
    String priceRate;
    int round;
    int consumerID;
    private static String transactionID;
    ArrayList<String> urls;
    long sendingTime;

    public BidSender(long sendingTime, ArrayList<String> urls, int round, int consumerID, UserContext userContext, String energyAmount, String priceRate){
    this.consumerID=consumerID;
    this.energyAmount=energyAmount;
    this.priceRate=priceRate;
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
/*            Util.cleanUp();
            String caUrl = Config.CA_ORG1_URL;
            CAClient caClient = new CAClient(caUrl, null);
            // Enroll Admin to Org1MSP
            UserContext adminUserContext = new UserContext();
            adminUserContext.setName(Config.ADMIN);
            adminUserContext.setAffiliation(Config.ORG1);
            adminUserContext.setMspId(Config.ORG1_MSP);
            caClient.setAdminUserContext(adminUserContext);
            adminUserContext = caClient.enrollAdminUser(Config.ADMIN, Config.ADMIN_PASSWORD);*/

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

            TransactionProposalRequest request = fabClient.getInstance().newTransactionProposalRequest();
            ChaincodeID ccid = ChaincodeID.newBuilder().setName(Config.CHAINCODE_1_NAME).build();
            request.setChaincodeID(ccid);
            request.setFcn("submitEnergyBid");
           // String carID= "CAR"+String.valueOf(consumerID);
            String[] arguments = { energyAmount, priceRate };
            request.setArgs(arguments);
            request.setProposalWaitTime(600000);

            Map<String, byte[]> tm2 = new HashMap<>();
            tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
            tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
            tm2.put("result", ":)".getBytes(UTF_8));
            tm2.put(EXPECTED_EVENT_NAME, EXPECTED_EVENT_DATA);
            request.setTransientMap(tm2);
            if(sendingTime-System.currentTimeMillis() >100)
                Thread.sleep(sendingTime-System.currentTimeMillis()); //  For now just send immediately. Correct it later
            submissionTime=System.currentTimeMillis();
           // System.out.println(" Transaction Submission Time: "+submissionTime);
            Collection<ProposalResponse> responses = channelClient.sendTransactionProposal(request);
            responseTime=System.currentTimeMillis();

            for (ProposalResponse res: responses) {
                ChaincodeResponse.Status status = res.getStatus();
               // System.err.println(" Proposal Response Timestamp : "+res.getProposalResponse().getTimestamp().getSeconds());
                transactionID= res.getTransactionID(); // This is my code
                Logger.getLogger(BidSender.class.getName()).log(Level.INFO,"Energy Bid Submitted "+Config.CHAINCODE_1_NAME + ". Status - " + status);
            }

             transaction =new Transaction(transactionID,Constants.TransactionType.PM_Bidding,
                            round, Constants.TransactionStatus.SUCCESS,submissionTime,responseTime);

            }
        catch (Exception e) {
            responseTime=System.currentTimeMillis();
             System.err.println("Round: "+round+" Consumer: "+consumerID+" Transaction Failed, Duration: "+ (responseTime-submissionTime));
            transaction =new Transaction(null,Constants.TransactionType.PM_Bidding,
                    round, Constants.TransactionStatus.FAILED,submissionTime,responseTime);
            System.out.println("Energy Bid: Amount: "+energyAmount+" Price: "+priceRate);
            e.printStackTrace();
        }
        channel.shutdown(true);
        Report.pushTransactionReport(transaction);
        
    }


}
