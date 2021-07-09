package club.dreamccc.ehtdownload;


import club.dreamccc.ehtdownload.eneity.ComicImageHtml;
import club.dreamccc.ehtdownload.eneity.ComicPageHtml;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.text.StrFormatter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

    private static Map<String, String> args(String[] args) {

        return Arrays.stream(args)
                .map(arg -> arg.split("="))
                .filter(new Predicate<String[]>() {
                    private Set<String> set = new HashSet<>();

                    @Override
                    public boolean test(String[] strings) {
                        return set.add(strings[0]);
                    }
                })
                .collect(Collectors.toMap(argz -> argz[0], argz -> argz[1]));

    }

    public static void main(String[] args) throws IOException {


        final var argMap = args(args);

        var comicIndexUrl = argMap.get("u");
        var indexHtml = getHtml(comicIndexUrl);
        int maxPageNum = getMaxPageNum(indexHtml);

        IntStream.range(0, maxPageNum)
                .boxed()
                // 拼接后面几页的url
                .map(pNum -> {
                    var urlBuilder = UrlBuilder.ofHttpWithoutEncode(comicIndexUrl);
                    var map = new HashMap<CharSequence, CharSequence>(urlBuilder.getQuery().getQueryMap());
                    map.put("p", pNum - 1 + "");
                    urlBuilder.setQuery(new UrlQuery(map));
                    return urlBuilder.build();
                })
                .map(url -> new ComicPageHtml(url, getHtml(url)))
                // 获取图片页列表
                .flatMap(comicPageHtml -> comicPageHtml.getComicImageUrls().stream())
                .map(url -> new ComicImageHtml(url, getHtml(url)))
                // 下载原图到本地
                .forEach(comicImageHtml -> {
                    byte[] bytes = downImages(comicImageHtml.getSourceImageUrl());
                    File comicDir = FileUtil.mkdir("./" + comicImageHtml.getComicTitle());
                    if (comicDir.exists()) {
                        FileUtil.writeBytes(bytes, StrFormatter.format("./{}/{}", comicImageHtml.getComicTitle(), comicImageHtml.getImageName()));
                    }
                });


    }

    private static int getMaxPageNum(String html) {
        String pageText = Jsoup.parse(html)
                .select(".gtb .gpc ").text();
        // calc maxPageNum
        int[] pageInfo = Stream.of(pageText.split("\\D"))
                .filter(s -> !s.isBlank())
                .mapToInt(Integer::valueOf)
                .toArray();

        final var imageIndexFirst = pageInfo[0];
        final var imageIndexLast = pageInfo[1];
        final var imageTotal = pageInfo[2];

        final var pageSize = imageIndexLast - imageIndexFirst + 1;
        final var pageNumTotal = imageTotal / pageSize + (imageTotal % pageSize == 0 ? 0 : 1);
        final var pageNumCurrent = imageIndexFirst / pageSize + (imageIndexFirst % pageSize == 0 ? 0 : 1);

        return pageNumTotal;
    }

    private synchronized static String getHtml(String url) {
        return getHtml(url, 0);
    }

    private synchronized static String getHtml(String url, int retryCount) {
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
            if (retryCount <= 5) {
                log.info("请求失败，重试次数[{}]", ++retryCount);
                return getHtml(url, retryCount);
            } else {
                throw new RuntimeException("请求失败: " + e.getMessage() + "\nCause: " + e.getCause().getMessage());
            }
        }
    }

    static synchronized byte[] downImages(String imgUrl) {
        return downImages(imgUrl, 0);
    }

    static synchronized byte[] downImages(String imgUrl, int retryCount) {

        Request getImageReq = new Request.Builder()
                .get()
                .url(imgUrl)
                .build();
        try {
            Response execute = okHttpClient.newCall(getImageReq).execute();
            if (execute.isSuccessful()) {
                log.debug("call {} is Successful! {}", imgUrl, execute.message());
                ResponseBody body = execute.body();

                if (body == null) {
                    throw new RuntimeException(execute.code() + ":Body为空,下载失败: " + execute.message());
                }
                try {
                    return body.bytes();
                } catch (IOException e) {
                    throw new RuntimeException(execute.code() + ":读取Body失败: " + execute.message());
                }
            } else {
                throw new RuntimeException(execute.code() + ": " + execute.message());
            }
        } catch (IOException e) {
            if (retryCount <= 5) {
                log.info("请求失败，重试次数[{}]", ++retryCount);
                return downImages(imgUrl, retryCount);
            } else {
                throw new RuntimeException("请求失败: " + e.getMessage() + "\nCause: " + e.getCause().getMessage());
            }
        }
    }

}
