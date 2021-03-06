/*
 * Author: Matthew Bardeen <me@mbardeen.net>, (C) 2008
 *
 * Copyright: See COPYING file that comes with this distribution
 *
*/

import java.io.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Arrays;
import java.util.Random;

public class HotSingleAgent {
	Board b; 			/// a copy of the current board.


	Move bestoverallmove;	/// the best move we have overall
	public ScheduleRunner timeout; 	/// this holds the method that we will execute once our time limit is up.
	public int nodesexpanded;	/// Number of nodes exanded
	public int maxdepthreached;	/// The max depth reached by the search

	public Heuristic utility;	/// A function to determine the utility of the current board
	
	public int algorithm; 		/// which algorithm to use in the search
	
	static final int MAXDEPTH=4; 	/// How deep we will search without limits
	static final int MINIMAX=1;	/// Do a minimax search
	static final int ALPHABETA=2; 	/// Do a minimax search with alpha-beta pruning
	static final int INFINITY=99999999;
	static final int WINVALUE=999999;
	static final int LOSSVALUE=-WINVALUE;
	Random r;
	
	public HotSingleAgent(int algorithm) 
	{
	  timeout=new ScheduleRunner();
	  nodesexpanded=0;
	  maxdepthreached=0;
	  r=new Random();
	  this.algorithm=algorithm;
	}
	  
	public HotSingleAgent() 
	{
	  timeout=new ScheduleRunner();
	  nodesexpanded=0;
	  maxdepthreached=0;
	  r=new Random();
	  this.algorithm=MINIMAX;
	}
	
	
	class MoveValue
	{
	    public Move move;
	    public int value;
	    
	    MoveValue() 
	    {
		move=null;
		value=0;
	    }
	    
	    MoveValue(Move m) 
	    {
	      this.move=m;
	      value=0;
	    }
	    
	    MoveValue(int v)
	    {
	      this.value=v;
	      this.move=null;
	    }
	    
	    
	    MoveValue(Move m, int v)
	    {
	      this.value=v;
	      this.move=m;
	    }
	}
	
	//inner class
	class ScheduleRunner extends TimerTask
   	{
   		/**
    		* executed when time is up.
    		*/
		public void run()
		{
			//time's up. Print out the best move and exit.
			System.out.println(bestoverallmove);
			System.out.println("Nodes Expanded:" +nodesexpanded);
			System.out.println("Max Depth Reached:" + maxdepthreached);
			System.exit(0);
		}
	} // end inner class ScheduleRunner
	


	/** Return the best move based on the current board 
	**/
    public Move getBestMove(Board inb, int MAXDEPTH) 
	{
	    MoveValue bestmove=new MoveValue();
	    for (int depth=1; depth<MAXDEPTH; depth++) 
	    {
		  if (algorithm==MINIMAX)
		    bestmove=minimax(inb, 1,depth);  // save the best move for this depth
		  else if (algorithm==ALPHABETA)
		    bestmove=alphabeta(inb, 1,depth,-INFINITY, INFINITY);  // save the best move for this depth
		  
		  //record the best move for this level in case we need to exit early
		  bestoverallmove=bestmove.move;
		 
		  //sure win, return this move as the best
		  if (bestmove.value==WINVALUE)
		    return bestmove.move;
		  //no hope, should resign here
		  if (bestmove.value==LOSSVALUE)
		  {
		    Move m=new Move();
		    m.setResign(true);
		    return m;
		  }
		  //Record some statistics about the search
		  maxdepthreached=depth;		  
	    }
	    return bestoverallmove;
	}

