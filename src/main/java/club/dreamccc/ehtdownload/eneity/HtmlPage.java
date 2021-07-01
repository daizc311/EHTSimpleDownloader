package club.dreamccc.ehtdownload.eneity;

import lombok.Getter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public abstract class HtmlPage {

    @Getter
    private final String url;

    @Getter
    private final String html;

    @Getter
    private final Document jsoupDoc;

    public HtmlPage(String url,String html) {
        this.url = url;
        this.html = html;
        this.jsoupDoc = Jsoup.parse(html);
    }
}
