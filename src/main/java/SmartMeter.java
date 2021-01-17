package main.java;
import main.java.chaincode.*;
import main.java.user.UserContext;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.ExecutionException;

public class SmartMeter implements Runnable {
    public int smartMeterID;
    Constants.Role role;
    TestSchedule testSchedule;
    ArrayList<Transaction> transactionList = new ArrayList<>();
    private Transaction transaction;
    private long submissionTime;
    private long responseTime;
    private Timer timer;
    ArrayList<Task> myRoundTasks;
    ArrayList<String> urls;
    ArrayList<UserContext> userList;
    ArrayList <EnergyData>  energyDataList;

    SmartMeter(Constants.Role role, ArrayList<String> urls, ArrayList<UserContext> userList, TestSchedule testSchedule){
        this.role=role;
        this.urls=urls;
        this.userList=userList;
        this.testSchedule=testSchedule;
    }

    @Override
    public void  run() {
        int numberOfRounds=testSchedule.numberOfRounds;
        Task task;
        long timeGapBetweenSchedulingAndSending=5000;
        long waitingTime=0;
        long sendingTime;
        int transactionCounter;

        if (this.role==  Constants.Role.CONSUMER){
            System.out.println("Consumer Main Thread started");
            for(int round=0; round <numberOfRounds;round++){

                waitingTime=testSchedule.testRounds.get(round).roundStartTime - System.currentTimeMillis();
                try {
                    if(waitingTime >1000)
                        Thread.sleep(waitingTime); //  Timers are scheduled just when 50 milli seconds is left for the transaction to be sent
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                myRoundTasks=getSmartMeterTasks(round);
                //Scheduling tasks for the round
                for(int ts=0;ts<myRoundTasks.size();ts++){
                   task=myRoundTasks.get(ts);

                  if(task.taskType==Constants.TransactionType.BiM_MultiSigContract){

                   }
                  else if(task.taskType==Constants.TransactionType.PM_Initialization){

                  }
                 else if(task.taskType==Constants.TransactionType.PM_Bidding){
                      readPMBidData(task.numberOfTransactionsPerClient);
                   //Scheduling Worker Threads ahead of time
                     transactionCounter=0; // Also used as the ID of the worker threads
                      System.out.println("Consumer Main Thread: Round: "+round+" Scheduling Worker Threads to send Bids at a rate of: " +task.transactionRatePerClient+
                              " TPS for a duration of "+task.duration/1000+" seconds");

                    while(transactionCounter<task.numberOfTransactionsPerClient){
                         sendingTime=task.startTime +transactionCounter*(1000/task.transactionRatePerClient);
                        BidSender bidSender= new BidSender( sendingTime, urls, round, transactionCounter, userList.get(transactionCounter), energyDataList.get(transactionCounter).energyAmount, energyDataList.get(transactionCounter).price);
                        Thread thread= new Thread(bidSender);
                          thread.start();
                          transactionCounter++;
                      }

                  }
                  else if(task.taskType==Constants.TransactionType.PS_getBalance){
                       transactionCounter=0; // Also used as the ID of the worker threads
                      System.out.println("Consumer Main Thread: Round: "+round+" Scheduling Worker Threads to Query balance at a rate of: " +task.transactionRatePerClient+
                              " TPS for a duration of "+task.duration/1000+" seconds");
                      while(transactionCounter<task.numberOfTransactionsPerClient){
                         sendingTime=task.startTime +transactionCounter*(1000/task.transactionRatePerClient);
                          QueryBalance queryBalance = new QueryBalance(sendingTime, urls, round, transactionCounter,userList.get(transactionCounter));
                          Thread thread= new Thread(queryBalance);
                          thread.start();
                          transactionCounter++;
                      }

                  }

                  else if(task.taskType==Constants.TransactionType.BaM_Offers){

                  }
                  else if(task.taskType==Constants.TransactionType.PS_ConsumptionProduction){

                  }


                }

                System.out.println("Consumer Main Thread Round "+round+" Scheduling is Finished");

            }

        }
        else if (this.role ==  Constants.Role.PRODUCER){

            System.out.println("Producer Main Thread started");

            for(int round=0; round <numberOfRounds;round++){

                waitingTime=testSchedule.testRounds.get(round).roundStartTime - System.currentTimeMillis();
                try {
                    if(waitingTime >1000)
                        Thread.sleep(waitingTime); //  Timers are scheduled just when 50 milli seconds is left for the transaction to be sent
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                myRoundTasks=getSmartMeterTasks(round);
                //Scheduling tasks for the round
                for(int ts=0;ts<myRoundTasks.size();ts++){
                    task=myRoundTasks.get(ts);

                    if(task.taskType==Constants.TransactionType.BiM_MultiSigContract){

                    }
                    else if(task.taskType==Constants.TransactionType.PM_Initialization){

                    }
                    else if(task.taskType==Constants.TransactionType.PM_Bidding){
                        readPMOfferData(task.numberOfTransactionsPerClient);
                        transactionCounter=0;
                        System.out.println("Producer Main Thread Round: "+round+" Scheduling Worker Threads to send Offers at a rate of: " +task.transactionRatePerClient+
                                " TPS for a duration of "+task.duration/1000+" seconds");
                        while(transactionCounter<task.numberOfTransactionsPerClient){
                            sendingTime=task.startTime +transactionCounter*(1000/task.transactionRatePerClient);
                           OfferSender offerSender= new OfferSender(sendingTime, urls,round, transactionCounter, userList.get(transactionCounter), energyDataList.get(transactionCounter).energyAmount, energyDataList.get(transactionCounter).price);
                           Thread thread= new Thread(offerSender);
                            thread.start();
                            transactionCounter++;

                        }

                    }
                    else if(task.taskType==Constants.TransactionType.PS_getBalance) {
                        transactionCounter = 0; // Also used as the ID of the worker threads
                        System.out.println("Producer Main Thread: Round: " + round + " Scheduling Worker Threads to Query Balance at a rate of: " + task.transactionRatePerClient +
                                " TPS for a duration of " + task.duration / 1000 + " seconds");
                        while (transactionCounter < task.numberOfTransactionsPerClient) {
                            sendingTime=task.startTime +transactionCounter*(1000/task.transactionRatePerClient);
                            QueryBalance queryBalance = new QueryBalance(sendingTime, urls, round, transactionCounter, userList.get(transactionCounter));
                            Thread thread= new Thread(queryBalance);
                            thread.start();
                            transactionCounter++;
                        }
                    }

                    else if(task.taskType==Constants.TransactionType.BaM_Offers){

                    }
                    else if(task.taskType==Constants.TransactionType.PS_ConsumptionProduction){

                    }

                }

                System.out.println("Producer Main Thread Round "+round+" Scheduling is Finished");
            }

        }

     // Waiting until worker Threads finish returning back transactions to this thread;
        try {
            if(testSchedule.endTime-System.currentTimeMillis()>100)
           Thread.sleep(testSchedule.endTime-System.currentTimeMillis());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    private ArrayList<Task> getSmartMeterTasks(int round) {
        ArrayList<Task> roundTasks;
        ArrayList<Task> smartMeterTasks=new ArrayList<>();
        System.out.println("Smart Meter Round "+round+" Tasks: ");
        roundTasks=testSchedule.testRounds.get(round).tasks;
        for(Task ts:roundTasks){
            if(ts.taskType==Constants.TransactionType.BiM_MultiSigContract || ts.taskType==Constants.TransactionType.PM_Bidding || ts.taskType==Constants.TransactionType.PM_getDispachedEnergy || ts.taskType==Constants.TransactionType.PS_getBalance || ts.taskType==Constants.TransactionType.BaM_Offers || ts.taskType==Constants.TransactionType.PS_ConsumptionProduction){
                smartMeterTasks.add(ts);
                System.out.println("Task Type: "+ts.taskType);
            }
        }
        return smartMeterTasks;
    }

    public void readPMBidData(long numberOfTransactions) {
        energyDataList=new ArrayList<>();
        System.out.println("Reading Bidding Data from File");
        //File path for energy bid data
        String powerDataFile = "/home/ubuntu/web3j/PowerData/Western Australia/WesAus_PoolMarketBidData.xlsx"; // This is only for PM_Bidding
       // For windows
        // String powerDataFile = "C:/Users/s3753266/RMIT-C/web3j/PoolMarketBidData.xlsx";
        Workbook workbook = null;
        try {
            workbook = WorkbookFactory.create(new File(powerDataFile));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        }
        // Getting the Sheet at index zero
        Sheet sheet = workbook.getSheetAt(0);
        long numberOfRowsToRead=0 ;
        for (Row row: sheet) {
            //We read one more row as the first row starts from 1 instead of 0
            if (numberOfRowsToRead > numberOfTransactions) { // The data should not be less than the number of transactions in each round
                break;
            }
            if (numberOfRowsToRead == 0) {
                //Don't read anything as this the header row
            } else {
                EnergyData energyData = new EnergyData();
                energyData.energyAmount = String.valueOf(Math.abs((int)row.getCell(5).getNumericCellValue()));
                energyData.price = String.valueOf(Math.abs((int) row.getCell(6).getNumericCellValue()));
                System.out.println("Energy Bid: Amount: "+energyData.energyAmount+" Price: "+energyData.price);
            energyDataList.add(energyData);
           // System.out.println("Consumer Main Thread: " + " Energy Amount: " + energyData.energyAmount + " Energy price: " + energyData.price);
        }
           numberOfRowsToRead++;
        }
        System.out.println("Reading Bidding Data Finished");
    }

    private void readPMOfferData(long numberOfTransactions) {
        energyDataList=new ArrayList<>();
        System.out.println("Reading Offer Data From File");
        //File path for energy offer data
      String powerDataFile = "/home/ubuntu/web3j/PowerData/Western Australia/WesAus_PoolMarketOfferData.xlsx"; // This is only for PM_Bidding
      // For Windows
        //  String powerDataFile = "C:/Users/s3753266/RMIT-C/web3j/PoolMarketOfferData.xlsx";
        Workbook workbook = null;
        try {
            workbook = WorkbookFactory.create(new File(powerDataFile));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        }
        // Getting the Sheet at index zero
        Sheet sheet = workbook.getSheetAt(0);
        long numberOfRowsToRead=0;
        for (Row row: sheet) {
            //We read one more row as the first row starts from 1 instead of 0
            if (numberOfRowsToRead > numberOfTransactions) { // The data should not be less than thenumber of transactions in each round
                break;
            }
            if (numberOfRowsToRead == 0) {
                //Don't read anything as this the header row
            } else {
                EnergyData energyData = new EnergyData();
                energyData.energyAmount = String.valueOf(Math.abs((int)row.getCell(5).getNumericCellValue()));
                energyData.price = String.valueOf(Math.abs((int)row.getCell(6).getNumericCellValue()));
                 // System.out.println("Energy Offer: Amount: "+energyData.energyAmount+" Price: "+energyData.price);
                energyDataList.add(energyData);
              //  System.out.println("Producer Main Thread: " + " Energy Amount: " + energyData.energyAmount + " Energy price: " + energyData.price);
            }
            numberOfRowsToRead++;
        }
    }


}
