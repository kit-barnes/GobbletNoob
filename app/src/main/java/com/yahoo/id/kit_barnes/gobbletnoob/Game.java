package com.yahoo.id.kit_barnes.gobbletnoob;

import android.util.Log;

/*
 * individual playing piece (cup) represented by radius: 2-5 and player: white or not-white
 * 										and a reference to the cup it contains (if any)
 *
 * a stack of cups is represented by a linked list of length 0 to 4
 *   the bottom of the stack (innermost cup) is at the end of the list
 *
 * stacks - array of 22 stacks - indices 0-15 represent board,
 *                                      16-21 player stacks:
 *
 *     player not-white      16 17 18			
 *		               /    0  1  2  3
 *       board		  /     4  5  6  7
 *		              \     8  9 10 11
 *		               \   12 13 14 15
 *     player white          19 20 21			
 *
 */

public class Game {
	
	class Cup {
		Cup inside;
		char size;
		boolean white;
		
		public Cup(char size, boolean white ) {
			this.inside = null;
			this.size = size;
			this.white = white;
		}
		
		void drop(int index) {
			inside = stack[index];
			stack[index] = this;
			if (index < 16) {
				int delta = weight();
				if (inside != null) delta -= inside.weight();
				whiteweight += delta * intersects[index].length;
				updateleval(index);
			}
		}
		
		int weight() {
			return white ? size : -size;
		}
	}
	

	Cup stack[];			// the board
	Cup liftedcup;			// cup being moved (lifted from board) or null
	int liftedfrom;			// stack index (invalid iff liftedcup==null)
	int wins;				// number of winning lines
	int winlines[];			// winning line indices (up to 3 of them)
	boolean uncoveredwin;
	StringBuilder history;
	
	Leval leval[];			// line evaluations
	int whiteweight;		// sum of visible cup weights
	
			
	final static byte[][] lines = {
		// lines 0-9 and the stacks contained in each
        { 0, 1, 2, 3 },       // horizontal
        { 4, 5, 6, 7 },
        { 8, 9, 10, 11 },
        { 12, 13, 14, 15 },

        { 0, 4, 8, 12 },      // vertical
        { 1, 5, 9, 13 },
        { 2, 6, 10, 14 },
        { 3, 7, 11, 15 },

        { 0, 5, 10, 15 },     // diagonal
        { 3, 6, 9, 12 }
	};
    final static byte[][] intersects = {
    	// for each stack 0-21, the lines that contain it
            { 0, 4, 8 }, { 0, 5 },    { 0, 6 },    { 0, 7, 9 },
            { 1, 4 },    { 1, 5, 8 }, { 1, 6, 9 }, { 1, 7 },
            { 2, 4 },    { 2, 5, 9 }, { 2, 6, 8 }, { 2, 7 },
            { 3, 4, 9 }, { 3, 5 },    { 3, 6 },    { 3, 7, 8 },
            {}, {}, {},   {}, {}, {}    // player stacks are in no lines
            };

	
	public Game(String hist) {
		int i = 22;
		stack = new Cup[i];
		while (--i >= 0) {
			stack[i] = null;
		}
		for ( char size = 2; size < 6; size++ ) {
			for ( i = 0; i < 3; i++ ) {
				Cup cup = new Cup(size, false);
				cup.drop(i+16);
				cup = new Cup(size, true);
				cup.drop(i+19);
			}
		}
		liftedcup = null;
		wins = 0;
 		winlines = new int[3];
 		uncoveredwin = false;
 		leval = new Leval[10];
 		for (i = 0; i < 10; i++) {
 			leval[i] = new Leval();
 		}
 		whiteweight = 0;
        history = new StringBuilder(hist);
        if (hist.length() > 0) {
        	int stk = -1;
	        for (i = 0; i < hist.length(); i++) {
	        	stk = hist.charAt(i) - 0x31;
	        	if (i%2 == 0) lift(stk);
	        	else drop(stk);
	        }
	        win(stk);
        }
	}
	
