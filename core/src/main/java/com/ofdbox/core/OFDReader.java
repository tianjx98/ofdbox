package com.ofdbox.core;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import com.ofdbox.core.utils.BeanValidUtils;
import com.ofdbox.core.utils.OfdXmlUtils;
import com.ofdbox.core.xmlobj.base.document.CT_TemplatePage;
import com.ofdbox.core.xmlobj.base.document.XDocument;
import com.ofdbox.core.xmlobj.base.ofd.NDocBody;
import com.ofdbox.core.xmlobj.base.ofd.XOFD;
import com.ofdbox.core.xmlobj.base.page.XPage;
import com.ofdbox.core.xmlobj.base.pages.NPage;
import com.ofdbox.core.xmlobj.base.res.XRes;
import com.ofdbox.core.xmlobj.st.ST_Loc;

import lombok.Data;
import org.ujmp.core.benchmark.LUBenchmarkTask;

//@Slf4j
public class OFDReader {
    private ParserConfig config = new ParserConfig();

    /**
     * 读取ofd输入流为一个ofd对象
     *
     * @param in ofd文件输入流
     * @return ofd对象
     * @throws IOException
     */
    public OFD read(InputStream in) throws IOException {
        FileManager fileManager = new MemoryFileManager();
        ZipInputStream zipInputStream = new ZipInputStream(in);
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[1024];
                    int len = 0;
                    while ((len = zipInputStream.read(buffer)) != -1) {
                        outStream.write(buffer, 0, len);
                    }
                    fileManager.write("/" + entry.getName(), new ByteArrayInputStream(outStream.toByteArray()));
                }
            }
            zipInputStream.closeEntry();
        }
        return buildOfd(fileManager);
    }

    public OFD read(File file) throws IOException {
        FileManager fileManager = new MemoryFileManager();
        ZipFile zipFile = new ZipFile(file);
        Enumeration<?> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            System.out.println(entry.getName());
            if (!entry.isDirectory()) {
                fileManager.write("/" + entry.getName(), zipFile.getInputStream(entry));
            }
        }
        zipFile.close();
        return buildOfd(fileManager);
    }

    private OFD buildOfd(FileManager fileManager) {
        try {
            OFD ofd = new OFD();
            ofd.setFileManager(fileManager);
            /*
             * OFD.xml
             * */
            XOFD xofd = OfdXmlUtils.toObject(fileManager.read("/OFD.xml"), XOFD.class);
            valid(xofd);
            ofd.setXofd(xofd);

            ofd.setDocuments(new ArrayList<>());

            for (NDocBody nDocBody : xofd.getDocBodys()) {
                ST_Loc docRoot = nDocBody.getDocRoot();

                /*
                 * Doc_0/Document.xml
                 * */
                XDocument xDocument = OfdXmlUtils.toObject(fileManager.read(docRoot.getFullLoc()), XDocument.class);
                valid(xDocument);

                Document document = new Document();
                document.setNDocBody(nDocBody);
                document.setXDocument(xDocument);

                ofd.addDocument(document);

                /*
                 * 资源
                 * */
                if (xDocument.getCommonData().getDocumentRes() != null) {
                    document.setRes(new ArrayList<>());
                    for (ST_Loc resLoc : xDocument.getCommonData().getDocumentRes()) {
                        resLoc.setParent(docRoot);
                        XRes xRes = OfdXmlUtils.toObject(fileManager.read(resLoc.getFullLoc()), XRes.class);
                        valid(xRes);
                        xRes.getBaseLoc().setParent(resLoc);

                        document.getRes().add(xRes);
                        if (!xRes.getBaseLoc().getLoc().endsWith("/")) {
                            xRes.getBaseLoc().setLoc(xRes.getBaseLoc().getLoc() + "/");
                        }
                    }
                }

                if (xDocument.getCommonData().getPublicRes() != null) {
                    document.setPublicResList(new ArrayList<>());
                    for (ST_Loc resLoc : xDocument.getCommonData().getPublicRes()) {
                        resLoc.setParent(docRoot);
                        XRes xRes = OfdXmlUtils.toObject(fileManager.read(resLoc.getFullLoc()), XRes.class);
                        valid(xRes);
                        xRes.getBaseLoc().setParent(resLoc);

                        document.getPublicResList().add(xRes);
                        if (!xRes.getBaseLoc().getLoc().endsWith("/")) {
                            xRes.getBaseLoc().setLoc(xRes.getBaseLoc().getLoc() + "/");
                        }
                    }
                }


                /*
                 * 模板
                 * */
                if (xDocument.getCommonData().getTemplatePages() != null) {
                    for (CT_TemplatePage ct_templatePage : xDocument.getCommonData().getTemplatePages()) {
                        ST_Loc templateLoc = ct_templatePage.getBaseLoc();
                        templateLoc.setParent(docRoot);
                        XPage xPage = OfdXmlUtils.toObject(fileManager.read(templateLoc.getFullLoc()), XPage.class);
                        Template template = new Template();
                        template.setXPage(xPage);
                        template.setCt_templatePage(ct_templatePage);
                        document.getTemplates().add(template);

                        template.setDocument(document);
                    }
                }


                document.setPages(new ArrayList<>());
                for (NPage nPage : xDocument.getPages().getList()) {

                    Page page = new Page();
                    page.setDocument(document);

                    ST_Loc pageBaseLoc = nPage.getBaseLoc();
                    pageBaseLoc.setParent(docRoot);
                    XPage xPage = OfdXmlUtils.toObject(fileManager.read(pageBaseLoc.getFullLoc()), XPage.class, this.config.ignoreNamespace);

                    page.setXPage(xPage);
                    document.getPages().add(page);
                }
            }
            return ofd;

        } finally {

        }
    }

    public ParserConfig getConfig() {
        return config;
    }

    private void valid(Object object) {
        if (this.config.valid) {
            BeanValidUtils.valid(object);
        }
    }

    @Data
    public static class ParserConfig {
        private boolean valid = true;
        private boolean ignoreNamespace = false;
    }
}
