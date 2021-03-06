/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
 *
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
 */
package com.hpe.caf.autoscale.workload.rabbit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.api.autoscale.ScalerException;
import com.squareup.okhttp.OkHttpClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.http.GET;

public final class RabbitSystemResourceMonitor
{
    private final RabbitManagementApi rabbitApi;
    private final String rabbitEnpoint;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final int RMQ_TIMEOUT = 10;

    public RabbitSystemResourceMonitor(final String endpoint, final String user, final String pass)
    {
        this.rabbitEnpoint = endpoint;
        final String credentials = user + ":" + pass;
        final OkHttpClient ok = new OkHttpClient();
        ok.setReadTimeout(RMQ_TIMEOUT, TimeUnit.SECONDS);
        ok.setConnectTimeout(RMQ_TIMEOUT, TimeUnit.SECONDS);
        // build up a RestAdapter that will automatically handle authentication for us
        final RestAdapter.Builder builder = new RestAdapter.Builder().setEndpoint(endpoint).setClient(new OkClient(ok));
        builder.setRequestInterceptor(requestFacade -> {
            final String str = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            requestFacade.addHeader("Accept", "application/json");
            requestFacade.addHeader("Authorization", str);
        });
        builder.setErrorHandler(new RabbitApiErrorHandler());
        final RestAdapter adapter = builder.build();
        rabbitApi = adapter.create(RabbitManagementApi.class);
    }

    public double getCurrentMemoryComsumption() throws ScalerException
    {
        try {
            final Response response = rabbitApi.getNodeStatus();
            final JsonNode nodeArray = mapper.readTree(response.getBody().in());
            final Iterator<JsonNode> iterator = nodeArray.elements();
            double highestMemUsedInCluster = 0;
            while (iterator.hasNext()) {
                final JsonNode node = iterator.next();
                final long memory_limit = node.get("mem_limit").asLong();
                final long memory_used = node.get("mem_used").asLong();
                final double memPercentage = ((double) memory_used / memory_limit) * 100;
                highestMemUsedInCluster = memPercentage > highestMemUsedInCluster ? memPercentage : highestMemUsedInCluster;
            }
            return highestMemUsedInCluster;
        } catch (final IOException ex) {
            throw new ScalerException("Unable to map response to status request.", ex);
        }
    }

    private static class RabbitApiErrorHandler implements ErrorHandler
    {
        @Override
        public Throwable handleError(final RetrofitError retrofitError)
        {
            return new ScalerException("Failed to contact RabbitMQ management API", retrofitError);
        }
    }

    public interface RabbitManagementApi
    {
        @GET("/api/nodes/")
        Response getNodeStatus()
            throws ScalerException;
    }
}
