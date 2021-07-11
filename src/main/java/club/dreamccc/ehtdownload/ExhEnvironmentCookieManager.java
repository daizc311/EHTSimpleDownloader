package club.dreamccc.ehtdownload;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class ExhEnvironmentCookieManager implements CookieJar {

    private final Map<String, Cookie> cookies = new HashMap<>();

    {
        Map<String, String> env = System.getenv();
        String cookie = env.get("COOKIE");
        String[] cookieStrs = cookie.split(";");
        if (cookie.length() >= 1) {
            var cookieBuilder = new Cookie.Builder()
                    .domain("exhentai.org")
                    .path("/");
            Stream.of(cookieStrs)
                    .map(cookieStr -> cookieStr.trim().split("="))
                    .filter(cookieEntry -> cookieEntry.length == 2)
                    .forEach(cookieEntry -> {
                        String k = cookieEntry[0];
                        String v = cookieEntry[1];
                        this.cookies.put(String.format("exhentai.org#%s", k), cookieBuilder.name(k).value(v).build());
                    });
        }

        var sj = new StringJoiner("\n");
        long maxCookieKeyLength = cookies.values().stream().filter(Objects::nonNull).map(Cookie::name).mapToLong(String::length).max().orElse(0L);
        sj.add("\n=========== COOKIE INIT ===========");
        for (String cookiesKey : cookies.keySet()) {
            final var currentCookie = cookies.get(cookiesKey);
            var name = currentCookie.name();
            while (name.length()<maxCookieKeyLength){
                name= name.concat(" ");
            }
            sj.add(String.format("%s \t %s", name,currentCookie.value()));
        }
        sj.add("========= COOKIE INIT END =========");
      log.info(sj.toString());
    }

    @Override
    public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {

        for (Cookie cookie : list) {
            cookies.put(String.format("exhentai.org#%s", cookie.name()), cookie);
        }
    }

    @NotNull
    @Override
    public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {

        return List.copyOf(cookies.values());
    }
}
