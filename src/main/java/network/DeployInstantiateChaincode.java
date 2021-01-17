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
package main.java.network;

import main.java.MainJavaClass;
import main.java.client.FabricClient;
import main.java.config.Config;
import main.java.client.ChannelClient;
import main.java.client.FabricClient;
import main.java.user.UserContext;
import main.java.util.Util;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.TransactionRequest.Type;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Balaji Kadambi
 *
 */

public class DeployInstantiateChaincode {

	public static void main(String[] args) {
		try {


			


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Adding peers to channel
	public  static void addPeers (ArrayList<Peer> peers, Channel mychannel){
		for(Peer p: peers){
			try {
				mychannel.addPeer(p);
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			}
		}
	}

	public static void deployChainCode (FabricClient fabClient, List<Peer> peers){
		Collection<ProposalResponse> response = null;
		try {
		response = fabClient.deployChainCode(Config.CHAINCODE_1_NAME,
					Config.CHAINCODE_1_PATH, Config.CHAINCODE_ROOT_DIR, Type.GO_LANG.toString(),
					Config.CHAINCODE_1_VERSION, peers);
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ProposalException e) {
			e.printStackTrace();
		}

		for (ProposalResponse res : response) {
			Logger.getLogger(DeployInstantiateChaincode.class.getName()).log(Level.INFO,
					Config.CHAINCODE_1_NAME + "- Chain code deployment " + res.getStatus());
		}
	}

	public static void instantiateChainCode(Channel mychannel, FabricClient fabClient, String [] arguments) {
		ChannelClient channelClient = new ChannelClient(mychannel.getName(), mychannel, fabClient);
		Collection<ProposalResponse> response = null;
		try {
			response = channelClient.instantiateChainCode(Config.CHAINCODE_1_NAME, Config.CHAINCODE_1_VERSION,
					Config.CHAINCODE_1_PATH, Type.GO_LANG.toString(), "init", arguments, null);
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (ProposalException e) {
			e.printStackTrace();
		} catch (ChaincodeEndorsementPolicyParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (ProposalResponse res : response) {
			Logger.getLogger(DeployInstantiateChaincode.class.getName()).log(Level.INFO,
					Config.CHAINCODE_1_NAME + "- Chain code instantiation " + res.getStatus());
		}
	}
}
