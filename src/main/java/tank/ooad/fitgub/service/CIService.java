package tank.ooad.fitgub.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import org.yaml.snakeyaml.Yaml;
import tank.ooad.fitgub.entity.ci.CI;
import tank.ooad.fitgub.entity.ci.Job;
import tank.ooad.fitgub.entity.ci.Step;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CIService {
    public void runCI(String path) throws InterruptedException {
        Yaml yaml = new Yaml();
        try {
            InputStream inputStream = new FileInputStream(path);
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
            System.out.println(ci.jobs.size());
            for (var job : ci.jobs) {
                CreateContainerResponse container = client.createContainerCmd("dgy/ci:v0.1").withCmd("sleep", "3").exec();
                client.startContainerCmd(container.getId()).exec();
                System.out.println("container id: " + container.getId());
                for (var step : job.steps) {
                    String run = String.join(";", step.run);
                    ExecCreateCmdResponse execCreateCmdResponse = client.execCreateCmd(container.getId())
                            .withAttachStdout(true)
                            .withAttachStderr(true)
                            .withCmd("bash", "-c", run)
                            .exec();
                    client.execStartCmd(execCreateCmdResponse.getId()).withDetach(false).withTty(true)
                            .exec(new ExecStartResultCallback(System.out, System.err)).awaitCompletion();
                }
                Thread.sleep(1000);
                client.stopContainerCmd(container.getId()).exec();
                client.removeContainerCmd(container.getId()).exec();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (Exception e){

        }





    }
}
