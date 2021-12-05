package test;

import Hydra.de.hpi.naumann.dc.input.Input;
import Hydra.de.hpi.naumann.dc.input.InputIterationException;
import Hydra.de.hpi.naumann.dc.input.RelationalInput;
import Hydra.de.hpi.naumann.dc.predicates.PredicateBuilder;

import java.io.File;
import java.io.IOException;

public class RunHyDC {
    public static void main(String[] args) throws IOException, InputIterationException {
        //Initial: get predicates
        String line ="dataset//Tax10k.csv";
        String sizeline ="10000";

        int size=Integer.valueOf(sizeline);
        File datafile = new File(line);

        RelationalInput data = new RelationalInput(datafile);
        Input input = new Input(data,size);
        PredicateBuilder predicates = new PredicateBuilder(input, false, 0.3d);

        // Sampling


    }
}
