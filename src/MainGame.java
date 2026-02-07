import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MainGame extends JFrame {
    private ChessLogic logic;
    private int SQUARE_SIZE;
    private int WINDOW_WIDTH;
    private int WINDOW_HEIGHT;

    // UI Components
    private JButton[][] squares = new JButton[8][8];
    private JPanel boardPanel, capturedBlackPanel, capturedWhitePanel;
    private JTextArea logBlackArea, logWhiteArea;
    private JLabel timerBlackLabel, timerWhiteLabel;
    private JButton surrenderBlackBtn, surrenderWhiteBtn, saveBtn;

    // Game State
    private int timerWhite = 900;
    private int timerBlack = 900;
    private boolean gameActive = true;
    private boolean isAnimating = false;
    private Thread gameThread;
    private boolean isThreadRunning = false;

    // Selection & Moves
    private int selectedRow = -1;
    private int selectedCol = -1;
    private List<Point> validMoves = new ArrayList<>();

    // 유니코드 fallback
    private static final java.util.Map<String, String> UNICODE_PIECES = new java.util.HashMap<>();
    static {
        UNICODE_PIECES.put("WhiteKing", "♔"); UNICODE_PIECES.put("WhiteQueen", "♕");
        UNICODE_PIECES.put("WhiteRook", "♖"); UNICODE_PIECES.put("WhiteBishop", "♗");
        UNICODE_PIECES.put("WhiteKnight", "♘"); UNICODE_PIECES.put("WhitePawn", "♙");
        UNICODE_PIECES.put("BlackKing", "♚"); UNICODE_PIECES.put("BlackQueen", "♛");
        UNICODE_PIECES.put("BlackRook", "♜"); UNICODE_PIECES.put("BlackBishop", "♝");
        UNICODE_PIECES.put("BlackKnight", "♞"); UNICODE_PIECES.put("BlackPawn", "♟");
    }

    public MainGame(boolean loadFromSave) {
        logic = new ChessLogic();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        WINDOW_WIDTH = (int) (screenSize.width * 0.95);
        WINDOW_HEIGHT = (int) (screenSize.height * 0.9);
        int availableHeight = WINDOW_HEIGHT - 100;
        SQUARE_SIZE = Math.min(availableHeight / 8, 120);

        setTitle("Chess Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setResizable(true);

        getLayeredPane().setLayout(null);

        ResourceManager.preloadImages(SQUARE_SIZE);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0)) {
            private Image bgImage = ResourceManager.getDialogImage("background.jpg");
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImage != null && bgImage.getWidth(null) > 0) {
                    g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(new Color(26, 26, 46));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        mainPanel.setBackground(new Color(26, 26, 46));

        mainPanel.add(createTopPanel(), BorderLayout.NORTH);
        mainPanel.add(createLeftPanel(), BorderLayout.WEST);
        mainPanel.add(createBoardPanel(), BorderLayout.CENTER);
        mainPanel.add(createRightPanel(), BorderLayout.EAST);

        setContentPane(mainPanel);
        setVisible(true);

        if (loadFromSave) loadGame();
        else {
            logic.initializeGame();
            updateBoardDisplay();
        }
        startGameThread();
    }

    // ==================== 항복(Surrender) 로직 ====================
    private void surrender(String surrenderPlayer) {
        if (!gameActive) return;
        gameActive = false;
        isThreadRunning = false;
        String winner = surrenderPlayer.equals("WHITE") ? "BLACK" : "WHITE";
        showVictoryDialog(winner);
    }

    // ==================== 게임 종료 로직 ====================
    private void gameOver(boolean isDraw, String reason) {
        gameActive = false;
        isThreadRunning = false;
        if (isDraw) {
            showEndDialog("DRAW!", "", "draw.wav", "Draw.png");
        } else {
            String winner = logic.getCurrentPlayer().equals("WHITE") ? "BLACK" : "WHITE";
            showVictoryDialog(winner);
        }
    }

    // ==================== 잡기 애니메이션 로직 ====================
    private void showCaptureAnimation(String attacker, String victim) {
        String attPrefix = attacker.startsWith("Black") ? "B" : "W";
        String attType = attacker.substring(5);
        String attName = attPrefix + attType;

        String vicPrefix = victim.startsWith("Black") ? "B" : "W";
        String vicType = victim.substring(5);
        String vicName = vicPrefix + vicType;

        String fileName = attName + "Cap" + vicName + ".png";

        ImageIcon icon = ResourceManager.getAnimationImage(fileName);

        if (icon != null && icon.getIconWidth() > 0) {
            JLabel animLabel = new JLabel(icon);
            animLabel.setSize(icon.getIconWidth(), icon.getIconHeight());
            int x = (getWidth() - animLabel.getWidth()) / 2;
            int y = (getHeight() - animLabel.getHeight()) / 2;
            animLabel.setLocation(x, y);

            getLayeredPane().add(animLabel, JLayeredPane.MODAL_LAYER);
            getLayeredPane().repaint();

            Timer removeTimer = new Timer(1500, e -> {
                getLayeredPane().remove(animLabel);
                getLayeredPane().repaint();
            });
            removeTimer.setRepeats(false);
            removeTimer.start();
        }
    }

    // ==================== UI 패널 생성 ====================

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topPanel.setOpaque(false);
        saveBtn = createStyledMenuButton("Save & Main Menu", new Color(70, 130, 180));
        saveBtn.addActionListener(e -> saveGameAndExit());
        topPanel.add(saveBtn);
        return topPanel;
    }

    private JButton createStyledMenuButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return btn;
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        leftPanel.setBackground(new Color(26, 26, 46, 0));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 50, 30));
        leftPanel.setPreferredSize(new Dimension(Math.min(350, WINDOW_WIDTH / 5), WINDOW_HEIGHT));

        leftPanel.add(createLogSection("BLACK", new Color(30, 30, 30), Color.WHITE));
        leftPanel.add(createLogSection("WHITE", new Color(245, 245, 245), Color.BLACK));
        return leftPanel;
    }

    private JPanel createLogSection(String player, Color bg, Color fg) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(bg);
        panel.setBorder(new LineBorder(new Color(101, 67, 33), 3));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(player.equals("BLACK") ? new Color(40,40,40) : new Color(235,235,235));

        JLabel timerLbl = new JLabel("15:00");
        timerLbl.setFont(new Font("Courier New", Font.BOLD, 16));
        timerLbl.setForeground(fg);
        timerLbl.setHorizontalAlignment(SwingConstants.CENTER);
        timerLbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        JButton surrenderBtn = new JButton("RESIGN");
        surrenderBtn.setBackground(new Color(150, 60, 60));
        surrenderBtn.setForeground(Color.WHITE);
        surrenderBtn.addActionListener(e -> surrender(player));

        if(player.equals("BLACK")) {
            timerBlackLabel = timerLbl;
            surrenderBlackBtn = surrenderBtn;
        } else {
            timerWhiteLabel = timerLbl;
            surrenderWhiteBtn = surrenderBtn;
        }

        topBar.add(timerLbl, BorderLayout.CENTER);
        topBar.add(surrenderBtn, BorderLayout.EAST);

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Arial Unicode MS", Font.PLAIN, 16));

        if(player.equals("BLACK")) {
            logBlackArea = logArea;
            logArea.setBackground(new Color(20, 20, 20));
            logArea.setForeground(Color.LIGHT_GRAY);
        } else {
            logWhiteArea = logArea;
            logArea.setBackground(Color.WHITE);
            logArea.setForeground(Color.DARK_GRAY);
        }

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        rightPanel.setBackground(new Color(26, 26, 46, 0));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 50, 30));
        rightPanel.setPreferredSize(new Dimension(Math.min(350, WINDOW_WIDTH / 5), WINDOW_HEIGHT));

        capturedBlackPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        capturedBlackPanel.setBackground(new Color(30, 30, 30));
        capturedWhitePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        capturedWhitePanel.setBackground(new Color(245, 245, 245));

        rightPanel.add(createCapturedSection("BLACK", capturedBlackPanel));
        rightPanel.add(createCapturedSection("WHITE", capturedWhitePanel));
        return rightPanel;
    }

    private JPanel createCapturedSection(String title, JPanel contentPanel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(title.equals("BLACK") ? new Color(30,30,30) : new Color(245,245,245));
        panel.setBorder(new LineBorder(new Color(101, 67, 33), 3));

        JLabel titleLbl = new JLabel(title + " CAPTURED");
        titleLbl.setHorizontalAlignment(JLabel.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLbl.setForeground(title.equals("BLACK") ? Color.WHITE : Color.BLACK);
        titleLbl.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        panel.add(titleLbl, BorderLayout.NORTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBoardPanel() {
        JPanel wrapperPanel = new JPanel(new GridBagLayout());
        wrapperPanel.setBackground(new Color(26, 26, 46, 0));

        boardPanel = new JPanel(new GridLayout(8, 8));
        boardPanel.setPreferredSize(new Dimension(SQUARE_SIZE * 8, SQUARE_SIZE * 8));
        boardPanel.setBorder(new LineBorder(new Color(139, 90, 43), 5));

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                JButton square = new JButton();
                square.setFocusPainted(false);
                square.setBorderPainted(false);
                final int r = row, c = col;
                square.addActionListener(e -> handleSquareClick(r, c));
                squares[row][col] = square;
                boardPanel.add(square);
            }
        }
        wrapperPanel.add(boardPanel);
        return wrapperPanel;
    }

    // ==================== 게임 플레이 로직 ====================

    private void handleSquareClick(int row, int col) {
        if (!gameActive || isAnimating) return;
        Piece clickedPiece = logic.getPieceAt(row, col);
        boolean isOwnPiece = clickedPiece != null && clickedPiece.isWhite() == (logic.getCurrentPlayer().equals("WHITE"));

        if (isOwnPiece) {
            selectPiece(row, col);
        } else if (selectedRow != -1 && isValidMoveInList(row, col)) {
            animatePiece(selectedRow, selectedCol, row, col);
        } else {
            resetSelection();
            updateBoardDisplay();
        }
    }

    private void selectPiece(int r, int c) {
        selectedRow = r; selectedCol = c;
        validMoves.clear();
        Piece p = logic.getPieceAt(r, c);
        for(int i=0; i<8; i++) for(int j=0; j<8; j++) {
            if(logic.checkRules(r, c, i, j, p) && logic.simulateMoveAndCheckSafety(r, c, i, j)) {
                validMoves.add(new Point(i, j));
            }
        }
        updateBoardDisplay();
    }

    // [수정 완료] 애니메이션 로직 복구
    private void animatePiece(int r1, int c1, int r2, int c2) {
        isAnimating = true;
        JButton startBtn = squares[r1][c1];
        JButton endBtn = squares[r2][c2];

        Point p1 = SwingUtilities.convertPoint(startBtn, 0, 0, getLayeredPane());
        Point p2 = SwingUtilities.convertPoint(endBtn, 0, 0, getLayeredPane());

        JLabel floatingPiece = new JLabel();
        if (startBtn.getIcon() != null) floatingPiece.setIcon(startBtn.getIcon());
        else floatingPiece.setText(startBtn.getText());

        floatingPiece.setSize(startBtn.getSize());
        floatingPiece.setLocation(p1);

        getLayeredPane().add(floatingPiece, JLayeredPane.DRAG_LAYER);
        startBtn.setIcon(null);
        startBtn.setText("");

        long startTime = System.currentTimeMillis();
        long duration = 200; // 0.2초 동안 이동

        Timer animTimer = new Timer(10, null);
        animTimer.addActionListener(e -> {
            long now = System.currentTimeMillis();
            float fraction = (float)(now - startTime) / duration;

            if (fraction >= 1.0f) {
                fraction = 1.0f;
                animTimer.stop();
                getLayeredPane().remove(floatingPiece);
                getLayeredPane().repaint();
                finalizeMove(r1, c1, r2, c2);
            } else {
                int curX = (int)(p1.x + (p2.x - p1.x) * fraction);
                int curY = (int)(p1.y + (p2.y - p1.y) * fraction);
                floatingPiece.setLocation(curX, curY);
            }
        });
        animTimer.start();
    }

    private void finalizeMove(int oldR, int oldC, int newR, int newC) {
        Piece p = logic.getPieceAt(oldR, oldC);
        Piece target = logic.getPieceAt(newR, newC);
        String moveLog = "";

        if (p instanceof King && Math.abs(newC - oldC) == 2) {
            logic.handleCastling(oldR, oldC, newR, newC);
            moveLog = " (Castling)";
            ResourceManager.playSound("castle.wav");
        } else if (target != null) {
            addCapturedPiece(logic.getCurrentPlayer(), target.getImageName());
            showCaptureAnimation(p.getImageName(), target.getImageName());
            ResourceManager.playSound("capture.wav");
        } else {
            ResourceManager.playSound("move.wav");
        }

        logic.executeMove(oldR, oldC, newR, newC);

        if (p instanceof Pawn && (newR == 0 || newR == 7)) {
            Piece newPiece = showPromotionDialog(logic.getCurrentPlayer());
            logic.promotePawn(newR, newC, newPiece);
            moveLog += " (Promoted)";
            ResourceManager.playSound("promote.wav");
        }

        String symbol = UNICODE_PIECES.get(p.getImageName());
        addLog(symbol + " " + getChessNotation(oldR, oldC) + " -> " + getChessNotation(newR, newC) + moveLog, logic.getCurrentPlayer());

        logic.switchTurn();
        resetSelection();
        checkGameOverState();
        isAnimating = false;
        updateBoardDisplay();
    }

    private void checkGameOverState() {
        if(logic.checkInsufficientMaterial()) {
            addLog("Draw (Insufficient Material)", logic.getCurrentPlayer());
            gameOver(true, "Insufficient Material");
            return;
        }
        if(logic.checkThreefoldRepetition()) {
            addLog("Draw (3-fold Repetition)", logic.getCurrentPlayer());
            gameOver(true, "3-fold Repetition");
            return;
        }

        boolean inCheck = logic.isKingInCheck(logic.getCurrentPlayer());
        boolean canMove = logic.hasLegalMoves(logic.getCurrentPlayer());

        if(inCheck) ResourceManager.playSound("check.wav");

        if(inCheck && !canMove) gameOver(false, "");
        else if(!inCheck && !canMove) {
            addLog("Draw (Stalemate)", logic.getCurrentPlayer());
            gameOver(true, "Stalemate");
        }
        else if(inCheck) {
            addLog("CHECK!", logic.getCurrentPlayer());
            showCheckDialog(logic.getCurrentPlayer());
        }
    }

    // ==================== 유틸리티 및 갱신 ====================

    private void updateBoardDisplay() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                JButton square = squares[r][c];
                Piece p = logic.getPieceAt(r, c);

                if ((r + c) % 2 == 0) square.setBackground(new Color(240, 230, 210));
                else square.setBackground(new Color(122, 111, 93));

                if (r == selectedRow && c == selectedCol) square.setBackground(new Color(100, 200, 100));
                for(Point pt : validMoves) {
                    if(pt.x == r && pt.y == c) {
                        if(logic.getPieceAt(r,c) != null) square.setBackground(new Color(200, 100, 100));
                        else square.setBackground(new Color(220, 220, 100));
                    }
                }

                if(p instanceof King) {
                    boolean isWhite = p.isWhite();
                    if((isWhite && logic.getCurrentPlayer().equals("WHITE") && logic.isKingInCheck("WHITE")) ||
                            (!isWhite && logic.getCurrentPlayer().equals("BLACK") && logic.isKingInCheck("BLACK"))) {
                        square.setBackground(new Color(255, 80, 80));
                    }
                }

                if (p != null) {
                    ImageIcon icon = ResourceManager.getImage(p.getImageName());
                    if (icon != null) {
                        square.setIcon(icon);
                        square.setText("");
                    } else {
                        square.setIcon(null);
                        square.setText(UNICODE_PIECES.get(p.getImageName()));
                        square.setFont(new Font("Arial Unicode MS", Font.PLAIN, SQUARE_SIZE/2));
                        square.setForeground(p.isWhite() ? Color.WHITE : Color.BLACK);
                    }
                } else {
                    square.setIcon(null);
                    square.setText("");
                }
            }
        }
    }

    private void addCapturedPiece(String player, String pieceName) {
        JPanel targetPanel = player.equals("BLACK") ? capturedBlackPanel : capturedWhitePanel;
        ImageIcon icon = ResourceManager.getImage(pieceName);
        JLabel pieceLabel = new JLabel();
        if (icon != null) {
            Image smallImg = icon.getImage().getScaledInstance(35, 35, Image.SCALE_SMOOTH);
            pieceLabel.setIcon(new ImageIcon(smallImg));
        } else {
            pieceLabel.setText(UNICODE_PIECES.get(pieceName));
        }
        targetPanel.add(pieceLabel);
        targetPanel.revalidate(); targetPanel.repaint();
    }

    private Piece showPromotionDialog(String player) {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};

        // 커스텀 버튼들이 들어갈 패널 생성
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 10));
        panel.setOpaque(false);

        // 사용자가 선택한 기물 타입 (기본값 Queen)
        final int[] selectedChoice = {0};

        JDialog dialog = new JDialog(this, "Promote to:", true);
        dialog.setUndecorated(true);
        dialog.setSize(500, 150);
        dialog.setLocationRelativeTo(this);

        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(player.equals("BLACK") ? new Color(40, 40, 40) : new Color(240, 240, 240));
        contentPane.setBorder(new LineBorder(new Color(100, 100, 100), 2));

        JLabel titleLabel = new JLabel("Choose Promotion Piece", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(player.equals("BLACK") ? Color.WHITE : Color.BLACK);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        contentPane.add(titleLabel, BorderLayout.NORTH);

        for (int i = 0; i < options.length; i++) {
            String type = options[i];
            String pieceName = (player.equals("WHITE") ? "White" : "Black") + type;

            JButton btn = new JButton();
            btn.setFocusPainted(false);
            btn.setContentAreaFilled(false);
            btn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            ImageIcon icon = ResourceManager.getImage(pieceName);
            if (icon != null) {
                Image scaled = icon.getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH);
                btn.setIcon(new ImageIcon(scaled));
            } else {
                btn.setText(type);
                btn.setForeground(player.equals("BLACK") ? Color.WHITE : Color.BLACK);
            }

            final int choice = i;
            btn.addActionListener(e -> {
                selectedChoice[0] = choice;
                dialog.dispose();
            });

            btn.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    btn.setBorder(new LineBorder(new Color(100, 150, 200), 3));
                }
                public void mouseExited(MouseEvent e) {
                    btn.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                }
            });
            panel.add(btn);
        }

        contentPane.add(panel, BorderLayout.CENTER);
        dialog.setContentPane(contentPane);
        dialog.setVisible(true);

        boolean w = player.equals("WHITE");
        switch(selectedChoice[0]) {
            case 1: return new Rook(w);
            case 2: return new Bishop(w);
            case 3: return new Knight(w);
            default: return new Queen(w);
        }
    }

    // ==================== 다이얼로그 ====================

    private void showVictoryDialog(String winner) {
        showEndDialog("VICTORY!", winner + " WINS!", "victory.wav", winner.equals("WHITE") ? "WhiteWin.png" : "BlackWin.png");
    }

    private void showEndDialog(String title, String msg, String sound, String imgName) {
        ResourceManager.playSound(sound);
        JDialog d = new JDialog(this, title, true);
        d.setUndecorated(true);
        d.setSize(600, 450);
        d.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image bg = ResourceManager.getDialogImage(imgName);
                if (bg != null && bg.getWidth(null) > 0) {
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(new Color(50, 50, 50));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(Color.LIGHT_GRAY);
                    g.drawRect(0, 0, getWidth()-1, getHeight()-1);
                }
            }
        };

        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 0, 10, 0);

        JLabel l1 = new JLabel(title, SwingConstants.CENTER);
        l1.setFont(new Font("Impact", Font.BOLD, 80));
        l1.setForeground(title.contains("DRAW") ? Color.LIGHT_GRAY : new Color(255, 215, 0));
        p.add(l1, c);

        if(!msg.isEmpty()) {
            JLabel l2 = new JLabel(msg, SwingConstants.CENTER);
            l2.setFont(new Font("Arial", Font.BOLD, 30));
            l2.setForeground(Color.WHITE);
            p.add(l2, c);
        }

        JButton b = new JButton("To Menu");
        b.setFont(new Font("Arial", Font.BOLD, 22));
        b.setBackground(new Color(50, 50, 50));
        b.setForeground(Color.WHITE);
        b.addActionListener(e -> { d.dispose(); dispose(); new MainMenu(); });

        c.insets = new Insets(30, 0, 0, 0);
        p.add(b, c);

        d.add(p);
        d.setVisible(true);
    }

    private void showCheckDialog(String player) {
        String fileName = player.equals("WHITE") ? "WhiteCheck.png" : "BlackCheck.png";
        JDialog d = new JDialog(this, "Check!", true);
        d.setUndecorated(true);
        d.setSize(400, 300);
        d.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image bg = ResourceManager.getDialogImage(fileName);
                if (bg != null && bg.getWidth(null) > 0) {
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(new Color(20, 20, 20));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(Color.RED);
                    g.drawRect(0, 0, getWidth()-1, getHeight()-1);
                }
            }
        };
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;

        JLabel l = new JLabel("CHECK!", SwingConstants.CENTER);
        l.setFont(new Font("Impact", Font.BOLD, 60));
        l.setForeground(Color.RED);
        p.add(l, c);

        JButton b = new JButton("OK");
        b.setFont(new Font("Arial", Font.BOLD, 18));
        b.setBackground(new Color(200, 50, 50));
        b.setForeground(Color.WHITE);
        b.addActionListener(e -> d.dispose());
        p.add(b, c);

        d.add(p);
        d.setVisible(true);
    }

    // ==================== 유틸리티 ====================

    private void resetSelection() { selectedRow = -1; selectedCol = -1; validMoves.clear(); }
    private void addLog(String msg, String player) { if(player.equals("WHITE")) logWhiteArea.append(msg+"\n"); else logBlackArea.append(msg+"\n"); }
    private void timeOutGameOver(String l) { gameOver(false, ""); JOptionPane.showMessageDialog(this, l + " TIME OUT!"); }
    private boolean isValidMoveInList(int r, int c) { for(Point p : validMoves) if(p.x==r && p.y==c) return true; return false; }
    private String getChessNotation(int r, int c) { return ""+(char)('a'+c)+(8-r); }
    private String formatTime(int s) { return String.format("%02d:%02d", s/60, s%60); }

    private void startGameThread() {
        if(isThreadRunning) return; isThreadRunning = true;
        gameThread = new Thread(() -> {
            while(isThreadRunning && gameActive) {
                try { Thread.sleep(1000); } catch(Exception e) {}
                SwingUtilities.invokeLater(() -> {
                    if(!gameActive) return;
                    if(logic.getCurrentPlayer().equals("WHITE")) { timerWhite--; if(timerWhite<=0) timeOutGameOver("WHITE"); else timerWhiteLabel.setText(formatTime(timerWhite)); }
                    else { timerBlack--; if(timerBlack<=0) timeOutGameOver("BLACK"); else timerBlackLabel.setText(formatTime(timerBlack)); }
                });
            }
        });
        gameThread.start();
    }

    private void saveGameAndExit() {
        try (BufferedWriter w = new BufferedWriter(new FileWriter("saved_game.txt"))) {
            // 1. 기본 정보
            w.write(logic.getCurrentPlayer() + "\n");
            w.write(timerWhite + "\n");
            w.write(timerBlack + "\n");

            // 2. 보드 상태
            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    Piece p = logic.getPieceAt(r, c);
                    w.write((p == null ? "null" : p.getImageName()) + ",");
                }
                w.write("\n");
            }

            // 3. 로그 텍스트
            String whiteLog = logWhiteArea.getText().replace("\n", "%%%");
            String blackLog = logBlackArea.getText().replace("\n", "%%%");

            w.write((whiteLog.isEmpty() ? "EMPTY" : whiteLog) + "\n");
            w.write((blackLog.isEmpty() ? "EMPTY" : blackLog) + "\n");

            // [수정] 알림창 코드 삭제됨 -> 바로 종료 및 메뉴 이동
            isThreadRunning = false;
            dispose();
            new MainMenu();

        } catch (Exception e) {
            // 에러 날 때만 알려줌
            JOptionPane.showMessageDialog(this, "저장 실패: " + e.getMessage());
        }
    }

    private void loadGame() {
        try (BufferedReader r = new BufferedReader(new FileReader("saved_game.txt"))) {
            // 1. 기본 정보 로드
            logic.setCurrentPlayer(r.readLine());
            timerWhite = Integer.parseInt(r.readLine());
            timerBlack = Integer.parseInt(r.readLine());

            // 2. 보드 상태 로드
            for (int row = 0; row < 8; row++) {
                String[] line = r.readLine().split(",");
                for (int col = 0; col < 8; col++) {
                    if (!line[col].equals("null")) {
                        boolean w = line[col].startsWith("White");
                        String t = line[col].substring(5);
                        logic.setPiece(row, col, logic.createPiece(w, t));
                    } else {
                        logic.setPiece(row, col, null); // 빈 칸은 확실히 지우기
                    }
                }
            }

            // 3. [추가된 부분] 로그 텍스트 복구
            String whiteLog = r.readLine();
            String blackLog = r.readLine();

            if (whiteLog != null && !whiteLog.equals("EMPTY"))
                logWhiteArea.setText(whiteLog.replace("%%%", "\n"));
            else
                logWhiteArea.setText("");

            if (blackLog != null && !blackLog.equals("EMPTY"))
                logBlackArea.setText(blackLog.replace("%%%", "\n"));
            else
                logBlackArea.setText("");

            // 4. [추가된 부분] 잡은 기물 패널 복구 (계산 로직 호출)
            refreshCapturedPanels();

            // UI 갱신
            updateBoardDisplay();
            timerWhiteLabel.setText(formatTime(timerWhite));
            timerBlackLabel.setText(formatTime(timerBlack));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "불러오기 실패 (새 게임 시작): " + e.getMessage());
            logic.initializeGame();
            updateBoardDisplay();
        }
    }
    private void refreshCapturedPanels() {
        // 1. 패널 초기화
        capturedBlackPanel.removeAll();
        capturedWhitePanel.removeAll();

        // 2. 전체 기물 리스트 정의 (원래 있어야 할 기물들)
        ArrayList<String> allWhitePieces = new ArrayList<>();
        ArrayList<String> allBlackPieces = new ArrayList<>();

        String[] types = {"Rook", "Knight", "Bishop", "Queen", "King", "Bishop", "Knight", "Rook"};
        for(String t : types) {
            allWhitePieces.add("White"+t);
            allBlackPieces.add("Black"+t);
        }
        for(int i=0; i<8; i++) {
            allWhitePieces.add("WhitePawn");
            allBlackPieces.add("BlackPawn");
        }

        // 3. 현재 보드에 살아있는 기물들을 리스트에서 제거 (소거법)
        for(int r=0; r<8; r++) {
            for(int c=0; c<8; c++) {
                Piece p = logic.getPieceAt(r, c);
                if(p != null) {
                    if(p.isWhite()) allWhitePieces.remove(p.getImageName());
                    else allBlackPieces.remove(p.getImageName());
                }
            }
        }

        // 4. [핵심 수정] 리스트에 남은 것(=잡힌 것)을 상대방 패널에 추가
        // 없어진 백 기물 -> 흑이 잡았으므로 BLACK 패널에 추가
        for(String piece : allWhitePieces) addCapturedPiece("BLACK", piece);

        // 없어진 흑 기물 -> 백이 잡았으므로 WHITE 패널에 추가
        for(String piece : allBlackPieces) addCapturedPiece("WHITE", piece);

        // 5. 화면 갱신
        capturedBlackPanel.revalidate(); capturedBlackPanel.repaint();
        capturedWhitePanel.revalidate(); capturedWhitePanel.repaint();
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(() -> new MainGame(false)); }
}