package main

import (
   // "fmt"
    "strconv"
	//"bytes"
    "github.com/hyperledger/fabric/core/chaincode/shim"
	sc "github.com/hyperledger/fabric/protos/peer"
	//"github.com/hyperledger/fabric-chaincode-go/shim"
	//sc "github.com/hyperledger/fabric-protos-go/peer"
)

// main function starts up the chaincode in the container during instantiate
    func main() {
        if err := shim.Start(new(PoolMarket)); err != nil {
                shim.Error("Error starting Pool Market ")
        }
    }

   type PoolMarket struct {

    }

   type  EnergyBid struct{
       amount int // Energy amount in KW
       priceRate int // The price of energy per KW
   }

    type  EnergyOffer struct{
         amount int // Energy amount in KW
         priceRate int // The price of energy per KW
    }

	var state int=1

    var suppliers= make([]string, 0)
    var consumers= make([]string, 0)
    var supplierWinners= make([]string, 0)
    var supplierLosers= make([]string, 0)
    var consumerWinners= make([]string, 0)
    var consumerLosers= make([]string, 0)

    var offers= make(map[string] EnergyOffer)
    var bids= make(map[string] EnergyBid)
    var totalOfferForAGivenPriceRate= make(map[int] int)
    var totalDemandForAGivenPriceRate= make(map[int] int)
    var supplierEnergyDispatch= make(map[string] int)
    var consumerEnergyDispatch= make(map[string] int)

    var netPaymentData = make([]int, 0) // Use something like numbers = append(numbers, 1) to add elements
    var allWinnersAddresses= make([]string, 0)

    var roundMaxRate int =100000
    var roundMinRate int =0

    var minAllowedPrice int = 0
    var maxAllowedPrice int = 100000

    var clearingPrice int =0
    var authorizedEntity []byte // may be address can be represented by string
    var _balanceOf=make(map[string] int)
    var productionCapacityOf=make(map[string] int)
    var balance int =10000000
    var productionCapacity int =10000000
    var balancingMarketClearingPrice int=0
	var err error
	var userID []byte

  func (s *PoolMarket) Init(stub shim.ChaincodeStubInterface) sc.Response {
    authorizedEntity, err=stub.GetCreator()
	if err !=nil{
	  return shim.Error("Cannot Initialize Chaincode ")

	}
    return shim.Success(nil)
  }

  func (s *PoolMarket) Invoke(stub shim.ChaincodeStubInterface) sc.Response {
    // Extract the function and args from the transaction proposal
    fn, args := stub.GetFunctionAndParameters()

     if fn == "initializeBidding" {
            return s.initializeBidding(stub)
     } else if fn == "submitEnergyBid" {
                return s.submitEnergyBid(stub, args)
     } else if fn == "submitEnergyOffer" {
                return s.submitEnergyOffer(stub, args)
     } else if fn == "calculateMarkerClearingPrice" {
                return s.calculateMarkerClearingPrice(stub)
     } else if fn == "settlePayments" {
                return s.settlePayments(stub)
     } else if fn == "Market Resetting" {
                return s.resetMarket(stub)
     } else if fn == "getMarketClearingPrice" {
                return s.getMarketClearingPrice(stub)
     } else if fn == "getNumberOfConsumers" {
                return s.getNumberOfConsumers(stub)
     } else if fn == "getNumberOfSuppliers" {
                return s.getNumberOfSuppliers(stub)
     } else if fn == "getNumberOfConsumerWinners" {
                return s.getNumberOfConsumerWinners(stub)
     } else if fn == "getNumberOfConsumerLosers" {
               return s.getNumberOfConsumerLosers(stub)
     } else if fn == "getNumberOfSupplierWinners" {
                return s.getNumberOfSupplierWinners(stub)
     } else if fn == "getNumberOfSupplierLosers" {
              return s.getNumberOfSupplierLosers(stub)
     } else if fn == "getConsumerDispatchedEnergy" {
             return s.getConsumerDispatchedEnergy(stub)
     } else if fn == "getSupplierDispatchedEnergy" {
            return s.getSupplierDispatchedEnergy(stub)
     } else if fn == "getBalance" {
           return s.getBalance(stub)
     } else if fn=="getState" {
	       return s.getState(stub)
	}

	return shim.Error("Invalid function name.")
  }


  func (s *PoolMarket) initializeBidding (stub shim.ChaincodeStubInterface) sc.Response {
	/* var authority, err=stub.GetCreator()
		 res := bytes.Compare(authority, authorizedEntity)
      if res != 0 {
           return shim.Error("Only Admin User Can Initialize Bidding")
      } else if err!=nil {
	    return shim.Error("GetCreator Returned Error")
	  } */
      state=1

	  return shim.Success([]byte("Bidding Initialized Successfully"))
  }


  func (s *PoolMarket) submitEnergyOffer (stub shim.ChaincodeStubInterface, args []string) sc.Response {
            if state !=1 {
                   return shim.Error("Bidding closed")
            }

            var value, err1 = strconv.ParseInt(args[0], 16, 64)
              if err1!=nil {
                  return shim.Error(" strconv.ParseInt(args[0], 16, 64) Returned Error")
              }
			var amount =int(value)
            var value2, err2 = strconv.ParseInt(args[1], 16, 64)
              if err2!=nil {
                  return shim.Error("strconv.ParseInt(args[1], 16, 64) returned error")
              }
			var price =int(value2)
             if (price < minAllowedPrice || price > maxAllowedPrice) {
                  return shim.Error("Price is out of allowed range")
             }
              if amount > productionCapacity {
                  return shim.Error("Amount is greater than allowed production Capacity")
              }

			 userID, err=stub.GetCreator()
            offers[string(userID)]= EnergyOffer{amount, price}
           // balanceOf[userID]=1000000
            totalOfferForAGivenPriceRate[price] = totalOfferForAGivenPriceRate[price] + amount
            suppliers=append (suppliers, string(userID))

			return shim.Success([]byte("Energy Offer Submitted Successfully"))
    }


   func (s *PoolMarket) submitEnergyBid (stub shim.ChaincodeStubInterface, args []string) sc.Response {
                if state !=1 {
                       return shim.Error("Bidding closed")
                }
				var value, err1 = strconv.ParseInt(args[0], 16, 64)
				if err1!=nil {
                  return shim.Error("strconv.ParseInt(args[0], 16, 64) returned error")
              }
				var amount =int(value)
				var value2, err2 = strconv.ParseInt(args[1], 16, 64)
				if err2!=nil {
                  return shim.Error("strconv.ParseInt(args[1], 16, 64) returned error")
              }
				var price =int(value2)
                 if (price < minAllowedPrice || price > maxAllowedPrice) {
                      return shim.Error("Price is out of allowed range")
                 }
					product:=amount*price
                  if (product > balance ) {
                      return shim.Error("No Enough balance")
                  }

		        userID, err=stub.GetCreator()
                bids[string(userID)] = EnergyBid{amount, price}
                totalOfferForAGivenPriceRate[price] = totalOfferForAGivenPriceRate[price] + amount
                consumers=append (consumers, string(userID))

		  return shim.Success([]byte("Energy Bid Submitted Successfully"))
     }


    //Calculating Market Clearing Price
  func (s *PoolMarket) calculateMarkerClearingPrice (stub shim.ChaincodeStubInterface) sc.Response { // We assume that the market clearnce is only for one interval
   	/*	var authority []byte
		authority, err=stub.GetCreator()
		res := bytes.Compare(authority, authorizedEntity)
		if res != 0 {
              return shim.Error("Only Admin User Can Perform Market Clearance")
         } */
    state =2

    for i := 0; i<len(consumers);i++ {
        if(bids[consumers[i]].priceRate > roundMaxRate){
            roundMaxRate=bids[consumers[i]].priceRate
        } else if(bids[consumers[i]].priceRate < roundMaxRate){
             roundMinRate=bids[consumers[i]].priceRate
        }
    }

    for i := 0; i<len(suppliers);i++{
        if(offers[suppliers[i]].priceRate > roundMaxRate){
            roundMaxRate=offers[suppliers[i]].priceRate
        } else if(offers[suppliers[i]].priceRate < roundMinRate){
             roundMinRate=offers[suppliers[i]].priceRate
       }
    }

     var _max = roundMaxRate
     for y:=1;y<=roundMaxRate;y++ {
            totalOfferForAGivenPriceRate[y] += totalOfferForAGivenPriceRate[y-1]; //This is calculating the supply and demand curves
            totalDemandForAGivenPriceRate[_max-1] += totalDemandForAGivenPriceRate[_max]
            _max--
      }

     for v:= 0; v <= roundMaxRate; v++ {
            if (totalOfferForAGivenPriceRate[v] >= totalDemandForAGivenPriceRate[v]) {
                if (totalDemandForAGivenPriceRate[v] > totalOfferForAGivenPriceRate[v-1]) {
                    clearingPrice=v
                } else {
                    clearingPrice=v-1
                }

    //Losers/Winners Mapping also add bool T/F, about winner/loser
            for q:=0; q < len(suppliers); q++ {
                    if (offers[suppliers[q]].priceRate < clearingPrice) {
                    supplierWinners= append(supplierWinners, suppliers[q])
                    supplierEnergyDispatch[suppliers[q]]= offers[suppliers[q]].amount
                   // supplierEnergyDispatchToPaymentSettlemt.push(offers[suppliers[q]].amount)
                    } else if(offers[suppliers[q]].priceRate == clearingPrice){
                        supplierWinners=append(supplierWinners, suppliers[q]);
                        supplierEnergyDispatch[suppliers[q]]= offers[suppliers[q]].amount
                       // supplierEnergyDispatchToPaymentSettlemt.push(offers[suppliers[q]].amount)
                    } else {
                        supplierLosers=append(supplierLosers, suppliers[q])
                      //  supplierEnergyDispatch[suppliers[q]]= SupplierEnergyDispatch(0,clearingPrice)
                    }
                }

            for r:=0; r < len(consumers); r++ {
                    if(bids[consumers[r]].priceRate >= clearingPrice) {
                    consumerWinners= append(consumerWinners, consumers[r])
                    consumerEnergyDispatch[consumers[r]]= bids[consumers[r]].amount
                   // consumerEnergyDispatchToPaymentSettlemt.push(bids[consumers[r]].amount)
                    } else if(bids[consumers[r]].priceRate == clearingPrice){
                    consumerWinners=append(consumerWinners, consumers[r])
                    consumerEnergyDispatch[consumers[r]]= offers[consumers[r]].amount
                   // consumerEnergyDispatchToPaymentSettlemt.push(bids[consumers[r]].amount)
                    } else {
                    consumerLosers=append(consumerLosers, consumers[r]);
                   // consumerEnergyDispatch[consumers[r]]= ConsumerEnergyDispatch(0,clearingPrice)
                    }
                }
           break
            }
        }

		return shim.Success([]byte("Market Cleared Successfully"))
    }

   func (s *PoolMarket) settlePayments (stub shim.ChaincodeStubInterface) sc.Response {
	/*	var authority []byte
      	authority, err=stub.GetCreator()
		res := bytes.Compare(authority, authorizedEntity)
      if res!= 0 {
              return shim.Error("Only Admin User Can Perform Payment Settlement")
         } */

		 state =3

        for i:=0; i<len(consumerWinners);i++ {
            consumerID:=consumerWinners[i]
            deviation:=0 //for now, i assume the deviation is 0 as there is no consumption and production data
            netPayment:= consumerEnergyDispatch[consumerID]*clearingPrice + deviation*balancingMarketClearingPrice
            netPaymentData =append(netPaymentData, netPayment)
            allWinnersAddresses=append(allWinnersAddresses, consumerID)
        }


       for i:=0; i<len(supplierWinners);i++ {
            supplierID:=supplierWinners[i]
            deviation:=0 //for now, i assume the deviation is 0 as there is no consumption and production data
            netPayment:= supplierEnergyDispatch[supplierID]*clearingPrice - deviation*balancingMarketClearingPrice
            netPaymentData =append(netPaymentData, netPayment)
            allWinnersAddresses=append(allWinnersAddresses, supplierID)
        }

    //Update Account balances for all userAddress
       for i:=0; i<len(allWinnersAddresses); i++{
             if(netPaymentData[i]<0){
               _balanceOf[allWinnersAddresses[i]]-= netPaymentData[i]
                } else{
                  _balanceOf[allWinnersAddresses[i]] += netPaymentData[i]
               }
        }

		  return shim.Success([]byte("Payment Settlement Successfully Completed"))
   }


    func (s *PoolMarket) resetMarket (stub shim.ChaincodeStubInterface) sc.Response {

    /*    consumers=nil
        consumerWinners=nil
        consumerLosers=nil

        suppliers=nil
        supplierWinners=nil
        supplierLosers=nil

        allWinnersAddresses=nil
        netPaymentData=nil */

        suppliers= make([]string, 0)
        consumers= make([]string, 0)
        supplierWinners= make([]string, 0)
        supplierLosers= make([]string, 0)
        consumerWinners= make([]string, 0)
        consumerLosers= make([]string, 0)

        offers= make(map[string] EnergyOffer)
        bids= make(map[string] EnergyBid)
        totalOfferForAGivenPriceRate= make(map[int] int)
        totalDemandForAGivenPriceRate= make(map[int] int)
        supplierEnergyDispatch= make(map[string] int)
        consumerEnergyDispatch= make(map[string] int)

        netPaymentData = make([]int, 0) // Use something like numbers = append(numbers, 1) to add elements
        allWinnersAddresses= make([]string, 0)
        _balanceOf=make(map[string] int)
        productionCapacityOf=make(map[string] int)

            state=1
            roundMaxRate =100000
            roundMinRate =0
             minAllowedPrice = 0
             maxAllowedPrice = 100000

             clearingPrice =0
             balance =10000000
             productionCapacity =10000000
            balancingMarketClearingPrice=0

     return shim.Success([]byte("Market Resetting Successful"))
    }

    func (s *PoolMarket) getMarketClearingPrice (stub shim.ChaincodeStubInterface) sc.Response {
	   return shim.Success([]byte(strconv.Itoa(clearingPrice)))
    }

	func (s *PoolMarket) getBalance (stub shim.ChaincodeStubInterface) sc.Response {
		 userID, err=stub.GetCreator()
	   return shim.Success([]byte(strconv.Itoa(_balanceOf[string(userID)])))
   }

    func (s *PoolMarket) getConsumerDispatchedEnergy (stub shim.ChaincodeStubInterface) sc.Response {
		 userID, err=stub.GetCreator()
		 return shim.Success([]byte(strconv.Itoa(consumerEnergyDispatch[string(userID)])))
    }

    func (s *PoolMarket) getSupplierDispatchedEnergy (stub shim.ChaincodeStubInterface) sc.Response {
			 userID, err=stub.GetCreator()
		 return shim.Success([]byte(strconv.Itoa(supplierEnergyDispatch[string(userID)])))
	 }

    func (s *PoolMarket) getNumberOfSuppliers (stub shim.ChaincodeStubInterface) sc.Response {
		return shim.Success([]byte(strconv.Itoa(len(suppliers))))
    }

    func (s *PoolMarket) getNumberOfConsumers (stub shim.ChaincodeStubInterface) sc.Response {
			return shim.Success([]byte(strconv.Itoa(len(consumers))))
    }

   func (s *PoolMarket) getNumberOfSupplierWinners (stub shim.ChaincodeStubInterface) sc.Response {
  			return shim.Success([]byte(strconv.Itoa(len(supplierWinners))))
    }

    func (s *PoolMarket) getNumberOfSupplierLosers (stub shim.ChaincodeStubInterface) sc.Response {
	  			return shim.Success([]byte(strconv.Itoa(len(supplierLosers))))
    }

    func (s *PoolMarket) getNumberOfConsumerWinners (stub shim.ChaincodeStubInterface) sc.Response {
		return shim.Success([]byte(strconv.Itoa(len(consumerWinners))))

    }

    func (s *PoolMarket) getNumberOfConsumerLosers (stub shim.ChaincodeStubInterface) sc.Response {
	return shim.Success([]byte(strconv.Itoa(len(consumerLosers))))
    }

   func (s *PoolMarket) getState (stub shim.ChaincodeStubInterface) sc.Response {
	   	return shim.Success([]byte(strconv.Itoa(state)))
    }