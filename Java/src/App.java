import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

public class App {
    private static Random rand = new Random();

    // function checks if order must not be changed
    private static boolean mustPrecede(Customer i, Customer j) {
        if (i.getId() == j.getId()) // could be removed due to implementation only left to be in line with original paper
            return i.getOrders().get(0) < j.getOrders().get(0);
        else if (i.getId() < j.getId())
            return i.getOrders().get(0) <= j.getOrders().get(0);
        else
            return j.getOrders().get(0) < i.getOrders().get(0) && i.getOrders().get(0) <= j.getOrders().get(0) + (j.getId() - i.getId() - 1);
    }

    // function to move all customers for a given order to process
    private static State serveCustomer(double vWaiter, double vCustomer, double tServing, State state, int indexNextCustomer) {
        Customer nextCustomer = state.getCustomers().get(indexNextCustomer);
        int servedOrderAt = nextCustomer.getOrders().remove(0);

        state.getWaiterSchedule().add(new int[] { nextCustomer.getId(), servedOrderAt });

        double walkedDistance = Math.abs(servedOrderAt - state.getWaiterPosition());
        state.incWalkedDistance(walkedDistance);

        double deltaTime = Math.max(servedOrderAt - nextCustomer.getPosition() / vCustomer, walkedDistance / vWaiter) + tServing; // time required to serve customer

        state.incTime(deltaTime);
        state.setWaiterPosition(servedOrderAt);
        nextCustomer.setPosition(servedOrderAt);

        // update customer position of each of the n customers
        double previousPosition = Double.MAX_VALUE;
        for (Iterator<Customer> it = (new ArrayList<>(state.getCustomers())).iterator(); it.hasNext();) {
            Customer c = it.next();
            if (!nextCustomer.equals(c)) {
                c.setPosition(Math.min(c.getPosition() + deltaTime * vCustomer, previousPosition - 1));// update position ignoring next order

                if (!c.getOrders().isEmpty()) { // take order into account if one exists
                    c.setPosition(Math.min(c.getOrders().get(0), c.getPosition()));
                }
            }
            previousPosition = c.getPosition(); // save as previous position to check for blocking of next customer
        }

        return state;
    }

    private static void testServeCustomer() {
        // Test of serve Customer function
        State s = new State();

        int pos = -1;
        s.addCustomer(new Customer(0, List.of(1, 3)));
        s.addCustomer(new Customer(1, List.of(1)));
        s.addCustomer(new Customer(2, List.of(2)));

        List<Integer> servedOrder = List.of(0, 1, 0, 2);
        double vWaiter = 1.0;
        double vCustomer = 1.0;
        double tServing = 2.0;

        for (int indexNextCustomer : servedOrder) {
            State newState = serveCustomer(vWaiter, vCustomer, tServing, s, indexNextCustomer);
            System.out.println("Time: " + newState.getTime());
        }
    }

    private static List<Integer> getCandidates(State state) {
        List<Integer> candidates = new ArrayList<>();
        if (!state.getCustomer(0).getOrders().isEmpty()) { // first customer is always possible if order are present
            candidates.add(0);
        }

        IntStream.range(1, state.getNoCustomers()).forEach(j -> {
            if (!state.getCustomer(j).getOrders().isEmpty()
                    && IntStream.range(0, j)
                            .allMatch(i -> state.getCustomer(i).getOrders().isEmpty() || !mustPrecede(state.getCustomer(i), state.getCustomer(j)))) {
                candidates.add(j);
            }
        });

        return candidates;
    }

