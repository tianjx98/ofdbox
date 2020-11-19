package com.ofdbox.convertor.test;

import com.ofdbox.convertor.img.Ofd2Img;
import com.ofdbox.core.model.OFD;
import com.ofdbox.core.OFDReader;
import com.ofdbox.core.model.page.Page;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @description:
 * @author: 张家尧
 * @create: 2020/10/28 09:45
 */
public class Test {
    public static void main(String[] args) throws IOException {
        OFDReader reader = new OFDReader();
        reader.getConfig().setValid(false);

        OFD ofd = reader.read(new File("/Users/devinlee/Downloads/ofd/7e3b58563a65bbd0a407e04b86ec1d6a.ofd"));
        Ofd2Img ofd2Img = new Ofd2Img();
        ofd2Img.getConfig().setDrawBoundary(false);

        int i = 1;
        for (Page page : ofd.getDocuments().get(0).getPages()) {
            BufferedImage image = ofd2Img.toImage(page, 30);
            ImageIO.write(image, "JPEG", new FileOutputStream(new File("/Users/devinlee/Downloads/ofd", i + ".jpg")));
            i++;
        }
    }
}