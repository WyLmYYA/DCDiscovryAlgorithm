package HyDCFinalVersion;

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
        int size = 10;
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
