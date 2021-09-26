import com.opencsv.exceptions.CsvValidationException;
import Hydra.de.hpi.naumann.dc.input.InputIterationException;
import test.TestInitialMMCS;


import java.io.IOException;

/**
 * @Author yoyuan
 * @Description:
 * @DateTime: 2021/9/22 18:22
 */
public class Application {
    public static void main(String[] args) throws CsvValidationException, IOException, InputIterationException {
        String filePath = "./dataset/Tax10k.csv";
        TestInitialMMCS dc = new TestInitialMMCS();
        dc.run(filePath, 10000, 0.3d);
    }
}