    // Beam search
    private static State beamSearch(int beta, double vWaiter, double vCustomer, double tServing, State state) {
        state.reset();

        int totalOrders = state.getCustomers().stream().mapToInt(c -> c.getOrders().size()).sum();

        List<State> currentLevel = new ArrayList<>(List.of(state));// list containing states at current level
        for (int i = 0; i < totalOrders; i++) { // the tree must be expanded once for every order to finish all orders
            List<State> newLevel = currentLevel.stream().parallel().map(s -> getCandidates(s).stream().map(c -> serveCustomer(vWaiter, vCustomer, tServing, new State(s), c)).toList()).flatMap(List::stream).toList();

            if (newLevel.size() > beta) {
                currentLevel = new ArrayList<>(newLevel.stream().sorted(State::compare).toList()).subList(0, beta);
            } else {
                currentLevel = newLevel;
            }
        }

        return currentLevel.stream().sorted(State::compare).toList().get(0); // we only return the best result
    }

    private static void testBeamSearchFixedCustomers() {
        // Test of Beams Search
        State s = new State();

        s.addCustomer(new Customer(0, List.of(1, 3)));
        s.addCustomer(new Customer(1, List.of(1)));
        s.addCustomer(new Customer(2, List.of(2)));

        // ======== Beispiel Seminararbeit ========
        // s.addCustomer(new Customer(0, List.of(0, 1)));
        // s.addCustomer(new Customer(1, List.of( 1)));
        // s.addCustomer(new Customer(2, List.of(1, 2, 3)));
        // s.addCustomer(new Customer(3, List.of(1, 3)));
        // ======== Ende des Beispiels ========

        double vWaiter = 1.0;
        double vCustomer = 1.0;
        double tServing = 2.0;
        int beta = 300;

        State result = beamSearch(beta, vWaiter, vCustomer, tServing, s);
        System.out.println("Time: " + result.getTime() + " waiterSchedule: " + String.join(" ", result.getWaiterSchedule().stream().map(o -> "[" + o[0] + "," + o[1] + "]").toList()));
    }

    private static void testBeamSearch() {
        int beta = 300; // beams search width

        System.out.println("Beta: " + beta + "\n\n");

        int n = 10; // number of customers
        int m = 10; // number of counters
        int minRequest = 1;
        int maxRequest = Math.min(10, m);
        double vWaiter = 1.0;
        double vCustomer = 1.0;
        double tServing = 2.0;

        State startState = new State();
        for (int i = 0; i < n; i++) {
            int noDishes = rand.nextInt(maxRequest - minRequest + 1) + minRequest;
            Set<Integer> order = new HashSet<>();
            while (order.size() < noDishes) {
                order.add(rand.nextInt(m));
            }
            startState.addCustomer(new Customer(i, new ArrayList<>(order)));
        }

        // Simulated annealing algorithm
        long startTime = System.currentTimeMillis();
        State finalState = beamSearch(beta, vWaiter, vCustomer, tServing, startState);
        long stopTime = System.currentTimeMillis();

        System.out.println("Runtime: " + (stopTime - startTime) / 1000.0 + " sec");
        System.out.println("Time: " + finalState.getTime() + " sec");
        System.out.println("\n\tWaiter schedule:" + finalState.getWaiterSchedule().stream().map(w -> String.format("%n\t\t Customer ID %2d served at counter %2d", w[0], w[1])).toList());
    }

