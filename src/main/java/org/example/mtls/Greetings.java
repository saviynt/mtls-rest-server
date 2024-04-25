package org.example.mtls;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Greetings {
    private String nodeName;
    private String nodeIpAddress;
    private String nameSpace;
    private String podName;
    private String podUid;
    private String podIpAddress;
}