	boolean whitesturn() {
		return (history.length() & 2) == 0;
	}
	
	void lift(int index) {
		liftedcup = stack[index];
		stack[index] = liftedcup.inside;
		if (index < 16) {
			int delta = -liftedcup.weight();
			if (liftedcup.inside != null) delta += liftedcup.inside.weight();
			whiteweight += delta * intersects[index].length;
			updateleval(index);
		}
		liftedcup.inside = null;			// probably not necessary
		liftedfrom = index;
	}
	boolean ok2lift(int index) {
		if ( index < 0 || index >= 22
				|| liftedcup != null
				|| stack[index] == null 
				|| stack[index].white != whitesturn()
				|| wins != 0
				) {
			return false;			
		}
		return true;
	}
	boolean pick(int index) {
		if (!ok2lift(index)) return false;
		lift(index);
		history.append((char)(index + 0x31));
		win(index);
		return true;
	}
	
	void drop(int index) {
		liftedcup.drop(index);
		liftedcup = null;
	}
	boolean ok2drop(int index) {
		if (uncoveredwin) {
			// MUST re-cover win
			int i;
			for (i = 0; i < 4; i++) {
				if (lines[winlines[0]][i] == index) break;
			}
			if (i == 4) return false;
		}
		if ( index >= 0 && index < 16
				&& wins == 0
				&& liftedcup != null
				&& index != liftedfrom
				&& ( liftedfrom < 16
						|| stack[index] == null
						|| ( stack[index].white != whitesturn()
							&& oneof3inrow(index) )
						)
				&& (stack[index] == null
						|| liftedcup.size > stack[index].size)
				) {
			return true;
		}
		return false;
	}
	boolean place(int index) {
		if (!ok2drop(index)) return false;
		drop(index);
		history.append((char)(index + 0x31));
		win(index);
		return true;
	}

	private boolean oneof3inrow(int index) {
		// assert there is a cup at index
		boolean color = stack[index].white;
		int li = intersects[index].length;		// number of lines containing index stack
		while (--li >= 0) {
			if (numinline(color,intersects[index][li]) == 3) return true;
		}
		return false;
	}
	
