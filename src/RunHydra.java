import Hydra.de.hpi.naumann.dc.algorithms.hybrid.Hydra;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.input.InputIterationException;
import Hydra.de.hpi.naumann.dc.input.RelationalInput;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import utils.TimeCal;

import java.io.File;
import java.io.IOException;


public class RunHydra {


	public static void main(String[] args) throws InputIterationException, IOException {
		// TODO Auto-generated method stub
		//		String line=args[0];


//		String line ="dataset//stock_100.csv";
//		String sizeline ="100";
//		String line ="dataset//Test.csv";
//		String sizeline ="7";
		String line ="dataset//Tax10k.csv";
		String sizeline ="10000";

		long starttime = System.currentTimeMillis();
		int size=Integer.valueOf(sizeline);
		File datafile = new File(line);
//		File index=new File(indexline);

		RelationalInput data = new RelationalInput(datafile);
		Input input = new Input(data,size);
		PredicateBuilder predicates = new PredicateBuilder(input, false, 0.3d);
//		PredicateBuilder predicates = new PredicateBuilder(index,input);
		System.out.println("predicate space:"+predicates.getPredicates().size());
//		for(Predicate p:predicates.getPredicates())
//			System.out.println(p);
//		for (Collection<Predicate> pSet : predicates.getPredicateGroups()) {
//			for (Predicate p : pSet) {
//				System.out.println(p);
//			}
//		}
		Hydra hydra = new Hydra();
		System.out.println("now is hydra for dc");

		DenialConstraintSet dcs = hydra.run(input, predicates, System.currentTimeMillis() - starttime );
		long endtime=System.currentTimeMillis();
		System.out.println("Used time: "+ (endtime - starttime)+ " ms");

		TimeCal.time.forEach(System.out::println);

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
