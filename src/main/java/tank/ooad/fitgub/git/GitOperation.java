package tank.ooad.fitgub.git;

import cn.hutool.core.io.CharsetDetector;
import cn.hutool.core.io.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.git.*;
import tank.ooad.fitgub.entity.repo.Repo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.BiConsumer;
import cn.hutool.core.lang.Pair;


@Component
public class GitOperation {

    private static final String REPO_STORE_PATH = "../repo-store";

    private static final GitPerson xynhub = new GitPerson("xynhub","ooad@dgy.ac.cn");

    /***
     * Create RepoStore in disk.
     */
    public void createGitRepo(Repo repo) throws IOException {
        File repoPath = new File(REPO_STORE_PATH, String.format("%s/%s", repo.owner.id, repo.id));
        if (repoPath.exists())
            throw new RuntimeException("Repo " + repoPath.getAbsolutePath() + " shouldn't exists");
        var fileRepo = new FileRepository(repoPath);
        fileRepo.create(true);
        fileRepo.close();
    }

    public void forkGitRepo(Repo originRepo, Repo forkedRepo) throws IOException {
        File origin = new File(REPO_STORE_PATH, String.format("%s/%s/", originRepo.owner.id, originRepo.id));
        File forked = new File(REPO_STORE_PATH, String.format("%s/%s/", forkedRepo.owner.id, forkedRepo.id));
        FileUtil.copyContent(origin, forked, false);
    }

    public void deleteGitRepo(Repo repo) throws IOException {
        File repoPath = new File(REPO_STORE_PATH, String.format("%s/%s", repo.owner.id, repo.id));
        if (!repoPath.exists())
            throw new RuntimeException("Repo " + repoPath.getAbsolutePath() + " shouldn't exists");
        FileUtils.deleteDirectory(repoPath);
    }

    public Repository getRepository(Repo repo) throws IOException {
        File repoPath = new File(REPO_STORE_PATH, String.format("%s/%s", repo.owner.id, repo.id));
        return new FileRepository(repoPath);
    }

    public GitRepo getGitRepo(Repo repo) throws IOException {
        GitRepo gitRepo = new GitRepo();
        Repository repository = getRepository(repo);
        gitRepo.default_branch = repository.getBranch();
        gitRepo.branches = repository.getRefDatabase().getRefsByPrefix("refs/heads/").stream().map(Ref::getName).map((str) -> StringUtils.removeStart(str, "refs/heads/")).toList();
        gitRepo.tags = repository.getRefDatabase().getRefsByPrefix("refs/tags/").stream().map(Ref::getName).map((str) -> StringUtils.removeStart(str, "refs/tags/")).toList();
        var HEAD = repository.resolve(gitRepo.default_branch);
        if (HEAD != null) {
            var commit = repository.parseCommit(HEAD);
            gitRepo.head = new GitCommit();
            gitRepo.head.commit_hash = commit.getName();
            gitRepo.head.commit_message = commit.getFullMessage();
            gitRepo.head.commit_time = commit.getCommitterIdent().getWhen().getTime();
            gitRepo.head.author = new GitPerson();
            var author = commit.getAuthorIdent();
            gitRepo.head.author.name = author.getName();
            gitRepo.head.author.email = author.getEmailAddress();
            gitRepo.head.committer = new GitPerson();
            var committer = commit.getCommitterIdent();
            gitRepo.head.committer.name = committer.getName();
            gitRepo.head.committer.email = committer.getEmailAddress();
        }
        return gitRepo;
    }

