/*
 *  Copyright 2020 Johnny850807 (Waterball) 潘冠辰
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import io.kubernetes.client.apis.BatchV1Api;
import io.kubernetes.client.models.V1Container;
import io.kubernetes.client.models.V1Job;
import io.kubernetes.client.models.V1ResourceRequirements;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import tw.waterball.judgegirl.commons.token.TokenService;
import tw.waterball.judgegirl.commons.utils.Delay;
import tw.waterball.judgegirl.entities.problem.JudgeStatus;
import tw.waterball.judgegirl.entities.problem.Problem;
import tw.waterball.judgegirl.entities.stubs.Stubs;
import tw.waterball.judgegirl.entities.submission.Judge;
import tw.waterball.judgegirl.entities.submission.ProgramProfile;
import tw.waterball.judgegirl.entities.submission.Submission;
import tw.waterball.judgegirl.entities.submission.SubmissionThrottling;
import tw.waterball.judgegirl.problemapi.clients.ProblemServiceDriver;
import tw.waterball.judgegirl.problemapi.views.ProblemView;
import tw.waterball.judgegirl.springboot.profiles.Profiles;
import tw.waterball.judgegirl.springboot.submission.SubmissionServiceApplication;
import tw.waterball.judgegirl.springboot.submission.controllers.VerdictIssuedEventHandler;
import tw.waterball.judgegirl.springboot.submission.impl.mongo.data.SubmissionData;
import tw.waterball.judgegirl.springboot.submission.impl.mongo.data.VerdictData;
import tw.waterball.judgegirl.submissionapi.clients.VerdictPublisher;
import tw.waterball.judgegirl.submissionapi.views.SubmissionView;
import tw.waterball.judgegirl.submissionservice.domain.usecases.SubmitCodeUseCase;
import tw.waterball.judgegirl.submissionapi.views.VerdictIssuedEvent;
import tw.waterball.judgegirl.testkit.jupiter.ReplaceUnderscoresWithCamelCasesDisplayNameGenerators;
import tw.waterball.judgegirl.testkit.resultmatchers.ZipResultMatcher;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static tw.waterball.judgegirl.commons.token.TokenService.Identity.admin;
import static tw.waterball.judgegirl.springboot.submission.controllers.SubmissionController.SUBMIT_CODE_MULTIPART_KEY_NAME;
import static tw.waterball.judgegirl.testkit.resultmatchers.ZipResultMatcher.zip;

/**
 * @author - johnny850807@gmail.com (Waterball)
 */
@SpringBootTest
@ActiveProfiles({Profiles.PROD, Profiles.MONGO, Profiles.AMQP, Profiles.K8S})
@AutoConfigureMockMvc
@AutoConfigureDataMongo
@ContextConfiguration(classes = {SubmissionServiceApplication.class, SubmissionControllerIT.TestConfig.class})
@DisplayNameGeneration(ReplaceUnderscoresWithCamelCasesDisplayNameGenerators.class)
public class SubmissionControllerIT {
    private final String API_PREFIX = "/api/problems/{problemId}/students/{studentId}/submissions";
    private final Problem problem = Stubs.problemTemplateBuilder().build();
    private final String SUBMISSION_EXCHANGE_NAME = "submissions";

    @Value("${spring.rabbitmq.username}")
    String amqpUsername;

    @Value("${spring.rabbitmq.password}")
    String amqpPassword;

    @Value("${spring.rabbitmq.virtual-host}")
    String amqpVirtualHost;

    @Value("${spring.rabbitmq.host}")
    String amqpAddress;

    @Value("${spring.rabbitmq.port}")
    int amqpPort;

    @Value("${jwt.test.student1.id}")
    int STUDENT1_ID;

    @Value("${jwt.test.student1.token}")
    String STUDENT1_TOKEN;

    @Value("${jwt.test.student2.id}")
    int STUDENT2_ID;

    @Value("${jwt.test.student2.token}")
    String STUDENT2_TOKEN;

    @Value("${jwt.token-admin}")
    String adminToken;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    SubmitCodeUseCase submitCodeUseCase;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    VerdictPublisher verdictPublisher;

    @MockBean
    ProblemServiceDriver problemServiceDriver;

