package test;

import HyDC.MMCSHyDC;
import Hydra.ch.javasoft.bitset.IBitSet;
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

import javax.swing.plaf.synth.SynthUI;
import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;

public class RunHyDC {

    //sampling efficiency : growth/total
    protected static double efficiencyThreshold = 0.005d;

    public static void main(String[] args) throws IOException, InputIterationException {
        //Initial: get predicates

        String line ="dataset//Tax10k.csv";
        String sizeLine ="50";
        //        line ="dataset//Test.csv";
        //        sizeLine ="7";

        //             hydra   2021.12.16   baseline
        // 20    dc 7304  t1:9s    t2: 8s(single)   t3:  8s(combine)
        // 30    dc 10770 t1:15s   t2: 15s(single)  t3:  19s(combine)
        // 40    dc 14109 t1:21s   t2: 31s(single)  t3:  34s(combine)
        // 50    dc 26396 t1:34s   t2: 100s(single) t3:  116(combine)
        //             To be continue...

         int sampleRounds = 5;

        int size=Integer.parseInt(sizeLine);
        File datafile = new File(line);
        char[] c = new char[2];
        int a = c.length;

        long beg = System.currentTimeMillis();
        RelationalInput data = new RelationalInput(datafile);
        Input input = new Input(data,size);

        PredicateBuilder predicates = new PredicateBuilder(input, false, 0.3d);

        // create indexProvider for predicates in order by = <> and needCombine

        Set<Predicate> equal = new HashSet<>();
        Set<Predicate> unEqual = new HashSet<>();
        Set<Predicate> needConbine = new HashSet<>();
        Set<Predicate> noConbine = new HashSet<>();
        predicates.getPredicates().forEach(predicate -> {
            if (predicate.getOperator() == Operator.EQUAL)equal.add(predicate);

            else if (predicate.getOperator() == Operator.UNEQUAL)unEqual.add(predicate);

            else if (predicate.needCombine())needConbine.add(predicate);

            else noConbine.add(predicate);
        });
        //TODO: next we can order every set. so we can change to List
        indexAddSet(unEqual, indexProvider);
        indexAddSet(equal, indexProvider);
        indexAddSet(noConbine, indexProvider);
        indexAddSet(needConbine, indexProvider);

        // Sampling
        for (int i = 0; i < 68; ++i){
            System.out.println(indexProvider.getObject(i));
        }
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
    public  static void indexAddSet(Set<Predicate> set, IndexProvider<Predicate> indexProvider){
        for (Predicate predicate : set) indexProvider.getIndex(predicate);
    }

}
