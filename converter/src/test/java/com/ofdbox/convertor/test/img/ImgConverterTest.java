package com.ofdbox.convertor.test.img;

import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.ofdbox.convertor.img.Ofd2Img;
import com.ofdbox.convertor.test.utils.PathUtils;
import com.ofdbox.convertor.utils.seal.SealUtil;
import com.ofdbox.core.model.OFD;
import com.ofdbox.core.OFDReader;

public class ImgConverterTest {
    protected static final String basePath = PathUtils.getClassPath(ImgConverterTest.class);

    @Test
    public void toImg() throws IOException {

        OFDReader reader = new OFDReader();
        reader.getConfig().setValid(false);
        OFD ofd = reader.read(new File(basePath + "发票.ofd"));
        OFD ofd1 = reader.read(new File(basePath + "旋转测试.ofd"));

        Ofd2Img ofd2Img = new Ofd2Img();
        ofd2Img.getConfig().setDrawBoundary(false);

        BufferedImage image = ofd2Img.toImage(ofd.getDocuments().get(0).getPages().get(0), 20);
        BufferedImage image1 = ofd2Img.toImage(ofd1.getDocuments().get(0).getPages().get(0), 20);

        ImageIO.write(image, "JPEG", new FileOutputStream(new File(basePath, "发票.jpg")));
        ImageIO.write(image1, "JPEG", new FileOutputStream(new File(basePath, "旋转测试.jpg")));

    }

    @Test
    public void toImgWithSeal() throws Exception {
        final int dpi = 20;
        // 生成印章
        String mainText = "全国统一发票监制章";
        String viceText = "宁波市税务局";
        String centerText = "国家税务总局";
        BufferedImage seal = SealUtil.buildInvoiceSeal(dpi, mainText, viceText, centerText);

        OFDReader reader = new OFDReader();
        reader.getConfig().setValid(false);
        OFD ofd;
        try (InputStream inputStream = new FileInputStream(new File(basePath + "invoice.ofd"))) {
            ofd = reader.read(inputStream);
        }
        //ofd = reader.read(new File(basePath + "invoice.ofd"));

        Ofd2Img ofd2Img = new Ofd2Img();
        ofd2Img.getConfig().setDrawBoundary(false);

        BufferedImage image = ofd2Img.toImage(ofd.getDocuments().get(0).getPages().get(0), 20);
        SealUtil.drawInto(seal, image);
        ImageIO.write(image, "JPEG", new FileOutputStream(new File("0000000005.jpg")));
    }
}
