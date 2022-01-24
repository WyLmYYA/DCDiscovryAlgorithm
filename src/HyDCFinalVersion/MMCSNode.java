package HyDCFinalVersion;

import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.ch.javasoft.bitset.LongBitSet;
import Hydra.ch.javasoft.bitset.search.NTreeSearch;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.PartitionEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.*;
import Hydra.de.hpi.naumann.dc.predicates.Operator;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.PredicatePair;
import Hydra.de.hpi.naumann.dc.predicates.sets.Closure;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateSetFactory;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import utils.TimeCal;
import utils.TimeCal2;
import utils.TimeCal3;

import java.util.*;

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;

public class MMCSNode {

    private int numberOfPredicates;
    /**
     *  the set of predicates which can cover the evidence set
    */
    public IBitSet element;

    IBitSet candidatePredicates;

    public HashEvidenceSet uncoverEvidenceSet;

    public List<List<PredicateBitSet>> crit;

    public IBitSet getElement() {
        return element;
    }

    /**
     * params added for hyDC
     */

//    public List<MMCSNode> nodesInPath = new ArrayList<>();
    private MMCSNode parentNode;

    public Predicate curPred;

    public boolean needRefine = true;



    public MMCSNode(int numberOfPredicates, IEvidenceSet evidenceToCover, int lineCount) {

        this.numberOfPredicates = numberOfPredicates;

        element = new LongBitSet();

        uncoverEvidenceSet = (HashEvidenceSet) evidenceToCover;

        candidatePredicates = MMCSDC.candidatePredicates;

//        clusterPairs.add(StrippedPartition.getFullParition(lineCount));

        crit = new ArrayList<>(numberOfPredicates);
        for (int i = 0; i < numberOfPredicates; ++i){
            crit.add(new ArrayList<>());
        }
//        nodesInPath.add(this);

    }

    public MMCSNode(int numberOfPredicates) {

        this.numberOfPredicates = numberOfPredicates;

    }

    public boolean canCover() {
        return uncoverEvidenceSet.isEmpty();
    }

    public PredicateBitSet getNextEvidence() {
        Comparator<PredicateBitSet> cmp = Comparator.comparing(predicates -> predicates.getBitset().getAnd(candidatePredicates));

        return Collections.min(uncoverEvidenceSet.getSetOfPredicateSets(), cmp);

    }

    public MMCSNode getChildNode(int predicateAdded, IBitSet nextCandidatePredicates) {

        MMCSNode childNode = new MMCSNode(numberOfPredicates);

        childNode.cloneContext(nextCandidatePredicates, this);

        childNode.updateContextFromParent(predicateAdded, this);

        return childNode;

    }

    private void updateContextFromParent(int predicateAdded, MMCSNode node) {

        //TODO: we can change TroveEvidenceSet to see which one is more suitable
        uncoverEvidenceSet = new HashEvidenceSet();

        /**
         *  after add new Predicates, some uncoverEvidenceSet can be removed, so we don't need add them to childNode
         *  and we should update crit meanwhile
        */

        // parent uncover evidenceSet has updated before,
        // in other words parent uncover has added newEvidence come from before nodes.
        // so we can update directly
        for (PredicateBitSet predicates : node.uncoverEvidenceSet.getSetOfPredicateSets()) {
            if(predicates.getBitset().get(predicateAdded)) crit.get(predicateAdded).add(predicates);
            else uncoverEvidenceSet.add(predicates);
        }
//        node.uncoverEvidenceSet.getSetOfPredicateSets().forEach(predicates -> {
//            if(predicates.getBitset().get(predicateAdded)) crit.get(predicateAdded).add(predicates);
//            else uncoverEvidenceSet.add(predicates);
//        });

        /**
         *  update crit in terms of  the EvidenceSet which is covered by predicateAdded
        */
        for (int next = element.nextSetBit(0); next >= 0; next = element.nextSetBit(next + 1))
            crit.get(next).removeIf(predicates -> predicates.getBitset().get(predicateAdded));

        /**
         *  update element
        */
        element.set(predicateAdded, true);

        /**
         *  judge current node predicate, and update clusterPairs
         */
        Predicate predicate = PredicateBitSet.indexProvider.getObject(predicateAdded);

        curPred = predicate;

        needRefine = parentNode.needRefine;

//        nodesInPath.add(this);



    }
    public void refinePS(Predicate predicate, IEJoin ieJoin){

        long l1 = System.currentTimeMillis();
        // refine by single predicate
        List<ClusterPair> newResult = new ArrayList<>();
        for (ClusterPair clusterPair : MMCSDC.clusterPairs) {
            TimeCal2.add(1,4);
            clusterPair.refinePsPublic(predicate.getInverse(), ieJoin, newResult);
        }
//        MMCSDC.clusterPairs.forEach(clusterPair -> {
//            TimeCal2.add(1,4);
//            clusterPair.refinePsPublic(predicate.getInverse(), ieJoin, newResult);
//        });
        MMCSDC.clusterPairs = newResult;
        TimeCal3.addPreCal(predicate, 1);
        TimeCal3.add(predicate, System.currentTimeMillis() - l1);
        TimeCal2.add((System.currentTimeMillis() - l1), 0);
    }


