package test;

import Hydra.de.hpi.naumann.dc.algorithms.hybrid.Hydra;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.input.InputIterationException;
import Hydra.de.hpi.naumann.dc.input.RelationalInput;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import utils.TimeCal;
import utils.TimeCal2;
import utils.TimeCal3;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class RunHydra {

	public  static int num;
	public static HashMap<String, PredicateBitSet> calP = new HashMap();
	public static void main(String[] args) throws InputIterationException, IOException {
		// TODO Auto-generated method stub
		//		String line=args[0];


//		String line ="dataset//stock_100.csv";
//		String sizeline ="100";
//		String line ="dataset//Test.csv";
//		String sizeline ="7";
		String line ="dataset//CLAIM.csv";
		String sizeline ="10000";
		 line =args[0];
		 sizeline =args[1];


		long starttime = System.currentTimeMillis();
		int size=Integer.valueOf(sizeline);
		File datafile = new File(line);
		File index=new File("dataset/claim10kPre");

		RelationalInput data = new RelationalInput(datafile);
		Input input = new Input(data,size);
		PredicateBuilder predicates = new PredicateBuilder(input, false, 0.3d);
//		PredicateBuilder predicates = new PredicateBuilder(index,input);
		num = predicates.getPredicates().size();
		System.out.println("predicate space:"+predicates.getPredicates().size());

		Hydra hydra = new Hydra();
		System.out.println("now is hydra for dc");

		DenialConstraintSet dcs = hydra.run(input, predicates, System.currentTimeMillis() - starttime );
		long endtime=System.currentTimeMillis();
		System.out.println("Used time: "+ (endtime - starttime)+ " ms");

		// time 3 is calculate evidence O(n2*R) in complete
		System.out.println("singel predicate valid count " + TimeCal2.getTime(0));
		System.out.println("double predicates valid  count " + TimeCal2.getTime(1));


//		List<Map.Entry<Predicate, Long>> list = new ArrayList<>(TimeCal3.time.entrySet());
//		Collections.sort(list, new Comparator<Map.Entry<Predicate, Long>>() {
//			@Override
//			public int compare(Map.Entry<Predicate, Long> o1, Map.Entry<Predicate, Long> o2) {
//				return o1.getValue().compareTo(o2.getValue());
//			}
//		});
//
//		for (Map.Entry<Predicate, Long> entry : list){
//			System.out.println(entry.getKey() + "  refine time: " + entry.getValue() +"  refine count: " + TimeCal3.getPreCalTime(entry.getKey()));
//		}
		System.out.println("cal evi count : " + TimeCal2.getTime(3));
//		dcs.forEach(System.out::println);
//		String od="";
//		int count=-1;
//		for(Iterator<DenialConstraint> iter = dcs.iterator();iter.hasNext();){
//			count++;
//			od+="this is "+count+" "+iter.next().toString()+"\n";
//		}
//		System.out.println("Minimal dc size : "+(count+1));


//		System.out.println(od);
	}
}
