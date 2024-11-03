
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