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

import main.java.client.FabricClient;
import main.java.user.UserContext;
import main.java.MainJavaClass;
import main.java.config.Config;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;

import javax.naming.InvalidNameException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Balaji Kadambi
 *
 */

public class CreateChannel {
	public static ArrayList <Orderer> createOrderers (int numberOfOrderers, FabricClient fabClient){
		// Configuring Various Properties for Orderers
		Properties ordererProperties = new Properties();
		ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {10L, TimeUnit.MINUTES});
		ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {60L, TimeUnit.SECONDS});
		ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveWithoutCalls", new Object[] {true});
		ordererProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 102400000);

		ordererProperties.put(org.hyperledger.fabric.sdk.helper.Config.ORDERER_RETRY_WAIT_TIME, new Object[] {600L, TimeUnit.SECONDS});
		ordererProperties.put(org.hyperledger.fabric.sdk.helper.Config.ORDERER_WAIT_TIME, new Object[] {600L, TimeUnit.SECONDS});
		ordererProperties.put(org.hyperledger.fabric.sdk.helper.Config.CLIENT_THREAD_EXECUTOR_KEEPALIVETIME, new Object[] {600L, TimeUnit.SECONDS});
		ordererProperties.put(org.hyperledger.fabric.sdk.helper.Config.CLIENT_THREAD_EXECUTOR_KEEPALIVETIMEUNIT, new Object[] {600L, TimeUnit.SECONDS});
		ordererProperties.put(org.hyperledger.fabric.sdk.helper.Config.GENESISBLOCK_WAIT_TIME, new Object[] {600L, TimeUnit.SECONDS});
		ordererProperties.put(org.hyperledger.fabric.sdk.helper.Config.PROPOSAL_WAIT_TIME, new Object[] {600L, TimeUnit.SECONDS});
		ordererProperties.put(org.hyperledger.fabric.sdk.helper.Config.TRANSACTION_CLEANUP_UP_TIMEOUT_WAIT_TIME, new Object[] {600L, TimeUnit.SECONDS});
		ordererProperties.put(org.hyperledger.fabric.sdk.helper.Config.SERVICE_DISCOVER_WAIT_TIME, new Object[] {600L, TimeUnit.SECONDS});
		ArrayList <Orderer> orderers = new ArrayList<>();
		int port = 0;
		for (int i=0; i<numberOfOrderers; i++){
			port = Config.ordererURLStartingPort + i*1000;
			String ordererUrl= "grpc://localhost:"+String.valueOf(port);
			String ordererName= "orderer"+String.valueOf(i)+".example.com";
			System.out.println(" Orderer "+i +" Name: "+ ordererName +" URL: "+ordererUrl);
			MainJavaClass.ordererUrls.add(ordererUrl);
			Orderer orderer = null;
			try {
				orderer = fabClient.getInstance().newOrderer(ordererName, ordererUrl, ordererProperties);
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			}
			orderers.add(orderer);
		}
		return orderers;
	}

	public static Channel createNewChannel (FabricClient fabClient, UserContext org1Admin, Orderer orderer){
		// Create a new channel

		ChannelConfiguration channelConfiguration = null;
		Channel mychannel=null;
		try {
			channelConfiguration = new ChannelConfiguration(new File(Config.CHANNEL_CONFIG_PATH));
			byte[] channelConfigurationSignatures = fabClient.getInstance()
					.getChannelConfigurationSignature(channelConfiguration, org1Admin);

			mychannel = fabClient.getInstance().newChannel(Config.CHANNEL_NAME, orderer, channelConfiguration,
					channelConfigurationSignatures);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			e.printStackTrace();
		} catch (TransactionException e) {
			e.printStackTrace();
		}

		return mychannel;
	}

	public static ArrayList<Peer> createPeers(int numberOfPeers, FabricClient fabClient){
		// Configuring Various Properties for peers
		Properties peerProperties = new Properties();
		peerProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {10L, TimeUnit.MINUTES});
		peerProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {60L, TimeUnit.SECONDS});
		peerProperties.put("grpc.NettyChannelBuilderOption.keepAliveWithoutCalls", new Object[] {true});
		peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 102400000);
		ArrayList<Peer> peers = new ArrayList<>();

		peerProperties.put(org.hyperledger.fabric.sdk.helper.Config.ORDERER_RETRY_WAIT_TIME, new Object[] {600L, TimeUnit.SECONDS});
		peerProperties.put(org.hyperledger.fabric.sdk.helper.Config.ORDERER_WAIT_TIME, new Object[] {600L, TimeUnit.SECONDS});
		peerProperties.put(org.hyperledger.fabric.sdk.helper.Config.CLIENT_THREAD_EXECUTOR_KEEPALIVETIME, new Object[] {600L, TimeUnit.SECONDS});
		peerProperties.put(org.hyperledger.fabric.sdk.helper.Config.CLIENT_THREAD_EXECUTOR_KEEPALIVETIMEUNIT, new Object[] {600L, TimeUnit.SECONDS});
		peerProperties.put(org.hyperledger.fabric.sdk.helper.Config.GENESISBLOCK_WAIT_TIME, new Object[] {600L, TimeUnit.SECONDS});
		peerProperties.put(org.hyperledger.fabric.sdk.helper.Config.PROPOSAL_WAIT_TIME, new Object[] {600L, TimeUnit.SECONDS});
		peerProperties.put(org.hyperledger.fabric.sdk.helper.Config.TRANSACTION_CLEANUP_UP_TIMEOUT_WAIT_TIME, new Object[] {600L, TimeUnit.SECONDS});
		peerProperties.put(org.hyperledger.fabric.sdk.helper.Config.SERVICE_DISCOVER_WAIT_TIME, new Object[] {600L, TimeUnit.SECONDS});
		int port = 0;
		for (int i=0; i<numberOfPeers; i++){
			port = Config.peerURLStartingPort + i*1000;
			String peerUrl= "grpc://localhost:"+String.valueOf(port);
			String peerName= "peer"+String.valueOf(i)+".org1.example.com";
			System.out.println(" Peer "+i +" Name: "+ peerName +" URL: "+peerUrl);
				MainJavaClass.peerUrls.add(peerUrl);
			Peer peer = null;
			try {
				peer = fabClient.getInstance().newPeer(peerName, peerUrl,peerProperties);
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			}
			peers.add(peer);
		}

		return peers;
	}

	// Joining peers to channel
	public  static void joinPeers (ArrayList<Peer> peers, Channel mychannel){
		for(Peer p: peers){
			try {
				mychannel.joinPeer(p);
			} catch (ProposalException e) {
				e.printStackTrace();
			}
		}

	}

	//Adding orderers to channel
	public  static void addOrderers (ArrayList<Orderer> orderers, Channel mychannel){
		for(Orderer o: orderers){
			try {
				mychannel.addOrderer(o);
			} catch (InvalidArgumentException e) {
				e.printStackTrace();
			}
		}
	}

	// This is just to check peers are registered appropriately and working
	public static void getPeersFromChannel(Channel mychannel) {
		Logger.getLogger(CreateChannel.class.getName()).log(Level.INFO, "Channel created "+mychannel.getName());
		Collection peers = mychannel.getPeers();
		Iterator peerIter = peers.iterator();
		while (peerIter.hasNext())
		{
			Peer pr = (Peer) peerIter.next();
			Logger.getLogger(CreateChannel.class.getName()).log(Level.INFO,pr.getName()+ " at " + pr.getUrl());
		}
	}

}
