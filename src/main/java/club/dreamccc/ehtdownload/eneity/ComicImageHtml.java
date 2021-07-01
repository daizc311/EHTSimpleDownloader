package club.dreamccc.ehtdownload.eneity;

import lombok.Getter;

@Getter
public class ComicImageHtml extends HtmlPage {

    private String comicTitle;

    private String imageName;
    private String sourceImageUrl;
    private String showImageUrl;

    public ComicImageHtml(String url, String html) {
        super(url, html);
    }
}
