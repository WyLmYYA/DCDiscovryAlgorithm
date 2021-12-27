package HyDCFinalVersion;

import Hydra.de.hpi.naumann.dc.algorithms.hybrid.ResultCompletion;
import Hydra.de.hpi.naumann.dc.cover.PrefixMinimalCoverSearch;
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

import javax.swing.plaf.synth.SynthUI;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RunHyDCFinalVersion {
    protected static int sampleRounds = 20;
    protected static double efficiencyThreshold = 0.005d;
    public static void main(String[] args) throws IOException, InputIterationException {
        long l1 = System.currentTimeMillis();
        String file ="dataset//Tax10k.csv";
        int size = 200;
        // 40    dc 14109 t1:21s            t2:
        // 50    dc 26396 t1:34s
        // 100   dc 34489 t1:57s
        // 200   dc 41150 t1:100s             51.4(37s) + 23 s
        // 1000  dc 84150 t1:211s inversion 52s mini 55s comp 25s inver 25s min 51s   700s
        // 10000 dc 85360 t1:525s
        File datafile = new File(file);
        RelationalInput data = new RelationalInput(datafile);
        Input input = new Input(data,size);

        // Get predicates
        PredicateBuilder predicates = new PredicateBuilder(input, false, 0.3d);
        // Sampling
        IEvidenceSet sampleEvidenceSet = new SystematicLinearEvidenceSetBuilder(predicates,
                sampleRounds).buildEvidenceSet(input);
        HashEvidenceSet set = new HashEvidenceSet();
        sampleEvidenceSet.getSetOfPredicateSets().forEach(i -> set.add(i));

        //get the sampling  evidence set
        IEvidenceSet fullSamplingEvidenceSet = new ColumnAwareEvidenceSetBuilder(predicates).buildEvidenceSet(set, input, efficiencyThreshold);

        // calculate selectivity and sort for predicate
//        calculateAndSortPredicate(set);

        // HyDC begin
//        long l11 = System.currentTimeMillis();
//        DenialConstraintSet dcsApprox = new PrefixMinimalCoverSearch(predicates).getDenialConstraints(fullSamplingEvidenceSet);
//
//        dcsApprox.minimize();
//
//        //complete之后得到set 利用set得到DC
//        IEvidenceSet result = new ResultCompletion(input, predicates).complete(dcsApprox, sampleEvidenceSet,
//                fullSamplingEvidenceSet);
//
//        System.out.println("get full evidence : sss : " + (System.currentTimeMillis() - l11));
        MMCSDC mmcsdc = new MMCSDC(predicates.getPredicates().size(), fullSamplingEvidenceSet, predicates, input);



        DenialConstraintSet denialConstraintSet = new DenialConstraintSet();
        System.out.println(mmcsdc.getCoverNodes().size());
        mmcsdc.getCoverNodes().forEach(mmcsNode -> {
            denialConstraintSet.add(mmcsNode.getDenialConstraint());
        });

        System.out.println("mmcs and get dcs cost:" + (System.currentTimeMillis() - l1));

        l1 = System.currentTimeMillis();
        denialConstraintSet.minimize();
        System.out.println("dcs :" + denialConstraintSet.size());
        System.out.println("minimize cost:" + (System.currentTimeMillis() - l1));

        System.out.println("valid time " + TimeCal.getTime(0));

    }

    private static void calculateAndSortPredicate( HashEvidenceSet sampleEvidenceSet) {
        Set<Predicate> predicates = new HashSet<>();
        sampleEvidenceSet.getSetOfPredicateSets().forEach(predicates1 -> {
            predicates1.forEach(predicate -> {
                predicates.add(predicate);
            });
        });
        Map<Integer, Integer> predicateMapSelectivity = new ConcurrentHashMap<>();
        List<Predicate> sortedPredicates = new ArrayList<>();
        IndexProvider<Predicate> sortedIndexProvider = new IndexProvider<>();
        for (Predicate predicate : predicates){
            AtomicInteger select = new AtomicInteger();
            sortedPredicates.add(predicate);
            sampleEvidenceSet.forEach(predicates1 -> {
                if (predicates1.containsPredicate(predicate)){
                    select.addAndGet(1);
                }
            });
            predicateMapSelectivity.put(PredicateBitSet.indexProvider.getIndex(predicate), select.intValue());
        }
        Collections.sort(sortedPredicates, new Comparator<Predicate>() {
            @Override
            public int compare(Predicate o1, Predicate o2) {
                int sel1 = predicateMapSelectivity.get(PredicateBitSet.indexProvider.getIndex(o1));
                int sel2 = predicateMapSelectivity.get(PredicateBitSet.indexProvider.getIndex(o2));
                return sel1 - sel2;
            }
        });
        for (Predicate predicate : sortedPredicates){
            sortedIndexProvider.getIndex(predicate);
        }
        for (Predicate predicate : predicates){
            sortedIndexProvider.getIndex(predicate);
        }

        // sampling evidence need to rehash from indexiProvideer to sortedProvider
        sampleEvidenceSet.getSetOfPredicateSets().forEach(predicates1 -> {
            PredicateBitSet predicates2 = new PredicateBitSet();
            predicates1.forEach(predicate -> {
                predicates2.getBitset().set(sortedIndexProvider.getIndex(predicate));
            });
            predicates1 = predicates2;
        });
        PredicateBitSet.indexProvider = sortedIndexProvider;
    }
}