    public void refineBySelectivity(CPTree cpTree){
        List<Predicate> needCombination = new ArrayList<>();
        List<Predicate> singleNode = new ArrayList<>();
        MMCSNode tmp = this;

        // get all nodes in path
        while (tmp.curPred != null && tmp != null){
            if (tmp.curPred.needCombine())
                needCombination.add(tmp.curPred);
            else
                singleNode.add(tmp.curPred);
            tmp = tmp.parentNode;
        }

        // sort
        long l1 = System.currentTimeMillis();
        sortPredicate(singleNode);
        // find subset
        MMCSDC.clusterPairs = cpTree.add(singleNode, 0);;
        TimeCal2.add((System.currentTimeMillis() - l1), 6);

        refineCombinationEnd(needCombination);


    }

    public static long pairTime = 0;
    private void refineCombinationEnd(List<Predicate> predicates){
//        long l2 = System.currentTimeMillis();
        Multiset<PredicatePair> paircountDC = HashMultiset.create();
        boolean[] v = new boolean[predicates.size()];
        for (int i = 0; i < predicates.size(); ++i) {
            if (v[i]) continue;
            Predicate p1 = predicates.get(i);
            if (StrippedPartition.isPairSupported(p1)){
                boolean pair = false;
                for (int j = i + 1; j < predicates.size(); ++j) {
                    if (v[j])continue;
                    Predicate p2 = predicates.get(j);
                    if (!p1.equals(p2) && StrippedPartition.isPairSupported(p2)) {
                        paircountDC.add(new PredicatePair(p1, p2));
//                        v[i] = true;
//                        v[j] = true;
                    }
                }
            }
        }
        //mmcs and get dcs cost:56255
        //130930
        //dcs :84150
        //minimize cost:35012
        //valid time 9376
        //transitivity prune time 12643
        //get child time 77
        //cal evidence for pair line count 14401
        //singel predicate valid count 12195793
        //double predicates valid  count 30077
        //get cluster pair time 3524

        for (int i = 0; i < predicates.size(); ++i){
            if (!v[i]){
                refinePS(predicates.get(i), MMCSDC.ieJoin);
            }
        }
        //D:\Java\jdk1.8.0_152\bin\java.exe "-javaagent:D:\IntelliJ IDEA 2020.3\lib\idea_rt.jar=55454:D:\IntelliJ IDEA 2020.3\bin" -Dfile.encoding=UTF-8 -classpath D:\Java\jdk1.8.0_152\jre\lib\charsets.jar;D:\Java\jdk1.8.0_152\jre\lib\deploy.jar;D:\Java\jdk1.8.0_152\jre\lib\ext\access-bridge-64.jar;D:\Java\jdk1.8.0_152\jre\lib\ext\cldrdata.jar;D:\Java\jdk1.8.0_152\jre\lib\ext\dnsns.jar;D:\Java\jdk1.8.0_152\jre\lib\ext\jaccess.jar;D:\Java\jdk1.8.0_152\jre\lib\ext\jfxrt.jar;D:\Java\jdk1.8.0_152\jre\lib\ext\localedata.jar;D:\Java\jdk1.8.0_152\jre\lib\ext\nashorn.jar;D:\Java\jdk1.8.0_152\jre\lib\ext\sunec.jar;D:\Java\jdk1.8.0_152\jre\lib\ext\sunjce_provider.jar;D:\Java\jdk1.8.0_152\jre\lib\ext\sunmscapi.jar;D:\Java\jdk1.8.0_152\jre\lib\ext\sunpkcs11.jar;D:\Java\jdk1.8.0_152\jre\lib\ext\zipfs.jar;D:\Java\jdk1.8.0_152\jre\lib\javaws.jar;D:\Java\jdk1.8.0_152\jre\lib\jce.jar;D:\Java\jdk1.8.0_152\jre\lib\jfr.jar;D:\Java\jdk1.8.0_152\jre\lib\jfxswt.jar;D:\Java\jdk1.8.0_152\jre\lib\jsse.jar;D:\Java\jdk1.8.0_152\jre\lib\management-agent.jar;D:\Java\jdk1.8.0_152\jre\lib\plugin.jar;D:\Java\jdk1.8.0_152\jre\lib\resources.jar;D:\Java\jdk1.8.0_152\jre\lib\rt.jar;D:\Desktop\YYA\DCDiscovryAlgorithm2\target\classes;C:\Users\86158\.m2\repository\com\koloboke\koloboke-api-jdk8\1.0.0\koloboke-api-jdk8-1.0.0.jar;C:\Users\86158\.m2\repository\com\koloboke\koloboke-impl-jdk8\1.0.0\koloboke-impl-jdk8-1.0.0.jar;C:\Users\86158\.m2\repository\com\koloboke\koloboke-impl-common-jdk8\1.0.0\koloboke-impl-common-jdk8-1.0.0.jar;C:\Users\86158\.m2\repository\org\apache\lucene\lucene-core\4.5.1\lucene-core-4.5.1.jar;C:\Users\86158\.m2\repository\commons-net\commons-net\3.1\commons-net-3.1.jar;C:\Users\86158\.m2\repository\com\google\guava\guava\17.0\guava-17.0.jar;C:\Users\86158\.m2\repository\net\sf\trove4j\trove4j\3.0.3\trove4j-3.0.3.jar;D:\Desktop\YYA\DCDiscovryAlgorithm2\java-sizeof-0.0.5.jar;D:\Desktop\YYA\DCDiscovryAlgorithm2\guava-19.0.jar;D:\Desktop\YYA\DCDiscovryAlgorithm2\commons-net-3.3.jar;D:\Desktop\YYA\DCDiscovryAlgorithm2\primitive-1.3.jar;D:\Desktop\YYA\DCDiscovryAlgorithm2\trove-3.1.jar;D:\Desktop\YYA\DCDiscovryAlgorithm2\guava-19.0-javadoc.jar;D:\Desktop\YYA\DCDiscovryAlgorithm2\guava-19.0-sources.jar HyDCFinalVersion.RunHyDCFinalVersion
        //mmcs and get dcs cost:64730
        //130709
        //dcs :84150
        //minimize cost:31305
        //valid time 24954
        //transitivity prune time 11087
        //get child time 108
        //cal evidence for pair line count 13985
        //singel predicate valid count 1715778
        //double predicates valid  count 10656081
        //get cluster pair time 1993
        //
        //Process finished with exit code 0
//        pairTime += System.currentTimeMillis() - l2;
        for (PredicatePair predicatePair : paircountDC){
            List<ClusterPair> newResult = new ArrayList<>();
            Predicate p1 = predicatePair.getP1();
            Predicate p2 = predicatePair.getP2();
            long l1 = System.currentTimeMillis();
            for (ClusterPair clusterPair : MMCSDC.clusterPairs) {
                TimeCal2.add(1,5);
                clusterPair.refinePPPublic(new PredicatePair(p1.getInverse(), p2.getInverse()), MMCSDC.ieJoin, clusterPair1 -> newResult.add(clusterPair1));
            }
//            MMCSDC.clusterPairs.forEach(clusterPair -> {
//                TimeCal2.add(1,5);
//                clusterPair.refinePPPublic(new PredicatePair(p1.getInverse(), p2.getInverse()), MMCSDC.ieJoin, clusterPair1 -> newResult.add(clusterPair1));
//            });
            MMCSDC.clusterPairs = newResult;

            TimeCal3.addPreCal(p1, 1);
            TimeCal3.addPreCal(p2, 1);
            TimeCal3.add(p1, System.currentTimeMillis() - l1);
            TimeCal3.add(p2, System.currentTimeMillis() - l1);
            TimeCal2.add((System.currentTimeMillis() - l1), 0);
        }
    }

