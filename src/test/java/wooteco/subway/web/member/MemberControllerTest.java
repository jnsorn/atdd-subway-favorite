package wooteco.subway.web.member;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static wooteco.subway.acceptance.member.MemberAcceptanceTest.*;
import static wooteco.subway.web.member.interceptor.BearerAuthInterceptor.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import wooteco.subway.doc.MemberDocumentation;
import wooteco.subway.domain.member.Member;
import wooteco.subway.infra.JwtTokenProvider;
import wooteco.subway.service.member.MemberService;
import wooteco.subway.service.member.dto.JoinRequest;
import wooteco.subway.service.member.dto.LoginRequest;
import wooteco.subway.service.member.dto.MemberResponse;
import wooteco.subway.service.member.dto.UpdateMemberRequest;
import wooteco.subway.web.exception.MemberCreationException;

@ExtendWith(RestDocumentationExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
public class MemberControllerTest {
    private static final Gson GSON = new Gson();

    @MockBean
    protected MemberService memberService;
    @Autowired
    protected MockMvc mockMvc;
    @MockBean
    private AuthorizationExtractor authExtractor;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private Member member;
    private String credential = " secret";
    private String mockToken = BEARER + credential;

    @BeforeEach
    public void setUp(WebApplicationContext webApplicationContext,
        RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilter(new ShallowEtagHeaderFilter())
            .apply(documentationConfiguration(restDocumentation))
            .build();
        this.member = new Member(1L, TEST_USER_EMAIL, TEST_USER_NAME, TEST_USER_PASSWORD);
    }

    @Test
    public void createMember() throws Exception {
        MemberResponse response = MemberResponse.of(member);
        given(memberService.createMember(any())).willReturn(response);
        JoinRequest joinRequest = new JoinRequest(TEST_USER_EMAIL, TEST_USER_NAME,
            TEST_USER_PASSWORD);

        String request = GSON.toJson(joinRequest);

        this.mockMvc.perform(post("/members")
            .content(request)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andDo(print())
            .andDo(MemberDocumentation.createMember());
    }

    @Test
    void login() throws Exception {
        LoginRequest loginRequest = new LoginRequest(TEST_USER_EMAIL, TEST_USER_PASSWORD);
        String request = GSON.toJson(loginRequest);

        this.mockMvc.perform(post("/login")
            .content(request)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(print())
            .andDo(MemberDocumentation.loginMember());
    }

    @Test
    void getMemberByEmail() throws Exception {
        setMockToken();
        given(memberService.findMemberByEmail(TEST_USER_EMAIL)).willReturn(member);

        MvcResult result = this.mockMvc.perform(get("/members")
            .header(HttpHeaders.AUTHORIZATION, mockToken))
            .andExpect(status().isOk())
            .andDo(print())
            .andDo(MemberDocumentation.getMemberByEmail())
            .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        mapper.readValue(result.getResponse().getContentAsString(), MemberResponse.class);
    }

    @Test
    void updateMember() throws Exception {
        setMockToken();
        UpdateMemberRequest updateMemberRequest = new UpdateMemberRequest(TEST_USER_NAME,
            TEST_USER_PASSWORD);

        String request = GSON.toJson(updateMemberRequest);

        this.mockMvc.perform(put("/members/{id}", member.getId())
            .header(HttpHeaders.AUTHORIZATION, mockToken)
            .content(request)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(print())
            .andDo(MemberDocumentation.updateMember());
    }

    @Test
    void deleteMember() throws Exception {
        setMockToken();
        this.mockMvc.perform(delete("/members/{id}", member.getId())
            .header(HttpHeaders.AUTHORIZATION, mockToken))
            .andExpect(status().isNoContent())
            .andDo(print())
            .andDo(MemberDocumentation.deleteMember());
    }

    public void setMockToken() {
        given(authExtractor.extract(any(), eq(BEARER))).willReturn(credential);
        given(jwtTokenProvider.validateToken(credential)).willReturn(true);
        given(jwtTokenProvider.getSubject(anyString())).willReturn(TEST_USER_EMAIL);
    }

    @DisplayName("이미 가입된 이메일로 가입 요청 시 예외 발생")
    @Test
    public void failedCreateMember() throws Exception {
        given(memberService.createMember(any())).willThrow(
            new MemberCreationException(MemberCreationException.DUPLICATED_EMAIL));
        JoinRequest joinRequest = new JoinRequest(TEST_USER_EMAIL, TEST_USER_NAME,
            TEST_USER_PASSWORD);

        String request = GSON.toJson(joinRequest);

        this.mockMvc.perform(post("/members")
            .content(request)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andDo(print())
            .andDo(MemberDocumentation.failedCreateMemberByDuplication());
    }

    @DisplayName("회원 가입 시 빈 문자열 입력이 들어올 경우 예외 발생")
    @Test
    public void failedCreateMember2() throws Exception {
        JoinRequest joinRequest = new JoinRequest(TEST_USER_EMAIL, "", TEST_USER_PASSWORD);

        String request = GSON.toJson(joinRequest);

        this.mockMvc.perform(post("/members")
            .content(request)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andDo(print())
            .andDo(MemberDocumentation.failedCreateMemberByBlank());
    }

    @DisplayName("정보 수정 시 빈 문자열 입력이 들어올 경우 예외 발생")
    @Test
    public void failedUpdateMember() throws Exception {
        setMockToken();
        UpdateMemberRequest updateMemberRequest = new UpdateMemberRequest("",
            TEST_USER_PASSWORD);

        String request = GSON.toJson(updateMemberRequest);

        this.mockMvc.perform(put("/members/{id}", member.getId())
            .header(HttpHeaders.AUTHORIZATION, mockToken)
            .content(request)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andDo(print())
            .andDo(MemberDocumentation.failedUpdateMemberByBlank());
    }
}
