
import java.awt.*;

public class Theme {
    // Background layers
    public static final Color BG_DARKEST   = new Color(0x0F, 0x10, 0x14);
    public static final Color BG_SIDEBAR   = new Color(0x16, 0x18, 0x1F);
    public static final Color BG_MAIN      = new Color(0x1C, 0x1E, 0x28);
    public static final Color BG_INPUT     = new Color(0x23, 0x26, 0x33);
    public static final Color BG_HOVER     = new Color(0x2A, 0x2D, 0x3E);
    public static final Color BG_SELECTED  = new Color(0x2F, 0x33, 0x4A);
    public static final Color BG_CARD      = new Color(0x1F, 0x22, 0x2E);

    // Accent
    public static final Color ACCENT       = new Color(0x6C, 0x63, 0xFF);
    public static final Color ACCENT_LIGHT = new Color(0x8B, 0x85, 0xFF);
    public static final Color ACCENT_DIM   = new Color(0x6C, 0x63, 0xFF, 60);
    public static final Color ACCENT_GLOW  = new Color(0x6C, 0x63, 0xFF, 30);

    // Text
    public static final Color TEXT_PRIMARY   = new Color(0xE8, 0xE9, 0xF0);
    public static final Color TEXT_SECONDARY = new Color(0x8B, 0x8D, 0x9E);
    public static final Color TEXT_MUTED     = new Color(0x55, 0x57, 0x6B);
    public static final Color TEXT_ACCENT    = new Color(0x9D, 0x98, 0xFF);

    // Borders
    public static final Color BORDER        = new Color(0x2E, 0x31, 0x42);
    public static final Color BORDER_LIGHT  = new Color(0x3E, 0x42, 0x58);

    // Status
    public static final Color ONLINE        = new Color(0x3D, 0xD6, 0x8C);
    public static final Color AWAY          = new Color(0xF5, 0xA5, 0x23);
    public static final Color BUBBLE_SENT   = new Color(0x5B, 0x54, 0xD6);
    public static final Color BUBBLE_RECV   = new Color(0x24, 0x27, 0x36);

    // Fonts
    public static Font font(int style, int size) {
        return new Font("Segoe UI", style, size);
    }
    public static Font mono(int size) {
        return new Font("Consolas", Font.PLAIN, size);
    }
}
