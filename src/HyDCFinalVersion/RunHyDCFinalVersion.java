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
    protected static int sampleRounds = 20;
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
         size = 1000;
        file =args[0];
        size = Integer.parseInt(args[1]);

         //-verbose:gc
        //-XX:+PrintGCDetails
        File datafile = new File(file);
        RelationalInput data = new RelationalInput(datafile);
        Input input = new Input(data,size);
        Input input11 = new Input(data,size);

        // Get predicates
        PredicateBuilder predicates;

        if (args.length == 3){
            predicates = new PredicateBuilder(new File(args[2]), input);
        }else {
            predicates = new PredicateBuilder(input, false, 0.3d);
        }
        //
        // Sampling
        IEvidenceSet sampleEvidenceSet = new SystematicLinearEvidenceSetBuilder(predicates,
                sampleRounds).buildEvidenceSet(input);
        HashEvidenceSet set = new HashEvidenceSet();
        sampleEvidenceSet.getSetOfPredicateSets().forEach(i -> set.add(i));

        samplingEvidence = sampleEvidenceSet;

        //get the sampling  evidence set
        IEvidenceSet fullSamplingEvidenceSet = set;
//        IEvidenceSet fullSamplingEvidenceSet = new ColumnAwareEvidenceSetBuilder(predicates).buildEvidenceSet(set, input, efficiencyThreshold);

//        printPredicateToEvidence( fullSamplingEvidenceSet);
        // calculate selectivity and sort for predicate
        calculatePredicate( set);

        // HyDC begin
        MMCSDC mmcsdc = new MMCSDC(predicates.getPredicates().size(), fullSamplingEvidenceSet, predicates, input);

        System.out.println("mmcs and get dcs cost:" + (System.currentTimeMillis() - l1));

//        System.out.println("mmcs node " + mmcsdc.getCoverNodes());

        DenialConstraintSet denialConstraintSet = new DenialConstraintSet();
        denialConstraintSet = mmcsdc.denialConstraintSet;


        System.out.println(denialConstraintSet.size());
        l1 = System.currentTimeMillis();
        denialConstraintSet.minimize();
        System.out.println("dcs :" + denialConstraintSet.size());
        System.out.println("minimize cost:" + (System.currentTimeMillis() - l1));

        System.out.println("valid time " + TimeCal2.getTime(0));
        System.out.println("transitivity prune time " + TimeCal2.getTime(1));
        System.out.println("get child time " + TimeCal2.getTime(2));
        System.out.println("cal evidence for pair line count " + TimeCal2.getTime(3));
        System.out.println("singel predicate valid count " + TimeCal2.getTime(4));
        System.out.println("double predicates valid  count " + TimeCal2.getTime(5));
        System.out.println("get cluster pair time " + TimeCal2.getTime(6));

    }

    private static void calculatePredicate(HashEvidenceSet set) {
        set.forEach(predicates1 -> {
            predicates1.forEach(predicate -> {
                predicate.coverSize ++;
            });
        });
    }



}
