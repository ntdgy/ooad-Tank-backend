package tank.ooad.fitgub.git;

import cn.hutool.core.io.CharsetDetector;
import cn.hutool.core.io.FileUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEditor;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tank.ooad.fitgub.entity.git.*;
import tank.ooad.fitgub.entity.repo.Issue;
import tank.ooad.fitgub.entity.repo.Repo;
import tank.ooad.fitgub.exception.CustomException;
import tank.ooad.fitgub.utils.ReturnCode;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;


@Component
@Slf4j
public class GitOperation {
    
    @Autowired
    private JdbcTemplate template;

    private static final String REPO_STORE_PATH = "../repo-store";

    private static final GitPerson xynhub = new GitPerson("xynhub", "ooad@dgy.ac.cn");

    public List<GitCommit> getCommits(Repo repo, String ref) throws IOException, GitAPIException {
        Repository repository = getRepository(repo);
        var head = repository.resolve(ref);
        Git git = new Git(repository);
        RevWalk walk = new RevWalk(repository);
        Iterable<RevCommit> commits = git.log().add(head).call();
        List<GitCommit> gitCommits = new ArrayList<>();
        for (RevCommit commit : commits) {
            GitCommit gitCommit = new GitCommit();
            gitCommit.commit_hash = commit.getName();
            gitCommit.commit_message = commit.getFullMessage();
            gitCommit.commit_time = commit.getCommitTime();
            var author = commit.getAuthorIdent();
            gitCommit.author = new GitPerson(author.getName(), author.getEmailAddress());
            var committer = commit.getCommitterIdent();
            gitCommit.committer = new GitPerson(committer.getName(), committer.getEmailAddress());
            gitCommits.add(gitCommit);
        }
        return gitCommits;
    }

    public record MergeBranch(int ownerId, int repoId, String branchName){}
    public record MergeRequest(MergeBranch from, MergeBranch to){}

