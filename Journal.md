所有的实验及代码都是在Hydra结合HPI的基础数据结构上实现的,因为有实现好的成系统的工具，虽然有些地方会多一些弯路，但相对而言不影响结果，而且工具十分完善，要自己写的话会花费很多时间

## 2021-9-24
1.基础版的MMCS和evidence inversion在 evidenceset~=10000左右输出真正的dc~=8w，mmcs得到的结果~=100w的时候，时间差不多是150s比80s，所以如果加入DC的性质优化mmcs之后，mmcs的时间应该可以提升一个量级

2.原因是基础版的MMCS是把所有的predicates都遍历了一遍，没有利用predicates之间的关系去优化，比如我列举了t0[Salary]=t1[Salary]我在做覆盖的时候就不能继续列举
t0[Salary]<>t1[Salary](<>表示≠),应用这些进MMCS的话，会加快很多

3.基础版的MMCS输出数量并不是真正的DC数量，1中说了8w 对 100w 的比例，一个是2的原因，一个是真正的DC需要在覆盖上取非，可能结果又会不同

4.在Hydra的步骤中Inversion时间相对rebuild也就是check而言，时间要短不少

5.下一步先加入优化，得到适合DC版的MMCS，然后再去考虑check中的IEJoin

## 2021-9-26
1.仔细看了下Bitset的源码也是用的long来实现的，仁杰动态FD中用的long比Bitset快，
或许是因为Bitset中是long数组，因为要处理任意长度的bitset，因为FD数据集中很少超过64列的所以long更适用，
并且处理起来会快很多，因为进行操作的时候不用去顾及long数组的切片什么的

2.用HPI metanome 封装好的Ibitset，LongBitSet以及Predicate，PredicateBitSet表示谓词集合，IEvidenceSet
表示Evidence集合，照着仁杰的long版本的mmcs改了一下

3.接下来是检验这个版本的正确性

4.然后加入谓词(Predicates)之间的互斥关系，使得MMCS适用于DC

## 2021-9-27
循环chosenEvidence写成了nextCandidatePredicates，找了两个小时，心态炸了，回家打游戏
```java
// MMCSDC.java
for (int next = chosenEvidence.nextSetBit(0); next >= 0; next = chosenEvidence.nextSetBit(next + 1)){
    MMCSNode childNode = currentNode.getChildNode(next, nextCandidatePredicates);
    if(childNode.isGlobalMinimal()){
        walkDown(childNode, currentCovers);
    }
}
```

## 2021-10-8
把mmcs转换存储成了复合hydra框架的版本，需要进一步对DC的MMCS进行剪枝，这个可以在Fast DC里面找到三种剪枝策略，明天先用Trivial试一下

## 2021-10-9
MMCSDC写完了，写了Trivial类型的简单剪枝，其他的包含关系及最小化用的是hydra的minimize

所有流程都和hydra一样，把第二次inversion换成mmcs得到覆盖然后转化成DCs

这个过程用Tax10k，predicates 70， EvidenceSet 11043, DCs 85360

inversion大概为80-90s， mmcs大概为100-110s

看了下MMCS是14年的算法，hydra是17年的，如果作者试过了mmcs发现这个inversion比mmcs快，那么mmcs是比不过inversion的

还有一种可能是，作者的inversion把这个问题转化成了一个非覆盖问题，因为顶点（在DC是谓词）数量太多了，mmcs运行时间会很长

再挑选Evidence的时候的策略用的是仁杰写FD的时候的策略，看看在这里优化下mmcs还能不能更快

## 2021-10-16
结合前段时间看的树状数组，看了下朱云鹏学长的树状数组BIT改进IEJoin，看上去从理论上好像
没有很明显的优势，但是学长文章中的实验，BIT会比IEJoin快很多

IEJoin的时间在排序和构建映射的数据结构的过程花了很多的时间，在Tax10k中甚至可以达到100s左右

但是树状数组的数据结构构建和一些映射比IEJoin少蛮多，所以可能在这一步会快不少，

今天先简单实现了一下BIT，接下来要Debug一下，然后再优化，简单的版本估计很难比IEJoin快

## 2021-10-16
今天看了一下Hydra的整体流程

可以超过Hydra的两个点：
1. Hydra的Complete过程是从抽样的EvidenceSet Iversion成DC直接Complete的，这里相当于所有的pre-EvidenceSet没有覆盖到的元组所产生的所有
EvidenceSet都加到了full EvidenceSet里边，这一步如果用多轮抽样不知是否会加快，
注意点： 抽样求EvidenceSet其实是一个n平方*属性的复杂度，所以是否能将这个过程加快，不然多轮抽样速度会更慢
   ，
2. Hydra的第二步Inversion在full EvidenceSet上进行的，第一步Inversion其实花费的时间也很多，但是在第二步上没起到作用，所以一定程度上可以理解为
第一次Inversion的时间其实是浪费掉了，在Tax10k这个数据集上，第一步Inversion和第二步时间其实差不多，所以如果我们是连续的MMCS然后用类似动态的思想去做的话其实是可以很大程度上去减少时间超过Hydra的
   
## 2021-10-20 
树状数组的表现似乎并不是很好，5.600s  比  2.300秒


## 2021-11.31
树状数组写完了，比IEJoin快