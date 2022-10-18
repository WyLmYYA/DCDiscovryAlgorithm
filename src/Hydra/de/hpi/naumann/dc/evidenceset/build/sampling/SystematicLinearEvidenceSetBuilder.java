package Hydra.de.hpi.naumann.dc.evidenceset.build.sampling;


import HyDCFinalVersion.MMCSDC;
import Hydra.de.hpi.naumann.dc.evidenceset.HashEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.IEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.TroveEvidenceSet;
import Hydra.de.hpi.naumann.dc.evidenceset.build.EvidenceSetBuilder;
import Hydra.de.hpi.naumann.dc.input.ColumnPair;
import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.paritions.Cluster;
import Hydra.de.hpi.naumann.dc.paritions.ClusterPair;
import Hydra.de.hpi.naumann.dc.paritions.LinePair;
import Hydra.de.hpi.naumann.dc.predicates.Predicate;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;
import Hydra.de.hpi.naumann.dc.predicates.sets.PredicateBitSet;
import gnu.trove.iterator.TIntIterator;

import java.util.*;
import java.util.concurrent.*;

public class SystematicLinearEvidenceSetBuilder extends EvidenceSetBuilder {
    private final int factor;

    private Map<LinePair, PredicateBitSet> linePairPredicateBitSetMap;

    public SystematicLinearEvidenceSetBuilder(PredicateBuilder pred, int factor) {
        super(pred);
        this.factor = factor;
        linePairPredicateBitSetMap = new HashMap<>();
    }

    boolean thread = true;

    public IEvidenceSet buildEvidenceSet(Input input) throws ExecutionException, InterruptedException {
        Collection<ColumnPair> pairs = predicates.getColumnPairs();
        createSets(pairs);

        IEvidenceSet evidenceSet = new TroveEvidenceSet();


        Random r = new Random();
        if (thread) {
//			linePairPredicateBitSetMap = new ConcurrentHashMap<>();
            int len = input.getLineCount();
            int qua = len / 4;
            int[] lef = new int[]{0, qua, qua * 2, qua * 3};
            int[] rig = new int[]{qua - 1, qua * 2 - 1, qua * 3 - 1, len - 1};
            ExecutorService executorService = Executors.newFixedThreadPool(4);
            List<Future<IEvidenceSet>> futures = new ArrayList<>();
            for (int thread = 0; thread < 4; ++thread) {
                int finalThread = thread;
                futures.add((Future<IEvidenceSet>) executorService.submit(() -> {
                    IEvidenceSet tmp = new TroveEvidenceSet();
                    for (int i = lef[finalThread]; i <= rig[finalThread]; ++i){
                        int finalI = i;
                        PredicateBitSet staticSet = getStatic(pairs, i);
                        for (int count = 0; count < factor; ++count) {
                            int j = r.nextInt(input.getLineCount() - 1);
                            if (j >= finalI)
                                j++;
//							LinePair linePair = new LinePair(finalI, j);
//							if (linePairPredicateBitSetMap.containsKey(linePair) ){
//								tmp.add(linePairPredicateBitSetMap.get(linePair));
//								continue;
//							}
                            int finalJ = j;
                            PredicateBitSet set = getPredicateSet(staticSet, pairs, finalI, finalJ);
//							linePairPredicateBitSetMap.put(linePair, set);
//							evidenceSet.add(set);
                            tmp.add(set);
                        }
                    }

                    return tmp;
                }));

            }
            executorService.shutdown();
            while (!executorService.isTerminated()){

            }
            long time = System.currentTimeMillis();
            for (Future<IEvidenceSet> future : futures){
                IEvidenceSet iEvidenceSet = future.get();
                for (PredicateBitSet tmp : iEvidenceSet){
                    evidenceSet.add(tmp);
                }
            }
            System.out.println("future: " + (System.currentTimeMillis() - time));
        } else {
            for (int i = 0; i < input.getLineCount(); ++i) {
                PredicateBitSet staticSet = getStatic(pairs, i);

                int finalI = i;

                for (int count = 0; count < factor; ++count) {
                    int j = r.nextInt(input.getLineCount() - 1);
                    if (j >= finalI)
                        j++;
                    LinePair linePair = new LinePair(finalI, j);
                    if (linePairPredicateBitSetMap.containsKey(linePair)) {
                        evidenceSet.add(linePairPredicateBitSetMap.get(linePair));
                        continue;
                    }
                    int finalJ = j;
//						PredicateBitSet set = task.get();
                    PredicateBitSet set = getPredicateSet(staticSet, pairs, finalI, finalJ);
                    linePairPredicateBitSetMap.put(linePair, set);
                    evidenceSet.add(set);

                }

            }
        }

        return evidenceSet;
    }

