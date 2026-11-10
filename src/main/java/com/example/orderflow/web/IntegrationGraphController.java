package com.example.orderflow.web;

import org.springframework.integration.graph.IntegrationGraphServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IntegrationGraphController {

    private final IntegrationGraphServer graphServer;

    public IntegrationGraphController(IntegrationGraphServer graphServer) {
        this.graphServer = graphServer;
    }

    @GetMapping("/api/integration/graph")
    public Object graph() {
        return graphServer.getGraph();
    }
}
