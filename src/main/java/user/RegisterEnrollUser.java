/****************************************************** 
 *  Copyright 2018 IBM Corporation 
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License. 
 *  You may obtain a copy of the License at 
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License.
 */
package main.java.user;

import main.java.MainJavaClass;
import main.java.client.CAClient;
import main.java.config.Config;
import main.java.user.UserContext;
import main.java.util.Util;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;

import static main.java.MainJavaClass.caClient;
import static main.java.MainJavaClass.org1Admin;

/**
 * 
 * @author Balaji Kadambi
 *
 */

public class RegisterEnrollUser {


	public static void enrollAdmin (UserContext adminUserContext){
		//Util.cleanUp();
	try {
		// Enroll Admin to Org1MSP
		String caUrl = Config.CA_ORG1_URL;
		CAClient caClient = new CAClient(caUrl, null);

		//UserContext adminUserContext = new UserContext();
		adminUserContext.setName(Config.ADMIN);
		adminUserContext.setAffiliation(Config.ORG1);
		adminUserContext.setMspId(Config.ORG1_MSP);
		caClient.setAdminUserContext(adminUserContext);
		adminUserContext = caClient.enrollAdminUser(Config.ADMIN, Config.ADMIN_PASSWORD);
		}
	 catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static UserContext enrollUser(int userID, CAClient caClient, Enrollment enrollment){
		// Register and Enroll user to Org1MSP
		//Util.cleanUp();
		UserContext userContext=null;
	try {
		//Util.cleanUp();
		//String caUrl = Config.CA_ORG1_URL;
		//CAClient caClient = new CAClient(caUrl, null);
		// Enroll Admin to Org1MSP
/*		UserContext adminUserContext = new UserContext();
		adminUserContext.setName(Config.ADMIN);
		adminUserContext.setAffiliation(Config.ORG1);
		adminUserContext.setMspId(Config.ORG1_MSP);
		caClient.setAdminUserContext(adminUserContext);
		adminUserContext = caClient.enrollAdminUser(Config.ADMIN, Config.ADMIN_PASSWORD);*/

		userContext = new UserContext();
		userContext.setEnrollment(enrollment);
		String name = "user"+String.valueOf(userID);
		userContext.setName(name);
		userContext.setAffiliation(Config.ORG1);
		userContext.setMspId(Config.ORG1_MSP);

		String eSecret = caClient.registerUser(name, Config.ORG1);
		userContext = caClient.enrollUser(userContext, eSecret);
		}
	catch (Exception e) {
			e.printStackTrace();
		}
	return userContext;
	}

	public static UserContext registerAndEnrollUser(int userID, HFCAClient hfcaClient, UserContext adminUserContext ){

		UserContext userContext=null;
		try {
			userContext = new UserContext();
			String name = "user"+String.valueOf(userID);
			userContext.setName(name);
			userContext.setAffiliation(Config.ORG1);
			userContext.setMspId(Config.ORG1_MSP);
			RegistrationRequest rr = new RegistrationRequest(name, Config.ORG1);
			String enrollmentSecret = hfcaClient.register(rr, adminUserContext);
			Enrollment enrollment = hfcaClient.enroll(userContext.getName(), enrollmentSecret);
			userContext.setEnrollment(enrollment);
			//Util.writeUserContext(userContext);
		} catch (Exception e) {
			e.printStackTrace();
		}
return  userContext;
	}

}
