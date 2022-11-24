package simulated_annealing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Heuristics {
    private static Random rand = new Random();
    private static int beta = 300; // Beam search width
    private static double initialTemp = 50.0; // set the initial annealing temperature
    private static double finalTemp = 1.0; // set the ending/stop annealing temperature
    private static double alpha = 0.99; // set the cooling parameters ,T(k)=alpha*T(k-1)
    private static int meanMarkov = 100; // Markov Chain length, that is the number of internal circulation runs

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

    public static void printParameters() {
        System.out.println("\nBeta: " + beta);

        int outerLoopIterations = (int) Math.ceil(Math.log(finalTemp / initialTemp) / Math.log(alpha));
        System.out.println("Initial Temperature: " + initialTemp);
        System.out.println("Final Temperature: " + finalTemp);
        System.out.println("Alpha: " + alpha);
        System.out.println("Outer loop (temp reduction): " + outerLoopIterations + " iterations");
        System.out.println("Inner loop (Markov Chain): " + meanMarkov + " iterations");
        System.out.println("Total (outer * inner loop): " + outerLoopIterations * meanMarkov + " iterations\n");
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
        // List<Long> recordIter = new ArrayList<>();
        // List<State> recordCurrentState = new ArrayList<>();
        // List<State> recordOptimalState = new ArrayList<>();
        // List<Double> recordPBad = new ArrayList<>(); // the acceptance probability of the inferior solution
        long kIter = 0; // the number of iterations of the outer loop , the number of temperature states
        long totalMarkov = 0; // total markov chain length
        long totalImprove = 0; // the number of found improvements
        int nMarkov = meanMarkov; // fixed length Markov chain

        // ====== Start simulated annealing optimization ======
        // Outer loop , Until the current temperature reaches the end temperature
        double tempNow = initialTemp; // initialize the current temperature (current temperature)

        while (tempNow >= finalTemp) { // outer loop , until the current temperature reaches the end temperature
            // To achieve thermal equilibrium at the current temperature, iterate multiple times (nMarkov)
            int kBetter = 0; // the number of times a good solution is obtained
            int kBadAccept = 0; // the number of times a bad solution is accepted
            int kBadRefuse = 0; // the number of times a bad solution is rejected
            // double scale = 1.0; // limit search radius gradually
            // --- Inner loop , the number of cycles is Markov Chain length
            for (int k = 0; k < nMarkov; k++) {
                totalMarkov++;

                // --- a new solution is generated by random perturbation near the current solution
                int s1 = rand.nextInt(state.getNoCustomers()); // first element to switch
                // int s2 = IntStream.rangeClosed(0, state.customers.size()).filter(i -> i != s1).toArray()[rand.nextInt(state.customers.size() - 1)];
                int s2 = (rand.nextInt(state.getNoCustomers() - 1) + 1 + s1) % state.getNoCustomers(); // select other customer at (postion + randNo[1, noCustomers-1]) mod noCustomers

                // switch customers and update their respective position in line
                State modifiedState = new State(currentState);
                modifiedState.getCustomers().set(s1, new Customer(currentState.getCustomer(s2), currentState.getCustomer(s1).getId()));
                modifiedState.getCustomers().set(s2, new Customer(currentState.getCustomer(s1), currentState.getCustomer(s2).getId()));

                // --- calculate the new time required to finish all orders
                State nextState = beamSearch(modifiedState);

                // --- Press Metropolis The criteria accept new interpretations
                // Accept judgment ： according to Metropolis the criteria decides whether to accept the new interpretation
                boolean accept;
                if (nextState.getTime() < currentState.getTime()) { // better solution： If the objective function of the new solution is better than the current solution, it's accepted
                    accept = true;
                    kBetter++;
                } else { // tolerance solution ： If the objective function of the new solution is worse than the current solution, the new solution is accepted with a certain
                         // probability
                    double deltaTime = nextState.getTime() - currentState.getTime();
                    double pAccept = Math.exp(-deltaTime / tempNow); // calculate the state transition probability of the tolerant solution
                    if (pAccept > rand.nextDouble()) {
                        accept = true; // accept the bad solution
                        kBadAccept++;
                    } else {
                        accept = false; // refuse inferior solutions
                        kBadRefuse++;
                    }
                }

                // Save the new solution
                if (accept) { // if you accept the new explanation, the new solution is saved as the current solution
                    currentState = nextState;
                    if (nextState.getTime() < optimalState.getTime()) { // if the objective function of the new solution is better than the optimal solution till now, then the new solution is saved as the optimal solution
                        optimalState = nextState;
                        totalImprove++;
                        // scale = scale*0.99 // Variable search step size , Gradually reduce the search scope , Improve search accuracy
                    }
                }
            }
            // --- Data processing after the end of the inner loop
            // Complete the current temperature search, save data and output
            double pBadAccept = ((double) kBadAccept) / ((kBadAccept + kBadRefuse) != 0 ? (kBadAccept + kBadRefuse) : 1); // the acceptance probability of the inferior solution
            // recordIter.add(kIter); // the current number of external loops
            // recordCurrentState.add(currentState); // the objective function value of the current solution
            // recordOptimalState.add(optimalState); // the objective function value of the best solution
            // recordPBad.add(pBadAccept); // the objective function value of the best solution
            // Slow down to a new temperature according to the cooling curve ：T(k)=alpha*T(k-1)
            tempNow = tempNow * alpha;
            kIter++;
            // fxBest = cal_Energy(xBest, nVar, kIter) // Because the penalty factor increases after iteration , Then we need to reconstruct the augmented objective
            // function

            /*
             * if (recordOptimalState.size() > 1 && recordOptimalState.get(recordOptimalState.size() - 1).time < recordOptimalState.get(recordOptimalState.size() - 2).time)
             * System.err.println("Better solution found: " + optimalState.time + " sec");
             */

        }
        // ====== End the simulated annealing process ======
        return optimalState;
    }
}