package club.dreamccc.ehtdownload;


import club.dreamccc.ehtdownload.eneity.ComicImageHtml;
import club.dreamccc.ehtdownload.eneity.ComicPageHtml;
import club.dreamccc.ehtdownload.exception.CanIgnoreException;
import club.dreamccc.ehtdownload.exception.CanRetryException;
import club.dreamccc.ehtdownload.exception.ExhException;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.text.StrFormatter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
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
public class ComiDetailDownloader {

    static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 10809)))
            .callTimeout(20, TimeUnit.SECONDS)
            .cookieJar(new ExhEnvironmentCookieManager())
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

    public static void main(String[] args) {

        try {
            final var argMap = args(args);

            var comicIndexUrl = argMap.get("u");
            var skipNumStr = argMap.getOrDefault("skip", "0");
            int skip = Integer.valueOf(skipNumStr);

            String indexHtml = null;

            indexHtml = getHtml(comicIndexUrl);

            int maxPageNum = getMaxPageNum(indexHtml);

            // 拼接后面几页的url
            IntStream.range(0, maxPageNum)
                    .boxed()
                    .map(pNum -> {
                        var urlBuilder = UrlBuilder.ofHttpWithoutEncode(comicIndexUrl);
                        var map = new HashMap<CharSequence, CharSequence>(urlBuilder.getQuery().getQueryMap());
                        map.put("p", pNum + "");
                        urlBuilder.setQuery(new UrlQuery(map));
                        return urlBuilder.build();
                    })
                    .map(url -> {
                        try {
                            return new ComicPageHtml(url, getHtml(url));
                        } catch (CanIgnoreException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    // 获取图片页列表
                    .flatMap(comicPageHtml -> comicPageHtml.getComicImageUrls().stream())
                    .skip(skip)
                    .map(url -> {
                        try {
                            return new ComicImageHtml(url, getHtml(url));
                        } catch (CanIgnoreException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    // 下载原图到本地
                    .forEach(comicImageHtml -> {
                        byte[] bytes = new byte[0];
                        final var sourceImageUrl = comicImageHtml.getSourceImageUrl();
                        final var showImageUrl = comicImageHtml.getShowImageUrl();
                        try {
                            bytes = downImages(sourceImageUrl);
                        } catch (ExhException | CanIgnoreException downloadSourceImageEx) {
                            log.warn("下载原图出错，开始下载展示图片\n[source:{}]==>[show:{}]", sourceImageUrl, showImageUrl);
                            try {
                                bytes = downImages(showImageUrl);
                            } catch (ExhException | CanIgnoreException downloadShowImageEx) {
                                log.error(downloadShowImageEx.getMessage());
                                return;
                            }
                        }

                        String comicTitle = comicImageHtml.getComicTitle()
                                .replace(":", " ")
                                .replace("?", " ")
                                .replace("*", " ")
                                .replace("/", " ")
                                .replace("|", " ")
                                .replace("<", " ")
                                .replace(">", " ")
                                .replace("\"", "")
                                .replace("\\", "");
                        File comicDir = FileUtil.mkdir("./" + comicTitle);
                        if (comicDir.exists()) {
                            String filePath = StrFormatter.format("./{}/{}", comicTitle, comicImageHtml.getImageName());
                            File file = new File(filePath);
                            log.info("下载文件到{}.", file.getAbsolutePath());
                            FileUtil.writeBytes(bytes, file);
                        }
                    });

        } catch (CanIgnoreException e) {
            log.error("", e);
        }

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

    private synchronized static String getHtml(String url) throws CanIgnoreException {
        return getHtml(url, 0);
    }

    private synchronized static String getHtml(String url, int retryCount) throws CanIgnoreException {
        Request getDocReq = new Request.Builder()
                .get()
                .url(url)
                .build();

        try {
            log.debug("call {}}", url);
            Response execute = okHttpClient.newCall(getDocReq).execute();
            if (execute.isSuccessful()) {
                log.debug("call {} is Successful! {}", url, execute.message());
                ResponseBody body = execute.body();
                if (body == null) {
                    throw new CanIgnoreException(execute.code() + ":获取ResponseBody为空: " + execute.message());
                }
                try {
                    final var bytes = body.bytes();
                    if (bytes.length <= 10) {
                        throw new ExhException("获取页面为空,请检查cookie是否设置正确");
                    }
                    return new String(bytes);
                } catch (IOException e) {
                    throw new CanRetryException(execute.code() + ":读取Body失败: " + execute.message());
                }
            } else {
                throw new CanRetryException(execute.code() + ": " + execute.message());
            }
        } catch (IOException | CanRetryException e) {
            if (retryCount <= 5) {
                log.info("请求失败，重试次数[{}]", ++retryCount);
                return getHtml(url, retryCount);
            } else {
                throw new CanIgnoreException("请求失败: " + e.getMessage() + "\nCause: " + e.getCause().getMessage());
            }
        }
    }

    static synchronized byte[] downImages(String imgUrl) throws CanIgnoreException {
        return downImages(imgUrl, 0);
    }

    static synchronized byte[] downImages(String imgUrl, int retryCount) throws CanIgnoreException {

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
                    throw new CanIgnoreException(execute.code() + ":获取ResponseBody为空: " + execute.message());
                }
                byte[] bodyBytes;
                try {
                    bodyBytes = body.bytes();
                } catch (IOException e) {
                    throw new CanRetryException(execute.code() + ":读取Body失败: " + execute.message(), e);
                }
                if (Objects.equals(body.contentType(), MediaType.parse("text/html; charset=UTF-8"))) {
                    String exMessage = new String(bodyBytes);
                    throw new ExhException(exMessage);
                }
                return bodyBytes;
            } else {
                throw new CanRetryException(execute.code() + ": " + execute.message());
            }
        } catch (IOException | CanRetryException e) {
            if (retryCount <= 5) {
                log.info("请求失败，重试次数[{}]", ++retryCount);
                return downImages(imgUrl, retryCount);
            } else {
                throw new CanIgnoreException("请求失败: " + e.getMessage() + "\nCause: " + e.getCause().getMessage());
            }
        }
    }

}
