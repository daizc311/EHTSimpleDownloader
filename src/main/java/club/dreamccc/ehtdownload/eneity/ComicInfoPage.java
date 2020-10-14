package club.dreamccc.ehtdownload.eneity;

import lombok.Data;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class ComicInfoPage {

    private final Document document;

    public ComicInfoPage(Document document) {
        //TODO 错误处理
        this.document = document;
    }

    /**
     * 标题
     */
    public String getTitle() {
        return document.select("#gn").text();
    }

    /**
     * 副标题
     */
    public String getSubTitle() {
        return document.select("#gj").text();
    }

    /**
     * 标签列表
     */
    public Map<String, List<String>> getTagMap() {
        return null;
    }

    /**
     * 图片页列表
     */
    public List<String> getImgPageUrls() {

        return document.select("#gdt")
                .first()
                .children()
                .stream()
                .filter(element -> !element.is(".c"))
                .map(gdtlDiv -> gdtlDiv.select("a").attr("href"))
                .collect(Collectors.toList());
    }

    public String getNextPageUrl() {
        return document.select(".ptt")
                .first()
                .select("td")
                .last()
                .select("a")
                .attr("href");
    }

    public Boolean hasNext() {
        return !document.select(".ptt")
                .first()
                .select("td")
                .last()
                .is(".ptdd");
    }

}
