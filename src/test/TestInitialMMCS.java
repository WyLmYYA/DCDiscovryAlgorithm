package test;

import com.opencsv.exceptions.CsvValidationException;
import Hydra.de.hpi.naumann.dc.cover.PrefixMinimalCoverSearch;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.ColumnAwareEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.SystematicLinearEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.input.InputIterationException;
import Hydra.de.hpi.naumann.dc.input.RelationalInput;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import mmcs.algorithm.MMCS;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @Author yoyuan
 * @Description:
 * @DateTime: 2021/9/22 12:34
 */
public class TestInitialMMCS {

    protected int sampleRounds = 20;

    protected double efficiencyThreshold = 0.005d;


    private final static Logger log = Logger.getLogger(TestInitialMMCS.class);

    public void run(String filePath, int rowLimit, double minimumSharedValue) throws IOException, CsvValidationException, InputIterationException {

        /**
         *  Part1: preprocess with relational input
        */
        log.info("--------------------Pre processing--------------------");

        log.info("[Begin getting initial relational input]");
        Input input = new Input(new RelationalInput(new File(filePath)), rowLimit);

        /**
         *  Part2: build predicates
        */
        log.info("[Begin getting predicates]");
        PredicateBuilder predicates = new PredicateBuilder(input, false, minimumSharedValue);
        log.info("predicates size: " + predicates.getPredicates().size());
//        predicates.getPredicates().forEach(predicate -> System.out.println(predicate));


        //two phases:random sampling and focused sampling
        log.info("Building approximate evidence set...");
        //preliminary evidence set
        IEvidenceSet sampleEvidenceSet = new SystematicLinearEvidenceSetBuilder(predicates,
                sampleRounds).buildEvidenceSet(input);


		log.info("-----Get DC-----");
		log.info("[Evidence inversion for pre-Evidence Set]");
		long t1 = System.currentTimeMillis();
        DenialConstraintSet dcsApprox = new PrefixMinimalCoverSearch(predicates).getDenialConstraints(sampleEvidenceSet);
//        dcsApprox.forEach(denialConstraint -> System.out.println(denialConstraint.getPredicateSet() + "  " + denialConstraint.getPredicateSet().getBitset().toBitSet()));
        System.out.println("DC count approx:" + dcsApprox.size());
        dcsApprox.minimize();
        System.out.println("DC count approx after minimize:" + dcsApprox.size());
		log.info("Time Cost: " + (System.currentTimeMillis() - t1));

//        dcsApprox.forEach(denialConstraint -> System.out.println(denialConstraint.getPredicateSet() + " " + denialConstraint.getPredicateSet().getBitset().toBitSet()));


        HashEvidenceSet set = new HashEvidenceSet();
        sampleEvidenceSet.getSetOfPredicateSets().forEach(i -> set.add(i));
        //get the full evidence set
        IEvidenceSet fullEvidenceSet = new ColumnAwareEvidenceSetBuilder(predicates).buildEvidenceSet(set, input, efficiencyThreshold);
        System.out.println("Evidence set size deterministic sampler fullEvidenceSet size: " + fullEvidenceSet.size());

        System.out.println("fullEvidenceSet");
//		Iterator<PredicateBitSet> iterator= fullEvidenceSet.iterator();
//		while(iterator.hasNext()){
//		    PredicateBitSet predicates1 = iterator.next();
//            System.out.println(predicates1);
//            System.out.println(predicates1.getBitset().toBitSet());
//        }

		log.info("[inital MMCS]");
        List<BitSet> fullEvidenceSetBitsets = new ArrayList<>();
        fullEvidenceSet.getSetOfPredicateSets().forEach(predicates1 -> {
            fullEvidenceSetBitsets.add(predicates1.getBitset().toBitSet());
//            System.out.println(predicates1.getBitset().toBitSet());
        });
		long t2 = System.currentTimeMillis();
        MMCS mmcs = new MMCS(70);
        mmcs.initiate(fullEvidenceSetBitsets);
        List<BitSet> mmcsRes = mmcs.getMinCoverSets();
        log.info("Time Cost: " + (System.currentTimeMillis() - t2));
//        mmcsRes.forEach(System.out::println);
        System.out.println(mmcsRes.size());

    }

}
