package test;

import HyDC.MMCSHyDC;
import HyDCV3.HyDC;
import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.de.hpi.naumann.dc.algorithms.hybrid.Hydra;
import Hydra.de.hpi.naumann.dc.algorithms.hybrid.ResultCompletion;
import Hydra.de.hpi.naumann.dc.cover.PrefixMinimalCoverSearch;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.ColumnAwareEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.SystematicLinearEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.helpers.IndexProvider;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.input.InputIterationException;
import Hydra.de.hpi.naumann.dc.input.RelationalInput;
import Hydra.de.hpi.naumann.dc.predicates.Operator;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import utils.TimeCal;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;

public class RunHyDCV3 {

    //sampling efficiency : growth/total
    protected static int sampleRounds = 5;
    protected static double efficiencyThreshold = 0.005d;


    public static void main(String[] args) throws IOException, InputIterationException {
        //Initial: get predicates

        String file ="dataset//Tax10k.csv";
        int size = 70;
        File datafile = new File(file);


        // Get meta data
        RelationalInput data = new RelationalInput(datafile);
        Input input = new Input(data,size);

        // Get predicates
        PredicateBuilder predicates = new PredicateBuilder(input, false, 0.3d);

//        Hydra hydra = new Hydra();
//        DenialConstraintSet dcs = hydra.run(input, predicates, 0 );

        long l1 = System.currentTimeMillis();
        // Sampling
        IEvidenceSet sampleEvidenceSet = new SystematicLinearEvidenceSetBuilder(predicates,
                sampleRounds).buildEvidenceSet(input);
        HashEvidenceSet set = new HashEvidenceSet();
        sampleEvidenceSet.getSetOfPredicateSets().forEach(i -> set.add(i));

        //get the sampling  evidence set
        IEvidenceSet fullSamplingEvidenceSet = new ColumnAwareEvidenceSetBuilder(predicates).buildEvidenceSet(set, input, efficiencyThreshold);

        new HyDC().run(predicates, set, fullSamplingEvidenceSet, input);

        System.out.println("hydc cost " + (System.currentTimeMillis() - l1));

    }


}
