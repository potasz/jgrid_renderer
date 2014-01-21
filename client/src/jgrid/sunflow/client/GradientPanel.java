package jgrid.sunflow.client;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;

import javax.swing.JPanel;

/**
 * A panel that is painted with a gradient.
 *
 * Source: Romain Guy - Fielthy Rich Clients
 */

public class GradientPanel extends JPanel {
    
    Color gradientStart = new Color(102, 153, 255);//220, 255, 149);
    Color gradientEnd = new Color(102, 153, 255);//183, 234, 98);        
//    Color gradientEnd = new Color(255, 255, 255);//183, 234, 98);            
    
    public GradientPanel() {        
    }
    
    public GradientPanel(Color gradientStart, Color gradientEnd) {
        this.gradientStart = gradientStart;
        this.gradientEnd = gradientEnd;
    }
    
    public void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        
        Graphics2D g2 = (Graphics2D) g;
        GradientPaint painter = new GradientPaint(0, 0, gradientStart,
                0, height, gradientEnd);
        Paint oldPainter = g2.getPaint();
        g2.setPaint(painter);
        g2.fill(g2.getClip());
        
//        gradientStart = new Color(102, 153, 255, 255);//220, 255, 149);
//        gradientEnd = new Color(102, 153, 255, 100);//183, 234, 98
        
        painter = new GradientPaint(0, 0, gradientEnd,
                0, height / 2, gradientStart);
        g2.setPaint(painter);
        g2.fill(g2.getClip());
        
        painter = new GradientPaint(0, height / 2, gradientStart,
                0, height, gradientEnd);
        g2.setPaint(painter);
        g2.fill(g2.getClip());
        
        g2.setPaint(oldPainter);
    }
}
