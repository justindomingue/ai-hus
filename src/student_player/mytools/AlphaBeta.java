package student_player.mytools;

import hus.HusBoardState;
import hus.HusMove;

import java.util.*;

/**
 * Created by justindomingue on 16-02-08.
 *
 * Resources used:
 *      - textbook
 *      - chessprogramming.wikispaces.com
 *      - wikipedia
 */
public class AlphaBeta extends BaseSolver {

    /* Alpha-beta will stop at that depth */
    public static int depth;

    public static HusMove chooseMove(HusBoardState state) {
        return alphabeta(state);
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * * *
                        ALPHA-BETA
     * * * * * * * * * * * * * * * * * * * * * * * * * */
    /* TODO iterative deepening - time runs out -> return move selected by deepeest completed search
        + helps with move ordering by remembering best move from last step
     */

    /** Runs alpha-beta pruning search on state */
    public static HusMove alphabeta(HusBoardState state) {
        int max = Integer.MIN_VALUE;
        HusMove best = null;

        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

//        ArrayList<HusMove> moves = get_ordered_moves(state, state.getTurnPlayer());
        for (HusMove move : state.getLegalMoves()) {
            int val = min_value(result(state, move), alpha, beta, depth);
            if (val > max) {
                max = val;
                best = move;
            }
        }

        return best;
    }

    /** A) Minimax implementation with min/max_value functions **/
    private static int max_value(HusBoardState state, int alpha, int beta, int depth) {
        if (cuttoff(state, depth)) return eval(state);

        int v = Integer.MIN_VALUE;

        // Get the legal moves for the current board state.
        ArrayList<HusBoardState> states = get_ordered_states(state);
        for (HusBoardState s : states) {
            v = Math.max(v, min_value(s, alpha, beta, depth - 1));
            if (v >= beta) return v;
            alpha = Math.max(alpha, v);
        }

        return v;
    }

    /** A) Minimax implementation with min/max_value functions **/
    protected static int min_value(HusBoardState state, int alpha, int beta, int depth) {
        if (cuttoff(state, depth)) return eval(state);

        int v = Integer.MAX_VALUE;

        // Get the legal moves for the current board state.
        ArrayList<HusBoardState> states = get_ordered_states(state);
        for (HusBoardState s : states) {
            v = Math.min(v, max_value(s, alpha, beta, depth - 1));
            if (v <= alpha) return v;
            beta = Math.min(beta, v);
        }

        return v;
    }
}
