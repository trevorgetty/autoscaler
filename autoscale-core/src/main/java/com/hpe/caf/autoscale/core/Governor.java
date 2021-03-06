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
package com.hpe.caf.autoscale.core;

import com.hpe.caf.api.autoscale.InstanceInfo;
import com.hpe.caf.api.autoscale.ScalingAction;
import com.hpe.caf.api.autoscale.ScalingConfiguration;

/**
 * The Governor serves to prevent services from monopolizing the resources of a particular environment.
 * Prior to the introduction of the Governor it was possible for one service to prevent others from reaching their minimum instance
 * requirements by consuming resource for instances over and above the minimum
 *
 * As services are scheduled for scaling their scaling configuration is registered with the governor.
 * When information regarding the number of running instances is obtained it is recorded with with the governor.
 * A ScalingThread will determine the ScalingAction and then ask the Governor to review this scaling request.
 */
interface Governor {
    /**
     *
     * @param serviceRef the named reference to the service the instances refers to
     * @param instanceInfo the current instanceInfo for for the service
     */
    void recordInstances(String serviceRef, InstanceInfo instanceInfo);

    /**
     *
     * @param serviceRef the named reference to the service for which the scaling action will be governed
     * @param action the scaling action to review
     * @return a governed ScalingAction that respects the minimum requirements of all services
     */
    ScalingAction govern(String serviceRef, ScalingAction action);

    /**
     *
     * @param scalingConfiguration record the scalingConfiguration for a service
     */
    void register(ScalingConfiguration scalingConfiguration);

    /**
     *
     * @param serviceRef the service name to remove and stop scaling operations for
     */
    void remove(String serviceRef);
}
