package club.dreamccc.ehtdownload.eneity;

import lombok.Getter;
import lombok.Setter;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Getter
@Setter
public class ComicPage extends HtmlPage {

    private PagingComponent pagingComponent;

    private List<String> comicImageUrls;

    public ComicPage(String url, String html) {
        super(url, html);
    }

}
