package lab.article;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class ArticleService {
    private final ArticleRepository repo;

    public ArticleService(ArticleRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void edit(long id, String title, String body) {
        Article a = repo.findById(id).orElseThrow();
        sleep(50);
        a.setTitle(title);
        a.setBody(body);
    }

    @Transactional
    public void edit(EditArticle cmd) {
        Article a = repo.findById(cmd.id()).orElseThrow();
        if (!Objects.equals(a.getVersion(), cmd.version())) {
            throw new ObjectOptimisticLockingFailureException(Article.class, cmd.id());
        }
        a.setTitle(cmd.title());
        a.setBody(cmd.body());
    }

    @Transactional
    public void editWithoutCheck(EditArticle cmd) {
        Article a = repo.findById(cmd.id()).orElseThrow();
        a.setTitle(cmd.title());
        a.setBody(cmd.body());
    }

    @Transactional
    public void like(long id) {
        Article a = repo.findById(id).orElseThrow();
        sleep(20);
        a.setLikes(a.getLikes() + 1);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
