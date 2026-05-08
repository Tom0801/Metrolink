import java.io.*;
import java.util.*;

public class MetrolinkPlanner {

    // Saves one station name with its line name
    static class Station {
        String name;
        String line;

        // Sets station data when a Station is made
        public Station(String name, String line) {
            this.name = name;
            this.line = line;
        }
    }

    // Saves one linked station with its travel time
    static class Edge {
        Station to;
        double time;

        // Sets edge data when an Edge is made
        public Edge(Station to, double time) {
            this.to = to;
            this.time = time;
        }
    }

    // Links a station name to all line versions
    private Map<String, List<Station>> nameToStations = new HashMap<>();
    // Links each station version to its next stations
    private Map<Station, List<Edge>> graph = new HashMap<>();

    // Keeps station names used in input checks
    private Set<String> stationNames = new HashSet<>();

    // Reads the CSV file and builds the station graph
    public void loadData(String file) throws Exception {
        // Open the file so the program can read it
        BufferedReader br = new BufferedReader(new FileReader(file));
        // Keep one line text read from the file
        String line;
        // Keep the active Metrolink line name
        String currentLine = "";
        // Read each CSV line until the file ends
        while ((line = br.readLine()) != null) {
            // Remove spaces at both ends
            line = line.trim();
            // Skip empty CSV lines
            if (line.isEmpty())
                continue;

            // Split one CSV line by commas
            String[] parts = line.split(",");

            // Detect a line name section
            if (parts.length >= 1 && (parts.length < 2 || parts[1].isEmpty())
                    && (parts.length < 3 || parts[2].isEmpty())) {
                // Set the active line name
                currentLine = parts[0];
                continue;
            }
            // Skip the heading line
            if (parts[0].contains("From") || parts[2].contains("Time"))
                continue;
            // Skip lines without enough values
            if (parts.length < 3)
                continue;

            // Read the two station names and travel time
            String from = parts[0];
            String to = parts[1];
            double time = Double.parseDouble(parts[2]);
            // Add both station versions
            addStation(from, currentLine);
            addStation(to, currentLine);
            // Add a link between the two stations
            connect(from, currentLine, to, currentLine, time);
        }
        // Close the file after reading
        br.close();

        // Add zero minute links between same-name stations
        connectInterchanges();
    }

    // Add change links between line versions with the same station name
    private void connectInterchanges() {
        // Go through each station name
        for (String name : nameToStations.keySet()) {
            // Get all line versions of this station
            List<Station> versions = nameToStations.get(name);
            // Compare each version with each linked version
            for (Station s1 : versions) {
                for (Station s2 : versions) {
                    if (!s1.line.equals(s2.line)) {
                        // Add a zero time edge when the lines are different
                        graph.get(s1).add(new Edge(s2, 0.0));
                    }
                }
            }
        }
    }

    // Add one station version when it is new
    private void addStation(String name, String line) {
        // Make a list when this station name is new
        if (!nameToStations.containsKey(name)) {
            nameToStations.put(name, new ArrayList<>());
        }

        // Check if this line version already exists
        boolean exists = false;
        for (Station s : nameToStations.get(name)) {
            if (s.line.equals(line)) {
                // Mark that the version already exists
                exists = true;
                break;
            }
        }

        // Add a new station version to all maps
        if (!exists) {
            Station st = new Station(name, line);
            nameToStations.get(name).add(st);
            graph.put(st, new ArrayList<>());
            stationNames.add(name);
        }
    }

    // Connect two station versions in both ways
    private void connect(String fromName, String fromLine, String toName, String toLine, double time) {
        // Find the two station versions
        Station from = findStation(fromName, fromLine);
        Station to = findStation(toName, toLine);

        // Add edges when both stations exist
        if (from != null && to != null) {
            graph.get(from).add(new Edge(to, time));
            graph.get(to).add(new Edge(from, time));
        }
    }

    // Find one station version by name and line
    private Station findStation(String name, String line) {
        // Get all versions with this station name
        List<Station> stations = nameToStations.get(name);
        // Stop when the station name is missing
        if (stations == null)
            return null;
        // Check each station version
        for (Station s : stations) {
            if (s.line.equals(line))
                // Give back the matching station
                return s;
        }
        // No matching station was found
        return null;
    }

