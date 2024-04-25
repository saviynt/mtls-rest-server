package org.example.mtls;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @Value("${app.k8s.node-name}")
    private String nodeName;

    @Value("${app.k8s.node-ip-address}")
    private String nodeIpAddress;

    @Value("${app.k8s.pod-name-space}")
    private String podNameSpace;

    @Value("${app.k8s.pod-name}")
    private String podName;

    @Value("${app.k8s.pod-uid}")
    private String podUid;

    @Value("${app.k8s.pod-ip-address}")
    private String podIpAddress;

    @GetMapping("/hello")
    public ResponseEntity<Greetings> getGreetings() {
        return ResponseEntity.ok(
                new Greetings(nodeName, nodeIpAddress, podNameSpace, podName, podUid, podIpAddress)
        );
    }
}
