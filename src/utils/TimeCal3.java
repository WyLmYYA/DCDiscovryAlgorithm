package utils;

import Hydra.de.hpi.naumann.dc.predicates.Predicate;

import java.util.*;

/**
 * @Author yoyuan
 * @Description:
 * @DateTime: 2021/10/15 18:14
 */
public class TimeCal3 {

    public static Map<Predicate, Long> time = new HashMap<>();

    public static void add(Predicate pre, long l1){
        if (time.containsKey(pre)){
            time.put(pre, time.get(pre) + l1);
        }else{
            time.put(pre, l1);
        }
    }

    public static long getTime(int index) {
        return time.get(index);
    }
}
