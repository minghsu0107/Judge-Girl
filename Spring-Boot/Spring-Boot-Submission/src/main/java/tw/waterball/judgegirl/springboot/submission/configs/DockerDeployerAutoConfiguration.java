/*
 * Copyright 2020 Johnny850807 (Waterball) 潘冠辰
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *       http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package tw.waterball.judgegirl.springboot.submission.configs;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tw.waterball.judgegirl.springboot.configs.properties.JudgeGirlAmqpProps;
import tw.waterball.judgegirl.springboot.configs.properties.JudgeGirlJudgerProps;
import tw.waterball.judgegirl.springboot.configs.properties.ServiceProps;
import tw.waterball.judgegirl.springboot.submission.impl.deployer.docker.DockerJudgerDeployer;
import tw.waterball.judgegirl.submissionservice.ports.JudgerDeployer;

/**
 * @author - johnny850807@gmail.com (Waterball)
 */
@ConditionalOnProperty(name = "judge-girl.judger.strategy",
        havingValue = DockerDeployerAutoConfiguration.STRATEGY)
@Configuration
public class DockerDeployerAutoConfiguration {
    static final String STRATEGY = "docker";

    @Bean
    public JudgerDeployer kubernetesJudgerDeployer(DockerClient dockerClient,
                                                   ServiceProps.ProblemService problemServiceProps,
                                                   ServiceProps.SubmissionService submissionServiceProps,
                                                   JudgeGirlAmqpProps amqpProp,
                                                   JudgeGirlJudgerProps deployProps) {
        return new DockerJudgerDeployer(dockerClient,
                problemServiceProps, submissionServiceProps, amqpProp, deployProps);
    }

    @Bean
    public DockerClientConfig dockerClientConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder().build();
    }

    @Bean
    public DockerCmdExecFactory dockerCmdExecFactory() {
        return new JerseyDockerCmdExecFactory();
    }

    @Bean
    public DockerClient dockerClient(DockerClientConfig config,
                                     DockerCmdExecFactory dockerCmdExecFactory) {
        return DockerClientImpl.getInstance(config)
                .withDockerCmdExecFactory(dockerCmdExecFactory);
    }
}
