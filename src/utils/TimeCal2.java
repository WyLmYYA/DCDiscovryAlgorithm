package utils;

/**
 * @Author yoyuan
 * @Description:
 * @DateTime: 2021/10/15 18:14
 */
public class TimeCal2 {

    static long time = 0L;

    public static void add(long t1){
        time += t1;
    }

    public static long getTime() {
        return time;
    }
}
