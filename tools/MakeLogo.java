import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Renders the logo (same drawing as icons/logo.svg). Usage: java tools/MakeLogo.java <size> <file.png> */
public class MakeLogo {
	public static void main(String[] args) throws Exception {
		int size = Integer.parseInt(args[0]);
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.scale(size / 64.0, size / 64.0);
		// A dark tile, so that the windows stand out on the light pages of the Marketplace and of GitHub too.
		g.setPaint(new Color(0x1E1E1E));
		g.fill(new RoundRectangle2D.Double(0, 0, 64, 64, 24, 24));
		g.translate(5, 5);
		g.scale(0.84375, 0.84375);
		// From the back to the front: the colors of the screenshots of macOS, Linux and Windows.
		window(g, 4, 4, 0x7B1FA2);
		window(g, 12, 14, 0x2E7D32);
		window(g, 20, 25, 0x1F6FB5);
		g.setPaint(Color.WHITE);
		g.fill(new Ellipse2D.Double(49 - 1.6, 29 - 1.6, 3.2, 3.2));
		g.fill(new Ellipse2D.Double(54 - 1.6, 29 - 1.6, 3.2, 3.2));
		g.setPaint(new Color(0xB0BEC5));
		g.fill(new RoundRectangle2D.Double(25, 37.5, 22, 2.5, 2.5, 2.5));
		g.fill(new RoundRectangle2D.Double(25, 42.5, 28, 2.5, 2.5, 2.5));
		g.fill(new RoundRectangle2D.Double(25, 47.5, 16, 2.5, 2.5, 2.5));
		g.dispose();
		ImageIO.write(image, "png", new File(args[1]));
	}

	/** A window of 40 x 30 whose frame and title bar have this color. */
	private static void window(Graphics2D g, double x, double y, int color) {
		g.setPaint(new Color(color));
		g.fill(new RoundRectangle2D.Double(x, y, 40, 30, 8, 8));
		g.setPaint(Color.WHITE);
		g.fill(new RoundRectangle2D.Double(x + 1.5, y + 8, 37, 20.5, 5, 5));
	}
}
