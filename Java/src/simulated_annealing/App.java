package simulated_annealing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

public class App {
    private static Random rand = new Random();

    private static List<State> generateStatePermutations(State state, int noStartStates) {
        List<State> startStates = new ArrayList<>();
        startStates.add(state); // add first generate state

        IntStream.range(1, noStartStates).forEach(i -> {
            State newState = new State(state);
            startStates.add(newState);

            newState.getCustomers().clear();
            List<Integer> positions = new ArrayList<>(IntStream.range(0, state.getNoCustomers()).boxed().toList());
            int newNo = 0;
            while (!positions.isEmpty()) {
                // add randomly selected customer of original state
                newState.addCustomer(new Customer(state.getCustomer(positions.remove(rand.nextInt(positions.size()))), newNo++));
            }
        });

        return startStates;
    }

    private static void printResult(State state, long runtime) {
        int offset = 1; // offset to make comparison easier (convert from index to id)
        System.out.println(String.format("Runtime: %.3f sec", runtime / 1000.0));

        System.out.println(String.format("%nServing time: %.2f", state.getServingTime()));
        System.out.println(String.format("Waiter velocity: %.2f", state.getWaiterVelocity()));
        System.out.println("Customer velocities: [" + String.join(" | ", state.getCustomers().stream().map(c -> String.format("%.2f", c.getVelocity())).toList()) + "]");

        System.out.println(String.format("%nBest Result: %.2f units of time, %.2f walked units of distance%n", state.getTime(), state.getWalkedDistance()));

        System.out.println("\tCustomer Order:");

        state.getCustomers().stream().map(c -> String.format("\t\t%2d: original-position = %2d, Orders = ", (c.getId() + offset), (c.getInitialId() + offset)) + c.getInitialOrders().stream().map(o -> o + offset).toList())
                .forEach(System.out::println);

        System.out.println("\n\tWaiter schedule:");
        state.getWaiterSchedule().stream().forEach(w -> System.out.println(String.format("\t\tCustomer %2d served at counter %2d", (w[0] + offset), (w[1] + offset))));
    }

    private static void printResult(List<State> states, long runtime) {
        State bestState = states.stream().sorted(State::compare).findFirst().orElse(new State());

        if (states.stream().filter(s -> bestState.compareTo(s) < 0).count() == 0) {
            System.out.println("All start states lead to an equal optimal solution. First solution will be shown in detail.");
        } else {
            System.out.println("Start states produced states with different optimality. Best solution will be shown in detail.");
            states.stream().sorted(State::compare).forEach(s -> System.out.println("\tResult: " + s.getTime() + " sec"));
        }

        printResult(bestState, runtime);
    }

    private static void addRandomCustomers(State state, int noCustomers, int noCounters, int minRequests, int maxRequests) {
        for (int i = 0; i < noCustomers; i++) {
            int noDishes = rand.nextInt(maxRequests - minRequests + 1) + minRequests;
            Set<Integer> order = new HashSet<>();
            while (order.size() < noDishes) {
                order.add(rand.nextInt(noCounters));
            }
            state.addCustomer(new Customer(i, new ArrayList<>(order)));
        }
    }

    private static List<State> generateRandomStartStates(int noCustomers, int noCounters, int minRequest, int maxRequest, boolean uniqueOrders, int noStartStates) {
        if (uniqueOrders && (maxRequest > noCounters || minRequest > noCounters)) {
            throw new IllegalArgumentException("There must not be more requests than counters!");
        }

        State firstState = new State();
        for (int i = 0; i < noCustomers; i++) {
            int noDishes = rand.nextInt(maxRequest - minRequest + 1) + minRequest;
            if (uniqueOrders) {
                Set<Integer> order = new HashSet<>();
                while (order.size() < noDishes) {
                    order.add(rand.nextInt(noCounters));
                }
                firstState.addCustomer(new Customer(i, new ArrayList<>(order)));
            } else {
                List<Integer> order = new ArrayList<>();
                for (int d = 0; d < noDishes; d++) {
                    order.add(rand.nextInt(noCounters));
                }
                order.sort(Integer::compare);
                firstState.addCustomer(new Customer(i, order));
            }
        }

        return generateStatePermutations(firstState, noStartStates);
    }

    private static void testServeCustomer() {
        // Test of serve Customer function
        State s = new State();

        s.addCustomer(new Customer(0, List.of(1, 3)));
        s.addCustomer(new Customer(1, List.of(1)));
        s.addCustomer(new Customer(2, List.of(2)));

        List<Integer> servedOrder = List.of(0, 1, 0, 2);

        for (int indexNextCustomer : servedOrder) {
            s.serveCustomer(indexNextCustomer);
            System.out.println("Time: " + s.getTime());
        }
    }

