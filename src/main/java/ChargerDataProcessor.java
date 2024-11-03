import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ChargerDataProcessor {

    private final String filePath;
    private final Map<ChargerAnalysis.Location, Map<String, Map<ChargerAnalysis.Status, Integer>>> locationChargerStatusCount = new HashMap<>();

    private final Map<String, Integer> chargingCycles = new HashMap<>();
    private final Map<String, Integer> totalTimeOnline = new HashMap<>();

    public ChargerDataProcessor(String filePath) {
        this.filePath = filePath;
    }

    private static Map<ChargerAnalysis.Status, Integer> initializeStatusCount() {
        return Arrays.stream(ChargerAnalysis.Status.values())
                .collect(Collectors.toMap(Function.identity(), status -> 0));
    }

    public void processData() {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            String prevCharger = null;
            ChargerAnalysis.Status prevStatus = null;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length < 5) {
                    System.out.println("Skipping incomplete line: " + line);
                    continue;
                }

                try {
                    ChargerAnalysis.Location location = ChargerAnalysis.Location.valueOf(values[1].trim().toUpperCase());
                    String charger = values[2].trim(); // ChargerID als String
                    ChargerAnalysis.Status status = ChargerAnalysis.Status.valueOf(values[4].trim().toUpperCase());

                    // Data per locatie en chargerID
                    locationChargerStatusCount.putIfAbsent(location, new HashMap<>());
                    locationChargerStatusCount.get(location).putIfAbsent(charger, initializeStatusCount());
                    locationChargerStatusCount.get(location).get(charger).merge(status, 1, Integer::sum);

                    // Onlinetijd bijwerken
                    if (status != ChargerAnalysis.Status.OFFLINE) {
                        totalTimeOnline.merge(charger, 1, Integer::sum);
                    }

                    // Aantal charging cycles bijwerken
                    if (prevCharger != null && prevCharger.equals(charger) && prevStatus == ChargerAnalysis.Status.AVAILABLE && status == ChargerAnalysis.Status.CHARGING) {
                        chargingCycles.merge(charger, 1, Integer::sum);
                    }

                    prevCharger = charger;
                    prevStatus = status;
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid data line - skipping line(s): " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    public void printSummary() {
        System.out.println("\n=== Summary of charger status ===");
        locationChargerStatusCount.forEach(this::printLocationSummary);
    }

    private void printLocationSummary(ChargerAnalysis.Location location, Map<String, Map<ChargerAnalysis.Status, Integer>> chargerMap) {
        System.out.println(location + ":");
        chargerMap.forEach(this::printChargerSummary);
    }

    private void printChargerSummary(String charger, Map<ChargerAnalysis.Status, Integer> statusMap) {
        int totalOnline = totalTimeOnline.getOrDefault(charger, 0);
        int offlineCount = statusMap.getOrDefault(ChargerAnalysis.Status.OFFLINE, 0);

        double offlineRate = calculatePercentage(offlineCount, totalOnline);
        double occupancyRate = calculatePercentage(statusMap.getOrDefault(ChargerAnalysis.Status.CHARGING, 0), totalOnline);
        double functioningRate = calculatePercentage(statusMap.getOrDefault(ChargerAnalysis.Status.AVAILABLE, 0) + statusMap.getOrDefault(ChargerAnalysis.Status.CHARGING, 0), totalOnline);
        double suspendedRate = calculatePercentage(statusMap.getOrDefault(ChargerAnalysis.Status.SUSPENDEDEV, 0), totalOnline);

        System.out.println("  " + charger + ":");
        printStatusSummary(statusMap);
        System.out.println("    Charging cycles: " + chargingCycles.getOrDefault(charger, 0));
        printRates(functioningRate, offlineRate, suspendedRate, occupancyRate);
    }

    private void printStatusSummary(Map<ChargerAnalysis.Status, Integer> statusMap) {
        statusMap.forEach((status, count) -> System.out.println("    " + status + ": " + count));
    }

    private void printRates(double functioningRate, double offlineRate, double suspendedRate, double occupancyRate) {
        System.out.printf("    %% Time functioning: %.2f%%\n", functioningRate);
        System.out.printf("    %% Time offline: %.2f%%\n", offlineRate);
        System.out.printf("    %% Time suspended: %.2f%%\n", suspendedRate);
        System.out.printf("    %% Time user spent in charging state: %.2f%%\n", occupancyRate);
    }


    private double calculatePercentage(int part, int total) {
        return total == 0 ? 0 : (double) part / total * 100;
    }
}
