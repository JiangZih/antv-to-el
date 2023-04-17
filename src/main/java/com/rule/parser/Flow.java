package com.rule.parser;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * @author jiangzihao@kanzhun.com
 */

@Data
public class Flow {
    private List<Node> nodes;

    public static Flow getInstance() {
        Flow flow = new Flow();
        flow.setNodes(new ArrayList<Node>());
        return flow;
    }
}
