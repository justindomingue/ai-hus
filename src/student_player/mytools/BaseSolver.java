package student_player.mytools;

import hus.HusBoardState;
import hus.HusMove;

import java.util.*;

/**
 * Created by justindomingue on 16-03-06.
 */
public class BaseSolver {

  /* WEIGHTS - found using linear regression
   * weight i is for pit i, i.e. h(n) = w1x1 + w2x2 + ... + wnxn (bias dropped)
   */
  protected static final int[] weights = {110 ,106 ,106 ,110 ,114 ,116 ,119 ,123 ,126 ,132 ,135 ,136 ,139 ,136 ,125 ,133 ,118 ,112 ,106 ,103 ,100 ,96 ,88 ,80 ,88 ,88 ,88 ,89 ,93 ,101 ,107 ,112};

    /* GAME-RELATED FIELDS */
    public static int black;
    public static int white;

    /* HEURISTIC */
    public enum HeuristicStrategy {
        NUM_SEEDS,
        WEIGHTED_POSITION,
    }
    public static HeuristicStrategy heuristic;


    /* -- SHARED UTILITIES -- */

    /* BOARD INTERACTIONS */

    /** The transition model, which defines the result of a move
     *  Applies move ``a`` to the board state ``s`` in an isolated
     *  environment and returns the new board state.
     */
    protected static HusBoardState result(HusBoardState s, HusMove a) {
        HusBoardState cloned_board_state = (HusBoardState) s.clone();
        cloned_board_state.move(a);
        return cloned_board_state;
    }

    protected static ArrayList<HusMove> get_ordered_moves(final HusBoardState initial, final int p) {
        ArrayList<HusMove> moves = initial.getLegalMoves();
        Collections.sort(moves, new Comparator<HusMove>() {
            @Override
            public int compare(HusMove o1, HusMove o2) {
                return eval(result(initial, o2), p) - eval(result(initial, o1), p);   // descending order
            }
        });
        return moves;
    }

    protected static ArrayList<HusBoardState> get_ordered_states(final HusBoardState initial) {
        final int p = initial.getTurnPlayer();

        ArrayList<HusMove> moves = initial.getLegalMoves();
        ArrayList<HusBoardState> states = new ArrayList<HusBoardState>();
        for (HusMove m : moves) states.add(result(initial, m));

        Collections.sort(states, new Comparator<HusBoardState>() {
            @Override
            public int compare(HusBoardState s1, HusBoardState s2) {
                return eval(s2, p) - eval(s1, p);   // descending order
            }
        });
        return states;
    }

    /** FAST SAMPLING **/

    /** Fast select k best move
     * Re-implement geLegalMoves for fast sampling
     */
    protected static List<HusMove> kbestmoves(HusBoardState from, int k) {
        PriorityQueue<Wrapper> q = new PriorityQueue<Wrapper>(k);

        // Find the k best states using a min-heap
        int p = from.getTurnPlayer();
        int []pits = from.getPits()[p];
        for(int i = 0; i < 2 * HusBoardState.BOARD_WIDTH; i++){
            if (pits[i] > 1) {
                HusMove move = new HusMove(i, from.getTurnPlayer());
                HusBoardState to = result(from,move);
                Wrapper sw = new Wrapper(move, eval(to, heuristic, p));
                if (q.size() < k) q.add(sw);
                else if (sw.val > q.peek().val) {
                        q.poll();
                        q.add(sw);
                    }
            }
        }

        // return k moves inside the heap
        List<HusMove> ret = new ArrayList<HusMove>();
        for (Wrapper sw : q)
        { ret.add(sw.move); }

        // shouldn't happen
        return ret;
    }

    /** Note. Re-implementation of HusBoardState.getLegalMoves() to be faster
     * Get the k best legal move for the current board state.
     *
     * Returned moves are assumed to be moves for the player whose turn
     * it currently is. */
    protected static HusBoardState samplekbeststates(HusBoardState from, int k){
        PriorityQueue<Wrapper> q = new PriorityQueue<Wrapper>(k);

        // Find the k best states using a min-heap
        int p = from.getTurnPlayer();
        int []pits = from.getPits()[p];
        for(int i = 0; i < 2 * HusBoardState.BOARD_WIDTH; i++){
            if (pits[i] > 1) {
                HusMove move = new HusMove(i, from.getTurnPlayer());
                HusBoardState to = result(from,move);
                Wrapper sw = new Wrapper(to, eval(to, heuristic, p));
                if (q.size() < k) q.add(sw);
                else {
                    if (sw.val > q.peek().val) {
                        q.poll();
                        q.add(sw);
                    }
                }
            }
        }

        // sample the priority queue
        int rnd = new Random().nextInt(Math.min(k, q.size()));
        for (Wrapper sw : q) {
            if (rnd-- == 0) {
                return sw.state;
            }
        }

        // shouldn't happen
        return result(from, from.getLegalMoves().get(0));
    }

    static class Wrapper implements Comparable<Wrapper> {
        public HusBoardState state;
        public HusMove move;
        public int val;
        public Wrapper(HusBoardState state, int val) {
            this.state = state;
            this.val = val;
        }
        public Wrapper(HusMove move, int val) {
            this.move = move;
            this.val = val;
        }

        @Override
        public int compareTo(Wrapper o) {
            return this.val - o.val;
        }
    }

    /* BOARD EVALUATION */

    /** Utility function defining the final numeric value for a game
     * that ends in terminal state ``s`` for a player ``p``.g
     */
    protected static int utility(HusBoardState s, int p) {
        return s.getWinner() == HusBoardState.DRAW || s.getWinner() == HusBoardState.NOBODY ?
                1 : s.getWinner() == p ?
                2 : 0
                ;
    }

    protected static int eval(HusBoardState s, int p) {
        return eval(s, heuristic, p);
    }

    protected static int eval(HusBoardState s) {
        return eval(s, heuristic, black);
    }

    /** Heuristic evaluation function
     * Evaluates the board state ``s`` using heuristic ``heuristic``
     */
    protected static int eval(HusBoardState s, HeuristicStrategy heuristicStrategy, int p) {
        int[][] pits;
        int[] my_pits;
        int val = 0;

        switch(heuristicStrategy) {
            /** Returns the sum of seeds in every pit, i.e. #seeds the player owns**/
            case NUM_SEEDS:
                // Get the contents of the pits so we can use it to make decisions.
                my_pits = s.getPits()[p];

                // Evaluate the pits
                for (int i = 0; i < my_pits.length; i++) {
                    val += (my_pits[i]);
                }
                break;

            /** Returns a weighted sum of the seeds in every pit **/
            case WEIGHTED_POSITION:
                // Get player's pits
                my_pits = s.getPits()[p];

                // Weighted sum of the pits
                val = weights[0];   // bias
                for (int i = 0; i < my_pits.length; i++) {
                    val += my_pits[i] * (weights[i+1]);
                }

                break;
        }

        return val;
    }

    /** Cutoff test */
    protected static boolean cuttoff(HusBoardState state, int depth) {
        return (state.gameOver() || depth <= 0);
    }

}
