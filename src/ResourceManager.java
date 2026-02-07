import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.*;

public class ResourceManager {
    // 경로는 MainGame에서도 쓸 수 있게 public으로 둡니다.
    public static final String IMAGE_PATH = "images/";
    public static final String SOUND_PATH = "sounds/";

    private static Map<String, ImageIcon> imageCache = new HashMap<>();

    public static void preloadImages(int squareSize) {
        String[] pieces = {
                "WhiteKing", "WhiteQueen", "WhiteRook", "WhiteBishop", "WhiteKnight", "WhitePawn",
                "BlackKing", "BlackQueen", "BlackRook", "BlackBishop", "BlackKnight", "BlackPawn"
        };
        for (String piece : pieces) {
            try {
                ImageIcon icon = new ImageIcon(IMAGE_PATH + piece + ".png");
                if (icon.getIconWidth() > 0) {
                    Image img = icon.getImage().getScaledInstance(squareSize - 10, squareSize - 10, Image.SCALE_SMOOTH);
                    imageCache.put(piece, new ImageIcon(img));
                }
            } catch (Exception e) {}
        }
    }

    public static ImageIcon getImage(String pieceName) {
        return imageCache.get(pieceName);
    }

    // 애니메이션용 이미지 (크기 조절 안 함)
    public static ImageIcon getAnimationImage(String fileName) {
        return new ImageIcon(IMAGE_PATH + "animation/" + fileName);
    }

    // [수정] 다이얼로그 배경용 Image 객체 반환 메서드 추가
    public static Image getDialogImage(String fileName) {
        return new ImageIcon(IMAGE_PATH + fileName).getImage();
    }

    public static void playSound(String fileName) {
        try {
            File soundFile = new File(SOUND_PATH + fileName);
            if (soundFile.exists()) {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInputStream);
                clip.start();
            }
        } catch (Exception e) {}
    }
}