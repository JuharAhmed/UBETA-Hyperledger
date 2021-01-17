package main.java;

import main.java.client.CAClient;
import main.java.client.FabricClient;
import main.java.config.Config;
import main.java.network.CreateChannel;
import main.java.network.DeployInstantiateChaincode;
import main.java.user.RegisterEnrollUser;
import main.java.user.UserContext;
import main.java.util.Util;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class MainJavaClass {
    public static ArrayList<Thread> smartMeters = new ArrayList<>();
    public static ArrayList<String> ordererUrls = new ArrayList<>();
    public static ArrayList<String> peerUrls =new ArrayList<>();
    static int numberOfUserAccounts = 10100; //The number of accounts should not be less than the number of transaction in each round as each transaction uses one account.
    public static int numberOfOrderers = 10; //This is used for monitoring performance
    public static int numberOfPeers =10;
    public static int startUpDelay = 180000; //This is in milliseconds
    static int finishingDelay = 600000; //This is in milliseconds
    static int shortIntervalDelay = 6000; //This is in milliseconds
    static int mediumIntervalDelay = 120000; //This is in milliseconds
    static int longIntervalDelay = 180000; //This is in milliseconds equal to 2 minute
    static int initialRate = 1000; //Initial number Of transactions for the task durations (It should be divisible by task durations without reminder. Currently the task duration is 60 seconds )
    static Constants.RateType rateType = Constants.RateType.INCREMENTAL;
    static long rateIncrement = 1000; //Should be set to zero for fixed rate
    static int numberOfRounds = 10;
    private static TestSchedule testSchedule;


    public static UserContext org1Admin;
    public static ArrayList<Orderer> orderers= new ArrayList<>();
    public static ArrayList<Peer> peers= new ArrayList<>();
    public static Channel mychannel;
    private static ArrayList<TestRound> testRounds= new ArrayList<>();

    static  BlockListener blockListener;
    static HashMap<Long, CustomBlock> customBlockList= new HashMap<>();
    static HashMap<Long, CustomBlock> customBlockList2= new HashMap<>();
    public static CAClient caClient;
    public static FabricClient fabClient;
    private static ArrayList<UserContext> registeredUserList = new ArrayList<>();
    private static ArrayList<UserContext> registeredConsumerList = new ArrayList<>();
    private static ArrayList<UserContext> registeredProducerList = new ArrayList<>();
    private static BlockListener blockListener2;


    public static void main(String[] args) throws IllegalAccessException, InvocationTargetException, InvalidArgumentException, InstantiationException, NoSuchMethodException, CryptoException, ClassNotFoundException {
        // Generate crypto suite for the admin user
        CryptoSuite.Factory.getCryptoSuite();
        Util.cleanUp();
        org1Admin = new UserContext();
        File pkFolder1 = new File(Config.ORG1_USR_ADMIN_PK);
        File[] pkFiles1 = pkFolder1.listFiles();
        File certFolder1 = new File(Config.ORG1_USR_ADMIN_CERT);
        File[] certFiles1 = certFolder1.listFiles();
        Enrollment enrollOrg1Admin  = null;

        try {
            enrollOrg1Admin  = Util.getEnrollment(Config.ORG1_USR_ADMIN_PK, pkFiles1[0].getName(),
                    Config.ORG1_USR_ADMIN_CERT, certFiles1[0].getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        org1Admin .setEnrollment(enrollOrg1Admin );
        org1Admin .setMspId(Config.ORG1_MSP);
        org1Admin.setName(Config.ADMIN);
        org1Admin.setAffiliation(Config.ORG1);

        fabClient = new FabricClient(org1Admin);

        orderers= CreateChannel.createOrderers(numberOfOrderers, fabClient);

        mychannel = CreateChannel.createNewChannel(fabClient,org1Admin,orderers.get(0));

        peers = CreateChannel.createPeers(numberOfPeers, fabClient);
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        CreateChannel.joinPeers(peers,mychannel);

        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        CreateChannel.addOrderers(orderers,mychannel);

        // DeployInstantiateChaincode.addPeers(peers, mychannel);

        try {
            mychannel.initialize();
        } catch (TransactionException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // This is just to check if the peers are successfully registered
        CreateChannel.getPeersFromChannel(mychannel);

        DeployInstantiateChaincode.deployChainCode(fabClient, peers);
        String[] chainCodeArguments = { "" };

        try {
            Thread.sleep(60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DeployInstantiateChaincode.instantiateChainCode(mychannel, fabClient, chainCodeArguments);
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        RegisterEnrollUser.enrollAdmin(org1Admin);


        registerUsers2(numberOfUserAccounts);
        registeredConsumerList=new ArrayList<UserContext>(registeredUserList.subList(0, registeredUserList.size() / 2));
        registeredProducerList= new ArrayList<UserContext>(registeredUserList.subList(registeredUserList.size()/2, registeredUserList.size()));
        System.out.println(" Number of Registered Consumers: "+registeredConsumerList.size());
        System.out.println(" Number of Registered Producers: "+registeredProducerList.size());


        long testStartTime= System.currentTimeMillis() + startUpDelay; // Sometime in the future
        System.out.println("Test Start Time: "+testStartTime);
        long roundDuration=0;

        //Create rate controller for the test
        RateController rateController = new RateController(rateType,rateIncrement,initialRate); //Increment should be set to zero for Fixed rate

        //Create rounds and list of tasks for each of the rounds
        // Set different parameters for each of the tasks. The parameters apply to one round. Add the tasks to round
        // We can set different transaction rate for different rounds
        for (int i =0; i <numberOfRounds;i++){
            // long roundStartTime=0;
            TestRound testRound= new TestRound(i);
            //Add the test round to the list of test rounds
            testRounds.add(testRound);
            //Create round tasks for each of the rounds. Also set the contractID, methodToInvoke and data during Task object creation
            // The tasks should be put in the correct sequential order. Start with BiM tasks, then PM tasks, then BAM tasks and finally PS tasks
            testRound.tasks = addRoundTasks(); // We can have different types of tasks for different rounds but for now, we are using the same list of task for all rounds

            // Determine the round duration based on the selected tasks
            roundDuration=0; //we assume the same duration for all rounds as we have the same list of tasks but the round duration can be set different for different rounds
            for(Task task:testRound.tasks){ //Assuming that the tasks are put in the correct sequential order
                if(task.taskType==Constants.TransactionType.BiM_MultiSigContract || task.taskType==Constants.TransactionType.PM_Bidding ||task.taskType==Constants.TransactionType.BaM_Offers ){
                    roundDuration+=task.duration+longIntervalDelay;
                }
                else if(task.taskType==Constants.TransactionType.PM_getDispachedEnergy ||task.taskType==Constants.TransactionType.PS_getBalance){
                    roundDuration+=task.duration+mediumIntervalDelay;
                }
                else{
                    roundDuration+=task.duration+shortIntervalDelay;
                }
            }
            testRound.roundDuration=roundDuration;

            //Determine round start. Again we assume the same duration for all rounds as we have the same list of tasks for all rounds
            if (i==0) {// if this is the first round
                testRound.roundStartTime=testStartTime +mediumIntervalDelay; // To give more time for the first round
                testRound.roundEndTime=testRound.roundStartTime+ testRound.roundDuration;
            }
            else {   // The start of the next round is the same us the end of the previous round. There is no time gap between the two.
                testRound.roundStartTime = testRounds.get(i-1).roundEndTime +mediumIntervalDelay;// We also add short delay between rounds not inside a round
                testRound.roundEndTime=testRound.roundStartTime+testRound.roundDuration;
            }

            //Determine the start and end time for each of the tasks in the round
            for(int j=0;j<testRound.tasks.size();j++) { //We assume that the tasks are put in the correct sequential order
                Task task = testRound.tasks.get(j);
                if (j == 0) { //If this is the first task in the round
                    task.startTime = testRound.roundStartTime + longIntervalDelay; //Because the first task is usually Market initialization sent by the dSO, so shwort interval is enough
                } else {
                    Task previousTask = testRound.tasks.get(j - 1); //The starting time of the current task is determined based on the type of task prior to this task
                    if (previousTask.taskType == Constants.TransactionType.BiM_MultiSigContract || previousTask.taskType == Constants.TransactionType.PM_Bidding || previousTask.taskType == Constants.TransactionType.BaM_Offers) {
                        // We use long duration after each of the above tasks
                        task.startTime = previousTask.endTime + longIntervalDelay;
                    }
                    else if(previousTask.taskType==Constants.TransactionType.PM_getDispachedEnergy || previousTask.taskType==Constants.TransactionType.PS_getBalance){
                        task.startTime = previousTask.endTime +mediumIntervalDelay;
                    }
                    else {
                        task.startTime = previousTask.endTime + shortIntervalDelay;
                    }
                }
                task.endTime = task.startTime + task.duration;
            }

            // Set the transaction rate and
            //The number of transactions in each round should not be greater than the number of accounts registered on the Blockchain as each transaction uses different account
            for(Task task:testRound.tasks){
                task.totalNumberOfTransactionsPerRound=rateController.getNumberOfTransactionsForThisRound(i,task.taskType);
                task.systemWideTransactionRate=task.totalNumberOfTransactionsPerRound/(task.duration/1000);

                if(task.taskType==Constants.TransactionType.BiM_MultiSigContract || task.taskType==Constants.TransactionType.PM_Bidding || task.taskType==Constants.TransactionType.BaM_Offers || task.taskType==Constants.TransactionType.PM_getDispachedEnergy || task.taskType==Constants.TransactionType.PS_getBalance){
                    task.transactionRatePerClient=task.systemWideTransactionRate/2; // There are two groups of clients i.e. consumer and producer client. And each of them have several worker threads
                    // totalNumberOfTransactionsPerRound and systemWideTransactionRate should be divisible by the number of clients without reminder as it uses long not double. or Different transaction rate should be assigned for consumer and producer separately
                    task.numberOfTransactionsPerClient=task.totalNumberOfTransactionsPerRound/2; // Currenty, the number of transactions sent by the consumer and producers are equal but could be modified to set it different
                }
                else{
                    // All other types of transactions are sent by DSO and they are just one transaction per round
                    task.transactionRatePerClient=1;
                    task.numberOfTransactionsPerClient=1;
                }
            }


            System.out.println("Round " +i+ " Start Time "+testRound.roundStartTime);
            for(int j=0;j<testRound.tasks.size();j++){
                System.out.println("Round " +i+ " Task "+j+" Type: "+testRound.tasks.get(j).taskType+" Start Time: "+testRound.tasks.get(j).startTime+" End Time: "+testRound.tasks.get(j).endTime+" Duration: "+testRound.tasks.get(j).duration+
                        " System Wide Transaction Rate: "+testRound.tasks.get(j).systemWideTransactionRate+" Total Number of Transactions: "+testRound.tasks.get(j).totalNumberOfTransactionsPerRound);

            }
            System.out.println("Round " +i+ " End Time "+testRound.roundEndTime);
            System.out.println("Round " +i+ " Round Duration "+testRound.roundDuration);
        }

        long testDuration= numberOfRounds*(roundDuration +mediumIntervalDelay) +startUpDelay + finishingDelay;// We assume that all rounds have the same duration. This does not work if the rounds have different duration
        long testEndTime= testStartTime+testDuration +finishingDelay; //We add finishing delay to give time for any unfinished task
        System.out.println("Test End Time: "+testEndTime);
        System.out.println("Test Duration: "+testDuration);

        //Create test schedule consisting of all rounds above
        testSchedule= new TestSchedule(testStartTime,testEndTime,numberOfRounds,testRounds);
        Report.setTestSchedule(testSchedule);

        // create the DSO
        Thread dso= new Thread (new DSO(testSchedule,org1Admin),"DSO"); //The Admin user is the DSO


        //Create the smart meters

        Thread consumer= new Thread(new SmartMeter(Constants.Role.CONSUMER,ordererUrls, registeredConsumerList, testSchedule),"Consumer");
        smartMeters.add(consumer);
        Thread producer= new Thread(new SmartMeter(Constants.Role.PRODUCER, ordererUrls, registeredProducerList, testSchedule),"Producer");
        smartMeters.add(producer);

        //Start Dso. The DSO is responsible for tasks such as starting and stopping the bidding, and market clearance, market resseting
        dso.start();

        //Start the smart meters. The smart meters send bids and offers
        consumer.start();
        producer.start();

        // This starts block listeners that listen for new blocks created and added to the blockchain to be used later in report calculation
        startBlockListeners();

        try {
            Thread.sleep((testSchedule.endTime-System.currentTimeMillis()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(finishingDelay); //This is to give time for fetching the last blocks
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Received Custom Block Size1: ========"+customBlockList.size());
        System.out.println("Received Custom Block Size2: ========"+customBlockList2.size());
        customBlockList.putAll(customBlockList2); // Merging blocks from two listeners. We start two listeners just to make sure no block is missed
        System.out.println("Merged Custom Block Size: ========"+customBlockList.size());


        Report.calculateConfirmationTime2(customBlockList, mychannel);
        Report.groupTransactionByType();
        Report.calculateTheNumberOfSuccessfulAndFailedTransactions();
        Report.calculateRoundTransactionLatency();
        Report.calculateRoundThroughtput();
        Report.calculateSummaryResult();
        Report.printReport();
        Report.writeToFile();

        System.out.println("==========================End of the Test===============================");

    }

    private static void registerUsers(int numberOfUserAccounts) {
        String pathToPublicKey;
        String pathToCertificate;
        File pkFolder1 = new File(Config.USER1_USR_ADMIN_PK);
        File[] pkFiles1 = pkFolder1.listFiles();
        File certFolder1 = new File(Config.USER1_USR_ADMIN_CERT);
        File[] certFiles1 = certFolder1.listFiles();
        Enrollment enrollment = null;
        try {
            enrollment = Util.getEnrollment(Config.USER1_USR_ADMIN_PK, pkFiles1[0].getName(),
                    Config.USER1_USR_ADMIN_CERT, certFiles1[0].getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        for(int i=0;i<numberOfUserAccounts;i++){
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(i<numberOfUserAccounts/2)
                registeredConsumerList.add(RegisterEnrollUser.enrollUser(i, caClient, enrollment));
            else
                registeredProducerList.add(RegisterEnrollUser.enrollUser(i, caClient, enrollment));
        }
    }

    private static void registerUsers2(int numberOfUserAccounts) {
        int range = 100;
        long firstThreadStartingTime = System.currentTimeMillis() + 120000;
        long startingTime = 0;
        long previousStartingTime = 0;
        int startingUserID, endUserID = 0;
        int numberOfThreads = numberOfUserAccounts / range;
        ArrayList<Thread> registrars = new ArrayList<>();
        HFCAClient hfcaClient = null;
        try {
            hfcaClient = HFCAClient.createNewInstance(Config.CA_ORG1_URL, null );
            CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
            hfcaClient.setCryptoSuite(cryptoSuite);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < numberOfThreads; i++) {
            startingUserID = i * range;
            endUserID = startingUserID + range-1;
            System.out.println(" Registering Users "+startingUserID +" to "+endUserID);
            if (i == 0) {
                startingTime = firstThreadStartingTime;
            } else {
                startingTime = previousStartingTime + 1000;
            }
            Thread registrar = new Thread(new Registrar(startingTime, startingUserID, endUserID, hfcaClient, org1Admin));
            previousStartingTime = startingTime;
            registrars.add(registrar);
            registrar.start();
        }

        for (Thread thread : registrars) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public static synchronized void  pushRegisteredUsers(ArrayList<UserContext> userContext) {
        registeredUserList.addAll(userContext);
    }

    private static void startBlockListeners() {
        blockListener = new BlockListener() {
            @Override
            public void received(BlockEvent blockInfo) {
                //System.out.println("BLock All FIelds :" + block.getAllFields());
                //  System.out.println("BLock Number :" + arg0.getBlockNumber());
                customBlockList.putIfAbsent(blockInfo.getBlockNumber(), new CustomBlock(blockInfo,System.currentTimeMillis()));
            }
        };

        blockListener2 = new BlockListener() {
            @Override
            public void received(BlockEvent blockInfo) {
                //System.out.println("BLock All FIelds :" + block.getAllFields());
                //  System.out.println("BLock Number :" + arg0.getBlockNumber());
                customBlockList2.putIfAbsent(blockInfo.getBlockNumber(), new CustomBlock(blockInfo,System.currentTimeMillis()));
            }
        };

        try {
            mychannel.registerBlockListener(blockListener);
            mychannel.registerBlockListener(blockListener2);
        } catch (InvalidArgumentException e) {
            e.printStackTrace();
        }
    }


//For now, i am creating only Pool Market tasks but we can create any one of the tasks as needed
    // The tasks should be put in the correct sequential order. Start with BiM tasks, then PM tasks, then BAM tasks and finally PS tasks
    //Also set the contractID, methodToInvoke and data during Task object creation

    private static ArrayList<Task> addRoundTasks() { //Also set the contractID, methodToInvoke and data during Task object creation
        Task task;
        ArrayList<Task> roundTasks= new ArrayList<>();
       task= new Task(Constants.TransactionType.PM_Bidding,TaskDurations.PM_BiddingDuration);
        roundTasks.add(task);
        task= new Task(Constants.TransactionType.PM_MarketClearance,TaskDurations.PM_MarketClearanceDuration);
        roundTasks.add(task);

        task= new Task(Constants.TransactionType.PS_SettlingPayments,TaskDurations.PS_SettlePaymentsDuration);
        roundTasks.add(task);

        task= new Task(Constants.TransactionType.PS_getBalance,TaskDurations.PS_getBalanceDuration);
        roundTasks.add(task);

        task= new Task(Constants.TransactionType.PM_MarketResetting,TaskDurations.PM_MarketResettingDuration);
        roundTasks.add(task);

        return roundTasks;
    }

    public static long getRunningDuration (Constants.TransactionType methodToInvoke){
        if(methodToInvoke==Constants.TransactionType.PM_Initialization){
            return TaskDurations.PM_InitializationDuration;
        }
        else if(methodToInvoke==Constants.TransactionType.PM_Bidding){
            return TaskDurations.PM_BiddingDuration;
        }
        else if (methodToInvoke==Constants.TransactionType.PM_MarketClearance){
            return TaskDurations.PM_MarketClearanceDuration;
        }
        else if(methodToInvoke==Constants.TransactionType.PM_MarketResetting){
            return TaskDurations.PM_MarketResettingDuration;
        }
        else {
            return 0;
        }
    }



}