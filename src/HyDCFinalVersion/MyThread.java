package HyDCFinalVersion;

import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;

import java.util.List;

import static HyDCFinalVersion.RunHyDCWithFastDC.denialConstraintSet;

/**
 * @Author yoyuan
 * @Description:
 * @DateTime: 2022/1/5 16:31
 */
public class MyThread extends Thread{
    private int predicate;

    private IEvidenceSet uncoverSet;

    private PredicateBuilder predicates;

    private  Input input;

    public List<MMCSNode> coverNodes;

    public MyThread(int predicate, IEvidenceSet uncoverSet, PredicateBuilder predicates, Input input){
        this.predicate = predicate;
        this.uncoverSet = uncoverSet;
        this.predicates = predicates;
        this.input = input;
    }
    public void run(){
        MMCSDC mmcsdc = new MMCSDC(predicates.getPredicates().size(), uncoverSet, predicates, input);
        mmcsdc.getCoverNodes().forEach(mmcsNode -> {
            denialConstraintSet.add(mmcsNode.getDenialConstraint(predicate));
        });
    }
}