	boolean win(int index) {
		wins = 0;
		uncoveredwin = false;
		if (index >= 16) return false;
		if (stack[index] == null) return false;
		boolean color = stack[index].white;
		int li = intersects[index].length;		// number of lines containing index stack
		while (--li >= 0) {
			if (numinline(color,intersects[index][li]) == 4) {
				winlines[wins++] = intersects[index][li];
			}
		}
		if (wins > 0) {
			if (wins == 1 && (liftedcup) != null) {
				// check to see if we can cover it up again
				// look for a cup (other than index) in winline smaller than liftedcup
				for (int i = 0; i < 4; i++) {
					int di = lines[winlines[0]][i];
					if (di == index) continue;
					if (stack[di].size < liftedcup.size) {
						uncoveredwin = true;
						wins = 0;
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}
		
	private int numinline(boolean color, int line) {
		int count = 0;
		for (int i = 0; i < 4; i++) {	// lines[x].length === 4
			Cup c = stack[ lines[line][i] ];
			if ( c==null || c.white != color ) continue;
			count++;
		}
		return count;
	}
	
    int getmove(int ai) {
    	// return === pick_stack_index * 32 + place_stack_index
    	StringBuilder bestmoves = new StringBuilder();
    	int bestvalue = -1000000;
    	
    	int firstfrom = 0;
    	int lastfrom = 21;
    	boolean alreadylifted = false;
    	if (liftedcup != null) {
    		firstfrom = lastfrom = liftedfrom;
    		alreadylifted = true;
    	}
    	int si, di;
    	for (si = firstfrom; si <= lastfrom; si++) {
    		if (!alreadylifted) {
    			if (!ok2lift(si)) continue;
    			lift(si);
    		}
			for (di = 0; di < 16; di++) {
				if (!ok2drop(di)) continue;
				drop(di);
				int value = evaluate(ai);
				lift(di);
				liftedfrom = si;
				if (value < bestvalue) continue;
				if (value > bestvalue) {
					bestmoves.setLength(0);
					bestvalue = value;
				}
				bestmoves.append((char)(si + 0x31)).append((char)(di + 0x31));
			}
	   		if (!alreadylifted) {
	   			drop(si);
	   		}
    	}
    	// randomly choose one of the best moves found.
		if (bestmoves.length()==0) {	// error!!!!!????? wtf
			Log.v("Gobblet","getmove: alreadylifted="+alreadylifted+", firstfrom="+firstfrom);
			return 0;
		}
    	int r = (int)(Math.random()*(bestmoves.length()/2));
       	si = alreadylifted? 22 : bestmoves.charAt(2*r)-0x31;
       	di = bestmoves.charAt(2*r + 1) - 0x31;
    	return si*32 + di;
    }
    
	class Leval {
		int countb;
		int biggestb;
		int countw;
		int biggestw;
		
		public Leval() {
			countb = countw = 0;
			biggestb = biggestw = 1;
		}
		
		void eval(int li) {
			countb = countw = 0;
			biggestb = biggestw = -1;
			for (int i = 0; i < 4; i++) {
				Cup c = stack[lines[li][i]];
				if (c == null) continue;
				if (c.white) {
					countw++;
					if (c.size > biggestw) biggestw = c.size;
				} else {
					countb++;
					if (c.size > biggestb) biggestb = c.size;
				}
			}
		}
		
		int biggest(boolean side) {
			return side? biggestw : biggestb ;
		}
		int count(boolean side) {
			return side? countw : countb ;
		}
		
	}		// end class Leval
	
	void updateleval(int si) {		// update evaluations for lines containing stack
		for (int i = 0; i < intersects[si].length; i++) {
			int li = intersects[si][i];
			leval[li].eval(li);
		}
	}
	
	int evaluate(int ai) {
		/*
		 * position value = sum of all cell and line values
		 * line values:
		 *	-1000000 for opponent win
		 *	  100000 for win
		 *	  -10000 for non-blocked opponent 3-in-a-row
		 *	    1000 for non-blocked 3-in-a-row
		 */
		if (ai == 1) return 0;	// all evaluations the same for random moves
		boolean side = whitesturn();
		int value = side? whiteweight : -whiteweight;
		for (int li = 0; li < 10; li++) {
			if (ai > 2 && leval[li].count(!side)==4) {
				// only see exposed win if smarter than beginner
				value -= 1000000;
			}
			if (leval[li].count(side)==4) {
				// win
				value += 100000;
			}
			if (leval[li].count(!side)==3  && leval[li].biggest(side)!=5) {
				// non-blocked opponent 3-in-a-row
				value -= 10000;
			}
			if (ai > 2 && leval[li].count(!side)==2  && leval[li].biggest(side)!=5) {
            	// non-blocked opponent 2-in-a-row
				value -= 1000;
			}
			if (leval[li].count(side)==3  && leval[li].biggest(!side)!=5) {
				// non-blocked 3-in-a-row
				value += 1600/leval[li].biggest(!side);
			}
			if (ai > 3 && leval[li].count(side)==2  && leval[li].biggest(!side)!=5) {
				// non-blocked 2-in-a-row
				// if there's another n-b-2-in-row intersecting (at a not-mine stack)
				// then this move is worth more than a n-b-3-in-row
				for (int i = 0; i < 4; i++) {
					int ci=lines[li][i];
					if ( stack[ci]==null || stack[ci].white!=side ) {
						// check for intersecting n-b-2-in-row
						for (int j = 0; j < intersects[ci].length; j++) {
							int lj = intersects[ci][j];
							if (leval[lj].count(side)==2  && leval[lj].biggest(!side)!=5) {
								value += 2400/leval[lj].biggest(!side);
							}
						}
					}
				}
				value += 600/leval[li].biggest(!side);
			}
		}
		return value;
	}
	
}

