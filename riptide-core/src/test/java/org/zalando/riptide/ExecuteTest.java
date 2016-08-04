package org.zalando.riptide;

/*
 * ⁣​
 * Riptide
 * ⁣⁣
 * Copyright (C) 2015 Zalando SE
 * ⁣⁣
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ​⁣
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestClientException;
import org.zalando.riptide.model.Success;

import java.io.IOException;
import java.util.Arrays;

import static org.springframework.http.HttpStatus.Series.SUCCESSFUL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.zalando.riptide.Bindings.on;
import static org.zalando.riptide.Navigators.series;
import static org.zalando.riptide.Route.pass;

public final class ExecuteTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private final String url = "https://api.example.com";

    private final Rest unit;
    private final MockRestServiceServer server;

    public ExecuteTest() {
        final AsyncRestTemplate template = new AsyncRestTemplate();
        this.server = MockRestServiceServer.createServer(template);
        this.unit = Rest.builder()
                .requestFactory(template.getAsyncRequestFactory())
                .converters(Arrays.asList(
                        new MappingJackson2HttpMessageConverter(new ObjectMapper().findAndRegisterModules()),
                        new StringHttpMessageConverter()
                ))
                .baseUrl("https://api.example.com")
                .build();
    }

    @After
    public void after() {
        server.verify();
    }

    @Test
    public void shouldSendNoBody() throws IOException {
        server.expect(requestTo(url))
                .andExpect(content().string(""))
                .andRespond(withSuccess());

        unit.trace(url)
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldSendQueryParameters() throws IOException {
        server.expect(requestTo(url + "?foo=bar&foo=baz&bar=null"))
                .andRespond(withSuccess());

        unit.head(url)
                .queryParam("foo", "bar")
                .queryParams(ImmutableMultimap.of(
                        "foo", "baz",
                        "bar", "null"
                ))
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldSendHeaders() throws IOException {
        server.expect(requestTo(url))
                .andExpect(header("X-Foo", "bar"))
                .andRespond(withSuccess());

        unit.head(url)
                .header("X-Foo", "bar")
                .dispatch(series(),
                        on(SUCCESSFUL).call(pass()));
    }

    @Test
    public void shouldSendBody() throws IOException {
        server.expect(requestTo(url))
                .andExpect(content().string("{\"foo\":\"bar\"}"))
                .andRespond(withSuccess());

        unit.post(url)
                .body(ImmutableMap.of("foo", "bar"));
    }

    @Test
    public void shouldSendHeadersAndBody() throws IOException {
        server.expect(requestTo(url))
                .andExpect(header("X-Foo", "bar"))
                .andExpect(content().string("{\"foo\":\"bar\"}"))
                .andRespond(withSuccess());

        unit.put(url)
                .header("X-Foo", "bar")
                .body(ImmutableMap.of("foo", "bar"));
    }

    @Test
    public void shouldFailIfNoConverterFoundForBody() throws IOException {
        // we never actually make the request, but the mock server is doing some magic pre-actively
        server.expect(requestTo(url))
                .andExpect(header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess());

        exception.expect(RestClientException.class);
        exception.expectMessage("no suitable HttpMessageConverter found ");
        exception.expectMessage("org.zalando.riptide.model.Success");
        exception.expectMessage("application/xml");

        unit.patch(url)
                .accept(APPLICATION_JSON)
                .contentType(APPLICATION_XML)
                .body(new Success(true));
    }

    @Test
    public void shouldFailIfNoConverterFoundForBodyOfUnknownContentType() throws IOException {
        final AsyncRestTemplate template = new AsyncRestTemplate();
        final MockRestServiceServer server = MockRestServiceServer.createServer(template);
        final Rest unit = Rest.builder()
                .requestFactory(template.getAsyncRequestFactory())
                .converter(new Jaxb2RootElementHttpMessageConverter())
                .baseUrl("https://api.example.com")
                .build();

        // we never actually make the request, but the mock server is doing some magic pre-actively
        server.expect(requestTo(url))
                .andRespond(withSuccess());

        exception.expect(RestClientException.class);
        exception.expectMessage("no suitable HttpMessageConverter found ");
        exception.expectMessage("org.zalando.riptide.model.Success");

        unit.delete(url)
                .body(new Success(true));
    }

    @Test
    public void shouldFailIfNoConverterFoundForBodyOfUnsupportedContentType() throws IOException {
        final AsyncRestTemplate template = new AsyncRestTemplate();
        final MockRestServiceServer server = MockRestServiceServer.createServer(template);
        final Rest unit = Rest.builder()
                .requestFactory(template.getAsyncRequestFactory())
                .defaultConverters()
                .baseUrl("https://api.example.com")
                .build();

        // we never actually make the request, but the mock server is doing some magic pre-actively
        server.expect(requestTo(url))
                .andRespond(withSuccess());

        exception.expect(RestClientException.class);
        exception.expectMessage("no suitable HttpMessageConverter found ");
        exception.expectMessage("org.zalando.riptide.model.Success");

        unit.delete(url)
                .contentType(MediaType.parseMediaType("application/x-json-stream"))
                .body(new Success(true));
    }

}
