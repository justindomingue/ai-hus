package student_player;

import hus.HusBoardState;
import hus.HusMove;
import hus.HusPlayer;
import student_player.mytools.AlphaBeta;
import student_player.mytools.BaseSolver;

/** A Hus player submitted by a student. */
public class StudentPlayer2 extends HusPlayer {

    /** You must modify this constructor to return your student number.
     * This is important, because this is what the code that runs the
     * competition uses to associate you with your agent.
     * The constructor should do nothing else. */
    public StudentPlayer2() { super("AB"); }

    /** This is the primary method that you need to implement.
     * The ``board_state`` object contains the current state of the game,
     * which your agent can use to make decisions. See the class hus.RandomHusPlayer
     * for another example agent. */
    public HusMove chooseMove(HusBoardState board_state) {

        // First turn
        if (board_state.getTurnNumber() == 0) {
            BaseSolver.black = player_id;
            BaseSolver.white = opponent_id;
            BaseSolver.heuristic = BaseSolver.HeuristicStrategy.WEIGHTED_POSITION;

            AlphaBeta.depth = 7;

//            MonteCarlo.rootSelection = 0;
//            return MonteCarlo.chooseMove(board_state, true);
        }

//         if (board_state.getTurnNumber() <= 1)
//             return (HusMove)board_state.getRandomMove();

        return AlphaBeta.chooseMove(board_state);
//        return ABMC.chooseMove(board_state);
//       return MonteCarlo.chooseMove(board_state, false);
    }
}
