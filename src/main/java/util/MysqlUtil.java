package util;

import model.Repository;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;

public class MysqlUtil {
    public static List<Repository> getRepositories() {
        List<Repository> list = null;
        try(Session session = SessionUtil.getSession()) {
            list = getRepositories(session);
        }
        return list;
    }

    public static Repository getRepositoryByName(String repoName) {
        Repository repo = null;
        try(Session session = SessionUtil.getSession()) {
            List<Repository> list = getRepositories(session);
            for (Repository repository : list) {
                if (repository.getRepoName().equals(repoName)){
                    repo = repository;
                    break;
                }
            }
        }
        return repo;
    }

    public static List<Repository> getRepositories(Session session) {
        List<Repository> list = null;
        //list = session.createQuery("from Repository where containTravisYml = true and (buildTool = 1 or buildTool = 2) and ignoreRepo = false", Repository.class).list();
        list = session.createQuery("from Repository where containTravisYml = true and (buildTool = 1 or buildTool = 2)", Repository.class).list();
        return list;
    }

    public static Repository getRepositoryById(int id) {
        Repository repository = null;
        try(Session session = SessionUtil.getSession()) {
            repository = getRepositoryById(session, id);
        }
        return repository;
    }

    public static Repository getRepositoryById(Session session, long id) {
        Repository repository = null;
        Query<Repository> query = session.createQuery("from Repository t where t.id=:id",
                Repository.class);
        query.setParameter("id", id);
        repository = query.uniqueResult();
        return repository;
    }
}