    // Check if a station name exists
    public boolean hasStation(String name) {
        return stationNames.contains(name);
    }

    // Dijkstra finds the path with the lowest total time
    public List<Station> shortestTimePath(String start, String end) {
        // Save the best known time to each station
        Map<Station, Double> dist = new HashMap<>();
        // Save the previous station used to build the path
        Map<Station, Station> prev = new HashMap<>();
        // Queue stations by current total time
        PriorityQueue<Object[]> pq = new PriorityQueue<>((a, b) -> Double.compare((double) a[0], (double) b[0]));
        // Start with a very large time value
        for (Station s : graph.keySet())
            dist.put(s, 999999.0);
        // Get all start line versions
        List<Station> startList = nameToStations.get(start);
        // Stop when the start station is missing
        if (startList == null)
            return new ArrayList<>();
        // Add every start version with zero time
        for (Station s : startList) {
            dist.put(s, 0.0);
            pq.add(new Object[] { 0.0, s });
        }
        // Keep checking the station with the best time
        while (!pq.isEmpty()) {
            // Read the next station from the queue
            Object[] entry = pq.poll();
            double d = (double) entry[0];
            Station u = (Station) entry[1];
            // Skip old queue data
            if (d > dist.get(u))
                continue;

            // Get all next edges
            List<Edge> edges = graph.get(u);
            // Skip when the station has no edges
            if (edges == null)
                continue;

            // Check every next edge
            for (Edge e : edges) {
                Station v = e.to;
                // Set change cost to zero at first
                double changeCost = 0.0;
                // Add two minutes when changing lines at the same station
                if (u.name.equals(v.name) && !u.line.equals(v.line))
                    changeCost = 2.0;

                // Add travel time and change time
                double newDist = dist.get(u) + e.time + changeCost;
                // Update data when the new time is better
                if (newDist < dist.get(v)) {
                    dist.put(v, newDist);
                    prev.put(v, u);
                    pq.add(new Object[] { newDist, v });
                }
            }
        }
        // Pick the best end line version
        List<Station> endList = nameToStations.get(end);
        // Stop when the end station is missing
        if (endList == null)
            return new ArrayList<>();
        Station bestEnd = null;
        double min = 999999.0;
        // Search the end version with lowest time
        for (Station s : endList) {
            if (dist.get(s) < min) {
                min = dist.get(s);
                bestEnd = s;
            }
        }
        // Build the path by moving back through previous stations
        List<Station> path = new ArrayList<>();
        // Send back an empty path when no route exists
        if (bestEnd == null)
            return path;
        Station cur = bestEnd;
        // Move backwards through the path
        while (cur != null) {
            path.add(cur);
            cur = prev.get(cur);
        }
        // Reverse the path to start-to-end sequence
        Collections.reverse(path);
        return path;
    }

    // BFS style search finds the path with least line changes
    public List<Station> fewestChangesPath(String start, String end) {
        // Save the best change count to each station
        Map<Station, Integer> changes = new HashMap<>();
        // Save the previous station used to build the path
        Map<Station, Station> prev = new HashMap<>();
        // Queue stations to visit
        Queue<Station> q = new LinkedList<>();

        // Start with a very large change count
        for (Station s : graph.keySet())
            changes.put(s, 999999);

        // Get all start line versions
        List<Station> startList = nameToStations.get(start);
        // Stop when the start station is missing
        if (startList == null)
            return new ArrayList<>();
        // Add every start version with zero changes
        for (Station s : startList) {
            changes.put(s, 0);
            q.add(s);
        }

        // Visit stations until the queue is empty
        while (!q.isEmpty()) {
            // Read the next station from the queue
            Station u = q.poll();
            // Get all next edges
            List<Edge> edges = graph.get(u);
            // Skip when the station has no edges
            if (edges == null)
                continue;

            // Check every next edge
            for (Edge e : edges) {
                Station v = e.to;
                // Start with no added change
                int add = 0;
                // Count one change when the line changes at the same station
                if (u.name.equals(v.name) && !u.line.equals(v.line))
                    add = 1;

                // Add the new change count
                int newC = changes.get(u) + add;
                // Update data when the new count is better
                if (newC < changes.get(v)) {
                    changes.put(v, newC);
                    prev.put(v, u);
                    q.add(v);
                }
            }
        }

        // Pick the end version with least changes
        List<Station> endList = nameToStations.get(end);
        // Stop when the end station is missing
        if (endList == null)
            return new ArrayList<>();
        Station bestEnd = null;
        int min = 999999;
        // Search the end version with least changes
        for (Station s : endList) {
            if (changes.get(s) < min) {
                min = changes.get(s);
                bestEnd = s;
            }
        }

        // Build the path by moving back through previous stations
        List<Station> path = new ArrayList<>();
        // Send back an empty path when no route exists
        if (bestEnd == null)
            return path;
        Station cur = bestEnd;
        // Move backwards through the path
        while (cur != null) {
            path.add(cur);
            cur = prev.get(cur);
        }
        // Reverse the path to start-to-end sequence
        Collections.reverse(path);
        return path;
    }

