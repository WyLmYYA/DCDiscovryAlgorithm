package HyDC;

import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.predicates.PartitionRefiner;

import java.util.List;

/**
 * @Author yoyuan
 * @Description:
 * @DateTime: 2021/12/17 16:50
 */
public class ClusterPairTreeNode {
    public List<ClusterPairTreeNode> children;

    public List<ClusterPair> clusterPair;

    public PartitionRefiner partitionRefiner;

    public ClusterPairTreeNode(PartitionRefiner partitionRefiner) {
        this.partitionRefiner = partitionRefiner;
    }
    public ClusterPairTreeNode(){}
}
