/**
 * Splash screen displayed at application launch.
 * 
 * Code borrowed from: Tutorialspoint, "How can we implement a splash screen 
 * using JWindow in Java?" 
 * https://www.tutorialspoint.com/how-can-we-implement-a-splash-screen-using-jwindow-in-java
 * 
 * Modifications: sleep duration reduced to 4 s;
 * on dispose(), launches SpectralGraphApp instead of exiting.
 * 
 * Image designed in PowerPoint using a licensed image (non-commercial use only) 
 */

import javax.swing.*;
import java.awt.*;

public class CreateSplashScreen extends JWindow {

    Image splashScreen;
    ImageIcon imageIcon;

    public static void main(String[] args) {
        CreateSplashScreen splash = new CreateSplashScreen();
        try {
            Thread.sleep(4000);
            splash.dispose();
            new SpectralGraphApp();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public CreateSplashScreen() {

        splashScreen = Toolkit.getDefaultToolkit().getImage("logo.png");
        imageIcon = new ImageIcon(splashScreen);
        setSize(imageIcon.getIconWidth(), imageIcon.getIconHeight());
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - getSize().width) / 2;
        int y = (screenSize.height - getSize().height) / 2;
        setLocation(x, y);
        setVisible(true);
    }

    public void paint(Graphics g) {
        super.paint(g);
        g.drawImage(splashScreen, 0, 0, this);
    }
}
