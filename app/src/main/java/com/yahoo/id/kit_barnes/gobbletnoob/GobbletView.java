package com.yahoo.id.kit_barnes.gobbletnoob;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;


//statusview.setText(new StringBuilder("Cell #").append(cell).append(" pressed").toString());

public class GobbletView extends View {
	
	private int cubit;		// size of board cell in pixels
	private int leftmargin;
	private int topmargin;
	private Bitmap cellbg;		// cell background image
	private RectF rectf;		// used to place cellbg on board (multiple times)

	private Paint whitePaint;
	private Paint blackPaint;
	private Paint winlinePaint;
	private Paint borderPaint;
	private int backgroundColor;
	
	Gobblet gobblet;
	Game game;
	boolean touchenabled;
	Thread thread;
	
	int cx, cy;			// coordinates of lifted cup
	
	public GobbletView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		rectf = new RectF();
		touchenabled = false;
		
        whitePaint = new Paint();
        whitePaint.setAntiAlias(true);
        whitePaint.setStyle(Paint.Style.FILL);
        whitePaint.setColor(getResources().getColor(R.color.white));
        blackPaint = new Paint(whitePaint);
        blackPaint.setColor(getResources().getColor(R.color.black));
        winlinePaint = new Paint(whitePaint);
        winlinePaint.setColor(getResources().getColor(R.color.winline));
        borderPaint = new Paint(whitePaint);
        borderPaint.setColor(getResources().getColor(R.color.border));
        backgroundColor = getResources().getColor(R.color.background);        
	}
	
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wmax =  MeasureSpec.getSize(widthMeasureSpec);
        int hmax =  MeasureSpec.getSize(heightMeasureSpec);
    	// ensure largest possible board
        // hopefully in portrait orientation
    	//float size = (wmax*6)<(hmax*4) ? wmax/4F : hmax/6F;
    	setMeasuredDimension(wmax, hmax);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setCubit(w, h);
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    	super.onLayout(changed, left, top, right, bottom);
    	setCubit(right-left, bottom-top);
    }

    protected void setCubit(int w, int h) {
    	cubit = (w*6)<(h*4) ? w/4 : h/6 ;
    	leftmargin = ( getWidth() - (4*cubit) ) / 2;
    	topmargin = ( getHeight() - (6*cubit) ) / 2;
     	cellbg = Bitmap.createBitmap((int)cubit, (int)cubit, Bitmap.Config.ARGB_8888);
    	Canvas c = new Canvas(cellbg);
        c.drawColor(backgroundColor);
    	float fhc = cubit/2;
    	float r = fhc*1.41421F;
    	c.drawCircle(-fhc, fhc, r, borderPaint);
    	c.drawCircle(3*fhc, fhc, r, borderPaint);
    	c.drawCircle(fhc, -fhc, r, borderPaint);
    	c.drawCircle(fhc, 3*fhc, r, borderPaint);
        winlinePaint.setStrokeWidth(cubit/16);
    }
    	 
    @Override
    protected void onDraw(Canvas canvas) {
    	canvas.drawColor(borderPaint.getColor());
    	for (int r = 0; r < 4; r++) {
    		for (int c = 0; c < 4; c++) {
		     	rectf.set( c*cubit+leftmargin, (r+1)*cubit+topmargin,
		     				(c+1)*cubit+leftmargin, (r+2)*cubit+topmargin);
		    	canvas.drawBitmap(cellbg, null, rectf, null);
    		}
    	}
    	if (game != null) {
	    	for (int i = 0; i < 22; i++) {
	    		Game.Cup cup = game.stack[i];
	    		if (cup != null) {
	    			canvas.drawCircle(cell2x(i), cell2y(i), cup.size*cubit/12,
	    					cup.white? whitePaint : blackPaint);
	    		}
	    	}
	    	for (int i = 0; i < game.wins; i++) {
	    		int sa = Game.lines[game.winlines[i]][0];
	    		int sb = Game.lines[game.winlines[i]][3];
	    		canvas.drawLine(cell2x(sa),cell2y(sa),cell2x(sb),cell2y(sb),winlinePaint);
	    	}
	    	if (game.liftedcup != null) {
	    		canvas.drawCircle(cx, cy, game.liftedcup.size*cubit/12,
	    				game.liftedcup.white? whitePaint : blackPaint);
	    	}
    	}
    }

    @Override
	public boolean onTouchEvent(MotionEvent event){
    	if (touchenabled) {
	    	int action = event.getAction();
			cx = (int)event.getX();
			cy = (int)event.getY();
			int cell = xy2cell(cx,cy);
	    	if (action == MotionEvent.ACTION_DOWN) {
	    		if (gobblet.pick(cell) || game.liftedcup != null) invalidate();
	    		return true;
	    	} else if (action == MotionEvent.ACTION_MOVE) {
	    		if (game.liftedcup != null) invalidate();
	    		return true;
	    	} else if (action == MotionEvent.ACTION_UP) {
	    		if (gobblet.place(cell)) invalidate();
	    		return true;
	    	}
    	}
		return false;
    }

    private int xy2cell(int x, int y) {
    	y -= topmargin + cubit/4;
    	if (y < 0) return -1;
    	x -= leftmargin + cubit/4;
    	if (x < 0) return -1;
    	y /= cubit/2;
    	if (y%2 != 0) return -1;
    	y /= 2;
    	if (y >= 6) return -1;
    	if ( y == 0 || y == 5 ) {
    		x -= cubit/2;
    		if (x < 0) return -1;
    		x /= cubit/2;
    		if (x%2 != 0) return -1;
    		x /= 2;
    		if (x >= 3) return -1;
    		if (y == 5) y = 3;
    		return 16 + x + y;
    	}
    	x /= cubit/2;
    	if (x%2 != 0) return -1;
		x /= 2;
    	if (x >= 4) return -1;
    	return (y-1)*4 + x;
    }
    
    private int cell2x(int cell) {	// returns horizontal center of cell
    	int c, offset;
    	if (cell >= 16) {
    		c = (cell - 16) % 3;
    		offset = cubit;
    	} else {
    		c = cell % 4;
    		offset = cubit/2;
    	}
    	return leftmargin + offset + c*cubit;
    }
    
    private int cell2y(int cell) {	// returns vertical center of cell
    	int r;
    	if (cell < 16) r = 1 + cell/4;
    	else if (cell < 19) r = 0;
    	else r = 5;
    	return topmargin + cubit/2 + r*cubit;
    }
    
    void setxy(int cell) {
    	cx = cell2x(cell);
    	cy = cell2y(cell);
    }
    
	void animateto(int to) {
		/*
		 * animate() is called only for undos and ai moves
		 * therefore need never check for legality
		 * does not modify game.history
		 */
		int dx, dy;
		if (to < 22) {
			dx = cell2x(to) - cx;
			dy = cell2y(to) - cy;
		} else {	// exposed win by random ai
					// move to center of board
			dx = leftmargin + (2*cubit) - cx;
			dy =  topmargin + (3*cubit) - cy;
		}
		thread = new Thread(new AnimationTask(this, dx, dy, to));
		thread.start();
	}
	private class AnimationTask implements Runnable {
		GobbletView view;
		float dx, dy;
		int to, count, frames;
		int fromx, fromy;
		public AnimationTask(GobbletView view, int dx, int dy, int to) {
			this.view = view;
			frames = 30;
			this.dx = (float)dx/frames;
			this.dy = (float)dy/frames;
			this.to = to;
			fromx = view.cx;
			fromy = view.cy;
			count = 0;
		}
		public void run() {
			try {
				while (count++ < frames) {
					view.cx = fromx + (int)(count*dx);
					view.cy = fromy + (int)(count*dy);
					view.post(new Runnable() {
						public void run() {
							view.invalidate();
							}
						});
					Thread.sleep(40);
				}
			} catch (InterruptedException e) {
				return;
			}
			view.post(new Runnable() {
				public void run() {
					view.animationdone(to);
					}
			});
		}
	}
	void animationdone(int to) {
		if (to < 22) {
			game.drop(to);
			game.win(to);
		}
		thread = null;
		invalidate();
		if (gobblet.undoing > 0) gobblet.undoing--;
		gobblet.whatsnext();
	}
	
	void killthread() {
		if (thread!=null && thread.isAlive()) {
			thread.interrupt();
			try {
				thread.join();
			} catch (InterruptedException e) {}
		}
		thread = null;
	}
	
}
