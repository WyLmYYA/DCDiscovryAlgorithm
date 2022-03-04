package HyDCFinalVersion;

import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.SystematicLinearEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.input.InputIterationException;
import Hydra.de.hpi.naumann.dc.input.RelationalInput;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import utils.TimeCal2;

import java.io.File;
import java.io.IOException;

public class RunHyDCFinalVersion {
    protected static int sampleRounds;
    protected static double efficiencyThreshold = 0.005d;


    public static IEvidenceSet samplingEvidence;
    public static void main(String[] args) throws IOException, InputIterationException {
        long l1 = System.currentTimeMillis();
        String file ="dataset//CLAIM_2.csv";
        int size = 30000;
         file ="dataset//Tax10k.csv";
         size = 10000;
         sampleRounds = (int) (size * 0.01);
//        file =args[0];
//        size = Integer.parseInt(args[1]);

         //-verbose:gc
        //-XX:+PrintGCDetails
        File datafile = new File(file);
        RelationalInput data = new RelationalInput(datafile);
        Input input = new Input(data,size);

        // Get predicates
        PredicateBuilder predicates;

        // build predicates
        long l2 = System.currentTimeMillis();
        if (args.length == 3){
            predicates = new PredicateBuilder(new File(args[2]), input);
        }else {
            predicates = new PredicateBuilder(input, false, 0.3d);
        }
        System.out.println("predicates num:" + predicates.getPredicates().size());
        System.out.println("build predicates cost:" + (System.currentTimeMillis() - l2) + "ms");

        // Sampling
        l2 = System.currentTimeMillis();
        IEvidenceSet sampleEvidenceSet = new SystematicLinearEvidenceSetBuilder(predicates,
                sampleRounds).buildEvidenceSet(input);
        HashEvidenceSet set = new HashEvidenceSet();
        sampleEvidenceSet.getSetOfPredicateSets().forEach(i -> set.add(i));

        samplingEvidence = sampleEvidenceSet;

        //get the sampling  evidence set
        IEvidenceSet fullSamplingEvidenceSet = set;
//        IEvidenceSet fullSamplingEvidenceSet = new ColumnAwareEvidenceSetBuilder(predicates).buildEvidenceSet(set, input, efficiencyThreshold);
        System.out.println("sampling and focused sampling cost:" + (System.currentTimeMillis() - l2) + "ms");


        // calculate selectivity and sort for predicate
        l2 = System.currentTimeMillis();
        calculatePredicate((HashEvidenceSet) fullSamplingEvidenceSet);
        System.out.println("calculate selectivity and sort cost:" + (System.currentTimeMillis() - l2) + "ms");

        // HyDC begin
        System.out.println("HyDC begin....");
        MMCSDC mmcsdc = new MMCSDC(predicates.getPredicates().size(), fullSamplingEvidenceSet, predicates, input);

        System.out.println("mmcs and get dcs cost:" + (System.currentTimeMillis() - l1));


        DenialConstraintSet denialConstraintSet = new DenialConstraintSet();
        denialConstraintSet = mmcsdc.denialConstraintSet;


        System.out.println(denialConstraintSet.size());
        l1 = System.currentTimeMillis();
        denialConstraintSet.minimize();
        System.out.println("dc result :" + denialConstraintSet.size());
        System.out.println("minimize cost:" + (System.currentTimeMillis() - l1));

        System.out.println("valid and get clusterPair time : " + MMCSDC.validTime);

        System.out.println("average shared times: " + (MMCSNode.sharedNum / MMCSNode.allValid));

    }

    private static void calculatePredicate(HashEvidenceSet set) {
        set.forEach(predicates1 -> {
            predicates1.forEach(predicate -> {
                predicate.coverSize ++;
            });
        });
    }



}
