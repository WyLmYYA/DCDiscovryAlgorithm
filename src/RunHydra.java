

import Hydra.de.hpi.naumann.dc.algorithms.hybrid.Hydra;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraint;
import Hydra.de.hpi.naumann.dc.denialcontraints.DenialConstraintSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.input.InputIterationException;
import Hydra.de.hpi.naumann.dc.input.RelationalInput;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import mmcsforDC.MMCSDC;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;


public class RunHydra {

	public static void main(String[] args) throws InputIterationException, IOException {
		// TODO Auto-generated method stub
		//		String line=args[0];
//		String odline=args[1];
//		String indexline=args[2];
//		String sizeline=args[3];


//		String line ="dataset//fd30.csv";
//		String odline ="dataset//stock_1k2_nocross.txt";
//		String sizeline ="1000";

		String line ="dataset//Test.csv";
//		String odline ="dataset//hospital1kDC.txt";
		String sizeline ="7";
//		String indexline="dataset//Hospital_dc.txt";

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


//		dcs.forEach(System.out::println);
		String od="";
		int count=-1;
		for(Iterator<DenialConstraint> iter = dcs.iterator();iter.hasNext();){
			count++;
			od+="this is "+count+" "+iter.next().toString()+"\n";
		}
		System.out.println("Minimal dc size : "+(count+1));
//		System.out.println(od);
	}
}