    private final HashMap<MergeRequest, GitMergeStatus> mergeStatusCache = new HashMap<>();

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
    private Repository getRepository(int ownerId, int repoId) throws IOException {
        File repoPath = new File(REPO_STORE_PATH, String.format("%s/%s", ownerId, repoId));
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

    public boolean checkBranchExistsInGitRepo(Repo repo, String branchName) throws IOException {
        try (Repository repository = getRepository(repo)) {
            return repository.getRefDatabase().getRefsByPrefix("refs/heads/").stream().anyMatch(ref -> ref.getName().equals("refs/heads/" + branchName));
        }
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
            var entry = new GitTreeEntry(treeWalk.getNameString());
            if (treeWalk.isSubtree())
                entry.name += "/";
            files.add(entry);
            var objHash = treeWalk.getObjectId(0).getName();
            if (treeBlobIndexExists(repo, objHash)) {
                var modify_commit = template.queryForObject("select commit_hash from commit_index where repo_id = ? and blob_or_tree_hash = ?;", String.class, repo.id, objHash);
                entry.modify_commit = getCommitFromIndex(repo, modify_commit, false);
            }
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
            Repo repo, String branchName, GitPerson person, String gitMessage, Map<String, byte[]> contents
    ) {
        PersonIdent author = new PersonIdent(person.name, person.email);
        RevCommit revCommit;
        try {
            Repository repository = getRepository(repo);
            try (ObjectInserter odi = repository.newObjectInserter()) {
                ObjectId headId = repository.resolve(branchName + "^{commit}");
                DirCache index = createTemporaryIndex(repository, headId, contents);
                if (index == null) return null;
                ObjectId indexTreeId = index.writeTree(odi);
                odi.flush();

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
                RevWalk revWalk = new RevWalk(repository);
                revCommit = revWalk.parseCommit(commitId);
                RefUpdate ru = repository.updateRef("refs/heads/" + branchName);
                if (headId == null) {
                    ru.setExpectedOldObjectId(ObjectId.zeroId());
                } else {
                    ru.setExpectedOldObjectId(headId);
                }
                ru.setNewObjectId(commitId);
                ru.update();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println(revCommit.getName());
        GitCommit gitCommit = new GitCommit();
        gitCommit.commit_time = revCommit.getCommitTime();
        gitCommit.commit_message = gitMessage;
        gitCommit.author = person;
        gitCommit.committer = xynhub;
        gitCommit.commit_hash = revCommit.getName();
        return gitCommit;
    }

    private static DirCache createTemporaryIndex(Repository repository, ObjectId headId, Map<String, byte[]> contents) {
        Map<String, ObjectId> fileObjects = new HashMap<>(contents.size());
        DirCache inCoreIndex = DirCache.newInCore();
        DirCacheEditor editor = inCoreIndex.editor();
        ObjectInserter inserter = repository.newObjectInserter();
        try {
            for (Map.Entry<String, byte[]> pathAndContent : contents.entrySet()) {
                String gPath = pathAndContent.getKey();
                ObjectId objId = inserter.insert(Constants.OBJ_BLOB, pathAndContent.getValue());
                fileObjects.put(gPath, objId);
            }
            System.out.println(fileObjects);
            iterateOverTreeWalk(repository, headId,
                    (walkPath, hTree) -> {
                        System.out.println(walkPath);
                        if (fileObjects.containsKey(walkPath) && fileObjects.get(walkPath).equals(hTree.getEntryObjectId())) {
                            fileObjects.remove(walkPath);
                        }
                        if (fileObjects.get(walkPath) == null) {
                            addToTemporaryInCoreIndex(editor, walkPath, hTree);
                        }
                    });
            System.out.println(fileObjects);
            fileObjects.forEach((path, objId) -> {
                editor.add(new DirCacheEditor.PathEdit(path) {
                    @Override
                    public void apply(DirCacheEntry ent) {
                        System.out.println("Apply Edit at Entry" + ent.getPathString());
                        ent.setFileMode(FileMode.REGULAR_FILE);
                        ent.setObjectId(objId);
                    }
                });
            });
            editor.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (fileObjects.isEmpty()) {
            return null;
        }
        return inCoreIndex;
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

    private static void addToTemporaryInCoreIndex(DirCacheEditor editor, String path, CanonicalTreeParser hTree) {
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


    public boolean changeDefaultBranch(Repo repo, String name) throws IOException {
        Repository repository = getRepository(repo);
        var branches = repository.getRefDatabase().getRefsByPrefix("refs/heads/");
        for (Ref branch : branches) {
            if (branch.getName().equals("refs/heads/" + name)) {
                var refUpdate = repository.getRefDatabase().newUpdate(Constants.HEAD, false);
                if (refUpdate.getRef().getTarget().getName().equals(branch.getName())) return false;
                refUpdate.link("refs/heads/" + name);
                refUpdate.update();
                return true;
            }
        }
        return false;
    }

    @SneakyThrows
    public GitMergeStatus getMergeStatus(MergeRequest mergeRequest, Issue pull) {
        if (mergeStatusCache.containsKey(mergeRequest)) {
            GitMergeStatus cached = mergeStatusCache.get(mergeRequest);
            try (var source = getRepository(mergeRequest.from.ownerId, mergeRequest.from.repoId);var target = getRepository(mergeRequest.to.ownerId, mergeRequest.to.repoId)) {
                var sourceBranch = source.resolve(mergeRequest.from.branchName);
                var targetBranch = target.resolve(mergeRequest.to.branchName);
                if (sourceBranch != null && sourceBranch.toObjectId().toString().equals(cached.from_branch_commit)
                    && targetBranch != null && targetBranch.toObjectId().toString().equals(cached.to_branch_commit)) {
                    if (cached.status != GitMergeStatus.Status.PENDING) {
                        log.info("Result cached!");
                        return cached;
                    }
                }
            }
        }
        // recalculate the result
        GitMergeStatus ret = new GitMergeStatus(pull);
        mergeStatusCache.put(mergeRequest, ret);
        File tempFolder;
        while (true) {
            tempFolder = new File(String.format("/tmp/merge-tmp-%s-%s-%s-%s-%s", ret.from.getOwnerName(), ret.from.getRepoName(), ret.to.getOwnerName(), ret.to.getRepoName(), UUID.randomUUID()));
            if (tempFolder.exists()) continue;
            tempFolder.mkdir();
            break;
        }
        try {
            try (var source = getRepository(mergeRequest.from.ownerId, mergeRequest.from.repoId);var target = getRepository(mergeRequest.to.ownerId, mergeRequest.to.repoId)) {
                var fromCommit = source.resolve(ret.from_branch);
                if (fromCommit == null) {
                    ret.status = GitMergeStatus.Status.BRANCH_DELETED;
                    return ret;
                }
                ret.from_branch_commit = fromCommit.toString();

                var toCommit = target.resolve(ret.to_branch);
                if (toCommit == null) {
                    ret.status = GitMergeStatus.Status.BRANCH_DELETED;
                    return ret;
                }
                ret.to_branch_commit = toCommit.toString();
            }
            log.info("check merge status at " + tempFolder);
            Git copiedRepo =  Git.init().setBare(false).setDirectory(tempFolder).call();

            var fromRepoPath = new File(REPO_STORE_PATH, String.format("%s/%s", mergeRequest.from.ownerId, mergeRequest.from.repoId));
            var toRepoPath = new File(REPO_STORE_PATH, String.format("%s/%s", mergeRequest.to.ownerId, mergeRequest.to.repoId));
            copiedRepo.remoteAdd().setName("from").setUri(new URIish(fromRepoPath.getAbsolutePath())).call();
            copiedRepo.fetch().setRemote("from").setInitialBranch(mergeRequest.from.branchName).call();
            copiedRepo.checkout().setName("from").setCreateBranch(true).setStartPoint("from/" + mergeRequest.from.branchName).call();

            copiedRepo.remoteAdd().setName("into").setUri(new URIish(toRepoPath.getAbsolutePath())).call();
            copiedRepo.fetch().setRemote("into").setInitialBranch(mergeRequest.to.branchName).call();
            copiedRepo.checkout().setName("into").setCreateBranch(true).setStartPoint("into/" + mergeRequest.to.branchName).call();

            Config config = copiedRepo.getRepository().getConfig();
            config.setBoolean(ConfigConstants.CONFIG_COMMIT_SECTION, null,
                    ConfigConstants.CONFIG_KEY_GPGSIGN, false);
            config.setString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_NAME, "xynHub");
            config.setString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_EMAIL, "ooad@ooad.dgy.ac.cn");
            var result = copiedRepo.merge()
                    .include(copiedRepo.getRepository().findRef("from"))
                    .setCommit(true)
                    .setStrategy(MergeStrategy.RECURSIVE)
                    .setMessage("Miao Miao Miao")
                    .call();
            log.info(result.toString());
            if (result.getMergeStatus().isSuccessful()) {
                ret.status = GitMergeStatus.Status.READY;
            } else {
                ret.status = GitMergeStatus.Status.CONFLICT;
                ret.conflict_files.addAll(result.getConflicts().keySet());
            }
            return ret;
        } finally {
            FileUtil.del(tempFolder);
        }
    }


    @SneakyThrows
    public boolean merge(MergeRequest mergeRequest, Issue pull, String message) {
        File tempFolder;
        while (true) {
            tempFolder = new File(String.format("/tmp/merge-tmp-%s", UUID.randomUUID()));
            if (tempFolder.exists()) continue;
            tempFolder.mkdir();
            break;
        }
        try (var source = getRepository(mergeRequest.from.ownerId, mergeRequest.from.repoId); var target = getRepository(mergeRequest.to.ownerId, mergeRequest.to.repoId)) {
            var fromCommit = source.resolve(mergeRequest.from.branchName);
            if (fromCommit == null) return false;
            var toCommit = target.resolve(mergeRequest.to.branchName);
            if (toCommit == null) return false;

            log.info("check merge status at " + tempFolder);
            Git copiedRepo = Git.init().setBare(false).setDirectory(tempFolder).call();
            var fromRepoPath = new File(REPO_STORE_PATH, String.format("%s/%s", mergeRequest.from.ownerId, mergeRequest.from.repoId));
            var toRepoPath = new File(REPO_STORE_PATH, String.format("%s/%s", mergeRequest.to.ownerId, mergeRequest.to.repoId));
            copiedRepo.remoteAdd().setName("from").setUri(new URIish(fromRepoPath.getAbsolutePath())).call();
            copiedRepo.fetch().setRemote("from").setInitialBranch(mergeRequest.from.branchName).call();
            copiedRepo.checkout().setName("from").setCreateBranch(true).setStartPoint("from/" + mergeRequest.from.branchName).call();

            copiedRepo.remoteAdd().setName("into").setUri(new URIish(toRepoPath.getAbsolutePath())).call();
            copiedRepo.fetch().setRemote("into").setInitialBranch(mergeRequest.to.branchName).call();
            copiedRepo.checkout().setName("into").setCreateBranch(true).setStartPoint("into/" + mergeRequest.to.branchName).call();

            Config config = copiedRepo.getRepository().getConfig();
            config.setBoolean(ConfigConstants.CONFIG_COMMIT_SECTION, null,
                    ConfigConstants.CONFIG_KEY_GPGSIGN, false);
            config.setString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_NAME, "xynHub");
            config.setString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_EMAIL, "ooad@ooad.dgy.ac.cn");
            var result = copiedRepo.merge()
                    .include(copiedRepo.getRepository().findRef("from"))
                    .setCommit(true)
                    .setStrategy(MergeStrategy.RECURSIVE)
                    .setMessage(message)
                    .call();
            log.info(result.toString());
            if (result.getMergeStatus().isSuccessful()) {
                var newHead = result.getNewHead();
                log.info("merged head: " + newHead);
                copyCommits(copiedRepo.getRepository(), target, newHead);
                var update = target.getRefDatabase().newUpdate("refs/heads/" + mergeRequest.to.branchName, false);
                update.setNewObjectId(newHead);
                update.update();
                return true;
            }
            return false;
        } finally {
            FileUtil.del(tempFolder);
        }
    }

    @SneakyThrows
    public boolean revert(Repo repo, String branchName, String revertedCommitHash) {
        File tempFolder;
        while (true) {
            tempFolder = new File(String.format("/tmp/merge-tmp-%s", UUID.randomUUID()));
            if (tempFolder.exists()) continue;
            tempFolder.mkdir();
            break;
        }
        try (var target = getRepository(repo)) {
            log.info("revert at " + tempFolder);
            Git copiedRepo = Git.init().setBare(false).setDirectory(tempFolder).call();
            copiedRepo.remoteAdd().setName("from").setUri(new URIish(target.getDirectory().getAbsolutePath())).call();
            copiedRepo.fetch().setRemote("from").setInitialBranch(branchName).call();
            copiedRepo.checkout().setName("from").setCreateBranch(true).setStartPoint("from/" + branchName).call();

            Config config = copiedRepo.getRepository().getConfig();
            config.setBoolean(ConfigConstants.CONFIG_COMMIT_SECTION, null,
                    ConfigConstants.CONFIG_KEY_GPGSIGN, false);
            config.setString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_NAME, "xynHub");
            config.setString(ConfigConstants.CONFIG_USER_SECTION, null, ConfigConstants.CONFIG_KEY_EMAIL, "ooad@ooad.dgy.ac.cn");
            var newHead = copiedRepo.revert()
                    .include(target.resolve(revertedCommitHash))
                    .call();

            copyCommits(copiedRepo.getRepository(), target, newHead);
            var update = target.getRefDatabase().newUpdate("refs/heads/" + branchName, false);
            update.setNewObjectId(newHead);
            update.update();
            return true;

        } finally {
            FileUtil.del(tempFolder);
        }

    }


    public static void copyCommits(Repository from, Repository to, ObjectId commitId) throws IOException {
        try (var toReader = to.getObjectDatabase().newReader(); var fromReader = from.getObjectDatabase().newReader(); var toWriter = to.getObjectDatabase().newInserter()) {
            copyCommits(from, fromReader, toReader, toWriter, commitId);
        }
    }

    private static void CopyObject(ObjectInserter inserter, ObjectLoader loader) throws IOException {
        try (var stream = loader.openStream()) {
            inserter.insert(loader.getType(), loader.getSize(), stream);
        }
    }

    public static void copyCommits(Repository from, ObjectReader fromReader, ObjectReader toReader, ObjectInserter toWriter, ObjectId rootCommitId) throws IOException {
        Stack<ObjectId> commits = new Stack<>();
        Stack<ObjectId> trees = new Stack<>();
        commits.push(rootCommitId);
        while (!commits.isEmpty()) {
            ObjectId currentCommit = commits.pop();
            if (toReader.has(currentCommit)) continue;
            // Insert Commit
            System.out.println("Copy Commit " + currentCommit.getName());
            CopyObject(toWriter, fromReader.open(currentCommit));

            RevCommit commit = new RevWalk(from).parseCommit(currentCommit);
            for (RevCommit parent : commit.getParents()) commits.push(parent.getId());
            // Check Tree
            trees.push(new RevWalk(from).parseCommit(currentCommit).getTree().getId());
            try (TreeWalk walk = new TreeWalk(from)) {
                walk.setRecursive(false);
                while (!trees.isEmpty()) {
                    var treeId = trees.pop();
                    if (toReader.has(treeId)) continue;
                    System.out.println("Copy tree " + currentCommit.getName());
                    CopyObject(toWriter, fromReader.open(treeId));
                    walk.reset(treeId);
                    MutableObjectId objectId = new MutableObjectId();
                    while (walk.next()) {
                        walk.getObjectId(objectId, 0);
                        if (toReader.has(objectId)) continue;
                        if (walk.isSubtree()) trees.push(objectId.toObjectId());
                        else {
                            System.out.println("Copy blob " + objectId.getName());
                            CopyObject(toWriter, fromReader.open(objectId));
                        }
                    }
                }
            }
        }
    }

    @SneakyThrows
    public void buildRepoIndex(Repo repo) {
        try (var repository = getRepository(repo)) {
            var branches = repository.getRefDatabase().getRefsByPrefix("refs/heads/").stream().map(Ref::getName).map((str) -> StringUtils.removeStart(str, "refs/heads/")).toList();
            for (String branch : branches) {
                var headCommit = repository.resolve(branch);
                if (indexExists(repo, headCommit.getName())) continue;
                // build index for headCommit
                buildIndexRecursively(repo, repository, headCommit);
            }
        }
    }

    @SneakyThrows
    private void buildIndexRecursively(Repo repo, Repository repository, ObjectId commitHash) {
        if (indexExists(repo, commitHash.getName())) return;

        RevCommit commit = new RevWalk(repository).parseCommit(commitHash);
        for (RevCommit parent : commit.getParents()) buildIndexRecursively(repo, repository, parent.getId());
        // Check Tree
        String parentHash = commit.getParents().length == 0 ? "" : commit.getParents()[0].getName();
        try (TreeWalk walk = new TreeWalk(repository)) {
            RevTree rootTree = new RevWalk(repository).parseCommit(commitHash).getTree();
            if (treeBlobIndexExists(repo, rootTree.getName())) return;
            walk.setRecursive(false);
            walk.reset(rootTree);
            MutableObjectId objectId = new MutableObjectId();
            while (walk.next()) {
                walk.getObjectId(objectId, 0);
                if (treeBlobIndexExists(repo, objectId.getName())) continue;
                if (walk.isSubtree()) {
                    log.info("test tree for " + walk.getPathString() + " in object " + objectId);
                    insertIndex(repo, objectId.getName(), walk.getPathString() + "/", commit.getName(), parentHash);
                    walk.enterSubtree();
                } else {
                    log.info("test blob for " + walk.getPathString() + " in object " + objectId);
                    insertIndex(repo, objectId.getName(), walk.getPathString(), commit.getName(), parentHash);
                }
            }
        }

    }

    private boolean indexExists(Repo repo, String commitHash) {
        return template.queryForObject("select count(*)>0 from commit_index where repo_id = ? and commit_hash = ?;", Boolean.class, repo.id, commitHash);
    }
    private boolean treeBlobIndexExists(Repo repo, String treeOrBlobHash) {
        return template.queryForObject("select count(*)>0 from commit_index where repo_id = ? and blob_or_tree_hash = ?;", Boolean.class, repo.id, treeOrBlobHash);
    }

    private void insertIndex(Repo repo, String treeOrBlobHash, String path, String commitHash, String parentCommitHash) {
        template.update("insert into commit_index(repo_id, file_path, blob_or_tree_hash, commit_hash, parent_commit_hash) VALUES (?,?,?,?,?)", repo.id, path, treeOrBlobHash, commitHash, parentCommitHash);
    }

    @SneakyThrows
    public GitCommit getCommitFromIndex(Repo repo, String hash, boolean containsDiff) {
        if (!indexExists(repo, hash)) {
            buildRepoIndex(repo);
            if (!indexExists(repo, hash)) {
                try (var repository = getRepository(repo)) {
                    if (repository.resolve(hash) == null)
                        throw new CustomException(ReturnCode.COMMIT_NON_EXIST);
                }
            }
        }
        try (var repository = getRepository(repo)) {
            RevWalk walk = new RevWalk(repository);
            RevCommit commit = walk.parseCommit(repository.resolve(hash));
            GitCommit gitCommit = new GitCommit();
            gitCommit.commit_hash = commit.getName();
            gitCommit.commit_message = commit.getFullMessage();
            gitCommit.commit_time = commit.getCommitTime();
            var author = commit.getAuthorIdent();
            gitCommit.author = new GitPerson(author.getName(), author.getEmailAddress());
            var committer = commit.getCommitterIdent();
            gitCommit.committer = new GitPerson(committer.getName(), committer.getEmailAddress());
            // Fill Diffs
            if (containsDiff) {
                gitCommit.diffList = new ArrayList<>();
                var changedFiles = template.queryForList("select * from commit_index where commit_hash = ? and file_path NOT LIKE '%/'", gitCommit.commit_hash);
                for (Map<String, Object> changedFile : changedFiles) {
                    GitCommit.Diff diff = new GitCommit.Diff();
                    diff.file_path = (String) changedFile.get("file_path");
                    diff.current = new String(getGitBlobLoader(repo, gitCommit.commit_hash, "/" + diff.file_path).getCachedBytes());
                    diff.origin = "";
                    if (!changedFile.get("parent_commit_hash").equals("")) {
                        var originLoader = getGitBlobLoader(repo, (String) changedFile.get("parent_commit_hash"), "/" + diff.file_path);
                        if (originLoader != null)
                            diff.origin = new String(originLoader.getCachedBytes());
                    }
                    gitCommit.diffList.add(diff);
                }
            }
            return gitCommit;
        }
    }

}