    private static State optimizationSSA(double tempInitial, double tempFinal, double alfa, int meanMarkov, int beta, double vWaiter, double vCustomer, double tServing, State state) {
        State initialState = beamSearch(beta, vWaiter, vCustomer, tServing, state);

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
        double tempNow = tempInitial; // initialize the current temperature (current temperature)

        while (tempNow >= tempFinal) { // outer loop , until the current temperature reaches the end temperature
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
                State nextState = beamSearch(beta, vWaiter, vCustomer, tServing, modifiedState);

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
            // Slow down to a new temperature according to the cooling curve ：T(k)=alfa*T(k-1)
            tempNow = tempNow * alfa;
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

    private static void customerSequencingAndCwspComplete() {
        double tempInitial = 50.0; // set the initial annealing temperature
        double tempFinal = 1.0; // set the ending/stop annealing temperature
        double alfa = 0.99; // set the cooling parameters ,T(k)=alfa*T(k-1)
        int meanMarkov = 100; // Markov Chain length, that is the number of internal circulation runs
        int outerLoopIterations = (int) Math.ceil(Math.log(tempFinal / tempInitial) / Math.log(alfa));
        System.out.println("Initial Temperature: " + tempInitial);
        System.out.println("Final Temperature: " + tempFinal);
        System.out.println("Alfa: " + alfa);
        System.out.println("Outer loop (temp reduction): " + outerLoopIterations + " iterations");
        System.out.println("Inner loop (Markov Chain): " + meanMarkov + " iterations");
        System.out.println("Total (outer * inner loop): " + outerLoopIterations * meanMarkov + " iterations\n");

        int beta = 300; // beams search width

        System.out.println("Beta: " + beta + "\n");

        int noCustomers = 10;
        int noCounters = 10;
        int minRequest = 1; // minimum no of request per customer
        int maxRequest = 10; // maximum no of request per customer
        boolean uniqueOrders = true; // defines if a customer may have multiple orders of the same kind
        int noStartStates = 10;

        System.out.println("No Customers: " + noCustomers);
        System.out.println("No Counters: " + noCounters);
        System.out.println("Min Request: " + minRequest);
        System.out.println("Max Request: " + maxRequest);
        System.out.println("Unique Orders: " + uniqueOrders);
        System.out.println("No Start States: " + noStartStates + "\n");

        List<State> startStates = generateRandomStartStates(noCustomers, noCounters, minRequest, maxRequest, uniqueOrders, noStartStates);

        // ======== Beispiel aus Seminararbeit ========
        // State state = new State();
        // state.addCustomer(new Customer(0, new ArrayList(List.of(1))));
        // state.addCustomer(new Customer(1, new ArrayList(List.of(1, 2, 3))));
        // state.addCustomer(new Customer(2, new ArrayList(List.of(0, 1))));
        // state.addCustomer(new Customer(3, new ArrayList(List.of(1, 3))));
        // List<State> startStates = generateStatePermutations(state, noStartStates);
        // ======== Ende Beispiel ========

        double vWaiter = 1.0; // waiter walking speed
        double vCustomer = 1.0; // customer walking speed
        double tServing = 2.0; // time required to serve a customer

        // Simulated annealing algorithm
        long startTime = System.currentTimeMillis();
        List<State> resultStates = startStates.stream().parallel().map(startState -> optimizationSSA(tempInitial, tempFinal, alfa, meanMarkov, beta, vWaiter, vCustomer, tServing, startState)).toList();
        State bestState = resultStates.stream().sorted(State::compare).findFirst().orElse(new State());

        long stopTime = System.currentTimeMillis();

        System.out.println("Runtime: " + (stopTime - startTime) / 1000.0 + " sec");

        if (resultStates.stream().mapToDouble(s -> s.getTime()).distinct().count() == 1) {
            System.out.println("All start states lead to an equal optimal solution. First solution will be shown in detail.");
        } else {
            System.out.println("Start states produced states with different optimality. Best solution will be shown in detail.");
            resultStates.stream().sorted(State::compare).forEach(s -> System.out.println("\tResult: " + s.getTime() + " sec"));
        }

        System.out.println("\nBest Result: " + bestState.getTime() + " sec");

        System.out.println("\tCustomer Order:" + bestState.getCustomers().stream().map(c -> "\n\t\tID (original ID): " + c.getId() + " (" + c.getInitialId() + ")\tOrders: " + c.getInitialOrders()).toList());

        System.out.println("\n\tWaiter schedule:" + bestState.getWaiterSchedule().stream().map(w -> String.format("%n\t\t Customer ID %2d served at counter %2d", w[0], w[1])).toList());
    }

    public static void main(String[] args) {
        rand.setSeed(42); // "Answer to the Ultimate Question of Life, the Universe, and Everything" ... and a good seed

        // --- test functions
        // testServeCustomer();
        // testBeamSearchFixedCustomers();
        // testBeamSearch();

        customerSequencingAndCwspComplete();

    }

}
