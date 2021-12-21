package test;

import HyDCNoSort.MMCSHyDC;
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
import utils.TimeCal;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;

public class RunHyDCV1 {

    //sampling efficiency : growth/total
    protected static double efficiencyThreshold = 0.005d;

    public static void main(String[] args) throws IOException, InputIterationException {
        //Initial: get predicates

        String line ="dataset//Tax10k.csv";
        String sizeLine ="100";
        //        line ="dataset//Test.csv";
        //        sizeLine ="7";
        //                Hydra HyDC(BIT)   HyDC(IEJoin Single Valid)
        // 40    dc 14109 t1:21s            t2:
        // 50    dc 26396 t1:34s
        // 100   dc 34489 t1:57s
        // 200   dc 41150 t1:80s
        // 1000  dc 84150 t1:171s
        // 10000 dc 85360 t1:525s

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

//        Set<Predicate> equal = new HashSet<>();
//        Set<Predicate> unEqual = new HashSet<>();
//        Set<Predicate> needConbine = new HashSet<>();
//        Set<Predicate> noConbine = new HashSet<>();
//        predicates.getPredicates().forEach(predicate -> {
//            if (predicate.getOperator() == Operator.EQUAL)equal.add(predicate);
//
//            else if (predicate.getOperator() == Operator.UNEQUAL)unEqual.add(predicate);
//
//            else if (predicate.needCombine())needConbine.add(predicate);
//
//            else noConbine.add(predicate);
//        });
//        //TODO: next we can order every set. so we can change to List
//        indexAddSet(unEqual, indexProvider);
//        indexAddSet(equal, indexProvider);
//        indexAddSet(noConbine, indexProvider);
//        indexAddSet(needConbine, indexProvider);

        // Sampling
        IEvidenceSet sampleEvidenceSet = new SystematicLinearEvidenceSetBuilder(predicates,
                sampleRounds).buildEvidenceSet(input);
        HashEvidenceSet set = new HashEvidenceSet();
        sampleEvidenceSet.getSetOfPredicateSets().forEach(i -> set.add(i));
        //get the full evidence set
        IEvidenceSet fullSamplingEvidenceSet = new ColumnAwareEvidenceSetBuilder(predicates).buildEvidenceSet(set, input, efficiencyThreshold);

        // HyDC MMCS begin

        MMCSHyDC mmcsHyDC = new MMCSHyDC(predicates.getPredicates().size(),input, predicates, (HashEvidenceSet) fullSamplingEvidenceSet);

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

        System.out.println("refine time:" + TimeCal.getTime(0));
        System.out.println("clone time:" + TimeCal.getTime(1));
        System.out.println("update time:" + TimeCal.getTime(2));

    }
    public  static void indexAddSet(Set<Predicate> set, IndexProvider<Predicate> indexProvider){
        for (Predicate predicate : set) indexProvider.getIndex(predicate);
    }

}
