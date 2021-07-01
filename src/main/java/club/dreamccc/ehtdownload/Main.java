package club.dreamccc.ehtdownload;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Matcher;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import kotlin.text.Regex;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.HttpCookie;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
public class Main {

    static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 10809)))
            .callTimeout(10, TimeUnit.SECONDS)
            .cookieJar(
                    new CookieJar() {

                        private final Map<String, Cookie> cookies = new HashMap<>();

                        {
                            var cookieBuilder = new Cookie.Builder()
                                    .domain("exhentai.org")
                                    .path("/");
                            cookies.put("exhentai.org#igneous", cookieBuilder.name("igneous").value("090464f47").build());
                            cookies.put("exhentai.org#ipb_member_id", cookieBuilder.name("ipb_member_id").value("1950231").build());
                            cookies.put("exhentai.org#ipb_pass_hash", cookieBuilder.name("ipb_pass_hash").value("247bb1ae9340b06e1e59700b61cbb5ad").build());
                            cookies.put("exhentai.org#sk", cookieBuilder.name("sk").value("c6ulx84auvfhys8vcqg950njumjk").build());
                            cookies.put("exhentai.org#u", cookieBuilder.name("u").value("1950231-0-tdhr5789obo").build());
                        }

                        @Override
                        public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {

                            for (Cookie cookie : list) {
                                cookies.put(cookie.domain() + "#" + cookie.name(), cookie);
                            }
                        }

                        @NotNull
                        @Override
                        public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {

                            return List.copyOf(cookies.values());
                        }
                    }
            )
            .followRedirects(true)
            .followSslRedirects(true)
            .build();

//    static Document getDocument(String url) {
//        if (null == url) {
//            return null;
//        }
//        Document doc = null;
//        try {
//            new Request.Builder().build()
//            okHttpClient.newCall(new Request.Builder().build())
//            log.debug("获取文档[{}]...", url);
//            Jsoup.parse()
//            doc = Jsoup.connect(url)
//                    .cookies(getCookies())
//                    .get();
//            log.info("获取文档成功[{}]", doc.title());
//        } catch (IOException e) {
//            log.error("获取文档异常", e);
//        }
//        return doc;
//    }

    public static void main(String[] args) throws IOException {

        String url = "https://exhentai.org/g/1937930/e531678284/";
        String index_0 = getHtml(url);

        String pageText = Jsoup.parse(index_0)
                .select(".gtb .gpc ").text();
        // calc maxPageNum
        int[] pageInfo = Stream.of(pageText.split("\\D"))
                .filter(s -> !s.isBlank())
                .mapToInt(Integer::valueOf)
                .toArray();
        int pageSize = pageInfo[1] - pageInfo[0] + 1;
        int total = pageInfo[2];
        int maxPageNum = total / pageSize + (total % pageSize == 0 ? 0 : 1);
        System.out.println(maxPageNum);
        log.debug("maxPageNum: {}", maxPageNum);






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
//        downLoadAndSaveImg("[春待氷柱 (市町村)] 十二時の魔法使い [中国翻訳] [DL版]\\004.jpg", "https://exhentai.org/s/cf26aee063/1464160-4");
    }

    private static String getHtml(String url) {
        Request getDocReq = new Request.Builder()
                .get()
                .url(url)
                .build();

        try {
            Response execute = okHttpClient.newCall(getDocReq).execute();
            if (execute.isSuccessful()) {
                log.debug("call {} is Successful! {}", url, execute.message());
                ResponseBody body = execute.body();

                if (body == null) {
                    return null;
                }
                try {
                    return new String(body.bytes());
                } catch (IOException e) {
                    throw new RuntimeException(execute.code() + ":读取Body失败: " + execute.message());
                }
            } else {
                throw new RuntimeException(execute.code() + ": " + execute.message());
            }
        } catch (IOException e) {
            throw new RuntimeException("请求失败: " + e.getMessage() + "\nCause: " + e.getCause().getMessage());
        }
    }


//    static void downLoadAndSaveImg(String filePath, String imgUrl) {
//
//        File file = FileUtil.file(filePath);
//        log.info("创建文件[{}]", file.getAbsolutePath());
//        downImages(file, imgUrl);
//    }

//    static void downImages(File file, String imgUrl) {
//
//        Map<String, String> cookies = getCookies();
//        ArrayList<HttpCookie> cookieArrayList = new ArrayList<>();
//        OkHttpClient.Builder
//        cookies.forEach((s, s2) -> cookieArrayList.add(new HttpCookie(s, s2)));
//        HttpResponse execute = HttpRequest.get(imgUrl).timeout(5000).cookie(cookieArrayList).execute();
//        execute.headers();
//        if (execute.getStatus() == 302) {
//            String redirectUrl = execute.header("Location");
//            log.info("重定向到[{}]", redirectUrl);
//            downImages(file, redirectUrl);
//        }
//        log.info("写入文件[{}]", file.getAbsolutePath());
//        execute.writeBody(file, null);
//    }

}
