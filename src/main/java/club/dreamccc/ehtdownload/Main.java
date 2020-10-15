package club.dreamccc.ehtdownload;


import club.dreamccc.ehtdownload.eneity.ComicInfoPage;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;

import java.io.*;
import java.net.HttpCookie;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Main {

    static final OkHttpClient okHttpClient = new OkHttpClient();

    static Map<String, String> getCookies() {

        HashMap<String, String> cookies = new HashMap<String, String>();
        cookies.put("igneous", "090464f47");
        cookies.put("sk", "c6ulx84auvfhys8vcqg950njumjk");
        cookies.put("u", "1950231-0-sw5xqkx9jii");
        cookies.put("ipb_pass_hash", "247bb1ae9340b06e1e59700b61cbb5ad");
        cookies.put("ipb_member_id", "1950231");
        return cookies;
    }

    static Document getDocument(String url) {
        if (null == url) {
            return null;
        }
        Document doc = null;
        try {
            log.debug("获取文档[{}]...", url);
            doc = Jsoup.connect(url)
                    .cookies(getCookies())
                    .get();
            log.info("获取文档成功[{}]", doc.title());
        } catch (IOException e) {
            log.error("获取文档异常", e);
        }
        return doc;
    }

    public static void main(String[] args) throws IOException {

//        Document document = getDocument("https://exhentai.org/g/1464160/94b5bd6708/");
//
//
//        ComicInfoPage currentPage = new ComicInfoPage(document);
//
//
//        List<ComicInfoPage> allInfoPage = new LinkedList<>();
//        allInfoPage.add(currentPage);
//        while (currentPage.hasNext()) {
//            String nextPageUrl = currentPage.getNextPageUrl();
//            Document docTemp = getDocument(nextPageUrl);
//            currentPage = new ComicInfoPage(docTemp);
//            allInfoPage.add(currentPage);
//        }
//        List<String> collect = allInfoPage.stream().flatMap(comicInfoPage -> comicInfoPage.getImgPageUrls().stream()).collect(Collectors.toList());
//
//        log.info("{}", collect);
//
//        String subTitle = currentPage.getSubTitle();
//        collect.forEach(imgPageUrl -> {
//            Document imgDoc = getDocument(imgPageUrl);
//
//            String[] fileNameTemps = imgDoc.select("#i2")
//                    .last()
//                    .text()
//                    .split("::")[0]
//                    .split(" ");
//            String fileName = fileNameTemps[fileNameTemps.length - 1];
//            String originalImageUrl = imgDoc
//                    .select("#i7")
//                    .select("a")
//                    .attr("href");
//
//
//            downImages(subTitle + "/" + fileName, originalImageUrl);
//        });
//
        downLoadAndSaveImg("[春待氷柱 (市町村)] 十二時の魔法使い [中国翻訳] [DL版]\\004.jpg", "https://exhentai.org/s/cf26aee063/1464160-4");
    }


    static void downLoadAndSaveImg(String filePath, String imgUrl) {

        File file = FileUtil.file(filePath);
        log.info("创建文件[{}]", file.getAbsolutePath());
        downImages(file,imgUrl);
    }

    static void downImages(File file, String imgUrl) {

        Map<String, String> cookies = getCookies();
        ArrayList<HttpCookie> cookieArrayList = new ArrayList<>();
        cookies.forEach((s, s2) -> cookieArrayList.add(new HttpCookie(s, s2)));
        HttpResponse execute = HttpRequest.get(imgUrl).timeout(5000).cookie(cookieArrayList).execute();
        execute.headers();
        if (execute.getStatus() == 302) {
            String redirectUrl = execute.header("Location");
            log.info("重定向到[{}]", redirectUrl);
            downImages(file, redirectUrl);
        }
        log.info("写入文件[{}]", file.getAbsolutePath());
        execute.writeBody(file, null);
    }

}
