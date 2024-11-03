import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ChargerDataProcessor {

        private final String filePath;
        private final Map<ChargerAnalysis.Location, Map<ChargerAnalysis.Status, Integer>> locationStatusCount = initializeLocationStatusCount();
        private final Map<ChargerAnalysis.Status, Integer> statusCount = initializeStatusCount();
        private final Map<ChargerAnalysis.Location, Integer> chargingCycles = initializeLocationCount();
        private final Map<ChargerAnalysis.Location, Integer> totalTimeOnline = initializeLocationCount();

        public ChargerDataProcessor(String filePath) {
            this.filePath = filePath;
        }

        private static Map<ChargerAnalysis.Location, Map<ChargerAnalysis.Status, Integer>> initializeLocationStatusCount() {
            return Arrays.stream(ChargerAnalysis.Location.values())
                    .collect(Collectors.toMap(location -> location, loc -> initializeStatusCount()));
        }

        private static Map<ChargerAnalysis.Status, Integer> initializeStatusCount() {
            return Arrays.stream(ChargerAnalysis.Status.values())
                    .collect(Collectors.toMap(Function.identity(), status -> 0));
        }

        private static Map<ChargerAnalysis.Location, Integer> initializeLocationCount() {
            return Arrays.stream(ChargerAnalysis.Location.values())
                    .collect(Collectors.toMap(Function.identity(), loc -> 0));
        }

        public void processData() {
            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line;
                ChargerAnalysis.Location prevLocation = null;
                ChargerAnalysis.Status prevStatus = null;

                while ((line = br.readLine()) != null) {
                    String[] values = line.split(",");
                    if (values.length < 5) {
                        System.out.println("Skipping incomplete line: " + line);
                        continue;
                    }

                    try {
                        ChargerAnalysis.Location location = ChargerAnalysis.Location.valueOf(values[1].trim().toUpperCase());
                        ChargerAnalysis.Status status = ChargerAnalysis.Status.valueOf(values[4].trim().toUpperCase());

                        statusCount.merge(status, 1, Integer::sum);
                        locationStatusCount.get(location).merge(status, 1, Integer::sum);

                        if (status != ChargerAnalysis.Status.OFFLINE) {
                            totalTimeOnline.merge(location, 1, Integer::sum);
                        }

                        if (prevLocation == location && prevStatus == ChargerAnalysis.Status.AVAILABLE && status == ChargerAnalysis.Status.CHARGING) {
                            chargingCycles.merge(location, 1, Integer::sum);
                        }

                        prevLocation = location;
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
            System.out.println("Combined status:");
            statusCount.forEach((status, count) -> System.out.println(status + ": " + count));

            System.out.println("\nSatus by location:");
            locationStatusCount.forEach((location, statusMap) -> {
                int totalOnline = totalTimeOnline.get(location);
                int offlineCount = statusMap.get(ChargerAnalysis.Status.OFFLINE);
                double offlineRate = calculatePercentage(offlineCount, totalOnline);
                double occupancyRate = calculatePercentage(statusMap.get(ChargerAnalysis.Status.CHARGING), totalOnline);
                double functioningRate = calculatePercentage(statusMap.get(ChargerAnalysis.Status.AVAILABLE) + statusMap.get(ChargerAnalysis.Status.CHARGING), totalOnline);

                System.out.println(location + ":");
                statusMap.forEach((status, count) -> System.out.println("  " + status + ": " + count));
                System.out.println("  Charging cycles: " + chargingCycles.get(location));
                System.out.printf("  %% Time functioning: %.2f%%\n", functioningRate);
                System.out.printf("  %% Time offline: %.2f%%\n", offlineRate);
                System.out.printf("  %% Time suspended: %.2f%%\n", 100 - functioningRate - offlineRate);
                System.out.printf("  %% Occupancy rate: %.2f%%\n", occupancyRate);
            });
        }

        private double calculatePercentage(int part, int total) {
            return total == 0 ? 0 : (double) part / total * 100;

    }
}