    @MockBean
    BatchV1Api batchV1Api;

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    AmqpTemplate amqpTemplate;

    @Autowired
    TokenService tokenService;

    @Autowired
    VerdictIssuedEventHandler verdictIssuedEventHandler;

    // For submission
    private MockMultipartFile[] mockFiles = {
            new MockMultipartFile(SUBMIT_CODE_MULTIPART_KEY_NAME, "func1.c", "text/plain",
                    "int plus(int a, int b) {return a + b;}".getBytes()),
            new MockMultipartFile(SUBMIT_CODE_MULTIPART_KEY_NAME, "func2.c", "text/plain",
                    "int minus(int a, int b) {return a - b;}".getBytes())};
    private String ADMIN_TOKEN;

    @BeforeEach
    void setup() {
        ADMIN_TOKEN = tokenService.createToken(admin()).toString();
        amqpAdmin.declareExchange(new TopicExchange(SUBMISSION_EXCHANGE_NAME));
        mockGetProblemById();
    }

    private void mockGetProblemById() {
        when(problemServiceDriver.getProblem(problem.getId())).thenReturn(
                ProblemView.fromEntity(problem));
    }

    @AfterEach
    void clean() {
        mongoTemplate.dropCollection(Submission.class);
        // throttling must be disabled, otherwise the following submissions will fail (be throttled)
        mongoTemplate.dropCollection(SubmissionThrottling.class);
    }

    @Test
    void TestSubmitAndThenDownload() throws Exception {
        SubmissionView submissionView = givenSubmitCode(STUDENT1_ID, STUDENT1_TOKEN);

        requestGetSubmission(STUDENT1_ID, STUDENT1_TOKEN)
                .andExpect(content().json(
                        objectMapper.writeValueAsString(singletonList(submissionView))));

        requestDownloadSubmittedCodes(STUDENT1_ID, STUDENT1_TOKEN, submissionView.id, submissionView.submittedCodesFileId);
    }

    private ResultActions requestGetSubmission(int studentId, String studentToken) throws Exception {
        return requestWithToken(() -> get(API_PREFIX, problem.getId(), studentId), studentToken)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    private ResultActions requestDownloadSubmittedCodes(int studentId, String studentToken, String submissionId, String submittedCodesFile) throws Exception {
        return requestWithToken(() -> get(API_PREFIX + "/{submissionId}/submittedCodes/{submittedCodesFileId}",
                problem.getId(), studentId, submissionId, submittedCodesFile), studentToken)
                .andExpect(status().isOk())
                .andExpect(zip().content(mockFiles));
    }


    // TODO: drunk code, need improving
    // A White-Box test: Strictly test the submission behavior
    @Test
    void WhenSubmitCodeWithValidToken_ShouldSaveIt_DeployJudger_ListenToVerdictIssuedEvent_AndHandleTheEvent() throws Exception {
        SubmissionView submissionView = givenSubmitCode(STUDENT1_ID, STUDENT1_TOKEN);
        ArgumentCaptor<V1Job> jobCaptor = ArgumentCaptor.forClass(V1Job.class);

        verify(batchV1Api).createNamespacedJob(anyString(), jobCaptor.capture(), any(), any(), any());
        V1Job job = jobCaptor.getValue();

        // Verify the applied k8s job is correct according to the problem, submission and the student
        assertEquals(1, job.getSpec().getTemplate().getSpec().getContainers().size(), "Should only deploy one containers.");
        V1Container judgerContainer = job.getSpec().getTemplate().getSpec().getContainers().get(0);
        assertTrue(judgerContainer.getEnv().stream().anyMatch(e ->
                e.getName().equals("problemId") && Integer.parseInt(e.getValue()) == problem.getId()), "env problemId incorrect.");
        assertTrue(judgerContainer.getEnv().stream().anyMatch(e ->
                e.getName().equals("submissionId") && e.getValue().equals(submissionView.getId())), "env submissionId incorrect.");
        assertTrue(judgerContainer.getEnv().stream().anyMatch(e ->
                e.getName().equals("studentId") && Integer.parseInt(e.getValue()) == STUDENT1_ID), "env studentId incorrect.");
        V1ResourceRequirements resources = judgerContainer.getResources();
        assertEquals(problem.getJudgeSpec().getCpu(),
                resources.getRequests().get("cpu").getNumber().floatValue());
        assertEquals(problem.getJudgeSpec().getGpu(),
                resources.getLimits().get("nvidia.com/gpu").getNumber().floatValue());

        // Publish the verdict through message queue after three seconds
        Delay.delay(3000);
        VerdictIssuedEvent verdictIssuedEvent = generateVerdictIssuedEvent(submissionView);
        verdictPublisher.publish(verdictIssuedEvent);

        // Verify the submission is updated with the verdict
        verdictIssuedEventHandler.onHandlingCompletion.doWait(3000);
        SubmissionData updatedSubmissionData = mongoTemplate.findById(submissionView.getId(), SubmissionData.class);
        assertNotNull(updatedSubmissionData);
        VerdictData verdictData = updatedSubmissionData.getVerdict();
        assertEquals(50, verdictData.getTotalGrade());
        assertEquals(JudgeStatus.WA, verdictData.getSummaryStatus());
        assertEquals(new HashSet<>(verdictIssuedEvent.getJudges()),
                new HashSet<>(verdictData.getJudges()));
    }

    private VerdictIssuedEvent generateVerdictIssuedEvent(SubmissionView submissionView) {
        return VerdictIssuedEvent.builder()
                .problemId(problem.getId())
                .problemTitle(problem.getTitle())
                .submissionId(submissionView.getId())
                .judge(new Judge("t1", JudgeStatus.AC, new ProgramProfile(5, 5, ""), 20))
                .judge(new Judge("t2", JudgeStatus.AC, new ProgramProfile(6, 6, ""), 30))
                .judge(new Judge("t3", JudgeStatus.WA, new ProgramProfile(7, 7, ""), 0))
                .issueTime(new Date())
                .build();
    }

    @Test
    void GivenStudent1Submission_WhenGetThatSubmissionUsingAdminToken_ShouldRespondSuccessfully() throws Exception {
        SubmissionView submissionView = givenSubmitCode(STUDENT1_ID, STUDENT1_TOKEN);

        // verify get submissions
        requestWithToken(() -> get(API_PREFIX, problem.getId(), STUDENT1_ID), adminToken)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        objectMapper.writeValueAsString(Collections.singletonList(submissionView))));

