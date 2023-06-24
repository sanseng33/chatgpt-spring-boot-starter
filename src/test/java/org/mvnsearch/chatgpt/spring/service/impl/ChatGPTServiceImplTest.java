package org.mvnsearch.chatgpt.spring.service.impl;

import org.junit.jupiter.api.Test;
import org.mvnsearch.chatgpt.demo.ProjectBootBaseTest;
import org.mvnsearch.chatgpt.model.ChatCompletionRequest;
import org.mvnsearch.chatgpt.model.ChatCompletionResponse;
import org.mvnsearch.chatgpt.model.ChatFunction;
import org.mvnsearch.chatgpt.model.ChatRequestBuilder;
import org.mvnsearch.chatgpt.spring.service.ChatGPTService;
import org.mvnsearch.chatgpt.spring.service.PromptManager;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ChatGPTServiceImplTest extends ProjectBootBaseTest {
    @Autowired
    private ChatGPTService chatGPTService;
    @Autowired
    private PromptManager promptManager;

    @Test
    public void testSimpleChat() {
        final ChatCompletionRequest request = ChatCompletionRequest.of("What's Java Language?");
        final ChatCompletionResponse response = chatGPTService.chat(request).block();
        System.out.println(response.getReplyText());
    }

    @Test
    public void testExecuteSQLQuery() throws Exception {
        final String prompt = "Query all employees whose salary is greater than the average.";
        final ChatCompletionRequest request = ChatRequestBuilder.of(promptManager.prompt("sql-developer", prompt))
                .function("execute_sql_query")
                .build();
        String result = chatGPTService.chat(request).flatMap(ChatCompletionResponse::getReplyCombinedText).block();
        System.out.println(result);
    }


    @Test
    public void testChatWithFunctions() throws Exception {
        final String prompt = "查找华为技术有限公司最新申请的20篇专利";

        Map<String, ChatFunction.JsonSchemaProperty> param = getFunctionParam();

        ChatFunction function = new ChatFunction();
        //函数名称
        function.setName("obtainPatentList");
        //函数描述
        function.setDescription("通过查询语句和查询数量等检索获取一批专利信息的结果");
        //函数参数
        function.setParameters(new ChatFunction.Parameters("object", param, Arrays.asList("sort","page","q","playbook","_type")));

        //构造Completion请求
        final ChatCompletionRequest request = ChatRequestBuilder.of(prompt)
                .function(function)
                .build();
        final ChatCompletionResponse response = chatGPTService.chat(request).block();
        // display reply combined text with function call
        System.out.println(response.getReplyCombinedText().block());


    }

    private Map<String, ChatFunction.JsonSchemaProperty> getFunctionParam() {
        //函数参数
        ChatFunction.JsonSchemaProperty page = new ChatFunction.JsonSchemaProperty("page", "number", "列表结果的页数");
        ChatFunction.JsonSchemaProperty limit = new ChatFunction.JsonSchemaProperty("limit", "number", "列表结果每页有几条专利");
        ChatFunction.JsonSchemaProperty q = new ChatFunction.JsonSchemaProperty("q", "string", "检索语句，比如查询百度公司的专利，语句为:ANCS\"百度公司\"");
        ChatFunction.JsonSchemaProperty sort = new ChatFunction.JsonSchemaProperty("sort", "string", "获取专利的排序方式，比如按最新申请排序（desc）、按相关度最高排序（sdesc）");
        ChatFunction.JsonSchemaProperty playbook = new ChatFunction.JsonSchemaProperty("playbook", "string", "检索类型，比如简单检索语句的为简单检索（smartSearch），检索语句超过100字符的为语义检索（NoveltySearch）");
        ChatFunction.JsonSchemaProperty type = new ChatFunction.JsonSchemaProperty("type", "string", "默认检索方式：query");
        Map<String, ChatFunction.JsonSchemaProperty> param = new HashMap<>();
        param.put("page", page);param.put("limit", limit);param.put("q", q);param.put("sort", sort);param.put("playbook", playbook);param.put("type", type);
        return param;
    }

    @Test
    public void testPromptAsFunction() {
        Function<String, Mono<String>> translateIntoChineseFunction = chatGPTService.promptAsLambda("translate-into-chinese");
        Function<String, Mono<String>> sendEmailFunction = chatGPTService.promptAsLambda("send-email", "send_email");
        String result = Mono.just("Hi Jackie, could you write an email to Libing(libing.chen@exaple.com) and Sam(linux_china@example.com) and invite them to join Mike's birthday party at 4 pm tomorrow? Thanks!")
                .flatMap(translateIntoChineseFunction)
                .flatMap(sendEmailFunction)
                .block();
        System.out.println(result);
    }

    public record TranslateRequest(String from, String to, String text) {
    }

    @Test
    public void testLambdaWithRecord() {
        Function<TranslateRequest, Mono<String>> translateFunction = chatGPTService.promptAsLambda("translate");
        String result = Mono.just(new TranslateRequest("Chinese", "English", "你好！"))
                .flatMap(translateFunction)
                .block();
        System.out.println(result);
    }

    @Test
    public void testLambdaWithFunctionResult() {
        Function<String, Mono<List<String>>> executeSqlQuery = chatGPTService.promptAsLambda("sql-developer", "execute_sql_query");
        List<String> result = Mono.just("Query all employees whose salary is greater than the average.")
                .flatMap(executeSqlQuery)
                .block();
//        assertThat(result).isNotEmpty();
    }
}
