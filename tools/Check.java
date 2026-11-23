import cn.goldcc.review.Matcher;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.util.*;

public class Check {
    public static void main(String[] args) throws Exception {
        Path assets = Paths.get("app/src/main/assets");
        ArrayList<int[]> refs = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        for (String line : Files.readAllLines(assets.resolve("units.tsv"))) {
            String[] unit = line.split("\t");
            refs.add(ImageIO.read(Paths.get("app/build/generated/portraitAssets",unit[0]+".png").toFile()).getRGB(0,0,64,64,null,0,64));
            names.add(unit[1]);
        }
        for (String path : args) {
            BufferedImage input = ImageIO.read(Paths.get(path).toFile());
            int w = Math.round(input.getWidth()*600f/input.getHeight());
            BufferedImage image = new BufferedImage(w,600,BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(input,0,0,w,600,null); g.dispose();
            int[] px = image.getRGB(0,0,w,600,null,0,w);
            System.out.println(path);
            for(int row=0;row<8;row++){
                for(Matcher.Hit h:Matcher.recognize(px,w,row,refs,names))
                    System.out.print((h.slot+1)+"="+names.get(h.unit)+"("+h.error+") ");
                System.out.println();
            }
        }
    }
}
