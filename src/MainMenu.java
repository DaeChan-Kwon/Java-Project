import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class MainMenu extends JFrame {
    private int WINDOW_WIDTH = 800;
    private int WINDOW_HEIGHT = 600;
    private String imagePath = "images/";

    public MainMenu() {
        setTitle("Chess Game - Main Menu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel backgroundPanel = new JPanel(new BorderLayout()) {
            private Image bgImage;
            {
                try {
                    bgImage = new ImageIcon(imagePath + "menubackground.jpg").getImage();
                } catch (Exception e) { }
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(26, 26, 46));
                g.fillRect(0, 0, getWidth(), getHeight());
                if (bgImage != null) {
                    g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(50, 0, 0, 0));

        JLabel titleLabel = new JLabel("CHESS MASTER");
        titleLabel.setFont(new Font("Serif", Font.BOLD, 60));
        titleLabel.setForeground(new Color(240, 230, 210));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // [수정] 버튼 3개 (새게임, 불러오기, 종료)
        JButton newGameButton = createStyledButton("NEW GAME");
        JButton loadGameButton = createStyledButton("LOAD GAME");
        JButton exitButton = createStyledButton("EXIT");

        // [로직] 새 게임 시작 (false 전달)
        newGameButton.addActionListener(e -> startGame(false));

        // [로직] 저장된 게임 불러오기 (true 전달)
        loadGameButton.addActionListener(e -> {
            File saveFile = new File("saved_game.txt");
            if (saveFile.exists()) {
                startGame(true);
            } else {
                JOptionPane.showMessageDialog(this, "저장된 게임 파일이 없습니다.");
            }
        });

        exitButton.addActionListener(e -> System.exit(0));

        contentPanel.add(Box.createVerticalGlue());
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 60)));
        contentPanel.add(newGameButton);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        contentPanel.add(loadGameButton); // 추가된 버튼
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        contentPanel.add(exitButton);
        contentPanel.add(Box.createVerticalGlue());

        backgroundPanel.add(contentPanel, BorderLayout.CENTER);

        JLabel footerLabel = new JLabel("Developed by Java Swing Project  ");
        footerLabel.setForeground(Color.GRAY);
        footerLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        footerLabel.setBorder(new EmptyBorder(0, 0, 10, 10));
        backgroundPanel.add(footerLabel, BorderLayout.SOUTH);

        setContentPane(backgroundPanel);
        setVisible(true);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 18));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(60, 63, 65));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(200, 50));
        button.setMaximumSize(new Dimension(200, 50));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(100, 150, 200));
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(60, 63, 65));
            }
        });
        return button;
    }

    private void startGame(boolean loadFromSave) {
        this.dispose(); // 메뉴 창 닫기
        SwingUtilities.invokeLater(() -> {
            // MainGame에 '불러오기 여부'를 전달
            new MainGame(loadFromSave);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainMenu::new);
    }
}