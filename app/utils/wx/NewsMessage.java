package utils.wx;

import java.util.List;  

/** 
 * 图文消息
 */  
public class NewsMessage extends BaseMessage {  
    // 数量  
    private int ArticleCount;  
    // 图文类集合  
    private List<Article> Articles;  
  
    public int getArticleCount() {  
        return ArticleCount;  
    }  
  
    public void setArticleCount(int articleCount) {  
        ArticleCount = articleCount;  
    }  
  
    public List<Article> getArticles() {  
        return Articles;  
    }  
  
    public void setArticles(List<Article> articles) {  
        Articles = articles;  
    }  
}  
