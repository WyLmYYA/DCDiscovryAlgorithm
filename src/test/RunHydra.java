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
		String sizeline ="50000";
//		 line ="dataset//Tax10k.csv";
//		 sizeline ="10000";
		/**
		 * 100000
		 * dc before minimize 2175
		 dc after minimize 750
		 second minimize time: 606 ms
		 dc size:750
		 Used time: 51201 ms

		 second minimize time: 1 ms
		 dc size:11
		 Used time: 9674 ms
		 singel predicate valid count 687815
		 double predicates valid  count 0

		 t0.AirlineName(String) <> t1.AirlineName(String)   refine time: 0  refine count: 1
		 t0.ItemCategory(String) <> t1.ItemCategory(String)   refine time: 0  refine count: 1
		 t0.YearReceived(Integer) <= t1.IncidentDate(Integer)   refine time: 2  refine count: 1
		 t0.ClaimType(String) <> t1.ClaimType(String)   refine time: 7  refine count: 1760
		 t0.ClaimSite(String) == t1.ClaimSite(String)   refine time: 10  refine count: 2626
		 t0.CloseAmount(Double) <> t1.CloseAmount(Double)   refine time: 11  refine count: 3281
		 t0.Disposition(String) == t1.Disposition(String)   refine time: 24  refine count: 5817
		 t0.Disposition(String) <> t1.Disposition(String)   refine time: 32  refine count: 8526
		 t0.ClaimNumber(String) == t1.ClaimNumber(String)   refine time: 50  refine count: 1
		 t0.ClaimType(String) == t1.ClaimType(String)   refine time: 67  refine count: 14595
		 t0.CloseAmount(Double) == t1.CloseAmount(Double)   refine time: 68  refine count: 14899
		 t0.ClaimSite(String) <> t1.ClaimSite(String)   refine time: 104  refine count: 21757
		 t0.AirlineName(String) == t1.AirlineName(String)   refine time: 180  refine count: 10476
		 t0.ItemCategory(String) == t1.ItemCategory(String)   refine time: 201  refine count: 1371
		 t0.YearReceived(Integer) == t1.IncidentDate(Integer)   refine time: 216  refine count: 1
		 */

		long starttime = System.currentTimeMillis();
		int size=Integer.valueOf(sizeline);
		File datafile = new File(line);
		File index=new File("dataset/Tax10kPredicates");

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