    private static void testBeamSearchFixedCustomers() {
        // Test of Beams Search
        State s = new State();

        s.addCustomer(new Customer(0, List.of(1, 3)));
        s.addCustomer(new Customer(1, List.of(1)));
        s.addCustomer(new Customer(2, List.of(2)));

        long start = System.currentTimeMillis();
        State result = Heuristics.beamSearch(s);
        printResult(result, System.currentTimeMillis() - start);
    }

    private static void testBeamSearchSeminararbeit() {
        // Test of Beams Search
        State s = new State();

        // ======== Beispiel Seminararbeit ========
        s.addCustomer(new Customer(0, List.of(1)));
        s.addCustomer(new Customer(1, List.of(1, 2, 3)));
        s.addCustomer(new Customer(2, List.of(0, 1)));
        s.addCustomer(new Customer(3, List.of(1, 3)));
        // ======== Ende des Beispiels ========

        long start = System.currentTimeMillis();
        State result = Heuristics.beamSearch(s);
        printResult(result, System.currentTimeMillis() - start);
    }

    private static void testBeamSearchRandomCustomers() {

        int noCustomers = 10; // number of customers
        int noCounters = 10; // number of counters
        int minRequests = 1;
        int maxRequest = Math.min(10, noCounters);

        State startState = new State();
        addRandomCustomers(startState, noCustomers, noCounters, minRequests, maxRequest);

        // Simulated annealing algorithm
        long startTime = System.currentTimeMillis();
        State finalState = Heuristics.beamSearch(startState);
        long stopTime = System.currentTimeMillis();

        printResult(finalState, stopTime - startTime);
    }

    private static void customerSequencingAndCwspComplete() {
        int noCustomers = 10;
        int noCounters = 10;
        int minRequest = 1; // minimum no of request per customer
        int maxRequest = 10; // maximum no of request per customer
        boolean uniqueOrders = true; // defines if a customer may have multiple orders of the same kind
        int noStartStates = 1;

        System.out.println("No Start States: " + noStartStates + "\n");
        List<State> startStates = generateRandomStartStates(noCustomers, noCounters, minRequest, maxRequest, uniqueOrders, noStartStates);
        startStates.get(0).printStats();

        long startTime = System.currentTimeMillis();
        List<State> resultStates = startStates.stream().parallel().map(Heuristics::simulatedAnnealing).toList();
        long stopTime = System.currentTimeMillis();

        printResult(resultStates, stopTime - startTime);
    }

    private static void customerSequencingAndCwspCompleteSeminararbeit() {
        int noStartStates = 1;

        System.out.println("No Start States: " + noStartStates + "\n");

        // ======== Beispiel aus Seminararbeit ========
        State state = new State();
        state.addCustomer(new Customer(0, new ArrayList<>(List.of(1, 3))));
        state.addCustomer(new Customer(1, new ArrayList<>(List.of(0, 1))));
        state.addCustomer(new Customer(2, new ArrayList<>(List.of(1, 2, 3))));
        state.addCustomer(new Customer(3, new ArrayList<>(List.of(1))));
        List<State> startStates = generateStatePermutations(state, noStartStates);
        startStates.get(0).printStats();
        // ======== Ende Beispiel ========

        // Simulated annealing algorithm
        long startTime = System.currentTimeMillis();
        List<State> resultStates = startStates.stream().parallel().map(Heuristics::simulatedAnnealing).toList();
        long stopTime = System.currentTimeMillis();

        printResult(resultStates, stopTime - startTime);
    }

    public static void main(String[] args) {
        long seed = 42; // "Answer to the Ultimate Question of Life, the Universe, and Everything" ... and a good seed
        Heuristics.setRandSeed(seed);
        rand.setSeed(seed);
        Heuristics.setAlpha(0.99);
        Heuristics.setMeanMarkov(100);
        Heuristics.setBeta(300);

        Heuristics.setLogSimulatedAnnealing(false);

        Heuristics.printParameters();

        // --- test functions
        // testServeCustomer();
        // testBeamSearchFixedCustomers();
        // testBeamSearchSeminararbeit();
        // testBeamSearchRandomCustomers();

        // customerSequencingAndCwspCompleteSeminararbeit();
        customerSequencingAndCwspComplete();

        // Heuristics.printSaStats(10);
    }

}
