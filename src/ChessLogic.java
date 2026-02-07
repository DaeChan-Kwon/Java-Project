import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// --- 기물 상속 구조 ---
abstract class Piece {
    protected boolean isWhite;
    protected String type;
    public Piece(boolean isWhite, String type) { this.isWhite = isWhite; this.type = type; }
    public boolean isWhite() { return isWhite; }
    public String getType() { return type; }
    public String getImageName() { return (isWhite ? "White" : "Black") + type; }
    public abstract boolean isValidMove(Board board, int startR, int startC, int endR, int endC);

    protected boolean isPathClear(Board board, int startR, int startC, int endR, int endC) {
        int dr = Integer.compare(endR, startR);
        int dc = Integer.compare(endC, startC);
        int currR = startR + dr;
        int currC = startC + dc;
        while (currR != endR || currC != endC) {
            if (board.getPiece(currR, currC) != null) return false;
            currR += dr; currC += dc;
        }
        return true;
    }
}

class Pawn extends Piece {
    public Pawn(boolean isWhite) { super(isWhite, "Pawn"); }
    public boolean isValidMove(Board board, int startR, int startC, int endR, int endC) {
        int direction = isWhite ? -1 : 1;
        int startRow = isWhite ? 6 : 1;
        int dr = endR - startR; int dc = endC - startC;
        Piece target = board.getPiece(endR, endC);
        if (dc == 0 && dr == direction && target == null) return true;
        if (dc == 0 && startR == startRow && dr == 2 * direction && target == null) return board.getPiece(startR + direction, startC) == null;
        if (Math.abs(dc) == 1 && dr == direction && target != null && target.isWhite() != this.isWhite) return true;
        return false;
    }
}
class Rook extends Piece {
    public Rook(boolean isWhite) { super(isWhite, "Rook"); }
    public boolean isValidMove(Board board, int startR, int startC, int endR, int endC) {
        if (startR != endR && startC != endC) return false;
        return isPathClear(board, startR, startC, endR, endC);
    }
}
class Knight extends Piece {
    public Knight(boolean isWhite) { super(isWhite, "Knight"); }
    public boolean isValidMove(Board board, int startR, int startC, int endR, int endC) {
        int dr = Math.abs(endR - startR); int dc = Math.abs(endC - startC);
        return (dr == 2 && dc == 1) || (dr == 1 && dc == 2);
    }
}
class Bishop extends Piece {
    public Bishop(boolean isWhite) { super(isWhite, "Bishop"); }
    public boolean isValidMove(Board board, int startR, int startC, int endR, int endC) {
        if (Math.abs(endR - startR) != Math.abs(endC - startC)) return false;
        return isPathClear(board, startR, startC, endR, endC);
    }
}
class Queen extends Piece {
    public Queen(boolean isWhite) { super(isWhite, "Queen"); }
    public boolean isValidMove(Board board, int startR, int startC, int endR, int endC) {
        boolean straight = (startR == endR || startC == endC);
        boolean diagonal = (Math.abs(endR - startR) == Math.abs(endC - startC));
        if (!straight && !diagonal) return false;
        return isPathClear(board, startR, startC, endR, endC);
    }
}
class King extends Piece {
    public King(boolean isWhite) { super(isWhite, "King"); }
    public boolean isValidMove(Board board, int startR, int startC, int endR, int endC) {
        int dr = Math.abs(endR - startR); int dc = Math.abs(endC - startC);
        if (dr <= 1 && dc <= 1) return true;
        if (dr == 0 && dc == 2) return true;
        return false;
    }
}

// --- 보드 데이터 ---
class Board {
    private Piece[][] grid;
    private final int SIZE = 8;
    public Board() { grid = new Piece[SIZE][SIZE]; }
    public void setPiece(int r, int c, Piece p) { grid[r][c] = p; }
    public Piece getPiece(int r, int c) { return grid[r][c]; }

    public void initialize() {
        for (int i = 0; i < SIZE; i++) { grid[1][i] = new Pawn(false); grid[6][i] = new Pawn(true); }
        setupMainPieces(0, false); setupMainPieces(7, true);
        for(int r=2; r<6; r++) for(int c=0; c<8; c++) grid[r][c] = null;
    }
    private void setupMainPieces(int row, boolean isWhite) {
        grid[row][0] = new Rook(isWhite); grid[row][1] = new Knight(isWhite);
        grid[row][2] = new Bishop(isWhite); grid[row][3] = new Queen(isWhite);
        grid[row][4] = new King(isWhite); grid[row][5] = new Bishop(isWhite);
        grid[row][6] = new Knight(isWhite); grid[row][7] = new Rook(isWhite);
    }
    public String getStateString(String currentPlayer, boolean wK, boolean bK, boolean[] wR, boolean[] bR) {
        StringBuilder sb = new StringBuilder();
        for(int r=0; r<SIZE; r++) for(int c=0; c<SIZE; c++) {
            Piece p = grid[r][c]; sb.append(p == null ? "-" : p.getImageName());
        }
        sb.append(currentPlayer).append(wK).append(bK).append(wR[0]).append(wR[1]).append(bR[0]).append(bR[1]);
        return sb.toString();
    }
}

// --- 로직 컨트롤러 ---
public class ChessLogic {
    private Board board;
    private String currentPlayer = "WHITE";
    private List<String> positionHistory = new ArrayList<>();
    public boolean whiteKingMoved = false, blackKingMoved = false;
    public boolean[] whiteRookMoved = {false, false}, blackRookMoved = {false, false};

