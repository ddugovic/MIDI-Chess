package org.chernovia.chess.client;

//TODO: http://stackoverflow.com/questions/4155364/how-to-prefetch-image-in-gwt
//Pieces determine rhythm?
//Ornicar sez drop fucking GWT
//50 move bug?  Wtf, test Naka-Rybka with "normal" pgnparser
//piece animation
//evaluation affects harmony?


import java.util.List;

import org.chernovia.midi.client.GWT_MIDI;
import org.chernovia.midi.client.GWT_MIDI_Listener;

import com.codethesis.pgnparse.MalformedMoveException;
import com.codethesis.pgnparse.PGNGame;
import com.codethesis.pgnparse.PGNMove;
import com.codethesis.pgnparse.PGNParseException;
import com.codethesis.pgnparse.PGNParser;
import com.google.gwt.canvas.client.Canvas;
import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.UmbrellaException;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Label;;

public class MIDIChess implements GWT_MIDI_Listener, EntryPoint {

	static final int EMPTY = 0, PAWN = 1, KNIGHT = 2, BISHOP = 3, ROOK = 4, QUEEN = 5, KING = 6;
	static final String[] Pieces = { "P","N","B","R","Q","K" };
	static final int[] STARTING_PIECES = { ROOK, KNIGHT, BISHOP, QUEEN, KING, BISHOP, KNIGHT, ROOK };
	static final int[] MAJOR_SCALE = { 0,2,4,5,7,9,11,12 }; 
	static final int[] MINOR_SCALE = { 0,2,3,5,7,8,10,12 }; 
	static final int[] BLUES_SCALE = { 0,2,3,4,7,9,11,12 }; 
	GWT_MIDI midi;
	private MIDIProg[] INSTRUMENTS = {
			new MIDIProg("acoustic_grand_piano",0),
			new MIDIProg("dulcimer",15),
			new MIDIProg("acoustic_guitar_nylon",24),
			new MIDIProg("violin",40),
			new MIDIProg("choir_aahs",52),
			new MIDIProg("orchestra_hit",55),
			new MIDIProg("clarinet",71),
 			new MIDIProg("ocarina",79),
			new MIDIProg("kalimba",108),
 			new MIDIProg("koto",107),
			new MIDIProg("banjo",105),
			new MIDIProg("gunshot",127)
			
	};  
	private VerticalPanel controlPanel = new VerticalPanel();
	private HorizontalPanel appPanel = new HorizontalPanel();
	private Button playButton = new Button("Play");
	private ListBox leadBox = new ListBox();
	private ListBox whiteCompBox = new ListBox();
	private ListBox blackCompBox = new ListBox();
	private ListBox tempoBox = new ListBox();
	private TextArea pgnTxt = new TextArea();
	private Label loadLab = new Label("LOADING MIDI: 0%");
	private Canvas boardCan;
	Image[] BlackPieces, WhitePieces;
	Image boardImg;
	Player player;
	
	class MIDIProg {
		String Instrument; int patch; 
		public MIDIProg(String i, int p) { Instrument = i; patch = p; }
	}
	
    class Player extends Timer {
    	
    	PGNGame game;
    	Context2d ctx;
    	int[][] chessBoard;
    	int moveNum, melody, volume, middleC, tempo;
    	
    	public Player(PGNGame g, Context2d c) {
    		game = g; ctx = c; chessBoard = initBoard(); 
    		moveNum = 0; middleC = 60; volume = 36; melody = 100;
    		midi.setProgram(0, INSTRUMENTS[leadBox.getSelectedIndex()].patch);
    		midi.setProgram(1, INSTRUMENTS[whiteCompBox.getSelectedIndex()].patch);
    		midi.setProgram(2, INSTRUMENTS[blackCompBox.getSelectedIndex()].patch);
    	}
    	
    	public void start(int t) {
    		tempo = t; if (this.isRunning()) this.cancel();
    		this.scheduleRepeating(tempo);
    	}
    	
        @Override
        public void run() {
        	if (game.getMove(moveNum).isEndGameMarked()) {
        		stop(); playButton.setText("Play"); return;
        	}
        	nextMove(moveNum++); drawBoard(ctx,chessBoard);
        }
        
        public void stop() {
        	midi.noteOff(0, melody, 0); 
        	this.cancel(); 
        }
        
