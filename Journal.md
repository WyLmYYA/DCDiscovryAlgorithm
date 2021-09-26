所有的实验及代码都是在Hydra结合HPI的基础数据结构上实现的,因为有实现好的成系统的工具，虽然有些地方会多一些弯路，但相对而言不影响结果，而且工具十分完善，要自己写的话会花费很多时间

## 2021-9-24
1.基础版的MMCS和evidence inversion在 evidenceset~=10000左右输出真正的dc~=8w，mmcs得到的结果~=100w的时候，时间差不多是150s比80s，所以如果加入DC的性质优化mmcs之后，mmcs的时间应该可以提升一个量级

2.原因是基础版的MMCS是把所有的predicates都遍历了一遍，没有利用predicates之间的关系去优化，比如我列举了t0[Salary]=t1[Salary]我在做覆盖的时候就不能继续列举
t0[Salary]<>t1[Salary](<>表示≠),应用这些进MMCS的话，会加快很多

3.基础版的MMCS输出数量并不是真正的DC数量，1中说了8w 对 100w 的比例，一个是2的原因，一个是真正的DC需要在覆盖上取非，可能结果又会不同

4.在Hydra的步骤中Inversion时间相对rebuild也就是check而言，时间要短不少

5.下一步先加入优化，得到适合DC版的MMCS，然后再去考虑check中的IEJoin