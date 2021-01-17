package main.java;
import org.hyperledger.fabric.protos.common.Common.Block;
import org.hyperledger.fabric.sdk.BlockInfo;

import java.util.ArrayList;
import java.util.Comparator;

public class CustomBlock implements Comparator {
    public long receivedTime;
    public BlockInfo blockInfo;
    public ArrayList<String> transactions = new ArrayList<>();
    public CustomBlock (BlockInfo blockInfo, long receivedTime){
        this.blockInfo=blockInfo;
        this.receivedTime=receivedTime;
    }

    @Override
    public int compare(Object o, Object t1) {
        CustomBlock customBlock1= (CustomBlock) o;
        CustomBlock customBlock2= (CustomBlock) o;
        return (int) (customBlock1.blockInfo.getBlockNumber()-customBlock2.blockInfo.getBlockNumber());
    }
}
