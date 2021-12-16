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

import static Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet.indexProvider;

public class Hydra {

	private IEvidenceSet fullEvidenceSet;
	protected int sampleRounds = 20;
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
		System.out.println("Estimation size systematic sampling:" + sampleEvidenceSet.size());

		HashEvidenceSet set = new HashEvidenceSet();
		sampleEvidenceSet.getSetOfPredicateSets().forEach(i -> set.add(i));
		//get the full evidence set
		IEvidenceSet fullEvidenceSet = new ColumnAwareEvidenceSetBuilder(predicates).buildEvidenceSet(set, input, efficiencyThreshold);
		System.out.println("Evidence set size deterministic sampler: " + fullEvidenceSet.size());

		System.out.println("fullEvidenceSet");
//		Iterator<PredicateBitSet> iterator= fullEvidenceSet.iterator();
//		while(iterator.hasNext())
//			System.out.println(iterator.next().getBitset().toBitSet());


		//得到DC之后都需要check 防止冗余
		long t2 = System.currentTimeMillis();
		DenialConstraintSet dcsApprox = new PrefixMinimalCoverSearch(predicates).getDenialConstraints(fullEvidenceSet);
		System.out.println("first Inversion :" + (System.currentTimeMillis() - t2));

//		for(DenialConstraint dcset:dcsApprox)
//			System.out.println(dcset.getPredicateSet().getBitset().toBitSet());

		System.out.println("DC count approx:" + dcsApprox.size());
		dcsApprox.minimize();
		System.out.println("DC count approx after minimize:" + dcsApprox.size());


		//complete之后得到set 利用set得到DC
		long t1 = System.currentTimeMillis();
		for (int i = 0; i < 68; ++i){
			System.out.println(indexProvider.getObject(i));
		}
		IEvidenceSet result = new ResultCompletion(input, predicates).complete(dcsApprox, sampleEvidenceSet,
				fullEvidenceSet);
		this.fullEvidenceSet = result;
		System.out.println("full EvidenceSet : " + result.size());
//		result.forEach(predicates1 -> {
//			System.out.println(predicates1);
//			System.out.println(predicates1.getBitset().toBitSet());
//		});
		System.out.println("complete time :" + (System.currentTimeMillis() - t1));
//
//		for(PredicateBitSet pre:result){
//			System.out.println(pre.getBitset().toBitSet());
//		}

		//写index文件
		/*System.out.println(indexProvider);

		try{
			System.out.println("Writting dc...");
			File file =new File("dataset/index.txt");
			FileWriter fileWritter = new FileWriter(file);
			for(int i=0;i<predicates.getPredicates().size();i++){
				System.out.println(indexProvider.getObject(i));
				fileWritter.write(indexProvider.getObject(i).toString()+"\r\n");
			}

			if(!file.exists()){
				file.createNewFile();
			}
			fileWritter.close();
			System.out.println("Write dc Done");
		}catch(IOException e){
			e.printStackTrace();
		}
*/
//		for (int i = 0; i < 20; ++i){
//			System.out.println(indexProvider.getObject(i));
//		}


		System.out.println("before second inversion cost : " + (System.currentTimeMillis() - hydraBegin + timebefore));

		long starttime = System.currentTimeMillis();
		DenialConstraintSet dcs = new PrefixMinimalCoverSearch(predicates).getDenialConstraints(result);

//		dcs.forEach(System.out::println);
		System.out.println("evidence inversion time: "+ (System.currentTimeMillis() - starttime)+ " ms");
		starttime = System.currentTimeMillis();
		dcs.minimize();
		System.out.println(" minimize time: "+ (System.currentTimeMillis() - starttime)+ " ms");
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
