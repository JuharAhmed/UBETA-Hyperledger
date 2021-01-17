package main.java;

import java.math.BigInteger;
import java.util.ArrayList;

public class EnergyData {
    int roundNo;
    int smartMeterID;
    Constants.TransactionType taskType;
    ArrayList<String []> energyPricePairList= new ArrayList<>();
    String energyAmount;
    String price;
}
