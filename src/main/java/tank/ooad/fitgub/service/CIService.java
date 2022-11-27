package tank.ooad.fitgub.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import tank.ooad.fitgub.entity.ci.CI;
import tank.ooad.fitgub.entity.ci.CiWork;
import tank.ooad.fitgub.entity.ci.Job;
import tank.ooad.fitgub.entity.ci.Step;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class CIService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Async
    public void runCI(int repoId, int userId, String ciName, InputStream inputStream) throws IOException {
        Yaml yaml = new Yaml();
        try {
//            InputStream inputStream = new FileInputStream(path);
            Map<String, Object> obj = yaml.load(inputStream);
            var ci = new CI();
            ci.name = (String) obj.get("name");
            var jobs = (List<Object>) obj.get("jobs");
            for (var job : jobs) {
                var jobMap = (Map<String, Object>) job;
                var jobObj = new Job();
                jobObj.name = (String) jobMap.get("name");
                jobObj.runs_on = (String) jobMap.get("runs_on");
                var steps = (List<Object>) jobMap.get("steps");
                for (var step : steps) {
                    var stepMap = (Map<String, Object>) step;
                    var stepObj = new Step();
                    stepObj.name = (String) stepMap.get("name");
                    // make run split by enter
                    var run = (String) stepMap.get("run");
                    stepObj.run = Arrays.asList(run.split(","));
//                stepObj.run = (List<String>) stepMap.get("run");
                    jobObj.steps.add(stepObj);
                }
                ci.jobs.add(jobObj);
            }
            DockerClientService dockerClientService = new DockerClientService();
            //连接docker服务器
            DockerClient client = dockerClientService.connectDocker();
            List<String> returnHash = new ArrayList<>();
            for (var job : ci.jobs) {
                CreateContainerResponse container = client.createContainerCmd("dgy/ci:v0.1").withCmd("sleep", "3").exec();
                client.startContainerCmd(container.getId()).exec();
                OutputStream outputStream = new FileOutputStream("src/main/docker-log/" + container.getId() + ".log");
                jdbcTemplate.update("insert into ci_log (repo_id, user_id,ci_name,output_hash) values (?, ?, ?,?)", repoId, userId, ciName, container.getId());
                returnHash.add(container.getId());
                System.out.println("container id: " + container.getId());
                for (var step : job.steps) {
                    String run = String.join(";", step.run);
                    ExecCreateCmdResponse execCreateCmdResponse = client.execCreateCmd(container.getId())
                            .withAttachStdout(true)
                            .withAttachStderr(true)
                            .withCmd("bash", "-c", run)
                            .exec();
                    client.execStartCmd(execCreateCmdResponse.getId()).withDetach(false).withTty(true)
                            .exec(new ExecStartResultCallback(outputStream, System.err)).awaitCompletion();
                }
                Thread.sleep(1000);
                //check whether the container is running
                if (Boolean.TRUE.equals(client.inspectContainerCmd(container.getId()).exec().getState().getRunning())) {
                    client.stopContainerCmd(container.getId()).exec();
                }
//                client.stopContainerCmd(container.getId()).exec();
                client.removeContainerCmd(container.getId()).exec();
            }
            CompletableFuture.completedFuture(returnHash);
            return;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }catch (java.lang.NullPointerException e){
            // get random 64 length string
            String hash = String.valueOf(Math.random()).substring(2, 66);
            OutputStream outputStream = new FileOutputStream("src/main/docker-log/" + hash + ".log");
            outputStream.write("yaml file is not valid".getBytes());
            outputStream.close();
            jdbcTemplate.update("insert into ci_log (repo_id, user_id,ci_name,output_hash) values (?, ?, ?,?)", repoId, userId, ciName, hash);
            CompletableFuture.completedFuture(Arrays.asList(hash));
        }
        CompletableFuture.completedFuture(null);
    }

    public List<CiWork> getCIList(int repoId) {
        return jdbcTemplate.query("select id as id,ci_name as ci_name,output_hash as output_hash from ci_log where repo_id = ?", CiWork.mapper, repoId);
    }

    public String getCIOutput(int id) {
        var logHash = jdbcTemplate.queryForObject("select output_hash from ci_log where id = ?", String.class, id);
        // open "src/main/docker-log/" + logHash + ".log"
        try {
            BufferedReader br = new BufferedReader(new FileReader("src/main/docker-log/" + logHash + ".log"));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
            }
            br.close();
            return sb.toString();
        } catch (IOException e) {
            return "文件不存在，请联系管理员";
        }
    }
}
