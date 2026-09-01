
package Core.Graphics;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class CustomGraphics{

    public final static RenderingHints qualityHints;
    
    static{
        qualityHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        qualityHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }
    
    public static Graphics2D getGraphics2D(Graphics g){
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHints(qualityHints);
        return g2;
    }
    
    public static BufferedImage OverlayColor(Graphics2D g, Color overlay, Image image){
        BufferedImage result = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D gbi = result.createGraphics();
        
        gbi.drawImage(image, 0, 0, null);
        gbi.setColor(overlay);
        gbi.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1f));
        gbi.fillRect(0, 0, image.getWidth(null), image.getHeight(null));
        gbi.dispose();
        
        return result;
    }

}
