package test;

import HyDC.MMCSHyDC;
import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.ColumnAwareEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.SystematicLinearEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.input.InputIterationException;
import Hydra.de.hpi.naumann.dc.input.RelationalInput;
import Hydra.de.hpi.naumann.dc.predicates.Operator;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;

import javax.swing.plaf.synth.SynthUI;
import java.io.File;
import java.io.IOException;

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;

public class RunHyDC {
    protected static int sampleRounds = 20;
    //sampling efficiency : growth/total
    protected static double efficiencyThreshold = 0.005d;

    public static void main(String[] args) throws IOException, InputIterationException {
        //Initial: get predicates
        String line ="dataset//Tax10k.csv";
        String sizeLine ="30";
        // 30 dc 10770
//        line ="dataset//Test.csv";
//        sizeLine ="7";

        int size=Integer.parseInt(sizeLine);
        File datafile = new File(line);
        char[] c = new char[2];
        int a = c.length;

        long beg = System.currentTimeMillis();
        RelationalInput data = new RelationalInput(datafile);
        Input input = new Input(data,size);
        PredicateBuilder predicates = new PredicateBuilder(input, false, 0.3d);

        // Sampling
        IEvidenceSet sampleEvidenceSet = new SystematicLinearEvidenceSetBuilder(predicates,
                sampleRounds).buildEvidenceSet(input);
        HashEvidenceSet set = new HashEvidenceSet();
        sampleEvidenceSet.getSetOfPredicateSets().forEach(i -> set.add(i));
        //get the full evidence set
        IEvidenceSet fullSamplingEvidenceSet = new ColumnAwareEvidenceSetBuilder(predicates).buildEvidenceSet(set, input, efficiencyThreshold);

        // HyDC MMCS begin
        int numOfNeedCombinePredicate =  (int)predicates.getPredicates().stream().filter(predicate -> predicate.needCombine() == true).count();
        MMCSHyDC mmcsHyDC = new MMCSHyDC(numOfNeedCombinePredicate, input, predicates, (HashEvidenceSet) fullSamplingEvidenceSet);

        System.out.println("mmcs end: " + (System.currentTimeMillis() - beg));
        // Transform cover to dc
        DenialConstraintSet denialConstraintSet = new DenialConstraintSet();

        mmcsHyDC.getCoverNodes().forEach(node -> {
            IBitSet bitSet = node.getElement();
            PredicateBitSet inverse = new PredicateBitSet();
            for (int next = bitSet.nextSetBit(0); next >= 0; next = bitSet.nextSetBit(next + 1)){
                Predicate predicate = indexProvider.getObject(next);
                inverse.add(predicate.getInverse());
            }
            denialConstraintSet.add(new DenialConstraint(inverse));
        });
        denialConstraintSet.minimize();

        System.out.println("algorithm end: " + (System.currentTimeMillis() - beg));
        System.out.println(denialConstraintSet.size());


    }
}
