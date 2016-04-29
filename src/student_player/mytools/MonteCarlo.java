package student_player.mytools;

import hus.HusBoardState;
import hus.HusMove;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by justindomingue on 16-03-04.
 */
public class MonteCarlo extends BaseSolver {

    /*-- FIELDS --*/

    /* TREE POLICY */
    protected static Node root;
    protected static int tp_samplesize = 15;
    protected static double EXPLORATION_CONSTANT = 0.5;
    protected static double epsilon = 1e-6;

    /* DEFAULT POLICY */
    protected static int dp_samplesize = 4;

    /* GAME-RELATED SETTINGS */
    protected static long TIME_LIMIT = 2000-100;

    /* MISC */
    protected static Random r = new Random();

    /*-- METHODS --*/

    public static HusMove chooseMove(HusBoardState state, boolean first_turn) {
        if (first_turn) {
//            System.out.println("First turn -- using full 30s with max tp_samplesize.");

            int old_samplesize = tp_samplesize;

            // Run monte for 30s with full tree
            tp_samplesize = 48;
            HusMove best = montecarlo(state, 30000-200);

            // Restore  sample size and return best
            tp_samplesize = old_samplesize;
            return best;
        }

        return montecarlo(state, TIME_LIMIT);
    }

    public static HusMove montecarlo(HusBoardState state, long time_limit) {
        long initial_time = System.currentTimeMillis();

        boolean found_root = false;

        // if it exists, look in the search for the new root
        if (root != null && root.children != null) {
            // at this point, root is the node we played in the previous game
            // look at every move from that root to determine which one the opponent took
            for (Node c : root.children) {
                // apply c.action from root.state and compare with current `state`
                if (equals(result(root.state, c.action), state)) {
                    root = c;
                    root.parent = null; // drop tree above
                    found_root = true;
                    break;
                }
            }
        }

        // create root with state `state`, empty parent and empty action
        if (!found_root) {
//            System.out.println("Couldn't salvage root.");
            root = new Node(state);
        }

        while ((System.currentTimeMillis()-initial_time) < time_limit) {
            // SELECT & EXPAND
            Node last  = treePolicy(root);

            // ROLLOUT AND BACKUP
            int delta = defaultPolicy(last.state);
            backup(last, delta);
        }

//        System.out.println(root.toString());

        // return best child for root (use constant 0 for no exploration
        Node best = select(root, 0);

        // update root to be at the node we just played
        root = best ;

        return best.action;
    }

    /* ---------------------------------------- *
     *              SEARCH POLICIES
     * ---------------------------------------- */

    /* Select or create a leaf node from the nodes already contained within
     * the search tree
     */
    //TODO compute state here
    private static Node treePolicy(Node root) {
        Node cur = root;
        while (cur.isNonTerminal()) {
            if (cur.isNotFullyExpanded()) {
                return expand(cur);
            }
            else {
                cur = select(cur, EXPLORATION_CONSTANT);
            }
        }
        return cur;
    }

    /* Play out the game from a non-terminal state all the way to then end
     * of the game to produce a value estimate
     */
    private static int defaultPolicy(HusBoardState state) {
      private static int defaultPolicy(HusBoardState state) {
          // remember who was the max player at that node
          int player = state.getTurnPlayer();

          HusBoardState s = (HusBoardState) state.clone();
          int i=0;

          // for the first X turns, play from the `k` best moves
          while (!s.gameOver() && i++ < policy_switch_depth)      // rollout
              s.move(samplekbestmoves(s, dp_samplesize));

          // after that, play random moves which are faster
          while (!s.gameOver()) {
              s.move(fastrandommove(s));
          }

          return reward(s, player);
      }

    /* ---------------------------------------- *
     *                UTILITIES
     * ---------------------------------------- */

    private static Node expand(Node parent) {
        // if first time expanding, load all children
        if (parent.children == null) parent.loadChildren();

        // note child.parent is already set
        return parent.nextChild();
    }

    private static Node select(Node parent, double c) {
//        System.out.println("select("+c+") " + parent.toString());
        Node best_child = null;
        double best_val = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < parent.last_expanded_child; i++) {
            Node v = parent.children[i];
            double uct = v.uctValue(c);

            if (uct > best_val) {
                best_child = v;
                best_val = uct;
            }
        }

        //note best_child.parent is already set
        return best_child;
    }

