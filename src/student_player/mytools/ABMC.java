package student_player.mytools;

import hus.HusBoardState;
import hus.HusMove;

/**
 * Created by justindomingue on 16-03-11.
 */
public class ABMC extends BaseSolver {
    /** An hybrid player playing Alpha-Beta until depth `x` and monte carlo after**/

    public static int switch_depth = 10;

    public static HusMove chooseMove(HusBoardState state) {
        if (state.getTurnNumber() < switch_depth) {
            return AlphaBeta.chooseMove(state);
        } else {
            return MonteCarlo.chooseMove(state, false);
        }
    }
}
