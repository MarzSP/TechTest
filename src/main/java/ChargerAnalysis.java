import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChargerAnalysis {

    private static final String FILE_PATH = "src/main/resources/datafile.csv";

    public enum Location { ARNHEM, EINDHOVEN }
    public enum Status { AVAILABLE, CHARGING, SUSPENDEDEV, OFFLINE }

    public static void main(String[] args) {
        ChargerDataProcessor processor = new ChargerDataProcessor(FILE_PATH);
        processor.processData();
        processor.printSummary();
    }
}