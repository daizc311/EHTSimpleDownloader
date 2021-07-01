package club.dreamccc.ehtdownload.eneity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;

@Getter
@Setter(value = AccessLevel.PROTECTED)
public class ComicImageHtml extends HtmlPage {


    public String getComicTitle() {
        return super.getJsoupDoc().select("h1").text();
    }

    public String getImageName() {
        return super.getJsoupDoc().select("#i4 > :first-child").text().split("::")[0].trim();
    }

    public String getSourceImageUrl() {
        return super.getJsoupDoc().select("a:containsOwn(Download original)").attr("href");
    }

    public String getShowImageUrl() {
        return super.getJsoupDoc().select("#img").attr("src");
    }

    public ComicImageHtml(String url, String html) {
        super(url, html);
    }
}
