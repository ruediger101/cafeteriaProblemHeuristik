package simulated_annealing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Heuristics {
    private static Random rand = new Random();
    private static int beta = 300; // Beam search width
    private static double initialTemp = 50.0; // set the initial annealing temperature
    private static double finalTemp = 1.0; // set the ending/stop annealing temperature
    private static double alpha = 0.99; // set the cooling parameters ,T(k)=alpha*T(k-1)
    private static int meanMarkov = 100; // Markov Chain length, that is the number of internal circulation runs
    private static boolean logBeamSearch = false;
    private static boolean logSimulatedAnnealing = false;

    // =======================
    // ==== SA Statistics ====
    // =======================
    private static List<State> recordCurrentState = new ArrayList<>();
    private static List<State> recordOptimalState = new ArrayList<>();
    private static List<Long> recordNoBetter = new ArrayList<>();
    private static List<Double> recordPBad = new ArrayList<>(); // the acceptance probability of the inferior solution

    private Heuristics() {
        throw new IllegalStateException("Utility class");
    }

    public static void setRandSeed(long seed) {
        Heuristics.rand.setSeed(seed);
    }

    public static int getBeta() {
        return Heuristics.beta;
    }

    public static void setBeta(int beta) {
        Heuristics.beta = beta;
    }

    public static int getMeanMarkov() {
        return Heuristics.meanMarkov;
    }

    public static void setMeanMarkov(int length) {
        Heuristics.meanMarkov = length;
    }

    public static double getInitialTemp() {
        return Heuristics.initialTemp;
    }

    public static void setInitialTemp(double temp) {
        Heuristics.initialTemp = temp;
    }

    public static double getFinalTemp() {
        return Heuristics.finalTemp;
    }

    public static void setFinalTemp(double temp) {
        Heuristics.finalTemp = temp;
    }

    public static double getAlpha() {
        return Heuristics.alpha;
    }

    public static void setAlpha(double alpha) {
        Heuristics.alpha = alpha;
    }

    public static boolean isLogBeamSearch() {
        return logBeamSearch;
    }

    public static void setLogBeamSearch(boolean log) {
        Heuristics.logBeamSearch = log;
    }

    public static boolean isLogSimulatedAnnealing() {
        return logSimulatedAnnealing;
    }

    public static void setLogSimulatedAnnealing(boolean log) {
        Heuristics.logSimulatedAnnealing = log;
    }

    public static void printSaStats(int noResults) {
        for (int i = 0; i < recordNoBetter.size() - 1; i += Math.max(1, (recordNoBetter.size() + 0.5) / noResults)) {
            System.out.println("\nOuter loop no " + (i));
            System.out.println("\tNo of found better solutions: " + recordNoBetter.get(i));
            System.out.println(String.format("\tCurrentState: time = %.2f\tdistance = %.2f", recordCurrentState.get(i).getTime(), recordCurrentState.get(i).getWalkedDistance()));
            System.out.println(String.format("\tOptimal State: time = %.2f\tdistance = %.2f", recordOptimalState.get(i).getTime(), recordOptimalState.get(i).getWalkedDistance()));
            System.out.println(String.format("\tPercentage of accepted bad solutions = %.2f", recordPBad.get(i)));
        }

        // print last state
        int i = recordOptimalState.size() - 1;
        System.out.println("Outer loop no " + (i));
        System.out.println("\tNo of found better solutions: " + recordNoBetter.get(i));
        System.out.println(String.format("\tCurrentState: time = %.2f\tdistance = %.2f", recordCurrentState.get(i).getTime(), recordCurrentState.get(i).getWalkedDistance()));
        System.out.println(String.format("\tOptimal State: time = %.2f\tdistance = %.2f", recordOptimalState.get(i).getTime(), recordOptimalState.get(i).getWalkedDistance()));
        System.out.println(String.format("\tPercentage of accepted bad solutions = %.2f", recordPBad.get(i)));

    }

    public static void printParameters() {
        int outerLoopIterations = (int) Math.ceil(Math.log(finalTemp / initialTemp) / Math.log(alpha));
        System.out.println(String.format("%nInitial Temperature: %.2f", initialTemp));
        System.out.println(String.format("Final Temperature: %.2f", finalTemp));
        System.out.println(String.format("Alpha: %.3f", alpha));
        System.out.println("Outer loop (temp reduction): " + outerLoopIterations + " iterations");
        System.out.println("Inner loop (Markov Chain): " + meanMarkov + " iterations");
        System.out.println("Total (outer x inner loop): " + outerLoopIterations * meanMarkov + " iterations");
        System.out.println();
        System.out.println("Beta: " + beta + "\n");
    }

    // Beam search
    public static State beamSearch(State state) {
        state.reset();

        int totalOrders = state.getCustomers().stream().mapToInt(c -> c.getOrders().size()).sum();

        List<State> currentLevel = new ArrayList<>(List.of(state));// list containing states at current level
        for (int i = 0; i < totalOrders; i++) { // the tree must be expanded once for every order to finish all orders
            List<State> newLevel = currentLevel.stream().parallel().map(s -> s.getCandidates().stream().map(c -> {
                State nextState = new State(s);
                nextState.serveCustomer(c);
                return nextState;
            }).toList()).flatMap(List::stream).toList();

            if (newLevel.size() > beta) {
                currentLevel = new ArrayList<>(newLevel.stream().sorted(State::compare).toList()).subList(0, beta);
            } else {
                currentLevel = newLevel;
            }
        }

        return currentLevel.stream().sorted(State::compare).toList().get(0); // we only return the best result
    }

    // Simulated Annealing
    public static State simulatedAnnealing(State state) {
        State initialState = beamSearch(state);

        // ====== Simulated annealing algorithm initialization ======
        State currentState = new State(initialState);
        State optimalState = new State(initialState);
        int nMarkov = meanMarkov; // use fixed length Markov chain

        // ====================
        // ==== Statistics ====
        // ====================
        recordCurrentState.clear();
        recordOptimalState.clear();
        recordNoBetter.clear();
        recordPBad.clear();
        long kIter = 0; // the number of iterations of the outer loop , the number of temperature states, can be used to reduce relaxations
        long totalMarkov = 0; // total markov chain length, useful if variable length chain is used
        long totalImprove = 0; // the number of found improvements

        // ====================================================
        // ====== Start simulated annealing optimization ======
        // ====================================================
        // Outer loop , Until the current temperature reaches the end temperature
        double tempNow = initialTemp; // initialize the current temperature (current temperature)

        while (tempNow >= finalTemp) { // outer loop , until the current temperature reaches the end temperature
            // int kBetter = 0; // the number of times a good solution is obtained
            int kBadAccept = 0; // the number of times a bad solution is accepted
            int kBadRefuse = 0; // the number of times a bad solution is rejected
            double scale = 1.0; // limit search radius gradually

            // To achieve thermal equilibrium at the current temperature, iterate nMarkov times
            for (int k = 0; k < nMarkov; k++) {
                totalMarkov++;

                // --- a new solution is generated by randomly switching customers
                // first element to switch
                int s1 = rand.nextInt(state.getNoCustomers());
                // select other customer at (postion + randNo[1, noCustomers-1]) mod noCustomers
                int s2 = (rand.nextInt(state.getNoCustomers() - 1) + 1 + s1) % state.getNoCustomers();

                // switch customers and update their respective position in line
                State modifiedState = new State(currentState);
                modifiedState.getCustomers().set(s1, new Customer(currentState.getCustomer(s2), currentState.getCustomer(s1).getId()));
                modifiedState.getCustomers().set(s2, new Customer(currentState.getCustomer(s1), currentState.getCustomer(s2).getId()));

                // calculate the new time required to finish all orders
                State nextState = beamSearch(modifiedState);

                // Accept judgment ： according to Metropolis this criteria decides whether to accept the new interpretation
                boolean accept;
                if (nextState.compareTo(currentState) < 0) {
                    // better solution： If the objective function of the new solution is better than the current solution, it's accepted
                    accept = true;
                    // kBetter++;
                } else {
                    // tolerance solution ： If the objective function of the new solution is worse than the current solution,
                    // the new solution is accepted with a certain probability
                    double deltaTime = nextState.getTime() - currentState.getTime();
                    double pAccept = Math.exp(-deltaTime / tempNow); // calculate the state transition probability of the tolerant solution
                    if (pAccept > rand.nextDouble()) {
                        // accept the bad solution
                        accept = true;
                        kBadAccept++;
                    } else {
                        // refuse inferior solutions
                        accept = false;
                        kBadRefuse++;
                    }
                }

                // Save the new solution
                if (accept) {
                    // if the new solution was accepted, ti is saved as the current solution
                    currentState = nextState;
                    if (nextState.compareTo(optimalState) < 0) {
                        // if the objective function of the new solution is better than the optimal solution to this point,
                        // the new solution is saved as the optimal solution
                        optimalState = nextState;
                        totalImprove++;
                        scale = scale * 0.99; // Variable search step size , Gradually reduce the search scope , Improve search accuracy
                    }
                }
            }
            // --- Data processing after the end of the inner loop

            // Slow down to a new temperature according to the cooling curve ：T(k)=alpha*T(k-1)
            tempNow = tempNow * alpha;
            kIter++;

            // =======================================
            // ==== Save data for optional output ====
            // =======================================
            if (logSimulatedAnnealing) {
                recordNoBetter.add(totalImprove);
                double pBadAccept = ((double) kBadAccept) / ((kBadAccept + kBadRefuse) != 0 ? (kBadAccept + kBadRefuse) : 1); // the acceptance probability of the inferior solution
                recordCurrentState.add(currentState); // the objective function value of the current solution
                recordOptimalState.add(optimalState); // the objective function value of the best solution
                recordPBad.add(pBadAccept); // the objective function value of the best solution
            }

        }
        // ====== End the simulated annealing process ======
        return optimalState;
    }

    private static int compareCustomers(Customer c, Customer c2, State state) {

        for (int i = 0; i < Math.min(c.getOrders().size(), c2.getOrders().size()); i++) {
            long no1 = 0;
            long no2 = 0;

            for (Customer cust : state.getCustomers()) {
                if (cust.getOrders().size() > i) {
                    if (!cust.equals(c) && c.getOrders().get(i) <= cust.getOrders().get(i))
                        no1++;
                    if (!cust.equals(c2) && c2.getOrders().get(i) <= cust.getOrders().get(i))
                        no2++;
                }
            }

            int result = Long.compare(no1, no2);
            if (true || result != 0) {
                return result;
            }
        }
        return Integer.compare(c.getOrders().size(), c2.getOrders().size());
    }

    public static State priorityBasedCustomerSorting(State state, int noCounters) {
        List<Customer> sorted = state.getCustomers().stream().sorted((a, b) -> compareCustomers(a, b, state)).toList();

        List<List<Customer>> clusters = new ArrayList<>();
        for (int i = 0; i < noCounters; i++) {
            List<Customer> temp = new ArrayList<>();
            for (int j = i; j < sorted.size(); j += noCounters) {
                temp.add(sorted.get(j));
            }
            clusters.add(temp);
        }

        state.getCustomers().clear();
        while (!clusters.isEmpty()) {
            int index = rand.nextInt(clusters.size());
            state.getCustomers().addAll(clusters.remove(index));
        }

        for (int i = 0; i < state.getCustomers().size(); i++) {
            state.getCustomers().get(i).setId(i);
        }

        return Heuristics.beamSearch(state);
    }
}