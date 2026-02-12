package com.payment_service.payment.services;

import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Service;

@Service
public class GatewayUrlResolver {

    private final LoadBalancerClient loadBalancer;

    public GatewayUrlResolver(LoadBalancerClient loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    public String gatewayBaseUrl() {
        ServiceInstance instance = loadBalancer.choose("CLOUD-GATEWAY");
        if (instance == null) {
            throw new IllegalStateException("No instances of CLOUD-GATEWAY found in Eureka");
        }
        return instance.getUri().toString(); // e.g. http://10.10.10.70:8085
    }
}