    private void cloneContext(IBitSet nextCandidatePredicates, MMCSNode parentNode) {
        element = parentNode.element.clone();

        candidatePredicates = nextCandidatePredicates.clone();

        crit = new ArrayList<>(numberOfPredicates);

//        parentNode.nodesInPath.forEach(mmcsNode -> nodesInPath.add(mmcsNode));
        this.parentNode = parentNode;
        for (int i = 0; i < numberOfPredicates; ++i){
            crit.add(new ArrayList<>(parentNode.crit.get(i)));
        }

    }

    /**
     *  Judge current node is global minimal or not by crit
    */
    public boolean isGlobalMinimal() {
        for(int next = element.nextSetBit(0); next >= 0; next = element.nextSetBit(next+1))
            if(crit.get(next).isEmpty()) return false;
        return true;
    }

    public HashEvidenceSet getAddedEvidenceSet(){
        HashEvidenceSet newEvi = new HashEvidenceSet();


        long l2 = System.currentTimeMillis();

        if (MMCSDC.partitionEvidenceSetBuilder == null){
            MMCSDC.partitionEvidenceSetBuilder = new PartitionEvidenceSetBuilder(MMCSDC.predicates, MMCSDC.input.getInts());
        }
//        Set<String> calP = new HashSet<>();
        for (ClusterPair clusterPair : MMCSDC.clusterPairs){
            MMCSDC.partitionEvidenceSetBuilder.addEvidencesForHyDC(clusterPair, newEvi);
        }
        TimeCal2.add((System.currentTimeMillis() - l2), 2);


        Iterator iterable = newEvi.getSetOfPredicateSets().iterator();
        while (iterable.hasNext()){
            PredicateBitSet predicates1 = (PredicateBitSet) iterable.next();
            IBitSet tmp = predicates1.getBitset().getAnd(element);
            if (tmp.cardinality() != 0){
                iterable.remove();
//                if (tmp.cardinality() == 1){
//                    crit.get(tmp.nextSetBit(0)).add(new PredicateBitSet(tmp));
//                }
            }
        }

        uncoverEvidenceSet.add(newEvi);
        MMCSDC.clusterPairs = null;
        needRefine = false;
        return newEvi;
    }
    public boolean isValidResult(){
        // there may be {12} : {12, 12}, so need to add one step to judge
        if (MMCSDC.clusterPairs == null)return true;
        for (ClusterPair clusterPair : MMCSDC.clusterPairs){
//            Iterator<LinePair> iter = clusterPair.getLinePairIterator();
//            while (iter.hasNext()) {
//                LinePair lPair = iter.next();
//                int i = lPair.getLine1();
//                int j = lPair.getLine2();
//                if (i != j) return false;
//            }
            if(clusterPair.containsLinePair()){
                return false;
            }
        }
        return true;
    }



