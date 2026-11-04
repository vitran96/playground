package lab.article;

import jakarta.persistence.*;

@Entity
@Table(name = "article")
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String body;
    private int likes;

    @Version
    private Long version;

    public Article() {}

    public Article(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public Long getVersion() { return version; }
}
