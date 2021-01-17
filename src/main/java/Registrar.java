package main.java;

import main.java.config.Config;
import main.java.user.RegisterEnrollUser;
import main.java.user.UserContext;
import main.java.util.Util;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

import java.util.ArrayList;

public class Registrar implements Runnable {

    private final long startingTime;
    private final int endUserID;
    private final int startingUserID;
    private final Object adminUserContext;
    private final HFCAClient hfcaClient;

    public Registrar(long startingTime, int startingUserID, int endUserID, HFCAClient hfcaClient, UserContext adminUserContext){
    this.startingTime=startingTime;
    this.startingUserID=startingUserID;
    this.endUserID=endUserID;
    this.hfcaClient=hfcaClient;
    this.adminUserContext=adminUserContext;
    }
    @Override
    public void run() {
        ArrayList<UserContext> userContextArrayList= new ArrayList<>();
        try {
            UserContext userContext = new UserContext();
            for (int i = startingUserID; i <= endUserID; i++) {
                String name = "user" + String.valueOf(i);
                userContext.setName(name);
                userContext.setAffiliation(Config.ORG1);
                userContext.setMspId(Config.ORG1_MSP);
                RegistrationRequest rr = new RegistrationRequest(name, Config.ORG1);
                String enrollmentSecret = hfcaClient.register(rr, MainJavaClass.org1Admin);
                Enrollment enrollment = hfcaClient.enroll(userContext.getName(), enrollmentSecret);
                userContext.setEnrollment(enrollment);
                userContextArrayList.add(userContext);
               // Util.writeUserContext(userContext);
            }
        }
        catch (Exception e) {
                e.printStackTrace();
            }
        MainJavaClass.pushRegisteredUsers(userContextArrayList);
       }
}
