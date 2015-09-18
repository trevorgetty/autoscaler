package com.hpe.caf.autoscale.source.marathon;


import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.ServicePath;
import com.hpe.caf.api.autoscale.ScalerException;
import com.hpe.caf.api.autoscale.ServiceSource;
import com.hpe.caf.api.autoscale.ServiceSourceProvider;
import com.hpe.caf.autoscale.MarathonAutoscaleConfiguration;
import feign.Feign;
import feign.Request;
import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;


public class MarathonServiceSourceProvider implements ServiceSourceProvider
{
    private static final int MARATHON_TIMEOUT = 10_000;


    @Override
    public ServiceSource getServiceSource(final ConfigurationSource configurationSource, final ServicePath servicePath)
            throws ScalerException
    {
        try {
            Iterator<String> groupIterator = servicePath.groupIterator();
            StringBuilder groupPath = new StringBuilder();
            while (groupIterator.hasNext()) {
                groupPath.append(groupIterator.next()).append('/');
            }
            MarathonAutoscaleConfiguration config = configurationSource.getConfiguration(MarathonAutoscaleConfiguration.class);
            Feign.Builder builder = Feign.builder().options(new Request.Options(MARATHON_TIMEOUT, MARATHON_TIMEOUT));
            Marathon marathon = MarathonClient.getInstance(builder, config.getEndpoint());
            return new MarathonServiceSource(marathon, groupPath.toString(), new URL(config.getEndpoint()));
        } catch (ConfigurationException | MalformedURLException e) {
            throw new ScalerException("Failed to create service source", e);
        }
    }
}