        // verify download submitted codes
        requestWithToken(() -> get(API_PREFIX + "/{submissionId}/submittedCodes/{submittedCodesFileId}",
                problem.getId(), STUDENT1_ID, submissionView.getId(), submissionView.submittedCodesFileId), adminToken)
                .andExpect(status().isOk())
                .andExpect(ZipResultMatcher.zip().content(mockFiles));
    }

    @Test
    void WhenGetStudent1SubmissionsWithStudent2Token_ShouldBeForbidden() throws Exception {
        requestWithToken(() -> get(API_PREFIX, problem.getId(),
                STUDENT1_ID), STUDENT2_TOKEN)
                .andExpect(status().isForbidden());
    }

    private ResultActions requestWithToken(Supplier<MockHttpServletRequestBuilder> requestBuilderSupplier,
                                           String token) throws Exception {
        return mockMvc.perform(requestBuilderSupplier.get()
                .header("Authorization", "bearer " + token));
    }

    //
    @Test
    void GivenStudent1Submission_WhenGetThatSubmissionUsingStudent2Token_ShouldRespondForbidden() throws Exception {
        SubmissionView submissionView = givenSubmitCode(STUDENT1_ID, STUDENT1_TOKEN);
        requestWithToken(() -> get(API_PREFIX + "/{submissionId}",
                problem.getId(), STUDENT1_ID, submissionView.getId()), STUDENT2_TOKEN)
                .andExpect(status().isForbidden());
    }

    @Test
    void GivenStudent1Submission_WhenGetThatSubmissionUnderStudent2_ShouldRespondNotFound() throws Exception {
        SubmissionView submissionView = givenSubmitCode(STUDENT1_ID, STUDENT1_TOKEN);
        requestWithToken(() -> get(API_PREFIX + "/{submissionId}",
                problem.getId(), STUDENT2_ID, submissionView.getId()), STUDENT2_TOKEN)
                .andExpect(status().isNotFound());
    }

    @Test
    void GivenStudent1Submission_WhenDownloadItsSubmittedCodesUnderStudent2_ShouldRespondNotFound() throws Exception {
        SubmissionView submissionView = givenSubmitCode(STUDENT1_ID, STUDENT1_TOKEN);
        requestWithToken(() -> get(API_PREFIX + "/{submissionId}/submittedCodes/{submittedCodesFileId}",
                problem.getId(), STUDENT2_ID, submissionView.getId(),
                submissionView.submittedCodesFileId), STUDENT2_TOKEN)
                .andExpect(status().isNotFound());
    }

    @Test
    void GivenStudent1Submission_WhenDownloadItsSubmittedCodeUsingStudent2Token_ShouldBeForbidden() throws Exception {
        SubmissionView submissionView = givenSubmitCode(STUDENT1_ID, STUDENT1_TOKEN);

        requestWithToken(() -> get(API_PREFIX + "/{submissionId}/submittedCodes/{submittedCodesFileId}",
                problem.getId(), STUDENT1_ID, submissionView.getId(), submissionView.submittedCodesFileId), STUDENT2_TOKEN)
                .andExpect(status().isForbidden());
    }

    @Test
    void GivenParallelSubmissions_WhenGetThoseSubmissionsInPage_ShouldReturnOnlyTheSubmissionsInThatPage() throws Exception {
        List<SubmissionView> submissionViews =
                givenParallelStudentSubmissions(STUDENT1_ID, 50);

        Set<SubmissionView> actualSubmissionsInPreviousPage = new HashSet<>();
        List<SubmissionView> actualSubmissions;
        List<SubmissionView> actualAllSubmissions = new ArrayList<>();

        int page = 0;
        do {
            actualSubmissions = getSubmissionsInPage(STUDENT1_ID, STUDENT1_TOKEN, page++);
            assertTrue(actualSubmissions.stream()
                    .noneMatch(actualSubmissionsInPreviousPage::contains));
            actualAllSubmissions.addAll(actualSubmissions);
            actualSubmissionsInPreviousPage = new HashSet<>(actualSubmissions);
        } while (!actualSubmissions.isEmpty());

        assertEquals(new HashSet<>(submissionViews), new HashSet<>(actualAllSubmissions));
    }

    @Test
    void WhenSubmitCodeUnderStudent1UsingStudent2Token_ShouldBeForbidden() throws Exception {
        requestSubmitCode(STUDENT1_ID, STUDENT2_TOKEN)
                .andExpect(status().isForbidden());
    }

    private List<SubmissionView> givenParallelStudentSubmissions(int studentId, int count) throws Exception {
        return IntStream.range(0, count).parallel()
                .mapToObj(i -> {
                    try {
                        return givenSubmitCode(studentId,
                                /*Must use the Admin token to avoid SubmissionThrottling*/
                                ADMIN_TOKEN);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }

    private SubmissionView givenSubmitCode(int studentId, String token) throws Exception {
        String responseJson = requestSubmitCode(studentId, token)
                .andExpect(status().isAccepted())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("id").exists())
                .andExpect(jsonPath("studentId").value(studentId))
                .andExpect(jsonPath("problemId").value(problem.getId()))
                .andExpect(jsonPath("submittedCodesFileId").exists())
                .andExpect(jsonPath("submissionTime").exists())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(responseJson, SubmissionView.class);
    }

    private ResultActions requestSubmitCode(int studentId, String token) throws Exception {
        return requestWithToken(() ->
                multipartRequestWithSubmittedCodes(studentId), token);
    }

    private MockMultipartHttpServletRequestBuilder multipartRequestWithSubmittedCodes(int studentId) {
        return multipart(API_PREFIX, problem.getId(), studentId)
                .file(mockFiles[0])
                .file(mockFiles[1]);
    }

    private List<SubmissionView> getSubmissionsInPage(int studentId, String studentToken, int page) throws Exception {
        MvcResult result = requestWithToken(() -> get(API_PREFIX + "?page={page}",
                problem.getId(), studentId, page), studentToken).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<List<SubmissionView>>() {
                });
    }

    @Configuration
    public static class TestConfig {
        @Bean
        @Primary
        public ConnectionFactory mockRabbitMqConnectionFactory() {
            return new CachingConnectionFactory(new MockConnectionFactory());
        }
    }

}