    public MoveValue minimax(Board b, int currentdepth, int maxdepth) 
	{
	   nodesexpanded++; 
	  /*	      
	      if node is a terminal node or depth == 0:
	      return the heuristic value of node
	 */
	    if (b.isCheckMate()) {
		return new MoveValue(LOSSVALUE);
	    }
	    if (b.isStalemate()) return new MoveValue(0); 
	    if (currentdepth==maxdepth) return new MoveValue(utility.evaluate(b)*b.turn);
      
	  /*else:
	    a = -infinity
	    for child in node:              # evaluation is identical for both players 
		a = max(a, -minimax(child, depth-1))
	    return a
	  */
	    MoveValue best= new MoveValue(-INFINITY);
	    Move[] moves=b.getValidMoves();
	    for (Move currentmove : moves) 
	    {
		Board child=b.clone();
		child.makeMove(currentmove);
		//reverse the sign since we're really evaluating our opponent's board state
		MoveValue childmove = minimax(child, currentdepth+1,maxdepth);

		childmove.move=currentmove;
		childmove.value=-childmove.value;
		//if (currentdepth==1) System.out.println(childmove.move+" "+childmove.value);
		if (childmove.value>best.value) {
		    best=childmove;
		   // System.out.println(best.value+":"+best.move);
		} else if (childmove.value==best.value) 
		// if we have a tie, replace the best move at random (to avoid the problem of loops)
		  if (r.nextDouble()>0.5)
		    best=childmove;
		

		
	    }
	   return best;
	}

    public MoveValue alphabeta(Board b, int currentdepth, int maxdepth, int alpha, int beta) 
	{
	    nodesexpanded++;
	  /*	      
	      if node is a terminal node or depth == 0:
	      return the heuristic value of node
	  */
	    if (b.isCheckMate()) return new MoveValue(LOSSVALUE);
	    if (b.isStalemate()) return new MoveValue(0); 
	    if (currentdepth==maxdepth) return new MoveValue(utility.evaluate(b)*b.turn);
	    Move m=new Move();
	    MoveValue best= new MoveValue(-INFINITY);
	    Move[] moves=b.getValidMoves();
	    //Arrays.sort(moves, m);
	    for (Move currentmove : moves) 
	    {
		Board child=b.clone();
		child.makeMove(currentmove);
		
		MoveValue childmove = alphabeta(child, currentdepth+1,maxdepth,-beta, -alpha);
		childmove.move=currentmove;
		//reverse the sign since we're really evaluating our opponent's board state
		childmove.value=-childmove.value;
		
		if (childmove.value>best.value) {
		    best=childmove;
		    alpha=childmove.value;
		} else if (childmove.value==best.value) 
		// if we have a tie, replace the best move at random (to avoid the problem of loops)
		  //if (r.nextDouble()>0.5)
		    //best=childmove;
		

		if (beta<=alpha) break; 
	    }

	    return best;
	}
	

	public static void main(String[] args) {

		int[][] board = new int[8][8];
		Board b=new Board();
		HotSingleAgent ids=new HotSingleAgent(Integer.parseInt(args[0]));
		ids.utility=new NeuralNetwork();
		Timer t = new Timer();
		
		//convert the numeric value given as parameter 3 into minutes, and then give 10 second leeway to return a response
		long limit=Integer.parseInt(args[2])*60000-10000;
		//schedule the timeout.. if we pass the timeout, the program will exit.
		t.schedule(ids.timeout, limit);
	
		try {
			BufferedReader input =   new BufferedReader(new FileReader(args[1]));
			for (int i=0; i<8; i++) {
				String line=input.readLine();
				String[] pieces=line.split("\\s");
				for (int j=0; j<8; j++) {
					board[i][j]=Integer.parseInt(pieces[j]);
				}
			}
			String turn=input.readLine();
			b.fromArray(board);
			if (turn.equals("N")) b.setTurn(b.TURNBLACK);
			else b.setTurn(b.TURNWHITE);
			b.setShortCastle(b.TURNWHITE,false);
			b.setLongCastle(b.TURNWHITE,false);
			b.setShortCastle(b.TURNBLACK,false);
			b.setLongCastle(b.TURNBLACK,false);
		
			String st=input.readLine();
			while (st!=null) {
				if (st.equals("EnroqueC_B")) b.setShortCastle(b.TURNWHITE,true);
				if (st.equals("EnroqueL_B")) b.setLongCastle(b.TURNWHITE,true);
				if (st.equals("EnroqueC_N")) b.setShortCastle(b.TURNBLACK,true);
				if (st.equals("EnroqueL_N")) b.setLongCastle(b.TURNBLACK,true);
				st=input.readLine();
			}
		} catch (Exception e) {}
		
		Move move=ids.getBestMove(b,MAXDEPTH);	

		System.out.println(move);
		System.out.println("Nodes Expanded:" +ids.nodesexpanded);
		System.out.println("Max Depth Reached:" + ids.maxdepthreached);
		System.exit(0);
	}
}
