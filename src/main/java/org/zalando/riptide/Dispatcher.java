package org.zalando.riptide;

/*
 * ⁣​
 * riptide
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

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;

public final class Dispatcher {

    private final RestTemplate template;
    private final ClientHttpResponse response;
    private final Router router = new Router();

    Dispatcher(RestTemplate template, ClientHttpResponse response) {
        this.template = template;
        this.response = response;
    }

    @SafeVarargs
    public final <A> Retriever dispatch(Selector<A> selector, Binding<A>... bindings)
            throws UnsupportedResponseException {
        final List<HttpMessageConverter<?>> converters = template.getMessageConverters();
        final Object value = route(selector, converters, bindings);
        return new Retriever(value);
    }

    private <A> Object route(Selector<A> selector, List<HttpMessageConverter<?>> converters, Binding<A>[] bindings) {
        try {
            return router.route(response, converters, selector, asList(bindings));
        } catch (IOException e) {
            throw new RestClientException(null, e);
        }
    }

}