    public List<GitTreeEntry> readGitTree(Repo repo,
                                          String Ref,
                                          String path) throws IOException {
        Repository repository = getRepository(repo);
        var head = repository.resolve(Ref);
        if (head == null) return List.of();
        RevWalk walk = new RevWalk(repository);
        RevCommit commit = walk.parseCommit(head);
        RevTree tree = commit.getTree();
        List<GitTreeEntry> files = new ArrayList<>();
        var prefix = path.split("/");
        if (prefix.length == 0) prefix = new String[]{""};
        int len = 1;
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(false);
        a:
        while (len < prefix.length) {
            len++;
            while (treeWalk.next()) {
                if (treeWalk.isSubtree() && treeWalk.getNameString().equals(prefix[len - 1])) {
                    treeWalk.enterSubtree();
                    continue a;
                }
            }
        }
        while (treeWalk.next() && treeWalk.getDepth() == len - 1) {
            if (treeWalk.isSubtree()) {
                files.add(new GitTreeEntry(treeWalk.getNameString() + "/"));
            } else
                files.add(new GitTreeEntry(treeWalk.getNameString()));
        }
        treeWalk.close();
        return files;
    }

    public GitBlob readTextGitBlob(Repo repo, String ref, String path) throws IOException {
        var loader = getGitBlobLoader(repo, ref, path);
        if (loader == null) return null;
        var istream = loader.openStream();
        var chars = CharsetDetector.detect(32768, istream, null);
        istream.reset();
        var blob = new GitBlob();
        blob.isText = chars != null;
        if (blob.isText)
            blob.content = new String(istream.readAllBytes());
        istream.close();
        return blob;
    }

    public ObjectLoader getGitBlobLoader(Repo repo, String ref, String path) throws IOException {
        Repository repository = getRepository(repo);
        var head = repository.resolve(ref);
        RevWalk walk = new RevWalk(repository);
        RevCommit commit = walk.parseCommit(head);
        RevTree tree = commit.getTree();
        TreeWalk treeWalk = new TreeWalk(repository);
        treeWalk.addTree(tree);
        treeWalk.setRecursive(false);
        var prefix = path.split("/");
        int len = 1;
        a:
        while (len < prefix.length - 1) {
            len++;
            while (treeWalk.next()) {
                if (treeWalk.isSubtree() && treeWalk.getNameString().equals(prefix[len - 1])) {
                    treeWalk.enterSubtree();
                    continue a;
                }
            }
        }
        while (treeWalk.next() && treeWalk.getDepth() == len - 1) {
            if (treeWalk.getNameString().equals(prefix[len])) {
                var blob = treeWalk.getObjectId(0);
                return repository.open(blob);
            }
        }
        return null;
    }

