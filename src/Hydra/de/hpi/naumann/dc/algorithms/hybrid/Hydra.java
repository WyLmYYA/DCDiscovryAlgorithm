package Hydra.de.hpi.naumann.dc.algorithms.hybrid;


import Hydra.de.hpi.naumann.dc.cover.PrefixMinimalCoverSearch;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.ColumnAwareEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.evidenceset.build.sampling.SystematicLinearEvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;

public class Hydra {

	protected int sampleRounds = 20;
	//sampling efficiency : growth/total
	protected double efficiencyThreshold = 0.005d;

	public DenialConstraintSet run(Input input, PredicateBuilder predicates) {

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
		DenialConstraintSet dcsApprox = new PrefixMinimalCoverSearch(predicates).getDenialConstraints(fullEvidenceSet);

//		for(DenialConstraint dcset:dcsApprox)
//			System.out.println(dcset.getPredicateSet().getBitset().toBitSet());

		System.out.println("DC count approx:" + dcsApprox.size());
		dcsApprox.minimize();
		System.out.println("DC count approx after minimize:" + dcsApprox.size());


		//complete之后得到set 利用set得到DC
		long t1 = System.currentTimeMillis();
		IEvidenceSet result = new ResultCompletion(input, predicates).complete(dcsApprox, sampleEvidenceSet,
				fullEvidenceSet);
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
		long starttime = System.currentTimeMillis();
		DenialConstraintSet dcs = new PrefixMinimalCoverSearch(predicates).getDenialConstraints(result);
		long endtime=System.currentTimeMillis();
		System.out.println("evidence inversion time: "+ (endtime - starttime)+ " ms");
		dcs.minimize();
//		for(DenialConstraint dc:dcs){
//			System.out.println(dc);
//		}
		System.out.println("dc size:"+dcs.size());
		return dcs;
	}

}