    public DenialConstraint getDenialConstraint() {
        PredicateBitSet inverse = new PredicateBitSet();
        for (int next = element.nextSetBit(0); next >= 0; next = element.nextSetBit(next + 1)) {
            Predicate predicate = indexProvider.getObject(next); //1
            inverse.add(predicate.getInverse());

        }
        return new DenialConstraint(inverse);
    }

    public DenialConstraint getDenialConstraint(int addPredicate) {
        PredicateBitSet inverse = new PredicateBitSet();
        for (int next = element.nextSetBit(0); next >= 0; next = element.nextSetBit(next + 1)) {
            Predicate predicate = indexProvider.getObject(next); //1
            inverse.add(predicate.getInverse());
        }
        inverse.add(indexProvider.getObject(addPredicate));
        return new DenialConstraint(inverse);
    }
    private void sortPredicate(List<Predicate> list){
        Collections.sort(list, new Comparator<Predicate>() {
            @Override
            public int compare(Predicate o1, Predicate o2) {
                return o2.coverSize - o1.coverSize;
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MMCSNode mmcsNode = (MMCSNode) o;
        return numberOfPredicates == mmcsNode.numberOfPredicates && Objects.equals(element, mmcsNode.element) && Objects.equals(candidatePredicates, mmcsNode.candidatePredicates) && Objects.equals(uncoverEvidenceSet, mmcsNode.uncoverEvidenceSet) && Objects.equals(crit, mmcsNode.crit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numberOfPredicates, element, candidatePredicates, uncoverEvidenceSet, crit);
    }
}
