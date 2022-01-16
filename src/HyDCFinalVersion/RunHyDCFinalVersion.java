package HyDCFinalVersion;

import Hydra.de.hpi.naumann.dc.algorithms.hybrid.ResultCompletion;
import Hydra.de.hpi.naumann.dc.cover.PrefixMinimalCoverSearch;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.PartitionEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.ColumnAwareEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.SystematicLinearEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.helpers.IndexProvider;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.input.InputIterationException;
import Hydra.de.hpi.naumann.dc.input.RelationalInput;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import utils.TimeCal;
import utils.TimeCal2;
import utils.TimeCal3;

import javax.swing.plaf.synth.SynthUI;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RunHyDCFinalVersion {
    protected static int sampleRounds = 5;
    protected static double efficiencyThreshold = 0.005d;

    /**
     * */
//    public static Map<Predicate, Integer> predicateIntegerMap = new HashMap<>();
//    static Map<Predicate, Integer> predicateIntegerMap2 = new HashMap<>();

    public static IEvidenceSet samplingEvidence;
    public static void main(String[] args) throws IOException, InputIterationException {
        long l1 = System.currentTimeMillis();
        String file ="dataset//CLAIM.csv";
        int size = 10000;
         file ="dataset//Tax10k.csv";
         size = 10000;
        file ="dataset//uce.csv";
        size = 10;
        //CLAIM
        // 10000
        // mmcs and get dcs cost:15468
        //1292
        //dcs :868
        //minimize cost:367
        //valid time 13801
        //transitivity prune time 84
        //get child time 50
        //cal evidence for pair line count 18909
        //singel predicate valid count 8045769
        //double predicates valid  count 19922

         //-verbose:gc
        //-XX:+PrintGCDetails
        File datafile = new File(file);
        RelationalInput data = new RelationalInput(datafile);
        Input input = new Input(data,size);
        Input input11 = new Input(data,size);

        // Get predicates
        PredicateBuilder predicates = new PredicateBuilder(input, false, 0.3d);
//        PredicateBuilder predicates = new PredicateBuilder(new File("dataset/claim10kPre"), input);
        // Sampling
        IEvidenceSet sampleEvidenceSet = new SystematicLinearEvidenceSetBuilder(predicates,
                sampleRounds).buildEvidenceSet(input);
        HashEvidenceSet set = new HashEvidenceSet();
        sampleEvidenceSet.getSetOfPredicateSets().forEach(i -> set.add(i));

        samplingEvidence = sampleEvidenceSet;

        //get the sampling  evidence set
        IEvidenceSet fullSamplingEvidenceSet = new ColumnAwareEvidenceSetBuilder(predicates).buildEvidenceSet(set, input, efficiencyThreshold);

//        printPredicateToEvidence( fullSamplingEvidenceSet);
        // calculate selectivity and sort for predicate
//        calculatePredicate( set);

        // HyDC begin
        MMCSDC mmcsdc = new MMCSDC(predicates.getPredicates().size(), fullSamplingEvidenceSet, predicates, input);

        sampleEvidenceSet = null;
        fullSamplingEvidenceSet = null;
        System.out.println("mmcs and get dcs cost:" + (System.currentTimeMillis() - l1));

//        System.out.println("mmcs node " + mmcsdc.getCoverNodes());

        DenialConstraintSet denialConstraintSet = new DenialConstraintSet();
        denialConstraintSet = mmcsdc.denialConstraintSet;


        mmcsdc = null;

        System.out.println(denialConstraintSet.size());
        l1 = System.currentTimeMillis();
//        printPredicateToEvidence(denialConstraintSet );
        denialConstraintSet.minimize();
        System.out.println("dcs :" + denialConstraintSet.size());
        System.out.println("minimize cost:" + (System.currentTimeMillis() - l1));

        System.out.println("valid time " + TimeCal2.getTime(0));
        System.out.println("transitivity prune time " + TimeCal2.getTime(1));
        System.out.println("get child time " + TimeCal2.getTime(2));
        System.out.println("cal evidence for pair line count " + TimeCal2.getTime(3));
        System.out.println("singel predicate valid count " + TimeCal2.getTime(4));
        System.out.println("double predicates valid  count " + TimeCal2.getTime(5));

//        System.out.println(dcsApprox.size() + " == ? " + MMCSDC.cal);

//        List<Map.Entry<Predicate, Long>> list = new ArrayList<>(TimeCal3.time.entrySet());
//        Collections.sort(list, new Comparator<Map.Entry<Predicate, Long>>() {
//            @Override
//            public int compare(Map.Entry<Predicate, Long> o1, Map.Entry<Predicate, Long> o2) {
//                return o1.getValue().compareTo(o2.getValue());
//            }
//        });
//
//        for (Map.Entry<Predicate, Long> entry : list){
//            System.out.println(entry.getKey() + "  refine time: " + entry.getValue() +"  refine count: " + TimeCal3.getPreCalTime(entry.getKey()) +  "  dcs count :" + predicateIntegerMap2.get(entry.getKey()) + "  cover count :" + predicateIntegerMap.get(entry.getKey()));
//        }
//
//        for (Predicate predicate: predicates.getPredicates()){
//            System.out.println(predicate);
//        }
//        IndexProvider<Predicate> predicateIndexProvider = PredicateBitSet.indexProvider;
        //singel predicate valid count 1119207
        //double predicates valid  count 396280

        //singel predicate valid count 4666084
        //double predicates valid  count 1583770

        //singel predicate valid count 4533055
        //double predicates valid  count 1492748
    }

    private static void calculatePredicate(HashEvidenceSet set) {
        set.forEach(predicates1 -> {
            predicates1.forEach(predicate -> {
                predicate.coverSize ++;
            });
        });
    }

//    public static void printPredicateToEvidence( IEvidenceSet iEvidenceSet){
//
//        iEvidenceSet.forEach(predicates1 -> {
//            predicates1.forEach(predicate -> {
//                if (predicateIntegerMap.containsKey(predicate)){
//                    predicateIntegerMap.put(predicate, predicateIntegerMap.get(predicate) + 1);
//                }else{
//                    predicateIntegerMap.put(predicate, 1);
//                }
//            });
//        });
//    }
//    public static void printPredicateToEvidence(DenialConstraintSet denialConstraintSet){
//
//        denialConstraintSet.forEach(predicates1 -> {
//            predicates1.getPredicateSet().forEach(predicate -> {
//                if (predicateIntegerMap2.containsKey(predicate)){
//                    predicateIntegerMap2.put(predicate, predicateIntegerMap2.get(predicate) + 1);
//                }else{
//                    predicateIntegerMap2.put(predicate, 1);
//                }
//            });
//        });
//    }


}