    public IEvidenceSet buildEvidenceSet(Input input, int rate) {
        Collection<ColumnPair> pairs = predicates.getColumnPairs();
        createSets(pairs);

        IEvidenceSet evidenceSet = new TroveEvidenceSet();

        Random r = new Random();
        for (int i = 0; i < input.getLineCount() / rate; ++i) {
            PredicateBitSet staticSet = getStatic(pairs, i);
            for (int count = 0; count < factor; ++count) {
                int j = r.nextInt(input.getLineCount() - 1);
                if (j >= i)
                    j++;

                LinePair linePair = new LinePair(i, j);
                if (linePairPredicateBitSetMap.containsKey(linePair)) {
                    evidenceSet.add(linePairPredicateBitSetMap.get(linePair));
                    continue;
                }
                PredicateBitSet set = getPredicateSet(staticSet, pairs, i, j);
                linePairPredicateBitSetMap.put(linePair, set);
                evidenceSet.add(set);
            }
        }
        return evidenceSet;
    }

    // get evidenceSet for clusterPair
    public HashEvidenceSet getEvidenceSet(ClusterPair clusterPair) {
        Collection<ColumnPair> pairs = predicates.getColumnPairs();
        createSets(pairs);

        HashEvidenceSet evidenceSet = new HashEvidenceSet();
        Cluster c1 = clusterPair.getC1();
        Cluster c2 = clusterPair.getC2();
        TIntIterator iter1 = c1.iterator();
        while (iter1.hasNext()) {
            int nextC1 = iter1.next();
            PredicateBitSet staticSet = getStatic(pairs, nextC1);
            TIntIterator iter2 = c2.iterator();
            while (iter2.hasNext()) {
                int nextC2 = iter2.next();
                if (nextC1 != nextC2) {
                    PredicateBitSet set = getPredicateSet(staticSet, pairs, nextC1, nextC2);
                    evidenceSet.add(set);

                    PredicateBitSet set1 = getPredicateSet(staticSet, pairs, nextC2, nextC1);
                    evidenceSet.add(set1);
                }

            }
        }

        return evidenceSet;
    }

    public HashEvidenceSet getEvidenceSet(List<ClusterPair> clusterPairs) throws InterruptedException {
        HashEvidenceSet evidenceSet = new HashEvidenceSet();
        CopyOnWriteArraySet tmpSet = new CopyOnWriteArraySet();
//		clusterPairs.forEach(clusterPair -> evidenceSet.add(getEvidenceSet(clusterPair)));

        for (int i = 0; i < clusterPairs.size(); i += 4) {
            CountDownLatch countDownLatch = new CountDownLatch(4);
            for (int j = 0; j < Math.min(4, clusterPairs.size() - i); ++j) {
                ClusterPair clusterPair = clusterPairs.get(i + j);
                Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        tmpSet.add(getEvidenceSet(clusterPair));
                        countDownLatch.countDown();
                    }
                };
                Thread thread = new Thread(task);
                thread.run();
            }
            countDownLatch.await();
        }
        for (Object ob : tmpSet) {
            evidenceSet.add((HashEvidenceSet) ob);
        }

        return evidenceSet;
    }

}