    // Print the route and summary
    public void printRoute(List<Station> path, String mode) {
        // Stop when the path is empty
        if (path.isEmpty()) {
            System.out.println("\nNo route found.");
            return;
        }

        // Print a blank line
        System.out.println();
        // Print the title based on selected mode
        if (mode.equals("time"))
            System.out.println("*** Minimal Time Route ***");
        else
            System.out.println("*** Route with Fewest Changes ***");

        // Track total time and line changes
        double totalTime = 0;
        int changeCount = 0;

        // Print each station in the path
        for (int i = 0; i < path.size(); i++) {
            Station s = path.get(i);
            System.out.println(s.name + " on " + s.line + " line");

            // Add time and change count after the first station
            if (i > 0) {
                // Get the previous station in the path
                Station prevSta = path.get(i - 1);
                // Count a line change at the same station
                if (prevSta.name.equals(s.name) && !prevSta.line.equals(s.line))
                    changeCount++;

                // Find the edge that links the two printed stations
                List<Edge> edges = graph.get(prevSta);
                for (Edge e : edges) {
                    if (e.to.name.equals(s.name) && e.to.line.equals(s.line)) {
                        // Add edge time to total time
                        totalTime += e.time;
                        break;
                    }
                }
            }
        }

        // Add change penalty only in time mode
        if (mode.equals("time"))
            totalTime += changeCount * 2.0;

        // Print the final summary
        System.out.println("\nOverall Journey Time (mins) = " + String.format("%.1f", totalTime));
        System.out.println("Number of Changes = " + changeCount);
        System.out.println();
    }

    // Start the interactive program
    public static void main(String[] args) {
        // Create input reader and planner
        Scanner sc = new Scanner(System.in);
        MetrolinkPlanner planner = new MetrolinkPlanner();

        // Ask the user to choose the data file
        System.out.print("Enter data file:(Press enter to default) ");
        String file = sc.nextLine().trim();
        // Use the default file when no file is entered
        if (file.isEmpty())
            file = "Metrolink_times_linecolour.csv";

        // Load the data file
        try {
            planner.loadData(file);
            System.out.println("Data loaded successfully.\n");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        // Keep the menu running until the user exits
        while (true) {
            // Show menu choices
            System.out.println("1) Shortest time");
            System.out.println("2) Fewest changes");
            System.out.println("3) Exit");
            System.out.print("Choose option: ");
            // Read the menu choice
            String op = sc.nextLine().trim();

            // Exit the menu when the user chooses 3
            if (op.equals("3"))
                break;
            // Skip invalid menu choices
            if (!op.equals("1") && !op.equals("2"))
                continue;

            String start, end;
            // Ask again until a valid start station is entered
            do {
                System.out.print("Enter start station: ");
                start = sc.nextLine().trim();
            } while (!planner.hasStation(start));

            // Ask again until a valid end station is entered
            do {
                System.out.print("Enter end station: ");
                end = sc.nextLine().trim();
            } while (!planner.hasStation(end));

            // Run the selected search
            if (op.equals("1")) {
                List<Station> path = planner.shortestTimePath(start, end);
                planner.printRoute(path, "time");
            } else {
                List<Station> path = planner.fewestChangesPath(start, end);
                planner.printRoute(path, "change");
            }
        }
        // Close the input reader
        sc.close();
    }
}