    public ChessLogic() { board = new Board(); initializeGame(); }

    public void initializeGame() {
        board.initialize(); currentPlayer = "WHITE"; positionHistory.clear();
        whiteKingMoved = false; blackKingMoved = false;
        whiteRookMoved = new boolean[]{false, false}; blackRookMoved = new boolean[]{false, false};
        recordBoardState();
    }

    public Piece getPieceAt(int r, int c) { return board.getPiece(r, c); }
    public String getCurrentPlayer() { return currentPlayer; }
    public void setCurrentPlayer(String p) { currentPlayer = p; }
    public void switchTurn() { currentPlayer = currentPlayer.equals("WHITE") ? "BLACK" : "WHITE"; }

    public boolean checkRules(int r1, int c1, int r2, int c2, Piece p) {
        if (r1 == r2 && c1 == c2) return false;
        Piece target = board.getPiece(r2, c2);
        if (target != null && target.isWhite() == p.isWhite()) return false;
        return p.isValidMove(board, r1, c1, r2, c2);
    }

    public boolean simulateMoveAndCheckSafety(int r1, int c1, int r2, int c2) {
        Piece p = board.getPiece(r1, c1); Piece target = board.getPiece(r2, c2);
        board.setPiece(r2, c2, p); board.setPiece(r1, c1, null);
        boolean safe = !isKingInCheck(currentPlayer);
        board.setPiece(r1, c1, p); board.setPiece(r2, c2, target);
        return safe;
    }

    public boolean isKingInCheck(String player) {
        boolean white = player.equals("WHITE");
        int kR = -1, kC = -1;
        for(int r=0; r<8; r++) for(int c=0; c<8; c++) {
            Piece p = board.getPiece(r, c);
            if(p instanceof King && p.isWhite() == white) { kR=r; kC=c; break; }
        }
        if(kR == -1) return false;
        for(int r=0; r<8; r++) for(int c=0; c<8; c++) {
            Piece p = board.getPiece(r, c);
            if(p != null && p.isWhite() != white && p.isValidMove(board, r, c, kR, kC)) return true;
        }
        return false;
    }

    public boolean hasLegalMoves(String player) {
        boolean white = player.equals("WHITE");
        for(int r1=0; r1<8; r1++) for(int c1=0; c1<8; c1++) {
            Piece p = board.getPiece(r1, c1);
            if(p != null && p.isWhite() == white) {
                for(int r2=0; r2<8; r2++) for(int c2=0; c2<8; c2++) {
                    if(checkRules(r1, c1, r2, c2, p) && simulateMoveAndCheckSafety(r1, c1, r2, c2)) return true;
                }
            }
        }
        return false;
    }

    public void executeMove(int r1, int c1, int r2, int c2) {
        Piece p = board.getPiece(r1, c1);
        board.setPiece(r2, c2, p); board.setPiece(r1, c1, null);
        if(p instanceof King) { if(p.isWhite()) whiteKingMoved=true; else blackKingMoved=true; }
        if(p instanceof Rook) {
            if(p.isWhite()) { if(r1==7&&c1==0) whiteRookMoved[0]=true; if(r1==7&&c1==7) whiteRookMoved[1]=true; }
            else { if(r1==0&&c1==0) blackRookMoved[0]=true; if(r1==0&&c1==7) blackRookMoved[1]=true; }
        }
        recordBoardState();
    }

    public void handleCastling(int oldR, int oldC, int newR, int newC) {
        if(Math.abs(newC - oldC) == 2) {
            int rookCol = (newC > oldC) ? 7 : 0;
            int rookTarget = (newC > oldC) ? 5 : 3;
            Piece rook = board.getPiece(newR, rookCol);
            board.setPiece(newR, rookTarget, rook); board.setPiece(newR, rookCol, null);
        }
    }

    public void promotePawn(int r, int c, Piece newPiece) { board.setPiece(r, c, newPiece); }
    public void recordBoardState() { positionHistory.add(board.getStateString(currentPlayer, whiteKingMoved, blackKingMoved, whiteRookMoved, blackRookMoved)); }
    public boolean checkThreefoldRepetition() {
        if(positionHistory.isEmpty()) return false;
        return Collections.frequency(positionHistory, positionHistory.get(positionHistory.size()-1)) >= 3;
    }
    public boolean checkInsufficientMaterial() {
        List<String> pieces = new ArrayList<>();
        for(int r=0; r<8; r++) for(int c=0; c<8; c++) if(board.getPiece(r, c) != null) pieces.add(board.getPiece(r, c).getType());
        if(pieces.size() <= 2) return true;
        if(pieces.size() == 3) for(String t : pieces) if(t.equals("Pawn") || t.equals("Rook") || t.equals("Queen")) return false;
        return pieces.size() == 3;
    }
    public Piece createPiece(boolean isWhite, String type) {
        switch(type) {
            case "Pawn": return new Pawn(isWhite); case "Rook": return new Rook(isWhite); case "Knight": return new Knight(isWhite);
            case "Bishop": return new Bishop(isWhite); case "Queen": return new Queen(isWhite); case "King": return new King(isWhite);
            default: return null;
        }
    }
    public void setPiece(int r, int c, Piece p) { board.setPiece(r, c, p); }
}