    // Back-propagate assuming two-player, zero-sum game with alternating moves
    private static void backup(Node last, int delta) {
        Node v = last;

        while(v.parent != null) {   // no need to update the parent
            v.visits += 1;
            v.wins += delta;
            v = v.parent;
            delta = 1-delta;     // flip delta
        }
    }

    // Used to find the root
    private static boolean equals(HusBoardState s1, HusBoardState s2) {
        return Arrays.deepEquals(s1.getPits(), s2.getPits()) ;
    }

    /** Utility function defining the final numeric value for a game
     * that ends in terminal state ``s`` for a player ``p``.g
     */
    protected static int reward(HusBoardState s, int p) {
        return ((s.getWinner() == HusBoardState.DRAW) || (s.getWinner() == HusBoardState.NOBODY)) ?
                0 : ((s.getWinner() == p) ?
                0 : 1);
    }

    private static class Node {
        int wins = 0;
        int visits = 0;

        Node parent;        // used during backpropagation
        Node[] children;
        int last_expanded_child = 0;  // to keep track of last expanded children

        HusBoardState state;    //TODO do not store state, calculate it, survey=mater, p9
        HusMove action;

        public Node(Node parent, HusMove action) {
            this.parent = parent;
            this.state = null;
            this.action = action;
            this.children = null;
        }

        public Node(HusBoardState state) {
            this.parent = null;
            this.state = state;
            this.action = null;
            this.children = null;
        }

        public boolean isNonTerminal() {
            return !this.state.gameOver();
        }
        public boolean isNotFullyExpanded() {
            return children == null || last_expanded_child != children.length;
        }

        // Adds all legal child state to the `children` array
        public void loadChildren() {
            List<HusMove> sample = kbestmoves(state, tp_samplesize);

            this.children = new Node[sample.size()];

            int i = 0;
            for(HusMove a : sample) this.children[i++] = new Node(this, a);
        }

        // Get the next unexpanded child
        public Node nextChild() {
            Node child = children[last_expanded_child++];
            child.state = result(child.parent.state, child.action);
            return child;
        }

        public double uctValue(double c) {
            return (wins / (visits + epsilon)) +
                    (c *
                            (Math.sqrt(Math.log(parent.visits + 1)
                                    /
                                    (visits + epsilon))))
                    + (r.nextDouble() * epsilon);
        }

        public Node mostVisitedChild() {
            int max_visits = -1;
            Node best = null;

            for (int i=0; i<last_expanded_child; i++) {
                if (children[i].visits > max_visits) {
                    max_visits = children[i].visits;
                    best = children[i];
                }
            }

            return best;
        }

        public double heuristic(){
            return BaseSolver.eval(state, HeuristicStrategy.WEIGHTED_POSITION, state.getTurnPlayer());
        }


        @Override
        public String toString() {
            return "Node[" +
                    (parent==null?"Root": "Parent(\u2713)") + "), " +
                    "Action(" + (action==null?"\u2613":"\u2713") + "), " +
                    "State(" + (state==null?"\u2613":"\u2713") + "), " +
                    "#wins(" + wins + "), " +
                    "Visits(" + visits + "), " +
                    "#children(" + (children==null ? 0 : children.length) + "), " +
                    "UCT(" + (parent!=null?uctValue(EXPLORATION_CONSTANT):"_") + ")" +
//                    "Last expanded(" + last_expanded_child + ")" +
//                    "Game over?(" + state.gameOver() + ")" +
                    "]";
        }
    }
}
