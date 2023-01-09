package simulated_annealing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
            states.stream().sorted(State::compare).forEach(s -> System.out.println(String.format("\tResult: %.2f units of time", s.getTime())));
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

    public static double calculateSD(List<Double> list) {
        double mean = list.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = list.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0);

        return Math.sqrt(variance);
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
        Heuristics.setBeta(300);

        State s = new State();

        s.addCustomer(new Customer(0, List.of(1, 3)));
        s.addCustomer(new Customer(1, List.of(1)));
        s.addCustomer(new Customer(2, List.of(2)));

        long start = System.currentTimeMillis();
        State result = Heuristics.beamSearch(s);
        printResult(result, System.currentTimeMillis() - start);
    }

    private static void testBeamSearchSeminararbeit() {
        Heuristics.setBeta(10);

        Heuristics.setLogBeamSearch(true);

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

        Heuristics.printBsStats(100);
    }

    private static void testBeamSearchRandomCustomers() {
        Heuristics.setBeta(300);

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
        Heuristics.setAlpha(0.99);
        Heuristics.setMeanMarkov(100);
        Heuristics.setBeta(300);
        int noCustomers = 50;
        int noCounters = 10;
        int minRequest = 1; // minimum no of request per customer
        int maxRequest = 10; // maximum no of request per customer
        boolean uniqueOrders = true; // defines if a customer may have multiple orders of the same kind
        int noStartStates = 10;

        System.out.println("No Start States: " + noStartStates + "\n");
        List<State> startStates = generateRandomStartStates(noCustomers, noCounters, minRequest, maxRequest, uniqueOrders, noStartStates);
        startStates.get(0).printStats();

        long startTime = System.currentTimeMillis();
        List<State> resultStates = startStates.stream().parallel().map(Heuristics::simulatedAnnealing).toList();
        long stopTime = System.currentTimeMillis();

        printResult(resultStates, stopTime - startTime);
    }

    private static void customerSequencingAndCwspCompleteSeminararbeit() {
        Heuristics.setAlpha(0.50);
        Heuristics.setMeanMarkov(2);
        Heuristics.setBeta(300);

        Heuristics.setLogSimulatedAnnealing(true);

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

        Heuristics.printSaStats(10);
    }

    private static void testPriorityBasedSequence() {
        System.out.println("Priority Based Sequence");
        int noCustomers = 50;
        int noCounters = 10;
        int minRequest = 1; // minimum no of request per customer
        int maxRequest = 10; // maximum no of request per customer
        boolean uniqueOrders = true; // defines if a customer may have multiple orders of the same kind
        int noStartStates = 1;

        List<State> startStates = generateRandomStartStates(noCustomers, noCounters, minRequest, maxRequest, uniqueOrders, noStartStates);
        startStates.get(0).printStats();

        long startTime = System.currentTimeMillis();
        State resultState = Heuristics.priorityBasedCustomerSorting(startStates.get(0), noCounters, false);
        long stopTime = System.currentTimeMillis();

        printResult(resultState, stopTime - startTime);
    }

    private static void PriorityBasedSequencingCompared() {
        List<Double> times = new ArrayList<>();
        List<Double> improvements = new ArrayList<>();
        for (int i = 0; i < 100; i++) {

            int noCustomers = 50; // number of customers
            int noCounters = 10; // number of counters
            int minRequests = 1;
            int maxRequest = Math.min(10, noCounters);

            State startState = new State();
            addRandomCustomers(startState, noCustomers, noCounters, minRequests, maxRequest);

            // Simulated annealing algorithm
            long startTime = System.currentTimeMillis();
            State finalState = Heuristics.beamSearch(startState);
            long stopTime = System.currentTimeMillis();

            // printResult(finalState, stopTime - startTime);

            double time = finalState.getTime();
            times.add(time);

            finalState.reset();
            startTime = System.currentTimeMillis();
            State alternativeState = Heuristics.priorityBasedCustomerSorting(finalState, noCounters, true);
            stopTime = System.currentTimeMillis();
            // printResult(alternativeState, stopTime - startTime);

            double timeImprovement = time - alternativeState.getTime();
            improvements.add(timeImprovement);

            System.out.println("Time improvement: " + timeImprovement);
        }

        System.out.println();
        System.out.println("Average Time before: " + times.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        System.out.println("Average Improvement: " + improvements.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        System.out.println("Standard Deviation: " + calculateSD(improvements));

    }

    private static void SAComparedAndPrioCompared() {
        Heuristics.setAlpha(0.99);
        Heuristics.setMeanMarkov(1);
        Heuristics.setBeta(300);

        List<Double> times = new ArrayList<>();
        List<Double> prioImprovements = new ArrayList<>();
        List<Double> saImprovements = new ArrayList<>();
        for (int i = 0; i < 100; i++) {

            int noCustomers = 50; // number of customers
            int noCounters = 10; // number of counters
            int minRequests = 1;
            int maxRequest = Math.min(10, noCounters);

            State startState = new State();
            addRandomCustomers(startState, noCustomers, noCounters, minRequests, maxRequest);

            State finalState = Heuristics.beamSearch(startState);

            double time = finalState.getTime();
            times.add(time);

            finalState.reset();
            State prioState = Heuristics.priorityBasedCustomerSorting(finalState, noCounters, true);
            double prioImprovement = time - prioState.getTime();
            prioImprovements.add(prioImprovement);

            finalState.reset();
            State saState = Heuristics.simulatedAnnealing(finalState);

            double saImprovement = time - saState.getTime();
            saImprovements.add(saImprovement);

            System.out.println();
            System.out.println("Average Time without Optimization: " + times.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
            System.out.println("Time improvement by Prio: " + prioImprovement);
            System.out.println("Time improvement by SA: " + saImprovement);
            System.out.println("Average Improvement by Prio: " + prioImprovements.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
            System.out.println("Average Improvement by SA: " + saImprovements.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
            System.out.println("Standard Deviation Prio: " + calculateSD(prioImprovements));
            System.out.println("Standard Deviation SA: " + calculateSD(saImprovements));
        }
    }

    private static void BeamSearchCompared() {
        List<Long> orderByOrderTimes = new ArrayList<>();
        List<Long> beta3Times = new ArrayList<>();
        List<Long> beta30Times = new ArrayList<>();
        List<Long> beta300Times = new ArrayList<>();
        List<Long> beta3000Times = new ArrayList<>();
        List<Double> orderByOrder = new ArrayList<>();
        List<Double> beta3 = new ArrayList<>();
        List<Double> beta30 = new ArrayList<>();
        List<Double> beta300 = new ArrayList<>();
        List<Double> beta3000 = new ArrayList<>();
        Heuristics.setLogBeamSearch(false);
        for (int i = 0; i < 100; i++) {
            System.out.println("Iteration: " + i);

            int noCustomers = 50; // number of customers
            int noCounters = 10; // number of counters
            int minRequests = 1;
            int maxRequest = Math.min(10, noCounters);

            State resultState;
            long start;
            long stop;

            State startState = new State();
            addRandomCustomers(startState, noCustomers, noCounters, minRequests, maxRequest);
            

            start = System.nanoTime();
            for (int j = 0; j < startState.getCustomers().size(); j++){
                while(!startState.getCustomer(j).getOrders().isEmpty()){
                    startState.serveCustomer(j);
                }
            }
            stop = System.nanoTime();
            orderByOrderTimes.add(stop-start);
            orderByOrder.add(startState.getTime());

            startState.reset();
            Heuristics.setBeta(3000);
            start = System.nanoTime();
            resultState = Heuristics.beamSearch(startState);
            stop = System.nanoTime();
            beta3000.add(resultState.getTime());
            beta3000Times.add(stop - start);

            startState.reset();
            Heuristics.setBeta(300);
            start = System.nanoTime();
            resultState = Heuristics.beamSearch(startState);
            stop = System.nanoTime();
            beta300.add(resultState.getTime());
            beta300Times.add(stop - start);

            startState.reset();
            Heuristics.setBeta(30);
            start = System.nanoTime();
            resultState = Heuristics.beamSearch(startState);
            stop = System.nanoTime();
            beta30.add(resultState.getTime());
            beta30Times.add(stop - start);

            startState.reset();
            Heuristics.setBeta(3);
            resultState = Heuristics.beamSearch(startState);
            stop = System.nanoTime();
            beta3.add(resultState.getTime());
            beta3Times.add(stop - start);
        }

        System.out.println();
        System.out.println("Average Runtime (order by order)[ms]: " + orderByOrderTimes.stream().mapToDouble(t -> t * 1E-6).average().orElse(0.0));
        System.out.println("Average Time (order by order): " + orderByOrder.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        System.out.println("Standard Deviation (order by order): " + calculateSD(orderByOrder));
        System.out.println();
        System.out.println("Average Runtime (Beta=3)[ms]: " + beta3Times.stream().mapToDouble(t -> t * 1E-6).average().orElse(0.0));
        System.out.println("Average Time (Beta=3): " + beta3.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        System.out.println("Standard Deviation (Beta=3): " + calculateSD(beta3));
        System.out.println();
        System.out.println("Average Runtime (Beta=30)[ms]: " + beta30Times.stream().mapToDouble(t -> t * 1E-6).average().orElse(0.0));
        System.out.println("Average Time (Beta=30): " + beta30.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        System.out.println("Standard Deviation (Beta=30): " + calculateSD(beta30));
        System.out.println();
        System.out.println("Average Runtime (Beta=300)[ms]: " + beta300Times.stream().mapToDouble(t -> t * 1E-6).average().orElse(0.0));
        System.out.println("Average Time (Beta=300): " + beta300.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        System.out.println("Standard Deviation (Beta=300): " + calculateSD(beta300));
        System.out.println();
        System.out.println("Average Runtime (Beta=3000)[ms]: " + beta3000Times.stream().mapToDouble(t -> t * 1E-6).average().orElse(0.0));
        System.out.println("Average Time (Beta=3000): " + beta3000.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
        System.out.println("Standard Deviation (Beta=3000): " + calculateSD(beta3000));

    }

    private static void SaCompared() {
        long seed = 1669654886;
        Heuristics.setRandSeed(seed);
        rand.setSeed(seed);

        int noCustomers = 50; // number of customers
        int noCounters = 10; // number of counters
        int minRequests = 1;
        int maxRequest = Math.min(10, noCounters);

        long start;
        long stop;

        File log = new File("SA Comparison 4.txt");
        if (log.exists()) {
            log.delete();
        }

        List<List<State>> states = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            State startState = new State();
            addRandomCustomers(startState, noCustomers, noCounters, minRequests, maxRequest);
            states.add(generateStatePermutations(startState, 4));
        }

        try (FileWriter fw = new FileWriter(log); PrintWriter pw = new PrintWriter(fw)) {

            Heuristics.setBeta(300);
            pw.println("Time (unoptimized): " + states.stream().mapToDouble(s -> Heuristics.beamSearch(s.get(0)).getTime()).average());
            pw.println();
            pw.flush();

            Heuristics.setAlpha(0.50);
            Heuristics.setMeanMarkov(1);
            start = System.nanoTime();
            pw.println("Time (a=50, markov=1): " + states.stream().mapToDouble(s -> Heuristics.simulatedAnnealing(s.get(0)).getTime()).average());
            stop = System.nanoTime();
            pw.println("Runtime (a=50, markov=1): " + ((stop - start) * 1E-6));
            pw.println();
            pw.flush();

            start = System.nanoTime();
            pw.println("Time (a=50, markov=1, Multi-start): " + states.stream().map(s -> s.stream().map(Heuristics::simulatedAnnealing).sorted(State::compare).findFirst().orElse(null)).mapToDouble(State::getTime).average());
            stop = System.nanoTime();
            pw.println("Runtime (a=50, markov=1, Multi-start): " + ((stop - start) * 1E-6));
            pw.println();
            pw.flush();

            Heuristics.setAlpha(0.90);
            Heuristics.setMeanMarkov(1);
            start = System.nanoTime();
            pw.println("Time (a=90, markov=1): " + states.stream().mapToDouble(s -> Heuristics.simulatedAnnealing(s.get(0)).getTime()).average());
            stop = System.nanoTime();
            pw.println("Runtime (a=90, markov=1): " + ((stop - start) * 1E-6));
            pw.println();
            pw.flush();

            start = System.nanoTime();
            pw.println("Time (a=90, markov=1, Multi-start): " + states.stream().map(s -> s.stream().map(Heuristics::simulatedAnnealing).sorted(State::compare).findFirst().orElse(null)).mapToDouble(State::getTime).average());
            stop = System.nanoTime();
            pw.println("Runtime (a=90, markov=1, Multi-start): " + ((stop - start) * 1E-6));
            pw.println();
            pw.flush();

            Heuristics.setAlpha(0.99);
            Heuristics.setMeanMarkov(1);
            start = System.nanoTime();
            pw.println("Time (a=99, markov=1): " + states.stream().mapToDouble(s -> Heuristics.simulatedAnnealing(s.get(0)).getTime()).average());
            stop = System.nanoTime();
            pw.println("Runtime (a=99, markov=1): " + ((stop - start) * 1E-6));
            pw.println();
            pw.flush();

            start = System.nanoTime();
            pw.println("Time (a=99, markov=1, Multi-start): " + states.stream().map(s -> s.stream().map(Heuristics::simulatedAnnealing).sorted(State::compare).findFirst().orElse(null)).mapToDouble(State::getTime).average());
            stop = System.nanoTime();
            pw.println("Runtime (a=99, markov=1, Multi-start): " + ((stop - start) * 1E-6));
            pw.println();
            pw.flush();

            Heuristics.setAlpha(0.50);
            Heuristics.setMeanMarkov(10);
            start = System.nanoTime();
            pw.println("Time (a=50, markov=10): " + states.stream().mapToDouble(s -> Heuristics.simulatedAnnealing(s.get(0)).getTime()).average());
            stop = System.nanoTime();
            pw.println("Runtime (a=50, markov=10): " + ((stop - start) * 1E-6));
            pw.println();
            pw.flush();

            start = System.nanoTime();
            pw.println("Time (a=50, markov=10, Multi-start): " + states.stream().map(s -> s.stream().map(Heuristics::simulatedAnnealing).sorted(State::compare).findFirst().orElse(null)).mapToDouble(State::getTime).average());
            stop = System.nanoTime();
            pw.println("Runtime (a=50, markov=10, Multi-start): " + ((stop - start) * 1E-6));
            pw.println();
            pw.flush();

            Heuristics.setAlpha(0.90);
            Heuristics.setMeanMarkov(10);
            start = System.nanoTime();
            pw.println("Time (a=90, markov=10): " + states.stream().mapToDouble(s -> Heuristics.simulatedAnnealing(s.get(0)).getTime()).average());
            stop = System.nanoTime();
            pw.println("Runtime (a=90, markov=10): " + ((stop - start) * 1E-6));
            pw.println();
            pw.flush();

            start = System.nanoTime();
            pw.println("Time (a=90, markov=10, Multi-start): " + states.stream().map(s -> s.stream().map(Heuristics::simulatedAnnealing).sorted(State::compare).findFirst().orElse(null)).mapToDouble(State::getTime).average());
            stop = System.nanoTime();
            pw.println("Runtime (a=90, markov=10, Multi-start): " + ((stop - start) * 1E-6));
            pw.println();
            pw.flush();

            Heuristics.setAlpha(0.99);
            Heuristics.setMeanMarkov(10);
            start = System.nanoTime();
            pw.println("Time (a=99, markov=10): " + states.stream().mapToDouble(s -> Heuristics.simulatedAnnealing(s.get(0)).getTime()).average());
            stop = System.nanoTime();
            pw.println("Runtime (a=99, markov=10): " + ((stop - start) * 1E-6));
            pw.println();
            pw.flush();

            Heuristics.setAlpha(0.50);
            Heuristics.setMeanMarkov(100);
            start = System.nanoTime();
            pw.println("Time (a=50, markov=100): " + states.stream().mapToDouble(s -> Heuristics.simulatedAnnealing(s.get(0)).getTime()).average());
            stop = System.nanoTime();
            pw.println("Runtime (a=50, markov=100): " + ((stop - start) * 1E-6));
            pw.println();
            pw.flush();

            start = System.nanoTime();
            pw.println("Time (a=50, markov=100, Multi-start): " + states.stream().map(s -> s.stream().map(Heuristics::simulatedAnnealing).sorted(State::compare).findFirst().orElse(null)).mapToDouble(State::getTime).average());
            stop = System.nanoTime();
            pw.println("Runtime (a=50, markov=100, Multi-start): " + ((stop - start) * 1E-6));
            pw.println();
            pw.flush();

            Heuristics.setAlpha(0.90);
            Heuristics.setMeanMarkov(100);
            start = System.nanoTime();
            pw.println("Time (a=90, markov=100): " + states.stream().mapToDouble(s -> Heuristics.simulatedAnnealing(s.get(0)).getTime()).average());
            stop = System.nanoTime();
            pw.println("Runtime (a=90, markov=100): " + ((stop - start) * 1E-6));
            pw.println();
            pw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        long seed = 42; // "Answer to the Ultimate Question of Life, the Universe, and Everything" ... and a good seed
        Heuristics.setRandSeed(seed);
        rand.setSeed(seed);

        // --- test functions
        // testServeCustomer();
        // testBeamSearchFixedCustomers();
        // testBeamSearchSeminararbeit();
        // testBeamSearchRandomCustomers();
        // PriorityBasedSequencingCompared();
        // SAComparedAndPrioCompared();

        // customerSequencingAndCwspCompleteSeminararbeit();
        // customerSequencingAndCwspComplete();
        // testPriorityBasedSequence();

        BeamSearchCompared();
        // SaCompared();
    }

}
