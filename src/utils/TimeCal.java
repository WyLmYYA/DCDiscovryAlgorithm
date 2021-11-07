package utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author yoyuan
 * @Description:
 * @DateTime: 2021/10/15 18:14
 */
public class TimeCal {

    public static List<Long> time = new ArrayList<>();

    public TimeCal(){

    }
    public static void add(long t1, int index){
        if(time.size() <= index){
            time.add(0L);
        }
        time.set(index, time.get(index) + t1);
    }

    public static long getTime(int index) {
        return time.get(index);
    }
}
