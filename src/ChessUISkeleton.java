import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ChessUISkeleton extends JFrame {

    public ChessUISkeleton() {
        setTitle("Chess Game Prototype - Unicode Ver.");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLayout(new BorderLayout(10, 10));

        // 1. 중앙: 체스판
        add(createBoardPanel(), BorderLayout.CENTER);

        // 2. 왼쪽: 로그 & 타이머
        add(createLeftPanel(), BorderLayout.WEST);

        // 3. 오른쪽: 잡은 말
        add(createRightPanel(), BorderLayout.EAST);

        setVisible(true);
    }

    // ★ 수정된 부분: 유니코드 기물 배치
    private JPanel createBoardPanel() {
        JPanel panel = new JPanel(new GridLayout(8, 8));
        panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        // 유니코드 체스 기물 정의
        // 흑: 룩, 나이트, 비숍, 퀸, 킹, 비숍, 나이트, 룩
        String[] blackBack = {"♜", "♞", "♝", "♛", "♚", "♝", "♞", "♜"};
        String[] whiteBack = {"♖", "♘", "♗", "♕", "♔", "♗", "♘", "♖"};
        String blackPawn = "♟";
        String whitePawn = "♙";

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                JButton btn = new JButton();

                // ★ 중요: 유니코드가 잘 보이려면 폰트를 아주 크게 키워야 합니다 (40px)
                btn.setFont(new Font("Serif", Font.PLAIN, 40));
                btn.setFocusPainted(false);

                // 체스판 색상 패턴 (흰색 / 회색)
                if ((row + col) % 2 == 0) {
                    btn.setBackground(Color.WHITE);
                } else {
                    btn.setBackground(new Color(200, 200, 200)); // 연한 회색
                }

                // 기물 배치 로직
                if (row == 0) {
                    btn.setText(blackBack[col]); // 흑 기물 (뒷줄)
                } else if (row == 1) {
                    btn.setText(blackPawn);      // 흑 폰
                } else if (row == 6) {
                    btn.setText(whitePawn);      // 백 폰
                } else if (row == 7) {
                    btn.setText(whiteBack[col]); // 백 기물 (뒷줄)
                }

                panel.add(btn);
            }
        }
        return panel;
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 10));
        panel.setPreferredSize(new Dimension(200, 0));

        panel.add(createPlayerInfoPanel("00 : 00", "log : a2 -> a4"));
        panel.add(createPlayerInfoPanel("00 : 00", "log : a7 -> a5"));

        return panel;
    }

    private JPanel createPlayerInfoPanel(String timeText, String logText) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JPanel top = new JPanel(new BorderLayout());
        JLabel timeLbl = new JLabel(timeText, SwingConstants.CENTER);
        timeLbl.setFont(new Font("Monospaced", Font.BOLD, 20));
        timeLbl.setBorder(new EmptyBorder(10, 10, 10, 10));
        JButton resignBtn = new JButton("Resign");

        top.add(timeLbl, BorderLayout.CENTER);
        top.add(resignBtn, BorderLayout.EAST);

        JTextArea logArea = new JTextArea(logText);
        logArea.setEditable(false);
        logArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        logArea.setBackground(new Color(240, 240, 240));

        p.add(top, BorderLayout.NORTH);
        p.add(logArea, BorderLayout.CENTER);
        return p;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 10));
        panel.setPreferredSize(new Dimension(150, 0));

        panel.add(createCapturedPanel("Black Captured"));
        panel.add(createCapturedPanel("White Captured"));

        return panel;
    }

    private JPanel createCapturedPanel(String label) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JLabel title = new JLabel(label, SwingConstants.CENTER);
        title.setBorder(new EmptyBorder(5, 5, 5, 5));
        p.add(title, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setBackground(Color.WHITE);
        // 예시로 잡힌 말 하나 넣어두기
        JLabel dummy = new JLabel("♟");
        dummy.setFont(new Font("Serif", Font.PLAIN, 20));
        content.add(dummy);

        p.add(content, BorderLayout.CENTER);
        return p;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}
        new ChessUISkeleton();
    }
}