        private void nextMove(int i) {
        	PGNMove move = game.getMove(i);
        	boolean black = (move.getColor() == com.codethesis.pgnparse.Color.black);
        	int blackFlag = black ? -1 : 1;
			int x1,x2,y1,y2; x1 = x2 = y1 = y2 = 0;
			String piece = move.getPiece();
			if (move.isCastle()) {
				x1 = 4; y1 = y2 = black ? 7 : 0;
				chessBoard[x1][y2] = EMPTY;
				if (move.isKingSideCastle()) {
					chessBoard[7][y2] = EMPTY;
					chessBoard[5][y2] = ROOK * blackFlag;
					x2 = 6; chessBoard[x2][y2] = KING * blackFlag;
				}
				else if (move.isQueenSideCastle()) {
					chessBoard[0][y2] = EMPTY;
					chessBoard[3][y2] = ROOK * blackFlag;
					x2 = 2; chessBoard[x2][y2] = KING * blackFlag;
				}
			}
			else {
				if (move.isEnpassantCapture()) {
					int cx = move.getEnpassantPieceSquare().charAt(0)-'a';
					int cy = (move.getEnpassantPieceSquare().charAt(1)-'0'-1);
					chessBoard[cx][cy] = EMPTY;
				}
				x1 = move.getFromSquare().charAt(0)-'a'; y1 = move.getFromSquare().charAt(1)-'0'-1;
				x2 = move.getToSquare().charAt(0)-'a'; y2 = move.getToSquare().charAt(1)-'0'-1;
				chessBoard[x1][y1] = EMPTY; 
				if (move.isPromoted()) piece = move.getPromotion();
				switch (piece) {
					case "P": chessBoard[x2][y2] = PAWN * blackFlag; break;
					case "N": chessBoard[x2][y2] = KNIGHT * blackFlag; break;
					case "B": chessBoard[x2][y2] = BISHOP * blackFlag; break;
					case "R": chessBoard[x2][y2] = ROOK * blackFlag; break;
					case "Q": chessBoard[x2][y2] = QUEEN * blackFlag; break;
					case "K": chessBoard[x2][y2] = KING * blackFlag; break;
					default: log("ERROR: unknown piece: " + piece);
				}
			}
			int from = 16 + (x1 * 12) +  MINOR_SCALE[y1];
			int to = 16 + (x2 * 12) + MINOR_SCALE[y2];
			float t = (tempo/1000f)/2;
			int chan = black ? 2 : 1;
			log("Playing " + move.getFullMove() + ": " + from + " -> " + to);
			midi.noteOn(chan,from,volume,0); midi.noteOff(chan,from,t);
			midi.noteOn(chan,to,volume,t); midi.noteOff(chan,to,t+t); 
			if (move.isCaptured()) { 
				int n = 64 + MINOR_SCALE[x2];
				if (n != melody) {
					midi.noteOff(0,melody,0); melody = n; midi.noteOn(0,melody,volume,0);
				}
			}
        }
    };
			
