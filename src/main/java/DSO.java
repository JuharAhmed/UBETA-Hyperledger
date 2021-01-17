package main.java;

import main.java.Constants;
import main.java.Task;
import main.java.chaincode.*;
import main.java.client.ChannelClient;
import main.java.client.FabricClient;
import main.java.config.Config;
import main.java.user.UserContext;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.hyperledger.fabric.sdk.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ExecutionException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static main.java.Constants.TransactionType.PM_MarketClearance;

public class DSO implements Runnable {
    private static final byte[] EXPECTED_EVENT_DATA = "!".getBytes(UTF_8);
    private static final String EXPECTED_EVENT_NAME = "event";
    private Transaction transaction;
    private long submissionTime;
    private long responseTime;
    // private PoolMarket poolMarketContract;
    BigInteger energyAmount;
    BigInteger priceRate;
    int round;
    int consumerID;
    private static String transactionID;
    ArrayList<String> urls;
    long sendingTime;
    UserContext userContext;
    private ArrayList<Task> DSORoundTasks;
    TestSchedule testSchedule;

    DSO(TestSchedule testSchedule, UserContext adminUserContext) {
        this.testSchedule = testSchedule;
        this.userContext = adminUserContext;
    }

    @Override
    public void run() {
        long numberOfRounds = testSchedule.numberOfRounds;
        int round = 0;
        long waitingTime = 0;
        Task task;

        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(2);
        while (round < numberOfRounds) {
            DSORoundTasks = getDSORoundTasks(round);
            System.out.println("DSO: Starting round: " + round);

            for (int ts = 0; ts < DSORoundTasks.size(); ts++) {
                task = DSORoundTasks.get(ts);
                System.out.println("DSO: Round " + round + " task " + task.taskType);
                waitingTime = task.startTime - System.currentTimeMillis();
                System.out.println("DSO: Waiting for " + waitingTime / 1000 + " Seconds Until round " + round + " " + task.taskType + " Time is Reached ");
                try {
                    if (waitingTime > 1000)
                        Thread.sleep(waitingTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                executor.schedule(new DSOWorkerThread(task, round, userContext), 0, TimeUnit.MILLISECONDS);
                try {
                    Thread.sleep(MainJavaClass.shortIntervalDelay/2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!taskSuccessful(task)) {
                    executor.schedule(new DSOWorkerThread(task, round, userContext), 0, TimeUnit.MILLISECONDS);
                } else {
                    continue;
                }

                try {
                    Thread.sleep(MainJavaClass.shortIntervalDelay/2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!taskSuccessful(task)) {
                    executor.schedule(new DSOWorkerThread(task, round, userContext), 0, TimeUnit.MILLISECONDS);
                } else {
                    continue;
                }
            }

            System.out.println("DSO: Finishing round: " + round);
            //Current Round Finished increment round
            round++;

        }

        // Waiting until worker Threads finish
        waitingTime = testSchedule.endTime - System.currentTimeMillis();
        try {
            if (waitingTime > 100)
                Thread.sleep(waitingTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private boolean taskSuccessful(Task task) {
        String stringResponse=null;
        Channel channel = null;
        try {
        Random rand = new Random();
        int peerNode = rand.nextInt(MainJavaClass.peers.size() - 1); //The first two nodes are allocated for DSO and Monitor
        int ordererNode = rand.nextInt(MainJavaClass.orderers.size() - 1); //The first two nodes are allocated for DSO and Monitor

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

        String[] args = {""};
        Collection<ProposalResponse> responsesQuery = channelClient.queryByChainCode(Config.CHAINCODE_1_NAME, "getState", args);
        for (ProposalResponse pres : responsesQuery) {
            stringResponse = new String(pres.getChaincodeActionResponsePayload());
            System.out.println(" Current State "+stringResponse);
        }
    }
    catch (Exception e){
        e.printStackTrace();
    }
        boolean taskSuccessful = false;
        switch (task.taskType) {
            case BiM_Initialization:
                break;
            case BiM_Resetting:
                break;
/*            case PM_Initialization:
                if (stringResponse.equals(String.valueOf(1)))
                        taskSuccessful = true;
                break;*/
            case PM_MarketClearance:
                if (stringResponse.equals(String.valueOf(2)))
                    taskSuccessful = true;
                break;
            case BaM_CalculateMismatch:
                break;
            case BaM_MarketClearance:
                break;
            case BaM_MarketResetting:
                break;
            case PS_SettlingPayments:
                if (stringResponse.equals(String.valueOf(3)))
                    taskSuccessful = true;
                break;
            case PM_MarketResetting:
                if (stringResponse.equals(String.valueOf(1)))
                    taskSuccessful = true;
                break;
            default:
                System.err.println("DSO: Task Type not recognized");
                break;
        }
        channel.shutdown(true);
        return taskSuccessful;

    }

    private ArrayList<Task> getDSORoundTasks(int round) {
        ArrayList<Task> roundTasks;
        ArrayList<Task> DSORoundTasks=new ArrayList<>();
        roundTasks=testSchedule.testRounds.get(round).tasks;
        for(Task ts:roundTasks){
            if(ts.taskType==Constants.TransactionType.PM_Initialization || ts.taskType== PM_MarketClearance || ts.taskType==Constants.TransactionType.PM_MarketResetting || ts.taskType==Constants.TransactionType.PS_SettlingPayments || ts.taskType==Constants.TransactionType.PS_Resetting){
                DSORoundTasks.add(ts);
                System.out.println("DSO Round : "+round+" Tasks " + ts.taskType);
            }
        }
        return DSORoundTasks;
    }

    protected static class DSOWorkerThread implements Runnable {
        Task task;
        int round;
        long submissionTime;
        long responseTime;
        Transaction transaction;
        UserContext userContext;
        FabricClient fabClient;
        ChannelClient channelClient;
        Channel channel;

        public DSOWorkerThread(Task task, int round, UserContext userContext) {
            this.userContext = userContext;
            this.task = task;
            this.round = round;

        }

        @Override
        public void run() {
            Random rand = new Random();
          int peerNode = rand.nextInt(MainJavaClass.peers.size() - 1); //The first two nodes are allocated for DSO and Monitor
          int ordererNode = rand.nextInt(MainJavaClass.orderers.size() - 1); //The first two nodes are allocated for DSO and Monitor

            try {
                fabClient = new FabricClient(userContext);

                channelClient = fabClient.createChannelClient(Config.CHANNEL_NAME);
                channel = channelClient.getChannel();
                Peer peer = fabClient.getInstance().newPeer(MainJavaClass.peers.get(peerNode).getName(), MainJavaClass.peers.get(peerNode).getUrl());
                // EventHub eventHub = fabClient.getInstance().newEventHub("eventhub01", "grpc://localhost:7053");
                Orderer orderer = fabClient.getInstance().newOrderer(MainJavaClass.orderers.get(ordererNode).getName(), MainJavaClass.orderers.get(ordererNode).getUrl());
                channel.addPeer(peer);
                // channel.addEventHub(eventHub);
                channel.addOrderer(orderer);
                channel.initialize();
            } catch (Exception e) {
                e.printStackTrace();
            }
            switch (task.taskType) {
                case PM_Initialization:
                    initializeBidding(round);
                    break;
                case PM_MarketClearance:
                    clearMarket(round);
                    break;
                case PM_MarketResetting:
                    resetMarket(round);
                    break;
                case BaM_CalculateMismatch:
                    break;
                case BaM_MarketClearance:
                    break;
                case BaM_MarketResetting:
                    break;
                case PS_SettlingPayments:
                    settlePayments(round);
                    break;
                default:
                    System.err.println("DSO: Task Type not recognized");
                    break;
            }

            channel.shutdown(true);

        }

        private void initializeBidding(int round) {
            System.out.println("DSO: Initializing Bidding for round " + round); //Just change this to changing status

            try {
                TransactionProposalRequest request = fabClient.getInstance().newTransactionProposalRequest();
                ChaincodeID ccid = ChaincodeID.newBuilder().setName(Config.CHAINCODE_1_NAME).build();
                request.setChaincodeID(ccid);
                request.setFcn("initializeBidding");
                String[] arguments = {""};
                request.setArgs(arguments);
                request.setProposalWaitTime(600000);

                Map<String, byte[]> tm2 = new HashMap<>();
                tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
                tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
                tm2.put("result", ":)".getBytes(UTF_8));
                tm2.put(EXPECTED_EVENT_NAME, EXPECTED_EVENT_DATA);
                request.setTransientMap(tm2);
                // System.out.println(" Transaction Submission Time: "+submissionTime);
                Collection<ProposalResponse> responses = channelClient.sendTransactionProposal(request);
                responseTime = System.currentTimeMillis();

                for (ProposalResponse res : responses) {
                    ChaincodeResponse.Status status = res.getStatus();
                    Logger.getLogger(DSO.class.getName()).log(Level.INFO, "Bidding Initialization for Round " + round + "" + status);
                }
            } catch (Exception e) {
                System.err.println("Round: " + round + " Bidding Initialization Failed");
                e.printStackTrace();
            }

        }

        private void clearMarket(int round) {
            System.out.println("DSO: Starting Market Clearance for round " + round);
            try {
                TransactionProposalRequest request = fabClient.getInstance().newTransactionProposalRequest();
                ChaincodeID ccid = ChaincodeID.newBuilder().setName(Config.CHAINCODE_1_NAME).build();
                request.setChaincodeID(ccid);
                request.setFcn("calculateMarkerClearingPrice");
                String[] arguments = {""};
                request.setArgs(arguments);
                request.setProposalWaitTime(600000);

                Map<String, byte[]> tm2 = new HashMap<>();
                tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
                tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
                tm2.put("result", ":)".getBytes(UTF_8));
                tm2.put(EXPECTED_EVENT_NAME, EXPECTED_EVENT_DATA);
                request.setTransientMap(tm2);
                submissionTime = System.currentTimeMillis();
                // System.out.println(" Transaction Submission Time: "+submissionTime);
                Collection<ProposalResponse> responses = channelClient.sendTransactionProposal(request);
                responseTime = System.currentTimeMillis();

                for (ProposalResponse res : responses) {
                    ChaincodeResponse.Status status = res.getStatus();
                    // System.err.println(" Proposal Response Timestamp : "+res.getProposalResponse().getTimestamp().getSeconds());
                    transactionID = res.getTransactionID();
                    Logger.getLogger(DSO.class.getName()).log(Level.INFO, "Market Clearance for round " + round + ". Status - " + status);
                }
                transaction = new Transaction(transactionID, PM_MarketClearance,
                        round, Constants.TransactionStatus.SUCCESS, submissionTime, responseTime);

            } catch (Exception e) {
                responseTime = System.currentTimeMillis();
                System.err.println("Round: " + round + " Market Clearance Duration: " + (responseTime - submissionTime));
                transaction = new Transaction(null, PM_MarketClearance,
                        round, Constants.TransactionStatus.FAILED, submissionTime, responseTime);
                e.printStackTrace();
            }
            Report.pushTransactionReport(transaction);

        }

        private void settlePayments(int round) {
            System.out.println("DSO: Starting Payment Settlement for round " + round);

            try {
                TransactionProposalRequest request = fabClient.getInstance().newTransactionProposalRequest();
                ChaincodeID ccid = ChaincodeID.newBuilder().setName(Config.CHAINCODE_1_NAME).build();
                request.setChaincodeID(ccid);
                request.setFcn("settlePayments");
                String[] arguments = {""};
                request.setArgs(arguments);
                request.setProposalWaitTime(600000);

                Map<String, byte[]> tm2 = new HashMap<>();
                tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
                tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
                tm2.put("result", ":)".getBytes(UTF_8));
                tm2.put(EXPECTED_EVENT_NAME, EXPECTED_EVENT_DATA);
                request.setTransientMap(tm2);
                submissionTime = System.currentTimeMillis();
                // System.out.println(" Transaction Submission Time: "+submissionTime);
                Collection<ProposalResponse> responses = channelClient.sendTransactionProposal(request);
                responseTime = System.currentTimeMillis();

                for (ProposalResponse res : responses) {
                    ChaincodeResponse.Status status = res.getStatus();
                    // System.err.println(" Proposal Response Timestamp : "+res.getProposalResponse().getTimestamp().getSeconds());
                    transactionID = res.getTransactionID();
                    Logger.getLogger(DSO.class.getName()).log(Level.INFO, "Market Clearance for round " + round + ". Status - " + status);
                }
                transaction = new Transaction(transactionID, Constants.TransactionType.PS_SettlingPayments,
                        round, Constants.TransactionStatus.SUCCESS, submissionTime, responseTime);

            } catch (Exception e) {
                responseTime = System.currentTimeMillis();
                System.err.println("Round: " + round + " Market Clearance Duration: " + (responseTime - submissionTime));
                transaction = new Transaction(null, Constants.TransactionType.PS_SettlingPayments,
                        round, Constants.TransactionStatus.FAILED, submissionTime, responseTime);
                e.printStackTrace();
            }
            Report.pushTransactionReport(transaction);

        }

        private void resetMarket(int round) {
            System.out.println("DSO: Starting Market Resetting for round " + round);

            try {
                TransactionProposalRequest request = fabClient.getInstance().newTransactionProposalRequest();
                ChaincodeID ccid = ChaincodeID.newBuilder().setName(Config.CHAINCODE_1_NAME).build();
                request.setChaincodeID(ccid);
                request.setFcn("Market Resetting");
                String[] arguments = {""};
                request.setArgs(arguments);
                request.setProposalWaitTime(600000);

                Map<String, byte[]> tm2 = new HashMap<>();
                tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
                tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
                tm2.put("result", ":)".getBytes(UTF_8));
                tm2.put(EXPECTED_EVENT_NAME, EXPECTED_EVENT_DATA);
                request.setTransientMap(tm2);
                submissionTime = System.currentTimeMillis();
                // System.out.println(" Transaction Submission Time: "+submissionTime);
                Collection<ProposalResponse> responses = channelClient.sendTransactionProposal(request);
                responseTime = System.currentTimeMillis();

                for (ProposalResponse res : responses) {
                    ChaincodeResponse.Status status = res.getStatus();
                    // System.err.println(" Proposal Response Timestamp : "+res.getProposalResponse().getTimestamp().getSeconds());
                    transactionID = res.getTransactionID();
                    Logger.getLogger(DSO.class.getName()).log(Level.INFO, "Market Resetting for round " + round + ". Status - " + status);
                }
                transaction = new Transaction(transactionID, Constants.TransactionType.PS_SettlingPayments,
                        round, Constants.TransactionStatus.SUCCESS, submissionTime, responseTime);

            } catch (Exception e) {
                responseTime = System.currentTimeMillis();
                System.err.println("Round: " + round + " Market Market Resetting Duration: " + (responseTime - submissionTime));
                transaction = new Transaction(null, Constants.TransactionType.PS_SettlingPayments,
                        round, Constants.TransactionStatus.FAILED, submissionTime, responseTime);
                e.printStackTrace();
            }

        }

    }
}