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

package tw.waterball.judgegirl.api.retrofit;

import com.fasterxml.jackson.databind.ObjectMapper;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author - johnny850807@gmail.com (Waterball)
 */

@Named
public class RetrofitFactory {
    private ObjectMapper objectMapper;

    @Inject
    public RetrofitFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Retrofit create(String scheme, String host, int port) {
        if (host.endsWith("/")) {
            throw new IllegalArgumentException("The base url should not end with '/'");
        }
        return new Retrofit.Builder()
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .baseUrl(String.format("%s://%s:%d", scheme, host, port))
                .build();
    }
}
