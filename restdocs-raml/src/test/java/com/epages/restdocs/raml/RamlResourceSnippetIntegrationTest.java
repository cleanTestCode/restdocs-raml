package com.epages.restdocs.raml;

import static com.epages.restdocs.raml.ParameterDescriptorWithRamlType.RamlScalarType.INTEGER;
import static com.epages.restdocs.raml.ParameterDescriptorWithRamlType.RamlScalarType.STRING;
import static com.epages.restdocs.raml.RamlResourceDocumentation.parameterWithName;
import static com.epages.restdocs.raml.RamlResourceDocumentation.ramlResource;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.snippet.Attributes.key;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.constraints.ValidatorConstraintResolver;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;

@RunWith(SpringRunner.class)
@WebMvcTest
public class RamlResourceSnippetIntegrationTest implements RamlResourceSnippetTestTrait {

    @Rule

    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    protected String operationName;

    protected ResultActions resultActions;

    @Before
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(documentationConfiguration(this.restDocumentation))
                .build();
        operationName = UUID.randomUUID().toString();
    }

    @Test
    @SneakyThrows
    public void should_document_request() {
        givenEndpointInvoked();

        whenRamlResourceSnippetDocumentedWithoutParameters();

        then(generatedRamlFragmentFile()).exists();
        then(generatedRequestJsonFile()).exists();
        then(generatedResponseJsonFile()).exists();
    }

    @Test
    @SneakyThrows
    public void should_document_request_with_fields() {
        givenEndpointInvoked();

        whenRamlResourceSnippetDocumentedWithRequestAndResponseFields();

        then(generatedRamlFragmentFile()).exists();
        then(generatedRequestJsonFile()).exists();
        then(generatedResponseJsonFile()).exists();

        then(generatedRequestSchemaFile()).exists();
        then(generatedResponseSchemaFile()).exists();
    }

    @Test
    @SneakyThrows
    public void should_document_request_with_null_field() {
        givenEndpointInvoked("null");

        assertThatCode(
            this::whenRamlResourceSnippetDocumentedWithRequestAndResponseFields
        ).doesNotThrowAnyException();
    }

    private void whenRamlResourceSnippetDocumentedWithoutParameters() throws Exception {
        resultActions
                .andDo(document(operationName, ramlResource()));
    }

    private void whenRamlResourceSnippetDocumentedWithRequestAndResponseFields() throws Exception {
        resultActions
                .andDo(document(operationName, buildFullRamlResourceSnippet()));
    }

    protected RamlResourceSnippet buildFullRamlResourceSnippet() {
        return ramlResource(RamlResourceSnippetParameters.builder()
                .requestFields(fieldDescriptors())
                .responseFields(fieldDescriptors().and(fieldWithPath("id").description("id")))
                .pathParameters(
                        parameterWithName("someId").description("some id").type(STRING),
                        parameterWithName("otherId").description("otherId id").type(INTEGER))
                .links(linkWithRel("self").description("some"))
                .build());
    }

    protected FieldDescriptors fieldDescriptors() {
        final ConstrainedFields fields = new ConstrainedFields(TestDateHolder.class);
        return RamlResourceDocumentation.fields(
                fields.withPath("comment").description("the comment").optional(),
                fields.withPath("flag").description("the flag"),
                fields.withPath("count").description("the count")
        );
    }

    protected void givenEndpointInvoked() throws Exception {
        givenEndpointInvoked("true");
    }

    protected void givenEndpointInvoked(String flagValue) throws Exception {
        resultActions = mockMvc.perform(post("/some/{someId}/other/{otherId}", "id", 1)
                .contentType(APPLICATION_JSON)
                .content(String.format("{\n" +
                        "    \"comment\": \"some\",\n" +
                        "    \"flag\": %s,\n" +
                        "    \"count\": 1\n" +
                        "}", flagValue)))
                .andExpect(status().isOk());
    }

    @Override
    public String getOperationName() {
        return operationName;
    }

    @Override
    public File getRootOutputDirectory() {
        return new File("build/generated-snippets");
    }

    @SpringBootApplication
    static class TestApplication {
        public static void main(String[] args) {
            SpringApplication.run(TestApplication.class, args);
        }
    }

    @RestController
    static class TestController {

        @PostMapping(path = "/some/{someId}/other/{otherId}")
        public ResponseEntity<Resource<TestDateHolder>> doSomething(@PathVariable String someId,
                                                                   @PathVariable Integer otherId,
                                                                   @RequestBody TestDateHolder testDateHolder) {
            testDateHolder.setId(UUID.randomUUID().toString());
            Resource<TestDateHolder> resource = new Resource<>(testDateHolder);
            resource.add(linkTo(methodOn(TestController.class).doSomething(someId, otherId, null)).withSelfRel());
            return ResponseEntity.ok(resource);
        }
    }

    @RequiredArgsConstructor
    @Getter
    static class TestDateHolder {
        @NotNull
        private final String comment;
        private final boolean flag;
        private int count;

        @Setter
        private String id;
    }


    static class ConstrainedFields {
        private final ValidatorConstraintResolver validatorConstraintResolver = new ValidatorConstraintResolver();

        private final Class<?> classHoldingConstraints;

        ConstrainedFields(Class<?> classHoldingConstraints) {
            this.classHoldingConstraints = classHoldingConstraints;
        }

        /**
         * Create a field description with constraints for bean property with the same name
         * @param path json path of the field
         */
        public FieldDescriptor withPath(String path) {
            return fieldWithPath(path).attributes(key("validationConstraints")
                    .value(this.validatorConstraintResolver.resolveForProperty(path, classHoldingConstraints)));
        }

        /**
         *
         * Create a field description with constraints for bean property with a name differing from the path
         * @param jsonPath json path of the field
         * @param beanPropertyName name of the property of the bean that is used to get the field constraint descriptions
         */
        public FieldDescriptor withMappedPath(String jsonPath, String beanPropertyName) {
            return fieldWithPath(jsonPath).attributes(key("validationConstraints")
                    .value(this.validatorConstraintResolver.resolveForProperty(beanPropertyName, classHoldingConstraints)));
        }
    }
}
