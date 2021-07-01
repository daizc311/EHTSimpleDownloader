package club.dreamccc.ehtdownload.eneity;

import lombok.Getter;

public abstract class HtmlPage {

    @Getter
    private final String url;

    @Getter
    private final String html;

    public HtmlPage(String url,String html) {
        this.url = url;
        this.html = html;
    }
}
