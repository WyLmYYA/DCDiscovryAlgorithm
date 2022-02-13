package utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author yoyuan
 * @Description:
 * @DateTime: 2021/10/15 18:14
 */
public class TimeCal2 {

    public static int len = 2;
    public static List<Long> time = new ArrayList<>();

    static {
        for(int i = 0; i < len; ++i){
            time.add(0L);
        }
    }
    public static void add(long t1, int index){
        time.set(index, time.get(index) + t1);
    }

    public static long getTime(int index) {
        return time.get(index);
    }
}
