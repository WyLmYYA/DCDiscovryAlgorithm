package Hydra.de.hpi.naumann.dc.algorithms.hybrid;


import Hydra.de.hpi.naumann.dc.cover.PrefixMinimalCoverSearch;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.ColumnAwareEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.SystematicLinearEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import mmcsforDC.MMCSDC;
import mmcsforDC.MMCSNode;

import java.util.LinkedList;
import java.util.Queue;

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;

public class Hydra {

	private IEvidenceSet fullEvidenceSet;
	protected int sampleRounds = 5;
	//sampling efficiency : growth/total
	protected double efficiencyThreshold = 0.005d;

	public IEvidenceSet getFullEvidenceSet() {
		return fullEvidenceSet;
	}

	public DenialConstraintSet run(Input input, PredicateBuilder predicates, long timebefore) {

		long hydraBegin = System.currentTimeMillis();
		//two phases:random sampling and focused sampling
		System.out.println("Building approximate evidence set...");
		//preliminary evidence set
		IEvidenceSet sampleEvidenceSet = new SystematicLinearEvidenceSetBuilder(predicates,
				sampleRounds).buildEvidenceSet(input);

//		System.out.println("Estimation size systematic sampling:" + sampleEvidenceSet.size());

		HashEvidenceSet set = new HashEvidenceSet();
		sampleEvidenceSet.getSetOfPredicateSets().forEach(i -> set.add(i));
		//get the full evidence set
//		IEvidenceSet fullEvidenceSet = set;
		IEvidenceSet fullEvidenceSet = new ColumnAwareEvidenceSetBuilder(predicates).buildEvidenceSet(set, input, efficiencyThreshold);
		System.out.println("Evidence set size deterministic sampler: " + fullEvidenceSet.size());

		System.out.println("sampling cost" + (System.currentTimeMillis() - hydraBegin));

		//??????DC???????????????check ????????????
		long t2 = System.currentTimeMillis();
		DenialConstraintSet dcsApprox = new PrefixMinimalCoverSearch(predicates).getDenialConstraints(fullEvidenceSet);

		System.out.println("hydra: " + dcsApprox.size());

		System.out.println("first Inversion cost:" + (System.currentTimeMillis() - t2));

//		for(DenialConstraint dcset:dcsApprox)
//			System.out.println(dcset.getPredicateSet().getBitset().toBitSet());

		t2 = System.currentTimeMillis();
		System.out.println("DC count approx:" + dcsApprox.size());
		dcsApprox.minimize();
		System.out.println("DC count approx after minimize:" + dcsApprox.size());
		System.out.println("first minimize cost: " + (System.currentTimeMillis() - t2));


		//complete????????????set ??????set??????DC
		long t1 = System.currentTimeMillis();
		IEvidenceSet result = new ResultCompletion(input, predicates).complete(dcsApprox, sampleEvidenceSet,
				fullEvidenceSet);
		this.fullEvidenceSet = result;
//		System.out.println("full EvidenceSet : " + result.size());
		System.out.println("complete dc size:" + dcsApprox.size());
		System.out.println("complete from sampling evidence cost :" + (System.currentTimeMillis() - t1));


//		System.out.println("before second inversion cost : " + (System.currentTimeMillis() - hydraBegin + timebefore));
		long starttime = System.currentTimeMillis();
		DenialConstraintSet dcs = new PrefixMinimalCoverSearch(predicates).getDenialConstraints(result);

//		dcs.forEach(System.out::println);
		System.out.println("second inversion cost: "+ (System.currentTimeMillis() - starttime));
		starttime = System.currentTimeMillis();
		System.out.println("dc before minimize " + dcs.size());
		dcs.minimize();
		System.out.println("dc after minimize " + dcs.size());
		System.out.println(" second minimize time: "+ (System.currentTimeMillis() - starttime)+ " ms");
//		for(DenialConstraint dc:dcs){
//			System.out.println(dc);
//		}
//		dcs.forEach(denialConstraint -> {
//			System.out.println(denialConstraint);
//			System.out.println(denialConstraint.getPredicateSet().getBitset().toBitSet());
//		});
		System.out.println("dc size:"+dcs.size());
//		for (int i = 0; i < 20; ++i){
//			System.out.println(indexProvider.getObject(i));
//		}
		return dcs;
	}

}
