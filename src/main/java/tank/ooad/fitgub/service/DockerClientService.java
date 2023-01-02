package tank.ooad.fitgub.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;

import java.util.Properties;

import static com.github.dockerjava.api.model.HostConfig.newHostConfig;

public class DockerClientService {

    public DockerClient connectDocker() {
        DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://127.0.0.1:2375").build();
        DockerClient dockerClient = DockerClientBuilder.getInstance(dockerClientConfig).build();
        Info info = dockerClient.infoCmd().exec();
//        String infoStr = JSONObject.toJSONString(info);
        System.out.println("docker的环境信息如下：=================");
        System.out.println(info);
        return dockerClient;
    }

    public CreateContainerResponse createContainers(DockerClient client, String containerName, String imageName) {
        //映射端口8088—>80
//        ExposedPort tcp80 = ExposedPort.tcp(80);
//        Ports portBindings = new Ports();
//        portBindings.bind(tcp80, Ports.Binding.bindPort(8088));

        //重定向标准输出
//        Bind bind = new Bind(filePath, new Volume("/var/jenkins_home"));



        return client.createContainerCmd(imageName)
                .withName(containerName)
                .withHostConfig(newHostConfig()).exec();
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try {
            properties.load(DockerClientService.class.getClassLoader().getResourceAsStream("docker.properties"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }

    public void startContainer(DockerClient client, String containerId) {
        client.startContainerCmd(containerId).exec();
    }


    public void stopContainer(DockerClient client, String containerId) {
        client.stopContainerCmd(containerId).exec();
    }

    public void removeContainer(DockerClient client, String containerId) {
        client.removeContainerCmd(containerId).exec();
    }


}

