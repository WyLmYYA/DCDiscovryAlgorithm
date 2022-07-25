关于BITJoin

现在的情况是IEJoin需要准备的时间

1. 4个sortedArray
2. 2个permutation
3. O1.O2

相对应的BITJoin
1. 3个sortedArray（有一个sortedArray是在BIT里面计算，所以其实也算是4个）
2. 1个O1

现在的情况看来，如果谓词对应的列取值分散的很稀疏，那么上述的准备时间会很长，那么BIT少了2个permutation和一个O2，优势就会比较大

如果取值比较均匀，在这方面的优势不大的话，那么BIT的额外时间会比较多
1. IEJoin的数据结构是Bitset构建和初始化不用时间
2. BIT的初始化和构建花费的时间比较长，那么如果行数多的话，这个构建的过程复杂度也是n级别的（相对这个本来就很快的过程来说）
3. IEJoin可以同化的次数比较多，现在的代码还可以优化一下，
