package test;


import Hydra.ch.javasoft.bitset.IBitSet;
import Hydra.de.hpi.naumann.dc.algorithms.hybrid.ResultCompletion;
import Hydra.de.hpi.naumann.dc.cover.PrefixMinimalCoverSearch;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.ColumnAwareEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.SystematicLinearEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.input.InputIterationException;
import Hydra.de.hpi.naumann.dc.input.RelationalInput;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import mmcsforDC.MMCSDC;
import java.io.File;
import java.io.IOException;

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;


/**
 *@Description:
 *      1. use the method from hydra to get fullEvidence
 *      2. get covers for fullEvidence using MMCS for DC
 *      3. transform cover to DCs
 *      4. use the way of minimize in hydra to get final result
 * @Author yoyuan
 * @DateTime: 2021-10-9
 */

public class TestMmcsForDC {

    protected static int sampleRounds = 20;
    protected static double efficiencyThreshold = 0.005d;

    protected static Input input;

    protected static PredicateBuilder predicates;

    static IEvidenceSet samplingEvidenceSet;

    public static void main(String[] args) throws InputIterationException, IOException {
        long DCBegin = System.currentTimeMillis();
        String dataFile ="dataset//Tax10k.csv";
        int lineSize=1000;
//        dataFile =args[0];
//        lineSize = Integer.parseInt(args[1]);

        String preFile = "";
        if (args.length == 3)preFile = args[2];
        getPredicates(dataFile, lineSize, preFile);

//        Hydra hydra = new Hydra();
//        DenialConstraintSet dcs = hydra.run(input, predicates, 0 );

        IEvidenceSet fullEvidenceSet = getFullEvidenceSet();


        long mmcsTime = System.currentTimeMillis();
        MMCSDC mmcsdc = new MMCSDC(predicates.getPredicates().size(), fullEvidenceSet);

        System.out.println(mmcsdc.getCoverNodes().size());

        System.out.println("mmcs get cover cost time :" + (System.currentTimeMillis() - mmcsTime));


        /** transform covers to DCs
        */
        DenialConstraintSet denialConstraintSet = new DenialConstraintSet();

        mmcsdc.getCoverNodes().forEach(node -> {
            IBitSet bitSet = node.getElement();
            PredicateBitSet inverse = new PredicateBitSet();
            for (int next = bitSet.nextSetBit(0); next >= 0; next = bitSet.nextSetBit(next + 1)){
                Predicate predicate = indexProvider.getObject(next);
                inverse.add(predicate.getInverse());
            }
//            System.out.println(inverse.getBitset().toBitSet());
            denialConstraintSet.add(new DenialConstraint(inverse));

        });
        System.out.println("mmcs all cost time :" + (System.currentTimeMillis() - mmcsTime));

        /** output
        */
        System.out.println("before minimize: ");
        System.out.println(denialConstraintSet.size());
//        denialConstraintSet.forEach(System.out::println);
        denialConstraintSet.minimize();
        System.out.println("after minimize ");
        System.out.println(denialConstraintSet.size());
//        denialConstraintSet.forEach(denialConstraint -> {
//            System.out.println(denialConstraint);
//            System.out.println(denialConstraint.getPredicateSet().getBitset().toBitSet());
//        });

        System.out.println("all time :" + (System.currentTimeMillis() - DCBegin));
    }

    private static IEvidenceSet getFullEvidenceSet() {
        /**   sampling
        */
        IEvidenceSet sampleEvidenceSet = new SystematicLinearEvidenceSetBuilder(predicates,
                sampleRounds).buildEvidenceSet(input);
        HashEvidenceSet set = new HashEvidenceSet();
        sampleEvidenceSet.getSetOfPredicateSets().forEach(i -> set.add(i));

        samplingEvidenceSet = set;
        IEvidenceSet approEvidenceSet = new ColumnAwareEvidenceSetBuilder(predicates).buildEvidenceSet(set, input, efficiencyThreshold);


        /** get approximate DCs
        */
        DenialConstraintSet dcsApprox = new PrefixMinimalCoverSearch(predicates).getDenialConstraints(sampleEvidenceSet);
        System.out.println("DC before minimize count approx:" + dcsApprox.size());
        dcsApprox.minimize();
        System.out.println("DC count approx after minimize:" + dcsApprox.size());


        /** get fullEvidenceSet
        */
        IEvidenceSet result = new ResultCompletion(input, predicates).complete(dcsApprox, sampleEvidenceSet,
                sampleEvidenceSet);
        System.out.println(" get full EvidenceSet : " + result.size());
//        result.forEach(predicates1 -> {
//            System.out.println(predicates1);
//            System.out.println(predicates1.getBitset().toBitSet());
//        });

//        for (int i = 0; i < predicates.getPredicates().size(); ++i){
//            System.out.println(indexProvider.getObject(i));
//        }
        return result;
    }

    private static void getPredicates(String dataFile, int lineSize, String preFile) throws IOException, InputIterationException {
        RelationalInput data = new RelationalInput(new File(dataFile));
        Input inputTmp = new Input(data,lineSize);
        if (preFile.length() != 0){
            predicates = new PredicateBuilder(new File(preFile), inputTmp);
        }else {
            predicates = new PredicateBuilder(inputTmp, false, 0.3d);
        }
        input = inputTmp;

        System.out.println("predicate space:"+predicates.getPredicates().size());

    }
}