	public void onModuleLoad() {
		log("Fetching images...");
		ChessPieceBundle images = GWT.create(ChessPieceBundle.class);
		BlackPieces = new Image[6]; WhitePieces = new Image[6];
		BlackPieces[0] = new Image(images.BP());
		BlackPieces[1] = new Image(images.BN());
		BlackPieces[2] = new Image(images.BB());
		BlackPieces[3] = new Image(images.BR());
		BlackPieces[4] = new Image(images.BQ());
		BlackPieces[5] = new Image(images.BK());
		WhitePieces[0] = new Image(images.WP());
		WhitePieces[1] = new Image(images.WN());
		WhitePieces[2] = new Image(images.WB());
		WhitePieces[3] = new Image(images.WR());
		WhitePieces[4] = new Image(images.WQ());
		WhitePieces[5] = new Image(images.WK());
		boardImg = new Image(images.board());

		midi = new GWT_MIDI(this);
		String[] instruments = new String[INSTRUMENTS.length]; //{"clarinet","violin"};
		for (int i=0;i<INSTRUMENTS.length;i++) {
			leadBox.addItem(INSTRUMENTS[i].Instrument);
			whiteCompBox.addItem(INSTRUMENTS[i].Instrument);
			blackCompBox.addItem(INSTRUMENTS[i].Instrument);
			instruments[i] = INSTRUMENTS[i].Instrument;
		}
		midi.initMIDI(instruments);
		
		leadBox.setSelectedIndex(7);
		whiteCompBox.setSelectedIndex(8);
		blackCompBox.setSelectedIndex(8);
		
		for (int i=250;i<2500;i+=250) {
			tempoBox.addItem(i+"");
		}
		tempoBox.setSelectedIndex(1);
		
		pgnTxt.setSize("400px","400px");
		pgnTxt.setText(initPgn());	
				
		playButton.setVisible(false);
		playButton.addClickHandler(new ClickHandler() { public void onClick(ClickEvent event) { playPgn(); } });
		
		controlPanel.add(new Label("Capture Melody:"));
		controlPanel.add(leadBox);
		controlPanel.add(new Label("White Instrument:"));
		controlPanel.add(whiteCompBox);
		controlPanel.add(new Label("Black Instrument:"));
		controlPanel.add(blackCompBox);
		controlPanel.add(new Label("Tempo:"));
		controlPanel.add(tempoBox);
		controlPanel.add(new Label("PGN:"));
		controlPanel.add(pgnTxt);
		controlPanel.add(playButton);
		controlPanel.add(loadLab);
		appPanel.add(controlPanel);
		
		//RootPanel.get("MCBox").add(mainPanel);
		
		boardCan = Canvas.createIfSupported();
        boardCan.setStyleName("chessboard");     // *** must match the div tag in CanvasExample.html ***
		boardCan.setWidth("800px");
		boardCan.setCoordinateSpaceWidth(800);
		boardCan.setHeight("800px");      
		boardCan.setCoordinateSpaceHeight(800);
		appPanel.add(boardCan);

		RootPanel.get("MCBox").add(appPanel);
 	}
	
	public PGNGame parsePgn(String pgn) {
		log("PGN: " + pgn);
		List<PGNGame> games = null; 
		try { games = PGNParser.parse(pgn);	} 
		catch (Exception e) { 
			log("Well, fuck: " + e.getLocalizedMessage());
			StackTraceElement[] stack = e.getStackTrace(); 
			for (int i=0;i<stack.length;i++) {
				log(stack[i].toString());
			}
		} 
		return games.get(0);
	}
	
	public void playPgn() {
		if (playButton.getText().equals("Play")) {
			player = new Player(parsePgn(pgnTxt.getText()),boardCan.getContext2d());
			player.start(Integer.parseInt(tempoBox.getValue(tempoBox.getSelectedIndex())));
			playButton.setText("Stop");
		}
		else {
			player.cancel(); playButton.setText("Play");
		}
	}
	
    private int[][] initBoard() {
		log("Initializing board...");
		int[][] board = new int[8][8];
		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				if (y==1) board[x][y] = PAWN;
				else if (y==6) board[x][y] = -PAWN;
				else if (y==0) { board[x][y] = STARTING_PIECES[x]; }
				else if (y==7) { board[x][y] = -STARTING_PIECES[x]; }
				else board[x][y] = EMPTY;
			}
		}
		return board;
	}
    
	private void drawBoard(Context2d ctx, int[][] chessBoard) {
		//log("Drawing board...");
		int w = boardCan.getCoordinateSpaceWidth();
		int h = boardCan.getCoordinateSpaceHeight();
		ctx.setFillStyle("ffffff");
		ctx.fillRect(0, 0, w, h);
		Image img = boardImg;
		ctx.drawImage(ImageElement.as(img.getElement()),0,0,w,h);
		int squareX = w/8; int squareY = h/8;
		for(int x=0;x<8;x++) for (int y=0;y<8;y++) {
			int piece = chessBoard[x][y];
			if (piece != EMPTY) {
				if (piece < 0) img = BlackPieces[(-piece)-1]; else img = WhitePieces[piece-1];
				ctx.drawImage(ImageElement.as(img.getElement()), x*squareX, (7-y)*squareY,squareX,squareY);
			}
		}
	}
	
	public native String initPgn() /*-{
		return $doc.getElementById("pgntxt").value;
	}-*/;

	public native void log(String msg) /*-{
		console.log(msg);
	}-*/;
	
	public void onLoad() {
		playButton.setVisible(true); loadLab.setVisible(false);
		drawBoard(boardCan.getContext2d(),initBoard());
	}

	@Override
	public void loadProgress(int progress) {
		loadLab.setText("LOADING MIDI: " + progress + "%");
		
	}
}
