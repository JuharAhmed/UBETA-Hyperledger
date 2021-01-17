//This class defined the time duration that each of the tasks shopuld run
package main.java;

public class TaskDurations {
 //All values are given in milliseconds
 final static long BiM_MultiSigContractDuration = 10000;//(This is in milliseconds)
 final static long BiM_ResettingDuration= 20000;//(This is in milliseconds)
 final static long PM_InitializationDuration= 30000;
 final static long PM_BiddingDuration= 60000;
 final static long PM_MarketClearanceDuration= 30000;
 final static long PM_getDispachedEnergyDuration=60000;
 final static long PM_MarketResettingDuration= 30000;
 final static long BaM_CalculateMismatchDuration= 20000;
 final static long BaM_OffersDuration= 50000;
 final static long BaM_MarketClearanceDuration= 20000;
 final static long BaM_MarketResettingDuration= 20000;
 final static long PS_ConsumptionProduction= 10000;
 final static long PS_SettlePaymentsDuration= 30000;
 final static long PS_getBalanceDuration=60000;
 final static long PS_ResettingDuration= 30000;

}
