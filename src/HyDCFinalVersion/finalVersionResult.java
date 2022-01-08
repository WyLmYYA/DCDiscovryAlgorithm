package HyDCFinalVersion;

/**
 * @Author yoyuan
 * @Description:
 * @DateTime: 2022/1/7 13:47
 */
public class finalVersionResult {
    // e3008 env:

    // 100  11s  valid 4s  transitivity 4s getchild 3s


    /** 1000
     * -- hydra ---
     * first Inversion cost:19173
     * first minimize cost: 45220
     * complete from sampling evidence cost :21216
     * second inversion cost: 17503
     * dc before minimize 287895
     * dc after minimize 84150
     *  second minimize time: 35400 ms
     * dc size:84150
     * Used time: 138866 ms
     *
     * -- hydc no transitivity --
     * 293692
     * mmcs and get dcs cost:243841
     * 287895
     * dcs :84150
     * minimize cost:36034
     * valid time 232539
     *
     * -- hydc with transitivity --
     * 124440
     * mmcs and get dcs cost:121573
     * 122411
     * dcs :84150
     * minimize cost:20164
     * valid time 80538
     * transitivity prune time 9263
     * get child time 14627
     *
     * -- hydc with BITJoin --
     * mmcs and get dcs cost:120061
     * 122518
     * dcs :84152
     * minimize cost:20853
     * valid time 80868
     * transitivity prune time 9070
     * get child time 14011
     */
    // 10000 dc 85360 t1:525s
    /**
     * 10000
     * first Inversion cost:49726
     * first minimize cost: 70892
     * complete from sampling evidence cost :184850
     * second inversion cost: 47519
     * dc before minimize 336594
     * dc after minimize 85360
     *  second minimize time: 57970 ms
     * dc size:85360
     * Used time: 412609 ms
     */

}
