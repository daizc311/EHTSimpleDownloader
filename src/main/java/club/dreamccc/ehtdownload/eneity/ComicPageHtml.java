package club.dreamccc.ehtdownload.eneity;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
public class ComicPageHtml extends HtmlPage {

    public ComicPageHtml(String url, String html) {
        super(url, html);
    }

    public List<String> getComicImageUrls() {

        return super.getJsoupDoc().select("#gdt a").stream().map(element -> element.attr("href")).collect(Collectors.toList());
    }

}