    public GitCommit commit(
            Repo repo,
            String branchName,
            GitPerson person,
            String gitMessage,
            Map<String, byte[]> contents
    ) {
        PersonIdent author = new PersonIdent(person.name, person.email);
        GitCommit gitCommit = new GitCommit();
        try {
            Repository repository = getRepository(repo);
            try (ObjectInserter odi = repository.newObjectInserter()) {
                ObjectId headId = repository.resolve(branchName + "^{commit}");
                DirCache index = createTemporaryIndex(repository, headId, contents);
                if (index != null) {
                    ObjectId indexTreeId = index.writeTree(odi);

                    CommitBuilder commit = new CommitBuilder();
                    commit.setAuthor(author);
                    commit.setCommitter(author);
                    commit.setEncoding(StandardCharsets.UTF_8);
                    commit.setMessage(gitMessage);
                    if (headId != null) {
                        commit.setParentId(headId);
                    }
                    commit.setTreeId(indexTreeId);
                    ObjectId commitId = odi.insert(commit);
                    odi.flush();
                    try (RevWalk revWalk = new RevWalk(repository)) {
                        RevCommit revCommit = revWalk.parseCommit(commitId);
                        RefUpdate ru = repository.updateRef("refs/heads/" + branchName);
                        if (headId == null) {
                            ru.setExpectedOldObjectId(ObjectId.zeroId());
                        } else {
                            ru.setExpectedOldObjectId(headId);
                        }
                        ru.setNewObjectId(commitId);
                        ru.setRefLogMessage("commit: " + revCommit.getShortMessage(), false);
                        RefUpdate.Result rc = ru.forceUpdate();
                        switch (rc) {
                            case NEW:
                            case FORCED:
                            case FAST_FORWARD:
                                break;
                            case REJECTED:
                            case LOCK_FAILURE:
                                throw new ConcurrentRefUpdateException(JGitText.get().couldNotLockHEAD,
                                        ru.getRef(),
                                        rc);
                            default:
                                throw new JGitInternalException(MessageFormat.format(JGitText.get().updatingRefFailed,
                                        Constants.HEAD,
                                        commitId.toString(),
                                        rc));
                        }
                        gitCommit.commit_time = new Date().getTime();
                        gitCommit.commit_message = gitMessage;
                        gitCommit.author = person;
                        gitCommit.committer = xynhub;
                        gitCommit.commit_hash = revCommit.getName();
                    }
                } else {
                    throw new RuntimeException();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return gitCommit;
    }

    private static DirCache createTemporaryIndex(Repository repository, ObjectId headId, Map<String, byte[]> contents) {
        Map<String, Pair<byte[], ObjectId>> paths = new HashMap<String, Pair<byte[], ObjectId>>(contents.size());
        DirCache inCoreIndex = DirCache.newInCore();
        DirCacheEditor editor = inCoreIndex.editor();
        ObjectInserter inserter = repository.newObjectInserter();
        try {
            for (Map.Entry<String, byte[]> pathAndContent : contents.entrySet()) {
                String gPath = pathAndContent.getKey();
                paths.putAll(storePathsIntoHashMap(inserter,
                        pathAndContent,
                        gPath));
            }
            iterateOverTreeWalk(repository,
                    headId,
                    (walkPath, hTree) -> {
                        if (paths.containsKey(walkPath) && paths.get(walkPath).getValue().equals(hTree.getEntryObjectId())) {
                            paths.remove(walkPath);
                        }

                        if (paths.get(walkPath) == null) {
                            addToTemporaryInCoreIndex(editor,
                                    walkPath,
                                    hTree);
                        }
                    });

            editor.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (paths.isEmpty()) {
            return null;
        }
        return inCoreIndex;
    }

    private static Map<String, Pair<byte[], ObjectId>> storePathsIntoHashMap(
            ObjectInserter inserter,
            Map.Entry<String, byte[]> pathAndContent,
            String gpath
    ) {
        Map<String, Pair<byte[], ObjectId>> paths = new HashMap<>();
        try {
            ObjectId objectId = inserter.insert(Constants.OBJ_BLOB, pathAndContent.getValue());
            paths.put(gpath, new Pair<>(pathAndContent.getValue(), objectId));
            return paths;


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void iterateOverTreeWalk(Repository repository, ObjectId headId, BiConsumer<String, CanonicalTreeParser> consumer) {
        try {
            if (headId != null) {
                TreeWalk treeWalk = new TreeWalk(repository);
                int hIdx = treeWalk.addTree(new RevWalk(repository).parseTree(headId));
                treeWalk.setRecursive(true);
                while (treeWalk.next()) {
                    String walkPath = treeWalk.getPathString();
                    CanonicalTreeParser hTree = treeWalk.getTree(hIdx, CanonicalTreeParser.class);
                    consumer.accept(walkPath, hTree);
                }
                treeWalk.close();

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void addToTemporaryInCoreIndex(
            DirCacheEditor editor,
            String path,
            CanonicalTreeParser hTree
    ) {
        DirCacheEntry dcEntry = new DirCacheEntry(path);
        ObjectId objectId = hTree.getEntryObjectId();
        FileMode fileMode = hTree.getEntryFileMode();
        editor.add(new DirCacheEditor.PathEdit(dcEntry) {
            @Override
            public void apply(DirCacheEntry ent) {
                ent.setObjectId(objectId);
                ent.setFileMode(fileMode);
            }
        });

    }


}
