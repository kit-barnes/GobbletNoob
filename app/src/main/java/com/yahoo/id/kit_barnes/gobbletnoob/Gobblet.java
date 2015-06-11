package com.yahoo.id.kit_barnes.gobbletnoob;

import android.os.Bundle;
import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class Gobblet extends Activity {
	
	GobbletView view;
	TextView statusview;
	Game game;
	int whiteplayer = 0, blackplayer = 1;		// AI level - 0:manual, 1:random
	int undoing = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_gobblet);
        view = (GobbletView)findViewById(R.id.gobbletView);
        view.gobblet = this;
        statusview = (TextView)findViewById(R.id.textView);
        SharedPreferences saved = getPreferences(MODE_PRIVATE);
        String hist = "";
		if (saved != null) {
			whiteplayer = saved.getInt("whiteplayer",0);
			blackplayer = saved.getInt("blackplayer",2);
	        hist = saved.getString("history", "");
			undoing = saved.getInt("undoing",0);
	        view.cx = saved.getInt("cx",0);
	        view.cy = saved.getInt("cy",0);
		}
        newGame(hist);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // if animating move, terminate thread and complete move
        view.killthread();
        // if undoing, just terminate animation thread
        SharedPreferences state = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor ed = state.edit();
		ed.putInt("whiteplayer", whiteplayer);
		ed.putInt("blackplayer", blackplayer);
		ed.putInt("undoing", undoing);
		ed.putString("history", game.history.toString());
		ed.putInt("cx", view.cx);
		ed.putInt("cy", view.cy);
		ed.commit();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_gobblet, menu);
        return true;
    }
    
    public void newGame(String hist) {
    	view.killthread();   	// stop animation
    	undoing = 0;
        view.game = game = new Game(hist);
        view.invalidate();
        whatsnext();
    }
    
    static final int DIALOG_OPTIONS_ID = 0;
    static final int DIALOG_HELP_ID = 1;
    Dialog dialog;
    
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        Button button;
        switch(id) {
        case DIALOG_OPTIONS_ID:
        	dialog = new Dialog(this);
        	dialog.setContentView(R.layout.dialog_options);
        	dialog.setTitle(getResources().getString(R.string.options_title));
        	
            Spinner spinner = (Spinner) dialog.findViewById(R.id.white_spinner);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    this, R.array.players, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            PlayerSelectedListener listener = new PlayerSelectedListener();
            spinner.setOnItemSelectedListener(listener);
        	
            spinner = (Spinner) dialog.findViewById(R.id.black_spinner);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setOnItemSelectedListener(listener);
                    	
        	dialog.findViewById(R.id.layout_options).setPadding(10, 0, 10, 10);
        	button = (Button) dialog.findViewById(R.id.options_done_button);
        	button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					dialog.cancel();
				}
        	});
        	break;
        case DIALOG_HELP_ID:
        	dialog = new Dialog(this);
        	dialog.setContentView(R.layout.dialog_help);
        	dialog.setTitle(getResources().getString(R.string.help_title));
        	
        	dialog.findViewById(R.id.layout_help).setPadding(10, 0, 10, 10);
        	button = (Button) dialog.findViewById(R.id.button_help_done);
        	button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					dialog.cancel();
				}
        	});
        	break;
        default:
            dialog = null;
        }
        return dialog;
    }
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
    	super.onPrepareDialog(id, dialog, args);
        switch(id) {
        case DIALOG_OPTIONS_ID:
            Spinner spinner = (Spinner) dialog.findViewById(R.id.white_spinner);
            spinner.setSelection(whiteplayer);
            spinner = (Spinner) dialog.findViewById(R.id.black_spinner);
            spinner.setSelection(blackplayer);
        	break;
        case DIALOG_HELP_ID:
        default:
        }   	
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
	        case R.id.menu_new:
	            newGame("");
	            return true;
	        case R.id.menu_undo:
	            undo();
	            return true;
            case R.id.menu_options:
            	showDialog(DIALOG_OPTIONS_ID);
                return true;
            case R.id.menu_help:
            	showDialog(DIALOG_HELP_ID);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    public class PlayerSelectedListener implements OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> parent, View clicked, int pos, long id) {
         	if (dialog.findViewById(R.id.white_spinner) == parent) whiteplayer = pos;
         	else if (dialog.findViewById(R.id.black_spinner) == parent) blackplayer = pos;
         	if (view.thread == null) whatsnext();
            }
        public void onNothingSelected(AdapterView<?> parent) {
          // Do nothing.
        }
    }
    
    boolean pick(int stack) {
		if (game.pick(stack)) {
			statusview.setText( (game.wins == 0)?
					( game.whitesturn()?
					R.string.waitWhitePlace : R.string.waitBlackPlace ) :
					( game.whitesturn()?
	    			R.string.winBlack : R.string.winWhite ) );
			return true;
 		}
    	return false;
    }
    
    boolean place(int stack) {
		if (game.place(stack)) {
			whatsnext();
			return true;
		}
    	return false;
    }
    
    void undo() {
    	if (undoing++ == 0) {
    		view.killthread();
    		whatsnext();
    		return;
    	}
    }
    
    void whatsnext() {
		int hlen = game.history.length();
												//if (game.wins > 0) undoing++;		//for testing
    	if (undoing > 0) {
	        view.touchenabled = false;
    		if (hlen == 0) {
    			undoing = 0;
    		} else {
    			statusview.setText(R.string.undoing);
    			int si;
    			if (hlen%2 == 0) {
    				if (game.liftedcup == null) {
	    				game.lift(si = game.history.charAt(--hlen) - 0x31);
	    				view.setxy(si);
    				} else hlen--;		// just discard intended ai move destination
    			}
    			si = game.history.charAt(--hlen) - 0x31;
    			game.history.setLength(hlen);
    			game.wins = 0;
    			view.animateto(si);
	    		return;
    		}
    	}
    	boolean whitesturn = game.whitesturn();
    	if (game.wins > 0) {
			statusview.setText( whitesturn?
	    			R.string.winBlack : R.string.winWhite );
    		if (game.liftedcup == null) {
	    		view.touchenabled = false;
    		}
    		return;
    	}
    	int ai = whitesturn ? whiteplayer : blackplayer;
    	if (ai == 0) {		// manual move
	        statusview.setText( game.liftedcup == null ?
	        		(whitesturn? R.string.waitWhitePick : R.string.waitBlackPick) :
	        			(whitesturn? R.string.waitWhitePlace : R.string.waitBlackPlace)	);
            view.touchenabled = true;
    	} else {			// ai move
            view.touchenabled = false;
            statusview.setText(whitesturn?
					R.string.thinkingWhite : R.string.thinkingBlack);
            int move = game.getmove(ai);
            if (move == 0) {			// error!!!!
            	return;
            }
    		int from = move/32;
    		int to = move%32;
    		if (from < 22) {
    			game.lift(from);
    			view.setxy(from);
    			game.history.append((char)(from + 0x31));
    		}
			statusview.setText( whitesturn ?
					R.string.waitWhitePlace : R.string.waitBlackPlace );
			if (game.win(game.liftedfrom)) to = 22;
			else game.history.append((char)(to + 0x31));
            view.animateto(to);
    	}
    }
    
}
